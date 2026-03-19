package com.mediatek.contacts.list.service;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.vcard.ProcessorBase;
import com.google.common.collect.Lists;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.SubContactsUtils;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimServiceUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.TimingStatistics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CopyProcessor extends ProcessorBase {
    private static final String[] DATA_ALLCOLUMNS;
    private static final String[] DATA_ALLCOLUMNS_INTERNAL = {"_id", "mimetype", "is_primary", "is_super_primary", "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10", "data11", "data12", "data13", "data14", "data15", "data_sync1", "data_sync2", "data_sync3", "data_sync4"};
    private final AccountWithDataSet mAccountDst;
    private final AccountWithDataSet mAccountSrc;
    private volatile boolean mIsCanceled;
    private volatile boolean mIsDone;
    private volatile boolean mIsRunning;
    private final int mJobId;
    private final MultiChoiceHandlerListener mListener;
    private final List<MultiChoiceRequest> mRequests;
    private final ContentResolver mResolver;
    private final MultiChoiceService mService;
    private PowerManager.WakeLock mWakeLock;

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(DATA_ALLCOLUMNS_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("is_additional_number");
        }
        DATA_ALLCOLUMNS = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public CopyProcessor(MultiChoiceService multiChoiceService, MultiChoiceHandlerListener multiChoiceHandlerListener, List<MultiChoiceRequest> list, int i, AccountWithDataSet accountWithDataSet, AccountWithDataSet accountWithDataSet2) {
        Log.i("CopyProcessor", "[CopyProcessor]new.");
        this.mService = multiChoiceService;
        this.mResolver = this.mService.getContentResolver();
        this.mListener = multiChoiceHandlerListener;
        this.mRequests = list;
        this.mJobId = i;
        this.mAccountSrc = accountWithDataSet;
        this.mAccountDst = accountWithDataSet2;
        this.mWakeLock = ((PowerManager) this.mService.getApplicationContext().getSystemService("power")).newWakeLock(536870918, "CopyProcessor");
    }

    @Override
    public synchronized boolean cancel(boolean z) {
        Log.i("CopyProcessor", "[cancel]mIsDone=" + this.mIsDone + ",mCanceled=" + this.mIsCanceled + ",mIsRunning = " + this.mIsRunning);
        if (!this.mIsDone && !this.mIsCanceled) {
            this.mIsCanceled = true;
            if (!this.mIsRunning) {
                this.mService.handleFinishNotification(this.mJobId, false);
                this.mListener.onCanceled(1, this.mJobId, -1, -1, -1);
            }
            return true;
        }
        return false;
    }

    @Override
    public int getType() {
        return 1;
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
        Log.i("CopyProcessor", "[run]");
        try {
            this.mIsRunning = true;
            this.mWakeLock.acquire();
            if (AccountTypeUtils.isAccountTypeIccCard(this.mAccountDst.type)) {
                copyContactsToSimWithRadioStateCheck();
            } else {
                copyContactsToAccount();
            }
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

    private void copyContactsToSimWithRadioStateCheck() {
        if (this.mIsCanceled) {
            Log.w("CopyProcessor", "[copyContactsToSimWithRadioCheck]mIsCanceled is true,return.");
            return;
        }
        AccountWithDataSetEx accountWithDataSetEx = (AccountWithDataSetEx) this.mAccountDst;
        Log.d("CopyProcessor", "[copyContactsToSimWithRadioCheck]AccountName: " + Log.anonymize(accountWithDataSetEx.name) + " | accountType: " + accountWithDataSetEx.type);
        int subId = accountWithDataSetEx.getSubId();
        if (!isPhoneBookReady(subId)) {
            int i = 0;
            while (true) {
                int i2 = i + 1;
                if (i >= 20) {
                    break;
                }
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isPhoneBookReady(subId)) {
                    break;
                } else {
                    i = i2;
                }
            }
        }
        if (!isPhoneBookReady(subId)) {
            this.mService.handleFinishNotification(this.mJobId, false);
            this.mListener.onFailed(1, this.mJobId, this.mRequests.size(), 0, this.mRequests.size(), 3);
        } else {
            copyContactsToSim();
        }
    }

    private void copyContactsToAccount() {
        int count;
        boolean z;
        int i;
        Log.d("CopyProcessor", "[copyContactsToAccount]mIsCanceled = " + this.mIsCanceled);
        if (this.mIsCanceled) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        Iterator<MultiChoiceRequest> it = this.mRequests.iterator();
        while (it.hasNext()) {
            sb.append(String.valueOf(it.next().mContactId));
            sb.append(",");
        }
        int i2 = 1;
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        Log.d("CopyProcessor", "[copyContactsToAccount]contactIds " + sb.toString() + " ");
        TimingStatistics timingStatistics = new TimingStatistics(CopyProcessor.class.getSimpleName());
        timingStatistics.timingStart();
        Cursor cursorQuery = this.mResolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"_id", "display_name"}, "contact_id IN " + sb.toString(), null, null);
        timingStatistics.timingEnd();
        int i3 = 0;
        if (cursorQuery != null) {
            count = cursorQuery.getCount();
        } else {
            count = 0;
        }
        ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
        if (cursorQuery == null) {
            z = false;
            i = 0;
        } else {
            Log.d("CopyProcessor", "[copyContactsToAccount]rawContactsCursor.size = " + cursorQuery.getCount());
            int i4 = 0;
            i = 0;
            while (true) {
                if (!cursorQuery.moveToNext()) {
                    break;
                }
                if (this.mIsCanceled) {
                    Log.d("CopyProcessor", "[copyContactsToAccount]runInternal run: mCanceled = true");
                    break;
                }
                int i5 = i4 + 1;
                this.mListener.onProcessed(1, this.mJobId, i5, count, cursorQuery.getString(i2));
                long j = cursorQuery.getLong(i3);
                ContentResolver contentResolver = this.mResolver;
                Uri uri = ContactsContract.Data.CONTENT_URI;
                String[] strArr = DATA_ALLCOLUMNS;
                String[] strArr2 = new String[i2];
                strArr2[i3] = String.valueOf(j);
                Cursor cursorQuery2 = contentResolver.query(uri, strArr, "raw_contact_id=? ", strArr2, null);
                if (cursorQuery2 != null) {
                    if (cursorQuery2.getCount() <= 0) {
                        Log.d("CopyProcessor", "[copyContactsToAccount]dataCursor is empty");
                        cursorQuery2.close();
                    } else {
                        int size = arrayList.size();
                        ContentProviderOperation.Builder builderNewInsert = ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI);
                        if (!TextUtils.isEmpty(this.mAccountDst.name) && !TextUtils.isEmpty(this.mAccountDst.type)) {
                            builderNewInsert.withValue("account_name", this.mAccountDst.name);
                            builderNewInsert.withValue("account_type", this.mAccountDst.type);
                        } else {
                            builderNewInsert.withValues(new ContentValues());
                        }
                        builderNewInsert.withValue("aggregation_mode", 3);
                        arrayList.add(builderNewInsert.build());
                        cursorQuery2.moveToPosition(-1);
                        String[] columnNames = cursorQuery2.getColumnNames();
                        while (cursorQuery2.moveToNext()) {
                            String string = cursorQuery2.getString(cursorQuery2.getColumnIndex("mimetype"));
                            Log.i("CopyProcessor", "mimeType:" + string);
                            if (!"vnd.android.cursor.item/group_membership".equals(string)) {
                                ContentProviderOperation.Builder builderNewInsert2 = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                                int i6 = size;
                                generateDataBuilder(cursorQuery2, builderNewInsert2, columnNames, string, this.mAccountSrc.type);
                                builderNewInsert2.withValueBackReference("raw_contact_id", i6);
                                arrayList.add(builderNewInsert2.build());
                                size = i6;
                            }
                        }
                        cursorQuery2.close();
                        i++;
                        if (arrayList.size() > 400) {
                            try {
                                Log.d("CopyProcessor", "[copyContactsToAccount]Before applyBatch. ");
                                timingStatistics.timingStart();
                                this.mResolver.applyBatch("com.android.contacts", arrayList);
                                timingStatistics.timingEnd();
                                Log.d("CopyProcessor", "[copyContactsToAccount]After applyBatch ");
                            } catch (OperationApplicationException e) {
                                Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                            } catch (RemoteException e2) {
                                Log.e("CopyProcessor", String.format("%s: %s", e2.toString(), e2.getMessage()));
                            }
                            arrayList.clear();
                        }
                        i4 = i5;
                        i2 = 1;
                        i3 = 0;
                    }
                }
                i4 = i5;
            }
            cursorQuery.close();
            if (arrayList.size() > 0) {
                try {
                    Log.d("CopyProcessor", "[copyContactsToAccount]Before end applyBatch. ");
                    timingStatistics.timingStart();
                    this.mResolver.applyBatch("com.android.contacts", arrayList);
                    timingStatistics.timingEnd();
                    Log.d("CopyProcessor", "[copyContactsToAccount]After end applyBatch ");
                } catch (OperationApplicationException e3) {
                    Log.e("CopyProcessor", String.format("%s: %s", e3.toString(), e3.getMessage()));
                } catch (RemoteException e4) {
                    Log.e("CopyProcessor", String.format("%s: %s", e4.toString(), e4.getMessage()));
                }
                arrayList.clear();
            }
            if (this.mIsCanceled) {
                Log.d("CopyProcessor", "[copyContactsToAccount]runInternal run: mCanceled = true");
                this.mService.handleFinishNotification(this.mJobId, false);
                this.mListener.onCanceled(1, this.mJobId, count, i, count - i);
                if (cursorQuery != null && !cursorQuery.isClosed()) {
                    cursorQuery.close();
                    return;
                }
                return;
            }
            z = false;
        }
        MultiChoiceService multiChoiceService = this.mService;
        int i7 = this.mJobId;
        if (i == count) {
            z = true;
        }
        multiChoiceService.handleFinishNotification(i7, z);
        if (i == count) {
            this.mListener.onFinished(1, this.mJobId, count);
        } else {
            this.mListener.onFailed(1, this.mJobId, count, i, count - i);
        }
        Log.d("CopyProcessor", "[copyContactsToAccount]end");
        timingStatistics.log("copyContactsToAccount():ContactsProviderTiming");
    }

    private void copyContactsToSim() {
        TimingStatistics timingStatistics;
        TimingStatistics timingStatistics2;
        int i;
        ContentResolver contentResolver;
        int i2;
        int i3;
        boolean z;
        TimingStatistics timingStatistics3;
        ArrayList<ContentProviderOperation> arrayList;
        int i4;
        int i5;
        int i6;
        String string;
        Uri uri;
        Uri uri2;
        int i7;
        ArrayList arrayList2;
        ArrayList arrayList3;
        boolean z2;
        String str;
        ArrayList arrayList4;
        boolean z3;
        int i8;
        int i9;
        String str2;
        TimingStatistics timingStatistics4;
        int i10;
        int i11;
        Uri uri3;
        String str3;
        String str4;
        TimingStatistics timingStatistics5;
        String str5;
        ContentResolver contentResolver2;
        int i12;
        ArrayList arrayList5;
        ContentValues contentValues;
        boolean z4;
        String str6;
        int i13;
        TimingStatistics timingStatistics6;
        int i14;
        ArrayList arrayList6;
        ArrayList arrayList7;
        Uri uri4;
        ArrayList<ContentProviderOperation> arrayList8;
        Uri uri5;
        int i15;
        ContentResolver contentResolver3;
        int i16;
        TimingStatistics timingStatistics7;
        boolean z5;
        ArrayList<ContentProviderOperation> arrayList9;
        String str7;
        int i17;
        boolean z6;
        String strReplace;
        int i18;
        String str8;
        int i19;
        TimingStatistics timingStatistics8;
        AccountWithDataSetEx accountWithDataSetEx = (AccountWithDataSetEx) this.mAccountDst;
        int subId = accountWithDataSetEx.getSubId();
        Log.d("CopyProcessor", "[copyContactsToSim] AccountName:" + Log.anonymize(accountWithDataSetEx.name) + ",accountType:" + accountWithDataSetEx.type + ",dstSubId:" + subId);
        boolean zIsUsimOrCsimType = SimCardUtils.isUsimOrCsimType(subId);
        String str9 = zIsUsimOrCsimType ? "USIM" : "SIM";
        Log.d("CopyProcessor", "[copyContactsToSim]dstSimType:" + str9);
        if (!isPhoneBookReady(subId)) {
            this.mService.handleFinishNotification(this.mJobId, false);
            this.mListener.onFailed(1, this.mJobId, this.mRequests.size(), 0, this.mRequests.size(), 3);
            return;
        }
        if (zIsUsimOrCsimType && !PhbInfoUtils.isInitialized(subId) && (PhbInfoUtils.getUsimEmailCount(subId) <= 0 || PhbInfoUtils.getUsimAnrCount(subId) <= 0)) {
            Log.w("CopyProcessor", "[copyContactsToSim]PhbInfo not ready so wait and try again:" + PhbInfoUtils.isInitialized(subId));
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!PhbInfoUtils.isInitialized(subId) && (PhbInfoUtils.getUsimEmailCount(subId) <= 0 || PhbInfoUtils.getUsimAnrCount(subId) <= 0)) {
                this.mService.handleFinishNotification(this.mJobId, false);
                this.mListener.onFailed(1, this.mJobId, this.mRequests.size(), 0, this.mRequests.size(), 3);
                Log.w("CopyProcessor", "[copyContactsToSim]Still fail after try again. init=" + PhbInfoUtils.isInitialized(subId));
                return;
            }
        }
        ArrayList arrayList10 = new ArrayList();
        ArrayList arrayList11 = new ArrayList();
        ArrayList arrayList12 = new ArrayList();
        ContentResolver contentResolver4 = this.mResolver;
        int size = this.mRequests.size();
        int usimEmailCount = PhbInfoUtils.getUsimEmailCount(subId);
        ArrayList<ContentProviderOperation> arrayList13 = new ArrayList<>();
        TimingStatistics timingStatistics9 = new TimingStatistics(CopyProcessor.class.getSimpleName());
        TimingStatistics timingStatistics10 = new TimingStatistics(CopyProcessor.class.getSimpleName());
        TimingStatistics timingStatistics11 = new TimingStatistics(CopyProcessor.class.getSimpleName());
        Iterator<MultiChoiceRequest> it = this.mRequests.iterator();
        int i20 = 0;
        int i21 = 0;
        int i22 = 0;
        boolean z7 = false;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            MultiChoiceRequest next = it.next();
            Iterator<MultiChoiceRequest> it2 = it;
            if (this.mIsCanceled) {
                Log.d("CopyProcessor", "[copyContactsToSim]mIsCanceled is true.");
                break;
            }
            timingStatistics11.timingStart();
            if (!isPhoneBookReady(subId) || SimServiceUtils.isServiceRunning(this.mService.getApplicationContext(), subId)) {
                break;
            }
            timingStatistics11.timingEnd();
            int i23 = i20 + 1;
            String str10 = str9;
            timingStatistics = timingStatistics11;
            TimingStatistics timingStatistics12 = timingStatistics9;
            ArrayList<ContentProviderOperation> arrayList14 = arrayList13;
            i = size;
            this.mListener.onProcessed(1, this.mJobId, i23, size, next.mContactName);
            arrayList10.clear();
            arrayList11.clear();
            arrayList12.clear();
            Uri uriWithAppendedPath = Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, next.mContactId), "data");
            TimingStatistics timingStatistics13 = timingStatistics10;
            timingStatistics13.timingStart();
            Cursor cursorQuery = contentResolver4.query(uriWithAppendedPath, new String[]{"_id", "mimetype", "data1", "is_additional_number", "name_raw_contact_id", "raw_contact_id"}, null, null, null);
            timingStatistics13.timingEnd();
            if (cursorQuery == null || !cursorQuery.moveToFirst()) {
                string = null;
            } else {
                string = null;
                do {
                    String string2 = cursorQuery.getString(1);
                    if ("vnd.android.cursor.item/phone_v2".equals(string2)) {
                        String string3 = cursorQuery.getString(2);
                        if (cursorQuery.getInt(3) == 1) {
                            arrayList11.add(string3);
                        } else {
                            arrayList10.add(string3);
                        }
                    } else if ("vnd.android.cursor.item/name".equals(string2) && cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("name_raw_contact_id")) == cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow("raw_contact_id"))) {
                        string = cursorQuery.getString(2);
                    }
                    if (zIsUsimOrCsimType && "vnd.android.cursor.item/email_v2".equals(string2) && usimEmailCount > 0) {
                        arrayList12.add(cursorQuery.getString(2));
                    }
                } while (cursorQuery.moveToNext());
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            Uri iccProviderUri = SubInfoUtils.getIccProviderUri(subId);
            int i24 = !TextUtils.isEmpty(string) ? 1 : 0;
            int usimAnrCount = PhbInfoUtils.getUsimAnrCount(subId);
            if (zIsUsimOrCsimType) {
                int size2 = arrayList10.size();
                int size3 = arrayList11.size();
                int size4 = arrayList12.size();
                if (i24 <= size3) {
                    i24 = size3;
                }
                if (i24 <= size4) {
                    i24 = size4;
                }
                uri = iccProviderUri;
                uri2 = uriWithAppendedPath;
                i7 = subId;
                double d = 1.0d + ((double) usimAnrCount);
                int i25 = (int) ((((double) (size2 + size3)) / d) + (((double) usimAnrCount) / d));
                Log.d("CopyProcessor", "[copyContactsToSim]maxAnr=" + usimAnrCount + "; numberQuota=" + i25 + ",additionalCount:" + size3);
                if (i24 <= i25) {
                    i24 = i25;
                }
            } else {
                uri = iccProviderUri;
                uri2 = uriWithAppendedPath;
                i7 = subId;
                arrayList10.addAll(arrayList11);
                arrayList11.clear();
                int size5 = arrayList10.size();
                if (i24 <= size5) {
                    i24 = size5;
                }
            }
            int i26 = i24;
            ContentValues contentValues2 = new ContentValues();
            String str11 = TextUtils.isEmpty(string) ? "" : string;
            if ((str11 == null || str11.isEmpty() || str11.length() == 0) && arrayList10.isEmpty()) {
                Log.e("CopyProcessor", " [copyContactsToSim]name and number are empty");
                timingStatistics10 = timingStatistics13;
                i20 = i23;
                it = it2;
                str9 = str10;
                timingStatistics11 = timingStatistics;
                timingStatistics9 = timingStatistics12;
                arrayList13 = arrayList14;
                size = i;
                subId = i7;
                i22 = 1;
            } else {
                int i27 = i22;
                int i28 = 0;
                int i29 = 0;
                while (i28 < i26) {
                    contentValues2.clear();
                    contentValues2.put("tag", str11);
                    Log.d("CopyProcessor", "[copyContactsToSim] simTag: " + Log.anonymize(str11));
                    if (arrayList10.isEmpty()) {
                        i9 = i28;
                        str2 = null;
                    } else {
                        String str12 = (String) arrayList10.remove(0);
                        String strReplace2 = TextUtils.isEmpty(str12) ? "" : str12.replace("-", "");
                        contentValues2.put("number", PhoneNumberUtils.stripSeparators(strReplace2));
                        StringBuilder sb = new StringBuilder();
                        i9 = i28;
                        sb.append("[copyContactsToSim] number is ");
                        sb.append(Log.anonymize(strReplace2));
                        Log.d("CopyProcessor", sb.toString());
                        str2 = strReplace2;
                    }
                    if (zIsUsimOrCsimType) {
                        Log.d("CopyProcessor", "[copyContactsToSim] copy to USIM");
                        if (arrayList11.isEmpty()) {
                            timingStatistics4 = timingStatistics13;
                            if (arrayList10.isEmpty()) {
                                strReplace = null;
                            } else {
                                int size6 = arrayList10.size() < usimAnrCount ? arrayList10.size() : usimAnrCount;
                                strReplace = null;
                                int i30 = 0;
                                while (i30 < size6) {
                                    String str13 = (String) arrayList10.remove(0);
                                    if (TextUtils.isEmpty(str13)) {
                                        strReplace = "";
                                        i18 = size6;
                                    } else {
                                        i18 = size6;
                                        strReplace = str13.replace("-", "");
                                    }
                                    contentValues2.put("anr", PhoneNumberUtils.stripSeparators(strReplace));
                                    i30++;
                                    size6 = i18;
                                }
                            }
                        } else {
                            int size7 = arrayList11.size() < usimAnrCount ? arrayList11.size() : usimAnrCount;
                            strReplace = null;
                            int i31 = 0;
                            while (i31 < size7) {
                                String str14 = (String) arrayList11.remove(0);
                                if (TextUtils.isEmpty(str14)) {
                                    strReplace = "";
                                    i19 = size7;
                                    timingStatistics8 = timingStatistics13;
                                } else {
                                    i19 = size7;
                                    timingStatistics8 = timingStatistics13;
                                    strReplace = str14.replace("-", "");
                                }
                                contentValues2.put("anr", PhoneNumberUtils.stripSeparators(strReplace));
                                i31++;
                                size7 = i19;
                                timingStatistics13 = timingStatistics8;
                            }
                            timingStatistics4 = timingStatistics13;
                            if (!arrayList11.isEmpty()) {
                                arrayList10.addAll(arrayList11);
                                arrayList11.clear();
                            }
                        }
                        if (arrayList12.isEmpty()) {
                            str4 = strReplace;
                            str8 = null;
                        } else {
                            String str15 = (String) arrayList12.remove(0);
                            str8 = TextUtils.isEmpty(str15) ? "" : str15;
                            contentValues2.put("emails", str8);
                            StringBuilder sb2 = new StringBuilder();
                            str4 = strReplace;
                            sb2.append("[copyContactsToSim] emails is ");
                            sb2.append(Log.anonymize(str8));
                            Log.d("CopyProcessor", sb2.toString());
                        }
                        i10 = i26;
                        i11 = i7;
                        uri3 = uri2;
                        GlobalEnv.getSimAasEditor().updateValuesforCopy(uri3, i11, this.mAccountDst.type, contentValues2);
                        GlobalEnv.getSimSneEditor().updateValuesforCopy(uri3, i11, this.mAccountDst.type, contentValues2);
                        str3 = str8;
                    } else {
                        timingStatistics4 = timingStatistics13;
                        i10 = i26;
                        i11 = i7;
                        uri3 = uri2;
                        str3 = null;
                        str4 = null;
                    }
                    if (!isPhoneBookReady(i11) || SimServiceUtils.isServiceRunning(this.mService.getApplicationContext(), i11)) {
                        contentResolver = contentResolver4;
                        arrayList2 = arrayList12;
                        arrayList4 = arrayList11;
                        i2 = i11;
                        arrayList3 = arrayList10;
                        z2 = zIsUsimOrCsimType;
                        str = str10;
                        timingStatistics2 = timingStatistics12;
                        arrayList = arrayList14;
                        timingStatistics3 = timingStatistics4;
                        i4 = 2;
                        i3 = 1;
                        break;
                    }
                    Log.sensitive("CopyProcessor", "[copyContactsToSim]Before insert Sim card. values=" + contentValues2);
                    TimingStatistics timingStatistics14 = timingStatistics12;
                    timingStatistics14.timingStart();
                    Uri uri6 = uri;
                    Uri uriInsert = contentResolver4.insert(uri6, contentValues2);
                    timingStatistics14.timingEnd();
                    int i32 = usimAnrCount;
                    ArrayList arrayList15 = arrayList12;
                    StringBuilder sb3 = new StringBuilder();
                    ArrayList arrayList16 = arrayList11;
                    sb3.append("[copyContactsToSim]After insert Sim card,retUri = ");
                    sb3.append(uriInsert);
                    Log.i("CopyProcessor", sb3.toString());
                    if (uriInsert != null) {
                        List<String> pathSegments = uriInsert.getPathSegments();
                        ArrayList arrayList17 = arrayList10;
                        if ("error".equals(pathSegments.get(0))) {
                            String str16 = pathSegments.get(1);
                            Log.i("CopyProcessor", "[copyContactsToSim]error code = " + str16);
                            printSimErrorDetails(str16);
                            if (i27 != 6) {
                                i27 = 1;
                            }
                            if ("-3".equals(pathSegments.get(1))) {
                                Log.e("CopyProcessor", "[copyContactsToSim]Fail to insert sim contacts fail because sim storage is full.");
                                timingStatistics2 = timingStatistics14;
                                contentResolver = contentResolver4;
                                i2 = i11;
                                z2 = zIsUsimOrCsimType;
                                str = str10;
                                arrayList = arrayList14;
                                timingStatistics3 = timingStatistics4;
                                arrayList2 = arrayList15;
                                arrayList4 = arrayList16;
                                arrayList3 = arrayList17;
                                i4 = 2;
                                i3 = 1;
                                z5 = false;
                                z7 = true;
                            } else {
                                if ("-12".equals(pathSegments.get(1))) {
                                    Log.e("CopyProcessor", "[copyContactsToSim]Fail to save USIM email  because emial slot is full in USIM.");
                                    Log.d("CopyProcessor", "[copyContactsToSim]Ignore this error and remove the email address to save this item again");
                                    contentValues2.remove("emails");
                                    timingStatistics14.timingStart();
                                    Uri uriInsert2 = contentResolver4.insert(uri6, contentValues2);
                                    timingStatistics14.timingEnd();
                                    Log.d("CopyProcessor", "[copyContactsToSim]The retUri is " + uriInsert2);
                                    if (uriInsert2 == null || !"error".equals(uriInsert2.getPathSegments().get(0))) {
                                        z6 = true;
                                    } else {
                                        z6 = true;
                                        if ("-3".equals(uriInsert2.getPathSegments().get(1))) {
                                            Log.e("CopyProcessor", "[copyContactsToSim]Fail to insert sim contacts fail because sim storage is full.");
                                            timingStatistics2 = timingStatistics14;
                                            contentResolver = contentResolver4;
                                            i3 = 1;
                                            z7 = true;
                                            i2 = i11;
                                            z2 = zIsUsimOrCsimType;
                                            str = str10;
                                            arrayList = arrayList14;
                                            timingStatistics3 = timingStatistics4;
                                            arrayList2 = arrayList15;
                                            arrayList4 = arrayList16;
                                            arrayList3 = arrayList17;
                                            i4 = 2;
                                            z5 = false;
                                        }
                                    }
                                    if (uriInsert2 == null || "error".equals(uriInsert2.getPathSegments().get(0))) {
                                        timingStatistics5 = timingStatistics14;
                                        str5 = str11;
                                        contentResolver2 = contentResolver4;
                                        i12 = i11;
                                        i17 = 6;
                                        contentValues = contentValues2;
                                        z4 = zIsUsimOrCsimType;
                                        arrayList9 = arrayList14;
                                        i13 = i9;
                                        timingStatistics6 = timingStatistics4;
                                        i14 = i32;
                                        arrayList6 = arrayList15;
                                        arrayList7 = arrayList16;
                                        arrayList5 = arrayList17;
                                        uri5 = uri6;
                                        uri4 = uri3;
                                        str7 = str10;
                                    } else {
                                        long id = ContentUris.parseId(uriInsert2);
                                        ArrayList<ContentProviderOperation> arrayList18 = arrayList14;
                                        int size8 = arrayList18.size();
                                        uri5 = uri6;
                                        timingStatistics5 = timingStatistics14;
                                        i14 = i32;
                                        i13 = i9;
                                        str5 = str11;
                                        contentResolver2 = contentResolver4;
                                        arrayList6 = arrayList15;
                                        arrayList7 = arrayList16;
                                        i12 = i11;
                                        i17 = 6;
                                        timingStatistics6 = timingStatistics4;
                                        arrayList5 = arrayList17;
                                        contentValues = contentValues2;
                                        z4 = zIsUsimOrCsimType;
                                        uri4 = uri3;
                                        str7 = str10;
                                        SubContactsUtils.buildInsertOperation(arrayList18, this.mAccountDst, str11, str2, null, str4, contentResolver4, i11, str10, id, null);
                                        arrayList9 = arrayList18;
                                        GlobalEnv.getSimSneEditor().copySimSneToAccount(arrayList9, this.mAccountDst, uri4, size8);
                                        i29++;
                                    }
                                } else {
                                    timingStatistics5 = timingStatistics14;
                                    str5 = str11;
                                    contentResolver2 = contentResolver4;
                                    i12 = i11;
                                    contentValues = contentValues2;
                                    z4 = zIsUsimOrCsimType;
                                    arrayList9 = arrayList14;
                                    i13 = i9;
                                    timingStatistics6 = timingStatistics4;
                                    i14 = i32;
                                    arrayList6 = arrayList15;
                                    arrayList7 = arrayList16;
                                    arrayList5 = arrayList17;
                                    uri5 = uri6;
                                    uri4 = uri3;
                                    str7 = str10;
                                    i17 = i27;
                                }
                                str6 = str7;
                                i15 = i17;
                                arrayList8 = arrayList9;
                            }
                            i8 = -3;
                            z3 = z5;
                            break;
                        }
                        timingStatistics5 = timingStatistics14;
                        str5 = str11;
                        contentResolver2 = contentResolver4;
                        i12 = i11;
                        contentValues = contentValues2;
                        z4 = zIsUsimOrCsimType;
                        ArrayList<ContentProviderOperation> arrayList19 = arrayList14;
                        i13 = i9;
                        timingStatistics6 = timingStatistics4;
                        i14 = i32;
                        arrayList6 = arrayList15;
                        arrayList7 = arrayList16;
                        arrayList5 = arrayList17;
                        uri5 = uri6;
                        uri4 = uri3;
                        String str17 = str10;
                        Log.d("CopyProcessor", "[copyContactsToSim]insertUsimFlag = true");
                        long id2 = ContentUris.parseId(uriInsert);
                        int size9 = arrayList19.size();
                        str6 = str17;
                        arrayList8 = arrayList19;
                        SubContactsUtils.buildInsertOperation(arrayList19, this.mAccountDst, str5, str2, str3, str4, contentResolver4, i12, str17, id2, null);
                        GlobalEnv.getSimSneEditor().copySimSneToAccount(arrayList8, this.mAccountDst, uri4, size9);
                        i29++;
                        i15 = i27;
                    } else {
                        timingStatistics5 = timingStatistics14;
                        str5 = str11;
                        contentResolver2 = contentResolver4;
                        i12 = i11;
                        arrayList5 = arrayList10;
                        contentValues = contentValues2;
                        z4 = zIsUsimOrCsimType;
                        str6 = str10;
                        i13 = i9;
                        timingStatistics6 = timingStatistics4;
                        i14 = i32;
                        arrayList6 = arrayList15;
                        arrayList7 = arrayList16;
                        uri4 = uri3;
                        arrayList8 = arrayList14;
                        uri5 = uri6;
                        i15 = 1;
                    }
                    if (arrayList8.size() > 400) {
                        try {
                            Log.i("CopyProcessor", "[copyContactsToSim]Before applyBatch. ");
                            i16 = i12;
                            try {
                                if (!isPhoneBookReady(i16) || SimServiceUtils.isServiceRunning(this.mService.getApplicationContext(), i16)) {
                                    contentResolver3 = contentResolver2;
                                    timingStatistics7 = timingStatistics6;
                                } else {
                                    timingStatistics7 = timingStatistics6;
                                    try {
                                        timingStatistics7.timingStart();
                                        contentResolver3 = contentResolver2;
                                    } catch (OperationApplicationException e2) {
                                        e = e2;
                                        contentResolver3 = contentResolver2;
                                        Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                                    } catch (RemoteException e3) {
                                        e = e3;
                                        contentResolver3 = contentResolver2;
                                        Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                                    }
                                    try {
                                        contentResolver3.applyBatch("com.android.contacts", arrayList8);
                                        timingStatistics7.timingEnd();
                                    } catch (OperationApplicationException e4) {
                                        e = e4;
                                        Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                                    } catch (RemoteException e5) {
                                        e = e5;
                                        Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                                    }
                                }
                                Log.i("CopyProcessor", "[copyContactsToSim]After applyBatch ");
                            } catch (OperationApplicationException e6) {
                                e = e6;
                                contentResolver3 = contentResolver2;
                                timingStatistics7 = timingStatistics6;
                                Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                                arrayList8.clear();
                                i28 = i13 + 1;
                                i27 = i15;
                                uri2 = uri4;
                                str11 = str5;
                                contentValues2 = contentValues;
                                usimAnrCount = i14;
                                uri = uri5;
                                arrayList10 = arrayList5;
                                arrayList12 = arrayList6;
                                arrayList11 = arrayList7;
                                timingStatistics12 = timingStatistics5;
                                zIsUsimOrCsimType = z4;
                                str10 = str6;
                                i7 = i16;
                                timingStatistics13 = timingStatistics7;
                                arrayList14 = arrayList8;
                                i26 = i10;
                                contentResolver4 = contentResolver3;
                            } catch (RemoteException e7) {
                                e = e7;
                                contentResolver3 = contentResolver2;
                                timingStatistics7 = timingStatistics6;
                                Log.e("CopyProcessor", String.format("%s: %s", e.toString(), e.getMessage()));
                                arrayList8.clear();
                                i28 = i13 + 1;
                                i27 = i15;
                                uri2 = uri4;
                                str11 = str5;
                                contentValues2 = contentValues;
                                usimAnrCount = i14;
                                uri = uri5;
                                arrayList10 = arrayList5;
                                arrayList12 = arrayList6;
                                arrayList11 = arrayList7;
                                timingStatistics12 = timingStatistics5;
                                zIsUsimOrCsimType = z4;
                                str10 = str6;
                                i7 = i16;
                                timingStatistics13 = timingStatistics7;
                                arrayList14 = arrayList8;
                                i26 = i10;
                                contentResolver4 = contentResolver3;
                            }
                        } catch (OperationApplicationException e8) {
                            e = e8;
                            contentResolver3 = contentResolver2;
                            i16 = i12;
                        } catch (RemoteException e9) {
                            e = e9;
                            contentResolver3 = contentResolver2;
                            i16 = i12;
                        }
                        arrayList8.clear();
                    } else {
                        contentResolver3 = contentResolver2;
                        i16 = i12;
                        timingStatistics7 = timingStatistics6;
                    }
                    i28 = i13 + 1;
                    i27 = i15;
                    uri2 = uri4;
                    str11 = str5;
                    contentValues2 = contentValues;
                    usimAnrCount = i14;
                    uri = uri5;
                    arrayList10 = arrayList5;
                    arrayList12 = arrayList6;
                    arrayList11 = arrayList7;
                    timingStatistics12 = timingStatistics5;
                    zIsUsimOrCsimType = z4;
                    str10 = str6;
                    i7 = i16;
                    timingStatistics13 = timingStatistics7;
                    arrayList14 = arrayList8;
                    i26 = i10;
                    contentResolver4 = contentResolver3;
                }
                contentResolver = contentResolver4;
                arrayList2 = arrayList12;
                timingStatistics3 = timingStatistics13;
                arrayList3 = arrayList10;
                z2 = zIsUsimOrCsimType;
                str = str10;
                timingStatistics2 = timingStatistics12;
                arrayList = arrayList14;
                i2 = i7;
                i4 = 2;
                i3 = 1;
                arrayList4 = arrayList11;
                z3 = false;
                i8 = i27;
                if (i29 > 0) {
                    i21++;
                }
                if (z7) {
                    i5 = i21;
                    i6 = i8;
                    z = z3;
                    break;
                }
                timingStatistics10 = timingStatistics3;
                arrayList13 = arrayList;
                i20 = i23;
                size = i;
                arrayList10 = arrayList3;
                arrayList12 = arrayList2;
                arrayList11 = arrayList4;
                i22 = i8;
                timingStatistics9 = timingStatistics2;
                zIsUsimOrCsimType = z2;
                str9 = str;
                subId = i2;
                contentResolver4 = contentResolver;
                it = it2;
                timingStatistics11 = timingStatistics;
            }
        }
        if (arrayList.size() > 0) {
            try {
                Log.i("CopyProcessor", "[copyContactsToSim]Before end applyBatch. ");
                if (isPhoneBookReady(i2) && !SimServiceUtils.isServiceRunning(this.mService.getApplicationContext(), i2)) {
                    timingStatistics3.timingStart();
                    contentResolver.applyBatch("com.android.contacts", arrayList);
                    timingStatistics3.timingEnd();
                }
                Log.i("CopyProcessor", "[copyContactsToSim]After end applyBatch ");
            } catch (OperationApplicationException e10) {
                Object[] objArr = new Object[i4];
                objArr[z ? 1 : 0] = e10.toString();
                objArr[i3] = e10.getMessage();
                Log.e("CopyProcessor", String.format("%s: %s", objArr));
            } catch (RemoteException e11) {
                Object[] objArr2 = new Object[i4];
                objArr2[z ? 1 : 0] = e11.toString();
                objArr2[i3] = e11.getMessage();
                Log.e("CopyProcessor", String.format("%s: %s", objArr2));
            }
            arrayList.clear();
        }
        if (this.mIsCanceled) {
            Log.d("CopyProcessor", "[copyContactsToSim] run: mCanceled = true");
            this.mService.handleFinishNotification(this.mJobId, z);
            int i33 = i;
            this.mListener.onCanceled(1, this.mJobId, i33, i5, i33 - i5);
            return;
        }
        int i34 = i;
        MultiChoiceService multiChoiceService = this.mService;
        int i35 = this.mJobId;
        boolean z8 = z;
        if (i6 == 0) {
            z8 = i3;
        }
        multiChoiceService.handleFinishNotification(i35, z8);
        if (i6 == 0) {
            this.mListener.onFinished(i3, this.mJobId, i34);
        } else {
            this.mListener.onFailed(1, this.mJobId, i34, i5, i34 - i5, i6);
        }
        timingStatistics2.log("copyContactsToSim():IccProviderTiming");
        timingStatistics3.log("copyContactsToSim():ContactsProviderTiming");
        timingStatistics.log("copyContactsToSim():CheckStatusTiming");
    }

    private boolean isPhoneBookReady(int i) {
        boolean zIsPhoneBookReady = SimCardUtils.isPhoneBookReady(i);
        Log.d("CopyProcessor", "[isPhoneBookReady]result= " + zIsPhoneBookReady);
        return zIsPhoneBookReady;
    }

    private void cursorColumnToBuilder(Cursor cursor, String[] strArr, int i, ContentProviderOperation.Builder builder) {
        switch (cursor.getType(i)) {
            case 0:
                return;
            case 1:
                builder.withValue(strArr[i], Long.valueOf(cursor.getLong(i)));
                return;
            case 2:
            default:
                throw new IllegalStateException("Invalid or unhandled data type");
            case 3:
                builder.withValue(strArr[i], cursor.getString(i));
                return;
            case CompatUtils.TYPE_ASSERT:
                builder.withValue(strArr[i], cursor.getBlob(i));
                return;
        }
    }

    private void printSimErrorDetails(String str) {
        int iIntValue = Integer.valueOf(str).intValue();
        if (iIntValue != 6) {
            switch (iIntValue) {
                case -11:
                    Log.d("CopyProcessor", "ERROR ICC ADN LIST NOT EXIST");
                    break;
                case -10:
                    Log.d("CopyProcessor", "ERROR ICC GENERIC FAILURE");
                    break;
                default:
                    switch (iIntValue) {
                        case -6:
                            Log.d("CopyProcessor", "ERROR ICC ANR TOO LONG");
                            break;
                        case -5:
                            Log.d("CopyProcessor", "ERROR ICC PASSWORD ERROR");
                            break;
                        case -4:
                            Log.d("CopyProcessor", "ERROR ICC NOT READY");
                            break;
                        case -3:
                            Log.d("CopyProcessor", "ERROR STORAGE FULL");
                            break;
                        case -2:
                            Log.d("CopyProcessor", "ERROR NAME TOO LONG");
                            break;
                        case -1:
                            Log.d("CopyProcessor", "ERROR PHONE NUMBER TOO LONG");
                            break;
                        default:
                            Log.d("CopyProcessor", "ERROR ICC UNKNOW");
                            break;
                    }
                    break;
            }
        }
        Log.d("CopyProcessor", "ERROR ICC USIM EMAIL LOST");
    }

    private void generateDataBuilder(Cursor cursor, ContentProviderOperation.Builder builder, String[] strArr, String str, String str2) {
        for (int i = 1; i < strArr.length; i++) {
            if (!GlobalEnv.getSimAasEditor().cursorColumnToBuilder(cursor, builder, str2, str, ((AccountWithDataSetEx) this.mAccountDst).getSubId(), i)) {
                cursorColumnToBuilder(cursor, strArr, i, builder);
            }
        }
    }
}
