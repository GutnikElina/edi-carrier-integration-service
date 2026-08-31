package com.innowise.edi_carrier_integration_service.edi.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IftminInstructionDto {
    private String controlNumber;
    private String documentType;
    private String consignorName;
    private String consignorAddress;
    private String consigneeName;
    private String consigneeAddress;
    private String carrierName;
}
