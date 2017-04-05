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
import edu.columbia.incite.uima.api.corpus.Tokens.POSClass;
import edu.columbia.incite.uima.api.corpus.Tokens.NonLexAction;
import edu.columbia.incite.uima.api.types.Span;
import edu.columbia.incite.util.collection.CollectionTools;

/**
 * TermNormal objects normalize terms for printing or indexing. Instances of this class are built
 * against a given {@link Conf} instance, and define thre basic transformations:
 * {@link #term(org.apache.uima.cas.text.AnnotationFS) produces a string representation
 * corresponding to the term identity of the token represented by the given annotation.
 * {@link #type(org.apache.uima.cas.text.AnnotationFS) produces a string representation
 * corresponding to the class or category of the token represented by the given annotation.
 * {@link #data(org.apache.uima.cas.text.AnnotationFS) produces a byte array corresponding to the
 * binary payload associated to the token represented by the given annotation. Binary payloads
 * allow arbitrary data to be associated with each token.
 * @author gorgonzola
 */
public class TermNormal {

    /**
     * Charset to use for output. Common-sense indicates UTF-8 *
     */
    public static final Charset CS = StandardCharsets.UTF_8;

    private Conf conf;

    /**
     * Build TermNormal instance with default values. {
     * @see TermNormal.Conf} for details. *
     */
    public TermNormal() {
        this( new Conf() );
    }

    /**
     * Build TermNormal instance against the given {@link TermNormal.Conf} object. {
     * @see TermNormal.Conf} for options.
     * @param conf A TermNormal.Conf instance holding configuration data.
     *
     */
    public TermNormal( Conf conf ) {
        this.conf = conf.commit();
    }

    /**
     * Obtain normalized text from UIMA annotation following the rules and transformations defined
     * in this TermNormal instance's TermNormal.Conf object.
     * @param ann A UIMA annotation
     * @return A string equal to the normalized representation of the given annotation
     *
     */
    public String term( AnnotationFS ann ) {
        String txt;
        if ( Token.class.isAssignableFrom( ann.getClass() ) ) {
            Token t = (Token) ann;
            txt = conf.isLexical( t ) ? conf.lexicalAction( t ) : conf.nonLexicalAction( t );
        } else if ( Span.class.isAssignableFrom( ann.getClass() ) ) {
            Span ent = (Span) ann;
            txt = conf.entityAction( ent );
        } else {
            txt = ann.getCoveredText();
        }

        if ( txt.length() > 0 && conf.substitute( txt ) ) {
            for ( LemmaSet ls : conf.lemmaSets() ) {
                if ( ls.test( txt ) ) {
                    txt = conf.delete( ls ) ? "" : ls.apply( txt );
                }
            }
        }

        return txt;
    }

    /**
     * Obtain normalized type from UIMA annotation
     * @param ann A UIMA annotation
     * @return A string equal to the normalized type identifier
     */
    public String type( AnnotationFS ann ) {
        return ann.getType().getShortName();
    }

