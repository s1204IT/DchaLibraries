package com.android.gallery3d.filtershow.controller;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
    private View mTopView;
    private final String LOGTAG = "StyleChooser";
    private Vector<ImageButton> mIconButton = new Vector<>();
    protected int mLayoutID = R.layout.filtershow_control_style_chooser;

    @Override
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        this.mEditor = editor;
        Context context = container.getContext();
        this.mParameter = (ParameterStyles) parameter;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mTopView = inflater.inflate(this.mLayoutID, container, true);
        this.mLinearLayout = (LinearLayout) this.mTopView.findViewById(R.id.listStyles);
        this.mTopView.setVisibility(0);
        int n = this.mParameter.getNumberOfStyles();
        this.mIconButton.clear();
        Resources res = context.getResources();
        int dim = res.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(dim, dim);
        for (int i = 0; i < n; i++) {
            final ImageButton button = new ImageButton(context);
            button.setScaleType(ImageView.ScaleType.CENTER_CROP);
            button.setLayoutParams(lp);
            button.setBackgroundResource(android.R.color.transparent);
            this.mIconButton.add(button);
            final int buttonNo = i;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    StyleChooser.this.mParameter.setSelected(buttonNo);
                }
            });
            this.mLinearLayout.addView(button);
            this.mParameter.getIcon(i, new BitmapCaller() {
                @Override
                public void available(Bitmap bmap) {
                    if (bmap != null) {
                        button.setImageBitmap(bmap);
                    }
                }
            });
        }
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
}
