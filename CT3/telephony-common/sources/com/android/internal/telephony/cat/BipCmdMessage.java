package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.cat.AppInterface;
import java.util.List;

public class BipCmdMessage implements Parcelable {

    private static final int[] f13x72eb89a2 = null;
    public static final Parcelable.Creator<BipCmdMessage> CREATOR = new Parcelable.Creator<BipCmdMessage>() {
        @Override
        public BipCmdMessage createFromParcel(Parcel in) {
            return new BipCmdMessage(in);
        }

        @Override
        public BipCmdMessage[] newArray(int size) {
            return new BipCmdMessage[size];
        }
    };
    public String mApn;
    public BearerDesc mBearerDesc;
    public int mBufferSize;
    public byte[] mChannelData;
    public int mChannelDataLength;
    public ChannelStatus mChannelStatusData;
    public List<ChannelStatus> mChannelStatusList;
    public boolean mCloseBackToTcpListen;
    public int mCloseCid;
    CommandDetails mCmdDet;
    public OtherAddress mDataDestinationAddress;
    public String mDestAddress;
    public DnsServerAddress mDnsServerAddress;
    public int mInfoType;
    public OtherAddress mLocalAddress;
    public String mLogin;
    public String mPwd;
    public int mReceiveDataCid;
    public int mRemainingDataLength;
    public int mSendDataCid;
    public int mSendMode;
    private SetupEventListSettings mSetupEventListSettings;
    private TextMessage mTextMsg;
    public TransportProtocol mTransportProtocol;

