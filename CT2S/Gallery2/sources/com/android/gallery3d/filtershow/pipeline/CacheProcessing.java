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

        public void add(FilterRepresentation representation) {
            this.representations.add(representation);
        }

        public boolean canMergeWith(FilterRepresentation representation) {
            for (FilterRepresentation rep : this.representations) {
                if (!rep.canMergeWith(representation)) {
                    return false;
                }
            }
            return true;
        }

        public boolean equals(CacheStep step) {
            if (this.representations.size() != step.representations.size()) {
                return false;
            }
            for (int i = 0; i < this.representations.size(); i++) {
                FilterRepresentation r1 = this.representations.get(i);
                FilterRepresentation r2 = step.representations.get(i);
                if (!r1.equals(r2)) {
                    return false;
                }
            }
            return true;
        }

        public static Vector<CacheStep> buildSteps(Vector<FilterRepresentation> filters) {
            Vector<CacheStep> steps = new Vector<>();
            CacheStep step = new CacheStep();
            for (int i = 0; i < filters.size(); i++) {
                FilterRepresentation representation = filters.elementAt(i);
                if (step.canMergeWith(representation)) {
                    step.add(representation.copy());
                } else {
                    steps.add(step);
                    step = new CacheStep();
                    step.add(representation.copy());
                }
            }
            steps.add(step);
            return steps;
        }

        public Bitmap apply(FilterEnvironment environment, Bitmap cacheBitmap) {
            boolean onlyGeometry = true;
            Iterator<FilterRepresentation> it = this.representations.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                FilterRepresentation representation = it.next();
                if (representation.getFilterType() != 7) {
                    onlyGeometry = false;
                    break;
                }
            }
            if (onlyGeometry) {
                ArrayList<FilterRepresentation> geometry = new ArrayList<>();
                for (FilterRepresentation representation2 : this.representations) {
                    geometry.add(representation2);
                }
                cacheBitmap = GeometryMathUtils.applyGeometryRepresentations(geometry, cacheBitmap);
            } else {
                for (FilterRepresentation representation3 : this.representations) {
                    cacheBitmap = environment.applyRepresentation(representation3, cacheBitmap);
                }
            }
            if (cacheBitmap != cacheBitmap) {
                environment.cache(cacheBitmap);
            }
            return cacheBitmap;
        }
    }

    public Bitmap process(Bitmap originalBitmap, Vector<FilterRepresentation> filters, FilterEnvironment environment) {
        if (filters.size() == 0) {
            return environment.getBitmapCopy(originalBitmap, 11);
        }
        environment.getBimapCache().setCacheProcessing(this);
        Vector<CacheStep> steps = CacheStep.buildSteps(filters);
        if (steps.size() != this.mSteps.size()) {
            this.mSteps = steps;
        }
        int similarUpToIndex = -1;
        boolean similar = true;
        for (int i = 0; i < steps.size(); i++) {
            CacheStep newStep = steps.elementAt(i);
            CacheStep cacheStep = this.mSteps.elementAt(i);
            if (similar) {
                similar = newStep.equals(cacheStep);
            }
            if (similar) {
                similarUpToIndex = i;
            } else {
                this.mSteps.remove(i);
                this.mSteps.insertElementAt(newStep, i);
                environment.cache(cacheStep.cache);
            }
        }
        Bitmap cacheBitmap = null;
        int findBaseImageIndex = similarUpToIndex;
        if (findBaseImageIndex > -1) {
            while (findBaseImageIndex > 0 && this.mSteps.elementAt(findBaseImageIndex).cache == null) {
                findBaseImageIndex--;
            }
            cacheBitmap = this.mSteps.elementAt(findBaseImageIndex).cache;
        }
        Bitmap originalCopy = null;
        int lastPositionCached = -1;
        for (int i2 = findBaseImageIndex; i2 < this.mSteps.size(); i2++) {
            if (i2 == -1 || cacheBitmap == null) {
                cacheBitmap = environment.getBitmapCopy(originalBitmap, 12);
                originalCopy = cacheBitmap;
                if (i2 != -1) {
                    CacheStep step = this.mSteps.elementAt(i2);
                    if (step.cache == null) {
                        cacheBitmap = step.apply(environment, environment.getBitmapCopy(cacheBitmap, 1));
                        step.cache = cacheBitmap;
                        lastPositionCached = i2;
                    }
                }
            }
        }
        environment.cache(originalCopy);
        for (int i3 = 0; i3 < similarUpToIndex; i3++) {
            CacheStep currentStep = this.mSteps.elementAt(i3);
            Bitmap bitmap = currentStep.cache;
            currentStep.cache = null;
            environment.cache(bitmap);
        }
        if (lastPositionCached != -1) {
            this.mSteps.elementAt(lastPositionCached).cache = null;
        }
        if (contains(cacheBitmap)) {
            return environment.getBitmapCopy(cacheBitmap, 13);
        }
        return cacheBitmap;
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
