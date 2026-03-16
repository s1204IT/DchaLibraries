package com.android.internal.location;

import android.location.LocationRequest;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.TimeUtils;
import java.util.ArrayList;
import java.util.List;

public final class ProviderRequest implements Parcelable {
    public static final Parcelable.Creator<ProviderRequest> CREATOR = new Parcelable.Creator<ProviderRequest>() {
        @Override
        public ProviderRequest createFromParcel(Parcel in) {
            ProviderRequest request = new ProviderRequest();
            request.reportLocation = in.readInt() == 1;
            request.interval = in.readLong();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                request.locationRequests.add(LocationRequest.CREATOR.createFromParcel(in));
            }
            return request;
        }

        @Override
        public ProviderRequest[] newArray(int size) {
            return new ProviderRequest[size];
        }
    };
    public boolean reportLocation = false;
    public long interval = Long.MAX_VALUE;
    public List<LocationRequest> locationRequests = new ArrayList();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.reportLocation ? 1 : 0);
        parcel.writeLong(this.interval);
        parcel.writeInt(this.locationRequests.size());
        for (LocationRequest request : this.locationRequests) {
            request.writeToParcel(parcel, flags);
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("ProviderRequest[");
        if (this.reportLocation) {
            s.append("ON");
            s.append(" interval=");
            TimeUtils.formatDuration(this.interval, s);
        } else {
            s.append("OFF");
        }
        s.append(']');
        return s.toString();
    }
}
