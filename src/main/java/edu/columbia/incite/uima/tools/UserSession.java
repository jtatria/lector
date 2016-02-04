/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
class UserSession<C> {
    private C conf;

    public void clearConf() {
        conf = null;
    }
    
    public void conf( C conf ) {
        this.conf = conf;
    }
    
    public C conf() {
        return this.conf;
    }
    
}
