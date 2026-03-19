package com.android.bluetooth.map;

import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class BluetoothMapbMessage {
    protected static final boolean D = BluetoothMapService.DEBUG;
    public static final int INVALID_VALUE = -1;
    protected static final String TAG = "BluetoothMapbMessage";
    protected static final boolean V = false;
    private String mVersionString = "VERSION:1.0";
    protected int mAppParamCharset = -1;
    private String mStatus = null;
    protected BluetoothMapUtils.TYPE mType = null;
    private String mFolder = null;
    private long mPartId = -1;
    protected String mEncoding = null;
    protected String mCharset = null;
    private String mLanguage = null;
    private int mBMsgLength = -1;
    private ArrayList<VCard> mOriginator = null;
    private ArrayList<VCard> mRecipient = null;

    public abstract byte[] encode() throws UnsupportedEncodingException;

    public abstract void parseMsgInit();

    public abstract void parseMsgPart(String str);

    public static class VCard {
        private String[] mBtUcis;
        private String[] mBtUids;
        private String[] mEmailAddresses;
        private int mEnvLevel;
        private String mFormattedName;
        private String mName;
        private String[] mPhoneNumbers;
        private String mVersion;

        public String getFormatName() {
            return this.mFormattedName;
        }

        public VCard(String str, String str2, String[] strArr, String[] strArr2, int i) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mBtUcis = new String[0];
            this.mBtUids = new String[0];
            this.mEnvLevel = i;
            this.mVersion = VCardConstants.VERSION_V30;
            this.mName = str == null ? "" : str;
            this.mFormattedName = str2 == null ? "" : str2;
            setPhoneNumbers(strArr);
            if (strArr2 != null) {
                this.mEmailAddresses = strArr2;
            }
        }

        public VCard(String str, String[] strArr, String[] strArr2, int i) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mBtUcis = new String[0];
            this.mBtUids = new String[0];
            this.mEnvLevel = i;
            this.mVersion = VCardConstants.VERSION_V21;
            this.mName = str == null ? "" : str;
            setPhoneNumbers(strArr);
            if (strArr2 != null) {
                this.mEmailAddresses = strArr2;
            }
        }

        public VCard(String str, String str2, String[] strArr, String[] strArr2, String[] strArr3, String[] strArr4) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mBtUcis = new String[0];
            this.mBtUids = new String[0];
            this.mVersion = VCardConstants.VERSION_V30;
            this.mName = str == null ? "" : str;
            this.mFormattedName = str2 == null ? "" : str2;
            setPhoneNumbers(strArr);
            if (strArr2 != null) {
                this.mEmailAddresses = strArr2;
            }
            if (strArr4 != null) {
                this.mBtUcis = strArr4;
            }
        }

        public VCard(String str, String[] strArr, String[] strArr2) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mBtUcis = new String[0];
            this.mBtUids = new String[0];
            this.mVersion = VCardConstants.VERSION_V21;
            this.mName = str == null ? "" : str;
            setPhoneNumbers(strArr);
            if (strArr2 != null) {
                this.mEmailAddresses = strArr2;
            }
        }

        private void setPhoneNumbers(String[] strArr) {
            if (strArr != null && strArr.length > 0) {
                this.mPhoneNumbers = new String[strArr.length];
                int length = strArr.length;
                for (int i = 0; i < length; i++) {
                    String strExtractNetworkPortion = PhoneNumberUtils.extractNetworkPortion(strArr[i]);
                    String strStripSeparators = PhoneNumberUtils.stripSeparators(strArr[i]);
                    Boolean boolValueOf = false;
                    if (strStripSeparators != null) {
                        boolValueOf = Boolean.valueOf(strStripSeparators.matches("[0-9]*[a-zA-Z]+[0-9]*"));
                    }
                    if (strExtractNetworkPortion != null && strExtractNetworkPortion.length() > 1 && !boolValueOf.booleanValue()) {
                        this.mPhoneNumbers[i] = strExtractNetworkPortion;
                    } else {
                        this.mPhoneNumbers[i] = strArr[i];
                    }
                }
            }
        }

        public String getFirstPhoneNumber() {
            if (this.mPhoneNumbers.length > 0) {
                return this.mPhoneNumbers[0];
            }
            return null;
        }

        public String[] getPhoneNumber() {
            return this.mPhoneNumbers;
        }

        public int getEnvLevel() {
            return this.mEnvLevel;
        }

        public String getName() {
            return this.mName;
        }

        public String getFirstEmail() {
            if (this.mEmailAddresses.length > 0) {
                return this.mEmailAddresses[0];
            }
            return null;
        }

        public String[] getEmail() {
            return this.mEmailAddresses;
        }

        public String getFirstBtUci() {
            if (this.mBtUcis.length > 0) {
                return this.mBtUcis[0];
            }
            return null;
        }

        public String getFirstBtUid() {
            if (this.mBtUids.length > 0) {
                return this.mBtUids[0];
            }
            return null;
        }

        public void encode(StringBuilder sb) {
            sb.append("BEGIN:VCARD");
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
            sb.append("VERSION:");
            sb.append(this.mVersion);
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
            if (this.mVersion.equals(VCardConstants.VERSION_V30) && this.mFormattedName != null) {
                sb.append("FN:");
                sb.append(this.mFormattedName);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mName != null) {
                sb.append("N:");
                sb.append(this.mName);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            for (String str : this.mPhoneNumbers) {
                sb.append("TEL:");
                sb.append(str);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            for (String str2 : this.mEmailAddresses) {
                sb.append("EMAIL:");
                sb.append(str2);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            for (String str3 : this.mBtUids) {
                sb.append("X-BT-UID:");
                sb.append(str3);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            for (String str4 : this.mBtUcis) {
                sb.append("X-BT-UCI:");
                sb.append(str4);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            sb.append("END:VCARD");
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }

        public static VCard parseVcard(BMsgReader bMsgReader, int i) {
            String[] strArr;
            String str;
            String str2;
            String lineEnforce = bMsgReader.getLineEnforce();
            ArrayList arrayList = null;
            String str3 = null;
            String str4 = null;
            ArrayList arrayList2 = null;
            ArrayList arrayList3 = null;
            ArrayList arrayList4 = null;
            while (!lineEnforce.contains("END:VCARD")) {
                String strTrim = lineEnforce.trim();
                if (strTrim.startsWith("N:")) {
                    String[] strArrSplit = strTrim.split("[^\\\\]:");
                    if (strArrSplit.length == 2) {
                        str2 = strArrSplit[1];
                    } else {
                        str2 = "";
                    }
                    str3 = str2;
                } else if (strTrim.startsWith("FN:")) {
                    String[] strArrSplit2 = strTrim.split("[^\\\\]:");
                    if (strArrSplit2.length == 2) {
                        str = strArrSplit2[1];
                    } else {
                        str = "";
                    }
                    str4 = str;
                } else if (strTrim.startsWith("TEL:")) {
                    String[] strArrSplit3 = strTrim.split("[^\\\\]:");
                    if (strArrSplit3.length == 2) {
                        String[] strArrSplit4 = strArrSplit3[1].split("[^\\\\];");
                        if (arrayList == null) {
                            arrayList = new ArrayList(1);
                        }
                        arrayList.add(strArrSplit4[strArrSplit4.length - 1]);
                    }
                } else if (strTrim.startsWith("EMAIL:")) {
                    String[] strArrSplit5 = strTrim.split("[^\\\\]:");
                    if (strArrSplit5.length == 2) {
                        String[] strArrSplit6 = strArrSplit5[1].split("[^\\\\];");
                        if (arrayList2 == null) {
                            arrayList2 = new ArrayList(1);
                        }
                        arrayList2.add(strArrSplit6[strArrSplit6.length - 1]);
                    }
                } else if (strTrim.startsWith("X-BT-UCI:")) {
                    String[] strArrSplit7 = strTrim.split("[^\\\\]:");
                    if (strArrSplit7.length == 2) {
                        String[] strArrSplit8 = strArrSplit7[1].split("[^\\\\];");
                        if (arrayList3 == null) {
                            arrayList3 = new ArrayList(1);
                        }
                        arrayList3.add(strArrSplit8[strArrSplit8.length - 1]);
                    }
                } else if (strTrim.startsWith("X-BT-UID:")) {
                    String[] strArrSplit9 = strTrim.split("[^\\\\]:");
                    if (strArrSplit9.length == 2) {
                        String[] strArrSplit10 = strArrSplit9[1].split("[^\\\\];");
                        if (arrayList4 == null) {
                            arrayList4 = new ArrayList(1);
                        }
                        arrayList4.add(strArrSplit10[strArrSplit10.length - 1]);
                    }
                }
                lineEnforce = bMsgReader.getLineEnforce();
            }
            if (arrayList != null) {
                strArr = (String[]) arrayList.toArray(new String[arrayList.size()]);
            } else {
                strArr = null;
            }
            return new VCard(str3, str4, strArr, arrayList2 != null ? (String[]) arrayList2.toArray(new String[arrayList2.size()]) : null, i);
        }
    }

    private static class BMsgReader {
        InputStream mInStream;

        BMsgReader(InputStream inputStream) {
            this.mInStream = inputStream;
        }

        private byte[] getLineAsBytes() {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (true) {
                try {
                    int i = this.mInStream.read();
                    if (i == -1) {
                        break;
                    }
                    if (i == 13) {
                        i = this.mInStream.read();
                        if (i != -1 && i == 10) {
                            if (byteArrayOutputStream.size() != 0) {
                                break;
                            }
                        } else {
                            byteArrayOutputStream.write(13);
                            byteArrayOutputStream.write(i);
                        }
                    } else if (i != 10 || byteArrayOutputStream.size() != 0) {
                        byteArrayOutputStream.write(i);
                    }
                } catch (IOException e) {
                    Log.w(BluetoothMapbMessage.TAG, e);
                    return null;
                }
            }
            return byteArrayOutputStream.toByteArray();
        }

        public String getLine() {
            try {
                byte[] lineAsBytes = getLineAsBytes();
                if (lineAsBytes.length == 0) {
                    return null;
                }
                return new String(lineAsBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.w(BluetoothMapbMessage.TAG, e);
                return null;
            }
        }

        public String getLineEnforce() {
            String line = getLine();
            if (line == null) {
                throw new IllegalArgumentException("Bmessage too short");
            }
            return line;
        }

        public void expect(String str) throws IllegalArgumentException {
            String line = getLine();
            if (line == null || str == null) {
                throw new IllegalArgumentException("Line or substring is null");
            }
            if (!line.toUpperCase().contains(str.toUpperCase())) {
                throw new IllegalArgumentException("Expected \"" + str + "\" in: \"" + line + "\"");
            }
        }

        public void expect(String str, String str2) throws IllegalArgumentException {
            String line = getLine();
            if (!line.toUpperCase().contains(str.toUpperCase())) {
                throw new IllegalArgumentException("Expected \"" + str + "\" in: \"" + line + "\"");
            }
            if (!line.toUpperCase().contains(str2.toUpperCase())) {
                throw new IllegalArgumentException("Expected \"" + str + "\" in: \"" + line + "\"");
            }
        }

        public byte[] getDataBytes(int i) {
            byte[] bArr = new byte[i];
            int i2 = 0;
            while (true) {
                try {
                    int i3 = i - i2;
                    int i4 = this.mInStream.read(bArr, i2, i3);
                    if (i4 != i3) {
                        if (i4 == -1) {
                            return null;
                        }
                        i2 += i4;
                    } else {
                        return bArr;
                    }
                } catch (IOException e) {
                    Log.w(BluetoothMapbMessage.TAG, e);
                    return null;
                }
            }
        }
    }

    public String getVersionString() {
        return this.mVersionString;
    }

    public void setVersionString(String str) {
        this.mVersionString = "VERSION:" + str;
    }

    public static BluetoothMapbMessage parse(InputStream inputStream, int i) throws IllegalArgumentException {
        String[] strArrSplit;
        String[] strArrSplit2;
        BMsgReader bMsgReader = new BMsgReader(inputStream);
        bMsgReader.expect("BEGIN:BMSG");
        bMsgReader.expect(VCardConstants.PROPERTY_VERSION);
        String lineEnforce = bMsgReader.getLineEnforce();
        BluetoothMapbMessage bluetoothMapbMessageSms = null;
        BluetoothMapUtils.TYPE typeValueOf = null;
        String strTrim = null;
        while (!lineEnforce.contains("BEGIN:VCARD") && !lineEnforce.contains("BEGIN:BENV")) {
            if (lineEnforce.contains("STATUS")) {
                String[] strArrSplit3 = lineEnforce.split(":");
                if (strArrSplit3 != null && strArrSplit3.length == 2) {
                    if (!strArrSplit3[1].trim().equals("READ") && !strArrSplit3[1].trim().equals("UNREAD")) {
                        throw new IllegalArgumentException("Wrong value in 'STATUS': " + strArrSplit3[1]);
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'STATUS': " + lineEnforce);
                }
            }
            if (lineEnforce.contains("EXTENDEDDATA") && (strArrSplit2 = lineEnforce.split(":")) != null && strArrSplit2.length == 2) {
                Log.i(TAG, "We got extended data with: " + strArrSplit2[1].trim());
            }
            if (lineEnforce.contains(VCardConstants.PARAM_TYPE)) {
                String[] strArrSplit4 = lineEnforce.split(":");
                if (strArrSplit4 != null && strArrSplit4.length == 2) {
                    typeValueOf = BluetoothMapUtils.TYPE.valueOf(strArrSplit4[1].trim());
                    if (i == 0 && typeValueOf != BluetoothMapUtils.TYPE.SMS_CDMA && typeValueOf != BluetoothMapUtils.TYPE.SMS_GSM) {
                        throw new IllegalArgumentException("Native appParamsCharset only supported for SMS");
                    }
                    switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[typeValueOf.ordinal()]) {
                        case 1:
                        case 2:
                            bluetoothMapbMessageSms = new BluetoothMapbMessageSms();
                            break;
                        case 3:
                            bluetoothMapbMessageSms = new BluetoothMapbMessageMime();
                            break;
                        case 4:
                            bluetoothMapbMessageSms = new BluetoothMapbMessageEmail();
                            break;
                        case 5:
                            bluetoothMapbMessageSms = new BluetoothMapbMessageMime();
                            break;
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'TYPE':" + lineEnforce);
                }
            }
            if (lineEnforce.contains("FOLDER") && (strArrSplit = lineEnforce.split(":")) != null && strArrSplit.length == 2) {
                strTrim = strArrSplit[1].trim();
            }
            lineEnforce = bMsgReader.getLineEnforce();
        }
        if (bluetoothMapbMessageSms == null) {
            throw new IllegalArgumentException("Missing bMessage TYPE: - unable to parse body-content");
        }
        bluetoothMapbMessageSms.setType(typeValueOf);
        bluetoothMapbMessageSms.mAppParamCharset = i;
        if (strTrim != null) {
            bluetoothMapbMessageSms.setCompleteFolder(strTrim);
        }
        while (lineEnforce.contains("BEGIN:VCARD")) {
            if (D) {
                Log.d(TAG, "Decoding vCard");
            }
            bluetoothMapbMessageSms.addOriginator(VCard.parseVcard(bMsgReader, 0));
            lineEnforce = bMsgReader.getLineEnforce();
        }
        if (lineEnforce.contains("BEGIN:BENV")) {
            bluetoothMapbMessageSms.parseEnvelope(bMsgReader, 0);
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            return bluetoothMapbMessageSms;
        }
        throw new IllegalArgumentException("Bmessage has no BEGIN:BENV - line:" + lineEnforce);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE = new int[BluetoothMapUtils.TYPE.values().length];

        static {
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.SMS_CDMA.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.SMS_GSM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.MMS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.EMAIL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[BluetoothMapUtils.TYPE.IM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private void parseEnvelope(BMsgReader bMsgReader, int i) {
        String lineEnforce = bMsgReader.getLineEnforce();
        if (D) {
            Log.d(TAG, "Decoding envelope level " + i);
        }
        while (lineEnforce.contains("BEGIN:VCARD")) {
            if (D) {
                Log.d(TAG, "Decoding recipient vCard level " + i);
            }
            if (this.mRecipient == null) {
                this.mRecipient = new ArrayList<>(1);
            }
            this.mRecipient.add(VCard.parseVcard(bMsgReader, i));
            lineEnforce = bMsgReader.getLineEnforce();
        }
        if (lineEnforce.contains("BEGIN:BENV")) {
            if (D) {
                Log.d(TAG, "Decoding nested envelope");
            }
            parseEnvelope(bMsgReader, i + 1);
        }
        if (lineEnforce.contains("BEGIN:BBODY")) {
            if (D) {
                Log.d(TAG, "Decoding bbody");
            }
            parseBody(bMsgReader);
        }
    }

    private void parseBody(BMsgReader bMsgReader) {
        String lineEnforce = bMsgReader.getLineEnforce();
        parseMsgInit();
        while (!lineEnforce.contains("END:")) {
            if (lineEnforce.contains("PARTID:")) {
                String[] strArrSplit = lineEnforce.split(":");
                if (strArrSplit != null && strArrSplit.length == 2) {
                    try {
                        this.mPartId = Long.parseLong(strArrSplit[1].trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Wrong value in 'PARTID': " + strArrSplit[1]);
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'PARTID': " + lineEnforce);
                }
            } else if (lineEnforce.contains("ENCODING:")) {
                String[] strArrSplit2 = lineEnforce.split(":");
                if (strArrSplit2 != null && strArrSplit2.length == 2) {
                    this.mEncoding = strArrSplit2[1].trim();
                } else {
                    throw new IllegalArgumentException("Missing value for 'ENCODING': " + lineEnforce);
                }
            } else if (lineEnforce.contains("CHARSET:")) {
                String[] strArrSplit3 = lineEnforce.split(":");
                if (strArrSplit3 != null && strArrSplit3.length == 2) {
                    this.mCharset = strArrSplit3[1].trim();
                } else {
                    throw new IllegalArgumentException("Missing value for 'CHARSET': " + lineEnforce);
                }
            } else if (lineEnforce.contains("LANGUAGE:")) {
                String[] strArrSplit4 = lineEnforce.split(":");
                if (strArrSplit4 != null && strArrSplit4.length == 2) {
                    this.mLanguage = strArrSplit4[1].trim();
                } else {
                    Log.i(TAG, "LANGUAGE value is missing.");
                    this.mLanguage = "";
                }
            } else if (lineEnforce.contains("LENGTH:")) {
                String[] strArrSplit5 = lineEnforce.split(":");
                if (strArrSplit5 != null && strArrSplit5.length == 2) {
                    try {
                        this.mBMsgLength = Integer.parseInt(strArrSplit5[1].trim());
                    } catch (NumberFormatException e2) {
                        throw new IllegalArgumentException("Wrong value in 'LENGTH': " + strArrSplit5[1]);
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'LENGTH': " + lineEnforce);
                }
            } else if (!lineEnforce.contains("BEGIN:MSG")) {
                continue;
            } else {
                if (this.mBMsgLength == -1) {
                    throw new IllegalArgumentException("Missing value for 'LENGTH'. Unable to read remaining part of the message");
                }
                String str = "";
                String lineEnforce2 = "";
                while (!lineEnforce2.equals("END:MSG")) {
                    str = str + lineEnforce2;
                    lineEnforce2 = bMsgReader.getLineEnforce();
                }
                str.replaceAll("([/]*)/END\\:MSG", "$1END:MSG");
                str.trim();
                parseMsgPart(str);
            }
            lineEnforce = bMsgReader.getLineEnforce();
        }
    }

    public void setStatus(boolean z) {
        if (z) {
            this.mStatus = "READ";
        } else {
            this.mStatus = "UNREAD";
        }
    }

    public void setType(BluetoothMapUtils.TYPE type) {
        this.mType = type;
    }

    public BluetoothMapUtils.TYPE getType() {
        return this.mType;
    }

    public void setCompleteFolder(String str) {
        this.mFolder = str;
    }

    public void setFolder(String str) {
        this.mFolder = "telecom/msg/" + str;
    }

    public String getFolder() {
        return this.mFolder;
    }

    public void setEncoding(String str) {
        this.mEncoding = str;
    }

    public ArrayList<VCard> getOriginators() {
        return this.mOriginator;
    }

    public void addOriginator(VCard vCard) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(vCard);
    }

    public void addOriginator(String str, String str2, String[] strArr, String[] strArr2, String[] strArr3, String[] strArr4) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(new VCard(str, str2, strArr, strArr2, strArr3, strArr4));
    }

    public void addOriginator(String[] strArr, String[] strArr2) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(new VCard(null, null, null, null, strArr2, strArr));
    }

    public void addOriginator(String str, String[] strArr, String[] strArr2) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(new VCard(str, strArr, strArr2));
    }

    public ArrayList<VCard> getRecipients() {
        return this.mRecipient;
    }

    public void setRecipient(VCard vCard) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(vCard);
    }

    public void addRecipient(String[] strArr, String[] strArr2) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(new VCard(null, null, null, null, strArr2, strArr));
    }

    public void addRecipient(String str, String str2, String[] strArr, String[] strArr2, String[] strArr3, String[] strArr4) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(new VCard(str, str2, strArr, strArr2, strArr3, strArr4));
    }

    public void addRecipient(String str, String[] strArr, String[] strArr2) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(new VCard(str, strArr, strArr2));
    }

    protected String encodeBinary(byte[] bArr, byte[] bArr2) {
        StringBuilder sb = new StringBuilder((bArr.length + bArr2.length) * 2);
        for (int i = 0; i < bArr2.length; i++) {
            sb.append(Integer.toString((bArr2[i] >> 4) & 15, 16));
            sb.append(Integer.toString(bArr2[i] & 15, 16));
        }
        for (int i2 = 0; i2 < bArr.length; i2++) {
            sb.append(Integer.toString((bArr[i2] >> 4) & 15, 16));
            sb.append(Integer.toString(bArr[i2] & 15, 16));
        }
        return sb.toString();
    }

    protected byte[] decodeBinary(String str) {
        byte[] bArr = new byte[str.length() / 2];
        if (D) {
            Log.d(TAG, "Decoding binary data: START:" + str + ":END");
        }
        int length = bArr.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            int i3 = 1 + i2 + 1;
            bArr[i] = (byte) (Integer.valueOf(str.substring(i2, i3), 16).intValue() & 255);
            i++;
            i2 = i3;
        }
        if (D) {
            StringBuilder sb = new StringBuilder(bArr.length);
            for (byte b : bArr) {
                sb.append(String.format("%02X", Integer.valueOf(b & 255)));
            }
            Log.d(TAG, "Decoded binary data: START:" + sb.toString() + ":END");
        }
        return bArr;
    }

    public byte[] encodeGeneric(ArrayList<byte[]> arrayList) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(256);
        sb.append("BEGIN:BMSG");
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        sb.append(this.mVersionString);
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        sb.append("STATUS:");
        sb.append(this.mStatus);
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        sb.append("TYPE:");
        sb.append(this.mType.name());
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        if (this.mFolder.length() > 512) {
            sb.append("FOLDER:");
            sb.append(this.mFolder.substring(this.mFolder.length() - 512, this.mFolder.length()));
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        } else {
            sb.append("FOLDER:");
            sb.append(this.mFolder);
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (!this.mVersionString.contains("1.0")) {
            sb.append("EXTENDEDDATA:");
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.mOriginator != null) {
            Iterator<VCard> it = this.mOriginator.iterator();
            while (it.hasNext()) {
                it.next().encode(sb);
            }
        }
        sb.append("BEGIN:BENV");
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        if (this.mRecipient != null) {
            Iterator<VCard> it2 = this.mRecipient.iterator();
            while (it2.hasNext()) {
                it2.next().encode(sb);
            }
        }
        sb.append("BEGIN:BBODY");
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        if (this.mEncoding != null && !this.mEncoding.isEmpty()) {
            sb.append("ENCODING:");
            sb.append(this.mEncoding);
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.mCharset != null && !this.mCharset.isEmpty()) {
            sb.append("CHARSET:");
            sb.append(this.mCharset);
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }
        int length = 0;
        Iterator<byte[]> it3 = arrayList.iterator();
        while (it3.hasNext()) {
            length += it3.next().length + 22;
        }
        sb.append("LENGTH:");
        sb.append(length);
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
        byte[] bytes = sb.toString().getBytes("UTF-8");
        StringBuilder sb2 = new StringBuilder(31);
        sb2.append("END:BBODY");
        sb2.append(VCardBuilder.VCARD_END_OF_LINE);
        sb2.append("END:BENV");
        sb2.append(VCardBuilder.VCARD_END_OF_LINE);
        sb2.append("END:BMSG");
        sb2.append(VCardBuilder.VCARD_END_OF_LINE);
        byte[] bytes2 = sb2.toString().getBytes("UTF-8");
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(bytes.length + bytes2.length + length);
            byteArrayOutputStream.write(bytes);
            for (byte[] bArr : arrayList) {
                byteArrayOutputStream.write("BEGIN:MSG\r\n".getBytes("UTF-8"));
                byteArrayOutputStream.write(bArr);
                byteArrayOutputStream.write("\r\nEND:MSG\r\n".getBytes("UTF-8"));
            }
            byteArrayOutputStream.write(bytes2);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public String getSingleRecipient() {
        ArrayList<VCard> recipients = getRecipients();
        if (recipients == null || recipients.size() == 0) {
            Log.d(TAG, "[getSingleRecipient] recipientList == null");
            return "";
        }
        for (VCard vCard : recipients) {
            if (vCard.getEnvLevel() == 0) {
                return vCard.getFirstPhoneNumber();
            }
        }
        return "";
    }
}
