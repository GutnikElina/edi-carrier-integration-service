package com.innowise.edi_carrier_integration_service.edi.infrastructure.parser;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiParseException;
import com.innowise.edi_carrier_integration_service.edi.domain.model.IftminInstructionDto;
import java.util.Objects;
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
    Objects.requireNonNull(edifactPayload, "EDIFACT payload byte array must not be null");
    if (edifactPayload.length == 0) {
      throw new EdiParseException("EDIFACT payload byte array is empty");
    }

    try {
      ExecutionContext executionContext = smooksIftminEngine.createExecutionContext();
      JavaSink javaSink = new JavaSink();

      smooksIftminEngine.filterSource(executionContext, new ByteSource(edifactPayload), javaSink);

      IftminInstructionDto dto = (IftminInstructionDto) javaSink.getBean("iftminDto");
      if (dto == null) {
        throw new EdiParseException(
            "Smooks parsing executed successfully but produced null Java Bean binding");
      }

      log.info(
          "Successfully parsed EDIFACT IFTMIN payload, controlNumber: {}", dto.getControlNumber());
      return dto;
    } catch (EdiParseException e) {
      throw e;
    } catch (Exception e) {
      throw new EdiParseException(
          "Unhandled error occurred during Smooks EDIFACT IFTMIN parsing", e);
    }
  }
}
