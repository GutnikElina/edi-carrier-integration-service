package com.innowise.edi_carrier_integration_service.edi.domain.exception;

public class EdiParseException extends EdiProcessingException {
  public EdiParseException(String message) {
    super(message);
  }

  public EdiParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
