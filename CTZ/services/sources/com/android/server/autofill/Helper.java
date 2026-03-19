package com.android.server.autofill;

import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.metrics.LogMaker;
import android.service.autofill.Dataset;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.view.WindowManager;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import com.android.internal.util.ArrayUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;

public final class Helper {
    private static final String TAG = "AutofillHelper";
    public static boolean sDebug = false;
    public static boolean sVerbose = false;
    static int sPartitionMaxCount = 10;
    public static int sVisibleDatasetsMaxCount = 3;
    public static Boolean sFullScreenMode = null;

    private interface ViewNodeFilter {
        boolean matches(AssistStructure.ViewNode viewNode);
    }

    private Helper() {
        throw new UnsupportedOperationException("contains static members only");
    }

    static AutofillId[] toArray(ArraySet<AutofillId> arraySet) {
        if (arraySet == null) {
            return null;
        }
        AutofillId[] autofillIdArr = new AutofillId[arraySet.size()];
        for (int i = 0; i < arraySet.size(); i++) {
            autofillIdArr[i] = arraySet.valueAt(i);
        }
        return autofillIdArr;
    }

    public static String paramsToString(WindowManager.LayoutParams layoutParams) {
        StringBuilder sb = new StringBuilder(25);
        layoutParams.dumpDimensions(sb);
        return sb.toString();
    }

    static ArrayMap<AutofillId, AutofillValue> getFields(Dataset dataset) {
        int size;
        ArrayList fieldIds = dataset.getFieldIds();
        ArrayList fieldValues = dataset.getFieldValues();
        if (fieldIds != null) {
            size = fieldIds.size();
        } else {
            size = 0;
        }
        ArrayMap<AutofillId, AutofillValue> arrayMap = new ArrayMap<>(size);
        for (int i = 0; i < size; i++) {
            arrayMap.put((AutofillId) fieldIds.get(i), (AutofillValue) fieldValues.get(i));
        }
        return arrayMap;
    }

    private static LogMaker newLogMaker(int i, String str, int i2, boolean z) {
        LogMaker logMakerAddTaggedData = new LogMaker(i).addTaggedData(908, str).addTaggedData(1456, Integer.toString(i2));
        if (z) {
            logMakerAddTaggedData.addTaggedData(1414, 1);
        }
        return logMakerAddTaggedData;
    }

    public static LogMaker newLogMaker(int i, String str, String str2, int i2, boolean z) {
        return newLogMaker(i, str2, i2, z).setPackageName(str);
    }

    public static LogMaker newLogMaker(int i, ComponentName componentName, String str, int i2, boolean z) {
        return newLogMaker(i, str, i2, z).setComponentName(componentName);
    }

    public static void printlnRedactedText(PrintWriter printWriter, CharSequence charSequence) {
        if (charSequence == null) {
            printWriter.println("null");
        } else {
            printWriter.print(charSequence.length());
            printWriter.println("_chars");
        }
    }

    public static AssistStructure.ViewNode findViewNodeByAutofillId(AssistStructure assistStructure, final AutofillId autofillId) {
        return findViewNode(assistStructure, new ViewNodeFilter() {
            @Override
            public final boolean matches(AssistStructure.ViewNode viewNode) {
                return autofillId.equals(viewNode.getAutofillId());
            }
        });
    }

    private static AssistStructure.ViewNode findViewNode(AssistStructure assistStructure, ViewNodeFilter viewNodeFilter) {
        LinkedList linkedList = new LinkedList();
        int windowNodeCount = assistStructure.getWindowNodeCount();
        for (int i = 0; i < windowNodeCount; i++) {
            linkedList.add(assistStructure.getWindowNodeAt(i).getRootViewNode());
        }
        while (!linkedList.isEmpty()) {
            AssistStructure.ViewNode viewNode = (AssistStructure.ViewNode) linkedList.removeFirst();
            if (viewNodeFilter.matches(viewNode)) {
                return viewNode;
            }
            for (int i2 = 0; i2 < viewNode.getChildCount(); i2++) {
                linkedList.addLast(viewNode.getChildAt(i2));
            }
        }
        return null;
    }

    public static AssistStructure.ViewNode sanitizeUrlBar(AssistStructure assistStructure, final String[] strArr) {
        AssistStructure.ViewNode viewNodeFindViewNode = findViewNode(assistStructure, new ViewNodeFilter() {
            @Override
            public final boolean matches(AssistStructure.ViewNode viewNode) {
                return ArrayUtils.contains(strArr, viewNode.getIdEntry());
            }
        });
        if (viewNodeFindViewNode != null) {
            String string = viewNodeFindViewNode.getText().toString();
            if (string.isEmpty()) {
                if (sDebug) {
                    Slog.d(TAG, "sanitizeUrlBar(): empty on " + viewNodeFindViewNode.getIdEntry());
                    return null;
                }
                return null;
            }
            viewNodeFindViewNode.setWebDomain(string);
            if (sDebug) {
                Slog.d(TAG, "sanitizeUrlBar(): id=" + viewNodeFindViewNode.getIdEntry() + ", domain=" + viewNodeFindViewNode.getWebDomain());
            }
        }
        return viewNodeFindViewNode;
    }

    static int getNumericValue(LogMaker logMaker, int i) {
        Object taggedData = logMaker.getTaggedData(i);
        if (!(taggedData instanceof Number)) {
            return 0;
        }
        return ((Number) taggedData).intValue();
    }
}
