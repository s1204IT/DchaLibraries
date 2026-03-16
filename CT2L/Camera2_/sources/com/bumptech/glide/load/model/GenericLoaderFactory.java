package com.bumptech.glide.load.model;

import android.content.Context;
import com.bumptech.glide.load.data.DataFetcher;
import java.util.HashMap;
import java.util.Map;

public class GenericLoaderFactory {
    private static final ModelLoader NULL_MODEL_LOADER = new ModelLoader() {
        @Override
        public DataFetcher getResourceFetcher(Object model, int width, int height) {
            throw new NoSuchMethodError("This should never be called!");
        }

        public String toString() {
            return "NULL_MODEL_LOADER";
        }
    };
    private Map<Class, Map<Class, ModelLoaderFactory>> modelClassToResourceFactories = new HashMap();
    private Map<Class, Map<Class, ModelLoader>> cachedModelLoaders = new HashMap();

    public <T, Y> ModelLoaderFactory<T, Y> unregister(Class<T> modelClass, Class<Y> resourceClass) {
        this.cachedModelLoaders.clear();
        Map<Class, ModelLoaderFactory> resourceToFactories = this.modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories == null) {
            return null;
        }
        ModelLoaderFactory<T, Y> result = resourceToFactories.remove(resourceClass);
        return result;
    }

    public <T, Y> ModelLoaderFactory<T, Y> register(Class<T> modelClass, Class<Y> resourceClass, ModelLoaderFactory<T, Y> factory) {
        this.cachedModelLoaders.clear();
        Map<Class, ModelLoaderFactory> resourceToFactories = this.modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories == null) {
            resourceToFactories = new HashMap<>();
            this.modelClassToResourceFactories.put(modelClass, resourceToFactories);
        }
        ModelLoaderFactory<T, Y> previous = resourceToFactories.put(resourceClass, factory);
        if (previous != null) {
            for (Map<Class, ModelLoaderFactory> currentResourceToFactories : this.modelClassToResourceFactories.values()) {
                if (currentResourceToFactories.containsValue(previous)) {
                    return null;
                }
            }
            return previous;
        }
        return previous;
    }

    public <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass, Context context) {
        ModelLoader<T, Y> result = getCachedLoader(modelClass, resourceClass);
        if (result != null) {
            if (NULL_MODEL_LOADER.equals(result)) {
                return null;
            }
            return result;
        }
        ModelLoaderFactory<T, Y> factory = getFactory(modelClass, resourceClass);
        if (factory != null) {
            result = factory.build(context, this);
            cacheModelLoader(modelClass, resourceClass, result);
        } else {
            cacheNullLoader(modelClass, resourceClass);
        }
        return result;
    }

    private <T, Y> void cacheNullLoader(Class<T> modelClass, Class<Y> resourceClass) {
        cacheModelLoader(modelClass, resourceClass, NULL_MODEL_LOADER);
    }

    private <T, Y> void cacheModelLoader(Class<T> modelClass, Class<Y> resourceClass, ModelLoader<T, Y> modelLoader) {
        Map<Class, ModelLoader> resourceToLoaders = this.cachedModelLoaders.get(modelClass);
        if (resourceToLoaders == null) {
            resourceToLoaders = new HashMap<>();
            this.cachedModelLoaders.put(modelClass, resourceToLoaders);
        }
        resourceToLoaders.put(resourceClass, modelLoader);
    }

    private <T, Y> ModelLoader<T, Y> getCachedLoader(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class, ModelLoader> resourceToLoaders = this.cachedModelLoaders.get(modelClass);
        if (resourceToLoaders == null) {
            return null;
        }
        return resourceToLoaders.get(resourceClass);
    }

    private <T, Y> ModelLoaderFactory<T, Y> getFactory(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class, ModelLoaderFactory> currentResourceToFactories;
        Map<Class, ModelLoaderFactory> resourceToFactories = this.modelClassToResourceFactories.get(modelClass);
        ModelLoaderFactory modelLoaderFactory = null;
        if (resourceToFactories != null) {
            modelLoaderFactory = resourceToFactories.get(resourceClass);
        }
        if (modelLoaderFactory == null) {
            for (Class registeredModelClass : this.modelClassToResourceFactories.keySet()) {
                if (registeredModelClass.isAssignableFrom(modelClass) && (currentResourceToFactories = this.modelClassToResourceFactories.get(registeredModelClass)) != null) {
                    ModelLoaderFactory result = currentResourceToFactories.get(resourceClass);
                    modelLoaderFactory = result;
                    if (modelLoaderFactory != null) {
                        break;
                    }
                }
            }
        }
        return modelLoaderFactory;
    }
}
