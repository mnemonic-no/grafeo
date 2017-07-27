package no.mnemonic.act.platform.api.service.v1;

import no.mnemonic.commons.utilities.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * The TraversalResult is a container holding the results from a graph traversal. It can also be used to transport
 * additional messages from the traversal back to the REST endpoint which called the service method.
 */
public class TraversalResult {

  private final Collection<?> values;
  private final Collection<Message> messages;

  private TraversalResult(Collection<?> values, Collection<Message> messages) {
    this.values = ObjectUtils.ifNotNull(values, Collections::unmodifiableCollection);
    this.messages = ObjectUtils.ifNotNull(messages, Collections::unmodifiableCollection);
  }

  public Collection<?> getValues() {
    return values;
  }

  public Collection<Message> getMessages() {
    return messages;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Collection<Object> values = new ArrayList<>();
    private final Collection<Message> messages = new ArrayList<>();

    private Builder() {
    }

    public TraversalResult build() {
      return new TraversalResult(values, messages);
    }

    public Builder addValue(Object value) {
      this.values.add(value);
      return this;
    }

    public Builder addMessage(String message, String template) {
      this.messages.add(new Message(message, template));
      return this;
    }
  }

  public static class Message {
    private final String message;
    private final String template;

    private Message(String message, String template) {
      this.message = message;
      this.template = template;
    }

    public String getMessage() {
      return message;
    }

    public String getTemplate() {
      return template;
    }
  }
}
