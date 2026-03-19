package com.android.gallery3d.filtershow.info;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import java.util.List;

public class InfoPanel extends DialogFragment {
    private TextView mExifData;
    private TextView mImageName;
    private TextView mImageSize;
    private ImageView mImageThumbnail;
    private LinearLayout mMainView;

    private String createStringFromIfFound(ExifTag exifTag, int i, int i2) {
        if (exifTag.getTagId() != ExifInterface.getTrueTagKey(i)) {
            return "";
        }
        return (("<b>" + getActivity().getString(i2) + ": </b>") + exifTag.forceGetValueAsString()) + "<br>";
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        String str;
        boolean z;
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(1);
        }
        this.mMainView = (LinearLayout) layoutInflater.inflate(R.layout.filtershow_info_panel, (ViewGroup) null, false);
        this.mImageThumbnail = (ImageView) this.mMainView.findViewById(R.id.imageThumbnail);
        Bitmap filteredImage = MasterImage.getImage().getFilteredImage();
        if (filteredImage == null) {
            return null;
        }
        this.mImageThumbnail.setImageBitmap(filteredImage);
        this.mImageName = (TextView) this.mMainView.findViewById(R.id.imageName);
        this.mImageSize = (TextView) this.mMainView.findViewById(R.id.imageSize);
        this.mExifData = (TextView) this.mMainView.findViewById(R.id.exifData);
        TextView textView = (TextView) this.mMainView.findViewById(R.id.exifLabel);
        ((HistogramView) this.mMainView.findViewById(R.id.histogramView)).setBitmap(filteredImage);
        String localPathFromUri = ImageLoader.getLocalPathFromUri(getActivity(), MasterImage.getImage().getUri());
        if (localPathFromUri != null) {
            this.mImageName.setText(localPathFromUri.substring(localPathFromUri.lastIndexOf("/") + 1));
        }
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        this.mImageSize.setText("" + originalBounds.width() + " x " + originalBounds.height());
        List<ExifTag> exif = MasterImage.getImage().getEXIF();
        if (exif == null) {
            str = "";
            z = false;
        } else {
            str = "";
            z = false;
            for (ExifTag exifTag : exif) {
                str = ((((((((str + createStringFromIfFound(exifTag, ExifInterface.TAG_MODEL, R.string.filtershow_exif_model)) + createStringFromIfFound(exifTag, ExifInterface.TAG_APERTURE_VALUE, R.string.filtershow_exif_aperture)) + createStringFromIfFound(exifTag, ExifInterface.TAG_FOCAL_LENGTH, R.string.filtershow_exif_focal_length)) + createStringFromIfFound(exifTag, ExifInterface.TAG_ISO_SPEED_RATINGS, R.string.filtershow_exif_iso)) + createStringFromIfFound(exifTag, ExifInterface.TAG_SUBJECT_DISTANCE, R.string.filtershow_exif_subject_distance)) + createStringFromIfFound(exifTag, ExifInterface.TAG_DATE_TIME_ORIGINAL, R.string.filtershow_exif_date)) + createStringFromIfFound(exifTag, ExifInterface.TAG_F_NUMBER, R.string.filtershow_exif_f_stop)) + createStringFromIfFound(exifTag, ExifInterface.TAG_EXPOSURE_TIME, R.string.filtershow_exif_exposure_time)) + createStringFromIfFound(exifTag, ExifInterface.TAG_COPYRIGHT, R.string.filtershow_exif_copyright);
                z = true;
            }
        }
        if (z) {
            textView.setVisibility(0);
            this.mExifData.setText(Html.fromHtml(str));
        } else {
            textView.setVisibility(8);
        }
        return this.mMainView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getView() == null) {
            dismissAllowingStateLoss();
        }
    }
}
