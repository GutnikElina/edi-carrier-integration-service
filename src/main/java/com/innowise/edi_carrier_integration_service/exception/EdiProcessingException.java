package com.innowise.edi_carrier_integration_service.exception;

public class EdiProcessingException extends RuntimeException {
    public EdiProcessingException(String message) {
        super(message);
    }

    public EdiProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
