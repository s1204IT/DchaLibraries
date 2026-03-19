package android.service.autofill;

import android.Manifest;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.metrics.LogMaker;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class AutofillServiceInfo {
    private static final String TAG = "AutofillServiceInfo";
    private static final String TAG_AUTOFILL_SERVICE = "autofill-service";
    private static final String TAG_COMPATIBILITY_PACKAGE = "compatibility-package";
    private final ArrayMap<String, Long> mCompatibilityPackages;
    private final ServiceInfo mServiceInfo;
    private final String mSettingsActivity;

    private static ServiceInfo getServiceInfoOrThrow(ComponentName componentName, int i) throws PackageManager.NameNotFoundException {
        try {
            ServiceInfo serviceInfo = AppGlobals.getPackageManager().getServiceInfo(componentName, 128, i);
            if (serviceInfo != null) {
                return serviceInfo;
            }
        } catch (RemoteException e) {
        }
        throw new PackageManager.NameNotFoundException(componentName.toString());
    }

    public AutofillServiceInfo(Context context, ComponentName componentName, int i) throws PackageManager.NameNotFoundException {
        this(context, getServiceInfoOrThrow(componentName, i));
    }

    public AutofillServiceInfo(Context context, ServiceInfo serviceInfo) throws Throwable {
        String string;
        TypedArray typedArrayObtainAttributes;
        if (!Manifest.permission.BIND_AUTOFILL_SERVICE.equals(serviceInfo.permission)) {
            if (Manifest.permission.BIND_AUTOFILL.equals(serviceInfo.permission)) {
                Log.w(TAG, "AutofillService from '" + serviceInfo.packageName + "' uses unsupported permission " + Manifest.permission.BIND_AUTOFILL + ". It works for now, but might not be supported on future releases");
                new MetricsLogger().write(new LogMaker(MetricsProto.MetricsEvent.AUTOFILL_INVALID_PERMISSION).setPackageName(serviceInfo.packageName));
            } else {
                Log.w(TAG, "AutofillService from '" + serviceInfo.packageName + "' does not require permission " + Manifest.permission.BIND_AUTOFILL_SERVICE);
                throw new SecurityException("Service does not require permission android.permission.BIND_AUTOFILL_SERVICE");
            }
        }
        this.mServiceInfo = serviceInfo;
        XmlResourceParser xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(context.getPackageManager(), AutofillService.SERVICE_META_DATA);
        ArrayMap<String, Long> compatibilityPackages = null;
        if (xmlResourceParserLoadXmlMetaData == null) {
            this.mSettingsActivity = null;
            this.mCompatibilityPackages = null;
            return;
        }
        try {
            Resources resourcesForApplication = context.getPackageManager().getResourcesForApplication(serviceInfo.applicationInfo);
            for (int next = 0; next != 1 && next != 2; next = xmlResourceParserLoadXmlMetaData.next()) {
            }
            if (TAG_AUTOFILL_SERVICE.equals(xmlResourceParserLoadXmlMetaData.getName())) {
                try {
                    typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData), R.styleable.AutofillService);
                    try {
                        string = typedArrayObtainAttributes.getString(0);
                        if (typedArrayObtainAttributes != null) {
                            try {
                                typedArrayObtainAttributes.recycle();
                            } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e) {
                                e = e;
                                Log.e(TAG, "Error parsing auto fill service meta-data", e);
                            }
                        }
                        compatibilityPackages = parseCompatibilityPackages(xmlResourceParserLoadXmlMetaData, resourcesForApplication);
                    } catch (Throwable th) {
                        th = th;
                        if (typedArrayObtainAttributes != null) {
                            typedArrayObtainAttributes.recycle();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    typedArrayObtainAttributes = null;
                }
            } else {
                Log.e(TAG, "Meta-data does not start with autofill-service tag");
                string = null;
            }
        } catch (PackageManager.NameNotFoundException | IOException | XmlPullParserException e2) {
            e = e2;
            string = null;
        }
        this.mSettingsActivity = string;
        this.mCompatibilityPackages = compatibilityPackages;
    }

    private ArrayMap<String, Long> parseCompatibilityPackages(XmlPullParser xmlPullParser, Resources resources) throws Throwable {
        TypedArray typedArrayObtainAttributes;
        Long lValueOf;
        int depth = xmlPullParser.getDepth();
        ArrayMap<String, Long> arrayMap = null;
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4 && TAG_COMPATIBILITY_PACKAGE.equals(xmlPullParser.getName())) {
                try {
                    typedArrayObtainAttributes = resources.obtainAttributes(Xml.asAttributeSet(xmlPullParser), R.styleable.AutofillService_CompatibilityPackage);
                } catch (Throwable th) {
                    th = th;
                    typedArrayObtainAttributes = null;
                }
                try {
                    String string = typedArrayObtainAttributes.getString(0);
                    if (TextUtils.isEmpty(string)) {
                        Log.e(TAG, "Invalid compatibility package:" + string);
                        XmlUtils.skipCurrentTag(xmlPullParser);
                        if (typedArrayObtainAttributes != null) {
                        }
                    } else {
                        String string2 = typedArrayObtainAttributes.getString(1);
                        if (string2 != null) {
                            try {
                                lValueOf = Long.valueOf(Long.parseLong(string2));
                                if (lValueOf.longValue() < 0) {
                                    Log.e(TAG, "Invalid compatibility max version code:" + lValueOf);
                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                    if (typedArrayObtainAttributes != null) {
                                    }
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid compatibility max version code:" + string2);
                                XmlUtils.skipCurrentTag(xmlPullParser);
                                if (typedArrayObtainAttributes != null) {
                                    typedArrayObtainAttributes.recycle();
                                }
                            }
                        } else {
                            lValueOf = Long.MAX_VALUE;
                        }
                        if (arrayMap == null) {
                            arrayMap = new ArrayMap<>();
                        }
                        arrayMap.put(string, lValueOf);
                        XmlUtils.skipCurrentTag(xmlPullParser);
                        if (typedArrayObtainAttributes != null) {
                            typedArrayObtainAttributes.recycle();
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    XmlUtils.skipCurrentTag(xmlPullParser);
                    if (typedArrayObtainAttributes != null) {
                        typedArrayObtainAttributes.recycle();
                    }
                    throw th;
                }
            }
        }
        typedArrayObtainAttributes.recycle();
        return arrayMap;
    }

    public ServiceInfo getServiceInfo() {
        return this.mServiceInfo;
    }

    public String getSettingsActivity() {
        return this.mSettingsActivity;
    }

    public ArrayMap<String, Long> getCompatibilityPackages() {
        return this.mCompatibilityPackages;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("[");
        sb.append(this.mServiceInfo);
        sb.append(", settings:");
        sb.append(this.mSettingsActivity);
        sb.append(", hasCompatPckgs:");
        sb.append((this.mCompatibilityPackages == null || this.mCompatibilityPackages.isEmpty()) ? false : true);
        sb.append("]");
        return sb.toString();
    }

    public void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("Component: ");
        printWriter.println(getServiceInfo().getComponentName());
        printWriter.print(str);
        printWriter.print("Settings: ");
        printWriter.println(this.mSettingsActivity);
        printWriter.print(str);
        printWriter.print("Compat packages: ");
        printWriter.println(this.mCompatibilityPackages);
    }
}
