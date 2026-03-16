package com.android.internal.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.InputChannel;
import com.android.internal.view.IInputMethodSession;

public final class InputBindResult implements Parcelable {
    public static final Parcelable.Creator<InputBindResult> CREATOR = new Parcelable.Creator<InputBindResult>() {
        @Override
        public InputBindResult createFromParcel(Parcel source) {
            return new InputBindResult(source);
        }

        @Override
        public InputBindResult[] newArray(int size) {
            return new InputBindResult[size];
        }
    };
    static final String TAG = "InputBindResult";
    public final InputChannel channel;
    public final String id;
    public final IInputMethodSession method;
    public final int sequence;
    public final int userActionNotificationSequenceNumber;

    public InputBindResult(IInputMethodSession _method, InputChannel _channel, String _id, int _sequence, int _userActionNotificationSequenceNumber) {
        this.method = _method;
        this.channel = _channel;
        this.id = _id;
        this.sequence = _sequence;
        this.userActionNotificationSequenceNumber = _userActionNotificationSequenceNumber;
    }

    InputBindResult(Parcel source) {
        this.method = IInputMethodSession.Stub.asInterface(source.readStrongBinder());
        if (source.readInt() != 0) {
            this.channel = InputChannel.CREATOR.createFromParcel(source);
        } else {
            this.channel = null;
        }
        this.id = source.readString();
        this.sequence = source.readInt();
        this.userActionNotificationSequenceNumber = source.readInt();
    }

    public String toString() {
        return "InputBindResult{" + this.method + " " + this.id + " sequence:" + this.sequence + " userActionNotificationSequenceNumber:" + this.userActionNotificationSequenceNumber + "}";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongInterface(this.method);
        if (this.channel != null) {
            dest.writeInt(1);
            this.channel.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.id);
        dest.writeInt(this.sequence);
        dest.writeInt(this.userActionNotificationSequenceNumber);
    }

    @Override
    public int describeContents() {
        if (this.channel != null) {
            return this.channel.describeContents();
        }
        return 0;
    }
}
