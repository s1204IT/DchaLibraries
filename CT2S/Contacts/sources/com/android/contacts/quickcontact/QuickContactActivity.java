package com.android.contacts.quickcontact;

import android.accounts.Account;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ParseException;
import android.net.Uri;
import android.net.WebAddress;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Trace;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.support.v7.graphics.Palette;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsActivity;
import com.android.contacts.NfcHandler;
import com.android.contacts.R;
import com.android.contacts.common.CallUtil;
import com.android.contacts.common.ClipboardUtils;
import com.android.contacts.common.Collapser;
import com.android.contacts.common.ContactsUtils;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.android.contacts.common.interactions.TouchPointManager;
import com.android.contacts.common.lettertiles.LetterTileDrawable;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.Contact;
import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.common.model.RawContact;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.EmailDataItem;
import com.android.contacts.common.model.dataitem.EventDataItem;
import com.android.contacts.common.model.dataitem.ImDataItem;
import com.android.contacts.common.model.dataitem.NicknameDataItem;
import com.android.contacts.common.model.dataitem.NoteDataItem;
import com.android.contacts.common.model.dataitem.OrganizationDataItem;
import com.android.contacts.common.model.dataitem.PhoneDataItem;
import com.android.contacts.common.model.dataitem.RelationDataItem;
import com.android.contacts.common.model.dataitem.SipAddressDataItem;
import com.android.contacts.common.model.dataitem.StructuredNameDataItem;
import com.android.contacts.common.model.dataitem.StructuredPostalDataItem;
import com.android.contacts.common.model.dataitem.WebsiteDataItem;
import com.android.contacts.common.util.DateUtils;
import com.android.contacts.common.util.MaterialColorMapUtils;
import com.android.contacts.common.util.TelephonyManagerUtils;
import com.android.contacts.common.util.ViewUtil;
import com.android.contacts.detail.ContactDisplayUtils;
import com.android.contacts.interactions.CalendarInteractionsLoader;
import com.android.contacts.interactions.CallLogInteractionsLoader;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.interactions.ContactInteraction;
import com.android.contacts.interactions.SmsInteractionsLoader;
import com.android.contacts.quickcontact.ExpandingEntryCardView;
import com.android.contacts.util.ImageViewDrawableSetter;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.SchedulingUtils;
import com.android.contacts.util.StructuredPostalUtils;
import com.android.contacts.widget.MultiShrinkScroller;
import com.android.contacts.widget.QuickContactImageView;
import com.google.common.collect.Lists;
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
    private Cp2DataCardModel mCachedCp2DataCardModel;
    private PorterDuffColorFilter mColorFilter;
    private ExpandingEntryCardView mContactCard;
    private Contact mContactData;
    private ContactLoader mContactLoader;
    private AsyncTask<Void, Void, Cp2DataCardModel> mEntriesAndActionsTask;
    private String[] mExcludeMimes;
    private int mExtraMode;
    private boolean mHasAlreadyBeenOpened;
    private boolean mHasComputedThemeColor;
    private boolean mHasIntentLaunched;
    private boolean mIsEntranceAnimationFinished;
    private boolean mIsExitAnimationInProgress;
    private Uri mLookupUri;
    private MaterialColorMapUtils mMaterialColorMapUtils;
    private ExpandingEntryCardView mNoContactDetailsCard;
    private boolean mOnlyOneEmail;
    private boolean mOnlyOnePhoneNumber;
    private QuickContactImageView mPhotoView;
    private ExpandingEntryCardView mRecentCard;
    private AsyncTask<Void, Void, Void> mRecentDataTask;
    private MultiShrinkScroller mScroller;
    private SelectAccountDialogFragmentListener mSelectAccountFragmentListener;
    private int mStatusBarColor;
    private ColorDrawable mWindowScrim;
    private static final int SCRIM_COLOR = Color.argb(200, 0, 0, 0);
    private static final List<String> LEADING_MIMETYPES = Lists.newArrayList("vnd.android.cursor.item/phone_v2", "vnd.android.cursor.item/sip_address", "vnd.android.cursor.item/email_v2", "vnd.android.cursor.item/postal-address_v2");
    private static final List<String> SORTED_ABOUT_CARD_MIMETYPES = Lists.newArrayList("vnd.android.cursor.item/nickname", "vnd.android.cursor.item/website", "vnd.android.cursor.item/organization", "vnd.android.cursor.item/contact_event", "vnd.android.cursor.item/relation", "vnd.android.cursor.item/im", "vnd.android.cursor.item/group_membership", "vnd.android.cursor.item/identity", "vnd.android.cursor.item/note");
    private static final BidiFormatter sBidiFormatter = BidiFormatter.getInstance();
    private static final String KEY_LOADER_EXTRA_PHONES = QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_PHONES";
    private static final String KEY_LOADER_EXTRA_EMAILS = QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_EMAILS";
    private static final int[] mRecentLoaderIds = {1, 2, 3};
    private final ImageViewDrawableSetter mPhotoSetter = new ImageViewDrawableSetter();
    private Map<Integer, List<ContactInteraction>> mRecentLoaderResults = new ConcurrentHashMap(4, 0.9f, 1);
    final View.OnClickListener mEntryClickHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Object entryTagObject = v.getTag();
            if (entryTagObject == null || !(entryTagObject instanceof ExpandingEntryCardView.EntryTag)) {
                Log.w("QuickContact", "EntryTag was not used correctly");
                return;
            }
            ExpandingEntryCardView.EntryTag entryTag = (ExpandingEntryCardView.EntryTag) entryTagObject;
            Intent intent = entryTag.getIntent();
            int dataId = entryTag.getId();
            if (dataId == -2) {
                QuickContactActivity.this.editContact();
                return;
            }
            String usageType = "call";
            Uri intentUri = intent.getData();
            if ((intentUri != null && intentUri.getScheme() != null && intentUri.getScheme().equals("smsto")) || (intent.getType() != null && intent.getType().equals("vnd.android-dir/mms-sms"))) {
                usageType = "short_text";
            }
            if (dataId > 0) {
                Uri dataUsageUri = ContactsContract.DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(String.valueOf(dataId)).appendQueryParameter("type", usageType).build();
                boolean successful = QuickContactActivity.this.getContentResolver().update(dataUsageUri, new ContentValues(), null, null) > 0;
                if (!successful) {
                    Log.w("QuickContact", "DataUsageFeedback increment failed");
                }
            } else {
                Log.w("QuickContact", "Invalid Data ID");
            }
            if ("android.intent.action.CALL".equals(intent.getAction()) && TouchPointManager.getInstance().hasValidPoint()) {
                Bundle extras = new Bundle();
                extras.putParcelable("touchPoint", TouchPointManager.getInstance().getPoint());
                intent.putExtra("android.telecom.extra.OUTGOING_CALL_EXTRAS", extras);
            }
            intent.addFlags(268435456);
            QuickContactActivity.this.mHasIntentLaunched = true;
            try {
                QuickContactActivity.this.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app, 0).show();
            } catch (SecurityException e2) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app, 0).show();
                Log.e("QuickContact", "QuickContacts does not have permission to launch " + intent);
            }
        }
    };
    final ExpandingEntryCardView.ExpandingEntryCardViewListener mExpandingEntryCardViewListener = new ExpandingEntryCardView.ExpandingEntryCardViewListener() {
        @Override
        public void onCollapse(int heightDelta) {
            QuickContactActivity.this.mScroller.prepareForShrinkingScrollChild(heightDelta);
        }

        @Override
        public void onExpand(int heightDelta) {
            QuickContactActivity.this.mScroller.prepareForExpandingScrollChild();
        }
    };
    private final View.OnCreateContextMenuListener mEntryContextMenuListener = new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            if (menuInfo != null) {
                ExpandingEntryCardView.EntryContextMenuInfo info = (ExpandingEntryCardView.EntryContextMenuInfo) menuInfo;
                menu.setHeaderTitle(info.getCopyText());
                menu.add(0, 0, 0, QuickContactActivity.this.getString(R.string.copy_text));
                if (QuickContactActivity.this.isContactEditable()) {
                    String selectedMimeType = info.getMimeType();
                    boolean onlyOneOfMimeType = true;
                    if ("vnd.android.cursor.item/phone_v2".equals(selectedMimeType)) {
                        onlyOneOfMimeType = QuickContactActivity.this.mOnlyOnePhoneNumber;
                    } else if ("vnd.android.cursor.item/email_v2".equals(selectedMimeType)) {
                        onlyOneOfMimeType = QuickContactActivity.this.mOnlyOneEmail;
                    }
                    if (info.isSuperPrimary()) {
                        menu.add(0, 1, 0, QuickContactActivity.this.getString(R.string.clear_default));
                    } else if (!onlyOneOfMimeType) {
                        menu.add(0, 2, 0, QuickContactActivity.this.getString(R.string.set_default));
                    }
                }
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
        public void onTransparentViewHeightChange(float ratio) {
            if (QuickContactActivity.this.mIsEntranceAnimationFinished) {
                QuickContactActivity.this.mWindowScrim.setAlpha((int) (255.0f * ratio));
            }
        }
    };
    private final Comparator<DataItem> mWithinMimeTypeDataItemComparator = new Comparator<DataItem>() {
        @Override
        public int compare(DataItem lhs, DataItem rhs) {
            if (!lhs.getMimeType().equals(rhs.getMimeType())) {
                Log.wtf("QuickContact", "Comparing DataItems with different mimetypes lhs.getMimeType(): " + lhs.getMimeType() + " rhs.getMimeType(): " + rhs.getMimeType());
                return 0;
            }
            if (lhs.isSuperPrimary()) {
                return -1;
            }
            if (rhs.isSuperPrimary()) {
                return 1;
            }
            if (lhs.isPrimary() && !rhs.isPrimary()) {
                return -1;
            }
            if (!lhs.isPrimary() && rhs.isPrimary()) {
                return 1;
            }
            int lhsTimesUsed = lhs.getTimesUsed() == null ? 0 : lhs.getTimesUsed().intValue();
            int rhsTimesUsed = rhs.getTimesUsed() == null ? 0 : rhs.getTimesUsed().intValue();
            return rhsTimesUsed - lhsTimesUsed;
        }
    };
    private final Comparator<List<DataItem>> mAmongstMimeTypeDataItemComparator = new Comparator<List<DataItem>>() {
        @Override
        public int compare(List<DataItem> lhsList, List<DataItem> rhsList) {
            DataItem lhs = lhsList.get(0);
            DataItem rhs = rhsList.get(0);
            int lhsTimesUsed = lhs.getTimesUsed() == null ? 0 : lhs.getTimesUsed().intValue();
            int rhsTimesUsed = rhs.getTimesUsed() == null ? 0 : rhs.getTimesUsed().intValue();
            int timesUsedDifference = rhsTimesUsed - lhsTimesUsed;
            if (timesUsedDifference == 0) {
                long lhsLastTimeUsed = lhs.getLastTimeUsed() == null ? 0L : lhs.getLastTimeUsed().longValue();
                long rhsLastTimeUsed = rhs.getLastTimeUsed() == null ? 0L : rhs.getLastTimeUsed().longValue();
                long lastTimeUsedDifference = rhsLastTimeUsed - lhsLastTimeUsed;
                if (lastTimeUsedDifference > 0) {
                    return 1;
                }
                if (lastTimeUsedDifference < 0) {
                    return -1;
                }
                String lhsMimeType = lhs.getMimeType();
                String rhsMimeType = rhs.getMimeType();
                for (String mimeType : QuickContactActivity.LEADING_MIMETYPES) {
                    if (lhsMimeType.equals(mimeType)) {
                        return -1;
                    }
                    if (rhsMimeType.equals(mimeType)) {
                        return 1;
                    }
                }
                return 0;
            }
            return timesUsedDifference;
        }
    };
    private final LoaderManager.LoaderCallbacks<Contact> mLoaderContactCallbacks = new LoaderManager.LoaderCallbacks<Contact>() {
        @Override
        public void onLoaderReset(Loader<Contact> loader) {
            QuickContactActivity.this.mContactData = null;
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            Trace.beginSection("onLoadFinished()");
            try {
                if (QuickContactActivity.this.isFinishing()) {
                    return;
                }
                if (data.isError()) {
                    Log.i("QuickContact", "Failed to load contact: " + ((ContactLoader) loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage, 1).show();
                    QuickContactActivity.this.finish();
                } else {
                    if (!data.isNotFound()) {
                        QuickContactActivity.this.bindContactData(data);
                        return;
                    }
                    Log.i("QuickContact", "No contact found: " + ((ContactLoader) loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage, 1).show();
                    QuickContactActivity.this.finish();
                }
            } finally {
                Trace.endSection();
            }
        }

        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            if (QuickContactActivity.this.mLookupUri == null) {
                Log.wtf("QuickContact", "Lookup uri wasn't initialized. Loader was started too early");
            }
            return new ContactLoader(QuickContactActivity.this.getApplicationContext(), QuickContactActivity.this.mLookupUri, true, false, true, true);
        }
    };
    private final LoaderManager.LoaderCallbacks<List<ContactInteraction>> mLoaderInteractionsCallbacks = new LoaderManager.LoaderCallbacks<List<ContactInteraction>>() {
        @Override
        public Loader<List<ContactInteraction>> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case 1:
                    Loader<List<ContactInteraction>> loader = new SmsInteractionsLoader(QuickContactActivity.this, args.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_PHONES), 3);
                    return loader;
                case 2:
                    String[] emailsArray = args.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_EMAILS);
                    List<String> emailsList = null;
                    if (emailsArray != null) {
                        emailsList = Arrays.asList(args.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_EMAILS));
                    }
                    Loader<List<ContactInteraction>> loader2 = new CalendarInteractionsLoader(QuickContactActivity.this, emailsList, 3, 3, 604800000L, 86400000L);
                    return loader2;
                case 3:
                    Loader<List<ContactInteraction>> loader3 = new CallLogInteractionsLoader(QuickContactActivity.this, args.getStringArray(QuickContactActivity.KEY_LOADER_EXTRA_PHONES), 3);
                    return loader3;
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<List<ContactInteraction>> loader, List<ContactInteraction> data) {
            QuickContactActivity.this.mRecentLoaderResults.put(Integer.valueOf(loader.getId()), data);
            if (QuickContactActivity.this.isAllRecentDataLoaded()) {
                QuickContactActivity.this.bindRecentData();
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ContactInteraction>> loader) {
            QuickContactActivity.this.mRecentLoaderResults.remove(Integer.valueOf(loader.getId()));
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        try {
            ExpandingEntryCardView.EntryContextMenuInfo menuInfo = (ExpandingEntryCardView.EntryContextMenuInfo) item.getMenuInfo();
            switch (item.getItemId()) {
                case 0:
                    ClipboardUtils.copyText(this, menuInfo.getCopyLabel(), menuInfo.getCopyText(), true);
                    return true;
                case 1:
                    Intent clearIntent = ContactSaveService.createClearPrimaryIntent(this, menuInfo.getId());
                    startService(clearIntent);
                    return true;
                case 2:
                    Intent setIntent = ContactSaveService.createSetSuperPrimaryIntent(this, menuInfo.getId());
                    startService(setIntent);
                    return true;
                default:
                    throw new IllegalArgumentException("Unknown menu option " + item.getItemId());
            }
        } catch (ClassCastException e) {
            Log.e("QuickContact", "bad menuInfo", e);
            return false;
        }
    }

    public static class SelectAccountDialogFragmentListener extends Fragment implements SelectAccountDialogFragment.Listener {
        private QuickContactActivity mQuickContactActivity;

        @Override
        public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
            DirectoryContactUtil.createCopy(this.mQuickContactActivity.mContactData.getContentValues(), account, this.mQuickContactActivity);
        }

        @Override
        public void onAccountSelectorCancelled() {
        }

        public void setQuickContactActivity(QuickContactActivity quickContactActivity) {
            this.mQuickContactActivity = quickContactActivity;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("onCreate()");
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0);
        processIntent(getIntent());
        getWindow().setFlags(131072, 131072);
        setContentView(R.layout.quickcontact_activity);
        this.mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());
        this.mScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);
        this.mContactCard = (ExpandingEntryCardView) findViewById(R.id.communication_card);
        this.mNoContactDetailsCard = (ExpandingEntryCardView) findViewById(R.id.no_contact_data_card);
        this.mRecentCard = (ExpandingEntryCardView) findViewById(R.id.recent_card);
        this.mAboutCard = (ExpandingEntryCardView) findViewById(R.id.about_card);
        this.mNoContactDetailsCard.setOnClickListener(this.mEntryClickHandler);
        this.mContactCard.setOnClickListener(this.mEntryClickHandler);
        this.mContactCard.setExpandButtonText(getResources().getString(R.string.expanding_entry_card_view_see_all));
        this.mContactCard.setOnCreateContextMenuListener(this.mEntryContextMenuListener);
        this.mRecentCard.setOnClickListener(this.mEntryClickHandler);
        this.mRecentCard.setTitle(getResources().getString(R.string.recent_card_title));
        this.mAboutCard.setOnClickListener(this.mEntryClickHandler);
        this.mAboutCard.setOnCreateContextMenuListener(this.mEntryContextMenuListener);
        this.mPhotoView = (QuickContactImageView) findViewById(R.id.photo);
        View transparentView = findViewById(R.id.transparent_view);
        if (this.mScroller != null) {
            transparentView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    QuickContactActivity.this.mScroller.scrollOffBottom();
                }
            });
        }
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle((CharSequence) null);
        toolbar.addView(getLayoutInflater().inflate(R.layout.quickcontact_title_placeholder, (ViewGroup) null));
        this.mHasAlreadyBeenOpened = savedInstanceState != null;
        this.mIsEntranceAnimationFinished = this.mHasAlreadyBeenOpened;
        this.mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        this.mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(this.mWindowScrim);
        this.mScroller.initialize(this.mMultiShrinkScrollerListener, this.mExtraMode == 4);
        this.mScroller.setVisibility(4);
        setHeaderNameText(R.string.missing_name);
        this.mSelectAccountFragmentListener = (SelectAccountDialogFragmentListener) getFragmentManager().findFragmentByTag("select_account_fragment");
        if (this.mSelectAccountFragmentListener == null) {
            this.mSelectAccountFragmentListener = new SelectAccountDialogFragmentListener();
            getFragmentManager().beginTransaction().add(0, this.mSelectAccountFragmentListener, "select_account_fragment").commit();
            this.mSelectAccountFragmentListener.setRetainInstance(true);
        }
        this.mSelectAccountFragmentListener.setQuickContactActivity(this);
        SchedulingUtils.doOnPreDraw(this.mScroller, true, new Runnable() {
            @Override
            public void run() {
                if (!QuickContactActivity.this.mHasAlreadyBeenOpened) {
                    float alphaRatio = QuickContactActivity.this.mExtraMode == 4 ? 1.0f : QuickContactActivity.this.mScroller.getStartingTransparentHeightRatio();
                    int duration = QuickContactActivity.this.getResources().getInteger(android.R.integer.config_shortAnimTime);
                    int desiredAlpha = (int) (255.0f * alphaRatio);
                    ObjectAnimator o = ObjectAnimator.ofInt(QuickContactActivity.this.mWindowScrim, "alpha", 0, desiredAlpha).setDuration(duration);
                    o.start();
                }
            }
        });
        if (savedInstanceState != null) {
            final int color = savedInstanceState.getInt("theme_color", 0);
            SchedulingUtils.doOnPreDraw(this.mScroller, false, new Runnable() {
                @Override
                public void run() {
                    if (QuickContactActivity.this.mHasAlreadyBeenOpened) {
                        QuickContactActivity.this.mScroller.setVisibility(0);
                        QuickContactActivity.this.mScroller.setScroll(QuickContactActivity.this.mScroller.getScrollNeededToBeFullScreen());
                    }
                    if (color != 0) {
                        QuickContactActivity.this.setThemeColor(QuickContactActivity.this.mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(color));
                    }
                }
            });
        }
        Trace.endSection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == 3) {
            finish();
        } else if (requestCode == 2 && resultCode != 0) {
            processIntent(data);
        }
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (this.mColorFilter != null) {
            savedInstanceState.putInt("theme_color", this.mColorFilter.getColor());
        }
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }
        Uri lookupUri = intent.getData();
        if (lookupUri != null && "contacts".equals(lookupUri.getAuthority())) {
            long rawContactId = ContentUris.parseId(lookupUri);
            lookupUri = ContactsContract.RawContacts.getContactLookupUri(getContentResolver(), ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId));
        }
        this.mExtraMode = getIntent().getIntExtra("android.provider.extra.MODE", 3);
        Uri oldLookupUri = this.mLookupUri;
        if (lookupUri == null) {
            finish();
            return;
        }
        this.mLookupUri = lookupUri;
        this.mExcludeMimes = intent.getStringArrayExtra("android.provider.extra.EXCLUDE_MIMES");
        if (oldLookupUri == null) {
            this.mContactLoader = (ContactLoader) getLoaderManager().initLoader(0, null, this.mLoaderContactCallbacks);
        } else if (oldLookupUri != this.mLookupUri) {
            destroyInteractionLoaders();
            this.mContactLoader = (ContactLoader) getLoaderManager().restartLoader(0, null, this.mLoaderContactCallbacks);
            this.mCachedCp2DataCardModel = null;
        }
        NfcHandler.register(this, this.mLookupUri);
    }

    private void destroyInteractionLoaders() {
        int[] arr$ = mRecentLoaderIds;
        for (int interactionLoaderId : arr$) {
            getLoaderManager().destroyLoader(interactionLoaderId);
        }
    }

    private void runEntranceAnimation() {
        if (!this.mHasAlreadyBeenOpened) {
            this.mHasAlreadyBeenOpened = true;
            this.mScroller.scrollUpForEntranceAnimation(this.mExtraMode != 4);
        }
    }

    private void setHeaderNameText(int resId) {
        if (this.mScroller != null) {
            this.mScroller.setTitle(getText(resId) == null ? null : getText(resId).toString());
        }
    }

    private void setHeaderNameText(String value) {
        if (!TextUtils.isEmpty(value) && this.mScroller != null) {
            this.mScroller.setTitle(value);
        }
    }

    private boolean isMimeExcluded(String mimeType) {
        if (this.mExcludeMimes == null) {
            return false;
        }
        String[] arr$ = this.mExcludeMimes;
        for (String excludedMime : arr$) {
            if (TextUtils.equals(excludedMime, mimeType)) {
                return true;
            }
        }
        return false;
    }

    private void bindContactData(final Contact data) {
        Trace.beginSection("bindContactData");
        this.mContactData = data;
        invalidateOptionsMenu();
        Trace.endSection();
        Trace.beginSection("Set display photo & name");
        this.mPhotoView.setIsBusiness(this.mContactData.isDisplayNameFromOrganization());
        this.mPhotoSetter.setupContactPhoto(data, this.mPhotoView);
        extractAndApplyTintFromPhotoViewAsynchronously();
        setHeaderNameText(ContactDisplayUtils.getDisplayName(this, data).toString());
        Trace.endSection();
        this.mEntriesAndActionsTask = new AsyncTask<Void, Void, Cp2DataCardModel>() {
            @Override
            protected Cp2DataCardModel doInBackground(Void... params) {
                return QuickContactActivity.this.generateDataModelFromContact(data);
            }

            @Override
            protected void onPostExecute(Cp2DataCardModel cardDataModel) {
                super.onPostExecute(cardDataModel);
                if (data == QuickContactActivity.this.mContactData && !isCancelled()) {
                    QuickContactActivity.this.bindDataToCards(cardDataModel);
                    QuickContactActivity.this.showActivity();
                }
            }
        };
        this.mEntriesAndActionsTask.execute(new Void[0]);
    }

    private void bindDataToCards(Cp2DataCardModel cp2DataCardModel) {
        startInteractionLoaders(cp2DataCardModel);
        populateContactAndAboutCard(cp2DataCardModel);
    }

    private void startInteractionLoaders(Cp2DataCardModel cp2DataCardModel) {
        Map<String, List<DataItem>> dataItemsMap = cp2DataCardModel.dataItemsMap;
        List<DataItem> phoneDataItems = dataItemsMap.get("vnd.android.cursor.item/phone_v2");
        if (phoneDataItems != null && phoneDataItems.size() == 1) {
            this.mOnlyOnePhoneNumber = true;
        }
        String[] phoneNumbers = null;
        if (phoneDataItems != null) {
            phoneNumbers = new String[phoneDataItems.size()];
            for (int i = 0; i < phoneDataItems.size(); i++) {
                phoneNumbers[i] = ((PhoneDataItem) phoneDataItems.get(i)).getNumber();
            }
        }
        Bundle phonesExtraBundle = new Bundle();
        phonesExtraBundle.putStringArray(KEY_LOADER_EXTRA_PHONES, phoneNumbers);
        Trace.beginSection("start sms loader");
        getLoaderManager().initLoader(1, phonesExtraBundle, this.mLoaderInteractionsCallbacks);
        Trace.endSection();
        Trace.beginSection("start call log loader");
        getLoaderManager().initLoader(3, phonesExtraBundle, this.mLoaderInteractionsCallbacks);
        Trace.endSection();
        Trace.beginSection("start calendar loader");
        List<DataItem> emailDataItems = dataItemsMap.get("vnd.android.cursor.item/email_v2");
        if (emailDataItems != null && emailDataItems.size() == 1) {
            this.mOnlyOneEmail = true;
        }
        String[] emailAddresses = null;
        if (emailDataItems != null) {
            emailAddresses = new String[emailDataItems.size()];
            for (int i2 = 0; i2 < emailDataItems.size(); i2++) {
                emailAddresses[i2] = ((EmailDataItem) emailDataItems.get(i2)).getAddress();
            }
        }
        Bundle emailsExtraBundle = new Bundle();
        emailsExtraBundle.putStringArray(KEY_LOADER_EXTRA_EMAILS, emailAddresses);
        getLoaderManager().initLoader(2, emailsExtraBundle, this.mLoaderInteractionsCallbacks);
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

    private List<List<ExpandingEntryCardView.Entry>> buildAboutCardEntries(Map<String, List<DataItem>> dataItemsMap) {
        List<List<ExpandingEntryCardView.Entry>> aboutCardEntries = new ArrayList<>();
        for (String mimetype : SORTED_ABOUT_CARD_MIMETYPES) {
            List<DataItem> mimeTypeItems = dataItemsMap.get(mimetype);
            if (mimeTypeItems != null) {
                List<ExpandingEntryCardView.Entry> aboutEntries = dataItemsToEntries(mimeTypeItems, null);
                if (aboutEntries.size() > 0) {
                    aboutCardEntries.add(aboutEntries);
                }
            }
        }
        return aboutCardEntries;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mHasIntentLaunched) {
            this.mHasIntentLaunched = false;
            populateContactAndAboutCard(this.mCachedCp2DataCardModel);
        }
        if (this.mCachedCp2DataCardModel != null) {
            destroyInteractionLoaders();
            startInteractionLoaders(this.mCachedCp2DataCardModel);
        }
    }

    private void populateContactAndAboutCard(Cp2DataCardModel cp2DataCardModel) {
        this.mCachedCp2DataCardModel = cp2DataCardModel;
        if (!this.mHasIntentLaunched && cp2DataCardModel != null) {
            Trace.beginSection("bind contact card");
            List<List<ExpandingEntryCardView.Entry>> contactCardEntries = cp2DataCardModel.contactCardEntries;
            List<List<ExpandingEntryCardView.Entry>> aboutCardEntries = cp2DataCardModel.aboutCardEntries;
            String customAboutCardName = cp2DataCardModel.customAboutCardName;
            if (contactCardEntries.size() > 0) {
                this.mContactCard.initialize(contactCardEntries, 3, this.mContactCard.isExpanded(), false, this.mExpandingEntryCardViewListener, this.mScroller);
                this.mContactCard.setVisibility(0);
            } else {
                this.mContactCard.setVisibility(8);
            }
            Trace.endSection();
            Trace.beginSection("bind about card");
            String phoneticName = this.mContactData.getPhoneticName();
            if (!TextUtils.isEmpty(phoneticName)) {
                ExpandingEntryCardView.Entry phoneticEntry = new ExpandingEntryCardView.Entry(-1, null, getResources().getString(R.string.name_phonetic), phoneticName, null, null, null, null, null, null, null, null, false, false, new ExpandingEntryCardView.EntryContextMenuInfo(phoneticName, getResources().getString(R.string.name_phonetic), null, -1L, false), null, null, null, 0);
                List<ExpandingEntryCardView.Entry> phoneticList = new ArrayList<>();
                phoneticList.add(phoneticEntry);
                if (aboutCardEntries.size() > 0 && aboutCardEntries.get(0).get(0).getHeader().equals(getResources().getString(R.string.header_nickname_entry))) {
                    aboutCardEntries.add(1, phoneticList);
                } else {
                    aboutCardEntries.add(0, phoneticList);
                }
            }
            if (!TextUtils.isEmpty(customAboutCardName)) {
                this.mAboutCard.setTitle(customAboutCardName);
            }
            if (aboutCardEntries.size() > 0) {
                this.mAboutCard.initialize(aboutCardEntries, 1, true, true, this.mExpandingEntryCardViewListener, this.mScroller);
            }
            if (contactCardEntries.size() == 0 && aboutCardEntries.size() == 0) {
                initializeNoContactDetailCard();
            } else {
                this.mNoContactDetailsCard.setVisibility(8);
            }
            if (isAllRecentDataLoaded() && aboutCardEntries.size() > 0) {
                this.mAboutCard.setVisibility(0);
            }
            Trace.endSection();
        }
    }

    private void initializeNoContactDetailCard() {
        Drawable phoneIcon = getResources().getDrawable(R.drawable.ic_phone_24dp).mutate();
        ExpandingEntryCardView.Entry phonePromptEntry = new ExpandingEntryCardView.Entry(-2, phoneIcon, getString(R.string.quickcontact_add_phone_number), null, null, null, null, null, getEditContactIntent(), null, null, null, true, false, null, null, null, null, R.drawable.ic_phone_24dp);
        Drawable emailIcon = getResources().getDrawable(R.drawable.ic_email_24dp).mutate();
        ExpandingEntryCardView.Entry emailPromptEntry = new ExpandingEntryCardView.Entry(-2, emailIcon, getString(R.string.quickcontact_add_email), null, null, null, null, null, getEditContactIntent(), null, null, null, true, false, null, null, null, null, R.drawable.ic_email_24dp);
        List<List<ExpandingEntryCardView.Entry>> promptEntries = new ArrayList<>();
        promptEntries.add(new ArrayList<>(1));
        promptEntries.add(new ArrayList<>(1));
        promptEntries.get(0).add(phonePromptEntry);
        promptEntries.get(1).add(emailPromptEntry);
        int subHeaderTextColor = getResources().getColor(R.color.quickcontact_entry_sub_header_text_color);
        PorterDuffColorFilter greyColorFilter = new PorterDuffColorFilter(subHeaderTextColor, PorterDuff.Mode.SRC_ATOP);
        this.mNoContactDetailsCard.initialize(promptEntries, 2, true, true, this.mExpandingEntryCardViewListener, this.mScroller);
        this.mNoContactDetailsCard.setVisibility(0);
        this.mNoContactDetailsCard.setEntryHeaderColor(subHeaderTextColor);
        this.mNoContactDetailsCard.setColorAndFilter(subHeaderTextColor, greyColorFilter);
    }

    private Cp2DataCardModel generateDataModelFromContact(Contact data) {
        Trace.beginSection("Build data items map");
        Map<String, List<DataItem>> dataItemsMap = new HashMap<>();
        ResolveCache.getInstance(this);
        for (RawContact rawContact : data.getRawContacts()) {
            for (DataItem dataItem : rawContact.getDataItems()) {
                dataItem.setRawContactId(rawContact.getId().longValue());
                String mimeType = dataItem.getMimeType();
                if (mimeType != null) {
                    AccountType accountType = rawContact.getAccountType(this);
                    DataKind dataKind = AccountTypeManager.getInstance(this).getKindOrFallback(accountType, mimeType);
                    if (dataKind != null) {
                        dataItem.setDataKind(dataKind);
                        boolean hasData = !TextUtils.isEmpty(dataItem.buildDataString(this, dataKind));
                        if (!isMimeExcluded(mimeType) && hasData) {
                            List<DataItem> dataItemListByType = dataItemsMap.get(mimeType);
                            if (dataItemListByType == null) {
                                dataItemListByType = new ArrayList<>();
                                dataItemsMap.put(mimeType, dataItemListByType);
                            }
                            dataItemListByType.add(dataItem);
                        }
                    }
                }
            }
        }
        Trace.endSection();
        Trace.beginSection("sort within mimetypes");
        List<List<DataItem>> dataItemsList = new ArrayList<>();
        for (List<DataItem> mimeTypeDataItems : dataItemsMap.values()) {
            Collapser.collapseList(mimeTypeDataItems, this);
            Collections.sort(mimeTypeDataItems, this.mWithinMimeTypeDataItemComparator);
            dataItemsList.add(mimeTypeDataItems);
        }
        Trace.endSection();
        Trace.beginSection("sort amongst mimetypes");
        Collections.sort(dataItemsList, this.mAmongstMimeTypeDataItemComparator);
        Trace.endSection();
        Trace.beginSection("cp2 data items to entries");
        List<List<ExpandingEntryCardView.Entry>> contactCardEntries = new ArrayList<>();
        List<List<ExpandingEntryCardView.Entry>> aboutCardEntries = buildAboutCardEntries(dataItemsMap);
        MutableString aboutCardName = new MutableString();
        for (int i = 0; i < dataItemsList.size(); i++) {
            List<DataItem> dataItemsByMimeType = dataItemsList.get(i);
            DataItem topDataItem = dataItemsByMimeType.get(0);
            if (!SORTED_ABOUT_CARD_MIMETYPES.contains(topDataItem.getMimeType())) {
                List<ExpandingEntryCardView.Entry> contactEntries = dataItemsToEntries(dataItemsList.get(i), aboutCardName);
                if (contactEntries.size() > 0) {
                    contactCardEntries.add(contactEntries);
                }
            }
        }
        Trace.endSection();
        Cp2DataCardModel dataModel = new Cp2DataCardModel();
        dataModel.customAboutCardName = aboutCardName.value;
        dataModel.aboutCardEntries = aboutCardEntries;
        dataModel.contactCardEntries = contactCardEntries;
        dataModel.dataItemsMap = dataItemsMap;
        return dataModel;
    }

    private static class Cp2DataCardModel {
        public List<List<ExpandingEntryCardView.Entry>> aboutCardEntries;
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

    private static ExpandingEntryCardView.Entry dataItemToEntry(DataItem dataItem, DataItem secondDataItem, Context context, Contact contactData, MutableString aboutCardName) {
        String text;
        Intent intent;
        String mimetype;
        WebAddress webAddress;
        int protocol;
        String subHeader;
        Drawable icon = null;
        String header = null;
        String subHeader2 = null;
        String text2 = null;
        StringBuilder primaryContentDescription = new StringBuilder();
        Intent intent2 = null;
        boolean shouldApplyColor = true;
        Drawable alternateIcon = null;
        Intent alternateIntent = null;
        StringBuilder alternateContentDescription = new StringBuilder();
        ExpandingEntryCardView.EntryContextMenuInfo entryContextMenuInfo = null;
        Drawable thirdIcon = null;
        Intent thirdIntent = null;
        String thirdContentDescription = null;
        int iconResourceId = 0;
        Context context2 = context.getApplicationContext();
        Resources res = context2.getResources();
        DataKind kind = dataItem.getDataKind();
        if (dataItem instanceof ImDataItem) {
            ImDataItem im = (ImDataItem) dataItem;
            intent = (Intent) ContactsUtils.buildImIntent(context2, im).first;
            boolean isEmail = im.isCreatedFromEmail();
            if (!im.isProtocolValid()) {
                protocol = -1;
            } else {
                protocol = isEmail ? 5 : im.getProtocol().intValue();
            }
            if (protocol == -1) {
                header = res.getString(R.string.header_im_entry);
                String subHeader3 = ContactsContract.CommonDataKinds.Im.getProtocolLabel(res, protocol, im.getCustomProtocol()).toString();
                String text3 = im.getData();
                text2 = text3;
                subHeader = subHeader3;
            } else {
                header = ContactsContract.CommonDataKinds.Im.getProtocolLabel(res, protocol, im.getCustomProtocol()).toString();
                subHeader = im.getData();
            }
            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(im.getData(), header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            text = text2;
            subHeader2 = subHeader;
        } else if (dataItem instanceof OrganizationDataItem) {
            OrganizationDataItem organization = (OrganizationDataItem) dataItem;
            header = res.getString(R.string.header_organization_entry);
            subHeader2 = organization.getCompany();
            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(subHeader2, header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            text = organization.getTitle();
            intent = null;
        } else if (dataItem instanceof NicknameDataItem) {
            NicknameDataItem nickname = (NicknameDataItem) dataItem;
            boolean isNameRawContact = contactData.getNameRawContactId() == dataItem.getRawContactId().longValue();
            boolean duplicatesTitle = isNameRawContact && contactData.getDisplayNameSource() == 35;
            if (!duplicatesTitle) {
                header = res.getString(R.string.header_nickname_entry);
                subHeader2 = nickname.getName();
                entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(subHeader2, header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            }
            intent = null;
            text = null;
        } else if (dataItem instanceof NoteDataItem) {
            NoteDataItem note = (NoteDataItem) dataItem;
            header = res.getString(R.string.header_note_entry);
            subHeader2 = note.getNote();
            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(subHeader2, header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            intent = null;
            text = null;
        } else if (dataItem instanceof WebsiteDataItem) {
            WebsiteDataItem website = (WebsiteDataItem) dataItem;
            header = res.getString(R.string.header_website_entry);
            subHeader2 = website.getUrl();
            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(subHeader2, header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            try {
                webAddress = new WebAddress(website.buildDataString(context2, kind));
            } catch (ParseException e) {
                Log.e("QuickContact", "Couldn't parse website: " + website.buildDataString(context2, kind));
            }
            if (BenesseExtension.getDchaState() != 0) {
                intent = null;
            } else {
                intent = new Intent("android.intent.action.VIEW", Uri.parse(webAddress.toString()));
            }
            text = null;
        } else if (dataItem instanceof EventDataItem) {
            EventDataItem event = (EventDataItem) dataItem;
            String dataString = event.buildDataString(context2, kind);
            Calendar cal = DateUtils.parseDate(dataString, false);
            if (cal != null) {
                Date nextAnniversary = DateUtils.getNextAnnualDate(cal);
                Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                ContentUris.appendId(builder, nextAnniversary.getTime());
                intent2 = new Intent("android.intent.action.VIEW").setData(builder.build());
            }
            header = res.getString(R.string.header_event_entry);
            if (event.hasKindTypeColumn(kind)) {
                subHeader2 = ContactsContract.CommonDataKinds.Event.getTypeLabel(res, event.getKindTypeColumn(kind), event.getLabel()).toString();
            }
            text = DateUtils.formatDate(context2, dataString);
            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(text, header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            intent = intent2;
        } else if (dataItem instanceof RelationDataItem) {
            RelationDataItem relation = (RelationDataItem) dataItem;
            String dataString2 = relation.buildDataString(context2, kind);
            if (!TextUtils.isEmpty(dataString2)) {
                Intent intent3 = new Intent("android.intent.action.SEARCH");
                intent3.putExtra("query", dataString2);
                intent3.setType("vnd.android.cursor.dir/contact");
                intent2 = intent3;
            }
            header = res.getString(R.string.header_relation_entry);
            subHeader2 = relation.getName();
            entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(subHeader2, header, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            if (!relation.hasKindTypeColumn(kind)) {
                text = null;
            } else {
                text = ContactsContract.CommonDataKinds.Relation.getTypeLabel(res, relation.getKindTypeColumn(kind), relation.getLabel()).toString();
            }
            intent = intent2;
        } else if (dataItem instanceof PhoneDataItem) {
            PhoneDataItem phone = (PhoneDataItem) dataItem;
            if (TextUtils.isEmpty(phone.getNumber())) {
                intent = null;
                text = null;
            } else {
                primaryContentDescription.append(res.getString(R.string.call_other)).append(" ");
                header = sBidiFormatter.unicodeWrap(phone.buildDataString(context2, kind), TextDirectionHeuristics.LTR);
                entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(header, res.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                if (!phone.hasKindTypeColumn(kind)) {
                    text = null;
                } else {
                    text = ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, phone.getKindTypeColumn(kind), phone.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                icon = res.getDrawable(R.drawable.ic_phone_24dp);
                iconResourceId = R.drawable.ic_phone_24dp;
                if (!PhoneCapabilityTester.isPhone(context2)) {
                    intent = null;
                } else {
                    intent = CallUtil.getCallIntent(phone.getNumber());
                }
                if (TelephonyManagerUtils.isSmsCapable(context2)) {
                    alternateIntent = new Intent("android.intent.action.SENDTO", Uri.fromParts("smsto", phone.getNumber(), null));
                    alternateIcon = res.getDrawable(R.drawable.ic_message_24dp);
                    alternateContentDescription.append(res.getString(R.string.sms_custom, header));
                }
                if (CallUtil.isVideoEnabled(context2)) {
                    thirdIcon = res.getDrawable(R.drawable.ic_videocam);
                    thirdIntent = CallUtil.getVideoCallIntent(phone.getNumber(), "com.android.contacts.quickcontact.QuickContactActivity");
                    thirdContentDescription = res.getString(R.string.description_video_call);
                }
            }
        } else if (dataItem instanceof EmailDataItem) {
            EmailDataItem email = (EmailDataItem) dataItem;
            String address = email.getData();
            if (!TextUtils.isEmpty(address)) {
                primaryContentDescription.append(res.getString(R.string.email_other)).append(" ");
                Uri mailUri = Uri.fromParts("mailto", address, null);
                Intent intent4 = new Intent("android.intent.action.SENDTO", mailUri);
                header = email.getAddress();
                entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(header, res.getString(R.string.emailLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                if (!email.hasKindTypeColumn(kind)) {
                    text = null;
                } else {
                    text = ContactsContract.CommonDataKinds.Email.getTypeLabel(res, email.getKindTypeColumn(kind), email.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                icon = res.getDrawable(R.drawable.ic_email_24dp);
                iconResourceId = R.drawable.ic_email_24dp;
                intent = intent4;
            }
        } else if (dataItem instanceof StructuredPostalDataItem) {
            StructuredPostalDataItem postal = (StructuredPostalDataItem) dataItem;
            String postalAddress = postal.getFormattedAddress();
            if (!TextUtils.isEmpty(postalAddress)) {
                primaryContentDescription.append(res.getString(R.string.map_other)).append(" ");
                Intent intent5 = StructuredPostalUtils.getViewPostalAddressIntent(postalAddress);
                header = postal.getFormattedAddress();
                entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(header, res.getString(R.string.postalLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                if (!postal.hasKindTypeColumn(kind)) {
                    text = null;
                } else {
                    text = ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(res, postal.getKindTypeColumn(kind), postal.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                alternateIntent = StructuredPostalUtils.getViewPostalAddressDirectionsIntent(postalAddress);
                alternateIcon = res.getDrawable(R.drawable.ic_directions_24dp);
                alternateContentDescription.append(res.getString(R.string.content_description_directions)).append(" ").append(header);
                icon = res.getDrawable(R.drawable.ic_place_24dp);
                iconResourceId = R.drawable.ic_place_24dp;
                intent = intent5;
            }
        } else if (dataItem instanceof SipAddressDataItem) {
            SipAddressDataItem sip = (SipAddressDataItem) dataItem;
            String address2 = sip.getSipAddress();
            if (!TextUtils.isEmpty(address2)) {
                primaryContentDescription.append(res.getString(R.string.call_other)).append(" ");
                if (PhoneCapabilityTester.isSipPhone(context2)) {
                    Uri callUri = Uri.fromParts("sip", address2, null);
                    intent2 = CallUtil.getCallIntent(callUri);
                }
                header = address2;
                entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(header, res.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                if (!sip.hasKindTypeColumn(kind)) {
                    text = null;
                } else {
                    text = ContactsContract.CommonDataKinds.SipAddress.getTypeLabel(res, sip.getKindTypeColumn(kind), sip.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                icon = res.getDrawable(R.drawable.ic_dialer_sip_black_24dp);
                iconResourceId = R.drawable.ic_dialer_sip_black_24dp;
                intent = intent2;
            }
        } else if (dataItem instanceof StructuredNameDataItem) {
            String givenName = ((StructuredNameDataItem) dataItem).getGivenName();
            if (!TextUtils.isEmpty(givenName)) {
                aboutCardName.value = res.getString(R.string.about_card_title) + " " + givenName;
            } else {
                aboutCardName.value = res.getString(R.string.about_card_title);
            }
            intent = null;
            text = null;
        } else {
            header = dataItem.buildDataStringForDisplay(context2, kind);
            text = kind.typeColumn;
            intent = new Intent("android.intent.action.VIEW");
            Uri uri = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataItem.getId());
            intent.setDataAndType(uri, dataItem.getMimeType());
            if (intent != null) {
                mimetype = intent.getType();
                switch (mimetype) {
                    case "vnd.android.cursor.item/vnd.googleplus.profile":
                        if (secondDataItem != null) {
                            Drawable icon2 = res.getDrawable(R.drawable.ic_google_plus_24dp);
                            alternateIcon = res.getDrawable(R.drawable.ic_add_to_circles_black_24);
                            GPlusOrHangoutsDataItemModel itemModel = new GPlusOrHangoutsDataItemModel(intent, null, dataItem, secondDataItem, alternateContentDescription, header, text, context2);
                            populateGPlusOrHangoutsDataItemModel(itemModel);
                            intent = itemModel.intent;
                            alternateIntent = itemModel.alternateIntent;
                            alternateContentDescription = itemModel.alternateContentDescription;
                            header = itemModel.header;
                            text = itemModel.text;
                            icon = icon2;
                            break;
                        } else {
                            if ("addtocircle".equals(intent.getDataString())) {
                                icon = res.getDrawable(R.drawable.ic_add_to_circles_black_24);
                            } else {
                                icon = res.getDrawable(R.drawable.ic_google_plus_24dp);
                            }
                            break;
                        }
                        break;
                    case "vnd.android.cursor.item/vnd.googleplus.profile.comm":
                        if (secondDataItem != null) {
                            Drawable icon3 = res.getDrawable(R.drawable.ic_hangout_24dp);
                            alternateIcon = res.getDrawable(R.drawable.ic_hangout_video_24dp);
                            GPlusOrHangoutsDataItemModel itemModel2 = new GPlusOrHangoutsDataItemModel(intent, null, dataItem, secondDataItem, alternateContentDescription, header, text, context2);
                            populateGPlusOrHangoutsDataItemModel(itemModel2);
                            intent = itemModel2.intent;
                            alternateIntent = itemModel2.alternateIntent;
                            alternateContentDescription = itemModel2.alternateContentDescription;
                            header = itemModel2.header;
                            text = itemModel2.text;
                            icon = icon3;
                            break;
                        } else {
                            if ("hangout".equals(intent.getDataString())) {
                                icon = res.getDrawable(R.drawable.ic_hangout_video_24dp);
                            } else {
                                icon = res.getDrawable(R.drawable.ic_hangout_24dp);
                            }
                            break;
                        }
                        break;
                    default:
                        entryContextMenuInfo = new ExpandingEntryCardView.EntryContextMenuInfo(header, mimetype, dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
                        icon = ResolveCache.getInstance(context2).getIcon(dataItem.getMimeType(), intent);
                        if (icon != null) {
                            icon.mutate();
                        }
                        shouldApplyColor = false;
                        break;
                }
            }
        }
        if (intent != null && !PhoneCapabilityTester.isIntentRegistered(context2, intent)) {
            intent = null;
        }
        if (alternateIntent != null) {
            if (!PhoneCapabilityTester.isIntentRegistered(context2, alternateIntent)) {
                alternateIntent = null;
            } else if (TextUtils.isEmpty(alternateContentDescription)) {
                alternateContentDescription.append(getIntentResolveLabel(alternateIntent, context2));
            }
        }
        if (icon == null && TextUtils.isEmpty(header) && TextUtils.isEmpty(subHeader2) && 0 == 0 && TextUtils.isEmpty(text) && 0 == 0) {
            return null;
        }
        int dataId = dataItem.getId() > 2147483647L ? -1 : (int) dataItem.getId();
        return new ExpandingEntryCardView.Entry(dataId, icon, header, subHeader2, null, text, null, new SpannableString(primaryContentDescription.toString()), intent, alternateIcon, alternateIntent, alternateContentDescription.toString(), shouldApplyColor, false, entryContextMenuInfo, thirdIcon, thirdIntent, thirdContentDescription, iconResourceId);
    }

    private List<ExpandingEntryCardView.Entry> dataItemsToEntries(List<DataItem> dataItems, MutableString aboutCardTitleOut) {
        if (dataItems.get(0).getMimeType().equals("vnd.android.cursor.item/vnd.googleplus.profile") || dataItems.get(0).getMimeType().equals("vnd.android.cursor.item/vnd.googleplus.profile.comm")) {
            return gPlusOrHangoutsDataItemsToEntries(dataItems);
        }
        List<ExpandingEntryCardView.Entry> entries = new ArrayList<>();
        for (DataItem dataItem : dataItems) {
            ExpandingEntryCardView.Entry entry = dataItemToEntry(dataItem, null, this, this.mContactData, aboutCardTitleOut);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private List<ExpandingEntryCardView.Entry> gPlusOrHangoutsDataItemsToEntries(List<DataItem> dataItems) {
        List<ExpandingEntryCardView.Entry> entries = new ArrayList<>();
        Map<Long, List<DataItem>> buckets = new HashMap<>();
        for (DataItem dataItem : dataItems) {
            List<DataItem> bucket = buckets.get(dataItem.getRawContactId());
            if (bucket == null) {
                bucket = new ArrayList<>();
                buckets.put(dataItem.getRawContactId(), bucket);
            }
            bucket.add(dataItem);
        }
        for (List<DataItem> bucket2 : buckets.values()) {
            if (bucket2.size() == 2) {
                ExpandingEntryCardView.Entry entry = dataItemToEntry(bucket2.get(0), bucket2.get(1), this, this.mContactData, null);
                if (entry != null) {
                    entries.add(entry);
                }
            } else {
                Iterator<DataItem> it = bucket2.iterator();
                while (it.hasNext()) {
                    ExpandingEntryCardView.Entry entry2 = dataItemToEntry(it.next(), null, this, this.mContactData, null);
                    if (entry2 != null) {
                        entries.add(entry2);
                    }
                }
            }
        }
        return entries;
    }

    private static final class GPlusOrHangoutsDataItemModel {
        public StringBuilder alternateContentDescription;
        public Intent alternateIntent;
        public Context context;
        public DataItem dataItem;
        public String header;
        public Intent intent;
        public DataItem secondDataItem;
        public String text;

        public GPlusOrHangoutsDataItemModel(Intent intent, Intent alternateIntent, DataItem dataItem, DataItem secondDataItem, StringBuilder alternateContentDescription, String header, String text, Context context) {
            this.intent = intent;
            this.alternateIntent = alternateIntent;
            this.dataItem = dataItem;
            this.secondDataItem = secondDataItem;
            this.alternateContentDescription = alternateContentDescription;
            this.header = header;
            this.text = text;
            this.context = context;
        }
    }

    private static void populateGPlusOrHangoutsDataItemModel(GPlusOrHangoutsDataItemModel dataModel) {
        Intent secondIntent = new Intent("android.intent.action.VIEW");
        secondIntent.setDataAndType(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataModel.secondDataItem.getId()), dataModel.secondDataItem.getMimeType());
        if ("hangout".equals(dataModel.dataItem.getContentValues().getAsString("data5")) || "addtocircle".equals(dataModel.dataItem.getContentValues().getAsString("data5"))) {
            dataModel.alternateIntent = dataModel.intent;
            dataModel.alternateContentDescription = new StringBuilder(dataModel.header);
            dataModel.intent = secondIntent;
            dataModel.header = dataModel.secondDataItem.buildDataStringForDisplay(dataModel.context, dataModel.secondDataItem.getDataKind());
            dataModel.text = dataModel.secondDataItem.getDataKind().typeColumn;
            return;
        }
        if ("conversation".equals(dataModel.dataItem.getContentValues().getAsString("data5")) || "view".equals(dataModel.dataItem.getContentValues().getAsString("data5"))) {
            dataModel.alternateIntent = secondIntent;
            dataModel.alternateContentDescription = new StringBuilder(dataModel.secondDataItem.buildDataStringForDisplay(dataModel.context, dataModel.secondDataItem.getDataKind()));
        }
    }

    private static String getIntentResolveLabel(Intent intent, Context context) {
        List<ResolveInfo> matches = context.getPackageManager().queryIntentActivities(intent, 65536);
        ResolveInfo bestResolve = null;
        int size = matches.size();
        if (size == 1) {
            ResolveInfo bestResolve2 = matches.get(0);
            bestResolve = bestResolve2;
        } else if (size > 1) {
            bestResolve = ResolveCache.getInstance(context).getBestResolve(intent, matches);
        }
        if (bestResolve == null) {
            return null;
        }
        return String.valueOf(bestResolve.loadLabel(context.getPackageManager()));
    }

    private void extractAndApplyTintFromPhotoViewAsynchronously() {
        if (this.mScroller != null) {
            final Drawable imageViewDrawable = this.mPhotoView.getDrawable();
            new AsyncTask<Void, Void, MaterialColorMapUtils.MaterialPalette>() {
                @Override
                protected MaterialColorMapUtils.MaterialPalette doInBackground(Void... params) {
                    if ((imageViewDrawable instanceof BitmapDrawable) && QuickContactActivity.this.mContactData != null && QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData() != null && QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData().length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData(), 0, QuickContactActivity.this.mContactData.getThumbnailPhotoBinaryData().length);
                        try {
                            int primaryColor = QuickContactActivity.this.colorFromBitmap(bitmap);
                            if (primaryColor != 0) {
                                return QuickContactActivity.this.mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(primaryColor);
                            }
                        } finally {
                            bitmap.recycle();
                        }
                    }
                    if (imageViewDrawable instanceof LetterTileDrawable) {
                        return QuickContactActivity.this.mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(((LetterTileDrawable) imageViewDrawable).getColor());
                    }
                    return MaterialColorMapUtils.getDefaultPrimaryAndSecondaryColors(QuickContactActivity.this.getResources());
                }

                @Override
                protected void onPostExecute(MaterialColorMapUtils.MaterialPalette palette) {
                    super.onPostExecute(palette);
                    if (!QuickContactActivity.this.mHasComputedThemeColor && imageViewDrawable == QuickContactActivity.this.mPhotoView.getDrawable()) {
                        QuickContactActivity.this.mHasComputedThemeColor = true;
                        QuickContactActivity.this.setThemeColor(palette);
                    }
                }
            }.execute(new Void[0]);
        }
    }

    private void setThemeColor(MaterialColorMapUtils.MaterialPalette palette) {
        int primaryColor = palette.mPrimaryColor;
        this.mScroller.setHeaderTintColor(primaryColor);
        this.mStatusBarColor = palette.mSecondaryColor;
        updateStatusBarColor();
        this.mColorFilter = new PorterDuffColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
        this.mContactCard.setColorAndFilter(primaryColor, this.mColorFilter);
        this.mRecentCard.setColorAndFilter(primaryColor, this.mColorFilter);
        this.mAboutCard.setColorAndFilter(primaryColor, this.mColorFilter);
    }

    private void updateStatusBarColor() {
        int desiredStatusBarColor;
        if (this.mScroller != null) {
            if (this.mScroller.getScrollNeededToBeFullScreen() <= 0) {
                desiredStatusBarColor = this.mStatusBarColor;
            } else {
                desiredStatusBarColor = 0;
            }
            ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor", getWindow().getStatusBarColor(), desiredStatusBarColor);
            animation.setDuration(150L);
            animation.setEvaluator(new ArgbEvaluator());
            animation.start();
        }
    }

    private int colorFromBitmap(Bitmap bitmap) {
        Palette palette = Palette.generate(bitmap, 24);
        if (palette == null || palette.getVibrantSwatch() == null) {
            return 0;
        }
        return palette.getVibrantSwatch().getRgb();
    }

    private List<ExpandingEntryCardView.Entry> contactInteractionsToEntries(List<ContactInteraction> interactions) {
        List<ExpandingEntryCardView.Entry> entries = new ArrayList<>();
        for (ContactInteraction interaction : interactions) {
            if (interaction != null) {
                entries.add(new ExpandingEntryCardView.Entry(-1, interaction.getIcon(this), interaction.getViewHeader(this), interaction.getViewBody(this), interaction.getBodyIcon(this), interaction.getViewFooter(this), interaction.getFooterIcon(this), interaction.getContentDescription(this), interaction.getIntent(), null, null, null, true, false, null, null, null, null, interaction.getIconResourceId()));
            }
        }
        return entries;
    }

    @Override
    public void onBackPressed() {
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
        final List<ContactInteraction> allInteractions = new ArrayList<>();
        final List<List<ExpandingEntryCardView.Entry>> interactionsWrapper = new ArrayList<>();
        for (List<ContactInteraction> loaderInteractions : this.mRecentLoaderResults.values()) {
            allInteractions.addAll(loaderInteractions);
        }
        this.mRecentDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Trace.beginSection("sort recent loader results");
                Collections.sort(allInteractions, new Comparator<ContactInteraction>() {
                    @Override
                    public int compare(ContactInteraction a, ContactInteraction b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return 1;
                        }
                        if (b != null && a.getInteractionDate() <= b.getInteractionDate()) {
                            return a.getInteractionDate() != b.getInteractionDate() ? 1 : 0;
                        }
                        return -1;
                    }
                });
                Trace.endSection();
                Trace.beginSection("contactInteractionsToEntries");
                for (ExpandingEntryCardView.Entry contactInteraction : QuickContactActivity.this.contactInteractionsToEntries(allInteractions)) {
                    ArrayList arrayList = new ArrayList(1);
                    arrayList.add(contactInteraction);
                    interactionsWrapper.add(arrayList);
                }
                Trace.endSection();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Trace.beginSection("initialize recents card");
                if (allInteractions.size() > 0) {
                    QuickContactActivity.this.mRecentCard.initialize(interactionsWrapper, 3, QuickContactActivity.this.mRecentCard.isExpanded(), false, QuickContactActivity.this.mExpandingEntryCardViewListener, QuickContactActivity.this.mScroller);
                    QuickContactActivity.this.mRecentCard.setVisibility(0);
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
        this.mRecentDataTask.execute(new Void[0]);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.mEntriesAndActionsTask != null) {
            this.mEntriesAndActionsTask.cancel(false);
        }
        if (this.mRecentDataTask != null) {
            this.mRecentDataTask.cancel(false);
        }
    }

    private boolean isContactEditable() {
        return (this.mContactData == null || this.mContactData.isDirectoryEntry()) ? false : true;
    }

    private boolean isContactShareable() {
        return (this.mContactData == null || this.mContactData.isDirectoryEntry()) ? false : true;
    }

    private Intent getEditContactIntent() {
        Intent intent = new Intent("android.intent.action.EDIT", this.mContactData.getLookupUri());
        intent.addFlags(524288);
        return intent;
    }

    private void editContact() {
        this.mHasIntentLaunched = true;
        this.mContactLoader.cacheResult();
        startActivityForResult(getEditContactIntent(), 1);
    }

    private void deleteContact() {
        Uri contactUri = this.mContactData.getLookupUri();
        ContactDeletionInteraction.start(this, contactUri, true);
    }

    private void toggleStar(MenuItem starredMenuItem) {
        if (this.mContactData != null) {
            boolean isStarred = starredMenuItem.isChecked();
            ContactDisplayUtils.configureStarredMenuItem(starredMenuItem, this.mContactData.isDirectoryEntry(), this.mContactData.isUserProfile(), !isStarred);
            Intent intent = ContactSaveService.createSetStarredIntent(this, this.mContactData.getLookupUri(), isStarred ? false : true);
            startService(intent);
            CharSequence accessibilityText = !isStarred ? getResources().getText(R.string.description_action_menu_add_star) : getResources().getText(R.string.description_action_menu_remove_star);
            this.mScroller.announceForAccessibility(accessibilityText);
        }
    }

    private Uri getPreAuthorizedUri(Uri uri) {
        Bundle uriBundle = new Bundle();
        uriBundle.putParcelable("uri_to_authorize", uri);
        Bundle authResponse = getContentResolver().call(ContactsContract.AUTHORITY_URI, "authorize", (String) null, uriBundle);
        return authResponse != null ? (Uri) authResponse.getParcelable("authorized_uri") : uri;
    }

    private void shareContact() {
        String lookupKey = this.mContactData.getLookupKey();
        Uri shareUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);
        if (this.mContactData.isUserProfile()) {
            shareUri = getPreAuthorizedUri(shareUri);
        }
        Intent intent = new Intent("android.intent.action.SEND");
        intent.setType("text/x-vcard");
        intent.putExtra("android.intent.extra.STREAM", shareUri);
        CharSequence chooseTitle = getText(R.string.share_via);
        Intent chooseIntent = Intent.createChooser(intent, chooseTitle);
        try {
            this.mHasIntentLaunched = true;
            startActivity(chooseIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.share_error, 0).show();
        }
    }

    private void createLauncherShortcutWithContact() {
        ShortcutIntentBuilder builder = new ShortcutIntentBuilder(this, new ShortcutIntentBuilder.OnShortcutIntentCreatedListener() {
            @Override
            public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
                shortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                QuickContactActivity.this.sendBroadcast(shortcutIntent);
                Toast.makeText(QuickContactActivity.this, R.string.createContactShortcutSuccessful, 0).show();
            }
        });
        builder.createContactShortcutIntent(this.mContactData.getLookupUri());
    }

    private boolean isShortcutCreatable() {
        if (this.mContactData == null || this.mContactData.isUserProfile()) {
            return false;
        }
        Intent createShortcutIntent = new Intent();
        createShortcutIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        List<ResolveInfo> receivers = getPackageManager().queryBroadcastReceivers(createShortcutIntent, 0);
        return receivers != null && receivers.size() > 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quickcontact, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mContactData == null) {
            return false;
        }
        MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
        ContactDisplayUtils.configureStarredMenuItem(starredMenuItem, this.mContactData.isDirectoryEntry(), this.mContactData.isUserProfile(), this.mContactData.getStarred());
        MenuItem editMenuItem = menu.findItem(R.id.menu_edit);
        editMenuItem.setVisible(true);
        if (DirectoryContactUtil.isDirectoryContact(this.mContactData) || InvisibleContactUtil.isInvisibleAndAddable(this.mContactData, this)) {
            editMenuItem.setIcon(R.drawable.ic_person_add_tinted_24dp);
            editMenuItem.setTitle(R.string.menu_add_contact);
        } else if (isContactEditable()) {
            editMenuItem.setIcon(R.drawable.ic_create_24dp);
            editMenuItem.setTitle(R.string.menu_editContact);
        } else {
            editMenuItem.setVisible(false);
        }
        MenuItem deleteMenuItem = menu.findItem(R.id.menu_delete);
        deleteMenuItem.setVisible(isContactEditable());
        MenuItem shareMenuItem = menu.findItem(R.id.menu_share);
        shareMenuItem.setVisible(isContactShareable());
        MenuItem shortcutMenuItem = menu.findItem(R.id.menu_create_contact_shortcut);
        shortcutMenuItem.setVisible(isShortcutCreatable());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                deleteContact();
                return true;
            case R.id.menu_star:
                toggleStar(item);
                return true;
            case R.id.menu_edit:
                if (DirectoryContactUtil.isDirectoryContact(this.mContactData)) {
                    Intent intent = new Intent("android.intent.action.INSERT_OR_EDIT");
                    intent.setType("vnd.android.cursor.item/contact");
                    ArrayList<ContentValues> values = this.mContactData.getContentValues();
                    if (this.mContactData.getDisplayNameSource() >= 35) {
                        intent.putExtra("name", this.mContactData.getDisplayName());
                    } else if (this.mContactData.getDisplayNameSource() == 30) {
                        ContentValues organization = new ContentValues();
                        organization.put("data1", this.mContactData.getDisplayName());
                        organization.put("mimetype", "vnd.android.cursor.item/organization");
                        values.add(organization);
                    }
                    for (ContentValues value : values) {
                        value.remove("last_time_used");
                        value.remove("times_used");
                    }
                    intent.putExtra("data", values);
                    if (this.mContactData.getDirectoryExportSupport() == 1) {
                        intent.putExtra("com.android.contacts.extra.ACCOUNT", new Account(this.mContactData.getDirectoryAccountName(), this.mContactData.getDirectoryAccountType()));
                        intent.putExtra("com.android.contacts.extra.DATA_SET", this.mContactData.getRawContacts().get(0).getDataSet());
                    }
                    intent.putExtra("disableDeleteMenuOption", true);
                    startActivityForResult(intent, 2);
                } else if (InvisibleContactUtil.isInvisibleAndAddable(this.mContactData, this)) {
                    InvisibleContactUtil.addToDefaultGroup(this.mContactData, this);
                } else if (isContactEditable()) {
                    editContact();
                }
                return true;
            case R.id.menu_share:
                if (isContactShareable()) {
                    shareContact();
                }
                return true;
            case R.id.menu_create_contact_shortcut:
                createLauncherShortcutWithContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
