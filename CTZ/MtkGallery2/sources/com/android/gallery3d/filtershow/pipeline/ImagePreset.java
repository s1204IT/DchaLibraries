package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.JsonReader;
import android.util.JsonWriter;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.filters.BaseFiltersManager;
import com.android.gallery3d.filtershow.filters.FilterCropRepresentation;
import com.android.gallery3d.filtershow.filters.FilterDrawRepresentation;
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
import com.mediatek.gallery3d.util.Log;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

public class ImagePreset {
    private Rect mPartialRenderingBounds;
    private Vector<FilterRepresentation> mFilters = new Vector<>();
    private boolean mDoApplyGeometry = true;
    private boolean mDoApplyFilters = true;
    private boolean mPartialRendering = false;

    public ImagePreset() {
    }

    public ImagePreset(ImagePreset imagePreset) {
        for (int i = 0; i < imagePreset.mFilters.size(); i++) {
            this.mFilters.add(imagePreset.mFilters.elementAt(i).copy());
        }
    }

    public Vector<FilterRepresentation> getFilters() {
        return this.mFilters;
    }

    public FilterRepresentation getFilterRepresentation(int i) {
        return this.mFilters.elementAt(i).copy();
    }

    private static boolean sameSerializationName(String str, String str2) {
        if (str == null || str2 == null) {
            return str == null && str2 == null;
        }
        return str.equals(str2);
    }

    public static boolean sameSerializationName(FilterRepresentation filterRepresentation, FilterRepresentation filterRepresentation2) {
        if (filterRepresentation == null || filterRepresentation2 == null) {
            return false;
        }
        return sameSerializationName(filterRepresentation.getSerializationName(), filterRepresentation2.getSerializationName());
    }

    public int getPositionForRepresentation(FilterRepresentation filterRepresentation) {
        for (int i = 0; i < this.mFilters.size(); i++) {
            if (sameSerializationName(this.mFilters.elementAt(i), filterRepresentation)) {
                return i;
            }
        }
        return -1;
    }

    private FilterRepresentation getFilterRepresentationForType(int i) {
        for (int i2 = 0; i2 < this.mFilters.size(); i2++) {
            if (this.mFilters.elementAt(i2).getFilterType() == i) {
                return this.mFilters.elementAt(i2);
            }
        }
        return null;
    }

    public int getPositionForType(int i) {
        for (int i2 = 0; i2 < this.mFilters.size(); i2++) {
            if (this.mFilters.elementAt(i2).getFilterType() == i) {
                return i2;
            }
        }
        return -1;
    }

    public FilterRepresentation getFilterRepresentationCopyFrom(FilterRepresentation filterRepresentation) {
        int positionForRepresentation;
        if (filterRepresentation == null || (positionForRepresentation = getPositionForRepresentation(filterRepresentation)) == -1) {
            return null;
        }
        FilterRepresentation filterRepresentationElementAt = this.mFilters.elementAt(positionForRepresentation);
        if (filterRepresentationElementAt != null) {
            return filterRepresentationElementAt.copy();
        }
        return filterRepresentationElementAt;
    }

    public void updateFilterRepresentations(Collection<FilterRepresentation> collection) {
        Iterator<FilterRepresentation> it = collection.iterator();
        while (it.hasNext()) {
            updateOrAddFilterRepresentation(it.next());
        }
    }

    public void updateOrAddFilterRepresentation(FilterRepresentation filterRepresentation) {
        if (filterRepresentation == null) {
            Log.w("ImagePreset", "<updateOrAddFilterRepresentation> rep=" + filterRepresentation, new Throwable());
            return;
        }
        int positionForRepresentation = getPositionForRepresentation(filterRepresentation);
        if (positionForRepresentation != -1) {
            this.mFilters.elementAt(positionForRepresentation).useParametersFrom(filterRepresentation);
        } else {
            addFilter(filterRepresentation.copy());
        }
    }

    public void setDoApplyGeometry(boolean z) {
        this.mDoApplyGeometry = z;
    }

