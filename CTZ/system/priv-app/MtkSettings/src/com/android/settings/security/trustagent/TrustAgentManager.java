package com.android.settings.security.trustagent;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.widget.LockPatternUtils;
import com.android.settingslib.RestrictedLockUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class TrustAgentManager {
    static final String PERMISSION_PROVIDE_AGENT = "android.permission.PROVIDE_TRUST_AGENT";
    private static final Intent TRUST_AGENT_INTENT = new Intent("android.service.trust.TrustAgentService");

    public static class TrustAgentComponentInfo {
        public RestrictedLockUtils.EnforcedAdmin admin = null;
        public ComponentName componentName;
        public String summary;
        public String title;
    }

    public boolean shouldProvideTrust(ResolveInfo resolveInfo, PackageManager packageManager) {
        String str = resolveInfo.serviceInfo.packageName;
        if (packageManager.checkPermission(PERMISSION_PROVIDE_AGENT, str) != 0) {
            Log.w("TrustAgentManager", "Skipping agent because package " + str + " does not have permission " + PERMISSION_PROVIDE_AGENT + ".");
            return false;
        }
        return true;
    }

    public CharSequence getActiveTrustAgentLabel(Context context, LockPatternUtils lockPatternUtils) throws Throwable {
        List<TrustAgentComponentInfo> activeTrustAgents = getActiveTrustAgents(context, lockPatternUtils);
        if (activeTrustAgents.isEmpty()) {
            return null;
        }
        return activeTrustAgents.get(0).title;
    }

    public List<TrustAgentComponentInfo> getActiveTrustAgents(Context context, LockPatternUtils lockPatternUtils) throws Throwable {
        int iMyUserId = UserHandle.myUserId();
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        PackageManager packageManager = context.getPackageManager();
        ArrayList arrayList = new ArrayList();
        List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(TRUST_AGENT_INTENT, 128);
        List enabledTrustAgents = lockPatternUtils.getEnabledTrustAgents(iMyUserId);
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfKeyguardFeaturesDisabled = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(context, 16, iMyUserId);
        if (enabledTrustAgents != null && !enabledTrustAgents.isEmpty()) {
            Iterator<ResolveInfo> it = listQueryIntentServices.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ResolveInfo next = it.next();
                if (next.serviceInfo != null && shouldProvideTrust(next, packageManager)) {
                    TrustAgentComponentInfo settingsComponent = getSettingsComponent(packageManager, next);
                    if (settingsComponent.componentName != null && enabledTrustAgents.contains(getComponentName(next)) && !TextUtils.isEmpty(settingsComponent.title)) {
                        if (enforcedAdminCheckIfKeyguardFeaturesDisabled != null && devicePolicyManager.getTrustAgentConfiguration(null, getComponentName(next)) == null) {
                            settingsComponent.admin = enforcedAdminCheckIfKeyguardFeaturesDisabled;
                        }
                        arrayList.add(settingsComponent);
                    }
                }
            }
        }
        return arrayList;
    }

    public ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        }
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [191=8] */
    /* JADX WARN: Removed duplicated region for block: B:55:0x00a5 A[PHI: r2 r3 r9
  0x00a5: PHI (r2v7 android.content.res.XmlResourceParser) = 
  (r2v5 android.content.res.XmlResourceParser)
  (r2v6 android.content.res.XmlResourceParser)
  (r2v8 android.content.res.XmlResourceParser)
 binds: [B:54:0x00a3, B:59:0x00ad, B:64:0x00b4] A[DONT_GENERATE, DONT_INLINE]
  0x00a5: PHI (r3v5 java.lang.String) = (r3v3 java.lang.String), (r3v4 java.lang.String), (r3v6 java.lang.String) binds: [B:54:0x00a3, B:59:0x00ad, B:64:0x00b4] A[DONT_GENERATE, DONT_INLINE]
  0x00a5: PHI (r9v8 'e' java.lang.Throwable) = (r9v6 'e' java.lang.Throwable), (r9v7 'e' java.lang.Throwable), (r9v9 'e' java.lang.Throwable) binds: [B:54:0x00a3, B:59:0x00ad, B:64:0x00b4] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Removed duplicated region for block: B:67:0x00b9  */
    /* JADX WARN: Removed duplicated region for block: B:69:0x00d4  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private TrustAgentComponentInfo getSettingsComponent(PackageManager packageManager, ResolveInfo resolveInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        String string;
        int next;
        if (resolveInfo == null || resolveInfo.serviceInfo == null || resolveInfo.serviceInfo.metaData == null) {
            return null;
        }
        TrustAgentComponentInfo trustAgentComponentInfo = new TrustAgentComponentInfo();
        try {
            xmlResourceParserLoadXmlMetaData = resolveInfo.serviceInfo.loadXmlMetaData(packageManager, "android.service.trust.trustagent");
        } catch (PackageManager.NameNotFoundException e) {
            e = e;
            xmlResourceParserLoadXmlMetaData = null;
            string = null;
        } catch (IOException e2) {
            e = e2;
            xmlResourceParserLoadXmlMetaData = null;
            string = null;
        } catch (XmlPullParserException e3) {
            e = e3;
            xmlResourceParserLoadXmlMetaData = null;
            string = null;
        } catch (Throwable th) {
            th = th;
            xmlResourceParserLoadXmlMetaData = null;
        }
        try {
            try {
            } catch (PackageManager.NameNotFoundException e4) {
                e = e4;
                string = null;
            } catch (IOException e5) {
                e = e5;
                string = null;
            } catch (XmlPullParserException e6) {
                e = e6;
                string = null;
            }
            if (xmlResourceParserLoadXmlMetaData == null) {
                Slog.w("TrustAgentManager", "Can't find android.service.trust.trustagent meta-data");
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                return null;
            }
            Resources resourcesForApplication = packageManager.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
            do {
                next = xmlResourceParserLoadXmlMetaData.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            if (!"trust-agent".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                Slog.w("TrustAgentManager", "Meta-data does not start with trust-agent tag");
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                return null;
            }
            TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.TrustAgent);
            trustAgentComponentInfo.summary = typedArrayObtainAttributes.getString(1);
            trustAgentComponentInfo.title = typedArrayObtainAttributes.getString(0);
            string = typedArrayObtainAttributes.getString(2);
            try {
                typedArrayObtainAttributes.recycle();
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                e = null;
            } catch (PackageManager.NameNotFoundException e7) {
                e = e7;
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                if (e == null) {
                }
            } catch (IOException e8) {
                e = e8;
                if (xmlResourceParserLoadXmlMetaData != null) {
                }
                if (e == null) {
                }
            } catch (XmlPullParserException e9) {
                e = e9;
                if (xmlResourceParserLoadXmlMetaData != null) {
                }
                if (e == null) {
                }
            }
            if (e == null) {
                Slog.w("TrustAgentManager", "Error parsing : " + resolveInfo.serviceInfo.packageName, e);
                return null;
            }
            if (string != null && string.indexOf(47) < 0) {
                string = resolveInfo.serviceInfo.packageName + "/" + string;
            }
            trustAgentComponentInfo.componentName = string != null ? ComponentName.unflattenFromString(string) : null;
            return trustAgentComponentInfo;
        } catch (Throwable th2) {
            th = th2;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            throw th;
        }
    }
}
