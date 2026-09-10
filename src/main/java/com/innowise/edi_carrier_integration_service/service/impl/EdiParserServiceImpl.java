package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.exception.EdiParseException;
import com.innowise.edi_carrier_integration_service.dto.IftminInstructionDto;
import com.innowise.edi_carrier_integration_service.service.EdiParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.ByteSource;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@RegisterReflectionForBinding(IftminInstructionDto.class)
public class EdiParserServiceImpl implements EdiParserService {

    private final Smooks smooksIftminEngine;

    @Override
    public IftminInstructionDto parseIftmin(byte[] edifactPayload) {
        return Optional.ofNullable(edifactPayload)
            .filter(payload -> payload.length > 0)
            .map(this::executeSmooksParsing)
            .orElseThrow(
                    () -> new EdiParseException(
                            "Smooks parsing executed successfully but produced null Java Bean binding"));
    }

    private IftminInstructionDto executeSmooksParsing(byte[] payload) {
        try {
            ExecutionContext executionContext = smooksIftminEngine.createExecutionContext();
            JavaSink javaSink = new JavaSink();

            smooksIftminEngine.filterSource(executionContext, new ByteSource(payload), javaSink);

            return Optional.ofNullable((IftminInstructionDto) javaSink.getBean("iftminDto"))
                .map(
                        dto -> {
                            log.info(
                                    "Successfully parsed EDIFACT IFTMIN payload, controlNumber: {}",
                                    dto.controlNumber());
                            return dto;
                        })
                .orElse(null);
        } catch (EdiParseException e) {
            log.error("Smooks parsing executed successfully but produced null Java Bean binding");
            throw e;
        } catch (Exception e) {
            log.error("Unhandled error occurred during Smooks EDIFACT IFTMIN parsing");
            throw new EdiParseException(
                    "Unhandled error occurred during Smooks EDIFACT IFTMIN parsing", e);
        }
    }
}
