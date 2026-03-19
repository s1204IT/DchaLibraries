package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.cat.AppInterface;
import com.mediatek.internal.telephony.ppl.PplMessageManager;

public class CatCmdMessage implements Parcelable {

    private static final int[] f16x72eb89a2 = null;
    public static final Parcelable.Creator<CatCmdMessage> CREATOR = new Parcelable.Creator<CatCmdMessage>() {
        @Override
        public CatCmdMessage createFromParcel(Parcel in) {
            return new CatCmdMessage(in);
        }

        @Override
        public CatCmdMessage[] newArray(int size) {
            return new CatCmdMessage[size];
        }
    };
    private BrowserSettings mBrowserSettings;
    private CallSettings mCallSettings;
    CommandDetails mCmdDet;
    public String mDestAddress;
    public int mInfoType;
    private Input mInput;
    private boolean mLoadIconFailed;
    private Menu mMenu;
    private SetupEventListSettings mSetupEventListSettings;
    private TextMessage mTextMsg;
    private ToneSettings mToneSettings;

    private static int[] m209xe796fd46() {
        if (f16x72eb89a2 != null) {
            return f16x72eb89a2;
        }
        int[] iArr = new int[AppInterface.CommandType.valuesCustom().length];
        try {
            iArr[AppInterface.CommandType.ACTIVATE.ordinal()] = 23;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[AppInterface.CommandType.CALLCTRL_RSP_MSG.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[AppInterface.CommandType.DECLARE_SERVICE.ordinal()] = 24;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_MULTIMEDIA_MESSAGE.ordinal()] = 25;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_TEXT.ordinal()] = 3;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[AppInterface.CommandType.GET_FRAME_STATUS.ordinal()] = 26;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INKEY.ordinal()] = 5;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INPUT.ordinal()] = 6;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[AppInterface.CommandType.GET_READER_STATUS.ordinal()] = 27;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[AppInterface.CommandType.GET_SERVICE_INFORMATION.ordinal()] = 28;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[AppInterface.CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 29;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[AppInterface.CommandType.LAUNCH_BROWSER.ordinal()] = 7;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[AppInterface.CommandType.MORE_TIME.ordinal()] = 30;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 8;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[AppInterface.CommandType.PERFORM_CARD_APDU.ordinal()] = 31;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[AppInterface.CommandType.PLAY_TONE.ordinal()] = 9;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[AppInterface.CommandType.POLLING_OFF.ordinal()] = 32;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[AppInterface.CommandType.POLL_INTERVAL.ordinal()] = 33;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_OFF_CARD.ordinal()] = 34;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_ON_CARD.ordinal()] = 35;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.ordinal()] = 10;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 11;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[AppInterface.CommandType.REFRESH.ordinal()] = 12;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[AppInterface.CommandType.RETRIEVE_MULTIMEDIA_MESSAGE.ordinal()] = 36;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[AppInterface.CommandType.RUN_AT_COMMAND.ordinal()] = 37;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[AppInterface.CommandType.SELECT_ITEM.ordinal()] = 13;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DATA.ordinal()] = 14;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DTMF.ordinal()] = 15;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SMS.ordinal()] = 16;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SS.ordinal()] = 17;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_USSD.ordinal()] = 18;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[AppInterface.CommandType.SERVICE_SEARCH.ordinal()] = 38;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[AppInterface.CommandType.SET_FRAME.ordinal()] = 39;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_CALL.ordinal()] = 19;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 20;
        } catch (NoSuchFieldError e37) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 21;
        } catch (NoSuchFieldError e38) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_MENU.ordinal()] = 22;
        } catch (NoSuchFieldError e39) {
        }
        try {
            iArr[AppInterface.CommandType.SUBMIT_MULTIMEDIA_MESSAGE.ordinal()] = 40;
        } catch (NoSuchFieldError e40) {
        }
        try {
            iArr[AppInterface.CommandType.TIMER_MANAGEMENT.ordinal()] = 41;
        } catch (NoSuchFieldError e41) {
        }
        f16x72eb89a2 = iArr;
        return iArr;
    }

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
        public static final int CHANNEL_STATUS_EVENT = 10;
        public static final int DATA_AVAILABLE_EVENT = 9;
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

    CatCmdMessage(CommandParams cmdParams) {
        this.mBrowserSettings = null;
        this.mToneSettings = null;
        this.mCallSettings = null;
        this.mSetupEventListSettings = null;
        this.mInfoType = 0;
        this.mDestAddress = null;
        this.mLoadIconFailed = false;
        this.mCmdDet = cmdParams.mCmdDet;
        this.mLoadIconFailed = cmdParams.mLoadIconFailed;
        switch (m209xe796fd46()[getCmdType().ordinal()]) {
            case 1:
                this.mTextMsg = ((CallCtrlBySimParams) cmdParams).mTextMsg;
                this.mInfoType = ((CallCtrlBySimParams) cmdParams).mInfoType;
                this.mDestAddress = ((CallCtrlBySimParams) cmdParams).mDestAddress;
                break;
            case 2:
            case 8:
            case 11:
            case 14:
                BIPClientParams param = (BIPClientParams) cmdParams;
                this.mTextMsg = param.mTextMsg;
                break;
            case 3:
            case 15:
            case 16:
            case 17:
            case 18:
            case 21:
                this.mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
                break;
            case 4:
                this.mTextMsg = ((CallSetupParams) cmdParams).mConfirmMsg;
                break;
            case 5:
            case 6:
                this.mInput = ((GetInputParams) cmdParams).mInput;
                break;
            case 7:
                this.mTextMsg = ((LaunchBrowserParams) cmdParams).mConfirmMsg;
                this.mBrowserSettings = new BrowserSettings();
                this.mBrowserSettings.url = ((LaunchBrowserParams) cmdParams).mUrl;
                this.mBrowserSettings.mode = ((LaunchBrowserParams) cmdParams).mMode;
                break;
            case 9:
                PlayToneParams params = (PlayToneParams) cmdParams;
                this.mToneSettings = params.mSettings;
                this.mTextMsg = params.mTextMsg;
                break;
            case 12:
                this.mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
                break;
            case 13:
            case 22:
                this.mMenu = ((SelectItemParams) cmdParams).mMenu;
                break;
            case 19:
                this.mCallSettings = new CallSettings();
                this.mCallSettings.confirmMsg = ((CallSetupParams) cmdParams).mConfirmMsg;
                this.mCallSettings.callMsg = ((CallSetupParams) cmdParams).mCallMsg;
                break;
            case 20:
                this.mSetupEventListSettings = new SetupEventListSettings();
                SetupEventListParams mEventParams = (SetupEventListParams) cmdParams;
                int length = mEventParams.eventList.length;
                this.mSetupEventListSettings.eventList = new int[length];
                for (int i = 0; i < length; i++) {
                    this.mSetupEventListSettings.eventList[i] = mEventParams.eventList[i] & PplMessageManager.Type.INVALID;
                }
                break;
        }
    }

    public CatCmdMessage(Parcel in) {
        this.mBrowserSettings = null;
        this.mToneSettings = null;
        this.mCallSettings = null;
        this.mSetupEventListSettings = null;
        this.mInfoType = 0;
        this.mDestAddress = null;
        this.mLoadIconFailed = false;
        this.mCmdDet = (CommandDetails) in.readParcelable(null);
        this.mTextMsg = (TextMessage) in.readParcelable(null);
        this.mMenu = (Menu) in.readParcelable(null);
        this.mInput = (Input) in.readParcelable(null);
        this.mLoadIconFailed = in.readByte() == 1;
        switch (m209xe796fd46()[getCmdType().ordinal()]) {
            case 7:
                this.mBrowserSettings = new BrowserSettings();
                this.mBrowserSettings.url = in.readString();
                this.mBrowserSettings.mode = LaunchBrowserMode.valuesCustom()[in.readInt()];
                break;
            case 9:
                this.mToneSettings = (ToneSettings) in.readParcelable(null);
                break;
            case 19:
                this.mCallSettings = new CallSettings();
                this.mCallSettings.confirmMsg = (TextMessage) in.readParcelable(null);
                this.mCallSettings.callMsg = (TextMessage) in.readParcelable(null);
                break;
            case 20:
                this.mSetupEventListSettings = new SetupEventListSettings();
                int length = in.readInt();
                this.mSetupEventListSettings.eventList = new int[length];
                for (int i = 0; i < length; i++) {
                    this.mSetupEventListSettings.eventList[i] = in.readInt();
                }
                break;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mCmdDet, 0);
        dest.writeParcelable(this.mTextMsg, 0);
        dest.writeParcelable(this.mMenu, 0);
        dest.writeParcelable(this.mInput, 0);
        dest.writeByte((byte) (this.mLoadIconFailed ? 1 : 0));
        switch (m209xe796fd46()[getCmdType().ordinal()]) {
            case 7:
                dest.writeString(this.mBrowserSettings.url);
                dest.writeInt(this.mBrowserSettings.mode.ordinal());
                break;
            case 9:
                dest.writeParcelable(this.mToneSettings, 0);
                break;
            case 19:
                dest.writeParcelable(this.mCallSettings.confirmMsg, 0);
                dest.writeParcelable(this.mCallSettings.callMsg, 0);
                break;
            case 20:
                dest.writeIntArray(this.mSetupEventListSettings.eventList);
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getCmdQualifier() {
        return this.mCmdDet.commandQualifier;
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
