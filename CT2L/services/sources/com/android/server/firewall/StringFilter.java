package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.PatternMatcher;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.IOException;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

abstract class StringFilter implements Filter {
    private static final String ATTR_CONTAINS = "contains";
    private static final String ATTR_EQUALS = "equals";
    private static final String ATTR_IS_NULL = "isNull";
    private static final String ATTR_PATTERN = "pattern";
    private static final String ATTR_REGEX = "regex";
    private static final String ATTR_STARTS_WITH = "startsWith";
    private final ValueProvider mValueProvider;
    public static final ValueProvider COMPONENT = new ValueProvider("component") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            if (resolvedComponent != null) {
                return resolvedComponent.flattenToString();
            }
            return null;
        }
    };
    public static final ValueProvider COMPONENT_NAME = new ValueProvider("component-name") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            if (resolvedComponent != null) {
                return resolvedComponent.getClassName();
            }
            return null;
        }
    };
    public static final ValueProvider COMPONENT_PACKAGE = new ValueProvider("component-package") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            if (resolvedComponent != null) {
                return resolvedComponent.getPackageName();
            }
            return null;
        }
    };
    public static final FilterFactory ACTION = new ValueProvider("action") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            return intent.getAction();
        }
    };
    public static final ValueProvider DATA = new ValueProvider(DatabaseHelper.SoundModelContract.KEY_DATA) {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            Uri data = intent.getData();
            if (data != null) {
                return data.toString();
            }
            return null;
        }
    };
    public static final ValueProvider MIME_TYPE = new ValueProvider("mime-type") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            return resolvedType;
        }
    };
    public static final ValueProvider SCHEME = new ValueProvider("scheme") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getScheme();
            }
            return null;
        }
    };
    public static final ValueProvider SSP = new ValueProvider("scheme-specific-part") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getSchemeSpecificPart();
            }
            return null;
        }
    };
    public static final ValueProvider HOST = new ValueProvider("host") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getHost();
            }
            return null;
        }
    };
    public static final ValueProvider PATH = new ValueProvider("path") {
        @Override
        public String getValue(ComponentName resolvedComponent, Intent intent, String resolvedType) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getPath();
            }
            return null;
        }
    };

    protected abstract boolean matchesValue(String str);

    private StringFilter(ValueProvider valueProvider) {
        this.mValueProvider = valueProvider;
    }

    public static StringFilter readFromXml(ValueProvider valueProvider, XmlPullParser parser) throws XmlPullParserException, IOException {
        StringFilter filter = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            StringFilter newFilter = getFilter(valueProvider, parser, i);
            if (newFilter != null) {
                if (filter != null) {
                    throw new XmlPullParserException("Multiple string filter attributes found");
                }
                filter = newFilter;
            }
        }
        if (filter == null) {
            StringFilter filter2 = new IsNullFilter(valueProvider, false);
            return filter2;
        }
        return filter;
    }

    private static StringFilter getFilter(ValueProvider valueProvider, XmlPullParser parser, int attributeIndex) {
        String attributeName = parser.getAttributeName(attributeIndex);
        switch (attributeName.charAt(0)) {
            case HdmiCecKeycode.CEC_KEYCODE_PAUSE_RECORD_FUNCTION:
                if (attributeName.equals(ATTR_CONTAINS)) {
                }
                break;
            case 'e':
                if (attributeName.equals(ATTR_EQUALS)) {
                }
                break;
            case HdmiCecKeycode.CEC_KEYCODE_SELECT_AV_INPUT_FUNCTION:
                if (attributeName.equals(ATTR_IS_NULL)) {
                }
                break;
            case 'p':
                if (attributeName.equals(ATTR_PATTERN)) {
                }
                break;
            case 'r':
                if (attributeName.equals(ATTR_REGEX)) {
                }
                break;
            case HdmiCecKeycode.CEC_KEYCODE_F3_GREEN:
                if (attributeName.equals(ATTR_STARTS_WITH)) {
                }
                break;
        }
        return null;
    }

    @Override
    public boolean matches(IntentFirewall ifw, ComponentName resolvedComponent, Intent intent, int callerUid, int callerPid, String resolvedType, int receivingUid) {
        String value = this.mValueProvider.getValue(resolvedComponent, intent, resolvedType);
        return matchesValue(value);
    }

    private static abstract class ValueProvider extends FilterFactory {
        public abstract String getValue(ComponentName componentName, Intent intent, String str);

        protected ValueProvider(String tag) {
            super(tag);
        }

        @Override
        public Filter newFilter(XmlPullParser parser) throws XmlPullParserException, IOException {
            return StringFilter.readFromXml(this, parser);
        }
    }

    private static class EqualsFilter extends StringFilter {
        private final String mFilterValue;

        public EqualsFilter(ValueProvider valueProvider, String attrValue) {
            super(valueProvider);
            this.mFilterValue = attrValue;
        }

        @Override
        public boolean matchesValue(String value) {
            return value != null && value.equals(this.mFilterValue);
        }
    }

    private static class ContainsFilter extends StringFilter {
        private final String mFilterValue;

        public ContainsFilter(ValueProvider valueProvider, String attrValue) {
            super(valueProvider);
            this.mFilterValue = attrValue;
        }

        @Override
        public boolean matchesValue(String value) {
            return value != null && value.contains(this.mFilterValue);
        }
    }

    private static class StartsWithFilter extends StringFilter {
        private final String mFilterValue;

        public StartsWithFilter(ValueProvider valueProvider, String attrValue) {
            super(valueProvider);
            this.mFilterValue = attrValue;
        }

        @Override
        public boolean matchesValue(String value) {
            return value != null && value.startsWith(this.mFilterValue);
        }
    }

    private static class PatternStringFilter extends StringFilter {
        private final PatternMatcher mPattern;

        public PatternStringFilter(ValueProvider valueProvider, String attrValue) {
            super(valueProvider);
            this.mPattern = new PatternMatcher(attrValue, 2);
        }

        @Override
        public boolean matchesValue(String value) {
            return value != null && this.mPattern.match(value);
        }
    }

    private static class RegexFilter extends StringFilter {
        private final Pattern mPattern;

        public RegexFilter(ValueProvider valueProvider, String attrValue) {
            super(valueProvider);
            this.mPattern = Pattern.compile(attrValue);
        }

        @Override
        public boolean matchesValue(String value) {
            return value != null && this.mPattern.matcher(value).matches();
        }
    }

    private static class IsNullFilter extends StringFilter {
        private final boolean mIsNull;

        public IsNullFilter(ValueProvider valueProvider, String attrValue) {
            super(valueProvider);
            this.mIsNull = Boolean.parseBoolean(attrValue);
        }

        public IsNullFilter(ValueProvider valueProvider, boolean isNull) {
            super(valueProvider);
            this.mIsNull = isNull;
        }

        @Override
        public boolean matchesValue(String value) {
            return (value == null) == this.mIsNull;
        }
    }
}
