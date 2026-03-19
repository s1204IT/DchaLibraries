package com.mediatek.contacts.simcontact;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.telephony.SubscriptionInfo;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class PhbInfoUtils {
    private static HashMap<Integer, PhbInfoWrapper> mActiveUsimPhbInfoMap = null;
    private static Executor sRefreshExecutor = Executors.newSingleThreadExecutor();

    private static final class PhbInfoWrapper {
        private boolean mHasSne;
        private boolean mInitialized;
        private int mSubId;
        private int mUsimAasCount;
        private int mUsimAasMaxNameLength;
        private int mUsimAnrCount;
        private int mUsimEmailCount;
        private int mUsimGroupCount;
        private int mUsimGroupMaxNameLength;
        private int mUsimSneMaxNameLength;
        private final long mStartTime = SystemClock.elapsedRealtime();
        private final Handler mUiHandler = new Handler(Looper.getMainLooper());

        public PhbInfoWrapper(int i) {
            this.mSubId = SubInfoUtils.getInvalidSubId();
            this.mSubId = i;
            resetPhbInfo();
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                refreshPhbInfo();
            }
        }

        private void resetPhbInfo() {
            this.mUsimGroupMaxNameLength = -1;
            this.mUsimGroupCount = -1;
            this.mUsimAnrCount = -1;
            this.mUsimEmailCount = -1;
            this.mInitialized = false;
            this.mUsimAasCount = -1;
            this.mUsimAasMaxNameLength = -1;
            this.mUsimSneMaxNameLength = -1;
            this.mHasSne = false;
        }

        private void refreshPhbInfo() {
            Log.i("PhbInfoUtils", "[refreshPhbInfo]refreshing phb info for subId: " + this.mSubId);
            if (!SimCardUtils.isPhoneBookReady(this.mSubId)) {
                Log.e("PhbInfoUtils", "[refreshPhbInfo]phb not ready, refresh aborted. slot: " + this.mSubId);
                this.mInitialized = false;
                return;
            }
            if (!SimCardUtils.isUsimOrCsimType(this.mSubId)) {
                Log.i("PhbInfoUtils", "[refreshPhbInfo]not usim phb, nothing to refresh, keep default , subId: " + this.mSubId);
                this.mInitialized = true;
                return;
            }
            if (!this.mInitialized) {
                Log.d("PhbInfoUtils", "[refreshPhbInfo] schedule GetSimInfoTask");
                new GetSimInfoTask(this).executeOnExecutor(PhbInfoUtils.sRefreshExecutor, Integer.valueOf(this.mSubId));
            }
        }

        private void refreshPhbInfoDelayed() {
            if (SystemClock.elapsedRealtime() - this.mStartTime <= 120000) {
                this.mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PhbInfoWrapper.this.refreshPhbInfo();
                    }
                }, 5000L);
            }
        }

        private int getUsimGroupMaxNameLength() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[getUsimGroupMaxNameLength] subId = " + this.mSubId + ",length = " + this.mUsimGroupMaxNameLength);
            return this.mUsimGroupMaxNameLength;
        }

        private int getUsimAnrCount() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[getUsimAnrCount] subId = " + this.mSubId + ", count = " + this.mUsimAnrCount);
            return this.mUsimAnrCount;
        }

        private int getUsimEmailCount() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[getUsimEmailCount] subId = " + this.mSubId + ", count = " + this.mUsimEmailCount);
            return this.mUsimEmailCount;
        }

        private int getUsimAasCount() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[getUsimAasCount] subId = " + this.mSubId + ", count = " + this.mUsimAasCount);
            return this.mUsimAasCount;
        }

        private boolean usimHasSne() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[usimHasSne] subId = " + this.mSubId + ", mHasSne = " + this.mHasSne);
            return this.mHasSne;
        }

        private int getUsimAasMaxNameLength() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[getUsimAasMaxNameLength] subId = " + this.mSubId + ", length = " + this.mUsimAasMaxNameLength);
            return this.mUsimAasMaxNameLength;
        }

        private int getUsimSneMaxNameLength() {
            if (!this.mInitialized) {
                refreshPhbInfo();
            }
            Log.d("PhbInfoUtils", "[getUsimSneMaxNameLength] subId = " + this.mSubId + ", length = " + this.mUsimSneMaxNameLength);
            return this.mUsimSneMaxNameLength;
        }

        private boolean isInitialized() {
            return this.mInitialized;
        }
    }

    private static final class GetSimInfoTask extends AsyncTask<Integer, Void, PhbInfoWrapper> {
        private PhbInfoWrapper mPhbInfoWrapper;

        public GetSimInfoTask(PhbInfoWrapper phbInfoWrapper) {
            this.mPhbInfoWrapper = phbInfoWrapper;
        }

        @Override
        protected PhbInfoWrapper doInBackground(Integer... numArr) {
            int iIntValue = numArr[0].intValue();
            Log.d("PhbInfoUtils", "[GetSimInfoTask] subId = " + iIntValue);
            try {
                IMtkIccPhoneBook iMtkIccPhoneBookAsInterface = IMtkIccPhoneBook.Stub.asInterface(ServiceManager.getService(SubInfoUtils.getMtkPhoneBookServiceName()));
                if (iMtkIccPhoneBookAsInterface == null) {
                    Log.e("PhbInfoUtils", "[GetSimInfoTask] IIccPhoneBook is null!");
                    this.mPhbInfoWrapper.mInitialized = false;
                    return null;
                }
                this.mPhbInfoWrapper.mUsimGroupMaxNameLength = iMtkIccPhoneBookAsInterface.getUsimGrpMaxNameLen(iIntValue);
                this.mPhbInfoWrapper.mUsimGroupCount = iMtkIccPhoneBookAsInterface.getUsimGrpMaxCount(iIntValue);
                this.mPhbInfoWrapper.mUsimAnrCount = iMtkIccPhoneBookAsInterface.getAnrCount(iIntValue);
                this.mPhbInfoWrapper.mUsimEmailCount = iMtkIccPhoneBookAsInterface.getEmailCount(iIntValue);
                this.mPhbInfoWrapper.mHasSne = iMtkIccPhoneBookAsInterface.hasSne(iIntValue);
                this.mPhbInfoWrapper.mUsimAasCount = iMtkIccPhoneBookAsInterface.getUsimAasMaxCount(iIntValue);
                this.mPhbInfoWrapper.mUsimAasMaxNameLength = iMtkIccPhoneBookAsInterface.getUsimAasMaxNameLen(iIntValue);
                this.mPhbInfoWrapper.mUsimSneMaxNameLength = iMtkIccPhoneBookAsInterface.getSneRecordLen(iIntValue);
                if (-1 == this.mPhbInfoWrapper.mUsimGroupMaxNameLength || -1 == this.mPhbInfoWrapper.mUsimGroupCount || -1 == this.mPhbInfoWrapper.mUsimAnrCount || -1 == this.mPhbInfoWrapper.mUsimEmailCount || -1 == this.mPhbInfoWrapper.mUsimAasCount || -1 == this.mPhbInfoWrapper.mUsimAasMaxNameLength || -1 == this.mPhbInfoWrapper.mUsimSneMaxNameLength) {
                    this.mPhbInfoWrapper.mInitialized = false;
                    Log.d("PhbInfoUtils", "[GetSimInfoTask] Initialize = false. Not all info ready,still need refresh next time");
                    this.mPhbInfoWrapper.refreshPhbInfoDelayed();
                } else {
                    this.mPhbInfoWrapper.mInitialized = true;
                    Log.d("PhbInfoUtils", "[GetSimInfoTask] Initialize = true");
                }
                Log.i("PhbInfoUtils", "[GetSimInfoTask]refreshing done,UsimGroupMaxNameLenght = " + this.mPhbInfoWrapper.mUsimGroupMaxNameLength + ", UsimGroupMaxCount = " + this.mPhbInfoWrapper.mUsimGroupCount + ", UsimAnrCount = " + this.mPhbInfoWrapper.mUsimAnrCount + ", UsimEmailCount = " + this.mPhbInfoWrapper.mUsimEmailCount + ", mHasSne = " + this.mPhbInfoWrapper.mHasSne + ", mUsimAasMaxCount = " + this.mPhbInfoWrapper.mUsimAasCount + ", mUsimAasMaxNameLength = " + this.mPhbInfoWrapper.mUsimAasMaxNameLength + ", mUsimSneMaxNameLength = " + this.mPhbInfoWrapper.mUsimSneMaxNameLength);
                return this.mPhbInfoWrapper;
            } catch (RemoteException e) {
                Log.e("PhbInfoUtils", "[GetSimInfoTask]Exception happened when refreshing phb info");
                e.printStackTrace();
                this.mPhbInfoWrapper.mInitialized = false;
                return null;
            }
        }
    }

    public static void clearActiveUsimPhbInfoMap() {
        Log.d("PhbInfoUtils", "clearActiveUsimPhbInfoMap");
        mActiveUsimPhbInfoMap = null;
    }

    public static HashMap<Integer, PhbInfoWrapper> getActiveUsimPhbInfoMap() {
        if (mActiveUsimPhbInfoMap == null) {
            mActiveUsimPhbInfoMap = new HashMap<>();
            List<SubscriptionInfo> activatedSubInfoList = SubInfoUtils.getActivatedSubInfoList();
            Log.sensitive("PhbInfoUtils", "[getActiveUsimPhbInfoMap] subscriptionInfoList: " + activatedSubInfoList);
            if (activatedSubInfoList != null && activatedSubInfoList.size() > 0) {
                for (SubscriptionInfo subscriptionInfo : activatedSubInfoList) {
                    mActiveUsimPhbInfoMap.put(Integer.valueOf(subscriptionInfo.getSubscriptionId()), new PhbInfoWrapper(subscriptionInfo.getSubscriptionId()));
                }
            }
        }
        return mActiveUsimPhbInfoMap;
    }

    public static void refreshActiveUsimPhbInfoMap(Boolean bool, Integer num) {
        Log.i("PhbInfoUtils", "[refreshActiveUsimPhbInfoMap] subId: " + num + ", isPhbReady: " + bool + ",mActiveUsimPhbInfoMap: " + mActiveUsimPhbInfoMap);
        if (mActiveUsimPhbInfoMap == null) {
            getActiveUsimPhbInfoMap();
            Log.i("PhbInfoUtils", "[refreshActiveUsimPhbInfoMap] get all PhbInfoMap done,,mActiveUsimPhbInfoMap: " + mActiveUsimPhbInfoMap);
            return;
        }
        if (num.intValue() < 0) {
            Log.d("PhbInfoUtils", "refreshActiveUsimPhbInfoMap subId wrong");
            return;
        }
        if (bool.booleanValue()) {
            Log.d("PhbInfoUtils", "[refreshActiveUsimPhbInfoMap] phb ready, put subId = " + num);
            mActiveUsimPhbInfoMap.put(num, new PhbInfoWrapper(num.intValue()));
            return;
        }
        Log.d("PhbInfoUtils", "[refreshActiveUsimPhbInfoMap] phb not ready, try to remove subId:" + num);
        if (mActiveUsimPhbInfoMap.containsKey(num)) {
            Log.d("PhbInfoUtils", "[refreshActiveUsimPhbInfoMap] remove subId: " + num);
            mActiveUsimPhbInfoMap.remove(num);
        }
    }

    public static int getUsimGroupMaxNameLength(int i) {
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper != null) {
            return phbInfoWrapper.getUsimGroupMaxNameLength();
        }
        return -1;
    }

    public static int getUsimAnrCount(int i) {
        int usimAnrCount;
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper == null || -1 == (usimAnrCount = phbInfoWrapper.getUsimAnrCount())) {
            return 0;
        }
        return usimAnrCount;
    }

    public static int getUsimEmailCount(int i) {
        int usimEmailCount;
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper == null || -1 == (usimEmailCount = phbInfoWrapper.getUsimEmailCount())) {
            return 0;
        }
        return usimEmailCount;
    }

    public static int getUsimAasCount(int i) {
        int usimAasCount;
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper == null || -1 == (usimAasCount = phbInfoWrapper.getUsimAasCount())) {
            return 0;
        }
        return usimAasCount;
    }

    public static boolean usimHasSne(int i) {
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper != null) {
            return phbInfoWrapper.usimHasSne();
        }
        return false;
    }

    public static int getUsimAasMaxNameLength(int i) {
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper != null) {
            return phbInfoWrapper.getUsimAasMaxNameLength();
        }
        return -1;
    }

    public static int getUsimSneMaxNameLength(int i) {
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper != null) {
            return phbInfoWrapper.getUsimSneMaxNameLength();
        }
        return -1;
    }

    public static boolean isInitialized(int i) {
        PhbInfoWrapper phbInfoWrapper = getActiveUsimPhbInfoMap().get(Integer.valueOf(i));
        if (phbInfoWrapper != null) {
            return phbInfoWrapper.isInitialized();
        }
        return false;
    }
}
