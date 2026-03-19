package com.android.documentsui.base;

import android.database.Cursor;
import java.util.function.Predicate;

public final class DocumentFilters {
    private static int MOVABLE_MASK = 1284;
    public static final Predicate<Cursor> ANY = new Predicate() {
        @Override
        public final boolean test(Object obj) {
            return DocumentFilters.lambda$static$0((Cursor) obj);
        }
    };
    public static final Predicate<Cursor> VIRTUAL = new Predicate() {
        @Override
        public final boolean test(Object obj) {
            return DocumentFilters.isVirtual((Cursor) obj);
        }
    };
    public static final Predicate<Cursor> NOT_MOVABLE = new Predicate() {
        @Override
        public final boolean test(Object obj) {
            return DocumentFilters.isNotMovable((Cursor) obj);
        }
    };
    private static final Predicate<Cursor> O_SHARABLE = new Predicate() {
        @Override
        public final boolean test(Object obj) {
            return DocumentFilters.isSharableInO((Cursor) obj);
        }
    };
    private static final Predicate<Cursor> PREO_SHARABLE = new Predicate() {
        @Override
        public final boolean test(Object obj) {
            return DocumentFilters.isSharablePreO((Cursor) obj);
        }
    };

    static boolean lambda$static$0(Cursor cursor) {
        return true;
    }

    public static Predicate<Cursor> sharable(Features features) {
        if (features.isVirtualFilesSharingEnabled()) {
            return O_SHARABLE;
        }
        return PREO_SHARABLE;
    }

    private static boolean isSharableInO(Cursor cursor) {
        return (DocumentInfo.getCursorInt(cursor, "flags") & 65536) == 0 && !"com.android.documentsui.archives".equals(DocumentInfo.getCursorString(cursor, "android:authority"));
    }

    private static boolean isSharablePreO(Cursor cursor) {
        int cursorInt = DocumentInfo.getCursorInt(cursor, "flags");
        return (65536 & cursorInt) == 0 && (cursorInt & 512) == 0 && !"com.android.documentsui.archives".equals(DocumentInfo.getCursorString(cursor, "android:authority"));
    }

    private static final boolean isVirtual(Cursor cursor) {
        return (DocumentInfo.getCursorInt(cursor, "flags") & 512) != 0;
    }

    private static final boolean isNotMovable(Cursor cursor) {
        return (DocumentInfo.getCursorInt(cursor, "flags") & MOVABLE_MASK) == 0;
    }
}
