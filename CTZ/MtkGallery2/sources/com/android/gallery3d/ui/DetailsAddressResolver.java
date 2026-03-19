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

        protected AddressLookupJob(double[] dArr) {
            this.mLatlng = dArr;
        }

        @Override
        public Address run(ThreadPool.JobContext jobContext) {
            return new ReverseGeocoder(DetailsAddressResolver.this.mContext.getAndroidContext()).lookupAddress(this.mLatlng[0], this.mLatlng[1], true);
        }
    }

    public DetailsAddressResolver(AbstractGalleryActivity abstractGalleryActivity) {
        this.mContext = abstractGalleryActivity;
    }

    public String resolveAddress(double[] dArr, AddressResolvingListener addressResolvingListener) {
        this.mListener = addressResolvingListener;
        this.mAddressLookupJob = this.mContext.getThreadPool().submit(new AddressLookupJob(dArr), new FutureListener<Address>() {
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
        return GalleryUtils.formatLatitudeLongitude("(%f,%f)", dArr[0], dArr[1]);
    }

    private void updateLocation(Address address) {
        if (address != null) {
            Context androidContext = this.mContext.getAndroidContext();
            String[] strArr = {address.getAdminArea(), address.getSubAdminArea(), address.getLocality(), address.getSubLocality(), address.getThoroughfare(), address.getSubThoroughfare(), address.getPremises(), address.getPostalCode(), address.getCountryName()};
            String str = "";
            for (int i = 0; i < strArr.length; i++) {
                if (strArr[i] != null && !strArr[i].isEmpty()) {
                    if (!str.isEmpty()) {
                        str = str + ", ";
                    }
                    str = str + strArr[i];
                }
            }
            this.mListener.onAddressAvailable(String.format("%s : %s", DetailsHelper.getDetailsName(androidContext, 4), str));
        }
    }

    public void cancel() {
        if (this.mAddressLookupJob != null) {
            this.mAddressLookupJob.cancel();
            this.mAddressLookupJob = null;
        }
    }
}
