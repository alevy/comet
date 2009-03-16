package edu.washington.cs.activedht.code.insecure;

import java.io.Serializable;

/** Event index. */
public enum DHTEvent implements Serializable {
    /** !PUT */
    PUT,
    /** !GET */
    GET,
    /** !DELETE */
    DELETE,
    /** !TIMER */
    TIMER  
}