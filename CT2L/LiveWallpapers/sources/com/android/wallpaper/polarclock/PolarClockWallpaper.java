package com.android.wallpaper.polarclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.text.format.Time;
import android.util.Log;
import android.util.MathUtils;
import android.view.SurfaceHolder;
import com.android.wallpaper.R;
import java.io.IOException;
import java.util.HashMap;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParserException;

public class PolarClockWallpaper extends WallpaperService {
    private IntentFilter mFilter;
    private final Handler mHandler = new Handler();

    static abstract class ClockPalette {
        public abstract int getBackgroundColor();

        public abstract int getDayColor(float f);

        public abstract int getHourColor(float f);

        public abstract String getId();

        public abstract int getMinuteColor(float f);

        public abstract int getMonthColor(float f);

        public abstract int getSecondColor(float f);

        ClockPalette() {
        }

        public static ClockPalette parseXmlPaletteTag(XmlResourceParser xrp) {
            String kind = xrp.getAttributeValue(null, "kind");
            return "cycling".equals(kind) ? CyclingClockPalette.parseXmlPaletteTag(xrp) : FixedClockPalette.parseXmlPaletteTag(xrp);
        }
    }

    static class FixedClockPalette extends ClockPalette {
        private static FixedClockPalette sFallbackPalette = null;
        protected int mBackgroundColor;
        protected int mDayColor;
        protected int mHourColor;
        protected String mId;
        protected int mMinuteColor;
        protected int mMonthColor;
        protected int mSecondColor;

        private FixedClockPalette() {
        }

        public static ClockPalette parseXmlPaletteTag(XmlResourceParser xrp) {
            FixedClockPalette pal = new FixedClockPalette();
            pal.mId = xrp.getAttributeValue(null, "id");
            String val = xrp.getAttributeValue(null, "background");
            if (val != null) {
                pal.mBackgroundColor = Color.parseColor(val);
            }
            String val2 = xrp.getAttributeValue(null, "second");
            if (val2 != null) {
                pal.mSecondColor = Color.parseColor(val2);
            }
            String val3 = xrp.getAttributeValue(null, "minute");
            if (val3 != null) {
                pal.mMinuteColor = Color.parseColor(val3);
            }
            String val4 = xrp.getAttributeValue(null, "hour");
            if (val4 != null) {
                pal.mHourColor = Color.parseColor(val4);
            }
            String val5 = xrp.getAttributeValue(null, "day");
            if (val5 != null) {
                pal.mDayColor = Color.parseColor(val5);
            }
            String val6 = xrp.getAttributeValue(null, "month");
            if (val6 != null) {
                pal.mMonthColor = Color.parseColor(val6);
            }
            if (pal.mId == null) {
                return null;
            }
            return pal;
        }

        @Override
        public int getBackgroundColor() {
            return this.mBackgroundColor;
        }

        @Override
        public int getSecondColor(float forAngle) {
            return this.mSecondColor;
        }

        @Override
        public int getMinuteColor(float forAngle) {
            return this.mMinuteColor;
        }

        @Override
        public int getHourColor(float forAngle) {
            return this.mHourColor;
        }

        @Override
        public int getDayColor(float forAngle) {
            return this.mDayColor;
        }

        @Override
        public int getMonthColor(float forAngle) {
            return this.mMonthColor;
        }

        @Override
        public String getId() {
            return this.mId;
        }
    }

    static class CyclingClockPalette extends ClockPalette {
        private static CyclingClockPalette sFallbackPalette = null;
        protected int mBackgroundColor;
        protected float mBrightness;
        private final int[] mColors = new int[720];
        protected String mId;
        protected float mSaturation;

        public static CyclingClockPalette getFallback() {
            if (sFallbackPalette == null) {
                sFallbackPalette = new CyclingClockPalette();
                sFallbackPalette.mId = "default_c";
                sFallbackPalette.mBackgroundColor = -1;
                sFallbackPalette.mSaturation = 0.8f;
                sFallbackPalette.mBrightness = 0.9f;
                sFallbackPalette.computeIntermediateColors();
            }
            return sFallbackPalette;
        }

        private CyclingClockPalette() {
        }

        private void computeIntermediateColors() {
            int[] colors = this.mColors;
            int count = colors.length;
            for (int i = 0; i < count; i++) {
                colors[i] = Color.HSBtoColor(i * 0.0013888889f, this.mSaturation, this.mBrightness);
            }
        }

