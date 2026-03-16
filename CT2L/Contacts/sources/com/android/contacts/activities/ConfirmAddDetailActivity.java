package com.android.contacts.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.EmptyService;
import com.android.contacts.editor.Editor;
import com.android.contacts.editor.EditorUiUtils;
import com.android.contacts.editor.ViewIdGenerator;
import com.android.contacts.util.DialogManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ConfirmAddDetailActivity extends Activity implements DialogManager.DialogShowingViewActivity {
    private static WeakReference<ProgressDialog> sProgressDialog;
    private AccountTypeManager mAccountTypeManager;
    private long mContactId;
    private Uri mContactUri;
    private ContentResolver mContentResolver;
    private String mDisplayName;
    private TextView mDisplayNameView;
    private AccountType mEditableAccountType;
    private ViewGroup mEditorContainerView;
    private RawContactDeltaList mEntityDeltaList;
    private LayoutInflater mInflater;
    private boolean mIsReadOnly;
    private String mLookupKey;
    private ImageView mPhotoView;
    private QueryHandler mQueryHandler;
    private RawContactDelta mRawContactDelta;
    private TextView mReadOnlyWarningView;
    private View mRootView;
    private static final boolean VERBOSE_LOGGING = Log.isLoggable("ConfirmAdd", 2);
    private static final String[] MIME_TYPE_PRIORITY_LIST = {"vnd.android.cursor.item/nickname", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/im", "vnd.android.cursor.item/postal-address_v2", "vnd.android.cursor.item/phone_v2"};
    private String mMimetype = "vnd.android.cursor.item/phone_v2";
    private final DialogManager mDialogManager = new DialogManager(this);
    private final View.OnClickListener mDetailsButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (ConfirmAddDetailActivity.this.mIsReadOnly) {
                ConfirmAddDetailActivity.this.onSaveCompleted(true);
            } else {
                ConfirmAddDetailActivity.this.doSaveAction();
            }
        }
    };
    private final View.OnClickListener mDoneButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ConfirmAddDetailActivity.this.doSaveAction();
        }
    };
    private final View.OnClickListener mCancelButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ConfirmAddDetailActivity.this.setResult(0);
            ConfirmAddDetailActivity.this.finish();
        }
    };

    private interface ContactQuery {
        public static final String[] COLUMNS = {"_id", "lookup", "photo_id", "display_name"};
    }

    private interface ExtraInfoQuery {
        public static final String[] COLUMNS = {"contact_id", "mimetype", "data1"};
    }

    private interface PhotoQuery {
        public static final String[] COLUMNS = {"data15"};
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mInflater = (LayoutInflater) getSystemService("layout_inflater");
        this.mContentResolver = getContentResolver();
        Intent intent = getIntent();
        this.mContactUri = intent.getData();
        if (this.mContactUri == null) {
            setResult(0);
            finish();
        }
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("phone")) {
                this.mMimetype = "vnd.android.cursor.item/phone_v2";
            } else if (extras.containsKey("email")) {
                this.mMimetype = "vnd.android.cursor.item/email_v2";
            } else {
                throw new IllegalStateException("Error: No valid mimetype found in intent extras");
            }
        }
        this.mAccountTypeManager = AccountTypeManager.getInstance(this);
        setContentView(R.layout.confirm_add_detail_activity);
        this.mRootView = findViewById(R.id.root_view);
        this.mReadOnlyWarningView = (TextView) findViewById(R.id.read_only_warning);
        findViewById(R.id.open_details_push_layer).setOnClickListener(this.mDetailsButtonClickListener);
        findViewById(R.id.btn_done).setOnClickListener(this.mDoneButtonClickListener);
        findViewById(R.id.btn_cancel).setOnClickListener(this.mCancelButtonClickListener);
        this.mDisplayNameView = (TextView) findViewById(R.id.name);
        this.mPhotoView = (ImageView) findViewById(R.id.photo);
        this.mPhotoView.setImageDrawable(ContactPhotoManager.getDefaultAvatarDrawableForContact(getResources(), false, null));
        this.mEditorContainerView = (ViewGroup) findViewById(R.id.editor_container);
        resetAsyncQueryHandler();
        startContactQuery(this.mContactUri);
        new QueryEntitiesTask(this).execute(intent);
    }

    @Override
    public DialogManager getDialogManager() {
        return this.mDialogManager;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) {
            return this.mDialogManager.onCreateDialog(id, args);
        }
        Log.w("ConfirmAdd", "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    private void resetAsyncQueryHandler() {
        this.mQueryHandler = new QueryHandler(this.mContentResolver);
    }

    private void startContactQuery(Uri contactUri) {
        this.mQueryHandler.startQuery(0, contactUri, contactUri, ContactQuery.COLUMNS, null, null, null);
    }

    private void startPhotoQuery(long photoId, Uri lookupKey) {
        this.mQueryHandler.startQuery(1, lookupKey, ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, photoId), PhotoQuery.COLUMNS, null, null, null);
    }

    private void startDisambiguationQuery(String contactDisplayName) {
        String displayNameSelection;
        String[] selectionArgs;
        Uri.Builder builder = ContactsContract.Contacts.CONTENT_URI.buildUpon();
        builder.appendQueryParameter("limit", String.valueOf(1));
        Uri uri = builder.build();
        if (TextUtils.isEmpty(contactDisplayName)) {
            displayNameSelection = "display_name IS NULL";
            selectionArgs = new String[]{String.valueOf(this.mContactId)};
        } else {
            displayNameSelection = "display_name = ?";
            selectionArgs = new String[]{contactDisplayName, String.valueOf(this.mContactId)};
        }
        this.mQueryHandler.startQuery(2, null, uri, new String[]{"_id"}, displayNameSelection + " AND photo_id IS NULL AND _id <> ?", selectionArgs, null);
    }

    private void startExtraInfoQuery() {
        this.mQueryHandler.startQuery(3, null, ContactsContract.Data.CONTENT_URI, ExtraInfoQuery.COLUMNS, "contact_id = ?", new String[]{String.valueOf(this.mContactId)}, null);
    }

    private static class QueryEntitiesTask extends AsyncTask<Intent, Void, RawContactDeltaList> {
        private ConfirmAddDetailActivity activityTarget;
        private String mSelection;

        public QueryEntitiesTask(ConfirmAddDetailActivity target) {
            this.activityTarget = target;
        }

        @Override
        protected RawContactDeltaList doInBackground(Intent... params) {
            Intent intent = params[0];
            ContentResolver resolver = this.activityTarget.getContentResolver();
            Uri data = intent.getData();
            String authority = data.getAuthority();
            String mimeType = intent.resolveType(resolver);
            this.mSelection = "0";
            String selectionArg = null;
            if ("com.android.contacts".equals(authority)) {
                if ("vnd.android.cursor.item/contact".equals(mimeType)) {
                    long contactId = ContentUris.parseId(data);
                    selectionArg = String.valueOf(contactId);
                    this.mSelection = "contact_id=?";
                } else if ("vnd.android.cursor.item/raw_contact".equals(mimeType)) {
                    long rawContactId = ContentUris.parseId(data);
                    long contactId2 = queryForContactId(resolver, rawContactId);
                    selectionArg = String.valueOf(contactId2);
                    this.mSelection = "contact_id=?";
                }
            } else if ("contacts".equals(authority)) {
                long rawContactId2 = ContentUris.parseId(data);
                selectionArg = String.valueOf(rawContactId2);
                this.mSelection = "raw_contact_id=?";
            }
            return RawContactDeltaList.fromQuery(ContactsContract.RawContactsEntity.CONTENT_URI, this.activityTarget.getContentResolver(), this.mSelection, new String[]{selectionArg}, null);
        }

        private static long queryForContactId(ContentResolver resolver, long rawContactId) {
            Cursor contactIdCursor = null;
            long contactId = -1;
            try {
                contactIdCursor = resolver.query(ContactsContract.RawContacts.CONTENT_URI, new String[]{"contact_id"}, "_id=?", new String[]{String.valueOf(rawContactId)}, null);
                if (contactIdCursor != null && contactIdCursor.moveToFirst()) {
                    contactId = contactIdCursor.getLong(0);
                }
                return contactId;
            } finally {
                if (contactIdCursor != null) {
                    contactIdCursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(RawContactDeltaList entityList) {
            if (!this.activityTarget.isFinishing()) {
                if (entityList != null && entityList.size() != 0) {
                    this.activityTarget.setEntityDeltaList(entityList);
                } else {
                    Log.e("ConfirmAdd", "Contact not found.");
                    this.activityTarget.finish();
                }
            }
        }
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            try {
                if (this != ConfirmAddDetailActivity.this.mQueryHandler) {
                    Log.d("ConfirmAdd", "onQueryComplete: discard result, the query handler is reset!");
                    if (cursor != null) {
                        return;
                    } else {
                        return;
                    }
                }
                if (ConfirmAddDetailActivity.this.isFinishing()) {
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                }
                switch (token) {
                    case 0:
                        if (cursor != null && cursor.moveToFirst()) {
                            ConfirmAddDetailActivity.this.mDisplayName = cursor.getString(3);
                            ConfirmAddDetailActivity.this.mLookupKey = cursor.getString(1);
                            ConfirmAddDetailActivity.this.setDefaultContactImage(ConfirmAddDetailActivity.this.mDisplayName, ConfirmAddDetailActivity.this.mLookupKey);
                            long photoId = cursor.getLong(2);
                            if (photoId != 0) {
                                Uri lookupUri = ContactsContract.Contacts.getLookupUri(ConfirmAddDetailActivity.this.mContactId, ConfirmAddDetailActivity.this.mLookupKey);
                                ConfirmAddDetailActivity.this.startPhotoQuery(photoId, lookupUri);
                                ConfirmAddDetailActivity.this.setDisplayName();
                                ConfirmAddDetailActivity.this.showDialogContent();
                            } else {
                                ConfirmAddDetailActivity.this.mContactId = cursor.getLong(0);
                                ConfirmAddDetailActivity.this.startDisambiguationQuery(ConfirmAddDetailActivity.this.mDisplayName);
                            }
                        }
                        break;
                    case 1:
                        Bitmap photoBitmap = null;
                        if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                            byte[] photoData = cursor.getBlob(0);
                            photoBitmap = BitmapFactory.decodeByteArray(photoData, 0, photoData.length, null);
                        }
                        if (photoBitmap != null) {
                            ConfirmAddDetailActivity.this.mPhotoView.setImageBitmap(photoBitmap);
                        }
                        break;
                    case 2:
                        if (cursor != null && cursor.getCount() > 0) {
                            ConfirmAddDetailActivity.this.startExtraInfoQuery();
                        } else {
                            ConfirmAddDetailActivity.this.setDisplayName();
                            ConfirmAddDetailActivity.this.showDialogContent();
                        }
                        break;
                    case 3:
                        if (cursor != null && cursor.moveToFirst()) {
                            HashMap<String, String> hashMapCursorData = new HashMap<>();
                            while (!cursor.isAfterLast()) {
                                String mimeType = cursor.getString(1);
                                if (!TextUtils.isEmpty(mimeType)) {
                                    String value = cursor.getString(2);
                                    if (!TextUtils.isEmpty(value)) {
                                        if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) {
                                            value = PhoneNumberUtils.formatNumber(value);
                                        }
                                        hashMapCursorData.put(mimeType, value);
                                    }
                                }
                                cursor.moveToNext();
                            }
                            String[] arr$ = ConfirmAddDetailActivity.MIME_TYPE_PRIORITY_LIST;
                            int len$ = arr$.length;
                            int i$ = 0;
                            while (true) {
                                if (i$ < len$) {
                                    String mimeType2 = arr$[i$];
                                    if (hashMapCursorData.containsKey(mimeType2)) {
                                        ConfirmAddDetailActivity.this.setDisplayName();
                                        ConfirmAddDetailActivity.this.setExtraInfoField(hashMapCursorData.get(mimeType2));
                                    } else {
                                        i$++;
                                    }
                                }
                            }
                            ConfirmAddDetailActivity.this.showDialogContent();
                        }
                        break;
                }
                if (cursor != null) {
                    cursor.close();
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void setEntityDeltaList(RawContactDeltaList entityList) {
        if (entityList == null) {
            throw new IllegalStateException();
        }
        if (VERBOSE_LOGGING) {
            Log.v("ConfirmAdd", "setEntityDeltaList: " + entityList);
        }
        this.mEntityDeltaList = entityList;
        this.mRawContactDelta = this.mEntityDeltaList.getFirstWritableRawContact(this);
        if (this.mRawContactDelta == null) {
            this.mRawContactDelta = addEditableRawContact(this, this.mEntityDeltaList);
            if (this.mRawContactDelta != null && VERBOSE_LOGGING) {
                Log.v("ConfirmAdd", "setEntityDeltaList: created editable raw_contact " + entityList);
            }
        }
        if (this.mRawContactDelta == null) {
            this.mIsReadOnly = true;
            this.mEditableAccountType = null;
        } else {
            this.mIsReadOnly = false;
            this.mEditableAccountType = this.mRawContactDelta.getRawContactAccountType(this);
            Bundle extras = getIntent().getExtras();
            if (extras != null && extras.size() > 0) {
                RawContactModifier.parseExtras(this, this.mEditableAccountType, this.mRawContactDelta, extras);
            }
        }
        bindEditor();
    }

    private static RawContactDelta addEditableRawContact(Context context, RawContactDeltaList entityDeltaList) {
        AccountTypeManager accounts = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> editableAccounts = accounts.getAccounts(true);
        if (editableAccounts.size() == 0) {
            return null;
        }
        AccountWithDataSet editableAccount = editableAccounts.get(0);
        AccountType accountType = accounts.getAccountType(editableAccount.type, editableAccount.dataSet);
        RawContact rawContact = new RawContact();
        rawContact.setAccount(editableAccount);
        RawContactDelta entityDelta = new RawContactDelta(ValuesDelta.fromAfter(rawContact.getValues()));
        Iterator<RawContactDelta> it = entityDeltaList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            RawContactDelta entity = it.next();
            ArrayList<ValuesDelta> readOnlyNames = entity.getMimeEntries("vnd.android.cursor.item/name");
            if (readOnlyNames != null && readOnlyNames.size() > 0) {
                ValuesDelta readOnlyName = readOnlyNames.get(0);
                ValuesDelta newName = RawContactModifier.ensureKindExists(entityDelta, accountType, "vnd.android.cursor.item/name");
                newName.copyStructuredNameFieldsFrom(readOnlyName);
                break;
            }
        }
        entityDeltaList.add(entityDelta);
        return entityDelta;
    }

    private void bindEditor() {
        ArrayList<ValuesDelta> deltas;
        if (this.mEntityDeltaList == null) {
            throw new IllegalStateException();
        }
        if (this.mIsReadOnly) {
            this.mReadOnlyWarningView.setText(getString(R.string.contact_read_only));
            this.mReadOnlyWarningView.setVisibility(0);
            this.mEditorContainerView.setVisibility(8);
            findViewById(R.id.btn_done).setVisibility(8);
            showDialogContent();
            return;
        }
        for (DataKind kind : this.mEditableAccountType.getSortedDataKinds()) {
            if (kind.editable && this.mMimetype.equals(kind.mimeType) && (deltas = this.mRawContactDelta.getMimeEntries(this.mMimetype)) != null) {
                for (ValuesDelta valuesDelta : deltas) {
                    if (valuesDelta.isVisible() && valuesDelta.isInsert()) {
                        inflateEditorView(kind, valuesDelta, this.mRawContactDelta);
                        return;
                    }
                }
            }
        }
    }

    private void inflateEditorView(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta state) {
        int layoutResId = EditorUiUtils.getLayoutResourceId(dataKind.mimeType);
        View viewInflate = this.mInflater.inflate(layoutResId, this.mEditorContainerView, false);
        if (viewInflate instanceof Editor) {
            Editor editor = (Editor) viewInflate;
            editor.setDeletable(false);
            editor.setValues(dataKind, valuesDelta, state, false, new ViewIdGenerator());
        }
        this.mEditorContainerView.addView(viewInflate);
    }

    private void setDisplayName() {
        this.mDisplayNameView.setText(this.mDisplayName);
    }

    private void setExtraInfoField(String value) {
        TextView extraTextView = (TextView) findViewById(R.id.extra_info);
        extraTextView.setVisibility(0);
        extraTextView.setText(value);
    }

    private void setDefaultContactImage(String displayName, String lookupKey) {
        this.mPhotoView.setImageDrawable(ContactPhotoManager.getDefaultAvatarDrawableForContact(getResources(), false, new ContactPhotoManager.DefaultImageRequest(displayName, lookupKey, false)));
    }

    private void showDialogContent() {
        this.mRootView.setVisibility(0);
    }

    private void doSaveAction() {
        PersistTask task = new PersistTask(this, this.mAccountTypeManager);
        task.execute(this.mEntityDeltaList);
    }

    private static class PersistTask extends AsyncTask<RawContactDeltaList, Void, Integer> {
        private ConfirmAddDetailActivity activityTarget;
        private AccountTypeManager mAccountTypeManager;

        public PersistTask(ConfirmAddDetailActivity target, AccountTypeManager accountTypeManager) {
            this.activityTarget = target;
            this.mAccountTypeManager = accountTypeManager;
        }

        @Override
        protected void onPreExecute() {
            WeakReference unused = ConfirmAddDetailActivity.sProgressDialog = new WeakReference(ProgressDialog.show(this.activityTarget, null, this.activityTarget.getText(R.string.savingContact)));
            Context context = this.activityTarget;
            context.startService(new Intent(context, (Class<?>) EmptyService.class));
        }

        @Override
        protected Integer doInBackground(RawContactDeltaList... params) {
            Context context = this.activityTarget;
            ContentResolver resolver = context.getContentResolver();
            RawContactDeltaList state = params[0];
            if (state == null) {
                return 2;
            }
            RawContactModifier.trimEmpty(state, this.mAccountTypeManager);
            int i = 2;
            int i2 = 0 + 1;
            if (0 < 3) {
                try {
                    ArrayList<ContentProviderOperation> diff = state.buildDiff();
                    if (!diff.isEmpty()) {
                        resolver.applyBatch("com.android.contacts", diff);
                    }
                    Integer result = Integer.valueOf(diff.size() > 0 ? 1 : 0);
                    return result;
                } catch (OperationApplicationException e) {
                    Log.e("ConfirmAdd", "Version consistency failed", e);
                    return i;
                } catch (RemoteException e2) {
                    Log.e("ConfirmAdd", "Problem persisting user edits", e2);
                    return i;
                }
            }
            return i;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Context context = this.activityTarget;
            ConfirmAddDetailActivity.dismissProgressDialog();
            if (result.intValue() == 1) {
                Toast.makeText(context, R.string.contactSavedToast, 0).show();
            } else if (result.intValue() == 2) {
                Toast.makeText(context, R.string.contactSavedErrorToast, 1).show();
            }
            context.stopService(new Intent(context, (Class<?>) EmptyService.class));
            this.activityTarget.onSaveCompleted(result.intValue() != 2);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        dismissProgressDialog();
    }

    private static void dismissProgressDialog() {
        ProgressDialog dialog = sProgressDialog == null ? null : sProgressDialog.get();
        if (dialog != null) {
            dialog.dismiss();
        }
        sProgressDialog = null;
    }

    private void onSaveCompleted(boolean success) {
        if (success) {
            Intent intent = new Intent("android.intent.action.VIEW", this.mContactUri);
            setResult(-1, intent);
        } else {
            setResult(0);
        }
        finish();
    }
}
