package com.clou.ess.actor;

import akka.actor.UntypedActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/26 10:06
 */
public class SubscriberClusterListener extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    Cluster cluster = Cluster.get(getContext().system());

    // subscribe to cluster changes
    @Override
    public void preStart() {
        // #subscribe
        cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
                MemberEvent.class, UnreachableMember.class,LeaderChanged.class);
        // #subscribe
    }

    // re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof MemberUp) {
            MemberUp mUp = (MemberUp) message;
            if(mUp.member().address().hostPort().contains("localhost")){
                cluster.down(mUp.member().address());
                log.error("节点ip地址异常: {}", mUp.member());
                return;
            }
            log.info("Member is Up: {}", mUp.member());
        } else if (message instanceof UnreachableMember) {
            UnreachableMember mUnreachable = (UnreachableMember) message;
            log.info("Member detected as unreachable: {}",
                    mUnreachable.member());
            cluster.down(mUnreachable.member().address());
        } else if (message instanceof LeaderChanged) {
            LeaderChanged lChanged = (LeaderChanged) message;
            log.info("Member is LeaderChanged: {}", lChanged.leader());
            String serviceAddr = lChanged.getLeader().protocol() + "://"+lChanged.getLeader().hostPort();
            if(serviceAddr.equals(InitSubscriber.getInstance().clusterConfig.getClusterNode())){
                //JedisGlobal.JedisUtil_DATA.setRootKeyValue(ClusterParamter.PUBLISHER_SUFF+ClusterParamter.CLUSTER_LEADER, serviceAddr);
            }
        }else if (message instanceof MemberRemoved) {
            MemberRemoved mRemoved = (MemberRemoved) message;
            cluster.join(mRemoved.member().address());
            log.info("Member is Removed: {}", mRemoved.member());
        } else if (message instanceof MemberEvent) {
            // ignore
        } else {
            unhandled(message);
        }
    }
}
