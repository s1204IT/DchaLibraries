package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.telecom.TelecomManager;
import com.android.contacts.CallUtil;
import com.android.contacts.compat.telecom.TelecomManagerCompat;
import com.android.contacts.list.ContactListFilter;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import mediatek.telecom.MtkTelecomManager;

public class ConferenceCallsPickerFragment extends DataKindBasePickerFragment {
    private String mCallingActivity;
    private Intent mIntent;
    private int mReferenceCallMaxNumber = 5;

    @Override
    protected ConferenceCallsPickerAdapter createListAdapter() {
        ConferenceCallsPickerAdapter conferenceCallsPickerAdapter = new ConferenceCallsPickerAdapter(getActivity(), getListView());
        conferenceCallsPickerAdapter.setFilter(ContactListFilter.createFilterWithType(-2));
        this.mIntent = (Intent) getArguments().getParcelable("intent");
        this.mReferenceCallMaxNumber = this.mIntent.getIntExtra("CONFERENCE_CALL_LIMIT_NUMBER", this.mReferenceCallMaxNumber);
        Log.i("ConferenceCallsPickerFragment", "mReferenceCallMaxNumber = " + this.mReferenceCallMaxNumber);
        conferenceCallsPickerAdapter.setRefenceCallMaxNumber(this.mReferenceCallMaxNumber);
        this.mCallingActivity = this.mIntent.getStringExtra("CONFERENCE_SENDER");
        return conferenceCallsPickerAdapter;
    }

    @Override
    protected int getMultiChoiceLimitCount() {
        return this.mReferenceCallMaxNumber;
    }

    @Override
    public void onOptionAction() throws Throwable {
        long[] checkedItemIds = getCheckedItemIds();
        if (checkedItemIds == null || checkedItemIds.length <= 0) {
            Log.w("ConferenceCallsPickerFragment", "[onOptionAction]return,idArray = " + checkedItemIds);
            return;
        }
        Activity activity = getActivity();
        activity.getCallingActivity();
        if ("CONTACTS".equals(this.mCallingActivity)) {
            ArrayList<String> phoneNumberByDataIds = ((ConferenceCallsPickerAdapter) getAdapter()).getPhoneNumberByDataIds(checkedItemIds);
            Intent intentCreateConferenceInvitationIntent = MtkTelecomManager.createConferenceInvitationIntent(getActivity());
            intentCreateConferenceInvitationIntent.setData(CallUtil.getCallUri(phoneNumberByDataIds.get(0)));
            intentCreateConferenceInvitationIntent.putStringArrayListExtra("mediatek.telecom.extra.VOLTE_CONFERENCE_NUMBERS", phoneNumberByDataIds);
            TelecomManagerCompat.placeCall(getActivity(), (TelecomManager) getActivity().getSystemService("telecom"), intentCreateConferenceInvitationIntent);
            Log.sensitive("ConferenceCallsPickerFragment", "[onOptionAction]placeCall():" + phoneNumberByDataIds);
        } else {
            Intent intent = new Intent();
            intent.putExtra("com.mediatek.contacts.list.pickdataresult", checkedItemIds);
            activity.setResult(-1, intent);
        }
        activity.finish();
    }

    @Override
    public void onSelectedContactsChangedViaCheckBox() {
        super.onSelectedContactsChangedViaCheckBox();
    }
}
