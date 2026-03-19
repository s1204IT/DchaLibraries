package com.android.settings.localepicker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.LocaleList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import java.util.ArrayList;
import java.util.List;

public class LocaleListEditor extends RestrictedSettingsFragment implements LocalePickerWithRegion.LocaleSelectedListener {
    private LocaleDragAndDropAdapter mAdapter;
    private View mAddLanguage;
    private boolean mIsUiRestricted;
    private Menu mMenu;
    private boolean mRemoveMode;
    private boolean mShowingRemoveDialog;

    public LocaleListEditor() {
        super("no_config_locale");
    }

    @Override
    public int getMetricsCategory() {
        return 344;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        LocaleStore.fillCache(getContext());
        this.mAdapter = new LocaleDragAndDropAdapter(getContext(), getUserLocaleList());
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View viewOnCreateView = super.onCreateView(layoutInflater, viewGroup, bundle);
        configureDragAndDrop(layoutInflater.inflate(R.layout.locale_order_list, (ViewGroup) viewOnCreateView));
        return viewOnCreateView;
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean z = this.mIsUiRestricted;
        this.mIsUiRestricted = isUiRestricted();
        TextView emptyTextView = getEmptyTextView();
        if (this.mIsUiRestricted && !z) {
            emptyTextView.setText(R.string.language_empty_list_user_restricted);
            emptyTextView.setVisibility(0);
            updateVisibilityOfRemoveMenu();
        } else if (!this.mIsUiRestricted && z) {
            emptyTextView.setVisibility(8);
            updateVisibilityOfRemoveMenu();
        }
    }

    @Override
    public void onViewStateRestored(Bundle bundle) {
        super.onViewStateRestored(bundle);
        if (bundle != null) {
            this.mRemoveMode = bundle.getBoolean("localeRemoveMode", false);
            this.mShowingRemoveDialog = bundle.getBoolean("showingLocaleRemoveDialog", false);
        }
        setRemoveMode(this.mRemoveMode);
        this.mAdapter.restoreState(bundle);
        if (this.mShowingRemoveDialog) {
            showRemoveLocaleWarningDialog();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("localeRemoveMode", this.mRemoveMode);
        bundle.putBoolean("showingLocaleRemoveDialog", this.mShowingRemoveDialog);
        this.mAdapter.saveState(bundle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 2) {
            if (this.mRemoveMode) {
                showRemoveLocaleWarningDialog();
            } else {
                setRemoveMode(true);
            }
            return true;
        }
        if (itemId == 16908332 && this.mRemoveMode) {
            setRemoveMode(false);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void setRemoveMode(boolean z) {
        this.mRemoveMode = z;
        this.mAdapter.setRemoveMode(z);
        this.mAddLanguage.setVisibility(z ? 4 : 0);
        updateVisibilityOfRemoveMenu();
    }

    private void showRemoveLocaleWarningDialog() {
        int checkedCount = this.mAdapter.getCheckedCount();
        if (checkedCount == 0) {
            setRemoveMode(!this.mRemoveMode);
            return;
        }
        if (checkedCount == this.mAdapter.getItemCount()) {
            this.mShowingRemoveDialog = true;
            new AlertDialog.Builder(getActivity()).setTitle(R.string.dlg_remove_locales_error_title).setMessage(R.string.dlg_remove_locales_error_message).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    LocaleListEditor.this.mShowingRemoveDialog = false;
                }
            }).create().show();
        } else {
            String quantityString = getResources().getQuantityString(R.plurals.dlg_remove_locales_title, checkedCount);
            this.mShowingRemoveDialog = true;
            new AlertDialog.Builder(getActivity()).setTitle(quantityString).setMessage(R.string.dlg_remove_locales_message).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    LocaleListEditor.this.setRemoveMode(false);
                }
            }).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    LocaleListEditor.this.mRemoveMode = false;
                    LocaleListEditor.this.mShowingRemoveDialog = false;
                    LocaleListEditor.this.mAdapter.removeChecked();
                    LocaleListEditor.this.setRemoveMode(false);
                }
            }).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    LocaleListEditor.this.mShowingRemoveDialog = false;
                }
            }).create().show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        MenuItem menuItemAdd = menu.add(0, 2, 0, R.string.locale_remove_menu);
        menuItemAdd.setShowAsAction(4);
        menuItemAdd.setIcon(R.drawable.ic_delete);
        super.onCreateOptionsMenu(menu, menuInflater);
        this.mMenu = menu;
        updateVisibilityOfRemoveMenu();
    }

    private List<LocaleStore.LocaleInfo> getUserLocaleList() {
        ArrayList arrayList = new ArrayList();
        LocaleList locales = LocalePicker.getLocales();
        for (int i = 0; i < locales.size(); i++) {
            arrayList.add(LocaleStore.getLocaleInfo(locales.get(i)));
        }
        return arrayList;
    }

    private void configureDragAndDrop(View view) {
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.dragList);
        LocaleLinearLayoutManager localeLinearLayoutManager = new LocaleLinearLayoutManager(getContext(), this.mAdapter);
        localeLinearLayoutManager.setAutoMeasureEnabled(true);
        recyclerView.setLayoutManager(localeLinearLayoutManager);
        recyclerView.setHasFixedSize(true);
        this.mAdapter.setRecyclerView(recyclerView);
        recyclerView.setAdapter(this.mAdapter);
        this.mAddLanguage = view.findViewById(R.id.add_language);
        this.mAddLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                LocaleListEditor.this.getFragmentManager().beginTransaction().setTransition(4097).replace(LocaleListEditor.this.getId(), LocalePickerWithRegion.createLanguagePicker(LocaleListEditor.this.getContext(), LocaleListEditor.this, false)).addToBackStack("localeListEditor").commit();
            }
        });
    }

    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
        this.mAdapter.addLocale(localeInfo);
        updateVisibilityOfRemoveMenu();
    }

    private void updateVisibilityOfRemoveMenu() {
        if (this.mMenu == null) {
            return;
        }
        MenuItem menuItemFindItem = this.mMenu.findItem(2);
        if (menuItemFindItem != null) {
            menuItemFindItem.setShowAsAction(this.mRemoveMode ? 2 : 0);
            menuItemFindItem.setVisible((this.mAdapter.getItemCount() > 1) && !this.mIsUiRestricted);
        }
    }
}
