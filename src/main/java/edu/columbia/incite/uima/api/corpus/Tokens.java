package edu.columbia.incite.uima.api.corpus;

import java.util.function.Function;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 *
 * @author gorgonzola
 */
public class Tokens {
    
    public static final String SEP = "_";
    public static final String NL_MRKR = "%%%";
    
    public static final int PGI  = 0;
    public static final int PFI  = 1;
    public static final int LMI  = 2;
    public static final int RAW  = 3;
    
    public static String[] parse( Token token ) {
        return parse( token, false );
    }
    
    public static String[] parse( Token token, boolean addTxt ) {
        String posg = token.getPos().getType().getShortName();
        String posf = token.getPos().getPosValue();
        String lemma = token.getLemma().getValue();
        String raw = addTxt ? token.getCoveredText() : null;
        return addTxt ? new String[]{ posg, posf, lemma, raw } : new String[]{ posg, posf, lemma };
    }
    
    public static String pos( Token token ) {
        String[] parse = parse( token );
        return String.join( SEP, parse[PGI], parse[PFI] );
    }
    
    public static String posG( Token token ) {
        return parse( token )[PGI];
    }
    
    public static String posF( Token token ) {
        return parse( token )[PFI];
    }
    
    public static String lemma( Token token ) {
        return parse( token )[LMI];
    }
    
    public static String raw( Token token ) {
        return parse( token )[RAW];
    }
    
    public static String build( Token token ) {
        return build( token, false );
    }
    
    public static String build( Token token, boolean addTxt ) {
        return String.join( SEP, parse( token, addTxt ) );
    }
    
    public enum LAction implements Function<Token,String> {
        /** Keep original text **/
        ASIS(   ( Token token ) -> parse( token, true )[RAW] ),
        /** Keep lemmatized form **/
        LEMMA( ( Token token ) -> parse( token )[LMI] ),
        /** Keep POS tag group and lemmatized form **/
        POSG(  ( Token token ) -> {
            String[] parse = parse( token );
            return String.join( SEP, parse[PGI], parse[LMI] );
        } ),
        /** Keep POS tag group, tag, and lemmatized form **/
        POSF(  ( Token token ) -> build( token ) ),
        FULL(  ( Token token ) -> build( token, true ) ),
        ;

        private final Function<Token,String> func;
        
        private LAction( Function<Token,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( Token token ) {
            return func.apply( token );
        }
    }

    public enum NLAction implements Function<Token,String> {
        DELETE( ( Token t ) -> "" ),
        MARK( ( Token t ) -> NL_MRKR ),
        POSG( ( Token t ) -> parse( t )[PGI] ),
        POSF( ( Token t ) -> parse( t )[PFI] ),
        ;

        private final Function<Token,String> func;
        
        private NLAction( Function<Token,String> func ) {
            this.func = func;
        }
        
        @Override
        public String apply( Token t ) {
            return func.apply( t );
        }
    }
}
