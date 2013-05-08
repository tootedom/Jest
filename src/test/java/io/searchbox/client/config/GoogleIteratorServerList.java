package io.searchbox.client.config;

import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: dominictootell
 * Date: 07/05/2013
 * Time: 18:31
 * To change this template use File | Settings | File Templates.
 */
public class GoogleIteratorServerList implements ServerList {

    private final Set<String> servers;
    private final Iterator<String> roundRobinIterator;


    public GoogleIteratorServerList(Set<String> serverList) {
        servers = new LinkedHashSet<String>(serverList);
        roundRobinIterator = Iterators.cycle(servers);
    }

    @Override
    public synchronized String getServer() {
        if (roundRobinIterator.hasNext())
            return roundRobinIterator.next();
        throw new RuntimeException("No Server is assigned to client to connect");
    }

    @Override
    public Set getServers() {
        return new LinkedHashSet<String>(servers);  //To change body of implemented methods use File | Settings | File Templates.
    }
}
