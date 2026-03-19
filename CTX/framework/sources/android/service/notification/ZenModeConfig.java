package android.service.notification;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Contacts;
import android.provider.Settings;
import android.provider.SettingsStringUtil;
import android.provider.Telephony;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.R;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.telephony.IccCardConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.UUID;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeConfig implements Parcelable {
    private static final String ALLOW_ATT_ALARMS = "alarms";
    private static final String ALLOW_ATT_CALLS = "calls";
    private static final String ALLOW_ATT_CALLS_FROM = "callsFrom";
    private static final String ALLOW_ATT_EVENTS = "events";
    private static final String ALLOW_ATT_FROM = "from";
    private static final String ALLOW_ATT_MEDIA = "media";
    private static final String ALLOW_ATT_MESSAGES = "messages";
    private static final String ALLOW_ATT_MESSAGES_FROM = "messagesFrom";
    private static final String ALLOW_ATT_REMINDERS = "reminders";
    private static final String ALLOW_ATT_REPEAT_CALLERS = "repeatCallers";
    private static final String ALLOW_ATT_SCREEN_OFF = "visualScreenOff";
    private static final String ALLOW_ATT_SCREEN_ON = "visualScreenOn";
    private static final String ALLOW_ATT_SYSTEM = "system";
    private static final String ALLOW_TAG = "allow";
    private static final String AUTOMATIC_TAG = "automatic";
    private static final String CONDITION_ATT_FLAGS = "flags";
    private static final String CONDITION_ATT_ICON = "icon";
    private static final String CONDITION_ATT_ID = "id";
    private static final String CONDITION_ATT_LINE1 = "line1";
    private static final String CONDITION_ATT_LINE2 = "line2";
    private static final String CONDITION_ATT_STATE = "state";
    private static final String CONDITION_ATT_SUMMARY = "summary";
    public static final String COUNTDOWN_PATH = "countdown";
    private static final int DAY_MINUTES = 1440;
    private static final boolean DEFAULT_ALLOW_ALARMS = true;
    private static final boolean DEFAULT_ALLOW_CALLS = true;
    private static final boolean DEFAULT_ALLOW_EVENTS = false;
    private static final boolean DEFAULT_ALLOW_MEDIA = true;
    private static final boolean DEFAULT_ALLOW_MESSAGES = false;
    private static final boolean DEFAULT_ALLOW_REMINDERS = false;
    private static final boolean DEFAULT_ALLOW_REPEAT_CALLERS = true;
    private static final boolean DEFAULT_ALLOW_SYSTEM = false;
    private static final int DEFAULT_CALLS_SOURCE = 2;
    private static final boolean DEFAULT_CHANNELS_BYPASSING_DND = false;
    private static final int DEFAULT_SOURCE = 1;
    private static final int DEFAULT_SUPPRESSED_VISUAL_EFFECTS = 0;
    private static final String DISALLOW_ATT_VISUAL_EFFECTS = "visualEffects";
    private static final String DISALLOW_TAG = "disallow";
    public static final String EVENT_PATH = "event";
    public static final String IS_ALARM_PATH = "alarm";
    private static final String MANUAL_TAG = "manual";
    public static final int MAX_SOURCE = 2;
    private static final int MINUTES_MS = 60000;
    private static final String RULE_ATT_COMPONENT = "component";
    private static final String RULE_ATT_CONDITION_ID = "conditionId";
    private static final String RULE_ATT_CREATION_TIME = "creationTime";
    private static final String RULE_ATT_ENABLED = "enabled";
    private static final String RULE_ATT_ENABLER = "enabler";
    private static final String RULE_ATT_ID = "ruleId";
    private static final String RULE_ATT_NAME = "name";
    private static final String RULE_ATT_SNOOZING = "snoozing";
    private static final String RULE_ATT_ZEN = "zen";
    public static final String SCHEDULE_PATH = "schedule";
    private static final int SECONDS_MS = 1000;
    public static final int SOURCE_ANYONE = 0;
    public static final int SOURCE_CONTACT = 1;
    public static final int SOURCE_STAR = 2;
    private static final String STATE_ATT_CHANNELS_BYPASSING_DND = "areChannelsBypassingDnd";
    private static final String STATE_TAG = "state";
    public static final String SYSTEM_AUTHORITY = "android";
    public static final int XML_VERSION = 8;
    private static final String ZEN_ATT_USER = "user";
    private static final String ZEN_ATT_VERSION = "version";
    public static final String ZEN_TAG = "zen";
    private static final int ZERO_VALUE_MS = 10000;
    public boolean allowAlarms;
    public boolean allowCalls;
    public int allowCallsFrom;
    public boolean allowEvents;
    public boolean allowMedia;
    public boolean allowMessages;
    public int allowMessagesFrom;
    public boolean allowReminders;
    public boolean allowRepeatCallers;
    public boolean allowSystem;
    public boolean areChannelsBypassingDnd;
    public ArrayMap<String, ZenRule> automaticRules;
    public ZenRule manualRule;
    public int suppressedVisualEffects;
    public int user;
    public int version;
    private static String TAG = "ZenModeConfig";
    public static final String EVERY_NIGHT_DEFAULT_RULE_ID = "EVERY_NIGHT_DEFAULT_RULE";
    public static final String EVENTS_DEFAULT_RULE_ID = "EVENTS_DEFAULT_RULE";
    public static final List<String> DEFAULT_RULE_IDS = Arrays.asList(EVERY_NIGHT_DEFAULT_RULE_ID, EVENTS_DEFAULT_RULE_ID);
    public static final int[] ALL_DAYS = {1, 2, 3, 4, 5, 6, 7};
    public static final int[] MINUTE_BUCKETS = generateMinuteBuckets();
    public static final Parcelable.Creator<ZenModeConfig> CREATOR = new Parcelable.Creator<ZenModeConfig>() {
        @Override
        public ZenModeConfig createFromParcel(Parcel parcel) {
            return new ZenModeConfig(parcel);
        }

        @Override
        public ZenModeConfig[] newArray(int i) {
            return new ZenModeConfig[i];
        }
    };

    public ZenModeConfig() {
        this.allowAlarms = true;
        this.allowMedia = true;
        this.allowSystem = false;
        this.allowCalls = true;
        this.allowRepeatCallers = true;
        this.allowMessages = false;
        this.allowReminders = false;
        this.allowEvents = false;
        this.allowCallsFrom = 2;
        this.allowMessagesFrom = 1;
        this.user = 0;
        this.suppressedVisualEffects = 0;
        this.areChannelsBypassingDnd = false;
        this.automaticRules = new ArrayMap<>();
    }

    public ZenModeConfig(Parcel parcel) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        this.allowAlarms = true;
        this.allowMedia = true;
        this.allowSystem = false;
        this.allowCalls = true;
        this.allowRepeatCallers = true;
        this.allowMessages = false;
        this.allowReminders = false;
        this.allowEvents = false;
        this.allowCallsFrom = 2;
        this.allowMessagesFrom = 1;
        this.user = 0;
        this.suppressedVisualEffects = 0;
        this.areChannelsBypassingDnd = false;
        this.automaticRules = new ArrayMap<>();
        if (parcel.readInt() == 1) {
            z = true;
        } else {
            z = false;
        }
        this.allowCalls = z;
        if (parcel.readInt() == 1) {
            z2 = true;
        } else {
            z2 = false;
        }
        this.allowRepeatCallers = z2;
        if (parcel.readInt() == 1) {
            z3 = true;
        } else {
            z3 = false;
        }
        this.allowMessages = z3;
        if (parcel.readInt() == 1) {
            z4 = true;
        } else {
            z4 = false;
        }
        this.allowReminders = z4;
        if (parcel.readInt() == 1) {
            z5 = true;
        } else {
            z5 = false;
        }
        this.allowEvents = z5;
        this.allowCallsFrom = parcel.readInt();
        this.allowMessagesFrom = parcel.readInt();
        this.user = parcel.readInt();
        this.manualRule = (ZenRule) parcel.readParcelable(null);
        int i = parcel.readInt();
        if (i > 0) {
            String[] strArr = new String[i];
            ZenRule[] zenRuleArr = new ZenRule[i];
            parcel.readStringArray(strArr);
            parcel.readTypedArray(zenRuleArr, ZenRule.CREATOR);
            for (int i2 = 0; i2 < i; i2++) {
                this.automaticRules.put(strArr[i2], zenRuleArr[i2]);
            }
        }
        if (parcel.readInt() == 1) {
            z6 = true;
        } else {
            z6 = false;
        }
        this.allowAlarms = z6;
        if (parcel.readInt() == 1) {
            z7 = true;
        } else {
            z7 = false;
        }
        this.allowMedia = z7;
        if (parcel.readInt() == 1) {
            z8 = true;
        } else {
            z8 = false;
        }
        this.allowSystem = z8;
        this.suppressedVisualEffects = parcel.readInt();
        this.areChannelsBypassingDnd = parcel.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.allowCalls ? 1 : 0);
        parcel.writeInt(this.allowRepeatCallers ? 1 : 0);
        parcel.writeInt(this.allowMessages ? 1 : 0);
        parcel.writeInt(this.allowReminders ? 1 : 0);
        parcel.writeInt(this.allowEvents ? 1 : 0);
        parcel.writeInt(this.allowCallsFrom);
        parcel.writeInt(this.allowMessagesFrom);
        parcel.writeInt(this.user);
        parcel.writeParcelable(this.manualRule, 0);
        if (!this.automaticRules.isEmpty()) {
            int size = this.automaticRules.size();
            String[] strArr = new String[size];
            ZenRule[] zenRuleArr = new ZenRule[size];
            for (int i2 = 0; i2 < size; i2++) {
                strArr[i2] = this.automaticRules.keyAt(i2);
                zenRuleArr[i2] = this.automaticRules.valueAt(i2);
            }
            parcel.writeInt(size);
            parcel.writeStringArray(strArr);
            parcel.writeTypedArray(zenRuleArr, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.allowAlarms ? 1 : 0);
        parcel.writeInt(this.allowMedia ? 1 : 0);
        parcel.writeInt(this.allowSystem ? 1 : 0);
        parcel.writeInt(this.suppressedVisualEffects);
        parcel.writeInt(this.areChannelsBypassingDnd ? 1 : 0);
    }

    public String toString() {
        return ZenModeConfig.class.getSimpleName() + "[user=" + this.user + ",allowAlarms=" + this.allowAlarms + ",allowMedia=" + this.allowMedia + ",allowSystem=" + this.allowSystem + ",allowReminders=" + this.allowReminders + ",allowEvents=" + this.allowEvents + ",allowCalls=" + this.allowCalls + ",allowRepeatCallers=" + this.allowRepeatCallers + ",allowMessages=" + this.allowMessages + ",allowCallsFrom=" + sourceToString(this.allowCallsFrom) + ",allowMessagesFrom=" + sourceToString(this.allowMessagesFrom) + ",suppressedVisualEffects=" + this.suppressedVisualEffects + ",areChannelsBypassingDnd=" + this.areChannelsBypassingDnd + ",automaticRules=" + this.automaticRules + ",manualRule=" + this.manualRule + ']';
    }

    private Diff diff(ZenModeConfig zenModeConfig) {
        Diff diff = new Diff();
        if (zenModeConfig != null) {
            if (this.user != zenModeConfig.user) {
                diff.addLine("user", Integer.valueOf(this.user), Integer.valueOf(zenModeConfig.user));
            }
            if (this.allowAlarms != zenModeConfig.allowAlarms) {
                diff.addLine("allowAlarms", Boolean.valueOf(this.allowAlarms), Boolean.valueOf(zenModeConfig.allowAlarms));
            }
            if (this.allowMedia != zenModeConfig.allowMedia) {
                diff.addLine("allowMedia", Boolean.valueOf(this.allowMedia), Boolean.valueOf(zenModeConfig.allowMedia));
            }
            if (this.allowSystem != zenModeConfig.allowSystem) {
                diff.addLine("allowSystem", Boolean.valueOf(this.allowSystem), Boolean.valueOf(zenModeConfig.allowSystem));
            }
            if (this.allowCalls != zenModeConfig.allowCalls) {
                diff.addLine("allowCalls", Boolean.valueOf(this.allowCalls), Boolean.valueOf(zenModeConfig.allowCalls));
            }
            if (this.allowReminders != zenModeConfig.allowReminders) {
                diff.addLine("allowReminders", Boolean.valueOf(this.allowReminders), Boolean.valueOf(zenModeConfig.allowReminders));
            }
            if (this.allowEvents != zenModeConfig.allowEvents) {
                diff.addLine("allowEvents", Boolean.valueOf(this.allowEvents), Boolean.valueOf(zenModeConfig.allowEvents));
            }
            if (this.allowRepeatCallers != zenModeConfig.allowRepeatCallers) {
                diff.addLine("allowRepeatCallers", Boolean.valueOf(this.allowRepeatCallers), Boolean.valueOf(zenModeConfig.allowRepeatCallers));
            }
            if (this.allowMessages != zenModeConfig.allowMessages) {
                diff.addLine("allowMessages", Boolean.valueOf(this.allowMessages), Boolean.valueOf(zenModeConfig.allowMessages));
            }
            if (this.allowCallsFrom != zenModeConfig.allowCallsFrom) {
                diff.addLine("allowCallsFrom", Integer.valueOf(this.allowCallsFrom), Integer.valueOf(zenModeConfig.allowCallsFrom));
            }
            if (this.allowMessagesFrom != zenModeConfig.allowMessagesFrom) {
                diff.addLine("allowMessagesFrom", Integer.valueOf(this.allowMessagesFrom), Integer.valueOf(zenModeConfig.allowMessagesFrom));
            }
            if (this.suppressedVisualEffects != zenModeConfig.suppressedVisualEffects) {
                diff.addLine("suppressedVisualEffects", Integer.valueOf(this.suppressedVisualEffects), Integer.valueOf(zenModeConfig.suppressedVisualEffects));
            }
            ArraySet arraySet = new ArraySet();
            addKeys(arraySet, this.automaticRules);
            addKeys(arraySet, zenModeConfig.automaticRules);
            int size = arraySet.size();
            for (int i = 0; i < size; i++) {
                String str = (String) arraySet.valueAt(i);
                ZenRule zenRule = null;
                ZenRule zenRule2 = this.automaticRules != null ? this.automaticRules.get(str) : null;
                if (zenModeConfig.automaticRules != null) {
                    zenRule = zenModeConfig.automaticRules.get(str);
                }
                ZenRule.appendDiff(diff, "automaticRule[" + str + "]", zenRule2, zenRule);
            }
            ZenRule.appendDiff(diff, "manualRule", this.manualRule, zenModeConfig.manualRule);
            if (this.areChannelsBypassingDnd != zenModeConfig.areChannelsBypassingDnd) {
                diff.addLine(STATE_ATT_CHANNELS_BYPASSING_DND, Boolean.valueOf(this.areChannelsBypassingDnd), Boolean.valueOf(zenModeConfig.areChannelsBypassingDnd));
            }
            return diff;
        }
        return diff.addLine("config", "delete");
    }

    public static Diff diff(ZenModeConfig zenModeConfig, ZenModeConfig zenModeConfig2) {
        if (zenModeConfig == null) {
            Diff diff = new Diff();
            if (zenModeConfig2 != null) {
                diff.addLine("config", "insert");
            }
            return diff;
        }
        return zenModeConfig.diff(zenModeConfig2);
    }

    private static <T> void addKeys(ArraySet<T> arraySet, ArrayMap<T, ?> arrayMap) {
        if (arrayMap != null) {
            for (int i = 0; i < arrayMap.size(); i++) {
                arraySet.add(arrayMap.keyAt(i));
            }
        }
    }

    public boolean isValid() {
        if (!isValidManualRule(this.manualRule)) {
            return false;
        }
        int size = this.automaticRules.size();
        for (int i = 0; i < size; i++) {
            if (!isValidAutomaticRule(this.automaticRules.valueAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidManualRule(ZenRule zenRule) {
        return zenRule == null || (Settings.Global.isValidZenMode(zenRule.zenMode) && sameCondition(zenRule));
    }

    private static boolean isValidAutomaticRule(ZenRule zenRule) {
        return (zenRule == null || TextUtils.isEmpty(zenRule.name) || !Settings.Global.isValidZenMode(zenRule.zenMode) || zenRule.conditionId == null || !sameCondition(zenRule)) ? false : true;
    }

    private static boolean sameCondition(ZenRule zenRule) {
        if (zenRule == null) {
            return false;
        }
        return zenRule.conditionId == null ? zenRule.condition == null : zenRule.condition == null || zenRule.conditionId.equals(zenRule.condition.id);
    }

    private static int[] generateMinuteBuckets() {
        int[] iArr = new int[15];
        iArr[0] = 15;
        iArr[1] = 30;
        iArr[2] = 45;
        for (int i = 1; i <= 12; i++) {
            iArr[2 + i] = 60 * i;
        }
        return iArr;
    }

    public static String sourceToString(int i) {
        switch (i) {
            case 0:
                return "anyone";
            case 1:
                return Contacts.AUTHORITY;
            case 2:
                return "stars";
            default:
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ZenModeConfig)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        ZenModeConfig zenModeConfig = (ZenModeConfig) obj;
        return zenModeConfig.allowAlarms == this.allowAlarms && zenModeConfig.allowMedia == this.allowMedia && zenModeConfig.allowSystem == this.allowSystem && zenModeConfig.allowCalls == this.allowCalls && zenModeConfig.allowRepeatCallers == this.allowRepeatCallers && zenModeConfig.allowMessages == this.allowMessages && zenModeConfig.allowCallsFrom == this.allowCallsFrom && zenModeConfig.allowMessagesFrom == this.allowMessagesFrom && zenModeConfig.allowReminders == this.allowReminders && zenModeConfig.allowEvents == this.allowEvents && zenModeConfig.user == this.user && Objects.equals(zenModeConfig.automaticRules, this.automaticRules) && Objects.equals(zenModeConfig.manualRule, this.manualRule) && zenModeConfig.suppressedVisualEffects == this.suppressedVisualEffects && zenModeConfig.areChannelsBypassingDnd == this.areChannelsBypassingDnd;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.allowAlarms), Boolean.valueOf(this.allowMedia), Boolean.valueOf(this.allowSystem), Boolean.valueOf(this.allowCalls), Boolean.valueOf(this.allowRepeatCallers), Boolean.valueOf(this.allowMessages), Integer.valueOf(this.allowCallsFrom), Integer.valueOf(this.allowMessagesFrom), Boolean.valueOf(this.allowReminders), Boolean.valueOf(this.allowEvents), Integer.valueOf(this.user), this.automaticRules, this.manualRule, Integer.valueOf(this.suppressedVisualEffects), Boolean.valueOf(this.areChannelsBypassingDnd));
    }

    private static String toDayList(int[] iArr) {
        if (iArr == null || iArr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iArr.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(iArr[i]);
        }
        return sb.toString();
    }

    private static int[] tryParseDayList(String str, String str2) {
        if (str == null) {
            return null;
        }
        String[] strArrSplit = str.split(str2);
        if (strArrSplit.length == 0) {
            return null;
        }
        int[] iArr = new int[strArrSplit.length];
        for (int i = 0; i < strArrSplit.length; i++) {
            int iTryParseInt = tryParseInt(strArrSplit[i], -1);
            if (iTryParseInt == -1) {
                return null;
            }
            iArr[i] = iTryParseInt;
        }
        return iArr;
    }

    private static int tryParseInt(String str, int i) {
        if (TextUtils.isEmpty(str)) {
            return i;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return i;
        }
    }

    private static long tryParseLong(String str, long j) {
        if (TextUtils.isEmpty(str)) {
            return j;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return j;
        }
    }

    public static ZenModeConfig readXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        if (xmlPullParser.getEventType() != 2 || !"zen".equals(xmlPullParser.getName())) {
            return null;
        }
        ZenModeConfig zenModeConfig = new ZenModeConfig();
        zenModeConfig.version = safeInt(xmlPullParser, "version", 8);
        zenModeConfig.user = safeInt(xmlPullParser, "user", zenModeConfig.user);
        boolean z = false;
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                String name = xmlPullParser.getName();
                if (next == 3 && "zen".equals(name)) {
                    return zenModeConfig;
                }
                if (next == 2) {
                    if (ALLOW_TAG.equals(name)) {
                        zenModeConfig.allowCalls = safeBoolean(xmlPullParser, ALLOW_ATT_CALLS, true);
                        zenModeConfig.allowRepeatCallers = safeBoolean(xmlPullParser, ALLOW_ATT_REPEAT_CALLERS, true);
                        zenModeConfig.allowMessages = safeBoolean(xmlPullParser, ALLOW_ATT_MESSAGES, false);
                        zenModeConfig.allowReminders = safeBoolean(xmlPullParser, ALLOW_ATT_REMINDERS, false);
                        zenModeConfig.allowEvents = safeBoolean(xmlPullParser, ALLOW_ATT_EVENTS, false);
                        int iSafeInt = safeInt(xmlPullParser, ALLOW_ATT_FROM, -1);
                        int iSafeInt2 = safeInt(xmlPullParser, ALLOW_ATT_CALLS_FROM, -1);
                        int iSafeInt3 = safeInt(xmlPullParser, ALLOW_ATT_MESSAGES_FROM, -1);
                        if (isValidSource(iSafeInt2) && isValidSource(iSafeInt3)) {
                            zenModeConfig.allowCallsFrom = iSafeInt2;
                            zenModeConfig.allowMessagesFrom = iSafeInt3;
                        } else if (isValidSource(iSafeInt)) {
                            Slog.i(TAG, "Migrating existing shared 'from': " + sourceToString(iSafeInt));
                            zenModeConfig.allowCallsFrom = iSafeInt;
                            zenModeConfig.allowMessagesFrom = iSafeInt;
                        } else {
                            zenModeConfig.allowCallsFrom = 2;
                            zenModeConfig.allowMessagesFrom = 1;
                        }
                        zenModeConfig.allowAlarms = safeBoolean(xmlPullParser, ALLOW_ATT_ALARMS, true);
                        zenModeConfig.allowMedia = safeBoolean(xmlPullParser, "media", true);
                        zenModeConfig.allowSystem = safeBoolean(xmlPullParser, "system", false);
                        Boolean boolUnsafeBoolean = unsafeBoolean(xmlPullParser, ALLOW_ATT_SCREEN_OFF);
                        if (boolUnsafeBoolean != null) {
                            if (boolUnsafeBoolean.booleanValue()) {
                                zenModeConfig.suppressedVisualEffects |= 12;
                            }
                            z = true;
                        }
                        Boolean boolUnsafeBoolean2 = unsafeBoolean(xmlPullParser, ALLOW_ATT_SCREEN_ON);
                        if (boolUnsafeBoolean2 != null) {
                            if (boolUnsafeBoolean2.booleanValue()) {
                                zenModeConfig.suppressedVisualEffects |= 16;
                            }
                            z = true;
                        }
                        if (z) {
                            Slog.d(TAG, "Migrated visual effects to " + zenModeConfig.suppressedVisualEffects);
                        }
                    } else if (DISALLOW_TAG.equals(name) && !z) {
                        zenModeConfig.suppressedVisualEffects = safeInt(xmlPullParser, DISALLOW_ATT_VISUAL_EFFECTS, 0);
                    } else if ("manual".equals(name)) {
                        zenModeConfig.manualRule = readRuleXml(xmlPullParser);
                    } else if (AUTOMATIC_TAG.equals(name)) {
                        String attributeValue = xmlPullParser.getAttributeValue(null, RULE_ATT_ID);
                        ZenRule ruleXml = readRuleXml(xmlPullParser);
                        if (attributeValue != null && ruleXml != null) {
                            ruleXml.id = attributeValue;
                            zenModeConfig.automaticRules.put(attributeValue, ruleXml);
                        }
                    } else if ("state".equals(name)) {
                        zenModeConfig.areChannelsBypassingDnd = safeBoolean(xmlPullParser, STATE_ATT_CHANNELS_BYPASSING_DND, false);
                    }
                }
            } else {
                throw new IllegalStateException("Failed to reach END_DOCUMENT");
            }
        }
    }

    public void writeXml(XmlSerializer xmlSerializer, Integer num) throws IOException {
        xmlSerializer.startTag(null, "zen");
        xmlSerializer.attribute(null, "version", Integer.toString(num == null ? 8 : num.intValue()));
        xmlSerializer.attribute(null, "user", Integer.toString(this.user));
        xmlSerializer.startTag(null, ALLOW_TAG);
        xmlSerializer.attribute(null, ALLOW_ATT_CALLS, Boolean.toString(this.allowCalls));
        xmlSerializer.attribute(null, ALLOW_ATT_REPEAT_CALLERS, Boolean.toString(this.allowRepeatCallers));
        xmlSerializer.attribute(null, ALLOW_ATT_MESSAGES, Boolean.toString(this.allowMessages));
        xmlSerializer.attribute(null, ALLOW_ATT_REMINDERS, Boolean.toString(this.allowReminders));
        xmlSerializer.attribute(null, ALLOW_ATT_EVENTS, Boolean.toString(this.allowEvents));
        xmlSerializer.attribute(null, ALLOW_ATT_CALLS_FROM, Integer.toString(this.allowCallsFrom));
        xmlSerializer.attribute(null, ALLOW_ATT_MESSAGES_FROM, Integer.toString(this.allowMessagesFrom));
        xmlSerializer.attribute(null, ALLOW_ATT_ALARMS, Boolean.toString(this.allowAlarms));
        xmlSerializer.attribute(null, "media", Boolean.toString(this.allowMedia));
        xmlSerializer.attribute(null, "system", Boolean.toString(this.allowSystem));
        xmlSerializer.endTag(null, ALLOW_TAG);
        xmlSerializer.startTag(null, DISALLOW_TAG);
        xmlSerializer.attribute(null, DISALLOW_ATT_VISUAL_EFFECTS, Integer.toString(this.suppressedVisualEffects));
        xmlSerializer.endTag(null, DISALLOW_TAG);
        if (this.manualRule != null) {
            xmlSerializer.startTag(null, "manual");
            writeRuleXml(this.manualRule, xmlSerializer);
            xmlSerializer.endTag(null, "manual");
        }
        int size = this.automaticRules.size();
        for (int i = 0; i < size; i++) {
            String strKeyAt = this.automaticRules.keyAt(i);
            ZenRule zenRuleValueAt = this.automaticRules.valueAt(i);
            xmlSerializer.startTag(null, AUTOMATIC_TAG);
            xmlSerializer.attribute(null, RULE_ATT_ID, strKeyAt);
            writeRuleXml(zenRuleValueAt, xmlSerializer);
            xmlSerializer.endTag(null, AUTOMATIC_TAG);
        }
        xmlSerializer.startTag(null, "state");
        xmlSerializer.attribute(null, STATE_ATT_CHANNELS_BYPASSING_DND, Boolean.toString(this.areChannelsBypassingDnd));
        xmlSerializer.endTag(null, "state");
        xmlSerializer.endTag(null, "zen");
    }

    public static ZenRule readRuleXml(XmlPullParser xmlPullParser) {
        ZenRule zenRule = new ZenRule();
        zenRule.enabled = safeBoolean(xmlPullParser, "enabled", true);
        zenRule.snoozing = safeBoolean(xmlPullParser, RULE_ATT_SNOOZING, false);
        zenRule.name = xmlPullParser.getAttributeValue(null, "name");
        String attributeValue = xmlPullParser.getAttributeValue(null, "zen");
        zenRule.zenMode = tryParseZenMode(attributeValue, -1);
        if (zenRule.zenMode == -1) {
            Slog.w(TAG, "Bad zen mode in rule xml:" + attributeValue);
            return null;
        }
        zenRule.conditionId = safeUri(xmlPullParser, RULE_ATT_CONDITION_ID);
        zenRule.component = safeComponentName(xmlPullParser, "component");
        zenRule.creationTime = safeLong(xmlPullParser, "creationTime", 0L);
        zenRule.enabler = xmlPullParser.getAttributeValue(null, RULE_ATT_ENABLER);
        zenRule.condition = readConditionXml(xmlPullParser);
        if (zenRule.zenMode != 1 && Condition.isValidId(zenRule.conditionId, SYSTEM_AUTHORITY)) {
            Slog.i(TAG, "Updating zenMode of automatic rule " + zenRule.name);
            zenRule.zenMode = 1;
        }
        return zenRule;
    }

    public static void writeRuleXml(ZenRule zenRule, XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.attribute(null, "enabled", Boolean.toString(zenRule.enabled));
        xmlSerializer.attribute(null, RULE_ATT_SNOOZING, Boolean.toString(zenRule.snoozing));
        if (zenRule.name != null) {
            xmlSerializer.attribute(null, "name", zenRule.name);
        }
        xmlSerializer.attribute(null, "zen", Integer.toString(zenRule.zenMode));
        if (zenRule.component != null) {
            xmlSerializer.attribute(null, "component", zenRule.component.flattenToString());
        }
        if (zenRule.conditionId != null) {
            xmlSerializer.attribute(null, RULE_ATT_CONDITION_ID, zenRule.conditionId.toString());
        }
        xmlSerializer.attribute(null, "creationTime", Long.toString(zenRule.creationTime));
        if (zenRule.enabler != null) {
            xmlSerializer.attribute(null, RULE_ATT_ENABLER, zenRule.enabler);
        }
        if (zenRule.condition != null) {
            writeConditionXml(zenRule.condition, xmlSerializer);
        }
    }

    public static Condition readConditionXml(XmlPullParser xmlPullParser) {
        Uri uriSafeUri = safeUri(xmlPullParser, "id");
        if (uriSafeUri == null) {
            return null;
        }
        try {
            return new Condition(uriSafeUri, xmlPullParser.getAttributeValue(null, "summary"), xmlPullParser.getAttributeValue(null, CONDITION_ATT_LINE1), xmlPullParser.getAttributeValue(null, CONDITION_ATT_LINE2), safeInt(xmlPullParser, "icon", -1), safeInt(xmlPullParser, "state", -1), safeInt(xmlPullParser, "flags", -1));
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Unable to read condition xml", e);
            return null;
        }
    }

    public static void writeConditionXml(Condition condition, XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.attribute(null, "id", condition.id.toString());
        xmlSerializer.attribute(null, "summary", condition.summary);
        xmlSerializer.attribute(null, CONDITION_ATT_LINE1, condition.line1);
        xmlSerializer.attribute(null, CONDITION_ATT_LINE2, condition.line2);
        xmlSerializer.attribute(null, "icon", Integer.toString(condition.icon));
        xmlSerializer.attribute(null, "state", Integer.toString(condition.state));
        xmlSerializer.attribute(null, "flags", Integer.toString(condition.flags));
    }

    public static boolean isValidHour(int i) {
        return i >= 0 && i < 24;
    }

    public static boolean isValidMinute(int i) {
        return i >= 0 && i < 60;
    }

    private static boolean isValidSource(int i) {
        return i >= 0 && i <= 2;
    }

    private static Boolean unsafeBoolean(XmlPullParser xmlPullParser, String str) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (TextUtils.isEmpty(attributeValue)) {
            return null;
        }
        return Boolean.valueOf(Boolean.parseBoolean(attributeValue));
    }

    private static boolean safeBoolean(XmlPullParser xmlPullParser, String str, boolean z) {
        return safeBoolean(xmlPullParser.getAttributeValue(null, str), z);
    }

    private static boolean safeBoolean(String str, boolean z) {
        return TextUtils.isEmpty(str) ? z : Boolean.parseBoolean(str);
    }

    private static int safeInt(XmlPullParser xmlPullParser, String str, int i) {
        return tryParseInt(xmlPullParser.getAttributeValue(null, str), i);
    }

    private static ComponentName safeComponentName(XmlPullParser xmlPullParser, String str) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (TextUtils.isEmpty(attributeValue)) {
            return null;
        }
        return ComponentName.unflattenFromString(attributeValue);
    }

    private static Uri safeUri(XmlPullParser xmlPullParser, String str) {
        String attributeValue = xmlPullParser.getAttributeValue(null, str);
        if (TextUtils.isEmpty(attributeValue)) {
            return null;
        }
        return Uri.parse(attributeValue);
    }

    private static long safeLong(XmlPullParser xmlPullParser, String str, long j) {
        return tryParseLong(xmlPullParser.getAttributeValue(null, str), j);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ZenModeConfig copy() {
        Parcel parcelObtain = Parcel.obtain();
        try {
            writeToParcel(parcelObtain, 0);
            parcelObtain.setDataPosition(0);
            return new ZenModeConfig(parcelObtain);
        } finally {
            parcelObtain.recycle();
        }
    }

    public NotificationManager.Policy toNotificationPolicy() {
        int i;
        if (this.allowCalls) {
            i = 8;
        } else {
            i = 0;
        }
        if (this.allowMessages) {
            i |= 4;
        }
        if (this.allowEvents) {
            i |= 2;
        }
        if (this.allowReminders) {
            i |= 1;
        }
        if (this.allowRepeatCallers) {
            i |= 16;
        }
        if (this.allowAlarms) {
            i |= 32;
        }
        if (this.allowMedia) {
            i |= 64;
        }
        if (this.allowSystem) {
            i |= 128;
        }
        return new NotificationManager.Policy(i, sourceToPrioritySenders(this.allowCallsFrom, 1), sourceToPrioritySenders(this.allowMessagesFrom, 1), this.suppressedVisualEffects, this.areChannelsBypassingDnd ? 1 : 0);
    }

    public static ScheduleCalendar toScheduleCalendar(Uri uri) {
        ScheduleInfo scheduleInfoTryParseScheduleConditionId = tryParseScheduleConditionId(uri);
        if (scheduleInfoTryParseScheduleConditionId == null || scheduleInfoTryParseScheduleConditionId.days == null || scheduleInfoTryParseScheduleConditionId.days.length == 0) {
            return null;
        }
        ScheduleCalendar scheduleCalendar = new ScheduleCalendar();
        scheduleCalendar.setSchedule(scheduleInfoTryParseScheduleConditionId);
        scheduleCalendar.setTimeZone(TimeZone.getDefault());
        return scheduleCalendar;
    }

    private static int sourceToPrioritySenders(int i, int i2) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return i2;
        }
    }

    private static int prioritySendersToSource(int i, int i2) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            default:
                return i2;
        }
    }

    public void applyNotificationPolicy(NotificationManager.Policy policy) {
        if (policy == null) {
            return;
        }
        this.allowAlarms = (policy.priorityCategories & 32) != 0;
        this.allowMedia = (policy.priorityCategories & 64) != 0;
        this.allowSystem = (policy.priorityCategories & 128) != 0;
        this.allowEvents = (policy.priorityCategories & 2) != 0;
        this.allowReminders = (policy.priorityCategories & 1) != 0;
        this.allowCalls = (policy.priorityCategories & 8) != 0;
        this.allowMessages = (policy.priorityCategories & 4) != 0;
        this.allowRepeatCallers = (policy.priorityCategories & 16) != 0;
        this.allowCallsFrom = prioritySendersToSource(policy.priorityCallSenders, this.allowCallsFrom);
        this.allowMessagesFrom = prioritySendersToSource(policy.priorityMessageSenders, this.allowMessagesFrom);
        if (policy.suppressedVisualEffects != -1) {
            this.suppressedVisualEffects = policy.suppressedVisualEffects;
        }
        if (policy.state != -1) {
            this.areChannelsBypassingDnd = (policy.state & 1) != 0;
        }
    }

    public static Condition toTimeCondition(Context context, int i, int i2) {
        return toTimeCondition(context, i, i2, false);
    }

    public static Condition toTimeCondition(Context context, int i, int i2, boolean z) {
        return toTimeCondition(context, System.currentTimeMillis() + (i == 0 ? JobInfo.MIN_BACKOFF_MILLIS : 60000 * i), i, i2, z);
    }

    public static Condition toTimeCondition(Context context, long j, int i, int i2, boolean z) {
        String string;
        String str;
        String str2;
        String quantityString;
        String quantityString2;
        String string2;
        CharSequence formattedTime = getFormattedTime(context, j, isToday(j), i2);
        Resources resources = context.getResources();
        if (i < 60) {
            quantityString = resources.getQuantityString(z ? R.plurals.zen_mode_duration_minutes_summary_short : R.plurals.zen_mode_duration_minutes_summary, i, Integer.valueOf(i), formattedTime);
            quantityString2 = resources.getQuantityString(z ? R.plurals.zen_mode_duration_minutes_short : R.plurals.zen_mode_duration_minutes, i, Integer.valueOf(i), formattedTime);
            string2 = resources.getString(R.string.zen_mode_until, formattedTime);
        } else {
            if (i >= 1440) {
                string = resources.getString(R.string.zen_mode_until, formattedTime);
                str = string;
                str2 = str;
                return new Condition(toCountdownConditionId(j, false), string, str, str2, 0, 1, 1);
            }
            int iRound = Math.round(i / 60.0f);
            quantityString = resources.getQuantityString(z ? R.plurals.zen_mode_duration_hours_summary_short : R.plurals.zen_mode_duration_hours_summary, iRound, Integer.valueOf(iRound), formattedTime);
            quantityString2 = resources.getQuantityString(z ? R.plurals.zen_mode_duration_hours_short : R.plurals.zen_mode_duration_hours, iRound, Integer.valueOf(iRound), formattedTime);
            string2 = resources.getString(R.string.zen_mode_until, formattedTime);
        }
        str2 = string2;
        str = quantityString2;
        string = quantityString;
        return new Condition(toCountdownConditionId(j, false), string, str, str2, 0, 1, 1);
    }

    public static Condition toNextAlarmCondition(Context context, long j, int i) {
        return new Condition(toCountdownConditionId(j, true), "", context.getResources().getString(R.string.zen_mode_until, getFormattedTime(context, j, isToday(j), i)), "", 0, 1, 1);
    }

    public static CharSequence getFormattedTime(Context context, long j, boolean z, int i) {
        StringBuilder sb = new StringBuilder();
        sb.append(!z ? "EEE " : "");
        sb.append(DateFormat.is24HourFormat(context, i) ? "Hm" : "hma");
        return DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), sb.toString()), j);
    }

    public static boolean isToday(long j) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        GregorianCalendar gregorianCalendar2 = new GregorianCalendar();
        gregorianCalendar2.setTimeInMillis(j);
        if (gregorianCalendar.get(1) == gregorianCalendar2.get(1) && gregorianCalendar.get(2) == gregorianCalendar2.get(2) && gregorianCalendar.get(5) == gregorianCalendar2.get(5)) {
            return true;
        }
        return false;
    }

    public static Uri toCountdownConditionId(long j, boolean z) {
        return new Uri.Builder().scheme(Condition.SCHEME).authority(SYSTEM_AUTHORITY).appendPath(COUNTDOWN_PATH).appendPath(Long.toString(j)).appendPath("alarm").appendPath(Boolean.toString(z)).build();
    }

    public static long tryParseCountdownConditionId(Uri uri) {
        if (!Condition.isValidId(uri, SYSTEM_AUTHORITY) || uri.getPathSegments().size() < 2 || !COUNTDOWN_PATH.equals(uri.getPathSegments().get(0))) {
            return 0L;
        }
        try {
            return Long.parseLong(uri.getPathSegments().get(1));
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error parsing countdown condition: " + uri, e);
            return 0L;
        }
    }

    public static boolean isValidCountdownConditionId(Uri uri) {
        return tryParseCountdownConditionId(uri) != 0;
    }

    public static boolean isValidCountdownToAlarmConditionId(Uri uri) {
        if (tryParseCountdownConditionId(uri) == 0 || uri.getPathSegments().size() < 4 || !"alarm".equals(uri.getPathSegments().get(2))) {
            return false;
        }
        try {
            return Boolean.parseBoolean(uri.getPathSegments().get(3));
        } catch (RuntimeException e) {
            Slog.w(TAG, "Error parsing countdown alarm condition: " + uri, e);
            return false;
        }
    }

    public static Uri toScheduleConditionId(ScheduleInfo scheduleInfo) {
        return new Uri.Builder().scheme(Condition.SCHEME).authority(SYSTEM_AUTHORITY).appendPath(SCHEDULE_PATH).appendQueryParameter("days", toDayList(scheduleInfo.days)).appendQueryParameter(Telephony.BaseMmsColumns.START, scheduleInfo.startHour + "." + scheduleInfo.startMinute).appendQueryParameter("end", scheduleInfo.endHour + "." + scheduleInfo.endMinute).appendQueryParameter("exitAtAlarm", String.valueOf(scheduleInfo.exitAtAlarm)).build();
    }

    public static boolean isValidScheduleConditionId(Uri uri) {
        try {
            ScheduleInfo scheduleInfoTryParseScheduleConditionId = tryParseScheduleConditionId(uri);
            if (scheduleInfoTryParseScheduleConditionId == null || scheduleInfoTryParseScheduleConditionId.days == null || scheduleInfoTryParseScheduleConditionId.days.length == 0) {
                return false;
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            return false;
        }
    }

    public static ScheduleInfo tryParseScheduleConditionId(Uri uri) {
        if (!(uri != null && uri.getScheme().equals(Condition.SCHEME) && uri.getAuthority().equals(SYSTEM_AUTHORITY) && uri.getPathSegments().size() == 1 && uri.getPathSegments().get(0).equals(SCHEDULE_PATH))) {
            return null;
        }
        int[] iArrTryParseHourAndMinute = tryParseHourAndMinute(uri.getQueryParameter(Telephony.BaseMmsColumns.START));
        int[] iArrTryParseHourAndMinute2 = tryParseHourAndMinute(uri.getQueryParameter("end"));
        if (iArrTryParseHourAndMinute == null || iArrTryParseHourAndMinute2 == null) {
            return null;
        }
        ScheduleInfo scheduleInfo = new ScheduleInfo();
        scheduleInfo.days = tryParseDayList(uri.getQueryParameter("days"), "\\.");
        scheduleInfo.startHour = iArrTryParseHourAndMinute[0];
        scheduleInfo.startMinute = iArrTryParseHourAndMinute[1];
        scheduleInfo.endHour = iArrTryParseHourAndMinute2[0];
        scheduleInfo.endMinute = iArrTryParseHourAndMinute2[1];
        scheduleInfo.exitAtAlarm = safeBoolean(uri.getQueryParameter("exitAtAlarm"), false);
        return scheduleInfo;
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

        public boolean equals(Object obj) {
            if (!(obj instanceof ScheduleInfo)) {
                return false;
            }
            ScheduleInfo scheduleInfo = (ScheduleInfo) obj;
            return ZenModeConfig.toDayList(this.days).equals(ZenModeConfig.toDayList(scheduleInfo.days)) && this.startHour == scheduleInfo.startHour && this.startMinute == scheduleInfo.startMinute && this.endHour == scheduleInfo.endHour && this.endMinute == scheduleInfo.endMinute && this.exitAtAlarm == scheduleInfo.exitAtAlarm;
        }

        public ScheduleInfo copy() {
            ScheduleInfo scheduleInfo = new ScheduleInfo();
            if (this.days != null) {
                scheduleInfo.days = new int[this.days.length];
                System.arraycopy(this.days, 0, scheduleInfo.days, 0, this.days.length);
            }
            scheduleInfo.startHour = this.startHour;
            scheduleInfo.startMinute = this.startMinute;
            scheduleInfo.endHour = this.endHour;
            scheduleInfo.endMinute = this.endMinute;
            scheduleInfo.exitAtAlarm = this.exitAtAlarm;
            scheduleInfo.nextAlarm = this.nextAlarm;
            return scheduleInfo;
        }

        public String toString() {
            return "ScheduleInfo{days=" + Arrays.toString(this.days) + ", startHour=" + this.startHour + ", startMinute=" + this.startMinute + ", endHour=" + this.endHour + ", endMinute=" + this.endMinute + ", exitAtAlarm=" + this.exitAtAlarm + ", nextAlarm=" + ts(this.nextAlarm) + '}';
        }

        protected static String ts(long j) {
            return new Date(j) + " (" + j + ")";
        }
    }

    public static Uri toEventConditionId(EventInfo eventInfo) {
        return new Uri.Builder().scheme(Condition.SCHEME).authority(SYSTEM_AUTHORITY).appendPath("event").appendQueryParameter("userId", Long.toString(eventInfo.userId)).appendQueryParameter("calendar", eventInfo.calendar != null ? eventInfo.calendar : "").appendQueryParameter("reply", Integer.toString(eventInfo.reply)).build();
    }

    public static boolean isValidEventConditionId(Uri uri) {
        return tryParseEventConditionId(uri) != null;
    }

    public static EventInfo tryParseEventConditionId(Uri uri) {
        boolean z = true;
        if (uri == null || !uri.getScheme().equals(Condition.SCHEME) || !uri.getAuthority().equals(SYSTEM_AUTHORITY) || uri.getPathSegments().size() != 1 || !uri.getPathSegments().get(0).equals("event")) {
            z = false;
        }
        if (!z) {
            return null;
        }
        EventInfo eventInfo = new EventInfo();
        eventInfo.userId = tryParseInt(uri.getQueryParameter("userId"), -10000);
        eventInfo.calendar = uri.getQueryParameter("calendar");
        if (TextUtils.isEmpty(eventInfo.calendar) || tryParseLong(eventInfo.calendar, -1L) != -1) {
            eventInfo.calendar = null;
        }
        eventInfo.reply = tryParseInt(uri.getQueryParameter("reply"), 0);
        return eventInfo;
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

        public boolean equals(Object obj) {
            if (!(obj instanceof EventInfo)) {
                return false;
            }
            EventInfo eventInfo = (EventInfo) obj;
            return this.userId == eventInfo.userId && Objects.equals(this.calendar, eventInfo.calendar) && this.reply == eventInfo.reply;
        }

        public EventInfo copy() {
            EventInfo eventInfo = new EventInfo();
            eventInfo.userId = this.userId;
            eventInfo.calendar = this.calendar;
            eventInfo.reply = this.reply;
            return eventInfo;
        }

        public static int resolveUserId(int i) {
            return i == -10000 ? ActivityManager.getCurrentUser() : i;
        }
    }

    private static int[] tryParseHourAndMinute(String str) {
        int iIndexOf;
        if (TextUtils.isEmpty(str) || (iIndexOf = str.indexOf(46)) < 1 || iIndexOf >= str.length() - 1) {
            return null;
        }
        int iTryParseInt = tryParseInt(str.substring(0, iIndexOf), -1);
        int iTryParseInt2 = tryParseInt(str.substring(iIndexOf + 1), -1);
        if (isValidHour(iTryParseInt) && isValidMinute(iTryParseInt2)) {
            return new int[]{iTryParseInt, iTryParseInt2};
        }
        return null;
    }

    private static int tryParseZenMode(String str, int i) {
        int iTryParseInt = tryParseInt(str, i);
        return Settings.Global.isValidZenMode(iTryParseInt) ? iTryParseInt : i;
    }

    public static String newRuleId() {
        return UUID.randomUUID().toString().replace(NativeLibraryHelper.CLEAR_ABI_OVERRIDE, "");
    }

    public static String getOwnerCaption(Context context, String str) {
        CharSequence charSequenceLoadLabel;
        PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(str, 0);
            if (applicationInfo != null && (charSequenceLoadLabel = applicationInfo.loadLabel(packageManager)) != null) {
                String strTrim = charSequenceLoadLabel.toString().trim();
                if (strTrim.length() > 0) {
                    return strTrim;
                }
                return "";
            }
            return "";
        } catch (Throwable th) {
            Slog.w(TAG, "Error loading owner caption", th);
            return "";
        }
    }

    public static String getConditionSummary(Context context, ZenModeConfig zenModeConfig, int i, boolean z) {
        return getConditionLine(context, zenModeConfig, i, false, z);
    }

    private static String getConditionLine(Context context, ZenModeConfig zenModeConfig, int i, boolean z, boolean z2) {
        String string;
        if (zenModeConfig == null) {
            return "";
        }
        String string2 = "";
        if (zenModeConfig.manualRule != null) {
            Uri uri = zenModeConfig.manualRule.conditionId;
            if (zenModeConfig.manualRule.enabler != null) {
                string2 = getOwnerCaption(context, zenModeConfig.manualRule.enabler);
            } else if (uri == null) {
                string2 = context.getString(R.string.zen_mode_forever);
            } else {
                long jTryParseCountdownConditionId = tryParseCountdownConditionId(uri);
                Condition timeCondition = zenModeConfig.manualRule.condition;
                if (jTryParseCountdownConditionId > 0) {
                    timeCondition = toTimeCondition(context, jTryParseCountdownConditionId, Math.round((jTryParseCountdownConditionId - System.currentTimeMillis()) / 60000.0f), i, z2);
                }
                String str = timeCondition == null ? "" : z ? timeCondition.line1 : timeCondition.summary;
                if (TextUtils.isEmpty(str)) {
                    str = "";
                }
                string2 = str;
            }
        }
        for (ZenRule zenRule : zenModeConfig.automaticRules.values()) {
            if (zenRule.isAutomaticActive()) {
                if (string2.isEmpty()) {
                    string = zenRule.name;
                } else {
                    string = context.getResources().getString(R.string.zen_mode_rule_name_combination, string2, zenRule.name);
                }
                string2 = string;
            }
        }
        return string2;
    }

    public static class ZenRule implements Parcelable {
        public static final Parcelable.Creator<ZenRule> CREATOR = new Parcelable.Creator<ZenRule>() {
            @Override
            public ZenRule createFromParcel(Parcel parcel) {
                return new ZenRule(parcel);
            }

            @Override
            public ZenRule[] newArray(int i) {
                return new ZenRule[i];
            }
        };
        public ComponentName component;
        public Condition condition;
        public Uri conditionId;
        public long creationTime;
        public boolean enabled;
        public String enabler;
        public String id;
        public String name;
        public boolean snoozing;
        public int zenMode;

        public ZenRule() {
        }

        public ZenRule(Parcel parcel) {
            this.enabled = parcel.readInt() == 1;
            this.snoozing = parcel.readInt() == 1;
            if (parcel.readInt() == 1) {
                this.name = parcel.readString();
            }
            this.zenMode = parcel.readInt();
            this.conditionId = (Uri) parcel.readParcelable(null);
            this.condition = (Condition) parcel.readParcelable(null);
            this.component = (ComponentName) parcel.readParcelable(null);
            if (parcel.readInt() == 1) {
                this.id = parcel.readString();
            }
            this.creationTime = parcel.readLong();
            if (parcel.readInt() == 1) {
                this.enabler = parcel.readString();
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.enabled ? 1 : 0);
            parcel.writeInt(this.snoozing ? 1 : 0);
            if (this.name != null) {
                parcel.writeInt(1);
                parcel.writeString(this.name);
            } else {
                parcel.writeInt(0);
            }
            parcel.writeInt(this.zenMode);
            parcel.writeParcelable(this.conditionId, 0);
            parcel.writeParcelable(this.condition, 0);
            parcel.writeParcelable(this.component, 0);
            if (this.id != null) {
                parcel.writeInt(1);
                parcel.writeString(this.id);
            } else {
                parcel.writeInt(0);
            }
            parcel.writeLong(this.creationTime);
            if (this.enabler != null) {
                parcel.writeInt(1);
                parcel.writeString(this.enabler);
            } else {
                parcel.writeInt(0);
            }
        }

        public String toString() {
            return ZenRule.class.getSimpleName() + "[enabled=" + this.enabled + ",snoozing=" + this.snoozing + ",name=" + this.name + ",zenMode=" + Settings.Global.zenModeToString(this.zenMode) + ",conditionId=" + this.conditionId + ",condition=" + this.condition + ",component=" + this.component + ",id=" + this.id + ",creationTime=" + this.creationTime + ",enabler=" + this.enabler + ']';
        }

        public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1138166333441L, this.id);
            protoOutputStream.write(1138166333442L, this.name);
            protoOutputStream.write(1112396529667L, this.creationTime);
            protoOutputStream.write(1133871366148L, this.enabled);
            protoOutputStream.write(1138166333445L, this.enabler);
            protoOutputStream.write(1133871366150L, this.snoozing);
            protoOutputStream.write(1159641169927L, this.zenMode);
            if (this.conditionId != null) {
                protoOutputStream.write(1138166333448L, this.conditionId.toString());
            }
            if (this.condition != null) {
                this.condition.writeToProto(protoOutputStream, 1146756268041L);
            }
            if (this.component != null) {
                this.component.writeToProto(protoOutputStream, 1146756268042L);
            }
            protoOutputStream.end(jStart);
        }

        private static void appendDiff(Diff diff, String str, ZenRule zenRule, ZenRule zenRule2) {
            if (diff == null) {
                return;
            }
            if (zenRule == null) {
                if (zenRule2 == null) {
                    return;
                }
                diff.addLine(str, "insert");
                return;
            }
            zenRule.appendDiff(diff, str, zenRule2);
        }

        private void appendDiff(Diff diff, String str, ZenRule zenRule) {
            if (zenRule != null) {
                if (this.enabled != zenRule.enabled) {
                    diff.addLine(str, "enabled", Boolean.valueOf(this.enabled), Boolean.valueOf(zenRule.enabled));
                }
                if (this.snoozing != zenRule.snoozing) {
                    diff.addLine(str, ZenModeConfig.RULE_ATT_SNOOZING, Boolean.valueOf(this.snoozing), Boolean.valueOf(zenRule.snoozing));
                }
                if (!Objects.equals(this.name, zenRule.name)) {
                    diff.addLine(str, "name", this.name, zenRule.name);
                }
                if (this.zenMode != zenRule.zenMode) {
                    diff.addLine(str, "zenMode", Integer.valueOf(this.zenMode), Integer.valueOf(zenRule.zenMode));
                }
                if (!Objects.equals(this.conditionId, zenRule.conditionId)) {
                    diff.addLine(str, ZenModeConfig.RULE_ATT_CONDITION_ID, this.conditionId, zenRule.conditionId);
                }
                if (!Objects.equals(this.condition, zenRule.condition)) {
                    diff.addLine(str, Condition.SCHEME, this.condition, zenRule.condition);
                }
                if (!Objects.equals(this.component, zenRule.component)) {
                    diff.addLine(str, "component", this.component, zenRule.component);
                }
                if (!Objects.equals(this.id, zenRule.id)) {
                    diff.addLine(str, "id", this.id, zenRule.id);
                }
                if (this.creationTime != zenRule.creationTime) {
                    diff.addLine(str, "creationTime", Long.valueOf(this.creationTime), Long.valueOf(zenRule.creationTime));
                }
                if (this.enabler != zenRule.enabler) {
                    diff.addLine(str, ZenModeConfig.RULE_ATT_ENABLER, this.enabler, zenRule.enabler);
                    return;
                }
                return;
            }
            diff.addLine(str, "delete");
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ZenRule)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            ZenRule zenRule = (ZenRule) obj;
            return zenRule.enabled == this.enabled && zenRule.snoozing == this.snoozing && Objects.equals(zenRule.name, this.name) && zenRule.zenMode == this.zenMode && Objects.equals(zenRule.conditionId, this.conditionId) && Objects.equals(zenRule.condition, this.condition) && Objects.equals(zenRule.component, this.component) && Objects.equals(zenRule.id, this.id) && zenRule.creationTime == this.creationTime && Objects.equals(zenRule.enabler, this.enabler);
        }

        public int hashCode() {
            return Objects.hash(Boolean.valueOf(this.enabled), Boolean.valueOf(this.snoozing), this.name, Integer.valueOf(this.zenMode), this.conditionId, this.condition, this.component, this.id, Long.valueOf(this.creationTime), this.enabler);
        }

        public boolean isAutomaticActive() {
            return this.enabled && !this.snoozing && this.component != null && isTrueOrUnknown();
        }

        public boolean isTrueOrUnknown() {
            return this.condition != null && (this.condition.state == 1 || this.condition.state == 2);
        }
    }

    public static class Diff {
        private final ArrayList<String> lines = new ArrayList<>();

        public String toString() {
            StringBuilder sb = new StringBuilder("Diff[");
            int size = this.lines.size();
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(this.lines.get(i));
            }
            sb.append(']');
            return sb.toString();
        }

        private Diff addLine(String str, String str2) {
            this.lines.add(str + SettingsStringUtil.DELIMITER + str2);
            return this;
        }

        public Diff addLine(String str, String str2, Object obj, Object obj2) {
            return addLine(str + "." + str2, obj, obj2);
        }

        public Diff addLine(String str, Object obj, Object obj2) {
            return addLine(str, obj + Session.SUBSESSION_SEPARATION_CHAR + obj2);
        }
    }

    public static boolean areAllPriorityOnlyNotificationZenSoundsMuted(NotificationManager.Policy policy) {
        return (((policy.priorityCategories & 1) != 0) || ((policy.priorityCategories & 8) != 0) || ((policy.priorityCategories & 4) != 0) || ((policy.priorityCategories & 2) != 0) || ((policy.priorityCategories & 16) != 0) || ((policy.state & 1) != 0)) ? false : true;
    }

    public static boolean isZenOverridingRinger(int i, ZenModeConfig zenModeConfig) {
        if (i == 2 || i == 3) {
            return true;
        }
        return i == 1 && areAllPriorityOnlyNotificationZenSoundsMuted(zenModeConfig);
    }

    public static boolean areAllPriorityOnlyNotificationZenSoundsMuted(ZenModeConfig zenModeConfig) {
        return (zenModeConfig.allowReminders || zenModeConfig.allowCalls || zenModeConfig.allowMessages || zenModeConfig.allowEvents || zenModeConfig.allowRepeatCallers || zenModeConfig.areChannelsBypassingDnd) ? false : true;
    }

    public static boolean areAllZenBehaviorSoundsMuted(ZenModeConfig zenModeConfig) {
        return (zenModeConfig.allowAlarms || zenModeConfig.allowMedia || zenModeConfig.allowSystem || !areAllPriorityOnlyNotificationZenSoundsMuted(zenModeConfig)) ? false : true;
    }

    public static String getDescription(Context context, boolean z, ZenModeConfig zenModeConfig, boolean z2) {
        if (!z || zenModeConfig == null) {
            return null;
        }
        String string = "";
        long jTryParseCountdownConditionId = -1;
        if (zenModeConfig.manualRule != null) {
            Uri uri = zenModeConfig.manualRule.conditionId;
            if (zenModeConfig.manualRule.enabler != null) {
                String ownerCaption = getOwnerCaption(context, zenModeConfig.manualRule.enabler);
                if (!ownerCaption.isEmpty()) {
                    string = ownerCaption;
                }
            } else {
                if (uri == null) {
                    if (!z2) {
                        return null;
                    }
                    return context.getString(R.string.zen_mode_forever);
                }
                jTryParseCountdownConditionId = tryParseCountdownConditionId(uri);
                if (jTryParseCountdownConditionId > 0) {
                    string = context.getString(R.string.zen_mode_until, getFormattedTime(context, jTryParseCountdownConditionId, isToday(jTryParseCountdownConditionId), context.getUserId()));
                }
            }
        }
        for (ZenRule zenRule : zenModeConfig.automaticRules.values()) {
            if (zenRule.isAutomaticActive()) {
                if (isValidEventConditionId(zenRule.conditionId) || isValidScheduleConditionId(zenRule.conditionId)) {
                    long automaticRuleEndTime = parseAutomaticRuleEndTime(context, zenRule.conditionId);
                    if (automaticRuleEndTime > jTryParseCountdownConditionId) {
                        string = zenRule.name;
                        jTryParseCountdownConditionId = automaticRuleEndTime;
                    }
                } else {
                    return zenRule.name;
                }
            }
        }
        if (string.equals("")) {
            return null;
        }
        return string;
    }

    private static long parseAutomaticRuleEndTime(Context context, Uri uri) {
        if (isValidEventConditionId(uri)) {
            return Long.MAX_VALUE;
        }
        if (isValidScheduleConditionId(uri)) {
            ScheduleCalendar scheduleCalendar = toScheduleCalendar(uri);
            long nextChangeTime = scheduleCalendar.getNextChangeTime(System.currentTimeMillis());
            if (scheduleCalendar.exitAtAlarm()) {
                long nextAlarm = getNextAlarm(context);
                scheduleCalendar.maybeSetNextAlarm(System.currentTimeMillis(), nextAlarm);
                if (scheduleCalendar.shouldExitForAlarm(nextChangeTime)) {
                    return nextAlarm;
                }
            }
            return nextChangeTime;
        }
        return -1L;
    }

    private static long getNextAlarm(Context context) {
        AlarmManager.AlarmClockInfo nextAlarmClock = ((AlarmManager) context.getSystemService("alarm")).getNextAlarmClock(context.getUserId());
        if (nextAlarmClock != null) {
            return nextAlarmClock.getTriggerTime();
        }
        return 0L;
    }
}
