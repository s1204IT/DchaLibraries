package com.mediatek.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.TextUtils;

public class MtkContactsContract {

    public interface ContactsColumns {
        public static final String FILTER = "filter";
        public static final int FILTER_NONE = 0;
        public static final int FILTER_WIDGET = 1;
        public static final String INDEX_IN_SIM = "index_in_sim";
        public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";
        public static final String IS_SDN_CONTACT = "is_sdn_contact";
        public static final String SEND_TO_VOICEMAIL_SIP = "send_to_voicemail_sip";
        public static final String SEND_TO_VOICEMAIL_VT = "send_to_voicemail_vt";
    }

    public interface DataColumns {
        public static final String IS_ADDITIONAL_NUMBER = "is_additional_number";
    }

    public static final class DataUsageFeedback {
        public static final String SIM_CONTACT_STATUS = "sim_contact_status";
        public static final int SIM_CONTACT_STATUS_LOADING = 1;
        public static final int SIM_CONTACT_STATUS_NORMAL = 0;
    }

    protected interface DialerSearchColumns {
        public static final String CALL_DATE = "call_date";
        public static final String CALL_LOG_ID = "call_log_id";
        public static final String CALL_NUMBER = "call_number";
        public static final String CALL_PHONE_TYPE = "call_phone_type";
        public static final String CALL_TYPE = "call_type";
        public static final String CONTACT_ID = "contact_id";
        public static final String CONTACT_NAME_LOOKUP = "contact_name_lookup";
        public static final String CONTACT_STARRED = "contact_starred";
        public static final String FIRST_PHONE_NUMBER = "first_phone_number";
        public static final String FIRST_PHONE_TYPE = "first_phone_type";
        public static final String INDICATE_PHONE_SIM = "indicate_phone_sim";
        public static final String NAME = "name";
        public static final String NAME_LOOKUP_ID = "_id";
        public static final String NAME_TYPE = "name_type";
        public static final String NORMALIZED_NAME = "normalized_name";
        public static final String NUMBER_COUNT = "number_count";
        public static final String NUMBER_PRESENTATION = "number_presentation";
        public static final String PHONE_ACCOUNT_COMPONENT_NAME = "phone_account_component_name";
        public static final String PHONE_ACCOUNT_ID = "phone_account_id";
        public static final String PHOTO_ID = "photo_id";
        public static final String SEARCH_DATA_OFFSETS = "search_data_offsets";
        public static final String SEARCH_PHONE_DATA_ID = "search_phone_data_id";
        public static final String SEARCH_PHONE_LABEL = "search_phone_label";
        public static final String SEARCH_PHONE_NUMBER = "search_phone_number";
        public static final String SEARCH_PHONE_TYPE = "search_phone_type";
        public static final String SORT_KEY_PRIMARY = "sort_key";
    }

    public static final class Groups {
        public static final String QUERY_WITH_GROUP_ID = "query_with_group_id";
    }

    public static final class Intents {

        public static final class Insert {
            public static final String IMS_ADDRESS = "ims_address";
            public static final String SIP_ADDRESS = "sip_address";
        }
    }

    public static final class PhoneLookup implements ContactsColumns {
    }

