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
 */
package ro.emilianbold.modules.maven.search.remote.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import org.openide.util.RequestProcessor;

/**
 *
 * @author dacert
 */
public class CacheSizeWatcher implements Runnable{

    private final File cacheDir;
    private final RequestProcessor RP; 
    private RequestProcessor.Task WATCH;
    public static final String SIZE_CHANGED = "sizeChange";
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    public CacheSizeWatcher(File cacheDir){
        this.cacheDir = cacheDir;
        RP = new RequestProcessor("CacheSizeWatcher", 1, true);        
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
            
    private void watchDir() {
        Long size = NexusMavenRemoteSearchPanel.getCacheSize(cacheDir);                
        try(WatchService watcher = FileSystems.getDefault().newWatchService();) {
            Path path = Paths.get(cacheDir.getAbsolutePath());
            path.register(watcher, ENTRY_CREATE, ENTRY_DELETE);

            while(!Thread.interrupted()){
                WatchKey watchKey = watcher.poll();
                if(watchKey == null){
                    continue;
                }
                List<WatchEvent<?>> events = watchKey.pollEvents(); 
                for (WatchEvent<?> event : events) { 
                    if(event.kind() == OVERFLOW)
                        continue;
                    else{
                        Long newSize = NexusMavenRemoteSearchPanel.getCacheSize(cacheDir);
                        pcs.firePropertyChange(SIZE_CHANGED, size, newSize);
                        size = newSize;                            
                    }
                }

                if(!watchKey.reset())
                    break;
            }            

        }catch(Exception e){
            Logger.getLogger(CacheSizeWatcher.class.getName()).log(Level.INFO, e.getMessage()); 
        }
    }

    public void start() {
        WATCH = RP.post(this, 0);
    }
    
    public void stop() {
        WATCH.cancel();
    }
    
    @Override
    public void run() {
        System.out.println("Start Watch");
        watchDir();
        System.out.println("Finish Watch");
    }    
}
