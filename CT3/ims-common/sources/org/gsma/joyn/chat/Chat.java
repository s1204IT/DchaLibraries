package org.gsma.joyn.chat;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.Logger;

public class Chat {
    public static final String TAG = "TAPI-Chat";
    protected IChat chatInf;

    public static class MessageState {
        public static final int DELIVERED = 2;
        public static final int FAILED = 3;
        public static final int SENDING = 0;
        public static final int SENT = 1;
    }

    Chat(IChat chatIntf) {
        this.chatInf = chatIntf;
    }

    public String getRemoteContact() throws JoynServiceException {
        Logger.i("TAPI-Chat", "getRemoteContact entry");
        try {
            return this.chatInf.getRemoteContact();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String sendMessage(String message) throws JoynServiceException {
        Logger.i("TAPI-Chat", "ABC sendMessage entry " + message);
        try {
            return this.chatInf.sendMessage(message);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String sendGeoloc(Geoloc geoloc) throws JoynServiceException {
        Logger.i("TAPI-Chat", "sendGeoloc entry " + geoloc);
        try {
            return this.chatInf.sendGeoloc(geoloc);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void sendDisplayedDeliveryReport(String msgId) throws JoynServiceException {
        Logger.i("TAPI-Chat", "sendDisplayedDeliveryReport entry " + msgId);
        try {
            this.chatInf.sendDisplayedDeliveryReport(msgId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void sendIsComposingEvent(boolean status) throws JoynServiceException {
        Logger.i("TAPI-Chat", "sendIsComposingEvent entry " + status);
        try {
            this.chatInf.sendIsComposingEvent(status);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public int resendMessage(String msgId) throws JoynServiceException {
        Logger.i("TAPI-Chat", "resendMessage msgId " + msgId);
        try {
            return this.chatInf.resendMessage(msgId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(ChatListener listener) throws JoynServiceException {
        Logger.i("TAPI-Chat", "addEventListener entry " + listener);
        try {
            this.chatInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(ChatListener listener) throws JoynServiceException {
        Logger.i("TAPI-Chat", "removeEventListener entry " + listener);
        try {
            this.chatInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }
}
