/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.client.config;

import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.MapIndexConfig;
import com.hazelcast.config.NearCacheConfig;
import com.hazelcast.config.QueryCacheConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.SocketInterceptorConfig;
import com.hazelcast.config.XMLConfigBuilderTest;
import com.hazelcast.core.HazelcastException;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hazelcast.test.annotation.QuickTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class tests the usage of {@link XmlClientConfigBuilder}
 */
//tests need to be executed sequentially because of system properties being set/unset
@RunWith(HazelcastSerialClassRunner.class)
@Category(QuickTest.class)
public class XmlClientConfigBuilderTest {

    ClientConfig clientConfig;

    @Before
    public void init() throws IOException {
        URL schemaResource = XMLConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-full.xml");
        clientConfig = new XmlClientConfigBuilder(schemaResource).build();
    }

    @After
    @Before
    public void after() {
        System.clearProperty("hazelcast.client.config");
    }

    @Test(expected = HazelcastException.class)
    public void loadingThroughSystemProperty_nonExistingFile() throws IOException {
        File file = File.createTempFile("foo", "bar");
        file.delete();
        System.setProperty("hazelcast.client.config", file.getAbsolutePath());

        new XmlClientConfigBuilder();
    }

    @Test
    public void loadingThroughSystemProperty_existingFile() throws IOException {
        String xml =
                "<hazelcast-client xmlns=\"http://www.hazelcast.com/schema/client-config\">\n" +
                        "    <group>\n" +
                        "        <name>foobar</name>\n" +
                        "        <password>dev-pass</password>\n" +
                        "    </group>" +
                        "</hazelcast-client>";

        File file = File.createTempFile("foo", "bar");
        file.deleteOnExit();
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println(xml);
        writer.close();

        System.setProperty("hazelcast.client.config", file.getAbsolutePath());

        XmlClientConfigBuilder configBuilder = new XmlClientConfigBuilder();
        ClientConfig config = configBuilder.build();
        assertEquals("foobar", config.getGroupConfig().getName());
    }

    @Test(expected = HazelcastException.class)
    public void loadingThroughSystemProperty_nonExistingClasspathResource() throws IOException {
        System.setProperty("hazelcast.client.config", "classpath:idontexist");
        new XmlClientConfigBuilder();
    }

    @Test
    public void loadingThroughSystemProperty_existingClasspathResource() throws IOException {
        System.setProperty("hazelcast.client.config", "classpath:test-hazelcast-client.xml");

        XmlClientConfigBuilder configBuilder = new XmlClientConfigBuilder();
        ClientConfig config = configBuilder.build();
        assertEquals("foobar", config.getGroupConfig().getName());
        assertEquals("com.hazelcast.nio.ssl.BasicSSLContextFactory", config.getNetworkConfig().getSSLConfig().getFactoryClassName());
        assertEquals(32, config.getNetworkConfig().getSocketOptions().getBufferSize());
        assertFalse(config.getNetworkConfig().getSocketOptions().isKeepAlive());
        assertFalse(config.getNetworkConfig().getSocketOptions().isTcpNoDelay());
        assertEquals(3, config.getNetworkConfig().getSocketOptions().getLingerSeconds());
    }

    @Test
    public void testGroupConfig() {
        final GroupConfig groupConfig = clientConfig.getGroupConfig();
        assertEquals("dev", groupConfig.getName());
        assertEquals("dev-pass", groupConfig.getPassword());
    }

    @Test
    public void testProperties() {
        assertEquals(6, clientConfig.getProperties().size());
        assertEquals("60000", clientConfig.getProperty("hazelcast.client.heartbeat.timeout"));
    }

    @Test
    public void testNetworkConfig() {
        final ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
        assertEquals(2, networkConfig.getConnectionAttemptLimit());
        assertEquals(2, networkConfig.getAddresses().size());
        assertTrue(networkConfig.getAddresses().contains("127.0.0.1"));
        assertTrue(networkConfig.getAddresses().contains("127.0.0.2"));

        assertTrue(networkConfig.isSmartRouting());
        assertTrue(networkConfig.isRedoOperation());

        final SocketInterceptorConfig socketInterceptorConfig = networkConfig.getSocketInterceptorConfig();
        assertTrue(socketInterceptorConfig.isEnabled());
        assertEquals("com.hazelcast.examples.MySocketInterceptor", socketInterceptorConfig.getClassName());
        assertEquals("bar", socketInterceptorConfig.getProperty("foo"));

        final ClientAwsConfig awsConfig = networkConfig.getAwsConfig();
        assertTrue(awsConfig.isEnabled());
        assertTrue(awsConfig.isInsideAws());
        assertEquals("TEST_ACCESS_KEY", awsConfig.getAccessKey());
        assertEquals("TEST_ACCESS_KEY", awsConfig.getAccessKey());
        assertEquals("TEST_SECRET_KEY", awsConfig.getSecretKey());
        assertEquals("us-east-1", awsConfig.getRegion());
        assertEquals("ec2.amazonaws.com", awsConfig.getHostHeader());
        assertEquals("type", awsConfig.getTagKey());
        assertEquals("hz-nodes", awsConfig.getTagValue());
        assertEquals(11, awsConfig.getConnectionTimeoutSeconds());
    }

