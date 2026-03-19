package com.mediatek.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.BerTlv;
import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.ComprehensionTlv;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.IconId;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.ResultException;
import com.android.internal.telephony.cat.SetEventListParams;
import com.android.internal.telephony.cat.TextMessage;
import com.android.internal.telephony.cat.ValueParser;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Iterator;
import java.util.List;

class BipCommandParamsFactory extends Handler {
    static final int LOAD_MULTI_ICONS = 2;
    static final int LOAD_NO_ICON = 0;
    static final int LOAD_SINGLE_ICON = 1;
    static final int MSG_ID_LOAD_ICON_DONE = 1;
    private static BipCommandParamsFactory sInstance = null;
    private BipRilMessageDecoder mCaller;
    private BipIconLoader mIconLoader;
    private CommandParams mCmdParams = null;
    private int mIconLoadState = 0;
    int tlvIndex = -1;

    static synchronized BipCommandParamsFactory getInstance(BipRilMessageDecoder bipRilMessageDecoder, IccFileHandler iccFileHandler) {
        if (sInstance != null) {
            return sInstance;
        }
        if (iccFileHandler != null) {
            return new BipCommandParamsFactory(bipRilMessageDecoder, iccFileHandler);
        }
        return null;
    }

    private BipCommandParamsFactory(BipRilMessageDecoder bipRilMessageDecoder, IccFileHandler iccFileHandler) {
        this.mCaller = null;
        this.mCaller = bipRilMessageDecoder;
        this.mIconLoader = BipIconLoader.getInstance(this, iccFileHandler, this.mCaller.getSlotId());
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> list) throws ResultException {
        ComprehensionTlv comprehensionTlvSearchForTag;
        if (list != null && (comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.COMMAND_DETAILS, list)) != null) {
            try {
                return ValueParser.retrieveCommandDetails(comprehensionTlvSearchForTag);
            } catch (ResultException e) {
                MtkCatLog.d(this, "Failed to procees command details");
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return null;
    }

    void make(BerTlv berTlv) {
        boolean zProcessSetUpEventList;
        if (berTlv == null) {
            return;
        }
        this.mCmdParams = null;
        this.mIconLoadState = 0;
        if (berTlv.getTag() != 208) {
            MtkCatLog.e(this, "CPF-make: Ununderstood proactive command tag");
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }
        List<ComprehensionTlv> comprehensionTlvs = berTlv.getComprehensionTlvs();
        try {
            CommandDetails commandDetailsProcessCommandDetails = processCommandDetails(comprehensionTlvs);
            if (commandDetailsProcessCommandDetails == null) {
                MtkCatLog.e(this, "CPF-make: No CommandDetails object");
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(commandDetailsProcessCommandDetails.typeOfCommand);
            if (commandTypeFromInt == null) {
                MtkCatLog.d(this, "CPF-make: Command type can't be found");
                this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
                sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                return;
            }
            if (!berTlv.isLengthValid()) {
                this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
                sendCmdParams(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                return;
            }
            try {
                switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandTypeFromInt.ordinal()]) {
                    case 1:
                        zProcessSetUpEventList = processSetUpEventList(commandDetailsProcessCommandDetails, comprehensionTlvs);
                        break;
                    case 2:
                        zProcessSetUpEventList = processOpenChannel(commandDetailsProcessCommandDetails, comprehensionTlvs);
                        MtkCatLog.d(this, "process OpenChannel");
                        break;
                    case 3:
                        zProcessSetUpEventList = processCloseChannel(commandDetailsProcessCommandDetails, comprehensionTlvs);
                        MtkCatLog.d(this, "process CloseChannel");
                        break;
                    case 4:
                        zProcessSetUpEventList = processSendData(commandDetailsProcessCommandDetails, comprehensionTlvs);
                        MtkCatLog.d(this, "process SendData");
                        break;
                    case 5:
                        zProcessSetUpEventList = processReceiveData(commandDetailsProcessCommandDetails, comprehensionTlvs);
                        MtkCatLog.d(this, "process ReceiveData");
                        break;
                    case 6:
                        zProcessSetUpEventList = processGetChannelStatus(commandDetailsProcessCommandDetails, comprehensionTlvs);
                        MtkCatLog.d(this, "process GetChannelStatus");
                        break;
                    default:
                        this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
                        MtkCatLog.d(this, "CPF-make: default case");
                        sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                        return;
                }
                if (!zProcessSetUpEventList) {
                    sendCmdParams(ResultCode.OK);
                }
            } catch (ResultException e) {
                MtkCatLog.d(this, "make: caught ResultException e=" + e);
                this.mCmdParams = new CommandParams(commandDetailsProcessCommandDetails);
                sendCmdParams(e.result());
            }
        } catch (ResultException e2) {
            MtkCatLog.e(this, "CPF-make: Except to procees command details : " + e2.result());
            sendCmdParams(e2.result());
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType = new int[AppInterface.CommandType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 1;
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
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_DATA.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1) {
            sendCmdParams(setIcons(message.obj));
        }
    }

    private ResultCode setIcons(Object obj) {
        if (obj == null) {
            return ResultCode.PRFRMD_ICON_NOT_DISPLAYED;
        }
        switch (this.mIconLoadState) {
            case 2:
                for (Bitmap bitmap : (Bitmap[]) obj) {
                }
                break;
        }
        return ResultCode.OK;
    }

    private void sendCmdParams(ResultCode resultCode) {
        this.mCaller.sendMsgParamsDecoded(resultCode, this.mCmdParams);
    }

    private ComprehensionTlv searchForTag(ComprehensionTlvTag comprehensionTlvTag, List<ComprehensionTlv> list) {
        return searchForNextTag(comprehensionTlvTag, list.iterator());
    }

    private ComprehensionTlv searchForNextTag(ComprehensionTlvTag comprehensionTlvTag, Iterator<ComprehensionTlv> it) {
        int iValue = comprehensionTlvTag.value();
        while (it.hasNext()) {
            ComprehensionTlv next = it.next();
            if (next.getTag() == iValue) {
                return next;
            }
        }
        return null;
    }

    private void resetTlvIndex() {
        this.tlvIndex = -1;
    }

    private ComprehensionTlv searchForNextTagAndIndex(ComprehensionTlvTag comprehensionTlvTag, Iterator<ComprehensionTlv> it) {
        if (comprehensionTlvTag == null || it == null) {
            MtkCatLog.d(this, "CPF-searchForNextTagAndIndex: Invalid params");
            return null;
        }
        int iValue = comprehensionTlvTag.value();
        while (it.hasNext()) {
            this.tlvIndex++;
            ComprehensionTlv next = it.next();
            if (next.getTag() == iValue) {
                return next;
            }
        }
        return null;
    }

    private ComprehensionTlv searchForTagAndIndex(ComprehensionTlvTag comprehensionTlvTag, List<ComprehensionTlv> list) {
        resetTlvIndex();
        return searchForNextTagAndIndex(comprehensionTlvTag, list.iterator());
    }

    private boolean processSetUpEventList(CommandDetails commandDetails, List<ComprehensionTlv> list) {
        MtkCatLog.d(this, "process SetUpEventList");
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.EVENT_LIST, list);
        if (comprehensionTlvSearchForTag != null) {
            try {
                byte[] rawValue = comprehensionTlvSearchForTag.getRawValue();
                int valueIndex = comprehensionTlvSearchForTag.getValueIndex();
                int length = comprehensionTlvSearchForTag.getLength();
                int[] iArr = new int[length];
                int i = 0;
                while (length > 0) {
                    int i2 = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
                    valueIndex++;
                    length--;
                    switch (i2) {
                        case 4:
                        case 5:
                        case 7:
                        case 8:
                        case 15:
                            iArr[i] = i2;
                            i++;
                            break;
                    }
                }
                this.mCmdParams = new SetEventListParams(commandDetails, iArr);
            } catch (IndexOutOfBoundsException e) {
                MtkCatLog.e(this, " IndexOutofBoundException in processSetUpEventList");
            }
        }
        return false;
    }

    private boolean processOpenChannel(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconId;
        BearerDesc bearerDesc;
        String str;
        String str2;
        String str3;
        TransportProtocol transportProtocol;
        OtherAddress otherAddress;
        OtherAddress otherAddress2;
        TransportProtocol transportProtocol2;
        int i;
        BearerDesc bearerDesc2;
        OtherAddress otherAddressRetrieveOtherAddress;
        MtkCatLog.d(this, "enter: process OpenChannel");
        if ((commandDetails.commandQualifier & 1) == 1) {
        }
        int i2 = commandDetails.commandQualifier & 2;
        TextMessage textMessage = new TextMessage();
        int i3 = -1;
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        OtherAddress otherAddressRetrieveOtherAddress2 = null;
        if (comprehensionTlvSearchForTag2 == null) {
            iconId = null;
        } else {
            this.mIconLoadState = 1;
            IconId iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
            iconId = iconIdRetrieveIconId;
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.BEARER_DESCRIPTION, list);
        if (comprehensionTlvSearchForTag3 != null) {
            BearerDesc bearerDescRetrieveBearerDesc = BipValueParser.retrieveBearerDesc(comprehensionTlvSearchForTag3);
            MtkCatLog.d("[BIP]", "bearerDesc bearer type: " + bearerDescRetrieveBearerDesc.bearerType);
            if (bearerDescRetrieveBearerDesc instanceof GPRSBearerDesc) {
                StringBuilder sb = new StringBuilder();
                sb.append("\nprecedence: ");
                GPRSBearerDesc gPRSBearerDesc = (GPRSBearerDesc) bearerDescRetrieveBearerDesc;
                sb.append(gPRSBearerDesc.precedence);
                sb.append("\ndelay: ");
                sb.append(gPRSBearerDesc.delay);
                sb.append("\nreliability: ");
                sb.append(gPRSBearerDesc.reliability);
                sb.append("\npeak: ");
                sb.append(gPRSBearerDesc.peak);
                sb.append("\nmean: ");
                sb.append(gPRSBearerDesc.mean);
                sb.append("\npdp type: ");
                sb.append(gPRSBearerDesc.pdpType);
                MtkCatLog.d("[BIP]", sb.toString());
            } else if (bearerDescRetrieveBearerDesc instanceof UTranBearerDesc) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("\ntrafficClass: ");
                UTranBearerDesc uTranBearerDesc = (UTranBearerDesc) bearerDescRetrieveBearerDesc;
                sb2.append(uTranBearerDesc.trafficClass);
                sb2.append("\nmaxBitRateUL_High: ");
                sb2.append(uTranBearerDesc.maxBitRateUL_High);
                sb2.append("\nmaxBitRateUL_Low: ");
                sb2.append(uTranBearerDesc.maxBitRateUL_Low);
                sb2.append("\nmaxBitRateDL_High: ");
                sb2.append(uTranBearerDesc.maxBitRateDL_High);
                sb2.append("\nmaxBitRateUL_Low: ");
                sb2.append(uTranBearerDesc.maxBitRateDL_Low);
                sb2.append("\nguarBitRateUL_High: ");
                sb2.append(uTranBearerDesc.guarBitRateUL_High);
                sb2.append("\nguarBitRateUL_Low: ");
                sb2.append(uTranBearerDesc.guarBitRateUL_Low);
                sb2.append("\nguarBitRateDL_High: ");
                sb2.append(uTranBearerDesc.guarBitRateDL_High);
                sb2.append("\nguarBitRateDL_Low: ");
                sb2.append(uTranBearerDesc.guarBitRateDL_Low);
                sb2.append("\ndeliveryOrder: ");
                sb2.append(uTranBearerDesc.deliveryOrder);
                sb2.append("\nmaxSduSize: ");
                sb2.append(uTranBearerDesc.maxSduSize);
                sb2.append("\nsduErrorRatio: ");
                sb2.append(uTranBearerDesc.sduErrorRatio);
                sb2.append("\nresidualBitErrorRadio: ");
                sb2.append(uTranBearerDesc.residualBitErrorRadio);
                sb2.append("\ndeliveryOfErroneousSdus: ");
                sb2.append(uTranBearerDesc.deliveryOfErroneousSdus);
                sb2.append("\ntransferDelay: ");
                sb2.append(uTranBearerDesc.transferDelay);
                sb2.append("\ntrafficHandlingPriority: ");
                sb2.append(uTranBearerDesc.trafficHandlingPriority);
                sb2.append("\npdp type: ");
                sb2.append(uTranBearerDesc.pdpType);
                MtkCatLog.d("[BIP]", sb2.toString());
            } else if (bearerDescRetrieveBearerDesc instanceof EUTranBearerDesc) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append("\nQCI: ");
                EUTranBearerDesc eUTranBearerDesc = (EUTranBearerDesc) bearerDescRetrieveBearerDesc;
                sb3.append(eUTranBearerDesc.QCI);
                sb3.append("\nmaxBitRateU: ");
                sb3.append(eUTranBearerDesc.maxBitRateU);
                sb3.append("\nmaxBitRateD: ");
                sb3.append(eUTranBearerDesc.maxBitRateD);
                sb3.append("\nguarBitRateU: ");
                sb3.append(eUTranBearerDesc.guarBitRateU);
                sb3.append("\nguarBitRateD: ");
                sb3.append(eUTranBearerDesc.guarBitRateD);
                sb3.append("\nmaxBitRateUEx: ");
                sb3.append(eUTranBearerDesc.maxBitRateUEx);
                sb3.append("\nmaxBitRateDEx: ");
                sb3.append(eUTranBearerDesc.maxBitRateDEx);
                sb3.append("\nguarBitRateUEx: ");
                sb3.append(eUTranBearerDesc.guarBitRateUEx);
                sb3.append("\nguarBitRateDEx: ");
                sb3.append(eUTranBearerDesc.guarBitRateDEx);
                sb3.append("\npdn Type: ");
                sb3.append(eUTranBearerDesc.pdnType);
                MtkCatLog.d("[BIP]", sb3.toString());
            } else if (!(bearerDescRetrieveBearerDesc instanceof DefaultBearerDesc)) {
                MtkCatLog.d("[BIP]", "Not support bearerDesc");
            }
            bearerDesc = bearerDescRetrieveBearerDesc;
        } else {
            MtkCatLog.d("[BIP]", "May Need BearerDescription object");
            bearerDesc = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.BUFFER_SIZE, list);
        if (comprehensionTlvSearchForTag4 != null) {
            int iRetrieveBufferSize = BipValueParser.retrieveBufferSize(comprehensionTlvSearchForTag4);
            MtkCatLog.d("[BIP]", "buffer size: " + iRetrieveBufferSize);
            ComprehensionTlv comprehensionTlvSearchForTag5 = searchForTag(ComprehensionTlvTag.NETWORK_ACCESS_NAME, list);
            if (comprehensionTlvSearchForTag5 == null) {
                str = null;
            } else {
                String strRetrieveNetworkAccessName = BipValueParser.retrieveNetworkAccessName(comprehensionTlvSearchForTag5);
                MtkCatLog.d("[BIP]", "access point name: " + strRetrieveNetworkAccessName);
                str = strRetrieveNetworkAccessName;
            }
            Iterator<ComprehensionTlv> it = list.iterator();
            ComprehensionTlv comprehensionTlvSearchForNextTag = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, it);
            if (comprehensionTlvSearchForNextTag == null) {
                str2 = null;
            } else {
                String strRetrieveTextString = ValueParser.retrieveTextString(comprehensionTlvSearchForNextTag);
                MtkCatLog.d("[BIP]", "user login: " + strRetrieveTextString);
                str2 = strRetrieveTextString;
            }
            ComprehensionTlv comprehensionTlvSearchForNextTag2 = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, it);
            if (comprehensionTlvSearchForNextTag2 == null) {
                str3 = null;
            } else {
                String strRetrieveTextString2 = ValueParser.retrieveTextString(comprehensionTlvSearchForNextTag2);
                MtkCatLog.d("[BIP]", "user password: " + strRetrieveTextString2);
                str3 = strRetrieveTextString2;
            }
            ComprehensionTlv comprehensionTlvSearchForTagAndIndex = searchForTagAndIndex(ComprehensionTlvTag.SIM_ME_INTERFACE_TRANSPORT_LEVEL, list);
            if (comprehensionTlvSearchForTagAndIndex != null) {
                i3 = this.tlvIndex;
                MtkCatLog.d("[BIP]", "CPF-processOpenChannel: indexTransportProtocol = " + i3);
                TransportProtocol transportProtocolRetrieveTransportProtocol = BipValueParser.retrieveTransportProtocol(comprehensionTlvSearchForTagAndIndex);
                MtkCatLog.d("[BIP]", "CPF-processOpenChannel: transport protocol(type/port): " + transportProtocolRetrieveTransportProtocol.protocolType + "/" + transportProtocolRetrieveTransportProtocol.portNumber);
                if ((1 == transportProtocolRetrieveTransportProtocol.protocolType || 2 == transportProtocolRetrieveTransportProtocol.protocolType) && bearerDesc == null) {
                    MtkCatLog.d("[BIP]", "Need BearerDescription object");
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                }
                transportProtocol = transportProtocolRetrieveTransportProtocol;
            } else {
                if (bearerDesc == null) {
                    MtkCatLog.d("[BIP]", "BearerDescription & transportProtocol object are null");
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                }
                transportProtocol = null;
            }
            if (transportProtocol == null) {
                otherAddress = null;
                otherAddress2 = null;
            } else {
                MtkCatLog.d("[BIP]", "CPF-processOpenChannel: transport protocol is existed");
                Iterator<ComprehensionTlv> it2 = list.iterator();
                resetTlvIndex();
                ComprehensionTlv comprehensionTlvSearchForNextTagAndIndex = searchForNextTagAndIndex(ComprehensionTlvTag.OTHER_ADDRESS, it2);
                if (comprehensionTlvSearchForNextTagAndIndex != null) {
                    if (this.tlvIndex < i3) {
                        MtkCatLog.d("[BIP]", "CPF-processOpenChannel: get local address, index is " + this.tlvIndex);
                        otherAddressRetrieveOtherAddress2 = BipValueParser.retrieveOtherAddress(comprehensionTlvSearchForNextTagAndIndex);
                        ComprehensionTlv comprehensionTlvSearchForNextTagAndIndex2 = searchForNextTagAndIndex(ComprehensionTlvTag.OTHER_ADDRESS, it2);
                        if (comprehensionTlvSearchForNextTagAndIndex2 != null && this.tlvIndex > i3) {
                            MtkCatLog.d("[BIP]", "CPF-processOpenChannel: get dest address, index is " + this.tlvIndex);
                            otherAddressRetrieveOtherAddress = BipValueParser.retrieveOtherAddress(comprehensionTlvSearchForNextTagAndIndex2);
                        } else {
                            MtkCatLog.d("[BIP]", "CPF-processOpenChannel: missing dest address " + this.tlvIndex + "/" + i3);
                            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                        }
                    } else if (this.tlvIndex > i3) {
                        MtkCatLog.d("[BIP]", "CPF-processOpenChannel: get dest address, but no local address");
                        otherAddressRetrieveOtherAddress = BipValueParser.retrieveOtherAddress(comprehensionTlvSearchForNextTagAndIndex);
                    } else {
                        MtkCatLog.d("[BIP]", "CPF-processOpenChannel: Incorrect index");
                    }
                    if (otherAddressRetrieveOtherAddress != null && (2 == transportProtocol.protocolType || 1 == transportProtocol.protocolType)) {
                        MtkCatLog.d("[BIP]", "BM-openChannel: dataDestinationAddress is null.");
                        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                    }
                    otherAddress2 = otherAddressRetrieveOtherAddress;
                    otherAddress = otherAddressRetrieveOtherAddress2;
                } else {
                    MtkCatLog.d("[BIP]", "CPF-processOpenChannel: No other address object");
                }
                otherAddressRetrieveOtherAddress = null;
                if (otherAddressRetrieveOtherAddress != null) {
                }
                otherAddress2 = otherAddressRetrieveOtherAddress;
                otherAddress = otherAddressRetrieveOtherAddress2;
            }
            if (bearerDesc != null) {
                if (bearerDesc.bearerType == 2 || bearerDesc.bearerType == 3 || bearerDesc.bearerType == 9 || bearerDesc.bearerType == 11) {
                    transportProtocol2 = transportProtocol;
                    i = iRetrieveBufferSize;
                    bearerDesc2 = bearerDesc;
                    this.mCmdParams = new OpenChannelParams(commandDetails, bearerDesc, iRetrieveBufferSize, otherAddress, transportProtocol, otherAddress2, str, str2, str3, textMessage);
                } else {
                    MtkCatLog.d("[BIP]", "Unsupport bearerType: " + bearerDesc.bearerType);
                    transportProtocol2 = transportProtocol;
                    i = iRetrieveBufferSize;
                    bearerDesc2 = bearerDesc;
                }
            } else {
                transportProtocol2 = transportProtocol;
                i = iRetrieveBufferSize;
                bearerDesc2 = bearerDesc;
            }
            this.mCmdParams = new OpenChannelParams(commandDetails, bearerDesc2, i, otherAddress, transportProtocol2, otherAddress2, str, str2, str3, textMessage);
            if (iconId != null) {
                this.mIconLoadState = 1;
                this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                return true;
            }
            return false;
        }
        MtkCatLog.d("[BIP]", "Need BufferSize object");
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processCloseChannel(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        int i;
        MtkCatLog.d(this, "enter: process CloseChannel");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, list);
        if (comprehensionTlvSearchForTag3 != null) {
            i = comprehensionTlvSearchForTag3.getRawValue()[comprehensionTlvSearchForTag3.getValueIndex() + 1] & 15;
            MtkCatLog.d("[BIP]", "To close channel " + i);
        } else {
            i = 0;
        }
        this.mCmdParams = new CloseChannelParams(commandDetails, i, textMessage, 1 == (commandDetails.commandQualifier & 1));
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processReceiveData(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        int iRetrieveChannelDataLength;
        IconId iconIdRetrieveIconId;
        int i;
        MtkCatLog.d(this, "enter: process ReceiveData");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.CHANNEL_DATA_LENGTH, list);
        if (comprehensionTlvSearchForTag != null) {
            iRetrieveChannelDataLength = BipValueParser.retrieveChannelDataLength(comprehensionTlvSearchForTag);
            MtkCatLog.d("[BIP]", "Channel data length: " + iRetrieveChannelDataLength);
        } else {
            iRetrieveChannelDataLength = 0;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag2);
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag3 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag3);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, list);
        if (comprehensionTlvSearchForTag4 != null) {
            i = comprehensionTlvSearchForTag4.getRawValue()[comprehensionTlvSearchForTag4.getValueIndex() + 1] & 15;
            MtkCatLog.d("[BIP]", "To Receive data: " + i);
        } else {
            i = 0;
        }
        this.mCmdParams = new ReceiveDataParams(commandDetails, iRetrieveChannelDataLength, i, textMessage);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSendData(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        byte[] bArrRetrieveChannelData;
        int i;
        MtkCatLog.d(this, "enter: process SendData");
        TextMessage textMessage = new TextMessage();
        int i2 = (commandDetails.commandQualifier & 1) == 1 ? 1 : 0;
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.CHANNEL_DATA, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag == null) {
            bArrRetrieveChannelData = null;
        } else {
            bArrRetrieveChannelData = BipValueParser.retrieveChannelData(comprehensionTlvSearchForTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag2);
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag3 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag3);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        IconId iconId = iconIdRetrieveIconId;
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, list);
        if (comprehensionTlvSearchForTag4 != null) {
            int i3 = comprehensionTlvSearchForTag4.getRawValue()[comprehensionTlvSearchForTag4.getValueIndex() + 1] & 15;
            MtkCatLog.d("[BIP]", "To send data: " + i3);
            i = i3;
        } else {
            i = 0;
        }
        this.mCmdParams = new SendDataParams(commandDetails, bArrRetrieveChannelData, i, textMessage, i2);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processGetChannelStatus(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        MtkCatLog.d(this, "enter: process GetChannelStatus");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        this.mCmdParams = new GetChannelStatusParams(commandDetails, textMessage);
        if (iconIdRetrieveIconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    public void dispose() {
        this.mIconLoader.dispose();
        this.mIconLoader = null;
        this.mCmdParams = null;
        this.mCaller = null;
        synchronized (BipCommandParamsFactory.class) {
            sInstance = null;
        }
    }
}
