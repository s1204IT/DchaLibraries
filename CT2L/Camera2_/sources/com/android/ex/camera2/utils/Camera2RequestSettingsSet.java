package com.android.ex.camera2.utils;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Camera2RequestSettingsSet {
    private final Map<CaptureRequest.Key<?>, Object> mDictionary;
    private long mRevision;

    public Camera2RequestSettingsSet() {
        this.mDictionary = new HashMap();
        this.mRevision = 0L;
    }

    public Camera2RequestSettingsSet(Camera2RequestSettingsSet other) {
        if (other == null) {
            throw new NullPointerException("Tried to copy null Camera2RequestSettingsSet");
        }
        this.mDictionary = new HashMap(other.mDictionary);
        this.mRevision = other.mRevision;
    }

    public <T> boolean set(CaptureRequest.Key<T> key, T value) {
        if (key == null) {
            throw new NullPointerException("Received a null key");
        }
        Object currentValue = get(key);
        if (this.mDictionary.containsKey(key) && Objects.equals(value, currentValue)) {
            return false;
        }
        this.mDictionary.put(key, value);
        this.mRevision++;
        return true;
    }

    public boolean unset(CaptureRequest.Key<?> key) {
        if (key == null) {
            throw new NullPointerException("Received a null key");
        }
        if (!this.mDictionary.containsKey(key)) {
            return false;
        }
        this.mDictionary.remove(key);
        this.mRevision++;
        return true;
    }

    public <T> T get(CaptureRequest.Key<T> key) {
        if (key == null) {
            throw new NullPointerException("Received a null key");
        }
        return (T) this.mDictionary.get(key);
    }

    public boolean contains(CaptureRequest.Key<?> key) {
        if (key == null) {
            throw new NullPointerException("Received a null key");
        }
        return this.mDictionary.containsKey(key);
    }

    public <T> boolean matches(CaptureRequest.Key<T> key, T value) {
        return Objects.equals(get(key), value);
    }

    public long getRevision() {
        return this.mRevision;
    }

    public boolean union(Camera2RequestSettingsSet moreSettings) {
        if (moreSettings == null || moreSettings == this) {
            return false;
        }
        this.mDictionary.putAll(moreSettings.mDictionary);
        this.mRevision++;
        return true;
    }

    public CaptureRequest createRequest(CameraDevice camera, int template, Surface... targets) throws CameraAccessException {
        if (camera == null) {
            throw new NullPointerException("Tried to create request using null CameraDevice");
        }
        CaptureRequest.Builder reqBuilder = camera.createCaptureRequest(template);
        for (CaptureRequest.Key<?> key : this.mDictionary.keySet()) {
            setRequestFieldIfNonNull(reqBuilder, key);
        }
        for (Surface target : targets) {
            if (target == null) {
                throw new NullPointerException("Tried to add null Surface as request target");
            }
            reqBuilder.addTarget(target);
        }
        return reqBuilder.build();
    }

    private <T> void setRequestFieldIfNonNull(CaptureRequest.Builder builder, CaptureRequest.Key<T> key) {
        Object obj = get(key);
        if (obj != null) {
            builder.set(key, obj);
        }
    }
}
