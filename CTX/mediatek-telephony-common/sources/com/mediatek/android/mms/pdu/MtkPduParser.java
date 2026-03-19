package com.mediatek.android.mms.pdu;

import android.hardware.radio.V1_0.RadioCdmaSmsConst;
import android.util.Log;
import com.google.android.mms.InvalidHeaderValueException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduContentTypes;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.RetrieveConf;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class MtkPduParser extends PduParser {
    static final boolean $assertionsDisabled = false;
    private static final String LOG_TAG = "MtkPduParser";
    protected static final int UNSIGNED_INT_LIMIT = 2;
    private boolean mForRestore;

    public MtkPduParser(byte[] bArr, boolean z) {
        super(bArr, z);
        this.mForRestore = false;
    }

    public GenericPdu parse() {
        RetrieveConf retrieveConf;
        if (this.mPduDataStream == null) {
            Log.i(LOG_TAG, "Input parse stream is null");
            return null;
        }
        this.mHeaders = parseHeaders(this.mPduDataStream);
        if (this.mHeaders == null) {
            Log.i(LOG_TAG, "Parse PduHeader Failed");
            return null;
        }
        int octet = this.mHeaders.getOctet(140);
        if (!checkMandatoryHeader(this.mHeaders)) {
            Log.d(LOG_TAG, "check mandatory headers failed!");
            return null;
        }
        this.mPduDataStream.mark(1);
        int unsignedInt = parseUnsignedInt(this.mPduDataStream);
        this.mPduDataStream.reset();
        if (132 == octet && unsignedInt >= 2) {
            byte[] textString = this.mHeaders.getTextString(132);
            if (textString == null) {
                Log.i(LOG_TAG, "Parse MESSAGE_TYPE_RETRIEVE_CONF Failed: content Type is null _0");
                return null;
            }
            String lowerCase = new String(textString).toLowerCase();
            if (!lowerCase.equals("application/vnd.wap.multipart.mixed") && !lowerCase.equals("application/vnd.wap.multipart.related") && !lowerCase.equals("application/vnd.wap.multipart.alternative") && lowerCase.equals("text/plain")) {
                Log.i(LOG_TAG, "Content Type is text/plain");
                PduPart pduPart = new PduPart();
                pduPart.setContentType(textString);
                pduPart.setContentLocation(Long.toOctalString(System.currentTimeMillis()).getBytes());
                pduPart.setContentId("<part1>".getBytes());
                this.mPduDataStream.mark(1);
                int i = 0;
                while (this.mPduDataStream.read() != -1) {
                    i++;
                }
                byte[] bArr = new byte[i];
                Log.i(LOG_TAG, "got part length: " + i);
                this.mPduDataStream.reset();
                this.mPduDataStream.read(bArr, 0, i);
                Log.i(LOG_TAG, "show data: " + new String(bArr));
                pduPart.setData(bArr);
                Log.i(LOG_TAG, "setData finish");
                PduBody pduBody = new PduBody();
                pduBody.addPart(pduPart);
                try {
                    retrieveConf = new RetrieveConf(this.mHeaders, pduBody);
                } catch (Exception e) {
                    Log.i(LOG_TAG, "new RetrieveConf has exception");
                    retrieveConf = null;
                }
                if (retrieveConf == null) {
                    Log.i(LOG_TAG, "retrieveConf is null");
                }
                return retrieveConf;
            }
        }
        if (128 == octet || 132 == octet) {
            this.mBody = parseParts(this.mPduDataStream);
            if (this.mBody == null) {
                Log.i(LOG_TAG, "Parse parts Failed");
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
            case RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX:
                break;
            case 132:
                RetrieveConf retrieveConf2 = new RetrieveConf(this.mHeaders, this.mBody);
                byte[] contentType = retrieveConf2.getContentType();
                if (contentType == null) {
                    Log.i(LOG_TAG, "Parse MESSAGE_TYPE_RETRIEVE_CONF Failed: content Type is null");
                    break;
                } else {
                    String lowerCase2 = new String(contentType).toLowerCase();
                    if (!lowerCase2.equals("application/vnd.wap.multipart.mixed") && !lowerCase2.equals("application/vnd.wap.multipart.related") && !lowerCase2.equals("application/vnd.wap.multipart.alternative") && !lowerCase2.equals("text/plain")) {
                        if (lowerCase2.equals("application/vnd.wap.multipart.alternative")) {
                            PduPart part = this.mBody.getPart(0);
                            this.mBody.removeAll();
                            this.mBody.addPart(0, part);
                        } else {
                            Log.i(LOG_TAG, "Parse MESSAGE_TYPE_RETRIEVE_CONF Failed: content Type is null _2");
                        }
                        break;
                    }
                }
                break;
            case 133:
                break;
            case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
                break;
            case 135:
                break;
            case 136:
                break;
            default:
                Log.d(LOG_TAG, "Parser doesn't support this message type in this version!");
                break;
        }
        return null;
    }

    protected void parseContentTypeParams(ByteArrayInputStream byteArrayInputStream, HashMap<Integer, Object> map, Integer num) {
        int iAvailable;
        int iIntValue;
        byte[] bytes;
        int iAvailable2 = byteArrayInputStream.available();
        int iIntValue2 = num.intValue();
        while (iIntValue2 > 0) {
            iIntValue2--;
            switch (byteArrayInputStream.read()) {
                case 129:
                    byteArrayInputStream.mark(1);
                    int iExtractByteValue = extractByteValue(byteArrayInputStream);
                    byteArrayInputStream.reset();
                    if ((iExtractByteValue > 32 && iExtractByteValue < 127) || iExtractByteValue == 0) {
                        byte[] wapString = parseWapString(byteArrayInputStream, 0);
                        try {
                            int mibEnumValue = MtkCharacterSets.getMibEnumValue(new String(wapString));
                            Log.i(LOG_TAG, "Parse CharacterSets: charsetStr");
                            if (map != null) {
                                map.put(129, Integer.valueOf(mibEnumValue));
                            }
                        } catch (UnsupportedEncodingException e) {
                            Log.e(LOG_TAG, Arrays.toString(wapString), e);
                            if (map != null) {
                                map.put(129, 0);
                            }
                        }
                    } else {
                        int integerValue = (int) parseIntegerValue(byteArrayInputStream);
                        if (map != null) {
                            Log.i(LOG_TAG, "Parse Well-known-charset: charset");
                            map.put(129, Integer.valueOf(integerValue));
                        }
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case 130:
                case 132:
                case 135:
                case 136:
                case 139:
                case 142:
                case 144:
                case 145:
                case MtkPduPart.P_DATE:
                case 147:
                case 148:
                case 149:
                case 150:
                case 154:
                default:
                    if (-1 == skipWapValue(byteArrayInputStream, iIntValue2)) {
                        Log.e(LOG_TAG, "Corrupt Content-Type");
                    } else {
                        iIntValue2 = 0;
                        continue;
                    }
                    break;
                case RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX:
                case MtkPduHeaders.STATE_SKIP_RETRYING:
                    byteArrayInputStream.mark(1);
                    int iExtractByteValue2 = extractByteValue(byteArrayInputStream);
                    byteArrayInputStream.reset();
                    if (iExtractByteValue2 > 127) {
                        int shortInteger = parseShortInteger(byteArrayInputStream);
                        if (shortInteger < PduContentTypes.contentTypes.length && (bytes = PduContentTypes.contentTypes[shortInteger].getBytes()) != null && map != null) {
                            map.put(Integer.valueOf(RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX), bytes);
                        }
                    } else {
                        byte[] wapString2 = parseWapString(byteArrayInputStream, 0);
                        if (wapString2 != null && map != null) {
                            map.put(Integer.valueOf(RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX), wapString2);
                        }
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case 133:
                case 151:
                    byte[] wapString3 = parseWapString(byteArrayInputStream, 0);
                    if (wapString3 != null && map != null) {
                        map.put(151, wapString3);
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
                case 152:
                    byte[] wapString4 = parseWapString(byteArrayInputStream, 0);
                    if (wapString4 != null && map != null) {
                        map.put(152, wapString4);
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case 138:
                case 153:
                    byte[] wapString5 = parseWapString(byteArrayInputStream, 0);
                    if (wapString5 != null && map != null) {
                        map.put(153, wapString5);
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case 140:
                case 155:
                    byte[] wapString6 = parseWapString(byteArrayInputStream, 0);
                    if (wapString6 != null && map != null) {
                        map.put(155, wapString6);
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case 141:
                case 156:
                    byte[] wapString7 = parseWapString(byteArrayInputStream, 0);
                    if (wapString7 != null && map != null) {
                        map.put(156, wapString7);
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
                case 143:
                case 157:
                    byte[] wapString8 = parseWapString(byteArrayInputStream, 0);
                    if (wapString8 != null && map != null) {
                        map.put(157, wapString8);
                    }
                    iAvailable = byteArrayInputStream.available();
                    iIntValue = num.intValue();
                    break;
            }
            iIntValue2 = iIntValue - (iAvailable2 - iAvailable);
        }
        if (iIntValue2 != 0) {
            Log.e(LOG_TAG, "Corrupt Content-Type");
        }
    }

    protected PduHeaders parseHeaders(ByteArrayInputStream byteArrayInputStream) {
        byte[] wapString;
        int iExtractByteValue;
        EncodedStringValue encodedStringValue;
        byte[] textString;
        EncodedStringValue encodedStringValue2;
        if (byteArrayInputStream == null) {
            return null;
        }
        MtkPduHeaders mtkPduHeaders = new MtkPduHeaders();
        boolean z = true;
        while (z && byteArrayInputStream.available() > 0) {
            byteArrayInputStream.mark(1);
            int iExtractByteValue2 = extractByteValue(byteArrayInputStream);
            if (iExtractByteValue2 >= 32 && iExtractByteValue2 <= 127) {
                byteArrayInputStream.reset();
                parseWapString(byteArrayInputStream, 0);
            } else if (iExtractByteValue2 != 175) {
                if (iExtractByteValue2 != 201) {
                    switch (iExtractByteValue2) {
                        case 129:
                        case 130:
                        case 151:
                            EncodedStringValue encodedStringValue3 = parseEncodedStringValue(byteArrayInputStream);
                            if (encodedStringValue3 == null) {
                                continue;
                            } else {
                                byte[] textString2 = encodedStringValue3.getTextString();
                                if (textString2 != null) {
                                    String str = new String(textString2);
                                    int iIndexOf = str.indexOf("/");
                                    if (iIndexOf > 0) {
                                        str = str.substring(0, iIndexOf);
                                    }
                                    try {
                                        encodedStringValue3.setTextString(str.getBytes());
                                    } catch (NullPointerException e) {
                                        log("null pointer error!");
                                        return null;
                                    }
                                }
                                try {
                                    mtkPduHeaders.appendEncodedStringValue(encodedStringValue3, iExtractByteValue2);
                                } catch (NullPointerException e2) {
                                    log("null pointer error!");
                                } catch (RuntimeException e3) {
                                    log(iExtractByteValue2 + "is not Encoded-String-Value header field!");
                                    return null;
                                }
                            }
                            break;
                        case RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX:
                        case 139:
                        case 152:
                        case 158:
                            wapString = parseWapString(byteArrayInputStream, 0);
                            if (wapString == null) {
                                try {
                                    mtkPduHeaders.setTextString(wapString, iExtractByteValue2);
                                } catch (NullPointerException e4) {
                                    log("null pointer error!");
                                } catch (RuntimeException e5) {
                                    log(iExtractByteValue2 + "is not Text-String header field!");
                                    return null;
                                }
                            }
                            break;
                        case 132:
                            HashMap map = new HashMap();
                            byte[] contentType = parseContentType(byteArrayInputStream, map);
                            if (contentType != null) {
                                try {
                                    mtkPduHeaders.setTextString(contentType, 132);
                                } catch (NullPointerException e6) {
                                    log("null pointer error!");
                                } catch (RuntimeException e7) {
                                    log(iExtractByteValue2 + "is not Text-String header field!");
                                    return null;
                                }
                            }
                            mStartParam = (byte[]) map.get(153);
                            mTypeParam = (byte[]) map.get(Integer.valueOf(RadioCdmaSmsConst.UDH_EO_DATA_SEGMENT_MAX));
                            z = false;
                            break;
                        case 133:
                        case 142:
                        case 159:
                            break;
                        case RadioCdmaSmsConst.UDH_VAR_PIC_SIZE:
                        case 143:
                        case 144:
                        case 145:
                        case MtkPduPart.P_DATE:
                        case 148:
                        case 149:
                        case 153:
                        case 155:
                        case 156:
                        case 162:
                        case 163:
                        case 165:
                        case MtkPduPart.P_TRANSFER_ENCODING:
                            iExtractByteValue = extractByteValue(byteArrayInputStream);
                            try {
                                mtkPduHeaders.setOctet(iExtractByteValue, iExtractByteValue2);
                            } catch (RuntimeException e8) {
                                log(iExtractByteValue2 + "is not Octet header field!");
                                return null;
                            } catch (InvalidHeaderValueException e9) {
                                log("Set invalid Octet value: " + iExtractByteValue + " into the header filed: " + iExtractByteValue2);
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
                                    mtkPduHeaders.setLongInteger(longInteger, iExtractByteValue2);
                                } catch (RuntimeException e10) {
                                    log(iExtractByteValue2 + "is not Long-Integer header field!");
                                    return null;
                                }
                            } catch (RuntimeException e11) {
                                log(iExtractByteValue2 + "is not Long-Integer header field!");
                                return null;
                            }
                            break;
                        case MtkPduHeaders.STATE_SKIP_RETRYING:
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
                                    } catch (NullPointerException e12) {
                                        log("null pointer error!");
                                        return null;
                                    }
                                }
                            } else {
                                try {
                                    encodedStringValue = new EncodedStringValue("insert-address-token".getBytes());
                                } catch (NullPointerException e13) {
                                    log(iExtractByteValue2 + "is not Encoded-String-Value header field!");
                                    return null;
                                }
                            }
                            try {
                                mtkPduHeaders.setEncodedStringValue(encodedStringValue, MtkPduHeaders.STATE_SKIP_RETRYING);
                            } catch (NullPointerException e14) {
                                log("null pointer error!");
                            } catch (RuntimeException e15) {
                                log(iExtractByteValue2 + "is not Encoded-String-Value header field!");
                                return null;
                            }
                            break;
                        case 138:
                            byteArrayInputStream.mark(1);
                            int iExtractByteValue4 = extractByteValue(byteArrayInputStream);
                            if (iExtractByteValue4 >= 128) {
                                if (128 == iExtractByteValue4) {
                                    try {
                                        mtkPduHeaders.setTextString("personal".getBytes(), 138);
                                    } catch (NullPointerException e16) {
                                        log("null pointer error!");
                                    } catch (RuntimeException e17) {
                                        log(iExtractByteValue2 + "is not Text-String header field!");
                                        return null;
                                    }
                                } else if (129 == iExtractByteValue4) {
                                    mtkPduHeaders.setTextString("advertisement".getBytes(), 138);
                                } else if (130 == iExtractByteValue4) {
                                    mtkPduHeaders.setTextString("informational".getBytes(), 138);
                                } else if (131 == iExtractByteValue4) {
                                    mtkPduHeaders.setTextString("auto".getBytes(), 138);
                                }
                            } else {
                                byteArrayInputStream.reset();
                                byte[] wapString2 = parseWapString(byteArrayInputStream, 0);
                                if (wapString2 != null) {
                                    try {
                                        mtkPduHeaders.setTextString(wapString2, 138);
                                    } catch (NullPointerException e18) {
                                        log("null pointer error!");
                                    } catch (RuntimeException e19) {
                                        log(iExtractByteValue2 + "is not Text-String header field!");
                                        return null;
                                    }
                                }
                            }
                            break;
                        case 140:
                            int iExtractByteValue5 = extractByteValue(byteArrayInputStream);
                            switch (iExtractByteValue5) {
                                case MtkPduHeaders.STATE_SKIP_RETRYING:
                                case 138:
                                case 139:
                                case 140:
                                case 141:
                                case 142:
                                case 143:
                                case 144:
                                case 145:
                                case MtkPduPart.P_DATE:
                                case 147:
                                case 148:
                                case 149:
                                case 150:
                                case 151:
                                    Log.i(LOG_TAG, "PduParser: parseHeaders: We don't support these kind of messages now.");
                                    break;
                                default:
                                    try {
                                        mtkPduHeaders.setOctet(iExtractByteValue5, iExtractByteValue2);
                                    } catch (InvalidHeaderValueException e20) {
                                        log("Set invalid Octet value: " + iExtractByteValue5 + " into the header filed: " + iExtractByteValue2);
                                        return null;
                                    } catch (RuntimeException e21) {
                                        log(iExtractByteValue2 + "is not Octet header field!");
                                        return null;
                                    }
                                    break;
                            }
                            break;
                        case 141:
                            int shortInteger = parseShortInteger(byteArrayInputStream);
                            try {
                                mtkPduHeaders.setOctet(shortInteger, 141);
                            } catch (InvalidHeaderValueException e22) {
                                log("Set invalid Octet value: " + shortInteger + " into the header filed: " + iExtractByteValue2);
                                return null;
                            } catch (RuntimeException e23) {
                                log(iExtractByteValue2 + "is not Octet header field!");
                                return null;
                            }
                            break;
                        case 147:
                        case 150:
                        case 154:
                        case 166:
                            encodedStringValue2 = parseEncodedStringValue(byteArrayInputStream);
                            if (encodedStringValue2 == null) {
                                try {
                                    mtkPduHeaders.setEncodedStringValue(encodedStringValue2, iExtractByteValue2);
                                } catch (NullPointerException e24) {
                                    log("null pointer error!");
                                } catch (RuntimeException e25) {
                                    log(iExtractByteValue2 + "is not Encoded-String-Value header field!");
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
                                        mtkPduHeaders.setEncodedStringValue(encodedStringValue4, 160);
                                    } catch (NullPointerException e26) {
                                        log("null pointer error!");
                                    } catch (RuntimeException e27) {
                                        log(iExtractByteValue2 + "is not Encoded-String-Value header field!");
                                        return null;
                                    }
                                }
                            } catch (RuntimeException e28) {
                                log(iExtractByteValue2 + " is not Integer-Value");
                                return null;
                            }
                            break;
                        case 161:
                            parseValueLength(byteArrayInputStream);
                            try {
                                parseIntegerValue(byteArrayInputStream);
                                try {
                                    mtkPduHeaders.setLongInteger(parseLongInteger(byteArrayInputStream), 161);
                                } catch (RuntimeException e29) {
                                    log(iExtractByteValue2 + "is not Long-Integer header field!");
                                    return null;
                                }
                            } catch (RuntimeException e30) {
                                log(iExtractByteValue2 + " is not Integer-Value");
                                return null;
                            }
                            break;
                        case 164:
                            parseValueLength(byteArrayInputStream);
                            extractByteValue(byteArrayInputStream);
                            parseEncodedStringValue(byteArrayInputStream);
                            break;
                        default:
                            switch (iExtractByteValue2) {
                                case 169:
                                case 171:
                                    iExtractByteValue = extractByteValue(byteArrayInputStream);
                                    mtkPduHeaders.setOctet(iExtractByteValue, iExtractByteValue2);
                                    break;
                                case 170:
                                case 172:
                                    parseValueLength(byteArrayInputStream);
                                    extractByteValue(byteArrayInputStream);
                                    try {
                                        parseIntegerValue(byteArrayInputStream);
                                    } catch (RuntimeException e31) {
                                        log(iExtractByteValue2 + " is not Integer-Value");
                                        return null;
                                    }
                                    break;
                                default:
                                    switch (iExtractByteValue2) {
                                        case 177:
                                        case 180:
                                        case 186:
                                        case 187:
                                        case 188:
                                        case 191:
                                            iExtractByteValue = extractByteValue(byteArrayInputStream);
                                            mtkPduHeaders.setOctet(iExtractByteValue, iExtractByteValue2);
                                            break;
                                        case 178:
                                            parseContentType(byteArrayInputStream, null);
                                            break;
                                        case 179:
                                            break;
                                        case 181:
                                        case 182:
                                            encodedStringValue2 = parseEncodedStringValue(byteArrayInputStream);
                                            if (encodedStringValue2 == null) {
                                            }
                                            break;
                                        case 183:
                                        case 184:
                                        case 185:
                                        case 189:
                                        case 190:
                                            wapString = parseWapString(byteArrayInputStream, 0);
                                            if (wapString == null) {
                                            }
                                            break;
                                        default:
                                            log("Unknown header");
                                            break;
                                    }
                                case 173:
                                    break;
                            }
                            break;
                    }
                    return null;
                }
                Log.d(LOG_TAG, "parseHeaders " + iExtractByteValue2);
                try {
                    long longInteger2 = parseLongInteger(byteArrayInputStream);
                    Log.d(LOG_TAG, "value = " + longInteger2);
                    if (iExtractByteValue2 == 133) {
                        mtkPduHeaders.setLongInteger(longInteger2, 201);
                        if (!this.mForRestore) {
                            longInteger2 = System.currentTimeMillis() / 1000;
                        }
                    }
                    mtkPduHeaders.setLongInteger(longInteger2, iExtractByteValue2);
                } catch (RuntimeException e32) {
                    log(iExtractByteValue2 + "is not Long-Integer header field!");
                    return null;
                }
            } else {
                try {
                    mtkPduHeaders.setLongInteger(parseIntegerValue(byteArrayInputStream), iExtractByteValue2);
                } catch (RuntimeException e33) {
                    log(iExtractByteValue2 + "is not Long-Integer header field!");
                    return null;
                }
            }
        }
        return mtkPduHeaders;
    }

    public GenericPdu parse(boolean z) {
        this.mForRestore = z;
        return parse();
    }

    protected boolean parsePartHeaders(ByteArrayInputStream byteArrayInputStream, PduPart pduPart, int i) {
        int iAvailable = byteArrayInputStream.available();
        int iAvailable2 = i;
        while (iAvailable2 > 0) {
            int i2 = byteArrayInputStream.read();
            iAvailable2--;
            Log.v(LOG_TAG, "Part headers: " + i2);
            if (i2 > 127) {
                if (i2 == 142) {
                    byte[] wapString = parseWapString(byteArrayInputStream, 0);
                    if (wapString != null) {
                        pduPart.setContentLocation(wapString);
                    }
                    iAvailable2 = i - (iAvailable - byteArrayInputStream.available());
                } else if (i2 == 167) {
                    byte[] wapString2 = parseWapString(byteArrayInputStream, 0);
                    if (wapString2 != null) {
                        pduPart.setContentTransferEncoding(wapString2);
                    }
                    iAvailable2 = i - (iAvailable - byteArrayInputStream.available());
                } else {
                    if (i2 != 174) {
                        if (i2 == 192) {
                            byte[] wapString3 = parseWapString(byteArrayInputStream, 1);
                            if (wapString3 != null) {
                                pduPart.setContentId(wapString3);
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
                byte[] wapString4 = parseWapString(byteArrayInputStream, 0);
                byte[] wapString5 = parseWapString(byteArrayInputStream, 0);
                if (true == "Content-Transfer-Encoding".equalsIgnoreCase(new String(wapString4))) {
                    pduPart.setContentTransferEncoding(wapString5);
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
}
