package com.mediatek.apm.frc;

import android.os.Build;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Slog;
import com.mediatek.am.AMEventHookData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class FocusRelationshipChainPolicy {
    public static final int FRC_ADD_POLICY_EMPTY = 0;
    public static final int FRC_ADD_POLICY_FROM_EXTRA_ALLOW_LIST = 1;
    public static final int FRC_ADD_POLICY_FROM_FRC = 8;
    public static final int FRC_ADD_POLICY_FROM_OTHERS = 4;
    public static final int FRC_ADD_POLICY_FROM_SYSTEM_CALLER = 2;
    private static FocusRelationshipChainPolicy c = null;
    private boolean a;
    private boolean b = "user".equals(Build.TYPE);
    private ArrayMap<String, a> d;

    private FocusRelationshipChainPolicy() {
        this.a = false;
        new com.mediatek.common.jpe.a().a();
        this.d = new ArrayMap<>();
        this.a = SystemProperties.get("persist.sys.apm.debug_mode").equals("1");
    }

    public static FocusRelationshipChainPolicy getInstance() {
        if (c == null) {
            c = new FocusRelationshipChainPolicy();
        }
        return c;
    }

    public void startFrc(String str, int i, List<String> list) {
        synchronized (this) {
            if (str == null) {
                Slog.w("FocusRelationshipChain", "The tag is null");
                return;
            }
            if (a(str)) {
                Slog.w("FocusRelationshipChain", "The tag:" + str + " exist");
                return;
            }
            a aVar = new a();
            aVar.e = i;
            aVar.f = list;
            aVar.g = new HashSet<>();
            this.d.put(str, aVar);
            Slog.v("FocusRelationshipChain", "startFrc(" + str + ", " + i + ")");
        }
    }

    public void stopFrc(String str) {
        synchronized (this) {
            if (str == null) {
                Slog.w("FocusRelationshipChain", "The tag is null");
            } else if (!a(str)) {
                Slog.w("FocusRelationshipChain", "The tag:" + str + " does not exist");
            } else {
                this.d.remove(str);
                Slog.v("FocusRelationshipChain", "stopFrc(" + str + ")");
            }
        }
    }

    public boolean isPackageInFrc(String str, String str2) {
        synchronized (this) {
            if (str == null) {
                Slog.w("FocusRelationshipChain", "The tag is null");
                return false;
            }
            if (!a(str)) {
                Slog.w("FocusRelationshipChain", "The tag:" + str + " does not exist");
                return false;
            }
            return this.d.get(str).g.contains(str2);
        }
    }

    public ArrayList<String> getFrcPackageList(String str) {
        synchronized (this) {
            if (str == null) {
                Slog.w("FocusRelationshipChain", "The tag is null");
                return null;
            }
            if (!a(str)) {
                Slog.w("FocusRelationshipChain", "The tag:" + str + " does not exist");
                return null;
            }
            return new ArrayList<>(this.d.get(str).g);
        }
    }

    public void updateFrcExtraAllowList(String str, List<String> list) {
        synchronized (this) {
            if (str == null) {
                Slog.w("FocusRelationshipChain", "The tag is null");
            } else if (!a(str)) {
                Slog.w("FocusRelationshipChain", "The tag:" + str + " does not exist");
            } else {
                this.d.get(str).f = list;
            }
        }
    }

    public int getUserAddPolicy(String str) {
        synchronized (this) {
            if (str == null) {
                Slog.w("FocusRelationshipChain", "The tag is null");
                return 0;
            }
            if (!a(str)) {
                Slog.w("FocusRelationshipChain", "The tag:" + str + " does not exist");
                return 0;
            }
            return this.d.get(str).e;
        }
    }

    public void onStartActivity(AMEventHookData.BeforeActivitySwitch beforeActivitySwitch) {
        String string = beforeActivitySwitch.getString(AMEventHookData.BeforeActivitySwitch.Index.nextResumedPackageName);
        boolean z = beforeActivitySwitch.getBoolean(AMEventHookData.BeforeActivitySwitch.Index.isNeedToPauseActivityFirst);
        ArrayList<String> arrayList = (ArrayList) beforeActivitySwitch.get(AMEventHookData.BeforeActivitySwitch.Index.nextTaskPackageList);
        if (!z) {
            a(string, arrayList);
        }
    }

    public void onStartService(AMEventHookData.ReadyToStartService readyToStartService) {
        a(readyToStartService.getString(AMEventHookData.ReadyToStartService.Index.packageName), readyToStartService.getString(AMEventHookData.ReadyToStartService.Index.callerPackageName), readyToStartService.getInt(AMEventHookData.ReadyToStartService.Index.callerUid), (ArrayList) readyToStartService.get(AMEventHookData.ReadyToStartService.Index.bindCallerPkgList), (ArrayList) readyToStartService.get(AMEventHookData.ReadyToStartService.Index.bindCallerUidList), (ArrayList) readyToStartService.get(AMEventHookData.ReadyToStartService.Index.delayedCallerPkgList), (ArrayList) readyToStartService.get(AMEventHookData.ReadyToStartService.Index.delayedCallerUidList), readyToStartService.getString(AMEventHookData.ReadyToStartService.Index.reason));
    }

    public void onStartDynamicReceiver(AMEventHookData.ReadyToStartDynamicReceiver readyToStartDynamicReceiver) {
        a(readyToStartDynamicReceiver.getString(AMEventHookData.ReadyToStartDynamicReceiver.Index.packageName), readyToStartDynamicReceiver.getString(AMEventHookData.ReadyToStartDynamicReceiver.Index.callerPackageName), readyToStartDynamicReceiver.getInt(AMEventHookData.ReadyToStartDynamicReceiver.Index.callerUid));
    }

    public void onStartStaticReceiver(AMEventHookData.ReadyToStartStaticReceiver readyToStartStaticReceiver) {
        a(readyToStartStaticReceiver.getString(AMEventHookData.ReadyToStartStaticReceiver.Index.packageName), readyToStartStaticReceiver.getString(AMEventHookData.ReadyToStartStaticReceiver.Index.callerPackageName), readyToStartStaticReceiver.getInt(AMEventHookData.ReadyToStartStaticReceiver.Index.callerUid));
    }

    public void onStartProvider(AMEventHookData.ReadyToGetProvider readyToGetProvider) {
        a(readyToGetProvider.getString(AMEventHookData.ReadyToGetProvider.Index.packageName), (ArrayList<String>) readyToGetProvider.get(AMEventHookData.ReadyToGetProvider.Index.callerPackageNameList), readyToGetProvider.getInt(AMEventHookData.ReadyToGetProvider.Index.callerUid));
    }

    private void a(String str, ArrayList<String> arrayList) {
        synchronized (this) {
            for (Map.Entry<String, a> entry : this.d.entrySet()) {
                a value = entry.getValue();
                value.g.add(str);
                if (this.a) {
                    Slog.v("FocusRelationshipChain", "add " + str + " to " + entry.getKey() + " by activity");
                }
                if (arrayList != null) {
                    for (int i = 0; i < arrayList.size(); i++) {
                        String str2 = arrayList.get(i);
                        value.g.add(str2);
                        if (this.a) {
                            Slog.v("FocusRelationshipChain", "add " + str2 + " to " + entry.getKey() + " by task of activity");
                        }
                    }
                }
            }
        }
    }

    private void a(String str, String str2, int i, ArrayList<String> arrayList, ArrayList<Integer> arrayList2, ArrayList<String> arrayList3, ArrayList<Integer> arrayList4, String str3) {
        boolean z;
        boolean z2;
        synchronized (this) {
            for (Map.Entry<String, a> entry : this.d.entrySet()) {
                a value = entry.getValue();
                if (!value.g.contains(str)) {
                    if (str3.equalsIgnoreCase("bind service") || str3.equalsIgnoreCase("start service")) {
                        if (a(value, str2, i)) {
                            value.g.add(str);
                            if (this.a) {
                                Slog.v("FocusRelationshipChain", "add " + str + " to " + entry.getKey() + " by start/bind service");
                            }
                        } else if (this.a) {
                            Slog.v("FocusRelationshipChain", str + " is not added by start/bind service,  caller is " + str2 + ", " + i);
                        }
                    } else if (str3.equalsIgnoreCase("restart service")) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= arrayList.size()) {
                                z2 = false;
                                break;
                            } else if (!a(value, arrayList.get(i2), arrayList2.get(i2).intValue())) {
                                i2++;
                            } else {
                                value.g.add(str);
                                if (this.a) {
                                    Slog.v("FocusRelationshipChain", "add " + str + " to " + entry.getKey() + " by restart service");
                                }
                                z2 = true;
                            }
                        }
                        if (this.a && !z2) {
                            Slog.v("FocusRelationshipChain", str + " is not added by restart service");
                            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                                Slog.v("FocusRelationshipChain", "caller is " + arrayList.get(i3) + ", " + arrayList2.get(i3).intValue());
                            }
                        }
                    } else if (str3.equalsIgnoreCase("delayed service")) {
                        int i4 = 0;
                        while (true) {
                            if (i4 >= arrayList3.size()) {
                                z = false;
                                break;
                            } else if (!a(value, arrayList3.get(i4), arrayList4.get(i4).intValue())) {
                                i4++;
                            } else {
                                value.g.add(str);
                                if (this.a) {
                                    Slog.v("FocusRelationshipChain", "add " + str + " to " + entry.getKey() + " by delay start service");
                                }
                                z = true;
                            }
                        }
                        if (this.a && !z) {
                            Slog.v("FocusRelationshipChain", str + " is not added by delay start service");
                            for (int i5 = 0; i5 < arrayList3.size(); i5++) {
                                Slog.v("FocusRelationshipChain", "caller is " + arrayList3.get(i5) + ", " + arrayList4.get(i5).intValue());
                            }
                        }
                    }
                }
            }
        }
    }

    private void a(String str, String str2, int i) {
        synchronized (this) {
            for (Map.Entry<String, a> entry : this.d.entrySet()) {
                a value = entry.getValue();
                if (!value.g.contains(str)) {
                    if (a(value, str2, i)) {
                        value.g.add(str);
                        if (this.a) {
                            Slog.v("FocusRelationshipChain", "add " + str + " to " + entry.getKey() + " by receiver");
                        }
                    } else if (this.a) {
                        Slog.v("FocusRelationshipChain", str + " is not added by receiver,  caller is " + str2 + ", " + i);
                    }
                }
            }
        }
    }

    private void a(String str, ArrayList<String> arrayList, int i) {
        boolean z;
        synchronized (this) {
            for (Map.Entry<String, a> entry : this.d.entrySet()) {
                a value = entry.getValue();
                if (!value.g.contains(str)) {
                    if (arrayList != null) {
                        int i2 = 0;
                        while (true) {
                            if (i2 >= arrayList.size()) {
                                z = false;
                                break;
                            } else if (!a(value, arrayList.get(i2), i)) {
                                i2++;
                            } else {
                                value.g.add(str);
                                if (this.a) {
                                    Slog.v("FocusRelationshipChain", "add " + str + " to " + entry.getKey() + " by provider");
                                }
                                z = true;
                            }
                        }
                        if (this.a && !z) {
                            Slog.v("FocusRelationshipChain", str + " is not added by provider");
                            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                                Slog.v("FocusRelationshipChain", "caller is " + arrayList.get(i3) + ", " + i);
                            }
                        }
                    } else if (this.a) {
                        Slog.v("FocusRelationshipChain", str + " is not added by provider, caller is null");
                    }
                }
            }
        }
    }

    private boolean a(a aVar, String str, int i) {
        if (aVar.g.contains(str)) {
            if (!this.b || this.a) {
                Slog.v("FocusRelationshipChain", "AddedByPolicy from 8");
            }
            return true;
        }
        if ((aVar.e & 1) != 0 && aVar.f != null && aVar.f.contains(str)) {
            if (!this.b || this.a) {
                Slog.v("FocusRelationshipChain", "AddedByPolicy from 1");
            }
            return true;
        }
        if ((aVar.e & 2) != 0 && i == 1000) {
            if (!this.b || this.a) {
                Slog.v("FocusRelationshipChain", "AddedByPolicy from 2");
            }
            return true;
        }
        if ((aVar.e & 4) == 0) {
            return false;
        }
        if (!this.b || this.a) {
            Slog.v("FocusRelationshipChain", "AddedByPolicy from 4");
        }
        return true;
    }

    private boolean a(String str) {
        return this.d.containsKey(str);
    }

    class a {
        int e;
        List<String> f;
        HashSet<String> g;

        a() {
        }
    }
}
