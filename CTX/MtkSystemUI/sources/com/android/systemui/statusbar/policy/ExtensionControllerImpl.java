package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.util.ArrayMap;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.ExtensionControllerImpl;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.leak.LeakDetector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class ExtensionControllerImpl implements ExtensionController {
    private final Context mDefaultContext;

    private interface Item<T> extends Producer<T> {
        int sortOrder();
    }

    private interface Producer<T> {
        void destroy();

        T get();
    }

    public ExtensionControllerImpl(Context context) {
        this.mDefaultContext = context;
    }

    @Override
    public <T> ExtensionBuilder<T> newExtension(Class<T> cls) {
        return new ExtensionBuilder<>();
    }

    private class ExtensionBuilder<T> implements ExtensionController.ExtensionBuilder<T> {
        private ExtensionImpl<T> mExtension;

        private ExtensionBuilder() {
            this.mExtension = new ExtensionImpl<>();
        }

        @Override
        public ExtensionController.ExtensionBuilder<T> withTunerFactory(ExtensionController.TunerFactory<T> tunerFactory) {
            this.mExtension.addTunerFactory(tunerFactory, tunerFactory.keys());
            return this;
        }

        @Override
        public <P extends T> ExtensionController.ExtensionBuilder<T> withPlugin(Class<P> cls) {
            return withPlugin(cls, PluginManager.getAction(cls));
        }

        public <P extends T> ExtensionController.ExtensionBuilder<T> withPlugin(Class<P> cls, String str) {
            return withPlugin(cls, str, null);
        }

        @Override
        public <P> ExtensionController.ExtensionBuilder<T> withPlugin(Class<P> cls, String str, ExtensionController.PluginConverter<T, P> pluginConverter) {
            this.mExtension.addPlugin(str, cls, pluginConverter);
            return this;
        }

        @Override
        public ExtensionController.ExtensionBuilder<T> withDefault(Supplier<T> supplier) {
            this.mExtension.addDefault(supplier);
            return this;
        }

        @Override
        public ExtensionController.ExtensionBuilder<T> withFeature(String str, Supplier<T> supplier) {
            this.mExtension.addFeature(str, supplier);
            return this;
        }

        @Override
        public ExtensionController.ExtensionBuilder<T> withCallback(Consumer<T> consumer) {
            ((ExtensionImpl) this.mExtension).mCallbacks.add(consumer);
            return this;
        }

        @Override
        public ExtensionController.Extension build() {
            Collections.sort(((ExtensionImpl) this.mExtension).mProducers, Comparator.comparingInt(new ToIntFunction() {
                @Override
                public final int applyAsInt(Object obj) {
                    return ((ExtensionControllerImpl.Item) obj).sortOrder();
                }
            }));
            this.mExtension.notifyChanged();
            return this.mExtension;
        }
    }

    private class ExtensionImpl<T> implements ExtensionController.Extension<T> {
        private final ArrayList<Consumer<T>> mCallbacks;
        private T mItem;
        private Context mPluginContext;
        private final ArrayList<Item<T>> mProducers;

        private ExtensionImpl() {
            this.mProducers = new ArrayList<>();
            this.mCallbacks = new ArrayList<>();
        }

        @Override
        public void addCallback(Consumer<T> consumer) {
            this.mCallbacks.add(consumer);
        }

        @Override
        public T get() {
            return this.mItem;
        }

        @Override
        public Context getContext() {
            return this.mPluginContext != null ? this.mPluginContext : ExtensionControllerImpl.this.mDefaultContext;
        }

        @Override
        public void destroy() {
            for (int i = 0; i < this.mProducers.size(); i++) {
                this.mProducers.get(i).destroy();
            }
        }

        @Override
        public void clearItem(boolean z) {
            if (z && this.mItem != null) {
                ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(this.mItem);
            }
            this.mItem = null;
        }

        private void notifyChanged() {
            if (this.mItem != null) {
                ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(this.mItem);
            }
            this.mItem = null;
            int i = 0;
            while (true) {
                if (i >= this.mProducers.size()) {
                    break;
                }
                T t = this.mProducers.get(i).get();
                if (t == null) {
                    i++;
                } else {
                    this.mItem = t;
                    break;
                }
            }
            for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
                this.mCallbacks.get(i2).accept(this.mItem);
            }
        }

        public void addDefault(Supplier<T> supplier) {
            this.mProducers.add(new Default(supplier));
        }

        public <P> void addPlugin(String str, Class<P> cls, ExtensionController.PluginConverter<T, P> pluginConverter) {
            this.mProducers.add(new PluginItem(str, cls, pluginConverter));
        }

        public void addTunerFactory(ExtensionController.TunerFactory<T> tunerFactory, String[] strArr) {
            this.mProducers.add(new TunerItem(tunerFactory, strArr));
        }

        public void addFeature(String str, Supplier<T> supplier) {
            this.mProducers.add(new FeatureItem(str, supplier));
        }

        private class PluginItem<P extends Plugin> implements PluginListener<P>, Item<T> {
            private final ExtensionController.PluginConverter<T, P> mConverter;
            private T mItem;

            public PluginItem(String str, Class<P> cls, ExtensionController.PluginConverter<T, P> pluginConverter) {
                this.mConverter = pluginConverter;
                ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(str, (PluginListener) this, (Class<?>) cls);
            }

            @Override
            public void onPluginConnected(P p, Context context) {
                ExtensionImpl.this.mPluginContext = context;
                if (this.mConverter != null) {
                    this.mItem = this.mConverter.getInterfaceFromPlugin(p);
                } else {
                    this.mItem = p;
                }
                ExtensionImpl.this.notifyChanged();
            }

            @Override
            public void onPluginDisconnected(P p) {
                ExtensionImpl.this.mPluginContext = null;
                this.mItem = null;
                ExtensionImpl.this.notifyChanged();
            }

            @Override
            public T get() {
                return this.mItem;
            }

            @Override
            public void destroy() {
                ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
            }

            @Override
            public int sortOrder() {
                return 0;
            }
        }

        private class TunerItem<T> implements Item<T>, TunerService.Tunable {
            private final ExtensionController.TunerFactory<T> mFactory;
            private T mItem;
            private final ArrayMap<String, String> mSettings = new ArrayMap<>();

            public TunerItem(ExtensionController.TunerFactory<T> tunerFactory, String... strArr) {
                this.mFactory = tunerFactory;
                ((TunerService) Dependency.get(TunerService.class)).addTunable(this, strArr);
            }

            @Override
            public T get() {
                return this.mItem;
            }

            @Override
            public void destroy() {
                ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
            }

            @Override
            public void onTuningChanged(String str, String str2) {
                this.mSettings.put(str, str2);
                this.mItem = this.mFactory.create(this.mSettings);
                ExtensionImpl.this.notifyChanged();
            }

            @Override
            public int sortOrder() {
                return 1;
            }
        }

        private class FeatureItem<T> implements Item<T> {
            private final String mFeature;
            private final Supplier<T> mSupplier;

            public FeatureItem(String str, Supplier<T> supplier) {
                this.mSupplier = supplier;
                this.mFeature = str;
            }

            @Override
            public T get() {
                if (ExtensionControllerImpl.this.mDefaultContext.getPackageManager().hasSystemFeature(this.mFeature)) {
                    return this.mSupplier.get();
                }
                return null;
            }

            @Override
            public void destroy() {
            }

            @Override
            public int sortOrder() {
                return 2;
            }
        }

        private class Default<T> implements Item<T> {
            private final Supplier<T> mSupplier;

            public Default(Supplier<T> supplier) {
                this.mSupplier = supplier;
            }

            @Override
            public T get() {
                return this.mSupplier.get();
            }

            @Override
            public void destroy() {
            }

            @Override
            public int sortOrder() {
                return 4;
            }
        }
    }
}
