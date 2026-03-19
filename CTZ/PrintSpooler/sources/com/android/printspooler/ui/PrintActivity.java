package com.android.printspooler.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserManager;
import android.print.IPrintDocumentAdapter;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.print.PrintServicesLoader;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.PrintServiceInfo;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.printspooler.R;
import com.android.printspooler.model.MutexFileProvider;
import com.android.printspooler.model.PrintSpoolerProvider;
import com.android.printspooler.model.PrintSpoolerService;
import com.android.printspooler.model.RemotePrintDocument;
import com.android.printspooler.renderer.IPdfEditor;
import com.android.printspooler.renderer.PdfManipulationService;
import com.android.printspooler.ui.PageAdapter;
import com.android.printspooler.ui.PrintErrorFragment;
import com.android.printspooler.ui.PrinterRegistry;
import com.android.printspooler.util.ApprovedPrintServices;
import com.android.printspooler.util.MediaSizeUtils;
import com.android.printspooler.util.PageRangeUtils;
import com.android.printspooler.widget.ClickInterceptSpinner;
import com.android.printspooler.widget.PrintContentView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class PrintActivity extends Activity implements LoaderManager.LoaderCallbacks<List<PrintServiceInfo>>, RemotePrintDocument.UpdateResultCallbacks, PageAdapter.ContentCallbacks, PrintErrorFragment.OnActionListener, PrintContentView.OptionsStateChangeListener, PrintContentView.OptionsStateController {
    private ComponentName mAdvancedPrintOptionsActivity;
    private boolean mArePrintServicesEnabled;
    private String mCallingPackageName;
    private Spinner mColorModeSpinner;
    private ArrayAdapter<SpinnerItem<Integer>> mColorModeSpinnerAdapter;
    private EditText mCopiesEditText;
    private int mCurrentPageCount;
    private PrinterInfo mCurrentPrinter;
    private PrinterId mDefaultPrinter;
    private ClickInterceptSpinner mDestinationSpinner;
    private DestinationAdapter mDestinationSpinnerAdapter;
    private Spinner mDuplexModeSpinner;
    private ArrayAdapter<SpinnerItem<Integer>> mDuplexModeSpinnerAdapter;
    private MutexFileProvider mFileProvider;
    private boolean mIsFinishing;
    private boolean mIsMoreOptionsActivityInProgress;
    private MediaSizeUtils.MediaSizeComparator mMediaSizeComparator;
    private Spinner mMediaSizeSpinner;
    private ArrayAdapter<SpinnerItem<PrintAttributes.MediaSize>> mMediaSizeSpinnerAdapter;
    private Button mMoreOptionsButton;
    private PrintContentView mOptionsContent;
    private Spinner mOrientationSpinner;
    private ArrayAdapter<SpinnerItem<Integer>> mOrientationSpinnerAdapter;
    private EditText mPageRangeEditText;
    private TextView mPageRangeTitle;
    private ImageView mPrintButton;
    private PrintJobInfo mPrintJob;
    private PrintPreviewController mPrintPreviewController;
    private RemotePrintDocument mPrintedDocument;
    private final PrinterAvailabilityDetector mPrinterAvailabilityDetector;
    private PrinterRegistry mPrinterRegistry;
    private PrintersObserver mPrintersObserver;
    private ProgressMessageController mProgressMessageController;
    private Spinner mRangeOptionsSpinner;
    private final View.OnFocusChangeListener mSelectAllOnFocusListener;
    private PageRange[] mSelectedPages;
    private boolean mShowDestinationPrompt;
    private PrintSpoolerProvider mSpoolerProvider;
    private View mSummaryContainer;
    private TextView mSummaryCopies;
    private TextView mSummaryPaperSize;
    private static final String MORE_OPTIONS_ACTIVITY_IN_PROGRESS_KEY = PrintActivity.class.getName() + ".MORE_OPTIONS_ACTIVITY_IN_PROGRESS";
    private static final String MIN_COPIES_STRING = String.valueOf(1);
    private boolean mIsOptionsUiBound = false;
    private int mState = 0;
    private int mUiState = 0;

    public PrintActivity() {
        this.mPrinterAvailabilityDetector = new PrinterAvailabilityDetector();
        this.mSelectAllOnFocusListener = new SelectAllOnFocusListener();
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        setTitle(R.string.print_dialog);
        Bundle extras = getIntent().getExtras();
        if (bundle != null) {
            this.mIsMoreOptionsActivityInProgress = bundle.getBoolean(MORE_OPTIONS_ACTIVITY_IN_PROGRESS_KEY);
        }
        this.mPrintJob = (PrintJobInfo) extras.getParcelable("android.print.intent.extra.EXTRA_PRINT_JOB");
        if (this.mPrintJob == null) {
            throw new IllegalArgumentException("android.print.intent.extra.EXTRA_PRINT_JOB cannot be null");
        }
        if (this.mPrintJob.getAttributes() == null) {
            this.mPrintJob.setAttributes(new PrintAttributes.Builder().build());
        }
        final IBinder binder = extras.getBinder("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER");
        if (binder == null) {
            throw new IllegalArgumentException("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER cannot be null");
        }
        this.mCallingPackageName = extras.getString("android.content.extra.PACKAGE_NAME");
        if (bundle == null) {
            MetricsLogger.action(this, 501, this.mCallingPackageName);
        }
        this.mSpoolerProvider = new PrintSpoolerProvider(this, new Runnable() {
            @Override
            public final void run() {
                PrintActivity.lambda$onCreate$0(this.f$0, bundle, binder);
            }
        });
        getLoaderManager().initLoader(1, null, this);
    }

    public static void lambda$onCreate$0(PrintActivity printActivity, Bundle bundle, IBinder iBinder) {
        if (printActivity.isFinishing() || printActivity.isDestroyed()) {
            if (bundle != null) {
                printActivity.mSpoolerProvider.getSpooler().setPrintJobState(printActivity.mPrintJob.getId(), 7, null);
            }
        } else {
            if (bundle == null) {
                printActivity.mSpoolerProvider.getSpooler().createPrintJob(printActivity.mPrintJob);
            }
            printActivity.onConnectedToPrintSpooler(iBinder);
        }
    }

    private void onConnectedToPrintSpooler(final IBinder iBinder) {
        this.mPrinterRegistry = new PrinterRegistry(this, new Runnable() {
            @Override
            public final void run() {
                PrintActivity printActivity = this.f$0;
                new Handler(printActivity.getMainLooper()).post(new Runnable() {
                    @Override
                    public final void run() {
                        printActivity.onPrinterRegistryReady(iBinder);
                    }
                });
            }
        }, 2, 3);
    }

    private void onPrinterRegistryReady(IBinder iBinder) {
        setContentView(R.layout.print_activity);
        try {
            this.mFileProvider = new MutexFileProvider(PrintSpoolerService.generateFileForPrintJob(this, this.mPrintJob.getId()));
            this.mPrintPreviewController = new PrintPreviewController(this, this.mFileProvider);
            this.mPrintedDocument = new RemotePrintDocument(this, IPrintDocumentAdapter.Stub.asInterface(iBinder), this.mFileProvider, new RemotePrintDocument.RemoteAdapterDeathObserver() {
                @Override
                public void onDied() {
                    Log.w("PrintActivity", "Printing app died unexpectedly");
                    if (!PrintActivity.this.isFinishing() && !PrintActivity.this.isDestroyed()) {
                        if (!PrintActivity.isFinalState(PrintActivity.this.mState) || PrintActivity.this.mPrintedDocument.isUpdating()) {
                            PrintActivity.this.setState(3);
                            PrintActivity.this.mPrintedDocument.cancel(true);
                            PrintActivity.this.doFinish();
                        }
                    }
                }
            }, this);
            this.mProgressMessageController = new ProgressMessageController(this);
            this.mMediaSizeComparator = new MediaSizeUtils.MediaSizeComparator(this);
            this.mDestinationSpinnerAdapter = new DestinationAdapter();
            bindUi();
            updateOptionsUi();
            this.mOptionsContent.setVisibility(0);
            this.mSelectedPages = computeSelectedPages();
            this.mPrintedDocument.start();
            ensurePreviewUiShown();
            setState(1);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create print job file", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.mPrinterRegistry != null && this.mCurrentPrinter != null) {
            this.mPrinterRegistry.setTrackedPrinter(this.mCurrentPrinter.getId());
        }
    }

    @Override
    public void onPause() {
        PrintSpoolerService spooler = this.mSpoolerProvider.getSpooler();
        if (this.mState == 0) {
            if (isFinishing() && spooler != null) {
                spooler.setPrintJobState(this.mPrintJob.getId(), 7, null);
            }
            super.onPause();
            return;
        }
        if (isFinishing()) {
            spooler.updatePrintJobUserConfigurableOptionsNoPersistence(this.mPrintJob);
            int i = this.mState;
            if (i == 5) {
                spooler.setPrintJobState(this.mPrintJob.getId(), 6, getString(R.string.print_write_error_message));
            } else if (i == 8) {
                if (this.mCurrentPrinter == this.mDestinationSpinnerAdapter.getPdfPrinter()) {
                    spooler.setPrintJobState(this.mPrintJob.getId(), 5, null);
                } else {
                    spooler.setPrintJobState(this.mPrintJob.getId(), 2, null);
                }
            } else {
                spooler.setPrintJobState(this.mPrintJob.getId(), 7, null);
            }
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(MORE_OPTIONS_ACTIVITY_IN_PROGRESS_KEY, this.mIsMoreOptionsActivityInProgress);
    }

    @Override
    protected void onStop() {
        this.mPrinterAvailabilityDetector.cancel();
        if (this.mPrinterRegistry != null) {
            this.mPrinterRegistry.setTrackedPrinter(null);
        }
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 4) {
            keyEvent.startTracking();
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mState == 0) {
            doFinish();
            return true;
        }
        if (this.mState == 3 || this.mState == 2 || this.mState == 8) {
            return true;
        }
        if (i == 4 && keyEvent.isTracking() && !keyEvent.isCanceled()) {
            if (this.mPrintPreviewController != null && this.mPrintPreviewController.isOptionsOpened() && !hasErrors()) {
                this.mPrintPreviewController.closeOptions();
            } else {
                cancelPrint();
            }
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public void onRequestContentUpdate() {
        if (canUpdateDocument()) {
            updateDocument(false);
        }
    }

    @Override
    public void onMalformedPdfFile() {
        onPrintDocumentError("Cannot print a malformed PDF file");
    }

    @Override
    public void onSecurePdfFile() {
        onPrintDocumentError("Cannot print a password protected PDF file");
    }

    private void onPrintDocumentError(String str) {
        setState(this.mProgressMessageController.cancel());
        ensureErrorUiShown(null, 1);
        setState(4);
        this.mPrintedDocument.kill(str);
    }

    @Override
    public void onActionPerformed() {
        if (this.mState == 4 && canUpdateDocument() && updateDocument(true)) {
            ensurePreviewUiShown();
            setState(1);
        }
    }

    @Override
    public void onUpdateCanceled() {
        setState(this.mProgressMessageController.cancel());
        ensurePreviewUiShown();
        int i = this.mState;
        if (i != 5 && i != 8) {
            switch (i) {
                case 2:
                    requestCreatePdfFileOrFinish();
                    break;
            }
        }
        doFinish();
    }

    @Override
    public void onUpdateCompleted(RemotePrintDocument.RemotePrintDocumentInfo remotePrintDocumentInfo) {
        setState(this.mProgressMessageController.cancel());
        ensurePreviewUiShown();
        PrintDocumentInfo printDocumentInfo = remotePrintDocumentInfo.info;
        if (printDocumentInfo != null) {
            PrintDocumentInfo printDocumentInfoBuild = new PrintDocumentInfo.Builder(printDocumentInfo.getName()).setContentType(printDocumentInfo.getContentType()).setPageCount(PageRangeUtils.getNormalizedPageCount(remotePrintDocumentInfo.pagesWrittenToFile, getAdjustedPageCount(printDocumentInfo))).build();
            try {
                printDocumentInfoBuild.setDataSize(this.mFileProvider.acquireFile(null).length());
                this.mFileProvider.releaseFile();
                this.mPrintJob.setDocumentInfo(printDocumentInfoBuild);
                this.mPrintJob.setPages(remotePrintDocumentInfo.pagesInFileToPrint);
            } catch (Throwable th) {
                this.mFileProvider.releaseFile();
                throw th;
            }
        }
        int i = this.mState;
        if (i != 5 && i != 8) {
            switch (i) {
                case 2:
                    requestCreatePdfFileOrFinish();
                    return;
                case 3:
                    break;
                default:
                    updatePrintPreviewController(remotePrintDocumentInfo.changed);
                    setState(1);
                    return;
            }
        }
        updateOptionsUi();
        doFinish();
    }

    @Override
    public void onUpdateFailed(CharSequence charSequence) {
        setState(this.mProgressMessageController.cancel());
        ensureErrorUiShown(charSequence, 1);
        if (this.mState == 5 || this.mState == 8 || this.mState == 3) {
            doFinish();
        }
        setState(4);
    }

    @Override
    public void onOptionsOpened() {
        MetricsLogger.action(this, 502);
        updateSelectedPagesFromPreview();
    }

    @Override
    public void onOptionsClosed() {
        ((InputMethodManager) getSystemService(InputMethodManager.class)).hideSoftInputFromWindow(this.mDestinationSpinner.getWindowToken(), 0);
    }

    private void updatePrintPreviewController(boolean z) {
        RemotePrintDocument.RemotePrintDocumentInfo documentInfo = this.mPrintedDocument.getDocumentInfo();
        if (!documentInfo.laidout) {
            return;
        }
        this.mPrintPreviewController.onContentUpdated(z, getAdjustedPageCount(documentInfo.info), this.mPrintedDocument.getDocumentInfo().pagesWrittenToFile, this.mSelectedPages, this.mPrintJob.getAttributes().getMediaSize(), this.mPrintJob.getAttributes().getMinMargins());
    }

    @Override
    public boolean canOpenOptions() {
        return true;
    }

    @Override
    public boolean canCloseOptions() {
        return !hasErrors();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (this.mMediaSizeComparator != null) {
            this.mMediaSizeComparator.onConfigurationChanged(configuration);
        }
        if (this.mPrintPreviewController != null) {
            this.mPrintPreviewController.onOrientationChanged();
        }
    }

    @Override
    protected void onDestroy() {
        if (this.mPrintedDocument != null) {
            this.mPrintedDocument.cancel(true);
        }
        doFinish();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        switch (i) {
            case 1:
                onStartCreateDocumentActivityResult(i2, intent);
                break;
            case 2:
                onSelectPrinterActivityResult(i2, intent);
                break;
            case 3:
                onAdvancedPrintOptionsActivityResult(i2, intent);
                break;
        }
    }

    private void startCreateDocumentActivity() {
        PrintDocumentInfo printDocumentInfo;
        if (!isResumed() || (printDocumentInfo = this.mPrintedDocument.getDocumentInfo().info) == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.setType("application/pdf");
        intent.putExtra("android.intent.extra.TITLE", printDocumentInfo.getName());
        intent.putExtra("android.content.extra.PACKAGE_NAME", this.mCallingPackageName);
        try {
            startActivityForResult(intent, 1);
        } catch (Exception e) {
            Log.e("PrintActivity", "Could not create file", e);
            Toast.makeText(this, getString(R.string.could_not_create_file), 0).show();
            onStartCreateDocumentActivityResult(0, null);
        }
    }

    private void onStartCreateDocumentActivityResult(int i, Intent intent) {
        if (i == -1 && intent != null) {
            updateOptionsUi();
            final Uri data = intent.getData();
            countPrintOperation(getPackageName());
            this.mDestinationSpinner.post(new Runnable() {
                @Override
                public void run() {
                    PrintActivity.this.transformDocumentAndFinish(data);
                }
            });
            return;
        }
        if (i == 0) {
            this.mState = 1;
            updateDocument(false);
            updateOptionsUi();
        } else {
            setState(5);
            this.mDestinationSpinner.post(new Runnable() {
                @Override
                public void run() {
                    PrintActivity.this.doFinish();
                }
            });
        }
    }

    private void startSelectPrinterActivity() {
        startActivityForResult(new Intent(this, (Class<?>) SelectPrinterActivity.class), 2);
    }

    private void onSelectPrinterActivityResult(int i, Intent intent) {
        PrinterInfo printerInfo;
        if (i == -1 && intent != null && (printerInfo = (PrinterInfo) intent.getParcelableExtra("INTENT_EXTRA_PRINTER")) != null) {
            this.mCurrentPrinter = printerInfo;
            this.mPrintJob.setPrinterId(printerInfo.getId());
            this.mPrintJob.setPrinterName(printerInfo.getName());
            if (canPrint(printerInfo)) {
                updatePrintAttributesFromCapabilities(printerInfo.getCapabilities());
                onPrinterAvailable(printerInfo);
            } else {
                onPrinterUnavailable(printerInfo);
            }
            this.mDestinationSpinnerAdapter.ensurePrinterInVisibleAdapterPosition(printerInfo);
            MetricsLogger.action(this, 507, printerInfo.getId().getServiceName().getPackageName());
        }
        if (this.mCurrentPrinter != null) {
            this.mDestinationSpinnerAdapter.notifyDataSetChanged();
        }
    }

    private void startAdvancedPrintOptionsActivity(PrinterInfo printerInfo) {
        if (this.mAdvancedPrintOptionsActivity == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setComponent(this.mAdvancedPrintOptionsActivity);
        List<ResolveInfo> listQueryIntentActivities = getPackageManager().queryIntentActivities(intent, 0);
        if (listQueryIntentActivities.isEmpty()) {
            Log.w("PrintActivity", "Advanced options activity " + this.mAdvancedPrintOptionsActivity + " could not be found");
            return;
        }
        if (((ComponentInfo) listQueryIntentActivities.get(0).activityInfo).exported) {
            PrintJobInfo.Builder builder = new PrintJobInfo.Builder(this.mPrintJob);
            builder.setPages(this.mSelectedPages);
            intent.putExtra("android.intent.extra.print.PRINT_JOB_INFO", builder.build());
            intent.putExtra("android.intent.extra.print.EXTRA_PRINTER_INFO", printerInfo);
            intent.putExtra("android.printservice.extra.PRINT_DOCUMENT_INFO", this.mPrintedDocument.getDocumentInfo().info);
            this.mIsMoreOptionsActivityInProgress = true;
            try {
                startActivityForResult(intent, 3);
            } catch (ActivityNotFoundException e) {
                this.mIsMoreOptionsActivityInProgress = false;
                Log.e("PrintActivity", "Error starting activity for intent: " + intent, e);
            }
            this.mMoreOptionsButton.setEnabled(true ^ this.mIsMoreOptionsActivityInProgress);
        }
    }

    private void onAdvancedPrintOptionsActivityResult(int i, Intent intent) {
        PrintJobInfo printJobInfo;
        PrinterCapabilitiesInfo capabilities;
        this.mIsMoreOptionsActivityInProgress = false;
        this.mMoreOptionsButton.setEnabled(true);
        if (i != -1 || intent == null || (printJobInfo = (PrintJobInfo) intent.getParcelableExtra("android.intent.extra.print.PRINT_JOB_INFO")) == null) {
            return;
        }
        this.mPrintJob.setAdvancedOptions(printJobInfo.getAdvancedOptions());
        if (printJobInfo.getCopies() < 1) {
            Log.w("PrintActivity", "Cannot apply return value from advanced options activity. Copies must be 1 or more. Actual value is: " + printJobInfo.getCopies() + ". Ignoring.");
        } else {
            this.mCopiesEditText.setText(String.valueOf(printJobInfo.getCopies()));
            this.mPrintJob.setCopies(printJobInfo.getCopies());
        }
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        PrintAttributes attributes2 = printJobInfo.getAttributes();
        if (attributes2 != null) {
            PrintAttributes.MediaSize mediaSize = attributes.getMediaSize();
            PrintAttributes.MediaSize mediaSize2 = attributes2.getMediaSize();
            if (mediaSize2 != null && !mediaSize.equals(mediaSize2)) {
                int count = this.mMediaSizeSpinnerAdapter.getCount();
                PrintAttributes.MediaSize mediaSizeAsPortrait = attributes2.getMediaSize().asPortrait();
                int i2 = 0;
                while (true) {
                    if (i2 >= count) {
                        break;
                    }
                    if (!this.mMediaSizeSpinnerAdapter.getItem(i2).value.asPortrait().equals(mediaSizeAsPortrait)) {
                        i2++;
                    } else {
                        attributes.setMediaSize(mediaSize2);
                        this.mMediaSizeSpinner.setSelection(i2);
                        if (attributes.getMediaSize().isPortrait()) {
                            if (this.mOrientationSpinner.getSelectedItemPosition() != 0) {
                                this.mOrientationSpinner.setSelection(0);
                            }
                        } else if (this.mOrientationSpinner.getSelectedItemPosition() != 1) {
                            this.mOrientationSpinner.setSelection(1);
                        }
                    }
                }
            }
            PrintAttributes.Resolution resolution = attributes.getResolution();
            PrintAttributes.Resolution resolution2 = attributes2.getResolution();
            if (!resolution.equals(resolution2) && (capabilities = this.mCurrentPrinter.getCapabilities()) != null) {
                List<PrintAttributes.Resolution> resolutions = capabilities.getResolutions();
                int size = resolutions.size();
                int i3 = 0;
                while (true) {
                    if (i3 >= size) {
                        break;
                    }
                    PrintAttributes.Resolution resolution3 = resolutions.get(i3);
                    if (!resolution3.equals(resolution2)) {
                        i3++;
                    } else {
                        attributes.setResolution(resolution3);
                        break;
                    }
                }
            }
            int colorMode = attributes.getColorMode();
            int colorMode2 = attributes2.getColorMode();
            if (colorMode != colorMode2) {
                int count2 = this.mColorModeSpinner.getCount();
                int i4 = 0;
                while (true) {
                    if (i4 >= count2) {
                        break;
                    }
                    if (this.mColorModeSpinnerAdapter.getItem(i4).value.intValue() != colorMode2) {
                        i4++;
                    } else {
                        attributes.setColorMode(colorMode2);
                        this.mColorModeSpinner.setSelection(i4);
                        break;
                    }
                }
            }
            int duplexMode = attributes.getDuplexMode();
            int duplexMode2 = attributes2.getDuplexMode();
            if (duplexMode != duplexMode2) {
                int count3 = this.mDuplexModeSpinner.getCount();
                int i5 = 0;
                while (true) {
                    if (i5 >= count3) {
                        break;
                    }
                    if (this.mDuplexModeSpinnerAdapter.getItem(i5).value.intValue() != duplexMode2) {
                        i5++;
                    } else {
                        attributes.setDuplexMode(duplexMode2);
                        this.mDuplexModeSpinner.setSelection(i5);
                        break;
                    }
                }
            }
        }
        PrintDocumentInfo printDocumentInfo = this.mPrintedDocument.getDocumentInfo().info;
        int adjustedPageCount = printDocumentInfo != null ? getAdjustedPageCount(printDocumentInfo) : 0;
        PageRange[] pages = printJobInfo.getPages();
        if (pages != null && adjustedPageCount > 0) {
            PageRange[] pageRangeArrNormalize = PageRangeUtils.normalize(pages);
            ArrayList arrayList = new ArrayList();
            int length = pageRangeArrNormalize.length;
            int i6 = 0;
            while (true) {
                if (i6 >= length) {
                    break;
                }
                PageRange pageRange = pageRangeArrNormalize[i6];
                if (pageRange.getEnd() >= adjustedPageCount) {
                    int start = pageRange.getStart();
                    int i7 = adjustedPageCount - 1;
                    if (start <= i7) {
                        arrayList.add(new PageRange(start, i7));
                    }
                } else {
                    arrayList.add(pageRange);
                    i6++;
                }
            }
            if (!arrayList.isEmpty()) {
                PageRange[] pageRangeArr = new PageRange[arrayList.size()];
                arrayList.toArray(pageRangeArr);
                updateSelectedPages(pageRangeArr, adjustedPageCount);
            }
        }
        if (canUpdateDocument()) {
            updateDocument(false);
        }
    }

    private void setState(int i) {
        if (isFinalState(this.mState)) {
            if (isFinalState(i)) {
                this.mState = i;
                updateOptionsUi();
                return;
            }
            return;
        }
        this.mState = i;
        updateOptionsUi();
    }

    private static boolean isFinalState(int i) {
        return i == 3 || i == 8 || i == 5;
    }

    private void updateSelectedPagesFromPreview() {
        PageRange[] selectedPages = this.mPrintPreviewController.getSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, selectedPages)) {
            updateSelectedPages(selectedPages, getAdjustedPageCount(this.mPrintedDocument.getDocumentInfo().info));
        }
    }

    private void updateSelectedPages(PageRange[] pageRangeArr, int i) {
        int start;
        int end;
        if (pageRangeArr == null || pageRangeArr.length <= 0) {
            return;
        }
        PageRange[] pageRangeArrNormalize = PageRangeUtils.normalize(pageRangeArr);
        if (PageRangeUtils.isAllPages(pageRangeArrNormalize, i)) {
            pageRangeArrNormalize = new PageRange[]{PageRange.ALL_PAGES};
        }
        if (Arrays.equals(this.mSelectedPages, pageRangeArrNormalize)) {
            return;
        }
        this.mSelectedPages = pageRangeArrNormalize;
        this.mPrintJob.setPages(pageRangeArrNormalize);
        if (Arrays.equals(pageRangeArrNormalize, PageRange.ALL_PAGES_ARRAY)) {
            if (this.mRangeOptionsSpinner.getSelectedItemPosition() != 0) {
                this.mRangeOptionsSpinner.setSelection(0);
                this.mPageRangeEditText.setText("");
                return;
            }
            return;
        }
        if (pageRangeArrNormalize[0].getStart() >= 0 && pageRangeArrNormalize[pageRangeArrNormalize.length - 1].getEnd() < i) {
            if (this.mRangeOptionsSpinner.getSelectedItemPosition() != 1) {
                this.mRangeOptionsSpinner.setSelection(1);
            }
            StringBuilder sb = new StringBuilder();
            for (PageRange pageRange : pageRangeArrNormalize) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                if (!pageRange.equals(PageRange.ALL_PAGES)) {
                    start = pageRange.getStart() + 1;
                    end = pageRange.getEnd() + 1;
                } else {
                    end = i;
                    start = 1;
                }
                sb.append(start);
                if (start != end) {
                    sb.append('-');
                    sb.append(end);
                }
            }
            this.mPageRangeEditText.setText(sb.toString());
        }
    }

    private void ensureProgressUiShown() {
        if (!isFinishing() && !isDestroyed() && this.mUiState != 2) {
            this.mUiState = 2;
            this.mPrintPreviewController.setUiShown(false);
            showFragment(PrintProgressFragment.newInstance());
        }
    }

    private void ensurePreviewUiShown() {
        if (!isFinishing() && !isDestroyed() && this.mUiState != 0) {
            this.mUiState = 0;
            this.mPrintPreviewController.setUiShown(true);
            showFragment(null);
        }
    }

    private void ensureErrorUiShown(CharSequence charSequence, int i) {
        if (!isFinishing() && !isDestroyed() && this.mUiState != 1) {
            this.mUiState = 1;
            this.mPrintPreviewController.setUiShown(false);
            showFragment(PrintErrorFragment.newInstance(charSequence, i));
        }
    }

    private void showFragment(Fragment fragment) {
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        Fragment fragmentFindFragmentByTag = getFragmentManager().findFragmentByTag("FRAGMENT_TAG");
        if (fragmentFindFragmentByTag != null) {
            fragmentTransactionBeginTransaction.remove(fragmentFindFragmentByTag);
        }
        if (fragment != null) {
            fragmentTransactionBeginTransaction.add(R.id.embedded_content_container, fragment, "FRAGMENT_TAG");
        }
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
    }

    private void countPrintOperation(String str) {
        MetricsLogger.action(this, 505, str);
        MetricsLogger.histogram(this, "print_pages", getAdjustedPageCount(this.mPrintJob.getDocumentInfo()));
        if (this.mPrintJob.getPrinterId().equals(this.mDefaultPrinter)) {
            MetricsLogger.histogram(this, "print_default", 1);
        }
        if (((UserManager) getSystemService("user")).isManagedProfile()) {
            MetricsLogger.histogram(this, "print_work", 1);
        }
    }

    private void requestCreatePdfFileOrFinish() {
        this.mPrintedDocument.cancel(false);
        if (this.mCurrentPrinter == this.mDestinationSpinnerAdapter.getPdfPrinter()) {
            startCreateDocumentActivity();
        } else {
            countPrintOperation(this.mCurrentPrinter.getId().getServiceName().getPackageName());
            transformDocumentAndFinish(null);
        }
    }

    private void clearPageRanges() {
        this.mRangeOptionsSpinner.setSelection(0);
        this.mPageRangeEditText.setError(null);
        this.mPageRangeEditText.setText("");
        this.mSelectedPages = PageRange.ALL_PAGES_ARRAY;
        if (!Arrays.equals(this.mSelectedPages, this.mPrintPreviewController.getSelectedPages())) {
            updatePrintPreviewController(false);
        }
    }

    private void updatePrintAttributesFromCapabilities(PrinterCapabilitiesInfo printerCapabilitiesInfo) {
        boolean z;
        PrintAttributes defaults = printerCapabilitiesInfo.getDefaults();
        ArrayList arrayList = new ArrayList(printerCapabilitiesInfo.getMediaSizes());
        Collections.sort(arrayList, this.mMediaSizeComparator);
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        PrintAttributes.MediaSize mediaSize = attributes.getMediaSize();
        boolean z2 = true;
        if (mediaSize == null) {
            attributes.setMediaSize(defaults.getMediaSize());
            z = true;
        } else {
            PrintAttributes.MediaSize mediaSize2 = null;
            boolean zIsPortrait = mediaSize.isPortrait();
            PrintAttributes.MediaSize mediaSizeAsPortrait = mediaSize.asPortrait();
            int size = arrayList.size();
            z = false;
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                PrintAttributes.MediaSize mediaSize3 = (PrintAttributes.MediaSize) arrayList.get(i);
                if (!mediaSizeAsPortrait.equals(mediaSize3.asPortrait())) {
                    i++;
                } else {
                    mediaSize2 = mediaSize3;
                    break;
                }
            }
            if (mediaSize2 == null) {
                mediaSize2 = defaults.getMediaSize();
                z = true;
            }
            if (mediaSize2 != null) {
                if (zIsPortrait) {
                    attributes.setMediaSize(mediaSize2.asPortrait());
                } else {
                    attributes.setMediaSize(mediaSize2.asLandscape());
                }
            }
        }
        if ((attributes.getColorMode() & printerCapabilitiesInfo.getColorModes()) == 0) {
            attributes.setColorMode(defaults.getColorMode());
        }
        if ((attributes.getDuplexMode() & printerCapabilitiesInfo.getDuplexModes()) == 0) {
            attributes.setDuplexMode(defaults.getDuplexMode());
        }
        PrintAttributes.Resolution resolution = attributes.getResolution();
        if (resolution == null || !printerCapabilitiesInfo.getResolutions().contains(resolution)) {
            attributes.setResolution(defaults.getResolution());
        }
        if (Objects.equals(attributes.getMinMargins(), defaults.getMinMargins())) {
            z2 = z;
        }
        attributes.setMinMargins(defaults.getMinMargins());
        if (z2) {
            clearPageRanges();
        }
    }

    private boolean updateDocument(boolean z) {
        PageRange[] selectedPages;
        if (!z && this.mPrintedDocument.hasUpdateError()) {
            return false;
        }
        if (z && this.mPrintedDocument.hasUpdateError()) {
            this.mPrintedDocument.clearUpdateError();
        }
        boolean z2 = this.mState != 2;
        if (z2) {
            selectedPages = this.mPrintPreviewController.getRequestedPages();
        } else {
            selectedPages = this.mPrintPreviewController.getSelectedPages();
        }
        boolean zUpdate = this.mPrintedDocument.update(this.mPrintJob.getAttributes(), selectedPages, z2);
        updateOptionsUi();
        if (zUpdate && !this.mPrintedDocument.hasLaidOutPages()) {
            this.mProgressMessageController.post();
            return true;
        }
        if (!zUpdate) {
            updatePrintPreviewController(false);
        }
        return false;
    }

    private void addCurrentPrinterToHistory() {
        if (this.mCurrentPrinter != null) {
            if (!this.mCurrentPrinter.getId().equals(this.mDestinationSpinnerAdapter.getPdfPrinter().getId())) {
                this.mPrinterRegistry.addHistoricalPrinter(this.mCurrentPrinter);
            }
        }
    }

    private void cancelPrint() {
        setState(3);
        this.mPrintedDocument.cancel(true);
        doFinish();
    }

    private void updateSelectedPagesFromTextField() {
        PageRange[] pageRangeArrComputeSelectedPages = computeSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, pageRangeArrComputeSelectedPages)) {
            this.mSelectedPages = pageRangeArrComputeSelectedPages;
            updatePrintPreviewController(false);
        }
    }

    private void confirmPrint() {
        setState(2);
        addCurrentPrinterToHistory();
        setUserPrinted();
        updateSelectedPagesFromPreview();
        updateSelectedPagesFromTextField();
        this.mPrintPreviewController.closeOptions();
        if (canUpdateDocument()) {
            updateDocument(false);
        }
        if (!this.mPrintedDocument.isUpdating()) {
            requestCreatePdfFileOrFinish();
        }
    }

    private void bindUi() {
        this.mSummaryContainer = findViewById(R.id.summary_content);
        this.mSummaryCopies = (TextView) findViewById(R.id.copies_count_summary);
        this.mSummaryPaperSize = (TextView) findViewById(R.id.paper_size_summary);
        this.mOptionsContent = (PrintContentView) findViewById(R.id.options_content);
        this.mOptionsContent.setOptionsStateChangeListener(this);
        this.mOptionsContent.setOpenOptionsController(this);
        MyOnItemSelectedListener myOnItemSelectedListener = new MyOnItemSelectedListener();
        MyClickListener myClickListener = new MyClickListener();
        this.mCopiesEditText = (EditText) findViewById(R.id.copies_edittext);
        this.mCopiesEditText.setOnFocusChangeListener(this.mSelectAllOnFocusListener);
        this.mCopiesEditText.setText(MIN_COPIES_STRING);
        this.mCopiesEditText.setSelection(this.mCopiesEditText.getText().length());
        this.mCopiesEditText.addTextChangedListener(new EditTextWatcher());
        this.mPrintersObserver = new PrintersObserver();
        this.mDestinationSpinnerAdapter.registerDataSetObserver(this.mPrintersObserver);
        this.mDestinationSpinner = (ClickInterceptSpinner) findViewById(R.id.destination_spinner);
        this.mDestinationSpinner.setAdapter((SpinnerAdapter) this.mDestinationSpinnerAdapter);
        this.mDestinationSpinner.setOnItemSelectedListener(myOnItemSelectedListener);
        this.mMediaSizeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mMediaSizeSpinner = (Spinner) findViewById(R.id.paper_size_spinner);
        this.mMediaSizeSpinner.setAdapter((SpinnerAdapter) this.mMediaSizeSpinnerAdapter);
        this.mMediaSizeSpinner.setOnItemSelectedListener(myOnItemSelectedListener);
        this.mColorModeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mColorModeSpinner = (Spinner) findViewById(R.id.color_spinner);
        this.mColorModeSpinner.setAdapter((SpinnerAdapter) this.mColorModeSpinnerAdapter);
        this.mColorModeSpinner.setOnItemSelectedListener(myOnItemSelectedListener);
        this.mDuplexModeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mDuplexModeSpinner = (Spinner) findViewById(R.id.duplex_spinner);
        this.mDuplexModeSpinner.setAdapter((SpinnerAdapter) this.mDuplexModeSpinnerAdapter);
        this.mDuplexModeSpinner.setOnItemSelectedListener(myOnItemSelectedListener);
        this.mOrientationSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        String[] stringArray = getResources().getStringArray(R.array.orientation_labels);
        this.mOrientationSpinnerAdapter.add(new SpinnerItem<>(0, stringArray[0]));
        this.mOrientationSpinnerAdapter.add(new SpinnerItem<>(1, stringArray[1]));
        this.mOrientationSpinner = (Spinner) findViewById(R.id.orientation_spinner);
        this.mOrientationSpinner.setAdapter((SpinnerAdapter) this.mOrientationSpinnerAdapter);
        this.mOrientationSpinner.setOnItemSelectedListener(myOnItemSelectedListener);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mRangeOptionsSpinner = (Spinner) findViewById(R.id.range_options_spinner);
        this.mRangeOptionsSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        this.mRangeOptionsSpinner.setOnItemSelectedListener(myOnItemSelectedListener);
        updatePageRangeOptions(-1);
        this.mPageRangeTitle = (TextView) findViewById(R.id.page_range_title);
        this.mPageRangeEditText = (EditText) findViewById(R.id.page_range_edittext);
        this.mPageRangeEditText.setVisibility(8);
        this.mPageRangeTitle.setVisibility(8);
        this.mPageRangeEditText.setOnFocusChangeListener(this.mSelectAllOnFocusListener);
        this.mPageRangeEditText.addTextChangedListener(new RangeTextWatcher());
        this.mMoreOptionsButton = (Button) findViewById(R.id.more_options_button);
        this.mMoreOptionsButton.setOnClickListener(myClickListener);
        this.mPrintButton = (ImageView) findViewById(R.id.print_button);
        this.mPrintButton.setOnClickListener(myClickListener);
        this.mIsOptionsUiBound = true;
        if (!hasUserEverPrinted()) {
            this.mShowDestinationPrompt = true;
            this.mSummaryCopies.setEnabled(false);
            this.mSummaryPaperSize.setEnabled(false);
            this.mDestinationSpinner.setPerformClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    PrintActivity.lambda$bindUi$3(this.f$0, view);
                }
            });
        }
    }

    public static void lambda$bindUi$3(PrintActivity printActivity, View view) {
        printActivity.mShowDestinationPrompt = false;
        printActivity.mSummaryCopies.setEnabled(true);
        printActivity.mSummaryPaperSize.setEnabled(true);
        printActivity.updateOptionsUi();
        printActivity.mDestinationSpinner.setPerformClickListener(null);
        printActivity.mDestinationSpinnerAdapter.notifyDataSetChanged();
    }

    @Override
    public Loader<List<PrintServiceInfo>> onCreateLoader(int i, Bundle bundle) {
        return new PrintServicesLoader((PrintManager) getSystemService("print"), this, 1);
    }

    @Override
    public void onLoadFinished(Loader<List<PrintServiceInfo>> loader, List<PrintServiceInfo> list) {
        ComponentName componentName;
        boolean z = false;
        if (this.mCurrentPrinter != null && list != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                PrintServiceInfo printServiceInfo = list.get(i);
                if (printServiceInfo.getComponentName().equals(this.mCurrentPrinter.getId().getServiceName())) {
                    String advancedOptionsActivityName = printServiceInfo.getAdvancedOptionsActivityName();
                    if (!TextUtils.isEmpty(advancedOptionsActivityName)) {
                        componentName = new ComponentName(printServiceInfo.getComponentName().getPackageName(), advancedOptionsActivityName);
                        break;
                    }
                }
            }
            componentName = null;
        } else {
            componentName = null;
        }
        if (!Objects.equals(componentName, this.mAdvancedPrintOptionsActivity)) {
            this.mAdvancedPrintOptionsActivity = componentName;
            updateOptionsUi();
        }
        if (list != null && !list.isEmpty()) {
            z = true;
        }
        if (this.mArePrintServicesEnabled != z) {
            this.mArePrintServicesEnabled = z;
            if (this.mDestinationSpinnerAdapter != null) {
                this.mDestinationSpinnerAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<PrintServiceInfo>> loader) {
        if (!isFinishing() && !isDestroyed()) {
            onLoadFinished(loader, (List<PrintServiceInfo>) null);
        }
    }

    public static final class PrintServiceApprovalDialog extends DialogFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private ApprovedPrintServices mApprovedServices;

        static PrintServiceApprovalDialog newInstance(ComponentName componentName) {
            PrintServiceApprovalDialog printServiceApprovalDialog = new PrintServiceApprovalDialog();
            Bundle bundle = new Bundle();
            bundle.putParcelable("PRINTSERVICE", componentName);
            printServiceApprovalDialog.setArguments(bundle);
            return printServiceApprovalDialog;
        }

        @Override
        public void onStop() {
            super.onStop();
            this.mApprovedServices.unregisterChangeListener(this);
        }

        @Override
        public void onStart() {
            super.onStart();
            ComponentName componentName = (ComponentName) getArguments().getParcelable("PRINTSERVICE");
            synchronized (ApprovedPrintServices.sLock) {
                if (this.mApprovedServices.isApprovedService(componentName)) {
                    dismiss();
                } else {
                    this.mApprovedServices.registerChangeListenerLocked(this);
                }
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle bundle) {
            CharSequence charSequenceLoadLabel;
            super.onCreateDialog(bundle);
            this.mApprovedServices = new ApprovedPrintServices(getActivity());
            PackageManager packageManager = getActivity().getPackageManager();
            try {
                charSequenceLoadLabel = packageManager.getApplicationInfo(((ComponentName) getArguments().getParcelable("PRINTSERVICE")).getPackageName(), 0).loadLabel(packageManager);
            } catch (PackageManager.NameNotFoundException e) {
                charSequenceLoadLabel = null;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.print_service_security_warning_title, charSequenceLoadLabel)).setMessage(getString(R.string.print_service_security_warning_summary, charSequenceLoadLabel)).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ComponentName componentName = (ComponentName) PrintServiceApprovalDialog.this.getArguments().getParcelable("PRINTSERVICE");
                    PrintServiceApprovalDialog.this.mApprovedServices.unregisterChangeListener(PrintServiceApprovalDialog.this);
                    PrintServiceApprovalDialog.this.mApprovedServices.addApprovedService(componentName);
                    ((PrintActivity) PrintServiceApprovalDialog.this.getActivity()).confirmPrint();
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String str) {
            ComponentName componentName = (ComponentName) getArguments().getParcelable("PRINTSERVICE");
            synchronized (ApprovedPrintServices.sLock) {
                if (this.mApprovedServices.isApprovedService(componentName)) {
                    dismiss();
                }
            }
        }
    }

    private final class MyClickListener implements View.OnClickListener {
        private MyClickListener() {
        }

        @Override
        public void onClick(View view) {
            if (view == PrintActivity.this.mPrintButton) {
                if (PrintActivity.this.mCurrentPrinter != null) {
                    if (PrintActivity.this.mDestinationSpinnerAdapter.getPdfPrinter() == PrintActivity.this.mCurrentPrinter) {
                        PrintActivity.this.confirmPrint();
                        return;
                    }
                    ApprovedPrintServices approvedPrintServices = new ApprovedPrintServices(PrintActivity.this);
                    ComponentName serviceName = PrintActivity.this.mCurrentPrinter.getId().getServiceName();
                    if (approvedPrintServices.isApprovedService(serviceName)) {
                        PrintActivity.this.confirmPrint();
                        return;
                    } else {
                        PrintServiceApprovalDialog.newInstance(serviceName).show(PrintActivity.this.getFragmentManager(), "approve");
                        return;
                    }
                }
                PrintActivity.this.cancelPrint();
                return;
            }
            if (view == PrintActivity.this.mMoreOptionsButton) {
                if (PrintActivity.this.mPageRangeEditText.getError() == null) {
                    PrintActivity.this.updateSelectedPagesFromTextField();
                }
                if (PrintActivity.this.mCurrentPrinter != null) {
                    PrintActivity.this.startAdvancedPrintOptionsActivity(PrintActivity.this.mCurrentPrinter);
                }
            }
        }
    }

    private static boolean canPrint(PrinterInfo printerInfo) {
        return (printerInfo.getCapabilities() == null || printerInfo.getStatus() == 3) ? false : true;
    }

    private void disableOptionsUi(boolean z) {
        this.mCopiesEditText.setEnabled(false);
        this.mCopiesEditText.setFocusable(false);
        this.mMediaSizeSpinner.setEnabled(false);
        this.mColorModeSpinner.setEnabled(false);
        this.mDuplexModeSpinner.setEnabled(false);
        this.mOrientationSpinner.setEnabled(false);
        this.mPrintButton.setVisibility(8);
        this.mMoreOptionsButton.setEnabled(false);
        if (z) {
            this.mRangeOptionsSpinner.setEnabled(false);
            this.mPageRangeEditText.setEnabled(false);
        }
    }

    void updateOptionsUi() {
        boolean z;
        boolean z2;
        boolean z3;
        if (!this.mIsOptionsUiBound) {
            return;
        }
        updateSummary();
        this.mDestinationSpinner.setEnabled(!isFinalState(this.mState));
        if (this.mState == 2 || this.mState == 8 || this.mState == 3 || this.mState == 4 || this.mState == 5 || this.mState == 6 || this.mState == 7) {
            disableOptionsUi(isFinalState(this.mState));
            return;
        }
        if (this.mCurrentPrinter == null || !canPrint(this.mCurrentPrinter)) {
            disableOptionsUi(false);
            return;
        }
        PrinterCapabilitiesInfo capabilities = this.mCurrentPrinter.getCapabilities();
        PrintAttributes defaults = capabilities.getDefaults();
        this.mDestinationSpinner.setEnabled(true);
        this.mMediaSizeSpinner.setEnabled(true);
        ArrayList arrayList = new ArrayList(capabilities.getMediaSizes());
        Collections.sort(arrayList, this.mMediaSizeComparator);
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        int size = arrayList.size();
        if (size == this.mMediaSizeSpinnerAdapter.getCount()) {
            for (int i = 0; i < size; i++) {
                if (!((PrintAttributes.MediaSize) arrayList.get(i)).equals(this.mMediaSizeSpinnerAdapter.getItem(i).value)) {
                    z = true;
                    break;
                }
            }
            z = false;
        } else {
            z = true;
            break;
        }
        if (z) {
            PrintAttributes.MediaSize mediaSize = attributes.getMediaSize();
            this.mMediaSizeSpinnerAdapter.clear();
            int i2 = -1;
            for (int i3 = 0; i3 < size; i3++) {
                PrintAttributes.MediaSize mediaSize2 = (PrintAttributes.MediaSize) arrayList.get(i3);
                if (mediaSize != null && mediaSize2.asPortrait().equals(mediaSize.asPortrait())) {
                    i2 = i3;
                }
                this.mMediaSizeSpinnerAdapter.add(new SpinnerItem<>(mediaSize2, mediaSize2.getLabel(getPackageManager())));
            }
            if (i2 != -1) {
                if (this.mMediaSizeSpinner.getSelectedItemPosition() != i2) {
                    this.mMediaSizeSpinner.setSelection(i2);
                }
            } else {
                int iMax = Math.max(arrayList.indexOf(defaults.getMediaSize()), 0);
                if (this.mMediaSizeSpinner.getSelectedItemPosition() != iMax) {
                    this.mMediaSizeSpinner.setSelection(iMax);
                }
                if (mediaSize != null) {
                    if (mediaSize.isPortrait()) {
                        attributes.setMediaSize(this.mMediaSizeSpinnerAdapter.getItem(iMax).value.asPortrait());
                    } else {
                        attributes.setMediaSize(this.mMediaSizeSpinnerAdapter.getItem(iMax).value.asLandscape());
                    }
                }
            }
        }
        this.mColorModeSpinner.setEnabled(true);
        int colorModes = capabilities.getColorModes();
        if (Integer.bitCount(colorModes) == this.mColorModeSpinnerAdapter.getCount()) {
            int i4 = colorModes;
            int i5 = 0;
            while (i4 != 0) {
                int iNumberOfTrailingZeros = 1 << Integer.numberOfTrailingZeros(i4);
                i4 &= ~iNumberOfTrailingZeros;
                if (iNumberOfTrailingZeros == this.mColorModeSpinnerAdapter.getItem(i5).value.intValue()) {
                    i5++;
                } else {
                    z2 = true;
                    break;
                }
            }
            z2 = false;
        } else {
            z2 = true;
            break;
        }
        if (z2) {
            int colorMode = attributes.getColorMode();
            this.mColorModeSpinnerAdapter.clear();
            String[] stringArray = getResources().getStringArray(R.array.color_mode_labels);
            int i6 = colorModes;
            int count = -1;
            while (i6 != 0) {
                int iNumberOfTrailingZeros2 = Integer.numberOfTrailingZeros(i6);
                int i7 = 1 << iNumberOfTrailingZeros2;
                if (i7 == colorMode) {
                    count = this.mColorModeSpinnerAdapter.getCount();
                }
                i6 &= ~i7;
                this.mColorModeSpinnerAdapter.add(new SpinnerItem<>(Integer.valueOf(i7), stringArray[iNumberOfTrailingZeros2]));
            }
            if (count != -1) {
                if (this.mColorModeSpinner.getSelectedItemPosition() != count) {
                    this.mColorModeSpinner.setSelection(count);
                }
            } else {
                int colorMode2 = colorModes & defaults.getColorMode();
                int count2 = this.mColorModeSpinnerAdapter.getCount();
                int i8 = 0;
                while (true) {
                    if (i8 >= count2) {
                        break;
                    }
                    if (colorMode2 != this.mColorModeSpinnerAdapter.getItem(i8).value.intValue()) {
                        i8++;
                    } else {
                        if (this.mColorModeSpinner.getSelectedItemPosition() != i8) {
                            this.mColorModeSpinner.setSelection(i8);
                        }
                        attributes.setColorMode(colorMode2);
                    }
                }
            }
        }
        this.mDuplexModeSpinner.setEnabled(true);
        int duplexModes = capabilities.getDuplexModes();
        if (Integer.bitCount(duplexModes) == this.mDuplexModeSpinnerAdapter.getCount()) {
            int i9 = duplexModes;
            int i10 = 0;
            while (i9 != 0) {
                int iNumberOfTrailingZeros3 = 1 << Integer.numberOfTrailingZeros(i9);
                i9 &= ~iNumberOfTrailingZeros3;
                if (iNumberOfTrailingZeros3 == this.mDuplexModeSpinnerAdapter.getItem(i10).value.intValue()) {
                    i10++;
                } else {
                    z3 = true;
                    break;
                }
            }
            z3 = false;
        } else {
            z3 = true;
            break;
        }
        if (z3) {
            int duplexMode = attributes.getDuplexMode();
            this.mDuplexModeSpinnerAdapter.clear();
            String[] stringArray2 = getResources().getStringArray(R.array.duplex_mode_labels);
            int count3 = -1;
            while (duplexModes != 0) {
                int iNumberOfTrailingZeros4 = Integer.numberOfTrailingZeros(duplexModes);
                int i11 = 1 << iNumberOfTrailingZeros4;
                if (i11 == duplexMode) {
                    count3 = this.mDuplexModeSpinnerAdapter.getCount();
                }
                duplexModes &= ~i11;
                this.mDuplexModeSpinnerAdapter.add(new SpinnerItem<>(Integer.valueOf(i11), stringArray2[iNumberOfTrailingZeros4]));
            }
            if (count3 != -1) {
                if (this.mDuplexModeSpinner.getSelectedItemPosition() != count3) {
                    this.mDuplexModeSpinner.setSelection(count3);
                }
            } else {
                int duplexMode2 = defaults.getDuplexMode();
                int count4 = this.mDuplexModeSpinnerAdapter.getCount();
                int i12 = 0;
                while (true) {
                    if (i12 >= count4) {
                        break;
                    }
                    if (duplexMode2 != this.mDuplexModeSpinnerAdapter.getItem(i12).value.intValue()) {
                        i12++;
                    } else {
                        if (this.mDuplexModeSpinner.getSelectedItemPosition() != i12) {
                            this.mDuplexModeSpinner.setSelection(i12);
                        }
                        attributes.setDuplexMode(duplexMode2);
                    }
                }
            }
        }
        this.mDuplexModeSpinner.setEnabled(this.mDuplexModeSpinnerAdapter.getCount() > 1);
        this.mOrientationSpinner.setEnabled(true);
        PrintAttributes.MediaSize mediaSize3 = attributes.getMediaSize();
        if (mediaSize3 != null) {
            if (mediaSize3.isPortrait() && this.mOrientationSpinner.getSelectedItemPosition() != 0) {
                this.mOrientationSpinner.setSelection(0);
            } else if (!mediaSize3.isPortrait() && this.mOrientationSpinner.getSelectedItemPosition() != 1) {
                this.mOrientationSpinner.setSelection(1);
            }
        }
        PrintDocumentInfo printDocumentInfo = this.mPrintedDocument.getDocumentInfo().info;
        int adjustedPageCount = getAdjustedPageCount(printDocumentInfo);
        if (adjustedPageCount > 0) {
            if (printDocumentInfo != null) {
                if (adjustedPageCount == 1) {
                    this.mRangeOptionsSpinner.setEnabled(false);
                } else {
                    this.mRangeOptionsSpinner.setEnabled(true);
                    if (this.mRangeOptionsSpinner.getSelectedItemPosition() > 0) {
                        if (!this.mPageRangeEditText.isEnabled()) {
                            this.mPageRangeEditText.setEnabled(true);
                            this.mPageRangeEditText.setVisibility(0);
                            this.mPageRangeTitle.setVisibility(0);
                            this.mPageRangeEditText.requestFocus();
                            ((InputMethodManager) getSystemService("input_method")).showSoftInput(this.mPageRangeEditText, 0);
                        }
                    } else {
                        this.mPageRangeEditText.setEnabled(false);
                        this.mPageRangeEditText.setVisibility(8);
                        this.mPageRangeTitle.setVisibility(8);
                    }
                }
            } else {
                if (this.mRangeOptionsSpinner.getSelectedItemPosition() != 0) {
                    this.mRangeOptionsSpinner.setSelection(0);
                    this.mPageRangeEditText.setText("");
                }
                this.mRangeOptionsSpinner.setEnabled(false);
                this.mPageRangeEditText.setEnabled(false);
                this.mPageRangeEditText.setVisibility(8);
                this.mPageRangeTitle.setVisibility(8);
            }
        }
        int adjustedPageCount2 = getAdjustedPageCount(printDocumentInfo);
        if (adjustedPageCount2 != this.mCurrentPageCount) {
            this.mCurrentPageCount = adjustedPageCount2;
            updatePageRangeOptions(adjustedPageCount2);
        }
        if (this.mAdvancedPrintOptionsActivity != null) {
            this.mMoreOptionsButton.setVisibility(0);
            this.mMoreOptionsButton.setEnabled(!this.mIsMoreOptionsActivityInProgress);
        } else {
            this.mMoreOptionsButton.setVisibility(8);
            this.mMoreOptionsButton.setEnabled(false);
        }
        if (this.mDestinationSpinnerAdapter.getPdfPrinter() != this.mCurrentPrinter) {
            this.mPrintButton.setImageResource(android.R.drawable.ic_media_route_connected_light_03_mtrl);
            this.mPrintButton.setContentDescription(getString(R.string.print_button));
        } else {
            this.mPrintButton.setImageResource(R.drawable.ic_menu_savetopdf);
            this.mPrintButton.setContentDescription(getString(R.string.savetopdf_button));
        }
        if (!this.mPrintedDocument.getDocumentInfo().updated || ((this.mRangeOptionsSpinner.getSelectedItemPosition() == 1 && (TextUtils.isEmpty(this.mPageRangeEditText.getText()) || hasErrors())) || (this.mRangeOptionsSpinner.getSelectedItemPosition() == 0 && (this.mPrintedDocument.getDocumentInfo() == null || hasErrors())))) {
            this.mPrintButton.setVisibility(8);
        } else {
            this.mPrintButton.setVisibility(0);
        }
        if (this.mDestinationSpinnerAdapter.getPdfPrinter() != this.mCurrentPrinter) {
            this.mCopiesEditText.setEnabled(true);
            this.mCopiesEditText.setFocusableInTouchMode(true);
        } else {
            Editable text = this.mCopiesEditText.getText();
            if (TextUtils.isEmpty(text) || !MIN_COPIES_STRING.equals(text.toString())) {
                this.mCopiesEditText.setText(MIN_COPIES_STRING);
            }
            this.mCopiesEditText.setEnabled(false);
            this.mCopiesEditText.setFocusable(false);
        }
        if (this.mCopiesEditText.getError() == null && TextUtils.isEmpty(this.mCopiesEditText.getText())) {
            this.mCopiesEditText.setText(MIN_COPIES_STRING);
            this.mCopiesEditText.requestFocus();
        }
        if (this.mShowDestinationPrompt) {
            disableOptionsUi(false);
        }
    }

    private void updateSummary() {
        Editable text;
        if (!this.mIsOptionsUiBound) {
            return;
        }
        CharSequence charSequence = null;
        if (!TextUtils.isEmpty(this.mCopiesEditText.getText())) {
            text = this.mCopiesEditText.getText();
            this.mSummaryCopies.setText(text);
        } else {
            text = null;
        }
        int selectedItemPosition = this.mMediaSizeSpinner.getSelectedItemPosition();
        if (selectedItemPosition >= 0) {
            charSequence = this.mMediaSizeSpinnerAdapter.getItem(selectedItemPosition).label;
            this.mSummaryPaperSize.setText(charSequence);
        }
        if (!TextUtils.isEmpty(text) && !TextUtils.isEmpty(charSequence)) {
            this.mSummaryContainer.setContentDescription(getString(R.string.summary_template, text, charSequence));
        }
    }

    private void updatePageRangeOptions(int i) {
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.mRangeOptionsSpinner.getAdapter();
        arrayAdapter.clear();
        int[] intArray = getResources().getIntArray(R.array.page_options_values);
        String strValueOf = i > 0 ? String.valueOf(i) : "";
        String[] strArr = {getString(R.string.template_all_pages, strValueOf), getString(R.string.template_page_range, strValueOf)};
        int length = strArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            arrayAdapter.add(new SpinnerItem(Integer.valueOf(intArray[i2]), strArr[i2]));
        }
    }

    private PageRange[] computeSelectedPages() {
        if (hasErrors()) {
            return null;
        }
        if (this.mRangeOptionsSpinner.getSelectedItemPosition() > 0) {
            PrintDocumentInfo printDocumentInfo = this.mPrintedDocument.getDocumentInfo().info;
            return PageRangeUtils.parsePageRanges(this.mPageRangeEditText.getText(), printDocumentInfo != null ? getAdjustedPageCount(printDocumentInfo) : 0);
        }
        return PageRange.ALL_PAGES_ARRAY;
    }

    private int getAdjustedPageCount(PrintDocumentInfo printDocumentInfo) {
        int pageCount;
        if (printDocumentInfo != null && (pageCount = printDocumentInfo.getPageCount()) != -1) {
            return pageCount;
        }
        return this.mPrintPreviewController.getFilePageCount();
    }

    private boolean hasErrors() {
        return this.mCopiesEditText.getError() != null || (this.mPageRangeEditText.getVisibility() == 0 && this.mPageRangeEditText.getError() != null);
    }

    public void onPrinterAvailable(PrinterInfo printerInfo) {
        if (this.mCurrentPrinter != null && this.mCurrentPrinter.equals(printerInfo)) {
            setState(1);
            if (canUpdateDocument()) {
                updateDocument(false);
            }
            ensurePreviewUiShown();
        }
    }

    public void onPrinterUnavailable(PrinterInfo printerInfo) {
        if (this.mCurrentPrinter == null || this.mCurrentPrinter.getId().equals(printerInfo.getId())) {
            setState(6);
            this.mPrintedDocument.cancel(false);
            ensureErrorUiShown(getString(R.string.print_error_printer_unavailable), 0);
        }
    }

    private boolean canUpdateDocument() {
        if (this.mPrintedDocument.isDestroyed() || hasErrors()) {
            return false;
        }
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        int colorMode = attributes.getColorMode();
        return ((colorMode != 2 && colorMode != 1) || attributes.getMediaSize() == null || attributes.getMinMargins() == null || attributes.getResolution() == null || this.mCurrentPrinter == null || this.mCurrentPrinter.getCapabilities() == null || this.mCurrentPrinter.getStatus() == 3) ? false : true;
    }

    private void transformDocumentAndFinish(final Uri uri) {
        new DocumentTransformer(this, this.mPrintJob, this.mFileProvider, this.mDestinationSpinnerAdapter.getPdfPrinter() == this.mCurrentPrinter ? this.mPrintJob.getAttributes() : null, new Consumer() {
            @Override
            public final void accept(Object obj) throws Throwable {
                PrintActivity.lambda$transformDocumentAndFinish$4(this.f$0, uri, (String) obj);
            }
        }).transform();
    }

    public static void lambda$transformDocumentAndFinish$4(PrintActivity printActivity, Uri uri, String str) throws Throwable {
        if (str == null) {
            if (uri != null) {
                printActivity.mPrintedDocument.writeContent(printActivity.getContentResolver(), uri);
            }
            printActivity.setState(8);
            printActivity.doFinish();
            return;
        }
        printActivity.onPrintDocumentError(str);
    }

    private void doFinish() {
        if ((this.mPrintedDocument != null && this.mPrintedDocument.isUpdating()) || this.mIsFinishing) {
            return;
        }
        this.mIsFinishing = true;
        if (this.mPrinterRegistry != null) {
            this.mPrinterRegistry.setTrackedPrinter(null);
            this.mPrinterRegistry.setOnPrintersChangeListener(null);
        }
        if (this.mPrintersObserver != null) {
            this.mDestinationSpinnerAdapter.unregisterDataSetObserver(this.mPrintersObserver);
        }
        if (this.mSpoolerProvider != null) {
            this.mSpoolerProvider.destroy();
        }
        if (this.mProgressMessageController != null) {
            setState(this.mProgressMessageController.cancel());
        }
        if (this.mState != 0) {
            this.mPrintedDocument.finish();
            this.mPrintedDocument.destroy();
            this.mPrintPreviewController.destroy(new Runnable() {
                @Override
                public void run() {
                    PrintActivity.this.finish();
                }
            });
            return;
        }
        finish();
    }

    private final class SpinnerItem<T> {
        final CharSequence label;
        final T value;

        public SpinnerItem(T t, CharSequence charSequence) {
            this.value = t;
            this.label = charSequence;
        }

        public String toString() {
            return this.label.toString();
        }
    }

    private final class PrinterAvailabilityDetector implements Runnable {
        private boolean mPosted;
        private PrinterInfo mPrinter;
        private boolean mPrinterUnavailable;

        private PrinterAvailabilityDetector() {
        }

        public void updatePrinter(PrinterInfo printerInfo) {
            if (printerInfo.equals(PrintActivity.this.mDestinationSpinnerAdapter.getPdfPrinter())) {
                return;
            }
            boolean z = true;
            boolean z2 = (printerInfo.getStatus() == 3 || printerInfo.getCapabilities() == null) ? false : true;
            if (this.mPrinter == null || !this.mPrinter.getId().equals(printerInfo.getId())) {
                unpostIfNeeded();
                this.mPrinterUnavailable = false;
                this.mPrinter = new PrinterInfo.Builder(printerInfo).build();
            } else {
                if ((this.mPrinter.getStatus() != 3 || printerInfo.getStatus() == 3) && (this.mPrinter.getCapabilities() != null || printerInfo.getCapabilities() == null)) {
                    z = false;
                }
                this.mPrinter = printerInfo;
            }
            if (z2) {
                unpostIfNeeded();
                this.mPrinterUnavailable = false;
                if (z) {
                    PrintActivity.this.onPrinterAvailable(this.mPrinter);
                    return;
                }
                return;
            }
            if (!this.mPrinterUnavailable) {
                postIfNeeded();
            }
        }

        public void cancel() {
            unpostIfNeeded();
            this.mPrinterUnavailable = false;
        }

        private void postIfNeeded() {
            if (!this.mPosted) {
                this.mPosted = true;
                PrintActivity.this.mDestinationSpinner.postDelayed(this, 10000L);
            }
        }

        private void unpostIfNeeded() {
            if (this.mPosted) {
                this.mPosted = false;
                PrintActivity.this.mDestinationSpinner.removeCallbacks(this);
            }
        }

        @Override
        public void run() {
            this.mPosted = false;
            this.mPrinterUnavailable = true;
            PrintActivity.this.onPrinterUnavailable(this.mPrinter);
        }
    }

    private static final class PrinterHolder {
        PrinterInfo printer;
        boolean removed;

        public PrinterHolder(PrinterInfo printerInfo) {
            this.printer = printerInfo;
        }
    }

    private boolean hasUserEverPrinted() {
        return getSharedPreferences("has_printed", 0).getBoolean("has_printed", false);
    }

    private void setUserPrinted() {
        SharedPreferences sharedPreferences = getSharedPreferences("has_printed", 0);
        if (!sharedPreferences.getBoolean("has_printed", false)) {
            SharedPreferences.Editor editorEdit = sharedPreferences.edit();
            editorEdit.putBoolean("has_printed", true);
            editorEdit.apply();
        }
    }

    private final class DestinationAdapter extends BaseAdapter implements PrinterRegistry.OnPrintersChangeListener {
        private boolean hadPromptView;
        private final PrinterHolder mFakePdfPrinterHolder;
        private boolean mHistoricalPrintersLoaded;
        private final List<PrinterHolder> mPrinterHolders = new ArrayList();

        public DestinationAdapter() {
            this.mHistoricalPrintersLoaded = PrintActivity.this.mPrinterRegistry.areHistoricalPrintersLoaded();
            if (this.mHistoricalPrintersLoaded) {
                addPrinters(this.mPrinterHolders, PrintActivity.this.mPrinterRegistry.getPrinters());
            }
            PrintActivity.this.mPrinterRegistry.setOnPrintersChangeListener(this);
            this.mFakePdfPrinterHolder = new PrinterHolder(createFakePdfPrinter());
        }

        public PrinterInfo getPdfPrinter() {
            return this.mFakePdfPrinterHolder.printer;
        }

        public int getPrinterIndex(PrinterId printerId) {
            for (int i = 0; i < getCount(); i++) {
                PrinterHolder printerHolder = (PrinterHolder) getItem(i);
                if (printerHolder != null && printerHolder.printer.getId().equals(printerId)) {
                    return i;
                }
            }
            return -1;
        }

        public void ensurePrinterInVisibleAdapterPosition(PrinterInfo printerInfo) {
            boolean z;
            int size = this.mPrinterHolders.size();
            int i = 0;
            while (true) {
                if (i < size) {
                    PrinterHolder printerHolder = this.mPrinterHolders.get(i);
                    if (!printerHolder.printer.getId().equals(printerInfo.getId())) {
                        i++;
                    } else {
                        if (i >= getCount() - 2) {
                            int count = getCount() - 3;
                            this.mPrinterHolders.set(i, this.mPrinterHolders.get(count));
                            this.mPrinterHolders.set(count, printerHolder);
                        }
                        z = true;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                PrinterHolder printerHolder2 = new PrinterHolder(printerInfo);
                printerHolder2.removed = true;
                this.mPrinterHolders.add(Math.max(0, getCount() - 3), printerHolder2);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            if (this.mHistoricalPrintersLoaded) {
                return Math.min(this.mPrinterHolders.size() + 2, 9);
            }
            return 0;
        }

        @Override
        public boolean isEnabled(int i) {
            Object item = getItem(i);
            if (!(item instanceof PrinterHolder)) {
                return true;
            }
            PrinterHolder printerHolder = (PrinterHolder) item;
            return (printerHolder.removed || printerHolder.printer.getStatus() == 3) ? false : true;
        }

        @Override
        public Object getItem(int i) {
            if (this.mPrinterHolders.isEmpty()) {
                if (i == 0) {
                    return this.mFakePdfPrinterHolder;
                }
                return null;
            }
            if (i < 1) {
                return this.mPrinterHolders.get(i);
            }
            if (i != 1) {
                if (i < getCount() - 1) {
                    return this.mPrinterHolders.get(i - 1);
                }
                return null;
            }
            return this.mFakePdfPrinterHolder;
        }

        @Override
        public long getItemId(int i) {
            if (this.mPrinterHolders.isEmpty()) {
                if (i == 0) {
                    return 2147483647L;
                }
                if (i == 1) {
                    return 2147483646L;
                }
            } else {
                if (i == 1) {
                    return 2147483647L;
                }
                if (i == getCount() - 1) {
                    return 2147483646L;
                }
            }
            return i;
        }

        @Override
        public View getDropDownView(int i, View view, ViewGroup viewGroup) {
            View view2 = getView(i, view, viewGroup);
            view2.setEnabled(isEnabled(i));
            return view2;
        }

        private String getMoreItemTitle() {
            if (PrintActivity.this.mArePrintServicesEnabled) {
                return PrintActivity.this.getString(R.string.all_printers);
            }
            return PrintActivity.this.getString(R.string.print_add_printer);
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            String description;
            String moreItemTitle;
            Drawable drawable;
            ?? r4;
            if (PrintActivity.this.mShowDestinationPrompt) {
                if (view == null) {
                    View viewInflate = PrintActivity.this.getLayoutInflater().inflate(R.layout.printer_dropdown_prompt, viewGroup, false);
                    this.hadPromptView = true;
                    return viewInflate;
                }
                return view;
            }
            if (this.hadPromptView || view == null) {
                view = PrintActivity.this.getLayoutInflater().inflate(R.layout.printer_dropdown_item, viewGroup, false);
            }
            if (this.mPrinterHolders.isEmpty()) {
                if (i == 0 && getPdfPrinter() != null) {
                    moreItemTitle = ((PrinterHolder) getItem(i)).printer.getName();
                    drawable = PrintActivity.this.getResources().getDrawable(R.drawable.ic_pdf_printer, null);
                    r4 = drawable;
                    description = null;
                } else if (i == 1) {
                    moreItemTitle = getMoreItemTitle();
                    description = null;
                    r4 = description;
                } else {
                    moreItemTitle = null;
                    description = null;
                    r4 = description;
                }
            } else if (i == 1 && getPdfPrinter() != null) {
                moreItemTitle = ((PrinterHolder) getItem(i)).printer.getName();
                drawable = PrintActivity.this.getResources().getDrawable(R.drawable.ic_pdf_printer, null);
                r4 = drawable;
                description = null;
            } else if (i == getCount() - 1) {
                moreItemTitle = getMoreItemTitle();
                description = null;
                r4 = description;
            } else {
                PrinterInfo printerInfo = ((PrinterHolder) getItem(i)).printer;
                String name = printerInfo.getName();
                Drawable drawableLoadIcon = printerInfo.loadIcon(PrintActivity.this);
                description = printerInfo.getDescription();
                moreItemTitle = name;
                r4 = drawableLoadIcon;
            }
            ((TextView) view.findViewById(R.id.title)).setText(moreItemTitle);
            TextView textView = (TextView) view.findViewById(R.id.subtitle);
            if (!TextUtils.isEmpty(description)) {
                textView.setText(description);
                textView.setVisibility(0);
            } else {
                textView.setText((CharSequence) null);
                textView.setVisibility(8);
            }
            ?? r10 = (ImageView) view.findViewById(R.id.icon);
            if (r4 != 0) {
                r10.setVisibility(0);
                if (!isEnabled(i)) {
                    r4.mutate();
                    TypedValue typedValue = new TypedValue();
                    PrintActivity.this.getTheme().resolveAttribute(android.R.attr.disabledAlpha, typedValue, true);
                    r4.setAlpha((int) (typedValue.getFloat() * 255.0f));
                }
                r10.setImageDrawable(r4);
            } else {
                r10.setVisibility(4);
            }
            return view;
        }

        @Override
        public void onPrintersChanged(List<PrinterInfo> list) {
            this.mHistoricalPrintersLoaded = PrintActivity.this.mPrinterRegistry.areHistoricalPrintersLoaded();
            if (this.mPrinterHolders.isEmpty()) {
                addPrinters(this.mPrinterHolders, list);
                notifyDataSetChanged();
                return;
            }
            ArrayMap arrayMap = new ArrayMap();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                PrinterInfo printerInfo = list.get(i);
                arrayMap.put(printerInfo.getId(), printerInfo);
            }
            ArrayList arrayList = new ArrayList();
            int size2 = this.mPrinterHolders.size();
            for (int i2 = 0; i2 < size2; i2++) {
                PrinterHolder printerHolder = this.mPrinterHolders.get(i2);
                PrinterId id = printerHolder.printer.getId();
                PrinterInfo printerInfo2 = (PrinterInfo) arrayMap.remove(id);
                if (printerInfo2 == null) {
                    if (PrintActivity.this.mCurrentPrinter != null && PrintActivity.this.mCurrentPrinter.getId().equals(id)) {
                        printerHolder.removed = true;
                        PrintActivity.this.onPrinterUnavailable(printerHolder.printer);
                        arrayList.add(printerHolder);
                    }
                } else {
                    printerHolder.printer = printerInfo2;
                    printerHolder.removed = false;
                    if (PrintActivity.canPrint(printerHolder.printer)) {
                        PrintActivity.this.onPrinterAvailable(printerHolder.printer);
                    } else {
                        PrintActivity.this.onPrinterUnavailable(printerHolder.printer);
                    }
                    arrayList.add(printerHolder);
                }
            }
            addPrinters(arrayList, arrayMap.values());
            this.mPrinterHolders.clear();
            this.mPrinterHolders.addAll(arrayList);
            notifyDataSetChanged();
        }

        @Override
        public void onPrintersInvalid() {
            this.mPrinterHolders.clear();
            notifyDataSetInvalidated();
        }

        public PrinterHolder getPrinterHolder(PrinterId printerId) {
            int count = getCount();
            for (int i = 0; i < count; i++) {
                ?? item = getItem(i);
                if ((item instanceof PrinterHolder) && printerId.equals(item.printer.getId())) {
                    return item;
                }
            }
            return null;
        }

        public boolean pruneRemovedPrinter(PrinterId printerId) {
            for (int size = this.mPrinterHolders.size() - 1; size >= 0; size--) {
                PrinterHolder printerHolder = this.mPrinterHolders.get(size);
                if (printerHolder.printer.getId().equals(printerId) && printerHolder.removed) {
                    this.mPrinterHolders.remove(size);
                    return true;
                }
            }
            return false;
        }

        private void addPrinters(List<PrinterHolder> list, Collection<PrinterInfo> collection) {
            Iterator<PrinterInfo> it = collection.iterator();
            while (it.hasNext()) {
                list.add(new PrinterHolder(it.next()));
            }
        }

        private PrinterInfo createFakePdfPrinter() {
            ArraySet allPredefinedSizes = PrintAttributes.MediaSize.getAllPredefinedSizes();
            PrintAttributes.MediaSize mediaSize = MediaSizeUtils.getDefault(PrintActivity.this);
            PrinterId printerId = new PrinterId(PrintActivity.this.getComponentName(), "PDF printer");
            PrinterCapabilitiesInfo.Builder builder = new PrinterCapabilitiesInfo.Builder(printerId);
            int size = allPredefinedSizes.size();
            for (int i = 0; i < size; i++) {
                PrintAttributes.MediaSize mediaSize2 = (PrintAttributes.MediaSize) allPredefinedSizes.valueAt(i);
                builder.addMediaSize(mediaSize2, mediaSize2.equals(mediaSize));
            }
            builder.addResolution(new PrintAttributes.Resolution("PDF resolution", "PDF resolution", 300, 300), true);
            builder.setColorModes(3, 2);
            return new PrinterInfo.Builder(printerId, PrintActivity.this.getString(R.string.save_as_pdf), 1).setCapabilities(builder.build()).build();
        }
    }

    private final class PrintersObserver extends DataSetObserver {
        private PrintersObserver() {
        }

        @Override
        public void onChanged() {
            PrinterInfo printerInfo = PrintActivity.this.mCurrentPrinter;
            if (printerInfo == null) {
                return;
            }
            PrinterHolder printerHolder = PrintActivity.this.mDestinationSpinnerAdapter.getPrinterHolder(printerInfo.getId());
            PrinterInfo printerInfo2 = printerHolder.printer;
            if (printerHolder.removed) {
                PrintActivity.this.onPrinterUnavailable(printerInfo2);
            }
            if (PrintActivity.this.mDestinationSpinner.getSelectedItem() != printerHolder) {
                PrintActivity.this.mDestinationSpinner.setSelection(PrintActivity.this.mDestinationSpinnerAdapter.getPrinterIndex(printerInfo2.getId()));
            }
            if (printerInfo.equals(printerInfo2)) {
                return;
            }
            PrinterCapabilitiesInfo capabilities = printerInfo.getCapabilities();
            PrinterCapabilitiesInfo capabilities2 = printerInfo2.getCapabilities();
            boolean z = capabilities != null;
            boolean z2 = capabilities2 != null;
            boolean z3 = capabilities == null && capabilities2 != null;
            boolean z4 = capabilities != null && capabilities2 == null;
            boolean zCapabilitiesChanged = capabilitiesChanged(capabilities, capabilities2);
            int status = printerInfo.getStatus();
            int status2 = printerInfo2.getStatus();
            boolean z5 = status2 != 3;
            boolean z6 = status == 3 && status != status2;
            boolean z7 = status2 == 3 && status != status2;
            PrintActivity.this.mPrinterAvailabilityDetector.updatePrinter(printerInfo2);
            PrintActivity.this.mCurrentPrinter = printerInfo2;
            boolean z8 = (zCapabilitiesChanged && z2 && z5) || (z6 && z2) || (z5 && z3);
            if (zCapabilitiesChanged && z2) {
                PrintActivity.this.updatePrintAttributesFromCapabilities(capabilities2);
            }
            if (z8) {
                PrintActivity.this.updatePrintPreviewController(false);
            }
            if ((z5 && z3) || (z6 && z2)) {
                PrintActivity.this.onPrinterAvailable(printerInfo2);
            } else if ((z7 && z) || (z5 && z4)) {
                PrintActivity.this.onPrinterUnavailable(printerInfo2);
            }
            if (z8 && PrintActivity.this.canUpdateDocument()) {
                PrintActivity.this.updateDocument(false);
            }
            PrintActivity.this.getLoaderManager().getLoader(1).forceLoad();
            PrintActivity.this.updateOptionsUi();
            PrintActivity.this.updateSummary();
        }

        private boolean capabilitiesChanged(PrinterCapabilitiesInfo printerCapabilitiesInfo, PrinterCapabilitiesInfo printerCapabilitiesInfo2) {
            if (printerCapabilitiesInfo == null) {
                if (printerCapabilitiesInfo2 != null) {
                    return true;
                }
                return false;
            }
            if (!printerCapabilitiesInfo.equals(printerCapabilitiesInfo2)) {
                return true;
            }
            return false;
        }
    }

    private final class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        private MyOnItemSelectedListener() {
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            PrintAttributes.MediaSize mediaSizeAsLandscape;
            boolean z = true;
            if (adapterView != PrintActivity.this.mDestinationSpinner) {
                if (adapterView == PrintActivity.this.mMediaSizeSpinner) {
                    SpinnerItem spinnerItem = (SpinnerItem) PrintActivity.this.mMediaSizeSpinnerAdapter.getItem(i);
                    PrintAttributes attributes = PrintActivity.this.mPrintJob.getAttributes();
                    if (PrintActivity.this.mOrientationSpinner.getSelectedItemPosition() == 0) {
                        mediaSizeAsLandscape = ((PrintAttributes.MediaSize) spinnerItem.value).asPortrait();
                    } else {
                        mediaSizeAsLandscape = ((PrintAttributes.MediaSize) spinnerItem.value).asLandscape();
                    }
                    if (mediaSizeAsLandscape != attributes.getMediaSize()) {
                        if (!mediaSizeAsLandscape.equals(attributes.getMediaSize()) && !attributes.getMediaSize().equals(PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE) && !attributes.getMediaSize().equals(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT) && PrintActivity.this.mState != 0) {
                            MetricsLogger.action(PrintActivity.this, 508, 4);
                        }
                        attributes.setMediaSize(mediaSizeAsLandscape);
                    } else {
                        z = false;
                    }
                } else if (adapterView != PrintActivity.this.mColorModeSpinner) {
                    if (adapterView != PrintActivity.this.mDuplexModeSpinner) {
                        if (adapterView == PrintActivity.this.mOrientationSpinner) {
                            SpinnerItem spinnerItem2 = (SpinnerItem) PrintActivity.this.mOrientationSpinnerAdapter.getItem(i);
                            PrintAttributes attributes2 = PrintActivity.this.mPrintJob.getAttributes();
                            if (PrintActivity.this.mMediaSizeSpinner.getSelectedItem() != null) {
                                boolean zIsPortrait = attributes2.isPortrait();
                                boolean z2 = ((Integer) spinnerItem2.value).intValue() == 0;
                                if (zIsPortrait != z2) {
                                    if (PrintActivity.this.mState != 0) {
                                        MetricsLogger.action(PrintActivity.this, 508, 5);
                                    }
                                    if (z2) {
                                        attributes2.copyFrom(attributes2.asPortrait());
                                    } else {
                                        attributes2.copyFrom(attributes2.asLandscape());
                                    }
                                } else {
                                    z = false;
                                }
                            }
                        } else if (adapterView == PrintActivity.this.mRangeOptionsSpinner) {
                            if (PrintActivity.this.mRangeOptionsSpinner.getSelectedItemPosition() == 0) {
                                PrintActivity.this.mPageRangeEditText.setText("");
                                if (PrintActivity.this.mPageRangeEditText.getVisibility() == 0 && PrintActivity.this.mState != 0) {
                                    MetricsLogger.action(PrintActivity.this, 508, 6);
                                }
                            } else if (TextUtils.isEmpty(PrintActivity.this.mPageRangeEditText.getText())) {
                                PrintActivity.this.mPageRangeEditText.setError("");
                                if (PrintActivity.this.mPageRangeEditText.getVisibility() != 0 && PrintActivity.this.mState != 0) {
                                    MetricsLogger.action(PrintActivity.this, 508, 6);
                                }
                            }
                        }
                    } else {
                        int iIntValue = ((Integer) ((SpinnerItem) PrintActivity.this.mDuplexModeSpinnerAdapter.getItem(i)).value).intValue();
                        if (PrintActivity.this.mPrintJob.getAttributes().getDuplexMode() != iIntValue && PrintActivity.this.mState != 0) {
                            MetricsLogger.action(PrintActivity.this, 508, 3);
                        }
                        PrintActivity.this.mPrintJob.getAttributes().setDuplexMode(iIntValue);
                    }
                } else {
                    int iIntValue2 = ((Integer) ((SpinnerItem) PrintActivity.this.mColorModeSpinnerAdapter.getItem(i)).value).intValue();
                    if (PrintActivity.this.mPrintJob.getAttributes().getColorMode() != iIntValue2 && PrintActivity.this.mState != 0) {
                        MetricsLogger.action(PrintActivity.this, 508, 2);
                    }
                    PrintActivity.this.mPrintJob.getAttributes().setColorMode(iIntValue2);
                }
                if (z) {
                    PrintActivity.this.clearPageRanges();
                }
                PrintActivity.this.updateOptionsUi();
                if (!PrintActivity.this.canUpdateDocument()) {
                    PrintActivity.this.updateDocument(false);
                    return;
                }
                return;
            }
            if (i == -1) {
                return;
            }
            if (j == 2147483646) {
                PrintActivity.this.startSelectPrinterActivity();
                return;
            }
            PrinterHolder printerHolder = (PrinterHolder) PrintActivity.this.mDestinationSpinner.getSelectedItem();
            PrinterId id = null;
            PrinterInfo printerInfo = printerHolder != null ? printerHolder.printer : null;
            if (PrintActivity.this.mCurrentPrinter != printerInfo) {
                if (PrintActivity.this.mDefaultPrinter == null) {
                    PrintActivity.this.mDefaultPrinter = printerInfo.getId();
                }
                if (PrintActivity.this.mCurrentPrinter != null) {
                    id = PrintActivity.this.mCurrentPrinter.getId();
                }
                PrintActivity.this.mCurrentPrinter = printerInfo;
                if (id != null) {
                    if (PrintActivity.this.mDestinationSpinnerAdapter.pruneRemovedPrinter(id)) {
                        PrintActivity.this.mDestinationSpinnerAdapter.notifyDataSetChanged();
                        return;
                    } else if (PrintActivity.this.mState != 0) {
                        if (printerInfo == null) {
                            MetricsLogger.action(PrintActivity.this, 506, "");
                        } else {
                            MetricsLogger.action(PrintActivity.this, 506, printerInfo.getId().getServiceName().getPackageName());
                        }
                    }
                }
                if (!PrintActivity.this.mDestinationSpinnerAdapter.getPrinterHolder(printerInfo.getId()).removed) {
                    PrintActivity.this.setState(1);
                    PrintActivity.this.ensurePreviewUiShown();
                }
                PrintActivity.this.mPrintJob.setPrinterId(printerInfo.getId());
                PrintActivity.this.mPrintJob.setPrinterName(printerInfo.getName());
                PrintActivity.this.mPrinterRegistry.setTrackedPrinter(printerInfo.getId());
                PrinterCapabilitiesInfo capabilities = printerInfo.getCapabilities();
                if (capabilities != null) {
                    PrintActivity.this.updatePrintAttributesFromCapabilities(capabilities);
                }
                PrintActivity.this.mPrinterAvailabilityDetector.updatePrinter(printerInfo);
                PrintActivity.this.getLoaderManager().getLoader(1).forceLoad();
            } else {
                return;
            }
            z = false;
            if (z) {
            }
            PrintActivity.this.updateOptionsUi();
            if (!PrintActivity.this.canUpdateDocument()) {
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    }

    private final class SelectAllOnFocusListener implements View.OnFocusChangeListener {
        private SelectAllOnFocusListener() {
        }

        @Override
        public void onFocusChange(View view, boolean z) {
            EditText editText = (EditText) view;
            if (!TextUtils.isEmpty(editText.getText())) {
                editText.setSelection(editText.getText().length());
            }
            if (view == PrintActivity.this.mPageRangeEditText && !z && PrintActivity.this.mPageRangeEditText.getError() == null) {
                PrintActivity.this.updateSelectedPagesFromTextField();
            }
        }
    }

    private final class RangeTextWatcher implements TextWatcher {
        private RangeTextWatcher() {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            int adjustedPageCount;
            boolean zHasErrors = PrintActivity.this.hasErrors();
            PrintDocumentInfo printDocumentInfo = PrintActivity.this.mPrintedDocument.getDocumentInfo().info;
            if (printDocumentInfo != null) {
                adjustedPageCount = PrintActivity.this.getAdjustedPageCount(printDocumentInfo);
            } else {
                adjustedPageCount = 0;
            }
            if (PageRangeUtils.parsePageRanges(editable, adjustedPageCount).length == 0) {
                if (PrintActivity.this.mPageRangeEditText.getError() == null) {
                    PrintActivity.this.mPageRangeEditText.setError("");
                    PrintActivity.this.updateOptionsUi();
                    return;
                }
                return;
            }
            if (PrintActivity.this.mPageRangeEditText.getError() != null) {
                PrintActivity.this.mPageRangeEditText.setError(null);
                PrintActivity.this.updateOptionsUi();
            }
            if (zHasErrors && PrintActivity.this.canUpdateDocument()) {
                PrintActivity.this.updateDocument(false);
            }
        }
    }

    private final class EditTextWatcher implements TextWatcher {
        private EditTextWatcher() {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            int i;
            boolean zHasErrors = PrintActivity.this.hasErrors();
            if (editable.length() == 0) {
                if (PrintActivity.this.mCopiesEditText.getError() == null) {
                    PrintActivity.this.mCopiesEditText.setError("");
                    PrintActivity.this.updateOptionsUi();
                    return;
                }
                return;
            }
            try {
                i = Integer.parseInt(editable.toString());
            } catch (NumberFormatException e) {
                i = 0;
            }
            if (PrintActivity.this.mState != 0) {
                MetricsLogger.action(PrintActivity.this, 508, 1);
            }
            if (i < 1) {
                if (PrintActivity.this.mCopiesEditText.getError() == null) {
                    PrintActivity.this.mCopiesEditText.setError("");
                    PrintActivity.this.updateOptionsUi();
                    return;
                }
                return;
            }
            PrintActivity.this.mPrintJob.setCopies(i);
            if (PrintActivity.this.mCopiesEditText.getError() != null) {
                PrintActivity.this.mCopiesEditText.setError(null);
                PrintActivity.this.updateOptionsUi();
            }
            if (zHasErrors && PrintActivity.this.canUpdateDocument()) {
                PrintActivity.this.updateDocument(false);
            }
        }
    }

    private final class ProgressMessageController implements Runnable {
        private final Handler mHandler;
        private boolean mPosted;
        private int mPreviousState = -1;

        public ProgressMessageController(Context context) {
            this.mHandler = new Handler(context.getMainLooper(), null, false);
        }

        public void post() {
            if (PrintActivity.this.mState == 7) {
                PrintActivity.this.setState(7);
                PrintActivity.this.ensureProgressUiShown();
            } else {
                if (this.mPosted) {
                    return;
                }
                this.mPreviousState = -1;
                this.mPosted = true;
                this.mHandler.postDelayed(this, 1000L);
            }
        }

        private int getStateAfterCancel() {
            if (this.mPreviousState == -1) {
                return PrintActivity.this.mState;
            }
            return this.mPreviousState;
        }

        public int cancel() {
            int stateAfterCancel;
            if (!this.mPosted) {
                stateAfterCancel = getStateAfterCancel();
            } else {
                this.mPosted = false;
                this.mHandler.removeCallbacks(this);
                stateAfterCancel = getStateAfterCancel();
            }
            this.mPreviousState = -1;
            return stateAfterCancel;
        }

        @Override
        public void run() {
            this.mPosted = false;
            this.mPreviousState = PrintActivity.this.mState;
            PrintActivity.this.setState(7);
            PrintActivity.this.ensureProgressUiShown();
        }
    }

    private static final class DocumentTransformer implements ServiceConnection {
        private final PrintAttributes mAttributesToApply;
        private final Consumer<String> mCallback;
        private final Context mContext;
        private final MutexFileProvider mFileProvider;
        private boolean mIsTransformationStarted;
        private final PageRange[] mPagesToShred;
        private final PrintJobInfo mPrintJob;

        public DocumentTransformer(Context context, PrintJobInfo printJobInfo, MutexFileProvider mutexFileProvider, PrintAttributes printAttributes, Consumer<String> consumer) {
            this.mContext = context;
            this.mPrintJob = printJobInfo;
            this.mFileProvider = mutexFileProvider;
            this.mCallback = consumer;
            this.mPagesToShred = computePagesToShred(this.mPrintJob);
            this.mAttributesToApply = printAttributes;
        }

        public void transform() {
            if (this.mPagesToShred.length <= 0 && this.mAttributesToApply == null) {
                this.mCallback.accept(null);
                return;
            }
            Intent intent = new Intent("com.android.printspooler.renderer.ACTION_GET_EDITOR");
            intent.setClass(this.mContext, PdfManipulationService.class);
            this.mContext.bindService(intent, this, 1);
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (!this.mIsTransformationStarted) {
                final IPdfEditor iPdfEditorAsInterface = IPdfEditor.Stub.asInterface(iBinder);
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... voidArr) {
                        try {
                            DocumentTransformer.this.doTransform(iPdfEditorAsInterface);
                            DocumentTransformer.this.updatePrintJob();
                            return null;
                        } catch (RemoteException | IOException | IllegalStateException e) {
                            return e.toString();
                        }
                    }

                    @Override
                    protected void onPostExecute(String str) {
                        DocumentTransformer.this.mContext.unbindService(DocumentTransformer.this);
                        DocumentTransformer.this.mCallback.accept(str);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                this.mIsTransformationStarted = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }

        private void doTransform(IPdfEditor iPdfEditor) throws Throwable {
            FileInputStream fileInputStream;
            ParcelFileDescriptor parcelFileDescriptorOpen;
            File fileCreateTempFile;
            ?? r5;
            File fileAcquireFile;
            ParcelFileDescriptor parcelFileDescriptorOpen2;
            FileOutputStream fileOutputStream;
            FileInputStream fileInputStream2;
            ParcelFileDescriptor parcelFileDescriptor = null;
            try {
                fileAcquireFile = this.mFileProvider.acquireFile(null);
                parcelFileDescriptorOpen = ParcelFileDescriptor.open(fileAcquireFile, 805306368);
            } catch (Throwable th) {
                th = th;
                fileInputStream = null;
                parcelFileDescriptorOpen = null;
                fileCreateTempFile = null;
            }
            try {
                iPdfEditor.openDocument(parcelFileDescriptorOpen);
                parcelFileDescriptorOpen.close();
                iPdfEditor.removePages(this.mPagesToShred);
                if (this.mAttributesToApply != null) {
                    iPdfEditor.applyPrintAttributes(this.mAttributesToApply);
                }
                fileCreateTempFile = File.createTempFile("print_job", ".pdf", this.mContext.getCacheDir());
                try {
                    parcelFileDescriptorOpen2 = ParcelFileDescriptor.open(fileCreateTempFile, 805306368);
                    try {
                        iPdfEditor.write(parcelFileDescriptorOpen2);
                        parcelFileDescriptorOpen2.close();
                        iPdfEditor.closeDocument();
                        fileAcquireFile.delete();
                        fileInputStream2 = new FileInputStream(fileCreateTempFile);
                    } catch (Throwable th2) {
                        th = th2;
                        fileInputStream = null;
                        fileOutputStream = null;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fileInputStream = null;
                    r5 = 0;
                }
                try {
                    FileOutputStream fileOutputStream2 = new FileOutputStream(fileAcquireFile);
                    try {
                        Streams.copy(fileInputStream2, fileOutputStream2);
                        IoUtils.closeQuietly(parcelFileDescriptorOpen);
                        IoUtils.closeQuietly(parcelFileDescriptorOpen2);
                        IoUtils.closeQuietly(fileInputStream2);
                        IoUtils.closeQuietly(fileOutputStream2);
                        if (fileCreateTempFile != null) {
                            fileCreateTempFile.delete();
                        }
                        this.mFileProvider.releaseFile();
                    } catch (Throwable th4) {
                        fileInputStream = fileInputStream2;
                        th = th4;
                        fileOutputStream = fileOutputStream2;
                        parcelFileDescriptor = parcelFileDescriptorOpen2;
                        r5 = fileOutputStream;
                        IoUtils.closeQuietly(parcelFileDescriptorOpen);
                        IoUtils.closeQuietly(parcelFileDescriptor);
                        IoUtils.closeQuietly(fileInputStream);
                        IoUtils.closeQuietly((AutoCloseable) r5);
                        if (fileCreateTempFile != null) {
                            fileCreateTempFile.delete();
                        }
                        this.mFileProvider.releaseFile();
                        throw th;
                    }
                } catch (Throwable th5) {
                    r5 = 0;
                    parcelFileDescriptor = parcelFileDescriptorOpen2;
                    fileInputStream = fileInputStream2;
                    th = th5;
                    IoUtils.closeQuietly(parcelFileDescriptorOpen);
                    IoUtils.closeQuietly(parcelFileDescriptor);
                    IoUtils.closeQuietly(fileInputStream);
                    IoUtils.closeQuietly((AutoCloseable) r5);
                    if (fileCreateTempFile != null) {
                    }
                    this.mFileProvider.releaseFile();
                    throw th;
                }
            } catch (Throwable th6) {
                th = th6;
                fileInputStream = null;
                fileCreateTempFile = null;
                r5 = fileCreateTempFile;
                IoUtils.closeQuietly(parcelFileDescriptorOpen);
                IoUtils.closeQuietly(parcelFileDescriptor);
                IoUtils.closeQuietly(fileInputStream);
                IoUtils.closeQuietly((AutoCloseable) r5);
                if (fileCreateTempFile != null) {
                }
                this.mFileProvider.releaseFile();
                throw th;
            }
        }

        private void updatePrintJob() {
            int normalizedPageCount = PageRangeUtils.getNormalizedPageCount(this.mPrintJob.getPages(), 0);
            this.mPrintJob.setPages(new PageRange[]{PageRange.ALL_PAGES});
            PrintDocumentInfo documentInfo = this.mPrintJob.getDocumentInfo();
            PrintDocumentInfo printDocumentInfoBuild = new PrintDocumentInfo.Builder(documentInfo.getName()).setContentType(documentInfo.getContentType()).setPageCount(normalizedPageCount).build();
            try {
                printDocumentInfoBuild.setDataSize(this.mFileProvider.acquireFile(null).length());
                this.mFileProvider.releaseFile();
                this.mPrintJob.setDocumentInfo(printDocumentInfoBuild);
            } catch (Throwable th) {
                this.mFileProvider.releaseFile();
                throw th;
            }
        }

        private static PageRange[] computePagesToShred(PrintJobInfo printJobInfo) {
            ArrayList arrayList = new ArrayList();
            PageRange[] pages = printJobInfo.getPages();
            int length = pages.length;
            PageRange pageRange = null;
            int i = 0;
            while (i < length) {
                PageRange pageRange2 = pages[i];
                if (pageRange == null) {
                    int start = pageRange2.getStart() - 1;
                    if (start >= 0) {
                        arrayList.add(new PageRange(0, start));
                    }
                } else {
                    int end = pageRange.getEnd() + 1;
                    int start2 = pageRange2.getStart() - 1;
                    if (end <= start2) {
                        arrayList.add(new PageRange(end, start2));
                    }
                }
                if (i == length - 1 && pageRange2.getEnd() != Integer.MAX_VALUE) {
                    arrayList.add(new PageRange(pageRange2.getEnd() + 1, Integer.MAX_VALUE));
                }
                i++;
                pageRange = pageRange2;
            }
            PageRange[] pageRangeArr = new PageRange[arrayList.size()];
            arrayList.toArray(pageRangeArr);
            return pageRangeArr;
        }
    }
}
