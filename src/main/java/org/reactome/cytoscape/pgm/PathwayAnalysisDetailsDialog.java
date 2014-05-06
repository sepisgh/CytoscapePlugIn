/*
 * Created on Apr 1, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.math.MathException;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.IPACalculator;
import org.reactome.pgm.PGMVariable;
import org.reactome.r3.util.MathUtilities;

/**
 * This customized JDialog is used to show details information for IPA values.
 * Boxplots are used to show distributions of IPA values for real samples and random samples.
 * P-values are calculated based on Mann-Whitney U test though TTest has been used originally
 * (see many TTest labeled code).
 * @author gwu
 *
 */
public class PathwayAnalysisDetailsDialog extends JDialog {
    private DefaultBoxAndWhiskerCategoryDataset dataset;
    private CategoryPlot plot;
    private ChartPanel chartPanel;
    private JTable tTestResultTable;
    // For node selection sync between table and network view
    private boolean isFromTable;
    private boolean isFromNetwork;
    // For combined p-value
    private JLabel combinedPValueLabel;
    // Cache calculated IPA values for sorting purpose
    private Map<String, List<Double>> realSampleToIPAs;
    private Map<String, List<Double>> randomSampleToIPAs;
    // Used for selecting a node
    private CyNetworkView networkView;
    private ServiceRegistration networkSelectionRegistration;
    
    public PathwayAnalysisDetailsDialog(JFrame frame) {
        super(frame);
        init();
    }
    
    public void setNetworkView(CyNetworkView networkView) {
        this.networkView = networkView;
    }
    
    private void init() {
        setTitle("Output Analysis Results");
        JPanel boxPlotPane = createBoxPlotPane();
        JPanel ttestResultPane = createTTestResultTable();
        JPanel combinedPValuePane = createCombinedPValuePane();
        
        JPanel lowerPane = new JPanel();
        lowerPane.setLayout(new BorderLayout());
        lowerPane.add(ttestResultPane, BorderLayout.CENTER);
        lowerPane.add(combinedPValuePane, BorderLayout.SOUTH);
        
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        boxPlotPane,
                                        lowerPane);
        jsp.setDividerLocation(0.5d);
        jsp.setDividerLocation(300); // Need to set an integer. Otherwise, the plot is too narrow.
        boxPlotPane.setPreferredSize(new Dimension(800, 300));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        JPanel closePane = new JPanel();
        closePane.add(closeBtn);
        
        getContentPane().add(jsp, BorderLayout.CENTER);
        getContentPane().add(closePane, BorderLayout.SOUTH);
        
        realSampleToIPAs = new HashMap<String, List<Double>>();
        randomSampleToIPAs = new HashMap<String, List<Double>>();
        
