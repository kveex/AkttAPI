package org.kveex.certificate;

import org.kveex.AkttAPI;

public class CertificateHandler {
    public static void sendCertificate(CertificateItem certificateItem) {
        AkttAPI.LOGGER.info(String.valueOf(certificateItem));
    }
}