        public static ClockPalette parseXmlPaletteTag(XmlResourceParser xrp) {
            CyclingClockPalette pal = new CyclingClockPalette();
            pal.mId = xrp.getAttributeValue(null, "id");
            String val = xrp.getAttributeValue(null, "background");
            if (val != null) {
                pal.mBackgroundColor = Color.parseColor(val);
            }
            String val2 = xrp.getAttributeValue(null, "saturation");
            if (val2 != null) {
                pal.mSaturation = Float.parseFloat(val2);
            }
            String val3 = xrp.getAttributeValue(null, "brightness");
            if (val3 != null) {
                pal.mBrightness = Float.parseFloat(val3);
            }
            if (pal.mId == null) {
                return null;
            }
            pal.computeIntermediateColors();
            return pal;
        }

        @Override
        public int getBackgroundColor() {
            return this.mBackgroundColor;
        }

        @Override
        public int getSecondColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) {
                forAngle = 0.0f;
            }
            return this.mColors[(int) (720.0f * forAngle)];
        }

        @Override
        public int getMinuteColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) {
                forAngle = 0.0f;
            }
            return this.mColors[(int) (720.0f * forAngle)];
        }

        @Override
        public int getHourColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) {
                forAngle = 0.0f;
            }
            return this.mColors[(int) (720.0f * forAngle)];
        }

        @Override
        public int getDayColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) {
                forAngle = 0.0f;
            }
            return this.mColors[(int) (720.0f * forAngle)];
        }

        @Override
        public int getMonthColor(float forAngle) {
            if (forAngle >= 1.0f || forAngle < 0.0f) {
                forAngle = 0.0f;
            }
            return this.mColors[(int) (720.0f * forAngle)];
        }

        @Override
        public String getId() {
            return this.mId;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mFilter = new IntentFilter();
        this.mFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public WallpaperService.Engine onCreateEngine() {
        return new ClockEngine();
    }

    class ClockEngine extends WallpaperService.Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        private Time mCalendar;
        private final Runnable mDrawClock;
        private float mOffsetX;
        private final Paint mPaint;
        private ClockPalette mPalette;
        private final HashMap<String, ClockPalette> mPalettes;
        private SharedPreferences mPrefs;
        private final RectF mRect;
        private boolean mShowSeconds;
        private boolean mVariableLineWidth;
        private boolean mVisible;
        private final BroadcastReceiver mWatcher;
        private boolean mWatcherRegistered;

        ClockEngine() {
            super(PolarClockWallpaper.this);
            this.mPalettes = new HashMap<>();
            this.mPaint = new Paint();
            this.mRect = new RectF();
            this.mWatcher = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String timeZone = intent.getStringExtra("time-zone");
                    ClockEngine.this.mCalendar = new Time(TimeZone.getTimeZone(timeZone).getID());
                    ClockEngine.this.drawFrame();
                }
            };
            this.mDrawClock = new Runnable() {
                @Override
                public void run() {
                    ClockEngine.this.drawFrame();
                }
            };
            XmlResourceParser xrp = PolarClockWallpaper.this.getResources().getXml(R.xml.polar_clock_palettes);
            try {
                for (int what = xrp.getEventType(); what != 1; what = xrp.next()) {
                    if (what == 2) {
                        if ("palette".equals(xrp.getName())) {
                            ClockPalette pal = ClockPalette.parseXmlPaletteTag(xrp);
                            if (pal.getId() != null) {
                                this.mPalettes.put(pal.getId(), pal);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e("PolarClock", "An error occured during wallpaper configuration:", e);
            } catch (XmlPullParserException e2) {
                Log.e("PolarClock", "An error occured during wallpaper configuration:", e2);
            } finally {
                xrp.close();
            }
            this.mPalette = CyclingClockPalette.getFallback();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.mPrefs = PolarClockWallpaper.this.getSharedPreferences("polar_clock_settings", 0);
            this.mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(this.mPrefs, null);
            this.mCalendar = new Time();
            this.mCalendar.setToNow();
            Paint paint = this.mPaint;
            paint.setAntiAlias(true);
            paint.setStrokeWidth(24.0f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            if (isPreview()) {
                this.mOffsetX = 0.5f;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (this.mWatcherRegistered) {
                this.mWatcherRegistered = false;
                PolarClockWallpaper.this.unregisterReceiver(this.mWatcher);
            }
            PolarClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean changed = false;
            if (key == null || "show_seconds".equals(key)) {
                this.mShowSeconds = sharedPreferences.getBoolean("show_seconds", true);
                changed = true;
            }
            if (key == null || "variable_line_width".equals(key)) {
                this.mVariableLineWidth = sharedPreferences.getBoolean("variable_line_width", true);
                changed = true;
            }
            if (key == null || "palette".equals(key)) {
                String paletteId = sharedPreferences.getString("palette", "");
                ClockPalette pal = this.mPalettes.get(paletteId);
                if (pal != null) {
                    this.mPalette = pal;
                    changed = true;
                }
            }
            if (this.mVisible && changed) {
                drawFrame();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.mVisible = visible;
            if (visible) {
                if (!this.mWatcherRegistered) {
                    this.mWatcherRegistered = true;
                    PolarClockWallpaper.this.registerReceiver(this.mWatcher, PolarClockWallpaper.this.mFilter, null, PolarClockWallpaper.this.mHandler);
                }
                this.mCalendar = new Time();
                this.mCalendar.setToNow();
            } else {
                if (this.mWatcherRegistered) {
                    this.mWatcherRegistered = false;
                    PolarClockWallpaper.this.unregisterReceiver(this.mWatcher);
                }
                PolarClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
            }
            drawFrame();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.mVisible = false;
            PolarClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            if (!isPreview()) {
                this.mOffsetX = xOffset;
                drawFrame();
            }
        }

        void drawFrame() {
            if (this.mPalette == null) {
                Log.w("PolarClockWallpaper", "no palette?!");
                return;
            }
            SurfaceHolder holder = getSurfaceHolder();
            Rect frame = holder.getSurfaceFrame();
            int width = frame.width();
            int height = frame.height();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    Time calendar = this.mCalendar;
                    Paint paint = this.mPaint;
                    long millis = System.currentTimeMillis();
                    calendar.set(millis);
                    calendar.normalize(false);
                    int s = width / 2;
                    int t = height / 2;
                    c.drawColor(this.mPalette.getBackgroundColor());
                    c.translate(s + MathUtils.lerp(s, -s, this.mOffsetX), t);
                    c.rotate(-90.0f);
                    if (height < width) {
                        c.scale(0.9f, 0.9f);
                    }
                    float size = (Math.min(width, height) * 0.5f) - 24.0f;
                    RectF rect = this.mRect;
                    rect.set(-size, -size, size, size);
                    float lastRingThickness = 24.0f;
                    if (this.mShowSeconds) {
                        float angle = (millis % 60000) / 60000.0f;
                        paint.setColor(this.mPalette.getSecondColor(angle));
                        if (this.mVariableLineWidth) {
                            lastRingThickness = 8.0f;
                            paint.setStrokeWidth(8.0f);
                        }
                        c.drawArc(rect, 0.0f, 360.0f * angle, false, paint);
                    }
                    float size2 = size - (14.0f + lastRingThickness);
                    rect.set(-size2, -size2, size2, size2);
                    float angle2 = (((calendar.minute * 60.0f) + calendar.second) % 3600.0f) / 3600.0f;
                    paint.setColor(this.mPalette.getMinuteColor(angle2));
                    if (this.mVariableLineWidth) {
                        lastRingThickness = 16.0f;
                        paint.setStrokeWidth(16.0f);
                    }
                    c.drawArc(rect, 0.0f, 360.0f * angle2, false, paint);
                    float size3 = size2 - (14.0f + lastRingThickness);
                    rect.set(-size3, -size3, size3, size3);
                    float angle3 = (((calendar.hour * 60.0f) + calendar.minute) % 1440.0f) / 1440.0f;
                    paint.setColor(this.mPalette.getHourColor(angle3));
                    if (this.mVariableLineWidth) {
                        lastRingThickness = 32.0f;
                        paint.setStrokeWidth(32.0f);
                    }
                    c.drawArc(rect, 0.0f, 360.0f * angle3, false, paint);
                    float size4 = size3 - (38.0f + lastRingThickness);
                    rect.set(-size4, -size4, size4, size4);
                    float angle4 = (calendar.monthDay - 1) / (calendar.getActualMaximum(4) - 1);
                    paint.setColor(this.mPalette.getDayColor(angle4));
                    if (this.mVariableLineWidth) {
                        lastRingThickness = 16.0f;
                        paint.setStrokeWidth(16.0f);
                    }
                    c.drawArc(rect, 0.0f, 360.0f * angle4, false, paint);
                    float size5 = size4 - (14.0f + lastRingThickness);
                    rect.set(-size5, -size5, size5, size5);
                    float angle5 = calendar.month / 11.0f;
                    paint.setColor(this.mPalette.getMonthColor(angle5));
                    if (this.mVariableLineWidth) {
                        paint.setStrokeWidth(32.0f);
                    }
                    c.drawArc(rect, 0.0f, 360.0f * angle5, false, paint);
                }
                PolarClockWallpaper.this.mHandler.removeCallbacks(this.mDrawClock);
                if (this.mVisible) {
                    if (this.mShowSeconds) {
                        PolarClockWallpaper.this.mHandler.postDelayed(this.mDrawClock, 40L);
                    } else {
                        PolarClockWallpaper.this.mHandler.postDelayed(this.mDrawClock, 2000L);
                    }
                }
            } finally {
                if (c != null) {
                    holder.unlockCanvasAndPost(c);
                }
            }
        }
    }
}
