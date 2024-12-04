
package io.helidon.benchmark.nima;

import com.jsoniter.output.JsonStream;

class JsonStreamSupplier {

    private static final int BUFFER_SIZE = 1024;
    private static final ThreadLocal<JsonStream> slot1 = new ThreadLocal<>();
    private static final ThreadLocal<JsonStream> slot2 = new ThreadLocal<>();

    public static JsonStream _borrowJsonStream() {
        JsonStream stream = slot1.get();
        if (stream != null) {
            slot1.set(null);
            return stream;
        }
        stream = slot2.get();
        if (stream != null) {
            slot2.set(null);
            return stream;
        }
        return new JsonStream(null, BUFFER_SIZE);
    }

    public static void _returnJsonStream(JsonStream jsonStream) {
        jsonStream.configCache = null;
        if (slot1.get() == null) {
            slot1.set(jsonStream);
            return;
        }
        if (slot2.get() == null) {
            slot2.set(jsonStream);
        }
    }

    public static JsonStream borrowJsonStream() {
        return new JsonStream(null, BUFFER_SIZE);
    }

    public static void returnJsonStream(JsonStream jsonStream) {
    }
}
