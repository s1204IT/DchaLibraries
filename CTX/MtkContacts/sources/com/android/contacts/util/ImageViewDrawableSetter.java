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
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.model.Contact;
import java.util.Arrays;

public class ImageViewDrawableSetter {
    private byte[] mCompressed;
    private Contact mContact;
    private int mDurationInMillis = 0;
    private Drawable mPreviousDrawable;
    private ImageView mTarget;

    public Bitmap setupContactPhoto(Contact contact, ImageView imageView) {
        this.mContact = contact;
        setTarget(imageView);
        return setCompressedImage(contact.getPhotoBinaryData());
    }

    protected void setTarget(ImageView imageView) {
        if (this.mTarget != imageView) {
            this.mTarget = imageView;
            this.mCompressed = null;
            this.mPreviousDrawable = null;
        }
    }

    protected Bitmap setCompressedImage(byte[] bArr) {
        if (this.mPreviousDrawable != null && this.mPreviousDrawable != null && (this.mPreviousDrawable instanceof BitmapDrawable) && Arrays.equals(this.mCompressed, bArr)) {
            return previousBitmap();
        }
        Drawable drawableDecodedBitmapDrawable = decodedBitmapDrawable(bArr);
        if (drawableDecodedBitmapDrawable == null) {
            drawableDecodedBitmapDrawable = defaultDrawable();
        }
        this.mCompressed = bArr;
        if (drawableDecodedBitmapDrawable == null) {
            return previousBitmap();
        }
        if (this.mPreviousDrawable == null || this.mDurationInMillis == 0) {
            this.mTarget.setImageDrawable(drawableDecodedBitmapDrawable);
        } else {
            TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{this.mPreviousDrawable, drawableDecodedBitmapDrawable});
            this.mTarget.setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(this.mDurationInMillis);
        }
        this.mPreviousDrawable = drawableDecodedBitmapDrawable;
        return previousBitmap();
    }

    private Bitmap previousBitmap() {
        if (this.mPreviousDrawable == null || (this.mPreviousDrawable instanceof LetterTileDrawable)) {
            return null;
        }
        return ((BitmapDrawable) this.mPreviousDrawable).getBitmap();
    }

    private Drawable defaultDrawable() {
        int i;
        ContactPhotoManager.DefaultImageRequest defaultImageRequest;
        Resources resources = this.mTarget.getResources();
        if (this.mContact.isDisplayNameFromOrganization()) {
            i = 2;
        } else {
            i = 1;
        }
        if (TextUtils.isEmpty(this.mContact.getLookupKey())) {
            defaultImageRequest = new ContactPhotoManager.DefaultImageRequest(null, this.mContact.getDisplayName(), i, false);
        } else {
            defaultImageRequest = new ContactPhotoManager.DefaultImageRequest(this.mContact.getDisplayName(), this.mContact.getLookupKey(), i, false);
        }
        return ContactPhotoManager.getDefaultAvatarDrawableForContact(resources, true, defaultImageRequest);
    }

    private BitmapDrawable decodedBitmapDrawable(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        Resources resources = this.mTarget.getResources();
        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(bArr, 0, bArr.length);
        if (bitmapDecodeByteArray == null) {
            return null;
        }
        if (bitmapDecodeByteArray.getHeight() != bitmapDecodeByteArray.getWidth()) {
            int iMin = Math.min(bitmapDecodeByteArray.getWidth(), bitmapDecodeByteArray.getHeight());
            bitmapDecodeByteArray = ThumbnailUtils.extractThumbnail(bitmapDecodeByteArray, iMin, iMin);
        }
        return new BitmapDrawable(resources, bitmapDecodeByteArray);
    }
}
