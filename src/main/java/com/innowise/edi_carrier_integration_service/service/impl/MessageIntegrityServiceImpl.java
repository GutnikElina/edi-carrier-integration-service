package com.innowise.edi_carrier_integration_service.service.impl;

import com.innowise.edi_carrier_integration_service.exception.MessageIntegrityCheckException;
import com.innowise.edi_carrier_integration_service.service.MessageIntegrityService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;

@Service
public class MessageIntegrityServiceImpl implements MessageIntegrityService {

    @Override
    public String computeMessageIntegrityCheck(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256",
                    BouncyCastleProvider.PROVIDER_NAME);

            byte previouseByte = -1;

            for (byte b : payload) {
                if (b == '\r') {
                    digest.update((byte) '\r');
                    digest.update((byte) '\n');
                } else if (b == '\n') {
                    if (previouseByte != '\r') {
                        digest.update((byte) '\r');
                        digest.update((byte) '\n');
                    }
                } else {
                    digest.update(b);
                }
                previouseByte = b;
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new MessageIntegrityCheckException(ex.getMessage());
        }
    }
}