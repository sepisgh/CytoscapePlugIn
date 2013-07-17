//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.07.13 at 06:14:13 PM PDT 
//

package org.reactome.cancerindex.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 */
// Note: for some reason, using original protected/Field cannot work!!!
// @XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DiseaseData")
public class DiseaseData
{

    // @XmlElement(name = "id", required = true)
    private Long id;
    // @XmlElement(name = "MatchedDiseaseTerm", required = true)
    private String matchedDiseaseTerm;
    // @XmlElement(name = "NCIDiseaseConceptCode", required = true)
    private String nciDiseaseConceptCode;
    // These two properties for constructing disease hierarchy
    private List<DiseaseData> subTerms;
    private List<DiseaseData> supTerms;
    private String definition;

    public List<DiseaseData> getSubTerms()
    {
        return subTerms;
    }

    public void setDefinition(String definition)
    {
        this.definition = definition;
    }

    public String getDefinition()
    {
        return this.definition;
    }

    public void setSubTerms(List<DiseaseData> subTerms)
    {
        this.subTerms = subTerms;
    }

    public void addSubTerm(DiseaseData term)
    {
        if (this.subTerms == null)
        {
            this.subTerms = new ArrayList<DiseaseData>();
        }
        this.subTerms.add(term);
    }

    public List<DiseaseData> getSupTerms()
    {
        return supTerms;
    }

    public void setSupTerms(List<DiseaseData> supTerms)
    {
        this.supTerms = supTerms;
    }

    public void addSupTerm(DiseaseData term)
    {
        if (supTerms == null)
        {
            supTerms = new ArrayList<DiseaseData>();
        }
        supTerms.add(term);
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getNciDiseaseConceptCode()
    {
        return nciDiseaseConceptCode;
    }

    public void setNciDiseaseConceptCode(String nciDiseaseConceptCode)
    {
        this.nciDiseaseConceptCode = nciDiseaseConceptCode;
    }

    /**
     * Gets the value of the matchedDiseaseTerm property.
     * 
     * @return possible object is {@link String }
     * 
     */
    public String getMatchedDiseaseTerm()
    {
        return matchedDiseaseTerm;
    }

    /**
     * Sets the value of the matchedDiseaseTerm property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setMatchedDiseaseTerm(String value)
    {
        this.matchedDiseaseTerm = value;
    }

    @Override
    public String toString()
    {
        return matchedDiseaseTerm;
    }

}
