package com.android.commands.monkey;

import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.content.pm.IPackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import com.android.commands.monkey.MonkeySourceNetwork;
import dalvik.system.DexClassLoader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MonkeySourceNetworkViews {
    private static final String CLASS_NOT_FOUND = "Error retrieving class information";
    private static final String HANDLER_THREAD_NAME = "UiAutomationHandlerThread";
    private static final String NO_ACCESSIBILITY_EVENT = "No accessibility event has occured yet";
    private static final String NO_CONNECTION = "Failed to connect to AccessibilityService, try restarting Monkey";
    private static final String NO_NODE = "Node with given ID does not exist";
    private static final String REMOTE_ERROR = "Unable to retrieve application info from PackageManager";
    private static final HandlerThread sHandlerThread;
    protected static UiAutomation sUiTestAutomationBridge;
    private static IPackageManager sPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    private static Map<String, Class<?>> sClassMap = new HashMap();
    private static final Map<String, ViewIntrospectionCommand> COMMAND_MAP = new HashMap();

    private interface ViewIntrospectionCommand {
        MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list);
    }

    static {
        COMMAND_MAP.put("getlocation", new GetLocation());
        COMMAND_MAP.put("gettext", new GetText());
        COMMAND_MAP.put("getclass", new GetClass());
        COMMAND_MAP.put("getchecked", new GetChecked());
        COMMAND_MAP.put("getenabled", new GetEnabled());
        COMMAND_MAP.put("getselected", new GetSelected());
        COMMAND_MAP.put("setselected", new SetSelected());
        COMMAND_MAP.put("getfocused", new GetFocused());
        COMMAND_MAP.put("setfocused", new SetFocused());
        COMMAND_MAP.put("getparent", new GetParent());
        COMMAND_MAP.put("getchildren", new GetChildren());
        COMMAND_MAP.put("getaccessibilityids", new GetAccessibilityIds());
        sHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
    }

    public static void setup() {
        sHandlerThread.setDaemon(true);
        sHandlerThread.start();
        sUiTestAutomationBridge = new UiAutomation(sHandlerThread.getLooper(), new UiAutomationConnection());
        sUiTestAutomationBridge.connect();
    }

    public static void teardown() {
        sHandlerThread.quit();
    }

    private static Class<?> getIdClass(String str, String str2) throws ClassNotFoundException {
        Class<?> cls = sClassMap.get(str);
        if (cls == null) {
            Class<?> clsLoadClass = new DexClassLoader(str2, "/data/local/tmp", null, ClassLoader.getSystemClassLoader()).loadClass(str + ".R$id");
            sClassMap.put(str, clsLoadClass);
            return clsLoadClass;
        }
        return cls;
    }

    private static AccessibilityNodeInfo getNodeByAccessibilityIds(String str, String str2) {
        return AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfoByAccessibilityId(sUiTestAutomationBridge.getConnectionId(), Integer.parseInt(str), Integer.parseInt(str2), false, 0, (Bundle) null);
    }

    private static AccessibilityNodeInfo getNodeByViewId(String str) throws MonkeyViewException {
        List listFindAccessibilityNodeInfosByViewId = AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByViewId(sUiTestAutomationBridge.getConnectionId(), Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, str);
        if (listFindAccessibilityNodeInfosByViewId.isEmpty()) {
            return null;
        }
        return (AccessibilityNodeInfo) listFindAccessibilityNodeInfosByViewId.get(0);
    }

    public static class ListViewsCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> list, MonkeySourceNetwork.CommandQueue commandQueue) {
            AccessibilityNodeInfo rootInActiveWindow = MonkeySourceNetworkViews.sUiTestAutomationBridge.getRootInActiveWindow();
            if (rootInActiveWindow == null) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.NO_ACCESSIBILITY_EVENT);
            }
            String string = rootInActiveWindow.getPackageName().toString();
            try {
                Class idClass = MonkeySourceNetworkViews.getIdClass(string, MonkeySourceNetworkViews.sPm.getApplicationInfo(string, 0, UserHandle.myUserId()).sourceDir);
                StringBuilder sb = new StringBuilder();
                for (Field field : idClass.getFields()) {
                    sb.append(field.getName() + " ");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, sb.toString());
            } catch (RemoteException e) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.REMOTE_ERROR);
            } catch (ClassNotFoundException e2) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.CLASS_NOT_FOUND);
            }
        }
    }

    public static class QueryViewCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> list, MonkeySourceNetwork.CommandQueue commandQueue) {
            AccessibilityNodeInfo nodeByViewId;
            String str;
            List<String> listSubList;
            if (list.size() > 2) {
                String str2 = list.get(1);
                if ("viewid".equals(str2)) {
                    try {
                        nodeByViewId = MonkeySourceNetworkViews.getNodeByViewId(list.get(2));
                        str = list.get(3);
                        listSubList = list.subList(4, list.size());
                    } catch (MonkeyViewException e) {
                        return new MonkeySourceNetwork.MonkeyCommandReturn(false, e.getMessage());
                    }
                } else if (str2.equals("accessibilityids")) {
                    try {
                        nodeByViewId = MonkeySourceNetworkViews.getNodeByAccessibilityIds(list.get(2), list.get(3));
                        str = list.get(4);
                        listSubList = list.subList(5, list.size());
                    } catch (NumberFormatException e2) {
                        return MonkeySourceNetwork.EARG;
                    }
                } else {
                    return MonkeySourceNetwork.EARG;
                }
                if (nodeByViewId != null) {
                    ViewIntrospectionCommand viewIntrospectionCommand = (ViewIntrospectionCommand) MonkeySourceNetworkViews.COMMAND_MAP.get(str);
                    if (viewIntrospectionCommand != null) {
                        return viewIntrospectionCommand.query(nodeByViewId, listSubList);
                    }
                    return MonkeySourceNetwork.EARG;
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.NO_NODE);
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetRootViewCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> list, MonkeySourceNetwork.CommandQueue commandQueue) {
            return new GetAccessibilityIds().query(MonkeySourceNetworkViews.sUiTestAutomationBridge.getRootInActiveWindow(), new ArrayList());
        }
    }

    public static class GetViewsWithTextCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> list, MonkeySourceNetwork.CommandQueue commandQueue) {
            if (list.size() == 2) {
                String str = list.get(1);
                List listFindAccessibilityNodeInfosByText = AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByText(MonkeySourceNetworkViews.sUiTestAutomationBridge.getConnectionId(), Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, str);
                GetAccessibilityIds getAccessibilityIds = new GetAccessibilityIds();
                ArrayList arrayList = new ArrayList();
                StringBuilder sb = new StringBuilder();
                Iterator it = listFindAccessibilityNodeInfosByText.iterator();
                while (it.hasNext()) {
                    MonkeySourceNetwork.MonkeyCommandReturn monkeyCommandReturnQuery = getAccessibilityIds.query((AccessibilityNodeInfo) it.next(), arrayList);
                    if (!monkeyCommandReturnQuery.wasSuccessful()) {
                        return monkeyCommandReturnQuery;
                    }
                    sb.append(monkeyCommandReturnQuery.getMessage());
                    sb.append(" ");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, sb.toString());
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetLocation implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                Rect rect = new Rect();
                accessibilityNodeInfo.getBoundsInScreen(rect);
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, rect.left + " " + rect.top + " " + (rect.right - rect.left) + " " + (rect.bottom - rect.top));
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetText implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                if (accessibilityNodeInfo.isPassword()) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(false, "Node contains a password");
                }
                if (accessibilityNodeInfo.getText() == null) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(true, "");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, accessibilityNodeInfo.getText().toString());
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetClass implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, accessibilityNodeInfo.getClassName().toString());
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetChecked implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(accessibilityNodeInfo.isChecked()));
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetEnabled implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(accessibilityNodeInfo.isEnabled()));
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetSelected implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(accessibilityNodeInfo.isSelected()));
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class SetSelected implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            boolean zPerformAction;
            if (list.size() == 1) {
                if (Boolean.valueOf(list.get(0)).booleanValue()) {
                    zPerformAction = accessibilityNodeInfo.performAction(4);
                } else if (!Boolean.valueOf(list.get(0)).booleanValue()) {
                    zPerformAction = accessibilityNodeInfo.performAction(8);
                } else {
                    return MonkeySourceNetwork.EARG;
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(zPerformAction);
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetFocused implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(accessibilityNodeInfo.isFocused()));
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class SetFocused implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            boolean zPerformAction;
            if (list.size() == 1) {
                if (Boolean.valueOf(list.get(0)).booleanValue()) {
                    zPerformAction = accessibilityNodeInfo.performAction(1);
                } else if (!Boolean.valueOf(list.get(0)).booleanValue()) {
                    zPerformAction = accessibilityNodeInfo.performAction(2);
                } else {
                    return MonkeySourceNetwork.EARG;
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(zPerformAction);
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetAccessibilityIds implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                try {
                    Field declaredField = accessibilityNodeInfo.getClass().getDeclaredField("mAccessibilityViewId");
                    declaredField.setAccessible(true);
                    return new MonkeySourceNetwork.MonkeyCommandReturn(true, accessibilityNodeInfo.getWindowId() + " " + ((Integer) declaredField.get(accessibilityNodeInfo)).intValue());
                } catch (IllegalAccessException e) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(false, "Access exception");
                } catch (NoSuchFieldException e2) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.NO_NODE);
                }
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetParent implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                AccessibilityNodeInfo parent = accessibilityNodeInfo.getParent();
                if (parent == null) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(false, "Given node has no parent");
                }
                return new GetAccessibilityIds().query(parent, new ArrayList());
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetChildren implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo accessibilityNodeInfo, List<String> list) {
            if (list.size() == 0) {
                GetAccessibilityIds getAccessibilityIds = new GetAccessibilityIds();
                ArrayList arrayList = new ArrayList();
                StringBuilder sb = new StringBuilder();
                int childCount = accessibilityNodeInfo.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    MonkeySourceNetwork.MonkeyCommandReturn monkeyCommandReturnQuery = getAccessibilityIds.query(accessibilityNodeInfo.getChild(i), arrayList);
                    if (!monkeyCommandReturnQuery.wasSuccessful()) {
                        return monkeyCommandReturnQuery;
                    }
                    sb.append(monkeyCommandReturnQuery.getMessage());
                    sb.append(" ");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, sb.toString());
            }
            return MonkeySourceNetwork.EARG;
        }
    }
}
