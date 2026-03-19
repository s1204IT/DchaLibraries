package com.mediatek.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.SetEventListParams;
import com.android.internal.telephony.cat.TextMessage;
import java.util.List;

public class BipCmdMessage implements Parcelable {
    public static final Parcelable.Creator<BipCmdMessage> CREATOR = new Parcelable.Creator<BipCmdMessage>() {
        @Override
        public BipCmdMessage createFromParcel(Parcel parcel) {
            return new BipCmdMessage(parcel);
        }

        @Override
        public BipCmdMessage[] newArray(int i) {
            return new BipCmdMessage[i];
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

    public class SetupEventListSettings {
        public int[] eventList;

        public SetupEventListSettings() {
        }
    }

    BipCmdMessage(CommandParams commandParams) {
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
        this.mCmdDet = commandParams.mCmdDet;
        switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()]) {
            case 1:
                this.mTextMsg = ((GetChannelStatusParams) commandParams).textMsg;
                break;
            case 2:
                OpenChannelParams openChannelParams = (OpenChannelParams) commandParams;
                this.mBearerDesc = openChannelParams.bearerDesc;
                this.mBufferSize = openChannelParams.bufferSize;
                this.mLocalAddress = openChannelParams.localAddress;
                this.mTransportProtocol = openChannelParams.transportProtocol;
                this.mDataDestinationAddress = openChannelParams.dataDestinationAddress;
                this.mTextMsg = openChannelParams.textMsg;
                if (this.mBearerDesc != null) {
                    if (this.mBearerDesc.bearerType == 2 || this.mBearerDesc.bearerType == 3 || this.mBearerDesc.bearerType == 9 || this.mBearerDesc.bearerType == 11) {
                        this.mApn = openChannelParams.gprsParams.accessPointName;
                        this.mLogin = openChannelParams.gprsParams.userLogin;
                        this.mPwd = openChannelParams.gprsParams.userPwd;
                    }
                } else {
                    MtkCatLog.d("[BIP]", "Invalid BearerDesc object");
                }
                break;
            case 3:
                CloseChannelParams closeChannelParams = (CloseChannelParams) commandParams;
                this.mTextMsg = closeChannelParams.textMsg;
                this.mCloseCid = closeChannelParams.mCloseCid;
                this.mCloseBackToTcpListen = closeChannelParams.mBackToTcpListen;
                break;
            case 4:
                ReceiveDataParams receiveDataParams = (ReceiveDataParams) commandParams;
                this.mTextMsg = receiveDataParams.textMsg;
                this.mChannelDataLength = receiveDataParams.channelDataLength;
                this.mReceiveDataCid = receiveDataParams.mReceiveDataCid;
                break;
            case 5:
                SendDataParams sendDataParams = (SendDataParams) commandParams;
                this.mTextMsg = sendDataParams.textMsg;
                this.mChannelData = sendDataParams.channelData;
                this.mSendDataCid = sendDataParams.mSendDataCid;
                this.mSendMode = sendDataParams.mSendMode;
                break;
            case 6:
                this.mSetupEventListSettings = new SetupEventListSettings();
                this.mSetupEventListSettings.eventList = ((SetEventListParams) commandParams).mEventInfo;
                break;
        }
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType = new int[AppInterface.CommandType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_DATA.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    public BipCmdMessage(Parcel parcel) {
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
        this.mCmdDet = parcel.readParcelable(null);
        this.mTextMsg = parcel.readParcelable(null);
        int i = AnonymousClass2.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()];
        if (i == 2) {
            this.mBearerDesc = (BearerDesc) parcel.readParcelable(null);
            return;
        }
        if (i == 6) {
            this.mSetupEventListSettings = new SetupEventListSettings();
            int i2 = parcel.readInt();
            this.mSetupEventListSettings.eventList = new int[i2];
            for (int i3 = 0; i3 < i2; i3++) {
                this.mSetupEventListSettings.eventList[i3] = parcel.readInt();
            }
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mCmdDet, 0);
        parcel.writeParcelable(this.mTextMsg, 0);
        int i2 = AnonymousClass2.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[getCmdType().ordinal()];
        if (i2 == 2) {
            parcel.writeParcelable(this.mBearerDesc, 0);
        } else if (i2 == 6) {
            parcel.writeIntArray(this.mSetupEventListSettings.eventList);
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
