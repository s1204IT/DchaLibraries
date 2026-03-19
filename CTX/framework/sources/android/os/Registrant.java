package android.os;

import java.lang.ref.WeakReference;

public class Registrant {
    WeakReference refH;
    Object userObj;
    int what;

    public Registrant(Handler handler, int i, Object obj) {
        this.refH = new WeakReference(handler);
        this.what = i;
        this.userObj = obj;
    }

    public void clear() {
        this.refH = null;
        this.userObj = null;
    }

    public void notifyRegistrant() {
        internalNotifyRegistrant(null, null);
    }

    public void notifyResult(Object obj) {
        internalNotifyRegistrant(obj, null);
    }

    public void notifyException(Throwable th) {
        internalNotifyRegistrant(null, th);
    }

    public void notifyRegistrant(AsyncResult asyncResult) {
        internalNotifyRegistrant(asyncResult.result, asyncResult.exception);
    }

    void internalNotifyRegistrant(Object obj, Throwable th) {
        Handler handler = getHandler();
        if (handler == null) {
            clear();
            return;
        }
        Message messageObtain = Message.obtain();
        messageObtain.what = this.what;
        messageObtain.obj = new AsyncResult(this.userObj, obj, th);
        handler.sendMessage(messageObtain);
    }

    public Message messageForRegistrant() {
        Handler handler = getHandler();
        if (handler == null) {
            clear();
            return null;
        }
        Message messageObtainMessage = handler.obtainMessage();
        messageObtainMessage.what = this.what;
        messageObtainMessage.obj = this.userObj;
        return messageObtainMessage;
    }

    public Handler getHandler() {
        if (this.refH == null) {
            return null;
        }
        return (Handler) this.refH.get();
    }
}
