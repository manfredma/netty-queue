package org.mitallast.queue.raft;

import com.google.inject.AbstractModule;
import org.mitallast.queue.raft.action.append.AppendAction;
import org.mitallast.queue.raft.action.command.CommandAction;
import org.mitallast.queue.raft.action.join.JoinAction;
import org.mitallast.queue.raft.action.keepalive.KeepAliveAction;
import org.mitallast.queue.raft.action.leave.LeaveAction;
import org.mitallast.queue.raft.action.query.QueryAction;
import org.mitallast.queue.raft.action.register.RegisterAction;
import org.mitallast.queue.raft.action.vote.VoteAction;
import org.mitallast.queue.raft.cluster.Cluster;
import org.mitallast.queue.raft.cluster.Members;
import org.mitallast.queue.raft.cluster.TransportCluster;
import org.mitallast.queue.raft.log.Compactor;
import org.mitallast.queue.raft.log.Log;
import org.mitallast.queue.raft.log.SegmentManager;
import org.mitallast.queue.raft.log.entry.EntryFilter;
import org.mitallast.queue.raft.resource.ResourceRegistry;
import org.mitallast.queue.raft.resource.ResourceService;
import org.mitallast.queue.raft.resource.manager.ResourceStateMachine;
import org.mitallast.queue.raft.state.ClusterState;
import org.mitallast.queue.raft.state.RaftState;
import org.mitallast.queue.raft.state.RaftStateClient;
import org.mitallast.queue.raft.state.RaftStateContext;
import org.mitallast.queue.raft.util.ExecutionContext;

public class RaftModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ExecutionContext.class).asEagerSingleton();
        bind(RaftStreamService.class).asEagerSingleton();

        // log
        bind(SegmentManager.class).asEagerSingleton();
        bind(Log.class).asEagerSingleton();
        bind(Compactor.class).asEagerSingleton();

        // state
        bind(ClusterState.class).asEagerSingleton();
        bind(RaftState.class).asEagerSingleton();
        bind(RaftStateContext.class).asEagerSingleton();
        bind(RaftStateClient.class).to(RaftStateContext.class);
        // cluster
        bind(TransportCluster.class).asEagerSingleton();
        bind(Cluster.class).to(TransportCluster.class);
        bind(Members.class).to(TransportCluster.class);

        // resource
        bind(ResourceRegistry.class).asEagerSingleton();

        // state machine
        bind(ResourceStateMachine.class).asEagerSingleton();
        bind(StateMachine.class).to(ResourceStateMachine.class);
        bind(EntryFilter.class).to(RaftState.class);

        bind(Protocol.class).to(RaftStateContext.class);
        bind(ResourceService.class).asEagerSingleton();

        // action
        bind(AppendAction.class).asEagerSingleton();
        bind(CommandAction.class).asEagerSingleton();
        bind(JoinAction.class).asEagerSingleton();
        bind(KeepAliveAction.class).asEagerSingleton();
        bind(LeaveAction.class).asEagerSingleton();
        bind(QueryAction.class).asEagerSingleton();
        bind(RegisterAction.class).asEagerSingleton();
        bind(VoteAction.class).asEagerSingleton();
    }
}