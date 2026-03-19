package com.android.location.provider;

import android.location.LocationRequest;
import com.android.internal.location.ProviderRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ProviderRequestUnbundled {
    private final ProviderRequest mRequest;

    public ProviderRequestUnbundled(ProviderRequest providerRequest) {
        this.mRequest = providerRequest;
    }

    public boolean getReportLocation() {
        return this.mRequest.reportLocation;
    }

    public long getInterval() {
        return this.mRequest.interval;
    }

    public List<LocationRequestUnbundled> getLocationRequests() {
        ArrayList arrayList = new ArrayList(this.mRequest.locationRequests.size());
        Iterator it = this.mRequest.locationRequests.iterator();
        while (it.hasNext()) {
            arrayList.add(new LocationRequestUnbundled((LocationRequest) it.next()));
        }
        return arrayList;
    }

    public String toString() {
        return this.mRequest.toString();
    }
}
