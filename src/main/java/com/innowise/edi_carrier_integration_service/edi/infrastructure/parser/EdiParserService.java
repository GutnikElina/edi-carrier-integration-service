package com.innowise.edi_carrier_integration_service.edi.infrastructure.parser;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiParseException;
import com.innowise.edi_carrier_integration_service.edi.domain.model.IftminInstructionDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.ByteSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EdiParserService {

  private final Smooks smooksIftminEngine;

  public IftminInstructionDto parseIftmin(byte[] edifactPayload) {
    return Optional.ofNullable(edifactPayload)
        .filter(payload -> payload.length > 0)
        .map(this::executeSmooksParsing)
        .orElseThrow(
            () ->
                new EdiParseException(
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
                    dto.getControlNumber());
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
