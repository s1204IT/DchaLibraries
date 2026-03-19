package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.EventList;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import android.util.ArrayMap;
import com.android.internal.util.XmlUtils;
import com.android.server.am.AssistDataRequester;
import com.android.server.pm.Settings;
import com.android.server.usage.IntervalStats;
import java.io.IOException;
import java.net.ProtocolException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class UsageStatsXmlV1 {
    private static final String ACTIVE_ATTR = "active";
    private static final String APP_LAUNCH_COUNT_ATTR = "appLaunchCount";
    private static final String CATEGORY_TAG = "category";
    private static final String CHOOSER_COUNT_TAG = "chosen_action";
    private static final String CLASS_ATTR = "class";
    private static final String CONFIGURATIONS_TAG = "configurations";
    private static final String CONFIG_TAG = "config";
    private static final String COUNT = "count";
    private static final String COUNT_ATTR = "count";
    private static final String END_TIME_ATTR = "endTime";
    private static final String EVENT_LOG_TAG = "event-log";
    private static final String EVENT_TAG = "event";
    private static final String FLAGS_ATTR = "flags";
    private static final String INTERACTIVE_TAG = "interactive";
    private static final String KEYGUARD_HIDDEN_TAG = "keyguard-hidden";
    private static final String KEYGUARD_SHOWN_TAG = "keyguard-shown";
    private static final String LAST_EVENT_ATTR = "lastEvent";
    private static final String LAST_TIME_ACTIVE_ATTR = "lastTimeActive";
    private static final String NAME = "name";
    private static final String NON_INTERACTIVE_TAG = "non-interactive";
    private static final String PACKAGES_TAG = "packages";
    private static final String PACKAGE_ATTR = "package";
    private static final String PACKAGE_TAG = "package";
    private static final String SHORTCUT_ID_ATTR = "shortcutId";
    private static final String STANDBY_BUCKET_ATTR = "standbyBucket";
    private static final String TAG = "UsageStatsXmlV1";
    private static final String TIME_ATTR = "time";
    private static final String TOTAL_TIME_ACTIVE_ATTR = "timeActive";
    private static final String TYPE_ATTR = "type";

    private static void loadUsageStats(XmlPullParser xmlPullParser, IntervalStats intervalStats) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_PACKAGE);
        if (attributeValue == null) {
            throw new ProtocolException("no package attribute present");
        }
        UsageStats orCreateUsageStats = intervalStats.getOrCreateUsageStats(attributeValue);
        orCreateUsageStats.mLastTimeUsed = intervalStats.beginTime + XmlUtils.readLongAttribute(xmlPullParser, LAST_TIME_ACTIVE_ATTR);
        orCreateUsageStats.mTotalTimeInForeground = XmlUtils.readLongAttribute(xmlPullParser, TOTAL_TIME_ACTIVE_ATTR);
        orCreateUsageStats.mLastEvent = XmlUtils.readIntAttribute(xmlPullParser, LAST_EVENT_ATTR);
        orCreateUsageStats.mAppLaunchCount = XmlUtils.readIntAttribute(xmlPullParser, APP_LAUNCH_COUNT_ATTR, 0);
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                String name = xmlPullParser.getName();
                if (next != 3 || !name.equals(Settings.ATTR_PACKAGE)) {
                    if (next == 2 && name.equals(CHOOSER_COUNT_TAG)) {
                        loadChooserCounts(xmlPullParser, orCreateUsageStats, XmlUtils.readStringAttribute(xmlPullParser, "name"));
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static void loadCountAndTime(XmlPullParser xmlPullParser, IntervalStats.EventTracker eventTracker) throws XmlPullParserException, IOException {
        eventTracker.count = XmlUtils.readIntAttribute(xmlPullParser, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, 0);
        eventTracker.duration = XmlUtils.readLongAttribute(xmlPullParser, TIME_ATTR, 0L);
        XmlUtils.skipCurrentTag(xmlPullParser);
    }

    private static void loadChooserCounts(XmlPullParser xmlPullParser, UsageStats usageStats, String str) throws XmlPullParserException, IOException {
        if (str == null) {
            return;
        }
        if (usageStats.mChooserCounts == null) {
            usageStats.mChooserCounts = new ArrayMap();
        }
        if (!usageStats.mChooserCounts.containsKey(str)) {
            usageStats.mChooserCounts.put(str, new ArrayMap());
        }
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                String name = xmlPullParser.getName();
                if (next != 3 || !name.equals(CHOOSER_COUNT_TAG)) {
                    if (next == 2 && name.equals(CATEGORY_TAG)) {
                        ((ArrayMap) usageStats.mChooserCounts.get(str)).put(XmlUtils.readStringAttribute(xmlPullParser, "name"), Integer.valueOf(XmlUtils.readIntAttribute(xmlPullParser, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT)));
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private static void loadConfigStats(XmlPullParser xmlPullParser, IntervalStats intervalStats) throws XmlPullParserException, IOException {
        Configuration configuration = new Configuration();
        Configuration.readXmlAttrs(xmlPullParser, configuration);
        ConfigurationStats orCreateConfigurationStats = intervalStats.getOrCreateConfigurationStats(configuration);
        orCreateConfigurationStats.mLastTimeActive = intervalStats.beginTime + XmlUtils.readLongAttribute(xmlPullParser, LAST_TIME_ACTIVE_ATTR);
        orCreateConfigurationStats.mTotalTimeActive = XmlUtils.readLongAttribute(xmlPullParser, TOTAL_TIME_ACTIVE_ATTR);
        orCreateConfigurationStats.mActivationCount = XmlUtils.readIntAttribute(xmlPullParser, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT);
        if (XmlUtils.readBooleanAttribute(xmlPullParser, ACTIVE_ATTR)) {
            intervalStats.activeConfiguration = orCreateConfigurationStats.mConfiguration;
        }
    }

    private static void loadEvent(XmlPullParser xmlPullParser, IntervalStats intervalStats) throws XmlPullParserException, IOException {
        String stringAttribute = XmlUtils.readStringAttribute(xmlPullParser, Settings.ATTR_PACKAGE);
        if (stringAttribute == null) {
            throw new ProtocolException("no package attribute present");
        }
        UsageEvents.Event eventBuildEvent = intervalStats.buildEvent(stringAttribute, XmlUtils.readStringAttribute(xmlPullParser, "class"));
        eventBuildEvent.mFlags = XmlUtils.readIntAttribute(xmlPullParser, FLAGS_ATTR, 0);
        eventBuildEvent.mTimeStamp = intervalStats.beginTime + XmlUtils.readLongAttribute(xmlPullParser, TIME_ATTR);
        eventBuildEvent.mEventType = XmlUtils.readIntAttribute(xmlPullParser, "type");
        int i = eventBuildEvent.mEventType;
        if (i == 5) {
            eventBuildEvent.mConfiguration = new Configuration();
            Configuration.readXmlAttrs(xmlPullParser, eventBuildEvent.mConfiguration);
        } else if (i == 8) {
            String stringAttribute2 = XmlUtils.readStringAttribute(xmlPullParser, SHORTCUT_ID_ATTR);
            eventBuildEvent.mShortcutId = stringAttribute2 != null ? stringAttribute2.intern() : null;
        } else if (i == 11) {
            eventBuildEvent.mBucketAndReason = XmlUtils.readIntAttribute(xmlPullParser, STANDBY_BUCKET_ATTR, 0);
        }
        if (intervalStats.events == null) {
            intervalStats.events = new EventList();
        }
        intervalStats.events.insert(eventBuildEvent);
    }

    private static void writeUsageStats(XmlSerializer xmlSerializer, IntervalStats intervalStats, UsageStats usageStats) throws IOException {
        xmlSerializer.startTag(null, Settings.ATTR_PACKAGE);
        XmlUtils.writeLongAttribute(xmlSerializer, LAST_TIME_ACTIVE_ATTR, usageStats.mLastTimeUsed - intervalStats.beginTime);
        XmlUtils.writeStringAttribute(xmlSerializer, Settings.ATTR_PACKAGE, usageStats.mPackageName);
        XmlUtils.writeLongAttribute(xmlSerializer, TOTAL_TIME_ACTIVE_ATTR, usageStats.mTotalTimeInForeground);
        XmlUtils.writeIntAttribute(xmlSerializer, LAST_EVENT_ATTR, usageStats.mLastEvent);
        if (usageStats.mAppLaunchCount > 0) {
            XmlUtils.writeIntAttribute(xmlSerializer, APP_LAUNCH_COUNT_ATTR, usageStats.mAppLaunchCount);
        }
        writeChooserCounts(xmlSerializer, usageStats);
        xmlSerializer.endTag(null, Settings.ATTR_PACKAGE);
    }

    private static void writeCountAndTime(XmlSerializer xmlSerializer, String str, int i, long j) throws IOException {
        xmlSerializer.startTag(null, str);
        XmlUtils.writeIntAttribute(xmlSerializer, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, i);
        XmlUtils.writeLongAttribute(xmlSerializer, TIME_ATTR, j);
        xmlSerializer.endTag(null, str);
    }

    private static void writeChooserCounts(XmlSerializer xmlSerializer, UsageStats usageStats) throws IOException {
        if (usageStats == null || usageStats.mChooserCounts == null || usageStats.mChooserCounts.keySet().isEmpty()) {
            return;
        }
        int size = usageStats.mChooserCounts.size();
        for (int i = 0; i < size; i++) {
            String str = (String) usageStats.mChooserCounts.keyAt(i);
            ArrayMap arrayMap = (ArrayMap) usageStats.mChooserCounts.valueAt(i);
            if (str != null && arrayMap != null && !arrayMap.isEmpty()) {
                xmlSerializer.startTag(null, CHOOSER_COUNT_TAG);
                XmlUtils.writeStringAttribute(xmlSerializer, "name", str);
                writeCountsForAction(xmlSerializer, arrayMap);
                xmlSerializer.endTag(null, CHOOSER_COUNT_TAG);
            }
        }
    }

    private static void writeCountsForAction(XmlSerializer xmlSerializer, ArrayMap<String, Integer> arrayMap) throws IOException {
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            String strKeyAt = arrayMap.keyAt(i);
            int iIntValue = arrayMap.valueAt(i).intValue();
            if (iIntValue > 0) {
                xmlSerializer.startTag(null, CATEGORY_TAG);
                XmlUtils.writeStringAttribute(xmlSerializer, "name", strKeyAt);
                XmlUtils.writeIntAttribute(xmlSerializer, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, iIntValue);
                xmlSerializer.endTag(null, CATEGORY_TAG);
            }
        }
    }

    private static void writeConfigStats(XmlSerializer xmlSerializer, IntervalStats intervalStats, ConfigurationStats configurationStats, boolean z) throws IOException {
        xmlSerializer.startTag(null, CONFIG_TAG);
        XmlUtils.writeLongAttribute(xmlSerializer, LAST_TIME_ACTIVE_ATTR, configurationStats.mLastTimeActive - intervalStats.beginTime);
        XmlUtils.writeLongAttribute(xmlSerializer, TOTAL_TIME_ACTIVE_ATTR, configurationStats.mTotalTimeActive);
        XmlUtils.writeIntAttribute(xmlSerializer, AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, configurationStats.mActivationCount);
        if (z) {
            XmlUtils.writeBooleanAttribute(xmlSerializer, ACTIVE_ATTR, true);
        }
        Configuration.writeXmlAttrs(xmlSerializer, configurationStats.mConfiguration);
        xmlSerializer.endTag(null, CONFIG_TAG);
    }

    private static void writeEvent(XmlSerializer xmlSerializer, IntervalStats intervalStats, UsageEvents.Event event) throws IOException {
        xmlSerializer.startTag(null, EVENT_TAG);
        XmlUtils.writeLongAttribute(xmlSerializer, TIME_ATTR, event.mTimeStamp - intervalStats.beginTime);
        XmlUtils.writeStringAttribute(xmlSerializer, Settings.ATTR_PACKAGE, event.mPackage);
        if (event.mClass != null) {
            XmlUtils.writeStringAttribute(xmlSerializer, "class", event.mClass);
        }
        XmlUtils.writeIntAttribute(xmlSerializer, FLAGS_ATTR, event.mFlags);
        XmlUtils.writeIntAttribute(xmlSerializer, "type", event.mEventType);
        int i = event.mEventType;
        if (i != 5) {
            if (i == 8) {
                if (event.mShortcutId != null) {
                    XmlUtils.writeStringAttribute(xmlSerializer, SHORTCUT_ID_ATTR, event.mShortcutId);
                }
            } else if (i == 11 && event.mBucketAndReason != 0) {
                XmlUtils.writeIntAttribute(xmlSerializer, STANDBY_BUCKET_ATTR, event.mBucketAndReason);
            }
        } else if (event.mConfiguration != null) {
            Configuration.writeXmlAttrs(xmlSerializer, event.mConfiguration);
        }
        xmlSerializer.endTag(null, EVENT_TAG);
    }

    public static void read(XmlPullParser xmlPullParser, IntervalStats intervalStats) throws XmlPullParserException, IOException {
        intervalStats.packageStats.clear();
        intervalStats.configurations.clear();
        intervalStats.activeConfiguration = null;
        if (intervalStats.events != null) {
            intervalStats.events.clear();
        }
        intervalStats.endTime = intervalStats.beginTime + XmlUtils.readLongAttribute(xmlPullParser, END_TIME_ATTR);
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next == 2) {
                        switch (xmlPullParser.getName()) {
                            case "interactive":
                                loadCountAndTime(xmlPullParser, intervalStats.interactiveTracker);
                                break;
                            case "non-interactive":
                                loadCountAndTime(xmlPullParser, intervalStats.nonInteractiveTracker);
                                break;
                            case "keyguard-shown":
                                loadCountAndTime(xmlPullParser, intervalStats.keyguardShownTracker);
                                break;
                            case "keyguard-hidden":
                                loadCountAndTime(xmlPullParser, intervalStats.keyguardHiddenTracker);
                                break;
                            case "package":
                                loadUsageStats(xmlPullParser, intervalStats);
                                break;
                            case "config":
                                loadConfigStats(xmlPullParser, intervalStats);
                                break;
                            case "event":
                                loadEvent(xmlPullParser, intervalStats);
                                break;
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    public static void write(XmlSerializer xmlSerializer, IntervalStats intervalStats) throws IOException {
        XmlUtils.writeLongAttribute(xmlSerializer, END_TIME_ATTR, intervalStats.endTime - intervalStats.beginTime);
        writeCountAndTime(xmlSerializer, INTERACTIVE_TAG, intervalStats.interactiveTracker.count, intervalStats.interactiveTracker.duration);
        writeCountAndTime(xmlSerializer, NON_INTERACTIVE_TAG, intervalStats.nonInteractiveTracker.count, intervalStats.nonInteractiveTracker.duration);
        writeCountAndTime(xmlSerializer, KEYGUARD_SHOWN_TAG, intervalStats.keyguardShownTracker.count, intervalStats.keyguardShownTracker.duration);
        writeCountAndTime(xmlSerializer, KEYGUARD_HIDDEN_TAG, intervalStats.keyguardHiddenTracker.count, intervalStats.keyguardHiddenTracker.duration);
        xmlSerializer.startTag(null, PACKAGES_TAG);
        int size = intervalStats.packageStats.size();
        for (int i = 0; i < size; i++) {
            writeUsageStats(xmlSerializer, intervalStats, intervalStats.packageStats.valueAt(i));
        }
        xmlSerializer.endTag(null, PACKAGES_TAG);
        xmlSerializer.startTag(null, CONFIGURATIONS_TAG);
        int size2 = intervalStats.configurations.size();
        for (int i2 = 0; i2 < size2; i2++) {
            writeConfigStats(xmlSerializer, intervalStats, intervalStats.configurations.valueAt(i2), intervalStats.activeConfiguration.equals(intervalStats.configurations.keyAt(i2)));
        }
        xmlSerializer.endTag(null, CONFIGURATIONS_TAG);
        xmlSerializer.startTag(null, EVENT_LOG_TAG);
        int size3 = intervalStats.events != null ? intervalStats.events.size() : 0;
        for (int i3 = 0; i3 < size3; i3++) {
            writeEvent(xmlSerializer, intervalStats, intervalStats.events.get(i3));
        }
        xmlSerializer.endTag(null, EVENT_LOG_TAG);
    }

    private UsageStatsXmlV1() {
    }
}
