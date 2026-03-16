package android.location;

import android.os.Parcel;
import android.os.Parcelable;
import java.security.InvalidParameterException;

public class GpsNavigationMessageEvent implements Parcelable {
    private final GpsNavigationMessage mNavigationMessage;
    public static int STATUS_NOT_SUPPORTED = 0;
    public static int STATUS_READY = 1;
    public static int STATUS_GPS_LOCATION_DISABLED = 2;
    public static final Parcelable.Creator<GpsNavigationMessageEvent> CREATOR = new Parcelable.Creator<GpsNavigationMessageEvent>() {
        @Override
        public GpsNavigationMessageEvent createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();
            GpsNavigationMessage navigationMessage = (GpsNavigationMessage) in.readParcelable(classLoader);
            return new GpsNavigationMessageEvent(navigationMessage);
        }

        @Override
        public GpsNavigationMessageEvent[] newArray(int size) {
            return new GpsNavigationMessageEvent[size];
        }
    };

    public interface Listener {
        void onGpsNavigationMessageReceived(GpsNavigationMessageEvent gpsNavigationMessageEvent);

        void onStatusChanged(int i);
    }

    public GpsNavigationMessageEvent(GpsNavigationMessage message) {
        if (message == null) {
            throw new InvalidParameterException("Parameter 'message' must not be null.");
        }
        this.mNavigationMessage = message;
    }

    public GpsNavigationMessage getNavigationMessage() {
        return this.mNavigationMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(this.mNavigationMessage, flags);
    }

    public String toString() {
        return "[ GpsNavigationMessageEvent:\n\n" + this.mNavigationMessage.toString() + "\n]";
    }
}
