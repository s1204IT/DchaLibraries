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

        public CustomBufferedReader(Reader in) {
            super(in);
        }

        @Override
        public String readLine() throws IOException {
            if (this.mNextLineIsValid) {
                String ret = this.mNextLine;
                this.mNextLine = null;
                this.mNextLineIsValid = false;
                return ret;
            }
            long start = System.currentTimeMillis();
            String line = super.readLine();
            long end = System.currentTimeMillis();
            this.mTime += end - start;
            return line;
        }

        public String peekLine() throws IOException {
            if (!this.mNextLineIsValid) {
                long start = System.currentTimeMillis();
                String line = super.readLine();
                long end = System.currentTimeMillis();
                this.mTime += end - start;
                this.mNextLine = line;
                this.mNextLineIsValid = true;
            }
            return this.mNextLine;
        }
    }

    public VCardParserImpl_V21() {
        this(VCardConfig.VCARD_TYPE_DEFAULT);
    }

    public VCardParserImpl_V21(int vcardType) {
        this.mInterpreterList = new ArrayList();
        this.mUnknownTypeSet = new HashSet();
        this.mUnknownValueSet = new HashSet();
        this.mIntermediateCharset = "ISO-8859-1";
    }

    protected boolean isValidPropertyName(String propertyName) {
        if (!getKnownPropertyNameSet().contains(propertyName.toUpperCase()) && !propertyName.startsWith("X-") && !this.mUnknownTypeSet.contains(propertyName)) {
            this.mUnknownTypeSet.add(propertyName);
            Log.w("vCard", "Property name unsupported by vCard 2.1: " + propertyName);
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
        if (!readBeginVCard(false)) {
            return false;
        }
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onEntryStarted();
        }
        parseItems();
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onEntryEnded();
        }
        return true;
    }

    protected boolean readBeginVCard(boolean allowGarbage) throws VCardException, IOException {
        while (true) {
            String line = getLine();
            if (line == null) {
                return false;
            }
            if (line.trim().length() > 0) {
                String[] strArray = line.split(":", 2);
                int length = strArray.length;
                if (length == 2 && strArray[0].trim().equalsIgnoreCase("BEGIN") && strArray[1].trim().equalsIgnoreCase("VCARD")) {
                    return true;
                }
                if (!allowGarbage) {
                    throw new VCardException("Expected String \"BEGIN:VCARD\" did not come (Instead, \"" + line + "\" came)");
                }
                if (!allowGarbage) {
                    throw new VCardException("Reached where must not be reached.");
                }
            }
        }
    }

    protected void parseItems() throws VCardException, IOException {
        boolean ended = false;
        try {
            ended = parseItem();
        } catch (VCardInvalidCommentLineException e) {
            Log.e("vCard", "Invalid line which looks like some comment was found. Ignored.");
        }
        while (!ended) {
            try {
                ended = parseItem();
            } catch (VCardInvalidCommentLineException e2) {
                Log.e("vCard", "Invalid line which looks like some comment was found. Ignored.");
            }
        }
    }

    protected boolean parseItem() throws VCardException, IOException {
        this.mCurrentEncoding = "8BIT";
        String line = getNonEmptyLine();
        VCardProperty propertyData = constructPropertyData(line);
        String propertyNameUpper = propertyData.getName().toUpperCase();
        String propertyRawValue = propertyData.getRawValue();
        if (propertyNameUpper.equals("BEGIN")) {
            if (propertyRawValue.equalsIgnoreCase("VCARD")) {
                handleNest();
            } else {
                throw new VCardException("Unknown BEGIN type: " + propertyRawValue);
            }
        } else {
            if (propertyNameUpper.equals("END")) {
                if (propertyRawValue.equalsIgnoreCase("VCARD")) {
                    return true;
                }
                throw new VCardException("Unknown END type: " + propertyRawValue);
            }
            parseItemInter(propertyData, propertyNameUpper);
        }
        return false;
    }

    private void parseItemInter(VCardProperty property, String propertyNameUpper) throws VCardException, IOException {
        String propertyRawValue = property.getRawValue();
        if (propertyNameUpper.equals("AGENT")) {
            handleAgent(property);
        } else {
            if (isValidPropertyName(propertyNameUpper)) {
                if (propertyNameUpper.equals("VERSION") && !propertyRawValue.equals(getVersionString())) {
                    throw new VCardVersionException("Incompatible version: " + propertyRawValue + " != " + getVersionString());
                }
                handlePropertyValue(property, propertyNameUpper);
                return;
            }
            throw new VCardException("Unknown property name: \"" + propertyNameUpper + "\"");
        }
    }

    private void handleNest() throws VCardException, IOException {
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onEntryStarted();
        }
        parseItems();
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onEntryEnded();
        }
    }

    protected VCardProperty constructPropertyData(String line) throws VCardException {
        VCardProperty propertyData = new VCardProperty();
        int length = line.length();
        if (length > 0 && line.charAt(0) == '#') {
            throw new VCardInvalidCommentLineException();
        }
        int state = 0;
        int nameIndex = 0;
        int i = 0;
        while (i < length) {
            char ch = line.charAt(i);
            switch (state) {
                case 0:
                    if (ch == ':') {
                        String propertyName = line.substring(nameIndex, i);
                        propertyData.setName(propertyName);
                        propertyData.setRawValue(i < length + (-1) ? line.substring(i + 1) : "");
                        return propertyData;
                    }
                    if (ch == '.') {
                        String groupName = line.substring(nameIndex, i);
                        if (groupName.length() == 0) {
                            Log.w("vCard", "Empty group found. Ignoring.");
                        } else {
                            propertyData.addGroup(groupName);
                        }
                        nameIndex = i + 1;
                    } else if (ch == ';') {
                        String propertyName2 = line.substring(nameIndex, i);
                        propertyData.setName(propertyName2);
                        nameIndex = i + 1;
                        state = 1;
                    }
                    break;
                    break;
                case 1:
                    if (ch == '\"') {
                        if ("2.1".equalsIgnoreCase(getVersionString())) {
                            Log.w("vCard", "Double-quoted params found in vCard 2.1. Silently allow it");
                        }
                        state = 2;
                    } else if (ch == ';') {
                        handleParams(propertyData, line.substring(nameIndex, i));
                        nameIndex = i + 1;
                    } else {
                        if (ch == ':') {
                            handleParams(propertyData, line.substring(nameIndex, i));
                            propertyData.setRawValue(i < length + (-1) ? line.substring(i + 1) : "");
                            return propertyData;
                        }
                    }
                    break;
                case 2:
                    if (ch == '\"') {
                        if ("2.1".equalsIgnoreCase(getVersionString())) {
                            Log.w("vCard", "Double-quoted params found in vCard 2.1. Silently allow it");
                        }
                        state = 1;
                    }
                    break;
            }
            i++;
        }
        throw new VCardInvalidLineException("Invalid line: \"" + line + "\"");
    }

    protected void handleParams(VCardProperty propertyData, String params) throws VCardException {
        String[] strArray = params.split("=", 2);
        if (strArray.length == 2) {
            String paramName = strArray[0].trim().toUpperCase();
            String paramValue = strArray[1].trim();
            if (paramName.equals("TYPE")) {
                handleType(propertyData, paramValue);
                return;
            }
            if (paramName.equals("VALUE")) {
                handleValue(propertyData, paramValue);
                return;
            }
            if (paramName.equals("ENCODING")) {
                handleEncoding(propertyData, paramValue.toUpperCase());
                return;
            }
            if (paramName.equals("CHARSET")) {
                handleCharset(propertyData, paramValue);
                return;
            } else if (paramName.equals("LANGUAGE")) {
                handleLanguage(propertyData, paramValue);
                return;
            } else {
                if (paramName.startsWith("X-")) {
                    handleAnyParam(propertyData, paramName, paramValue);
                    return;
                }
                throw new VCardException("Unknown type \"" + paramName + "\"");
            }
        }
        handleParamWithoutName(propertyData, strArray[0]);
    }

    protected void handleParamWithoutName(VCardProperty propertyData, String paramValue) {
        handleType(propertyData, paramValue);
    }

    protected void handleType(VCardProperty propertyData, String ptypeval) {
        if (!getKnownTypeSet().contains(ptypeval.toUpperCase()) && !ptypeval.startsWith("X-") && !this.mUnknownTypeSet.contains(ptypeval)) {
            this.mUnknownTypeSet.add(ptypeval);
            Log.w("vCard", String.format("TYPE unsupported by %s: ", Integer.valueOf(getVersion()), ptypeval));
        }
        propertyData.addParameter("TYPE", ptypeval);
    }

    protected void handleValue(VCardProperty propertyData, String pvalueval) {
        if (!getKnownValueSet().contains(pvalueval.toUpperCase()) && !pvalueval.startsWith("X-") && !this.mUnknownValueSet.contains(pvalueval)) {
            this.mUnknownValueSet.add(pvalueval);
            Log.w("vCard", String.format("The value unsupported by TYPE of %s: ", Integer.valueOf(getVersion()), pvalueval));
        }
        propertyData.addParameter("VALUE", pvalueval);
    }

    protected void handleEncoding(VCardProperty propertyData, String pencodingval) throws VCardException {
        if (getAvailableEncodingSet().contains(pencodingval) || pencodingval.startsWith("X-")) {
            propertyData.addParameter("ENCODING", pencodingval);
            this.mCurrentEncoding = pencodingval.toUpperCase();
            return;
        }
        throw new VCardException("Unknown encoding \"" + pencodingval + "\"");
    }

    protected void handleCharset(VCardProperty propertyData, String charsetval) {
        this.mCurrentCharset = charsetval;
        propertyData.addParameter("CHARSET", charsetval);
    }

    protected void handleLanguage(VCardProperty propertyData, String langval) throws VCardException {
        String[] strArray = langval.split("-");
        if (strArray.length != 2) {
            throw new VCardException("Invalid Language: \"" + langval + "\"");
        }
        String tmp = strArray[0];
        int length = tmp.length();
        for (int i = 0; i < length; i++) {
            if (!isAsciiLetter(tmp.charAt(i))) {
                throw new VCardException("Invalid Language: \"" + langval + "\"");
            }
        }
        String tmp2 = strArray[1];
        int length2 = tmp2.length();
        for (int i2 = 0; i2 < length2; i2++) {
            if (!isAsciiLetter(tmp2.charAt(i2))) {
                throw new VCardException("Invalid Language: \"" + langval + "\"");
            }
        }
        propertyData.addParameter("LANGUAGE", langval);
    }

    private boolean isAsciiLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
    }

    protected void handleAnyParam(VCardProperty propertyData, String paramName, String paramValue) {
        propertyData.addParameter(paramName, paramValue);
    }

    protected void handlePropertyValue(VCardProperty property, String propertyName) throws VCardException, IOException {
        String propertyNameUpper = property.getName().toUpperCase();
        String propertyRawValue = property.getRawValue();
        Collection<String> charsetCollection = property.getParameters("CHARSET");
        String targetCharset = charsetCollection != null ? charsetCollection.iterator().next() : null;
        if (TextUtils.isEmpty(targetCharset)) {
            targetCharset = "UTF-8";
        }
        if (propertyNameUpper.equals("ADR") || propertyNameUpper.equals("ORG") || propertyNameUpper.equals("N")) {
            handleAdrOrgN(property, propertyRawValue, "ISO-8859-1", targetCharset);
            return;
        }
        if (this.mCurrentEncoding.equals("QUOTED-PRINTABLE") || (propertyNameUpper.equals("FN") && property.getParameters("ENCODING") == null && VCardUtils.appearsLikeAndroidVCardQuotedPrintable(propertyRawValue))) {
            String quotedPrintablePart = getQuotedPrintablePart(propertyRawValue);
            String propertyEncodedValue = VCardUtils.parseQuotedPrintable(quotedPrintablePart, false, "ISO-8859-1", targetCharset);
            property.setRawValue(quotedPrintablePart);
            property.setValues(propertyEncodedValue);
            for (VCardInterpreter interpreter : this.mInterpreterList) {
                interpreter.onPropertyCreated(property);
            }
            return;
        }
        if (this.mCurrentEncoding.equals("BASE64") || this.mCurrentEncoding.equals("B")) {
            try {
                String base64Property = getBase64(propertyRawValue);
                try {
                    property.setByteValue(Base64.decode(base64Property, 0));
                    for (VCardInterpreter interpreter2 : this.mInterpreterList) {
                        interpreter2.onPropertyCreated(property);
                    }
                    return;
                } catch (IllegalArgumentException e) {
                    throw new VCardException("Decode error on base64 photo: " + propertyRawValue);
                }
            } catch (OutOfMemoryError e2) {
                Log.e("vCard", "OutOfMemoryError happened during parsing BASE64 data!");
                for (VCardInterpreter interpreter3 : this.mInterpreterList) {
                    interpreter3.onPropertyCreated(property);
                }
                return;
            }
        }
        if (!this.mCurrentEncoding.equals("7BIT") && !this.mCurrentEncoding.equals("8BIT") && !this.mCurrentEncoding.startsWith("X-")) {
            Log.w("vCard", String.format("The encoding \"%s\" is unsupported by vCard %s", this.mCurrentEncoding, getVersionString()));
        }
        if (getVersion() == 0) {
            StringBuilder builder = null;
            while (true) {
                String nextLine = peekLine();
                if (TextUtils.isEmpty(nextLine) || nextLine.charAt(0) != ' ' || "END:VCARD".contains(nextLine.toUpperCase())) {
                    break;
                }
                getLine();
                if (builder == null) {
                    builder = new StringBuilder();
                    builder.append(propertyRawValue);
                }
                builder.append(nextLine.substring(1));
            }
            if (builder != null) {
                propertyRawValue = builder.toString();
            }
        }
        ArrayList<String> propertyValueList = new ArrayList<>();
        String value = VCardUtils.convertStringCharset(maybeUnescapeText(propertyRawValue), "ISO-8859-1", targetCharset);
        propertyValueList.add(value);
        property.setValues(propertyValueList);
        for (VCardInterpreter interpreter4 : this.mInterpreterList) {
            interpreter4.onPropertyCreated(property);
        }
    }

    private void handleAdrOrgN(VCardProperty property, String propertyRawValue, String sourceCharset, String targetCharset) throws VCardException, IOException {
        List<String> encodedValueList = new ArrayList<>();
        if (this.mCurrentEncoding.equals("QUOTED-PRINTABLE")) {
            String quotedPrintablePart = getQuotedPrintablePart(propertyRawValue);
            property.setRawValue(quotedPrintablePart);
            List<String> quotedPrintableValueList = VCardUtils.constructListFromValue(quotedPrintablePart, getVersion());
            for (String quotedPrintableValue : quotedPrintableValueList) {
                String encoded = VCardUtils.parseQuotedPrintable(quotedPrintableValue, false, sourceCharset, targetCharset);
                encodedValueList.add(encoded);
            }
        } else {
            String propertyValue = getPotentialMultiline(propertyRawValue);
            List<String> rawValueList = VCardUtils.constructListFromValue(propertyValue, getVersion());
            for (String rawValue : rawValueList) {
                encodedValueList.add(VCardUtils.convertStringCharset(rawValue, sourceCharset, targetCharset));
            }
        }
        property.setValues(encodedValueList);
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onPropertyCreated(property);
        }
    }

    private String getQuotedPrintablePart(String firstString) throws VCardException, IOException {
        if (firstString.trim().endsWith("=")) {
            int pos = firstString.length() - 1;
            while (firstString.charAt(pos) != '=') {
            }
            StringBuilder builder = new StringBuilder();
            builder.append(firstString.substring(0, pos + 1));
            builder.append("\r\n");
            while (true) {
                String line = getLine();
                if (line == null) {
                    throw new VCardException("File ended during parsing a Quoted-Printable String");
                }
                if (line.trim().endsWith("=")) {
                    int pos2 = line.length() - 1;
                    while (line.charAt(pos2) != '=') {
                    }
                    builder.append(line.substring(0, pos2 + 1));
                    builder.append("\r\n");
                } else {
                    builder.append(line);
                    return builder.toString();
                }
            }
        } else {
            return firstString;
        }
    }

    private String getPotentialMultiline(String firstString) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(firstString);
        while (true) {
            String line = peekLine();
            if (line != null && line.length() != 0) {
                String propertyName = getPropertyNameUpperCase(line);
                if (propertyName != null) {
                    break;
                }
                getLine();
                builder.append(" ").append(line);
            } else {
                break;
            }
        }
        return builder.toString();
    }

    protected String getBase64(String firstString) throws VCardException, IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(firstString);
        while (true) {
            String line = peekLine();
            if (line == null) {
                throw new VCardException("File ended during parsing BASE64 binary");
            }
            String propertyName = getPropertyNameUpperCase(line);
            if (!getKnownPropertyNameSet().contains(propertyName) && !"X-ANDROID-CUSTOM".equals(propertyName)) {
                getLine();
                if (line.length() == 0) {
                    break;
                }
                builder.append(line.trim());
            } else {
                break;
            }
        }
        return builder.toString();
    }

    private String getPropertyNameUpperCase(String line) {
        int minIndex;
        int colonIndex = line.indexOf(":");
        if (colonIndex > -1) {
            int semiColonIndex = line.indexOf(";");
            if (colonIndex == -1) {
                minIndex = semiColonIndex;
            } else if (semiColonIndex == -1) {
                minIndex = colonIndex;
            } else {
                minIndex = Math.min(colonIndex, semiColonIndex);
            }
            return line.substring(0, minIndex).toUpperCase();
        }
        return null;
    }

    protected void handleAgent(VCardProperty property) throws VCardException {
        if (!property.getRawValue().toUpperCase().contains("BEGIN:VCARD")) {
            for (VCardInterpreter interpreter : this.mInterpreterList) {
                interpreter.onPropertyCreated(property);
            }
            return;
        }
        throw new VCardAgentNotSupportedException("AGENT Property is not supported now.");
    }

    protected String maybeUnescapeText(String text) {
        return text;
    }

    static String unescapeCharacter(char ch) {
        if (ch == '\\' || ch == ';' || ch == ':' || ch == ',') {
            return String.valueOf(ch);
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

    public void addInterpreter(VCardInterpreter interpreter) {
        this.mInterpreterList.add(interpreter);
    }

    public void parse(InputStream is) throws VCardException, IOException {
        if (is == null) {
            throw new NullPointerException("InputStream must not be null.");
        }
        InputStreamReader tmpReader = new InputStreamReader(is, this.mIntermediateCharset);
        this.mReader = new CustomBufferedReader(tmpReader);
        System.currentTimeMillis();
        for (VCardInterpreter interpreter : this.mInterpreterList) {
            interpreter.onVCardStarted();
        }
        while (true) {
            synchronized (this) {
                if (!this.mCanceled) {
                    if (!parseOneVCard()) {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        for (VCardInterpreter interpreter2 : this.mInterpreterList) {
            interpreter2.onVCardEnded();
        }
    }

    public final synchronized void cancel() {
        Log.i("vCard", "ParserImpl received cancel operation.");
        this.mCanceled = true;
    }
}
