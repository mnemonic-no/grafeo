package no.mnemonic.act.platform.api.request.v1;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import no.mnemonic.act.platform.api.request.ValidatingRequest;
import no.mnemonic.act.platform.api.validation.constraints.ServiceNotNull;

import javax.validation.constraints.NotBlank;
import java.util.UUID;

@ApiModel(description = "Add a comment to a Fact.")
public class CreateFactCommentRequest implements ValidatingRequest {

  @ApiModelProperty(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @ApiModelProperty(value = "Content of comment", example = "Hello World!", required = true)
  @NotBlank
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
