/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.openflowplugin.applications.bulk.o.matic;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.opendaylight.infrautils.utils.concurrent.LoggingFutures;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.AddFlowsDsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.AddFlowsDsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.AddFlowsRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.AddFlowsRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.BulkFlowBaseContentGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.FlowRpcAddMultipleInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.FlowRpcAddMultipleOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.FlowRpcAddTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.FlowRpcAddTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.FlowTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.FlowTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.ReadFlowTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.ReadFlowTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RegisterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RegisterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RegisterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RemoveFlowsDsInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RemoveFlowsDsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RemoveFlowsRpcInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.RemoveFlowsRpcOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.SalBulkFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.TableTestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.TableTestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.bulk.flow.service.rev150608.bulk.flow.ds.list.grouping.BulkFlowDsItem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation providing bulk flows operations.
 */
public class SalBulkFlowServiceImpl implements SalBulkFlowService {

    private static final Logger LOG = LoggerFactory.getLogger(SalBulkFlowServiceImpl.class);

    private final SalFlowService flowService;
    private final DataBroker dataBroker;
    private final FlowCounter flowCounterBeanImpl = new FlowCounter();
    private final ExecutorService fjService = new ForkJoinPool();

    public SalBulkFlowServiceImpl(SalFlowService flowService, DataBroker dataBroker) {
        this.flowService = Preconditions.checkNotNull(flowService);
        this.dataBroker = Preconditions.checkNotNull(dataBroker);

        LoggingFutures.addErrorLogging(register(new RegisterInputBuilder().build()), LOG, "register");
    }

