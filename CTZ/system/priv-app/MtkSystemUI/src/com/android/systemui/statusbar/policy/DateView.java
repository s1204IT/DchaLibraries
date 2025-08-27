package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import java.util.Date;
import java.util.Locale;

/* loaded from: classes.dex */
public class DateView extends TextView {
    private final Date mCurrentTime;
    private DateFormat mDateFormat;
    private String mDatePattern;
    private BroadcastReceiver mIntentReceiver;
    private String mLastText;

    /* renamed from: com.android.systemui.statusbar.policy.DateView$1 */
    class AnonymousClass1 extends BroadcastReceiver {
        AnonymousClass1() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action)) {
                if ("android.intent.action.LOCALE_CHANGED".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                    DateView.this.getHandler().post(new Runnable() { // from class: com.android.systemui.statusbar.policy.-$$Lambda$DateView$1$I-3qZI4QmwEIAfQqo2b2oUNiPII
                        @Override // java.lang.Runnable
                        public final void run() {
                            DateView.this.mDateFormat = null;
                        }
                    });
                }
                DateView.this.getHandler().post(new Runnable() { // from class: com.android.systemui.statusbar.policy.-$$Lambda$DateView$1$v1y3JoGtv68dyea2Bk7AdwrkpMI
                    @Override // java.lang.Runnable
                    public final void run() {
                        DateView.this.updateClock();
                    }
                });
            }
        }
    }

    public DateView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCurrentTime = new Date();
        this.mIntentReceiver = new AnonymousClass1();
        TypedArray typedArrayObtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.DateView, 0, 0);
        try {
            this.mDatePattern = typedArrayObtainStyledAttributes.getString(0);
            typedArrayObtainStyledAttributes.recycle();
            if (this.mDatePattern == null) {
                this.mDatePattern = getContext().getString(R.string.system_ui_date_pattern);
            }
        } catch (Throwable th) {
            typedArrayObtainStyledAttributes.recycle();
            throw th;
        }
    }

    @Override // android.widget.TextView, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_TICK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, (Handler) Dependency.get(Dependency.TIME_TICK_HANDLER));
        updateClock();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mDateFormat = null;
        getContext().unregisterReceiver(this.mIntentReceiver);
    }

    protected void updateClock() {
        if (this.mDateFormat == null) {
            DateFormat instanceForSkeleton = DateFormat.getInstanceForSkeleton(this.mDatePattern, Locale.getDefault());
            instanceForSkeleton.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
            this.mDateFormat = instanceForSkeleton;
        }
        this.mCurrentTime.setTime(System.currentTimeMillis());
        String str = this.mDateFormat.format(this.mCurrentTime);
        if (!str.equals(this.mLastText)) {
            setText(str);
            this.mLastText = str;
        }
    }
}
