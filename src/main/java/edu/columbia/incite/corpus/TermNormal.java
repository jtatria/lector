/* 
 * Copyright (C) 2017 José Tomás Atria <jtatria at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.columbia.incite.corpus;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.Operations;
import org.apache.uima.cas.text.AnnotationFS;

import edu.columbia.incite.uima.api.types.Entities;
import edu.columbia.incite.uima.api.types.Entities.EntityAction;
import edu.columbia.incite.uima.api.types.Tokens;
import edu.columbia.incite.uima.api.types.Tokens.LexAction;
import edu.columbia.incite.uima.api.types.Tokens.NonLexAction;

/**
 * Term normalization procedures. Instances of this class control thew way in UIMA annotations are 
 * transformed into normalized term forms, by applying a combination of actions, substitutions and 
 * deletions to the values covered by the annotation or contained in its feature values. This class
 * makes no assumptions about the actual UIMA type of annotations, but distinguishes between Token 
 * and Entity annotations. See {@link Tokens} and {@link Entities} for available actions. Custom 
 * actions are not supported by this class yet. Additional transformations of the string value 
 * resulting from the application of token and entity actions are obtained by applying 
 * substitutions and deletions to a collection of {@link LemmaSet} instances.
 * 
 * All values for actions, substitutions and deletions are given to objects of this class through 
 * the construction of a {@link TermNormal.Conf} object. See that class's documentation for details.
 * This class also implements additional methods for the computation of extra attributes from an 
 * annotation instance. See {@link #type(org.apache.uima.cas.text.AnnotationFS) } and 
 * {@link #data(org.apache.uima.cas.text.AnnotationFS) } for details.
 * 
 * In a way, this class implements equivalent functionality to a Lucene analyzer, in a very naive 
 * and simple way.
 *
 * TODO: refactor using generics, allow for custom transformations. Conform to Lucene's analysis API?
 * 
 * @author José Tomás Atria <jtatria@gmail.com>
 */
public class TermNormal {
    
    /** Charset used for byte representation of strings produced by this class **/
    public static final Charset CS = StandardCharsets.UTF_8;
    
    private final Conf conf;

    /** Build a TermNormal instance with default values **/
    public TermNormal() {
        this( new Conf() );
    }
    
    /** Build a TermNormal instance with the given Conf values **/
    public TermNormal( Conf conf ) {
        this.conf = conf;
    }
    
    /**
     * Obtain the normalized string value corresponding to the given annotation from the 
     * configured actions, substitutions and deletions for this TermNormal.
     * 
     * The given annotation is first subject to thos TermNormal's configured {@emph actions}, and 
     * then checked against the LemmaSet instances configured for deletion or substitution. Note 
     * that this means that the resulting value can be the empty string, soo clients should check 
     * the returned value before proceeding with further processing (e.g. to avoid indexing empty 
     * terms, etc).
     * 
     * 'Token' annotations are categorized as lexical or non-lexical, in order to apply the 
     * configured {@link LexAction} or {@link NonLexAction}, depending. See 
     * {@link Conf.isLexical(AnnotationFS)} for details on lexicality determination.
     * 
     * 'Entity' annotations are subject to the configured {@link EntityAction}.
     * Annotations that can not be reliably categorized as either Tokens or Entities have the value 
     * of their covering text. See {@link Tokens.isToken(AnnotationFS)} and 
     * {@link Entities.isEntity(AnnotationFS)} for details of the annotation classification 
     * procedure.
     * Substitutions and deletions are matched against the string value of the result of the 
     * configured actions. See {@link LemmaSet} for details of ex-post transformations.
     * 
     * @param ann A UIMA annotation.
     * 
     * @return A normalized string term value corresponding to the given annotation.
     */
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
        
        txt = conf.applySubstitutions( txt );

