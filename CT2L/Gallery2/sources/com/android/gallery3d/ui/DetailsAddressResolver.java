package com.android.gallery3d.ui;

import android.content.Context;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.GalleryUtils;
import com.android.gallery3d.util.ReverseGeocoder;
import com.android.gallery3d.util.ThreadPool;

public class DetailsAddressResolver {
    private Future<Address> mAddressLookupJob;
    private final AbstractGalleryActivity mContext;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private AddressResolvingListener mListener;

    public interface AddressResolvingListener {
        void onAddressAvailable(String str);
    }

    private class AddressLookupJob implements ThreadPool.Job<Address> {
        private double[] mLatlng;

        protected AddressLookupJob(double[] latlng) {
            this.mLatlng = latlng;
        }

        @Override
        public Address run(ThreadPool.JobContext jc) {
            ReverseGeocoder geocoder = new ReverseGeocoder(DetailsAddressResolver.this.mContext.getAndroidContext());
            return geocoder.lookupAddress(this.mLatlng[0], this.mLatlng[1], true);
        }
    }

    public DetailsAddressResolver(AbstractGalleryActivity context) {
        this.mContext = context;
    }

    public String resolveAddress(double[] latlng, AddressResolvingListener listener) {
        this.mListener = listener;
        this.mAddressLookupJob = this.mContext.getThreadPool().submit(new AddressLookupJob(latlng), new FutureListener<Address>() {
            @Override
            public void onFutureDone(final Future<Address> future) {
                DetailsAddressResolver.this.mAddressLookupJob = null;
                if (!future.isCancelled()) {
                    DetailsAddressResolver.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            DetailsAddressResolver.this.updateLocation((Address) future.get());
                        }
                    });
                }
            }
        });
        return GalleryUtils.formatLatitudeLongitude("(%f,%f)", latlng[0], latlng[1]);
    }

    private void updateLocation(Address address) {
        if (address != null) {
            Context context = this.mContext.getAndroidContext();
            String[] parts = {address.getAdminArea(), address.getSubAdminArea(), address.getLocality(), address.getSubLocality(), address.getThoroughfare(), address.getSubThoroughfare(), address.getPremises(), address.getPostalCode(), address.getCountryName()};
            String addressText = "";
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] != null && !parts[i].isEmpty()) {
                    if (!addressText.isEmpty()) {
                        addressText = addressText + ", ";
                    }
                    addressText = addressText + parts[i];
                }
            }
            String text = String.format("%s : %s", DetailsHelper.getDetailsName(context, 4), addressText);
            this.mListener.onAddressAvailable(text);
        }
    }

    public void cancel() {
        if (this.mAddressLookupJob != null) {
            this.mAddressLookupJob.cancel();
            this.mAddressLookupJob = null;
        }
    }
}
