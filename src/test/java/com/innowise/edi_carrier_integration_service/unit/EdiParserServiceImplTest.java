package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.dto.IftminInstructionDto;
import com.innowise.edi_carrier_integration_service.exception.EdiParseException;
import com.innowise.edi_carrier_integration_service.service.impl.EdiParserServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdiParserServiceImplTest {

    @Mock
    private Smooks smooks;
    @Mock
    private ExecutionContext ctx;
    @Mock
    private JavaSink javaSink;

    @InjectMocks
    private EdiParserServiceImpl parser;

    @Test
    @DisplayName("parseIftmin: success")
    void parseIftmin_success() {
        byte[] payload = new byte[0];
        IftminInstructionDto expected = new IftminInstructionDto(null, null,
                null, null,
                null, null,
                null);

        when(javaSink.getBean("iftminDto")).thenReturn(expected);

        IftminInstructionDto result = parser.parseIftmin(payload);

        verify(smooks).filterSource(eq(ctx), any(ByteSource.class), eq(javaSink));
        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("parseIftmin: throws EdiParseException on payload length > 0")
    void parseIftmin_notEmptyPayload() {
        assertThatThrownBy(() -> parser.parseIftmin("EDIFACT".getBytes()))
            .isInstanceOf(EdiParseException.class)
            .hasMessage("Payload is empty");
    }

    @Test
    @DisplayName("parseIftmin: throws EdiParseException when DTO is null")
    void parseIftmin_dtoNull() {
        byte[] payload = new byte[0];
        when(javaSink.getBean("iftminDto")).thenReturn(null);

        assertThatThrownBy(() -> parser.parseIftmin(payload))
            .isInstanceOf(EdiParseException.class)
            .hasMessageContaining("produced null Java Bean binding");
    }
}
