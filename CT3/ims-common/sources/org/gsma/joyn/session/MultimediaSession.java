package org.gsma.joyn.session;

import org.gsma.joyn.JoynServiceException;

public class MultimediaSession {
    private IMultimediaSession sessionInf;

    public static class Direction {
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
    }

    public static class State {
        public static final int ABORTED = 5;
        public static final int FAILED = 7;
        public static final int INITIATED = 2;
        public static final int INVITED = 1;
        public static final int STARTED = 3;
        public static final int TERMINATED = 6;
        public static final int UNKNOWN = 0;

        private State() {
        }
    }

    public static class Error {
        public static final int INVITATION_DECLINED = 0;
        public static final int MEDIA_FAILED = 2;
        public static final int SESSION_FAILED = 1;

        private Error() {
        }
    }

    MultimediaSession(IMultimediaSession sessionInf) {
        this.sessionInf = sessionInf;
    }

    public String getSessionId() throws JoynServiceException {
        try {
            return this.sessionInf.getSessionId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getRemoteContact() throws JoynServiceException {
        try {
            return this.sessionInf.getRemoteContact();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getServiceId() throws JoynServiceException {
        try {
            return this.sessionInf.getServiceId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getState() throws JoynServiceException {
        try {
            return this.sessionInf.getState();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getDirection() throws JoynServiceException {
        try {
            return this.sessionInf.getDirection();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void acceptInvitation() throws JoynServiceException {
        try {
            this.sessionInf.acceptInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void rejectInvitation() throws JoynServiceException {
        try {
            this.sessionInf.rejectInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void abortSession() throws JoynServiceException {
        try {
            this.sessionInf.abortSession();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(MultimediaSessionListener listener) throws JoynServiceException {
        try {
            this.sessionInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(MultimediaSessionListener listener) throws JoynServiceException {
        try {
            this.sessionInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public boolean sendMessage(byte[] content) throws JoynServiceException {
        try {
            return this.sessionInf.sendMessage(content);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
