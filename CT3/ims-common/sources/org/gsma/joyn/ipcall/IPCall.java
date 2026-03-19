package org.gsma.joyn.ipcall;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.Logger;

public class IPCall {
    public static final String TAG = "IPCall";
    private IIPCall callInf;

    public static class Direction {
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
    }

    public static class State {
        public static final int ABORTED = 5;
        public static final int FAILED = 7;
        public static final int HOLD = 8;
        public static final int INITIATED = 2;
        public static final int INVITED = 1;
        public static final int STARTED = 3;
        public static final int TERMINATED = 6;
        public static final int UNKNOWN = 0;

        private State() {
        }
    }

    public static class Error {
        public static final int CALL_FAILED = 0;
        public static final int INVITATION_DECLINED = 1;

        private Error() {
        }
    }

    IPCall(IIPCall callInf) {
        this.callInf = callInf;
    }

    public String getCallId() throws JoynServiceException {
        Logger.i(TAG, "getCallId entry");
        try {
            String callId = this.callInf.getCallId();
            Logger.i(TAG, "getCallId entry" + callId);
            return callId;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getRemoteContact() throws JoynServiceException {
        Logger.i(TAG, "getRemoteContact entry");
        try {
            String contact = this.callInf.getRemoteContact();
            Logger.i(TAG, "getRemoteContact value " + contact);
            return contact;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getState() throws JoynServiceException {
        Logger.i(TAG, "getState entry");
        try {
            int State2 = this.callInf.getState();
            Logger.i(TAG, "getState value " + State2);
            return State2;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getDirection() throws JoynServiceException {
        Logger.i(TAG, "getDirection entry");
        try {
            int direction = this.callInf.getDirection();
            Logger.i(TAG, "getDirection value" + direction);
            return direction;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void acceptInvitation(IPCallPlayer player, IPCallRenderer renderer) throws JoynServiceException {
        try {
            Logger.i(TAG, "acceptInvitation entry");
            this.callInf.acceptInvitation(player, renderer);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void rejectInvitation() throws JoynServiceException {
        try {
            Logger.i(TAG, "rejectInvitation entry");
            this.callInf.rejectInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void abortCall() throws JoynServiceException {
        try {
            Logger.i(TAG, "abortCall entry");
            this.callInf.abortCall();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public boolean isVideo() throws JoynServiceException {
        try {
            boolean isVideo = this.callInf.isVideo();
            Logger.i(TAG, "abortCall value" + isVideo);
            return isVideo;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addVideo() throws JoynServiceException {
        try {
            Logger.i(TAG, "addVideo entry");
            this.callInf.addVideo();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeVideo() throws JoynServiceException {
        try {
            Logger.i(TAG, "removeVideo entry");
            this.callInf.removeVideo();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public boolean isOnHold() throws JoynServiceException {
        try {
            boolean isonHold = this.callInf.isOnHold();
            Logger.i(TAG, "isOnHold value" + isonHold);
            return isonHold;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void holdCall() throws JoynServiceException {
        try {
            Logger.i(TAG, "holdCall entry");
            this.callInf.holdCall();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void continueCall() throws JoynServiceException {
        try {
            Logger.i(TAG, "continueCall entry");
            this.callInf.continueCall();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(IPCallListener listener) throws JoynServiceException {
        try {
            Logger.i(TAG, "addEventListener entry" + listener);
            this.callInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(IPCallListener listener) throws JoynServiceException {
        try {
            Logger.i(TAG, "removeEventListener entry" + listener);
            this.callInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
