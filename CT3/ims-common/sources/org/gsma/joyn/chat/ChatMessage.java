package org.gsma.joyn.chat;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import org.gsma.joyn.Logger;

public class ChatMessage implements Parcelable {
    public static final Parcelable.Creator<ChatMessage> CREATOR = new Parcelable.Creator<ChatMessage>() {
        @Override
        public ChatMessage createFromParcel(Parcel source) {
            return new ChatMessage(source);
        }

        @Override
        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };
    public static final String MIME_TYPE = "text/plain";
    public static final String TAG = "TAPI-ChatMessage";
    private String contact;
    private String displayName;
    private boolean displayedReportRequested;
    private String id;
    private String message;
    private Date receiptAt;

    public ChatMessage(String messageId, String remote, String message, Date receiptAt, boolean displayedReportRequested, String displayName) {
        this.displayedReportRequested = false;
        Logger.i(TAG, "ChatMessage entrymessageId=" + messageId + " remote=" + remote + " message=" + message + " receiptAt=" + receiptAt + " displayedReportRequested=" + displayedReportRequested);
        Logger.i(TAG, "ABCG ChatMessage entrydisplayname=" + displayName);
        this.id = messageId;
        this.contact = remote;
        this.message = message;
        this.displayedReportRequested = displayedReportRequested;
        this.receiptAt = receiptAt;
        this.displayName = displayName;
    }

    public ChatMessage(Parcel source) {
        this.displayedReportRequested = false;
        this.id = source.readString();
        this.contact = source.readString();
        this.message = source.readString();
        this.receiptAt = new Date(source.readLong());
        this.displayedReportRequested = source.readInt() != 0;
        this.displayName = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.contact);
        dest.writeString(this.message);
        dest.writeLong(this.receiptAt.getTime());
        dest.writeInt(this.displayedReportRequested ? 1 : 0);
        dest.writeString(this.displayName);
    }

    public String getId() {
        return this.id;
    }

    public String getContact() {
        return this.contact;
    }

    public String getMessage() {
        return this.message;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Date getReceiptDate() {
        return this.receiptAt;
    }

    public boolean isDisplayedReportRequested() {
        return this.displayedReportRequested;
    }
}
