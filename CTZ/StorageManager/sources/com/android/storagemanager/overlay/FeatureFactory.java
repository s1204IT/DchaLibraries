package com.android.storagemanager.overlay;

import android.content.Context;
import android.text.TextUtils;
import com.android.storagemanager.R;

public abstract class FeatureFactory {
    private static FeatureFactory sFactory;

    public abstract DeletionHelperFeatureProvider getDeletionHelperFeatureProvider();

    public abstract StorageManagementJobProvider getStorageManagementJobProvider();

    public static FeatureFactory getFactory(Context context) {
        if (sFactory != null) {
            return sFactory;
        }
        String string = context.getString(R.string.config_featureFactory);
        if (TextUtils.isEmpty(string)) {
            throw new UnsupportedOperationException("No feature factory configured");
        }
        try {
            sFactory = (FeatureFactory) context.getClassLoader().loadClass(string).newInstance();
            return sFactory;
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            throw new FactoryNotFoundException(e);
        }
    }

    public static class FactoryNotFoundException extends RuntimeException {
        public FactoryNotFoundException(Throwable th) {
            super("Unable to create factory. Did you misconfigure Proguard?", th);
        }
    }
}
