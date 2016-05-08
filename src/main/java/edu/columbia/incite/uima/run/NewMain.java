/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 *
 * @author gorgonzola
 */
public class NewMain {

    /**
     * @param args the command line arguments
     */
    public static void main( String[] args ) {
        // This is insane
        String format = LocalDateTime.now().format( DateTimeFormatter.ofPattern( "yyyy-MM-dd_HH:mm" ) );
        System.out.print( format );
    }
    
}
