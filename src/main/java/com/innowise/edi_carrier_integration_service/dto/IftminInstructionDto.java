package com.innowise.edi_carrier_integration_service.dto;

public record IftminInstructionDto(String controlNumber, String documentType,
        String consignorName, String consignorAddress,
        String consigneeName, String consigneeAddress,
        String carrierName) {
}
