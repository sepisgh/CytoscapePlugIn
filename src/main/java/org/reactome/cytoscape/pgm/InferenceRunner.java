/*
 * Created on Jan 27, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.RenderablePathway;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.service.PathwayDiagramHighlighter;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.InferenceCannotConvergeException;
import org.reactome.factorgraph.InferenceType;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;

/**
 * Use this class for performing actual PGM inference.
 * @author gwu
 *
 */
public class InferenceRunner {
    private FactorGraph factorGraph;
    private Set<Variable> pathwayVars;
    // A subset of the above for outputs only
    private Set<Variable> outputVars;
    // Two inferencer
    private Inferencer lbp;
    private Inferencer gibbs;
    // Current status
    private InferenceStatus status;
    // To display running information
    private ProgressPane progressPane;
    // A flag to cancel inference
    private boolean abort;
    // Flag indicating the final results should be performed based on two cases
    private boolean usedForTwoCases;
    // To be highlight
    private PathwayEditor pathwayEditor;
    
    /**
     * Default constructor.
     */
    public InferenceRunner() {
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    public Set<Variable> getOutputVars() {
        return outputVars;
    }

    public void setOutputVars(Set<Variable> outputVars) {
        this.outputVars = outputVars;
    }

    public boolean isUsedForTwoCases() {
        return usedForTwoCases;
    }

    public void setUsedForTwoCases(boolean usedForTwoCases) {
        this.usedForTwoCases = usedForTwoCases;
    }

    public ProgressPane getProgressPane() {
        return progressPane;
    }

    public void setProgressPane(ProgressPane progressPane) {
        this.progressPane = progressPane;
    }

    public FactorGraph getFactorGraph() {
        return factorGraph;
    }

    public void setFactorGraph(FactorGraph factorGraph) {
        this.factorGraph = factorGraph;
    }
    
    public void setPathwayVars(Set<Variable> variables) {
        this.pathwayVars = variables;
    }

    public void setAlgorithms(List<Inferencer> algorithms) {
        if (algorithms == null)
            return;
        for (Inferencer alg : algorithms) {
            if (alg instanceof LoopyBeliefPropagation)
                lbp = alg;
            else if (alg instanceof GibbsSampling)
                gibbs = alg;
        }
    }
    
    public void abort() {
        this.abort = true;
    }

    public InferenceStatus getStatus() {
        return status;
    }
    
    /**
     * Calculate and show IPA values.
     * @param resultsList
     * @param target
     * @return true if values are shown.
     */
    private void showIPANodeValues(FactorGraphInferenceResults fgResults) {
        if (!fgResults.hasPosteriorResults()) // Just prior probabilities
            return ;
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        String title = "IPA Node Values";
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAValueTablePane valuePane = null;
        if (index < 0)
            valuePane = new IPAValueTablePane(title);
        else
            valuePane = (IPAValueTablePane) tableBrowserPane.getComponentAt(index);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        valuePane.setInferenceResults(fgResults);
    }
    
    private void showIPAPathwayValues(FactorGraphInferenceResults fgResults) throws MathException {
        if (!fgResults.hasPosteriorResults())
            return; 
        String title = "IPA Sample Analysis";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane,
                                                                    title);
        IPASampleAnalysisPane valuePane = null;
        if (index > -1)
            valuePane = (IPASampleAnalysisPane) tableBrowserPane.getComponentAt(index);
        else
            valuePane = new IPASampleAnalysisPane(title);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        valuePane.setInferenceResults(fgResults, pathwayVars);

        // Show outputs results
        title = "IPA Pathway Analysis";
        index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAPathwaySummaryPane outputPane = null;
        if (index > -1)
            outputPane = (IPAPathwaySummaryPane) tableBrowserPane.getComponentAt(index);
        else
            outputPane = new IPAPathwaySummaryPane(title);
        outputPane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        outputPane.setVariableResults(valuePane.getInferenceResults(),
                                      outputVars,
                                      fgResults.isUsedForTwoCases() ? fgResults.getSampleToType() : null);
        if (index == -1)
            index = tableBrowserPane.indexOfComponent(outputPane);
        if (index >= 0) // Select this as the default table for viewing the results
            tableBrowserPane.setSelectedIndex(index);
        // Highlight pathway diagram
        if (pathwayEditor != null) {
            Map<String, Double> idToValue = outputPane.getReactomeIdToIPADiff();
            highlightPathway(idToValue);
        }
    }

    private void highlightPathway(Map<String, Double> idToValue) {
        PathwayDiagramHighlighter highlighter = new PathwayDiagramHighlighter();
        double[] minMaxValue = PlugInObjectManager.getManager().getMinMaxColorValues();
        highlighter.highlightELV((RenderablePathway)pathwayEditor.getRenderable(),
                                 idToValue,
                                 minMaxValue[0],
                                 minMaxValue[1]);
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        pathwayEditor.firePropertyChange("showColorSpectrum", false, true);
    }
    
