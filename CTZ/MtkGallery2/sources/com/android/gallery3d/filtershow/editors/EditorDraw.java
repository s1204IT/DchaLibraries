package com.android.gallery3d.filtershow.editors;

import android.content.Context;
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
    public String calculateUserMessage(Context context, String str, Object obj) {
        FilterDrawRepresentation drawRep = getDrawRep();
        if (this.mDrawString != null) {
            this.mImageDraw.displayDrawLook();
            return this.mDrawString;
        }
        if (drawRep == null) {
            return "";
        }
        ParametricEditor.useCompact(this.mContext);
        if (this.mParameterString == null) {
            this.mParameterString = "";
        }
        String valueString = drawRep.getValueString();
        this.mImageDraw.displayDrawLook();
        return this.mParameterString + valueString;
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
        if (getLocalRepresentation() != null && (getLocalRepresentation() instanceof FilterDrawRepresentation)) {
            FilterDrawRepresentation filterDrawRepresentation = (FilterDrawRepresentation) getLocalRepresentation();
            this.mImageDraw.setFilterDrawRepresentation(filterDrawRepresentation);
            if (!ParametricEditor.useCompact(this.mContext)) {
                if (this.mTabletUI != null) {
                    this.mTabletUI.setDrawRepresentation(filterDrawRepresentation);
                }
            } else {
                filterDrawRepresentation.getParam(1).setFilterView(this);
                filterDrawRepresentation.setPramMode(2);
                this.mParameterString = this.mContext.getString(R.string.draw_color);
                control(filterDrawRepresentation.getCurrentParam(), this.mEditControl);
            }
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout linearLayout) {
        Button button = (Button) linearLayout.findViewById(R.id.applyEffect);
        button.setText(this.mContext.getString(R.string.draw_color));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditorDraw.this.showPopupMenu(linearLayout);
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
        popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_draw, popupMenu.getMenu());
        if (!ParametricEditor.useCompact(this.mContext)) {
            Menu menu = popupMenu.getMenu();
            int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() != R.id.draw_menu_clear) {
                    item.setVisible(false);
                }
            }
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    EditorDraw.this.clearDrawing();
                    return true;
                }
            });
        } else {
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    EditorDraw.this.selectMenuItem(menuItem);
                    return true;
                }
            });
        }
        popupMenu.show();
        ((FilterShowActivity) this.mContext).onShowMenu(popupMenu);
    }

    protected void selectMenuItem(MenuItem menuItem) {
        FilterDrawRepresentation drawRep = getDrawRep();
        if (drawRep == null) {
            return;
        }
        switch (menuItem.getItemId()) {
            case R.id.draw_menu_style:
                drawRep.setPramMode(1);
                break;
            case R.id.draw_menu_size:
                drawRep.setPramMode(0);
                break;
            case R.id.draw_menu_color:
                drawRep.setPramMode(2);
                break;
            case R.id.draw_menu_clear:
                clearDrawing();
                break;
        }
        if (menuItem.getItemId() != R.id.draw_menu_clear) {
            this.mParameterString = menuItem.getTitle().toString();
            updateText();
        }
        if (this.mControl instanceof ColorChooser) {
            this.mBasColors = ((ColorChooser) this.mControl).getColorSet();
        }
        control(drawRep.getCurrentParam(), this.mEditControl);
        if (this.mControl instanceof ColorChooser) {
            ((ColorChooser) this.mControl).setColorSet(this.mBasColors);
        }
        this.mControl.updateUI();
        this.mView.invalidate();
    }

    public void clearDrawing() {
        ((ImageDraw) this.mImageShow).resetParameter();
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
        this.mTabletUI = new EditorDrawTabletUI(this, this.mContext, (LinearLayout) ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.filtershow_draw_ui, (ViewGroup) view2, true));
        this.mDrawString = this.mContext.getResources().getString(R.string.imageDraw).toUpperCase();
        setMenuIcon(true);
    }

    FilterDrawRepresentation getDrawRep() {
        ?? localRepresentation = getLocalRepresentation();
        if (localRepresentation instanceof FilterDrawRepresentation) {
            return localRepresentation;
        }
        return null;
    }

    @Override
    public void computeIcon(int i, BitmapCaller bitmapCaller) {
        bitmapCaller.available(BitmapFactory.decodeResource(this.mContext.getResources(), this.brushIcons[i]));
    }
}
