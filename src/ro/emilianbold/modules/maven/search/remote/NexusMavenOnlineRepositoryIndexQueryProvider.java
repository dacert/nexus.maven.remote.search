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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.QueryField;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.spi.ArchetypeQueries;
import org.netbeans.modules.maven.indexer.spi.BaseQueries;
import org.netbeans.modules.maven.indexer.spi.ChecksumQueries;
import org.netbeans.modules.maven.indexer.spi.ClassUsageQuery;
import org.netbeans.modules.maven.indexer.spi.ClassesQuery;
import org.netbeans.modules.maven.indexer.spi.ContextLoadedQuery;
import org.netbeans.modules.maven.indexer.spi.DependencyInfoQueries;
import org.netbeans.modules.maven.indexer.spi.GenericFindQuery;
import org.netbeans.modules.maven.indexer.spi.RepositoryIndexQueryProvider;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;
import org.openide.modules.Places;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import ro.emilianbold.modules.maven.search.remote.options.NexusMavenRemoteSearchPanel;

@ServiceProvider(service = RepositoryIndexQueryProvider.class, position = 100)
public class NexusMavenOnlineRepositoryIndexQueryProvider implements RepositoryIndexQueryProvider, QuerryManager {

    private final static int CONNECTION_TIMEOUT = 10 * 1000; //NOI18N
    private final static int READ_TIMEOUT = 10 * 1000; //NOI18N
    public final static int CACHE_SIZE_MB = 50 * 1024 * 1024;  //NOI18N    
    
    private final OkHttpClient client;
    private final OkHttpClient index_client;
    
    private final LinkedHashMap<String, Integer> queryMap = new LinkedHashMap<>();

