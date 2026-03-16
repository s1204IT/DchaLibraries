package com.android.systemui.recent;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public final class TaskDescription {
    final CharSequence description;
    final Intent intent;
    private Drawable mIcon;
    private CharSequence mLabel;
    private boolean mLoaded;
    private Drawable mThumbnail;
    final String packageName;
    final int persistentTaskId;
    final ResolveInfo resolveInfo;
    final int taskId;
    final int userId;

    public TaskDescription(int _taskId, int _persistentTaskId, ResolveInfo _resolveInfo, Intent _intent, String _packageName, CharSequence _description, int _userId) {
        this.resolveInfo = _resolveInfo;
        this.intent = _intent;
        this.taskId = _taskId;
        this.persistentTaskId = _persistentTaskId;
        this.description = _description;
        this.packageName = _packageName;
        this.userId = _userId;
    }

    public TaskDescription() {
        this.resolveInfo = null;
        this.intent = null;
        this.taskId = -1;
        this.persistentTaskId = -1;
        this.description = null;
        this.packageName = null;
        this.userId = -10000;
    }

    public void setLoaded(boolean loaded) {
        this.mLoaded = loaded;
    }

    public boolean isLoaded() {
        return this.mLoaded;
    }

    public boolean isNull() {
        return this.resolveInfo == null;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public void setLabel(CharSequence label) {
        this.mLabel = label;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public void setThumbnail(Drawable thumbnail) {
        this.mThumbnail = thumbnail;
    }

    public Drawable getThumbnail() {
        return this.mThumbnail;
    }
}
