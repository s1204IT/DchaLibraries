package android.net.wifi.hotspot2;

import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.wifi.WifiSsid;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class OsuProvider implements Parcelable {
    public static final Parcelable.Creator<OsuProvider> CREATOR = new Parcelable.Creator<OsuProvider>() {
        @Override
        public OsuProvider createFromParcel(Parcel parcel) {
            WifiSsid wifiSsid = (WifiSsid) parcel.readParcelable(null);
            String string = parcel.readString();
            String string2 = parcel.readString();
            Uri uri = (Uri) parcel.readParcelable(null);
            String string3 = parcel.readString();
            ArrayList arrayList = new ArrayList();
            parcel.readList(arrayList, null);
            return new OsuProvider(wifiSsid, string, string2, uri, string3, arrayList, (Icon) parcel.readParcelable(null));
        }

        @Override
        public OsuProvider[] newArray(int i) {
            return new OsuProvider[i];
        }
    };
    public static final int METHOD_OMA_DM = 0;
    public static final int METHOD_SOAP_XML_SPP = 1;
    private final String mFriendlyName;
    private final Icon mIcon;
    private final List<Integer> mMethodList;
    private final String mNetworkAccessIdentifier;
    private final WifiSsid mOsuSsid;
    private final Uri mServerUri;
    private final String mServiceDescription;

    public OsuProvider(WifiSsid wifiSsid, String str, String str2, Uri uri, String str3, List<Integer> list, Icon icon) {
        this.mOsuSsid = wifiSsid;
        this.mFriendlyName = str;
        this.mServiceDescription = str2;
        this.mServerUri = uri;
        this.mNetworkAccessIdentifier = str3;
        if (list == null) {
            this.mMethodList = new ArrayList();
        } else {
            this.mMethodList = new ArrayList(list);
        }
        this.mIcon = icon;
    }

    public OsuProvider(OsuProvider osuProvider) {
        if (osuProvider == null) {
            this.mOsuSsid = null;
            this.mFriendlyName = null;
            this.mServiceDescription = null;
            this.mServerUri = null;
            this.mNetworkAccessIdentifier = null;
            this.mMethodList = new ArrayList();
            this.mIcon = null;
            return;
        }
        this.mOsuSsid = osuProvider.mOsuSsid;
        this.mFriendlyName = osuProvider.mFriendlyName;
        this.mServiceDescription = osuProvider.mServiceDescription;
        this.mServerUri = osuProvider.mServerUri;
        this.mNetworkAccessIdentifier = osuProvider.mNetworkAccessIdentifier;
        if (osuProvider.mMethodList == null) {
            this.mMethodList = new ArrayList();
        } else {
            this.mMethodList = new ArrayList(osuProvider.mMethodList);
        }
        this.mIcon = osuProvider.mIcon;
    }

    public WifiSsid getOsuSsid() {
        return this.mOsuSsid;
    }

    public String getFriendlyName() {
        return this.mFriendlyName;
    }

    public String getServiceDescription() {
        return this.mServiceDescription;
    }

    public Uri getServerUri() {
        return this.mServerUri;
    }

    public String getNetworkAccessIdentifier() {
        return this.mNetworkAccessIdentifier;
    }

    public List<Integer> getMethodList() {
        return Collections.unmodifiableList(this.mMethodList);
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mOsuSsid, i);
        parcel.writeString(this.mFriendlyName);
        parcel.writeString(this.mServiceDescription);
        parcel.writeParcelable(this.mServerUri, i);
        parcel.writeString(this.mNetworkAccessIdentifier);
        parcel.writeList(this.mMethodList);
        parcel.writeParcelable(this.mIcon, i);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OsuProvider)) {
            return false;
        }
        OsuProvider osuProvider = (OsuProvider) obj;
        if (this.mOsuSsid != null ? this.mOsuSsid.equals(osuProvider.mOsuSsid) : osuProvider.mOsuSsid == null) {
            if (TextUtils.equals(this.mFriendlyName, osuProvider.mFriendlyName) && TextUtils.equals(this.mServiceDescription, osuProvider.mServiceDescription) && (this.mServerUri != null ? this.mServerUri.equals(osuProvider.mServerUri) : osuProvider.mServerUri == null) && TextUtils.equals(this.mNetworkAccessIdentifier, osuProvider.mNetworkAccessIdentifier) && (this.mMethodList != null ? this.mMethodList.equals(osuProvider.mMethodList) : osuProvider.mMethodList == null)) {
                if (this.mIcon == null) {
                    if (osuProvider.mIcon == null) {
                        return true;
                    }
                } else if (this.mIcon.sameAs(osuProvider.mIcon)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.mOsuSsid, this.mFriendlyName, this.mServiceDescription, this.mServerUri, this.mNetworkAccessIdentifier, this.mMethodList, this.mIcon);
    }

    public String toString() {
        return "OsuProvider{mOsuSsid=" + this.mOsuSsid + " mFriendlyName=" + this.mFriendlyName + " mServiceDescription=" + this.mServiceDescription + " mServerUri=" + this.mServerUri + " mNetworkAccessIdentifier=" + this.mNetworkAccessIdentifier + " mMethodList=" + this.mMethodList + " mIcon=" + this.mIcon;
    }
}
