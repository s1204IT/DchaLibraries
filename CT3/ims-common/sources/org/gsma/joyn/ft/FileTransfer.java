package org.gsma.joyn.ft;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.Logger;

public class FileTransfer {
    public static final String TAG = "TAPI-FileTransfer";
    private IFileTransfer transferInf;

    public static class Direction {
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
    }

    public static class Type {
        public static final String BURNED = "burned";
        public static final String NORMAL = "normal";
        public static final String PROSECUTE = "prosecute";
        public static final String PUBACCOUNT = "pubaccount";
    }

    public static class State {
        public static final int ABORTED = 5;
        public static final int DELIVERED = 7;
        public static final int DISPLAYED = 8;
        public static final int FAILED = 6;
        public static final int INITIATED = 2;
        public static final int INVITED = 1;
        public static final int PAUSED = 8;
        public static final int STARTED = 3;
        public static final int TRANSFERRED = 4;
        public static final int UNKNOWN = 0;

        private State() {
        }
    }

    public static class Error {
        public static final int INVITATION_DECLINED = 1;
        public static final int INVITATION_FAILED = 5;
        public static final int SAVING_FAILED = 2;
        public static final int TRANSFER_FAILED = 0;
        public static final int TRANSFER_FALLBACK_MMS = 4;
        public static final int TRANSFER_RESUME = 3;

        private Error() {
        }
    }

    public FileTransfer(IFileTransfer transferIntf) {
        this.transferInf = transferIntf;
    }

    public boolean isHttpFileTransfer() throws JoynServiceException {
        Logger.i(TAG, "isHttpFileTransfer ");
        try {
            return this.transferInf.isHttpFileTransfer();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getTransferId() throws JoynServiceException {
        Logger.i(TAG, "getTransferId() entry ");
        try {
            return this.transferInf.getTransferId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getTransferDuration() throws JoynServiceException {
        Logger.i(TAG, "getTransferDuration() entry ");
        try {
            return this.transferInf.getTransferDuration();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getRemoteContact() throws JoynServiceException {
        Logger.i(TAG, "getRemoteContact() entry ");
        try {
            return this.transferInf.getRemoteContact();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getFileName() throws JoynServiceException {
        Logger.i(TAG, "getFileName() entry ");
        try {
            return this.transferInf.getFileName();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public long getFileSize() throws JoynServiceException {
        Logger.i(TAG, "getFileSize() entry ");
        try {
            return this.transferInf.getFileSize();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getFileType() throws JoynServiceException {
        Logger.i(TAG, "getFileType() entry ");
        try {
            return this.transferInf.getFileType();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getFileIconName() throws JoynServiceException {
        Logger.i(TAG, "getFileIconName() entry ");
        try {
            return this.transferInf.getFileIconName();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getState() throws JoynServiceException {
        Logger.i(TAG, "getState() entry ");
        try {
            return this.transferInf.getState();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getDirection() throws JoynServiceException {
        Logger.i(TAG, "getDirection() entry ");
        try {
            return this.transferInf.getDirection();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getTransferType() throws JoynServiceException {
        Logger.i(TAG, "getTransferType() entry ");
        try {
            return this.transferInf.getTransferType();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public boolean isTransferFromSecondaryDevice() throws JoynServiceException {
        Logger.i(TAG, "isTransferFromSecondaryDevice() entry ");
        try {
            return this.transferInf.isTransferFromSecondaryDevice();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void acceptInvitation() throws JoynServiceException {
        Logger.i(TAG, "acceptInvitation() entry ");
        try {
            this.transferInf.acceptInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void rejectInvitation() throws JoynServiceException {
        Logger.i(TAG, "rejectInvitation() entry ");
        try {
            this.transferInf.rejectInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void abortTransfer() throws JoynServiceException {
        Logger.i(TAG, "abortTransfer() entry ");
        try {
            this.transferInf.abortTransfer();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void pauseTransfer() throws JoynServiceException {
        Logger.i(TAG, "pauseTransfer() entry ");
        try {
            this.transferInf.pauseTransfer();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void resumeTransfer() throws JoynServiceException {
        Logger.i(TAG, "resumeTransfer() entry ");
        try {
            this.transferInf.resumeTransfer();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "addEventListener() entry " + listener);
        try {
            this.transferInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeEventListener() entry " + listener);
        try {
            this.transferInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
