package com.android.bluetooth.mapclient;

import android.util.Log;
import com.android.bluetooth.mapclient.Bmessage;
import com.android.bluetooth.mapclient.BmsgTokenizer;
import com.android.vcard.VCardConstants;
import com.android.vcard.VCardEntry;
import com.android.vcard.VCardEntryConstructor;
import com.android.vcard.VCardEntryHandler;
import com.android.vcard.VCardParser_V21;
import com.android.vcard.VCardParser_V30;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardVersionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

class BmessageParser {
    private static final String CRLF = "\r\n";
    private static final int CRLF_LEN = 2;
    private static final boolean DBG = false;
    private static final int MSG_CONTAINER_LEN = 22;
    private static final String TAG = "BmessageParser";
    private final Bmessage mBmsg = new Bmessage();
    private BmsgTokenizer mParser;
    private static final BmsgTokenizer.Property BEGIN_BMSG = new BmsgTokenizer.Property(VCardConstants.PROPERTY_BEGIN, "BMSG");
    private static final BmsgTokenizer.Property END_BMSG = new BmsgTokenizer.Property(VCardConstants.PROPERTY_END, "BMSG");
    private static final BmsgTokenizer.Property BEGIN_VCARD = new BmsgTokenizer.Property(VCardConstants.PROPERTY_BEGIN, "VCARD");
    private static final BmsgTokenizer.Property END_VCARD = new BmsgTokenizer.Property(VCardConstants.PROPERTY_END, "VCARD");
    private static final BmsgTokenizer.Property BEGIN_BENV = new BmsgTokenizer.Property(VCardConstants.PROPERTY_BEGIN, "BENV");
    private static final BmsgTokenizer.Property END_BENV = new BmsgTokenizer.Property(VCardConstants.PROPERTY_END, "BENV");
    private static final BmsgTokenizer.Property BEGIN_BBODY = new BmsgTokenizer.Property(VCardConstants.PROPERTY_BEGIN, "BBODY");
    private static final BmsgTokenizer.Property END_BBODY = new BmsgTokenizer.Property(VCardConstants.PROPERTY_END, "BBODY");
    private static final BmsgTokenizer.Property BEGIN_MSG = new BmsgTokenizer.Property(VCardConstants.PROPERTY_BEGIN, VCardConstants.PARAM_TYPE_MSG);
    private static final BmsgTokenizer.Property END_MSG = new BmsgTokenizer.Property(VCardConstants.PROPERTY_END, VCardConstants.PARAM_TYPE_MSG);

    private BmessageParser() {
    }

    public static Bmessage createBmessage(String str) {
        BmessageParser bmessageParser = new BmessageParser();
        try {
            bmessageParser.parse(str);
            return bmessageParser.mBmsg;
        } catch (IOException e) {
            Log.e(TAG, "I/O exception when parsing bMessage", e);
            return null;
        } catch (ParseException e2) {
            Log.e(TAG, "Cannot parse bMessage", e2);
            return null;
        }
    }

    private ParseException expected(BmsgTokenizer.Property... propertyArr) {
        StringBuilder sb = new StringBuilder();
        int length = propertyArr.length;
        boolean z = true;
        int i = 0;
        while (i < length) {
            BmsgTokenizer.Property property = propertyArr[i];
            if (!z) {
                sb.append(" or ");
            }
            sb.append(property);
            i++;
            z = false;
        }
        return new ParseException("Expected: " + sb.toString(), this.mParser.pos());
    }

    private void parse(String str) throws IOException, ParseException {
        this.mParser = new BmsgTokenizer(str + "\r\n");
        if (!this.mParser.next().equals(BEGIN_BMSG)) {
            throw expected(BEGIN_BMSG);
        }
        BmsgTokenizer.Property properties = parseProperties();
        while (properties.equals(BEGIN_VCARD)) {
            StringBuilder sb = new StringBuilder();
            BmsgTokenizer.Property propertyExtractVcard = extractVcard(sb);
            this.mBmsg.mOriginators.add(parseVcard(sb.toString()));
            properties = propertyExtractVcard;
        }
        if (!properties.equals(BEGIN_BENV)) {
            throw expected(BEGIN_BENV);
        }
        if (!parseEnvelope(1).equals(END_BMSG)) {
            throw expected(END_BENV);
        }
        this.mParser = null;
    }

