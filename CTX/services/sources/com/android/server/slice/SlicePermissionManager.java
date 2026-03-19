package com.android.server.slice;

import android.content.ContentProvider;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.XmlUtils;
import com.android.server.slice.DirtyTracker;
import com.android.server.slice.SliceProviderPermissions;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public class SlicePermissionManager implements DirtyTracker {
    static final int DB_VERSION = 2;
    private static final long PERMISSION_CACHE_PERIOD = 300000;
    private static final String SLICE_DIR = "slice";
    private static final String TAG = "SlicePermissionManager";
    private static final String TAG_LIST = "slice-access-list";
    private static final long WRITE_GRACE_PERIOD = 500;
    private final String ATT_VERSION;
    private final ArrayMap<PkgUser, SliceClientPermissions> mCachedClients;
    private final ArrayMap<PkgUser, SliceProviderPermissions> mCachedProviders;
    private final Context mContext;
    private final ArraySet<DirtyTracker.Persistable> mDirty;
    private final Handler mHandler;
    private final File mSliceDir;

    @VisibleForTesting
    SlicePermissionManager(Context context, Looper looper, File file) {
        this.ATT_VERSION = "version";
        this.mCachedProviders = new ArrayMap<>();
        this.mCachedClients = new ArrayMap<>();
        this.mDirty = new ArraySet<>();
        this.mContext = context;
        this.mHandler = new H(looper);
        this.mSliceDir = file;
    }

    public SlicePermissionManager(Context context, Looper looper) {
        this(context, looper, new File(Environment.getDataDirectory(), "system/slice"));
    }

    public void grantFullAccess(String str, int i) {
        getClient(new PkgUser(str, i)).setHasFullAccess(true);
    }

    public void grantSliceAccess(String str, int i, String str2, int i2, Uri uri) {
        PkgUser pkgUser = new PkgUser(str, i);
        PkgUser pkgUser2 = new PkgUser(str2, i2);
        getClient(pkgUser).grantUri(uri, pkgUser2);
        getProvider(pkgUser2).getOrCreateAuthority(ContentProvider.getUriWithoutUserId(uri).getAuthority()).addPkg(pkgUser);
    }

    public void revokeSliceAccess(String str, int i, String str2, int i2, Uri uri) {
        PkgUser pkgUser = new PkgUser(str, i);
        getClient(pkgUser).revokeUri(uri, new PkgUser(str2, i2));
    }

    public void removePkg(String str, int i) {
        PkgUser pkgUser = new PkgUser(str, i);
        for (SliceProviderPermissions.SliceAuthority sliceAuthority : getProvider(pkgUser).getAuthorities()) {
            Iterator<PkgUser> it = sliceAuthority.getPkgs().iterator();
            while (it.hasNext()) {
                getClient(it.next()).removeAuthority(sliceAuthority.getAuthority(), i);
            }
        }
        getClient(pkgUser).clear();
        this.mHandler.obtainMessage(3, pkgUser);
    }

    public String[] getAllPackagesGranted(String str) {
        ArraySet arraySet = new ArraySet();
        Iterator<SliceProviderPermissions.SliceAuthority> it = getProvider(new PkgUser(str, 0)).getAuthorities().iterator();
        while (it.hasNext()) {
            Iterator<PkgUser> it2 = it.next().getPkgs().iterator();
            while (it2.hasNext()) {
                arraySet.add(it2.next().mPkg);
            }
        }
        return (String[]) arraySet.toArray(new String[arraySet.size()]);
    }

    public boolean hasFullAccess(String str, int i) {
        return getClient(new PkgUser(str, i)).hasFullAccess();
    }

    public boolean hasPermission(String str, int i, Uri uri) throws Exception {
        SliceClientPermissions client = getClient(new PkgUser(str, i));
        return client.hasFullAccess() || client.hasPermission(ContentProvider.getUriWithoutUserId(uri), ContentProvider.getUserIdFromUri(uri, i));
    }

    @Override
    public void onPersistableDirty(DirtyTracker.Persistable persistable) {
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(1, persistable).sendToTarget();
        this.mHandler.sendEmptyMessageDelayed(2, 500L);
    }

    public void writeBackup(XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
        synchronized (this) {
            Throwable th = null;
            xmlSerializer.startTag(null, TAG_LIST);
            xmlSerializer.attribute(null, "version", String.valueOf(2));
            $$Lambda$SlicePermissionManager$y3Tun5dTftw8s8sky62syeWR34U __lambda_slicepermissionmanager_y3tun5dtftw8s8sky62syewr34u = new DirtyTracker() {
                @Override
                public final void onPersistableDirty(DirtyTracker.Persistable persistable) {
                    SlicePermissionManager.lambda$writeBackup$0(persistable);
                }
            };
            if (this.mHandler.hasMessages(2)) {
                this.mHandler.removeMessages(2);
                handlePersist();
            }
            for (String str : new File(this.mSliceDir.getAbsolutePath()).list()) {
                if (!str.isEmpty()) {
                    ParserHolder parser = getParser(str);
                    while (parser.parser.getEventType() != 2) {
                        try {
                            try {
                                parser.parser.next();
                            } finally {
                            }
                        } finally {
                            if (parser != null) {
                                $closeResource(th, parser);
                            }
                        }
                    }
                    ("client".equals(parser.parser.getName()) ? SliceClientPermissions.createFrom(parser.parser, __lambda_slicepermissionmanager_y3tun5dtftw8s8sky62syewr34u) : SliceProviderPermissions.createFrom(parser.parser, __lambda_slicepermissionmanager_y3tun5dtftw8s8sky62syewr34u)).writeTo(xmlSerializer);
                }
            }
            xmlSerializer.endTag(null, TAG_LIST);
        }
    }

    static void lambda$writeBackup$0(DirtyTracker.Persistable persistable) {
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public void readRestore(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        synchronized (this) {
            while (true) {
                if ((xmlPullParser.getEventType() == 2 && TAG_LIST.equals(xmlPullParser.getName())) || xmlPullParser.getEventType() == 1) {
                    break;
                } else {
                    xmlPullParser.next();
                }
            }
            if (XmlUtils.readIntAttribute(xmlPullParser, "version", 0) < 2) {
                return;
            }
            while (xmlPullParser.getEventType() != 1) {
                if (xmlPullParser.getEventType() == 2) {
                    if ("client".equals(xmlPullParser.getName())) {
                        SliceClientPermissions sliceClientPermissionsCreateFrom = SliceClientPermissions.createFrom(xmlPullParser, this);
                        synchronized (this.mCachedClients) {
                            this.mCachedClients.put(sliceClientPermissionsCreateFrom.getPkg(), sliceClientPermissionsCreateFrom);
                        }
                        onPersistableDirty(sliceClientPermissionsCreateFrom);
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4, sliceClientPermissionsCreateFrom.getPkg()), 300000L);
                    } else if ("provider".equals(xmlPullParser.getName())) {
                        SliceProviderPermissions sliceProviderPermissionsCreateFrom = SliceProviderPermissions.createFrom(xmlPullParser, this);
                        synchronized (this.mCachedProviders) {
                            this.mCachedProviders.put(sliceProviderPermissionsCreateFrom.getPkg(), sliceProviderPermissionsCreateFrom);
                        }
                        onPersistableDirty(sliceProviderPermissionsCreateFrom);
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, sliceProviderPermissionsCreateFrom.getPkg()), 300000L);
                    } else {
                        xmlPullParser.next();
                    }
                } else {
                    xmlPullParser.next();
                }
            }
        }
    }

    private SliceClientPermissions getClient(PkgUser pkgUser) throws Exception {
        SliceClientPermissions sliceClientPermissions;
        synchronized (this.mCachedClients) {
            sliceClientPermissions = this.mCachedClients.get(pkgUser);
        }
        if (sliceClientPermissions == null) {
            try {
                ParserHolder parser = getParser(SliceClientPermissions.getFileName(pkgUser));
                try {
                    SliceClientPermissions sliceClientPermissionsCreateFrom = SliceClientPermissions.createFrom(parser.parser, this);
                    synchronized (this.mCachedClients) {
                        this.mCachedClients.put(pkgUser, sliceClientPermissionsCreateFrom);
                    }
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(4, pkgUser), 300000L);
                    return sliceClientPermissionsCreateFrom;
                } finally {
                    if (parser != null) {
                        $closeResource(null, parser);
                    }
                }
            } catch (FileNotFoundException e) {
                sliceClientPermissions = new SliceClientPermissions(pkgUser, this);
                synchronized (this.mCachedClients) {
                    this.mCachedClients.put(pkgUser, sliceClientPermissions);
                }
                return sliceClientPermissions;
            } catch (IOException e2) {
                Log.e(TAG, "Can't read client", e2);
                sliceClientPermissions = new SliceClientPermissions(pkgUser, this);
                synchronized (this.mCachedClients) {
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "Can't read client", e3);
                sliceClientPermissions = new SliceClientPermissions(pkgUser, this);
                synchronized (this.mCachedClients) {
                }
            }
        }
        return sliceClientPermissions;
    }

    private SliceProviderPermissions getProvider(PkgUser pkgUser) throws Exception {
        SliceProviderPermissions sliceProviderPermissions;
        synchronized (this.mCachedProviders) {
            sliceProviderPermissions = this.mCachedProviders.get(pkgUser);
        }
        if (sliceProviderPermissions == null) {
            try {
                ParserHolder parser = getParser(SliceProviderPermissions.getFileName(pkgUser));
                try {
                    SliceProviderPermissions sliceProviderPermissionsCreateFrom = SliceProviderPermissions.createFrom(parser.parser, this);
                    synchronized (this.mCachedProviders) {
                        this.mCachedProviders.put(pkgUser, sliceProviderPermissionsCreateFrom);
                    }
                    this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(5, pkgUser), 300000L);
                    return sliceProviderPermissionsCreateFrom;
                } finally {
                    if (parser != null) {
                        $closeResource(null, parser);
                    }
                }
            } catch (FileNotFoundException e) {
                sliceProviderPermissions = new SliceProviderPermissions(pkgUser, this);
                synchronized (this.mCachedProviders) {
                    this.mCachedProviders.put(pkgUser, sliceProviderPermissions);
                }
                return sliceProviderPermissions;
            } catch (IOException e2) {
                Log.e(TAG, "Can't read provider", e2);
                sliceProviderPermissions = new SliceProviderPermissions(pkgUser, this);
                synchronized (this.mCachedProviders) {
                }
            } catch (XmlPullParserException e3) {
                Log.e(TAG, "Can't read provider", e3);
                sliceProviderPermissions = new SliceProviderPermissions(pkgUser, this);
                synchronized (this.mCachedProviders) {
                }
            }
        }
        return sliceProviderPermissions;
    }

    private ParserHolder getParser(String str) throws XmlPullParserException, FileNotFoundException {
        AtomicFile file = getFile(str);
        ParserHolder parserHolder = new ParserHolder();
        parserHolder.input = file.openRead();
        parserHolder.parser = XmlPullParserFactory.newInstance().newPullParser();
        parserHolder.parser.setInput(parserHolder.input, Xml.Encoding.UTF_8.name());
        return parserHolder;
    }

    private AtomicFile getFile(String str) {
        if (!this.mSliceDir.exists()) {
            this.mSliceDir.mkdir();
        }
        return new AtomicFile(new File(this.mSliceDir, str));
    }

    private void handlePersist() {
        synchronized (this) {
            for (DirtyTracker.Persistable persistable : this.mDirty) {
                AtomicFile file = getFile(persistable.getFileName());
                try {
                    FileOutputStream fileOutputStreamStartWrite = file.startWrite();
                    try {
                        XmlSerializer xmlSerializerNewSerializer = XmlPullParserFactory.newInstance().newSerializer();
                        xmlSerializerNewSerializer.setOutput(fileOutputStreamStartWrite, Xml.Encoding.UTF_8.name());
                        persistable.writeTo(xmlSerializerNewSerializer);
                        xmlSerializerNewSerializer.flush();
                        file.finishWrite(fileOutputStreamStartWrite);
                    } catch (IOException | XmlPullParserException e) {
                        Slog.w(TAG, "Failed to save access file, restoring backup", e);
                        file.failWrite(fileOutputStreamStartWrite);
                    }
                } catch (IOException e2) {
                    Slog.w(TAG, "Failed to save access file", e2);
                    return;
                }
            }
            this.mDirty.clear();
        }
    }

    private void handleRemove(PkgUser pkgUser) {
        getFile(SliceClientPermissions.getFileName(pkgUser)).delete();
        getFile(SliceProviderPermissions.getFileName(pkgUser)).delete();
        this.mDirty.remove(this.mCachedClients.remove(pkgUser));
        this.mDirty.remove(this.mCachedProviders.remove(pkgUser));
    }

    private final class H extends Handler {
        private static final int MSG_ADD_DIRTY = 1;
        private static final int MSG_CLEAR_CLIENT = 4;
        private static final int MSG_CLEAR_PROVIDER = 5;
        private static final int MSG_PERSIST = 2;
        private static final int MSG_REMOVE = 3;

        public H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SlicePermissionManager.this.mDirty.add((DirtyTracker.Persistable) message.obj);
                    return;
                case 2:
                    SlicePermissionManager.this.handlePersist();
                    return;
                case 3:
                    SlicePermissionManager.this.handleRemove((PkgUser) message.obj);
                    return;
                case 4:
                    synchronized (SlicePermissionManager.this.mCachedClients) {
                        SlicePermissionManager.this.mCachedClients.remove(message.obj);
                        break;
                    }
                    return;
                case 5:
                    synchronized (SlicePermissionManager.this.mCachedProviders) {
                        SlicePermissionManager.this.mCachedProviders.remove(message.obj);
                        break;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public static class PkgUser {
        private static final String FORMAT = "%s@%d";
        private static final String SEPARATOR = "@";
        private final String mPkg;
        private final int mUserId;

        public PkgUser(String str, int i) {
            this.mPkg = str;
            this.mUserId = i;
        }

        public PkgUser(String str) throws IllegalArgumentException {
            try {
                String[] strArrSplit = str.split(SEPARATOR, 2);
                this.mPkg = strArrSplit[0];
                this.mUserId = Integer.parseInt(strArrSplit[1]);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        public String getPkg() {
            return this.mPkg;
        }

        public int getUserId() {
            return this.mUserId;
        }

        public int hashCode() {
            return this.mPkg.hashCode() + this.mUserId;
        }

        public boolean equals(Object obj) {
            if (!getClass().equals(obj != null ? obj.getClass() : null)) {
                return false;
            }
            PkgUser pkgUser = (PkgUser) obj;
            return Objects.equals(pkgUser.mPkg, this.mPkg) && pkgUser.mUserId == this.mUserId;
        }

        public String toString() {
            return String.format(FORMAT, this.mPkg, Integer.valueOf(this.mUserId));
        }
    }

    private class ParserHolder implements AutoCloseable {
        private InputStream input;
        private XmlPullParser parser;

        private ParserHolder() {
        }

        @Override
        public void close() throws IOException {
            this.input.close();
        }
    }
}
