package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.ServiceProperties;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cancerindex.model.DiseaseData;
import org.reactome.cytoscape.drug.DrugDataSource;
import org.reactome.cytoscape.drug.NetworkDrugManager;
import org.reactome.cytoscape.service.AbstractPopupMenuHandler;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableFormatterImpl;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape3.EdgeActionCollection.DrugTargetDetailsMenuItem;
import org.reactome.cytoscape3.EdgeActionCollection.EdgeLoadMechsimoMenuItem;
import org.reactome.cytoscape3.EdgeActionCollection.EdgeQueryFIMenuItem;
import org.reactome.cytoscape3.NodeActionCollection.CancerGeneIndexMenu;
import org.reactome.cytoscape3.NodeActionCollection.CosmicMenu;
import org.reactome.cytoscape3.NodeActionCollection.FetchFIsMenu;
import org.reactome.cytoscape3.NodeActionCollection.GeneCardMenu;
import org.reactome.cytoscape3.NodeActionCollection.GoogleMenu;

/**
 * This class is used to generate popup menus for a FI network.
 * 
 * @author Eric T. Dawson & Guanming Wu
 */
public class FINetworkPopupMenuHandler extends AbstractPopupMenuHandler {
    protected Map<CyNetworkViewContextMenuFactory, ServiceRegistration> menuToRegistration;
    
    public FINetworkPopupMenuHandler() {
        menuToRegistration = new HashMap<CyNetworkViewContextMenuFactory, ServiceRegistration>();
        // // Add a listener for NewtorkView selection
        // SetCurrentNetworkViewListener currentNetworkViewListener = new
        // SetCurrentNetworkViewListener() {
        //
        // @Override
        // public void handleEvent(SetCurrentNetworkViewEvent event) {
        // if (event.getNetworkView() == null)
        // return; // This is more like a Pathway view
        // CyNetwork network = event.getNetworkView().getModel();
        // // Check if this network is a converted
        // CyRow row =
        // network.getDefaultNetworkTable().getRow(network.getSUID());
        // String dataSetType = row.get("dataSetType",
        // String.class);
        // if ("PathwayDiagram".equals(dataSetType)) {
        // // Check the ReactomeNetworkType
        // ReactomeNetworkType type = new
        // TableHelper().getReactomeNetworkType(network);
        // if (type == ReactomeNetworkType.FINetwork) {
        // installConvertToDiagramMenu();
        // }
        // else if (type == ReactomeNetworkType.FactorGraph) {
        // uninstallDynamicMenu(networkToDiagramMenu);
        // }
        // }
        // else {
        // uninstallDynamicMenu(networkToDiagramMenu);
        // }
        // }
        // };
        // BundleContext context =
        // PlugInObjectManager.getManager().getBundleContext();
        // context.registerService(SetCurrentNetworkViewListener.class.getName(),
        // currentNetworkViewListener,
        // null);
    }
    
    protected <T> void addPopupMenu(BundleContext context, T menuFactory, Class<T> cls, Properties properties) {
        ServiceRegistration registration = context.registerService(cls.getName(), menuFactory, properties);
        menuRegistrations.add(registration);
    }
    
