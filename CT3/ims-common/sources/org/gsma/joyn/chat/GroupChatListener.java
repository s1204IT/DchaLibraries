package org.gsma.joyn.chat;

import java.util.List;
import org.gsma.joyn.chat.ConferenceEventData;
import org.gsma.joyn.chat.IGroupChatListener;

public abstract class GroupChatListener extends IGroupChatListener.Stub {
    @Override
    public abstract void onComposingEvent(String str, boolean z);

    @Override
    public abstract void onNewGeoloc(GeolocMessage geolocMessage);

    @Override
    public abstract void onNewMessage(ChatMessage chatMessage);

    @Override
    public abstract void onParticipantDisconnected(String str);

    @Override
    public abstract void onParticipantJoined(String str, String str2);

    @Override
    public abstract void onParticipantLeft(String str);

    @Override
    public abstract void onReportMessageDelivered(String str);

    @Override
    public abstract void onReportMessageDisplayed(String str);

    @Override
    public abstract void onReportMessageFailed(String str);

    @Override
    public abstract void onSessionAborted();

    @Override
    public abstract void onSessionError(int i);

    @Override
    public abstract void onSessionStarted();

    @Override
    public void onNewExtendMessage(ExtendMessage message) {
    }

    @Override
    public void onSessionAbortedbyChairman() {
    }

    @Override
    public void onReportMessageDeliveredContact(String msgId, String contact) {
    }

    @Override
    public void onReportMessageDisplayedContact(String msgId, String contact) {
    }

    @Override
    public void onReportMessageFailedContact(String msgId, String contact) {
    }

    @Override
    public void onReportFailedMessage(String msgId, int errtype, String statusCode) {
    }

    @Override
    public void onReportSentMessage(String msgId) {
    }

    @Override
    public void onGroupChatDissolved() {
    }

    @Override
    public void onInviteParticipantsResult(int errType, String statusCode) {
    }

    @Override
    public void onSetChairmanResult(int errType, int statusCode) {
    }

    @Override
    public void onChairmanChanged(String newChairman) {
    }

    @Override
    public void onModifySubjectResult(int errType, int statusCode) {
    }

    @Override
    public void onSubjectChanged(String newSubject) {
    }

    @Override
    public void onModifyNickNameResult(int errType, int statusCode) {
    }

    @Override
    public void onNickNameChanged(String contact, String newNickName) {
    }

    @Override
    public void onRemoveParticipantResult(int errType, int statusCode, String participant) {
    }

    @Override
    public void onReportMeKickedOut(String from) {
    }

    @Override
    public void onReportParticipantKickedOut(String contact) {
    }

    @Override
    public void onAbortConversationResult(int errType, int statusCode) {
    }

    @Override
    public void onQuitConversationResult(int errType, int statusCode) {
    }

    @Override
    public void onConferenceNotify(String confState, List<ConferenceEventData.ConferenceUser> users) {
    }
}
