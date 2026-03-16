package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.BaseFiltersManager;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterFxRepresentation;
import com.android.gallery3d.filtershow.filters.FilterImageBorderRepresentation;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRotateRepresentation;
import com.android.gallery3d.filtershow.filters.FilterStraightenRepresentation;
import com.android.gallery3d.filtershow.filters.FilterUserPresetRepresentation;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.State;
import com.android.gallery3d.filtershow.state.StateAdapter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

public class ImagePreset {
    private Rect mPartialRenderingBounds;
    private Vector<FilterRepresentation> mFilters = new Vector<>();
    private boolean mDoApplyGeometry = true;
    private boolean mDoApplyFilters = true;
    private boolean mPartialRendering = false;

    public ImagePreset() {
    }

    public ImagePreset(ImagePreset source) {
        if (source != null) {
            for (int i = 0; i < source.mFilters.size(); i++) {
                FilterRepresentation sourceRepresentation = source.mFilters.elementAt(i);
                this.mFilters.add(sourceRepresentation.copy());
            }
        }
    }

    public Vector<FilterRepresentation> getFilters() {
        return this.mFilters;
    }

    public FilterRepresentation getFilterRepresentation(int position) {
        FilterRepresentation representation = this.mFilters.elementAt(position).copy();
        return representation;
    }

    private static boolean sameSerializationName(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.equals(b);
    }

    public static boolean sameSerializationName(FilterRepresentation a, FilterRepresentation b) {
        if (a == null || b == null) {
            return false;
        }
        return sameSerializationName(a.getSerializationName(), b.getSerializationName());
    }

    public int getPositionForRepresentation(FilterRepresentation representation) {
        for (int i = 0; i < this.mFilters.size(); i++) {
            if (sameSerializationName(this.mFilters.elementAt(i), representation)) {
                return i;
            }
        }
        return -1;
    }

    private FilterRepresentation getFilterRepresentationForType(int type) {
        for (int i = 0; i < this.mFilters.size(); i++) {
            if (this.mFilters.elementAt(i).getFilterType() == type) {
                return this.mFilters.elementAt(i);
            }
        }
        return null;
    }

    public int getPositionForType(int type) {
        for (int i = 0; i < this.mFilters.size(); i++) {
            if (this.mFilters.elementAt(i).getFilterType() == type) {
                return i;
            }
        }
        return -1;
    }

    public FilterRepresentation getFilterRepresentationCopyFrom(FilterRepresentation filterRepresentation) {
        int position;
        if (filterRepresentation == null || (position = getPositionForRepresentation(filterRepresentation)) == -1) {
            return null;
        }
        FilterRepresentation representation = this.mFilters.elementAt(position);
        if (representation != null) {
            return representation.copy();
        }
        return representation;
    }

    public void updateFilterRepresentations(Collection<FilterRepresentation> reps) {
        for (FilterRepresentation r : reps) {
            updateOrAddFilterRepresentation(r);
        }
    }

    public void updateOrAddFilterRepresentation(FilterRepresentation rep) {
        int pos = getPositionForRepresentation(rep);
        if (pos != -1) {
            this.mFilters.elementAt(pos).useParametersFrom(rep);
        } else {
            addFilter(rep.copy());
        }
    }

    public void setDoApplyGeometry(boolean value) {
        this.mDoApplyGeometry = value;
    }

    public void setDoApplyFilters(boolean value) {
        this.mDoApplyFilters = value;
    }

