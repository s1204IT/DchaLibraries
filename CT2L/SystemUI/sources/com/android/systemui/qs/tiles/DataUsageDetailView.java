package com.android.systemui.qs.tiles;

import android.R;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.qs.DataUsageGraph;
import com.android.systemui.statusbar.policy.NetworkController;
import java.text.DecimalFormat;

public class DataUsageDetailView extends LinearLayout {
    private final DecimalFormat FORMAT;

    public DataUsageDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.FORMAT = new DecimalFormat("#.##");
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this, R.id.title, com.android.systemui.R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, com.android.systemui.R.id.usage_text, com.android.systemui.R.dimen.qs_data_usage_usage_text_size);
        FontSizeUtils.updateFontSize(this, com.android.systemui.R.id.usage_carrier_text, com.android.systemui.R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, com.android.systemui.R.id.usage_info_top_text, com.android.systemui.R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, com.android.systemui.R.id.usage_period_text, com.android.systemui.R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, com.android.systemui.R.id.usage_info_bottom_text, com.android.systemui.R.dimen.qs_data_usage_text_size);
    }

    public void bind(NetworkController.MobileDataController.DataUsageInfo info) {
        int titleId;
        long bytes;
        String top;
        Resources res = this.mContext.getResources();
        int usageColor = com.android.systemui.R.color.system_accent_color;
        String bottom = null;
        if (info.usageLevel < info.warningLevel || info.limitLevel <= 0) {
            titleId = com.android.systemui.R.string.quick_settings_cellular_detail_data_usage;
            bytes = info.usageLevel;
            top = res.getString(com.android.systemui.R.string.quick_settings_cellular_detail_data_warning, formatBytes(info.warningLevel));
        } else if (info.usageLevel <= info.limitLevel) {
            titleId = com.android.systemui.R.string.quick_settings_cellular_detail_remaining_data;
            bytes = info.limitLevel - info.usageLevel;
            top = res.getString(com.android.systemui.R.string.quick_settings_cellular_detail_data_used, formatBytes(info.usageLevel));
            bottom = res.getString(com.android.systemui.R.string.quick_settings_cellular_detail_data_limit, formatBytes(info.limitLevel));
        } else {
            titleId = com.android.systemui.R.string.quick_settings_cellular_detail_over_limit;
            bytes = info.usageLevel - info.limitLevel;
            top = res.getString(com.android.systemui.R.string.quick_settings_cellular_detail_data_used, formatBytes(info.usageLevel));
            bottom = res.getString(com.android.systemui.R.string.quick_settings_cellular_detail_data_limit, formatBytes(info.limitLevel));
            usageColor = com.android.systemui.R.color.system_warning_color;
        }
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(titleId);
        TextView usage = (TextView) findViewById(com.android.systemui.R.id.usage_text);
        usage.setText(formatBytes(bytes));
        usage.setTextColor(res.getColor(usageColor));
        DataUsageGraph graph = (DataUsageGraph) findViewById(com.android.systemui.R.id.usage_graph);
        graph.setLevels(info.limitLevel, info.warningLevel, info.usageLevel);
        TextView carrier = (TextView) findViewById(com.android.systemui.R.id.usage_carrier_text);
        carrier.setText(info.carrier);
        TextView period = (TextView) findViewById(com.android.systemui.R.id.usage_period_text);
        period.setText(info.period);
        TextView infoTop = (TextView) findViewById(com.android.systemui.R.id.usage_info_top_text);
        infoTop.setVisibility(top != null ? 0 : 8);
        infoTop.setText(top);
        TextView infoBottom = (TextView) findViewById(com.android.systemui.R.id.usage_info_bottom_text);
        infoBottom.setVisibility(bottom != null ? 0 : 8);
        infoBottom.setText(bottom);
    }

    private String formatBytes(long bytes) {
        double val;
        String suffix;
        long b = Math.abs(bytes);
        if (b > 1.048576E8d) {
            val = b / 1.073741824E9d;
            suffix = "GB";
        } else if (b > 102400.0d) {
            val = b / 1048576.0d;
            suffix = "MB";
        } else {
            val = b / 1024.0d;
            suffix = "KB";
        }
        return this.FORMAT.format(((double) (bytes < 0 ? -1 : 1)) * val) + " " + suffix;
    }
}