    public static final class ProviderStatus {
        public static final Uri SIM_CONTACT_CONTENT_URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "provider_sim_contact_status");
    }

    public static final class RawContacts {
        public static final int INDICATE_PHONE = -1;
        public static final String TIMESTAMP = "timestamp";
    }

    public interface RawContactsColumns {
        public static final String INDEX_IN_SIM = "index_in_sim";
        public static final String INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";
        public static final String IS_SDN_CONTACT = "is_sdn_contact";
        public static final String SEND_TO_VOICEMAIL_SIP = "send_to_voicemail_sip";
        public static final String SEND_TO_VOICEMAIL_VT = "send_to_voicemail_vt";
    }

    protected interface ViewDialerSearchColumns {
        public static final String CALL_DATE = "vds_call_date";
        public static final String CALL_GEOCODED_LOCATION = "vds_geocoded_location";
        public static final String CALL_LOG_ID = "vds_call_log_id";
        public static final String CALL_TYPE = "vds_call_type";
        public static final String CONTACT_ID = "vds_contact_id";
        public static final String CONTACT_NAME_LOOKUP = "vds_lookup";
        public static final String CONTACT_STARRED = "vds_starred";
        public static final String DS_DATA1 = "vds_data1";
        public static final String DS_DATA2 = "vds_data2";
        public static final String DS_DATA3 = "vds_data3";
        public static final String INDICATE_PHONE_SIM = "vds_indicate_phone_sim";
        public static final String IS_SDN_CONTACT = "vds_is_sdn_contact";
        public static final String NAME = "vds_name";
        public static final String NAME_ALTERNATIVE = "vds_name_alternative";
        public static final String NAME_ID = "vds_name_id";
        public static final String NAME_LOOKUP_ID = "_id";
        public static final String NUMBER_COUNT = "vds_number_count";
        public static final String NUMBER_ID = "vds_number_id";
        public static final String NUMBER_PRESENTATION = "vds_number_presentation";
        public static final String PHONE_ACCOUNT_COMPONENT_NAME = "vds_phone_account_component_name";
        public static final String PHONE_ACCOUNT_ID = "vds_phone_account_id";
        public static final String PHOTO_ID = "vds_photo_id";
        public static final String RAW_CONTACT_ID = "vds_raw_contact_id";
        public static final String SEARCH_DATA_OFFSETS = "search_data_offsets";
        public static final String SEARCH_DATA_OFFSETS_ALTERNATIVE = "search_data_offsets_alternative";
        public static final String SEARCH_PHONE_LABEL = "vds_search_phone_label";
        public static final String SEARCH_PHONE_NUMBER = "vds_phone_number";
        public static final String SEARCH_PHONE_TYPE = "vds_phone_type";
        public static final String SIM_ID = "vds_sim_id";
        public static final String SORT_KEY_ALTERNATIVE = "vds_sort_key_alternative";
        public static final String SORT_KEY_PRIMARY = "vds_sort_key";
        public static final String VTCALL = "vds_vtcall";
    }

    public static final class CommonDataKinds {

        public static final class Phone {
            public static final CharSequence getTypeLabel(Context context, int i, CharSequence charSequence) {
                if (i == 102) {
                    return "";
                }
                if (i == 101) {
                    if (!TextUtils.isEmpty(charSequence)) {
                        return Aas.getLabel(context.getContentResolver(), charSequence);
                    }
                    return "";
                }
                return ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), i, charSequence);
            }
        }

        public static final class ImsCall {
            public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/ims";
            public static final String DATA = "data1";
            public static final String LABEL = "data3";
            public static final String TYPE = "data2";
            public static final String URL = "data1";

            private ImsCall() {
            }
        }
    }

    public static final class DialerSearch implements BaseColumns, ViewDialerSearchColumns {
        public static final String MATCHED_DATA_OFFSET = "matched_data_offset";
        public static final String MATCHED_NAME_OFFSET = "matched_name_offset";

        private DialerSearch() {
        }
    }

    public static final class Aas {
        public static final String AAS_METHOD = "get_aas";
        public static final String ENCODE_SYMBOL = "-";
        public static final String KEY_AAS = "aas";
        public static final String LABEL_EMPTY = "";
        public static final int PHONE_TYPE_AAS = 101;
        public static final int PHONE_TYPE_EMPTY = 102;

        public static final String buildIndicator(int i, int i2) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(i);
            stringBuffer.append(ENCODE_SYMBOL);
            stringBuffer.append(i2);
            return stringBuffer.toString();
        }

        public static CharSequence getLabel(ContentResolver contentResolver, CharSequence charSequence) {
            Bundle bundleCall = contentResolver.call(ContactsContract.AUTHORITY_URI, AAS_METHOD, charSequence.toString(), (Bundle) null);
            if (bundleCall != null) {
                return bundleCall.getCharSequence(KEY_AAS, "");
            }
            return "";
        }
    }
}
