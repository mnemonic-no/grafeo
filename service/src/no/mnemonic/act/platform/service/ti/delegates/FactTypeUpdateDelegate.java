package no.mnemonic.act.platform.service.ti.delegates;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.FactType;
import no.mnemonic.act.platform.api.request.v1.UpdateFactTypeRequest;
import no.mnemonic.act.platform.dao.cassandra.entity.FactTypeEntity;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.TiFunctionConstants;
import no.mnemonic.act.platform.service.ti.TiRequestContext;
import no.mnemonic.commons.utilities.StringUtils;
import no.mnemonic.commons.utilities.collections.CollectionUtils;
import no.mnemonic.commons.utilities.collections.ListUtils;

public class FactTypeUpdateDelegate extends AbstractDelegate {

  public static FactTypeUpdateDelegate create() {
    return new FactTypeUpdateDelegate();
  }

  public FactType handle(UpdateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    SecurityContext.get().checkPermission(TiFunctionConstants.updateTypes);

    FactTypeEntity entity = fetchExistingFactType(request.getId());

    if (!StringUtils.isBlank(request.getName())) {
      assertFactTypeNotExists(request.getName());
      entity.setName(request.getName());
    }

    if (!CollectionUtils.isEmpty(request.getAddObjectBindings())) {
      assertObjectTypesToBindExist(request.getAddObjectBindings(), "addObjectBindings.objectType");
      entity.setRelevantObjectBindings(ListUtils.concatenate(entity.getRelevantObjectBindings(), convertFactObjectBindingDefinitions(request.getAddObjectBindings())));
    }

    entity = TiRequestContext.get().getFactManager().saveFactType(entity);
    return TiRequestContext.get().getFactTypeConverter().apply(entity);
  }

}
