package javax.obex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Calendar;

public final class HeaderSet {
    public static final int APPLICATION_PARAMETER = 76;
    public static final int AUTH_CHALLENGE = 77;
    public static final int AUTH_RESPONSE = 78;
    public static final int BODY = 72;
    public static final int CONNECTION_ID = 203;
    public static final int COUNT = 192;
    public static final int DESCRIPTION = 5;
    public static final int END_OF_BODY = 73;
    public static final int HTTP = 71;
    public static final int LENGTH = 195;
    public static final int NAME = 1;
    public static final int OBJECT_CLASS = 79;
    public static final int SINGLE_RESPONSE_MODE = 151;
    public static final int SINGLE_RESPONSE_MODE_PARAMETER = 152;
    public static final int TARGET = 70;
    public static final int TIME_4_BYTE = 196;
    public static final int TIME_ISO_8601 = 68;
    public static final int TYPE = 66;
    public static final int WHO = 74;
    private byte[] mAppParam;
    public byte[] mAuthChall;
    public byte[] mAuthResp;
    private Calendar mByteTime;
    public byte[] mConnectionID;
    private Long mCount;
    private String mDescription;
    private boolean mEmptyName;
    private byte[] mHttpHeader;
    private Calendar mIsoTime;
    private Long mLength;
    private String mName;
    private byte[] mObjectClass;
    private Byte mSingleResponseMode;
    private Byte mSrmParam;
    private byte[] mTarget;
    private String mType;
    private byte[] mWho;
    byte[] nonce;
    private SecureRandom mRandom = null;
    private String[] mUnicodeUserDefined = new String[16];
    private byte[][] mSequenceUserDefined = new byte[16][];
    private Byte[] mByteUserDefined = new Byte[16];
    private Long[] mIntegerUserDefined = new Long[16];
    public int responseCode = -1;

    public void setEmptyNameHeader() {
        this.mName = null;
        this.mEmptyName = true;
    }

    public boolean getEmptyNameHeader() {
        return this.mEmptyName;
    }

