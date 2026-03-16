package com.android.printspooler.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
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
import android.print.IPrintDocumentAdapter;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.print.PrintJobInfo;
import android.print.PrinterCapabilitiesInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.util.Log;
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
import com.android.printspooler.util.MediaSizeUtils;
import com.android.printspooler.util.PageRangeUtils;
import com.android.printspooler.util.PrintOptionUtils;
import com.android.printspooler.widget.PrintContentView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class PrintActivity extends Activity implements RemotePrintDocument.UpdateResultCallbacks, PageAdapter.ContentCallbacks, PrintErrorFragment.OnActionListener, PrintContentView.OptionsStateChangeListener, PrintContentView.OptionsStateController {
    private String mCallingPackageName;
    private Spinner mColorModeSpinner;
    private ArrayAdapter<SpinnerItem<Integer>> mColorModeSpinnerAdapter;
    private EditText mCopiesEditText;
    private int mCurrentPageCount;
    private PrinterInfo mCurrentPrinter;
    private Spinner mDestinationSpinner;
    private DestinationAdapter mDestinationSpinnerAdapter;
    private MutexFileProvider mFileProvider;
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
    private ProgressMessageController mProgressMessageController;
    private Spinner mRangeOptionsSpinner;
    private final View.OnFocusChangeListener mSelectAllOnFocusListener;
    private PageRange[] mSelectedPages;
    private PrintSpoolerProvider mSpoolerProvider;
    private View mSummaryContainer;
    private TextView mSummaryCopies;
    private TextView mSummaryPaperSize;
    private static final String MIN_COPIES_STRING = String.valueOf(1);
    private static final Pattern PATTERN_DIGITS = Pattern.compile("[\\d]+");
    private static final Pattern PATTERN_ESCAPE_SPECIAL_CHARS = Pattern.compile("(?=[]\\[+&|!(){}^\"~*?:\\\\])");
    private static final Pattern PATTERN_PAGE_RANGE = Pattern.compile("[\\s]*[0-9]+[\\-]?[\\s]*[0-9]*[\\s]*?(([,])[\\s]*[0-9]+[\\s]*[\\-]?[\\s]*[0-9]*[\\s]*|[\\s]*)+");
    public static final PageRange[] ALL_PAGES_ARRAY = {PageRange.ALL_PAGES};
    private final TextUtils.SimpleStringSplitter mStringCommaSplitter = new TextUtils.SimpleStringSplitter(',');
    private int mState = 0;
    private int mUiState = 0;

    public PrintActivity() {
        this.mPrinterAvailabilityDetector = new PrinterAvailabilityDetector();
        this.mSelectAllOnFocusListener = new SelectAllOnFocusListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        this.mPrintJob = (PrintJobInfo) extras.getParcelable("android.print.intent.extra.EXTRA_PRINT_JOB");
        if (this.mPrintJob == null) {
            throw new IllegalArgumentException("android.print.intent.extra.EXTRA_PRINT_JOB cannot be null");
        }
        this.mPrintJob.setAttributes(new PrintAttributes.Builder().build());
        final IBinder adapter = extras.getBinder("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER");
        if (adapter == null) {
            throw new IllegalArgumentException("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER cannot be null");
        }
        this.mCallingPackageName = extras.getString("android.content.extra.PACKAGE_NAME");
        this.mSpoolerProvider = new PrintSpoolerProvider(this, new Runnable() {
            @Override
            public void run() {
                PrintActivity.this.onConnectedToPrintSpooler(adapter);
            }
        });
    }

    private void onConnectedToPrintSpooler(final IBinder documentAdapter) {
        this.mPrinterRegistry = new PrinterRegistry(this, new Runnable() {
            @Override
            public void run() {
                PrintActivity.this.onPrinterRegistryReady(documentAdapter);
            }
        });
    }

    private void onPrinterRegistryReady(IBinder documentAdapter) {
        setTitle(R.string.print_dialog);
        setContentView(R.layout.print_activity);
        try {
            this.mFileProvider = new MutexFileProvider(PrintSpoolerService.generateFileForPrintJob(this, this.mPrintJob.getId()));
            this.mPrintPreviewController = new PrintPreviewController(this, this.mFileProvider);
            this.mPrintedDocument = new RemotePrintDocument(this, IPrintDocumentAdapter.Stub.asInterface(documentAdapter), this.mFileProvider, new RemotePrintDocument.RemoteAdapterDeathObserver() {
                @Override
                public void onDied() {
                    if (!PrintActivity.this.isFinishing()) {
                        if (!PrintActivity.isFinalState(PrintActivity.this.mState) || PrintActivity.this.mPrintedDocument.isUpdating()) {
                            if (PrintActivity.this.mPrintedDocument.isUpdating()) {
                                PrintActivity.this.mPrintedDocument.cancel();
                            }
                            PrintActivity.this.setState(3);
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
        } catch (IOException ioe) {
            throw new IllegalStateException("Cannot create print job file", ioe);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mState != 0 && this.mCurrentPrinter != null) {
            this.mPrinterRegistry.setTrackedPrinter(this.mCurrentPrinter.getId());
        }
    }

    @Override
    public void onPause() {
        PrintSpoolerService spooler = this.mSpoolerProvider.getSpooler();
        if (this.mState == 0) {
            if (isFinishing()) {
                spooler.setPrintJobState(this.mPrintJob.getId(), 7, null);
            }
            super.onPause();
            return;
        }
        if (isFinishing()) {
            spooler.updatePrintJobUserConfigurableOptionsNoPersistence(this.mPrintJob);
            switch (this.mState) {
                case 2:
                    spooler.setPrintJobState(this.mPrintJob.getId(), 2, null);
                    break;
                case 5:
                    spooler.setPrintJobState(this.mPrintJob.getId(), 6, getString(R.string.print_write_error_message));
                    break;
                case 8:
                    spooler.setPrintJobState(this.mPrintJob.getId(), 5, null);
                    break;
                default:
                    spooler.setPrintJobState(this.mPrintJob.getId(), 7, null);
                    break;
            }
        }
        this.mPrinterAvailabilityDetector.cancel();
        this.mPrinterRegistry.setTrackedPrinter(null);
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 4) {
            return super.onKeyDown(keyCode, event);
        }
        event.startTracking();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (this.mState == 0) {
            doFinish();
            return true;
        }
        if (this.mState == 3 || this.mState == 2 || this.mState == 8) {
            return true;
        }
        if (keyCode == 4 && event.isTracking() && !event.isCanceled()) {
            if (this.mPrintPreviewController != null && this.mPrintPreviewController.isOptionsOpened() && !hasErrors()) {
                this.mPrintPreviewController.closeOptions();
                return true;
            }
            cancelPrint();
            return true;
        }
        return super.onKeyUp(keyCode, event);
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

    private void onPrintDocumentError(String message) {
        this.mProgressMessageController.cancel();
        ensureErrorUiShown(null, 1);
        setState(4);
        updateOptionsUi();
        this.mPrintedDocument.kill(message);
    }

    @Override
    public void onActionPerformed() {
        if (this.mState == 4 && canUpdateDocument() && updateDocument(true)) {
            ensurePreviewUiShown();
            setState(1);
            updateOptionsUi();
        }
    }

    @Override
    public void onUpdateCanceled() {
        this.mProgressMessageController.cancel();
        ensurePreviewUiShown();
        switch (this.mState) {
            case 2:
                requestCreatePdfFileOrFinish();
                break;
            case 3:
                doFinish();
                break;
        }
    }

    @Override
    public void onUpdateCompleted(RemotePrintDocument.RemotePrintDocumentInfo document) {
        this.mProgressMessageController.cancel();
        ensurePreviewUiShown();
        PrintDocumentInfo info = document.info;
        if (info != null) {
            int pageCount = PageRangeUtils.getNormalizedPageCount(document.writtenPages, getAdjustedPageCount(info));
            PrintDocumentInfo adjustedInfo = new PrintDocumentInfo.Builder(info.getName()).setContentType(info.getContentType()).setPageCount(pageCount).build();
            this.mPrintJob.setDocumentInfo(adjustedInfo);
            this.mPrintJob.setPages(document.printedPages);
        }
        switch (this.mState) {
            case 2:
                requestCreatePdfFileOrFinish();
                break;
            case 3:
                updateOptionsUi();
                break;
            default:
                updatePrintPreviewController(document.changed);
                setState(1);
                updateOptionsUi();
                break;
        }
    }

    @Override
    public void onUpdateFailed(CharSequence error) {
        this.mProgressMessageController.cancel();
        ensureErrorUiShown(error, 1);
        setState(4);
        updateOptionsUi();
    }

    @Override
    public void onOptionsOpened() {
        updateSelectedPagesFromPreview();
    }

    @Override
    public void onOptionsClosed() {
        PageRange[] selectedPages = computeSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, selectedPages)) {
            this.mSelectedPages = selectedPages;
            updatePrintPreviewController(false);
        }
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        imm.hideSoftInputFromWindow(this.mDestinationSpinner.getWindowToken(), 0);
    }

    private void updatePrintPreviewController(boolean contentUpdated) {
        RemotePrintDocument.RemotePrintDocumentInfo documentInfo = this.mPrintedDocument.getDocumentInfo();
        if (documentInfo.laidout) {
            this.mPrintPreviewController.onContentUpdated(contentUpdated, getAdjustedPageCount(documentInfo.info), this.mPrintedDocument.getDocumentInfo().writtenPages, this.mSelectedPages, this.mPrintJob.getAttributes().getMediaSize(), this.mPrintJob.getAttributes().getMinMargins());
        }
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mPrintPreviewController != null) {
            this.mPrintPreviewController.onOrientationChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                onStartCreateDocumentActivityResult(resultCode, data);
                break;
            case 2:
                onSelectPrinterActivityResult(resultCode, data);
                break;
            case 3:
                onAdvancedPrintOptionsActivityResult(resultCode, data);
                break;
        }
    }

    private void startCreateDocumentActivity() {
        PrintDocumentInfo info;
        if (isResumed() && (info = this.mPrintedDocument.getDocumentInfo().info) != null) {
            Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
            intent.setType("application/pdf");
            intent.putExtra("android.intent.extra.TITLE", info.getName());
            intent.putExtra("android.content.extra.PACKAGE_NAME", this.mCallingPackageName);
            startActivityForResult(intent, 1);
        }
    }

    private void onStartCreateDocumentActivityResult(int resultCode, Intent data) {
        if (resultCode == -1 && data != null) {
            setState(8);
            updateOptionsUi();
            final Uri uri = data.getData();
            this.mDestinationSpinner.post(new Runnable() {
                @Override
                public void run() {
                    PrintActivity.this.transformDocumentAndFinish(uri);
                }
            });
            return;
        }
        if (resultCode == 0) {
            this.mState = 1;
            updateOptionsUi();
        } else {
            setState(5);
            updateOptionsUi();
            this.mDestinationSpinner.post(new Runnable() {
                @Override
                public void run() {
                    PrintActivity.this.doFinish();
                }
            });
        }
    }

    private void startSelectPrinterActivity() {
        Intent intent = new Intent(this, (Class<?>) SelectPrinterActivity.class);
        startActivityForResult(intent, 2);
    }

    private void onSelectPrinterActivityResult(int resultCode, Intent data) {
        PrinterId printerId;
        if (resultCode == -1 && data != null && (printerId = (PrinterId) data.getParcelableExtra("INTENT_EXTRA_PRINTER_ID")) != null) {
            this.mDestinationSpinnerAdapter.ensurePrinterInVisibleAdapterPosition(printerId);
            int index = this.mDestinationSpinnerAdapter.getPrinterIndex(printerId);
            if (index != -1) {
                this.mDestinationSpinner.setSelection(index);
                return;
            }
        }
        this.mDestinationSpinner.setSelection(this.mDestinationSpinnerAdapter.getPrinterIndex(this.mCurrentPrinter.getId()));
    }

    private void startAdvancedPrintOptionsActivity(PrinterInfo printer) {
        ComponentName serviceName = printer.getId().getServiceName();
        String activityName = PrintOptionUtils.getAdvancedOptionsActivityName(this, serviceName);
        if (!TextUtils.isEmpty(activityName)) {
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(new ComponentName(serviceName.getPackageName(), activityName));
            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(intent, 0);
            if (!resolvedActivities.isEmpty() && resolvedActivities.get(0).activityInfo.exported) {
                intent.putExtra("android.intent.extra.print.PRINT_JOB_INFO", this.mPrintJob);
                intent.putExtra("android.intent.extra.print.EXTRA_PRINTER_INFO", printer);
                try {
                    startActivityForResult(intent, 3);
                } catch (ActivityNotFoundException anfe) {
                    Log.e("PrintActivity", "Error starting activity for intent: " + intent, anfe);
                }
            }
        }
    }

    private void onAdvancedPrintOptionsActivityResult(int resultCode, Intent data) {
        PrintJobInfo printJobInfo;
        PrinterCapabilitiesInfo capabilities;
        if (resultCode == -1 && data != null && (printJobInfo = (PrintJobInfo) data.getParcelableExtra("android.intent.extra.print.PRINT_JOB_INFO")) != null) {
            this.mPrintJob.setAdvancedOptions(printJobInfo.getAdvancedOptions());
            this.mCopiesEditText.setText(String.valueOf(printJobInfo.getCopies()));
            this.mPrintJob.setCopies(printJobInfo.getCopies());
            PrintAttributes currAttributes = this.mPrintJob.getAttributes();
            PrintAttributes newAttributes = printJobInfo.getAttributes();
            if (newAttributes != null) {
                PrintAttributes.MediaSize oldMediaSize = currAttributes.getMediaSize();
                PrintAttributes.MediaSize newMediaSize = newAttributes.getMediaSize();
                if (!oldMediaSize.equals(newMediaSize)) {
                    int mediaSizeCount = this.mMediaSizeSpinnerAdapter.getCount();
                    PrintAttributes.MediaSize newMediaSizePortrait = newAttributes.getMediaSize().asPortrait();
                    int i = 0;
                    while (true) {
                        if (i >= mediaSizeCount) {
                            break;
                        }
                        PrintAttributes.MediaSize supportedSizePortrait = this.mMediaSizeSpinnerAdapter.getItem(i).value.asPortrait();
                        if (!supportedSizePortrait.equals(newMediaSizePortrait)) {
                            i++;
                        } else {
                            currAttributes.setMediaSize(newMediaSize);
                            this.mMediaSizeSpinner.setSelection(i);
                            if (currAttributes.getMediaSize().isPortrait()) {
                                if (this.mOrientationSpinner.getSelectedItemPosition() != 0) {
                                    this.mOrientationSpinner.setSelection(0);
                                }
                            } else if (this.mOrientationSpinner.getSelectedItemPosition() != 1) {
                                this.mOrientationSpinner.setSelection(1);
                            }
                        }
                    }
                }
                PrintAttributes.Resolution oldResolution = currAttributes.getResolution();
                PrintAttributes.Resolution newResolution = newAttributes.getResolution();
                if (!oldResolution.equals(newResolution) && (capabilities = this.mCurrentPrinter.getCapabilities()) != null) {
                    List<PrintAttributes.Resolution> resolutions = capabilities.getResolutions();
                    int resolutionCount = resolutions.size();
                    int i2 = 0;
                    while (true) {
                        if (i2 >= resolutionCount) {
                            break;
                        }
                        PrintAttributes.Resolution resolution = resolutions.get(i2);
                        if (!resolution.equals(newResolution)) {
                            i2++;
                        } else {
                            currAttributes.setResolution(resolution);
                            break;
                        }
                    }
                }
                int currColorMode = currAttributes.getColorMode();
                int newColorMode = newAttributes.getColorMode();
                if (currColorMode != newColorMode) {
                    int colorModeCount = this.mColorModeSpinner.getCount();
                    int i3 = 0;
                    while (true) {
                        if (i3 >= colorModeCount) {
                            break;
                        }
                        int supportedColorMode = this.mColorModeSpinnerAdapter.getItem(i3).value.intValue();
                        if (supportedColorMode != newColorMode) {
                            i3++;
                        } else {
                            currAttributes.setColorMode(newColorMode);
                            this.mColorModeSpinner.setSelection(i3);
                            break;
                        }
                    }
                }
            }
            PrintDocumentInfo info = this.mPrintedDocument.getDocumentInfo().info;
            int pageCount = info != null ? getAdjustedPageCount(info) : 0;
            PageRange[] pageRanges = printJobInfo.getPages();
            if (pageRanges != null && pageCount > 0) {
                PageRange[] pageRanges2 = PageRangeUtils.normalize(pageRanges);
                List<PageRange> validatedList = new ArrayList<>();
                int rangeCount = pageRanges2.length;
                int i4 = 0;
                while (true) {
                    if (i4 >= rangeCount) {
                        break;
                    }
                    PageRange pageRange = pageRanges2[i4];
                    if (pageRange.getEnd() >= pageCount) {
                        int rangeStart = pageRange.getStart();
                        int rangeEnd = pageCount - 1;
                        if (rangeStart <= rangeEnd) {
                            validatedList.add(new PageRange(rangeStart, rangeEnd));
                        }
                    } else {
                        validatedList.add(pageRange);
                        i4++;
                    }
                }
                if (!validatedList.isEmpty()) {
                    PageRange[] validatedArray = new PageRange[validatedList.size()];
                    validatedList.toArray(validatedArray);
                    updateSelectedPages(validatedArray, pageCount);
                }
            }
            if (canUpdateDocument()) {
                updateDocument(false);
            }
        }
    }

    private void setState(int state) {
        if (isFinalState(this.mState)) {
            if (isFinalState(state)) {
                this.mState = state;
                return;
            }
            return;
        }
        this.mState = state;
    }

    private static boolean isFinalState(int state) {
        return state == 2 || state == 3 || state == 8;
    }

    private void updateSelectedPagesFromPreview() {
        PageRange[] selectedPages = this.mPrintPreviewController.getSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, selectedPages)) {
            updateSelectedPages(selectedPages, getAdjustedPageCount(this.mPrintedDocument.getDocumentInfo().info));
        }
    }

    private void updateSelectedPages(PageRange[] selectedPages, int pageInDocumentCount) {
        int shownStartPage;
        int shownEndPage;
        if (selectedPages != null && selectedPages.length > 0) {
            PageRange[] selectedPages2 = PageRangeUtils.normalize(selectedPages);
            if (PageRangeUtils.isAllPages(selectedPages2, pageInDocumentCount)) {
                selectedPages2 = new PageRange[]{PageRange.ALL_PAGES};
            }
            if (!Arrays.equals(this.mSelectedPages, selectedPages2)) {
                this.mSelectedPages = selectedPages2;
                this.mPrintJob.setPages(selectedPages2);
                if (Arrays.equals(selectedPages2, ALL_PAGES_ARRAY)) {
                    if (this.mRangeOptionsSpinner.getSelectedItemPosition() != 0) {
                        this.mRangeOptionsSpinner.setSelection(0);
                        this.mPageRangeEditText.setText("");
                        return;
                    }
                    return;
                }
                if (selectedPages2[0].getStart() >= 0 && selectedPages2[selectedPages2.length - 1].getEnd() < pageInDocumentCount) {
                    if (this.mRangeOptionsSpinner.getSelectedItemPosition() != 1) {
                        this.mRangeOptionsSpinner.setSelection(1);
                    }
                    StringBuilder builder = new StringBuilder();
                    for (PageRange pageRange : selectedPages2) {
                        if (builder.length() > 0) {
                            builder.append(',');
                        }
                        if (pageRange.equals(PageRange.ALL_PAGES)) {
                            shownStartPage = 1;
                            shownEndPage = pageInDocumentCount;
                        } else {
                            shownStartPage = pageRange.getStart() + 1;
                            shownEndPage = pageRange.getEnd() + 1;
                        }
                        builder.append(shownStartPage);
                        if (shownStartPage != shownEndPage) {
                            builder.append('-');
                            builder.append(shownEndPage);
                        }
                    }
                    this.mPageRangeEditText.setText(builder.toString());
                }
            }
        }
    }

    private void ensureProgressUiShown() {
        if (!isFinishing() && this.mUiState != 2) {
            this.mUiState = 2;
            this.mPrintPreviewController.setUiShown(false);
            Fragment fragment = PrintProgressFragment.newInstance();
            showFragment(fragment);
        }
    }

    private void ensurePreviewUiShown() {
        if (!isFinishing() && this.mUiState != 0) {
            this.mUiState = 0;
            this.mPrintPreviewController.setUiShown(true);
            showFragment(null);
        }
    }

    private void ensureErrorUiShown(CharSequence message, int action) {
        if (!isFinishing() && this.mUiState != 1) {
            this.mUiState = 1;
            this.mPrintPreviewController.setUiShown(false);
            Fragment fragment = PrintErrorFragment.newInstance(message, action);
            showFragment(fragment);
        }
    }

    private void showFragment(Fragment newFragment) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        Fragment oldFragment = getFragmentManager().findFragmentByTag("FRAGMENT_TAG");
        if (oldFragment != null) {
            transaction.remove(oldFragment);
        }
        if (newFragment != null) {
            transaction.add(R.id.embedded_content_container, newFragment, "FRAGMENT_TAG");
        }
        transaction.commit();
        getFragmentManager().executePendingTransactions();
    }

    private void requestCreatePdfFileOrFinish() {
        if (this.mCurrentPrinter == this.mDestinationSpinnerAdapter.getPdfPrinter()) {
            startCreateDocumentActivity();
        } else {
            transformDocumentAndFinish(null);
        }
    }

    private void updatePrintAttributesFromCapabilities(PrinterCapabilitiesInfo capabilities) {
        PrintAttributes defaults = capabilities.getDefaults();
        List<PrintAttributes.MediaSize> sortedMediaSizes = new ArrayList<>(capabilities.getMediaSizes());
        Collections.sort(sortedMediaSizes, this.mMediaSizeComparator);
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        PrintAttributes.MediaSize currMediaSize = attributes.getMediaSize();
        if (currMediaSize == null) {
            attributes.setMediaSize(defaults.getMediaSize());
        } else {
            boolean foundCurrentMediaSize = false;
            PrintAttributes.MediaSize currMediaSizePortrait = currMediaSize.asPortrait();
            int mediaSizeCount = sortedMediaSizes.size();
            int i = 0;
            while (true) {
                if (i >= mediaSizeCount) {
                    break;
                }
                PrintAttributes.MediaSize mediaSize = sortedMediaSizes.get(i);
                if (!currMediaSizePortrait.equals(mediaSize.asPortrait())) {
                    i++;
                } else {
                    attributes.setMediaSize(currMediaSize);
                    foundCurrentMediaSize = true;
                    break;
                }
            }
            if (!foundCurrentMediaSize) {
                attributes.setMediaSize(defaults.getMediaSize());
            }
        }
        int colorMode = attributes.getColorMode();
        if ((capabilities.getColorModes() & colorMode) == 0) {
            attributes.setColorMode(defaults.getColorMode());
        }
        PrintAttributes.Resolution resolution = attributes.getResolution();
        if (resolution == null || !capabilities.getResolutions().contains(resolution)) {
            attributes.setResolution(defaults.getResolution());
        }
        attributes.setMinMargins(defaults.getMinMargins());
    }

    private boolean updateDocument(boolean clearLastError) {
        PageRange[] pages;
        if (!clearLastError && this.mPrintedDocument.hasUpdateError()) {
            return false;
        }
        if (clearLastError && this.mPrintedDocument.hasUpdateError()) {
            this.mPrintedDocument.clearUpdateError();
        }
        boolean preview = this.mState != 2;
        if (preview) {
            pages = this.mPrintPreviewController.getRequestedPages();
        } else {
            pages = this.mPrintPreviewController.getSelectedPages();
        }
        boolean willUpdate = this.mPrintedDocument.update(this.mPrintJob.getAttributes(), pages, preview);
        if (willUpdate && !this.mPrintedDocument.hasLaidOutPages()) {
            this.mProgressMessageController.post();
            return true;
        }
        if (willUpdate) {
            return false;
        }
        updatePrintPreviewController(false);
        return false;
    }

    private void addCurrentPrinterToHistory() {
        if (this.mCurrentPrinter != null) {
            PrinterId fakePdfPrinterId = this.mDestinationSpinnerAdapter.getPdfPrinter().getId();
            if (!this.mCurrentPrinter.getId().equals(fakePdfPrinterId)) {
                this.mPrinterRegistry.addHistoricalPrinter(this.mCurrentPrinter);
            }
        }
    }

    private void cancelPrint() {
        setState(3);
        updateOptionsUi();
        if (this.mPrintedDocument.isUpdating()) {
            this.mPrintedDocument.cancel();
        }
        doFinish();
    }

    private void confirmPrint() {
        setState(2);
        updateOptionsUi();
        addCurrentPrinterToHistory();
        PageRange[] selectedPages = computeSelectedPages();
        if (!Arrays.equals(this.mSelectedPages, selectedPages)) {
            this.mSelectedPages = selectedPages;
            updatePrintPreviewController(false);
        }
        updateSelectedPagesFromPreview();
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
        AdapterView.OnItemSelectedListener itemSelectedListener = new MyOnItemSelectedListener();
        View.OnClickListener clickListener = new MyClickListener();
        this.mCopiesEditText = (EditText) findViewById(R.id.copies_edittext);
        this.mCopiesEditText.setOnFocusChangeListener(this.mSelectAllOnFocusListener);
        this.mCopiesEditText.setText(MIN_COPIES_STRING);
        this.mCopiesEditText.setSelection(this.mCopiesEditText.getText().length());
        this.mCopiesEditText.addTextChangedListener(new EditTextWatcher());
        this.mDestinationSpinnerAdapter.registerDataSetObserver(new PrintersObserver());
        this.mDestinationSpinner = (Spinner) findViewById(R.id.destination_spinner);
        this.mDestinationSpinner.setAdapter((SpinnerAdapter) this.mDestinationSpinnerAdapter);
        this.mDestinationSpinner.setOnItemSelectedListener(itemSelectedListener);
        this.mMediaSizeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mMediaSizeSpinner = (Spinner) findViewById(R.id.paper_size_spinner);
        this.mMediaSizeSpinner.setAdapter((SpinnerAdapter) this.mMediaSizeSpinnerAdapter);
        this.mMediaSizeSpinner.setOnItemSelectedListener(itemSelectedListener);
        this.mColorModeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mColorModeSpinner = (Spinner) findViewById(R.id.color_spinner);
        this.mColorModeSpinner.setAdapter((SpinnerAdapter) this.mColorModeSpinnerAdapter);
        this.mColorModeSpinner.setOnItemSelectedListener(itemSelectedListener);
        this.mOrientationSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        String[] orientationLabels = getResources().getStringArray(R.array.orientation_labels);
        this.mOrientationSpinnerAdapter.add(new SpinnerItem<>(0, orientationLabels[0]));
        this.mOrientationSpinnerAdapter.add(new SpinnerItem<>(1, orientationLabels[1]));
        this.mOrientationSpinner = (Spinner) findViewById(R.id.orientation_spinner);
        this.mOrientationSpinner.setAdapter((SpinnerAdapter) this.mOrientationSpinnerAdapter);
        this.mOrientationSpinner.setOnItemSelectedListener(itemSelectedListener);
        ArrayAdapter<SpinnerItem<Integer>> rangeOptionsSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, android.R.id.text1);
        this.mRangeOptionsSpinner = (Spinner) findViewById(R.id.range_options_spinner);
        this.mRangeOptionsSpinner.setAdapter((SpinnerAdapter) rangeOptionsSpinnerAdapter);
        this.mRangeOptionsSpinner.setOnItemSelectedListener(itemSelectedListener);
        updatePageRangeOptions(-1);
        this.mPageRangeTitle = (TextView) findViewById(R.id.page_range_title);
        this.mPageRangeEditText = (EditText) findViewById(R.id.page_range_edittext);
        this.mPageRangeEditText.setOnFocusChangeListener(this.mSelectAllOnFocusListener);
        this.mPageRangeEditText.addTextChangedListener(new RangeTextWatcher());
        this.mMoreOptionsButton = (Button) findViewById(R.id.more_options_button);
        this.mMoreOptionsButton.setOnClickListener(clickListener);
        this.mPrintButton = (ImageView) findViewById(R.id.print_button);
        this.mPrintButton.setOnClickListener(clickListener);
    }

    private final class MyClickListener implements View.OnClickListener {
        private MyClickListener() {
        }

        @Override
        public void onClick(View view) {
            if (view == PrintActivity.this.mPrintButton) {
                if (PrintActivity.this.mCurrentPrinter != null) {
                    PrintActivity.this.confirmPrint();
                    return;
                } else {
                    PrintActivity.this.cancelPrint();
                    return;
                }
            }
            if (view == PrintActivity.this.mMoreOptionsButton && PrintActivity.this.mCurrentPrinter != null) {
                PrintActivity.this.startAdvancedPrintOptionsActivity(PrintActivity.this.mCurrentPrinter);
            }
        }
    }

    private static boolean canPrint(PrinterInfo printer) {
        return (printer.getCapabilities() == null || printer.getStatus() == 3) ? false : true;
    }

    void updateOptionsUi() {
        updateSummary();
        if (this.mState == 2 || this.mState == 8 || this.mState == 3 || this.mState == 4 || this.mState == 5 || this.mState == 6 || this.mState == 7) {
            if (this.mState != 6) {
                this.mDestinationSpinner.setEnabled(false);
            }
            this.mCopiesEditText.setEnabled(false);
            this.mCopiesEditText.setFocusable(false);
            this.mMediaSizeSpinner.setEnabled(false);
            this.mColorModeSpinner.setEnabled(false);
            this.mOrientationSpinner.setEnabled(false);
            this.mRangeOptionsSpinner.setEnabled(false);
            this.mPageRangeEditText.setEnabled(false);
            this.mPrintButton.setVisibility(8);
            this.mMoreOptionsButton.setEnabled(false);
            return;
        }
        if (this.mCurrentPrinter == null || !canPrint(this.mCurrentPrinter)) {
            this.mCopiesEditText.setEnabled(false);
            this.mCopiesEditText.setFocusable(false);
            this.mMediaSizeSpinner.setEnabled(false);
            this.mColorModeSpinner.setEnabled(false);
            this.mOrientationSpinner.setEnabled(false);
            this.mRangeOptionsSpinner.setEnabled(false);
            this.mPageRangeEditText.setEnabled(false);
            this.mPrintButton.setVisibility(8);
            this.mMoreOptionsButton.setEnabled(false);
            return;
        }
        PrinterCapabilitiesInfo capabilities = this.mCurrentPrinter.getCapabilities();
        PrintAttributes defaultAttributes = capabilities.getDefaults();
        this.mDestinationSpinner.setEnabled(true);
        this.mMediaSizeSpinner.setEnabled(true);
        List<PrintAttributes.MediaSize> mediaSizes = new ArrayList<>(capabilities.getMediaSizes());
        Collections.sort(mediaSizes, this.mMediaSizeComparator);
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        boolean mediaSizesChanged = false;
        int mediaSizeCount = mediaSizes.size();
        if (mediaSizeCount != this.mMediaSizeSpinnerAdapter.getCount()) {
            mediaSizesChanged = true;
        } else {
            int i = 0;
            while (true) {
                if (i >= mediaSizeCount) {
                    break;
                }
                if (mediaSizes.get(i).equals(this.mMediaSizeSpinnerAdapter.getItem(i).value)) {
                    i++;
                } else {
                    mediaSizesChanged = true;
                    break;
                }
            }
        }
        if (mediaSizesChanged) {
            int oldMediaSizeNewIndex = -1;
            PrintAttributes.MediaSize oldMediaSize = attributes.getMediaSize();
            this.mMediaSizeSpinnerAdapter.clear();
            for (int i2 = 0; i2 < mediaSizeCount; i2++) {
                PrintAttributes.MediaSize mediaSize = mediaSizes.get(i2);
                if (oldMediaSize != null && mediaSize.asPortrait().equals(oldMediaSize.asPortrait())) {
                    oldMediaSizeNewIndex = i2;
                }
                this.mMediaSizeSpinnerAdapter.add(new SpinnerItem<>(mediaSize, mediaSize.getLabel(getPackageManager())));
            }
            if (oldMediaSizeNewIndex != -1) {
                if (this.mMediaSizeSpinner.getSelectedItemPosition() != oldMediaSizeNewIndex) {
                    this.mMediaSizeSpinner.setSelection(oldMediaSizeNewIndex);
                }
            } else {
                int mediaSizeIndex = Math.max(mediaSizes.indexOf(defaultAttributes.getMediaSize()), 0);
                if (this.mMediaSizeSpinner.getSelectedItemPosition() != mediaSizeIndex) {
                    this.mMediaSizeSpinner.setSelection(mediaSizeIndex);
                }
                if (oldMediaSize != null) {
                    if (oldMediaSize.isPortrait()) {
                        attributes.setMediaSize(this.mMediaSizeSpinnerAdapter.getItem(mediaSizeIndex).value.asPortrait());
                    } else {
                        attributes.setMediaSize(this.mMediaSizeSpinnerAdapter.getItem(mediaSizeIndex).value.asLandscape());
                    }
                }
            }
        }
        this.mColorModeSpinner.setEnabled(true);
        int colorModes = capabilities.getColorModes();
        boolean colorModesChanged = false;
        if (Integer.bitCount(colorModes) != this.mColorModeSpinnerAdapter.getCount()) {
            colorModesChanged = true;
        } else {
            int remainingColorModes = colorModes;
            int adapterIndex = 0;
            while (true) {
                if (remainingColorModes == 0) {
                    break;
                }
                int colorMode = 1 << Integer.numberOfTrailingZeros(remainingColorModes);
                remainingColorModes &= colorMode ^ (-1);
                if (colorMode != this.mColorModeSpinnerAdapter.getItem(adapterIndex).value.intValue()) {
                    colorModesChanged = true;
                    break;
                }
                adapterIndex++;
            }
        }
        if (colorModesChanged) {
            int oldColorModeNewIndex = -1;
            int oldColorMode = attributes.getColorMode();
            this.mColorModeSpinnerAdapter.clear();
            String[] colorModeLabels = getResources().getStringArray(R.array.color_mode_labels);
            int remainingColorModes2 = colorModes;
            while (remainingColorModes2 != 0) {
                int colorBitOffset = Integer.numberOfTrailingZeros(remainingColorModes2);
                int colorMode2 = 1 << colorBitOffset;
                if (colorMode2 == oldColorMode) {
                    oldColorModeNewIndex = colorBitOffset;
                }
                remainingColorModes2 &= colorMode2 ^ (-1);
                this.mColorModeSpinnerAdapter.add(new SpinnerItem<>(Integer.valueOf(colorMode2), colorModeLabels[colorBitOffset]));
            }
            if (oldColorModeNewIndex != -1) {
                if (this.mColorModeSpinner.getSelectedItemPosition() != oldColorModeNewIndex) {
                    this.mColorModeSpinner.setSelection(oldColorModeNewIndex);
                }
            } else {
                int selectedColorMode = colorModes & defaultAttributes.getColorMode();
                int itemCount = this.mColorModeSpinnerAdapter.getCount();
                for (int i3 = 0; i3 < itemCount; i3++) {
                    SpinnerItem<Integer> item = this.mColorModeSpinnerAdapter.getItem(i3);
                    if (selectedColorMode == item.value.intValue()) {
                        if (this.mColorModeSpinner.getSelectedItemPosition() != i3) {
                            this.mColorModeSpinner.setSelection(i3);
                        }
                        attributes.setColorMode(selectedColorMode);
                    }
                }
            }
        }
        this.mOrientationSpinner.setEnabled(true);
        PrintAttributes.MediaSize mediaSize2 = attributes.getMediaSize();
        if (mediaSize2 != null) {
            if (mediaSize2.isPortrait() && this.mOrientationSpinner.getSelectedItemPosition() != 0) {
                this.mOrientationSpinner.setSelection(0);
            } else if (!mediaSize2.isPortrait() && this.mOrientationSpinner.getSelectedItemPosition() != 1) {
                this.mOrientationSpinner.setSelection(1);
            }
        }
        PrintDocumentInfo info = this.mPrintedDocument.getDocumentInfo().info;
        int pageCount = getAdjustedPageCount(info);
        if (info != null && pageCount > 0) {
            if (pageCount == 1) {
                this.mRangeOptionsSpinner.setEnabled(false);
            } else {
                this.mRangeOptionsSpinner.setEnabled(true);
                if (this.mRangeOptionsSpinner.getSelectedItemPosition() > 0) {
                    if (!this.mPageRangeEditText.isEnabled()) {
                        this.mPageRangeEditText.setEnabled(true);
                        this.mPageRangeEditText.setVisibility(0);
                        this.mPageRangeTitle.setVisibility(0);
                        this.mPageRangeEditText.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                        imm.showSoftInput(this.mPageRangeEditText, 0);
                    }
                } else {
                    this.mPageRangeEditText.setEnabled(false);
                    this.mPageRangeEditText.setVisibility(4);
                    this.mPageRangeTitle.setVisibility(4);
                }
            }
        } else {
            if (this.mRangeOptionsSpinner.getSelectedItemPosition() != 0) {
                this.mRangeOptionsSpinner.setSelection(0);
                this.mPageRangeEditText.setText("");
            }
            this.mRangeOptionsSpinner.setEnabled(false);
            this.mPageRangeEditText.setEnabled(false);
            this.mPageRangeEditText.setVisibility(4);
            this.mPageRangeTitle.setVisibility(4);
        }
        int newPageCount = getAdjustedPageCount(info);
        if (newPageCount != this.mCurrentPageCount) {
            this.mCurrentPageCount = newPageCount;
            updatePageRangeOptions(newPageCount);
        }
        ComponentName serviceName = this.mCurrentPrinter.getId().getServiceName();
        if (!TextUtils.isEmpty(PrintOptionUtils.getAdvancedOptionsActivityName(this, serviceName))) {
            this.mMoreOptionsButton.setVisibility(0);
            this.mMoreOptionsButton.setEnabled(true);
        } else {
            this.mMoreOptionsButton.setVisibility(8);
            this.mMoreOptionsButton.setEnabled(false);
        }
        if (this.mDestinationSpinnerAdapter.getPdfPrinter() != this.mCurrentPrinter) {
            this.mPrintButton.setImageResource(android.R.drawable.ic_drag_handle);
            this.mPrintButton.setContentDescription(getString(R.string.print_button));
        } else {
            this.mPrintButton.setImageResource(R.drawable.ic_menu_savetopdf);
            this.mPrintButton.setContentDescription(getString(R.string.savetopdf_button));
        }
        if (!this.mPrintedDocument.getDocumentInfo().laidout || ((this.mRangeOptionsSpinner.getSelectedItemPosition() == 1 && (TextUtils.isEmpty(this.mPageRangeEditText.getText()) || hasErrors())) || (this.mRangeOptionsSpinner.getSelectedItemPosition() == 0 && (this.mPrintedDocument.getDocumentInfo() == null || hasErrors())))) {
            this.mPrintButton.setVisibility(8);
        } else {
            this.mPrintButton.setVisibility(0);
        }
        if (this.mDestinationSpinnerAdapter.getPdfPrinter() != this.mCurrentPrinter) {
            this.mCopiesEditText.setEnabled(true);
            this.mCopiesEditText.setFocusableInTouchMode(true);
        } else {
            CharSequence text = this.mCopiesEditText.getText();
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
    }

    private void updateSummary() {
        CharSequence copiesText = null;
        CharSequence mediaSizeText = null;
        if (!TextUtils.isEmpty(this.mCopiesEditText.getText())) {
            copiesText = this.mCopiesEditText.getText();
            this.mSummaryCopies.setText(copiesText);
        }
        int selectedMediaIndex = this.mMediaSizeSpinner.getSelectedItemPosition();
        if (selectedMediaIndex >= 0) {
            SpinnerItem<PrintAttributes.MediaSize> mediaItem = this.mMediaSizeSpinnerAdapter.getItem(selectedMediaIndex);
            mediaSizeText = mediaItem.label;
            this.mSummaryPaperSize.setText(mediaSizeText);
        }
        if (!TextUtils.isEmpty(copiesText) && !TextUtils.isEmpty(mediaSizeText)) {
            String summaryText = getString(R.string.summary_template, new Object[]{copiesText, mediaSizeText});
            this.mSummaryContainer.setContentDescription(summaryText);
        }
    }

    private void updatePageRangeOptions(int pageCount) {
        ArrayAdapter<SpinnerItem<Integer>> rangeOptionsSpinnerAdapter = (ArrayAdapter) this.mRangeOptionsSpinner.getAdapter();
        rangeOptionsSpinnerAdapter.clear();
        int[] rangeOptionsValues = getResources().getIntArray(R.array.page_options_values);
        String pageCountLabel = pageCount > 0 ? String.valueOf(pageCount) : "";
        String[] rangeOptionsLabels = {getString(R.string.template_all_pages, new Object[]{pageCountLabel}), getString(R.string.template_page_range, new Object[]{pageCountLabel})};
        int rangeOptionsCount = rangeOptionsLabels.length;
        for (int i = 0; i < rangeOptionsCount; i++) {
            rangeOptionsSpinnerAdapter.add(new SpinnerItem<>(Integer.valueOf(rangeOptionsValues[i]), rangeOptionsLabels[i]));
        }
    }

    private PageRange[] computeSelectedPages() {
        int toIndex;
        int fromIndex;
        if (hasErrors()) {
            return null;
        }
        if (this.mRangeOptionsSpinner.getSelectedItemPosition() > 0) {
            List<PageRange> pageRanges = new ArrayList<>();
            this.mStringCommaSplitter.setString(this.mPageRangeEditText.getText().toString());
            while (this.mStringCommaSplitter.hasNext()) {
                String range = this.mStringCommaSplitter.next().trim();
                if (!TextUtils.isEmpty(range)) {
                    int dashIndex = range.indexOf(45);
                    if (dashIndex > 0) {
                        fromIndex = Integer.parseInt(range.substring(0, dashIndex).trim()) - 1;
                        if (dashIndex < range.length() - 1) {
                            String fromString = range.substring(dashIndex + 1, range.length()).trim();
                            toIndex = Integer.parseInt(fromString) - 1;
                        } else {
                            toIndex = fromIndex;
                        }
                    } else {
                        toIndex = Integer.parseInt(range) - 1;
                        fromIndex = toIndex;
                    }
                    PageRange pageRange = new PageRange(Math.min(fromIndex, toIndex), Math.max(fromIndex, toIndex));
                    pageRanges.add(pageRange);
                }
            }
            PageRange[] pageRangesArray = new PageRange[pageRanges.size()];
            pageRanges.toArray(pageRangesArray);
            return PageRangeUtils.normalize(pageRangesArray);
        }
        return ALL_PAGES_ARRAY;
    }

    private int getAdjustedPageCount(PrintDocumentInfo info) {
        int pageCount;
        return (info == null || (pageCount = info.getPageCount()) == -1) ? this.mPrintPreviewController.getFilePageCount() : pageCount;
    }

    private boolean hasErrors() {
        return this.mCopiesEditText.getError() != null || (this.mPageRangeEditText.getVisibility() == 0 && this.mPageRangeEditText.getError() != null);
    }

    public void onPrinterAvailable(PrinterInfo printer) {
        if (this.mCurrentPrinter.equals(printer)) {
            setState(1);
            if (canUpdateDocument()) {
                updateDocument(false);
            }
            ensurePreviewUiShown();
            updateOptionsUi();
        }
    }

    public void onPrinterUnavailable(PrinterInfo printer) {
        if (this.mCurrentPrinter.getId().equals(printer.getId())) {
            setState(6);
            if (this.mPrintedDocument.isUpdating()) {
                this.mPrintedDocument.cancel();
            }
            ensureErrorUiShown(getString(R.string.print_error_printer_unavailable), 0);
            updateOptionsUi();
        }
    }

    private boolean canUpdateDocument() {
        if (this.mPrintedDocument.isDestroyed() || hasErrors()) {
            return false;
        }
        PrintAttributes attributes = this.mPrintJob.getAttributes();
        int colorMode = attributes.getColorMode();
        if ((colorMode != 2 && colorMode != 1) || attributes.getMediaSize() == null || attributes.getMinMargins() == null || attributes.getResolution() == null || this.mCurrentPrinter == null) {
            return false;
        }
        PrinterCapabilitiesInfo capabilities = this.mCurrentPrinter.getCapabilities();
        return (capabilities == null || this.mCurrentPrinter.getStatus() == 3) ? false : true;
    }

    private void transformDocumentAndFinish(final Uri writeToUri) {
        PrintAttributes attributes = this.mDestinationSpinnerAdapter.getPdfPrinter() == this.mCurrentPrinter ? this.mPrintJob.getAttributes() : null;
        new DocumentTransformer(this, this.mPrintJob, this.mFileProvider, attributes, new Runnable() {
            @Override
            public void run() throws Throwable {
                if (writeToUri != null) {
                    PrintActivity.this.mPrintedDocument.writeContent(PrintActivity.this.getContentResolver(), writeToUri);
                }
                PrintActivity.this.doFinish();
            }
        }).transform();
    }

    private void doFinish() {
        if (this.mState != 0) {
            this.mProgressMessageController.cancel();
            this.mPrinterRegistry.setTrackedPrinter(null);
            this.mSpoolerProvider.destroy();
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

        public SpinnerItem(T value, CharSequence label) {
            this.value = value;
            this.label = label;
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

        public void updatePrinter(PrinterInfo printer) {
            boolean notifyIfAvailable;
            if (!printer.equals(PrintActivity.this.mDestinationSpinnerAdapter.getPdfPrinter())) {
                boolean available = (printer.getStatus() == 3 || printer.getCapabilities() == null) ? false : true;
                if (this.mPrinter == null || !this.mPrinter.getId().equals(printer.getId())) {
                    notifyIfAvailable = true;
                    unpostIfNeeded();
                    this.mPrinterUnavailable = false;
                    this.mPrinter = new PrinterInfo.Builder(printer).build();
                } else {
                    notifyIfAvailable = (this.mPrinter.getStatus() == 3 && printer.getStatus() != 3) || (this.mPrinter.getCapabilities() == null && printer.getCapabilities() != null);
                    this.mPrinter.copyFrom(printer);
                }
                if (available) {
                    unpostIfNeeded();
                    this.mPrinterUnavailable = false;
                    if (notifyIfAvailable) {
                        PrintActivity.this.onPrinterAvailable(this.mPrinter);
                        return;
                    }
                    return;
                }
                if (!this.mPrinterUnavailable) {
                    postIfNeeded();
                }
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

        public PrinterHolder(PrinterInfo printer) {
            this.printer = printer;
        }
    }

    private final class DestinationAdapter extends BaseAdapter implements PrinterRegistry.OnPrintersChangeListener {
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
                if (printerHolder != null && !printerHolder.removed && printerHolder.printer.getId().equals(printerId)) {
                    return i;
                }
            }
            return -1;
        }

        public void ensurePrinterInVisibleAdapterPosition(PrinterId printerId) {
            int printerCount = this.mPrinterHolders.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterHolder printerHolder = this.mPrinterHolders.get(i);
                if (printerHolder.printer.getId().equals(printerId)) {
                    if (i >= getCount() - 2) {
                        int lastPrinterIndex = getCount() - 3;
                        this.mPrinterHolders.set(i, this.mPrinterHolders.get(lastPrinterIndex));
                        this.mPrinterHolders.set(lastPrinterIndex, printerHolder);
                        notifyDataSetChanged();
                        return;
                    }
                    return;
                }
            }
        }

        @Override
        public int getCount() {
            if (this.mHistoricalPrintersLoaded) {
                return Math.min(this.mPrinterHolders.size() + 2, 9);
            }
            return 0;
        }

        @Override
        public boolean isEnabled(int position) {
            Object item = getItem(position);
            if (!(item instanceof PrinterHolder)) {
                return true;
            }
            PrinterHolder printerHolder = (PrinterHolder) item;
            return (printerHolder.removed || printerHolder.printer.getStatus() == 3) ? false : true;
        }

        @Override
        public Object getItem(int position) {
            if (this.mPrinterHolders.isEmpty()) {
                if (position == 0) {
                    return this.mFakePdfPrinterHolder;
                }
            } else {
                if (position < 1) {
                    return this.mPrinterHolders.get(position);
                }
                if (position == 1) {
                    return this.mFakePdfPrinterHolder;
                }
                if (position < getCount() - 1) {
                    return this.mPrinterHolders.get(position - 1);
                }
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            if (this.mPrinterHolders.isEmpty()) {
                if (position == 0) {
                    return 2147483647L;
                }
                if (position == 1) {
                    return 2147483646L;
                }
            } else {
                if (position == 1) {
                    return 2147483647L;
                }
                if (position == getCount() - 1) {
                    return 2147483646L;
                }
            }
            return position;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = getView(position, convertView, parent);
            view.setEnabled(isEnabled(position));
            return view;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = PrintActivity.this.getLayoutInflater().inflate(R.layout.printer_dropdown_item, parent, false);
            }
            CharSequence title = null;
            CharSequence subtitle = null;
            Drawable icon = null;
            if (this.mPrinterHolders.isEmpty()) {
                if (position == 0 && getPdfPrinter() != null) {
                    title = ((PrinterHolder) getItem(position)).printer.getName();
                    icon = PrintActivity.this.getResources().getDrawable(R.drawable.ic_menu_savetopdf);
                } else if (position == 1) {
                    title = PrintActivity.this.getString(R.string.all_printers);
                }
            } else if (position == 1 && getPdfPrinter() != null) {
                title = ((PrinterHolder) getItem(position)).printer.getName();
                icon = PrintActivity.this.getResources().getDrawable(R.drawable.ic_menu_savetopdf);
            } else if (position == getCount() - 1) {
                title = PrintActivity.this.getString(R.string.all_printers);
            } else {
                PrinterHolder printerHolder = (PrinterHolder) getItem(position);
                title = printerHolder.printer.getName();
                try {
                    PackageInfo packageInfo = PrintActivity.this.getPackageManager().getPackageInfo(printerHolder.printer.getId().getServiceName().getPackageName(), 0);
                    subtitle = packageInfo.applicationInfo.loadLabel(PrintActivity.this.getPackageManager());
                    icon = packageInfo.applicationInfo.loadIcon(PrintActivity.this.getPackageManager());
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            titleView.setText(title);
            TextView subtitleView = (TextView) convertView.findViewById(R.id.subtitle);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(0);
            } else {
                subtitleView.setText((CharSequence) null);
                subtitleView.setVisibility(8);
            }
            ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
            if (icon != null) {
                iconView.setImageDrawable(icon);
                iconView.setVisibility(0);
            } else {
                iconView.setVisibility(4);
            }
            return convertView;
        }

        @Override
        public void onPrintersChanged(List<PrinterInfo> printers) {
            this.mHistoricalPrintersLoaded = PrintActivity.this.mPrinterRegistry.areHistoricalPrintersLoaded();
            if (this.mPrinterHolders.isEmpty()) {
                addPrinters(this.mPrinterHolders, printers);
                notifyDataSetChanged();
                return;
            }
            ArrayMap<PrinterId, PrinterInfo> newPrintersMap = new ArrayMap<>();
            int printerCount = printers.size();
            for (int i = 0; i < printerCount; i++) {
                PrinterInfo printer = printers.get(i);
                newPrintersMap.put(printer.getId(), printer);
            }
            List<PrinterHolder> newPrinterHolders = new ArrayList<>();
            int oldPrinterCount = this.mPrinterHolders.size();
            for (int i2 = 0; i2 < oldPrinterCount; i2++) {
                PrinterHolder printerHolder = this.mPrinterHolders.get(i2);
                PrinterId oldPrinterId = printerHolder.printer.getId();
                PrinterInfo updatedPrinter = newPrintersMap.remove(oldPrinterId);
                if (updatedPrinter != null) {
                    printerHolder.printer = updatedPrinter;
                } else {
                    printerHolder.removed = true;
                }
                newPrinterHolders.add(printerHolder);
            }
            addPrinters(newPrinterHolders, newPrintersMap.values());
            this.mPrinterHolders.clear();
            this.mPrinterHolders.addAll(newPrinterHolders);
            notifyDataSetChanged();
        }

        @Override
        public void onPrintersInvalid() {
            this.mPrinterHolders.clear();
            notifyDataSetInvalidated();
        }

        public PrinterHolder getPrinterHolder(PrinterId printerId) {
            int itemCount = getCount();
            for (int i = 0; i < itemCount; i++) {
                Object item = getItem(i);
                if (item instanceof PrinterHolder) {
                    PrinterHolder printerHolder = (PrinterHolder) item;
                    if (printerId.equals(printerHolder.printer.getId())) {
                        return printerHolder;
                    }
                }
            }
            return null;
        }

        public void pruneRemovedPrinters() {
            int holderCounts = this.mPrinterHolders.size();
            for (int i = holderCounts - 1; i >= 0; i--) {
                PrinterHolder printerHolder = this.mPrinterHolders.get(i);
                if (printerHolder.removed) {
                    this.mPrinterHolders.remove(i);
                }
            }
        }

        private void addPrinters(List<PrinterHolder> list, Collection<PrinterInfo> printers) {
            for (PrinterInfo printer : printers) {
                PrinterHolder printerHolder = new PrinterHolder(printer);
                list.add(printerHolder);
            }
        }

        private PrinterInfo createFakePdfPrinter() {
            PrintAttributes.MediaSize defaultMediaSize = MediaSizeUtils.getDefault(PrintActivity.this);
            PrinterId printerId = new PrinterId(PrintActivity.this.getComponentName(), "PDF printer");
            PrinterCapabilitiesInfo.Builder builder = new PrinterCapabilitiesInfo.Builder(printerId);
            String[] mediaSizeIds = PrintActivity.this.getResources().getStringArray(R.array.pdf_printer_media_sizes);
            for (String id : mediaSizeIds) {
                PrintAttributes.MediaSize mediaSize = PrintAttributes.MediaSize.getStandardMediaSizeById(id);
                builder.addMediaSize(mediaSize, mediaSize.equals(defaultMediaSize));
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
            PrinterHolder printerHolder;
            PrinterInfo oldPrinterState = PrintActivity.this.mCurrentPrinter;
            if (oldPrinterState != null && (printerHolder = PrintActivity.this.mDestinationSpinnerAdapter.getPrinterHolder(oldPrinterState.getId())) != null) {
                PrinterInfo newPrinterState = printerHolder.printer;
                if (!printerHolder.removed) {
                    PrintActivity.this.mDestinationSpinnerAdapter.pruneRemovedPrinters();
                } else {
                    PrintActivity.this.onPrinterUnavailable(newPrinterState);
                }
                if (!oldPrinterState.equals(newPrinterState)) {
                    PrinterCapabilitiesInfo oldCapab = oldPrinterState.getCapabilities();
                    PrinterCapabilitiesInfo newCapab = newPrinterState.getCapabilities();
                    boolean hasCapab = newCapab != null;
                    boolean gotCapab = oldCapab == null && newCapab != null;
                    boolean lostCapab = oldCapab != null && newCapab == null;
                    boolean capabChanged = capabilitiesChanged(oldCapab, newCapab);
                    int oldStatus = oldPrinterState.getStatus();
                    int newStatus = newPrinterState.getStatus();
                    boolean isActive = newStatus != 3;
                    boolean becameActive = oldStatus == 3 && oldStatus != newStatus;
                    boolean becameInactive = newStatus == 3 && oldStatus != newStatus;
                    PrintActivity.this.mPrinterAvailabilityDetector.updatePrinter(newPrinterState);
                    oldPrinterState.copyFrom(newPrinterState);
                    if ((isActive && gotCapab) || (becameActive && hasCapab)) {
                        if (hasCapab && capabChanged) {
                            PrintActivity.this.updatePrintAttributesFromCapabilities(newCapab);
                            PrintActivity.this.updatePrintPreviewController(false);
                        }
                        PrintActivity.this.onPrinterAvailable(newPrinterState);
                    } else if ((becameInactive && hasCapab) || (isActive && lostCapab)) {
                        PrintActivity.this.onPrinterUnavailable(newPrinterState);
                    }
                    boolean updateNeeded = (capabChanged && hasCapab && isActive) || (becameActive && hasCapab) || (isActive && gotCapab);
                    if (updateNeeded && PrintActivity.this.canUpdateDocument()) {
                        PrintActivity.this.updateDocument(false);
                    }
                    PrintActivity.this.updateOptionsUi();
                }
            }
        }

        private boolean capabilitiesChanged(PrinterCapabilitiesInfo oldCapabilities, PrinterCapabilitiesInfo newCapabilities) {
            if (oldCapabilities == null) {
                if (newCapabilities != null) {
                    return true;
                }
            } else if (!oldCapabilities.equals(newCapabilities)) {
                return true;
            }
            return false;
        }
    }

    private final class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        private MyOnItemSelectedListener() {
        }

        @Override
        public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
            if (spinner != PrintActivity.this.mDestinationSpinner) {
                if (spinner == PrintActivity.this.mMediaSizeSpinner) {
                    SpinnerItem<PrintAttributes.MediaSize> mediaItem = (SpinnerItem) PrintActivity.this.mMediaSizeSpinnerAdapter.getItem(position);
                    PrintAttributes attributes = PrintActivity.this.mPrintJob.getAttributes();
                    if (PrintActivity.this.mOrientationSpinner.getSelectedItemPosition() == 0) {
                        attributes.setMediaSize(mediaItem.value.asPortrait());
                    } else {
                        attributes.setMediaSize(mediaItem.value.asLandscape());
                    }
                } else if (spinner == PrintActivity.this.mColorModeSpinner) {
                    SpinnerItem<Integer> colorModeItem = (SpinnerItem) PrintActivity.this.mColorModeSpinnerAdapter.getItem(position);
                    PrintActivity.this.mPrintJob.getAttributes().setColorMode(colorModeItem.value.intValue());
                } else if (spinner == PrintActivity.this.mOrientationSpinner) {
                    SpinnerItem<Integer> orientationItem = (SpinnerItem) PrintActivity.this.mOrientationSpinnerAdapter.getItem(position);
                    PrintAttributes attributes2 = PrintActivity.this.mPrintJob.getAttributes();
                    if (PrintActivity.this.mMediaSizeSpinner.getSelectedItem() != null) {
                        if (orientationItem.value.intValue() == 0) {
                            attributes2.copyFrom(attributes2.asPortrait());
                        } else {
                            attributes2.copyFrom(attributes2.asLandscape());
                        }
                    }
                } else if (spinner == PrintActivity.this.mRangeOptionsSpinner) {
                    if (PrintActivity.this.mRangeOptionsSpinner.getSelectedItemPosition() == 0) {
                        PrintActivity.this.mPageRangeEditText.setText("");
                    } else if (TextUtils.isEmpty(PrintActivity.this.mPageRangeEditText.getText())) {
                        PrintActivity.this.mPageRangeEditText.setError("");
                    }
                }
            } else {
                if (position == -1) {
                    return;
                }
                if (id == 2147483646) {
                    PrintActivity.this.startSelectPrinterActivity();
                    return;
                }
                PrinterHolder currentItem = (PrinterHolder) PrintActivity.this.mDestinationSpinner.getSelectedItem();
                PrinterInfo currentPrinter = currentItem != null ? currentItem.printer : null;
                if (PrintActivity.this.mCurrentPrinter != currentPrinter) {
                    PrintActivity.this.mCurrentPrinter = currentPrinter;
                    PrinterHolder printerHolder = PrintActivity.this.mDestinationSpinnerAdapter.getPrinterHolder(currentPrinter.getId());
                    if (!printerHolder.removed) {
                        PrintActivity.this.setState(1);
                        PrintActivity.this.mDestinationSpinnerAdapter.pruneRemovedPrinters();
                        PrintActivity.this.ensurePreviewUiShown();
                    }
                    PrintActivity.this.mPrintJob.setPrinterId(currentPrinter.getId());
                    PrintActivity.this.mPrintJob.setPrinterName(currentPrinter.getName());
                    PrintActivity.this.mPrinterRegistry.setTrackedPrinter(currentPrinter.getId());
                    PrinterCapabilitiesInfo capabilities = currentPrinter.getCapabilities();
                    if (capabilities != null) {
                        PrintActivity.this.updatePrintAttributesFromCapabilities(capabilities);
                    }
                    PrintActivity.this.mPrinterAvailabilityDetector.updatePrinter(currentPrinter);
                } else {
                    return;
                }
            }
            if (PrintActivity.this.canUpdateDocument()) {
                PrintActivity.this.updateDocument(false);
            }
            PrintActivity.this.updateOptionsUi();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    private final class SelectAllOnFocusListener implements View.OnFocusChangeListener {
        private SelectAllOnFocusListener() {
        }

        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            EditText editText = (EditText) view;
            if (!TextUtils.isEmpty(editText.getText())) {
                editText.setSelection(editText.getText().length());
            }
        }
    }

    private final class RangeTextWatcher implements TextWatcher {
        private RangeTextWatcher() {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            int pageCount;
            int pageIndex;
            boolean hadErrors = PrintActivity.this.hasErrors();
            String text = editable.toString();
            if (TextUtils.isEmpty(text)) {
                PrintActivity.this.mPageRangeEditText.setError("");
                PrintActivity.this.updateOptionsUi();
                return;
            }
            String escapedText = PrintActivity.PATTERN_ESCAPE_SPECIAL_CHARS.matcher(text).replaceAll("////");
            if (!PrintActivity.PATTERN_PAGE_RANGE.matcher(escapedText).matches()) {
                PrintActivity.this.mPageRangeEditText.setError("");
                PrintActivity.this.updateOptionsUi();
                return;
            }
            PrintDocumentInfo info = PrintActivity.this.mPrintedDocument.getDocumentInfo().info;
            if (info != null) {
                pageCount = PrintActivity.this.getAdjustedPageCount(info);
            } else {
                pageCount = 0;
            }
            Matcher matcher = PrintActivity.PATTERN_DIGITS.matcher(text);
            while (matcher.find()) {
                String numericString = text.substring(matcher.start(), matcher.end()).trim();
                if (!TextUtils.isEmpty(numericString) && ((pageIndex = Integer.parseInt(numericString)) < 1 || pageIndex > pageCount)) {
                    PrintActivity.this.mPageRangeEditText.setError("");
                    PrintActivity.this.updateOptionsUi();
                    return;
                }
            }
            PrintActivity.this.mPageRangeEditText.setError(null);
            PrintActivity.this.mPrintButton.setEnabled(true);
            PrintActivity.this.updateOptionsUi();
            if (hadErrors && !PrintActivity.this.hasErrors()) {
                PrintActivity.this.updateOptionsUi();
            }
        }
    }

    private final class EditTextWatcher implements TextWatcher {
        private EditTextWatcher() {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            boolean hadErrors = PrintActivity.this.hasErrors();
            if (editable.length() == 0) {
                PrintActivity.this.mCopiesEditText.setError("");
                PrintActivity.this.updateOptionsUi();
                return;
            }
            int copies = 0;
            try {
                copies = Integer.parseInt(editable.toString());
            } catch (NumberFormatException e) {
            }
            if (copies < 1) {
                PrintActivity.this.mCopiesEditText.setError("");
                PrintActivity.this.updateOptionsUi();
                return;
            }
            PrintActivity.this.mPrintJob.setCopies(copies);
            PrintActivity.this.mCopiesEditText.setError(null);
            PrintActivity.this.updateOptionsUi();
            if (hadErrors && PrintActivity.this.canUpdateDocument()) {
                PrintActivity.this.updateDocument(false);
            }
        }
    }

    private final class ProgressMessageController implements Runnable {
        private final Handler mHandler;
        private boolean mPosted;

        public ProgressMessageController(Context context) {
            this.mHandler = new Handler(context.getMainLooper(), null, false);
        }

        public void post() {
            if (!this.mPosted) {
                this.mPosted = true;
                this.mHandler.postDelayed(this, 1000L);
            }
        }

        public void cancel() {
            if (this.mPosted) {
                this.mPosted = false;
                this.mHandler.removeCallbacks(this);
            }
        }

        @Override
        public void run() {
            this.mPosted = false;
            PrintActivity.this.setState(7);
            PrintActivity.this.ensureProgressUiShown();
            PrintActivity.this.updateOptionsUi();
        }
    }

    private static final class DocumentTransformer implements ServiceConnection {
        private final PrintAttributes mAttributesToApply;
        private final Runnable mCallback;
        private final Context mContext;
        private final MutexFileProvider mFileProvider;
        private final PageRange[] mPagesToShred;
        private final PrintJobInfo mPrintJob;

        public DocumentTransformer(Context context, PrintJobInfo printJob, MutexFileProvider fileProvider, PrintAttributes attributes, Runnable callback) {
            this.mContext = context;
            this.mPrintJob = printJob;
            this.mFileProvider = fileProvider;
            this.mCallback = callback;
            this.mPagesToShred = computePagesToShred(this.mPrintJob);
            this.mAttributesToApply = attributes;
        }

        public void transform() {
            if (this.mPagesToShred.length <= 0 && this.mAttributesToApply == null) {
                this.mCallback.run();
                return;
            }
            Intent intent = new Intent("com.android.printspooler.renderer.ACTION_GET_EDITOR");
            intent.setClass(this.mContext, PdfManipulationService.class);
            this.mContext.bindService(intent, this, 1);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final IPdfEditor editor = IPdfEditor.Stub.asInterface(service);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) throws Throwable {
                    DocumentTransformer.this.doTransform(editor);
                    DocumentTransformer.this.updatePrintJob();
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    DocumentTransformer.this.mContext.unbindService(DocumentTransformer.this);
                    DocumentTransformer.this.mCallback.run();
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        private void doTransform(IPdfEditor editor) throws Throwable {
            Exception e;
            File tempFile = null;
            ParcelFileDescriptor src = null;
            ParcelFileDescriptor dst = null;
            InputStream in = null;
            OutputStream out = null;
            try {
                try {
                    File jobFile = this.mFileProvider.acquireFile(null);
                    src = ParcelFileDescriptor.open(jobFile, 805306368);
                    editor.openDocument(src);
                    src.close();
                    editor.removePages(this.mPagesToShred);
                    if (this.mAttributesToApply != null) {
                        editor.applyPrintAttributes(this.mAttributesToApply);
                    }
                    tempFile = File.createTempFile("print_job", ".pdf", this.mContext.getCacheDir());
                    dst = ParcelFileDescriptor.open(tempFile, 805306368);
                    editor.write(dst);
                    dst.close();
                    editor.closeDocument();
                    jobFile.delete();
                    InputStream in2 = new FileInputStream(tempFile);
                    try {
                        OutputStream out2 = new FileOutputStream(jobFile);
                        try {
                            Streams.copy(in2, out2);
                            IoUtils.closeQuietly(src);
                            IoUtils.closeQuietly(dst);
                            IoUtils.closeQuietly(in2);
                            IoUtils.closeQuietly(out2);
                            if (tempFile != null) {
                                tempFile.delete();
                            }
                            this.mFileProvider.releaseFile();
                        } catch (RemoteException e2) {
                            e = e2;
                            out = out2;
                            in = in2;
                            e = e;
                            Log.e("PrintActivity", "Error dropping pages", e);
                            IoUtils.closeQuietly(src);
                            IoUtils.closeQuietly(dst);
                            IoUtils.closeQuietly(in);
                            IoUtils.closeQuietly(out);
                            if (tempFile != null) {
                                tempFile.delete();
                            }
                            this.mFileProvider.releaseFile();
                        } catch (IOException e3) {
                            e = e3;
                            out = out2;
                            in = in2;
                            e = e;
                            Log.e("PrintActivity", "Error dropping pages", e);
                            IoUtils.closeQuietly(src);
                            IoUtils.closeQuietly(dst);
                            IoUtils.closeQuietly(in);
                            IoUtils.closeQuietly(out);
                            if (tempFile != null) {
                            }
                            this.mFileProvider.releaseFile();
                        } catch (Throwable th) {
                            th = th;
                            out = out2;
                            in = in2;
                            IoUtils.closeQuietly(src);
                            IoUtils.closeQuietly(dst);
                            IoUtils.closeQuietly(in);
                            IoUtils.closeQuietly(out);
                            if (tempFile != null) {
                                tempFile.delete();
                            }
                            this.mFileProvider.releaseFile();
                            throw th;
                        }
                    } catch (RemoteException e4) {
                        e = e4;
                        in = in2;
                    } catch (IOException e5) {
                        e = e5;
                        in = in2;
                    } catch (Throwable th2) {
                        th = th2;
                        in = in2;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (RemoteException e6) {
                e = e6;
            } catch (IOException e7) {
                e = e7;
            }
        }

        private void updatePrintJob() {
            int newPageCount = PageRangeUtils.getNormalizedPageCount(this.mPrintJob.getPages(), 0);
            this.mPrintJob.setPages(new PageRange[]{PageRange.ALL_PAGES});
            PrintDocumentInfo oldDocInfo = this.mPrintJob.getDocumentInfo();
            PrintDocumentInfo newDocInfo = new PrintDocumentInfo.Builder(oldDocInfo.getName()).setContentType(oldDocInfo.getContentType()).setPageCount(newPageCount).build();
            this.mPrintJob.setDocumentInfo(newDocInfo);
        }

        private static PageRange[] computePagesToShred(PrintJobInfo printJob) {
            int startPageIdx;
            int endPageIdx;
            List<PageRange> rangesToShred = new ArrayList<>();
            PageRange previousRange = null;
            int pageCount = printJob.getDocumentInfo().getPageCount();
            PageRange[] printedPages = printJob.getPages();
            int rangeCount = printedPages.length;
            for (int i = 0; i < rangeCount; i++) {
                PageRange range = PageRangeUtils.asAbsoluteRange(printedPages[i], pageCount);
                if (previousRange == null) {
                    int endPageIdx2 = range.getStart() - 1;
                    if (endPageIdx2 >= 0) {
                        PageRange removedRange = new PageRange(0, endPageIdx2);
                        rangesToShred.add(removedRange);
                    }
                } else {
                    int startPageIdx2 = previousRange.getEnd() + 1;
                    int endPageIdx3 = range.getStart() - 1;
                    if (startPageIdx2 <= endPageIdx3) {
                        PageRange removedRange2 = new PageRange(startPageIdx2, endPageIdx3);
                        rangesToShred.add(removedRange2);
                    }
                }
                if (i == rangeCount - 1 && (startPageIdx = range.getEnd() + 1) <= printJob.getDocumentInfo().getPageCount() - 1) {
                    PageRange removedRange3 = new PageRange(startPageIdx, endPageIdx);
                    rangesToShred.add(removedRange3);
                }
                previousRange = range;
            }
            PageRange[] result = new PageRange[rangesToShred.size()];
            rangesToShred.toArray(result);
            return result;
        }
    }
}
