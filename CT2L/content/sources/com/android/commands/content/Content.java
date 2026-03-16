package com.android.commands.content;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.ContentValues;
import android.content.IContentProvider;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ICancellationSignal;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import libcore.io.IoUtils;

public class Content {
    private static final String USAGE = "usage: adb shell content [subcommand] [options]\n\nusage: adb shell content insert --uri <URI> [--user <USER_ID>] --bind <BINDING> [--bind <BINDING>...]\n  <URI> a content provider URI.\n  <BINDING> binds a typed value to a column and is formatted:\n  <COLUMN_NAME>:<TYPE>:<COLUMN_VALUE> where:\n  <TYPE> specifies data type such as:\n  b - boolean, s - string, i - integer, l - long, f - float, d - double\n  Note: Omit the value for passing an empty string, e.g column:s:\n  Example:\n  # Add \"new_setting\" secure setting with value \"new_value\".\n  adb shell content insert --uri content://settings/secure --bind name:s:new_setting --bind value:s:new_value\n\nusage: adb shell content update --uri <URI> [--user <USER_ID>] [--where <WHERE>]\n  <WHERE> is a SQL style where clause in quotes (You have to escape single quotes - see example below).\n  Example:\n  # Change \"new_setting\" secure setting to \"newer_value\".\n  adb shell content update --uri content://settings/secure --bind value:s:newer_value --where \"name='new_setting'\"\n\nusage: adb shell content delete --uri <URI> [--user <USER_ID>] --bind <BINDING> [--bind <BINDING>...] [--where <WHERE>]\n  Example:\n  # Remove \"new_setting\" secure setting.\n  adb shell content delete --uri content://settings/secure --where \"name='new_setting'\"\n\nusage: adb shell content query --uri <URI> [--user <USER_ID>] [--projection <PROJECTION>] [--where <WHERE>] [--sort <SORT_ORDER>]\n  <PROJECTION> is a list of colon separated column names and is formatted:\n  <COLUMN_NAME>[:<COLUMN_NAME>...]\n  <SORT_ORDER> is the order in which rows in the result should be sorted.\n  Example:\n  # Select \"name\" and \"value\" columns from secure settings where \"name\" is equal to \"new_setting\" and sort the result by name in ascending order.\n  adb shell content query --uri content://settings/secure --projection name:value --where \"name='new_setting'\" --sort \"name ASC\"\n\nusage: adb shell content call --uri <URI> --method <METHOD> [--arg <ARG>]\n       [--extra <BINDING> ...]\n  <METHOD> is the name of a provider-defined method\n  <ARG> is an optional string argument\n  <BINDING> is like --bind above, typed data of the form <KEY>:{b,s,i,l,f,d}:<VAL>\n\nusage: adb shell content read --uri <URI> [--user <USER_ID>]\n  Example:\n  # cat default ringtone to a file, then pull to host\n  adb shell 'content read --uri content://settings/system/ringtone > /mnt/sdcard/tmp.ogg' && adb pull /mnt/sdcard/tmp.ogg\n\n";

    private static class Parser {
        private static final String ARGUMENT_ARG = "--arg";
        private static final String ARGUMENT_BIND = "--bind";
        private static final String ARGUMENT_CALL = "call";
        private static final String ARGUMENT_DELETE = "delete";
        private static final String ARGUMENT_EXTRA = "--extra";
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
        private static final String COLON = ":";
        private static final String TYPE_BOOLEAN = "b";
        private static final String TYPE_DOUBLE = "d";
        private static final String TYPE_FLOAT = "f";
        private static final String TYPE_INTEGER = "i";
        private static final String TYPE_LONG = "l";
        private static final String TYPE_STRING = "s";
        private final Tokenizer mTokenizer;

        public Parser(String[] args) {
            this.mTokenizer = new Tokenizer(args);
        }

        public Command parseCommand() {
            try {
                String operation = this.mTokenizer.nextArg();
                if (ARGUMENT_INSERT.equals(operation)) {
                    return parseInsertCommand();
                }
                if (ARGUMENT_DELETE.equals(operation)) {
                    return parseDeleteCommand();
                }
                if (ARGUMENT_UPDATE.equals(operation)) {
                    return parseUpdateCommand();
                }
                if (ARGUMENT_QUERY.equals(operation)) {
                    return parseQueryCommand();
                }
                if (ARGUMENT_CALL.equals(operation)) {
                    return parseCallCommand();
                }
                if (ARGUMENT_READ.equals(operation)) {
                    return parseReadCommand();
                }
                throw new IllegalArgumentException("Unsupported operation: " + operation);
            } catch (IllegalArgumentException iae) {
                System.out.println(Content.USAGE);
                System.out.println("[ERROR] " + iae.getMessage());
                return null;
            }
        }

