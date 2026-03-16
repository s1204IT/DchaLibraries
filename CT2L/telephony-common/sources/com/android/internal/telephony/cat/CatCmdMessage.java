package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.InterfaceTransportLevel;

public class CatCmdMessage implements Parcelable {
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
    private ChannelSettings mChannelSettings;
    CommandDetails mCmdDet;
    private DataSettings mDataSettings;
    private Input mInput;
    private Menu mMenu;
    private RefreshSettings mRefreshSettings;
    private SendDTMFSettings mSendDTMFSettings;
    private SendSMSSettings mSendSMSSettings;
    private SendSSSettings mSendSSSettings;
    private SendUSSDSettings mSendUSSDSettings;
    private SetupEventListSettings mSetupEventListSettings;
    private TextMessage mTextMsg;
    private ToneSettings mToneSettings;

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

    public class ChannelSettings {
        public BearerDescription bearerDescription;
        public int bufSize;
        public int channel;
        public Integer cid;
        public byte[] destinationAddress;
        public String networkAccessName;
        public int port;
        public InterfaceTransportLevel.TransportProtocol protocol;
        public String userLogin;
        public String userPassword;

        public ChannelSettings() {
        }
    }

    public class DataSettings {
        public int channel;
        public byte[] data;
        public int length;

        public DataSettings() {
        }
    }

    public class RefreshSettings {
        public boolean fileChanged;
        public int[] fileList;

        public RefreshSettings() {
        }
    }

    public class SendDTMFSettings {
        public String dtmfString;
        public TextMessage text;

        public SendDTMFSettings() {
        }
    }

    public class SendSSSettings {
        public String sSString;
        public TextMessage text;

        public SendSSSettings() {
        }
    }

    public class SendUSSDSettings {
        public TextMessage text;
        public String uSSDString;

        public SendUSSDSettings() {
        }
    }

    public class SendSMSSettings {
        public byte[] data;
        public int length;
        public String smsAdress;
        public TextMessage text;

        public SendSMSSettings() {
        }
    }

