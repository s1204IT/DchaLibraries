package com.android.gallery3d.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class DialogDetailsView implements DetailsHelper.DetailsViewContainer {
    private static final String TAG = "Gallery2/DialogDetailsView";
    private final AbstractGalleryActivity mActivity;
    private DetailsAdapter mAdapter;
    private MediaDetails mDetails;
    private Dialog mDialog;
    private int mIndex;
    private DetailsHelper.CloseListener mListener;
    private final DetailsHelper.DetailsSource mSource;

    public DialogDetailsView(AbstractGalleryActivity abstractGalleryActivity, DetailsHelper.DetailsSource detailsSource) {
        this.mActivity = abstractGalleryActivity;
        this.mSource = detailsSource;
    }

    @Override
    public void show() {
        reloadDetails();
        if (this.mDialog == null) {
            return;
        }
        this.mDialog.show();
    }

    @Override
    public void hide() {
        if (this.mDialog == null) {
            return;
        }
        this.mDialog.hide();
    }

    @Override
    public void reloadDetails() {
        MediaDetails details;
        int index = this.mSource.setIndex();
        if (index != -1 && (details = this.mSource.getDetails()) != null) {
            if (this.mIndex == index && this.mDetails == details) {
                return;
            }
            this.mIndex = index;
            this.mDetails = details;
            setDetails(details);
        }
    }

    private void setDetails(MediaDetails mediaDetails) {
        this.mAdapter = new DetailsAdapter(mediaDetails);
        String str = String.format(this.mActivity.getAndroidContext().getString(R.string.details_title), Integer.valueOf(this.mIndex + 1), Integer.valueOf(this.mSource.size()));
        ListView listView = (ListView) LayoutInflater.from(this.mActivity.getAndroidContext()).inflate(R.layout.details_list, (ViewGroup) null, false);
        listView.setAdapter((ListAdapter) this.mAdapter);
        this.mDialog = new AlertDialog.Builder(this.mActivity).setView(listView).setTitle(str).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (DialogDetailsView.this.mDialog != null) {
                    DialogDetailsView.this.mDialog.dismiss();
                }
            }
        }).create();
        this.mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
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

        public DetailsAdapter(MediaDetails mediaDetails) {
            Context androidContext = DialogDetailsView.this.mActivity.getAndroidContext();
            this.mItems = new ArrayList<>(mediaDetails.size());
            this.mLocationIndex = -1;
            setDetails(androidContext, mediaDetails);
        }

        private void setDetails(Context context, MediaDetails mediaDetails) {
            String fileSize;
            String str;
            for (Map.Entry<Integer, Object> entry : mediaDetails) {
                int iIntValue = entry.getKey().intValue();
                if (iIntValue == 10) {
                    fileSize = Formatter.formatFileSize(context, ((Long) entry.getValue()).longValue());
                } else if (iIntValue != 200) {
                    switch (iIntValue) {
                        case 4:
                            double[] dArr = (double[]) entry.getValue();
                            this.mLocationIndex = this.mItems.size();
                            fileSize = DetailsHelper.resolveAddress(DialogDetailsView.this.mActivity, dArr, this);
                            break;
                        case 5:
                            this.mWidthIndex = this.mItems.size();
                            if (entry.getValue().toString().equalsIgnoreCase(SchemaSymbols.ATTVAL_FALSE_0)) {
                                fileSize = context.getString(R.string.unknown);
                            } else {
                                fileSize = toLocalInteger(entry.getValue());
                            }
                            break;
                        case 6:
                            this.mHeightIndex = this.mItems.size();
                            if (entry.getValue().toString().equalsIgnoreCase(SchemaSymbols.ATTVAL_FALSE_0)) {
                                fileSize = context.getString(R.string.unknown);
                            } else {
                                fileSize = toLocalInteger(entry.getValue());
                            }
                            break;
                        case 7:
                            fileSize = toLocalInteger(entry.getValue());
                            break;
                        default:
                            switch (iIntValue) {
                                case 102:
                                    if (((MediaDetails.FlashState) entry.getValue()).isFlashFired()) {
                                        fileSize = context.getString(R.string.flash_on);
                                    } else {
                                        fileSize = context.getString(R.string.flash_off);
                                    }
                                    break;
                                case 103:
                                    fileSize = toLocalNumber(Double.parseDouble(entry.getValue().toString()));
                                    break;
                                case 104:
                                    if (SchemaSymbols.ATTVAL_TRUE_1.equals(entry.getValue())) {
                                        fileSize = context.getString(R.string.manual);
                                    } else {
                                        fileSize = context.getString(R.string.auto);
                                    }
                                    break;
                                default:
                                    switch (iIntValue) {
                                        case 107:
                                            double dDoubleValue = Double.valueOf((String) entry.getValue()).doubleValue();
                                            if (dDoubleValue < 1.0d) {
                                                fileSize = String.format(this.mDefaultLocale, "%d/%d", 1, Integer.valueOf((int) (0.5d + (1.0d / dDoubleValue))));
                                            } else {
                                                int i = (int) dDoubleValue;
                                                double d = dDoubleValue - ((double) i);
                                                String str2 = String.valueOf(i) + "''";
                                                fileSize = d <= 1.0E-4d ? str2 : str2 + String.format(this.mDefaultLocale, " %d/%d", 1, Integer.valueOf((int) (0.5d + (1.0d / d))));
                                            }
                                            break;
                                        case 108:
                                            fileSize = toLocalNumber(Integer.parseInt((String) entry.getValue()));
                                            break;
                                        default:
                                            Object value = entry.getValue();
                                            if (value == null) {
                                                Utils.fail("%s's value is Null", DetailsHelper.getDetailsName(context, entry.getKey().intValue()));
                                            }
                                            fileSize = value.toString();
                                            break;
                                    }
                                    break;
                            }
                            break;
                    }
                } else {
                    fileSize = "\n" + entry.getValue().toString();
                    entry.getValue().toString();
                }
                int iIntValue2 = entry.getKey().intValue();
                if (mediaDetails.hasUnit(iIntValue2)) {
                    str = String.format("%s: %s %s", DetailsHelper.getDetailsName(context, iIntValue2), fileSize, context.getString(mediaDetails.getUnit(iIntValue2)));
                } else {
                    str = String.format("%s: %s", DetailsHelper.getDetailsName(context, iIntValue2), fileSize);
                }
                this.mItems.add(str);
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return false;
        }

        @Override
        public int getCount() {
            return this.mItems.size();
        }

        @Override
        public Object getItem(int i) {
            return DialogDetailsView.this.mDetails.getDetail(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView textView;
            if (view == null) {
                textView = (TextView) LayoutInflater.from(DialogDetailsView.this.mActivity.getAndroidContext()).inflate(R.layout.details, viewGroup, false);
            } else {
                textView = (TextView) view;
            }
            textView.setText(this.mItems.get(i));
            return textView;
        }

        @Override
        public void onAddressAvailable(String str) {
            this.mItems.set(this.mLocationIndex, str);
            notifyDataSetChanged();
        }

        @Override
        public void onResolutionAvailable(int i, int i2) {
            if (i != 0 && i2 != 0) {
                Context androidContext = DialogDetailsView.this.mActivity.getAndroidContext();
                String str = String.format(this.mDefaultLocale, "%s: %d", DetailsHelper.getDetailsName(androidContext, 5), Integer.valueOf(i));
                String str2 = String.format(this.mDefaultLocale, "%s: %d", DetailsHelper.getDetailsName(androidContext, 6), Integer.valueOf(i2));
                this.mItems.set(this.mWidthIndex, String.valueOf(str));
                this.mItems.set(this.mHeightIndex, String.valueOf(str2));
                notifyDataSetChanged();
            }
        }

        private String toLocalInteger(Object obj) {
            if (obj instanceof Integer) {
                return toLocalNumber(obj.intValue());
            }
            String string = obj.toString();
            try {
                return toLocalNumber(Integer.parseInt(string));
            } catch (NumberFormatException e) {
                return string;
            }
        }

        private String toLocalNumber(int i) {
            return String.format(this.mDefaultLocale, "%d", Integer.valueOf(i));
        }

        private String toLocalNumber(double d) {
            return this.mDecimalFormat.format(d);
        }
    }

    @Override
    public void setCloseListener(DetailsHelper.CloseListener closeListener) {
        this.mListener = closeListener;
    }
}
