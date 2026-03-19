package com.mediatek.apm.suppression;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.pm.PackageManagerService;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import java.util.ArrayList;
import java.util.List;

public class SuppressionAction {
    private List<a> k;
    private List<b> l;
    private boolean n;
    private static Context mContext = null;
    private static SuppressionAction i = null;
    private static SuppressionPolicy j = null;
    private List<String> m = null;
    private boolean o = "user".equals(Build.TYPE);

    private SuppressionAction(Context context) {
        this.k = null;
        this.l = null;
        this.n = false;
        mContext = context;
        new com.mediatek.common.jpe.a().a();
        if (j == null) {
            j = SuppressionPolicy.getInstance();
        }
        this.k = new ArrayList();
        this.l = new ArrayList();
        this.n = SystemProperties.get("persist.sys.apm.debug_mode").equals("1");
    }

    public static SuppressionAction getInstance(Context context) {
        if (i == null) {
            i = new SuppressionAction(context);
        }
        return i;
    }

    public void onReadyToStartComponent(AMEventHookData.ReadyToStartComponent readyToStartComponent) {
        String str;
        String string = readyToStartComponent.getString(AMEventHookData.ReadyToStartComponent.Index.packageName);
        int i2 = readyToStartComponent.getInt(AMEventHookData.ReadyToStartComponent.Index.uid);
        List list = (List) readyToStartComponent.get(AMEventHookData.ReadyToStartComponent.Index.callerList);
        List list2 = (List) readyToStartComponent.get(AMEventHookData.ReadyToStartComponent.Index.callerUidList);
        List list3 = (List) readyToStartComponent.get(AMEventHookData.ReadyToStartComponent.Index.delayedCallerList);
        List list4 = (List) readyToStartComponent.get(AMEventHookData.ReadyToStartComponent.Index.delayedCallerUidList);
        List list5 = (List) readyToStartComponent.get(AMEventHookData.ReadyToStartComponent.Index.clientList);
        List list6 = (List) readyToStartComponent.get(AMEventHookData.ReadyToStartComponent.Index.clientUidList);
        String string2 = readyToStartComponent.getString(AMEventHookData.ReadyToStartComponent.Index.suppressReason);
        readyToStartComponent.getString(AMEventHookData.ReadyToStartComponent.Index.suppressAction);
        if (this.n) {
            Slog.d("SuppressionAction", "onReadyToStartComponent: begin! package = " + string);
        }
        ArrayList<String> suppressionList = j.getSuppressionList();
        if (string2 == null) {
            if (this.n) {
                Slog.d("SuppressionAction", "onReadyToStartComponent: suppressReason = null!");
                return;
            }
            return;
        }
        if (suppressionList != null) {
            int i3 = 0;
            while (true) {
                int i4 = i3;
                if (i4 >= suppressionList.size()) {
                    break;
                }
                ArrayList arrayList = new ArrayList();
                ArrayList arrayList2 = new ArrayList();
                String str2 = suppressionList.get(i4);
                int iD = j.d(str2);
                if (string2.equals("bind service") || string2.equals("delayed service") || string2.equals("restart service") || string2.equals("start service")) {
                    if ((iD & 32) != 0 || (iD & 64) != 0) {
                        arrayList.add(string);
                        arrayList2.add(new Integer(i2));
                        if (string2.equals("bind service") || string2.equals("start service")) {
                            if (list != null && list2 != null) {
                                arrayList.addAll(list);
                                arrayList2.addAll(list2);
                            }
                        } else if (string2.equals("restart service")) {
                            if (list5 != null && list6 != null) {
                                arrayList.addAll(list5);
                                arrayList2.addAll(list6);
                            }
                        } else if (string2.equals("delayed service") && list3 != null && list4 != null) {
                            arrayList.addAll(list3);
                            arrayList2.addAll(list4);
                        }
                        if (!a(str2, arrayList, arrayList2)) {
                            if ((iD & 32) == 0) {
                                str = (iD & 64) == 0 ? "allowed" : "skipped";
                            } else {
                                str = "delayed";
                            }
                            if (this.n) {
                                Slog.d("SuppressionAction", "onReadyToStartComponent:suppressClient = " + str2 + " suppressPolicy = " + iD + " suppressReason = " + string2 + " suppressAction = " + str);
                            }
                            readyToStartComponent.set(new Object[]{string, Integer.valueOf(i2), list, list2, list3, list4, list5, list6, string2, str});
                            return;
                        }
                    }
                } else if (string2.equals("broadcast")) {
                    if ((iD & PackageManagerService.DumpState.DUMP_KEYSETS) != 0) {
                        arrayList.add(string);
                        arrayList2.add(new Integer(i2));
                        if (!a(str2, arrayList, arrayList2)) {
                            if (this.n) {
                                Slog.d("SuppressionAction", "onReadyToStartComponent:suppressClient = " + str2 + " suppressPolicy = " + iD + " suppressReason = " + string2 + " suppressAction = skipped");
                            }
                            readyToStartComponent.set(new Object[]{string, Integer.valueOf(i2), null, null, null, null, null, null, "broadcast", "skipped"});
                            return;
                        }
                    } else {
                        continue;
                    }
                } else if (string2.equals("broadcast_p")) {
                    if ((32768 & iD) != 0) {
                        arrayList.add(string);
                        arrayList2.add(new Integer(i2));
                        if (!a(str2, arrayList, arrayList2)) {
                            if (this.n) {
                                Slog.d("SuppressionAction", "onReadyToStartComponent:suppressClient = " + str2 + " suppressPolicy = " + iD + " suppressReason = " + string2 + " suppressAction = skipped");
                            }
                            readyToStartComponent.set(new Object[]{string, Integer.valueOf(i2), null, null, null, null, null, null, "broadcast_p", "skipped"});
                            return;
                        }
                    } else {
                        continue;
                    }
                } else if (string2.equals("provider")) {
                    if ((iD & 512) != 0) {
                        arrayList.add(string);
                        arrayList2.add(new Integer(i2));
                        if (list != null && list2 != null) {
                            arrayList.addAll(list);
                            arrayList2.addAll(list2);
                        }
                        if (!a(str2, arrayList, arrayList2)) {
                            if (this.n) {
                                Slog.d("SuppressionAction", "onReadyToStartComponent:suppressClient = " + str2 + " suppressPolicy = " + iD + " suppressReason = " + string2 + " suppressAction = delayed");
                            }
                            readyToStartComponent.set(new Object[]{string, Integer.valueOf(i2), list, list2, null, null, null, null, "provider", "delayed"});
                            return;
                        }
                    } else {
                        continue;
                    }
                } else if (this.n) {
                    Slog.d("SuppressionAction", "onReadyToStartComponent: not support " + string2);
                }
                i3 = i4 + 1;
            }
        }
        if (this.n) {
            Slog.d("SuppressionAction", "onReadyToStartComponent: end! suppressAction = allowed");
        }
    }

