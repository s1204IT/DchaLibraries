package com.android.launcher3.model;

import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableInt;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.InstallShortcutReceiver;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.logging.DumpTargetWrapper;
import com.android.launcher3.model.nano.LauncherDumpProto;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.MultiHashMap;
import com.google.protobuf.nano.MessageNano;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class BgDataModel {
    private static final String TAG = "BgDataModel";
    public boolean hasShortcutHostPermission;
    public final LongArrayMap<ItemInfo> itemsIdMap = new LongArrayMap<>();
    public final ArrayList<ItemInfo> workspaceItems = new ArrayList<>();
    public final ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<>();
    public final LongArrayMap<FolderInfo> folders = new LongArrayMap<>();
    public final ArrayList<Long> workspaceScreens = new ArrayList<>();
    public final Map<ShortcutKey, MutableInt> pinnedShortcutCounts = new HashMap();
    public final MultiHashMap<ComponentKey, String> deepShortcutMap = new MultiHashMap<>();
    public final WidgetsModel widgetsModel = new WidgetsModel();
    public int lastBindId = 0;

    public synchronized void clear() {
        this.workspaceItems.clear();
        this.appWidgets.clear();
        this.folders.clear();
        this.itemsIdMap.clear();
        this.workspaceScreens.clear();
        this.pinnedShortcutCounts.clear();
        this.deepShortcutMap.clear();
    }

    public synchronized void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (Arrays.asList(strArr).contains("--proto")) {
            dumpProto(str, fileDescriptor, printWriter, strArr);
            return;
        }
        printWriter.println(str + "Data Model:");
        printWriter.print(str + " ---- workspace screens: ");
        for (int i = 0; i < this.workspaceScreens.size(); i++) {
            printWriter.print(" " + this.workspaceScreens.get(i).toString());
        }
        printWriter.println();
        printWriter.println(str + " ---- workspace items ");
        for (int i2 = 0; i2 < this.workspaceItems.size(); i2++) {
            printWriter.println(str + '\t' + this.workspaceItems.get(i2).toString());
        }
        printWriter.println(str + " ---- appwidget items ");
        for (int i3 = 0; i3 < this.appWidgets.size(); i3++) {
            printWriter.println(str + '\t' + this.appWidgets.get(i3).toString());
        }
        printWriter.println(str + " ---- folder items ");
        for (int i4 = 0; i4 < this.folders.size(); i4++) {
            printWriter.println(str + '\t' + this.folders.valueAt(i4).toString());
        }
        printWriter.println(str + " ---- items id map ");
        for (int i5 = 0; i5 < this.itemsIdMap.size(); i5++) {
            printWriter.println(str + '\t' + this.itemsIdMap.valueAt(i5).toString());
        }
        if (strArr.length > 0 && TextUtils.equals(strArr[0], "--all")) {
            printWriter.println(str + "shortcuts");
            Iterator<String> it = this.deepShortcutMap.values().iterator();
            while (it.hasNext()) {
                ArrayList arrayList = (ArrayList) it.next();
                printWriter.print(str + "  ");
                Iterator it2 = arrayList.iterator();
                while (it2.hasNext()) {
                    printWriter.print(((String) it2.next()) + ", ");
                }
                printWriter.println();
            }
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v15, resolved type: E */
    /* JADX DEBUG: Multi-variable search result rejected for r5v21, resolved type: E */
    /* JADX DEBUG: Multi-variable search result rejected for r5v26, resolved type: E */
    /* JADX DEBUG: Multi-variable search result rejected for r5v32, resolved type: E */
    /* JADX WARN: Multi-variable type inference failed */
    private synchronized void dumpProto(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        int i = 0;
        DumpTargetWrapper dumpTargetWrapper = new DumpTargetWrapper(2, 0);
        LongArrayMap longArrayMap = new LongArrayMap();
        for (int i2 = 0; i2 < this.workspaceScreens.size(); i2++) {
            longArrayMap.put(this.workspaceScreens.get(i2).longValue(), new DumpTargetWrapper(1, i2));
        }
        for (int i3 = 0; i3 < this.folders.size(); i3++) {
            FolderInfo folderInfoValueAt = this.folders.valueAt(i3);
            DumpTargetWrapper dumpTargetWrapper2 = new DumpTargetWrapper(3, this.folders.size());
            dumpTargetWrapper2.writeToDumpTarget(folderInfoValueAt);
            Iterator<ShortcutInfo> it = folderInfoValueAt.contents.iterator();
            while (it.hasNext()) {
                ShortcutInfo next = it.next();
                DumpTargetWrapper dumpTargetWrapper3 = new DumpTargetWrapper(next);
                dumpTargetWrapper3.writeToDumpTarget(next);
                dumpTargetWrapper2.add(dumpTargetWrapper3);
            }
            if (folderInfoValueAt.container == -101) {
                dumpTargetWrapper.add(dumpTargetWrapper2);
            } else if (folderInfoValueAt.container == -100) {
                ((DumpTargetWrapper) longArrayMap.get(folderInfoValueAt.screenId)).add(dumpTargetWrapper2);
            }
        }
        for (int i4 = 0; i4 < this.workspaceItems.size(); i4++) {
            ItemInfo itemInfo = this.workspaceItems.get(i4);
            if (!(itemInfo instanceof FolderInfo)) {
                DumpTargetWrapper dumpTargetWrapper4 = new DumpTargetWrapper(itemInfo);
                dumpTargetWrapper4.writeToDumpTarget(itemInfo);
                if (itemInfo.container == -101) {
                    dumpTargetWrapper.add(dumpTargetWrapper4);
                } else if (itemInfo.container == -100) {
                    ((DumpTargetWrapper) longArrayMap.get(itemInfo.screenId)).add(dumpTargetWrapper4);
                }
            }
        }
        for (int i5 = 0; i5 < this.appWidgets.size(); i5++) {
            LauncherAppWidgetInfo launcherAppWidgetInfo = this.appWidgets.get(i5);
            DumpTargetWrapper dumpTargetWrapper5 = new DumpTargetWrapper(launcherAppWidgetInfo);
            dumpTargetWrapper5.writeToDumpTarget(launcherAppWidgetInfo);
            if (launcherAppWidgetInfo.container == -101) {
                dumpTargetWrapper.add(dumpTargetWrapper5);
            } else if (launcherAppWidgetInfo.container == -100) {
                ((DumpTargetWrapper) longArrayMap.get(launcherAppWidgetInfo.screenId)).add(dumpTargetWrapper5);
            }
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(dumpTargetWrapper.getFlattenedList());
        for (int i6 = 0; i6 < longArrayMap.size(); i6++) {
            arrayList.addAll(((DumpTargetWrapper) longArrayMap.valueAt(i6)).getFlattenedList());
        }
        if (Arrays.asList(strArr).contains("--debug")) {
            while (i < arrayList.size()) {
                printWriter.println(str + DumpTargetWrapper.getDumpTargetStr((LauncherDumpProto.DumpTarget) arrayList.get(i)));
                i++;
            }
            return;
        }
        LauncherDumpProto.LauncherImpression launcherImpression = new LauncherDumpProto.LauncherImpression();
        launcherImpression.targets = new LauncherDumpProto.DumpTarget[arrayList.size()];
        while (i < arrayList.size()) {
            launcherImpression.targets[i] = (LauncherDumpProto.DumpTarget) arrayList.get(i);
            i++;
        }
        try {
            new FileOutputStream(fileDescriptor).write(MessageNano.toByteArray(launcherImpression));
            Log.d(TAG, MessageNano.toByteArray(launcherImpression).length + "Bytes");
        } catch (IOException e) {
            Log.e(TAG, "Exception writing dumpsys --proto", e);
        }
    }

    public synchronized void removeItem(Context context, ItemInfo... itemInfoArr) {
        removeItem(context, Arrays.asList(itemInfoArr));
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x002d A[Catch: all -> 0x0062, TryCatch #0 {, blocks: (B:3:0x0001, B:4:0x0005, B:6:0x000b, B:7:0x0013, B:19:0x0058, B:9:0x0017, B:11:0x0025, B:13:0x002d, B:15:0x0037, B:16:0x003f, B:17:0x0045, B:18:0x0052), top: B:26:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void removeItem(Context context, Iterable<? extends ItemInfo> iterable) {
        for (ItemInfo itemInfo : iterable) {
            switch (itemInfo.itemType) {
                case 0:
                case 1:
                    break;
                case 2:
                    this.folders.remove(itemInfo.id);
                    this.workspaceItems.remove(itemInfo);
                    continue;
                    this.itemsIdMap.remove(itemInfo.id);
                case 3:
                default:
                    continue;
                    this.itemsIdMap.remove(itemInfo.id);
                case 4:
                case 5:
                    this.appWidgets.remove(itemInfo);
                    continue;
                    this.itemsIdMap.remove(itemInfo.id);
                case 6:
                    ShortcutKey shortcutKeyFromItemInfo = ShortcutKey.fromItemInfo(itemInfo);
                    MutableInt mutableInt = this.pinnedShortcutCounts.get(shortcutKeyFromItemInfo);
                    if (mutableInt != null) {
                        int i = mutableInt.value - 1;
                        mutableInt.value = i;
                        if (i == 0) {
                            if (!InstallShortcutReceiver.getPendingShortcuts(context).contains(shortcutKeyFromItemInfo)) {
                                DeepShortcutManager.getInstance(context).unpinShortcut(shortcutKeyFromItemInfo);
                                break;
                            }
                        }
                    }
                    this.itemsIdMap.remove(itemInfo.id);
                    break;
            }
            this.workspaceItems.remove(itemInfo);
            this.itemsIdMap.remove(itemInfo.id);
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:26:0x009a A[Catch: all -> 0x00a1, TRY_LEAVE, TryCatch #0 {, blocks: (B:3:0x0001, B:4:0x000a, B:6:0x000f, B:8:0x001e, B:11:0x0030, B:13:0x0034, B:9:0x0029, B:14:0x003c, B:15:0x0044, B:16:0x0054, B:18:0x005c, B:22:0x0067, B:24:0x0071, B:25:0x008d, B:26:0x009a), top: B:32:0x0001 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public synchronized void addItem(Context context, ItemInfo itemInfo, boolean z) {
        this.itemsIdMap.put(itemInfo.id, itemInfo);
        switch (itemInfo.itemType) {
            case 0:
            case 1:
                if (itemInfo.container != -100 || itemInfo.container == -101) {
                    this.workspaceItems.add(itemInfo);
                    break;
                } else if (z) {
                    if (!this.folders.containsKey(itemInfo.container)) {
                        Log.e(TAG, "adding item: " + itemInfo + " to a folder that  doesn't exist");
                        break;
                    }
                } else {
                    findOrMakeFolder(itemInfo.container).add((ShortcutInfo) itemInfo, false);
                    break;
                }
                break;
            case 2:
                this.folders.put(itemInfo.id, (FolderInfo) itemInfo);
                this.workspaceItems.add(itemInfo);
                break;
            case 4:
            case 5:
                this.appWidgets.add((LauncherAppWidgetInfo) itemInfo);
                break;
            case 6:
                ShortcutKey shortcutKeyFromItemInfo = ShortcutKey.fromItemInfo(itemInfo);
                MutableInt mutableInt = this.pinnedShortcutCounts.get(shortcutKeyFromItemInfo);
                if (mutableInt != null) {
                    mutableInt.value++;
                } else {
                    mutableInt = new MutableInt(1);
                    this.pinnedShortcutCounts.put(shortcutKeyFromItemInfo, mutableInt);
                }
                if (z && mutableInt.value == 1) {
                    DeepShortcutManager.getInstance(context).pinShortcut(shortcutKeyFromItemInfo);
                }
                if (itemInfo.container != -100) {
                    this.workspaceItems.add(itemInfo);
                    break;
                }
                break;
        }
    }

    public synchronized FolderInfo findOrMakeFolder(long j) {
        FolderInfo folderInfo;
        folderInfo = this.folders.get(j);
        if (folderInfo == null) {
            folderInfo = new FolderInfo();
            this.folders.put(j, folderInfo);
        }
        return folderInfo;
    }

    public synchronized void updateDeepShortcutMap(String str, UserHandle userHandle, List<ShortcutInfoCompat> list) {
        if (str != null) {
            try {
                Iterator<ComponentKey> it = this.deepShortcutMap.keySet().iterator();
                while (it.hasNext()) {
                    ComponentKey next = it.next();
                    if (next.componentName.getPackageName().equals(str) && next.user.equals(userHandle)) {
                        it.remove();
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        for (ShortcutInfoCompat shortcutInfoCompat : list) {
            if (shortcutInfoCompat.isEnabled() && (shortcutInfoCompat.isDeclaredInManifest() || shortcutInfoCompat.isDynamic())) {
                this.deepShortcutMap.addToList(new ComponentKey(shortcutInfoCompat.getActivity(), shortcutInfoCompat.getUserHandle()), shortcutInfoCompat.getId());
            }
        }
    }
}
