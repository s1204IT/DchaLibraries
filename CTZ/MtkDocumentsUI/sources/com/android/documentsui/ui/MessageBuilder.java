package com.android.documentsui.ui;

import android.content.Context;
import android.net.Uri;
import android.text.BidiFormatter;
import android.text.Html;
import com.android.documentsui.R;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.Shared;
import java.util.Iterator;
import java.util.List;

public class MessageBuilder {
    private Context mContext;

    public MessageBuilder(Context context) {
        this.mContext = context;
    }

    public String generateDeleteMessage(List<DocumentInfo> list) {
        Iterator<DocumentInfo> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().isDirectory()) {
                i++;
            }
        }
        if (list.size() == 1) {
            String strUnicodeWrap = BidiFormatter.getInstance().unicodeWrap(list.get(0).displayName);
            return i == 0 ? this.mContext.getString(R.string.delete_filename_confirmation_message, strUnicodeWrap) : this.mContext.getString(R.string.delete_foldername_confirmation_message, strUnicodeWrap);
        }
        if (i == 0) {
            return Shared.getQuantityString(this.mContext, R.plurals.delete_files_confirmation_message, list.size());
        }
        if (i == list.size()) {
            return Shared.getQuantityString(this.mContext, R.plurals.delete_folders_confirmation_message, list.size());
        }
        return Shared.getQuantityString(this.mContext, R.plurals.delete_items_confirmation_message, list.size());
    }

    public String generateListMessage(int i, int i2, List<DocumentInfo> list, List<Uri> list2) {
        int i3;
        switch (i) {
            case 1:
                switch (i2) {
                    case 1:
                        i3 = R.plurals.copy_failure_alert_content;
                        break;
                    case 2:
                        i3 = R.plurals.extract_failure_alert_content;
                        break;
                    case 3:
                        i3 = R.plurals.compress_failure_alert_content;
                        break;
                    case 4:
                        i3 = R.plurals.move_failure_alert_content;
                        break;
                    case 5:
                        i3 = R.plurals.delete_failure_alert_content;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                break;
            case 2:
                i3 = R.plurals.copy_converted_warning_content;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        StringBuilder sb = new StringBuilder("<p>");
        Iterator<DocumentInfo> it = list.iterator();
        while (it.hasNext()) {
            sb.append("&#8226; " + Html.escapeHtml(BidiFormatter.getInstance().unicodeWrap(it.next().displayName)) + "<br>");
        }
        if (list2 != null) {
            Iterator<Uri> it2 = list2.iterator();
            while (it2.hasNext()) {
                sb.append("&#8226; " + BidiFormatter.getInstance().unicodeWrap(it2.next().toSafeString()) + "<br>");
            }
        }
        sb.append("</p>");
        return this.mContext.getResources().getQuantityString(i3, list.size() + (list2 != null ? list2.size() : 0), sb.toString());
    }

    public String getQuantityString(int i, int i2) {
        return Shared.getQuantityString(this.mContext, i, i2);
    }
}
