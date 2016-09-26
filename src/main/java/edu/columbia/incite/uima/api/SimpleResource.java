/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.incite.uima.api;

import org.apache.uima.resource.Resource;

/**
 *
 * @author gorgonzola
 */
public interface SimpleResource<T> {
    T get();
}
