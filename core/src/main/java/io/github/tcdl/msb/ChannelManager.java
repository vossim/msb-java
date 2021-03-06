package io.github.tcdl.msb;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tcdl.msb.adapters.*;
import io.github.tcdl.msb.api.Callback;
import io.github.tcdl.msb.api.exception.ConsumerSubscriptionException;
import io.github.tcdl.msb.collector.CollectorManager;
import io.github.tcdl.msb.config.MsbConfig;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.impl.SimpleMessageHandlerResolverImpl;
import io.github.tcdl.msb.monitor.agent.ChannelMonitorAgent;
import io.github.tcdl.msb.monitor.agent.NoopChannelMonitorAgent;
import io.github.tcdl.msb.threading.MessageHandlerInvoker;
import io.github.tcdl.msb.support.JsonValidator;
import io.github.tcdl.msb.support.Utils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ChannelManager} creates consumers or producers on demand and manages them.
 */
public class ChannelManager {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelManager.class);
    private static final String RESPONDER_LOGGING_NAME = "Responder server";

    private final MsbConfig msbConfig;
    private final Clock clock;
    private final JsonValidator validator;
    private final ObjectMapper messageMapper;
    private final AdapterFactory adapterFactory;
    private final MessageHandlerInvoker messageHandlerInvoker;
    private ChannelMonitorAgent channelMonitorAgent;

    private final Map<String, Producer> producersByTopic;
    private final Map<String, Consumer> consumersByTopic;

    public ChannelManager(MsbConfig msbConfig, Clock clock, JsonValidator validator, ObjectMapper messageMapper, AdapterFactory adapterFactory, MessageHandlerInvoker messageHandlerInvoker) {
        this.msbConfig = msbConfig;
        this.clock = clock;
        this.validator = validator;
        this.messageMapper = messageMapper;
        this.adapterFactory = adapterFactory;
        this.messageHandlerInvoker = messageHandlerInvoker;
        this.producersByTopic = new ConcurrentHashMap<>();
        this.consumersByTopic = new ConcurrentHashMap<>();

        channelMonitorAgent = new NoopChannelMonitorAgent();
    }

    public Producer findOrCreateProducer(final String topic) {
        Validate.notNull(topic, "field 'topic' is null");
        Producer producer = producersByTopic.computeIfAbsent(topic, key -> {
            Producer newProducer = createProducer(key);
            channelMonitorAgent.producerTopicCreated(key);
            return newProducer;
        });

        return producer;
    }

    /**
     * Start consuming messages on specified topic with handler.
     * Calls to subscribe() and unsubscribe() have to be properly synchronized by client code not to lose messages.
     *
     * @param topic
     * @param messageHandler handler for processing messages
     * @throws ConsumerSubscriptionException if subscriber for topic already exist
     */
    public synchronized boolean subscribe(String topic, MessageHandler messageHandler) {
        Validate.notNull(topic, "field 'topic' is null");
        Validate.notNull(messageHandler, "field 'messageHandler' is null");
        if (consumersByTopic.get(topic) != null) {
            throw new ConsumerSubscriptionException("Subscriber for this topic: " + topic + " already exist");
        } else {
            Consumer newConsumer = createConsumer(topic, false, new SimpleMessageHandlerResolverImpl(messageHandler, RESPONDER_LOGGING_NAME));
            channelMonitorAgent.consumerTopicCreated(topic);
            consumersByTopic.put(topic, newConsumer);
            return false;
        }
    }

    /**
     * Start consuming response messages on specified topic and pass processing to CollectorManager.
     * Calls to subscribe() and unsubscribe() have to be properly synchronized by client code not to lose messages.
     *
     * @param topic
     * @param collectorManager resolver of {@link MessageHandler}  for processing messages
     * @throws ConsumerSubscriptionException if subscriber for topic already exist
     */
    public synchronized boolean subscribeForResponses(String topic, CollectorManager collectorManager) {
        Validate.notNull(topic, "field 'topic' is null");
        Validate.notNull(collectorManager, "field 'collectorManager' is null");
        if (consumersByTopic.get(topic) != null) {
            throw new ConsumerSubscriptionException("Subscriber for this topic: " + topic + " already exist");
        } else {
            Consumer newConsumer = createConsumer(topic, true, collectorManager);
            channelMonitorAgent.consumerTopicCreated(topic);
            consumersByTopic.put(topic, newConsumer);
            return false;
        }
    }

    /**
     * Stop consuming messages on specified topic.
     * Calls to subscribe() and unsubscribe() have to be properly synchronized by client code not to lose messages.
     *
     * @param topic
     */
    public void unsubscribe(String topic) {
        Consumer consumer = consumersByTopic.remove(topic);
        if (consumer != null) {
            consumer.end();
            channelMonitorAgent.consumerTopicRemoved(topic);
        }
    }

    private Producer createProducer(String topic) {
        Utils.validateTopic(topic);

        ProducerAdapter adapter = getAdapterFactory().createProducerAdapter(topic);
        Callback<Message> handler = message -> channelMonitorAgent.producerMessageSent(topic);
        return new Producer(adapter, topic, handler, messageMapper);
    }

    private Consumer createConsumer(String topic, boolean isResponseTopic, MessageHandlerResolver messageHandlerResolver) {
        Utils.validateTopic(topic);

        ConsumerAdapter adapter = getAdapterFactory().createConsumerAdapter(topic, isResponseTopic );

        return new Consumer(adapter, messageHandlerInvoker, topic, messageHandlerResolver, msbConfig, clock, channelMonitorAgent, validator, messageMapper);
    }

    public void shutdown() {
        LOG.info("Shutting down...");
        adapterFactory.shutdown();
        messageHandlerInvoker.shutdown();
        LOG.info("Shutdown complete");
    }

    private AdapterFactory getAdapterFactory() {
        return this.adapterFactory;
    }


    public void setChannelMonitorAgent(ChannelMonitorAgent channelMonitorAgent) {
        this.channelMonitorAgent = channelMonitorAgent;
    }
}