    @Override
    protected void installMenus() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        // Instantiate and register the context menus for the network view
        ClusterFINetworkMenu clusterMenu = new ClusterFINetworkMenu();
        Properties clusterProps = new Properties();
        clusterProps.setProperty(ServiceProperties.TITLE, "Cluster FI Network");
        clusterProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, clusterMenu, CyNetworkViewContextMenuFactory.class, clusterProps);
        
        NetworkPathwayEnrichmentMenu netPathMenu = new NetworkPathwayEnrichmentMenu();
        Properties netPathProps = new Properties();
        netPathProps.setProperty(ServiceProperties.TITLE, "Network Pathway Enrichment");
        String preferredMenuText = PREFERRED_MENU + ".Analyze Network Functions[10]";
        netPathProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, netPathMenu, CyNetworkViewContextMenuFactory.class, netPathProps);
        
        NetworkGOCellComponentMenu netGOCellMenu = new NetworkGOCellComponentMenu();
        Properties netGOCellProps = new Properties();
        netGOCellProps.setProperty(ServiceProperties.TITLE, "Network GO Cellular Component");
        netGOCellProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, netGOCellMenu, CyNetworkViewContextMenuFactory.class, netGOCellProps);
        
        NetworkGOBioProcessMenu netGOBioMenu = new NetworkGOBioProcessMenu();
        Properties netGOBioProps = new Properties();
        netGOBioProps.setProperty(ServiceProperties.TITLE, "Network GO Biological Process");
        netGOBioProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, netGOBioMenu, CyNetworkViewContextMenuFactory.class, netGOBioProps);
        
        NetworkGOMolecularFunctionMenu netGOMolMenu = new NetworkGOMolecularFunctionMenu();
        Properties netGOMolProps = new Properties();
        netGOMolProps.setProperty(ServiceProperties.TITLE, "Network GO Molecular Function");
        netGOMolProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, netGOMolMenu, CyNetworkViewContextMenuFactory.class, netGOMolProps);
        
        ModulePathwayEnrichmentMenu modPathMenu = new ModulePathwayEnrichmentMenu();
        Properties modPathProps = new Properties();
        preferredMenuText = PREFERRED_MENU + ".Analyze Module Functions[30]";
        modPathProps.setProperty(ServiceProperties.TITLE, "Module Pathway Enrichment");
        modPathProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, modPathMenu, CyNetworkViewContextMenuFactory.class, modPathProps);
        
        ModuleGOCellComponentMenu modCellMenu = new ModuleGOCellComponentMenu();
        Properties modCellProps = new Properties();
        modCellProps.setProperty(ServiceProperties.TITLE, "Module GO Cellular Component");
        modCellProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, modCellMenu, CyNetworkViewContextMenuFactory.class, modCellProps);
        
        ModuleGOBioProcessMenu modBioMenu = new ModuleGOBioProcessMenu();
        Properties modBioProps = new Properties();
        modBioProps.setProperty(ServiceProperties.TITLE, "Module GO Biological Process");
        modBioProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, modBioMenu, CyNetworkViewContextMenuFactory.class, modBioProps);
        
        ModuleGOMolecularFunctionMenu modMolMenu = new ModuleGOMolecularFunctionMenu();
        Properties modMolProps = new Properties();
        modMolProps.setProperty(ServiceProperties.TITLE, "Module GO Molecular Function");
        modMolProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, modMolMenu, CyNetworkViewContextMenuFactory.class, modMolProps);
        
        SurvivalAnalysisMenu survivalMenu = new SurvivalAnalysisMenu();
        Properties survivalMenuProps = new Properties();
        survivalMenuProps.setProperty(ServiceProperties.TITLE, "Survival Analysis");
        survivalMenuProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, survivalMenu, CyNetworkViewContextMenuFactory.class, survivalMenuProps);
        
        LoadCancerGeneIndexForNetwork fetchCGINetwork = new LoadCancerGeneIndexForNetwork();
        Properties fetchCGINetprops = new Properties();
        fetchCGINetprops.setProperty(ServiceProperties.TITLE, "Fetch Cancer Gene Index[40]");
        fetchCGINetprops.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, fetchCGINetwork, CyNetworkViewContextMenuFactory.class, fetchCGINetprops);
        
        // Cancer drugs overlay feature
        if (PlugInObjectManager.getManager().isCancerTargetEnabled()) {
            FetchCancerDrugMenu fetchCancerDrugMenu = new FetchCancerDrugMenu();
            fetchCancerDrugMenu.dataSource = DrugDataSource.Targetome;
            Properties cancerProperties = new Properties();
            preferredMenuText = PREFERRED_MENU + ".Overlay Drugs[50]";
            cancerProperties.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
            addPopupMenu(context, fetchCancerDrugMenu, CyNetworkViewContextMenuFactory.class, cancerProperties);
            
            FetchCancerDrugMenu fetchDrugMenu = new FetchCancerDrugMenu();
            fetchDrugMenu.dataSource = DrugDataSource.DrugCentral;
            cancerProperties = new Properties();
            cancerProperties.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
            addPopupMenu(context, fetchDrugMenu, CyNetworkViewContextMenuFactory.class, cancerProperties);
            
            FilterCancerDrugMenu filterCancerDrugMenu = new FilterCancerDrugMenu();
            cancerProperties = new Properties();
            cancerProperties.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
            addPopupMenu(context, filterCancerDrugMenu, CyNetworkViewContextMenuFactory.class, cancerProperties);
            
            RemoveCancerDrugMenu removeCancerDrugMenu = new RemoveCancerDrugMenu();
            cancerProperties = new Properties();
            cancerProperties.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
            addPopupMenu(context, removeCancerDrugMenu, CyNetworkViewContextMenuFactory.class, cancerProperties);
        }
        
        // Instantiate and register the context menus for the node views
        GeneCardMenu geneCardMenu = new NodeActionCollection.GeneCardMenu();
        Properties geneCardProps = new Properties();
        geneCardProps.setProperty(ServiceProperties.TITLE, "Gene Card");
        geneCardProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, geneCardMenu, CyNodeViewContextMenuFactory.class, geneCardProps);
        
        GoogleMenu googleMenu = new GoogleMenu();
        Properties googleMenuProps = new Properties();
        googleMenuProps.setProperty(ServiceProperties.TITLE, "Google");
        googleMenuProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, googleMenu, CyNodeViewContextMenuFactory.class, googleMenuProps);
        
        CosmicMenu cosmicMenu = new NodeActionCollection.CosmicMenu();
        Properties cosmicProps = new Properties();
        cosmicProps.setProperty(ServiceProperties.TITLE, "Cosmic");
        cosmicProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, cosmicMenu, CyNodeViewContextMenuFactory.class, geneCardProps);
        
        CancerGeneIndexMenu cgiMenu = new NodeActionCollection.CancerGeneIndexMenu();
        Properties cgiMenuProps = new Properties();
        cgiMenuProps.setProperty(ServiceProperties.TITLE, "Fetch Cancer Gene Index");
        cgiMenuProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, cgiMenu, CyNodeViewContextMenuFactory.class, cgiMenuProps);
        
        FetchFIsMenu fetchFIs = new NodeActionCollection.FetchFIsMenu();
        Properties fetchFIsProps = new Properties();
        fetchFIsProps.setProperty(ServiceProperties.TITLE, "Fetch FIs");
        fetchFIsProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, fetchFIs, CyNodeViewContextMenuFactory.class, fetchFIsProps);
        
        // For nodes functional annotations
        preferredMenuText = PREFERRED_MENU + ".Analyze Nodes Functions[10]";
        String[] titles = new String[] {
                "Pathway Enrichment",
                "GO Cellular Component",
                "GO Biological Process",
                "GO Molecular Function"
        };
        CyNodeViewContextMenuFactory[] menus = new CyNodeViewContextMenuFactory[] {
                new NodesPathwayEnrichmentMenu(),
                new NodesGOCellComponentMenu(),
                new NodesGOBioprocessMenu(),
                new NodesGOMFMenu()
        };
        for (int i = 0; i < titles.length; i++) {
            Properties properties = new Properties();
            properties.setProperty(ServiceProperties.TITLE, 
                                          titles[i]);
            properties.setProperty(ServiceProperties.PREFERRED_MENU,
                                          preferredMenuText);
            addPopupMenu(context, 
                         menus[i], 
                         CyNodeViewContextMenuFactory.class,
                         properties);
        }
        
        // Instantiate and register the context menus for edge views
        EdgeQueryFIMenuItem edgeQueryMenu = new EdgeActionCollection.EdgeQueryFIMenuItem();
        Properties edgeMenuProps = new Properties();
        edgeMenuProps.setProperty(ServiceProperties.TITLE, "Query FI Source");
        edgeMenuProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, edgeQueryMenu, CyEdgeViewContextMenuFactory.class, edgeMenuProps);
        
        EdgeLoadMechsimoMenuItem mechimoMenu = new EdgeActionCollection.EdgeLoadMechsimoMenuItem();
        edgeMenuProps = new Properties();
        edgeMenuProps.setProperty(ServiceProperties.TITLE, "Fetch Mechismo Results");
        edgeMenuProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, mechimoMenu, CyEdgeViewContextMenuFactory.class, edgeMenuProps);
        
        DrugTargetDetailsMenuItem drugTargetDetailsMenu = new EdgeActionCollection.DrugTargetDetailsMenuItem();
        edgeMenuProps = new Properties();
        edgeMenuProps.setProperty(ServiceProperties.TITLE, "Show Details");
        edgeMenuProps.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, drugTargetDetailsMenu, CyEdgeViewContextMenuFactory.class, edgeMenuProps);
    }
    
    protected void installOtherNetworkMenu(CyNetworkViewContextMenuFactory menu, String title) {
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration != null)
            return; // This menu has been installed.
        Properties props = new Properties();
        props.setProperty(ServiceProperties.TITLE, title);
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        addPopupMenu(context, menu, CyNetworkViewContextMenuFactory.class, props);
    }
    
    /**
     * A class for the network view right-click menu item which clusters the
     * network and a corresponding task/factory.
     * 
     * @author Eric T. Dawson
     */
    private class ClusterFINetworkMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem clusterMenuItem = new JMenuItem("Cluster FI Network");
            clusterMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Thread t = new Thread() {
                        public void run() {
                            clusterFINetwork(view);
                        }
                    };
                    t.start();
                }
            });
            
            return new CyMenuItem(clusterMenuItem, 20.0f);
        }
    }
    
    private class NodesPathwayEnrichmentMenu implements CyNodeViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            if (!NodeActionCollection.isGeneNode(netView.getModel(),
                                                 nodeView.getModel()))
                return null;
            JMenuItem netPathMenuItem = new JMenuItem("Pathway Enrichment");
            netPathMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(netView, "Pathway");
                }
            });
            return new CyMenuItem(netPathMenuItem, 2.0f);
        }
    }
    
    protected class NetworkPathwayEnrichmentMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem netPathMenuItem = new JMenuItem("Pathway Enrichment");
            netPathMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(view, "Pathway");
                }
            });
            return new CyMenuItem(netPathMenuItem, 2.0f);
        }
        
    }
    
    private class NodesGOCellComponentMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            if (!NodeActionCollection.isGeneNode(netView.getModel(),
                                                 nodeView.getModel()))
                return null;
            JMenuItem netGOCellComponentMenuItem = new JMenuItem("GO Cell Component");
            netGOCellComponentMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(netView,
                                    "CC");
                }
                
            });
            return new CyMenuItem(netGOCellComponentMenuItem, 3.0f);
        }
    }
    
    private class NetworkGOCellComponentMenu implements CyNetworkViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem netGOCellComponentMenuItem = new JMenuItem("GO Cell Component");
            netGOCellComponentMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(view, "CC");
                }
                
            });
            return new CyMenuItem(netGOCellComponentMenuItem, 3.0f);
            
        }
    }
    
    private class NodesGOBioprocessMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            if (!NodeActionCollection.isGeneNode(netView.getModel(),
                                                 nodeView.getModel()))
                return null;
            JMenuItem netGOBioMenuItem = new JMenuItem("GO Biological Process");
            netGOBioMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(netView, "BP");
                }
                
            });
            return new CyMenuItem(netGOBioMenuItem, 4.0f);
        }
        
    }
    
    private class NetworkGOBioProcessMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem netGOBioMenuItem = new JMenuItem("GO Biological Process");
            netGOBioMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(view, "BP");
                }
                
            });
            return new CyMenuItem(netGOBioMenuItem, 4.0f);
        }
        
    }
    
    private class NodesGOMFMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            if (!NodeActionCollection.isGeneNode(netView.getModel(),
                                                 nodeView.getModel()))
                return null;
            JMenuItem netGOMolFuncMenuItem = new JMenuItem("GO Molecular Function");
            netGOMolFuncMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(netView, "MF");
                }
                
            });
            return new CyMenuItem(netGOMolFuncMenuItem, 5.0f);
        }
        
    }
    
    private class NetworkGOMolecularFunctionMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem netGOMolFuncMenuItem = new JMenuItem("GO Molecular Function");
            netGOMolFuncMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetwork(view, "MF");
                }
                
            });
            return new CyMenuItem(netGOMolFuncMenuItem, 5.0f);
        }
        
    }
    
    private class ModulePathwayEnrichmentMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem modPathMenuItem = new JMenuItem("Pathway Enrichment");
            modPathMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetworkModules(view, "Pathway");
                }
                
            });
            return new CyMenuItem(modPathMenuItem, 6.0f);
        }
        
    }
    
    private class ModuleGOCellComponentMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem modGOCellMenuItem = new JMenuItem("GO Cell Component");
            modGOCellMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetworkModules(view, "CC");
                }
                
            });
            return new CyMenuItem(modGOCellMenuItem, 7.0f);
        }
        
    }
    
    /**
     * A class for showing the gene ontology biological processes of a given
     * module using the network view context menu item.
     * 
     * @author Eric T. Dawson
     */
    private class ModuleGOBioProcessMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem modGOBioProcessMenuItem = new JMenuItem("GO Biological Process");
            modGOBioProcessMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetworkModules(view, "BP");
                }
                
            });
            return new CyMenuItem(modGOBioProcessMenuItem, 8.0f);
        }
        
    }
    
    /**
     * A class for showing the gene ontology molecular functions of a given
     * module using the given network view context menu item.
     * 
     * @author Eric T. Dawson
     */
    private class ModuleGOMolecularFunctionMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem modGOMolFuncMenuItem = new JMenuItem("GO Molecular Function");
            modGOMolFuncMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    annotateNetworkModules(view, "MF");
                }
                
            });
            return new CyMenuItem(modGOMolFuncMenuItem, 9.0f);
        }
        
    }
    
    /**
     * A class for performing survival analysis using the network view context
     * menu item.
     * 
     * @author Eric T. Dawson
     */
    private class SurvivalAnalysisMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem survivalAnalysisMenuItem = new JMenuItem("Survival Analysis");
            survivalAnalysisMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    doModuleSurvivalAnalysis(view);
                }
            });
            return new CyMenuItem(survivalAnalysisMenuItem, 10.f);
        }
    }
    
    private class FetchCancerDrugMenu implements CyNetworkViewContextMenuFactory {
        DrugDataSource dataSource;

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            String title = null;
            float weight = 1.0f;
            if (dataSource == DrugDataSource.Targetome)
                title = "Fetch Cancer Drugs";
            else {
                title = "Fetch DrugCentral Drugs";
                weight = 2.0f;
            }
            JMenuItem fetchCancerDrugMenuItem = new JMenuItem(title);
            fetchCancerDrugMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    fetchCancerDrugs(netView);
                }
            });
            return new CyMenuItem(fetchCancerDrugMenuItem, weight);
        }
        
        private void fetchCancerDrugs(final CyNetworkView networkView) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    ProgressPane progPane = new ProgressPane();
                    progPane.setIndeterminate(true);
                    progPane.setText("Fetching drugs...");
                    JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                    frame.setGlassPane(progPane);
                    frame.getGlassPane().setVisible(true);
                    try {
                        NetworkDrugManager.getManager().fetchCancerDrugs(networkView, dataSource);
                    }
                    catch (Exception e) {
                        PlugInUtilities.showErrorMessage("Error in Fetching Drugs",
                                                         "Cannot fetch drugs:\n" + e.getMessage());
                    }
                    progPane.setIndeterminate(false);
                    frame.getGlassPane().setVisible(false);
                }
            };
            t.start();
        }
        
    }
    
    private class FilterCancerDrugMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            JMenuItem filterCancerDrugMenuItem = new JMenuItem("Filter Drugs");
            filterCancerDrugMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetworkDrugManager.getManager().filterCancerDrugs(netView);
                }
            });
            return new CyMenuItem(filterCancerDrugMenuItem, 5.0f);
        }
        
    }
    
    private class RemoveCancerDrugMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            JMenuItem removeCancerDrugMenuItem = new JMenuItem("Remove Drugs");
            removeCancerDrugMenuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    NetworkDrugManager.getManager().removeCancerDrugs(netView);
                }
            });
            return new CyMenuItem(removeCancerDrugMenuItem, 10.0f);
        }
        
    }
    
    private class LoadCancerGeneIndexForNetwork implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem loadCGIItem = new JMenuItem("Load Cancer Gene Index");
            loadCGIItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fetchNetworkCGI(view);
                }
            });
            return new CyMenuItem(loadCGIItem, 40.0f);
        }
    }
    
    private void fetchNetworkCGI(final CyNetworkView view) {
        Thread t = new Thread() {
            @Override
            public void run() {
                ProgressPane progPane = new ProgressPane();
                progPane.setIndeterminate(true);
                progPane.setText("Fetching cancer gene index annotations...");
                FIPlugInHelper r = FIPlugInHelper.getHelper();
                JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                frame.setGlassPane(progPane);
                progPane.setVisible(true);
                try {
                    NCICancerIndexDiseaseHelper diseaseHelper = new NCICancerIndexDiseaseHelper(view);
                    if (!diseaseHelper.areDiseasesShown()) {
                        progPane.setText("Loading NCI disease terms...");
                        Map<String, DiseaseData> codeToDisease = diseaseHelper.fetchDiseases();
                        diseaseHelper.displayDiseases(codeToDisease);
                    }
                    progPane.setText("Querying gene to diseases mapping...");
                    Set<String> genes = getGenesInNetwork(view);
                    RESTFulFIService service = new RESTFulFIService();
                    Map<String, String> geneToDiseases = service.queryGeneToDisease(genes);
                    TableHelper tableHelper = new TableHelper();
                    CyTable nodeTable = view.getModel().getDefaultNodeTable();
                    if (view.getModel().getDefaultNodeTable().getColumn("diseases") == null) {
                        tableHelper.createNewColumn(nodeTable, "diseases", String.class);
                    }
                    tableHelper.storeNodeAttributesByName(view.getModel(), "diseases", geneToDiseases);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    PlugInUtilities.showErrorMessage("Error in Loading CGI",
                                                     "The network's cancer gene index could not be loaded.");
                }
                progPane.setIndeterminate(false);
                progPane.setVisible(false);
                frame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    private Set<String> getGenesInNetwork(CyNetworkView view) {
        Set<String> genes = new HashSet<String>();
        CyTable netTable = view.getModel().getDefaultNodeTable();
        for (CyNode node : view.getModel().getNodeList()) {
            Long nodeSUID = node.getSUID();
            String name = netTable.getRow(nodeSUID).get("name", String.class);
            genes.add(name);
        }
        return genes;
    }
    
    private void doModuleSurvivalAnalysis(CyNetworkView view) {
        NetworkModuleHelper helper = new NetworkModuleHelper();
        Map<String, Integer> nodeToModule = helper.extractNodeToModule(view);
        if (nodeToModule == null || nodeToModule.isEmpty())
            return; // There is no data for analysis
            
        TableHelper tableHelper = new TableHelper();
        String dataType = tableHelper.getDataSetType(view);
        if (dataType == null)
            return; // The type of data is unknown so nothing can be done.
        try {
            if (dataType.equals(TableFormatterImpl.getSampleMutationData())) {
                Map<String, Object> nodeToSamples = tableHelper.getNodeTableValuesByName(view.getModel(), "samples",
                                                                                         String.class);
                if (nodeToSamples == null || nodeToSamples.isEmpty()) {
                    PlugInUtilities
                            .showErrorMessage("No Sample Information",
                                              "Survival Analysis can not be performed because no sample information exists.");
                    return;
                }
                Map<String, Set<String>> nodeToSampleSet = PlugInUtilities.extractNodeToSampleSet(nodeToSamples);
                ModuleBasedSurvivalAnalysisHelper survivalHelper = new ModuleBasedSurvivalAnalysisHelper();
                survivalHelper.doSurvivalAnalysis(nodeToModule, nodeToSampleSet);
            }
            else if (dataType.equals(TableFormatterImpl.getMCLArrayClustering())) {
                Map<Integer, Map<String, Double>> moduleToSampleToValue = FIPlugInHelper.getHelper()
                        .getMCLModuleToSampleToValue();
                if (moduleToSampleToValue == null || moduleToSampleToValue.size() == 0) {
                    PlugInUtilities
                            .showErrorMessage("No Sample Information",
                                              "No sample information has been provided. Survival analysis cannot be done.");
                    return;
                }
                ModuleBasedSurvivalAnalysisHelper survivalHelper = new ModuleBasedSurvivalAnalysisHelper();
                survivalHelper.doSurvivalAnalysisForMCLModules(nodeToModule, moduleToSampleToValue);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            PlugInUtilities.showErrorMessage("Error During Survival Analysis",
                                             "Survival Analysis could not be performed.\n Please see the logs.");
        }
    }
    
    /**
     * This method is called by a wrapped thread in the menu item for doing FI
     * network clustering.
     */
    private void clusterFINetwork(CyNetworkView view) {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        String clustering = netTable.getRow(view.getModel().getSUID()).get("clustering_Type", String.class);
        if (clustering != null && !(clustering.length() <= 0)
                && !clustering.equals(TableFormatterImpl.getSpectralPartitionCluster())) {
            CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
            int reply = JOptionPane.showConfirmDialog(frame,
                                                      "The displayed network has been clustered before using a different algorithm.\n"
                                                              + "You may get different clustering results using this clustering feature. Do\n"
                                                              + "you want to continue?",
                                                      "Clustering Algorithm Warning", JOptionPane.OK_CANCEL_OPTION);
                                                      
            if (reply != JOptionPane.OK_OPTION) {
                return;
            }
        }
        
        clusterFINetwork(frame, view);
    }
    
    /**
     * The actual place for doing network clustering.
     */
    private void clusterFINetwork(JFrame frame, CyNetworkView view) {
        // Since the implementation of task has its GUI handling, we cannot use
        // TaskManager to avoid thread and GUI issue
        final ClusterFINetworkTask task = new ClusterFINetworkTask(view, frame);
        Thread t = new Thread() {
            public void run() {
                task.clusterFINetwork();
            }
        };
        t.start();
    }
    
    /**
     * Annotate network. If there are some nodes selected, the annotation will
     * be performed for the selected nodes.
     * 
     * @param view
     * @param type
     */
    private void annotateNetwork(final CyNetworkView view, final String type) {
        final Set<String> genes = new HashSet<String>();
        // Check if linkers were used.
        Set<String> linkers = new HashSet<String>();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        // Check if there is any node selected
        List<CyNode> nodeList = CyTableUtil.getNodesInState(view.getModel(), CyNetwork.SELECTED, true);
        if (nodeList == null || nodeList.size() == 0)
            nodeList = view.getModel().getNodeList(); // Otherwise, we choose
                                                      // annotation for the
                                                      // whole network
        for (CyNode node : nodeList) {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
            genes.add(nodeName);
            Boolean isLinker = nodeTable.getRow(nodeSUID).get("isLinker", Boolean.class);
            if (isLinker != null && isLinker) {
                linkers.add(nodeName);
            }
        }
        if (!linkers.isEmpty()) {
            int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                      "Linkers have been used in network construction. Including linkers will\n"
                                                     + "bias results. Would you like to include them anyway?",
                                                      "Include Linker Genes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)
                return;
            if (reply == JOptionPane.NO_OPTION) {
                genes.removeAll(linkers);
            }
        }
        Thread t = new Thread() {
            @Override
            public void run() {
                ProgressPane progPane = new ProgressPane();
                progPane.setIndeterminate(true);
                progPane.setText("Annotating network...");
                JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
                frame.setGlassPane(progPane);
                frame.getGlassPane().setVisible(true);
                try {
                    RESTFulFIService fiService = new RESTFulFIService(view);
                    List<ModuleGeneSetAnnotation> annotations = null;
                    boolean isReactomeReactionNetwork = new TableHelper().isReactomeReactionNetwork(view.getModel());
                    if (isReactomeReactionNetwork)
                        annotations = fiService.annotateReactions(genes);
                    else
                        annotations = fiService.annotateGeneSet(genes, type);
                    // Check if selection is used
                    List<CyNode> nodeList = CyTableUtil.getNodesInState(view.getModel(), CyNetwork.SELECTED, true);
                    if (nodeList != null && nodeList.size() > 0)
                        ResultDisplayHelper.getHelper().displaySelectedGenesAnnotations(annotations, view, type, genes);
                    else
                        ResultDisplayHelper.getHelper().displayModuleAnnotations(annotations, view, type, false);
                }
                catch (Exception e) {
                    PlugInUtilities.showErrorMessage("Error in Annotating Network",
                                                     "Could not annotate network. Please see the logs for details.");
                }
                progPane.setIndeterminate(false);
                frame.getGlassPane().setVisible(false);
            }
        };
        t.start();
    }
    
    private void annotateNetworkModules(CyNetworkView view, String type) {
        // Since the task handles its GUIs by itself, we should not use TaskManager
        // to avoid GUI confilcts for now. But this may be change soon to use Cytoscape
        // native TaskManager.
        AnnotateNetworkModuleTask task = new AnnotateNetworkModuleTask(view, type);
        Thread t = new Thread() {
            public void run() {
                task.annotateNetworkModules();
            }
        };
        t.start();
    }   
}