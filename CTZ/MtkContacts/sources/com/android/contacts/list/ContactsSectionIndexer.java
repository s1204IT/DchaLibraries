package com.android.contacts.list;

import android.text.TextUtils;
import android.widget.SectionIndexer;
import java.util.Arrays;

public class ContactsSectionIndexer implements SectionIndexer {
    protected static final String BLANK_HEADER_STRING = "…";
    private int mCount;
    private int[] mPositions;
    private String[] mSections;

    public ContactsSectionIndexer(String[] strArr, int[] iArr) {
        if (strArr == null || iArr == null) {
            throw new NullPointerException();
        }
        if (strArr.length != iArr.length) {
            throw new IllegalArgumentException("The sections and counts arrays must have the same length");
        }
        this.mSections = strArr;
        this.mPositions = new int[iArr.length];
        int i = 0;
        for (int i2 = 0; i2 < iArr.length; i2++) {
            if (TextUtils.isEmpty(this.mSections[i2])) {
                this.mSections[i2] = BLANK_HEADER_STRING;
            } else if (!this.mSections[i2].equals(BLANK_HEADER_STRING)) {
                this.mSections[i2] = this.mSections[i2].trim();
            }
            this.mPositions[i2] = i;
            i += iArr[i2];
        }
        this.mCount = i;
    }

    @Override
    public Object[] getSections() {
        return this.mSections;
    }

    public int[] getPositions() {
        return this.mPositions;
    }

    @Override
    public int getPositionForSection(int i) {
        if (i < 0) {
            return -1;
        }
        if (i >= this.mSections.length) {
            return this.mCount;
        }
        return this.mPositions[i];
    }

    @Override
    public int getSectionForPosition(int i) {
        if (i < 0 || i >= this.mCount) {
            return -1;
        }
        int iBinarySearch = Arrays.binarySearch(this.mPositions, i);
        return iBinarySearch >= 0 ? iBinarySearch : (-iBinarySearch) - 2;
    }

    public void setFavoritesHeader(int i) {
        if (this.mSections == null || i == 0) {
            return;
        }
        if (this.mSections.length > 0 && this.mSections[0].isEmpty()) {
            return;
        }
        String[] strArr = new String[this.mSections.length + 1];
        int[] iArr = new int[this.mPositions.length + 1];
        strArr[0] = "";
        iArr[0] = 0;
        for (int i2 = 1; i2 <= this.mPositions.length; i2++) {
            int i3 = i2 - 1;
            strArr[i2] = this.mSections[i3];
            iArr[i2] = this.mPositions[i3] + i;
        }
        this.mSections = strArr;
        this.mPositions = iArr;
        this.mCount += i;
    }

    public void setSdnHeader(String str, int i) {
        if (this.mSections != null) {
            String[] strArr = new String[this.mSections.length + 1];
            int[] iArr = new int[this.mPositions.length + 1];
            strArr[0] = str;
            iArr[0] = 0;
            for (int i2 = 1; i2 <= this.mPositions.length; i2++) {
                int i3 = i2 - 1;
                strArr[i2] = this.mSections[i3];
                iArr[i2] = this.mPositions[i3] + i;
            }
            this.mSections = strArr;
            this.mPositions = iArr;
            this.mCount += i;
        }
    }
}
