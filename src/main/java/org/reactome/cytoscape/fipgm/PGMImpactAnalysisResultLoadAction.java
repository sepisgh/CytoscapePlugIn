/*
 * Created on Oct 19, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.event.ActionEvent;

import org.reactome.cytoscape.service.FICytoscapeAction;

/**
 * @author gwu
 *
 */
public class PGMImpactAnalysisResultLoadAction extends FICytoscapeAction {
    
    /**
     * @param title
     */
    public PGMImpactAnalysisResultLoadAction() {
        super("Open");
        setPreferredMenu("Apps.Reactome FI.PGM Impact Analysis[5]");
        setMenuGravity(6.0f);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!createNewSession())
            return; // Cannot create a new Cytoscape session. Stop here.
        PGMImpactResultLoadTask task = new PGMImpactResultLoadTask();
        Thread t = new Thread(task);
        t.start();
    }
    
}