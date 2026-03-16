package com.android.internal.telephony.cat;

import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.cat.BearerDescription;
import com.android.internal.telephony.cat.Duration;
import com.android.internal.telephony.cat.InterfaceTransportLevel;
import com.android.internal.telephony.uicc.IccUtils;
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
            cmdDet.commandNumber = rawValue[valueIndex] & 255;
            cmdDet.typeOfCommand = rawValue[valueIndex + 1] & 255;
            cmdDet.commandQualifier = rawValue[valueIndex + 2] & 255;
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
            devIds.sourceId = rawValue[valueIndex] & 255;
            devIds.destinationId = rawValue[valueIndex + 1] & 255;
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
            Duration.TimeUnit timeUnit2 = Duration.TimeUnit.values()[rawValue[valueIndex] & 255];
            int timeInterval = rawValue[valueIndex + 1] & 255;
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
            int id = rawValue[valueIndex] & 255;
            String text = IccUtils.adnStringFieldToString(rawValue, valueIndex + 1, textLen);
            Item item = new Item(id, text);
            return item;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveItemId(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int id = rawValue[valueIndex] & 255;
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
            id.selfExplanatory = (rawValue[valueIndex] & 255) == 0;
            id.recordNumber = rawValue[valueIndex2] & 255;
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
            id.selfExplanatory = (rawValue[valueIndex] & 255) == 0;
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

    static List<TextAttribute> retrieveItemTextAttributeList(ComprehensionTlv ctlv) throws ResultException {
        ArrayList<TextAttribute> lst = new ArrayList<>();
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length != 0) {
            int itemCount = length / 4;
            int i = 0;
            while (i < itemCount) {
                try {
                    int start = rawValue[valueIndex] & 255;
                    int textLength = rawValue[valueIndex + 1] & 255;
                    int format = rawValue[valueIndex + 2] & 255;
                    int colorValue = rawValue[valueIndex + 3] & 15;
                    int colorBackGround = (rawValue[valueIndex + 3] >> 4) & 15;
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
                    TextColor colorBG = TextColor.fromInt(colorBackGround);
                    TextAttribute attr = new TextAttribute(start, textLength, align, size, bold, italic, underlined, strikeThrough, color, colorBG);
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

    static TextAttribute retrieveTextAttribute(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length != 0) {
            try {
                int start = rawValue[valueIndex] & 255;
                int textLength = rawValue[valueIndex + 1] & 255;
                int format = rawValue[valueIndex + 2] & 255;
                int colorValue = rawValue[valueIndex + 3] & 15;
                int colorBackGround = (rawValue[valueIndex + 3] >> 4) & 15;
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
                TextColor colorBG = TextColor.fromInt(colorBackGround);
                return new TextAttribute(start, textLength, align, size, bold, italic, underlined, strikeThrough, color, colorBG);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return null;
    }

    static String retrieveAlphaId(ComprehensionTlv ctlv) throws ResultException {
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
            return null;
        }
        return "Default Message";
    }

    static String retrieveTextString(ComprehensionTlv ctlv) throws ResultException {
        String text;
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
                text = GsmAlphabet.gsm7BitPackedToString(rawValue, valueIndex + 1, (textLen2 * 8) / 7);
            } else if (codingScheme == 4) {
                text = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex + 1, textLen2);
            } else if (codingScheme == 8) {
                text = new String(rawValue, valueIndex + 1, textLen2, "UTF-16");
            } else {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            return text;
        } catch (UnsupportedEncodingException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        } catch (IndexOutOfBoundsException e2) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int[] retrieveFileList(ComprehensionTlv ctlv) throws ResultException {
        CatLog.d("ValueParser", "retrieveFileList");
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int valueLen = ctlv.getLength();
        int i = 3;
        if (valueLen == 0) {
            return null;
        }
        try {
            int fileNumber = rawValue[valueIndex] & 255;
            CatLog.d("retrieveFileList", "fileNumber = " + fileNumber + ",valueLen = " + valueLen + ",valueIndex = " + valueIndex);
            int[] fileList = new int[fileNumber];
            for (int fileIdCount = 0; fileIdCount < fileNumber - 1; fileIdCount++) {
                int tempValue = rawValue[valueIndex + i] << 8;
                int tempValue2 = tempValue | rawValue[valueIndex + i + 1];
                int filePathTag = rawValue[(valueIndex + i) + 2] << 8;
                int filePathTag2 = filePathTag | rawValue[valueIndex + i + 3];
                CatLog.d("ValueParser", "fileList[" + fileIdCount + "] = " + fileList[fileIdCount]);
                if (filePathTag2 == 16128) {
                    fileList[fileIdCount] = tempValue2;
                    i += 4;
                    CatLog.d("ValueParser", "meet filePathTag = " + filePathTag2 + "now get file is " + fileList[fileIdCount] + "index = " + i);
                } else {
                    fileList[fileIdCount] = ((tempValue2 << 16) & (-65536)) | (filePathTag2 & 65535);
                    i += 6;
                    CatLog.d("ValueParser", "filePath more than two bytes, now file is" + fileList[fileIdCount] + "index = " + i);
                }
            }
            if (valueLen - i == 2) {
                fileList[fileNumber - 1] = rawValue[valueIndex + i];
                int i2 = fileNumber - 1;
                fileList[i2] = fileList[i2] | rawValue[valueIndex + i + 1];
                return fileList;
            }
            if (valueLen - i == 4) {
                int tempValue3 = rawValue[valueIndex + i] << 8;
                int tempValue4 = tempValue3 | rawValue[valueIndex + i + 1];
                int filePathTag3 = rawValue[(valueIndex + i) + 2] << 8;
                fileList[fileNumber - 1] = ((tempValue4 << 16) & (-65536)) | ((filePathTag3 | rawValue[valueIndex + i + 3]) & 65535);
                return fileList;
            }
            return fileList;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveBufferSize(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int sz = (rawValue[valueIndex] & 255) << 8;
            return sz | (rawValue[valueIndex + 1] & 255);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static InterfaceTransportLevel retrieveInterfaceTransportLevel(ComprehensionTlv ctlv) throws ResultException {
        InterfaceTransportLevel.TransportProtocol transportProtocol = InterfaceTransportLevel.TransportProtocol.TCP_SERVER;
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            InterfaceTransportLevel.TransportProtocol protocol = InterfaceTransportLevel.TransportProtocol.values()[rawValue[valueIndex] & 255];
            int port = (rawValue[valueIndex + 1] & 255) << 8;
            return new InterfaceTransportLevel(port | (rawValue[valueIndex + 2] & 255), protocol);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static int retrieveChannelDataLength(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int len = rawValue[valueIndex] & 255;
            return len;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static byte[] retrieveChannelData(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        byte[] data = new byte[ctlv.getLength()];
        System.arraycopy(rawValue, valueIndex, data, 0, data.length);
        return data;
    }

    static byte[] retrieveOtherAddress(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            int addrType = rawValue[valueIndex] & 255;
            if (addrType != 33 && addrType != 87) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            if ((addrType == 33 && ctlv.getLength() != 5) || (addrType == 87 && ctlv.getLength() != 17)) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            byte[] addr = new byte[ctlv.getLength() - 1];
            System.arraycopy(rawValue, valueIndex + 1, addr, 0, addr.length);
            return addr;
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static String retrieveNetworkAccessName(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        String networkAccessName = null;
        int len = valueIndex + ctlv.getLength();
        while (valueIndex < len) {
            try {
                byte labelLen = rawValue[valueIndex];
                if (labelLen <= 0) {
                    break;
                }
                String label = GsmAlphabet.gsm8BitUnpackedToString(rawValue, valueIndex + 1, labelLen);
                valueIndex += labelLen + 1;
                if (networkAccessName == null) {
                    networkAccessName = label;
                } else {
                    networkAccessName = networkAccessName + "." + label;
                }
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        return networkAccessName;
    }

    static BearerDescription retrieveBearerDescription(ComprehensionTlv ctlv) throws ResultException {
        BearerDescription.BearerType type = null;
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        try {
            BearerDescription.BearerType[] arr$ = BearerDescription.BearerType.values();
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                BearerDescription.BearerType bt = arr$[i$];
                if (bt.value() != rawValue[valueIndex]) {
                    i$++;
                } else {
                    type = bt;
                    break;
                }
            }
            if (type == null) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
            byte[] parameters = new byte[ctlv.getLength() - 1];
            System.arraycopy(rawValue, valueIndex + 1, parameters, 0, parameters.length);
            return new BearerDescription(type, parameters);
        } catch (IndexOutOfBoundsException e) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }

    static String retrieveDTMFString(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length != 0) {
            try {
                return PhoneNumberUtils.calledPartyBCDFragmentToString(rawValue, valueIndex, length);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        CatLog.d("ValueParser", "Send DTMF String length=" + length);
        return null;
    }

    static String retrieveSSString(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        if (length != 0) {
            try {
                return PhoneNumberUtils.calledPartyBCDToString(rawValue, valueIndex, length);
            } catch (IndexOutOfBoundsException e) {
                throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
            }
        }
        CatLog.d("ValueParser", "Send SS String length=" + length);
        return null;
    }

    static byte[] retrieveSMSTPDUData(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        byte[] data = new byte[ctlv.getLength()];
        System.arraycopy(rawValue, valueIndex, data, 0, data.length);
        return data;
    }

    static String retrieveSMSAddress(ComprehensionTlv ctlv) throws ResultException {
        byte[] rawValue = ctlv.getRawValue();
        int valueIndex = ctlv.getValueIndex();
        int length = ctlv.getLength();
        try {
            int i = rawValue[valueIndex] & 255;
            if (length != 0) {
                try {
                    return PhoneNumberUtils.calledPartyBCDToString(rawValue, valueIndex, length);
                } catch (IndexOutOfBoundsException e) {
                    throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
                }
            }
            CatLog.d("ValueParser", "Send SMS Adress length=" + length);
            return null;
        } catch (IndexOutOfBoundsException e2) {
            throw new ResultException(ResultCode.CMD_DATA_NOT_UNDERSTOOD);
        }
    }
}
