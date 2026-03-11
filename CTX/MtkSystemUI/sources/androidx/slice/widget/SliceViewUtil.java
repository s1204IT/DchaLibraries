package androidx.slice.widget;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.IconCompat;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.Calendar;

public class SliceViewUtil {
    public static int getColorAccent(Context context) {
        return getColorAttr(context, R.attr.colorAccent);
    }

    public static int getColorAttr(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    public static Drawable getDrawable(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        Drawable drawable = ta.getDrawable(0);
        ta.recycle();
        return drawable;
    }

    public static IconCompat createIconFromDrawable(Drawable d) {
        if (d instanceof BitmapDrawable) {
            return IconCompat.createWithBitmap(((BitmapDrawable) d).getBitmap());
        }
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return IconCompat.createWithBitmap(b);
    }

    public static void createCircledIcon(Context context, int iconSizePx, IconCompat icon, boolean isLarge, ViewGroup parent) {
        ImageView v = new ImageView(context);
        v.setImageDrawable(icon.loadDrawable(context));
        v.setScaleType(isLarge ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
        parent.addView(v);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        if (isLarge) {
            Bitmap iconBm = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBm);
            v.layout(0, 0, iconSizePx, iconSizePx);
            v.draw(iconCanvas);
            v.setImageBitmap(getCircularBitmap(iconBm));
        } else {
            v.setColorFilter(-1);
        }
        lp.width = iconSizePx;
        lp.height = iconSizePx;
        lp.gravity = 17;
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static CharSequence getRelativeTimeString(long time) {
        return DateUtils.getRelativeTimeSpanString(time, Calendar.getInstance().getTimeInMillis(), 60000L, 262144);
    }

    public static int resolveLayoutDirection(int layoutDir) {
        if (layoutDir == 2 || layoutDir == 3 || layoutDir == 1 || layoutDir == 0) {
            return layoutDir;
        }
        return -1;
    }
}
