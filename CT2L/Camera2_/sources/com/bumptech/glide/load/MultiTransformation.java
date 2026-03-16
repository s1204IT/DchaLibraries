package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;
import java.util.Iterator;
import java.util.List;

public class MultiTransformation<T> implements Transformation<T> {
    private String id;
    private List<Transformation<T>> transformationList;
    private Transformation<T>[] transformations;

    public MultiTransformation(Transformation<T>... transformations) {
        if (transformations.length < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformations = transformations;
    }

    public MultiTransformation(List<Transformation<T>> transformationList) {
        if (transformationList.size() < 1) {
            throw new IllegalArgumentException("MultiTransformation must contain at least one Transformation");
        }
        this.transformationList = transformationList;
    }

    @Override
    public Resource<T> transform(Resource<T> resource, int outWidth, int outHeight) {
        Resource<T> previous = resource;
        if (this.transformations != null) {
            for (Transformation<T> transformation : this.transformations) {
                Resource<T> transformed = transformation.transform(previous, outWidth, outHeight);
                if (transformed != previous && previous != resource && previous != null) {
                    previous.recycle();
                }
                previous = transformed;
            }
        } else {
            for (Transformation<T> transformation2 : this.transformationList) {
                Resource<T> transformed2 = transformation2.transform(previous, outWidth, outHeight);
                if (transformed2 != previous && previous != resource && previous != null) {
                    previous.recycle();
                }
                previous = transformed2;
            }
        }
        return previous;
    }

    @Override
    public String getId() {
        if (this.id == null) {
            StringBuilder sb = new StringBuilder();
            if (this.transformations != null) {
                for (Transformation<T> transformation : this.transformations) {
                    sb.append(transformation.getId());
                }
            } else {
                Iterator<Transformation<T>> it = this.transformationList.iterator();
                while (it.hasNext()) {
                    sb.append(it.next().getId());
                }
            }
            this.id = sb.toString();
        }
        return this.id;
    }
}