    @Test
    public void testSerializationConfig() {
        final SerializationConfig serializationConfig = clientConfig.getSerializationConfig();
        assertEquals(3, serializationConfig.getPortableVersion());

        final Map<Integer, String> dsClasses = serializationConfig.getDataSerializableFactoryClasses();
        assertEquals(1, dsClasses.size());
        assertEquals("com.hazelcast.examples.DataSerializableFactory", dsClasses.get(1));

        final Map<Integer, String> pfClasses = serializationConfig.getPortableFactoryClasses();
        assertEquals(1, pfClasses.size());
        assertEquals("com.hazelcast.examples.PortableFactory", pfClasses.get(2));

        final Collection<SerializerConfig> serializerConfigs = serializationConfig.getSerializerConfigs();
        assertEquals(1, serializerConfigs.size());
        final SerializerConfig serializerConfig = serializerConfigs.iterator().next();

        assertEquals("com.hazelcast.examples.DummyType", serializerConfig.getTypeClassName());
        assertEquals("com.hazelcast.examples.SerializerFactory", serializerConfig.getClassName());

        final GlobalSerializerConfig globalSerializerConfig = serializationConfig.getGlobalSerializerConfig();
        assertEquals("com.hazelcast.examples.GlobalSerializerFactory", globalSerializerConfig.getClassName());

        assertEquals(ByteOrder.BIG_ENDIAN, serializationConfig.getByteOrder());
        assertEquals(true, serializationConfig.isCheckClassDefErrors());
        assertEquals(false, serializationConfig.isAllowUnsafe());
        assertEquals(false, serializationConfig.isEnableCompression());
        assertEquals(true, serializationConfig.isEnableSharedObject());
        assertEquals(true, serializationConfig.isUseNativeByteOrder());
    }

    @Test
    public void testProxyFactories() {
        final List<ProxyFactoryConfig> pfc = clientConfig.getProxyFactoryConfigs();
        assertEquals(3, pfc.size());
        assertTrue(pfc.contains(new ProxyFactoryConfig("com.hazelcast.examples.ProxyXYZ1", "sampleService1")));
        assertTrue(pfc.contains(new ProxyFactoryConfig("com.hazelcast.examples.ProxyXYZ2", "sampleService1")));
        assertTrue(pfc.contains(new ProxyFactoryConfig("com.hazelcast.examples.ProxyXYZ3", "sampleService3")));
    }

    @Test
    public void testNearCacheConfigs() {
        assertEquals(1, clientConfig.getNearCacheConfigMap().size());
        final NearCacheConfig nearCacheConfig = clientConfig.getNearCacheConfig("asd");

        assertEquals(2000, nearCacheConfig.getMaxSize());
        assertEquals(90, nearCacheConfig.getTimeToLiveSeconds());
        assertEquals(100, nearCacheConfig.getMaxIdleSeconds());
        assertEquals("LFU", nearCacheConfig.getEvictionPolicy());
        assertTrue(nearCacheConfig.isInvalidateOnChange());
        assertEquals(InMemoryFormat.OBJECT, nearCacheConfig.getInMemoryFormat());
    }

