/*

 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.quickcontact;

import android.Manifest;
import android.accounts.Account;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Trace;
import android.provider.CalendarContract;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Identity;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.DataUsageFeedback;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.DisplayNameSources;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.QuickContact;
import android.provider.ContactsContract.RawContacts;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.os.BuildCompat;
import android.support.v7.graphics.Palette;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.WindowManager;
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
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.activities.RequestDesiredPermissionsActivity;
import com.android.contacts.activities.RequestPermissionsActivity;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.compat.EventCompat;
import com.android.contacts.compat.MultiWindowCompat;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.ContactUtils;
import com.android.contacts.SimContactsConstants;
import com.android.contacts.detail.ContactDisplayUtils;
import com.android.contacts.dialog.CallSubjectDialog;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.EditorIntents;
import com.android.contacts.editor.EditorUiUtils;
import com.android.contacts.interactions.CalendarInteractionsLoader;
import com.android.contacts.interactions.CallLogInteractionsLoader;
import com.android.contacts.interactions.ContactDeletionInteraction;
import com.android.contacts.interactions.ContactInteraction;
import com.android.contacts.interactions.SmsInteractionsLoader;
import com.android.contacts.interactions.TouchPointManager;
import com.android.contacts.lettertiles.LetterTileDrawable;
import com.android.contacts.ShortcutIntentBuilder;
import com.android.contacts.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.list.UiIntentActions;
import com.android.contacts.logging.Logger;
import com.android.contacts.logging.QuickContactEvent.ActionType;
import com.android.contacts.logging.QuickContactEvent.CardType;
import com.android.contacts.logging.QuickContactEvent.ContactType;
import com.android.contacts.logging.ScreenEvent.ScreenType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.Contact;
import com.android.contacts.model.ContactLoader;
import com.android.contacts.model.RawContact;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.CustomDataItem;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.dataitem.EmailDataItem;
import com.android.contacts.model.dataitem.EventDataItem;
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
import com.android.contacts.quickcontact.ExpandingEntryCardView.Entry;
import com.android.contacts.quickcontact.ExpandingEntryCardView.EntryContextMenuInfo;
import com.android.contacts.quickcontact.ExpandingEntryCardView.EntryTag;
import com.android.contacts.quickcontact.ExpandingEntryCardView.ExpandingEntryCardViewListener;
import com.android.contacts.quickcontact.WebAddress.ParseException;
import com.android.contacts.util.DateUtils;
import com.android.contacts.util.ImageViewDrawableSetter;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contacts.util.MaterialColorMapUtils;
import com.android.contacts.util.MaterialColorMapUtils.MaterialPalette;
import com.android.contacts.util.PermissionsUtil;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.SchedulingUtils;
import com.android.contacts.util.SharedPreferenceUtil;
import com.android.contacts.util.StructuredPostalUtils;
import com.android.contacts.util.UriUtils;
import com.android.contacts.util.ViewUtil;
import com.android.contacts.widget.MultiShrinkScroller;
import com.android.contacts.widget.MultiShrinkScroller.MultiShrinkScrollerListener;
import com.android.contacts.widget.QuickContactImageView;
import com.android.contactsbind.HelpUtils;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mostly translucent {@link Activity} that shows QuickContact dialog. It loads
 * data asynchronously, and then shows a popup with details centered around
 * {@link Intent#getSourceBounds()}.
 */
public class QuickContactActivity extends ContactsActivity {

    /**
     * QuickContacts immediately takes up the full screen. All possible information is shown.
     * This value for {@link android.provider.ContactsContract.QuickContact#EXTRA_MODE}
     * should only be used by the Contacts app.
     */
    public static final int MODE_FULLY_EXPANDED = 4;

    /** Used to pass the screen where the user came before launching this Activity. */
    public static final String EXTRA_PREVIOUS_SCREEN_TYPE = "previous_screen_type";
    /** Used to pass the Contact card action. */
    public static final String EXTRA_ACTION_TYPE = "action_type";
    public static final String EXTRA_THIRD_PARTY_ACTION = "third_party_action";

    /** Used to tell the QuickContact that the previous contact was edited, so it can return an
     * activity result back to the original Activity that launched it. */
    public static final String EXTRA_CONTACT_EDITED = "contact_edited";

    private static final String TAG = "QuickContact";

    private static final String KEY_THEME_COLOR = "theme_color";
    private static final String KEY_PREVIOUS_CONTACT_ID = "previous_contact_id";

    private static final String KEY_SEND_TO_VOICE_MAIL_STATE = "sendToVoicemailState";
    private static final String KEY_ARE_PHONE_OPTIONS_CHANGEABLE = "arePhoneOptionsChangable";
    private static final String KEY_CUSTOM_RINGTONE = "customRingtone";

    private static final int ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION = 150;
    private static final int REQUEST_CODE_CONTACT_EDITOR_ACTIVITY = 1;
    private static final int SCRIM_COLOR = Color.argb(0xC8, 0, 0, 0);
    private static final int REQUEST_CODE_CONTACT_SELECTION_ACTIVITY = 2;
    private static final String MIMETYPE_SMS = "vnd.android-dir/mms-sms";
    private static final int REQUEST_CODE_JOIN = 3;
    private static final int REQUEST_CODE_PICK_RINGTONE = 4;

    private static final int CURRENT_API_VERSION = android.os.Build.VERSION.SDK_INT;

    /** This is the Intent action to install a shortcut in the launcher. */
    private static final String ACTION_INSTALL_SHORTCUT =
            "com.android.launcher.action.INSTALL_SHORTCUT";

    public static final String ACTION_SPLIT_COMPLETED = "splitCompleted";

    // Phone specific option menu items
    private boolean mSendToVoicemailState;
    private boolean mArePhoneOptionsChangable;
    private String mCustomRingtone;

    @SuppressWarnings("deprecation")
    private static final String LEGACY_AUTHORITY = android.provider.Contacts.AUTHORITY;

    private static final String MIMETYPE_GPLUS_PROFILE =
            "vnd.android.cursor.item/vnd.googleplus.profile";
    private static final String GPLUS_PROFILE_DATA_5_VIEW_PROFILE = "view";
    private static final String MIMETYPE_HANGOUTS =
            "vnd.android.cursor.item/vnd.googleplus.profile.comm";
    private static final String HANGOUTS_DATA_5_VIDEO = "hangout";
    private static final String HANGOUTS_DATA_5_MESSAGE = "conversation";
    private static final String CALL_ORIGIN_QUICK_CONTACTS_ACTIVITY =
            "com.android.contacts.quickcontact.QuickContactActivity";

    // Set true in {@link #onCreate} after orientation change for later use in processIntent().
    private boolean mIsRecreatedInstance;
    private boolean mShortcutUsageReported = false;

    private boolean mShouldLog;

    // Used to store and log the referrer package name and the contact type.
    private String mReferrer;
    private int mContactType;

    /**
     * The URI used to load the the Contact. Once the contact is loaded, use Contact#getLookupUri()
     * instead of referencing this URI.
     */
    private Uri mLookupUri;
    private String[] mExcludeMimes;
    private int mExtraMode;
    private String mExtraPrioritizedMimeType;
    private int mStatusBarColor;
    private boolean mHasAlreadyBeenOpened;
    private boolean mOnlyOnePhoneNumber;
    private boolean mOnlyOneEmail;
    private ProgressDialog mProgressDialog;
    private SaveServiceListener mListener;

    private QuickContactImageView mPhotoView;
    private ExpandingEntryCardView mContactCard;
    private ExpandingEntryCardView mNoContactDetailsCard;
    private ExpandingEntryCardView mRecentCard;
    private ExpandingEntryCardView mAboutCard;
    private ExpandingEntryCardView mPermissionExplanationCard;

    private long mPreviousContactId = 0;
    // Permission explanation card.
    private boolean mShouldShowPermissionExplanation = false;
    private String mPermissionExplanationCardSubHeader = "";

    private MultiShrinkScroller mScroller;
    private AsyncTask<Void, Void, Cp2DataCardModel> mEntriesAndActionsTask;
    private AsyncTask<Void, Void, Void> mRecentDataTask;

    /**
     * The last copy of Cp2DataCardModel that was passed to {@link #populateContactAndAboutCard}.
     */
    private Cp2DataCardModel mCachedCp2DataCardModel;
    /**
     *  This scrim's opacity is controlled in two different ways. 1) Before the initial entrance
     *  animation finishes, the opacity is animated by a value animator. This is designed to
     *  distract the user from the length of the initial loading time. 2) After the initial
     *  entrance animation, the opacity is directly related to scroll position.
     */
    private ColorDrawable mWindowScrim;
    private boolean mIsEntranceAnimationFinished;
    private MaterialColorMapUtils mMaterialColorMapUtils;
    private boolean mIsExitAnimationInProgress;
    private boolean mHasComputedThemeColor;

    /**
     * Used to stop the ExpandingEntry cards from adjusting between an entry click and the intent
     * being launched.
     */
    private boolean mHasIntentLaunched;

    private Contact mContactData;
    private ContactLoader mContactLoader;
    private PorterDuffColorFilter mColorFilter;
    private int mColorFilterColor;

    private final ImageViewDrawableSetter mPhotoSetter = new ImageViewDrawableSetter();

    private TelephonyManager tm;
    /**
     * {@link #LEADING_MIMETYPES} is used to sort MIME-types.
     *
     * <p>The MIME-types in {@link #LEADING_MIMETYPES} appear in the front of the dialog,
     * in the order specified here.</p>
     */
    private static final List<String> LEADING_MIMETYPES = Lists.newArrayList(
            Phone.CONTENT_ITEM_TYPE, SipAddress.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE,
            StructuredPostal.CONTENT_ITEM_TYPE);

    private static final List<String> SORTED_ABOUT_CARD_MIMETYPES = Lists.newArrayList(
            Nickname.CONTENT_ITEM_TYPE,
            // Phonetic name is inserted after nickname if it is available.
            // No mimetype for phonetic name exists.
            Website.CONTENT_ITEM_TYPE,
            Organization.CONTENT_ITEM_TYPE,
            Event.CONTENT_ITEM_TYPE,
            Relation.CONTENT_ITEM_TYPE,
            Im.CONTENT_ITEM_TYPE,
            GroupMembership.CONTENT_ITEM_TYPE,
            Identity.CONTENT_ITEM_TYPE,
            CustomDataItem.MIMETYPE_CUSTOM_FIELD,
            Note.CONTENT_ITEM_TYPE);

    private static final BidiFormatter sBidiFormatter = BidiFormatter.getInstance();

    /** Id for the background contact loader */
    private static final int LOADER_CONTACT_ID = 0;

    /** Id for the background Sms Loader */
    private static final int LOADER_SMS_ID = 1;
    private static final int MAX_SMS_RETRIEVE = 3;

    /** Id for the back Calendar Loader */
    private static final int LOADER_CALENDAR_ID = 2;
    private static final String KEY_LOADER_EXTRA_EMAILS =
            QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_EMAILS";
    private static final int MAX_PAST_CALENDAR_RETRIEVE = 3;
    private static final int MAX_FUTURE_CALENDAR_RETRIEVE = 3;
    private static final long PAST_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR =
            1L * 24L * 60L * 60L * 1000L /* 1 day */;
    private static final long FUTURE_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR =
            7L * 24L * 60L * 60L * 1000L /* 7 days */;

    /** Id for the background Call Log Loader */
    private static final int LOADER_CALL_LOG_ID = 3;
    private static final int MAX_CALL_LOG_RETRIEVE = 3;
    private static final int MIN_NUM_CONTACT_ENTRIES_SHOWN = 3;
    private static final int MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN = 3;
    private static final int CARD_ENTRY_ID_EDIT_CONTACT = -2;
    private static final int CARD_ENTRY_ID_REQUEST_PERMISSION = -3;
    private static final String KEY_LOADER_EXTRA_PHONES =
            QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_PHONES";
    private static final String KEY_LOADER_EXTRA_SIP_NUMBERS =
            QuickContactActivity.class.getCanonicalName() + ".KEY_LOADER_EXTRA_SIP_NUMBERS";

    private static final int[] mRecentLoaderIds = new int[]{
        LOADER_SMS_ID,
        LOADER_CALENDAR_ID,
        LOADER_CALL_LOG_ID};
    /**
     * ConcurrentHashMap constructor params: 4 is initial table size, 0.9f is
     * load factor before resizing, 1 means we only expect a single thread to
     * write to the map so make only a single shard
     */
    private Map<Integer, List<ContactInteraction>> mRecentLoaderResults =
        new ConcurrentHashMap<>(4, 0.9f, 1);

    private static final String FRAGMENT_TAG_SELECT_ACCOUNT = "select_account_fragment";
    private boolean simOneLoadComplete = false;
    private boolean simTwoLoadComplete = false;

    final OnClickListener mEntryClickHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final Object entryTagObject = v.getTag();
            if (entryTagObject == null || !(entryTagObject instanceof EntryTag)) {
                Log.w(TAG, "EntryTag was not used correctly");
                return;
            }
            final EntryTag entryTag = (EntryTag) entryTagObject;
            final Intent intent = entryTag.getIntent();
            final int dataId = entryTag.getId();

            if (dataId == CARD_ENTRY_ID_EDIT_CONTACT) {
                editContact();
                return;
            }

            if (dataId == CARD_ENTRY_ID_REQUEST_PERMISSION) {
                finish();
                RequestDesiredPermissionsActivity.startPermissionActivity(
                        QuickContactActivity.this);
                return;
            }

            // Pass the touch point through the intent for use in the InCallUI
            if (Intent.ACTION_CALL.equals(intent.getAction())) {
                if (TouchPointManager.getInstance().hasValidPoint()) {
                    Bundle extras = new Bundle();
                    extras.putParcelable(TouchPointManager.TOUCH_POINT,
                            TouchPointManager.getInstance().getPoint());
                    intent.putExtra(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, extras);
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            mHasIntentLaunched = true;
            try {
                final int actionType = intent.getIntExtra(EXTRA_ACTION_TYPE,
                        ActionType.UNKNOWN_ACTION);
                final String thirdPartyAction = intent.getStringExtra(EXTRA_THIRD_PARTY_ACTION);
                Logger.logQuickContactEvent(mReferrer, mContactType,
                        CardType.UNKNOWN_CARD, actionType, thirdPartyAction);
                ImplicitIntentsUtil.startActivityInAppIfPossible(QuickContactActivity.this, intent);
            } catch (SecurityException ex) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app,
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "QuickContacts does not have permission to launch "
                        + intent);
            } catch (ActivityNotFoundException ex) {
                Toast.makeText(QuickContactActivity.this, R.string.missing_app,
                        Toast.LENGTH_SHORT).show();
            }

            // Default to USAGE_TYPE_CALL. Usage is summed among all types for sorting each data id
            // so the exact usage type is not necessary in all cases
            String usageType = DataUsageFeedback.USAGE_TYPE_CALL;

            final Uri intentUri = intent.getData();
            if ((intentUri != null && intentUri.getScheme() != null &&
                    intentUri.getScheme().equals(ContactsUtils.SCHEME_SMSTO)) ||
                    (intent.getType() != null && intent.getType().equals(MIMETYPE_SMS))) {
                usageType = DataUsageFeedback.USAGE_TYPE_SHORT_TEXT;
            }

