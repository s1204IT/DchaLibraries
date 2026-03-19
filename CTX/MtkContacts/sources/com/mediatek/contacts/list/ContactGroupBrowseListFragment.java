package com.mediatek.contacts.list;

import android.R;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.util.WeakAsyncTask;
import com.mediatek.contacts.group.GroupBrowseListAdapter;
import com.mediatek.contacts.group.GroupBrowseListFragment;
import com.mediatek.contacts.util.Log;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ContactGroupBrowseListFragment extends GroupBrowseListFragment {
    private Context mContext;

    @Override
    public void onActivityCreated(Bundle bundle) {
        Log.i("ContactGroupBrowseListFragment", "[onActivityCreated]");
        super.onActivityCreated(bundle);
        getListView().setChoiceMode(2);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mContext = null;
    }

    @Override
    protected GroupBrowseListAdapter configAdapter() {
        return new ContactGroupListAdapter(this.mContext);
    }

    @Override
    protected AdapterView.OnItemClickListener configOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
                ((CheckBox) view.findViewById(R.id.checkbox)).setChecked(ContactGroupBrowseListFragment.this.getListView().isItemChecked(i));
                ContactGroupBrowseListFragment.this.updateOkButton(ContactGroupBrowseListFragment.this.getListView().getCheckedItemCount() > 0);
            }
        };
    }

    @Override
    public void onStart() {
        SparseBooleanArray checkedItemPositions = getListView().getCheckedItemPositions();
        if (checkedItemPositions != null) {
            ((ContactGroupListAdapter) getListView().getAdapter()).setSparseBooleanArray(checkedItemPositions);
            updateOkButton(checkedItemPositions.size() > 0);
        }
        super.onStart();
    }

    public void onOkClick() {
        if (getListView().getCheckedItemCount() == 0) {
            Log.w("ContactGroupBrowseListFragment", "[onOkClick]tap OK when no item selected");
            getActivity().setResult(0, null);
            getActivity().finish();
            return;
        }
        ArrayList arrayList = new ArrayList();
        GroupBrowseListAdapter groupBrowseListAdapter = (GroupBrowseListAdapter) getListView().getAdapter();
        int count = getListView().getCount();
        for (int i = 0; i < count; i++) {
            if (getListView().isItemChecked(i)) {
                GroupListItem item = groupBrowseListAdapter.getItem(i);
                if (item != null) {
                    arrayList.add(String.valueOf(item.getGroupId()));
                }
            } else {
                Log.w("ContactGroupBrowseListFragment", "[onOkClick]position " + i + " item is not checked");
            }
        }
        if (arrayList.isEmpty()) {
            Log.w("ContactGroupBrowseListFragment", "[onOkClick]finally, no group selected");
            getActivity().setResult(0, null);
            getActivity().finish();
            return;
        }
        new GroupQueryTask(getActivity()).execute(new List[]{arrayList});
    }

    public class GroupQueryTask extends WeakAsyncTask<List<String>, Void, long[], Activity> {
        private WeakReference<ProgressDialog> mProgress;

        public GroupQueryTask(Activity activity) {
            super(activity);
        }

        @Override
        protected void onPreExecute(Activity activity) {
            Log.d("ContactGroupBrowseListFragment", "[onPreExecute]");
            this.mProgress = new WeakReference<>(ProgressDialog.show(activity, null, activity.getText(com.android.contacts.R.string.please_wait), false, false));
            super.onPreExecute(activity);
        }

        @Override
        protected long[] doInBackground(Activity activity, List<String>... listArr) throws Throwable {
            int i = 0;
            List<String> list = listArr[0];
            Log.d("ContactGroupBrowseListFragment", "[doInBackground]groupIdList = " + list);
            Cursor cursor = null;
            if (list == null || list.isEmpty()) {
                Log.e("ContactGroupBrowseListFragment", "[doInBackground] groupIds is empty");
                return null;
            }
            ArrayList arrayList = new ArrayList();
            StringBuilder sb = new StringBuilder();
            String[] strArr = new String[list.size()];
            Arrays.fill(strArr, "?");
            sb.append("mimetype='vnd.android.cursor.item/group_membership'");
            sb.append(" AND ");
            sb.append("data1 IN (");
            sb.append(TextUtils.join(",", strArr));
            sb.append(")");
            sb.delete(0, sb.length());
            sb.append("(mimetype ='");
            sb.append("vnd.android.cursor.item/phone_v2' OR ");
            sb.append("mimetype ='");
            sb.append("vnd.android.cursor.item/email_v2') ");
            sb.append("AND raw_contact_id IN (" + ("select raw_contact_id from view_data where (" + ((Object) sb) + ")"));
            sb.append(")");
            try {
                Cursor cursorQuery = ContactGroupBrowseListFragment.this.mContext.getContentResolver().query(ContactsContract.Data.CONTENT_URI, new String[]{"_id"}, sb.toString(), (String[]) list.toArray(new String[list.size()]), null);
                if (cursorQuery != null) {
                    try {
                        cursorQuery.moveToPosition(-1);
                        while (cursorQuery.moveToNext()) {
                            arrayList.add(Long.valueOf(cursorQuery.getLong(0)));
                        }
                    } catch (Throwable th) {
                        th = th;
                        cursor = cursorQuery;
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                long[] jArr = new long[arrayList.size()];
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    jArr[i] = ((Long) it.next()).longValue();
                    i++;
                }
                return jArr;
            } catch (Throwable th2) {
                th = th2;
            }
        }

        @Override
        protected void onPostExecute(Activity activity, long[] jArr) {
            Log.d("ContactGroupBrowseListFragment", "[onPostExecute]");
            ProgressDialog progressDialog = this.mProgress.get();
            if (!activity.isFinishing() && progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            super.onPostExecute(activity, jArr);
            if (jArr == null || jArr.length == 0) {
                ContactGroupBrowseListFragment.this.getActivity().setResult(0, new Intent());
            } else {
                ContactGroupBrowseListFragment.this.getActivity().setResult(1, new Intent().putExtra("checkedids", jArr));
            }
            activity.finish();
        }
    }

    private void updateOkButton(boolean z) {
        ((Button) getActivity().findViewById(com.android.contacts.R.id.btn_ok)).setEnabled(z);
    }
}
