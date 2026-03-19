package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Iterator;
import java.util.List;

class BipCommandParamsFactory extends Handler {

    private static final int[] f14x72eb89a2 = null;
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

    private static int[] m171xe796fd46() {
        if (f14x72eb89a2 != null) {
            return f14x72eb89a2;
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
        f14x72eb89a2 = iArr;
        return iArr;
    }

    static synchronized BipCommandParamsFactory getInstance(BipRilMessageDecoder caller, IccFileHandler fh) {
        if (sInstance != null) {
            return sInstance;
        }
        if (fh == null) {
            return null;
        }
        return new BipCommandParamsFactory(caller, fh);
    }

    private BipCommandParamsFactory(BipRilMessageDecoder caller, IccFileHandler fh) {
        this.mCaller = null;
        this.mCaller = caller;
        this.mIconLoader = BipIconLoader.getInstance(this, fh, this.mCaller.getSlotId());
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> ctlvs) throws ResultException {
        ComprehensionTlv ctlvCmdDet;
        if (ctlvs == null || (ctlvCmdDet = searchForTag(ComprehensionTlvTag.COMMAND_DETAILS, ctlvs)) == null) {
            return null;
        }
        try {
            CommandDetails cmdDet = ValueParser.retrieveCommandDetails(ctlvCmdDet);
            return cmdDet;
        } catch (ResultException e) {
            CatLog.d(this, "Failed to procees command details");
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    void make(BerTlv berTlv) {
        boolean cmdPending;
        if (berTlv == null) {
            return;
        }
        this.mCmdParams = null;
        this.mIconLoadState = 0;
        if (berTlv.getTag() != 208) {
            CatLog.e(this, "CPF-make: Ununderstood proactive command tag");
            sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
            return;
        }
        List<ComprehensionTlv> ctlvs = berTlv.getComprehensionTlvs();
        try {
            CommandDetails cmdDet = processCommandDetails(ctlvs);
            if (cmdDet == null) {
                CatLog.e(this, "CPF-make: No CommandDetails object");
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            AppInterface.CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
            if (cmdType == null) {
                CatLog.d(this, "CPF-make: Command type can't be found");
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                return;
            }
            if (!berTlv.isLengthValid()) {
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                return;
            }
            try {
                switch (m171xe796fd46()[cmdType.ordinal()]) {
                    case 1:
                        cmdPending = processCloseChannel(cmdDet, ctlvs);
                        CatLog.d(this, "process CloseChannel");
                        break;
                    case 2:
                        cmdPending = processGetChannelStatus(cmdDet, ctlvs);
                        CatLog.d(this, "process GetChannelStatus");
                        break;
                    case 3:
                        cmdPending = processOpenChannel(cmdDet, ctlvs);
                        CatLog.d(this, "process OpenChannel");
                        break;
                    case 4:
                        cmdPending = processReceiveData(cmdDet, ctlvs);
                        CatLog.d(this, "process ReceiveData");
                        break;
                    case 5:
                        cmdPending = processSendData(cmdDet, ctlvs);
                        CatLog.d(this, "process SendData");
                        break;
                    case 6:
                        cmdPending = processSetUpEventList(cmdDet, ctlvs);
                        break;
                    default:
                        this.mCmdParams = new CommandParams(cmdDet);
                        CatLog.d(this, "CPF-make: default case");
                        sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                        return;
                }
                if (cmdPending) {
                    return;
                }
                sendCmdParams(ResultCode.OK);
            } catch (ResultException e) {
                CatLog.d(this, "make: caught ResultException e=" + e);
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(e.result());
            }
        } catch (ResultException e2) {
            CatLog.e(this, "CPF-make: Except to procees command details : " + e2.result());
            sendCmdParams(e2.result());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                sendCmdParams(setIcons(msg.obj));
                break;
        }
    }

    private ResultCode setIcons(Object data) {
        if (data == null) {
            return ResultCode.PRFRMD_ICON_NOT_DISPLAYED;
        }
        switch (this.mIconLoadState) {
            case 1:
                this.mCmdParams.setIcon((Bitmap) data);
                break;
            case 2:
                Bitmap[] icons = (Bitmap[]) data;
                for (Bitmap icon : icons) {
                    this.mCmdParams.setIcon(icon);
                }
                break;
        }
        return ResultCode.OK;
    }

    private void sendCmdParams(ResultCode resCode) {
        this.mCaller.sendMsgParamsDecoded(resCode, this.mCmdParams);
    }

    private ComprehensionTlv searchForTag(ComprehensionTlvTag tag, List<ComprehensionTlv> ctlvs) {
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        return searchForNextTag(tag, iter);
    }

    private ComprehensionTlv searchForNextTag(ComprehensionTlvTag tag, Iterator<ComprehensionTlv> iter) {
        int tagValue = tag.value();
        while (iter.hasNext()) {
            ComprehensionTlv ctlv = iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }
        return null;
    }

    private void resetTlvIndex() {
        this.tlvIndex = -1;
    }

    private ComprehensionTlv searchForNextTagAndIndex(ComprehensionTlvTag tag, Iterator<ComprehensionTlv> iter) {
        if (tag == null || iter == null) {
            CatLog.d(this, "CPF-searchForNextTagAndIndex: Invalid params");
            return null;
        }
        int tagValue = tag.value();
        while (iter.hasNext()) {
            this.tlvIndex++;
            ComprehensionTlv ctlv = iter.next();
            if (ctlv.getTag() == tagValue) {
                return ctlv;
            }
        }
        return null;
    }

    private ComprehensionTlv searchForTagAndIndex(ComprehensionTlvTag tag, List<ComprehensionTlv> ctlvs) {
        resetTlvIndex();
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        return searchForNextTagAndIndex(tag, iter);
    }

    private boolean processSetUpEventList(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) {
        CatLog.d(this, "process SetUpEventList");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.EVENT_LIST, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                int[] eventList = new int[valueLen];
                int i = 0;
                while (valueLen > 0) {
                    int eventValue = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
                    valueIndex++;
                    valueLen--;
                    switch (eventValue) {
                        case 4:
                        case 5:
                        case 7:
                        case 8:
                        case 15:
                            eventList[i] = eventValue;
                            i++;
                            break;
                    }
                }
                this.mCmdParams = new SetEventListParams(cmdDet, eventList);
            } catch (IndexOutOfBoundsException e) {
                CatLog.e(this, " IndexOutofBoundException in processSetUpEventList");
            }
        }
        return false;
    }

    private boolean processOpenChannel(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        CatLog.d(this, "enter: process OpenChannel");
        ?? r4 = 0;
        if ((commandDetails.commandQualifier & 1) == 1) {
        }
        if ((commandDetails.commandQualifier & 2) == 0) {
        }
        String strRetrieveNetworkAccessName = null;
        OtherAddress otherAddressRetrieveOtherAddress = null;
        String strRetrieveTextString = null;
        String strRetrieveTextString2 = null;
        TransportProtocol transportProtocolRetrieveTransportProtocol = null;
        OtherAddress otherAddressRetrieveOtherAddress2 = null;
        TextMessage textMessage = new TextMessage();
        IconId iconIdRetrieveIconId = null;
        int i = -1;
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = ValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            this.mIconLoadState = 1;
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.BEARER_DESCRIPTION, list);
        if (comprehensionTlvSearchForTag3 != null) {
            ?? RetrieveBearerDesc = BipValueParser.retrieveBearerDesc(comprehensionTlvSearchForTag3);
            CatLog.d("[BIP]", "bearerDesc bearer type: " + RetrieveBearerDesc.bearerType);
            if (RetrieveBearerDesc instanceof GPRSBearerDesc) {
                CatLog.d("[BIP]", "\nprecedence: " + RetrieveBearerDesc.precedence + "\ndelay: " + RetrieveBearerDesc.delay + "\nreliability: " + RetrieveBearerDesc.reliability + "\npeak: " + RetrieveBearerDesc.peak + "\nmean: " + RetrieveBearerDesc.mean + "\npdp type: " + RetrieveBearerDesc.pdpType);
                r4 = RetrieveBearerDesc;
            } else if (RetrieveBearerDesc instanceof UTranBearerDesc) {
                CatLog.d("[BIP]", "\ntrafficClass: " + RetrieveBearerDesc.trafficClass + "\nmaxBitRateUL_High: " + RetrieveBearerDesc.maxBitRateUL_High + "\nmaxBitRateUL_Low: " + RetrieveBearerDesc.maxBitRateUL_Low + "\nmaxBitRateDL_High: " + RetrieveBearerDesc.maxBitRateDL_High + "\nmaxBitRateUL_Low: " + RetrieveBearerDesc.maxBitRateDL_Low + "\nguarBitRateUL_High: " + RetrieveBearerDesc.guarBitRateUL_High + "\nguarBitRateUL_Low: " + RetrieveBearerDesc.guarBitRateUL_Low + "\nguarBitRateDL_High: " + RetrieveBearerDesc.guarBitRateDL_High + "\nguarBitRateDL_Low: " + RetrieveBearerDesc.guarBitRateDL_Low + "\ndeliveryOrder: " + RetrieveBearerDesc.deliveryOrder + "\nmaxSduSize: " + RetrieveBearerDesc.maxSduSize + "\nsduErrorRatio: " + RetrieveBearerDesc.sduErrorRatio + "\nresidualBitErrorRadio: " + RetrieveBearerDesc.residualBitErrorRadio + "\ndeliveryOfErroneousSdus: " + RetrieveBearerDesc.deliveryOfErroneousSdus + "\ntransferDelay: " + RetrieveBearerDesc.transferDelay + "\ntrafficHandlingPriority: " + RetrieveBearerDesc.trafficHandlingPriority + "\npdp type: " + RetrieveBearerDesc.pdpType);
                r4 = RetrieveBearerDesc;
            } else if (RetrieveBearerDesc instanceof EUTranBearerDesc) {
                CatLog.d("[BIP]", "\nQCI: " + RetrieveBearerDesc.QCI + "\nmaxBitRateU: " + RetrieveBearerDesc.maxBitRateU + "\nmaxBitRateD: " + RetrieveBearerDesc.maxBitRateD + "\nguarBitRateU: " + RetrieveBearerDesc.guarBitRateU + "\nguarBitRateD: " + RetrieveBearerDesc.guarBitRateD + "\nmaxBitRateUEx: " + RetrieveBearerDesc.maxBitRateUEx + "\nmaxBitRateDEx: " + RetrieveBearerDesc.maxBitRateDEx + "\nguarBitRateUEx: " + RetrieveBearerDesc.guarBitRateUEx + "\nguarBitRateDEx: " + RetrieveBearerDesc.guarBitRateDEx + "\npdn Type: " + RetrieveBearerDesc.pdnType);
                r4 = RetrieveBearerDesc;
            } else {
                boolean z = RetrieveBearerDesc instanceof DefaultBearerDesc;
                r4 = RetrieveBearerDesc;
                if (!z) {
                    CatLog.d("[BIP]", "Not support bearerDesc");
                    r4 = RetrieveBearerDesc;
                }
            }
        } else {
            CatLog.d("[BIP]", "May Need BearerDescription object");
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.BUFFER_SIZE, list);
        if (comprehensionTlvSearchForTag4 == null) {
            CatLog.d("[BIP]", "Need BufferSize object");
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        int iRetrieveBufferSize = BipValueParser.retrieveBufferSize(comprehensionTlvSearchForTag4);
        CatLog.d("[BIP]", "buffer size: " + iRetrieveBufferSize);
        ComprehensionTlv comprehensionTlvSearchForTag5 = searchForTag(ComprehensionTlvTag.NETWORK_ACCESS_NAME, list);
        if (comprehensionTlvSearchForTag5 != null) {
            strRetrieveNetworkAccessName = BipValueParser.retrieveNetworkAccessName(comprehensionTlvSearchForTag5);
            CatLog.d("[BIP]", "access point name: " + strRetrieveNetworkAccessName);
        }
        Iterator<ComprehensionTlv> it = list.iterator();
        ComprehensionTlv comprehensionTlvSearchForNextTag = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, it);
        if (comprehensionTlvSearchForNextTag != null) {
            strRetrieveTextString = ValueParser.retrieveTextString(comprehensionTlvSearchForNextTag);
            CatLog.d("[BIP]", "user login: " + strRetrieveTextString);
        }
        ComprehensionTlv comprehensionTlvSearchForNextTag2 = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, it);
        if (comprehensionTlvSearchForNextTag2 != null) {
            strRetrieveTextString2 = ValueParser.retrieveTextString(comprehensionTlvSearchForNextTag2);
            CatLog.d("[BIP]", "user password: " + strRetrieveTextString2);
        }
        ComprehensionTlv comprehensionTlvSearchForTagAndIndex = searchForTagAndIndex(ComprehensionTlvTag.SIM_ME_INTERFACE_TRANSPORT_LEVEL, list);
        if (comprehensionTlvSearchForTagAndIndex != null) {
            i = this.tlvIndex;
            CatLog.d("[BIP]", "CPF-processOpenChannel: indexTransportProtocol = " + i);
            transportProtocolRetrieveTransportProtocol = BipValueParser.retrieveTransportProtocol(comprehensionTlvSearchForTagAndIndex);
            CatLog.d("[BIP]", "CPF-processOpenChannel: transport protocol(type/port): " + transportProtocolRetrieveTransportProtocol.protocolType + "/" + transportProtocolRetrieveTransportProtocol.portNumber);
            if ((1 == transportProtocolRetrieveTransportProtocol.protocolType || 2 == transportProtocolRetrieveTransportProtocol.protocolType) && r4 == 0) {
                CatLog.d("[BIP]", "Need BearerDescription object");
                throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
            }
        } else if (r4 == 0) {
            CatLog.d("[BIP]", "BearerDescription & transportProtocol object are null");
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        if (transportProtocolRetrieveTransportProtocol != null) {
            CatLog.d("[BIP]", "CPF-processOpenChannel: transport protocol is existed");
            Iterator<ComprehensionTlv> it2 = list.iterator();
            resetTlvIndex();
            ComprehensionTlv comprehensionTlvSearchForNextTagAndIndex = searchForNextTagAndIndex(ComprehensionTlvTag.OTHER_ADDRESS, it2);
            if (comprehensionTlvSearchForNextTagAndIndex == null) {
                CatLog.d("[BIP]", "CPF-processOpenChannel: No other address object");
            } else if (this.tlvIndex < i) {
                CatLog.d("[BIP]", "CPF-processOpenChannel: get local address, index is " + this.tlvIndex);
                otherAddressRetrieveOtherAddress = BipValueParser.retrieveOtherAddress(comprehensionTlvSearchForNextTagAndIndex);
                ComprehensionTlv comprehensionTlvSearchForNextTagAndIndex2 = searchForNextTagAndIndex(ComprehensionTlvTag.OTHER_ADDRESS, it2);
                if (comprehensionTlvSearchForNextTagAndIndex2 == null || this.tlvIndex <= i) {
                    CatLog.d("[BIP]", "CPF-processOpenChannel: missing dest address " + this.tlvIndex + "/" + i);
                    throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                }
                CatLog.d("[BIP]", "CPF-processOpenChannel: get dest address, index is " + this.tlvIndex);
                otherAddressRetrieveOtherAddress2 = BipValueParser.retrieveOtherAddress(comprehensionTlvSearchForNextTagAndIndex2);
            } else if (this.tlvIndex > i) {
                CatLog.d("[BIP]", "CPF-processOpenChannel: get dest address, but no local address");
                otherAddressRetrieveOtherAddress2 = BipValueParser.retrieveOtherAddress(comprehensionTlvSearchForNextTagAndIndex);
            } else {
                CatLog.d("[BIP]", "CPF-processOpenChannel: Incorrect index");
            }
            if (otherAddressRetrieveOtherAddress2 == null && (2 == transportProtocolRetrieveTransportProtocol.protocolType || 1 == transportProtocolRetrieveTransportProtocol.protocolType)) {
                CatLog.d("[BIP]", "BM-openChannel: dataDestinationAddress is null.");
                throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
            }
        }
        if (r4 != 0) {
            if (r4.bearerType == 2 || r4.bearerType == 3 || r4.bearerType == 9 || r4.bearerType == 11) {
                this.mCmdParams = new OpenChannelParams(commandDetails, r4, iRetrieveBufferSize, otherAddressRetrieveOtherAddress, transportProtocolRetrieveTransportProtocol, otherAddressRetrieveOtherAddress2, strRetrieveNetworkAccessName, strRetrieveTextString, strRetrieveTextString2, textMessage);
            } else {
                CatLog.d("[BIP]", "Unsupport bearerType: " + r4.bearerType);
            }
        }
        this.mCmdParams = new OpenChannelParams(commandDetails, r4, iRetrieveBufferSize, otherAddressRetrieveOtherAddress, transportProtocolRetrieveTransportProtocol, otherAddressRetrieveOtherAddress2, strRetrieveNetworkAccessName, strRetrieveTextString, strRetrieveTextString2, textMessage);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processCloseChannel(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "enter: process CloseChannel");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        int channelId = 0;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv3 != null) {
            byte cidByte = ctlv3.getRawValue()[ctlv3.getValueIndex() + 1];
            channelId = cidByte & 15;
            CatLog.d("[BIP]", "To close channel " + channelId);
        }
        boolean backToTcpListen = 1 == (cmdDet.commandQualifier & 1);
        this.mCmdParams = new CloseChannelParams(cmdDet, channelId, textMsg, backToTcpListen);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processReceiveData(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "enter: process ReceiveData");
        int channelDataLength = 0;
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        int channelId = 0;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.CHANNEL_DATA_LENGTH, ctlvs);
        if (ctlv != null) {
            channelDataLength = BipValueParser.retrieveChannelDataLength(ctlv);
            CatLog.d("[BIP]", "Channel data length: " + channelDataLength);
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv2 != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv2);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv3 != null) {
            iconId = ValueParser.retrieveIconId(ctlv3);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv4 != null) {
            byte cidByte = ctlv4.getRawValue()[ctlv4.getValueIndex() + 1];
            channelId = cidByte & 15;
            CatLog.d("[BIP]", "To Receive data: " + channelId);
        }
        this.mCmdParams = new ReceiveDataParams(cmdDet, channelDataLength, channelId, textMsg);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processSendData(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "enter: process SendData");
        byte[] channelData = null;
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        int channelId = 0;
        int sendMode = (cmdDet.commandQualifier & 1) == 1 ? 1 : 0;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.CHANNEL_DATA, ctlvs);
        if (ctlv != null) {
            channelData = BipValueParser.retrieveChannelData(ctlv);
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv2 != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv2);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv3 != null) {
            iconId = ValueParser.retrieveIconId(ctlv3);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv4 != null) {
            byte cidByte = ctlv4.getRawValue()[ctlv4.getValueIndex() + 1];
            channelId = cidByte & 15;
            CatLog.d("[BIP]", "To send data: " + channelId);
        }
        this.mCmdParams = new SendDataParams(cmdDet, channelData, channelId, textMsg, sendMode);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processGetChannelStatus(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "enter: process GetChannelStatus");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        this.mCmdParams = new GetChannelStatusParams(cmdDet, textMsg);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
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
