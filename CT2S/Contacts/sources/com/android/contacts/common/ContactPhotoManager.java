package com.android.contacts.common;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.android.contacts.common.lettertiles.LetterTileDrawable;

public abstract class ContactPhotoManager implements ComponentCallbacks2 {
    public static DefaultImageProvider DEFAULT_AVATAR;
    public static final DefaultImageProvider DEFAULT_BLANK;
    private static final Uri DEFAULT_IMAGE_URI = Uri.parse("defaultimage://");
    private static Drawable sDefaultLetterAvatar = null;

    public static abstract class DefaultImageProvider {
        public abstract void applyDefaultImage(ImageView imageView, int i, boolean z, DefaultImageRequest defaultImageRequest);
    }

    public abstract void cancelPendingRequests(View view);

    public abstract void loadPhoto(ImageView imageView, Uri uri, int i, boolean z, boolean z2, DefaultImageRequest defaultImageRequest, DefaultImageProvider defaultImageProvider);

    public abstract void loadThumbnail(ImageView imageView, long j, boolean z, boolean z2, DefaultImageRequest defaultImageRequest, DefaultImageProvider defaultImageProvider);

    public abstract void pause();

    public abstract void preloadPhotosInBackground();

    public abstract void refreshCache();

    public abstract void resume();

    static {
        DEFAULT_AVATAR = new LetterTileDefaultImageProvider();
        DEFAULT_BLANK = new BlankDefaultImageProvider();
    }

    public static Drawable getDefaultAvatarDrawableForContact(Resources resources, boolean hires, DefaultImageRequest defaultImageRequest) {
        if (defaultImageRequest != null) {
            return LetterTileDefaultImageProvider.getDefaultImageForContact(resources, defaultImageRequest);
        }
        if (sDefaultLetterAvatar == null) {
            sDefaultLetterAvatar = LetterTileDefaultImageProvider.getDefaultImageForContact(resources, null);
        }
        return sDefaultLetterAvatar;
    }

    public static Uri removeContactType(Uri photoUri) {
        String encodedFragment = photoUri.getEncodedFragment();
        if (!TextUtils.isEmpty(encodedFragment)) {
            Uri.Builder builder = photoUri.buildUpon();
            builder.encodedFragment(null);
            return builder.build();
        }
        return photoUri;
    }

    public static boolean isBusinessContactUri(Uri photoUri) {
        if (photoUri == null) {
            return false;
        }
        String encodedFragment = photoUri.getEncodedFragment();
        return !TextUtils.isEmpty(encodedFragment) && encodedFragment.equals(String.valueOf(2));
    }

    protected static DefaultImageRequest getDefaultImageRequestFromUri(Uri uri) {
        DefaultImageRequest request = new DefaultImageRequest(uri.getQueryParameter("display_name"), uri.getQueryParameter("identifier"), false);
        try {
            String contactType = uri.getQueryParameter("contact_type");
            if (!TextUtils.isEmpty(contactType)) {
                request.contactType = Integer.valueOf(contactType).intValue();
            }
            String scale = uri.getQueryParameter("scale");
            if (!TextUtils.isEmpty(scale)) {
                request.scale = Float.valueOf(scale).floatValue();
            }
            String offset = uri.getQueryParameter("offset");
            if (!TextUtils.isEmpty(offset)) {
                request.offset = Float.valueOf(offset).floatValue();
            }
            String isCircular = uri.getQueryParameter("is_circular");
            if (!TextUtils.isEmpty(isCircular)) {
                request.isCircular = Boolean.valueOf(isCircular).booleanValue();
            }
        } catch (NumberFormatException e) {
            Log.w("ContactPhotoManager", "Invalid DefaultImageRequest image parameters provided, ignoring and using defaults.");
        }
        return request;
    }

    protected boolean isDefaultImageUri(Uri uri) {
        return "defaultimage".equals(uri.getScheme());
    }

    public static class DefaultImageRequest {
        public int contactType;
        public String displayName;
        public String identifier;
        public boolean isCircular;
        public float offset;
        public float scale;
        public static DefaultImageRequest EMPTY_DEFAULT_IMAGE_REQUEST = new DefaultImageRequest();
        public static DefaultImageRequest EMPTY_DEFAULT_BUSINESS_IMAGE_REQUEST = new DefaultImageRequest(null, null, 2, false);
        public static DefaultImageRequest EMPTY_CIRCULAR_DEFAULT_IMAGE_REQUEST = new DefaultImageRequest(null, null, true);
        public static DefaultImageRequest EMPTY_CIRCULAR_BUSINESS_IMAGE_REQUEST = new DefaultImageRequest(null, null, 2, true);

