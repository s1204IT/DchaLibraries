package com.android.settings.intelligence.suggestions.model;

public class SuggestionCategory {
    private final String mCategory;
    private final boolean mExclusive;
    private final long mExclusiveExpireDaysInMillis;

    private SuggestionCategory(Builder builder) {
        this.mCategory = builder.mCategory;
        this.mExclusive = builder.mExclusive;
        this.mExclusiveExpireDaysInMillis = builder.mExclusiveExpireDaysInMillis;
    }

    public String getCategory() {
        return this.mCategory;
    }

    public boolean isExclusive() {
        return this.mExclusive;
    }

    public long getExclusiveExpireDaysInMillis() {
        return this.mExclusiveExpireDaysInMillis;
    }

    public static class Builder {
        private String mCategory;
        private boolean mExclusive;
        private long mExclusiveExpireDaysInMillis;

        public Builder setCategory(String str) {
            this.mCategory = str;
            return this;
        }

        public Builder setExclusive(boolean z) {
            this.mExclusive = z;
            return this;
        }

        public Builder setExclusiveExpireDaysInMillis(long j) {
            this.mExclusiveExpireDaysInMillis = j;
            return this;
        }

        public SuggestionCategory build() {
            return new SuggestionCategory(this);
        }
    }
}
