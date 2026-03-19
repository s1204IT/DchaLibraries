package com.android.vcard;

import android.util.Log;
import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.util.Set;

class VCardParserImpl_V30 extends VCardParserImpl_V21 {
    private static final String LOG_TAG = "MTK_vCard";
    private boolean mEmittedAgentWarning;
    private String mPreviousLine;

    public VCardParserImpl_V30() {
        this.mEmittedAgentWarning = false;
    }

    public VCardParserImpl_V30(int i) {
        super(i);
        this.mEmittedAgentWarning = false;
    }

    @Override
    protected int getVersion() {
        return 1;
    }

    @Override
    protected String getVersionString() {
        return VCardConstants.VERSION_V30;
    }

    @Override
    protected String peekLine() throws IOException {
        if (this.mPreviousLine != null) {
            return this.mPreviousLine;
        }
        return this.mReader.peekLine();
    }

    @Override
    protected String getLine() throws IOException {
        if (this.mPreviousLine != null) {
            String str = this.mPreviousLine;
            this.mPreviousLine = null;
            return str;
        }
        return this.mReader.readLine();
    }

    @Override
    protected String getNonEmptyLine() throws VCardException, IOException {
        String line;
        String string = null;
        StringBuilder sb = null;
        while (true) {
            line = this.mReader.readLine();
            if (line == null) {
                break;
            }
            if (line.length() != 0) {
                if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                    if (sb == null) {
                        sb = new StringBuilder();
                    }
                    if (this.mPreviousLine != null) {
                        sb.append(this.mPreviousLine);
                        this.mPreviousLine = null;
                    }
                    sb.append(line.substring(1));
                } else {
                    if (sb != null || this.mPreviousLine != null) {
                        break;
                    }
                    this.mPreviousLine = line;
                }
            }
        }
        if (sb != null) {
            string = sb.toString();
        } else if (this.mPreviousLine != null) {
            string = this.mPreviousLine;
        }
        this.mPreviousLine = line;
        if (string == null) {
            throw new VCardException("Reached end of buffer.");
        }
        return string;
    }

    @Override
    protected boolean readBeginVCard(boolean z) throws VCardException, IOException {
        return super.readBeginVCard(z);
    }

    @Override
    protected void handleParams(VCardProperty vCardProperty, String str) throws VCardException {
        try {
            super.handleParams(vCardProperty, str);
        } catch (VCardException e) {
            String[] strArrSplit = str.split("=", 2);
            if (strArrSplit.length == 2) {
                handleAnyParam(vCardProperty, strArrSplit[0], strArrSplit[1]);
                return;
            }
            throw new VCardException("Unknown params value: " + str);
        }
    }

    @Override
    protected void handleAnyParam(VCardProperty vCardProperty, String str, String str2) {
        splitAndPutParam(vCardProperty, str, str2);
    }

    @Override
    protected void handleParamWithoutName(VCardProperty vCardProperty, String str) {
        handleType(vCardProperty, str);
    }

    @Override
    protected void handleType(VCardProperty vCardProperty, String str) {
        splitAndPutParam(vCardProperty, VCardConstants.PARAM_TYPE, str);
    }

    private void splitAndPutParam(VCardProperty vCardProperty, String str, String str2) {
        int length = str2.length();
        boolean z = false;
        StringBuilder sb = null;
        for (int i = 0; i < length; i++) {
            char cCharAt = str2.charAt(i);
            if (cCharAt == '\"') {
                if (z) {
                    vCardProperty.addParameter(str, encodeParamValue(sb.toString()));
                    z = false;
                    sb = null;
                } else {
                    if (sb != null) {
                        if (sb.length() > 0) {
                            Log.w(LOG_TAG, "Unexpected Dquote inside property.");
                        } else {
                            vCardProperty.addParameter(str, encodeParamValue(sb.toString()));
                        }
                    }
                    z = true;
                }
            } else if (cCharAt != ',' || z) {
                if (sb == null) {
                    sb = new StringBuilder();
                }
                sb.append(cCharAt);
            } else if (sb == null) {
                Log.w(LOG_TAG, "Comma is used before actual string comes. (" + str2 + ")");
            } else {
                vCardProperty.addParameter(str, encodeParamValue(sb.toString()));
                sb = null;
            }
        }
        if (z) {
            Log.d(LOG_TAG, "Dangling Dquote.");
        }
        if (sb != null) {
            if (sb.length() == 0) {
                Log.w(LOG_TAG, "Unintended behavior. We must not see empty StringBuilder at the end of parameter value parsing.");
            } else {
                vCardProperty.addParameter(str, encodeParamValue(sb.toString()));
            }
        }
    }

    protected String encodeParamValue(String str) {
        return VCardUtils.convertStringCharset(str, VCardConfig.DEFAULT_INTERMEDIATE_CHARSET, "UTF-8");
    }

    @Override
    protected void handleAgent(VCardProperty vCardProperty) {
        if (!this.mEmittedAgentWarning) {
            Log.w(LOG_TAG, "AGENT in vCard 3.0 is not supported yet. Ignore it");
            this.mEmittedAgentWarning = true;
        }
    }

    @Override
    protected String getBase64(String str) throws VCardException, IOException {
        return str;
    }

    @Override
    protected String maybeUnescapeText(String str) {
        return unescapeText(str);
    }

    public static String unescapeText(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\' && i < length - 1) {
                i++;
                char cCharAt2 = str.charAt(i);
                if (cCharAt2 == 'n' || cCharAt2 == 'N') {
                    sb.append("\n");
                } else {
                    sb.append(cCharAt2);
                }
            } else {
                sb.append(cCharAt);
            }
            i++;
        }
        return sb.toString();
    }

    @Override
    protected String maybeUnescapeCharacter(char c) {
        return unescapeCharacter(c);
    }

    public static String unescapeCharacter(char c) {
        if (c == 'n' || c == 'N') {
            return "\n";
        }
        return String.valueOf(c);
    }

    @Override
    protected Set<String> getKnownPropertyNameSet() {
        return VCardParser_V30.sKnownPropertyNameSet;
    }
}
