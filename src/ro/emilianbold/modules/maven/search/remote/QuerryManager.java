/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.emilianbold.modules.maven.search.remote;

/**
 *
 * @author dacert
 */
public interface QuerryManager{
    void addQuerry(String url, int handle);
    void removeQuerry(String url);
    boolean hasQuerry(String url, int handle);
}
