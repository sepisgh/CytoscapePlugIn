/*
 * Created on Mar 3, 2014
 *
 */
package org.reactome.cytoscape.service;

/**
 * @author gwu
 *
 */
public enum ReactomeNetworkType {
    
    FINetwork, // Just normal FI network
    PathwayFINetwork, // A FI converted from a pathway diagram
    FactorGraph, // A network showing a factor graph
    PGMFINetwork, // FI generated based on FI PGM
    ReactionNetwork, // Networks generated based on preceding/following relationships or shared inputs among reactions
    MechismoNetwork // Used to perform mechismo network visualization
    
}
