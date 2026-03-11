package com.mediatek.systemui.statusbar.extcb;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class IconIdWrapper implements Cloneable {
    private int mIconId;
    private Resources mResources;

    public IconIdWrapper() {
        this(null, 0);
    }

    public IconIdWrapper(Resources resources, int i) {
        this.mResources = null;
        this.mIconId = 0;
        this.mResources = resources;
        this.mIconId = i;
    }

    public Resources getResources() {
        return this.mResources;
    }

    public void setResources(Resources resources) {
        this.mResources = resources;
    }

    public int getIconId() {
        return this.mIconId;
    }

    public void setIconId(int i) {
        this.mIconId = i;
    }

    public Drawable getDrawable() {
        if (this.mResources != null && this.mIconId != 0) {
            return this.mResources.getDrawable(this.mIconId);
        }
        return null;
    }

    public IconIdWrapper m29clone() {
        try {
            IconIdWrapper iconIdWrapper = (IconIdWrapper) super.clone();
            iconIdWrapper.mResources = this.mResources;
            iconIdWrapper.mIconId = this.mIconId;
            return iconIdWrapper;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public String toString() {
        if (getResources() == null) {
            return "IconIdWrapper [mResources == null, mIconId=" + this.mIconId + "]";
        }
        return "IconIdWrapper [mResources != null, mIconId=" + this.mIconId + "]";
    }

    public int hashCode() {
        return (31 * (this.mIconId + 31)) + (this.mResources == null ? 0 : this.mResources.hashCode());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        IconIdWrapper iconIdWrapper = (IconIdWrapper) obj;
        if (this.mIconId != iconIdWrapper.mIconId) {
            return false;
        }
        if (this.mResources == null) {
            if (iconIdWrapper.mResources != null) {
                return false;
            }
        } else if (!this.mResources.equals(iconIdWrapper.mResources)) {
            return false;
        }
        return true;
    }
}
