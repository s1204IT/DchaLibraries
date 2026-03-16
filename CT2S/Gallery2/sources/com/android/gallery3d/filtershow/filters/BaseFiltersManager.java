package com.android.gallery3d.filtershow.filters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public abstract class BaseFiltersManager implements FiltersManagerInterface {
    private static int mImageBorderSize = 4;
    protected HashMap<Class, ImageFilter> mFilters = null;
    protected HashMap<String, FilterRepresentation> mRepresentationLookup = null;
    protected ArrayList<FilterRepresentation> mLooks = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mBorders = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mTools = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mEffects = new ArrayList<>();

    protected void init() {
        this.mFilters = new HashMap<>();
        this.mRepresentationLookup = new HashMap<>();
        Vector<Class> filters = new Vector<>();
        addFilterClasses(filters);
        for (Class filterClass : filters) {
            try {
                Object filterInstance = filterClass.newInstance();
                if (filterInstance instanceof ImageFilter) {
                    this.mFilters.put(filterClass, (ImageFilter) filterInstance);
                    FilterRepresentation rep = ((ImageFilter) filterInstance).getDefaultRepresentation();
                    if (rep != null) {
                        addRepresentation(rep);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void addRepresentation(FilterRepresentation rep) {
        this.mRepresentationLookup.put(rep.getSerializationName(), rep);
    }

    public FilterRepresentation createFilterFromName(String name) {
        try {
            return this.mRepresentationLookup.get(name).copy();
        } catch (Exception e) {
            Log.v("BaseFiltersManager", "unable to generate a filter representation for \"" + name + "\"");
            e.printStackTrace();
            return null;
        }
    }

    public ImageFilter getFilter(Class c) {
        return this.mFilters.get(c);
    }

    @Override
    public ImageFilter getFilterForRepresentation(FilterRepresentation representation) {
        return this.mFilters.get(representation.getFilterClass());
    }

    public FilterRepresentation getRepresentation(Class c) {
        ImageFilter filter = this.mFilters.get(c);
        if (filter != null) {
            return filter.getDefaultRepresentation();
        }
        return null;
    }

    public void freeFilterResources(ImagePreset preset) {
        if (preset != null) {
            Vector<ImageFilter> usedFilters = preset.getUsedFilters(this);
            for (Class c : this.mFilters.keySet()) {
                ImageFilter filter = this.mFilters.get(c);
                if (!usedFilters.contains(filter)) {
                    filter.freeResources();
                }
            }
        }
    }

    public void freeRSFilterScripts() {
        for (Class c : this.mFilters.keySet()) {
            ImageFilter filter = this.mFilters.get(c);
            if (filter != null && (filter instanceof ImageFilterRS)) {
                ((ImageFilterRS) filter).resetScripts();
            }
        }
    }

    protected void addFilterClasses(Vector<Class> filters) {
        filters.add(ImageFilterTinyPlanet.class);
        filters.add(ImageFilterRedEye.class);
        filters.add(ImageFilterWBalance.class);
        filters.add(ImageFilterExposure.class);
        filters.add(ImageFilterVignette.class);
        filters.add(ImageFilterGrad.class);
        filters.add(ImageFilterContrast.class);
        filters.add(ImageFilterShadows.class);
        filters.add(ImageFilterHighlights.class);
        filters.add(ImageFilterVibrance.class);
        filters.add(ImageFilterSharpen.class);
        filters.add(ImageFilterCurves.class);
        filters.add(ImageFilterDraw.class);
        filters.add(ImageFilterHue.class);
        filters.add(ImageFilterChanSat.class);
        filters.add(ImageFilterSaturated.class);
        filters.add(ImageFilterBwFilter.class);
        filters.add(ImageFilterNegative.class);
        filters.add(ImageFilterEdge.class);
        filters.add(ImageFilterKMeans.class);
        filters.add(ImageFilterFx.class);
        filters.add(ImageFilterBorder.class);
        filters.add(ImageFilterColorBorder.class);
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
        String[] serializationNames = {"FRAME_4X5", "FRAME_BRUSH", "FRAME_GRUNGE", "FRAME_SUMI_E", "FRAME_TAPE", "FRAME_BLACK", "FRAME_BLACK_ROUNDED", "FRAME_WHITE", "FRAME_WHITE_ROUNDED", "FRAME_CREAM", "FRAME_CREAM_ROUNDED"};
        int i = 0;
        FilterRepresentation rep = new FilterImageBorderRepresentation(R.string.none, 0);
        this.mBorders.add(rep);
        ArrayList<FilterRepresentation> borderList = new ArrayList<>();
        FilterRepresentation rep2 = new FilterImageBorderRepresentation(R.string.image_border_4x5, R.drawable.filtershow_border_4x5);
        borderList.add(rep2);
        FilterRepresentation rep3 = new FilterImageBorderRepresentation(R.string.image_border_brush, R.drawable.filtershow_border_brush);
        borderList.add(rep3);
        FilterRepresentation rep4 = new FilterImageBorderRepresentation(R.string.image_border_grunge, R.drawable.filtershow_border_grunge);
        borderList.add(rep4);
        FilterRepresentation rep5 = new FilterImageBorderRepresentation(R.string.image_border_sumi_e, R.drawable.filtershow_border_sumi_e);
        borderList.add(rep5);
        FilterRepresentation rep6 = new FilterImageBorderRepresentation(R.string.image_border_tape, R.drawable.filtershow_border_tape);
        borderList.add(rep6);
        FilterRepresentation rep7 = new FilterColorBorderRepresentation(R.string.color_border_black, -16777216, mImageBorderSize, 0);
        borderList.add(rep7);
        FilterRepresentation rep8 = new FilterColorBorderRepresentation(R.string.color_border_black_arc, -16777216, mImageBorderSize, mImageBorderSize);
        borderList.add(rep8);
        FilterRepresentation rep9 = new FilterColorBorderRepresentation(R.string.color_border_white, -1, mImageBorderSize, 0);
        borderList.add(rep9);
        FilterRepresentation rep10 = new FilterColorBorderRepresentation(R.string.color_border_white_arc, -1, mImageBorderSize, mImageBorderSize);
        borderList.add(rep10);
        int creamColor = Color.argb(255, 237, 237, 227);
        FilterRepresentation rep11 = new FilterColorBorderRepresentation(R.string.color_border_cream, creamColor, mImageBorderSize, 0);
        borderList.add(rep11);
        FilterRepresentation rep12 = new FilterColorBorderRepresentation(R.string.color_border_cream_arc, creamColor, mImageBorderSize, mImageBorderSize);
        borderList.add(rep12);
        for (FilterRepresentation filter : borderList) {
            filter.setSerializationName(serializationNames[i]);
            addRepresentation(filter);
            this.mBorders.add(filter);
            i++;
        }
    }

    public void addLooks(Context context) {
        int[] drawid = {R.drawable.filtershow_fx_0005_punch, R.drawable.filtershow_fx_0000_vintage, R.drawable.filtershow_fx_0004_bw_contrast, R.drawable.filtershow_fx_0002_bleach, R.drawable.filtershow_fx_0001_instant, R.drawable.filtershow_fx_0007_washout, R.drawable.filtershow_fx_0003_blue_crush, R.drawable.filtershow_fx_0008_washout_color, R.drawable.filtershow_fx_0006_x_process};
        int[] fxNameid = {R.string.ffx_punch, R.string.ffx_vintage, R.string.ffx_bw_contrast, R.string.ffx_bleach, R.string.ffx_instant, R.string.ffx_washout, R.string.ffx_blue_crush, R.string.ffx_washout_color, R.string.ffx_x_process};
        String[] serializationNames = {"LUT3D_PUNCH", "LUT3D_VINTAGE", "LUT3D_BW", "LUT3D_BLEACH", "LUT3D_INSTANT", "LUT3D_WASHOUT", "LUT3D_BLUECRUSH", "LUT3D_WASHOUT_COLOR", "LUT3D_XPROCESS"};
        FilterFxRepresentation nullFx = new FilterFxRepresentation(context.getString(R.string.none), 0, R.string.none);
        this.mLooks.add(nullFx);
        for (int i = 0; i < drawid.length; i++) {
            FilterFxRepresentation fx = new FilterFxRepresentation(context.getString(fxNameid[i]), drawid[i], fxNameid[i]);
            fx.setSerializationName(serializationNames[i]);
            ImagePreset preset = new ImagePreset();
            preset.addFilter(fx);
            FilterUserPresetRepresentation rep = new FilterUserPresetRepresentation(context.getString(fxNameid[i]), preset, -1);
            this.mLooks.add(rep);
            addRepresentation(fx);
        }
    }

    public void addEffects() {
        this.mEffects.add(getRepresentation(ImageFilterTinyPlanet.class));
        this.mEffects.add(getRepresentation(ImageFilterWBalance.class));
        this.mEffects.add(getRepresentation(ImageFilterExposure.class));
        this.mEffects.add(getRepresentation(ImageFilterVignette.class));
        this.mEffects.add(getRepresentation(ImageFilterGrad.class));
        this.mEffects.add(getRepresentation(ImageFilterContrast.class));
        this.mEffects.add(getRepresentation(ImageFilterShadows.class));
        this.mEffects.add(getRepresentation(ImageFilterHighlights.class));
        this.mEffects.add(getRepresentation(ImageFilterVibrance.class));
        this.mEffects.add(getRepresentation(ImageFilterSharpen.class));
        this.mEffects.add(getRepresentation(ImageFilterCurves.class));
        this.mEffects.add(getRepresentation(ImageFilterHue.class));
        this.mEffects.add(getRepresentation(ImageFilterChanSat.class));
        this.mEffects.add(getRepresentation(ImageFilterBwFilter.class));
        this.mEffects.add(getRepresentation(ImageFilterNegative.class));
        this.mEffects.add(getRepresentation(ImageFilterEdge.class));
        this.mEffects.add(getRepresentation(ImageFilterKMeans.class));
    }

    public void addTools(Context context) {
        int[] textId = {R.string.crop, R.string.straighten, R.string.rotate, R.string.mirror};
        int[] overlayId = {R.drawable.filtershow_button_geometry_crop, R.drawable.filtershow_button_geometry_straighten, R.drawable.filtershow_button_geometry_rotate, R.drawable.filtershow_button_geometry_flip};
        FilterRepresentation[] geometryFilters = {new FilterCropRepresentation(), new FilterStraightenRepresentation(), new FilterRotateRepresentation(), new FilterMirrorRepresentation()};
        for (int i = 0; i < textId.length; i++) {
            FilterRepresentation geometry = geometryFilters[i];
            geometry.setTextId(textId[i]);
            geometry.setOverlayId(overlayId[i]);
            geometry.setOverlayOnly(true);
            if (geometry.getTextId() != 0) {
                geometry.setName(context.getString(geometry.getTextId()));
            }
            this.mTools.add(geometry);
        }
        this.mTools.add(getRepresentation(ImageFilterDraw.class));
    }

    public void setFilterResources(Resources resources) {
        ImageFilterBorder filterBorder = (ImageFilterBorder) getFilter(ImageFilterBorder.class);
        filterBorder.setResources(resources);
        ImageFilterFx filterFx = (ImageFilterFx) getFilter(ImageFilterFx.class);
        filterFx.setResources(resources);
    }
}
