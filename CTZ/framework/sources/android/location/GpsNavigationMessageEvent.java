package android.location;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import java.security.InvalidParameterException;

@SystemApi
public class GpsNavigationMessageEvent implements Parcelable {
    private final GpsNavigationMessage mNavigationMessage;
    public static int STATUS_NOT_SUPPORTED = 0;
    public static int STATUS_READY = 1;
    public static int STATUS_GPS_LOCATION_DISABLED = 2;
    public static final Parcelable.Creator<GpsNavigationMessageEvent> CREATOR = new Parcelable.Creator<GpsNavigationMessageEvent>() {
        @Override
        public GpsNavigationMessageEvent createFromParcel(Parcel parcel) {
            return new GpsNavigationMessageEvent((GpsNavigationMessage) parcel.readParcelable(getClass().getClassLoader()));
        }

        @Override
        public GpsNavigationMessageEvent[] newArray(int i) {
            return new GpsNavigationMessageEvent[i];
        }
    };

    @SystemApi
    public interface Listener {
        void onGpsNavigationMessageReceived(GpsNavigationMessageEvent gpsNavigationMessageEvent);

        void onStatusChanged(int i);
    }

    public GpsNavigationMessageEvent(GpsNavigationMessage gpsNavigationMessage) {
        if (gpsNavigationMessage == null) {
            throw new InvalidParameterException("Parameter 'message' must not be null.");
        }
        this.mNavigationMessage = gpsNavigationMessage;
    }

    public GpsNavigationMessage getNavigationMessage() {
        return this.mNavigationMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(this.mNavigationMessage, i);
    }

    public String toString() {
        return "[ GpsNavigationMessageEvent:\n\n" + this.mNavigationMessage.toString() + "\n]";
    }
}
