package com.android.internal.telephony.cat;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.BearerDescription;
import com.android.internal.telephony.cat.InterfaceTransportLevel;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;

class CommandParamsFactory extends Handler {
    static final int DTTZ_SETTING = 3;
    static final int LANGUAGE_SETTING = 4;
    static final int LOAD_MULTI_ICONS = 2;
    static final int LOAD_NO_ICON = 0;
    static final int LOAD_SINGLE_ICON = 1;
    static final int MSG_ID_LOAD_ICON_DONE = 1;
    static final int NON_SPECIFIC_LANGUAGE_NOTIFICATION = 0;
    static final int REFRESH_APPLICATION_RESET = 5;
    static final int REFRESH_FILE_CHANGE = 1;
    static final int REFRESH_NAA_INIT = 3;
    static final int REFRESH_NAA_INIT_AND_FILE_CHANGE = 2;
    static final int REFRESH_NAA_INIT_AND_FULL_FILE_CHANGE = 0;
    static final int REFRESH_SESSION_RESET = 6;
    static final int REFRESH_STEERING_OF_ROAMING = 7;
    static final int REFRESH_UICC_RESET = 4;
    static final int SMS_PACKING_NOT_REQUIRED = 0;
    static final int SMS_PACKING_REQUIRED = 1;
    static final int SPECIFIC_LANGUAGE_NOTIFICATION = 1;
    private static CommandParamsFactory sInstance = null;
    private RilMessageDecoder mCaller;
    private CommandParams mCmdParams = null;
    private int mIconLoadState = 0;
    private IconLoader mIconLoader;

    static synchronized CommandParamsFactory getInstance(RilMessageDecoder caller, IccFileHandler fh) {
        CommandParamsFactory commandParamsFactory;
        if (sInstance != null) {
            commandParamsFactory = sInstance;
        } else if (fh != null) {
            commandParamsFactory = new CommandParamsFactory(caller, fh);
        } else {
            commandParamsFactory = null;
        }
        return commandParamsFactory;
    }

    private CommandParamsFactory(RilMessageDecoder caller, IccFileHandler fh) {
        this.mCaller = null;
        this.mCaller = caller;
        this.mIconLoader = IconLoader.getInstance(this, fh);
    }

    private CommandDetails processCommandDetails(List<ComprehensionTlv> ctlvs) {
        ComprehensionTlv ctlvCmdDet;
        if (ctlvs == null || (ctlvCmdDet = searchForTag(ComprehensionTlvTag.COMMAND_DETAILS, ctlvs)) == null) {
            return null;
        }
        try {
            CommandDetails cmdDet = ValueParser.retrieveCommandDetails(ctlvCmdDet);
            return cmdDet;
        } catch (ResultException e) {
            CatLog.d(this, "processCommandDetails: Failed to procees command details e=" + e);
            return null;
        }
    }

    private boolean checkTerminalProfileValue(int checkbyte, byte value) {
        String tp;
        UiccCardApplication ca = CatService.getUiccApplication();
        if (ca != null && ca.getType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            tp = SystemProperties.get("ril.stk.usim.profile", "UNKNOWN");
        } else if (ca != null && ca.getType() == IccCardApplicationStatus.AppType.APPTYPE_SIM) {
            tp = SystemProperties.get("ril.stk.sim.profile", "UNKNOWN");
        } else {
            tp = "UNKNOWN";
        }
        if (tp.equals("UNKNOWN")) {
            return false;
        }
        CatLog.d(this, "checkTerminalProfileValue " + tp + ",checkbyte " + checkbyte + " value " + Byte.toHexString(value, true));
        byte[] profile = IccUtils.hexStringToBytes(tp);
        if (checkbyte > profile.length) {
            CatLog.d(this, "checkbyte is outof supported");
            return false;
        }
        byte[] me = new byte[1];
        System.arraycopy(profile, checkbyte - 1, me, 0, 1);
        CatLog.d(this, "tp " + Byte.toHexString(me[0], true));
        CatLog.d(this, "compare result " + (me[0] & value));
        if ((me[0] & value) == value) {
            return true;
        }
        CatLog.d(this, "please check Terminal Profile");
        return false;
    }

