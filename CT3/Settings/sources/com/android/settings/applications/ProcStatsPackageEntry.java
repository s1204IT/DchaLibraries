package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.ArrayList;

public class ProcStatsPackageEntry implements Parcelable {
    long mAvgBgMem;
    long mAvgRunMem;
    long mBgDuration;
    double mBgWeight;
    final ArrayList<ProcStatsEntry> mEntries = new ArrayList<>();
    long mMaxBgMem;
    long mMaxRunMem;
    final String mPackage;
    long mRunDuration;
    double mRunWeight;
    public String mUiLabel;
    public ApplicationInfo mUiTargetApp;
    private long mWindowLength;
    private static boolean DEBUG = false;
    public static final Parcelable.Creator<ProcStatsPackageEntry> CREATOR = new Parcelable.Creator<ProcStatsPackageEntry>() {
        @Override
        public ProcStatsPackageEntry createFromParcel(Parcel in) {
            return new ProcStatsPackageEntry(in);
        }

        @Override
        public ProcStatsPackageEntry[] newArray(int size) {
            return new ProcStatsPackageEntry[size];
        }
    };

    public ProcStatsPackageEntry(String pkg, long windowLength) {
        this.mPackage = pkg;
        this.mWindowLength = windowLength;
    }

    public ProcStatsPackageEntry(Parcel in) {
        this.mPackage = in.readString();
        in.readTypedList(this.mEntries, ProcStatsEntry.CREATOR);
        this.mBgDuration = in.readLong();
        this.mAvgBgMem = in.readLong();
        this.mMaxBgMem = in.readLong();
        this.mBgWeight = in.readDouble();
        this.mRunDuration = in.readLong();
        this.mAvgRunMem = in.readLong();
        this.mMaxRunMem = in.readLong();
        this.mRunWeight = in.readDouble();
    }

    public void addEntry(ProcStatsEntry entry) {
        this.mEntries.add(entry);
    }

    public void updateMetrics() {
        this.mMaxBgMem = 0L;
        this.mAvgBgMem = 0L;
        this.mBgDuration = 0L;
        this.mBgWeight = 0.0d;
        this.mMaxRunMem = 0L;
        this.mAvgRunMem = 0L;
        this.mRunDuration = 0L;
        this.mRunWeight = 0.0d;
        int N = this.mEntries.size();
        for (int i = 0; i < N; i++) {
            ProcStatsEntry entry = this.mEntries.get(i);
            this.mBgDuration = Math.max(entry.mBgDuration, this.mBgDuration);
            this.mAvgBgMem += entry.mAvgBgMem;
            this.mBgWeight += entry.mBgWeight;
            this.mRunDuration = Math.max(entry.mRunDuration, this.mRunDuration);
            this.mAvgRunMem += entry.mAvgRunMem;
            this.mRunWeight += entry.mRunWeight;
            this.mMaxBgMem += entry.mMaxBgMem;
            this.mMaxRunMem += entry.mMaxRunMem;
        }
        this.mAvgBgMem /= (long) N;
        this.mAvgRunMem /= (long) N;
    }

    public void retrieveUiData(Context context, PackageManager pm) {
        this.mUiTargetApp = null;
        this.mUiLabel = this.mPackage;
        try {
            if ("os".equals(this.mPackage)) {
                this.mUiTargetApp = pm.getApplicationInfo("android", 41472);
                this.mUiLabel = context.getString(R.string.process_stats_os_label);
            } else {
                this.mUiTargetApp = pm.getApplicationInfo(this.mPackage, 41472);
                this.mUiLabel = this.mUiTargetApp.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPackage);
        dest.writeTypedList(this.mEntries);
        dest.writeLong(this.mBgDuration);
        dest.writeLong(this.mAvgBgMem);
        dest.writeLong(this.mMaxBgMem);
        dest.writeDouble(this.mBgWeight);
        dest.writeLong(this.mRunDuration);
        dest.writeLong(this.mAvgRunMem);
        dest.writeLong(this.mMaxRunMem);
        dest.writeDouble(this.mRunWeight);
    }

    public static CharSequence getFrequency(float amount, Context context) {
        if (amount > 0.95f) {
            return context.getString(R.string.always_running, Utils.formatPercentage((int) (100.0f * amount)));
        }
        if (amount > 0.25f) {
            return context.getString(R.string.sometimes_running, Utils.formatPercentage((int) (100.0f * amount)));
        }
        return context.getString(R.string.rarely_running, Utils.formatPercentage((int) (100.0f * amount)));
    }
}
