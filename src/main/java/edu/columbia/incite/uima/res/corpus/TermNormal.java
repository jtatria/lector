/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res.corpus;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.lucene.util.automaton.RegExp;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.corpus.Entities.EntityAction;
import edu.columbia.incite.uima.api.corpus.Tokens;
import edu.columbia.incite.uima.api.corpus.Tokens.LexAction;
import edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet;
import edu.columbia.incite.uima.api.corpus.Tokens.LexClass;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.util.collection.CollectionTools;

/**
 *
 * @author gorgonzola
 */
public class TermNormal {
    
    public static final Charset CS = StandardCharsets.UTF_8;
    
    private Conf conf;

    public TermNormal() {
        this( new Conf() );
    }
    
    public TermNormal( Conf conf ) {
        this.conf = conf.commit();
    }
        
    public String term( AnnotationFS ann ) {
        String txt;
        if( Token.class.isAssignableFrom( ann.getClass() ) ) {
            Token t = (Token) ann;
            txt = conf.isLexical( t ) ? conf.lexicalAction( t ) : conf.nonLexicalAction( t );
        } else if( Span.class.isAssignableFrom( ann.getClass() ) ) {
            Span ent = (Span) ann;
            txt = conf.entityAction( ent );
        } else {
            txt = ann.getCoveredText();
        }
        
        if( txt.length() > 0 && conf.substitute( txt ) ) {
            for( LemmaSet ls : conf.lemmaSets() ) {
                if( ls.test( txt ) ) {
                    txt = conf.delete( ls ) ? "" : ls.apply( txt );
                }
            }
        }
        
        return txt;
    }
    
    public String type( AnnotationFS ann ) {
        return ann.getType().getShortName();
    }

    public byte[] data( AnnotationFS ann ) {
        if( Token.class.isAssignableFrom( ann.getClass() ) ) {
            Token t = (Token) ann;
            return t.getPos().getPosValue().getBytes( CS );
        } else {
            return ann.getType().getShortName().getBytes( CS );
        }
    }
    
    public static class Conf {
        
        private CharacterRunAutomaton isLexical;
        private CharacterRunAutomaton lexOverride;
        private CharacterRunAutomaton isLemmaSet;
        private CharacterRunAutomaton delete;
        
        private EntityAction eAction;
        private LexAction lAction;
        private NonLexAction nlAction;

        private List<LemmaSet> lemmaSets = new ArrayList<>();
        private Set<LemmaSet> deletions = new HashSet<>();
        
        public Conf setLexClasses( LexClass... classes ) {
            this.isLexical = new CharacterRunAutomaton( LexClass.make( classes ) );
            return this;
        }
        
        public Conf setLexOverrides( String... overrides ) {
            List<String> lOverride = CollectionTools.toSortedList( overrides );
            Automaton au = Automata.makeEmpty();
            for( String nlo : lOverride ) {
                RegExp rx = new RegExp( nlo );
                au = Operations.union( au, rx.toAutomaton() );
            }
            this.lexOverride = new CharacterRunAutomaton( au );
            return this;
        }
        
        public Conf setLexAction( LexAction action ) {
            this.lAction = action;
            return this;
        }
        
        public Conf setNonLexAction( NonLexAction action ) {
            this.nlAction = action;
            return this;
        }
        
        public Conf setEntityAction( EntityAction action ) {
            this.eAction = action;
            return this;
        }
        
        public Conf setLemmaSubstitutions( LemmaSet... substitute ) {
            this.isLemmaSet = new CharacterRunAutomaton( LemmaSet.make( substitute ) );
            lemmaSets.addAll( Arrays.asList( substitute ) );
            return this;
        }
        
        public Conf setLemmaDeletions( LemmaSet... delete ) {
            this.delete = new CharacterRunAutomaton( LemmaSet.make( delete ) );
            deletions.addAll( Arrays.asList( delete ) );
            return this;
        }
        
        public Conf commit() {
            if( this.isLexical == null ) {
                this.isLexical = new CharacterRunAutomaton( Automata.makeAnyString() );
            }
            if( this.lexOverride == null ) {
                this.lexOverride = new CharacterRunAutomaton( Automata.makeEmpty() );
            }
            
            if( this.isLemmaSet == null ) {
                this.isLemmaSet = new CharacterRunAutomaton( Automata.makeEmpty() );
            }
            this.lemmaSets = Collections.unmodifiableList( lemmaSets );
            
            if( this.delete == null ) {
                this.delete = new CharacterRunAutomaton( Automata.makeEmpty() );
            }
            this.deletions = Collections.unmodifiableSet( deletions );
            
            if( this.eAction == null ) {
                this.eAction = EntityAction.TYPE;
            }
            if( this.lAction == null ) {
                this.lAction = LexAction.POST;
            }
            if( this.nlAction == null ) {
                this.nlAction = NonLexAction.DELETE;
            }
            
            return this;
        }

        private boolean isLexical( Token t ) {
            return isLexical.run( Tokens.pos( t) ) || lexOverride.run( Tokens.build( t ) );
        }

        private String lexicalAction( Token t ) {
            return lAction.apply( t );
        }

        private String entityAction( Span ent ) {
            return eAction.apply( ent );
        }

        private String nonLexicalAction( Token t ) {
            return nlAction.apply( t );
        }

        private boolean substitute( String txt ) {
            return isLemmaSet.run( txt );
        }

        private List<LemmaSet> lemmaSets() {
            return lemmaSets;
        }

        private boolean delete( LemmaSet ls ) {
            return deletions.contains( ls );
        }
                
    }
}
