package com.android.server.usage;

import android.app.usage.ConfigurationStats;
import android.app.usage.TimeSparseArray;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.content.res.Configuration;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.net.ProtocolException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

final class UsageStatsXmlV1 {
    private static final String ACTIVE_ATTR = "active";
    private static final String CLASS_ATTR = "class";
    private static final String CONFIGURATIONS_TAG = "configurations";
    private static final String CONFIG_TAG = "config";
    private static final String COUNT_ATTR = "count";
    private static final String END_TIME_ATTR = "endTime";
    private static final String EVENT_LOG_TAG = "event-log";
    private static final String EVENT_TAG = "event";
    private static final String LAST_EVENT_ATTR = "lastEvent";
    private static final String LAST_TIME_ACTIVE_ATTR = "lastTimeActive";
    private static final String PACKAGES_TAG = "packages";
    private static final String PACKAGE_ATTR = "package";
    private static final String PACKAGE_TAG = "package";
    private static final String TIME_ATTR = "time";
    private static final String TOTAL_TIME_ACTIVE_ATTR = "timeActive";
    private static final String TYPE_ATTR = "type";

    private static void loadUsageStats(XmlPullParser parser, IntervalStats statsOut) throws XmlPullParserException, IOException {
        String pkg = parser.getAttributeValue(null, "package");
        if (pkg == null) {
            throw new ProtocolException("no package attribute present");
        }
        UsageStats stats = statsOut.getOrCreateUsageStats(pkg);
        stats.mLastTimeUsed = statsOut.beginTime + XmlUtils.readLongAttribute(parser, LAST_TIME_ACTIVE_ATTR);
        stats.mTotalTimeInForeground = XmlUtils.readLongAttribute(parser, TOTAL_TIME_ACTIVE_ATTR);
        stats.mLastEvent = XmlUtils.readIntAttribute(parser, LAST_EVENT_ATTR);
    }

    private static void loadConfigStats(XmlPullParser parser, IntervalStats statsOut) throws XmlPullParserException, IOException {
        Configuration config = new Configuration();
        Configuration.readXmlAttrs(parser, config);
        ConfigurationStats configStats = statsOut.getOrCreateConfigurationStats(config);
        configStats.mLastTimeActive = statsOut.beginTime + XmlUtils.readLongAttribute(parser, LAST_TIME_ACTIVE_ATTR);
        configStats.mTotalTimeActive = XmlUtils.readLongAttribute(parser, TOTAL_TIME_ACTIVE_ATTR);
        configStats.mActivationCount = XmlUtils.readIntAttribute(parser, COUNT_ATTR);
        if (XmlUtils.readBooleanAttribute(parser, ACTIVE_ATTR)) {
            statsOut.activeConfiguration = configStats.mConfiguration;
        }
    }

    private static void loadEvent(XmlPullParser parser, IntervalStats statsOut) throws XmlPullParserException, IOException {
        String packageName = XmlUtils.readStringAttribute(parser, "package");
        if (packageName == null) {
            throw new ProtocolException("no package attribute present");
        }
        String className = XmlUtils.readStringAttribute(parser, CLASS_ATTR);
        UsageEvents.Event event = statsOut.buildEvent(packageName, className);
        event.mTimeStamp = statsOut.beginTime + XmlUtils.readLongAttribute(parser, TIME_ATTR);
        event.mEventType = XmlUtils.readIntAttribute(parser, "type");
        if (event.mEventType == 5) {
            event.mConfiguration = new Configuration();
            Configuration.readXmlAttrs(parser, event.mConfiguration);
        }
        if (statsOut.events == null) {
            statsOut.events = new TimeSparseArray<>();
        }
        statsOut.events.put(event.mTimeStamp, event);
    }

    private static void writeUsageStats(XmlSerializer xml, IntervalStats stats, UsageStats usageStats) throws IOException {
        xml.startTag(null, "package");
        XmlUtils.writeLongAttribute(xml, LAST_TIME_ACTIVE_ATTR, usageStats.mLastTimeUsed - stats.beginTime);
        XmlUtils.writeStringAttribute(xml, "package", usageStats.mPackageName);
        XmlUtils.writeLongAttribute(xml, TOTAL_TIME_ACTIVE_ATTR, usageStats.mTotalTimeInForeground);
        XmlUtils.writeIntAttribute(xml, LAST_EVENT_ATTR, usageStats.mLastEvent);
        xml.endTag(null, "package");
    }

