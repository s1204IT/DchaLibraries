package com.android.documentsui.inspector;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.R;
import com.android.documentsui.ThumbnailLoader;
import com.android.documentsui.base.Display;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.inspector.HeaderTextSelector;
import com.android.documentsui.inspector.InspectorController;
import java.util.function.Consumer;

public final class HeaderView extends RelativeLayout implements InspectorController.HeaderDisplay {
    private final Context mContext;
    private final View mHeader;
    private Point mImageDimensions;
    private ImageView mThumbnail;
    private final TextView mTitle;

    public HeaderView(Context context) {
        this(context, null);
    }

    public HeaderView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public HeaderView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mContext = context;
        this.mHeader = layoutInflater.inflate(R.layout.inspector_header, (ViewGroup) null);
        this.mThumbnail = (ImageView) this.mHeader.findViewById(R.id.inspector_thumbnail);
        this.mTitle = (TextView) this.mHeader.findViewById(R.id.inspector_file_title);
        this.mImageDimensions = new Point((int) Display.screenWidth((Activity) context), this.mContext.getResources().getDimensionPixelSize(R.dimen.inspector_header_height));
        addView(this.mHeader);
    }

    @Override
    public void accept(DocumentInfo documentInfo, String str) {
        loadHeaderImage(documentInfo);
        this.mTitle.setText(str);
        this.mTitle.setCustomSelectionActionModeCallback(new HeaderTextSelector(this.mTitle, new HeaderTextSelector.Selector() {
            @Override
            public final void select(Spannable spannable, int i, int i2) {
                this.f$0.selectText(spannable, i, i2);
            }
        }));
    }

    private void selectText(Spannable spannable, int i, int i2) {
        Selection.setSelection(spannable, i, i2);
    }

    private void loadHeaderImage(final DocumentInfo documentInfo) {
        if (!documentInfo.isThumbnailSupported()) {
            showImage(documentInfo, null);
        } else {
            new ThumbnailLoader(documentInfo.derivedUri, this.mThumbnail, this.mImageDimensions, documentInfo.lastModified, new Consumer<Bitmap>() {
                @Override
                public void accept(Bitmap bitmap) {
                    HeaderView.this.showImage(documentInfo, bitmap);
                }
            }, false).executeOnExecutor(ProviderExecutor.forAuthority(documentInfo.derivedUri.getAuthority()), documentInfo.derivedUri);
        }
    }

    private void showImage(DocumentInfo documentInfo, Bitmap bitmap) {
        if (bitmap != null) {
            this.mThumbnail.resetPaddingToInitialValues();
            this.mThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            this.mThumbnail.setImageBitmap(bitmap);
        } else {
            this.mThumbnail.setPadding(0, 0, 0, this.mTitle.getHeight());
            Drawable typeDrawable = this.mContext.getContentResolver().getTypeDrawable(documentInfo.mimeType);
            this.mThumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.mThumbnail.setImageDrawable(typeDrawable);
        }
        this.mThumbnail.animate().alpha(1.0f).start();
    }
}
