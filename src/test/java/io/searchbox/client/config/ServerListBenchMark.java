package io.searchbox.client.config;


import com.google.caliper.Param;



import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;

import io.searchbox.client.util.PaddedAtomicReference;



import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 *
 * Example command line options: -t 3 -i allocation -Cvm.args="-server -Xmx1024m -Xms1024m -Xmn712m" --time-limit=120s -Cinstrument.micro.options.warmup=30s -p -DnoThreads=1,2,3,4 -DnonBlocking=true,false -DnoOfServers=1,2,3,4,5
 * Results: https://microbenchmarks.appspot.com/runs/8bb077e8-70e7-4827-a152-bc399a70431c
 */
public class ServerListBenchMark extends Benchmark {

    @Param
    int noThreads = 4;

    @Param
    int noOfServers = 2;

    @Param
    boolean nonBlocking = true;

    final ScheduledExecutorService updateServerList = Executors.newScheduledThreadPool(1);

    Set<String> servers;

    final PaddedAtomicReference<ServerList> serverList = new PaddedAtomicReference<ServerList>(null);


    @Override
    protected void setUp() {

        servers = new LinkedHashSet<String>(noOfServers);
        for(int i = 0; i<noOfServers; i++) {
            servers.add("http://localhost:920" + i);
        }

        if(nonBlocking) {
            serverList.set(new RoundRobinServerList(servers));
        } else {
            serverList.set(new GoogleIteratorServerList(servers));
        }

        updateServerList.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(nonBlocking)  {
                    serverList.set(new RoundRobinServerList(new LinkedHashSet<String>(servers)));
                }  else {
                    serverList.set(new GoogleIteratorServerList(new LinkedHashSet<String>(servers)));
                }
            }
        },10000,1000, TimeUnit.MILLISECONDS);

    }

    @Override
    protected void tearDown() {
        updateServerList.shutdownNow();
    }



    public void timeGetElasticSearchServer(int reps) {

        System.out.println("reps:" + reps + " " + nonBlocking);
        System.out.println("server list:" + serverList.get().getClass().getName());
        long start = System.nanoTime();

        Thread[] threads = new Thread[noThreads];

        for (int i = 0; i < threads.length; i++)
        {
            threads[i] = new Thread(new GetServerNameRunnable(reps));
        }

        for (Thread t : threads)
        {
            t.start();
        }

        for (Thread t : threads)
        {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        long stop = System.nanoTime();
        System.out.println(stop - start);

    }

    public class GetServerNameRunnable implements Runnable {

        final int iterations;
        String server;

        public GetServerNameRunnable(int iterations) {
            this.iterations = iterations+1;
        }

        @Override
        public void run() {
            int noOfIterations = iterations;
            while(--noOfIterations !=0) {
                ServerList serverList1 = serverList.get();
                if(serverList1!=null) {
                    server = serverList1.getServer();
                }
            }

        }
    }



    public static void main(String[] args) throws Exception {
        CaliperMain.main(ServerListBenchMark.class,args);
    }

}
