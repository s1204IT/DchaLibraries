package gov.nist.javax.sip;

import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.sip.SipProvider;
import javax.sip.Transaction;

public interface TransactionExt extends Transaction {
    String getCipherSuite() throws UnsupportedOperationException;

    @Override
    String getHost();

    Certificate[] getLocalCertificates() throws UnsupportedOperationException;

    @Override
    String getPeerAddress();

    Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException;

    @Override
    int getPeerPort();

    @Override
    int getPort();

    @Override
    SipProvider getSipProvider();

    @Override
    String getTransport();
}
