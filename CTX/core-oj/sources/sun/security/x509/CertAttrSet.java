package sun.security.x509;

import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.Enumeration;

public interface CertAttrSet<T> {
    void delete(String str) throws IOException, CertificateException;

    void encode(OutputStream outputStream) throws IOException, CertificateException;

    Object get(String str) throws IOException, CertificateException;

    Enumeration<T> getElements();

    String getName();

    void set(String str, Object obj) throws IOException, CertificateException;

    String toString();
}
