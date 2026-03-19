package com.mediatek.gallery3d.video;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.mediatek.galleryportable.Log;

public class DetailDialog extends AlertDialog implements DialogInterface.OnClickListener {
    private static final int BTN_OK = -1;
    private static final String TAG = "VP_DetailDialog";
    private final String mAuthor;
    private TextView mAuthorView;
    private final Context mContext;
    private final String mCopyright;
    private TextView mCopyrightView;
    private final String mTitle;
    private TextView mTitleView;
    private View mView;

    public DetailDialog(Context context, String str, String str2, String str3) {
        super(context);
        this.mContext = context;
        this.mTitle = str == null ? "" : str;
        this.mAuthor = str2 == null ? "" : str2;
        this.mCopyright = str3 == null ? "" : str3;
        Log.v(TAG, "DetailDialog() mTitle=" + this.mTitle + ", mAuthor=" + this.mAuthor + ", mCopyRight=" + this.mCopyright);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        setTitle(R.string.media_detail);
        this.mView = getLayoutInflater().inflate(R.layout.m_detail_dialog, (ViewGroup) null);
        if (this.mView == null) {
            Log.e(TAG, "mView is null, return");
            return;
        }
        setView(this.mView);
        this.mTitleView = (TextView) this.mView.findViewById(R.id.title);
        this.mAuthorView = (TextView) this.mView.findViewById(R.id.author);
        this.mCopyrightView = (TextView) this.mView.findViewById(R.id.copyright);
        this.mTitleView.setText(this.mContext.getString(R.string.detail_title, this.mTitle));
        this.mAuthorView.setText(this.mContext.getString(R.string.detail_session, this.mAuthor));
        this.mCopyrightView.setText(this.mContext.getString(R.string.detail_copyright, this.mCopyright));
        setButton(-1, this.mContext.getString(android.R.string.ok), this);
        super.onCreate(bundle);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
    }
}
