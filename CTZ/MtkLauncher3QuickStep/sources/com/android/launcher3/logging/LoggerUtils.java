package com.android.launcher3.logging;

import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.View;
import com.android.launcher3.AppInfo;
import com.android.launcher3.ButtonDropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.userevent.nano.LauncherLogExtensions;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.InstantAppResolver;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class LoggerUtils {
    private static final String UNKNOWN = "UNKNOWN";
    private static final ArrayMap<Class, SparseArray<String>> sNameCache = new ArrayMap<>();

    public static String getFieldName(int i, Class cls) {
        SparseArray<String> sparseArray;
        synchronized (sNameCache) {
            sparseArray = sNameCache.get(cls);
            if (sparseArray == null) {
                sparseArray = new SparseArray<>();
                for (Field field : cls.getDeclaredFields()) {
                    if (field.getType() == Integer.TYPE && Modifier.isStatic(field.getModifiers())) {
                        try {
                            field.setAccessible(true);
                            sparseArray.put(field.getInt(null), field.getName());
                        } catch (IllegalAccessException e) {
                        }
                    }
                }
                sNameCache.put(cls, sparseArray);
            }
        }
        String str = sparseArray.get(i);
        return str != null ? str : UNKNOWN;
    }

    public static String getActionStr(LauncherLogProto.Action action) {
        int i = action.type;
        if (i != 0) {
            if (i == 2) {
                return getFieldName(action.command, LauncherLogProto.Action.Command.class);
            }
            return getFieldName(action.type, LauncherLogProto.Action.Type.class);
        }
        String str = "" + getFieldName(action.touch, LauncherLogProto.Action.Touch.class);
        if (action.touch == 3 || action.touch == 4) {
            return str + " direction=" + getFieldName(action.dir, LauncherLogProto.Action.Direction.class);
        }
        return str;
    }

    public static String getTargetStr(LauncherLogProto.Target target) {
        String itemStr;
        if (target == null) {
            return "";
        }
        switch (target.type) {
            case 1:
                itemStr = getItemStr(target);
                break;
            case 2:
                itemStr = getFieldName(target.controlType, LauncherLogProto.ControlType.class);
                break;
            case 3:
                itemStr = getFieldName(target.containerType, LauncherLogProto.ContainerType.class);
                if (target.containerType == 1 || target.containerType == 2) {
                    itemStr = itemStr + " id=" + target.pageIndex;
                } else if (target.containerType == 3) {
                    itemStr = itemStr + " grid(" + target.gridX + "," + target.gridY + ")";
                }
                break;
            default:
                itemStr = "UNKNOWN TARGET TYPE";
                break;
        }
        if (target.tipType != 0) {
            return itemStr + " " + getFieldName(target.tipType, LauncherLogProto.TipType.class);
        }
        return itemStr;
    }

    private static String getItemStr(LauncherLogProto.Target target) {
        String fieldName = getFieldName(target.itemType, LauncherLogProto.ItemType.class);
        if (target.packageNameHash != 0) {
            fieldName = fieldName + ", packageHash=" + target.packageNameHash;
        }
        if (target.componentHash != 0) {
            fieldName = fieldName + ", componentHash=" + target.componentHash;
        }
        if (target.intentHash != 0) {
            fieldName = fieldName + ", intentHash=" + target.intentHash;
        }
        if ((target.packageNameHash != 0 || target.componentHash != 0 || target.intentHash != 0) && target.itemType != 9) {
            fieldName = fieldName + ", predictiveRank=" + target.predictedRank + ", grid(" + target.gridX + "," + target.gridY + "), span(" + target.spanX + "," + target.spanY + "), pageIdx=" + target.pageIndex;
        }
        if (target.itemType == 9) {
            return fieldName + ", pageIdx=" + target.pageIndex;
        }
        return fieldName;
    }

    public static LauncherLogProto.Target newItemTarget(int i) {
        LauncherLogProto.Target targetNewTarget = newTarget(1);
        targetNewTarget.itemType = i;
        return targetNewTarget;
    }

    public static LauncherLogProto.Target newItemTarget(View view, InstantAppResolver instantAppResolver) {
        if (view.getTag() instanceof ItemInfo) {
            return newItemTarget((ItemInfo) view.getTag(), instantAppResolver);
        }
        return newTarget(1);
    }

    public static LauncherLogProto.Target newItemTarget(ItemInfo itemInfo, InstantAppResolver instantAppResolver) {
        int i = 1;
        LauncherLogProto.Target targetNewTarget = newTarget(1);
        int i2 = itemInfo.itemType;
        if (i2 == 4) {
            targetNewTarget.itemType = 3;
        } else if (i2 != 6) {
            switch (i2) {
                case 0:
                    if (instantAppResolver != null && (itemInfo instanceof AppInfo) && instantAppResolver.isInstantApp((AppInfo) itemInfo)) {
                        i = 10;
                    }
                    targetNewTarget.itemType = i;
                    targetNewTarget.predictedRank = -100;
                    break;
                case 1:
                    targetNewTarget.itemType = 2;
                    break;
                case 2:
                    targetNewTarget.itemType = 4;
                    break;
            }
        } else {
            targetNewTarget.itemType = 5;
        }
        return targetNewTarget;
    }

    public static LauncherLogProto.Target newDropTarget(View view) {
        boolean z = view instanceof ButtonDropTarget;
        if (!z) {
            return newTarget(3);
        }
        if (z) {
            return ((ButtonDropTarget) view).getDropTargetForLogging();
        }
        return newTarget(2);
    }

    public static LauncherLogProto.Target newTarget(int i, LauncherLogExtensions.TargetExtension targetExtension) {
        LauncherLogProto.Target target = new LauncherLogProto.Target();
        target.type = i;
        target.extension = targetExtension;
        return target;
    }

    public static LauncherLogProto.Target newTarget(int i) {
        LauncherLogProto.Target target = new LauncherLogProto.Target();
        target.type = i;
        return target;
    }

    public static LauncherLogProto.Target newControlTarget(int i) {
        LauncherLogProto.Target targetNewTarget = newTarget(2);
        targetNewTarget.controlType = i;
        return targetNewTarget;
    }

    public static LauncherLogProto.Target newContainerTarget(int i) {
        LauncherLogProto.Target targetNewTarget = newTarget(3);
        targetNewTarget.containerType = i;
        return targetNewTarget;
    }

    public static LauncherLogProto.Action newAction(int i) {
        LauncherLogProto.Action action = new LauncherLogProto.Action();
        action.type = i;
        return action;
    }

    public static LauncherLogProto.Action newCommandAction(int i) {
        LauncherLogProto.Action actionNewAction = newAction(2);
        actionNewAction.command = i;
        return actionNewAction;
    }

    public static LauncherLogProto.Action newTouchAction(int i) {
        LauncherLogProto.Action actionNewAction = newAction(0);
        actionNewAction.touch = i;
        return actionNewAction;
    }

    public static LauncherLogProto.LauncherEvent newLauncherEvent(LauncherLogProto.Action action, LauncherLogProto.Target... targetArr) {
        LauncherLogProto.LauncherEvent launcherEvent = new LauncherLogProto.LauncherEvent();
        launcherEvent.srcTarget = targetArr;
        launcherEvent.action = action;
        return launcherEvent;
    }
}
