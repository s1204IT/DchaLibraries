package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.MathUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import libcore.util.Objects;

public class QSTileView extends QSTileBaseView {
    protected final Context mContext;
    protected TextView mLabel;
    private ImageView mPadLock;
    private int mTilePaddingTopPx;
    private final int mTileSpacingPx;

    public QSTileView(Context context, QSIconView icon) {
        this(context, icon, false);
    }

    public QSTileView(Context context, QSIconView icon, boolean collapsedView) {
        super(context, icon, collapsedView);
        this.mContext = context;
        Resources res = context.getResources();
        this.mTileSpacingPx = res.getDimensionPixelSize(R.dimen.qs_tile_spacing);
        setClipChildren(false);
        setClickable(true);
        updateTopPadding();
        setId(View.generateViewId());
        createLabel();
        setOrientation(1);
        setGravity(17);
    }

    TextView getLabel() {
        return this.mLabel;
    }

    private void updateTopPadding() {
        Resources res = getResources();
        int padding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top);
        int largePadding = res.getDimensionPixelSize(R.dimen.qs_tile_padding_top_large_text);
        float largeFactor = (MathUtils.constrain(getResources().getConfiguration().fontScale, 1.0f, 1.3f) - 1.0f) / 0.29999995f;
        this.mTilePaddingTopPx = Math.round(((1.0f - largeFactor) * padding) + (largePadding * largeFactor));
        setPadding(this.mTileSpacingPx, this.mTilePaddingTopPx + this.mTileSpacingPx, this.mTileSpacingPx, this.mTileSpacingPx);
        requestLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTopPadding();
        FontSizeUtils.updateFontSize(this.mLabel, R.dimen.qs_tile_text_size);
    }

    protected void createLabel() {
        View view = LayoutInflater.from(this.mContext).inflate(R.layout.qs_tile_label, (ViewGroup) null);
        this.mLabel = (TextView) view.findViewById(R.id.tile_label);
        this.mPadLock = (ImageView) view.findViewById(R.id.restricted_padlock);
        addView(view);
    }

    @Override
    protected void handleStateChanged(QSTile.State state) {
        super.handleStateChanged(state);
        if (!Objects.equal(this.mLabel.getText(), state.label)) {
            this.mLabel.setText(state.label);
        }
        this.mLabel.setEnabled(!state.disabledByPolicy);
        this.mPadLock.setVisibility(state.disabledByPolicy ? 0 : 8);
    }
}
