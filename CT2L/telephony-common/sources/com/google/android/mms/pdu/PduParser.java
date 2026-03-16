package com.google.android.mms.pdu;

import android.util.Log;
import com.android.internal.telephony.RadioNVItems;
import com.android.internal.telephony.WspTypeDecoder;
import com.android.internal.telephony.cdma.SignalToneUtil;
import com.android.internal.telephony.gsm.CallFailCause;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class PduParser {
    static final boolean $assertionsDisabled;
    private static final boolean DEBUG = false;
    private static final int END_STRING_FLAG = 0;
    private static final int LENGTH_QUOTE = 31;
    private static final boolean LOCAL_LOGV = false;
    private static final String LOG_TAG = "PduParser";
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final int QUOTE = 127;
    private static final int QUOTED_STRING_FLAG = 34;
    private static final int SHORT_INTEGER_MAX = 127;
    private static final int SHORT_LENGTH_MAX = 30;
    private static final int TEXT_MAX = 127;
    private static final int TEXT_MIN = 32;
    private static final int THE_FIRST_PART = 0;
    private static final int THE_LAST_PART = 1;
    private static final int TYPE_QUOTED_STRING = 1;
    private static final int TYPE_TEXT_STRING = 0;
    private static final int TYPE_TOKEN_STRING = 2;
    private static byte[] mStartParam;
    private static byte[] mTypeParam;
    private final boolean mParseContentDisposition;
    private ByteArrayInputStream mPduDataStream;
    private PduHeaders mHeaders = null;
    private PduBody mBody = null;

    static {
        $assertionsDisabled = !PduParser.class.desiredAssertionStatus();
        mTypeParam = null;
        mStartParam = null;
    }

    public PduParser(byte[] pduDataStream, boolean parseContentDisposition) {
        this.mPduDataStream = null;
        this.mPduDataStream = new ByteArrayInputStream(pduDataStream);
        this.mParseContentDisposition = parseContentDisposition;
    }

    public GenericPdu parse() {
        if (this.mPduDataStream == null) {
            return null;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            return null;
        }
        int messageType = this.mHeaders.getOctet(140);
        if (!checkMandatoryHeader(this.mHeaders)) {
            log("check mandatory headers failed!");
            return null;
        }
        if (128 == messageType || 132 == messageType) {
            this.mBody = parseParts(this.mPduDataStream);
            if (this.mBody == null) {
                return null;
            }
        }
        switch (messageType) {
            case 128:
                SendReq sendReq = new SendReq(this.mHeaders, this.mBody);
                break;
            case 129:
                SendConf sendConf = new SendConf(this.mHeaders);
                break;
            case 130:
                NotificationInd notificationInd = new NotificationInd(this.mHeaders);
                break;
            case 131:
                NotifyRespInd notifyRespInd = new NotifyRespInd(this.mHeaders);
                break;
            case 132:
                RetrieveConf retrieveConf = new RetrieveConf(this.mHeaders, this.mBody);
                byte[] contentType = retrieveConf.getContentType();
                if (contentType != null) {
                    String ctTypeStr = new String(contentType);
                    if (!ctTypeStr.equals(ContentType.MULTIPART_MIXED) && !ctTypeStr.equals(ContentType.MULTIPART_RELATED) && !ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                        if (ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                            PduPart firstPart = this.mBody.getPart(0);
                            this.mBody.removeAll();
                            this.mBody.addPart(0, firstPart);
                        }
                        break;
                    }
                }
                break;
            case 133:
                AcknowledgeInd acknowledgeInd = new AcknowledgeInd(this.mHeaders);
                break;
            case 134:
                DeliveryInd deliveryInd = new DeliveryInd(this.mHeaders);
                break;
            case 135:
                ReadRecInd readRecInd = new ReadRecInd(this.mHeaders);
                break;
            case 136:
                ReadOrigInd readOrigInd = new ReadOrigInd(this.mHeaders);
                break;
            default:
                log("Parser doesn't support this message type in this version!");
                break;
        }
        return null;
    }

    protected PduHeaders parseHeaders(ByteArrayInputStream pduDataStream) {
        EncodedStringValue from;
        byte[] address;
        if (pduDataStream == null) {
            return null;
        }
        boolean keepParsing = true;
        PduHeaders headers = new PduHeaders();
        while (keepParsing && pduDataStream.available() > 0) {
            pduDataStream.mark(1);
            int headerField = extractByteValue(pduDataStream);
            if (headerField >= 32 && headerField <= 127) {
                pduDataStream.reset();
                parseWapString(pduDataStream, 0);
            } else {
                switch (headerField) {
                    case 129:
                    case 130:
                    case 151:
                        EncodedStringValue value = parseEncodedStringValue(pduDataStream);
                        if (value == null) {
                            continue;
                        } else {
                            byte[] address2 = value.getTextString();
                            if (address2 != null) {
                                String str = new String(address2);
                                int endIndex = str.indexOf("/");
                                if (endIndex > 0) {
                                    str = str.substring(0, endIndex);
                                }
                                try {
                                    value.setTextString(str.getBytes());
                                } catch (NullPointerException e) {
                                    log("null pointer error!");
                                    return null;
                                }
                            }
                            try {
                                headers.appendEncodedStringValue(value, headerField);
                            } catch (NullPointerException e2) {
                                log("null pointer error!");
                            } catch (RuntimeException e3) {
                                log(headerField + "is not Encoded-String-Value header field!");
                                return null;
                            }
                        }
                        break;
                    case 131:
                    case 139:
                    case 152:
                    case PduHeaders.REPLY_CHARGING_ID:
                    case PduHeaders.APPLIC_ID:
                    case PduHeaders.REPLY_APPLIC_ID:
                    case PduHeaders.AUX_APPLIC_ID:
                    case PduHeaders.REPLACE_ID:
                    case PduHeaders.CANCEL_ID:
                        byte[] value2 = parseWapString(pduDataStream, 0);
                        if (value2 != null) {
                            try {
                                headers.setTextString(value2, headerField);
                            } catch (NullPointerException e4) {
                                log("null pointer error!");
                            } catch (RuntimeException e5) {
                                log(headerField + "is not Text-String header field!");
                                return null;
                            }
                        }
                        break;
                    case 132:
                        HashMap<Integer, Object> map = new HashMap<>();
                        byte[] contentType = parseContentType(pduDataStream, map);
                        if (contentType != null) {
                            try {
                                headers.setTextString(contentType, 132);
                            } catch (NullPointerException e6) {
                                log("null pointer error!");
                            } catch (RuntimeException e7) {
                                log(headerField + "is not Text-String header field!");
                                return null;
                            }
                        }
                        mStartParam = (byte[]) map.get(153);
                        mTypeParam = (byte[]) map.get(131);
                        keepParsing = false;
                        break;
                    case 133:
                    case 142:
                    case PduHeaders.REPLY_CHARGING_SIZE:
                        try {
                            headers.setLongInteger(parseLongInteger(pduDataStream), headerField);
                        } catch (RuntimeException e8) {
                            log(headerField + "is not Long-Integer header field!");
                            return null;
                        }
                        break;
                    case 134:
                    case 143:
                    case 144:
                    case 145:
                    case 146:
                    case 148:
                    case 149:
                    case 153:
                    case 155:
                    case 156:
                    case PduHeaders.STORE:
                    case PduHeaders.MM_STATE:
                    case PduHeaders.STORE_STATUS:
                    case 167:
                    case 169:
                    case PduHeaders.QUOTAS:
                    case PduHeaders.DISTRIBUTION_INDICATOR:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE:
                    case PduHeaders.CONTENT_CLASS:
                    case PduHeaders.DRM_CONTENT:
                    case PduHeaders.ADAPTATION_ALLOWED:
                    case PduHeaders.CANCEL_STATUS:
                        int value3 = extractByteValue(pduDataStream);
                        try {
                            headers.setOctet(value3, headerField);
                        } catch (InvalidHeaderValueException e9) {
                            log("Set invalid Octet value: " + value3 + " into the header filed: " + headerField);
                            return null;
                        } catch (RuntimeException e10) {
                            log(headerField + "is not Octet header field!");
                            return null;
                        }
                        break;
                    case 135:
                    case 136:
                    case 157:
                        parseValueLength(pduDataStream);
                        int token = extractByteValue(pduDataStream);
                        try {
                            long timeValue = parseLongInteger(pduDataStream);
                            if (129 == token) {
                                timeValue += System.currentTimeMillis() / 1000;
                            }
                            try {
                                headers.setLongInteger(timeValue, headerField);
                            } catch (RuntimeException e11) {
                                log(headerField + "is not Long-Integer header field!");
                                return null;
                            }
                        } catch (RuntimeException e12) {
                            log(headerField + "is not Long-Integer header field!");
                            return null;
                        }
                        break;
                    case 137:
                        parseValueLength(pduDataStream);
                        int fromToken = extractByteValue(pduDataStream);
                        if (128 == fromToken) {
                            from = parseEncodedStringValue(pduDataStream);
                            if (from != null && (address = from.getTextString()) != null) {
                                String str2 = new String(address);
                                int endIndex2 = str2.indexOf("/");
                                if (endIndex2 > 0) {
                                    str2 = str2.substring(0, endIndex2);
                                }
                                try {
                                    from.setTextString(str2.getBytes());
                                } catch (NullPointerException e13) {
                                    log("null pointer error!");
                                    return null;
                                }
                            }
                        } else {
                            try {
                                from = new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes());
                            } catch (NullPointerException e14) {
                                log(headerField + "is not Encoded-String-Value header field!");
                                return null;
                            }
                        }
                        try {
                            headers.setEncodedStringValue(from, 137);
                        } catch (NullPointerException e15) {
                            log("null pointer error!");
                        } catch (RuntimeException e16) {
                            log(headerField + "is not Encoded-String-Value header field!");
                            return null;
                        }
                        break;
                    case 138:
                        pduDataStream.mark(1);
                        int messageClass = extractByteValue(pduDataStream);
                        if (messageClass >= 128) {
                            if (128 == messageClass) {
                                try {
                                    headers.setTextString(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes(), 138);
                                } catch (NullPointerException e17) {
                                    log("null pointer error!");
                                } catch (RuntimeException e18) {
                                    log(headerField + "is not Text-String header field!");
                                    return null;
                                }
                            } else if (129 == messageClass) {
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR.getBytes(), 138);
                            } else if (130 == messageClass) {
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR.getBytes(), 138);
                            } else if (131 == messageClass) {
                                headers.setTextString(PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes(), 138);
                            }
                        } else {
                            pduDataStream.reset();
                            byte[] messageClassString = parseWapString(pduDataStream, 0);
                            if (messageClassString != null) {
                                try {
                                    headers.setTextString(messageClassString, 138);
                                } catch (NullPointerException e19) {
                                    log("null pointer error!");
                                } catch (RuntimeException e20) {
                                    log(headerField + "is not Text-String header field!");
                                    return null;
                                }
                            }
                        }
                        break;
                    case 140:
                        int messageType = extractByteValue(pduDataStream);
                        switch (messageType) {
                            case 137:
                            case 138:
                            case 139:
                            case 140:
                            case 141:
                            case 142:
                            case 143:
                            case 144:
                            case 145:
                            case 146:
                            case 147:
                            case 148:
                            case 149:
                            case 150:
                            case 151:
                                return null;
                            default:
                                try {
                                    headers.setOctet(messageType, headerField);
                                } catch (InvalidHeaderValueException e21) {
                                    log("Set invalid Octet value: " + messageType + " into the header filed: " + headerField);
                                    return null;
                                } catch (RuntimeException e22) {
                                    log(headerField + "is not Octet header field!");
                                    return null;
                                }
                                break;
                        }
                        break;
                    case 141:
                        int version = parseShortInteger(pduDataStream);
                        try {
                            headers.setOctet(version, 141);
                        } catch (InvalidHeaderValueException e23) {
                            log("Set invalid Octet value: " + version + " into the header filed: " + headerField);
                            return null;
                        } catch (RuntimeException e24) {
                            log(headerField + "is not Octet header field!");
                            return null;
                        }
                        break;
                    case 147:
                    case 150:
                    case 154:
                    case PduHeaders.STORE_STATUS_TEXT:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE_TEXT:
                    case PduHeaders.STATUS_TEXT:
                        EncodedStringValue value4 = parseEncodedStringValue(pduDataStream);
                        if (value4 != null) {
                            try {
                                headers.setEncodedStringValue(value4, headerField);
                            } catch (NullPointerException e25) {
                                log("null pointer error!");
                            } catch (RuntimeException e26) {
                                log(headerField + "is not Encoded-String-Value header field!");
                                return null;
                            }
                        }
                        break;
                    case 160:
                        parseValueLength(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                            EncodedStringValue previouslySentBy = parseEncodedStringValue(pduDataStream);
                            if (previouslySentBy != null) {
                                try {
                                    headers.setEncodedStringValue(previouslySentBy, 160);
                                } catch (NullPointerException e27) {
                                    log("null pointer error!");
                                } catch (RuntimeException e28) {
                                    log(headerField + "is not Encoded-String-Value header field!");
                                    return null;
                                }
                            }
                        } catch (RuntimeException e29) {
                            log(headerField + " is not Integer-Value");
                            return null;
                        }
                        break;
                    case PduHeaders.PREVIOUSLY_SENT_DATE:
                        parseValueLength(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                            try {
                                long perviouslySentDate = parseLongInteger(pduDataStream);
                                headers.setLongInteger(perviouslySentDate, PduHeaders.PREVIOUSLY_SENT_DATE);
                            } catch (RuntimeException e30) {
                                log(headerField + "is not Long-Integer header field!");
                                return null;
                            }
                        } catch (RuntimeException e31) {
                            log(headerField + " is not Integer-Value");
                            return null;
                        }
                        break;
                    case PduHeaders.MM_FLAGS:
                        parseValueLength(pduDataStream);
                        extractByteValue(pduDataStream);
                        parseEncodedStringValue(pduDataStream);
                        break;
                    case 168:
                    case 174:
                    case 176:
                    default:
                        log("Unknown header");
                        break;
                    case 170:
                    case PduHeaders.MBOX_QUOTAS:
                        parseValueLength(pduDataStream);
                        extractByteValue(pduDataStream);
                        try {
                            parseIntegerValue(pduDataStream);
                        } catch (RuntimeException e32) {
                            log(headerField + " is not Integer-Value");
                            return null;
                        }
                        break;
                    case PduHeaders.MESSAGE_COUNT:
                    case PduHeaders.START:
                    case PduHeaders.LIMIT:
                        try {
                            headers.setLongInteger(parseIntegerValue(pduDataStream), headerField);
                        } catch (RuntimeException e33) {
                            log(headerField + "is not Long-Integer header field!");
                            return null;
                        }
                        break;
                    case PduHeaders.ELEMENT_DESCRIPTOR:
                        parseContentType(pduDataStream, null);
                        break;
                }
            }
        }
        return headers;
    }

    protected PduBody parseParts(ByteArrayInputStream pduDataStream) {
        if (pduDataStream == null) {
            return null;
        }
        int count = parseUnsignedInt(pduDataStream);
        PduBody body = new PduBody();
        for (int i = 0; i < count; i++) {
            int headerLength = parseUnsignedInt(pduDataStream);
            int dataLength = parseUnsignedInt(pduDataStream);
            PduPart part = new PduPart();
            int startPos = pduDataStream.available();
            if (startPos <= 0) {
                return null;
            }
            HashMap<Integer, Object> map = new HashMap<>();
            byte[] contentType = parseContentType(pduDataStream, map);
            if (contentType != null) {
                part.setContentType(contentType);
            } else {
                part.setContentType(PduContentTypes.contentTypes[0].getBytes());
            }
            byte[] name = (byte[]) map.get(151);
            if (name != null) {
                part.setName(name);
            }
            Integer charset = (Integer) map.get(129);
            if (charset != null) {
                part.setCharset(charset.intValue());
            }
            int endPos = pduDataStream.available();
            int partHeaderLen = headerLength - (startPos - endPos);
            if (partHeaderLen > 0) {
                if (!parsePartHeaders(pduDataStream, part, partHeaderLen)) {
                    return null;
                }
            } else if (partHeaderLen < 0) {
                return null;
            }
            if (part.getContentLocation() == null && part.getName() == null && part.getFilename() == null && part.getContentId() == null) {
                part.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
            }
            if (dataLength > 0) {
                byte[] partData = new byte[dataLength];
                String partContentType = new String(part.getContentType());
                pduDataStream.read(partData, 0, dataLength);
                if (partContentType.equalsIgnoreCase(ContentType.MULTIPART_ALTERNATIVE)) {
                    PduBody childBody = parseParts(new ByteArrayInputStream(partData));
                    part = childBody.getPart(0);
                } else {
                    byte[] partDataEncoding = part.getContentTransferEncoding();
                    if (partDataEncoding != null) {
                        String encoding = new String(partDataEncoding);
                        if (encoding.equalsIgnoreCase(PduPart.P_BASE64)) {
                            partData = Base64.decodeBase64(partData);
                        } else if (encoding.equalsIgnoreCase(PduPart.P_QUOTED_PRINTABLE)) {
                            partData = QuotedPrintable.decodeQuotedPrintable(partData);
                        }
                    }
                    if (partData == null) {
                        log("Decode part data error!");
                        return null;
                    }
                    part.setData(partData);
                }
            }
            if (checkPartPosition(part) == 0) {
                body.addPart(0, part);
            } else {
                body.addPart(part);
            }
        }
        return body;
    }

    private static void log(String text) {
    }

    protected static int parseUnsignedInt(ByteArrayInputStream pduDataStream) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        int result = 0;
        int temp = pduDataStream.read();
        if (temp == -1) {
            return temp;
        }
        while ((temp & 128) != 0) {
            result = (result << 7) | (temp & 127);
            temp = pduDataStream.read();
            if (temp == -1) {
                return temp;
            }
        }
        return (result << 7) | (temp & 127);
    }

    protected static int parseValueLength(ByteArrayInputStream pduDataStream) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        int first = temp & 255;
        if (first > 30) {
            if (first == 31) {
                return parseUnsignedInt(pduDataStream);
            }
            throw new RuntimeException("Value length > LENGTH_QUOTE!");
        }
        return first;
    }

    protected static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream pduDataStream) {
        EncodedStringValue returnValue;
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        pduDataStream.mark(1);
        int charset = 0;
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        int first = temp & 255;
        if (first == 0) {
            return new EncodedStringValue("");
        }
        pduDataStream.reset();
        if (first < 32) {
            parseValueLength(pduDataStream);
            charset = parseShortInteger(pduDataStream);
        }
        byte[] textString = parseWapString(pduDataStream, 0);
        try {
            if (charset != 0) {
                EncodedStringValue returnValue2 = new EncodedStringValue(charset, textString);
                returnValue = returnValue2;
            } else {
                EncodedStringValue returnValue3 = new EncodedStringValue(textString);
                returnValue = returnValue3;
            }
            return returnValue;
        } catch (Exception e) {
            return null;
        }
    }

    protected static byte[] parseWapString(ByteArrayInputStream pduDataStream, int stringType) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        if (1 == stringType && 34 == temp) {
            pduDataStream.mark(1);
        } else if (stringType == 0 && 127 == temp) {
            pduDataStream.mark(1);
        } else {
            pduDataStream.reset();
        }
        return getWapString(pduDataStream, stringType);
    }

    protected static boolean isTokenCharacter(int ch) {
        if (ch < 33 || ch > 126) {
            return false;
        }
        switch (ch) {
            case 34:
            case RadioNVItems.RIL_NV_MIP_PROFILE_MN_HA_SS:
            case 41:
            case CallFailCause.CHANNEL_NOT_AVAIL:
            case WspTypeDecoder.PARAMETER_ID_X_WAP_APPLICATION_ID:
            case 58:
            case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
            case 60:
            case 61:
            case 62:
            case SignalToneUtil.IS95_CONST_IR_SIG_TONE_NO_TONE:
            case 64:
            case 91:
            case 92:
            case 93:
            case 123:
            case 125:
                break;
        }
        return false;
    }

    protected static boolean isText(int ch) {
        if (ch >= 32 && ch <= 126) {
            return true;
        }
        if (ch >= 128 && ch <= 255) {
            return true;
        }
        switch (ch) {
        }
        return true;
    }

    protected static byte[] getWapString(ByteArrayInputStream pduDataStream, int stringType) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        while (-1 != temp && temp != 0) {
            if (stringType == 2) {
                if (isTokenCharacter(temp)) {
                    out.write(temp);
                }
            } else if (isText(temp)) {
                out.write(temp);
            }
            temp = pduDataStream.read();
            if (!$assertionsDisabled && -1 == temp) {
                throw new AssertionError();
            }
        }
        if (out.size() > 0) {
            return out.toByteArray();
        }
        return null;
    }

    protected static int extractByteValue(ByteArrayInputStream pduDataStream) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        int temp = pduDataStream.read();
        if ($assertionsDisabled || -1 != temp) {
            return temp & 255;
        }
        throw new AssertionError();
    }

    protected static int parseShortInteger(ByteArrayInputStream pduDataStream) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        int temp = pduDataStream.read();
        if ($assertionsDisabled || -1 != temp) {
            return temp & 127;
        }
        throw new AssertionError();
    }

    protected static long parseLongInteger(ByteArrayInputStream pduDataStream) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        int count = temp & 255;
        if (count > 8) {
            throw new RuntimeException("Octet count greater than 8 and I can't represent that!");
        }
        long result = 0;
        for (int i = 0; i < count; i++) {
            int temp2 = pduDataStream.read();
            if (!$assertionsDisabled && -1 == temp2) {
                throw new AssertionError();
            }
            result = (result << 8) + ((long) (temp2 & 255));
        }
        return result;
    }

    protected static long parseIntegerValue(ByteArrayInputStream pduDataStream) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        pduDataStream.reset();
        return temp > 127 ? parseShortInteger(pduDataStream) : parseLongInteger(pduDataStream);
    }

    protected static int skipWapValue(ByteArrayInputStream pduDataStream, int length) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        byte[] area = new byte[length];
        int readLen = pduDataStream.read(area, 0, length);
        if (readLen < length) {
            return -1;
        }
        return readLen;
    }

    protected static void parseContentTypeParams(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map, Integer length) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        if (!$assertionsDisabled && length.intValue() <= 0) {
            throw new AssertionError();
        }
        int startPos = pduDataStream.available();
        int lastLen = length.intValue();
        while (lastLen > 0) {
            int param = pduDataStream.read();
            if (!$assertionsDisabled && -1 == param) {
                throw new AssertionError();
            }
            lastLen--;
            switch (param) {
                case 129:
                    pduDataStream.mark(1);
                    int firstValue = extractByteValue(pduDataStream);
                    pduDataStream.reset();
                    if ((firstValue > 32 && firstValue < 127) || firstValue == 0) {
                        byte[] charsetStr = parseWapString(pduDataStream, 0);
                        try {
                            int charsetInt = CharacterSets.getMibEnumValue(new String(charsetStr));
                            map.put(129, Integer.valueOf(charsetInt));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(LOG_TAG, Arrays.toString(charsetStr), e);
                            map.put(129, 0);
                        }
                    } else {
                        int charset = (int) parseIntegerValue(pduDataStream);
                        if (map != null) {
                            map.put(129, Integer.valueOf(charset));
                        }
                    }
                    int tempPos = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos);
                    break;
                case 131:
                case 137:
                    pduDataStream.mark(1);
                    int first = extractByteValue(pduDataStream);
                    pduDataStream.reset();
                    if (first > 127) {
                        int index = parseShortInteger(pduDataStream);
                        if (index < PduContentTypes.contentTypes.length) {
                            map.put(131, PduContentTypes.contentTypes[index].getBytes());
                        }
                    } else {
                        byte[] type = parseWapString(pduDataStream, 0);
                        if (type != null && map != null) {
                            map.put(131, type);
                        }
                    }
                    int tempPos2 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos2);
                    break;
                case 133:
                case 151:
                    byte[] name = parseWapString(pduDataStream, 0);
                    if (name != null && map != null) {
                        map.put(151, name);
                    }
                    int tempPos3 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos3);
                    break;
                case 138:
                case 153:
                    byte[] start = parseWapString(pduDataStream, 0);
                    if (start != null && map != null) {
                        map.put(153, start);
                    }
                    int tempPos4 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos4);
                    break;
                default:
                    if (-1 == skipWapValue(pduDataStream, lastLen)) {
                        Log.e(LOG_TAG, "Corrupt Content-Type");
                    } else {
                        lastLen = 0;
                    }
                    break;
            }
        }
        if (lastLen != 0) {
            Log.e(LOG_TAG, "Corrupt Content-Type");
        }
    }

    protected static byte[] parseContentType(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map) {
        byte[] contentType;
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (!$assertionsDisabled && -1 == temp) {
            throw new AssertionError();
        }
        pduDataStream.reset();
        int cur = temp & 255;
        if (cur < 32) {
            int length = parseValueLength(pduDataStream);
            int startPos = pduDataStream.available();
            pduDataStream.mark(1);
            int temp2 = pduDataStream.read();
            if (!$assertionsDisabled && -1 == temp2) {
                throw new AssertionError();
            }
            pduDataStream.reset();
            int first = temp2 & 255;
            if (first >= 32 && first <= 127) {
                contentType = parseWapString(pduDataStream, 0);
            } else if (first > 127) {
                int index = parseShortInteger(pduDataStream);
                if (index < PduContentTypes.contentTypes.length) {
                    contentType = PduContentTypes.contentTypes[index].getBytes();
                } else {
                    pduDataStream.reset();
                    contentType = parseWapString(pduDataStream, 0);
                }
            } else {
                Log.e(LOG_TAG, "Corrupt content-type");
                return PduContentTypes.contentTypes[0].getBytes();
            }
            int endPos = pduDataStream.available();
            int parameterLen = length - (startPos - endPos);
            if (parameterLen > 0) {
                parseContentTypeParams(pduDataStream, map, Integer.valueOf(parameterLen));
            }
            if (parameterLen < 0) {
                Log.e(LOG_TAG, "Corrupt MMS message");
                return PduContentTypes.contentTypes[0].getBytes();
            }
        } else if (cur <= 127) {
            contentType = parseWapString(pduDataStream, 0);
        } else {
            contentType = PduContentTypes.contentTypes[parseShortInteger(pduDataStream)].getBytes();
        }
        return contentType;
    }

    protected boolean parsePartHeaders(ByteArrayInputStream pduDataStream, PduPart part, int length) {
        if (!$assertionsDisabled && pduDataStream == null) {
            throw new AssertionError();
        }
        if (!$assertionsDisabled && part == null) {
            throw new AssertionError();
        }
        if (!$assertionsDisabled && length <= 0) {
            throw new AssertionError();
        }
        int startPos = pduDataStream.available();
        int lastLen = length;
        while (lastLen > 0) {
            int header = pduDataStream.read();
            if (!$assertionsDisabled && -1 == header) {
                throw new AssertionError();
            }
            lastLen--;
            if (header > 127) {
                switch (header) {
                    case 142:
                        byte[] contentLocation = parseWapString(pduDataStream, 0);
                        if (contentLocation != null) {
                            part.setContentLocation(contentLocation);
                        }
                        int tempPos = pduDataStream.available();
                        lastLen = length - (startPos - tempPos);
                        break;
                    case 174:
                    case PduPart.P_CONTENT_DISPOSITION:
                        if (this.mParseContentDisposition) {
                            int len = parseValueLength(pduDataStream);
                            pduDataStream.mark(1);
                            int thisStartPos = pduDataStream.available();
                            int value = pduDataStream.read();
                            if (value == 128) {
                                part.setContentDisposition(PduPart.DISPOSITION_FROM_DATA);
                            } else if (value == 129) {
                                part.setContentDisposition(PduPart.DISPOSITION_ATTACHMENT);
                            } else if (value == 130) {
                                part.setContentDisposition(PduPart.DISPOSITION_INLINE);
                            } else {
                                pduDataStream.reset();
                                part.setContentDisposition(parseWapString(pduDataStream, 0));
                            }
                            if (thisStartPos - pduDataStream.available() < len) {
                                if (pduDataStream.read() == 152) {
                                    part.setFilename(parseWapString(pduDataStream, 0));
                                }
                                int thisEndPos = pduDataStream.available();
                                if (thisStartPos - thisEndPos < len) {
                                    int last = len - (thisStartPos - thisEndPos);
                                    byte[] temp = new byte[last];
                                    pduDataStream.read(temp, 0, last);
                                }
                            }
                            int tempPos2 = pduDataStream.available();
                            lastLen = length - (startPos - tempPos2);
                        }
                        break;
                    case 192:
                        byte[] contentId = parseWapString(pduDataStream, 1);
                        if (contentId != null) {
                            part.setContentId(contentId);
                        }
                        int tempPos3 = pduDataStream.available();
                        lastLen = length - (startPos - tempPos3);
                        break;
                    default:
                        if (-1 == skipWapValue(pduDataStream, lastLen)) {
                            Log.e(LOG_TAG, "Corrupt Part headers");
                            return false;
                        }
                        lastLen = 0;
                        break;
                        break;
                }
            } else if (header >= 32 && header <= 127) {
                byte[] tempHeader = parseWapString(pduDataStream, 0);
                byte[] tempValue = parseWapString(pduDataStream, 0);
                if (true == PduPart.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(new String(tempHeader))) {
                    part.setContentTransferEncoding(tempValue);
                }
                int tempPos4 = pduDataStream.available();
                lastLen = length - (startPos - tempPos4);
            } else {
                if (-1 == skipWapValue(pduDataStream, lastLen)) {
                    Log.e(LOG_TAG, "Corrupt Part headers");
                    return false;
                }
                lastLen = 0;
            }
        }
        if (lastLen != 0) {
            Log.e(LOG_TAG, "Corrupt Part headers");
            return false;
        }
        return true;
    }

    private static int checkPartPosition(PduPart part) {
        byte[] contentType;
        byte[] contentId;
        if (!$assertionsDisabled && part == null) {
            throw new AssertionError();
        }
        if (mTypeParam == null && mStartParam == null) {
            return 1;
        }
        if (mStartParam == null || (contentId = part.getContentId()) == null || true != Arrays.equals(mStartParam, contentId)) {
            return (mTypeParam == null || (contentType = part.getContentType()) == null || true != Arrays.equals(mTypeParam, contentType)) ? 1 : 0;
        }
        return 0;
    }

    protected static boolean checkMandatoryHeader(PduHeaders headers) {
        if (headers == null) {
            return false;
        }
        int messageType = headers.getOctet(140);
        int mmsVersion = headers.getOctet(141);
        if (mmsVersion == 0) {
            return false;
        }
        switch (messageType) {
            case 128:
                byte[] srContentType = headers.getTextString(132);
                if (srContentType == null) {
                    return false;
                }
                EncodedStringValue srFrom = headers.getEncodedStringValue(137);
                if (srFrom == null) {
                    return false;
                }
                byte[] srTransactionId = headers.getTextString(152);
                if (srTransactionId == null) {
                    return false;
                }
                return true;
            case 129:
                int scResponseStatus = headers.getOctet(146);
                if (scResponseStatus == 0) {
                    return false;
                }
                byte[] scTransactionId = headers.getTextString(152);
                if (scTransactionId == null) {
                    return false;
                }
                break;
            case 130:
                byte[] niContentLocation = headers.getTextString(131);
                if (niContentLocation == null) {
                    return false;
                }
                long niExpiry = headers.getLongInteger(136);
                if (-1 == niExpiry) {
                    return false;
                }
                byte[] niMessageClass = headers.getTextString(138);
                if (niMessageClass == null) {
                    return false;
                }
                long niMessageSize = headers.getLongInteger(142);
                if (-1 == niMessageSize) {
                    return false;
                }
                byte[] niTransactionId = headers.getTextString(152);
                if (niTransactionId == null) {
                    return false;
                }
                break;
            case 131:
                int nriStatus = headers.getOctet(149);
                if (nriStatus == 0) {
                    return false;
                }
                byte[] nriTransactionId = headers.getTextString(152);
                if (nriTransactionId == null) {
                    return false;
                }
                break;
            case 132:
                byte[] rcContentType = headers.getTextString(132);
                if (rcContentType == null) {
                    return false;
                }
                long rcDate = headers.getLongInteger(133);
                if (-1 == rcDate) {
                    return false;
                }
                break;
            case 133:
                byte[] aiTransactionId = headers.getTextString(152);
                if (aiTransactionId == null) {
                    return false;
                }
                break;
            case 134:
                long diDate = headers.getLongInteger(133);
                if (-1 == diDate) {
                    return false;
                }
                byte[] diMessageId = headers.getTextString(139);
                if (diMessageId == null) {
                    return false;
                }
                int diStatus = headers.getOctet(149);
                if (diStatus == 0) {
                    return false;
                }
                EncodedStringValue[] diTo = headers.getEncodedStringValues(151);
                if (diTo == null) {
                    return false;
                }
                break;
            case 135:
                EncodedStringValue rrFrom = headers.getEncodedStringValue(137);
                if (rrFrom == null) {
                    return false;
                }
                byte[] rrMessageId = headers.getTextString(139);
                if (rrMessageId == null) {
                    return false;
                }
                int rrReadStatus = headers.getOctet(155);
                if (rrReadStatus == 0) {
                    return false;
                }
                EncodedStringValue[] rrTo = headers.getEncodedStringValues(151);
                if (rrTo == null) {
                    return false;
                }
                break;
            case 136:
                long roDate = headers.getLongInteger(133);
                if (-1 == roDate) {
                    return false;
                }
                EncodedStringValue roFrom = headers.getEncodedStringValue(137);
                if (roFrom == null) {
                    return false;
                }
                byte[] roMessageId = headers.getTextString(139);
                if (roMessageId == null) {
                    return false;
                }
                int roReadStatus = headers.getOctet(155);
                if (roReadStatus == 0) {
                    return false;
                }
                EncodedStringValue[] roTo = headers.getEncodedStringValues(151);
                if (roTo == null) {
                    return false;
                }
                break;
            default:
                return false;
        }
    }
}
