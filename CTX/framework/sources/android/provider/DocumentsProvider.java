package android.provider;

import android.Manifest;
import android.app.backup.FullBackup;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.ParcelableException;
import android.provider.DocumentsContract;
import android.util.Log;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Objects;
import libcore.io.IoUtils;

public abstract class DocumentsProvider extends ContentProvider {
    private static final int MATCH_CHILDREN = 6;
    private static final int MATCH_CHILDREN_TREE = 8;
    private static final int MATCH_DOCUMENT = 5;
    private static final int MATCH_DOCUMENT_TREE = 7;
    private static final int MATCH_RECENT = 3;
    private static final int MATCH_ROOT = 2;
    private static final int MATCH_ROOTS = 1;
    private static final int MATCH_SEARCH = 4;
    private static final String TAG = "DocumentsProvider";
    private String mAuthority;
    private UriMatcher mMatcher;

    public abstract ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) throws FileNotFoundException;

    public abstract Cursor queryChildDocuments(String str, String[] strArr, String str2) throws FileNotFoundException;

    public abstract Cursor queryDocument(String str, String[] strArr) throws FileNotFoundException;

    public abstract Cursor queryRoots(String[] strArr) throws FileNotFoundException;

    @Override
    public void attachInfo(Context context, ProviderInfo providerInfo) {
        registerAuthority(providerInfo.authority);
        if (!providerInfo.exported) {
            throw new SecurityException("Provider must be exported");
        }
        if (!providerInfo.grantUriPermissions) {
            throw new SecurityException("Provider must grantUriPermissions");
        }
        if (!Manifest.permission.MANAGE_DOCUMENTS.equals(providerInfo.readPermission) || !Manifest.permission.MANAGE_DOCUMENTS.equals(providerInfo.writePermission)) {
            throw new SecurityException("Provider must be protected by MANAGE_DOCUMENTS");
        }
        super.attachInfo(context, providerInfo);
    }

    @Override
    public void attachInfoForTesting(Context context, ProviderInfo providerInfo) {
        registerAuthority(providerInfo.authority);
        super.attachInfoForTesting(context, providerInfo);
    }

    private void registerAuthority(String str) {
        this.mAuthority = str;
        this.mMatcher = new UriMatcher(-1);
        this.mMatcher.addURI(this.mAuthority, "root", 1);
        this.mMatcher.addURI(this.mAuthority, "root/*", 2);
        this.mMatcher.addURI(this.mAuthority, "root/*/recent", 3);
        this.mMatcher.addURI(this.mAuthority, "root/*/search", 4);
        this.mMatcher.addURI(this.mAuthority, "document/*", 5);
        this.mMatcher.addURI(this.mAuthority, "document/*/children", 6);
        this.mMatcher.addURI(this.mAuthority, "tree/*/document/*", 7);
        this.mMatcher.addURI(this.mAuthority, "tree/*/document/*/children", 8);
    }

    public boolean isChildDocument(String str, String str2) {
        return false;
    }

    private void enforceTree(Uri uri) {
        if (DocumentsContract.isTreeUri(uri)) {
            String treeDocumentId = DocumentsContract.getTreeDocumentId(uri);
            String documentId = DocumentsContract.getDocumentId(uri);
            if (!Objects.equals(treeDocumentId, documentId) && !isChildDocument(treeDocumentId, documentId)) {
                throw new SecurityException("Document " + documentId + " is not a descendant of " + treeDocumentId);
            }
        }
    }

    public String createDocument(String str, String str2, String str3) throws FileNotFoundException {
        throw new UnsupportedOperationException("Create not supported");
    }

    public String renameDocument(String str, String str2) throws FileNotFoundException {
        throw new UnsupportedOperationException("Rename not supported");
    }

    public void deleteDocument(String str) throws FileNotFoundException {
        throw new UnsupportedOperationException("Delete not supported");
    }

    public String copyDocument(String str, String str2) throws FileNotFoundException {
        throw new UnsupportedOperationException("Copy not supported");
    }

    public String moveDocument(String str, String str2, String str3) throws FileNotFoundException {
        throw new UnsupportedOperationException("Move not supported");
    }

    public void removeDocument(String str, String str2) throws FileNotFoundException {
        throw new UnsupportedOperationException("Remove not supported");
    }

    public DocumentsContract.Path findDocumentPath(String str, String str2) throws FileNotFoundException {
        throw new UnsupportedOperationException("findDocumentPath not supported.");
    }

    public IntentSender createWebLinkIntent(String str, Bundle bundle) throws FileNotFoundException {
        throw new UnsupportedOperationException("createWebLink is not supported.");
    }

    public Cursor queryRecentDocuments(String str, String[] strArr) throws FileNotFoundException {
        throw new UnsupportedOperationException("Recent not supported");
    }

    public Cursor queryChildDocuments(String str, String[] strArr, Bundle bundle) throws FileNotFoundException {
        return queryChildDocuments(str, strArr, getSortClause(bundle));
    }

    public Cursor queryChildDocumentsForManage(String str, String[] strArr, String str2) throws FileNotFoundException {
        throw new UnsupportedOperationException("Manage not supported");
    }

    public Cursor querySearchDocuments(String str, String str2, String[] strArr) throws FileNotFoundException {
        throw new UnsupportedOperationException("Search not supported");
    }

    public void ejectRoot(String str) {
        throw new UnsupportedOperationException("Eject not supported");
    }

    public Bundle getDocumentMetadata(String str) throws FileNotFoundException {
        throw new UnsupportedOperationException("Metadata not supported");
    }

    public String getDocumentType(String str) throws FileNotFoundException {
        Cursor cursorQueryDocument = queryDocument(str, null);
        try {
            if (!cursorQueryDocument.moveToFirst()) {
                return null;
            }
            return cursorQueryDocument.getString(cursorQueryDocument.getColumnIndexOrThrow("mime_type"));
        } finally {
            IoUtils.closeQuietly(cursorQueryDocument);
        }
    }

    public AssetFileDescriptor openDocumentThumbnail(String str, Point point, CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new UnsupportedOperationException("Thumbnails not supported");
    }

    public AssetFileDescriptor openTypedDocument(String str, String str2, Bundle bundle, CancellationSignal cancellationSignal) throws FileNotFoundException {
        throw new FileNotFoundException("The requested MIME type is not supported.");
    }

    @Override
    public final Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        throw new UnsupportedOperationException("Pre-Android-O query format not supported.");
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2, CancellationSignal cancellationSignal) {
        throw new UnsupportedOperationException("Pre-Android-O query format not supported.");
    }

    @Override
    public final Cursor query(Uri uri, String[] strArr, Bundle bundle, CancellationSignal cancellationSignal) {
        try {
            int iMatch = this.mMatcher.match(uri);
            if (iMatch == 1) {
                return queryRoots(strArr);
            }
            switch (iMatch) {
                case 3:
                    return queryRecentDocuments(DocumentsContract.getRootId(uri), strArr);
                case 4:
                    return querySearchDocuments(DocumentsContract.getRootId(uri), DocumentsContract.getSearchDocumentsQuery(uri), strArr);
                case 5:
                case 7:
                    enforceTree(uri);
                    return queryDocument(DocumentsContract.getDocumentId(uri), strArr);
                case 6:
                case 8:
                    enforceTree(uri);
                    if (DocumentsContract.isManageMode(uri)) {
                        return queryChildDocumentsForManage(DocumentsContract.getDocumentId(uri), strArr, getSortClause(bundle));
                    }
                    return queryChildDocuments(DocumentsContract.getDocumentId(uri), strArr, bundle);
                default:
                    throw new UnsupportedOperationException("Unsupported Uri " + uri);
            }
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed during query", e);
            return null;
        }
    }

    private static String getSortClause(Bundle bundle) {
        if (bundle == null) {
            bundle = Bundle.EMPTY;
        }
        String string = bundle.getString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER);
        if (string == null && bundle.containsKey(ContentResolver.QUERY_ARG_SORT_COLUMNS)) {
            return ContentResolver.createSqlSortClause(bundle);
        }
        return string;
    }

    @Override
    public final String getType(Uri uri) {
        try {
            int iMatch = this.mMatcher.match(uri);
            if (iMatch == 2) {
                return DocumentsContract.Root.MIME_TYPE_ITEM;
            }
            if (iMatch != 5 && iMatch != 7) {
                return null;
            }
            enforceTree(uri);
            return getDocumentType(DocumentsContract.getDocumentId(uri));
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed during getType", e);
            return null;
        }
    }

    @Override
    public Uri canonicalize(Uri uri) {
        Context context = getContext();
        if (this.mMatcher.match(uri) == 7) {
            enforceTree(uri);
            Uri uriBuildDocumentUri = DocumentsContract.buildDocumentUri(uri.getAuthority(), DocumentsContract.getDocumentId(uri));
            context.grantUriPermission(getCallingPackage(), uriBuildDocumentUri, getCallingOrSelfUriPermissionModeFlags(context, uri));
            return uriBuildDocumentUri;
        }
        return null;
    }

    private static int getCallingOrSelfUriPermissionModeFlags(Context context, Uri uri) {
        int i = 1;
        if (context.checkCallingOrSelfUriPermission(uri, 1) != 0) {
            i = 0;
        }
        if (context.checkCallingOrSelfUriPermission(uri, 2) == 0) {
            i |= 2;
        }
        if (context.checkCallingOrSelfUriPermission(uri, 65) == 0) {
            return i | 64;
        }
        return i;
    }

    @Override
    public final Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException("Insert not supported");
    }

    @Override
    public final int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException("Delete not supported");
    }

    @Override
    public final int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException("Update not supported");
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) {
        if (!str.startsWith("android:")) {
            return super.call(str, str2, bundle);
        }
        try {
            return callUnchecked(str, str2, bundle);
        } catch (FileNotFoundException e) {
            throw new ParcelableException(e);
        }
    }

    private Bundle callUnchecked(String str, String str2, Bundle bundle) throws FileNotFoundException {
        String treeDocumentId;
        Context context = getContext();
        Bundle bundle2 = new Bundle();
        if (DocumentsContract.METHOD_EJECT_ROOT.equals(str)) {
            Uri uri = (Uri) bundle.getParcelable("uri");
            enforceWritePermissionInner(uri, getCallingPackage(), null);
            ejectRoot(DocumentsContract.getRootId(uri));
            return bundle2;
        }
        Uri uri2 = (Uri) bundle.getParcelable("uri");
        String authority = uri2.getAuthority();
        String documentId = DocumentsContract.getDocumentId(uri2);
        if (!this.mAuthority.equals(authority)) {
            throw new SecurityException("Requested authority " + authority + " doesn't match provider " + this.mAuthority);
        }
        enforceTree(uri2);
        if (DocumentsContract.METHOD_IS_CHILD_DOCUMENT.equals(str)) {
            enforceReadPermissionInner(uri2, getCallingPackage(), null);
            Uri uri3 = (Uri) bundle.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
            bundle2.putBoolean("result", this.mAuthority.equals(uri3.getAuthority()) && isChildDocument(documentId, DocumentsContract.getDocumentId(uri3)));
        } else if (DocumentsContract.METHOD_CREATE_DOCUMENT.equals(str)) {
            enforceWritePermissionInner(uri2, getCallingPackage(), null);
            bundle2.putParcelable("uri", DocumentsContract.buildDocumentUriMaybeUsingTree(uri2, createDocument(documentId, bundle.getString("mime_type"), bundle.getString("_display_name"))));
        } else if (DocumentsContract.METHOD_CREATE_WEB_LINK_INTENT.equals(str)) {
            enforceWritePermissionInner(uri2, getCallingPackage(), null);
            bundle2.putParcelable("result", createWebLinkIntent(documentId, bundle.getBundle(DocumentsContract.EXTRA_OPTIONS)));
        } else if (DocumentsContract.METHOD_RENAME_DOCUMENT.equals(str)) {
            enforceWritePermissionInner(uri2, getCallingPackage(), null);
            String strRenameDocument = renameDocument(documentId, bundle.getString("_display_name"));
            if (strRenameDocument != null) {
                Uri uriBuildDocumentUriMaybeUsingTree = DocumentsContract.buildDocumentUriMaybeUsingTree(uri2, strRenameDocument);
                if (!DocumentsContract.isTreeUri(uriBuildDocumentUriMaybeUsingTree)) {
                    context.grantUriPermission(getCallingPackage(), uriBuildDocumentUriMaybeUsingTree, getCallingOrSelfUriPermissionModeFlags(context, uri2));
                }
                bundle2.putParcelable("uri", uriBuildDocumentUriMaybeUsingTree);
                revokeDocumentPermission(documentId);
            }
        } else if (DocumentsContract.METHOD_DELETE_DOCUMENT.equals(str)) {
            enforceWritePermissionInner(uri2, getCallingPackage(), null);
            deleteDocument(documentId);
            revokeDocumentPermission(documentId);
        } else if (DocumentsContract.METHOD_COPY_DOCUMENT.equals(str)) {
            Uri uri4 = (Uri) bundle.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
            String documentId2 = DocumentsContract.getDocumentId(uri4);
            enforceReadPermissionInner(uri2, getCallingPackage(), null);
            enforceWritePermissionInner(uri4, getCallingPackage(), null);
            String strCopyDocument = copyDocument(documentId, documentId2);
            if (strCopyDocument != null) {
                Uri uriBuildDocumentUriMaybeUsingTree2 = DocumentsContract.buildDocumentUriMaybeUsingTree(uri2, strCopyDocument);
                if (!DocumentsContract.isTreeUri(uriBuildDocumentUriMaybeUsingTree2)) {
                    context.grantUriPermission(getCallingPackage(), uriBuildDocumentUriMaybeUsingTree2, getCallingOrSelfUriPermissionModeFlags(context, uri2));
                }
                bundle2.putParcelable("uri", uriBuildDocumentUriMaybeUsingTree2);
            }
        } else if (DocumentsContract.METHOD_MOVE_DOCUMENT.equals(str)) {
            Uri uri5 = (Uri) bundle.getParcelable(DocumentsContract.EXTRA_PARENT_URI);
            String documentId3 = DocumentsContract.getDocumentId(uri5);
            Uri uri6 = (Uri) bundle.getParcelable(DocumentsContract.EXTRA_TARGET_URI);
            String documentId4 = DocumentsContract.getDocumentId(uri6);
            enforceWritePermissionInner(uri2, getCallingPackage(), null);
            enforceReadPermissionInner(uri5, getCallingPackage(), null);
            enforceWritePermissionInner(uri6, getCallingPackage(), null);
            String strMoveDocument = moveDocument(documentId, documentId3, documentId4);
            if (strMoveDocument != null) {
                Uri uriBuildDocumentUriMaybeUsingTree3 = DocumentsContract.buildDocumentUriMaybeUsingTree(uri2, strMoveDocument);
                if (!DocumentsContract.isTreeUri(uriBuildDocumentUriMaybeUsingTree3)) {
                    context.grantUriPermission(getCallingPackage(), uriBuildDocumentUriMaybeUsingTree3, getCallingOrSelfUriPermissionModeFlags(context, uri2));
                }
                bundle2.putParcelable("uri", uriBuildDocumentUriMaybeUsingTree3);
            }
        } else if (DocumentsContract.METHOD_REMOVE_DOCUMENT.equals(str)) {
            Uri uri7 = (Uri) bundle.getParcelable(DocumentsContract.EXTRA_PARENT_URI);
            String documentId5 = DocumentsContract.getDocumentId(uri7);
            enforceReadPermissionInner(uri7, getCallingPackage(), null);
            enforceWritePermissionInner(uri2, getCallingPackage(), null);
            removeDocument(documentId, documentId5);
        } else if (DocumentsContract.METHOD_FIND_DOCUMENT_PATH.equals(str)) {
            boolean zIsTreeUri = DocumentsContract.isTreeUri(uri2);
            if (zIsTreeUri) {
                enforceReadPermissionInner(uri2, getCallingPackage(), null);
            } else {
                getContext().enforceCallingPermission(Manifest.permission.MANAGE_DOCUMENTS, null);
            }
            if (zIsTreeUri) {
                treeDocumentId = DocumentsContract.getTreeDocumentId(uri2);
            } else {
                treeDocumentId = null;
            }
            DocumentsContract.Path pathFindDocumentPath = findDocumentPath(treeDocumentId, documentId);
            if (zIsTreeUri) {
                if (!Objects.equals(pathFindDocumentPath.getPath().get(0), treeDocumentId)) {
                    Log.wtf(TAG, "Provider doesn't return path from the tree root. Expected: " + treeDocumentId + " found: " + pathFindDocumentPath.getPath().get(0));
                    LinkedList linkedList = new LinkedList(pathFindDocumentPath.getPath());
                    while (linkedList.size() > 1 && !Objects.equals(linkedList.getFirst(), treeDocumentId)) {
                        linkedList.removeFirst();
                    }
                    pathFindDocumentPath = new DocumentsContract.Path(null, linkedList);
                }
                if (pathFindDocumentPath.getRootId() != null) {
                    Log.wtf(TAG, "Provider returns root id :" + pathFindDocumentPath.getRootId() + " unexpectedly. Erase root id.");
                    pathFindDocumentPath = new DocumentsContract.Path(null, pathFindDocumentPath.getPath());
                }
            }
            bundle2.putParcelable("result", pathFindDocumentPath);
        } else {
            if (DocumentsContract.METHOD_GET_DOCUMENT_METADATA.equals(str)) {
                enforceReadPermissionInner(uri2, getCallingPackage(), null);
                return getDocumentMetadata(documentId);
            }
            throw new UnsupportedOperationException("Method not supported " + str);
        }
        return bundle2;
    }

    public final void revokeDocumentPermission(String str) {
        Context context = getContext();
        context.revokeUriPermission(DocumentsContract.buildDocumentUri(this.mAuthority, str), -1);
        context.revokeUriPermission(DocumentsContract.buildTreeDocumentUri(this.mAuthority, str), -1);
    }

    @Override
    public final ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        enforceTree(uri);
        return openDocument(DocumentsContract.getDocumentId(uri), str, null);
    }

    @Override
    public final ParcelFileDescriptor openFile(Uri uri, String str, CancellationSignal cancellationSignal) throws FileNotFoundException {
        enforceTree(uri);
        return openDocument(DocumentsContract.getDocumentId(uri), str, cancellationSignal);
    }

    @Override
    public final AssetFileDescriptor openAssetFile(Uri uri, String str) throws FileNotFoundException {
        enforceTree(uri);
        ParcelFileDescriptor parcelFileDescriptorOpenDocument = openDocument(DocumentsContract.getDocumentId(uri), str, null);
        if (parcelFileDescriptorOpenDocument != null) {
            return new AssetFileDescriptor(parcelFileDescriptorOpenDocument, 0L, -1L);
        }
        return null;
    }

    @Override
    public final AssetFileDescriptor openAssetFile(Uri uri, String str, CancellationSignal cancellationSignal) throws FileNotFoundException {
        enforceTree(uri);
        ParcelFileDescriptor parcelFileDescriptorOpenDocument = openDocument(DocumentsContract.getDocumentId(uri), str, cancellationSignal);
        if (parcelFileDescriptorOpenDocument != null) {
            return new AssetFileDescriptor(parcelFileDescriptorOpenDocument, 0L, -1L);
        }
        return null;
    }

    @Override
    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String str, Bundle bundle) throws FileNotFoundException {
        return openTypedAssetFileImpl(uri, str, bundle, null);
    }

    @Override
    public final AssetFileDescriptor openTypedAssetFile(Uri uri, String str, Bundle bundle, CancellationSignal cancellationSignal) throws FileNotFoundException {
        return openTypedAssetFileImpl(uri, str, bundle, cancellationSignal);
    }

    public String[] getDocumentStreamTypes(String str, String str2) throws Throwable {
        Cursor cursorQueryDocument;
        try {
            cursorQueryDocument = queryDocument(str, null);
            try {
                if (cursorQueryDocument.moveToFirst()) {
                    String string = cursorQueryDocument.getString(cursorQueryDocument.getColumnIndexOrThrow("mime_type"));
                    if ((cursorQueryDocument.getLong(cursorQueryDocument.getColumnIndexOrThrow("flags")) & 512) == 0 && string != null && mimeTypeMatches(str2, string)) {
                        String[] strArr = {string};
                        IoUtils.closeQuietly(cursorQueryDocument);
                        return strArr;
                    }
                }
                IoUtils.closeQuietly(cursorQueryDocument);
                return null;
            } catch (FileNotFoundException e) {
                IoUtils.closeQuietly(cursorQueryDocument);
                return null;
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(cursorQueryDocument);
                throw th;
            }
        } catch (FileNotFoundException e2) {
            cursorQueryDocument = null;
        } catch (Throwable th2) {
            th = th2;
            cursorQueryDocument = null;
        }
    }

    @Override
    public String[] getStreamTypes(Uri uri, String str) {
        enforceTree(uri);
        return getDocumentStreamTypes(DocumentsContract.getDocumentId(uri), str);
    }

    private final AssetFileDescriptor openTypedAssetFileImpl(Uri uri, String str, Bundle bundle, CancellationSignal cancellationSignal) throws FileNotFoundException {
        enforceTree(uri);
        String documentId = DocumentsContract.getDocumentId(uri);
        if (bundle != null && bundle.containsKey(ContentResolver.EXTRA_SIZE)) {
            return openDocumentThumbnail(documentId, (Point) bundle.getParcelable(ContentResolver.EXTRA_SIZE), cancellationSignal);
        }
        if ("*/*".equals(str)) {
            return openAssetFile(uri, FullBackup.ROOT_TREE_TOKEN);
        }
        String type = getType(uri);
        if (type != null && ClipDescription.compareMimeTypes(type, str)) {
            return openAssetFile(uri, FullBackup.ROOT_TREE_TOKEN);
        }
        return openTypedDocument(documentId, str, bundle, cancellationSignal);
    }

    public static boolean mimeTypeMatches(String str, String str2) {
        if (str2 == null) {
            return false;
        }
        if (str == null || "*/*".equals(str) || str.equals(str2)) {
            return true;
        }
        if (str.endsWith("/*")) {
            return str.regionMatches(0, str2, 0, str.indexOf(47));
        }
        return false;
    }
}
