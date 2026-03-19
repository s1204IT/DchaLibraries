package org.gsma.joyn.chat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import org.gsma.joyn.Logger;

public class ExtendMessage extends ChatMessage implements Parcelable {
    public static final Parcelable.Creator<ExtendMessage> CREATOR = new Parcelable.Creator<ExtendMessage>() {
        @Override
        public ExtendMessage createFromParcel(Parcel source) {
            return new ExtendMessage(source);
        }

        @Override
        public ExtendMessage[] newArray(int size) {
            return new ExtendMessage[size];
        }
    };
    public static final String TAG = "TAPI-ExtendChatMessage";
    private int direction;
    private int msgType;
    private boolean secondary;

    public ExtendMessage(String messageId, String remote, String message, Date receiptAt, boolean imdnDisplayedRequested, String displayName, int msgType) {
        super(messageId, remote, message, receiptAt, imdnDisplayedRequested, displayName);
        this.msgType = msgType;
        Logger.i(TAG, "ExtendChatMessage entry");
    }

    public ExtendMessage(Parcel source) {
        super(source);
        this.msgType = source.readInt();
        this.direction = source.readInt();
        this.secondary = source.readInt() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.msgType);
        dest.writeInt(this.direction);
        dest.writeInt(this.secondary ? 1 : 0);
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setSecondary(boolean secondary) {
        this.secondary = secondary;
    }

    public int getMessageType() {
        return this.msgType;
    }

    public int getDirection() {
        return this.direction;
    }

    public boolean isSecondary() {
        return this.secondary;
    }
}
