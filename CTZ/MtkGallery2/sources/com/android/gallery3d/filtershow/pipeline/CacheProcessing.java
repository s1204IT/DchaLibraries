package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.GeometryMathUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

public class CacheProcessing {
    private Vector<CacheStep> mSteps = new Vector<>();

    static class CacheStep {
        Bitmap cache;
        ArrayList<FilterRepresentation> representations = new ArrayList<>();

        public void add(FilterRepresentation filterRepresentation) {
            this.representations.add(filterRepresentation);
        }

        public boolean canMergeWith(FilterRepresentation filterRepresentation) {
            Iterator<FilterRepresentation> it = this.representations.iterator();
            while (it.hasNext()) {
                if (!it.next().canMergeWith(filterRepresentation)) {
                    return false;
                }
            }
            return true;
        }

        public boolean equals(CacheStep cacheStep) {
            if (this.representations.size() != cacheStep.representations.size()) {
                return false;
            }
            for (int i = 0; i < this.representations.size(); i++) {
                if (!this.representations.get(i).equals(cacheStep.representations.get(i))) {
                    return false;
                }
            }
            return true;
        }

        public static Vector<CacheStep> buildSteps(Vector<FilterRepresentation> vector) {
            Vector<CacheStep> vector2 = new Vector<>();
            CacheStep cacheStep = new CacheStep();
            for (int i = 0; i < vector.size(); i++) {
                FilterRepresentation filterRepresentationElementAt = vector.elementAt(i);
                if (cacheStep.canMergeWith(filterRepresentationElementAt)) {
                    cacheStep.add(filterRepresentationElementAt.copy());
                } else {
                    vector2.add(cacheStep);
                    cacheStep = new CacheStep();
                    cacheStep.add(filterRepresentationElementAt.copy());
                }
            }
            vector2.add(cacheStep);
            return vector2;
        }

        public Bitmap apply(FilterEnvironment filterEnvironment, Bitmap bitmap) {
            boolean z;
            Bitmap bitmapApplyGeometryRepresentations;
            Iterator<FilterRepresentation> it = this.representations.iterator();
            while (true) {
                if (it.hasNext()) {
                    if (it.next().getFilterType() != 7) {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                ArrayList arrayList = new ArrayList();
                Iterator<FilterRepresentation> it2 = this.representations.iterator();
                while (it2.hasNext()) {
                    arrayList.add(it2.next());
                }
                bitmapApplyGeometryRepresentations = GeometryMathUtils.applyGeometryRepresentations(arrayList, bitmap);
            } else {
                Iterator<FilterRepresentation> it3 = this.representations.iterator();
                Bitmap bitmapApplyRepresentation = bitmap;
                while (it3.hasNext()) {
                    bitmapApplyRepresentation = filterEnvironment.applyRepresentation(it3.next(), bitmapApplyRepresentation);
                }
                bitmapApplyGeometryRepresentations = bitmapApplyRepresentation;
            }
            if (bitmapApplyGeometryRepresentations != bitmap) {
                filterEnvironment.cache(bitmap);
            }
            return bitmapApplyGeometryRepresentations;
        }
    }

    public Bitmap process(Bitmap bitmap, Vector<FilterRepresentation> vector, FilterEnvironment filterEnvironment) {
        Bitmap bitmap2;
        int i;
        Bitmap bitmap3;
        if (vector.size() == 0) {
            return filterEnvironment.getBitmapCopy(bitmap, 11);
        }
        filterEnvironment.getBimapCache().setCacheProcessing(this);
        Vector<CacheStep> vectorBuildSteps = CacheStep.buildSteps(vector);
        if (vectorBuildSteps.size() != this.mSteps.size()) {
            this.mSteps = vectorBuildSteps;
        }
        boolean zEquals = true;
        int i2 = -1;
        for (int i3 = 0; i3 < vectorBuildSteps.size(); i3++) {
            CacheStep cacheStepElementAt = vectorBuildSteps.elementAt(i3);
            CacheStep cacheStepElementAt2 = this.mSteps.elementAt(i3);
            if (zEquals) {
                zEquals = cacheStepElementAt.equals(cacheStepElementAt2);
            }
            if (zEquals) {
                i2 = i3;
            } else {
                this.mSteps.remove(i3);
                this.mSteps.insertElementAt(cacheStepElementAt, i3);
                filterEnvironment.cache(cacheStepElementAt2.cache);
            }
        }
        if (i2 > -1) {
            i = i2;
            while (i > 0 && this.mSteps.elementAt(i).cache == null) {
                i--;
            }
            bitmap2 = this.mSteps.elementAt(i).cache;
        } else {
            bitmap2 = null;
            i = i2;
        }
        int i4 = -1;
        Bitmap bitmap4 = bitmap2;
        Bitmap bitmapCopy = null;
        while (i < this.mSteps.size()) {
            if (i == -1 || bitmap4 == null) {
                bitmapCopy = filterEnvironment.getBitmapCopy(bitmap, 12);
                if (i == -1) {
                    bitmap4 = bitmapCopy;
                    i++;
                } else {
                    bitmap3 = bitmapCopy;
                }
            } else {
                Bitmap bitmap5 = bitmap4;
                bitmap3 = bitmapCopy;
                bitmapCopy = bitmap5;
            }
            CacheStep cacheStepElementAt3 = this.mSteps.elementAt(i);
            if (cacheStepElementAt3.cache == null) {
                bitmapCopy = cacheStepElementAt3.apply(filterEnvironment, filterEnvironment.getBitmapCopy(bitmapCopy, 1));
                cacheStepElementAt3.cache = bitmapCopy;
                i4 = i;
            }
            Bitmap bitmap6 = bitmap3;
            bitmap4 = bitmapCopy;
            bitmapCopy = bitmap6;
            i++;
        }
        filterEnvironment.cache(bitmapCopy);
        for (int i5 = 0; i5 < i2; i5++) {
            CacheStep cacheStepElementAt4 = this.mSteps.elementAt(i5);
            Bitmap bitmap7 = cacheStepElementAt4.cache;
            cacheStepElementAt4.cache = null;
            filterEnvironment.cache(bitmap7);
        }
        if (i4 != -1) {
            this.mSteps.elementAt(i4).cache = null;
        }
        if (contains(bitmap4)) {
            return filterEnvironment.getBitmapCopy(bitmap4, 13);
        }
        return bitmap4;
    }

    public boolean contains(Bitmap bitmap) {
        for (int i = 0; i < this.mSteps.size(); i++) {
            if (this.mSteps.elementAt(i).cache == bitmap) {
                return true;
            }
        }
        return false;
    }
}
