package shop.zailushang.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


// F**K checked Exception in read Json
public class JsonUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static ObjectNode createObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    public static <T> T readValue(String jsonStr, Class<T> cls) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode readTree(String jsonStr) {
        try {
            return OBJECT_MAPPER.readTree(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static <T> T treeToValue(ObjectNode objectNode, Class<T> cls) {
        try {
            return OBJECT_MAPPER.treeToValue(objectNode, cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
