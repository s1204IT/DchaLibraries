package com.android.vcard;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.vcard.exception.VCardAgentNotSupportedException;
import com.android.vcard.exception.VCardException;
import com.android.vcard.exception.VCardInvalidCommentLineException;
import com.android.vcard.exception.VCardInvalidLineException;
import com.android.vcard.exception.VCardVersionException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class VCardParserImpl_V21 {
    private boolean mCanceled;
    protected String mCurrentCharset;
    protected String mCurrentEncoding;
    protected final String mIntermediateCharset;
    private final List<VCardInterpreter> mInterpreterList;
    protected CustomBufferedReader mReader;
    protected final Set<String> mUnknownTypeSet;
    protected final Set<String> mUnknownValueSet;

    protected static final class CustomBufferedReader extends BufferedReader {
        private String mNextLine;
        private boolean mNextLineIsValid;
        private long mTime;

        public CustomBufferedReader(Reader reader) {
            super(reader);
        }

        @Override
        public String readLine() throws IOException {
            if (this.mNextLineIsValid) {
                String str = this.mNextLine;
                this.mNextLine = null;
                this.mNextLineIsValid = false;
                return str;
            }
            long jCurrentTimeMillis = System.currentTimeMillis();
            String line = super.readLine();
            this.mTime += System.currentTimeMillis() - jCurrentTimeMillis;
            return line;
        }

        public String peekLine() throws IOException {
            if (!this.mNextLineIsValid) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                String line = super.readLine();
                this.mTime += System.currentTimeMillis() - jCurrentTimeMillis;
                this.mNextLine = line;
                this.mNextLineIsValid = true;
            }
            return this.mNextLine;
        }
    }

    public VCardParserImpl_V21() {
        this(VCardConfig.VCARD_TYPE_DEFAULT);
    }

    public VCardParserImpl_V21(int i) {
        this.mInterpreterList = new ArrayList();
        this.mUnknownTypeSet = new HashSet();
        this.mUnknownValueSet = new HashSet();
        this.mIntermediateCharset = "ISO-8859-1";
    }

    protected boolean isValidPropertyName(String str) {
        if (!getKnownPropertyNameSet().contains(str.toUpperCase()) && !str.startsWith("X-") && !this.mUnknownTypeSet.contains(str)) {
            this.mUnknownTypeSet.add(str);
            Log.w("MTK_vCard", "Property name unsupported by vCard 2.1: " + str);
            return true;
        }
        return true;
    }

    protected String getLine() throws IOException {
        return this.mReader.readLine();
    }

    protected String peekLine() throws IOException {
        return this.mReader.peekLine();
    }

    protected String getNonEmptyLine() throws VCardException, IOException {
        String line;
        do {
            line = getLine();
            if (line == null) {
                throw new VCardException("Reached end of buffer.");
            }
        } while (line.trim().length() <= 0);
        return line;
    }

    private boolean parseOneVCard() throws VCardException, IOException {
        this.mCurrentEncoding = "8BIT";
        this.mCurrentCharset = "UTF-8";
        if (!readBeginVCard(true)) {
            return false;
        }
        Iterator<VCardInterpreter> it = this.mInterpreterList.iterator();
        while (it.hasNext()) {
            it.next().onEntryStarted();
        }
        parseItems();
        Iterator<VCardInterpreter> it2 = this.mInterpreterList.iterator();
        while (it2.hasNext()) {
            it2.next().onEntryEnded();
        }
        return true;
    }

    protected boolean readBeginVCard(boolean z) throws VCardException, IOException {
        while (true) {
            String line = getLine();
            if (line == null) {
                return false;
            }
            if (line.trim().length() > 0) {
                String[] strArrSplit = line.split(":", 2);
                if (strArrSplit.length == 2 && strArrSplit[0].trim().equalsIgnoreCase("BEGIN") && strArrSplit[1].trim().equalsIgnoreCase("VCARD")) {
                    return true;
                }
                if (!z) {
                    throw new VCardException("Expected String \"BEGIN:VCARD\" did not come (Instead, \"" + line + "\" came)");
                }
                if (!z) {
                    throw new VCardException("Reached where must not be reached.");
                }
            }
        }
    }

    protected void parseItems() throws VCardException, IOException {
        boolean item;
        try {
            item = parseItem();
        } catch (VCardInvalidCommentLineException e) {
            Log.e("MTK_vCard", "Invalid line which looks like some comment was found. Ignored.");
            item = false;
        }
        while (!item) {
            try {
                item = parseItem();
            } catch (VCardInvalidCommentLineException e2) {
                Log.e("MTK_vCard", "Invalid line which looks like some comment was found. Ignored.");
            }
        }
    }

    protected boolean parseItem() throws VCardException, IOException {
        this.mCurrentEncoding = "8BIT";
        VCardProperty vCardPropertyConstructPropertyData = constructPropertyData(getNonEmptyLine());
        String upperCase = vCardPropertyConstructPropertyData.getName().toUpperCase();
        String rawValue = vCardPropertyConstructPropertyData.getRawValue();
        if (upperCase.equals("BEGIN")) {
            if (rawValue.equalsIgnoreCase("VCARD")) {
                handleNest();
                return false;
            }
            throw new VCardException("Unknown BEGIN type: " + rawValue);
        }
        if (upperCase.equals("END")) {
            if (rawValue.equalsIgnoreCase("VCARD")) {
                return true;
            }
            throw new VCardException("Unknown END type: " + rawValue);
        }
        parseItemInter(vCardPropertyConstructPropertyData, upperCase);
        return false;
    }

    private void parseItemInter(VCardProperty vCardProperty, String str) throws VCardException, IOException {
        String rawValue = vCardProperty.getRawValue();
        if (str.equals("AGENT")) {
            handleAgent(vCardProperty);
            return;
        }
        if (isValidPropertyName(str)) {
            if (str.equals("VERSION") && !rawValue.equals(getVersionString())) {
                throw new VCardVersionException("Incompatible version: " + rawValue + " != " + getVersionString());
            }
            handlePropertyValue(vCardProperty, str);
            return;
        }
        throw new VCardException("Unknown property name: \"" + str + "\"");
    }

    private void handleNest() throws VCardException, IOException {
        Iterator<VCardInterpreter> it = this.mInterpreterList.iterator();
        while (it.hasNext()) {
            it.next().onEntryStarted();
        }
        parseItems();
        Iterator<VCardInterpreter> it2 = this.mInterpreterList.iterator();
        while (it2.hasNext()) {
            it2.next().onEntryEnded();
        }
    }

    protected VCardProperty constructPropertyData(String str) throws VCardException {
        VCardProperty vCardProperty = new VCardProperty();
        int length = str.length();
        int i = 0;
        if (length > 0 && str.charAt(0) == '#') {
            throw new VCardInvalidCommentLineException();
        }
        char c = 0;
        int i2 = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            switch (c) {
                case 0:
                    if (cCharAt == ':') {
                        vCardProperty.setName(str.substring(i2, i));
                        vCardProperty.setRawValue(i < length - 1 ? str.substring(i + 1) : "");
                        return vCardProperty;
                    }
                    if (cCharAt == '.') {
                        String strSubstring = str.substring(i2, i);
                        if (strSubstring.length() == 0) {
                            Log.w("MTK_vCard", "Empty group found. Ignoring.");
                        } else {
                            vCardProperty.addGroup(strSubstring);
                        }
                        i2 = i + 1;
                    } else if (cCharAt == ';') {
                        vCardProperty.setName(str.substring(i2, i));
                        i2 = i + 1;
                        break;
                    }
                    i++;
                    break;
                    break;
                case 1:
                    if (cCharAt == '\"') {
                        if ("2.1".equalsIgnoreCase(getVersionString())) {
                            Log.w("MTK_vCard", "Double-quoted params found in vCard 2.1. Silently allow it");
                        }
                        c = 2;
                        continue;
                    } else if (cCharAt == ';') {
                        handleParams(vCardProperty, str.substring(i2, i));
                        i2 = i + 1;
                    } else if (cCharAt == ':') {
                        handleParams(vCardProperty, str.substring(i2, i));
                        vCardProperty.setRawValue(i < length - 1 ? str.substring(i + 1) : "");
                        return vCardProperty;
                    }
                    i++;
                    break;
                case 2:
                    if (cCharAt != '\"') {
                        i++;
                    } else if ("2.1".equalsIgnoreCase(getVersionString())) {
                        Log.w("MTK_vCard", "Double-quoted params found in vCard 2.1. Silently allow it");
                    }
                    break;
                default:
                    i++;
                    break;
            }
            c = 1;
            i++;
        }
        throw new VCardInvalidLineException("Invalid line: \"" + str + "\"");
    }

    protected void handleParams(VCardProperty vCardProperty, String str) throws VCardException {
        String[] strArrSplit = str.split("=", 2);
        if (strArrSplit.length == 2) {
            String upperCase = strArrSplit[0].trim().toUpperCase();
            String strTrim = strArrSplit[1].trim();
            if (upperCase.equals("TYPE")) {
                handleType(vCardProperty, strTrim);
                return;
            }
            if (upperCase.equals("VALUE")) {
                handleValue(vCardProperty, strTrim);
                return;
            }
            if (upperCase.equals("ENCODING")) {
                handleEncoding(vCardProperty, strTrim.toUpperCase());
                return;
            }
            if (upperCase.equals("CHARSET")) {
                handleCharset(vCardProperty, strTrim);
                return;
            }
            if (upperCase.equals("LANGUAGE")) {
                handleLanguage(vCardProperty, strTrim);
                return;
            }
            if (upperCase.startsWith("X-")) {
                handleAnyParam(vCardProperty, upperCase, strTrim);
                return;
            }
            throw new VCardException("Unknown type \"" + upperCase + "\"");
        }
        handleParamWithoutName(vCardProperty, strArrSplit[0]);
    }

    protected void handleParamWithoutName(VCardProperty vCardProperty, String str) {
        handleType(vCardProperty, str);
    }

    protected void handleType(VCardProperty vCardProperty, String str) {
        if (!getKnownTypeSet().contains(str.toUpperCase()) && !str.startsWith("X-") && !this.mUnknownTypeSet.contains(str)) {
            this.mUnknownTypeSet.add(str);
            Log.w("MTK_vCard", String.format("TYPE unsupported by %s: ", Integer.valueOf(getVersion()), str));
        }
        vCardProperty.addParameter("TYPE", str);
    }

    protected void handleValue(VCardProperty vCardProperty, String str) {
        if (!getKnownValueSet().contains(str.toUpperCase()) && !str.startsWith("X-") && !this.mUnknownValueSet.contains(str)) {
            this.mUnknownValueSet.add(str);
            Log.w("MTK_vCard", String.format("The value unsupported by TYPE of %s: ", Integer.valueOf(getVersion()), str));
        }
        vCardProperty.addParameter("VALUE", str);
    }

    protected void handleEncoding(VCardProperty vCardProperty, String str) throws VCardException {
        if (getAvailableEncodingSet().contains(str) || str.startsWith("X-")) {
            vCardProperty.addParameter("ENCODING", str);
            this.mCurrentEncoding = str.toUpperCase();
        } else {
            throw new VCardException("Unknown encoding \"" + str + "\"");
        }
    }

    protected void handleCharset(VCardProperty vCardProperty, String str) {
        this.mCurrentCharset = str;
        vCardProperty.addParameter("CHARSET", str);
    }

    protected void handleLanguage(VCardProperty vCardProperty, String str) throws VCardException {
        String[] strArrSplit = str.split("-");
        if (strArrSplit.length != 2) {
            throw new VCardException("Invalid Language: \"" + str + "\"");
        }
        String str2 = strArrSplit[0];
        int length = str2.length();
        for (int i = 0; i < length; i++) {
            if (!isAsciiLetter(str2.charAt(i))) {
                throw new VCardException("Invalid Language: \"" + str + "\"");
            }
        }
        String str3 = strArrSplit[1];
        int length2 = str3.length();
        for (int i2 = 0; i2 < length2; i2++) {
            if (!isAsciiLetter(str3.charAt(i2))) {
                throw new VCardException("Invalid Language: \"" + str + "\"");
            }
        }
        vCardProperty.addParameter("LANGUAGE", str);
    }

    private boolean isAsciiLetter(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        return false;
    }

    protected void handleAnyParam(VCardProperty vCardProperty, String str, String str2) {
        vCardProperty.addParameter(str, str2);
    }

    protected void handlePropertyValue(VCardProperty vCardProperty, String str) throws VCardException, IOException {
        String upperCase = vCardProperty.getName().toUpperCase();
        String rawValue = vCardProperty.getRawValue();
        Collection<String> parameters = vCardProperty.getParameters("CHARSET");
        StringBuilder sb = null;
        String next = parameters != null ? parameters.iterator().next() : null;
        if (TextUtils.isEmpty(next)) {
            next = "UTF-8";
        }
        if (upperCase.equals("ADR") || upperCase.equals("ORG") || upperCase.equals("N")) {
            handleAdrOrgN(vCardProperty, rawValue, "ISO-8859-1", next);
            return;
        }
        if (this.mCurrentEncoding.equals("QUOTED-PRINTABLE") || (upperCase.equals("FN") && vCardProperty.getParameters("ENCODING") == null && VCardUtils.appearsLikeAndroidVCardQuotedPrintable(rawValue))) {
            String quotedPrintablePart = getQuotedPrintablePart(rawValue);
            String quotedPrintable = VCardUtils.parseQuotedPrintable(quotedPrintablePart, false, "ISO-8859-1", next);
            vCardProperty.setRawValue(quotedPrintablePart);
            vCardProperty.setValues(quotedPrintable);
            Iterator<VCardInterpreter> it = this.mInterpreterList.iterator();
            while (it.hasNext()) {
                it.next().onPropertyCreated(vCardProperty);
            }
            return;
        }
        if (this.mCurrentEncoding.equals("BASE64") || this.mCurrentEncoding.equals("B")) {
            try {
                try {
                    vCardProperty.setByteValue(Base64.decode(getBase64(rawValue), 0));
                    Iterator<VCardInterpreter> it2 = this.mInterpreterList.iterator();
                    while (it2.hasNext()) {
                        it2.next().onPropertyCreated(vCardProperty);
                    }
                    return;
                } catch (IllegalArgumentException e) {
                    throw new VCardException("Decode error on base64 photo: " + rawValue);
                }
            } catch (OutOfMemoryError e2) {
                Log.e("MTK_vCard", "OutOfMemoryError happened during parsing BASE64 data!");
                Iterator<VCardInterpreter> it3 = this.mInterpreterList.iterator();
                while (it3.hasNext()) {
                    it3.next().onPropertyCreated(vCardProperty);
                }
                return;
            }
        }
        if (!this.mCurrentEncoding.equals("7BIT") && !this.mCurrentEncoding.equals("8BIT") && !this.mCurrentEncoding.startsWith("X-")) {
            Log.w("MTK_vCard", String.format("The encoding \"%s\" is unsupported by vCard %s", this.mCurrentEncoding, getVersionString()));
        }
        if (getVersion() == 0) {
            while (true) {
                String strPeekLine = peekLine();
                if (TextUtils.isEmpty(strPeekLine) || strPeekLine.charAt(0) != ' ' || "END:VCARD".contains(strPeekLine.toUpperCase())) {
                    break;
                }
                getLine();
                if (sb == null) {
                    sb = new StringBuilder();
                    sb.append(rawValue);
                }
                sb.append(strPeekLine.substring(1));
            }
            if (sb != null) {
                rawValue = sb.toString();
            }
        }
        ArrayList arrayList = new ArrayList();
        arrayList.add(maybeUnescapeText(VCardUtils.convertStringCharset(rawValue, "ISO-8859-1", next)));
        vCardProperty.setValues(arrayList);
        Iterator<VCardInterpreter> it4 = this.mInterpreterList.iterator();
        while (it4.hasNext()) {
            it4.next().onPropertyCreated(vCardProperty);
        }
    }

    private void handleAdrOrgN(VCardProperty vCardProperty, String str, String str2, String str3) throws VCardException, IOException {
        ArrayList arrayList = new ArrayList();
        if (this.mCurrentEncoding.equals("QUOTED-PRINTABLE")) {
            String quotedPrintablePart = getQuotedPrintablePart(str);
            vCardProperty.setRawValue(quotedPrintablePart);
            Iterator<String> it = VCardUtils.constructListFromValue(quotedPrintablePart, getVersion()).iterator();
            while (it.hasNext()) {
                arrayList.add(VCardUtils.parseQuotedPrintable(it.next(), false, str2, str3));
            }
        } else {
            Iterator<String> it2 = VCardUtils.constructListFromValue(VCardUtils.convertStringCharset(getPotentialMultiline(str), str2, str3), getVersion()).iterator();
            while (it2.hasNext()) {
                arrayList.add(it2.next());
            }
        }
        vCardProperty.setValues(arrayList);
        Iterator<VCardInterpreter> it3 = this.mInterpreterList.iterator();
        while (it3.hasNext()) {
            it3.next().onPropertyCreated(vCardProperty);
        }
    }

    private String getQuotedPrintablePart(String str) throws VCardException, IOException {
        if (str.trim().endsWith("=")) {
            int length = str.length() - 1;
            while (str.charAt(length) != '=') {
            }
            StringBuilder sb = new StringBuilder();
            sb.append(str.substring(0, length + 1));
            sb.append("\r\n");
            while (true) {
                String line = getLine();
                if (line == null) {
                    throw new VCardException("File ended during parsing a Quoted-Printable String");
                }
                if (line.trim().endsWith("=")) {
                    int length2 = line.length() - 1;
                    while (line.charAt(length2) != '=') {
                    }
                    sb.append(line.substring(0, length2 + 1));
                    sb.append("\r\n");
                } else {
                    sb.append(line);
                    return sb.toString();
                }
            }
        } else {
            return str;
        }
    }

    private String getPotentialMultiline(String str) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        while (true) {
            String strPeekLine = peekLine();
            if (strPeekLine == null || strPeekLine.length() == 0 || getPropertyNameUpperCase(strPeekLine) != null) {
                break;
            }
            getLine();
            sb.append(" ");
            sb.append(strPeekLine);
        }
        return sb.toString();
    }

    protected String getBase64(String str) throws VCardException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        while (true) {
            String strPeekLine = peekLine();
            if (strPeekLine == null) {
                throw new VCardException("File ended during parsing BASE64 binary");
            }
            String propertyNameUpperCase = getPropertyNameUpperCase(strPeekLine);
            if (getKnownPropertyNameSet().contains(propertyNameUpperCase) || "X-ANDROID-CUSTOM".equals(propertyNameUpperCase)) {
                break;
            }
            getLine();
            if (strPeekLine.length() == 0) {
                break;
            }
            sb.append(strPeekLine.trim());
        }
        return sb.toString();
    }

    private String getPropertyNameUpperCase(String str) {
        int iIndexOf = str.indexOf(":");
        if (iIndexOf > -1) {
            int iIndexOf2 = str.indexOf(";");
            if (iIndexOf != -1) {
                if (iIndexOf2 != -1) {
                    iIndexOf = Math.min(iIndexOf, iIndexOf2);
                }
            } else {
                iIndexOf = iIndexOf2;
            }
            return str.substring(0, iIndexOf).toUpperCase();
        }
        return null;
    }

    protected void handleAgent(VCardProperty vCardProperty) throws VCardException {
        if (!vCardProperty.getRawValue().toUpperCase().contains("BEGIN:VCARD")) {
            Iterator<VCardInterpreter> it = this.mInterpreterList.iterator();
            while (it.hasNext()) {
                it.next().onPropertyCreated(vCardProperty);
            }
            return;
        }
        throw new VCardAgentNotSupportedException("AGENT Property is not supported now.");
    }

    protected String maybeUnescapeText(String str) {
        return str;
    }

    static String unescapeCharacter(char c) {
        if (c == '\\' || c == ';' || c == ':' || c == ',') {
            return String.valueOf(c);
        }
        return null;
    }

    protected int getVersion() {
        return 0;
    }

    protected String getVersionString() {
        return "2.1";
    }

    protected Set<String> getKnownPropertyNameSet() {
        return VCardParser_V21.sKnownPropertyNameSet;
    }

    protected Set<String> getKnownTypeSet() {
        return VCardParser_V21.sKnownTypeSet;
    }

    protected Set<String> getKnownValueSet() {
        return VCardParser_V21.sKnownValueSet;
    }

    protected Set<String> getAvailableEncodingSet() {
        return VCardParser_V21.sAvailableEncoding;
    }

    public void addInterpreter(VCardInterpreter vCardInterpreter) {
        this.mInterpreterList.add(vCardInterpreter);
    }

    public void parse(InputStream inputStream) throws VCardException, IOException {
        if (inputStream == null) {
            throw new NullPointerException("InputStream must not be null.");
        }
        this.mReader = new CustomBufferedReader(new InputStreamReader(inputStream, this.mIntermediateCharset));
        System.currentTimeMillis();
        Iterator<VCardInterpreter> it = this.mInterpreterList.iterator();
        while (it.hasNext()) {
            it.next().onVCardStarted();
        }
        while (true) {
            synchronized (this) {
                if (this.mCanceled) {
                    break;
                } else if (!parseOneVCard()) {
                    break;
                }
            }
        }
        Iterator<VCardInterpreter> it2 = this.mInterpreterList.iterator();
        while (it2.hasNext()) {
            it2.next().onVCardEnded();
        }
    }

    public final synchronized void cancel() {
        Log.i("MTK_vCard", "ParserImpl received cancel operation.");
        this.mCanceled = true;
    }
}
