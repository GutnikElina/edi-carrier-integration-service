package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.innowise.edi_carrier_integration_service.edi.domain.exception.EdiParseException;
import com.innowise.edi_carrier_integration_service.edi.domain.model.IftminInstructionDto;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.parser.EdiParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smooks.Smooks;
import org.smooks.api.ExecutionContext;
import org.smooks.io.sink.JavaSink;
import org.smooks.io.source.ByteSource;

@ExtendWith(MockitoExtension.class)
class EdiParserServiceTest {

  @Mock private Smooks smooksIftminEngine;

  @Mock private ExecutionContext executionContext;

  @InjectMocks private EdiParserService ediParserService;

  @Test
  @DisplayName("Should parse EDIFACT payload successfully")
  void parseIftmin_success() {
    byte[] payload = "EDIFACT_DATA".getBytes();
    IftminInstructionDto expectedDto = new IftminInstructionDto();

    when(smooksIftminEngine.createExecutionContext()).thenReturn(executionContext);

    doAnswer(
            invocation -> {
              JavaSink javaSink = invocation.getArgument(2);
              javaSink.getResultMap().put("iftminDto", expectedDto);
              return null;
            })
        .when(smooksIftminEngine)
        .filterSource(any(ExecutionContext.class), any(ByteSource.class), any(JavaSink.class));

    IftminInstructionDto result = ediParserService.parseIftmin(payload);

    assertThat(result).isNotNull().isEqualTo(expectedDto);
  }

  @Test
  @DisplayName("Should throw EdiParseException when payload is empty")
  void parseIftmin_emptyPayload_throwsException() {
    assertThatThrownBy(() -> ediParserService.parseIftmin(new byte[0]))
        .isInstanceOf(EdiParseException.class)
        .hasMessage("EDIFACT payload byte array is empty");
  }
}
