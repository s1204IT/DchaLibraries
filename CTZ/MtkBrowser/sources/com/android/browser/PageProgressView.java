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

    public PageProgressView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    public PageProgressView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context);
    }

    public PageProgressView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        this.mBounds = new Rect(0, 0, 0, 0);
        this.mCurrentProgress = 0;
        this.mTargetProgress = 0;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 42) {
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
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        this.mBounds.left = 0;
        this.mBounds.right = ((i3 - i) * this.mCurrentProgress) / 10000;
        this.mBounds.top = 0;
        this.mBounds.bottom = i4 - i2;
    }

    void setProgress(int i) {
        this.mCurrentProgress = this.mTargetProgress;
        this.mTargetProgress = i;
        this.mIncrement = (this.mTargetProgress - this.mCurrentProgress) / 10;
        this.mHandler.removeMessages(42);
        this.mHandler.sendEmptyMessage(42);
    }

    @Override
    public void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        drawable.setBounds(this.mBounds);
        drawable.draw(canvas);
    }
}
