package com.mediatek.view;

import android.util.Log;

public class SurfaceFactory {
    private static final String CLASS_NAME_SURFACE_FACTORY_IMPL = "com.mediatek.view.impl.SurfaceFactoryImpl";
    private static final String TAG = "SurfaceFactory";
    private static final SurfaceFactory sSurfaceFactory;

    static {
        SurfaceFactory surfaceFactory;
        try {
            surfaceFactory = (SurfaceFactory) Class.forName(CLASS_NAME_SURFACE_FACTORY_IMPL).newInstance();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "[static] ClassNotFoundException", e);
            surfaceFactory = null;
        } catch (IllegalAccessException e2) {
            Log.e(TAG, "[static] InstantiationException", e2);
            surfaceFactory = null;
        } catch (InstantiationException e3) {
            Log.e(TAG, "[static] InstantiationException", e3);
            surfaceFactory = null;
        }
        if (surfaceFactory == null) {
            surfaceFactory = new SurfaceFactory();
        }
        sSurfaceFactory = surfaceFactory;
        Log.i(TAG, "[static] sSurfaceFactory = " + sSurfaceFactory);
    }

    public static final SurfaceFactory getInstance() {
        return sSurfaceFactory;
    }

    public SurfaceExt getSurfaceExt() {
        return new SurfaceExt();
    }
}
