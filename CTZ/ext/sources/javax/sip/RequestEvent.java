package javax.sip;

import java.util.EventObject;
import javax.sip.message.Request;

public class RequestEvent extends EventObject {
    private Dialog mDialog;
    private Request mRequest;
    private ServerTransaction mServerTransaction;

    public RequestEvent(Object obj, ServerTransaction serverTransaction, Dialog dialog, Request request) {
        super(obj);
        this.mDialog = dialog;
        this.mRequest = request;
        this.mServerTransaction = serverTransaction;
    }

    public Dialog getDialog() {
        return this.mDialog;
    }

    public Request getRequest() {
        return this.mRequest;
    }

    public ServerTransaction getServerTransaction() {
        return this.mServerTransaction;
    }
}
