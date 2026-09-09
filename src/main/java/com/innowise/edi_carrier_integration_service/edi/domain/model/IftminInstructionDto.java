package com.innowise.edi_carrier_integration_service.edi.domain.model;

public record IftminInstructionDto(String controlNumber, String documentType,
        String consignorName, String consignorAddress,
        String consigneeName, String consigneeAddress,
        String carrierName) {
}
