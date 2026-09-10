package com.innowise.edi_carrier_integration_service.exception;

public class PayloadTooLargeException extends EdiProcessingException {
    public PayloadTooLargeException(String message) {
        super(message);
    }

    public PayloadTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }
}
