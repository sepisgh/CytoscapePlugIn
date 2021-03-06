/*
 * Created on Apr 20, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.StringUtils;
import org.osgi.framework.BundleContext;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.FuzzyLogicSimulator.ANDGateMode;
import org.reactome.booleannetwork.HillFunction;
import org.reactome.booleannetwork.IdentityFunction;
import org.reactome.booleannetwork.TransferFunction;
import org.reactome.cytoscape.drug.DrugDataSource;
import org.reactome.cytoscape.drug.DrugTargetInteractionManager;
import org.reactome.cytoscape.drug.InteractionListTableModel;
import org.reactome.cytoscape.drug.InteractionListView;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pathway.booleannetwork.AffinityToModificationMap;
import org.reactome.pathway.booleannetwork.DefaultAffinityToModificationMap;
import org.reactome.pathway.booleannetwork.DrugTargetInteractionTypeMapper;
import org.reactome.pathway.booleannetwork.ModificationType;

import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * Implemented as a CytoPanelComponent to be listed in the "Results Panel".
 * @author gwu
 *
 */
public class BooleanNetworkMainPane extends JPanel implements CytoPanelComponent {
    public static final String TITLE = "Boolean Network Modelling";
    private PathwayEditor pathwayEditor;
    // To highlight pathway
    private PathwayHighlightControlPanel hiliteControlPane;
    // To hold samples
    private JTabbedPane tabbedPane;
    // Control buttons
    private JButton simulateBtn;
    private JButton newBtn;
    private JButton deleteBtn;
    private JButton compareBtn;
    // To avoid index error
    private boolean duringDeletion;
    // Cached BooleanNetwork to increase the performance
    private Map<String, BooleanNetwork> keyToBN;
    
    /**
     * Default constructor.
     */
    public BooleanNetworkMainPane() {
        init();
    }
    
