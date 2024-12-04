
package io.helidon.benchmark.nima;

import java.io.OutputStream;

import com.jsoniter.output.JsonStream;

public class JsonStreamSupplier {

    private static final int BUFFER_SIZE = 1024;
    private static final ThreadLocal<JsonStream> slot1 = new ThreadLocal<>();
    private static final ThreadLocal<JsonStream> slot2 = new ThreadLocal<>();

    public static JsonStream borrowJsonStream() {
        return borrowJsonStream(null);
    }

    public static JsonStream borrowJsonStream(OutputStream outputStream) {
        JsonStream stream = slot1.get();
        if (stream != null) {
            slot1.set(null);
            stream.reset(outputStream);
            return stream;
        }
        stream = slot2.get();
        if (stream != null) {
            slot2.set(null);
            stream.reset(outputStream);
            return stream;
        }
        return new JsonStream(outputStream, BUFFER_SIZE);
    }

    public static void returnJsonStream(JsonStream jsonStream) {
        jsonStream.configCache = null;
        if (slot1.get() == null) {
            slot1.set(jsonStream);
            return;
        }
        if (slot2.get() == null) {
            slot2.set(jsonStream);
        }
    }
}
