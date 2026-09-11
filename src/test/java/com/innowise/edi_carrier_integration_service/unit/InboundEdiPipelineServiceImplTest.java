package com.innowise.edi_carrier_integration_service.unit;

import com.innowise.edi_carrier_integration_service.dto.IftminInstructionDto;
import com.innowise.edi_carrier_integration_service.exception.PayloadTooLargeException;
import com.innowise.edi_carrier_integration_service.service.As2MdnGeneratorService;
import com.innowise.edi_carrier_integration_service.service.EdiArchiveService;
import com.innowise.edi_carrier_integration_service.service.EdiParserService;
import com.innowise.edi_carrier_integration_service.service.SMimeDecryptionService;
import com.innowise.edi_carrier_integration_service.service.impl.InboundEdiPipelineServiceImpl;
import org.apache.camel.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundEdiPipelineServiceImplTest {

    @Mock
    private SMimeDecryptionService sMimeDecryptionService;
    @Mock
    private EdiParserService ediParserService;
    @Mock
    private EdiArchiveService ediArchiveService;
    @Mock
    private As2MdnGeneratorService as2MdnGeneratorService;
    @Mock
    private Message camelMessage;

    @InjectMocks
    private InboundEdiPipelineServiceImpl pipeline;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pipeline, "maxAllowedPayloadBytes", 1024L);
    }

    @Test
    @DisplayName("processInboundSmimeMessage: success")
    void processInboundSmimeMessage_success() {
        byte[] rawPayload = "smime-data".getBytes();
        byte[] decryptedPayload = "edifact-data".getBytes();
        IftminInstructionDto expectedDto = new IftminInstructionDto(null, null, null, null, null,
                null, null);

        when(camelMessage.getHeader("Message-ID", String.class)).thenReturn("msg-123");
        when(camelMessage.getHeader("AS2-From", String.class)).thenReturn("SENDER_AS2");
        when(camelMessage.getHeader("AS2-To", String.class)).thenReturn("RECEIVER_AS2");
        when(camelMessage.getHeader("Receipt-Delivery-Option", String.class))
            .thenReturn("http://async.mdn");
        when(camelMessage.getBody(byte[].class)).thenReturn(rawPayload);

        when(ediArchiveService.saveRawPayload(anyString(), eq(rawPayload),
                eq("application/pkcs7-mime")))
            .thenReturn("raw/123.smime");
        when(sMimeDecryptionService.decrypt(rawPayload, "RECIPIENT_ALIAS", "SENDER_ALIAS")) // Hardcoded
                                                                                            // aliases
                                                                                            // from
                                                                                            // TODO
            .thenReturn(decryptedPayload);
        when(as2MdnGeneratorService.generateMdn(decryptedPayload, "msg-123", "SENDER_AS2",
                "RECEIVER_AS2"))
            .thenReturn("generated-mdn");
        when(ediParserService.parseIftmin(decryptedPayload)).thenReturn(expectedDto);

        pipeline.processInboundSmimeMessage(camelMessage);

        verify(ediArchiveService).saveRawPayload(anyString(), eq(rawPayload),
                eq("application/pkcs7-mime"));
        verify(ediParserService).parseIftmin(decryptedPayload);
    }

    @Test
    @DisplayName("processInboundSmimeMessage: throws PayloadTooLargeException")
    void processInboundSmimeMessage_tooLarge() {
        byte[] rawPayload = new byte[2048]; // More than 1024 max

        when(camelMessage.getHeader("Message-ID", String.class)).thenReturn("msg-123");
        when(camelMessage.getHeader("AS2-From", String.class)).thenReturn("SENDER_AS2");
        when(camelMessage.getHeader("AS2-To", String.class)).thenReturn("RECEIVER_AS2");
        when(camelMessage.getBody(byte[].class)).thenReturn(rawPayload);

        assertThatThrownBy(() -> pipeline.processInboundSmimeMessage(camelMessage))
            .isInstanceOf(PayloadTooLargeException.class)
            .hasMessageContaining("exceeds maximum allowed limit");
    }

    @Test
    @DisplayName("processInboundSmimeMessage: NPE on null required fields")
    void processInboundSmimeMessage_nullFields() {
        when(camelMessage.getBody(byte[].class)).thenReturn(null);

        assertThatThrownBy(() -> pipeline.processInboundSmimeMessage(camelMessage))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Raw S/MIME bytes array must not be null");
    }
}
