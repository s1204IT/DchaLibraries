package com.android.launcher3.logging;

import android.os.Process;
import android.text.TextUtils;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.model.nano.LauncherDumpProto;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DumpTargetWrapper {
    ArrayList<DumpTargetWrapper> children;
    LauncherDumpProto.DumpTarget node;

    public DumpTargetWrapper() {
        this.children = new ArrayList<>();
    }

    public DumpTargetWrapper(int i, int i2) {
        this();
        this.node = newContainerTarget(i, i2);
    }

    public DumpTargetWrapper(ItemInfo itemInfo) {
        this();
        this.node = newItemTarget(itemInfo);
    }

    public LauncherDumpProto.DumpTarget getDumpTarget() {
        return this.node;
    }

    public void add(DumpTargetWrapper dumpTargetWrapper) {
        this.children.add(dumpTargetWrapper);
    }

    public List<LauncherDumpProto.DumpTarget> getFlattenedList() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(this.node);
        if (!this.children.isEmpty()) {
            Iterator<DumpTargetWrapper> it = this.children.iterator();
            while (it.hasNext()) {
                arrayList.addAll(it.next().getFlattenedList());
            }
            arrayList.add(this.node);
        }
        return arrayList;
    }

    public LauncherDumpProto.DumpTarget newItemTarget(ItemInfo itemInfo) {
        LauncherDumpProto.DumpTarget dumpTarget = new LauncherDumpProto.DumpTarget();
        dumpTarget.type = 1;
        int i = itemInfo.itemType;
        if (i == 4) {
            dumpTarget.itemType = 2;
        } else if (i != 6) {
            switch (i) {
                case 0:
                    dumpTarget.itemType = 1;
                    break;
                case 1:
                    dumpTarget.itemType = 0;
                    break;
            }
        } else {
            dumpTarget.itemType = 3;
        }
        return dumpTarget;
    }

    public LauncherDumpProto.DumpTarget newContainerTarget(int i, int i2) {
        LauncherDumpProto.DumpTarget dumpTarget = new LauncherDumpProto.DumpTarget();
        dumpTarget.type = 2;
        dumpTarget.containerType = i;
        dumpTarget.pageId = i2;
        return dumpTarget;
    }

    public static String getDumpTargetStr(LauncherDumpProto.DumpTarget dumpTarget) {
        if (dumpTarget == null) {
            return "";
        }
        switch (dumpTarget.type) {
            case 1:
                return getItemStr(dumpTarget);
            case 2:
                String fieldName = LoggerUtils.getFieldName(dumpTarget.containerType, LauncherDumpProto.ContainerType.class);
                if (dumpTarget.containerType == 1) {
                    return fieldName + " id=" + dumpTarget.pageId;
                }
                if (dumpTarget.containerType == 3) {
                    return fieldName + " grid(" + dumpTarget.gridX + "," + dumpTarget.gridY + ")";
                }
                return fieldName;
            default:
                return "UNKNOWN TARGET TYPE";
        }
    }

    private static String getItemStr(LauncherDumpProto.DumpTarget dumpTarget) {
        String fieldName = LoggerUtils.getFieldName(dumpTarget.itemType, LauncherDumpProto.ItemType.class);
        if (!TextUtils.isEmpty(dumpTarget.packageName)) {
            fieldName = fieldName + ", package=" + dumpTarget.packageName;
        }
        if (!TextUtils.isEmpty(dumpTarget.component)) {
            fieldName = fieldName + ", component=" + dumpTarget.component;
        }
        return fieldName + ", grid(" + dumpTarget.gridX + "," + dumpTarget.gridY + "), span(" + dumpTarget.spanX + "," + dumpTarget.spanY + "), pageIdx=" + dumpTarget.pageId + " user=" + dumpTarget.userType;
    }

    public LauncherDumpProto.DumpTarget writeToDumpTarget(ItemInfo itemInfo) {
        this.node.component = itemInfo.getTargetComponent() == null ? "" : itemInfo.getTargetComponent().flattenToString();
        this.node.packageName = itemInfo.getTargetComponent() == null ? "" : itemInfo.getTargetComponent().getPackageName();
        if (itemInfo instanceof LauncherAppWidgetInfo) {
            LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) itemInfo;
            this.node.component = launcherAppWidgetInfo.providerName.flattenToString();
            this.node.packageName = launcherAppWidgetInfo.providerName.getPackageName();
        }
        this.node.gridX = itemInfo.cellX;
        this.node.gridY = itemInfo.cellY;
        this.node.spanX = itemInfo.spanX;
        this.node.spanY = itemInfo.spanY;
        this.node.userType = !itemInfo.user.equals(Process.myUserHandle()) ? 1 : 0;
        return this.node;
    }
}
