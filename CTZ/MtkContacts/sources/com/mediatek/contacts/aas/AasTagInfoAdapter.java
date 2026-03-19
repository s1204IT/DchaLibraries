package com.mediatek.contacts.aas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.contacts.R;
import com.mediatek.contacts.aassne.SimAasSneUtils;
import com.mediatek.contacts.simcontact.PhbInfoUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.internal.telephony.phb.AlphaTag;
import java.util.ArrayList;
import java.util.Iterator;

public class AasTagInfoAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mInflater;
    private int mSubId;
    private ToastHelper mToastHelper;
    private int mMode = 0;
    private ArrayList<TagItemInfo> mTagItemInfos = new ArrayList<>();

    public AasTagInfoAdapter(Context context, int i) {
        this.mContext = null;
        this.mInflater = null;
        this.mSubId = -1;
        this.mToastHelper = null;
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mSubId = i;
        this.mToastHelper = new ToastHelper(context);
    }

    public void updateAlphaTags() {
        this.mTagItemInfos.clear();
        SimAasSneUtils.refreshAASList(this.mSubId);
        for (AlphaTag alphaTag : SimAasSneUtils.getAAS(this.mSubId)) {
            this.mTagItemInfos.add(new TagItemInfo(alphaTag));
            Log.d("CustomAasAdapter", "[updateAlphaTags] getPbrIndex: " + alphaTag.getPbrIndex() + ",getRecordIndex: " + alphaTag.getRecordIndex() + ",getAlphaTag: " + Log.anonymize(alphaTag.getAlphaTag()));
        }
        notifyDataSetChanged();
    }

    public int getMode() {
        return this.mMode;
    }

    public void setMode(int i) {
        Log.d("CustomAasAdapter", "[setMode] mode: " + i);
        if (this.mMode != i) {
            this.mMode = i;
            if (isMode(0)) {
                Iterator<TagItemInfo> it = this.mTagItemInfos.iterator();
                while (it.hasNext()) {
                    it.next().mChecked = false;
                }
            }
            notifyDataSetChanged();
        }
    }

    public boolean isMode(int i) {
        return this.mMode == i;
    }

    @Override
    public int getCount() {
        return this.mTagItemInfos.size();
    }

    @Override
    public TagItemInfo getItem(int i) {
        return this.mTagItemInfos.get(i);
    }

    public void updateChecked(int i) {
        getItem(i).mChecked = !r2.mChecked;
        notifyDataSetChanged();
    }

    public void setAllChecked(boolean z) {
        Log.d("CustomAasAdapter", "[setAllChecked] checked: " + z);
        Iterator<TagItemInfo> it = this.mTagItemInfos.iterator();
        while (it.hasNext()) {
            it.next().mChecked = z;
        }
        notifyDataSetChanged();
    }

    public void deleteCheckedAasTag() {
        for (TagItemInfo tagItemInfo : this.mTagItemInfos) {
            if (tagItemInfo.mChecked) {
                if (!SimAasSneUtils.removeUsimAasById(this.mSubId, tagItemInfo.mAlphaTag.getRecordIndex(), tagItemInfo.mAlphaTag.getPbrIndex())) {
                    this.mToastHelper.showToast(this.mContext.getResources().getString(R.string.aas_delete_fail, tagItemInfo.mAlphaTag.getAlphaTag()));
                    Log.d("CustomAasAdapter", "[deleteCheckedAasTag] delete failed:" + Log.anonymize(tagItemInfo.mAlphaTag.getAlphaTag()));
                } else {
                    ContentResolver contentResolver = this.mContext.getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.putNull("data3");
                    String str = Integer.toString(this.mSubId) + "-" + tagItemInfo.mAlphaTag.getRecordIndex();
                    Log.d("CustomAasAdapter", "[deleteCheckedAasTag] aasIndex=" + str);
                    Log.sensitive("CustomAasAdapter", "[deleteCheckedAasTag] whereClause=mimetype='vnd.android.cursor.item/phone_v2' AND is_additional_number=1 AND data2=101 AND data3=?");
                    Log.d("CustomAasAdapter", "[deleteCheckedAasTag] updatedCount=" + contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, "mimetype='vnd.android.cursor.item/phone_v2' AND is_additional_number=1 AND data2=101 AND data3=?", new String[]{str}));
                }
            }
        }
        updateAlphaTags();
    }

    public int getCheckedItemCount() {
        int i = 0;
        if (isMode(1)) {
            Iterator<TagItemInfo> it = this.mTagItemInfos.iterator();
            while (it.hasNext()) {
                if (it.next().mChecked) {
                    i++;
                }
            }
        }
        return i;
    }

    public int[] getCheckedIndexArray() {
        int[] iArr = new int[getCheckedItemCount()];
        int i = 0;
        for (int i2 = 0; i2 < this.mTagItemInfos.size(); i2++) {
            if (this.mTagItemInfos.get(i2).mChecked) {
                iArr[i] = i2;
                i++;
            }
        }
        return iArr;
    }

    public void setCheckedByIndexArray(int[] iArr) {
        for (int i : iArr) {
            this.mTagItemInfos.get(i).mChecked = true;
        }
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public Boolean isExist(String str) {
        for (int i = 0; i < this.mTagItemInfos.size(); i++) {
            if (this.mTagItemInfos.get(i).mAlphaTag.getAlphaTag().equals(str)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        int usimAasCount = PhbInfoUtils.getUsimAasCount(this.mSubId);
        Log.d("CustomAasAdapter", "[isFull] getCount: " + getCount() + ",maxCount=" + usimAasCount + ",sub: " + this.mSubId);
        return getCount() >= usimAasCount;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View viewInflate = this.mInflater.inflate(R.layout.custom_aas_item, (ViewGroup) null);
        TextView textView = (TextView) viewInflate.findViewById(R.id.aas_item_tag);
        ImageView imageView = (ImageView) viewInflate.findViewById(R.id.aas_edit);
        CheckBox checkBox = (CheckBox) viewInflate.findViewById(R.id.aas_item_check);
        TagItemInfo item = getItem(i);
        textView.setText(item.mAlphaTag.getAlphaTag());
        if (isMode(0)) {
            imageView.setVisibility(0);
            checkBox.setChecked(item.mChecked);
            checkBox.setVisibility(8);
        } else {
            imageView.setVisibility(8);
            checkBox.setVisibility(0);
            checkBox.setChecked(item.mChecked);
        }
        return viewInflate;
    }

    public static class TagItemInfo {
        AlphaTag mAlphaTag;
        boolean mChecked = false;

        public TagItemInfo(AlphaTag alphaTag) {
            this.mAlphaTag = null;
            this.mAlphaTag = alphaTag;
        }
    }
}
