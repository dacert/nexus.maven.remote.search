/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.emilianbold.modules.maven.search.remote;

import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.QueryField;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;

/**
 *
 * @author dacert
 */
public class NexusMavenFindAllQuery extends NexusMavenGenericFindQuery{    

    public NexusMavenFindAllQuery(OkHttpClient client, QuerryManager manager) {
        super(client, manager);
    }

    @Override
    protected ResultImplementation<NBVersionInfo> find(List<QueryField> fields, String base, String repo) {
        QueryField groupField = null;
        QueryField packagingField = null;
        
	for (QueryField field : fields) { 
	    if (QueryField.FIELD_GROUPID.equals(field.getField())) {
		groupField = field;
	    }
            if (QueryField.FIELD_PACKAGING.equals(field.getField())) {
		packagingField = field;
	    }
	}
        
        //See https://help.sonatype.com/repomanager3/rest-and-integration-api/search-api        
	       
        if (packagingField != null) {
            String mavenSearchURLText = String.format("%s/service/rest/v1/search?repository=%s", base, repo);           
	    return queryCentralRepository(mavenSearchURLText, repo, packagingField.getValue(), -1);	    
            
	}else if (groupField != null) {
	    String mavenSearchURLText = String.format("%s/service/rest/v1/search?repository=%s", base, repo);  
            return queryCentralRepository(mavenSearchURLText, repo, null, -1);
	}
        
        return Utils.emptyResult();
    } 
   
    @Override
    protected Request getRequest(URL url, int maxStale, TimeUnit timeUnit, int progressHash) {
        int index = url.toString().indexOf("&continuationToken=");
        String key = index != -1 ? url.toString().substring(0, index) : url.toString();        
        if(manager.hasQuerry(key, progressHash)){
            return new Request.Builder().url(url).cacheControl(new CacheControl.Builder()
                                .maxStale(maxStale, timeUnit)
                                .onlyIfCached()
                                .build())
                        .build();
        }else
            return super.getRequest(url, maxStale, timeUnit, progressHash); 
    }
}
