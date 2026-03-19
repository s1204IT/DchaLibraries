package com.mediatek.camera.ui.modepicker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.mediatek.camera.R;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.ui.modepicker.ModePickerManager;
import java.util.List;

class ModeItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ModeItemAdapter.class.getSimpleName());
    private OnViewItemClickListener mClickedListener;
    private String mCurrentModeName = "Normal";
    private LayoutInflater mLayoutInflater;
    private List<ModePickerManager.ModeInfo> mModes;

    public enum ITEM_TYPE {
        ITEM_TYPE_IMAGE
    }

    public interface OnViewItemClickListener {
        boolean onItemCLicked(ModePickerManager.ModeInfo modeInfo);
    }

    public ModeItemAdapter(Context context, OnViewItemClickListener onViewItemClickListener) {
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mClickedListener = onViewItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ModeViewHolder(this.mLayoutInflater.inflate(R.layout.mode_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof ModeViewHolder) {
            viewHolder.mTextView.setText(this.mModes.get(i).mName);
            viewHolder.mModeView.setContentDescription(this.mModes.get(i).mName);
            LogHelper.d(TAG, "onBindViewHolder: mode name = " + this.mModes.get(i).mName + " position = " + i);
            if (this.mModes.get(i).mName.equals(this.mCurrentModeName)) {
                if (this.mModes.get(i).mSelectedIcon != null) {
                    viewHolder.mImageView.setImageDrawable(this.mModes.get(i).mSelectedIcon);
                } else {
                    viewHolder.mImageView.setImageResource(R.drawable.ic_normal_mode_selected);
                }
            } else if (this.mModes.get(i).mUnselectedIcon != null) {
                viewHolder.mImageView.setImageDrawable(this.mModes.get(i).mUnselectedIcon);
            } else {
                viewHolder.mImageView.setImageResource(R.drawable.ic_normal_mode_unselected);
            }
            viewHolder.mTextView.setTag(this.mModes.get(i));
        }
    }

    @Override
    public int getItemViewType(int i) {
        return ITEM_TYPE.ITEM_TYPE_IMAGE.ordinal();
    }

    @Override
    public int getItemCount() {
        if (this.mModes == null) {
            return 0;
        }
        return this.mModes.size();
    }

    public void setModeList(List<ModePickerManager.ModeInfo> list) {
        this.mModes = list;
        notifyDataSetChanged();
    }

    public void updateCurrentModeName(String str) {
        this.mCurrentModeName = str;
    }

    private class ModeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView mImageView;
        View mModeView;
        TextView mTextView;

        ModeViewHolder(View view) {
            super(view);
            this.mModeView = view;
            this.mTextView = (TextView) view.findViewById(R.id.text_view);
            this.mImageView = (ImageView) view.findViewById(R.id.image_view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (ModeItemAdapter.this.mClickedListener.onItemCLicked((ModePickerManager.ModeInfo) this.mTextView.getTag())) {
                ModeItemAdapter.this.mCurrentModeName = ((ModePickerManager.ModeInfo) this.mTextView.getTag()).mName;
                LogHelper.d(ModeItemAdapter.TAG, "onClick: mode name = " + ModeItemAdapter.this.mCurrentModeName);
                ModeItemAdapter.this.notifyDataSetChanged();
            }
        }
    }
}
