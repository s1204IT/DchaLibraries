package org.gsma.joyn.chat;

import java.util.List;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.Logger;

public class ExtendChat {
    public static final String TAG = "TAPI-Chat";
    protected IExtendChat chatInf;

    public static class ErrorCodes {
        public static final int INTERNAL = 3;
        public static final int OUTOFSIZE = 4;
        public static final int TIMEOUT = 1;
        public static final int UNKNOWN = 2;
    }

    ExtendChat(IExtendChat chatIntf) {
        this.chatInf = chatIntf;
    }

    public List<String> getRemoteContacts() throws JoynServiceException {
        Logger.i("TAPI-Chat", "getRemoteContact entry");
        try {
            return this.chatInf.getRemoteContacts();
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String sendMessage(String message) throws JoynServiceException {
        return sendMessage(message, 0);
    }

    public String sendMessage(String message, int msgType) throws JoynServiceException {
        Logger.i("TAPI-Chat", "ABC sendMessage entry " + message + " with Type " + msgType);
        try {
            return this.chatInf.sendMessage(message, msgType);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void sendBurnedDeliveryReport(String msgId) throws JoynServiceException {
        Logger.i("TAPI-Chat", "sendDeliveryReport entry " + msgId);
        try {
            this.chatInf.sendBurnedDeliveryReport(msgId);
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

    public int resendMessage(String msgId) throws JoynServiceException {
        Logger.i("TAPI-Chat", "resendMessage msgId " + msgId);
        try {
            return this.chatInf.resendMessage(msgId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public String prosecuteMessage(String msgId) throws JoynServiceException {
        Logger.i("TAPI-Chat", "prosecute message of msgId " + msgId);
        try {
            return this.chatInf.prosecuteMessage(msgId);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void addEventListener(ExtendChatListener listener) throws JoynServiceException {
        Logger.i("TAPI-Chat", "addEventListener entry " + listener);
        try {
            this.chatInf.addEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public void removeEventListener(ExtendChatListener listener) throws JoynServiceException {
        Logger.i("TAPI-Chat", "removeEventListener entry " + listener);
        try {
            this.chatInf.removeEventListener(listener);
        } catch (Exception e) {
            throw new JoynServiceException(e.getMessage());
        }
    }

    public boolean isAlive() {
        return this.chatInf.asBinder().isBinderAlive();
    }
}
