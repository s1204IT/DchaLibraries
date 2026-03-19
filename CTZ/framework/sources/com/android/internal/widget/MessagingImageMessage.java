package com.android.internal.widget;

import android.app.Notification;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pools;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.internal.R;
import java.io.IOException;

@RemoteViews.RemoteView
public class MessagingImageMessage extends ImageView implements MessagingMessage {
    private static final String TAG = "MessagingImageMessage";
    private static Pools.SimplePool<MessagingImageMessage> sInstancePool = new Pools.SynchronizedPool(10);
    private int mActualHeight;
    private int mActualWidth;
    private float mAspectRatio;
    private Drawable mDrawable;
    private final int mExtraSpacing;
    private final int mImageRounding;
    private boolean mIsIsolated;
    private final int mIsolatedSize;
    private final int mMaxImageHeight;
    private final int mMinImageHeight;
    private final Path mPath;
    private final MessagingMessageState mState;

    public MessagingImageMessage(Context context) {
        this(context, null);
    }

    public MessagingImageMessage(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public MessagingImageMessage(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public MessagingImageMessage(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mState = new MessagingMessageState(this);
        this.mPath = new Path();
        this.mMinImageHeight = context.getResources().getDimensionPixelSize(R.dimen.messaging_image_min_size);
        this.mMaxImageHeight = context.getResources().getDimensionPixelSize(R.dimen.messaging_image_max_height);
        this.mImageRounding = context.getResources().getDimensionPixelSize(R.dimen.messaging_image_rounding);
        this.mExtraSpacing = context.getResources().getDimensionPixelSize(R.dimen.messaging_image_extra_spacing);
        setMaxHeight(this.mMaxImageHeight);
        this.mIsolatedSize = getResources().getDimensionPixelSize(R.dimen.messaging_avatar_size);
    }

    @Override
    public MessagingMessageState getState() {
        return this.mState;
    }

    @Override
    public boolean setMessage(Notification.MessagingStyle.Message message) {
        super.setMessage(message);
        try {
            Drawable drawableResolveImage = LocalImageResolver.resolveImage(message.getDataUri(), getContext());
            int intrinsicHeight = drawableResolveImage.getIntrinsicHeight();
            if (intrinsicHeight == 0) {
                Log.w(TAG, "Drawable with 0 intrinsic height was returned");
                return false;
            }
            this.mDrawable = drawableResolveImage;
            this.mAspectRatio = this.mDrawable.getIntrinsicWidth() / intrinsicHeight;
            setImageDrawable(drawableResolveImage);
            setContentDescription(message.getText());
            return true;
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
            return false;
        }
    }

    static MessagingMessage createMessage(MessagingLayout messagingLayout, Notification.MessagingStyle.Message message) {
        MessagingLinearLayout messagingLinearLayout = messagingLayout.getMessagingLinearLayout();
        MessagingImageMessage messagingImageMessageAcquire = sInstancePool.acquire();
        if (messagingImageMessageAcquire == null) {
            messagingImageMessageAcquire = (MessagingImageMessage) LayoutInflater.from(messagingLayout.getContext()).inflate(R.layout.notification_template_messaging_image_message, (ViewGroup) messagingLinearLayout, false);
            messagingImageMessageAcquire.addOnLayoutChangeListener(MessagingLayout.MESSAGING_PROPERTY_ANIMATOR);
        }
        if (!messagingImageMessageAcquire.setMessage(message)) {
            messagingImageMessageAcquire.recycle();
            return MessagingTextMessage.createMessage(messagingLayout, message);
        }
        return messagingImageMessageAcquire;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(getRoundedRectPath());
        int iMax = (int) Math.max(getActualWidth(), getActualHeight() * this.mAspectRatio);
        int actualWidth = (int) ((getActualWidth() - iMax) / 2.0f);
        this.mDrawable.setBounds(actualWidth, 0, iMax + actualWidth, (int) (iMax / this.mAspectRatio));
        this.mDrawable.draw(canvas);
        canvas.restore();
    }

    public Path getRoundedRectPath() {
        int actualWidth = getActualWidth();
        int actualHeight = getActualHeight();
        this.mPath.reset();
        float f = this.mImageRounding;
        float f2 = this.mImageRounding;
        float fMin = Math.min((actualWidth + 0) / 2, f);
        float fMin2 = Math.min((actualHeight + 0) / 2, f2);
        float f3 = 0;
        float f4 = f3 + fMin2;
        this.mPath.moveTo(f3, f4);
        float f5 = f3 + fMin;
        this.mPath.quadTo(f3, f3, f5, f3);
        float f6 = actualWidth;
        float f7 = f6 - fMin;
        this.mPath.lineTo(f7, f3);
        this.mPath.quadTo(f6, f3, f6, f4);
        float f8 = actualHeight;
        float f9 = f8 - fMin2;
        this.mPath.lineTo(f6, f9);
        this.mPath.quadTo(f6, f8, f7, f8);
        this.mPath.lineTo(f5, f8);
        this.mPath.quadTo(f3, f8, f3, f9);
        this.mPath.close();
        return this.mPath;
    }

    @Override
    public void recycle() {
        super.recycle();
        setImageBitmap(null);
        this.mDrawable = null;
        sInstancePool.release(this);
    }

    public static void dropCache() {
        sInstancePool = new Pools.SynchronizedPool(10);
    }

    @Override
    public int getMeasuredType() {
        int i;
        int measuredHeight = getMeasuredHeight();
        if (this.mIsIsolated) {
            i = this.mIsolatedSize;
        } else {
            i = this.mMinImageHeight;
        }
        if (measuredHeight < i && measuredHeight != this.mDrawable.getIntrinsicHeight()) {
            return 2;
        }
        return (this.mIsIsolated || measuredHeight == this.mDrawable.getIntrinsicHeight()) ? 0 : 1;
    }

    @Override
    public void setMaxDisplayedLines(int i) {
    }

    @Override
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        if (this.mIsIsolated) {
            setMeasuredDimension(View.MeasureSpec.getSize(i), View.MeasureSpec.getSize(i2));
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setActualWidth(getStaticWidth());
        setActualHeight(getHeight());
    }

    @Override
    public int getConsumedLines() {
        return 3;
    }

    public void setActualWidth(int i) {
        this.mActualWidth = i;
        invalidate();
    }

    public int getActualWidth() {
        return this.mActualWidth;
    }

    public void setActualHeight(int i) {
        this.mActualHeight = i;
        invalidate();
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public int getStaticWidth() {
        if (this.mIsIsolated) {
            return getWidth();
        }
        return (int) (getHeight() * this.mAspectRatio);
    }

    public void setIsolated(boolean z) {
        if (this.mIsIsolated != z) {
            this.mIsIsolated = z;
            ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
            marginLayoutParams.topMargin = z ? 0 : this.mExtraSpacing;
            setLayoutParams(marginLayoutParams);
        }
    }

    @Override
    public int getExtraSpacing() {
        return this.mExtraSpacing;
    }
}
