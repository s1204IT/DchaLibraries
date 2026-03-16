package com.android.commands.monkey;

import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.graphics.Rect;
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

    private static Class<?> getIdClass(String packageName, String sourceDir) throws ClassNotFoundException {
        Class<?> klass = sClassMap.get(packageName);
        if (klass == null) {
            DexClassLoader classLoader = new DexClassLoader(sourceDir, "/data/local/tmp", null, ClassLoader.getSystemClassLoader());
            Class<?> klass2 = classLoader.loadClass(packageName + ".R$id");
            sClassMap.put(packageName, klass2);
            return klass2;
        }
        return klass;
    }

    private static AccessibilityNodeInfo getNodeByAccessibilityIds(String windowString, String viewString) {
        int windowId = Integer.parseInt(windowString);
        int viewId = Integer.parseInt(viewString);
        int connectionId = sUiTestAutomationBridge.getConnectionId();
        AccessibilityInteractionClient client = AccessibilityInteractionClient.getInstance();
        return client.findAccessibilityNodeInfoByAccessibilityId(connectionId, windowId, viewId, false, 0);
    }

    private static AccessibilityNodeInfo getNodeByViewId(String viewId) throws MonkeyViewException {
        int connectionId = sUiTestAutomationBridge.getConnectionId();
        AccessibilityInteractionClient client = AccessibilityInteractionClient.getInstance();
        List<AccessibilityNodeInfo> infos = client.findAccessibilityNodeInfosByViewId(connectionId, Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, viewId);
        if (infos.isEmpty()) {
            return null;
        }
        return infos.get(0);
    }

    public static class ListViewsCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> command, MonkeySourceNetwork.CommandQueue queue) {
            AccessibilityNodeInfo node = MonkeySourceNetworkViews.sUiTestAutomationBridge.getRootInActiveWindow();
            if (node == null) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.NO_ACCESSIBILITY_EVENT);
            }
            String packageName = node.getPackageName().toString();
            try {
                ApplicationInfo appInfo = MonkeySourceNetworkViews.sPm.getApplicationInfo(packageName, 0, UserHandle.myUserId());
                Class<?> klass = MonkeySourceNetworkViews.getIdClass(packageName, appInfo.sourceDir);
                StringBuilder fieldBuilder = new StringBuilder();
                Field[] fields = klass.getFields();
                for (Field field : fields) {
                    fieldBuilder.append(field.getName() + " ");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, fieldBuilder.toString());
            } catch (RemoteException e) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.REMOTE_ERROR);
            } catch (ClassNotFoundException e2) {
                return new MonkeySourceNetwork.MonkeyCommandReturn(false, MonkeySourceNetworkViews.CLASS_NOT_FOUND);
            }
        }
    }

    public static class QueryViewCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> command, MonkeySourceNetwork.CommandQueue queue) {
            AccessibilityNodeInfo node;
            String viewQuery;
            List<String> args;
            if (command.size() > 2) {
                String idType = command.get(1);
                if ("viewid".equals(idType)) {
                    try {
                        node = MonkeySourceNetworkViews.getNodeByViewId(command.get(2));
                        viewQuery = command.get(3);
                        args = command.subList(4, command.size());
                    } catch (MonkeyViewException e) {
                        return new MonkeySourceNetwork.MonkeyCommandReturn(false, e.getMessage());
                    }
                } else if (idType.equals("accessibilityids")) {
                    try {
                        node = MonkeySourceNetworkViews.getNodeByAccessibilityIds(command.get(2), command.get(3));
                        viewQuery = command.get(4);
                        args = command.subList(5, command.size());
                    } catch (NumberFormatException e2) {
                        return MonkeySourceNetwork.EARG;
                    }
                } else {
                    return MonkeySourceNetwork.EARG;
                }
                if (node != null) {
                    ViewIntrospectionCommand getter = (ViewIntrospectionCommand) MonkeySourceNetworkViews.COMMAND_MAP.get(viewQuery);
                    if (getter != null) {
                        return getter.query(node, args);
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
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> command, MonkeySourceNetwork.CommandQueue queue) {
            AccessibilityNodeInfo node = MonkeySourceNetworkViews.sUiTestAutomationBridge.getRootInActiveWindow();
            return new GetAccessibilityIds().query(node, new ArrayList());
        }
    }

    public static class GetViewsWithTextCommand implements MonkeySourceNetwork.MonkeyCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn translateCommand(List<String> command, MonkeySourceNetwork.CommandQueue queue) {
            if (command.size() == 2) {
                String text = command.get(1);
                int connectionId = MonkeySourceNetworkViews.sUiTestAutomationBridge.getConnectionId();
                List<AccessibilityNodeInfo> nodes = AccessibilityInteractionClient.getInstance().findAccessibilityNodeInfosByText(connectionId, Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, text);
                ViewIntrospectionCommand idGetter = new GetAccessibilityIds();
                List<String> emptyArgs = new ArrayList<>();
                StringBuilder ids = new StringBuilder();
                for (AccessibilityNodeInfo node : nodes) {
                    MonkeySourceNetwork.MonkeyCommandReturn result = idGetter.query(node, emptyArgs);
                    if (result.wasSuccessful()) {
                        ids.append(result.getMessage()).append(" ");
                    } else {
                        return result;
                    }
                }
                MonkeySourceNetwork.MonkeyCommandReturn result2 = new MonkeySourceNetwork.MonkeyCommandReturn(true, ids.toString());
                return result2;
            }
            MonkeySourceNetwork.MonkeyCommandReturn result3 = MonkeySourceNetwork.EARG;
            return result3;
        }
    }

    public static class GetLocation implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            if (args.size() != 0) {
                return MonkeySourceNetwork.EARG;
            }
            Rect nodePosition = new Rect();
            node.getBoundsInScreen(nodePosition);
            StringBuilder positions = new StringBuilder();
            positions.append(nodePosition.left).append(" ").append(nodePosition.top);
            positions.append(" ").append(nodePosition.right - nodePosition.left).append(" ");
            positions.append(nodePosition.bottom - nodePosition.top);
            return new MonkeySourceNetwork.MonkeyCommandReturn(true, positions.toString());
        }
    }

    public static class GetText implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            if (args.size() == 0) {
                if (node.isPassword()) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(false, "Node contains a password");
                }
                if (node.getText() == null) {
                    return new MonkeySourceNetwork.MonkeyCommandReturn(true, "");
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(true, node.getText().toString());
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetClass implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            return args.size() == 0 ? new MonkeySourceNetwork.MonkeyCommandReturn(true, node.getClassName().toString()) : MonkeySourceNetwork.EARG;
        }
    }

    public static class GetChecked implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            return args.size() == 0 ? new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(node.isChecked())) : MonkeySourceNetwork.EARG;
        }
    }

    public static class GetEnabled implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            return args.size() == 0 ? new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(node.isEnabled())) : MonkeySourceNetwork.EARG;
        }
    }

    public static class GetSelected implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            return args.size() == 0 ? new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(node.isSelected())) : MonkeySourceNetwork.EARG;
        }
    }

    public static class SetSelected implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            boolean actionPerformed;
            if (args.size() == 1) {
                if (Boolean.valueOf(args.get(0)).booleanValue()) {
                    actionPerformed = node.performAction(4);
                } else if (!Boolean.valueOf(args.get(0)).booleanValue()) {
                    actionPerformed = node.performAction(8);
                } else {
                    return MonkeySourceNetwork.EARG;
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(actionPerformed);
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetFocused implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            return args.size() == 0 ? new MonkeySourceNetwork.MonkeyCommandReturn(true, Boolean.toString(node.isFocused())) : MonkeySourceNetwork.EARG;
        }
    }

    public static class SetFocused implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            boolean actionPerformed;
            if (args.size() == 1) {
                if (Boolean.valueOf(args.get(0)).booleanValue()) {
                    actionPerformed = node.performAction(1);
                } else if (!Boolean.valueOf(args.get(0)).booleanValue()) {
                    actionPerformed = node.performAction(2);
                } else {
                    return MonkeySourceNetwork.EARG;
                }
                return new MonkeySourceNetwork.MonkeyCommandReturn(actionPerformed);
            }
            return MonkeySourceNetwork.EARG;
        }
    }

    public static class GetAccessibilityIds implements ViewIntrospectionCommand {
        @Override
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            if (args.size() == 0) {
                try {
                    Class<?> klass = node.getClass();
                    Field field = klass.getDeclaredField("mAccessibilityViewId");
                    field.setAccessible(true);
                    int viewId = ((Integer) field.get(node)).intValue();
                    String ids = node.getWindowId() + " " + viewId;
                    return new MonkeySourceNetwork.MonkeyCommandReturn(true, ids);
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
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            if (args.size() == 0) {
                AccessibilityNodeInfo parent = node.getParent();
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
        public MonkeySourceNetwork.MonkeyCommandReturn query(AccessibilityNodeInfo node, List<String> args) {
            if (args.size() == 0) {
                ViewIntrospectionCommand idGetter = new GetAccessibilityIds();
                List<String> emptyArgs = new ArrayList<>();
                StringBuilder ids = new StringBuilder();
                int totalChildren = node.getChildCount();
                for (int i = 0; i < totalChildren; i++) {
                    MonkeySourceNetwork.MonkeyCommandReturn result = idGetter.query(node.getChild(i), emptyArgs);
                    if (result.wasSuccessful()) {
                        ids.append(result.getMessage()).append(" ");
                    } else {
                        return result;
                    }
                }
                MonkeySourceNetwork.MonkeyCommandReturn result2 = new MonkeySourceNetwork.MonkeyCommandReturn(true, ids.toString());
                return result2;
            }
            MonkeySourceNetwork.MonkeyCommandReturn result3 = MonkeySourceNetwork.EARG;
            return result3;
        }
    }
}
