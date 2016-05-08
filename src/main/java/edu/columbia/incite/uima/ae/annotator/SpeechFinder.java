/* 
 * Copyright (C) 2015 Jose Tomas Atria <jtatria@gmail.com>
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
package edu.columbia.incite.uima.ae.annotator;

import com.google.common.collect.Range;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.component.CasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.resource.ResourceInitializationException;

import edu.columbia.incite.uima.util.Types;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public class SpeechFinder extends CasAnnotator_ImplBase {

    public final static String PARAM_SECTION_TYPENAME = "sectTypeName";
    @ConfigurationParameter( name = PARAM_SECTION_TYPENAME, mandatory = false,
        description = "Type name for containers in which to look for speech" )
    private String sectTypeName;

    public final static String PARAM_BASE_TYPENAME = "baseTypeName";
    @ConfigurationParameter( name = PARAM_BASE_TYPENAME, mandatory = false,
        description = "Type name for top type of annotations to include in section indices" )
    private String baseTypeName;

    private ProcessingStatus proc;
    private Type speechType;

    @Override
    public void initialize( UimaContext ctx ) throws ResourceInitializationException {
        super.initialize( ctx );
//        sectTypeName = sectTypeName == null ? OBOMappings.getTrialTypeName() : sectTypeName;
//        baseTypeName = baseTypeName == null ? OBOMappings.getBaseTypeName() : baseTypeName;
        proc = new ProcessingStatus();
    }

    @Override
    public void process( CAS cas ) throws AnalysisEngineProcessException {
        proc.cas = cas;
        Type trialType = cas.getTypeSystem().getType( sectTypeName );
        Type baseType = cas.getTypeSystem().getType( baseTypeName );

        Types.checkTypes( cas.getTypeSystem(), sectTypeName, baseTypeName );
        
        AnnotationIndex<AnnotationFS> trials = cas.getAnnotationIndex( trialType );

        for( AnnotationFS trial : trials ) {
            System.out
                .println( "==================================================================" );
//            System.out.println( ( ( Trial ) trial ).getXmlId() );
            String trialText = trial.getCoveredText();
            int trialOffset = trial.getBegin();
            proc.trialAnns = CasUtil.selectCovered( baseType, trial );

//            List<Integer> lbs = new ArrayList<>();
//            int cur = 0;
//            while( ( cur = trialText.indexOf( "\n", cur + 1 ) ) != -1 ) {
//                lbs.add( cur );
//            }
//
//            int paraStart = 0;
//            for( Integer lb : lbs ) {
//                int paraEnd = lb + 1;
//                int[] para = new int[] { paraStart + trialOffset, paraEnd + trialOffset };
//
//                findSpeech( cas.getDocumentText(), para );
//                paraStart = paraEnd;
//            }
//            for( int[] para : CharUtils.getParagraphOffsets( trialText, trialOffset ) ) {
//                findSpeech( cas.getDocumentText(), para );
//            }
        }
    }

    private void findSpeech( String src, int[] chunk ) {
//        Type perType = proc.cas.getTypeSystem().getType( OBOMappings.getPersonTypeName() );
        Type perType = null;
//        Type offType = proc.cas.getTypeSystem().getType( OBOMappings.getOffenceTypeName() );
        Type offType = null;
//        Type verType = proc.cas.getTypeSystem().getType( OBOMappings.getVerdictTypeName() );
        Type verType = null;

        if( containsType( offType, chunk ) ) {
            System.out.println( "OFFENCE: " + src.substring( chunk[0], chunk[1] - 1 ) );
            return;
        }
        if( containsType( verType, chunk ) ) {
            System.out.println( "VERDICT: " + src.substring( chunk[0], chunk[1] - 1 ) );
            return;
        }

        String text = src.substring( chunk[0], chunk[1] );
        int offset = chunk[0];

        Matcher m;

        boolean exclude = false;

        exclude = checkExclussions( text, new String[] {
            "^(before|for|in|guilty)",
            "conducted the prosecution",
            "sworn\\.$",
            "^N\\.\\ ?B\\.?",
            "^\\w+\\b(indictment|count)",
            "cross examination"
        } );

        // First pass: paragraphs that start with "names". A name is simply defined
        // as any character [A-Za-z']. Lines starting with Mr. or Mrs. are included.
        // The line can start with either one "name" or two "names" followed by a 
        // dot. Example: 'Smith. I was walking...' or 'John Smith. I was walking...'
        // or 'Mr. John Smith. I didn't see...'. Special cases that should not be 
        // tagged are stated and not changed. An example is 'First Indictment',
        // lines starting with 'Before' etc.
        // We can't use named capturing groups, because we don't care about strings,
        // just integer offests, which we can only recover with group indices.
        m = Pattern.compile( "^"
            + "((mrs?\\. )?" // Prefix
            + "[a-z']+( [a-z']+)?)" // Speaker
            + "(, a [a-z'])?" // Descriptor
            + "\\.(.+)" // Testimony
        ).matcher( text.toLowerCase() );

        if( m.find() && !exclude ) {
            int[] pref = { m.start( 2 ) + offset, m.end( 2 ) + offset };
            int[] spkr = { m.start( 1 ) + offset, m.end( 1 ) + offset };
            int[] desc = { m.start( 4 ) + offset, m.end( 4 ) + offset };
            int[] tsty = { m.start( 5 ) + offset, m.end( 5 ) + offset };

            if( isConnectedToType( verType, chunk ) ) {
                while( isConnectedToType( verType, tsty ) ) {
                    tsty[1]--;
                }
//              proc.cas.createAnnotation(speechType, tsty[0], tsty[1] );
            }
            if( !( pref[0] < chunk[0] ) ) { // Pref is less than chunk if no pref was found.
                System.out.println( "SPEECH (1): Prefix: " + src.substring( pref[0], pref[1] ) );
            }
            System.out.println( "SPEECH (1): Speaker: " + src.substring( spkr[0], spkr[1] ) );
            if( !( desc[0] < chunk[0] ) ) { // Desc is less than chunk if no desc was found.
                System.out.println( "SPEECH (1): Descriptor: " + src.substring( desc[0], desc[1] ) );
            }
            System.out.println( "SPEECH (1): Testimony: " + src.substring( tsty[0], tsty[1] ) );
        } else {
            m = null;
            exclude = false;

            // Second pass: 'Q - A' sequences in different forms.
            m = Pattern.compile(
                "(Q\\.?\\ ?)" // Question marker
                + "(.*)" // Question
                + "(- A[.,\\ ])" // Answer marker
                + "(.*)" // Answer
            ).matcher( text );
            if( m.find() && !exclude ) {
                int[] qmrk = new int[] { m.start( 1 ) + offset, m.end( 1 ) + offset };
                int[] qstn = new int[] { m.start( 2 ) + offset, m.end( 2 ) + offset };
                int[] amrk = new int[] { m.start( 3 ) + offset, m.end( 3 ) + offset };
                int[] ansr = new int[] { m.start( 4 ) + offset, m.end( 4 ) + offset };

                if( !isContainedByType( perType, qmrk ) && !isContainedByType( perType, amrk ) ) {
//                    proc.cas.createAnnotation( speechType, qstn[0], qstn[1] );
                    System.out
                        .println( "SPEECH (2): Question: " + src.substring( qstn[0], qstn[1] ) );
//                    proc.cas.createAnnotation( speechType, ansr[0], ansr[1] );
                    System.out.println( "SPEECH (2): Answer: " + src.substring( ansr[0], ansr[1] ) );
                }
            } else {
                m = null;
                exclude = false;

                m = Pattern.compile(
                    "(.*)" // Question
                    + "(\\?\\ -A\\.?)" // Answer marker
                    + "(.*)" // Answer
                ).matcher( text );

                if( m.find() && !exclude ) {
                    int[] qstn = new int[] { m.start( 1 ), m.end( 1 ) };
                    int[] qamk = new int[] { m.start( 2 ), m.end( 2 ) };
                    int[] ansr = new int[] { m.start( 3 ), m.end( 3 ) };

                    while( isConnectedToType( perType, qstn ) ) {
                        qstn[0]++;
                    }
                    //                proc.cas.createAnnotation( speechType, qstn[0], qstn[1] );
                    System.out
                        .println( "SPEECH (3): Question: " + src.substring( qstn[0], qstn[1] ) );
                    //                proc.cas.createAnnotation( speechType, ansr[0], ansr[1] );
                    System.out.println( "SPEECH (3): Answer: " + src.substring( ansr[0], ansr[1] ) );
                } else {
                    System.out.println( "NOT SPEECH: " + src.substring( chunk[0], chunk[1] - 1 ) );
                }
            }
        }
    }

    private boolean isConnectedToType( Type type, int[] chunk ) {
        Range chunkRange = Range.closed( chunk[0], chunk[1] );

        boolean out = false;
        for( int i = 0 ; ( !out && i < proc.trialAnns.size() ) ; i++ ) {
            AnnotationFS ann = proc.trialAnns.get( i );
            if( ann.getType() != type ) {
                continue;
            }
            Range annRange = Range.closed( ann.getBegin(), ann.getEnd() );
            out = annRange.isConnected( chunkRange );
        }
        return out;
    }

    private boolean isContainedByType( Type type, int[] chunk ) {
        Range chunkRange = Range.closed( chunk[0], chunk[1] );

        boolean out = false;
        for( int i = 0 ; ( !out && i < proc.trialAnns.size() ) ; i++ ) {
            AnnotationFS ann = proc.trialAnns.get( i );
            if( ann.getType() != type ) {
                continue;
            }
            Range annRange = Range.closed( ann.getBegin(), ann.getEnd() );
            out = annRange.encloses( chunkRange );
        }
        return out;
    }

    private boolean containsType( Type type, int[] chunk ) {
        Range chunkRange = Range.closed( chunk[0], chunk[1] );

        boolean out = false;
        for( int i = 0 ; ( !out && i < proc.trialAnns.size() ) ; i++ ) {
            AnnotationFS ann = proc.trialAnns.get( i );
            if( ann.getType() != type ) {
                continue;
            }
            Range annRange = Range.closed( ann.getBegin(), ann.getEnd() );
            out = chunkRange.encloses( annRange );
        }
        return out;
    }

    private boolean checkExclussions( String text, String[] exclusions ) {
        boolean exclude = false;
        
        for( int i = 0 ; ( !exclude && i < exclusions.length ) ; i++ ) {
            String exclusion = exclusions[i];
            if( Pattern.compile( exclusion ).matcher( text.toLowerCase() ).find() ) {
                exclude = true;
            }
        }
        
        return exclude;
    }

    private class ProcessingStatus {
        List<AnnotationFS> trialAnns;
        CAS cas;

        ProcessingStatus() {
        }
    }
}
