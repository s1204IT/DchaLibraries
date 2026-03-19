package com.android.server.display;

import android.hardware.display.AmbientBrightnessDayStats;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AmbientBrightnessStatsTracker {

    @VisibleForTesting
    static final float[] BUCKET_BOUNDARIES_FOR_NEW_STATS = {0.0f, 0.1f, 0.3f, 1.0f, 3.0f, 10.0f, 30.0f, 100.0f, 300.0f, 1000.0f, 3000.0f, 10000.0f};
    private static final boolean DEBUG = false;

    @VisibleForTesting
    static final int MAX_DAYS_TO_TRACK = 7;
    private static final String TAG = "AmbientBrightnessStatsTracker";
    private final AmbientBrightnessStats mAmbientBrightnessStats;
    private float mCurrentAmbientBrightness;
    private int mCurrentUserId;
    private final Injector mInjector;
    private final Timer mTimer;
    private final UserManager mUserManager;

    @VisibleForTesting
    interface Clock {
        long elapsedTimeMillis();
    }

    public AmbientBrightnessStatsTracker(UserManager userManager, Injector injector) {
        this.mUserManager = userManager;
        if (injector != null) {
            this.mInjector = injector;
        } else {
            this.mInjector = new Injector();
        }
        this.mAmbientBrightnessStats = new AmbientBrightnessStats();
        this.mTimer = new Timer(new Clock() {
            @Override
            public final long elapsedTimeMillis() {
                return this.f$0.mInjector.elapsedRealtimeMillis();
            }
        });
        this.mCurrentAmbientBrightness = -1.0f;
    }

    public synchronized void start() {
        this.mTimer.reset();
        this.mTimer.start();
    }

    public synchronized void stop() {
        if (this.mTimer.isRunning()) {
            this.mAmbientBrightnessStats.log(this.mCurrentUserId, this.mInjector.getLocalDate(), this.mCurrentAmbientBrightness, this.mTimer.totalDurationSec());
        }
        this.mTimer.reset();
        this.mCurrentAmbientBrightness = -1.0f;
    }

    public synchronized void add(int i, float f) {
        if (this.mTimer.isRunning()) {
            if (i == this.mCurrentUserId) {
                this.mAmbientBrightnessStats.log(this.mCurrentUserId, this.mInjector.getLocalDate(), this.mCurrentAmbientBrightness, this.mTimer.totalDurationSec());
            } else {
                this.mCurrentUserId = i;
            }
            this.mTimer.reset();
            this.mTimer.start();
            this.mCurrentAmbientBrightness = f;
        }
    }

    public synchronized void writeStats(OutputStream outputStream) throws IOException {
        this.mAmbientBrightnessStats.writeToXML(outputStream);
    }

    public synchronized void readStats(InputStream inputStream) throws IOException {
        this.mAmbientBrightnessStats.readFromXML(inputStream);
    }

    public synchronized ArrayList<AmbientBrightnessDayStats> getUserStats(int i) {
        return this.mAmbientBrightnessStats.getUserStats(i);
    }

    public synchronized void dump(PrintWriter printWriter) {
        printWriter.println("AmbientBrightnessStats:");
        printWriter.print(this.mAmbientBrightnessStats);
    }

    class AmbientBrightnessStats {
        private static final String ATTR_BUCKET_BOUNDARIES = "bucket-boundaries";
        private static final String ATTR_BUCKET_STATS = "bucket-stats";
        private static final String ATTR_LOCAL_DATE = "local-date";
        private static final String ATTR_USER = "user";
        private static final String TAG_AMBIENT_BRIGHTNESS_DAY_STATS = "ambient-brightness-day-stats";
        private static final String TAG_AMBIENT_BRIGHTNESS_STATS = "ambient-brightness-stats";
        private Map<Integer, Deque<AmbientBrightnessDayStats>> mStats = new HashMap();

        public AmbientBrightnessStats() {
        }

        public void log(int i, LocalDate localDate, float f, float f2) {
            getOrCreateDayStats(getOrCreateUserStats(this.mStats, i), localDate).log(f, f2);
        }

        public ArrayList<AmbientBrightnessDayStats> getUserStats(int i) {
            if (this.mStats.containsKey(Integer.valueOf(i))) {
                return new ArrayList<>(this.mStats.get(Integer.valueOf(i)));
            }
            return null;
        }

        public void writeToXML(OutputStream outputStream) throws IOException {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            LocalDate localDateMinusDays = AmbientBrightnessStatsTracker.this.mInjector.getLocalDate().minusDays(7L);
            fastXmlSerializer.startTag(null, TAG_AMBIENT_BRIGHTNESS_STATS);
            for (Map.Entry<Integer, Deque<AmbientBrightnessDayStats>> entry : this.mStats.entrySet()) {
                for (AmbientBrightnessDayStats ambientBrightnessDayStats : entry.getValue()) {
                    int userSerialNumber = AmbientBrightnessStatsTracker.this.mInjector.getUserSerialNumber(AmbientBrightnessStatsTracker.this.mUserManager, entry.getKey().intValue());
                    if (userSerialNumber != -1 && ambientBrightnessDayStats.getLocalDate().isAfter(localDateMinusDays)) {
                        fastXmlSerializer.startTag(null, TAG_AMBIENT_BRIGHTNESS_DAY_STATS);
                        fastXmlSerializer.attribute(null, ATTR_USER, Integer.toString(userSerialNumber));
                        fastXmlSerializer.attribute(null, ATTR_LOCAL_DATE, ambientBrightnessDayStats.getLocalDate().toString());
                        StringBuilder sb = new StringBuilder();
                        StringBuilder sb2 = new StringBuilder();
                        for (int i = 0; i < ambientBrightnessDayStats.getBucketBoundaries().length; i++) {
                            if (i > 0) {
                                sb.append(",");
                                sb2.append(",");
                            }
                            sb.append(ambientBrightnessDayStats.getBucketBoundaries()[i]);
                            sb2.append(ambientBrightnessDayStats.getStats()[i]);
                        }
                        fastXmlSerializer.attribute(null, ATTR_BUCKET_BOUNDARIES, sb.toString());
                        fastXmlSerializer.attribute(null, ATTR_BUCKET_STATS, sb2.toString());
                        fastXmlSerializer.endTag(null, TAG_AMBIENT_BRIGHTNESS_DAY_STATS);
                    }
                }
            }
            fastXmlSerializer.endTag(null, TAG_AMBIENT_BRIGHTNESS_STATS);
            fastXmlSerializer.endDocument();
            outputStream.flush();
        }

        public void readFromXML(InputStream inputStream) throws IOException {
            int next;
            try {
                HashMap map = new HashMap();
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(inputStream, StandardCharsets.UTF_8.name());
                do {
                    next = xmlPullParserNewPullParser.next();
                    if (next == 1) {
                        break;
                    }
                } while (next != 2);
                String name = xmlPullParserNewPullParser.getName();
                if (TAG_AMBIENT_BRIGHTNESS_STATS.equals(name)) {
                    LocalDate localDateMinusDays = AmbientBrightnessStatsTracker.this.mInjector.getLocalDate().minusDays(7L);
                    xmlPullParserNewPullParser.next();
                    int depth = xmlPullParserNewPullParser.getDepth();
                    while (true) {
                        int next2 = xmlPullParserNewPullParser.next();
                        if (next2 == 1 || (next2 == 3 && xmlPullParserNewPullParser.getDepth() <= depth)) {
                            break;
                        }
                        if (next2 != 3 && next2 != 4 && TAG_AMBIENT_BRIGHTNESS_DAY_STATS.equals(xmlPullParserNewPullParser.getName())) {
                            String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_USER);
                            LocalDate localDate = LocalDate.parse(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_LOCAL_DATE));
                            String[] strArrSplit = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_BUCKET_BOUNDARIES).split(",");
                            String[] strArrSplit2 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_BUCKET_STATS).split(",");
                            if (strArrSplit.length != strArrSplit2.length || strArrSplit.length < 1) {
                                break;
                            }
                            float[] fArr = new float[strArrSplit.length];
                            float[] fArr2 = new float[strArrSplit2.length];
                            for (int i = 0; i < strArrSplit.length; i++) {
                                fArr[i] = Float.parseFloat(strArrSplit[i]);
                                fArr2[i] = Float.parseFloat(strArrSplit2[i]);
                            }
                            int userId = AmbientBrightnessStatsTracker.this.mInjector.getUserId(AmbientBrightnessStatsTracker.this.mUserManager, Integer.parseInt(attributeValue));
                            if (userId != -1 && localDate.isAfter(localDateMinusDays)) {
                                getOrCreateUserStats(map, userId).offer(new AmbientBrightnessDayStats(localDate, fArr, fArr2));
                            }
                        }
                    }
                    throw new IOException("Invalid brightness stats string.");
                }
                throw new XmlPullParserException("Ambient brightness stats not found in tracker file " + name);
            } catch (IOException | NullPointerException | NumberFormatException | DateTimeParseException | XmlPullParserException e) {
                throw new IOException("Failed to parse brightness stats file.", e);
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Integer, Deque<AmbientBrightnessDayStats>> entry : this.mStats.entrySet()) {
                for (AmbientBrightnessDayStats ambientBrightnessDayStats : entry.getValue()) {
                    sb.append("  ");
                    sb.append(entry.getKey());
                    sb.append(" ");
                    sb.append(ambientBrightnessDayStats);
                    sb.append("\n");
                }
            }
            return sb.toString();
        }

        private Deque<AmbientBrightnessDayStats> getOrCreateUserStats(Map<Integer, Deque<AmbientBrightnessDayStats>> map, int i) {
            if (!map.containsKey(Integer.valueOf(i))) {
                map.put(Integer.valueOf(i), new ArrayDeque());
            }
            return map.get(Integer.valueOf(i));
        }

        private AmbientBrightnessDayStats getOrCreateDayStats(Deque<AmbientBrightnessDayStats> deque, LocalDate localDate) {
            AmbientBrightnessDayStats ambientBrightnessDayStatsPeekLast = deque.peekLast();
            if (ambientBrightnessDayStatsPeekLast != null && ambientBrightnessDayStatsPeekLast.getLocalDate().equals(localDate)) {
                return ambientBrightnessDayStatsPeekLast;
            }
            AmbientBrightnessDayStats ambientBrightnessDayStats = new AmbientBrightnessDayStats(localDate, AmbientBrightnessStatsTracker.BUCKET_BOUNDARIES_FOR_NEW_STATS);
            if (deque.size() == 7) {
                deque.poll();
            }
            deque.offer(ambientBrightnessDayStats);
            return ambientBrightnessDayStats;
        }
    }

    @VisibleForTesting
    static class Timer {
        private final Clock clock;
        private long startTimeMillis;
        private boolean started;

        public Timer(Clock clock) {
            this.clock = clock;
        }

        public void reset() {
            this.started = false;
        }

        public void start() {
            if (!this.started) {
                this.startTimeMillis = this.clock.elapsedTimeMillis();
                this.started = true;
            }
        }

        public boolean isRunning() {
            return this.started;
        }

        public float totalDurationSec() {
            if (this.started) {
                return (float) ((this.clock.elapsedTimeMillis() - this.startTimeMillis) / 1000.0d);
            }
            return 0.0f;
        }
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public long elapsedRealtimeMillis() {
            return SystemClock.elapsedRealtime();
        }

        public int getUserSerialNumber(UserManager userManager, int i) {
            return userManager.getUserSerialNumber(i);
        }

        public int getUserId(UserManager userManager, int i) {
            return userManager.getUserHandle(i);
        }

        public LocalDate getLocalDate() {
            return LocalDate.now();
        }
    }
}
