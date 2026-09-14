package com.innowise.edi_carrier_integration_service.exception;

public class MessageIntegrityCheckException extends RuntimeException {
    public MessageIntegrityCheckException(String message) {
        super(message);
    }
}