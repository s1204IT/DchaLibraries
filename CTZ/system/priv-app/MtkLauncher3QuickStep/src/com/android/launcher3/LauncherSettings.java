package com.android.launcher3;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;

/* loaded from: classes.dex */
public class LauncherSettings {

    public interface BaseLauncherColumns extends ChangeLogColumns {
        public static final String ICON = "icon";
        public static final String ICON_PACKAGE = "iconPackage";
        public static final String ICON_RESOURCE = "iconResource";
        public static final String INTENT = "intent";
        public static final String ITEM_TYPE = "itemType";
        public static final int ITEM_TYPE_APPLICATION = 0;
        public static final int ITEM_TYPE_SHORTCUT = 1;
        public static final String TITLE = "title";
    }

    interface ChangeLogColumns extends BaseColumns {
        public static final String MODIFIED = "modified";
    }

    public static final class WorkspaceScreens implements ChangeLogColumns {
        public static final String SCREEN_RANK = "screenRank";
        public static final String TABLE_NAME = "workspaceScreens";
        public static final Uri CONTENT_URI = Uri.parse("content://" + LauncherProvider.AUTHORITY + "/" + TABLE_NAME);
    }

    public static final class Favorites implements BaseLauncherColumns {
        public static final String APPWIDGET_ID = "appWidgetId";
        public static final String APPWIDGET_PROVIDER = "appWidgetProvider";
        public static final String CELLX = "cellX";
        public static final String CELLY = "cellY";
        public static final String CONTAINER = "container";
        public static final int CONTAINER_DESKTOP = -100;
        public static final int CONTAINER_HOTSEAT = -101;
        public static final int ITEM_TYPE_APPWIDGET = 4;
        public static final int ITEM_TYPE_CUSTOM_APPWIDGET = 5;
        public static final int ITEM_TYPE_DEEP_SHORTCUT = 6;
        public static final int ITEM_TYPE_FOLDER = 2;
        public static final String OPTIONS = "options";
        public static final String PROFILE_ID = "profileId";
        public static final String RANK = "rank";
        public static final String RESTORED = "restored";
        public static final String SCREEN = "screen";
        public static final String SPANX = "spanX";
        public static final String SPANY = "spanY";
        public static final String TABLE_NAME = "favorites";
        public static final Uri CONTENT_URI = Uri.parse("content://" + LauncherProvider.AUTHORITY + "/" + TABLE_NAME);

        public static Uri getContentUri(long j) {
            return Uri.parse("content://" + LauncherProvider.AUTHORITY + "/" + TABLE_NAME + "/" + j);
        }

        static final String containerToString(int i) {
            switch (i) {
                case CONTAINER_HOTSEAT /* -101 */:
                    return "hotseat";
                case -100:
                    return "desktop";
                default:
                    return String.valueOf(i);
            }
        }

        static final String itemTypeToString(int i) {
            switch (i) {
                case 0:
                    return "APP";
                case 1:
                    return "SHORTCUT";
                case 2:
                    return "FOLDER";
                case 3:
                default:
                    return String.valueOf(i);
                case 4:
                    return "WIDGET";
                case 5:
                    return "CUSTOMWIDGET";
                case 6:
                    return "DEEPSHORTCUT";
            }
        }

        public static void addTableToDb(SQLiteDatabase sQLiteDatabase, long j, boolean z) {
            sQLiteDatabase.execSQL("CREATE TABLE " + (z ? " IF NOT EXISTS " : "") + TABLE_NAME + " (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,container INTEGER,screen INTEGER,cellX INTEGER,cellY INTEGER,spanX INTEGER,spanY INTEGER,itemType INTEGER,appWidgetId INTEGER NOT NULL DEFAULT -1,iconPackage TEXT,iconResource TEXT,icon BLOB,appWidgetProvider TEXT,modified INTEGER NOT NULL DEFAULT 0,restored INTEGER NOT NULL DEFAULT 0,profileId INTEGER DEFAULT " + j + ",rank INTEGER NOT NULL DEFAULT 0,options INTEGER NOT NULL DEFAULT 0);");
        }
    }

    public static final class Settings {
        public static final Uri CONTENT_URI = Uri.parse("content://" + LauncherProvider.AUTHORITY + "/settings");
        public static final String EXTRA_VALUE = "value";
        public static final String METHOD_CLEAR_EMPTY_DB_FLAG = "clear_empty_db_flag";
        public static final String METHOD_CREATE_EMPTY_DB = "create_empty_db";
        public static final String METHOD_DELETE_EMPTY_FOLDERS = "delete_empty_folders";
        public static final String METHOD_LOAD_DEFAULT_FAVORITES = "load_default_favorites";
        public static final String METHOD_NEW_ITEM_ID = "generate_new_item_id";
        public static final String METHOD_NEW_SCREEN_ID = "generate_new_screen_id";
        public static final String METHOD_REMOVE_GHOST_WIDGETS = "remove_ghost_widgets";
        public static final String METHOD_WAS_EMPTY_DB_CREATED = "get_empty_db_flag";

        public static Bundle call(ContentResolver contentResolver, String str) {
            return contentResolver.call(CONTENT_URI, str, (String) null, (Bundle) null);
        }
    }
}