    private boolean checkEventTerminalProfileValue(int event) {
        switch (EventList.values()[event]) {
            case USER_ACTIVITY:
                return checkTerminalProfileValue(5, (byte) 32);
            case IDLE_SCREEN_AVAILABLE:
                return checkTerminalProfileValue(5, (byte) 64);
            case BROWER_TERMINATION:
                return checkTerminalProfileValue(6, (byte) 2);
            case LANGUAGE_SELECTION:
                return checkTerminalProfileValue(6, (byte) 1);
            case CALL_CONNECTED:
                return checkTerminalProfileValue(5, (byte) 4);
            case CARD_READER_STATUS:
                return false;
            case DATA_AVAILABLE:
                return checkTerminalProfileValue(6, (byte) 4);
            case CHANNEL_STATUS:
                return checkTerminalProfileValue(6, (byte) 8);
            default:
                return true;
        }
    }

    private boolean checkCmdTerminalProfileValue(AppInterface.CommandType cmdType) {
        if (cmdType == null) {
            return false;
        }
        switch (cmdType) {
            case SET_UP_MENU:
                return true;
            case SELECT_ITEM:
                return true;
            case DISPLAY_TEXT:
                return true;
            case SET_UP_IDLE_MODE_TEXT:
                return true;
            case GET_INKEY:
                return true;
            case GET_INPUT:
                return true;
            case SEND_DTMF:
            case SEND_SMS:
            case SEND_SS:
            case SEND_USSD:
                return true;
            case SET_UP_CALL:
                return true;
            case REFRESH:
                return true;
            case LAUNCH_BROWSER:
                return true;
            case PLAY_TONE:
                return true;
            case SET_UP_EVENT_LIST:
                return true;
            case PROVIDE_LOCAL_INFORMATION:
                return true;
            case LANGUAGE_NOTIFICATION:
                return true;
            case OPEN_CHANNEL:
                return checkTerminalProfileValue(12, (byte) 1);
            case CLOSE_CHANNEL:
                return checkTerminalProfileValue(12, (byte) 2);
            case RECEIVE_DATA:
                return checkTerminalProfileValue(12, (byte) 4);
            case SEND_DATA:
                return checkTerminalProfileValue(12, (byte) 8);
            case GET_CHANNEL_STATUS:
                return checkTerminalProfileValue(12, (byte) 16);
            default:
                return false;
        }
    }

