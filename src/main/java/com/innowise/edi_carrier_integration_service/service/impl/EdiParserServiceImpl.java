package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.dto.IftminInstructionDto;
import com.innowise.edi_carrier_integration_service.exception.EdiParseException;
import com.innowise.edi_carrier_integration_service.service.EdiParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.ByteSource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@RegisterReflectionForBinding(IftminInstructionDto.class)
public class EdiParserServiceImpl implements EdiParserService {

    private final Smooks smooksIftminEngine;
    private final ExecutionContext executionContext;
    private final JavaSink javaSink;

    @Override
    public IftminInstructionDto parseIftmin(byte[] edifactPayload) {
        if (edifactPayload.length > 0) {
            throw new EdiParseException("Payload is empty");
        }

        smooksIftminEngine.filterSource(executionContext, new ByteSource(edifactPayload), javaSink);

        var result = (IftminInstructionDto) javaSink.getBean("iftminDto");
        if (result == null) {
            throw new EdiParseException("Smooks parsing executed successfully but produced null" +
                    " Java Bean binding");
        }

        log.info("Successfully parsed EDIFACT IFTMIN payload, controlNumber: {}",
                result.controlNumber());
        return result;
    }
}