        return txt;
    }
    
    /**
     * Obtain a value suitable for indexing as this annotation's 'type'. By default, this wull be 
     * equal to the actual UIMA type's short name, but clients may define 'type' in different ways 
     * and override this method in consequence.
     * 
     * @param ann A UIMA annotation.
     * 
     * @return A string value suitable for identification of the given annotation's 'type'.
     */
    public String type( AnnotationFS ann ) {
        return ann.getType().getShortName();
    }
    
    /** 
     * Obtain a byte[] array with arbitrary data. By default, this implementation returns the byte 
     * representation of the POS tag string for Tokens and the short type name for Entities. 
     * Clients may encode other kinds of data in the returned byte array and override this method 
     * in consequence. Bytes are produced against the curent value of {@link TermNormal.CS}.
     * 
     * @param ann A UIMA annotation
     * 
     * @return A byte array encoding arbitrary data. By default, the byte representation of the 
     * string value of Tokens' POS tags and Entities' UIMA tye short name.
     */
    public byte[] data( AnnotationFS ann ) {
        if( Tokens.isToken( ann ) ) {
            return Tokens.posT( ann ).getBytes( CS );
        } else {
            return ann.getType().getShortName().getBytes( CS );
        }
    }
   
    @Override
    public String toString() {
        return this.conf.toString();
    }
    
    /**
     * Configuration objects for TermNormal instances.
     * Construction of this class's instances follow a fluent interface:
     * {@code
     *      Conf conf = new TermNormal.Conf()
     *          .setLexClasses( new POSClass[]{} )
     *          .setNonLexClasses( POSClass[]{} )
     *          .setLexicalAction( LexicalAction.SOME_ACTION ) // etc.
     *          .finish(); // <- necessary for parameter sanity checks.
     *      TermNormal tn = new TermNormal( conf );
     * }
     */
    public static class Conf {
        
        /** Default Lexical Token Action: Lemmatize **/
        public static final LexAction    DFLT_LEX_ACTION     = LexAction.LEMMATIZE;
        /** Default Non-Lexical Token Action: Delete **/
        public static final NonLexAction DFLT_NON_LEX_ACTION = NonLexAction.DELETE;
        /** Default Entity Action: Replace by their type **/
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append( String.format( "Lexical Classes:\t%s\n", Arrays.toString( lexClasses ) ) );
            sb.append( String.format( "Lexical Action:\t%s\n", lAction.name() ) );
            sb.append( String.format( "Non-Lexical Action:\t%s\n", nlAction.name() ) );
            sb.append( String.format( "Entity Action:\t%s\n", eAction.name() ) );
            sb.append( String.format( "Overrides:\t%s\n", Arrays.toString( overrides ) ) );
            sb.append( String.format( "Substitutions:\t%s\n", Arrays.toString( substitutions ) ) );
            sb.append( String.format( "Deletions:\t%s\n", Arrays.toString( deletions ) ) );
            
            return sb.toString();
        }
                
        /** 
         * Set the given {@link POSClass POS classes} as 'lexical'.
         * @param classes An array of POSClass values.
         * @return this Conf instance.
         */
        public Conf setLexClasses( POSClass... classes ) {
            this.lexClasses = classes;
            return this;
        }

        /** 
         * Set the given patterns as lexical overrides for for identifying tokens that should 
         * always be considered lexical. Patterns in this array will be checked against the strings 
         * produced by either {@link Tokens.build(AnnotationFS)} or 
         * {@link Entities.build(AnnotaitonFS)}, in the order specified in the given array.
         * 
         * @param overrides A string array with patterns to test against the strings built by 
         *                  Tokens and Entities.
         * 
         * @return this Conf instance.
         */
        public Conf setLexicalOverrides( String... overrides ) {
            this.overrides = overrides;
            return this;
        }
        
        /**
         * Apply the given {@link LexicalAction action} to lexical tokens.
         * @param lAction A LexicalAction value.
         * @return this Conf instance.
         */
        public Conf setLexicalAction( LexAction lAction ) {
            this.lAction = lAction;
            return this;
        }

        /**
         * Apply the given {@link NonLexAction action} to non-lexical tokens.
         * @param nlAction A NonLexAction value.
         * @return this Conf instance.        
         */
        public Conf setNonLexicalAction( NonLexAction nlAction ) {
            this.nlAction = nlAction;
            return this;
        }
        
        /**
         * Apply the given {@link EntityAction action} to entities..
         * @param eAction An EntityAction value.
         * @return this Conf instance.
         */
        public Conf setEntityAction( EntityAction eAction ) {
            this.eAction = eAction;
            return this;
        }
        
        /**
         * Set the given {@link LemmaSet lemma sets} as substitutions.
         * @param substs An array of LemmaSet instances.
         * @return this Conf instance.
         */
        public Conf setLemmaSubstitutions( LemmaSet... substs ) {
            this.substitutions = substs;
            return this;
        }

        /**
         * Set the given {@link LemmaSet lemma sets} as deletions.
         * @param deletions An array of LemmaSet instances.
         * @return this Conf instance.
         */        
        public Conf setLemmaDeletions( LemmaSet... deletions ) {
            this.deletions = deletions;
            return this;
        }
        
        /**
         * Finalize this configuration object and check sanity of given values, replace non-given 
         * values by their defaults, etc.
         * This instance should not be given any new configuration values after calls to finish(), 
         * but the default implementation does not enforce this restriction. The results of 
         * configuration changes after calls to this methods is undefined.
         * 
         * @return this Conf instance.
         */
        public Conf commit() {
            
            this.lAction  = lAction == null   ? DFLT_LEX_ACTION     : lAction;
            this.nlAction = nlAction == null  ? DFLT_NON_LEX_ACTION : nlAction;
            this.eAction  = eAction == null   ? DFLT_ENTITY_ACTION  : eAction;
            
            this.lexClasses = ( lexClasses == null || lexClasses.length <= 0 ) ?
                              Tokens.ALL_CLASSES :
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
            return lexical.run( Tokens.posT( ann ) ) || override.run( Tokens.build( ann ) );
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
        
        @Override
        public Conf clone() {
            return clone( this );
        }
        
        public static Conf clone( Conf other ) {
            Conf out = new Conf();
            out.delete        = other.delete;
            out.deletions     = other.deletions;
            out.eAction       = other.eAction;
            out.lAction       = other.lAction;
            out.lexClasses    = other.lexClasses;
            out.lexical       = other.lexical;
            out.nlAction      = other.nlAction;
            out.override      = other.override;
            out.overrides     = other.overrides;
            out.substitute    = other.substitute;
            out.substitutions = other.substitutions;
            return out;
        }
    }
}
