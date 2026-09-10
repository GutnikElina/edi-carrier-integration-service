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
            int len = payload.length;
            for (int i = 0; i < len; i++) {
                byte b = payload[i];
                if (b == '\r') {
                    digest.update((byte) '\r');
                    digest.update((byte) '\n');
                    if (i + 1 < len && payload[i + 1] == '\n') {
                        i++;
                    }
                } else if (b == '\n') {
                    digest.update((byte) '\r');
                    digest.update((byte) '\n');
                } else {
                    digest.update(b);
                }
            }
            byte[] hash = digest.digest();
            return Base64.getEncoder()
                .encodeToString(hash);
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new MessageIntegrityCheckException(ex.getMessage());
        }

    }

}
