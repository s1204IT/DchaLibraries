package com.android.server.display;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.RingBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BrightnessTracker {
    private static final String AMBIENT_BRIGHTNESS_STATS_FILE = "ambient_brightness_stats.xml";
    private static final String ATTR_BATTERY_LEVEL = "batteryLevel";
    private static final String ATTR_COLOR_TEMPERATURE = "colorTemperature";
    private static final String ATTR_DEFAULT_CONFIG = "defaultConfig";
    private static final String ATTR_LAST_NITS = "lastNits";
    private static final String ATTR_LUX = "lux";
    private static final String ATTR_LUX_TIMESTAMPS = "luxTimestamps";
    private static final String ATTR_NIGHT_MODE = "nightMode";
    private static final String ATTR_NITS = "nits";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_POWER_SAVE = "powerSaveFactor";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_USER = "user";
    private static final String ATTR_USER_POINT = "userPoint";
    static final boolean DEBUG = false;
    private static final String EVENTS_FILE = "brightness_events.xml";
    private static final int MAX_EVENTS = 100;
    private static final int MSG_BACKGROUND_START = 0;
    private static final int MSG_BRIGHTNESS_CHANGED = 1;
    private static final int MSG_START_SENSOR_LISTENER = 3;
    private static final int MSG_STOP_SENSOR_LISTENER = 2;
    static final String TAG = "BrightnessTracker";
    private static final String TAG_EVENT = "event";
    private static final String TAG_EVENTS = "events";
    private AmbientBrightnessStatsTracker mAmbientBrightnessStatsTracker;
    private final Handler mBgHandler;
    private BroadcastReceiver mBroadcastReceiver;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    @GuardedBy("mEventsLock")
    private boolean mEventsDirty;
    private final Injector mInjector;
    private SensorListener mSensorListener;
    private boolean mSensorRegistered;
    private SettingsObserver mSettingsObserver;

    @GuardedBy("mDataCollectionLock")
    private boolean mStarted;
    private final UserManager mUserManager;
    private volatile boolean mWriteBrightnessTrackerStateScheduled;
    private static final long MAX_EVENT_AGE = TimeUnit.DAYS.toMillis(30);
    private static final long LUX_EVENT_HORIZON = TimeUnit.SECONDS.toNanos(10);
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private final Object mEventsLock = new Object();

    @GuardedBy("mEventsLock")
    private RingBuffer<BrightnessChangeEvent> mEvents = new RingBuffer<>(BrightnessChangeEvent.class, 100);
    private int mCurrentUserId = -10000;
    private final Object mDataCollectionLock = new Object();

    @GuardedBy("mDataCollectionLock")
    private Deque<LightData> mLastSensorReadings = new ArrayDeque();

    @GuardedBy("mDataCollectionLock")
    private float mLastBatteryLevel = Float.NaN;

    @GuardedBy("mDataCollectionLock")
    private float mLastBrightness = -1.0f;

    public BrightnessTracker(Context context, Injector injector) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        if (injector != null) {
            this.mInjector = injector;
        } else {
            this.mInjector = new Injector();
        }
        this.mBgHandler = new TrackerHandler(this.mInjector.getBackgroundHandler().getLooper());
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
    }

    public void start(float f) {
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mBgHandler.obtainMessage(0, Float.valueOf(f)).sendToTarget();
    }

    private void backgroundStart(float f) throws Throwable {
        readEvents();
        readAmbientBrightnessStats();
        this.mSensorListener = new SensorListener();
        this.mSettingsObserver = new SettingsObserver(this.mBgHandler);
        this.mInjector.registerBrightnessModeObserver(this.mContentResolver, this.mSettingsObserver);
        startSensorListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mBroadcastReceiver = new Receiver();
        this.mInjector.registerReceiver(this.mContext, this.mBroadcastReceiver, intentFilter);
        this.mInjector.scheduleIdleJob(this.mContext);
        synchronized (this.mDataCollectionLock) {
            this.mLastBrightness = f;
            this.mStarted = true;
        }
    }

    @VisibleForTesting
    void stop() {
        this.mBgHandler.removeMessages(0);
        stopSensorListener();
        this.mInjector.unregisterSensorListener(this.mContext, this.mSensorListener);
        this.mInjector.unregisterBrightnessModeObserver(this.mContext, this.mSettingsObserver);
        this.mInjector.unregisterReceiver(this.mContext, this.mBroadcastReceiver);
        this.mInjector.cancelIdleJob(this.mContext);
        synchronized (this.mDataCollectionLock) {
            this.mStarted = false;
        }
    }

    public void onSwitchUser(int i) {
        this.mCurrentUserId = i;
    }

    public ParceledListSlice<BrightnessChangeEvent> getEvents(int i, boolean z) {
        BrightnessChangeEvent[] brightnessChangeEventArr;
        synchronized (this.mEventsLock) {
            brightnessChangeEventArr = (BrightnessChangeEvent[]) this.mEvents.toArray();
        }
        int[] profileIds = this.mInjector.getProfileIds(this.mUserManager, i);
        HashMap map = new HashMap();
        int i2 = 0;
        while (true) {
            boolean z2 = true;
            if (i2 >= profileIds.length) {
                break;
            }
            int i3 = profileIds[i2];
            if (z && i3 == i) {
                z2 = false;
            }
            map.put(Integer.valueOf(profileIds[i2]), Boolean.valueOf(z2));
            i2++;
        }
        ArrayList arrayList = new ArrayList(brightnessChangeEventArr.length);
        for (int i4 = 0; i4 < brightnessChangeEventArr.length; i4++) {
            Boolean bool = (Boolean) map.get(Integer.valueOf(brightnessChangeEventArr[i4].userId));
            if (bool != null) {
                if (!bool.booleanValue()) {
                    arrayList.add(brightnessChangeEventArr[i4]);
                } else {
                    arrayList.add(new BrightnessChangeEvent(brightnessChangeEventArr[i4], true));
                }
            }
        }
        return new ParceledListSlice<>(arrayList);
    }

    public void persistBrightnessTrackerState() {
        scheduleWriteBrightnessTrackerState();
    }

    public void notifyBrightnessChanged(float f, boolean z, float f2, boolean z2, boolean z3) {
        this.mBgHandler.obtainMessage(1, z ? 1 : 0, 0, new BrightnessChangeValues(f, f2, z2, z3, this.mInjector.currentTimeMillis())).sendToTarget();
    }

    private void handleBrightnessChanged(float f, boolean z, float f2, boolean z2, boolean z3, long j) {
        synchronized (this.mDataCollectionLock) {
            if (this.mStarted) {
                float f3 = this.mLastBrightness;
                this.mLastBrightness = f;
                if (z) {
                    BrightnessChangeEvent.Builder builder = new BrightnessChangeEvent.Builder();
                    builder.setBrightness(f);
                    builder.setTimeStamp(j);
                    builder.setPowerBrightnessFactor(f2);
                    builder.setUserBrightnessPoint(z2);
                    builder.setIsDefaultBrightnessConfig(z3);
                    int size = this.mLastSensorReadings.size();
                    if (size == 0) {
                        return;
                    }
                    float[] fArr = new float[size];
                    long[] jArr = new long[size];
                    long jCurrentTimeMillis = this.mInjector.currentTimeMillis();
                    long jElapsedRealtimeNanos = this.mInjector.elapsedRealtimeNanos();
                    int i = 0;
                    for (LightData lightData : this.mLastSensorReadings) {
                        fArr[i] = lightData.lux;
                        int i2 = i;
                        jArr[i2] = jCurrentTimeMillis - TimeUnit.NANOSECONDS.toMillis(jElapsedRealtimeNanos - lightData.timestamp);
                        i = i2 + 1;
                    }
                    builder.setLuxValues(fArr);
                    builder.setLuxTimestamps(jArr);
                    builder.setBatteryLevel(this.mLastBatteryLevel);
                    builder.setLastBrightness(f3);
                    try {
                        ActivityManager.StackInfo focusedStack = this.mInjector.getFocusedStack();
                        if (focusedStack != null && focusedStack.topActivity != null) {
                            builder.setUserId(focusedStack.userId);
                            builder.setPackageName(focusedStack.topActivity.getPackageName());
                            builder.setNightMode(this.mInjector.getSecureIntForUser(this.mContentResolver, "night_display_activated", 0, -2) == 1);
                            builder.setColorTemperature(this.mInjector.getSecureIntForUser(this.mContentResolver, "night_display_color_temperature", 0, -2));
                            BrightnessChangeEvent brightnessChangeEventBuild = builder.build();
                            synchronized (this.mEventsLock) {
                                this.mEventsDirty = true;
                                this.mEvents.append(brightnessChangeEventBuild);
                            }
                        }
                    } catch (RemoteException e) {
                    }
                }
            }
        }
    }

    private void startSensorListener() {
        if (!this.mSensorRegistered && this.mInjector.isInteractive(this.mContext) && this.mInjector.isBrightnessModeAutomatic(this.mContentResolver)) {
            this.mAmbientBrightnessStatsTracker.start();
            this.mSensorRegistered = true;
            this.mInjector.registerSensorListener(this.mContext, this.mSensorListener, this.mInjector.getBackgroundHandler());
        }
    }

    private void stopSensorListener() {
        if (this.mSensorRegistered) {
            this.mAmbientBrightnessStatsTracker.stop();
            this.mInjector.unregisterSensorListener(this.mContext, this.mSensorListener);
            this.mSensorRegistered = false;
        }
    }

    private void scheduleWriteBrightnessTrackerState() {
        if (!this.mWriteBrightnessTrackerStateScheduled) {
            this.mBgHandler.post(new Runnable() {
                @Override
                public final void run() {
                    BrightnessTracker.lambda$scheduleWriteBrightnessTrackerState$0(this.f$0);
                }
            });
            this.mWriteBrightnessTrackerStateScheduled = true;
        }
    }

    public static void lambda$scheduleWriteBrightnessTrackerState$0(BrightnessTracker brightnessTracker) {
        brightnessTracker.mWriteBrightnessTrackerStateScheduled = false;
        brightnessTracker.writeEvents();
        brightnessTracker.writeAmbientBrightnessStats();
    }

    private void writeEvents() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        synchronized (this.mEventsLock) {
            if (this.mEventsDirty) {
                AtomicFile file = this.mInjector.getFile(EVENTS_FILE);
                if (file == null) {
                    return;
                }
                if (this.mEvents.isEmpty()) {
                    if (file.exists()) {
                        file.delete();
                    }
                    this.mEventsDirty = false;
                } else {
                    try {
                        fileOutputStreamStartWrite = file.startWrite();
                    } catch (IOException e2) {
                        fileOutputStreamStartWrite = null;
                        e = e2;
                    }
                    try {
                        writeEventsLocked(fileOutputStreamStartWrite);
                        file.finishWrite(fileOutputStreamStartWrite);
                        this.mEventsDirty = false;
                    } catch (IOException e3) {
                        e = e3;
                        file.failWrite(fileOutputStreamStartWrite);
                        Slog.e(TAG, "Failed to write change mEvents.", e);
                    }
                }
            }
        }
    }

    private void writeAmbientBrightnessStats() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        AtomicFile file = this.mInjector.getFile(AMBIENT_BRIGHTNESS_STATS_FILE);
        if (file == null) {
            return;
        }
        try {
            fileOutputStreamStartWrite = file.startWrite();
            try {
                this.mAmbientBrightnessStatsTracker.writeStats(fileOutputStreamStartWrite);
                file.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e2) {
                e = e2;
                file.failWrite(fileOutputStreamStartWrite);
                Slog.e(TAG, "Failed to write ambient brightness stats.", e);
            }
        } catch (IOException e3) {
            fileOutputStreamStartWrite = null;
            e = e3;
        }
    }

    private void readEvents() {
        FileInputStream fileInputStreamOpenRead;
        IOException e;
        synchronized (this.mEventsLock) {
            this.mEventsDirty = true;
            this.mEvents.clear();
            AtomicFile file = this.mInjector.getFile(EVENTS_FILE);
            if (file != null && file.exists()) {
                try {
                    fileInputStreamOpenRead = file.openRead();
                    try {
                        try {
                            readEventsLocked(fileInputStreamOpenRead);
                        } catch (IOException e2) {
                            e = e2;
                            file.delete();
                            Slog.e(TAG, "Failed to read change mEvents.", e);
                        }
                    } catch (Throwable th) {
                        th = th;
                        IoUtils.closeQuietly(fileInputStreamOpenRead);
                        throw th;
                    }
                } catch (IOException e3) {
                    fileInputStreamOpenRead = null;
                    e = e3;
                } catch (Throwable th2) {
                    th = th2;
                    fileInputStreamOpenRead = null;
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    throw th;
                }
                IoUtils.closeQuietly(fileInputStreamOpenRead);
            }
        }
    }

    private void readAmbientBrightnessStats() throws Throwable {
        FileInputStream fileInputStreamOpenRead;
        IOException e;
        this.mAmbientBrightnessStatsTracker = new AmbientBrightnessStatsTracker(this.mUserManager, null);
        AtomicFile file = this.mInjector.getFile(AMBIENT_BRIGHTNESS_STATS_FILE);
        if (file == null || !file.exists()) {
            return;
        }
        try {
            fileInputStreamOpenRead = file.openRead();
            try {
                try {
                    this.mAmbientBrightnessStatsTracker.readStats(fileInputStreamOpenRead);
                } catch (IOException e2) {
                    e = e2;
                    file.delete();
                    Slog.e(TAG, "Failed to read ambient brightness stats.", e);
                }
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(fileInputStreamOpenRead);
                throw th;
            }
        } catch (IOException e3) {
            e = e3;
            fileInputStreamOpenRead = null;
        } catch (Throwable th2) {
            th = th2;
            fileInputStreamOpenRead = null;
            IoUtils.closeQuietly(fileInputStreamOpenRead);
            throw th;
        }
        IoUtils.closeQuietly(fileInputStreamOpenRead);
    }

    @GuardedBy("mEventsLock")
    @VisibleForTesting
    void writeEventsLocked(OutputStream outputStream) throws IOException {
        FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
        fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
        fastXmlSerializer.startDocument(null, true);
        fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        fastXmlSerializer.startTag(null, TAG_EVENTS);
        BrightnessChangeEvent[] brightnessChangeEventArr = (BrightnessChangeEvent[]) this.mEvents.toArray();
        this.mEvents.clear();
        long jCurrentTimeMillis = this.mInjector.currentTimeMillis() - MAX_EVENT_AGE;
        for (int i = 0; i < brightnessChangeEventArr.length; i++) {
            int userSerialNumber = this.mInjector.getUserSerialNumber(this.mUserManager, brightnessChangeEventArr[i].userId);
            if (userSerialNumber != -1 && brightnessChangeEventArr[i].timeStamp > jCurrentTimeMillis) {
                this.mEvents.append(brightnessChangeEventArr[i]);
                fastXmlSerializer.startTag(null, TAG_EVENT);
                fastXmlSerializer.attribute(null, ATTR_NITS, Float.toString(brightnessChangeEventArr[i].brightness));
                fastXmlSerializer.attribute(null, "timestamp", Long.toString(brightnessChangeEventArr[i].timeStamp));
                fastXmlSerializer.attribute(null, "packageName", brightnessChangeEventArr[i].packageName);
                fastXmlSerializer.attribute(null, ATTR_USER, Integer.toString(userSerialNumber));
                fastXmlSerializer.attribute(null, ATTR_BATTERY_LEVEL, Float.toString(brightnessChangeEventArr[i].batteryLevel));
                fastXmlSerializer.attribute(null, ATTR_NIGHT_MODE, Boolean.toString(brightnessChangeEventArr[i].nightMode));
                fastXmlSerializer.attribute(null, ATTR_COLOR_TEMPERATURE, Integer.toString(brightnessChangeEventArr[i].colorTemperature));
                fastXmlSerializer.attribute(null, ATTR_LAST_NITS, Float.toString(brightnessChangeEventArr[i].lastBrightness));
                fastXmlSerializer.attribute(null, ATTR_DEFAULT_CONFIG, Boolean.toString(brightnessChangeEventArr[i].isDefaultBrightnessConfig));
                fastXmlSerializer.attribute(null, ATTR_POWER_SAVE, Float.toString(brightnessChangeEventArr[i].powerBrightnessFactor));
                fastXmlSerializer.attribute(null, ATTR_USER_POINT, Boolean.toString(brightnessChangeEventArr[i].isUserSetBrightness));
                StringBuilder sb = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                for (int i2 = 0; i2 < brightnessChangeEventArr[i].luxValues.length; i2++) {
                    if (i2 > 0) {
                        sb.append(',');
                        sb2.append(',');
                    }
                    sb.append(Float.toString(brightnessChangeEventArr[i].luxValues[i2]));
                    sb2.append(Long.toString(brightnessChangeEventArr[i].luxTimestamps[i2]));
                }
                fastXmlSerializer.attribute(null, ATTR_LUX, sb.toString());
                fastXmlSerializer.attribute(null, ATTR_LUX_TIMESTAMPS, sb2.toString());
                fastXmlSerializer.endTag(null, TAG_EVENT);
            }
        }
        fastXmlSerializer.endTag(null, TAG_EVENTS);
        fastXmlSerializer.endDocument();
        outputStream.flush();
    }

    @GuardedBy("mEventsLock")
    @VisibleForTesting
    void readEventsLocked(InputStream inputStream) throws IOException {
        int next;
        try {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
            do {
                next = xmlPullParserNewPullParser.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            String name = xmlPullParserNewPullParser.getName();
            if (!TAG_EVENTS.equals(name)) {
                throw new XmlPullParserException("Events not found in brightness tracker file " + name);
            }
            long jCurrentTimeMillis = this.mInjector.currentTimeMillis() - MAX_EVENT_AGE;
            xmlPullParserNewPullParser.next();
            int depth = xmlPullParserNewPullParser.getDepth();
            while (true) {
                int next2 = xmlPullParserNewPullParser.next();
                if (next2 != 1) {
                    if (next2 != 3 || xmlPullParserNewPullParser.getDepth() > depth) {
                        if (next2 != 3 && next2 != 4 && TAG_EVENT.equals(xmlPullParserNewPullParser.getName())) {
                            BrightnessChangeEvent.Builder builder = new BrightnessChangeEvent.Builder();
                            builder.setBrightness(Float.parseFloat(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NITS)));
                            builder.setTimeStamp(Long.parseLong(xmlPullParserNewPullParser.getAttributeValue(null, "timestamp")));
                            builder.setPackageName(xmlPullParserNewPullParser.getAttributeValue(null, "packageName"));
                            builder.setUserId(this.mInjector.getUserId(this.mUserManager, Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_USER))));
                            builder.setBatteryLevel(Float.parseFloat(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_BATTERY_LEVEL)));
                            builder.setNightMode(Boolean.parseBoolean(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_NIGHT_MODE)));
                            builder.setColorTemperature(Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_COLOR_TEMPERATURE)));
                            builder.setLastBrightness(Float.parseFloat(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_LAST_NITS)));
                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_LUX);
                            String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_LUX_TIMESTAMPS);
                            String[] strArrSplit = attributeValue.split(",");
                            String[] strArrSplit2 = attributeValue2.split(",");
                            if (strArrSplit.length == strArrSplit2.length) {
                                float[] fArr = new float[strArrSplit.length];
                                long[] jArr = new long[strArrSplit.length];
                                for (int i = 0; i < fArr.length; i++) {
                                    fArr[i] = Float.parseFloat(strArrSplit[i]);
                                    jArr[i] = Long.parseLong(strArrSplit2[i]);
                                }
                                builder.setLuxValues(fArr);
                                builder.setLuxTimestamps(jArr);
                                String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_DEFAULT_CONFIG);
                                if (attributeValue3 != null) {
                                    builder.setIsDefaultBrightnessConfig(Boolean.parseBoolean(attributeValue3));
                                }
                                String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_POWER_SAVE);
                                if (attributeValue4 != null) {
                                    builder.setPowerBrightnessFactor(Float.parseFloat(attributeValue4));
                                } else {
                                    builder.setPowerBrightnessFactor(1.0f);
                                }
                                String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_USER_POINT);
                                if (attributeValue5 != null) {
                                    builder.setUserBrightnessPoint(Boolean.parseBoolean(attributeValue5));
                                }
                                BrightnessChangeEvent brightnessChangeEventBuild = builder.build();
                                if (brightnessChangeEventBuild.userId != -1 && brightnessChangeEventBuild.timeStamp > jCurrentTimeMillis && brightnessChangeEventBuild.luxValues.length > 0) {
                                    this.mEvents.append(brightnessChangeEventBuild);
                                }
                            }
                        }
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException | NullPointerException | NumberFormatException | XmlPullParserException e) {
            this.mEvents = new RingBuffer<>(BrightnessChangeEvent.class, 100);
            Slog.e(TAG, "Failed to parse brightness event", e);
            throw new IOException("failed to parse file", e);
        }
    }

    public void dump(final PrintWriter printWriter) {
        printWriter.println("BrightnessTracker state:");
        synchronized (this.mDataCollectionLock) {
            printWriter.println("  mStarted=" + this.mStarted);
            printWriter.println("  mLastBatteryLevel=" + this.mLastBatteryLevel);
            printWriter.println("  mLastBrightness=" + this.mLastBrightness);
            printWriter.println("  mLastSensorReadings.size=" + this.mLastSensorReadings.size());
            if (!this.mLastSensorReadings.isEmpty()) {
                printWriter.println("  mLastSensorReadings time span " + this.mLastSensorReadings.peekFirst().timestamp + "->" + this.mLastSensorReadings.peekLast().timestamp);
            }
        }
        synchronized (this.mEventsLock) {
            printWriter.println("  mEventsDirty=" + this.mEventsDirty);
            printWriter.println("  mEvents.size=" + this.mEvents.size());
            BrightnessChangeEvent[] brightnessChangeEventArr = (BrightnessChangeEvent[]) this.mEvents.toArray();
            for (int i = 0; i < brightnessChangeEventArr.length; i++) {
                printWriter.print("    " + FORMAT.format(new Date(brightnessChangeEventArr[i].timeStamp)));
                printWriter.print(", userId=" + brightnessChangeEventArr[i].userId);
                printWriter.print(", " + brightnessChangeEventArr[i].lastBrightness + "->" + brightnessChangeEventArr[i].brightness);
                StringBuilder sb = new StringBuilder();
                sb.append(", isUserSetBrightness=");
                sb.append(brightnessChangeEventArr[i].isUserSetBrightness);
                printWriter.print(sb.toString());
                printWriter.print(", powerBrightnessFactor=" + brightnessChangeEventArr[i].powerBrightnessFactor);
                printWriter.print(", isDefaultBrightnessConfig=" + brightnessChangeEventArr[i].isDefaultBrightnessConfig);
                printWriter.print(" {");
                for (int i2 = 0; i2 < brightnessChangeEventArr[i].luxValues.length; i2++) {
                    if (i2 != 0) {
                        printWriter.print(", ");
                    }
                    printWriter.print("(" + brightnessChangeEventArr[i].luxValues[i2] + "," + brightnessChangeEventArr[i].luxTimestamps[i2] + ")");
                }
                printWriter.println("}");
            }
        }
        printWriter.println("  mWriteBrightnessTrackerStateScheduled=" + this.mWriteBrightnessTrackerStateScheduled);
        this.mBgHandler.runWithScissors(new Runnable() {
            @Override
            public final void run() {
                this.f$0.dumpLocal(printWriter);
            }
        }, 1000L);
        if (this.mAmbientBrightnessStatsTracker != null) {
            printWriter.println();
            this.mAmbientBrightnessStatsTracker.dump(printWriter);
        }
    }

    private void dumpLocal(PrintWriter printWriter) {
        printWriter.println("  mSensorRegistered=" + this.mSensorRegistered);
    }

    public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats(int i) {
        ArrayList<AmbientBrightnessDayStats> userStats;
        if (this.mAmbientBrightnessStatsTracker != null && (userStats = this.mAmbientBrightnessStatsTracker.getUserStats(i)) != null) {
            return new ParceledListSlice<>(userStats);
        }
        return ParceledListSlice.emptyList();
    }

    private static class LightData {
        public float lux;
        public long timestamp;

        private LightData() {
        }
    }

    private void recordSensorEvent(SensorEvent sensorEvent) {
        long jElapsedRealtimeNanos = this.mInjector.elapsedRealtimeNanos() - LUX_EVENT_HORIZON;
        synchronized (this.mDataCollectionLock) {
            if (this.mLastSensorReadings.isEmpty() || sensorEvent.timestamp >= this.mLastSensorReadings.getLast().timestamp) {
                LightData lightDataRemoveFirst = null;
                while (!this.mLastSensorReadings.isEmpty() && this.mLastSensorReadings.getFirst().timestamp < jElapsedRealtimeNanos) {
                    lightDataRemoveFirst = this.mLastSensorReadings.removeFirst();
                }
                if (lightDataRemoveFirst != null) {
                    this.mLastSensorReadings.addFirst(lightDataRemoveFirst);
                }
                LightData lightData = new LightData();
                lightData.timestamp = sensorEvent.timestamp;
                lightData.lux = sensorEvent.values[0];
                this.mLastSensorReadings.addLast(lightData);
            }
        }
    }

    private void recordAmbientBrightnessStats(SensorEvent sensorEvent) {
        this.mAmbientBrightnessStatsTracker.add(this.mCurrentUserId, sensorEvent.values[0]);
    }

    private void batteryLevelChanged(int i, int i2) {
        synchronized (this.mDataCollectionLock) {
            this.mLastBatteryLevel = i / i2;
        }
    }

    private final class SensorListener implements SensorEventListener {
        private SensorListener() {
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            BrightnessTracker.this.recordSensorEvent(sensorEvent);
            BrightnessTracker.this.recordAmbientBrightnessStats(sensorEvent);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            if (BrightnessTracker.this.mInjector.isBrightnessModeAutomatic(BrightnessTracker.this.mContentResolver)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(3).sendToTarget();
            } else {
                BrightnessTracker.this.mBgHandler.obtainMessage(2).sendToTarget();
            }
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                BrightnessTracker.this.stop();
                BrightnessTracker.this.scheduleWriteBrightnessTrackerState();
                return;
            }
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int intExtra = intent.getIntExtra("level", -1);
                int intExtra2 = intent.getIntExtra("scale", 0);
                if (intExtra != -1 && intExtra2 != 0) {
                    BrightnessTracker.this.batteryLevelChanged(intExtra, intExtra2);
                    return;
                }
                return;
            }
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(2).sendToTarget();
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(3).sendToTarget();
            }
        }
    }

    private final class TrackerHandler extends Handler {
        public TrackerHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) throws Throwable {
            switch (message.what) {
                case 0:
                    BrightnessTracker.this.backgroundStart(((Float) message.obj).floatValue());
                    break;
                case 1:
                    BrightnessChangeValues brightnessChangeValues = (BrightnessChangeValues) message.obj;
                    BrightnessTracker.this.handleBrightnessChanged(brightnessChangeValues.brightness, message.arg1 == 1, brightnessChangeValues.powerBrightnessFactor, brightnessChangeValues.isUserSetBrightness, brightnessChangeValues.isDefaultBrightnessConfig, brightnessChangeValues.timestamp);
                    break;
                case 2:
                    BrightnessTracker.this.stopSensorListener();
                    break;
                case 3:
                    BrightnessTracker.this.startSensorListener();
                    break;
            }
        }
    }

    private static class BrightnessChangeValues {
        final float brightness;
        final boolean isDefaultBrightnessConfig;
        final boolean isUserSetBrightness;
        final float powerBrightnessFactor;
        final long timestamp;

        BrightnessChangeValues(float f, float f2, boolean z, boolean z2, long j) {
            this.brightness = f;
            this.powerBrightnessFactor = f2;
            this.isUserSetBrightness = z;
            this.isDefaultBrightnessConfig = z2;
            this.timestamp = j;
        }
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public void registerSensorListener(Context context, SensorEventListener sensorEventListener, Handler handler) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
            sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(5), 3, handler);
        }

        public void unregisterSensorListener(Context context, SensorEventListener sensorEventListener) {
            ((SensorManager) context.getSystemService(SensorManager.class)).unregisterListener(sensorEventListener);
        }

        public void registerBrightnessModeObserver(ContentResolver contentResolver, ContentObserver contentObserver) {
            contentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, contentObserver, -1);
        }

        public void unregisterBrightnessModeObserver(Context context, ContentObserver contentObserver) {
            context.getContentResolver().unregisterContentObserver(contentObserver);
        }

        public void registerReceiver(Context context, BroadcastReceiver broadcastReceiver, IntentFilter intentFilter) {
            context.registerReceiver(broadcastReceiver, intentFilter);
        }

        public void unregisterReceiver(Context context, BroadcastReceiver broadcastReceiver) {
            context.unregisterReceiver(broadcastReceiver);
        }

        public Handler getBackgroundHandler() {
            return BackgroundThread.getHandler();
        }

        public boolean isBrightnessModeAutomatic(ContentResolver contentResolver) {
            return Settings.System.getIntForUser(contentResolver, "screen_brightness_mode", 0, -2) == 1;
        }

        public int getSecureIntForUser(ContentResolver contentResolver, String str, int i, int i2) {
            return Settings.Secure.getIntForUser(contentResolver, str, i, i2);
        }

        public AtomicFile getFile(String str) {
            return new AtomicFile(new File(Environment.getDataSystemDeDirectory(), str));
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        public long elapsedRealtimeNanos() {
            return SystemClock.elapsedRealtimeNanos();
        }

        public int getUserSerialNumber(UserManager userManager, int i) {
            return userManager.getUserSerialNumber(i);
        }

        public int getUserId(UserManager userManager, int i) {
            return userManager.getUserHandle(i);
        }

        public int[] getProfileIds(UserManager userManager, int i) {
            if (userManager != null) {
                return userManager.getProfileIds(i, false);
            }
            return new int[]{i};
        }

        public ActivityManager.StackInfo getFocusedStack() throws RemoteException {
            return ActivityManager.getService().getFocusedStackInfo();
        }

        public void scheduleIdleJob(Context context) {
            BrightnessIdleJob.scheduleJob(context);
        }

        public void cancelIdleJob(Context context) {
            BrightnessIdleJob.cancelJob(context);
        }

        public boolean isInteractive(Context context) {
            return ((PowerManager) context.getSystemService(PowerManager.class)).isInteractive();
        }
    }
}
