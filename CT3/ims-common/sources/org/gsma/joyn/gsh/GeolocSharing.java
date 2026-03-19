package org.gsma.joyn.gsh;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.Logger;
import org.gsma.joyn.chat.Geoloc;

public class GeolocSharing {
    public static final String TAG = "TAPI-GeolocSharing";
    private IGeolocSharing sharingInf;

    public static class Direction {
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
    }

    public static class State {
        public static final int ABORTED = 5;
        public static final int FAILED = 6;
        public static final int INITIATED = 2;
        public static final int INVITED = 1;
        public static final int STARTED = 3;
        public static final int TRANSFERRED = 4;
        public static final int UNKNOWN = 0;

        private State() {
        }
    }

    public static class Error {
        public static final int INVITATION_DECLINED = 1;
        public static final int SHARING_FAILED = 0;

        private Error() {
        }
    }

    GeolocSharing(IGeolocSharing sharingInf) {
        this.sharingInf = sharingInf;
    }

    public String getSharingId() throws JoynServiceException {
        Logger.i(TAG, "getSharingId() entry ");
        try {
            return this.sharingInf.getSharingId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getRemoteContact() throws JoynServiceException {
        Logger.i(TAG, "getRemoteContact() entry ");
        try {
            return this.sharingInf.getRemoteContact();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public Geoloc getGeoloc() throws JoynServiceException {
        Logger.i(TAG, "getGeoloc() entry ");
        try {
            return this.sharingInf.getGeoloc();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getState() throws JoynServiceException {
        Logger.i(TAG, "getState() entry ");
        try {
            return this.sharingInf.getState();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getDirection() throws JoynServiceException {
        Logger.i(TAG, "getDirection() entry ");
        try {
            return this.sharingInf.getDirection();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void acceptInvitation() throws JoynServiceException {
        Logger.i(TAG, "acceptInvitation() entry ");
        try {
            this.sharingInf.acceptInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void rejectInvitation() throws JoynServiceException {
        Logger.i(TAG, "rejectInvitation() entry ");
        try {
            this.sharingInf.rejectInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void abortSharing() throws JoynServiceException {
        Logger.i(TAG, "abortSharing() entry ");
        try {
            this.sharingInf.abortSharing();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(GeolocSharingListener listener) throws JoynServiceException {
        Logger.i(TAG, "addEventListener() entry " + listener);
        try {
            this.sharingInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(GeolocSharingListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeEventListener() entry " + listener);
        try {
            this.sharingInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
