package com.android.internal.location;

import android.location.LocationRequest;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TimeUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ProviderRequest implements Parcelable {
    public static final Parcelable.Creator<ProviderRequest> CREATOR = new Parcelable.Creator<ProviderRequest>() {
        @Override
        public ProviderRequest createFromParcel(Parcel parcel) {
            ProviderRequest providerRequest = new ProviderRequest();
            providerRequest.reportLocation = parcel.readInt() == 1;
            providerRequest.interval = parcel.readLong();
            providerRequest.lowPowerMode = parcel.readBoolean();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                providerRequest.locationRequests.add(LocationRequest.CREATOR.createFromParcel(parcel));
            }
            return providerRequest;
        }

        @Override
        public ProviderRequest[] newArray(int i) {
            return new ProviderRequest[i];
        }
    };
    public boolean reportLocation = false;
    public long interval = Long.MAX_VALUE;
    public boolean lowPowerMode = false;
    public List<LocationRequest> locationRequests = new ArrayList();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.reportLocation ? 1 : 0);
        parcel.writeLong(this.interval);
        parcel.writeBoolean(this.lowPowerMode);
        parcel.writeInt(this.locationRequests.size());
        Iterator<LocationRequest> it = this.locationRequests.iterator();
        while (it.hasNext()) {
            it.next().writeToParcel(parcel, i);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProviderRequest[");
        if (this.reportLocation) {
            sb.append("ON");
            sb.append(" interval=");
            TimeUtils.formatDuration(this.interval, sb);
            sb.append(" lowPowerMode=" + this.lowPowerMode);
        } else {
            sb.append("OFF");
        }
        sb.append(']');
        return sb.toString();
    }
}
