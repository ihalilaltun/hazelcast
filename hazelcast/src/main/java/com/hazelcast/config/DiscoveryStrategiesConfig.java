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

package com.hazelcast.config;

import com.hazelcast.spi.discovery.integration.DiscoveryServiceProvider;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.NodeFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This configuration class describes the top-level config of the discovery
 * SPI and its discovery strategies.
 */
public class DiscoveryStrategiesConfig {

    private final List<DiscoveryStrategyConfig> discoveryStrategyConfigs = new ArrayList<DiscoveryStrategyConfig>();
    private DiscoveryServiceProvider discoveryServiceProvider;
    private NodeFilter nodeFilter;
    private String nodeFilterClass;

    private DiscoveryStrategiesConfig readonly;

    public DiscoveryStrategiesConfig() {
    }

    protected DiscoveryStrategiesConfig(DiscoveryServiceProvider discoveryServiceProvider, NodeFilter nodeFilter,
                                        String nodeFilterClass, Collection<DiscoveryStrategyConfig> discoveryStrategyConfigs) {
        this.discoveryServiceProvider = discoveryServiceProvider;
        this.nodeFilter = nodeFilter;
        this.nodeFilterClass = nodeFilterClass;
        this.discoveryStrategyConfigs.addAll(discoveryStrategyConfigs);
    }

    public DiscoveryStrategiesConfig getAsReadOnly() {
        if (readonly != null) {
            return readonly;
        }
        readonly = new DiscoveryStrategiesConfigReadOnly(this);
        return readonly;
    }

    public void setDiscoveryServiceProvider(DiscoveryServiceProvider discoveryServiceProvider) {
        this.discoveryServiceProvider = discoveryServiceProvider;
    }

    public DiscoveryServiceProvider getDiscoveryServiceProvider() {
        return discoveryServiceProvider;
    }

    public NodeFilter getNodeFilter() {
        return nodeFilter;
    }

    public void setNodeFilter(NodeFilter nodeFilter) {
        this.nodeFilter = nodeFilter;
    }

    public String getNodeFilterClass() {
        return nodeFilterClass;
    }

    public void setNodeFilterClass(String nodeFilterClass) {
        this.nodeFilterClass = nodeFilterClass;
    }

    public boolean isEnabled() {
        return discoveryStrategyConfigs.size() > 0;
    }

    /**
     * Returns the defined {@link DiscoveryStrategy}
     * configurations. This collection does not include deactivated configurations
     * since those are automatically skipped while reading the configuration file.
     * <p/>
     * All returned configurations are expected to be active, this is to remember
     * when building custom {@link com.hazelcast.config.Config} instances.
     *
     * @return all enabled {@link DiscoveryStrategy} configurations
     */
    public Collection<DiscoveryStrategyConfig> getDiscoveryStrategyConfigs() {
        return Collections.unmodifiableCollection(discoveryStrategyConfigs);
    }

    /**
     * Adds an enabled {@link DiscoveryStrategy}
     * configuration.
     * <p/>
     * All added configurations are strictly meant to be enabled, this is to
     * remember when building custom {@link com.hazelcast.config.Config} instances.
     *
     * @param discoveryStrategyConfig
     */
    public void addDiscoveryProviderConfig(DiscoveryStrategyConfig discoveryStrategyConfig) {
        discoveryStrategyConfigs.add(discoveryStrategyConfig);
    }

}
