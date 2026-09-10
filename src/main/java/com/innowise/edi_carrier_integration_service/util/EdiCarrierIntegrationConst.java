package com.innowise.edi_carrier_integration_service.util;

public final class EdiCarrierIntegrationConst {

    public static final String MDN_TEMPLATE = """
            AS2-Version: 1.2
            From: %s
            To: %s
            Message-ID: %s
            Date: %s
            Content-Type: multipart/report; report-type=disposition-notification; boundary="%s"

            --%s
            Content-Type: text/plain; charset=us-ascii

            The EDIFACT IFTMIN message sent to %s with Message ID %s has been received and processed successfully.

            --%s
            Content-Type: message/disposition-notification

            Reporting-UA: Innowise-EDI-Carrier-Integration-Service
            Original-Recipient: rfc822; %s
            Final-Recipient: rfc822; %s
            Original-Message-ID: %s
            Disposition: automatic-action/MDN-sent-automatically; processed
            Received-Content-MIC: %s, sha-256

            --%s--
            """;

    private EdiCarrierIntegrationConst() {
    }
}