    private static int[] m170xe796fd46() {
        if (f13x72eb89a2 != null) {
            return f13x72eb89a2;
        }
        int[] iArr = new int[AppInterface.CommandType.valuesCustom().length];
        try {
            iArr[AppInterface.CommandType.ACTIVATE.ordinal()] = 7;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[AppInterface.CommandType.CALLCTRL_RSP_MSG.ordinal()] = 8;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[AppInterface.CommandType.DECLARE_SERVICE.ordinal()] = 9;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_MULTIMEDIA_MESSAGE.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_TEXT.ordinal()] = 11;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 2;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[AppInterface.CommandType.GET_FRAME_STATUS.ordinal()] = 12;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INKEY.ordinal()] = 13;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INPUT.ordinal()] = 14;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[AppInterface.CommandType.GET_READER_STATUS.ordinal()] = 15;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[AppInterface.CommandType.GET_SERVICE_INFORMATION.ordinal()] = 16;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[AppInterface.CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 17;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[AppInterface.CommandType.LAUNCH_BROWSER.ordinal()] = 18;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[AppInterface.CommandType.MORE_TIME.ordinal()] = 19;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 3;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[AppInterface.CommandType.PERFORM_CARD_APDU.ordinal()] = 20;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[AppInterface.CommandType.PLAY_TONE.ordinal()] = 21;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[AppInterface.CommandType.POLLING_OFF.ordinal()] = 22;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[AppInterface.CommandType.POLL_INTERVAL.ordinal()] = 23;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_OFF_CARD.ordinal()] = 24;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_ON_CARD.ordinal()] = 25;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.ordinal()] = 26;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 4;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[AppInterface.CommandType.REFRESH.ordinal()] = 27;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[AppInterface.CommandType.RETRIEVE_MULTIMEDIA_MESSAGE.ordinal()] = 28;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[AppInterface.CommandType.RUN_AT_COMMAND.ordinal()] = 29;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[AppInterface.CommandType.SELECT_ITEM.ordinal()] = 30;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DATA.ordinal()] = 5;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DTMF.ordinal()] = 31;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SMS.ordinal()] = 32;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SS.ordinal()] = 33;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_USSD.ordinal()] = 34;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[AppInterface.CommandType.SERVICE_SEARCH.ordinal()] = 35;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[AppInterface.CommandType.SET_FRAME.ordinal()] = 36;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_CALL.ordinal()] = 37;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 6;
        } catch (NoSuchFieldError e37) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 38;
        } catch (NoSuchFieldError e38) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_MENU.ordinal()] = 39;
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
        f13x72eb89a2 = iArr;
        return iArr;
    }

    public class SetupEventListSettings {
        public int[] eventList;

        public SetupEventListSettings() {
        }
    }

    BipCmdMessage(CommandParams cmdParams) {
        this.mBearerDesc = null;
        this.mBufferSize = 0;
        this.mLocalAddress = null;
        this.mDnsServerAddress = null;
        this.mTransportProtocol = null;
        this.mDataDestinationAddress = null;
        this.mApn = null;
        this.mLogin = null;
        this.mPwd = null;
        this.mChannelDataLength = 0;
        this.mRemainingDataLength = 0;
        this.mChannelData = null;
        this.mChannelStatusData = null;
        this.mCloseCid = 0;
        this.mSendDataCid = 0;
        this.mReceiveDataCid = 0;
        this.mCloseBackToTcpListen = false;
        this.mSendMode = 0;
        this.mChannelStatusList = null;
        this.mInfoType = 0;
        this.mDestAddress = null;
        this.mSetupEventListSettings = null;
        this.mCmdDet = cmdParams.mCmdDet;
        switch (m170xe796fd46()[getCmdType().ordinal()]) {
            case 1:
                this.mTextMsg = ((CloseChannelParams) cmdParams).textMsg;
                this.mCloseCid = ((CloseChannelParams) cmdParams).mCloseCid;
                this.mCloseBackToTcpListen = ((CloseChannelParams) cmdParams).mBackToTcpListen;
                break;
            case 2:
                this.mTextMsg = ((GetChannelStatusParams) cmdParams).textMsg;
                break;
            case 3:
                this.mBearerDesc = ((OpenChannelParams) cmdParams).bearerDesc;
                this.mBufferSize = ((OpenChannelParams) cmdParams).bufferSize;
                this.mLocalAddress = ((OpenChannelParams) cmdParams).localAddress;
                this.mTransportProtocol = ((OpenChannelParams) cmdParams).transportProtocol;
                this.mDataDestinationAddress = ((OpenChannelParams) cmdParams).dataDestinationAddress;
                this.mTextMsg = ((OpenChannelParams) cmdParams).textMsg;
                if (this.mBearerDesc != null) {
                    if (this.mBearerDesc.bearerType == 2 || this.mBearerDesc.bearerType == 3 || this.mBearerDesc.bearerType == 9 || this.mBearerDesc.bearerType == 11) {
                        this.mApn = ((OpenChannelParams) cmdParams).gprsParams.accessPointName;
                        this.mLogin = ((OpenChannelParams) cmdParams).gprsParams.userLogin;
                        this.mPwd = ((OpenChannelParams) cmdParams).gprsParams.userPwd;
                    }
                } else {
                    CatLog.d("[BIP]", "Invalid BearerDesc object");
                }
                break;
            case 4:
                this.mTextMsg = ((ReceiveDataParams) cmdParams).textMsg;
                this.mChannelDataLength = ((ReceiveDataParams) cmdParams).channelDataLength;
                this.mReceiveDataCid = ((ReceiveDataParams) cmdParams).mReceiveDataCid;
                break;
            case 5:
                this.mTextMsg = ((SendDataParams) cmdParams).textMsg;
                this.mChannelData = ((SendDataParams) cmdParams).channelData;
                this.mSendDataCid = ((SendDataParams) cmdParams).mSendDataCid;
                this.mSendMode = ((SendDataParams) cmdParams).mSendMode;
                break;
            case 6:
                this.mSetupEventListSettings = new SetupEventListSettings();
                this.mSetupEventListSettings.eventList = ((SetEventListParams) cmdParams).mEventInfo;
                break;
        }
    }

    public BipCmdMessage(Parcel in) {
        this.mBearerDesc = null;
        this.mBufferSize = 0;
        this.mLocalAddress = null;
        this.mDnsServerAddress = null;
        this.mTransportProtocol = null;
        this.mDataDestinationAddress = null;
        this.mApn = null;
        this.mLogin = null;
        this.mPwd = null;
        this.mChannelDataLength = 0;
        this.mRemainingDataLength = 0;
        this.mChannelData = null;
        this.mChannelStatusData = null;
        this.mCloseCid = 0;
        this.mSendDataCid = 0;
        this.mReceiveDataCid = 0;
        this.mCloseBackToTcpListen = false;
        this.mSendMode = 0;
        this.mChannelStatusList = null;
        this.mInfoType = 0;
        this.mDestAddress = null;
        this.mSetupEventListSettings = null;
        this.mCmdDet = (CommandDetails) in.readParcelable(null);
        this.mTextMsg = (TextMessage) in.readParcelable(null);
        switch (m170xe796fd46()[getCmdType().ordinal()]) {
            case 3:
                this.mBearerDesc = (BearerDesc) in.readParcelable(null);
                break;
            case 6:
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
        switch (m170xe796fd46()[getCmdType().ordinal()]) {
            case 3:
                dest.writeParcelable(this.mBearerDesc, 0);
                break;
            case 6:
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

    public BearerDesc getBearerDesc() {
        return this.mBearerDesc;
    }
}
