package no.mnemonic.act.platform.service.ti.handlers;

import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.service.validators.Validator;
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
    when(validatorFactory.get("someValidator", "someParameter")).thenReturn(createValidator(Validator.ApplicableType.FactType));
    validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType);
    verify(validatorFactory).get("someValidator", "someParameter");
  }

  @Test
  public void testValidatorDoesNotExist() {
    when(validatorFactory.get("someValidator", "someParameter")).thenThrow(IllegalArgumentException.class);

    InvalidArgumentException ex = assertThrows(
            InvalidArgumentException.class,
            () -> validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType)
    );
    assertEquals(
            SetUtils.set("validator.not.exist"),
            SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidatorNotApplicable() {
    when(validatorFactory.get("someValidator", "someParameter")).thenReturn(createValidator(Validator.ApplicableType.FactType));

    InvalidArgumentException ex = assertThrows(
            InvalidArgumentException.class,
            () -> validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.ObjectType)
    );
    assertEquals(
            SetUtils.set("validator.not.applicable"),
            SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidatorApplicableFactType() throws InvalidArgumentException {
    when(validatorFactory.get("someValidator", "someParameter")).thenReturn(createValidator(Validator.ApplicableType.FactType));
    validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType);
  }

  @Test
  public void testValidatorApplicableObjectType() throws InvalidArgumentException {
    when(validatorFactory.get("someValidator", "someParameter")).thenReturn(createValidator(Validator.ApplicableType.ObjectType));
    validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.ObjectType);
  }

  @Test
  public void testValidatorApplicableBoth() throws InvalidArgumentException {
    when(validatorFactory.get("someValidator", "someParameter")).thenReturn(createValidator(Validator.ApplicableType.values()));
    validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType);
    validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.ObjectType);
  }

  private Validator createValidator(Validator.ApplicableType... applicableType) {
    return new Validator() {
      @Override
      public boolean validate(String value) {
        return true;
      }

      @Override
      public ApplicableType[] appliesTo() {
        return applicableType;
      }
    };
  }
}