    private boolean a(String str, List<String> list, List<Integer> list2) {
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (!j.isPackageInSuppression(str, list.get(i2), list2.get(i2).intValue())) {
                return true;
            }
        }
        return false;
    }

    public int getSuppressPackagePolicy(int i2) {
        int i3 = (1048576 & i2) != 0 ? 1 : 0;
        if ((2097152 & i2) != 0) {
            i3 |= 2;
        }
        if ((4194304 & i2) != 0) {
            i3 |= 4;
        }
        if ((8388608 & i2) != 0) {
            i3 |= 8388616;
        }
        if ((16777216 & i2) != 0) {
            i3 |= 16;
        }
        if ((33554432 & i2) != 0) {
            i3 |= 32;
        }
        if ((67108864 & i2) != 0) {
            i3 |= 64;
        }
        if ((134217728 & i2) != 0) {
            i3 |= 128;
        }
        if ((268435456 & i2) != 0) {
            i3 |= 256;
        }
        if ((536870912 & i2) != 0) {
            i3 |= 512;
        }
        if (this.n) {
            Slog.d("SuppressionAction", "getSuppressPackagePolicy: suppressPolicy = " + i2 + " suppressPackagePolicy = " + i3);
        }
        return i3;
    }

    public int getUnsuppressPackagePolicy(int i2) {
        int i3 = i2 == 0 ? 1 : 0;
        if (this.n) {
            Slog.d("SuppressionAction", "getUnsuppressPackagePolicy: unsuppressPolicy = " + i2 + " unsuppressPackagePolicy = " + i3);
        }
        return i3;
    }

    public void onPackageStoppedStatusChanged(AMEventHookData.PackageStoppedStatusChanged packageStoppedStatusChanged) {
        synchronized (this) {
            String string = packageStoppedStatusChanged.getString(AMEventHookData.PackageStoppedStatusChanged.Index.packageName);
            int i2 = packageStoppedStatusChanged.getInt(AMEventHookData.PackageStoppedStatusChanged.Index.suppressAction);
            String string2 = packageStoppedStatusChanged.getString(AMEventHookData.PackageStoppedStatusChanged.Index.tag);
            if (i2 == 1) {
                int iB = b(string2);
                if (iB < 0) {
                    b bVar = new b();
                    bVar.tag = string2;
                    bVar.r = new ArrayList();
                    bVar.r.add(string);
                    this.l.add(bVar);
                } else {
                    b bVar2 = this.l.get(iB);
                    if (a(string, bVar2.r)) {
                        return;
                    } else {
                        bVar2.r.add(string);
                    }
                }
                int iC = c(string);
                if (iC < 0) {
                    a aVar = new a();
                    aVar.packageName = string;
                    aVar.p = 1;
                    this.k.add(aVar);
                } else {
                    this.k.get(iC).p++;
                }
            } else if (i2 == 0) {
                for (int i3 = 0; i3 < this.l.size(); i3++) {
                    this.l.get(i3).r.remove(string);
                }
                int iC2 = c(string);
                if (iC2 >= 0) {
                    this.k.get(iC2).p = 0;
                }
            }
            if (!this.o || this.n) {
                Slog.d("SuppressionAction", "onPackageStoppedStatusChanged: packageName = " + string + " suppressAction = " + i2 + " tag = " + string2);
            }
        }
    }

    public List<String> getUnsuppressPackageList(String str) {
        synchronized (this) {
            ArrayList arrayList = new ArrayList();
            int iB = b(str);
            if (iB >= 0) {
                b bVar = this.l.get(iB);
                for (int i2 = 0; i2 < bVar.r.size(); i2++) {
                    int iC = c(bVar.r.get(i2));
                    if (iC >= 0) {
                        if (this.k.get(iC).p > 1) {
                            a aVar = this.k.get(iC);
                            aVar.p--;
                        } else {
                            this.k.get(iC).p = 0;
                            arrayList.add(bVar.r.get(i2));
                        }
                    }
                }
                this.l.remove(iB);
                if (this.n) {
                    Slog.d("SuppressionAction", "getUnsuppressPackageList: tag = " + str + " unSuppressPackageList.size() = " + arrayList.size());
                }
                return arrayList;
            }
            if (this.n) {
                Slog.d("SuppressionAction", "getUnsuppressPackageList: tag = " + str + " not found.");
            }
            return null;
        }
    }

    private int b(String str) {
        int i2 = 0;
        if (str == null) {
            return -1;
        }
        while (true) {
            int i3 = i2;
            if (i3 >= this.l.size()) {
                return -1;
            }
            if (!str.equals(this.l.get(i3).tag)) {
                i2 = i3 + 1;
            } else {
                return i3;
            }
        }
    }

    private int c(String str) {
        int i2 = 0;
        if (str == null) {
            return -1;
        }
        while (true) {
            int i3 = i2;
            if (i3 >= this.k.size()) {
                return -1;
            }
            if (!str.equals(this.k.get(i3).packageName)) {
                i2 = i3 + 1;
            } else {
                return i3;
            }
        }
    }

    private boolean a(String str, List<String> list) {
        if (str == null) {
            return false;
        }
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (str.equals(list.get(i2))) {
                return true;
            }
        }
        return false;
    }

    public AMEventHookResult onBeforeSendBroadcast(AMEventHookData.BeforeSendBroadcast beforeSendBroadcast, AMEventHookResult aMEventHookResult) {
        boolean z;
        boolean z2 = false;
        ArrayList<String> suppressionList = j.getSuppressionList();
        if (suppressionList != null) {
            int i2 = 0;
            boolean z3 = false;
            while (i2 < suppressionList.size()) {
                String str = suppressionList.get(i2);
                if ((j.d(str) & 2097152) == 0) {
                    z = z3;
                } else {
                    List list = (List) beforeSendBroadcast.get(AMEventHookData.BeforeSendBroadcast.Index.filterStaticList);
                    if (this.m == null) {
                        this.m = new ArrayList();
                        List<ApplicationInfo> installedApplications = mContext.getPackageManager().getInstalledApplications(8704);
                        List<ResolveInfo> listQueryIntentActivities = mContext.getPackageManager().queryIntentActivities(new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER"), 512);
                        for (ApplicationInfo applicationInfo : installedApplications) {
                            String str2 = applicationInfo.packageName;
                            if ((applicationInfo.flags & 1) != 0 && b(str2, listQueryIntentActivities)) {
                                this.m.add(str2);
                            }
                        }
                    }
                    boolean z4 = z3;
                    for (int i3 = 0; i3 < this.m.size(); i3++) {
                        String str3 = this.m.get(i3);
                        if (j.isPackageInSuppression(str, str3, 9999)) {
                            list.add(str3);
                            z4 = true;
                        }
                    }
                    z = z4;
                }
                i2++;
                z3 = z;
            }
            z2 = z3;
        }
        if (z2) {
            aMEventHookResult.addAction(AMEventHookAction.AM_FilterStaticReceiver);
        }
        return aMEventHookResult;
    }

    private boolean b(String str, List<ResolveInfo> list) {
        for (int i2 = 0; i2 < list.size(); i2++) {
            if (str.equals(list.get(i2).activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    class b {
        List<String> r;
        String tag;

        b() {
        }
    }

    class a {
        int p;
        String packageName;

        a() {
        }
    }
}
