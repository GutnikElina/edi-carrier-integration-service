package com.innowise.edi_carrier_integration_service;

import com.innowise.edi_carrier_integration_service.edi.infrastructure.archive.EdiArchiveService;
import com.innowise.edi_carrier_integration_service.edi.infrastructure.crypto.KeyManagementService;
import io.minio.MinioClient;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@UseAdviceWith
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CamelAs2ServerApplicationTests {

  @MockBean private KeyManagementService keyManagementService;
  @MockBean private EdiArchiveService ediArchiveService;
  @MockBean private MinioClient minioClient;

  @Autowired private CamelContext camelContext;

    @EndpointInject("direct:as2-test")
    private ProducerTemplate producerTemplate;

    @EndpointInject("mock:result")
    private MockEndpoint mockResult;

    @BeforeAll
    void setUp() throws Exception {
        AdviceWith.adviceWith(
                camelContext,
                "as2-inbound-route",
                route -> {
                    route.replaceFromWith("direct:as2-test");
                    route.weaveAddLast().to("mock:result");
                });
        camelContext.start();
    }

    @BeforeEach
    void resetMocks() {
        mockResult.reset();
    }

  @Test
  void shouldReceiveEdiMessage() throws Exception {
    final String edi =
        """
                ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *260821*1000*U*00401*000000001*0*P*>~
                GS*PO*SENDER*RECEIVER*20260821*1000*1*X*004010~
                ST*850*0001~
                SE*2*0001~
                GE*1*1~
                IEA*1*000000001~
                """;

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived(edi);

        producerTemplate.sendBody("direct:as2-test", edi);

        mockResult.assertIsSatisfied();
    }

    @Test
    void shouldProcessEmptyBody() throws Exception {
        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived("");

        producerTemplate.sendBody("direct:as2-test", "");

        mockResult.assertIsSatisfied();
    }
}
