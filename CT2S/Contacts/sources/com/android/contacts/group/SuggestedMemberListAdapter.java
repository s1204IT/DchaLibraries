package com.android.contacts.group;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.group.GroupEditorFragment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SuggestedMemberListAdapter extends ArrayAdapter<SuggestedMember> {
    private static final String[] PROJECTION_FILTERED_MEMBERS = {"_id", "contact_id", "display_name"};
    private static final String[] PROJECTION_MEMBER_DATA = {"_id", "contact_id", "mimetype", "data1", "data15"};
    private String mAccountName;
    private String mAccountType;
    private ContentResolver mContentResolver;
    private Context mContext;
    private String mDataSet;
    private final List<Long> mExistingMemberContactIds;
    private Filter mFilter;
    private LayoutInflater mInflater;

    public SuggestedMemberListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.mExistingMemberContactIds = new ArrayList();
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mContext = context;
    }

    public void setAccountType(String accountType) {
        this.mAccountType = accountType;
    }

    public void setAccountName(String accountName) {
        this.mAccountName = accountName;
    }

    public void setDataSet(String dataSet) {
        this.mDataSet = dataSet;
    }

    public void setContentResolver(ContentResolver resolver) {
        this.mContentResolver = resolver;
    }

    public void updateExistingMembersList(List<GroupEditorFragment.Member> list) {
        this.mExistingMemberContactIds.clear();
        for (GroupEditorFragment.Member member : list) {
            this.mExistingMemberContactIds.add(Long.valueOf(member.getContactId()));
        }
    }

    public void addNewMember(long contactId) {
        this.mExistingMemberContactIds.add(Long.valueOf(contactId));
    }

    public void removeMember(long contactId) {
        if (this.mExistingMemberContactIds.contains(Long.valueOf(contactId))) {
            this.mExistingMemberContactIds.remove(Long.valueOf(contactId));
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View result = convertView;
        if (result == null) {
            result = this.mInflater.inflate(R.layout.group_member_suggestion, parent, false);
        }
        SuggestedMember member = getItem(position);
        TextView text1 = (TextView) result.findViewById(R.id.text1);
        TextView text2 = (TextView) result.findViewById(R.id.text2);
        ImageView icon = (ImageView) result.findViewById(R.id.icon);
        text1.setText(member.getDisplayName());
        if (member.hasExtraInfo()) {
            text2.setText(member.getExtraInfo());
        } else {
            text2.setVisibility(8);
        }
        SubscriptionManager subscriptionManager = SubscriptionManager.from(this.mContext);
        if (this.mAccountType != null && this.mAccountType.equals("com.android.contact.sim")) {
            int[] subs = SubscriptionManager.getSubId(0);
            if (subs != null) {
                SubscriptionInfo subinfo = subscriptionManager.getActiveSubscriptionInfo(subs[0]);
                Bitmap iconBitmap = subinfo.createIconBitmap(this.mContext);
                icon.setImageBitmap(iconBitmap);
            }
        } else if (this.mAccountType != null && this.mAccountType.equals("com.android.contact.sim2")) {
            int[] subs2 = SubscriptionManager.getSubId(1);
            if (subs2 != null) {
                SubscriptionInfo subinfo2 = subscriptionManager.getActiveSubscriptionInfo(subs2[0]);
                Bitmap iconBitmap2 = subinfo2.createIconBitmap(this.mContext);
                icon.setImageBitmap(iconBitmap2);
            }
        } else {
            byte[] byteArray = member.getPhotoByteArray();
            if (byteArray == null) {
                icon.setImageDrawable(ContactPhotoManager.getDefaultAvatarDrawableForContact(icon.getResources(), false, null));
            } else {
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                icon.setImageBitmap(bitmap);
            }
        }
        result.setTag(member);
        return result;
    }

    @Override
    public Filter getFilter() {
        if (this.mFilter == null) {
            this.mFilter = new SuggestedMemberFilter();
        }
        return this.mFilter;
    }

    public class SuggestedMemberFilter extends Filter {
        public SuggestedMemberFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence prefix) {
            String accountClause;
            String[] args;
            Filter.FilterResults results = new Filter.FilterResults();
            if (SuggestedMemberListAdapter.this.mContentResolver != null && !TextUtils.isEmpty(prefix)) {
                List<SuggestedMember> suggestionsList = new ArrayList<>();
                HashMap<Long, SuggestedMember> suggestionsMap = new HashMap<>();
                String searchQuery = prefix.toString() + "%";
                if (SuggestedMemberListAdapter.this.mDataSet == null) {
                    accountClause = "account_name=? AND account_type=? AND data_set IS NULL";
                    args = new String[]{SuggestedMemberListAdapter.this.mAccountName, SuggestedMemberListAdapter.this.mAccountType, searchQuery, searchQuery};
                } else {
                    accountClause = "account_name=? AND account_type=? AND data_set=?";
                    args = new String[]{SuggestedMemberListAdapter.this.mAccountName, SuggestedMemberListAdapter.this.mAccountType, SuggestedMemberListAdapter.this.mDataSet, searchQuery, searchQuery};
                }
                Cursor memberDataCursor = SuggestedMemberListAdapter.this.mContentResolver.query(ContactsContract.RawContacts.CONTENT_URI, SuggestedMemberListAdapter.PROJECTION_FILTERED_MEMBERS, accountClause + " AND (display_name LIKE ? OR display_name_alt LIKE ? ) AND deleted!=1", args, "display_name COLLATE LOCALIZED ASC");
                if (memberDataCursor != null) {
                    try {
                        memberDataCursor.moveToPosition(-1);
                        while (memberDataCursor.moveToNext() && suggestionsMap.keySet().size() < 5) {
                            long rawContactId = memberDataCursor.getLong(0);
                            long contactId = memberDataCursor.getLong(1);
                            if (!SuggestedMemberListAdapter.this.mExistingMemberContactIds.contains(Long.valueOf(contactId))) {
                                String displayName = memberDataCursor.getString(2);
                                SuggestedMember member = SuggestedMemberListAdapter.this.new SuggestedMember(rawContactId, displayName, contactId);
                                suggestionsList.add(member);
                                suggestionsMap.put(Long.valueOf(rawContactId), member);
                            }
                        }
                        memberDataCursor.close();
                        int numSuggestions = suggestionsMap.keySet().size();
                        if (numSuggestions != 0) {
                            StringBuilder rawContactIdSelectionBuilder = new StringBuilder();
                            String[] questionMarks = new String[numSuggestions];
                            Arrays.fill(questionMarks, "?");
                            rawContactIdSelectionBuilder.append("_id IN (").append(TextUtils.join(",", questionMarks)).append(")");
                            List<String> selectionArgs = new ArrayList<>();
                            selectionArgs.add("vnd.android.cursor.item/photo");
                            selectionArgs.add("vnd.android.cursor.item/email_v2");
                            selectionArgs.add("vnd.android.cursor.item/phone_v2");
                            for (Long rawContactId2 : suggestionsMap.keySet()) {
                                selectionArgs.add(String.valueOf(rawContactId2));
                            }
                            memberDataCursor = SuggestedMemberListAdapter.this.mContentResolver.query(ContactsContract.RawContactsEntity.CONTENT_URI, SuggestedMemberListAdapter.PROJECTION_MEMBER_DATA, "(mimetype=? OR mimetype=? OR mimetype=?) AND " + rawContactIdSelectionBuilder.toString(), (String[]) selectionArgs.toArray(new String[0]), null);
                            if (memberDataCursor != null) {
                                try {
                                    memberDataCursor.moveToPosition(-1);
                                    while (memberDataCursor.moveToNext()) {
                                        long rawContactId3 = memberDataCursor.getLong(0);
                                        SuggestedMember member2 = suggestionsMap.get(Long.valueOf(rawContactId3));
                                        if (member2 != null) {
                                            String mimetype = memberDataCursor.getString(2);
                                            if ("vnd.android.cursor.item/photo".equals(mimetype)) {
                                                byte[] bitmapArray = memberDataCursor.getBlob(4);
                                                member2.setPhotoByteArray(bitmapArray);
                                            } else if ("vnd.android.cursor.item/email_v2".equals(mimetype) || "vnd.android.cursor.item/phone_v2".equals(mimetype)) {
                                                if (!member2.hasExtraInfo()) {
                                                    String info = memberDataCursor.getString(3);
                                                    member2.setExtraInfo(info);
                                                }
                                            }
                                        }
                                    }
                                } finally {
                                }
                            }
                            results.values = suggestionsList;
                        }
                    } finally {
                    }
                }
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            List<SuggestedMember> suggestionsList = (List) results.values;
            if (suggestionsList != null) {
                SuggestedMemberListAdapter.this.clear();
                for (SuggestedMember member : suggestionsList) {
                    SuggestedMemberListAdapter.this.add(member);
                }
                SuggestedMemberListAdapter.this.notifyDataSetChanged();
            }
        }
    }

    public class SuggestedMember {
        private long mContactId;
        private String mDisplayName;
        private String mExtraInfo;
        private byte[] mPhoto;
        private long mRawContactId;

        public SuggestedMember(long rawContactId, String displayName, long contactId) {
            this.mRawContactId = rawContactId;
            this.mDisplayName = displayName;
            this.mContactId = contactId;
        }

        public String getDisplayName() {
            return this.mDisplayName;
        }

        public String getExtraInfo() {
            return this.mExtraInfo;
        }

        public long getRawContactId() {
            return this.mRawContactId;
        }

        public long getContactId() {
            return this.mContactId;
        }

        public byte[] getPhotoByteArray() {
            return this.mPhoto;
        }

        public boolean hasExtraInfo() {
            return this.mExtraInfo != null;
        }

        public void setExtraInfo(String info) {
            this.mExtraInfo = info;
        }

        public void setPhotoByteArray(byte[] photo) {
            this.mPhoto = photo;
        }

        public String toString() {
            return getDisplayName();
        }
    }
}
