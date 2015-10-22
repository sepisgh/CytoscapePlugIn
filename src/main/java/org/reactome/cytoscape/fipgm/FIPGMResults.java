/*
 * Created on Oct 8, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;

/**
 * This singleton class is used to store observations and inference results.
 * This method is similar to these three classes in package org.reactome.cytoscape.pgm:
 * FactorGraphRegistry, FactorGraphInferenceResults and VariableInferenceResults. Most 
 * likely, this class should be merged into those three classes.
 * @author gwu
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FIPGMResults {
    @XmlTransient
    private static FIPGMResults results;
    // Stored information
    private List<Observation<Number>> observations;
    private List<Observation<Number>> randomObservations;
    private Map<String, Map<Variable, Double>> sampleToVarToScore;
    private Map<String, Map<Variable, Double>> randomSampleToVarToScore;
    // Just for saving purposes
    @XmlElement(name="variable")
    private List<Variable> variables;
    
    /**
     * Default constructor should not be called.
     */
    private FIPGMResults() {
    }
    
    public static FIPGMResults getResults() {
        if (results == null)
            results = new FIPGMResults();
        return results;
    }

    public List<Observation<Number>> getObservations() {
        return observations;
    }

    public void setObservations(List<Observation<Number>> observations) {
        this.observations = observations;
    }

    public List<Observation<Number>> getRandomObservations() {
        return randomObservations;
    }

    public void setRandomObservations(List<Observation<Number>> randomObservations) {
        this.randomObservations = randomObservations;
    }

    public Map<String, Map<Variable, Double>> getSampleToVarToScore() {
        return sampleToVarToScore;
    }
    
    /**
     * Get results for a subset of variables.
     * @param variables
     * @return
     */
    public Map<String, Map<Variable, Double>> getSampleToVarToScore(Collection<Variable> variables) {
        return _getSampleToVarToScore(sampleToVarToScore, variables);
    }
    
    public Map<String, Map<Variable, Double>> getRandomSampleToVarToScore(Collection<Variable> variables) {
        return _getSampleToVarToScore(randomSampleToVarToScore, variables);
    }
    
    private Map<String, Map<Variable, Double>> _getSampleToVarToScore(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                                                      Collection<Variable> variables) {
        Map<String, Map<Variable, Double>> rtn = new HashMap<>();
        for (String sample : sampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            Map<Variable, Double> varToScore1 = new HashMap<>();
            for (Variable var : variables) {
                Double score = varToScore.get(var);
                if (score == null)
                    continue;
                varToScore1.put(var, score);
            }
            if (varToScore1.size() > 0)
                rtn.put(sample, varToScore1);
        }
        return rtn;
    }

    public void setSampleToVarToScore(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        this.sampleToVarToScore = sampleToVarToScore;
    }

    public Map<String, Map<Variable, Double>> getRandomSampleToVarToScore() {
        return randomSampleToVarToScore;
    }

    public void setRandomSampleToVarToScore(Map<String, Map<Variable, Double>> randomSampleToVarToScore) {
        this.randomSampleToVarToScore = randomSampleToVarToScore;
    }
    
    /**
     * Save this analysis results.
     * @param fileName
     * @throws Exception
     */
    public void saveResults(File file) throws Exception {
        FIPGMResultsIO writer = new FIPGMResultsIO();
        writer.write(this, file);
    }
    
    /**
     * Load the analysis results saved in a file.
     * @param fileName
     * @throws Exception
     */
    public void loadResults(File file) throws Exception {
        FIPGMResultsIO writer = new FIPGMResultsIO();
        writer.read(this, file);
    }
    
}
