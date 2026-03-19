package com.android.bluetooth.map;

import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Base64;
import android.util.Log;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConstants;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

public class BluetoothMapbMessageMime extends BluetoothMapbMessage {
    private boolean mIncludeAttachments;
    private long mDate = -1;
    private String mSubject = null;
    private ArrayList<Rfc822Token> mFrom = null;
    private ArrayList<Rfc822Token> mSender = null;
    private ArrayList<Rfc822Token> mTo = null;
    private ArrayList<Rfc822Token> mCc = null;
    private ArrayList<Rfc822Token> mBcc = null;
    private ArrayList<Rfc822Token> mReplyTo = null;
    private String mMessageId = null;
    private ArrayList<MimePart> mParts = null;
    private String mContentType = null;
    private String mBoundary = null;
    private boolean mTextonly = false;
    private boolean mHasHeaders = false;
    private String mMyEncoding = null;

    public static class MimePart {
        public long mId = -1;
        public String mContentType = null;
        public String mContentId = null;
        public String mContentLocation = null;
        public String mContentDisposition = null;
        public String mPartName = null;
        public String mCharsetName = null;
        public String mFileName = null;
        public byte[] mData = null;

        String getDataAsString() {
            String upperCase;
            String str = this.mCharsetName;
            if (str == null) {
                upperCase = "UTF-8";
            } else {
                upperCase = str.toUpperCase();
                try {
                    if (!Charset.isSupported(upperCase)) {
                        upperCase = "UTF-8";
                    }
                } catch (IllegalCharsetNameException e) {
                    Log.w("BluetoothMapbMessage", "Received unknown charset: " + upperCase + " - using UTF-8.");
                    upperCase = "UTF-8";
                }
            }
            try {
                return new String(this.mData, upperCase);
            } catch (UnsupportedEncodingException e2) {
                try {
                    return new String(this.mData, "UTF-8");
                } catch (UnsupportedEncodingException e3) {
                    Log.e("BluetoothMapbMessage", "getDataAsString: " + e2);
                    return null;
                }
            }
        }

        public void encode(StringBuilder sb, String str, boolean z) throws UnsupportedEncodingException {
            sb.append("--");
            sb.append(str);
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
            if (this.mContentType != null) {
                sb.append("Content-Type: ");
                sb.append(this.mContentType);
            }
            if (this.mCharsetName != null) {
                sb.append("; ");
                sb.append("charset=\"");
                sb.append(this.mCharsetName);
                sb.append("\"");
            }
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
            if (this.mContentLocation != null) {
                sb.append("Content-Location: ");
                sb.append(this.mContentLocation);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mContentId != null) {
                sb.append("Content-ID: ");
                sb.append(this.mContentId);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mContentDisposition != null) {
                sb.append("Content-Disposition: ");
                sb.append(this.mContentDisposition);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mData != null) {
                if (this.mContentType != null && (this.mContentType.toUpperCase().contains("TEXT") || this.mContentType.toUpperCase().contains("SMIL"))) {
                    String str2 = new String(this.mData, "UTF-8");
                    if (str2.getBytes().length == str2.getBytes("UTF-8").length) {
                        sb.append("Content-Transfer-Encoding: 8BIT\r\n\r\n");
                    } else {
                        sb.append("Content-Transfer-Encoding: Quoted-Printable\r\n\r\n");
                        str2 = BluetoothMapUtils.encodeQuotedPrintable(this.mData);
                    }
                    sb.append(str2);
                    sb.append(VCardBuilder.VCARD_END_OF_LINE);
                } else {
                    sb.append("Content-Transfer-Encoding: Base64\r\n\r\n");
                    sb.append(Base64.encodeToString(this.mData, 0));
                    sb.append(VCardBuilder.VCARD_END_OF_LINE);
                }
            }
            if (z) {
                sb.append("--");
                sb.append(str);
                sb.append("--");
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
        }

        public void encodePlainText(StringBuilder sb) throws UnsupportedEncodingException {
            if (this.mContentType != null && this.mContentType.toUpperCase().contains("TEXT")) {
                String str = new String(this.mData, "UTF-8");
                if (str.getBytes().length != str.getBytes("UTF-8").length) {
                    str = BluetoothMapUtils.encodeQuotedPrintable(this.mData);
                }
                sb.append(str);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
                return;
            }
            if (this.mContentType == null || !this.mContentType.toUpperCase().contains("/SMIL")) {
                if (this.mPartName != null) {
                    sb.append("<");
                    sb.append(this.mPartName);
                    sb.append(">\r\n");
                } else {
                    sb.append("<");
                    sb.append("attachment");
                    sb.append(">\r\n");
                }
            }
        }
    }

