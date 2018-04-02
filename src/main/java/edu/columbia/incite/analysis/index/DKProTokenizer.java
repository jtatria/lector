/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.analysis.index;

import java.util.Locale;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.corpus.POSClass;


/**
 * Thread safe implementation of a @link{Tokenizer} for DKPro's @link{Token}s.
 * 
 * Currently supports POS and Lemma data. Support for additional tokendata planned but not 
 * implemented yet.
 * 
 * This Tokenizer caches all available data from a token once, and creates term and paylod data 
 * according to the given configuration passed to its constructor.
 * 
 * TODO: define API for flexile configuration?
 * TODO: uses Lucene's automata, which are awesome bu currently impose a dependency on lucene. 
 * TODO: ask upstream to spawn off automata into separate package?
 * 
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class DKProTokenizer implements Tokenizer {        
    /** POS group index in canonical array **/
    public static final int POSG  = 0;
    /** POS tag index in canonical array **/
    public static final int POST  = 1;
    /** Lemma index in canonical array **/
    public static final int LEMA  = 2;
    /** Raw text index in canonical array **/
    public static final int TEXT  = 3;
    
    private final ThreadLocal<Token> curToken = new ThreadLocal<>();
    
    private final ThreadLocal<String[]>     curParts = ThreadLocal.withInitial(
        () -> new String[4] 
    );
    
    private final ThreadLocal<StringBuilder> tlsb = ThreadLocal.withInitial(
        () -> new StringBuilder() 
    );
    
    private final CharacterRunAutomaton exclude;
    private final boolean pos;
    private final boolean lemma;
    
    /**
     * Create a new tokenizer with the given options.
     * @param exclude   @link{POSClass} array with excluded POS tags.
     * @param addPos    Include POS tag in charterm.
     * @param lemmatize Use lemmatized form instead of covered text.
     */
    public DKProTokenizer( POSClass[] exclude, boolean addPos, boolean lemmatize ) {
        this.exclude       = new CharacterRunAutomaton( POSClass.union( exclude ) );
        this.pos           = addPos;
        this.lemma         = lemmatize;
    }
    
    /**
     * Create canonical array from the given @link{Token}.
     * @param token
     * @return A @link{String[]} containing all token data.
     */
    public String[] parts( Token token ) {
        if( curToken.get() == null || curToken.get() != token ) {
            curToken.set( token );
            curParts.get()[POSG] = token.getPos().getType().getShortName();
            curParts.get()[POST] = token.getPos().getPosValue();
            curParts.get()[LEMA] = token.getLemma().getValue().toLowerCase( Locale.ROOT );
            curParts.get()[TEXT] = token.getCoveredText().toLowerCase( Locale.ROOT );
        }
        return curParts.get();
    }

    @Override
    public String charterm( AnnotationFS ann ) {
        if( !isToken( ann ) ) return NOTERM;
        Token token = (Token) ann;
        String[] parts = this.parts( token );
        if( !preFilter( parts ) ) return NOTERM;
        StringBuilder sb = tlsb.get();
        sb.delete( 0, sb.length() );
        sb.append( pos   ? parts[POSG] + SEP : "" );
        sb.append( lemma ? parts[LEMA]       : parts[TEXT] );
        return posFilter( sb.toString() );
    }

    @Override
    public byte[] payload( AnnotationFS ann ) {
        if( !isToken( ann ) ) return NODATA;
        return this.parts( (Token) ann )[POST].getBytes( CS );
    }
    
    private boolean isToken( AnnotationFS ann ) {
        // TODO: profile
        return Token.class.isAssignableFrom( ann.getClass() );
    }

    protected boolean preFilter( String[] parts ) {
        return !exclude.run( parts[POSG] );
    }

    /**
     * This method is called at the end of this class's implementation of @link{Tokenizer.charterm} 
     * in order to offer the option of creating additional string transformations over the default 
     * canonical serialization.
     * 
     * Derived classes may override this term to apply any filtering or substitutions over the 
     * standard token string form.
     * 
     * @param term The token string created by this tokenizer.
     * @return The string that will actually be passed to consumers.
     */
    protected String posFilter( String term ) {
        return term;
    }
}
