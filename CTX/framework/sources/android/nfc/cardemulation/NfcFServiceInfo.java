package android.nfc.cardemulation;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import org.xmlpull.v1.XmlPullParserException;

public final class NfcFServiceInfo implements Parcelable {
    public static final Parcelable.Creator<NfcFServiceInfo> CREATOR = new Parcelable.Creator<NfcFServiceInfo>() {
        @Override
        public NfcFServiceInfo createFromParcel(Parcel parcel) {
            return new NfcFServiceInfo(ResolveInfo.CREATOR.createFromParcel(parcel), parcel.readString(), parcel.readString(), parcel.readInt() != 0 ? parcel.readString() : null, parcel.readString(), parcel.readInt() != 0 ? parcel.readString() : null, parcel.readInt(), parcel.readString());
        }

        @Override
        public NfcFServiceInfo[] newArray(int i) {
            return new NfcFServiceInfo[i];
        }
    };
    private static final String DEFAULT_T3T_PMM = "FFFFFFFFFFFFFFFF";
    static final String TAG = "NfcFServiceInfo";
    final String mDescription;
    String mDynamicNfcid2;
    String mDynamicSystemCode;
    final String mNfcid2;
    final ResolveInfo mService;
    final String mSystemCode;
    final String mT3tPmm;
    final int mUid;

    public NfcFServiceInfo(ResolveInfo resolveInfo, String str, String str2, String str3, String str4, String str5, int i, String str6) {
        this.mService = resolveInfo;
        this.mDescription = str;
        this.mSystemCode = str2;
        this.mDynamicSystemCode = str3;
        this.mNfcid2 = str4;
        this.mDynamicNfcid2 = str5;
        this.mUid = i;
        this.mT3tPmm = str6;
    }

