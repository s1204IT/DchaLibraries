package com.mediatek.internal.telephony.cat;

import android.content.Context;
import android.hardware.radio.V1_0.DataCallFailCause;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.CallSetupParams;
import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParamsFactory;
import com.android.internal.telephony.cat.ComprehensionTlv;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.DisplayTextParams;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.cat.GetInputParams;
import com.android.internal.telephony.cat.IconId;
import com.android.internal.telephony.cat.Input;
import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.ItemsIconId;
import com.android.internal.telephony.cat.LaunchBrowserMode;
import com.android.internal.telephony.cat.LaunchBrowserParams;
import com.android.internal.telephony.cat.PlayToneParams;
import com.android.internal.telephony.cat.PresentationType;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.cat.ResultException;
import com.android.internal.telephony.cat.RilMessageDecoder;
import com.android.internal.telephony.cat.SelectItemParams;
import com.android.internal.telephony.cat.SetEventListParams;
import com.android.internal.telephony.cat.TextMessage;
import com.android.internal.telephony.cat.Tone;
import com.android.internal.telephony.cat.ValueParser;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.util.Iterator;
import java.util.List;

public class MtkCommandParamsFactory extends CommandParamsFactory {
    public static final int BATTERY_STATE = 10;
    private Context mContext;
    int tlvIndex;

