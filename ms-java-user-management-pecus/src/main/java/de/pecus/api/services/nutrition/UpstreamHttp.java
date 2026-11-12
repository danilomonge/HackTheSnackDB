package de.pecus.api.services.nutrition;

import java.io.IOException;
import java.util.Map;

/** Minimal HTTP access to the USDA and Azure Translator APIs (replaceable in tests). */
public interface UpstreamHttp {

    /** Status code and body of an upstream response. */
    final class Result {
        private final int status;
        private final String body;

        public Result(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int status() { return status; }
        public String body() { return body; }
    }

    Result get(String url) throws IOException;

    Result postJson(String url, Map<String, String> headers, String json) throws IOException;
}
