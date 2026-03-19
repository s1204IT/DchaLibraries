package org.gsma.joyn.vsh;

import org.gsma.joyn.JoynServiceException;

public class VideoSharing {
    private IVideoSharing sharingInf;

    public static class Direction {
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
    }

    public static class Encoding {
        public static final int H264 = 0;
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
        public static final int INVITATION_DECLINED = 1;
        public static final int SHARING_FAILED = 0;

        private Error() {
        }
    }

    VideoSharing(IVideoSharing sharingInf) {
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

    public String getVideoEncoding() throws JoynServiceException {
        try {
            return this.sharingInf.getVideoEncoding();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getVideoFormat() throws JoynServiceException {
        try {
            return this.sharingInf.getVideoFormat();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public VideoCodec getVideoCodec() throws JoynServiceException {
        try {
            return this.sharingInf.getVideoCodec();
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

    public void acceptInvitation(VideoRenderer renderer) throws JoynServiceException {
        try {
            this.sharingInf.acceptInvitation(renderer);
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

    public void addEventListener(VideoSharingListener listener) throws JoynServiceException {
        try {
            this.sharingInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(VideoSharingListener listener) throws JoynServiceException {
        try {
            this.sharingInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