    /**
     * Stub: hardcoded method to obtain bytearray payloads from DKPro's token API.
     * @param ann
     * @return Bytes equal to the pos tag of a DKPro token, or the string name of the ann's type
     */
    public byte[] data( AnnotationFS ann ) {
        if ( Token.class.isAssignableFrom( ann.getClass() ) ) {
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

        /**
         * Configure POSClasses to be treated as "lexical" classes.
         * Default: all tokens are lexical.
         * @param classes POSClasses that will be considered lexical.
         * @return This Conf object.
         */
        public Conf setLexClasses( POSClass... classes ) {
            this.isLexical = new CharacterRunAutomaton( POSClass.make( classes ) );
            return this;
        }

        /**
         * Set regexp patterns that should always be considered as "lexical", independently of
         * their POSClass.
         * Patterns will be matched against the canonical string representation of Annotations.
         * For {@link de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token} instances, this is
         * equal to the the result of
         * {@link edu.columbia.incite.uima.api.corpus.Tokens#build(
         * de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)
         * }.
         * Default: no strings are overrides.
         * @param overrides Regexp patterns for terms that are always lexical
         * @return This Conf object.
         */
        public Conf setLexOverrides( String... overrides ) {
            List<String> lOverride = CollectionTools.toSortedList( overrides );
            Automaton au = Automata.makeEmpty();
            for ( String nlo : lOverride ) {
                RegExp rx = new RegExp( nlo );
                au = Operations.union( au, rx.toAutomaton() );
            }
            this.lexOverride = new CharacterRunAutomaton( au );
            return this;
        }

        /**
         * Set the given {@link LexAction} for transforming lexical tokens.
         * Default: {@link Tokens.LexAction#POST}.
         * @param action A {@link LexAction} value.
         * @return This Conf object.
         */
        public Conf setLexAction( LexAction action ) {
            this.lAction = action;
            return this;
        }

        /**
         * Set the given {@link NonLexAction} for transforming non lexical tokens.
         * Degfault: {@link Tokens.NonLexAction#DELETE}.
         * @param action A {@link NonLexAction} value.
         * @return This Conf object.
         */
        public Conf setNonLexAction( NonLexAction action ) {
            this.nlAction = action;
            return this;
        }

        /**
         * Set the given {@link EntityAction} for transforming entity annotations.
         * Default: {@link EntityAction#TYPE}.
         * @param action A {@link EntityAction} value.
         * @return This Conf object.
         */
        public Conf setEntityAction( EntityAction action ) {
            this.eAction = action;
            return this;
        }

        /**
         * Configure the given {@link LemmaSet} for replacement with the corresponding marker.
         * See {@link edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet} for values.
         * Default: No lemma sets will be replaced.
         * @param substitute {@link LemmaSet} values that will be marked.
         * @return This Conf object.
         */
        public Conf setLemmaSubstitutions( LemmaSet... substitute ) {
            this.isLemmaSet = new CharacterRunAutomaton( LemmaSet.make( substitute ) );
            lemmaSets.addAll( Arrays.asList( substitute ) );
            return this;
        }

        /**
         * Configure the given {@link LemmaSet} for deletion.
         * See {@link edu.columbia.incite.uima.api.corpus.Tokens.LemmaSet} for values.
         * Default: No lemma sets will be deleted.
         * @param delete {@link LemmaSet} values that will be deleted.
         * @return This Conf object.
         */
        public Conf setLemmaDeletions( LemmaSet... delete ) {
            this.delete = new CharacterRunAutomaton( LemmaSet.make( delete ) );
            deletions.addAll( Arrays.asList( delete ) );
            return this;
        }

        /**
         * Commit the values curently set in this Conf object.
         * This method will replace all non-set values with their defaults, compile all the
         * necessary automata for string texts and prepare this Conf for construction of a
         * {@link TermNormal} instance.
         * @return This Conf object.
         */
        public Conf commit() {
            if ( this.isLexical == null ) {
                this.isLexical = new CharacterRunAutomaton( Automata.makeAnyString() );
            }
            if ( this.lexOverride == null ) {
                this.lexOverride = new CharacterRunAutomaton( Automata.makeEmpty() );
            }

            if ( this.isLemmaSet == null ) {
                this.isLemmaSet = new CharacterRunAutomaton( Automata.makeEmpty() );
            }
            this.lemmaSets = Collections.unmodifiableList( lemmaSets );

            if ( this.delete == null ) {
                this.delete = new CharacterRunAutomaton( Automata.makeEmpty() );
            }
            this.deletions = Collections.unmodifiableSet( deletions );

            if ( this.eAction == null ) {
                this.eAction = EntityAction.TYPE;
            }
            if ( this.lAction == null ) {
                this.lAction = LexAction.POST;
            }
            if ( this.nlAction == null ) {
                this.nlAction = NonLexAction.DELETE;
            }

            return this;
        }

        private boolean isLexical( Token t ) {
            return isLexical.run( Tokens.pos( t ) ) || lexOverride.run( Tokens.build( t ) );
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
