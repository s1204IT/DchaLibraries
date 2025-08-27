package com.android.systemui.tuner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Icon;
import android.util.AttributeSet;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class ShortcutParser {
    private AttributeSet mAttrs;
    private final Context mContext;
    private final String mName;
    private final String mPkg;
    private final int mResId;
    private Resources mResources;

    public ShortcutParser(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException {
        this(context, componentName.getPackageName(), componentName.getClassName(), getResId(context, componentName));
    }

    private static int getResId(Context context, ComponentName componentName) throws PackageManager.NameNotFoundException {
        ActivityInfo activityInfo = context.getPackageManager().getActivityInfo(componentName, 128);
        if (activityInfo.metaData != null && activityInfo.metaData.containsKey("android.app.shortcuts")) {
            return activityInfo.metaData.getInt("android.app.shortcuts");
        }
        return 0;
    }

    public ShortcutParser(Context context, String str, String str2, int i) {
        this.mContext = context;
        this.mPkg = str;
        this.mResId = i;
        this.mName = str2;
    }

    public List<Shortcut> getShortcuts() throws Resources.NotFoundException {
        Shortcut shortcut;
        ArrayList arrayList = new ArrayList();
        if (this.mResId != 0) {
            try {
                this.mResources = this.mContext.getPackageManager().getResourcesForApplication(this.mPkg);
                XmlResourceParser xml = this.mResources.getXml(this.mResId);
                this.mAttrs = Xml.asAttributeSet(xml);
                while (true) {
                    int next = xml.next();
                    if (next == 1) {
                        break;
                    }
                    if (next == 2 && xml.getName().equals("shortcut") && (shortcut = parseShortcut(xml)) != null) {
                        arrayList.add(shortcut);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return arrayList;
    }

    private Shortcut parseShortcut(XmlResourceParser xmlResourceParser) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = this.mResources.obtainAttributes(this.mAttrs, R.styleable.Shortcut);
        Shortcut shortcut = new Shortcut();
        if (!typedArrayObtainAttributes.getBoolean(1, true)) {
            return null;
        }
        String string = typedArrayObtainAttributes.getString(2);
        int resourceId = typedArrayObtainAttributes.getResourceId(0, 0);
        int resourceId2 = typedArrayObtainAttributes.getResourceId(3, 0);
        shortcut.pkg = this.mPkg;
        shortcut.icon = Icon.createWithResource(this.mPkg, resourceId);
        shortcut.id = string;
        shortcut.label = this.mResources.getString(resourceId2);
        shortcut.name = this.mName;
        while (true) {
            int next = xmlResourceParser.next();
            if (next == 3) {
                break;
            }
            if (next == 2 && xmlResourceParser.getName().equals("intent")) {
                shortcut.intent = Intent.parseIntent(this.mResources, xmlResourceParser, this.mAttrs);
            }
        }
        if (shortcut.intent != null) {
            return shortcut;
        }
        return null;
    }

    public static class Shortcut {
        public Icon icon;
        public String id;
        public Intent intent;
        public String label;
        public String name;
        public String pkg;

        public static Shortcut create(Context context, String str) {
            String[] strArrSplit = str.split("::");
            try {
                for (Shortcut shortcut : new ShortcutParser(context, new ComponentName(strArrSplit[0], strArrSplit[1])).getShortcuts()) {
                    if (shortcut.id.equals(strArrSplit[2])) {
                        return shortcut;
                    }
                }
                return null;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        public String toString() {
            return this.pkg + "::" + this.name + "::" + this.id;
        }
    }
}
