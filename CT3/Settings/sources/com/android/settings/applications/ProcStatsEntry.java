package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
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
        public ProcStatsEntry createFromParcel(Parcel in) {
            return new ProcStatsEntry(in);
        }

        @Override
        public ProcStatsEntry[] newArray(int size) {
            return new ProcStatsEntry[size];
        }
    };
    final ArrayList<String> mPackages = new ArrayList<>();
    ArrayMap<String, ArrayList<Service>> mServices = new ArrayMap<>(1);

    public ProcStatsEntry(ProcessState proc, String packageName, ProcessStats.ProcessDataCollection tmpBgTotals, ProcessStats.ProcessDataCollection tmpRunTotals, boolean useUss) {
        proc.computeProcessData(tmpBgTotals, 0L);
        proc.computeProcessData(tmpRunTotals, 0L);
        this.mPackage = proc.getPackage();
        this.mUid = proc.getUid();
        this.mName = proc.getName();
        this.mPackages.add(packageName);
        this.mBgDuration = tmpBgTotals.totalTime;
        this.mAvgBgMem = useUss ? tmpBgTotals.avgUss : tmpBgTotals.avgPss;
        this.mMaxBgMem = useUss ? tmpBgTotals.maxUss : tmpBgTotals.maxPss;
        this.mBgWeight = this.mAvgBgMem * this.mBgDuration;
        this.mRunDuration = tmpRunTotals.totalTime;
        this.mAvgRunMem = useUss ? tmpRunTotals.avgUss : tmpRunTotals.avgPss;
        this.mMaxRunMem = useUss ? tmpRunTotals.maxUss : tmpRunTotals.maxPss;
        this.mRunWeight = this.mAvgRunMem * this.mRunDuration;
        if (DEBUG) {
            Log.d("ProcStatsEntry", "New proc entry " + proc.getName() + ": dur=" + this.mBgDuration + " avgpss=" + this.mAvgBgMem + " weight=" + this.mBgWeight);
        }
    }

    public ProcStatsEntry(String pkgName, int uid, String procName, long duration, long mem, long memDuration) {
        this.mPackage = pkgName;
        this.mUid = uid;
        this.mName = procName;
        this.mRunDuration = duration;
        this.mBgDuration = duration;
        this.mMaxRunMem = mem;
        this.mAvgRunMem = mem;
        this.mMaxBgMem = mem;
        this.mAvgBgMem = mem;
        double d = memDuration * mem;
        this.mRunWeight = d;
        this.mBgWeight = d;
        if (DEBUG) {
            Log.d("ProcStatsEntry", "New proc entry " + procName + ": dur=" + this.mBgDuration + " avgpss=" + this.mAvgBgMem + " weight=" + this.mBgWeight);
        }
    }

    public ProcStatsEntry(Parcel in) {
        this.mPackage = in.readString();
        this.mUid = in.readInt();
        this.mName = in.readString();
        in.readStringList(this.mPackages);
        this.mBgDuration = in.readLong();
        this.mAvgBgMem = in.readLong();
        this.mMaxBgMem = in.readLong();
        this.mBgWeight = in.readDouble();
        this.mRunDuration = in.readLong();
        this.mAvgRunMem = in.readLong();
        this.mMaxRunMem = in.readLong();
        this.mRunWeight = in.readDouble();
        this.mBestTargetPackage = in.readString();
        int N = in.readInt();
        if (N <= 0) {
            return;
        }
        this.mServices.ensureCapacity(N);
        for (int i = 0; i < N; i++) {
            String key = in.readString();
            ArrayList<Service> value = new ArrayList<>();
            in.readTypedList(value, Service.CREATOR);
            this.mServices.append(key, value);
        }
    }

    public void addPackage(String packageName) {
        this.mPackages.add(packageName);
    }

    public void evaluateTargetPackage(PackageManager pm, ProcessStats stats, ProcessStats.ProcessDataCollection bgTotals, ProcessStats.ProcessDataCollection runTotals, Comparator<ProcStatsEntry> compare, boolean useUss) {
        this.mBestTargetPackage = null;
        if (this.mPackages.size() == 1) {
            if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": single pkg " + this.mPackages.get(0));
            }
            this.mBestTargetPackage = this.mPackages.get(0);
            return;
        }
        for (int ipkg = 0; ipkg < this.mPackages.size(); ipkg++) {
            if ("android".equals(this.mPackages.get(ipkg))) {
                this.mBestTargetPackage = this.mPackages.get(ipkg);
                return;
            }
        }
        ArrayList<ProcStatsEntry> subProcs = new ArrayList<>();
        for (int ipkg2 = 0; ipkg2 < this.mPackages.size(); ipkg2++) {
            SparseArray<ProcessStats.PackageState> vpkgs = (SparseArray) stats.mPackages.get(this.mPackages.get(ipkg2), this.mUid);
            for (int ivers = 0; ivers < vpkgs.size(); ivers++) {
                ProcessStats.PackageState pkgState = vpkgs.valueAt(ivers);
                if (DEBUG) {
                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ", pkg " + pkgState + ":");
                }
                if (pkgState == null) {
                    Log.w("ProcStatsEntry", "No package state found for " + this.mPackages.get(ipkg2) + "/" + this.mUid + " in process " + this.mName);
                } else {
                    ProcessState pkgProc = (ProcessState) pkgState.mProcesses.get(this.mName);
                    if (pkgProc == null) {
                        Log.w("ProcStatsEntry", "No process " + this.mName + " found in package state " + this.mPackages.get(ipkg2) + "/" + this.mUid);
                    } else {
                        subProcs.add(new ProcStatsEntry(pkgProc, pkgState.mPackageName, bgTotals, runTotals, useUss));
                    }
                }
            }
        }
        if (subProcs.size() <= 1) {
            if (subProcs.size() == 1) {
                this.mBestTargetPackage = subProcs.get(0).mPackage;
                return;
            }
            return;
        }
        Collections.sort(subProcs, compare);
        if (subProcs.get(0).mRunWeight > subProcs.get(1).mRunWeight * 3.0d) {
            if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": best pkg " + subProcs.get(0).mPackage + " weight " + subProcs.get(0).mRunWeight + " better than " + subProcs.get(1).mPackage + " weight " + subProcs.get(1).mRunWeight);
            }
            this.mBestTargetPackage = subProcs.get(0).mPackage;
            return;
        }
        double maxWeight = subProcs.get(0).mRunWeight;
        long bestRunTime = -1;
        boolean bestPersistent = false;
        for (int i = 0; i < subProcs.size(); i++) {
            ProcStatsEntry subProc = subProcs.get(i);
            if (subProc.mRunWeight >= maxWeight / 2.0d) {
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(subProc.mPackage, 0);
                    if (ai.icon == 0) {
                        if (DEBUG) {
                            Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " has no icon");
                        }
                    } else if ((ai.flags & 8) != 0) {
                        long thisRunTime = subProc.mRunDuration;
                        if (!bestPersistent || thisRunTime > bestRunTime) {
                            if (DEBUG) {
                                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " new best pers run time " + thisRunTime);
                            }
                            bestRunTime = thisRunTime;
                            bestPersistent = true;
                        } else if (DEBUG) {
                            Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " pers run time " + thisRunTime + " not as good as last " + bestRunTime);
                        }
                    } else if (!bestPersistent) {
                        ArrayList<Service> subProcServices = null;
                        int isp = 0;
                        int NSP = this.mServices.size();
                        while (true) {
                            if (isp >= NSP) {
                                break;
                            }
                            ArrayList<Service> subServices = this.mServices.valueAt(isp);
                            if (subServices.get(0).mPackage.equals(subProc.mPackage)) {
                                subProcServices = subServices;
                                break;
                            }
                            isp++;
                        }
                        long thisRunTime2 = 0;
                        if (subProcServices != null) {
                            int iss = 0;
                            int NSS = subProcServices.size();
                            while (true) {
                                if (iss >= NSS) {
                                    break;
                                }
                                Service service = subProcServices.get(iss);
                                if (service.mDuration > 0) {
                                    if (DEBUG) {
                                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " service " + service.mName + " run time is " + service.mDuration);
                                    }
                                    thisRunTime2 = service.mDuration;
                                } else {
                                    iss++;
                                }
                            }
                        }
                        if (thisRunTime2 > bestRunTime) {
                            if (DEBUG) {
                                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " new best run time " + thisRunTime2);
                            }
                            this.mBestTargetPackage = subProc.mPackage;
                            bestRunTime = thisRunTime2;
                        } else if (DEBUG) {
                            Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " run time " + thisRunTime2 + " not as good as last " + bestRunTime);
                        }
                    } else if (DEBUG) {
                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " is not persistent");
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    if (DEBUG) {
                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " failed finding app info");
                    }
                }
            } else if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProc.mPackage + " weight " + subProc.mRunWeight + " too small");
            }
        }
    }

    public void addService(ServiceState svc) {
        ArrayList<Service> services = this.mServices.get(svc.getPackage());
        if (services == null) {
            services = new ArrayList<>();
            this.mServices.put(svc.getPackage(), services);
        }
        services.add(new Service(svc));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPackage);
        dest.writeInt(this.mUid);
        dest.writeString(this.mName);
        dest.writeStringList(this.mPackages);
        dest.writeLong(this.mBgDuration);
        dest.writeLong(this.mAvgBgMem);
        dest.writeLong(this.mMaxBgMem);
        dest.writeDouble(this.mBgWeight);
        dest.writeLong(this.mRunDuration);
        dest.writeLong(this.mAvgRunMem);
        dest.writeLong(this.mMaxRunMem);
        dest.writeDouble(this.mRunWeight);
        dest.writeString(this.mBestTargetPackage);
        int N = this.mServices.size();
        dest.writeInt(N);
        for (int i = 0; i < N; i++) {
            dest.writeString(this.mServices.keyAt(i));
            dest.writeTypedList(this.mServices.valueAt(i));
        }
    }

    public static final class Service implements Parcelable {
        public static final Parcelable.Creator<Service> CREATOR = new Parcelable.Creator<Service>() {
            @Override
            public Service createFromParcel(Parcel in) {
                return new Service(in);
            }

            @Override
            public Service[] newArray(int size) {
                return new Service[size];
            }
        };
        final long mDuration;
        final String mName;
        final String mPackage;
        final String mProcess;

        public Service(ServiceState service) {
            this.mPackage = service.getPackage();
            this.mName = service.getName();
            this.mProcess = service.getProcessName();
            this.mDuration = service.dumpTime((PrintWriter) null, (String) null, 0, -1, 0L, 0L);
        }

        public Service(Parcel in) {
            this.mPackage = in.readString();
            this.mName = in.readString();
            this.mProcess = in.readString();
            this.mDuration = in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.mPackage);
            dest.writeString(this.mName);
            dest.writeString(this.mProcess);
            dest.writeLong(this.mDuration);
        }
    }
}
