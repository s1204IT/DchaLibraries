package com.android.documentsui.base;

import android.app.Activity;
import com.android.documentsui.base.CheckedTask;
import java.util.Objects;

public abstract class PairedTask<Owner extends Activity, Input, Output> extends CheckedTask<Input, Output> {
    protected final Owner mOwner;

    public PairedTask(final Owner owner) {
        super(new CheckedTask.Check() {
            @Override
            public final boolean stop() {
                return owner.isDestroyed();
            }
        });
        Objects.requireNonNull(owner);
        this.mOwner = owner;
    }
}
