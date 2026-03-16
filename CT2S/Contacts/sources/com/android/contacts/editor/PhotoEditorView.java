package com.android.contacts.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.editor.Editor;
import com.android.contacts.util.ContactPhotoUtils;

public class PhotoEditorView extends LinearLayout implements Editor {
    private Button mChangeButton;
    private ContactPhotoManager mContactPhotoManager;
    private ValuesDelta mEntry;
    private boolean mHasSetPhoto;
    private Editor.EditorListener mListener;
    private ImageView mPhotoImageView;
    private RadioButton mPrimaryCheckBox;
    private boolean mReadOnly;

    public PhotoEditorView(Context context) {
        super(context);
        this.mHasSetPhoto = false;
    }

    public PhotoEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHasSetPhoto = false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mContactPhotoManager = ContactPhotoManager.getInstance(this.mContext);
        this.mPhotoImageView = (ImageView) findViewById(R.id.photo);
        this.mPrimaryCheckBox = (RadioButton) findViewById(R.id.primary_checkbox);
        this.mChangeButton = (Button) findViewById(R.id.change_button);
        this.mPrimaryCheckBox = (RadioButton) findViewById(R.id.primary_checkbox);
        if (this.mChangeButton != null) {
            this.mChangeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PhotoEditorView.this.mListener != null) {
                        PhotoEditorView.this.mListener.onRequest(1);
                    }
                }
            });
        }
        this.mPrimaryCheckBox.setSaveEnabled(false);
        this.mPrimaryCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PhotoEditorView.this.mListener != null) {
                    PhotoEditorView.this.mListener.onRequest(0);
                }
            }
        });
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta values, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        Integer photoFileId;
        this.mEntry = values;
        this.mReadOnly = readOnly;
        setId(vig.getId(state, kind, values, 0));
        this.mPrimaryCheckBox.setChecked(values != null && values.isSuperPrimary());
        if (values != null) {
            byte[] photoBytes = values.getAsByteArray("data15");
            if (photoBytes != null) {
                Bitmap photo = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                this.mPhotoImageView.setImageBitmap(photo);
                this.mHasSetPhoto = true;
                this.mEntry.setFromTemplate(false);
                if ((values.getAfter() == null || values.getAfter().get("data15") == null) && (photoFileId = values.getAsInteger("data14")) != null) {
                    Uri photoUri = ContactsContract.DisplayPhoto.CONTENT_URI.buildUpon().appendPath(photoFileId.toString()).build();
                    setFullSizedPhoto(photoUri);
                    return;
                }
                return;
            }
            resetDefault();
            return;
        }
        resetDefault();
    }

    public void setShowPrimary(boolean showPrimaryCheckBox) {
        this.mPrimaryCheckBox.setVisibility(showPrimaryCheckBox ? 0 : 8);
    }

    public boolean hasSetPhoto() {
        return this.mHasSetPhoto;
    }

    public void setPhotoEntry(Bitmap photo) {
        if (photo == null) {
            this.mEntry.put("data15", (byte[]) null);
            resetDefault();
            return;
        }
        int size = ContactsUtils.getThumbnailSize(getContext());
        Bitmap scaled = Bitmap.createScaledBitmap(photo, size, size, false);
        this.mPhotoImageView.setImageBitmap(scaled);
        this.mHasSetPhoto = true;
        this.mEntry.setFromTemplate(false);
        this.mEntry.setSuperPrimary(true);
        byte[] compressed = ContactPhotoUtils.compressBitmap(scaled);
        if (compressed != null) {
            this.mEntry.setPhoto(compressed);
        }
    }

    public void setFullSizedPhoto(Uri photoUri) {
        if (photoUri != null) {
            ContactPhotoManager.DefaultImageProvider fallbackToPreviousImage = new ContactPhotoManager.DefaultImageProvider() {
                @Override
                public void applyDefaultImage(ImageView view, int extent, boolean darkTheme, ContactPhotoManager.DefaultImageRequest defaultImageRequest) {
                }
            };
            this.mContactPhotoManager.loadPhoto(this.mPhotoImageView, photoUri, this.mPhotoImageView.getWidth(), false, false, null, fallbackToPreviousImage);
        }
    }

    public void setSuperPrimary(boolean superPrimary) {
        this.mEntry.put("is_super_primary", superPrimary ? 1 : 0);
    }

    protected void resetDefault() {
        this.mPhotoImageView.setImageDrawable(ContactPhotoManager.getDefaultAvatarDrawableForContact(getResources(), false, null));
        this.mHasSetPhoto = false;
        this.mEntry.setFromTemplate(true);
    }

    @Override
    public void setEditorListener(Editor.EditorListener listener) {
        this.mListener = listener;
    }

    @Override
    public void setDeletable(boolean deletable) {
    }

    @Override
    public boolean isEmpty() {
        return !this.mHasSetPhoto;
    }

    @Override
    public void deleteEditor() {
    }

    @Override
    public void clearAllFields() {
        resetDefault();
    }

    public View getChangeAnchorView() {
        return this.mChangeButton;
    }
}