    public NfcFServiceInfo(PackageManager packageManager, ResolveInfo resolveInfo) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        XmlResourceParser xmlResourceParser;
        ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        try {
            xmlResourceParserLoadXmlMetaData = serviceInfo.loadXmlMetaData(packageManager, HostNfcFService.SERVICE_META_DATA);
            try {
                if (xmlResourceParserLoadXmlMetaData == null) {
                    throw new XmlPullParserException("No android.nfc.cardemulation.host_nfcf_service meta-data");
                }
                for (int eventType = xmlResourceParserLoadXmlMetaData.getEventType(); eventType != 2 && eventType != 1; eventType = xmlResourceParserLoadXmlMetaData.next()) {
                }
                if (!"host-nfcf-service".equals(xmlResourceParserLoadXmlMetaData.getName())) {
                    throw new XmlPullParserException("Meta-data does not start with <host-nfcf-service> tag");
                }
                Resources resourcesForApplication = packageManager.getResourcesForApplication(serviceInfo.applicationInfo);
                AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserLoadXmlMetaData);
                TypedArray typedArrayObtainAttributes = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.HostNfcFService);
                this.mService = resolveInfo;
                this.mDescription = typedArrayObtainAttributes.getString(0);
                this.mDynamicSystemCode = null;
                this.mDynamicNfcid2 = null;
                typedArrayObtainAttributes.recycle();
                int depth = xmlResourceParserLoadXmlMetaData.getDepth();
                String str = null;
                String upperCase = null;
                String upperCase2 = null;
                while (true) {
                    int next = xmlResourceParserLoadXmlMetaData.next();
                    if ((next == 3 && xmlResourceParserLoadXmlMetaData.getDepth() <= depth) || next == 1) {
                        break;
                    }
                    String name = xmlResourceParserLoadXmlMetaData.getName();
                    if (next == 2 && "system-code-filter".equals(name) && str == null) {
                        TypedArray typedArrayObtainAttributes2 = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.SystemCodeFilter);
                        String upperCase3 = typedArrayObtainAttributes2.getString(0).toUpperCase();
                        if (!NfcFCardEmulation.isValidSystemCode(upperCase3) && !upperCase3.equalsIgnoreCase(WifiEnterpriseConfig.EMPTY_VALUE)) {
                            Log.e(TAG, "Invalid System Code: " + upperCase3);
                            upperCase3 = null;
                        }
                        typedArrayObtainAttributes2.recycle();
                        str = upperCase3;
                    } else if (next == 2 && "nfcid2-filter".equals(name) && upperCase == null) {
                        TypedArray typedArrayObtainAttributes3 = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.Nfcid2Filter);
                        upperCase = typedArrayObtainAttributes3.getString(0).toUpperCase();
                        if (!upperCase.equalsIgnoreCase("RANDOM") && !upperCase.equalsIgnoreCase(WifiEnterpriseConfig.EMPTY_VALUE) && !NfcFCardEmulation.isValidNfcid2(upperCase)) {
                            Log.e(TAG, "Invalid NFCID2: " + upperCase);
                            upperCase = null;
                        }
                        typedArrayObtainAttributes3.recycle();
                    } else if (next == 2 && name.equals("t3tPmm-filter") && upperCase2 == null) {
                        TypedArray typedArrayObtainAttributes4 = resourcesForApplication.obtainAttributes(attributeSetAsAttributeSet, R.styleable.T3tPmmFilter);
                        upperCase2 = typedArrayObtainAttributes4.getString(0).toUpperCase();
                        typedArrayObtainAttributes4.recycle();
                    }
                }
                this.mSystemCode = str == null ? WifiEnterpriseConfig.EMPTY_VALUE : str;
                this.mNfcid2 = upperCase == null ? WifiEnterpriseConfig.EMPTY_VALUE : upperCase;
                this.mT3tPmm = upperCase2 == null ? DEFAULT_T3T_PMM : upperCase2;
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                this.mUid = serviceInfo.applicationInfo.uid;
            } catch (PackageManager.NameNotFoundException e) {
                xmlResourceParser = xmlResourceParserLoadXmlMetaData;
                try {
                    throw new XmlPullParserException("Unable to create context for: " + serviceInfo.packageName);
                } catch (Throwable th) {
                    th = th;
                    xmlResourceParserLoadXmlMetaData = xmlResourceParser;
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                    }
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                if (xmlResourceParserLoadXmlMetaData != null) {
                }
                throw th;
            }
        } catch (PackageManager.NameNotFoundException e2) {
            xmlResourceParser = null;
        } catch (Throwable th3) {
            th = th3;
            xmlResourceParserLoadXmlMetaData = null;
        }
    }

    public ComponentName getComponent() {
        return new ComponentName(this.mService.serviceInfo.packageName, this.mService.serviceInfo.name);
    }

    public String getSystemCode() {
        return this.mDynamicSystemCode == null ? this.mSystemCode : this.mDynamicSystemCode;
    }

    public void setOrReplaceDynamicSystemCode(String str) {
        this.mDynamicSystemCode = str;
    }

    public String getNfcid2() {
        return this.mDynamicNfcid2 == null ? this.mNfcid2 : this.mDynamicNfcid2;
    }

    public void setOrReplaceDynamicNfcid2(String str) {
        this.mDynamicNfcid2 = str;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getT3tPmm() {
        return this.mT3tPmm;
    }

    public CharSequence loadLabel(PackageManager packageManager) {
        return this.mService.loadLabel(packageManager);
    }

    public Drawable loadIcon(PackageManager packageManager) {
        return this.mService.loadIcon(packageManager);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("NfcFService: ");
        sb.append(getComponent());
        sb.append(", description: " + this.mDescription);
        sb.append(", System Code: " + this.mSystemCode);
        if (this.mDynamicSystemCode != null) {
            sb.append(", dynamic System Code: " + this.mDynamicSystemCode);
        }
        sb.append(", NFCID2: " + this.mNfcid2);
        if (this.mDynamicNfcid2 != null) {
            sb.append(", dynamic NFCID2: " + this.mDynamicNfcid2);
        }
        sb.append(", T3T PMM:" + this.mT3tPmm);
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NfcFServiceInfo)) {
            return false;
        }
        NfcFServiceInfo nfcFServiceInfo = (NfcFServiceInfo) obj;
        return nfcFServiceInfo.getComponent().equals(getComponent()) && nfcFServiceInfo.mSystemCode.equalsIgnoreCase(this.mSystemCode) && nfcFServiceInfo.mNfcid2.equalsIgnoreCase(this.mNfcid2) && nfcFServiceInfo.mT3tPmm.equalsIgnoreCase(this.mT3tPmm);
    }

    public int hashCode() {
        return getComponent().hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mService.writeToParcel(parcel, i);
        parcel.writeString(this.mDescription);
        parcel.writeString(this.mSystemCode);
        parcel.writeInt(this.mDynamicSystemCode != null ? 1 : 0);
        if (this.mDynamicSystemCode != null) {
            parcel.writeString(this.mDynamicSystemCode);
        }
        parcel.writeString(this.mNfcid2);
        parcel.writeInt(this.mDynamicNfcid2 != null ? 1 : 0);
        if (this.mDynamicNfcid2 != null) {
            parcel.writeString(this.mDynamicNfcid2);
        }
        parcel.writeInt(this.mUid);
        parcel.writeString(this.mT3tPmm);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("    " + getComponent() + " (Description: " + getDescription() + ")");
        StringBuilder sb = new StringBuilder();
        sb.append("    System Code: ");
        sb.append(getSystemCode());
        printWriter.println(sb.toString());
        printWriter.println("    NFCID2: " + getNfcid2());
        printWriter.println("    T3tPmm: " + getT3tPmm());
    }
}
