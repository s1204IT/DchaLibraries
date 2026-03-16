package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentManagerImpl;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.ui.DetailsAddressResolver;

public class DetailsHelper {
    private static DetailsAddressResolver sAddressResolver;
    private DetailsViewContainer mContainer;

    public interface CloseListener {
        void onClose();
    }

    public interface DetailsSource {
        MediaDetails getDetails();

        int setIndex();

        int size();
    }

    public interface DetailsViewContainer {
        void hide();

        void reloadDetails();

        void setCloseListener(CloseListener closeListener);

        void show();
    }

    public interface ResolutionResolvingListener {
        void onResolutionAvailable(int i, int i2);
    }

    public DetailsHelper(AbstractGalleryActivity activity, GLView rootPane, DetailsSource source) {
        this.mContainer = new DialogDetailsView(activity, source);
    }

    public void layout(int left, int top, int right, int bottom) {
        if (this.mContainer instanceof GLView) {
            GLView view = (GLView) this.mContainer;
            view.measure(0, View.MeasureSpec.makeMeasureSpec(bottom - top, Integer.MIN_VALUE));
            view.layout(0, top, view.getMeasuredWidth(), view.getMeasuredHeight() + top);
        }
    }

    public void reloadDetails() {
        this.mContainer.reloadDetails();
    }

    public void setCloseListener(CloseListener listener) {
        this.mContainer.setCloseListener(listener);
    }

    public static String resolveAddress(AbstractGalleryActivity activity, double[] latlng, DetailsAddressResolver.AddressResolvingListener listener) {
        if (sAddressResolver == null) {
            sAddressResolver = new DetailsAddressResolver(activity);
        } else {
            sAddressResolver.cancel();
        }
        return sAddressResolver.resolveAddress(latlng, listener);
    }

    public static void resolveResolution(String path, ResolutionResolvingListener listener) {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if (bitmap != null) {
            listener.onResolutionAvailable(bitmap.getWidth(), bitmap.getHeight());
        }
    }

    public static void pause() {
        if (sAddressResolver != null) {
            sAddressResolver.cancel();
        }
    }

    public void show() {
        this.mContainer.show();
    }

    public void hide() {
        this.mContainer.hide();
    }

    public static String getDetailsName(Context context, int key) {
        switch (key) {
            case 1:
                return context.getString(R.string.title);
            case 2:
                return context.getString(R.string.description);
            case 3:
                return context.getString(R.string.time);
            case 4:
                return context.getString(R.string.location);
            case 5:
                return context.getString(R.string.width);
            case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                return context.getString(R.string.height);
            case 7:
                return context.getString(R.string.orientation);
            case NotificationCompat.FLAG_ONLY_ALERT_ONCE:
                return context.getString(R.string.duration);
            case 9:
                return context.getString(R.string.mimetype);
            case 10:
                return context.getString(R.string.file_size);
            case 100:
                return context.getString(R.string.maker);
            case 101:
                return context.getString(R.string.model);
            case 102:
                return context.getString(R.string.flash);
            case 103:
                return context.getString(R.string.focal_length);
            case 104:
                return context.getString(R.string.white_balance);
            case 105:
                return context.getString(R.string.aperture);
            case 107:
                return context.getString(R.string.exposure_time);
            case 108:
                return context.getString(R.string.iso);
            case 200:
                return context.getString(R.string.path);
            default:
                return "Unknown key" + key;
        }
    }
}