    private void initGUI() {
        setLayout(new BorderLayout());
        
        JPanel controlPane = createControlPane();
        add(controlPane, BorderLayout.NORTH);
        
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        tabbedPane.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!duringDeletion)
                    validateButtons();
            }
        });
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    public void close() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component c = tabbedPane.getTabComponentAt(i);
            if (c instanceof BooleanNetworkSamplePane) {
                BooleanNetworkSamplePane bnPane = (BooleanNetworkSamplePane) c;
                bnPane.remove();
            }
        }
        getParent().remove(this);
    }
    
    private JPanel createControlPane() {
        JPanel controlPane = new JPanel();
        controlPane.setBorder(BorderFactory.createEtchedBorder());
        
        simulateBtn = new JButton("Simulate");
        simulateBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                performSimulation();
            }
        });
        controlPane.add(simulateBtn);
        
        newBtn = new JButton("New");
        newBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewSimulation();
            }
        });
        controlPane.add(newBtn);
        
        deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSimulation();
            }
        });
        controlPane.add(deleteBtn);
        
        compareBtn = new JButton("Compare");
        compareBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                compareSimulation();
            }
        });
        controlPane.add(compareBtn);
        
        return controlPane;
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }

    private void compareSimulation() {
        CompareSimulationDialog dialog = new CompareSimulationDialog();
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOKClicked)
            return;
        String[] simuationNames = dialog.getSelectedSimulations();
        // Get simulations based on name
        int index = tabbedPane.indexOfTab(simuationNames[0]);
        BooleanNetworkSamplePane pane1 = (BooleanNetworkSamplePane) tabbedPane.getComponentAt(index);
        index = tabbedPane.indexOfTab(simuationNames[1]);
        BooleanNetworkSamplePane pane2 = (BooleanNetworkSamplePane) tabbedPane.getComponentAt(index);
        // Check to make sure two selected simulations generated from the same pathway diagram
        if (!pane1.getPathwayDiagramID().equals(pane2.getPathwayDiagramID())) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Selected simulations for comparison were generated from\n"
                                                  + "two different pathways. Choose two simulations from the\n"
                                                  + "same pathway for comparison.",
                                                  "Error in Comparison",
                                                  JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        SimulationTableModel sim1 = pane1.getSimulation();
        SimulationTableModel sim2 = pane2.getSimulation();
        displayComparison(sim1, 
                          sim2,
                          pane1.getHiliteControlPane());
    }
    
    private void displayComparison(SimulationTableModel sim1,
                                   SimulationTableModel sim2,
                                   PathwayHighlightControlPanel hilitePane) {
        SimulationComparisonPane comparisonPane = new SimulationComparisonPane(sim1.getSimulationName() + " vs. " + sim2.getSimulationName());
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(comparisonPane.getCytoPanelName());
        int index = cytoPanel.indexOfComponent(comparisonPane);
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
        comparisonPane.setHiliteControlPane(hilitePane);
        comparisonPane.setSimulations(sim1, sim2);
    }
    
    private void deleteSimulation() {
        BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
        if (samplePane == null)
            return;
        duringDeletion = true;
        samplePane.delete();
        duringDeletion = false;
        validateButtons();
    }
    
    /**
     * Create a new simulation.
     */
    public void createNewSimulation() {
        NewSimulationDialog dialog = new NewSimulationDialog();
        Set<String> usedNames = getUsedNames();
        dialog.usedNames = usedNames;
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        BooleanNetworkSamplePane samplePane = new BooleanNetworkSamplePane();
        // The following order is important to display selected set of variables
        samplePane.setPathwayEditor(pathwayEditor);
        samplePane.setHiliteControlPane(hiliteControlPane);
        // Make sure default value set is called before setBooleanNetwork.
        samplePane.setDefaultValue(dialog.getDefaultValue());
        samplePane.setSampleName(dialog.getSimulationName());
        samplePane.setAndGateMode(dialog.getAndGateMode());
        List<String> targets = null;
        if (dialog.isDrugSelected()) {
            samplePane.setProteinActivation(dialog.getActivation());
            samplePane.setProteinInhibtion(dialog.getInhibition());
//            if (dialog.isFilterMemebrsToTargets())
//                targets = dialog.getSelectedTargets();
        }
        // Even if there is no drug assigned, however, when the user provides
        // a target lists and select filterMemberToTargets, we will use a targeted
        // network. This is in order to create the same background network for perturbation
        // analysis. (G.W. on Oct 2, 2018)
        if (dialog.isFilterMemebrsToTargets())
            targets = dialog.getSelectedTargets();
        samplePane.setTransferFunction(dialog.getTransferFunction());
        BooleanNetwork network = getBooleanNetwork(targets);
        samplePane.setBooleanNetwork(network,
                                     dialog.isDrugSelected() ? dialog.getSelectedDrugs() : null);
        tabbedPane.add(dialog.getSimulationName(), samplePane);
        tabbedPane.setSelectedComponent(samplePane); // Select the newly created one
        validateButtons();
    }
    
    private BooleanNetwork getBooleanNetwork(List<String> targets) {
        Long pathwayId = pathwayEditor.getRenderable().getReactomeId();
        // Check if it has been loaded previously
        String key = generateBNKey(pathwayId, targets);
        BooleanNetwork network = keyToBN.get(key);
        if (network != null)
            return network;
        
        RESTFulFIService fiService = new RESTFulFIService();
        try {
            network = fiService.convertPathwayToBooleanNetwork(pathwayId,
                                                               targets);
            if (targets != null && targets.size() > 0) {
                String name = network.getName();
                network.setName(name + " (focused on " + String.join(", ", targets) + ")");
            }
            keyToBN.put(key, network);
            return network;
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot convert the displayed pathway to a Boolean network:\n" + e.getMessage(),
                                          "Error in Converting",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null; // Cannot do anything basically
        }
    }

    private String generateBNKey(Long pathwayId, 
                                 List<String> targets) {
        // Check if it has been loaded previously
        if (targets == null) {
            targets = new ArrayList<>();
        }
        targets.sort(Comparator.naturalOrder());
        String key = pathwayId + "|" + String.join(",", targets);
        return key;
    }
    
    private Set<String> getUsedNames() {
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
            usedNames.add(tabbedPane.getTitleAt(i));
        }
        return usedNames;
    }
    
    private void validateButtons() {
        deleteBtn.setEnabled(tabbedPane.getComponentCount() > 0);
        // Simulate button
        if (tabbedPane.getComponentCount() == 0)
            simulateBtn.setEnabled(false);
        else {
            BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
            simulateBtn.setEnabled(!samplePane.isSimulationPerformed());
        }
        // Compare button: At least two simulations
        if (tabbedPane.getComponentCount() < 2)
            compareBtn.setEnabled(false);
        else {
            int count = 0;
            for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
                BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getComponentAt(i);
                if (samplePane.isSimulationPerformed())
                    count ++;
            }
            compareBtn.setEnabled(count > 1);
        }
    }
    
    private void performSimulation() {
        BooleanNetworkSamplePane samplePane = (BooleanNetworkSamplePane) tabbedPane.getSelectedComponent();
        samplePane.simulate();
        validateButtons();
    }
    
    private void init() {
        initGUI();
        PlugInUtilities.registerCytoPanelComponent(this);
        // Most likely SessionAboutToBeLoadedListener should be used in 3.1.0.
        SessionLoadedListener sessionListener = new SessionLoadedListener() {
            
            @Override
            public void handleEvent(SessionLoadedEvent e) {
                getParent().remove(BooleanNetworkMainPane.this);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(SessionLoadedListener.class.getName(),
                                sessionListener, 
                                null);
        
        keyToBN = new HashMap<>();
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    /**
     * Return EAST so that it is displayed as a tab in the Results Panel.
     */
    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public Icon getIcon() {
        return null;
    }
    
    private class CompareSimulationDialog extends JDialog {
        private boolean isOKClicked;
        private JComboBox<String> simulationIBox;
        private JComboBox<String> simulationIIBox;
        
        public CompareSimulationDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
        }
        
        private void init() {
            setTitle("Compare Simulations");
            JPanel contentPane = createContentPane();
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!checkSelections())
                        return;
                    isOKClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = false;
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setSize(400, 225);
            setLocationRelativeTo(getOwner());
        }
        
        private boolean checkSelections() {
            String[] selections = getSelectedSimulations();
            if (selections[0].equals(selections[1])) {
                JOptionPane.showMessageDialog(this,
                                              "The selected simulations are the same. Please choose\n" +
                                              "two different simulations to copmare.",
                                              "Error in Selection", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        private JPanel createContentPane() {
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(2, 2, 2, 2);
            constraints.anchor = GridBagConstraints.WEST;
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose simulations to compare:");
            constraints.gridwidth = 3;
            contentPane.add(label, constraints);
            label = new JLabel("Simulation I");
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            contentPane.add(label, constraints);
            label = new JLabel("Simulation II");
            constraints.gridx = 2;
            contentPane.add(label, constraints);
            
            simulationIBox = createSimulationBox();
            constraints.gridx = 0;
            constraints.gridy = 2;
            simulationIBox.setSelectedIndex(0);
            contentPane.add(simulationIBox, constraints);
            label = new JLabel("vs.");
            constraints.gridx = 1;
            contentPane.add(label, constraints);
            simulationIIBox = createSimulationBox();
            constraints.gridx = 2;
            simulationIIBox.setSelectedIndex(1);
            contentPane.add(simulationIIBox, constraints);
            
            return contentPane;
        }
        
        private JComboBox<String> createSimulationBox() {
            Vector<String> items = new Vector<>();
            for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
                String title = tabbedPane.getTitleAt(i);
                items.add(title);
            }
            Collections.sort(items);
            JComboBox<String> box = new JComboBox<>(items);
            box.setEditable(false);
            return box;
        }
        
        public String[] getSelectedSimulations() {
            String[] rtn = new String[2];
            rtn[0] = (String) simulationIBox.getSelectedItem();
            rtn[1] = (String) simulationIIBox.getSelectedItem();
            return rtn;
        }
    }
    
    private class NewSimulationDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField nameTF;
        private JTextField defaultValueTF;
        private JComboBox<ANDGateMode> andGateBox;
        private Set<String> usedNames;
        private JTextField drugBox;
        // Cache drug selection dialog for easy handling: to keep the original
        // selection (not for performance).
        private Map<DrugDataSource, DrugSelectionDialog> srcToDrugList;
        // Cache values from the drug list: We should not get these values
        // from drugList dialog since new selections may be discarded
        private Map<String, Double> inhibition;
        private Map<String, Double> activation;
        private JCheckBox filterMembersToTargetsBox;
        private JTextField targetBox;
        private JComboBox<DrugDataSource> sourceBox;
        private Map<String, JTextField> hillParaToBox;
        // For function selection
        private ButtonGroup functionBtnGroup;
        
        public NewSimulationDialog() {
            super(PlugInObjectManager.getManager().getCytoscapeDesktop());
            init();
            srcToDrugList = new HashMap<>();
        }
        
        public boolean isDrugSelected() {
            return drugBox.getText().length() > 0;
        }
        
        public String getSelectedDrugs() {
            return drugBox.getText().trim();
        }
        
        public boolean isFilterMemebrsToTargets() {
            return filterMembersToTargetsBox.isSelected();
        }
        
        private List<String> getTargets() {
            Map<String, Double> inhibition = getInhibition();
            Set<String> targets1 = inhibition.keySet().stream().collect(Collectors.toSet());
            Map<String, Double> activation = getActivation();
            Set<String> targets2 = activation.keySet().stream().collect(Collectors.toSet());
            Set<String> targets = new HashSet<String>(targets1);
            targets.addAll(targets2);
            List<String> list = new ArrayList<>(targets);
            list.sort(Comparator.naturalOrder());
            return list;
        }
        
        private List<String> getSelectedTargets() {
            String text = targetBox.getText().trim();
            String[] tokens = text.split(",");
            List<String> rtn = new ArrayList<>();
            for (String token : tokens)
                rtn.add(token.trim());
            return rtn;
        }
        
        public Map<String, Double> getInhibition() {
            return inhibition;
        }
        
        public Map<String, Double> getActivation() {
            return activation;
        }
        
        private void init() {
            setTitle("New Logic Model Simulation");
            
            JPanel setupPane = createSetUpPanel();
            JPanel functionPane = createFunctionPane();
            JPanel drugPane = createDrugPane();
            
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.add("General", setupPane);
            tabbedPane.add("Transfer Function", functionPane);
            tabbedPane.addTab("Drug Application", drugPane);
            
            getContentPane().add(tabbedPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.setBorder(BorderFactory.createEtchedBorder());
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(!checkValues())
                        return;
                    isOkClicked = true;
                    dispose();
                }
            });
            controlPane.getCancelBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOkClicked = false;
                    dispose();
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
            
            setSize(500, 375);
            setLocationRelativeTo(this.getOwner());
            
            getRootPane().setDefaultButton(controlPane.getOKBtn());
        }

        protected JPanel createSetUpPanel() {
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.WEST;
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Enter a name for the simulation:");
            constraints.gridx = 0;
            constraints.gridy = 0;
            contentPane.add(label, constraints);
            nameTF = new JTextField();
            nameTF.setColumns(20);
            // Just give it a name for easy test
            nameTF.setText("Untitled");
            constraints.gridy = 1;
            contentPane.add(nameTF, constraints);
            label = GKApplicationUtilities.createTitleLabel("Enter default input value between 0 and 1:");
            constraints.gridy = 2;
            contentPane.add(label, constraints);
            defaultValueTF = new JTextField();
            defaultValueTF.setText(1.0 + ""); // Use 1.0 as the default.
            constraints.gridy = 3;
            contentPane.add(defaultValueTF, constraints);
            defaultValueTF.setColumns(20);
            
            JPanel pane = createAndGatePane();
            constraints.gridy = 4;
            contentPane.add(pane, constraints);
            return contentPane;
        }
        
        private JPanel createFunctionPane() {
            JPanel panel = new JPanel();
            panel.setBorder(BorderFactory.createEtchedBorder());
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(1, 1, 1, 1);
            
            JLabel label = GKApplicationUtilities.createTitleLabel("Choose a transfer function:");
            constraints.gridy = 0;
            panel.add(label, constraints);
            
            JRadioButton identityBtn = new JRadioButton("Identity Function");
            JRadioButton hillBtn = new JRadioButton("Hill Function");
            functionBtnGroup = new ButtonGroup();
            functionBtnGroup.add(identityBtn);
            functionBtnGroup.add(hillBtn);
            identityBtn.setSelected(true); // Use as the default
            constraints.gridy ++;
            panel.add(identityBtn, constraints);
            constraints.gridy ++;
            panel.add(hillBtn, constraints);
            
            // Create parameters for hill function
            constraints.anchor = GridBagConstraints.CENTER;
            JPanel hillParameterPane = createHillParameterPane();
            constraints.gridy ++;
            panel.add(hillParameterPane, constraints);
            
            JLabel infoLabel = new JLabel("<html><i>*Click to view the information about <br />the transfer function and parameters.<i><html>");
            infoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            infoLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    String url = "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3292705/figure/fig01/";
                    PlugInUtilities.openURL(url);
                }
                
            });
            constraints.gridy ++;
            constraints.anchor = GridBagConstraints.WEST;
            panel.add(infoLabel, constraints);
            
            return panel;
        }
        
        private JPanel createHillParameterPane() {
            JPanel pane = new JPanel();
            pane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(1, 1, 1, 1);

            // Want to get the default values from an Hill function
            HillFunction function = new HillFunction();
            Map<String, Number> nameToValue = function.getParameters();
            hillParaToBox = new HashMap<>();
            nameToValue.keySet().stream()
            .sorted()
            .forEach(name -> {
                JLabel pLabel = new JLabel(name + ": ");
                JTextField tf = new JTextField();
                tf.getDocument().addDocumentListener(new DocumentListener() {

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        validateTF(name.equals("n") ? Integer.class : Double.class, tf);
                    }

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        validateTF(name.equals("n") ? Integer.class : Double.class, tf);
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                    }
                });
                // Record for later use
                hillParaToBox.put(name, tf);
                tf.setColumns(4);
                tf.setText(nameToValue.get(name) + "");
                constraints.gridy ++;
                constraints.gridx = 0;
                pane.add(pLabel, constraints);
                constraints.gridx = 1;
                pane.add(tf, constraints);
            });

            return pane;
        }
        
        private void validateTF(Class<?> cls, JTextField tf) {
            try {
                if (cls == Integer.class)
                    Integer.parseInt(tf.getText().trim());
                else if (cls == Double.class)
                    Double.parseDouble(tf.getText().trim());
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "The input should be an " + (cls == Integer.class ? "integer" : "double!"),
                                              "Parameter Input Error", 
                                              JOptionPane.ERROR_MESSAGE);
            }
        }

        private JPanel createDrugPane() {
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            panel.setBorder(BorderFactory.createEtchedBorder());
            
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = new Insets(1, 1, 1, 1);
            
            // Add a JComBox for choosing data source
            JPanel sourcePane = new JPanel();
            // 5 is the default value
            sourcePane.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
            JLabel sourceLabel = new JLabel("Choose data source: ");
            sourcePane.add(sourceLabel);
            sourceBox = new JComboBox<>();
            Arrays.asList(DrugDataSource.values()).forEach(s -> sourceBox.addItem(s));
            sourceBox.setEditable(false);
            sourceBox.setSelectedIndex(0); // Use the first as default
            sourcePane.add(sourceBox);
            constraints.gridy = 0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(sourcePane, constraints);
            
            JPanel drugPane = new JPanel();
            drugPane.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
            JLabel chooseDrugLabel = new JLabel("Select drugs: ");
            drugBox = new JTextField();
            drugBox.setColumns(20);
            drugBox.setEditable(false);
            JButton drugBtn = new JButton("...");
            drugBtn.setToolTipText("Click to choose drugs");
            drugBtn.setPreferredSize(new Dimension(20, 20));
            drugBtn.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showDrugsForPathway();
                }
            });
            drugPane.add(chooseDrugLabel);
            drugPane.add(drugBox);
            drugPane.add(drugBtn);
            constraints.gridy ++;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(drugPane, constraints);
            
            JLabel targetLabel = new JLabel("Selected targets (you may edit below):");
            constraints.gridy ++;
            panel.add(targetLabel, constraints);
            
            targetBox = new JTextField();
            targetBox.setColumns(20);
            constraints.gridy ++;
            panel.add(targetBox, constraints);
            
            // Add another checkbox
            filterMembersToTargetsBox = new JCheckBox("Filter members in sets to drug targets only");
            constraints.gridy ++;
            panel.add(filterMembersToTargetsBox, constraints);
            
            return panel;
        }
        
        private void showDrugsForPathway() {
            DrugSelectionDialog drugList = srcToDrugList.get(sourceBox.getSelectedItem());
            if (drugList != null) {
                drugList.isOKClicked = false;
                drugList.setModal(true);
                drugList.setVisible(true);
                drugList.toFront();
                if (!drugList.isOKClicked)
                    return;
                extractDrugSelectionInfo(drugList);
                return;
            }
            Thread t = new Thread() {
                public void run() {
                    DrugTargetInteractionManager manager = DrugTargetInteractionManager.getManager();
                    Set<Interaction> interactions = manager.fetchDrugsInteractions(pathwayEditor,
                                                                                   (DrugDataSource)sourceBox.getSelectedItem());
                    if (interactions.size() == 0) {
                        JOptionPane.showMessageDialog(NewSimulationDialog.this,
                                "Cannot find a drug that interacts with any component in the pathway.", 
                                "No Drug Found",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                    DrugSelectionDialog drugList = new DrugSelectionDialog(NewSimulationDialog.this);
                    drugList.setInteractions(new ArrayList<>(interactions));
                    drugList.setModal(true);
                    drugList.setVisible(true);
                    if (!drugList.isOKClicked)
                        return;
                    extractDrugSelectionInfo(drugList);
                    srcToDrugList.put((DrugDataSource)sourceBox.getSelectedItem(),
                                      drugList);
                }
            };
            t.start();
        }
        
        private void extractDrugSelectionInfo(DrugSelectionDialog drugList) {
            List<String> drugs = drugList.getSelectedDrugs();
            drugBox.setText(StringUtils.join(", ", drugs));
            inhibition = drugList.getInhibition();
            activation = drugList.getActivation();
            List<String> targets = getTargets();
            targetBox.setText(String.join(", ", targets));
        }
        
        private JPanel createAndGatePane() {
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel label = new JLabel("Choose AND gate mode: ");
            andGateBox = new JComboBox<>(ANDGateMode.values());
            panel.add(label);
            panel.add(andGateBox);
            // Use PROD as the default
            andGateBox.setSelectedIndex(1);
            return panel;
        }
        
        private boolean checkValues() {
            String name = nameTF.getText().trim();
            if (name.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "The name field is empty!",
                                              "Empty Name",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            // Check if the name has been used
            if (usedNames != null && usedNames.contains(name)) {
                JOptionPane.showMessageDialog(this,
                                              "The entered name has been used. Try a new name.",
                                              "Duplicated Name",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            String value = defaultValueTF.getText().trim();
            if (value.length() == 0) {
                JOptionPane.showMessageDialog(this,
                                              "The value field is empty. Enter a number between 0 and 1 (inclusively).",
                                              "Empty Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            Double number = null;
            try {
                number = new Double(value);
            }
            catch(NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                                              "Enter a number between 0 and 1 (inclusively) for the default value.",
                                              "Wrong Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (number > 1.0 || number < 0.0) {
                JOptionPane.showMessageDialog(this,
                                              "Enter a number between 0 and 1 (inclusively) for the default value.",
                                              "Wrong Value",
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        public ANDGateMode getAndGateMode() {
            return (ANDGateMode) andGateBox.getSelectedItem();
        }
        
        public String getSimulationName() {
            return nameTF.getText().trim();
        }
        
        public double getDefaultValue() {
            return new Double(defaultValueTF.getText().trim());
        }
        
        public TransferFunction getTransferFunction() {
            TransferFunction function = null;
            Enumeration<AbstractButton> buttons = functionBtnGroup.getElements();
            while (buttons.hasMoreElements()) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    if (button.getText().startsWith("Identity"))
                        return new IdentityFunction();
                    else if (button.getText().startsWith("Hill"))
                        return createHillFunction();
                }
            }
            return new IdentityFunction(); // Default
        }
        
        private HillFunction createHillFunction() {
            HillFunction function = new HillFunction();
            int n = Integer.parseInt(hillParaToBox.get("n").getText().trim());
            double k = Double.parseDouble(hillParaToBox.get("h").getText().trim());
            double g = Double.parseDouble(hillParaToBox.get("g").getText().trim());
            function.setParameters(n, k, g);
            return function;
        }
        
    }
    
    private class DrugSelectionTableModel extends InteractionListTableModel {
        // Used to map from affinity to modification strength
        private AffinityToModificationMap affinityToModificationMap;
        private DrugTargetInteractionTypeMapper typeManager;
        
        public DrugSelectionTableModel() {
            super();
//            String[] labels = new String[] {
//                    "ID",
//                    "Drug",
//                    "Target",
//                    "KD (nM)",
//                    "IC50 (nM)",
//                    "Ki (nM)",
//                    "EC50 (nM)",
//                    "Modification",
//                    "Strength"
//            };
            affinityToModificationMap = new DefaultAffinityToModificationMap();
            typeManager = new DrugTargetInteractionTypeMapper();
        }
        
        @Override
        protected void resetColumns(List<Interaction> interactions) {
            super.resetColumns(interactions);
            // Need to add the following extra columns
//          "Modification",
//          "Strength"
            colNames.add("Modification");
            colNames.add("Strength");
        }

        @Override
        protected void initRow(Interaction interaction, Object[] row) {
            super.initRow(interaction, row);
            row[colNames.size() - 2] = typeManager.getModificationType(interaction.getInteractionType());
            Double minValue = getMinValue(row);
            if (minValue == null)
                row[colNames.size() - 1] = null;
            else {
                row[colNames.size() - 1] = getModificationStrenth(minValue);
            }
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            Object[] rowValues = data.get(rowIndex);
            rowValues[columnIndex] = aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex >= colNames.size() - 2)
                return true;
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == colNames.size() - 2)
                return ModificationType.class;
            else if (columnIndex == colNames.size() - 1)
                return Double.class;
            else
                return super.getColumnClass(columnIndex);
        }
        
        @Override
        public String getColumnName(int column) {
            if (column >= colNames.size() - 2)
                return colNames.get(column);
            return super.getColumnName(column);
        }

        /**
         * The following map is hard-coded and should be improved in the future.
         * @param value
         * @return
         */
        private double getModificationStrenth(double value) {
            return affinityToModificationMap.getModificationStrenth(value);
//            if (value < 1) // Less than 1 nm
//                return 0.99d;
//            if (value < 10)
//                return 0.90d;
//            if (value < 100)
//                return 0.70;
//            if (value < 1000)
//                return 0.50;
//            if (value < 10000)
//                return 0.30;
//            if (value < 100000)
//                return 0.10;
//            return 0.0d;
        }
        
        private Double getMinValue(Object[] row) {
            Double rtn = null;
            for (int i = 4; i < colNames.size() - 2; i++) {
                Double value = (Double) row[i];
                if (value == null)
                    continue;
                if (rtn == null)
                    rtn = value;
                else if (rtn > value)
                    rtn = value;
            }
            return rtn;
        }
    }
    
    private class DrugSelectionDialog extends InteractionListView {
        private boolean isOKClicked;
        private JLabel titleLabel;
        
        public DrugSelectionDialog(JDialog owner) {
            super(owner);
        }
        
        @Override
        public void setInteractions(List<Interaction> interactions) {
            super.setInteractions(interactions);
            modifyTable();
        }

        @Override
        protected TableListInteractionFilter createInteractionFilter() {
            TableListInteractionFilter filter = super.createInteractionFilter();
            filter.resetAffinityFilterValues();
            return filter;
        }

        @Override
        protected JComponent createContentPane() {
            return super.createTablePane();
        }

        @Override
        protected JPanel createDialogControlPane() {
            JPanel controlPane = super.createTableControlPane();
            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dispose());
            controlPane.add(closeBtn);
            return controlPane;
        }

        @Override
        protected JPanel createTableControlPane() {
            return null;
        }

        @Override
        protected void init() {
            super.init();
            setTitle("Drug Selection");
            titleLabel = GKApplicationUtilities.createTitleLabel("Choose drugs by selecting:");
            titleLabel.setToolTipText("Note: Selecting one row will select all rows for the selected drug.");
            getContentPane().add(titleLabel, BorderLayout.NORTH);
            // Since the column is dynamically generated, we need to call this later on.
//            modifyTable();
        }
        
        public Map<String, Double> getInhibition() {
            return getModification(ModificationType.Inhibition);
        }
        
        public Map<String, Double> getActivation() {
            return getModification(ModificationType.Activation);
        }
        
        private Map<String, Double> getModification(ModificationType type) {
            List<String> drugs = getSelectedDrugs();
            if (drugs.size() == 0)
                return new HashMap<>();
            Map<String, Double> targetToValue = new HashMap<>();
            DrugSelectionTableModel model = (DrugSelectionTableModel) interactionTable.getModel();
            int col = model.getColumnCount();
            for (int i = 0; i < model.getRowCount(); i++) {
                String drug = (String) model.getValueAt(i, 1);
                if (!drugs.contains(drug))
                    continue;
                ModificationType type1 = (ModificationType) model.getValueAt(i, col - 2);
                if (type1 != type)
                    continue;
                Double value = (Double) model.getValueAt(i, col - 1);
                if (value == null)
                    continue; // Don't want to have null
                String target = (String) model.getValueAt(i, 2);
                targetToValue.put(target, value);
            }
            return targetToValue;
        }
        
        public List<String> getSelectedDrugs() {
            List<String> rtn = new ArrayList<>();
            String title = titleLabel.getText().trim();
            int index = title.indexOf(":");
            String text = title.substring(index + 1).trim();
            if (text.length() == 0)
                return rtn;
            String[] tokens = text.split(", ");
            for (String token : tokens)
                rtn.add(token);
            return rtn;
        }

        private void modifyTable() {
            // Handle table editing
            DefaultTableCellRenderer defaultCellRenderer = new DefaultTableCellRenderer();
            int col = interactionTable.getColumnCount();
            TableColumn modificationCol = interactionTable.getColumnModel().getColumn(col - 2);
            JComboBox<ModificationType> modificationEditor = new JComboBox<>();
            for (ModificationType type : ModificationType.values())
                modificationEditor.addItem(type);
            modificationCol.setCellEditor(new DefaultCellEditor(modificationEditor));
            modificationCol.setCellRenderer(defaultCellRenderer);

            DefaultCellEditor numberEditor = new DefaultCellEditor(new JTextField()) {
                @Override
                public Object getCellEditorValue() {
                    String text = super.getCellEditorValue().toString();
                    return new Double(text);
                }
            };

            TableColumn initCol = interactionTable.getColumnModel().getColumn(col - 1);
            initCol.setCellEditor(numberEditor);
            initCol.setCellRenderer(defaultCellRenderer);

            // Sort based on the drug name
            List<SortKey> sortedKeys = new ArrayList<>();
            sortedKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
            interactionTable.getRowSorter().setSortKeys(sortedKeys);
        }
        
        @Override
        protected InteractionListTableModel createTableModel() {
            return new DrugSelectionTableModel();
        }
        
        @Override
        protected void handleTableSelection() {
            super.handleTableSelection();
            // Get selected drugs
            TableModel model = interactionTable.getModel();
            Set<String> drugs = new HashSet<>();
            if (interactionTable.getSelectedRowCount() > 0) {
                for (int viewRow : interactionTable.getSelectedRows()) {
                    int modelRow = interactionTable.convertRowIndexToModel(viewRow);
                    String drug = (String) model.getValueAt(modelRow, 1);
                    drugs.add(drug);
                }
            }
            List<String> drugList = new ArrayList<>(drugs);
            Collections.sort(drugList);
            String text = StringUtils.join(", ", drugList);
            String title = titleLabel.getText();
            int index = title.indexOf(":");
            title = title.substring(0, index + 1) + " " + text;
            titleLabel.setText(title);
        }
        
        @Override
        protected JButton createActionButton() {
            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    isOKClicked = true;
                    dispose();
                }
            });
            return okButton;
        }
        
    }
    
}
