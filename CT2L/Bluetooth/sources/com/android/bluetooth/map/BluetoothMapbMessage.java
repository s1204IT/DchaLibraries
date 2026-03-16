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

public abstract class BluetoothMapbMessage {
    protected static final boolean D = true;
    protected static final boolean V = false;
    private static final String VERSION = "VERSION:1.0";
    protected static String TAG = "BluetoothMapbMessage";
    public static int INVALID_VALUE = -1;
    protected int mAppParamCharset = -1;
    private String mStatus = null;
    protected BluetoothMapUtils.TYPE mType = null;
    private String mFolder = null;
    private long mPartId = INVALID_VALUE;
    protected String mEncoding = null;
    protected String mCharset = null;
    private String mLanguage = null;
    private int mBMsgLength = INVALID_VALUE;
    private ArrayList<vCard> mOriginator = null;
    private ArrayList<vCard> mRecipient = null;

    public abstract byte[] encode() throws UnsupportedEncodingException;

    public abstract void parseMsgInit();

    public abstract void parseMsgPart(String str);

    public static class vCard {
        private String[] mEmailAddresses;
        private int mEnvLevel;
        private String mFormattedName;
        private String mName;
        private String[] mPhoneNumbers;
        private String mVersion;

        public vCard(String name, String formattedName, String[] phoneNumbers, String[] emailAddresses, int envLevel) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mEnvLevel = envLevel;
            this.mVersion = VCardConstants.VERSION_V30;
            this.mName = name == null ? "" : name;
            this.mFormattedName = formattedName == null ? "" : formattedName;
            setPhoneNumbers(phoneNumbers);
            if (emailAddresses != null) {
                this.mEmailAddresses = emailAddresses;
            }
        }

