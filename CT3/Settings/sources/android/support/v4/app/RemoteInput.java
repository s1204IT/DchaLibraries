package android.support.v4.app;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.RemoteInputCompatBase$RemoteInput;

public final class RemoteInput extends RemoteInputCompatBase$RemoteInput {
    public static final RemoteInputCompatBase$RemoteInput.Factory FACTORY;
    private static final Impl IMPL;
    private final boolean mAllowFreeFormInput;
    private final CharSequence[] mChoices;
    private final Bundle mExtras;
    private final CharSequence mLabel;
    private final String mResultKey;

    interface Impl {
    }

    @Override
    public String getResultKey() {
        return this.mResultKey;
    }

    @Override
    public CharSequence getLabel() {
        return this.mLabel;
    }

    @Override
    public CharSequence[] getChoices() {
        return this.mChoices;
    }

    @Override
    public boolean getAllowFreeFormInput() {
        return this.mAllowFreeFormInput;
    }

    @Override
    public Bundle getExtras() {
        return this.mExtras;
    }

    static class ImplBase implements Impl {
        ImplBase() {
        }
    }

    static class ImplJellybean implements Impl {
        ImplJellybean() {
        }
    }

    static class ImplApi20 implements Impl {
        ImplApi20() {
        }
    }

    static {
        if (Build.VERSION.SDK_INT >= 20) {
            IMPL = new ImplApi20();
        } else if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new ImplJellybean();
        } else {
            IMPL = new ImplBase();
        }
        FACTORY = new RemoteInputCompatBase$RemoteInput.Factory() {
        };
    }
}