    private static void writeConfigStats(XmlSerializer xml, IntervalStats stats, ConfigurationStats configStats, boolean isActive) throws IOException {
        xml.startTag(null, CONFIG_TAG);
        XmlUtils.writeLongAttribute(xml, LAST_TIME_ACTIVE_ATTR, configStats.mLastTimeActive - stats.beginTime);
        XmlUtils.writeLongAttribute(xml, TOTAL_TIME_ACTIVE_ATTR, configStats.mTotalTimeActive);
        XmlUtils.writeIntAttribute(xml, COUNT_ATTR, configStats.mActivationCount);
        if (isActive) {
            XmlUtils.writeBooleanAttribute(xml, ACTIVE_ATTR, true);
        }
        Configuration.writeXmlAttrs(xml, configStats.mConfiguration);
        xml.endTag(null, CONFIG_TAG);
    }

    private static void writeEvent(XmlSerializer xml, IntervalStats stats, UsageEvents.Event event) throws IOException {
        xml.startTag(null, EVENT_TAG);
        XmlUtils.writeLongAttribute(xml, TIME_ATTR, event.mTimeStamp - stats.beginTime);
        XmlUtils.writeStringAttribute(xml, "package", event.mPackage);
        if (event.mClass != null) {
            XmlUtils.writeStringAttribute(xml, CLASS_ATTR, event.mClass);
        }
        XmlUtils.writeIntAttribute(xml, "type", event.mEventType);
        if (event.mEventType == 5 && event.mConfiguration != null) {
            Configuration.writeXmlAttrs(xml, event.mConfiguration);
        }
        xml.endTag(null, EVENT_TAG);
    }

    public static void read(XmlPullParser parser, IntervalStats statsOut) throws XmlPullParserException, IOException {
        statsOut.packageStats.clear();
        statsOut.configurations.clear();
        statsOut.activeConfiguration = null;
        if (statsOut.events != null) {
            statsOut.events.clear();
        }
        statsOut.endTime = XmlUtils.readLongAttribute(parser, END_TIME_ATTR);
        int outerDepth = parser.getDepth();
        while (true) {
            int eventCode = parser.next();
            if (eventCode == 1) {
                return;
            }
            if (eventCode != 3 || parser.getDepth() > outerDepth) {
                if (eventCode == 2) {
                    String tag = parser.getName();
                    switch (tag) {
                        case "package":
                            loadUsageStats(parser, statsOut);
                            break;
                        case "config":
                            loadConfigStats(parser, statsOut);
                            break;
                        case "event":
                            loadEvent(parser, statsOut);
                            break;
                    }
                }
            } else {
                return;
            }
        }
    }

    public static void write(XmlSerializer xml, IntervalStats stats) throws IOException {
        XmlUtils.writeLongAttribute(xml, END_TIME_ATTR, stats.endTime - stats.beginTime);
        xml.startTag(null, PACKAGES_TAG);
        int statsCount = stats.packageStats.size();
        for (int i = 0; i < statsCount; i++) {
            writeUsageStats(xml, stats, stats.packageStats.valueAt(i));
        }
        xml.endTag(null, PACKAGES_TAG);
        xml.startTag(null, CONFIGURATIONS_TAG);
        int configCount = stats.configurations.size();
        for (int i2 = 0; i2 < configCount; i2++) {
            boolean active = stats.activeConfiguration.equals(stats.configurations.keyAt(i2));
            writeConfigStats(xml, stats, stats.configurations.valueAt(i2), active);
        }
        xml.endTag(null, CONFIGURATIONS_TAG);
        xml.startTag(null, EVENT_LOG_TAG);
        int eventCount = stats.events != null ? stats.events.size() : 0;
        for (int i3 = 0; i3 < eventCount; i3++) {
            writeEvent(xml, stats, (UsageEvents.Event) stats.events.valueAt(i3));
        }
        xml.endTag(null, EVENT_LOG_TAG);
    }

    private UsageStatsXmlV1() {
    }
}
