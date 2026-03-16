package com.android.gallery3d.filtershow.editors;

import android.content.Context;
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
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
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
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterColorBorderRepresentation rep = getColorBorderRep();
        if (rep == null) {
            return "";
        }
        if (this.mParameterString == null) {
            this.mParameterString = "";
        }
        String val = rep.getValueString();
        return this.mParameterString + val;
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
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null && (getLocalRepresentation() instanceof FilterColorBorderRepresentation)) {
            FilterColorBorderRepresentation cbRep = (FilterColorBorderRepresentation) getLocalRepresentation();
            if (!ParametricEditor.useCompact(this.mContext) && this.mTabletUI != null) {
                this.mTabletUI.setColorBorderRepresentation(cbRep);
            }
            cbRep.setPramMode(0);
            this.mParameterString = this.mContext.getString(R.string.color_border_size);
            if (this.mEditControl != null) {
                control(cbRep.getCurrentParam(), this.mEditControl);
            }
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        view.setText(this.mContext.getString(R.string.color_border_size));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditorColorBorder.this.showPopupMenu(accessoryViewList);
            }
        });
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    private void showPopupMenu(LinearLayout accessoryViewList) {
        Button button = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        if (button != null) {
            PopupMenu popupMenu = new PopupMenu(this.mImageShow.getActivity(), button);
            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_color_border, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    EditorColorBorder.this.selectMenuItem(item);
                    return true;
                }
            });
            popupMenu.show();
            ((FilterShowActivity) this.mContext).onShowMenu(popupMenu);
        }
    }

    protected void selectMenuItem(MenuItem item) {
        FilterColorBorderRepresentation rep = getColorBorderRep();
        if (rep != null) {
            switch (item.getItemId()) {
                case R.id.color_border_menu_corner_size:
                    rep.setPramMode(1);
                    break;
                case R.id.color_border_menu_size:
                    rep.setPramMode(0);
                    break;
                case R.id.color_border_menu_color:
                    rep.setPramMode(2);
                    break;
                case R.id.color_border_menu_clear:
                    clearFrame();
                    break;
            }
            if (item.getItemId() != R.id.color_border_menu_clear) {
                this.mParameterString = item.getTitle().toString();
            }
            if (this.mControl instanceof ColorChooser) {
                ColorChooser c = (ColorChooser) this.mControl;
                this.mBasColors = c.getColorSet();
            }
            if (this.mEditControl != null) {
                control(rep.getCurrentParam(), this.mEditControl);
            }
            if (this.mControl instanceof ColorChooser) {
                ColorChooser c2 = (ColorChooser) this.mControl;
                c2.setColorSet(this.mBasColors);
            }
            updateText();
            this.mControl.updateUI();
            this.mView.invalidate();
        }
    }

    public void clearFrame() {
        commitLocalRepresentation();
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        if (ParametricEditor.useCompact(this.mContext)) {
            super.setUtilityPanelUI(actionButton, editControl);
            return;
        }
        this.mSeekBar = (SeekBar) editControl.findViewById(R.id.primarySeekBar);
        if (this.mSeekBar != null) {
            this.mSeekBar.setVisibility(8);
        }
        this.mTabletUI = new EditorColorBorderTabletUI(this, this.mContext, editControl);
    }

    FilterColorBorderRepresentation getColorBorderRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep instanceof FilterColorBorderRepresentation) {
            return (FilterColorBorderRepresentation) rep;
        }
        return null;
    }
}
