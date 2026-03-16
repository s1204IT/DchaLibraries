package com.android.browser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PageProgressView extends ImageView {
    private Rect mBounds;
    private int mCurrentProgress;
    private Handler mHandler;
    private int mIncrement;
    private int mTargetProgress;

    public PageProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public PageProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PageProgressView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context ctx) {
        this.mBounds = new Rect(0, 0, 0, 0);
        this.mCurrentProgress = 0;
        this.mTargetProgress = 0;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 42) {
                    PageProgressView.this.mCurrentProgress = Math.min(PageProgressView.this.mTargetProgress, PageProgressView.this.mCurrentProgress + PageProgressView.this.mIncrement);
                    PageProgressView.this.mBounds.right = (PageProgressView.this.getWidth() * PageProgressView.this.mCurrentProgress) / 10000;
                    PageProgressView.this.invalidate();
                    if (PageProgressView.this.mCurrentProgress < PageProgressView.this.mTargetProgress) {
                        sendMessageDelayed(PageProgressView.this.mHandler.obtainMessage(42), 40L);
                    }
                }
            }
        };
    }

    @Override
    public void onLayout(boolean f, int l, int t, int r, int b) {
        this.mBounds.left = 0;
        this.mBounds.right = ((r - l) * this.mCurrentProgress) / 10000;
        this.mBounds.top = 0;
        this.mBounds.bottom = b - t;
    }

    void setProgress(int progress) {
        this.mCurrentProgress = this.mTargetProgress;
        this.mTargetProgress = progress;
        this.mIncrement = (this.mTargetProgress - this.mCurrentProgress) / 10;
        this.mHandler.removeMessages(42);
        this.mHandler.sendEmptyMessage(42);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Drawable d = getDrawable();
        d.setBounds(this.mBounds);
        d.draw(canvas);
    }
}
