/**
 * Copyright (c) 2018, David Cervantes Pérez
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * 
 * Copyright (c) 2016, Emilian Marius Bold
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package ro.emilianbold.modules.maven.search.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openide.util.NbBundle.Messages;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.QueryField;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.spi.GenericFindQuery;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;
import org.openide.util.NbPreferences;
import ro.emilianbold.modules.maven.search.remote.options.NexusMavenRemoteSearchPanel;

class NexusMavenGenericFindQuery implements GenericFindQuery {
    static final Pattern REPOSITORY_PATTERN = Pattern.compile("/repository/([^/]+)/");
    //"ec":["-sources.jar","-javadoc.jar",".jar",".pom"]
    static final Pattern CLASSIFIER_PATTERN = Pattern.compile("^(-([^\\.]+))*\\.(.*)$");
    
    private final OkHttpClient client;    
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    private final int maxStale;
    private final int pages;
    protected final QuerryManager manager;
    
    public NexusMavenGenericFindQuery(OkHttpClient client, QuerryManager manager) {
        this.client = client;
        this.manager = manager;
        
        Preferences pref = NbPreferences.forModule(NexusMavenRemoteSearchPanel.class);
        maxStale = pref.getInt("maxStale", 30);
        pages = pref.getInt("pages", 1);         
    }
    
    protected ResultImplementation<NBVersionInfo> find(List<QueryField> fields, final String base, final String repo){
        QueryField nameField = null;
	QueryField groupField = null;
	QueryField artifactField = null;
	QueryField versionField = null;
        
	for (QueryField field : fields) {            
	    if (QueryField.FIELD_NAME.equals(field.getField())) {
		nameField = field;
	    }
	    if (QueryField.FIELD_GROUPID.equals(field.getField())) {
		groupField = field;
	    }
	    if (QueryField.FIELD_ARTIFACTID.equals(field.getField())) {
		artifactField = field;
	    }
	    if (QueryField.FIELD_VERSION.equals(field.getField())) {
		versionField = field;
	    }
	}
        
        //See https://help.sonatype.com/repomanager3/rest-and-integration-api/search-api        
	if (nameField != null) {
            String mavenSearchURLText = String.format("%s/service/rest/v1/search?repository=%s&q=%s", base, repo, Utils.encode(nameField.getValue()));
	    return queryCentralRepository(mavenSearchURLText, repo, null, pages);
	} 
        
	if (groupField != null || artifactField != null || versionField != null) {
	    String mavenSearchURLText = String.format("%s/service/rest/v1/search?repository=%s", base, repo);
	    if (groupField != null) {
                mavenSearchURLText = String.format("%s&group=%s", mavenSearchURLText, Utils.encode(groupField.getValue()));
	    }
	    if (artifactField != null) {
                mavenSearchURLText = String.format("%s&name=%s", mavenSearchURLText, Utils.encode(artifactField.getValue()));		
	    }
	    if (versionField != null) {
		mavenSearchURLText = String.format("%s&version=%s", mavenSearchURLText, Utils.encode(versionField.getValue()));
	    }
            
	    return queryCentralRepository(mavenSearchURLText, repo, null, pages);
	}
        return Utils.emptyResult();
    }

    @Override
    public ResultImplementation<NBVersionInfo> find(List<QueryField> fields, final List<RepositoryInfo> repos) {        
        for (RepositoryInfo repo : repos) {            
            try {
                URL url = new URL(repo.getRepositoryUrl());
                String base = String.format("%s://%s", url.getProtocol(),url.getAuthority());
                String path = url.getPath();                
                Matcher matcher = REPOSITORY_PATTERN.matcher(path);
                if (matcher.matches() && matcher.group(1) != null) {                    
                    ResultImplementation<NBVersionInfo> result = find(fields, base, matcher.group(1));
                    if(result.getTotalResultCount() > 0 || cancel.get())
                        return result;
                }                
            } catch (MalformedURLException e) {
               e.printStackTrace();
            }
        }
        cancel.set(false);
	return Utils.emptyResult();
    }

    @Messages({
	"# {0} - URL",
	"query.url=Querying Nexus Maven: {0}",
	"query.parsing=Querying Nexus Maven: parsing results"
    })
    protected ResultImplementation<NBVersionInfo> queryCentralRepository(String mavenSearchURLText, String repo, String filterExtension, int pages) {       
        final AtomicBoolean partial = new AtomicBoolean(false);
        final ProgressHandle ph = ProgressHandle.createHandle(Bundle.query_url(mavenSearchURLText), () -> { 
            partial.set(true);
            cancel.set(true);      
            return true;
        });
        manager.addQuerry(mavenSearchURLText, ph.hashCode());
        
        ph.start(); 
        String continuationToken = null;
        int loop = pages;
        final List<NBVersionInfo> infos = new LinkedList<>();
        
        do {
            try {                
                String url = continuationToken !=null
                        ? String.format("%s&continuationToken=%s", mavenSearchURLText, continuationToken)
                        : mavenSearchURLText;
                
                URL u = new URL(url);
                Request okRequest = getRequest(u, pages == -1 ? maxStale : 5, 
                                        pages == -1 ? TimeUnit.DAYS : TimeUnit.MINUTES,
                                        ph.hashCode());
                
                try ( Response okResponse = client.newCall(okRequest).execute(); InputStream in = okResponse.body().byteStream()) {
                    ph.progress(Bundle.query_parsing());
                    Object parse = JSONValue.parse(new BufferedReader(new InputStreamReader(in)));
                    if (!(parse instanceof JSONObject)) {
                        if(!infos.isEmpty())
                            partial.set(true); 
                        break;
                    }
                    JSONObject searchJSON = (JSONObject) parse;
                    Object responseObj = searchJSON.get("items");
                    if (!(responseObj instanceof JSONArray)) {
                        if(!infos.isEmpty())
                            partial.set(true); 
                        break;
                    }
                    JSONArray items = (JSONArray) responseObj;
                    
                    for (int i = 0; i < items.size(); i++) {                      
                        Object docItems = items.get(i);
                        JSONObject item = (JSONObject) docItems;
                        infos.addAll(jsonToInfo(item, repo, filterExtension));
                    }
                    
                    Object paginationObj = searchJSON.get("continuationToken");                   
                    if (paginationObj instanceof String) {                        
                        continuationToken = paginationObj.toString().isEmpty() ? null : paginationObj.toString();
                    }else 
                        continuationToken = null;     
                    
                }catch (Exception e) {
                    if(!infos.isEmpty())
                        partial.set(true); 
                    break;
                }
                
            } catch (Exception e) {
                if(!infos.isEmpty())
                    partial.set(true); 
                break;
            }finally{
                loop = loop == -1 ? -1 : loop-1;
            }
        } while ((loop > 0 || loop == -1) && continuationToken != null && !cancel.get());
        
        ph.finish();
        manager.removeQuerry(mavenSearchURLText);
        return Utils.create(infos, partial.get());
    }
    
    protected Request getRequest(URL url, int maxStale, TimeUnit timeUnit, int progressHash){
        return new Request.Builder()
                    .url(url)
                    .cacheControl(new CacheControl.Builder()
                            .maxStale(maxStale, timeUnit)
                            .build())
                    .build();
    }

    private List<NBVersionInfo> jsonToInfo(JSONObject item, String repo, String filterExtension) {
	List<NBVersionInfo> infos = new ArrayList<>();

	String groupId = String.valueOf(item.get("group")); //NOI18N
	String artifactId = String.valueOf(item.get("name")); //NOI18N
	String version = String.valueOf(item.get("version"));
	
	Object assetsObj = item.get("assets"); //NOI18N
	if (assetsObj instanceof JSONArray) {
	    JSONArray assets = (JSONArray) assetsObj;
	    for (Object o : assets) {
                JSONObject asset = (JSONObject) o;                
		String path = String.valueOf(asset.get("path"));                
                String name = path;
                String[] parts = name.split("/");
                if(parts.length > 0){
                    name = parts[parts.length-1];
                    name = name.replace(String.format("%s-%s", artifactId, version), "");
                }
		Matcher matcher = CLASSIFIER_PATTERN.matcher(name);
		if (matcher.matches()) {
		    String classifier = matcher.group(2);
		    String extension = matcher.group(3);
                    
                    if( filterExtension != null && !filterExtension.equalsIgnoreCase(extension) )
                        continue;
                    
		    infos.add(new NBVersionInfo(
			    repo, //NOI18N
			    groupId, artifactId, version,
			    extension,
			    extension,
			    String.valueOf(item.get("id")), null, classifier)); //NOI18N
		}
	    }
	}

	if (infos.isEmpty()) {
	    infos.add(new NBVersionInfo(
		    repo, //NOI18N
		    groupId, artifactId, version, "jar", "jar",
		    String.valueOf(item.get("id")), null, null)); //NOI18N
	}

	return infos;
    }

    
}
