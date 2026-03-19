package android.location;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Address implements Parcelable {
    public static final Parcelable.Creator<Address> CREATOR = new Parcelable.Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel parcel) {
            Locale locale;
            String string = parcel.readString();
            String string2 = parcel.readString();
            if (string2.length() > 0) {
                locale = new Locale(string, string2);
            } else {
                locale = new Locale(string);
            }
            Address address = new Address(locale);
            int i = parcel.readInt();
            if (i > 0) {
                address.mAddressLines = new HashMap(i);
                for (int i2 = 0; i2 < i; i2++) {
                    int i3 = parcel.readInt();
                    address.mAddressLines.put(Integer.valueOf(i3), parcel.readString());
                    address.mMaxAddressLineIndex = Math.max(address.mMaxAddressLineIndex, i3);
                }
            } else {
                address.mAddressLines = null;
                address.mMaxAddressLineIndex = -1;
            }
            address.mFeatureName = parcel.readString();
            address.mAdminArea = parcel.readString();
            address.mSubAdminArea = parcel.readString();
            address.mLocality = parcel.readString();
            address.mSubLocality = parcel.readString();
            address.mThoroughfare = parcel.readString();
            address.mSubThoroughfare = parcel.readString();
            address.mPremises = parcel.readString();
            address.mPostalCode = parcel.readString();
            address.mCountryCode = parcel.readString();
            address.mCountryName = parcel.readString();
            address.mHasLatitude = parcel.readInt() != 0;
            if (address.mHasLatitude) {
                address.mLatitude = parcel.readDouble();
            }
            address.mHasLongitude = parcel.readInt() != 0;
            if (address.mHasLongitude) {
                address.mLongitude = parcel.readDouble();
            }
            address.mPhone = parcel.readString();
            address.mUrl = parcel.readString();
            address.mExtras = parcel.readBundle();
            return address;
        }

        @Override
        public Address[] newArray(int i) {
            return new Address[i];
        }
    };
    private HashMap<Integer, String> mAddressLines;
    private String mAdminArea;
    private String mCountryCode;
    private String mCountryName;
    private String mFeatureName;
    private double mLatitude;
    private Locale mLocale;
    private String mLocality;
    private double mLongitude;
    private String mPhone;
    private String mPostalCode;
    private String mPremises;
    private String mSubAdminArea;
    private String mSubLocality;
    private String mSubThoroughfare;
    private String mThoroughfare;
    private String mUrl;
    private int mMaxAddressLineIndex = -1;
    private boolean mHasLatitude = false;
    private boolean mHasLongitude = false;
    private Bundle mExtras = null;

    public Address(Locale locale) {
        this.mLocale = locale;
    }

    public Locale getLocale() {
        return this.mLocale;
    }

    public int getMaxAddressLineIndex() {
        return this.mMaxAddressLineIndex;
    }

    public String getAddressLine(int i) {
        if (i < 0) {
            throw new IllegalArgumentException("index = " + i + " < 0");
        }
        if (this.mAddressLines == null) {
            return null;
        }
        return this.mAddressLines.get(Integer.valueOf(i));
    }

    public void setAddressLine(int i, String str) {
        if (i < 0) {
            throw new IllegalArgumentException("index = " + i + " < 0");
        }
        if (this.mAddressLines == null) {
            this.mAddressLines = new HashMap<>();
        }
        this.mAddressLines.put(Integer.valueOf(i), str);
        if (str == null) {
            this.mMaxAddressLineIndex = -1;
            Iterator<Integer> it = this.mAddressLines.keySet().iterator();
            while (it.hasNext()) {
                this.mMaxAddressLineIndex = Math.max(this.mMaxAddressLineIndex, it.next().intValue());
            }
            return;
        }
        this.mMaxAddressLineIndex = Math.max(this.mMaxAddressLineIndex, i);
    }

    public String getFeatureName() {
        return this.mFeatureName;
    }

    public void setFeatureName(String str) {
        this.mFeatureName = str;
    }

    public String getAdminArea() {
        return this.mAdminArea;
    }

    public void setAdminArea(String str) {
        this.mAdminArea = str;
    }

    public String getSubAdminArea() {
        return this.mSubAdminArea;
    }

    public void setSubAdminArea(String str) {
        this.mSubAdminArea = str;
    }

    public String getLocality() {
        return this.mLocality;
    }

    public void setLocality(String str) {
        this.mLocality = str;
    }

    public String getSubLocality() {
        return this.mSubLocality;
    }

    public void setSubLocality(String str) {
        this.mSubLocality = str;
    }

    public String getThoroughfare() {
        return this.mThoroughfare;
    }

    public void setThoroughfare(String str) {
        this.mThoroughfare = str;
    }

    public String getSubThoroughfare() {
        return this.mSubThoroughfare;
    }

    public void setSubThoroughfare(String str) {
        this.mSubThoroughfare = str;
    }

    public String getPremises() {
        return this.mPremises;
    }

    public void setPremises(String str) {
        this.mPremises = str;
    }

    public String getPostalCode() {
        return this.mPostalCode;
    }

    public void setPostalCode(String str) {
        this.mPostalCode = str;
    }

    public String getCountryCode() {
        return this.mCountryCode;
    }

    public void setCountryCode(String str) {
        this.mCountryCode = str;
    }

    public String getCountryName() {
        return this.mCountryName;
    }

    public void setCountryName(String str) {
        this.mCountryName = str;
    }

    public boolean hasLatitude() {
        return this.mHasLatitude;
    }

    public double getLatitude() {
        if (this.mHasLatitude) {
            return this.mLatitude;
        }
        throw new IllegalStateException();
    }

    public void setLatitude(double d) {
        this.mLatitude = d;
        this.mHasLatitude = true;
    }

    public void clearLatitude() {
        this.mHasLatitude = false;
    }

    public boolean hasLongitude() {
        return this.mHasLongitude;
    }

    public double getLongitude() {
        if (this.mHasLongitude) {
            return this.mLongitude;
        }
        throw new IllegalStateException();
    }

    public void setLongitude(double d) {
        this.mLongitude = d;
        this.mHasLongitude = true;
    }

    public void clearLongitude() {
        this.mHasLongitude = false;
    }

    public String getPhone() {
        return this.mPhone;
    }

    public void setPhone(String str) {
        this.mPhone = str;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public void setUrl(String str) {
        this.mUrl = str;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public void setExtras(Bundle bundle) {
        this.mExtras = bundle == null ? null : new Bundle(bundle);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Address[addressLines=[");
        for (int i = 0; i <= this.mMaxAddressLineIndex; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
            sb.append(':');
            String str = this.mAddressLines.get(Integer.valueOf(i));
            if (str == null) {
                sb.append("null");
            } else {
                sb.append('\"');
                sb.append(str);
                sb.append('\"');
            }
        }
        sb.append(']');
        sb.append(",feature=");
        sb.append(this.mFeatureName);
        sb.append(",admin=");
        sb.append(this.mAdminArea);
        sb.append(",sub-admin=");
        sb.append(this.mSubAdminArea);
        sb.append(",locality=");
        sb.append(this.mLocality);
        sb.append(",thoroughfare=");
        sb.append(this.mThoroughfare);
        sb.append(",postalCode=");
        sb.append(this.mPostalCode);
        sb.append(",countryCode=");
        sb.append(this.mCountryCode);
        sb.append(",countryName=");
        sb.append(this.mCountryName);
        sb.append(",hasLatitude=");
        sb.append(this.mHasLatitude);
        sb.append(",latitude=");
        sb.append(this.mLatitude);
        sb.append(",hasLongitude=");
        sb.append(this.mHasLongitude);
        sb.append(",longitude=");
        sb.append(this.mLongitude);
        sb.append(",phone=");
        sb.append(this.mPhone);
        sb.append(",url=");
        sb.append(this.mUrl);
        sb.append(",extras=");
        sb.append(this.mExtras);
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int describeContents() {
        if (this.mExtras != null) {
            return this.mExtras.describeContents();
        }
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mLocale.getLanguage());
        parcel.writeString(this.mLocale.getCountry());
        if (this.mAddressLines == null) {
            parcel.writeInt(0);
        } else {
            Set<Map.Entry<Integer, String>> setEntrySet = this.mAddressLines.entrySet();
            parcel.writeInt(setEntrySet.size());
            for (Map.Entry<Integer, String> entry : setEntrySet) {
                parcel.writeInt(entry.getKey().intValue());
                parcel.writeString(entry.getValue());
            }
        }
        parcel.writeString(this.mFeatureName);
        parcel.writeString(this.mAdminArea);
        parcel.writeString(this.mSubAdminArea);
        parcel.writeString(this.mLocality);
        parcel.writeString(this.mSubLocality);
        parcel.writeString(this.mThoroughfare);
        parcel.writeString(this.mSubThoroughfare);
        parcel.writeString(this.mPremises);
        parcel.writeString(this.mPostalCode);
        parcel.writeString(this.mCountryCode);
        parcel.writeString(this.mCountryName);
        parcel.writeInt(this.mHasLatitude ? 1 : 0);
        if (this.mHasLatitude) {
            parcel.writeDouble(this.mLatitude);
        }
        parcel.writeInt(this.mHasLongitude ? 1 : 0);
        if (this.mHasLongitude) {
            parcel.writeDouble(this.mLongitude);
        }
        parcel.writeString(this.mPhone);
        parcel.writeString(this.mUrl);
        parcel.writeBundle(this.mExtras);
    }
}
