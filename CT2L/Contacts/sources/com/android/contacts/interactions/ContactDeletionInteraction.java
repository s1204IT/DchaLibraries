package com.android.contacts.interactions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.google.common.collect.Sets;
import java.util.HashSet;

public class ContactDeletionInteraction extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, DialogInterface.OnDismissListener {
    private static final String[] ENTITY_PROJECTION = {"raw_contact_id", "account_type", "data_set", "contact_id", "lookup"};
    private boolean mActive;
    private Uri mContactUri;
    private Context mContext;
    private AlertDialog mDialog;
    private boolean mFinishActivityWhenDone;
    int mMessageId;
    private TestLoaderManager mTestLoaderManager;

    public static ContactDeletionInteraction start(Activity activity, Uri contactUri, boolean finishActivityWhenDone) {
        return startWithTestLoaderManager(activity, contactUri, finishActivityWhenDone, null);
    }

    static ContactDeletionInteraction startWithTestLoaderManager(Activity activity, Uri contactUri, boolean finishActivityWhenDone, TestLoaderManager testLoaderManager) {
        if (contactUri == null) {
            return null;
        }
        FragmentManager fragmentManager = activity.getFragmentManager();
        ContactDeletionInteraction fragment = (ContactDeletionInteraction) fragmentManager.findFragmentByTag("deleteContact");
        if (fragment == null) {
            ContactDeletionInteraction fragment2 = new ContactDeletionInteraction();
            fragment2.setTestLoaderManager(testLoaderManager);
            fragment2.setContactUri(contactUri);
            fragment2.setFinishActivityWhenDone(finishActivityWhenDone);
            fragmentManager.beginTransaction().add(fragment2, "deleteContact").commitAllowingStateLoss();
            return fragment2;
        }
        fragment.setTestLoaderManager(testLoaderManager);
        fragment.setContactUri(contactUri);
        fragment.setFinishActivityWhenDone(finishActivityWhenDone);
        return fragment;
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

    private void setTestLoaderManager(TestLoaderManager mockLoaderManager) {
        this.mTestLoaderManager = mockLoaderManager;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
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

    public void setContactUri(Uri contactUri) {
        this.mContactUri = contactUri;
        this.mActive = true;
        if (isStarted()) {
            Bundle args = new Bundle();
            args.putParcelable("contactUri", this.mContactUri);
            getLoaderManager().restartLoader(R.id.dialog_delete_contact_loader_id, args, this);
        }
    }

    private void setFinishActivityWhenDone(boolean finishActivityWhenDone) {
        this.mFinishActivityWhenDone = finishActivityWhenDone;
    }

    boolean isStarted() {
        return isAdded();
    }

    @Override
    public void onStart() {
        if (this.mActive) {
            Bundle args = new Bundle();
            args.putParcelable("contactUri", this.mContactUri);
            getLoaderManager().initLoader(R.id.dialog_delete_contact_loader_id, args, this);
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri contactUri = (Uri) args.getParcelable("contactUri");
        return new CursorLoader(this.mContext, Uri.withAppendedPath(contactUri, "entities"), ENTITY_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (this.mDialog != null) {
            this.mDialog.dismiss();
            this.mDialog = null;
        }
        if (this.mActive) {
            long contactId = 0;
            String lookupKey = null;
            HashSet<Long> readOnlyRawContacts = Sets.newHashSet();
            HashSet<Long> writableRawContacts = Sets.newHashSet();
            AccountTypeManager accountTypes = AccountTypeManager.getInstance(getActivity());
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                long rawContactId = cursor.getLong(0);
                String accountType = cursor.getString(1);
                String dataSet = cursor.getString(2);
                contactId = cursor.getLong(3);
                lookupKey = cursor.getString(4);
                AccountType type = accountTypes.getAccountType(accountType, dataSet);
                boolean writable = type == null || type.areContactsWritable();
                if (writable) {
                    writableRawContacts.add(Long.valueOf(rawContactId));
                } else {
                    readOnlyRawContacts.add(Long.valueOf(rawContactId));
                }
            }
            int readOnlyCount = readOnlyRawContacts.size();
            int writableCount = writableRawContacts.size();
            if (readOnlyCount > 0 && writableCount > 0) {
                this.mMessageId = R.string.readOnlyContactDeleteConfirmation;
            } else if (readOnlyCount > 0 && writableCount == 0) {
                this.mMessageId = R.string.readOnlyContactWarning;
            } else if (readOnlyCount == 0 && writableCount > 1) {
                this.mMessageId = R.string.multipleContactDeleteConfirmation;
            } else {
                this.mMessageId = R.string.deleteConfirmation;
            }
            Uri contactUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
            showDialog(this.mMessageId, contactUri);
            getLoaderManager().destroyLoader(R.id.dialog_delete_contact_loader_id);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void showDialog(int messageId, final Uri contactUri) {
        this.mDialog = new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(messageId).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                ContactDeletionInteraction.this.doDeleteContact(contactUri);
            }
        }).create();
        this.mDialog.setOnDismissListener(this);
        this.mDialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.mActive = false;
        this.mDialog = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("active", this.mActive);
        outState.putParcelable("contactUri", this.mContactUri);
        outState.putBoolean("finishWhenDone", this.mFinishActivityWhenDone);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            this.mActive = savedInstanceState.getBoolean("active");
            this.mContactUri = (Uri) savedInstanceState.getParcelable("contactUri");
            this.mFinishActivityWhenDone = savedInstanceState.getBoolean("finishWhenDone");
        }
    }

    protected void doDeleteContact(Uri contactUri) {
        this.mContext.startService(ContactSaveService.createDeleteContactIntent(this.mContext, contactUri));
        if (isAdded() && this.mFinishActivityWhenDone) {
            getActivity().setResult(3);
            getActivity().finish();
        }
    }
}
