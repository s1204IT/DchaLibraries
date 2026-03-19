package com.android.internal.view;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.InputChannel;
import com.android.internal.view.IInputMethodSession;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class InputBindResult implements Parcelable {
    public final InputChannel channel;
    public final String id;
    public final IInputMethodSession method;
    public final int result;
    public final int sequence;
    public final int userActionNotificationSequenceNumber;
    public static final Parcelable.Creator<InputBindResult> CREATOR = new Parcelable.Creator<InputBindResult>() {
        @Override
        public InputBindResult createFromParcel(Parcel parcel) {
            return new InputBindResult(parcel);
        }

        @Override
        public InputBindResult[] newArray(int i) {
            return new InputBindResult[i];
        }
    };
    public static final InputBindResult NULL = error(4);
    public static final InputBindResult NO_IME = error(5);
    public static final InputBindResult NO_EDITOR = error(12);
    public static final InputBindResult INVALID_PACKAGE_NAME = error(6);
    public static final InputBindResult NULL_EDITOR_INFO = error(10);
    public static final InputBindResult NOT_IME_TARGET_WINDOW = error(11);
    public static final InputBindResult IME_NOT_CONNECTED = error(8);
    public static final InputBindResult INVALID_USER = error(9);

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {
        public static final int ERROR_IME_NOT_CONNECTED = 8;
        public static final int ERROR_INVALID_PACKAGE_NAME = 6;
        public static final int ERROR_INVALID_USER = 9;
        public static final int ERROR_NOT_IME_TARGET_WINDOW = 11;
        public static final int ERROR_NO_EDITOR = 12;
        public static final int ERROR_NO_IME = 5;
        public static final int ERROR_NULL = 4;
        public static final int ERROR_NULL_EDITOR_INFO = 10;
        public static final int ERROR_SYSTEM_NOT_READY = 7;
        public static final int SUCCESS_REPORT_WINDOW_FOCUS_ONLY = 3;
        public static final int SUCCESS_WAITING_IME_BINDING = 2;
        public static final int SUCCESS_WAITING_IME_SESSION = 1;
        public static final int SUCCESS_WITH_IME_SESSION = 0;
    }

    public InputBindResult(int i, IInputMethodSession iInputMethodSession, InputChannel inputChannel, String str, int i2, int i3) {
        this.result = i;
        this.method = iInputMethodSession;
        this.channel = inputChannel;
        this.id = str;
        this.sequence = i2;
        this.userActionNotificationSequenceNumber = i3;
    }

    InputBindResult(Parcel parcel) {
        this.result = parcel.readInt();
        this.method = IInputMethodSession.Stub.asInterface(parcel.readStrongBinder());
        if (parcel.readInt() != 0) {
            this.channel = InputChannel.CREATOR.createFromParcel(parcel);
        } else {
            this.channel = null;
        }
        this.id = parcel.readString();
        this.sequence = parcel.readInt();
        this.userActionNotificationSequenceNumber = parcel.readInt();
    }

    public String toString() {
        return "InputBindResult{result=" + getResultString() + " method=" + this.method + " id=" + this.id + " sequence=" + this.sequence + " userActionNotificationSequenceNumber=" + this.userActionNotificationSequenceNumber + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.result);
        parcel.writeStrongInterface(this.method);
        if (this.channel != null) {
            parcel.writeInt(1);
            this.channel.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeString(this.id);
        parcel.writeInt(this.sequence);
        parcel.writeInt(this.userActionNotificationSequenceNumber);
    }

    @Override
    public int describeContents() {
        if (this.channel != null) {
            return this.channel.describeContents();
        }
        return 0;
    }

    public String getResultString() {
        switch (this.result) {
            case 0:
                return "SUCCESS_WITH_IME_SESSION";
            case 1:
                return "SUCCESS_WAITING_IME_SESSION";
            case 2:
                return "SUCCESS_WAITING_IME_BINDING";
            case 3:
                return "SUCCESS_REPORT_WINDOW_FOCUS_ONLY";
            case 4:
                return "ERROR_NULL";
            case 5:
                return "ERROR_NO_IME";
            case 6:
                return "ERROR_INVALID_PACKAGE_NAME";
            case 7:
                return "ERROR_SYSTEM_NOT_READY";
            case 8:
                return "ERROR_IME_NOT_CONNECTED";
            case 9:
                return "ERROR_INVALID_USER";
            case 10:
                return "ERROR_NULL_EDITOR_INFO";
            case 11:
                return "ERROR_NOT_IME_TARGET_WINDOW";
            case 12:
                return "ERROR_NO_EDITOR";
            default:
                return "Unknown(" + this.result + ")";
        }
    }

    private static InputBindResult error(int i) {
        return new InputBindResult(i, null, null, null, -1, -1);
    }
}
