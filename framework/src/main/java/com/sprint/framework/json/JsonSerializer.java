package com.sprint.framework.json;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

public final class JsonSerializer {
    private JsonSerializer() {
    }

    public static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence || value instanceof Character) {
            return quote(value.toString());
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map) {
            StringBuilder json = new StringBuilder("{");
            Iterator<?> entries = ((Map<?, ?>) value).entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entries.next();
                if (json.length() > 1) {
                    json.append(',');
                }
                json.append(quote(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
            }
            return json.append('}').toString();
        }
        if (value instanceof Iterable) {
            StringBuilder json = new StringBuilder("[");
            for (Object item : (Iterable<?>) value) {
                if (json.length() > 1) {
                    json.append(',');
                }
                json.append(toJson(item));
            }
            return json.append(']').toString();
        }
        if (value.getClass().isArray()) {
            StringBuilder json = new StringBuilder("[");
            for (int index = 0; index < Array.getLength(value); index++) {
                if (json.length() > 1) {
                    json.append(',');
                }
                json.append(toJson(Array.get(value, index)));
            }
            return json.append(']').toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}