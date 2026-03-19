package com.android.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.RemoteException;
import android.service.carrier.ICarrierMessagingCallback;
import android.service.carrier.ICarrierMessagingService;
import android.service.carrier.MessagePdu;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.Rlog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class CarrierServicesSmsFilter {
    protected static final boolean DBG = true;
    private final CarrierServicesSmsFilterCallbackInterface mCarrierServicesSmsFilterCallback;
    private final Context mContext;
    private final int mDestPort;
    private final String mLogTag;
    private final String mPduFormat;
    private final byte[][] mPdus;
    private final Phone mPhone;

    @VisibleForTesting
    public interface CarrierServicesSmsFilterCallbackInterface {
        void onFilterComplete(int i);
    }

    @VisibleForTesting
    public CarrierServicesSmsFilter(Context context, Phone phone, byte[][] bArr, int i, String str, CarrierServicesSmsFilterCallbackInterface carrierServicesSmsFilterCallbackInterface, String str2) {
        this.mContext = context;
        this.mPhone = phone;
        this.mPdus = bArr;
        this.mDestPort = i;
        this.mPduFormat = str;
        this.mCarrierServicesSmsFilterCallback = carrierServicesSmsFilterCallbackInterface;
        this.mLogTag = str2;
    }

    @VisibleForTesting
    public boolean filter() {
        Optional<String> carrierAppPackageForFiltering = getCarrierAppPackageForFiltering();
        ArrayList arrayList = new ArrayList();
        if (carrierAppPackageForFiltering.isPresent()) {
            arrayList.add(carrierAppPackageForFiltering.get());
        }
        String carrierImsPackageForIntent = CarrierSmsUtils.getCarrierImsPackageForIntent(this.mContext, this.mPhone, new Intent("android.service.carrier.CarrierMessagingService"));
        if (carrierImsPackageForIntent != null) {
            arrayList.add(carrierImsPackageForIntent);
        }
        FilterAggregator filterAggregator = new FilterAggregator(arrayList.size());
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            filterWithPackage((String) it.next(), filterAggregator);
        }
        return arrayList.size() > 0;
    }

    private Optional<String> getCarrierAppPackageForFiltering() {
        List<String> carrierPackageNamesForIntent;
        UiccCard uiccCard = UiccController.getInstance().getUiccCard(this.mPhone.getPhoneId());
        if (uiccCard != null) {
            carrierPackageNamesForIntent = uiccCard.getCarrierPackageNamesForIntent(this.mContext.getPackageManager(), new Intent("android.service.carrier.CarrierMessagingService"));
        } else {
            Rlog.e(this.mLogTag, "UiccCard not initialized.");
            carrierPackageNamesForIntent = null;
        }
        if (carrierPackageNamesForIntent != null && carrierPackageNamesForIntent.size() == 1) {
            log("Found carrier package.");
            return Optional.of(carrierPackageNamesForIntent.get(0));
        }
        List<String> systemAppForIntent = getSystemAppForIntent(new Intent("android.service.carrier.CarrierMessagingService"));
        if (systemAppForIntent != null && systemAppForIntent.size() == 1) {
            log("Found system package.");
            return Optional.of(systemAppForIntent.get(0));
        }
        logv("Unable to find carrier package: " + carrierPackageNamesForIntent + ", nor systemPackages: " + systemAppForIntent);
        return Optional.empty();
    }

    private void filterWithPackage(String str, FilterAggregator filterAggregator) {
        CarrierSmsFilter carrierSmsFilter = new CarrierSmsFilter(this.mPdus, this.mDestPort, this.mPduFormat);
        carrierSmsFilter.filterSms(str, new CarrierSmsFilterCallback(filterAggregator, carrierSmsFilter));
    }

    private List<String> getSystemAppForIntent(Intent intent) {
        ArrayList arrayList = new ArrayList();
        PackageManager packageManager = this.mContext.getPackageManager();
        for (ResolveInfo resolveInfo : packageManager.queryIntentServices(intent, 0)) {
            if (resolveInfo.serviceInfo == null) {
                loge("Can't get service information from " + resolveInfo);
            } else {
                String str = resolveInfo.serviceInfo.packageName;
                if (packageManager.checkPermission("android.permission.CARRIER_FILTER_SMS", str) == 0) {
                    arrayList.add(str);
                    log("getSystemAppForIntent: added package " + str);
                }
            }
        }
        return arrayList;
    }

    private void log(String str) {
        Rlog.d(this.mLogTag, str);
    }

    private void loge(String str) {
        Rlog.e(this.mLogTag, str);
    }

    private void logv(String str) {
        Rlog.e(this.mLogTag, str);
    }

    private final class CarrierSmsFilter extends CarrierMessagingServiceManager {
        private final int mDestPort;
        private final byte[][] mPdus;
        private volatile CarrierSmsFilterCallback mSmsFilterCallback;
        private final String mSmsFormat;

        CarrierSmsFilter(byte[][] bArr, int i, String str) {
            this.mPdus = bArr;
            this.mDestPort = i;
            this.mSmsFormat = str;
        }

        void filterSms(String str, CarrierSmsFilterCallback carrierSmsFilterCallback) {
            this.mSmsFilterCallback = carrierSmsFilterCallback;
            if (!bindToCarrierMessagingService(CarrierServicesSmsFilter.this.mContext, str)) {
                CarrierServicesSmsFilter.this.loge("bindService() for carrier messaging service failed");
                carrierSmsFilterCallback.onFilterComplete(0);
            } else {
                CarrierServicesSmsFilter.this.logv("bindService() for carrier messaging service succeeded");
            }
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService iCarrierMessagingService) {
            try {
                iCarrierMessagingService.filterSms(new MessagePdu(Arrays.asList(this.mPdus)), this.mSmsFormat, this.mDestPort, CarrierServicesSmsFilter.this.mPhone.getSubId(), this.mSmsFilterCallback);
            } catch (RemoteException e) {
                CarrierServicesSmsFilter.this.loge("Exception filtering the SMS: " + e);
                this.mSmsFilterCallback.onFilterComplete(0);
            }
        }
    }

    private final class CarrierSmsFilterCallback extends ICarrierMessagingCallback.Stub {
        private final CarrierMessagingServiceManager mCarrierMessagingServiceManager;
        private final FilterAggregator mFilterAggregator;

        CarrierSmsFilterCallback(FilterAggregator filterAggregator, CarrierMessagingServiceManager carrierMessagingServiceManager) {
            this.mFilterAggregator = filterAggregator;
            this.mCarrierMessagingServiceManager = carrierMessagingServiceManager;
        }

        public void onFilterComplete(int i) {
            this.mCarrierMessagingServiceManager.disposeConnection(CarrierServicesSmsFilter.this.mContext);
            this.mFilterAggregator.onFilterComplete(i);
        }

        public void onSendSmsComplete(int i, int i2) {
            CarrierServicesSmsFilter.this.loge("Unexpected onSendSmsComplete call with result: " + i);
        }

        public void onSendMultipartSmsComplete(int i, int[] iArr) {
            CarrierServicesSmsFilter.this.loge("Unexpected onSendMultipartSmsComplete call with result: " + i);
        }

        public void onSendMmsComplete(int i, byte[] bArr) {
            CarrierServicesSmsFilter.this.loge("Unexpected onSendMmsComplete call with result: " + i);
        }

        public void onDownloadMmsComplete(int i) {
            CarrierServicesSmsFilter.this.loge("Unexpected onDownloadMmsComplete call with result: " + i);
        }
    }

    private final class FilterAggregator {
        private final Object mFilterLock = new Object();
        private int mFilterResult = 0;
        private int mNumPendingFilters;

        FilterAggregator(int i) {
            this.mNumPendingFilters = i;
        }

        void onFilterComplete(int i) {
            synchronized (this.mFilterLock) {
                this.mNumPendingFilters--;
                combine(i);
                if (this.mNumPendingFilters == 0) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        CarrierServicesSmsFilter.this.mCarrierServicesSmsFilterCallback.onFilterComplete(this.mFilterResult);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
            }
        }

        private void combine(int i) {
            this.mFilterResult = i | this.mFilterResult;
        }
    }
}
