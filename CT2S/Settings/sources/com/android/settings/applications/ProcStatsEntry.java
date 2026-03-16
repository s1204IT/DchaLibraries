package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.app.ProcessStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class ProcStatsEntry implements Parcelable {
    final long mAvgPss;
    final long mAvgUss;
    String mBestTargetPackage;
    final long mDuration;
    final long mMaxPss;
    final long mMaxUss;
    final String mName;
    final String mPackage;
    final ArrayList<String> mPackages = new ArrayList<>();
    ArrayMap<String, ArrayList<Service>> mServices = new ArrayMap<>(1);
    public String mUiBaseLabel;
    public String mUiLabel;
    public String mUiPackage;
    public ApplicationInfo mUiTargetApp;
    final int mUid;
    final long mWeight;
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

    public ProcStatsEntry(ProcessStats.ProcessState proc, String packageName, ProcessStats.ProcessDataCollection tmpTotals, boolean useUss, boolean weightWithTime) {
        ProcessStats.computeProcessData(proc, tmpTotals, 0L);
        this.mPackage = proc.mPackage;
        this.mUid = proc.mUid;
        this.mName = proc.mName;
        this.mPackages.add(packageName);
        this.mDuration = tmpTotals.totalTime;
        this.mAvgPss = tmpTotals.avgPss;
        this.mMaxPss = tmpTotals.maxPss;
        this.mAvgUss = tmpTotals.avgUss;
        this.mMaxUss = tmpTotals.maxUss;
        this.mWeight = (useUss ? this.mAvgUss : this.mAvgPss) * (weightWithTime ? this.mDuration : 1L);
        if (DEBUG) {
            Log.d("ProcStatsEntry", "New proc entry " + proc.mName + ": dur=" + this.mDuration + " avgpss=" + this.mAvgPss + " weight=" + this.mWeight);
        }
    }

    public ProcStatsEntry(Parcel in) {
        this.mPackage = in.readString();
        this.mUid = in.readInt();
        this.mName = in.readString();
        in.readStringList(this.mPackages);
        this.mDuration = in.readLong();
        this.mAvgPss = in.readLong();
        this.mMaxPss = in.readLong();
        this.mAvgUss = in.readLong();
        this.mMaxUss = in.readLong();
        this.mWeight = in.readLong();
        this.mBestTargetPackage = in.readString();
        int N = in.readInt();
        if (N > 0) {
            this.mServices.ensureCapacity(N);
            for (int i = 0; i < N; i++) {
                String key = in.readString();
                ArrayList<Service> value = new ArrayList<>();
                in.readTypedList(value, Service.CREATOR);
                this.mServices.append(key, value);
            }
        }
    }

    public void addPackage(String packageName) {
        this.mPackages.add(packageName);
    }

    public void evaluateTargetPackage(PackageManager pm, ProcessStats stats, ProcessStats.ProcessDataCollection totals, Comparator<ProcStatsEntry> compare, boolean useUss, boolean weightWithTime) {
        this.mBestTargetPackage = null;
        if (this.mPackages.size() == 1) {
            if (DEBUG) {
                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": single pkg " + this.mPackages.get(0));
            }
            this.mBestTargetPackage = this.mPackages.get(0);
            return;
        }
        ArrayList<ProcStatsEntry> subProcs = new ArrayList<>();
        for (int ipkg = 0; ipkg < this.mPackages.size(); ipkg++) {
            SparseArray<ProcessStats.PackageState> vpkgs = (SparseArray) stats.mPackages.get(this.mPackages.get(ipkg), this.mUid);
            for (int ivers = 0; ivers < vpkgs.size(); ivers++) {
                ProcessStats.PackageState pkgState = vpkgs.valueAt(ivers);
                if (DEBUG) {
                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ", pkg " + pkgState + ":");
                }
                if (pkgState == null) {
                    Log.w("ProcStatsEntry", "No package state found for " + this.mPackages.get(ipkg) + "/" + this.mUid + " in process " + this.mName);
                } else {
                    ProcessStats.ProcessState pkgProc = (ProcessStats.ProcessState) pkgState.mProcesses.get(this.mName);
                    if (pkgProc == null) {
                        Log.w("ProcStatsEntry", "No process " + this.mName + " found in package state " + this.mPackages.get(ipkg) + "/" + this.mUid);
                    } else {
                        subProcs.add(new ProcStatsEntry(pkgProc, pkgState.mPackageName, totals, useUss, weightWithTime));
                    }
                }
            }
        }
        if (subProcs.size() > 1) {
            Collections.sort(subProcs, compare);
            if (subProcs.get(0).mWeight > subProcs.get(1).mWeight * 3) {
                if (DEBUG) {
                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": best pkg " + subProcs.get(0).mPackage + " weight " + subProcs.get(0).mWeight + " better than " + subProcs.get(1).mPackage + " weight " + subProcs.get(1).mWeight);
                }
                this.mBestTargetPackage = subProcs.get(0).mPackage;
                return;
            }
            long maxWeight = subProcs.get(0).mWeight;
            long bestRunTime = -1;
            for (int i = 0; i < subProcs.size(); i++) {
                if (subProcs.get(i).mWeight < maxWeight / 2) {
                    if (DEBUG) {
                        Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProcs.get(i).mPackage + " weight " + subProcs.get(i).mWeight + " too small");
                    }
                } else {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(subProcs.get(i).mPackage, 0);
                        if (ai.icon == 0) {
                            if (DEBUG) {
                                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProcs.get(i).mPackage + " has no icon");
                            }
                        } else {
                            ArrayList<Service> subProcServices = null;
                            int isp = 0;
                            int NSP = this.mServices.size();
                            while (true) {
                                if (isp >= NSP) {
                                    break;
                                }
                                ArrayList<Service> subServices = this.mServices.valueAt(isp);
                                if (!subServices.get(0).mPackage.equals(subProcs.get(i).mPackage)) {
                                    isp++;
                                } else {
                                    subProcServices = subServices;
                                    break;
                                }
                            }
                            long thisRunTime = 0;
                            if (subProcServices != null) {
                                int iss = 0;
                                int NSS = subProcServices.size();
                                while (true) {
                                    if (iss >= NSS) {
                                        break;
                                    }
                                    Service service = subProcServices.get(iss);
                                    if (service.mDuration <= 0) {
                                        iss++;
                                    } else {
                                        if (DEBUG) {
                                            Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProcs.get(i).mPackage + " service " + service.mName + " run time is " + service.mDuration);
                                        }
                                        thisRunTime = service.mDuration;
                                    }
                                }
                            }
                            if (thisRunTime > bestRunTime) {
                                if (DEBUG) {
                                    Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProcs.get(i).mPackage + " new best run time " + thisRunTime);
                                }
                                this.mBestTargetPackage = subProcs.get(i).mPackage;
                                bestRunTime = thisRunTime;
                            } else if (DEBUG) {
                                Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProcs.get(i).mPackage + " run time " + thisRunTime + " not as good as last " + bestRunTime);
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        if (DEBUG) {
                            Log.d("ProcStatsEntry", "Eval pkg of " + this.mName + ": pkg " + subProcs.get(i).mPackage + " failed finding app info");
                        }
                    }
                }
            }
            return;
        }
        if (subProcs.size() == 1) {
            this.mBestTargetPackage = subProcs.get(0).mPackage;
        }
    }

    public void retrieveUiData(PackageManager pm) {
        this.mUiTargetApp = null;
        String str = this.mName;
        this.mUiBaseLabel = str;
        this.mUiLabel = str;
        this.mUiPackage = this.mBestTargetPackage;
        if (this.mUiPackage != null) {
            try {
                this.mUiTargetApp = pm.getApplicationInfo(this.mUiPackage, 41472);
                String name = this.mUiTargetApp.loadLabel(pm).toString();
                this.mUiBaseLabel = name;
                if (this.mName.equals(this.mUiPackage)) {
                    this.mUiLabel = name;
                } else if (this.mName.startsWith(this.mUiPackage)) {
                    int off = this.mUiPackage.length();
                    if (this.mName.length() > off) {
                        off++;
                    }
                    this.mUiLabel = name + " (" + this.mName.substring(off) + ")";
                } else {
                    this.mUiLabel = name + " (" + this.mName + ")";
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (this.mUiTargetApp == null) {
            String[] packages = pm.getPackagesForUid(this.mUid);
            if (packages != null) {
                for (String curPkg : packages) {
                    try {
                        PackageInfo pi = pm.getPackageInfo(curPkg, 41472);
                        if (pi.sharedUserLabel != 0) {
                            this.mUiTargetApp = pi.applicationInfo;
                            CharSequence nm = pm.getText(curPkg, pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                this.mUiBaseLabel = nm.toString();
                                this.mUiLabel = this.mUiBaseLabel + " (" + this.mName + ")";
                            } else {
                                this.mUiBaseLabel = this.mUiTargetApp.loadLabel(pm).toString();
                                this.mUiLabel = this.mUiBaseLabel + " (" + this.mName + ")";
                            }
                            return;
                        }
                        continue;
                    } catch (PackageManager.NameNotFoundException e2) {
                    }
                }
                return;
            }
            Log.i("ProcStatsEntry", "No package for uid " + this.mUid);
        }
    }

    public void addService(ProcessStats.ServiceState svc) {
        ArrayList<Service> services = this.mServices.get(svc.mPackage);
        if (services == null) {
            services = new ArrayList<>();
            this.mServices.put(svc.mPackage, services);
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
        dest.writeLong(this.mDuration);
        dest.writeLong(this.mAvgPss);
        dest.writeLong(this.mMaxPss);
        dest.writeLong(this.mAvgUss);
        dest.writeLong(this.mMaxUss);
        dest.writeLong(this.mWeight);
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

        public Service(ProcessStats.ServiceState service) {
            this.mPackage = service.mPackage;
            this.mName = service.mName;
            this.mProcess = service.mProcessName;
            this.mDuration = ProcessStats.dumpSingleServiceTime((PrintWriter) null, (String) null, service, 0, -1, 0L, 0L);
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
