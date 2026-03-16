package com.android.bluetooth.map;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import com.android.bluetooth.map.BluetoothMapUtils;
import com.android.bluetooth.map.BluetoothMapbMessageMms;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.bluetooth.opp.BluetoothShare;
import com.google.android.mms.pdu.CharacterSets;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class BluetoothMapContent {
    private static final boolean D = true;
    public static final String INSERT_ADDRES_TOKEN = "insert-address-token";
    public static final int MAP_MESSAGE_CHARSET_NATIVE = 0;
    public static final int MAP_MESSAGE_CHARSET_UTF8 = 1;
    private static final int MASK_ATTACHMENT_SIZE = 1024;
    private static final int MASK_DATETIME = 2;
    private static final int MASK_PRIORITY = 2048;
    private static final int MASK_PROTECTED = 16384;
    private static final int MASK_READ = 4096;
    private static final int MASK_RECEPTION_STATUS = 256;
    private static final int MASK_RECIPIENT_ADDRESSING = 32;
    private static final int MASK_RECIPIENT_NAME = 16;
    private static final int MASK_REPLYTO_ADDRESSING = 32768;
    private static final int MASK_SENDER_ADDRESSING = 8;
    private static final int MASK_SENDER_NAME = 4;
    private static final int MASK_SENT = 8192;
    private static final int MASK_SIZE = 128;
    private static final int MASK_SUBJECT = 1;
    private static final int MASK_TEXT = 512;
    private static final int MASK_TYPE = 64;
    public static final int MMS_BCC = 129;
    public static final int MMS_CC = 130;
    public static final int MMS_FROM = 137;
    public static final int MMS_TO = 151;
    private static final String TAG = "BluetoothMapContent";
    private static final boolean V = false;
    private String mBaseEmailUri;
    private Context mContext;
    private ContentResolver mResolver;
    static final String[] SMS_PROJECTION = {"_id", "thread_id", "address", "body", "date", "read", "type", BluetoothShare.STATUS, "locked", "error_code"};
    static final String[] MMS_PROJECTION = {"_id", "thread_id", "m_id", "m_size", "sub", "ct_t", "text_only", "date", "date_sent", "read", "msg_box", "st", "pri"};

    private class FilterInfo {
        public static final int TYPE_EMAIL = 2;
        public static final int TYPE_MMS = 1;
        public static final int TYPE_SMS = 0;
        public int mEmailColAttachementSize;
        public int mEmailColAttachment;
        public int mEmailColBccAddress;
        public int mEmailColCcAddress;
        public int mEmailColDate;
        public int mEmailColFolder;
        public int mEmailColFromAddress;
        public int mEmailColId;
        public int mEmailColPriority;
        public int mEmailColProtected;
        public int mEmailColRead;
        public int mEmailColSize;
        public int mEmailColSubject;
        public int mEmailColThreadId;
        public int mEmailColToAddress;
        public int mMmsColAttachmentSize;
        public int mMmsColDate;
        public int mMmsColFolder;
        public int mMmsColId;
        public int mMmsColRead;
        public int mMmsColSize;
        public int mMmsColSubject;
        public int mMmsColTextOnly;
        int mMsgType;
        String mPhoneAlphaTag;
        String mPhoneNum;
        int mPhoneType;
        public int mSmsColAddress;
        public int mSmsColDate;
        public int mSmsColFolder;
        public int mSmsColId;
        public int mSmsColRead;
        public int mSmsColSubject;
        public int mSmsColType;

        private FilterInfo() {
            this.mMsgType = 0;
            this.mPhoneType = 0;
            this.mPhoneNum = null;
            this.mPhoneAlphaTag = null;
            this.mEmailColThreadId = -1;
            this.mEmailColProtected = -1;
            this.mEmailColFolder = -1;
            this.mMmsColFolder = -1;
            this.mSmsColFolder = -1;
            this.mEmailColRead = -1;
            this.mSmsColRead = -1;
            this.mMmsColRead = -1;
            this.mEmailColPriority = -1;
            this.mMmsColAttachmentSize = -1;
            this.mEmailColAttachment = -1;
            this.mEmailColAttachementSize = -1;
            this.mMmsColTextOnly = -1;
            this.mMmsColId = -1;
            this.mSmsColId = -1;
            this.mEmailColSize = -1;
            this.mSmsColSubject = -1;
            this.mMmsColSize = -1;
            this.mEmailColToAddress = -1;
            this.mEmailColCcAddress = -1;
            this.mEmailColBccAddress = -1;
            this.mSmsColAddress = -1;
            this.mSmsColDate = -1;
            this.mMmsColDate = -1;
            this.mEmailColDate = -1;
            this.mMmsColSubject = -1;
            this.mEmailColSubject = -1;
            this.mSmsColType = -1;
            this.mEmailColFromAddress = -1;
            this.mEmailColId = -1;
        }

        public void setEmailColumns(Cursor c) {
            this.mEmailColThreadId = c.getColumnIndex("thread_id");
            this.mEmailColProtected = c.getColumnIndex("flag_protected");
            this.mEmailColFolder = c.getColumnIndex("folder_id");
            this.mEmailColRead = c.getColumnIndex("flag_read");
            this.mEmailColPriority = c.getColumnIndex("high_priority");
            this.mEmailColAttachment = c.getColumnIndex("flag_attachment");
            this.mEmailColAttachementSize = c.getColumnIndex("attachment_size");
            this.mEmailColSize = c.getColumnIndex("message_size");
            this.mEmailColToAddress = c.getColumnIndex("to_list");
            this.mEmailColCcAddress = c.getColumnIndex("cc_list");
            this.mEmailColBccAddress = c.getColumnIndex("bcc_list");
            this.mEmailColDate = c.getColumnIndex("date");
            this.mEmailColSubject = c.getColumnIndex("subject");
            this.mEmailColFromAddress = c.getColumnIndex("from_list");
            this.mEmailColId = c.getColumnIndex("_id");
        }

        public void setSmsColumns(Cursor c) {
            this.mSmsColId = c.getColumnIndex("_id");
            this.mSmsColFolder = c.getColumnIndex("type");
            this.mSmsColRead = c.getColumnIndex("read");
            this.mSmsColSubject = c.getColumnIndex("body");
            this.mSmsColAddress = c.getColumnIndex("address");
            this.mSmsColDate = c.getColumnIndex("date");
            this.mSmsColType = c.getColumnIndex("type");
        }

        public void setMmsColumns(Cursor c) {
            this.mMmsColId = c.getColumnIndex("_id");
            this.mMmsColFolder = c.getColumnIndex("msg_box");
            this.mMmsColRead = c.getColumnIndex("read");
            this.mMmsColAttachmentSize = c.getColumnIndex("m_size");
            this.mMmsColTextOnly = c.getColumnIndex("text_only");
            this.mMmsColSize = c.getColumnIndex("m_size");
            this.mMmsColDate = c.getColumnIndex("date");
            this.mMmsColSubject = c.getColumnIndex("sub");
        }
    }

    public BluetoothMapContent(Context context, String emailBaseUri) {
        this.mBaseEmailUri = null;
        this.mContext = context;
        this.mResolver = this.mContext.getContentResolver();
        if (this.mResolver == null) {
            Log.d(TAG, "getContentResolver failed");
        }
        this.mBaseEmailUri = emailBaseUri;
    }

    private static void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    private void setProtected(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 16384) != 0) {
            String protect = "no";
            if (fi.mMsgType == 2) {
                int flagProtected = c.getInt(fi.mEmailColProtected);
                if (flagProtected == 1) {
                    protect = "yes";
                }
            }
            e.setProtect(protect);
        }
    }

    private void setThreadId(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if (fi.mMsgType == 2) {
            long threadId = c.getLong(fi.mEmailColThreadId);
            e.setThreadId(threadId);
        }
    }

    private void setSent(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String sent;
        if ((ap.getParameterMask() & 8192) != 0) {
            int msgType = 0;
            if (fi.mMsgType == 0) {
                msgType = c.getInt(fi.mSmsColFolder);
            } else if (fi.mMsgType == 1) {
                msgType = c.getInt(fi.mMmsColFolder);
            } else if (fi.mMsgType == 2) {
                msgType = c.getInt(fi.mEmailColFolder);
            }
            if (msgType == 2) {
                sent = "yes";
            } else {
                sent = "no";
            }
            e.setSent(sent);
        }
    }

    private void setRead(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        int read = 0;
        if (fi.mMsgType == 0) {
            read = c.getInt(fi.mSmsColRead);
        } else if (fi.mMsgType == 1) {
            read = c.getInt(fi.mMmsColRead);
        } else if (fi.mMsgType == 2) {
            read = c.getInt(fi.mEmailColRead);
        }
        e.setRead(read == 1, (ap.getParameterMask() & 4096) != 0);
    }

    private void setPriority(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 2048) != 0) {
            String priority = "no";
            if (fi.mMsgType == 2) {
                int highPriority = c.getInt(fi.mEmailColPriority);
                if (highPriority == 1) {
                    priority = "yes";
                }
            }
            int pri = 0;
            if (fi.mMsgType == 1) {
                pri = c.getInt(c.getColumnIndex("pri"));
            }
            if (pri == 130) {
                priority = "yes";
            }
            e.setPriority(priority);
        }
    }

    private void setAttachmentSize(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 1024) != 0) {
            int size = 0;
            if (fi.mMsgType == 1) {
                if (c.getInt(fi.mMmsColTextOnly) == 0 && (size = c.getInt(fi.mMmsColAttachmentSize)) <= 0) {
                    Log.d(TAG, "Error in message database, size reported as: " + size + " Changing size to 1");
                    size = 1;
                }
            } else if (fi.mMsgType == 2) {
                int attachment = c.getInt(fi.mEmailColAttachment);
                size = c.getInt(fi.mEmailColAttachementSize);
                if (attachment == 1 && size == 0) {
                    Log.d(TAG, "Error in message database, attachment size reported as: " + size + " Changing size to 1");
                    size = 1;
                }
            }
            e.setAttachmentSize(size);
        }
    }

    private void setText(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 512) != 0) {
            String hasText = "";
            if (fi.mMsgType == 0) {
                hasText = "yes";
            } else if (fi.mMsgType == 1) {
                int textOnly = c.getInt(fi.mMmsColTextOnly);
                if (textOnly == 1) {
                    hasText = "yes";
                } else {
                    long id = c.getLong(fi.mMmsColId);
                    String text = getTextPartsMms(id);
                    if (text != null && text.length() > 0) {
                        hasText = "yes";
                    } else {
                        hasText = "no";
                    }
                }
            } else if (fi.mMsgType == 2) {
                hasText = "yes";
            }
            e.setText(hasText);
        }
    }

    private void setReceptionStatus(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 256) != 0) {
            e.setReceptionStatus("complete");
        }
    }

    private void setSize(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 128) != 0) {
            int size = 0;
            if (fi.mMsgType == 0) {
                String subject = c.getString(fi.mSmsColSubject);
                size = subject.length();
            } else if (fi.mMsgType == 1) {
                size = c.getInt(fi.mMmsColSize);
            } else if (fi.mMsgType == 2) {
                size = c.getInt(fi.mEmailColSize);
            }
            if (size <= 0) {
                Log.d(TAG, "Error in message database, size reported as: " + size + " Changing size to 1");
                size = 1;
            }
            e.setSize(size);
        }
    }

    private void setType(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 64) != 0) {
            BluetoothMapUtils.TYPE type = null;
            if (fi.mMsgType == 0) {
                if (fi.mPhoneType == 1) {
                    type = BluetoothMapUtils.TYPE.SMS_GSM;
                } else if (fi.mPhoneType == 2) {
                    type = BluetoothMapUtils.TYPE.SMS_CDMA;
                }
            } else if (fi.mMsgType == 1) {
                type = BluetoothMapUtils.TYPE.MMS;
            } else if (fi.mMsgType == 2) {
                type = BluetoothMapUtils.TYPE.EMAIL;
            }
            e.setType(type);
        }
    }

    private String setRecipientAddressingEmail(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi) {
        String toAddress = c.getString(fi.mEmailColToAddress);
        String ccAddress = c.getString(fi.mEmailColCcAddress);
        String bccAddress = c.getString(fi.mEmailColBccAddress);
        String address = "";
        if (toAddress != null) {
            address = "" + toAddress;
            if (ccAddress != null) {
                address = address + ",";
            }
        }
        if (ccAddress != null) {
            address = address + ccAddress;
            if (bccAddress != null) {
                address = address + ",";
            }
        }
        if (bccAddress != null) {
            return address + bccAddress;
        }
        return address;
    }

    private void setRecipientAddressing(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 32) != 0) {
            String address = null;
            if (fi.mMsgType == 0) {
                int msgType = c.getInt(fi.mSmsColType);
                if (msgType == 1) {
                    address = fi.mPhoneNum;
                } else {
                    address = c.getString(c.getColumnIndex("address"));
                }
            } else if (fi.mMsgType == 1) {
                long id = c.getLong(c.getColumnIndex("_id"));
                address = getAddressMms(this.mResolver, id, MMS_TO);
            } else if (fi.mMsgType == 2) {
                address = setRecipientAddressingEmail(e, c, fi);
            }
            if (address == null) {
                address = "";
            }
            e.setRecipientAddressing(address);
        }
    }

    private void setRecipientName(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String phone;
        if ((ap.getParameterMask() & 16) != 0) {
            String name = null;
            if (fi.mMsgType == 0) {
                int msgType = c.getInt(fi.mSmsColType);
                if (msgType != 1) {
                    String phone2 = c.getString(fi.mSmsColAddress);
                    if (phone2 != null && !phone2.isEmpty()) {
                        name = getContactNameFromPhone(phone2);
                    }
                } else {
                    name = fi.mPhoneAlphaTag;
                }
            } else if (fi.mMsgType == 1) {
                long id = c.getLong(fi.mMmsColId);
                if (e.getRecipientAddressing() != null) {
                    phone = getAddressMms(this.mResolver, id, MMS_TO);
                } else {
                    phone = e.getRecipientAddressing();
                }
                if (phone != null && !phone.isEmpty()) {
                    name = getContactNameFromPhone(phone);
                }
            } else if (fi.mMsgType == 2) {
                name = setRecipientAddressingEmail(e, c, fi);
            }
            if (name == null) {
                name = "";
            }
            e.setRecipientName(name);
        }
    }

    private void setSenderAddressing(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String tempAddress;
        if ((ap.getParameterMask() & 8) != 0) {
            String address = null;
            if (fi.mMsgType == 0) {
                int msgType = c.getInt(fi.mSmsColType);
                if (msgType == 1) {
                    tempAddress = c.getString(fi.mSmsColAddress);
                } else {
                    tempAddress = fi.mPhoneNum;
                }
                if (tempAddress != null) {
                    address = PhoneNumberUtils.extractNetworkPortion(tempAddress);
                    Boolean alpha = Boolean.valueOf(PhoneNumberUtils.stripSeparators(tempAddress).matches("[0-9]*[a-zA-Z]+[0-9]*"));
                    if (address == null || address.length() < 2 || alpha.booleanValue()) {
                        address = tempAddress;
                    }
                }
            } else if (fi.mMsgType == 1) {
                long id = c.getLong(fi.mMmsColId);
                String tempAddress2 = getAddressMms(this.mResolver, id, MMS_FROM);
                address = PhoneNumberUtils.extractNetworkPortion(tempAddress2);
                if (address == null || address.length() < 1) {
                    address = tempAddress2;
                }
            } else if (fi.mMsgType == 2) {
                address = c.getString(fi.mEmailColFromAddress);
            }
            if (address == null) {
                address = "";
            }
            e.setSenderAddressing(address);
        }
    }

    private void setSenderName(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String phone;
        if ((ap.getParameterMask() & 4) != 0) {
            String name = null;
            if (fi.mMsgType == 0) {
                int msgType = c.getInt(c.getColumnIndex("type"));
                if (msgType == 1) {
                    String phone2 = c.getString(fi.mSmsColAddress);
                    if (phone2 != null && !phone2.isEmpty()) {
                        name = getContactNameFromPhone(phone2);
                    }
                } else {
                    name = fi.mPhoneAlphaTag;
                }
            } else if (fi.mMsgType == 1) {
                long id = c.getLong(fi.mMmsColId);
                if (e.getSenderAddressing() != null) {
                    phone = getAddressMms(this.mResolver, id, MMS_FROM);
                } else {
                    phone = e.getSenderAddressing();
                }
                if (phone != null && !phone.isEmpty()) {
                    name = getContactNameFromPhone(phone);
                }
            } else if (fi.mMsgType == 2) {
                name = c.getString(fi.mEmailColFromAddress);
            }
            if (name == null) {
                name = "";
            }
            e.setSenderName(name);
        }
    }

    private void setDateTime(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        if ((ap.getParameterMask() & 2) != 0) {
            long date = 0;
            if (fi.mMsgType == 0) {
                date = c.getLong(fi.mSmsColDate);
            } else if (fi.mMsgType == 1) {
                date = c.getLong(fi.mMmsColDate) * 1000;
            } else if (fi.mMsgType == 2) {
                date = c.getLong(fi.mEmailColDate);
            }
            e.setDateTime(date);
        }
    }

    private String getTextPartsMms(long id) {
        String part;
        String text = "";
        String selection = new String("mid=" + id);
        String uriStr = new String(Telephony.Mms.CONTENT_URI + "/" + id + "/part");
        Uri uriAddress = Uri.parse(uriStr);
        Cursor c = this.mResolver.query(uriAddress, null, selection, null, null);
        while (c != null) {
            try {
                if (!c.moveToNext()) {
                    break;
                }
                String ct = c.getString(c.getColumnIndex("ct"));
                if (ct.equals("text/plain") && (part = c.getString(c.getColumnIndex("text"))) != null) {
                    text = text + part;
                }
            } finally {
                close(c);
            }
        }
        return text;
    }

    private void setSubject(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String subject = "";
        int subLength = ap.getSubjectLength();
        if (subLength == -1) {
            subLength = MASK_RECEPTION_STATUS;
        }
        if ((ap.getParameterMask() & 1) != 0) {
            if (fi.mMsgType == 0) {
                subject = c.getString(fi.mSmsColSubject);
            } else if (fi.mMsgType == 1) {
                subject = c.getString(fi.mMmsColSubject);
                if (subject == null || subject.length() == 0) {
                    long id = c.getLong(fi.mMmsColId);
                    subject = getTextPartsMms(id);
                }
            } else if (fi.mMsgType == 2) {
                subject = c.getString(fi.mEmailColSubject);
            }
            if (subject != null && subject.length() > subLength) {
                subject = subject.substring(0, subLength);
            }
            e.setSubject(subject);
        }
    }

    private void setHandle(BluetoothMapMessageListingElement e, Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        long handle = -1;
        if (fi.mMsgType == 0) {
            handle = c.getLong(fi.mSmsColId);
        } else if (fi.mMsgType == 1) {
            handle = c.getLong(fi.mMmsColId);
        } else if (fi.mMsgType == 2) {
            handle = c.getLong(fi.mEmailColId);
        }
        e.setHandle(handle);
    }

    private BluetoothMapMessageListingElement element(Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        BluetoothMapMessageListingElement e = new BluetoothMapMessageListingElement();
        setHandle(e, c, fi, ap);
        setDateTime(e, c, fi, ap);
        setType(e, c, fi, ap);
        setRead(e, c, fi, ap);
        e.setCursorIndex(c.getPosition());
        return e;
    }

    private String getContactNameFromPhone(String phone) {
        String name = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
        String[] projection = {"_id", "display_name"};
        Cursor c = this.mResolver.query(uri, projection, "in_visible_group=1", null, "display_name ASC");
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    name = c.getString(c.getColumnIndex("display_name"));
                }
            } finally {
                close(c);
            }
        }
        return name;
    }

    public static String getAddressMms(ContentResolver r, long id, int type) {
        String selection = new String("msg_id=" + id + " AND type=" + type);
        String uriStr = new String(Telephony.Mms.CONTENT_URI + "/" + id + "/addr");
        Uri uriAddress = Uri.parse(uriStr);
        String addr = null;
        Cursor c = r.query(uriAddress, null, selection, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    addr = c.getString(c.getColumnIndex("address"));
                    if (addr.equals(INSERT_ADDRES_TOKEN)) {
                        addr = "";
                    }
                }
            } finally {
                close(c);
            }
        }
        return addr;
    }

    private boolean matchRecipientMms(Cursor c, FilterInfo fi, String recip) {
        long id = c.getLong(c.getColumnIndex("_id"));
        String phone = getAddressMms(this.mResolver, id, MMS_TO);
        if (phone != null && phone.length() > 0) {
            if (phone.matches(recip)) {
                return true;
            }
            String name = getContactNameFromPhone(phone);
            if (name != null && name.length() > 0 && name.matches(recip)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean matchRecipientSms(Cursor c, FilterInfo fi, String recip) {
        int msgType = c.getInt(c.getColumnIndex("type"));
        if (msgType == 1) {
            String phone = fi.mPhoneNum;
            String name = fi.mPhoneAlphaTag;
            if (phone != null && phone.length() > 0 && phone.matches(recip)) {
                return true;
            }
            if (name != null && name.length() > 0 && name.matches(recip)) {
                return true;
            }
            return false;
        }
        String phone2 = c.getString(c.getColumnIndex("address"));
        if (phone2 != null && phone2.length() > 0) {
            if (phone2.matches(recip)) {
                return true;
            }
            String name2 = getContactNameFromPhone(phone2);
            if (name2 != null && name2.length() > 0 && name2.matches(recip)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean matchRecipient(Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String recip = ap.getFilterRecipient();
        if (recip != null && recip.length() > 0) {
            String recip2 = ".*" + recip.replace("*", ".*") + ".*";
            if (fi.mMsgType == 0) {
                boolean res = matchRecipientSms(c, fi, recip2);
                return res;
            }
            if (fi.mMsgType == 1) {
                boolean res2 = matchRecipientMms(c, fi, recip2);
                return res2;
            }
            Log.d(TAG, "matchRecipient: Unknown msg type: " + fi.mMsgType);
            return false;
        }
        return true;
    }

    private boolean matchOriginatorMms(Cursor c, FilterInfo fi, String orig) {
        long id = c.getLong(c.getColumnIndex("_id"));
        String phone = getAddressMms(this.mResolver, id, MMS_FROM);
        if (phone != null && phone.length() > 0) {
            if (phone.matches(orig)) {
                return true;
            }
            String name = getContactNameFromPhone(phone);
            if (name != null && name.length() > 0 && name.matches(orig)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean matchOriginatorSms(Cursor c, FilterInfo fi, String orig) {
        int msgType = c.getInt(c.getColumnIndex("type"));
        if (msgType == 1) {
            String phone = c.getString(c.getColumnIndex("address"));
            if (phone != null && phone.length() > 0) {
                if (phone.matches(orig)) {
                    return true;
                }
                String name = getContactNameFromPhone(phone);
                if (name != null && name.length() > 0 && name.matches(orig)) {
                    return true;
                }
                return false;
            }
            return false;
        }
        String phone2 = fi.mPhoneNum;
        String name2 = fi.mPhoneAlphaTag;
        if (phone2 != null && phone2.length() > 0 && phone2.matches(orig)) {
            return true;
        }
        if (name2 != null && name2.length() > 0 && name2.matches(orig)) {
            return true;
        }
        return false;
    }

    private boolean matchOriginator(Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        String orig = ap.getFilterOriginator();
        if (orig != null && orig.length() > 0) {
            String orig2 = ".*" + orig.replace("*", ".*") + ".*";
            if (fi.mMsgType == 0) {
                boolean res = matchOriginatorSms(c, fi, orig2);
                return res;
            }
            if (fi.mMsgType == 1) {
                boolean res2 = matchOriginatorMms(c, fi, orig2);
                return res2;
            }
            Log.d(TAG, "matchOriginator: Unknown msg type: " + fi.mMsgType);
            return false;
        }
        return true;
    }

    private boolean matchAddresses(Cursor c, FilterInfo fi, BluetoothMapAppParams ap) {
        return matchOriginator(c, fi, ap) && matchRecipient(c, fi, ap);
    }

    private String setWhereFilterFolderTypeSms(String folder) {
        if ("inbox".equalsIgnoreCase(folder)) {
            return "type = 1 AND thread_id <> -1";
        }
        if ("outbox".equalsIgnoreCase(folder)) {
            return "(type = 4 OR type = 5 OR type = 6) AND thread_id <> -1";
        }
        if ("sent".equalsIgnoreCase(folder)) {
            return "type = 2 AND thread_id <> -1";
        }
        if ("draft".equalsIgnoreCase(folder)) {
            return "type = 3 AND thread_id <> -1";
        }
        if (!"deleted".equalsIgnoreCase(folder)) {
            return "";
        }
        return "thread_id = -1";
    }

    private String setWhereFilterFolderTypeMms(String folder) {
        if ("inbox".equalsIgnoreCase(folder)) {
            return "msg_box = 1 AND thread_id <> -1";
        }
        if ("outbox".equalsIgnoreCase(folder)) {
            return "msg_box = 4 AND thread_id <> -1";
        }
        if ("sent".equalsIgnoreCase(folder)) {
            return "msg_box = 2 AND thread_id <> -1";
        }
        if ("draft".equalsIgnoreCase(folder)) {
            return "msg_box = 3 AND thread_id <> -1";
        }
        if (!"deleted".equalsIgnoreCase(folder)) {
            return "";
        }
        return "thread_id = -1";
    }

    private String setWhereFilterFolderTypeEmail(long folderId) {
        if (folderId >= 0) {
            String where = "folder_id = " + folderId;
            return where;
        }
        Log.e(TAG, "setWhereFilterFolderTypeEmail: not valid!");
        throw new IllegalArgumentException("Invalid folder ID");
    }

    private String setWhereFilterFolderType(BluetoothMapFolderElement folderElement, FilterInfo fi) {
        if (fi.mMsgType == 0) {
            String where = setWhereFilterFolderTypeSms(folderElement.getName());
            return where;
        }
        if (fi.mMsgType == 1) {
            String where2 = setWhereFilterFolderTypeMms(folderElement.getName());
            return where2;
        }
        if (fi.mMsgType != 2) {
            return "";
        }
        String where3 = setWhereFilterFolderTypeEmail(folderElement.getEmailFolderId());
        return where3;
    }

    private String setWhereFilterReadStatus(BluetoothMapAppParams ap, FilterInfo fi) {
        String where = "";
        if (ap.getFilterReadStatus() == -1) {
            return "";
        }
        if (fi.mMsgType == 0) {
            if ((ap.getFilterReadStatus() & 1) != 0) {
                where = " AND read= 0";
            }
            if ((ap.getFilterReadStatus() & 2) != 0) {
                return " AND read= 1";
            }
            return where;
        }
        if (fi.mMsgType == 1) {
            if ((ap.getFilterReadStatus() & 1) != 0) {
                where = " AND read= 0";
            }
            if ((ap.getFilterReadStatus() & 2) != 0) {
                return " AND read= 1";
            }
            return where;
        }
        if (fi.mMsgType != 2) {
            return "";
        }
        if ((ap.getFilterReadStatus() & 1) != 0) {
            where = " AND flag_read= 0";
        }
        if ((ap.getFilterReadStatus() & 2) != 0) {
            return " AND flag_read= 1";
        }
        return where;
    }

    private String setWhereFilterPeriod(BluetoothMapAppParams ap, FilterInfo fi) {
        String where = "";
        if (ap.getFilterPeriodBegin() != -1) {
            if (fi.mMsgType == 0) {
                where = " AND date >= " + ap.getFilterPeriodBegin();
            } else if (fi.mMsgType == 1) {
                where = " AND date >= " + (ap.getFilterPeriodBegin() / 1000);
            } else if (fi.mMsgType == 2) {
                where = " AND date >= " + ap.getFilterPeriodBegin();
            }
        }
        if (ap.getFilterPeriodEnd() != -1) {
            if (fi.mMsgType == 0) {
                return where + " AND date < " + ap.getFilterPeriodEnd();
            }
            if (fi.mMsgType == 1) {
                return where + " AND date < " + (ap.getFilterPeriodEnd() / 1000);
            }
            if (fi.mMsgType == 2) {
                return where + " AND date < " + ap.getFilterPeriodEnd();
            }
            return where;
        }
        return where;
    }

    private String setWhereFilterPhones(String str) {
        String where = "";
        String str2 = str.replace("*", "%");
        Cursor p = this.mResolver.query(ContactsContract.Contacts.CONTENT_URI, null, "display_name like ?", new String[]{str2}, "display_name ASC");
        while (p != null) {
            try {
                if (!p.moveToNext()) {
                    break;
                }
                String contactId = p.getString(p.getColumnIndex("_id"));
                p = this.mResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, "contact_id = ?", new String[]{contactId}, null);
                while (p != null) {
                    if (!p.moveToNext()) {
                        break;
                    }
                    String number = p.getString(p.getColumnIndex("data1"));
                    where = where + " address = '" + number + "'";
                    if (!p.isLast()) {
                        where = where + " OR ";
                    }
                }
                close(p);
                if (!p.isLast()) {
                    where = where + " OR ";
                }
            } catch (Throwable th) {
                throw th;
            } finally {
                close(p);
            }
        }
        if (str2 != null && str2.length() > 0) {
            if (where.length() > 0) {
                where = where + " OR ";
            }
            return where + " address like '" + str2 + "'";
        }
        return where;
    }

    private String setWhereFilterOriginatorEmail(BluetoothMapAppParams ap) {
        String orig = ap.getFilterOriginator();
        if (orig == null || orig.length() <= 0) {
            return "";
        }
        String where = " AND from_list LIKE '%" + orig.replace("*", "%") + "%'";
        return where;
    }

    private String setWhereFilterPriority(BluetoothMapAppParams ap, FilterInfo fi) {
        int pri = ap.getFilterPriority();
        if (fi.mMsgType != 1) {
            return "";
        }
        if (pri == 2) {
            String where = " AND pri<=" + Integer.toString(MMS_BCC);
            return where;
        }
        if (pri == 1) {
            String where2 = " AND pri=" + Integer.toString(MMS_CC);
            return where2;
        }
        return "";
    }

    private String setWhereFilterRecipientEmail(BluetoothMapAppParams ap) {
        String recip = ap.getFilterRecipient();
        if (recip == null || recip.length() <= 0) {
            return "";
        }
        String recip2 = recip.replace("*", "%");
        String where = " AND (to_list LIKE '%" + recip2 + "%' OR cc_list LIKE '%" + recip2 + "%' OR bcc_list LIKE '%" + recip2 + "%' )";
        return where;
    }

    private String setWhereFilter(BluetoothMapFolderElement folderElement, FilterInfo fi, BluetoothMapAppParams ap) {
        String where = "" + setWhereFilterFolderType(folderElement, fi);
        if (!where.isEmpty()) {
            String where2 = ((where + setWhereFilterReadStatus(ap, fi)) + setWhereFilterPeriod(ap, fi)) + setWhereFilterPriority(ap, fi);
            if (fi.mMsgType == 2) {
                return (where2 + setWhereFilterOriginatorEmail(ap)) + setWhereFilterRecipientEmail(ap);
            }
            return where2;
        }
        return where;
    }

    private boolean smsSelected(FilterInfo fi, BluetoothMapAppParams ap) {
        int msgType = ap.getFilterMessageType();
        int phoneType = fi.mPhoneType;
        Log.d(TAG, "smsSelected msgType: " + msgType);
        if (msgType == -1 || (msgType & 3) == 0) {
            return true;
        }
        if ((msgType & 1) == 0 && phoneType == 1) {
            return true;
        }
        return (msgType & 2) == 0 && phoneType == 2;
    }

    private boolean mmsSelected(FilterInfo fi, BluetoothMapAppParams ap) {
        int msgType = ap.getFilterMessageType();
        Log.d(TAG, "mmsSelected msgType: " + msgType);
        return msgType == -1 || (msgType & 8) == 0;
    }

    private boolean emailSelected(FilterInfo fi, BluetoothMapAppParams ap) {
        int msgType = ap.getFilterMessageType();
        Log.d(TAG, "emailSelected msgType: " + msgType);
        return msgType == -1 || (msgType & 4) == 0;
    }

    private void setFilterInfo(FilterInfo fi) {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        if (tm != null) {
            fi.mPhoneType = tm.getPhoneType();
            fi.mPhoneNum = tm.getLine1Number();
            fi.mPhoneAlphaTag = tm.getLine1AlphaTag();
            Log.d(TAG, "phone type = " + fi.mPhoneType + " phone num = " + fi.mPhoneNum + " phone alpha tag = " + fi.mPhoneAlphaTag);
        }
    }

    public BluetoothMapMessageListing msgListing(BluetoothMapFolderElement folderElement, BluetoothMapAppParams ap) {
        Log.d(TAG, "msgListing: folderName = " + folderElement.getName() + " folderId = " + folderElement.getEmailFolderId() + " messageType = " + ap.getFilterMessageType());
        BluetoothMapMessageListing bmList = new BluetoothMapMessageListing();
        if (ap.getParameterMask() == -1 || ap.getParameterMask() == 0) {
            ap.setParameterMask(BluetoothMapAppParams.PARAMETER_MASK_ALL_ENABLED);
        }
        FilterInfo fi = new FilterInfo();
        setFilterInfo(fi);
        Cursor smsCursor = null;
        Cursor mmsCursor = null;
        Cursor emailCursor = null;
        try {
            String limit = "";
            ap.getMaxListCount();
            int offsetNum = ap.getStartOffset();
            if (ap.getMaxListCount() > 0) {
                limit = " LIMIT " + (ap.getMaxListCount() + ap.getStartOffset());
            }
            if (smsSelected(fi, ap) && folderElement.hasSmsMmsContent()) {
                if (ap.getFilterMessageType() == 13 || ap.getFilterMessageType() == 14) {
                    limit = " LIMIT " + ap.getMaxListCount() + " OFFSET " + ap.getStartOffset();
                    Log.d(TAG, "SMS Limit => " + limit);
                    offsetNum = 0;
                }
                fi.mMsgType = 0;
                if (ap.getFilterPriority() != 1) {
                    String where = setWhereFilter(folderElement, fi, ap);
                    if (!where.isEmpty()) {
                        Log.d(TAG, "msgType: " + fi.mMsgType);
                        smsCursor = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, where, null, "date DESC" + limit);
                        if (smsCursor != null) {
                            Log.d(TAG, "Found " + smsCursor.getCount() + " sms messages.");
                            fi.setSmsColumns(smsCursor);
                            while (smsCursor.moveToNext()) {
                                if (matchAddresses(smsCursor, fi, ap)) {
                                    BluetoothMapMessageListingElement e = element(smsCursor, fi, ap);
                                    bmList.add(e);
                                }
                            }
                        }
                    }
                }
            }
            if (mmsSelected(fi, ap) && folderElement.hasSmsMmsContent()) {
                if (ap.getFilterMessageType() == 7) {
                    limit = " LIMIT " + ap.getMaxListCount() + " OFFSET " + ap.getStartOffset();
                    Log.d(TAG, "MMS Limit => " + limit);
                    offsetNum = 0;
                }
                fi.mMsgType = 1;
                String where2 = setWhereFilter(folderElement, fi, ap);
                if (!where2.isEmpty()) {
                    Log.d(TAG, "msgType: " + fi.mMsgType);
                    mmsCursor = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, where2, null, "date DESC" + limit);
                    if (mmsCursor != null) {
                        fi.setMmsColumns(mmsCursor);
                        Log.d(TAG, "Found " + mmsCursor.getCount() + " mms messages.");
                        while (mmsCursor.moveToNext()) {
                            if (matchAddresses(mmsCursor, fi, ap)) {
                                BluetoothMapMessageListingElement e2 = element(mmsCursor, fi, ap);
                                bmList.add(e2);
                            }
                        }
                    }
                }
            }
            if (emailSelected(fi, ap) && folderElement.getEmailFolderId() != -1) {
                if (ap.getFilterMessageType() == 11) {
                    limit = " LIMIT " + ap.getMaxListCount() + " OFFSET " + ap.getStartOffset();
                    Log.d(TAG, "Email Limit => " + limit);
                    offsetNum = 0;
                }
                fi.mMsgType = 2;
                String where3 = setWhereFilter(folderElement, fi, ap);
                if (!where3.isEmpty()) {
                    Log.d(TAG, "msgType: " + fi.mMsgType);
                    Uri contentUri = Uri.parse(this.mBaseEmailUri + "Message");
                    emailCursor = this.mResolver.query(contentUri, BluetoothMapContract.BT_MESSAGE_PROJECTION, where3, null, "date DESC" + limit);
                    if (emailCursor != null) {
                        fi.setEmailColumns(emailCursor);
                        while (emailCursor.moveToNext()) {
                            Log.d(TAG, "Found " + emailCursor.getCount() + " email messages.");
                            BluetoothMapMessageListingElement e3 = element(emailCursor, fi, ap);
                            bmList.add(e3);
                        }
                    }
                }
            }
            bmList.sort();
            bmList.segment(ap.getMaxListCount(), offsetNum);
            List<BluetoothMapMessageListingElement> list = bmList.getList();
            int listSize = list.size();
            Cursor tmpCursor = null;
            for (int x = 0; x < listSize; x++) {
                BluetoothMapMessageListingElement ele = list.get(x);
                if ((ele.getType().equals(BluetoothMapUtils.TYPE.SMS_GSM) || ele.getType().equals(BluetoothMapUtils.TYPE.SMS_CDMA)) && smsCursor != null) {
                    tmpCursor = smsCursor;
                    fi.mMsgType = 0;
                } else if (ele.getType().equals(BluetoothMapUtils.TYPE.MMS) && mmsCursor != null) {
                    tmpCursor = mmsCursor;
                    fi.mMsgType = 1;
                } else if (ele.getType().equals(BluetoothMapUtils.TYPE.EMAIL) && emailCursor != null) {
                    tmpCursor = emailCursor;
                    fi.mMsgType = 2;
                }
                if (tmpCursor != null) {
                    if (tmpCursor.moveToPosition(ele.getCursorIndex())) {
                        setSenderAddressing(ele, tmpCursor, fi, ap);
                        setSenderName(ele, tmpCursor, fi, ap);
                        setRecipientAddressing(ele, tmpCursor, fi, ap);
                        setRecipientName(ele, tmpCursor, fi, ap);
                        setSubject(ele, tmpCursor, fi, ap);
                        setSize(ele, tmpCursor, fi, ap);
                        setReceptionStatus(ele, tmpCursor, fi, ap);
                        setText(ele, tmpCursor, fi, ap);
                        setAttachmentSize(ele, tmpCursor, fi, ap);
                        setPriority(ele, tmpCursor, fi, ap);
                        setSent(ele, tmpCursor, fi, ap);
                        setProtected(ele, tmpCursor, fi, ap);
                        setThreadId(ele, tmpCursor, fi, ap);
                    }
                }
            }
            close(emailCursor);
            close(smsCursor);
            close(mmsCursor);
            Log.d(TAG, "messagelisting end");
            return bmList;
        } catch (Throwable th) {
            close(emailCursor);
            close(smsCursor);
            close(mmsCursor);
            throw th;
        }
    }

    public int msgListingSize(BluetoothMapFolderElement folderElement, BluetoothMapAppParams ap) {
        Log.d(TAG, "msgListingSize: folder = " + folderElement.getName());
        FilterInfo fi = new FilterInfo();
        setFilterInfo(fi);
        if (smsSelected(fi, ap) && folderElement.hasSmsMmsContent()) {
            fi.mMsgType = 0;
            Cursor c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, setWhereFilter(folderElement, fi, ap), null, "date DESC");
            cnt = c != null ? c.getCount() : 0;
            close(c);
        }
        if (mmsSelected(fi, ap) && folderElement.hasSmsMmsContent()) {
            fi.mMsgType = 1;
            Cursor c2 = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, setWhereFilter(folderElement, fi, ap), null, "date DESC");
            if (c2 != null) {
                cnt += c2.getCount();
            }
            close(c2);
        }
        if (emailSelected(fi, ap) && folderElement.getEmailFolderId() != -1) {
            fi.mMsgType = 2;
            String where = setWhereFilter(folderElement, fi, ap);
            if (!where.isEmpty()) {
                Uri contentUri = Uri.parse(this.mBaseEmailUri + "Message");
                Cursor c3 = this.mResolver.query(contentUri, BluetoothMapContract.BT_MESSAGE_PROJECTION, where, null, "date DESC");
                if (c3 != null) {
                    cnt += c3.getCount();
                }
                close(c3);
            }
        }
        Log.d(TAG, "msgListingSize: size = " + cnt);
        return cnt;
    }

    public boolean msgListingHasUnread(BluetoothMapFolderElement folderElement, BluetoothMapAppParams ap) {
        Log.d(TAG, "msgListingHasUnread: folder = " + folderElement.getName());
        FilterInfo fi = new FilterInfo();
        setFilterInfo(fi);
        if (smsSelected(fi, ap) && folderElement.hasSmsMmsContent()) {
            fi.mMsgType = 0;
            Cursor c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, (setWhereFilterFolderType(folderElement, fi) + " AND read=0 ") + setWhereFilterPeriod(ap, fi), null, "date DESC");
            cnt = c != null ? 0 + c.getCount() : 0;
            close(c);
        }
        if (mmsSelected(fi, ap) && folderElement.hasSmsMmsContent()) {
            fi.mMsgType = 1;
            Cursor c2 = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, (setWhereFilterFolderType(folderElement, fi) + " AND read=0 ") + setWhereFilterPeriod(ap, fi), null, "date DESC");
            if (c2 != null) {
                cnt += c2.getCount();
            }
            close(c2);
        }
        if (emailSelected(fi, ap) && folderElement.getEmailFolderId() != -1) {
            fi.mMsgType = 2;
            String where = setWhereFilterFolderType(folderElement, fi);
            if (!where.isEmpty()) {
                String where2 = (where + " AND flag_read=0 ") + setWhereFilterPeriod(ap, fi);
                Uri contentUri = Uri.parse(this.mBaseEmailUri + "Message");
                Cursor c3 = this.mResolver.query(contentUri, BluetoothMapContract.BT_MESSAGE_PROJECTION, where2, null, "date DESC");
                if (c3 != null) {
                    cnt += c3.getCount();
                }
                close(c3);
            }
        }
        Log.d(TAG, "msgListingHasUnread: numUnread = " + cnt);
        return cnt > 0;
    }

    private String getFolderName(int type, int threadId) {
        if (threadId == -1) {
            return "deleted";
        }
        switch (type) {
            case 1:
                return "inbox";
            case 2:
                return "sent";
            case 3:
                return "draft";
            case 4:
            case 5:
            case 6:
                return "outbox";
            default:
                return "";
        }
    }

    public byte[] getMessage(String handle, BluetoothMapAppParams appParams, BluetoothMapFolderElement folderElement) throws UnsupportedEncodingException {
        BluetoothMapUtils.TYPE type = BluetoothMapUtils.getMsgTypeFromHandle(handle);
        long id = BluetoothMapUtils.getCpHandle(handle);
        if (appParams.getFractionRequest() == 1) {
            throw new IllegalArgumentException("FRACTION_REQUEST_NEXT does not make sence as we always return the full message.");
        }
        switch (type) {
            case SMS_GSM:
            case SMS_CDMA:
                return getSmsMessage(id, appParams.getCharset());
            case MMS:
                return getMmsMessage(id, appParams);
            case EMAIL:
                return getEmailMessage(id, appParams, folderElement);
            default:
                throw new IllegalArgumentException("Invalid message handle.");
        }
    }

    private String setVCardFromPhoneNumber(BluetoothMapbMessage message, String phone, boolean incoming) {
        String[] phoneNumbers;
        String contactId = null;
        String contactName = null;
        String[] emailAddresses = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));
        String[] projection = {"_id", "display_name"};
        Cursor p = this.mResolver.query(uri, projection, "in_visible_group=1", null, "_id ASC");
        if (p != null) {
            try {
                if (p.moveToFirst()) {
                    contactId = p.getString(p.getColumnIndex("_id"));
                    contactName = p.getString(p.getColumnIndex("display_name"));
                }
            } catch (Throwable th) {
                close(p);
                throw th;
            }
        }
        if (contactId == null) {
            phoneNumbers = new String[]{phone};
        } else {
            phoneNumbers = new String[]{phone};
            close(p);
            p = this.mResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, "contact_id = ?", new String[]{contactId}, null);
            if (p != null) {
                emailAddresses = new String[p.getCount()];
                int i = 0;
                while (p != null && p.moveToNext()) {
                    String emailAddress = p.getString(p.getColumnIndex("data1"));
                    int i2 = i + 1;
                    emailAddresses[i] = emailAddress;
                    i = i2;
                }
            }
        }
        close(p);
        if (incoming) {
            message.addOriginator(contactName, contactName, phoneNumbers, emailAddresses);
        } else {
            message.addRecipient(contactName, contactName, phoneNumbers, emailAddresses);
        }
        return contactName;
    }

    public byte[] getSmsMessage(long id, int charset) throws UnsupportedEncodingException {
        BluetoothMapbMessageSms message = new BluetoothMapbMessageSms();
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        Cursor c = this.mResolver.query(Telephony.Sms.CONTENT_URI, SMS_PROJECTION, "_ID = " + id, null, null);
        if (c == null || !c.moveToFirst()) {
            throw new IllegalArgumentException("SMS handle not found");
        }
        try {
            if (tm.getPhoneType() == 1) {
                message.setType(BluetoothMapUtils.TYPE.SMS_GSM);
            } else if (tm.getPhoneType() == 2) {
                message.setType(BluetoothMapUtils.TYPE.SMS_CDMA);
            }
            String read = c.getString(c.getColumnIndex("read"));
            if (read.equalsIgnoreCase("1")) {
                message.setStatus(true);
            } else {
                message.setStatus(false);
            }
            int type = c.getInt(c.getColumnIndex("type"));
            int threadId = c.getInt(c.getColumnIndex("thread_id"));
            message.setFolder(getFolderName(type, threadId));
            String msgBody = c.getString(c.getColumnIndex("body"));
            String phone = c.getString(c.getColumnIndex("address"));
            long time = c.getLong(c.getColumnIndex("date"));
            if (type == 1) {
                setVCardFromPhoneNumber(message, phone, true);
            } else {
                setVCardFromPhoneNumber(message, phone, false);
            }
            if (charset == 0) {
                if (type == 1) {
                    message.setSmsBodyPdus(BluetoothMapSmsPdu.getDeliverPdus(msgBody, phone, time));
                } else {
                    message.setSmsBodyPdus(BluetoothMapSmsPdu.getSubmitPdus(msgBody, phone));
                }
            } else {
                message.setSmsBody(msgBody);
            }
            close(c);
            return message.encode();
        } catch (Throwable th) {
            close(c);
            throw th;
        }
    }

    private void extractMmsAddresses(long id, BluetoothMapbMessageMms message) {
        String selection = new String("msg_id=" + id);
        String uriStr = new String(Telephony.Mms.CONTENT_URI + "/" + id + "/addr");
        Uri uriAddress = Uri.parse(uriStr);
        Cursor c = this.mResolver.query(uriAddress, null, selection, null, null);
        while (c != null) {
            try {
                if (c.moveToNext()) {
                    String address = c.getString(c.getColumnIndex("address"));
                    if (!address.equals(INSERT_ADDRES_TOKEN)) {
                        Integer type = Integer.valueOf(c.getInt(c.getColumnIndex("type")));
                        switch (type.intValue()) {
                            case MMS_BCC:
                                String contactName = setVCardFromPhoneNumber(message, address, false);
                                message.addBcc(contactName, address);
                                break;
                            case MMS_CC:
                                String contactName2 = setVCardFromPhoneNumber(message, address, false);
                                message.addCc(contactName2, address);
                                break;
                            case MMS_FROM:
                                String contactName3 = setVCardFromPhoneNumber(message, address, true);
                                message.addFrom(contactName3, address);
                                break;
                            case MMS_TO:
                                String contactName4 = setVCardFromPhoneNumber(message, address, false);
                                message.addTo(contactName4, address);
                                break;
                        }
                    }
                }
            } finally {
                close(c);
            }
        }
    }

    private byte[] readMmsDataPart(long partid) {
        String uriStr = new String(Telephony.Mms.CONTENT_URI + "/part/" + partid);
        Uri uriAddress = Uri.parse(uriStr);
        InputStream is = null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[MASK_SENT];
        byte[] retVal = null;
        try {
            is = this.mResolver.openInputStream(uriAddress);
            while (true) {
                int len = is.read(buffer);
                if (len == -1) {
                    break;
                }
                os.write(buffer, 0, len);
            }
            retVal = os.toByteArray();
        } catch (IOException e) {
            Log.w(TAG, "Error reading part data", e);
        } finally {
            close(os);
            close(is);
        }
        return retVal;
    }

    private void extractMmsParts(long id, BluetoothMapbMessageMms message) {
        String selection = new String("mid=" + id);
        String uriStr = new String(Telephony.Mms.CONTENT_URI + "/" + id + "/part");
        Uri uriAddress = Uri.parse(uriStr);
        Cursor c = this.mResolver.query(uriAddress, null, selection, null, null);
        while (c != null) {
            try {
                if (!c.moveToNext()) {
                    break;
                }
                Long partId = Long.valueOf(c.getLong(c.getColumnIndex("_id")));
                String contentType = c.getString(c.getColumnIndex("ct"));
                String name = c.getString(c.getColumnIndex("name"));
                String charset = c.getString(c.getColumnIndex("chset"));
                String filename = c.getString(c.getColumnIndex("fn"));
                String text = c.getString(c.getColumnIndex("text"));
                Integer.valueOf(c.getInt(c.getColumnIndex(BluetoothShare._DATA)));
                String cid = c.getString(c.getColumnIndex("cid"));
                String cl = c.getString(c.getColumnIndex("cl"));
                String cdisp = c.getString(c.getColumnIndex("cd"));
                BluetoothMapbMessageMms.MimePart part = message.addMimePart();
                part.mContentType = contentType;
                part.mPartName = name;
                part.mContentId = cid;
                part.mContentLocation = cl;
                part.mContentDisposition = cdisp;
                if (text != null) {
                    try {
                        part.mData = text.getBytes("UTF-8");
                        part.mCharsetName = "utf-8";
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, "extractMmsParts", e);
                        part.mData = null;
                        part.mCharsetName = null;
                    } catch (NumberFormatException e2) {
                        Log.d(TAG, "extractMmsParts", e2);
                        part.mData = null;
                        part.mCharsetName = null;
                    }
                } else {
                    part.mData = readMmsDataPart(partId.longValue());
                    if (charset != null) {
                        part.mCharsetName = CharacterSets.getMimeName(Integer.parseInt(charset));
                    }
                }
                part.mFileName = filename;
            } catch (Throwable th) {
                close(c);
                throw th;
            }
        }
        close(c);
        message.updateCharset();
    }

    public byte[] getMmsMessage(long id, BluetoothMapAppParams appParams) throws UnsupportedEncodingException {
        if (appParams.getCharset() == 0) {
            throw new IllegalArgumentException("MMS charset native not allowed for MMS - must be utf-8");
        }
        BluetoothMapbMessageMms message = new BluetoothMapbMessageMms();
        Cursor c = this.mResolver.query(Telephony.Mms.CONTENT_URI, MMS_PROJECTION, "_ID = " + id, null, null);
        if (c == null || !c.moveToFirst()) {
            throw new IllegalArgumentException("MMS handle not found");
        }
        try {
            message.setType(BluetoothMapUtils.TYPE.MMS);
            String read = c.getString(c.getColumnIndex("read"));
            if (read.equalsIgnoreCase("1")) {
                message.setStatus(true);
            } else {
                message.setStatus(false);
            }
            int msgBox = c.getInt(c.getColumnIndex("msg_box"));
            int threadId = c.getInt(c.getColumnIndex("thread_id"));
            message.setFolder(getFolderName(msgBox, threadId));
            message.setSubject(c.getString(c.getColumnIndex("sub")));
            message.setMessageId(c.getString(c.getColumnIndex("m_id")));
            message.setContentType(c.getString(c.getColumnIndex("ct_t")));
            message.setDate(c.getLong(c.getColumnIndex("date")) * 1000);
            message.setTextOnly(c.getInt(c.getColumnIndex("text_only")) != 0);
            message.setIncludeAttachments(appParams.getAttachment() != 0);
            extractMmsParts(id, message);
            extractMmsAddresses(id, message);
            close(c);
            return message.encode();
        } catch (Throwable th) {
            close(c);
            throw th;
        }
    }

    public byte[] getEmailMessage(long id, BluetoothMapAppParams appParams, BluetoothMapFolderElement currentFolder) throws UnsupportedEncodingException {
        FileInputStream is;
        StringBuilder email;
        byte[] buffer;
        if (appParams != null) {
            Log.d(TAG, "TYPE_MESSAGE (GET): Attachment = " + appParams.getAttachment() + ", Charset = " + appParams.getCharset() + ", FractionRequest = " + appParams.getFractionRequest());
        }
        if (appParams.getCharset() == 0) {
            throw new IllegalArgumentException("EMAIL charset not UTF-8");
        }
        BluetoothMapbMessageEmail message = new BluetoothMapbMessageEmail();
        Uri contentUri = Uri.parse(this.mBaseEmailUri + "Message");
        Cursor c = this.mResolver.query(contentUri, BluetoothMapContract.BT_MESSAGE_PROJECTION, "_ID = " + id, null, null);
        if (c != null && c.moveToFirst()) {
            throw new IllegalArgumentException("EMAIL handle not found");
        }
        try {
            int fractionRequest = appParams.getFractionRequest();
            if (fractionRequest != -1 && !c.getString(c.getColumnIndex("reception_state")).equalsIgnoreCase("complete")) {
                Log.w(TAG, "getEmailMessage - receptionState not COMPLETE -  Not Implemented!");
            }
            String read = c.getString(c.getColumnIndex("flag_read"));
            if (read == null || !read.equalsIgnoreCase("1")) {
                message.setStatus(false);
            } else {
                message.setStatus(true);
            }
            message.setType(BluetoothMapUtils.TYPE.EMAIL);
            long folderId = c.getLong(c.getColumnIndex("folder_id"));
            BluetoothMapFolderElement folderElement = currentFolder.getEmailFolderById(folderId);
            message.setCompleteFolder(folderElement.getFullPath());
            String nameEmail = c.getString(c.getColumnIndex("to_list"));
            Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(nameEmail);
            if (tokens.length != 0) {
                Log.d(TAG, "Recipient count= " + tokens.length);
                for (int i = 0; i < tokens.length; i++) {
                    String[] emails = {tokens[i].getAddress()};
                    String name = tokens[i].getName();
                    message.addRecipient(name, name, null, emails);
                }
            }
            String nameEmail2 = c.getString(c.getColumnIndex("from_list"));
            Rfc822Token[] tokens2 = Rfc822Tokenizer.tokenize(nameEmail2);
            if (tokens2.length != 0) {
                Log.d(TAG, "Originator count= " + tokens2.length);
                for (int i2 = 0; i2 < tokens2.length; i2++) {
                    String[] emails2 = {tokens2[i2].getAddress()};
                    String name2 = tokens2[i2].getName();
                    message.addOriginator(name2, name2, null, emails2);
                }
            }
            String attStr = appParams.getAttachment() == 0 ? "/NO_ATTACHMENTS" : "";
            Uri uri = Uri.parse(contentUri + "/" + id + attStr);
            FileInputStream is2 = null;
            ParcelFileDescriptor fd = null;
            try {
                try {
                    fd = this.mResolver.openFileDescriptor(uri, "r");
                    is = new FileInputStream(fd.getFileDescriptor());
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
                e = e;
            } catch (IOException e2) {
                e = e2;
            } catch (NullPointerException e3) {
                e = e3;
            }
            try {
                email = new StringBuilder("");
                buffer = new byte[MASK_ATTACHMENT_SIZE];
            } catch (FileNotFoundException e4) {
                e = e4;
                is2 = is;
                Log.w(TAG, e);
                close(is2);
                close(fd);
            } catch (IOException e5) {
                e = e5;
                is2 = is;
                Log.w(TAG, e);
                close(is2);
                close(fd);
            } catch (NullPointerException e6) {
                e = e6;
                is2 = is;
                Log.w(TAG, e);
                close(is2);
                close(fd);
            } catch (Throwable th2) {
                th = th2;
                is2 = is;
                close(is2);
                close(fd);
                throw th;
            }
            while (true) {
                int count = is.read(buffer);
                if (count == -1) {
                    break;
                }
                email.append(new String(buffer, 0, count));
                close(c);
                return message.encode();
            }
            message.setEmailBody(email.toString());
            close(is);
            close(fd);
            is2 = is;
            close(c);
            return message.encode();
        } catch (Throwable th3) {
            close(c);
            throw th3;
        }
    }
}
