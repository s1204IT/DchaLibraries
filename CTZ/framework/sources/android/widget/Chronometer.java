package android.widget;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.icu.text.MeasureFormat;
import android.icu.util.Measure;
import android.icu.util.MeasureUnit;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.RemotableViewMethod;
import android.view.View;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.Locale;

@RemoteViews.RemoteView
public class Chronometer extends TextView {
    private static final int HOUR_IN_SEC = 3600;
    private static final int MIN_IN_SEC = 60;
    private static final String TAG = "Chronometer";
    private long mBase;
    private boolean mCountDown;
    private String mFormat;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private Object[] mFormatterArgs;
    private Locale mFormatterLocale;
    private boolean mLogged;
    private long mNow;
    private OnChronometerTickListener mOnChronometerTickListener;
    private StringBuilder mRecycle;
    private boolean mRunning;
    private boolean mStarted;
    private final Runnable mTickRunnable;
    private boolean mVisible;

    public interface OnChronometerTickListener {
        void onChronometerTick(Chronometer chronometer);
    }

    public Chronometer(Context context) {
        this(context, null, 0);
    }

    public Chronometer(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public Chronometer(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public Chronometer(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFormatterArgs = new Object[1];
        this.mRecycle = new StringBuilder(8);
        this.mTickRunnable = new Runnable() {
            @Override
            public void run() {
                if (Chronometer.this.mRunning) {
                    Chronometer.this.updateText(SystemClock.elapsedRealtime());
                    Chronometer.this.dispatchChronometerTick();
                    Chronometer.this.postDelayed(Chronometer.this.mTickRunnable, 1000L);
                }
            }
        };
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.Chronometer, i, i2);
        setFormat(typedArrayObtainStyledAttributes.getString(0));
        setCountDown(typedArrayObtainStyledAttributes.getBoolean(1, false));
        typedArrayObtainStyledAttributes.recycle();
        init();
    }

    private void init() {
        this.mBase = SystemClock.elapsedRealtime();
        updateText(this.mBase);
    }

    @RemotableViewMethod
    public void setCountDown(boolean z) {
        this.mCountDown = z;
        updateText(SystemClock.elapsedRealtime());
    }

    public boolean isCountDown() {
        return this.mCountDown;
    }

    public boolean isTheFinalCountDown() {
        if (BenesseExtension.getDchaState() != 0) {
            return false;
        }
        try {
            getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://youtu.be/9jK-NcRmVcw")).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(528384));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @RemotableViewMethod
    public void setBase(long j) {
        this.mBase = j;
        dispatchChronometerTick();
        updateText(SystemClock.elapsedRealtime());
    }

    public long getBase() {
        return this.mBase;
    }

    @RemotableViewMethod
    public void setFormat(String str) {
        this.mFormat = str;
        if (str != null && this.mFormatBuilder == null) {
            this.mFormatBuilder = new StringBuilder(str.length() * 2);
        }
    }

    public String getFormat() {
        return this.mFormat;
    }

    public void setOnChronometerTickListener(OnChronometerTickListener onChronometerTickListener) {
        this.mOnChronometerTickListener = onChronometerTickListener;
    }

    public OnChronometerTickListener getOnChronometerTickListener() {
        return this.mOnChronometerTickListener;
    }

    public void start() {
        this.mStarted = true;
        updateRunning();
    }

    public void stop() {
        this.mStarted = false;
        updateRunning();
    }

    @RemotableViewMethod
    public void setStarted(boolean z) {
        this.mStarted = z;
        updateRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mVisible = false;
        updateRunning();
    }

    @Override
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        this.mVisible = i == 0;
        updateRunning();
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        updateRunning();
    }

    private synchronized void updateText(long j) {
        boolean z;
        String string;
        this.mNow = j;
        long j2 = (this.mCountDown ? this.mBase - j : j - this.mBase) / 1000;
        if (j2 < 0) {
            j2 = -j2;
            z = true;
        } else {
            z = false;
        }
        String elapsedTime = DateUtils.formatElapsedTime(this.mRecycle, j2);
        if (z) {
            elapsedTime = getResources().getString(R.string.negative_duration, elapsedTime);
        }
        if (this.mFormat != null) {
            Locale locale = Locale.getDefault();
            if (this.mFormatter == null || !locale.equals(this.mFormatterLocale)) {
                this.mFormatterLocale = locale;
                this.mFormatter = new Formatter(this.mFormatBuilder, locale);
            }
            this.mFormatBuilder.setLength(0);
            this.mFormatterArgs[0] = elapsedTime;
            try {
                this.mFormatter.format(this.mFormat, this.mFormatterArgs);
                string = this.mFormatBuilder.toString();
            } catch (IllegalFormatException e) {
                if (!this.mLogged) {
                    Log.w(TAG, "Illegal format string: " + this.mFormat);
                    this.mLogged = true;
                }
                string = elapsedTime;
            }
            setText(string);
        } else {
            string = elapsedTime;
            setText(string);
        }
    }

    private void updateRunning() {
        boolean z = this.mVisible && this.mStarted && isShown();
        if (z != this.mRunning) {
            if (z) {
                updateText(SystemClock.elapsedRealtime());
                dispatchChronometerTick();
                postDelayed(this.mTickRunnable, 1000L);
            } else {
                removeCallbacks(this.mTickRunnable);
            }
            this.mRunning = z;
        }
    }

    void dispatchChronometerTick() {
        if (this.mOnChronometerTickListener != null) {
            this.mOnChronometerTickListener.onChronometerTick(this);
        }
    }

    private static String formatDuration(long j) {
        int i;
        int i2 = (int) (j / 1000);
        if (i2 < 0) {
            i2 = -i2;
        }
        int i3 = 0;
        if (i2 >= 3600) {
            i = i2 / 3600;
            i2 -= i * 3600;
        } else {
            i = 0;
        }
        if (i2 >= 60) {
            i3 = i2 / 60;
            i2 -= i3 * 60;
        }
        ArrayList arrayList = new ArrayList();
        if (i > 0) {
            arrayList.add(new Measure(Integer.valueOf(i), MeasureUnit.HOUR));
        }
        if (i3 > 0) {
            arrayList.add(new Measure(Integer.valueOf(i3), MeasureUnit.MINUTE));
        }
        arrayList.add(new Measure(Integer.valueOf(i2), MeasureUnit.SECOND));
        return MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.WIDE).formatMeasures((Measure[]) arrayList.toArray(new Measure[arrayList.size()]));
    }

    @Override
    public CharSequence getContentDescription() {
        return formatDuration(this.mNow - this.mBase);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return Chronometer.class.getName();
    }
}
