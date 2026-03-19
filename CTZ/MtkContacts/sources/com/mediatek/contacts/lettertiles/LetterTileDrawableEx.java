package com.mediatek.contacts.lettertiles;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.telephony.SubscriptionManager;
import com.android.contacts.ContactPhotoManager;
import com.android.contacts.R;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.ContactsCommonListUtils;
import com.mediatek.contacts.util.Log;
import java.util.HashMap;

public class LetterTileDrawableEx extends LetterTileDrawable {
    private static Bitmap DEFAULT_SIM_SDN_AVATAR_LOCKED;
    private int mBackgroundColor;
    private long mSdnPhotoId;
    private Paint mSimPaint;
    private int mSubId;
    private static final String TAG = LetterTileDrawableEx.class.getSimpleName();
    private static float SIM_AVATAR_HEIGHT_RATIO = 0.32f;
    private static float SIM_AVATAR_WIDTH_RATIO = 0.32f;
    private static float SDN_LOCKED_RATIO = 0.3f;
    private static int SIM_ALPHA = 240;
    private static final Paint sPaint = new Paint();
    private static HashMap<Integer, IconEntry> BITMAP_ICONS = new HashMap<>();

    public LetterTileDrawableEx(Resources resources) {
        super(resources);
        this.mSdnPhotoId = 0L;
        this.mSubId = 0;
        this.mSimPaint = new Paint();
        this.mSimPaint.setAntiAlias(true);
        this.mSimPaint.setDither(true);
        this.mBackgroundColor = resources.getColor(R.color.background_primary);
        if (DEFAULT_SIM_SDN_AVATAR_LOCKED == null) {
            DEFAULT_SIM_SDN_AVATAR_LOCKED = BitmapFactory.decodeResource(resources, R.drawable.sim_indicator_sim_locked);
        }
    }

    public void setSIMProperty(ContactPhotoManager.DefaultImageRequest defaultImageRequest) {
        if (defaultImageRequest.subId > 0) {
            this.mSubId = defaultImageRequest.subId;
            this.mSdnPhotoId = defaultImageRequest.photoId;
        }
    }

    class IconEntry {
        public Bitmap iconBitmap;
        public int iconTint;

        IconEntry() {
        }
    }

    public static void clearSimIconBitmaps() {
        BITMAP_ICONS.clear();
    }

    public void initSimIconBitmaps() {
        BITMAP_ICONS.clear();
        int[] activeSubscriptionIdList = SubInfoUtils.getActiveSubscriptionIdList();
        if (activeSubscriptionIdList == null) {
            Log.e(TAG, "[initSimIconBitmaps] maybe has no basic permissions!");
            return;
        }
        int length = activeSubscriptionIdList.length;
        for (int i = 0; i < length; i++) {
            IconEntry iconEntry = new IconEntry();
            iconEntry.iconBitmap = SubInfoUtils.getIconBitmap(activeSubscriptionIdList[i]);
            iconEntry.iconTint = SubInfoUtils.getColorUsingSubId(activeSubscriptionIdList[i]);
            BITMAP_ICONS.put(Integer.valueOf(SubscriptionManager.getSlotIndex(activeSubscriptionIdList[i])), iconEntry);
        }
    }

    private Bitmap getIconBitmapUsingSubId(int i) {
        IconEntry iconEntry = BITMAP_ICONS.get(Integer.valueOf(SubscriptionManager.getSlotIndex(i)));
        if (iconEntry != null) {
            return iconEntry.iconBitmap;
        }
        return null;
    }

    public Bitmap getIconBitmapCache(int i) {
        IconEntry iconEntry = BITMAP_ICONS.get(Integer.valueOf(SubscriptionManager.getSlotIndex(i)));
        if (iconEntry == null || SubInfoUtils.iconTintChange(iconEntry.iconTint, i)) {
            Log.d(TAG, "icon tint changed need to re-get sim icons bitmap");
            initSimIconBitmaps();
        }
        return getIconBitmapUsingSubId(i);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (SubInfoUtils.checkSubscriber(this.mSubId)) {
            Bitmap iconBitmapCache = getIconBitmapCache(this.mSubId);
            if (iconBitmapCache != null) {
                drawSimAvatar(iconBitmapCache, iconBitmapCache.getWidth(), iconBitmapCache.getHeight(), canvas);
            } else {
                Log.e(TAG, "[draw]bitmap is null !!! subId=" + this.mSubId);
            }
            if (ContactsCommonListUtils.isSdnPhotoId(this.mSdnPhotoId)) {
                Bitmap bitmap = DEFAULT_SIM_SDN_AVATAR_LOCKED;
                drawSdnAvatar(bitmap, bitmap.getWidth(), bitmap.getHeight(), canvas);
            }
        }
    }

    private void drawSimAvatar(Bitmap bitmap, int i, int i2, Canvas canvas) {
        Rect rectCopyBounds = copyBounds();
        rectCopyBounds.set((int) (rectCopyBounds.right - ((getScale() * SIM_AVATAR_WIDTH_RATIO) * rectCopyBounds.width())), (int) (rectCopyBounds.bottom - ((getScale() * SIM_AVATAR_HEIGHT_RATIO) * rectCopyBounds.height())), rectCopyBounds.right, rectCopyBounds.bottom);
        sPaint.setColor(this.mBackgroundColor);
        sPaint.setAntiAlias(true);
        canvas.drawCircle(rectCopyBounds.centerX(), rectCopyBounds.centerY(), (rectCopyBounds.width() / 2) * 1.2f, sPaint);
        canvas.drawBitmap(bitmap, (Rect) null, rectCopyBounds, this.mSimPaint);
    }

    private void drawSdnAvatar(Bitmap bitmap, int i, int i2, Canvas canvas) {
        Rect rectCopyBounds = copyBounds();
        rectCopyBounds.set(rectCopyBounds.left, (int) (rectCopyBounds.top + (getScale() * SDN_LOCKED_RATIO * rectCopyBounds.height())), (int) (rectCopyBounds.left + (getScale() * SDN_LOCKED_RATIO * rectCopyBounds.width())), (int) (rectCopyBounds.top + (2.0f * getScale() * SDN_LOCKED_RATIO * rectCopyBounds.height())));
        canvas.drawBitmap(bitmap, (Rect) null, rectCopyBounds, this.mSimPaint);
    }
}
