package com.android.mediadrm.signer;

import android.media.DeniedByServerException;
import android.media.MediaDrm;

public final class MediaDrmSigner {
    public static final int CERTIFICATE_TYPE_X509 = 1;

    private MediaDrmSigner() {
    }

    public static final class CertificateRequest {
        private final MediaDrm.CertificateRequest mCertRequest;

        CertificateRequest(MediaDrm.CertificateRequest certificateRequest) {
            this.mCertRequest = certificateRequest;
        }

        public byte[] getData() {
            return this.mCertRequest.getData();
        }

        public String getDefaultUrl() {
            return this.mCertRequest.getDefaultUrl();
        }
    }

    public static final class Certificate {
        private final MediaDrm.Certificate mCertificate;

        Certificate(MediaDrm.Certificate certificate) {
            this.mCertificate = certificate;
        }

        public byte[] getWrappedPrivateKey() {
            return this.mCertificate.getWrappedPrivateKey();
        }

        public byte[] getContent() {
            return this.mCertificate.getContent();
        }
    }

    public static CertificateRequest getCertificateRequest(MediaDrm mediaDrm, int i, String str) {
        return new CertificateRequest(mediaDrm.getCertificateRequest(i, str));
    }

    public static Certificate provideCertificateResponse(MediaDrm mediaDrm, byte[] bArr) throws DeniedByServerException {
        return new Certificate(mediaDrm.provideCertificateResponse(bArr));
    }

    public static byte[] signRSA(MediaDrm mediaDrm, byte[] bArr, String str, byte[] bArr2, byte[] bArr3) {
        return mediaDrm.signRSA(bArr, str, bArr2, bArr3);
    }
}