        public DefaultImageRequest() {
            this.contactType = 1;
            this.scale = 1.0f;
            this.offset = 0.0f;
            this.isCircular = false;
        }

        public DefaultImageRequest(String displayName, String identifier, boolean isCircular) {
            this(displayName, identifier, 1, 1.0f, 0.0f, isCircular);
        }

        public DefaultImageRequest(String displayName, String identifier, int contactType, boolean isCircular) {
            this(displayName, identifier, contactType, 1.0f, 0.0f, isCircular);
        }

        public DefaultImageRequest(String displayName, String identifier, int contactType, float scale, float offset, boolean isCircular) {
            this.contactType = 1;
            this.scale = 1.0f;
            this.offset = 0.0f;
            this.isCircular = false;
            this.displayName = displayName;
            this.identifier = identifier;
            this.contactType = contactType;
            this.scale = scale;
            this.offset = offset;
            this.isCircular = isCircular;
        }
    }

    private static class LetterTileDefaultImageProvider extends DefaultImageProvider {
        private LetterTileDefaultImageProvider() {
        }

        @Override
        public void applyDefaultImage(ImageView view, int extent, boolean darkTheme, DefaultImageRequest defaultImageRequest) {
            Drawable drawable = getDefaultImageForContact(view.getResources(), defaultImageRequest);
            view.setImageDrawable(drawable);
        }

        public static Drawable getDefaultImageForContact(Resources resources, DefaultImageRequest defaultImageRequest) {
            LetterTileDrawable drawable = new LetterTileDrawable(resources);
            if (defaultImageRequest != null) {
                if (TextUtils.isEmpty(defaultImageRequest.identifier)) {
                    drawable.setContactDetails(null, defaultImageRequest.displayName);
                } else {
                    drawable.setContactDetails(defaultImageRequest.displayName, defaultImageRequest.identifier);
                }
                drawable.setContactType(defaultImageRequest.contactType);
                drawable.setScale(defaultImageRequest.scale);
                drawable.setOffset(defaultImageRequest.offset);
                drawable.setIsCircular(defaultImageRequest.isCircular);
            }
            return drawable;
        }
    }

    private static class BlankDefaultImageProvider extends DefaultImageProvider {
        private static Drawable sDrawable;

        private BlankDefaultImageProvider() {
        }

        @Override
        public void applyDefaultImage(ImageView view, int extent, boolean darkTheme, DefaultImageRequest defaultImageRequest) {
            if (sDrawable == null) {
                Context context = view.getContext();
                sDrawable = new ColorDrawable(context.getResources().getColor(com.android.contacts.R.color.image_placeholder));
            }
            view.setImageDrawable(sDrawable);
        }
    }

    public static ContactPhotoManager getInstance(Context context) {
        Context applicationContext = context.getApplicationContext();
        ContactPhotoManager service = (ContactPhotoManager) applicationContext.getSystemService("contactPhotos");
        if (service == null) {
            ContactPhotoManager service2 = createContactPhotoManager(applicationContext);
            Log.e("ContactPhotoManager", "No contact photo service in context: " + applicationContext);
            return service2;
        }
        return service;
    }

    public static synchronized ContactPhotoManager createContactPhotoManager(Context context) {
        return new ContactPhotoManagerImpl(context);
    }

    public final void loadThumbnail(ImageView view, long photoId, boolean darkTheme, boolean isCircular, DefaultImageRequest defaultImageRequest) {
        loadThumbnail(view, photoId, darkTheme, isCircular, defaultImageRequest, DEFAULT_AVATAR);
    }

    public final void loadPhoto(ImageView view, Uri photoUri, int requestedExtent, boolean darkTheme, boolean isCircular, DefaultImageRequest defaultImageRequest) {
        loadPhoto(view, photoUri, requestedExtent, darkTheme, isCircular, defaultImageRequest, DEFAULT_AVATAR);
    }

    public final void loadDirectoryPhoto(ImageView view, Uri photoUri, boolean darkTheme, boolean isCircular, DefaultImageRequest defaultImageRequest) {
        loadPhoto(view, photoUri, -1, darkTheme, isCircular, defaultImageRequest, DEFAULT_AVATAR);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(int level) {
    }
}
