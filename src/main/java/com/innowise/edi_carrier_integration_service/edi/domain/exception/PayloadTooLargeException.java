package com.innowise.edi_carrier_integration_service.edi.domain.exception;

public class PayloadTooLargeException extends EdiProcessingException {
  public PayloadTooLargeException(String message) {
    super(message);
  }

  public PayloadTooLargeException(String message, Throwable cause) {
    super(message, cause);
  }
}
