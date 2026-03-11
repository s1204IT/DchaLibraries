package com.android.quickstep;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;
import android.util.Xml;
import com.android.launcher3.R;
import com.android.launcher3.graphics.IconShapeOverride;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

@TargetApi(26)
public class LauncherSearchIndexablesProvider extends SearchIndexablesProvider {
    public boolean onCreate() {
        return true;
    }

    public Cursor queryXmlResources(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);
        matrixCursor.newRow().add("xmlResId", Integer.valueOf(R.xml.indexable_launcher_prefs)).add("intentAction", "android.intent.action.APPLICATION_PREFERENCES").add("intentTargetPackage", getContext().getPackageName()).add("intentTargetClass", getContext().getPackageManager().resolveActivity(new Intent("android.intent.action.APPLICATION_PREFERENCES").setPackage(getContext().getPackageName()), 0).activityInfo.name);
        return matrixCursor;
    }

    public Cursor queryRawData(String[] strArr) {
        return new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
    }

    public Cursor queryNonIndexableKeys(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        if (!((LauncherApps) getContext().getSystemService(LauncherApps.class)).hasShortcutHostPermission()) {
            try {
                XmlResourceParser xml = getContext().getResources().getXml(R.xml.indexable_launcher_prefs);
                Throwable th = null;
                try {
                    try {
                        int depth = xml.getDepth();
                        int[] iArr = {android.R.attr.key};
                        while (true) {
                            int next = xml.next();
                            if ((next == 3 && xml.getDepth() <= depth) || next == 1) {
                                break;
                            }
                            if (next == 2) {
                                TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(Xml.asAttributeSet(xml), iArr);
                                matrixCursor.addRow(new String[]{typedArrayObtainStyledAttributes.getString(0)});
                                typedArrayObtainStyledAttributes.recycle();
                            }
                        }
                        if (xml != null) {
                            xml.close();
                        }
                    } finally {
                    }
                } catch (Throwable th2) {
                    if (xml != null) {
                        if (th != null) {
                            try {
                                xml.close();
                            } catch (Throwable th3) {
                                th.addSuppressed(th3);
                            }
                        } else {
                            xml.close();
                        }
                    }
                    throw th2;
                }
            } catch (IOException | XmlPullParserException e) {
                throw new RuntimeException(e);
            }
        } else if (!IconShapeOverride.isSupported(getContext())) {
            matrixCursor.addRow(new String[]{IconShapeOverride.KEY_PREFERENCE});
        }
        return matrixCursor;
    }
}
