package android.nfc.cardemulation;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.NetworkCapabilities;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

public final class ApduServiceInfo implements Parcelable {
    public static final Parcelable.Creator<ApduServiceInfo> CREATOR = new Parcelable.Creator<ApduServiceInfo>() {
        @Override
        public ApduServiceInfo createFromParcel(Parcel source) {
            ResolveInfo info = ResolveInfo.CREATOR.createFromParcel(source);
            String description = source.readString();
            boolean onHost = source.readInt() != 0;
            ArrayList<AidGroup> staticAidGroups = new ArrayList<>();
            int numStaticGroups = source.readInt();
            if (numStaticGroups > 0) {
                source.readTypedList(staticAidGroups, AidGroup.CREATOR);
            }
            ArrayList<AidGroup> dynamicAidGroups = new ArrayList<>();
            int numDynamicGroups = source.readInt();
            if (numDynamicGroups > 0) {
                source.readTypedList(dynamicAidGroups, AidGroup.CREATOR);
            }
            boolean requiresUnlock = source.readInt() != 0;
            int bannerResource = source.readInt();
            int uid = source.readInt();
            String settingsActivityName = source.readString();
            return new ApduServiceInfo(info, onHost, description, staticAidGroups, dynamicAidGroups, requiresUnlock, bannerResource, uid, settingsActivityName);
        }

        @Override
        public ApduServiceInfo[] newArray(int size) {
            return new ApduServiceInfo[size];
        }
    };
    static final String TAG = "ApduServiceInfo";
    final int mBannerResourceId;
    final String mDescription;
    final HashMap<String, AidGroup> mDynamicAidGroups;
    final boolean mOnHost;
    final boolean mRequiresDeviceUnlock;
    final ResolveInfo mService;
    final String mSettingsActivityName;
    final HashMap<String, AidGroup> mStaticAidGroups;
    final int mUid;

    public ApduServiceInfo(ResolveInfo info, boolean onHost, String description, ArrayList<AidGroup> staticAidGroups, ArrayList<AidGroup> dynamicAidGroups, boolean requiresUnlock, int bannerResource, int uid, String settingsActivityName) {
        this.mService = info;
        this.mDescription = description;
        this.mStaticAidGroups = new HashMap<>();
        this.mDynamicAidGroups = new HashMap<>();
        this.mOnHost = onHost;
        this.mRequiresDeviceUnlock = requiresUnlock;
        for (AidGroup aidGroup : staticAidGroups) {
            this.mStaticAidGroups.put(aidGroup.category, aidGroup);
        }
        for (AidGroup aidGroup2 : dynamicAidGroups) {
            this.mDynamicAidGroups.put(aidGroup2.category, aidGroup2);
        }
        this.mBannerResourceId = bannerResource;
        this.mUid = uid;
        this.mSettingsActivityName = settingsActivityName;
    }

