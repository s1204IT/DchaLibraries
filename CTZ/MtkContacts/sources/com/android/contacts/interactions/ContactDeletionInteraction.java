package com.android.contacts.interactions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.preference.ContactsPreferences;
import com.android.contacts.util.ContactDisplayUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mediatek.contacts.eventhandler.BaseEventHandlerFragment;
import com.mediatek.contacts.interactions.ContactDeletionInteractionUtils;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.simservice.SimDeleteProcessor;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.HashSet;

public class ContactDeletionInteraction extends BaseEventHandlerFragment implements LoaderManager.LoaderCallbacks<Cursor>, DialogInterface.OnDismissListener, DialogInterface.OnShowListener, SimDeleteProcessor.Listener {
    public static final String ARG_CONTACT_URI = "contactUri";
    private static final int COLUMN_INDEX_ACCOUNT_TYPE = 1;
    private static final int COLUMN_INDEX_CONTACT_ID = 3;
    private static final int COLUMN_INDEX_DATA_SET = 2;
    private static final int COLUMN_INDEX_DISPLAY_NAME = 5;
    private static final int COLUMN_INDEX_DISPLAY_NAME_ALT = 6;
    private static final int COLUMN_INDEX_INDICATE_PHONE_SIM = 7;
    private static final int COLUMN_INDEX_IN_SIM = 8;
    private static final int COLUMN_INDEX_LOOKUP_KEY = 4;
    private static final int COLUMN_INDEX_RAW_CONTACT_ID = 0;
    private static final String[] ENTITY_PROJECTION;
    private static final String[] ENTITY_PROJECTION_INTERNAL = {"raw_contact_id", "account_type", "data_set", "contact_id", "lookup", "display_name", "display_name_alt"};
    private static final String FRAGMENT_TAG = "deleteContact";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_CONTACT_SIM_INDEX = "contact_sim_index";
    private static final String KEY_CONTACT_SIM_URI = "contact_sim_uri";
    private static final String KEY_CONTACT_SUB_ID = "contact_sub_id";
    private static final String KEY_CONTACT_URI = "contactUri";
    private static final String KEY_FINISH_WHEN_DONE = "finishWhenDone";
    public static final int RESULT_CODE_DELETED = 3;
    private static final String TAG = "ContactDeletion";
    private boolean mActive;
    private Uri mContactUri;
    private Context mContext;
    private AlertDialog mDialog;
    private String mDisplayName;
    private String mDisplayNameAlt;
    private boolean mFinishActivityWhenDone;
    int mMessageId;
    private TestLoaderManagerBase mTestLoaderManager;
    private Uri mSimUri = null;
    private int mSimIndex = -1;
    private int mSubId = SubInfoUtils.getInvalidSubId();

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(ENTITY_PROJECTION_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("indicate_phone_or_sim_contact");
            arrayListNewArrayList.add("index_in_sim");
        }
        ENTITY_PROJECTION = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
    }

    public static ContactDeletionInteraction start(Activity activity, Uri uri, boolean z) {
        Log.d(FRAGMENT_TAG, "[start] contactUri=" + uri);
        return startWithTestLoaderManager(activity, uri, z, null);
    }

    static ContactDeletionInteraction startWithTestLoaderManager(Activity activity, Uri uri, boolean z, TestLoaderManagerBase testLoaderManagerBase) {
        if (uri == null || activity.isDestroyed()) {
            return null;
        }
        FragmentManager fragmentManager = activity.getFragmentManager();
        ContactDeletionInteraction contactDeletionInteraction = (ContactDeletionInteraction) fragmentManager.findFragmentByTag(FRAGMENT_TAG);
        if (contactDeletionInteraction == null) {
            contactDeletionInteraction = new ContactDeletionInteraction();
            contactDeletionInteraction.setTestLoaderManager(testLoaderManagerBase);
            contactDeletionInteraction.setContactUri(uri);
            contactDeletionInteraction.setFinishActivityWhenDone(z);
            fragmentManager.beginTransaction().add(contactDeletionInteraction, FRAGMENT_TAG).commitAllowingStateLoss();
        } else {
            contactDeletionInteraction.setTestLoaderManager(testLoaderManagerBase);
            contactDeletionInteraction.setContactUri(uri);
            contactDeletionInteraction.setFinishActivityWhenDone(z);
        }
        contactDeletionInteraction.mSimUri = null;
        contactDeletionInteraction.mSimIndex = -1;
        return contactDeletionInteraction;
    }

    @Override
    public LoaderManager getLoaderManager() {
        LoaderManager loaderManager = super.getLoaderManager();
        if (this.mTestLoaderManager != null) {
            this.mTestLoaderManager.setDelegate(loaderManager);
            return this.mTestLoaderManager;
        }
        return loaderManager;
    }

    private void setTestLoaderManager(TestLoaderManagerBase testLoaderManagerBase) {
        this.mTestLoaderManager = testLoaderManagerBase;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        SimDeleteProcessor.registerListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.setOnDismissListener(null);
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        SimDeleteProcessor.unregisterListener(this);
    }

    public void setContactUri(Uri uri) {
        this.mContactUri = uri;
        this.mActive = true;
        if (isStarted()) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("contactUri", this.mContactUri);
            getLoaderManager().restartLoader(R.id.dialog_delete_contact_loader_id, bundle, this);
        }
    }

    private void setFinishActivityWhenDone(boolean z) {
        this.mFinishActivityWhenDone = z;
    }

    boolean isStarted() {
        return isAdded();
    }

    @Override
    public void onStart() {
        if (this.mActive) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("contactUri", this.mContactUri);
            getLoaderManager().initLoader(R.id.dialog_delete_contact_loader_id, bundle, this);
        }
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mDialog != null) {
            this.mDialog.hide();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this.mContext, Uri.withAppendedPath((Uri) bundle.getParcelable("contactUri"), "entities"), ENTITY_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        String str = null;
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (!this.mActive) {
            return;
        }
        if (cursor == null || cursor.isClosed()) {
            Log.e(TAG, "Failed to load contacts");
            return;
        }
        long j = 0;
        HashSet hashSetNewHashSet = Sets.newHashSet();
        HashSet hashSetNewHashSet2 = Sets.newHashSet();
        AccountTypeManager accountTypeManager = AccountTypeManager.getInstance(getActivity());
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            boolean z = false;
            long j2 = cursor.getLong(0);
            String string = cursor.getString(1);
            String string2 = cursor.getString(2);
            long j3 = cursor.getLong(3);
            String string3 = cursor.getString(4);
            this.mDisplayName = cursor.getString(COLUMN_INDEX_DISPLAY_NAME);
            this.mDisplayNameAlt = cursor.getString(COLUMN_INDEX_DISPLAY_NAME_ALT);
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                this.mSubId = cursor.getInt(COLUMN_INDEX_INDICATE_PHONE_SIM);
                this.mSimIndex = cursor.getInt(COLUMN_INDEX_IN_SIM);
                this.mSimUri = SubInfoUtils.getIccProviderUri(this.mSubId);
            }
            AccountType accountType = accountTypeManager.getAccountType(string, string2);
            if (accountType == null || accountType.areContactsWritable()) {
                z = true;
            }
            if (z) {
                hashSetNewHashSet2.add(Long.valueOf(j2));
            } else {
                hashSetNewHashSet.add(Long.valueOf(j2));
            }
            j = j3;
            str = string3;
        }
        if (TextUtils.isEmpty(str)) {
            Log.e(TAG, "Failed to find contact lookup key");
            getActivity().finish();
            return;
        }
        int size = hashSetNewHashSet.size();
        int size2 = hashSetNewHashSet2.size();
        int i = android.R.string.ok;
        if (size > 0 && size2 > 0) {
            this.mMessageId = R.string.readOnlyContactDeleteConfirmation;
        } else if (size > 0 && size2 == 0) {
            this.mMessageId = R.string.readOnlyContactWarning;
            i = R.string.readOnlyContactWarning_positive_button;
        } else {
            if (size == 0 && size2 > 1) {
                this.mMessageId = R.string.multipleContactDeleteConfirmation;
            } else {
                this.mMessageId = R.string.deleteConfirmation;
            }
            i = R.string.deleteConfirmation_positive_button;
        }
        if (size > 0) {
            showReadonlyDialog();
        } else {
            showDialog(this.mMessageId, i, ContactsContract.Contacts.getLookupUri(j, str));
        }
        getLoaderManager().destroyLoader(R.id.dialog_delete_contact_loader_id);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void showDialog(int i, int i2, final Uri uri) {
        this.mDialog = new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(i).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(i2, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                ContactDeletionInteraction.this.doDeleteContact(uri);
            }
        }).create();
        this.mDialog.setOnDismissListener(this);
        this.mDialog.setOnShowListener(this);
        this.mDialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        this.mActive = false;
        this.mDialog = null;
    }

    @Override
    public void onShow(DialogInterface dialogInterface) {
        this.mActive = true;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(KEY_ACTIVE, this.mActive);
        bundle.putParcelable("contactUri", this.mContactUri);
        bundle.putParcelable(KEY_CONTACT_SIM_URI, this.mSimUri);
        bundle.putInt(KEY_CONTACT_SIM_INDEX, this.mSimIndex);
        bundle.putInt(KEY_CONTACT_SUB_ID, this.mSubId);
        bundle.putBoolean(KEY_FINISH_WHEN_DONE, this.mFinishActivityWhenDone);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        if (bundle != null) {
            this.mActive = bundle.getBoolean(KEY_ACTIVE);
            this.mContactUri = (Uri) bundle.getParcelable("contactUri");
            this.mSimUri = (Uri) bundle.getParcelable(KEY_CONTACT_SIM_URI);
            this.mSimIndex = bundle.getInt(KEY_CONTACT_SIM_INDEX);
            this.mSubId = bundle.getInt(KEY_CONTACT_SUB_ID);
            this.mFinishActivityWhenDone = bundle.getBoolean(KEY_FINISH_WHEN_DONE);
        }
    }

    protected void doDeleteContact(Uri uri) {
        String string;
        if (!isAdded() || ContactDeletionInteractionUtils.doDeleteSimContact(this.mContext, uri, this.mSimUri, this.mSimIndex, this.mSubId, this)) {
            return;
        }
        this.mContext.startService(ContactSaveService.createDeleteContactIntent(this.mContext, uri));
        if (isAdded() && this.mFinishActivityWhenDone) {
            Log.d(FRAGMENT_TAG, "[doDeleteContact] finished");
            getActivity().setResult(3);
            getActivity().finish();
            String preferredDisplayName = ContactDisplayUtils.getPreferredDisplayName(this.mDisplayName, this.mDisplayNameAlt, new ContactsPreferences(this.mContext));
            if (TextUtils.isEmpty(preferredDisplayName)) {
                string = getResources().getQuantityString(R.plurals.contacts_deleted_toast, 1);
            } else {
                string = getResources().getString(R.string.contacts_deleted_one_named_toast, preferredDisplayName);
            }
            Toast.makeText(this.mContext, string, 1).show();
        }
    }

    @Override
    public void onSIMDeleteFailed() {
        if (isAdded()) {
            getActivity().finish();
        }
    }

    @Override
    public void onSIMDeleteCompleted() {
        if (isAdded() && this.mFinishActivityWhenDone) {
            getActivity().setResult(3);
            final String quantityString = getResources().getQuantityString(R.plurals.contacts_deleted_toast, 1);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ContactDeletionInteraction.this.mContext, quantityString, 1).show();
                }
            });
            getActivity().finish();
        }
    }

    @Override
    public void onReceiveEvent(String str, Intent intent) {
        int intExtra = intent.getIntExtra("subscription", -1000);
        Log.i(TAG, "[onReceiveEvent] eventType: " + str + ", extraData: " + intent.toString() + ",stateChangeSubId: " + intExtra + ",mSubId: " + this.mSubId);
        if ("PhbChangeEvent".equals(str) && this.mSubId == intExtra) {
            Log.i(TAG, "[onReceiveEvent] phb state change,finish EditorActivity ");
            getActivity().setResult(3);
            getActivity().finish();
        }
    }

    private void showReadonlyDialog() {
        this.mDialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.deleteConfirmation_title).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.readOnlyContactWarning).setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null).create();
        this.mDialog.setOnDismissListener(this);
        this.mDialog.setOnShowListener(this);
        this.mDialog.show();
    }
}
