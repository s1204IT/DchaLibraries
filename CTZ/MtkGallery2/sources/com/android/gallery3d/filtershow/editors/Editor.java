package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.SwapButton;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import java.util.ArrayList;
import java.util.Collection;

public class Editor implements SeekBar.OnSeekBarChangeListener, SwapButton.SwapButtonListener {
    private Button mButton;
    protected Context mContext;
    Button mEditTitle;
    protected Button mFilterTitle;
    protected FrameLayout mFrameLayout;
    protected int mID;
    protected ImageShow mImageShow;
    protected SeekBar mSeekBar;
    protected View mView;
    public static byte SHOW_VALUE_UNDEFINED = -1;
    public static byte SHOW_VALUE_OFF = 0;
    public static byte SHOW_VALUE_INT = 1;
    private final String LOGTAG = "Editor";
    protected boolean mChangesGeometry = false;
    protected FilterRepresentation mLocalRepresentation = null;
    protected byte mShowParameter = SHOW_VALUE_UNDEFINED;

    public static void hackFixStrings(Menu menu) {
        int size = menu.size();
        for (int i = 0; i < size; i++) {
            MenuItem item = menu.getItem(i);
            item.setTitle(item.getTitle().toString().toUpperCase());
        }
    }

    public String calculateUserMessage(Context context, String str, Object obj) {
        return str.toUpperCase() + " " + obj;
    }

    protected Editor(int i) {
        this.mID = i;
    }

    public int getID() {
        return this.mID;
    }

    public boolean showsSeekBar() {
        return true;
    }

    public void setUpEditorUI(View view, View view2, Button button, Button button2) {
        this.mEditTitle = button;
        this.mFilterTitle = button2;
        this.mButton = button;
        MasterImage.getImage().resetGeometryImages(false);
        setUtilityPanelUI(view, view2);
    }

    public boolean showsPopupIndicator() {
        return false;
    }

    public void setUtilityPanelUI(View view, View view2) {
        Context context = view2.getContext();
        this.mSeekBar = (SeekBar) ((LinearLayout) ((LayoutInflater) context.getSystemService("layout_inflater")).inflate(R.layout.filtershow_seekbar, (ViewGroup) view2, true)).findViewById(R.id.primarySeekBar);
        this.mSeekBar.setOnSeekBarChangeListener(this);
        this.mSeekBar.setVisibility(8);
        if (context.getResources().getConfiguration().orientation == 1 && showsSeekBar()) {
            this.mSeekBar.setVisibility(0);
        }
        if (this.mButton != null) {
            setMenuIcon(showsPopupIndicator());
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
    }

    public void createEditor(Context context, FrameLayout frameLayout) {
        this.mContext = context;
        this.mFrameLayout = frameLayout;
        this.mLocalRepresentation = null;
    }

    protected void unpack(int i, int i2) {
        if (this.mView == null) {
            this.mView = this.mFrameLayout.findViewById(i);
            if (this.mView == null) {
                this.mView = ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(i2, (ViewGroup) this.mFrameLayout, false);
                this.mFrameLayout.addView(this.mView, this.mView.getLayoutParams());
            }
        }
        this.mImageShow = findImageShow(this.mView);
    }

    private ImageShow findImageShow(View view) {
        if (view instanceof ImageShow) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ?? childAt = viewGroup.getChildAt(i);
            if (childAt instanceof ImageShow) {
                return childAt;
            }
            if (childAt instanceof ViewGroup) {
                return findImageShow(childAt);
            }
        }
        return null;
    }

    public View getTopLevelView() {
        return this.mView;
    }

    public ImageShow getImageShow() {
        return this.mImageShow;
    }

    public void setVisibility(int i) {
        this.mView.setVisibility(i);
    }

    public FilterRepresentation getLocalRepresentation() {
        if (this.mLocalRepresentation == null) {
            ImagePreset preset = MasterImage.getImage().getPreset();
            FilterRepresentation currentFilterRepresentation = MasterImage.getImage().getCurrentFilterRepresentation();
            this.mLocalRepresentation = preset.getFilterRepresentationCopyFrom(currentFilterRepresentation);
            if (this.mShowParameter == SHOW_VALUE_UNDEFINED && currentFilterRepresentation != null) {
                this.mShowParameter = currentFilterRepresentation.showParameterValue() ? SHOW_VALUE_INT : SHOW_VALUE_OFF;
            }
        }
        return this.mLocalRepresentation;
    }

    public void commitLocalRepresentation() {
        commitLocalRepresentation(getLocalRepresentation());
    }

    public void commitLocalRepresentation(FilterRepresentation filterRepresentation) {
        ArrayList arrayList = new ArrayList(1);
        arrayList.add(filterRepresentation);
        commitLocalRepresentation(arrayList);
    }

    public void commitLocalRepresentation(Collection<FilterRepresentation> collection) {
        ImagePreset preset = MasterImage.getImage().getPreset();
        preset.updateFilterRepresentations(collection);
        if (this.mButton != null) {
            updateText();
        }
        if (this.mChangesGeometry) {
            MasterImage.getImage().resetGeometryImages(true);
        }
        MasterImage.getImage().invalidateFiltersOnly();
        preset.fillImageStateAdapter(MasterImage.getImage().getState());
    }

    public void finalApplyCalled() {
        commitLocalRepresentation();
    }

    protected void updateText() {
        String string = "";
        if (this.mLocalRepresentation != null) {
            string = this.mContext.getString(this.mLocalRepresentation.getTextId());
        }
        this.mButton.setText(calculateUserMessage(this.mContext, string, ""));
    }

    public void reflectCurrentFilter() {
        this.mLocalRepresentation = null;
        FilterRepresentation localRepresentation = getLocalRepresentation();
        if (localRepresentation != null && this.mFilterTitle != null && localRepresentation.getTextId() != 0) {
            this.mFilterTitle.setText(this.mContext.getString(localRepresentation.getTextId()).toUpperCase());
            updateText();
        }
    }

    public boolean useUtilityPanel() {
        return true;
    }

    public void openUtilityPanel(LinearLayout linearLayout) {
        setMenuIcon(showsPopupIndicator());
        if (this.mImageShow != null) {
            this.mImageShow.openUtilityPanel(linearLayout);
        }
    }

    protected void setMenuIcon(boolean z) {
        int i = Build.VERSION.SDK_INT;
        int i2 = R.drawable.filtershow_menu_marker_rtl;
        if (i >= 17) {
            Button button = this.mEditTitle;
            if (!z) {
                i2 = 0;
            }
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, i2, 0);
            return;
        }
        Button button2 = this.mEditTitle;
        if (!z) {
            i2 = 0;
        }
        button2.setCompoundDrawablesWithIntrinsicBounds(0, 0, i2, 0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void swapLeft(MenuItem menuItem) {
    }

    @Override
    public void swapRight(MenuItem menuItem) {
    }

    public void detach() {
        if (this.mImageShow != null) {
            this.mImageShow.detach();
        }
    }

    public void finalCancelCalled() {
    }
}
