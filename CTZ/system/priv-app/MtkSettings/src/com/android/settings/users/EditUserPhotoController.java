package com.android.settings.users;

import android.app.Fragment;
import android.content.ClipData;
import android.content.ContentResolver;
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
import android.util.EventLog;
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
import libcore.io.Streams;

/* loaded from: classes.dex */
public class EditUserPhotoController {
    private final Context mContext;
    private final Uri mCropPictureUri;
    private final Fragment mFragment;
    private final ImageView mImageView;
    private Bitmap mNewUserPhotoBitmap;
    private Drawable mNewUserPhotoDrawable;
    private final int mPhotoSize;
    private final Uri mTakePictureUri;

    public EditUserPhotoController(Fragment fragment, ImageView imageView, Bitmap bitmap, Drawable drawable, boolean z) {
        this.mContext = imageView.getContext();
        this.mFragment = fragment;
        this.mImageView = imageView;
        this.mCropPictureUri = createTempImageUri(this.mContext, "CropEditUserPhoto.jpg", !z);
        this.mTakePictureUri = createTempImageUri(this.mContext, "TakeEditUserPhoto2.jpg", !z);
        this.mPhotoSize = getPhotoSize(this.mContext);
        this.mImageView.setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.users.EditUserPhotoController.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                EditUserPhotoController.this.showUpdatePhotoPopup();
            }
        });
        this.mNewUserPhotoBitmap = bitmap;
        this.mNewUserPhotoDrawable = drawable;
    }

    public boolean onActivityResult(int i, int i2, Intent intent) {
        if (i2 != -1) {
            return false;
        }
        Uri data = (intent == null || intent.getData() == null) ? this.mTakePictureUri : intent.getData();
        if (!"content".equals(data.getScheme())) {
            Log.e("EditUserPhotoController", "Invalid pictureUri scheme: " + data.getScheme());
            EventLog.writeEvent(1397638484, "172939189", -1, data.getPath());
            return false;
        }
        switch (i) {
            case 1001:
            case 1002:
                if (this.mTakePictureUri.equals(data)) {
                    cropPhoto();
                    break;
                } else {
                    copyAndCropPhoto(data);
                    break;
                }
            case 1003:
                onPhotoCropped(data, true);
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

    private void showUpdatePhotoPopup() {
        boolean zCanTakePhoto = canTakePhoto();
        boolean zCanChoosePhoto = canChoosePhoto();
        if (!zCanTakePhoto && !zCanChoosePhoto) {
            return;
        }
        Context context = this.mImageView.getContext();
        ArrayList arrayList = new ArrayList();
        if (zCanTakePhoto) {
            arrayList.add(new RestrictedMenuItem(context, context.getString(R.string.user_image_take_photo), "no_set_user_icon", new Runnable() { // from class: com.android.settings.users.EditUserPhotoController.2
                @Override // java.lang.Runnable
                public void run() {
                    EditUserPhotoController.this.takePhoto();
                }
            }));
        }
        if (zCanChoosePhoto) {
            arrayList.add(new RestrictedMenuItem(context, context.getString(R.string.user_image_choose_photo), "no_set_user_icon", new Runnable() { // from class: com.android.settings.users.EditUserPhotoController.3
                @Override // java.lang.Runnable
                public void run() {
                    EditUserPhotoController.this.choosePhoto();
                }
            }));
        }
        final ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        listPopupWindow.setAnchorView(this.mImageView);
        listPopupWindow.setModal(true);
        listPopupWindow.setInputMethodMode(2);
        listPopupWindow.setAdapter(new RestrictedPopupMenuAdapter(context, arrayList));
        listPopupWindow.setWidth(Math.max(this.mImageView.getWidth(), context.getResources().getDimensionPixelSize(R.dimen.update_user_photo_popup_min_width)));
        listPopupWindow.setDropDownGravity(8388611);
        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.android.settings.users.EditUserPhotoController.4
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                listPopupWindow.dismiss();
                ((RestrictedMenuItem) adapterView.getAdapter().getItem(i)).doAction();
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

    private void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        appendOutputExtra(intent, this.mTakePictureUri);
        this.mFragment.startActivityForResult(intent, 1002);
    }

    private void choosePhoto() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT", (Uri) null);
        intent.setType("image/*");
        appendOutputExtra(intent, this.mTakePictureUri);
        this.mFragment.startActivityForResult(intent, 1001);
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.settings.users.EditUserPhotoController$5] */
    private void copyAndCropPhoto(final Uri uri) {
        new AsyncTask<Void, Void, Void>() { // from class: com.android.settings.users.EditUserPhotoController.5
            /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [241=4, 244=4] */
            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            /* JADX WARN: Removed duplicated region for block: B:18:0x0035 A[Catch: all -> 0x0039, Throwable -> 0x003c, TRY_ENTER, TryCatch #6 {all -> 0x0039, Throwable -> 0x003c, blocks: (B:4:0x0011, B:7:0x0021, B:18:0x0035, B:19:0x0038), top: B:39:0x0011 }] */
            /* JADX WARN: Removed duplicated region for block: B:27:0x0044 A[Catch: IOException -> 0x0048, TRY_ENTER, TryCatch #2 {IOException -> 0x0048, blocks: (B:3:0x000b, B:9:0x0026, B:27:0x0044, B:28:0x0047), top: B:32:0x000b }] */
            /* JADX WARN: Removed duplicated region for block: B:40:? A[Catch: all -> 0x0039, Throwable -> 0x003c, SYNTHETIC, TRY_LEAVE, TryCatch #6 {all -> 0x0039, Throwable -> 0x003c, blocks: (B:4:0x0011, B:7:0x0021, B:18:0x0035, B:19:0x0038), top: B:39:0x0011 }] */
            /* JADX WARN: Removed duplicated region for block: B:41:? A[Catch: IOException -> 0x0048, SYNTHETIC, TRY_LEAVE, TryCatch #2 {IOException -> 0x0048, blocks: (B:3:0x000b, B:9:0x0026, B:27:0x0044, B:28:0x0047), top: B:32:0x000b }] */
            @Override // android.os.AsyncTask
            /*
                Code decompiled incorrectly, please refer to instructions dump.
            */
            protected Void doInBackground(Void... voidArr) throws Exception {
                Throwable th;
                Throwable th2;
                Throwable th3;
                Throwable th4;
                ContentResolver contentResolver = EditUserPhotoController.this.mContext.getContentResolver();
                try {
                    InputStream inputStreamOpenInputStream = contentResolver.openInputStream(uri);
                    try {
                        OutputStream outputStreamOpenOutputStream = contentResolver.openOutputStream(EditUserPhotoController.this.mTakePictureUri);
                        try {
                            Streams.copy(inputStreamOpenInputStream, outputStreamOpenOutputStream);
                            if (outputStreamOpenOutputStream != null) {
                                $closeResource(null, outputStreamOpenOutputStream);
                            }
                            if (inputStreamOpenInputStream != null) {
                                $closeResource(null, inputStreamOpenInputStream);
                            }
                        } catch (Throwable th5) {
                            try {
                                throw th5;
                            } catch (Throwable th6) {
                                th3 = th5;
                                th4 = th6;
                                if (outputStreamOpenOutputStream != null) {
                                    throw th4;
                                }
                                $closeResource(th3, outputStreamOpenOutputStream);
                                throw th4;
                            }
                        }
                    } catch (Throwable th7) {
                        try {
                            throw th7;
                        } catch (Throwable th8) {
                            th = th7;
                            th2 = th8;
                            if (inputStreamOpenInputStream != null) {
                                throw th2;
                            }
                            $closeResource(th, inputStreamOpenInputStream);
                            throw th2;
                        }
                    }
                } catch (IOException e) {
                    Log.w("EditUserPhotoController", "Failed to copy photo", e);
                }
                return null;
            }

            private static /* synthetic */ void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
                if (th == null) {
                    autoCloseable.close();
                    return;
                }
                try {
                    autoCloseable.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }

            /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
            @Override // android.os.AsyncTask
            protected void onPostExecute(Void r1) {
                if (EditUserPhotoController.this.mFragment.isAdded()) {
                    EditUserPhotoController.this.cropPhoto();
                }
            }
        }.execute(new Void[0]);
    }

    private void cropPhoto() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(this.mTakePictureUri, "image/*");
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
        onPhotoCropped(this.mTakePictureUri, false);
    }

    private void appendOutputExtra(Intent intent, Uri uri) {
        intent.putExtra("output", uri);
        intent.addFlags(3);
        intent.setClipData(ClipData.newRawUri("output", uri));
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

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.settings.users.EditUserPhotoController$6] */
    private void onPhotoCropped(final Uri uri, final boolean z) {
        new AsyncTask<Void, Void, Bitmap>() { // from class: com.android.settings.users.EditUserPhotoController.6
            /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [307=4, 312=4] */
            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            /* JADX WARN: Type inference failed for: r9v1, types: [boolean] */
            @Override // android.os.AsyncTask
            protected Bitmap doInBackground(Void... voidArr) throws Throwable {
                Throwable th;
                InputStream inputStreamOpenInputStream;
                ?? r9 = z;
                InputStream inputStream = null;
                try {
                    if (r9 == 0) {
                        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(EditUserPhotoController.this.mPhotoSize, EditUserPhotoController.this.mPhotoSize, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmapCreateBitmap);
                        try {
                            Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(EditUserPhotoController.this.mContext.getContentResolver().openInputStream(uri));
                            if (bitmapDecodeStream == null) {
                                return null;
                            }
                            int iMin = Math.min(bitmapDecodeStream.getWidth(), bitmapDecodeStream.getHeight());
                            int width = (bitmapDecodeStream.getWidth() - iMin) / 2;
                            int height = (bitmapDecodeStream.getHeight() - iMin) / 2;
                            canvas.drawBitmap(bitmapDecodeStream, new Rect(width, height, width + iMin, iMin + height), new Rect(0, 0, EditUserPhotoController.this.mPhotoSize, EditUserPhotoController.this.mPhotoSize), new Paint());
                            return bitmapCreateBitmap;
                        } catch (FileNotFoundException e) {
                            return null;
                        }
                    }
                    try {
                        inputStreamOpenInputStream = EditUserPhotoController.this.mContext.getContentResolver().openInputStream(uri);
                    } catch (FileNotFoundException e2) {
                        e = e2;
                        inputStreamOpenInputStream = null;
                    } catch (Throwable th2) {
                        th = th2;
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e3) {
                                Log.w("EditUserPhotoController", "Cannot close image stream", e3);
                            }
                        }
                        throw th;
                    }
                    try {
                        Bitmap bitmapDecodeStream2 = BitmapFactory.decodeStream(inputStreamOpenInputStream);
                        if (inputStreamOpenInputStream != null) {
                            try {
                                inputStreamOpenInputStream.close();
                            } catch (IOException e4) {
                                Log.w("EditUserPhotoController", "Cannot close image stream", e4);
                            }
                        }
                        return bitmapDecodeStream2;
                    } catch (FileNotFoundException e5) {
                        e = e5;
                        Log.w("EditUserPhotoController", "Cannot find image file", e);
                        if (inputStreamOpenInputStream != null) {
                            try {
                                inputStreamOpenInputStream.close();
                            } catch (IOException e6) {
                                Log.w("EditUserPhotoController", "Cannot close image stream", e6);
                            }
                        }
                        return null;
                    }
                } catch (Throwable th3) {
                    inputStream = r9;
                    th = th3;
                }
            }

            /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
            @Override // android.os.AsyncTask
            protected void onPostExecute(Bitmap bitmap) {
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
        Cursor cursorQuery = context.getContentResolver().query(ContactsContract.DisplayPhoto.CONTENT_MAX_DIMENSIONS_URI, new String[]{"display_max_dim"}, null, null, null);
        try {
            cursorQuery.moveToFirst();
            return cursorQuery.getInt(0);
        } finally {
            cursorQuery.close();
        }
    }

    private Uri createTempImageUri(Context context, String str, boolean z) {
        File cacheDir = context.getCacheDir();
        cacheDir.mkdirs();
        File file = new File(cacheDir, str);
        if (z) {
            file.delete();
        }
        return FileProvider.getUriForFile(context, "com.android.settings.files", file);
    }

    File saveNewUserPhotoBitmap() throws IOException {
        if (this.mNewUserPhotoBitmap == null) {
            return null;
        }
        try {
            File file = new File(this.mContext.getCacheDir(), "NewUserPhoto.png");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            this.mNewUserPhotoBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
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

        public RestrictedMenuItem(Context context, String str, String str2, Runnable runnable) {
            this.mContext = context;
            this.mTitle = str;
            this.mAction = runnable;
            int iMyUserId = UserHandle.myUserId();
            this.mAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(context, str2, iMyUserId);
            this.mIsRestrictedByBase = RestrictedLockUtils.hasBaseUserRestriction(this.mContext, str2, iMyUserId);
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
        public RestrictedPopupMenuAdapter(Context context, List<RestrictedMenuItem> list) {
            super(context, R.layout.restricted_popup_menu_item, R.id.text, list);
        }

        @Override // android.widget.ArrayAdapter, android.widget.Adapter
        public View getView(int i, View view, ViewGroup viewGroup) {
            View view2 = super.getView(i, view, viewGroup);
            RestrictedMenuItem item = getItem(i);
            TextView textView = (TextView) view2.findViewById(R.id.text);
            ImageView imageView = (ImageView) view2.findViewById(R.id.restricted_icon);
            textView.setEnabled((item.isRestrictedByAdmin() || item.isRestrictedByBase()) ? false : true);
            imageView.setVisibility((!item.isRestrictedByAdmin() || item.isRestrictedByBase()) ? 8 : 0);
            return view2;
        }
    }
}
