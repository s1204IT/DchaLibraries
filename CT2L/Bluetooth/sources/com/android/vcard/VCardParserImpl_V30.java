package com.android.vcard;

import android.util.Log;
import com.android.vcard.exception.VCardException;
import java.io.IOException;
import java.util.Set;

class VCardParserImpl_V30 extends VCardParserImpl_V21 {
    private static final String LOG_TAG = "vCard";
    private boolean mEmittedAgentWarning;
    private String mPreviousLine;

    public VCardParserImpl_V30() {
        this.mEmittedAgentWarning = false;
    }

    public VCardParserImpl_V30(int vcardType) {
        super(vcardType);
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
    protected String getLine() throws IOException {
        if (this.mPreviousLine != null) {
            String ret = this.mPreviousLine;
            this.mPreviousLine = null;
            return ret;
        }
        String ret2 = this.mReader.readLine();
        return ret2;
    }

    @Override
    protected String getNonEmptyLine() throws VCardException, IOException {
        String line;
        StringBuilder builder = null;
        while (true) {
            line = this.mReader.readLine();
            if (line == null) {
                break;
            }
            if (line.length() != 0) {
                if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                    if (builder == null) {
                        builder = new StringBuilder();
                    }
                    if (this.mPreviousLine != null) {
                        builder.append(this.mPreviousLine);
                        this.mPreviousLine = null;
                    }
                    builder.append(line.substring(1));
                } else {
                    if (builder != null || this.mPreviousLine != null) {
                        break;
                    }
                    this.mPreviousLine = line;
                }
            }
        }
        String ret = null;
        if (builder != null) {
            ret = builder.toString();
        } else if (this.mPreviousLine != null) {
            ret = this.mPreviousLine;
        }
        this.mPreviousLine = line;
        if (ret == null) {
            throw new VCardException("Reached end of buffer.");
        }
        return ret;
    }

    @Override
    protected boolean readBeginVCard(boolean allowGarbage) throws VCardException, IOException {
        return super.readBeginVCard(allowGarbage);
    }

    @Override
    protected void handleParams(VCardProperty propertyData, String params) throws VCardException {
        try {
            super.handleParams(propertyData, params);
        } catch (VCardException e) {
            String[] strArray = params.split("=", 2);
            if (strArray.length == 2) {
                handleAnyParam(propertyData, strArray[0], strArray[1]);
                return;
            }
            throw new VCardException("Unknown params value: " + params);
        }
    }

    @Override
    protected void handleAnyParam(VCardProperty propertyData, String paramName, String paramValue) {
        splitAndPutParam(propertyData, paramName, paramValue);
    }

    @Override
    protected void handleParamWithoutName(VCardProperty property, String paramValue) {
        handleType(property, paramValue);
    }

    @Override
    protected void handleType(VCardProperty property, String paramValue) {
        splitAndPutParam(property, VCardConstants.PARAM_TYPE, paramValue);
    }

    private void splitAndPutParam(VCardProperty property, String paramName, String paramValue) {
        StringBuilder builder = null;
        boolean insideDquote = false;
        int length = paramValue.length();
        for (int i = 0; i < length; i++) {
            char ch = paramValue.charAt(i);
            if (ch == '\"') {
                if (insideDquote) {
                    property.addParameter(paramName, encodeParamValue(builder.toString()));
                    builder = null;
                    insideDquote = false;
                } else {
                    if (builder != null) {
                        if (builder.length() > 0) {
                            Log.w(LOG_TAG, "Unexpected Dquote inside property.");
                        } else {
                            property.addParameter(paramName, encodeParamValue(builder.toString()));
                        }
                    }
                    insideDquote = true;
                }
            } else if (ch == ',' && !insideDquote) {
                if (builder == null) {
                    Log.w(LOG_TAG, "Comma is used before actual string comes. (" + paramValue + ")");
                } else {
                    property.addParameter(paramName, encodeParamValue(builder.toString()));
                    builder = null;
                }
            } else {
                if (builder == null) {
                    builder = new StringBuilder();
                }
                builder.append(ch);
            }
        }
        if (insideDquote) {
            Log.d(LOG_TAG, "Dangling Dquote.");
        }
        if (builder != null) {
            if (builder.length() == 0) {
                Log.w(LOG_TAG, "Unintended behavior. We must not see empty StringBuilder at the end of parameter value parsing.");
            } else {
                property.addParameter(paramName, encodeParamValue(builder.toString()));
            }
        }
    }

    protected String encodeParamValue(String paramValue) {
        return VCardUtils.convertStringCharset(paramValue, VCardConfig.DEFAULT_INTERMEDIATE_CHARSET, "UTF-8");
    }

    @Override
    protected void handleAgent(VCardProperty property) {
        if (!this.mEmittedAgentWarning) {
            Log.w(LOG_TAG, "AGENT in vCard 3.0 is not supported yet. Ignore it");
            this.mEmittedAgentWarning = true;
        }
    }

    @Override
    protected String getBase64(String firstString) throws VCardException, IOException {
        return firstString;
    }

    @Override
    protected String maybeUnescapeText(String text) {
        return unescapeText(text);
    }

    public static String unescapeText(String text) {
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        int i = 0;
        while (i < length) {
            char ch = text.charAt(i);
            if (ch == '\\' && i < length - 1) {
                i++;
                char next_ch = text.charAt(i);
                if (next_ch == 'n' || next_ch == 'N') {
                    builder.append("\n");
                } else {
                    builder.append(next_ch);
                }
            } else {
                builder.append(ch);
            }
            i++;
        }
        return builder.toString();
    }

    @Override
    protected String maybeUnescapeCharacter(char ch) {
        return unescapeCharacter(ch);
    }

    public static String unescapeCharacter(char ch) {
        return (ch == 'n' || ch == 'N') ? "\n" : String.valueOf(ch);
    }

    @Override
    protected Set<String> getKnownPropertyNameSet() {
        return VCardParser_V30.sKnownPropertyNameSet;
    }
}