    public MtkCommandParamsFactory(RilMessageDecoder rilMessageDecoder, IccFileHandler iccFileHandler) {
        super(rilMessageDecoder, iccFileHandler);
        this.tlvIndex = -1;
        this.mContext = ((MtkCatService) rilMessageDecoder.mCaller).getContext();
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

    protected boolean processDisplayText(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        MtkCatLog.d(this, "process DisplayText");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
        }
        if (textMessage.text == null) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
        if (searchForTag(ComprehensionTlvTag.IMMEDIATE_RESPONSE, list) != null) {
            textMessage.responseNeeded = false;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag2 != null) {
            try {
                iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            } catch (ResultException e) {
                MtkCatLog.e(this, "retrieveIconId ResultException: " + e.result());
            }
            try {
                textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
            } catch (NullPointerException e2) {
                MtkCatLog.e(this, "iconId is null.");
            }
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DURATION, list);
        if (comprehensionTlvSearchForTag3 != null) {
            try {
                textMessage.duration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag3);
            } catch (ResultException e3) {
                MtkCatLog.e(this, "retrieveDuration ResultException: " + e3.result());
            }
        }
        textMessage.isHighPriority = (commandDetails.commandQualifier & 1) != 0;
        textMessage.userClear = (commandDetails.commandQualifier & 128) != 0;
        this.mCmdParams = new DisplayTextParams(commandDetails, textMessage);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mloadIcon = true;
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    protected boolean processGetInkey(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        MtkCatLog.d(this, "process GetInkey");
        Input input = new Input();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            input.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
            ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
            IconId iconIdRetrieveIconId = null;
            if (comprehensionTlvSearchForTag2 != null) {
                try {
                    iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
                } catch (ResultException e) {
                    MtkCatLog.e(this, "retrieveIconId ResultException: " + e.result());
                }
                try {
                    input.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
                } catch (NullPointerException e2) {
                    MtkCatLog.e(this, "iconId is null.");
                }
            }
            ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DURATION, list);
            if (comprehensionTlvSearchForTag3 != null) {
                try {
                    input.duration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag3);
                } catch (ResultException e3) {
                    MtkCatLog.e(this, "retrieveDuration ResultException: " + e3.result());
                }
            }
            input.minLen = 1;
            input.maxLen = 1;
            input.digitOnly = (commandDetails.commandQualifier & 1) == 0;
            input.ucs2 = (commandDetails.commandQualifier & 2) != 0;
            input.yesNo = (commandDetails.commandQualifier & 4) != 0;
            input.helpAvailable = (commandDetails.commandQualifier & 128) != 0;
            input.echo = true;
            this.mCmdParams = new GetInputParams(commandDetails, input);
            if (iconIdRetrieveIconId == null) {
                return false;
            }
            this.mloadIcon = true;
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
            return true;
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    protected boolean processGetInput(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        MtkCatLog.d(this, "process GetInput");
        Input input = new Input();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TEXT_STRING, list);
        if (comprehensionTlvSearchForTag != null) {
            input.text = ValueParser.retrieveTextString(comprehensionTlvSearchForTag);
            ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.RESPONSE_LENGTH, list);
            if (comprehensionTlvSearchForTag2 != null) {
                try {
                    byte[] rawValue = comprehensionTlvSearchForTag2.getRawValue();
                    int valueIndex = comprehensionTlvSearchForTag2.getValueIndex();
                    input.minLen = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
                    if (input.minLen > 239) {
                        input.minLen = 239;
                    }
                    input.maxLen = rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID;
                    if (input.maxLen > 239) {
                        input.maxLen = 239;
                    }
                    ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DEFAULT_TEXT, list);
                    if (comprehensionTlvSearchForTag3 != null) {
                        try {
                            input.defaultText = ValueParser.retrieveTextString(comprehensionTlvSearchForTag3);
                        } catch (ResultException e) {
                            MtkCatLog.e(this, "retrieveTextString ResultException: " + e.result());
                        }
                    }
                    ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
                    IconId iconIdRetrieveIconId = null;
                    if (comprehensionTlvSearchForTag4 != null) {
                        try {
                            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag4);
                        } catch (ResultException e2) {
                            MtkCatLog.e(this, "retrieveIconId ResultException: " + e2.result());
                        }
                        try {
                            input.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
                        } catch (NullPointerException e3) {
                            MtkCatLog.e(this, "iconId is null.");
                        }
                    }
                    input.digitOnly = (commandDetails.commandQualifier & 1) == 0;
                    input.ucs2 = (commandDetails.commandQualifier & 2) != 0;
                    input.echo = (commandDetails.commandQualifier & 4) == 0;
                    input.packed = (commandDetails.commandQualifier & 8) != 0;
                    input.helpAvailable = (commandDetails.commandQualifier & 128) != 0;
                    if (input.ucs2 && input.maxLen > 118) {
                        MtkCatLog.d(this, "UCS2: received maxLen = " + input.maxLen + ", truncating to " + DataCallFailCause.COMPANION_IFACE_IN_USE);
                        input.maxLen = DataCallFailCause.COMPANION_IFACE_IN_USE;
                    } else if (!input.packed && input.maxLen > 239) {
                        MtkCatLog.d(this, "GSM 7Bit Default: received maxLen = " + input.maxLen + ", truncating to 239");
                        input.maxLen = 239;
                    }
                    this.mCmdParams = new GetInputParams(commandDetails, input);
                    if (iconIdRetrieveIconId == null) {
                        return false;
                    }
                    this.mloadIcon = true;
                    this.mIconLoadState = 1;
                    this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
                    return true;
                } catch (IndexOutOfBoundsException e4) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
    }

    protected boolean processSelectItem(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        MtkCatLog.d(this, "process SelectItem");
        MtkMenu mtkMenu = new MtkMenu();
        Iterator<ComprehensionTlv> it = list.iterator();
        AppInterface.CommandType commandTypeFromInt = AppInterface.CommandType.fromInt(commandDetails.typeOfCommand);
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag != null) {
            try {
                mtkMenu.title = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
            } catch (ResultException e) {
                MtkCatLog.e(this, "retrieveAlphaId ResultException: " + e.result());
            }
            MtkCatLog.d(this, "add AlphaId: " + mtkMenu.title);
        } else if (commandTypeFromInt == AppInterface.CommandType.SET_UP_MENU) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        while (true) {
            ComprehensionTlv comprehensionTlvSearchForNextTag = searchForNextTag(ComprehensionTlvTag.ITEM, it);
            if (comprehensionTlvSearchForNextTag == null) {
                break;
            }
            Item itemRetrieveItem = MtkValueParser.retrieveItem(comprehensionTlvSearchForNextTag);
            StringBuilder sb = new StringBuilder();
            sb.append("add menu item: ");
            sb.append(itemRetrieveItem == null ? "" : itemRetrieveItem.toString());
            MtkCatLog.d(this, sb.toString());
            mtkMenu.items.add(itemRetrieveItem);
        }
        if (mtkMenu.items.size() == 0) {
            MtkCatLog.d(this, "no menu item");
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.NEXT_ACTION_INDICATOR, list);
        ItemsIconId itemsIconIdRetrieveItemsIconId = null;
        if (comprehensionTlvSearchForTag2 != null) {
            try {
                mtkMenu.nextActionIndicator = MtkValueParser.retrieveNextActionIndicator(comprehensionTlvSearchForTag2);
            } catch (ResultException e2) {
                MtkCatLog.e(this, "retrieveNextActionIndicator ResultException: " + e2.result());
            }
            try {
                if (mtkMenu.nextActionIndicator.length != mtkMenu.items.size()) {
                    MtkCatLog.d(this, "nextActionIndicator.length != number of menu items");
                    mtkMenu.nextActionIndicator = null;
                }
            } catch (NullPointerException e3) {
                MtkCatLog.e(this, "nextActionIndicator is null.");
            }
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.ITEM_ID, list);
        if (comprehensionTlvSearchForTag3 != null) {
            try {
                mtkMenu.defaultItem = ValueParser.retrieveItemId(comprehensionTlvSearchForTag3) - 1;
            } catch (ResultException e4) {
                MtkCatLog.e(this, "retrieveItemId ResultException: " + e4.result());
            }
            MtkCatLog.d(this, "default item: " + mtkMenu.defaultItem);
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag4 != null) {
            this.mIconLoadState = 1;
            try {
                iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag4);
            } catch (ResultException e5) {
                MtkCatLog.e(this, "retrieveIconId ResultException: " + e5.result());
                iconIdRetrieveIconId = null;
            }
            try {
                mtkMenu.titleIconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
            } catch (NullPointerException e6) {
                MtkCatLog.e(this, "titleIconId is null.");
            }
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag5 = searchForTag(ComprehensionTlvTag.ITEM_ICON_ID_LIST, list);
        if (comprehensionTlvSearchForTag5 != null) {
            this.mIconLoadState = 2;
            try {
                itemsIconIdRetrieveItemsIconId = ValueParser.retrieveItemsIconId(comprehensionTlvSearchForTag5);
            } catch (ResultException e7) {
                MtkCatLog.e(this, "retrieveItemsIconId ResultException: " + e7.result());
            }
            try {
                mtkMenu.itemsIconSelfExplanatory = itemsIconIdRetrieveItemsIconId.selfExplanatory;
            } catch (NullPointerException e8) {
                MtkCatLog.e(this, "itemsIconId is null.");
            }
        }
        if ((commandDetails.commandQualifier & 1) != 0) {
            if ((commandDetails.commandQualifier & 2) == 0) {
                mtkMenu.presentationType = PresentationType.DATA_VALUES;
            } else {
                mtkMenu.presentationType = PresentationType.NAVIGATION_OPTIONS;
            }
        }
        mtkMenu.softKeyPreferred = (commandDetails.commandQualifier & 4) != 0;
        mtkMenu.helpAvailable = (commandDetails.commandQualifier & 128) != 0;
        this.mCmdParams = new SelectItemParams(commandDetails, mtkMenu, iconIdRetrieveIconId != null);
        switch (this.mIconLoadState) {
            case 0:
                return false;
            case 1:
                if (iconIdRetrieveIconId == null || iconIdRetrieveIconId.recordNumber <= 0) {
                    return false;
                }
                this.mloadIcon = true;
                this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
                return true;
            case 2:
                if (itemsIconIdRetrieveItemsIconId == null) {
                    return false;
                }
                int[] iArr = itemsIconIdRetrieveItemsIconId.recordNumbers;
                if (iconIdRetrieveIconId != null) {
                    iArr = new int[itemsIconIdRetrieveItemsIconId.recordNumbers.length + 1];
                    iArr[0] = iconIdRetrieveIconId.recordNumber;
                    System.arraycopy(itemsIconIdRetrieveItemsIconId.recordNumbers, 0, iArr, 1, itemsIconIdRetrieveItemsIconId.recordNumbers.length);
                }
                this.mloadIcon = true;
                this.mIconLoader.loadIcons(iArr, obtainMessage(1));
                return true;
            default:
                return true;
        }
    }

    protected boolean processEventNotify(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        MtkCatLog.d(this, "process EventNotify");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag);
        } else {
            textMessage.text = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag2);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        textMessage.responseNeeded = false;
        this.mCmdParams = new DisplayTextParams(commandDetails, textMessage);
        if (iconIdRetrieveIconId == null) {
            return false;
        }
        this.mloadIcon = true;
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
        return true;
    }

    protected boolean processSetUpEventList(CommandDetails commandDetails, List<ComprehensionTlv> list) {
        MtkCatLog.d(this, "process SetUpEventList");
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.EVENT_LIST, list);
        if (comprehensionTlvSearchForTag != null) {
            try {
                byte[] rawValue = comprehensionTlvSearchForTag.getRawValue();
                int valueIndex = comprehensionTlvSearchForTag.getValueIndex();
                int length = comprehensionTlvSearchForTag.getLength();
                int[] iArr = new int[length];
                int i = valueIndex;
                int i2 = 0;
                while (i2 < length) {
                    iArr[i2] = rawValue[i];
                    MtkCatLog.v(this, "CPF-processSetUpEventList: eventList[" + i2 + "] = " + iArr[i2]);
                    i2++;
                    i++;
                }
                this.mCmdParams = new SetEventListParams(commandDetails, iArr);
            } catch (IndexOutOfBoundsException e) {
                MtkCatLog.e(this, " IndexOutofBoundException in processSetUpEventList");
            }
        }
        return false;
    }

    protected boolean processLaunchBrowser(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        String strGsm8BitUnpackedToString;
        LaunchBrowserMode launchBrowserMode;
        MtkCatLog.d(this, "process LaunchBrowser");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.URL, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag != null) {
            try {
                byte[] rawValue = comprehensionTlvSearchForTag.getRawValue();
                int valueIndex = comprehensionTlvSearchForTag.getValueIndex();
                int length = comprehensionTlvSearchForTag.getLength();
                if (length > 0) {
                    strGsm8BitUnpackedToString = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex, length);
                } else {
                    strGsm8BitUnpackedToString = null;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } else {
            strGsm8BitUnpackedToString = null;
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
        switch (commandDetails.commandQualifier) {
            case 2:
                launchBrowserMode = LaunchBrowserMode.USE_EXISTING_BROWSER;
                break;
            case 3:
                launchBrowserMode = LaunchBrowserMode.LAUNCH_NEW_BROWSER;
                break;
            default:
                launchBrowserMode = LaunchBrowserMode.LAUNCH_IF_NOT_ALREADY_LAUNCHED;
                break;
        }
        this.mCmdParams = new LaunchBrowserParams(commandDetails, textMessage, strGsm8BitUnpackedToString, launchBrowserMode);
        if (iconIdRetrieveIconId != null) {
            this.mIconLoadState = 1;
            this.mIconLoader.loadIcon(iconIdRetrieveIconId.recordNumber, obtainMessage(1));
            return true;
        }
        return false;
    }

    protected boolean processPlayTone(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        Tone toneFromInt;
        Duration durationRetrieveDuration;
        MtkCatLog.d(this, "process PlayTone");
        TextMessage textMessage = new TextMessage();
        ComprehensionTlv comprehensionTlvSearchForTag = searchForTag(ComprehensionTlvTag.TONE, list);
        IconId iconIdRetrieveIconId = null;
        if (comprehensionTlvSearchForTag != null && comprehensionTlvSearchForTag.getLength() > 0) {
            try {
                toneFromInt = Tone.fromInt(comprehensionTlvSearchForTag.getRawValue()[comprehensionTlvSearchForTag.getValueIndex()]);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        } else {
            toneFromInt = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag2 = searchForTag(ComprehensionTlvTag.ALPHA_ID, list);
        if (comprehensionTlvSearchForTag2 != null) {
            try {
                textMessage.text = MtkValueParser.retrieveAlphaId(comprehensionTlvSearchForTag2);
            } catch (ResultException e2) {
                MtkCatLog.e(this, "retrieveAlphaId ResultException: " + e2.result());
            }
        }
        ComprehensionTlv comprehensionTlvSearchForTag3 = searchForTag(ComprehensionTlvTag.DURATION, list);
        if (comprehensionTlvSearchForTag3 != null) {
            try {
                durationRetrieveDuration = ValueParser.retrieveDuration(comprehensionTlvSearchForTag3);
            } catch (ResultException e3) {
                MtkCatLog.e(this, "retrieveDuration ResultException: " + e3.result());
                durationRetrieveDuration = null;
            }
        } else {
            durationRetrieveDuration = null;
        }
        ComprehensionTlv comprehensionTlvSearchForTag4 = searchForTag(ComprehensionTlvTag.ICON_ID, list);
        if (comprehensionTlvSearchForTag4 != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(comprehensionTlvSearchForTag4);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        }
        IconId iconId = iconIdRetrieveIconId;
        boolean z = (commandDetails.commandQualifier & 1) != 0;
        textMessage.responseNeeded = false;
        this.mCmdParams = new PlayToneParams(commandDetails, textMessage, toneFromInt, durationRetrieveDuration, z);
        if (iconId == null) {
            return false;
        }
        this.mIconLoadState = 1;
        this.mIconLoader.loadIcon(iconId.recordNumber, obtainMessage(1));
        return true;
    }

    protected boolean processSetupCall(CommandDetails commandDetails, List<ComprehensionTlv> list) throws ResultException {
        IconId iconIdRetrieveIconId;
        MtkCatLog.d(this, "process SetupCall");
        list.iterator();
        TextMessage textMessage = new TextMessage();
        TextMessage textMessage2 = new TextMessage();
        int addrIndex = getAddrIndex(list);
        if (-1 == addrIndex) {
            MtkCatLog.d(this, "fail to get ADDRESS data object");
            return false;
        }
        int confirmationAlphaIdIndex = getConfirmationAlphaIdIndex(list, addrIndex);
        int callingAlphaIdIndex = getCallingAlphaIdIndex(list, addrIndex);
        ComprehensionTlv confirmationAlphaId = getConfirmationAlphaId(list, addrIndex);
        if (confirmationAlphaId != null) {
            textMessage.text = MtkValueParser.retrieveAlphaId(confirmationAlphaId);
        }
        ComprehensionTlv confirmationIconId = getConfirmationIconId(list, confirmationAlphaIdIndex, callingAlphaIdIndex);
        IconId iconIdRetrieveIconId2 = null;
        if (confirmationIconId != null) {
            iconIdRetrieveIconId = ValueParser.retrieveIconId(confirmationIconId);
            textMessage.iconSelfExplanatory = iconIdRetrieveIconId.selfExplanatory;
        } else {
            iconIdRetrieveIconId = null;
        }
        ComprehensionTlv callingAlphaId = getCallingAlphaId(list, addrIndex);
        if (callingAlphaId != null) {
            textMessage2.text = MtkValueParser.retrieveAlphaId(callingAlphaId);
        }
        ComprehensionTlv callingIconId = getCallingIconId(list, callingAlphaIdIndex);
        if (callingIconId != null) {
            iconIdRetrieveIconId2 = ValueParser.retrieveIconId(callingIconId);
            textMessage2.iconSelfExplanatory = iconIdRetrieveIconId2.selfExplanatory;
        }
        this.mCmdParams = new CallSetupParams(commandDetails, textMessage, textMessage2);
        if (iconIdRetrieveIconId == null && iconIdRetrieveIconId2 == null) {
            return false;
        }
        this.mIconLoadState = 2;
        int[] iArr = new int[2];
        iArr[0] = iconIdRetrieveIconId != null ? iconIdRetrieveIconId.recordNumber : -1;
        iArr[1] = iconIdRetrieveIconId2 != null ? iconIdRetrieveIconId2.recordNumber : -1;
        this.mIconLoader.loadIcons(iArr, obtainMessage(1));
        return true;
    }

    private int getAddrIndex(List<ComprehensionTlv> list) {
        Iterator<ComprehensionTlv> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().getTag() == ComprehensionTlvTag.ADDRESS.value()) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private int getConfirmationAlphaIdIndex(List<ComprehensionTlv> list, int i) {
        Iterator<ComprehensionTlv> it = list.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            if (it.next().getTag() == ComprehensionTlvTag.ALPHA_ID.value() && i2 < i) {
                return i2;
            }
            i2++;
        }
        return -1;
    }

    private int getCallingAlphaIdIndex(List<ComprehensionTlv> list, int i) {
        Iterator<ComprehensionTlv> it = list.iterator();
        int i2 = 0;
        while (it.hasNext()) {
            if (it.next().getTag() == ComprehensionTlvTag.ALPHA_ID.value() && i2 > i) {
                return i2;
            }
            i2++;
        }
        return -1;
    }

    private ComprehensionTlv getConfirmationAlphaId(List<ComprehensionTlv> list, int i) {
        int i2 = 0;
        for (ComprehensionTlv comprehensionTlv : list) {
            if (comprehensionTlv.getTag() == ComprehensionTlvTag.ALPHA_ID.value() && i2 < i) {
                return comprehensionTlv;
            }
            i2++;
        }
        return null;
    }

    private ComprehensionTlv getCallingAlphaId(List<ComprehensionTlv> list, int i) {
        int i2 = 0;
        for (ComprehensionTlv comprehensionTlv : list) {
            if (comprehensionTlv.getTag() == ComprehensionTlvTag.ALPHA_ID.value() && i2 > i) {
                return comprehensionTlv;
            }
            i2++;
        }
        return null;
    }

    private ComprehensionTlv getConfirmationIconId(List<ComprehensionTlv> list, int i, int i2) {
        if (-1 == i) {
            return null;
        }
        int i3 = 0;
        for (ComprehensionTlv comprehensionTlv : list) {
            if (comprehensionTlv.getTag() == ComprehensionTlvTag.ICON_ID.value() && (-1 == i2 || i3 < i2)) {
                return comprehensionTlv;
            }
            i3++;
        }
        return null;
    }

    private ComprehensionTlv getCallingIconId(List<ComprehensionTlv> list, int i) {
        if (-1 == i) {
            return null;
        }
        int i2 = 0;
        for (ComprehensionTlv comprehensionTlv : list) {
            if (comprehensionTlv.getTag() == ComprehensionTlvTag.ICON_ID.value() && i2 > i) {
                return comprehensionTlv;
            }
            i2++;
        }
        return null;
    }
}