    private BmsgTokenizer.Property parseProperties() throws ParseException {
        BmsgTokenizer.Property next;
        do {
            next = this.mParser.next();
            if (next.name.equals(VCardConstants.PROPERTY_VERSION)) {
                this.mBmsg.mBmsgVersion = next.value;
            } else {
                int i = 0;
                if (next.name.equals("STATUS")) {
                    Bmessage.Status[] statusArrValues = Bmessage.Status.values();
                    int length = statusArrValues.length;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        Bmessage.Status status = statusArrValues[i];
                        if (!next.value.equals(status.toString())) {
                            i++;
                        } else {
                            this.mBmsg.mBmsgStatus = status;
                            break;
                        }
                    }
                } else if (next.name.equals(VCardConstants.PARAM_TYPE)) {
                    Bmessage.Type[] typeArrValues = Bmessage.Type.values();
                    int length2 = typeArrValues.length;
                    while (true) {
                        if (i >= length2) {
                            break;
                        }
                        Bmessage.Type type = typeArrValues[i];
                        if (!next.value.equals(type.toString())) {
                            i++;
                        } else {
                            this.mBmsg.mBmsgType = type;
                            break;
                        }
                    }
                } else if (next.name.equals("FOLDER")) {
                    this.mBmsg.mBmsgFolder = next.value;
                }
            }
            if (next.equals(BEGIN_VCARD)) {
                break;
            }
        } while (!next.equals(BEGIN_BENV));
        return next;
    }

    private BmsgTokenizer.Property parseEnvelope(int i) throws IOException, ParseException {
        BmsgTokenizer.Property body;
        if (i > 3) {
            throw new ParseException("bEnvelope is nested more than 3 times", this.mParser.pos());
        }
        BmsgTokenizer.Property next = this.mParser.next();
        while (next.equals(BEGIN_VCARD)) {
            StringBuilder sb = new StringBuilder();
            BmsgTokenizer.Property propertyExtractVcard = extractVcard(sb);
            if (i == 1) {
                this.mBmsg.mRecipients.add(parseVcard(sb.toString()));
            }
            next = propertyExtractVcard;
        }
        if (next.equals(BEGIN_BENV)) {
            body = parseEnvelope(i + 1);
        } else {
            if (!next.equals(BEGIN_BBODY)) {
                throw expected(BEGIN_BENV, BEGIN_BBODY);
            }
            body = parseBody();
        }
        if (body.equals(END_BENV)) {
            return this.mParser.next();
        }
        throw expected(END_BENV);
    }

    private BmsgTokenizer.Property parseBody() throws IOException, ParseException {
        BmsgTokenizer.Property next;
        do {
            next = this.mParser.next();
            if (!next.name.equals("PARTID")) {
                if (next.name.equals(VCardConstants.PARAM_ENCODING)) {
                    this.mBmsg.mBbodyEncoding = next.value;
                } else if (next.name.equals(VCardConstants.PARAM_CHARSET)) {
                    this.mBmsg.mBbodyCharset = next.value;
                } else if (next.name.equals(VCardConstants.PARAM_LANGUAGE)) {
                    this.mBmsg.mBbodyLanguage = next.value;
                } else if (next.name.equals("LENGTH")) {
                    try {
                        this.mBmsg.mBbodyLength = Integer.parseInt(next.value);
                    } catch (NumberFormatException e) {
                        throw new ParseException("Invalid LENGTH value", this.mParser.pos());
                    }
                }
            }
        } while (!next.equals(BEGIN_MSG));
        if (!"UTF-8".equals(this.mBmsg.mBbodyCharset)) {
            Log.e(TAG, "The charset was not set to charset UTF-8: " + this.mBmsg.mBbodyCharset);
        }
        int i = this.mBmsg.mBbodyLength - 22;
        int i2 = i + 2;
        int iPos = this.mParser.pos() + i2;
        String strRemaining = this.mParser.remaining();
        byte[] bytes = strRemaining.getBytes();
        this.mParser = new BmsgTokenizer(new String(bytes, i2, bytes.length - i2), iPos);
        BmsgTokenizer.Property next2 = this.mParser.next(true);
        if (next2 != null) {
            if (next2.equals(END_MSG)) {
                if (!"UTF-8".equals(this.mBmsg.mBbodyCharset)) {
                    this.mBmsg.mMessage = null;
                } else {
                    this.mBmsg.mMessage = new String(bytes, 0, i, StandardCharsets.UTF_8);
                }
            } else {
                Log.e(TAG, "Prop Invalid: " + next2.toString());
                Log.e(TAG, "Possible Invalid LENGTH value");
                throw expected(END_MSG);
            }
        } else {
            if (i2 < 0 || i2 > strRemaining.length()) {
                throw new ParseException("Invalid LENGTH value", this.mParser.pos());
            }
            Log.w(TAG, "byte LENGTH seems to be invalid, trying with char length");
            this.mParser = new BmsgTokenizer(strRemaining.substring(i2));
            if (!this.mParser.next().equals(END_MSG)) {
                throw expected(END_MSG);
            }
            if (!"UTF-8".equals(this.mBmsg.mBbodyCharset)) {
                this.mBmsg.mMessage = null;
            } else {
                this.mBmsg.mMessage = strRemaining.substring(0, i);
            }
        }
        if (this.mParser.next().equals(END_BBODY)) {
            return this.mParser.next();
        }
        throw expected(END_BBODY);
    }

    private BmsgTokenizer.Property extractVcard(StringBuilder sb) throws IOException, ParseException {
        BmsgTokenizer.Property next;
        sb.append(BEGIN_VCARD);
        sb.append("\r\n");
        do {
            next = this.mParser.next();
            sb.append(next);
            sb.append("\r\n");
        } while (!next.equals(END_VCARD));
        return this.mParser.next();
    }

    private VCardEntry parseVcard(String str) throws IOException, ParseException {
        VCardEntry vCardEntry = 0;
        vCardEntry = 0;
        vCardEntry = 0;
        try {
            VCardParser_V21 vCardParser_V21 = new VCardParser_V21();
            VCardEntryConstructor vCardEntryConstructor = new VCardEntryConstructor();
            VcardHandler vcardHandler = new VcardHandler();
            vCardEntryConstructor.addEntryHandler(vcardHandler);
            vCardParser_V21.addInterpreter(vCardEntryConstructor);
            vCardParser_V21.parse(new ByteArrayInputStream(str.getBytes()));
            vCardEntry = vcardHandler.vcard;
        } catch (VCardVersionException e) {
            try {
                VCardParser_V30 vCardParser_V30 = new VCardParser_V30();
                VCardEntryConstructor vCardEntryConstructor2 = new VCardEntryConstructor();
                VcardHandler vcardHandler2 = new VcardHandler();
                vCardEntryConstructor2.addEntryHandler(vcardHandler2);
                vCardParser_V30.addInterpreter(vCardEntryConstructor2);
                vCardParser_V30.parse(new ByteArrayInputStream(str.getBytes()));
                vCardEntry = vcardHandler2.vcard;
            } catch (VCardVersionException e2) {
            } catch (VCardException e3) {
            }
        } catch (VCardException e4) {
        }
        if (vCardEntry == 0) {
            throw new ParseException("Cannot parse vCard object (neither 2.1 nor 3.0?)", this.mParser.pos());
        }
        return vCardEntry;
    }

    private class VcardHandler implements VCardEntryHandler {
        public VCardEntry vcard;

        private VcardHandler() {
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onEntryCreated(VCardEntry vCardEntry) {
            this.vcard = vCardEntry;
        }

        @Override
        public void onEnd() {
        }
    }
}
