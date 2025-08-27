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

/* loaded from: classes.dex */
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

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        Logger.d("ApListAdapter", "getView 0001");
        if (view == null) {
            Logger.d("ApListAdapter", "getView 0002");
            view = this.mInflater.inflate(R.layout.wifi_ap_list, (ViewGroup) null);
        }
        AccessPoint item = getItem(i);
        String ssidStr = item.getSsidStr();
        String summary = item.getSummary();
        this.mApNameTitle = (TextView) view.findViewById(R.id.tv_ap_name_title);
        this.mApName = (TextView) view.findViewById(R.id.tv_ap_name);
        this.mApSecurity = (TextView) view.findViewById(R.id.tv_ap_security);
        this.mWifinetworkChecked = (ImageView) view.findViewById(R.id.wifinetwork_checked);
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
        if (ssidStr.equals(this.mOnAccessSsid.substring(1, this.mOnAccessSsid.length() - 1)) && ((NetworkSettingActivity) this.mContext).getResources().getString(R.string.wifi_status_connected).equals(this.mApSecurity.getText().toString())) {
            Logger.d("ApListAdapter", "getView 0003");
            view.setBackgroundResource(R.drawable.background);
            this.mApNameTitle.setTextColor(Color.parseColor("#0056a2"));
            this.mApName.setTextColor(Color.parseColor("#0056a2"));
            this.mApSecurity.setTextColor(Color.parseColor("#0056a2"));
            this.mWifinetworkChecked.setVisibility(0);
        } else {
            Logger.d("ApListAdapter", "getView 0004");
            view.setBackgroundColor(-1);
            this.mApNameTitle.setTextColor(-16777216);
            this.mApName.setTextColor(-16777216);
            this.mApSecurity.setTextColor(-16777216);
            this.mWifinetworkChecked.setVisibility(4);
        }
        this.mApRssi = (ImageView) view.findViewById(R.id.img_wifi_level);
        this.mIconLocked = (ImageView) view.findViewById(R.id.icon_locked);
        int level = item.getLevel();
        if (level < 0) {
            Logger.d("ApListAdapter", "getView 0005");
            this.mApRssi.setImageDrawable(null);
            this.mIconLocked.setImageDrawable(null);
        } else {
            Logger.d("ApListAdapter", "getView 0006");
            this.mApRssi.setImageLevel(level);
            this.mApRssi.setImageResource(R.drawable.wifi_signal);
            if (item.getSecurity() != 0) {
                Logger.d("ApListAdapter", "getView 0007");
                this.mIconLocked.setImageResource(R.drawable.icon_locked);
            } else {
                Logger.d("ApListAdapter", "getView 0008");
                this.mIconLocked.setImageDrawable(null);
            }
        }
        Logger.d("ApListAdapter", "getView 0009");
        return view;
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

    private boolean updateAccessPoint(AccessPoint accessPoint, AccessPoint accessPoint2) {
        Logger.d("ApListAdapter", "updateAccessPoint 0004");
        if (accessPoint2.equals(accessPoint)) {
            Logger.d("ApListAdapter", "updateAccessPoint 0005");
            return true;
        }
        if (accessPoint2.update(accessPoint.getConfig(), accessPoint.getInfo(), accessPoint.getNetworkInfo())) {
            Logger.d("ApListAdapter", "updateAccessPoint 0006");
            notifyDataSetChanged();
            return true;
        }
        Logger.d("ApListAdapter", "updateAccessPoint 0007");
        return false;
    }
}
