package com.android.providers.contacts.aggregation.util;

public class MatchScore implements Comparable<MatchScore> {
    private long mAccountId;
    private long mContactId;
    private boolean mKeepIn;
    private boolean mKeepOut;
    private int mMatchCount;
    private int mPrimaryScore;
    private long mRawContactId;
    private int mSecondaryScore;

    public MatchScore(long j, long j2, long j3) {
        this.mRawContactId = j;
        this.mContactId = j2;
        this.mAccountId = j3;
    }

    public MatchScore(long j) {
        this.mRawContactId = 0L;
        this.mContactId = j;
        this.mAccountId = 0L;
    }

    public void reset(long j, long j2, long j3) {
        this.mRawContactId = j;
        this.mContactId = j2;
        this.mAccountId = j3;
        this.mKeepIn = false;
        this.mKeepOut = false;
        this.mPrimaryScore = 0;
        this.mSecondaryScore = 0;
        this.mMatchCount = 0;
    }

    public void reset(long j) {
        reset(0L, j, 0L);
    }

    public long getRawContactId() {
        return this.mRawContactId;
    }

    public long getContactId() {
        return this.mContactId;
    }

    public long getAccountId() {
        return this.mAccountId;
    }

    public void updatePrimaryScore(int i) {
        if (i > this.mPrimaryScore) {
            this.mPrimaryScore = i;
        }
        this.mMatchCount++;
    }

    public void updateSecondaryScore(int i) {
        if (i > this.mSecondaryScore) {
            this.mSecondaryScore = i;
        }
        this.mMatchCount++;
    }

    public void keepIn() {
        this.mKeepIn = true;
    }

    public void keepOut() {
        this.mKeepOut = true;
    }

    public int getScore() {
        if (this.mKeepOut) {
            return 0;
        }
        if (this.mKeepIn) {
            return 100;
        }
        return ((this.mPrimaryScore > this.mSecondaryScore ? this.mPrimaryScore : this.mSecondaryScore) * 1000) + this.mMatchCount;
    }

    public boolean isKeepIn() {
        return this.mKeepIn;
    }

    public boolean isKeepOut() {
        return this.mKeepOut;
    }

    public int getPrimaryScore() {
        return this.mPrimaryScore;
    }

    public int getSecondaryScore() {
        return this.mSecondaryScore;
    }

    public void setPrimaryScore(int i) {
        this.mPrimaryScore = i;
    }

    @Override
    public int compareTo(MatchScore matchScore) {
        return matchScore.getScore() - getScore();
    }

    public String toString() {
        return this.mRawContactId + "/" + this.mContactId + "/" + this.mAccountId + ": " + this.mPrimaryScore + "/" + this.mSecondaryScore + "(" + this.mMatchCount + ")";
    }
}
