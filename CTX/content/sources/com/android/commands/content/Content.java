package com.android.commands.content;

import android.app.ActivityManager;
import android.app.ContentProviderHolder;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.text.TextUtils;
import java.io.FileDescriptor;

public class Content {
    private static final String USAGE = "usage: adb shell content [subcommand] [options]\n\nusage: adb shell content insert --uri <URI> [--user <USER_ID>] --bind <BINDING> [--bind <BINDING>...]\n  <URI> a content provider URI.\n  <BINDING> binds a typed value to a column and is formatted:\n  <COLUMN_NAME>:<TYPE>:<COLUMN_VALUE> where:\n  <TYPE> specifies data type such as:\n  b - boolean, s - string, i - integer, l - long, f - float, d - double\n  Note: Omit the value for passing an empty string, e.g column:s:\n  Example:\n  # Add \"new_setting\" secure setting with value \"new_value\".\n  adb shell content insert --uri content://settings/secure --bind name:s:new_setting --bind value:s:new_value\n\nusage: adb shell content update --uri <URI> [--user <USER_ID>] [--where <WHERE>]\n  <WHERE> is a SQL style where clause in quotes (You have to escape single quotes - see example below).\n  Example:\n  # Change \"new_setting\" secure setting to \"newer_value\".\n  adb shell content update --uri content://settings/secure --bind value:s:newer_value --where \"name='new_setting'\"\n\nusage: adb shell content delete --uri <URI> [--user <USER_ID>] --bind <BINDING> [--bind <BINDING>...] [--where <WHERE>]\n  Example:\n  # Remove \"new_setting\" secure setting.\n  adb shell content delete --uri content://settings/secure --where \"name='new_setting'\"\n\nusage: adb shell content query --uri <URI> [--user <USER_ID>] [--projection <PROJECTION>] [--where <WHERE>] [--sort <SORT_ORDER>]\n  <PROJECTION> is a list of colon separated column names and is formatted:\n  <COLUMN_NAME>[:<COLUMN_NAME>...]\n  <SORT_ORDER> is the order in which rows in the result should be sorted.\n  Example:\n  # Select \"name\" and \"value\" columns from secure settings where \"name\" is equal to \"new_setting\" and sort the result by name in ascending order.\n  adb shell content query --uri content://settings/secure --projection name:value --where \"name='new_setting'\" --sort \"name ASC\"\n\nusage: adb shell content call --uri <URI> --method <METHOD> [--arg <ARG>]\n       [--extra <BINDING> ...]\n  <METHOD> is the name of a provider-defined method\n  <ARG> is an optional string argument\n  <BINDING> is like --bind above, typed data of the form <KEY>:{b,s,i,l,f,d}:<VAL>\n\nusage: adb shell content read --uri <URI> [--user <USER_ID>]\n  Example:\n  adb shell 'content read --uri content://settings/system/ringtone_cache' > host.ogg\n\nusage: adb shell content write --uri <URI> [--user <USER_ID>]\n  Example:\n  adb shell 'content write --uri content://settings/system/ringtone_cache' < host.ogg\n\nusage: adb shell content gettype --uri <URI> [--user <USER_ID>]\n  Example:\n  adb shell content gettype --uri content://media/internal/audio/media/\n\n";

    private static class Parser {
        private static final String ARGUMENT_ARG = "--arg";
        private static final String ARGUMENT_BIND = "--bind";
        private static final String ARGUMENT_CALL = "call";
        private static final String ARGUMENT_DELETE = "delete";
        private static final String ARGUMENT_EXTRA = "--extra";
        private static final String ARGUMENT_GET_TYPE = "gettype";
        private static final String ARGUMENT_INSERT = "insert";
        private static final String ARGUMENT_METHOD = "--method";
        private static final String ARGUMENT_PREFIX = "--";
        private static final String ARGUMENT_PROJECTION = "--projection";
        private static final String ARGUMENT_QUERY = "query";
        private static final String ARGUMENT_READ = "read";
        private static final String ARGUMENT_SORT = "--sort";
        private static final String ARGUMENT_UPDATE = "update";
        private static final String ARGUMENT_URI = "--uri";
        private static final String ARGUMENT_USER = "--user";
        private static final String ARGUMENT_WHERE = "--where";
        private static final String ARGUMENT_WRITE = "write";
        private static final String COLON = ":";
        private static final String TYPE_BOOLEAN = "b";
        private static final String TYPE_DOUBLE = "d";
        private static final String TYPE_FLOAT = "f";
        private static final String TYPE_INTEGER = "i";
        private static final String TYPE_LONG = "l";
        private static final String TYPE_STRING = "s";
        private final Tokenizer mTokenizer;

