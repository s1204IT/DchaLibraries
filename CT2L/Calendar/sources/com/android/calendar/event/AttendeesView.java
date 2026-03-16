package com.android.calendar.event;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.ContactsAsyncHelper;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.event.EditEventHelper;
import java.util.ArrayList;
import java.util.HashMap;

public class AttendeesView extends LinearLayout implements View.OnClickListener {
    private static final String[] PROJECTION = {"contact_id", "lookup", "photo_id"};
    private final Context mContext;
    private final Drawable mDefaultBadge;
    private final int mDefaultPhotoAlpha;
    private final View mDividerForMaybe;
    private final View mDividerForNo;
    private final View mDividerForNoResponse;
    private final View mDividerForYes;
    private final CharSequence[] mEntries;
    private final ColorMatrixColorFilter mGrayscaleFilter;
    private final LayoutInflater mInflater;
    private int mMaybe;
    private int mNo;
    private int mNoResponse;
    private final int mNoResponsePhotoAlpha;
    private final PresenceQueryHandler mPresenceQueryHandler;
    HashMap<String, Drawable> mRecycledPhotos;
    private int mYes;

    public AttendeesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mPresenceQueryHandler = new PresenceQueryHandler(context.getContentResolver());
        Resources resources = context.getResources();
        this.mDefaultBadge = resources.getDrawable(R.drawable.ic_contact_picture);
        this.mNoResponsePhotoAlpha = resources.getInteger(R.integer.noresponse_attendee_photo_alpha_level);
        this.mDefaultPhotoAlpha = resources.getInteger(R.integer.default_attendee_photo_alpha_level);
        this.mEntries = resources.getTextArray(R.array.response_labels1);
        this.mDividerForYes = constructDividerView(this.mEntries[1]);
        this.mDividerForNo = constructDividerView(this.mEntries[3]);
        this.mDividerForMaybe = constructDividerView(this.mEntries[2]);
        this.mDividerForNoResponse = constructDividerView(this.mEntries[0]);
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.0f);
        this.mGrayscaleFilter = new ColorMatrixColorFilter(matrix);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int visibility = isEnabled() ? 0 : 8;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            View minusButton = child.findViewById(R.id.contact_remove);
            if (minusButton != null) {
                minusButton.setVisibility(visibility);
            }
        }
    }

    private View constructDividerView(CharSequence label) {
        TextView textView = (TextView) this.mInflater.inflate(R.layout.event_info_label, (ViewGroup) this, false);
        textView.setText(label);
        textView.setClickable(false);
        return textView;
    }

    private void updateDividerViewLabel(View divider, CharSequence label, int count) {
        if (count <= 0) {
            ((TextView) divider).setText(label);
        } else {
            ((TextView) divider).setText(((Object) label) + " (" + count + ")");
        }
    }

    private View constructAttendeeView(EditEventHelper.AttendeeItem item) {
        item.mView = this.mInflater.inflate(R.layout.contact_item, (ViewGroup) null);
        return updateAttendeeView(item);
    }

    private View updateAttendeeView(EditEventHelper.AttendeeItem item) {
        CalendarEventModel.Attendee attendee = item.mAttendee;
        View view = item.mView;
        TextView nameView = (TextView) view.findViewById(R.id.name);
        nameView.setText(TextUtils.isEmpty(attendee.mName) ? attendee.mEmail : attendee.mName);
        if (item.mRemoved) {
            nameView.setPaintFlags(nameView.getPaintFlags() | 16);
        } else {
            nameView.setPaintFlags(nameView.getPaintFlags() & (-17));
        }
        ImageButton button = (ImageButton) view.findViewById(R.id.contact_remove);
        button.setVisibility(isEnabled() ? 0 : 8);
        button.setTag(item);
        if (item.mRemoved) {
            button.setImageResource(R.drawable.ic_menu_add_field_holo_light);
            button.setContentDescription(this.mContext.getString(R.string.accessibility_add_attendee));
        } else {
            button.setImageResource(R.drawable.ic_menu_remove_field_holo_light);
            button.setContentDescription(this.mContext.getString(R.string.accessibility_remove_attendee));
        }
        button.setOnClickListener(this);
        QuickContactBadge badgeView = (QuickContactBadge) view.findViewById(R.id.badge);
        Drawable badge = null;
        if (this.mRecycledPhotos != null) {
            Drawable badge2 = this.mRecycledPhotos.get(item.mAttendee.mEmail);
            badge = badge2;
        }
        if (badge != null) {
            item.mBadge = badge;
        }
        badgeView.setImageDrawable(item.mBadge);
        if (item.mAttendee.mStatus == 0) {
            item.mBadge.setAlpha(this.mNoResponsePhotoAlpha);
        } else {
            item.mBadge.setAlpha(this.mDefaultPhotoAlpha);
        }
        if (item.mAttendee.mStatus == 2) {
            item.mBadge.setColorFilter(this.mGrayscaleFilter);
        } else {
            item.mBadge.setColorFilter(null);
        }
        if (item.mContactLookupUri != null) {
            badgeView.assignContactUri(item.mContactLookupUri);
        } else {
            badgeView.assignContactFromEmail(item.mAttendee.mEmail, true);
        }
        badgeView.setMaxHeight(60);
        return view;
    }

    public boolean contains(CalendarEventModel.Attendee attendee) {
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            if (!(view instanceof TextView)) {
                EditEventHelper.AttendeeItem attendeeItem = (EditEventHelper.AttendeeItem) view.getTag();
                if (TextUtils.equals(attendee.mEmail, attendeeItem.mAttendee.mEmail)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clearAttendees() {
        this.mRecycledPhotos = new HashMap<>();
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View view = getChildAt(i);
            if (!(view instanceof TextView)) {
                EditEventHelper.AttendeeItem attendeeItem = (EditEventHelper.AttendeeItem) view.getTag();
                this.mRecycledPhotos.put(attendeeItem.mAttendee.mEmail, attendeeItem.mBadge);
            }
        }
        removeAllViews();
        this.mYes = 0;
        this.mNo = 0;
        this.mMaybe = 0;
        this.mNoResponse = 0;
    }

    private void addOneAttendee(CalendarEventModel.Attendee attendee) {
        int index;
        Uri uri;
        View prevItem;
        View Separator;
        if (!contains(attendee)) {
            EditEventHelper.AttendeeItem item = new EditEventHelper.AttendeeItem(attendee, this.mDefaultBadge);
            int status = attendee.mStatus;
            boolean firstAttendeeInCategory = false;
            switch (status) {
                case 1:
                    updateDividerViewLabel(this.mDividerForYes, this.mEntries[1], this.mYes + 1);
                    if (this.mYes == 0) {
                        addView(this.mDividerForYes, 0);
                        firstAttendeeInCategory = true;
                    }
                    this.mYes++;
                    index = this.mYes + 0;
                    break;
                case 2:
                    int startIndex = this.mYes == 0 ? 0 : this.mYes + 1;
                    updateDividerViewLabel(this.mDividerForNo, this.mEntries[3], this.mNo + 1);
                    if (this.mNo == 0) {
                        addView(this.mDividerForNo, startIndex);
                        firstAttendeeInCategory = true;
                    }
                    this.mNo++;
                    index = startIndex + this.mNo;
                    break;
                case 3:
                default:
                    int startIndex2 = (this.mNo == 0 ? 0 : this.mNo + 1) + (this.mYes == 0 ? 0 : this.mYes + 1) + (this.mMaybe == 0 ? 0 : this.mMaybe + 1);
                    updateDividerViewLabel(this.mDividerForNoResponse, this.mEntries[0], this.mNoResponse + 1);
                    if (this.mNoResponse == 0) {
                        addView(this.mDividerForNoResponse, startIndex2);
                        firstAttendeeInCategory = true;
                    }
                    this.mNoResponse++;
                    index = startIndex2 + this.mNoResponse;
                    break;
                case 4:
                    int startIndex3 = (this.mYes == 0 ? 0 : this.mYes + 1) + (this.mNo == 0 ? 0 : this.mNo + 1);
                    updateDividerViewLabel(this.mDividerForMaybe, this.mEntries[2], this.mMaybe + 1);
                    if (this.mMaybe == 0) {
                        addView(this.mDividerForMaybe, startIndex3);
                        firstAttendeeInCategory = true;
                    }
                    this.mMaybe++;
                    index = startIndex3 + this.mMaybe;
                    break;
            }
            View view = constructAttendeeView(item);
            view.setTag(item);
            addView(view, index);
            if (!firstAttendeeInCategory && (prevItem = getChildAt(index - 1)) != null && (Separator = prevItem.findViewById(R.id.contact_separator)) != null) {
                Separator.setVisibility(0);
            }
            String selection = null;
            String[] selectionArgs = null;
            if (attendee.mIdentity != null && attendee.mIdNamespace != null) {
                uri = ContactsContract.Data.CONTENT_URI;
                selection = "mimetype=? AND data1=? AND data2=?";
                selectionArgs = new String[]{"vnd.android.cursor.item/identity", attendee.mIdentity, attendee.mIdNamespace};
            } else {
                uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(attendee.mEmail));
            }
            this.mPresenceQueryHandler.startQuery(item.mUpdateCounts + 1, item, uri, PROJECTION, selection, selectionArgs, null);
        }
    }

    public void addAttendees(ArrayList<CalendarEventModel.Attendee> attendees) {
        synchronized (this) {
            for (CalendarEventModel.Attendee attendee : attendees) {
                addOneAttendee(attendee);
            }
        }
    }

    private class PresenceQueryHandler extends AsyncQueryHandler {
        public PresenceQueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int queryIndex, Object cookie, Cursor cursor) {
            if (cursor != null && cookie != null) {
                final EditEventHelper.AttendeeItem item = (EditEventHelper.AttendeeItem) cookie;
                try {
                    if (item.mUpdateCounts < queryIndex) {
                        item.mUpdateCounts = queryIndex;
                        if (cursor.moveToFirst()) {
                            long contactId = cursor.getLong(0);
                            Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
                            String lookupKey = cursor.getString(1);
                            item.mContactLookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
                            long photoId = cursor.getLong(2);
                            if (photoId > 0) {
                                ContactsAsyncHelper.retrieveContactPhotoAsync(AttendeesView.this.mContext, item, new Runnable() {
                                    @Override
                                    public void run() {
                                        AttendeesView.this.updateAttendeeView(item);
                                    }
                                }, contactUri);
                            } else {
                                AttendeesView.this.updateAttendeeView(item);
                            }
                        } else {
                            item.mContactLookupUri = null;
                            if (!Utils.isValidEmail(item.mAttendee.mEmail)) {
                                item.mAttendee.mEmail = null;
                                AttendeesView.this.updateAttendeeView(item);
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        EditEventHelper.AttendeeItem item = (EditEventHelper.AttendeeItem) view.getTag();
        item.mRemoved = !item.mRemoved;
        updateAttendeeView(item);
    }
}
