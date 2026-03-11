package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import com.android.systemui.ConfigurationChangedReceiver;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.util.ArrayList;
import java.util.function.Consumer;

public class ConfigurationControllerImpl implements ConfigurationChangedReceiver, ConfigurationController {
    private int mDensity;
    private float mFontScale;
    private boolean mInCarMode;
    private LocaleList mLocaleList;
    private int mUiMode;
    private final ArrayList<ConfigurationController.ConfigurationListener> mListeners = new ArrayList<>();
    private final Configuration mLastConfig = new Configuration();

    public ConfigurationControllerImpl(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        this.mFontScale = configuration.fontScale;
        this.mDensity = configuration.densityDpi;
        this.mInCarMode = (configuration.uiMode & 15) == 3;
        this.mUiMode = configuration.uiMode & 48;
        this.mLocaleList = configuration.getLocales();
    }

    @Override
    public void onConfigurationChanged(final Configuration configuration) {
        ArrayList arrayList = new ArrayList(this.mListeners);
        arrayList.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ConfigurationControllerImpl.lambda$onConfigurationChanged$0(this.f$0, configuration, (ConfigurationController.ConfigurationListener) obj);
            }
        });
        float f = configuration.fontScale;
        int i = configuration.densityDpi;
        int i2 = configuration.uiMode & 48;
        if (i != this.mDensity || f != this.mFontScale || (this.mInCarMode && i2 != this.mUiMode)) {
            arrayList.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ConfigurationControllerImpl.lambda$onConfigurationChanged$1(this.f$0, (ConfigurationController.ConfigurationListener) obj);
                }
            });
            this.mDensity = i;
            this.mFontScale = f;
            this.mUiMode = i2;
        }
        LocaleList locales = configuration.getLocales();
        if (!locales.equals(this.mLocaleList)) {
            this.mLocaleList = locales;
            arrayList.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ConfigurationControllerImpl.lambda$onConfigurationChanged$2(this.f$0, (ConfigurationController.ConfigurationListener) obj);
                }
            });
        }
        if ((this.mLastConfig.updateFrom(configuration) & Integer.MIN_VALUE) != 0) {
            arrayList.forEach(new Consumer() {
                @Override
                public final void accept(Object obj) {
                    ConfigurationControllerImpl.lambda$onConfigurationChanged$3(this.f$0, (ConfigurationController.ConfigurationListener) obj);
                }
            });
        }
    }

    public static void lambda$onConfigurationChanged$0(ConfigurationControllerImpl configurationControllerImpl, Configuration configuration, ConfigurationController.ConfigurationListener configurationListener) {
        if (configurationControllerImpl.mListeners.contains(configurationListener)) {
            configurationListener.onConfigChanged(configuration);
        }
    }

    public static void lambda$onConfigurationChanged$1(ConfigurationControllerImpl configurationControllerImpl, ConfigurationController.ConfigurationListener configurationListener) {
        if (configurationControllerImpl.mListeners.contains(configurationListener)) {
            configurationListener.onDensityOrFontScaleChanged();
        }
    }

    public static void lambda$onConfigurationChanged$2(ConfigurationControllerImpl configurationControllerImpl, ConfigurationController.ConfigurationListener configurationListener) {
        if (configurationControllerImpl.mListeners.contains(configurationListener)) {
            configurationListener.onLocaleListChanged();
        }
    }

    public static void lambda$onConfigurationChanged$3(ConfigurationControllerImpl configurationControllerImpl, ConfigurationController.ConfigurationListener configurationListener) {
        if (configurationControllerImpl.mListeners.contains(configurationListener)) {
            configurationListener.onOverlayChanged();
        }
    }

    @Override
    public void addCallback(ConfigurationController.ConfigurationListener configurationListener) {
        this.mListeners.add(configurationListener);
        configurationListener.onDensityOrFontScaleChanged();
    }

    @Override
    public void removeCallback(ConfigurationController.ConfigurationListener configurationListener) {
        this.mListeners.remove(configurationListener);
    }
}