    CatCmdMessage(CommandParams cmdParams) {
        this.mBrowserSettings = null;
        this.mToneSettings = null;
        this.mCallSettings = null;
        this.mSetupEventListSettings = null;
        this.mRefreshSettings = null;
        this.mChannelSettings = null;
        this.mDataSettings = null;
        this.mSendDTMFSettings = null;
        this.mSendSSSettings = null;
        this.mSendUSSDSettings = null;
        this.mSendSMSSettings = null;
        this.mCmdDet = cmdParams.mCmdDet;
        switch (getCmdType()) {
            case SET_UP_MENU:
            case SELECT_ITEM:
                this.mMenu = ((SelectItemParams) cmdParams).mMenu;
                break;
            case DISPLAY_TEXT:
            case SET_UP_IDLE_MODE_TEXT:
            case SEND_DTMF:
                this.mSendDTMFSettings = new SendDTMFSettings();
                if (cmdParams instanceof SendDTMFParams) {
                    this.mSendDTMFSettings.text = ((SendDTMFParams) cmdParams).mTextMsg;
                    this.mSendDTMFSettings.dtmfString = ((SendDTMFParams) cmdParams).mDTMFString;
                } else if (cmdParams instanceof DisplayTextParams) {
                    this.mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
                }
                break;
            case SEND_SMS:
                this.mSendSMSSettings = new SendSMSSettings();
                if (cmdParams instanceof SMSTPDUParams) {
                    this.mSendSMSSettings.text = ((SMSTPDUParams) cmdParams).mTextMsg;
                    this.mSendSMSSettings.smsAdress = ((SMSTPDUParams) cmdParams).mSMSAdress;
                    this.mSendSMSSettings.length = ((SMSTPDUParams) cmdParams).mLength;
                    this.mSendSMSSettings.data = ((SMSTPDUParams) cmdParams).mData;
                } else if (cmdParams instanceof DisplayTextParams) {
                    this.mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
                }
                break;
            case SEND_SS:
                this.mSendSSSettings = new SendSSSettings();
                if (cmdParams instanceof SendSSParams) {
                    this.mSendSSSettings.text = ((SendSSParams) cmdParams).mTextMsg;
                    this.mSendSSSettings.sSString = ((SendSSParams) cmdParams).mSSString;
                } else if (cmdParams instanceof DisplayTextParams) {
                    this.mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
                }
                break;
            case SEND_USSD:
                this.mSendUSSDSettings = new SendUSSDSettings();
                if (cmdParams instanceof SendUSSDParams) {
                    this.mSendUSSDSettings.text = ((SendUSSDParams) cmdParams).mTextMsg;
                    this.mSendUSSDSettings.uSSDString = ((SendUSSDParams) cmdParams).mUSSDString;
                } else if (cmdParams instanceof DisplayTextParams) {
                    this.mTextMsg = ((DisplayTextParams) cmdParams).mTextMsg;
                }
                break;
            case GET_INPUT:
            case GET_INKEY:
                this.mInput = ((GetInputParams) cmdParams).mInput;
                break;
            case LAUNCH_BROWSER:
                this.mTextMsg = ((LaunchBrowserParams) cmdParams).mConfirmMsg;
                this.mBrowserSettings = new BrowserSettings();
                this.mBrowserSettings.url = ((LaunchBrowserParams) cmdParams).mUrl;
                this.mBrowserSettings.mode = ((LaunchBrowserParams) cmdParams).mMode;
                break;
            case PLAY_TONE:
                PlayToneParams params = (PlayToneParams) cmdParams;
                this.mToneSettings = params.mSettings;
                this.mTextMsg = params.mTextMsg;
                break;
            case SET_UP_CALL:
                this.mCallSettings = new CallSettings();
                this.mCallSettings.confirmMsg = ((CallSetupParams) cmdParams).mConfirmMsg;
                this.mCallSettings.callMsg = ((CallSetupParams) cmdParams).mCallMsg;
                break;
            case OPEN_CHANNEL:
                this.mTextMsg = ((OpenChannelParams) cmdParams).confirmMsg;
                this.mChannelSettings = new ChannelSettings();
                this.mChannelSettings.channel = 0;
                this.mChannelSettings.protocol = ((OpenChannelParams) cmdParams).itl.protocol;
                this.mChannelSettings.port = ((OpenChannelParams) cmdParams).itl.port;
                this.mChannelSettings.bufSize = ((OpenChannelParams) cmdParams).bufSize;
                this.mChannelSettings.destinationAddress = ((OpenChannelParams) cmdParams).destinationAddress;
                this.mChannelSettings.bearerDescription = ((OpenChannelParams) cmdParams).bearerDescription;
                this.mChannelSettings.networkAccessName = ((OpenChannelParams) cmdParams).networkAccessName;
                this.mChannelSettings.userLogin = ((OpenChannelParams) cmdParams).userLogin;
                this.mChannelSettings.userPassword = ((OpenChannelParams) cmdParams).userPassword;
                break;
            case CLOSE_CHANNEL:
                this.mDataSettings = new DataSettings();
                this.mDataSettings.channel = ((CloseChannelParams) cmdParams).channel;
                this.mDataSettings.length = 0;
                this.mDataSettings.data = null;
                break;
            case RECEIVE_DATA:
                this.mDataSettings = new DataSettings();
                this.mDataSettings.channel = ((ReceiveDataParams) cmdParams).channel;
                this.mDataSettings.length = ((ReceiveDataParams) cmdParams).datLen;
                this.mDataSettings.data = null;
                break;
            case SEND_DATA:
                this.mDataSettings = new DataSettings();
                this.mDataSettings.channel = ((SendDataParams) cmdParams).channel;
                this.mDataSettings.length = 0;
                this.mDataSettings.data = ((SendDataParams) cmdParams).data;
                break;
            case GET_CHANNEL_STATUS:
                this.mDataSettings = new DataSettings();
                this.mDataSettings.channel = 0;
                this.mDataSettings.length = 0;
                this.mDataSettings.data = null;
                break;
            case SET_UP_EVENT_LIST:
                this.mSetupEventListSettings = new SetupEventListSettings();
                this.mSetupEventListSettings.eventList = ((SetEventListParams) cmdParams).mEventInfo;
                break;
            case REFRESH:
                this.mRefreshSettings = new RefreshSettings();
                this.mRefreshSettings.fileChanged = ((SetRefreshParams) cmdParams).fileChanged;
                this.mRefreshSettings.fileList = ((SetRefreshParams) cmdParams).fileList;
                break;
        }
    }

