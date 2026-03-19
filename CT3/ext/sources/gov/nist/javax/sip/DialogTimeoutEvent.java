package gov.nist.javax.sip;

import java.util.EventObject;
import javax.sip.Dialog;

public class DialogTimeoutEvent extends EventObject {
    private static final long serialVersionUID = -2514000059989311925L;
    private Dialog m_dialog;
    private Reason m_reason;

    public enum Reason {
        AckNotReceived,
        AckNotSent,
        ReInviteTimeout;

        public static Reason[] valuesCustom() {
            return values();
        }
    }

    public DialogTimeoutEvent(Object source, Dialog dialog, Reason reason) {
        super(source);
        this.m_dialog = null;
        this.m_reason = null;
        this.m_dialog = dialog;
        this.m_reason = reason;
    }

    public Dialog getDialog() {
        return this.m_dialog;
    }

    public Reason getReason() {
        return this.m_reason;
    }
}
