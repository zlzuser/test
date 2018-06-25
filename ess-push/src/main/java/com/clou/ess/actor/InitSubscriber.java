package com.clou.ess.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AddressFromURIString;
import akka.actor.Props;
import akka.cluster.Cluster;
import com.clou.config.ClusterConfig;
import com.clou.ess.push.SocketDataPush;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/26 9:33
 */
public class InitSubscriber implements Runnable {


    private ActorSystem actorSystem;

    ClusterConfig clusterConfig = ClusterConfig.getConfig();

    private ConcurrentHashMap<String, ActorRef> subscribers = new ConcurrentHashMap<String, ActorRef>();

    private static InitSubscriber instance = new InitSubscriber();

    public static InitSubscriber getInstance() {
        return instance;
    }

    private InitSubscriber() {
        Thread subThread = new Thread(this);
        subThread.start();
        try {
            //等待初始化完成
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化
     */
    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            Config cfg = ConfigFactory
                    .parseString(
                            "akka.remote.netty.tcp.port="
                                    + clusterConfig.getPort())
                    .withValue(
                            "akka.remote.netty.tcp.hostname",
                            ConfigValueFactory.fromAnyRef(clusterConfig.getIp()));
            cfg = cfg.withFallback(ConfigFactory.load("application"));
            actorSystem = ActorSystem.create(clusterConfig.getClusterName(), cfg);
            /*String clusterNodes = clusterConfig.getClusterNode();
            String leaderNode = JedisGlobal.JedisUtil_DATA
                    .queryRootKeyValue(ClusterParamter.PUBLISHER_SUFF
                            + ClusterParamter.CLUSTER_LEADER);*/
            /*if (leaderNode != null) {
                Cluster.get(actorSystem).join(AddressFromURIString.parse(leaderNode));
            } else {*///"akka.tcp://publisher@10.13.175.88:1006"
                Cluster.get(actorSystem).join(AddressFromURIString.parse("akka.tcp://publisher@10.13.3.23:1002"));
            //}
            actorSystem.actorOf(Props.create(SubscriberClusterListener.class), "Listener");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param topic        订阅主题
     * @param subName      订阅者名称
     * @param infoConsumer 消息接受者
     */
    public ActorRef getSubscriber(String topic, String subName, InfoConsumer infoConsumer) {
        ActorRef actorRef = actorSystem.actorOf(Props.create(Subscriber.class, topic, infoConsumer), subName);
        return actorRef;
    }

    public static void main(String[] args) {
        InitSubscriber.getInstance().getSubscriber("RMP_store", "ess", SocketDataPush.getInstance());
    }
}