    public void setHeader(int i, Object obj) {
        switch (i) {
            case 1:
                if (obj != 0 && !(obj instanceof String)) {
                    throw new IllegalArgumentException("Name must be a String");
                }
                this.mEmptyName = false;
                this.mName = (String) obj;
                return;
            case 5:
                if (obj != 0 && !(obj instanceof String)) {
                    throw new IllegalArgumentException("Description must be a String");
                }
                this.mDescription = (String) obj;
                return;
            case TYPE:
                if (obj != 0 && !(obj instanceof String)) {
                    throw new IllegalArgumentException("Type must be a String");
                }
                this.mType = (String) obj;
                return;
            case TIME_ISO_8601:
                if (obj != 0 && !(obj instanceof Calendar)) {
                    throw new IllegalArgumentException("Time ISO 8601 must be a Calendar");
                }
                this.mIsoTime = (Calendar) obj;
                return;
            case TARGET:
                if (obj == 0) {
                    this.mTarget = null;
                    return;
                } else {
                    if (!(obj instanceof byte[])) {
                        throw new IllegalArgumentException("Target must be a byte array");
                    }
                    this.mTarget = new byte[obj.length];
                    System.arraycopy(obj, 0, this.mTarget, 0, this.mTarget.length);
                    return;
                }
            case HTTP:
                if (obj == 0) {
                    this.mHttpHeader = null;
                    return;
                } else {
                    if (!(obj instanceof byte[])) {
                        throw new IllegalArgumentException("HTTP must be a byte array");
                    }
                    this.mHttpHeader = new byte[obj.length];
                    System.arraycopy(obj, 0, this.mHttpHeader, 0, this.mHttpHeader.length);
                    return;
                }
            case WHO:
                if (obj == 0) {
                    this.mWho = null;
                    return;
                } else {
                    if (!(obj instanceof byte[])) {
                        throw new IllegalArgumentException("WHO must be a byte array");
                    }
                    this.mWho = new byte[obj.length];
                    System.arraycopy(obj, 0, this.mWho, 0, this.mWho.length);
                    return;
                }
            case APPLICATION_PARAMETER:
                if (obj == 0) {
                    this.mAppParam = null;
                    return;
                } else {
                    if (!(obj instanceof byte[])) {
                        throw new IllegalArgumentException("Application Parameter must be a byte array");
                    }
                    this.mAppParam = new byte[obj.length];
                    System.arraycopy(obj, 0, this.mAppParam, 0, this.mAppParam.length);
                    return;
                }
            case OBJECT_CLASS:
                if (obj == 0) {
                    this.mObjectClass = null;
                    return;
                } else {
                    if (!(obj instanceof byte[])) {
                        throw new IllegalArgumentException("Object Class must be a byte array");
                    }
                    this.mObjectClass = new byte[obj.length];
                    System.arraycopy(obj, 0, this.mObjectClass, 0, this.mObjectClass.length);
                    return;
                }
            case SINGLE_RESPONSE_MODE:
                if (obj == 0) {
                    this.mSingleResponseMode = null;
                    return;
                } else {
                    if (!(obj instanceof Byte)) {
                        throw new IllegalArgumentException("Single Response Mode must be a Byte");
                    }
                    this.mSingleResponseMode = obj;
                    return;
                }
            case SINGLE_RESPONSE_MODE_PARAMETER:
                if (obj == 0) {
                    this.mSrmParam = null;
                    return;
                } else {
                    if (!(obj instanceof Byte)) {
                        throw new IllegalArgumentException("Single Response Mode Parameter must be a Byte");
                    }
                    this.mSrmParam = obj;
                    return;
                }
            case 192:
                if (!(obj instanceof Long)) {
                    if (obj == 0) {
                        this.mCount = null;
                        return;
                    }
                    throw new IllegalArgumentException("Count must be a Long");
                }
                long jLongValue = obj.longValue();
                if (jLongValue < 0 || jLongValue > 4294967295L) {
                    throw new IllegalArgumentException("Count must be between 0 and 0xFFFFFFFF");
                }
                this.mCount = obj;
                return;
            case 195:
                if (!(obj instanceof Long)) {
                    if (obj == 0) {
                        this.mLength = null;
                        return;
                    }
                    throw new IllegalArgumentException("Length must be a Long");
                }
                long jLongValue2 = obj.longValue();
                if (jLongValue2 < 0 || jLongValue2 > 4294967295L) {
                    throw new IllegalArgumentException("Length must be between 0 and 0xFFFFFFFF");
                }
                this.mLength = obj;
                return;
            case 196:
                if (obj != 0 && !(obj instanceof Calendar)) {
                    throw new IllegalArgumentException("Time 4 Byte must be a Calendar");
                }
                this.mByteTime = (Calendar) obj;
                return;
            default:
                if (i >= 48 && i <= 63) {
                    if (obj == 0 || (obj instanceof String)) {
                        this.mUnicodeUserDefined[i - 48] = (String) obj;
                        return;
                    }
                    throw new IllegalArgumentException("Unicode String User Defined must be a String");
                }
                if (i >= 112 && i <= 127) {
                    if (obj == 0) {
                        this.mSequenceUserDefined[i - 112] = null;
                        return;
                    } else {
                        if (obj instanceof byte[]) {
                            int i2 = i - 112;
                            this.mSequenceUserDefined[i2] = new byte[obj.length];
                            System.arraycopy(obj, 0, this.mSequenceUserDefined[i2], 0, this.mSequenceUserDefined[i2].length);
                            return;
                        }
                        throw new IllegalArgumentException("Byte Sequence User Defined must be a byte array");
                    }
                }
                if (i >= 176 && i <= 191) {
                    if (obj == 0 || (obj instanceof Byte)) {
                        this.mByteUserDefined[i - ResponseCodes.OBEX_HTTP_MULT_CHOICE] = (Byte) obj;
                        return;
                    }
                    throw new IllegalArgumentException("ByteUser Defined must be a Byte");
                }
                if (i >= 240 && i <= 255) {
                    if (!(obj instanceof Long)) {
                        if (obj == 0) {
                            this.mIntegerUserDefined[i - 240] = null;
                            return;
                        }
                        throw new IllegalArgumentException("Integer User Defined must be a Long");
                    }
                    long jLongValue3 = obj.longValue();
                    if (jLongValue3 >= 0 && jLongValue3 <= 4294967295L) {
                        this.mIntegerUserDefined[i - 240] = obj;
                        return;
                    }
                    throw new IllegalArgumentException("Integer User Defined must be between 0 and 0xFFFFFFFF");
                }
                throw new IllegalArgumentException("Invalid Header Identifier");
        }
    }

