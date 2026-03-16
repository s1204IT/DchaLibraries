package com.android.gallery3d.filtershow.info;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
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

    private String createStringFromIfFound(ExifTag exifTag, int tag, int str) {
        short tagId = exifTag.getTagId();
        if (tagId != ExifInterface.getTrueTagKey(tag)) {
            return "";
        }
        String label = getActivity().getString(str);
        String exifString = "<b>" + label + ": </b>";
        return (exifString + exifTag.forceGetValueAsString()) + "<br>";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().getWindow().requestFeature(1);
        }
        this.mMainView = (LinearLayout) inflater.inflate(R.layout.filtershow_info_panel, (ViewGroup) null, false);
        this.mImageThumbnail = (ImageView) this.mMainView.findViewById(R.id.imageThumbnail);
        Bitmap bitmap = MasterImage.getImage().getFilteredImage();
        this.mImageThumbnail.setImageBitmap(bitmap);
        this.mImageName = (TextView) this.mMainView.findViewById(R.id.imageName);
        this.mImageSize = (TextView) this.mMainView.findViewById(R.id.imageSize);
        this.mExifData = (TextView) this.mMainView.findViewById(R.id.exifData);
        TextView exifLabel = (TextView) this.mMainView.findViewById(R.id.exifLabel);
        HistogramView histogramView = (HistogramView) this.mMainView.findViewById(R.id.histogramView);
        histogramView.setBitmap(bitmap);
        Uri uri = MasterImage.getImage().getUri();
        String path = ImageLoader.getLocalPathFromUri(getActivity(), uri);
        Uri localUri = null;
        if (path != null) {
            localUri = Uri.parse(path);
        }
        if (localUri != null) {
            this.mImageName.setText(localUri.getLastPathSegment());
        }
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        this.mImageSize.setText("" + originalBounds.width() + " x " + originalBounds.height());
        List<ExifTag> exif = MasterImage.getImage().getEXIF();
        String exifString = "";
        boolean hasExifData = false;
        if (exif != null) {
            for (ExifTag tag : exif) {
                exifString = ((((((((exifString + createStringFromIfFound(tag, ExifInterface.TAG_MODEL, R.string.filtershow_exif_model)) + createStringFromIfFound(tag, ExifInterface.TAG_APERTURE_VALUE, R.string.filtershow_exif_aperture)) + createStringFromIfFound(tag, ExifInterface.TAG_FOCAL_LENGTH, R.string.filtershow_exif_focal_length)) + createStringFromIfFound(tag, ExifInterface.TAG_ISO_SPEED_RATINGS, R.string.filtershow_exif_iso)) + createStringFromIfFound(tag, ExifInterface.TAG_SUBJECT_DISTANCE, R.string.filtershow_exif_subject_distance)) + createStringFromIfFound(tag, ExifInterface.TAG_DATE_TIME_ORIGINAL, R.string.filtershow_exif_date)) + createStringFromIfFound(tag, ExifInterface.TAG_F_NUMBER, R.string.filtershow_exif_f_stop)) + createStringFromIfFound(tag, ExifInterface.TAG_EXPOSURE_TIME, R.string.filtershow_exif_exposure_time)) + createStringFromIfFound(tag, ExifInterface.TAG_COPYRIGHT, R.string.filtershow_exif_copyright);
                hasExifData = true;
            }
        }
        if (hasExifData) {
            exifLabel.setVisibility(0);
            this.mExifData.setText(Html.fromHtml(exifString));
        } else {
            exifLabel.setVisibility(8);
        }
        return this.mMainView;
    }
}
