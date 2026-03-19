package org.gsma.joyn.chat;

import android.os.Parcel;
import android.os.Parcelable;
import org.gsma.joyn.Logger;

public class ChatServiceConfiguration {
    public static final Parcelable.Creator<ChatServiceConfiguration> CREATOR = new Parcelable.Creator<ChatServiceConfiguration>() {
        @Override
        public ChatServiceConfiguration createFromParcel(Parcel source) {
            return new ChatServiceConfiguration(source);
        }

        @Override
        public ChatServiceConfiguration[] newArray(int size) {
            return new ChatServiceConfiguration[size];
        }
    };
    public static final String TAG = "ChatServiceConfiguration";
    private boolean autoAcceptGroupChat;
    private boolean autoAcceptSingleChat;
    private int chatTimeout;
    private boolean displayedDeliveryReport;
    private int geolocExpireTime;
    private int isComposingTimeout;
    private boolean mIsImCapAlwaysOn;
    private int maxGeolocLabelLength;
    private int maxGroupChat;
    private int maxGroupChatParticipants;
    private long maxMsgLengthGroupChat;
    private long maxMsgLengthSingleChat;
    private boolean smsFallback;
    private boolean warnSF;

    public ChatServiceConfiguration(boolean warnSF, int chatTimeout, int isComposingTimeout, int maxGroupChatParticipants, long maxMsgLengthSingleChat, long maxMsgLengthGroupChat, int maxGroupChat, boolean smsFallback, boolean autoAcceptSingleChat, boolean autoAcceptGroupChat, boolean displayedDeliveryReport, int maxGeolocLabelLength, int geolocExpireTime, boolean isImCapAlwaysOn) {
        Logger.i(TAG, "ChatServiceConfiguration entrywarnSF " + warnSF + "chatTimeout " + chatTimeout + "isComposingTimeout " + isComposingTimeout + "maxGroupChatParticipants" + maxGroupChatParticipants + "maxMsgLengthSingleChat " + maxMsgLengthSingleChat + "maxMsgLengthGroupChat" + maxMsgLengthGroupChat + "maxGroupChat " + maxGroupChat + "smsFallback " + smsFallback + "autoAcceptSingleChat " + autoAcceptSingleChat + "autoAcceptGroupChat " + autoAcceptGroupChat + "displayedDeliveryReport " + displayedDeliveryReport + "maxGeolocLabelLength " + maxGeolocLabelLength + "geolocExpireTime " + geolocExpireTime + "isImCapAlwaysOn " + isImCapAlwaysOn);
        this.warnSF = warnSF;
        this.chatTimeout = chatTimeout;
        this.isComposingTimeout = isComposingTimeout;
        this.maxGroupChatParticipants = maxGroupChatParticipants;
        this.maxMsgLengthSingleChat = maxMsgLengthSingleChat;
        this.maxMsgLengthGroupChat = maxMsgLengthGroupChat;
        this.maxGroupChat = maxGroupChat;
        this.smsFallback = smsFallback;
        this.autoAcceptSingleChat = autoAcceptSingleChat;
        this.autoAcceptGroupChat = autoAcceptGroupChat;
        this.displayedDeliveryReport = displayedDeliveryReport;
        this.maxGeolocLabelLength = maxGeolocLabelLength;
        this.geolocExpireTime = geolocExpireTime;
        this.mIsImCapAlwaysOn = isImCapAlwaysOn;
    }

    public ChatServiceConfiguration(Parcel source) {
        this.chatTimeout = source.readInt();
        this.isComposingTimeout = source.readInt();
        this.maxGroupChatParticipants = source.readInt();
        this.maxMsgLengthSingleChat = source.readLong();
        this.maxMsgLengthGroupChat = source.readLong();
        this.smsFallback = source.readInt() != 0;
        this.autoAcceptSingleChat = source.readInt() != 0;
        this.autoAcceptGroupChat = source.readInt() != 0;
        this.displayedDeliveryReport = source.readInt() != 0;
        this.warnSF = source.readInt() != 0;
        this.maxGroupChat = source.readInt();
        this.maxGeolocLabelLength = source.readInt();
        this.geolocExpireTime = source.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.chatTimeout);
        dest.writeInt(this.isComposingTimeout);
        dest.writeInt(this.maxGroupChatParticipants);
        dest.writeLong(this.maxMsgLengthSingleChat);
        dest.writeLong(this.maxMsgLengthGroupChat);
        dest.writeInt(this.smsFallback ? 1 : 0);
        dest.writeInt(this.autoAcceptSingleChat ? 1 : 0);
        dest.writeInt(this.autoAcceptGroupChat ? 1 : 0);
        dest.writeInt(this.displayedDeliveryReport ? 1 : 0);
        dest.writeInt(this.warnSF ? 1 : 0);
        dest.writeInt(this.maxGroupChat);
        dest.writeInt(this.maxGeolocLabelLength);
        dest.writeInt(this.geolocExpireTime);
    }

    public boolean isImWarnSf() {
        return this.warnSF;
    }

    public int getChatSessionTimeout() {
        return this.chatTimeout;
    }

    public int getIsComposingTimeout() {
        return this.isComposingTimeout;
    }

    public int getGroupChatMaxParticipantsNumber() {
        return this.maxGroupChatParticipants;
    }

    public long getSingleChatMessageMaxLength() {
        return this.maxMsgLengthSingleChat;
    }

    public long getGroupChatMessageMaxLength() {
        return this.maxMsgLengthGroupChat;
    }

    public int getMaxGroupChats() {
        return this.maxGroupChat;
    }

    public boolean isSmsFallback() {
        return this.smsFallback;
    }

    public boolean isChatAutoAcceptMode() {
        return this.autoAcceptSingleChat;
    }

    public boolean isGroupChatAutoAcceptMode() {
        return this.autoAcceptGroupChat;
    }

    public boolean isDisplayedDeliveryReport() {
        return this.displayedDeliveryReport;
    }

    public void setDisplayedDeliveryReport(boolean state) {
        this.displayedDeliveryReport = state;
    }

    public int getGeolocLabelMaxLength() {
        return this.maxGeolocLabelLength;
    }

    public int getGeolocExpirationTime() {
        return this.geolocExpireTime;
    }

    public boolean isImCapAlwaysOn() {
        return this.mIsImCapAlwaysOn;
    }
}
