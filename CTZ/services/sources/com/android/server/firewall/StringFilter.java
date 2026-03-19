package com.android.server.firewall;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.PatternMatcher;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
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
        public String getValue(ComponentName componentName, Intent intent, String str) {
            if (componentName != null) {
                return componentName.flattenToString();
            }
            return null;
        }
    };
    public static final ValueProvider COMPONENT_NAME = new ValueProvider("component-name") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            if (componentName != null) {
                return componentName.getClassName();
            }
            return null;
        }
    };
    public static final ValueProvider COMPONENT_PACKAGE = new ValueProvider("component-package") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            if (componentName != null) {
                return componentName.getPackageName();
            }
            return null;
        }
    };
    public static final FilterFactory ACTION = new ValueProvider("action") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            return intent.getAction();
        }
    };
    public static final ValueProvider DATA = new ValueProvider("data") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            Uri data = intent.getData();
            if (data != null) {
                return data.toString();
            }
            return null;
        }
    };
    public static final ValueProvider MIME_TYPE = new ValueProvider("mime-type") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            return str;
        }
    };
    public static final ValueProvider SCHEME = new ValueProvider("scheme") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getScheme();
            }
            return null;
        }
    };
    public static final ValueProvider SSP = new ValueProvider("scheme-specific-part") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getSchemeSpecificPart();
            }
            return null;
        }
    };
    public static final ValueProvider HOST = new ValueProvider(WatchlistLoggingHandler.WatchlistEventKeys.HOST) {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
            Uri data = intent.getData();
            if (data != null) {
                return data.getHost();
            }
            return null;
        }
    };
    public static final ValueProvider PATH = new ValueProvider("path") {
        @Override
        public String getValue(ComponentName componentName, Intent intent, String str) {
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

    public static StringFilter readFromXml(ValueProvider valueProvider, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        StringFilter stringFilter = null;
        for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
            StringFilter filter = getFilter(valueProvider, xmlPullParser, i);
            if (filter != null) {
                if (stringFilter == null) {
                    stringFilter = filter;
                } else {
                    throw new XmlPullParserException("Multiple string filter attributes found");
                }
            }
        }
        if (stringFilter == null) {
            return new IsNullFilter(valueProvider, false);
        }
        return stringFilter;
    }

    private static StringFilter getFilter(ValueProvider valueProvider, XmlPullParser xmlPullParser, int i) {
        String attributeName = xmlPullParser.getAttributeName(i);
        char cCharAt = attributeName.charAt(0);
        if (cCharAt == 'c') {
            if (attributeName.equals(ATTR_CONTAINS)) {
                return new ContainsFilter(valueProvider, xmlPullParser.getAttributeValue(i));
            }
            return null;
        }
        if (cCharAt == 'e') {
            if (attributeName.equals(ATTR_EQUALS)) {
                return new EqualsFilter(valueProvider, xmlPullParser.getAttributeValue(i));
            }
            return null;
        }
        if (cCharAt == 'i') {
            if (attributeName.equals(ATTR_IS_NULL)) {
                return new IsNullFilter(valueProvider, xmlPullParser.getAttributeValue(i));
            }
            return null;
        }
        if (cCharAt == 'p') {
            if (attributeName.equals(ATTR_PATTERN)) {
                return new PatternStringFilter(valueProvider, xmlPullParser.getAttributeValue(i));
            }
            return null;
        }
        switch (cCharAt) {
            case 'r':
                if (attributeName.equals(ATTR_REGEX)) {
                    break;
                }
                break;
            case HdmiCecKeycode.CEC_KEYCODE_F3_GREEN:
                if (attributeName.equals(ATTR_STARTS_WITH)) {
                    break;
                }
                break;
        }
        return null;
    }

    @Override
    public boolean matches(IntentFirewall intentFirewall, ComponentName componentName, Intent intent, int i, int i2, String str, int i3) {
        return matchesValue(this.mValueProvider.getValue(componentName, intent, str));
    }

    private static abstract class ValueProvider extends FilterFactory {
        public abstract String getValue(ComponentName componentName, Intent intent, String str);

        protected ValueProvider(String str) {
            super(str);
        }

        @Override
        public Filter newFilter(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            return StringFilter.readFromXml(this, xmlPullParser);
        }
    }

    private static class EqualsFilter extends StringFilter {
        private final String mFilterValue;

        public EqualsFilter(ValueProvider valueProvider, String str) {
            super(valueProvider);
            this.mFilterValue = str;
        }

        @Override
        public boolean matchesValue(String str) {
            return str != null && str.equals(this.mFilterValue);
        }
    }

    private static class ContainsFilter extends StringFilter {
        private final String mFilterValue;

        public ContainsFilter(ValueProvider valueProvider, String str) {
            super(valueProvider);
            this.mFilterValue = str;
        }

        @Override
        public boolean matchesValue(String str) {
            return str != null && str.contains(this.mFilterValue);
        }
    }

    private static class StartsWithFilter extends StringFilter {
        private final String mFilterValue;

        public StartsWithFilter(ValueProvider valueProvider, String str) {
            super(valueProvider);
            this.mFilterValue = str;
        }

        @Override
        public boolean matchesValue(String str) {
            return str != null && str.startsWith(this.mFilterValue);
        }
    }

    private static class PatternStringFilter extends StringFilter {
        private final PatternMatcher mPattern;

        public PatternStringFilter(ValueProvider valueProvider, String str) {
            super(valueProvider);
            this.mPattern = new PatternMatcher(str, 2);
        }

        @Override
        public boolean matchesValue(String str) {
            return str != null && this.mPattern.match(str);
        }
    }

    private static class RegexFilter extends StringFilter {
        private final Pattern mPattern;

        public RegexFilter(ValueProvider valueProvider, String str) {
            super(valueProvider);
            this.mPattern = Pattern.compile(str);
        }

        @Override
        public boolean matchesValue(String str) {
            return str != null && this.mPattern.matcher(str).matches();
        }
    }

    private static class IsNullFilter extends StringFilter {
        private final boolean mIsNull;

        public IsNullFilter(ValueProvider valueProvider, String str) {
            super(valueProvider);
            this.mIsNull = Boolean.parseBoolean(str);
        }

        public IsNullFilter(ValueProvider valueProvider, boolean z) {
            super(valueProvider);
            this.mIsNull = z;
        }

        @Override
        public boolean matchesValue(String str) {
            return (str == null) == this.mIsNull;
        }
    }
}