    private String getBoundary() {
        if (this.mBoundary == null) {
            this.mBoundary = "--=_" + UUID.randomUUID();
        }
        return this.mBoundary;
    }

    public ArrayList<MimePart> getMimeParts() {
        return this.mParts;
    }

    public String getMessageAsText() {
        StringBuilder sb = new StringBuilder();
        if (this.mSubject != null && !this.mSubject.isEmpty()) {
            sb.append("<Sub:");
            sb.append(this.mSubject);
            sb.append("> ");
        }
        if (this.mParts != null) {
            for (MimePart mimePart : this.mParts) {
                if (mimePart.mContentType.toUpperCase().contains("TEXT")) {
                    sb.append(new String(mimePart.mData));
                }
            }
        }
        return sb.toString();
    }

    public MimePart addMimePart() {
        if (this.mParts == null) {
            this.mParts = new ArrayList<>();
        }
        MimePart mimePart = new MimePart();
        this.mParts.add(mimePart);
        return mimePart;
    }

    public String getDateString() {
        return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).format(new Date(this.mDate));
    }

    public long getDate() {
        return this.mDate;
    }

    public void setDate(long j) {
        this.mDate = j;
    }

    public String getSubject() {
        return this.mSubject;
    }

    public void setSubject(String str) {
        this.mSubject = str;
    }

    public ArrayList<Rfc822Token> getFrom() {
        return this.mFrom;
    }

    public void setFrom(ArrayList<Rfc822Token> arrayList) {
        this.mFrom = arrayList;
    }

    public void addFrom(String str, String str2) {
        if (this.mFrom == null) {
            this.mFrom = new ArrayList<>(1);
        }
        this.mFrom.add(new Rfc822Token(str, str2, null));
    }

    public ArrayList<Rfc822Token> getSender() {
        return this.mSender;
    }

    public void setSender(ArrayList<Rfc822Token> arrayList) {
        this.mSender = arrayList;
    }

    public void addSender(String str, String str2) {
        if (this.mSender == null) {
            this.mSender = new ArrayList<>(1);
        }
        this.mSender.add(new Rfc822Token(str, str2, null));
    }

    public ArrayList<Rfc822Token> getTo() {
        return this.mTo;
    }

    public void setTo(ArrayList<Rfc822Token> arrayList) {
        this.mTo = arrayList;
    }

    public void addTo(String str, String str2) {
        if (this.mTo == null) {
            this.mTo = new ArrayList<>(1);
        }
        this.mTo.add(new Rfc822Token(str, str2, null));
    }

    public ArrayList<Rfc822Token> getCc() {
        return this.mCc;
    }

    public void setCc(ArrayList<Rfc822Token> arrayList) {
        this.mCc = arrayList;
    }

    public void addCc(String str, String str2) {
        if (this.mCc == null) {
            this.mCc = new ArrayList<>(1);
        }
        this.mCc.add(new Rfc822Token(str, str2, null));
    }

    public ArrayList<Rfc822Token> getBcc() {
        return this.mBcc;
    }

    public void setBcc(ArrayList<Rfc822Token> arrayList) {
        this.mBcc = arrayList;
    }

    public void addBcc(String str, String str2) {
        if (this.mBcc == null) {
            this.mBcc = new ArrayList<>(1);
        }
        this.mBcc.add(new Rfc822Token(str, str2, null));
    }

    public ArrayList<Rfc822Token> getReplyTo() {
        return this.mReplyTo;
    }

    public void setReplyTo(ArrayList<Rfc822Token> arrayList) {
        this.mReplyTo = arrayList;
    }

    public void addReplyTo(String str, String str2) {
        if (this.mReplyTo == null) {
            this.mReplyTo = new ArrayList<>(1);
        }
        this.mReplyTo.add(new Rfc822Token(str, str2, null));
    }

    public void setMessageId(String str) {
        this.mMessageId = str;
    }

    public String getMessageId() {
        return this.mMessageId;
    }

    public void setContentType(String str) {
        this.mContentType = str;
    }

    public String getContentType() {
        return this.mContentType;
    }

