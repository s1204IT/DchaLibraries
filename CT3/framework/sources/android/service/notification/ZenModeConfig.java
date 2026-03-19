package android.service.notification;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.net.ProxyInfo;
import android.net.Uri;
import android.nfc.cardemulation.CardEmulation;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeConfig implements Parcelable {
    private static final String ALLOW_ATT_CALLS = "calls";
    private static final String ALLOW_ATT_CALLS_FROM = "callsFrom";
    private static final String ALLOW_ATT_EVENTS = "events";
    private static final String ALLOW_ATT_FROM = "from";
    private static final String ALLOW_ATT_MESSAGES = "messages";
    private static final String ALLOW_ATT_MESSAGES_FROM = "messagesFrom";
    private static final String ALLOW_ATT_REMINDERS = "reminders";
    private static final String ALLOW_ATT_REPEAT_CALLERS = "repeatCallers";
    private static final String ALLOW_ATT_SCREEN_OFF = "visualScreenOff";
    private static final String ALLOW_ATT_SCREEN_ON = "visualScreenOn";
    private static final String ALLOW_TAG = "allow";
    private static final String AUTOMATIC_TAG = "automatic";
    private static final String CONDITION_ATT_COMPONENT = "component";
    private static final String CONDITION_ATT_FLAGS = "flags";
    private static final String CONDITION_ATT_ICON = "icon";
    private static final String CONDITION_ATT_ID = "id";
    private static final String CONDITION_ATT_LINE1 = "line1";
    private static final String CONDITION_ATT_LINE2 = "line2";
    private static final String CONDITION_ATT_STATE = "state";
    private static final String CONDITION_ATT_SUMMARY = "summary";
    private static final String CONDITION_TAG = "condition";
    public static final String COUNTDOWN_PATH = "countdown";
    private static final int DAY_MINUTES = 1440;
    private static final boolean DEFAULT_ALLOW_CALLS = true;
    private static final boolean DEFAULT_ALLOW_EVENTS = true;
    private static final boolean DEFAULT_ALLOW_MESSAGES = false;
    private static final boolean DEFAULT_ALLOW_REMINDERS = true;
    private static final boolean DEFAULT_ALLOW_REPEAT_CALLERS = false;
    private static final boolean DEFAULT_ALLOW_SCREEN_OFF = true;
    private static final boolean DEFAULT_ALLOW_SCREEN_ON = true;
    private static final int DEFAULT_SOURCE = 1;
    public static final String EVENT_PATH = "event";
    private static final String MANUAL_TAG = "manual";
    public static final int MAX_SOURCE = 2;
    private static final int MINUTES_MS = 60000;
    private static final String RULE_ATT_COMPONENT = "component";
    private static final String RULE_ATT_CONDITION_ID = "conditionId";
    private static final String RULE_ATT_CREATION_TIME = "creationTime";
    private static final String RULE_ATT_ENABLED = "enabled";
    private static final String RULE_ATT_ID = "ruleId";
    private static final String RULE_ATT_NAME = "name";
    private static final String RULE_ATT_SNOOZING = "snoozing";
    private static final String RULE_ATT_ZEN = "zen";
    public static final String SCHEDULE_PATH = "schedule";
    private static final int SECONDS_MS = 1000;
    public static final int SOURCE_ANYONE = 0;
    public static final int SOURCE_CONTACT = 1;
    public static final int SOURCE_STAR = 2;
    public static final String SYSTEM_AUTHORITY = "android";
    private static final int XML_VERSION = 2;
    private static final String ZEN_ATT_USER = "user";
    private static final String ZEN_ATT_VERSION = "version";
    private static final String ZEN_TAG = "zen";
    private static final int ZERO_VALUE_MS = 10000;
    public boolean allowCalls;
    public int allowCallsFrom;
    public boolean allowEvents;
    public boolean allowMessages;
    public int allowMessagesFrom;
    public boolean allowReminders;
    public boolean allowRepeatCallers;
    public boolean allowWhenScreenOff;
    public boolean allowWhenScreenOn;
    public ArrayMap<String, ZenRule> automaticRules;
    public ZenRule manualRule;
    public int user;
    private static String TAG = "ZenModeConfig";
    public static final int[] ALL_DAYS = {1, 2, 3, 4, 5, 6, 7};
    public static final int[] WEEKNIGHT_DAYS = {1, 2, 3, 4, 5};
    public static final int[] WEEKEND_DAYS = {6, 7};
    public static final int[] MINUTE_BUCKETS = generateMinuteBuckets();
    public static final Parcelable.Creator<ZenModeConfig> CREATOR = new Parcelable.Creator<ZenModeConfig>() {
        @Override
        public ZenModeConfig createFromParcel(Parcel source) {
            return new ZenModeConfig(source);
        }

        @Override
        public ZenModeConfig[] newArray(int size) {
            return new ZenModeConfig[size];
        }
    };

    public interface Migration {
        ZenModeConfig migrate(XmlV1 xmlV1);
    }

    public ZenModeConfig() {
        this.allowCalls = true;
        this.allowRepeatCallers = false;
        this.allowMessages = false;
        this.allowReminders = true;
        this.allowEvents = true;
        this.allowCallsFrom = 1;
        this.allowMessagesFrom = 1;
        this.user = 0;
        this.allowWhenScreenOff = true;
        this.allowWhenScreenOn = true;
        this.automaticRules = new ArrayMap<>();
    }

    public ZenModeConfig(Parcel source) {
        this.allowCalls = true;
        this.allowRepeatCallers = false;
        this.allowMessages = false;
        this.allowReminders = true;
        this.allowEvents = true;
        this.allowCallsFrom = 1;
        this.allowMessagesFrom = 1;
        this.user = 0;
        this.allowWhenScreenOff = true;
        this.allowWhenScreenOn = true;
        this.automaticRules = new ArrayMap<>();
        this.allowCalls = source.readInt() == 1;
        this.allowRepeatCallers = source.readInt() == 1;
        this.allowMessages = source.readInt() == 1;
        this.allowReminders = source.readInt() == 1;
        this.allowEvents = source.readInt() == 1;
        this.allowCallsFrom = source.readInt();
        this.allowMessagesFrom = source.readInt();
        this.user = source.readInt();
        this.manualRule = (ZenRule) source.readParcelable(null);
        int len = source.readInt();
        if (len > 0) {
            String[] ids = new String[len];
            ZenRule[] rules = new ZenRule[len];
            source.readStringArray(ids);
            source.readTypedArray(rules, ZenRule.CREATOR);
            for (int i = 0; i < len; i++) {
                this.automaticRules.put(ids[i], rules[i]);
            }
        }
        this.allowWhenScreenOff = source.readInt() == 1;
        this.allowWhenScreenOn = source.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.allowCalls ? 1 : 0);
        dest.writeInt(this.allowRepeatCallers ? 1 : 0);
        dest.writeInt(this.allowMessages ? 1 : 0);
        dest.writeInt(this.allowReminders ? 1 : 0);
        dest.writeInt(this.allowEvents ? 1 : 0);
        dest.writeInt(this.allowCallsFrom);
        dest.writeInt(this.allowMessagesFrom);
        dest.writeInt(this.user);
        dest.writeParcelable(this.manualRule, 0);
        if (!this.automaticRules.isEmpty()) {
            int len = this.automaticRules.size();
            String[] ids = new String[len];
            ZenRule[] rules = new ZenRule[len];
            for (int i = 0; i < len; i++) {
                ids[i] = this.automaticRules.keyAt(i);
                rules[i] = this.automaticRules.valueAt(i);
            }
            dest.writeInt(len);
            dest.writeStringArray(ids);
            dest.writeTypedArray(rules, 0);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.allowWhenScreenOff ? 1 : 0);
        dest.writeInt(this.allowWhenScreenOn ? 1 : 0);
    }

    public String toString() {
        return ZenModeConfig.class.getSimpleName() + "[user=" + this.user + ",allowCalls=" + this.allowCalls + ",allowRepeatCallers=" + this.allowRepeatCallers + ",allowMessages=" + this.allowMessages + ",allowCallsFrom=" + sourceToString(this.allowCallsFrom) + ",allowMessagesFrom=" + sourceToString(this.allowMessagesFrom) + ",allowReminders=" + this.allowReminders + ",allowEvents=" + this.allowEvents + ",allowWhenScreenOff=" + this.allowWhenScreenOff + ",allowWhenScreenOn=" + this.allowWhenScreenOn + ",automaticRules=" + this.automaticRules + ",manualRule=" + this.manualRule + ']';
    }

    private Diff diff(ZenModeConfig to) {
        Diff d = new Diff();
        if (to == null) {
            return d.addLine("config", "delete");
        }
        if (this.user != to.user) {
            d.addLine("user", Integer.valueOf(this.user), Integer.valueOf(to.user));
        }
        if (this.allowCalls != to.allowCalls) {
            d.addLine("allowCalls", Boolean.valueOf(this.allowCalls), Boolean.valueOf(to.allowCalls));
        }
        if (this.allowRepeatCallers != to.allowRepeatCallers) {
            d.addLine("allowRepeatCallers", Boolean.valueOf(this.allowRepeatCallers), Boolean.valueOf(to.allowRepeatCallers));
        }
        if (this.allowMessages != to.allowMessages) {
            d.addLine("allowMessages", Boolean.valueOf(this.allowMessages), Boolean.valueOf(to.allowMessages));
        }
        if (this.allowCallsFrom != to.allowCallsFrom) {
            d.addLine("allowCallsFrom", Integer.valueOf(this.allowCallsFrom), Integer.valueOf(to.allowCallsFrom));
        }
        if (this.allowMessagesFrom != to.allowMessagesFrom) {
            d.addLine("allowMessagesFrom", Integer.valueOf(this.allowMessagesFrom), Integer.valueOf(to.allowMessagesFrom));
        }
        if (this.allowReminders != to.allowReminders) {
            d.addLine("allowReminders", Boolean.valueOf(this.allowReminders), Boolean.valueOf(to.allowReminders));
        }
        if (this.allowEvents != to.allowEvents) {
            d.addLine("allowEvents", Boolean.valueOf(this.allowEvents), Boolean.valueOf(to.allowEvents));
        }
        if (this.allowWhenScreenOff != to.allowWhenScreenOff) {
            d.addLine("allowWhenScreenOff", Boolean.valueOf(this.allowWhenScreenOff), Boolean.valueOf(to.allowWhenScreenOff));
        }
        if (this.allowWhenScreenOn != to.allowWhenScreenOn) {
            d.addLine("allowWhenScreenOn", Boolean.valueOf(this.allowWhenScreenOn), Boolean.valueOf(to.allowWhenScreenOn));
        }
        ArraySet<String> allRules = new ArraySet<>();
        addKeys(allRules, this.automaticRules);
        addKeys(allRules, to.automaticRules);
        int N = allRules.size();
        for (int i = 0; i < N; i++) {
            String rule = allRules.valueAt(i);
            ZenRule.appendDiff(d, "automaticRule[" + rule + "]", this.automaticRules != null ? this.automaticRules.get(rule) : null, to.automaticRules != null ? to.automaticRules.get(rule) : null);
        }
        ZenRule.appendDiff(d, "manualRule", this.manualRule, to.manualRule);
        return d;
    }

    public static Diff diff(ZenModeConfig from, ZenModeConfig to) {
        if (from == null) {
            Diff d = new Diff();
            if (to != null) {
                d.addLine("config", "insert");
            }
            return d;
        }
        return from.diff(to);
    }

    private static <T> void addKeys(ArraySet<T> set, ArrayMap<T, ?> map) {
        if (map == null) {
            return;
        }
        for (int i = 0; i < map.size(); i++) {
            set.add(map.keyAt(i));
        }
    }

    public boolean isValid() {
        if (!isValidManualRule(this.manualRule)) {
            return false;
        }
        int N = this.automaticRules.size();
        for (int i = 0; i < N; i++) {
            if (!isValidAutomaticRule(this.automaticRules.valueAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidManualRule(ZenRule rule) {
        if (rule == null) {
            return true;
        }
        if (Settings.Global.isValidZenMode(rule.zenMode)) {
            return sameCondition(rule);
        }
        return false;
    }

    private static boolean isValidAutomaticRule(ZenRule rule) {
        if (rule == null || TextUtils.isEmpty(rule.name) || !Settings.Global.isValidZenMode(rule.zenMode) || rule.conditionId == null) {
            return false;
        }
        return sameCondition(rule);
    }

    private static boolean sameCondition(ZenRule rule) {
        if (rule == null) {
            return false;
        }
        if (rule.conditionId == null) {
            return rule.condition == null;
        }
        if (rule.condition != null) {
            return rule.conditionId.equals(rule.condition.id);
        }
        return true;
    }

    private static int[] generateMinuteBuckets() {
        int[] buckets = new int[15];
        buckets[0] = 15;
        buckets[1] = 30;
        buckets[2] = 45;
        for (int i = 1; i <= 12; i++) {
            buckets[i + 2] = i * 60;
        }
        return buckets;
    }

    public static String sourceToString(int source) {
        switch (source) {
            case 0:
                return "anyone";
            case 1:
                return Contacts.AUTHORITY;
            case 2:
                return "stars";
            default:
                return "UNKNOWN";
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof ZenModeConfig)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        ZenModeConfig other = (ZenModeConfig) o;
        if (other.allowCalls == this.allowCalls && other.allowRepeatCallers == this.allowRepeatCallers && other.allowMessages == this.allowMessages && other.allowCallsFrom == this.allowCallsFrom && other.allowMessagesFrom == this.allowMessagesFrom && other.allowReminders == this.allowReminders && other.allowEvents == this.allowEvents && other.allowWhenScreenOff == this.allowWhenScreenOff && other.allowWhenScreenOn == this.allowWhenScreenOn && other.user == this.user && Objects.equals(other.automaticRules, this.automaticRules)) {
            return Objects.equals(other.manualRule, this.manualRule);
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.allowCalls), Boolean.valueOf(this.allowRepeatCallers), Boolean.valueOf(this.allowMessages), Integer.valueOf(this.allowCallsFrom), Integer.valueOf(this.allowMessagesFrom), Boolean.valueOf(this.allowReminders), Boolean.valueOf(this.allowEvents), Boolean.valueOf(this.allowWhenScreenOff), Boolean.valueOf(this.allowWhenScreenOn), Integer.valueOf(this.user), this.automaticRules, this.manualRule);
    }

    private static String toDayList(int[] days) {
        if (days == null || days.length == 0) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(days[i]);
        }
        return sb.toString();
    }

    private static int[] tryParseDayList(String dayList, String sep) {
        if (dayList == null) {
            return null;
        }
        String[] tokens = dayList.split(sep);
        if (tokens.length == 0) {
            return null;
        }
        int[] rt = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            int day = tryParseInt(tokens[i], -1);
            if (day == -1) {
                return null;
            }
            rt[i] = day;
        }
        return rt;
    }

    private static int tryParseInt(String value, int defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    private static long tryParseLong(String value, long defValue) {
        if (TextUtils.isEmpty(value)) {
            return defValue;
        }
        try {
            return Long.valueOf(value).longValue();
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    public static ZenModeConfig readXml(XmlPullParser parser, Migration migration) throws XmlPullParserException, IOException {
        if (parser.getEventType() != 2 || !"zen".equals(parser.getName())) {
            return null;
        }
        ZenModeConfig rt = new ZenModeConfig();
        int version = safeInt(parser, "version", 2);
        if (version == 1) {
            XmlV1 v1 = XmlV1.readXml(parser);
            return migration.migrate(v1);
        }
        rt.user = safeInt(parser, "user", rt.user);
        while (true) {
            int type = parser.next();
            if (type != 1) {
                String tag = parser.getName();
                if (type == 3 && "zen".equals(tag)) {
                    return rt;
                }
                if (type == 2) {
                    if (ALLOW_TAG.equals(tag)) {
                        rt.allowCalls = safeBoolean(parser, ALLOW_ATT_CALLS, false);
                        rt.allowRepeatCallers = safeBoolean(parser, ALLOW_ATT_REPEAT_CALLERS, false);
                        rt.allowMessages = safeBoolean(parser, ALLOW_ATT_MESSAGES, false);
                        rt.allowReminders = safeBoolean(parser, ALLOW_ATT_REMINDERS, true);
                        rt.allowEvents = safeBoolean(parser, ALLOW_ATT_EVENTS, true);
                        int from = safeInt(parser, ALLOW_ATT_FROM, -1);
                        int callsFrom = safeInt(parser, ALLOW_ATT_CALLS_FROM, -1);
                        int messagesFrom = safeInt(parser, ALLOW_ATT_MESSAGES_FROM, -1);
                        if (isValidSource(callsFrom) && isValidSource(messagesFrom)) {
                            rt.allowCallsFrom = callsFrom;
                            rt.allowMessagesFrom = messagesFrom;
                        } else if (isValidSource(from)) {
                            Slog.i(TAG, "Migrating existing shared 'from': " + sourceToString(from));
                            rt.allowCallsFrom = from;
                            rt.allowMessagesFrom = from;
                        } else {
                            rt.allowCallsFrom = 1;
                            rt.allowMessagesFrom = 1;
                        }
                        rt.allowWhenScreenOff = safeBoolean(parser, ALLOW_ATT_SCREEN_OFF, true);
                        rt.allowWhenScreenOn = safeBoolean(parser, ALLOW_ATT_SCREEN_ON, true);
                    } else if ("manual".equals(tag)) {
                        rt.manualRule = readRuleXml(parser);
                    } else if (AUTOMATIC_TAG.equals(tag)) {
                        String id = parser.getAttributeValue(null, RULE_ATT_ID);
                        ZenRule automaticRule = readRuleXml(parser);
                        if (id != null && automaticRule != null) {
                            automaticRule.id = id;
                            rt.automaticRules.put(id, automaticRule);
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Failed to reach END_DOCUMENT");
            }
        }
    }

    public void writeXml(XmlSerializer out) throws IOException {
        out.startTag(null, "zen");
        out.attribute(null, "version", Integer.toString(2));
        out.attribute(null, "user", Integer.toString(this.user));
        out.startTag(null, ALLOW_TAG);
        out.attribute(null, ALLOW_ATT_CALLS, Boolean.toString(this.allowCalls));
        out.attribute(null, ALLOW_ATT_REPEAT_CALLERS, Boolean.toString(this.allowRepeatCallers));
        out.attribute(null, ALLOW_ATT_MESSAGES, Boolean.toString(this.allowMessages));
        out.attribute(null, ALLOW_ATT_REMINDERS, Boolean.toString(this.allowReminders));
        out.attribute(null, ALLOW_ATT_EVENTS, Boolean.toString(this.allowEvents));
        out.attribute(null, ALLOW_ATT_CALLS_FROM, Integer.toString(this.allowCallsFrom));
        out.attribute(null, ALLOW_ATT_MESSAGES_FROM, Integer.toString(this.allowMessagesFrom));
        out.attribute(null, ALLOW_ATT_SCREEN_OFF, Boolean.toString(this.allowWhenScreenOff));
        out.attribute(null, ALLOW_ATT_SCREEN_ON, Boolean.toString(this.allowWhenScreenOn));
        out.endTag(null, ALLOW_TAG);
        if (this.manualRule != null) {
            out.startTag(null, "manual");
            writeRuleXml(this.manualRule, out);
            out.endTag(null, "manual");
        }
        int N = this.automaticRules.size();
        for (int i = 0; i < N; i++) {
            String id = this.automaticRules.keyAt(i);
            ZenRule automaticRule = this.automaticRules.valueAt(i);
            out.startTag(null, AUTOMATIC_TAG);
            out.attribute(null, RULE_ATT_ID, id);
            writeRuleXml(automaticRule, out);
            out.endTag(null, AUTOMATIC_TAG);
        }
        out.endTag(null, "zen");
    }

    public static ZenRule readRuleXml(XmlPullParser parser) {
        ZenRule rt = new ZenRule();
        rt.enabled = safeBoolean(parser, "enabled", true);
        rt.snoozing = safeBoolean(parser, RULE_ATT_SNOOZING, false);
        rt.name = parser.getAttributeValue(null, "name");
        String zen = parser.getAttributeValue(null, "zen");
        rt.zenMode = tryParseZenMode(zen, -1);
        if (rt.zenMode == -1) {
            Slog.w(TAG, "Bad zen mode in rule xml:" + zen);
            return null;
        }
        rt.conditionId = safeUri(parser, RULE_ATT_CONDITION_ID);
        rt.component = safeComponentName(parser, CardEmulation.EXTRA_SERVICE_COMPONENT);
        rt.creationTime = safeLong(parser, "creationTime", 0L);
        rt.condition = readConditionXml(parser);
        return rt;
    }

    public static void writeRuleXml(ZenRule rule, XmlSerializer out) throws IOException {
        out.attribute(null, "enabled", Boolean.toString(rule.enabled));
        out.attribute(null, RULE_ATT_SNOOZING, Boolean.toString(rule.snoozing));
        if (rule.name != null) {
            out.attribute(null, "name", rule.name);
        }
        out.attribute(null, "zen", Integer.toString(rule.zenMode));
        if (rule.component != null) {
            out.attribute(null, CardEmulation.EXTRA_SERVICE_COMPONENT, rule.component.flattenToString());
        }
        if (rule.conditionId != null) {
            out.attribute(null, RULE_ATT_CONDITION_ID, rule.conditionId.toString());
        }
        out.attribute(null, "creationTime", Long.toString(rule.creationTime));
        if (rule.condition == null) {
            return;
        }
        writeConditionXml(rule.condition, out);
    }

    public static Condition readConditionXml(XmlPullParser parser) {
        Uri id = safeUri(parser, "id");
        if (id == null) {
            return null;
        }
        String summary = parser.getAttributeValue(null, "summary");
        String line1 = parser.getAttributeValue(null, CONDITION_ATT_LINE1);
        String line2 = parser.getAttributeValue(null, CONDITION_ATT_LINE2);
        int icon = safeInt(parser, "icon", -1);
        int state = safeInt(parser, "state", -1);
        int flags = safeInt(parser, "flags", -1);
        try {
            return new Condition(id, summary, line1, line2, icon, state, flags);
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Unable to read condition xml", e);
            return null;
        }
    }

    public static void writeConditionXml(Condition c, XmlSerializer out) throws IOException {
        out.attribute(null, "id", c.id.toString());
        out.attribute(null, "summary", c.summary);
        out.attribute(null, CONDITION_ATT_LINE1, c.line1);
        out.attribute(null, CONDITION_ATT_LINE2, c.line2);
        out.attribute(null, "icon", Integer.toString(c.icon));
        out.attribute(null, "state", Integer.toString(c.state));
        out.attribute(null, "flags", Integer.toString(c.flags));
    }

    public static boolean isValidHour(int val) {
        return val >= 0 && val < 24;
    }

    public static boolean isValidMinute(int val) {
        return val >= 0 && val < 60;
    }

    private static boolean isValidSource(int source) {
        return source >= 0 && source <= 2;
    }

    private static boolean safeBoolean(XmlPullParser parser, String att, boolean defValue) {
        String val = parser.getAttributeValue(null, att);
        return safeBoolean(val, defValue);
    }

    private static boolean safeBoolean(String val, boolean defValue) {
        return TextUtils.isEmpty(val) ? defValue : Boolean.valueOf(val).booleanValue();
    }

    private static int safeInt(XmlPullParser parser, String att, int defValue) {
        String val = parser.getAttributeValue(null, att);
        return tryParseInt(val, defValue);
    }

    private static ComponentName safeComponentName(XmlPullParser parser, String att) {
        String val = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return ComponentName.unflattenFromString(val);
    }

    private static Uri safeUri(XmlPullParser parser, String att) {
        String val = parser.getAttributeValue(null, att);
        if (TextUtils.isEmpty(val)) {
            return null;
        }
        return Uri.parse(val);
    }

    private static long safeLong(XmlPullParser parser, String att, long defValue) {
        String val = parser.getAttributeValue(null, att);
        return tryParseLong(val, defValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ZenModeConfig copy() {
        Parcel parcel = Parcel.obtain();
        try {
            writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return new ZenModeConfig(parcel);
        } finally {
            parcel.recycle();
        }
    }

    public NotificationManager.Policy toNotificationPolicy() {
        int priorityCategories = 0;
        if (this.allowCalls) {
            priorityCategories = 8;
        }
        if (this.allowMessages) {
            priorityCategories |= 4;
        }
        if (this.allowEvents) {
            priorityCategories |= 2;
        }
        if (this.allowReminders) {
            priorityCategories |= 1;
        }
        if (this.allowRepeatCallers) {
            priorityCategories |= 16;
        }
        int suppressedVisualEffects = 0;
        if (!this.allowWhenScreenOff) {
            suppressedVisualEffects = 1;
        }
        if (!this.allowWhenScreenOn) {
            suppressedVisualEffects |= 2;
        }
        int priorityCallSenders = sourceToPrioritySenders(this.allowCallsFrom, 1);
        int priorityMessageSenders = sourceToPrioritySenders(this.allowMessagesFrom, 1);
        return new NotificationManager.Policy(priorityCategories, priorityCallSenders, priorityMessageSenders, suppressedVisualEffects);
    }

    private static int sourceToPrioritySenders(int source, int def) {
        switch (source) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return def;
        }
    }

    private static int prioritySendersToSource(int prioritySenders, int def) {
        switch (prioritySenders) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return def;
        }
    }

    public void applyNotificationPolicy(NotificationManager.Policy policy) {
        if (policy == null) {
            return;
        }
        this.allowCalls = (policy.priorityCategories & 8) != 0;
        this.allowMessages = (policy.priorityCategories & 4) != 0;
        this.allowEvents = (policy.priorityCategories & 2) != 0;
        this.allowReminders = (policy.priorityCategories & 1) != 0;
        this.allowRepeatCallers = (policy.priorityCategories & 16) != 0;
        this.allowCallsFrom = prioritySendersToSource(policy.priorityCallSenders, this.allowCallsFrom);
        this.allowMessagesFrom = prioritySendersToSource(policy.priorityMessageSenders, this.allowMessagesFrom);
        if (policy.suppressedVisualEffects == -1) {
            return;
        }
        this.allowWhenScreenOff = (policy.suppressedVisualEffects & 1) == 0;
        this.allowWhenScreenOn = (policy.suppressedVisualEffects & 2) == 0;
    }

    public static Condition toTimeCondition(Context context, int minutesFromNow, int userHandle) {
        return toTimeCondition(context, minutesFromNow, userHandle, false);
    }

    public static Condition toTimeCondition(Context context, int minutesFromNow, int userHandle, boolean shortVersion) {
        long now = System.currentTimeMillis();
        long millis = minutesFromNow == 0 ? 10000 : MINUTES_MS * minutesFromNow;
        return toTimeCondition(context, now + millis, minutesFromNow, userHandle, shortVersion);
    }

    public static Condition toTimeCondition(Context context, long time, int minutes, int userHandle, boolean shortVersion) {
        String line2;
        String line1;
        String summary;
        CharSequence formattedTime = getFormattedTime(context, time, userHandle);
        Resources res = context.getResources();
        if (minutes < 60) {
            int summaryResId = shortVersion ? 18087964 : 18087963;
            summary = res.getQuantityString(summaryResId, minutes, Integer.valueOf(minutes), formattedTime);
            int line1ResId = shortVersion ? 18087968 : 18087967;
            line1 = res.getQuantityString(line1ResId, minutes, Integer.valueOf(minutes), formattedTime);
            line2 = res.getString(17040817, formattedTime);
        } else if (minutes < DAY_MINUTES) {
            int num = Math.round(minutes / 60.0f);
            int summaryResId2 = shortVersion ? 18087966 : 18087965;
            summary = res.getQuantityString(summaryResId2, num, Integer.valueOf(num), formattedTime);
            int line1ResId2 = shortVersion ? 18087970 : 18087969;
            line1 = res.getQuantityString(line1ResId2, num, Integer.valueOf(num), formattedTime);
            line2 = res.getString(17040817, formattedTime);
        } else {
            line2 = res.getString(17040817, formattedTime);
            line1 = line2;
            summary = line2;
        }
        Uri id = toCountdownConditionId(time);
        return new Condition(id, summary, line1, line2, 0, 1, 1);
    }

    public static Condition toNextAlarmCondition(Context context, long now, long alarm, int userHandle) {
        CharSequence formattedTime = getFormattedTime(context, alarm, userHandle);
        Resources res = context.getResources();
        String line1 = res.getString(17040818, formattedTime);
        Uri id = toCountdownConditionId(alarm);
        return new Condition(id, ProxyInfo.LOCAL_EXCL_LIST, line1, ProxyInfo.LOCAL_EXCL_LIST, 0, 1, 1);
    }

    private static CharSequence getFormattedTime(Context context, long time, int userHandle) {
        String skeleton = "EEE " + (DateFormat.is24HourFormat(context, userHandle) ? "Hm" : "hma");
        GregorianCalendar now = new GregorianCalendar();
        GregorianCalendar endTime = new GregorianCalendar();
        endTime.setTimeInMillis(time);
        if (now.get(1) == endTime.get(1) && now.get(2) == endTime.get(2) && now.get(5) == endTime.get(5)) {
            skeleton = DateFormat.is24HourFormat(context, userHandle) ? "Hm" : "hma";
        }
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, time);
    }

    public static Uri toCountdownConditionId(long time) {
        return new Uri.Builder().scheme("condition").authority(SYSTEM_AUTHORITY).appendPath(COUNTDOWN_PATH).appendPath(Long.toString(time)).build();
    }

    public static long tryParseCountdownConditionId(Uri conditionId) {
        if (!Condition.isValidId(conditionId, SYSTEM_AUTHORITY) || conditionId.getPathSegments().size() != 2 || !COUNTDOWN_PATH.equals(conditionId.getPathSegments().get(0))) {
            return 0L;
        }
        try {
            return Long.parseLong(conditionId.getPathSegments().get(1));
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error parsing countdown condition: " + conditionId, e);
            return 0L;
        }
    }

    public static boolean isValidCountdownConditionId(Uri conditionId) {
        return tryParseCountdownConditionId(conditionId) != 0;
    }

    public static Uri toScheduleConditionId(ScheduleInfo schedule) {
        return new Uri.Builder().scheme("condition").authority(SYSTEM_AUTHORITY).appendPath(SCHEDULE_PATH).appendQueryParameter("days", toDayList(schedule.days)).appendQueryParameter("start", schedule.startHour + "." + schedule.startMinute).appendQueryParameter("end", schedule.endHour + "." + schedule.endMinute).appendQueryParameter("exitAtAlarm", String.valueOf(schedule.exitAtAlarm)).build();
    }

    public static boolean isValidScheduleConditionId(Uri conditionId) {
        return tryParseScheduleConditionId(conditionId) != null;
    }

    public static ScheduleInfo tryParseScheduleConditionId(Uri conditionId) {
        boolean isSchedule = (conditionId != null && conditionId.getScheme().equals("condition") && conditionId.getAuthority().equals(SYSTEM_AUTHORITY) && conditionId.getPathSegments().size() == 1) ? conditionId.getPathSegments().get(0).equals(SCHEDULE_PATH) : false;
        if (!isSchedule) {
            return null;
        }
        int[] start = tryParseHourAndMinute(conditionId.getQueryParameter("start"));
        int[] end = tryParseHourAndMinute(conditionId.getQueryParameter("end"));
        if (start == null || end == null) {
            return null;
        }
        ScheduleInfo rt = new ScheduleInfo();
        rt.days = tryParseDayList(conditionId.getQueryParameter("days"), "\\.");
        rt.startHour = start[0];
        rt.startMinute = start[1];
        rt.endHour = end[0];
        rt.endMinute = end[1];
        rt.exitAtAlarm = safeBoolean(conditionId.getQueryParameter("exitAtAlarm"), false);
        return rt;
    }

    public static ComponentName getScheduleConditionProvider() {
        return new ComponentName(SYSTEM_AUTHORITY, "ScheduleConditionProvider");
    }

    public static class ScheduleInfo {
        public int[] days;
        public int endHour;
        public int endMinute;
        public boolean exitAtAlarm;
        public long nextAlarm;
        public int startHour;
        public int startMinute;

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            if (!(o instanceof ScheduleInfo)) {
                return false;
            }
            ScheduleInfo other = (ScheduleInfo) o;
            return ZenModeConfig.toDayList(this.days).equals(ZenModeConfig.toDayList(other.days)) && this.startHour == other.startHour && this.startMinute == other.startMinute && this.endHour == other.endHour && this.endMinute == other.endMinute && this.exitAtAlarm == other.exitAtAlarm;
        }

        public ScheduleInfo copy() {
            ScheduleInfo rt = new ScheduleInfo();
            if (this.days != null) {
                rt.days = new int[this.days.length];
                System.arraycopy(this.days, 0, rt.days, 0, this.days.length);
            }
            rt.startHour = this.startHour;
            rt.startMinute = this.startMinute;
            rt.endHour = this.endHour;
            rt.endMinute = this.endMinute;
            rt.exitAtAlarm = this.exitAtAlarm;
            rt.nextAlarm = this.nextAlarm;
            return rt;
        }

        public String toString() {
            return "ScheduleInfo{days=" + Arrays.toString(this.days) + ", startHour=" + this.startHour + ", startMinute=" + this.startMinute + ", endHour=" + this.endHour + ", endMinute=" + this.endMinute + ", exitAtAlarm=" + this.exitAtAlarm + ", nextAlarm=" + this.nextAlarm + '}';
        }
    }

    public static Uri toEventConditionId(EventInfo event) {
        return new Uri.Builder().scheme("condition").authority(SYSTEM_AUTHORITY).appendPath("event").appendQueryParameter("userId", Long.toString(event.userId)).appendQueryParameter("calendar", event.calendar != null ? event.calendar : ProxyInfo.LOCAL_EXCL_LIST).appendQueryParameter("reply", Integer.toString(event.reply)).build();
    }

    public static boolean isValidEventConditionId(Uri conditionId) {
        return tryParseEventConditionId(conditionId) != null;
    }

    public static EventInfo tryParseEventConditionId(Uri conditionId) {
        boolean isEvent = (conditionId != null && conditionId.getScheme().equals("condition") && conditionId.getAuthority().equals(SYSTEM_AUTHORITY) && conditionId.getPathSegments().size() == 1) ? conditionId.getPathSegments().get(0).equals("event") : false;
        if (!isEvent) {
            return null;
        }
        EventInfo rt = new EventInfo();
        rt.userId = tryParseInt(conditionId.getQueryParameter("userId"), -10000);
        rt.calendar = conditionId.getQueryParameter("calendar");
        if (TextUtils.isEmpty(rt.calendar) || tryParseLong(rt.calendar, -1L) != -1) {
            rt.calendar = null;
        }
        rt.reply = tryParseInt(conditionId.getQueryParameter("reply"), 0);
        return rt;
    }

    public static ComponentName getEventConditionProvider() {
        return new ComponentName(SYSTEM_AUTHORITY, "EventConditionProvider");
    }

    public static class EventInfo {
        public static final int REPLY_ANY_EXCEPT_NO = 0;
        public static final int REPLY_YES = 2;
        public static final int REPLY_YES_OR_MAYBE = 1;
        public String calendar;
        public int reply;
        public int userId = -10000;

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            if (!(o instanceof EventInfo)) {
                return false;
            }
            EventInfo other = (EventInfo) o;
            return this.userId == other.userId && Objects.equals(this.calendar, other.calendar) && this.reply == other.reply;
        }

        public EventInfo copy() {
            EventInfo rt = new EventInfo();
            rt.userId = this.userId;
            rt.calendar = this.calendar;
            rt.reply = this.reply;
            return rt;
        }

        public static int resolveUserId(int userId) {
            return userId == -10000 ? ActivityManager.getCurrentUser() : userId;
        }
    }

    private static int[] tryParseHourAndMinute(String value) {
        int i;
        if (TextUtils.isEmpty(value) || (i = value.indexOf(46)) < 1 || i >= value.length() - 1) {
            return null;
        }
        int hour = tryParseInt(value.substring(0, i), -1);
        int minute = tryParseInt(value.substring(i + 1), -1);
        if (isValidHour(hour) && isValidMinute(minute)) {
            return new int[]{hour, minute};
        }
        return null;
    }

    private static int tryParseZenMode(String value, int defValue) {
        int rt = tryParseInt(value, defValue);
        return Settings.Global.isValidZenMode(rt) ? rt : defValue;
    }

    public static String newRuleId() {
        return UUID.randomUUID().toString().replace(ContactsContract.Aas.ENCODE_SYMBOL, ProxyInfo.LOCAL_EXCL_LIST);
    }

    public static String getConditionSummary(Context context, ZenModeConfig config, int userHandle, boolean shortVersion) {
        return getConditionLine(context, config, userHandle, false, shortVersion);
    }

    private static String getConditionLine(Context context, ZenModeConfig config, int userHandle, boolean useLine1, boolean shortVersion) {
        String rt;
        if (config == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (config.manualRule != null) {
            Uri id = config.manualRule.conditionId;
            if (id == null) {
                return context.getString(17040819);
            }
            long time = tryParseCountdownConditionId(id);
            Condition c = config.manualRule.condition;
            if (time > 0) {
                long now = System.currentTimeMillis();
                long span = time - now;
                c = toTimeCondition(context, time, Math.round(span / 60000.0f), userHandle, shortVersion);
            }
            if (c == null) {
                rt = ProxyInfo.LOCAL_EXCL_LIST;
            } else {
                rt = useLine1 ? c.line1 : c.summary;
            }
            return TextUtils.isEmpty(rt) ? ProxyInfo.LOCAL_EXCL_LIST : rt;
        }
        String summary = ProxyInfo.LOCAL_EXCL_LIST;
        for (ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.isAutomaticActive()) {
                if (summary.isEmpty()) {
                    summary = automaticRule.name;
                } else {
                    summary = context.getResources().getString(17040821, summary, automaticRule.name);
                }
            }
        }
        return summary;
    }

    public static class ZenRule implements Parcelable {
        public static final Parcelable.Creator<ZenRule> CREATOR = new Parcelable.Creator<ZenRule>() {
            @Override
            public ZenRule createFromParcel(Parcel source) {
                return new ZenRule(source);
            }

            @Override
            public ZenRule[] newArray(int size) {
                return new ZenRule[size];
            }
        };
        public ComponentName component;
        public Condition condition;
        public Uri conditionId;
        public long creationTime;
        public boolean enabled;
        public String id;
        public String name;
        public boolean snoozing;
        public int zenMode;

        public ZenRule() {
        }

        public ZenRule(Parcel source) {
            this.enabled = source.readInt() == 1;
            this.snoozing = source.readInt() == 1;
            if (source.readInt() == 1) {
                this.name = source.readString();
            }
            this.zenMode = source.readInt();
            this.conditionId = (Uri) source.readParcelable(null);
            this.condition = (Condition) source.readParcelable(null);
            this.component = (ComponentName) source.readParcelable(null);
            if (source.readInt() == 1) {
                this.id = source.readString();
            }
            this.creationTime = source.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.enabled ? 1 : 0);
            dest.writeInt(this.snoozing ? 1 : 0);
            if (this.name != null) {
                dest.writeInt(1);
                dest.writeString(this.name);
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(this.zenMode);
            dest.writeParcelable(this.conditionId, 0);
            dest.writeParcelable(this.condition, 0);
            dest.writeParcelable(this.component, 0);
            if (this.id != null) {
                dest.writeInt(1);
                dest.writeString(this.id);
            } else {
                dest.writeInt(0);
            }
            dest.writeLong(this.creationTime);
        }

        public String toString() {
            return ZenRule.class.getSimpleName() + "[enabled=" + this.enabled + ",snoozing=" + this.snoozing + ",name=" + this.name + ",zenMode=" + Settings.Global.zenModeToString(this.zenMode) + ",conditionId=" + this.conditionId + ",condition=" + this.condition + ",component=" + this.component + ",id=" + this.id + ",creationTime=" + this.creationTime + ']';
        }

        private static void appendDiff(Diff d, String item, ZenRule from, ZenRule to) {
            if (d == null) {
                return;
            }
            if (from == null) {
                if (to != null) {
                    d.addLine(item, "insert");
                    return;
                }
                return;
            }
            from.appendDiff(d, item, to);
        }

        private void appendDiff(Diff d, String item, ZenRule to) {
            if (to == null) {
                d.addLine(item, "delete");
                return;
            }
            if (this.enabled != to.enabled) {
                d.addLine(item, "enabled", Boolean.valueOf(this.enabled), Boolean.valueOf(to.enabled));
            }
            if (this.snoozing != to.snoozing) {
                d.addLine(item, ZenModeConfig.RULE_ATT_SNOOZING, Boolean.valueOf(this.snoozing), Boolean.valueOf(to.snoozing));
            }
            if (!Objects.equals(this.name, to.name)) {
                d.addLine(item, "name", this.name, to.name);
            }
            if (this.zenMode != to.zenMode) {
                d.addLine(item, "zenMode", Integer.valueOf(this.zenMode), Integer.valueOf(to.zenMode));
            }
            if (!Objects.equals(this.conditionId, to.conditionId)) {
                d.addLine(item, ZenModeConfig.RULE_ATT_CONDITION_ID, this.conditionId, to.conditionId);
            }
            if (!Objects.equals(this.condition, to.condition)) {
                d.addLine(item, "condition", this.condition, to.condition);
            }
            if (!Objects.equals(this.component, to.component)) {
                d.addLine(item, CardEmulation.EXTRA_SERVICE_COMPONENT, this.component, to.component);
            }
            if (!Objects.equals(this.id, to.id)) {
                d.addLine(item, "id", this.id, to.id);
            }
            if (this.creationTime == to.creationTime) {
                return;
            }
            d.addLine(item, "creationTime", Long.valueOf(this.creationTime), Long.valueOf(to.creationTime));
        }

        public boolean equals(Object o) {
            if (!(o instanceof ZenRule)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            ZenRule other = (ZenRule) o;
            if (other.enabled == this.enabled && other.snoozing == this.snoozing && Objects.equals(other.name, this.name) && other.zenMode == this.zenMode && Objects.equals(other.conditionId, this.conditionId) && Objects.equals(other.condition, this.condition) && Objects.equals(other.component, this.component) && Objects.equals(other.id, this.id)) {
                return other.creationTime == this.creationTime;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hash(Boolean.valueOf(this.enabled), Boolean.valueOf(this.snoozing), this.name, Integer.valueOf(this.zenMode), this.conditionId, this.condition, this.component, this.id, Long.valueOf(this.creationTime));
        }

        public boolean isAutomaticActive() {
            if (!this.enabled || this.snoozing || this.component == null) {
                return false;
            }
            return isTrueOrUnknown();
        }

        public boolean isTrueOrUnknown() {
            if (this.condition != null) {
                return this.condition.state == 1 || this.condition.state == 2;
            }
            return false;
        }
    }

    public static final class XmlV1 {
        private static final String EXIT_CONDITION_ATT_COMPONENT = "component";
        private static final String EXIT_CONDITION_TAG = "exitCondition";
        private static final String SLEEP_ATT_END_HR = "endHour";
        private static final String SLEEP_ATT_END_MIN = "endMin";
        private static final String SLEEP_ATT_MODE = "mode";
        private static final String SLEEP_ATT_NONE = "none";
        private static final String SLEEP_ATT_START_HR = "startHour";
        private static final String SLEEP_ATT_START_MIN = "startMin";
        public static final String SLEEP_MODE_DAYS_PREFIX = "days:";
        public static final String SLEEP_MODE_NIGHTS = "nights";
        public static final String SLEEP_MODE_WEEKNIGHTS = "weeknights";
        private static final String SLEEP_TAG = "sleep";
        public boolean allowCalls;
        public boolean allowMessages;
        public ComponentName[] conditionComponents;
        public Uri[] conditionIds;
        public Condition exitCondition;
        public ComponentName exitConditionComponent;
        public int sleepEndHour;
        public int sleepEndMinute;
        public String sleepMode;
        public boolean sleepNone;
        public int sleepStartHour;
        public int sleepStartMinute;
        public boolean allowReminders = true;
        public boolean allowEvents = true;
        public int allowFrom = 0;

        private static boolean isValidSleepMode(String sleepMode) {
            return sleepMode == null || sleepMode.equals(SLEEP_MODE_NIGHTS) || sleepMode.equals(SLEEP_MODE_WEEKNIGHTS) || tryParseDays(sleepMode) != null;
        }

        public static int[] tryParseDays(String sleepMode) {
            if (sleepMode == null) {
                return null;
            }
            String sleepMode2 = sleepMode.trim();
            if (SLEEP_MODE_NIGHTS.equals(sleepMode2)) {
                return ZenModeConfig.ALL_DAYS;
            }
            if (SLEEP_MODE_WEEKNIGHTS.equals(sleepMode2)) {
                return ZenModeConfig.WEEKNIGHT_DAYS;
            }
            if (sleepMode2.startsWith(SLEEP_MODE_DAYS_PREFIX) && !sleepMode2.equals(SLEEP_MODE_DAYS_PREFIX)) {
                return ZenModeConfig.tryParseDayList(sleepMode2.substring(SLEEP_MODE_DAYS_PREFIX.length()), ",");
            }
            return null;
        }

        public static XmlV1 readXml(XmlPullParser parser) throws XmlPullParserException, IOException {
            XmlV1 rt = new XmlV1();
            ArrayList<ComponentName> conditionComponents = new ArrayList<>();
            ArrayList<Uri> conditionIds = new ArrayList<>();
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    String tag = parser.getName();
                    if (type == 3 && "zen".equals(tag)) {
                        if (!conditionComponents.isEmpty()) {
                            rt.conditionComponents = (ComponentName[]) conditionComponents.toArray(new ComponentName[conditionComponents.size()]);
                            rt.conditionIds = (Uri[]) conditionIds.toArray(new Uri[conditionIds.size()]);
                        }
                        return rt;
                    }
                    if (type == 2) {
                        if (ZenModeConfig.ALLOW_TAG.equals(tag)) {
                            rt.allowCalls = ZenModeConfig.safeBoolean(parser, ZenModeConfig.ALLOW_ATT_CALLS, false);
                            rt.allowMessages = ZenModeConfig.safeBoolean(parser, ZenModeConfig.ALLOW_ATT_MESSAGES, false);
                            rt.allowReminders = ZenModeConfig.safeBoolean(parser, ZenModeConfig.ALLOW_ATT_REMINDERS, true);
                            rt.allowEvents = ZenModeConfig.safeBoolean(parser, ZenModeConfig.ALLOW_ATT_EVENTS, true);
                            rt.allowFrom = ZenModeConfig.safeInt(parser, ZenModeConfig.ALLOW_ATT_FROM, 0);
                            if (rt.allowFrom < 0 || rt.allowFrom > 2) {
                                break;
                            }
                        } else if (SLEEP_TAG.equals(tag)) {
                            String mode = parser.getAttributeValue(null, "mode");
                            if (!isValidSleepMode(mode)) {
                                mode = null;
                            }
                            rt.sleepMode = mode;
                            rt.sleepNone = ZenModeConfig.safeBoolean(parser, "none", false);
                            int startHour = ZenModeConfig.safeInt(parser, SLEEP_ATT_START_HR, 0);
                            int startMinute = ZenModeConfig.safeInt(parser, SLEEP_ATT_START_MIN, 0);
                            int endHour = ZenModeConfig.safeInt(parser, SLEEP_ATT_END_HR, 0);
                            int endMinute = ZenModeConfig.safeInt(parser, SLEEP_ATT_END_MIN, 0);
                            if (!ZenModeConfig.isValidHour(startHour)) {
                                startHour = 0;
                            }
                            rt.sleepStartHour = startHour;
                            if (!ZenModeConfig.isValidMinute(startMinute)) {
                                startMinute = 0;
                            }
                            rt.sleepStartMinute = startMinute;
                            if (!ZenModeConfig.isValidHour(endHour)) {
                                endHour = 0;
                            }
                            rt.sleepEndHour = endHour;
                            if (!ZenModeConfig.isValidMinute(endMinute)) {
                                endMinute = 0;
                            }
                            rt.sleepEndMinute = endMinute;
                        } else if ("condition".equals(tag)) {
                            ComponentName component = ZenModeConfig.safeComponentName(parser, "component");
                            Uri conditionId = ZenModeConfig.safeUri(parser, "id");
                            if (component != null && conditionId != null) {
                                conditionComponents.add(component);
                                conditionIds.add(conditionId);
                            }
                        } else if (EXIT_CONDITION_TAG.equals(tag)) {
                            rt.exitCondition = ZenModeConfig.readConditionXml(parser);
                            if (rt.exitCondition != null) {
                                rt.exitConditionComponent = ZenModeConfig.safeComponentName(parser, "component");
                            }
                        }
                    }
                } else {
                    throw new IllegalStateException("Failed to reach END_DOCUMENT");
                }
            }
            throw new IndexOutOfBoundsException("bad source in config:" + rt.allowFrom);
        }
    }

    public static class Diff {
        private final ArrayList<String> lines = new ArrayList<>();

        public String toString() {
            StringBuilder sb = new StringBuilder("Diff[");
            int N = this.lines.size();
            for (int i = 0; i < N; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(this.lines.get(i));
            }
            return sb.append(']').toString();
        }

        private Diff addLine(String item, String action) {
            this.lines.add(item + ":" + action);
            return this;
        }

        public Diff addLine(String item, String subitem, Object from, Object to) {
            return addLine(item + "." + subitem, from, to);
        }

        public Diff addLine(String item, Object from, Object to) {
            return addLine(item, from + "->" + to);
        }
    }
}
