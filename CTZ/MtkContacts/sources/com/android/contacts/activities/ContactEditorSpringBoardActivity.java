package com.android.contacts.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import com.android.contacts.AppCompatContactsActivity;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.editor.PickRawContactDialogFragment;
import com.android.contacts.editor.PickRawContactLoader;
import com.android.contacts.editor.SplitContactConfirmationDialogFragment;
import com.android.contacts.logging.Logger;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.quickcontact.QuickContactActivity;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contactsbind.FeedbackHelper;
import java.lang.reflect.Array;

public class ContactEditorSpringBoardActivity extends AppCompatContactsActivity implements PickRawContactDialogFragment.PickRawContactListener, SplitContactConfirmationDialogFragment.Listener {
    private boolean mHasWritableAccount;
    private MaterialColorMapUtils.MaterialPalette mMaterialPalette;
    protected final LoaderManager.LoaderCallbacks<PickRawContactLoader.RawContactsMetadata> mRawContactLoaderListener = new LoaderManager.LoaderCallbacks<PickRawContactLoader.RawContactsMetadata>() {
        @Override
        public Loader<PickRawContactLoader.RawContactsMetadata> onCreateLoader(int i, Bundle bundle) {
            return new PickRawContactLoader(ContactEditorSpringBoardActivity.this, ContactEditorSpringBoardActivity.this.mUri);
        }

        @Override
        public void onLoadFinished(Loader<PickRawContactLoader.RawContactsMetadata> loader, PickRawContactLoader.RawContactsMetadata rawContactsMetadata) {
            if (rawContactsMetadata == null) {
                ContactEditorSpringBoardActivity.this.toastErrorAndFinish();
            } else {
                ContactEditorSpringBoardActivity.this.mResult = rawContactsMetadata;
                ContactEditorSpringBoardActivity.this.onLoad();
            }
        }

        @Override
        public void onLoaderReset(Loader<PickRawContactLoader.RawContactsMetadata> loader) {
        }
    };
    private PickRawContactLoader.RawContactsMetadata mResult;
    private boolean mShowReadOnly;
    private Uri mUri;
    private int mWritableAccountPosition;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        Intent intent = getIntent();
        if (!"android.intent.action.EDIT".equals(intent.getAction())) {
            finish();
            return;
        }
        if (intent.hasExtra("material_palette_primary_color") && intent.hasExtra("material_palette_secondary_color")) {
            this.mMaterialPalette = new MaterialColorMapUtils.MaterialPalette(intent.getIntExtra("material_palette_primary_color", -1), intent.getIntExtra("material_palette_secondary_color", -1));
        }
        this.mShowReadOnly = intent.getBooleanExtra("showReadOnly", false);
        this.mUri = intent.getData();
        String authority = this.mUri.getAuthority();
        String type = getContentResolver().getType(this.mUri);
        if ("com.android.contacts".equals(authority) && "vnd.android.cursor.item/raw_contact".equals(type)) {
            Logger.logEditorEvent(1, 0);
            startEditorAndForwardExtras(getIntentForRawContact(ContentUris.parseId(this.mUri)));
        } else if ("contacts".equals(authority)) {
            FeedbackHelper.sendFeedback(this, "EditorSpringBoard", "Legacy Uri was passed to editor.", new IllegalArgumentException());
            toastErrorAndFinish();
        } else {
            getLoaderManager().initLoader(1, null, this.mRawContactLoaderListener);
        }
    }

    @Override
    public void onPickRawContact(long j) {
        startEditorAndForwardExtras(getIntentForRawContact(j));
    }

    private void onLoad() {
        maybeTrimReadOnly();
        setHasWritableAccount();
        if (this.mShowReadOnly || (this.mResult.rawContacts.size() > 1 && this.mHasWritableAccount)) {
            showDialog();
        } else {
            loadEditor();
        }
    }

    private void maybeTrimReadOnly() {
        this.mResult.showReadOnly = this.mShowReadOnly;
        if (this.mShowReadOnly) {
            return;
        }
        this.mResult.trimReadOnly(AccountTypeManager.getInstance(this));
    }

    private void showDialog() {
        FragmentManager fragmentManager = getFragmentManager();
        SplitContactConfirmationDialogFragment splitContactConfirmationDialogFragment = (SplitContactConfirmationDialogFragment) fragmentManager.findFragmentByTag("SplitConfirmation");
        if (splitContactConfirmationDialogFragment != null && splitContactConfirmationDialogFragment.isAdded()) {
            fragmentManager.beginTransaction().show(splitContactConfirmationDialogFragment).commitAllowingStateLoss();
        } else if (((PickRawContactDialogFragment) fragmentManager.findFragmentByTag("rawContactsDialog")) == null) {
            PickRawContactDialogFragment pickRawContactDialogFragment = PickRawContactDialogFragment.getInstance(this.mResult);
            FragmentTransaction fragmentTransactionBeginTransaction = fragmentManager.beginTransaction();
            fragmentTransactionBeginTransaction.add(pickRawContactDialogFragment, "rawContactsDialog");
            fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        }
    }

    private void loadEditor() {
        Intent intentCreateEditContactIntent;
        Logger.logEditorEvent(1, 0);
        if (this.mHasWritableAccount) {
            intentCreateEditContactIntent = getIntentForRawContact(this.mResult.rawContacts.get(this.mWritableAccountPosition).id);
        } else {
            intentCreateEditContactIntent = EditorIntents.createEditContactIntent(this, this.mUri, this.mMaterialPalette, -1L);
            intentCreateEditContactIntent.setClass(this, ContactEditorActivity.class);
        }
        startEditorAndForwardExtras(intentCreateEditContactIntent);
    }

    private void setHasWritableAccount() {
        this.mWritableAccountPosition = this.mResult.getIndexOfFirstWritableAccount(AccountTypeManager.getInstance(this));
        this.mHasWritableAccount = this.mWritableAccountPosition != -1;
    }

    private Intent getIntentForRawContact(long j) {
        Intent intentCreateEditContactIntentForRawContact = EditorIntents.createEditContactIntentForRawContact(this, this.mUri, j, this.mMaterialPalette);
        intentCreateEditContactIntentForRawContact.setFlags(33554432);
        return intentCreateEditContactIntentForRawContact;
    }

    private void startEditorAndForwardExtras(Intent intent) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        ImplicitIntentsUtil.startActivityInApp(this, intent);
        finish();
    }

    private void toastErrorAndFinish() {
        Toast.makeText(this, R.string.editor_failed_to_load, 0).show();
        setResult(0, null);
        finish();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i2 != -1) {
            finish();
        }
        if (intent != null) {
            startService(ContactSaveService.createJoinContactsIntent(this, this.mResult.contactId, ContentUris.parseId(intent.getData()), QuickContactActivity.class, "android.intent.action.VIEW"));
            finish();
        }
    }

    @Override
    public void onSplitContactConfirmed(boolean z) {
        startService(ContactSaveService.createHardSplitContactIntent(this, getRawContactIds()));
        finish();
    }

    @Override
    public void onSplitContactCanceled() {
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("rawContactsMetadata", this.mResult);
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        this.mResult = (PickRawContactLoader.RawContactsMetadata) bundle.getParcelable("rawContactsMetadata");
    }

    private long[][] getRawContactIds() {
        long[][] jArr = (long[][]) Array.newInstance((Class<?>) long.class, this.mResult.rawContacts.size(), 1);
        for (int i = 0; i < this.mResult.rawContacts.size(); i++) {
            jArr[i][0] = this.mResult.rawContacts.get(i).id;
        }
        return jArr;
    }
}
