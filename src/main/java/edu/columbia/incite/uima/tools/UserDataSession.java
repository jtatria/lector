/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.tools;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
class UserDataSession<C,D> extends UserSession<C> {
    private D data;
    
    public void clearData() {
        this.data = null;
    }
    
    public void data( D data ) {
        this.data = data;
    }
    
    public D data() {
        return this.data;
    }
}