        private InsertCommand parseInsertCommand() {
            Uri uri = null;
            int userId = 0;
            ContentValues values = new ContentValues();
            while (true) {
                String argument = this.mTokenizer.nextArg();
                if (argument != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_BIND.equals(argument)) {
                        parseBindValue(values);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + argument);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    if (values.size() == 0) {
                        throw new IllegalArgumentException("Bindings not specified. Did you specify --bind argument(s)?");
                    }
                    return new InsertCommand(uri, userId, values);
                }
            }
        }

        private DeleteCommand parseDeleteCommand() {
            Uri uri = null;
            int userId = 0;
            String where = null;
            while (true) {
                String argument = this.mTokenizer.nextArg();
                if (argument != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_WHERE.equals(argument)) {
                        where = argumentValueRequired(argument);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + argument);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new DeleteCommand(uri, userId, where);
                }
            }
        }

        private UpdateCommand parseUpdateCommand() {
            Uri uri = null;
            int userId = 0;
            String where = null;
            ContentValues values = new ContentValues();
            while (true) {
                String argument = this.mTokenizer.nextArg();
                if (argument != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_WHERE.equals(argument)) {
                        where = argumentValueRequired(argument);
                    } else if (ARGUMENT_BIND.equals(argument)) {
                        parseBindValue(values);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + argument);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    if (values.size() == 0) {
                        throw new IllegalArgumentException("Bindings not specified. Did you specify --bind argument(s)?");
                    }
                    return new UpdateCommand(uri, userId, values, where);
                }
            }
        }

        public CallCommand parseCallCommand() {
            String method = null;
            int userId = 0;
            String arg = null;
            Uri uri = null;
            ContentValues values = new ContentValues();
            while (true) {
                String argument = this.mTokenizer.nextArg();
                if (argument != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_METHOD.equals(argument)) {
                        method = argumentValueRequired(argument);
                    } else if (ARGUMENT_ARG.equals(argument)) {
                        arg = argumentValueRequired(argument);
                    } else if (ARGUMENT_EXTRA.equals(argument)) {
                        parseBindValue(values);
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + argument);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    if (method == null) {
                        throw new IllegalArgumentException("Content provider method not specified.");
                    }
                    return new CallCommand(uri, userId, method, arg, values);
                }
            }
        }

        private ReadCommand parseReadCommand() {
            Uri uri = null;
            int userId = 0;
            while (true) {
                String argument = this.mTokenizer.nextArg();
                if (argument != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + argument);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new ReadCommand(uri, userId);
                }
            }
        }

        public QueryCommand parseQueryCommand() {
            Uri uri = null;
            int userId = 0;
            String[] projection = null;
            String sort = null;
            String where = null;
            while (true) {
                String argument = this.mTokenizer.nextArg();
                if (argument != null) {
                    if (ARGUMENT_URI.equals(argument)) {
                        uri = Uri.parse(argumentValueRequired(argument));
                    } else if (ARGUMENT_USER.equals(argument)) {
                        userId = Integer.parseInt(argumentValueRequired(argument));
                    } else if (ARGUMENT_WHERE.equals(argument)) {
                        where = argumentValueRequired(argument);
                    } else if (ARGUMENT_SORT.equals(argument)) {
                        sort = argumentValueRequired(argument);
                    } else if (ARGUMENT_PROJECTION.equals(argument)) {
                        projection = argumentValueRequired(argument).split("[\\s]*:[\\s]*");
                    } else {
                        throw new IllegalArgumentException("Unsupported argument: " + argument);
                    }
                } else {
                    if (uri == null) {
                        throw new IllegalArgumentException("Content provider URI not specified. Did you specify --uri argument?");
                    }
                    return new QueryCommand(uri, userId, projection, where, sort);
                }
            }
        }

        private void parseBindValue(ContentValues values) {
            String argument = this.mTokenizer.nextArg();
            if (TextUtils.isEmpty(argument)) {
                throw new IllegalArgumentException("Binding not well formed: " + argument);
            }
            int firstColonIndex = argument.indexOf(COLON);
            if (firstColonIndex < 0) {
                throw new IllegalArgumentException("Binding not well formed: " + argument);
            }
            int secondColonIndex = argument.indexOf(COLON, firstColonIndex + 1);
            if (secondColonIndex < 0) {
                throw new IllegalArgumentException("Binding not well formed: " + argument);
            }
            String column = argument.substring(0, firstColonIndex);
            String type = argument.substring(firstColonIndex + 1, secondColonIndex);
            String value = argument.substring(secondColonIndex + 1);
            if (TYPE_STRING.equals(type)) {
                values.put(column, value);
                return;
            }
            if (TYPE_BOOLEAN.equalsIgnoreCase(type)) {
                values.put(column, Boolean.valueOf(Boolean.parseBoolean(value)));
                return;
            }
            if (TYPE_INTEGER.equalsIgnoreCase(type) || TYPE_LONG.equalsIgnoreCase(type)) {
                values.put(column, Long.valueOf(Long.parseLong(value)));
            } else {
                if (TYPE_FLOAT.equalsIgnoreCase(type) || TYPE_DOUBLE.equalsIgnoreCase(type)) {
                    values.put(column, Double.valueOf(Double.parseDouble(value)));
                    return;
                }
                throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }

        private String argumentValueRequired(String argument) {
            String value = this.mTokenizer.nextArg();
            if (TextUtils.isEmpty(value) || value.startsWith(ARGUMENT_PREFIX)) {
                throw new IllegalArgumentException("No value for argument: " + argument);
            }
            return value;
        }
    }

    private static class Tokenizer {
        private final String[] mArgs;
        private int mNextArg;

        public Tokenizer(String[] args) {
            this.mArgs = args;
        }

        private String nextArg() {
            if (this.mNextArg >= this.mArgs.length) {
                return null;
            }
            String[] strArr = this.mArgs;
            int i = this.mNextArg;
            this.mNextArg = i + 1;
            return strArr[i];
        }
    }

    private static abstract class Command {
        final Uri mUri;
        final int mUserId;

        protected abstract void onExecute(IContentProvider iContentProvider) throws Exception;

        public Command(Uri uri, int userId) {
            this.mUri = uri;
            this.mUserId = userId;
        }

        public final void execute() {
            String providerName = this.mUri.getAuthority();
            try {
                IActivityManager activityManager = ActivityManagerNative.getDefault();
                IContentProvider provider = null;
                IBinder token = new Binder();
                try {
                    IActivityManager.ContentProviderHolder holder = activityManager.getContentProviderExternal(providerName, this.mUserId, token);
                    if (holder == null) {
                        throw new IllegalStateException("Could not find provider: " + providerName);
                    }
                    provider = holder.provider;
                    onExecute(provider);
                } finally {
                    if (provider != null) {
                        activityManager.removeContentProviderExternal(providerName, token);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error while accessing provider:" + providerName);
                e.printStackTrace();
            }
        }
    }

    private static class InsertCommand extends Command {
        final ContentValues mContentValues;

        public InsertCommand(Uri uri, int userId, ContentValues contentValues) {
            super(uri, userId);
            this.mContentValues = contentValues;
        }

        @Override
        public void onExecute(IContentProvider provider) throws Exception {
            provider.insert((String) null, this.mUri, this.mContentValues);
        }
    }

    private static class DeleteCommand extends Command {
        final String mWhere;

        public DeleteCommand(Uri uri, int userId, String where) {
            super(uri, userId);
            this.mWhere = where;
        }

        @Override
        public void onExecute(IContentProvider provider) throws Exception {
            provider.delete((String) null, this.mUri, this.mWhere, (String[]) null);
        }
    }

    private static class CallCommand extends Command {
        final String mArg;
        Bundle mExtras;
        final String mMethod;

        public CallCommand(Uri uri, int userId, String method, String arg, ContentValues values) {
            super(uri, userId);
            this.mExtras = null;
            this.mMethod = method;
            this.mArg = arg;
            if (values != null) {
                this.mExtras = new Bundle();
                for (String key : values.keySet()) {
                    Object val = values.get(key);
                    if (val instanceof String) {
                        this.mExtras.putString(key, (String) val);
                    } else if (val instanceof Float) {
                        this.mExtras.putFloat(key, ((Float) val).floatValue());
                    } else if (val instanceof Double) {
                        this.mExtras.putDouble(key, ((Double) val).doubleValue());
                    } else if (val instanceof Boolean) {
                        this.mExtras.putBoolean(key, ((Boolean) val).booleanValue());
                    } else if (val instanceof Integer) {
                        this.mExtras.putInt(key, ((Integer) val).intValue());
                    } else if (val instanceof Long) {
                        this.mExtras.putLong(key, ((Long) val).longValue());
                    }
                }
            }
        }

        @Override
        public void onExecute(IContentProvider provider) throws Exception {
            Bundle result = provider.call((String) null, this.mMethod, this.mArg, this.mExtras);
            result.size();
            System.out.println("Result: " + result);
        }
    }

    private static class ReadCommand extends Command {
        public ReadCommand(Uri uri, int userId) {
            super(uri, userId);
        }

        @Override
        public void onExecute(IContentProvider provider) throws Exception {
            ParcelFileDescriptor fd = provider.openFile((String) null, this.mUri, "r", (ICancellationSignal) null, (IBinder) null);
            copy(new FileInputStream(fd.getFileDescriptor()), System.out);
        }

        private static void copy(InputStream is, OutputStream os) throws IOException {
            byte[] buffer = new byte[8192];
            while (true) {
                try {
                    int read = is.read(buffer);
                    if (read > -1) {
                        os.write(buffer, 0, read);
                    } else {
                        return;
                    }
                } finally {
                    IoUtils.closeQuietly(is);
                    IoUtils.closeQuietly(os);
                }
            }
        }
    }

    private static class QueryCommand extends DeleteCommand {
        final String[] mProjection;
        final String mSortOrder;

        public QueryCommand(Uri uri, int userId, String[] projection, String where, String sortOrder) {
            super(uri, userId, where);
            this.mProjection = projection;
            this.mSortOrder = sortOrder;
        }

        @Override
        public void onExecute(IContentProvider provider) throws Exception {
            Cursor cursor = provider.query((String) null, this.mUri, this.mProjection, this.mWhere, (String[]) null, this.mSortOrder, (ICancellationSignal) null);
            if (cursor == null) {
                System.out.println("No result found.");
                return;
            }
            try {
                if (cursor.moveToFirst()) {
                    int rowIndex = 0;
                    StringBuilder builder = new StringBuilder();
                    do {
                        builder.setLength(0);
                        builder.append("Row: ").append(rowIndex).append(" ");
                        rowIndex++;
                        int columnCount = cursor.getColumnCount();
                        for (int i = 0; i < columnCount; i++) {
                            if (i > 0) {
                                builder.append(", ");
                            }
                            String columnName = cursor.getColumnName(i);
                            String columnValue = null;
                            int columnIndex = cursor.getColumnIndex(columnName);
                            int type = cursor.getType(columnIndex);
                            switch (type) {
                                case 0:
                                    columnValue = "NULL";
                                    break;
                                case 1:
                                    columnValue = String.valueOf(cursor.getLong(columnIndex));
                                    break;
                                case 2:
                                    columnValue = String.valueOf(cursor.getFloat(columnIndex));
                                    break;
                                case 3:
                                    columnValue = cursor.getString(columnIndex);
                                    break;
                                case 4:
                                    columnValue = "BLOB";
                                    break;
                            }
                            builder.append(columnName).append("=").append(columnValue);
                        }
                        System.out.println(builder);
                    } while (cursor.moveToNext());
                } else {
                    System.out.println("No result found.");
                }
            } finally {
                cursor.close();
            }
        }
    }

    private static class UpdateCommand extends InsertCommand {
        final String mWhere;

        public UpdateCommand(Uri uri, int userId, ContentValues contentValues, String where) {
            super(uri, userId, contentValues);
            this.mWhere = where;
        }

        @Override
        public void onExecute(IContentProvider provider) throws Exception {
            provider.update((String) null, this.mUri, this.mContentValues, this.mWhere, (String[]) null);
        }
    }

    public static void main(String[] args) {
        Parser parser = new Parser(args);
        Command command = parser.parseCommand();
        if (command != null) {
            command.execute();
        }
    }
}