        installListeners();
    }
    
    private void installListeners() {
        tTestResultTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            
            @Override
            public void sorterChanged(RowSorterEvent e) {
                rePlotData();
            }
        });
        // Use this simple method to make sure marker is syncrhonized between two views.
        TableAndPlotActionSynchronizer tpSyncHelper = new TableAndPlotActionSynchronizer(tTestResultTable, chartPanel);
        
        tTestResultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                doTableSelection();
            }
        });
        
        // Synchronize selection from network to pathway overview
        RowsSetListener selectionListener = new RowsSetListener() {
            
            @Override
            public void handleEvent(RowsSetEvent event) {
                if (!event.containsColumn(CyNetwork.SELECTED) || 
                        networkView == null ||
                        networkView.getModel() == null || 
                        networkView.getModel().getDefaultEdgeTable() == null ||
                        networkView.getModel().getDefaultNodeTable() == null) {
                    return;
                }
                List<CyNode> nodes = CyTableUtil.getNodesInState(networkView.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
                handleNetworkSelection(nodes);
            }
            
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        networkSelectionRegistration = context.registerService(RowsSetListener.class.getName(),
                                                               selectionListener, 
                                                               null);
        
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                // Unregistered registered service for easy GC.
                if (networkSelectionRegistration != null) {
                    networkSelectionRegistration.unregister();
                    networkSelectionRegistration = null; 
                }
            }
            
        });
    }
    
    private void handleNetworkSelection(List<CyNode> selectedNodes) {
        if (isFromTable)
            return;
        isFromNetwork = true;
        Set<String> rowKeys = new HashSet<String>();
        TableHelper helper = new TableHelper();
        for (CyNode node : selectedNodes) {
            String label = helper.getStoredNodeAttribute(networkView.getModel(),
                                                         node,
                                                         "nodeLabel",
                                                         String.class);
            rowKeys.add(label);
        }
        tTestResultTable.clearSelection();
        if (rowKeys.size() > 0) {
            // Find the row index in the table model
            TableModel model = tTestResultTable.getModel();
            int selected = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                String tmp = (String) model.getValueAt(i, 0);
                if (rowKeys.contains(tmp)) {
                    int viewIndex = tTestResultTable.convertRowIndexToView(i);
                    tTestResultTable.getSelectionModel().addSelectionInterval(viewIndex, viewIndex);
                    if (selected == -1)
                        selected = viewIndex;
                }
            }
            if (selected > -1) {
                Rectangle rect = tTestResultTable.getCellRect(selected, 0, false);
                tTestResultTable.scrollRectToVisible(rect);
            }
        }
        isFromNetwork = false;
    }
    
    private void doTableSelection() {
        if (networkView == null || isFromNetwork)
            return;
        isFromTable = true;
        // Get the selected variable labels
        Set<String> variables = new HashSet<String>();
        TTestTableModel model = (TTestTableModel) tTestResultTable.getModel();
        if (tTestResultTable.getSelectedRowCount() > 0) {
            for (int row : tTestResultTable.getSelectedRows()) {
                int modelIndex = tTestResultTable.convertRowIndexToModel(row);
                String variable = (String) model.getValueAt(modelIndex, 0);
                variables.add(variable);
            }
        }
        // Clear all selection
        TableHelper tableHelper = new TableHelper();
        CyNetwork network = networkView.getModel();
        int totalSelected = 0;
        for (View<CyNode> nodeView : networkView.getNodeViews()) {
            CyNode node = nodeView.getModel();
            Long nodeSUID = node.getSUID();
            String nodeLabel = tableHelper.getStoredNodeAttribute(network,
                                                                  node, 
                                                                  "nodeLabel", 
                                                                  String.class);
            boolean isSelected = variables.contains(nodeLabel);
            if (isSelected)
                totalSelected ++;
            tableHelper.setNodeSelected(network, 
                                        node,
                                        isSelected);
        }
        PlugInUtilities.zoomToSelected(networkView,
                                       totalSelected);
        networkView.updateView();
        isFromTable = false;
    }
    
    private void rePlotData() {
        dataset.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        for (int i = 0; i < tTestResultTable.getRowCount(); i++) {
            int index = tTestResultTable.convertRowIndexToModel(i);
            String varLabel = (String) tableModel.getValueAt(index, 0);
            List<Double> realIPAs = realSampleToIPAs.get(varLabel);
            dataset.add(realIPAs, "Real Samples", varLabel);
            List<Double> randomIPAs = randomSampleToIPAs.get(varLabel);
            dataset.add(randomIPAs, "Random Samples", varLabel);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
    }
    
    private JPanel createTTestResultTable() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel label = new JLabel("Mean Value Comparison Results");
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2.0f));
        // Want to make sure label is in the middle, so
        // add a panel
        JPanel labelPane = new JPanel();
        labelPane.add(label);
        panel.add(labelPane, BorderLayout.NORTH);
        
        TTestTableModel model = new TTestTableModel();
        tTestResultTable = new JTable(model);
        // Need to add a row sorter
        TableRowSorter<TTestTableModel> sorter = new TableRowSorter<TTestTableModel>(model) {

            @Override
            public Comparator<?> getComparator(int column) {
                if (column == 0) // Just use the String comparator.
                    return super.getComparator(0);
                Comparator<String> rtn = new Comparator<String>() {
                    public int compare(String var1, String var2) {
                        Double value1 = new Double(var1);
                        Double value2 = new Double(var2);
                        return value1.compareTo(value2);
                    }
                };
                return rtn;
            }
            
        };
        tTestResultTable.setRowSorter(sorter);
        
        panel.add(new JScrollPane(tTestResultTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCombinedPValuePane() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JLabel titleLabel = new JLabel("Combined p-value using an extension of Fisher's method (click to see the reference): ");
        titleLabel.setToolTipText("Click to view the reference");
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combinedPValueLabel = new JLabel("1.0");
        titleLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                String url = "http://www.sciencedirect.com/science/article/pii/S0167715202003103";
                PlugInUtilities.openURL(url);
            }
            
        });
        panel.add(titleLabel);
        panel.add(combinedPValueLabel);
        
        return panel;
    }
    
    private JPanel createBoxPlotPane() {
        dataset = new DefaultBoxAndWhiskerCategoryDataset();
        // Want to control data update by this object self to avoid
        // conflict exception.
        dataset.setNotify(false);
        CategoryAxis xAxis = new CategoryAxis("Output Variable");
        NumberAxis yAxis = new NumberAxis("IPA");
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        // We want to show the variable label
        BoxAndWhiskerToolTipGenerator tooltipGenerator = new BoxAndWhiskerToolTipGenerator() {

            @Override
            public String generateToolTip(CategoryDataset dataset,
                                          int row,
                                          int column) {
                Object variable = dataset.getColumnKey(column);
                String rtn = super.generateToolTip(dataset, row, column);
                return "Variable: " + variable + " " + rtn;
            }
        };
        renderer.setBaseToolTipGenerator(tooltipGenerator);
        plot = new CategoryPlot(dataset,
                                xAxis, 
                                yAxis,
                                renderer);
        JFreeChart chart = new JFreeChart("Boxplot for Integrated Pathway Activities (IPAs) of Outputs", 
                                          plot);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEtchedBorder());
        return chartPanel;
    }
    
    public void setVariables(List<PGMVariable> variables) throws MathException {
        // Do a sort
        List<PGMVariable> sortedVars = new ArrayList<PGMVariable>(variables);
        Collections.sort(sortedVars, new Comparator<PGMVariable>() {
            public int compare(PGMVariable var1, PGMVariable var2) {
                return var1.getLabel().compareTo(var2.getLabel());
            }
        });
        dataset.clear();
        realSampleToIPAs.clear();
        randomSampleToIPAs.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        List<Double> pvalues = new ArrayList<Double>();
        for (PGMVariable var : sortedVars) {
            Map<String, List<Double>> sampleToProbs = var.getPosteriorValues();
            List<Double> realIPAs = addValueToDataset(sampleToProbs, 
                                                      "Real Samples",
                                                      var);
            List<Double> randomIPAs = addValueToDataset(var.getRandomPosteriorValues(),
                                                        "Random Samples",
                                                        var);
            double pvalue = tableModel.addRow(realIPAs, 
                                              randomIPAs,
                                              var.getLabel());
            pvalues.add(pvalue);
            realSampleToIPAs.put(var.getLabel(), realIPAs);
            randomSampleToIPAs.put(var.getLabel(), randomIPAs);
        }
        // The following code is used to control performance:
        // 16 is arbitrary
        CategoryAxis axis = plot.getDomainAxis();
        if (variables.size() > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
            axis.setTickLabelsVisible(false);
            axis.setTickMarksVisible(false);
        }
        else {
            axis.setTickLabelsVisible(true);
            axis.setTickMarksVisible(true);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
        // Make a copy to avoid modifying by the called method
        tableModel.calculateFDRs(new ArrayList<Double>(pvalues));
        tableModel.fireTableStructureChanged();
        setCombinedPValue(pvalues);
    }
    
    /**
     * Calculate combined p-value from a list of p-values using Fisher's method and display
     * it in a label.
     * @param pvalues
     * @throws MathException
     */
    private void setCombinedPValue(List<Double> pvalues) throws MathException {
//        double combinedPValue = MathUtilities.combinePValuesWithFisherMethod(pvalues);
        // Since it is pretty sure, variables are dependent in pathway, use another method
        // to combine p-values
        PValueCombiner combiner = new PValueCombiner();
        double combinedPValue = combiner.combinePValue(new ArrayList<List<Double>>(realSampleToIPAs.values()),
                                                        pvalues);
        combinedPValueLabel.setText(PlugInUtilities.formatProbability(combinedPValue));
    }
    
    private List<Double> addValueToDataset(Map<String, List<Double>> sampleToProbs,
                                           String label,
                                           PGMVariable var) {
        List<Double> ipas = new ArrayList<Double>();
        for (List<Double> probs : sampleToProbs.values()) {
            double ipa = IPACalculator.calculateIPA(var.getValues(), probs);
            ipas.add(ipa);
        }
        dataset.add(ipas, label, var.getLabel());
        return ipas;
    }
    
    class TTestTableModel extends AbstractTableModel {
        private List<String> colHeaders;
        private List<String[]> data;
        
        public TTestTableModel() {
            String[] headers = new String[]{
                    "Variable",
                    "RealMean",
                    "RandomMean",
                    "MeanDiff",
//                    "t-statistic",
                    "p-value",
                    "FDR"
            };
            colHeaders = Arrays.asList(headers);
            data = new ArrayList<String[]>();
        }
        
        /**
         * Add a new column to the table model. P-value will be returned from this method.
         * @param realIPAs
         * @param randomIPAs
         * @param varLabel
         * @return
         * @throws MathException
         */
        public double addRow(List<Double> realIPAs,
                             List<Double> randomIPAs,
                             String varLabel) throws MathException {
            double realMean = MathUtilities.calculateMean(realIPAs);
            double randomMean = MathUtilities.calculateMean(randomIPAs);
            double diff = realMean - randomMean;
            // Need a double array
            double[] realArray = new double[realIPAs.size()];
            for (int i = 0; i < realIPAs.size(); i++)
                realArray[i] = realIPAs.get(i);
            double[] randomArray = new double[randomIPAs.size()];
            for (int i = 0; i < randomIPAs.size(); i++)
                randomArray[i] = randomIPAs.get(i);
//            double t = TestUtils.t(realArray,
//                                   randomArray);
//            double pvalue = TestUtils.tTest(realArray,
//                                            randomArray);
            double pvalue = new MannWhitneyUTest().mannWhitneyUTest(realArray, randomArray);
            
            String[] row = new String[colHeaders.size()];
            row[0] = varLabel;
            row[1] = PlugInUtilities.formatProbability(realMean);
            row[2] = PlugInUtilities.formatProbability(randomMean);
            row[3] = PlugInUtilities.formatProbability(diff);
//            row[4] = PlugInUtilities.formatProbability(t);
            row[4] = PlugInUtilities.formatProbability(pvalue);
            
            data.add(row);
            
            return pvalue;
        }
        
        /**
         * A method to calcualte FDRs. The order in the passed List should be the same
         * as p-values stored in the data object. Otherwise, the calculated FDRs assignment
         * will be wrong.
         * @param pvalues
         */
        void calculateFDRs(List<Double> pvalues) {
            if (data.size() != pvalues.size())
                throw new IllegalArgumentException("Passed pvalues list has different size to the stored table data.");
            List<String[]> pvalueSortedList = new ArrayList<String[]>(data);
            // Just copy pvalues into rowdata for the time being
            for (int i = 0; i < pvalueSortedList.size(); i++) {
                Double pvalue = pvalues.get(i);
                String[] rowData = pvalueSortedList.get(i);
                rowData[5] = pvalue + "";
            }
            Collections.sort(pvalueSortedList, new Comparator<String[]>() {
                public int compare(String[] row1, String[] row2) {
                    Double pvalue1 = new Double(row1[5]);
                    Double pvalue2 = new Double(row2[5]);
                    return pvalue1.compareTo(pvalue2);
                }
            });
            // pvalues will be sorted 
            List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
            // Modify pvalues into FDRs for the last column. Since the same String[] objects are
            // used in the sorted list and the original data, there is no need to do anything for
            // table display purpose.
            for (int i = 0; i < pvalueSortedList.size(); i++) {
                String[] rowData = pvalueSortedList.get(i);
                rowData[5] = PlugInUtilities.formatProbability(fdrs.get(i));
            }
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return colHeaders.size(); 
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String[] row = data.get(rowIndex);
            return row[columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return colHeaders.get(column);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
}