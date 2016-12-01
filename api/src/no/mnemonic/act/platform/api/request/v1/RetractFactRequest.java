package no.mnemonic.act.platform.api.request.v1;

import no.mnemonic.commons.utilities.collections.ListUtils;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class RetractFactRequest {

  @NotNull
  private UUID fact;
  private UUID organization;
  private UUID source;
  private AccessMode accessMode;
  private String comment;
  private List<UUID> acl;
  // TODO: Add confidenceLevel once defined.

  public UUID getFact() {
    return fact;
  }

  public RetractFactRequest setFact(UUID fact) {
    this.fact = fact;
    return this;
  }

  public UUID getOrganization() {
    return organization;
  }

  public RetractFactRequest setOrganization(UUID organization) {
    this.organization = organization;
    return this;
  }

  public UUID getSource() {
    return source;
  }

  public RetractFactRequest setSource(UUID source) {
    this.source = source;
    return this;
  }

  public AccessMode getAccessMode() {
    return accessMode;
  }

  public RetractFactRequest setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
    return this;
  }

  public String getComment() {
    return comment;
  }

  public RetractFactRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public List<UUID> getAcl() {
    return acl;
  }

  public RetractFactRequest setAcl(List<UUID> acl) {
    this.acl = acl;
    return this;
  }

  public RetractFactRequest addAcl(UUID acl) {
    this.acl = ListUtils.addToList(this.acl, acl);
    return this;
  }

}
