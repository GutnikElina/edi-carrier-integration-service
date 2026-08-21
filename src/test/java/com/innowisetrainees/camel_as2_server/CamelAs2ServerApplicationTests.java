package com.innowisetrainees.camel_as2_server;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@UseAdviceWith
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CamelAs2ServerApplicationTests {

    @Autowired
    CamelContext camelContext;

    @EndpointInject("direct:as2-test")
    ProducerTemplate producerTemplate;

    @EndpointInject("mock:result")
    MockEndpoint mockResult;

    @BeforeEach
    void setUp() throws Exception {
        AdviceWith.adviceWith(
                camelContext,
                "as2-inbound-route",
                route -> {
                    route.replaceFromWith("direct:as2-test");
                    route.weaveAddLast().to("mock:result");
                }
        );
        camelContext.start();
    }

    @Test
    void shouldReceiveEdiMessage() throws Exception {
        String edi = """
                ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *260821*1000*U*00401*000000001*0*P*>~
                GS*PO*SENDER*RECEIVER*20260821*1000*1*X*004010~
                ST*850*0001~
                SE*2*0001~
                GE*1*1~
                IEA*1*000000001~
                """;

        mockResult.expectedMessageCount(1);
        mockResult.expectedBodyReceived().body().contains("ISA*00*");

        producerTemplate.sendBody("direct:as2-test", edi);

        mockResult.assertIsSatisfied();
    }

    @Test
    void shouldProcessEmptyBody() throws Exception {
        mockResult.expectedMessageCount(1);

        producerTemplate.sendBody("direct:as2-test", "");

        mockResult.assertIsSatisfied();
    }

}
