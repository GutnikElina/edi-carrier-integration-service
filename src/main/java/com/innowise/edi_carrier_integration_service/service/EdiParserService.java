package com.innowise.edi_carrier_integration_service.service;

import com.innowise.edi_carrier_integration_service.dto.IftminInstructionDto;

public interface EdiParserService {
    IftminInstructionDto parseIftmin(byte[] edifactPayload);
}
