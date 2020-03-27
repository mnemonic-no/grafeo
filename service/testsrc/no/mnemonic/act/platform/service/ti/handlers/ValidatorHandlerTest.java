package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.collections.SetUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ValidatorHandlerTest {

  @Mock
  private ValidatorFactory validatorFactory;

  private ValidatorHandler validatorHandler;

  @Before
  public void initialize() {
    initMocks(this);
    validatorHandler = new ValidatorHandler(validatorFactory);
  }

  @Test
  public void testValidatorExists() throws InvalidArgumentException {
    validatorHandler.assertValidatorExists("someValidator", "someParameter");
    verify(validatorFactory).get("someValidator", "someParameter");
  }

  @Test
  public void testValidatorDoesNotExist() {
    when(validatorFactory.get("someValidator", "someParameter")).thenThrow(IllegalArgumentException.class);

    InvalidArgumentException ex = assertThrows(
      InvalidArgumentException.class,
      () -> validatorHandler.assertValidatorExists("someValidator", "someParameter")
    );
    assertEquals(
      SetUtils.set("validator.not.exist"),
      SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }
}
