package no.mnemonic.act.platform.api.request.v1;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

public class CreateFactCommentRequest {

  @NotNull
  private UUID fact;
  @NotNull
  @Size(min = 1)
  private String comment;
  private UUID replyTo;

  public UUID getFact() {
    return fact;
  }

  public CreateFactCommentRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public CreateFactCommentRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public UUID getReplyTo() {
    return replyTo;
  }

  public CreateFactCommentRequest setReplyTo(UUID replyTo) {
    this.replyTo = replyTo;
    return this;
  }

}
