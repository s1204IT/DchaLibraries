package com.android.mtp;

import java.util.HashMap;
import java.util.Map;

class MtpDatabaseConstants {
    static final String JOIN_ROOTS = createJoinFromClosure("Documents", "RootExtra", "document_id", "root_id");
    static final Map<String, String> COLUMN_MAP_ROOTS = new HashMap();

    static {
        COLUMN_MAP_ROOTS.put("root_id", "RootExtra.root_id");
        COLUMN_MAP_ROOTS.put("flags", "RootExtra.flags");
        COLUMN_MAP_ROOTS.put("icon", "Documents.icon AS icon");
        COLUMN_MAP_ROOTS.put("title", "Documents._display_name AS title");
        COLUMN_MAP_ROOTS.put("summary", "Documents.summary AS summary");
        COLUMN_MAP_ROOTS.put("document_id", "Documents.document_id AS document_id");
        COLUMN_MAP_ROOTS.put("available_bytes", "RootExtra.available_bytes");
        COLUMN_MAP_ROOTS.put("capacity_bytes", "RootExtra.capacity_bytes");
        COLUMN_MAP_ROOTS.put("mime_types", "RootExtra.mime_types");
        COLUMN_MAP_ROOTS.put("device_id", "device_id");
    }

    private static String createJoinFromClosure(String str, String str2, String str3, String str4) {
        return str + " LEFT JOIN " + str2 + " ON " + str + "." + str3 + " = " + str2 + "." + str4;
    }
}
