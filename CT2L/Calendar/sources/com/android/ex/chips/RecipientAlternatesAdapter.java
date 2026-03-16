package com.android.ex.chips;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.DropdownChipLayouter;
import com.android.ex.chips.Queries;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipientAlternatesAdapter extends CursorAdapter {
    private static final Map<String, String> sCorrectedPhotoUris = new HashMap();
    private OnCheckedItemChangedListener mCheckedItemChangedListener;
    private int mCheckedItemPosition;
    private final long mCurrentId;
    private final StateListDrawable mDeleteDrawable;
    private final Long mDirectoryId;
    private DropdownChipLayouter mDropdownChipLayouter;

    interface OnCheckedItemChangedListener {
        void onCheckedItemChanged(int i);
    }

    public interface RecipientMatchCallback {
        void matchesFound(Map<String, RecipientEntry> map);

        void matchesNotFound(Set<String> set);
    }

    public static void getMatchingRecipients(Context context, BaseRecipientAdapter adapter, ArrayList<String> inAddresses, Account account, RecipientMatchCallback callback) {
        getMatchingRecipients(context, adapter, inAddresses, 0, account, callback);
    }

    public static void getMatchingRecipients(Context context, BaseRecipientAdapter adapter, ArrayList<String> inAddresses, int addressType, Account account, RecipientMatchCallback callback) {
        Queries.Query query;
        if (addressType == 0) {
            query = Queries.EMAIL;
        } else {
            query = Queries.PHONE;
        }
        int addressesSize = Math.min(50, inAddresses.size());
        HashSet<String> addresses = new HashSet<>();
        StringBuilder bindString = new StringBuilder();
        for (int i = 0; i < addressesSize; i++) {
            Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(inAddresses.get(i).toLowerCase());
            addresses.add(tokens.length > 0 ? tokens[0].getAddress() : inAddresses.get(i));
            bindString.append("?");
            if (i < addressesSize - 1) {
                bindString.append(",");
            }
        }
        if (Log.isLoggable("RecipAlternates", 3)) {
            Log.d("RecipAlternates", "Doing reverse lookup for " + addresses.toString());
        }
        String[] addressArray = new String[addresses.size()];
        addresses.toArray(addressArray);
        Cursor c = null;
        try {
            c = context.getContentResolver().query(query.getContentUri(), query.getProjection(), query.getProjection()[1] + " IN (" + bindString.toString() + ")", addressArray, null);
            HashMap<String, RecipientEntry> recipientEntries = processContactEntries(c, null);
            callback.matchesFound(recipientEntries);
            Set<String> matchesNotFound = new HashSet<>();
            getMatchingRecipientsFromDirectoryQueries(context, recipientEntries, addresses, account, matchesNotFound, query, callback);
            getMatchingRecipientsFromExtensionMatcher(adapter, matchesNotFound, callback);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static void getMatchingRecipientsFromDirectoryQueries(Context context, Map<String, RecipientEntry> recipientEntries, Set<String> addresses, Account account, Set<String> matchesNotFound, Queries.Query query, RecipientMatchCallback callback) {
        List<BaseRecipientAdapter.DirectorySearchParams> paramsList;
        int count;
        if (recipientEntries.size() < addresses.size()) {
            Cursor directoryCursor = null;
            try {
                directoryCursor = context.getContentResolver().query(BaseRecipientAdapter.DirectoryListQuery.URI, BaseRecipientAdapter.DirectoryListQuery.PROJECTION, null, null, null);
                if (directoryCursor == null) {
                    paramsList = null;
                } else {
                    paramsList = BaseRecipientAdapter.setupOtherDirectories(context, directoryCursor, account);
                }
                HashSet<String> unresolvedAddresses = new HashSet<>();
                for (String address : addresses) {
                    if (!recipientEntries.containsKey(address)) {
                        unresolvedAddresses.add(address);
                    }
                }
                matchesNotFound.addAll(unresolvedAddresses);
                if (paramsList != null) {
                    Cursor directoryContactsCursor = null;
                    for (String unresolvedAddress : unresolvedAddresses) {
                        Long directoryId = null;
                        int i = 0;
                        while (i < paramsList.size()) {
                            try {
                                directoryContactsCursor = doQuery(unresolvedAddress, 1, Long.valueOf(paramsList.get(i).directoryId), account, context.getContentResolver(), query);
                                if (directoryContactsCursor != null && directoryContactsCursor.getCount() == 0) {
                                    directoryContactsCursor.close();
                                    directoryContactsCursor = null;
                                    i++;
                                } else {
                                    directoryId = Long.valueOf(paramsList.get(i).directoryId);
                                    break;
                                }
                            } finally {
                                if (directoryContactsCursor != null) {
                                    if (count == 0) {
                                    }
                                }
                            }
                        }
                        if (directoryContactsCursor != null) {
                            try {
                                Map<String, RecipientEntry> entries = processContactEntries(directoryContactsCursor, directoryId);
                                Iterator<String> it = entries.keySet().iterator();
                                while (it.hasNext()) {
                                    matchesNotFound.remove(it.next());
                                }
                                callback.matchesFound(entries);
                            } finally {
                                directoryContactsCursor.close();
                            }
                        }
                    }
                }
            } finally {
                if (directoryCursor != null) {
                    directoryCursor.close();
                }
            }
        }
    }

    public static void getMatchingRecipientsFromExtensionMatcher(BaseRecipientAdapter adapter, Set<String> matchesNotFound, RecipientMatchCallback callback) {
        Map<String, RecipientEntry> entries;
        if (adapter != null && (entries = adapter.getMatchingRecipients(matchesNotFound)) != null && entries.size() > 0) {
            callback.matchesFound(entries);
            for (String address : entries.keySet()) {
                matchesNotFound.remove(address);
            }
        }
        callback.matchesNotFound(matchesNotFound);
    }

    private static HashMap<String, RecipientEntry> processContactEntries(Cursor c, Long directoryId) {
        HashMap<String, RecipientEntry> recipientEntries = new HashMap<>();
        if (c != null && c.moveToFirst()) {
            do {
                String address = c.getString(1);
                RecipientEntry newRecipientEntry = RecipientEntry.constructTopLevelEntry(c.getString(0), c.getInt(7), c.getString(1), c.getInt(2), c.getString(3), c.getLong(4), directoryId, c.getLong(5), c.getString(6), true, c.getString(8));
                RecipientEntry recipientEntry = getBetterRecipient(recipientEntries.get(address), newRecipientEntry);
                recipientEntries.put(address, recipientEntry);
                if (Log.isLoggable("RecipAlternates", 3)) {
                    Log.d("RecipAlternates", "Received reverse look up information for " + address + " RESULTS:  NAME : " + c.getString(0) + " CONTACT ID : " + c.getLong(4) + " ADDRESS :" + c.getString(1));
                }
            } while (c.moveToNext());
        }
        return recipientEntries;
    }

    static RecipientEntry getBetterRecipient(RecipientEntry entry1, RecipientEntry entry2) {
        if (entry2 == null) {
            return entry1;
        }
        if (entry1 != null) {
            if (!TextUtils.isEmpty(entry1.getDisplayName()) && TextUtils.isEmpty(entry2.getDisplayName())) {
                return entry1;
            }
            if (TextUtils.isEmpty(entry2.getDisplayName()) || !TextUtils.isEmpty(entry1.getDisplayName())) {
                if (!TextUtils.equals(entry1.getDisplayName(), entry1.getDestination()) && TextUtils.equals(entry2.getDisplayName(), entry2.getDestination())) {
                    return entry1;
                }
                if (TextUtils.equals(entry2.getDisplayName(), entry2.getDestination()) || !TextUtils.equals(entry1.getDisplayName(), entry1.getDestination())) {
                    if (!(entry1.getPhotoThumbnailUri() == null && entry1.getPhotoBytes() == null) && entry2.getPhotoThumbnailUri() == null && entry2.getPhotoBytes() == null) {
                        return entry1;
                    }
                    if ((entry2.getPhotoThumbnailUri() != null || entry2.getPhotoBytes() != null) && entry1.getPhotoThumbnailUri() == null && entry1.getPhotoBytes() == null) {
                    }
                    return entry2;
                }
                return entry2;
            }
            return entry2;
        }
        return entry2;
    }

    private static Cursor doQuery(CharSequence constraint, int limit, Long directoryId, Account account, ContentResolver resolver, Queries.Query query) {
        Uri.Builder builder = query.getContentFilterUri().buildUpon().appendPath(constraint.toString()).appendQueryParameter("limit", String.valueOf(limit + 5));
        if (directoryId != null) {
            builder.appendQueryParameter("directory", String.valueOf(directoryId));
        }
        if (account != null) {
            builder.appendQueryParameter("name_for_primary_account", account.name);
            builder.appendQueryParameter("type_for_primary_account", account.type);
        }
        Cursor cursor = resolver.query(builder.build(), query.getProjection(), null, null, null);
        return cursor;
    }

    public RecipientAlternatesAdapter(Context context, long contactId, Long directoryId, String lookupKey, long currentId, int queryMode, OnCheckedItemChangedListener listener, DropdownChipLayouter dropdownChipLayouter, StateListDrawable deleteDrawable) {
        super(context, getCursorForConstruction(context, contactId, directoryId, lookupKey, queryMode), 0);
        this.mCheckedItemPosition = -1;
        this.mCurrentId = currentId;
        this.mDirectoryId = directoryId;
        this.mCheckedItemChangedListener = listener;
        this.mDropdownChipLayouter = dropdownChipLayouter;
        this.mDeleteDrawable = deleteDrawable;
    }

    private static Cursor getCursorForConstruction(Context context, long contactId, Long directoryId, String lookupKey, int queryType) {
        Uri uri;
        String desiredMimeType;
        Cursor cursor;
        Uri uri2;
        if (queryType == 0) {
            if (directoryId == null || lookupKey == null) {
                uri2 = Queries.EMAIL.getContentUri();
                desiredMimeType = null;
            } else {
                Uri.Builder builder = ContactsContract.Contacts.getLookupUri(contactId, lookupKey).buildUpon();
                builder.appendPath("entities").appendQueryParameter("directory", String.valueOf(directoryId));
                uri2 = builder.build();
                desiredMimeType = "vnd.android.cursor.item/email_v2";
            }
            cursor = context.getContentResolver().query(uri2, Queries.EMAIL.getProjection(), Queries.EMAIL.getProjection()[4] + " = ?", new String[]{String.valueOf(contactId)}, null);
        } else {
            if (lookupKey == null || directoryId == null) {
                uri = Queries.PHONE.getContentUri();
                desiredMimeType = null;
            } else {
                Uri.Builder builder2 = ContactsContract.Contacts.getLookupUri(contactId, lookupKey).buildUpon();
                builder2.appendPath("entities").appendQueryParameter("directory", String.valueOf(directoryId));
                uri = builder2.build();
                desiredMimeType = "vnd.android.cursor.item/phone_v2";
            }
            cursor = context.getContentResolver().query(uri, Queries.PHONE.getProjection(), Queries.PHONE.getProjection()[4] + " = ?", new String[]{String.valueOf(contactId)}, null);
        }
        if (cursor == null) {
            return null;
        }
        Cursor cursorRemoveUndesiredDestinations = removeUndesiredDestinations(cursor, desiredMimeType, lookupKey);
        cursor.close();
        return cursorRemoveUndesiredDestinations;
    }

    static Cursor removeUndesiredDestinations(Cursor original, String desiredMimeType, String lookupKey) {
        MatrixCursor result = new MatrixCursor(original.getColumnNames(), original.getCount());
        HashSet<String> destinationsSeen = new HashSet<>();
        String defaultDisplayName = null;
        String defaultPhotoThumbnailUri = null;
        int defaultDisplayNameSource = 0;
        original.moveToPosition(-1);
        while (true) {
            if (!original.moveToNext()) {
                break;
            }
            String mimeType = original.getString(9);
            if ("vnd.android.cursor.item/name".equals(mimeType)) {
                defaultDisplayName = original.getString(0);
                defaultPhotoThumbnailUri = original.getString(6);
                defaultDisplayNameSource = original.getInt(7);
                break;
            }
        }
        original.moveToPosition(-1);
        while (original.moveToNext()) {
            if (desiredMimeType != null) {
                String mimeType2 = original.getString(9);
                if (desiredMimeType.equals(mimeType2)) {
                }
            }
            String destination = original.getString(1);
            if (!destinationsSeen.contains(destination)) {
                destinationsSeen.add(destination);
                Object[] row = {original.getString(0), original.getString(1), Integer.valueOf(original.getInt(2)), original.getString(3), Long.valueOf(original.getLong(4)), Long.valueOf(original.getLong(5)), original.getString(6), Integer.valueOf(original.getInt(7)), original.getString(8), original.getString(9)};
                if (row[0] == null) {
                    row[0] = defaultDisplayName;
                }
                if (row[6] == null) {
                    row[6] = defaultPhotoThumbnailUri;
                }
                if (((Integer) row[7]).intValue() == 0) {
                    row[7] = Integer.valueOf(defaultDisplayNameSource);
                }
                if (row[8] == null) {
                    row[8] = lookupKey;
                }
                String photoThumbnailUri = (String) row[6];
                if (photoThumbnailUri != null) {
                    if (sCorrectedPhotoUris.containsKey(photoThumbnailUri)) {
                        row[6] = sCorrectedPhotoUris.get(photoThumbnailUri);
                    } else if (photoThumbnailUri.indexOf(63) != photoThumbnailUri.lastIndexOf(63)) {
                        String[] parts = photoThumbnailUri.split("\\?");
                        StringBuilder correctedUriBuilder = new StringBuilder();
                        for (int i = 0; i < parts.length; i++) {
                            if (i == 1) {
                                correctedUriBuilder.append("?");
                            } else if (i > 1) {
                                correctedUriBuilder.append("&");
                            }
                            correctedUriBuilder.append(parts[i]);
                        }
                        String correctedUri = correctedUriBuilder.toString();
                        sCorrectedPhotoUris.put(photoThumbnailUri, correctedUri);
                        row[6] = correctedUri;
                    }
                }
                result.addRow(row);
            }
        }
        return result;
    }

    @Override
    public long getItemId(int position) {
        Cursor c = getCursor();
        if (c.moveToPosition(position)) {
            c.getLong(5);
            return -1L;
        }
        return -1L;
    }

    public RecipientEntry getRecipientEntry(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        return RecipientEntry.constructTopLevelEntry(c.getString(0), c.getInt(7), c.getString(1), c.getInt(2), c.getString(3), c.getLong(4), this.mDirectoryId, c.getLong(5), c.getString(6), true, c.getString(8));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        if (convertView == null) {
            convertView = this.mDropdownChipLayouter.newView(DropdownChipLayouter.AdapterType.RECIPIENT_ALTERNATES);
        }
        if (cursor.getLong(5) == this.mCurrentId) {
            this.mCheckedItemPosition = position;
            if (this.mCheckedItemChangedListener != null) {
                this.mCheckedItemChangedListener.onCheckedItemChanged(this.mCheckedItemPosition);
            }
        }
        bindView(convertView, convertView.getContext(), cursor);
        return convertView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int position = cursor.getPosition();
        RecipientEntry entry = getRecipientEntry(position);
        this.mDropdownChipLayouter.bindView(view, null, entry, position, DropdownChipLayouter.AdapterType.RECIPIENT_ALTERNATES, null, this.mDeleteDrawable);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return this.mDropdownChipLayouter.newView(DropdownChipLayouter.AdapterType.RECIPIENT_ALTERNATES);
    }
}
