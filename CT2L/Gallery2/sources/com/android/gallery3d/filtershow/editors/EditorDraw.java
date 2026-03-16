package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.BitmapCaller;
import com.android.gallery3d.filtershow.controller.ColorChooser;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageDraw;

public class EditorDraw extends ParametricEditor implements FilterView {
    int[] brushIcons;
    int[] mBasColors;
    private String mDrawString;
    public ImageDraw mImageDraw;
    private String mParameterString;
    private EditorDrawTabletUI mTabletUI;

    public EditorDraw() {
        super(R.id.editorDraw);
        this.brushIcons = new int[]{R.drawable.brush_flat, R.drawable.brush_round, R.drawable.brush_gauss, R.drawable.brush_marker, R.drawable.brush_spatter};
        this.mBasColors = new int[]{FilterDrawRepresentation.DEFAULT_MENU_COLOR1, FilterDrawRepresentation.DEFAULT_MENU_COLOR2, FilterDrawRepresentation.DEFAULT_MENU_COLOR3, FilterDrawRepresentation.DEFAULT_MENU_COLOR4, FilterDrawRepresentation.DEFAULT_MENU_COLOR5};
        this.mDrawString = null;
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterDrawRepresentation rep = getDrawRep();
        if (this.mDrawString != null) {
            this.mImageDraw.displayDrawLook();
            return this.mDrawString;
        }
        if (rep == null) {
            return "";
        }
        if (!ParametricEditor.useCompact(this.mContext)) {
        }
        if (this.mParameterString == null) {
            this.mParameterString = "";
        }
        String val = rep.getValueString();
        this.mImageDraw.displayDrawLook();
        return this.mParameterString + val;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        ImageDraw imageDraw = new ImageDraw(context);
        this.mImageDraw = imageDraw;
        this.mImageShow = imageDraw;
        this.mView = imageDraw;
        super.createEditor(context, frameLayout);
        this.mImageDraw.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null && (getLocalRepresentation() instanceof FilterDrawRepresentation)) {
            FilterDrawRepresentation drawRep = (FilterDrawRepresentation) getLocalRepresentation();
            this.mImageDraw.setFilterDrawRepresentation(drawRep);
            if (!ParametricEditor.useCompact(this.mContext)) {
                if (this.mTabletUI != null) {
                    this.mTabletUI.setDrawRepresentation(drawRep);
                }
            } else {
                drawRep.getParam(1).setFilterView(this);
                drawRep.setPramMode(2);
                this.mParameterString = this.mContext.getString(R.string.draw_color);
                control(drawRep.getCurrentParam(), this.mEditControl);
            }
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        Button view = (Button) accessoryViewList.findViewById(R.id.applyEffect);
        view.setText(this.mContext.getString(R.string.draw_color));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                EditorDraw.this.showPopupMenu(accessoryViewList);
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
            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_draw, popupMenu.getMenu());
            if (!ParametricEditor.useCompact(this.mContext)) {
                Menu menu = popupMenu.getMenu();
                int count = menu.size();
                for (int i = 0; i < count; i++) {
                    MenuItem item = menu.getItem(i);
                    if (item.getItemId() != R.id.draw_menu_clear) {
                        item.setVisible(false);
                    }
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item2) {
                        EditorDraw.this.clearDrawing();
                        return true;
                    }
                });
            } else {
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item2) {
                        EditorDraw.this.selectMenuItem(item2);
                        return true;
                    }
                });
            }
            popupMenu.show();
            ((FilterShowActivity) this.mContext).onShowMenu(popupMenu);
        }
    }

    protected void selectMenuItem(MenuItem item) {
        FilterDrawRepresentation rep = getDrawRep();
        if (rep != null) {
            switch (item.getItemId()) {
                case R.id.draw_menu_style:
                    rep.setPramMode(1);
                    break;
                case R.id.draw_menu_size:
                    rep.setPramMode(0);
                    break;
                case R.id.draw_menu_color:
                    rep.setPramMode(2);
                    break;
                case R.id.draw_menu_clear:
                    clearDrawing();
                    break;
            }
            if (item.getItemId() != R.id.draw_menu_clear) {
                this.mParameterString = item.getTitle().toString();
                updateText();
            }
            if (this.mControl instanceof ColorChooser) {
                ColorChooser c = (ColorChooser) this.mControl;
                this.mBasColors = c.getColorSet();
            }
            control(rep.getCurrentParam(), this.mEditControl);
            if (this.mControl instanceof ColorChooser) {
                ColorChooser c2 = (ColorChooser) this.mControl;
                c2.setColorSet(this.mBasColors);
            }
            this.mControl.updateUI();
            this.mView.invalidate();
        }
    }

    public void clearDrawing() {
        ImageDraw idraw = (ImageDraw) this.mImageShow;
        idraw.resetParameter();
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
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
        LinearLayout lp = (LinearLayout) inflater.inflate(R.layout.filtershow_draw_ui, (ViewGroup) editControl, true);
        this.mTabletUI = new EditorDrawTabletUI(this, this.mContext, lp);
        this.mDrawString = this.mContext.getResources().getString(R.string.imageDraw).toUpperCase();
        setMenuIcon(true);
    }

    FilterDrawRepresentation getDrawRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep instanceof FilterDrawRepresentation) {
            return (FilterDrawRepresentation) rep;
        }
        return null;
    }

    @Override
    public void computeIcon(int index, BitmapCaller caller) {
        Bitmap bitmap = BitmapFactory.decodeResource(this.mContext.getResources(), this.brushIcons[index]);
        caller.available(bitmap);
    }
}
