package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

@ApiModel(description = "Add a comment to a Fact.")
public class CreateFactCommentRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @NotNull
  private UUID fact;
  @ApiModelProperty(value = "Content of comment", example = "Hello World!", required = true)
  @NotNull
  @Size(min = 1)
  private String comment;
  @ApiModelProperty(value = "Set if new comment is a reply to an existing comment (takes comment UUID)")
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
