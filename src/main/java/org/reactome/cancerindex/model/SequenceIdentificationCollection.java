//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.07.13 at 06:14:13 PM PDT 
//


package org.reactome.cancerindex.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SequenceIdentificationCollection")
public class SequenceIdentificationCollection {
    @XmlElement(name="id", required = true)
    protected Long id;
    @XmlElement(name = "HgncID", required = true)
    protected String hgncID;
    @XmlElement(name = "LocusLinkID", required = true)
    protected String locusLinkID;
    @XmlElement(name = "GenbankAccession", required = true)
    protected String genbankAccession;
    @XmlElement(name = "RefSeqID", required = true)
    protected String refSeqID;
    @XmlElement(name = "UniProtID", required = true)
    protected String uniProtID;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the value of the hgncID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHgncID() {
        return hgncID;
    }

    /**
     * Sets the value of the hgncID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHgncID(String value) {
        this.hgncID = value;
    }

    /**
     * Gets the value of the locusLinkID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocusLinkID() {
        return locusLinkID;
    }

    /**
     * Sets the value of the locusLinkID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocusLinkID(String value) {
        this.locusLinkID = value;
    }

    /**
     * Gets the value of the genbankAccession property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGenbankAccession() {
        return genbankAccession;
    }

    /**
     * Sets the value of the genbankAccession property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGenbankAccession(String value) {
        this.genbankAccession = value;
    }

    /**
     * Gets the value of the refSeqID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRefSeqID() {
        return refSeqID;
    }

    /**
     * Sets the value of the refSeqID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRefSeqID(String value) {
        this.refSeqID = value;
    }

    /**
     * Gets the value of the uniProtID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUniProtID() {
        return uniProtID;
    }

    /**
     * Sets the value of the uniProtID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUniProtID(String value) {
        this.uniProtID = value;
    }

}