    @Test
    public void testNearCacheConfigWithEvictionConfig() throws IOException {
        URL schemaResource = XMLConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-test.xml");
        ClientConfig clientConfig = new XmlClientConfigBuilder(schemaResource).build();
        assertEquals("MyName", clientConfig.getInstanceName());

        NearCacheConfig nearCacheConfig = clientConfig.getNearCacheConfig("nearCacheWithEviction");
        
        assertEquals(10000, nearCacheConfig.getTimeToLiveSeconds());
        assertEquals(5000, nearCacheConfig.getMaxIdleSeconds());
        assertFalse(nearCacheConfig.isInvalidateOnChange());
        assertEquals(InMemoryFormat.OBJECT, nearCacheConfig.getInMemoryFormat());
        assertTrue(nearCacheConfig.isCacheLocalEntries());

        assertNotNull(nearCacheConfig.getEvictionConfig());
        assertEquals(100, nearCacheConfig.getEvictionConfig().getSize());
        assertEquals(EvictionConfig.MaxSizePolicy.ENTRY_COUNT, nearCacheConfig.getEvictionConfig().getMaximumSizePolicy());
        assertEquals(EvictionPolicy.LFU, nearCacheConfig.getEvictionConfig().getEvictionPolicy());
    }

    @Test
    public void testQueryCacheFullConfig() throws Exception {
        QueryCacheConfig queryCacheConfig = clientConfig.getQueryCacheConfigs().get("map-name").get("query-cache-name");
        EntryListenerConfig entryListenerConfig = queryCacheConfig.getEntryListenerConfigs().get(0);

        assertEquals("query-cache-name", queryCacheConfig.getName());
        assertTrue(entryListenerConfig.isIncludeValue());
        assertFalse(entryListenerConfig.isLocal());
        assertEquals("com.hazelcast.examples.EntryListener", entryListenerConfig.getClassName());
        assertTrue(queryCacheConfig.isIncludeValue());
        assertEquals(1, queryCacheConfig.getBatchSize());
        assertEquals(16, queryCacheConfig.getBufferSize());
        assertEquals(0, queryCacheConfig.getDelaySeconds());
        assertEquals(EvictionPolicy.LRU, queryCacheConfig.getEvictionConfig().getEvictionPolicy());
        assertEquals(EvictionConfig.MaxSizePolicy.ENTRY_COUNT, queryCacheConfig.getEvictionConfig().getMaximumSizePolicy());
        assertEquals(10000, queryCacheConfig.getEvictionConfig().getSize());
        assertEquals(InMemoryFormat.BINARY, queryCacheConfig.getInMemoryFormat());
        assertFalse(queryCacheConfig.isCoalesce());
        assertTrue(queryCacheConfig.isPopulate());
        Iterator<MapIndexConfig> iterator = queryCacheConfig.getIndexConfigs().iterator();
        while (iterator.hasNext()) {
            MapIndexConfig mapIndexConfig = iterator.next();
            assertEquals("name", mapIndexConfig.getAttribute());
            assertFalse(mapIndexConfig.isOrdered());
        }

        assertEquals("com.hazelcast.examples.ExamplePredicate", queryCacheConfig.getPredicateConfig().getClassName());

    }

    @Test
    public void testLeftovers() {
        assertEquals(40, clientConfig.getExecutorPoolSize());

        assertEquals("com.hazelcast.security.UsernamePasswordCredentials",
                clientConfig.getSecurityConfig().getCredentialsClassname());
        assertEquals(40, clientConfig.getExecutorPoolSize());

        assertEquals("com.hazelcast.client.util.RandomLB", clientConfig.getLoadBalancer().getClass().getName());

        final List<ListenerConfig> listenerConfigs = clientConfig.getListenerConfigs();
        assertEquals(3, listenerConfigs.size());
        assertTrue(listenerConfigs.contains(new ListenerConfig("com.hazelcast.examples.MembershipListener")));
        assertTrue(listenerConfigs.contains(new ListenerConfig("com.hazelcast.examples.InstanceListener")));
        assertTrue(listenerConfigs.contains(new ListenerConfig("com.hazelcast.examples.MigrationListener")));
    }

    @Test
    public void testXSDDefaultXML() throws SAXException, IOException {
        testXSDConfigXML("hazelcast-client-default.xml");
    }

    @Test
    public void testFullConfigXML() throws SAXException, IOException {
        testXSDConfigXML("hazelcast-client-full.xml");
    }

    private void testXSDConfigXML(String xmlFileName) throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL schemaResource = XMLConfigBuilderTest.class.getClassLoader().getResource("hazelcast-client-config-3.6.xsd");
        InputStream xmlResource = XMLConfigBuilderTest.class.getClassLoader().getResourceAsStream(xmlFileName);
        Schema schema = factory.newSchema(schemaResource);
        Source source = new StreamSource(xmlResource);
        Validator validator = schema.newValidator();
        try {
            validator.validate(source);
        } catch (SAXException ex) {
            fail(xmlFileName + " is not valid because: " + ex.toString());
        }
    }
}
