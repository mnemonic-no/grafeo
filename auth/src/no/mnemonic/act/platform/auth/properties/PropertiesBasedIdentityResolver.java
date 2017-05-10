package no.mnemonic.act.platform.auth.properties;

import no.mnemonic.act.platform.auth.IdentityResolver;
import no.mnemonic.act.platform.auth.properties.model.FunctionIdentifier;
import no.mnemonic.act.platform.auth.properties.model.OrganizationIdentifier;
import no.mnemonic.act.platform.auth.properties.model.SubjectIdentifier;
import no.mnemonic.services.common.auth.model.FunctionIdentity;
import no.mnemonic.services.common.auth.model.OrganizationIdentity;
import no.mnemonic.services.common.auth.model.SubjectIdentity;

import java.util.UUID;

/**
 * IdentityResolver implementation for the PropertiesBasedAccessController.
 */
public class PropertiesBasedIdentityResolver implements IdentityResolver {

  @Override
  public FunctionIdentity resolveFunctionIdentity(String name) {
    return FunctionIdentifier.builder().setName(name).build();
  }

  @Override
  public OrganizationIdentity resolveOrganizationIdentity(UUID id) {
    return OrganizationIdentifier.builder().setGlobalID(id).build();
  }

  @Override
  public SubjectIdentity resolveSubjectIdentity(UUID id) {
    return SubjectIdentifier.builder().setGlobalID(id).build();
  }

}