    public Object getHeader(int i) throws IOException {
        switch (i) {
            case 1:
                return this.mName;
            case 5:
                return this.mDescription;
            case TYPE:
                return this.mType;
            case TIME_ISO_8601:
                return this.mIsoTime;
            case TARGET:
                return this.mTarget;
            case HTTP:
                return this.mHttpHeader;
            case WHO:
                return this.mWho;
            case APPLICATION_PARAMETER:
                return this.mAppParam;
            case OBJECT_CLASS:
                return this.mObjectClass;
            case SINGLE_RESPONSE_MODE:
                return this.mSingleResponseMode;
            case SINGLE_RESPONSE_MODE_PARAMETER:
                return this.mSrmParam;
            case 192:
                return this.mCount;
            case 195:
                return this.mLength;
            case 196:
                return this.mByteTime;
            case 203:
                return this.mConnectionID;
            default:
                if (i >= 48 && i <= 63) {
                    return this.mUnicodeUserDefined[i - 48];
                }
                if (i >= 112 && i <= 127) {
                    return this.mSequenceUserDefined[i - 112];
                }
                if (i >= 176 && i <= 191) {
                    return this.mByteUserDefined[i - ResponseCodes.OBEX_HTTP_MULT_CHOICE];
                }
                if (i >= 240 && i <= 255) {
                    return this.mIntegerUserDefined[i - 240];
                }
                throw new IllegalArgumentException("Invalid Header Identifier");
        }
    }

    public int[] getHeaderList() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if (this.mCount != null) {
            byteArrayOutputStream.write(192);
        }
        if (this.mName != null) {
            byteArrayOutputStream.write(1);
        }
        if (this.mType != null) {
            byteArrayOutputStream.write(66);
        }
        if (this.mLength != null) {
            byteArrayOutputStream.write(195);
        }
        if (this.mIsoTime != null) {
            byteArrayOutputStream.write(68);
        }
        if (this.mByteTime != null) {
            byteArrayOutputStream.write(196);
        }
        if (this.mDescription != null) {
            byteArrayOutputStream.write(5);
        }
        if (this.mTarget != null) {
            byteArrayOutputStream.write(70);
        }
        if (this.mHttpHeader != null) {
            byteArrayOutputStream.write(71);
        }
        if (this.mWho != null) {
            byteArrayOutputStream.write(74);
        }
        if (this.mAppParam != null) {
            byteArrayOutputStream.write(76);
        }
        if (this.mObjectClass != null) {
            byteArrayOutputStream.write(79);
        }
        if (this.mSingleResponseMode != null) {
            byteArrayOutputStream.write(SINGLE_RESPONSE_MODE);
        }
        if (this.mSrmParam != null) {
            byteArrayOutputStream.write(SINGLE_RESPONSE_MODE_PARAMETER);
        }
        for (int i = 48; i < 64; i++) {
            if (this.mUnicodeUserDefined[i - 48] != null) {
                byteArrayOutputStream.write(i);
            }
        }
        for (int i2 = 112; i2 < 128; i2++) {
            if (this.mSequenceUserDefined[i2 - 112] != null) {
                byteArrayOutputStream.write(i2);
            }
        }
        for (int i3 = ResponseCodes.OBEX_HTTP_MULT_CHOICE; i3 < 192; i3++) {
            if (this.mByteUserDefined[i3 - 176] != null) {
                byteArrayOutputStream.write(i3);
            }
        }
        for (int i4 = 240; i4 < 256; i4++) {
            if (this.mIntegerUserDefined[i4 - 240] != null) {
                byteArrayOutputStream.write(i4);
            }
        }
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        if (byteArray == null || byteArray.length == 0) {
            return null;
        }
        int[] iArr = new int[byteArray.length];
        for (int i5 = 0; i5 < byteArray.length; i5++) {
            iArr[i5] = byteArray[i5] & 255;
        }
        return iArr;
    }

    public void createAuthenticationChallenge(String str, boolean z, boolean z2) throws IOException {
        this.nonce = new byte[16];
        if (this.mRandom == null) {
            this.mRandom = new SecureRandom();
        }
        for (int i = 0; i < 16; i++) {
            this.nonce[i] = (byte) this.mRandom.nextInt();
        }
        this.mAuthChall = ObexHelper.computeAuthenticationChallenge(this.nonce, str, z2, z);
    }

    public int getResponseCode() throws IOException {
        if (this.responseCode == -1) {
            throw new IOException("May not be called on a server");
        }
        return this.responseCode;
    }
}
