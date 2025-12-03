package io.vertx.mcp.client.transport.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mcp.client.MessageDecoder;

public class EventStreamDecoder implements MessageDecoder {

  private final StringBuilder sseBuffer = new StringBuilder();

  @Override
  public JsonObject decode(Buffer buffer) {
    String data = buffer.toString();
    sseBuffer.append(data);

    String buffered = sseBuffer.toString();
    String[] lines = buffered.split("\n");

    StringBuilder currentMessage = new StringBuilder();
    int lastProcessedIndex = -1;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];

      if (line.isEmpty() || line.equals("\r")) {
        if (currentMessage.length() > 0) {
          try {
            return new JsonObject(currentMessage.toString());
          } catch (Exception e) {
            e.printStackTrace();
            /*if (exceptionHandler != null) {
              exceptionHandler.handle(e);
            }*/
          }
          currentMessage.setLength(0);
          lastProcessedIndex = i;
        }
      } else if (line.startsWith("data:")) {
        String messageData = line.substring(5).trim();
        currentMessage.append(messageData);
      }
    }

    if (lastProcessedIndex >= 0 && lastProcessedIndex < lines.length - 1) {
      sseBuffer.setLength(0);
      for (int i = lastProcessedIndex + 1; i < lines.length; i++) {
        sseBuffer.append(lines[i]);
        if (i < lines.length - 1) {
          sseBuffer.append("\n");
        }
      }
    } else if (lastProcessedIndex == lines.length - 1) {
      sseBuffer.setLength(0);
    }

    return null;
  }
}