    public void performInference(boolean needFinishDialog) throws Exception {
        if (progressPane != null) {
            progressPane.enableCancelAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    abort();
                    // Need a interface to abort inference.
                    //                if (algorithm instanceof LoopyBeliefPropagation)
                    //                    ((LoopyBeliefPropagation)algorithm).setMaxIteration(0);
                    //                else if (algorithm instanceof GibbsSampling)
                    //                    ((GibbsSampling)algorithm).setMaxIteration(0);
                }
            });
        }
        // Use a new thread for performing inference so that it can be cancelled
        Thread t = new Thread() {
            public void run() {
                performInference();
            }
        };
        t.start();
        while (!progressPane.isCancelled()) {
            InferenceStatus status = getStatus();
            if (status == InferenceStatus.DONE || status == InferenceStatus.ERROR || status == InferenceStatus.ABORT) {
                break;
            }
            // Sleep for 2 seconds
            Thread.sleep(2000);
        }
        InferenceStatus status = getStatus();
        if (progressPane.isCancelled() || status != InferenceStatus.DONE) {
            return;
        }
        if (status == InferenceStatus.DONE) {
            FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(factorGraph);
            showInferenceResults(fgResults);
            if (needFinishDialog) {
                String message = "Inference has finished successfully. ";
                // Check if any posterior inference is done
                if (!fgResults.hasPosteriorResults())
                    message += "You may use \"View Marginal Probabilities\" by\n" + 
                        "selecting a variable node.";
                else
                    message += "You may view IPA values at the bottom \"IPA Node Values\" tab. \n" + 
                            "You may also view pathway level results at the \"IPA Pathway Analysis\" and \"IPA Sample Analysis\" tab.\n" +
                            "Note: IPA stands for \"Integrated Pathway Activity\".";
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              message,
                                              "Inference Finished",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public void showInferenceResults(FactorGraphInferenceResults fgResults) throws MathException {
        showIPANodeValues(fgResults);
        showIPAPathwayValues(fgResults);
    }
    
    /**
     * This is the actual place to run inference
     */
    private void performInference(Map<Variable, Integer> varToState,
                                  String sample) throws InferenceCannotConvergeException {
        // If LBP is set, we will use it.
        if (lbp != null) {
            try {
                lbp.setObservation(varToState);
                lbp.runInference();
            }
            catch(InferenceCannotConvergeException e) {
                if (gibbs == null || ((LoopyBeliefPropagation)lbp).getInferenceType() == InferenceType.MAX_PRODUCT)
                    throw e; // Gibbs cannot support MAX_PRODUCT now.
                if (gibbs != null) { // Try switch to Gibbs automatically if it is set.
                    progressPane.setText("Use Gibbs for " + 
                                         (sample == null ? " prior" : sample));
                    gibbs.setObservation(varToState);
                    gibbs.runInference();
                }
            }
        }
        else { // Only Gibbs is set
            gibbs.setObservation(varToState);
            gibbs.runInference();
        }
    }

    private synchronized void performInference() {
        if (lbp == null && gibbs == null) // We cannot do anything if there is no inferencer is set.
            return;
        if (factorGraph == null)
            return; // Nothing to be inferred
        // If we have lbp, try lbp first. The following check must work
        // since we have already made sure at least one inferencer should be set.
        if (lbp != null)
            lbp.setFactorGraph(factorGraph);
        if (gibbs != null)
            gibbs.setFactorGraph(factorGraph);
        try {
            status = InferenceStatus.WORKING;
            if (progressPane != null)
                progressPane.setText("Perform prior inference...");
            performInference(null, null);
            FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(factorGraph);
            fgResults.setUsedForTwoCases(usedForTwoCases);
            fgResults.storeInferenceResults(null); // Store prior result
            List<Observation> observations = FactorGraphRegistry.getRegistry().getObservations(factorGraph);
            Map<String, String> sampleToType = new HashMap<String, String>();
            fgResults.setSampleToType(sampleToType);
            if (observations != null) {
                progressPane.setIndeterminate(false);
                progressPane.setMaximum(observations.size());
                progressPane.setMinimum(0);
                int count = 0;
                for (Observation observation : observations) {
                    // If this is used for two cases and there is no type information for the observation
                    // the inference will not be performed for it.
                    if (usedForTwoCases && observation.getAnnoation() == null)
                        continue; 
                    if (progressPane != null)
                        progressPane.setText("Sample: " + observation.getName());
                    performInference(observation.getVariableToAssignment(), observation.getName());
                    fgResults.storeInferenceResults(observation.getName());
                    // If there is no sample type, don't include to avoid a new type (null!)
                    // appears in the further analysis.
                    if (observation.getAnnoation() != null)
                        sampleToType.put(observation.getName(), observation.getAnnoation());
                    count ++;
                    progressPane.setValue(count);
                    if (abort)
                        break;
                }
            }
            if (!abort) { // Maybe abort in the above loop.
                observations = FactorGraphRegistry.getRegistry().getRandomObservations(factorGraph);
                if (observations != null) {
                    int count = 0;
                    progressPane.setMaximum(observations.size());
                    for (Observation observation : observations) {
                        count ++;
                        if (progressPane != null)
                            progressPane.setText("Random sample: " + count);
                        performInference(observation.getVariableToAssignment(), "Random sample " + count);
                        fgResults.storeInferenceResults(observation.getName());
                        progressPane.setValue(count);
                        if (abort)
                            break;
                    }
                }
            }
            if (abort) {
                status = InferenceStatus.ABORT;
                // Empty inference results
                fgResults.clear();
            }
            else
                status = InferenceStatus.DONE;
        }
        catch(InferenceCannotConvergeException e) {
            String message = "Inference cannot converge. You may try to run inference again, which may converge\n" + 
                             "because of its stochastic feature, or try the Gibbs sampling algorithm.";
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          message,
                                          "Inference Cannot Converge",
                                          JOptionPane.ERROR_MESSAGE);
            status = InferenceStatus.ERROR;
        }
    }
    
}
