package com.android.phone;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.preference.Preference;
import android.telephony.CellInfo;
import com.android.settingslib.graph.SignalDrawable;
import java.util.List;

public class NetworkOperatorPreference extends Preference {
    private static final boolean DBG = false;
    private static final Drawable EMPTY_DRAWABLE = new ColorDrawable(0);
    private static final int NO_CELL_DATA_CONNECTED_ICON = 0;
    public static final int NUMBER_OF_LEVELS = 5;
    private static final String TAG = "NetworkOperatorPref";
    private CellInfo mCellInfo;
    private List<String> mForbiddenPlmns;
    private int mLevel;

    public NetworkOperatorPreference(CellInfo cellInfo, Context context, List<String> list) {
        super(context);
        this.mLevel = -1;
        this.mCellInfo = cellInfo;
        this.mForbiddenPlmns = list;
        refresh();
    }

    public CellInfo getCellInfo() {
        return this.mCellInfo;
    }

    public void refresh() {
        String networkTitle = CellInfoUtil.getNetworkTitle(this.mCellInfo);
        if (CellInfoUtil.isForbidden(this.mCellInfo, this.mForbiddenPlmns)) {
            networkTitle = networkTitle + " " + getContext().getResources().getString(R.string.forbidden_network);
        }
        setTitle(networkTitle);
        int level = CellInfoUtil.getLevel(this.mCellInfo);
        if (this.mLevel != level) {
            this.mLevel = level;
            updateIcon(this.mLevel);
        }
    }

    @Override
    public void setIcon(int i) {
        updateIcon(i);
    }

    private int getIconId(int i) {
        if (i == 4) {
            return R.drawable.signal_strength_1x;
        }
        if (i == 13) {
            return R.drawable.signal_strength_lte;
        }
        if (i == 3) {
            return R.drawable.signal_strength_3g;
        }
        if (i == 16) {
            return R.drawable.signal_strength_g;
        }
        return 0;
    }

    private void updateIcon(int i) {
        Drawable drawable;
        if (i < 0 || i >= 5) {
            return;
        }
        Context context = getContext();
        int state = SignalDrawable.getState(i, 5, false);
        SignalDrawable signalDrawable = new SignalDrawable(getContext());
        signalDrawable.setLevel(state);
        signalDrawable.setDarkIntensity(0.0f);
        int iconId = getIconId(CellInfoUtil.getNetworkType(this.mCellInfo));
        if (iconId == 0) {
            drawable = EMPTY_DRAWABLE;
        } else {
            drawable = getContext().getResources().getDrawable(iconId, getContext().getTheme());
        }
        Drawable[] drawableArr = {drawable, signalDrawable};
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.signal_strength_icon_size);
        LayerDrawable layerDrawable = new LayerDrawable(drawableArr);
        layerDrawable.setLayerGravity(0, 51);
        layerDrawable.setLayerGravity(1, 85);
        layerDrawable.setLayerSize(1, dimensionPixelSize, dimensionPixelSize);
        setIcon(layerDrawable);
    }
}
