package com.ebikes.notifications.support.json;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@UtilityClass
public class JsonNodeUtilities {

  public static JsonNode extractFirstEntry(ObjectMapper objectMapper, String responseBody) {
    JsonNode root = objectMapper.readTree(responseBody);
    return root.isArray() && root.get(0) != null && !root.get(0).isNull() ? root.get(0) : root;
  }

  public static String stringValueOrNull(JsonNode node) {
    return node.isString() ? node.stringValue() : null;
  }
}
