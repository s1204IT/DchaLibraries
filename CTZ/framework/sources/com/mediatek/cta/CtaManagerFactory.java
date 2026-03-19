package com.mediatek.cta;

import android.util.Log;

public class CtaManagerFactory {
    private static final String TAG = "CtaManagerFactory";
    private static CtaManagerFactory sInstance = null;
    protected static CtaManager sCtaManager = null;

    public static CtaManagerFactory getInstance() {
        CtaManagerFactory ctaManagerFactory;
        if (sInstance != null) {
            return sInstance;
        }
        try {
            try {
                sInstance = (CtaManagerFactory) Class.forName("com.mediatek.cta.CtaManagerFactoryImpl").getConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (Exception e) {
                Log.w(TAG, "CtaManagerFactoryImpl not found");
                if (sInstance == null) {
                    ctaManagerFactory = new CtaManagerFactory();
                }
            }
        } catch (Throwable th) {
            if (sInstance == null) {
                ctaManagerFactory = new CtaManagerFactory();
            }
        }
        if (sInstance == null) {
            ctaManagerFactory = new CtaManagerFactory();
            sInstance = ctaManagerFactory;
        }
        return sInstance;
    }

    public CtaManager makeCtaManager() {
        if (sCtaManager == null) {
            sCtaManager = new CtaManager();
        }
        return sCtaManager;
    }
}
