/*
 * Created on Sep 1, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.event.ActionEvent;

import org.reactome.cytoscape.service.FICytoscapeAction;

/**
 * @author gwu
 *
 */
public class PGMImpactAnalysisAction extends FICytoscapeAction {
    
    public PGMImpactAnalysisAction() {
        super("PGM Impact Analysis");
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!createNewSession())
            return; // Cannot create a new Cytoscape session. Stop here.
        PGMImpactAnalysisDialog dialog = new PGMImpactAnalysisDialog();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setModal(true);
        dialog.setVisible(true);
        if (!dialog.isOkClicked())
            return;
        PGMImpactAnalysisTask task = new PGMImpactAnalysisTask();
        task.setData(dialog.getSelectedData());
        task.setLbp(dialog.getLBP());
        task.setPGMType(dialog.getPGMType());
        task.setNumberOfPermutation(dialog.getNumberOfPermutation());
        Thread t = new Thread(task);
        t.start();
    }
    
}
