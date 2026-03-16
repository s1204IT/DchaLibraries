package com.android.camera.util;

import java.lang.Enum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

public class ConjunctionListenerMux<Input extends Enum<Input>> {
    private final EnumMap<Input, Boolean> mInputs;
    private final List<OutputChangeListener> mListeners;
    private final Object mLock;
    private boolean mOutput;

    public interface OutputChangeListener {
        void onOutputChange(boolean z);
    }

    public void addListener(OutputChangeListener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(OutputChangeListener listener) {
        this.mListeners.remove(listener);
    }

    public boolean getOutput() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mOutput;
        }
        return z;
    }

    public boolean setInput(Input input, boolean newValue) {
        boolean z;
        synchronized (this.mLock) {
            this.mInputs.put(input, Boolean.valueOf(newValue));
            if (newValue == this.mOutput) {
                z = this.mOutput;
            } else {
                boolean oldOutput = this.mOutput;
                this.mOutput = true;
                for (Boolean b : this.mInputs.values()) {
                    this.mOutput &= b.booleanValue();
                }
                if (oldOutput != this.mOutput) {
                    notifyListeners();
                }
                z = this.mOutput;
            }
        }
        return z;
    }

    public ConjunctionListenerMux(Class<Input> clazz, OutputChangeListener listener) {
        this(clazz);
        addListener(listener);
    }

    public ConjunctionListenerMux(Class<Input> clazz) {
        this.mLock = new Object();
        this.mListeners = Collections.synchronizedList(new ArrayList());
        this.mInputs = new EnumMap<>(clazz);
        for (Input i : clazz.getEnumConstants()) {
            this.mInputs.put(i, false);
        }
        this.mOutput = false;
    }

    public void notifyListeners() {
        synchronized (this.mLock) {
            for (OutputChangeListener listener : this.mListeners) {
                listener.onOutputChange(this.mOutput);
            }
        }
    }
}
