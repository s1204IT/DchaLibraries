package com.mediatek.contacts.list.service;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.R;
import com.android.contacts.vcard.ProcessorBase;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimServiceUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.util.TimingStatistics;
import com.mediatek.internal.telephony.phb.IMtkIccPhoneBook;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class DeleteProcessor extends ProcessorBase {
    private volatile boolean mIsCanceled;
    private volatile boolean mIsDone;
    private volatile boolean mIsRunning;
    private final int mJobId;
    private final MultiChoiceHandlerListener mListener;
    private final List<MultiChoiceRequest> mRequests;
    private final ContentResolver mResolver;
    private final MultiChoiceService mService;
    private PowerManager.WakeLock mWakeLock;

    public DeleteProcessor(MultiChoiceService multiChoiceService, MultiChoiceHandlerListener multiChoiceHandlerListener, List<MultiChoiceRequest> list, int i) {
        Log.i("DeleteProcessor", "[DeleteProcessor]new.");
        this.mService = multiChoiceService;
        this.mResolver = this.mService.getContentResolver();
        this.mListener = multiChoiceHandlerListener;
        this.mRequests = list;
        this.mJobId = i;
        this.mWakeLock = ((PowerManager) this.mService.getApplicationContext().getSystemService("power")).newWakeLock(536870918, "DeleteProcessor");
    }

    @Override
    public synchronized boolean cancel(boolean z) {
        Log.i("DeleteProcessor", "[cancel]mIsDone = " + this.mIsDone + ",mIsCanceled = " + this.mIsCanceled + ",mIsRunning = " + this.mIsRunning);
        if (!this.mIsDone && !this.mIsCanceled) {
            this.mIsCanceled = true;
            if (!this.mIsRunning) {
                this.mService.handleFinishNotification(this.mJobId, false);
                this.mListener.onCanceled(2, this.mJobId, -1, -1, -1);
            } else {
                this.mService.handleFinishNotification(this.mJobId, false);
                this.mListener.onCanceling(2, this.mJobId);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getType() {
        return 2;
    }

    @Override
    public synchronized boolean isCancelled() {
        return this.mIsCanceled;
    }

    @Override
    public synchronized boolean isDone() {
        return this.mIsDone;
    }

    @Override
    public void run() {
        Log.i("DeleteProcessor", "[run].");
        try {
            this.mIsRunning = true;
            this.mWakeLock.acquire();
            Process.setThreadPriority(19);
            runInternal();
            synchronized (this) {
                this.mIsDone = true;
            }
            if (this.mWakeLock == null || !this.mWakeLock.isHeld()) {
                return;
            }
            this.mWakeLock.release();
        } catch (Throwable th) {
            synchronized (this) {
                this.mIsDone = true;
                if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
                    this.mWakeLock.release();
                }
                throw th;
            }
        }
    }

    private void runInternal() {
        int i;
        char c;
        Uri iccProviderUri;
        if (isCancelled()) {
            Log.i("DeleteProcessor", "[runInternal]Canceled before actually handling");
            return;
        }
        int size = this.mRequests.size();
        if (size > 1551) {
            i = 50;
            Log.i("DeleteProcessor", "[runInternal]iBatchDel = 50");
        } else {
            i = 100;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        ArrayList<Long> arrayList = new ArrayList<>();
        SubInfoUtils.getInvalidSubId();
        HashMap map = new HashMap();
        TimingStatistics timingStatistics = new TimingStatistics(DeleteProcessor.class.getSimpleName());
        TimingStatistics timingStatistics2 = new TimingStatistics(DeleteProcessor.class.getSimpleName());
        Iterator<MultiChoiceRequest> it = this.mRequests.iterator();
        int i2 = i;
        boolean z = true;
        int i3 = 0;
        boolean z2 = false;
        int i4 = 0;
        int i5 = 0;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            MultiChoiceRequest next = it.next();
            if (this.mIsCanceled) {
                Log.d("DeleteProcessor", "[runInternal] run: mCanceled = true, break looper");
                break;
            }
            int i6 = i3 + 1;
            long j = jCurrentTimeMillis;
            int i7 = i2;
            boolean z3 = z;
            Iterator<MultiChoiceRequest> it2 = it;
            this.mListener.onProcessed(2, this.mJobId, i6, size, next.mContactName);
            Log.d("DeleteProcessor", "[runInternal]Indicator: " + next.mIndicator);
            if (next.mIndicator > 0) {
                int i8 = next.mIndicator;
                if (z2) {
                    i2 = i7;
                } else {
                    if (!SimCardUtils.isPhoneBookReady(i8)) {
                        Log.d("DeleteProcessor", "[runInternal]phb not ready, skip all of sim contacts");
                        MtkToast.toastFromNoneUiThread(R.string.icc_phone_book_invalid);
                    } else if (SimServiceUtils.isServiceRunning(this.mService.getApplicationContext(), i8)) {
                        Log.d("DeleteProcessor", "[runInternal]sim service is running, skip all of sim contacts");
                        MtkToast.toastFromNoneUiThread(R.string.phone_book_busy);
                    } else {
                        if (map.containsKey(Integer.valueOf(i8))) {
                            iccProviderUri = (Uri) map.get(Integer.valueOf(i8));
                        } else {
                            iccProviderUri = SubInfoUtils.getIccProviderUri(i8);
                            map.put(Integer.valueOf(i8), iccProviderUri);
                        }
                        String str = "index = " + next.mSimIndex;
                        timingStatistics.timingStart();
                        int iDelete = this.mResolver.delete(iccProviderUri, str, null);
                        timingStatistics.timingEnd();
                        if (iDelete > 0) {
                            i4++;
                            arrayList.add(Long.valueOf(next.mContactId));
                        } else if (isReadyForDelete(i8) && isAdnReady(i8) && isSimContactDisappear(iccProviderUri, next.mSimIndex) && SimCardUtils.isPhoneBookReady(i8)) {
                            i4++;
                            arrayList.add(Long.valueOf(next.mContactId));
                            Log.w("DeleteProcessor", "[runInternal]handle as delete success: " + next.mSimIndex);
                        } else {
                            z3 = false;
                        }
                    }
                    i2 = i7;
                    z2 = true;
                }
                it = it2;
                i3 = i6;
                jCurrentTimeMillis = j;
                z = false;
            } else {
                i4++;
                arrayList.add(Long.valueOf(next.mContactId));
            }
            z = z3;
            if (arrayList.size() >= i7) {
                timingStatistics2.timingStart();
                actualBatchDelete(arrayList);
                timingStatistics2.timingEnd();
                StringBuilder sb = new StringBuilder();
                sb.append("[runInternal]the ");
                int i9 = i5 + 1;
                sb.append(i9);
                sb.append(",iBatchDel = ");
                sb.append(i7);
                Log.i("DeleteProcessor", sb.toString());
                arrayList.clear();
                c = 1551;
                if (size - i6 <= 1551) {
                    i5 = i9;
                    i2 = 100;
                    it = it2;
                    i3 = i6;
                    jCurrentTimeMillis = j;
                } else {
                    i5 = i9;
                }
            } else {
                c = 1551;
            }
            i2 = i7;
            it = it2;
            i3 = i6;
            jCurrentTimeMillis = j;
        }
        boolean z4 = z;
        long j2 = jCurrentTimeMillis;
        if (arrayList.size() > 0) {
            timingStatistics2.timingStart();
            actualBatchDelete(arrayList);
            timingStatistics2.timingEnd();
            arrayList.clear();
        }
        Log.d("DeleteProcessor", "[runInternal]totaltime: " + (System.currentTimeMillis() - j2));
        if (this.mIsCanceled) {
            Log.d("DeleteProcessor", "[runInternal]run: mCanceled = true, return");
            this.mService.handleFinishNotification(this.mJobId, false);
            this.mListener.onCanceled(2, this.mJobId, size, i4, size - i4);
            return;
        }
        this.mService.handleFinishNotification(this.mJobId, z4);
        if (z4) {
            this.mListener.onFinished(2, this.mJobId, size);
        } else {
            this.mListener.onFailed(2, this.mJobId, size, i4, size - i4);
        }
        timingStatistics.log("runInternal():IccProviderTiming");
        timingStatistics2.log("runInternal():ContactsProviderTiming");
    }

    private boolean isSimContactDisappear(Uri uri, int i) {
        Cursor cursorQuery = this.mResolver.query(uri, new String[]{"index"}, null, null, null);
        if (cursorQuery == null) {
            Log.e("DeleteProcessor", "[isSimContactDisappear] Cursor is null");
            return false;
        }
        boolean z = true;
        while (cursorQuery.moveToNext()) {
            try {
                if (cursorQuery.getLong(0) == i) {
                    z = false;
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        Log.d("DeleteProcessor", "[isSimContactDisappear]ret=" + z + ", index=" + i + "simUri:" + uri);
        return z;
    }

    private boolean isAdnReady(int i) {
        IMtkIccPhoneBook iMtkIccPhoneBookAsInterface = IMtkIccPhoneBook.Stub.asInterface(ServiceManager.getService(SubInfoUtils.getMtkPhoneBookServiceName()));
        boolean zIsAdnAccessible = false;
        try {
            if (iMtkIccPhoneBookAsInterface == null) {
                Log.e("DeleteProcessor", "[isAdnReady] iIccPhb is null");
            } else {
                zIsAdnAccessible = iMtkIccPhoneBookAsInterface.isAdnAccessible(i);
            }
        } catch (RemoteException e) {
            Log.e("DeleteProcessor", "[isAdnReady]Exception happened", e);
        }
        Log.d("DeleteProcessor", "[isAdnReady] isAdnAccessible:" + zIsAdnAccessible + ", subId=" + i);
        return zIsAdnAccessible;
    }

    private int actualBatchDelete(ArrayList<Long> arrayList) {
        Log.d("DeleteProcessor", "[actualBatchDelete]");
        if (arrayList == null || arrayList.size() == 0) {
            Log.w("DeleteProcessor", "[actualBatchDelete]input error,contactIdList = " + arrayList);
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList2 = new ArrayList();
        String[] strArr = new String[arrayList.size()];
        Iterator<Long> it = arrayList.iterator();
        while (it.hasNext()) {
            arrayList2.add(String.valueOf(it.next().longValue()));
        }
        Arrays.fill(strArr, "?");
        sb.append("_id IN (");
        sb.append(TextUtils.join(",", strArr));
        sb.append(")");
        int iDelete = this.mResolver.delete(ContactsContract.Contacts.CONTENT_URI.buildUpon().appendQueryParameter("batch", "true").build(), sb.toString(), (String[]) arrayList2.toArray(new String[0]));
        Log.d("DeleteProcessor", "[actualBatchDelete]deleteCount:" + iDelete + " Contacts");
        return iDelete;
    }

    private boolean isReadyForDelete(int i) {
        return SimCardUtils.isSimStateIdle(this.mService.getApplicationContext(), i);
    }
}
