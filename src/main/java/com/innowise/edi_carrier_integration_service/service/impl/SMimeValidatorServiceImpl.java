package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.service.SMimeValidatorService;
import com.innowise.edi_carrier_integration_service.util.SMimeValidatorUtil;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SMimeValidatorServiceImpl implements SMimeValidatorService {

    private final SMimeValidatorUtil validatorUtil;

    @Override
    public void verifyPayload(MimeBodyPart decryptedPart, String senderAlias,
            MimeMultipart multipart) {
        validatorUtil.validateMimeType(decryptedPart);
        validatorUtil.verifySignatures(senderAlias, multipart);
    }

}