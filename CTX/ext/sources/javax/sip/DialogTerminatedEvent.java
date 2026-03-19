package javax.sip;

import java.util.EventObject;

public class DialogTerminatedEvent extends EventObject {
    private Dialog mDialog;

    public DialogTerminatedEvent(Object obj, Dialog dialog) {
        super(obj);
        this.mDialog = dialog;
    }

    public Dialog getDialog() {
        return this.mDialog;
    }
}
