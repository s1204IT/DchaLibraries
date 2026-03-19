package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.os.Build;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.ColorChooser;
import com.android.gallery3d.filtershow.filters.FilterColorBorderRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageShow;

public class EditorColorBorder extends ParametricEditor {
    int[] mBasColors;
    private String mParameterString;
    private EditorColorBorderTabletUI mTabletUI;

    public EditorColorBorder() {
        super(R.id.editorColorBorder);
        this.mBasColors = new int[]{FilterColorBorderRepresentation.DEFAULT_MENU_COLOR1, FilterColorBorderRepresentation.DEFAULT_MENU_COLOR2, FilterColorBorderRepresentation.DEFAULT_MENU_COLOR3, FilterColorBorderRepresentation.DEFAULT_MENU_COLOR4, FilterColorBorderRepresentation.DEFAULT_MENU_COLOR5};
    }

    @Override
    public String calculateUserMessage(Context context, String str, Object obj) {
        FilterColorBorderRepresentation colorBorderRep = getColorBorderRep();
        if (colorBorderRep == null) {
            return "";
        }
        if (this.mParameterString == null) {
            this.mParameterString = "";
        }
        return this.mParameterString + colorBorderRep.getValueString();
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        ImageShow imageShow = new ImageShow(context);
        this.mImageShow = imageShow;
        this.mView = imageShow;
        super.createEditor(context, frameLayout);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterColorBorderRepresentation)) {
            FilterColorBorderRepresentation filterColorBorderRepresentation = (FilterColorBorderRepresentation) getLocalRepresentation();
            if (!ParametricEditor.useCompact(this.mContext) && this.mTabletUI != null) {
                this.mTabletUI.setColorBorderRepresentation(filterColorBorderRepresentation);
            }
            filterColorBorderRepresentation.setPramMode(0);
            if (ParametricEditor.useCompact(this.mContext)) {
                this.mParameterString = this.mContext.getString(R.string.color_border_size);
            } else {
                this.mParameterString = this.mContext.getString(R.string.color_border_clear);
            }
            if (this.mEditControl != null) {
                control(filterColorBorderRepresentation.getCurrentParam(), this.mEditControl);
            }
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout linearLayout) {
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        if (ParametricEditor.useCompact(this.mContext)) {
            button.setText(this.mContext.getString(R.string.color_border_size));
        } else {
            button.setText(this.mContext.getString(R.string.color_border_clear));
            if (Build.VERSION.SDK_INT >= 17) {
                button.setCompoundDrawablesRelative(null, null, null, null);
            } else {
                button.setCompoundDrawables(null, null, null, null);
            }
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ParametricEditor.useCompact(EditorColorBorder.this.mContext)) {
                    EditorColorBorder.this.showPopupMenu(linearLayout);
                } else {
                    EditorColorBorder.this.clearFrame();
                }
            }
        });
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    private void showPopupMenu(LinearLayout linearLayout) {
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        if (button == null) {
            return;
        }
        PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), button);
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_color_border, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                EditorColorBorder.this.selectMenuItem(menuItem);
                return true;
            }
        });
        popupMenu.show();
        ((FilterShowActivity) this.mContext).onShowMenu(popupMenu);
    }

    protected void selectMenuItem(MenuItem menuItem) {
        FilterColorBorderRepresentation colorBorderRep = getColorBorderRep();
        if (colorBorderRep == null) {
            return;
        }
        switch (menuItem.getItemId()) {
            case R.id.color_border_menu_corner_size:
                colorBorderRep.setPramMode(1);
                break;
            case R.id.color_border_menu_size:
                colorBorderRep.setPramMode(0);
                break;
            case R.id.color_border_menu_color:
                colorBorderRep.setPramMode(2);
                break;
            case R.id.color_border_menu_clear:
                clearFrame();
                break;
        }
        if (menuItem.getItemId() != R.id.color_border_menu_clear) {
            this.mParameterString = menuItem.getTitle().toString();
        }
        if (this.mControl instanceof ColorChooser) {
            this.mBasColors = ((ColorChooser) this.mControl).getColorSet();
        }
        if (this.mEditControl != null) {
            control(colorBorderRep.getCurrentParam(), this.mEditControl);
        }
        if (this.mControl instanceof ColorChooser) {
            ((ColorChooser) this.mControl).setColorSet(this.mBasColors);
        }
        updateText();
        if (ParametricEditor.useCompact(this.mContext)) {
            this.mControl.updateUI();
        }
        this.mView.invalidate();
    }

    public void clearFrame() {
        FilterColorBorderRepresentation colorBorderRep = getColorBorderRep();
        if (colorBorderRep == null) {
            return;
        }
        colorBorderRep.clearToDefault();
        commitLocalRepresentation();
    }

    @Override
    public void setUtilityPanelUI(View view, View view2) {
        if (ParametricEditor.useCompact(this.mContext)) {
            super.setUtilityPanelUI(view, view2);
            return;
        }
        this.mSeekBar = (SeekBar) view2.findViewById(R.id.primarySeekBar);
        if (this.mSeekBar != null) {
            this.mSeekBar.setVisibility(8);
        }
        this.mTabletUI = new EditorColorBorderTabletUI(this, this.mContext, view2);
    }

    FilterColorBorderRepresentation getColorBorderRep() {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation instanceof FilterColorBorderRepresentation) {
            return localRepresentation;
        }
        return null;
    }
}
