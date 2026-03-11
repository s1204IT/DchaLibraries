package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class DreamBackend {
    private static final String TAG = DreamSettings.class.getSimpleName() + ".Backend";
    private final Context mContext;
    private final boolean mDreamsActivatedOnDockByDefault;
    private final boolean mDreamsActivatedOnSleepByDefault;
    private final boolean mDreamsEnabledByDefault;
    private final IDreamManager mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
    private final DreamInfoComparator mComparator = new DreamInfoComparator(getDefaultDream());

    public static class DreamInfo {
        CharSequence caption;
        public ComponentName componentName;
        Drawable icon;
        boolean isActive;
        public ComponentName settingsComponentName;

        public String toString() {
            StringBuilder sb = new StringBuilder(DreamInfo.class.getSimpleName());
            sb.append('[').append(this.caption);
            if (this.isActive) {
                sb.append(",active");
            }
            sb.append(',').append(this.componentName);
            if (this.settingsComponentName != null) {
                sb.append("settings=").append(this.settingsComponentName);
            }
            return sb.append(']').toString();
        }
    }

    public DreamBackend(Context context) {
        this.mContext = context;
        this.mDreamsEnabledByDefault = context.getResources().getBoolean(android.R.^attr-private.glowDot);
        this.mDreamsActivatedOnSleepByDefault = context.getResources().getBoolean(android.R.^attr-private.glyphMap);
        this.mDreamsActivatedOnDockByDefault = context.getResources().getBoolean(android.R.^attr-private.glyphDrawable);
    }

    public List<DreamInfo> getDreamInfos() {
        logd("getDreamInfos()", new Object[0]);
        ComponentName activeDream = getActiveDream();
        PackageManager pm = this.mContext.getPackageManager();
        Intent dreamIntent = new Intent("android.service.dreams.DreamService");
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(dreamIntent, 128);
        List<DreamInfo> dreamInfos = new ArrayList<>(resolveInfos.size());
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo != null) {
                DreamInfo dreamInfo = new DreamInfo();
                dreamInfo.caption = resolveInfo.loadLabel(pm);
                dreamInfo.icon = resolveInfo.loadIcon(pm);
                dreamInfo.componentName = getDreamComponentName(resolveInfo);
                dreamInfo.isActive = dreamInfo.componentName.equals(activeDream);
                dreamInfo.settingsComponentName = getSettingsComponentName(pm, resolveInfo);
                dreamInfos.add(dreamInfo);
            }
        }
        Collections.sort(dreamInfos, this.mComparator);
        return dreamInfos;
    }

    public ComponentName getDefaultDream() {
        if (this.mDreamManager == null) {
            return null;
        }
        try {
            return this.mDreamManager.getDefaultDreamComponent();
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get default dream", e);
            return null;
        }
    }

    public CharSequence getActiveDreamName() {
        ComponentName cn = getActiveDream();
        if (cn == null) {
            return null;
        }
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ServiceInfo ri = pm.getServiceInfo(cn, 0);
            if (ri != null) {
                return ri.loadLabel(pm);
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return getBoolean("screensaver_enabled", this.mDreamsEnabledByDefault);
    }

    public void setEnabled(boolean value) {
        logd("setEnabled(%s)", Boolean.valueOf(value));
        setBoolean("screensaver_enabled", value);
    }

    public boolean isActivatedOnDock() {
        return getBoolean("screensaver_activate_on_dock", this.mDreamsActivatedOnDockByDefault);
    }

    public void setActivatedOnDock(boolean value) {
        logd("setActivatedOnDock(%s)", Boolean.valueOf(value));
        setBoolean("screensaver_activate_on_dock", value);
    }

    public boolean isActivatedOnSleep() {
        return getBoolean("screensaver_activate_on_sleep", this.mDreamsActivatedOnSleepByDefault);
    }

    public void setActivatedOnSleep(boolean value) {
        logd("setActivatedOnSleep(%s)", Boolean.valueOf(value));
        setBoolean("screensaver_activate_on_sleep", value);
    }

    private boolean getBoolean(String key, boolean def) {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), key, def ? 1 : 0) == 1;
    }

    private void setBoolean(String key, boolean value) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), key, value ? 1 : 0);
    }

    public void setActiveDream(ComponentName dream) {
        logd("setActiveDream(%s)", dream);
        if (this.mDreamManager != null) {
            try {
                ComponentName[] dreams = {dream};
                IDreamManager iDreamManager = this.mDreamManager;
                if (dream == null) {
                    dreams = null;
                }
                iDreamManager.setDreamComponents(dreams);
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to set active dream to " + dream, e);
            }
        }
    }

    public ComponentName getActiveDream() {
        if (this.mDreamManager == null) {
            return null;
        }
        try {
            ComponentName[] dreams = this.mDreamManager.getDreamComponents();
            if (dreams == null || dreams.length <= 0) {
                return null;
            }
            return dreams[0];
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get active dream", e);
            return null;
        }
    }

    public void launchSettings(DreamInfo dreamInfo) {
        logd("launchSettings(%s)", dreamInfo);
        if (dreamInfo != null && dreamInfo.settingsComponentName != null) {
            this.mContext.startActivity(new Intent().setComponent(dreamInfo.settingsComponentName));
        }
    }

    public void startDreaming() {
        logd("startDreaming()", new Object[0]);
        if (this.mDreamManager != null) {
            try {
                this.mDreamManager.dream();
            } catch (RemoteException e) {
                Log.w(TAG, "Failed to dream", e);
            }
        }
    }

    private static ComponentName getDreamComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private static ComponentName getSettingsComponentName(PackageManager pm, ResolveInfo resolveInfo) {
        XmlResourceParser parser;
        int type;
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        String cn = null;
        XmlResourceParser parser2 = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, "android.service.dream");
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
            if (0 != 0) {
                parser2.close();
            }
        } catch (IOException e2) {
            caughtException = e2;
            if (0 != 0) {
                parser2.close();
            }
        } catch (XmlPullParserException e3) {
            caughtException = e3;
            if (0 != 0) {
                parser2.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                parser2.close();
            }
            throw th;
        }
        if (parser == null) {
            Log.w(TAG, "No android.service.dream meta-data");
            if (parser == null) {
                return null;
            }
            parser.close();
            return null;
        }
        Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        do {
            type = parser.next();
            if (type == 1) {
                break;
            }
        } while (type != 2);
        String nodeName = parser.getName();
        if (!"dream".equals(nodeName)) {
            Log.w(TAG, "Meta-data does not start with dream tag");
            if (parser == null) {
                return null;
            }
            parser.close();
            return null;
        }
        TypedArray sa = res.obtainAttributes(attrs, com.android.internal.R.styleable.Dream);
        cn = sa.getString(0);
        sa.recycle();
        if (parser != null) {
            parser.close();
        }
        if (caughtException != null) {
            Log.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName, caughtException);
            return null;
        }
        if (cn != null && cn.indexOf(47) < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        if (cn != null) {
            return ComponentName.unflattenFromString(cn);
        }
        return null;
    }

    private static void logd(String msg, Object... args) {
    }

    private static class DreamInfoComparator implements Comparator<DreamInfo> {
        private final ComponentName mDefaultDream;

        public DreamInfoComparator(ComponentName defaultDream) {
            this.mDefaultDream = defaultDream;
        }

        @Override
        public int compare(DreamInfo lhs, DreamInfo rhs) {
            return sortKey(lhs).compareTo(sortKey(rhs));
        }

        private String sortKey(DreamInfo di) {
            StringBuilder sb = new StringBuilder();
            sb.append(di.componentName.equals(this.mDefaultDream) ? '0' : '1');
            sb.append(di.caption);
            return sb.toString();
        }
    }
}
