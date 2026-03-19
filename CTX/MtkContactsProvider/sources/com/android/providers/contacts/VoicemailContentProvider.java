package com.android.providers.contacts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.VoicemailContract;
import android.util.ArraySet;
import android.util.Log;
import com.android.providers.contacts.VoicemailTable;
import com.android.providers.contacts.util.ContactsPermissions;
import com.android.providers.contacts.util.DbQueryUtils;
import com.android.providers.contacts.util.PackageUtils;
import com.android.providers.contacts.util.SelectionBuilder;
import com.android.providers.contacts.util.TypedUriMatcherImpl;
import com.android.providers.contacts.util.UserUtils;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class VoicemailContentProvider extends ContentProvider implements VoicemailTable.DelegateHelper {
    public static final boolean VERBOSE_LOGGING = Log.isLoggable("VoicemailProvider", 2);
    private ContactsTaskScheduler mTaskScheduler;
    private VoicemailTable.Delegate mVoicemailContentTable;
    private VoicemailPermissions mVoicemailPermissions;
    private VoicemailTable.Delegate mVoicemailStatusTable;

    @Override
    public boolean onCreate() {
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "onCreate: " + getClass().getSimpleName() + " user=" + Process.myUserHandle().getIdentifier());
        }
        if (Log.isLoggable("ContactsPerf", 4)) {
            Log.i("ContactsPerf", "VoicemailContentProvider.onCreate start");
        }
        Context context = context();
        setReadPermission("com.android.voicemail.permission.ADD_VOICEMAIL");
        setWritePermission("com.android.voicemail.permission.ADD_VOICEMAIL");
        setAppOps(52, 52);
        this.mVoicemailPermissions = new VoicemailPermissions(context);
        this.mVoicemailContentTable = new VoicemailContentTable("calls", context, getDatabaseHelper(context), this, createCallLogInsertionHelper(context));
        this.mVoicemailStatusTable = new VoicemailStatusTable("voicemail_status", context, getDatabaseHelper(context), this);
        this.mTaskScheduler = new ContactsTaskScheduler(getClass().getSimpleName()) {
            @Override
            public void onPerformTask(int i, Object obj) {
                VoicemailContentProvider.this.performBackgroundTask(i, obj);
            }
        };
        scheduleScanStalePackages();
        ContactsPackageMonitor.start(getContext());
        if (Log.isLoggable("ContactsPerf", 4)) {
            Log.i("ContactsPerf", "VoicemailContentProvider.onCreate finish");
            return true;
        }
        return true;
    }

    protected int enforceReadPermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        if (this.mVoicemailPermissions.callerHasCarrierPrivileges()) {
            return 0;
        }
        return super.enforceReadPermissionInner(uri, str, iBinder);
    }

    protected int enforceWritePermissionInner(Uri uri, String str, IBinder iBinder) throws SecurityException {
        if (this.mVoicemailPermissions.callerHasCarrierPrivileges()) {
            return 0;
        }
        return super.enforceWritePermissionInner(uri, str, iBinder);
    }

    void scheduleScanStalePackages() {
        scheduleTask(0, null);
    }

    void scheduleTask(int i, Object obj) {
        this.mTaskScheduler.scheduleTask(i, obj);
    }

    CallLogInsertionHelper createCallLogInsertionHelper(Context context) {
        return DefaultCallLogInsertionHelper.getInstance(context);
    }

    CallLogDatabaseHelper getDatabaseHelper(Context context) {
        return CallLogDatabaseHelper.getInstance(context);
    }

    Context context() {
        return getContext();
    }

    @Override
    public String getType(Uri uri) {
        try {
            UriData uriDataCreateUriData = UriData.createUriData(uri);
            return getTableDelegate(uriDataCreateUriData).getType(uriDataCreateUriData);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "insert: uri=" + uri + "  values=[" + contentValues + "] CPID=" + Binder.getCallingPid());
        }
        UriData uriDataCheckPermissionsAndCreateUriDataForWrite = checkPermissionsAndCreateUriDataForWrite(uri, contentValues);
        return getTableDelegate(uriDataCheckPermissionsAndCreateUriDataForWrite).insert(uriDataCheckPermissionsAndCreateUriDataForWrite, contentValues);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) {
        UriData uriDataCheckPermissionsAndCreateUriDataForWrite = checkPermissionsAndCreateUriDataForWrite(uri, contentValuesArr);
        return getTableDelegate(uriDataCheckPermissionsAndCreateUriDataForWrite).bulkInsert(uriDataCheckPermissionsAndCreateUriDataForWrite, contentValuesArr);
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "query: uri=" + uri + "  projection=" + Arrays.toString(strArr) + "  selection=[" + str + "]  args=" + Arrays.toString(strArr2) + "  order=[" + str2 + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        UriData uriDataCheckPermissionsAndCreateUriDataForRead = checkPermissionsAndCreateUriDataForRead(uri);
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        selectionBuilder.addClause(getPackageRestrictionClause(true));
        return getTableDelegate(uriDataCheckPermissionsAndCreateUriDataForRead).query(uriDataCheckPermissionsAndCreateUriDataForRead, strArr, selectionBuilder.build(), strArr2, str2);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "update: uri=" + uri + "  selection=[" + str + "]  args=" + Arrays.toString(strArr) + "  values=[" + contentValues + "] CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        UriData uriDataCheckPermissionsAndCreateUriDataForWrite = checkPermissionsAndCreateUriDataForWrite(uri, contentValues);
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        selectionBuilder.addClause(getPackageRestrictionClause(false));
        return getTableDelegate(uriDataCheckPermissionsAndCreateUriDataForWrite).update(uriDataCheckPermissionsAndCreateUriDataForWrite, contentValues, selectionBuilder.build(), strArr);
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) {
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "delete: uri=" + uri + "  selection=[" + str + "]  args=" + Arrays.toString(strArr) + " CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
        }
        UriData uriDataCheckPermissionsAndCreateUriDataForWrite = checkPermissionsAndCreateUriDataForWrite(uri, new ContentValues[0]);
        SelectionBuilder selectionBuilder = new SelectionBuilder(str);
        selectionBuilder.addClause(getPackageRestrictionClause(false));
        return getTableDelegate(uriDataCheckPermissionsAndCreateUriDataForWrite).delete(uriDataCheckPermissionsAndCreateUriDataForWrite, selectionBuilder.build(), strArr);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        try {
            UriData uriDataCheckPermissionsAndCreateUriDataForRead = str.equals("r") ? checkPermissionsAndCreateUriDataForRead(uri) : checkPermissionsAndCreateUriDataForWrite(uri, new ContentValues[0]);
            ParcelFileDescriptor parcelFileDescriptorOpenFile = getTableDelegate(uriDataCheckPermissionsAndCreateUriDataForRead).openFile(uriDataCheckPermissionsAndCreateUriDataForRead, str);
            if (VERBOSE_LOGGING) {
                Log.v("VoicemailProvider", "openFile uri=" + uri + " mode=" + str + " success=true CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
            }
            return parcelFileDescriptorOpenFile;
        } catch (Throwable th) {
            if (VERBOSE_LOGGING) {
                Log.v("VoicemailProvider", "openFile uri=" + uri + " mode=" + str + " success=false CPID=" + Binder.getCallingPid() + " User=" + UserUtils.getCurrentUserHandle(getContext()));
            }
            throw th;
        }
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

        private UriData(Uri uri, VoicemailUriType voicemailUriType, String str, String str2) {
            this.mUriType = voicemailUriType;
            this.mUri = uri;
            this.mId = str;
            this.mSourcePackage = str2;
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
            String queryParameter = uri.getQueryParameter("source_package");
            List<String> pathSegments = uri.getPathSegments();
            VoicemailUriType voicemailUriType = (VoicemailUriType) createUriMatcher().match(uri);
            switch (voicemailUriType) {
                case STATUS:
                case VOICEMAILS:
                    return new UriData(uri, voicemailUriType, null, queryParameter);
                case STATUS_ID:
                case VOICEMAILS_ID:
                    return new UriData(uri, voicemailUriType, pathSegments.get(1), queryParameter);
                case NO_MATCH:
                    throw new IllegalArgumentException("Invalid URI: " + uri);
                default:
                    throw new IllegalStateException("Impossible, all cases are covered");
            }
        }

        private static TypedUriMatcherImpl<VoicemailUriType> createUriMatcher() {
            return new TypedUriMatcherImpl<>("com.android.voicemail", VoicemailUriType.values());
        }
    }

    @Override
    public void checkAndAddSourcePackageIntoValues(UriData uriData, ContentValues contentValues) {
        if (!contentValues.containsKey("source_package")) {
            contentValues.put("source_package", uriData.hasSourcePackage() ? uriData.getSourcePackage() : getInjectedCallingPackage());
        }
        if (!this.mVoicemailPermissions.callerHasWriteAccess(getCallingPackage())) {
            checkPackagesMatch(getInjectedCallingPackage(), contentValues.getAsString("source_package"), uriData.getUri());
        }
    }

    private void checkSourcePackageSameIfSet(UriData uriData, ContentValues contentValues) {
        if (uriData.hasSourcePackage() && contentValues.containsKey("source_package") && !uriData.getSourcePackage().equals(contentValues.get("source_package"))) {
            throw new SecurityException("source_package in URI was " + uriData.getSourcePackage() + " but doesn't match source_package in ContentValues which was " + contentValues.get("source_package"));
        }
    }

    @Override
    public ParcelFileDescriptor openDataFile(UriData uriData, String str) throws FileNotFoundException {
        return openFileHelper(uriData.getUri(), str);
    }

    private UriData checkPermissionsAndCreateUriDataForRead(Uri uri) {
        if (ContactsPermissions.hasCallerUriPermission(getContext(), uri, 1)) {
            return UriData.createUriData(uri);
        }
        if (this.mVoicemailPermissions.callerHasReadAccess(getCallingPackage())) {
            return UriData.createUriData(uri);
        }
        return checkPermissionsAndCreateUriData(uri, true);
    }

    private UriData checkPermissionsAndCreateUriData(Uri uri, boolean z) {
        UriData uriDataCreateUriData = UriData.createUriData(uri);
        if (!hasReadWritePermission(z)) {
            this.mVoicemailPermissions.checkCallerHasOwnVoicemailAccess();
            checkPackagePermission(uriDataCreateUriData);
        }
        return uriDataCreateUriData;
    }

    private UriData checkPermissionsAndCreateUriDataForWrite(Uri uri, ContentValues... contentValuesArr) {
        UriData uriDataCheckPermissionsAndCreateUriData = checkPermissionsAndCreateUriData(uri, false);
        for (ContentValues contentValues : contentValuesArr) {
            checkSourcePackageSameIfSet(uriDataCheckPermissionsAndCreateUriData, contentValues);
        }
        return uriDataCheckPermissionsAndCreateUriData;
    }

    private final void checkPackagesMatch(String str, String str2, Uri uri) {
        if (!str2.equals(str)) {
            throw new SecurityException(String.format("Permission denied for URI: %s\n. Package %s cannot perform this operation for %s. Requires %s permission.", uri, str, str2, "com.android.voicemail.permission.WRITE_VOICEMAIL"));
        }
    }

    private void checkPackagePermission(UriData uriData) {
        if (!this.mVoicemailPermissions.callerHasWriteAccess(getCallingPackage())) {
            if (!uriData.hasSourcePackage()) {
                throw new SecurityException(String.format("Provider %s does not have %s permission.\nPlease set query parameter '%s' in the URI.\nURI: %s", getInjectedCallingPackage(), "com.android.voicemail.permission.WRITE_VOICEMAIL", "source_package", uriData.getUri()));
            }
            checkPackagesMatch(getInjectedCallingPackage(), uriData.getSourcePackage(), uriData.getUri());
        }
    }

    String getInjectedCallingPackage() {
        return super.getCallingPackage();
    }

    private String getPackageRestrictionClause(boolean z) {
        if (hasReadWritePermission(z)) {
            return null;
        }
        return DbQueryUtils.getEqualityClause("source_package", getInjectedCallingPackage());
    }

    private boolean hasReadWritePermission(boolean z) {
        return z ? this.mVoicemailPermissions.callerHasReadAccess(getCallingPackage()) : this.mVoicemailPermissions.callerHasWriteAccess(getCallingPackage());
    }

    public void removeBySourcePackage(String str) {
        delete(VoicemailContract.Voicemails.buildSourceUri(str), null, null);
        delete(VoicemailContract.Status.buildSourceUri(str), null, null);
    }

    void performBackgroundTask(int i, Object obj) {
        if (i == 0) {
            removeStalePackages();
        }
    }

    private void removeStalePackages() {
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "scanStalePackages start");
        }
        ArraySet<String> sourcePackages = this.mVoicemailContentTable.getSourcePackages();
        sourcePackages.addAll((ArraySet<? extends String>) this.mVoicemailStatusTable.getSourcePackages());
        for (int size = sourcePackages.size() - 1; size >= 0; size--) {
            String strValueAt = sourcePackages.valueAt(size);
            boolean zIsPackageInstalled = PackageUtils.isPackageInstalled(getContext(), strValueAt);
            if (VERBOSE_LOGGING) {
                StringBuilder sb = new StringBuilder();
                sb.append("  ");
                sb.append(strValueAt);
                sb.append(zIsPackageInstalled ? " installed" : " removed");
                Log.v("VoicemailProvider", sb.toString());
            }
            if (!zIsPackageInstalled) {
                removeBySourcePackage(strValueAt);
            }
        }
        if (VERBOSE_LOGGING) {
            Log.v("VoicemailProvider", "scanStalePackages finish");
        }
    }
}
