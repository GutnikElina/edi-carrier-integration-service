package unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Mock
    private Smooks smooks;
    @Mock
    private ExecutionContext ctx;
    @InjectMocks
    private EdiParserService parser;

    @Test
    @DisplayName("parseIftmin: success")
    void parseIftmin_success() throws Exception {
        byte[] payload = "EDIFACT".getBytes();
        IftminInstructionDto expected = new IftminInstructionDto();
        when(smooks.createExecutionContext()).thenReturn(ctx);

        doAnswer(
                inv -> {
                    JavaSink sink = inv.getArgument(2);
                    sink.getResultMap().put("iftminDto", expected);
                    return null;
                })
            .when(smooks)
            .filterSource(any(), any(ByteSource.class), any(JavaSink.class));

        IftminInstructionDto result = parser.parseIftmin(payload);
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("parseIftmin: throws NPE on null payload")
    void parseIftmin_nullPayload() {
        assertThatThrownBy(() -> parser.parseIftmin(null))
            .isInstanceOf(EdiParseException.class)
            .hasMessageContaining(
                    "Smooks parsing executed successfully but produced null Java Bean binding");
    }

    @Test
    @DisplayName("parseIftmin: throws EdiParseException on empty payload")
    void parseIftmin_emptyPayload() {
        assertThatThrownBy(() -> parser.parseIftmin(new byte[0]))
            .isInstanceOf(EdiParseException.class)
            .hasMessage("Smooks parsing executed successfully but produced null Java Bean binding");
    }

  @Test
  @DisplayName("parseIftmin: throws EdiParseException when Smooks fails")
  void parseIftmin_smooksException() throws Exception {
    when(smooks.createExecutionContext()).thenReturn(ctx);
    doThrow(new RuntimeException("smooks error")).when(smooks).filterSource(any(), any(), any());

    assertThatThrownBy(() -> parser.parseIftmin("data".getBytes()))
        .isInstanceOf(EdiParseException.class)
        .hasMessageContaining("Unhandled error occurred");
  }

  @Test
  @DisplayName("parseIftmin: throws EdiParseException when DTO is null")
  void parseIftmin_dtoNull() throws Exception {
    when(smooks.createExecutionContext()).thenReturn(ctx);
    doAnswer(
            inv -> {
              JavaSink sink = inv.getArgument(2);
              sink.getResultMap().put("iftminDto", null);
              return null;
            })
        .when(smooks)
        .filterSource(any(), any(), any());

    assertThatThrownBy(() -> parser.parseIftmin("data".getBytes()))
        .isInstanceOf(EdiParseException.class)
        .hasMessageContaining("null Java Bean binding");
  }

  @Test
  @DisplayName("parseIftmin: rethrows EdiParseException (catch block)")
  void parseIftmin_rethrowsEdiParseException() throws Exception {
    when(smooks.createExecutionContext()).thenReturn(ctx);
    doThrow(new EdiParseException("inner")).when(smooks).filterSource(any(), any(), any());

    assertThatThrownBy(() -> parser.parseIftmin("data".getBytes()))
        .isInstanceOf(EdiParseException.class)
        .hasMessage("inner");
  }
}
