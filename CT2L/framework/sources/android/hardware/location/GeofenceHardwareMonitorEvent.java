package android.hardware.location;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class GeofenceHardwareMonitorEvent implements Parcelable {
    public static final Parcelable.Creator<GeofenceHardwareMonitorEvent> CREATOR = new Parcelable.Creator<GeofenceHardwareMonitorEvent>() {
        @Override
        public GeofenceHardwareMonitorEvent createFromParcel(Parcel source) {
            ClassLoader classLoader = GeofenceHardwareMonitorEvent.class.getClassLoader();
            int monitoringType = source.readInt();
            int monitoringStatus = source.readInt();
            int sourceTechnologies = source.readInt();
            Location location = (Location) source.readParcelable(classLoader);
            return new GeofenceHardwareMonitorEvent(monitoringType, monitoringStatus, sourceTechnologies, location);
        }

        @Override
        public GeofenceHardwareMonitorEvent[] newArray(int size) {
            return new GeofenceHardwareMonitorEvent[size];
        }
    };
    private final Location mLocation;
    private final int mMonitoringStatus;
    private final int mMonitoringType;
    private final int mSourceTechnologies;

    public GeofenceHardwareMonitorEvent(int monitoringType, int monitoringStatus, int sourceTechnologies, Location location) {
        this.mMonitoringType = monitoringType;
        this.mMonitoringStatus = monitoringStatus;
        this.mSourceTechnologies = sourceTechnologies;
        this.mLocation = location;
    }

    public int getMonitoringType() {
        return this.mMonitoringType;
    }

    public int getMonitoringStatus() {
        return this.mMonitoringStatus;
    }

    public int getSourceTechnologies() {
        return this.mSourceTechnologies;
    }

    public Location getLocation() {
        return this.mLocation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.mMonitoringType);
        parcel.writeInt(this.mMonitoringStatus);
        parcel.writeInt(this.mSourceTechnologies);
        parcel.writeParcelable(this.mLocation, flags);
    }

    public String toString() {
        return String.format("GeofenceHardwareMonitorEvent: type=%d, status=%d, sources=%d, location=%s", Integer.valueOf(this.mMonitoringType), Integer.valueOf(this.mMonitoringStatus), Integer.valueOf(this.mSourceTechnologies), this.mLocation);
    }
}