    public void setTextOnly(boolean z) {
        this.mTextonly = z;
    }

    public boolean getTextOnly() {
        return this.mTextonly;
    }

    public void setIncludeAttachments(boolean z) {
        this.mIncludeAttachments = z;
    }

    public boolean getIncludeAttachments() {
        return this.mIncludeAttachments;
    }

    public void updateCharset() {
        if (this.mParts != null) {
            this.mCharset = null;
            for (MimePart mimePart : this.mParts) {
                if (mimePart.mContentType != null && mimePart.mContentType.toUpperCase().contains("TEXT")) {
                    this.mCharset = "UTF-8";
                    return;
                }
            }
        }
    }

    public int getSize() {
        int length = 0;
        if (this.mParts != null) {
            Iterator<MimePart> it = this.mParts.iterator();
            while (it.hasNext()) {
                length += it.next().mData.length;
            }
        }
        return length;
    }

    public void encodeHeaderAddresses(StringBuilder sb, String str, ArrayList<Rfc822Token> arrayList) {
        int length = str.getBytes().length + 0;
        sb.append(str);
        for (Rfc822Token rfc822Token : arrayList) {
            int length2 = rfc822Token.toString().getBytes().length + 1;
            if (length + length2 >= 998) {
                sb.append("\r\n ");
                length = 0;
            }
            sb.append(rfc822Token.toString());
            sb.append(";");
            length += length2;
        }
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
    }

    public void encodeHeaders(StringBuilder sb) throws UnsupportedEncodingException {
        if (this.mDate != -1) {
            sb.append("Date: ");
            sb.append(getDateString());
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.mSubject != null) {
            sb.append("Subject: ");
            sb.append(this.mSubject);
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.mFrom == null) {
            sb.append("From: \r\n");
        }
        if (this.mFrom != null) {
            encodeHeaderAddresses(sb, "From: ", this.mFrom);
        }
        if (this.mSender != null) {
            encodeHeaderAddresses(sb, "Sender: ", this.mSender);
        }
        if (this.mTo == null && this.mCc == null && this.mBcc == null) {
            sb.append("To:  undisclosed-recipients:;\r\n");
        }
        if (this.mTo != null) {
            encodeHeaderAddresses(sb, "To: ", this.mTo);
        }
        if (this.mCc != null) {
            encodeHeaderAddresses(sb, "Cc: ", this.mCc);
        }
        if (this.mBcc != null) {
            encodeHeaderAddresses(sb, "Bcc: ", this.mBcc);
        }
        if (this.mReplyTo != null) {
            encodeHeaderAddresses(sb, "Reply-To: ", this.mReplyTo);
        }
        if (this.mIncludeAttachments) {
            if (this.mMessageId != null) {
                sb.append("Message-Id: ");
                sb.append(this.mMessageId);
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mContentType != null) {
                sb.append("Content-Type: ");
                sb.append(this.mContentType);
                sb.append("; boundary=");
                sb.append(getBoundary());
                sb.append(VCardBuilder.VCARD_END_OF_LINE);
            }
        }
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
    }

    public byte[] encodeMime() throws UnsupportedEncodingException {
        ArrayList<byte[]> arrayList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        this.mEncoding = VCardConstants.PARAM_ENCODING_8BIT;
        encodeHeaders(sb);
        if (this.mParts != null) {
            if (!getIncludeAttachments()) {
                Iterator<MimePart> it = this.mParts.iterator();
                while (it.hasNext()) {
                    it.next().encodePlainText(sb);
                }
            } else {
                int i = 0;
                for (MimePart mimePart : this.mParts) {
                    boolean z = true;
                    i++;
                    String boundary = getBoundary();
                    if (i != this.mParts.size()) {
                        z = false;
                    }
                    mimePart.encode(sb, boundary, z);
                }
            }
        }
        String string = sb.toString();
        if (string != null) {
            arrayList.add(string.replaceAll("END:MSG", "/END\\:MSG").getBytes("UTF-8"));
        } else {
            arrayList.add(new byte[0]);
        }
        return encodeGeneric(arrayList);
    }

