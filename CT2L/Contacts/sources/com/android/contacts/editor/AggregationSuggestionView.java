package com.android.contacts.editor;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.editor.AggregationSuggestionEngine;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class AggregationSuggestionView extends LinearLayout {
    private long mContactId;
    private Listener mListener;
    private String mLookupKey;
    private boolean mNewContact;
    private List<AggregationSuggestionEngine.RawContact> mRawContacts;

    public interface Listener {
        void onEditAction(Uri uri);

        void onJoinAction(long j, List<Long> list);
    }

    public AggregationSuggestionView(Context context) {
        super(context);
        this.mRawContacts = Lists.newArrayList();
    }

    public AggregationSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRawContacts = Lists.newArrayList();
    }

    public AggregationSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mRawContacts = Lists.newArrayList();
    }

    public void setNewContact(boolean flag) {
        this.mNewContact = flag;
    }

    public void bindSuggestion(AggregationSuggestionEngine.Suggestion suggestion) {
        this.mContactId = suggestion.contactId;
        this.mLookupKey = suggestion.lookupKey;
        this.mRawContacts = suggestion.rawContacts;
        ImageView photo = (ImageView) findViewById(R.id.aggregation_suggestion_photo);
        if (suggestion.photo != null) {
            photo.setImageBitmap(BitmapFactory.decodeByteArray(suggestion.photo, 0, suggestion.photo.length));
        } else {
            photo.setImageDrawable(ContactPhotoManager.getDefaultAvatarDrawableForContact(getResources(), false, null));
        }
        TextView name = (TextView) findViewById(R.id.aggregation_suggestion_name);
        name.setText(suggestion.name);
        TextView data = (TextView) findViewById(R.id.aggregation_suggestion_data);
        String dataText = null;
        if (suggestion.nickname != null) {
            dataText = suggestion.nickname;
        } else if (suggestion.emailAddress != null) {
            dataText = suggestion.emailAddress;
        } else if (suggestion.phoneNumber != null) {
            dataText = suggestion.phoneNumber;
        }
        data.setText(dataText);
    }

    private boolean canEditSuggestedContact() {
        if (!this.mNewContact) {
            return false;
        }
        AccountTypeManager accountTypes = AccountTypeManager.getInstance(getContext());
        for (AggregationSuggestionEngine.RawContact rawContact : this.mRawContacts) {
            String accountType = rawContact.accountType;
            String dataSet = rawContact.dataSet;
            if (accountType == null) {
                return true;
            }
            AccountType type = accountTypes.getAccountType(accountType, dataSet);
            if (type.areContactsWritable()) {
                return true;
            }
        }
        return false;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public boolean handleItemClickEvent() {
        if (this.mListener != null && isEnabled()) {
            if (canEditSuggestedContact()) {
                this.mListener.onEditAction(ContactsContract.Contacts.getLookupUri(this.mContactId, this.mLookupKey));
            } else {
                ArrayList<Long> rawContactIds = Lists.newArrayList();
                for (AggregationSuggestionEngine.RawContact rawContact : this.mRawContacts) {
                    rawContactIds.add(Long.valueOf(rawContact.rawContactId));
                }
                this.mListener.onJoinAction(this.mContactId, rawContactIds);
            }
            return true;
        }
        return false;
    }
}
