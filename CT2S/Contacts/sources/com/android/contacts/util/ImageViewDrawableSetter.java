package com.android.contacts.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.ThumbnailUtils;
import android.text.TextUtils;
import android.widget.ImageView;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.lettertiles.LetterTileDrawable;
import com.android.contacts.common.model.Contact;
import java.util.Arrays;

public class ImageViewDrawableSetter {
    private byte[] mCompressed;
    private Contact mContact;
    private int mDurationInMillis = 0;
    private Drawable mPreviousDrawable;
    private ImageView mTarget;

    public Bitmap setupContactPhoto(Contact contactData, ImageView photoView) {
        this.mContact = contactData;
        setTarget(photoView);
        return setCompressedImage(contactData.getPhotoBinaryData());
    }

    protected void setTarget(ImageView target) {
        if (this.mTarget != target) {
            this.mTarget = target;
            this.mCompressed = null;
            this.mPreviousDrawable = null;
        }
    }

    protected Bitmap setCompressedImage(byte[] compressed) {
        if (this.mPreviousDrawable != null && this.mPreviousDrawable != null && (this.mPreviousDrawable instanceof BitmapDrawable) && Arrays.equals(this.mCompressed, compressed)) {
            return previousBitmap();
        }
        Drawable newDrawable = decodedBitmapDrawable(compressed);
        if (newDrawable == null) {
            newDrawable = defaultDrawable();
        }
        this.mCompressed = compressed;
        if (newDrawable == null) {
            return previousBitmap();
        }
        if (this.mPreviousDrawable == null || this.mDurationInMillis == 0) {
            this.mTarget.setImageDrawable(newDrawable);
        } else {
            Drawable[] beforeAndAfter = {this.mPreviousDrawable, newDrawable};
            TransitionDrawable transition = new TransitionDrawable(beforeAndAfter);
            this.mTarget.setImageDrawable(transition);
            transition.startTransition(this.mDurationInMillis);
        }
        this.mPreviousDrawable = newDrawable;
        return previousBitmap();
    }

    private Bitmap previousBitmap() {
        if (this.mPreviousDrawable == null || (this.mPreviousDrawable instanceof LetterTileDrawable)) {
            return null;
        }
        return ((BitmapDrawable) this.mPreviousDrawable).getBitmap();
    }

    private Drawable defaultDrawable() {
        ContactPhotoManager.DefaultImageRequest request;
        Resources resources = this.mTarget.getResources();
        int contactType = 1;
        if (this.mContact.isDisplayNameFromOrganization()) {
            contactType = 2;
        }
        if (TextUtils.isEmpty(this.mContact.getLookupKey())) {
            request = new ContactPhotoManager.DefaultImageRequest(null, this.mContact.getDisplayName(), contactType, false);
        } else {
            request = new ContactPhotoManager.DefaultImageRequest(this.mContact.getDisplayName(), this.mContact.getLookupKey(), contactType, false);
        }
        return ContactPhotoManager.getDefaultAvatarDrawableForContact(resources, true, request);
    }

    private BitmapDrawable decodedBitmapDrawable(byte[] compressed) {
        if (compressed == null) {
            return null;
        }
        Resources rsrc = this.mTarget.getResources();
        Bitmap bitmap = BitmapFactory.decodeByteArray(compressed, 0, compressed.length);
        if (bitmap == null) {
            return null;
        }
        if (bitmap.getHeight() != bitmap.getWidth()) {
            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, size, size);
        }
        return new BitmapDrawable(rsrc, bitmap);
    }
}
