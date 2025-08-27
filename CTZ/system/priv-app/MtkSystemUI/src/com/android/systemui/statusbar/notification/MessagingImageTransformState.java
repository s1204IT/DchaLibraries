package com.android.systemui.statusbar.notification;

import android.util.Pools;
import android.view.View;
import com.android.internal.widget.MessagingImageMessage;
import com.android.systemui.R;
import com.android.systemui.statusbar.ViewTransformationHelper;
import com.android.systemui.statusbar.notification.TransformState;

/* loaded from: classes.dex */
public class MessagingImageTransformState extends ImageTransformState {
    private static Pools.SimplePool<MessagingImageTransformState> sInstancePool = new Pools.SimplePool<>(40);
    private MessagingImageMessage mImageMessage;

    @Override // com.android.systemui.statusbar.notification.ImageTransformState, com.android.systemui.statusbar.notification.TransformState
    public void initFrom(View view, TransformState.TransformInfo transformInfo) {
        super.initFrom(view, transformInfo);
        this.mImageMessage = (MessagingImageMessage) view;
    }

    @Override // com.android.systemui.statusbar.notification.ImageTransformState, com.android.systemui.statusbar.notification.TransformState
    protected boolean sameAs(TransformState transformState) {
        if (super.sameAs(transformState)) {
            return true;
        }
        if (transformState instanceof MessagingImageTransformState) {
            return this.mImageMessage.sameAs(((MessagingImageTransformState) transformState).mImageMessage);
        }
        return false;
    }

    public static MessagingImageTransformState obtain() {
        MessagingImageTransformState messagingImageTransformState = (MessagingImageTransformState) sInstancePool.acquire();
        if (messagingImageTransformState != null) {
            return messagingImageTransformState;
        }
        return new MessagingImageTransformState();
    }

    @Override // com.android.systemui.statusbar.notification.ImageTransformState, com.android.systemui.statusbar.notification.TransformState
    protected boolean transformScale(TransformState transformState) {
        return false;
    }

    @Override // com.android.systemui.statusbar.notification.TransformState
    protected void transformViewFrom(TransformState transformState, int i, ViewTransformationHelper.CustomTransformation customTransformation, float f) {
        super.transformViewFrom(transformState, i, customTransformation, f);
        float interpolation = this.mDefaultInterpolator.getInterpolation(f);
        if ((transformState instanceof MessagingImageTransformState) && sameAs(transformState)) {
            MessagingImageMessage messagingImageMessage = ((MessagingImageTransformState) transformState).mImageMessage;
            if (f == 0.0f) {
                setStartActualWidth(messagingImageMessage.getActualWidth());
                setStartActualHeight(messagingImageMessage.getActualHeight());
            }
            this.mImageMessage.setActualWidth((int) NotificationUtils.interpolate(getStartActualWidth(), this.mImageMessage.getStaticWidth(), interpolation));
            this.mImageMessage.setActualHeight((int) NotificationUtils.interpolate(getStartActualHeight(), this.mImageMessage.getHeight(), interpolation));
        }
    }

    public int getStartActualWidth() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_actual_width);
        if (tag == null) {
            return -1;
        }
        return ((Integer) tag).intValue();
    }

    public void setStartActualWidth(int i) {
        this.mTransformedView.setTag(R.id.transformation_start_actual_width, Integer.valueOf(i));
    }

    public int getStartActualHeight() {
        Object tag = this.mTransformedView.getTag(R.id.transformation_start_actual_height);
        if (tag == null) {
            return -1;
        }
        return ((Integer) tag).intValue();
    }

    public void setStartActualHeight(int i) {
        this.mTransformedView.setTag(R.id.transformation_start_actual_height, Integer.valueOf(i));
    }

    @Override // com.android.systemui.statusbar.notification.ImageTransformState, com.android.systemui.statusbar.notification.TransformState
    public void recycle() {
        super.recycle();
        if (getClass() == MessagingImageTransformState.class) {
            sInstancePool.release(this);
        }
    }

    @Override // com.android.systemui.statusbar.notification.TransformState
    protected void resetTransformedView() {
        super.resetTransformedView();
        this.mImageMessage.setActualWidth(this.mImageMessage.getStaticWidth());
        this.mImageMessage.setActualHeight(this.mImageMessage.getHeight());
    }

    @Override // com.android.systemui.statusbar.notification.ImageTransformState, com.android.systemui.statusbar.notification.TransformState
    protected void reset() {
        super.reset();
        this.mImageMessage = null;
    }
}
