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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryInfo;
import org.netbeans.modules.maven.indexer.spi.ResultImplementation;
import ro.emilianbold.modules.maven.search.remote.options.CacheSizeWatcher;

public class Utils {

    public static boolean isCentral(RepositoryInfo r) {
	return !r.isLocal() && r.getRepositoryUrl() != null;
    }

    public static ResultImplementation<String> emptyString() {
	return create(new LinkedList<String>(), false);
    }

    public static ResultImplementation<NBVersionInfo> emptyResult() {
	return create(new LinkedList<NBVersionInfo>(), false);
    }

    public static <T> ResultImplementation<T> create(final List<T> contents, boolean partial) {
	return new ResultImplementation<T>() {
	    @Override
	    public boolean isPartial() {
		return partial;
	    }

	    @Override
	    public void waitForSkipped() {
		//nothing to do
	    }

	    @Override
	    public List<T> getResults() {
		return contents;
	    }

	    @Override
	    public int getTotalResultCount() {
		return getResults().size();
	    }

	    @Override
	    public int getReturnedResultCount() {
		return getResults().size();
	    }
	};
    }
    
    public static String encode(String s) {
	try {
	    return URLEncoder.encode(s, "UTF-8");
	} catch (UnsupportedEncodingException ex) {
	    //UTF-8 should always(?) be a supported encoding
	    Logger.getLogger(NexusMavenGenericFindQuery.class.getName()).log(Level.SEVERE, null, ex);
	    return "";
	}
    }
    
    public static long getCacheSize(File cacheDir){
        long size = 0;
        try {
            for (File file : cacheDir.listFiles((pathname) -> {
                    return !pathname.getName().equals("journal");
                })) {
                    size += file.length();
            }
        } catch (Exception e) {
            Logger.getLogger(CacheSizeWatcher.class.getName()).log(Level.WARNING, e.getMessage()); 
        }        
        return size;
    }
    
    public static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) 
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp-1));
    }
}
