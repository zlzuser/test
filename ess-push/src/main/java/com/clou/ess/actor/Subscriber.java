package com.clou.ess.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.contrib.pattern.DistributedPubSubExtension;
import akka.contrib.pattern.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;

import java.util.ArrayList;
import java.util.List;

/**
 * @author :zhanglz
 * @project :ess-app
 * @discription :
 * @since :2018/3/26 10:38
 */
public class Subscriber extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private String topic;

    private InfoConsumer consumer;

    private String group;

    private Router router;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public InfoConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(InfoConsumer consumer) {
        this.consumer = consumer;
    }

    public Subscriber() {
        ActorRef mediator = DistributedPubSubExtension.get(
                getContext().system()).mediator();
        // subscribe to the topic named "content"
        mediator.tell(new DistributedPubSubMediator.Subscribe("default",
                getSelf()), getSelf());
        this.topic = "default";
        this.consumer = null;
    }

    public Subscriber(String topic, InfoConsumer consumer) {
        this.topic = topic;
        this.consumer = consumer;
        ActorRef mediator = DistributedPubSubExtension.get(
                getContext().system()).mediator();
        mediator.tell(
                new DistributedPubSubMediator.Subscribe(topic, getSelf()),
                getSelf());
    }

    public Subscriber(String topic, String group, InfoConsumer consumer) {
        this.topic = topic;
        this.consumer = consumer;
        this.group = group;
        ActorRef mediator = DistributedPubSubExtension.get(
                getContext().system()).mediator();
        mediator.tell(new DistributedPubSubMediator.Subscribe(topic, group,
                getSelf()), getSelf());
    }

    @Override
    public void onReceive(Object msg) {
        if (msg instanceof String) {
            router.route(msg, getSender());
        } else if (msg instanceof DistributedPubSubMediator.SubscribeAck) {
            log.info("订阅成功，主题是" + "--" + topic);
            List<Routee> routees = new ArrayList<Routee>();
            for (int i = 0; i < 5; i++) {
                ActorRef r = getContext().actorOf(
                        Props.create(InfoRouterActor.class, consumer));
                getContext().watch(r);
                routees.add(new ActorRefRoutee(r));
            }
            router = new Router(new RoundRobinRoutingLogic(), routees);
        } else {
            unhandled(msg);
        }
    }

    public static void main(String[] args) {
        System.out.println (Integer.TYPE+"整形的最大取值范围是"+Integer.MAX_VALUE+"最小取值范围是"+Integer.MIN_VALUE
                +"位数是"+Integer.SIZE);
    }
}
