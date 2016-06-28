/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.io.resources.neu;

import org.apache.uima.resource.Resource;

/**
 *
 * @author gorgonzola
 */
public interface CharStreamProcessor extends Resource {
    
    public static final String EOL = "\n";
    public static final String EOW = " ";
    
    void consume( char[] chars, int offset, int length );
    
    void addBreak( String brk );
    
    int curLength();
    
    String result();

    void reset();

}
