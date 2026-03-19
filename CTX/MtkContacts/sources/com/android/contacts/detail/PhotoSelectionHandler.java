package com.android.contacts.detail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.Toast;
import com.android.contacts.R;
import com.android.contacts.editor.PhotoActionPopup;
import com.android.contacts.model.RawContactDeltaList;
import com.android.contacts.util.ContactPhotoUtils;
import com.mediatek.contacts.ContactsSystemProperties;
import com.mediatek.contacts.util.DrmUtils;
import com.mediatek.contacts.util.Log;
import java.io.FileNotFoundException;
import java.util.List;

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

    public PhotoSelectionHandler(Context context, View view, int i, boolean z, RawContactDeltaList rawContactDeltaList) {
        this.mContext = context;
        this.mChangeAnchorView = view;
        this.mPhotoMode = i;
        this.mTempPhotoUri = ContactPhotoUtils.generateTempImageUri(context);
        this.mCroppedPhotoUri = ContactPhotoUtils.generateTempCroppedImageUri(this.mContext);
        this.mIsDirectoryContact = z;
        this.mState = rawContactDeltaList;
    }

    @Override
    public void onClick(View view) {
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

    public boolean handlePhotoActivityResult(int i, int i2, Intent intent) {
        Uri currentPhotoUri;
        Uri uri;
        boolean z;
        Uri uri2;
        Log.d(TAG, "[handlePhotoActivityResult] requestCode: " + i + ", resultCode: " + i2);
        PhotoActionListener listener = getListener();
        if (i2 == -1) {
            switch (i) {
                case 1001:
                case 1002:
                    if (intent != null && intent.getData() != null) {
                        currentPhotoUri = intent.getData();
                        Log.d(TAG, "[handlePhotoActivityResult] uri:" + currentPhotoUri);
                        if (!DrmUtils.isDrmImage(this.mContext, currentPhotoUri)) {
                            uri = currentPhotoUri;
                            z = false;
                        }
                        if (z) {
                            uri2 = this.mTempPhotoUri;
                            try {
                                if (!ContactPhotoUtils.savePhotoFromUriToUri(this.mContext, uri, uri2, false)) {
                                    return false;
                                }
                            } catch (SecurityException e) {
                                if (Log.isLoggable(TAG, 3)) {
                                    Log.d(TAG, "Did not have read-access to uri : " + uri);
                                }
                                return false;
                            }
                        } else {
                            uri2 = uri;
                        }
                        doCropPhoto(uri2, this.mCroppedPhotoUri);
                        return true;
                    }
                    currentPhotoUri = listener.getCurrentPhotoUri();
                    uri = currentPhotoUri;
                    z = true;
                    if (z) {
                    }
                    doCropPhoto(uri2, this.mCroppedPhotoUri);
                    return true;
                case 1003:
                    if (intent != null && intent.getData() != null) {
                        ContactPhotoUtils.savePhotoFromUriToUri(this.mContext, intent.getData(), this.mCroppedPhotoUri, false);
                    }
                    try {
                        this.mContext.getContentResolver().delete(this.mTempPhotoUri, null, null);
                        listener.onPhotoSelected(this.mCroppedPhotoUri);
                        return true;
                    } catch (FileNotFoundException e2) {
                        return false;
                    }
            }
        }
        return false;
    }

    private int getWritableEntityIndex() {
        if (this.mIsDirectoryContact) {
            return -1;
        }
        return this.mState.indexOfFirstWritableRawContact(this.mContext);
    }

    private void doCropPhoto(Uri uri, Uri uri2) {
        Intent cropImageIntent = getCropImageIntent(uri, uri2);
        if (!hasIntentHandler(cropImageIntent)) {
            try {
                getListener().onPhotoSelected(uri);
                return;
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Cannot save uncropped photo", e);
                Toast.makeText(this.mContext, R.string.contactPhotoSavedErrorToast, 1).show();
                return;
            }
        }
        try {
            startPhotoActivity(cropImageIntent, 1003, uri);
        } catch (Exception e2) {
            Log.e(TAG, "Cannot crop image", e2);
            Toast.makeText(this.mContext, R.string.photoPickerNotFoundText, 1).show();
        }
    }

    private void startTakePhotoActivity(Uri uri) {
        startPhotoActivity(getTakePhotoIntent(uri), 1001, uri);
    }

    private void startPickFromGalleryActivity(Uri uri) {
        Intent photoPickIntent = getPhotoPickIntent(uri);
        if (ContactsSystemProperties.MTK_DRM_SUPPORT) {
            Log.d(TAG, "[startPickFromGalleryActivity] use DRM intent : " + photoPickIntent);
            photoPickIntent.putExtra("android.intent.extra.drm_level", 1);
        }
        startPhotoActivity(photoPickIntent, 1002, uri);
    }

    private int getPhotoPickSize() {
        if (mPhotoDim != 0) {
            return mPhotoDim;
        }
        Cursor cursorQuery = this.mContext.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"display_max_dim"}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    mPhotoDim = cursorQuery.getInt(0);
                }
            } finally {
                cursorQuery.close();
            }
        }
        if (mPhotoDim != 0) {
            return mPhotoDim;
        }
        return 720;
    }

    private Intent getTakePhotoIntent(Uri uri) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE", (Uri) null);
        ContactPhotoUtils.addPhotoPickerExtras(intent, uri);
        return intent;
    }

    private Intent getPhotoPickIntent(Uri uri) {
        Intent intent = new Intent("android.intent.action.PICK", (Uri) null);
        intent.setType("image/*");
        ContactPhotoUtils.addPhotoPickerExtras(intent, uri);
        return intent;
    }

    private boolean hasIntentHandler(Intent intent) {
        List<ResolveInfo> listQueryIntentActivities = this.mContext.getPackageManager().queryIntentActivities(intent, 65536);
        return listQueryIntentActivities != null && listQueryIntentActivities.size() > 0;
    }

    private Intent getCropImageIntent(Uri uri, Uri uri2) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        ContactPhotoUtils.addPhotoPickerExtras(intent, uri2);
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
