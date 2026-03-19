package com.android.contacts.activities;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorUtils;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.Contact;
import com.android.contacts.model.ContactLoader;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountInfo;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.ContactPhotoUtils;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mediatek.contacts.util.DrmUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import java.io.FileNotFoundException;
import java.util.List;

public class AttachPhotoActivity extends ContactsActivity {
    private static final String TAG = AttachPhotoActivity.class.getSimpleName();
    private static int mPhotoDim;
    private ListenableFuture<List<AccountInfo>> mAccountsFuture;
    private Uri mContactUri;
    private ContentResolver mContentResolver;
    private Uri mCroppedPhotoUri;
    private Uri mTempPhotoUri;

    private interface Listener {
        void onContactLoaded(Contact contact);
    }

    @Override
    public void onCreate(Bundle bundle) {
        Cursor cursorQuery;
        super.onCreate(bundle);
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        if (bundle != null) {
            String string = bundle.getString("contact_uri");
            this.mContactUri = string == null ? null : Uri.parse(string);
            this.mTempPhotoUri = Uri.parse(bundle.getString("temp_photo_uri"));
            this.mCroppedPhotoUri = Uri.parse(bundle.getString("cropped_photo_uri"));
        } else {
            this.mTempPhotoUri = ContactPhotoUtils.generateTempImageUri(this);
            this.mCroppedPhotoUri = ContactPhotoUtils.generateTempCroppedImageUri(this);
            Intent intent = new Intent("android.intent.action.PICK");
            intent.setType("vnd.android.cursor.dir/contact");
            intent.setPackage(getPackageName());
            intent.putExtra("account_type", 1);
            startActivityForResult(intent, 1);
        }
        this.mContentResolver = getContentResolver();
        if (mPhotoDim == 0 && (cursorQuery = this.mContentResolver.query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"display_max_dim"}, null, null, null)) != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    mPhotoDim = cursorQuery.getInt(0);
                }
            } finally {
                cursorQuery.close();
            }
        }
        this.mAccountsFuture = AccountTypeManager.getInstance(this).filterAccountsAsync(AccountTypeManager.writableFilter());
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mContactUri != null) {
            bundle.putString("contact_uri", this.mContactUri.toString());
        }
        if (this.mTempPhotoUri != null) {
            bundle.putString("temp_photo_uri", this.mTempPhotoUri.toString());
        }
        if (this.mCroppedPhotoUri != null) {
            bundle.putString("cropped_photo_uri", this.mCroppedPhotoUri.toString());
        }
    }

    private void grantUriWritePermission(Intent intent, Uri uri) {
        for (ResolveInfo resolveInfo : getPackageManager().queryIntentActivities(intent, 65536)) {
            Log.d(TAG, "[grantUriWritePermission] targetUri=" + uri + ", package=" + resolveInfo.getComponentInfo().packageName);
            grantUriPermission(resolveInfo.getComponentInfo().packageName, uri, 2);
        }
    }

    private void revokeUriWritePermission(Uri uri) {
        List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(new Intent("com.android.camera.action.CROP", this.mCroppedPhotoUri), 65536);
        Log.d(TAG, "[revokeUriWritePermission] resolveInfo=" + listQueryIntentActivities);
        for (ResolveInfo resolveInfo : listQueryIntentActivities) {
            Log.d(TAG, "[revokeUriWritePermission] targetUri=" + uri + ", package=" + resolveInfo.getComponentInfo().packageName);
            revokeUriPermission(resolveInfo.getComponentInfo().packageName, uri, 2);
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        AccountWithDataSet accountWithDataSet;
        Log.i(TAG, "[onActivityResult],requestCode:" + i + ",resultCode:" + i2 + ",result:" + intent);
        if (i == 3) {
            if (i2 != -1) {
                Log.w(TAG, "account selector was not successful");
                finish();
                return;
            } else if (intent != null && (accountWithDataSet = (AccountWithDataSet) intent.getParcelableExtra("android.provider.extra.ACCOUNT")) != null) {
                createNewRawContact(accountWithDataSet);
                return;
            } else {
                createNewRawContact(null);
                return;
            }
        }
        if (i == 1) {
            if (i2 != -1) {
                finish();
                return;
            }
            Intent intent2 = getIntent();
            Uri data = intent2.getData();
            Uri uri = DrmUtils.isDrmImage(this, data) ? data : null;
            if (uri == null) {
                if (!ContactPhotoUtils.savePhotoFromUriToUri(this, data, this.mTempPhotoUri, false)) {
                    finish();
                    return;
                }
                uri = this.mTempPhotoUri;
            }
            Log.d(TAG, "[onActivityResult] inputUri:" + data + ",toCrop" + uri);
            Intent intent3 = new Intent("com.android.camera.action.CROP", uri);
            if (intent2.getStringExtra("mimeType") != null) {
                intent3.setDataAndType(uri, intent2.getStringExtra("mimeType"));
            }
            ContactPhotoUtils.addPhotoPickerExtras(intent3, this.mCroppedPhotoUri);
            ContactPhotoUtils.addCropExtras(intent3, mPhotoDim != 0 ? mPhotoDim : 720);
            if (!hasIntentHandler(intent3)) {
                this.mCroppedPhotoUri = this.mTempPhotoUri;
                this.mContactUri = intent.getData();
                loadContact(this.mContactUri, new Listener() {
                    @Override
                    public void onContactLoaded(Contact contact) {
                        AttachPhotoActivity.this.saveContact(contact);
                    }
                });
                return;
            }
            try {
                if ((intent2.getFlags() & 2) == 0 && uri == data) {
                    intent3.removeFlags(2);
                    grantUriWritePermission(intent3, this.mCroppedPhotoUri);
                }
                startActivityForResult(intent3, 2);
                this.mContactUri = intent.getData();
                return;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.missing_app, 0).show();
                return;
            }
        }
        if (i == 2) {
            revokeUriWritePermission(this.mCroppedPhotoUri);
            getContentResolver().delete(this.mTempPhotoUri, null, null);
            if (i2 != -1) {
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

    private boolean hasIntentHandler(Intent intent) {
        List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(intent, 65536);
        return listQueryIntentActivities != null && listQueryIntentActivities.size() > 0;
    }

    private void loadContact(Uri uri, final Listener listener) {
        ContactLoader contactLoader = new ContactLoader(this, uri, true);
        contactLoader.registerListener(0, new Loader.OnLoadCompleteListener<Contact>() {
            @Override
            public void onLoadComplete(Loader<Contact> loader, Contact contact) {
                try {
                    loader.reset();
                } catch (RuntimeException e) {
                    Log.e(AttachPhotoActivity.TAG, "Error resetting loader", e);
                }
                listener.onContactLoaded(contact);
            }
        });
        contactLoader.startLoading();
    }

    private void saveContact(Contact contact) {
        if (contact.getRawContacts() == null) {
            Log.w(TAG, "No raw contacts found for contact");
            finish();
            return;
        }
        RawContactDeltaList rawContactDeltaListCreateRawContactDeltaList = contact.createRawContactDeltaList();
        if (rawContactDeltaListCreateRawContactDeltaList == null) {
            Log.w(TAG, "[saveContact]no writable raw-contact found");
            MtkToast.toast(getApplicationContext(), R.string.invalidContactMessage);
            finish();
        } else {
            RawContactDelta firstWritableRawContact = rawContactDeltaListCreateRawContactDeltaList.getFirstWritableRawContact(this);
            if (firstWritableRawContact == null) {
                selectAccountAndCreateContact();
            } else {
                saveToContact(contact, rawContactDeltaListCreateRawContactDeltaList, firstWritableRawContact);
            }
        }
    }

    private void saveToContact(Contact contact, RawContactDeltaList rawContactDeltaList, RawContactDelta rawContactDelta) {
        int thumbnailSize = ContactsUtils.getThumbnailSize(this);
        try {
            Bitmap bitmapFromUri = ContactPhotoUtils.getBitmapFromUri(this, this.mCroppedPhotoUri);
            if (bitmapFromUri == null) {
                Log.w(TAG, "Could not decode bitmap");
                finish();
                return;
            }
            byte[] bArrCompressBitmap = ContactPhotoUtils.compressBitmap(Bitmap.createScaledBitmap(bitmapFromUri, thumbnailSize, thumbnailSize, false));
            if (bArrCompressBitmap == null) {
                Log.w(TAG, "could not create scaled and compressed Bitmap");
                finish();
                return;
            }
            ValuesDelta valuesDeltaEnsureKindExists = RawContactModifier.ensureKindExists(rawContactDelta, rawContactDelta.getRawContactAccountType(this), "vnd.android.cursor.item/photo");
            if (valuesDeltaEnsureKindExists == null) {
                Log.w(TAG, "cannot attach photo to this account type");
                finish();
                return;
            }
            valuesDeltaEnsureKindExists.setPhoto(bArrCompressBitmap);
            if (Log.isLoggable(TAG, 2)) {
                Log.v(TAG, "all prerequisites met, about to save photo to contact");
            }
            ContactSaveService.startService(this, ContactSaveService.createSaveContactIntent(this, rawContactDeltaList, "", 0, contact.isUserProfile(), null, null, rawContactDelta.getRawContactId() != null ? rawContactDelta.getRawContactId().longValue() : -1L, this.mCroppedPhotoUri));
            finish();
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Could not find bitmap");
            finish();
        }
    }

    private void selectAccountAndCreateContact() {
        Preconditions.checkNotNull(this.mAccountsFuture, "Accounts future must be initialized first");
        ContactEditorUtils contactEditorUtilsCreate = ContactEditorUtils.create(this);
        List<AccountWithDataSet> listExtractAccounts = AccountInfo.extractAccounts((List) Futures.getUnchecked(this.mAccountsFuture));
        if (contactEditorUtilsCreate.shouldShowAccountChangedNotification(listExtractAccounts)) {
            startActivityForResult(new Intent(this, (Class<?>) ContactEditorAccountsChangedActivity.class).addFlags(603979776), 3);
        } else {
            createNewRawContact(contactEditorUtilsCreate.getOnlyOrDefaultAccount(listExtractAccounts));
        }
    }

    private void createNewRawContact(final AccountWithDataSet accountWithDataSet) {
        loadContact(this.mContactUri, new Listener() {
            @Override
            public void onContactLoaded(Contact contact) {
                RawContactDeltaList rawContactDeltaListCreateRawContactDeltaList = contact.createRawContactDeltaList();
                ContentValues contentValues = new ContentValues();
                contentValues.put("account_type", accountWithDataSet != null ? accountWithDataSet.type : null);
                contentValues.put("account_name", accountWithDataSet != null ? accountWithDataSet.name : null);
                contentValues.put("data_set", accountWithDataSet != null ? accountWithDataSet.dataSet : null);
                RawContactDelta rawContactDelta = new RawContactDelta(ValuesDelta.fromAfter(contentValues));
                rawContactDeltaListCreateRawContactDeltaList.add(rawContactDelta);
                AttachPhotoActivity.this.saveToContact(contact, rawContactDeltaListCreateRawContactDeltaList, rawContactDelta);
            }
        });
    }
}