    public void setDoApplyFilters(boolean z) {
        this.mDoApplyFilters = z;
    }

    public boolean hasModifications() {
        for (int i = 0; i < this.mFilters.size(); i++) {
            if (!this.mFilters.elementAt(i).isNil()) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(byte b) {
        for (FilterRepresentation filterRepresentation : this.mFilters) {
            if (filterRepresentation.getFilterType() == b && !filterRepresentation.isNil()) {
                return true;
            }
        }
        return false;
    }

    public boolean isPanoramaSafe() {
        for (FilterRepresentation filterRepresentation : this.mFilters) {
            if (filterRepresentation.getFilterType() == 7 && !filterRepresentation.isNil()) {
                return false;
            }
            if (filterRepresentation.getFilterType() == 1 && !filterRepresentation.isNil()) {
                return false;
            }
            if (filterRepresentation.getFilterType() == 4 && !filterRepresentation.isNil()) {
                return false;
            }
            if (filterRepresentation.getFilterType() == 6 && !filterRepresentation.isNil()) {
                return false;
            }
        }
        return true;
    }

    public boolean same(ImagePreset imagePreset) {
        if (imagePreset == null || imagePreset.mFilters.size() != this.mFilters.size() || this.mDoApplyGeometry != imagePreset.mDoApplyGeometry) {
            return false;
        }
        if (this.mDoApplyFilters != imagePreset.mDoApplyFilters && (this.mFilters.size() > 0 || imagePreset.mFilters.size() > 0)) {
            return false;
        }
        if (this.mDoApplyFilters && imagePreset.mDoApplyFilters) {
            for (int i = 0; i < imagePreset.mFilters.size(); i++) {
                if (!imagePreset.mFilters.elementAt(i).same(this.mFilters.elementAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public boolean equals(ImagePreset imagePreset) {
        if (imagePreset == null || imagePreset.mFilters.size() != this.mFilters.size() || this.mDoApplyGeometry != imagePreset.mDoApplyGeometry) {
            return false;
        }
        if (this.mDoApplyFilters != imagePreset.mDoApplyFilters && (this.mFilters.size() > 0 || imagePreset.mFilters.size() > 0)) {
            return false;
        }
        int i = 0;
        while (true) {
            boolean z = true;
            if (i >= imagePreset.mFilters.size()) {
                return true;
            }
            FilterRepresentation filterRepresentationElementAt = imagePreset.mFilters.elementAt(i);
            FilterRepresentation filterRepresentationElementAt2 = this.mFilters.elementAt(i);
            boolean z2 = (filterRepresentationElementAt instanceof FilterRotateRepresentation) || (filterRepresentationElementAt instanceof FilterMirrorRepresentation) || (filterRepresentationElementAt instanceof FilterCropRepresentation) || (filterRepresentationElementAt instanceof FilterStraightenRepresentation);
            if ((!z2 && this.mDoApplyGeometry && !this.mDoApplyFilters) || (z2 && !this.mDoApplyGeometry && this.mDoApplyFilters)) {
                z = false;
            }
            if (z && !filterRepresentationElementAt.equals(filterRepresentationElementAt2)) {
                return false;
            }
            i++;
        }
    }

    public void showFilters() {
        Log.v("ImagePreset", "\\\\\\ showFilters -- " + this.mFilters.size() + " filters");
        Iterator<FilterRepresentation> it = this.mFilters.iterator();
        int i = 0;
        while (it.hasNext()) {
            Log.v("ImagePreset", " filter " + i + " : " + it.next().toString());
            i++;
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
        int i = 0;
        if (filterRepresentation.getFilterType() == 1) {
            while (i < this.mFilters.size()) {
                if (this.mFilters.elementAt(i).getFilterType() != filterRepresentation.getFilterType()) {
                    i++;
                } else {
                    this.mFilters.remove(i);
                    return;
                }
            }
            return;
        }
        while (i < this.mFilters.size()) {
            if (!sameSerializationName(this.mFilters.elementAt(i), filterRepresentation)) {
                i++;
            } else {
                this.mFilters.remove(i);
                return;
            }
        }
    }

    public void addFilter(FilterRepresentation filterRepresentation) {
        boolean z;
        int i = 0;
        if (filterRepresentation instanceof FilterUserPresetRepresentation) {
            ImagePreset imagePreset = ((FilterUserPresetRepresentation) filterRepresentation).getImagePreset();
            if (imagePreset.nbFilters() == 1 && imagePreset.contains((byte) 2)) {
                addFilter(imagePreset.getFilterRepresentationForType(2));
            } else {
                this.mFilters.clear();
                for (int i2 = 0; i2 < imagePreset.nbFilters(); i2++) {
                    addFilter(imagePreset.getFilterRepresentation(i2));
                }
            }
        } else if (filterRepresentation.getFilterType() == 7) {
            for (int i3 = 0; i3 < this.mFilters.size(); i3++) {
                if (sameSerializationName(filterRepresentation, this.mFilters.elementAt(i3))) {
                    this.mFilters.remove(i3);
                }
            }
            int i4 = 0;
            while (i4 < this.mFilters.size() && this.mFilters.elementAt(i4).getFilterType() == 7) {
                i4++;
            }
            if (!filterRepresentation.isNil()) {
                this.mFilters.insertElementAt(filterRepresentation, i4);
            }
        } else if (filterRepresentation.getFilterType() == 1) {
            removeFilter(filterRepresentation);
            if (!isNoneBorderFilter(filterRepresentation)) {
                this.mFilters.add(filterRepresentation);
            }
        } else if (filterRepresentation.getFilterType() == 2) {
            int i5 = 0;
            while (true) {
                if (i5 < this.mFilters.size()) {
                    if (this.mFilters.elementAt(i5).getFilterType() != 2) {
                        i5++;
                    } else {
                        this.mFilters.remove(i5);
                        if (!isNoneFxFilter(filterRepresentation)) {
                            this.mFilters.add(i5, filterRepresentation);
                        }
                        z = true;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z && !isNoneFxFilter(filterRepresentation)) {
                this.mFilters.add(filterRepresentation);
            }
        } else {
            this.mFilters.add(filterRepresentation);
        }
        FilterRepresentation filterRepresentation2 = null;
        while (i < this.mFilters.size()) {
            FilterRepresentation filterRepresentationElementAt = this.mFilters.elementAt(i);
            if (filterRepresentationElementAt.getFilterType() == 1) {
                this.mFilters.remove(i);
                filterRepresentation2 = filterRepresentationElementAt;
            } else {
                i++;
            }
        }
        if (filterRepresentation2 != null) {
            this.mFilters.add(filterRepresentation2);
        }
    }

    private boolean isNoneBorderFilter(FilterRepresentation filterRepresentation) {
        return (filterRepresentation instanceof FilterImageBorderRepresentation) && filterRepresentation.getDrawableResource() == 0;
    }

    private boolean isNoneFxFilter(FilterRepresentation filterRepresentation) {
        return (filterRepresentation instanceof FilterFxRepresentation) && filterRepresentation.getNameResource() == R.string.none;
    }

    public FilterRepresentation getRepresentation(FilterRepresentation filterRepresentation) {
        for (int i = 0; i < this.mFilters.size(); i++) {
            FilterRepresentation filterRepresentationElementAt = this.mFilters.elementAt(i);
            if (sameSerializationName(filterRepresentationElementAt, filterRepresentation)) {
                return filterRepresentationElementAt;
            }
        }
        return null;
    }

    public Bitmap apply(Bitmap bitmap, FilterEnvironment filterEnvironment) {
        return applyBorder(applyFilters(bitmap, -1, -1, filterEnvironment), filterEnvironment);
    }

    public Collection<FilterRepresentation> getGeometryFilters() {
        ArrayList arrayList = new ArrayList();
        for (FilterRepresentation filterRepresentation : this.mFilters) {
            if (filterRepresentation.getFilterType() == 7) {
                arrayList.add(filterRepresentation);
            }
        }
        return arrayList;
    }

    public FilterRepresentation getFilterWithSerializationName(String str) {
        for (FilterRepresentation filterRepresentation : this.mFilters) {
            if (filterRepresentation != null && sameSerializationName(filterRepresentation.getSerializationName(), str)) {
                return filterRepresentation.copy();
            }
        }
        return null;
    }

    public Rect finalGeometryRect(int i, int i2) {
        return GeometryMathUtils.finalGeometryRect(i, i2, getGeometryFilters());
    }

    public Bitmap applyGeometry(Bitmap bitmap, FilterEnvironment filterEnvironment) {
        if (this.mDoApplyGeometry) {
            Bitmap bitmapApplyGeometryRepresentations = GeometryMathUtils.applyGeometryRepresentations(getGeometryFilters(), bitmap);
            if (bitmapApplyGeometryRepresentations != bitmap) {
                filterEnvironment.cache(bitmap);
            }
            return bitmapApplyGeometryRepresentations;
        }
        return bitmap;
    }

    public Bitmap applyBorder(Bitmap bitmap, FilterEnvironment filterEnvironment) {
        FilterRepresentation filterRepresentationForType = getFilterRepresentationForType(1);
        if (filterRepresentationForType != null && this.mDoApplyGeometry) {
            Bitmap bitmapApplyRepresentation = filterEnvironment.applyRepresentation(filterRepresentationForType, bitmap);
            filterEnvironment.getQuality();
            return bitmapApplyRepresentation;
        }
        return bitmap;
    }

    public int nbFilters() {
        return this.mFilters.size();
    }

    public Bitmap applyFilters(Bitmap bitmap, int i, int i2, FilterEnvironment filterEnvironment) {
        if (this.mDoApplyFilters) {
            if (i < 0) {
                i = 0;
            }
            if (i2 == -1) {
                i2 = this.mFilters.size();
            }
            while (i < i2) {
                FilterRepresentation filterRepresentationElementAt = this.mFilters.elementAt(i);
                if (filterRepresentationElementAt.getFilterType() != 7 && filterRepresentationElementAt.getFilterType() != 1) {
                    Bitmap bitmapApplyRepresentation = filterEnvironment.applyRepresentation(filterRepresentationElementAt, bitmap);
                    if (bitmap != bitmapApplyRepresentation) {
                        filterEnvironment.cache(bitmap);
                    }
                    if (!filterEnvironment.needsStop()) {
                        bitmap = bitmapApplyRepresentation;
                    } else {
                        return bitmapApplyRepresentation;
                    }
                }
                i++;
            }
        }
        return bitmap;
    }

    public boolean canDoPartialRendering() {
        if (MasterImage.getImage().getZoomOrientation() != 1) {
            return false;
        }
        for (int i = 0; i < this.mFilters.size(); i++) {
            if (!this.mFilters.elementAt(i).supportsPartialRendering()) {
                return false;
            }
        }
        return true;
    }

    public void fillImageStateAdapter(StateAdapter stateAdapter) {
        State state;
        if (stateAdapter == null) {
            return;
        }
        Vector<State> vector = new Vector<>();
        for (FilterRepresentation filterRepresentation : this.mFilters) {
            if (!(filterRepresentation instanceof FilterUserPresetRepresentation)) {
                if (filterRepresentation instanceof FilterDrawRepresentation) {
                    state = new State(stateAdapter.getContext().getString(R.string.imageDraw));
                } else if (filterRepresentation instanceof FilterRotateRepresentation) {
                    state = new State(stateAdapter.getContext().getString(R.string.rotate));
                } else if (filterRepresentation instanceof FilterMirrorRepresentation) {
                    state = new State(stateAdapter.getContext().getString(R.string.mirror));
                } else if (filterRepresentation instanceof FilterCropRepresentation) {
                    state = new State(stateAdapter.getContext().getString(R.string.crop));
                } else if (filterRepresentation instanceof FilterStraightenRepresentation) {
                    state = new State(stateAdapter.getContext().getString(R.string.straighten));
                } else {
                    state = new State(filterRepresentation.getName());
                }
                state.setFilterRepresentation(filterRepresentation);
                vector.add(state);
            }
        }
        stateAdapter.fill(vector);
    }

    public void setPartialRendering(boolean z, Rect rect) {
        this.mPartialRendering = z;
        this.mPartialRenderingBounds = rect;
    }

    public Vector<ImageFilter> getUsedFilters(BaseFiltersManager baseFiltersManager) {
        Vector<ImageFilter> vector = new Vector<>();
        for (int i = 0; i < this.mFilters.size(); i++) {
            vector.add(baseFiltersManager.getFilterForRepresentation(this.mFilters.elementAt(i)));
        }
        return vector;
    }

    public String getJsonString(String str) {
        StringWriter stringWriter = new StringWriter();
        try {
            JsonWriter jsonWriter = new JsonWriter(stringWriter);
            writeJson(jsonWriter, str);
            jsonWriter.close();
            return stringWriter.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public void writeJson(JsonWriter jsonWriter, String str) {
        int size = this.mFilters.size();
        try {
            jsonWriter.beginObject();
            for (int i = 0; i < size; i++) {
                FilterRepresentation filterRepresentation = this.mFilters.get(i);
                if (!(filterRepresentation instanceof FilterUserPresetRepresentation)) {
                    jsonWriter.name(filterRepresentation.getSerializationName());
                    filterRepresentation.serializeRepresentation(jsonWriter);
                }
            }
            jsonWriter.endObject();
        } catch (IOException e) {
            Log.e("ImagePreset", "Error encoding JASON", e);
        }
    }

    public boolean readJsonFromString(String str) {
        if (str == null) {
            Log.v("ImagePreset", "<readJsonFromString> filterString is null!!");
            return false;
        }
        try {
            JsonReader jsonReader = new JsonReader(new StringReader(str));
            if (!readJson(jsonReader)) {
                jsonReader.close();
                return false;
            }
            jsonReader.close();
            return true;
        } catch (Exception e) {
            Log.e("ImagePreset", "\"" + str + "\"");
            Log.e("ImagePreset", "parsing the filter parameters:", e);
            return false;
        }
    }

    public boolean readJson(JsonReader jsonReader) throws IOException {
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String strNextName = jsonReader.nextName();
            FilterRepresentation filterRepresentationCreatFilterFromName = creatFilterFromName(strNextName);
            if (filterRepresentationCreatFilterFromName == null) {
                Log.w("ImagePreset", "UNKNOWN FILTER! " + strNextName);
                return false;
            }
            filterRepresentationCreatFilterFromName.deSerializeRepresentation(jsonReader);
            addFilter(filterRepresentationCreatFilterFromName);
        }
        jsonReader.endObject();
        return true;
    }

    FilterRepresentation creatFilterFromName(String str) {
        if ("ROTATION".equals(str)) {
            return new FilterRotateRepresentation();
        }
        if ("MIRROR".equals(str)) {
            return new FilterMirrorRepresentation();
        }
        if ("STRAIGHTEN".equals(str)) {
            return new FilterStraightenRepresentation();
        }
        if ("CROP".equals(str)) {
            return new FilterCropRepresentation();
        }
        return FiltersManager.getManager().createFilterFromName(str);
    }

    public void updateWith(ImagePreset imagePreset) {
        if (imagePreset.mFilters.size() != this.mFilters.size()) {
            Log.e("ImagePreset", "Updating a preset with an incompatible one");
            return;
        }
        for (int i = 0; i < this.mFilters.size(); i++) {
            this.mFilters.elementAt(i).useParametersFrom(imagePreset.mFilters.elementAt(i));
        }
    }
}