    public CatCmdMessage(Parcel in) {
        this.mBrowserSettings = null;
        this.mToneSettings = null;
        this.mCallSettings = null;
        this.mSetupEventListSettings = null;
        this.mRefreshSettings = null;
        this.mChannelSettings = null;
        this.mDataSettings = null;
        this.mSendDTMFSettings = null;
        this.mSendSSSettings = null;
        this.mSendUSSDSettings = null;
        this.mSendSMSSettings = null;
        this.mCmdDet = (CommandDetails) in.readParcelable(null);
        this.mTextMsg = (TextMessage) in.readParcelable(null);
        this.mMenu = (Menu) in.readParcelable(null);
        this.mInput = (Input) in.readParcelable(null);
        switch (getCmdType()) {
            case SEND_DTMF:
                this.mSendDTMFSettings = new SendDTMFSettings();
                this.mSendDTMFSettings.text = (TextMessage) in.readParcelable(null);
                this.mSendDTMFSettings.dtmfString = in.readString();
                break;
            case SEND_SMS:
                this.mSendSMSSettings = new SendSMSSettings();
                this.mSendSMSSettings.text = (TextMessage) in.readParcelable(null);
                this.mSendSMSSettings.smsAdress = in.readString();
                this.mSendSMSSettings.length = in.readInt();
                this.mSendSMSSettings.data = null;
                if (this.mSendSMSSettings.length > 0) {
                    this.mSendSMSSettings.data = new byte[this.mSendSMSSettings.length];
                    in.readByteArray(this.mSendSMSSettings.data);
                }
                break;
            case SEND_SS:
                this.mSendSSSettings = new SendSSSettings();
                this.mSendSSSettings.text = (TextMessage) in.readParcelable(null);
                this.mSendSSSettings.sSString = in.readString();
                break;
            case SEND_USSD:
                this.mSendUSSDSettings = new SendUSSDSettings();
                this.mSendUSSDSettings.text = (TextMessage) in.readParcelable(null);
                this.mSendUSSDSettings.uSSDString = in.readString();
                break;
            case LAUNCH_BROWSER:
                this.mBrowserSettings = new BrowserSettings();
                this.mBrowserSettings.url = in.readString();
                this.mBrowserSettings.mode = LaunchBrowserMode.values()[in.readInt()];
                break;
            case PLAY_TONE:
                this.mToneSettings = (ToneSettings) in.readParcelable(null);
                break;
            case SET_UP_CALL:
                this.mCallSettings = new CallSettings();
                this.mCallSettings.confirmMsg = (TextMessage) in.readParcelable(null);
                this.mCallSettings.callMsg = (TextMessage) in.readParcelable(null);
                break;
            case OPEN_CHANNEL:
                this.mChannelSettings = new ChannelSettings();
                this.mChannelSettings.channel = in.readInt();
                this.mChannelSettings.protocol = InterfaceTransportLevel.TransportProtocol.values()[in.readInt()];
                this.mChannelSettings.port = in.readInt();
                this.mChannelSettings.bufSize = in.readInt();
                this.mChannelSettings.destinationAddress = new byte[in.readInt()];
                if (this.mChannelSettings.destinationAddress.length > 0) {
                    in.readByteArray(this.mChannelSettings.destinationAddress);
                }
                this.mChannelSettings.bearerDescription = (BearerDescription) in.readValue(BearerDescription.class.getClassLoader());
                this.mChannelSettings.networkAccessName = in.readString();
                this.mChannelSettings.userLogin = in.readString();
                this.mChannelSettings.userPassword = in.readString();
                break;
            case CLOSE_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
                this.mDataSettings = new DataSettings();
                this.mDataSettings.channel = in.readInt();
                this.mDataSettings.length = in.readInt();
                this.mDataSettings.data = null;
                int len = in.readInt();
                if (len > 0) {
                    this.mDataSettings.data = new byte[len];
                    in.readByteArray(this.mDataSettings.data);
                }
                break;
            case SET_UP_EVENT_LIST:
                this.mSetupEventListSettings = new SetupEventListSettings();
                int length = in.readInt();
                this.mSetupEventListSettings.eventList = new int[length];
                for (int i = 0; i < length; i++) {
                    this.mSetupEventListSettings.eventList[i] = in.readInt();
                }
                break;
            case REFRESH:
                this.mRefreshSettings = new RefreshSettings();
                this.mRefreshSettings.fileChanged = in.readInt() != 0;
                this.mRefreshSettings.fileList = new int[in.readInt()];
                in.readIntArray(this.mRefreshSettings.fileList);
                break;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mCmdDet, 0);
        dest.writeParcelable(this.mTextMsg, 0);
        dest.writeParcelable(this.mMenu, 0);
        dest.writeParcelable(this.mInput, 0);
        switch (getCmdType()) {
            case SEND_DTMF:
                dest.writeParcelable(this.mSendDTMFSettings.text, 0);
                dest.writeString(this.mSendDTMFSettings.dtmfString);
                break;
            case SEND_SMS:
                dest.writeParcelable(this.mSendSMSSettings.text, 0);
                dest.writeString(this.mSendSMSSettings.smsAdress);
                dest.writeInt(this.mSendSMSSettings.length);
                if (this.mSendSMSSettings.length > 0) {
                    dest.writeByteArray(this.mSendSMSSettings.data);
                }
                break;
            case SEND_SS:
                dest.writeParcelable(this.mSendSSSettings.text, 0);
                dest.writeString(this.mSendSSSettings.sSString);
                break;
            case SEND_USSD:
                dest.writeParcelable(this.mSendUSSDSettings.text, 0);
                dest.writeString(this.mSendUSSDSettings.uSSDString);
                break;
            case LAUNCH_BROWSER:
                dest.writeString(this.mBrowserSettings.url);
                dest.writeInt(this.mBrowserSettings.mode.ordinal());
                break;
            case PLAY_TONE:
                dest.writeParcelable(this.mToneSettings, 0);
                break;
            case SET_UP_CALL:
                dest.writeParcelable(this.mCallSettings.confirmMsg, 0);
                dest.writeParcelable(this.mCallSettings.callMsg, 0);
                break;
            case OPEN_CHANNEL:
                dest.writeInt(this.mChannelSettings.channel);
                dest.writeInt(this.mChannelSettings.protocol.value());
                dest.writeInt(this.mChannelSettings.port);
                dest.writeInt(this.mChannelSettings.bufSize);
                if (this.mChannelSettings.destinationAddress != null) {
                    dest.writeInt(this.mChannelSettings.destinationAddress.length);
                    dest.writeByteArray(this.mChannelSettings.destinationAddress);
                } else {
                    dest.writeInt(0);
                }
                dest.writeValue(this.mChannelSettings.bearerDescription);
                dest.writeString(this.mChannelSettings.networkAccessName);
                dest.writeString(this.mChannelSettings.userLogin);
                dest.writeString(this.mChannelSettings.userPassword);
                break;
            case CLOSE_CHANNEL:
            case RECEIVE_DATA:
            case SEND_DATA:
                dest.writeInt(this.mDataSettings.channel);
                dest.writeInt(this.mDataSettings.length);
                int len = 0;
                if (this.mDataSettings.data != null) {
                    len = this.mDataSettings.data.length;
                }
                dest.writeInt(len);
                if (len > 0) {
                    dest.writeByteArray(this.mDataSettings.data);
                }
                break;
            case SET_UP_EVENT_LIST:
                dest.writeIntArray(this.mSetupEventListSettings.eventList);
                break;
            case REFRESH:
                dest.writeInt(this.mRefreshSettings.fileChanged ? 1 : 0);
                dest.writeInt(this.mRefreshSettings.fileList.length);
                dest.writeIntArray(this.mRefreshSettings.fileList);
                break;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public AppInterface.CommandType getCmdType() {
        return AppInterface.CommandType.fromInt(this.mCmdDet.typeOfCommand);
    }

    public int getCommandQualifier() {
        return this.mCmdDet.commandQualifier;
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

    public RefreshSettings getRefreshSettings() {
        return this.mRefreshSettings;
    }

    public DataSettings getDataSettings() {
        return this.mDataSettings;
    }

    public ChannelSettings getChannelSettings() {
        return this.mChannelSettings;
    }

    public SendSSSettings getSendSSSettings() {
        return this.mSendSSSettings;
    }

    public SendUSSDSettings getSendUSSDSettings() {
        return this.mSendUSSDSettings;
    }

    public SendSMSSettings getSendSMSSettings() {
        return this.mSendSMSSettings;
    }
}
