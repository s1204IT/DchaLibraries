package com.android.systemui.plugins;

import android.util.ArrayMap;
import com.android.systemui.plugins.VersionInfo;
import com.android.systemui.plugins.annotations.Dependencies;
import com.android.systemui.plugins.annotations.DependsOn;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import com.android.systemui.plugins.annotations.Requirements;
import com.android.systemui.plugins.annotations.Requires;
import java.util.function.BiConsumer;

/* loaded from: classes.dex */
public class VersionInfo {
    private Class<?> mDefault;
    private final ArrayMap<Class<?>, Version> mVersions = new ArrayMap<>();

    public boolean hasVersionInfo() {
        return !this.mVersions.isEmpty();
    }

    public int getDefaultVersion() {
        return this.mVersions.get(this.mDefault).mVersion;
    }

    public VersionInfo addClass(Class<?> cls) {
        if (this.mDefault == null) {
            this.mDefault = cls;
        }
        addClass(cls, false);
        return this;
    }

    private void addClass(Class<?> cls, boolean z) {
        if (this.mVersions.containsKey(cls)) {
            return;
        }
        ProvidesInterface providesInterface = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (providesInterface != null) {
            this.mVersions.put(cls, new Version(providesInterface.version(), true));
        }
        Requires requires = (Requires) cls.getDeclaredAnnotation(Requires.class);
        if (requires != null) {
            this.mVersions.put(requires.target(), new Version(requires.version(), z));
        }
        Requirements requirements = (Requirements) cls.getDeclaredAnnotation(Requirements.class);
        if (requirements != null) {
            for (Requires requires2 : requirements.value()) {
                this.mVersions.put(requires2.target(), new Version(requires2.version(), z));
            }
        }
        DependsOn dependsOn = (DependsOn) cls.getDeclaredAnnotation(DependsOn.class);
        if (dependsOn != null) {
            addClass(dependsOn.target(), true);
        }
        Dependencies dependencies = (Dependencies) cls.getDeclaredAnnotation(Dependencies.class);
        if (dependencies != null) {
            for (DependsOn dependsOn2 : dependencies.value()) {
                addClass(dependsOn2.target(), true);
            }
        }
    }

    public void checkVersion(VersionInfo versionInfo) throws InvalidVersionException {
        final ArrayMap arrayMap = new ArrayMap(this.mVersions);
        versionInfo.mVersions.forEach(new BiConsumer() { // from class: com.android.systemui.plugins.-$$Lambda$VersionInfo$36RaSV8LaXl8e0tazdPbdpQ95k8
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                VersionInfo.lambda$checkVersion$0(this.f$0, arrayMap, (Class) obj, (VersionInfo.Version) obj2);
            }
        });
        arrayMap.forEach(new BiConsumer() { // from class: com.android.systemui.plugins.-$$Lambda$VersionInfo$OmM59IDVY01yoqUNn9tP6VG6ndM
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                VersionInfo.lambda$checkVersion$1((Class) obj, (VersionInfo.Version) obj2);
            }
        });
    }

    public static /* synthetic */ void lambda$checkVersion$0(VersionInfo versionInfo, ArrayMap arrayMap, Class cls, Version version) {
        Version versionCreateVersion = (Version) arrayMap.remove(cls);
        if (versionCreateVersion == null) {
            versionCreateVersion = versionInfo.createVersion(cls);
        }
        if (versionCreateVersion == null) {
            throw new InvalidVersionException(cls.getSimpleName() + " does not provide an interface", false);
        }
        if (versionCreateVersion.mVersion != version.mVersion) {
            throw new InvalidVersionException(cls, versionCreateVersion.mVersion < version.mVersion, versionCreateVersion.mVersion, version.mVersion);
        }
    }

    static /* synthetic */ void lambda$checkVersion$1(Class cls, Version version) {
        if (version.mRequired) {
            throw new InvalidVersionException("Missing required dependency " + cls.getSimpleName(), false);
        }
    }

    private Version createVersion(Class<?> cls) {
        ProvidesInterface providesInterface = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (providesInterface != null) {
            return new Version(providesInterface.version(), false);
        }
        return null;
    }

    public <T> boolean hasClass(Class<T> cls) {
        return this.mVersions.containsKey(cls);
    }

    public static class InvalidVersionException extends RuntimeException {
        private final boolean mTooNew;

        public InvalidVersionException(String str, boolean z) {
            super(str);
            this.mTooNew = z;
        }

        public InvalidVersionException(Class<?> cls, boolean z, int i, int i2) {
            super(cls.getSimpleName() + " expected version " + i + " but had " + i2);
            this.mTooNew = z;
        }

        public boolean isTooNew() {
            return this.mTooNew;
        }
    }

    private static class Version {
        private final boolean mRequired;
        private final int mVersion;

        public Version(int i, boolean z) {
            this.mVersion = i;
            this.mRequired = z;
        }
    }
}
