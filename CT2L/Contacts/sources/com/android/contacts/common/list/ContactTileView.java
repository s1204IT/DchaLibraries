package com.android.contacts.common.list;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.MoreContactUtils;

public abstract class ContactTileView extends FrameLayout {
    private static final String TAG = ContactTileView.class.getSimpleName();
    private View mHorizontalDivider;
    protected Listener mListener;
    private Uri mLookupUri;
    private TextView mName;
    private TextView mPhoneLabel;
    private TextView mPhoneNumber;
    private ImageView mPhoto;
    private ContactPhotoManager mPhotoManager;
    private View mPushState;
    private QuickContactBadge mQuickContact;
    private TextView mStatus;

    public interface Listener {
        int getApproximateTileWidth();

        void onCallNumberDirectly(String str);

        void onContactSelected(Uri uri, Rect rect);
    }

    protected abstract int getApproximateImageSize();

    protected abstract boolean isDarkTheme();

    public ContactTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPhotoManager = null;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mName = (TextView) findViewById(R.id.contact_tile_name);
        this.mQuickContact = (QuickContactBadge) findViewById(R.id.contact_tile_quick);
        this.mPhoto = (ImageView) findViewById(R.id.contact_tile_image);
        this.mStatus = (TextView) findViewById(R.id.contact_tile_status);
        this.mPhoneLabel = (TextView) findViewById(R.id.contact_tile_phone_type);
        this.mPhoneNumber = (TextView) findViewById(R.id.contact_tile_phone_number);
        this.mPushState = findViewById(R.id.contact_tile_push_state);
        this.mHorizontalDivider = findViewById(R.id.contact_tile_horizontal_divider);
        View.OnClickListener listener = createClickListener();
        setOnClickListener(listener);
    }

    protected View.OnClickListener createClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContactTileView.this.mListener != null) {
                    ContactTileView.this.mListener.onContactSelected(ContactTileView.this.getLookupUri(), MoreContactUtils.getTargetRectFromView(ContactTileView.this));
                }
            }
        };
    }

    public void setPhotoManager(ContactPhotoManager photoManager) {
        this.mPhotoManager = photoManager;
    }

    public void loadFromContact(ContactEntry entry) {
        if (entry != null) {
            this.mName.setText(getNameForView(entry.name));
            this.mLookupUri = entry.lookupUri;
            if (this.mStatus != null) {
                if (entry.status == null) {
                    this.mStatus.setVisibility(8);
                } else {
                    this.mStatus.setText(entry.status);
                    this.mStatus.setCompoundDrawablesWithIntrinsicBounds(entry.presenceIcon, (Drawable) null, (Drawable) null, (Drawable) null);
                    this.mStatus.setVisibility(0);
                }
            }
            if (this.mPhoneLabel != null) {
                if (TextUtils.isEmpty(entry.phoneLabel)) {
                    this.mPhoneLabel.setVisibility(8);
                } else {
                    this.mPhoneLabel.setVisibility(0);
                    this.mPhoneLabel.setText(entry.phoneLabel);
                }
            }
            if (this.mPhoneNumber != null) {
                this.mPhoneNumber.setText(entry.phoneNumber);
            }
            setVisibility(0);
            if (this.mPhotoManager != null) {
                ContactPhotoManager.DefaultImageRequest request = getDefaultImageRequest(entry.name, entry.lookupKey);
                configureViewForImage(entry.photoUri == null);
                if (this.mPhoto != null) {
                    this.mPhotoManager.loadPhoto(this.mPhoto, entry.photoUri, getApproximateImageSize(), isDarkTheme(), isContactPhotoCircular(), request);
                    if (this.mQuickContact != null) {
                        this.mQuickContact.assignContactUri(this.mLookupUri);
                    }
                } else if (this.mQuickContact != null) {
                    this.mQuickContact.assignContactUri(this.mLookupUri);
                    this.mPhotoManager.loadPhoto(this.mQuickContact, entry.photoUri, getApproximateImageSize(), isDarkTheme(), isContactPhotoCircular(), request);
                }
            } else {
                Log.w(TAG, "contactPhotoManager not set");
            }
            if (this.mPushState != null) {
                this.mPushState.setContentDescription(entry.name);
                return;
            } else {
                if (this.mQuickContact != null) {
                    this.mQuickContact.setContentDescription(entry.name);
                    return;
                }
                return;
            }
        }
        setVisibility(4);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setHorizontalDividerVisibility(int visibility) {
        if (this.mHorizontalDivider != null) {
            this.mHorizontalDivider.setVisibility(visibility);
        }
    }

    public Uri getLookupUri() {
        return this.mLookupUri;
    }

    protected QuickContactBadge getQuickContact() {
        return this.mQuickContact;
    }

    protected View getPhotoView() {
        return this.mPhoto;
    }

    protected String getNameForView(String name) {
        return name;
    }

    protected void configureViewForImage(boolean isDefaultImage) {
    }

    protected ContactPhotoManager.DefaultImageRequest getDefaultImageRequest(String displayName, String lookupKey) {
        return new ContactPhotoManager.DefaultImageRequest(displayName, lookupKey, isContactPhotoCircular());
    }

    protected boolean isContactPhotoCircular() {
        return true;
    }
}
