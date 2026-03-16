package com.android.bluetooth.map;

import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Base64;
import android.util.Log;
import com.android.vcard.VCardBuilder;
import com.android.vcard.VCardConstants;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class BluetoothMapbMessageMms extends BluetoothMapbMessage {
    private boolean includeAttachments;
    private long date = INVALID_VALUE;
    private String subject = null;
    private ArrayList<Rfc822Token> from = null;
    private ArrayList<Rfc822Token> sender = null;
    private ArrayList<Rfc822Token> to = null;
    private ArrayList<Rfc822Token> cc = null;
    private ArrayList<Rfc822Token> bcc = null;
    private ArrayList<Rfc822Token> replyTo = null;
    private String messageId = null;
    private ArrayList<MimePart> parts = null;
    private String contentType = null;
    private String boundary = null;
    private boolean textOnly = false;
    private boolean hasHeaders = false;
    private String encoding = null;

    public static class MimePart {
        public long mId = BluetoothMapbMessage.INVALID_VALUE;
        public String mContentType = null;
        public String mContentId = null;
        public String mContentLocation = null;
        public String mContentDisposition = null;
        public String mPartName = null;
        public String mCharsetName = null;
        public String mFileName = null;
        public byte[] mData = null;

        String getDataAsString() {
            String charset;
            String charset2 = this.mCharsetName;
            if (charset2 == null) {
                charset = "UTF-8";
            } else {
                charset = charset2.toUpperCase();
                try {
                    if (!Charset.isSupported(charset)) {
                        charset = "UTF-8";
                    }
                } catch (IllegalCharsetNameException e) {
                    Log.w(BluetoothMapbMessage.TAG, "Received unknown charset: " + charset + " - using UTF-8.");
                    charset = "UTF-8";
                }
            }
            try {
                String result = new String(this.mData, charset);
                return result;
            } catch (UnsupportedEncodingException e2) {
                try {
                    String result2 = new String(this.mData, "UTF-8");
                    return result2;
                } catch (UnsupportedEncodingException e3) {
                    return null;
                }
            }
        }

        public void encode(StringBuilder sb, String boundaryTag, boolean last) throws UnsupportedEncodingException {
            sb.append("--").append(boundaryTag).append(VCardBuilder.VCARD_END_OF_LINE);
            if (this.mContentType != null) {
                sb.append("Content-Type: ").append(this.mContentType);
            }
            if (this.mCharsetName != null) {
                sb.append("; ").append("charset=\"").append(this.mCharsetName).append("\"");
            }
            sb.append(VCardBuilder.VCARD_END_OF_LINE);
            if (this.mContentLocation != null) {
                sb.append("Content-Location: ").append(this.mContentLocation).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mContentId != null) {
                sb.append("Content-ID: ").append(this.mContentId).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mContentDisposition != null) {
                sb.append("Content-Disposition: ").append(this.mContentDisposition).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.mData != null) {
                if (this.mContentType != null && (this.mContentType.toUpperCase().contains("TEXT") || this.mContentType.toUpperCase().contains("SMIL"))) {
                    sb.append("Content-Transfer-Encoding: 8BIT\r\n\r\n");
                    sb.append(new String(this.mData, "UTF-8")).append(VCardBuilder.VCARD_END_OF_LINE);
                } else {
                    sb.append("Content-Transfer-Encoding: Base64\r\n\r\n");
                    sb.append(Base64.encodeToString(this.mData, 0)).append(VCardBuilder.VCARD_END_OF_LINE);
                }
            }
            if (last) {
                sb.append("--").append(boundaryTag).append("--").append(VCardBuilder.VCARD_END_OF_LINE);
            }
        }

        public void encodePlainText(StringBuilder sb) throws UnsupportedEncodingException {
            if (this.mContentType != null && this.mContentType.toUpperCase().contains("TEXT")) {
                sb.append(new String(this.mData, "UTF-8")).append(VCardBuilder.VCARD_END_OF_LINE);
                return;
            }
            if (this.mContentType == null || !this.mContentType.toUpperCase().contains("/SMIL")) {
                if (this.mPartName != null) {
                    sb.append("<").append(this.mPartName).append(">\r\n");
                } else {
                    sb.append("<").append("attachment").append(">\r\n");
                }
            }
        }
    }

    private String getBoundary() {
        if (this.boundary == null) {
            this.boundary = "--=_" + UUID.randomUUID();
        }
        return this.boundary;
    }

    public ArrayList<MimePart> getMimeParts() {
        return this.parts;
    }

    public String getMessageAsText() {
        StringBuilder sb = new StringBuilder();
        if (this.subject != null && !this.subject.isEmpty()) {
            sb.append("<Sub:").append(this.subject).append("> ");
        }
        if (this.parts != null) {
            for (MimePart part : this.parts) {
                if (part.mContentType.toUpperCase().contains("TEXT")) {
                    sb.append(new String(part.mData));
                }
            }
        }
        return sb.toString();
    }

    public MimePart addMimePart() {
        if (this.parts == null) {
            this.parts = new ArrayList<>();
        }
        MimePart newPart = new MimePart();
        this.parts.add(newPart);
        return newPart;
    }

    public String getDateString() {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        Date dateObj = new Date(this.date);
        return format.format(dateObj);
    }

    public long getDate() {
        return this.date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public ArrayList<Rfc822Token> getFrom() {
        return this.from;
    }

    public void setFrom(ArrayList<Rfc822Token> from) {
        this.from = from;
    }

    public void addFrom(String name, String address) {
        if (this.from == null) {
            this.from = new ArrayList<>(1);
        }
        this.from.add(new Rfc822Token(name, address, null));
    }

    public ArrayList<Rfc822Token> getSender() {
        return this.sender;
    }

    public void setSender(ArrayList<Rfc822Token> sender) {
        this.sender = sender;
    }

    public void addSender(String name, String address) {
        if (this.sender == null) {
            this.sender = new ArrayList<>(1);
        }
        this.sender.add(new Rfc822Token(name, address, null));
    }

    public ArrayList<Rfc822Token> getTo() {
        return this.to;
    }

    public void setTo(ArrayList<Rfc822Token> to) {
        this.to = to;
    }

    public void addTo(String name, String address) {
        if (this.to == null) {
            this.to = new ArrayList<>(1);
        }
        this.to.add(new Rfc822Token(name, address, null));
    }

    public ArrayList<Rfc822Token> getCc() {
        return this.cc;
    }

    public void setCc(ArrayList<Rfc822Token> cc) {
        this.cc = cc;
    }

    public void addCc(String name, String address) {
        if (this.cc == null) {
            this.cc = new ArrayList<>(1);
        }
        this.cc.add(new Rfc822Token(name, address, null));
    }

    public ArrayList<Rfc822Token> getBcc() {
        return this.bcc;
    }

    public void setBcc(ArrayList<Rfc822Token> bcc) {
        this.bcc = bcc;
    }

    public void addBcc(String name, String address) {
        if (this.bcc == null) {
            this.bcc = new ArrayList<>(1);
        }
        this.bcc.add(new Rfc822Token(name, address, null));
    }

    public ArrayList<Rfc822Token> getReplyTo() {
        return this.replyTo;
    }

    public void setReplyTo(ArrayList<Rfc822Token> replyTo) {
        this.replyTo = replyTo;
    }

    public void addReplyTo(String name, String address) {
        if (this.replyTo == null) {
            this.replyTo = new ArrayList<>(1);
        }
        this.replyTo.add(new Rfc822Token(name, address, null));
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setTextOnly(boolean textOnly) {
        this.textOnly = textOnly;
    }

    public boolean getTextOnly() {
        return this.textOnly;
    }

    public void setIncludeAttachments(boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }

    public boolean getIncludeAttachments() {
        return this.includeAttachments;
    }

    public void updateCharset() {
        if (this.parts != null) {
            this.mCharset = null;
            for (MimePart part : this.parts) {
                if (part.mContentType != null && part.mContentType.toUpperCase().contains("TEXT")) {
                    this.mCharset = "UTF-8";
                    return;
                }
            }
        }
    }

    public int getSize() {
        int message_size = 0;
        if (this.parts != null) {
            for (MimePart part : this.parts) {
                message_size += part.mData.length;
            }
        }
        return message_size;
    }

    public void encodeHeaderAddresses(StringBuilder sb, String headerName, ArrayList<Rfc822Token> addresses) {
        int lineLength = 0 + headerName.getBytes().length;
        sb.append(headerName);
        for (Rfc822Token address : addresses) {
            int partLength = address.toString().getBytes().length + 1;
            if (lineLength + partLength >= 998) {
                sb.append("\r\n ");
                lineLength = 0;
            }
            sb.append(address.toString()).append(";");
            lineLength += partLength;
        }
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
    }

    public void encodeHeaders(StringBuilder sb) throws UnsupportedEncodingException {
        if (this.date != INVALID_VALUE) {
            sb.append("Date: ").append(getDateString()).append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.subject != null) {
            sb.append("Subject: ").append(this.subject).append(VCardBuilder.VCARD_END_OF_LINE);
        }
        if (this.from == null) {
            sb.append("From: \r\n");
        }
        if (this.from != null) {
            encodeHeaderAddresses(sb, "From: ", this.from);
        }
        if (this.sender != null) {
            encodeHeaderAddresses(sb, "Sender: ", this.sender);
        }
        if (this.to == null && this.cc == null && this.bcc == null) {
            sb.append("To:  undisclosed-recipients:;\r\n");
        }
        if (this.to != null) {
            encodeHeaderAddresses(sb, "To: ", this.to);
        }
        if (this.cc != null) {
            encodeHeaderAddresses(sb, "Cc: ", this.cc);
        }
        if (this.bcc != null) {
            encodeHeaderAddresses(sb, "Bcc: ", this.bcc);
        }
        if (this.replyTo != null) {
            encodeHeaderAddresses(sb, "Reply-To: ", this.replyTo);
        }
        if (this.includeAttachments) {
            if (this.messageId != null) {
                sb.append("Message-Id: ").append(this.messageId).append(VCardBuilder.VCARD_END_OF_LINE);
            }
            if (this.contentType != null) {
                sb.append("Content-Type: ").append(this.contentType).append("; boundary=").append(getBoundary()).append(VCardBuilder.VCARD_END_OF_LINE);
            }
        }
        sb.append(VCardBuilder.VCARD_END_OF_LINE);
    }

    public byte[] encodeMms() throws UnsupportedEncodingException {
        ArrayList<byte[]> bodyFragments = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        this.encoding = VCardConstants.PARAM_ENCODING_8BIT;
        encodeHeaders(sb);
        if (this.parts != null) {
            if (!getIncludeAttachments()) {
                for (MimePart part : this.parts) {
                    part.encodePlainText(sb);
                }
            } else {
                for (MimePart part2 : this.parts) {
                    count++;
                    part2.encode(sb, getBoundary(), count == this.parts.size());
                }
            }
        }
        String mmsBody = sb.toString();
        if (mmsBody != null) {
            String tmpBody = mmsBody.replaceAll("END:MSG", "/END\\:MSG");
            bodyFragments.add(tmpBody.getBytes("UTF-8"));
        } else {
            bodyFragments.add(new byte[0]);
        }
        return encodeGeneric(bodyFragments);
    }

    private String parseMmsHeaders(String hdrPart) {
        String[] headers = hdrPart.split(VCardBuilder.VCARD_END_OF_LINE);
        Log.d(TAG, "Header count=" + headers.length);
        this.hasHeaders = false;
        int i = 0;
        int c = headers.length;
        while (i < c) {
            String header = headers[i];
            Log.d(TAG, "Header[" + i + "]: " + header);
            if (header.trim() != "") {
                String[] headerParts = header.split(":", 2);
                if (headerParts.length != 2) {
                    StringBuilder remaining = new StringBuilder();
                    while (i < c) {
                        remaining.append(headers[i]);
                        i++;
                    }
                    return remaining.toString();
                }
                String headerType = headerParts[0].toUpperCase();
                String headerValue = headerParts[1].trim();
                if (headerType.contains("FROM")) {
                    Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(headerValue);
                    this.from = new ArrayList<>(Arrays.asList(tokens));
                } else if (headerType.contains("TO")) {
                    Rfc822Token[] tokens2 = Rfc822Tokenizer.tokenize(headerValue);
                    this.to = new ArrayList<>(Arrays.asList(tokens2));
                } else if (headerType.contains("CC")) {
                    Rfc822Token[] tokens3 = Rfc822Tokenizer.tokenize(headerValue);
                    this.cc = new ArrayList<>(Arrays.asList(tokens3));
                } else if (headerType.contains("BCC")) {
                    Rfc822Token[] tokens4 = Rfc822Tokenizer.tokenize(headerValue);
                    this.bcc = new ArrayList<>(Arrays.asList(tokens4));
                } else if (headerType.contains("REPLY-TO")) {
                    Rfc822Token[] tokens5 = Rfc822Tokenizer.tokenize(headerValue);
                    this.replyTo = new ArrayList<>(Arrays.asList(tokens5));
                } else if (headerType.contains("SUBJECT")) {
                    this.subject = headerValue;
                } else if (headerType.contains("MESSAGE-ID")) {
                    this.messageId = headerValue;
                } else if (!headerType.contains("DATE") && !headerType.contains("MIME-VERSION")) {
                    if (headerType.contains("CONTENT-TYPE")) {
                        String[] contentTypeParts = headerValue.split(";");
                        this.contentType = contentTypeParts[0];
                        int n = contentTypeParts.length;
                        for (int j = 1; j < n; j++) {
                            if (contentTypeParts[j].contains("boundary")) {
                                this.boundary = contentTypeParts[j].split("boundary[\\s]*=", 2)[1].trim();
                                if (this.boundary.charAt(0) == '\"' && this.boundary.charAt(this.boundary.length() - 1) == '\"') {
                                    this.boundary = this.boundary.substring(1, this.boundary.length() - 1);
                                }
                                Log.d(TAG, "Boundary tag=" + this.boundary);
                            } else if (contentTypeParts[j].contains("charset")) {
                                this.mCharset = contentTypeParts[j].split("charset[\\s]*=", 2)[1].trim();
                            }
                        }
                    } else if (headerType.contains("CONTENT-TRANSFER-ENCODING")) {
                        this.encoding = headerValue;
                    } else {
                        Log.w(TAG, "Skipping unknown header: " + headerType + " (" + header + ")");
                    }
                }
            }
            i++;
        }
        return null;
    }

    private void parseMmsMimePart(String partStr) {
        String body;
        String[] parts = partStr.split("\r\n\r\n", 2);
        MimePart newPart = addMimePart();
        String partEncoding = this.encoding;
        String[] headers = parts[0].split(VCardBuilder.VCARD_END_OF_LINE);
        Log.d(TAG, "parseMmsMimePart: headers count=" + headers.length);
        if (parts.length != 2) {
            body = partStr;
        } else {
            for (String header : headers) {
                if (header.length() != 0 && !header.trim().isEmpty() && !header.trim().equals("--")) {
                    String[] headerParts = header.split(":", 2);
                    if (headerParts.length != 2) {
                        Log.w(TAG, "part-Header not formatted correctly: ");
                    } else {
                        Log.d(TAG, "parseMmsMimePart: header=" + header);
                        String headerType = headerParts[0].toUpperCase();
                        String headerValue = headerParts[1].trim();
                        if (headerType.contains("CONTENT-TYPE")) {
                            String[] contentTypeParts = headerValue.split(";");
                            newPart.mContentType = contentTypeParts[0];
                            int n = contentTypeParts.length;
                            for (int j = 1; j < n; j++) {
                                String value = contentTypeParts[j].toLowerCase();
                                if (value.contains("charset")) {
                                    newPart.mCharsetName = value.split("charset[\\s]*=", 2)[1].trim();
                                }
                            }
                        } else if (headerType.contains("CONTENT-LOCATION")) {
                            newPart.mContentLocation = headerValue;
                            newPart.mPartName = headerValue;
                        } else if (headerType.contains("CONTENT-TRANSFER-ENCODING")) {
                            partEncoding = headerValue;
                        } else if (headerType.contains("CONTENT-ID")) {
                            newPart.mContentId = headerValue;
                        } else if (headerType.contains("CONTENT-DISPOSITION")) {
                            newPart.mContentDisposition = headerValue;
                        } else {
                            Log.w(TAG, "Skipping unknown part-header: " + headerType + " (" + header + ")");
                        }
                    }
                }
            }
            body = parts[1];
            if (body.length() > 2 && body.charAt(body.length() - 2) == '\r' && body.charAt(body.length() - 2) == '\n') {
                body = body.substring(0, body.length() - 2);
            }
        }
        newPart.mData = decodeBody(body, partEncoding, newPart.mCharsetName);
    }

    private void parseMmsMimeBody(String body) {
        MimePart newPart = addMimePart();
        newPart.mCharsetName = this.mCharset;
        newPart.mData = decodeBody(body, this.encoding, this.mCharset);
    }

    private byte[] decodeBody(String body, String encoding, String charset) {
        if (encoding != null && encoding.toUpperCase().contains(VCardConstants.PARAM_ENCODING_BASE64)) {
            return Base64.decode(body, 0);
        }
        if (encoding != null && encoding.toUpperCase().contains(VCardConstants.PARAM_ENCODING_QP)) {
            return quotedPrintableToUtf8(body, charset);
        }
        try {
            return body.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private void parseMms(String message) {
        String messageBody;
        String message2 = message.replaceAll("\\r\\n[ \\\t]+", "");
        String[] messageParts = message2.split("\r\n\r\n", 2);
        if (messageParts.length != 2) {
            messageBody = message2;
        } else {
            String remaining = parseMmsHeaders(messageParts[0]);
            if (remaining != null) {
                messageBody = remaining + messageParts[1];
                Log.d(TAG, "parseMms remaining=" + remaining);
            } else {
                messageBody = messageParts[1];
            }
        }
        if (this.boundary == null) {
            parseMmsMimeBody(messageBody);
            setTextOnly(true);
            if (this.contentType == null) {
                this.contentType = "text/plain";
            }
            this.parts.get(0).mContentType = this.contentType;
            return;
        }
        String[] mimeParts = messageBody.split("--" + this.boundary);
        Log.d(TAG, "mimePart count=" + mimeParts.length);
        for (int i = 1; i < mimeParts.length - 1; i++) {
            String part = mimeParts[i];
            if (part != null && part.length() > 0) {
                parseMmsMimePart(part);
            }
        }
    }

    public static byte[] quotedPrintableToUtf8(String text, String charset) {
        String charset2;
        int out;
        byte[] output = new byte[text.length()];
        byte[] input = null;
        try {
            input = text.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
        }
        if (input == null) {
            return "".getBytes();
        }
        int stopCnt = input.length - 2;
        int in = 0;
        int out2 = 0;
        while (in < stopCnt) {
            byte b0 = input[in];
            if (b0 == 61) {
                int in2 = in + 1;
                byte b1 = input[in2];
                in = in2 + 1;
                byte b2 = input[in];
                if (b1 == 13 && b2 == 10) {
                    out = out2;
                } else if (((b1 >= 48 && b1 <= 57) || ((b1 >= 65 && b1 <= 70) || (b1 >= 97 && b1 <= 102))) && ((b2 >= 48 && b2 <= 57) || ((b2 >= 65 && b2 <= 70) || (b2 >= 97 && b2 <= 102)))) {
                    if (b1 <= 57) {
                        b1 = (byte) (b1 - 48);
                    } else if (b1 <= 70) {
                        b1 = (byte) ((b1 - 65) + 10);
                    } else if (b1 <= 102) {
                        b1 = (byte) ((b1 - 97) + 10);
                    }
                    if (b2 <= 57) {
                        b2 = (byte) (b2 - 48);
                    } else if (b2 <= 70) {
                        b2 = (byte) ((b2 - 65) + 10);
                    } else if (b2 <= 102) {
                        b2 = (byte) ((b2 - 97) + 10);
                    }
                    out = out2 + 1;
                    output[out2] = (byte) ((b1 << 4) | b2);
                } else {
                    Log.w(TAG, "Received wrongly quoted printable encoded text. Continuing at best effort...");
                    out = out2 + 1;
                    output[out2] = b0;
                    in -= 2;
                }
            } else {
                out = out2 + 1;
                output[out2] = b0;
            }
            in++;
            out2 = out;
        }
        int out3 = out2;
        while (in < input.length) {
            output[out3] = input[in];
            out3++;
            in++;
        }
        String result = null;
        if (charset == null) {
            charset2 = "UTF-8";
        } else {
            charset2 = charset.toUpperCase();
            try {
                if (!Charset.isSupported(charset2)) {
                    charset2 = "UTF-8";
                }
            } catch (IllegalCharsetNameException e2) {
                Log.w(TAG, "Received unknown charset: " + charset2 + " - using UTF-8.");
                charset2 = "UTF-8";
            }
        }
        try {
            String result2 = new String(output, 0, out3, charset2);
            result = result2;
        } catch (UnsupportedEncodingException e3) {
            try {
                String result3 = new String(output, 0, out3, "UTF-8");
                result = result3;
            } catch (UnsupportedEncodingException e4) {
            }
        }
        return result.getBytes();
    }

    @Override
    public void parseMsgPart(String msgPart) {
        parseMms(msgPart);
    }

    @Override
    public void parseMsgInit() {
    }

    @Override
    public byte[] encode() throws UnsupportedEncodingException {
        return encodeMms();
    }
}
