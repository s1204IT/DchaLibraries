package com.android.contacts;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.provider.ContactsContract;
import com.android.contacts.common.preference.ContactsPreferences;
import java.util.ArrayList;
import java.util.List;

public final class GroupMemberLoader extends CursorLoader {
    private final long mGroupId;

    public static class GroupEditorQuery {
        private static final String[] PROJECTION = {"contact_id", "raw_contact_id", "display_name", "photo_uri", "lookup"};
    }

    public static class GroupDetailQuery {
        private static final String[] PROJECTION = {"contact_id", "photo_uri", "lookup", "display_name", "contact_presence", "contact_status"};
    }

    public static GroupMemberLoader constructLoaderForGroupEditorQuery(Context context, long groupId) {
        return new GroupMemberLoader(context, groupId, GroupEditorQuery.PROJECTION);
    }

    public static GroupMemberLoader constructLoaderForGroupDetailQuery(Context context, long groupId) {
        return new GroupMemberLoader(context, groupId, GroupDetailQuery.PROJECTION);
    }

    private GroupMemberLoader(Context context, long groupId, String[] projection) {
        super(context);
        this.mGroupId = groupId;
        setUri(createUri());
        setProjection(projection);
        setSelection(createSelection());
        setSelectionArgs(createSelectionArgs());
        ContactsPreferences prefs = new ContactsPreferences(context);
        if (prefs.getSortOrder() == 1) {
            setSortOrder("sort_key");
        } else {
            setSortOrder("sort_key_alt");
        }
    }

    private Uri createUri() {
        Uri uri = ContactsContract.Data.CONTENT_URI;
        return uri.buildUpon().appendQueryParameter("directory", String.valueOf(0L)).build();
    }

    private String createSelection() {
        return "mimetype=? AND data1=?";
    }

    private String[] createSelectionArgs() {
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add("vnd.android.cursor.item/group_membership");
        selectionArgs.add(String.valueOf(this.mGroupId));
        return (String[]) selectionArgs.toArray(new String[0]);
    }
}
