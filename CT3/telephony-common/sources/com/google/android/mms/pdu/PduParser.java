package com.google.android.mms.pdu;

import android.util.Log;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.RadioNVItems;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class PduParser {

    static final boolean f37assertionsDisabled;
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
    private static final int UNSIGNED_INT_LIMIT = 2;
    private static byte[] mStartParam;
    private static byte[] mTypeParam;
    private final boolean mParseContentDisposition;
    private ByteArrayInputStream mPduDataStream;
    private PduHeaders mHeaders = null;
    private PduBody mBody = null;
    private boolean mForRestore = false;

    static {
        f37assertionsDisabled = !PduParser.class.desiredAssertionStatus();
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
            Log.i(LOG_TAG, "Input parse stream is null");
            return null;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            Log.i(LOG_TAG, "Parse PduHeader Failed");
            return null;
        }
        int messageType = this.mHeaders.getOctet(140);
        if (!checkMandatoryHeader(this.mHeaders)) {
            log("check mandatory headers failed!");
            return null;
        }
        this.mPduDataStream.mark(1);
        int count = parseUnsignedInt(this.mPduDataStream);
        this.mPduDataStream.reset();
        if (132 == messageType && count >= 2) {
            byte[] contentType = this.mHeaders.getTextString(132);
            if (contentType == null) {
                Log.i(LOG_TAG, "Parse MESSAGE_TYPE_RETRIEVE_CONF Failed: content Type is null _0");
                return null;
            }
            String contentTypeStr = new String(contentType).toLowerCase();
            if (!contentTypeStr.equals(ContentType.MULTIPART_MIXED) && !contentTypeStr.equals(ContentType.MULTIPART_RELATED) && !contentTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE) && contentTypeStr.equals(ContentType.TEXT_PLAIN)) {
                Log.i(LOG_TAG, "Content Type is text/plain");
                PduPart theOnlyPart = new PduPart();
                theOnlyPart.setContentType(contentType);
                theOnlyPart.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
                theOnlyPart.setContentId("<part1>".getBytes());
                this.mPduDataStream.mark(1);
                int partDataLen = 0;
                while (this.mPduDataStream.read() != -1) {
                    partDataLen++;
                }
                byte[] partData = new byte[partDataLen];
                Log.i(LOG_TAG, "got part length: " + partDataLen);
                this.mPduDataStream.reset();
                this.mPduDataStream.read(partData, 0, partDataLen);
                String showData = new String(partData);
                Log.i(LOG_TAG, "show data: " + showData);
                theOnlyPart.setData(partData);
                Log.i(LOG_TAG, "setData finish");
                PduBody onlyPartBody = new PduBody();
                onlyPartBody.addPart(theOnlyPart);
                RetrieveConf retrieveConf = null;
                try {
                    retrieveConf = new RetrieveConf(this.mHeaders, onlyPartBody);
                } catch (Exception e) {
                    Log.i(LOG_TAG, "new RetrieveConf has exception");
                }
                if (retrieveConf == null) {
                    Log.i(LOG_TAG, "retrieveConf is null");
                }
                return retrieveConf;
            }
        }
        if (128 == messageType || 132 == messageType) {
            this.mBody = parseParts(this.mPduDataStream);
            if (this.mBody == null) {
                Log.i(LOG_TAG, "Parse parts Failed");
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
                RetrieveConf retrieveConf2 = new RetrieveConf(this.mHeaders, this.mBody);
                byte[] contentType2 = retrieveConf2.getContentType();
                if (contentType2 == null) {
                    Log.i(LOG_TAG, "Parse MESSAGE_TYPE_RETRIEVE_CONF Failed: content Type is null");
                    break;
                } else {
                    String ctTypeStr = new String(contentType2).toLowerCase();
                    if (!ctTypeStr.equals(ContentType.MULTIPART_MIXED) && !ctTypeStr.equals(ContentType.MULTIPART_RELATED) && !ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE) && !ctTypeStr.equals(ContentType.TEXT_PLAIN)) {
                        if (!ctTypeStr.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                            Log.i(LOG_TAG, "Parse MESSAGE_TYPE_RETRIEVE_CONF Failed: content Type is null _2");
                        } else {
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
                            long value3 = parseLongInteger(pduDataStream);
                            if (headerField == 133) {
                                headers.setLongInteger(value3, PduHeaders.DATE_SENT);
                                if (!this.mForRestore) {
                                    value3 = System.currentTimeMillis() / 1000;
                                }
                            }
                            headers.setLongInteger(value3, headerField);
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
                    case PduHeaders.TOTALS:
                    case PduHeaders.QUOTAS:
                    case PduHeaders.DISTRIBUTION_INDICATOR:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE:
                    case PduHeaders.CONTENT_CLASS:
                    case PduHeaders.DRM_CONTENT:
                    case PduHeaders.ADAPTATION_ALLOWED:
                    case PduHeaders.CANCEL_STATUS:
                        int value4 = extractByteValue(pduDataStream);
                        try {
                            headers.setOctet(value4, headerField);
                        } catch (InvalidHeaderValueException e9) {
                            log("Set invalid Octet value: " + value4 + " into the header filed: " + headerField);
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
                                headers.setTextString("auto".getBytes(), 138);
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
                                Log.i(LOG_TAG, "PduParser: parseHeaders: We don't support these kind of messages now.");
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
                        EncodedStringValue value5 = parseEncodedStringValue(pduDataStream);
                        if (value5 != null) {
                            try {
                                headers.setEncodedStringValue(value5, headerField);
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
                    case PduHeaders.ATTRIBUTES:
                    case 174:
                    case 176:
                    default:
                        log("Unknown header");
                        break;
                    case PduHeaders.MBOX_TOTALS:
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
                Log.i(LOG_TAG, "PduParser: parseParts: Invalid part.");
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
                    Log.i(LOG_TAG, "PduParser: parseParts: Parse part header faild.");
                    return null;
                }
            } else if (partHeaderLen < 0) {
                Log.i(LOG_TAG, "PduParser: parseParts: Invalid length of content-type");
                return null;
            }
            if (part.getContentLocation() == null && part.getName() == null && part.getFilename() == null && part.getContentId() == null) {
                Log.i(LOG_TAG, "PduParser: parseParts: Hasn't find ContentLocation,so generate one.");
                part.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
            }
            if (dataLength > 0) {
                byte[] partData = new byte[dataLength];
                String partContentType = new String(part.getContentType());
                int readLen = pduDataStream.read(partData, 0, dataLength);
                if (readLen != dataLength) {
                    Log.d(LOG_TAG, "len=" + dataLength + " readLen=" + readLen);
                    return null;
                }
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
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        int result = 0;
        int temp = pduDataStream.read();
        if (temp == -1) {
            return temp;
        }
        while ((temp & 128) != 0) {
            result = (result << 7) | (temp & CallFailCause.INTERWORKING_UNSPECIFIED);
            temp = pduDataStream.read();
            if (temp == -1) {
                return temp;
            }
        }
        return (result << 7) | (temp & CallFailCause.INTERWORKING_UNSPECIFIED);
    }

    protected static int parseValueLength(ByteArrayInputStream pduDataStream) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        int first = temp & 255;
        if (first <= 30) {
            return first;
        }
        if (first == 31) {
            return parseUnsignedInt(pduDataStream);
        }
        throw new RuntimeException("Value length > LENGTH_QUOTE!");
    }

    protected static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream pduDataStream) {
        EncodedStringValue returnValue;
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        pduDataStream.mark(1);
        int charset = 0;
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        int first = temp & 255;
        if (first == 0) {
            return new EncodedStringValue(UsimPBMemInfo.STRING_NOT_SET);
        }
        pduDataStream.reset();
        if (first < 32) {
            parseValueLength(pduDataStream);
            charset = parseShortInteger(pduDataStream);
        }
        byte[] textString = parseWapString(pduDataStream, 0);
        try {
            if (charset != 0) {
                returnValue = new EncodedStringValue(charset, textString);
            } else {
                returnValue = new EncodedStringValue(textString);
            }
            return returnValue;
        } catch (Exception e) {
            return null;
        }
    }

    protected static byte[] parseWapString(ByteArrayInputStream pduDataStream, int stringType) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
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
            case 40:
            case 41:
            case CallFailCause.CHANNEL_NOT_AVAIL:
            case 47:
            case 58:
            case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
            case 60:
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_2:
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_3:
            case 63:
            case 64:
            case CallFailCause.INVALID_TRANSIT_NETWORK_SELECTION:
            case 92:
            case 93:
            case 123:
            case 125:
                break;
        }
        return false;
    }

    protected static boolean isText(int ch) {
        if ((ch >= 32 && ch <= 126) || (ch >= 128 && ch <= 255)) {
            return true;
        }
        switch (ch) {
        }
        return true;
    }

    protected static byte[] getWapString(ByteArrayInputStream pduDataStream, int stringType) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
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
            if (!f37assertionsDisabled) {
                if (!(-1 != temp)) {
                    throw new AssertionError();
                }
            }
        }
        if (out.size() > 0) {
            return out.toByteArray();
        }
        return null;
    }

    protected static int extractByteValue(ByteArrayInputStream pduDataStream) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        return temp & 255;
    }

    protected static int parseShortInteger(ByteArrayInputStream pduDataStream) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        return temp & CallFailCause.INTERWORKING_UNSPECIFIED;
    }

    protected static long parseLongInteger(ByteArrayInputStream pduDataStream) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        int count = temp & 255;
        if (count > 8) {
            throw new RuntimeException("Octet count greater than 8 and I can't represent that!");
        }
        long result = 0;
        for (int i = 0; i < count; i++) {
            int temp2 = pduDataStream.read();
            if (!f37assertionsDisabled) {
                if (!(-1 != temp2)) {
                    throw new AssertionError();
                }
            }
            result = (result << 8) + ((long) (temp2 & 255));
        }
        return result;
    }

    protected static long parseIntegerValue(ByteArrayInputStream pduDataStream) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        pduDataStream.reset();
        if (temp > 127) {
            return parseShortInteger(pduDataStream);
        }
        return parseLongInteger(pduDataStream);
    }

    protected static int skipWapValue(ByteArrayInputStream pduDataStream, int length) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        byte[] area = new byte[length];
        int readLen = pduDataStream.read(area, 0, length);
        if (readLen < length) {
            return -1;
        }
        return readLen;
    }

    protected static void parseContentTypeParams(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map, Integer length) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        if (!f37assertionsDisabled) {
            if (!(length.intValue() > 0)) {
                throw new AssertionError();
            }
        }
        int startPos = pduDataStream.available();
        int lastLen = length.intValue();
        while (lastLen > 0) {
            int param = pduDataStream.read();
            if (!f37assertionsDisabled) {
                if (!(-1 != param)) {
                    throw new AssertionError();
                }
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
                            Log.i(LOG_TAG, "Parse CharacterSets: charsetStr");
                            map.put(129, Integer.valueOf(charsetInt));
                        } catch (UnsupportedEncodingException e) {
                            Log.e(LOG_TAG, Arrays.toString(charsetStr), e);
                            map.put(129, 0);
                        }
                    } else {
                        int charset = (int) parseIntegerValue(pduDataStream);
                        if (map != null) {
                            Log.i(LOG_TAG, "Parse Well-known-charset: charset");
                            map.put(129, Integer.valueOf(charset));
                        }
                    }
                    int tempPos = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos);
                    break;
                case 130:
                case 132:
                case 135:
                case 136:
                case 139:
                case 142:
                case 144:
                case 145:
                case 146:
                case 147:
                case 148:
                case 149:
                case 150:
                case 154:
                default:
                    if (-1 == skipWapValue(pduDataStream, lastLen)) {
                        Log.e(LOG_TAG, "Corrupt Content-Type");
                    } else {
                        lastLen = 0;
                    }
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
                case 134:
                case 152:
                    byte[] fileName = parseWapString(pduDataStream, 0);
                    if (fileName != null && map != null) {
                        map.put(152, fileName);
                    }
                    int tempPos4 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos4);
                    break;
                case 138:
                case 153:
                    byte[] start = parseWapString(pduDataStream, 0);
                    if (start != null && map != null) {
                        map.put(153, start);
                    }
                    int tempPos5 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos5);
                    break;
                case 140:
                case 155:
                    byte[] comment = parseWapString(pduDataStream, 0);
                    if (comment != null && map != null) {
                        map.put(155, comment);
                    }
                    int tempPos6 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos6);
                    break;
                case 141:
                case 156:
                    byte[] domain = parseWapString(pduDataStream, 0);
                    if (domain != null && map != null) {
                        map.put(156, domain);
                    }
                    int tempPos7 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos7);
                    break;
                case 143:
                case 157:
                    byte[] path = parseWapString(pduDataStream, 0);
                    if (path != null && map != null) {
                        map.put(157, path);
                    }
                    int tempPos8 = pduDataStream.available();
                    lastLen = length.intValue() - (startPos - tempPos8);
                    break;
            }
        }
        if (lastLen == 0) {
            return;
        }
        Log.e(LOG_TAG, "Corrupt Content-Type");
    }

    protected static byte[] parseContentType(ByteArrayInputStream pduDataStream, HashMap<Integer, Object> map) {
        byte[] contentType;
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        pduDataStream.mark(1);
        int temp = pduDataStream.read();
        if (!f37assertionsDisabled) {
            if (!(-1 != temp)) {
                throw new AssertionError();
            }
        }
        pduDataStream.reset();
        int cur = temp & 255;
        if (cur < 32) {
            int length = parseValueLength(pduDataStream);
            int startPos = pduDataStream.available();
            pduDataStream.mark(1);
            int temp2 = pduDataStream.read();
            if (!f37assertionsDisabled) {
                if (!(-1 != temp2)) {
                    throw new AssertionError();
                }
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
            return contentType;
        }
        if (cur <= 127) {
            byte[] contentType2 = parseWapString(pduDataStream, 0);
            return contentType2;
        }
        byte[] contentType3 = PduContentTypes.contentTypes[parseShortInteger(pduDataStream)].getBytes();
        return contentType3;
    }

    protected boolean parsePartHeaders(ByteArrayInputStream pduDataStream, PduPart part, int length) {
        if (!f37assertionsDisabled) {
            if (!(pduDataStream != null)) {
                throw new AssertionError();
            }
        }
        if (!f37assertionsDisabled) {
            if (!(part != null)) {
                throw new AssertionError();
            }
        }
        if (!f37assertionsDisabled) {
            if (!(length > 0)) {
                throw new AssertionError();
            }
        }
        int startPos = pduDataStream.available();
        int lastLen = length;
        while (lastLen > 0) {
            pduDataStream.mark(1);
            int header = pduDataStream.read();
            if (!f37assertionsDisabled) {
                if (!(-1 != header)) {
                    throw new AssertionError();
                }
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
                    case 146:
                        parseLongInteger(pduDataStream);
                        long date = System.currentTimeMillis() / 1000;
                        part.setDate(date);
                        int tempPos2 = pduDataStream.available();
                        lastLen = length - (startPos - tempPos2);
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
                            int tempPos3 = pduDataStream.available();
                            lastLen = length - (startPos - tempPos3);
                        }
                        break;
                    case 176:
                        byte[] xWapContentUri = parseWapString(pduDataStream, 0);
                        if (xWapContentUri != null) {
                            part.setXWapContentUri(xWapContentUri);
                        }
                        int tempPos4 = pduDataStream.available();
                        lastLen = length - (startPos - tempPos4);
                        break;
                    case 192:
                        byte[] contentId = parseWapString(pduDataStream, 1);
                        if (contentId != null) {
                            part.setContentId(contentId);
                        }
                        int tempPos5 = pduDataStream.available();
                        lastLen = length - (startPos - tempPos5);
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
                pduDataStream.reset();
                byte[] tempHeader = parseWapString(pduDataStream, 0);
                byte[] tempValue = parseWapString(pduDataStream, 0);
                String unAssignedHeader = new String(tempHeader);
                Log.i(LOG_TAG, "Got unknown header field: " + unAssignedHeader);
                if (tempValue != null) {
                    Log.i(LOG_TAG, "Got unknown header tempValue: " + new String(tempValue));
                } else {
                    Log.i(LOG_TAG, "tempValue is null ");
                }
                if (PduPart.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(unAssignedHeader)) {
                    part.setContentTransferEncoding(tempValue);
                }
                if (PduPart.CONTENT_ID.equalsIgnoreCase(unAssignedHeader) && tempValue != null) {
                    part.setContentId(tempValue);
                }
                if (PduPart.CONTENT_LOCATION.equalsIgnoreCase(unAssignedHeader) && tempValue != null) {
                    part.setContentLocation(tempValue);
                }
                if (PduPart.CONTENT_DISPOSITION.equalsIgnoreCase(unAssignedHeader) && tempValue != null) {
                    part.setContentDisposition(tempValue);
                }
                if (PduPart.PARA_NAME.equalsIgnoreCase(unAssignedHeader) && tempValue != null) {
                    part.setName(tempValue);
                }
                int tempPos6 = pduDataStream.available();
                lastLen = length - (startPos - tempPos6);
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
        if (!f37assertionsDisabled) {
            if (!(part != null)) {
                throw new AssertionError();
            }
        }
        if (mTypeParam == null && mStartParam == null) {
            return 1;
        }
        if (mStartParam == null) {
            return (mTypeParam == null || (contentType = part.getContentType()) == null || !Arrays.equals(mTypeParam, contentType)) ? 1 : 0;
        }
        byte[] contentId = part.getContentId();
        return (contentId == null || !Arrays.equals(mStartParam, contentId)) ? 1 : 0;
    }

    protected static boolean checkMandatoryHeader(PduHeaders headers) {
        if (headers == null) {
            return false;
        }
        int messageType = headers.getOctet(140);
        int mmsVersion = headers.getOctet(141);
        if (mmsVersion == 0) {
            Log.i(LOG_TAG, "Parse MandatoryHeader Failed: PDU hasn't mmsVersion");
            return false;
        }
        switch (messageType) {
            case 128:
                byte[] srContentType = headers.getTextString(132);
                if (srContentType == null) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_SEND_REQ PDU hasn't content type field");
                }
                break;
            case 129:
                int scResponseStatus = headers.getOctet(146);
                if (scResponseStatus == 0) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_SEND_CONF PDU hasn't Response-Status field");
                } else {
                    byte[] scTransactionId = headers.getTextString(152);
                    if (scTransactionId == null) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_SEND_CONF PDU hasn't Transaction-Id field");
                    }
                }
                break;
            case 130:
                byte[] niContentLocation = headers.getTextString(131);
                if (niContentLocation == null) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFICATION_IND PDU hasn't Content-Location field");
                } else {
                    long niExpiry = headers.getLongInteger(136);
                    if (-1 == niExpiry) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFICATION_IND PDU hasn't Expiry field");
                    } else {
                        byte[] niMessageClass = headers.getTextString(138);
                        if (niMessageClass == null) {
                            Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFICATION_IND PDU hasn't Message-Class field");
                        } else {
                            long niMessageSize = headers.getLongInteger(142);
                            if (-1 == niMessageSize) {
                                Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFICATION_IND PDU hasn't Message-Size field");
                            } else {
                                byte[] niTransactionId = headers.getTextString(152);
                                if (niTransactionId == null) {
                                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFICATION_IND PDU hasn't Transaction-Id field");
                                }
                            }
                        }
                    }
                }
                break;
            case 131:
                int nriStatus = headers.getOctet(149);
                if (nriStatus == 0) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFYRESP_IND PDU hasn't Status field");
                } else {
                    byte[] nriTransactionId = headers.getTextString(152);
                    if (nriTransactionId == null) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_NOTIFYRESP_IND PDU hasn't Transaction-Id field");
                    }
                }
                break;
            case 132:
                byte[] rcContentType = headers.getTextString(132);
                if (rcContentType == null) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_RETRIEVE_CONF PDU hasn't Content-Type field");
                } else {
                    long rcDate = headers.getLongInteger(133);
                    if (-1 == rcDate) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_RETRIEVE_CONF PDU hasn't Date field");
                    }
                }
                break;
            case 133:
                byte[] aiTransactionId = headers.getTextString(152);
                if (aiTransactionId == null) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_ACKNOWLEDGE_IND PDU hasn't Transaction-Id field");
                }
                break;
            case 134:
                long diDate = headers.getLongInteger(133);
                if (-1 == diDate) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_DELIVERY_IND PDU hasn't Date field");
                } else {
                    byte[] diMessageId = headers.getTextString(139);
                    if (diMessageId == null) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_DELIVERY_IND PDU hasn't Message-Id field");
                    } else {
                        int diStatus = headers.getOctet(149);
                        if (diStatus == 0) {
                            Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_DELIVERY_IND PDU hasn't Status field");
                        } else {
                            EncodedStringValue[] diTo = headers.getEncodedStringValues(151);
                            if (diTo == null) {
                                Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_DELIVERY_IND PDU hasn't To field");
                            }
                        }
                    }
                }
                break;
            case 135:
                EncodedStringValue rrFrom = headers.getEncodedStringValue(137);
                if (rrFrom == null) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_REC_IND PDU hasn't From field");
                } else {
                    byte[] rrMessageId = headers.getTextString(139);
                    if (rrMessageId == null) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_REC_IND PDU hasn't Message-Id field");
                    } else {
                        int rrReadStatus = headers.getOctet(155);
                        if (rrReadStatus == 0) {
                            Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_REC_IND PDU hasn't Read-Status field");
                        } else {
                            EncodedStringValue[] rrTo = headers.getEncodedStringValues(151);
                            if (rrTo == null) {
                                Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_REC_IND PDU hasn't To field");
                            }
                        }
                    }
                }
                break;
            case 136:
                long roDate = headers.getLongInteger(133);
                if (-1 == roDate) {
                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_ORIG_IND PDU hasn't Date field");
                } else {
                    EncodedStringValue roFrom = headers.getEncodedStringValue(137);
                    if (roFrom == null) {
                        Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_ORIG_IND PDU hasn't From field");
                    } else {
                        byte[] roMessageId = headers.getTextString(139);
                        if (roMessageId == null) {
                            Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_ORIG_IND PDU hasn't Message-Id field");
                        } else {
                            int roReadStatus = headers.getOctet(155);
                            if (roReadStatus == 0) {
                                Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_ORIG_IND PDU hasn't Read-Status field");
                            } else {
                                EncodedStringValue[] roTo = headers.getEncodedStringValues(151);
                                if (roTo == null) {
                                    Log.i(LOG_TAG, "Parse MandatoryHeader Failed: MESSAGE_TYPE_READ_ORIG_IND PDU hasn't To field");
                                }
                            }
                        }
                    }
                }
                break;
            default:
                Log.i(LOG_TAG, "Parse MandatoryHeader Failed: Parser doesn't support this message type in this version");
                break;
        }
        return false;
    }

    public GenericPdu parse(boolean forRestore) {
        this.mForRestore = forRestore;
        return parse();
    }
}
