package com.android.managedprovisioning.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.android.internal.annotations.VisibleForTesting;
import com.android.managedprovisioning.R;
import java.io.File;

public class LogoUtils {

    @VisibleForTesting
    static final int DEFAULT_LOGO_ID = 2131230838;

    public static void saveOrganisationLogo(Context context, Uri uri) {
        StoreUtils.copyUriIntoFile(context.getContentResolver(), uri, getOrganisationLogoFile(context));
    }

    public static Drawable getOrganisationLogo(Context context, Integer num) {
        Bitmap bitmapPartiallyResized;
        File organisationLogoFile = getOrganisationLogoFile(context);
        int dimension = (int) context.getResources().getDimension(R.dimen.max_logo_width);
        int dimension2 = (int) context.getResources().getDimension(R.dimen.max_logo_height);
        if (organisationLogoFile.exists()) {
            bitmapPartiallyResized = getBitmapPartiallyResized(organisationLogoFile.getPath(), dimension, dimension2);
            if (bitmapPartiallyResized == null) {
                ProvisionLogger.loge("Could not get organisation logo from " + organisationLogoFile);
            }
        } else {
            bitmapPartiallyResized = null;
        }
        if (bitmapPartiallyResized != null) {
            return new BitmapDrawable(context.getResources(), resizeBitmap(bitmapPartiallyResized, dimension, dimension2));
        }
        Drawable drawable = context.getDrawable(R.drawable.ic_enterprise_blue_24dp);
        if (num != null) {
            drawable.setColorFilter(num.intValue(), PorterDuff.Mode.SRC_ATOP);
        }
        return drawable;
    }

    @VisibleForTesting
    static Bitmap getBitmapPartiallyResized(String str, int i, int i2) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(str, options);
        int iMax = Math.max(options.outWidth / i, options.outHeight / i2);
        if (iMax > 1) {
            options.inSampleSize = iMax;
        }
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(str, options);
    }

    @VisibleForTesting
    static Bitmap resizeBitmap(Bitmap bitmap, int i, int i2) {
        double width = bitmap.getWidth();
        double height = bitmap.getHeight();
        double dMax = Math.max(width / ((double) i), height / ((double) i2));
        if (dMax > 1.0d) {
            return Bitmap.createScaledBitmap(bitmap, (int) (width / dMax), (int) (height / dMax), false);
        }
        return bitmap;
    }

    public static void cleanUp(Context context) {
        getOrganisationLogoFile(context).delete();
    }

    private static File getOrganisationLogoFile(Context context) {
        return new File(context.getFilesDir() + File.separator + "organisation_logo");
    }
}
