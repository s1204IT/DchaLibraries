package com.android.contacts.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.editor.ContactEditorUtils;
import com.android.contacts.util.ContactPhotoUtils;
import com.google.common.collect.Lists;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class AttachPhotoActivity extends ContactsActivity {
    private static final String TAG = AttachPhotoActivity.class.getSimpleName();
    private static int mPhotoDim;
    private Uri mContactUri;
    private ContentResolver mContentResolver;
    private Uri mCroppedPhotoUri;
    private Uri mTempPhotoUri;

    private interface Listener {
        void onContactLoaded(Contact contact);
    }

    @Override
    public void onCreate(Bundle icicle) {
        Cursor c;
        super.onCreate(icicle);
        ArrayList<ContactListFilter> accountFilters = Lists.newArrayList();
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(this);
        List<AccountWithDataSet> accounts = accountTypes.getAccounts(true);
        for (AccountWithDataSet account : accounts) {
            AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            Drawable icon = accountType != null ? accountType.getDisplayIcon(this) : null;
            if (account.type != "com.android.contact.sim" && account.type != "com.android.contact.sim2") {
                accountFilters.add(ContactListFilter.createAccountFilter(account.type, account.name, account.dataSet, icon));
                Log.v(TAG, "Add ContactListFilter : " + account.type);
            }
        }
        if (icicle != null) {
            String uri = icicle.getString("contact_uri");
            this.mContactUri = uri == null ? null : Uri.parse(uri);
            this.mTempPhotoUri = Uri.parse(icicle.getString("temp_photo_uri"));
            this.mCroppedPhotoUri = Uri.parse(icicle.getString("cropped_photo_uri"));
        } else {
            this.mTempPhotoUri = ContactPhotoUtils.generateTempImageUri(this);
            this.mCroppedPhotoUri = ContactPhotoUtils.generateTempCroppedImageUri(this);
            Intent intent = new Intent("android.intent.action.PICK");
            intent.setType("vnd.android.cursor.dir/contact");
            intent.putParcelableArrayListExtra("CONTACT_LIST_FILTER", accountFilters);
            startActivityForResult(intent, 1);
        }
        this.mContentResolver = getContentResolver();
        if (mPhotoDim == 0 && (c = this.mContentResolver.query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"display_max_dim"}, null, null, null)) != null) {
            try {
                if (c.moveToFirst()) {
                    mPhotoDim = c.getInt(0);
                }
            } finally {
                c.close();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mContactUri != null) {
            outState.putString("contact_uri", this.mContactUri.toString());
        }
        if (this.mTempPhotoUri != null) {
            outState.putString("temp_photo_uri", this.mTempPhotoUri.toString());
        }
        if (this.mCroppedPhotoUri != null) {
            outState.putString("cropped_photo_uri", this.mCroppedPhotoUri.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        AccountWithDataSet account;
        if (requestCode == 3) {
            if (resultCode != -1) {
                Log.w(TAG, "account selector was not successful");
                finish();
                return;
            } else if (result != null && (account = (AccountWithDataSet) result.getParcelableExtra("com.android.contacts.extra.ACCOUNT")) != null) {
                createNewRawContact(account);
                return;
            } else {
                createNewRawContact(null);
                return;
            }
        }
        if (requestCode == 1) {
            if (resultCode != -1) {
                finish();
                return;
            }
            Intent myIntent = getIntent();
            Uri inputUri = myIntent.getData();
            ContactPhotoUtils.savePhotoFromUriToUri(this, inputUri, this.mTempPhotoUri, false);
            Uri toCrop = this.mTempPhotoUri;
            Intent intent = new Intent("com.android.camera.action.CROP", toCrop);
            if (myIntent.getStringExtra("mimeType") != null) {
                intent.setDataAndType(toCrop, myIntent.getStringExtra("mimeType"));
            }
            ContactPhotoUtils.addPhotoPickerExtras(intent, this.mCroppedPhotoUri);
            ContactPhotoUtils.addCropExtras(intent, mPhotoDim != 0 ? mPhotoDim : 720);
            try {
                startActivityForResult(intent, 2);
                this.mContactUri = result.getData();
                return;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.missing_app, 0).show();
                return;
            }
        }
        if (requestCode == 2) {
            getContentResolver().delete(this.mTempPhotoUri, null, null);
            if (resultCode != -1) {
                finish();
            } else {
                loadContact(this.mContactUri, new Listener() {
                    @Override
                    public void onContactLoaded(Contact contact) {
                        AttachPhotoActivity.this.saveContact(contact);
                    }
                });
            }
        }
    }

    private void loadContact(Uri contactUri, final Listener listener) {
        ContactLoader loader = new ContactLoader(this, contactUri, true);
        loader.registerListener(0, new Loader.OnLoadCompleteListener<Contact>() {
            @Override
            public void onLoadComplete(Loader<Contact> loader2, Contact contact) {
                try {
                    loader2.reset();
                } catch (RuntimeException e) {
                    Log.e(AttachPhotoActivity.TAG, "Error resetting loader", e);
                }
                listener.onContactLoaded(contact);
            }
        });
        loader.startLoading();
    }

    private void saveContact(Contact contact) {
        if (contact.getRawContacts() == null) {
            Log.w(TAG, "No raw contacts found for contact");
            finish();
            return;
        }
        RawContactDeltaList deltaList = contact.createRawContactDeltaList();
        RawContactDelta raw = deltaList.getFirstWritableRawContact(this);
        if (raw == null) {
            selectAccountAndCreateContact();
        } else {
            saveToContact(contact, deltaList, raw);
        }
    }

    private void saveToContact(Contact contact, RawContactDeltaList deltaList, RawContactDelta raw) {
        int size = ContactsUtils.getThumbnailSize(this);
        try {
            Bitmap bitmap = ContactPhotoUtils.getBitmapFromUri(this, this.mCroppedPhotoUri);
            if (bitmap == null) {
                Log.w(TAG, "Could not decode bitmap");
                finish();
                return;
            }
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, size, size, false);
            byte[] compressed = ContactPhotoUtils.compressBitmap(scaled);
            if (compressed == null) {
                Log.w(TAG, "could not create scaled and compressed Bitmap");
                finish();
                return;
            }
            AccountType account = raw.getRawContactAccountType(this);
            ValuesDelta values = RawContactModifier.ensureKindExists(raw, account, "vnd.android.cursor.item/photo");
            if (values == null) {
                Log.w(TAG, "cannot attach photo to this account type");
                finish();
                return;
            }
            values.setPhoto(compressed);
            Log.v(TAG, "all prerequisites met, about to save photo to contact");
            Intent intent = ContactSaveService.createSaveContactIntent(this, deltaList, (RawContactDeltaList) null, "", 0, contact.isUserProfile(), (Class<? extends Activity>) null, (String) null, raw.getRawContactId() != null ? raw.getRawContactId().longValue() : -1L, this.mCroppedPhotoUri);
            startService(intent);
            finish();
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Could not find bitmap");
            finish();
        }
    }

    private void selectAccountAndCreateContact() {
        ContactEditorUtils editorUtils = ContactEditorUtils.getInstance(this);
        if (editorUtils.shouldShowAccountChangedNotification()) {
            Intent intent = new Intent(this, (Class<?>) ContactEditorAccountsChangedActivity.class);
            startActivityForResult(intent, 3);
            return;
        }
        AccountWithDataSet defaultAccount = editorUtils.getDefaultAccount();
        if (defaultAccount == null) {
            createNewRawContact(null);
        } else {
            createNewRawContact(defaultAccount);
        }
    }

    private void createNewRawContact(final AccountWithDataSet account) {
        loadContact(this.mContactUri, new Listener() {
            @Override
            public void onContactLoaded(Contact contactToSave) {
                RawContactDeltaList deltaList = contactToSave.createRawContactDeltaList();
                ContentValues after = new ContentValues();
                after.put("account_type", account != null ? account.type : null);
                after.put("account_name", account != null ? account.name : null);
                after.put("data_set", account != null ? account.dataSet : null);
                RawContactDelta newRawContactDelta = new RawContactDelta(ValuesDelta.fromAfter(after));
                deltaList.add(newRawContactDelta);
                AttachPhotoActivity.this.saveToContact(contactToSave, deltaList, newRawContactDelta);
            }
        });
    }
}