    private String parseMimeHeaders(String str) {
        String[] strArrSplit = str.split(VCardBuilder.VCARD_END_OF_LINE);
        if (D) {
            Log.d("BluetoothMapbMessage", "Header count=" + strArrSplit.length);
        }
        this.mHasHeaders = false;
        int length = strArrSplit.length;
        int i = 0;
        while (i < length) {
            String str2 = strArrSplit[i];
            if (D) {
                Log.d("BluetoothMapbMessage", "Header[" + i + "]: " + str2);
            }
            if (!str2.trim().isEmpty()) {
                String[] strArrSplit2 = str2.split(":", 2);
                if (strArrSplit2.length != 2) {
                    StringBuilder sb = new StringBuilder();
                    while (i < length) {
                        sb.append(strArrSplit[i]);
                        i++;
                    }
                    return sb.toString();
                }
                String upperCase = strArrSplit2[0].toUpperCase();
                String strTrim = strArrSplit2[1].trim();
                if (upperCase.contains("FROM")) {
                    this.mFrom = new ArrayList<>(Arrays.asList(Rfc822Tokenizer.tokenize(BluetoothMapUtils.stripEncoding(strTrim))));
                } else if (upperCase.contains("TO")) {
                    this.mTo = new ArrayList<>(Arrays.asList(Rfc822Tokenizer.tokenize(BluetoothMapUtils.stripEncoding(strTrim))));
                } else if (upperCase.contains("CC")) {
                    this.mCc = new ArrayList<>(Arrays.asList(Rfc822Tokenizer.tokenize(BluetoothMapUtils.stripEncoding(strTrim))));
                } else if (upperCase.contains("BCC")) {
                    this.mBcc = new ArrayList<>(Arrays.asList(Rfc822Tokenizer.tokenize(BluetoothMapUtils.stripEncoding(strTrim))));
                } else if (upperCase.contains("REPLY-TO")) {
                    this.mReplyTo = new ArrayList<>(Arrays.asList(Rfc822Tokenizer.tokenize(BluetoothMapUtils.stripEncoding(strTrim))));
                } else if (upperCase.contains("SUBJECT")) {
                    this.mSubject = BluetoothMapUtils.stripEncoding(strTrim);
                } else if (upperCase.contains("MESSAGE-ID")) {
                    this.mMessageId = strTrim;
                } else if (!upperCase.contains("DATE") && !upperCase.contains("MIME-VERSION")) {
                    if (upperCase.contains("CONTENT-TYPE")) {
                        String[] strArrSplit3 = strTrim.split(";");
                        this.mContentType = strArrSplit3[0];
                        int length2 = strArrSplit3.length;
                        for (int i2 = 1; i2 < length2; i2++) {
                            if (strArrSplit3[i2].contains("boundary")) {
                                this.mBoundary = strArrSplit3[i2].split("boundary[\\s]*=", 2)[1].trim();
                                if (this.mBoundary.charAt(0) == '\"' && this.mBoundary.charAt(this.mBoundary.length() - 1) == '\"') {
                                    this.mBoundary = this.mBoundary.substring(1, this.mBoundary.length() - 1);
                                }
                                if (D) {
                                    Log.d("BluetoothMapbMessage", "Boundary tag=" + this.mBoundary);
                                }
                            } else if (strArrSplit3[i2].contains(BluetoothMapContract.MessagePartColumns.CHARSET)) {
                                this.mCharset = strArrSplit3[i2].split("charset[\\s]*=", 2)[1].trim();
                            }
                        }
                    } else if (upperCase.contains("CONTENT-TRANSFER-ENCODING")) {
                        this.mMyEncoding = strTrim;
                    } else if (D) {
                        Log.w("BluetoothMapbMessage", "Skipping unknown header: " + upperCase + " (" + str2 + ")");
                    }
                }
            }
            i++;
        }
        return null;
    }

