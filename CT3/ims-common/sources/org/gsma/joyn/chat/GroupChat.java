package org.gsma.joyn.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.Logger;
import org.gsma.joyn.ft.FileTransfer;
import org.gsma.joyn.ft.FileTransferListener;
import org.gsma.joyn.ft.IFileTransfer;

public class GroupChat {
    public static final String TAG = "TAPI-GroupChat";
    private IGroupChat chatInf;

    public static class ConfState {
        public static final String FULL = "full";
        public static final String PARTIAL = "partial";
    }

    public static class Direction {
        public static final int INCOMING = 0;
        public static final int OUTGOING = 1;
    }

    public static class ErrorCodes {
        public static final int INTERNAL_EROR = 3;
        public static final int OUT_OF_SIZE = 4;
        public static final int TIMEOUT = 1;
        public static final int UNKNOWN = 2;
    }

    public static class MessageState {
        public static final int DELIVERED = 2;
        public static final int FAILED = 3;
        public static final int SENDING = 0;
        public static final int SENT = 1;
    }

    public static class ParticipantStatus {
        public static final int FAIL = 1;
        public static final int SUCCESS = 0;
    }

    public static class ReasonCode {
        public static final int INTERNAL_ERROR = 3;
        public static final int SUCCESSFUL = 1;
        public static final int TIME_OUT = 4;
        public static final int UNKNOWN = 2;
    }

    public static class State {
        public static final int ABORTED = 5;
        public static final int CLOSED_BY_USER = 6;
        public static final int FAILED = 7;
        public static final int INITIATED = 2;
        public static final int INVITED = 1;
        public static final int STARTED = 3;
        public static final int TERMINATED = 4;
        public static final int UNKNOWN = 0;

        private State() {
        }
    }

    public static class Error {
        public static final int CHAT_FAILED = 0;
        public static final int CHAT_NOT_FOUND = 2;
        public static final int INVITATION_DECLINED = 1;
        public static final int INVITATION_FORBIDDEN = 3;

        private Error() {
        }
    }

    GroupChat(IGroupChat chatIntf) {
        this.chatInf = chatIntf;
    }

    public String getChatId() throws JoynServiceException {
        Logger.i(TAG, "getChatId entry " + this.chatInf);
        try {
            return this.chatInf.getChatId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getChatSessionId() throws JoynServiceException {
        Logger.i(TAG, "getChatSessionId entry " + this.chatInf);
        try {
            return this.chatInf.getChatSessionId();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getDirection() throws JoynServiceException {
        Logger.i(TAG, "getDirection entry " + this.chatInf);
        try {
            return this.chatInf.getDirection();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getState() throws JoynServiceException {
        Logger.i(TAG, "getState() entry " + this.chatInf);
        try {
            return this.chatInf.getState();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getMessageState(String messageId) throws JoynServiceException {
        Logger.i(TAG, "getState() entry " + this.chatInf + "Message Id = " + messageId);
        try {
            return this.chatInf.getMessageState(messageId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getRemoteContact() throws JoynServiceException {
        Logger.i(TAG, "getRemoteContact() entry " + this.chatInf);
        try {
            return this.chatInf.getRemoteContact();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String getSubject() throws JoynServiceException {
        Logger.i(TAG, "getSubject() entry " + this.chatInf);
        try {
            return this.chatInf.getSubject();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public Set<String> getParticipants() throws JoynServiceException {
        Logger.i(TAG, "getParticipants() entry " + this.chatInf);
        try {
            return new HashSet(this.chatInf.getParticipants());
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public Set<String> getAllParticipants() throws JoynServiceException {
        Logger.i(TAG, "getAllParticipants() entry " + this.chatInf);
        try {
            return new HashSet(this.chatInf.getAllParticipants());
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void acceptInvitation() throws JoynServiceException {
        Logger.i(TAG, "acceptInvitation() entry " + this.chatInf);
        try {
            this.chatInf.acceptInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void rejectInvitation() throws JoynServiceException {
        Logger.i(TAG, "rejectInvitation() entry " + this.chatInf);
        try {
            this.chatInf.rejectInvitation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String sendMessage(String text) throws JoynServiceException {
        Logger.i(TAG, "sendMessage() entry " + text);
        try {
            return this.chatInf.sendMessage(text);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String sendMessage(String text, int msgType) throws JoynServiceException {
        Logger.i(TAG, "sendMessage() entry " + text);
        try {
            return this.chatInf.sendMessageEx(text, msgType);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String sendGeoloc(Geoloc geoloc) throws JoynServiceException {
        Logger.i(TAG, "sendGeoloc() entry " + geoloc);
        try {
            return this.chatInf.sendGeoloc(geoloc);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public FileTransfer sendFile(String filename, String fileicon, FileTransferListener listener) throws JoynServiceException {
        Logger.i(TAG, "sendFile() entry filename=" + filename + " fileicon=" + fileicon + " listener =" + listener);
        try {
            IFileTransfer ftIntf = this.chatInf.sendFile(filename, fileicon, listener);
            if (ftIntf != null) {
                return new FileTransfer(ftIntf);
            }
            return null;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void sendIsComposingEvent(boolean status) throws JoynServiceException {
        Logger.i(TAG, "sendIsComposingEvent() entry " + status);
        try {
            this.chatInf.sendIsComposingEvent(status);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int resendMessage(String msgId) throws JoynServiceException {
        Logger.i(TAG, "resendMessage msgId " + msgId);
        try {
            return this.chatInf.resendMessage(msgId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void sendDisplayedDeliveryReport(String msgId) throws JoynServiceException {
        Logger.i(TAG, "sendDisplayedDeliveryReport() entry " + msgId);
        try {
            this.chatInf.sendDisplayedDeliveryReport(msgId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addParticipants(Set<String> participants) throws JoynServiceException {
        Logger.i(TAG, "addParticipants() entry " + participants);
        try {
            this.chatInf.addParticipants(new ArrayList(participants));
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int getMaxParticipants() throws JoynServiceException {
        Logger.i(TAG, "getMaxParticipants() entry ");
        try {
            return this.chatInf.getMaxParticipants();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void quitConversation() throws JoynServiceException {
        Logger.i(TAG, "quitConversation() entry ");
        try {
            this.chatInf.quitConversation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(GroupChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "addEventListener() entry " + listener);
        try {
            this.chatInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(GroupChatListener listener) throws JoynServiceException {
        Logger.i(TAG, "removeEventListener() entry " + listener);
        try {
            this.chatInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void transferChairman(String newChairman) throws JoynServiceException {
        Logger.i(TAG, "transferChairman() entry " + newChairman);
        try {
            this.chatInf.transferChairman(newChairman);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void modifySubject(String newSubject) throws JoynServiceException {
        Logger.i(TAG, "modifySubject() entry " + newSubject);
        try {
            this.chatInf.modifySubject(newSubject);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void modifyMyNickName(String newNickname) throws JoynServiceException {
        Logger.i(TAG, "modifyMyNickName() entry " + newNickname);
        try {
            this.chatInf.modifyMyNickName(newNickname);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeParticipants(List<String> participants) throws JoynServiceException {
        Logger.i(TAG, "removeParticipants() entry " + participants);
        try {
            this.chatInf.removeParticipants(participants);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void abortConversation() throws JoynServiceException {
        Logger.i(TAG, "abortConversation() entry ");
        try {
            this.chatInf.abortConversation();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public boolean isMeChairman() throws JoynServiceException {
        Logger.i(TAG, "isMeChairman() entry ");
        try {
            boolean flag = this.chatInf.isMeChairman();
            return flag;
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
