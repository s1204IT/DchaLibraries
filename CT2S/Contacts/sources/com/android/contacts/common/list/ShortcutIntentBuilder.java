package com.android.contacts.common.list;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextPaint;
import android.text.TextUtils;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;

public class ShortcutIntentBuilder {
    private static final String[] CONTACT_COLUMNS = {"display_name", "photo_id", "lookup"};
    private static final String[] PHONE_COLUMNS = {"display_name", "photo_id", "data1", "data2", "data3", "lookup"};
    private static final String[] PHOTO_COLUMNS = {"data15"};
    private final Context mContext;
    private final int mIconDensity;
    private int mIconSize;
    private final OnShortcutIntentCreatedListener mListener;
    private final int mOverlayTextBackgroundColor;
    private final Resources mResources;

    public interface OnShortcutIntentCreatedListener {
        void onShortcutIntentCreated(Uri uri, Intent intent);
    }

    public ShortcutIntentBuilder(Context context, OnShortcutIntentCreatedListener listener) {
        this.mContext = context;
        this.mListener = listener;
        this.mResources = context.getResources();
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        this.mIconSize = this.mResources.getDimensionPixelSize(R.dimen.shortcut_icon_size);
        if (this.mIconSize == 0) {
            this.mIconSize = am.getLauncherLargeIconSize();
        }
        this.mIconDensity = am.getLauncherLargeIconDensity();
        this.mOverlayTextBackgroundColor = this.mResources.getColor(R.color.shortcut_overlay_text_background);
    }

    public void createContactShortcutIntent(Uri contactUri) {
        new ContactLoadingAsyncTask(contactUri).execute(new Void[0]);
    }

    public void createPhoneNumberShortcutIntent(Uri dataUri, String shortcutAction) {
        new PhoneNumberLoadingAsyncTask(dataUri, shortcutAction).execute(new Void[0]);
    }

    private abstract class LoadingAsyncTask extends AsyncTask<Void, Void, Void> {
        protected byte[] mBitmapData;
        protected String mContentType;
        protected String mDisplayName;
        protected String mLookupKey;
        protected long mPhotoId;
        protected Uri mUri;

        protected abstract void loadData();

        public LoadingAsyncTask(Uri uri) {
            this.mUri = uri;
        }

        @Override
        protected Void doInBackground(Void... params) {
            this.mContentType = ShortcutIntentBuilder.this.mContext.getContentResolver().getType(this.mUri);
            loadData();
            loadPhoto();
            return null;
        }

