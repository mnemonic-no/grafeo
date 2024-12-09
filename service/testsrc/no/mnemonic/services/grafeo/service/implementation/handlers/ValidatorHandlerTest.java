package no.mnemonic.services.grafeo.service.implementation.handlers;

import no.mnemonic.commons.utilities.collections.SetUtils;
import no.mnemonic.services.grafeo.api.exceptions.InvalidArgumentException;
import no.mnemonic.services.grafeo.service.validators.Validator;
import no.mnemonic.services.grafeo.service.validators.ValidatorConfigurationException;
import no.mnemonic.services.grafeo.service.validators.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.mnemonic.services.grafeo.service.validators.ValidatorConfigurationException.Reason.Misconfigured;
import static no.mnemonic.services.grafeo.service.validators.ValidatorConfigurationException.Reason.NotFound;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ValidatorHandlerTest {

  @Mock
  private ValidatorFactory validatorFactory;
  @InjectMocks
  private ValidatorHandler validatorHandler;

  @Test
  public void testValidatorExists() throws InvalidArgumentException {
    when(validatorFactory.get("someValidator", "someParameter")).thenReturn(createValidator(Validator.ApplicableType.FactType));
    validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType);
    verify(validatorFactory).get("someValidator", "someParameter");
  }

  @Test
  public void testValidatorDoesNotExist() {
    when(validatorFactory.get("someValidator", "someParameter")).thenThrow(new ValidatorConfigurationException("Test", NotFound));

    InvalidArgumentException ex = assertThrows(
            InvalidArgumentException.class,
            () -> validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType)
    );
    assertEquals(
            SetUtils.set("validator.not.exist"),
            SetUtils.set(ex.getValidationErrors(), InvalidArgumentException.ValidationError::getMessageTemplate));
  }

  @Test
  public void testValidatorMisconfigured() {
    when(validatorFactory.get("someValidator", "someParameter")).thenThrow(new ValidatorConfigurationException("Test", Misconfigured));

    InvalidArgumentException ex = assertThrows(
            InvalidArgumentException.class,
            () -> validatorHandler.assertValidator("someValidator", "someParameter", Validator.ApplicableType.FactType)
    );
    assertEquals(
            SetUtils.set("validator.misconfigured"),
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
