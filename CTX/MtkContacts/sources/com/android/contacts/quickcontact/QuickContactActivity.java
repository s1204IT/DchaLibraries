package com.android.contacts.quickcontact;

import android.accounts.Account;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.Trace;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.os.BuildCompat;
import android.support.v7.graphics.Palette;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.contacts.CallUtil;
import com.android.contacts.ClipboardUtils;
import com.android.contacts.Collapser;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.DynamicShortcuts;
import com.android.contacts.NfcHandler;
import com.android.contacts.R;
import com.android.contacts.ShortcutIntentBuilder;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.activities.RequestDesiredPermissionsActivity;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.EventCompat;
import com.android.contacts.compat.MultiWindowCompat;
import com.android.contacts.detail.ContactDisplayUtils;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.editor.EditorUiUtils;
import com.android.contacts.interactions.CalendarInteractionsLoader;
import com.android.contacts.interactions.CallLogInteractionsLoader;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.interactions.ContactInteraction;
import com.android.contacts.interactions.SmsInteractionsLoader;
import com.android.contacts.interactions.TouchPointManager;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.logging.Logger;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.Contact;
import com.android.contacts.model.ContactLoader;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.model.dataitem.CustomDataItem;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.dataitem.EmailDataItem;
import com.android.contacts.model.dataitem.EventDataItem;
import com.android.contacts.model.dataitem.GroupMembershipDataItem;
import com.android.contacts.model.dataitem.ImDataItem;
import com.android.contacts.model.dataitem.NicknameDataItem;
import com.android.contacts.model.dataitem.NoteDataItem;
import com.android.contacts.model.dataitem.OrganizationDataItem;
import com.android.contacts.model.dataitem.PhoneDataItem;
import com.android.contacts.model.dataitem.RelationDataItem;
import com.android.contacts.model.dataitem.SipAddressDataItem;
import com.android.contacts.model.dataitem.StructuredNameDataItem;
import com.android.contacts.model.dataitem.StructuredPostalDataItem;
import com.android.contacts.model.dataitem.WebsiteDataItem;
import com.android.contacts.quickcontact.ExpandingEntryCardView;
import com.android.contacts.quickcontact.WebAddress;
import com.android.contacts.util.DateUtils;
import com.android.contacts.util.ImageViewDrawableSetter;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.SchedulingUtils;
import com.android.contacts.util.SharedPreferenceUtil;
import com.android.contacts.util.StructuredPostalUtils;
import com.android.contacts.util.UriUtils;
import com.android.contacts.util.ViewUtil;
import com.android.contacts.widget.MultiShrinkScroller;
import com.android.contacts.widget.QuickContactImageView;
import com.android.contactsbind.HelpUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.quickcontact.QuickContactUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuickContactActivity extends ContactsActivity {
    private ExpandingEntryCardView mAboutCard;
    private boolean mArePhoneOptionsChangable;
    private Cp2DataCardModel mCachedCp2DataCardModel;
    private PorterDuffColorFilter mColorFilter;
    private int mColorFilterColor;
    private ExpandingEntryCardView mContactCard;
    private Contact mContactData;
    private ContactLoader mContactLoader;
    private int mContactType;
    private String mCustomRingtone;
    private AsyncTask<Void, Void, Cp2DataCardModel> mEntriesAndActionsTask;
    private String[] mExcludeMimes;
    private int mExtraMode;
    private String mExtraPrioritizedMimeType;
    private boolean mHasAlreadyBeenOpened;
    private boolean mHasComputedThemeColor;
    private boolean mHasIntentLaunched;
    private boolean mIsEntranceAnimationFinished;
    private boolean mIsExitAnimationInProgress;
    private boolean mIsRecreatedInstance;
    private ExpandingEntryCardView mJoynCard;
    private SaveServiceListener mListener;
    private Uri mLookupUri;
    private MaterialColorMapUtils mMaterialColorMapUtils;
    private ExpandingEntryCardView mNoContactDetailsCard;
    private boolean mOnlyOneEmail;
    private boolean mOnlyOnePhoneNumber;
    private ExpandingEntryCardView mPermissionExplanationCard;
    private QuickContactImageView mPhotoView;
    private ProgressDialog mProgressDialog;
    private ExpandingEntryCardView mRecentCard;
    private AsyncTask<Void, Void, Void> mRecentDataTask;
    private String mReferrer;
    private MultiShrinkScroller mScroller;
    private boolean mSendToVoicemailState;
    private boolean mShouldLog;
    private int mStatusBarColor;
    private ColorDrawable mWindowScrim;
    private static final int SCRIM_COLOR = Color.argb(200, 0, 0, 0);
    private static final int CURRENT_API_VERSION = Build.VERSION.SDK_INT;
    private static final List<String> LEADING_MIMETYPES = Lists.newArrayList("vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/sip_address", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/postal-address_v2");
    private static final List<String> SORTED_ABOUT_CARD_MIMETYPES = Lists.newArrayList("vnd.android.cursor.item/nickname", "vnd.android.cursor.item/website", "vnd.android.cursor.item/organization", "vnd.android.cursor.item/contact_event", "vnd.android.cursor.item/relation", "vnd.android.cursor.item/im", "vnd.android.cursor.item/group_membership", "vnd.android.cursor.item/identity", "vnd.com.google.cursor.item/contact_user_defined_field", "vnd.android.cursor.item/note");
    private static final BidiFormatter sBidiFormatter = BidiFormatter.getInstance();
    private static final String KEY_LOADER_EXTRA_EMAILS = QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_EMAILS";
    private static final String KEY_LOADER_EXTRA_PHONES = QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_PHONES";
    private static final String KEY_LOADER_EXTRA_SIP_NUMBERS = QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_SIP_NUMBERS";
    private static final int[] mRecentLoaderIds = {1, 2, 3};
    private boolean mShortcutUsageReported = false;
    private long mPreviousContactId = 0;
    private boolean mShouldShowPermissionExplanation = false;
    private String mPermissionExplanationCardSubHeader = "";
    private boolean mIsResumed = false;
    private final ImageViewDrawableSetter mPhotoSetter = new ImageViewDrawableSetter();
    private Map<Integer, List<ContactInteraction>> mRecentLoaderResults = new ConcurrentHashMap(4, 0.9f, 1);
    final View.OnClickListener mEntryClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Object tag = view.getTag();
            if (tag == null || !(tag instanceof ExpandingEntryCardView.EntryTag)) {
                Log.w("QuickContact", "EntryTag was not used correctly");
                return;
            }
            ExpandingEntryCardView.EntryTag entryTag = (ExpandingEntryCardView.EntryTag) tag;
            Intent intent = entryTag.getIntent();
            int id = entryTag.getId();
            Log.d("QuickContact", "[onClick]intent = " + intent + ",dataId = " + id);
            if (id == -2) {
                QuickContactActivity.this.editContact();
                return;
            }
            if (id == -3) {
                QuickContactActivity.this.finish();
                RequestDesiredPermissionsActivity.startPermissionActivity(QuickContactActivity.this);
                return;
            }
            if ("android.intent.action.CALL".equals(intent.getAction()) && 12 == intent.getIntExtra("action_type", -1)) {
                ExtensionManager.getInstance();
                if (!ExtensionManager.getContactsCommonPresenceExtension().isVideoIconClickable(intent.getData())) {
                    return;
                }
            }
            if ("android.intent.action.CALL".equals(intent.getAction()) && TouchPointManager.getInstance().hasValidPoint()) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("touchPoint", TouchPointManager.getInstance().getPoint());
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS", bundle);
            }
            boolean z = true;
            QuickContactActivity.this.mHasIntentLaunched = true;
            try {
                Logger.logQuickContactEvent(QuickContactActivity.this.mReferrer, QuickContactActivity.this.mContactType, 0, intent.getIntExtra("action_type", 0), intent.getStringExtra("third_party_action"));
                if ("com.google.android.apps.tachyon.action.CALL".equals(intent.getAction())) {
                    QuickContactActivity.this.startActivityForResult(intent, 0);
                } else {
                    intent.addFlags(268435456);
                    if (BenesseExtension.getDchaState() == 0) {
                        ImplicitIntentsUtil.startActivityInAppIfPossible(QuickContactActivity.this, intent);
                    }
                }
            } catch (ActivityNotFoundException e) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app, 0).show();
            } catch (SecurityException e2) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app, 0).show();
                Log.e("QuickContact", "QuickContacts does not have permission to launch " + intent);
            }
            String str = "call";
            Uri data = intent.getData();
            if ((data != null && data.getScheme() != null && data.getScheme().equals(ContactsUtils.SCHEME_SMSTO)) || (intent.getType() != null && intent.getType().equals("vnd.android-dir/mms-sms"))) {
                str = "short_text";
            }
            if (id > 0) {
                try {
                    if (QuickContactActivity.this.getContentResolver().update(ContactsContract.DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(String.valueOf(id)).appendQueryParameter(BaseAccountType.Attr.TYPE, str).build(), new ContentValues(), null, null) <= 0) {
                        z = false;
                    }
                    if (!z) {
                        Log.w("QuickContact", "DataUsageFeedback increment failed");
                        return;
                    }
                    return;
                } catch (SecurityException e3) {
                    Log.w("QuickContact", "DataUsageFeedback increment failed", e3);
                    return;
                }
            }
            Log.w("QuickContact", "Invalid Data ID");
        }
    };
    final ExpandingEntryCardView.ExpandingEntryCardViewListener mExpandingEntryCardViewListener = new ExpandingEntryCardView.ExpandingEntryCardViewListener() {
        @Override
        public void onCollapse(int i) {
            QuickContactActivity.this.mScroller.prepareForShrinkingScrollChild(i);
        }

        @Override
        public void onExpand() {
            QuickContactActivity.this.mScroller.setDisableTouchesForSuppressLayout(true);
        }

        @Override
        public void onExpandDone() {
            QuickContactActivity.this.mScroller.setDisableTouchesForSuppressLayout(false);
        }
    };
    private final View.OnCreateContextMenuListener mEntryContextMenuListener = new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
            boolean z;
            if (contextMenuInfo == null) {
                return;
            }
            ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo = (ExpandingEntryCardView.EntryContextMenuInfo) contextMenuInfo;
            contextMenu.setHeaderTitle(entryContextMenuInfo.getCopyText());
            contextMenu.add(0, 0, 0, QuickContactActivity.this.getString(R.string.copy_text));
            if (!QuickContactActivity.this.isContactEditable()) {
                return;
            }
            String mimeType = entryContextMenuInfo.getMimeType();
            if ("vnd.android.cursor.item/phone_v2".equals(mimeType)) {
                z = QuickContactActivity.this.mOnlyOnePhoneNumber;
            } else {
                z = "vnd.android.cursor.item/email_v2".equals(mimeType) ? QuickContactActivity.this.mOnlyOneEmail : true;
            }
            if (entryContextMenuInfo.isSuperPrimary()) {
                contextMenu.add(0, 1, 0, QuickContactActivity.this.getString(R.string.clear_default));
            } else if (!z) {
                contextMenu.add(0, 2, 0, QuickContactActivity.this.getString(R.string.set_default));
            }
        }
    };
    final MultiShrinkScroller.MultiShrinkScrollerListener mMultiShrinkScrollerListener = new MultiShrinkScroller.MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            QuickContactActivity.this.finish();
        }

        @Override
        public void onEnterFullscreen() {
            QuickContactActivity.this.updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            QuickContactActivity.this.updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {
            QuickContactActivity.this.mIsExitAnimationInProgress = true;
        }

        @Override
        public void onEntranceAnimationDone() {
            QuickContactActivity.this.mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float f) {
            if (QuickContactActivity.this.mIsEntranceAnimationFinished) {
                QuickContactActivity.this.mWindowScrim.setAlpha((int) (255.0f * f));
            }
        }
    };
    private final Comparator<DataItem> mWithinMimeTypeDataItemComparator = new Comparator<DataItem>() {
        @Override
        public int compare(DataItem dataItem, DataItem dataItem2) {
            int iIntValue;
            if (!dataItem.getMimeType().equals(dataItem2.getMimeType())) {
                Log.wtf("QuickContact", "Comparing DataItems with different mimetypes lhs.getMimeType(): " + dataItem.getMimeType() + " rhs.getMimeType(): " + dataItem2.getMimeType());
                return 0;
            }
            if (dataItem.isSuperPrimary()) {
                return -1;
            }
            if (dataItem2.isSuperPrimary()) {
                return 1;
            }
            if (dataItem.isPrimary() && !dataItem2.isPrimary()) {
                return -1;
            }
            if (!dataItem.isPrimary() && dataItem2.isPrimary()) {
                return 1;
            }
            if (dataItem.getTimesUsed() != null) {
                iIntValue = dataItem.getTimesUsed().intValue();
            } else {
                iIntValue = 0;
            }
            return (dataItem2.getTimesUsed() != null ? dataItem2.getTimesUsed().intValue() : 0) - iIntValue;
        }
    };
    private final Comparator<List<DataItem>> mAmongstMimeTypeDataItemComparator = new Comparator<List<DataItem>>() {
        @Override
        public int compare(List<DataItem> list, List<DataItem> list2) {
            int iIntValue;
            int iIntValue2;
            long jLongValue;
            long jLongValue2;
            DataItem dataItem = list.get(0);
            DataItem dataItem2 = list2.get(0);
            String mimeType = dataItem.getMimeType();
            String mimeType2 = dataItem2.getMimeType();
            if (!TextUtils.isEmpty(QuickContactActivity.this.mExtraPrioritizedMimeType) && !mimeType.equals(mimeType2)) {
                if (mimeType2.equals(QuickContactActivity.this.mExtraPrioritizedMimeType)) {
                    return 1;
                }
                if (mimeType.equals(QuickContactActivity.this.mExtraPrioritizedMimeType)) {
                    return -1;
                }
            }
            if (dataItem.getTimesUsed() != null) {
                iIntValue = dataItem.getTimesUsed().intValue();
            } else {
                iIntValue = 0;
            }
            if (dataItem2.getTimesUsed() != null) {
                iIntValue2 = dataItem2.getTimesUsed().intValue();
            } else {
                iIntValue2 = 0;
            }
            int i = iIntValue2 - iIntValue;
            if (i != 0) {
                return i;
            }
            if (dataItem.getLastTimeUsed() != null) {
                jLongValue = dataItem.getLastTimeUsed().longValue();
            } else {
                jLongValue = 0;
            }
            if (dataItem2.getLastTimeUsed() != null) {
                jLongValue2 = dataItem2.getLastTimeUsed().longValue();
            } else {
                jLongValue2 = 0;
            }
            long j = jLongValue2 - jLongValue;
            if (j > 0) {
                return 1;
            }
            if (j < 0) {
                return -1;
            }
            if (!mimeType.equals(mimeType2)) {
                for (String str : QuickContactActivity.LEADING_MIMETYPES) {
                    if (mimeType.equals(str)) {
                        return -1;
                    }
                    if (mimeType2.equals(str)) {
                        return 1;
                    }
                }
            }
            return 0;
        }
    };
    private final LoaderManager.LoaderCallbacks<Contact> mLoaderContactCallbacks = new LoaderManager.LoaderCallbacks<Contact>() {
        @Override
        public void onLoaderReset(Loader<Contact> loader) {
            Log.d("QuickContact", "[onLoaderReset], mContactData been set null");
            QuickContactActivity.this.mContactData = null;
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact contact) {
            Trace.beginSection("onLoadFinished()");
            try {
                if (QuickContactActivity.this.isFinishing()) {
                    return;
                }
                if (contact.isError()) {
                    Log.i("QuickContact", "Failed to load contact: " + ((ContactLoader) loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage, 1).show();
                    QuickContactActivity.this.finish();
                    return;
                }
                if (contact.isNotFound()) {
                    Log.i("QuickContact", "No contact found: " + ((ContactLoader) loader).getLookupUri());
                    ExtensionManager.getInstance();
                    if (!ExtensionManager.getContactsPickerExtension().openAddProfileScreen(QuickContactActivity.this.mLookupUri, QuickContactActivity.this)) {
                        Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage, 1).show();
                    }
                    QuickContactActivity.this.finish();
                    return;
                }
                if (!QuickContactActivity.this.mIsRecreatedInstance && !QuickContactActivity.this.mShortcutUsageReported && contact != null) {
                    QuickContactActivity.this.mShortcutUsageReported = true;
                    DynamicShortcuts.reportShortcutUsed(QuickContactActivity.this, contact.getLookupKey());
                }
                QuickContactActivity.this.bindContactData(contact);
                ExtensionManager.getInstance();
                ExtensionManager.getRcsExtension().getQuickContactRcsScroller().updateRcsContact(QuickContactActivity.this.mContactLoader.getLookupUri(), true);
                Log.d("QuickContact", "onLoadFinished end");
            } finally {
                Trace.endSection();
            }
        }

        @Override
        public Loader<Contact> onCreateLoader(int i, Bundle bundle) {
            if (QuickContactActivity.this.mLookupUri == null) {
                Log.wtf("QuickContact", "Lookup uri wasn't initialized. Loader was started too early");
            }
            return new ContactLoader(QuickContactActivity.this.getApplicationContext(), QuickContactActivity.this.mLookupUri, true, true, true);
        }
    };
    private final LoaderManager.LoaderCallbacks<List<ContactInteraction>> mLoaderInteractionsCallbacks = new LoaderManager.LoaderCallbacks<List<ContactInteraction>>() {
        @Override
        public Loader<List<ContactInteraction>> onCreateLoader(int i, Bundle bundle) {
            switch (i) {
                case 1:
                    return new SmsInteractionsLoader(QuickContactActivity.this, bundle.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_PHONES), 3);
                case 2:
                    return new CalendarInteractionsLoader(QuickContactActivity.this, bundle.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_EMAILS) != null ? Arrays.asList(bundle.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_EMAILS)) : null, 3, 3, 604800000L, 86400000L);
                case 3:
                    return new CallLogInteractionsLoader(QuickContactActivity.this, bundle.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_PHONES), bundle.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_SIP_NUMBERS), 3);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<List<ContactInteraction>> loader, List<ContactInteraction> list) {
            QuickContactActivity.this.mRecentLoaderResults.put(Integer.valueOf(loader.getId()), list);
            if (QuickContactActivity.this.isAllRecentDataLoaded()) {
                Log.d("QuickContact", "all recent data loaded");
                QuickContactActivity.this.bindRecentData();
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ContactInteraction>> loader) {
            QuickContactActivity.this.mRecentLoaderResults.remove(Integer.valueOf(loader.getId()));
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        try {
            ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo = (ExpandingEntryCardView.EntryContextMenuInfo) menuItem.getMenuInfo();
            switch (menuItem.getItemId()) {
                case 0:
                    ClipboardUtils.copyText(this, entryContextMenuInfo.getCopyLabel(), entryContextMenuInfo.getCopyText(), true);
                    return true;
                case 1:
                    startService(ContactSaveService.createClearPrimaryIntent(this, entryContextMenuInfo.getId()));
                    return true;
                case 2:
                    startService(ContactSaveService.createSetSuperPrimaryIntent(this, entryContextMenuInfo.getId()));
                    return true;
                default:
                    throw new IllegalArgumentException("Unknown menu option " + menuItem.getItemId());
            }
        } catch (ClassCastException e) {
            Log.e("QuickContact", "bad menuInfo", e);
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            TouchPointManager.getInstance().setPoint((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    @Override
    protected void onCreate(Bundle bundle) {
        Trace.beginSection("onCreate()");
        super.onCreate(bundle);
        Log.sensitive("QuickContact", "[onCreate()] bundle = " + bundle);
        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }
        this.mIsRecreatedInstance = bundle != null;
        if (this.mIsRecreatedInstance) {
            this.mPreviousContactId = bundle.getLong("previous_contact_id");
            this.mSendToVoicemailState = bundle.getBoolean("sendToVoicemailState");
            this.mArePhoneOptionsChangable = bundle.getBoolean("arePhoneOptionsChangable");
            this.mCustomRingtone = bundle.getString(ContactSaveService.EXTRA_CUSTOM_RINGTONE);
        }
        this.mProgressDialog = new ProgressDialog(this);
        this.mProgressDialog.setIndeterminate(true);
        this.mProgressDialog.setCancelable(false);
        this.mListener = new SaveServiceListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ContactSaveService.BROADCAST_LINK_COMPLETE);
        intentFilter.addAction(ContactSaveService.BROADCAST_UNLINK_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mListener, intentFilter);
        this.mShouldLog = true;
        boolean zHasSystemFeature = getPackageManager().hasSystemFeature("android.hardware.telephony");
        boolean zHasPermission = PermissionsUtil.hasPermission(this, "android.permission.READ_CALENDAR");
        boolean z = zHasSystemFeature && PermissionsUtil.hasPermission(this, "android.permission.READ_SMS");
        boolean zShouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.READ_CALENDAR");
        boolean z2 = zHasSystemFeature && ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.READ_SMS");
        boolean z3 = (zHasPermission || zShouldShowRequestPermissionRationale) ? false : true;
        boolean z4 = (!zHasSystemFeature || z || z2) ? false : true;
        this.mShouldShowPermissionExplanation = z3 || z4;
        if (z3 && z4) {
            this.mPermissionExplanationCardSubHeader = getString(R.string.permission_explanation_subheader_calendar_and_SMS);
        } else if (z3) {
            this.mPermissionExplanationCardSubHeader = getString(R.string.permission_explanation_subheader_calendar);
        } else if (z4) {
            this.mPermissionExplanationCardSubHeader = getString(R.string.permission_explanation_subheader_SMS);
        }
        Logger.logScreenView(this, 5, getIntent().getIntExtra("previous_screen_type", 0));
        this.mReferrer = getCallingPackage();
        if (this.mReferrer == null && CompatUtils.isLollipopMr1Compatible() && getReferrer() != null) {
            this.mReferrer = getReferrer().getAuthority();
        }
        this.mContactType = 0;
        if (CompatUtils.isLollipopCompatible()) {
            getWindow().setStatusBarColor(0);
        }
        processIntent(getIntent());
        getWindow().setFlags(131072, 131072);
        setContentView(R.layout.quickcontact_activity);
        this.mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());
        this.mScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);
        this.mContactCard = (ExpandingEntryCardView) findViewById(R.id.communication_card);
        ExtensionManager.getInstance();
        this.mJoynCard = (ExpandingEntryCardView) ExtensionManager.getViewCustomExtension().getQuickContactCardViewCustom().createCardView((LinearLayout) findViewById(R.id.card_container), this.mContactCard, this.mLookupUri, this);
        this.mNoContactDetailsCard = (ExpandingEntryCardView) findViewById(R.id.no_contact_data_card);
        this.mRecentCard = (ExpandingEntryCardView) findViewById(R.id.recent_card);
        this.mAboutCard = (ExpandingEntryCardView) findViewById(R.id.about_card);
        this.mPermissionExplanationCard = (ExpandingEntryCardView) findViewById(R.id.permission_explanation_card);
        this.mPermissionExplanationCard.setOnClickListener(this.mEntryClickHandler);
        this.mNoContactDetailsCard.setOnClickListener(this.mEntryClickHandler);
        this.mContactCard.setOnClickListener(this.mEntryClickHandler);
        this.mContactCard.setOnCreateContextMenuListener(this.mEntryContextMenuListener);
        this.mRecentCard.setOnClickListener(this.mEntryClickHandler);
        this.mRecentCard.setTitle(getResources().getString(R.string.recent_card_title));
        this.mAboutCard.setOnClickListener(this.mEntryClickHandler);
        this.mAboutCard.setOnCreateContextMenuListener(this.mEntryContextMenuListener);
        this.mPhotoView = (QuickContactImageView) findViewById(R.id.photo);
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().updateContactPhotoFromRcsServer(this.mLookupUri, this.mPhotoView, this);
        View viewFindViewById = findViewById(R.id.transparent_view);
        if (this.mScroller != null) {
            viewFindViewById.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    QuickContactActivity.this.mScroller.scrollOffBottom();
                }
            });
        }
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle((CharSequence) null);
        toolbar.addView(getLayoutInflater().inflate(R.layout.quickcontact_title_placeholder, (ViewGroup) null));
        this.mHasAlreadyBeenOpened = bundle != null;
        this.mIsEntranceAnimationFinished = this.mHasAlreadyBeenOpened;
        this.mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        this.mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(this.mWindowScrim);
        this.mScroller.initialize(this.mMultiShrinkScrollerListener, this.mExtraMode == 4, -1, true);
        this.mScroller.setVisibility(4);
        setHeaderNameText(R.string.missing_name);
        SchedulingUtils.doOnPreDraw(this.mScroller, true, new Runnable() {
            @Override
            public void run() {
                float startingTransparentHeightRatio;
                if (!QuickContactActivity.this.mHasAlreadyBeenOpened) {
                    if (QuickContactActivity.this.mExtraMode != 4) {
                        startingTransparentHeightRatio = QuickContactActivity.this.mScroller.getStartingTransparentHeightRatio();
                    } else {
                        startingTransparentHeightRatio = 1.0f;
                    }
                    ObjectAnimator.ofInt(QuickContactActivity.this.mWindowScrim, "alpha", 0, (int) (255.0f * startingTransparentHeightRatio)).setDuration(QuickContactActivity.this.getResources().getInteger(android.R.integer.config_shortAnimTime)).start();
                }
            }
        });
        if (bundle != null) {
            final int i = bundle.getInt("theme_color", 0);
            SchedulingUtils.doOnPreDraw(this.mScroller, false, new Runnable() {
                @Override
                public void run() {
                    if (QuickContactActivity.this.mHasAlreadyBeenOpened) {
                        QuickContactActivity.this.mScroller.setVisibility(0);
                        QuickContactActivity.this.mScroller.setScroll(QuickContactActivity.this.mScroller.getScrollNeededToBeFullScreen());
                    }
                    if (i != 0) {
                        QuickContactActivity.this.setThemeColor(QuickContactActivity.this.mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(i));
                    }
                }
            });
        }
        Trace.endSection();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        boolean z = true;
        if (i != 1 || (i2 != 3 && i2 != 2)) {
            z = false;
        }
        setResult(i2);
        if (z) {
            finish();
            return;
        }
        if (i == 2 && i2 != 0) {
            this.mHasComputedThemeColor = false;
            processIntent(intent);
        } else {
            if (i == 3) {
                if (i2 == -1 && intent != null) {
                    joinAggregate(ContentUris.parseId(intent.getData()));
                    return;
                }
                return;
            }
            if (i == 4 && intent != null) {
                onRingtonePicked((Uri) intent.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI"));
            }
        }
    }

    private void onRingtonePicked(Uri uri) {
        this.mCustomRingtone = EditorUiUtils.getRingtoneStringFromUri(uri, CURRENT_API_VERSION);
        startService(ContactSaveService.createSetRingtone(this, this.mLookupUri, this.mCustomRingtone));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.mHasAlreadyBeenOpened = true;
        this.mIsEntranceAnimationFinished = true;
        this.mHasComputedThemeColor = false;
        processIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mColorFilter != null) {
            bundle.putInt("theme_color", this.mColorFilterColor);
        }
        bundle.putLong("previous_contact_id", this.mPreviousContactId);
        bundle.putBoolean("sendToVoicemailState", this.mSendToVoicemailState);
        bundle.putBoolean("arePhoneOptionsChangable", this.mArePhoneOptionsChangable);
        bundle.putString(ContactSaveService.EXTRA_CUSTOM_RINGTONE, this.mCustomRingtone);
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }
        if ("splitCompleted".equals(intent.getAction())) {
            Toast.makeText(this, R.string.contactUnlinkedToast, 0).show();
            finish();
            return;
        }
        Uri data = intent.getData();
        Log.sensitive("QuickContact", "The original uri from intent: " + data);
        if (intent.getBooleanExtra("contact_edited", false)) {
            setResult(4);
        }
        if (data != null && "contacts".equals(data.getAuthority())) {
            data = ContactsContract.RawContacts.getContactLookupUri(getContentResolver(), ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, ContentUris.parseId(data)));
            Log.d("QuickContact", "The uri from old version: " + data);
        }
        this.mExtraMode = getIntent().getIntExtra("android.provider.extra.MODE", 3);
        if (isMultiWindowOnPhone()) {
            this.mExtraMode = 3;
        }
        this.mExtraPrioritizedMimeType = getIntent().getStringExtra("android.provider.extra.PRIORITIZED_MIMETYPE");
        Uri uri = this.mLookupUri;
        if (data == null) {
            Log.w("QuickContact", "[processIntent]lookupUri is null,return!");
            finish();
            return;
        }
        this.mLookupUri = data;
        this.mExcludeMimes = intent.getStringArrayExtra("android.provider.extra.EXCLUDE_MIMES");
        if (uri == null) {
            this.mShouldLog = !this.mIsRecreatedInstance;
            this.mContactLoader = (ContactLoader) getLoaderManager().initLoader(0, null, this.mLoaderContactCallbacks);
        } else if (uri != this.mLookupUri) {
            this.mShouldLog = true;
            destroyInteractionLoaders();
            this.mContactLoader = (ContactLoader) getLoaderManager().getLoader(0);
            this.mContactLoader.setNewLookup(this.mLookupUri);
            this.mCachedCp2DataCardModel = null;
        }
        this.mContactLoader.forceLoad();
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().processIntent(intent);
    }

    private void destroyInteractionLoaders() {
        for (int i : mRecentLoaderIds) {
            getLoaderManager().destroyLoader(i);
        }
    }

    private void runEntranceAnimation() {
        if (this.mHasAlreadyBeenOpened) {
            return;
        }
        this.mHasAlreadyBeenOpened = true;
        this.mScroller.scrollUpForEntranceAnimation((isMultiWindowOnPhone() || this.mExtraMode == 4) ? false : true);
    }

    private boolean isMultiWindowOnPhone() {
        return MultiWindowCompat.isInMultiWindowMode(this) && PhoneCapabilityTester.isPhone(this);
    }

    private void setHeaderNameText(int i) {
        if (this.mScroller != null) {
            this.mScroller.setTitle(getText(i) == null ? null : getText(i).toString(), false);
        }
    }

    private void setHeaderNameText(String str, boolean z) {
        if (!TextUtils.isEmpty(str) && this.mScroller != null) {
            this.mScroller.setTitle(str, z);
        }
    }

    private boolean isMimeExcluded(String str) {
        if (this.mExcludeMimes == null) {
            return false;
        }
        for (String str2 : this.mExcludeMimes) {
            if (TextUtils.equals(str2, str)) {
                return true;
            }
        }
        return false;
    }

    private void bindContactData(final Contact contact) {
        int i;
        Trace.beginSection("bindContactData");
        int i2 = this.mContactData == null ? 1 : 0;
        this.mContactData = contact;
        if (DirectoryContactUtil.isDirectoryContact(this.mContactData)) {
            i = 3;
        } else if (InvisibleContactUtil.isInvisibleAndAddable(this.mContactData, this)) {
            i = 2;
        } else {
            i = isContactEditable() ? 1 : 0;
        }
        if (this.mShouldLog && this.mContactType != i) {
            Logger.logQuickContactEvent(this.mReferrer, i, 0, i2, null);
        }
        this.mContactType = i;
        setStateForPhoneMenuItems(this.mContactData);
        invalidateOptionsMenu();
        Trace.endSection();
        Trace.beginSection("Set display photo & name");
        this.mPhotoView.setIsBusiness(this.mContactData.isDisplayNameFromOrganization());
        this.mPhotoSetter.setupContactPhoto(contact, this.mPhotoView);
        extractAndApplyTintFromPhotoViewAsynchronously();
        String string = ContactDisplayUtils.getDisplayName(this, contact).toString();
        setHeaderNameText(string, this.mContactData.getDisplayNameSource() == 20);
        String phoneticName = ContactDisplayUtils.getPhoneticName(this, contact);
        if (this.mScroller != null) {
            if (!TextUtils.isEmpty(phoneticName) && !phoneticName.equals(string)) {
                this.mScroller.setPhoneticName(phoneticName);
            } else {
                this.mScroller.setPhoneticNameGone();
            }
        }
        Trace.endSection();
        this.mEntriesAndActionsTask = new AsyncTask<Void, Void, Cp2DataCardModel>() {
            @Override
            protected Cp2DataCardModel doInBackground(Void... voidArr) {
                Log.d("QuickContact", "[Cp2DataCardModel] doInBackground(). " + QuickContactActivity.this.mEntriesAndActionsTask);
                return QuickContactActivity.this.generateDataModelFromContact(contact);
            }

            @Override
            protected void onPostExecute(Cp2DataCardModel cp2DataCardModel) {
                super.onPostExecute(cp2DataCardModel);
                Log.d("QuickContact", "[Cp2DataCardModel] onPostExecute(). " + QuickContactActivity.this.mEntriesAndActionsTask);
                if (contact == QuickContactActivity.this.mContactData && !isCancelled()) {
                    QuickContactActivity.this.bindDataToCards(cp2DataCardModel);
                    QuickContactActivity.this.showActivity();
                } else {
                    Log.e("QuickContact", "[Cp2DataCardModel] Async task cancelled !!! isCancelled():" + isCancelled() + ", data:" + contact + ", mContactData:" + QuickContactActivity.this.mContactData);
                }
                QuickContactActivity.this.mEntriesAndActionsTask = null;
            }
        };
        this.mEntriesAndActionsTask.execute(new Void[0]);
        NfcHandler.register(this, this.mContactData.getLookupUri());
        Log.d("QuickContact", "[bindContactData]mEntriesAndActionsTask.execute()." + this.mEntriesAndActionsTask);
    }

    private void bindDataToCards(Cp2DataCardModel cp2DataCardModel) {
        startInteractionLoaders(cp2DataCardModel);
        populateContactAndAboutCard(cp2DataCardModel, true);
    }

    private void startInteractionLoaders(Cp2DataCardModel cp2DataCardModel) {
        String[] strArr;
        String[] strArr2;
        Map<String, List<DataItem>> map = cp2DataCardModel.dataItemsMap;
        List<DataItem> list = map.get("vnd.android.cursor.item/phone_v2");
        List<DataItem> list2 = map.get("vnd.android.cursor.item/sip_address");
        if (list != null && list.size() == 1) {
            this.mOnlyOnePhoneNumber = true;
        } else {
            this.mOnlyOnePhoneNumber = false;
        }
        String[] strArr3 = null;
        if (list != null) {
            strArr = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                strArr[i] = ((PhoneDataItem) list.get(i)).getNumber();
            }
        } else {
            strArr = null;
        }
        if (list2 != null) {
            strArr2 = new String[list2.size()];
            for (int i2 = 0; i2 < list2.size(); i2++) {
                strArr2[i2] = ((SipAddressDataItem) list2.get(i2)).getSipAddress();
            }
        } else {
            strArr2 = null;
        }
        Bundle bundle = new Bundle();
        bundle.putStringArray(KEY_LOADER_EXTRA_PHONES, strArr);
        bundle.putStringArray(KEY_LOADER_EXTRA_SIP_NUMBERS, strArr2);
        Trace.beginSection("start sms loader");
        getLoaderManager().initLoader(1, bundle, this.mLoaderInteractionsCallbacks);
        Trace.endSection();
        Trace.beginSection("start call log loader");
        getLoaderManager().initLoader(3, bundle, this.mLoaderInteractionsCallbacks);
        Trace.endSection();
        Trace.beginSection("start calendar loader");
        List<DataItem> list3 = map.get("vnd.android.cursor.item/email_v2");
        if (list3 != null && list3.size() == 1) {
            this.mOnlyOneEmail = true;
        } else {
            this.mOnlyOneEmail = false;
        }
        if (list3 != null) {
            strArr3 = new String[list3.size()];
            for (int i3 = 0; i3 < list3.size(); i3++) {
                strArr3[i3] = ((EmailDataItem) list3.get(i3)).getAddress();
            }
        }
        Bundle bundle2 = new Bundle();
        bundle2.putStringArray(KEY_LOADER_EXTRA_EMAILS, strArr3);
        getLoaderManager().initLoader(2, bundle2, this.mLoaderInteractionsCallbacks);
        Trace.endSection();
    }

    private void showActivity() {
        if (this.mScroller != null) {
            this.mScroller.setVisibility(0);
            SchedulingUtils.doOnPreDraw(this.mScroller, false, new Runnable() {
                @Override
                public void run() {
                    QuickContactActivity.this.runEntranceAnimation();
                }
            });
        }
    }

    private List<List<ExpandingEntryCardView.Entry>> buildAboutCardEntries(Map<String, List<DataItem>> map) {
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = SORTED_ABOUT_CARD_MIMETYPES.iterator();
        while (it.hasNext()) {
            List<DataItem> list = map.get(it.next());
            if (list != null) {
                List<ExpandingEntryCardView.Entry> listDataItemsToEntries = dataItemsToEntries(list, null);
                if (listDataItemsToEntries.size() > 0) {
                    arrayList.add(listDataItemsToEntries);
                }
            }
        }
        return arrayList;
    }

    @Override
    protected void onResume() {
        Log.w("QuickContact", "[onResume] beg: mHasIntentLaunched = " + this.mHasIntentLaunched + ", mCachedCp2DataCardModel = " + this.mCachedCp2DataCardModel);
        super.onResume();
        this.mIsResumed = true;
        if (this.mHasIntentLaunched) {
            this.mHasIntentLaunched = false;
            populateContactAndAboutCard(this.mCachedCp2DataCardModel, false);
        }
        if (this.mCachedCp2DataCardModel != null) {
            destroyInteractionLoaders();
            startInteractionLoaders(this.mCachedCp2DataCardModel);
        }
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().getQuickContactRcsScroller().updateRcsContact(this.mContactLoader.getLookupUri(), false);
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().onHostActivityResumed(this);
        maybeShowProgressDialog();
        Log.w("QuickContact", "[onResume] end");
    }

    @Override
    protected void onPause() {
        Log.w("QuickContact", "[onPause]");
        super.onPause();
        dismissProgressBar();
        this.mIsResumed = false;
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().onHostActivityPaused();
    }

    private void populateContactAndAboutCard(Cp2DataCardModel cp2DataCardModel, boolean z) {
        this.mCachedCp2DataCardModel = cp2DataCardModel;
        if (this.mHasIntentLaunched || cp2DataCardModel == null) {
            return;
        }
        Trace.beginSection("bind contact card");
        List<List<ExpandingEntryCardView.Entry>> list = cp2DataCardModel.contactCardEntries;
        List<List<ExpandingEntryCardView.Entry>> list2 = cp2DataCardModel.aboutCardEntries;
        String str = cp2DataCardModel.customAboutCardName;
        if (list.size() > 0) {
            this.mContactCard.initialize(list, 3, this.mContactCard.isExpanded(), true, this.mExpandingEntryCardViewListener, this.mScroller);
            if (this.mContactCard.getVisibility() == 8 && this.mShouldLog) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 2, 0, null);
            }
            this.mContactCard.setVisibility(0);
        } else {
            this.mContactCard.setVisibility(8);
        }
        Trace.endSection();
        Trace.beginSection("bind about card");
        String phoneticName = this.mContactData.getPhoneticName();
        if (z && !TextUtils.isEmpty(phoneticName)) {
            ExpandingEntryCardView.Entry entry = new ExpandingEntryCardView.Entry(-1, null, getResources().getString(R.string.name_phonetic), phoneticName, null, null, null, null, null, null, null, null, false, false, new ExpandingEntryCardView.EntryContextMenuInfo(phoneticName, getResources().getString(R.string.name_phonetic), null, -1L, false), null, null, null, 1, null, true, 0);
            ArrayList arrayList = new ArrayList();
            arrayList.add(entry);
            if (list2.size() > 0 && list2.get(0).get(0).getHeader().equals(getResources().getString(R.string.header_nickname_entry))) {
                list2.add(1, arrayList);
            } else {
                list2.add(0, arrayList);
            }
        }
        this.mAboutCard.setTitle(str);
        this.mAboutCard.initialize(list2, 1, true, true, this.mExpandingEntryCardViewListener, this.mScroller);
        if (list.size() == 0 && list2.size() == 0) {
            initializeNoContactDetailCard(cp2DataCardModel.areAllRawContactsSimAccounts);
        } else {
            this.mNoContactDetailsCard.setVisibility(8);
        }
        if (list2.size() > 0) {
            if (this.mAboutCard.getVisibility() == 8 && this.mShouldLog) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 4, 0, null);
            }
            if (isAllRecentDataLoaded()) {
                this.mAboutCard.setVisibility(0);
            }
        } else {
            this.mAboutCard.setVisibility(8);
        }
        Trace.endSection();
    }

    private void initializeNoContactDetailCard(boolean z) {
        Log.d("QuickContact", "[initializeNoContactDetailCard]areAllRawContactsSimAccounts is" + z);
        ExpandingEntryCardView.Entry entry = new ExpandingEntryCardView.Entry(-2, ResourcesCompat.getDrawable(getResources(), R.drawable.quantum_ic_phone_vd_theme_24, null).mutate(), getString(R.string.quickcontact_add_phone_number), null, null, null, null, null, getEditContactIntent(), null, null, null, true, false, null, null, null, null, 1, null, true, R.drawable.quantum_ic_phone_vd_theme_24);
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ArrayList(1));
        ((List) arrayList.get(0)).add(entry);
        if (QuickContactUtils.isSupportShowEmailData(this.mContactData, getApplicationContext())) {
            ExpandingEntryCardView.Entry entry2 = new ExpandingEntryCardView.Entry(-2, ResourcesCompat.getDrawable(getResources(), R.drawable.quantum_ic_email_vd_theme_24, null).mutate(), getString(R.string.quickcontact_add_email), null, null, null, null, null, getEditContactIntent(), null, null, null, true, false, null, null, null, null, 1, null, true, R.drawable.quantum_ic_email_vd_theme_24);
            arrayList.add(new ArrayList(1));
            ((List) arrayList.get(1)).add(entry2);
        }
        int color = getResources().getColor(R.color.quickcontact_entry_sub_header_text_color);
        PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        this.mNoContactDetailsCard.initialize(arrayList, arrayList.size(), true, true, this.mExpandingEntryCardViewListener, this.mScroller);
        if (this.mNoContactDetailsCard.getVisibility() == 8 && this.mShouldLog) {
            Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 1, 0, null);
        }
        this.mNoContactDetailsCard.setVisibility(0);
        this.mNoContactDetailsCard.setEntryHeaderColor(color);
        this.mNoContactDetailsCard.setColorAndFilter(color, porterDuffColorFilter);
    }

    private Cp2DataCardModel generateDataModelFromContact(Contact contact) {
        List<DataItem> arrayList;
        Trace.beginSection("Build data items map");
        Log.sensitive("QuickContact", "[generateDataModelFromContact] start contact: " + contact);
        HashMap map = new HashMap();
        boolean zIsTachyonEnabled = CallUtil.isTachyonEnabled(this);
        UnmodifiableIterator<RawContact> it = contact.getRawContacts().iterator();
        while (it.hasNext()) {
            RawContact next = it.next();
            for (DataItem dataItem : next.getDataItems()) {
                dataItem.setRawContactId(next.getId().longValue());
                String mimeType = dataItem.getMimeType();
                if (mimeType != null) {
                    if (!"vnd.android.cursor.item/com.google.android.apps.tachyon.phone".equals(mimeType)) {
                        DataKind kindOrFallback = AccountTypeManager.getInstance(this).getKindOrFallback(next.getAccountType(this), mimeType);
                        if (kindOrFallback != null) {
                            dataItem.setDataKind(kindOrFallback);
                            boolean z = !TextUtils.isEmpty(dataItem.buildDataString(this, kindOrFallback));
                            if (!isMimeExcluded(mimeType) && z) {
                                arrayList = map.get(mimeType);
                                if (arrayList == null) {
                                    arrayList = new ArrayList<>();
                                    map.put(mimeType, arrayList);
                                }
                                arrayList.add(dataItem);
                            }
                        }
                    } else if (zIsTachyonEnabled) {
                        arrayList = map.get(mimeType);
                        if (arrayList == null) {
                        }
                        arrayList.add(dataItem);
                    }
                }
            }
        }
        Trace.endSection();
        bindReachability(map);
        Trace.beginSection("sort within mimetypes");
        ArrayList arrayList2 = new ArrayList();
        for (List<DataItem> list : map.values()) {
            Collapser.collapseList(list, this);
            Collections.sort(list, this.mWithinMimeTypeDataItemComparator);
            arrayList2.add(list);
        }
        Trace.endSection();
        Trace.beginSection("sort amongst mimetypes");
        Collections.sort(arrayList2, this.mAmongstMimeTypeDataItemComparator);
        Trace.endSection();
        Trace.beginSection("cp2 data items to entries");
        ArrayList arrayList3 = new ArrayList();
        List<List<ExpandingEntryCardView.Entry>> listBuildAboutCardEntries = buildAboutCardEntries(map);
        MutableString mutableString = new MutableString();
        for (int i = 0; i < arrayList2.size(); i++) {
            if (!SORTED_ABOUT_CARD_MIMETYPES.contains(((DataItem) ((List) arrayList2.get(i)).get(0)).getMimeType())) {
                List<ExpandingEntryCardView.Entry> listDataItemsToEntries = dataItemsToEntries((List) arrayList2.get(i), mutableString);
                if (listDataItemsToEntries.size() > 0) {
                    arrayList3.add(listDataItemsToEntries);
                }
            }
        }
        Trace.endSection();
        Cp2DataCardModel cp2DataCardModel = new Cp2DataCardModel();
        cp2DataCardModel.customAboutCardName = mutableString.value;
        cp2DataCardModel.aboutCardEntries = listBuildAboutCardEntries;
        cp2DataCardModel.contactCardEntries = arrayList3;
        cp2DataCardModel.dataItemsMap = map;
        cp2DataCardModel.areAllRawContactsSimAccounts = contact.areAllRawContactsSimAccounts(this);
        Log.sensitive("QuickContact", "[generateDataModelFromContact] end contact: " + contact + ", areAllRawContactsSimAccounts : " + cp2DataCardModel.areAllRawContactsSimAccounts);
        return cp2DataCardModel;
    }

    private void bindReachability(Map<String, List<DataItem>> map) {
        List<DataItem> list = map.get("vnd.android.cursor.item/phone_v2");
        List<DataItem> list2 = map.get("vnd.android.cursor.item/com.google.android.apps.tachyon.phone");
        if (list != null && list2 != null) {
            for (DataItem dataItem : list) {
                if (dataItem instanceof PhoneDataItem) {
                    PhoneDataItem phoneDataItem = (PhoneDataItem) dataItem;
                    if (phoneDataItem.getNumber() != null) {
                        for (DataItem dataItem2 : list2) {
                            if (phoneDataItem.getNumber().equals(dataItem2.getContentValues().getAsString("data1"))) {
                                phoneDataItem.setTachyonReachable(true);
                                phoneDataItem.setReachableDataItem(dataItem2);
                            }
                        }
                    }
                }
            }
        }
    }

    private static class Cp2DataCardModel {
        public List<List<ExpandingEntryCardView.Entry>> aboutCardEntries;
        public boolean areAllRawContactsSimAccounts;
        public List<List<ExpandingEntryCardView.Entry>> contactCardEntries;
        public String customAboutCardName;
        public Map<String, List<DataItem>> dataItemsMap;

        private Cp2DataCardModel() {
        }
    }

    private static class MutableString {
        public String value;

        private MutableString() {
        }
    }

    private static ExpandingEntryCardView.Entry dataItemToEntry(DataItem dataItem, DataItem dataItem2, Context context, Contact contact, MutableString mutableString) {
        StringBuilder sb;
        StringBuilder sb2;
        String string;
        Intent intent;
        Intent intent2;
        Drawable icon;
        String string2;
        boolean z;
        int i;
        int i2;
        String groupTitle;
        Drawable drawable;
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo;
        boolean z2;
        boolean z3;
        String str;
        Drawable drawable2;
        ?? r29;
        ?? r30;
        String str2;
        ?? r33;
        Object obj;
        Object obj2;
        ?? drawable3;
        boolean z4;
        Drawable drawable4;
        String string3;
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo2;
        String string4;
        Intent viewPostalAddressDirectionsIntent;
        Drawable drawable5;
        Drawable drawable6;
        String formattedAddress;
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo3;
        Drawable drawable7;
        int i3;
        Drawable drawable8;
        Intent intent3;
        String asString;
        Bundle bundle;
        int i4;
        String str3;
        String str4;
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo4;
        Spannable spannable;
        Intent intent4;
        String str5;
        Drawable drawable9;
        Spannable spannable2;
        Drawable drawable10;
        Intent intent5;
        String subheaderString;
        String str6;
        String str7;
        Intent callIntent;
        Spannable spannable3;
        Intent intent6;
        Drawable drawable11;
        boolean z5;
        Intent data;
        Intent intent7;
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo5;
        String str8;
        String string5;
        String name;
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo6;
        Intent intent8;
        Intent intent9;
        String string6;
        String data2;
        String data3;
        Log.sensitive("QuickContact", "[dataItemToEntry] contact:" + contact + " dataItem:" + dataItem.getClass());
        if (contact == null) {
            return null;
        }
        StringBuilder sb3 = new StringBuilder();
        StringBuilder sb4 = new StringBuilder();
        Context applicationContext = context.getApplicationContext();
        Resources resources = applicationContext.getResources();
        DataKind dataKind = dataItem.getDataKind();
        if (contact == null) {
            Log.w("QuickContact", "[dataItemToEntry] contact data is null.");
            return null;
        }
        int i5 = 0;
        if (dataItem instanceof ImDataItem) {
            ImDataItem imDataItem = (ImDataItem) dataItem;
            Intent intent10 = (Intent) ContactsUtils.buildImIntent(applicationContext, imDataItem).first;
            int iIntValue = !imDataItem.isProtocolValid() ? -1 : imDataItem.isCreatedFromEmail() ? 5 : imDataItem.getProtocol().intValue();
            if (iIntValue == -1) {
                String string7 = resources.getString(R.string.header_im_entry);
                data2 = ContactsContract.CommonDataKinds.Im.getProtocolLabel(resources, iIntValue, imDataItem.getCustomProtocol()).toString();
                string6 = string7;
                data3 = imDataItem.getData();
            } else {
                string6 = ContactsContract.CommonDataKinds.Im.getProtocolLabel(resources, iIntValue, imDataItem.getCustomProtocol()).toString();
                data2 = imDataItem.getData();
                data3 = null;
            }
            ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo7 = new ExpandingEntryCardView.EntryContextMenuInfo(imDataItem.getData(), string6, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            intent = intent10;
            string2 = string6;
            groupTitle = data2;
            string = data3;
            entryContextMenuInfo = entryContextMenuInfo7;
            str8 = null;
        } else {
            if (!(dataItem instanceof OrganizationDataItem)) {
                if (dataItem instanceof NicknameDataItem) {
                    NicknameDataItem nicknameDataItem = (NicknameDataItem) dataItem;
                    if (((contact.getNameRawContactId() > dataItem.getRawContactId().longValue() ? 1 : (contact.getNameRawContactId() == dataItem.getRawContactId().longValue() ? 0 : -1)) == 0) && contact.getDisplayNameSource() == 35) {
                        string5 = null;
                        name = null;
                        entryContextMenuInfo6 = null;
                    } else {
                        string5 = resources.getString(R.string.header_nickname_entry);
                        name = nicknameDataItem.getName();
                        entryContextMenuInfo6 = new ExpandingEntryCardView.EntryContextMenuInfo(name, string5, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                    }
                    entryContextMenuInfo = entryContextMenuInfo6;
                    intent = null;
                    string = null;
                    drawable2 = null;
                    r29 = 0;
                    r30 = 0;
                    str2 = null;
                    r33 = 0;
                    obj = null;
                    obj2 = null;
                    i = 0;
                    intent2 = null;
                    z = true;
                    i2 = 1;
                    drawable3 = 0;
                    sb2 = sb3;
                    String str9 = name;
                    string2 = string5;
                    groupTitle = str9;
                } else {
                    if (dataItem instanceof CustomDataItem) {
                        CustomDataItem customDataItem = (CustomDataItem) dataItem;
                        string2 = customDataItem.getSummary();
                        if (TextUtils.isEmpty(string2)) {
                            string2 = resources.getString(R.string.label_custom_field);
                        }
                        groupTitle = customDataItem.getContent();
                        entryContextMenuInfo5 = new ExpandingEntryCardView.EntryContextMenuInfo(groupTitle, string2, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                    } else if (dataItem instanceof NoteDataItem) {
                        string2 = resources.getString(R.string.header_note_entry);
                        groupTitle = ((NoteDataItem) dataItem).getNote();
                        entryContextMenuInfo5 = new ExpandingEntryCardView.EntryContextMenuInfo(groupTitle, string2, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                    } else if (dataItem instanceof WebsiteDataItem) {
                        WebsiteDataItem websiteDataItem = (WebsiteDataItem) dataItem;
                        String string8 = resources.getString(R.string.header_website_entry);
                        String url = websiteDataItem.getUrl();
                        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo8 = new ExpandingEntryCardView.EntryContextMenuInfo(url, string8, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                        try {
                            intent7 = new Intent("android.intent.action.VIEW", Uri.parse(new WebAddress(websiteDataItem.buildDataStringForDisplay(applicationContext, dataKind)).toString()));
                        } catch (WebAddress.ParseException e) {
                            Log.e("QuickContact", "Couldn't parse website: " + websiteDataItem.buildDataStringForDisplay(applicationContext, dataKind));
                            intent7 = null;
                        }
                        string2 = string8;
                        groupTitle = url;
                        entryContextMenuInfo = entryContextMenuInfo8;
                        intent = intent7;
                        string = null;
                        str8 = string;
                    } else {
                        if (dataItem instanceof EventDataItem) {
                            EventDataItem eventDataItem = (EventDataItem) dataItem;
                            String strBuildDataStringForDisplay = eventDataItem.buildDataStringForDisplay(applicationContext, dataKind);
                            Calendar date = DateUtils.parseDate(strBuildDataStringForDisplay, false);
                            if (date != null) {
                                Date nextAnnualDate = DateUtils.getNextAnnualDate(date);
                                Uri.Builder builderBuildUpon = CalendarContract.CONTENT_URI.buildUpon();
                                builderBuildUpon.appendPath("time");
                                sb = sb3;
                                ContentUris.appendId(builderBuildUpon, nextAnnualDate.getTime());
                                data = new Intent("android.intent.action.VIEW").setData(builderBuildUpon.build());
                            } else {
                                sb = sb3;
                                data = null;
                            }
                            String string9 = resources.getString(R.string.header_event_entry);
                            groupTitle = eventDataItem.hasKindTypeColumn(dataKind) ? EventCompat.getTypeLabel(resources, eventDataItem.getKindTypeColumn(dataKind), eventDataItem.getLabel()).toString() : null;
                            String date2 = DateUtils.formatDate(applicationContext, strBuildDataStringForDisplay);
                            string = date2;
                            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(date2, string9, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                            string2 = string9;
                        } else {
                            sb = sb3;
                            if (dataItem instanceof RelationDataItem) {
                                RelationDataItem relationDataItem = (RelationDataItem) dataItem;
                                String strBuildDataStringForDisplay2 = relationDataItem.buildDataStringForDisplay(applicationContext, dataKind);
                                if (TextUtils.isEmpty(strBuildDataStringForDisplay2)) {
                                    data = null;
                                } else {
                                    Intent intent11 = new Intent("android.intent.action.SEARCH");
                                    intent11.putExtra("query", strBuildDataStringForDisplay2);
                                    intent11.setType("vnd.android.cursor.dir/contact");
                                    data = intent11;
                                }
                                string2 = resources.getString(R.string.header_relation_entry);
                                String name2 = relationDataItem.getName();
                                ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo9 = new ExpandingEntryCardView.EntryContextMenuInfo(name2, string2, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                                string = relationDataItem.hasKindTypeColumn(dataKind) ? ContactsContract.CommonDataKinds.Relation.getTypeLabel(resources, relationDataItem.getKindTypeColumn(dataKind), relationDataItem.getLabel()).toString() : null;
                                groupTitle = name2;
                                entryContextMenuInfo = entryContextMenuInfo9;
                            } else if (dataItem instanceof PhoneDataItem) {
                                PhoneDataItem phoneDataItem = (PhoneDataItem) dataItem;
                                if (TextUtils.isEmpty(phoneDataItem.getNumber())) {
                                    sb2 = sb;
                                    i3 = 0;
                                    drawable8 = null;
                                    intent3 = null;
                                    asString = null;
                                    bundle = null;
                                    i4 = 1;
                                    str3 = null;
                                    str4 = null;
                                    entryContextMenuInfo4 = null;
                                    spannable = null;
                                    intent4 = null;
                                    str5 = null;
                                    drawable9 = null;
                                    spannable2 = null;
                                    drawable10 = null;
                                    intent5 = null;
                                } else {
                                    sb2 = sb;
                                    sb2.append(resources.getString(R.string.call_other));
                                    sb2.append(" ");
                                    String strUnicodeWrap = sBidiFormatter.unicodeWrap(phoneDataItem.buildDataStringForDisplay(applicationContext, dataKind), TextDirectionHeuristics.LTR);
                                    ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo10 = new ExpandingEntryCardView.EntryContextMenuInfo(strUnicodeWrap, resources.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                                    if (phoneDataItem.hasKindTypeColumn(dataKind)) {
                                        int kindTypeColumn = phoneDataItem.getKindTypeColumn(dataKind);
                                        String label = phoneDataItem.getLabel();
                                        if (kindTypeColumn == 0 && TextUtils.isEmpty(label)) {
                                            subheaderString = null;
                                            str7 = "";
                                            str6 = label;
                                        } else {
                                            String string10 = ContactsContract.CommonDataKinds.Phone.getTypeLabel(resources, kindTypeColumn, label).toString();
                                            int indicate = contact.getIndicate();
                                            subheaderString = GlobalEnv.getSimAasEditor().getSubheaderString(indicate, dataItem.getContentValues().getAsInteger("data2").intValue());
                                            str6 = (String) GlobalEnv.getSimAasEditor().getTypeLabel(dataItem.getContentValues().getAsInteger("data2").intValue(), dataItem.getContentValues().getAsString("data3"), string10, indicate);
                                            sb2.append(str6);
                                            sb2.append(" ");
                                            str7 = str6;
                                        }
                                    } else {
                                        subheaderString = null;
                                        str6 = null;
                                        str7 = null;
                                    }
                                    sb2.append(strUnicodeWrap);
                                    Spannable telephoneTtsSpannable = com.android.contacts.util.ContactDisplayUtils.getTelephoneTtsSpannable(sb2.toString(), strUnicodeWrap);
                                    Drawable drawable12 = resources.getDrawable(R.drawable.quantum_ic_phone_vd_theme_24);
                                    if (PhoneCapabilityTester.isPhone(applicationContext)) {
                                        callIntent = CallUtil.getCallIntent(phoneDataItem.getNumber());
                                        str3 = subheaderString;
                                        callIntent.putExtra("action_type", 10);
                                    } else {
                                        str3 = subheaderString;
                                        callIntent = null;
                                    }
                                    if (PhoneCapabilityTester.isSupportSms(applicationContext)) {
                                        str4 = str7;
                                        entryContextMenuInfo4 = entryContextMenuInfo10;
                                        spannable3 = telephoneTtsSpannable;
                                        intent6 = new Intent("android.intent.action.SENDTO", Uri.fromParts(ContactsUtils.SCHEME_SMSTO, phoneDataItem.getNumber(), null));
                                        intent6.putExtra("action_type", 11);
                                        drawable11 = resources.getDrawable(R.drawable.quantum_ic_message_vd_theme_24);
                                        sb4.append(resources.getString(R.string.sms_custom, strUnicodeWrap));
                                    } else {
                                        str4 = str7;
                                        entryContextMenuInfo4 = entryContextMenuInfo10;
                                        spannable3 = telephoneTtsSpannable;
                                        intent6 = null;
                                        drawable11 = null;
                                    }
                                    Spannable telephoneTtsSpannable2 = com.android.contacts.util.ContactDisplayUtils.getTelephoneTtsSpannable(sb4.toString(), strUnicodeWrap);
                                    int videoCallingAvailability = CallUtil.getVideoCallingAvailability(applicationContext);
                                    boolean z6 = (videoCallingAvailability & 2) != 0;
                                    if ((videoCallingAvailability & 1) != 0) {
                                        intent4 = intent6;
                                        z5 = true;
                                    } else {
                                        intent4 = intent6;
                                        z5 = false;
                                    }
                                    ExtensionManager.getInstance();
                                    str5 = strUnicodeWrap;
                                    drawable9 = drawable11;
                                    spannable2 = telephoneTtsSpannable2;
                                    drawable10 = drawable12;
                                    boolean zIsVideoButtonEnabled = ExtensionManager.getOp01Extension().isVideoButtonEnabled(z5, contact.getLookupUri(), applicationContext);
                                    int carrierPresence = dataItem.getCarrierPresence();
                                    boolean z7 = (carrierPresence & 1) != 0;
                                    StringBuilder sb5 = new StringBuilder();
                                    intent5 = callIntent;
                                    sb5.append("[dataItemToEntry] videoCapability = ");
                                    sb5.append(videoCallingAvailability);
                                    sb5.append(", after Op01Extension, isVideoEnabled = ");
                                    sb5.append(zIsVideoButtonEnabled);
                                    sb5.append(", carrierPresence = ");
                                    sb5.append(carrierPresence);
                                    sb5.append(", isPresent = ");
                                    sb5.append(z7);
                                    sb5.append(", CommonPresenceExtension().isShowVideoIcon() = ");
                                    ExtensionManager.getInstance();
                                    sb5.append(ExtensionManager.getContactsCommonPresenceExtension().isShowVideoIcon());
                                    Log.d("QuickContact", sb5.toString());
                                    if (CallUtil.isCallWithSubjectSupported(applicationContext)) {
                                        Drawable drawable13 = resources.getDrawable(R.drawable.quantum_ic_perm_phone_msg_vd_theme_24);
                                        String string11 = resources.getString(R.string.call_with_a_note);
                                        bundle = new Bundle();
                                        bundle.putLong("PHOTO_ID", contact.getPhotoId());
                                        bundle.putParcelable("PHOTO_URI", UriUtils.parseUriOrNull(contact.getPhotoUri()));
                                        bundle.putParcelable("CONTACT_URI", contact.getLookupUri());
                                        bundle.putString("NAME_OR_NUMBER", contact.getDisplayName());
                                        bundle.putBoolean("IS_BUSINESS", false);
                                        bundle.putString("NUMBER", phoneDataItem.getNumber());
                                        bundle.putString("DISPLAY_NUMBER", phoneDataItem.getFormattedPhoneNumber());
                                        bundle.putString("NUMBER_LABEL", str6);
                                        asString = string11;
                                        drawable8 = drawable13;
                                        i4 = 3;
                                        intent3 = null;
                                    } else if (zIsVideoButtonEnabled && (!z6 || z7)) {
                                        drawable8 = resources.getDrawable(R.drawable.quantum_ic_videocam_vd_theme_24);
                                        Intent videoCallIntent = CallUtil.getVideoCallIntent(phoneDataItem.getNumber(), "com.android.contacts.quickcontact.QuickContactActivity");
                                        videoCallIntent.putExtra("action_type", 12);
                                        i4 = 2;
                                        bundle = null;
                                        asString = resources.getString(R.string.description_video_call);
                                        intent3 = videoCallIntent;
                                    } else if (CallUtil.isTachyonEnabled(applicationContext) && phoneDataItem.isTachyonReachable()) {
                                        drawable8 = resources.getDrawable(R.drawable.quantum_ic_videocam_vd_theme_24);
                                        intent3 = new Intent("com.google.android.apps.tachyon.action.CALL");
                                        intent3.setData(Uri.fromParts("tel", phoneDataItem.getNumber(), null));
                                        asString = phoneDataItem.getReachableDataItem().getContentValues().getAsString("data2");
                                        i4 = 2;
                                        bundle = null;
                                    } else {
                                        drawable8 = null;
                                        intent3 = null;
                                        asString = null;
                                        bundle = null;
                                        i4 = 1;
                                    }
                                    ExtensionManager.getInstance();
                                    ExtensionManager.getContactsCommonPresenceExtension().setVideoIconAlpha(phoneDataItem.getNumber(), drawable8, zIsVideoButtonEnabled);
                                    i3 = R.drawable.quantum_ic_phone_vd_theme_24;
                                    spannable = spannable3;
                                }
                                r29 = drawable8;
                                r30 = intent3;
                                str2 = asString;
                                r33 = bundle;
                                i2 = i4;
                                i = i3;
                                groupTitle = str3;
                                string = str4;
                                entryContextMenuInfo = entryContextMenuInfo4;
                                intent2 = intent4;
                                string2 = str5;
                                drawable2 = drawable9;
                                drawable3 = drawable10;
                                intent = intent5;
                                z = true;
                                obj = spannable;
                                obj2 = spannable2;
                            } else {
                                sb2 = sb;
                                if (dataItem instanceof EmailDataItem) {
                                    EmailDataItem emailDataItem = (EmailDataItem) dataItem;
                                    String data4 = emailDataItem.getData();
                                    if (TextUtils.isEmpty(data4)) {
                                        string3 = null;
                                        string2 = null;
                                        icon = null;
                                        intent = null;
                                        entryContextMenuInfo2 = null;
                                    } else {
                                        sb2.append(resources.getString(R.string.email_other));
                                        sb2.append(" ");
                                        intent = new Intent("android.intent.action.SENDTO", Uri.fromParts(ContactsUtils.SCHEME_MAILTO, data4, null));
                                        intent.putExtra("action_type", 13);
                                        string2 = emailDataItem.getAddress();
                                        entryContextMenuInfo2 = new ExpandingEntryCardView.EntryContextMenuInfo(string2, resources.getString(R.string.emailLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                                        if (emailDataItem.hasKindTypeColumn(dataKind)) {
                                            string3 = ContactsContract.CommonDataKinds.Email.getTypeLabel(resources, emailDataItem.getKindTypeColumn(dataKind), emailDataItem.getLabel()).toString();
                                            sb2.append(string3);
                                            sb2.append(" ");
                                        } else {
                                            string3 = null;
                                        }
                                        sb2.append(string2);
                                        i5 = R.drawable.quantum_ic_email_vd_theme_24;
                                        icon = resources.getDrawable(R.drawable.quantum_ic_email_vd_theme_24);
                                    }
                                } else if (dataItem instanceof StructuredPostalDataItem) {
                                    StructuredPostalDataItem structuredPostalDataItem = (StructuredPostalDataItem) dataItem;
                                    String formattedAddress2 = structuredPostalDataItem.getFormattedAddress();
                                    if (TextUtils.isEmpty(formattedAddress2)) {
                                        string4 = null;
                                        viewPostalAddressDirectionsIntent = null;
                                        drawable5 = null;
                                        intent = null;
                                        drawable6 = null;
                                        formattedAddress = null;
                                        entryContextMenuInfo3 = null;
                                    } else {
                                        sb2.append(resources.getString(R.string.map_other));
                                        sb2.append(" ");
                                        intent = StructuredPostalUtils.getViewPostalAddressIntent(formattedAddress2);
                                        intent.putExtra("action_type", 15);
                                        formattedAddress = structuredPostalDataItem.getFormattedAddress();
                                        entryContextMenuInfo3 = new ExpandingEntryCardView.EntryContextMenuInfo(formattedAddress, resources.getString(R.string.postalLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                                        if (structuredPostalDataItem.hasKindTypeColumn(dataKind)) {
                                            string4 = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(resources, structuredPostalDataItem.getKindTypeColumn(dataKind), structuredPostalDataItem.getLabel()).toString();
                                            sb2.append(string4);
                                            sb2.append(" ");
                                        } else {
                                            string4 = null;
                                        }
                                        sb2.append(formattedAddress);
                                        viewPostalAddressDirectionsIntent = StructuredPostalUtils.getViewPostalAddressDirectionsIntent(formattedAddress2);
                                        viewPostalAddressDirectionsIntent.putExtra("action_type", 16);
                                        drawable6 = resources.getDrawable(R.drawable.quantum_ic_directions_vd_theme_24);
                                        sb4.append(resources.getString(R.string.content_description_directions));
                                        sb4.append(" ");
                                        sb4.append(formattedAddress);
                                        i5 = R.drawable.quantum_ic_place_vd_theme_24;
                                        drawable5 = resources.getDrawable(R.drawable.quantum_ic_place_vd_theme_24);
                                    }
                                    string = string4;
                                    drawable2 = drawable6;
                                    entryContextMenuInfo = entryContextMenuInfo3;
                                    i = i5;
                                    groupTitle = null;
                                    z = true;
                                    r29 = 0;
                                    r30 = 0;
                                    str2 = null;
                                    i2 = 1;
                                    r33 = 0;
                                    obj = null;
                                    obj2 = null;
                                    intent2 = viewPostalAddressDirectionsIntent;
                                    drawable3 = drawable5;
                                    string2 = formattedAddress;
                                } else if (dataItem instanceof SipAddressDataItem) {
                                    SipAddressDataItem sipAddressDataItem = (SipAddressDataItem) dataItem;
                                    string2 = sipAddressDataItem.getSipAddress();
                                    if (TextUtils.isEmpty(string2)) {
                                        string3 = null;
                                        string2 = null;
                                        icon = null;
                                        intent = null;
                                        entryContextMenuInfo2 = null;
                                    } else {
                                        sb2.append(resources.getString(R.string.call_other));
                                        sb2.append(" ");
                                        if (PhoneCapabilityTester.isSipPhone(applicationContext)) {
                                            intent = CallUtil.getCallIntent(Uri.fromParts("sip", string2, null));
                                            intent.putExtra("action_type", 14);
                                        } else {
                                            intent = null;
                                        }
                                        entryContextMenuInfo2 = new ExpandingEntryCardView.EntryContextMenuInfo(string2, resources.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                                        if (sipAddressDataItem.hasKindTypeColumn(dataKind)) {
                                            string3 = ContactsContract.CommonDataKinds.SipAddress.getTypeLabel(resources, sipAddressDataItem.getKindTypeColumn(dataKind), sipAddressDataItem.getLabel()).toString();
                                            sb2.append(string3);
                                            sb2.append(" ");
                                        } else {
                                            string3 = null;
                                        }
                                        sb2.append(string2);
                                        i5 = R.drawable.quantum_ic_dialer_sip_vd_theme_24;
                                        icon = resources.getDrawable(R.drawable.quantum_ic_dialer_sip_vd_theme_24);
                                    }
                                } else {
                                    if (dataItem instanceof StructuredNameDataItem) {
                                        if (dataItem.isSuperPrimary() || mutableString.value == null || mutableString.value.isEmpty()) {
                                            String givenName = ((StructuredNameDataItem) dataItem).getGivenName();
                                            if (TextUtils.isEmpty(givenName)) {
                                                mutableString.value = resources.getString(R.string.about_card_title);
                                            } else {
                                                mutableString.value = resources.getString(R.string.about_card_title) + " " + givenName;
                                            }
                                        }
                                        i = 0;
                                        groupTitle = null;
                                        string2 = null;
                                    } else {
                                        if (CallUtil.isTachyonEnabled(applicationContext) && "vnd.android.cursor.item/com.google.android.apps.tachyon.phone".equals(dataItem.getMimeType())) {
                                            return null;
                                        }
                                        if (dataItem instanceof GroupMembershipDataItem) {
                                            groupTitle = QuickContactUtils.getGroupTitle(contact.getGroupMetaData(), ((GroupMembershipDataItem) dataItem).getGroupRowId().longValue());
                                            if (TextUtils.isEmpty(groupTitle)) {
                                                groupTitle = null;
                                                string2 = null;
                                            } else {
                                                string2 = resources.getString(R.string.groupsLabel);
                                            }
                                            i = 0;
                                        } else {
                                            String strBuildDataStringForDisplay3 = dataItem.buildDataStringForDisplay(applicationContext, dataKind);
                                            string = dataKind.typeColumn;
                                            intent = new Intent("android.intent.action.VIEW");
                                            intent.setDataAndType(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataItem.getId()), dataItem.getMimeType());
                                            intent.putExtra("action_type", 17);
                                            intent.putExtra("third_party_action", dataItem.getMimeType());
                                            String type = intent.getType();
                                            if (!"vnd.android.cursor.item/vnd.googleplus.profile.comm".equals(type)) {
                                                intent2 = null;
                                                icon = ResolveCache.getInstance(applicationContext).getIcon(dataItem.getMimeType(), intent);
                                                if (icon != null) {
                                                    icon.mutate();
                                                }
                                                if ("vnd.android.cursor.item/vnd.googleplus.profile".equals(type)) {
                                                    string2 = strBuildDataStringForDisplay3;
                                                    z = false;
                                                    i = 0;
                                                    i2 = 1;
                                                    groupTitle = null;
                                                    drawable = null;
                                                    entryContextMenuInfo = null;
                                                } else {
                                                    ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo11 = new ExpandingEntryCardView.EntryContextMenuInfo(strBuildDataStringForDisplay3, type, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                                                    string2 = strBuildDataStringForDisplay3;
                                                    entryContextMenuInfo = entryContextMenuInfo11;
                                                    z = false;
                                                    i = 0;
                                                    i2 = 1;
                                                    groupTitle = null;
                                                    drawable = null;
                                                }
                                                z2 = false;
                                                z3 = false;
                                                str = null;
                                                drawable7 = drawable;
                                                r33 = 0;
                                                obj = null;
                                                obj2 = null;
                                                drawable3 = icon;
                                                drawable2 = drawable7;
                                                r29 = z2;
                                                r30 = z3;
                                                str2 = str;
                                            } else if (dataItem2 != null) {
                                                Drawable drawable14 = resources.getDrawable(R.drawable.quantum_ic_hangout_vd_theme_24);
                                                Drawable drawable15 = resources.getDrawable(R.drawable.quantum_ic_hangout_video_vd_theme_24);
                                                HangoutsDataItemModel hangoutsDataItemModel = new HangoutsDataItemModel(intent, null, dataItem, dataItem2, sb4, strBuildDataStringForDisplay3, string, applicationContext);
                                                populateHangoutsDataItemModel(hangoutsDataItemModel);
                                                Intent intent12 = hangoutsDataItemModel.intent;
                                                Intent intent13 = hangoutsDataItemModel.alternateIntent;
                                                sb4 = hangoutsDataItemModel.alternateContentDescription;
                                                String str10 = hangoutsDataItemModel.header;
                                                string = hangoutsDataItemModel.text;
                                                intent2 = intent13;
                                                i = 0;
                                                z = true;
                                                i2 = 1;
                                                groupTitle = null;
                                                entryContextMenuInfo = null;
                                                r29 = 0;
                                                r30 = 0;
                                                str2 = null;
                                                r33 = 0;
                                                obj = null;
                                                obj2 = null;
                                                drawable3 = drawable14;
                                                drawable2 = drawable15;
                                                intent = intent12;
                                                string2 = str10;
                                            } else {
                                                intent2 = null;
                                                i = 0;
                                                z = true;
                                                i2 = 1;
                                                drawable2 = null;
                                                entryContextMenuInfo = null;
                                                r29 = 0;
                                                r30 = 0;
                                                str2 = null;
                                                r33 = 0;
                                                obj = null;
                                                obj2 = null;
                                                drawable3 = "hangout".equals(intent.getDataString()) ? resources.getDrawable(R.drawable.quantum_ic_hangout_video_vd_theme_24) : resources.getDrawable(R.drawable.quantum_ic_hangout_vd_theme_24);
                                                string2 = strBuildDataStringForDisplay3;
                                                groupTitle = null;
                                            }
                                        }
                                    }
                                    intent = null;
                                    intent2 = null;
                                    string = null;
                                    z4 = false;
                                    drawable4 = null;
                                    z = true;
                                    entryContextMenuInfo = null;
                                    r29 = 0;
                                    r30 = 0;
                                    str2 = null;
                                    i2 = 1;
                                    r33 = 0;
                                    obj = null;
                                    obj2 = null;
                                    drawable3 = z4;
                                    drawable2 = drawable4;
                                }
                                string = string3;
                                entryContextMenuInfo = entryContextMenuInfo2;
                                i = i5;
                                groupTitle = null;
                                intent2 = null;
                                drawable7 = null;
                                z = true;
                                z2 = false;
                                z3 = false;
                                str = null;
                                i2 = 1;
                                r33 = 0;
                                obj = null;
                                obj2 = null;
                                drawable3 = icon;
                                drawable2 = drawable7;
                                r29 = z2;
                                r30 = z3;
                                str2 = str;
                            }
                        }
                        intent = data;
                        i = 0;
                        sb2 = sb;
                        intent2 = null;
                        z4 = false;
                        drawable4 = null;
                        z = true;
                        r29 = 0;
                        r30 = 0;
                        str2 = null;
                        i2 = 1;
                        r33 = 0;
                        obj = null;
                        obj2 = null;
                        drawable3 = z4;
                        drawable2 = drawable4;
                    }
                    entryContextMenuInfo = entryContextMenuInfo5;
                    intent = null;
                    string = null;
                    str8 = string;
                }
                intent8 = (intent != null || PhoneCapabilityTester.isIntentRegistered(applicationContext, intent)) ? intent : null;
                if (intent2 == null) {
                    intent9 = intent2;
                } else if (PhoneCapabilityTester.isIntentRegistered(applicationContext, intent2)) {
                    if (TextUtils.isEmpty(sb4)) {
                        sb4.append(getIntentResolveLabel(intent2, applicationContext));
                    }
                    intent9 = intent2;
                } else {
                    intent9 = null;
                }
                if (drawable3 == 0 || !TextUtils.isEmpty(string2) || !TextUtils.isEmpty(groupTitle) || !TextUtils.isEmpty(string)) {
                    return new ExpandingEntryCardView.Entry(dataItem.getId() <= 2147483647L ? -1 : (int) dataItem.getId(), drawable3, string2, groupTitle, null, string, null, obj != null ? new SpannableString(sb2.toString()) : obj, intent8, drawable2, intent9, obj2 != null ? new SpannableString(sb4.toString()) : obj2, z, false, entryContextMenuInfo, r29, r30, str2, i2, r33, true, i);
                }
                Log.d("QuickContact", "[dataItemToEntry] has no visual elements");
                return null;
            }
            OrganizationDataItem organizationDataItem = (OrganizationDataItem) dataItem;
            string2 = resources.getString(R.string.header_organization_entry);
            String company = organizationDataItem.getCompany();
            ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo12 = new ExpandingEntryCardView.EntryContextMenuInfo(company, string2, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            string = organizationDataItem.getTitle();
            groupTitle = company;
            entryContextMenuInfo = entryContextMenuInfo12;
            intent = null;
            str8 = null;
        }
        String str11 = str8;
        String str12 = str11;
        String str13 = str12;
        String str14 = str13;
        String str15 = str14;
        String str16 = str15;
        i = 0;
        intent2 = null;
        z = true;
        i2 = 1;
        drawable3 = str16;
        sb2 = sb3;
        drawable2 = str8;
        r29 = str11;
        r30 = str12;
        str2 = str13;
        r33 = str14;
        obj = str15;
        obj2 = str16;
        if (intent != null) {
        }
        if (intent2 == null) {
        }
        if (drawable3 == 0) {
        }
        return new ExpandingEntryCardView.Entry(dataItem.getId() <= 2147483647L ? -1 : (int) dataItem.getId(), drawable3, string2, groupTitle, null, string, null, obj != null ? new SpannableString(sb2.toString()) : obj, intent8, drawable2, intent9, obj2 != null ? new SpannableString(sb4.toString()) : obj2, z, false, entryContextMenuInfo, r29, r30, str2, i2, r33, true, i);
    }

    private List<ExpandingEntryCardView.Entry> dataItemsToEntries(List<DataItem> list, MutableString mutableString) {
        Log.d("QuickContact", "[dataItemsToEntries]");
        if (list.get(0).getMimeType().equals("vnd.android.cursor.item/vnd.googleplus.profile")) {
            return gPlusDataItemsToEntries(list);
        }
        if (list.get(0).getMimeType().equals("vnd.android.cursor.item/vnd.googleplus.profile.comm")) {
            return hangoutsDataItemsToEntries(list);
        }
        ArrayList arrayList = new ArrayList();
        Iterator<DataItem> it = list.iterator();
        while (it.hasNext()) {
            ExpandingEntryCardView.Entry entryDataItemToEntry = dataItemToEntry(it.next(), null, this, this.mContactData, mutableString);
            if (entryDataItemToEntry != null) {
                arrayList.add(entryDataItemToEntry);
            }
        }
        return arrayList;
    }

    private Map<Long, List<DataItem>> dataItemsToBucket(List<DataItem> list) {
        HashMap map = new HashMap();
        for (DataItem dataItem : list) {
            List arrayList = (List) map.get(dataItem.getRawContactId());
            if (arrayList == null) {
                arrayList = new ArrayList();
                map.put(dataItem.getRawContactId(), arrayList);
            }
            arrayList.add(dataItem);
        }
        return map;
    }

    private List<ExpandingEntryCardView.Entry> gPlusDataItemsToEntries(List<DataItem> list) {
        ExpandingEntryCardView.Entry entryDataItemToEntry;
        ArrayList arrayList = new ArrayList();
        Iterator<List<DataItem>> it = dataItemsToBucket(list).values().iterator();
        while (it.hasNext()) {
            for (DataItem dataItem : it.next()) {
                if ("view".equals(dataItem.getContentValues().getAsString("data5")) && (entryDataItemToEntry = dataItemToEntry(dataItem, null, this, this.mContactData, null)) != null) {
                    arrayList.add(entryDataItemToEntry);
                }
            }
        }
        return arrayList;
    }

    private List<ExpandingEntryCardView.Entry> hangoutsDataItemsToEntries(List<DataItem> list) {
        ArrayList arrayList = new ArrayList();
        for (List<DataItem> list2 : dataItemsToBucket(list).values()) {
            if (list2.size() == 2) {
                ExpandingEntryCardView.Entry entryDataItemToEntry = dataItemToEntry(list2.get(0), list2.get(1), this, this.mContactData, null);
                if (entryDataItemToEntry != null) {
                    arrayList.add(entryDataItemToEntry);
                }
            } else {
                Iterator<DataItem> it = list2.iterator();
                while (it.hasNext()) {
                    ExpandingEntryCardView.Entry entryDataItemToEntry2 = dataItemToEntry(it.next(), null, this, this.mContactData, null);
                    if (entryDataItemToEntry2 != null) {
                        arrayList.add(entryDataItemToEntry2);
                    }
                }
            }
        }
        return arrayList;
    }

    private static final class HangoutsDataItemModel {
        public StringBuilder alternateContentDescription;
        public Intent alternateIntent;
        public Context context;
        public DataItem dataItem;
        public String header;
        public Intent intent;
        public DataItem secondDataItem;
        public String text;

        public HangoutsDataItemModel(Intent intent, Intent intent2, DataItem dataItem, DataItem dataItem2, StringBuilder sb, String str, String str2, Context context) {
            this.intent = intent;
            this.alternateIntent = intent2;
            this.dataItem = dataItem;
            this.secondDataItem = dataItem2;
            this.alternateContentDescription = sb;
            this.header = str;
            this.text = str2;
            this.context = context;
        }
    }

    private static void populateHangoutsDataItemModel(HangoutsDataItemModel hangoutsDataItemModel) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, hangoutsDataItemModel.secondDataItem.getId()), hangoutsDataItemModel.secondDataItem.getMimeType());
        intent.putExtra("action_type", 17);
        intent.putExtra("third_party_action", hangoutsDataItemModel.secondDataItem.getMimeType());
        if ("hangout".equals(hangoutsDataItemModel.dataItem.getContentValues().getAsString("data5"))) {
            hangoutsDataItemModel.alternateIntent = hangoutsDataItemModel.intent;
            hangoutsDataItemModel.alternateContentDescription = new StringBuilder(hangoutsDataItemModel.header);
            hangoutsDataItemModel.intent = intent;
            hangoutsDataItemModel.header = hangoutsDataItemModel.secondDataItem.buildDataStringForDisplay(hangoutsDataItemModel.context, hangoutsDataItemModel.secondDataItem.getDataKind());
            hangoutsDataItemModel.text = hangoutsDataItemModel.secondDataItem.getDataKind().typeColumn;
            return;
        }
        if ("conversation".equals(hangoutsDataItemModel.dataItem.getContentValues().getAsString("data5"))) {
            hangoutsDataItemModel.alternateIntent = intent;
            hangoutsDataItemModel.alternateContentDescription = new StringBuilder(hangoutsDataItemModel.secondDataItem.buildDataStringForDisplay(hangoutsDataItemModel.context, hangoutsDataItemModel.secondDataItem.getDataKind()));
        }
    }

    private static String getIntentResolveLabel(Intent intent, Context context) {
        ResolveInfo bestResolve;
        List<ResolveInfo> listQueryIntentActivities = context.getPackageManager().queryIntentActivities(intent, 65536);
        int size = listQueryIntentActivities.size();
        if (size == 1) {
            bestResolve = listQueryIntentActivities.get(0);
        } else if (size > 1) {
            bestResolve = ResolveCache.getInstance(context).getBestResolve(intent, listQueryIntentActivities);
        } else {
            bestResolve = null;
        }
        if (bestResolve == null) {
            return null;
        }
        return String.valueOf(bestResolve.loadLabel(context.getPackageManager()));
    }

    private void extractAndApplyTintFromPhotoViewAsynchronously() {
        if (this.mScroller == null) {
            Log.d("QuickContact", "[extractAndApplyTintFromPhotoViewAsynchronously] mScroller=null");
            return;
        }
        final Drawable drawable = this.mPhotoView.getDrawable();
        new AsyncTask<Void, Void, MaterialColorMapUtils.MaterialPalette>() {
            @Override
            protected MaterialColorMapUtils.MaterialPalette doInBackground(Void... voidArr) {
                Log.d("QuickContact", "[extractAndApplyTintFromPhotoViewAsynchronously] doInBackground()." + this);
                if ((drawable instanceof BitmapDrawable) && QuickContactActivity.this.mContactData != null && QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData() != null && QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData().length > 0) {
                    Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData(), 0, QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData().length);
                    try {
                        int iColorFromBitmap = QuickContactActivity.this.colorFromBitmap(bitmapDecodeByteArray);
                        if (iColorFromBitmap != 0) {
                            return QuickContactActivity.this.mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(iColorFromBitmap);
                        }
                    } finally {
                        bitmapDecodeByteArray.recycle();
                    }
                }
                if (drawable instanceof LetterTileDrawable) {
                    return QuickContactActivity.this.mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(((LetterTileDrawable) drawable).getColor());
                }
                return MaterialColorMapUtils.getDefaultPrimaryAndSecondaryColors(QuickContactActivity.this.getResources());
            }

            @Override
            protected void onPostExecute(MaterialColorMapUtils.MaterialPalette materialPalette) {
                super.onPostExecute(materialPalette);
                QuickContactActivity quickContactActivity = QuickContactActivity.this;
                ExtensionManager.getInstance();
                quickContactActivity.mHasComputedThemeColor = ExtensionManager.getRcsExtension().needUpdateContactPhoto(drawable instanceof LetterTileDrawable, QuickContactActivity.this.mHasComputedThemeColor);
                Log.d("QuickContact", "[extractAndApplyTintFromPhotoViewAsynchronously] onPostExecute()" + this + ", mHasComputedThemeColor = " + QuickContactActivity.this.mHasComputedThemeColor + ", mColorFilterColor = " + QuickContactActivity.this.mColorFilterColor + ", palette.mPrimaryColor = " + materialPalette.mPrimaryColor);
                if ((!QuickContactActivity.this.mHasComputedThemeColor || QuickContactActivity.this.mColorFilterColor != materialPalette.mPrimaryColor) && drawable == QuickContactActivity.this.mPhotoView.getDrawable()) {
                    QuickContactActivity.this.mHasComputedThemeColor = true;
                    QuickContactActivity.this.setThemeColor(materialPalette);
                }
            }
        }.execute(new Void[0]);
        Log.d("QuickContact", "[extractAndApplyTintFromPhotoViewAsynchronously] execute(). " + this);
    }

    private void setThemeColor(MaterialColorMapUtils.MaterialPalette materialPalette) {
        Log.d("QuickContact", "[setThemeColor]");
        this.mColorFilterColor = materialPalette.mPrimaryColor;
        this.mScroller.setHeaderTintColor(this.mColorFilterColor);
        this.mStatusBarColor = materialPalette.mSecondaryColor;
        updateStatusBarColor();
        this.mColorFilter = new PorterDuffColorFilter(this.mColorFilterColor, PorterDuff.Mode.SRC_ATOP);
        this.mContactCard.setColorAndFilter(this.mColorFilterColor, this.mColorFilter);
        this.mRecentCard.setColorAndFilter(this.mColorFilterColor, this.mColorFilter);
        this.mAboutCard.setColorAndFilter(this.mColorFilterColor, this.mColorFilter);
        if (this.mJoynCard != null) {
            this.mJoynCard.setColorAndFilter(this.mColorFilterColor, this.mColorFilter);
        }
    }

    private void updateStatusBarColor() {
        int i;
        if (this.mScroller == null || !CompatUtils.isLollipopCompatible()) {
            return;
        }
        if (this.mScroller.getScrollNeededToBeFullScreen() <= 0) {
            i = this.mStatusBarColor;
        } else {
            i = 0;
        }
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt(getWindow(), "statusBarColor", getWindow().getStatusBarColor(), i);
        objectAnimatorOfInt.setDuration(150L);
        objectAnimatorOfInt.setEvaluator(new ArgbEvaluator());
        objectAnimatorOfInt.start();
    }

    private int colorFromBitmap(Bitmap bitmap) {
        Palette paletteGenerate = Palette.generate(bitmap, 24);
        if (paletteGenerate != null && paletteGenerate.getVibrantSwatch() != null) {
            return paletteGenerate.getVibrantSwatch().getRgb();
        }
        return 0;
    }

    private List<ExpandingEntryCardView.Entry> contactInteractionsToEntries(List<ContactInteraction> list) {
        ArrayList arrayList = new ArrayList();
        for (ContactInteraction contactInteraction : list) {
            if (contactInteraction != null) {
                arrayList.add(new ExpandingEntryCardView.Entry(-1, contactInteraction.getIcon(this), contactInteraction.getViewHeader(this), contactInteraction.getViewBody(this), contactInteraction.getBodyIcon(this), contactInteraction.getViewFooter(this), contactInteraction.getFooterIcon(this), contactInteraction.getSimIcon(this), contactInteraction.getSimName(this), contactInteraction.getContentDescription(this), contactInteraction.getIntent(), null, null, null, true, false, null, null, null, null, 1, null, true, contactInteraction.getIconResourceId()));
            }
        }
        return arrayList;
    }

    @Override
    public void onBackPressed() {
        int intExtra = getIntent().getIntExtra("previous_screen_type", 0);
        if ((intExtra == 4 || intExtra == 3) && !SharedPreferenceUtil.getHamburgerPromoTriggerActionHappenedBefore(this)) {
            SharedPreferenceUtil.setHamburgerPromoTriggerActionHappenedBefore(this);
        }
        if (this.mScroller != null) {
            if (!this.mIsExitAnimationInProgress) {
                this.mScroller.scrollOffBottom();
                return;
            }
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private boolean isAllRecentDataLoaded() {
        return this.mRecentLoaderResults.size() == mRecentLoaderIds.length;
    }

    private void bindRecentData() {
        final ArrayList arrayList = new ArrayList();
        final ArrayList arrayList2 = new ArrayList();
        Iterator<List<ContactInteraction>> it = this.mRecentLoaderResults.values().iterator();
        while (it.hasNext()) {
            arrayList.addAll(it.next());
        }
        this.mRecentDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                Log.d("QuickContact", "[bindRecentData] doInBackground(). " + QuickContactActivity.this.mRecentDataTask);
                Trace.beginSection("sort recent loader results");
                Collections.sort(arrayList, new Comparator<ContactInteraction>() {
                    @Override
                    public int compare(ContactInteraction contactInteraction, ContactInteraction contactInteraction2) {
                        if (contactInteraction == null && contactInteraction2 == null) {
                            return 0;
                        }
                        if (contactInteraction == null) {
                            return 1;
                        }
                        if (contactInteraction2 == null || contactInteraction.getInteractionDate() > contactInteraction2.getInteractionDate()) {
                            return -1;
                        }
                        if (contactInteraction.getInteractionDate() == contactInteraction2.getInteractionDate()) {
                            return 0;
                        }
                        return 1;
                    }
                });
                Trace.endSection();
                Trace.beginSection("contactInteractionsToEntries");
                for (ExpandingEntryCardView.Entry entry : QuickContactActivity.this.contactInteractionsToEntries(arrayList)) {
                    ArrayList arrayList3 = new ArrayList(1);
                    arrayList3.add(entry);
                    arrayList2.add(arrayList3);
                }
                Trace.endSection();
                return null;
            }

            @Override
            protected void onPostExecute(Void r37) {
                super.onPostExecute(r37);
                Log.d("QuickContact", "[bindRecentData] onPostExecute(). " + QuickContactActivity.this.mRecentDataTask + ", allInteractions.size() = " + arrayList.size());
                Trace.beginSection("initialize recents card");
                if (arrayList.size() <= 0) {
                    QuickContactActivity.this.mRecentCard.setVisibility(8);
                } else {
                    QuickContactActivity.this.mRecentCard.initialize(arrayList2, 3, QuickContactActivity.this.mRecentCard.isExpanded(), false, QuickContactActivity.this.mExpandingEntryCardViewListener, QuickContactActivity.this.mScroller);
                    if (QuickContactActivity.this.mRecentCard.getVisibility() == 8 && QuickContactActivity.this.mShouldLog) {
                        Logger.logQuickContactEvent(QuickContactActivity.this.mReferrer, QuickContactActivity.this.mContactType, 3, 0, null);
                    }
                    QuickContactActivity.this.mRecentCard.setVisibility(0);
                }
                Trace.endSection();
                Trace.beginSection("initialize permission explanation card");
                ExpandingEntryCardView.Entry entry = new ExpandingEntryCardView.Entry(-3, ResourcesCompat.getDrawable(QuickContactActivity.this.getResources(), R.drawable.quantum_ic_history_vd_theme_24, null), QuickContactActivity.this.getString(R.string.permission_explanation_header), QuickContactActivity.this.mPermissionExplanationCardSubHeader, null, null, null, null, QuickContactActivity.this.getIntent(), null, null, null, true, false, null, null, null, null, 1, null, true, R.drawable.quantum_ic_history_vd_theme_24);
                ArrayList arrayList3 = new ArrayList();
                arrayList3.add(new ArrayList());
                ((List) arrayList3.get(0)).add(entry);
                int color = QuickContactActivity.this.getResources().getColor(android.R.color.white);
                PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP);
                QuickContactActivity.this.mPermissionExplanationCard.initialize(arrayList3, 1, true, true, null, QuickContactActivity.this.mScroller);
                QuickContactActivity.this.mPermissionExplanationCard.setColorAndFilter(color, porterDuffColorFilter);
                QuickContactActivity.this.mPermissionExplanationCard.setBackgroundColor(QuickContactActivity.this.mColorFilterColor);
                QuickContactActivity.this.mPermissionExplanationCard.setEntryHeaderColor(color);
                QuickContactActivity.this.mPermissionExplanationCard.setEntrySubHeaderColor(color);
                if (QuickContactActivity.this.mShouldShowPermissionExplanation) {
                    if (QuickContactActivity.this.mPermissionExplanationCard.getVisibility() == 8 && QuickContactActivity.this.mShouldLog) {
                        Logger.logQuickContactEvent(QuickContactActivity.this.mReferrer, QuickContactActivity.this.mContactType, 5, 0, null);
                    }
                    QuickContactActivity.this.mPermissionExplanationCard.setVisibility(0);
                } else {
                    QuickContactActivity.this.mPermissionExplanationCard.setVisibility(8);
                }
                Trace.endSection();
                if (QuickContactActivity.this.mAboutCard.shouldShow()) {
                    QuickContactActivity.this.mAboutCard.setVisibility(0);
                } else {
                    QuickContactActivity.this.mAboutCard.setVisibility(8);
                }
                QuickContactActivity.this.mRecentDataTask = null;
            }
        };
        Log.d("QuickContact", "[bindRecentData] mRecentDataTask.execute(). " + this.mRecentDataTask);
        this.mRecentDataTask.execute(new Void[0]);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w("QuickContact", "[onStop]mEntriesAndActionsTask=" + this.mEntriesAndActionsTask + ", mRecentDataTask=" + this.mRecentDataTask);
        if (this.mEntriesAndActionsTask != null) {
            this.mEntriesAndActionsTask.cancel(false);
        }
        if (this.mRecentDataTask != null) {
            this.mRecentDataTask.cancel(false);
        }
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().onHostActivityStopped();
    }

    @Override
    public void onDestroy() {
        Log.w("QuickContact", "[onDestroy]");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mListener);
        super.onDestroy();
        ExtensionManager.getInstance();
        ExtensionManager.getOp01Extension().resetVideoState();
    }

    private boolean isContactEditable() {
        return (this.mContactData == null || this.mContactData.isDirectoryEntry() || this.mContactData.isSdnContacts()) ? false : true;
    }

    private boolean isContactShareable() {
        return (this.mContactData == null || this.mContactData.isDirectoryEntry()) ? false : true;
    }

    private Intent getEditContactIntent() {
        return EditorIntents.createEditContactIntent(this, this.mContactData.getLookupUri(), this.mHasComputedThemeColor ? new MaterialColorMapUtils.MaterialPalette(this.mColorFilterColor, this.mStatusBarColor) : null, this.mContactData.getPhotoId());
    }

    private void editContact() {
        this.mHasIntentLaunched = true;
        this.mContactLoader.cacheResult();
        startActivityForResult(getEditContactIntent(), 1);
    }

    private void deleteContact() {
        ContactDeletionInteraction.start(this, this.mContactData.getLookupUri(), true);
    }

    private void toggleStar(MenuItem menuItem, boolean z) {
        CharSequence text;
        ContactDisplayUtils.configureStarredMenuItem(menuItem, this.mContactData.isDirectoryEntry(), this.mContactData.isUserProfile(), !z);
        startService(ContactSaveService.createSetStarredIntent(this, this.mContactData.getLookupUri(), !z));
        if (!z) {
            text = getResources().getText(R.string.description_action_menu_add_star);
        } else {
            text = getResources().getText(R.string.description_action_menu_remove_star);
        }
        this.mScroller.announceForAccessibility(text);
    }

    private void shareContact() {
        Log.d("QuickContact", "[shareContact]");
        Uri uriWithAppendedPath = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, this.mContactData.getLookupKey());
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/x-vcard");
        intent.putExtra("android.intent.extra.STREAM", uriWithAppendedPath);
        Intent intentCreateChooser = Intent.createChooser(intent, getResources().getQuantityString(R.plurals.title_share_via, 1));
        try {
            this.mHasIntentLaunched = true;
            ImplicitIntentsUtil.startActivityOutsideApp(this, intentCreateChooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.share_error, 0).show();
        }
    }

    private void createLauncherShortcutWithContact() {
        if (!this.mIsResumed) {
            Log.d("QuickContact", "[onShortcutIntentCreated]ignore! mIsResumed = " + this.mIsResumed);
            return;
        }
        if (BuildCompat.isAtLeastO()) {
            ShortcutManager shortcutManager = (ShortcutManager) getSystemService("shortcut");
            DynamicShortcuts dynamicShortcuts = new DynamicShortcuts(this);
            String displayName = this.mContactData.getDisplayName();
            if (displayName == null) {
                displayName = getString(R.string.missing_name);
            }
            ShortcutInfo quickContactShortcutInfo = dynamicShortcuts.getQuickContactShortcutInfo(this.mContactData.getId(), this.mContactData.getLookupKey(), displayName);
            if (quickContactShortcutInfo != null) {
                shortcutManager.requestPinShortcut(quickContactShortcutInfo, null);
                return;
            }
            return;
        }
        new ShortcutIntentBuilder(this, new ShortcutIntentBuilder.OnShortcutIntentCreatedListener() {
            @Override
            public void onShortcutIntentCreated(Uri uri, Intent intent) {
                String string;
                intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                QuickContactActivity.this.sendBroadcast(intent);
                String stringExtra = intent.getStringExtra("android.intent.extra.shortcut.NAME");
                if (TextUtils.isEmpty(stringExtra)) {
                    string = QuickContactActivity.this.getString(R.string.createContactShortcutSuccessful_NoName);
                } else {
                    string = QuickContactActivity.this.getString(R.string.createContactShortcutSuccessful, new Object[]{stringExtra});
                }
                Toast.makeText(QuickContactActivity.this, string, 0).show();
            }
        }).createContactShortcutIntent(this.mContactData.getLookupUri());
    }

    private boolean isShortcutCreatable() {
        if (this.mContactData == null || this.mContactData.isUserProfile() || this.mContactData.isDirectoryEntry()) {
            return false;
        }
        if (this.mContactData.getIndicate() >= 0) {
            Log.d("QuickContact", "[isShortcutCreatable()] contact indicator: " + this.mContactData.getIndicate());
            return false;
        }
        if (BuildCompat.isAtLeastO()) {
            return ((ShortcutManager) getSystemService("shortcut")).isRequestPinShortcutSupported();
        }
        Intent intent = new Intent();
        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        List<ResolveInfo> listQueryBroadcastReceivers = getPackageManager().queryBroadcastReceivers(intent, 0);
        return listQueryBroadcastReceivers != null && listQueryBroadcastReceivers.size() > 0;
    }

    private void setStateForPhoneMenuItems(Contact contact) {
        if (contact != null) {
            this.mSendToVoicemailState = contact.isSendToVoicemail();
            this.mCustomRingtone = contact.getCustomRingtone();
            this.mArePhoneOptionsChangable = isContactEditable() && PhoneCapabilityTester.isPhone(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.quickcontact, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mContactData == null) {
            return false;
        }
        MenuItem menuItemFindItem = menu.findItem(R.id.menu_star);
        ContactDisplayUtils.configureStarredMenuItem(menuItemFindItem, this.mContactData.isDirectoryEntry(), this.mContactData.isUserProfile(), this.mContactData.getStarred());
        MenuItem menuItemFindItem2 = menu.findItem(R.id.menu_edit);
        Log.d("QuickContact", "[onPrepareOptionsMenu] is sdn contact: " + this.mContactData.isSdnContacts());
        if (this.mContactData.isSdnContacts()) {
            menuItemFindItem2.setVisible(false);
        } else {
            menuItemFindItem2.setVisible(true);
            if (DirectoryContactUtil.isDirectoryContact(this.mContactData) || InvisibleContactUtil.isInvisibleAndAddable(this.mContactData, this)) {
                menuItemFindItem2.setIcon(R.drawable.quantum_ic_person_add_vd_theme_24);
                menuItemFindItem2.setTitle(R.string.menu_add_contact);
            } else if (isContactEditable()) {
                menuItemFindItem2.setIcon(R.drawable.quantum_ic_create_vd_theme_24);
                menuItemFindItem2.setTitle(R.string.menu_editContact);
            } else {
                menuItemFindItem2.setVisible(false);
            }
        }
        MenuItem menuItemFindItem3 = menu.findItem(R.id.menu_join);
        menuItemFindItem3.setVisible((InvisibleContactUtil.isInvisibleAndAddable(this.mContactData, this) || !isContactEditable() || this.mContactData.isUserProfile() || this.mContactData.isMultipleRawContacts()) ? false : true);
        menu.findItem(R.id.menu_linked_contacts).setVisible(this.mContactData.isMultipleRawContacts() && !menuItemFindItem3.isVisible());
        menu.findItem(R.id.menu_delete).setVisible(isContactEditable() && !this.mContactData.isUserProfile());
        menu.findItem(R.id.menu_share).setVisible(isContactShareable());
        MenuItem menuItemFindItem4 = menu.findItem(R.id.menu_create_contact_shortcut);
        menuItemFindItem4.setVisible(isShortcutCreatable());
        MenuItem menuItemFindItem5 = menu.findItem(R.id.menu_set_ringtone);
        menuItemFindItem5.setVisible(!this.mContactData.isUserProfile() && this.mArePhoneOptionsChangable);
        MenuItem menuItemFindItem6 = menu.findItem(R.id.menu_send_to_voicemail);
        menuItemFindItem6.setVisible(Build.VERSION.SDK_INT < 23 && !this.mContactData.isUserProfile() && this.mArePhoneOptionsChangable);
        menuItemFindItem6.setTitle(this.mSendToVoicemailState ? R.string.menu_unredirect_calls_to_vm : R.string.menu_redirect_calls_to_vm);
        menu.findItem(R.id.menu_help).setVisible(HelpUtils.isHelpAndFeedbackAvailable());
        ExtensionManager.getInstance();
        ExtensionManager.getRcsExtension().addQuickContactMenuOptions(menu, this.mLookupUri, this);
        if (this.mContactData.getIndicate() >= 0) {
            menuItemFindItem.setVisible(false);
            menuItemFindItem3.setVisible(false);
            menuItemFindItem4.setVisible(false);
            menuItemFindItem5.setVisible(false);
            menuItemFindItem6.setVisible(false);
        }
        ExtensionManager.getInstance();
        ExtensionManager.getContactsCommonPresenceExtension().addRefreshMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int i;
        int itemId = menuItem.getItemId();
        if (this.mContactData != null) {
            ExtensionManager.getInstance();
            if (ExtensionManager.getContactsCommonPresenceExtension().onOptionsItemSelected(itemId, this.mContactData.getContactId())) {
                return true;
            }
        }
        if (itemId == R.id.menu_star) {
            if (this.mContactData != null) {
                boolean zIsChecked = menuItem.isChecked();
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, zIsChecked ? 3 : 2, null);
                toggleStar(menuItem, zIsChecked);
            }
        } else if (itemId == R.id.menu_edit) {
            if (DirectoryContactUtil.isDirectoryContact(this.mContactData)) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 5, null);
                Intent intent = new Intent("android.intent.action.INSERT_OR_EDIT");
                intent.setType("vnd.android.cursor.item/contact");
                ArrayList<ContentValues> contentValues = this.mContactData.getContentValues();
                if (this.mContactData.getDisplayNameSource() >= 35) {
                    intent.putExtra("name", this.mContactData.getDisplayName());
                } else if (this.mContactData.getDisplayNameSource() == 30) {
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("data1", this.mContactData.getDisplayName());
                    contentValues2.put("mimetype", "vnd.android.cursor.item/organization");
                    contentValues.add(contentValues2);
                }
                for (ContentValues contentValues3 : contentValues) {
                    contentValues3.remove("last_time_used");
                    contentValues3.remove("times_used");
                }
                intent.putExtra("data", contentValues);
                if (this.mContactData.getDirectoryExportSupport() == 1) {
                    intent.putExtra("android.provider.extra.ACCOUNT", new Account(this.mContactData.getDirectoryAccountName(), this.mContactData.getDirectoryAccountType()));
                    intent.putExtra("android.provider.extra.DATA_SET", this.mContactData.getRawContacts().get(0).getDataSet());
                }
                intent.putExtra("disableDeleteMenuOption", true);
                intent.setPackage(getPackageName());
                startActivityForResult(intent, 2);
            } else if (InvisibleContactUtil.isInvisibleAndAddable(this.mContactData, this)) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 5, null);
                InvisibleContactUtil.addToDefaultGroup(this.mContactData, this);
            } else if (isContactEditable()) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 4, null);
                editContact();
            }
        } else {
            if (itemId == R.id.menu_join) {
                return doJoinContactAction();
            }
            if (itemId == R.id.menu_linked_contacts) {
                return showRawContactPickerDialog();
            }
            if (itemId == R.id.menu_delete) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 6, null);
                if (isContactEditable()) {
                    deleteContact();
                }
            } else if (itemId == R.id.menu_share) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 7, null);
                if (isContactShareable()) {
                    shareContact();
                }
            } else if (itemId == R.id.menu_create_contact_shortcut) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 8, null);
                if (isShortcutCreatable()) {
                    createLauncherShortcutWithContact();
                }
            } else if (itemId == R.id.menu_set_ringtone) {
                doPickRingtone();
            } else if (itemId == R.id.menu_send_to_voicemail) {
                this.mSendToVoicemailState = !this.mSendToVoicemailState;
                if (this.mSendToVoicemailState) {
                    i = R.string.menu_unredirect_calls_to_vm;
                } else {
                    i = R.string.menu_redirect_calls_to_vm;
                }
                menuItem.setTitle(i);
                startService(ContactSaveService.createSetSendToVoicemail(this, this.mLookupUri, this.mSendToVoicemailState));
            } else if (itemId == R.id.menu_help) {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 9, null);
                HelpUtils.launchHelpAndFeedbackForContactScreen(this);
            } else {
                Logger.logQuickContactEvent(this.mReferrer, this.mContactType, 0, 0, null);
                return super.onOptionsItemSelected(menuItem);
            }
        }
        return true;
    }

    private boolean showRawContactPickerDialog() {
        MaterialColorMapUtils.MaterialPalette materialPalette;
        if (this.mContactData == null) {
            return false;
        }
        Uri lookupUri = this.mContactData.getLookupUri();
        if (this.mHasComputedThemeColor) {
            materialPalette = new MaterialColorMapUtils.MaterialPalette(this.mColorFilterColor, this.mStatusBarColor);
        } else {
            materialPalette = null;
        }
        startActivityForResult(EditorIntents.createViewLinkedContactsIntent(this, lookupUri, materialPalette), 1);
        return true;
    }

    private boolean doJoinContactAction() {
        Log.d("QuickContact", "[doJoinContactAction]");
        if (this.mContactData == null) {
            return false;
        }
        this.mPreviousContactId = this.mContactData.getId();
        Intent intent = new Intent(this, (Class<?>) ContactSelectionActivity.class);
        intent.setAction("com.android.contacts.action.JOIN_CONTACT");
        intent.putExtra("com.android.contacts.action.CONTACT_ID", this.mPreviousContactId);
        startActivityForResult(intent, 3);
        return true;
    }

    private void joinAggregate(long j) {
        startService(ContactSaveService.createJoinContactsIntent(this, this.mPreviousContactId, j, QuickContactActivity.class, "android.intent.action.VIEW"));
        showLinkProgressBar();
    }

    private void doPickRingtone() {
        Intent intent = new Intent("android.intent.action.RINGTONE_PICKER");
        intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true);
        intent.putExtra("android.intent.extra.ringtone.TYPE", 1);
        intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", true);
        intent.putExtra("android.intent.extra.ringtone.EXISTING_URI", EditorUiUtils.getRingtoneUriFromString(this.mCustomRingtone, CURRENT_API_VERSION));
        try {
            startActivityForResult(intent, 4);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.missing_app, 0).show();
        }
    }

    private void dismissProgressBar() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            this.mProgressDialog.dismiss();
        }
    }

    private void showLinkProgressBar() {
        this.mProgressDialog.setMessage(getString(R.string.contacts_linking_progress_bar));
        this.mProgressDialog.show();
    }

    private void showUnlinkProgressBar() {
        this.mProgressDialog.setMessage(getString(R.string.contacts_unlinking_progress_bar));
        this.mProgressDialog.show();
    }

    private void maybeShowProgressDialog() {
        if (ContactSaveService.getState().isActionPending(ContactSaveService.ACTION_SPLIT_CONTACT)) {
            showUnlinkProgressBar();
        } else if (ContactSaveService.getState().isActionPending(ContactSaveService.ACTION_JOIN_CONTACTS)) {
            showLinkProgressBar();
        }
    }

    private class SaveServiceListener extends BroadcastReceiver {
        private SaveServiceListener() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable("QuickContact", 3)) {
                Log.d("QuickContact", "Got broadcast from save service " + intent);
            }
            if (ContactSaveService.BROADCAST_LINK_COMPLETE.equals(intent.getAction()) || ContactSaveService.BROADCAST_UNLINK_COMPLETE.equals(intent.getAction())) {
                QuickContactActivity.this.dismissProgressBar();
                if (ContactSaveService.BROADCAST_UNLINK_COMPLETE.equals(intent.getAction())) {
                    QuickContactActivity.this.finish();
                }
            }
        }
    }
}
