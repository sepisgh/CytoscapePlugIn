/*
 * Created on Sep 18, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * @author gwu
 *
 */
public class FIPGMImpactVisualStyle extends FIVisualStyleImpl {
    private CyNetworkViewManager viewManager;
    private double[] minMaxGeneValues;
    
    /**
     * Default constructor.
     */
    public FIPGMImpactVisualStyle() {
    }
    
    public double[] getMinMaxGeneValues() {
        return minMaxGeneValues;
    }

    public void setMinMaxGeneValues(double[] minMaxGeneValues) {
        this.minMaxGeneValues = minMaxGeneValues;
    }

    public CyNetworkViewManager getViewManager() {
        return viewManager;
    }

    public void setViewManager(CyNetworkViewManager viewManager) {
        this.viewManager = viewManager;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void setNodeSizes(CyNetworkView view, 
                                VisualStyle fiVisualStyle,
                                VisualMappingFunctionFactory visMapFuncFactoryC) {
        // Set the node size based on sample number
        double[] scoreRange = getGeneScoreRange(view);
        if (scoreRange != null) {
            ContinuousMapping scoreToSizeFunc = (ContinuousMapping) visMapFuncFactoryC.createVisualMappingFunction(FIVisualStyle.GENE_VALUE_ATT, 
                                                                                                                              Double.class,
                                                                                                                              BasicVisualLexicon.NODE_SIZE);
            BoundaryRangeValues<Double> lowerBoundary = new BoundaryRangeValues<Double>(
                    10.0, 10.0, 10.0);
            BoundaryRangeValues<Double> upperBoundary = new BoundaryRangeValues<Double>(
                    100.0, 100.0, 100.0);
            scoreToSizeFunc.addPoint(scoreRange[0],
                    lowerBoundary);
            scoreToSizeFunc.addPoint(scoreRange[1],
                    upperBoundary);
            fiVisualStyle.addVisualMappingFunction(scoreToSizeFunc);
        }
    }
    
    private double[] getGeneScoreRange(CyNetworkView view) {
        if (minMaxGeneValues != null)
            return minMaxGeneValues;
        Map<Long, Object> idToValue = new HashMap<Long, Object>();
        TableHelper helper = new TableHelper();
        if (viewManager != null) {
            Set<CyNetworkView> views = viewManager.getNetworkViewSet();
            if (views != null) {
                for (CyNetworkView view1 : views) {
                    if(!helper.isReactomeNetwork(view1))
                        continue;
                    Map<Long, Object> idToValue1 = helper.getNodeTableValuesBySUID(view1.getModel(), 
                                                                                   FIVisualStyle.GENE_VALUE_ATT, 
                                                                                   Double.class);
                    if (idToValue1 == null || idToValue1.size() == 0)
                        continue;
                    idToValue.putAll(idToValue1);
                }
            }
        }
        else {
            Map<Long, Object> idToValue1 = new TableHelper().getNodeTableValuesBySUID(view.getModel(), 
                                                                                      FIVisualStyle.GENE_VALUE_ATT, 
                                                                                      Double.class);
            if (idToValue1 != null && idToValue1.size() > 0)
                idToValue.putAll(idToValue1);
        }
        if (idToValue.size() == 0)
            return null;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Object obj : idToValue.values()) {
            Double value = (Double) obj;
            if (value > max)
                max = value;
            if (value < min)
                min = value;
        }
        return new double[]{min, max};
    }

    @Override
    protected void setDefaultEdgeStyle(VisualStyle fiVisualStyle) {
        super.setDefaultEdgeStyle(fiVisualStyle);
        // Use light gray to emphasize nodes
        fiVisualStyle.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT,
                                      Color.LIGHT_GRAY);
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference reference = context.getServiceReference(VisualLexicon.class.getName());
        VisualLexicon lexicon = (VisualLexicon) context.getService(reference);
        VisualProperty<Paint> edgeTargetArrowPaint = (VisualProperty<Paint>) lexicon.lookup(CyEdge.class,
                "edgeTargetArrowColor"); 
        fiVisualStyle.setDefaultValue(edgeTargetArrowPaint,
                                      Color.LIGHT_GRAY);
        VisualProperty<Paint> edgeSourceArrowPaint = (VisualProperty<Paint>) lexicon.lookup(CyEdge.class,
                "edgeSourceArrowColor"); 
        fiVisualStyle.setDefaultValue(edgeSourceArrowPaint,
                                      Color.LIGHT_GRAY);
        lexicon = null;
        context.ungetService(reference);
    }
    
}
