package com.mediatek.vcalendar;

import com.android.common.speech.LoggingEvents;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.component.ComponentFactory;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.parameter.ParameterFactory;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.property.PropertyFactory;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.utils.Utility;

public class ComponentParser {
    private static final String TAG = "ComponentParser";

    private ComponentParser() {
    }

    private static final class ParserState {
        public int mIndex;
        public String mLine;

        private ParserState() {
        }
    }

    public static Component parse(String str) {
        return parseComponentImpl(null, normalizeText(str));
    }

    private static String normalizeText(String str) {
        String strReplaceAll = str.replaceAll(Component.NEWLINE, "\n").replaceAll("\r", "\n");
        if (Utility.needQpEncode()) {
            strReplaceAll = strReplaceAll.replaceAll("=\n", LoggingEvents.EXTRA_CALLING_APP_NAME);
        }
        String strReplaceAll2 = strReplaceAll.replaceAll("\n\t", LoggingEvents.EXTRA_CALLING_APP_NAME).replaceAll("\n ", LoggingEvents.EXTRA_CALLING_APP_NAME);
        LogUtil.d(TAG, "normalizeText(): normalized text: \n" + strReplaceAll2);
        return strReplaceAll2;
    }

    private static Component parseComponentImpl(Component component, String str) {
        ParserState parserState = new ParserState();
        parserState.mIndex = 0;
        Component component2 = component;
        for (String str2 : str.split("\n")) {
            try {
                Component line = parseLine(str2, parserState, component2);
                if (component == null) {
                    component = line;
                }
                component2 = line;
            } catch (VCalendarException e) {
                LogUtil.e(TAG, "parseComponentImpl(): Can NOT parse line: " + str2, e);
            }
        }
        return component;
    }

    private static Component parseLine(String str, ParserState parserState, Component component) throws VCalendarException {
        parserState.mLine = str;
        int length = parserState.mLine.length();
        parserState.mIndex = 0;
        char cCharAt = 0;
        while (parserState.mIndex < length && (cCharAt = str.charAt(parserState.mIndex)) != ';' && cCharAt != ':') {
            parserState.mIndex++;
        }
        String strSubstring = str.substring(0, parserState.mIndex);
        if (component == null && !Component.BEGIN.equals(strSubstring)) {
            throw new VCalendarException("parseLine(): Expected \"BEGIN\" but NOT.");
        }
        if (Component.BEGIN.equals(strSubstring)) {
            Component componentCreateComponent = ComponentFactory.createComponent(extractValue(parserState), component);
            if (component != null) {
                component.addChild(componentCreateComponent);
            }
            return componentCreateComponent;
        }
        if (Component.END.equals(strSubstring)) {
            String strExtractValue = extractValue(parserState);
            if (component == null || strExtractValue == null || !strExtractValue.equals(component.getName())) {
                throw new VCalendarException("parseLine(): Unexpected \"END\" in component: " + strExtractValue);
            }
            return component.getParent();
        }
        Property propertyCreateProperty = PropertyFactory.createProperty(strSubstring, null);
        if (cCharAt == ';') {
            while (true) {
                Parameter parameterExtractParameter = extractParameter(parserState);
                if (parameterExtractParameter == null) {
                    break;
                }
                propertyCreateProperty.addParameter(parameterExtractParameter);
            }
        }
        String strExtractValue2 = extractValue(parserState);
        if (strExtractValue2 != null) {
            LogUtil.d(TAG, "parseLine(): property value = " + strExtractValue2);
            propertyCreateProperty.setValue(strExtractValue2, propertyCreateProperty.getFirstParameter(Parameter.ENCODING));
            component.addProperty(propertyCreateProperty);
            LogUtil.d(TAG, "parseLine(): \"" + propertyCreateProperty.getName() + "\" added to component:\"" + component.getName() + "\"");
        }
        return component;
    }

    private static String extractValue(ParserState parserState) throws VCalendarException {
        String str = parserState.mLine;
        if (parserState.mIndex >= str.length() || str.charAt(parserState.mIndex) != ':') {
            LogUtil.d(TAG, "extractValue(): Expect ':' before end of line in: " + str);
            return null;
        }
        String strSubstring = str.substring(parserState.mIndex + 1);
        parserState.mIndex = str.length() - 1;
        return strSubstring;
    }

    private static Parameter extractParameter(ParserState parserState) throws VCalendarException {
        String str = parserState.mLine;
        int length = str.length();
        Parameter parameter = null;
        int i = -1;
        int i2 = -1;
        while (parserState.mIndex < length) {
            char cCharAt = str.charAt(parserState.mIndex);
            if (cCharAt == ':') {
                if (parameter != null) {
                    if (i == -1) {
                        throw new VCalendarException("extractParameter(): Expected '=' within parameter in " + str);
                    }
                    parameter.setValue(str.substring(i + 1, parserState.mIndex));
                }
                return parameter;
            }
            if (cCharAt == ';') {
                if (parameter == null) {
                    i2 = parserState.mIndex;
                } else {
                    if (i == -1) {
                        throw new VCalendarException("extractParameter(): Expected '=' within parameter in " + str);
                    }
                    parameter.setValue(str.substring(i + 1, parserState.mIndex));
                    return parameter;
                }
            } else if (cCharAt == '=') {
                if (parameter == null && i2 != -1) {
                    int i3 = parserState.mIndex;
                    Parameter parameterCreateParameter = ParameterFactory.createParameter(str.substring(i2 + 1, i3), null);
                    i2 = -1;
                    i = i3;
                    parameter = parameterCreateParameter;
                } else {
                    LogUtil.e(TAG, "extractParameter(): FormatException happened, Expected one ';' before one '=' in " + str);
                }
            } else if (cCharAt == '\"') {
                if (parameter == null) {
                    throw new VCalendarException("extractParameter(): Expected parameter before '\"' in " + str);
                }
                if (i == -1) {
                    throw new VCalendarException("extractParameter(): Expected '=' within parameter in " + str);
                }
                if (parserState.mIndex <= i + 1) {
                    int iIndexOf = str.indexOf(34, parserState.mIndex + 1);
                    if (iIndexOf < 0) {
                        throw new VCalendarException("extractParameter(): Expected closing '\"' in " + str);
                    }
                    parameter.setValue(str.substring(parserState.mIndex + 1, iIndexOf));
                    parserState.mIndex = iIndexOf + 1;
                    return parameter;
                }
                throw new VCalendarException("extractParameter(): Parameter value cannot contain a '\"' in " + str);
            }
            parserState.mIndex++;
        }
        throw new VCalendarException("extractParameter(): Expected ':' before end of line in " + str);
    }
}
