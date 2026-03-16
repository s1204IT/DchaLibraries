package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.android.providers.contacts.VoicemailTable;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.TypedUriMatcherImpl;
import java.io.FileNotFoundException;
import java.util.List;

public class VoicemailContentProvider extends ContentProvider implements VoicemailTable.DelegateHelper {
    private VoicemailTable.Delegate mVoicemailContentTable;
    private VoicemailPermissions mVoicemailPermissions;
    private VoicemailTable.Delegate mVoicemailStatusTable;

    @Override
    public boolean onCreate() {
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "VoicemailContentProvider.onCreate start");
        }
        Context context = context();
        this.mVoicemailPermissions = new VoicemailPermissions(context);
        this.mVoicemailContentTable = new VoicemailContentTable("calls", context, getDatabaseHelper(context), this, createCallLogInsertionHelper(context));
        this.mVoicemailStatusTable = new VoicemailStatusTable("voicemail_status", context, getDatabaseHelper(context), this);
        if (Log.isLoggable("ContactsPerf", 3)) {
            Log.d("ContactsPerf", "VoicemailContentProvider.onCreate finish");
            return true;
        }
        return true;
    }

    CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    ContactsDatabaseHelper getDatabaseHelper(Context context) {
        return ContactsDatabaseHelper.getInstance(context);
    }

    Context context() {
        return getContext();
    }

    @Override
    public String getType(Uri uri) {
        try {
            UriData uriData = UriData.createUriData(uri);
            return getTableDelegate(uriData).getType(uriData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        UriData uriData = checkPermissionsAndCreateUriDataForWrite(uri, values);
        return getTableDelegate(uriData).insert(uriData, values);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        UriData uriData = checkPermissionsAndCreateUriDataForRead(uri);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        selectionBuilder.addClause(getPackageRestrictionClause(true));
        return getTableDelegate(uriData).query(uriData, projection, selectionBuilder.build(), selectionArgs, sortOrder);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        UriData uriData = checkPermissionsAndCreateUriDataForWrite(uri, values);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        selectionBuilder.addClause(getPackageRestrictionClause(false));
        return getTableDelegate(uriData).update(uriData, values, selectionBuilder.build(), selectionArgs);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        UriData uriData = checkPermissionsAndCreateUriDataForWrite(uri, new ContentValues[0]);
        SelectionBuilder selectionBuilder = new SelectionBuilder(selection);
        selectionBuilder.addClause(getPackageRestrictionClause(false));
        return getTableDelegate(uriData).delete(uriData, selectionBuilder.build(), selectionArgs);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        UriData uriData;
        if (mode.equals("r")) {
            uriData = checkPermissionsAndCreateUriDataForRead(uri);
        } else {
            uriData = checkPermissionsAndCreateUriDataForWrite(uri, new ContentValues[0]);
        }
        return getTableDelegate(uriData).openFile(uriData, mode);
    }

    private VoicemailTable.Delegate getTableDelegate(UriData uriData) {
        switch (uriData.getUriType()) {
            case STATUS:
            case STATUS_ID:
                return this.mVoicemailStatusTable;
            case VOICEMAILS:
            case VOICEMAILS_ID:
                return this.mVoicemailContentTable;
            case NO_MATCH:
                throw new IllegalStateException("Invalid uri type for uri: " + uriData.getUri());
            default:
                throw new IllegalStateException("Impossible, all cases are covered.");
        }
    }

    public static class UriData {
        private final String mId;
        private final String mSourcePackage;
        private final Uri mUri;
        private final VoicemailUriType mUriType;

        public UriData(Uri uri, VoicemailUriType uriType, String id, String sourcePackage) {
            this.mUriType = uriType;
            this.mUri = uri;
            this.mId = id;
            this.mSourcePackage = sourcePackage;
        }

        public final Uri getUri() {
            return this.mUri;
        }

        public final boolean hasId() {
            return this.mId != null;
        }

        public final String getId() {
            return this.mId;
        }

        public final boolean hasSourcePackage() {
            return this.mSourcePackage != null;
        }

        public final String getSourcePackage() {
            return this.mSourcePackage;
        }

        public final VoicemailUriType getUriType() {
            return this.mUriType;
        }

        public final String getWhereClause() {
            String[] strArr = new String[2];
            strArr[0] = hasId() ? DbQueryUtils.getEqualityClause("_id", getId()) : null;
            strArr[1] = hasSourcePackage() ? DbQueryUtils.getEqualityClause("source_package", getSourcePackage()) : null;
            return DbQueryUtils.concatenateClauses(strArr);
        }

        public static UriData createUriData(Uri uri) {
            String sourcePackage = uri.getQueryParameter("source_package");
            List<String> segments = uri.getPathSegments();
            VoicemailUriType uriType = (VoicemailUriType) createUriMatcher().match(uri);
            switch (uriType) {
                case STATUS:
                case VOICEMAILS:
                    return new UriData(uri, uriType, null, sourcePackage);
                case STATUS_ID:
                case VOICEMAILS_ID:
                    return new UriData(uri, uriType, segments.get(1), sourcePackage);
                case NO_MATCH:
                    throw new IllegalArgumentException("Invalid URI: " + uri);
                default:
                    throw new IllegalStateException("Impossible, all cases are covered");
            }
        }

        private static TypedUriMatcherImpl<VoicemailUriType> createUriMatcher() {
            return new TypedUriMatcherImpl("com.android.voicemail", VoicemailUriType.values());
        }
    }

    @Override
    public void checkAndAddSourcePackageIntoValues(UriData uriData, ContentValues values) {
        if (!values.containsKey("source_package")) {
            String provider = uriData.hasSourcePackage() ? uriData.getSourcePackage() : getCallingPackage_();
            values.put("source_package", provider);
        }
        if (!this.mVoicemailPermissions.callerHasWriteAccess()) {
            checkPackagesMatch(getCallingPackage_(), values.getAsString("source_package"), uriData.getUri());
        }
    }

    private void checkSourcePackageSameIfSet(UriData uriData, ContentValues values) {
        if (uriData.hasSourcePackage() && values.containsKey("source_package") && !uriData.getSourcePackage().equals(values.get("source_package"))) {
            throw new SecurityException("source_package in URI was " + uriData.getSourcePackage() + " but doesn't match source_package in ContentValues which was " + values.get("source_package"));
        }
    }

    @Override
    public ParcelFileDescriptor openDataFile(UriData uriData, String mode) throws FileNotFoundException {
        return openFileHelper(uriData.getUri(), mode);
    }

    private UriData checkPermissionsAndCreateUriDataForRead(Uri uri) {
        if (context().checkCallingUriPermission(uri, 1) == 0) {
            return UriData.createUriData(uri);
        }
        if (this.mVoicemailPermissions.callerHasReadAccess()) {
            return UriData.createUriData(uri);
        }
        return checkPermissionsAndCreateUriData(uri, true);
    }

    private UriData checkPermissionsAndCreateUriData(Uri uri, boolean read) {
        UriData uriData = UriData.createUriData(uri);
        if (!hasReadWritePermission(read)) {
            this.mVoicemailPermissions.checkCallerHasOwnVoicemailAccess();
            checkPackagePermission(uriData);
        }
        return uriData;
    }

    private UriData checkPermissionsAndCreateUriDataForWrite(Uri uri, ContentValues... valuesArray) {
        UriData uriData = checkPermissionsAndCreateUriData(uri, false);
        for (ContentValues values : valuesArray) {
            checkSourcePackageSameIfSet(uriData, values);
        }
        return uriData;
    }

    private final void checkPackagesMatch(String callingPackage, String voicemailSourcePackage, Uri uri) {
        if (!voicemailSourcePackage.equals(callingPackage)) {
            String errorMsg = String.format("Permission denied for URI: %s\n. Package %s cannot perform this operation for %s. Requires %s permission.", uri, callingPackage, voicemailSourcePackage, "com.android.voicemail.permission.WRITE_VOICEMAIL");
            throw new SecurityException(errorMsg);
        }
    }

    private void checkPackagePermission(UriData uriData) {
        if (!this.mVoicemailPermissions.callerHasWriteAccess()) {
            if (!uriData.hasSourcePackage()) {
                throw new SecurityException(String.format("Provider %s does not have %s permission.\nPlease set query parameter '%s' in the URI.\nURI: %s", getCallingPackage_(), "com.android.voicemail.permission.WRITE_VOICEMAIL", "source_package", uriData.getUri()));
            }
            checkPackagesMatch(getCallingPackage_(), uriData.getSourcePackage(), uriData.getUri());
        }
    }

    String getCallingPackage_() {
        String[] callerPackages;
        int caller = Binder.getCallingUid();
        if (caller == 0 || (callerPackages = context().getPackageManager().getPackagesForUid(caller)) == null || callerPackages.length == 0) {
            return null;
        }
        if (callerPackages.length == 1) {
            return callerPackages[0];
        }
        String bestSoFar = callerPackages[0];
        for (String callerPackage : callerPackages) {
            if (!this.mVoicemailPermissions.packageHasWriteAccess(callerPackage)) {
                if (this.mVoicemailPermissions.packageHasOwnVoicemailAccess(callerPackage)) {
                    bestSoFar = callerPackage;
                }
            } else {
                return callerPackage;
            }
        }
        return bestSoFar;
    }

    private String getPackageRestrictionClause(boolean isQuery) {
        if (hasReadWritePermission(isQuery)) {
            return null;
        }
        return DbQueryUtils.getEqualityClause("source_package", getCallingPackage_());
    }

    private boolean hasReadWritePermission(boolean read) {
        return read ? this.mVoicemailPermissions.callerHasReadAccess() : this.mVoicemailPermissions.callerHasWriteAccess();
    }
}
