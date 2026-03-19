package com.google.android.mms.pdu;

import android.util.Log;
import com.google.android.mms.ContentType;
import com.google.android.mms.InvalidHeaderValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class PduParser {
    static final boolean $assertionsDisabled = false;
    private static final boolean DEBUG = false;
    protected static final int END_STRING_FLAG = 0;
    private static final int LENGTH_QUOTE = 31;
    protected static final boolean LOCAL_LOGV = false;
    private static final String LOG_TAG = "PduParser";
    private static final int LONG_INTEGER_LENGTH_MAX = 8;
    private static final int QUOTE = 127;
    private static final int QUOTED_STRING_FLAG = 34;
    private static final int SHORT_INTEGER_MAX = 127;
    private static final int SHORT_LENGTH_MAX = 30;
    protected static final int TEXT_MAX = 127;
    protected static final int TEXT_MIN = 32;
    private static final int THE_FIRST_PART = 0;
    private static final int THE_LAST_PART = 1;
    protected static final int TYPE_QUOTED_STRING = 1;
    protected static final int TYPE_TEXT_STRING = 0;
    private static final int TYPE_TOKEN_STRING = 2;
    protected final boolean mParseContentDisposition;
    protected ByteArrayInputStream mPduDataStream;
    protected static byte[] mTypeParam = null;
    protected static byte[] mStartParam = null;
    protected PduHeaders mHeaders = null;
    protected PduBody mBody = null;

    public PduParser(byte[] bArr, boolean z) {
        this.mPduDataStream = null;
        this.mPduDataStream = new ByteArrayInputStream(bArr);
        this.mParseContentDisposition = z;
    }

    public GenericPdu parse() {
        if (this.mPduDataStream == null) {
            return null;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            return null;
        }
        int octet = this.mHeaders.getOctet(140);
        if (!checkMandatoryHeader(this.mHeaders)) {
            log("check mandatory headers failed!");
            return null;
        }
        if (128 == octet || 132 == octet) {
            this.mBody = parseParts(this.mPduDataStream);
            if (this.mBody == null) {
                return null;
            }
        }
        switch (octet) {
            case 128:
                break;
            case 129:
                break;
            case 130:
                break;
            case 131:
                break;
            case 132:
                RetrieveConf retrieveConf = new RetrieveConf(this.mHeaders, this.mBody);
                byte[] contentType = retrieveConf.getContentType();
                if (contentType != null) {
                    String str = new String(contentType);
                    if (!str.equals(ContentType.MULTIPART_MIXED) && !str.equals(ContentType.MULTIPART_RELATED) && !str.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                        if (str.equals(ContentType.MULTIPART_ALTERNATIVE)) {
                            PduPart part = this.mBody.getPart(0);
                            this.mBody.removeAll();
                            this.mBody.addPart(0, part);
                        }
                        break;
                    }
                }
                break;
            case 133:
                break;
            case 134:
                break;
            case 135:
                break;
            case 136:
                break;
            default:
                log("Parser doesn't support this message type in this version!");
                break;
        }
        return null;
    }

    protected PduHeaders parseHeaders(ByteArrayInputStream byteArrayInputStream) {
        EncodedStringValue encodedStringValue;
        byte[] textString;
        if (byteArrayInputStream == null) {
            return null;
        }
        PduHeaders pduHeaders = new PduHeaders();
        boolean z = true;
        while (z && byteArrayInputStream.available() > 0) {
            byteArrayInputStream.mark(1);
            int iExtractByteValue = extractByteValue(byteArrayInputStream);
            if (iExtractByteValue >= 32 && iExtractByteValue <= 127) {
                byteArrayInputStream.reset();
                parseWapString(byteArrayInputStream, 0);
            } else {
                switch (iExtractByteValue) {
                    case 129:
                    case 130:
                    case 151:
                        EncodedStringValue encodedStringValue2 = parseEncodedStringValue(byteArrayInputStream);
                        if (encodedStringValue2 == null) {
                            continue;
                        } else {
                            byte[] textString2 = encodedStringValue2.getTextString();
                            if (textString2 != null) {
                                String str = new String(textString2);
                                int iIndexOf = str.indexOf("/");
                                if (iIndexOf > 0) {
                                    str = str.substring(0, iIndexOf);
                                }
                                try {
                                    encodedStringValue2.setTextString(str.getBytes());
                                } catch (NullPointerException e) {
                                    log("null pointer error!");
                                    return null;
                                }
                            }
                            try {
                                pduHeaders.appendEncodedStringValue(encodedStringValue2, iExtractByteValue);
                            } catch (NullPointerException e2) {
                                log("null pointer error!");
                            } catch (RuntimeException e3) {
                                log(iExtractByteValue + "is not Encoded-String-Value header field!");
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
                        byte[] wapString = parseWapString(byteArrayInputStream, 0);
                        if (wapString != null) {
                            try {
                                pduHeaders.setTextString(wapString, iExtractByteValue);
                            } catch (NullPointerException e4) {
                                log("null pointer error!");
                            } catch (RuntimeException e5) {
                                log(iExtractByteValue + "is not Text-String header field!");
                                return null;
                            }
                        }
                        break;
                    case 132:
                        HashMap<Integer, Object> map = new HashMap<>();
                        byte[] contentType = parseContentType(byteArrayInputStream, map);
                        if (contentType != null) {
                            try {
                                pduHeaders.setTextString(contentType, 132);
                            } catch (NullPointerException e6) {
                                log("null pointer error!");
                            } catch (RuntimeException e7) {
                                log(iExtractByteValue + "is not Text-String header field!");
                                return null;
                            }
                        }
                        mStartParam = (byte[]) map.get(153);
                        mTypeParam = (byte[]) map.get(131);
                        z = false;
                        break;
                    case 133:
                    case 142:
                    case PduHeaders.REPLY_CHARGING_SIZE:
                        try {
                            pduHeaders.setLongInteger(parseLongInteger(byteArrayInputStream), iExtractByteValue);
                        } catch (RuntimeException e8) {
                            log(iExtractByteValue + "is not Long-Integer header field!");
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
                    case PduHeaders.STORED:
                    case PduHeaders.TOTALS:
                    case PduHeaders.QUOTAS:
                    case PduHeaders.DISTRIBUTION_INDICATOR:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE:
                    case PduHeaders.CONTENT_CLASS:
                    case PduHeaders.DRM_CONTENT:
                    case PduHeaders.ADAPTATION_ALLOWED:
                    case PduHeaders.CANCEL_STATUS:
                        int iExtractByteValue2 = extractByteValue(byteArrayInputStream);
                        try {
                            pduHeaders.setOctet(iExtractByteValue2, iExtractByteValue);
                        } catch (InvalidHeaderValueException e9) {
                            log("Set invalid Octet value: " + iExtractByteValue2 + " into the header filed: " + iExtractByteValue);
                            return null;
                        } catch (RuntimeException e10) {
                            log(iExtractByteValue + "is not Octet header field!");
                            return null;
                        }
                        break;
                    case 135:
                    case 136:
                    case 157:
                        parseValueLength(byteArrayInputStream);
                        int iExtractByteValue3 = extractByteValue(byteArrayInputStream);
                        try {
                            long longInteger = parseLongInteger(byteArrayInputStream);
                            if (129 == iExtractByteValue3) {
                                longInteger += System.currentTimeMillis() / 1000;
                            }
                            try {
                                pduHeaders.setLongInteger(longInteger, iExtractByteValue);
                            } catch (RuntimeException e11) {
                                log(iExtractByteValue + "is not Long-Integer header field!");
                                return null;
                            }
                        } catch (RuntimeException e12) {
                            log(iExtractByteValue + "is not Long-Integer header field!");
                            return null;
                        }
                        break;
                    case 137:
                        parseValueLength(byteArrayInputStream);
                        if (128 == extractByteValue(byteArrayInputStream)) {
                            encodedStringValue = parseEncodedStringValue(byteArrayInputStream);
                            if (encodedStringValue != null && (textString = encodedStringValue.getTextString()) != null) {
                                String str2 = new String(textString);
                                int iIndexOf2 = str2.indexOf("/");
                                if (iIndexOf2 > 0) {
                                    str2 = str2.substring(0, iIndexOf2);
                                }
                                try {
                                    encodedStringValue.setTextString(str2.getBytes());
                                } catch (NullPointerException e13) {
                                    log("null pointer error!");
                                    return null;
                                }
                            }
                        } else {
                            try {
                                encodedStringValue = new EncodedStringValue(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR.getBytes());
                            } catch (NullPointerException e14) {
                                log(iExtractByteValue + "is not Encoded-String-Value header field!");
                                return null;
                            }
                        }
                        try {
                            pduHeaders.setEncodedStringValue(encodedStringValue, 137);
                        } catch (NullPointerException e15) {
                            log("null pointer error!");
                        } catch (RuntimeException e16) {
                            log(iExtractByteValue + "is not Encoded-String-Value header field!");
                            return null;
                        }
                        break;
                    case 138:
                        byteArrayInputStream.mark(1);
                        int iExtractByteValue4 = extractByteValue(byteArrayInputStream);
                        if (iExtractByteValue4 >= 128) {
                            if (128 == iExtractByteValue4) {
                                try {
                                    pduHeaders.setTextString(PduHeaders.MESSAGE_CLASS_PERSONAL_STR.getBytes(), 138);
                                } catch (NullPointerException e17) {
                                    log("null pointer error!");
                                } catch (RuntimeException e18) {
                                    log(iExtractByteValue + "is not Text-String header field!");
                                    return null;
                                }
                            } else if (129 == iExtractByteValue4) {
                                pduHeaders.setTextString(PduHeaders.MESSAGE_CLASS_ADVERTISEMENT_STR.getBytes(), 138);
                            } else if (130 == iExtractByteValue4) {
                                pduHeaders.setTextString(PduHeaders.MESSAGE_CLASS_INFORMATIONAL_STR.getBytes(), 138);
                            } else if (131 == iExtractByteValue4) {
                                pduHeaders.setTextString(PduHeaders.MESSAGE_CLASS_AUTO_STR.getBytes(), 138);
                            }
                        } else {
                            byteArrayInputStream.reset();
                            byte[] wapString2 = parseWapString(byteArrayInputStream, 0);
                            if (wapString2 != null) {
                                try {
                                    pduHeaders.setTextString(wapString2, 138);
                                } catch (NullPointerException e19) {
                                    log("null pointer error!");
                                } catch (RuntimeException e20) {
                                    log(iExtractByteValue + "is not Text-String header field!");
                                    return null;
                                }
                            }
                        }
                        break;
                    case 140:
                        int iExtractByteValue5 = extractByteValue(byteArrayInputStream);
                        switch (iExtractByteValue5) {
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
                                    pduHeaders.setOctet(iExtractByteValue5, iExtractByteValue);
                                } catch (InvalidHeaderValueException e21) {
                                    log("Set invalid Octet value: " + iExtractByteValue5 + " into the header filed: " + iExtractByteValue);
                                    return null;
                                } catch (RuntimeException e22) {
                                    log(iExtractByteValue + "is not Octet header field!");
                                    return null;
                                }
                                break;
                        }
                        break;
                    case 141:
                        int shortInteger = parseShortInteger(byteArrayInputStream);
                        try {
                            pduHeaders.setOctet(shortInteger, 141);
                        } catch (InvalidHeaderValueException e23) {
                            log("Set invalid Octet value: " + shortInteger + " into the header filed: " + iExtractByteValue);
                            return null;
                        } catch (RuntimeException e24) {
                            log(iExtractByteValue + "is not Octet header field!");
                            return null;
                        }
                        break;
                    case 147:
                    case 150:
                    case 154:
                    case PduHeaders.STORE_STATUS_TEXT:
                    case PduHeaders.RECOMMENDED_RETRIEVAL_MODE_TEXT:
                    case PduHeaders.STATUS_TEXT:
                        EncodedStringValue encodedStringValue3 = parseEncodedStringValue(byteArrayInputStream);
                        if (encodedStringValue3 != null) {
                            try {
                                pduHeaders.setEncodedStringValue(encodedStringValue3, iExtractByteValue);
                            } catch (NullPointerException e25) {
                                log("null pointer error!");
                            } catch (RuntimeException e26) {
                                log(iExtractByteValue + "is not Encoded-String-Value header field!");
                                return null;
                            }
                        }
                        break;
                    case 160:
                        parseValueLength(byteArrayInputStream);
                        try {
                            parseIntegerValue(byteArrayInputStream);
                            EncodedStringValue encodedStringValue4 = parseEncodedStringValue(byteArrayInputStream);
                            if (encodedStringValue4 != null) {
                                try {
                                    pduHeaders.setEncodedStringValue(encodedStringValue4, 160);
                                } catch (NullPointerException e27) {
                                    log("null pointer error!");
                                } catch (RuntimeException e28) {
                                    log(iExtractByteValue + "is not Encoded-String-Value header field!");
                                    return null;
                                }
                            }
                        } catch (RuntimeException e29) {
                            log(iExtractByteValue + " is not Integer-Value");
                            return null;
                        }
                        break;
                    case PduHeaders.PREVIOUSLY_SENT_DATE:
                        parseValueLength(byteArrayInputStream);
                        try {
                            parseIntegerValue(byteArrayInputStream);
                            try {
                                pduHeaders.setLongInteger(parseLongInteger(byteArrayInputStream), PduHeaders.PREVIOUSLY_SENT_DATE);
                            } catch (RuntimeException e30) {
                                log(iExtractByteValue + "is not Long-Integer header field!");
                                return null;
                            }
                        } catch (RuntimeException e31) {
                            log(iExtractByteValue + " is not Integer-Value");
                            return null;
                        }
                        break;
                    case PduHeaders.MM_FLAGS:
                        parseValueLength(byteArrayInputStream);
                        extractByteValue(byteArrayInputStream);
                        parseEncodedStringValue(byteArrayInputStream);
                        break;
                    case PduHeaders.ATTRIBUTES:
                    case 174:
                    case PduHeaders.ADDITIONAL_HEADERS:
                    default:
                        log("Unknown header");
                        break;
                    case PduHeaders.MBOX_TOTALS:
                    case PduHeaders.MBOX_QUOTAS:
                        parseValueLength(byteArrayInputStream);
                        extractByteValue(byteArrayInputStream);
                        try {
                            parseIntegerValue(byteArrayInputStream);
                        } catch (RuntimeException e32) {
                            log(iExtractByteValue + " is not Integer-Value");
                            return null;
                        }
                        break;
                    case PduHeaders.MESSAGE_COUNT:
                    case PduHeaders.START:
                    case PduHeaders.LIMIT:
                        try {
                            pduHeaders.setLongInteger(parseIntegerValue(byteArrayInputStream), iExtractByteValue);
                        } catch (RuntimeException e33) {
                            log(iExtractByteValue + "is not Long-Integer header field!");
                            return null;
                        }
                        break;
                    case PduHeaders.ELEMENT_DESCRIPTOR:
                        parseContentType(byteArrayInputStream, null);
                        break;
                }
            }
        }
        return pduHeaders;
    }

    protected PduBody parseParts(ByteArrayInputStream byteArrayInputStream) {
        if (byteArrayInputStream == null) {
            return null;
        }
        int unsignedInt = parseUnsignedInt(byteArrayInputStream);
        PduBody pduBody = new PduBody();
        for (int i = 0; i < unsignedInt; i++) {
            int unsignedInt2 = parseUnsignedInt(byteArrayInputStream);
            int unsignedInt3 = parseUnsignedInt(byteArrayInputStream);
            PduPart pduPart = new PduPart();
            int iAvailable = byteArrayInputStream.available();
            if (iAvailable <= 0) {
                return null;
            }
            HashMap<Integer, Object> map = new HashMap<>();
            byte[] contentType = parseContentType(byteArrayInputStream, map);
            if (contentType == null) {
                pduPart.setContentType(PduContentTypes.contentTypes[0].getBytes());
            } else {
                pduPart.setContentType(contentType);
            }
            byte[] bArr = (byte[]) map.get(151);
            if (bArr != null) {
                pduPart.setName(bArr);
            }
            Integer num = (Integer) map.get(129);
            if (num != null) {
                pduPart.setCharset(num.intValue());
            }
            int iAvailable2 = unsignedInt2 - (iAvailable - byteArrayInputStream.available());
            if (iAvailable2 > 0) {
                if (!parsePartHeaders(byteArrayInputStream, pduPart, iAvailable2)) {
                    return null;
                }
            } else if (iAvailable2 < 0) {
                return null;
            }
            if (pduPart.getContentLocation() == null && pduPart.getName() == null && pduPart.getFilename() == null && pduPart.getContentId() == null) {
                pduPart.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
            }
            if (unsignedInt3 > 0) {
                byte[] bArrDecodeQuotedPrintable = new byte[unsignedInt3];
                String str = new String(pduPart.getContentType());
                byteArrayInputStream.read(bArrDecodeQuotedPrintable, 0, unsignedInt3);
                if (str.equalsIgnoreCase(ContentType.MULTIPART_ALTERNATIVE)) {
                    pduPart = parseParts(new ByteArrayInputStream(bArrDecodeQuotedPrintable)).getPart(0);
                } else {
                    byte[] contentTransferEncoding = pduPart.getContentTransferEncoding();
                    if (contentTransferEncoding != null) {
                        String str2 = new String(contentTransferEncoding);
                        if (str2.equalsIgnoreCase(PduPart.P_BASE64)) {
                            bArrDecodeQuotedPrintable = Base64.decodeBase64(bArrDecodeQuotedPrintable);
                        } else if (str2.equalsIgnoreCase(PduPart.P_QUOTED_PRINTABLE)) {
                            bArrDecodeQuotedPrintable = QuotedPrintable.decodeQuotedPrintable(bArrDecodeQuotedPrintable);
                        }
                    }
                    if (bArrDecodeQuotedPrintable == null) {
                        log("Decode part data error!");
                        return null;
                    }
                    pduPart.setData(bArrDecodeQuotedPrintable);
                }
            }
            if (checkPartPosition(pduPart) == 0) {
                pduBody.addPart(0, pduPart);
            } else {
                pduBody.addPart(pduPart);
            }
        }
        return pduBody;
    }

    protected static void log(String str) {
    }

    protected static int parseUnsignedInt(ByteArrayInputStream byteArrayInputStream) {
        int i = 0;
        int i2 = byteArrayInputStream.read();
        if (i2 == -1) {
            return i2;
        }
        while ((i2 & 128) != 0) {
            i = (i << 7) | (i2 & 127);
            i2 = byteArrayInputStream.read();
            if (i2 == -1) {
                return i2;
            }
        }
        return (i << 7) | (i2 & 127);
    }

    protected static int parseValueLength(ByteArrayInputStream byteArrayInputStream) {
        int i = byteArrayInputStream.read() & 255;
        if (i <= 30) {
            return i;
        }
        if (i == 31) {
            return parseUnsignedInt(byteArrayInputStream);
        }
        throw new RuntimeException("Value length > LENGTH_QUOTE!");
    }

    protected static EncodedStringValue parseEncodedStringValue(ByteArrayInputStream byteArrayInputStream) {
        int shortInteger;
        EncodedStringValue encodedStringValue;
        byteArrayInputStream.mark(1);
        int i = byteArrayInputStream.read() & 255;
        if (i == 0) {
            return new EncodedStringValue("");
        }
        byteArrayInputStream.reset();
        if (i < 32) {
            parseValueLength(byteArrayInputStream);
            shortInteger = parseShortInteger(byteArrayInputStream);
        } else {
            shortInteger = 0;
        }
        byte[] wapString = parseWapString(byteArrayInputStream, 0);
        try {
            if (shortInteger != 0) {
                encodedStringValue = new EncodedStringValue(shortInteger, wapString);
            } else {
                encodedStringValue = new EncodedStringValue(wapString);
            }
            return encodedStringValue;
        } catch (Exception e) {
            return null;
        }
    }

    protected static byte[] parseWapString(ByteArrayInputStream byteArrayInputStream, int i) {
        byteArrayInputStream.mark(1);
        int i2 = byteArrayInputStream.read();
        if (1 == i && 34 == i2) {
            byteArrayInputStream.mark(1);
        } else if (i == 0 && 127 == i2) {
            byteArrayInputStream.mark(1);
        } else {
            byteArrayInputStream.reset();
        }
        return getWapString(byteArrayInputStream, i);
    }

    protected static boolean isTokenCharacter(int r2) {
        if (r2 >= 33 && r2 <= 126 && r2 != 34 && r2 != 44 && r2 != 47 && r2 != 123 && r2 != 125) {
            switch (r2) {
                default:
                    switch (r2) {
                        default:
                            switch (r2) {
                            }
                        case 58:
                        case 59:
                        case 60:
                        case 61:
                        case 62:
                        case 63:
                        case 64:
                            return false;
                    }
                case 40:
                case 41:
            }
        }
        return false;
    }

    protected static boolean isText(int i) {
        if ((i < 32 || i > 126) && ((i < 128 || i > 255) && i != 13)) {
            switch (i) {
            }
            return true;
        }
        return true;
    }

    protected static byte[] getWapString(ByteArrayInputStream byteArrayInputStream, int i) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i2 = byteArrayInputStream.read();
        while (-1 != i2 && i2 != 0) {
            if (i == 2) {
                if (isTokenCharacter(i2)) {
                    byteArrayOutputStream.write(i2);
                }
            } else if (isText(i2)) {
                byteArrayOutputStream.write(i2);
            }
            i2 = byteArrayInputStream.read();
        }
        if (byteArrayOutputStream.size() > 0) {
            return byteArrayOutputStream.toByteArray();
        }
        return null;
    }

    protected static int extractByteValue(ByteArrayInputStream byteArrayInputStream) {
        return byteArrayInputStream.read() & 255;
    }

    protected static int parseShortInteger(ByteArrayInputStream byteArrayInputStream) {
        return byteArrayInputStream.read() & 127;
    }

    protected static long parseLongInteger(ByteArrayInputStream byteArrayInputStream) {
        int i = byteArrayInputStream.read() & 255;
        if (i > 8) {
            throw new RuntimeException("Octet count greater than 8 and I can't represent that!");
        }
        long j = 0;
        for (int i2 = 0; i2 < i; i2++) {
            j = (j << 8) + ((long) (byteArrayInputStream.read() & 255));
        }
        return j;
    }

    protected static long parseIntegerValue(ByteArrayInputStream byteArrayInputStream) {
        byteArrayInputStream.mark(1);
        int i = byteArrayInputStream.read();
        byteArrayInputStream.reset();
        if (i > 127) {
            return parseShortInteger(byteArrayInputStream);
        }
        return parseLongInteger(byteArrayInputStream);
    }

    protected static int skipWapValue(ByteArrayInputStream byteArrayInputStream, int i) {
        int i2 = byteArrayInputStream.read(new byte[i], 0, i);
        if (i2 < i) {
            return -1;
        }
        return i2;
    }

    protected void parseContentTypeParams(ByteArrayInputStream byteArrayInputStream, HashMap<Integer, Object> map, Integer num) {
        int iAvailable;
        int iIntValue;
        int iAvailable2 = byteArrayInputStream.available();
        int iIntValue2 = num.intValue();
        while (iIntValue2 > 0) {
            int i = byteArrayInputStream.read();
            iIntValue2--;
            if (i != 129) {
                if (i != 131) {
                    if (i != 133 && i != 151) {
                        if (i != 153) {
                            switch (i) {
                                case 137:
                                    break;
                                case 138:
                                    break;
                                default:
                                    if (-1 == skipWapValue(byteArrayInputStream, iIntValue2)) {
                                        Log.e(LOG_TAG, "Corrupt Content-Type");
                                    } else {
                                        iIntValue2 = 0;
                                    }
                                    break;
                            }
                        }
                        byte[] wapString = parseWapString(byteArrayInputStream, 0);
                        if (wapString != null && map != null) {
                            map.put(153, wapString);
                        }
                        iAvailable = byteArrayInputStream.available();
                        iIntValue = num.intValue();
                    } else {
                        byte[] wapString2 = parseWapString(byteArrayInputStream, 0);
                        if (wapString2 != null && map != null) {
                            map.put(151, wapString2);
                        }
                        iAvailable = byteArrayInputStream.available();
                        iIntValue = num.intValue();
                    }
                }
                byteArrayInputStream.mark(1);
                int iExtractByteValue = extractByteValue(byteArrayInputStream);
                byteArrayInputStream.reset();
                if (iExtractByteValue > 127) {
                    int shortInteger = parseShortInteger(byteArrayInputStream);
                    if (shortInteger < PduContentTypes.contentTypes.length) {
                        map.put(131, PduContentTypes.contentTypes[shortInteger].getBytes());
                    }
                } else {
                    byte[] wapString3 = parseWapString(byteArrayInputStream, 0);
                    if (wapString3 != null && map != null) {
                        map.put(131, wapString3);
                    }
                }
                iAvailable = byteArrayInputStream.available();
                iIntValue = num.intValue();
            } else {
                byteArrayInputStream.mark(1);
                int iExtractByteValue2 = extractByteValue(byteArrayInputStream);
                byteArrayInputStream.reset();
                if ((iExtractByteValue2 > 32 && iExtractByteValue2 < 127) || iExtractByteValue2 == 0) {
                    byte[] wapString4 = parseWapString(byteArrayInputStream, 0);
                    try {
                        map.put(129, Integer.valueOf(CharacterSets.getMibEnumValue(new String(wapString4))));
                    } catch (UnsupportedEncodingException e) {
                        Log.e(LOG_TAG, Arrays.toString(wapString4), e);
                        map.put(129, 0);
                    }
                } else {
                    int integerValue = (int) parseIntegerValue(byteArrayInputStream);
                    if (map != null) {
                        map.put(129, Integer.valueOf(integerValue));
                    }
                }
                iAvailable = byteArrayInputStream.available();
                iIntValue = num.intValue();
            }
            iIntValue2 = iIntValue - (iAvailable2 - iAvailable);
        }
        if (iIntValue2 != 0) {
            Log.e(LOG_TAG, "Corrupt Content-Type");
        }
    }

    protected byte[] parseContentType(ByteArrayInputStream byteArrayInputStream, HashMap<Integer, Object> map) {
        byte[] wapString;
        byteArrayInputStream.mark(1);
        int i = byteArrayInputStream.read();
        byteArrayInputStream.reset();
        int i2 = i & 255;
        if (i2 < 32) {
            int valueLength = parseValueLength(byteArrayInputStream);
            int iAvailable = byteArrayInputStream.available();
            byteArrayInputStream.mark(1);
            int i3 = byteArrayInputStream.read();
            byteArrayInputStream.reset();
            int i4 = i3 & 255;
            if (i4 >= 32 && i4 <= 127) {
                wapString = parseWapString(byteArrayInputStream, 0);
            } else if (i4 > 127) {
                int shortInteger = parseShortInteger(byteArrayInputStream);
                if (shortInteger < PduContentTypes.contentTypes.length) {
                    wapString = PduContentTypes.contentTypes[shortInteger].getBytes();
                } else {
                    byteArrayInputStream.reset();
                    wapString = parseWapString(byteArrayInputStream, 0);
                }
            } else {
                Log.e(LOG_TAG, "Corrupt content-type");
                return PduContentTypes.contentTypes[0].getBytes();
            }
            int iAvailable2 = valueLength - (iAvailable - byteArrayInputStream.available());
            if (iAvailable2 > 0) {
                parseContentTypeParams(byteArrayInputStream, map, Integer.valueOf(iAvailable2));
            }
            if (iAvailable2 < 0) {
                Log.e(LOG_TAG, "Corrupt MMS message");
                return PduContentTypes.contentTypes[0].getBytes();
            }
            return wapString;
        }
        if (i2 <= 127) {
            return parseWapString(byteArrayInputStream, 0);
        }
        return PduContentTypes.contentTypes[parseShortInteger(byteArrayInputStream)].getBytes();
    }

    protected boolean parsePartHeaders(ByteArrayInputStream byteArrayInputStream, PduPart pduPart, int i) {
        int iAvailable = byteArrayInputStream.available();
        int iAvailable2 = i;
        while (iAvailable2 > 0) {
            int i2 = byteArrayInputStream.read();
            iAvailable2--;
            if (i2 > 127) {
                if (i2 == 142) {
                    byte[] wapString = parseWapString(byteArrayInputStream, 0);
                    if (wapString != null) {
                        pduPart.setContentLocation(wapString);
                    }
                    iAvailable2 = i - (iAvailable - byteArrayInputStream.available());
                } else {
                    if (i2 != 174) {
                        if (i2 == 192) {
                            byte[] wapString2 = parseWapString(byteArrayInputStream, 1);
                            if (wapString2 != null) {
                                pduPart.setContentId(wapString2);
                            }
                            iAvailable2 = i - (iAvailable - byteArrayInputStream.available());
                        } else if (i2 != 197) {
                            if (-1 == skipWapValue(byteArrayInputStream, iAvailable2)) {
                                Log.e(LOG_TAG, "Corrupt Part headers");
                                return false;
                            }
                            iAvailable2 = 0;
                        }
                    }
                    if (this.mParseContentDisposition) {
                        int valueLength = parseValueLength(byteArrayInputStream);
                        byteArrayInputStream.mark(1);
                        int iAvailable3 = byteArrayInputStream.available();
                        int i3 = byteArrayInputStream.read();
                        if (i3 == 128) {
                            pduPart.setContentDisposition(PduPart.DISPOSITION_FROM_DATA);
                        } else if (i3 == 129) {
                            pduPart.setContentDisposition(PduPart.DISPOSITION_ATTACHMENT);
                        } else if (i3 == 130) {
                            pduPart.setContentDisposition(PduPart.DISPOSITION_INLINE);
                        } else {
                            byteArrayInputStream.reset();
                            pduPart.setContentDisposition(parseWapString(byteArrayInputStream, 0));
                        }
                        if (iAvailable3 - byteArrayInputStream.available() < valueLength) {
                            if (byteArrayInputStream.read() == 152) {
                                pduPart.setFilename(parseWapString(byteArrayInputStream, 0));
                            }
                            int iAvailable4 = iAvailable3 - byteArrayInputStream.available();
                            if (iAvailable4 < valueLength) {
                                int i4 = valueLength - iAvailable4;
                                byteArrayInputStream.read(new byte[i4], 0, i4);
                            }
                        }
                        iAvailable2 = i - (iAvailable - byteArrayInputStream.available());
                    }
                }
            } else if (i2 >= 32 && i2 <= 127) {
                byte[] wapString3 = parseWapString(byteArrayInputStream, 0);
                byte[] wapString4 = parseWapString(byteArrayInputStream, 0);
                if (true == PduPart.CONTENT_TRANSFER_ENCODING.equalsIgnoreCase(new String(wapString3))) {
                    pduPart.setContentTransferEncoding(wapString4);
                }
                iAvailable2 = i - (iAvailable - byteArrayInputStream.available());
            } else {
                if (-1 == skipWapValue(byteArrayInputStream, iAvailable2)) {
                    Log.e(LOG_TAG, "Corrupt Part headers");
                    return false;
                }
                iAvailable2 = 0;
            }
        }
        if (iAvailable2 == 0) {
            return true;
        }
        Log.e(LOG_TAG, "Corrupt Part headers");
        return false;
    }

    private static int checkPartPosition(PduPart pduPart) {
        byte[] contentType;
        if (mTypeParam == null && mStartParam == null) {
            return 1;
        }
        if (mStartParam == null) {
            return (mTypeParam == null || (contentType = pduPart.getContentType()) == null || true != Arrays.equals(mTypeParam, contentType)) ? 1 : 0;
        }
        byte[] contentId = pduPart.getContentId();
        return (contentId == null || true != Arrays.equals(mStartParam, contentId)) ? 1 : 0;
    }

    protected static boolean checkMandatoryHeader(PduHeaders pduHeaders) {
        if (pduHeaders == null) {
            return false;
        }
        int octet = pduHeaders.getOctet(140);
        if (pduHeaders.getOctet(141) == 0) {
            return false;
        }
        switch (octet) {
            case 128:
                if (pduHeaders.getTextString(132) != null && pduHeaders.getEncodedStringValue(137) != null && pduHeaders.getTextString(152) != null) {
                }
                break;
            case 129:
                if (pduHeaders.getOctet(146) != 0 && pduHeaders.getTextString(152) != null) {
                }
                break;
            case 130:
                if (pduHeaders.getTextString(131) != null && -1 != pduHeaders.getLongInteger(136) && pduHeaders.getTextString(138) != null && -1 != pduHeaders.getLongInteger(142) && pduHeaders.getTextString(152) != null) {
                }
                break;
            case 131:
                if (pduHeaders.getOctet(149) != 0 && pduHeaders.getTextString(152) != null) {
                }
                break;
            case 132:
                if (pduHeaders.getTextString(132) != null && -1 != pduHeaders.getLongInteger(133)) {
                }
                break;
            case 133:
                if (pduHeaders.getTextString(152) == null) {
                }
                break;
            case 134:
                if (-1 != pduHeaders.getLongInteger(133) && pduHeaders.getTextString(139) != null && pduHeaders.getOctet(149) != 0 && pduHeaders.getEncodedStringValues(151) != null) {
                }
                break;
            case 135:
                if (pduHeaders.getEncodedStringValue(137) != null && pduHeaders.getTextString(139) != null && pduHeaders.getOctet(155) != 0 && pduHeaders.getEncodedStringValues(151) != null) {
                }
                break;
            case 136:
                if (-1 != pduHeaders.getLongInteger(133) && pduHeaders.getEncodedStringValue(137) != null && pduHeaders.getTextString(139) != null && pduHeaders.getOctet(155) != 0 && pduHeaders.getEncodedStringValues(151) != null) {
                }
                break;
        }
        return false;
    }
}
