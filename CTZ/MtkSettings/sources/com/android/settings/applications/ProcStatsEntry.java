package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseArray;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ServiceState;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class ProcStatsEntry implements Parcelable {
    final long mAvgBgMem;
    final long mAvgRunMem;
    String mBestTargetPackage;
    final long mBgDuration;
    final double mBgWeight;
    public CharSequence mLabel;
    final long mMaxBgMem;
    final long mMaxRunMem;
    final String mName;
    final String mPackage;
    final long mRunDuration;
    final double mRunWeight;
    final int mUid;
    private static boolean DEBUG = false;
    public static final Parcelable.Creator<ProcStatsEntry> CREATOR = new Parcelable.Creator<ProcStatsEntry>() {
        @Override
        public ProcStatsEntry createFromParcel(Parcel parcel) {
            return new ProcStatsEntry(parcel);
        }

        @Override
        public ProcStatsEntry[] newArray(int i) {
            return new ProcStatsEntry[i];
        }
    };
    final ArrayList<String> mPackages = new ArrayList<>();
    ArrayMap<String, ArrayList<Service>> mServices = new ArrayMap<>(1);

    public ProcStatsEntry(ProcessState processState, String str, ProcessStats.ProcessDataCollection processDataCollection, ProcessStats.ProcessDataCollection processDataCollection2, boolean z) {
        processState.computeProcessData(processDataCollection, 0L);
        processState.computeProcessData(processDataCollection2, 0L);
        this.mPackage = processState.getPackage();
        this.mUid = processState.getUid();
        this.mName = processState.getName();
        this.mPackages.add(str);
        this.mBgDuration = processDataCollection.totalTime;
        this.mAvgBgMem = z ? processDataCollection.avgUss : processDataCollection.avgPss;
        this.mMaxBgMem = z ? processDataCollection.maxUss : processDataCollection.maxPss;
        this.mBgWeight = this.mAvgBgMem * this.mBgDuration;
        this.mRunDuration = processDataCollection2.totalTime;
        this.mAvgRunMem = z ? processDataCollection2.avgUss : processDataCollection2.avgPss;
        this.mMaxRunMem = z ? processDataCollection2.maxUss : processDataCollection2.maxPss;
        this.mRunWeight = this.mAvgRunMem * this.mRunDuration;
        if (DEBUG) {
            Log.d("ProcStatsEntry", "New proc entry " + processState.getName() + ": dur=" + this.mBgDuration + " avgpss=" + this.mAvgBgMem + " weight=" + this.mBgWeight);
        }
    }

    public ProcStatsEntry(String str, int i, String str2, long j, long j2, long j3) {
        this.mPackage = str;
        this.mUid = i;
        this.mName = str2;
        this.mRunDuration = j;
        this.mBgDuration = j;
        this.mMaxRunMem = j2;
        this.mAvgRunMem = j2;
        this.mMaxBgMem = j2;
        this.mAvgBgMem = j2;
        double d = j3 * j2;
        this.mRunWeight = d;
        this.mBgWeight = d;
        if (DEBUG) {
            Log.d("ProcStatsEntry", "New proc entry " + str2 + ": dur=" + this.mBgDuration + " avgpss=" + this.mAvgBgMem + " weight=" + this.mBgWeight);
        }
    }

    public ProcStatsEntry(Parcel parcel) {
        this.mPackage = parcel.readString();
        this.mUid = parcel.readInt();
        this.mName = parcel.readString();
        parcel.readStringList(this.mPackages);
        this.mBgDuration = parcel.readLong();
        this.mAvgBgMem = parcel.readLong();
        this.mMaxBgMem = parcel.readLong();
        this.mBgWeight = parcel.readDouble();
        this.mRunDuration = parcel.readLong();
        this.mAvgRunMem = parcel.readLong();
        this.mMaxRunMem = parcel.readLong();
        this.mRunWeight = parcel.readDouble();
        this.mBestTargetPackage = parcel.readString();
        int i = parcel.readInt();
        if (i > 0) {
            this.mServices.ensureCapacity(i);
            for (int i2 = 0; i2 < i; i2++) {
                String string = parcel.readString();
                ArrayList arrayList = new ArrayList();
                parcel.readTypedList(arrayList, Service.CREATOR);
                this.mServices.append(string, arrayList);
            }
        }
    }

    public void addPackage(String str) {
        this.mPackages.add(str);
    }

    public void evaluateTargetPackage(PackageManager packageManager, ProcessStats processStats, ProcessStats.ProcessDataCollection processDataCollection, ProcessStats.ProcessDataCollection processDataCollection2, Comparator<ProcStatsEntry> comparator, boolean z) {
        double d;
        ApplicationInfo applicationInfo;
        ArrayList<Service> arrayListValueAt;
        this.mBestTargetPackage = null;
        int i = 0;
        if (this.mPackages.size() == 1) {
            if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": single pkg " + this.mPackages.get(0));
            }
            this.mBestTargetPackage = this.mPackages.get(0);
            return;
        }
        for (int i2 = 0; i2 < this.mPackages.size(); i2++) {
            if ("android".equals(this.mPackages.get(i2))) {
                this.mBestTargetPackage = this.mPackages.get(i2);
                return;
            }
        }
        ArrayList arrayList = new ArrayList();
        for (int i3 = 0; i3 < this.mPackages.size(); i3++) {
            LongSparseArray longSparseArray = (LongSparseArray) processStats.mPackages.get(this.mPackages.get(i3), this.mUid);
            for (int i4 = 0; i4 < longSparseArray.size(); i4++) {
                ProcessStats.PackageState packageState = (ProcessStats.PackageState) longSparseArray.valueAt(i4);
                if (DEBUG) {
                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ", pkg " + packageState + ":");
                }
                if (packageState == null) {
                    Log.w("ProcStatsEntry", "No package state found for " + this.mPackages.get(i3) + "/" + this.mUid + " in process " + this.mName);
                } else {
                    ProcessState processState = (ProcessState) packageState.mProcesses.get(this.mName);
                    if (processState == null) {
                        Log.w("ProcStatsEntry", "No process " + this.mName + " found in package state " + this.mPackages.get(i3) + "/" + this.mUid);
                    } else {
                        arrayList.add(new ProcStatsEntry(processState, packageState.mPackageName, processDataCollection, processDataCollection2, z));
                    }
                }
            }
        }
        if (arrayList.size() <= 1) {
            if (arrayList.size() == 1) {
                this.mBestTargetPackage = ((ProcStatsEntry) arrayList.get(0)).mPackage;
                return;
            }
            return;
        }
        Collections.sort(arrayList, comparator);
        if (((ProcStatsEntry) arrayList.get(0)).mRunWeight > ((ProcStatsEntry) arrayList.get(1)).mRunWeight * 3.0d) {
            if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": best pkg " + ((ProcStatsEntry) arrayList.get(0)).mPackage + " weight " + ((ProcStatsEntry) arrayList.get(0)).mRunWeight + " better than " + ((ProcStatsEntry) arrayList.get(1)).mPackage + " weight " + ((ProcStatsEntry) arrayList.get(1)).mRunWeight);
            }
            this.mBestTargetPackage = ((ProcStatsEntry) arrayList.get(0)).mPackage;
            return;
        }
        double d2 = ((ProcStatsEntry) arrayList.get(0)).mRunWeight;
        long j = -1;
        int i5 = 0;
        boolean z2 = false;
        while (i5 < arrayList.size()) {
            ProcStatsEntry procStatsEntry = (ProcStatsEntry) arrayList.get(i5);
            if (procStatsEntry.mRunWeight >= d2 / 2.0d) {
                try {
                    applicationInfo = packageManager.getApplicationInfo(procStatsEntry.mPackage, i);
                } catch (PackageManager.NameNotFoundException e) {
                    d = d2;
                    if (DEBUG) {
                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " failed finding app info");
                    }
                }
                if (applicationInfo.icon == 0) {
                    if (DEBUG) {
                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " has no icon");
                    }
                } else if ((applicationInfo.flags & 8) != 0) {
                    long j2 = procStatsEntry.mRunDuration;
                    if (!z2 || j2 > j) {
                        if (DEBUG) {
                            Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " new best pers run time " + j2);
                        }
                        j = j2;
                        z2 = true;
                    } else if (DEBUG) {
                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " pers run time " + j2 + " not as good as last " + j);
                    }
                } else if (!z2) {
                    int size = this.mServices.size();
                    int i6 = i;
                    while (true) {
                        if (i6 >= size) {
                            arrayListValueAt = null;
                            break;
                        }
                        arrayListValueAt = this.mServices.valueAt(i6);
                        if (arrayListValueAt.get(i).mPackage.equals(procStatsEntry.mPackage)) {
                            break;
                        } else {
                            i6++;
                        }
                    }
                    long j3 = 0;
                    if (arrayListValueAt != null) {
                        int size2 = arrayListValueAt.size();
                        int i7 = i;
                        while (i7 < size2) {
                            Service service = arrayListValueAt.get(i7);
                            d = d2;
                            if (service.mDuration > 0) {
                                if (DEBUG) {
                                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " service " + service.mName + " run time is " + service.mDuration);
                                }
                                j3 = service.mDuration;
                                if (j3 > j) {
                                    if (DEBUG) {
                                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " new best run time " + j3);
                                    }
                                    this.mBestTargetPackage = procStatsEntry.mPackage;
                                    j = j3;
                                } else if (DEBUG) {
                                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " run time " + j3 + " not as good as last " + j);
                                }
                            } else {
                                i7++;
                                d2 = d;
                            }
                        }
                        d = d2;
                        if (j3 > j) {
                        }
                    } else {
                        d = d2;
                        if (j3 > j) {
                        }
                    }
                    i5++;
                    d2 = d;
                    i = 0;
                } else if (DEBUG) {
                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " is not persistent");
                }
            } else if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + procStatsEntry.mPackage + " weight " + procStatsEntry.mRunWeight + " too small");
            }
            d = d2;
            i5++;
            d2 = d;
            i = 0;
        }
        if (TextUtils.isEmpty(this.mBestTargetPackage)) {
            this.mBestTargetPackage = ((ProcStatsEntry) arrayList.get(0)).mPackage;
        }
    }

    public void addService(ServiceState serviceState) {
        ArrayList<Service> arrayList = this.mServices.get(serviceState.getPackage());
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mServices.put(serviceState.getPackage(), arrayList);
        }
        arrayList.add(new Service(serviceState));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackage);
        parcel.writeInt(this.mUid);
        parcel.writeString(this.mName);
        parcel.writeStringList(this.mPackages);
        parcel.writeLong(this.mBgDuration);
        parcel.writeLong(this.mAvgBgMem);
        parcel.writeLong(this.mMaxBgMem);
        parcel.writeDouble(this.mBgWeight);
        parcel.writeLong(this.mRunDuration);
        parcel.writeLong(this.mAvgRunMem);
        parcel.writeLong(this.mMaxRunMem);
        parcel.writeDouble(this.mRunWeight);
        parcel.writeString(this.mBestTargetPackage);
        int size = this.mServices.size();
        parcel.writeInt(size);
        for (int i2 = 0; i2 < size; i2++) {
            parcel.writeString(this.mServices.keyAt(i2));
            parcel.writeTypedList(this.mServices.valueAt(i2));
        }
    }

    public int getUid() {
        return this.mUid;
    }

    public static final class Service implements Parcelable {
        public static final Parcelable.Creator<Service> CREATOR = new Parcelable.Creator<Service>() {
            @Override
            public Service createFromParcel(Parcel parcel) {
                return new Service(parcel);
            }

            @Override
            public Service[] newArray(int i) {
                return new Service[i];
            }
        };
        final long mDuration;
        final String mName;
        final String mPackage;
        final String mProcess;

        public Service(ServiceState serviceState) {
            this.mPackage = serviceState.getPackage();
            this.mName = serviceState.getName();
            this.mProcess = serviceState.getProcessName();
            this.mDuration = serviceState.dumpTime((PrintWriter) null, (String) null, 0, -1, 0L, 0L);
        }

        public Service(Parcel parcel) {
            this.mPackage = parcel.readString();
            this.mName = parcel.readString();
            this.mProcess = parcel.readString();
            this.mDuration = parcel.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(this.mPackage);
            parcel.writeString(this.mName);
            parcel.writeString(this.mProcess);
            parcel.writeLong(this.mDuration);
        }
    }
}
