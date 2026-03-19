package android.media;

import android.media.update.ApiLoader;
import android.media.update.VolumeProvider2Provider;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class VolumeProvider2 {
    public static final int VOLUME_CONTROL_ABSOLUTE = 2;
    public static final int VOLUME_CONTROL_FIXED = 0;
    public static final int VOLUME_CONTROL_RELATIVE = 1;
    private final VolumeProvider2Provider mProvider;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ControlType {
    }

    public VolumeProvider2(int i, int i2, int i3) {
        this.mProvider = ApiLoader.getProvider().createVolumeProvider2(this, i, i2, i3);
    }

    public VolumeProvider2Provider getProvider() {
        return this.mProvider;
    }

    public final int getControlType() {
        return this.mProvider.getControlType_impl();
    }

    public final int getMaxVolume() {
        return this.mProvider.getMaxVolume_impl();
    }

    public final int getCurrentVolume() {
        return this.mProvider.getCurrentVolume_impl();
    }

    public final void setCurrentVolume(int i) {
        this.mProvider.setCurrentVolume_impl(i);
    }

    public void onSetVolumeTo(int i) {
    }

    public void onAdjustVolume(int i) {
    }
}
