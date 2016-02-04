/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.res;

/**
 *
 * @author José Tomás Atria <ja2612@columbia.edu>
 */
public interface TokenProvider<T> {
    T getToken();
}
