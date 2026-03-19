package com.android.managedprovisioning.task;

import android.content.IntentFilter;
import com.android.internal.annotations.Immutable;
import com.android.internal.util.Preconditions;

@Immutable
final class CrossProfileIntentFilter {
    public final int direction;
    public final IntentFilter filter;
    public final int flags;
    public final boolean letsPersonalDataIntoProfile;

    private CrossProfileIntentFilter(IntentFilter intentFilter, int i, int i2, boolean z) {
        this.filter = (IntentFilter) Preconditions.checkNotNull(intentFilter);
        this.flags = i;
        this.direction = i2;
        this.letsPersonalDataIntoProfile = z;
    }

    static final class Builder {
        private int mDirection;
        private IntentFilter mFilter = new IntentFilter();
        private int mFlags;
        public boolean mLetsPersonalDataIntoProfile;

        public Builder(int i, int i2, boolean z) {
            this.mFlags = 0;
            this.mDirection = i;
            this.mFlags = i2;
            this.mLetsPersonalDataIntoProfile = z;
        }

        Builder addAction(String str) {
            this.mFilter.addAction(str);
            return this;
        }

        Builder addCategory(String str) {
            this.mFilter.addCategory(str);
            return this;
        }

        Builder addDataType(String str) {
            try {
                this.mFilter.addDataType(str);
            } catch (IntentFilter.MalformedMimeTypeException e) {
            }
            return this;
        }

        Builder addDataScheme(String str) {
            this.mFilter.addDataScheme(str);
            return this;
        }

        CrossProfileIntentFilter build() {
            return new CrossProfileIntentFilter(this.mFilter, this.mFlags, this.mDirection, this.mLetsPersonalDataIntoProfile);
        }
    }
}
