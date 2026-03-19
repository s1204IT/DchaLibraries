package com.android.server.policy;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.UserHandle;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParserException;

final class GlobalKeyManager {
    private static final String ATTR_COMPONENT = "component";
    private static final String ATTR_KEY_CODE = "keyCode";
    private static final String ATTR_VERSION = "version";
    private static final int GLOBAL_KEY_FILE_VERSION = 1;
    private static final String TAG = "GlobalKeyManager";
    private static final String TAG_GLOBAL_KEYS = "global_keys";
    private static final String TAG_KEY = "key";
    private SparseArray<ComponentName> mKeyMapping = new SparseArray<>();

    public GlobalKeyManager(Context context) throws Throwable {
        loadGlobalKeys(context);
    }

    boolean handleGlobalKey(Context context, int i, KeyEvent keyEvent) {
        ComponentName componentName;
        if (this.mKeyMapping.size() > 0 && (componentName = this.mKeyMapping.get(i)) != null) {
            context.sendBroadcastAsUser(new Intent("android.intent.action.GLOBAL_BUTTON").setComponent(componentName).setFlags(268435456).putExtra("android.intent.extra.KEY_EVENT", keyEvent), UserHandle.CURRENT, null);
            return true;
        }
        return false;
    }

    boolean shouldHandleGlobalKey(int i, KeyEvent keyEvent) {
        return this.mKeyMapping.get(i) != null;
    }

    private void loadGlobalKeys(Context context) throws Throwable {
        Throwable th;
        XmlPullParserException e;
        XmlResourceParser xml;
        IOException e2;
        Resources.NotFoundException e3;
        try {
            try {
                xml = context.getResources().getXml(R.xml.default_zen_mode_config);
                try {
                    XmlUtils.beginDocument(xml, TAG_GLOBAL_KEYS);
                    if (1 == xml.getAttributeIntValue(null, ATTR_VERSION, 0)) {
                        while (true) {
                            XmlUtils.nextElement(xml);
                            String name = xml.getName();
                            if (name == null) {
                                break;
                            }
                            if (TAG_KEY.equals(name)) {
                                String attributeValue = xml.getAttributeValue(null, ATTR_KEY_CODE);
                                String attributeValue2 = xml.getAttributeValue(null, ATTR_COMPONENT);
                                int iKeyCodeFromString = KeyEvent.keyCodeFromString(attributeValue);
                                if (iKeyCodeFromString != 0) {
                                    this.mKeyMapping.put(iKeyCodeFromString, ComponentName.unflattenFromString(attributeValue2));
                                }
                            }
                        }
                    }
                    if (xml == null) {
                        return;
                    }
                } catch (Resources.NotFoundException e4) {
                    e3 = e4;
                    Log.w(TAG, "global keys file not found", e3);
                    if (xml == null) {
                        return;
                    }
                } catch (IOException e5) {
                    e2 = e5;
                    Log.w(TAG, "I/O exception reading global keys file", e2);
                    if (xml == null) {
                        return;
                    }
                } catch (XmlPullParserException e6) {
                    e = e6;
                    Log.w(TAG, "XML parser exception reading global keys file", e);
                    if (xml == null) {
                        return;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                if (context != 0) {
                    context.close();
                }
                throw th;
            }
        } catch (Resources.NotFoundException e7) {
            e3 = e7;
            xml = null;
        } catch (IOException e8) {
            e2 = e8;
            xml = null;
        } catch (XmlPullParserException e9) {
            e = e9;
            xml = null;
        } catch (Throwable th3) {
            th = th3;
            context = 0;
            if (context != 0) {
            }
            throw th;
        }
        xml.close();
    }

    public void dump(String str, PrintWriter printWriter) {
        int size = this.mKeyMapping.size();
        if (size == 0) {
            printWriter.print(str);
            printWriter.println("mKeyMapping.size=0");
            return;
        }
        printWriter.print(str);
        printWriter.println("mKeyMapping={");
        for (int i = 0; i < size; i++) {
            printWriter.print("  ");
            printWriter.print(str);
            printWriter.print(KeyEvent.keyCodeToString(this.mKeyMapping.keyAt(i)));
            printWriter.print("=");
            printWriter.println(this.mKeyMapping.valueAt(i).flattenToString());
        }
        printWriter.print(str);
        printWriter.println("}");
    }
}
