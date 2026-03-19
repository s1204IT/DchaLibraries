package gov.nist.javax.sip;

import java.io.IOException;
import javax.sip.ListeningPoint;
import javax.sip.header.ContactHeader;
import javax.sip.header.ViaHeader;

public interface ListeningPointExt extends ListeningPoint {
    @Override
    ContactHeader createContactHeader();

    ViaHeader createViaHeader();

    @Override
    void sendHeartbeat(String str, int i) throws IOException;
}
