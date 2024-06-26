package jp.co.benesse.dcha.systemsettings;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import jp.co.benesse.dcha.util.Logger;
/* loaded from: s.zip:jp/co/benesse/dcha/systemsettings/ApListAdapter.class */
public class ApListAdapter extends ArrayAdapter<AccessPoint> {
    private static Typeface mTypeFace;
    private TextView mApName;
    private TextView mApNameTitle;
    private ImageView mApRssi;
    private TextView mApSecurity;
    private Context mContext;
    private ImageView mIconLocked;
    private LayoutInflater mInflater;
    private String mOnAccessSsid;
    private ImageView mWifinetworkChecked;

    public ApListAdapter(Context context, int i, List<AccessPoint> list, String str) {
        super(context, i, list);
        Logger.d("ApListAdapter", "ApListAdapter 0001");
        this.mOnAccessSsid = str;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mContext = context;
        Logger.d("ApListAdapter", "ApListAdapter 0002");
    }

    private boolean updateAccessPoint(AccessPoint accessPoint, AccessPoint accessPoint2) {
        Logger.d("ApListAdapter", "updateAccessPoint 0004");
        if (accessPoint2.equals(accessPoint)) {
            Logger.d("ApListAdapter", "updateAccessPoint 0005");
            return true;
        } else if (!accessPoint2.update(accessPoint.getConfig(), accessPoint.getInfo(), accessPoint.getNetworkInfo())) {
            Logger.d("ApListAdapter", "updateAccessPoint 0007");
            return false;
        } else {
            Logger.d("ApListAdapter", "updateAccessPoint 0006");
            notifyDataSetChanged();
            return true;
        }
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        Logger.d("ApListAdapter", "getView 0001");
        View view2 = view;
        if (view == null) {
            Logger.d("ApListAdapter", "getView 0002");
            view2 = this.mInflater.inflate(2130903050, (ViewGroup) null);
        }
        AccessPoint item = getItem(i);
        String ssidStr = item.getSsidStr();
        String summary = item.getSummary();
        this.mApNameTitle = (TextView) view2.findViewById(2131361850);
        this.mApName = (TextView) view2.findViewById(2131361851);
        this.mApSecurity = (TextView) view2.findViewById(2131361852);
        this.mWifinetworkChecked = (ImageView) view2.findViewById(2131361854);
        this.mApName.setText(ssidStr);
        this.mApSecurity.setText(summary);
        try {
            if (ParentSettingActivity.canReadSystemFont()) {
                if (mTypeFace == null) {
                    mTypeFace = Typeface.createFromFile("system/fonts/gjsgm.ttf");
                }
                this.mApNameTitle.setTypeface(mTypeFace);
                this.mApName.setTypeface(mTypeFace);
                this.mApSecurity.setTypeface(mTypeFace);
            }
        } catch (RuntimeException e) {
        }
        if (ssidStr.equals(this.mOnAccessSsid.substring(1, this.mOnAccessSsid.length() - 1)) && ((NetworkSettingActivity) this.mContext).getResources().getString(2131230949).equals(this.mApSecurity.getText().toString())) {
            Logger.d("ApListAdapter", "getView 0003");
            view2.setBackgroundResource(2130837514);
            this.mApNameTitle.setTextColor(Color.parseColor("#0056a2"));
            this.mApName.setTextColor(Color.parseColor("#0056a2"));
            this.mApSecurity.setTextColor(Color.parseColor("#0056a2"));
            this.mWifinetworkChecked.setVisibility(0);
        } else {
            Logger.d("ApListAdapter", "getView 0004");
            view2.setBackgroundColor(-1);
            this.mApNameTitle.setTextColor(-16777216);
            this.mApName.setTextColor(-16777216);
            this.mApSecurity.setTextColor(-16777216);
            this.mWifinetworkChecked.setVisibility(4);
        }
        this.mApRssi = (ImageView) view2.findViewById(2131361853);
        this.mIconLocked = (ImageView) view2.findViewById(2131361855);
        int level = item.getLevel();
        if (level < 0) {
            Logger.d("ApListAdapter", "getView 0005");
            this.mApRssi.setImageDrawable(null);
            this.mIconLocked.setImageDrawable(null);
        } else {
            Logger.d("ApListAdapter", "getView 0006");
            this.mApRssi.setImageLevel(level);
            this.mApRssi.setImageResource(2130837618);
            if (item.getSecurity() != 0) {
                Logger.d("ApListAdapter", "getView 0007");
                this.mIconLocked.setImageResource(2130837575);
            } else {
                Logger.d("ApListAdapter", "getView 0008");
                this.mIconLocked.setImageDrawable(null);
            }
        }
        Logger.d("ApListAdapter", "getView 0009");
        return view2;
    }

    public boolean updateAccessPoint(AccessPoint accessPoint) {
        Logger.d("ApListAdapter", "updateAccessPoint 0001");
        String ssidStr = accessPoint.getSsidStr();
        int count = getCount();
        for (int i = 0; i < count; i++) {
            AccessPoint item = getItem(i);
            if (TextUtils.equals(ssidStr, item.getSsidStr())) {
                Logger.d("ApListAdapter", "updateAccessPoint 0002");
                return updateAccessPoint(accessPoint, item);
            }
        }
        Logger.d("ApListAdapter", "updateAccessPoint 0003");
        return false;
    }
}
