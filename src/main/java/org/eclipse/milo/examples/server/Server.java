/*
 * Copyright (c) 2022 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.examples.server;


import java.security.Security;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;


import static java.lang.Thread.sleep;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;

public class Server {

    private static final int TCP_BIND_PORT = 12686;

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());

        try {
            NonceUtil.blockUntilSecureRandomSeeded(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.startup().get();

        while(true) {
            sleep(1000); // sleep 1s
        }
    }

    private final OpcUaServer server;
    private final Namespace namespace;

    public Server() throws Exception {

        Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations();

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationUri("urn:sdu:milo:server")
            .setApplicationName(LocalizedText.english("Milo OPC UA Server"))
            .setEndpoints(endpointConfigurations)
            .setBuildInfo(
                new BuildInfo(
                    "urn:sdu:milo:server",
                    "sdu",
                    "milo server",
                    OpcUaServer.SDK_VERSION,
                    "", DateTime.now()))
            .setProductUri("urn:sdu:milo:server")
            .build();

        server = new OpcUaServer(serverConfig);

        namespace = new Namespace(server);
        namespace.startup();
    }

    private Set<EndpointConfiguration> createEndpointConfigurations() {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                .setBindAddress("localhost")
                .setHostname(HostnameUtil.getHostname())
                .setPath("/milo")
                .addTokenPolicies(USER_TOKEN_POLICY_ANONYMOUS); //anonymous login only

        EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                .setSecurityPolicy(SecurityPolicy.None)
                .setSecurityMode(MessageSecurityMode.None)
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY) //opc.tcp only
                .setBindPort(TCP_BIND_PORT);

        endpointConfigurations.add(noSecurityBuilder.build());

        return endpointConfigurations;
    }


    public OpcUaServer getServer() {
        return server;
    }

    public CompletableFuture<OpcUaServer> startup() {
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        namespace.shutdown();

        return server.shutdown();
    }

}