            // Data IDs start at 1 so anything less is invalid
            if (dataId > 0) {
                final Uri dataUsageUri = DataUsageFeedback.FEEDBACK_URI.buildUpon()
                        .appendPath(String.valueOf(dataId))
                        .appendQueryParameter(DataUsageFeedback.USAGE_TYPE, usageType)
                        .build();
                try {
                    final boolean successful = getContentResolver().update(
                            dataUsageUri, new ContentValues(), null, null) > 0;
                    if (!successful) {
                        Log.w(TAG, "DataUsageFeedback increment failed");
                    }
                } catch (SecurityException ex) {
                    Log.w(TAG, "DataUsageFeedback increment failed", ex);
                }
            } else {
                Log.w(TAG, "Invalid Data ID");
            }
        }
    };

    final ExpandingEntryCardViewListener mExpandingEntryCardViewListener
            = new ExpandingEntryCardViewListener() {
        @Override
        public void onCollapse(int heightDelta) {
            mScroller.prepareForShrinkingScrollChild(heightDelta);
        }

        @Override
        public void onExpand() {
            mScroller.setDisableTouchesForSuppressLayout(/* areTouchesDisabled = */ true);
        }

        @Override
        public void onExpandDone() {
            mScroller.setDisableTouchesForSuppressLayout(/* areTouchesDisabled = */ false);
        }
    };

    private interface ContextMenuIds {
        static final int COPY_TEXT = 0;
        static final int CLEAR_DEFAULT = 1;
        static final int SET_DEFAULT = 2;
    }

    private final OnCreateContextMenuListener mEntryContextMenuListener =
            new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            if (menuInfo == null) {
                return;
            }
            final EntryContextMenuInfo info = (EntryContextMenuInfo) menuInfo;
            menu.setHeaderTitle(info.getCopyText());
            menu.add(ContextMenu.NONE, ContextMenuIds.COPY_TEXT,
                    ContextMenu.NONE, getString(R.string.copy_text));

            // Don't allow setting or clearing of defaults for non-editable contacts
            if (!isContactEditable()) {
                return;
            }

            final String selectedMimeType = info.getMimeType();

            // Defaults to true will only enable the detail to be copied to the clipboard.
            boolean onlyOneOfMimeType = true;

            // Only allow primary support for Phone and Email content types
            if (Phone.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
                onlyOneOfMimeType = mOnlyOnePhoneNumber;
            } else if (Email.CONTENT_ITEM_TYPE.equals(selectedMimeType)) {
                onlyOneOfMimeType = mOnlyOneEmail;
            }

            // Checking for previously set default
            if (info.isSuperPrimary()) {
                menu.add(ContextMenu.NONE, ContextMenuIds.CLEAR_DEFAULT,
                        ContextMenu.NONE, getString(R.string.clear_default));
            } else if (!onlyOneOfMimeType) {
                menu.add(ContextMenu.NONE, ContextMenuIds.SET_DEFAULT,
                        ContextMenu.NONE, getString(R.string.set_default));
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        EntryContextMenuInfo menuInfo;
        try {
            menuInfo = (EntryContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
            case ContextMenuIds.COPY_TEXT:
                ClipboardUtils.copyText(this, menuInfo.getCopyLabel(), menuInfo.getCopyText(),
                        true);
                return true;
            case ContextMenuIds.SET_DEFAULT:
                final Intent setIntent = ContactSaveService.createSetSuperPrimaryIntent(this,
                        menuInfo.getId());
                this.startService(setIntent);
                return true;
            case ContextMenuIds.CLEAR_DEFAULT:
                final Intent clearIntent = ContactSaveService.createClearPrimaryIntent(this,
                        menuInfo.getId());
                this.startService(clearIntent);
                return true;
            default:
                throw new IllegalArgumentException("Unknown menu option " + item.getItemId());
        }
    }

    final MultiShrinkScrollerListener mMultiShrinkScrollerListener
            = new MultiShrinkScrollerListener() {
        @Override
        public void onScrolledOffBottom() {
            finish();
        }

        @Override
        public void onEnterFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onExitFullscreen() {
            updateStatusBarColor();
        }

        @Override
        public void onStartScrollOffBottom() {
            mIsExitAnimationInProgress = true;
        }

        @Override
        public void onEntranceAnimationDone() {
            mIsEntranceAnimationFinished = true;
        }

        @Override
        public void onTransparentViewHeightChange(float ratio) {
            if (mIsEntranceAnimationFinished) {
                mWindowScrim.setAlpha((int) (0xFF * ratio));
            }
        }
    };


    /**
     * Data items are compared to the same mimetype based off of three qualities:
     * 1. Super primary
     * 2. Primary
     * 3. Times used
     */
    private final Comparator<DataItem> mWithinMimeTypeDataItemComparator =
            new Comparator<DataItem>() {
        @Override
        public int compare(DataItem lhs, DataItem rhs) {
            if (!lhs.getMimeType().equals(rhs.getMimeType())) {
                Log.wtf(TAG, "Comparing DataItems with different mimetypes lhs.getMimeType(): " +
                        lhs.getMimeType() + " rhs.getMimeType(): " + rhs.getMimeType());
                return 0;
            }

            if (lhs.isSuperPrimary()) {
                return -1;
            } else if (rhs.isSuperPrimary()) {
                return 1;
            } else if (lhs.isPrimary() && !rhs.isPrimary()) {
                return -1;
            } else if (!lhs.isPrimary() && rhs.isPrimary()) {
                return 1;
            } else {
                final int lhsTimesUsed =
                        lhs.getTimesUsed() == null ? 0 : lhs.getTimesUsed();
                final int rhsTimesUsed =
                        rhs.getTimesUsed() == null ? 0 : rhs.getTimesUsed();

                return rhsTimesUsed - lhsTimesUsed;
            }
        }
    };

    /**
     * Sorts among different mimetypes based off:
     * 1. Whether one of the mimetypes is the prioritized mimetype
     * 2. Number of times used
     * 3. Last time used
     * 4. Statically defined
     */
    private final Comparator<List<DataItem>> mAmongstMimeTypeDataItemComparator =
            new Comparator<List<DataItem>> () {
        @Override
        public int compare(List<DataItem> lhsList, List<DataItem> rhsList) {
            final DataItem lhs = lhsList.get(0);
            final DataItem rhs = rhsList.get(0);
            final String lhsMimeType = lhs.getMimeType();
            final String rhsMimeType = rhs.getMimeType();

            // 1. Whether one of the mimetypes is the prioritized mimetype
            if (!TextUtils.isEmpty(mExtraPrioritizedMimeType) && !lhsMimeType.equals(rhsMimeType)) {
                if (rhsMimeType.equals(mExtraPrioritizedMimeType)) {
                    return 1;
                }
                if (lhsMimeType.equals(mExtraPrioritizedMimeType)) {
                    return -1;
                }
            }

            // 2. Number of times used
            final int lhsTimesUsed = lhs.getTimesUsed() == null ? 0 : lhs.getTimesUsed();
            final int rhsTimesUsed = rhs.getTimesUsed() == null ? 0 : rhs.getTimesUsed();
            final int timesUsedDifference = rhsTimesUsed - lhsTimesUsed;
            if (timesUsedDifference != 0) {
                return timesUsedDifference;
            }

            // 3. Last time used
            final long lhsLastTimeUsed =
                    lhs.getLastTimeUsed() == null ? 0 : lhs.getLastTimeUsed();
            final long rhsLastTimeUsed =
                    rhs.getLastTimeUsed() == null ? 0 : rhs.getLastTimeUsed();
            final long lastTimeUsedDifference = rhsLastTimeUsed - lhsLastTimeUsed;
            if (lastTimeUsedDifference > 0) {
                return 1;
            } else if (lastTimeUsedDifference < 0) {
                return -1;
            }

            // 4. Resort to a statically defined mimetype order.
            if (!lhsMimeType.equals(rhsMimeType)) {
                for (String mimeType : LEADING_MIMETYPES) {
                    if (lhsMimeType.equals(mimeType)) {
                        return -1;
                    } else if (rhsMimeType.equals(mimeType)) {
                        return 1;
                    }
                }
            }
            return 0;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            TouchPointManager.getInstance().setPoint((int) ev.getRawX(), (int) ev.getRawY());
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Trace.beginSection("onCreate()");
        super.onCreate(savedInstanceState);

        if (RequestPermissionsActivity.startPermissionActivityIfNeeded(this)) {
            return;
        }

        mIsRecreatedInstance = savedInstanceState != null;
        if (mIsRecreatedInstance) {
            mPreviousContactId = savedInstanceState.getLong(KEY_PREVIOUS_CONTACT_ID);

            // Phone specific options menus
            mSendToVoicemailState = savedInstanceState.getBoolean(KEY_SEND_TO_VOICE_MAIL_STATE);
            mArePhoneOptionsChangable =
                    savedInstanceState.getBoolean(KEY_ARE_PHONE_OPTIONS_CHANGEABLE);
            mCustomRingtone = savedInstanceState.getString(KEY_CUSTOM_RINGTONE);
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);

        mListener = new SaveServiceListener();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ContactSaveService.BROADCAST_LINK_COMPLETE);
        intentFilter.addAction(ContactSaveService.BROADCAST_UNLINK_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(mListener,
                intentFilter);


        mShouldLog = true;

        // There're 3 states for each permission:
        // 1. App doesn't have permission, not asked user yet.
        // 2. App doesn't have permission, user denied it previously.
        // 3. App has permission.
        // Permission explanation card is displayed only for case 1.
        final boolean hasTelephonyFeature =
                getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        final boolean hasCalendarPermission = PermissionsUtil.hasPermission(
                this, Manifest.permission.READ_CALENDAR);
        final boolean hasSMSPermission = hasTelephonyFeature
                && PermissionsUtil.hasPermission(this, Manifest.permission.READ_SMS);

        final boolean wasCalendarPermissionDenied =
                ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.READ_CALENDAR);
        final boolean wasSMSPermissionDenied =
                hasTelephonyFeature && ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.READ_SMS);

        final boolean shouldDisplayCalendarMessage =
                !hasCalendarPermission && !wasCalendarPermissionDenied;
        final boolean shouldDisplaySMSMessage =
                hasTelephonyFeature && !hasSMSPermission && !wasSMSPermissionDenied;
        mShouldShowPermissionExplanation = shouldDisplayCalendarMessage || shouldDisplaySMSMessage;

        if (shouldDisplayCalendarMessage && shouldDisplaySMSMessage) {
            mPermissionExplanationCardSubHeader =
                    getString(R.string.permission_explanation_subheader_calendar_and_SMS);
        } else if (shouldDisplayCalendarMessage) {
            mPermissionExplanationCardSubHeader =
                    getString(R.string.permission_explanation_subheader_calendar);
        } else if (shouldDisplaySMSMessage) {
            mPermissionExplanationCardSubHeader =
                    getString(R.string.permission_explanation_subheader_SMS);
        }

        final int previousScreenType = getIntent().getIntExtra
                (EXTRA_PREVIOUS_SCREEN_TYPE, ScreenType.UNKNOWN);
        Logger.logScreenView(this, ScreenType.QUICK_CONTACT, previousScreenType);

        mReferrer = getCallingPackage();
        if (mReferrer == null && CompatUtils.isLollipopMr1Compatible() && getReferrer() != null) {
            mReferrer = getReferrer().getAuthority();
        }
        mContactType = ContactType.UNKNOWN_TYPE;

        if (CompatUtils.isLollipopCompatible()) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        processIntent(getIntent());

        // Show QuickContact in front of soft input
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.quickcontact_activity);

        mMaterialColorMapUtils = new MaterialColorMapUtils(getResources());

        mScroller = (MultiShrinkScroller) findViewById(R.id.multiscroller);

        mContactCard = (ExpandingEntryCardView) findViewById(R.id.communication_card);
        mNoContactDetailsCard = (ExpandingEntryCardView) findViewById(R.id.no_contact_data_card);
        mRecentCard = (ExpandingEntryCardView) findViewById(R.id.recent_card);
        mAboutCard = (ExpandingEntryCardView) findViewById(R.id.about_card);
        mPermissionExplanationCard =
                (ExpandingEntryCardView) findViewById(R.id.permission_explanation_card);

        mPermissionExplanationCard.setOnClickListener(mEntryClickHandler);
        mNoContactDetailsCard.setOnClickListener(mEntryClickHandler);
        mContactCard.setOnClickListener(mEntryClickHandler);
        mContactCard.setOnCreateContextMenuListener(mEntryContextMenuListener);

        mRecentCard.setOnClickListener(mEntryClickHandler);
        mRecentCard.setTitle(getResources().getString(R.string.recent_card_title));

        mAboutCard.setOnClickListener(mEntryClickHandler);
        mAboutCard.setOnCreateContextMenuListener(mEntryContextMenuListener);

        mPhotoView = (QuickContactImageView) findViewById(R.id.photo);
        final View transparentView = findViewById(R.id.transparent_view);
        if (mScroller != null) {
            transparentView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mScroller.scrollOffBottom();
                }
            });
        }

        // Allow a shadow to be shown under the toolbar.
        ViewUtil.addRectangularOutlineProvider(findViewById(R.id.toolbar_parent), getResources());

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setTitle(null);
        // Put a TextView with a known resource id into the ActionBar. This allows us to easily
        // find the correct TextView location & size later.
        toolbar.addView(getLayoutInflater().inflate(R.layout.quickcontact_title_placeholder, null));

        mHasAlreadyBeenOpened = savedInstanceState != null;
        mIsEntranceAnimationFinished = mHasAlreadyBeenOpened;
        mWindowScrim = new ColorDrawable(SCRIM_COLOR);
        mWindowScrim.setAlpha(0);
        getWindow().setBackgroundDrawable(mWindowScrim);

        mScroller.initialize(mMultiShrinkScrollerListener, mExtraMode == MODE_FULLY_EXPANDED,
                /* maximumHeaderTextSize */ -1,
                /* shouldUpdateNameViewHeight */ true);
        // mScroller needs to perform asynchronous measurements after initalize(), therefore
        // we can't mark this as GONE.
        mScroller.setVisibility(View.INVISIBLE);

        setHeaderNameText(R.string.missing_name);

        SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ true,
                new Runnable() {
                    @Override
                    public void run() {
                        if (!mHasAlreadyBeenOpened) {
                            // The initial scrim opacity must match the scrim opacity that would be
                            // achieved by scrolling to the starting position.
                            final float alphaRatio = mExtraMode == MODE_FULLY_EXPANDED ?
                                    1 : mScroller.getStartingTransparentHeightRatio();
                            final int duration = getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);
                            final int desiredAlpha = (int) (0xFF * alphaRatio);
                            ObjectAnimator o = ObjectAnimator.ofInt(mWindowScrim, "alpha", 0,
                                    desiredAlpha).setDuration(duration);

                            o.start();
                        }
                    }
                });

        if (savedInstanceState != null) {
            final int color = savedInstanceState.getInt(KEY_THEME_COLOR, 0);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            // Need to wait for the pre draw before setting the initial scroll
                            // value. Prior to pre draw all scroll values are invalid.
                            if (mHasAlreadyBeenOpened) {
                                mScroller.setVisibility(View.VISIBLE);
                                mScroller.setScroll(mScroller.getScrollNeededToBeFullScreen());
                            }
                            // Need to wait for pre draw for setting the theme color. Setting the
                            // header tint before the MultiShrinkScroller has been measured will
                            // cause incorrect tinting calculations.
                            if (color != 0) {
                                setThemeColor(mMaterialColorMapUtils
                                        .calculatePrimaryAndSecondaryColor(color));
                            }
                        }
                    });
        }

        Trace.endSection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final boolean deletedOrSplit = requestCode == REQUEST_CODE_CONTACT_EDITOR_ACTIVITY &&
                (resultCode == ContactDeletionInteraction.RESULT_CODE_DELETED ||
                resultCode == ContactEditorActivity.RESULT_CODE_SPLIT);
        setResult(resultCode, data);
        if (deletedOrSplit) {
            finish();
        } else if (requestCode == REQUEST_CODE_CONTACT_SELECTION_ACTIVITY &&
                resultCode != RESULT_CANCELED) {
            processIntent(data);
        } else if (requestCode == REQUEST_CODE_JOIN) {
            // Ignore failed requests
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            if (data != null) {
                joinAggregate(ContentUris.parseId(data.getData()));
            }
        } else if (requestCode == REQUEST_CODE_PICK_RINGTONE && data != null) {
            final Uri pickedUri = data.getParcelableExtra(
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            onRingtonePicked(pickedUri);
        }
    }

    private void onRingtonePicked(Uri pickedUri) {
        mCustomRingtone = EditorUiUtils.getRingtoneStringFromUri(pickedUri, CURRENT_API_VERSION);
        Intent intent = ContactSaveService.createSetRingtone(
                this, mLookupUri, mCustomRingtone);
        this.startService(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mHasAlreadyBeenOpened = true;
        mIsEntranceAnimationFinished = true;
        mHasComputedThemeColor = false;
        processIntent(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mColorFilter != null) {
            savedInstanceState.putInt(KEY_THEME_COLOR, mColorFilterColor);
        }
        savedInstanceState.putLong(KEY_PREVIOUS_CONTACT_ID, mPreviousContactId);

        // Phone specific options
        savedInstanceState.putBoolean(KEY_SEND_TO_VOICE_MAIL_STATE, mSendToVoicemailState);
        savedInstanceState.putBoolean(KEY_ARE_PHONE_OPTIONS_CHANGEABLE, mArePhoneOptionsChangable);
        savedInstanceState.putString(KEY_CUSTOM_RINGTONE, mCustomRingtone);
    }

    private void processIntent(Intent intent) {
        if (intent == null) {
            finish();
            return;
        }
        if (ACTION_SPLIT_COMPLETED.equals(intent.getAction())) {
            Toast.makeText(this, R.string.contactUnlinkedToast, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri lookupUri = intent.getData();
        if (intent.getBooleanExtra(EXTRA_CONTACT_EDITED, false)) {
            setResult(ContactEditorActivity.RESULT_CODE_EDITED);
        }

        // Check to see whether it comes from the old version.
        if (lookupUri != null && LEGACY_AUTHORITY.equals(lookupUri.getAuthority())) {
            final long rawContactId = ContentUris.parseId(lookupUri);
            lookupUri = RawContacts.getContactLookupUri(getContentResolver(),
                    ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId));
        }
        mExtraMode = getIntent().getIntExtra(QuickContact.EXTRA_MODE, QuickContact.MODE_LARGE);
        if (isMultiWindowOnPhone()) {
            mExtraMode = QuickContact.MODE_LARGE;
        }
        mExtraPrioritizedMimeType =
                getIntent().getStringExtra(QuickContact.EXTRA_PRIORITIZED_MIMETYPE);
        final Uri oldLookupUri = mLookupUri;


        if (lookupUri == null) {
            finish();
            return;
        }
        mLookupUri = lookupUri;
        mExcludeMimes = intent.getStringArrayExtra(QuickContact.EXTRA_EXCLUDE_MIMES);
        if (oldLookupUri == null) {
            // Should not log if only orientation changes.
            mShouldLog = !mIsRecreatedInstance;
            mContactLoader = (ContactLoader) getLoaderManager().initLoader(
                    LOADER_CONTACT_ID, null, mLoaderContactCallbacks);
        } else if (oldLookupUri != mLookupUri) {
            // Should log when reload happens, regardless of orientation change.
            mShouldLog = true;
            // After copying a directory contact, the contact URI changes. Therefore,
            // we need to reload the new contact.
            destroyInteractionLoaders();
            mContactLoader = (ContactLoader) (Loader<?>) getLoaderManager().getLoader(
                    LOADER_CONTACT_ID);
            mContactLoader.setNewLookup(mLookupUri);
            mCachedCp2DataCardModel = null;
        }
        mContactLoader.forceLoad();

        NfcHandler.register(this, mLookupUri);
    }

    private void destroyInteractionLoaders() {
        for (int interactionLoaderId : mRecentLoaderIds) {
            getLoaderManager().destroyLoader(interactionLoaderId);
        }
    }

    private void runEntranceAnimation() {
        if (mHasAlreadyBeenOpened) {
            return;
        }
        mHasAlreadyBeenOpened = true;
        mScroller.scrollUpForEntranceAnimation(/* scrollToCurrentPosition */ !isMultiWindowOnPhone()
                && (mExtraMode != MODE_FULLY_EXPANDED));
    }

    private boolean isMultiWindowOnPhone() {
        return MultiWindowCompat.isInMultiWindowMode(this) && PhoneCapabilityTester.isPhone(this);
    }

    /** Assign this string to the view if it is not empty. */
    private void setHeaderNameText(int resId) {
        if (mScroller != null) {
            mScroller.setTitle(getText(resId) == null ? null : getText(resId).toString(),
                    /* isPhoneNumber= */ false);
        }
    }

    /** Assign this string to the view if it is not empty. */
    private void setHeaderNameText(String value, boolean isPhoneNumber) {
        if (!TextUtils.isEmpty(value)) {
            if (mScroller != null) {
                mScroller.setTitle(value, isPhoneNumber);
            }
        }
    }

    /**
     * Check if the given MIME-type appears in the list of excluded MIME-types
     * that the most-recent caller requested.
     */
    private boolean isMimeExcluded(String mimeType) {
        if (mExcludeMimes == null) return false;
        for (String excludedMime : mExcludeMimes) {
            if (TextUtils.equals(excludedMime, mimeType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle the result from the ContactLoader
     */
    private void bindContactData(final Contact data) {
        Trace.beginSection("bindContactData");

        final int actionType = mContactData == null ? ActionType.START : ActionType.UNKNOWN_ACTION;
        mContactData = data;

        final int newContactType;
        if (DirectoryContactUtil.isDirectoryContact(mContactData)) {
            newContactType = ContactType.DIRECTORY;
        } else if (InvisibleContactUtil.isInvisibleAndAddable(mContactData, this)) {
            newContactType = ContactType.INVISIBLE_AND_ADDABLE;
        } else if (isContactEditable()) {
            newContactType = ContactType.EDITABLE;
        } else {
            newContactType = ContactType.UNKNOWN_TYPE;
        }
        if (mShouldLog && mContactType != newContactType) {
            Logger.logQuickContactEvent(mReferrer, newContactType, CardType.UNKNOWN_CARD,
                    actionType, /* thirdPartyAction */ null);
        }
        mContactType = newContactType;

        setStateForPhoneMenuItems(mContactData);
        invalidateOptionsMenu();

        Trace.endSection();
        Trace.beginSection("Set display photo & name");

        mPhotoView.setIsBusiness(mContactData.isDisplayNameFromOrganization());
        mPhotoSetter.setupContactPhoto(data, mPhotoView);
        extractAndApplyTintFromPhotoViewAsynchronously();
        final String displayName = ContactDisplayUtils.getDisplayName(this, data).toString();
        setHeaderNameText(
                displayName, mContactData.getDisplayNameSource() == DisplayNameSources.PHONE);
        final String phoneticName = ContactDisplayUtils.getPhoneticName(this, data);
        if (mScroller != null) {
            // Show phonetic name only when it doesn't equal the display name.
            if (!TextUtils.isEmpty(phoneticName) && !phoneticName.equals(displayName)) {
                mScroller.setPhoneticName(phoneticName);
            } else {
                mScroller.setPhoneticNameGone();
            }
        }

        Trace.endSection();

        mEntriesAndActionsTask = new AsyncTask<Void, Void, Cp2DataCardModel>() {

            @Override
            protected Cp2DataCardModel doInBackground(
                    Void... params) {
                return generateDataModelFromContact(data);
            }

            @Override
            protected void onPostExecute(Cp2DataCardModel cardDataModel) {
                super.onPostExecute(cardDataModel);
                // Check that original AsyncTask parameters are still valid and the activity
                // is still running before binding to UI. A new intent could invalidate
                // the results, for example.
                if (data == mContactData && !isCancelled()) {
                    bindDataToCards(cardDataModel);
                    showActivity();
                }
            }
        };
        mEntriesAndActionsTask.execute();
    }

    private void bindDataToCards(Cp2DataCardModel cp2DataCardModel) {
        startInteractionLoaders(cp2DataCardModel);
        populateContactAndAboutCard(cp2DataCardModel, /* shouldAddPhoneticName */ true);
    }

    private void startInteractionLoaders(Cp2DataCardModel cp2DataCardModel) {
        final Map<String, List<DataItem>> dataItemsMap = cp2DataCardModel.dataItemsMap;
        final List<DataItem> phoneDataItems = dataItemsMap.get(Phone.CONTENT_ITEM_TYPE);
        final List<DataItem> sipCallDataItems = dataItemsMap.get(SipAddress.CONTENT_ITEM_TYPE);
        if (phoneDataItems != null && phoneDataItems.size() == 1) {
            mOnlyOnePhoneNumber = true;
        }
        String[] phoneNumbers = null;
        if (phoneDataItems != null) {
            phoneNumbers = new String[phoneDataItems.size()];
            for (int i = 0; i < phoneDataItems.size(); ++i) {
                phoneNumbers[i] = ((PhoneDataItem) phoneDataItems.get(i)).getNumber();
            }
        }
        String[] sipNumbers = null;
        if (sipCallDataItems != null) {
            sipNumbers = new String[sipCallDataItems.size()];
            for (int i = 0; i < sipCallDataItems.size(); ++i) {
                sipNumbers[i] = ((SipAddressDataItem) sipCallDataItems.get(i)).getSipAddress();
            }
        }
        final Bundle phonesExtraBundle = new Bundle();
        phonesExtraBundle.putStringArray(KEY_LOADER_EXTRA_PHONES, phoneNumbers);
        phonesExtraBundle.putStringArray(KEY_LOADER_EXTRA_SIP_NUMBERS, sipNumbers);

        Trace.beginSection("start sms loader");
        getLoaderManager().initLoader(
                LOADER_SMS_ID,
                phonesExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();

        Trace.beginSection("start call log loader");
        getLoaderManager().initLoader(
                LOADER_CALL_LOG_ID,
                phonesExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();


        Trace.beginSection("start calendar loader");
        final List<DataItem> emailDataItems = dataItemsMap.get(Email.CONTENT_ITEM_TYPE);
        if (emailDataItems != null && emailDataItems.size() == 1) {
            mOnlyOneEmail = true;
        }
        String[] emailAddresses = null;
        if (emailDataItems != null) {
            emailAddresses = new String[emailDataItems.size()];
            for (int i = 0; i < emailDataItems.size(); ++i) {
                emailAddresses[i] = ((EmailDataItem) emailDataItems.get(i)).getAddress();
            }
        }
        final Bundle emailsExtraBundle = new Bundle();
        emailsExtraBundle.putStringArray(KEY_LOADER_EXTRA_EMAILS, emailAddresses);
        getLoaderManager().initLoader(
                LOADER_CALENDAR_ID,
                emailsExtraBundle,
                mLoaderInteractionsCallbacks);
        Trace.endSection();
    }

    private void showActivity() {
        if (mScroller != null) {
            mScroller.setVisibility(View.VISIBLE);
            SchedulingUtils.doOnPreDraw(mScroller, /* drawNextFrame = */ false,
                    new Runnable() {
                        @Override
                        public void run() {
                            runEntranceAnimation();
                        }
                    });
        }
    }

    private List<List<Entry>> buildAboutCardEntries(Map<String, List<DataItem>> dataItemsMap) {
        final List<List<Entry>> aboutCardEntries = new ArrayList<>();
        for (String mimetype : SORTED_ABOUT_CARD_MIMETYPES) {
            final List<DataItem> mimeTypeItems = dataItemsMap.get(mimetype);
            if (mimeTypeItems == null) {
                continue;
            }
            // Set aboutCardTitleOut = null, since SORTED_ABOUT_CARD_MIMETYPES doesn't contain
            // the name mimetype.
            final List<Entry> aboutEntries = dataItemsToEntries(mimeTypeItems,
                    /* aboutCardTitleOut = */ null);
            if (aboutEntries.size() > 0) {
                aboutCardEntries.add(aboutEntries);
            }
        }
        return aboutCardEntries;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // If returning from a launched activity, repopulate the contact and about card
        if (mHasIntentLaunched) {
            mHasIntentLaunched = false;
            populateContactAndAboutCard(mCachedCp2DataCardModel, /* shouldAddPhoneticName */ false);
        }

        // When exiting the activity and resuming, we want to force a full reload of all the
        // interaction data in case something changed in the background. On screen rotation,
        // we don't need to do this. And, mCachedCp2DataCardModel will be null, so we won't.
        if (mCachedCp2DataCardModel != null) {
            destroyInteractionLoaders();
            startInteractionLoaders(mCachedCp2DataCardModel);
        }
        maybeShowProgressDialog();
    }


    @Override
    protected void onPause() {
        super.onPause();
        dismissProgressBar();
    }

    private void populateContactAndAboutCard(Cp2DataCardModel cp2DataCardModel,
            boolean shouldAddPhoneticName) {
        mCachedCp2DataCardModel = cp2DataCardModel;
        if (mHasIntentLaunched || cp2DataCardModel == null) {
            return;
        }
        Trace.beginSection("bind contact card");

        final List<List<Entry>> contactCardEntries = cp2DataCardModel.contactCardEntries;
        final List<List<Entry>> aboutCardEntries = cp2DataCardModel.aboutCardEntries;
        final String customAboutCardName = cp2DataCardModel.customAboutCardName;

        if (contactCardEntries.size() > 0) {
            mContactCard.initialize(contactCardEntries,
                    /* numInitialVisibleEntries = */ MIN_NUM_CONTACT_ENTRIES_SHOWN,
                    /* isExpanded = */ mContactCard.isExpanded(),
                    /* isAlwaysExpanded = */ true,
                    mExpandingEntryCardViewListener,
                    mScroller);
            if (mContactCard.getVisibility() == View.GONE && mShouldLog) {
                Logger.logQuickContactEvent(mReferrer, mContactType, CardType.CONTACT,
                        ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
            }
            mContactCard.setVisibility(View.VISIBLE);
        } else {
            mContactCard.setVisibility(View.GONE);
        }
        Trace.endSection();

        Trace.beginSection("bind about card");
        // Phonetic name is not a data item, so the entry needs to be created separately
        // But if mCachedCp2DataCardModel is passed to this method (e.g. returning from editor
        // without saving any changes), then it should include phoneticName and the phoneticName
        // shouldn't be changed. If this is the case, we shouldn't add it again. b/27459294
        final String phoneticName = mContactData.getPhoneticName();
        if (shouldAddPhoneticName && !TextUtils.isEmpty(phoneticName)) {
            Entry phoneticEntry = new Entry(/* viewId = */ -1,
                    /* icon = */ null,
                    getResources().getString(R.string.name_phonetic),
                    phoneticName,
                    /* subHeaderIcon = */ null,
                    /* text = */ null,
                    /* textIcon = */ null,
                    /* primaryContentDescription = */ null,
                    /* intent = */ null,
                    /* alternateIcon = */ null,
                    /* alternateIntent = */ null,
                    /* alternateContentDescription = */ null,
                    /* shouldApplyColor = */ false,
                    /* isEditable = */ false,
                    /* EntryContextMenuInfo = */ new EntryContextMenuInfo(phoneticName,
                            getResources().getString(R.string.name_phonetic),
                            /* mimeType = */ null, /* id = */ -1, /* isPrimary = */ false),
                    /* thirdIcon = */ null,
                    /* thirdIntent = */ null,
                    /* thirdContentDescription = */ null,
                    /* thirdAction = */ Entry.ACTION_NONE,
                    /* thirdExtras = */ null,
                    /* iconResourceId = */  0);
            List<Entry> phoneticList = new ArrayList<>();
            phoneticList.add(phoneticEntry);
            // Phonetic name comes after nickname. Check to see if the first entry type is nickname
            if (aboutCardEntries.size() > 0 && aboutCardEntries.get(0).get(0).getHeader().equals(
                    getResources().getString(R.string.header_nickname_entry))) {
                aboutCardEntries.add(1, phoneticList);
            } else {
                aboutCardEntries.add(0, phoneticList);
            }
        }

        if (!TextUtils.isEmpty(customAboutCardName)) {
            mAboutCard.setTitle(customAboutCardName);
        }

        mAboutCard.initialize(aboutCardEntries,
                /* numInitialVisibleEntries = */ 1,
                /* isExpanded = */ true,
                /* isAlwaysExpanded = */ true,
                mExpandingEntryCardViewListener,
                mScroller);

        if (contactCardEntries.size() == 0 && aboutCardEntries.size() == 0) {
            initializeNoContactDetailCard(cp2DataCardModel.areAllRawContactsSimAccounts);
        } else {
            mNoContactDetailsCard.setVisibility(View.GONE);
        }

        // If the Recent card is already initialized (all recent data is loaded), show the About
        // card if it has entries. Otherwise About card visibility will be set in bindRecentData()
        if (aboutCardEntries.size() > 0) {
            if (mAboutCard.getVisibility() == View.GONE && mShouldLog) {
                Logger.logQuickContactEvent(mReferrer, mContactType, CardType.ABOUT,
                        ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
            }
            if (isAllRecentDataLoaded()) {
                mAboutCard.setVisibility(View.VISIBLE);
            }
        }
        Trace.endSection();
    }

    /**
     * Create a card that shows "Add email" and "Add phone number" entries in grey.
     * When contact is a SIM contact, only shows "Add phone number".
     */
    private void initializeNoContactDetailCard(boolean areAllRawContactsSimAccounts) {
        final Drawable phoneIcon = ResourcesCompat.getDrawable(getResources(),
                R.drawable.quantum_ic_phone_vd_theme_24, null).mutate();
        final Entry phonePromptEntry = new Entry(CARD_ENTRY_ID_EDIT_CONTACT,
                phoneIcon, getString(R.string.quickcontact_add_phone_number),
                /* subHeader = */ null, /* subHeaderIcon = */ null, /* text = */ null,
                /* textIcon = */ null, /* primaryContentDescription = */ null,
                getEditContactIntent(),
                /* alternateIcon = */ null, /* alternateIntent = */ null,
                /* alternateContentDescription = */ null, /* shouldApplyColor = */ true,
                /* isEditable = */ false, /* EntryContextMenuInfo = */ null,
                /* thirdIcon = */ null, /* thirdIntent = */ null,
                /* thirdContentDescription = */ null,
                /* thirdAction = */ Entry.ACTION_NONE,
                /* thirdExtras = */ null,
                R.drawable.quantum_ic_phone_vd_theme_24);

        final List<List<Entry>> promptEntries = new ArrayList<>();
        promptEntries.add(new ArrayList<Entry>(1));
        promptEntries.get(0).add(phonePromptEntry);

        if (!areAllRawContactsSimAccounts) {
            final Drawable emailIcon = ResourcesCompat.getDrawable(getResources(),
                    R.drawable.quantum_ic_email_vd_theme_24, null).mutate();
            final Entry emailPromptEntry = new Entry(CARD_ENTRY_ID_EDIT_CONTACT,
                    emailIcon, getString(R.string.quickcontact_add_email), /* subHeader = */ null,
                    /* subHeaderIcon = */ null,
                    /* text = */ null, /* textIcon = */ null, /* primaryContentDescription = */ null,
                    getEditContactIntent(), /* alternateIcon = */ null,
                    /* alternateIntent = */ null, /* alternateContentDescription = */ null,
                    /* shouldApplyColor = */ true, /* isEditable = */ false,
                    /* EntryContextMenuInfo = */ null, /* thirdIcon = */ null,
                    /* thirdIntent = */ null, /* thirdContentDescription = */ null,
                    /* thirdAction = */ Entry.ACTION_NONE, /* thirdExtras = */ null,
                    R.drawable.quantum_ic_email_vd_theme_24);

            promptEntries.add(new ArrayList<Entry>(1));
            promptEntries.get(1).add(emailPromptEntry);
        }

        final int subHeaderTextColor = getResources().getColor(
                R.color.quickcontact_entry_sub_header_text_color);
        final PorterDuffColorFilter greyColorFilter =
                new PorterDuffColorFilter(subHeaderTextColor, PorterDuff.Mode.SRC_ATOP);
        mNoContactDetailsCard.initialize(promptEntries, 2, /* isExpanded = */ true,
                /* isAlwaysExpanded = */ true, mExpandingEntryCardViewListener, mScroller);
        if (mNoContactDetailsCard.getVisibility() == View.GONE && mShouldLog) {
            Logger.logQuickContactEvent(mReferrer, mContactType, CardType.NO_CONTACT,
                    ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
        }
        mNoContactDetailsCard.setVisibility(View.VISIBLE);
        mNoContactDetailsCard.setEntryHeaderColor(subHeaderTextColor);
        mNoContactDetailsCard.setColorAndFilter(subHeaderTextColor, greyColorFilter);
    }

    /**
     * Builds the {@link DataItem}s Map out of the Contact.
     * @param data The contact to build the data from.
     * @return A pair containing a list of data items sorted within mimetype and sorted
     *  amongst mimetype. The map goes from mimetype string to the sorted list of data items within
     *  mimetype
     */
    private Cp2DataCardModel generateDataModelFromContact(
            Contact data) {
        Trace.beginSection("Build data items map");

        final Map<String, List<DataItem>> dataItemsMap = new HashMap<>();

        final ResolveCache cache = ResolveCache.getInstance(this);
        for (RawContact rawContact : data.getRawContacts()) {
            for (DataItem dataItem : rawContact.getDataItems()) {
                dataItem.setRawContactId(rawContact.getId());

                final String mimeType = dataItem.getMimeType();
                if (mimeType == null) continue;

                final AccountType accountType = rawContact.getAccountType(this);
                final DataKind dataKind = AccountTypeManager.getInstance(this)
                        .getKindOrFallback(accountType, mimeType);
                if (dataKind == null) continue;

                dataItem.setDataKind(dataKind);

                final boolean hasData = !TextUtils.isEmpty(dataItem.buildDataString(this,
                        dataKind));

                if (isMimeExcluded(mimeType) || !hasData) continue;

                List<DataItem> dataItemListByType = dataItemsMap.get(mimeType);
                if (dataItemListByType == null) {
                    dataItemListByType = new ArrayList<>();
                    dataItemsMap.put(mimeType, dataItemListByType);
                }
                dataItemListByType.add(dataItem);
            }
        }
        Trace.endSection();

        Trace.beginSection("sort within mimetypes");
        /*
         * Sorting is a multi part step. The end result is to a have a sorted list of the most
         * used data items, one per mimetype. Then, within each mimetype, the list of data items
         * for that type is also sorted, based off of {super primary, primary, times used} in that
         * order.
         */
        final List<List<DataItem>> dataItemsList = new ArrayList<>();
        for (List<DataItem> mimeTypeDataItems : dataItemsMap.values()) {
            // Remove duplicate data items
            Collapser.collapseList(mimeTypeDataItems, this);
            // Sort within mimetype
            Collections.sort(mimeTypeDataItems, mWithinMimeTypeDataItemComparator);
            // Add to the list of data item lists
            dataItemsList.add(mimeTypeDataItems);
        }
        Trace.endSection();

        Trace.beginSection("sort amongst mimetypes");
        // Sort amongst mimetypes to bubble up the top data items for the contact card
        Collections.sort(dataItemsList, mAmongstMimeTypeDataItemComparator);
        Trace.endSection();

        Trace.beginSection("cp2 data items to entries");

        final List<List<Entry>> contactCardEntries = new ArrayList<>();
        final List<List<Entry>> aboutCardEntries = buildAboutCardEntries(dataItemsMap);
        final MutableString aboutCardName = new MutableString();

        for (int i = 0; i < dataItemsList.size(); ++i) {
            final List<DataItem> dataItemsByMimeType = dataItemsList.get(i);
            final DataItem topDataItem = dataItemsByMimeType.get(0);
            if (SORTED_ABOUT_CARD_MIMETYPES.contains(topDataItem.getMimeType())) {
                // About card mimetypes are built in buildAboutCardEntries, skip here
                continue;
            } else {
                List<Entry> contactEntries = dataItemsToEntries(dataItemsList.get(i),
                        aboutCardName);
                if (contactEntries.size() > 0) {
                    contactCardEntries.add(contactEntries);
                }
            }
        }

        Trace.endSection();

        final Cp2DataCardModel dataModel = new Cp2DataCardModel();
        dataModel.customAboutCardName = aboutCardName.value;
        dataModel.aboutCardEntries = aboutCardEntries;
        dataModel.contactCardEntries = contactCardEntries;
        dataModel.dataItemsMap = dataItemsMap;
        dataModel.areAllRawContactsSimAccounts = data.areAllRawContactsSimAccounts(this);
        return dataModel;
    }

    /**
     * Class used to hold the About card and Contact cards' data model that gets generated
     * on a background thread. All data is from CP2.
     */
    private static class Cp2DataCardModel {
        /**
         * A map between a mimetype string and the corresponding list of data items. The data items
         * are in sorted order using mWithinMimeTypeDataItemComparator.
         */
        public Map<String, List<DataItem>> dataItemsMap;
        public List<List<Entry>> aboutCardEntries;
        public List<List<Entry>> contactCardEntries;
        public String customAboutCardName;
        public boolean areAllRawContactsSimAccounts;
    }

    private static class MutableString {
        public String value;
    }

    /**
     * Converts a {@link DataItem} into an {@link ExpandingEntryCardView.Entry} for display.
     * If the {@link ExpandingEntryCardView.Entry} has no visual elements, null is returned.
     *
     * This runs on a background thread. This is set as static to avoid accidentally adding
     * additional dependencies on unsafe things (like the Activity).
     *
     * @param dataItem The {@link DataItem} to convert.
     * @param secondDataItem A second {@link DataItem} to help build a full entry for some
     *  mimetypes
     * @return The {@link ExpandingEntryCardView.Entry}, or null if no visual elements are present.
     */
    private static Entry dataItemToEntry(DataItem dataItem, DataItem secondDataItem,
            Context context, Contact contactData,
            final MutableString aboutCardName) {
        Drawable icon = null;
        String header = null;
        String subHeader = null;
        Drawable subHeaderIcon = null;
        String text = null;
        Drawable textIcon = null;
        StringBuilder primaryContentDescription = new StringBuilder();
        Spannable phoneContentDescription = null;
        Spannable smsContentDescription = null;
        Intent intent = null;
        boolean shouldApplyColor = true;
        Drawable alternateIcon = null;
        Intent alternateIntent = null;
        StringBuilder alternateContentDescription = new StringBuilder();
        final boolean isEditable = false;
        EntryContextMenuInfo entryContextMenuInfo = null;
        Drawable thirdIcon = null;
        Intent thirdIntent = null;
        int thirdAction = Entry.ACTION_NONE;
        String thirdContentDescription = null;
        Bundle thirdExtras = null;
        int iconResourceId = 0;

        context = context.getApplicationContext();
        final Resources res = context.getResources();
        DataKind kind = dataItem.getDataKind();

        if (dataItem instanceof ImDataItem) {
            final ImDataItem im = (ImDataItem) dataItem;
            intent = ContactsUtils.buildImIntent(context, im).first;
            final boolean isEmail = im.isCreatedFromEmail();
            final int protocol;
            if (!im.isProtocolValid()) {
                protocol = Im.PROTOCOL_CUSTOM;
            } else {
                protocol = isEmail ? Im.PROTOCOL_GOOGLE_TALK : im.getProtocol();
            }
            if (protocol == Im.PROTOCOL_CUSTOM) {
                // If the protocol is custom, display the "IM" entry header as well to distinguish
                // this entry from other ones
                header = res.getString(R.string.header_im_entry);
                subHeader = Im.getProtocolLabel(res, protocol,
                        im.getCustomProtocol()).toString();
                text = im.getData();
            } else {
                header = Im.getProtocolLabel(res, protocol,
                        im.getCustomProtocol()).toString();
                subHeader = im.getData();
            }
            entryContextMenuInfo = new EntryContextMenuInfo(im.getData(), header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof OrganizationDataItem) {
            final OrganizationDataItem organization = (OrganizationDataItem) dataItem;
            header = res.getString(R.string.header_organization_entry);
            subHeader = organization.getCompany();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            text = organization.getTitle();
        } else if (dataItem instanceof NicknameDataItem) {
            final NicknameDataItem nickname = (NicknameDataItem) dataItem;
            // Build nickname entries
            final boolean isNameRawContact =
                (contactData.getNameRawContactId() == dataItem.getRawContactId());

            final boolean duplicatesTitle =
                isNameRawContact
                && contactData.getDisplayNameSource() == DisplayNameSources.NICKNAME;

            if (!duplicatesTitle) {
                header = res.getString(R.string.header_nickname_entry);
                subHeader = nickname.getName();
                entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                        dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            }
        } else if (dataItem instanceof CustomDataItem) {
            final CustomDataItem customDataItem = (CustomDataItem) dataItem;
            final String summary = customDataItem.getSummary();
            header = TextUtils.isEmpty(summary)
                    ? res.getString(R.string.label_custom_field) : summary;
            subHeader = customDataItem.getContent();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof NoteDataItem) {
            final NoteDataItem note = (NoteDataItem) dataItem;
            header = res.getString(R.string.header_note_entry);
            subHeader = note.getNote();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof WebsiteDataItem) {
            final WebsiteDataItem website = (WebsiteDataItem) dataItem;
            header = res.getString(R.string.header_website_entry);
            subHeader = website.getUrl();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            try {
                final WebAddress webAddress = new WebAddress(website.buildDataStringForDisplay
                        (context, kind));
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(webAddress.toString()));
            } catch (final ParseException e) {
                Log.e(TAG, "Couldn't parse website: " + website.buildDataStringForDisplay(
                        context, kind));
            }
        } else if (dataItem instanceof EventDataItem) {
            final EventDataItem event = (EventDataItem) dataItem;
            final String dataString = event.buildDataStringForDisplay(context, kind);
            final Calendar cal = DateUtils.parseDate(dataString, false);
            if (cal != null) {
                final Date nextAnniversary =
                        DateUtils.getNextAnnualDate(cal);
                final Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
                builder.appendPath("time");
                ContentUris.appendId(builder, nextAnniversary.getTime());
                intent = new Intent(Intent.ACTION_VIEW).setData(builder.build());
            }
            header = res.getString(R.string.header_event_entry);
            if (event.hasKindTypeColumn(kind)) {
                subHeader = EventCompat.getTypeLabel(res, event.getKindTypeColumn(kind),
                        event.getLabel()).toString();
            }
            text = DateUtils.formatDate(context, dataString);
            entryContextMenuInfo = new EntryContextMenuInfo(text, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
        } else if (dataItem instanceof RelationDataItem) {
            final RelationDataItem relation = (RelationDataItem) dataItem;
            final String dataString = relation.buildDataStringForDisplay(context, kind);
            if (!TextUtils.isEmpty(dataString)) {
                intent = new Intent(Intent.ACTION_SEARCH);
                intent.putExtra(SearchManager.QUERY, dataString);
                intent.setType(Contacts.CONTENT_TYPE);
            }
            header = res.getString(R.string.header_relation_entry);
            subHeader = relation.getName();
            entryContextMenuInfo = new EntryContextMenuInfo(subHeader, header,
                    dataItem.getMimeType(), dataItem.getId(), dataItem.isSuperPrimary());
            if (relation.hasKindTypeColumn(kind)) {
                text = Relation.getTypeLabel(res,
                        relation.getKindTypeColumn(kind),
                        relation.getLabel()).toString();
            }
        } else if (dataItem instanceof PhoneDataItem) {
            final PhoneDataItem phone = (PhoneDataItem) dataItem;
            String phoneLabel = null;
            if (!TextUtils.isEmpty(phone.getNumber())) {
                primaryContentDescription.append(res.getString(R.string.call_other)).append(" ");
                header = sBidiFormatter.unicodeWrap(phone.buildDataStringForDisplay(context, kind),
                        TextDirectionHeuristics.LTR);
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (phone.hasKindTypeColumn(kind)) {
                    final int kindTypeColumn = phone.getKindTypeColumn(kind);
                    final String label = phone.getLabel();
                    phoneLabel = label;
                    if (kindTypeColumn == Phone.TYPE_CUSTOM && TextUtils.isEmpty(label)) {
                        text = "";
                    } else {
                        text = Phone.getTypeLabel(res, kindTypeColumn, label).toString();
                        phoneLabel= text;
                        primaryContentDescription.append(text).append(" ");
                    }
                }
                primaryContentDescription.append(header);
                phoneContentDescription = com.android.contacts.util.ContactDisplayUtils
                        .getTelephoneTtsSpannable(primaryContentDescription.toString(), header);
                iconResourceId = R.drawable.quantum_ic_phone_vd_theme_24;
                icon = res.getDrawable(iconResourceId);
                if (PhoneCapabilityTester.isPhone(context)) {
                    intent = CallUtil.getCallIntent(phone.getNumber());
                    intent.putExtra(EXTRA_ACTION_TYPE, ActionType.CALL);
                }
                alternateIntent = new Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts(ContactsUtils.SCHEME_SMSTO, phone.getNumber(), null));
                alternateIntent.putExtra(EXTRA_ACTION_TYPE, ActionType.SMS);

                alternateIcon = res.getDrawable(R.drawable.quantum_ic_message_vd_theme_24);
                alternateContentDescription.append(res.getString(R.string.sms_custom, header));
                smsContentDescription = com.android.contacts.util.ContactDisplayUtils
                        .getTelephoneTtsSpannable(alternateContentDescription.toString(), header);

                int videoCapability = CallUtil.getVideoCallingAvailability(context);
                boolean isPresenceEnabled =
                        (videoCapability & CallUtil.VIDEO_CALLING_PRESENCE) != 0;
                boolean isVideoEnabled = (videoCapability & CallUtil.VIDEO_CALLING_ENABLED) != 0;

                if (CallUtil.isCallWithSubjectSupported(context)) {
                    thirdIcon = res.getDrawable(R.drawable.quantum_ic_perm_phone_msg_vd_theme_24);
                    thirdAction = Entry.ACTION_CALL_WITH_SUBJECT;
                    thirdContentDescription =
                            res.getString(R.string.call_with_a_note);
                    // Create a bundle containing the data the call subject dialog requires.
                    thirdExtras = new Bundle();
                    thirdExtras.putLong(CallSubjectDialog.ARG_PHOTO_ID,
                            contactData.getPhotoId());
                    thirdExtras.putParcelable(CallSubjectDialog.ARG_PHOTO_URI,
                            UriUtils.parseUriOrNull(contactData.getPhotoUri()));
                    thirdExtras.putParcelable(CallSubjectDialog.ARG_CONTACT_URI,
                            contactData.getLookupUri());
                    thirdExtras.putString(CallSubjectDialog.ARG_NAME_OR_NUMBER,
                            contactData.getDisplayName());
                    thirdExtras.putBoolean(CallSubjectDialog.ARG_IS_BUSINESS, false);
                    thirdExtras.putString(CallSubjectDialog.ARG_NUMBER,
                            phone.getNumber());
                    thirdExtras.putString(CallSubjectDialog.ARG_DISPLAY_NUMBER,
                            phone.getFormattedPhoneNumber());
                    thirdExtras.putString(CallSubjectDialog.ARG_NUMBER_LABEL,
                            phoneLabel);
                } else if (isVideoEnabled) {
                    // Check to ensure carrier presence indicates the number supports video calling.
                    int carrierPresence = dataItem.getCarrierPresence();
                    boolean isPresent = (carrierPresence & Phone.CARRIER_PRESENCE_VT_CAPABLE) != 0;

                    if ((isPresenceEnabled && isPresent) || !isPresenceEnabled) {
                        thirdIcon = res.getDrawable(R.drawable.quantum_ic_videocam_vd_theme_24);
                        thirdAction = Entry.ACTION_INTENT;
                        thirdIntent = CallUtil.getVideoCallIntent(phone.getNumber(),
                                CALL_ORIGIN_QUICK_CONTACTS_ACTIVITY);
                        thirdIntent.putExtra(EXTRA_ACTION_TYPE, ActionType.VIDEOCALL);
                        thirdContentDescription =
                                res.getString(R.string.description_video_call);
                    }
                }
            }
        } else if (dataItem instanceof EmailDataItem) {
            final EmailDataItem email = (EmailDataItem) dataItem;
            final String address = email.getData();
            if (!TextUtils.isEmpty(address)) {
                primaryContentDescription.append(res.getString(R.string.email_other)).append(" ");
                final Uri mailUri = Uri.fromParts(ContactsUtils.SCHEME_MAILTO, address, null);
                intent = new Intent(Intent.ACTION_SENDTO, mailUri);
                intent.putExtra(EXTRA_ACTION_TYPE, ActionType.EMAIL);
                header = email.getAddress();
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.emailLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (email.hasKindTypeColumn(kind)) {
                    text = Email.getTypeLabel(res, email.getKindTypeColumn(kind),
                            email.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                iconResourceId = R.drawable.quantum_ic_email_vd_theme_24;
                icon = res.getDrawable(iconResourceId);
            }
        } else if (dataItem instanceof StructuredPostalDataItem) {
            StructuredPostalDataItem postal = (StructuredPostalDataItem) dataItem;
            final String postalAddress = postal.getFormattedAddress();
            if (!TextUtils.isEmpty(postalAddress)) {
                primaryContentDescription.append(res.getString(R.string.map_other)).append(" ");
                intent = StructuredPostalUtils.getViewPostalAddressIntent(postalAddress);
                intent.putExtra(EXTRA_ACTION_TYPE, ActionType.ADDRESS);
                header = postal.getFormattedAddress();
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.postalLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (postal.hasKindTypeColumn(kind)) {
                    text = StructuredPostal.getTypeLabel(res,
                            postal.getKindTypeColumn(kind), postal.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                alternateIntent =
                        StructuredPostalUtils.getViewPostalAddressDirectionsIntent(postalAddress);
                alternateIntent.putExtra(EXTRA_ACTION_TYPE, ActionType.DIRECTIONS);
                alternateIcon = res.getDrawable(R.drawable.quantum_ic_directions_vd_theme_24);
                alternateContentDescription.append(res.getString(
                        R.string.content_description_directions)).append(" ").append(header);
                iconResourceId = R.drawable.quantum_ic_place_vd_theme_24;
                icon = res.getDrawable(iconResourceId);
            }
        } else if (dataItem instanceof SipAddressDataItem) {
            final SipAddressDataItem sip = (SipAddressDataItem) dataItem;
            final String address = sip.getSipAddress();
            if (!TextUtils.isEmpty(address)) {
                primaryContentDescription.append(res.getString(R.string.call_other)).append(
                        " ");
                if (PhoneCapabilityTester.isSipPhone(context)) {
                    final Uri callUri = Uri.fromParts(PhoneAccount.SCHEME_SIP, address, null);
                    intent = CallUtil.getCallIntent(callUri);
                    intent.putExtra(EXTRA_ACTION_TYPE, ActionType.SIPCALL);
                }
                header = address;
                entryContextMenuInfo = new EntryContextMenuInfo(header,
                        res.getString(R.string.phoneLabelsGroup), dataItem.getMimeType(),
                        dataItem.getId(), dataItem.isSuperPrimary());
                if (sip.hasKindTypeColumn(kind)) {
                    text = SipAddress.getTypeLabel(res,
                            sip.getKindTypeColumn(kind), sip.getLabel()).toString();
                    primaryContentDescription.append(text).append(" ");
                }
                primaryContentDescription.append(header);
                iconResourceId = R.drawable.quantum_ic_dialer_sip_vd_theme_24;
                icon = res.getDrawable(iconResourceId);
            }
        } else if (dataItem instanceof StructuredNameDataItem) {
            // If the name is already set and this is not the super primary value then leave the
            // current value. This way we show the super primary value when we are able to.
            if (dataItem.isSuperPrimary() || aboutCardName.value == null
                    || aboutCardName.value.isEmpty()) {
                final String givenName = ((StructuredNameDataItem) dataItem).getGivenName();
                if (!TextUtils.isEmpty(givenName)) {
                    aboutCardName.value = res.getString(R.string.about_card_title) +
                            " " + givenName;
                } else {
                    aboutCardName.value = res.getString(R.string.about_card_title);
                }
            }
        } else {
            // Custom DataItem
            header = dataItem.buildDataStringForDisplay(context, kind);
            text = kind.typeColumn;
            intent = new Intent(Intent.ACTION_VIEW);
            final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, dataItem.getId());
            intent.setDataAndType(uri, dataItem.getMimeType());
            intent.putExtra(EXTRA_ACTION_TYPE, ActionType.THIRD_PARTY);
            intent.putExtra(EXTRA_THIRD_PARTY_ACTION, dataItem.getMimeType());

            if (intent != null) {
                final String mimetype = intent.getType();

                // Build advanced entry for known 3p types. Otherwise default to ResolveCache icon.
                if (MIMETYPE_HANGOUTS.equals(mimetype)) {
                    // If a secondDataItem is available, use it to build an entry with
                    // alternate actions
                    if (secondDataItem != null) {
                        icon = res.getDrawable(R.drawable.quantum_ic_hangout_vd_theme_24);
                        alternateIcon = res.getDrawable(
                                R.drawable.quantum_ic_hangout_video_vd_theme_24);
                        final HangoutsDataItemModel itemModel =
                                new HangoutsDataItemModel(intent, alternateIntent,
                                        dataItem, secondDataItem, alternateContentDescription,
                                        header, text, context);

                        populateHangoutsDataItemModel(itemModel);
                        intent = itemModel.intent;
                        alternateIntent = itemModel.alternateIntent;
                        alternateContentDescription = itemModel.alternateContentDescription;
                        header = itemModel.header;
                        text = itemModel.text;
                    } else {
                        if (HANGOUTS_DATA_5_VIDEO.equals(intent.getDataString())) {
                            icon = res.getDrawable(R.drawable.quantum_ic_hangout_video_vd_theme_24);
                        } else {
                            icon = res.getDrawable(R.drawable.quantum_ic_hangout_vd_theme_24);
                        }
                    }
                } else {
                    icon = ResolveCache.getInstance(context).getIcon(
                            dataItem.getMimeType(), intent);
                    // Call mutate to create a new Drawable.ConstantState for color filtering
                    if (icon != null) {
                        icon.mutate();
                    }
                    shouldApplyColor = false;

                    if (!MIMETYPE_GPLUS_PROFILE.equals(mimetype)) {
                        entryContextMenuInfo = new EntryContextMenuInfo(header, mimetype,
                                dataItem.getMimeType(), dataItem.getId(),
                                dataItem.isSuperPrimary());
                    }
                }
            }
        }

        if (intent != null) {
            // Do not set the intent is there are no resolves
            if (!PhoneCapabilityTester.isIntentRegistered(context, intent)) {
                intent = null;
            }
        }

        if (alternateIntent != null) {
            // Do not set the alternate intent is there are no resolves
            if (!PhoneCapabilityTester.isIntentRegistered(context, alternateIntent)) {
                alternateIntent = null;
            } else if (TextUtils.isEmpty(alternateContentDescription)) {
                // Attempt to use package manager to find a suitable content description if needed
                alternateContentDescription.append(getIntentResolveLabel(alternateIntent, context));
            }
        }

        // If the Entry has no visual elements, return null
        if (icon == null && TextUtils.isEmpty(header) && TextUtils.isEmpty(subHeader) &&
                subHeaderIcon == null && TextUtils.isEmpty(text) && textIcon == null) {
            return null;
        }

        // Ignore dataIds from the Me profile.
        final int dataId = dataItem.getId() > Integer.MAX_VALUE ?
                -1 : (int) dataItem.getId();

        return new Entry(dataId, icon, header, subHeader, subHeaderIcon, text, textIcon,
                phoneContentDescription == null
                        ? new SpannableString(primaryContentDescription.toString())
                        : phoneContentDescription,
                intent, alternateIcon, alternateIntent,
                smsContentDescription == null
                        ? new SpannableString(alternateContentDescription.toString())
                        : smsContentDescription,
                shouldApplyColor, isEditable,
                entryContextMenuInfo, thirdIcon, thirdIntent, thirdContentDescription, thirdAction,
                thirdExtras, iconResourceId);
    }

    private List<Entry> dataItemsToEntries(List<DataItem> dataItems,
            MutableString aboutCardTitleOut) {
        // Hangouts and G+ use two data items to create one entry.
        if (dataItems.get(0).getMimeType().equals(MIMETYPE_GPLUS_PROFILE)) {
            return gPlusDataItemsToEntries(dataItems);
        } else if (dataItems.get(0).getMimeType().equals(MIMETYPE_HANGOUTS)) {
            return hangoutsDataItemsToEntries(dataItems);
        } else {
            final List<Entry> entries = new ArrayList<>();
            for (DataItem dataItem : dataItems) {
                final Entry entry = dataItemToEntry(dataItem, /* secondDataItem = */ null,
                        this, mContactData, aboutCardTitleOut);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return entries;
        }
    }

    /**
     * Put the data items into buckets based on the raw contact id
     */
    private Map<Long, List<DataItem>> dataItemsToBucket(List<DataItem> dataItems) {
        final Map<Long, List<DataItem>> buckets = new HashMap<>();
        for (DataItem dataItem : dataItems) {
            List<DataItem> bucket = buckets.get(dataItem.getRawContactId());
            if (bucket == null) {
                bucket = new ArrayList<>();
                buckets.put(dataItem.getRawContactId(), bucket);
            }
            bucket.add(dataItem);
        }
        return buckets;
    }

    /**
     * For G+ entries, a single ExpandingEntryCardView.Entry consists of two data items. This
     * method use only the View profile to build entry.
     */
    private List<Entry> gPlusDataItemsToEntries(List<DataItem> dataItems) {
        final List<Entry> entries = new ArrayList<>();

        for (List<DataItem> bucket : dataItemsToBucket(dataItems).values()) {
            for (DataItem dataItem : bucket) {
                if (GPLUS_PROFILE_DATA_5_VIEW_PROFILE.equals(
                        dataItem.getContentValues().getAsString(Data.DATA5))) {
                    final Entry entry = dataItemToEntry(dataItem, /* secondDataItem = */ null,
                            this, mContactData, /* aboutCardName = */ null);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * For Hangouts entries, a single ExpandingEntryCardView.Entry consists of two data items. This
     * method attempts to build each entry using the two data items if they are available. If there
     * are more or less than two data items, a fall back is used and each data item gets its own
     * entry.
     */
    private List<Entry> hangoutsDataItemsToEntries(List<DataItem> dataItems) {
        final List<Entry> entries = new ArrayList<>();

        // Use the buckets to build entries. If a bucket contains two data items, build the special
        // entry, otherwise fall back to the normal entry.
        for (List<DataItem> bucket : dataItemsToBucket(dataItems).values()) {
            if (bucket.size() == 2) {
                // Use the pair to build an entry
                final Entry entry = dataItemToEntry(bucket.get(0),
                        /* secondDataItem = */ bucket.get(1), this, mContactData,
                        /* aboutCardName = */ null);
                if (entry != null) {
                    entries.add(entry);
                }
            } else {
                for (DataItem dataItem : bucket) {
                    final Entry entry = dataItemToEntry(dataItem, /* secondDataItem = */ null,
                            this, mContactData, /* aboutCardName = */ null);
                    if (entry != null) {
                        entries.add(entry);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * Used for statically passing around Hangouts data items and entry fields to
     * populateHangoutsDataItemModel.
     */
    private static final class HangoutsDataItemModel {
        public Intent intent;
        public Intent alternateIntent;
        public DataItem dataItem;
        public DataItem secondDataItem;
        public StringBuilder alternateContentDescription;
        public String header;
        public String text;
        public Context context;

        public HangoutsDataItemModel(Intent intent, Intent alternateIntent, DataItem dataItem,
                DataItem secondDataItem, StringBuilder alternateContentDescription, String header,
                String text, Context context) {
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

    private static void populateHangoutsDataItemModel(
            HangoutsDataItemModel dataModel) {
        final Intent secondIntent = new Intent(Intent.ACTION_VIEW);
        secondIntent.setDataAndType(ContentUris.withAppendedId(Data.CONTENT_URI,
                dataModel.secondDataItem.getId()), dataModel.secondDataItem.getMimeType());
        secondIntent.putExtra(EXTRA_ACTION_TYPE, ActionType.THIRD_PARTY);
        secondIntent.putExtra(EXTRA_THIRD_PARTY_ACTION, dataModel.secondDataItem.getMimeType());

        // There is no guarantee the order the data items come in. Second
        // data item does not necessarily mean it's the alternate.
        // Hangouts video should be alternate. Swap if needed
        if (HANGOUTS_DATA_5_VIDEO.equals(
                dataModel.dataItem.getContentValues().getAsString(Data.DATA5))) {
            dataModel.alternateIntent = dataModel.intent;
            dataModel.alternateContentDescription = new StringBuilder(dataModel.header);

            dataModel.intent = secondIntent;
            dataModel.header = dataModel.secondDataItem.buildDataStringForDisplay(
                    dataModel.context, dataModel.secondDataItem.getDataKind());
            dataModel.text = dataModel.secondDataItem.getDataKind().typeColumn;
        } else if (HANGOUTS_DATA_5_MESSAGE.equals(
                dataModel.dataItem.getContentValues().getAsString(Data.DATA5))) {
            dataModel.alternateIntent = secondIntent;
            dataModel.alternateContentDescription = new StringBuilder(
                    dataModel.secondDataItem.buildDataStringForDisplay(dataModel.context,
                            dataModel.secondDataItem.getDataKind()));
        }
    }

    private static String getIntentResolveLabel(Intent intent, Context context) {
        final List<ResolveInfo> matches = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        // Pick first match, otherwise best found
        ResolveInfo bestResolve = null;
        final int size = matches.size();
        if (size == 1) {
            bestResolve = matches.get(0);
        } else if (size > 1) {
            bestResolve = ResolveCache.getInstance(context).getBestResolve(intent, matches);
        }

        if (bestResolve == null) {
            return null;
        }

        return String.valueOf(bestResolve.loadLabel(context.getPackageManager()));
    }

    /**
     * Asynchronously extract the most vibrant color from the PhotoView. Once extracted,
     * apply this tint to {@link MultiShrinkScroller}. This operation takes about 20-30ms
     * on a Nexus 5.
     */
    private void extractAndApplyTintFromPhotoViewAsynchronously() {
        if (mScroller == null) {
            return;
        }
        final Drawable imageViewDrawable = mPhotoView.getDrawable();
        new AsyncTask<Void, Void, MaterialPalette>() {
            @Override
            protected MaterialPalette doInBackground(Void... params) {

                if (imageViewDrawable instanceof BitmapDrawable && mContactData != null
                        && mContactData.getThumbnailPhotoBinaryData() != null
                        && mContactData.getThumbnailPhotoBinaryData().length > 0) {
                    // Perform the color analysis on the thumbnail instead of the full sized
                    // image, so that our results will be as similar as possible to the Bugle
                    // app.
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(
                            mContactData.getThumbnailPhotoBinaryData(), 0,
                            mContactData.getThumbnailPhotoBinaryData().length);
                    try {
                        final int primaryColor = colorFromBitmap(bitmap);
                        if (primaryColor != 0) {
                            return mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(
                                    primaryColor);
                        }
                    } finally {
                        bitmap.recycle();
                    }
                }
                if (imageViewDrawable instanceof LetterTileDrawable) {
                    final int primaryColor = ((LetterTileDrawable) imageViewDrawable).getColor();
                    return mMaterialColorMapUtils.calculatePrimaryAndSecondaryColor(primaryColor);
                }
                return MaterialColorMapUtils.getDefaultPrimaryAndSecondaryColors(getResources());
            }

            @Override
            protected void onPostExecute(MaterialPalette palette) {
                super.onPostExecute(palette);
                if (mHasComputedThemeColor) {
                    // If we had previously computed a theme color from the contact photo,
                    // then do not update the theme color. Changing the theme color several
                    // seconds after QC has started, as a result of an updated/upgraded photo,
                    // is a jarring experience. On the other hand, changing the theme color after
                    // a rotation or onNewIntent() is perfectly fine.
                    return;
                }
                // Check that the Photo has not changed. If it has changed, the new tint
                // color needs to be extracted
                if (imageViewDrawable == mPhotoView.getDrawable()) {
                    mHasComputedThemeColor = true;
                    setThemeColor(palette);
                }
            }
        }.execute();
    }

    private void setThemeColor(MaterialPalette palette) {
        // If the color is invalid, use the predefined default
        mColorFilterColor = palette.mPrimaryColor;
        mScroller.setHeaderTintColor(mColorFilterColor);
        mStatusBarColor = palette.mSecondaryColor;
        updateStatusBarColor();

        mColorFilter =
                new PorterDuffColorFilter(mColorFilterColor, PorterDuff.Mode.SRC_ATOP);
        mContactCard.setColorAndFilter(mColorFilterColor, mColorFilter);
        mRecentCard.setColorAndFilter(mColorFilterColor, mColorFilter);
        mAboutCard.setColorAndFilter(mColorFilterColor, mColorFilter);
    }

    private void updateStatusBarColor() {
        if (mScroller == null || !CompatUtils.isLollipopCompatible()) {
            return;
        }
        final int desiredStatusBarColor;
        // Only use a custom status bar color if QuickContacts touches the top of the viewport.
        if (mScroller.getScrollNeededToBeFullScreen() <= 0) {
            desiredStatusBarColor = mStatusBarColor;
        } else {
            desiredStatusBarColor = Color.TRANSPARENT;
        }
        // Animate to the new color.
        final ObjectAnimator animation = ObjectAnimator.ofInt(getWindow(), "statusBarColor",
                getWindow().getStatusBarColor(), desiredStatusBarColor);
        animation.setDuration(ANIMATION_STATUS_BAR_COLOR_CHANGE_DURATION);
        animation.setEvaluator(new ArgbEvaluator());
        animation.start();
    }

    private int colorFromBitmap(Bitmap bitmap) {
        // Author of Palette recommends using 24 colors when analyzing profile photos.
        final int NUMBER_OF_PALETTE_COLORS = 24;
        final Palette palette = Palette.generate(bitmap, NUMBER_OF_PALETTE_COLORS);
        if (palette != null && palette.getVibrantSwatch() != null) {
            return palette.getVibrantSwatch().getRgb();
        }
        return 0;
    }

    private List<Entry> contactInteractionsToEntries(List<ContactInteraction> interactions) {
        final List<Entry> entries = new ArrayList<>();
        for (ContactInteraction interaction : interactions) {
            if (interaction == null) {
                continue;
            }
            entries.add(new Entry(/* id = */ -1,
                    interaction.getIcon(this),
                    interaction.getViewHeader(this),
                    interaction.getViewBody(this),
                    interaction.getBodyIcon(this),
                    interaction.getViewFooter(this),
                    interaction.getFooterIcon(this),
                    interaction.getContentDescription(this),
                    interaction.getIntent(),
                    /* alternateIcon = */ null,
                    /* alternateIntent = */ null,
                    /* alternateContentDescription = */ null,
                    /* shouldApplyColor = */ true,
                    /* isEditable = */ false,
                    /* EntryContextMenuInfo = */ null,
                    /* thirdIcon = */ null,
                    /* thirdIntent = */ null,
                    /* thirdContentDescription = */ null,
                    /* thirdAction = */ Entry.ACTION_NONE,
                    /* thirdActionExtras = */ null,
                    interaction.getIconResourceId()));
        }
        return entries;
    }

    private final LoaderCallbacks<Contact> mLoaderContactCallbacks =
            new LoaderCallbacks<Contact>() {
        @Override
        public void onLoaderReset(Loader<Contact> loader) {
            mContactData = null;
        }

        @Override
        public void onLoadFinished(Loader<Contact> loader, Contact data) {
            Trace.beginSection("onLoadFinished()");
            try {

                if (isFinishing()) {
                    return;
                }
                if (data.isError()) {
                    // This means either the contact is invalid or we had an
                    // internal error such as an acore crash.
                    Log.i(TAG, "Failed to load contact: " + ((ContactLoader)loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                if (data.isNotFound()) {
                    Log.i(TAG, "No contact found: " + ((ContactLoader)loader).getLookupUri());
                    Toast.makeText(QuickContactActivity.this, R.string.invalidContactMessage,
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                if (!mIsRecreatedInstance && !mShortcutUsageReported && data != null) {
                    mShortcutUsageReported = true;
                    DynamicShortcuts.reportShortcutUsed(QuickContactActivity.this,
                            data.getLookupKey());
                }
                bindContactData(data);

            } finally {
                Trace.endSection();
            }
        }

        @Override
        public Loader<Contact> onCreateLoader(int id, Bundle args) {
            if (mLookupUri == null) {
                Log.wtf(TAG, "Lookup uri wasn't initialized. Loader was started too early");
            }
            // Load all contact data. We need loadGroupMetaData=true to determine whether the
            // contact is invisible. If it is, we need to display an "Add to Contacts" MenuItem.
            return new ContactLoader(getApplicationContext(), mLookupUri,
                    true /*loadGroupMetaData*/, true /*postViewNotification*/,
                    true /*computeFormattedPhoneNumber*/);
        }
    };

    @Override
    public void onBackPressed() {
        final int previousScreenType = getIntent().getIntExtra
                (EXTRA_PREVIOUS_SCREEN_TYPE, ScreenType.UNKNOWN);
        if ((previousScreenType == ScreenType.ALL_CONTACTS
                || previousScreenType == ScreenType.FAVORITES)
                && !SharedPreferenceUtil.getHamburgerPromoTriggerActionHappenedBefore(this)) {
            SharedPreferenceUtil.setHamburgerPromoTriggerActionHappenedBefore(this);
        }
        if (mScroller != null) {
            if (!mIsExitAnimationInProgress) {
                mScroller.scrollOffBottom();
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void finish() {
        super.finish();

        // override transitions to skip the standard window animations
        overridePendingTransition(0, 0);
    }

    private final LoaderCallbacks<List<ContactInteraction>> mLoaderInteractionsCallbacks =
            new LoaderCallbacks<List<ContactInteraction>>() {

        @Override
        public Loader<List<ContactInteraction>> onCreateLoader(int id, Bundle args) {
            Loader<List<ContactInteraction>> loader = null;
            switch (id) {
                case LOADER_SMS_ID:
                    loader = new SmsInteractionsLoader(
                            QuickContactActivity.this,
                            args.getStringArray(KEY_LOADER_EXTRA_PHONES),
                            MAX_SMS_RETRIEVE);
                    break;
                case LOADER_CALENDAR_ID:
                    final String[] emailsArray = args.getStringArray(KEY_LOADER_EXTRA_EMAILS);
                    List<String> emailsList = null;
                    if (emailsArray != null) {
                        emailsList = Arrays.asList(args.getStringArray(KEY_LOADER_EXTRA_EMAILS));
                    }
                    loader = new CalendarInteractionsLoader(
                            QuickContactActivity.this,
                            emailsList,
                            MAX_FUTURE_CALENDAR_RETRIEVE,
                            MAX_PAST_CALENDAR_RETRIEVE,
                            FUTURE_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR,
                            PAST_MILLISECOND_TO_SEARCH_LOCAL_CALENDAR);
                    break;
                case LOADER_CALL_LOG_ID:
                    loader = new CallLogInteractionsLoader(
                            QuickContactActivity.this,
                            args.getStringArray(KEY_LOADER_EXTRA_PHONES),
                            args.getStringArray(KEY_LOADER_EXTRA_SIP_NUMBERS),
                            MAX_CALL_LOG_RETRIEVE);
            }
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<List<ContactInteraction>> loader,
                List<ContactInteraction> data) {
            mRecentLoaderResults.put(loader.getId(), data);

            if (isAllRecentDataLoaded()) {
                bindRecentData();
            }
        }

        @Override
        public void onLoaderReset(Loader<List<ContactInteraction>> loader) {
            mRecentLoaderResults.remove(loader.getId());
        }
    };

    private boolean isAllRecentDataLoaded() {
        return mRecentLoaderResults.size() == mRecentLoaderIds.length;
    }

    private void bindRecentData() {
        final List<ContactInteraction> allInteractions = new ArrayList<>();
        final List<List<Entry>> interactionsWrapper = new ArrayList<>();

        // Serialize mRecentLoaderResults into a single list. This should be done on the main
        // thread to avoid races against mRecentLoaderResults edits.
        for (List<ContactInteraction> loaderInteractions : mRecentLoaderResults.values()) {
            allInteractions.addAll(loaderInteractions);
        }

        mRecentDataTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Trace.beginSection("sort recent loader results");

                // Sort the interactions by most recent
                Collections.sort(allInteractions, new Comparator<ContactInteraction>() {
                    @Override
                    public int compare(ContactInteraction a, ContactInteraction b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return 1;
                        }
                        if (b == null) {
                            return -1;
                        }
                        if (a.getInteractionDate() > b.getInteractionDate()) {
                            return -1;
                        }
                        if (a.getInteractionDate() == b.getInteractionDate()) {
                            return 0;
                        }
                        return 1;
                    }
                });

                Trace.endSection();
                Trace.beginSection("contactInteractionsToEntries");

                // Wrap each interaction in its own list so that an icon is displayed for each entry
                for (Entry contactInteraction : contactInteractionsToEntries(allInteractions)) {
                    List<Entry> entryListWrapper = new ArrayList<>(1);
                    entryListWrapper.add(contactInteraction);
                    interactionsWrapper.add(entryListWrapper);
                }

                Trace.endSection();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Trace.beginSection("initialize recents card");

                if (allInteractions.size() > 0) {
                    mRecentCard.initialize(interactionsWrapper,
                    /* numInitialVisibleEntries = */ MIN_NUM_COLLAPSED_RECENT_ENTRIES_SHOWN,
                    /* isExpanded = */ mRecentCard.isExpanded(), /* isAlwaysExpanded = */ false,
                            mExpandingEntryCardViewListener, mScroller);
                    if (mRecentCard.getVisibility() == View.GONE && mShouldLog) {
                        Logger.logQuickContactEvent(mReferrer, mContactType, CardType.RECENT,
                                ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
                    }
                    mRecentCard.setVisibility(View.VISIBLE);
                } else {
                    mRecentCard.setVisibility(View.GONE);
                }

                Trace.endSection();
                Trace.beginSection("initialize permission explanation card");

                final Drawable historyIcon = ResourcesCompat.getDrawable(getResources(),
                        R.drawable.quantum_ic_history_vd_theme_24, null);

                final Entry permissionExplanationEntry = new Entry(CARD_ENTRY_ID_REQUEST_PERMISSION,
                        historyIcon, getString(R.string.permission_explanation_header),
                        mPermissionExplanationCardSubHeader, /* subHeaderIcon = */ null,
                        /* text = */ null, /* textIcon = */ null,
                        /* primaryContentDescription = */ null, getIntent(),
                        /* alternateIcon = */ null, /* alternateIntent = */ null,
                        /* alternateContentDescription = */ null, /* shouldApplyColor = */ true,
                        /* isEditable = */ false, /* EntryContextMenuInfo = */ null,
                        /* thirdIcon = */ null, /* thirdIntent = */ null,
                        /* thirdContentDescription = */ null, /* thirdAction = */ Entry.ACTION_NONE,
                        /* thirdExtras = */ null, R.drawable.quantum_ic_history_vd_theme_24);

                final List<List<Entry>> permissionExplanationEntries = new ArrayList<>();
                permissionExplanationEntries.add(new ArrayList<Entry>());
                permissionExplanationEntries.get(0).add(permissionExplanationEntry);

                final int subHeaderTextColor = getResources().getColor(android.R.color.white);
                final PorterDuffColorFilter whiteColorFilter =
                        new PorterDuffColorFilter(subHeaderTextColor, PorterDuff.Mode.SRC_ATOP);

                mPermissionExplanationCard.initialize(permissionExplanationEntries,
                        /* numInitialVisibleEntries = */ 1,
                        /* isExpanded = */ true,
                        /* isAlwaysExpanded = */ true,
                        /* listener = */ null,
                        mScroller);

                mPermissionExplanationCard.setColorAndFilter(subHeaderTextColor, whiteColorFilter);
                mPermissionExplanationCard.setBackgroundColor(mColorFilterColor);
                mPermissionExplanationCard.setEntryHeaderColor(subHeaderTextColor);
                mPermissionExplanationCard.setEntrySubHeaderColor(subHeaderTextColor);

                if (mShouldShowPermissionExplanation) {
                    if (mPermissionExplanationCard.getVisibility() == View.GONE
                            && mShouldLog) {
                        Logger.logQuickContactEvent(mReferrer, mContactType, CardType.PERMISSION,
                                ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
                    }
                    mPermissionExplanationCard.setVisibility(View.VISIBLE);
                } else {
                    mPermissionExplanationCard.setVisibility(View.GONE);
                }

                Trace.endSection();

                // About card is initialized along with the contact card, but since it appears after
                // the recent card in the UI, we hold off until making it visible until the recent
                // card is also ready to avoid stuttering.
                if (mAboutCard.shouldShow()) {
                    mAboutCard.setVisibility(View.VISIBLE);
                } else {
                    mAboutCard.setVisibility(View.GONE);
                }
                mRecentDataTask = null;
            }
        };
        mRecentDataTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mEntriesAndActionsTask != null) {
            // Once the activity is stopped, we will no longer want to bind mEntriesAndActionsTask's
            // results on the UI thread. In some circumstances Activities are killed without
            // onStop() being called. This is not a problem, because in these circumstances
            // the entire process will be killed.
            mEntriesAndActionsTask.cancel(/* mayInterruptIfRunning = */ false);
        }
        if (mRecentDataTask != null) {
            mRecentDataTask.cancel(/* mayInterruptIfRunning = */ false);
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mListener);
        super.onDestroy();
    }

    /**
     * Returns true if it is possible to edit the current contact.
     */
    private boolean isContactEditable() {
        return mContactData != null && !mContactData.isDirectoryEntry();
    }

    /**
     * Returns true if it is possible to share the current contact.
     */
    private boolean isContactShareable() {
        return mContactData != null && !mContactData.isDirectoryEntry();
    }

    private Intent getEditContactIntent() {
        return EditorIntents.createEditContactIntent(QuickContactActivity.this,
                mContactData.getLookupUri(),
                mHasComputedThemeColor
                        ? new MaterialPalette(mColorFilterColor, mStatusBarColor) : null,
                mContactData.getPhotoId());
    }

    private void editContact() {
        mHasIntentLaunched = true;
        mContactLoader.cacheResult();
        startActivityForResult(getEditContactIntent(), REQUEST_CODE_CONTACT_EDITOR_ACTIVITY);
    }

    private void deleteContact() {
        final Uri contactUri = mContactData.getLookupUri();
        ContactDeletionInteraction.start(this, contactUri, /* finishActivityWhenDone =*/ true);
    }

    private void toggleStar(MenuItem starredMenuItem, boolean isStarred) {
        // To improve responsiveness, swap out the picture (and tag) in the UI already
        ContactDisplayUtils.configureStarredMenuItem(starredMenuItem,
                mContactData.isDirectoryEntry(), mContactData.isUserProfile(), !isStarred);

        // Now perform the real save
        final Intent intent = ContactSaveService.createSetStarredIntent(
                QuickContactActivity.this, mContactData.getLookupUri(), !isStarred);
        startService(intent);

        final CharSequence accessibilityText = !isStarred
                ? getResources().getText(R.string.description_action_menu_add_star)
                : getResources().getText(R.string.description_action_menu_remove_star);
        // Accessibility actions need to have an associated view. We can't access the MenuItem's
        // underlying view, so put this accessibility action on the root view.
        mScroller.announceForAccessibility(accessibilityText);
    }

    private void shareContact() {
        final String lookupKey = mContactData.getLookupKey();
        final Uri shareUri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey);
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(Contacts.CONTENT_VCARD_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, shareUri);

        // Launch chooser to share contact via
        final CharSequence chooseTitle = getResources().getQuantityString(
                R.plurals.title_share_via, /* quantity */ 1);
        final Intent chooseIntent = Intent.createChooser(intent, chooseTitle);

        try {
            mHasIntentLaunched = true;
            ImplicitIntentsUtil.startActivityOutsideApp(this, chooseIntent);
        } catch (final ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Creates a launcher shortcut with the current contact.
     */
    private void createLauncherShortcutWithContact() {
        final ShortcutIntentBuilder builder = new ShortcutIntentBuilder(this,
                new OnShortcutIntentCreatedListener() {

                    @Override
                    public void onShortcutIntentCreated(Uri uri, Intent shortcutIntent) {
                        if (BuildCompat.isAtLeastO()) {
                            final ShortcutManager shortcutManager = (ShortcutManager)
                                    getSystemService(SHORTCUT_SERVICE);
                            final DynamicShortcuts shortcuts =
                                    new DynamicShortcuts(QuickContactActivity.this);
                            Account account = null;
                            if (mContactData.getAccountName() != null) {
                                account = new Account(mContactData.getAccountName(),
                                        mContactData.getAccountType());
                            }
                            shortcutManager.requestPinShortcut(
                                    shortcuts.getQuickContactShortcutInfo(
                                            mContactData.getId(), mContactData.getLookupKey(),
                                            mContactData.getDisplayName(), account), null);
                        } else {
                            // Broadcast the shortcutIntent to the launcher to create a
                            // shortcut to this contact
                            shortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
                            QuickContactActivity.this.sendBroadcast(shortcutIntent);
                            // Send a toast to give feedback to the user that a shortcut to this
                            // contact was added to the launcher.
                            final String displayName = shortcutIntent
                                    .getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                            final String toastMessage = TextUtils.isEmpty(displayName)
                                    ? getString(R.string.createContactShortcutSuccessful_NoName)
                                    : getString(R.string.createContactShortcutSuccessful,
                                            displayName);
                            Toast.makeText(QuickContactActivity.this, toastMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        builder.createContactShortcutIntent(mContactData.getLookupUri());
    }

    private boolean isShortcutCreatable() {
        if (mContactData == null || mContactData.isUserProfile() ||
                mContactData.isDirectoryEntry()) {
            return false;
        }

        if (BuildCompat.isAtLeastO()) {
            final ShortcutManager manager = (ShortcutManager)
                    getSystemService(Context.SHORTCUT_SERVICE);
            return manager.isRequestPinShortcutSupported();
        }

        final Intent createShortcutIntent = new Intent();
        createShortcutIntent.setAction(ACTION_INSTALL_SHORTCUT);
        final List<ResolveInfo> receivers = getPackageManager()
                .queryBroadcastReceivers(createShortcutIntent, 0);
        return receivers != null && receivers.size() > 0;
    }

    private void setStateForPhoneMenuItems(Contact contact) {
        if (contact != null) {
            mSendToVoicemailState = contact.isSendToVoicemail();
            mCustomRingtone = contact.getCustomRingtone();
            mArePhoneOptionsChangable = isContactEditable()
                    && PhoneCapabilityTester.isPhone(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.quickcontact, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mContactData != null) {
            final MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
            ContactDisplayUtils.configureStarredMenuItem(starredMenuItem,
                    mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                    mContactData.getStarred());
            if (!simOneLoadComplete) {
                simOneLoadComplete = (ContactUtils.getAdnRecordsCapacity(this,
                        SimContactsConstants.SLOT1)[0] > 0) ? true : false;
            }
            if (!simTwoLoadComplete) {
                simTwoLoadComplete = (ContactUtils.getAdnRecordsCapacity(this,
                        SimContactsConstants.SLOT2)[0] > 0)? true : false;
            }
            // Configure edit MenuItem
            final MenuItem editMenuItem = menu.findItem(R.id.menu_edit);
            editMenuItem.setVisible(true);
            if (DirectoryContactUtil.isDirectoryContact(mContactData) || InvisibleContactUtil
                    .isInvisibleAndAddable(mContactData, this)) {
                editMenuItem.setIcon(R.drawable.quantum_ic_person_add_vd_theme_24);
                editMenuItem.setTitle(R.string.menu_add_contact);
            } else if (isContactEditable()) {
                editMenuItem.setIcon(R.drawable.quantum_ic_create_vd_theme_24);
                editMenuItem.setTitle(R.string.menu_editContact);
            } else {
                editMenuItem.setVisible(false);
            }

            // The link menu item is only visible if this has a single raw contact.
            final MenuItem joinMenuItem = menu.findItem(R.id.menu_join);
            joinMenuItem.setVisible(!InvisibleContactUtil.isInvisibleAndAddable(mContactData, this)
                    && isContactEditable() && !mContactData.isUserProfile()
                    && !mContactData.isMultipleRawContacts());

            // Viewing linked contacts can only happen if there are multiple raw contacts and
            // the link menu isn't available.
            final MenuItem linkedContactsMenuItem = menu.findItem(R.id.menu_linked_contacts);
            linkedContactsMenuItem.setVisible(mContactData.isMultipleRawContacts()
                    && !joinMenuItem.isVisible());

            final MenuItem deleteMenuItem = menu.findItem(R.id.menu_delete);
            deleteMenuItem.setVisible(isContactEditable() && !mContactData.isUserProfile());

            final MenuItem shareMenuItem = menu.findItem(R.id.menu_share);
            shareMenuItem.setVisible(isContactShareable());

            final MenuItem shortcutMenuItem = menu.findItem(R.id.menu_create_contact_shortcut);
            shortcutMenuItem.setVisible(isShortcutCreatable());

            // Hide telephony-related settings (ringtone, send to voicemail)
            // if we don't have a telephone
            final MenuItem ringToneMenuItem = menu.findItem(R.id.menu_set_ringtone);
            ringToneMenuItem.setVisible(!mContactData.isUserProfile() && mArePhoneOptionsChangable);

            final MenuItem sendToVoiceMailMenuItem = menu.findItem(R.id.menu_send_to_voicemail);
            sendToVoiceMailMenuItem.setVisible(!mContactData.isUserProfile()
                    && mArePhoneOptionsChangable);
            sendToVoiceMailMenuItem.setTitle(mSendToVoicemailState
                    ? R.string.menu_unredirect_calls_to_vm : R.string.menu_redirect_calls_to_vm);

            final MenuItem helpMenu = menu.findItem(R.id.menu_help);
            helpMenu.setVisible(HelpUtils.isHelpAndFeedbackAvailable());

            final RawContact rawContact = mContactData.getRawContacts().get(0);
            String accoutName = rawContact.getAccountName();
            String accoutType = rawContact.getAccountTypeString();

            final MenuItem copyToPhoneMenu = menu.findItem(R.id.menu_copy_to_phone);
            if (copyToPhoneMenu != null) {
                copyToPhoneMenu.setVisible(false);
            }

            final MenuItem copyToSim1Menu = menu.findItem(R.id.menu_copy_to_sim1);
            if (copyToSim1Menu != null) {
                copyToSim1Menu.setVisible(false);
            }

            final MenuItem copyToSim2Menu = menu.findItem(R.id.menu_copy_to_sim2);
            if (copyToSim2Menu != null) {
                copyToSim2Menu.setVisible(false);
            }

            if (!TextUtils.isEmpty(accoutType)) {
                if (SimContactsConstants.ACCOUNT_TYPE_SIM.equals(accoutType)) {
                    copyToPhoneMenu.setVisible(true);
                    copyToPhoneMenu.setTitle(getString(R.string.menu_copyTo)
                            + getString(R.string.phoneLabelsGroup));
                    if (tm.getPhoneCount() > 1) {
                        if (SimContactsConstants.SIM_NAME_1.equals(accoutName)
                                && simTwoLoadComplete) {
                            copyToSim2Menu.setTitle(getString(R.string.menu_copyTo)
                                    + ContactUtils.getAcount( QuickContactActivity.this,
                                            SimContactsConstants.SLOT2).name);
                            copyToSim2Menu.setVisible(true);
                        }
                        if (SimContactsConstants.SIM_NAME_2.equals(accoutName)
                                && simOneLoadComplete) {
                            copyToSim1Menu.setTitle(getString(R.string.menu_copyTo)
                                    + ContactUtils.getAcount(QuickContactActivity.this,
                                            SimContactsConstants.SLOT1).name);
                            copyToSim1Menu.setVisible(true);
                        }
                    }
                }
            } else {
                copyToPhoneMenu.setVisible(false);
                if (tm.getPhoneCount() > 1) {
                    if (simOneLoadComplete) {
                        copyToSim1Menu.setTitle(getString(R.string.menu_copyTo)
                                + ContactUtils.getAcount(
                                        this, SimContactsConstants.SLOT1).name);
                        copyToSim1Menu.setVisible(true);
                    }
                    if (simTwoLoadComplete) {
                        copyToSim2Menu.setTitle(getString(R.string.menu_copyTo)
                                + ContactUtils.getAcount(
                                        this, SimContactsConstants.SLOT2).name);
                        copyToSim2Menu.setVisible(true);
                    }
                } else {
                    if (simOneLoadComplete) {
                        copyToSim1Menu.setTitle(getString(R.string.menu_copyTo)
                                + SimContactsConstants.SIM_NAME);
                        copyToSim1Menu.setVisible(true);
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_star) {// Make sure there is a contact
            if (mContactData != null) {
                // Read the current starred value from the UI instead of using the last
                // loaded state. This allows rapid tapping without writing the same
                // value several times
                final boolean isStarred = item.isChecked();
                Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                        isStarred ? ActionType.UNSTAR : ActionType.STAR,
                            /* thirdPartyAction */ null);
                toggleStar(item, isStarred);
            }
        } else if (id == R.id.menu_edit) {
            if (DirectoryContactUtil.isDirectoryContact(mContactData)) {
                Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                        ActionType.ADD, /* thirdPartyAction */ null);

                // This action is used to launch the contact selector, with the option of
                // creating a new contact. Creating a new contact is an INSERT, while selecting
                // an exisiting one is an edit. The fields in the edit screen will be
                // prepopulated with data.

                final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                intent.setType(Contacts.CONTENT_ITEM_TYPE);

                ArrayList<ContentValues> values = mContactData.getContentValues();

                // Only pre-fill the name field if the provided display name is an nickname
                // or better (e.g. structured name, nickname)
                if (mContactData.getDisplayNameSource() >= DisplayNameSources.NICKNAME) {
                    intent.putExtra(Intents.Insert.NAME, mContactData.getDisplayName());
                } else if (mContactData.getDisplayNameSource()
                        == DisplayNameSources.ORGANIZATION) {
                    // This is probably an organization. Instead of copying the organization
                    // name into a name entry, copy it into the organization entry. This
                    // way we will still consider the contact an organization.
                    final ContentValues organization = new ContentValues();
                    organization.put(Organization.COMPANY, mContactData.getDisplayName());
                    organization.put(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
                    values.add(organization);
                }

                // Last time used and times used are aggregated values from the usage stat
                // table. They need to be removed from data values so the SQL table can insert
                // properly
                for (ContentValues value : values) {
                    value.remove(Data.LAST_TIME_USED);
                    value.remove(Data.TIMES_USED);
                }
                intent.putExtra(Intents.Insert.DATA, values);

                // If the contact can only export to the same account, add it to the intent.
                // Otherwise the ContactEditorFragment will show a dialog for selecting
                // an account.
                if (mContactData.getDirectoryExportSupport() ==
                        Directory.EXPORT_SUPPORT_SAME_ACCOUNT_ONLY) {
                    intent.putExtra(Intents.Insert.EXTRA_ACCOUNT,
                            new Account(mContactData.getDirectoryAccountName(),
                                    mContactData.getDirectoryAccountType()));
                    intent.putExtra(Intents.Insert.EXTRA_DATA_SET,
                            mContactData.getRawContacts().get(0).getDataSet());
                }

                // Add this flag to disable the delete menu option on directory contact joins
                // with local contacts. The delete option is ambiguous when joining contacts.
                intent.putExtra(
                        ContactEditorFragment.INTENT_EXTRA_DISABLE_DELETE_MENU_OPTION,
                        true);

                intent.setPackage(getPackageName());
                startActivityForResult(intent, REQUEST_CODE_CONTACT_SELECTION_ACTIVITY);
            } else if (InvisibleContactUtil.isInvisibleAndAddable(mContactData, this)) {
                Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                        ActionType.ADD, /* thirdPartyAction */ null);
                InvisibleContactUtil.addToDefaultGroup(mContactData, this);
            } else if (isContactEditable()) {
                Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                        ActionType.EDIT, /* thirdPartyAction */ null);
                editContact();
            }
        } else if (id == R.id.menu_join) {
            return doJoinContactAction();
        } else if (id == R.id.menu_linked_contacts) {
            return showRawContactPickerDialog();
        } else if (id == R.id.menu_delete) {
            Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                    ActionType.REMOVE, /* thirdPartyAction */ null);
            if (isContactEditable()) {
                deleteContact();
            }
        } else if (id == R.id.menu_share) {
            Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                    ActionType.SHARE, /* thirdPartyAction */ null);
            if (isContactShareable()) {
                shareContact();
            }
        } else if (id == R.id.menu_create_contact_shortcut) {
            Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                    ActionType.SHORTCUT, /* thirdPartyAction */ null);
            if (isShortcutCreatable()) {
                createLauncherShortcutWithContact();
            }
        } else if (id == R.id.menu_set_ringtone) {
            doPickRingtone();
        } else if (id == R.id.menu_send_to_voicemail) {// Update state and save
            mSendToVoicemailState = !mSendToVoicemailState;
            item.setTitle(mSendToVoicemailState
                    ? R.string.menu_unredirect_calls_to_vm
                    : R.string.menu_redirect_calls_to_vm);
            final Intent intent = ContactSaveService.createSetSendToVoicemail(
                    this, mLookupUri, mSendToVoicemailState);
            this.startService(intent);
        } else if (id == R.id.menu_help) {
            Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                    ActionType.HELP, /* thirdPartyAction */ null);
            HelpUtils.launchHelpAndFeedbackForContactScreen(this);
        } else if(id == R.id.menu_copy_to_phone) {
            if (mContactData == null) return false;
            copyToPhone();
            return true;
        } else if(id == R.id.menu_copy_to_sim1) {
            if (mContactData == null) return false;
            copyToCard(SimContactsConstants.SLOT1);
            return true;
        }  else if(id == R.id.menu_copy_to_sim2) {
            if (mContactData == null) return false;
            copyToCard(SimContactsConstants.SLOT2);
            return true;
        } else {
            Logger.logQuickContactEvent(mReferrer, mContactType, CardType.UNKNOWN_CARD,
                    ActionType.UNKNOWN_ACTION, /* thirdPartyAction */ null);
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    //supply phone number and email which could stored in one ADN
    class UsimEntity {
        private ArrayList<String> mNumberList = new ArrayList<String>();
        private ArrayList<String> mEmailList = new ArrayList<String>();

        public ArrayList<String> getEmailList() {
            return mEmailList;
        }

        public ArrayList<String> getNumberList() {
            return mNumberList;
        }

        public void putEmailList(ArrayList<String> list) {
            mEmailList = list;
        }

        public void putNumberList(ArrayList<String> list) {
            mNumberList = list;
        }
    }

    private void copyToPhone() {
        String name = mContactData.getDisplayName();
        if (TextUtils.isEmpty(name)) {
            name = "";
        }
        String phoneNumber = "";
        StringBuilder anrNumber = new StringBuilder();
        StringBuilder email = new StringBuilder();

        //get phonenumber,email,anr from SIM contacts,then insert them to phone
        for (RawContact rawContact : mContactData.getRawContacts()) {
            for (DataItem dataItem : rawContact.getDataItems()) {
                if (dataItem.getMimeType() == null) {
                    continue;
                }
                if (dataItem instanceof PhoneDataItem) {
                    PhoneDataItem phoneNum = (PhoneDataItem) dataItem;
                    final String number = phoneNum.getNumber();
                    if (!TextUtils.isEmpty(number)) {
                        if (Phone.TYPE_MOBILE == phoneNum.getContentValues().getAsInteger(
                                Phone.TYPE)) {
                            phoneNumber = number;
                        } else {
                            if(!TextUtils.isEmpty(anrNumber.toString())) {
                                anrNumber.append(SimContactsConstants.ANR_SEP);
                            }
                            anrNumber.append(number);
                        }
                    }
                } else if (dataItem instanceof EmailDataItem) {
                    EmailDataItem emailData = (EmailDataItem) dataItem;
                    final String address = emailData.getData();
                    if (!TextUtils.isEmpty(address)) {
                        if(!TextUtils.isEmpty(email.toString())) {
                            email.append(SimContactsConstants.EMAIL_SEP);
                        }
                        email.append(address);
                    }
                }
            }
        }

        String[] value = new String[] {
                name, phoneNumber, email.toString(), anrNumber.toString()
        };
        boolean success = ContactUtils
                .insertToPhone(value, QuickContactActivity.this,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        Toast.makeText(this, success ? R.string.copy_done : R.string.copy_failure,
                Toast.LENGTH_SHORT).show();
    }

    private Handler mHandler = null;

    private void copyToCard(final int sub) {
        final int MSG_COPY_DONE = 0;
        final int MSG_COPY_FAILURE = 1;
        final int MSG_CARD_NO_SPACE = 2;
        final int MSG_NO_EMPTY_EMAIL = 3;
        if (mHandler == null) {
            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_COPY_DONE:
                            Toast.makeText(QuickContactActivity.this, R.string.copy_done,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case MSG_COPY_FAILURE:
                            Toast.makeText(QuickContactActivity.this, R.string.copy_failure,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case MSG_CARD_NO_SPACE:
                            Toast.makeText(QuickContactActivity.this, R.string.card_no_space,
                                    Toast.LENGTH_SHORT).show();
                            break;
                        case MSG_NO_EMPTY_EMAIL:
                            Toast.makeText(QuickContactActivity.this,
                                    R.string.no_empty_email_in_usim,
                                    Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            };
        }

        new Thread(new Runnable() {
            public void run() {
                synchronized (this) {
                    int adnCountInSimContact = 1;
                    int anrCountInSimContact = ContactUtils.getOneSimAnrCount
                                (QuickContactActivity.this, sub);
                    int emailCountInSimContact = ContactUtils.getOneSimEmailCount
                                (QuickContactActivity.this, sub);
                    int totalEmptyAdn = ContactUtils.getSimFreeCount(
                            QuickContactActivity.this, sub);
                    int totalEmptyAnr = ContactUtils.getSpareAnrCount(
                            QuickContactActivity.this, sub);
                    int totalEmptyEmail = ContactUtils.getSpareEmailCount(
                            QuickContactActivity.this, sub);

                    Message msg = Message.obtain();
                    if (totalEmptyAdn <= 0) {
                        msg.what = MSG_CARD_NO_SPACE;
                        mHandler.sendMessage(msg);
                        return;
                    }

                    //to indiacate how many number in one ADN can saved to SIM card,
                    //1 means can only save one number,2,3 ... means can save anr
                    int numEntitySize = adnCountInSimContact + anrCountInSimContact;

                    //empty number is equals to the sum of adn and anr
                    int emptyNumTotal = totalEmptyAdn + totalEmptyAnr;

                    String strName = mContactData.getDisplayName();

                    ArrayList<String> arrayNumber = new ArrayList<String>();
                    ArrayList<String> arrayEmail = new ArrayList<String>();

                    for (RawContact rawContact : mContactData.getRawContacts()) {
                        for (DataItem dataItem : rawContact.getDataItems()) {
                            if (dataItem.getMimeType() == null) {
                                continue;
                            }
                            if (dataItem instanceof PhoneDataItem) {
                                // Get phone string
                                PhoneDataItem phoneNum = (PhoneDataItem) dataItem;
                                final String number = phoneNum.getNumber();
                                if (!TextUtils.isEmpty(number) && emptyNumTotal-- > 0) {
                                    arrayNumber.add(number);
                                }
                            } else if (dataItem instanceof EmailDataItem) {
                                // Get email string
                                EmailDataItem emailData = (EmailDataItem) dataItem;
                                final String address = emailData.getData();
                                if (!TextUtils.isEmpty(address) && totalEmptyEmail-- > 0) {
                                    arrayEmail.add(address);
                                }
                            }
                        }
                    }

                    //calculate how many ADN needed according to the number and email,
                    int groupNumCount = (arrayNumber.size() % numEntitySize) != 0 ? (arrayNumber
                            .size() / numEntitySize + 1) : (arrayNumber.size() / numEntitySize);
                    int groupEmailCount = emailCountInSimContact == 0 ? 0
                            : ((arrayEmail.size() % emailCountInSimContact) != 0 ? (arrayEmail
                                    .size() / emailCountInSimContact + 1)
                                    : (arrayEmail.size() / emailCountInSimContact));

                    int groupCount = Math.max(groupEmailCount, groupNumCount);

                    ArrayList<UsimEntity> results = new ArrayList<UsimEntity>();
                    for (int i = 0; i < groupCount; i++) {
                        results.add(new UsimEntity());
                    }

                    UsimEntity value;
                    //get the phone number for each ADN from arrayNumber,put them in UsimEntity
                    for (int i = 0; i < groupNumCount; i++) {
                        value = results.get(i);
                        ArrayList<String> numberItem = new ArrayList<String>();
                        for (int j = 0; j < numEntitySize; j++) {
                            if ((i * numEntitySize + j) < arrayNumber.size()) {
                                numberItem.add(arrayNumber.get(i * numEntitySize + j));
                            }
                        }
                        value.putNumberList(numberItem);
                    }

                    for (int i = 0; i < groupEmailCount; i++) {
                        value = results.get(i);
                        ArrayList<String> emailItem = new ArrayList<String>();
                        for (int j = 0; j < emailCountInSimContact; j++) {
                            if ((i * emailCountInSimContact + j) < arrayEmail.size()) {
                                emailItem.add(arrayEmail.get(i * emailCountInSimContact + j));
                            }
                        }
                        value.putEmailList(emailItem);
                    }

                    Uri itemUri = null;
                    if (totalEmptyEmail < 0 && ContactUtils.canSaveEmail(
                            QuickContactActivity.this, sub)) {
                        Message e_msg = Message.obtain();
                        e_msg.what = MSG_NO_EMPTY_EMAIL;
                        mHandler.sendMessage(e_msg);
                    }

                    //get phone number from UsimEntity,then insert to SIM card
                    for (int i = 0; i < groupCount; i++) {
                        value = results.get(i);
                        arrayNumber = (ArrayList<String>) value.getNumberList();
                        arrayEmail = (ArrayList<String>) value.getEmailList();
                        String strNum = arrayNumber.size() > 0 ? arrayNumber.get(0) : null;
                        StringBuilder strAnrNum = new StringBuilder();
                        for (int j = 1; j < arrayNumber.size(); j++) {
                            String s = arrayNumber.get(j);
                            strAnrNum.append(s);
                            strAnrNum.append(SimContactsConstants.ANR_SEP);
                        }
                        StringBuilder strEmail = new StringBuilder();
                        for (int j = 0; j < arrayEmail.size(); j++) {
                            String s = arrayEmail.get(j);
                            strEmail.append(s);
                            strEmail.append(SimContactsConstants.EMAIL_SEP);
                        }
                        itemUri = ContactUtils.insertToCard(QuickContactActivity.this, strName,
                                strNum, strEmail.toString(), strAnrNum.toString(), sub);
                    }
                    if (itemUri != null) {
                        msg.what = MSG_COPY_DONE;
                        mHandler.sendMessage(msg);
                    } else {
                        msg.what = MSG_COPY_FAILURE;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }

    private boolean showRawContactPickerDialog() {
        if (mContactData == null) return false;
        startActivityForResult(EditorIntents.createViewLinkedContactsIntent(
                QuickContactActivity.this,
                mContactData.getLookupUri(),
                mHasComputedThemeColor
                        ? new MaterialPalette(mColorFilterColor, mStatusBarColor)
                        : null),
                REQUEST_CODE_CONTACT_EDITOR_ACTIVITY);
        return true;
    }

    private boolean doJoinContactAction() {
        if (mContactData == null) return false;

        mPreviousContactId = mContactData.getId();
        final Intent intent = new Intent(this, ContactSelectionActivity.class);
        intent.setAction(UiIntentActions.PICK_JOIN_CONTACT_ACTION);
        intent.putExtra(UiIntentActions.TARGET_CONTACT_ID_EXTRA_KEY, mPreviousContactId);
        startActivityForResult(intent, REQUEST_CODE_JOIN);
        return true;
    }

    /**
     * Performs aggregation with the contact selected by the user from suggestions or A-Z list.
     */
    private void joinAggregate(final long contactId) {
        final Intent intent = ContactSaveService.createJoinContactsIntent(
                this, mPreviousContactId, contactId, QuickContactActivity.class,
                Intent.ACTION_VIEW);
        this.startService(intent);
        showLinkProgressBar();
    }


    private void doPickRingtone() {
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        // Allow user to pick 'Default'
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        // Show only ringtones
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        // Allow the user to pick a silent ringtone
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        final Uri ringtoneUri = EditorUiUtils.getRingtoneUriFromString(mCustomRingtone,
                CURRENT_API_VERSION);

        // Put checkmark next to the current ringtone for this contact
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneUri);

        // Launch!
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_RINGTONE);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.missing_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissProgressBar() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    private void showLinkProgressBar() {
        mProgressDialog.setMessage(getString(R.string.contacts_linking_progress_bar));
        mProgressDialog.show();
    }

    private void showUnlinkProgressBar() {
        mProgressDialog.setMessage(getString(R.string.contacts_unlinking_progress_bar));
        mProgressDialog.show();
    }

    private void maybeShowProgressDialog() {
        if (ContactSaveService.getState().isActionPending(
                ContactSaveService.ACTION_SPLIT_CONTACT)) {
            showUnlinkProgressBar();
        } else if (ContactSaveService.getState().isActionPending(
                ContactSaveService.ACTION_JOIN_CONTACTS)) {
            showLinkProgressBar();
        }
    }

    private class SaveServiceListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Got broadcast from save service " + intent);
            }
            if (ContactSaveService.BROADCAST_LINK_COMPLETE.equals(intent.getAction())
                    || ContactSaveService.BROADCAST_UNLINK_COMPLETE.equals(intent.getAction())) {
                dismissProgressBar();
                if (ContactSaveService.BROADCAST_UNLINK_COMPLETE.equals(intent.getAction())) {
                    finish();
                }
            }
        }
    }
}
