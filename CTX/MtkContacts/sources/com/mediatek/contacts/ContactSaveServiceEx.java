package com.mediatek.contacts;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class ContactSaveServiceEx {
    private static Handler mMainHandler = new Handler(Looper.getMainLooper());

    private static void showToast(final int i) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsApplicationEx.getContactsApplication(), i, 1).show();
            }
        });
    }

    public static void bufferOperations(ArrayList<ContentProviderOperation> arrayList, ContentResolver contentResolver) {
        try {
            Log.d("ContactSaveServiceEx", "[bufferOperatation] begin applyBatch ");
            contentResolver.applyBatch("com.android.contacts", arrayList);
            Log.d("ContactSaveServiceEx", "[bufferOperatation] end applyBatch");
            arrayList.clear();
        } catch (OperationApplicationException e) {
            Log.e("ContactSaveServiceEx", "[bufferOperatation]OperationApplicationException:", e);
            showToast(R.string.contactSavedErrorToast);
        } catch (RemoteException e2) {
            Log.e("ContactSaveServiceEx", "[bufferOperatation]RemoteException:", e2);
            showToast(R.string.contactSavedErrorToast);
        }
    }

    public static boolean containSimContact(long[] jArr, ContentResolver contentResolver) {
        String[] strArr = {"index_in_sim"};
        StringBuilder sb = new StringBuilder();
        sb.append("contact_id IN (");
        for (int i = 0; i < jArr.length; i++) {
            sb.append(String.valueOf(jArr[i]));
            if (i < jArr.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        Cursor cursorQuery = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, strArr, sb.toString(), null, null, null);
        if (cursorQuery != null) {
            do {
                try {
                    if (!cursorQuery.moveToNext()) {
                        return false;
                    }
                } finally {
                    cursorQuery.close();
                }
            } while (cursorQuery.getLong(0) <= 0);
            Log.d("ContactSaveServiceEx", "[containSimContact]return true, index_in_sim=" + cursorQuery.getLong(0));
            return true;
        }
        Log.e("ContactSaveServiceEx", "[containSimContact] query fail, cursor is null!");
        return false;
    }

    public static void refreshPhotoCache(long j) {
        Cursor cursorQuery;
        Context applicationContext = GlobalEnv.getApplicationContext();
        try {
            cursorQuery = applicationContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[]{"_id"}, "raw_contact_id=? AND mimetype=?", new String[]{String.valueOf(j), "vnd.android.cursor.item/photo"}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() == 1) {
                        cursorQuery.moveToPosition(-1);
                        cursorQuery.moveToNext();
                        long j2 = cursorQuery.getLong(0);
                        Log.d("ContactSaveServiceEx", "[refreshPhotoCache] rawContactId = " + j + ", photoId = " + j2);
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        ContactPhotoManager.getInstance(applicationContext).refreshCacheByKey(Long.valueOf(j2));
                        return;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    throw th;
                }
            }
            Log.w("ContactSaveServiceEx", "[refreshPhotoCache] return for cursor is null or count is not 1");
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = null;
        }
    }
}
