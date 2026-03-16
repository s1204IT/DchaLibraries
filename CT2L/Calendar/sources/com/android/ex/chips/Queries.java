package com.android.ex.chips;

import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;

class Queries {
    public static final Query PHONE = new Query(new String[]{"display_name", "data1", "data2", "data3", "contact_id", "_id", "photo_thumb_uri", "display_name_source", "lookup", "mimetype"}, ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, ContactsContract.CommonDataKinds.Phone.CONTENT_URI) {
        @Override
        public CharSequence getTypeLabel(Resources res, int type, CharSequence label) {
            return ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, type, label);
        }
    };
    public static final Query EMAIL = new Query(new String[]{"display_name", "data1", "data2", "data3", "contact_id", "_id", "photo_thumb_uri", "display_name_source", "lookup", "mimetype"}, ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, ContactsContract.CommonDataKinds.Email.CONTENT_URI) {
        @Override
        public CharSequence getTypeLabel(Resources res, int type, CharSequence label) {
            return ContactsContract.CommonDataKinds.Email.getTypeLabel(res, type, label);
        }
    };

    static abstract class Query {
        private final Uri mContentFilterUri;
        private final Uri mContentUri;
        private final String[] mProjection;

        public abstract CharSequence getTypeLabel(Resources resources, int i, CharSequence charSequence);

        public Query(String[] projection, Uri contentFilter, Uri content) {
            this.mProjection = projection;
            this.mContentFilterUri = contentFilter;
            this.mContentUri = content;
        }

        public String[] getProjection() {
            return this.mProjection;
        }

        public Uri getContentFilterUri() {
            return this.mContentFilterUri;
        }

        public Uri getContentUri() {
            return this.mContentUri;
        }
    }
}
