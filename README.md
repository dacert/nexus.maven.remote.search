NOTE
====

This is not the original version from https://bitbucket.org/emilianbold/maven.search.remote It is a fork of the original.
This version is to use with "Nexus Repositories Manager 3" components search api. See https://help.sonatype.com/repomanager3/rest-and-integration-api/search-api

NetBeans plugin to Search Maven Artifacts in all nexus like repositories that are ready conf in your project.

Using this plugin will save you about 1GB of disk space.


## Options

Tools > Options > Java > Nexus Maven Remote Search


## [2.0]
 -The searches are made in the repos that you have configured and not fixed to the "central" as previously
 -The api is used (NXRM) 3 instead of the (NXRM) 2
 -Allows to cancel the search if this delay, returning the results found so far.
 -Resume of the canceled searches (internal behavior via cache).
 -Configuration of cache max size, max stale and pagination.
 -Allow to clear the cache.
