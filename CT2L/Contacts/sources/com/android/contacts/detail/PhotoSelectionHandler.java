package com.android.contacts.detail;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.editor.PhotoActionPopup;
import com.android.contacts.util.ContactPhotoUtils;
import com.android.contacts.util.UiClosables;
import java.io.FileNotFoundException;

public abstract class PhotoSelectionHandler implements View.OnClickListener {
    private static final String TAG = PhotoSelectionHandler.class.getSimpleName();
    private static int mPhotoDim;
    private final View mChangeAnchorView;
    protected final Context mContext;
    private final Uri mCroppedPhotoUri;
    private final boolean mIsDirectoryContact;
    private final int mPhotoMode;
    private final int mPhotoPickSize = getPhotoPickSize();
    private ListPopupWindow mPopup;
    private final RawContactDeltaList mState;
    private final Uri mTempPhotoUri;

    public abstract PhotoActionListener getListener();

    protected abstract void startPhotoActivity(Intent intent, int i, Uri uri);

    public PhotoSelectionHandler(Context context, View changeAnchorView, int photoMode, boolean isDirectoryContact, RawContactDeltaList state) {
        this.mContext = context;
        this.mChangeAnchorView = changeAnchorView;
        this.mPhotoMode = photoMode;
        this.mTempPhotoUri = ContactPhotoUtils.generateTempImageUri(context);
        this.mCroppedPhotoUri = ContactPhotoUtils.generateTempCroppedImageUri(this.mContext);
        this.mIsDirectoryContact = isDirectoryContact;
        this.mState = state;
    }

    public void destroy() {
        UiClosables.closeQuietly(this.mPopup);
    }

    @Override
    public void onClick(View v) {
        final PhotoActionListener listener = getListener();
        if (listener != null && getWritableEntityIndex() != -1) {
            this.mPopup = PhotoActionPopup.createPopupMenu(this.mContext, this.mChangeAnchorView, listener, this.mPhotoMode);
            this.mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    listener.onPhotoSelectionDismissed();
                }
            });
            this.mPopup.show();
        }
    }

    public boolean handlePhotoActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        Uri toCrop;
        Uri uri2;
        PhotoActionListener listener = getListener();
        if (resultCode == -1) {
            switch (requestCode) {
                case 1001:
                case 1002:
                    boolean isWritable = false;
                    if (data != null && data.getData() != null) {
                        uri = data.getData();
                    } else {
                        uri = listener.getCurrentPhotoUri();
                        isWritable = true;
                    }
                    if (isWritable) {
                        toCrop = uri;
                    } else {
                        toCrop = this.mTempPhotoUri;
                        try {
                            ContactPhotoUtils.savePhotoFromUriToUri(this.mContext, uri, toCrop, false);
                        } catch (SecurityException e) {
                            Log.d(TAG, "Did not have read-access to uri : " + uri);
                            return false;
                        }
                    }
                    doCropPhoto(toCrop, this.mCroppedPhotoUri);
                    break;
                case 1003:
                    if (data != null && data.getData() != null) {
                        uri2 = data.getData();
                    } else {
                        uri2 = this.mCroppedPhotoUri;
                    }
                    try {
                        this.mContext.getContentResolver().delete(this.mTempPhotoUri, null, null);
                        listener.onPhotoSelected(uri2);
                    } catch (FileNotFoundException e2) {
                        return false;
                    }
                    break;
            }
            return false;
        }
        return false;
    }

    private int getWritableEntityIndex() {
        if (this.mIsDirectoryContact) {
            return -1;
        }
        return this.mState.indexOfFirstWritableRawContact(this.mContext);
    }

    protected long getWritableEntityId() {
        int index = getWritableEntityIndex();
        if (index == -1) {
            return -1L;
        }
        return this.mState.get(index).getValues().getId().longValue();
    }

    public RawContactDeltaList getDeltaForAttachingPhotoToContact() {
        int writableEntityIndex = getWritableEntityIndex();
        if (writableEntityIndex == -1) {
            return null;
        }
        RawContactDelta delta = this.mState.get(writableEntityIndex);
        ContentValues entityValues = delta.getValues().getCompleteValues();
        String type = entityValues.getAsString("account_type");
        String dataSet = entityValues.getAsString("data_set");
        AccountType accountType = AccountTypeManager.getInstance(this.mContext).getAccountType(type, dataSet);
        ValuesDelta child = RawContactModifier.ensureKindExists(delta, accountType, "vnd.android.cursor.item/photo");
        child.setFromTemplate(false);
        child.setSuperPrimary(true);
        return this.mState;
    }

    private void doCropPhoto(Uri inputUri, Uri outputUri) {
        try {
            Intent intent = getCropImageIntent(inputUri, outputUri);
            startPhotoActivity(intent, 1003, inputUri);
        } catch (Exception e) {
            Log.e(TAG, "Cannot crop image", e);
            Toast.makeText(this.mContext, R.string.photoPickerNotFoundText, 1).show();
        }
    }

    private void startTakePhotoActivity(Uri photoUri) {
        Intent intent = getTakePhotoIntent(photoUri);
        startPhotoActivity(intent, 1001, photoUri);
    }

    private void startPickFromGalleryActivity(Uri photoUri) {
        Intent intent = getPhotoPickIntent(photoUri);
        startPhotoActivity(intent, 1002, photoUri);
    }

    private int getPhotoPickSize() {
        if (mPhotoDim != 0) {
            return mPhotoDim;
        }
        Cursor c = this.mContext.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"display_max_dim"}, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    mPhotoDim = c.getInt(0);
                }
            } finally {
                c.close();
            }
        }
        if (mPhotoDim != 0) {
            return mPhotoDim;
        }
        return 720;
    }

    private Intent getTakePhotoIntent(Uri outputUri) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE", (Uri) null);
        ContactPhotoUtils.addPhotoPickerExtras(intent, outputUri);
        return intent;
    }

    private Intent getPhotoPickIntent(Uri outputUri) {
        Intent intent = new Intent("android.intent.action.GET_CONTENT", (Uri) null);
        intent.setType("image/*");
        ContactPhotoUtils.addPhotoPickerExtras(intent, outputUri);
        return intent;
    }

    private Intent getCropImageIntent(Uri inputUri, Uri outputUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(inputUri, "image/*");
        ContactPhotoUtils.addPhotoPickerExtras(intent, outputUri);
        ContactPhotoUtils.addCropExtras(intent, this.mPhotoPickSize);
        return intent;
    }

    public abstract class PhotoActionListener implements PhotoActionPopup.Listener {
        public abstract Uri getCurrentPhotoUri();

        public abstract void onPhotoSelected(Uri uri) throws FileNotFoundException;

        public abstract void onPhotoSelectionDismissed();

        public PhotoActionListener() {
        }

        @Override
        public void onRemovePictureChosen() {
        }

        @Override
        public void onTakePhotoChosen() {
            try {
                PhotoSelectionHandler.this.startTakePhotoActivity(PhotoSelectionHandler.this.mTempPhotoUri);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(PhotoSelectionHandler.this.mContext, R.string.photoPickerNotFoundText, 1).show();
            }
        }

        @Override
        public void onPickFromGalleryChosen() {
            try {
                PhotoSelectionHandler.this.startPickFromGalleryActivity(PhotoSelectionHandler.this.mTempPhotoUri);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(PhotoSelectionHandler.this.mContext, R.string.photoPickerNotFoundText, 1).show();
            }
        }
    }
}
