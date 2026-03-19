package com.android.internal.telephony.cat;

import android.R;
import android.content.res.Resources;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public abstract class ValueParser {
    public static CommandDetails retrieveCommandDetails(ComprehensionTlv comprehensionTlv) throws ResultException {
        CommandDetails commandDetails = new CommandDetails();
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        try {
            commandDetails.compRequired = comprehensionTlv.isComprehensionRequired();
            commandDetails.commandNumber = rawValue[valueIndex] & 255;
            commandDetails.typeOfCommand = rawValue[valueIndex + 1] & 255;
            commandDetails.commandQualifier = rawValue[valueIndex + 2] & 255;
            return commandDetails;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    public static DeviceIdentities retrieveDeviceIdentities(ComprehensionTlv comprehensionTlv) throws ResultException {
        DeviceIdentities deviceIdentities = new DeviceIdentities();
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        try {
            deviceIdentities.sourceId = rawValue[valueIndex] & 255;
            deviceIdentities.destinationId = rawValue[valueIndex + 1] & 255;
            return deviceIdentities;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.REQUIRED_VALUES_MISSING);
        }
    }

    public static Duration retrieveDuration(ComprehensionTlv comprehensionTlv) throws ResultException {
        Duration.TimeUnit timeUnit = Duration.TimeUnit.SECOND;
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        try {
            return new Duration(rawValue[valueIndex + 1] & 255, Duration.TimeUnit.values()[rawValue[valueIndex] & 255]);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static Item retrieveItem(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int length = comprehensionTlv.getLength();
        if (length != 0) {
            try {
                return new Item(rawValue[valueIndex] & 255, IccUtils.adnStringFieldToString(rawValue, valueIndex + 1, length - 1));
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return null;
    }

    public static int retrieveItemId(ComprehensionTlv comprehensionTlv) throws ResultException {
        try {
            return comprehensionTlv.getRawValue()[comprehensionTlv.getValueIndex()] & 255;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    public static IconId retrieveIconId(ComprehensionTlv comprehensionTlv) throws ResultException {
        IconId iconId = new IconId();
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int i = valueIndex + 1;
        try {
            iconId.selfExplanatory = (rawValue[valueIndex] & 255) == 0;
            iconId.recordNumber = rawValue[i] & 255;
            return iconId;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    public static ItemsIconId retrieveItemsIconId(ComprehensionTlv comprehensionTlv) throws ResultException {
        CatLog.d("ValueParser", "retrieveItemsIconId:");
        ItemsIconId itemsIconId = new ItemsIconId();
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        boolean z = true;
        int length = comprehensionTlv.getLength() - 1;
        itemsIconId.recordNumbers = new int[length];
        int i = valueIndex + 1;
        try {
            int i2 = 0;
            if ((rawValue[valueIndex] & 255) != 0) {
                z = false;
            }
            itemsIconId.selfExplanatory = z;
            while (i2 < length) {
                int i3 = i2 + 1;
                int i4 = i + 1;
                itemsIconId.recordNumbers[i2] = rawValue[i];
                i2 = i3;
                i = i4;
            }
            return itemsIconId;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    public static List<TextAttribute> retrieveTextAttribute(ComprehensionTlv comprehensionTlv) throws ResultException {
        ArrayList arrayList = new ArrayList();
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int length = comprehensionTlv.getLength();
        if (length != 0) {
            int i = length / 4;
            int i2 = valueIndex;
            int i3 = 0;
            while (i3 < i) {
                try {
                    int i4 = rawValue[i2] & 255;
                    int i5 = rawValue[i2 + 1] & 255;
                    int i6 = rawValue[i2 + 2] & 255;
                    int i7 = rawValue[i2 + 3] & 255;
                    TextAlignment textAlignmentFromInt = TextAlignment.fromInt(i6 & 3);
                    FontSize fontSizeFromInt = FontSize.fromInt((i6 >> 2) & 3);
                    if (fontSizeFromInt == null) {
                        fontSizeFromInt = FontSize.NORMAL;
                    }
                    arrayList.add(new TextAttribute(i4, i5, textAlignmentFromInt, fontSizeFromInt, (i6 & 16) != 0, (i6 & 32) != 0, (i6 & 64) != 0, (i6 & 128) != 0, TextColor.fromInt(i7)));
                    i3++;
                    i2 += 4;
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            return arrayList;
        }
        return null;
    }

    public static String retrieveAlphaId(ComprehensionTlv comprehensionTlv) throws ResultException {
        boolean z;
        if (comprehensionTlv != null) {
            byte[] rawValue = comprehensionTlv.getRawValue();
            int valueIndex = comprehensionTlv.getValueIndex();
            int length = comprehensionTlv.getLength();
            if (length != 0) {
                try {
                    return IccUtils.adnStringFieldToString(rawValue, valueIndex, length);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            CatLog.d("ValueParser", "Alpha Id length=" + length);
            return null;
        }
        try {
            z = Resources.getSystem().getBoolean(R.^attr-private.outKeycode);
        } catch (Resources.NotFoundException e2) {
            z = false;
        }
        if (z) {
            return null;
        }
        return CatService.STK_DEFAULT;
    }

    public static String retrieveTextString(ComprehensionTlv comprehensionTlv) throws ResultException {
        byte[] rawValue = comprehensionTlv.getRawValue();
        int valueIndex = comprehensionTlv.getValueIndex();
        int length = comprehensionTlv.getLength();
        if (length == 0) {
            return null;
        }
        int i = length - 1;
        try {
            byte b = (byte) (rawValue[valueIndex] & 12);
            if (b == 0) {
                return GsmAlphabet.gsm7BitPackedToString(rawValue, valueIndex + 1, (i * 8) / 7);
            }
            if (b == 4) {
                return GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex + 1, i);
            }
            if (b == 8) {
                return new String(rawValue, valueIndex + 1, i, "UTF-16");
            }
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (UnsupportedEncodingException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (IndexOutOfBoundsException e2) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }
}