        public vCard(String name, String[] phoneNumbers, String[] emailAddresses, int envLevel) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mEnvLevel = envLevel;
            this.mVersion = VCardConstants.VERSION_V21;
            this.mName = name == null ? "" : name;
            setPhoneNumbers(phoneNumbers);
            if (emailAddresses != null) {
                this.mEmailAddresses = emailAddresses;
            }
        }

        public vCard(String name, String formattedName, String[] phoneNumbers, String[] emailAddresses) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mVersion = VCardConstants.VERSION_V30;
            this.mName = name == null ? "" : name;
            this.mFormattedName = formattedName == null ? "" : formattedName;
            setPhoneNumbers(phoneNumbers);
            if (emailAddresses != null) {
                this.mEmailAddresses = emailAddresses;
            }
        }

        public vCard(String name, String[] phoneNumbers, String[] emailAddresses) {
            this.mName = null;
            this.mFormattedName = null;
            this.mPhoneNumbers = new String[0];
            this.mEmailAddresses = new String[0];
            this.mEnvLevel = 0;
            this.mVersion = VCardConstants.VERSION_V21;
            this.mName = name == null ? "" : name;
            setPhoneNumbers(phoneNumbers);
            if (emailAddresses != null) {
                this.mEmailAddresses = emailAddresses;
            }
        }

        private void setPhoneNumbers(String[] numbers) {
            if (numbers != null && numbers.length > 0) {
                this.mPhoneNumbers = new String[numbers.length];
                int n = numbers.length;
                for (int i = 0; i < n; i++) {
                    String networkNumber = PhoneNumberUtils.extractNetworkPortion(numbers[i]);
                    Boolean alpha = Boolean.valueOf(PhoneNumberUtils.stripSeparators(numbers[i]).matches("[0-9]*[a-zA-Z]+[0-9]*"));
                    if (networkNumber != null && networkNumber.length() > 1 && !alpha.booleanValue()) {
                        this.mPhoneNumbers[i] = networkNumber;
                    } else {
                        this.mPhoneNumbers[i] = numbers[i];
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

        public void encode(StringBuilder sb) {
            sb.append("BEGIN:VCARD").append(VCardBuilder.VCARD_END_OF_LINE);
            sb.append("VERSION:").append(this.mVersion).append(VCardBuilder.VCARD_END_OF_LINE);
            if (this.mVersion.equals(VCardConstants.VERSION_V30) && this.mFormattedName != null) {
                sb.append("FN:").append(this.mFormattedName).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mName != null) {
                sb.append("N:").append(this.mName).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            String[] arr$ = this.mPhoneNumbers;
            for (String phoneNumber : arr$) {
                sb.append("TEL:").append(phoneNumber).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            String[] arr$2 = this.mEmailAddresses;
            for (String emailAddress : arr$2) {
                sb.append("EMAIL:").append(emailAddress).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            sb.append("END:VCARD").append(VCardBuilder.VCARD_END_OF_LINE);
        }

        public static vCard parseVcard(BMsgReader reader, int envLevel) {
            String formattedName = null;
            String name = null;
            ArrayList<String> phoneNumbers = null;
            ArrayList<String> emailAddresses = null;
            String line = reader.getLineEnforce();
            while (!line.contains("END:VCARD")) {
                String line2 = line.trim();
                if (line2.startsWith("N:")) {
                    String[] parts = line2.split("[^\\\\]:");
                    if (parts.length == 2) {
                        name = parts[1];
                    } else {
                        name = "";
                    }
                } else if (line2.startsWith("FN:")) {
                    String[] parts2 = line2.split("[^\\\\]:");
                    if (parts2.length == 2) {
                        formattedName = parts2[1];
                    } else {
                        formattedName = "";
                    }
                } else if (line2.startsWith("TEL:")) {
                    String[] parts3 = line2.split("[^\\\\]:");
                    if (parts3.length == 2) {
                        String[] subParts = parts3[1].split("[^\\\\];");
                        if (phoneNumbers == null) {
                            phoneNumbers = new ArrayList<>(1);
                        }
                        phoneNumbers.add(subParts[subParts.length - 1]);
                    }
                } else if (line2.startsWith("EMAIL:")) {
                    String[] parts4 = line2.split("[^\\\\]:");
                    if (parts4.length == 2) {
                        String[] subParts2 = parts4[1].split("[^\\\\];");
                        if (emailAddresses == null) {
                            emailAddresses = new ArrayList<>(1);
                        }
                        emailAddresses.add(subParts2[subParts2.length - 1]);
                    }
                }
                line = reader.getLineEnforce();
            }
            return new vCard(name, formattedName, phoneNumbers == null ? null : (String[]) phoneNumbers.toArray(new String[phoneNumbers.size()]), emailAddresses != null ? (String[]) emailAddresses.toArray(new String[emailAddresses.size()]) : null, envLevel);
        }
    }

    private static class BMsgReader {
        InputStream mInStream;

        public BMsgReader(InputStream is) {
            this.mInStream = is;
        }

        private byte[] getLineAsBytes() {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            while (true) {
                try {
                    int readByte = this.mInStream.read();
                    if (readByte == -1) {
                        break;
                    }
                    if (readByte == 13) {
                        readByte = this.mInStream.read();
                        if (readByte != -1 && readByte == 10) {
                            if (output.size() != 0) {
                                break;
                            }
                        } else {
                            output.write(13);
                            output.write(readByte);
                        }
                    } else if (readByte != 10 || output.size() != 0) {
                        output.write(readByte);
                    }
                } catch (IOException e) {
                    Log.w(BluetoothMapbMessage.TAG, e);
                    return null;
                }
            }
            return output.toByteArray();
        }

        public String getLine() {
            try {
                byte[] line = getLineAsBytes();
                if (line.length == 0) {
                    return null;
                }
                return new String(line, "UTF-8");
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

        public void expect(String subString) throws IllegalArgumentException {
            String line = getLine();
            if (line == null || subString == null) {
                throw new IllegalArgumentException("Line or substring is null");
            }
            if (!line.toUpperCase().contains(subString.toUpperCase())) {
                throw new IllegalArgumentException("Expected \"" + subString + "\" in: \"" + line + "\"");
            }
        }

        public void expect(String subString, String subString2) throws IllegalArgumentException {
            String line = getLine();
            if (!line.toUpperCase().contains(subString.toUpperCase())) {
                throw new IllegalArgumentException("Expected \"" + subString + "\" in: \"" + line + "\"");
            }
            if (!line.toUpperCase().contains(subString2.toUpperCase())) {
                throw new IllegalArgumentException("Expected \"" + subString + "\" in: \"" + line + "\"");
            }
        }

        public byte[] getDataBytes(int length) {
            byte[] data = new byte[length];
            int offset = 0;
            while (true) {
                try {
                    int bytesRead = this.mInStream.read(data, offset, length - offset);
                    if (bytesRead != length - offset) {
                        if (bytesRead == -1) {
                            return null;
                        }
                        offset += bytesRead;
                    } else {
                        return data;
                    }
                } catch (IOException e) {
                    Log.w(BluetoothMapbMessage.TAG, e);
                    return null;
                }
            }
        }
    }

    public static BluetoothMapbMessage parse(InputStream bMsgStream, int appParamCharset) throws IllegalArgumentException {
        String[] arg;
        BluetoothMapbMessage newBMsg = null;
        boolean status = V;
        BluetoothMapUtils.TYPE type = null;
        String folder = null;
        BMsgReader reader = new BMsgReader(bMsgStream);
        reader.expect("BEGIN:BMSG");
        reader.expect(VCardConstants.PROPERTY_VERSION, "1.0");
        String line = reader.getLineEnforce();
        while (!line.contains("BEGIN:VCARD") && !line.contains("BEGIN:BENV")) {
            if (line.contains("STATUS")) {
                String[] arg2 = line.split(":");
                if (arg2 != null && arg2.length == 2) {
                    if (arg2[1].trim().equals("READ")) {
                        status = true;
                    } else if (arg2[1].trim().equals("UNREAD")) {
                        status = V;
                    } else {
                        throw new IllegalArgumentException("Wrong value in 'STATUS': " + arg2[1]);
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'STATUS': " + line);
                }
            }
            if (line.contains(VCardConstants.PARAM_TYPE)) {
                String[] arg3 = line.split(":");
                if (arg3 != null && arg3.length == 2) {
                    String value = arg3[1].trim();
                    type = BluetoothMapUtils.TYPE.valueOf(value);
                    if (appParamCharset == 0 && type != BluetoothMapUtils.TYPE.SMS_CDMA && type != BluetoothMapUtils.TYPE.SMS_GSM) {
                        throw new IllegalArgumentException("Native appParamsCharset only supported for SMS");
                    }
                    switch (type) {
                        case SMS_CDMA:
                        case SMS_GSM:
                            newBMsg = new BluetoothMapbMessageSms();
                            break;
                        case MMS:
                            newBMsg = new BluetoothMapbMessageMms();
                            break;
                        case EMAIL:
                            newBMsg = new BluetoothMapbMessageEmail();
                            break;
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'TYPE':" + line);
                }
            }
            if (line.contains("FOLDER") && (arg = line.split(":")) != null && arg.length == 2) {
                folder = arg[1].trim();
            }
            line = reader.getLineEnforce();
        }
        if (newBMsg == null) {
            throw new IllegalArgumentException("Missing bMessage TYPE: - unable to parse body-content");
        }
        newBMsg.setType(type);
        newBMsg.mAppParamCharset = appParamCharset;
        if (folder != null) {
            newBMsg.setCompleteFolder(folder);
        }
        if (0 != 0) {
            newBMsg.setStatus(status);
        }
        while (line.contains("BEGIN:VCARD")) {
            Log.d(TAG, "Decoding vCard");
            newBMsg.addOriginator(vCard.parseVcard(reader, 0));
            line = reader.getLineEnforce();
        }
        if (line.contains("BEGIN:BENV")) {
            newBMsg.parseEnvelope(reader, 0);
            try {
                bMsgStream.close();
            } catch (IOException e) {
            }
            return newBMsg;
        }
        throw new IllegalArgumentException("Bmessage has no BEGIN:BENV - line:" + line);
    }

    private void parseEnvelope(BMsgReader reader, int level) {
        String line = reader.getLineEnforce();
        Log.d(TAG, "Decoding envelope level " + level);
        while (line.contains("BEGIN:VCARD")) {
            Log.d(TAG, "Decoding recipient vCard level " + level);
            if (this.mRecipient == null) {
                this.mRecipient = new ArrayList<>(1);
            }
            this.mRecipient.add(vCard.parseVcard(reader, level));
            line = reader.getLineEnforce();
        }
        if (line.contains("BEGIN:BENV")) {
            Log.d(TAG, "Decoding nested envelope");
            parseEnvelope(reader, level + 1);
        }
        if (line.contains("BEGIN:BBODY")) {
            Log.d(TAG, "Decoding bbody");
            parseBody(reader);
        }
    }

    private void parseBody(BMsgReader reader) {
        String line = reader.getLineEnforce();
        while (!line.contains("END:")) {
            if (line.contains("PARTID:")) {
                String[] arg = line.split(":");
                if (arg != null && arg.length == 2) {
                    try {
                        this.mPartId = Long.parseLong(arg[1].trim());
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Wrong value in 'PARTID': " + arg[1]);
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'PARTID': " + line);
                }
            } else if (line.contains("ENCODING:")) {
                String[] arg2 = line.split(":");
                if (arg2 != null && arg2.length == 2) {
                    this.mEncoding = arg2[1].trim();
                } else {
                    throw new IllegalArgumentException("Missing value for 'ENCODING': " + line);
                }
            } else if (line.contains("CHARSET:")) {
                String[] arg3 = line.split(":");
                if (arg3 != null && arg3.length == 2) {
                    this.mCharset = arg3[1].trim();
                } else {
                    throw new IllegalArgumentException("Missing value for 'CHARSET': " + line);
                }
            } else if (line.contains("LANGUAGE:")) {
                String[] arg4 = line.split(":");
                if (arg4 != null && arg4.length == 2) {
                    this.mLanguage = arg4[1].trim();
                } else {
                    throw new IllegalArgumentException("Missing value for 'LANGUAGE': " + line);
                }
            } else if (line.contains("LENGTH:")) {
                String[] arg5 = line.split(":");
                if (arg5 != null && arg5.length == 2) {
                    try {
                        this.mBMsgLength = Integer.parseInt(arg5[1].trim());
                    } catch (NumberFormatException e2) {
                        throw new IllegalArgumentException("Wrong value in 'LENGTH': " + arg5[1]);
                    }
                } else {
                    throw new IllegalArgumentException("Missing value for 'LENGTH': " + line);
                }
            } else if (!line.contains("BEGIN:MSG")) {
                continue;
            } else {
                if (this.mBMsgLength == INVALID_VALUE) {
                    throw new IllegalArgumentException("Missing value for 'LENGTH'. Unable to read remaining part of the message");
                }
                byte[] rawData = reader.getDataBytes(this.mBMsgLength - (line.getBytes().length + 2));
                try {
                    String data = new String(rawData, "UTF-8");
                    String[] messages = data.split("\r\nEND:MSG\r\n");
                    parseMsgInit();
                    for (int i = 0; i < messages.length; i++) {
                        messages[i] = messages[i].replaceFirst("^BEGIN:MSG\r\n", "");
                        messages[i] = messages[i].replaceAll("\r\n([/]*)/END\\:MSG", "\r\n$1END:MSG");
                        messages[i] = messages[i].trim();
                        parseMsgPart(messages[i]);
                    }
                } catch (UnsupportedEncodingException e3) {
                    Log.w(TAG, e3);
                    throw new IllegalArgumentException("Unable to convert to UTF-8");
                }
            }
            line = reader.getLineEnforce();
        }
    }

    public void setStatus(boolean read) {
        if (read) {
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

    public void setCompleteFolder(String folder) {
        this.mFolder = folder;
    }

    public void setFolder(String folder) {
        this.mFolder = "telecom/msg/" + folder;
    }

    public String getFolder() {
        return this.mFolder;
    }

    public void setEncoding(String encoding) {
        this.mEncoding = encoding;
    }

    public ArrayList<vCard> getOriginators() {
        return this.mOriginator;
    }

    public void addOriginator(vCard originator) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(originator);
    }

    public void addOriginator(String name, String formattedName, String[] phoneNumbers, String[] emailAddresses) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(new vCard(name, formattedName, phoneNumbers, emailAddresses));
    }

    public void addOriginator(String name, String[] phoneNumbers, String[] emailAddresses) {
        if (this.mOriginator == null) {
            this.mOriginator = new ArrayList<>();
        }
        this.mOriginator.add(new vCard(name, phoneNumbers, emailAddresses));
    }

    public ArrayList<vCard> getRecipients() {
        return this.mRecipient;
    }

    public void setRecipient(vCard recipient) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(recipient);
    }

    public void addRecipient(String name, String formattedName, String[] phoneNumbers, String[] emailAddresses) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(new vCard(name, formattedName, phoneNumbers, emailAddresses));
    }

    public void addRecipient(String name, String[] phoneNumbers, String[] emailAddresses) {
        if (this.mRecipient == null) {
            this.mRecipient = new ArrayList<>();
        }
        this.mRecipient.add(new vCard(name, phoneNumbers, emailAddresses));
    }

    protected String encodeBinary(byte[] pduData, byte[] scAddressData) {
        StringBuilder out = new StringBuilder((pduData.length + scAddressData.length) * 2);
        for (int i = 0; i < scAddressData.length; i++) {
            out.append(Integer.toString((scAddressData[i] >> 4) & 15, 16));
            out.append(Integer.toString(scAddressData[i] & 15, 16));
        }
        for (int i2 = 0; i2 < pduData.length; i2++) {
            out.append(Integer.toString((pduData[i2] >> 4) & 15, 16));
            out.append(Integer.toString(pduData[i2] & 15, 16));
        }
        return out.toString();
    }

    protected byte[] decodeBinary(String data) {
        byte[] out = new byte[data.length() / 2];
        Log.d(TAG, "Decoding binary data: START:" + data + ":END");
        int i = 0;
        int n = out.length;
        int j = 0;
        while (i < n) {
            int j2 = j + 1 + 1;
            String value = data.substring(j, j2);
            out[i] = (byte) (Integer.valueOf(value, 16).intValue() & 255);
            i++;
            j = j2;
        }
        StringBuilder sb = new StringBuilder(out.length);
        for (byte b : out) {
            sb.append(String.format("%02X", Integer.valueOf(b & 255)));
        }
        Log.d(TAG, "Decoded binary data: START:" + sb.toString() + ":END");
        return out;
    }

    public byte[] encodeGeneric(ArrayList<byte[]> bodyFragments) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(256);
        sb.append("BEGIN:BMSG").append(VCardBuilder.VCARD_END_OF_LINE);
        sb.append(VERSION).append(VCardBuilder.VCARD_END_OF_LINE);
        sb.append("STATUS:").append(this.mStatus).append(VCardBuilder.VCARD_END_OF_LINE);
        sb.append("TYPE:").append(this.mType.name()).append(VCardBuilder.VCARD_END_OF_LINE);
        if (this.mFolder.length() > 512) {
            sb.append("FOLDER:").append(this.mFolder.substring(this.mFolder.length() - 512, this.mFolder.length())).append(VCardBuilder.VCARD_END_OF_LINE);
        } else {
            sb.append("FOLDER:").append(this.mFolder).append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.mOriginator != null) {
            for (vCard element : this.mOriginator) {
                element.encode(sb);
            }
        }
        sb.append("BEGIN:BENV").append(VCardBuilder.VCARD_END_OF_LINE);
        if (this.mRecipient != null) {
            for (vCard element2 : this.mRecipient) {
                element2.encode(sb);
            }
        }
        sb.append("BEGIN:BBODY").append(VCardBuilder.VCARD_END_OF_LINE);
        if (this.mEncoding != null && this.mEncoding != "") {
            sb.append("ENCODING:").append(this.mEncoding).append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.mCharset != null && this.mCharset != "") {
            sb.append("CHARSET:").append(this.mCharset).append(VCardBuilder.VCARD_END_OF_LINE);
        }
        int length = 0;
        for (byte[] fragment : bodyFragments) {
            length += fragment.length + 22;
        }
        sb.append("LENGTH:").append(length).append(VCardBuilder.VCARD_END_OF_LINE);
        byte[] msgStart = sb.toString().getBytes("UTF-8");
        StringBuilder sb2 = new StringBuilder(31);
        sb2.append("END:BBODY").append(VCardBuilder.VCARD_END_OF_LINE);
        sb2.append("END:BENV").append(VCardBuilder.VCARD_END_OF_LINE);
        sb2.append("END:BMSG").append(VCardBuilder.VCARD_END_OF_LINE);
        byte[] msgEnd = sb2.toString().getBytes("UTF-8");
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream(msgStart.length + msgEnd.length + length);
            stream.write(msgStart);
            for (byte[] fragment2 : bodyFragments) {
                stream.write("BEGIN:MSG\r\n".getBytes("UTF-8"));
                stream.write(fragment2);
                stream.write("\r\nEND:MSG\r\n".getBytes("UTF-8"));
            }
            stream.write(msgEnd);
            return stream.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }
    }
}
