package com.android.gallery3d.common;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import com.android.gallery3d.common.Entry;
import com.mediatek.gallery3d.video.BookmarkEnhance;
import java.lang.reflect.Field;
import java.util.ArrayList;

public final class EntrySchema {
    private static final String[] SQLITE_TYPES = {"TEXT", "INTEGER", "INTEGER", "INTEGER", "INTEGER", "REAL", "REAL", "NONE"};
    private final ColumnInfo[] mColumnInfo;
    private final boolean mHasFullTextIndex;
    private final String[] mProjection;
    private final String mTableName;

    public EntrySchema(Class<? extends Entry> cls) {
        ColumnInfo[] columnInfo = parseColumnInfo(cls);
        this.mTableName = parseTableName(cls);
        this.mColumnInfo = columnInfo;
        boolean z = false;
        String[] strArr = new String[0];
        if (columnInfo != null) {
            strArr = new String[columnInfo.length];
            boolean z2 = false;
            for (int i = 0; i != columnInfo.length; i++) {
                ColumnInfo columnInfo2 = columnInfo[i];
                strArr[i] = columnInfo2.name;
                if (columnInfo2.fullText) {
                    z2 = true;
                }
            }
            z = z2;
        }
        this.mProjection = strArr;
        this.mHasFullTextIndex = z;
    }

    public String getTableName() {
        return this.mTableName;
    }

    private void logExecSql(SQLiteDatabase sQLiteDatabase, String str) {
        sQLiteDatabase.execSQL(str);
    }

    public void createTables(SQLiteDatabase sQLiteDatabase) {
        String str = this.mTableName;
        Utils.assertTrue(str != null);
        StringBuilder sb = new StringBuilder("CREATE TABLE ");
        sb.append(str);
        sb.append(" (_id INTEGER PRIMARY KEY AUTOINCREMENT");
        StringBuilder sb2 = new StringBuilder();
        for (ColumnInfo columnInfo : this.mColumnInfo) {
            if (!columnInfo.isId()) {
                sb.append(',');
                sb.append(columnInfo.name);
                sb.append(' ');
                sb.append(SQLITE_TYPES[columnInfo.type]);
                if (!TextUtils.isEmpty(columnInfo.defaultValue)) {
                    sb.append(" DEFAULT ");
                    sb.append(columnInfo.defaultValue);
                }
                if (columnInfo.unique) {
                    if (sb2.length() == 0) {
                        sb2.append(columnInfo.name);
                    } else {
                        sb2.append(',');
                        sb2.append(columnInfo.name);
                    }
                }
            }
        }
        if (sb2.length() > 0) {
            sb.append(",UNIQUE(");
            sb.append((CharSequence) sb2);
            sb.append(')');
        }
        sb.append(");");
        logExecSql(sQLiteDatabase, sb.toString());
        sb.setLength(0);
        for (ColumnInfo columnInfo2 : this.mColumnInfo) {
            if (columnInfo2.indexed) {
                sb.append("CREATE INDEX ");
                sb.append(str);
                sb.append("_index_");
                sb.append(columnInfo2.name);
                sb.append(" ON ");
                sb.append(str);
                sb.append(" (");
                sb.append(columnInfo2.name);
                sb.append(");");
                logExecSql(sQLiteDatabase, sb.toString());
                sb.setLength(0);
            }
        }
        if (this.mHasFullTextIndex) {
            String str2 = str + "_fulltext";
            sb.append("CREATE VIRTUAL TABLE ");
            sb.append(str2);
            sb.append(" USING FTS3 (_id INTEGER PRIMARY KEY");
            for (ColumnInfo columnInfo3 : this.mColumnInfo) {
                if (columnInfo3.fullText) {
                    String str3 = columnInfo3.name;
                    sb.append(',');
                    sb.append(str3);
                    sb.append(" TEXT");
                }
            }
            sb.append(");");
            logExecSql(sQLiteDatabase, sb.toString());
            sb.setLength(0);
            StringBuilder sb3 = new StringBuilder("INSERT OR REPLACE INTO ");
            sb3.append(str2);
            sb3.append(" (_id");
            for (ColumnInfo columnInfo4 : this.mColumnInfo) {
                if (columnInfo4.fullText) {
                    sb3.append(',');
                    sb3.append(columnInfo4.name);
                }
            }
            sb3.append(") VALUES (new._id");
            for (ColumnInfo columnInfo5 : this.mColumnInfo) {
                if (columnInfo5.fullText) {
                    sb3.append(",new.");
                    sb3.append(columnInfo5.name);
                }
            }
            sb3.append(");");
            String string = sb3.toString();
            sb.append("CREATE TRIGGER ");
            sb.append(str);
            sb.append("_insert_trigger AFTER INSERT ON ");
            sb.append(str);
            sb.append(" FOR EACH ROW BEGIN ");
            sb.append(string);
            sb.append("END;");
            logExecSql(sQLiteDatabase, sb.toString());
            sb.setLength(0);
            sb.append("CREATE TRIGGER ");
            sb.append(str);
            sb.append("_update_trigger AFTER UPDATE ON ");
            sb.append(str);
            sb.append(" FOR EACH ROW BEGIN ");
            sb.append(string);
            sb.append("END;");
            logExecSql(sQLiteDatabase, sb.toString());
            sb.setLength(0);
            sb.append("CREATE TRIGGER ");
            sb.append(str);
            sb.append("_delete_trigger AFTER DELETE ON ");
            sb.append(str);
            sb.append(" FOR EACH ROW BEGIN DELETE FROM ");
            sb.append(str2);
            sb.append(" WHERE _id = old._id; END;");
            logExecSql(sQLiteDatabase, sb.toString());
            sb.setLength(0);
        }
    }

