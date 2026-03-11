package com.android.settings.users;

import android.app.Fragment;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.drawable.CircleFramedDrawable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class EditUserPhotoController {
    private final Context mContext;
    private final Uri mCropPictureUri;
    private final Fragment mFragment;
    private final ImageView mImageView;
    private Bitmap mNewUserPhotoBitmap;
    private Drawable mNewUserPhotoDrawable;
    private final int mPhotoSize;
    private final Uri mTakePictureUri;

    public EditUserPhotoController(Fragment fragment, ImageView view, Bitmap bitmap, Drawable drawable, boolean waiting) {
        this.mContext = view.getContext();
        this.mFragment = fragment;
        this.mImageView = view;
        this.mCropPictureUri = createTempImageUri(this.mContext, "CropEditUserPhoto.jpg", !waiting);
        this.mTakePictureUri = createTempImageUri(this.mContext, "TakeEditUserPhoto2.jpg", waiting ? false : true);
        this.mPhotoSize = getPhotoSize(this.mContext);
        this.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditUserPhotoController.this.showUpdatePhotoPopup();
            }
        });
        this.mNewUserPhotoBitmap = bitmap;
        this.mNewUserPhotoDrawable = drawable;
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != -1) {
            return false;
        }
        Uri pictureUri = (data == null || data.getData() == null) ? this.mTakePictureUri : data.getData();
        switch (requestCode) {
            case 1001:
            case 1002:
                cropPhoto(pictureUri);
                break;
            case 1003:
                onPhotoCropped(pictureUri, true);
                break;
        }
        return false;
    }

    public Bitmap getNewUserPhotoBitmap() {
        return this.mNewUserPhotoBitmap;
    }

    public Drawable getNewUserPhotoDrawable() {
        return this.mNewUserPhotoDrawable;
    }

    public void showUpdatePhotoPopup() {
        boolean canTakePhoto = canTakePhoto();
        boolean canChoosePhoto = canChoosePhoto();
        if (!canTakePhoto && !canChoosePhoto) {
            return;
        }
        Context context = this.mImageView.getContext();
        List<RestrictedMenuItem> items = new ArrayList<>();
        if (canTakePhoto) {
            String title = context.getString(R.string.user_image_take_photo);
            Runnable action = new Runnable() {
                @Override
                public void run() {
                    EditUserPhotoController.this.takePhoto();
                }
            };
            items.add(new RestrictedMenuItem(context, title, "no_set_user_icon", action));
        }
        if (canChoosePhoto) {
            String title2 = context.getString(R.string.user_image_choose_photo);
            Runnable action2 = new Runnable() {
                @Override
                public void run() {
                    EditUserPhotoController.this.choosePhoto();
                }
            };
            items.add(new RestrictedMenuItem(context, title2, "no_set_user_icon", action2));
        }
        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        listPopupWindow.setAnchorView(this.mImageView);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(2);
        listPopupWindow.setAdapter(new RestrictedPopupMenuAdapter(context, items));
        int width = Math.max(this.mImageView.getWidth(), context.getResources().getDimensionPixelSize(R.dimen.update_user_photo_popup_min_width));
        listPopupWindow.setWidth(width);
        listPopupWindow.setDropDownGravity(8388611);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopupWindow.dismiss();
                RestrictedMenuItem item = (RestrictedMenuItem) parent.getAdapter().getItem(position);
                item.doAction();
            }
        });
        listPopupWindow.show();
    }

    private boolean canTakePhoto() {
        return this.mImageView.getContext().getPackageManager().queryIntentActivities(new Intent("android.media.action.IMAGE_CAPTURE"), 65536).size() > 0;
    }

    private boolean canChoosePhoto() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        return this.mImageView.getContext().getPackageManager().queryIntentActivities(intent, 0).size() > 0;
    }

    public void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        appendOutputExtra(intent, this.mTakePictureUri);
        this.mFragment.startActivityForResult(intent, 1002);
    }

    public void choosePhoto() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT", (Uri) null);
        intent.setType("image/*");
        appendOutputExtra(intent, this.mTakePictureUri);
        this.mFragment.startActivityForResult(intent, 1001);
    }

    private void cropPhoto(Uri pictureUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(pictureUri, "image/*");
        appendOutputExtra(intent, this.mCropPictureUri);
        appendCropExtras(intent);
        if (intent.resolveActivity(this.mContext.getPackageManager()) != null) {
            try {
                StrictMode.disableDeathOnFileUriExposure();
                this.mFragment.startActivityForResult(intent, 1003);
                return;
            } finally {
                StrictMode.enableDeathOnFileUriExposure();
            }
        }
        onPhotoCropped(pictureUri, false);
    }

    private void appendOutputExtra(Intent intent, Uri pictureUri) {
        intent.putExtra("output", pictureUri);
        intent.addFlags(3);
        intent.setClipData(ClipData.newRawUri("output", pictureUri));
    }

    private void appendCropExtras(Intent intent) {
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", this.mPhotoSize);
        intent.putExtra("outputY", this.mPhotoSize);
    }

    private void onPhotoCropped(final Uri data, final boolean cropped) {
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            public Bitmap doInBackground(Void... params) {
                if (cropped) {
                    InputStream imageStream = null;
                    try {
                        try {
                            imageStream = EditUserPhotoController.this.mContext.getContentResolver().openInputStream(data);
                            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(imageStream);
                            if (imageStream != null) {
                                try {
                                    imageStream.close();
                                } catch (IOException ioe) {
                                    Log.w("EditUserPhotoController", "Cannot close image stream", ioe);
                                }
                            }
                            return bitmapDecodeStream;
                        } catch (FileNotFoundException fe) {
                            Log.w("EditUserPhotoController", "Cannot find image file", fe);
                            if (imageStream != null) {
                                try {
                                    imageStream.close();
                                } catch (IOException ioe2) {
                                    Log.w("EditUserPhotoController", "Cannot close image stream", ioe2);
                                }
                            }
                            return null;
                        }
                    } catch (Throwable th) {
                        if (imageStream != null) {
                            try {
                                imageStream.close();
                            } catch (IOException ioe3) {
                                Log.w("EditUserPhotoController", "Cannot close image stream", ioe3);
                            }
                        }
                        throw th;
                    }
                }
                Bitmap croppedImage = Bitmap.createBitmap(EditUserPhotoController.this.mPhotoSize, EditUserPhotoController.this.mPhotoSize, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(croppedImage);
                try {
                    Bitmap fullImage = BitmapFactory.decodeStream(EditUserPhotoController.this.mContext.getContentResolver().openInputStream(data));
                    if (fullImage != null) {
                        int squareSize = Math.min(fullImage.getWidth(), fullImage.getHeight());
                        int left = (fullImage.getWidth() - squareSize) / 2;
                        int top = (fullImage.getHeight() - squareSize) / 2;
                        Rect rectSource = new Rect(left, top, left + squareSize, top + squareSize);
                        Rect rectDest = new Rect(0, 0, EditUserPhotoController.this.mPhotoSize, EditUserPhotoController.this.mPhotoSize);
                        Paint paint = new Paint();
                        canvas.drawBitmap(fullImage, rectSource, rectDest, paint);
                        return croppedImage;
                    }
                    return null;
                } catch (FileNotFoundException e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    EditUserPhotoController.this.mNewUserPhotoBitmap = bitmap;
                    EditUserPhotoController.this.mNewUserPhotoDrawable = CircleFramedDrawable.getInstance(EditUserPhotoController.this.mImageView.getContext(), EditUserPhotoController.this.mNewUserPhotoBitmap);
                    EditUserPhotoController.this.mImageView.setImageDrawable(EditUserPhotoController.this.mNewUserPhotoDrawable);
                }
                new File(EditUserPhotoController.this.mContext.getCacheDir(), "TakeEditUserPhoto2.jpg").delete();
                new File(EditUserPhotoController.this.mContext.getCacheDir(), "CropEditUserPhoto.jpg").delete();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    private static int getPhotoSize(Context context) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"display_max_dim"}, null, null, null);
        try {
            cursor.moveToFirst();
            return cursor.getInt(0);
        } finally {
            cursor.close();
        }
    }

    private Uri createTempImageUri(Context context, String fileName, boolean purge) {
        File folder = context.getCacheDir();
        folder.mkdirs();
        File fullPath = new File(folder, fileName);
        if (purge) {
            fullPath.delete();
        }
        return FileProvider.getUriForFile(context, "com.android.settings.files", fullPath);
    }

    File saveNewUserPhotoBitmap() {
        if (this.mNewUserPhotoBitmap == null) {
            return null;
        }
        try {
            File file = new File(this.mContext.getCacheDir(), "NewUserPhoto.png");
            OutputStream os = new FileOutputStream(file);
            this.mNewUserPhotoBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
            return file;
        } catch (IOException e) {
            Log.e("EditUserPhotoController", "Cannot create temp file", e);
            return null;
        }
    }

    static Bitmap loadNewUserPhotoBitmap(File file) {
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    void removeNewUserPhotoBitmapFile() {
        new File(this.mContext.getCacheDir(), "NewUserPhoto.png").delete();
    }

    private static final class RestrictedMenuItem {
        private final Runnable mAction;
        private final RestrictedLockUtils.EnforcedAdmin mAdmin;
        private final Context mContext;
        private final boolean mIsRestrictedByBase;
        private final String mTitle;

        public RestrictedMenuItem(Context context, String title, String restriction, Runnable action) {
            this.mContext = context;
            this.mTitle = title;
            this.mAction = action;
            int myUserId = UserHandle.myUserId();
            this.mAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(context, restriction, myUserId);
            this.mIsRestrictedByBase = RestrictedLockUtils.hasBaseUserRestriction(this.mContext, restriction, myUserId);
        }

        public String toString() {
            return this.mTitle;
        }

        final void doAction() {
            if (isRestrictedByBase()) {
                return;
            }
            if (isRestrictedByAdmin()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(this.mContext, this.mAdmin);
            } else {
                this.mAction.run();
            }
        }

        final boolean isRestrictedByAdmin() {
            return this.mAdmin != null;
        }

        final boolean isRestrictedByBase() {
            return this.mIsRestrictedByBase;
        }
    }

    private static final class RestrictedPopupMenuAdapter extends ArrayAdapter<RestrictedMenuItem> {
        public RestrictedPopupMenuAdapter(Context context, List<RestrictedMenuItem> items) {
            super(context, R.layout.restricted_popup_menu_item, R.id.text, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            RestrictedMenuItem item = getItem(position);
            TextView text = (TextView) view.findViewById(R.id.text);
            ImageView image = (ImageView) view.findViewById(R.id.restricted_icon);
            text.setEnabled((item.isRestrictedByAdmin() || item.isRestrictedByBase()) ? false : true);
            image.setVisibility((!item.isRestrictedByAdmin() || item.isRestrictedByBase()) ? 8 : 0);
            return view;
        }
    }
}
