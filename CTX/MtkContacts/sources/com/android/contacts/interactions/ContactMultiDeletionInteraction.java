package com.android.contacts.interactions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.ContactDisplayUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mediatek.contacts.eventhandler.BaseEventHandlerFragment;
import com.mediatek.contacts.list.service.MultiChoiceHandlerListener;
import com.mediatek.contacts.list.service.MultiChoiceRequest;
import com.mediatek.contacts.list.service.MultiChoiceService;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

public class ContactMultiDeletionInteraction extends BaseEventHandlerFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String[] RAW_CONTACT_PROJECTION;
    private static final String[] RAW_CONTACT_PROJECTION_INTERNAL = {"_id", "account_type", "data_set", "contact_id", "display_name", "display_name_alt"};
    private TreeSet<Long> mContactIds;
    private Context mContext;
    private AlertDialog mDialog;
    private HandlerThread mHandlerThread;
    private boolean mIsLoaderActive;
    private MultiContactDeleteListener mListener;
    private SendRequestHandler mRequestHandler;
    List<MultiChoiceRequest> mRequests = null;
    private DeleteRequestConnection mConnection = null;

    public interface MultiContactDeleteListener {
        void onDeletionCancelled();

        void onDeletionFinished();
    }

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(RAW_CONTACT_PROJECTION_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("indicate_phone_or_sim_contact");
            arrayListNewArrayList.add("index_in_sim");
        }
        RAW_CONTACT_PROJECTION = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public static ContactMultiDeletionInteraction start(Fragment fragment, TreeSet<Long> treeSet) {
        if (treeSet == null) {
            return null;
        }
        FragmentManager fragmentManager = fragment.getFragmentManager();
        ContactMultiDeletionInteraction contactMultiDeletionInteraction = (ContactMultiDeletionInteraction) fragmentManager.findFragmentByTag(ContactSaveService.ACTION_DELETE_MULTIPLE_CONTACTS);
        if (contactMultiDeletionInteraction == null) {
            Log.i("ContactMultiDeletionInteraction", "[start] new...");
            ContactMultiDeletionInteraction contactMultiDeletionInteraction2 = new ContactMultiDeletionInteraction();
            contactMultiDeletionInteraction2.setContactIds(treeSet);
            fragmentManager.beginTransaction().add(contactMultiDeletionInteraction2, ContactSaveService.ACTION_DELETE_MULTIPLE_CONTACTS).commitAllowingStateLoss();
            return contactMultiDeletionInteraction2;
        }
        contactMultiDeletionInteraction.setContactIds(treeSet);
        return contactMultiDeletionInteraction;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        Log.i("ContactMultiDeletionInteraction", "[onAttach].");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.setOnDismissListener(null);
            this.mDialog.dismiss();
            this.mDialog = null;
        }
    }

    public void setContactIds(TreeSet<Long> treeSet) {
        Log.i("ContactMultiDeletionInteraction", "[setContactIds]");
        this.mContactIds = treeSet;
        this.mIsLoaderActive = true;
        if (isStarted() && !treeSet.isEmpty()) {
            Log.i("ContactMultiDeletionInteraction", "[setContactIds]isStarted");
            Bundle bundle = new Bundle();
            bundle.putSerializable(ContactSaveService.EXTRA_CONTACT_IDS, this.mContactIds);
            getLoaderManager().restartLoader(R.id.dialog_delete_multiple_contact_loader_id, bundle, this);
        }
    }

    private boolean isStarted() {
        return isAdded();
    }

    @Override
    public void onStart() {
        Log.i("ContactMultiDeletionInteraction", "[onStart]mIsLoaderActive = " + this.mIsLoaderActive);
        if (this.mIsLoaderActive) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(ContactSaveService.EXTRA_CONTACT_IDS, this.mContactIds);
            getLoaderManager().initLoader(R.id.dialog_delete_multiple_contact_loader_id, bundle, this);
        }
        super.onStart();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.i("ContactMultiDeletionInteraction", "[onCreateLoader]...");
        TreeSet treeSet = (TreeSet) bundle.getSerializable(ContactSaveService.EXTRA_CONTACT_IDS);
        Object[] array = treeSet.toArray();
        StringBuilder sb = new StringBuilder();
        sb.append("contact_id IN (");
        for (int i2 = 0; i2 < treeSet.size(); i2++) {
            sb.append(String.valueOf(array[i2]));
            if (i2 < treeSet.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        return new CursorLoader(this.mContext, ContactsContract.RawContacts.CONTENT_URI, RAW_CONTACT_PROJECTION, sb.toString(), null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int i;
        HashSet hashSet;
        String str;
        Object obj;
        byte b;
        HashSet hashSet2;
        Cursor cursor2 = cursor;
        Log.i("ContactMultiDeletionInteraction", "[onLoadFinished]...");
        if (this.mDialog != null) {
            this.mDialog.setOnDismissListener(null);
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (!this.mIsLoaderActive) {
            Log.e("ContactMultiDeletionInteraction", "[onLoadFinished]mIsLoaderActive is false, return!");
            return;
        }
        if (cursor2 == null || cursor.isClosed()) {
            Log.e("ContactMultiDeletionInteraction", "Failed to load contacts");
            return;
        }
        HashSet hashSetNewHashSet = Sets.newHashSet();
        HashSet hashSetNewHashSet2 = Sets.newHashSet();
        HashSet hashSetNewHashSet3 = Sets.newHashSet();
        HashSet hashSetNewHashSet4 = Sets.newHashSet();
        ContactsPreferences contactsPreferences = new ContactsPreferences(this.mContext);
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(getActivity());
        cursor2.moveToPosition(-1);
        this.mRequests = new ArrayList();
        while (cursor.moveToNext()) {
            long j = cursor2.getLong(0);
            String string = cursor2.getString(1);
            String string2 = cursor2.getString(2);
            long j2 = cursor2.getLong(3);
            String preferredDisplayName = ContactDisplayUtils.getPreferredDisplayName(cursor2.getString(4), cursor2.getString(5), contactsPreferences);
            if (!TextUtils.isEmpty(preferredDisplayName)) {
                hashSetNewHashSet4.add(preferredDisplayName);
            }
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                hashSet = hashSetNewHashSet2;
                str = string;
                this.mRequests.add(new MultiChoiceRequest((int) cursor2.getLong(6), (int) cursor2.getLong(7), (int) j2, preferredDisplayName));
                obj = null;
                b = -1;
            } else {
                hashSet = hashSetNewHashSet2;
                str = string;
                obj = null;
                b = -1;
                this.mRequests.add(new MultiChoiceRequest(-1, -1, (int) j2, null));
            }
            AccountType accountType = accountTypeManager.getAccountType(str, string2);
            if (accountType == null || accountType.areContactsWritable()) {
                hashSet2 = hashSet;
                hashSet2.add(Long.valueOf(j));
            } else {
                hashSet2 = hashSet;
                hashSetNewHashSet.add(Long.valueOf(j));
            }
            hashSetNewHashSet2 = hashSet2;
            cursor2 = cursor;
        }
        int size = hashSetNewHashSet.size();
        int size2 = hashSetNewHashSet2.size();
        int i2 = android.R.string.ok;
        if (size > 0 && size2 > 0) {
            i = R.string.batch_delete_multiple_accounts_confirmation;
        } else if (size > 0 && size2 == 0) {
            i = R.string.batch_delete_read_only_contact_confirmation;
            i2 = R.string.readOnlyContactWarning_positive_button;
        } else {
            if (size2 == 1) {
                i = R.string.single_delete_confirmation;
            } else {
                i = R.string.batch_delete_confirmation;
            }
            i2 = R.string.deleteConfirmation_positive_button;
        }
        if (size > 0) {
            showReadonlyDialog();
        } else {
            Long[] lArr = (Long[]) hashSetNewHashSet3.toArray(new Long[hashSetNewHashSet3.size()]);
            long[] jArr = new long[hashSetNewHashSet3.size()];
            for (int i3 = 0; i3 < hashSetNewHashSet3.size(); i3++) {
                jArr[i3] = lArr[i3].longValue();
            }
            showDialog(i, i2, jArr, (String[]) hashSetNewHashSet4.toArray(new String[hashSetNewHashSet4.size()]));
        }
        getLoaderManager().destroyLoader(R.id.dialog_delete_multiple_contact_loader_id);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void showDialog(int i, int i2, final long[] jArr, final String[] strArr) {
        this.mDialog = new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(i).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                ContactMultiDeletionInteraction.this.mListener.onDeletionCancelled();
            }
        }).setPositiveButton(i2, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                ContactMultiDeletionInteraction.this.doDeleteContact(jArr, strArr);
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ContactMultiDeletionInteraction.this.mListener.onDeletionCancelled();
            }
        }).create();
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                ContactMultiDeletionInteraction.this.mIsLoaderActive = false;
            }
        });
        this.mDialog.show();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("active", this.mIsLoaderActive);
        bundle.putSerializable(ContactSaveService.EXTRA_CONTACT_IDS, this.mContactIds);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (bundle != null) {
            this.mIsLoaderActive = bundle.getBoolean("active");
            this.mContactIds = (TreeSet) bundle.getSerializable(ContactSaveService.EXTRA_CONTACT_IDS);
        }
    }

    protected void doDeleteContact(long[] jArr, String[] strArr) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        for (MultiChoiceRequest multiChoiceRequest : this.mRequests) {
            arrayList.add(Long.valueOf(multiChoiceRequest.mContactId));
            arrayList2.add(multiChoiceRequest.mContactName);
        }
        Long[] lArr = (Long[]) arrayList.toArray(new Long[arrayList.size()]);
        long[] jArr2 = new long[arrayList.size()];
        String[] strArr2 = (String[]) arrayList2.toArray(new String[arrayList.size()]);
        String[] strArr3 = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            jArr2[i] = lArr[i].longValue();
            strArr3[i] = strArr2[i];
        }
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            handleDelete();
        } else {
            this.mContext.startService(ContactSaveService.createDeleteMultipleContactsIntent(this.mContext, jArr2, strArr3));
        }
        this.mListener.onDeletionFinished();
    }

    public void setListener(MultiContactDeleteListener multiContactDeleteListener) {
        this.mListener = multiContactDeleteListener;
    }

    private void handleDelete() {
        Log.d("ContactMultiDeletionInteraction", "[handleDelete]...");
        if (this.mConnection != null) {
            Log.w("ContactMultiDeletionInteraction", "[handleDelete]abort due to mConnection is not null,return.");
            return;
        }
        startDeleteService();
        if (this.mHandlerThread == null) {
            this.mHandlerThread = new HandlerThread("ContactMultiDeletionInteraction");
            this.mHandlerThread.start();
            this.mRequestHandler = new SendRequestHandler(this.mHandlerThread.getLooper());
        }
        if (this.mRequests.size() > 0) {
            this.mRequestHandler.sendMessage(this.mRequestHandler.obtainMessage(100, this.mRequests));
        } else {
            this.mRequestHandler.sendMessage(this.mRequestHandler.obtainMessage(200));
        }
    }

    private class DeleteRequestConnection implements ServiceConnection {
        private MultiChoiceService mService;

        private DeleteRequestConnection() {
        }

        public boolean sendDeleteRequest(List<MultiChoiceRequest> list) {
            Log.d("ContactMultiDeletionInteraction", "[sendDeleteRequest] Send an delete request");
            if (this.mService == null) {
                Log.i("ContactMultiDeletionInteraction", "[sendDeleteRequest] mService is not ready");
                return false;
            }
            this.mService.handleDeleteRequest(list, new MultiChoiceHandlerListener(this.mService));
            return true;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("ContactMultiDeletionInteraction", "[onServiceConnected]");
            this.mService = ((MultiChoiceService.MyBinder) iBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("ContactMultiDeletionInteraction", "[onServiceDisconnected] Disconnected from MultiChoiceService");
        }
    }

    private class SendRequestHandler extends Handler {
        private int mRetryCount;

        public SendRequestHandler(Looper looper) {
            super(looper);
            this.mRetryCount = 20;
        }

        @Override
        public void handleMessage(Message message) {
            Log.i("ContactMultiDeletionInteraction", "[handleMessage]msg.what = " + message.what);
            if (message.what == 100) {
                if (!ContactMultiDeletionInteraction.this.mConnection.sendDeleteRequest((List) message.obj)) {
                    Log.i("ContactMultiDeletionInteraction", "[handleMessage]send fail, mRetryCount = " + this.mRetryCount);
                    int i = this.mRetryCount;
                    this.mRetryCount = i + (-1);
                    if (i > 0) {
                        sendMessageDelayed(obtainMessage(message.what, message.obj), 500L);
                        return;
                    } else {
                        sendMessage(obtainMessage(200));
                        return;
                    }
                }
                sendMessage(obtainMessage(200));
                return;
            }
            if (message.what == 200) {
                ContactMultiDeletionInteraction.this.destroyMyself();
            } else {
                super.handleMessage(message);
            }
        }
    }

    void startDeleteService() {
        Log.i("ContactMultiDeletionInteraction", "[startDeleteService]");
        this.mConnection = new DeleteRequestConnection();
        Intent intent = new Intent(getActivity(), (Class<?>) MultiChoiceService.class);
        getContext().startService(intent);
        getContext().bindService(intent, this.mConnection, 1);
    }

    void destroyMyself() {
        Log.i("ContactMultiDeletionInteraction", "[destroyMyself]mHandlerThread:" + this.mHandlerThread);
        if (this.mConnection != null) {
            if (getContext() != null) {
                getContext().unbindService(this.mConnection);
            } else {
                Log.e("ContactMultiDeletionInteraction", "[destroyMyself] getContext() is null !!!");
            }
            this.mConnection = null;
        }
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
            this.mHandlerThread = null;
        }
    }

    @Override
    public void onReceiveEvent(String str, Intent intent) {
        Log.i("ContactMultiDeletionInteraction", "[onReceiveEvent] eventType: " + str);
        if ("PhbChangeEvent".equals(str) && this.mDialog != null && this.mDialog.isShowing()) {
            Log.i("ContactMultiDeletionInteraction", "[onReceiveEvent] mDialog will dismiss");
            this.mDialog.dismiss();
            if (this.mListener != null) {
                this.mListener.onDeletionCancelled();
            }
        }
    }

    private void showReadonlyDialog() {
        this.mDialog = new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.readOnlyContactWarning).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ContactMultiDeletionInteraction.this.mListener.onDeletionCancelled();
            }
        }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ContactMultiDeletionInteraction.this.mListener.onDeletionFinished();
            }
        }).create();
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                ContactMultiDeletionInteraction.this.mIsLoaderActive = false;
                ContactMultiDeletionInteraction.this.mDialog = null;
            }
        });
        this.mDialog.show();
    }
}
