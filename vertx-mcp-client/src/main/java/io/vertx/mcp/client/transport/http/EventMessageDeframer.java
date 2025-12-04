package io.vertx.mcp.client.transport.http;

import io.vertx.core.buffer.Buffer;
import io.vertx.mcp.client.MessageDeframer;

import java.util.ArrayList;
import java.util.List;

/**
 * Message deframer for Server-Sent Events (SSE) format.
 * Parses SSE messages which consist of lines prefixed with "data: " and separated by empty lines.
 */
public class EventMessageDeframer implements MessageDeframer {

  private static final String DATA_PREFIX = "data: ";
  private static final String EVENT_PREFIX = "event: ";
  private static final String ID_PREFIX = "id: ";
  private static final byte LF = '\n';
  private static final byte CR = '\r';

  private long maxMessageSize;
  private Buffer lineBuffer = Buffer.buffer();
  private List<String> currentEventData = new ArrayList<>();
  private List<Buffer> completedMessages = new ArrayList<>();
  private boolean streamEnded = false;

  @Override
  public void maxMessageSize(long maxMessageSize) {
    this.maxMessageSize = maxMessageSize;
  }

  @Override
  public void update(Buffer chunk) {
    if (chunk == null || chunk.length() == 0) {
      return;
    }

    int index = 0;
    while (index < chunk.length()) {
      byte b = chunk.getByte(index);

      if (b == LF) {
        // Process complete line
        processLine(lineBuffer);
        lineBuffer = Buffer.buffer();
      } else if (b == CR) {
        // Skip CR, handle CRLF line endings
        if (index + 1 < chunk.length() && chunk.getByte(index + 1) == LF) {
          index++; // Skip the LF that follows
        }
        // Process complete line
        processLine(lineBuffer);
        lineBuffer = Buffer.buffer();
      } else {
        lineBuffer.appendByte(b);
      }

      index++;
    }

    // Check if accumulated data exceeds max message size
    if (maxMessageSize > 0 && lineBuffer.length() > maxMessageSize) {
      // Clear buffers to prevent memory issues
      lineBuffer = Buffer.buffer();
      currentEventData.clear();
    }
  }

  private void processLine(Buffer line) {
    String lineStr = line.toString();

    // Empty line marks the end of an SSE event
    if (lineStr.isEmpty()) {
      if (!currentEventData.isEmpty()) {
        // Combine all data lines into a single message
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < currentEventData.size(); i++) {
          if (i > 0) {
            message.append('\n');
          }
          message.append(currentEventData.get(i));
        }
        completedMessages.add(Buffer.buffer(message.toString()));
        currentEventData.clear();
      }
      return;
    }

    // Process data lines
    if (lineStr.startsWith(DATA_PREFIX)) {
      String data = lineStr.substring(DATA_PREFIX.length());
      currentEventData.add(data);
    }
    // Ignore event, id, and comment lines for now
    // These could be processed if needed for more advanced SSE handling
  }

  @Override
  public void end() {
    streamEnded = true;

    // Process any remaining line data
    if (lineBuffer.length() > 0) {
      processLine(lineBuffer);
      lineBuffer = Buffer.buffer();
    }

    // If there's accumulated event data without a trailing empty line, process it
    if (!currentEventData.isEmpty()) {
      StringBuilder message = new StringBuilder();
      for (int i = 0; i < currentEventData.size(); i++) {
        if (i > 0) {
          message.append('\n');
        }
        message.append(currentEventData.get(i));
      }
      completedMessages.add(Buffer.buffer(message.toString()));
      currentEventData.clear();
    }
  }

  @Override
  public Object next() {
    if (!completedMessages.isEmpty()) {
      return completedMessages.remove(0);
    }
    return null;
  }
}

