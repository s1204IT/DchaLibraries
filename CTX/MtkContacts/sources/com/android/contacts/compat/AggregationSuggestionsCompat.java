package com.android.contacts.compat;

import android.net.Uri;
import android.provider.ContactsContract;
import java.util.ArrayList;

public class AggregationSuggestionsCompat {

    public static final class Builder {
        private long mContactId;
        private int mLimit;
        private final ArrayList<String> mValues = new ArrayList<>();

        public Builder setContactId(long j) {
            this.mContactId = j;
            return this;
        }

        public Builder addNameParameter(String str) {
            this.mValues.add(str);
            return this;
        }

        public Builder setLimit(int i) {
            this.mLimit = i;
            return this;
        }

        public Uri build() {
            Uri.Builder builderBuildUpon = ContactsContract.Contacts.CONTENT_URI.buildUpon();
            builderBuildUpon.appendEncodedPath(String.valueOf(this.mContactId));
            builderBuildUpon.appendPath("suggestions");
            if (this.mLimit != 0) {
                builderBuildUpon.appendQueryParameter("limit", String.valueOf(this.mLimit));
            }
            int size = this.mValues.size();
            for (int i = 0; i < size; i++) {
                builderBuildUpon.appendQueryParameter("query", "name:" + this.mValues.get(i));
            }
            return builderBuildUpon.build();
        }
    }
}
