package com.android.internal.telephony.cat;

import android.R;
import android.content.res.Resources;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

abstract class ValueParser {
    ValueParser() {
    }

    static CommandDetails retrieveCommandDetails(ComprehensionTlv ctlv) throws ResultException {
        CommandDetails cmdDet = new CommandDetails();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            cmdDet.compRequired = ctlv.isComprehensionRequired();
            cmdDet.commandNumber = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            cmdDet.typeOfCommand = rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID;
            cmdDet.commandQualifier = rawValue[valueIndex + 2] & PplMessageManager.Type.INVALID;
            return cmdDet;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static DeviceIdentities retrieveDeviceIdentities(ComprehensionTlv ctlv) throws ResultException {
        DeviceIdentities devIds = new DeviceIdentities();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            devIds.sourceId = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            devIds.destinationId = rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID;
            return devIds;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
    }

    static Duration retrieveDuration(ComprehensionTlv ctlv) throws ResultException {
        Duration.TimeUnit timeUnit = Duration.TimeUnit.SECOND;
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            Duration.TimeUnit timeUnit2 = Duration.TimeUnit.valuesCustom()[rawValue[valueIndex] & PplMessageManager.Type.INVALID];
            int timeInterval = rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID;
            return new Duration(timeInterval, timeUnit2);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static Item retrieveItem(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length == 0) {
            return null;
        }
        int textLen = length - 1;
        try {
            int id = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            String text = IccUtils.adnStringFieldToString(rawValue, valueIndex + 1, removeInvalidCharInItemTextString(rawValue, valueIndex, textLen));
            Item item = new Item(id, text);
            return item;
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("ValueParser", "retrieveItem fail");
            return null;
        }
    }

    static int removeInvalidCharInItemTextString(byte[] rawValue, int valueIndex, int textLen) {
        Boolean isucs2 = false;
        int len = textLen;
        CatLog.d("ValueParser", "Try to remove invalid raw data 0xf0, valueIndex: " + valueIndex + ", textLen: " + textLen);
        if ((textLen >= 1 && rawValue[valueIndex + 1] == -128) || ((textLen >= 3 && rawValue[valueIndex + 1] == -127) || (textLen >= 4 && rawValue[valueIndex + 1] == -126))) {
            isucs2 = true;
        }
        CatLog.d("ValueParser", "Is the text string format UCS2? " + isucs2);
        if (!isucs2.booleanValue() && textLen > 0) {
            for (int i = textLen; i > 0 && rawValue[valueIndex + i] == -16; i--) {
                CatLog.d("ValueParser", "find invalid raw data 0xf0");
                len--;
            }
        }
        CatLog.d("ValueParser", "new textLen: " + len);
        return len;
    }

    static int retrieveItemId(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int id = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            return id;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static IconId retrieveIconId(ComprehensionTlv ctlv) throws ResultException {
        IconId id = new IconId();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int valueIndex2 = valueIndex + 1;
        try {
            id.selfExplanatory = (rawValue[valueIndex] & PplMessageManager.Type.INVALID) == 0;
            id.recordNumber = rawValue[valueIndex2] & PplMessageManager.Type.INVALID;
            return id;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static ItemsIconId retrieveItemsIconId(ComprehensionTlv ctlv) throws ResultException {
        CatLog.d("ValueParser", "retrieveItemsIconId:");
        ItemsIconId id = new ItemsIconId();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int numOfItems = ctlv.getLength() - 1;
        id.recordNumbers = new int[numOfItems];
        int valueIndex2 = valueIndex + 1;
        try {
            id.selfExplanatory = (rawValue[valueIndex] & PplMessageManager.Type.INVALID) == 0;
            int index = 0;
            while (index < numOfItems) {
                int index2 = index + 1;
                int valueIndex3 = valueIndex2 + 1;
                try {
                    id.recordNumbers[index] = rawValue[valueIndex2];
                    index = index2;
                    valueIndex2 = valueIndex3;
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            return id;
        } catch (IndexOutOfBoundsException e2) {
        }
    }

    static List<TextAttribute> retrieveTextAttribute(ComprehensionTlv ctlv) throws ResultException {
        ArrayList<TextAttribute> lst = new ArrayList<>();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length != 0) {
            int itemCount = length / 4;
            int i = 0;
            while (i < itemCount) {
                try {
                    int start = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
                    int textLength = rawValue[valueIndex + 1] & PplMessageManager.Type.INVALID;
                    int format = rawValue[valueIndex + 2] & PplMessageManager.Type.INVALID;
                    int colorValue = rawValue[valueIndex + 3] & PplMessageManager.Type.INVALID;
                    int alignValue = format & 3;
                    TextAlignment align = TextAlignment.fromInt(alignValue);
                    int sizeValue = (format >> 2) & 3;
                    FontSize size = FontSize.fromInt(sizeValue);
                    if (size == null) {
                        size = FontSize.NORMAL;
                    }
                    boolean bold = (format & 16) != 0;
                    boolean italic = (format & 32) != 0;
                    boolean underlined = (format & 64) != 0;
                    boolean strikeThrough = (format & 128) != 0;
                    TextColor color = TextColor.fromInt(colorValue);
                    TextAttribute attr = new TextAttribute(start, textLength, align, size, bold, italic, underlined, strikeThrough, color);
                    lst.add(attr);
                    i++;
                    valueIndex += 4;
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            return lst;
        }
        return null;
    }

    static String retrieveAlphaId(ComprehensionTlv ctlv) throws ResultException {
        boolean noAlphaUsrCnf;
        if (ctlv != null) {
            byte[] rawValue = ctlv.getRawValue();
            int valueIndex = ctlv.getValueIndex();
            int length = ctlv.getLength();
            if (length != 0) {
                try {
                    return IccUtils.adnStringFieldToString(rawValue, valueIndex, length);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            CatLog.d("ValueParser", "Alpha Id length=" + length);
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        Resources resource = Resources.getSystem();
        try {
            noAlphaUsrCnf = resource.getBoolean(R.^attr-private.lightRadius);
        } catch (Resources.NotFoundException e2) {
            noAlphaUsrCnf = false;
        }
        if (noAlphaUsrCnf) {
            return null;
        }
        return "Default Message";
    }

    static String retrieveTextString(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int textLen = ctlv.getLength();
        if (textLen == 0) {
            return null;
        }
        int textLen2 = textLen - 1;
        try {
            byte codingScheme = (byte) (rawValue[valueIndex] & 12);
            if (codingScheme == 0) {
                String text = GsmAlphabet.gsm7BitPackedToString(rawValue, valueIndex + 1, (textLen2 * 8) / 7);
                return text;
            }
            if (codingScheme == 4) {
                String text2 = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex + 1, textLen2);
                return text2;
            }
            if (codingScheme == 8) {
                String text3 = new String(rawValue, valueIndex + 1, textLen2, "UTF-16");
                return text3;
            }
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnsupportedEncodingException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (IndexOutOfBoundsException e2) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveNextActionIndicator(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        byte[] nai = new byte[length];
        int index = 0;
        int valueIndex2 = valueIndex;
        while (index < length) {
            int index2 = index + 1;
            int valueIndex3 = valueIndex2 + 1;
            try {
                nai[index] = rawValue[valueIndex2];
                index = index2;
                valueIndex2 = valueIndex3;
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return nai;
    }

    static int retrieveTarget(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int target = rawValue[valueIndex] & PplMessageManager.Type.INVALID;
            return target;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }
}