        private void loadPhoto() {
            if (this.mPhotoId != 0) {
                ContentResolver resolver = ShortcutIntentBuilder.this.mContext.getContentResolver();
                Cursor cursor = resolver.query(ContactsContract.Data.CONTENT_URI, ShortcutIntentBuilder.PHOTO_COLUMNS, "_id=?", new String[]{String.valueOf(this.mPhotoId)}, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            this.mBitmapData = cursor.getBlob(0);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        }
    }

    private final class ContactLoadingAsyncTask extends LoadingAsyncTask {
        public ContactLoadingAsyncTask(Uri uri) {
            super(uri);
        }

        @Override
        protected void loadData() {
            ContentResolver resolver = ShortcutIntentBuilder.this.mContext.getContentResolver();
            Cursor cursor = resolver.query(this.mUri, ShortcutIntentBuilder.CONTACT_COLUMNS, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        this.mDisplayName = cursor.getString(0);
                        this.mPhotoId = cursor.getLong(1);
                        this.mLookupKey = cursor.getString(2);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            ShortcutIntentBuilder.this.createContactShortcutIntent(this.mUri, this.mContentType, this.mDisplayName, this.mLookupKey, this.mBitmapData);
        }
    }

    private final class PhoneNumberLoadingAsyncTask extends LoadingAsyncTask {
        private String mPhoneLabel;
        private String mPhoneNumber;
        private int mPhoneType;
        private final String mShortcutAction;

        public PhoneNumberLoadingAsyncTask(Uri uri, String shortcutAction) {
            super(uri);
            this.mShortcutAction = shortcutAction;
        }

        @Override
        protected void loadData() {
            ContentResolver resolver = ShortcutIntentBuilder.this.mContext.getContentResolver();
            Cursor cursor = resolver.query(this.mUri, ShortcutIntentBuilder.PHONE_COLUMNS, null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        this.mDisplayName = cursor.getString(0);
                        this.mPhotoId = cursor.getLong(1);
                        this.mPhoneNumber = cursor.getString(2);
                        this.mPhoneType = cursor.getInt(3);
                        this.mPhoneLabel = cursor.getString(4);
                        this.mLookupKey = cursor.getString(5);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            ShortcutIntentBuilder.this.createPhoneNumberShortcutIntent(this.mUri, this.mDisplayName, this.mLookupKey, this.mBitmapData, this.mPhoneNumber, this.mPhoneType, this.mPhoneLabel, this.mShortcutAction);
        }
    }

    private Drawable getPhotoDrawable(byte[] bitmapData, String displayName, String lookupKey) {
        if (bitmapData == null) {
            return ContactPhotoManager.getDefaultAvatarDrawableForContact(this.mContext.getResources(), false, new ContactPhotoManager.DefaultImageRequest(displayName, lookupKey, false));
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length, null);
        return new BitmapDrawable(this.mContext.getResources(), bitmap);
    }

    private void createContactShortcutIntent(Uri contactUri, String contentType, String displayName, String lookupKey, byte[] bitmapData) {
        Drawable drawable = getPhotoDrawable(bitmapData, displayName, lookupKey);
        Intent shortcutIntent = new Intent("android.provider.action.QUICK_CONTACT");
        shortcutIntent.addFlags(268533760);
        shortcutIntent.putExtra("com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION", true);
        shortcutIntent.setDataAndType(contactUri, contentType);
        shortcutIntent.putExtra("android.provider.extra.EXCLUDE_MIMES", (String[]) null);
        Bitmap icon = generateQuickContactIcon(drawable);
        Intent intent = new Intent();
        intent.putExtra("android.intent.extra.shortcut.ICON", icon);
        intent.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
        if (TextUtils.isEmpty(displayName)) {
            intent.putExtra("android.intent.extra.shortcut.NAME", this.mContext.getResources().getString(R.string.missing_name));
        } else {
            intent.putExtra("android.intent.extra.shortcut.NAME", displayName);
        }
        this.mListener.onShortcutIntentCreated(contactUri, intent);
    }

    private void createPhoneNumberShortcutIntent(Uri uri, String displayName, String lookupKey, byte[] bitmapData, String phoneNumber, int phoneType, String phoneLabel, String shortcutAction) {
        Uri phoneUri;
        Bitmap bitmap;
        Drawable drawable = getPhotoDrawable(bitmapData, displayName, lookupKey);
        if ("android.intent.action.CALL".equals(shortcutAction)) {
            phoneUri = Uri.fromParts("tel", phoneNumber, null);
            bitmap = generatePhoneNumberIcon(drawable, phoneType, phoneLabel, R.drawable.badge_action_call);
        } else {
            phoneUri = Uri.fromParts("smsto", phoneNumber, null);
            bitmap = generatePhoneNumberIcon(drawable, phoneType, phoneLabel, R.drawable.badge_action_sms);
        }
        Intent shortcutIntent = new Intent(shortcutAction, phoneUri);
        shortcutIntent.setFlags(67108864);
        Intent intent = new Intent();
        intent.putExtra("android.intent.extra.shortcut.ICON", bitmap);
        intent.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
        intent.putExtra("android.intent.extra.shortcut.NAME", displayName);
        this.mListener.onShortcutIntentCreated(uri, intent);
    }

    private Bitmap generateQuickContactIcon(Drawable photo) {
        Bitmap bitmap = Bitmap.createBitmap(this.mIconSize, this.mIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Rect dst = new Rect(0, 0, this.mIconSize, this.mIconSize);
        photo.setBounds(dst);
        photo.draw(canvas);
        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(this.mResources, bitmap);
        roundedDrawable.setAntiAlias(true);
        roundedDrawable.setCornerRadius(this.mIconSize / 2);
        Bitmap roundedBitmap = Bitmap.createBitmap(this.mIconSize, this.mIconSize, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(roundedBitmap);
        roundedDrawable.setBounds(dst);
        roundedDrawable.draw(canvas);
        canvas.setBitmap(null);
        return roundedBitmap;
    }

    private Bitmap generatePhoneNumberIcon(Drawable photo, int phoneType, String phoneLabel, int actionResId) {
        Resources r = this.mContext.getResources();
        float density = r.getDisplayMetrics().density;
        Bitmap phoneIcon = ((BitmapDrawable) r.getDrawableForDensity(actionResId, this.mIconDensity)).getBitmap();
        Bitmap icon = generateQuickContactIcon(photo);
        Canvas canvas = new Canvas(icon);
        Paint photoPaint = new Paint();
        photoPaint.setDither(true);
        photoPaint.setFilterBitmap(true);
        Rect dst = new Rect(0, 0, this.mIconSize, this.mIconSize);
        CharSequence overlay = ContactsContract.CommonDataKinds.Phone.getTypeLabel(r, phoneType, phoneLabel);
        if (overlay != null) {
            TextPaint textPaint = new TextPaint(257);
            textPaint.setTextSize(r.getDimension(R.dimen.shortcut_overlay_text_size));
            textPaint.setColor(r.getColor(R.color.textColorIconOverlay));
            textPaint.setShadowLayer(4.0f, 0.0f, 2.0f, r.getColor(R.color.textColorIconOverlayShadow));
            Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt();
            Paint workPaint = new Paint();
            workPaint.setColor(this.mOverlayTextBackgroundColor);
            workPaint.setStyle(Paint.Style.FILL);
            int textPadding = r.getDimensionPixelOffset(R.dimen.shortcut_overlay_text_background_padding);
            int textBandHeight = (fmi.descent - fmi.ascent) + (textPadding * 2);
            dst.set(0, this.mIconSize - textBandHeight, this.mIconSize, this.mIconSize);
            canvas.drawRect(dst, workPaint);
            CharSequence overlay2 = TextUtils.ellipsize(overlay, textPaint, this.mIconSize, TextUtils.TruncateAt.END);
            float textWidth = textPaint.measureText(overlay2, 0, overlay2.length());
            canvas.drawText(overlay2, 0, overlay2.length(), (this.mIconSize - textWidth) / 2.0f, (this.mIconSize - fmi.descent) - textPadding, textPaint);
        }
        Rect src = new Rect(0, 0, phoneIcon.getWidth(), phoneIcon.getHeight());
        int iconWidth = icon.getWidth();
        dst.set(iconWidth - ((int) (20.0f * density)), -1, iconWidth, (int) (19.0f * density));
        canvas.drawBitmap(phoneIcon, src, dst, photoPaint);
        canvas.setBitmap(null);
        return icon;
    }
}
