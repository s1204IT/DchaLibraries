package com.android.gallery3d.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentManagerImpl;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.ui.DetailsAddressResolver;
import com.android.gallery3d.ui.DetailsHelper;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class DialogDetailsView implements DetailsHelper.DetailsViewContainer {
    private final AbstractGalleryActivity mActivity;
    private DetailsAdapter mAdapter;
    private MediaDetails mDetails;
    private Dialog mDialog;
    private int mIndex;
    private DetailsHelper.CloseListener mListener;
    private final DetailsHelper.DetailsSource mSource;

    public DialogDetailsView(AbstractGalleryActivity activity, DetailsHelper.DetailsSource source) {
        this.mActivity = activity;
        this.mSource = source;
    }

    @Override
    public void show() {
        reloadDetails();
        this.mDialog.show();
    }

    @Override
    public void hide() {
        this.mDialog.hide();
    }

    @Override
    public void reloadDetails() {
        MediaDetails details;
        int index = this.mSource.setIndex();
        if (index != -1 && (details = this.mSource.getDetails()) != null) {
            if (this.mIndex != index || this.mDetails != details) {
                this.mIndex = index;
                this.mDetails = details;
                setDetails(details);
            }
        }
    }

    private void setDetails(MediaDetails details) {
        this.mAdapter = new DetailsAdapter(details);
        String title = String.format(this.mActivity.getAndroidContext().getString(R.string.details_title), Integer.valueOf(this.mIndex + 1), Integer.valueOf(this.mSource.size()));
        ListView detailsList = (ListView) LayoutInflater.from(this.mActivity.getAndroidContext()).inflate(R.layout.details_list, (ViewGroup) null, false);
        detailsList.setAdapter((ListAdapter) this.mAdapter);
        this.mDialog = new AlertDialog.Builder(this.mActivity).setView(detailsList).setTitle(title).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                DialogDetailsView.this.mDialog.dismiss();
            }
        }).create();
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (DialogDetailsView.this.mListener != null) {
                    DialogDetailsView.this.mListener.onClose();
                }
            }
        });
    }

    private class DetailsAdapter extends BaseAdapter implements DetailsAddressResolver.AddressResolvingListener, DetailsHelper.ResolutionResolvingListener {
        private final ArrayList<String> mItems;
        private int mLocationIndex;
        private final Locale mDefaultLocale = Locale.getDefault();
        private final DecimalFormat mDecimalFormat = new DecimalFormat(".####");
        private int mWidthIndex = -1;
        private int mHeightIndex = -1;

        public DetailsAdapter(MediaDetails details) {
            Context context = DialogDetailsView.this.mActivity.getAndroidContext();
            this.mItems = new ArrayList<>(details.size());
            this.mLocationIndex = -1;
            setDetails(context, details);
        }

        private void setDetails(Context context, MediaDetails details) {
            String value;
            String value2;
            boolean resolutionIsValid = true;
            String path = null;
            for (Map.Entry<Integer, Object> detail : details) {
                switch (detail.getKey().intValue()) {
                    case 4:
                        double[] latlng = (double[]) detail.getValue();
                        this.mLocationIndex = this.mItems.size();
                        value = DetailsHelper.resolveAddress(DialogDetailsView.this.mActivity, latlng, this);
                        break;
                    case 5:
                        this.mWidthIndex = this.mItems.size();
                        if (detail.getValue().toString().equalsIgnoreCase("0")) {
                            value = context.getString(R.string.unknown);
                            resolutionIsValid = false;
                        } else {
                            value = toLocalInteger(detail.getValue());
                        }
                        break;
                    case FragmentManagerImpl.ANIM_STYLE_FADE_EXIT:
                        this.mHeightIndex = this.mItems.size();
                        if (detail.getValue().toString().equalsIgnoreCase("0")) {
                            value = context.getString(R.string.unknown);
                            resolutionIsValid = false;
                        } else {
                            value = toLocalInteger(detail.getValue());
                        }
                        break;
                    case 7:
                        value = toLocalInteger(detail.getValue());
                        break;
                    case 10:
                        value = Formatter.formatFileSize(context, ((Long) detail.getValue()).longValue());
                        break;
                    case 102:
                        MediaDetails.FlashState flash = (MediaDetails.FlashState) detail.getValue();
                        if (flash.isFlashFired()) {
                            value = context.getString(R.string.flash_on);
                        } else {
                            value = context.getString(R.string.flash_off);
                        }
                        break;
                    case 103:
                        double focalLength = Double.parseDouble(detail.getValue().toString());
                        value = toLocalNumber(focalLength);
                        break;
                    case 104:
                        value = "1".equals(detail.getValue()) ? context.getString(R.string.manual) : context.getString(R.string.auto);
                        break;
                    case 107:
                        String value3 = (String) detail.getValue();
                        double time = Double.valueOf(value3).doubleValue();
                        if (time < 1.0d) {
                            value = String.format(this.mDefaultLocale, "%d/%d", 1, Integer.valueOf((int) (0.5d + (1.0d / time))));
                        } else {
                            int integer = (int) time;
                            double time2 = time - ((double) integer);
                            value = String.valueOf(integer) + "''";
                            if (time2 > 1.0E-4d) {
                                value = value + String.format(this.mDefaultLocale, " %d/%d", 1, Integer.valueOf((int) (0.5d + (1.0d / time2))));
                            }
                        }
                        break;
                    case 108:
                        value = toLocalNumber(Integer.parseInt((String) detail.getValue()));
                        break;
                    case 200:
                        value = "\n" + detail.getValue().toString();
                        path = detail.getValue().toString();
                        break;
                    default:
                        Object valueObj = detail.getValue();
                        if (valueObj == null) {
                            Utils.fail("%s's value is Null", DetailsHelper.getDetailsName(context, detail.getKey().intValue()));
                        }
                        value = valueObj.toString();
                        break;
                }
                int key = detail.getKey().intValue();
                if (details.hasUnit(key)) {
                    value2 = String.format("%s: %s %s", DetailsHelper.getDetailsName(context, key), value, context.getString(details.getUnit(key)));
                } else {
                    value2 = String.format("%s: %s", DetailsHelper.getDetailsName(context, key), value);
                }
                this.mItems.add(value2);
            }
            if (!resolutionIsValid) {
                DetailsHelper.resolveResolution(path, this);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            return this.mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return DialogDetailsView.this.mDetails.getDetail(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv;
            if (convertView == null) {
                tv = (TextView) LayoutInflater.from(DialogDetailsView.this.mActivity.getAndroidContext()).inflate(R.layout.details, parent, false);
            } else {
                tv = (TextView) convertView;
            }
            tv.setText(this.mItems.get(position));
            return tv;
        }

        @Override
        public void onAddressAvailable(String address) {
            this.mItems.set(this.mLocationIndex, address);
            notifyDataSetChanged();
        }

        @Override
        public void onResolutionAvailable(int width, int height) {
            if (width != 0 && height != 0) {
                Context context = DialogDetailsView.this.mActivity.getAndroidContext();
                String widthString = String.format(this.mDefaultLocale, "%s: %d", DetailsHelper.getDetailsName(context, 5), Integer.valueOf(width));
                String heightString = String.format(this.mDefaultLocale, "%s: %d", DetailsHelper.getDetailsName(context, 6), Integer.valueOf(height));
                this.mItems.set(this.mWidthIndex, String.valueOf(widthString));
                this.mItems.set(this.mHeightIndex, String.valueOf(heightString));
                notifyDataSetChanged();
            }
        }

        private String toLocalInteger(Object valueObj) {
            if (valueObj instanceof Integer) {
                return toLocalNumber(((Integer) valueObj).intValue());
            }
            String value = valueObj.toString();
            try {
                return toLocalNumber(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                return value;
            }
        }

        private String toLocalNumber(int n) {
            return String.format(this.mDefaultLocale, "%d", Integer.valueOf(n));
        }

        private String toLocalNumber(double n) {
            return this.mDecimalFormat.format(n);
        }
    }

    @Override
    public void setCloseListener(DetailsHelper.CloseListener listener) {
        this.mListener = listener;
    }
}
