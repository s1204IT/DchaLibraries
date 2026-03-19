package com.android.internal.telephony;

import android.hardware.radio.V1_0.RadioError;
import com.google.android.mms.ContentType;
import java.util.HashMap;

public class WspTypeDecoder {
    public static final String CONTENT_TYPE_B_MMS = "application/vnd.wap.mms-message";
    public static final String CONTENT_TYPE_B_PUSH_CO = "application/vnd.wap.coc";
    public static final String CONTENT_TYPE_B_PUSH_SYNCML_NOTI = "application/vnd.syncml.notification";
    public static final int PARAMETER_ID_X_WAP_APPLICATION_ID = 47;
    public static final int PDU_TYPE_CONFIRMED_PUSH = 7;
    public static final int PDU_TYPE_PUSH = 6;
    private static final int Q_VALUE = 0;
    protected static final int WAP_PDU_LENGTH_QUOTE = 31;
    private static final int WAP_PDU_SHORT_LENGTH_MAX = 30;
    private static final HashMap<Integer, String> WELL_KNOWN_MIME_TYPES = new HashMap<>();
    private static final HashMap<Integer, String> WELL_KNOWN_PARAMETERS = new HashMap<>();
    HashMap<String, String> mContentParameters;
    protected int mDataLength;
    protected String mStringValue;
    protected long mUnsigned32bit;
    protected byte[] mWspData;

