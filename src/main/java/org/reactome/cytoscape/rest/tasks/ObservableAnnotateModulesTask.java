package org.reactome.cytoscape.rest.tasks;

import java.util.Collections;
import java.util.List;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.reactome.annotate.AnnotationType;
import org.reactome.cytoscape3.AnnotateNetworkModuleTask;
import org.reactome.cytoscape3.GeneSetAnnotationPanelForModules;

public class ObservableAnnotateModulesTask extends AnnotateNetworkModuleTask implements ObservableTask {
    private ReactomeFIVizTable result;
    
    public ObservableAnnotateModulesTask(CyNetworkView view, String type) {
        super(view, type);
    }
    
    @Override
    public void run(TaskMonitor monitor) throws Exception {
        super.run(monitor);
        extractResults();
    }
    
    private void extractResults() {
        if (isAborted)
            return;
        String title = (type.equals(AnnotationType.Pathway.toString())) ? "Pathways in Modules" : "GO " + type + " in Modules";
        result = new ReactomeFIVizTable();
        result.fillTableFromResults(GeneSetAnnotationPanelForModules.class, title);
    }

    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(ReactomeFIVizTable.class))
            return (R) result;
        return null;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(ReactomeFIVizTable.class);
    }
    
    
    

}
