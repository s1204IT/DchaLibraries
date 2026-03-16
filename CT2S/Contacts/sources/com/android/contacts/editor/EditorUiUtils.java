package com.android.contacts.editor;

import com.android.contacts.R;
import com.google.common.collect.Maps;
import java.util.HashMap;

public class EditorUiUtils {
    private static final HashMap<String, Integer> mimetypeLayoutMap = Maps.newHashMap();

    static {
        mimetypeLayoutMap.put("#phoneticName", Integer.valueOf(R.layout.phonetic_name_editor_view));
        mimetypeLayoutMap.put("vnd.android.cursor.item/name", Integer.valueOf(R.layout.structured_name_editor_view));
        mimetypeLayoutMap.put("vnd.android.cursor.item/group_membership", -1);
        mimetypeLayoutMap.put("vnd.android.cursor.item/photo", -1);
        mimetypeLayoutMap.put("vnd.android.cursor.item/contact_event", Integer.valueOf(R.layout.event_field_editor_view));
    }

    public static int getLayoutResourceId(String mimetype) {
        Integer id = mimetypeLayoutMap.get(mimetype);
        return id == null ? R.layout.text_fields_editor_view : id.intValue();
    }
}
