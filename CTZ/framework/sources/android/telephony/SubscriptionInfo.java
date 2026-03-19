package android.telephony;

import android.annotation.SystemApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.util.DisplayMetrics;
import java.util.Arrays;
import java.util.List;

public class SubscriptionInfo implements Parcelable {
    public static final Parcelable.Creator<SubscriptionInfo> CREATOR = new Parcelable.Creator<SubscriptionInfo>() {
        @Override
        public SubscriptionInfo createFromParcel(Parcel parcel) {
            return new SubscriptionInfo(parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.readCharSequence(), parcel.readCharSequence(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt(), Bitmap.CREATOR.createFromParcel(parcel), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readBoolean(), (UiccAccessRule[]) parcel.createTypedArray(UiccAccessRule.CREATOR), parcel.readString());
        }

        @Override
        public SubscriptionInfo[] newArray(int i) {
            return new SubscriptionInfo[i];
        }
    };
    private static final int TEXT_SIZE = 16;
    private UiccAccessRule[] mAccessRules;
    private String mCardId;
    private CharSequence mCarrierName;
    private String mCountryIso;
    private int mDataRoaming;
    private CharSequence mDisplayName;
    private String mIccId;
    private Bitmap mIconBitmap;
    private int mIconTint;
    private int mId;
    private boolean mIsEmbedded;
    private int mMcc;
    private int mMnc;
    private int mNameSource;
    private String mNumber;
    private int mSimSlotIndex;

    public SubscriptionInfo(int i, String str, int i2, CharSequence charSequence, CharSequence charSequence2, int i3, int i4, String str2, int i5, Bitmap bitmap, int i6, int i7, String str3) {
        this(i, str, i2, charSequence, charSequence2, i3, i4, str2, i5, bitmap, i6, i7, str3, false, null, null);
    }

    public SubscriptionInfo(int i, String str, int i2, CharSequence charSequence, CharSequence charSequence2, int i3, int i4, String str2, int i5, Bitmap bitmap, int i6, int i7, String str3, boolean z, UiccAccessRule[] uiccAccessRuleArr) {
        this(i, str, i2, charSequence, charSequence2, i3, i4, str2, i5, bitmap, i6, i7, str3, z, uiccAccessRuleArr, null);
    }

    public SubscriptionInfo(int i, String str, int i2, CharSequence charSequence, CharSequence charSequence2, int i3, int i4, String str2, int i5, Bitmap bitmap, int i6, int i7, String str3, boolean z, UiccAccessRule[] uiccAccessRuleArr, String str4) {
        this.mId = i;
        this.mIccId = str;
        this.mSimSlotIndex = i2;
        this.mDisplayName = charSequence;
        this.mCarrierName = charSequence2;
        this.mNameSource = i3;
        this.mIconTint = i4;
        this.mNumber = str2;
        this.mDataRoaming = i5;
        this.mIconBitmap = bitmap;
        this.mMcc = i6;
        this.mMnc = i7;
        this.mCountryIso = str3;
        this.mIsEmbedded = z;
        this.mAccessRules = uiccAccessRuleArr;
        this.mCardId = str4;
    }

    public int getSubscriptionId() {
        return this.mId;
    }

    public String getIccId() {
        return this.mIccId;
    }

    public int getSimSlotIndex() {
        return this.mSimSlotIndex;
    }

    public CharSequence getDisplayName() {
        return this.mDisplayName;
    }

    public void setDisplayName(CharSequence charSequence) {
        this.mDisplayName = charSequence;
    }

    public CharSequence getCarrierName() {
        return this.mCarrierName;
    }

    public void setCarrierName(CharSequence charSequence) {
        this.mCarrierName = charSequence;
    }

    public int getNameSource() {
        return this.mNameSource;
    }

    public Bitmap createIconBitmap(Context context) {
        int width = this.mIconBitmap.getWidth();
        int height = this.mIconBitmap.getHeight();
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(displayMetrics, width, height, this.mIconBitmap.getConfig());
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(this.mIconTint, PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(this.mIconBitmap, 0.0f, 0.0f, paint);
        paint.setColorFilter(null);
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.create("sans-serif", 0));
        paint.setColor(-1);
        paint.setTextSize(16.0f * displayMetrics.density);
        String str = String.format("%d", Integer.valueOf(this.mSimSlotIndex + 1));
        paint.getTextBounds(str, 0, 1, new Rect());
        canvas.drawText(str, (width / 2.0f) - r6.centerX(), (height / 2.0f) - r6.centerY(), paint);
        return bitmapCreateBitmap;
    }

    public int getIconTint() {
        return this.mIconTint;
    }

    public void setIconTint(int i) {
        this.mIconTint = i;
    }

    public String getNumber() {
        return this.mNumber;
    }

    public int getDataRoaming() {
        return this.mDataRoaming;
    }

    public int getMcc() {
        return this.mMcc;
    }

    public int getMnc() {
        return this.mMnc;
    }

    public String getCountryIso() {
        return this.mCountryIso;
    }

    public boolean isEmbedded() {
        return this.mIsEmbedded;
    }

    @Deprecated
    public boolean canManageSubscription(Context context) {
        return canManageSubscription(context, context.getPackageName());
    }

    @Deprecated
    public boolean canManageSubscription(Context context, String str) {
        if (!isEmbedded()) {
            throw new UnsupportedOperationException("Not an embedded subscription");
        }
        if (this.mAccessRules == null) {
            return false;
        }
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(str, 64);
            for (UiccAccessRule uiccAccessRule : this.mAccessRules) {
                if (uiccAccessRule.getCarrierPrivilegeStatus(packageInfo) == 1) {
                    return true;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Unknown package: " + str, e);
        }
    }

    @SystemApi
    public List<UiccAccessRule> getAccessRules() {
        if (!isEmbedded()) {
            throw new UnsupportedOperationException("Not an embedded subscription");
        }
        if (this.mAccessRules == null) {
            return null;
        }
        return Arrays.asList(this.mAccessRules);
    }

    public String getCardId() {
        return this.mCardId;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mId);
        parcel.writeString(this.mIccId);
        parcel.writeInt(this.mSimSlotIndex);
        parcel.writeCharSequence(this.mDisplayName);
        parcel.writeCharSequence(this.mCarrierName);
        parcel.writeInt(this.mNameSource);
        parcel.writeInt(this.mIconTint);
        parcel.writeString(this.mNumber);
        parcel.writeInt(this.mDataRoaming);
        parcel.writeInt(this.mMcc);
        parcel.writeInt(this.mMnc);
        parcel.writeString(this.mCountryIso);
        this.mIconBitmap.writeToParcel(parcel, i);
        parcel.writeBoolean(this.mIsEmbedded);
        parcel.writeTypedArray(this.mAccessRules, i);
        parcel.writeString(this.mCardId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static String givePrintableIccid(String str) {
        if (str != null) {
            if (str.length() <= 9 || Build.IS_DEBUGGABLE) {
                return str;
            }
            return str.substring(0, 9) + Rlog.pii(false, (Object) str.substring(9));
        }
        return null;
    }

    public String toString() {
        return "{id=" + this.mId + ", iccId=" + givePrintableIccid(this.mIccId) + " simSlotIndex=" + this.mSimSlotIndex + " displayName=" + ((Object) this.mDisplayName) + " carrierName=" + ((Object) this.mCarrierName) + " nameSource=" + this.mNameSource + " iconTint=" + this.mIconTint + " dataRoaming=" + this.mDataRoaming + " iconBitmap=" + this.mIconBitmap + " mcc " + this.mMcc + " mnc " + this.mMnc + " isEmbedded " + this.mIsEmbedded + " accessRules " + Arrays.toString(this.mAccessRules) + " cardId=" + givePrintableIccid(this.mCardId) + "}";
    }
}
