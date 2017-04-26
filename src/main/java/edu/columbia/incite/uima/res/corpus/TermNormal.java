/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.corpus.Entities;
import edu.columbia.incite.uima.api.corpus.Entities.EntityAction;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.api.corpus.POSClass;

/**
 *
 * @author gorgonzola
 */
public class TermNormal {
    
    public static final Charset CS = StandardCharsets.UTF_8;
    
    private final Conf conf;
    
    public TermNormal() {
        this( new Conf() );
    }
    
    public TermNormal( Conf conf ) {
        this.conf = conf;
    }
    
    public String term( AnnotationFS ann ) {
        String txt;
        if( Tokens.isToken( ann ) ) {
            txt = conf.isLexical( ann ) ? conf.lexicalAction( ann ) : conf.nonLexicalAction( ann );
        } else if( Entities.isEntity( ann ) ) {
            txt = conf.entityAction( ann );
        } else {
            txt = ann.getCoveredText();
        }
        
        if( conf.delete( txt ) ) return "";
        
        return conf.applySubstitutions( txt );
    }
    
    public String type( AnnotationFS ann ) {
        return ann.getType().getShortName();
    }
    
    public byte[] data( AnnotationFS ann ) {
        if( Tokens.isToken( ann ) ) {
            return Tokens.posT( ann ).getBytes( CS );
        } else {
            return ann.getType().getShortName().getBytes( CS );
        }
    }
    
    public static class Conf {
        
        public static final LexAction DFLT_LEX_ACTION        = LexAction.LEMMATIZE;
        public static final NonLexAction DFLT_NON_LEX_ACTION = NonLexAction.DELETE;
        public static final EntityAction DFLT_ENTITY_ACTION  = EntityAction.TYPE;
        
        private LexAction lAction;
        private NonLexAction nlAction;
        private EntityAction eAction;
        
        private CharacterRunAutomaton lexical;
        private CharacterRunAutomaton override;
        private CharacterRunAutomaton delete;
        private CharacterRunAutomaton substitute;
        
        private POSClass[] lexClasses;
        private String[] overrides;
        private LemmaSet[] substitutions;
        private LemmaSet[] deletions;
        
        public Conf setLexClasses( POSClass... classes ) {
            this.lexClasses = classes;
            return this;
        }
        
        public Conf setLexicalOverrides( String... overrides ) {
            this.overrides = overrides;
            return this;
        }
        
        public Conf setLexicalAction( LexAction lAction ) {
            this.lAction = lAction;
            return this;
        }
        
        public Conf setNonLexicalAction( NonLexAction nlAction ) {
            this.nlAction = nlAction;
            return this;
        }
        
        public Conf setEntityAction( EntityAction eAction ) {
            this.eAction = eAction;
            return this;
        }
        
        public Conf setLemmaSubstitutions( LemmaSet... substs ) {
            this.substitutions = substs;
            return this;
        }
        
        public Conf setLemmaDeletions( LemmaSet... deletions ) {
            this.deletions = deletions;
            return this;
        }
        
        public Conf commit() {
            
            this.lAction  = lAction == null   ? DFLT_LEX_ACTION     : lAction;
            this.nlAction = nlAction == null  ? DFLT_NON_LEX_ACTION : nlAction;
            this.eAction  = eAction == null   ? DFLT_ENTITY_ACTION  : eAction;
            
            this.lexClasses = ( lexClasses == null || lexClasses.length <= 0 ) ?
                              POSClass.ALL_CLASSES :
                              lexClasses;
            
            this.lexical = new CharacterRunAutomaton( POSClass.make( lexClasses ) );
            
            this.overrides = overrides == null ?
                             new String[]{} :
                             overrides;
            
            Automaton orAu = Automata.makeEmpty();
            Arrays.sort( overrides );
            for( String or : overrides ) {
                Automaton i = Automata.makeString( or );
                Operations.union( orAu, i );
            }
            this.override = new CharacterRunAutomaton( orAu );
            
            
            this.substitutions = substitutions == null ? new LemmaSet[]{} : substitutions;
            this.substitute = new CharacterRunAutomaton( LemmaSet.make( substitutions ) );

            this.deletions = deletions == null ? new LemmaSet[]{} : deletions;
            this.delete = new CharacterRunAutomaton( LemmaSet.make( deletions ) );
            
            return this;
        }

        private boolean isLexical( AnnotationFS ann ) {
            return lexical.run( Tokens.pos( ann ) ) || override.run( Tokens.build( ann ) );
        }

        private String lexicalAction( AnnotationFS ann ) {
            return lAction.apply( ann );
        }

        private String nonLexicalAction( AnnotationFS ann ) {
            return nlAction.apply( ann );
        }

        private String entityAction( AnnotationFS ann ) {
            return eAction.apply( ann );
        }
        
        private boolean delete( String txt ) {
            return this.delete.run( txt );
        }
        
        private String applySubstitutions( String txt ) {
            if( !this.substitute.run( txt ) ) return txt;
            String t = txt;
            for( LemmaSet ls : this.substitutions ) {
                t = ls.apply( t );
            }
            return t;
        }
    }
}
