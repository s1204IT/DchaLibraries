package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;
import com.android.contacts.R;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.mediatek.contacts.list.DataKindBasePickerAdapter;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import java.util.ArrayList;

public class ConferenceCallsPickerAdapter extends PhoneNumbersPickerAdapter {
    private final View.OnClickListener mCheckBoxListener;
    private DataKindBasePickerAdapter.SelectedContactsListener mConferenceCallSelectedListener;
    private Context mContext;
    private int mReferenceCallMaxNumber;
    public static final Uri PICK_CONFERENCE_CALL_URI = ContactsContract.CommonDataKinds.Callable.CONTENT_URI;
    public static final Uri PICK_CONFERENCE_CALL_FILTER_URI = ContactsContract.CommonDataKinds.Callable.CONTENT_FILTER_URI;

    public ConferenceCallsPickerAdapter(Context context, ListView listView) {
        super(context, listView);
        this.mReferenceCallMaxNumber = 5;
        this.mCheckBoxListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view;
                Long l = (Long) checkBox.getTag();
                boolean z = ConferenceCallsPickerAdapter.this.getSelectedContactIds().size() >= ConferenceCallsPickerAdapter.this.mReferenceCallMaxNumber;
                boolean zIsChecked = checkBox.isChecked();
                if (z && zIsChecked) {
                    Log.i("ConferenceCallsPickerAdapter", "[mCheckBoxClickListener] Current selected Contact cnt > 5,cannot select more");
                    checkBox.setChecked(false);
                    MtkToast.toast(ConferenceCallsPickerAdapter.this.mContext.getApplicationContext(), ConferenceCallsPickerAdapter.this.mContext.getResources().getString(R.string.multichoice_contacts_limit, Integer.valueOf(ConferenceCallsPickerAdapter.this.mReferenceCallMaxNumber)));
                    return;
                }
                ((ContactListItemView) checkBox.getParent()).getDataView().getText().toString();
                if (checkBox.isChecked()) {
                    ConferenceCallsPickerAdapter.this.getSelectedContactIds().add(l);
                } else {
                    ConferenceCallsPickerAdapter.this.getSelectedContactIds().remove(l);
                }
                if (ConferenceCallsPickerAdapter.this.mConferenceCallSelectedListener != null) {
                    ConferenceCallsPickerAdapter.this.mConferenceCallSelectedListener.onSelectedContactsChangedViaCheckBox();
                }
            }
        };
        this.mContext = context;
    }

    public void setRefenceCallMaxNumber(int i) {
        this.mReferenceCallMaxNumber = i;
    }

    @Override
    protected Uri configLoaderUri(long j) {
        boolean zIsSearchMode = isSearchMode();
        Log.i("ConferenceCallsPickerAdapter", "[configLoaderUri]directoryId = " + j + ",isSearchMode = " + zIsSearchMode);
        if (j != 0) {
            Log.w("ConferenceCallsPickerAdapter", "[configLoaderUri] is not ready for non-default directory ID");
        }
        if (zIsSearchMode) {
            String queryString = getQueryString();
            Uri.Builder builderBuildUpon = PICK_CONFERENCE_CALL_FILTER_URI.buildUpon();
            if (TextUtils.isEmpty(queryString)) {
                builderBuildUpon.appendPath("");
            } else {
                builderBuildUpon.appendPath(queryString);
            }
            builderBuildUpon.appendQueryParameter("directory", String.valueOf(j));
            builderBuildUpon.appendQueryParameter("checked_ids_arg", PICK_CONFERENCE_CALL_URI.toString());
            return builderBuildUpon.build();
        }
        Uri uriBuild = PICK_CONFERENCE_CALL_URI.buildUpon().appendQueryParameter("directory", String.valueOf(j)).build();
        if (isSectionHeaderDisplayEnabled()) {
            return buildSectionIndexerUri(uriBuild);
        }
        return uriBuild;
    }

    @Override
    protected void bindData(ContactListItemView contactListItemView, Cursor cursor) {
        contactListItemView.getLabelView().setText("");
        super.bindData(contactListItemView, cursor);
    }

    @Override
    public void bindView(View view, int i, Cursor cursor, int i2) {
        Log.d("ConferenceCallsPickerAdapter", "[bindView]position = " + i2 + ",partition = " + i);
        super.bindView(view, i, cursor, i2);
        ((ContactListItemView) view).getCheckBox().setOnClickListener(this.mCheckBoxListener);
    }

    @Override
    protected void configureSelection(CursorLoader cursorLoader, long j, ContactListFilter contactListFilter) {
        if (contactListFilter == null || j != 0) {
            Log.w("ConferenceCallsPickerAdapter", "[configureSelection]return,filter = " + contactListFilter + ",directoryId = " + j);
            return;
        }
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        Log.i("ConferenceCallsPickerAdapter", "[configureSelection]filter.filterType = " + contactListFilter.filterType);
        switch (contactListFilter.filterType) {
            case -2:
            case -1:
                Log.d("ConferenceCallsPickerAdapter", "[configureSelection] filterType" + contactListFilter.filterType);
                break;
            default:
                Log.w("ConferenceCallsPickerAdapter", "[configureSelection]Unsupported filter type came (type: " + contactListFilter.filterType + ", toString: " + contactListFilter + ") showing all contacts.");
                break;
        }
        cursorLoader.setSelection(sb.toString());
        cursorLoader.setSelectionArgs((String[]) arrayList.toArray(new String[0]));
    }

    @Override
    public void setSelectedContactsListener(DataKindBasePickerAdapter.SelectedContactsListener selectedContactsListener) {
        Log.d("ConferenceCallsPickerAdapter", "[setSelectedContactsListener]");
        this.mConferenceCallSelectedListener = selectedContactsListener;
    }

    public ArrayList<String> getPhoneNumberByDataIds(long[] jArr) throws Throwable {
        Cursor cursorQuery;
        Throwable th;
        ArrayList<String> arrayList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("_id");
        sb.append(" IN (");
        sb.append(jArr[0]);
        for (int i = 1; i < jArr.length; i++) {
            sb.append(",");
            sb.append(jArr[i]);
        }
        sb.append(")");
        Log.d("ConferenceCallsPickerAdapter", "[getPhoneNumberByDataIds] dataIds " + sb.toString());
        try {
            cursorQuery = this.mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[]{"_id", "data1"}, sb.toString(), null, null);
            if (cursorQuery != null) {
                while (cursorQuery.moveToNext()) {
                    try {
                        Log.sensitive("ConferenceCallsPickerAdapter", "[getPhoneNumberByDataIds] got _ID=" + cursorQuery.getInt(0) + ", NUMBER=" + cursorQuery.getString(1));
                        arrayList.add(cursorQuery.getString(1));
                    } catch (Throwable th2) {
                        th = th2;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return arrayList;
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return arrayList;
        } catch (Throwable th3) {
            cursorQuery = null;
            th = th3;
        }
    }
}
