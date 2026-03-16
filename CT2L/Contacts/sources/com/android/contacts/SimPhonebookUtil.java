package com.android.contacts;

import android.util.Log;
import com.android.contacts.common.model.RawContactDeltaList;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.util.SimPhoneBookCommonUtil;
import java.util.ArrayList;

public class SimPhonebookUtil {
    public static SimPhoneBookCommonUtil.AdnRecord getSimAdnViaState(RawContactDeltaList state) {
        Log.d("SimPhonebookUtil", "getSimAdnViaState() ");
        Log.v("SimPhonebookUtil", "--> state = " + state);
        SimPhoneBookCommonUtil.AdnRecord adnRec = new SimPhoneBookCommonUtil.AdnRecord();
        if (state != null) {
            ArrayList<ValuesDelta> phoneList = state.get(0).getMimeEntries("vnd.android.cursor.item/phone_v2");
            if (phoneList != null && !phoneList.isEmpty()) {
                for (ValuesDelta vd : phoneList) {
                    Log.v("SimPhonebookUtil", "--> vd.isDelete()= " + vd.isDelete());
                    Log.v("SimPhonebookUtil", "--> vd.isInsert()= " + vd.isInsert());
                    Log.v("SimPhonebookUtil", "--> vd.isUpdate()= " + vd.isUpdate());
                    int phone_type = vd.getAsInteger("data2", 0).intValue();
                    if (phone_type == 2 && vd.getAsString("data1") != null) {
                        if (vd.isDelete()) {
                            adnRec.phone = "";
                        } else {
                            adnRec.phone = vd.getAsString("data1");
                        }
                    } else if (phone_type == 1 && vd.getAsString("data1") != null) {
                        if (vd.isDelete()) {
                            adnRec.phone2 = "";
                        } else {
                            adnRec.phone2 = vd.getAsString("data1");
                        }
                    }
                }
                if (adnRec.phone.isEmpty() && !adnRec.phone2.isEmpty()) {
                    adnRec.phone = adnRec.phone2;
                    adnRec.phone2 = "";
                }
            }
            ArrayList<ValuesDelta> nameList = state.get(0).getMimeEntries("vnd.android.cursor.item/name");
            if (nameList != null && !nameList.isEmpty()) {
                for (ValuesDelta vd2 : nameList) {
                    Log.v("SimPhonebookUtil", "--> name vd.isDelete()= " + vd2.isDelete());
                    if (vd2.isDelete()) {
                        adnRec.name = "";
                    } else {
                        adnRec.name = vd2.getAsString("data1") == null ? "" : vd2.getAsString("data1");
                    }
                }
            }
            ArrayList<ValuesDelta> sneList = state.get(0).getMimeEntries("vnd.android.cursor.item/nickname");
            if (sneList != null && !sneList.isEmpty()) {
                for (ValuesDelta vd3 : sneList) {
                    adnRec.name_sne = vd3.getAsString("data1") == null ? "" : vd3.getAsString("data1");
                }
            }
            ArrayList<ValuesDelta> emailList = state.get(0).getMimeEntries("vnd.android.cursor.item/email_v2");
            if (emailList != null && !emailList.isEmpty()) {
                for (ValuesDelta vd4 : emailList) {
                    int email_type = vd4.getAsInteger("data2", 0).intValue();
                    if (email_type == 4 && vd4.getAsString("data1") != null) {
                        if (vd4.isDelete()) {
                            adnRec.email = "";
                        } else {
                            adnRec.email = vd4.getAsString("data1");
                        }
                    } else if (email_type == 3 && vd4.getAsString("data1") != null) {
                        if (vd4.isDelete()) {
                            adnRec.email2 = "";
                        } else {
                            adnRec.email2 = vd4.getAsString("data1");
                        }
                    }
                }
            }
        }
        return adnRec;
    }
}
