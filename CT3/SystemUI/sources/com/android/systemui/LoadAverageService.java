package com.android.systemui;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.os.ProcessCpuTracker;

public class LoadAverageService extends Service {
    private View mView;

    private static final class CpuTracker extends ProcessCpuTracker {
        String mLoadText;
        int mLoadWidth;
        private final Paint mPaint;

        CpuTracker(Paint paint) {
            super(false);
            this.mPaint = paint;
        }

        public void onLoadChanged(float load1, float load5, float load15) {
            this.mLoadText = load1 + " / " + load5 + " / " + load15;
            this.mLoadWidth = (int) this.mPaint.measureText(this.mLoadText);
        }

        public int onMeasureProcessName(String name) {
            return (int) this.mPaint.measureText(name);
        }
    }

    private class LoadView extends View {
        private Paint mAddedPaint;
        private float mAscent;
        private int mFH;
        private Handler mHandler;
        private Paint mIrqPaint;
        private Paint mLoadPaint;
        private int mNeededHeight;
        private int mNeededWidth;
        private Paint mRemovedPaint;
        private Paint mShadow2Paint;
        private Paint mShadowPaint;
        private final CpuTracker mStats;
        private Paint mSystemPaint;
        private Paint mUserPaint;

        LoadView(Context c) {
            int textSize;
            super(c);
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what != 1) {
                        return;
                    }
                    LoadView.this.mStats.update();
                    LoadView.this.updateDisplay();
                    Message m = obtainMessage(1);
                    sendMessageDelayed(m, 2000L);
                }
            };
            setPadding(4, 4, 4, 4);
            float density = c.getResources().getDisplayMetrics().density;
            if (density < 1.0f) {
                textSize = 9;
            } else {
                textSize = (int) (10.0f * density);
                if (textSize < 10) {
                    textSize = 10;
                }
            }
            this.mLoadPaint = new Paint();
            this.mLoadPaint.setAntiAlias(true);
            this.mLoadPaint.setTextSize(textSize);
            this.mLoadPaint.setARGB(255, 255, 255, 255);
            this.mAddedPaint = new Paint();
            this.mAddedPaint.setAntiAlias(true);
            this.mAddedPaint.setTextSize(textSize);
            this.mAddedPaint.setARGB(255, 128, 255, 128);
            this.mRemovedPaint = new Paint();
            this.mRemovedPaint.setAntiAlias(true);
            this.mRemovedPaint.setStrikeThruText(true);
            this.mRemovedPaint.setTextSize(textSize);
            this.mRemovedPaint.setARGB(255, 255, 128, 128);
            this.mShadowPaint = new Paint();
            this.mShadowPaint.setAntiAlias(true);
            this.mShadowPaint.setTextSize(textSize);
            this.mShadowPaint.setARGB(192, 0, 0, 0);
            this.mLoadPaint.setShadowLayer(4.0f, 0.0f, 0.0f, -16777216);
            this.mShadow2Paint = new Paint();
            this.mShadow2Paint.setAntiAlias(true);
            this.mShadow2Paint.setTextSize(textSize);
            this.mShadow2Paint.setARGB(192, 0, 0, 0);
            this.mLoadPaint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
            this.mIrqPaint = new Paint();
            this.mIrqPaint.setARGB(128, 0, 0, 255);
            this.mIrqPaint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
            this.mSystemPaint = new Paint();
            this.mSystemPaint.setARGB(128, 255, 0, 0);
            this.mSystemPaint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
            this.mUserPaint = new Paint();
            this.mUserPaint.setARGB(128, 0, 255, 0);
            this.mSystemPaint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
            this.mAscent = this.mLoadPaint.ascent();
            float descent = this.mLoadPaint.descent();
            this.mFH = (int) ((descent - this.mAscent) + 0.5f);
            this.mStats = new CpuTracker(this.mLoadPaint);
            this.mStats.init();
            updateDisplay();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            this.mHandler.sendEmptyMessage(1);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            this.mHandler.removeMessages(1);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(this.mNeededWidth, widthMeasureSpec), resolveSize(this.mNeededHeight, heightMeasureSpec));
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int W = this.mNeededWidth;
            int RIGHT = getWidth() - 1;
            CpuTracker stats = this.mStats;
            int userTime = stats.getLastUserTime();
            int systemTime = stats.getLastSystemTime();
            int iowaitTime = stats.getLastIoWaitTime();
            int irqTime = stats.getLastIrqTime();
            int softIrqTime = stats.getLastSoftIrqTime();
            int idleTime = stats.getLastIdleTime();
            int totalTime = userTime + systemTime + iowaitTime + irqTime + softIrqTime + idleTime;
            if (totalTime == 0) {
                return;
            }
            int userW = (userTime * W) / totalTime;
            int systemW = (systemTime * W) / totalTime;
            int irqW = (((iowaitTime + irqTime) + softIrqTime) * W) / totalTime;
            int paddingRight = getPaddingRight();
            int x = RIGHT - paddingRight;
            int top = getPaddingTop() + 2;
            int bottom = (getPaddingTop() + this.mFH) - 2;
            if (irqW > 0) {
                canvas.drawRect(x - irqW, top, x, bottom, this.mIrqPaint);
                x -= irqW;
            }
            if (systemW > 0) {
                canvas.drawRect(x - systemW, top, x, bottom, this.mSystemPaint);
                x -= systemW;
            }
            if (userW > 0) {
                canvas.drawRect(x - userW, top, x, bottom, this.mUserPaint);
                int i = x - userW;
            }
            int y = getPaddingTop() - ((int) this.mAscent);
            canvas.drawText(stats.mLoadText, ((RIGHT - paddingRight) - stats.mLoadWidth) - 1, y - 1, this.mShadowPaint);
            canvas.drawText(stats.mLoadText, ((RIGHT - paddingRight) - stats.mLoadWidth) - 1, y + 1, this.mShadowPaint);
            canvas.drawText(stats.mLoadText, ((RIGHT - paddingRight) - stats.mLoadWidth) + 1, y - 1, this.mShadow2Paint);
            canvas.drawText(stats.mLoadText, ((RIGHT - paddingRight) - stats.mLoadWidth) + 1, y + 1, this.mShadow2Paint);
            canvas.drawText(stats.mLoadText, (RIGHT - paddingRight) - stats.mLoadWidth, y, this.mLoadPaint);
            int N = stats.countWorkingStats();
            for (int i2 = 0; i2 < N; i2++) {
                ProcessCpuTracker.Stats st = stats.getWorkingStats(i2);
                y += this.mFH;
                top += this.mFH;
                bottom += this.mFH;
                int userW2 = (st.rel_utime * W) / totalTime;
                int systemW2 = (st.rel_stime * W) / totalTime;
                int x2 = RIGHT - paddingRight;
                if (systemW2 > 0) {
                    canvas.drawRect(x2 - systemW2, top, x2, bottom, this.mSystemPaint);
                    x2 -= systemW2;
                }
                if (userW2 > 0) {
                    canvas.drawRect(x2 - userW2, top, x2, bottom, this.mUserPaint);
                    int i3 = x2 - userW2;
                }
                canvas.drawText(st.name, ((RIGHT - paddingRight) - st.nameWidth) - 1, y - 1, this.mShadowPaint);
                canvas.drawText(st.name, ((RIGHT - paddingRight) - st.nameWidth) - 1, y + 1, this.mShadowPaint);
                canvas.drawText(st.name, ((RIGHT - paddingRight) - st.nameWidth) + 1, y - 1, this.mShadow2Paint);
                canvas.drawText(st.name, ((RIGHT - paddingRight) - st.nameWidth) + 1, y + 1, this.mShadow2Paint);
                Paint p = this.mLoadPaint;
                if (st.added) {
                    p = this.mAddedPaint;
                }
                if (st.removed) {
                    p = this.mRemovedPaint;
                }
                canvas.drawText(st.name, (RIGHT - paddingRight) - st.nameWidth, y, p);
            }
        }

        void updateDisplay() {
            CpuTracker stats = this.mStats;
            int NW = stats.countWorkingStats();
            int maxWidth = stats.mLoadWidth;
            for (int i = 0; i < NW; i++) {
                ProcessCpuTracker.Stats st = stats.getWorkingStats(i);
                if (st.nameWidth > maxWidth) {
                    maxWidth = st.nameWidth;
                }
            }
            int neededWidth = getPaddingLeft() + getPaddingRight() + maxWidth;
            int neededHeight = getPaddingTop() + getPaddingBottom() + (this.mFH * (NW + 1));
            if (neededWidth != this.mNeededWidth || neededHeight != this.mNeededHeight) {
                this.mNeededWidth = neededWidth;
                this.mNeededHeight = neededHeight;
                requestLayout();
                return;
            }
            invalidate();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mView = new LoadView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(-1, -2, 2015, 24, -3);
        params.gravity = 8388661;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager) getSystemService("window");
        wm.addView(this.mView, params);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((WindowManager) getSystemService("window")).removeView(this.mView);
        this.mView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
