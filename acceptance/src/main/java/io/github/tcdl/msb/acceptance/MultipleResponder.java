package io.github.tcdl.msb.acceptance;

import io.github.tcdl.msb.api.MessageTemplate;
import io.github.tcdl.msb.api.MsbContext;
import io.github.tcdl.msb.api.MsbContextBuilder;
import io.github.tcdl.msb.api.message.payload.RestPayload;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

public class MultipleResponder {

    public static void main(String... args) {
        MsbContext msbContext = new MsbContextBuilder()
                .enableShutdownHook(true)
                .build();
        runResponder("test:aggregator", msbContext);
    }

    public static void runResponder(String namespace, MsbContext msbContext) {
        MessageTemplate options = new MessageTemplate();
        msbContext.getObjectFactory().createResponderServer(namespace, options, (request, responderContext) -> {
            Map requestBody = request.getBody();
            System.out.println(">>> GOT request: " + requestBody);

            String requestId = (String) requestBody.get("requestId");
            SearchResponse response = new SearchResponse(requestId, "response");
            System.out.println(">>> SENDING response in request to " + requestId);
            responderContext.getResponder().send(new RestPayload.Builder<Object, Object, Object, SearchResponse>()
                    .withBody(response)
                    .build());
        }, new TypeReference<RestPayload<Object, Object, Object, Map>>() {})
        .listen();
    }

    public static class SearchResponse {

        private String requestId;
        private String result;

        public SearchResponse() {
        }

        public SearchResponse(String requestId, String result) {
            this.requestId = requestId;
            this.result = result;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        @Override public String toString() {
            return "response {requestId=" + requestId + ", result=" + result + "}";
        }
    }
}
