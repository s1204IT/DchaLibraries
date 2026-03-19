package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.cat.AppInterface;

public class CatCmdMessage implements Parcelable {
    public static final Parcelable.Creator<CatCmdMessage> CREATOR = new Parcelable.Creator<CatCmdMessage>() {
        @Override
        public CatCmdMessage createFromParcel(Parcel parcel) {
            return new CatCmdMessage(parcel);
        }

        @Override
        public CatCmdMessage[] newArray(int i) {
            return new CatCmdMessage[i];
        }
    };
    public BrowserSettings mBrowserSettings;
    public CallSettings mCallSettings;
    public CommandDetails mCmdDet;
    public Input mInput;
    public boolean mLoadIconFailed;
    public Menu mMenu;
    public SetupEventListSettings mSetupEventListSettings;
    public TextMessage mTextMsg;
    public ToneSettings mToneSettings;

    public class BrowserSettings {
        public LaunchBrowserMode mode;
        public String url;

        public BrowserSettings() {
        }
    }

    public class CallSettings {
        public TextMessage callMsg;
        public TextMessage confirmMsg;

        public CallSettings() {
        }
    }

    public class SetupEventListSettings {
        public int[] eventList;

        public SetupEventListSettings() {
        }
    }

    public final class SetupEventListConstants {
        public static final int BROWSER_TERMINATION_EVENT = 8;
        public static final int BROWSING_STATUS_EVENT = 15;
        public static final int IDLE_SCREEN_AVAILABLE_EVENT = 5;
        public static final int LANGUAGE_SELECTION_EVENT = 7;
        public static final int USER_ACTIVITY_EVENT = 4;

        public SetupEventListConstants() {
        }
    }

    public final class BrowserTerminationCauses {
        public static final int ERROR_TERMINATION = 1;
        public static final int USER_TERMINATION = 0;

        public BrowserTerminationCauses() {
        }
    }

    public CatCmdMessage(CommandParams commandParams) {
        this.mBrowserSettings = null;
        this.mToneSettings = null;
        this.mCallSettings = null;
        this.mSetupEventListSettings = null;
        this.mLoadIconFailed = false;
        this.mCmdDet = commandParams.mCmdDet;
        this.mLoadIconFailed = commandParams.mLoadIconFailed;
        switch (getCmdType()) {
            case SET_UP_MENU:
            case SELECT_ITEM:
                this.mMenu = ((SelectItemParams) commandParams).mMenu;
                break;
            case DISPLAY_TEXT:
            case SET_UP_IDLE_MODE_TEXT:
            case SEND_DTMF:
            case SEND_SMS:
            case SEND_SS:
            case SEND_USSD:
                this.mTextMsg = ((DisplayTextParams) commandParams).mTextMsg;
                break;
            case GET_INPUT:
            case GET_INKEY:
                this.mInput = ((GetInputParams) commandParams).mInput;
                break;
            case LAUNCH_BROWSER:
                LaunchBrowserParams launchBrowserParams = (LaunchBrowserParams) commandParams;
                this.mTextMsg = launchBrowserParams.mConfirmMsg;
                this.mBrowserSettings = new BrowserSettings();
                this.mBrowserSettings.url = launchBrowserParams.mUrl;
                this.mBrowserSettings.mode = launchBrowserParams.mMode;
                break;
            case PLAY_TONE:
                PlayToneParams playToneParams = (PlayToneParams) commandParams;
                this.mToneSettings = playToneParams.mSettings;
                this.mTextMsg = playToneParams.mTextMsg;
                break;
            case GET_CHANNEL_STATUS:
                this.mTextMsg = ((CallSetupParams) commandParams).mConfirmMsg;
                break;
            case SET_UP_CALL:
                this.mCallSettings = new CallSettings();
                CallSetupParams callSetupParams = (CallSetupParams) commandParams;
                this.mCallSettings.confirmMsg = callSetupParams.mConfirmMsg;
                this.mCallSettings.callMsg = callSetupParams.mCallMsg;
                break;
            case OPEN_CHANNEL:
            case CLOSE_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
                this.mTextMsg = ((BIPClientParams) commandParams).mTextMsg;
                break;
            case SET_UP_EVENT_LIST:
                this.mSetupEventListSettings = new SetupEventListSettings();
                this.mSetupEventListSettings.eventList = ((SetEventListParams) commandParams).mEventInfo;
                break;
        }
    }

    public CatCmdMessage(Parcel parcel) {
        this.mBrowserSettings = null;
        this.mToneSettings = null;
        this.mCallSettings = null;
        this.mSetupEventListSettings = null;
        this.mLoadIconFailed = false;
        this.mCmdDet = (CommandDetails) parcel.readParcelable(null);
        this.mTextMsg = (TextMessage) parcel.readParcelable(null);
        this.mMenu = (Menu) parcel.readParcelable(null);
        this.mInput = (Input) parcel.readParcelable(null);
        this.mLoadIconFailed = parcel.readByte() == 1;
        int i = AnonymousClass2.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()];
        if (i == 14) {
            this.mCallSettings = new CallSettings();
            this.mCallSettings.confirmMsg = (TextMessage) parcel.readParcelable(null);
            this.mCallSettings.callMsg = (TextMessage) parcel.readParcelable(null);
            return;
        }
        if (i != 19) {
            switch (i) {
                case 11:
                    this.mBrowserSettings = new BrowserSettings();
                    this.mBrowserSettings.url = parcel.readString();
                    this.mBrowserSettings.mode = LaunchBrowserMode.values()[parcel.readInt()];
                    break;
                case 12:
                    this.mToneSettings = (ToneSettings) parcel.readParcelable(null);
                    break;
            }
            return;
        }
        this.mSetupEventListSettings = new SetupEventListSettings();
        int i2 = parcel.readInt();
        this.mSetupEventListSettings.eventList = new int[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            this.mSetupEventListSettings.eventList[i3] = parcel.readInt();
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mCmdDet, 0);
        parcel.writeParcelable(this.mTextMsg, 0);
        parcel.writeParcelable(this.mMenu, 0);
        parcel.writeParcelable(this.mInput, 0);
        parcel.writeByte(this.mLoadIconFailed ? (byte) 1 : (byte) 0);
        int i2 = AnonymousClass2.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()];
        if (i2 == 14) {
            parcel.writeParcelable(this.mCallSettings.confirmMsg, 0);
            parcel.writeParcelable(this.mCallSettings.callMsg, 0);
        } else {
            if (i2 != 19) {
                switch (i2) {
                    case 11:
                        parcel.writeString(this.mBrowserSettings.url);
                        parcel.writeInt(this.mBrowserSettings.mode.ordinal());
                        break;
                    case 12:
                        parcel.writeParcelable(this.mToneSettings, 0);
                        break;
                }
            }
            parcel.writeIntArray(this.mSetupEventListSettings.eventList);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public AppInterface.CommandType getCmdType() {
        return AppInterface.CommandType.fromInt(this.mCmdDet.typeOfCommand);
    }

    public Menu getMenu() {
        return this.mMenu;
    }

    public Input geInput() {
        return this.mInput;
    }

    public TextMessage geTextMessage() {
        return this.mTextMsg;
    }

    public BrowserSettings getBrowserSettings() {
        return this.mBrowserSettings;
    }

    public ToneSettings getToneSettings() {
        return this.mToneSettings;
    }

    public CallSettings getCallSettings() {
        return this.mCallSettings;
    }

    public SetupEventListSettings getSetEventList() {
        return this.mSetupEventListSettings;
    }

    public boolean hasIconLoadFailed() {
        return this.mLoadIconFailed;
    }
}
