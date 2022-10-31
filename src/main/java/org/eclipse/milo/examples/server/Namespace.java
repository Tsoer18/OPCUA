/*
 * Copyright (c) 2021 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.examples.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.Lifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.intellij.lang.annotations.Identifier;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.examples.server.DataCollector.getDatapoints;
import static org.eclipse.milo.examples.server.DataCollector.sendGET;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class Namespace extends ManagedNamespaceWithLifecycle {
    DataCollector dataCollector = new DataCollector();

    public static final String NAMESPACE_URI = "urn:sdu:milo:ICPS";


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile Thread eventThread;
    private volatile boolean keepPostingEvents = true;

    private final Random random = new Random();

    private final DataTypeDictionaryManager dictionaryManager;

    private final SubscriptionModel subscriptionModel;

    Namespace(OpcUaServer server) throws IOException, ParseException {
        super(server, NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);
        dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), NAMESPACE_URI);

        getLifecycleManager().addLifecycle(dictionaryManager);
        getLifecycleManager().addLifecycle(subscriptionModel);

        getLifecycleManager().addStartupTask(this::createAndAddNodes);

        getLifecycleManager().addLifecycle(new Lifecycle() {
            @Override
            public void startup() {
                startBogusEventNotifier();

            }

            @Override
            public void shutdown() {
                try {
                    keepPostingEvents = false;
                    eventThread.interrupt();
                    eventThread.join();
                } catch (InterruptedException ignored) {
                    // ignored
                }
            }
        });
    }

    private void createAndAddNodes() {
        // Create a "HelloWorld" folder and add it to the node manager
        NodeId folderNodeId = newNodeId("Devices");

        UaFolderNode folderNode = new UaFolderNode(
            getNodeContext(),
            folderNodeId,
            newQualifiedName("Devices"),
            LocalizedText.english("Devices")
        );

        getNodeManager().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder.
        folderNode.addReference(new Reference(
            folderNode.getNodeId(),
            Identifiers.Organizes,
            Identifiers.ObjectsFolder.expanded(),
            false
        ));

        // Add the rest of the nodes
        addDevices(folderNode);
    }

    private void startBogusEventNotifier() {
        // Set the EventNotifier bit on Server Node for Events.
        UaNode serverNode = getServer()
            .getAddressSpaceManager()
            .getManagedNode(Identifiers.Server)
            .orElse(null);

        if (serverNode instanceof ServerTypeNode) {
            ((ServerTypeNode) serverNode).setEventNotifier(ubyte(1));

            // Post a bogus Event every couple seconds
            eventThread = new Thread(() -> {
                while (keepPostingEvents) {
                    try {
                        BaseEventTypeNode eventNode = getServer().getEventFactory().createEvent(
                            newNodeId(UUID.randomUUID()),
                            Identifiers.BaseEventType
                        );

                        eventNode.setBrowseName(new QualifiedName(1, "foo"));
                        eventNode.setDisplayName(LocalizedText.english("foo"));
                        eventNode.setEventId(ByteString.of(new byte[]{0, 1, 2, 3}));
                        eventNode.setEventType(Identifiers.BaseEventType);
                        eventNode.setSourceNode(serverNode.getNodeId());
                        eventNode.setSourceName(serverNode.getDisplayName().getText());
                        eventNode.setTime(DateTime.now());
                        eventNode.setReceiveTime(DateTime.NULL_VALUE);
                        eventNode.setMessage(LocalizedText.english("event message!"));
                        eventNode.setSeverity(ushort(2));

                        //noinspection UnstableApiUsage
                        getServer().getEventBus().post(eventNode);

                        eventNode.delete();
                    } catch (Throwable e) {
                        logger.error("Error creating EventNode: {}", e.getMessage(), e);
                    }

                    try {
                        //noinspection BusyWait
                        Thread.sleep(2_000);
                    } catch (InterruptedException ignored) {
                        // ignored
                    }
                }
            }, "bogus-event-poster");

            eventThread.start();
        }
    }



    private void addDevices(UaFolderNode rootNode) {
        for (Device i : dataCollector.getDevices()) {
            UaFolderNode scalarTypesFolder = new UaFolderNode(
                    getNodeContext(),
                    newNodeId("Devices/" + i.getID()),
                    newQualifiedName("deviceID"),
                    LocalizedText.english(i.getID())
            );

            getNodeManager().addNode(scalarTypesFolder);
            rootNode.addOrganizes(scalarTypesFolder);
            for (LogicalDevice logicalDevice : i.getLogicalDevices()){
                UaFolderNode logicalDeviceFolder = new UaFolderNode(
                        getNodeContext(),
                        newNodeId("Devices/" + i.getID()+ "/"+ logicalDevice.getKey()),
                        newQualifiedName("logicalDeviceID"),
                        LocalizedText.english(logicalDevice.getKey())
                );

                getNodeManager().addNode(logicalDeviceFolder);
                scalarTypesFolder.addOrganizes(logicalDeviceFolder);
                for (Datapoint datapoint : logicalDevice.datapoints) {
                    {

                        if (datapoint.access.equals("r")){
                            String name = datapoint.dpkey;
                            NodeId typeId =  new NodeId(2,"i");
                            Variant variant = new Variant(0);
                            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                                    .setNodeId(newNodeId("Devices/" + i.getID()+ "/"+ logicalDevice.getKey()+ "/" + name))
                                    .setBrowseName(newQualifiedName(name))
                                    .setUserAccessLevel(AccessLevel.READ_ONLY)
                                    .setDisplayName(LocalizedText.english(name))
                                    .setDataType(typeId)
                                    .setTypeDefinition(Identifiers.Int32)
                                    .build();
                            node.setValue(new DataValue(variant));

                            node.getFilterChain().addLast(
                                    new AttributeLoggingFilter(),
                                    AttributeFilters.getValue(
                                            ctx ->
                                            {
                                                Object value;
                                                try {
                                                    value = getJsonNode(datapoint.geturl);
                                                } catch (ParseException e) {
                                                    throw new RuntimeException(e);
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                                return new DataValue(new Variant(value));
                                            }

                                    )
                            );



                            getNodeManager().addNode(node);
                            logicalDeviceFolder.addOrganizes(node);
                        }
                        if (datapoint.access.equals("w")){
                            String name = datapoint.dpkey;
                            NodeId typeId =  new NodeId(2,"i");
                            Variant variant = new Variant(0);
                            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                                    .setNodeId(newNodeId("Devices/" + i.getID()+ "/"+ logicalDevice.getKey()+ "/" + name))
                                    .setBrowseName(newQualifiedName(name))
                                    .setUserAccessLevel(AccessLevel.WRITE_ONLY)
                                    .setDisplayName(LocalizedText.english(name))
                                    .setDataType(typeId)
                                    .setTypeDefinition(Identifiers.Int32)
                                    .build();
                            node.setValue(new DataValue(variant));

                            node.getFilterChain().addLast(
                                    new AttributeLoggingFilter(),
                                    AttributeFilters.getValue(
                                            ctx ->
                                            {
                                                Object value;
                                                try {
                                                    value = getJsonNode(datapoint.geturl);
                                                } catch (ParseException e) {
                                                    throw new RuntimeException(e);
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                                return new DataValue(new Variant(value));
                                            }
                                    )
                            );



                            getNodeManager().addNode(node);
                            logicalDeviceFolder.addOrganizes(node);
                        }
                        if (datapoint.access.equals("rw")){
                            String name = datapoint.dpkey;
                                NodeId typeId =  new NodeId(2,datapoint.type);
                            Variant variant = new Variant(0);
                            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                                    .setNodeId(newNodeId("Devices/" + i.getID()+ "/"+ logicalDevice.getKey()+ "/" + name))
                                    .setBrowseName(newQualifiedName(name))
                                    .setUserAccessLevel(AccessLevel.READ_WRITE)
                                    .setDisplayName(LocalizedText.english(name))
                                    .setDataType(typeId)
                                    .setTypeDefinition(Identifiers.Int32)
                                    .build();
                            node.setValue(new DataValue(variant));

                            node.getFilterChain().addLast(
                                    new AttributeLoggingFilter(),
                                    AttributeFilters.getValue(
                                            ctx ->
                                            {
                                                Object value;
                                                try {
                                                    value = getJsonNode(datapoint.geturl);
                                                } catch (ParseException e) {
                                                    throw new RuntimeException(e);
                                                } catch (IOException e) {
                                                    throw new RuntimeException(e);
                                                }
                                                return new DataValue(new Variant(value));
                                            }
                                    )
                            );



                            getNodeManager().addNode(node);
                            logicalDeviceFolder.addOrganizes(node);

                        }

                    }
                }
                }
            }
        }



    private void addStatic(UaFolderNode rootNode) {
        UaFolderNode scalarTypesFolder = new UaFolderNode(
            getNodeContext(),
            newNodeId("ICPS/Static"),
            newQualifiedName("Static"),
            LocalizedText.english("Static")
        );

        getNodeManager().addNode(scalarTypesFolder);
        rootNode.addOrganizes(scalarTypesFolder);

        String name = "Int32";
        NodeId typeId = Identifiers.Int32;
        Variant variant = new Variant(uint(32));

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId("ICPS/Static/" + name))
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setUserAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        node.setValue(new DataValue(variant));

        node.getFilterChain().addLast(new AttributeLoggingFilter(AttributeId.Value::equals));

        getNodeManager().addNode(node);
        scalarTypesFolder.addOrganizes(node);
    }
    private  Object getJsonNode(URL geturl) throws ParseException, IOException {
        StringBuffer datapointResponse = sendGET(geturl);
        ArrayList datapointAccessAndName = getDatapoints(datapointResponse);
        System.out.println(datapointAccessAndName.get(3));
        if (datapointAccessAndName.get(3).equals("boolean")) {
            return (Boolean) datapointAccessAndName.get(2);
        }else if(datapointAccessAndName.get(3).equals("integer")){
            return (Integer) datapointAccessAndName.get(2);
        }else if (datapointAccessAndName.get(3).equals("double")){
            return (Double) datapointAccessAndName.get(2);
        }else {
            return (String) datapointAccessAndName.get(2);
        }


    }

    private void addDynamic(UaFolderNode rootNode) {
        UaFolderNode dynamicFolder = new UaFolderNode(
            getNodeContext(),
            newNodeId("ICPS/Dynamic"),
            newQualifiedName("Dynamic"),
            LocalizedText.english("Dynamic")
        );

        getNodeManager().addNode(dynamicFolder);
        rootNode.addOrganizes(dynamicFolder);

        // Dynamic Int32
        {
            String name = "Int32";
            NodeId typeId = Identifiers.Int32;
            Variant variant = new Variant(0);

            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                .setNodeId(newNodeId("ICPS/Dynamic/" + name))
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setBrowseName(newQualifiedName(name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(typeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

            node.setValue(new DataValue(variant));

            node.getFilterChain().addLast(
                new AttributeLoggingFilter(),
                AttributeFilters.getValue(
                    ctx ->
                        new DataValue(new Variant(random.nextInt(100)))
                )
            );

            getNodeManager().addNode(node);
            dynamicFolder.addOrganizes(node);
        }
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

}
