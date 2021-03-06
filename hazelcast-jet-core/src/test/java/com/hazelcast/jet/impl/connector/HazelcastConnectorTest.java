/*
 * Copyright (c) 2008-2017, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.jet.impl.connector;

import com.hazelcast.cache.ICache;
import com.hazelcast.cache.journal.EventJournalCacheEvent;
import com.hazelcast.config.CacheSimpleConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.EventJournalConfig;
import com.hazelcast.jet.JetInstance;
import com.hazelcast.jet.config.JetConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.stream.IStreamCache;
import com.hazelcast.jet.stream.IStreamList;
import com.hazelcast.jet.stream.IStreamMap;
import com.hazelcast.jet.stream.JetCacheManager;
import com.hazelcast.map.journal.EventJournalMapEvent;
import com.hazelcast.projection.Projections;
import com.hazelcast.query.Predicates;
import com.hazelcast.test.HazelcastParallelClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.hazelcast.jet.core.Edge.between;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeCacheP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeListP;
import static com.hazelcast.jet.core.processor.SinkProcessors.writeMapP;
import static com.hazelcast.jet.core.processor.SourceProcessors.readCacheP;
import static com.hazelcast.jet.core.processor.SourceProcessors.readListP;
import static com.hazelcast.jet.core.processor.SourceProcessors.readMapP;
import static com.hazelcast.jet.core.processor.SourceProcessors.streamCacheP;
import static com.hazelcast.jet.core.processor.SourceProcessors.streamMapP;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(QuickTest.class)
@RunWith(HazelcastParallelClassRunner.class)
public class HazelcastConnectorTest extends JetTestSupport {

    private static final int ENTRY_COUNT = 100;

    private JetInstance jetInstance;

    private String sourceName;
    private String sinkName;

    private String streamSourceName;
    private String streamSinkName;

    @Before
    public void setup() {
        JetConfig jetConfig = new JetConfig();
        Config hazelcastConfig = jetConfig.getHazelcastConfig();
        hazelcastConfig.addCacheConfig(new CacheSimpleConfig().setName("*"));
        hazelcastConfig.addEventJournalConfig(new EventJournalConfig().setCacheName("stream*").setMapName("stream*"));
        jetInstance = createJetMember(jetConfig);
        JetInstance jetInstance2 = createJetMember(jetConfig);

        sourceName = randomString();
        sinkName = randomString();

        streamSourceName = "stream" + sourceName;
        streamSinkName = "stream" + sinkName;

        // workaround for `cache is not created` exception, create cache locally on all nodes
        JetCacheManager cacheManager = jetInstance2.getCacheManager();
        cacheManager.getCache(sourceName);
        cacheManager.getCache(sinkName);
        cacheManager.getCache(streamSourceName);
        cacheManager.getCache(streamSinkName);

    }

    @Test
    public void when_readMap_and_writeMap() throws ExecutionException, InterruptedException {
        IStreamMap<Integer, Integer> sourceMap = jetInstance.getMap(sourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceMap.put(i, i));

        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", readMapP(sourceName));
        Vertex sink = dag.newVertex("sink", writeMapP(sinkName));

        dag.edge(between(source, sink));

        jetInstance.newJob(dag).join();

        assertEquals(ENTRY_COUNT, jetInstance.getMap(sinkName).size());
    }

    @Test
    public void when_readMap_withNativePredicateAndProjection() throws ExecutionException, InterruptedException {
        IStreamMap<Integer, Integer> sourceMap = jetInstance.getMap(sourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceMap.put(i, i));

        DAG dag = new DAG();
        Vertex source = dag.newVertex("source",
                readMapP(sourceName,
                        Predicates.greaterThan("this", "0"),
                        Projections.singleAttribute("value")
                )
        );
        Vertex sink = dag.newVertex("sink", writeListP(sinkName));

        dag.edge(between(source, sink));

        jetInstance.newJob(dag).join();

        IStreamList<Object> list = jetInstance.getList(sinkName);
        assertEquals(ENTRY_COUNT - 1, list.size());
        assertFalse(list.contains(0));
        assertTrue(list.contains(1));
    }

    @Test
    public void when_readMap_withPredicateAndDistributedFunction() throws ExecutionException, InterruptedException {
        IStreamMap<Integer, Integer> sourceMap = jetInstance.getMap(sourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceMap.put(i, i));

        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", readMapP(sourceName, e -> !e.getKey().equals(0), Map.Entry::getKey));
        Vertex sink = dag.newVertex("sink", writeListP(sinkName));

        dag.edge(between(source, sink));

        jetInstance.newJob(dag).join();

        IStreamList<Object> list = jetInstance.getList(sinkName);
        assertEquals(ENTRY_COUNT - 1, list.size());
        assertFalse(list.contains(0));
        assertTrue(list.contains(1));
    }

    @Test
    public void when_streamMap() throws ExecutionException, InterruptedException {
        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", streamMapP(streamSourceName, false));
        Vertex sink = dag.newVertex("sink", writeListP(streamSinkName));

        dag.edge(between(source, sink));

        Future<Void> future = jetInstance.newJob(dag).getFuture();

        IStreamMap<Integer, Integer> sourceMap = jetInstance.getMap(streamSourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceMap.put(i, i));

        assertSizeEventually(ENTRY_COUNT, jetInstance.getList(streamSinkName));
        future.cancel(true);
    }

    @Test
    public void when_streamMap_withFilterAndProjection() throws ExecutionException, InterruptedException {
        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", streamMapP(streamSourceName,
                event -> !event.getKey().equals(0), EventJournalMapEvent::getKey, false));
        Vertex sink = dag.newVertex("sink", writeListP(streamSinkName));

        dag.edge(between(source, sink));

        Future<Void> future = jetInstance.newJob(dag).getFuture();

        IStreamMap<Integer, Integer> sourceMap = jetInstance.getMap(streamSourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceMap.put(i, i));

        assertSizeEventually(ENTRY_COUNT - 1, jetInstance.getList(streamSinkName));
        assertFalse(jetInstance.getList(streamSinkName).contains(0));
        assertTrue(jetInstance.getList(streamSinkName).contains(1));
        future.cancel(true);
    }

    @Test
    public void when_readCache_and_writeCache() throws ExecutionException, InterruptedException {
        ICache<Integer, Integer> sourceCache = jetInstance.getCacheManager().getCache(sourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceCache.put(i, i));

        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", readCacheP(sourceName));
        Vertex sink = dag.newVertex("sink", writeCacheP(sinkName));

        dag.edge(between(source, sink));

        jetInstance.newJob(dag).join();

        assertEquals(ENTRY_COUNT, jetInstance.getCacheManager().getCache(sinkName).size());
    }

    @Test
    public void when_streamCache() throws ExecutionException, InterruptedException {
        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", streamCacheP(streamSourceName, false));
        Vertex sink = dag.newVertex("sink", writeListP(streamSinkName));

        dag.edge(between(source, sink));

        Future<Void> future = jetInstance.newJob(dag).getFuture();

        IStreamCache<Integer, Integer> sourceCache = jetInstance.getCacheManager().getCache(streamSourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceCache.put(i, i));

        assertSizeEventually(ENTRY_COUNT, jetInstance.getList(streamSinkName));
        future.cancel(true);
    }

    @Test
    public void when_streamCache_withFilterAndProjection() throws ExecutionException, InterruptedException {
        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", streamCacheP(streamSourceName,
                event -> !event.getKey().equals(0), EventJournalCacheEvent::getKey, false));
        Vertex sink = dag.newVertex("sink", writeListP(streamSinkName));

        dag.edge(between(source, sink));

        Future<Void> future = jetInstance.newJob(dag).getFuture();

        IStreamCache<Integer, Integer> sourceCache = jetInstance.getCacheManager().getCache(streamSourceName);
        range(0, ENTRY_COUNT).forEach(i -> sourceCache.put(i, i));

        assertSizeEventually(ENTRY_COUNT - 1, jetInstance.getList(streamSinkName));
        assertFalse(jetInstance.getList(streamSinkName).contains(0));
        assertTrue(jetInstance.getList(streamSinkName).contains(1));
        future.cancel(true);
    }

    @Test
    public void when_readList_and_writeList() throws ExecutionException, InterruptedException {
        IStreamList<Integer> list = jetInstance.getList(sourceName);
        list.addAll(range(0, ENTRY_COUNT).boxed().collect(toList()));

        DAG dag = new DAG();
        Vertex source = dag.newVertex("source", readListP(sourceName)).localParallelism(1);
        Vertex sink = dag.newVertex("sink", writeListP(sinkName)).localParallelism(1);

        dag.edge(between(source, sink));

        jetInstance.newJob(dag).join();

        assertEquals(ENTRY_COUNT, jetInstance.getList(sinkName).size());
    }

}