    public ApduServiceInfo(PackageManager pm, ResolveInfo info, boolean onHost) throws XmlPullParserException, IOException {
        XmlResourceParser parser;
        ServiceInfo si = info.serviceInfo;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                if (onHost) {
                    parser = si.loadXmlMetaData(pm, HostApduService.SERVICE_META_DATA);
                    if (parser == null) {
                        throw new XmlPullParserException("No android.nfc.cardemulation.host_apdu_service meta-data");
                    }
                } else {
                    parser = si.loadXmlMetaData(pm, OffHostApduService.SERVICE_META_DATA);
                    if (parser == null) {
                        throw new XmlPullParserException("No android.nfc.cardemulation.off_host_apdu_service meta-data");
                    }
                }
                for (int eventType = parser.getEventType(); eventType != 2 && eventType != 1; eventType = parser.next()) {
                }
                String tagName = parser.getName();
                if (onHost && !"host-apdu-service".equals(tagName)) {
                    throw new XmlPullParserException("Meta-data does not start with <host-apdu-service> tag");
                }
                if (!onHost && !"offhost-apdu-service".equals(tagName)) {
                    throw new XmlPullParserException("Meta-data does not start with <offhost-apdu-service> tag");
                }
                Resources res = pm.getResourcesForApplication(si.applicationInfo);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                if (onHost) {
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.HostApduService);
                    this.mService = info;
                    this.mDescription = sa.getString(0);
                    this.mRequiresDeviceUnlock = sa.getBoolean(2, false);
                    this.mBannerResourceId = sa.getResourceId(3, -1);
                    this.mSettingsActivityName = sa.getString(1);
                    sa.recycle();
                } else {
                    TypedArray sa2 = res.obtainAttributes(attrs, R.styleable.OffHostApduService);
                    this.mService = info;
                    this.mDescription = sa2.getString(0);
                    this.mRequiresDeviceUnlock = false;
                    this.mBannerResourceId = sa2.getResourceId(2, -1);
                    this.mSettingsActivityName = sa2.getString(1);
                    sa2.recycle();
                }
                this.mStaticAidGroups = new HashMap<>();
                this.mDynamicAidGroups = new HashMap<>();
                this.mOnHost = onHost;
                int depth = parser.getDepth();
                AidGroup currentGroup = null;
                while (true) {
                    int eventType2 = parser.next();
                    if ((eventType2 == 3 && parser.getDepth() <= depth) || eventType2 == 1) {
                        break;
                    }
                    String tagName2 = parser.getName();
                    if (eventType2 == 2 && "aid-group".equals(tagName2) && currentGroup == null) {
                        TypedArray groupAttrs = res.obtainAttributes(attrs, R.styleable.AidGroup);
                        String groupCategory = groupAttrs.getString(1);
                        String groupDescription = groupAttrs.getString(0);
                        groupCategory = CardEmulation.CATEGORY_PAYMENT.equals(groupCategory) ? groupCategory : CardEmulation.CATEGORY_OTHER;
                        currentGroup = this.mStaticAidGroups.get(groupCategory);
                        if (currentGroup != null) {
                            if (!CardEmulation.CATEGORY_OTHER.equals(groupCategory)) {
                                Log.e(TAG, "Not allowing multiple aid-groups in the " + groupCategory + " category");
                                currentGroup = null;
                            }
                        } else {
                            currentGroup = new AidGroup(groupCategory, groupDescription);
                        }
                        groupAttrs.recycle();
                    } else if (eventType2 == 3 && "aid-group".equals(tagName2) && currentGroup != null) {
                        if (currentGroup.aids.size() > 0) {
                            if (!this.mStaticAidGroups.containsKey(currentGroup.category)) {
                                this.mStaticAidGroups.put(currentGroup.category, currentGroup);
                            }
                        } else {
                            Log.e(TAG, "Not adding <aid-group> with empty or invalid AIDs");
                        }
                        currentGroup = null;
                    } else if (eventType2 == 2 && "aid-filter".equals(tagName2) && currentGroup != null) {
                        TypedArray a = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        String aid = a.getString(0).toUpperCase();
                        if (CardEmulation.isValidAid(aid) && !currentGroup.aids.contains(aid)) {
                            currentGroup.aids.add(aid);
                        } else {
                            Log.e(TAG, "Ignoring invalid or duplicate aid: " + aid);
                        }
                        a.recycle();
                    } else if (eventType2 == 2 && "aid-prefix-filter".equals(tagName2) && currentGroup != null) {
                        TypedArray a2 = res.obtainAttributes(attrs, R.styleable.AidFilter);
                        String aid2 = a2.getString(0).toUpperCase().concat(NetworkCapabilities.MATCH_ALL_REQUESTS_NETWORK_SPECIFIER);
                        if (CardEmulation.isValidAid(aid2) && !currentGroup.aids.contains(aid2)) {
                            currentGroup.aids.add(aid2);
                        } else {
                            Log.e(TAG, "Ignoring invalid or duplicate aid: " + aid2);
                        }
                        a2.recycle();
                    }
                }
                if (parser != null) {
                    parser.close();
                }
                this.mUid = si.applicationInfo.uid;
            } catch (PackageManager.NameNotFoundException e) {
                throw new XmlPullParserException("Unable to create context for: " + si.packageName);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                xmlResourceParser.close();
            }
            throw th;
        }
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public List<String> getAids() {
        ArrayList<String> aids = new ArrayList<>();
        for (AidGroup group : getAidGroups()) {
            aids.addAll(group.aids);
        }
        return aids;
    }

    public List<String> getPrefixAids() {
        ArrayList<String> prefixAids = new ArrayList<>();
        for (AidGroup group : getAidGroups()) {
            for (String aid : group.aids) {
                if (aid.endsWith(NetworkCapabilities.MATCH_ALL_REQUESTS_NETWORK_SPECIFIER)) {
                    prefixAids.add(aid);
                }
            }
        }
        return prefixAids;
    }

    public AidGroup getDynamicAidGroupForCategory(String category) {
        return this.mDynamicAidGroups.get(category);
    }

    public boolean removeDynamicAidGroupForCategory(String category) {
        return this.mDynamicAidGroups.remove(category) != null;
    }

    public ArrayList<AidGroup> getAidGroups() {
        ArrayList<AidGroup> groups = new ArrayList<>();
        Iterator entry$iterator = this.mDynamicAidGroups.entrySet().iterator();
        while (entry$iterator.hasNext()) {
            groups.add(((Map.Entry) entry$iterator.next()).getValue());
        }
        for (Map.Entry<String, AidGroup> entry : this.mStaticAidGroups.entrySet()) {
            if (!this.mDynamicAidGroups.containsKey(entry.getKey())) {
                groups.add(entry.getValue());
            }
        }
        return groups;
    }

    public String getCategoryForAid(String aid) {
        ArrayList<AidGroup> groups = getAidGroups();
        for (AidGroup group : groups) {
            if (group.aids.contains(aid.toUpperCase())) {
                return group.category;
            }
        }
        return null;
    }

    public boolean hasCategory(String category) {
        if (this.mStaticAidGroups.containsKey(category)) {
            return true;
        }
        return this.mDynamicAidGroups.containsKey(category);
    }

    public boolean isOnHost() {
        return this.mOnHost;
    }

    public boolean requiresUnlock() {
        return this.mRequiresDeviceUnlock;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public int getUid() {
        return this.mUid;
    }

    public void setOrReplaceDynamicAidGroup(AidGroup aidGroup) {
        this.mDynamicAidGroups.put(aidGroup.getCategory(), aidGroup);
    }

    public CharSequence loadLabel(PackageManager pm) {
        return this.mService.loadLabel(pm);
    }

    public CharSequence loadAppLabel(PackageManager pm) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(this.mService.resolvePackageName, 128));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public Drawable loadIcon(PackageManager pm) {
        return this.mService.loadIcon(pm);
    }

    public Drawable loadBanner(PackageManager pm) {
        try {
            Resources res = pm.getResourcesForApplication(this.mService.serviceInfo.packageName);
            Drawable banner = res.getDrawable(this.mBannerResourceId);
            return banner;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not load banner.");
            return null;
        } catch (Resources.NotFoundException e2) {
            Log.e(TAG, "Could not load banner.");
            return null;
        }
    }

    public String getSettingsActivityName() {
        return this.mSettingsActivityName;
    }

    public String toString() {
        StringBuilder out = new StringBuilder("ApduService: ");
        out.append(getComponent());
        out.append(", description: ").append(this.mDescription);
        out.append(", Static AID Groups: ");
        for (AidGroup aidGroup : this.mStaticAidGroups.values()) {
            out.append(aidGroup.toString());
        }
        out.append(", Dynamic AID Groups: ");
        for (AidGroup aidGroup2 : this.mDynamicAidGroups.values()) {
            out.append(aidGroup2.toString());
        }
        return out.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApduServiceInfo)) {
            return false;
        }
        ApduServiceInfo thatService = (ApduServiceInfo) o;
        return thatService.getComponent().equals(getComponent());
    }

    public int hashCode() {
        return getComponent().hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        this.mService.writeToParcel(dest, flags);
        dest.writeString(this.mDescription);
        dest.writeInt(this.mOnHost ? 1 : 0);
        dest.writeInt(this.mStaticAidGroups.size());
        if (this.mStaticAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mStaticAidGroups.values()));
        }
        dest.writeInt(this.mDynamicAidGroups.size());
        if (this.mDynamicAidGroups.size() > 0) {
            dest.writeTypedList(new ArrayList(this.mDynamicAidGroups.values()));
        }
        dest.writeInt(this.mRequiresDeviceUnlock ? 1 : 0);
        dest.writeInt(this.mBannerResourceId);
        dest.writeInt(this.mUid);
        dest.writeString(this.mSettingsActivityName);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("    " + getComponent() + " (Description: " + getDescription() + ")");
        pw.println("    Static AID groups:");
        for (AidGroup group : this.mStaticAidGroups.values()) {
            pw.println("        Category: " + group.category);
            for (String aid : group.aids) {
                pw.println("            AID: " + aid);
            }
        }
        pw.println("    Dynamic AID groups:");
        for (AidGroup group2 : this.mDynamicAidGroups.values()) {
            pw.println("        Category: " + group2.category);
            for (String aid2 : group2.aids) {
                pw.println("            AID: " + aid2);
            }
        }
        pw.println("    Settings Activity: " + this.mSettingsActivityName);
    }
}
