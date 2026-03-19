package android.view.inputmethod;

import android.os.Bundle;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class InputConnectionInspector {
    private static final Map<Class, Integer> sMissingMethodsMap = Collections.synchronizedMap(new WeakHashMap());

    @Retention(RetentionPolicy.SOURCE)
    public @interface MissingMethodFlags {
        public static final int CLOSE_CONNECTION = 64;
        public static final int COMMIT_CONTENT = 128;
        public static final int COMMIT_CORRECTION = 4;
        public static final int DELETE_SURROUNDING_TEXT_IN_CODE_POINTS = 16;
        public static final int GET_HANDLER = 32;
        public static final int GET_SELECTED_TEXT = 1;
        public static final int REQUEST_CURSOR_UPDATES = 8;
        public static final int SET_COMPOSING_REGION = 2;
    }

    public static int getMissingMethodFlags(InputConnection inputConnection) {
        if (inputConnection == null || (inputConnection instanceof BaseInputConnection)) {
            return 0;
        }
        if (inputConnection instanceof InputConnectionWrapper) {
            return ((InputConnectionWrapper) inputConnection).getMissingMethodFlags();
        }
        return getMissingMethodFlagsInternal(inputConnection.getClass());
    }

    public static int getMissingMethodFlagsInternal(Class cls) {
        Integer num = sMissingMethodsMap.get(cls);
        if (num != null) {
            return num.intValue();
        }
        int i = 0;
        if (!hasGetSelectedText(cls)) {
            i = 1;
        }
        if (!hasSetComposingRegion(cls)) {
            i |= 2;
        }
        if (!hasCommitCorrection(cls)) {
            i |= 4;
        }
        if (!hasRequestCursorUpdate(cls)) {
            i |= 8;
        }
        if (!hasDeleteSurroundingTextInCodePoints(cls)) {
            i |= 16;
        }
        if (!hasGetHandler(cls)) {
            i |= 32;
        }
        if (!hasCloseConnection(cls)) {
            i |= 64;
        }
        if (!hasCommitContent(cls)) {
            i |= 128;
        }
        sMissingMethodsMap.put(cls, Integer.valueOf(i));
        return i;
    }

    private static boolean hasGetSelectedText(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("getSelectedText", Integer.TYPE).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasSetComposingRegion(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("setComposingRegion", Integer.TYPE, Integer.TYPE).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasCommitCorrection(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("commitCorrection", CorrectionInfo.class).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasRequestCursorUpdate(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("requestCursorUpdates", Integer.TYPE).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasDeleteSurroundingTextInCodePoints(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("deleteSurroundingTextInCodePoints", Integer.TYPE, Integer.TYPE).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasGetHandler(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("getHandler", new Class[0]).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasCloseConnection(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("closeConnection", new Class[0]).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean hasCommitContent(Class cls) {
        try {
            return !Modifier.isAbstract(cls.getMethod("commitContent", InputContentInfo.class, Integer.TYPE, Bundle.class).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static String getMissingMethodFlagsAsString(int i) {
        boolean z;
        StringBuilder sb = new StringBuilder();
        if ((i & 1) != 0) {
            sb.append("getSelectedText(int)");
            z = false;
        } else {
            z = true;
        }
        if ((i & 2) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("setComposingRegion(int, int)");
            z = false;
        }
        if ((i & 4) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("commitCorrection(CorrectionInfo)");
            z = false;
        }
        if ((i & 8) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("requestCursorUpdate(int)");
            z = false;
        }
        if ((i & 16) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("deleteSurroundingTextInCodePoints(int, int)");
            z = false;
        }
        if ((i & 32) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("getHandler()");
        }
        if ((i & 64) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("closeConnection()");
        }
        if ((i & 128) != 0) {
            if (!z) {
                sb.append(",");
            }
            sb.append("commitContent(InputContentInfo, Bundle)");
        }
        return sb.toString();
    }
}
