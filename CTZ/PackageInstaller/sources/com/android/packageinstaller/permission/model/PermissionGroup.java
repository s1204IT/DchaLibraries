package com.android.packageinstaller.permission.model;

import android.graphics.drawable.Drawable;

public final class PermissionGroup implements Comparable<PermissionGroup> {
    private final String mDeclaringPackage;
    private final int mGranted;
    private final Drawable mIcon;
    private final CharSequence mLabel;
    private final String mName;
    private final int mTotal;

    PermissionGroup(String str, String str2, CharSequence charSequence, Drawable drawable, int i, int i2) {
        this.mDeclaringPackage = str2;
        this.mName = str;
        this.mLabel = charSequence;
        this.mIcon = drawable;
        this.mTotal = i;
        this.mGranted = i2;
    }

    public String getName() {
        return this.mName;
    }

    public String getDeclaringPackage() {
        return this.mDeclaringPackage;
    }

    public CharSequence getLabel() {
        return this.mLabel;
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public int getTotal() {
        return this.mTotal;
    }

    public int getGranted() {
        return this.mGranted;
    }

    @Override
    public int compareTo(PermissionGroup permissionGroup) {
        return this.mLabel.toString().compareTo(permissionGroup.mLabel.toString());
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PermissionGroup permissionGroup = (PermissionGroup) obj;
        if (this.mName == null) {
            if (permissionGroup.mName != null) {
                return false;
            }
        } else if (!this.mName.equals(permissionGroup.mName)) {
            return false;
        }
        if (this.mTotal == permissionGroup.mTotal && this.mGranted == permissionGroup.mGranted) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (this.mName != null ? this.mName.hashCode() + this.mTotal : this.mTotal) + this.mGranted;
    }
}
