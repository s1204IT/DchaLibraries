package com.android.settings.users;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.drawable.CircleFramedDrawable;
import java.io.File;

public class EditUserInfoController {
    private Dialog mEditUserInfoDialog;
    private EditUserPhotoController mEditUserPhotoController;
    private Bitmap mSavedPhoto;
    private UserHandle mUser;
    private UserManager mUserManager;
    private boolean mWaitingForActivityResult = false;

    public interface OnContentChangedCallback {
        void onLabelChanged(CharSequence charSequence);

        void onPhotoChanged(Drawable drawable);
    }

    public void clear() {
        this.mEditUserPhotoController.removeNewUserPhotoBitmapFile();
        this.mEditUserInfoDialog = null;
        this.mSavedPhoto = null;
    }

    public void onRestoreInstanceState(Bundle bundle) {
        String string = bundle.getString("pending_photo");
        if (string != null) {
            this.mSavedPhoto = EditUserPhotoController.loadNewUserPhotoBitmap(new File(string));
        }
        this.mWaitingForActivityResult = bundle.getBoolean("awaiting_result", false);
    }

    public void onSaveInstanceState(Bundle bundle) {
        File fileSaveNewUserPhotoBitmap;
        if (this.mEditUserInfoDialog != null && this.mEditUserInfoDialog.isShowing() && this.mEditUserPhotoController != null && (fileSaveNewUserPhotoBitmap = this.mEditUserPhotoController.saveNewUserPhotoBitmap()) != null) {
            bundle.putString("pending_photo", fileSaveNewUserPhotoBitmap.getPath());
        }
        if (this.mWaitingForActivityResult) {
            bundle.putBoolean("awaiting_result", this.mWaitingForActivityResult);
        }
    }

    public void startingActivityForResult() {
        this.mWaitingForActivityResult = true;
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        this.mWaitingForActivityResult = false;
        if (this.mEditUserInfoDialog == null || !this.mEditUserInfoDialog.isShowing() || this.mEditUserPhotoController.onActivityResult(i, i2, intent)) {
        }
    }

    public Dialog createDialog(final Fragment fragment, final Drawable drawable, final CharSequence charSequence, int i, final OnContentChangedCallback onContentChangedCallback, UserHandle userHandle) {
        Drawable drawable2;
        Drawable userIcon;
        Activity activity = fragment.getActivity();
        this.mUser = userHandle;
        if (this.mUserManager == null) {
            this.mUserManager = UserManager.get(activity);
        }
        View viewInflate = activity.getLayoutInflater().inflate(R.layout.edit_user_info_dialog_content, (ViewGroup) null);
        UserInfo userInfo = this.mUserManager.getUserInfo(this.mUser.getIdentifier());
        final EditText editText = (EditText) viewInflate.findViewById(R.id.user_name);
        editText.setText(userInfo.name);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.user_photo);
        if (this.mSavedPhoto != null) {
            userIcon = CircleFramedDrawable.getInstance(activity, this.mSavedPhoto);
        } else if (drawable == null) {
            userIcon = Utils.getUserIcon(activity, this.mUserManager, userInfo);
        } else {
            drawable2 = drawable;
            imageView.setImageDrawable(drawable2);
            this.mEditUserPhotoController = new EditUserPhotoController(fragment, imageView, this.mSavedPhoto, drawable2, this.mWaitingForActivityResult);
            this.mEditUserInfoDialog = new AlertDialog.Builder(activity).setTitle(R.string.profile_info_settings_title).setView(viewInflate).setCancelable(true).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    if (i2 == -1) {
                        Editable text = editText.getText();
                        if (!TextUtils.isEmpty(text) && (charSequence == null || !text.toString().equals(charSequence.toString()))) {
                            if (onContentChangedCallback != null) {
                                onContentChangedCallback.onLabelChanged(text.toString());
                            }
                            EditUserInfoController.this.mUserManager.setUserName(EditUserInfoController.this.mUser.getIdentifier(), text.toString());
                        }
                        Drawable newUserPhotoDrawable = EditUserInfoController.this.mEditUserPhotoController.getNewUserPhotoDrawable();
                        Bitmap newUserPhotoBitmap = EditUserInfoController.this.mEditUserPhotoController.getNewUserPhotoBitmap();
                        if (newUserPhotoDrawable != null && newUserPhotoBitmap != null && !newUserPhotoDrawable.equals(drawable)) {
                            if (onContentChangedCallback != null) {
                                onContentChangedCallback.onPhotoChanged(newUserPhotoDrawable);
                            }
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                public Void doInBackground(Void... voidArr) {
                                    EditUserInfoController.this.mUserManager.setUserIcon(EditUserInfoController.this.mUser.getIdentifier(), EditUserInfoController.this.mEditUserPhotoController.getNewUserPhotoBitmap());
                                    return null;
                                }
                            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                        }
                        fragment.getActivity().removeDialog(1);
                    }
                    EditUserInfoController.this.clear();
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i2) {
                    EditUserInfoController.this.clear();
                }
            }).create();
            this.mEditUserInfoDialog.getWindow().setSoftInputMode(4);
            return this.mEditUserInfoDialog;
        }
        drawable2 = userIcon;
        imageView.setImageDrawable(drawable2);
        this.mEditUserPhotoController = new EditUserPhotoController(fragment, imageView, this.mSavedPhoto, drawable2, this.mWaitingForActivityResult);
        this.mEditUserInfoDialog = new AlertDialog.Builder(activity).setTitle(R.string.profile_info_settings_title).setView(viewInflate).setCancelable(true).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                if (i2 == -1) {
                    Editable text = editText.getText();
                    if (!TextUtils.isEmpty(text) && (charSequence == null || !text.toString().equals(charSequence.toString()))) {
                        if (onContentChangedCallback != null) {
                            onContentChangedCallback.onLabelChanged(text.toString());
                        }
                        EditUserInfoController.this.mUserManager.setUserName(EditUserInfoController.this.mUser.getIdentifier(), text.toString());
                    }
                    Drawable newUserPhotoDrawable = EditUserInfoController.this.mEditUserPhotoController.getNewUserPhotoDrawable();
                    Bitmap newUserPhotoBitmap = EditUserInfoController.this.mEditUserPhotoController.getNewUserPhotoBitmap();
                    if (newUserPhotoDrawable != null && newUserPhotoBitmap != null && !newUserPhotoDrawable.equals(drawable)) {
                        if (onContentChangedCallback != null) {
                            onContentChangedCallback.onPhotoChanged(newUserPhotoDrawable);
                        }
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            public Void doInBackground(Void... voidArr) {
                                EditUserInfoController.this.mUserManager.setUserIcon(EditUserInfoController.this.mUser.getIdentifier(), EditUserInfoController.this.mEditUserPhotoController.getNewUserPhotoBitmap());
                                return null;
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
                    }
                    fragment.getActivity().removeDialog(1);
                }
                EditUserInfoController.this.clear();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i2) {
                EditUserInfoController.this.clear();
            }
        }).create();
        this.mEditUserInfoDialog.getWindow().setSoftInputMode(4);
        return this.mEditUserInfoDialog;
    }
}