    void make(BerTlv berTlv) {
        boolean cmdPending;
        if (berTlv != null) {
            this.mCmdParams = null;
            this.mIconLoadState = 0;
            if (berTlv.getTag() != 208) {
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            List<ComprehensionTlv> ctlvs = berTlv.getComprehensionTlvs();
            CommandDetails cmdDet = processCommandDetails(ctlvs);
            if (cmdDet == null) {
                sendCmdParams(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                return;
            }
            AppInterface.CommandType cmdType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
            if (cmdType == null) {
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
                switch (cmdType) {
                    case SET_UP_MENU:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processSelectItem(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SELECT_ITEM:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processSelectItem(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case DISPLAY_TEXT:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processDisplayText(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SET_UP_IDLE_MODE_TEXT:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processSetUpIdleModeText(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case GET_INKEY:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processGetInkey(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case GET_INPUT:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processGetInput(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SEND_DTMF:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            if (searchForTag(ComprehensionTlvTag.DTMF_STRING, ctlvs) == null) {
                                cmdPending = processEventNotify(cmdDet, ctlvs);
                            } else {
                                cmdPending = processSendDTMF(cmdDet, ctlvs);
                            }
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SEND_SMS:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            if (searchForTag(ComprehensionTlvTag.ADDRESS, ctlvs) == null && searchForTag(ComprehensionTlvTag.SMS_TPDU, ctlvs) == null) {
                                cmdPending = processEventNotify(cmdDet, ctlvs);
                            } else {
                                cmdPending = processSendSMS(cmdDet, ctlvs);
                            }
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SEND_SS:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            if (searchForTag(ComprehensionTlvTag.SS_STRING, ctlvs) == null) {
                                cmdPending = processEventNotify(cmdDet, ctlvs);
                            } else {
                                cmdPending = processSendSS(cmdDet, ctlvs);
                            }
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SEND_USSD:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            if (searchForTag(ComprehensionTlvTag.USSD_STRING, ctlvs) == null) {
                                cmdPending = processEventNotify(cmdDet, ctlvs);
                            } else {
                                cmdPending = processSendUSSD(cmdDet, ctlvs);
                            }
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SET_UP_CALL:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processSetupCall(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case REFRESH:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processRefresh(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case LAUNCH_BROWSER:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processLaunchBrowser(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case PLAY_TONE:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processPlayTone(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SET_UP_EVENT_LIST:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processSetUpEventList(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case PROVIDE_LOCAL_INFORMATION:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processProvideLocalInfo(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case LANGUAGE_NOTIFICATION:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processLanguageNotification(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case OPEN_CHANNEL:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processOpenChannel(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case CLOSE_CHANNEL:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processCloseChannel(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case RECEIVE_DATA:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processReceiveData(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case SEND_DATA:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processSendData(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    case GET_CHANNEL_STATUS:
                        if (checkCmdTerminalProfileValue(cmdType)) {
                            cmdPending = processGetChannelStatus(cmdDet, ctlvs);
                        } else {
                            this.mCmdParams = new CommandParams(cmdDet);
                            sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                            return;
                        }
                        break;
                    default:
                        this.mCmdParams = new CommandParams(cmdDet);
                        sendCmdParams(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                        return;
                }
                if (!cmdPending) {
                    sendCmdParams(ResultCode.OK);
                }
            } catch (ResultException e) {
                CatLog.d(this, "make: caught ResultException e=" + e);
                this.mCmdParams = new CommandParams(cmdDet);
                sendCmdParams(e.result());
            }
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
            return ResultCode.OK;
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

    private boolean processDisplayText(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process DisplayText");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv);
        }
        if (textMsg.text == null) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        if (searchForTag(ComprehensionTlvTag.IMMEDIATE_RESPONSE, ctlvs) != null) {
            textMsg.responseNeeded = false;
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv3 != null) {
            textMsg.duration = ValueParser.retrieveDuration(ctlv3);
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.TEXT_ATTRIBUTE, ctlvs);
        if (ctlv4 != null) {
            CatLog.d(this, "Text Attribute pararmeter");
            textMsg.textAttr = ValueParser.retrieveTextAttribute(ctlv4);
        }
        textMsg.isHighPriority = (cmdDet.commandQualifier & 1) != 0;
        textMsg.userClear = (cmdDet.commandQualifier & 128) != 0;
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSetUpIdleModeText(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        ComprehensionTlv ctlv;
        CatLog.d(this, "process SetUpIdleModeText");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv2 != null) {
            textMsg.text = ValueParser.retrieveTextString(ctlv2);
        }
        if (textMsg.text != null && (ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs)) != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processGetInkey(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process GetInkey");
        Input input = new Input();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
            ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
            if (ctlv2 != null) {
                iconId = ValueParser.retrieveIconId(ctlv2);
            }
            ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
            if (ctlv3 != null) {
                input.duration = ValueParser.retrieveDuration(ctlv3);
            }
            input.minLen = 1;
            input.maxLen = 1;
            input.digitOnly = (cmdDet.commandQualifier & 1) == 0;
            input.ucs2 = (cmdDet.commandQualifier & 2) != 0;
            input.yesNo = (cmdDet.commandQualifier & 4) != 0;
            input.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
            input.echo = true;
            if (AppInterface.STK_ICON_DISABLED && iconId != null) {
                input.disableIcon = true;
                iconId = null;
            }
            this.mCmdParams = new GetInputParams(cmdDet, input);
            if (iconId == null) {
                return false;
            }
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processGetInput(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process GetInput");
        Input input = new Input();
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TEXT_STRING, ctlvs);
        if (ctlv != null) {
            input.text = ValueParser.retrieveTextString(ctlv);
            ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.RESPONSE_LENGTH, ctlvs);
            if (ctlv2 != null) {
                try {
                    byte[] rawValue = ctlv2.getRawValue();
                    int valueIndex = ctlv2.getValueIndex();
                    input.minLen = rawValue[valueIndex] & 255;
                    input.maxLen = rawValue[valueIndex + 1] & 255;
                    ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DEFAULT_TEXT, ctlvs);
                    if (ctlv3 != null) {
                        input.defaultText = ValueParser.retrieveTextString(ctlv3);
                    }
                    ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
                    if (ctlv4 != null) {
                        iconId = ValueParser.retrieveIconId(ctlv4);
                    }
                    input.digitOnly = (cmdDet.commandQualifier & 1) == 0;
                    input.ucs2 = (cmdDet.commandQualifier & 2) != 0;
                    input.echo = (cmdDet.commandQualifier & 4) == 0;
                    input.packed = (cmdDet.commandQualifier & 8) != 0;
                    input.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
                    if (input.ucs2 && input.maxLen > 70) {
                        input.maxLen = 70;
                    }
                    if (AppInterface.STK_ICON_DISABLED && iconId != null) {
                        input.disableIcon = true;
                        iconId = null;
                    }
                    this.mCmdParams = new GetInputParams(cmdDet, input);
                    if (iconId == null) {
                        return false;
                    }
                    this.mIconLoadState = 1;
                    this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                    return true;
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processRefresh(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process Refresh");
        switch (cmdDet.commandQualifier) {
            case 0:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
                this.mCmdParams = new SetRefreshParams(cmdDet, false, null);
                return false;
            case 1:
            case 2:
                ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.FILE_LIST, ctlvs);
                if (ctlv != null) {
                    try {
                        int[] fileList = ValueParser.retrieveFileList(ctlv);
                        this.mCmdParams = new SetRefreshParams(cmdDet, true, fileList);
                    } catch (IndexOutOfBoundsException e) {
                        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                    }
                    break;
                }
                return false;
            default:
                CatLog.d(this, "refresh commandQualifier waiting extend" + cmdDet.commandQualifier);
                return false;
        }
    }

    private boolean processSelectItem(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SelectItem");
        Menu menu = new Menu();
        IconId titleIconId = null;
        ItemsIconId itemsIconId = null;
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            menu.title = ValueParser.retrieveAlphaId(ctlv);
        }
        while (true) {
            ComprehensionTlv ctlv2 = searchForNextTag(ComprehensionTlvTag.ITEM, iter);
            if (ctlv2 == null) {
                break;
            }
            menu.items.add(ValueParser.retrieveItem(ctlv2));
        }
        if (menu.items.size() == 0) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.ITEM_ID, ctlvs);
        if (ctlv3 != null) {
            menu.defaultItem = ValueParser.retrieveItemId(ctlv3) - 1;
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv4 != null) {
            this.mIconLoadState = 1;
            titleIconId = ValueParser.retrieveIconId(ctlv4);
            menu.titleIconSelfExplanatory = titleIconId.selfExplanatory;
        }
        ComprehensionTlv ctlv5 = searchForTag(ComprehensionTlvTag.ITEM_ICON_ID_LIST, ctlvs);
        if (ctlv5 != null) {
            this.mIconLoadState = 2;
            itemsIconId = ValueParser.retrieveItemsIconId(ctlv5);
            menu.itemsIconSelfExplanatory = itemsIconId.selfExplanatory;
        }
        boolean presentTypeSpecified = (cmdDet.commandQualifier & 1) != 0;
        if (presentTypeSpecified) {
            if ((cmdDet.commandQualifier & 2) == 0) {
                menu.presentationType = PresentationType.DATA_VALUES;
            } else {
                menu.presentationType = PresentationType.NAVIGATION_OPTIONS;
            }
        }
        menu.softKeyPreferred = (cmdDet.commandQualifier & 4) != 0;
        menu.helpAvailable = (cmdDet.commandQualifier & 128) != 0;
        if (AppInterface.STK_ICON_DISABLED) {
            if (titleIconId != null) {
                CatLog.d(this, "titleIconId exist");
                menu.disableTitleIcon = true;
                titleIconId = null;
            }
            if (itemsIconId != null) {
                CatLog.d(this, "itemsIconId exist");
                menu.disableItemsIcon = true;
                itemsIconId = null;
            }
            this.mIconLoadState = 0;
        }
        this.mCmdParams = new SelectItemParams(cmdDet, menu, titleIconId != null);
        switch (this.mIconLoadState) {
            case 0:
                return false;
            case 1:
                this.mIconLoader.loadIcon(titleIconId.recordNumber, obtainMessage(1));
                break;
            case 2:
                int[] recordNumbers = itemsIconId.recordNumbers;
                if (titleIconId != null) {
                    recordNumbers = new int[itemsIconId.recordNumbers.length + 1];
                    recordNumbers[0] = titleIconId.recordNumber;
                    System.arraycopy(itemsIconId.recordNumbers, 0, recordNumbers, 1, itemsIconId.recordNumbers.length);
                }
                this.mIconLoader.loadIcons(recordNumbers, obtainMessage(1));
                break;
        }
        return true;
    }

    private boolean processEventNotify(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process EventNotify");
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
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        if (textMsg != null) {
            textMsg.responseNeeded = false;
            this.mCmdParams = new DisplayTextParams(cmdDet, textMsg);
        }
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSetUpEventList(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        int[] events;
        CatLog.d(this, "process SetUpEventList");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.EVENT_LIST, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                if (valueLen > 0) {
                    events = new int[valueLen];
                    for (int i = 0; i < valueLen; i++) {
                        events[i] = rawValue[valueIndex + i];
                        CatLog.d(this, "process SetupEventList event = " + events[i]);
                        if (checkEventTerminalProfileValue(events[i])) {
                            CatLog.d(this, "we can support this event,continute");
                        } else {
                            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                        }
                    }
                } else {
                    events = new int[]{EventList.REMOVE_EVENT.value()};
                }
                this.mCmdParams = new SetEventListParams(cmdDet, events);
            } catch (IndexOutOfBoundsException e) {
                CatLog.e(this, " IndexOutofBoundException in processSetUpEventList");
            }
        }
        return false;
    }

    private boolean processLaunchBrowser(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        LaunchBrowserMode mode;
        CatLog.d(this, "process LaunchBrowser");
        TextMessage confirmMsg = new TextMessage();
        IconId iconId = null;
        String url = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.URL, ctlvs);
        if (ctlv != null) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int valueLen = ctlv.getLength();
                if (valueLen > 0) {
                    url = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex, valueLen);
                } else {
                    url = null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        confirmMsg.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs));
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            confirmMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        switch (cmdDet.commandQualifier) {
            case 2:
                mode = LaunchBrowserMode.USE_EXISTING_BROWSER;
                break;
            case 3:
                mode = LaunchBrowserMode.LAUNCH_NEW_BROWSER;
                break;
            default:
                mode = LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED;
                break;
        }
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            confirmMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new LaunchBrowserParams(cmdDet, confirmMsg, url, mode);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processPlayTone(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process PlayTone");
        Tone tone = null;
        TextMessage textMsg = new TextMessage();
        Duration duration = null;
        IconId iconId = null;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.TONE, ctlvs);
        if (ctlv != null && ctlv.getLength() > 0) {
            try {
                byte[] rawValue = ctlv.getRawValue();
                int valueIndex = ctlv.getValueIndex();
                int toneVal = rawValue[valueIndex];
                tone = Tone.fromInt(toneVal);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv2 != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv2);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.DURATION, ctlvs);
        if (ctlv3 != null) {
            duration = ValueParser.retrieveDuration(ctlv3);
        }
        ComprehensionTlv ctlv4 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv4 != null) {
            iconId = ValueParser.retrieveIconId(ctlv4);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        boolean vibrate = (cmdDet.commandQualifier & 1) != 0;
        textMsg.responseNeeded = false;
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new PlayToneParams(cmdDet, textMsg, tone, duration, vibrate);
        if (iconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    private boolean processSetupCall(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SetupCall");
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        TextMessage confirmMsg = new TextMessage();
        TextMessage callMsg = new TextMessage();
        IconId confirmIconId = null;
        IconId callIconId = null;
        confirmMsg.text = ValueParser.retrieveAlphaId(searchForNextTag(ComprehensionTlvTag.ALPHA_ID, iter));
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            confirmIconId = ValueParser.retrieveIconId(ctlv);
            confirmMsg.iconSelfExplanatory = confirmIconId.selfExplanatory;
        }
        ComprehensionTlv ctlv2 = searchForNextTag(ComprehensionTlvTag.ALPHA_ID, iter);
        if (ctlv2 != null) {
            callMsg.text = ValueParser.retrieveAlphaId(ctlv2);
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv3 != null) {
            callIconId = ValueParser.retrieveIconId(ctlv3);
            callMsg.iconSelfExplanatory = callIconId.selfExplanatory;
        }
        if (AppInterface.STK_ICON_DISABLED) {
            if (confirmIconId != null) {
                confirmMsg.disableIcon = true;
                confirmIconId = null;
            }
            if (callIconId != null) {
                callMsg.disableIcon = true;
                callIconId = null;
            }
        }
        this.mCmdParams = new CallSetupParams(cmdDet, confirmMsg, callMsg);
        if (confirmIconId == null && callIconId == null) {
            return false;
        }
        this.mIconLoadState = 2;
        int[] recordNumbers = new int[2];
        recordNumbers[0] = confirmIconId != null ? confirmIconId.recordNumber : -1;
        recordNumbers[1] = callIconId != null ? callIconId.recordNumber : -1;
        this.mIconLoader.loadIcons(recordNumbers, obtainMessage(1));
        return true;
    }

    private boolean processProvideLocalInfo(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process ProvideLocalInfo");
        switch (cmdDet.commandQualifier) {
            case 3:
                CatLog.d(this, "PLI [DTTZ_SETTING]");
                this.mCmdParams = new CommandParams(cmdDet);
                return false;
            case 4:
                CatLog.d(this, "PLI [LANGUAGE_SETTING]");
                this.mCmdParams = new CommandParams(cmdDet);
                return false;
            default:
                CatLog.d(this, "PLI[" + cmdDet.commandQualifier + "] Command Not Supported");
                this.mCmdParams = new CommandParams(cmdDet);
                throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
        }
    }

    private boolean processLanguageNotification(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process processLanguageNotification");
        switch (cmdDet.commandQualifier) {
            case 0:
                this.mCmdParams = new CommandParams(cmdDet);
                break;
            case 1:
                ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.LANGUAGE, ctlvs);
                if (ctlv != null) {
                }
                this.mCmdParams = new CommandParams(cmdDet);
                break;
        }
        return false;
    }

    private boolean processBIPClient(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        AppInterface.CommandType commandType = AppInterface.CommandType.fromInt(cmdDet.typeOfCommand);
        if (commandType != null) {
            CatLog.d(this, "process " + commandType.name());
        }
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        boolean has_alpha_id = false;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            textMsg.text = ValueParser.retrieveAlphaId(ctlv);
            CatLog.d(this, "alpha TLV text=" + textMsg.text);
            has_alpha_id = true;
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        textMsg.responseNeeded = false;
        this.mCmdParams = new BIPClientParams(cmdDet, textMsg, has_alpha_id);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    public void dispose() {
        this.mIconLoader.dispose();
        this.mIconLoader = null;
        this.mCmdParams = null;
        this.mCaller = null;
        sInstance = null;
    }

    private boolean processOpenChannel(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        String addressString;
        CatLog.d(this, "process OpenChannel");
        TextMessage confirmMsg = new TextMessage();
        InterfaceTransportLevel itl = null;
        BearerDescription bearerDescription = null;
        byte[] destinationAddress = null;
        String networkAccessName = null;
        String userLogin = null;
        String userPassword = null;
        confirmMsg.responseNeeded = false;
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs);
        if (ctlv != null) {
            confirmMsg.text = ValueParser.retrieveAlphaId(ctlv);
            if (confirmMsg.text != null) {
                confirmMsg.responseNeeded = true;
            }
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.BUFFER_SIZE, ctlvs);
        if (ctlv2 != null) {
            int bufSize = ValueParser.retrieveBufferSize(ctlv2);
            Iterator<ComprehensionTlv> iter = ctlvs.iterator();
            ComprehensionTlv ctlv3 = searchForNextTag(ComprehensionTlvTag.IF_TRANS_LEVEL, iter);
            if (ctlv3 != null) {
                itl = ValueParser.retrieveInterfaceTransportLevel(ctlv3);
                ComprehensionTlv ctlv4 = searchForNextTag(ComprehensionTlvTag.OTHER_ADDRESS, iter);
                if (ctlv4 != null) {
                    destinationAddress = ValueParser.retrieveOtherAddress(ctlv4);
                    if (destinationAddress.length != 4) {
                        throw new ResultException(ResultCode.CMD_TYPE_NOT_UNDERSTOOD);
                    }
                }
            }
            ComprehensionTlv ctlv5 = searchForTag(ComprehensionTlvTag.BEARER_DESC, ctlvs);
            if (ctlv5 != null) {
                bearerDescription = ValueParser.retrieveBearerDescription(ctlv5);
                CatLog.d(this, "processOpenChannel bearer: " + bearerDescription.type.value() + " param.len: " + bearerDescription.parameters.length);
            }
            Iterator<ComprehensionTlv> iter2 = ctlvs.iterator();
            ComprehensionTlv ctlv6 = searchForNextTag(ComprehensionTlvTag.NETWORK_ACCESS_NAME, iter2);
            if (ctlv6 != null) {
                networkAccessName = ValueParser.retrieveNetworkAccessName(ctlv6);
                ComprehensionTlv ctlv7 = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, iter2);
                if (ctlv7 != null) {
                    userLogin = ValueParser.retrieveTextString(ctlv7);
                }
                ComprehensionTlv ctlv8 = searchForNextTag(ComprehensionTlvTag.TEXT_STRING, iter2);
                if (ctlv8 != null) {
                    userPassword = ValueParser.retrieveTextString(ctlv8);
                }
            }
            ComprehensionTlv ctlv9 = searchForTag(ComprehensionTlvTag.TEXT_ATTRIBUTE, ctlvs);
            if (ctlv9 != null) {
                CatLog.d(this, "Text Attribute pararmeter");
                if (confirmMsg.text != null) {
                    confirmMsg.textAttr = ValueParser.retrieveTextAttribute(ctlv9);
                } else {
                    CatLog.d(this, "text is null, can't set text attribute");
                }
            }
            if (itl != null && bearerDescription == null) {
                if (itl.protocol != InterfaceTransportLevel.TransportProtocol.TCP_SERVER && itl.protocol != InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_LOCAL && itl.protocol != InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_LOCAL) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            } else if (bearerDescription != null) {
                if (bearerDescription.type != BearerDescription.BearerType.DEFAULT_BEARER && bearerDescription.type != BearerDescription.BearerType.MOBILE_PS && bearerDescription.type != BearerDescription.BearerType.MOBILE_PS_EXTENDED_QOS && bearerDescription.type != BearerDescription.BearerType.E_UTRAN) {
                    throw new ResultException(ResultCode.BEYOND_TERMINAL_CAPABILITY);
                }
                if (itl != null) {
                    if (itl.protocol != InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_REMOTE && itl.protocol != InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_REMOTE) {
                        throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                    }
                    if (destinationAddress == null) {
                        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
                    }
                } else {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            } else {
                throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
            }
            try {
                addressString = InetAddress.getByAddress(destinationAddress).getHostAddress();
            } catch (UnknownHostException e) {
                addressString = "unknown";
            }
            String msg = new String("processOpenChannel bufSize=" + bufSize + " protocol=" + (itl != null ? itl.protocol : "undefined") + " port=" + (itl != null ? Integer.valueOf(itl.port) : "undefined") + " destination=" + addressString + " APN=" + (networkAccessName != null ? networkAccessName : "undefined") + " user/password=" + (userLogin != null ? userLogin : "---") + "/" + (userPassword != null ? userPassword : "---"));
            CatLog.d(this, msg);
            this.mCmdParams = new OpenChannelParams(cmdDet, confirmMsg, bufSize, itl, destinationAddress, bearerDescription, networkAccessName, userLogin, userPassword);
            return false;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processCloseChannel(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process CloseChannel");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            int channel = ValueParser.retrieveDeviceIdentities(ctlv).destinationId;
            if (channel < 33 || channel > 39) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            this.mCmdParams = new CloseChannelParams(cmdDet, channel & 15);
            return false;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processReceiveData(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process ReceiveData");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            int channel = ValueParser.retrieveDeviceIdentities(ctlv).destinationId;
            if (channel < 33 || channel > 39) {
                CatLog.d(this, "Invalid Channel number given: " + channel);
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            int channel2 = channel & 15;
            ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.CHANNEL_DATA_LENGTH, ctlvs);
            if (ctlv2 != null) {
                int datLen = ValueParser.retrieveChannelDataLength(ctlv2);
                this.mCmdParams = new ReceiveDataParams(cmdDet, channel2, datLen);
                return false;
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processSendData(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process SendData");
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.DEVICE_IDENTITIES, ctlvs);
        if (ctlv != null) {
            int channel = ValueParser.retrieveDeviceIdentities(ctlv).destinationId;
            if (channel < 33 || channel > 39) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            int channel2 = channel & 15;
            ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.CHANNEL_DATA, ctlvs);
            if (ctlv2 != null) {
                byte[] data = ValueParser.retrieveChannelData(ctlv2);
                this.mCmdParams = new SendDataParams(cmdDet, channel2, data);
                return false;
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    private boolean processGetChannelStatus(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process GetChannelStatus");
        this.mCmdParams = new GetChannelStatusParams(cmdDet);
        return false;
    }

    private boolean processSendDTMF(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process Send DTMF");
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        String dtmfString = null;
        textMsg.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs));
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv2 = searchForNextTag(ComprehensionTlvTag.DTMF_STRING, iter);
        if (ctlv2 != null) {
            dtmfString = ValueParser.retrieveDTMFString(ctlv2);
        }
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new SendDTMFParams(cmdDet, textMsg, dtmfString);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSendSS(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process Send SS");
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        String sSString = null;
        textMsg.text = ValueParser.retrieveAlphaId(searchForNextTag(ComprehensionTlvTag.ALPHA_ID, iter));
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv2 = searchForNextTag(ComprehensionTlvTag.SS_STRING, iter);
        if (ctlv2 != null) {
            sSString = ValueParser.retrieveSSString(ctlv2);
        }
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new SendSSParams(cmdDet, textMsg, sSString);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSendUSSD(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process Send SS");
        Iterator<ComprehensionTlv> iter = ctlvs.iterator();
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        String uSSDString = null;
        textMsg.text = ValueParser.retrieveAlphaId(searchForNextTag(ComprehensionTlvTag.ALPHA_ID, iter));
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv != null) {
            iconId = ValueParser.retrieveIconId(ctlv);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv2 = searchForNextTag(ComprehensionTlvTag.USSD_STRING, iter);
        if (ctlv2 != null) {
            uSSDString = ValueParser.retrieveTextString(ctlv2);
        }
        if (AppInterface.STK_ICON_DISABLED && iconId != null) {
            textMsg.disableIcon = true;
            iconId = null;
        }
        this.mCmdParams = new SendUSSDParams(cmdDet, textMsg, uSSDString);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    private boolean processSendSMS(CommandDetails cmdDet, List<ComprehensionTlv> ctlvs) throws ResultException {
        CatLog.d(this, "process Send SMS");
        TextMessage textMsg = new TextMessage();
        IconId iconId = null;
        String smsAdress = null;
        textMsg.text = ValueParser.retrieveAlphaId(searchForTag(ComprehensionTlvTag.ALPHA_ID, ctlvs));
        ComprehensionTlv ctlv = searchForTag(ComprehensionTlvTag.ADDRESS, ctlvs);
        if (ctlv != null) {
            smsAdress = ValueParser.retrieveSMSAddress(ctlv);
        }
        ComprehensionTlv ctlv2 = searchForTag(ComprehensionTlvTag.ICON_ID, ctlvs);
        if (ctlv2 != null) {
            iconId = ValueParser.retrieveIconId(ctlv2);
            textMsg.iconSelfExplanatory = iconId.selfExplanatory;
        }
        ComprehensionTlv ctlv3 = searchForTag(ComprehensionTlvTag.SMS_TPDU, ctlvs);
        if (ctlv3 != null) {
            byte[] data = ValueParser.retrieveSMSTPDUData(ctlv3);
            if (AppInterface.STK_ICON_DISABLED && iconId != null) {
                textMsg.disableIcon = true;
                iconId = null;
            }
            if (cmdDet.commandQualifier == 1) {
                StkSmsMessage ssm = StkSmsMessage.createFromPdu(data);
                int dcs = ssm.getDataCodingScheme();
                if ((dcs & 4) == 4) {
                    CatLog.d(this, "sms packing requied and dcs is 8-bit");
                    byte[] encodeMessage = ssm.getEncodeMessage();
                    data = encodeMessage;
                }
            }
            this.mCmdParams = new SMSTPDUParams(cmdDet, textMsg, smsAdress, data.length, data);
            if (iconId != null) {
                this.mIconLoadState = 1;
                this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
                return true;
            }
            return false;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }
}
