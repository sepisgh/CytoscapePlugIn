package org.reactome.cytoscape.mechismo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.mechismo.model.Interaction;
import org.reactome.mechismo.model.Reaction;
import org.reactome.r3.util.InteractionUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used to query mechsimo data.
 * @author wug
 *
 */
public class MechismoDataFetcher {
    private Logger logger = LoggerFactory.getLogger(MechismoDataFetcher.class);
    private String restfulHost;
    
    public MechismoDataFetcher() {
        restfulHost = PlugInObjectManager.getManager().getProperties().getProperty("MechismoWSURL");
        if (restfulHost == null)
            throw new IllegalStateException("MechismoWSURL has not been configured!");
    }
    
    public void loadMechismoInteractions(CyNetworkView networkView) {
        Set<String> fis = grepFINames(networkView);
        String url = restfulHost + "interactions";
        try {
            String output = PlugInUtilities.callHttpInJson(url,
                                PlugInUtilities.HTTP_POST,
                                String.join(",", fis));
            ObjectMapper mapper = new ObjectMapper();
            List<Interaction> interactions = mapper.readValue(output,
                                                        new TypeReference<List<Interaction>>() {
                                                        });
            displayInteractions(interactions);
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in fetching Mechismo analysis results:\n" + e.getMessage(),
                                          "Error in Fetching Mechismo Results",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayInteractions(List<Interaction> interactions) {
        MechismoInteractionPane pane = PlugInUtilities.getCytoPanelComponent(MechismoInteractionPane.class,
                CytoPanelName.SOUTH, 
                MechismoInteractionPane.TITLE);
        pane.setInteractions(interactions);
    }
    
    private Set<String> grepFINames(CyNetworkView view) {
        CyTable edgeTable = view.getModel().getDefaultEdgeTable();
        Set<String> names = view.getEdgeViews()
                                .stream()
                                .map(edgeView -> getEdgeName(edgeView.getModel(), edgeTable))
                                .collect(Collectors.toSet());
        return names;
    }
    
    private String getEdgeName(CyEdge edge, CyTable table) {
        String name = table.getRow(edge.getSUID()).get("name", String.class);
        // The format of name should be something like: Gene1 (FI) Gene2
        // We need to convert names that can be used by the RESTful API
        String[] tokens = name.split(" ");
        return InteractionUtilities.generateFIFromGene(tokens[0], tokens[2]);
    }
    
    public void loadMechismoReactions(PathwayEditor pathwayEditor) {
        Set<String> dbIds = grepReactionIds(pathwayEditor);
        String text = String.join(",", dbIds);
        String url = restfulHost + "reactions";
        try {
            String output = PlugInUtilities.callHttpInJson(url,
                    PlugInUtilities.HTTP_POST,
                    text);
            ObjectMapper mapper = new ObjectMapper();
            List<Reaction> reactions = mapper.readValue(output,
                                                        new TypeReference<List<Reaction>>() {
                                                        });
            // Remove null
            reactions = reactions.stream().filter(rxt -> rxt != null).collect(Collectors.toList());
            displayReactions(reactions);
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in fetching Mechismo analysis results:\n" + e.getMessage(),
                                          "Error in Fetching Mechismo Results",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayReactions(List<Reaction> reactions) {
        MechismoReactionPane pane = PlugInUtilities.getCytoPanelComponent(MechismoReactionPane.class,
                                                                          CytoPanelName.SOUTH, 
                                                                          MechismoReactionPane.TITLE);
         pane.setReactions(reactions);
    }
    
    @SuppressWarnings("unchecked")
    private Set<String> grepReactionIds(PathwayEditor editor) {
        // Have to cast the following first. Otherwise, statements
        // after this will not work.
        List<Renderable> renderables = editor.getDisplayedObjects();
        Set<String> dbIds = renderables
                .stream()
                .filter(r -> r instanceof HyperEdge)
                .filter(r -> r.getReactomeId() != null)
                .map(r -> r.getReactomeId() + "")
                .collect(Collectors.toSet());
        return dbIds;
    }
    
    

}