    static {
        WELL_KNOWN_MIME_TYPES.put(0, "*/*");
        WELL_KNOWN_MIME_TYPES.put(1, "text/*");
        WELL_KNOWN_MIME_TYPES.put(2, ContentType.TEXT_HTML);
        WELL_KNOWN_MIME_TYPES.put(3, ContentType.TEXT_PLAIN);
        WELL_KNOWN_MIME_TYPES.put(4, "text/x-hdml");
        WELL_KNOWN_MIME_TYPES.put(5, "text/x-ttml");
        WELL_KNOWN_MIME_TYPES.put(6, ContentType.TEXT_VCALENDAR);
        WELL_KNOWN_MIME_TYPES.put(7, ContentType.TEXT_VCARD);
        WELL_KNOWN_MIME_TYPES.put(8, "text/vnd.wap.wml");
        WELL_KNOWN_MIME_TYPES.put(9, "text/vnd.wap.wmlscript");
        WELL_KNOWN_MIME_TYPES.put(10, "text/vnd.wap.wta-event");
        WELL_KNOWN_MIME_TYPES.put(11, "multipart/*");
        WELL_KNOWN_MIME_TYPES.put(12, "multipart/mixed");
        WELL_KNOWN_MIME_TYPES.put(13, "multipart/form-data");
        WELL_KNOWN_MIME_TYPES.put(14, "multipart/byterantes");
        WELL_KNOWN_MIME_TYPES.put(15, "multipart/alternative");
        WELL_KNOWN_MIME_TYPES.put(16, "application/*");
        WELL_KNOWN_MIME_TYPES.put(17, "application/java-vm");
        WELL_KNOWN_MIME_TYPES.put(18, "application/x-www-form-urlencoded");
        WELL_KNOWN_MIME_TYPES.put(19, "application/x-hdmlc");
        WELL_KNOWN_MIME_TYPES.put(20, "application/vnd.wap.wmlc");
        WELL_KNOWN_MIME_TYPES.put(21, "application/vnd.wap.wmlscriptc");
        WELL_KNOWN_MIME_TYPES.put(22, "application/vnd.wap.wta-eventc");
        WELL_KNOWN_MIME_TYPES.put(23, "application/vnd.wap.uaprof");
        WELL_KNOWN_MIME_TYPES.put(24, "application/vnd.wap.wtls-ca-certificate");
        WELL_KNOWN_MIME_TYPES.put(25, "application/vnd.wap.wtls-user-certificate");
        WELL_KNOWN_MIME_TYPES.put(26, "application/x-x509-ca-cert");
        WELL_KNOWN_MIME_TYPES.put(27, "application/x-x509-user-cert");
        WELL_KNOWN_MIME_TYPES.put(28, ContentType.IMAGE_UNSPECIFIED);
        WELL_KNOWN_MIME_TYPES.put(29, ContentType.IMAGE_GIF);
        WELL_KNOWN_MIME_TYPES.put(30, ContentType.IMAGE_JPEG);
        WELL_KNOWN_MIME_TYPES.put(31, "image/tiff");
        WELL_KNOWN_MIME_TYPES.put(32, ContentType.IMAGE_PNG);
        WELL_KNOWN_MIME_TYPES.put(33, ContentType.IMAGE_WBMP);
        WELL_KNOWN_MIME_TYPES.put(34, "application/vnd.wap.multipart.*");
        WELL_KNOWN_MIME_TYPES.put(35, ContentType.MULTIPART_MIXED);
        WELL_KNOWN_MIME_TYPES.put(36, "application/vnd.wap.multipart.form-data");
        WELL_KNOWN_MIME_TYPES.put(37, "application/vnd.wap.multipart.byteranges");
        WELL_KNOWN_MIME_TYPES.put(38, ContentType.MULTIPART_ALTERNATIVE);
        WELL_KNOWN_MIME_TYPES.put(39, "application/xml");
        WELL_KNOWN_MIME_TYPES.put(40, "text/xml");
        WELL_KNOWN_MIME_TYPES.put(41, "application/vnd.wap.wbxml");
        WELL_KNOWN_MIME_TYPES.put(42, "application/x-x968-cross-cert");
        WELL_KNOWN_MIME_TYPES.put(43, "application/x-x968-ca-cert");
        WELL_KNOWN_MIME_TYPES.put(44, "application/x-x968-user-cert");
        WELL_KNOWN_MIME_TYPES.put(45, "text/vnd.wap.si");
        WELL_KNOWN_MIME_TYPES.put(46, "application/vnd.wap.sic");
        WELL_KNOWN_MIME_TYPES.put(47, "text/vnd.wap.sl");
        WELL_KNOWN_MIME_TYPES.put(48, "application/vnd.wap.slc");
        WELL_KNOWN_MIME_TYPES.put(49, "text/vnd.wap.co");
        WELL_KNOWN_MIME_TYPES.put(50, CONTENT_TYPE_B_PUSH_CO);
        WELL_KNOWN_MIME_TYPES.put(51, ContentType.MULTIPART_RELATED);
        WELL_KNOWN_MIME_TYPES.put(52, "application/vnd.wap.sia");
        WELL_KNOWN_MIME_TYPES.put(53, "text/vnd.wap.connectivity-xml");
        WELL_KNOWN_MIME_TYPES.put(54, "application/vnd.wap.connectivity-wbxml");
        WELL_KNOWN_MIME_TYPES.put(55, "application/pkcs7-mime");
        WELL_KNOWN_MIME_TYPES.put(56, "application/vnd.wap.hashed-certificate");
        WELL_KNOWN_MIME_TYPES.put(57, "application/vnd.wap.signed-certificate");
        WELL_KNOWN_MIME_TYPES.put(58, "application/vnd.wap.cert-response");
        WELL_KNOWN_MIME_TYPES.put(59, ContentType.APP_XHTML);
        WELL_KNOWN_MIME_TYPES.put(60, "application/wml+xml");
        WELL_KNOWN_MIME_TYPES.put(61, "text/css");
        WELL_KNOWN_MIME_TYPES.put(62, "application/vnd.wap.mms-message");
        WELL_KNOWN_MIME_TYPES.put(63, "application/vnd.wap.rollover-certificate");
        WELL_KNOWN_MIME_TYPES.put(64, "application/vnd.wap.locc+wbxml");
        WELL_KNOWN_MIME_TYPES.put(65, "application/vnd.wap.loc+xml");
        WELL_KNOWN_MIME_TYPES.put(66, "application/vnd.syncml.dm+wbxml");
        WELL_KNOWN_MIME_TYPES.put(67, "application/vnd.syncml.dm+xml");
        WELL_KNOWN_MIME_TYPES.put(68, CONTENT_TYPE_B_PUSH_SYNCML_NOTI);
        WELL_KNOWN_MIME_TYPES.put(69, ContentType.APP_WAP_XHTML);
        WELL_KNOWN_MIME_TYPES.put(70, "application/vnd.wv.csp.cir");
        WELL_KNOWN_MIME_TYPES.put(71, "application/vnd.oma.dd+xml");
        WELL_KNOWN_MIME_TYPES.put(72, "application/vnd.oma.drm.message");
        WELL_KNOWN_MIME_TYPES.put(73, ContentType.APP_DRM_CONTENT);
        WELL_KNOWN_MIME_TYPES.put(74, "application/vnd.oma.drm.rights+xml");
        WELL_KNOWN_MIME_TYPES.put(75, "application/vnd.oma.drm.rights+wbxml");
        WELL_KNOWN_MIME_TYPES.put(76, "application/vnd.wv.csp+xml");
        WELL_KNOWN_MIME_TYPES.put(77, "application/vnd.wv.csp+wbxml");
        WELL_KNOWN_MIME_TYPES.put(78, "application/vnd.syncml.ds.notification");
        WELL_KNOWN_MIME_TYPES.put(79, ContentType.AUDIO_UNSPECIFIED);
        WELL_KNOWN_MIME_TYPES.put(80, ContentType.VIDEO_UNSPECIFIED);
        WELL_KNOWN_MIME_TYPES.put(81, "application/vnd.oma.dd2+xml");
        WELL_KNOWN_MIME_TYPES.put(82, "application/mikey");
        WELL_KNOWN_MIME_TYPES.put(83, "application/vnd.oma.dcd");
        WELL_KNOWN_MIME_TYPES.put(84, "application/vnd.oma.dcdc");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_13), "application/vnd.uplanet.cacheop-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_14), "application/vnd.uplanet.signal");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_15), "application/vnd.uplanet.alert-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_16), "application/vnd.uplanet.list-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_17), "application/vnd.uplanet.listcmd-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_18), "application/vnd.uplanet.channel-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_19), "application/vnd.uplanet.provisioning-status-uri");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_20), "x-wap.multipart/vnd.uplanet.header-set");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_21), "application/vnd.uplanet.bearer-choice-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_22), "application/vnd.phonecom.mmc-wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_23), "application/vnd.nokia.syncset+wbxml");
        WELL_KNOWN_MIME_TYPES.put(Integer.valueOf(RadioError.OEM_ERROR_24), "image/x-up-wpng");
        WELL_KNOWN_MIME_TYPES.put(768, "application/iota.mmc-wbxml");
        WELL_KNOWN_MIME_TYPES.put(769, "application/iota.mmc-xml");
        WELL_KNOWN_MIME_TYPES.put(770, "application/vnd.syncml+xml");
        WELL_KNOWN_MIME_TYPES.put(771, "application/vnd.syncml+wbxml");
        WELL_KNOWN_MIME_TYPES.put(772, "text/vnd.wap.emn+xml");
        WELL_KNOWN_MIME_TYPES.put(773, "text/calendar");
        WELL_KNOWN_MIME_TYPES.put(774, "application/vnd.omads-email+xml");
        WELL_KNOWN_MIME_TYPES.put(775, "application/vnd.omads-file+xml");
        WELL_KNOWN_MIME_TYPES.put(776, "application/vnd.omads-folder+xml");
        WELL_KNOWN_MIME_TYPES.put(777, "text/directory;profile=vCard");
        WELL_KNOWN_MIME_TYPES.put(778, "application/vnd.wap.emn+wbxml");
        WELL_KNOWN_MIME_TYPES.put(779, "application/vnd.nokia.ipdc-purchase-response");
        WELL_KNOWN_MIME_TYPES.put(780, "application/vnd.motorola.screen3+xml");
        WELL_KNOWN_MIME_TYPES.put(781, "application/vnd.motorola.screen3+gzip");
        WELL_KNOWN_MIME_TYPES.put(782, "application/vnd.cmcc.setting+wbxml");
        WELL_KNOWN_MIME_TYPES.put(783, "application/vnd.cmcc.bombing+wbxml");
        WELL_KNOWN_MIME_TYPES.put(784, "application/vnd.docomo.pf");
        WELL_KNOWN_MIME_TYPES.put(785, "application/vnd.docomo.ub");
        WELL_KNOWN_MIME_TYPES.put(786, "application/vnd.omaloc-supl-init");
        WELL_KNOWN_MIME_TYPES.put(787, "application/vnd.oma.group-usage-list+xml");
        WELL_KNOWN_MIME_TYPES.put(788, "application/oma-directory+xml");
        WELL_KNOWN_MIME_TYPES.put(789, "application/vnd.docomo.pf2");
        WELL_KNOWN_MIME_TYPES.put(790, "application/vnd.oma.drm.roap-trigger+wbxml");
        WELL_KNOWN_MIME_TYPES.put(791, "application/vnd.sbm.mid2");
        WELL_KNOWN_MIME_TYPES.put(792, "application/vnd.wmf.bootstrap");
        WELL_KNOWN_MIME_TYPES.put(793, "application/vnc.cmcc.dcd+xml");
        WELL_KNOWN_MIME_TYPES.put(794, "application/vnd.sbm.cid");
        WELL_KNOWN_MIME_TYPES.put(795, "application/vnd.oma.bcast.provisioningtrigger");
        WELL_KNOWN_PARAMETERS.put(0, "Q");
        WELL_KNOWN_PARAMETERS.put(1, "Charset");
        WELL_KNOWN_PARAMETERS.put(2, "Level");
        WELL_KNOWN_PARAMETERS.put(3, "Type");
        WELL_KNOWN_PARAMETERS.put(7, "Differences");
        WELL_KNOWN_PARAMETERS.put(8, "Padding");
        WELL_KNOWN_PARAMETERS.put(9, "Type");
        WELL_KNOWN_PARAMETERS.put(14, "Max-Age");
        WELL_KNOWN_PARAMETERS.put(16, "Secure");
        WELL_KNOWN_PARAMETERS.put(17, "SEC");
        WELL_KNOWN_PARAMETERS.put(18, "MAC");
        WELL_KNOWN_PARAMETERS.put(19, "Creation-date");
        WELL_KNOWN_PARAMETERS.put(20, "Modification-date");
        WELL_KNOWN_PARAMETERS.put(21, "Read-date");
        WELL_KNOWN_PARAMETERS.put(22, "Size");
        WELL_KNOWN_PARAMETERS.put(23, "Name");
        WELL_KNOWN_PARAMETERS.put(24, "Filename");
        WELL_KNOWN_PARAMETERS.put(25, "Start");
        WELL_KNOWN_PARAMETERS.put(26, "Start-info");
        WELL_KNOWN_PARAMETERS.put(27, "Comment");
        WELL_KNOWN_PARAMETERS.put(28, "Domain");
        WELL_KNOWN_PARAMETERS.put(29, "Path");
    }

    public WspTypeDecoder(byte[] bArr) {
        this.mWspData = bArr;
    }

    public boolean decodeTextString(int i) {
        int i2 = i;
        while (this.mWspData[i2] != 0) {
            i2++;
        }
        this.mDataLength = (i2 - i) + 1;
        if (this.mWspData[i] == 127) {
            this.mStringValue = new String(this.mWspData, i + 1, this.mDataLength - 2);
        } else {
            this.mStringValue = new String(this.mWspData, i, this.mDataLength - 1);
        }
        return true;
    }

    public boolean decodeTokenText(int i) {
        int i2 = i;
        while (this.mWspData[i2] != 0) {
            i2++;
        }
        this.mDataLength = (i2 - i) + 1;
        this.mStringValue = new String(this.mWspData, i, this.mDataLength - 1);
        return true;
    }

    public boolean decodeShortInteger(int i) {
        if ((this.mWspData[i] & 128) == 0) {
            return false;
        }
        this.mUnsigned32bit = this.mWspData[i] & 127;
        this.mDataLength = 1;
        return true;
    }

    public boolean decodeLongInteger(int i) {
        int i2 = this.mWspData[i] & 255;
        if (i2 > 30) {
            return false;
        }
        this.mUnsigned32bit = 0L;
        for (int i3 = 1; i3 <= i2; i3++) {
            this.mUnsigned32bit = (this.mUnsigned32bit << 8) | ((long) (this.mWspData[i + i3] & 255));
        }
        this.mDataLength = i2 + 1;
        return true;
    }

    public boolean decodeIntegerValue(int i) {
        if (decodeShortInteger(i)) {
            return true;
        }
        return decodeLongInteger(i);
    }

    public boolean decodeUintvarInteger(int i) {
        this.mUnsigned32bit = 0L;
        int i2 = i;
        while ((this.mWspData[i2] & 128) != 0) {
            if (i2 - i >= 4) {
                return false;
            }
            this.mUnsigned32bit = (this.mUnsigned32bit << 7) | ((long) (this.mWspData[i2] & 127));
            i2++;
        }
        this.mUnsigned32bit = (this.mUnsigned32bit << 7) | ((long) (this.mWspData[i2] & 127));
        this.mDataLength = (i2 - i) + 1;
        return true;
    }

    public boolean decodeValueLength(int i) {
        if ((this.mWspData[i] & 255) > 31) {
            return false;
        }
        if (this.mWspData[i] < 31) {
            this.mUnsigned32bit = this.mWspData[i];
            this.mDataLength = 1;
        } else {
            decodeUintvarInteger(i + 1);
            this.mDataLength++;
        }
        return true;
    }

    public boolean decodeExtensionMedia(int i) {
        boolean z = false;
        this.mDataLength = 0;
        this.mStringValue = null;
        int length = this.mWspData.length;
        if (i < length) {
            z = true;
        }
        int i2 = i;
        while (i2 < length && this.mWspData[i2] != 0) {
            i2++;
        }
        this.mDataLength = (i2 - i) + 1;
        this.mStringValue = new String(this.mWspData, i, this.mDataLength - 1);
        return z;
    }

    public boolean decodeConstrainedEncoding(int i) {
        if (decodeShortInteger(i)) {
            this.mStringValue = null;
            return true;
        }
        return decodeExtensionMedia(i);
    }

    public boolean decodeContentType(int i) {
        this.mContentParameters = new HashMap<>();
        try {
            if (!decodeValueLength(i)) {
                boolean zDecodeConstrainedEncoding = decodeConstrainedEncoding(i);
                if (zDecodeConstrainedEncoding) {
                    expandWellKnownMimeType();
                }
                return zDecodeConstrainedEncoding;
            }
            int i2 = (int) this.mUnsigned32bit;
            int decodedDataLength = getDecodedDataLength();
            int i3 = i + decodedDataLength;
            if (decodeIntegerValue(i3)) {
                this.mDataLength += decodedDataLength;
                int i4 = this.mDataLength;
                this.mStringValue = null;
                expandWellKnownMimeType();
                long j = this.mUnsigned32bit;
                String str = this.mStringValue;
                if (!readContentParameters(i + this.mDataLength, i2 - (this.mDataLength - decodedDataLength), 0)) {
                    return false;
                }
                this.mDataLength += i4;
                this.mUnsigned32bit = j;
                this.mStringValue = str;
                return true;
            }
            if (decodeExtensionMedia(i3)) {
                this.mDataLength += decodedDataLength;
                int i5 = this.mDataLength;
                expandWellKnownMimeType();
                long j2 = this.mUnsigned32bit;
                String str2 = this.mStringValue;
                if (readContentParameters(i + this.mDataLength, i2 - (this.mDataLength - decodedDataLength), 0)) {
                    this.mDataLength += i5;
                    this.mUnsigned32bit = j2;
                    this.mStringValue = str2;
                    return true;
                }
            }
            return false;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    private boolean readContentParameters(int i, int i2, int i3) {
        int i4;
        String str;
        int i5;
        String strSubstring;
        if (i2 > 0) {
            byte b = this.mWspData[i];
            if ((b & 128) == 0 && b > 31) {
                decodeTokenText(i);
                str = this.mStringValue;
                i4 = 0 + this.mDataLength;
            } else {
                if (!decodeIntegerValue(i)) {
                    return false;
                }
                i4 = this.mDataLength + 0;
                int i6 = (int) this.mUnsigned32bit;
                str = WELL_KNOWN_PARAMETERS.get(Integer.valueOf(i6));
                if (str == null) {
                    str = "unassigned/0x" + Long.toHexString(i6);
                }
                if (i6 == 0) {
                    if (!decodeUintvarInteger(i + i4)) {
                        return false;
                    }
                    int i7 = i4 + this.mDataLength;
                    this.mContentParameters.put(str, String.valueOf(this.mUnsigned32bit));
                    return readContentParameters(i + i7, i2 - i7, i3 + i7);
                }
            }
            int i8 = i + i4;
            if (decodeNoValue(i8)) {
                i5 = i4 + this.mDataLength;
                strSubstring = null;
            } else if (decodeIntegerValue(i8)) {
                i5 = i4 + this.mDataLength;
                strSubstring = String.valueOf((int) this.mUnsigned32bit);
            } else {
                decodeTokenText(i8);
                i5 = i4 + this.mDataLength;
                String str2 = this.mStringValue;
                if (str2.startsWith("\"")) {
                    strSubstring = str2.substring(1);
                } else {
                    strSubstring = str2;
                }
            }
            this.mContentParameters.put(str, strSubstring);
            return readContentParameters(i + i5, i2 - i5, i3 + i5);
        }
        this.mDataLength = i3;
        return true;
    }

    private boolean decodeNoValue(int i) {
        if (this.mWspData[i] == 0) {
            this.mDataLength = 1;
            return true;
        }
        return false;
    }

    private void expandWellKnownMimeType() {
        if (this.mStringValue == null) {
            this.mStringValue = WELL_KNOWN_MIME_TYPES.get(Integer.valueOf((int) this.mUnsigned32bit));
        } else {
            this.mUnsigned32bit = -1L;
        }
    }

    public boolean decodeContentLength(int i) {
        return decodeIntegerValue(i);
    }

    public boolean decodeContentLocation(int i) {
        return decodeTextString(i);
    }

    public boolean decodeXWapApplicationId(int i) {
        if (decodeIntegerValue(i)) {
            this.mStringValue = null;
            return true;
        }
        return decodeTextString(i);
    }

    public boolean seekXWapApplicationId(int i, int i2) {
        while (i <= i2) {
            try {
                if (decodeIntegerValue(i)) {
                    if (((int) getValue32()) == 47) {
                        this.mUnsigned32bit = i + 1;
                        return true;
                    }
                } else if (!decodeTextString(i)) {
                    return false;
                }
                int decodedDataLength = i + getDecodedDataLength();
                if (decodedDataLength > i2) {
                    return false;
                }
                byte b = this.mWspData[decodedDataLength];
                if (b >= 0 && b <= 30) {
                    i = decodedDataLength + this.mWspData[decodedDataLength] + 1;
                } else if (b == 31) {
                    int i3 = decodedDataLength + 1;
                    if (i3 >= i2 || !decodeUintvarInteger(i3)) {
                        return false;
                    }
                    i = i3 + getDecodedDataLength();
                } else if (31 < b && b <= 127) {
                    if (!decodeTextString(decodedDataLength)) {
                        return false;
                    }
                    i = decodedDataLength + getDecodedDataLength();
                } else {
                    i = decodedDataLength + 1;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return false;
            }
        }
        return false;
    }

    public boolean decodeXWapContentURI(int i) {
        return decodeTextString(i);
    }

    public boolean decodeXWapInitiatorURI(int i) {
        return decodeTextString(i);
    }

    public int getDecodedDataLength() {
        return this.mDataLength;
    }

    public long getValue32() {
        return this.mUnsigned32bit;
    }

    public String getValueString() {
        return this.mStringValue;
    }

    public HashMap<String, String> getContentParameters() {
        return this.mContentParameters;
    }
}
