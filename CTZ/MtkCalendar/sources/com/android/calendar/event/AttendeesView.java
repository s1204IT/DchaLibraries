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
import java.util.Iterator;

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

    public AttendeesView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
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
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.0f);
        this.mGrayscaleFilter = new ColorMatrixColorFilter(colorMatrix);
    }

    @Override
    public void setEnabled(boolean z) {
        int i;
        super.setEnabled(z);
        if (!isEnabled()) {
            i = 8;
        } else {
            i = 0;
        }
        int childCount = getChildCount();
        for (int i2 = 0; i2 < childCount; i2++) {
            View viewFindViewById = getChildAt(i2).findViewById(R.id.contact_remove);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(i);
            }
        }
    }

    private View constructDividerView(CharSequence charSequence) {
        TextView textView = (TextView) this.mInflater.inflate(R.layout.event_info_label, (ViewGroup) this, false);
        textView.setText(charSequence);
        textView.setClickable(false);
        return textView;
    }

    private void updateDividerViewLabel(View view, CharSequence charSequence, int i) {
        if (i <= 0) {
            ((TextView) view).setText(charSequence);
            return;
        }
        ((TextView) view).setText(((Object) charSequence) + " (" + i + ")");
    }

    private View constructAttendeeView(EditEventHelper.AttendeeItem attendeeItem) {
        attendeeItem.mView = this.mInflater.inflate(R.layout.contact_item, (ViewGroup) null);
        return updateAttendeeView(attendeeItem);
    }

    private View updateAttendeeView(EditEventHelper.AttendeeItem attendeeItem) {
        Drawable drawable;
        CalendarEventModel.Attendee attendee = attendeeItem.mAttendee;
        View view = attendeeItem.mView;
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setText(TextUtils.isEmpty(attendee.mName) ? attendee.mEmail : attendee.mName);
        if (attendeeItem.mRemoved) {
            textView.setPaintFlags(16 | textView.getPaintFlags());
        } else {
            textView.setPaintFlags((-17) & textView.getPaintFlags());
        }
        ImageButton imageButton = (ImageButton) view.findViewById(R.id.contact_remove);
        imageButton.setVisibility(isEnabled() ? 0 : 8);
        imageButton.setTag(attendeeItem);
        if (attendeeItem.mRemoved) {
            imageButton.setImageResource(R.drawable.ic_menu_add_field_holo_light);
            imageButton.setContentDescription(this.mContext.getString(R.string.accessibility_add_attendee));
        } else {
            imageButton.setImageResource(R.drawable.ic_menu_remove_field_holo_light);
            imageButton.setContentDescription(this.mContext.getString(R.string.accessibility_remove_attendee));
        }
        imageButton.setOnClickListener(this);
        QuickContactBadge quickContactBadge = (QuickContactBadge) view.findViewById(R.id.badge);
        if (this.mRecycledPhotos != null) {
            drawable = this.mRecycledPhotos.get(attendeeItem.mAttendee.mEmail);
        } else {
            drawable = null;
        }
        if (drawable != null) {
            attendeeItem.mBadge = drawable;
        }
        quickContactBadge.setImageDrawable(attendeeItem.mBadge);
        if (attendeeItem.mAttendee.mStatus == 0) {
            attendeeItem.mBadge.setAlpha(this.mNoResponsePhotoAlpha);
        } else {
            attendeeItem.mBadge.setAlpha(this.mDefaultPhotoAlpha);
        }
        if (attendeeItem.mAttendee.mStatus == 2) {
            attendeeItem.mBadge.setColorFilter(this.mGrayscaleFilter);
        } else {
            attendeeItem.mBadge.setColorFilter(null);
        }
        if (attendeeItem.mContactLookupUri != null) {
            quickContactBadge.assignContactUri(attendeeItem.mContactLookupUri);
        } else {
            quickContactBadge.assignContactFromEmail(attendeeItem.mAttendee.mEmail, true);
        }
        quickContactBadge.setMaxHeight(60);
        return view;
    }

    public boolean contains(CalendarEventModel.Attendee attendee) {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (!(childAt instanceof TextView)) {
                if (TextUtils.equals(attendee.mEmail, ((EditEventHelper.AttendeeItem) childAt.getTag()).mAttendee.mEmail)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void clearAttendees() {
        this.mRecycledPhotos = new HashMap<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (!(childAt instanceof TextView)) {
                EditEventHelper.AttendeeItem attendeeItem = (EditEventHelper.AttendeeItem) childAt.getTag();
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
        boolean z;
        int i;
        Uri uriWithAppendedPath;
        String str;
        String[] strArr;
        View childAt;
        View viewFindViewById;
        int i2;
        if (contains(attendee)) {
            return;
        }
        EditEventHelper.AttendeeItem attendeeItem = new EditEventHelper.AttendeeItem(attendee, this.mDefaultBadge);
        int i3 = attendee.mStatus;
        if (i3 != 4) {
            switch (i3) {
                case 1:
                    updateDividerViewLabel(this.mDividerForYes, this.mEntries[1], this.mYes + 1);
                    if (this.mYes == 0) {
                        addView(this.mDividerForYes, 0);
                        z = true;
                    } else {
                        z = false;
                    }
                    this.mYes++;
                    i = this.mYes + 0;
                    break;
                case 2:
                    if (this.mYes != 0) {
                        i2 = this.mYes + 1;
                    } else {
                        i2 = 0;
                    }
                    updateDividerViewLabel(this.mDividerForNo, this.mEntries[3], this.mNo + 1);
                    if (this.mNo == 0) {
                        addView(this.mDividerForNo, i2);
                        z = true;
                    } else {
                        z = false;
                    }
                    this.mNo++;
                    i = i2 + this.mNo;
                    break;
                default:
                    int i4 = (this.mYes == 0 ? 0 : this.mYes + 1) + (this.mNo == 0 ? 0 : this.mNo + 1) + (this.mMaybe == 0 ? 0 : this.mMaybe + 1);
                    updateDividerViewLabel(this.mDividerForNoResponse, this.mEntries[0], this.mNoResponse + 1);
                    if (this.mNoResponse == 0) {
                        addView(this.mDividerForNoResponse, i4);
                        z = true;
                    } else {
                        z = false;
                    }
                    this.mNoResponse++;
                    i = i4 + this.mNoResponse;
                    break;
            }
        } else {
            int i5 = (this.mYes == 0 ? 0 : this.mYes + 1) + (this.mNo == 0 ? 0 : this.mNo + 1);
            updateDividerViewLabel(this.mDividerForMaybe, this.mEntries[2], this.mMaybe + 1);
            if (this.mMaybe == 0) {
                addView(this.mDividerForMaybe, i5);
                z = true;
            } else {
                z = false;
            }
            this.mMaybe++;
            i = i5 + this.mMaybe;
        }
        View viewConstructAttendeeView = constructAttendeeView(attendeeItem);
        viewConstructAttendeeView.setTag(attendeeItem);
        addView(viewConstructAttendeeView, i);
        if (!z && (childAt = getChildAt(i - 1)) != null && (viewFindViewById = childAt.findViewById(R.id.contact_separator)) != null) {
            viewFindViewById.setVisibility(0);
        }
        if (attendee.mIdentity != null && attendee.mIdNamespace != null) {
            Uri uri = ContactsContract.Data.CONTENT_URI;
            String[] strArr2 = {"vnd.android.cursor.item/identity", attendee.mIdentity, attendee.mIdNamespace};
            str = "mimetype=? AND data1=? AND data2=?";
            strArr = strArr2;
            uriWithAppendedPath = uri;
        } else {
            uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_LOOKUP_URI, Uri.encode(attendee.mEmail));
            str = null;
            strArr = null;
        }
        this.mPresenceQueryHandler.startQuery(attendeeItem.mUpdateCounts + 1, attendeeItem, uriWithAppendedPath, PROJECTION, str, strArr, null);
    }

    public void addAttendees(ArrayList<CalendarEventModel.Attendee> arrayList) {
        synchronized (this) {
            Iterator<CalendarEventModel.Attendee> it = arrayList.iterator();
            while (it.hasNext()) {
                addOneAttendee(it.next());
            }
        }
    }

    private class PresenceQueryHandler extends AsyncQueryHandler {
        public PresenceQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            if (cursor == null || obj == null) {
                if (cursor != null) {
                    return;
                } else {
                    return;
                }
            }
            final EditEventHelper.AttendeeItem attendeeItem = (EditEventHelper.AttendeeItem) obj;
            try {
                if (attendeeItem.mUpdateCounts < i) {
                    attendeeItem.mUpdateCounts = i;
                    if (cursor.moveToFirst()) {
                        long j = cursor.getLong(0);
                        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, j);
                        attendeeItem.mContactLookupUri = ContactsContract.Contacts.getLookupUri(j, cursor.getString(1));
                        if (cursor.getLong(2) > 0) {
                            ContactsAsyncHelper.retrieveContactPhotoAsync(AttendeesView.this.mContext, attendeeItem, new Runnable() {
                                @Override
                                public void run() {
                                    AttendeesView.this.mRecycledPhotos = null;
                                    AttendeesView.this.updateAttendeeView(attendeeItem);
                                }
                            }, uriWithAppendedId);
                        } else {
                            AttendeesView.this.mRecycledPhotos = null;
                            AttendeesView.this.updateAttendeeView(attendeeItem);
                        }
                    } else {
                        attendeeItem.mContactLookupUri = null;
                        if (!Utils.isValidEmail(attendeeItem.mAttendee.mEmail)) {
                            attendeeItem.mAttendee.mEmail = null;
                            AttendeesView.this.updateAttendeeView(attendeeItem);
                        } else if (AttendeesView.this.mRecycledPhotos != null && AttendeesView.this.mDefaultBadge != attendeeItem.mBadge) {
                            AttendeesView.this.mRecycledPhotos = null;
                            attendeeItem.mBadge = AttendeesView.this.mDefaultBadge;
                            AttendeesView.this.updateAttendeeView(attendeeItem);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
    }

    @Override
    public void onClick(View view) {
        EditEventHelper.AttendeeItem attendeeItem = (EditEventHelper.AttendeeItem) view.getTag();
        attendeeItem.mRemoved = !attendeeItem.mRemoved;
        updateAttendeeView(attendeeItem);
    }
}