    public NexusMavenOnlineRepositoryIndexQueryProvider() {
        File cacheFolder = Places.getCacheSubdirectory("nexus.maven.remote.search/okhttpcache"); //NOI18N   
        File index_cacheFolder = Places.getCacheSubdirectory("nexus.maven.remote.search/indexcache"); //NOI18N 
        
        Cache cache = new Cache(cacheFolder, CACHE_SIZE_MB);
        //reset cache on start
        try {
            cache.evictAll();            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .cache(cache)
                .build();   
                
        Preferences pref = NbPreferences.forModule(NexusMavenRemoteSearchPanel.class);        
        Integer size = pref.getInt("maxSize", 100);
        index_client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .cache(new Cache(index_cacheFolder, size * 1204 * 1204))
                .build();
    }
    
    

    @Override
    public boolean handlesRepository(RepositoryInfo r) {
	return Utils.isCentral(r);
    }

    @Override
    public GenericFindQuery getGenericFindQuery() {        
        return new NexusMavenGenericFindQuery(client, this);
    }
    
    public GenericFindQuery getGroupsFindQuery() {   
        return new NexusMavenFindAllQuery(index_client, this);       
    }

    @Override
    public BaseQueries getBaseQueries() {
	return new BaseQueries() {
	    @Override
	    public ResultImplementation<NBVersionInfo> getRecords(String groupId, String artifactId, String version, List<RepositoryInfo> repos) {
		List<QueryField> query = new ArrayList<>();
		if (groupId != null && !groupId.isEmpty()) {
		    QueryField qf = new QueryField();
		    qf.setField(QueryField.FIELD_GROUPID);
		    qf.setValue(groupId);

		    query.add(qf);                    
		}
		if (artifactId != null && !artifactId.isEmpty()) {
		    QueryField qf = new QueryField();
		    qf.setField(QueryField.FIELD_ARTIFACTID);
		    qf.setValue(artifactId);

		    query.add(qf);
		}
		if (version != null && !version.isEmpty()) {
		    QueryField qf = new QueryField();
		    qf.setField(QueryField.FIELD_VERSION);
		    qf.setValue(version);

		    query.add(qf);
		}
		return getGenericFindQuery().find(query, repos);
	    }

	    @Override
	    public ResultImplementation<NBVersionInfo> getVersions(String groupId, String artifactId, List<RepositoryInfo> repos) {
		return getRecords(groupId, artifactId, null, repos);
	    }

	    @Override
	    public ResultImplementation<String> getGroups(List<RepositoryInfo> repos) {
		QueryField qf = new QueryField();
		qf.setField(QueryField.FIELD_GROUPID);

		ResultImplementation<NBVersionInfo> records = getGroupsFindQuery().find(Collections.singletonList(qf), repos);

		List<String> groups = records.getResults()
			.stream()
			.map((NBVersionInfo info) -> info.getGroupId())
			.distinct()
			.collect(Collectors.toList());

		return Utils.create(groups, records.isPartial());
	    }

	    @Override
	    public ResultImplementation<String> getArtifacts(String groupId, List<RepositoryInfo> repos) {
		ResultImplementation<NBVersionInfo> records = getRecords(groupId, null, null, repos);

		List<String> artifacts = records.getResults()
			.stream()
			.map((NBVersionInfo info) -> info.getArtifactId())
			.distinct()
			.collect(Collectors.toList());

		return Utils.create(artifacts, records.isPartial());
	    }

	    @Override
	    public ResultImplementation<String> filterPluginGroupIds(String prefix, List<RepositoryInfo> repos) {
		//TODO: it would be better to to a filtered search directly
		ResultImplementation<String> groups = getGroups(repos);
		List<String> filtered = groups.getResults()
			.stream()
			.filter(s -> s.startsWith(prefix))
			.distinct()
			.collect(Collectors.toList());
		return Utils.create(filtered, groups.isPartial());
	    }

	    @Override
	    public ResultImplementation<String> filterPluginArtifactIds(String groupId, String prefix, List<RepositoryInfo> repos) {
		//TODO: it would be better to to a filtered search directly
		ResultImplementation<String> artifacts = getArtifacts(groupId, repos);
		List<String> filtered = artifacts.getResults()
			.stream()
			.filter(s -> s.startsWith(prefix))
			.collect(Collectors.toList());
		return Utils.create(filtered, artifacts.isPartial());
	    }

	    @Override
	    public ResultImplementation<String> getGAVsForPackaging(String packaging, List<RepositoryInfo> repos) {
		QueryField qf = new QueryField();
		qf.setField(QueryField.FIELD_PACKAGING);
		qf.setValue(packaging);

		ResultImplementation<NBVersionInfo> records = getGroupsFindQuery().find(Collections.singletonList(qf), repos);

		List<String> gavs = records.getResults()
			.stream()
			.map((NBVersionInfo info) -> info.getGroupId() + ":" + info.getArtifactId() + ":" + info.getVersion()) //NOI18N
			.distinct()
			.collect(Collectors.toList());

		return Utils.create(gavs, records.isPartial());
	    }
	};
    }

    @Override
    public ArchetypeQueries getArchetypeQueries() {
	return null;
    }

    @Override
    public ChecksumQueries getChecksumQueries() {
	return null;
    }

    @Override
    public ClassUsageQuery getClassUsageQuery() {
	return null;
    }

    @Override
    public ClassesQuery getClassesQuery() {
	return new ClassesQuery() {
            @Override
            public ResultImplementation<NBVersionInfo> findVersionsByClass(final String string, List<RepositoryInfo> repos) {                
                QueryField qf = new QueryField();
		qf.setField(QueryField.FIELD_GROUPID);

		ResultImplementation<NBVersionInfo> records = getGroupsFindQuery().find(Collections.singletonList(qf), repos);
                
                AtomicInteger maxOccur = new AtomicInteger(0);
                List<QueryResultItem> pre_filter = records.getResults()
			.stream()
                        .distinct()
                        .map((NBVersionInfo info) -> new QueryResultItem(info))
			.filter((QueryResultItem qr) -> {   
                                int occur = 0;
                                String class_name = String.join(".",string.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"));
                                String[] parts = class_name.replace("..", ".").split("[.]|[-]|[_]");
                                for (String part : parts) {
                                    if(qr.getInfo().getGroupId().contains(part) || qr.getInfo().getArtifactId().contains(part))
                                        occur++;
                                }
                                qr.setOccur(occur);
                                maxOccur.set(maxOccur.get() < occur ? occur : maxOccur.get());
                                return occur > 0;
                            })                        
                        .collect(Collectors.toList());
                
                List<NBVersionInfo> filtered = pre_filter.stream()
                        .filter((QueryResultItem qr) -> qr.getOccur() == maxOccur.get() )
                        .map((QueryResultItem qr) ->  qr.getInfo())
                        .collect(Collectors.toList());

		return Utils.create(filtered, records.isPartial());
            }
        };
    }

    @Override
    public ContextLoadedQuery getContextLoadedQuery() {
	return null;
    }

    @Override
    public DependencyInfoQueries getDependencyInfoQueries() {
	return null;
    }

    @Override
    public void addQuerry(String url, int handle) {
        if(!queryMap.containsKey(url))
            queryMap.put(url, handle);
}

    @Override
    public void removeQuerry(String url) {
        queryMap.remove(url);
    }

    @Override
    public boolean hasQuerry(String url, int handle) {
        return queryMap.containsKey(url) && queryMap.get(url) != handle;
    }
}