    @Override
    public ListenableFuture<RpcResult<AddFlowsDsOutput>> addFlowsDs(AddFlowsDsInput input) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        boolean createParentsNextTime = MoreObjects.firstNonNull(input.isAlwaysCreateParents(), Boolean.FALSE);
        boolean createParents = true;
        for (BulkFlowDsItem bulkFlow : input.getBulkFlowDsItem()) {
            FlowBuilder flowBuilder = new FlowBuilder(bulkFlow);
            flowBuilder.setTableId(bulkFlow.getTableId());
            flowBuilder.setId(new FlowId(bulkFlow.getFlowId()));
            writeTransaction.put(LogicalDatastoreType.CONFIGURATION, getFlowInstanceIdentifier(bulkFlow),
                    flowBuilder.build(), createParents);
            createParents = createParentsNextTime;
        }
        ListenableFuture<?> submitFuture = writeTransaction.commit();
        return Futures.transform(handleResultFuture(Futures.allAsList(submitFuture)), voidRpcResult -> {
            if (voidRpcResult.isSuccessful()) {
                return RpcResultBuilder.<AddFlowsDsOutput>success().build();
            } else {
                return RpcResultBuilder.<AddFlowsDsOutput>failed().build();
            }
        },MoreExecutors.directExecutor());
    }

    private InstanceIdentifier<Flow> getFlowInstanceIdentifier(BulkFlowDsItem bulkFlow) {
        final NodeRef nodeRef = bulkFlow.getNode();
        return ((InstanceIdentifier<Node>) nodeRef.getValue()).augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(bulkFlow.getTableId()))
                .child(Flow.class, new FlowKey(new FlowId(bulkFlow.getFlowId())));
    }

    @Override
    public ListenableFuture<RpcResult<RemoveFlowsDsOutput>> removeFlowsDs(RemoveFlowsDsInput input) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        for (BulkFlowDsItem bulkFlow : input.getBulkFlowDsItem()) {
            writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, getFlowInstanceIdentifier(bulkFlow));
        }
        return Futures.transform(handleResultFuture(Futures.allAsList(writeTransaction.commit())), voidRpcResult -> {
            if (voidRpcResult.isSuccessful()) {
                return RpcResultBuilder.<RemoveFlowsDsOutput>success().build();
            } else {
                return RpcResultBuilder.<RemoveFlowsDsOutput>failed().build();
            }
        }, MoreExecutors.directExecutor());
    }

    private <T> ListenableFuture<RpcResult<Void>> handleResultFuture(ListenableFuture<List<T>> submitFuture) {
        final SettableFuture<RpcResult<Void>> rpcResult = SettableFuture.create();
        Futures.addCallback(submitFuture, new FutureCallback<List<T>>() {
            @Override
            public void onSuccess(List<T> result) {
                rpcResult.set(RpcResultBuilder.success((Void) null).build());
            }

            @Override
            public void onFailure(Throwable throwable) {
                RpcResultBuilder<Void> rpcResultBld = RpcResultBuilder.<Void>failed()
                        .withRpcErrors(Collections.singleton(RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION,
                                null, throwable.getMessage())));
                rpcResult.set(rpcResultBld.build());
            }
        }, MoreExecutors.directExecutor());
        return rpcResult;
    }

    @Override
    public ListenableFuture<RpcResult<AddFlowsRpcOutput>> addFlowsRpc(AddFlowsRpcInput input) {
        List<ListenableFuture<RpcResult<AddFlowOutput>>> bulkResults = new ArrayList<>();

        for (BulkFlowBaseContentGrouping bulkFlow : input.getBulkFlowItem()) {
            AddFlowInputBuilder flowInputBuilder = new AddFlowInputBuilder(
                    (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) bulkFlow);
            final NodeRef nodeRef = bulkFlow.getNode();
            flowInputBuilder.setNode(nodeRef);
            flowInputBuilder.setTableId(bulkFlow.getTableId());
            bulkResults.add(flowService.addFlow(flowInputBuilder.build()));
        }
        return Futures.transform(handleResultFuture(Futures.allAsList(bulkResults)), voidRpcResult -> {
            if (voidRpcResult.isSuccessful()) {
                return RpcResultBuilder.<AddFlowsRpcOutput>success().build();
            } else {
                return RpcResultBuilder.<AddFlowsRpcOutput>failed().build();
            }
        },MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<ReadFlowTestOutput>> readFlowTest(ReadFlowTestInput input) {
        FlowReader flowReader = FlowReader.getNewInstance(dataBroker, input.getDpnCount().intValue(),
                input.getFlowsPerDpn().intValue(), input.isVerbose(), input.isIsConfigDs(),
                input.getStartTableId().shortValue(), input.getEndTableId().shortValue());
        flowCounterBeanImpl.setReader(flowReader);
        fjService.execute(flowReader);
        RpcResultBuilder<ReadFlowTestOutput> rpcResultBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<FlowRpcAddTestOutput>> flowRpcAddTest(FlowRpcAddTestInput input) {
        FlowWriterDirectOFRpc flowAddRpcTestImpl = new FlowWriterDirectOFRpc(dataBroker, flowService, fjService);
        flowAddRpcTestImpl.rpcFlowAdd(input.getDpnId(), input.getFlowCount().intValue(),
                input.getRpcBatchSize().intValue());

        RpcResultBuilder<FlowRpcAddTestOutput> rpcResultBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<RegisterOutput>> register(RegisterInput input) {
        RpcResultBuilder<RegisterOutput> rpcResultBuilder = RpcResultBuilder.success();
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            String pathToMBean = String.format("%s:type=%s", FlowCounter.class.getPackage().getName(),
                    FlowCounter.class.getSimpleName());
            ObjectName name = new ObjectName(pathToMBean);
            mbs.registerMBean(flowCounterBeanImpl, name);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException e) {
            rpcResultBuilder = RpcResultBuilder.failed();
            LOG.warn("Exception occurred", e);
        }
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<RemoveFlowsRpcOutput>> removeFlowsRpc(RemoveFlowsRpcInput input) {
        List<ListenableFuture<RpcResult<RemoveFlowOutput>>> bulkResults = new ArrayList<>();

        for (BulkFlowBaseContentGrouping bulkFlow : input.getBulkFlowItem()) {
            RemoveFlowInputBuilder flowInputBuilder = new RemoveFlowInputBuilder(
                    (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow) bulkFlow);
            final NodeRef nodeRef = bulkFlow.getNode();
            flowInputBuilder.setNode(nodeRef);
            flowInputBuilder.setTableId(bulkFlow.getTableId());
            bulkResults.add(flowService.removeFlow(flowInputBuilder.build()));
        }
        return Futures.transform(handleResultFuture(Futures.allAsList(bulkResults)), voidRpcResult -> {
            if (voidRpcResult.isSuccessful()) {
                return RpcResultBuilder.<RemoveFlowsRpcOutput>success().build();
            } else {
                return RpcResultBuilder.<RemoveFlowsRpcOutput>failed().build();
            }
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<FlowTestOutput>> flowTest(FlowTestInput input) {
        if (input.isTxChain()) {
            FlowWriterTxChain flowTester = new FlowWriterTxChain(dataBroker, fjService);
            flowCounterBeanImpl.setWriter(flowTester);
            if (input.isIsAdd()) {
                flowTester.addFlows(input.getDpnCount().intValue(), input.getFlowsPerDpn().intValue(),
                        input.getBatchSize().intValue(), input.getSleepFor().intValue(),
                        input.getSleepAfter().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue(), input.isCreateParents());
            } else {
                flowTester.deleteFlows(input.getDpnCount().intValue(), input.getFlowsPerDpn().intValue(),
                        input.getBatchSize().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue());
            }
            RpcResultBuilder<FlowTestOutput> rpcResultBuilder = RpcResultBuilder.success();
            return Futures.immediateFuture(rpcResultBuilder.build());
        }
        if (input.isSeq()) {
            FlowWriterSequential flowTester = new FlowWriterSequential(dataBroker, fjService);
            flowCounterBeanImpl.setWriter(flowTester);
            if (input.isIsAdd()) {
                flowTester.addFlows(input.getDpnCount().intValue(), input.getFlowsPerDpn().intValue(),
                        input.getBatchSize().intValue(), input.getSleepFor().intValue(),
                        input.getStartTableId().shortValue(), input.getEndTableId().shortValue(),
                        input.isCreateParents());
            } else {
                flowTester.deleteFlows(input.getDpnCount().intValue(), input.getFlowsPerDpn().intValue(),
                        input.getBatchSize().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue());
            }
        } else {
            FlowWriterConcurrent flowTester = new FlowWriterConcurrent(dataBroker, fjService);
            flowCounterBeanImpl.setWriter(flowTester);
            if (input.isIsAdd()) {
                flowTester.addFlows(input.getDpnCount().intValue(), input.getFlowsPerDpn().intValue(),
                        input.getBatchSize().intValue(), input.getSleepFor().intValue(),
                        input.getSleepAfter().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue(), input.isCreateParents());
            } else {
                flowTester.deleteFlows(input.getDpnCount().intValue(), input.getFlowsPerDpn().intValue(),
                        input.getBatchSize().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue());
            }
        }
        RpcResultBuilder<FlowTestOutput> rpcResultBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<TableTestOutput>> tableTest(final TableTestInput input) {
        final TableWriter writer = new TableWriter(dataBroker, fjService);
        flowCounterBeanImpl.setWriter(writer);
        switch (input.getOperation()) {
            case Add:
                writer.addTables(input.getDpnCount().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue());
                break;
            case Delete:
                writer.deleteTables(input.getDpnCount().intValue(), input.getStartTableId().shortValue(),
                        input.getEndTableId().shortValue());
                break;
            default:
                RpcResultBuilder<TableTestOutput> rpcResultBuilder = RpcResultBuilder.failed();
                return Futures.immediateFuture(rpcResultBuilder.build());
        }
        RpcResultBuilder<TableTestOutput> rpcResultBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public ListenableFuture<RpcResult<FlowRpcAddMultipleOutput>> flowRpcAddMultiple(FlowRpcAddMultipleInput input) {
        FlowWriterDirectOFRpc flowTesterRPC = new FlowWriterDirectOFRpc(dataBroker, flowService, fjService);
        flowTesterRPC.rpcFlowAddAll(input.getFlowCount().intValue(), input.getRpcBatchSize().intValue());
        RpcResultBuilder<FlowRpcAddMultipleOutput> rpcResultBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
