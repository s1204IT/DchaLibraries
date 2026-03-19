package org.gsma.joyn.ish;

import org.gsma.joyn.JoynServiceException;

public class ImageSharing {
    private IImageSharing sharingInf;

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
        public static final int SAVING_FAILED = 2;
        public static final int SHARING_FAILED = 0;

        private Error() {
        }
    }

    ImageSharing(IImageSharing sharingInf) {
        this.sharingInf = sharingInf;
    }

    public String getSharingId() throws JoynServiceException {
        try {
            return this.sharingInf.getSharingId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getRemoteContact() throws JoynServiceException {
        try {
            return this.sharingInf.getRemoteContact();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getFileName() throws JoynServiceException {
        try {
            return this.sharingInf.getFileName();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public long getFileSize() throws JoynServiceException {
        try {
            return this.sharingInf.getFileSize();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getFileType() throws JoynServiceException {
        try {
            return this.sharingInf.getFileType();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getState() throws JoynServiceException {
        try {
            return this.sharingInf.getState();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getDirection() throws JoynServiceException {
        try {
            return this.sharingInf.getDirection();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void acceptInvitation() throws JoynServiceException {
        try {
            this.sharingInf.acceptInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void rejectInvitation() throws JoynServiceException {
        try {
            this.sharingInf.rejectInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void abortSharing() throws JoynServiceException {
        try {
            this.sharingInf.abortSharing();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(ImageSharingListener listener) throws JoynServiceException {
        try {
            this.sharingInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(ImageSharingListener listener) throws JoynServiceException {
        try {
            this.sharingInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