        public Parser(String[] strArr) {
            this.mTokenizer = new Tokenizer(strArr);
        }

        public Command parseCommand() {
            try {
                String strNextArg = this.mTokenizer.nextArg();
                if (ARGUMENT_INSERT.equals(strNextArg)) {
                    return parseInsertCommand();
                }
                if (ARGUMENT_DELETE.equals(strNextArg)) {
                    return parseDeleteCommand();
                }
                if (ARGUMENT_UPDATE.equals(strNextArg)) {
                    return parseUpdateCommand();
                }
                if (ARGUMENT_QUERY.equals(strNextArg)) {
                    return parseQueryCommand();
                }
                if (ARGUMENT_CALL.equals(strNextArg)) {
                    return parseCallCommand();
                }
                if (ARGUMENT_READ.equals(strNextArg)) {
                    return parseReadCommand();
                }
                if (ARGUMENT_WRITE.equals(strNextArg)) {
                    return parseWriteCommand();
                }
                if (ARGUMENT_GET_TYPE.equals(strNextArg)) {
                    return parseGetTypeCommand();
                }
                throw new IllegalArgumentException("Unsupported operation: " + strNextArg);
            } catch (IllegalArgumentException e) {
                System.out.println(Content.USAGE);
                System.out.println("[ERROR] " + e.getMessage());
                return null;
            }
        }

