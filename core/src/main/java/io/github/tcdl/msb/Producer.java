package io.github.tcdl.msb;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tcdl.msb.adapters.ProducerAdapter;
import io.github.tcdl.msb.api.Callback;
import io.github.tcdl.msb.api.exception.ChannelException;
import io.github.tcdl.msb.api.exception.JsonConversionException;
import io.github.tcdl.msb.api.message.Message;
import io.github.tcdl.msb.support.Utils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Producer} is a component responsible for producing messages to the bus.
 */
public class Producer {

    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    private final ProducerAdapter rawAdapter;
    private final Callback<Message> messageHandler;
    private final ObjectMapper messageMapper;

    public Producer(ProducerAdapter rawAdapter, String topic, Callback<Message> messageHandler, ObjectMapper messageMapper) {
        LOG.debug("Creating producer for topic: {}", topic);
        Validate.notNull(rawAdapter, "the 'rawAdapter' must not be null");
        Validate.notNull(topic, "the 'topic' must not be null");
        Validate.notNull(messageHandler, "the 'messageHandler' must not be null");
        Validate.notNull(messageMapper, "the 'messageMapper' must not be null");

        this.rawAdapter = rawAdapter;
        this.messageHandler = messageHandler;
        this.messageMapper = messageMapper;
    }

    public void publish(Message message) {
        try {
            String jsonMessage = Utils.toJson(message, messageMapper);
            LOG.debug("Publishing message to adapter : {}", jsonMessage);
            rawAdapter.publish(jsonMessage);
            messageHandler.call(message);
        } catch (ChannelException | JsonConversionException e) {
            LOG.error("Exception while message publish to adapter", e);
            throw e;
        }
    }


}
