package com.android.contacts.common.list;

import android.text.TextUtils;
import android.widget.SectionIndexer;
import java.util.Arrays;

public class ContactsSectionIndexer implements SectionIndexer {
    private int mCount;
    private int[] mPositions;
    private String[] mSections;

    public ContactsSectionIndexer(String[] sections, int[] counts) {
        if (sections == null || counts == null) {
            throw new NullPointerException();
        }
        if (sections.length != counts.length) {
            throw new IllegalArgumentException("The sections and counts arrays must have the same length");
        }
        this.mSections = sections;
        this.mPositions = new int[counts.length];
        int position = 0;
        for (int i = 0; i < counts.length; i++) {
            if (TextUtils.isEmpty(this.mSections[i])) {
                this.mSections[i] = " ";
            } else if (!this.mSections[i].equals(" ")) {
                this.mSections[i] = this.mSections[i].trim();
            }
            this.mPositions[i] = position;
            position += counts[i];
        }
        this.mCount = position;
    }

    @Override
    public Object[] getSections() {
        return this.mSections;
    }

    @Override
    public int getPositionForSection(int section) {
        if (section < 0 || section >= this.mSections.length) {
            return -1;
        }
        return this.mPositions[section];
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position < 0 || position >= this.mCount) {
            return -1;
        }
        int index = Arrays.binarySearch(this.mPositions, position);
        return index < 0 ? (-index) - 2 : index;
    }

    public void setProfileHeader(String header) {
        if (this.mSections != null) {
            if (this.mSections.length <= 0 || !header.equals(this.mSections[0])) {
                String[] tempSections = new String[this.mSections.length + 1];
                int[] tempPositions = new int[this.mPositions.length + 1];
                tempSections[0] = header;
                tempPositions[0] = 0;
                for (int i = 1; i <= this.mPositions.length; i++) {
                    tempSections[i] = this.mSections[i - 1];
                    tempPositions[i] = this.mPositions[i - 1] + 1;
                }
                this.mSections = tempSections;
                this.mPositions = tempPositions;
                this.mCount++;
            }
        }
    }
}
