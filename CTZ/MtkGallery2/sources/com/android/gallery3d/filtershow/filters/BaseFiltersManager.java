package com.android.gallery3d.filtershow.filters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public abstract class BaseFiltersManager implements FiltersManagerInterface {
    private static int mImageBorderSize = 4;
    private static int sBorderSampleSize = 2;
    protected HashMap<Class, ImageFilter> mFilters = null;
    protected HashMap<String, FilterRepresentation> mRepresentationLookup = null;
    protected ArrayList<FilterRepresentation> mLooks = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mBorders = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mTools = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mEffects = new ArrayList<>();

    protected void init() {
        this.mFilters = new HashMap<>();
        this.mRepresentationLookup = new HashMap<>();
        Vector<Class> vector = new Vector<>();
        addFilterClasses(vector);
        for (Class cls : vector) {
            try {
                ?? NewInstance = cls.newInstance();
                if (NewInstance instanceof ImageFilter) {
                    this.mFilters.put(cls, (ImageFilter) NewInstance);
                    FilterRepresentation defaultRepresentation = NewInstance.getDefaultRepresentation();
                    if (defaultRepresentation != null) {
                        addRepresentation(defaultRepresentation);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void addRepresentation(FilterRepresentation filterRepresentation) {
        this.mRepresentationLookup.put(filterRepresentation.getSerializationName(), filterRepresentation);
    }

    public FilterRepresentation createFilterFromName(String str) {
        try {
            return this.mRepresentationLookup.get(str).copy();
        } catch (Exception e) {
            Log.v("BaseFiltersManager", "unable to generate a filter representation for \"" + str + "\"");
            e.printStackTrace();
            return null;
        }
    }

    public ImageFilter getFilter(Class cls) {
        return this.mFilters.get(cls);
    }

    @Override
    public ImageFilter getFilterForRepresentation(FilterRepresentation filterRepresentation) {
        return this.mFilters.get(filterRepresentation.getFilterClass());
    }

    public FilterRepresentation getRepresentation(Class cls) {
        ImageFilter imageFilter = this.mFilters.get(cls);
        if (imageFilter != null) {
            return imageFilter.getDefaultRepresentation();
        }
        return null;
    }

    public void freeFilterResources(ImagePreset imagePreset) {
        if (imagePreset == null) {
            return;
        }
        Vector<ImageFilter> usedFilters = imagePreset.getUsedFilters(this);
        Iterator<Class> it = this.mFilters.keySet().iterator();
        while (it.hasNext()) {
            ImageFilter imageFilter = this.mFilters.get(it.next());
            if (!usedFilters.contains(imageFilter)) {
                imageFilter.freeResources();
            }
        }
    }

    public void freeRSFilterScripts() {
        Iterator<Class> it = this.mFilters.keySet().iterator();
        while (it.hasNext()) {
            ImageFilter imageFilter = this.mFilters.get(it.next());
            if (imageFilter != 0 && (imageFilter instanceof ImageFilterRS)) {
                imageFilter.resetScripts();
            }
        }
    }

    protected void addFilterClasses(Vector<Class> vector) {
        vector.add(ImageFilterTinyPlanet.class);
        vector.add(ImageFilterRedEye.class);
        vector.add(ImageFilterWBalance.class);
        vector.add(ImageFilterExposure.class);
        vector.add(ImageFilterVignette.class);
        vector.add(ImageFilterGrad.class);
        vector.add(ImageFilterContrast.class);
        vector.add(ImageFilterShadows.class);
        vector.add(ImageFilterHighlights.class);
        vector.add(ImageFilterVibrance.class);
        vector.add(ImageFilterSharpen.class);
        vector.add(ImageFilterCurves.class);
        vector.add(ImageFilterDraw.class);
        vector.add(ImageFilterHue.class);
        vector.add(ImageFilterChanSat.class);
        vector.add(ImageFilterSaturated.class);
        vector.add(ImageFilterBwFilter.class);
        vector.add(ImageFilterNegative.class);
        vector.add(ImageFilterEdge.class);
        vector.add(ImageFilterKMeans.class);
        vector.add(ImageFilterFx.class);
        vector.add(ImageFilterBorder.class);
        vector.add(ImageFilterColorBorder.class);
    }

    public ArrayList<FilterRepresentation> getLooks() {
        return this.mLooks;
    }

    public ArrayList<FilterRepresentation> getBorders() {
        return this.mBorders;
    }

    public ArrayList<FilterRepresentation> getTools() {
        return this.mTools;
    }

    public ArrayList<FilterRepresentation> getEffects() {
        return this.mEffects;
    }

    public void addBorders(Context context) {
        String[] strArr = {"FRAME_4X5", "FRAME_BRUSH", "FRAME_GRUNGE", "FRAME_SUMI_E", "FRAME_TAPE", "FRAME_BLACK", "FRAME_BLACK_ROUNDED", "FRAME_WHITE", "FRAME_WHITE_ROUNDED", "FRAME_CREAM", "FRAME_CREAM_ROUNDED"};
        int i = 0;
        this.mBorders.add(new FilterImageBorderRepresentation(0));
        ArrayList<FilterRepresentation> arrayList = new ArrayList();
        arrayList.add(new FilterImageBorderRepresentation(R.drawable.filtershow_border_4x5));
        arrayList.add(new FilterImageBorderRepresentation(R.drawable.filtershow_border_brush));
        arrayList.add(new FilterImageBorderRepresentation(R.drawable.filtershow_border_grunge));
        arrayList.add(new FilterImageBorderRepresentation(R.drawable.filtershow_border_sumi_e));
        arrayList.add(new FilterImageBorderRepresentation(R.drawable.filtershow_border_tape));
        arrayList.add(new FilterColorBorderRepresentation(-16777216, mImageBorderSize, 0));
        arrayList.add(new FilterColorBorderRepresentation(-16777216, mImageBorderSize, mImageBorderSize));
        arrayList.add(new FilterColorBorderRepresentation(-1, mImageBorderSize, 0));
        arrayList.add(new FilterColorBorderRepresentation(-1, mImageBorderSize, mImageBorderSize));
        int iArgb = Color.argb(255, 237, 237, 227);
        arrayList.add(new FilterColorBorderRepresentation(iArgb, mImageBorderSize, 0));
        arrayList.add(new FilterColorBorderRepresentation(iArgb, mImageBorderSize, mImageBorderSize));
        for (FilterRepresentation filterRepresentation : arrayList) {
            filterRepresentation.setSerializationName(strArr[i]);
            addRepresentation(filterRepresentation);
            this.mBorders.add(filterRepresentation);
            i++;
        }
    }

    public void addLooks(Context context) {
        int[] iArr = {R.drawable.filtershow_fx_0005_punch, R.drawable.filtershow_fx_0000_vintage, R.drawable.filtershow_fx_0004_bw_contrast, R.drawable.filtershow_fx_0002_bleach, R.drawable.filtershow_fx_0001_instant, R.drawable.filtershow_fx_0007_washout, R.drawable.filtershow_fx_0003_blue_crush, R.drawable.filtershow_fx_0008_washout_color, R.drawable.filtershow_fx_0006_x_process};
        int[] iArr2 = {R.string.ffx_punch, R.string.ffx_vintage, R.string.ffx_bw_contrast, R.string.ffx_bleach, R.string.ffx_instant, R.string.ffx_washout, R.string.ffx_blue_crush, R.string.ffx_washout_color, R.string.ffx_x_process};
        String[] strArr = {"LUT3D_PUNCH", "LUT3D_VINTAGE", "LUT3D_BW", "LUT3D_BLEACH", "LUT3D_INSTANT", "LUT3D_WASHOUT", "LUT3D_BLUECRUSH", "LUT3D_WASHOUT_COLOR", "LUT3D_XPROCESS"};
        this.mLooks.add(new FilterFxRepresentation(context.getString(R.string.none), 0, R.string.none));
        for (int i = 0; i < iArr.length; i++) {
            FilterFxRepresentation filterFxRepresentation = new FilterFxRepresentation(context.getString(iArr2[i]), iArr[i], iArr2[i]);
            filterFxRepresentation.setSerializationName(strArr[i]);
            ImagePreset imagePreset = new ImagePreset();
            imagePreset.addFilter(filterFxRepresentation);
            this.mLooks.add(new FilterUserPresetRepresentation(context.getString(iArr2[i]), imagePreset, -1));
            addRepresentation(filterFxRepresentation);
        }
    }

    public void addEffects() {
        this.mEffects.add(getRepresentation(ImageFilterTinyPlanet.class));
        this.mEffects.add(getRepresentation(ImageFilterWBalance.class));
        this.mEffects.add(getRepresentation(ImageFilterExposure.class));
        if (shouldEnableRSEffect()) {
            this.mEffects.add(getRepresentation(ImageFilterVignette.class));
            this.mEffects.add(getRepresentation(ImageFilterGrad.class));
        }
        this.mEffects.add(getRepresentation(ImageFilterContrast.class));
        this.mEffects.add(getRepresentation(ImageFilterShadows.class));
        this.mEffects.add(getRepresentation(ImageFilterHighlights.class));
        this.mEffects.add(getRepresentation(ImageFilterVibrance.class));
        if (shouldEnableRSEffect()) {
            this.mEffects.add(getRepresentation(ImageFilterSharpen.class));
        }
        this.mEffects.add(getRepresentation(ImageFilterCurves.class));
        this.mEffects.add(getRepresentation(ImageFilterHue.class));
        if (shouldEnableRSEffect()) {
            this.mEffects.add(getRepresentation(ImageFilterChanSat.class));
        }
        this.mEffects.add(getRepresentation(ImageFilterBwFilter.class));
        this.mEffects.add(getRepresentation(ImageFilterNegative.class));
        this.mEffects.add(getRepresentation(ImageFilterEdge.class));
        this.mEffects.add(getRepresentation(ImageFilterKMeans.class));
    }

    public void addTools(Context context) {
        int[] iArr = {R.string.crop, R.string.straighten, R.string.rotate, R.string.mirror, R.string.imageDraw};
        int[] iArr2 = {R.drawable.filtershow_button_geometry_crop, R.drawable.filtershow_button_geometry_straighten, R.drawable.filtershow_button_geometry_rotate, R.drawable.filtershow_button_geometry_flip, R.drawable.filtershow_button_colors};
        FilterRepresentation[] filterRepresentationArr = {new FilterCropRepresentation(), new FilterStraightenRepresentation(), new FilterRotateRepresentation(), new FilterMirrorRepresentation(), new FilterDrawRepresentation()};
        for (int i = 0; i < iArr.length; i++) {
            FilterRepresentation filterRepresentation = filterRepresentationArr[i];
            filterRepresentation.setTextId(iArr[i]);
            filterRepresentation.setOverlayId(iArr2[i]);
            filterRepresentation.setOverlayOnly(true);
            if (filterRepresentation.getTextId() != 0) {
                filterRepresentation.setName(context.getString(filterRepresentation.getTextId()));
            }
            this.mTools.add(filterRepresentation);
        }
    }

    public void setFilterResources(Resources resources) {
        ImageFilterBorder imageFilterBorder = (ImageFilterBorder) getFilter(ImageFilterBorder.class);
        ImageFilterBorder.setBorderSampleSize(sBorderSampleSize);
        imageFilterBorder.setResources(resources);
        ((ImageFilterFx) getFilter(ImageFilterFx.class)).setResources(resources);
    }

    public static void setBorderSampleSize(int i) {
        sBorderSampleSize = i;
    }

    private boolean shouldEnableRSEffect() {
        return Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 24;
    }
}
