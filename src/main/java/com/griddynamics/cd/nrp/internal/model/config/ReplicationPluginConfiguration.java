/*
 * Copyright 2015, Grid Dynamics International, Inc.
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
package com.griddynamics.cd.nrp.internal.model.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashSet;
import java.util.Set;

/**
 * DTO Class encapsulates replication plugin configurations
 */
@NoArgsConstructor
@RequiredArgsConstructor
@XmlRootElement(name = "configurations")
public class ReplicationPluginConfiguration {
    @Getter
    @XmlElement(name = "server")
    @XmlElementWrapper(name = "servers")
    private final Set<NexusServer> servers = new HashSet<>();
    @Getter
    @NonNull
    @XmlAttribute(name = "myUrl")
    private String myUrl;
    @Getter
    @XmlAttribute(name = "requestsQueueSize")
    private Integer requestsQueueSize = 500;
    @Getter
    @XmlAttribute(name = "requestsSendingThreadsCount")
    private Integer requestsSendingThreadsCount = 1;

    public void addServer(NexusServer server) {
        servers.add(server);
    }
}
