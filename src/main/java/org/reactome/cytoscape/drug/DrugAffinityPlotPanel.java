package org.reactome.cytoscape.drug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jdom.Element;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.CategoryTableXYDataset;
import org.jfree.ui.RectangleEdge;
import org.reactome.cytoscape.service.RESTFulFIService;

import edu.ohsu.bcb.druggability.dataModel.ExpEvidence;
import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * This customized JPanel is used to plot drug affinities as shown in Rory's
 * original TIBS paper (PMID: 28964549).
 * Note: Some of code in this class is modified from 
 * https://github.com/sissonr/TestApp/blob/master/appClientModule/testcases/StackedXYBarChartDemo2.java
 * @author wug
 *
 */
@SuppressWarnings("serial")
public class DrugAffinityPlotPanel extends JPanel {
    // Cache this so that only one renderer is used to get the same colors for same target.
    private StackedXYBarRenderer renderer;
    
    public DrugAffinityPlotPanel() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout(2, 2));
    }
    
    public void setInteractions(List<Interaction> interactions) {
        updatePlot(interactions);
    }
    
    private void checkInteractions(List<Interaction> interactions,
                                   Set<String> drugs,
                                   List<String> targets) {
        Set<String> targetSet = new HashSet<>();
        for (Interaction interaction : interactions) {
            String drug = interaction.getIntDrug().getDrugName();
            drugs.add(drug);
            if (interaction.getExpEvidenceSet() != null) {
                for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                    String relation = evidence.getAssayRelation();
                    if (relation == null)
                        continue;
                    if (relation.equals("=")) { // Use "=" value only
                        targetSet.add(interaction.getIntTarget().getTargetName());
                        break;
                    }
                }
            }
        }
        targets.addAll(targetSet);
        Collections.sort(targets);
    }
    
    private void showEmptyMessage(String message) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        JLabel label = new JLabel(message);
        panel.add(label, new GridBagConstraints());
        add(panel, BorderLayout.CENTER);
    }
    
    private void updatePlot(List<Interaction> interactions) {
        // Ensure only one drug is used
        Set<String> drugs = new HashSet<>();
        List<String> targets = new ArrayList<>();
        checkInteractions(interactions, drugs, targets);
        if (drugs.size() == 0) {
            showEmptyMessage("Cannot find any drug for plotting!");
            return; // Do nothing
        }
        if (drugs.size() > 1) {
            String message = "<html>More than one drug is found. Plotting<br />"
                           + "is desgined for one drug only.</html>";
            showEmptyMessage(message);
            return;
//            throw new IllegalArgumentException("More than one drug is listed in the table!");
        }
        
        if (targets.size() == 0) {
            showEmptyMessage("Cannot find any target having exact value for plotting!");
            return;
        }
        
        List<String> types = DrugTargetInteractionManager.getAssayTypes(interactions);
        NumberAxis xAxis = createAxis("Binding Assay Value (nM)");
        xAxis.setRange(0, 100); // Default range between 0 and 100 nM
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(xAxis);
        for (String type : types) {
            CategoryTableXYDataset dataset = createDataset(interactions,
                                                           type,
                                                           targets);
            if (dataset == null)
                continue;
            XYPlot plot = createPlot(dataset, type);
            combinedPlot.add(plot);
        }
        
        // We may get this problem because of the data error
        if (combinedPlot.getSubplots() == null || combinedPlot.getSubplots().size() == 0) {
            showEmptyMessage("Nothing to plot!");
            return;
        }
        
        JFreeChart chart = new JFreeChart(drugs.iterator().next(), combinedPlot);
        chart.removeLegend();
        
        XYPlot plot = (XYPlot) combinedPlot.getSubplots().get(0);
        LegendTitle legendtitle = new LegendTitle(plot);
        legendtitle.setBackgroundPaint(Color.white);
        legendtitle.setFrame(new BlockBorder());
        legendtitle.setPosition(RectangleEdge.BOTTOM);
        chart.addSubtitle(legendtitle);
        ChartUtilities.applyCurrentTheme(chart);
        
        ChartPanel chartPanel = new ChartPanel(chart);
        add(chartPanel, BorderLayout.CENTER);
    }
    
    private NumberAxis createAxis(String label) {
        NumberAxis numberaxis = new NumberAxis(label);
        numberaxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//        numberaxis.setUpperMargin(0.05d);
        return numberaxis;
    }
    
    private XYPlot createPlot(CategoryTableXYDataset dataset,
                              String type) {
        NumberAxis xAxis = createAxis("Binding Assay Value (nM)");
        xAxis.setRange(0, 100); // Default range between 0 and 100 nM
        NumberAxis yAxis = createAxis("Count");
        if (renderer == null)
            renderer = new StackedXYBarRenderer();
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        renderer.setDrawBarOutline(false);
        XYPlot plot = new XYPlot(dataset,
                xAxis,
                yAxis, 
                renderer);
        // This is a hack to add a label for different plots
        ValueAxis axis = new SymbolAxis(type, new String[] {});
        axis.setTickMarksVisible(false);
        axis.setMinorTickMarksVisible(false);
        // Keep the line, which looks better than without the line.
//        axis.setAxisLineVisible(false); // Don't show the actual line
        plot.setRangeAxis(1, axis);
        
        return plot;
    }
    
    private CategoryTableXYDataset createDataset(List<Interaction> interactions,
                                                 String neededType,
                                                 List<String> targets) {
        Map<String, Integer> keyToCount = new HashMap<>();
        interactions.forEach(interaction -> {
            String target = interaction.getIntTarget().getTargetName();
            if (interaction.getExpEvidenceSet() != null) {
                for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                    String relation = evidence.getAssayRelation();
                    if (relation == null || !(relation.equals("=") || relation.equals("<")))
                        continue; // Only pick equal and maximum value
                    String type = evidence.getAssayType();
                    if (type == null)
                        continue;
                    if (type.equals("KI"))
                        type = "Ki";
                    if (!neededType.equals(type))
                        continue;
                    double value = DrugTargetInteractionManager.getManager().getExpEvidenceValue(evidence).doubleValue();
                    String currentKey = target + "\t" + (int) value;
                    keyToCount.compute(currentKey, (key, count) -> {
                        if (count == null)
                            count = 1;
                        else
                            count ++;
                        return count;
                    });
                }
            }
        });
        if (keyToCount.size() == 0)
            return null;
        
        List<String> keyList = new ArrayList<>(keyToCount.keySet());
        Collections.sort(keyList); // This should sort based on targets
        CategoryTableXYDataset dataset = new CategoryTableXYDataset();
        
        // Add these two lines so that we have the same set of targets across all plots
        // and force to have same width bars
        targets.forEach(target -> dataset.add(0, 0, target));
        
        for (String key : keyList) {
            String[] tokens = key.split("\t");
            Integer bin = new Integer(tokens[1]);
            dataset.add(bin, keyToCount.get(key), tokens[0]);
        }
        
        return dataset;
    }
    
    /**
     * A test method so that we don't need to run through Cytoscape.
     */
    public static void main(String[] args) throws Exception {
        RESTFulFIService restfulService = new RESTFulFIService();
        List<String> drugs = Collections.singletonList("Imatinib Mesylate");
        Element rootElm = restfulService.queryInteractionsForDrugs(drugs, DrugDataSource.Targetome.toString());
        DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
        parser.parse(rootElm);
        List<Interaction> interactions = parser.getInteractions();
        System.out.println("Total interactions: " + interactions.size());
        
        DrugAffinityPlotPanel plotPane = new DrugAffinityPlotPanel();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(plotPane, BorderLayout.CENTER);
        plotPane.setInteractions(interactions);
        frame.pack();
        frame.setVisible(true);
    }
}
