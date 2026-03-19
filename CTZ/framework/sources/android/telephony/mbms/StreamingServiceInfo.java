package android.telephony.mbms;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StreamingServiceInfo extends ServiceInfo implements Parcelable {
    public static final Parcelable.Creator<StreamingServiceInfo> CREATOR = new Parcelable.Creator<StreamingServiceInfo>() {
        @Override
        public StreamingServiceInfo createFromParcel(Parcel parcel) {
            return new StreamingServiceInfo(parcel);
        }

        @Override
        public StreamingServiceInfo[] newArray(int i) {
            return new StreamingServiceInfo[i];
        }
    };

    @SystemApi
    public StreamingServiceInfo(Map<Locale, String> map, String str, List<Locale> list, String str2, Date date, Date date2) {
        super(map, str, list, str2, date, date2);
    }

    private StreamingServiceInfo(Parcel parcel) {
        super(parcel);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
