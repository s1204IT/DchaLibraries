package com.android.uiautomator.core;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TableLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import org.xmlpull.v1.XmlSerializer;

public class AccessibilityNodeInfoDumper {
    private static final String LOGTAG = AccessibilityNodeInfoDumper.class.getSimpleName();
    private static final String[] NAF_EXCLUDED_CLASSES = {GridView.class.getName(), GridLayout.class.getName(), ListView.class.getName(), TableLayout.class.getName()};

    public static void dumpWindowToFile(AccessibilityNodeInfo accessibilityNodeInfo, int i, int i2, int i3) {
        File file = new File(Environment.getDataDirectory(), "local");
        if (!file.exists()) {
            file.mkdir();
            file.setExecutable(true, false);
            file.setWritable(true, false);
            file.setReadable(true, false);
        }
        dumpWindowToFile(accessibilityNodeInfo, new File(new File(Environment.getDataDirectory(), "local"), "window_dump.xml"), i, i2, i3);
    }

    public static void dumpWindowToFile(AccessibilityNodeInfo accessibilityNodeInfo, File file, int i, int i2, int i3) {
        if (accessibilityNodeInfo == null) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        try {
            FileWriter fileWriter = new FileWriter(file);
            XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
            StringWriter stringWriter = new StringWriter();
            xmlSerializerNewSerializer.setOutput(stringWriter);
            xmlSerializerNewSerializer.startDocument("UTF-8", true);
            xmlSerializerNewSerializer.startTag("", "hierarchy");
            xmlSerializerNewSerializer.attribute("", "rotation", Integer.toString(i));
            dumpNodeRec(accessibilityNodeInfo, xmlSerializerNewSerializer, 0, i2, i3);
            xmlSerializerNewSerializer.endTag("", "hierarchy");
            xmlSerializerNewSerializer.endDocument();
            fileWriter.write(stringWriter.toString());
            fileWriter.close();
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to dump window to file", e);
        }
        long jUptimeMillis2 = SystemClock.uptimeMillis();
        Log.w(LOGTAG, "Fetch time: " + (jUptimeMillis2 - jUptimeMillis) + "ms");
    }

    private static void dumpNodeRec(AccessibilityNodeInfo accessibilityNodeInfo, XmlSerializer xmlSerializer, int i, int i2, int i3) throws IOException {
        xmlSerializer.startTag("", "node");
        if (!nafExcludedClass(accessibilityNodeInfo) && !nafCheck(accessibilityNodeInfo)) {
            xmlSerializer.attribute("", "NAF", Boolean.toString(true));
        }
        xmlSerializer.attribute("", "index", Integer.toString(i));
        xmlSerializer.attribute("", "text", safeCharSeqToString(accessibilityNodeInfo.getText()));
        xmlSerializer.attribute("", "resource-id", safeCharSeqToString(accessibilityNodeInfo.getViewIdResourceName()));
        xmlSerializer.attribute("", "class", safeCharSeqToString(accessibilityNodeInfo.getClassName()));
        xmlSerializer.attribute("", "package", safeCharSeqToString(accessibilityNodeInfo.getPackageName()));
        xmlSerializer.attribute("", "content-desc", safeCharSeqToString(accessibilityNodeInfo.getContentDescription()));
        xmlSerializer.attribute("", "checkable", Boolean.toString(accessibilityNodeInfo.isCheckable()));
        xmlSerializer.attribute("", "checked", Boolean.toString(accessibilityNodeInfo.isChecked()));
        xmlSerializer.attribute("", "clickable", Boolean.toString(accessibilityNodeInfo.isClickable()));
        xmlSerializer.attribute("", "enabled", Boolean.toString(accessibilityNodeInfo.isEnabled()));
        xmlSerializer.attribute("", "focusable", Boolean.toString(accessibilityNodeInfo.isFocusable()));
        xmlSerializer.attribute("", "focused", Boolean.toString(accessibilityNodeInfo.isFocused()));
        xmlSerializer.attribute("", "scrollable", Boolean.toString(accessibilityNodeInfo.isScrollable()));
        xmlSerializer.attribute("", "long-clickable", Boolean.toString(accessibilityNodeInfo.isLongClickable()));
        xmlSerializer.attribute("", "password", Boolean.toString(accessibilityNodeInfo.isPassword()));
        xmlSerializer.attribute("", "selected", Boolean.toString(accessibilityNodeInfo.isSelected()));
        xmlSerializer.attribute("", "bounds", AccessibilityNodeInfoHelper.getVisibleBoundsInScreen(accessibilityNodeInfo, i2, i3).toShortString());
        int childCount = accessibilityNodeInfo.getChildCount();
        for (int i4 = 0; i4 < childCount; i4++) {
            AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i4);
            if (child != null) {
                if (child.isVisibleToUser()) {
                    dumpNodeRec(child, xmlSerializer, i4, i2, i3);
                    child.recycle();
                } else {
                    Log.i(LOGTAG, String.format("Skipping invisible child: %s", child.toString()));
                }
            } else {
                Log.i(LOGTAG, String.format("Null child %d/%d, parent: %s", Integer.valueOf(i4), Integer.valueOf(childCount), accessibilityNodeInfo.toString()));
            }
        }
        xmlSerializer.endTag("", "node");
    }

    private static boolean nafExcludedClass(AccessibilityNodeInfo accessibilityNodeInfo) {
        String strSafeCharSeqToString = safeCharSeqToString(accessibilityNodeInfo.getClassName());
        for (String str : NAF_EXCLUDED_CLASSES) {
            if (strSafeCharSeqToString.endsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private static boolean nafCheck(AccessibilityNodeInfo accessibilityNodeInfo) {
        boolean z;
        if (!accessibilityNodeInfo.isClickable() || !accessibilityNodeInfo.isEnabled() || !safeCharSeqToString(accessibilityNodeInfo.getContentDescription()).isEmpty() || !safeCharSeqToString(accessibilityNodeInfo.getText()).isEmpty()) {
            z = false;
        } else {
            z = true;
        }
        if (z) {
            return childNafCheck(accessibilityNodeInfo);
        }
        return true;
    }

    private static boolean childNafCheck(AccessibilityNodeInfo accessibilityNodeInfo) {
        int childCount = accessibilityNodeInfo.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = accessibilityNodeInfo.getChild(i);
            if (!safeCharSeqToString(child.getContentDescription()).isEmpty() || !safeCharSeqToString(child.getText()).isEmpty() || childNafCheck(child)) {
                return true;
            }
        }
        return false;
    }

    private static String safeCharSeqToString(CharSequence charSequence) {
        if (charSequence == null) {
            return "";
        }
        return stripInvalidXMLChars(charSequence);
    }

    private static String stripInvalidXMLChars(CharSequence charSequence) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < charSequence.length(); i++) {
            char cCharAt = charSequence.charAt(i);
            if ((cCharAt >= 1 && cCharAt <= '\b') || ((cCharAt >= 11 && cCharAt <= '\f') || ((cCharAt >= 14 && cCharAt <= 31) || ((cCharAt >= 127 && cCharAt <= 132) || ((cCharAt >= 134 && cCharAt <= 159) || ((cCharAt >= 64976 && cCharAt <= 64991) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || ((cCharAt >= 65534 && cCharAt <= 65535) || (cCharAt >= 65534 && cCharAt <= 65535)))))))))))))))))))))) {
                stringBuffer.append(".");
            } else {
                stringBuffer.append(cCharAt);
            }
        }
        return stringBuffer.toString();
    }
}
