package com.innowise.edi_carrier_integration_service.dto;

public record InboundEdiResult(IftminInstructionDto payloadDto, String mdnContent,
        String s3RawObjectPath) {
}