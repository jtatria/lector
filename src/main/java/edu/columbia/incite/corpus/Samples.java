/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.corpus;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author José Tomás Atria <jtatria at gmail.com>
 */
public class Samples {
    
    public static final String UNIVERSE_KEY = "UNIVERSE";
    public static final Query UNIVERSE = new MatchAllDocsQuery();
    
    public static DocSet getSample( IndexReader ir, Query q ) throws IOException {
        IndexSearcher is = new IndexSearcher( ir );
        TopDocs hits = is.search( q, ir.numDocs() );
        return new DocSet( hits, ir.numDocs() );
    }   
    
    public static DocSet complement( IndexReader ir, Query q ) throws IOException {
        BooleanQuery.Builder bldr = new BooleanQuery.Builder();
        bldr.add( UNIVERSE, BooleanClause.Occur.FILTER );
        bldr.add( q, BooleanClause.Occur.MUST_NOT );
        Query notq = bldr.build();
        IndexSearcher is = new IndexSearcher( ir );
        TopDocs hits = is.search( notq, ir.numDocs() );
        return new DocSet( hits, ir.numDocs() );
    }
}
