package com.innowise.edi_carrier_integration_service.exception;

public class CertificateValidationException extends RuntimeException {
    public CertificateValidationException(String message) {
        super(message);
    }
}