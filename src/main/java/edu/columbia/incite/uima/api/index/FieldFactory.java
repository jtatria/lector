/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api.index;

import edu.columbia.incite.util.data.Datum;
import org.apache.lucene.document.Document;
import org.apache.uima.resource.Resource;

/**
 *
 * @author jta
 */
public interface FieldFactory extends Resource {

    public void updateFields( Document docInstance, Datum metadata );
    
}
