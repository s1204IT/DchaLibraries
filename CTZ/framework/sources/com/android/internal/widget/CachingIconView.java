package com.android.internal.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.widget.ImageView;
import android.widget.RemoteViews;
import java.util.Objects;

@RemoteViews.RemoteView
public class CachingIconView extends ImageView {
    private int mDesiredVisibility;
    private boolean mForceHidden;
    private boolean mInternalSetDrawable;
    private String mLastPackage;
    private int mLastResId;

    public CachingIconView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    @RemotableViewMethod(asyncImpl = "setImageIconAsync")
    public void setImageIcon(Icon icon) {
        if (!testAndSetCache(icon)) {
            this.mInternalSetDrawable = true;
            super.setImageIcon(icon);
            this.mInternalSetDrawable = false;
        }
    }

    @Override
    public Runnable setImageIconAsync(Icon icon) {
        resetCache();
        return super.setImageIconAsync(icon);
    }

    @Override
    @RemotableViewMethod(asyncImpl = "setImageResourceAsync")
    public void setImageResource(int i) {
        if (!testAndSetCache(i)) {
            this.mInternalSetDrawable = true;
            super.setImageResource(i);
            this.mInternalSetDrawable = false;
        }
    }

    @Override
    public Runnable setImageResourceAsync(int i) {
        resetCache();
        return super.setImageResourceAsync(i);
    }

    @Override
    @RemotableViewMethod(asyncImpl = "setImageURIAsync")
    public void setImageURI(Uri uri) {
        resetCache();
        super.setImageURI(uri);
    }

    @Override
    public Runnable setImageURIAsync(Uri uri) {
        resetCache();
        return super.setImageURIAsync(uri);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (!this.mInternalSetDrawable) {
            resetCache();
        }
        super.setImageDrawable(drawable);
    }

    @Override
    @RemotableViewMethod
    public void setImageBitmap(Bitmap bitmap) {
        resetCache();
        super.setImageBitmap(bitmap);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        resetCache();
    }

    private synchronized boolean testAndSetCache(Icon icon) {
        boolean z = false;
        if (icon != null) {
            if (icon.getType() == 2) {
                String strNormalizeIconPackage = normalizeIconPackage(icon);
                if (this.mLastResId != 0 && icon.getResId() == this.mLastResId && Objects.equals(strNormalizeIconPackage, this.mLastPackage)) {
                    z = true;
                }
                this.mLastPackage = strNormalizeIconPackage;
                this.mLastResId = icon.getResId();
                return z;
            }
        }
        resetCache();
        return false;
    }

    private synchronized boolean testAndSetCache(int i) {
        boolean z;
        z = false;
        if (i != 0) {
            try {
                if (this.mLastResId != 0 && i == this.mLastResId && this.mLastPackage == null) {
                    z = true;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        this.mLastPackage = null;
        this.mLastResId = i;
        return z;
    }

    private String normalizeIconPackage(Icon icon) {
        if (icon == null) {
            return null;
        }
        String resPackage = icon.getResPackage();
        if (TextUtils.isEmpty(resPackage) || resPackage.equals(this.mContext.getPackageName())) {
            return null;
        }
        return resPackage;
    }

    private synchronized void resetCache() {
        this.mLastResId = 0;
        this.mLastPackage = null;
    }

    public void setForceHidden(boolean z) {
        this.mForceHidden = z;
        updateVisibility();
    }

    @Override
    @RemotableViewMethod
    public void setVisibility(int i) {
        this.mDesiredVisibility = i;
        updateVisibility();
    }

    private void updateVisibility() {
        super.setVisibility((this.mDesiredVisibility == 0 && this.mForceHidden) ? 4 : this.mDesiredVisibility);
    }
}
