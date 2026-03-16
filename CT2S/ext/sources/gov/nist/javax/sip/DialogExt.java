package gov.nist.javax.sip;

import javax.sip.Dialog;
import javax.sip.SipProvider;

public interface DialogExt extends Dialog {
    void disableSequenceNumberValidation();

    @Override
    SipProvider getSipProvider();

    @Override
    void setBackToBackUserAgent();
}
