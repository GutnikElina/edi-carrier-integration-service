package com.innowise.edi_carrier_integration_service.exception;

public class EdiSecurityException extends EdiProcessingException {
    public EdiSecurityException(String message) {
        super(message);
    }

    public EdiSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
