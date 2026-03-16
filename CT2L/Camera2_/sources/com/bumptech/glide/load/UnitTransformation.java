package com.bumptech.glide.load;

import com.bumptech.glide.load.engine.Resource;

public class UnitTransformation<T> implements Transformation<T> {
    private static final UnitTransformation TRANSFORMATION = new UnitTransformation();

    public static <T> UnitTransformation<T> get() {
        return TRANSFORMATION;
    }

    @Override
    public Resource<T> transform(Resource<T> resource, int outWidth, int outHeight) {
        return resource;
    }

    @Override
    public String getId() {
        return "";
    }
}
