package com.android.contacts.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.SchedulingUtils;
import com.android.contacts.widget.QuickContactImageView;

public class PhotoEditorView extends RelativeLayout implements View.OnClickListener {
    private boolean mIsNonDefaultPhotoBound;
    private final boolean mIsTwoPanel;
    private final float mLandscapePhotoRatio;
    private Listener mListener;
    private MaterialColorMapUtils.MaterialPalette mMaterialPalette;
    private View mPhotoIcon;
    private View mPhotoIconOverlay;
    private QuickContactImageView mPhotoImageView;
    private View mPhotoTouchInterceptOverlay;
    private final float mPortraitPhotoRatio;
    private boolean mReadOnly;

    public interface Listener {
        void onPhotoEditorViewClicked();
    }

    public PhotoEditorView(Context context) {
        this(context, null);
    }

    public PhotoEditorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLandscapePhotoRatio = getTypedFloat(R.dimen.quickcontact_landscape_photo_ratio);
        this.mPortraitPhotoRatio = getTypedFloat(R.dimen.editor_portrait_photo_ratio);
        this.mIsTwoPanel = getResources().getBoolean(R.bool.contacteditor_two_panel);
    }

    private float getTypedFloat(int i) {
        TypedValue typedValue = new TypedValue();
        getResources().getValue(i, typedValue, true);
        return typedValue.getFloat();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPhotoImageView = (QuickContactImageView) findViewById(R.id.photo);
        this.mPhotoIcon = findViewById(R.id.photo_icon);
        this.mPhotoIconOverlay = findViewById(R.id.photo_icon_overlay);
        this.mPhotoTouchInterceptOverlay = findViewById(R.id.photo_touch_intercept_overlay);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setReadOnly(boolean z) {
        this.mReadOnly = z;
        if (this.mReadOnly) {
            this.mPhotoIcon.setVisibility(8);
            this.mPhotoIconOverlay.setVisibility(8);
            this.mPhotoTouchInterceptOverlay.setClickable(false);
            this.mPhotoTouchInterceptOverlay.setContentDescription(getContext().getString(R.string.editor_contact_photo_content_description));
            return;
        }
        this.mPhotoIcon.setVisibility(0);
        this.mPhotoIconOverlay.setVisibility(0);
        this.mPhotoTouchInterceptOverlay.setOnClickListener(this);
        updatePhotoDescription();
    }

    public void setPalette(MaterialColorMapUtils.MaterialPalette materialPalette) {
        this.mMaterialPalette = materialPalette;
    }

    public void setPhoto(ValuesDelta valuesDelta) {
        Long photoFileId = EditorUiUtils.getPhotoFileId(valuesDelta);
        if (photoFileId != null) {
            setFullSizedPhoto(ContactsContract.DisplayPhoto.CONTENT_URI.buildUpon().appendPath(photoFileId.toString()).build());
            adjustDimensions();
            return;
        }
        Bitmap photoBitmap = EditorUiUtils.getPhotoBitmap(valuesDelta);
        if (photoBitmap != null) {
            setPhoto(photoBitmap);
            adjustDimensions();
        } else {
            setDefaultPhoto(this.mMaterialPalette);
            adjustDimensions();
        }
    }

    private void adjustDimensions() {
        SchedulingUtils.doOnPreDraw(this, false, new Runnable() {
            @Override
            public void run() {
                int width;
                int height;
                if (PhotoEditorView.this.mIsTwoPanel) {
                    height = PhotoEditorView.this.getHeight();
                    width = (int) (height * PhotoEditorView.this.mLandscapePhotoRatio);
                } else {
                    width = PhotoEditorView.this.getWidth();
                    height = (int) (width / PhotoEditorView.this.mPortraitPhotoRatio);
                }
                ViewGroup.LayoutParams layoutParams = PhotoEditorView.this.getLayoutParams();
                layoutParams.height = height;
                layoutParams.width = width;
                PhotoEditorView.this.setLayoutParams(layoutParams);
            }
        });
    }

    public boolean isWritablePhotoSet() {
        return !this.mReadOnly && this.mIsNonDefaultPhotoBound;
    }

    private void setPhoto(Bitmap bitmap) {
        this.mPhotoImageView.setImageBitmap(bitmap);
        this.mIsNonDefaultPhotoBound = true;
        updatePhotoDescription();
    }

    private void setDefaultPhoto(MaterialColorMapUtils.MaterialPalette materialPalette) {
        this.mIsNonDefaultPhotoBound = false;
        updatePhotoDescription();
        EditorUiUtils.setDefaultPhoto(this.mPhotoImageView, getResources(), materialPalette);
    }

    private void updatePhotoDescription() {
        int i;
        View view = this.mPhotoTouchInterceptOverlay;
        Context context = getContext();
        if (this.mIsNonDefaultPhotoBound) {
            i = R.string.editor_change_photo_content_description;
        } else {
            i = R.string.editor_add_photo_content_description;
        }
        view.setContentDescription(context.getString(i));
    }

    public void setFullSizedPhoto(Uri uri) {
        EditorUiUtils.loadPhoto(ContactPhotoManager.getInstance(getContext()), this.mPhotoImageView, uri);
        this.mIsNonDefaultPhotoBound = true;
        updatePhotoDescription();
    }

    public void removePhoto() {
        setDefaultPhoto(this.mMaterialPalette);
    }

    @Override
    public void onClick(View view) {
        if (this.mListener != null) {
            this.mListener.onPhotoEditorViewClicked();
        }
    }

    public void setSelectPhotoEnable(boolean z) {
        if (z) {
            this.mPhotoIcon.setVisibility(0);
            this.mPhotoIconOverlay.setVisibility(0);
            this.mPhotoTouchInterceptOverlay.setVisibility(0);
        } else {
            this.mPhotoIcon.setVisibility(8);
            this.mPhotoIconOverlay.setVisibility(8);
            this.mPhotoTouchInterceptOverlay.setVisibility(8);
        }
    }
}
