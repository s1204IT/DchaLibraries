package jp.co.omronsoft.iwnnime.ml.candidate;

import android.content.Context;
import android.view.View;
import android.widget.TabHost;

public class SymbolCandidatesCategoryListTabContentView implements TabHost.TabContentFactory {
    private Context mContext;

    public SymbolCandidatesCategoryListTabContentView(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    @Override
    public View createTabContent(String tag) {
        View ret = new View(this.mContext);
        ret.setVisibility(8);
        return ret;
    }
}
