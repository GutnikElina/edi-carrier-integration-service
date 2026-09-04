package com.innowise.edi_carrier_integration_service.edi.domain.exception;

public class EdiSecurityException extends EdiProcessingException {
    public EdiSecurityException(String message) {
        super(message);
    }

    public EdiSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
