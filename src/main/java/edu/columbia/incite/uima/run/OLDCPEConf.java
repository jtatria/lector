///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package edu.columbia.incite.uima.run;
//
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.Writer;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//
//import edu.columbia.incite.util.io.FileUtils;
//
///**
// *
// * @author Jose Tomas Atria <jtatria@gmail.com>
// */
//public final class CPEConf {
//    public static final String META_DIR        = ".metaDir";
//    public static final String ACTION_ON_ERROR = ".actionOnError";
//    public static final String DUMP_METADATA   = ".dumpMetadata";
//    public static final String THREADS         = ".threads";
//    public static final String READER_CLASS    = ".readerClass";
//    public static final String AE_CLASSES      = ".aeCLasses";
//
//    private static final String NAMESPACE = "edu.columbia.incite";
//
//    private final Properties props;
//    private final String ns;
//
//    public CPEConf( String ns, Properties props ) {
//        this.ns = ns;
//        this.props = props;
//
//        if( Boolean.parseBoolean( getSetting( DUMP_METADATA ) ) ) {
//            try( Writer w = FileUtils.getWriter( getSetting( META_DIR ), "config.properties", true, true ) ) {
//                dump( w );
//            } catch( IOException ex ) {}
//        }
//    }
//
//    private void dump( Writer w ) throws IOException {
//        List<String> out = new ArrayList<>();
//        props.stringPropertyNames().stream().filter( ( k ) -> ( k.startsWith( NAMESPACE ) ) )
//            .forEach( ( k ) -> out.add( String.format( "%s=%s", k, props.getProperty( k ) ) ) );
//        out.sort( ( String s1, String s2 ) -> s1.compareTo( s2 ) );
//        PrintWriter pw = new PrintWriter( w );
//        out.stream().forEach( ( line ) -> pw.println( line ) );
//        pw.flush();
//    }
//
//    public String getActionOnError() {
//        return getSetting( ACTION_ON_ERROR );
//    }
//
//    public int getNumberOfThreads() {
//        return Integer.parseInt( getSetting( THREADS ) );
//    }
//
//    public Class getReaderClass() throws ClassNotFoundException {
//        return Class.forName( getSetting( READER_CLASS ) );
//    }
//
//    public List<Class> getAEClasses() throws ClassNotFoundException {
//        List<Class> aeClasses = new ArrayList<>();
//        for( String aeClass : getSetting( AE_CLASSES ).split( "," ) ) {
//            aeClasses.add( Class.forName( aeClass ) );
//        }
//        return aeClasses;
//    }
//
//    public Properties getProps() {
//        return this.props;
//    }
//
//    public String getMetaOutputDir() {
//        return getSetting( META_DIR );
//    }
//
//    private String getSetting( String key ) {
//        return props.getProperty( ns + key );
//    }
//}
