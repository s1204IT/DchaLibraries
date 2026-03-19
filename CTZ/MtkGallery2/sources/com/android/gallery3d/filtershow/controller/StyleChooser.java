package com.android.gallery3d.filtershow.controller;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.Editor;
import java.util.Vector;

public class StyleChooser implements Control {
    protected Editor mEditor;
    protected LinearLayout mLinearLayout;
    protected ParameterStyles mParameter;
    private int mSelected;
    private View mTopView;
    private int mTransparent;
    private final String LOGTAG = "StyleChooser";
    private Vector<ImageButton> mIconButton = new Vector<>();
    protected int mLayoutID = R.layout.filtershow_control_style_chooser;

    @Override
    public void setUp(ViewGroup viewGroup, Parameter parameter, Editor editor) {
        viewGroup.removeAllViews();
        this.mEditor = editor;
        Context context = viewGroup.getContext();
        this.mParameter = (ParameterStyles) parameter;
        this.mTopView = ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(this.mLayoutID, viewGroup, true);
        this.mLinearLayout = (LinearLayout) this.mTopView.findViewById(R.id.listStyles);
        this.mTopView.setVisibility(0);
        int numberOfStyles = this.mParameter.getNumberOfStyles();
        this.mIconButton.clear();
        Resources resources = context.getResources();
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(dimensionPixelSize, dimensionPixelSize);
        for (final int i = 0; i < numberOfStyles; i++) {
            final ImageButton imageButton = new ImageButton(context);
            imageButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageButton.setLayoutParams(layoutParams);
            imageButton.setBackgroundResource(R.drawable.filtershow_color_picker_circle);
            ((GradientDrawable) imageButton.getBackground()).setColor(android.R.color.transparent);
            this.mIconButton.add(imageButton);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    StyleChooser.this.mParameter.setSelected(i);
                    StyleChooser.this.resetBorders();
                }
            });
            this.mLinearLayout.addView(imageButton);
            this.mParameter.getIcon(i, new BitmapCaller() {
                @Override
                public void available(Bitmap bitmap) {
                    if (bitmap == null) {
                        return;
                    }
                    imageButton.setImageBitmap(bitmap);
                }
            });
        }
        this.mTransparent = resources.getColor(R.color.color_chooser_unslected_border);
        this.mSelected = resources.getColor(R.color.color_chooser_slected_border);
        resetBorders();
    }

    @Override
    public void setPrameter(Parameter parameter) {
        this.mParameter = (ParameterStyles) parameter;
        updateUI();
    }

    @Override
    public void updateUI() {
        if (this.mParameter == null) {
        }
    }

    private void resetBorders() {
        int selected = this.mParameter.getSelected();
        int numberOfStyles = this.mParameter.getNumberOfStyles();
        int i = 0;
        while (i < numberOfStyles) {
            ((GradientDrawable) this.mIconButton.get(i).getBackground()).setStroke(3, selected == i ? this.mSelected : this.mTransparent);
            i++;
        }
    }
}
