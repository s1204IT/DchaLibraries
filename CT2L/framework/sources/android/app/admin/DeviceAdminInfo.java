package android.app.admin;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
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
    final ResolveInfo mReceiver;
    int mUsesPolicies;
    boolean mVisible;
    static ArrayList<PolicyInfo> sPoliciesDisplayOrder = new ArrayList<>();
    static HashMap<String, Integer> sKnownPolicies = new HashMap<>();
    static SparseArray<PolicyInfo> sRevKnownPolicies = new SparseArray<>();

    public static class PolicyInfo {
        public final int description;
        public final int ident;
        public final int label;
        public final String tag;

        public PolicyInfo(int identIn, String tagIn, int labelIn, int descriptionIn) {
            this.ident = identIn;
            this.tag = tagIn;
            this.label = labelIn;
            this.description = descriptionIn;
        }
    }

    static {
        sPoliciesDisplayOrder.add(new PolicyInfo(4, "wipe-data", R.string.policylab_wipeData, R.string.policydesc_wipeData));
        sPoliciesDisplayOrder.add(new PolicyInfo(2, "reset-password", R.string.policylab_resetPassword, R.string.policydesc_resetPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(0, "limit-password", R.string.policylab_limitPassword, R.string.policydesc_limitPassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(1, "watch-login", R.string.policylab_watchLogin, R.string.policydesc_watchLogin));
        sPoliciesDisplayOrder.add(new PolicyInfo(3, "force-lock", R.string.policylab_forceLock, R.string.policydesc_forceLock));
        sPoliciesDisplayOrder.add(new PolicyInfo(5, "set-global-proxy", R.string.policylab_setGlobalProxy, R.string.policydesc_setGlobalProxy));
        sPoliciesDisplayOrder.add(new PolicyInfo(6, "expire-password", R.string.policylab_expirePassword, R.string.policydesc_expirePassword));
        sPoliciesDisplayOrder.add(new PolicyInfo(7, "encrypted-storage", R.string.policylab_encryptedStorage, R.string.policydesc_encryptedStorage));
        sPoliciesDisplayOrder.add(new PolicyInfo(8, "disable-camera", R.string.policylab_disableCamera, R.string.policydesc_disableCamera));
        sPoliciesDisplayOrder.add(new PolicyInfo(9, "disable-keyguard-features", R.string.policylab_disableKeyguardFeatures, R.string.policydesc_disableKeyguardFeatures));
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo pi = sPoliciesDisplayOrder.get(i);
            sRevKnownPolicies.put(pi.ident, pi);
            sKnownPolicies.put(pi.tag, Integer.valueOf(pi.ident));
        }
        CREATOR = new Parcelable.Creator<DeviceAdminInfo>() {
            @Override
            public DeviceAdminInfo createFromParcel(Parcel source) {
                return new DeviceAdminInfo(source);
            }

            @Override
            public DeviceAdminInfo[] newArray(int size) {
                return new DeviceAdminInfo[size];
            }
        };
    }

    public DeviceAdminInfo(Context context, ResolveInfo receiver) throws XmlPullParserException, IOException {
        int type;
        this.mReceiver = receiver;
        ActivityInfo ai = receiver.activityInfo;
        PackageManager pm = context.getPackageManager();
        XmlResourceParser parser = null;
        try {
            try {
                parser = ai.loadXmlMetaData(pm, DeviceAdminReceiver.DEVICE_ADMIN_META_DATA);
                if (parser == null) {
                    throw new XmlPullParserException("No android.app.device_admin meta-data");
                }
                Resources res = pm.getResourcesForApplication(ai.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"device-admin".equals(nodeName)) {
                    throw new XmlPullParserException("Meta-data does not start with device-admin tag");
                }
                TypedArray sa = res.obtainAttributes(attrs, R.styleable.DeviceAdmin);
                this.mVisible = sa.getBoolean(0, true);
                sa.recycle();
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        String tagName = parser.getName();
                        if (tagName.equals("uses-policies")) {
                            int innerDepth = parser.getDepth();
                            while (true) {
                                int type3 = parser.next();
                                if (type3 == 1 || (type3 == 3 && parser.getDepth() <= innerDepth)) {
                                    break;
                                }
                                if (type3 != 3 && type3 != 4) {
                                    String policyName = parser.getName();
                                    Integer val = sKnownPolicies.get(policyName);
                                    if (val != null) {
                                        this.mUsesPolicies |= 1 << val.intValue();
                                    } else {
                                        Log.w(TAG, "Unknown tag under uses-policies of " + getComponent() + ": " + policyName);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                throw new XmlPullParserException("Unable to create context for: " + ai.packageName);
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    DeviceAdminInfo(Parcel source) {
        this.mReceiver = ResolveInfo.CREATOR.createFromParcel(source);
        this.mUsesPolicies = source.readInt();
    }

    public String getPackageName() {
        return this.mReceiver.activityInfo.packageName;
    }

    public String getReceiverName() {
        return this.mReceiver.activityInfo.name;
    }

    public ActivityInfo getActivityInfo() {
        return this.mReceiver.activityInfo;
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mReceiver.activityInfo.packageName, this.mReceiver.activityInfo.name);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mReceiver.loadLabel(pm);
    }

    public CharSequence loadDescription(PackageManager pm) throws Resources.NotFoundException {
        if (this.mReceiver.activityInfo.descriptionRes != 0) {
            String packageName = this.mReceiver.resolvePackageName;
            ApplicationInfo applicationInfo = null;
            if (packageName == null) {
                packageName = this.mReceiver.activityInfo.packageName;
                applicationInfo = this.mReceiver.activityInfo.applicationInfo;
            }
            return pm.getText(packageName, this.mReceiver.activityInfo.descriptionRes, applicationInfo);
        }
        throw new Resources.NotFoundException();
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mReceiver.loadIcon(pm);
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    public boolean usesPolicy(int policyIdent) {
        return (this.mUsesPolicies & (1 << policyIdent)) != 0;
    }

    public String getTagForPolicy(int policyIdent) {
        return sRevKnownPolicies.get(policyIdent).tag;
    }

    public ArrayList<PolicyInfo> getUsedPolicies() {
        ArrayList<PolicyInfo> res = new ArrayList<>();
        for (int i = 0; i < sPoliciesDisplayOrder.size(); i++) {
            PolicyInfo pi = sPoliciesDisplayOrder.get(i);
            if (usesPolicy(pi.ident)) {
                res.add(pi);
            }
        }
        return res;
    }

    public void writePoliciesToXml(XmlSerializer out) throws IllegalStateException, IOException, IllegalArgumentException {
        out.attribute(null, "flags", Integer.toString(this.mUsesPolicies));
    }

    public void readPoliciesFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        this.mUsesPolicies = Integer.parseInt(parser.getAttributeValue(null, "flags"));
    }

    public void dump(Printer pw, String prefix) {
        pw.println(prefix + "Receiver:");
        this.mReceiver.dump(pw, prefix + "  ");
    }

    public String toString() {
        return "DeviceAdminInfo{" + this.mReceiver.activityInfo.name + "}";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mReceiver.writeToParcel(dest, flags);
        dest.writeInt(this.mUsesPolicies);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
