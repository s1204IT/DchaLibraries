package com.android.settings.intelligence.search.sitemap;

import android.content.ContentValues;
import android.text.TextUtils;
import java.util.Objects;

public class SiteMapPair {
    private final String mChildClass;
    private final String mChildTitle;
    private final String mParentClass;
    private final String mParentTitle;

    public SiteMapPair(String str, String str2, String str3, String str4) {
        this.mParentClass = str;
        this.mParentTitle = str2;
        this.mChildClass = str3;
        this.mChildTitle = str4;
    }

    public int hashCode() {
        return Objects.hash(this.mParentClass, this.mChildClass);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SiteMapPair)) {
            return false;
        }
        SiteMapPair siteMapPair = (SiteMapPair) obj;
        return TextUtils.equals(this.mParentClass, siteMapPair.mParentClass) && TextUtils.equals(this.mChildClass, siteMapPair.mChildClass);
    }

    public String getParentClass() {
        return this.mParentClass;
    }

    public String getParentTitle() {
        return this.mParentTitle;
    }

    public String getChildClass() {
        return this.mChildClass;
    }

    public String getChildTitle() {
        return this.mChildTitle;
    }

    public ContentValues toContentValue() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("docid", Integer.valueOf(hashCode()));
        contentValues.put("parent_class", this.mParentClass);
        contentValues.put("parent_title", this.mParentTitle);
        contentValues.put("child_class", this.mChildClass);
        contentValues.put("child_title", this.mChildTitle);
        return contentValues;
    }
}
