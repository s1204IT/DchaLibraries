package android.location;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Locale;

public class GeocoderParams implements Parcelable {
    public static final Parcelable.Creator<GeocoderParams> CREATOR = new Parcelable.Creator<GeocoderParams>() {
        @Override
        public GeocoderParams createFromParcel(Parcel parcel) {
            GeocoderParams geocoderParams = new GeocoderParams();
            geocoderParams.mLocale = new Locale(parcel.readString(), parcel.readString(), parcel.readString());
            geocoderParams.mPackageName = parcel.readString();
            return geocoderParams;
        }

        @Override
        public GeocoderParams[] newArray(int i) {
            return new GeocoderParams[i];
        }
    };
    private Locale mLocale;
    private String mPackageName;

    private GeocoderParams() {
    }

    public GeocoderParams(Context context, Locale locale) {
        this.mLocale = locale;
        this.mPackageName = context.getPackageName();
    }

    public Locale getLocale() {
        return this.mLocale;
    }

    public String getClientPackage() {
        return this.mPackageName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mLocale.getLanguage());
        parcel.writeString(this.mLocale.getCountry());
        parcel.writeString(this.mLocale.getVariant());
        parcel.writeString(this.mPackageName);
    }
}
