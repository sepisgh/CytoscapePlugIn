package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cancerindex.model.DiseaseData;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.graph.GeneClusterPair;
import org.reactome.r3.graph.NetworkClusterResult;
import org.reactome.r3.util.InteractionUtilities;

/**
 * This class provides a bunch of methods to modify a network (e.g. clustering,
 * retrieving cancer gene index, etc). Most functions appear as pop-up menus in
 * the network view. All context menu items are contained as subclasses
 * 
 * @author Eric T. Dawson
 * 
 */
class NetworkActionCollection // implements NetworkAboutToBeDestroyedListener,
                              // SessionLoadedListener
{
    private TableHelper tableHelper;
    ServiceReference tableFormatterServRef;
    TableFormatterImpl tableFormatter;
    private ModuleBasedSurvivalAnalysisHelper survivalHelper;

    // private ModuleBasedSurvivalHelper survivalHelper;
    public NetworkActionCollection()
    {
        tableHelper = new TableHelper();
    }

    /**
     * A method to grab the TableFormatterImpl from the address space so that
     * new tables can be created and filled with the proper columns.
     * 
     * @author Eric T Dawson
     */
    private void getTableFormatter()
    {
        try
        {
            BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
            ServiceReference servRef = context.getServiceReference(TableFormatter.class.getName());
            if (servRef != null)
            {
                this.tableFormatterServRef = servRef;
                this.tableFormatter = (TableFormatterImpl) context.getService(servRef);
            }
            else
                throw new Exception();
        }
        catch (Throwable t)
        {
            JOptionPane.showMessageDialog(null,
                    "The table formatter could not be retrieved.",
                    "Table Formatting Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * A method to release the TableFormatterImpl
     * 
     * @author Eric T Dawson
     */
    private void releaseTableFormatter()
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        context.ungetService(tableFormatterServRef);
    }

    /**
     * A convenience method to show a node in the current network view.
     * 
     * @param nodeView
     *            The View object for the node to show.
     */
    public static void showNode(View<CyNode> nodeView)
    {
        nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, true);
    }

    /**
     * A convenience method to hide a node in the current network view.
     * 
     * @param nodeView
     *            The View object for the node to be hidden.
     */
    public static void hideNode(View<CyNode> nodeView)
    {
        nodeView.setLockedValue(BasicVisualLexicon.NODE_VISIBLE, false);
    }

    public static void showEdge(View<CyEdge> edgeView)
    {
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, true);
    }

    public static void hideEdge(View<CyEdge> edgeView)
    {
        edgeView.setLockedValue(BasicVisualLexicon.EDGE_VISIBLE, false);
    }

    public static void setSelectedOrUnselected(CyNetwork network, CyNode node,
            boolean value)
    {
        Long nodeSUID = node.getSUID();
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTable.getRow(nodeSUID).set("selected", value);
    }

    private static void setSelectedOrUnselected(CyNetworkView view,
            View<CyNode> nodeView, boolean value)
    {
        setSelectedOrUnselected(view.getModel(), nodeView.getModel(), value);
    }

    public static void selectNodesFromList(CyNetwork network,
            Collection<CyNode> nodes, boolean value)
    {
        for (CyNode node : nodes)
        {
            setSelectedOrUnselected(network, node, value);
        }
    }

    public static boolean nodeNameInNetwork(String name, CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        for (CyNode node : network.getNodeList())
        {
            Long tmpSUID = node.getSUID();
            String tmp = nodeTable.getRow(tmpSUID).get("name", String.class);
            if (name.equals(tmp)) return true;
        }
        return false;
    }

    /**
     * A class for the network view right-click menu item which clusters the
     * network and a corresponding task/factory.
     * 
     * @author Eric T. Dawson
     * 
     */
    class ClusterFINetworkMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem clusterMenuItem = new JMenuItem("Cluster FI Network");
            clusterMenuItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ProgressPane progPane = new ProgressPane();
                    progPane.setIndeterminate(true);
                    progPane.setText("Clustering FI network...");
                    PlugInScopeObjectManager.getManager().getCytoscapeDesktop().setGlassPane(
                            progPane);
                    PlugInScopeObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(
                            true);
                    CyTable netTable = view.getModel().getDefaultNetworkTable();
                    String clustering = netTable.getRow(
                            view.getModel().getSUID()).get("clustering_Type",
                            String.class);
                    try
                    {
                        if (clustering != null && !(clustering.length() <=0)
                                && !clustering.equals(TableFormatterImpl.getSpectralPartitionCluster()))
                        {
                            CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
                            int reply = JOptionPane.showConfirmDialog(
                                    desktopApp.getJFrame(),
                                    "The displayed network has been clustered before using a different algorithm.\n"
                                            + "You may get different clustering results using this clustering feature. Do\n"
                                            + "you want to continue?",
                                    "Clustering Algorithm Warning",
                                    JOptionPane.OK_CANCEL_OPTION);

                            if (reply != JOptionPane.OK_OPTION)
                            {
                                progPane.setIndeterminate(false);
                                progPane.setVisible(false);
                                return;
                            }
                        }
                        Thread t = new Thread()
                        {
                            @Override
                            public void run()
                            {
                                clusterFINetwork(view);
                            }
                        };
                        t.start();
                    }
                    catch (Throwable t)
                    {
                        JOptionPane.showMessageDialog(null,
                                "The network cannot be clustered at this time\n"
                                        + t, "Error in Clustering Network",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    progPane.setIndeterminate(false);
                    progPane.setVisible(false);
                }

            });

            return new CyMenuItem(clusterMenuItem, 3.0f);
        }

    }

    /**
     * A class for the network view context menu item to fetch FI annotations.
     * 
     * @author Eric T. Dawson
     * 
     */
    class FIAnnotationFetcherMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem fetchFIAnnotationsMenu = new JMenuItem(
                    "Fetch FI Annotations");
            fetchFIAnnotationsMenu.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Thread t = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            EdgeActionCollection.annotateFIs(view);
                            try
                            {
                                BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
                                ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
                                FIVisualStyleImpl visStyler = (FIVisualStyleImpl) context.getService(servRef);
                                visStyler.setVisualStyle(view);
                                context.ungetService(servRef);
                            }
                            catch (Throwable t)
                            {
                                JOptionPane.showMessageDialog(
                                        null,
                                        "The visual style could not be applied.",
                                        "Visual Style Error",
                                        JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    };
                    t.start();
                }
            });
            return new CyMenuItem(fetchFIAnnotationsMenu, 1.0f);
        }

    }

    class NetworkPathwayEnrichmentMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem netPathMenuItem = new JMenuItem("Pathway Enrichment");
            netPathMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    annotateNetwork(view, "Pathway");
                }

            });
            return new CyMenuItem(netPathMenuItem, 2.0f);
        }

    }

    class NetworkGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem netGOCellComponentMenuItem = new JMenuItem(
                    "GO Cell Component");
            netGOCellComponentMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    annotateNetwork(view, "CC");
                }

            });
            return new CyMenuItem(netGOCellComponentMenuItem, 3.0f);

        }
    }

    class NetworkGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem netGOBioMenuItem = new JMenuItem("GO Biological Process");
            netGOBioMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    annotateNetwork(view, "BP");
                }

            });
            return new CyMenuItem(netGOBioMenuItem, 4.0f);
        }

    }

    class NetworkGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem netGOMolFuncMenuItem = new JMenuItem(
                    "GO Molecular Function");
            netGOMolFuncMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    annotateNetwork(view, "MF");
                }

            });
            return new CyMenuItem(netGOMolFuncMenuItem, 5.0f);
        }

    }

    class ModulePathwayEnrichmentMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem modPathMenuItem = new JMenuItem("Pathway Enrichment");
            modPathMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    annotateNetworkModules(view, "Pathway");
                }

            });
            return new CyMenuItem(modPathMenuItem, 6.0f);
        }

    }

    class ModuleGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem modGOCellMenuItem = new JMenuItem("GO Cell Component");
            modGOCellMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
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
     * 
     */
    class ModuleGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem modGOBioProcessMenuItem = new JMenuItem(
                    "GO Biological Process");
            modGOBioProcessMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
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
     * 
     */
    class ModuleGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem modGOMolFuncMenuItem = new JMenuItem(
                    "GO Molecular Function");
            modGOMolFuncMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
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
     * 
     */
    class SurvivalAnalysisMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem survivalAnalysisMenuItem = new JMenuItem(
                    "Survival Analysis");
            survivalAnalysisMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    doModuleSurvivalAnalysis(view);
                }
            });
            return new CyMenuItem(survivalAnalysisMenuItem, 10.f);
        }
    }

    class LoadCancerGeneIndexForNetwork implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem loadCGIItem = new JMenuItem("Fetch Cancer Gene Index");
            loadCGIItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    fetchNetworkCGI(view);
                }
            });
            return new CyMenuItem(loadCGIItem, 15f);
        }
    }

    private void fetchNetworkCGI(final CyNetworkView view)
    {
        // TODO Auto-generated method stub
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                ProgressPane progPane = new ProgressPane();
                progPane.setIndeterminate(true);
                progPane.setText("Fetching Cancer Gene Indices");
                JFrame frame = PlugInScopeObjectManager.getManager().getCytoscapeDesktop();
                frame.setGlassPane(progPane);
                progPane.setVisible(true);
                try
                {
                    NCICancerIndexDiseaseHelper diseaseHelper = new NCICancerIndexDiseaseHelper(
                            view);
                    if (!diseaseHelper.areDiseasesShown())
                    {
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
                    if (view.getModel().getDefaultNodeTable().getColumn(
                            "diseases") == null)
                    {
                        tableHelper.createNewColumn(nodeTable, "diseases",
                                String.class);
                    }
                    tableHelper.loadNodeAttributesByName(view.getModel(),
                            "diseases", geneToDiseases);
                }
                catch (Exception e)
                {
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

    private Set<String> getGenesInNetwork(CyNetworkView view)
    {
        Set<String> genes = new HashSet<String>();
        CyTable netTable = view.getModel().getDefaultNodeTable();
        for (CyNode node : view.getModel().getNodeList())
        {
            Long nodeSUID = node.getSUID();
            String name = netTable.getRow(nodeSUID).get("name", String.class);
            genes.add(name);
        }
        return genes;
    }

    private void doModuleSurvivalAnalysis(CyNetworkView view)
    {
        Map<String, Integer> nodeToModule = extractNodeToModule(view);
        if (nodeToModule == null || nodeToModule.isEmpty()) return; // There is
                                                                    // not data
                                                                    // for
                                                                    // analysis.
        TableHelper tableHelper = new TableHelper();
        String dataType = tableHelper.getDataSetType(view);
        if (dataType == null) return; // The type of data is unknown so nothing
                                      // can be done.
        try
        {
            if (dataType.equals(TableFormatterImpl.getSampleMutationData()))
            {
                Map<String, Object> nodeToSamples = tableHelper.getNodeTableValuesByName(
                        view.getModel(), "samples", String.class);
                if (nodeToSamples == null || nodeToSamples.isEmpty())
                {
                    PlugInUtilities.showErrorMessage("No Sample Information",
                            "Survival Analysis can not be performed because no sample information exists.");
                    return;
                }
                Map<String, Set<String>> nodeToSampleSet = extractNodeToSampleSet(nodeToSamples);
                if (survivalHelper == null)
                {
                    survivalHelper = new ModuleBasedSurvivalAnalysisHelper();
                }
                survivalHelper.doSurvivalAnalysis(nodeToModule, nodeToSampleSet);
            }
            else if (dataType.equals(TableFormatterImpl.getMCLArrayClustering()))
            {
                Map<Integer, Map<String, Double>> moduleToSampleToValue = PlugInScopeObjectManager.getManager().getMCLModuleToSampleToValue();
                if (moduleToSampleToValue == null
                        || moduleToSampleToValue.size() == 0)
                {
                    PlugInUtilities.showErrorMessage(
                            "No sample information has been provided. Survival analysis cannot be done.",
                            "No Sample Information");
                    return;
                }
                if (survivalHelper == null)
                {
                    survivalHelper = new ModuleBasedSurvivalAnalysisHelper();
                }
                survivalHelper.doSurvivalAnalysisForMCLModules(nodeToModule,
                        moduleToSampleToValue);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PlugInUtilities.showErrorMessage("Error During Survival Analysis",
                    "Survival Analysis could not be performed.\n Please see the logs.");
        }
    }

    /**
     * A method for performing clustering of the FI network. This function is
     * only applicable for Spectral Partition Clustering. Reclustering networks
     * originally clustered with other algorithms may change results.
     * 
     * @param view
     *            The current network view.
     * @author Eric T Dawson
     */
    public void clusterFINetwork(CyNetworkView view)
    {
        try
        {
            // ProgressPane progPane = new ProgressPane();
            // progPane.setText("Clustering network...");
            // progPane.setIndeterminate(true);
            // desktopApp.getJFrame().
            getTableFormatter();
            tableFormatter.makeModuleAnalysisTables(view.getModel());
            List<CyEdge> edgeList = view.getModel().getEdgeList();
            RESTFulFIService service = new RESTFulFIService(view);
            // The below method takes CyEdges as an input type, but with the
            // reorganization of the API in 3.x it should really take the
            // name
            // of the nodes (nodes now have a Long SUID and not a String
            // Identifier).

            NetworkClusterResult clusterResult = service.cluster(edgeList, view);
            Map<String, Integer> nodeToCluster = new HashMap<String, Integer>();
            List<GeneClusterPair> geneClusterPairs = clusterResult.getGeneClusterPairs();
            if (geneClusterPairs != null)
            {
                for (GeneClusterPair geneCluster : geneClusterPairs)
                {
                    nodeToCluster.put(geneCluster.getGeneId(),
                            geneCluster.getCluster());
                }
            }

            tableHelper.loadNodeAttributesByName(view, "module", nodeToCluster);
            tableHelper.storeClusteringType(view,
                    TableFormatterImpl.getSpectralPartitionCluster());
            Map<String, Object> nodeToSamples = tableHelper.getNodeTableValuesByName(
                    view.getModel(), "samples", String.class);

            try
            {
                showModuleInTab(nodeToCluster, nodeToSamples,
                        clusterResult.getModularity(), view);

                BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
                ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
                FIVisualStyleImpl visStyler = (FIVisualStyleImpl) context.getService(servRef);
                visStyler.setVisualStyle(view);
            }
            catch (Throwable t)
            {
                JOptionPane.showMessageDialog(null,
                        "The visual style could not be applied.",
                        "Visual Style Error", JOptionPane.ERROR_MESSAGE);
            }
            releaseTableFormatter();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null,
                    "There was an error during network clustering.",
                    "Error in clustering", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }

    private void showModuleInTab(Map<String, Integer> nodeToCluster,
            Map<String, Object> nodeToSamples, Double modularity,
            CyNetworkView view)
    {
        Map<String, Set<String>> nodeToSampleSet = extractNodeToSampleSet(nodeToSamples);
        ResultDisplayHelper.getHelper().showModuleInTab(nodeToCluster,
                nodeToSampleSet, modularity, view);
    }

    private Map<String, Set<String>> extractNodeToSampleSet(
            Map<String, Object> nodeToSamples)
    {
        Map<String, Set<String>> nodeToSampleSet = null;
        if (nodeToSamples != null)
        {
            nodeToSampleSet = new HashMap<String, Set<String>>();
            for (String node : nodeToSamples.keySet())
            {
                String sampleText = (String) nodeToSamples.get(node);
                String[] tokens = sampleText.split(";");
                Set<String> set = new HashSet<String>();
                for (String token : tokens)
                {
                    set.add(token);
                }
                nodeToSampleSet.put(node, set);
            }
        }
        return nodeToSampleSet;
    }

    protected void annotateNetwork(final CyNetworkView view, final String type)
    {
        final Set<String> genes = new HashSet<String>();
        // Check if linkers were used.
        Set<String> linkers = new HashSet<String>();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        for (CyNode node : view.getModel().getNodeList())
        {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name",
                    String.class);
            genes.add(nodeName);
            Boolean isLinker = nodeTable.getRow(nodeSUID).get("isLinker",
                    Boolean.class);
            if (isLinker != null && isLinker)
            {
                linkers.add(nodeName);
            }
        }
        if (!linkers.isEmpty())
        {
            CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
            int reply = JOptionPane.showConfirmDialog(
                    desktopApp.getJFrame(),
                    "Linkers have been used in network construction.\n"
                            + "Including linkers will bias results. Would you like to include them anyway?",
                    "Include Linker Genes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION) return;
            if (reply == JOptionPane.NO_OPTION)
            {
                genes.removeAll(linkers);
            }
        }
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                ProgressPane progPane = new ProgressPane();
                progPane.setIndeterminate(true);
                progPane.setText("Annotating network...");
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop().setGlassPane(
                        progPane);
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(
                        true);
                try
                {
                    RESTFulFIService fiService = new RESTFulFIService(view);
                    List<ModuleGeneSetAnnotation> annotations = fiService.annotateGeneSet(
                            genes, type);
                    displayModuleAnnotations(annotations, view, type, false);
                }
                catch (Exception e)
                {
                    PlugInUtilities.showErrorMessage(
                            "Error in Annotating Network",
                            "Could not annotate network. Please see the logs for details.");
                }
                progPane.setIndeterminate(false);
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(
                        false);
            }
        };
        t.start();
    }

    private void annotateNetworkModules(final CyNetworkView view,
            final String type)
    {
        final Map<String, Integer> nodeToModule = extractNodeToModule(view);
        if (nodeToModule == null || nodeToModule.isEmpty()) return;
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                ProgressPane progPane = new ProgressPane();
                progPane.setIndeterminate(true);
                progPane.setText("Annotating modules...");
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop().setGlassPane(
                        progPane);
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(
                        true);
                try
                {
                    RESTFulFIService fiService = new RESTFulFIService(view);
                    List<ModuleGeneSetAnnotation> annotations = fiService.annotateNetworkModules(
                            nodeToModule, type);
                    displayModuleAnnotations(annotations, view, type, true);
                }
                catch (Exception e)
                {
                    PlugInUtilities.showErrorMessage(
                            "Error in Annotating Modules",
                            "Please see the logs for details.");
                    e.printStackTrace();
                }
                progPane.setIndeterminate(false);
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop().getGlassPane().setVisible(
                        false);
            }
        };
        t.start();
    }

    private void displayModuleAnnotations(
            List<ModuleGeneSetAnnotation> annotations, CyNetworkView view,
            String type, boolean isForModule)
    {
        ResultDisplayHelper.getHelper().displayModuleAnnotations(annotations,
                view, type, isForModule);
    }

    private Map<String, Integer> extractNodeToModule(CyNetworkView view)
    {
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        CyTable netTable = view.getModel().getDefaultNetworkTable();
        Long netSUID = view.getModel().getSUID();
        // Check if the network has been clustered
        if (netTable.getRow(netSUID).get("clustering_Type", String.class) == null)
        {
            PlugInUtilities.showErrorMessage("Error in Annotating Modules",
                    "Please cluster the FI network before annotating modules.");
            return null;
        }
        final Map<String, Integer> nodeToModule = new HashMap<String, Integer>();
        Set<String> linkers = new HashSet<String>();
        for (CyNode node : view.getModel().getNodeList())
        {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name",
                    String.class);
            Integer module = nodeTable.getRow(nodeSUID).get("module",
                    Integer.class);
            // Since nodes which are unlinked will have null value for module
            // (as may some other nodes),
            // only use those nodes with value for module.
            if (module != null)
            {
                nodeToModule.put(nodeName, module);
                Boolean isLinker = nodeTable.getRow(nodeSUID).get("isLinker",
                        Boolean.class);
                if (isLinker != null && isLinker)
                {
                    linkers.add(nodeName);
                }
            }
        }
        Integer cutoff = applyModuleSizeFiler(nodeToModule);
        if (cutoff == null) return null; // Equivalent to canceling the task.
        if (!linkers.isEmpty())
        {
            CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
            int reply = JOptionPane.showConfirmDialog(
                    desktopApp.getJFrame(),
                    "Linkers have been used in network construction."
                            + " Including linkers\n will bias results. Would you like to exclude them from analysis?",
                    "Exclude Linkers?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)

            return null;
            if (reply == JOptionPane.YES_OPTION)
            {
                nodeToModule.keySet().removeAll(linkers);
                if (nodeToModule.isEmpty())
                {
                    JOptionPane.showMessageDialog(
                            desktopApp.getJFrame(),
                            "No genes remain after removing linkers. Annotation cannot be performed.",
                            "Cannot Annotate Modules",
                            JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            }
        }
        return nodeToModule;
    }

    private Integer applyModuleSizeFiler(Map<String, Integer> nodeToModule)
    {
        Map<Integer, Set<String>> clusterToGenes = new HashMap<Integer, Set<String>>();
        for (String node : nodeToModule.keySet())
        {
            Integer module = nodeToModule.get(node);
            InteractionUtilities.addElementToSet(clusterToGenes, module, node);
        }
        Set<Integer> values = new HashSet<Integer>();
        for (Set<String> set : clusterToGenes.values())
        {
            values.add(set.size());
        }
        List<Integer> sizeList = new ArrayList<Integer>(values);
        Collections.sort(sizeList);
        Integer input = (Integer) JOptionPane.showInputDialog(
                PlugInScopeObjectManager.getManager().getCytoscapeDesktop(),
                "Please choose a size cutoff for modules. Modules with sizes equal\n"
                        + "or more than the cutoff will be used for analysis:",
                "Choose Module Size", JOptionPane.QUESTION_MESSAGE, null,
                sizeList.toArray(), sizeList.get(0));
        if (input == null) return null; // Cancel has been pressed.
        // Do a filtering based on size
        Set<String> filtered = new HashSet<String>();
        for (Set<String> set : clusterToGenes.values())
        {
            if (set.size() < input)
            {
                filtered.addAll(set);
            }
        }
        nodeToModule.keySet().removeAll(filtered);
        return input;
    }

}