    public boolean hasModifications() {
        for (int i = 0; i < this.mFilters.size(); i++) {
            FilterRepresentation filter = this.mFilters.elementAt(i);
            if (!filter.isNil()) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(byte type) {
        for (FilterRepresentation representation : this.mFilters) {
            if (representation.getFilterType() == type && !representation.isNil()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPanoramaSafe() {
        for (FilterRepresentation representation : this.mFilters) {
            if (representation.getFilterType() == 7 && !representation.isNil()) {
                return false;
            }
            if (representation.getFilterType() == 1 && !representation.isNil()) {
                return false;
            }
            if (representation.getFilterType() == 4 && !representation.isNil()) {
                return false;
            }
            if (representation.getFilterType() == 6 && !representation.isNil()) {
                return false;
            }
        }
        return true;
    }

    public boolean same(ImagePreset preset) {
        if (preset == null || preset.mFilters.size() != this.mFilters.size() || this.mDoApplyGeometry != preset.mDoApplyGeometry) {
            return false;
        }
        if (this.mDoApplyFilters != preset.mDoApplyFilters && (this.mFilters.size() > 0 || preset.mFilters.size() > 0)) {
            return false;
        }
        if (this.mDoApplyFilters && preset.mDoApplyFilters) {
            for (int i = 0; i < preset.mFilters.size(); i++) {
                FilterRepresentation a = preset.mFilters.elementAt(i);
                FilterRepresentation b = this.mFilters.elementAt(i);
                if (!a.same(b)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean equals(ImagePreset preset) {
        if (preset == null || preset.mFilters.size() != this.mFilters.size() || this.mDoApplyGeometry != preset.mDoApplyGeometry) {
            return false;
        }
        if (this.mDoApplyFilters != preset.mDoApplyFilters && (this.mFilters.size() > 0 || preset.mFilters.size() > 0)) {
            return false;
        }
        for (int i = 0; i < preset.mFilters.size(); i++) {
            FilterRepresentation a = preset.mFilters.elementAt(i);
            FilterRepresentation b = this.mFilters.elementAt(i);
            boolean isGeometry = false;
            if ((a instanceof FilterRotateRepresentation) || (a instanceof FilterMirrorRepresentation) || (a instanceof FilterCropRepresentation) || (a instanceof FilterStraightenRepresentation)) {
                isGeometry = true;
            }
            boolean evaluate = true;
            if (!isGeometry && this.mDoApplyGeometry && !this.mDoApplyFilters) {
                evaluate = false;
            } else if (isGeometry && !this.mDoApplyGeometry && this.mDoApplyFilters) {
                evaluate = false;
            }
            if (evaluate && !a.equals(b)) {
                return false;
            }
        }
        return true;
    }

    public void showFilters() {
        Log.v("ImagePreset", "\\\\\\ showFilters -- " + this.mFilters.size() + " filters");
        int n = 0;
        for (FilterRepresentation representation : this.mFilters) {
            Log.v("ImagePreset", " filter " + n + " : " + representation.toString());
            n++;
        }
        Log.v("ImagePreset", "/// showFilters -- " + this.mFilters.size() + " filters");
    }

    public FilterRepresentation getLastRepresentation() {
        if (this.mFilters.size() > 0) {
            return this.mFilters.lastElement();
        }
        return null;
    }

    public void removeFilter(FilterRepresentation filterRepresentation) {
        if (filterRepresentation.getFilterType() == 1) {
            for (int i = 0; i < this.mFilters.size(); i++) {
                if (this.mFilters.elementAt(i).getFilterType() == filterRepresentation.getFilterType()) {
                    this.mFilters.remove(i);
                    return;
                }
            }
            return;
        }
        for (int i2 = 0; i2 < this.mFilters.size(); i2++) {
            if (sameSerializationName(this.mFilters.elementAt(i2), filterRepresentation)) {
                this.mFilters.remove(i2);
                return;
            }
        }
    }

    public void addFilter(FilterRepresentation representation) {
        if (representation instanceof FilterUserPresetRepresentation) {
            ImagePreset preset = ((FilterUserPresetRepresentation) representation).getImagePreset();
            if (preset.nbFilters() == 1 && preset.contains((byte) 2)) {
                addFilter(preset.getFilterRepresentationForType(2));
            } else {
                this.mFilters.clear();
                for (int i = 0; i < preset.nbFilters(); i++) {
                    addFilter(preset.getFilterRepresentation(i));
                }
            }
        } else if (representation.getFilterType() == 7) {
            for (int i2 = 0; i2 < this.mFilters.size(); i2++) {
                if (sameSerializationName(representation, this.mFilters.elementAt(i2))) {
                    this.mFilters.remove(i2);
                }
            }
            int index = 0;
            while (index < this.mFilters.size() && this.mFilters.elementAt(index).getFilterType() == 7) {
                index++;
            }
            if (!representation.isNil()) {
                this.mFilters.insertElementAt(representation, index);
            }
        } else if (representation.getFilterType() == 1) {
            removeFilter(representation);
            if (!isNoneBorderFilter(representation)) {
                this.mFilters.add(representation);
            }
        } else if (representation.getFilterType() == 2) {
            boolean replaced = false;
            int i3 = 0;
            while (true) {
                if (i3 >= this.mFilters.size()) {
                    break;
                }
                FilterRepresentation current = this.mFilters.elementAt(i3);
                if (current.getFilterType() != 2) {
                    i3++;
                } else {
                    this.mFilters.remove(i3);
                    replaced = true;
                    if (!isNoneFxFilter(representation)) {
                        this.mFilters.add(i3, representation);
                    }
                }
            }
            if (!replaced && !isNoneFxFilter(representation)) {
                this.mFilters.add(0, representation);
            }
        } else {
            this.mFilters.add(representation);
        }
        FilterRepresentation border = null;
        int i4 = 0;
        while (i4 < this.mFilters.size()) {
            FilterRepresentation rep = this.mFilters.elementAt(i4);
            if (rep.getFilterType() == 1) {
                border = rep;
                this.mFilters.remove(i4);
            } else {
                i4++;
            }
        }
        if (border != null) {
            this.mFilters.add(border);
        }
    }

    private boolean isNoneBorderFilter(FilterRepresentation representation) {
        return (representation instanceof FilterImageBorderRepresentation) && ((FilterImageBorderRepresentation) representation).getDrawableResource() == 0;
    }

    private boolean isNoneFxFilter(FilterRepresentation representation) {
        return (representation instanceof FilterFxRepresentation) && ((FilterFxRepresentation) representation).getNameResource() == R.string.none;
    }

    public FilterRepresentation getRepresentation(FilterRepresentation filterRepresentation) {
        for (int i = 0; i < this.mFilters.size(); i++) {
            FilterRepresentation representation = this.mFilters.elementAt(i);
            if (sameSerializationName(representation, filterRepresentation)) {
                return representation;
            }
        }
        return null;
    }

    public Bitmap apply(Bitmap original, FilterEnvironment environment) {
        Bitmap bitmap = applyFilters(original, -1, -1, environment);
        return applyBorder(bitmap, environment);
    }

    public Collection<FilterRepresentation> getGeometryFilters() {
        ArrayList<FilterRepresentation> geometry = new ArrayList<>();
        for (FilterRepresentation r : this.mFilters) {
            if (r.getFilterType() == 7) {
                geometry.add(r);
            }
        }
        return geometry;
    }

    public FilterRepresentation getFilterWithSerializationName(String serializationName) {
        for (FilterRepresentation r : this.mFilters) {
            if (r != null && sameSerializationName(r.getSerializationName(), serializationName)) {
                return r.copy();
            }
        }
        return null;
    }

    public Rect finalGeometryRect(int width, int height) {
        return GeometryMathUtils.finalGeometryRect(width, height, getGeometryFilters());
    }

    public Bitmap applyGeometry(Bitmap bitmap, FilterEnvironment environment) {
        if (!this.mDoApplyGeometry) {
            return bitmap;
        }
        Bitmap bmp = GeometryMathUtils.applyGeometryRepresentations(getGeometryFilters(), bitmap);
        if (bmp != bitmap) {
            environment.cache(bitmap);
            return bmp;
        }
        return bmp;
    }

    public Bitmap applyBorder(Bitmap bitmap, FilterEnvironment environment) {
        FilterRepresentation border = getFilterRepresentationForType(1);
        if (border != null && this.mDoApplyGeometry) {
            bitmap = environment.applyRepresentation(border, bitmap);
            if (environment.getQuality() == 2) {
            }
        }
        return bitmap;
    }

    public int nbFilters() {
        return this.mFilters.size();
    }

    public Bitmap applyFilters(Bitmap bitmap, int from, int to, FilterEnvironment environment) {
        if (this.mDoApplyFilters) {
            if (from < 0) {
                from = 0;
            }
            if (to == -1) {
                to = this.mFilters.size();
            }
            for (int i = from; i < to; i++) {
                FilterRepresentation representation = this.mFilters.elementAt(i);
                if (representation.getFilterType() != 7 && representation.getFilterType() != 1) {
                    Bitmap tmp = bitmap;
                    bitmap = environment.applyRepresentation(representation, bitmap);
                    if (tmp != bitmap) {
                        environment.cache(tmp);
                    }
                    if (environment.needsStop()) {
                        return bitmap;
                    }
                }
            }
        }
        return bitmap;
    }

    public boolean canDoPartialRendering() {
        if (MasterImage.getImage().getZoomOrientation() != 1) {
            return false;
        }
        for (int i = 0; i < this.mFilters.size(); i++) {
            FilterRepresentation representation = this.mFilters.elementAt(i);
            if (!representation.supportsPartialRendering()) {
                return false;
            }
        }
        return true;
    }

    public void fillImageStateAdapter(StateAdapter imageStateAdapter) {
        if (imageStateAdapter != null) {
            Vector<State> states = new Vector<>();
            for (FilterRepresentation filter : this.mFilters) {
                if (!(filter instanceof FilterUserPresetRepresentation)) {
                    State state = new State(filter.getName());
                    state.setFilterRepresentation(filter);
                    states.add(state);
                }
            }
            imageStateAdapter.fill(states);
        }
    }

    public void setPartialRendering(boolean partialRendering, Rect bounds) {
        this.mPartialRendering = partialRendering;
        this.mPartialRenderingBounds = bounds;
    }

    public Vector<ImageFilter> getUsedFilters(BaseFiltersManager filtersManager) {
        Vector<ImageFilter> usedFilters = new Vector<>();
        for (int i = 0; i < this.mFilters.size(); i++) {
            FilterRepresentation representation = this.mFilters.elementAt(i);
            ImageFilter filter = filtersManager.getFilterForRepresentation(representation);
            usedFilters.add(filter);
        }
        return usedFilters;
    }

    public String getJsonString(String name) {
        StringWriter swriter = new StringWriter();
        try {
            JsonWriter writer = new JsonWriter(swriter);
            writeJson(writer, name);
            writer.close();
            return swriter.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public void writeJson(JsonWriter writer, String name) {
        int numFilters = this.mFilters.size();
        try {
            writer.beginObject();
            for (int i = 0; i < numFilters; i++) {
                FilterRepresentation filter = this.mFilters.get(i);
                if (!(filter instanceof FilterUserPresetRepresentation)) {
                    String sname = filter.getSerializationName();
                    writer.name(sname);
                    filter.serializeRepresentation(writer);
                }
            }
            writer.endObject();
        } catch (IOException e) {
            Log.e("ImagePreset", "Error encoding JASON", e);
        }
    }

    public boolean readJsonFromString(String filterString) {
        boolean z = false;
        StringReader sreader = new StringReader(filterString);
        try {
            JsonReader reader = new JsonReader(sreader);
            boolean ok = readJson(reader);
            if (!ok) {
                reader.close();
            } else {
                reader.close();
                z = true;
            }
        } catch (Exception e) {
            Log.e("ImagePreset", "\"" + filterString + "\"");
            Log.e("ImagePreset", "parsing the filter parameters:", e);
        }
        return z;
    }

    public boolean readJson(JsonReader sreader) throws IOException {
        sreader.beginObject();
        while (sreader.hasNext()) {
            String name = sreader.nextName();
            FilterRepresentation filter = creatFilterFromName(name);
            if (filter == null) {
                Log.w("ImagePreset", "UNKNOWN FILTER! " + name);
                return false;
            }
            filter.deSerializeRepresentation(sreader);
            addFilter(filter);
        }
        sreader.endObject();
        return true;
    }

    FilterRepresentation creatFilterFromName(String name) {
        if ("ROTATION".equals(name)) {
            return new FilterRotateRepresentation();
        }
        if ("MIRROR".equals(name)) {
            return new FilterMirrorRepresentation();
        }
        if ("STRAIGHTEN".equals(name)) {
            return new FilterStraightenRepresentation();
        }
        if ("CROP".equals(name)) {
            return new FilterCropRepresentation();
        }
        FiltersManager filtersManager = FiltersManager.getManager();
        return filtersManager.createFilterFromName(name);
    }

    public void updateWith(ImagePreset preset) {
        if (preset.mFilters.size() != this.mFilters.size()) {
            Log.e("ImagePreset", "Updating a preset with an incompatible one");
            return;
        }
        for (int i = 0; i < this.mFilters.size(); i++) {
            FilterRepresentation destRepresentation = this.mFilters.elementAt(i);
            FilterRepresentation sourceRepresentation = preset.mFilters.elementAt(i);
            destRepresentation.useParametersFrom(sourceRepresentation);
        }
    }
}