    public void dropTables(SQLiteDatabase sQLiteDatabase) {
        String str = this.mTableName;
        StringBuilder sb = new StringBuilder("DROP TABLE IF EXISTS ");
        sb.append(str);
        sb.append(';');
        logExecSql(sQLiteDatabase, sb.toString());
        sb.setLength(0);
        if (this.mHasFullTextIndex) {
            sb.append("DROP TABLE IF EXISTS ");
            sb.append(str);
            sb.append("_fulltext");
            sb.append(';');
            logExecSql(sQLiteDatabase, sb.toString());
        }
    }

    private String parseTableName(Class<? extends Object> cls) {
        Entry.Table table = (Entry.Table) cls.getAnnotation(Entry.Table.class);
        if (table == null) {
            return null;
        }
        return table.value();
    }

    private ColumnInfo[] parseColumnInfo(Class<? extends Object> cls) {
        ArrayList<ColumnInfo> arrayList = new ArrayList<>();
        while (cls != null) {
            parseColumnInfo(cls, arrayList);
            cls = cls.getSuperclass();
        }
        ColumnInfo[] columnInfoArr = new ColumnInfo[arrayList.size()];
        arrayList.toArray(columnInfoArr);
        return columnInfoArr;
    }

    private void parseColumnInfo(Class<? extends Object> cls, ArrayList<ColumnInfo> arrayList) {
        int i;
        int i2;
        Field[] declaredFields = cls.getDeclaredFields();
        for (int i3 = 0; i3 != declaredFields.length; i3++) {
            Field field = declaredFields[i3];
            Entry.Column column = (Entry.Column) field.getAnnotation(Entry.Column.class);
            if (column != null) {
                Class<?> type = field.getType();
                if (type != String.class) {
                    if (type != Boolean.TYPE) {
                        if (type == Short.TYPE) {
                            i = 2;
                        } else if (type == Integer.TYPE) {
                            i = 3;
                        } else if (type == Long.TYPE) {
                            i = 4;
                        } else if (type == Float.TYPE) {
                            i = 5;
                        } else if (type == Double.TYPE) {
                            i = 6;
                        } else if (type == byte[].class) {
                            i = 7;
                        } else {
                            throw new IllegalArgumentException("Unsupported field type for column: " + type.getName());
                        }
                    } else {
                        i = 1;
                    }
                    i2 = i;
                } else {
                    i2 = 0;
                }
                arrayList.add(new ColumnInfo(column.value(), i2, column.indexed(), column.unique(), column.fullText(), column.defaultValue(), field, arrayList.size()));
            }
        }
    }

    public static final class ColumnInfo {
        public final String defaultValue;
        public final Field field;
        public final boolean fullText;
        public final boolean indexed;
        public final String name;
        public final int projectionIndex;
        public final int type;
        public final boolean unique;

        public ColumnInfo(String str, int i, boolean z, boolean z2, boolean z3, String str2, Field field, int i2) {
            this.name = str.toLowerCase();
            this.type = i;
            this.indexed = z;
            this.unique = z2;
            this.fullText = z3;
            this.defaultValue = str2;
            this.field = field;
            this.projectionIndex = i2;
            field.setAccessible(true);
        }

        public boolean isId() {
            return BookmarkEnhance.COLUMN_ID.equals(this.name);
        }
    }
}
