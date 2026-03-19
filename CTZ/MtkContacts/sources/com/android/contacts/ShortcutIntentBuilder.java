package com.android.contacts;

import android.app.ActivityManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
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
import android.support.v4.graphics.drawable.IconCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.os.BuildCompat;
import android.text.TextPaint;
import android.text.TextUtils;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.util.BitmapUtil;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.google.common.collect.Lists;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.ContactsPortableUtils;
import java.util.ArrayList;

public class ShortcutIntentBuilder {
    private static final String[] PHONE_COLUMNS;
    private static final String[] PHOTO_COLUMNS;
    private final Context mContext;
    private final int mIconDensity;
    private int mIconSize;
    private final OnShortcutIntentCreatedListener mListener;
    private final int mOverlayTextBackgroundColor;
    private final Resources mResources;
    private static final String[] CONTACT_COLUMNS = {"display_name", "photo_id", "lookup"};
    private static final String[] PHONE_COLUMNS_INTERNAL = {"display_name", "photo_id", "data1", "data2", "data3", "lookup"};

    public interface OnShortcutIntentCreatedListener {
        void onShortcutIntentCreated(Uri uri, Intent intent);
    }

    static {
        ArrayList arrayListNewArrayList = Lists.newArrayList(PHONE_COLUMNS_INTERNAL);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            arrayListNewArrayList.add("indicate_phone_or_sim_contact");
        }
        PHONE_COLUMNS = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
        PHOTO_COLUMNS = new String[]{"data15"};
    }

    public ShortcutIntentBuilder(Context context, OnShortcutIntentCreatedListener onShortcutIntentCreatedListener) {
        this.mContext = context;
        this.mListener = onShortcutIntentCreatedListener;
        this.mResources = context.getResources();
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        this.mIconSize = this.mResources.getDimensionPixelSize(R.dimen.shortcut_icon_size);
        if (this.mIconSize == 0) {
            this.mIconSize = activityManager.getLauncherLargeIconSize();
        }
        this.mIconDensity = activityManager.getLauncherLargeIconDensity();
        this.mOverlayTextBackgroundColor = this.mResources.getColor(R.color.shortcut_overlay_text_background);
    }

    public void createContactShortcutIntent(Uri uri) {
        new ContactLoadingAsyncTask(uri).execute(new Void[0]);
    }

    public void createPhoneNumberShortcutIntent(Uri uri, String str) {
        new PhoneNumberLoadingAsyncTask(uri, str).execute(new Void[0]);
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
        protected Void doInBackground(Void... voidArr) {
            this.mContentType = ShortcutIntentBuilder.this.mContext.getContentResolver().getType(this.mUri);
            loadData();
            loadPhoto();
            return null;
        }

        private void loadPhoto() {
            Cursor cursorQuery;
            if (this.mPhotoId != 0 && (cursorQuery = ShortcutIntentBuilder.this.mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, ShortcutIntentBuilder.PHOTO_COLUMNS, "_id=?", new String[]{String.valueOf(this.mPhotoId)}, null)) != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        this.mBitmapData = cursorQuery.getBlob(0);
                    }
                } finally {
                    cursorQuery.close();
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
            Cursor cursorQuery = ShortcutIntentBuilder.this.mContext.getContentResolver().query(this.mUri, ShortcutIntentBuilder.CONTACT_COLUMNS, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        this.mDisplayName = cursorQuery.getString(0);
                        this.mPhotoId = cursorQuery.getLong(1);
                        this.mLookupKey = cursorQuery.getString(2);
                    }
                } finally {
                    cursorQuery.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Void r7) {
            if (!TextUtils.isEmpty(this.mLookupKey)) {
                ShortcutIntentBuilder.this.createContactShortcutIntent(this.mUri, this.mContentType, this.mDisplayName, this.mLookupKey, this.mBitmapData);
            }
        }
    }

    private final class PhoneNumberLoadingAsyncTask extends LoadingAsyncTask {
        private String mPhoneLabel;
        private String mPhoneNumber;
        private int mPhoneType;
        private final String mShortcutAction;
        private int mSimId;

        public PhoneNumberLoadingAsyncTask(Uri uri, String str) {
            super(uri);
            this.mSimId = -1;
            this.mShortcutAction = str;
        }

        @Override
        protected void loadData() {
            Cursor cursorQuery = ShortcutIntentBuilder.this.mContext.getContentResolver().query(this.mUri, ShortcutIntentBuilder.PHONE_COLUMNS, null, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        this.mDisplayName = cursorQuery.getString(0);
                        this.mPhotoId = cursorQuery.getLong(1);
                        this.mPhoneNumber = cursorQuery.getString(2);
                        this.mPhoneType = cursorQuery.getInt(3);
                        this.mPhoneLabel = cursorQuery.getString(4);
                        this.mLookupKey = cursorQuery.getString(5);
                        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                            this.mSimId = cursorQuery.getInt(6);
                        }
                    }
                } finally {
                    cursorQuery.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Void r11) {
            if (!TextUtils.isEmpty(this.mLookupKey)) {
                ShortcutIntentBuilder.this.createPhoneNumberShortcutIntent(this.mUri, this.mDisplayName, this.mLookupKey, this.mBitmapData, this.mPhoneNumber, this.mPhoneType, this.mPhoneLabel, this.mShortcutAction, this.mSimId);
            }
        }
    }

    private Drawable getPhotoDrawable(byte[] bArr, String str, String str2) {
        if (bArr != null) {
            return new BitmapDrawable(this.mContext.getResources(), BitmapFactory.decodeByteArray(bArr, 0, bArr.length, null));
        }
        ContactPhotoManager.DefaultImageRequest defaultImageRequest = new ContactPhotoManager.DefaultImageRequest(str, str2, false);
        if (BuildCompat.isAtLeastO()) {
            defaultImageRequest.scale = LetterTileDrawable.getAdaptiveIconScale();
        }
        return ContactPhotoManager.getDefaultAvatarDrawableForContact(this.mContext.getResources(), false, defaultImageRequest);
    }

    private void createContactShortcutIntent(Uri uri, String str, String str2, String str3, byte[] bArr) {
        Intent intent;
        if (TextUtils.isEmpty(str2)) {
            str2 = this.mContext.getResources().getString(R.string.missing_name);
        }
        if (BuildCompat.isAtLeastO()) {
            long id = ContentUris.parseId(uri);
            ShortcutManager shortcutManager = (ShortcutManager) this.mContext.getSystemService("shortcut");
            ShortcutInfo quickContactShortcutInfo = new DynamicShortcuts(this.mContext).getQuickContactShortcutInfo(id, str3, str2);
            if (quickContactShortcutInfo != null) {
                intent = shortcutManager.createShortcutResultIntent(quickContactShortcutInfo);
            } else {
                intent = null;
            }
        }
        Drawable photoDrawable = getPhotoDrawable(bArr, str2, str3);
        Intent intentForQuickContactLauncherShortcut = ImplicitIntentsUtil.getIntentForQuickContactLauncherShortcut(this.mContext, uri);
        if (intent == null) {
            intent = new Intent();
        }
        Bitmap bitmapGenerateQuickContactIcon = generateQuickContactIcon(photoDrawable);
        if (BuildCompat.isAtLeastO()) {
            IconCompat.createWithAdaptiveBitmap(bitmapGenerateQuickContactIcon).addToShortcutIntent(intent, null, this.mContext);
        } else {
            intent.putExtra("android.intent.extra.shortcut.ICON", bitmapGenerateQuickContactIcon);
        }
        intent.putExtra("android.intent.extra.shortcut.INTENT", intentForQuickContactLauncherShortcut);
        intent.putExtra("android.intent.extra.shortcut.NAME", str2);
        this.mListener.onShortcutIntentCreated(uri, intent);
    }

    private void createPhoneNumberShortcutIntent(Uri uri, String str, String str2, byte[] bArr, String str3, int i, String str4, String str5, int i2) {
        Uri uriFromParts;
        Bitmap bitmapGeneratePhoneNumberIcon;
        String string;
        IconCompat iconCompatCreateWithAdaptiveBitmap;
        Intent intent;
        String string2 = str;
        Drawable photoDrawable = getPhotoDrawable(bArr, string2, str2);
        if (TextUtils.isEmpty(str)) {
            string2 = this.mContext.getResources().getString(R.string.missing_name);
        }
        String str6 = string2;
        if ("android.intent.action.CALL".equals(str5)) {
            uriFromParts = Uri.fromParts("tel", str3, null);
            bitmapGeneratePhoneNumberIcon = generatePhoneNumberIcon(photoDrawable, i, str4, R.drawable.quantum_ic_phone_vd_theme_24, i2);
            string = this.mContext.getResources().getString(R.string.call_by_shortcut, str6);
        } else {
            uriFromParts = Uri.fromParts(ContactsUtils.SCHEME_SMSTO, str3, null);
            bitmapGeneratePhoneNumberIcon = generatePhoneNumberIcon(photoDrawable, i, str4, R.drawable.quantum_ic_message_vd_theme_24, i2);
            string = this.mContext.getResources().getString(R.string.sms_by_shortcut, str6);
        }
        Intent intent2 = new Intent(str5, uriFromParts);
        intent2.setFlags(67108864);
        if (BuildCompat.isAtLeastO()) {
            iconCompatCreateWithAdaptiveBitmap = IconCompat.createWithAdaptiveBitmap(bitmapGeneratePhoneNumberIcon);
            ShortcutManager shortcutManager = (ShortcutManager) this.mContext.getSystemService("shortcut");
            ShortcutInfo actionShortcutInfo = new DynamicShortcuts(this.mContext).getActionShortcutInfo(str5 + str2 + uriFromParts.toString().hashCode(), str6, intent2, iconCompatCreateWithAdaptiveBitmap.toIcon());
            if (actionShortcutInfo != null) {
                intent = shortcutManager.createShortcutResultIntent(actionShortcutInfo);
            } else {
                intent = null;
            }
        } else {
            iconCompatCreateWithAdaptiveBitmap = null;
            intent = null;
        }
        if (intent == null) {
            intent = new Intent();
        }
        if (iconCompatCreateWithAdaptiveBitmap != null) {
            iconCompatCreateWithAdaptiveBitmap.addToShortcutIntent(intent, null, this.mContext);
        } else {
            intent.putExtra("android.intent.extra.shortcut.ICON", bitmapGeneratePhoneNumberIcon);
        }
        intent.putExtra("android.intent.extra.shortcut.INTENT", intent2);
        intent.putExtra("android.intent.extra.shortcut.NAME", string);
        this.mListener.onShortcutIntentCreated(uri, intent);
    }

    private Bitmap generateQuickContactIcon(Drawable drawable) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(this.mIconSize, this.mIconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Rect rect = new Rect(0, 0, this.mIconSize, this.mIconSize);
        drawable.setBounds(rect);
        drawable.draw(canvas);
        if (BuildCompat.isAtLeastO()) {
            return bitmapCreateBitmap;
        }
        RoundedBitmapDrawable roundedBitmapDrawableCreate = RoundedBitmapDrawableFactory.create(this.mResources, bitmapCreateBitmap);
        roundedBitmapDrawableCreate.setAntiAlias(true);
        roundedBitmapDrawableCreate.setCornerRadius(this.mIconSize / 2);
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(this.mIconSize, this.mIconSize, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmapCreateBitmap2);
        roundedBitmapDrawableCreate.setBounds(rect);
        roundedBitmapDrawableCreate.draw(canvas);
        canvas.setBitmap(null);
        return bitmapCreateBitmap2;
    }

    private Bitmap generatePhoneNumberIcon(Drawable drawable, int i, String str, int i2, int i3) {
        Rect rect;
        Resources resources = this.mContext.getResources();
        float f = resources.getDisplayMetrics().density;
        Drawable drawableForDensity = resources.getDrawableForDensity(i2, this.mIconDensity);
        Bitmap bitmapDrawableToBitmap = BitmapUtil.drawableToBitmap(drawableForDensity, drawableForDensity.getIntrinsicHeight());
        Bitmap bitmapGenerateQuickContactIcon = generateQuickContactIcon(drawable);
        Canvas canvas = new Canvas(bitmapGenerateQuickContactIcon);
        Paint paint = new Paint();
        paint.setDither(true);
        paint.setFilterBitmap(true);
        Rect rect2 = new Rect(0, 0, this.mIconSize, this.mIconSize);
        CharSequence typeLabel = GlobalEnv.getSimAasEditor().getTypeLabel(i, str, (String) ContactsContract.CommonDataKinds.Phone.getTypeLabel(resources, i, str), i3);
        if (!BuildCompat.isAtLeastO() && typeLabel != null) {
            TextPaint textPaint = new TextPaint(257);
            textPaint.setTextSize(resources.getDimension(R.dimen.shortcut_overlay_text_size));
            textPaint.setColor(resources.getColor(R.color.textColorIconOverlay));
            textPaint.setShadowLayer(4.0f, ContactPhotoManager.OFFSET_DEFAULT, 2.0f, resources.getColor(R.color.textColorIconOverlayShadow));
            Paint.FontMetricsInt fontMetricsInt = textPaint.getFontMetricsInt();
            Paint paint2 = new Paint();
            paint2.setColor(this.mOverlayTextBackgroundColor);
            paint2.setStyle(Paint.Style.FILL);
            rect2.set(0, this.mIconSize - ((fontMetricsInt.descent - fontMetricsInt.ascent) + (resources.getDimensionPixelOffset(R.dimen.shortcut_overlay_text_background_padding) * 2)), this.mIconSize, this.mIconSize);
            canvas.drawRect(rect2, paint2);
            CharSequence charSequenceEllipsize = TextUtils.ellipsize(typeLabel, textPaint, this.mIconSize, TextUtils.TruncateAt.END);
            rect = rect2;
            canvas.drawText(charSequenceEllipsize, 0, charSequenceEllipsize.length(), (this.mIconSize - textPaint.measureText(charSequenceEllipsize, 0, charSequenceEllipsize.length())) / 2.0f, (this.mIconSize - fontMetricsInt.descent) - r3, textPaint);
        } else {
            rect = rect2;
        }
        int width = bitmapGenerateQuickContactIcon.getWidth();
        if (BuildCompat.isAtLeastO()) {
            canvas.drawBitmap(bitmapDrawableToBitmap, (int) (this.mIconSize - (45.0f * f)), (int) (21.0f * f), paint);
        } else {
            rect.set(width - ((int) (20.0f * f)), -1, width, (int) (19.0f * f));
            canvas.drawBitmap(bitmapDrawableToBitmap, (Rect) null, rect, paint);
        }
        canvas.setBitmap(null);
        return bitmapGenerateQuickContactIcon;
    }
}