        private InsertCommand parseInsertCommand() {
            ContentValues contentValues = new ContentValues();
            Uri uri = null;
            int i = 0;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_BIND.equals(strNextArg)) {
                        parseBindValue(contentValues);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    if (contentValues.size() == 0) {
                        throw new IllegalArgumentException("Bindings not specified. Did you specify --bind argument(s)?");
                    }
                    return new InsertCommand(uri, i, contentValues);
                }
            }
        }

        private DeleteCommand parseDeleteCommand() {
            Uri uri = null;
            int i = 0;
            String strArgumentValueRequired = null;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_WHERE.equals(strNextArg)) {
                        strArgumentValueRequired = argumentValueRequired(strNextArg);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new DeleteCommand(uri, i, strArgumentValueRequired);
                }
            }
        }

        private UpdateCommand parseUpdateCommand() {
            ContentValues contentValues = new ContentValues();
            Uri uri = null;
            int i = 0;
            String strArgumentValueRequired = null;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_WHERE.equals(strNextArg)) {
                        strArgumentValueRequired = argumentValueRequired(strNextArg);
                    } else if (ARGUMENT_BIND.equals(strNextArg)) {
                        parseBindValue(contentValues);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    if (contentValues.size() == 0) {
                        throw new IllegalArgumentException("Bindings not specified. Did you specify --bind argument(s)?");
                    }
                    return new UpdateCommand(uri, i, contentValues, strArgumentValueRequired);
                }
            }
        }

        public CallCommand parseCallCommand() {
            ContentValues contentValues = new ContentValues();
            String strArgumentValueRequired = null;
            String strArgumentValueRequired2 = null;
            int i = 0;
            Uri uri = null;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_METHOD.equals(strNextArg)) {
                        strArgumentValueRequired = argumentValueRequired(strNextArg);
                    } else if (ARGUMENT_ARG.equals(strNextArg)) {
                        strArgumentValueRequired2 = argumentValueRequired(strNextArg);
                    } else if (ARGUMENT_EXTRA.equals(strNextArg)) {
                        parseBindValue(contentValues);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    if (strArgumentValueRequired == null) {
                        throw new IllegalArgumentException("Content provider method not specified.");
                    }
                    return new CallCommand(uri, i, strArgumentValueRequired, strArgumentValueRequired2, contentValues);
                }
            }
        }

        private GetTypeCommand parseGetTypeCommand() {
            Uri uri = null;
            int i = 0;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new GetTypeCommand(uri, i);
                }
            }
        }

        private ReadCommand parseReadCommand() {
            Uri uri = null;
            int i = 0;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new ReadCommand(uri, i);
                }
            }
        }

        private WriteCommand parseWriteCommand() {
            Uri uri = null;
            int i = 0;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new WriteCommand(uri, i);
                }
            }
        }

        public QueryCommand parseQueryCommand() {
            Uri uri = null;
            String[] strArrSplit = null;
            String strArgumentValueRequired = null;
            String strArgumentValueRequired2 = null;
            int i = 0;
            while (true) {
                String strNextArg = this.mTokenizer.nextArg();
                if (strNextArg != null) {
                    if (ARGUMENT_URI.equals(strNextArg)) {
                        uri = Uri.parse(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_USER.equals(strNextArg)) {
                        i = Integer.parseInt(argumentValueRequired(strNextArg));
                    } else if (ARGUMENT_WHERE.equals(strNextArg)) {
                        strArgumentValueRequired = argumentValueRequired(strNextArg);
                    } else if (ARGUMENT_SORT.equals(strNextArg)) {
                        strArgumentValueRequired2 = argumentValueRequired(strNextArg);
                    } else if (ARGUMENT_PROJECTION.equals(strNextArg)) {
                        strArrSplit = argumentValueRequired(strNextArg).split("[\\s]*:[\\s]*");
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + strNextArg);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new QueryCommand(uri, i, strArrSplit, strArgumentValueRequired, strArgumentValueRequired2);
                }
            }
        }

        private void parseBindValue(ContentValues contentValues) {
            String strNextArg = this.mTokenizer.nextArg();
            if (TextUtils.isEmpty(strNextArg)) {
                throw new IllegalArgumentException("Binding not well formed: " + strNextArg);
            }
            int iIndexOf = strNextArg.indexOf(COLON);
            if (iIndexOf < 0) {
                throw new IllegalArgumentException("Binding not well formed: " + strNextArg);
            }
            int i = iIndexOf + 1;
            int iIndexOf2 = strNextArg.indexOf(COLON, i);
            if (iIndexOf2 < 0) {
                throw new IllegalArgumentException("Binding not well formed: " + strNextArg);
            }
            String strSubstring = strNextArg.substring(0, iIndexOf);
            String strSubstring2 = strNextArg.substring(i, iIndexOf2);
            String strSubstring3 = strNextArg.substring(iIndexOf2 + 1);
            if (TYPE_STRING.equals(strSubstring2)) {
                contentValues.put(strSubstring, strSubstring3);
                return;
            }
            if (TYPE_BOOLEAN.equalsIgnoreCase(strSubstring2)) {
                contentValues.put(strSubstring, Boolean.valueOf(Boolean.parseBoolean(strSubstring3)));
                return;
            }
            if (TYPE_INTEGER.equalsIgnoreCase(strSubstring2) || TYPE_LONG.equalsIgnoreCase(strSubstring2)) {
                contentValues.put(strSubstring, Long.valueOf(Long.parseLong(strSubstring3)));
                return;
            }
            if (TYPE_FLOAT.equalsIgnoreCase(strSubstring2) || TYPE_DOUBLE.equalsIgnoreCase(strSubstring2)) {
                contentValues.put(strSubstring, Double.valueOf(Double.parseDouble(strSubstring3)));
                return;
            }
            throw new IllegalArgumentException("Unsupported type: " + strSubstring2);
        }

        private String argumentValueRequired(String str) {
            String strNextArg = this.mTokenizer.nextArg();
            if (TextUtils.isEmpty(strNextArg) || strNextArg.startsWith(ARGUMENT_PREFIX)) {
                throw new IllegalArgumentException("No value for argument: " + str);
            }
            return strNextArg;
        }
    }

    private static class Tokenizer {
        private final String[] mArgs;
        private int mNextArg;

        public Tokenizer(String[] strArr) {
            this.mArgs = strArr;
        }

        private String nextArg() {
            if (this.mNextArg < this.mArgs.length) {
                String[] strArr = this.mArgs;
                int i = this.mNextArg;
                this.mNextArg = i + 1;
                return strArr[i];
            }
            return null;
        }
    }

    private static abstract class Command {
        final Uri mUri;
        final int mUserId;

        protected abstract void onExecute(IContentProvider iContentProvider) throws Exception;

        public Command(Uri uri, int i) {
            this.mUri = uri;
            this.mUserId = i;
        }

        public final void execute() throws Throwable {
            String authority = this.mUri.getAuthority();
            try {
                IActivityManager service = ActivityManager.getService();
                IContentProvider iContentProvider = null;
                Binder binder = new Binder();
                try {
                    ContentProviderHolder contentProviderExternal = service.getContentProviderExternal(authority, this.mUserId, binder);
                    if (contentProviderExternal == null) {
                        throw new IllegalStateException("Could not find provider: " + authority);
                    }
                    IContentProvider iContentProvider2 = contentProviderExternal.provider;
                    try {
                        onExecute(iContentProvider2);
                        if (iContentProvider2 != null) {
                            service.removeContentProviderExternal(authority, binder);
                        }
                    } catch (Throwable th) {
                        th = th;
                        iContentProvider = iContentProvider2;
                        if (iContentProvider != null) {
                            service.removeContentProviderExternal(authority, binder);
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Exception e) {
                System.err.println("Error while accessing provider:" + authority);
                e.printStackTrace();
            }
        }

        public static String resolveCallingPackage() {
            int iMyUid = Process.myUid();
            if (iMyUid == 0) {
                return "root";
            }
            if (iMyUid == 2000) {
                return "com.android.shell";
            }
            return null;
        }
    }

    private static class InsertCommand extends Command {
        final ContentValues mContentValues;

        public InsertCommand(Uri uri, int i, ContentValues contentValues) {
            super(uri, i);
            this.mContentValues = contentValues;
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            iContentProvider.insert(resolveCallingPackage(), this.mUri, this.mContentValues);
        }
    }

    private static class DeleteCommand extends Command {
        final String mWhere;

        public DeleteCommand(Uri uri, int i, String str) {
            super(uri, i);
            this.mWhere = str;
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            iContentProvider.delete(resolveCallingPackage(), this.mUri, this.mWhere, (String[]) null);
        }
    }

    private static class CallCommand extends Command {
        final String mArg;
        Bundle mExtras;
        final String mMethod;

        public CallCommand(Uri uri, int i, String str, String str2, ContentValues contentValues) {
            super(uri, i);
            this.mExtras = null;
            this.mMethod = str;
            this.mArg = str2;
            if (contentValues != null) {
                this.mExtras = new Bundle();
                for (String str3 : contentValues.keySet()) {
                    ?? r5 = contentValues.get(str3);
                    if (r5 instanceof String) {
                        this.mExtras.putString(str3, r5);
                    } else if (r5 instanceof Float) {
                        this.mExtras.putFloat(str3, r5.floatValue());
                    } else if (r5 instanceof Double) {
                        this.mExtras.putDouble(str3, r5.doubleValue());
                    } else if (r5 instanceof Boolean) {
                        this.mExtras.putBoolean(str3, r5.booleanValue());
                    } else if (r5 instanceof Integer) {
                        this.mExtras.putInt(str3, r5.intValue());
                    } else if (r5 instanceof Long) {
                        this.mExtras.putLong(str3, r5.longValue());
                    }
                }
            }
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            Bundle bundleCall = iContentProvider.call((String) null, this.mMethod, this.mArg, this.mExtras);
            if (bundleCall != null) {
                bundleCall.size();
            }
            System.out.println("Result: " + bundleCall);
        }
    }

    private static class GetTypeCommand extends Command {
        public GetTypeCommand(Uri uri, int i) {
            super(uri, i);
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            String type = iContentProvider.getType(this.mUri);
            System.out.println("Result: " + type);
        }
    }

    private static class ReadCommand extends Command {
        public ReadCommand(Uri uri, int i) {
            super(uri, i);
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            ParcelFileDescriptor parcelFileDescriptorOpenFile = iContentProvider.openFile((String) null, this.mUri, "r", (ICancellationSignal) null, (IBinder) null);
            Throwable th = null;
            try {
                FileUtils.copy(parcelFileDescriptorOpenFile.getFileDescriptor(), FileDescriptor.out);
                if (parcelFileDescriptorOpenFile != null) {
                    parcelFileDescriptorOpenFile.close();
                }
            } catch (Throwable th2) {
                if (parcelFileDescriptorOpenFile != null) {
                    if (th != null) {
                        try {
                            parcelFileDescriptorOpenFile.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        parcelFileDescriptorOpenFile.close();
                    }
                }
                throw th2;
            }
        }
    }

    private static class WriteCommand extends Command {
        public WriteCommand(Uri uri, int i) {
            super(uri, i);
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            ParcelFileDescriptor parcelFileDescriptorOpenFile = iContentProvider.openFile((String) null, this.mUri, "w", (ICancellationSignal) null, (IBinder) null);
            Throwable th = null;
            try {
                FileUtils.copy(FileDescriptor.in, parcelFileDescriptorOpenFile.getFileDescriptor());
                if (parcelFileDescriptorOpenFile != null) {
                    parcelFileDescriptorOpenFile.close();
                }
            } catch (Throwable th2) {
                if (parcelFileDescriptorOpenFile != null) {
                    if (th != null) {
                        try {
                            parcelFileDescriptorOpenFile.close();
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                        }
                    } else {
                        parcelFileDescriptorOpenFile.close();
                    }
                }
                throw th2;
            }
        }
    }

    private static class QueryCommand extends DeleteCommand {
        final String[] mProjection;
        final String mSortOrder;

        public QueryCommand(Uri uri, int i, String[] strArr, String str, String str2) {
            super(uri, i, str);
            this.mProjection = strArr;
            this.mSortOrder = str2;
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            String strValueOf;
            Cursor cursorQuery = iContentProvider.query(resolveCallingPackage(), this.mUri, this.mProjection, ContentResolver.createSqlQueryBundle(this.mWhere, null, this.mSortOrder), (ICancellationSignal) null);
            if (cursorQuery == null) {
                System.out.println("No result found.");
                return;
            }
            try {
                if (cursorQuery.moveToFirst()) {
                    StringBuilder sb = new StringBuilder();
                    int i = 0;
                    do {
                        sb.setLength(0);
                        sb.append("Row: ");
                        sb.append(i);
                        sb.append(" ");
                        i++;
                        int columnCount = cursorQuery.getColumnCount();
                        for (int i2 = 0; i2 < columnCount; i2++) {
                            if (i2 > 0) {
                                sb.append(", ");
                            }
                            String columnName = cursorQuery.getColumnName(i2);
                            int columnIndex = cursorQuery.getColumnIndex(columnName);
                            switch (cursorQuery.getType(columnIndex)) {
                                case 0:
                                    strValueOf = "NULL";
                                    break;
                                case 1:
                                    strValueOf = String.valueOf(cursorQuery.getLong(columnIndex));
                                    break;
                                case 2:
                                    strValueOf = String.valueOf(cursorQuery.getFloat(columnIndex));
                                    break;
                                case 3:
                                    strValueOf = cursorQuery.getString(columnIndex);
                                    break;
                                case 4:
                                    strValueOf = "BLOB";
                                    break;
                                default:
                                    strValueOf = null;
                                    break;
                            }
                            sb.append(columnName);
                            sb.append("=");
                            sb.append(strValueOf);
                        }
                        System.out.println(sb);
                    } while (cursorQuery.moveToNext());
                } else {
                    System.out.println("No result found.");
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private static class UpdateCommand extends InsertCommand {
        final String mWhere;

        public UpdateCommand(Uri uri, int i, ContentValues contentValues, String str) {
            super(uri, i, contentValues);
            this.mWhere = str;
        }

        @Override
        public void onExecute(IContentProvider iContentProvider) throws Exception {
            iContentProvider.update(resolveCallingPackage(), this.mUri, this.mContentValues, this.mWhere, (String[]) null);
        }
    }

    public static void main(String[] strArr) throws Throwable {
        Command command = new Parser(strArr).parseCommand();
        if (command != null) {
            command.execute();
        }
    }
}
