package android.app.admin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Printer;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class DeviceAdminInfo implements Parcelable {
    public static final Parcelable.Creator<DeviceAdminInfo> CREATOR;
    static final String TAG = "DeviceAdminInfo";
    public static final int USES_ENCRYPTED_STORAGE = 7;
    public static final int USES_POLICY_DEVICE_OWNER = -2;
    public static final int USES_POLICY_DISABLE_CAMERA = 8;
    public static final int USES_POLICY_DISABLE_KEYGUARD_FEATURES = 9;
    public static final int USES_POLICY_EXPIRE_PASSWORD = 6;
    public static final int USES_POLICY_FORCE_LOCK = 3;
    public static final int USES_POLICY_LIMIT_PASSWORD = 0;
    public static final int USES_POLICY_PROFILE_OWNER = -1;
    public static final int USES_POLICY_RESET_PASSWORD = 2;
    public static final int USES_POLICY_SETS_GLOBAL_PROXY = 5;
    public static final int USES_POLICY_WATCH_LOGIN = 1;
    public static final int USES_POLICY_WIPE_DATA = 4;
    final ActivityInfo mActivityInfo;
    boolean mSupportsTransferOwnership;
    int mUsesPolicies;
    boolean mVisible;
    static ArrayList<PolicyInfo> sPoliciesDisplayOrder = new ArrayList<>();
    static HashMap<String, Integer> sKnownPolicies = new HashMap<>();
    static SparseArray<PolicyInfo> sRevKnownPolicies = new SparseArray<>();

    public static class PolicyInfo {
        public final int description;
        public final int descriptionForSecondaryUsers;
        public final int ident;
        public final int label;
        public final int labelForSecondaryUsers;
        public final String tag;

        public PolicyInfo(int i, String str, int i2, int i3) {
            this(i, str, i2, i3, i2, i3);
        }

        public PolicyInfo(int i, String str, int i2, int i3, int i4, int i5) {
            this.ident = i;
            this.tag = str;
            this.label = i2;
            this.description = i3;
            this.labelForSecondaryUsers = i4;
            this.descriptionForSecondaryUsers = i5;
        }
    }

    static {
        sPoliciesDisplayOrder.add(new PolicyInfo(4, "wipe-data", R.string.policylab_wipeData, R.string.policydesc_wipeData, R.string.policylab_wipeData_secondaryUser, R.string.policydesc_wipeData_secondaryUser));
        sPoliciesDisplayOrder.add(new PolicyInfo(2, "reset-password", R.string.policylab_resetPassword, R.string.policydesc_resetPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(0, "limit-password", R.string.policylab_limitPassword, R.string.policydesc_limitPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(1, "watch-login", R.string.policylab_watchLogin, R.string.policydesc_watchLogin, R.string.policylab_watchLogin, R.string.policydesc_watchLogin_secondaryUser));
        sPoliciesDisplayOrder.add(new PolicyInfo(3, "force-lock", R.string.policylab_forceLock, R.string.policydesc_forceLock));
        sPoliciesDisplayOrder.add(new PolicyInfo(5, "set-global-proxy", R.string.policylab_setGlobalProxy, R.string.policydesc_setGlobalProxy));
        sPoliciesDisplayOrder.add(new PolicyInfo(6, "expire-password", R.string.policylab_expirePassword, R.string.policydesc_expirePassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(7, "encrypted-storage", R.string.policylab_encryptedStorage, R.string.policydesc_encryptedStorage));
        sPoliciesDisplayOrder.add(new PolicyInfo(8, "disable-camera", R.string.policylab_disableCamera, R.string.policydesc_disableCamera));
        sPoliciesDisplayOrder.add(new PolicyInfo(9, "disable-keyguard-features", R.string.policylab_disableKeyguardFeatures, R.string.policydesc_disableKeyguardFeatures));
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo policyInfo = sPoliciesDisplayOrder.get(i);
            sRevKnownPolicies.put(policyInfo.ident, policyInfo);
            sKnownPolicies.put(policyInfo.tag, Integer.valueOf(policyInfo.ident));
        }
        CREATOR = new Parcelable.Creator<DeviceAdminInfo>() {
            @Override
            public DeviceAdminInfo createFromParcel(Parcel parcel) {
                return new DeviceAdminInfo(parcel);
            }

            @Override
            public DeviceAdminInfo[] newArray(int i2) {
                return new DeviceAdminInfo[i2];
            }
        };
    }

    public DeviceAdminInfo(Context context, ResolveInfo resolveInfo) throws XmlPullParserException, IOException {
        this(context, resolveInfo.activityInfo);
    }

    public DeviceAdminInfo(Context context, ActivityInfo activityInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        int next;
        this.mActivityInfo = activityInfo;
        PackageManager packageManager = context.getPackageManager();
        try {
            try {
                xmlResourceParserLoadXmlMetaData = this.mActivityInfo.loadXmlMetaData(packageManager, DeviceAdminReceiver.DEVICE_ADMIN_META_DATA);
            } catch (Throwable th) {
                th = th;
                xmlResourceParserLoadXmlMetaData = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        try {
            if (xmlResourceParserLoadXmlMetaData == null) {
                throw new XmlPullParserException("No android.app.device_admin meta-data");
            }
            Resources resourcesForApplication = packageManager.getResourcesForApplication(this.mActivityInfo.applicationInfo);
            AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
            do {
                next = xmlResourceParserLoadXmlMetaData.next();
                if (next == 1) {
                    break;
                }
            } while (next != 2);
            if (!"device-admin".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                throw new XmlPullParserException("Meta-data does not start with device-admin tag");
            }
            TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.DeviceAdmin);
            this.mVisible = typedArrayObtainAttributes.getBoolean(0, true);
            typedArrayObtainAttributes.recycle();
            int depth = xmlResourceParserLoadXmlMetaData.getDepth();
            while (true) {
                int next2 = xmlResourceParserLoadXmlMetaData.next();
                if (next2 == 1 || (next2 == 3 && xmlResourceParserLoadXmlMetaData.getDepth() <= depth)) {
                    break;
                }
                if (next2 != 3 && next2 != 4) {
                    String name = xmlResourceParserLoadXmlMetaData.getName();
                    if (name.equals("uses-policies")) {
                        int depth2 = xmlResourceParserLoadXmlMetaData.getDepth();
                        while (true) {
                            int next3 = xmlResourceParserLoadXmlMetaData.next();
                            if (next3 == 1 || (next3 == 3 && xmlResourceParserLoadXmlMetaData.getDepth() <= depth2)) {
                                break;
                            }
                            if (next3 != 3 && next3 != 4) {
                                String name2 = xmlResourceParserLoadXmlMetaData.getName();
                                Integer num = sKnownPolicies.get(name2);
                                if (num != null) {
                                    this.mUsesPolicies |= 1 << num.intValue();
                                } else {
                                    Log.w(TAG, "Unknown tag under uses-policies of " + getComponent() + ": " + name2);
                                }
                            }
                        }
                    } else if (!name.equals("support-transfer-ownership")) {
                        continue;
                    } else {
                        if (xmlResourceParserLoadXmlMetaData.next() != 3) {
                            throw new XmlPullParserException("support-transfer-ownership tag must be empty.");
                        }
                        this.mSupportsTransferOwnership = true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e2) {
            throw new XmlPullParserException("Unable to create context for: " + this.mActivityInfo.packageName);
        } catch (Throwable th2) {
            th = th2;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            throw th;
        }
    }

    DeviceAdminInfo(Parcel parcel) {
        this.mActivityInfo = ActivityInfo.CREATOR.createFromParcel(parcel);
        this.mUsesPolicies = parcel.readInt();
        this.mSupportsTransferOwnership = parcel.readBoolean();
    }

    public String getPackageName() {
        return this.mActivityInfo.packageName;
    }

    public String getReceiverName() {
        return this.mActivityInfo.name;
    }

    public ActivityInfo getActivityInfo() {
        return this.mActivityInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mActivityInfo.packageName, this.mActivityInfo.name);
    }

    public CharSequence loadLabel(PackageManager packageManager) {
        return this.mActivityInfo.loadLabel(packageManager);
    }

    public CharSequence loadDescription(PackageManager packageManager) throws Resources.NotFoundException {
        if (this.mActivityInfo.descriptionRes != 0) {
            return packageManager.getText(this.mActivityInfo.packageName, this.mActivityInfo.descriptionRes, this.mActivityInfo.applicationInfo);
        }
        throw new Resources.NotFoundException();
    }

    public Drawable loadIcon(PackageManager packageManager) {
        return this.mActivityInfo.loadIcon(packageManager);
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    public boolean usesPolicy(int i) {
        return ((1 << i) & this.mUsesPolicies) != 0;
    }

    public String getTagForPolicy(int i) {
        return sRevKnownPolicies.get(i).tag;
    }

    public boolean supportsTransferOwnership() {
        return this.mSupportsTransferOwnership;
    }

    public ArrayList<PolicyInfo> getUsedPolicies() {
        ArrayList<PolicyInfo> arrayList = new ArrayList<>();
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo policyInfo = sPoliciesDisplayOrder.get(i);
            if (usesPolicy(policyInfo.ident)) {
                arrayList.add(policyInfo);
            }
        }
        return arrayList;
    }

    public void writePoliciesToXml(XmlSerializer xmlSerializer) throws IllegalStateException, IOException, IllegalArgumentException {
        xmlSerializer.attribute(null, "flags", Integer.toString(this.mUsesPolicies));
    }

    public void readPoliciesFromXml(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        this.mUsesPolicies = Integer.parseInt(xmlPullParser.getAttributeValue(null, "flags"));
    }

    public void dump(Printer printer, String str) {
        printer.println(str + "Receiver:");
        this.mActivityInfo.dump(printer, str + "  ");
    }

    public String toString() {
        return "DeviceAdminInfo{" + this.mActivityInfo.name + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mActivityInfo.writeToParcel(parcel, i);
        parcel.writeInt(this.mUsesPolicies);
        parcel.writeBoolean(this.mSupportsTransferOwnership);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
