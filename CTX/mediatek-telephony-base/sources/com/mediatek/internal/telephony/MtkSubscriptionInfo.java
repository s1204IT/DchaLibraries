package com.mediatek.internal.telephony;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.DisplayMetrics;

public class MtkSubscriptionInfo extends SubscriptionInfo {
    public static final Parcelable.Creator<MtkSubscriptionInfo> CREATOR;
    private static final boolean IS_DEBUG_BUILD;
    private static final String LOG_TAG = "MtkSubscriptionInfo";
    private static final int TEXT_SIZE = 16;
    private Bitmap mIconBitmap;

    static {
        IS_DEBUG_BUILD = Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");
        CREATOR = new Parcelable.Creator<MtkSubscriptionInfo>() {
            @Override
            public MtkSubscriptionInfo createFromParcel(Parcel parcel) {
                return new MtkSubscriptionInfo(parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readCharSequence(), parcel.readCharSequence(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt(), (Bitmap) Bitmap.CREATOR.createFromParcel(parcel), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readBoolean(), (UiccAccessRule[]) parcel.createTypedArray(UiccAccessRule.CREATOR), parcel.readString());
            }

            @Override
            public MtkSubscriptionInfo[] newArray(int i) {
                return new MtkSubscriptionInfo[i];
            }
        };
    }

    public MtkSubscriptionInfo(int i, String str, int i2, CharSequence charSequence, CharSequence charSequence2, int i3, int i4, String str2, int i5, Bitmap bitmap, int i6, int i7, String str3) {
        super(i, str, i2, charSequence, charSequence2, i3, i4, str2, i5, bitmap, i6, i7, str3);
        this.mIconBitmap = bitmap;
    }

    public MtkSubscriptionInfo(int i, String str, int i2, CharSequence charSequence, CharSequence charSequence2, int i3, int i4, String str2, int i5, Bitmap bitmap, int i6, int i7, String str3, boolean z, UiccAccessRule[] uiccAccessRuleArr) {
        super(i, str, i2, charSequence, charSequence2, i3, i4, str2, i5, bitmap, i6, i7, str3, z, uiccAccessRuleArr);
        this.mIconBitmap = bitmap;
    }

    public MtkSubscriptionInfo(int i, String str, int i2, CharSequence charSequence, CharSequence charSequence2, int i3, int i4, String str2, int i5, Bitmap bitmap, int i6, int i7, String str3, boolean z, UiccAccessRule[] uiccAccessRuleArr, String str4) {
        super(i, str, i2, charSequence, charSequence2, i3, i4, str2, i5, bitmap, i6, i7, str3, z, uiccAccessRuleArr, str4);
        this.mIconBitmap = bitmap;
    }

    @Override
    public Bitmap createIconBitmap(Context context) {
        return createIconBitmap(context, -1, true);
    }

    public Bitmap createIconBitmap(Context context, int i) {
        return createIconBitmap(context, i, true);
    }

    public Bitmap createIconBitmap(Context context, int i, boolean z) {
        Bitmap bitmapCreateBitmap;
        synchronized (this) {
            int width = this.mIconBitmap.getWidth();
            int height = this.mIconBitmap.getHeight();
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            if (IS_DEBUG_BUILD) {
                Rlog.d(LOG_TAG, "mIconBitmap width:" + width + " height:" + height + " metrics:" + displayMetrics.toString());
            }
            bitmapCreateBitmap = Bitmap.createBitmap(displayMetrics, width, height, this.mIconBitmap.getConfig());
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Paint paint = new Paint();
            if (i == -1) {
                i = getIconTint();
            }
            paint.setColorFilter(new PorterDuffColorFilter(i, PorterDuff.Mode.SRC_ATOP));
            canvas.drawBitmap(this.mIconBitmap, 0.0f, 0.0f, paint);
            paint.setColorFilter(null);
            if (z) {
                paint.setAntiAlias(true);
                paint.setTypeface(Typeface.create("sans-serif", 0));
                paint.setColor(-1);
                paint.setTextSize(16.0f * displayMetrics.density);
                String str = String.format("%d", Integer.valueOf(getSimSlotIndex() + 1));
                paint.getTextBounds(str, 0, 1, new Rect());
                canvas.drawText(str, (width / 2.0f) - r11.centerX(), (height / 2.0f) - r11.centerY(), paint);
            }
        }
        return bitmapCreateBitmap;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        synchronized (this) {
            super.writeToParcel(parcel, i);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String getCountryIso() {
        String countryIso = super.getCountryIso();
        if (TextUtils.isEmpty(countryIso)) {
            return TelephonyManager.getDefault().getSimCountryIso(super.getSubscriptionId());
        }
        return countryIso;
    }
}
