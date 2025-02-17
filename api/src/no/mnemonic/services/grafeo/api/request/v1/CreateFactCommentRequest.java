package no.mnemonic.services.grafeo.api.request.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import no.mnemonic.services.grafeo.api.request.ValidatingRequest;
import no.mnemonic.services.grafeo.api.validation.constraints.ServiceNotNull;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Add a comment to a Fact.")
public class CreateFactCommentRequest implements ValidatingRequest {

  @Schema(hidden = true)
  @ServiceNotNull
  private UUID fact;
  @Schema(description = "Content of comment", example = "Hello World!", requiredMode = REQUIRED)
  @NotBlank
  private String comment;
  @Schema(description = "Set if new comment is a reply to an existing comment (takes comment UUID)",
          example = "123e4567-e89b-12d3-a456-426655440000")
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
