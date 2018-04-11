/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.run;

import java.text.DecimalFormat;

/**
 *
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public abstract class Memory {
    
    public static <T> T release( T data ) {
        // TODO: move info to conf for messages
        String clz = data.getClass().getSimpleName();
        String adr = ( (Object) data ).toString();
        adr = adr.substring( adr.lastIndexOf( "@" ), adr.length() - 1 );
        Logs.infof( "Attempting to recover memory from %s at %s", clz, adr );
        Runtime rt = Runtime.getRuntime();
        long fm = rt.freeMemory();
        data = null;
        rt.gc();
        long diff = rt.freeMemory() - fm;
        Logs.infof( "Recovered %s of memory", humanSizes( diff ) );
        return data;
    }
    
    public static String humanSizes( long size ) {
        if(size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int dgrp = (int) ( Math.log10( size ) / Math.log10( 1024 ) );
        return new DecimalFormat( "#,##0.#" ).format( size / Math.pow( 1024, dgrp ) ) + " " + units[dgrp];
    }
}