    private void parseMimePart(String str) {
        String str2;
        String[] strArrSplit = str.split("\r\n\r\n", 2);
        MimePart mimePartAddMimePart = addMimePart();
        String str3 = this.mMyEncoding;
        String[] strArrSplit2 = strArrSplit[0].split(VCardBuilder.VCARD_END_OF_LINE);
        if (D) {
            Log.d("BluetoothMapbMessage", "parseMimePart: headers count=" + strArrSplit2.length);
        }
        if (strArrSplit.length == 2) {
            str2 = str3;
            for (String str4 : strArrSplit2) {
                if (str4.length() != 0 && !str4.trim().isEmpty() && !str4.trim().equals("--")) {
                    String[] strArrSplit3 = str4.split(":", 2);
                    if (strArrSplit3.length != 2) {
                        if (D) {
                            Log.w("BluetoothMapbMessage", "part-Header not formatted correctly: ");
                        }
                    } else {
                        if (D) {
                            Log.d("BluetoothMapbMessage", "parseMimePart: header=" + str4);
                        }
                        String upperCase = strArrSplit3[0].toUpperCase();
                        String strTrim = strArrSplit3[1].trim();
                        if (upperCase.contains("CONTENT-TYPE")) {
                            String[] strArrSplit4 = strTrim.split(";");
                            mimePartAddMimePart.mContentType = strArrSplit4[0];
                            int length = strArrSplit4.length;
                            for (int i = 1; i < length; i++) {
                                String lowerCase = strArrSplit4[i].toLowerCase();
                                if (lowerCase.contains(BluetoothMapContract.MessagePartColumns.CHARSET)) {
                                    mimePartAddMimePart.mCharsetName = lowerCase.split("charset[\\s]*=", 2)[1].trim();
                                }
                            }
                        } else if (upperCase.contains("CONTENT-LOCATION")) {
                            mimePartAddMimePart.mContentLocation = strTrim;
                            mimePartAddMimePart.mPartName = strTrim;
                        } else if (upperCase.contains("CONTENT-TRANSFER-ENCODING")) {
                            str2 = strTrim;
                        } else if (upperCase.contains("CONTENT-ID")) {
                            mimePartAddMimePart.mContentId = strTrim;
                        } else if (upperCase.contains("CONTENT-DISPOSITION")) {
                            mimePartAddMimePart.mContentDisposition = strTrim;
                        } else if (D) {
                            Log.w("BluetoothMapbMessage", "Skipping unknown part-header: " + upperCase + " (" + str4 + ")");
                        }
                    }
                }
            }
            str = strArrSplit[1];
            if (str.length() > 2 && str.charAt(str.length() - 2) == '\r' && str.charAt(str.length() - 2) == '\n') {
                str = str.substring(0, str.length() - 2);
            }
        } else {
            str2 = str3;
        }
        mimePartAddMimePart.mData = decodeBody(str, str2, mimePartAddMimePart.mCharsetName);
    }

    private void parseMimeBody(String str) {
        MimePart mimePartAddMimePart = addMimePart();
        mimePartAddMimePart.mCharsetName = this.mCharset;
        mimePartAddMimePart.mData = decodeBody(str, this.mMyEncoding, this.mCharset);
    }

    private byte[] decodeBody(String str, String str2, String str3) {
        if (str2 != null && str2.toUpperCase().contains(VCardConstants.PARAM_ENCODING_BASE64)) {
            return Base64.decode(str, 0);
        }
        if (str2 != null && str2.toUpperCase().contains(VCardConstants.PARAM_ENCODING_QP)) {
            return BluetoothMapUtils.quotedPrintableToUtf8(str, str3);
        }
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private void parseMime(String str) {
        String strReplaceAll = str.replaceAll("\\r\\n[ \\\t]+", "");
        String[] strArrSplit = strReplaceAll.split("\r\n\r\n", 2);
        if (strArrSplit.length == 2) {
            String mimeHeaders = parseMimeHeaders(strArrSplit[0]);
            if (mimeHeaders != null) {
                String str2 = mimeHeaders + strArrSplit[1];
                if (D) {
                    Log.d("BluetoothMapbMessage", "parseMime remaining=" + mimeHeaders);
                }
                strReplaceAll = str2;
            } else {
                strReplaceAll = strArrSplit[1];
            }
        }
        if (this.mBoundary == null) {
            parseMimeBody(strReplaceAll);
            setTextOnly(true);
            if (this.mContentType == null) {
                this.mContentType = "text/plain";
            }
            this.mParts.get(0).mContentType = this.mContentType;
            return;
        }
        String[] strArrSplit2 = strReplaceAll.split("--" + this.mBoundary);
        if (D) {
            Log.d("BluetoothMapbMessage", "mimePart count=" + strArrSplit2.length);
        }
        for (int i = 1; i < strArrSplit2.length - 1; i++) {
            String str3 = strArrSplit2[i];
            if (str3 != null && str3.length() > 0) {
                parseMimePart(str3);
            }
        }
    }

    @Override
    public void parseMsgPart(String str) {
        parseMime(str);
    }

    @Override
    public void parseMsgInit() {
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        return encodeMime();
    }
}
