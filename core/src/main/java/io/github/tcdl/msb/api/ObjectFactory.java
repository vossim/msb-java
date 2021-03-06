package io.github.tcdl.msb.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.tcdl.msb.api.monitor.AggregatorStats;
import io.github.tcdl.msb.api.monitor.ChannelMonitorAggregator;

import java.lang.reflect.Type;

/**
 * Provides methods for creation client-facing API classes.
 */
public interface ObjectFactory {

    /**
     * Convenience method that specifies response payload type as {@link JsonNode}
     *
     * See {@link #createRequester(String, RequestOptions, TypeReference)}
     */
    default Requester<JsonNode> createRequester(String namespace, RequestOptions requestOptions) {
        return createRequester(namespace, requestOptions, JsonNode.class);
    }

    /**
     * Convenience method that allows to specify response payload type via {@link Class}
     *
     * See {@link #createRequester(String, RequestOptions, TypeReference)}
     */
    default <T> Requester<T> createRequester(String namespace, RequestOptions requestOptions, Class<T> payloadClass) {
        return createRequester(namespace, requestOptions, new TypeReference<T>() {
            @Override
            public Type getType() {
                return payloadClass;
            }
        });
    }

    /**
     * @param namespace             topic name to send a request to
     * @param requestOptions        options to configure a requester
     * @param payloadTypeReference  expected payload type of response messages
     * @return new instance of a {@link Requester} with original message
     */
    <T> Requester<T> createRequester(String namespace, RequestOptions requestOptions, TypeReference<T> payloadTypeReference);

    /**
     * Same as
     * {@link ObjectFactory#createRequesterForSingleResponse(java.lang.String, java.lang.Class, io.github.tcdl.msb.api.RequestOptions)}
     * with default request options
     */
    <T> Requester<T> createRequesterForSingleResponse(String namespace, Class<T> payloadClass);

    /**
     * Creates requester for single response with default response and acknowledgment timeouts
     *
     * @param namespace          topic name to send a request to
     * @param payloadClass       expected payload class of response messages
     * @param baseRequestOptions request options to be used as a source of response timeout and {@link MessageTemplate}.
     *                           Response time however will be 1 even if {@code baseRequestOptions} define other value.
     * @return new instance of a {@link Requester} with original message
     */
    <T> Requester<T> createRequesterForSingleResponse(String namespace, Class<T> payloadClass, RequestOptions baseRequestOptions);

    /**
     * Convenience method that specifies incoming payload type as {@link JsonNode}
     *
     * See {@link #createRequester(String, RequestOptions, TypeReference)}
     */
    default ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<JsonNode> requestHandler) {
        return createResponderServer(namespace, messageTemplate, requestHandler, JsonNode.class);
    }

    /**
     * Convenience method that allows to specify incoming payload type via {@link Class}
     *
     * See {@link #createRequester(String, RequestOptions, TypeReference)}
     */
    default <T> ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<T> requestHandler, Class<T> payloadClass) {
        return createResponderServer(namespace, messageTemplate, requestHandler, new TypeReference<T>() {
            @Override
            public Type getType() {
                return payloadClass;
            }
        });
    }

    default <T> ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<T> requestHandler, ResponderServer.ErrorHandler errorHandler, Class<T> payloadClass) {
        return createResponderServer(namespace, messageTemplate, requestHandler, errorHandler, new TypeReference<T>() {
            @Override
            public Type getType() {
                return payloadClass;
            }
        });
    }

    /**
     * Same as
     * {@link ObjectFactory#createRequesterForFireAndForget(java.lang.String, io.github.tcdl.msb.api.MessageTemplate)}
     * with default messageTemplate
     */
    <T> Requester<T> createRequesterForFireAndForget(String namespace);

    /**
     * Creates requester that doesn't wait for any responses or acknowledgments
     *
     * @param namespace       topic name to send a request to
     * @param messageTemplate {@link MessageTemplate} to be used
     * @return new instance of a {@link Requester} with original message
     */
    <T> Requester<T> createRequesterForFireAndForget(String namespace, MessageTemplate messageTemplate);

    /**
     * @param namespace                 topic on a bus for listening on incoming requests
     * @param messageTemplate           template used for creating response messages
     * @param requestHandler            handler for processing the request
     * @param payloadTypeReference      expected payload type of incoming messages
     * @return new instance of a {@link ResponderServer} that unmarshals payload into specified payload type
     */
    <T> ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<T> requestHandler, TypeReference<T> payloadTypeReference);

    /**
     * @param namespace                 topic on a bus for listening on incoming requests
     * @param messageTemplate           template used for creating response messages
     * @param requestHandler            handler for processing the request
     * @param errorHandler              handler for errors to be called after default
     * @param payloadTypeReference      expected payload type of incoming messages
     * @return new instance of a {@link ResponderServer} that unmarshals payload into specified payload type
     */
    <T> ResponderServer createResponderServer(String namespace, MessageTemplate messageTemplate,
            ResponderServer.RequestHandler<T> requestHandler, ResponderServer.ErrorHandler errorHandler, TypeReference<T> payloadTypeReference);

    /**
     * @return instance of converter to convert any objects
     * using object mapper from {@link MsbContext}
     */
    PayloadConverter getPayloadConverter();

    /**
     * @param aggregatorStatsHandler this handler is invoked whenever statistics is updated via announcement channel or heartbeats.
     *                               THE HANDLER SHOULD BE THREAD SAFE because it may be invoked from parallel threads.
     * @return new instance of {@link ChannelMonitorAggregator}
     */
    ChannelMonitorAggregator createChannelMonitorAggregator(Callback<AggregatorStats> aggregatorStatsHandler);

    /**
     * Shuts down the factory and all the objects that were created by it.
     */
    void shutdown();
}