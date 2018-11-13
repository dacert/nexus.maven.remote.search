/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.emilianbold.modules.maven.search.remote;

import org.netbeans.modules.maven.indexer.api.NBVersionInfo;

/**
 *
 * @author dacert
 */
public class QueryResultItem{
    
    private NBVersionInfo info;
    private int occur;

    public QueryResultItem(NBVersionInfo info, int occur) {
        this.info = info;
        this.occur = occur;
    }

    public QueryResultItem(NBVersionInfo info) {
        this(info, 0);
    }

    /**
     * @return the info
     */
    public NBVersionInfo getInfo() {
        return info;
    }

    /**
     * @return the occur
     */
    public int getOccur() {
        return occur;
    }

    /**
     * @param occur the occur to set
     */
    public void setOccur(int occur) {
        this.occur = occur;
    }
    
}
