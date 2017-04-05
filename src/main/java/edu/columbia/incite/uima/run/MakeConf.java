
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.run;

import java.util.List;
import java.util.Properties;

import edu.columbia.incite.uima.util.ComponentFactory;
import edu.columbia.incite.util.collection.CollectionTools;

/**
 *
 * @author gorgonzola
 */
public class MakeConf {

    /**
     * @param args the command line arguments
     * @throws java.lang.ClassNotFoundException
     */
    public static void main( String[] args ) throws ClassNotFoundException {
        for( String arg : args ) {
            Properties props = ComponentFactory.makeDefaultProperties( Class.forName( arg ) );
            List<String> keys = CollectionTools.toSortedList( props.stringPropertyNames() );
            keys.stream().forEach( ( key ) -> System.out.printf( "%s=%s\n", key, props.get( key ) ) );
        }
    }
    
}
