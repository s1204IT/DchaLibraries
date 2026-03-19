package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    public DetailsHelper(AbstractGalleryActivity abstractGalleryActivity, GLView gLView, DetailsSource detailsSource) {
        this.mContainer = new DialogDetailsView(abstractGalleryActivity, detailsSource);
    }

    public void layout(int i, int i2, int i3, int i4) {
        if (this.mContainer instanceof GLView) {
            GLView gLView = (GLView) this.mContainer;
            gLView.measure(0, View.MeasureSpec.makeMeasureSpec(i4 - i2, Integer.MIN_VALUE));
            gLView.layout(0, i2, gLView.getMeasuredWidth(), gLView.getMeasuredHeight() + i2);
        }
    }

    public void reloadDetails() {
        this.mContainer.reloadDetails();
    }

    public void setCloseListener(CloseListener closeListener) {
        this.mContainer.setCloseListener(closeListener);
    }

    public static String resolveAddress(AbstractGalleryActivity abstractGalleryActivity, double[] dArr, DetailsAddressResolver.AddressResolvingListener addressResolvingListener) {
        if (sAddressResolver == null) {
            sAddressResolver = new DetailsAddressResolver(abstractGalleryActivity);
        } else {
            sAddressResolver.cancel();
        }
        return sAddressResolver.resolveAddress(dArr, addressResolvingListener);
    }

    public static void resolveResolution(String str, ResolutionResolvingListener resolutionResolvingListener) {
        Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(str);
        if (bitmapDecodeFile == null) {
            return;
        }
        resolutionResolvingListener.onResolutionAvailable(bitmapDecodeFile.getWidth(), bitmapDecodeFile.getHeight());
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

    public static String getDetailsName(Context context, int i) {
        if (i != 200) {
            switch (i) {
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
                case 6:
                    return context.getString(R.string.height);
                case 7:
                    return context.getString(R.string.orientation);
                case 8:
                    return context.getString(R.string.duration);
                case 9:
                    return context.getString(R.string.mimetype);
                case 10:
                    return context.getString(R.string.file_size);
                default:
                    switch (i) {
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
                        default:
                            switch (i) {
                                case 107:
                                    return context.getString(R.string.exposure_time);
                                case 108:
                                    return context.getString(R.string.iso);
                                default:
                                    return "Unknown key" + i;
                            }
                    }
            }
        }
        return context.getString(R.string.path);
    }
}
