/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.ex.chips;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Directory;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.android.ex.chips.DropdownChipLayouter.AdapterType;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for showing a recipient list.
 */
public class BaseRecipientAdapter extends BaseAdapter implements Filterable, AccountSpecifier {
    private static final String TAG = "BaseRecipientAdapter";

    private static final boolean DEBUG = true;

    /**
     * The preferred number of results to be retrieved. This number may be
     * exceeded if there are several directories configured, because we will use
     * the same limit for all directories.
     */
    private static final int DEFAULT_PREFERRED_MAX_RESULT_COUNT = 10;

    /**
     * The number of extra entries requested to allow for duplicates. Duplicates
     * are removed from the overall result.
     */
    static final int ALLOWANCE_FOR_DUPLICATES = 5;

    // This is ContactsContract.PRIMARY_ACCOUNT_NAME. Available from ICS as hidden
    static final String PRIMARY_ACCOUNT_NAME = "name_for_primary_account";
    // This is ContactsContract.PRIMARY_ACCOUNT_TYPE. Available from ICS as hidden
    static final String PRIMARY_ACCOUNT_TYPE = "type_for_primary_account";

    /** The number of photos cached in this Adapter. */
    private static final int PHOTO_CACHE_SIZE = 200;

    /**
     * The "Waiting for more contacts" message will be displayed if search is not complete
     * within this many milliseconds.
     */
    private static final int MESSAGE_SEARCH_PENDING_DELAY = 1000;
    /** Used to prepare "Waiting for more contacts" message. */
    private static final int MESSAGE_SEARCH_PENDING = 1;

    public static final int QUERY_TYPE_EMAIL = 0;
    public static final int QUERY_TYPE_PHONE = 1;

    private final Queries.Query mQuery;
    private final int mQueryType;

    private boolean showMobileOnly = true;

    /**
     * Model object for a {@link Directory} row.
     */
    public final static class DirectorySearchParams {
        public long directoryId;
        public String directoryType;
        public String displayName;
        public String accountName;
        public String accountType;
        public CharSequence constraint;
        public DirectoryFilter filter;
    }

    private static class PhotoQuery {
        public static final String[] PROJECTION = {
            Photo.PHOTO
        };

        public static final int PHOTO = 0;
    }

    protected static class DirectoryListQuery {

        public static final Uri URI =
                Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "directories");
        public static final String[] PROJECTION = {
            Directory._ID,              // 0
            Directory.ACCOUNT_NAME,     // 1
            Directory.ACCOUNT_TYPE,     // 2
            Directory.DISPLAY_NAME,     // 3
            Directory.PACKAGE_NAME,     // 4
            Directory.TYPE_RESOURCE_ID, // 5
        };

        public static final int ID = 0;
        public static final int ACCOUNT_NAME = 1;
        public static final int ACCOUNT_TYPE = 2;
        public static final int DISPLAY_NAME = 3;
        public static final int PACKAGE_NAME = 4;
        public static final int TYPE_RESOURCE_ID = 5;
    }

    /** Used to temporarily hold results in Cursor objects. */
    protected static class TemporaryEntry {
        public final String displayName;
        public final String destination;
        public final int destinationType;
        public final String destinationLabel;
        public final long contactId;
        public final Long directoryId;
        public final long dataId;
        public final String thumbnailUriString;
        public final int displayNameSource;
        public final String lookupKey;

        public TemporaryEntry(
                String displayName,
                String destination,
                int destinationType,
                String destinationLabel,
                long contactId,
                Long directoryId,
                long dataId,
                String thumbnailUriString,
                int displayNameSource,
                String lookupKey) {
            this.displayName = displayName;
            this.destination = destination;
            this.destinationType = destinationType;
            this.destinationLabel = destinationLabel;
            this.contactId = contactId;
            this.directoryId = directoryId;
            this.dataId = dataId;
            this.thumbnailUriString = thumbnailUriString;
            this.displayNameSource = displayNameSource;
            this.lookupKey = lookupKey;
        }

        public TemporaryEntry(Cursor cursor, Long directoryId) {
            this.displayName = cursor.getString(Queries.Query.NAME);
            this.destination = cursor.getString(Queries.Query.DESTINATION);
            this.destinationType = cursor.getInt(Queries.Query.DESTINATION_TYPE);
            this.destinationLabel = cursor.getString(Queries.Query.DESTINATION_LABEL);
            this.contactId = cursor.getLong(Queries.Query.CONTACT_ID);
            this.directoryId = directoryId;
            this.dataId = cursor.getLong(Queries.Query.DATA_ID);
            this.thumbnailUriString = cursor.getString(Queries.Query.PHOTO_THUMBNAIL_URI);
            this.displayNameSource = cursor.getInt(Queries.Query.DISPLAY_NAME_SOURCE);
            this.lookupKey = cursor.getString(Queries.Query.LOOKUP_KEY);
        }
    }

    /**
     * Used to pass results from {@link DefaultFilter#performFiltering(CharSequence)} to
     * {@link DefaultFilter#publishResults(CharSequence, android.widget.Filter.FilterResults)}
     */
    private static class DefaultFilterResult {
        public final List<RecipientEntry> entries;
        public final LinkedHashMap<Long, List<RecipientEntry>> entryMap;
        public final List<RecipientEntry> nonAggregatedEntries;
        public final Set<String> existingDestinations;
        public final List<DirectorySearchParams> paramsList;

        public DefaultFilterResult(List<RecipientEntry> entries,
                LinkedHashMap<Long, List<RecipientEntry>> entryMap,
                List<RecipientEntry> nonAggregatedEntries,
                Set<String> existingDestinations,
                List<DirectorySearchParams> paramsList) {
            this.entries = entries;
            this.entryMap = entryMap;
            this.nonAggregatedEntries = nonAggregatedEntries;
            this.existingDestinations = existingDestinations;
            this.paramsList = paramsList;
        }
    }

    /**
     * An asynchronous filter used for loading two data sets: email rows from the local
     * contact provider and the list of {@link Directory}'s.
     */
    private final class DefaultFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (DEBUG) {
                Log.d(TAG, "start filtering. constraint: " + constraint + ", thread:"
                        + Thread.currentThread());
            }

            if (constraint == null) {
                return new FilterResults();
            }

            final FilterResults results = new FilterResults();
            Cursor defaultDirectoryCursor = null;
            Cursor directoryCursor = null;
            boolean limitResults = true;

            if (TextUtils.isEmpty(constraint)) {
                limitResults = false;
            }

            try {
                defaultDirectoryCursor = doQuery(constraint, limitResults ? mPreferredMaxResultCount : -1,
                        null /* directoryId */);

                if (defaultDirectoryCursor == null) {
                    if (DEBUG) {
                        Log.w(TAG, "null cursor returned for default Email filter query.");
                    }
                } else {
                    // These variables will become mEntries, mEntryMap, mNonAggregatedEntries, and
                    // mExistingDestinations. Here we shouldn't use those member variables directly
                    // since this method is run outside the UI thread.
                    final LinkedHashMap<Long, List<RecipientEntry>> entryMap =
                            new LinkedHashMap<Long, List<RecipientEntry>>();
                    final List<RecipientEntry> nonAggregatedEntries =
                            new ArrayList<RecipientEntry>();
                    final Set<String> existingDestinations = new HashSet<String>();

                    while (defaultDirectoryCursor.moveToNext()) {
                        // Note: At this point each entry doesn't contain any photo
                        // (thus getPhotoBytes() returns null).
                        putOneEntry(new TemporaryEntry(defaultDirectoryCursor,
                                null /* directoryId */),
                                true, entryMap, nonAggregatedEntries, existingDestinations);
                    }

                    // We'll copy this result to mEntry in publicResults() (run in the UX thread).
                    final List<RecipientEntry> entries = constructEntryList(
                            entryMap, nonAggregatedEntries);

                    // After having local results, check the size of results. If the results are
                    // not enough, we search remote directories, which will take longer time.
                    final int limit = mPreferredMaxResultCount - existingDestinations.size();
                    final List<DirectorySearchParams> paramsList;
                    if (limit > 0 && limitResults) {
                        if (DEBUG) {
                            Log.d(TAG, "More entries should be needed (current: "
                                    + existingDestinations.size()
                                    + ", remaining limit: " + limit + ") ");
                        }
                        directoryCursor = mContentResolver.query(
                                DirectoryListQuery.URI, DirectoryListQuery.PROJECTION,
                                null, null, null);
                        paramsList = setupOtherDirectories(mContext, directoryCursor, mAccount);
                    } else {
                        // We don't need to search other directories.
                        paramsList = null;
                    }

                    results.values = new DefaultFilterResult(
                            entries, entryMap, nonAggregatedEntries,
                            existingDestinations, paramsList);
                    results.count = 1;
                }
            } finally {
                if (defaultDirectoryCursor != null) {
                    defaultDirectoryCursor.close();
                }
                if (directoryCursor != null) {
                    directoryCursor.close();
                }
            }
            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, FilterResults results) {
            // If a user types a string very quickly and database is slow, "constraint" refers to
            // an older text which shows inconsistent results for users obsolete (b/4998713).
            // TODO: Fix it.
            mCurrentConstraint = constraint;

            clearTempEntries();

            if (results.values != null) {
                DefaultFilterResult defaultFilterResult = (DefaultFilterResult) results.values;
                mEntryMap = defaultFilterResult.entryMap;
                mNonAggregatedEntries = defaultFilterResult.nonAggregatedEntries;
                mExistingDestinations = defaultFilterResult.existingDestinations;

                // If there are no local results, in the new result set, cache off what had been
                // shown to the user for use until the first directory result is returned
                if (defaultFilterResult.entries.size() == 0 &&
                        defaultFilterResult.paramsList != null) {
                    cacheCurrentEntries();
                }

                updateEntries(defaultFilterResult.entries);

                // We need to search other remote directories, doing other Filter requests.
                if (defaultFilterResult.paramsList != null) {
                    final int limit = mPreferredMaxResultCount -
                            defaultFilterResult.existingDestinations.size();
                    startSearchOtherDirectories(constraint, defaultFilterResult.paramsList, limit);
                }
            }

        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            final RecipientEntry entry = (RecipientEntry)resultValue;
            final String displayName = entry.getDisplayName();
            final String emailAddress = entry.getDestination();
            if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, emailAddress)) {
                 return emailAddress;
            } else {
                return new Rfc822Token(displayName, emailAddress, null).toString();
            }
        }
    }

    /**
     * An asynchronous filter that performs search in a particular directory.
     */
    protected class DirectoryFilter extends Filter {
        private final DirectorySearchParams mParams;
        private int mLimit;

        public DirectoryFilter(DirectorySearchParams params) {
            mParams = params;
        }

        public synchronized void setLimit(int limit) {
            this.mLimit = limit;
        }

        public synchronized int getLimit() {
            return this.mLimit;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (DEBUG) {
                Log.d(TAG, "DirectoryFilter#performFiltering. directoryId: " + mParams.directoryId
                        + ", constraint: " + constraint + ", thread: " + Thread.currentThread());
            }
            final FilterResults results = new FilterResults();
            results.values = null;
            results.count = 0;

            if (!TextUtils.isEmpty(constraint)) {
                final ArrayList<TemporaryEntry> tempEntries = new ArrayList<TemporaryEntry>();

                Cursor cursor = null;
                try {
                    // We don't want to pass this Cursor object to UI thread (b/5017608).
                    // Assuming the result should contain fairly small results (at most ~10),
                    // We just copy everything to local structure.
                    cursor = doQuery(constraint, getLimit(), mParams.directoryId);

                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            tempEntries.add(new TemporaryEntry(cursor, mParams.directoryId));
                        }
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
                if (!tempEntries.isEmpty()) {
                    results.values = tempEntries;
                    results.count = 1;
                }
            }

            if (DEBUG) {
                Log.v(TAG, "finished loading directory \"" + mParams.displayName + "\"" +
                        " with query " + constraint);
            }

            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, FilterResults results) {
            if (DEBUG) {
                Log.d(TAG, "DirectoryFilter#publishResult. constraint: " + constraint
                        + ", mCurrentConstraint: " + mCurrentConstraint);
            }
            mDelayedMessageHandler.removeDelayedLoadMessage();
            // Check if the received result matches the current constraint
            // If not - the user must have continued typing after the request was issued, which
            // means several member variables (like mRemainingDirectoryLoad) are already
            // overwritten so shouldn't be touched here anymore.
            if (TextUtils.equals(constraint, mCurrentConstraint)) {
                if (results.count > 0) {
                    @SuppressWarnings("unchecked")
                    final ArrayList<TemporaryEntry> tempEntries =
                            (ArrayList<TemporaryEntry>) results.values;

                    for (TemporaryEntry tempEntry : tempEntries) {
                        putOneEntry(tempEntry, mParams.directoryId == Directory.DEFAULT,
                                mEntryMap, mNonAggregatedEntries, mExistingDestinations);
                    }
                }

                // If there are remaining directories, set up delayed message again.
                mRemainingDirectoryCount--;
                if (mRemainingDirectoryCount > 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Resend delayed load message. Current mRemainingDirectoryLoad: "
                                + mRemainingDirectoryCount);
                    }
                    mDelayedMessageHandler.sendDelayedLoadMessage();
                }

                // If this directory result has some items, or there are no more directories that
                // we are waiting for, clear the temp results
                if (results.count > 0 || mRemainingDirectoryCount == 0) {
                    // Clear the temp entries
                    clearTempEntries();
                }
            }

            // Show the list again without "waiting" message.
            updateEntries(constructEntryList(mEntryMap, mNonAggregatedEntries));
        }
    }

    private final Context mContext;
    private final ContentResolver mContentResolver;
    private final LayoutInflater mInflater;
    private Account mAccount;
    private final int mPreferredMaxResultCount;
    private DropdownChipLayouter mDropdownChipLayouter;

    /**
     * {@link #mEntries} is responsible for showing every result for this Adapter. To
     * construct it, we use {@link #mEntryMap}, {@link #mNonAggregatedEntries}, and
     * {@link #mExistingDestinations}.
     *
     * First, each destination (an email address or a phone number) with a valid contactId is
     * inserted into {@link #mEntryMap} and grouped by the contactId. Destinations without valid
     * contactId (possible if they aren't in local storage) are stored in
     * {@link #mNonAggregatedEntries}.
     * Duplicates are removed using {@link #mExistingDestinations}.
     *
     * After having all results from Cursor objects, all destinations in mEntryMap are copied to
     * {@link #mEntries}. If the number of destinations is not enough (i.e. less than
     * {@link #mPreferredMaxResultCount}), destinations in mNonAggregatedEntries are also used.
     *
     * These variables are only used in UI thread, thus should not be touched in
     * performFiltering() methods.
     */
    private LinkedHashMap<Long, List<RecipientEntry>> mEntryMap;
    private List<RecipientEntry> mNonAggregatedEntries;
    private Set<String> mExistingDestinations;
    /** Note: use {@link #updateEntries(List)} to update this variable. */
    private List<RecipientEntry> mEntries;
    private List<RecipientEntry> mTempEntries;

    /** The number of directories this adapter is waiting for results. */
    private int mRemainingDirectoryCount;

    /**
     * Used to ignore asynchronous queries with a different constraint, which may happen when
     * users type characters quickly.
     */
    private CharSequence mCurrentConstraint;

    private static LruCache<Uri, byte[]> mPhotoCacheMap;

    /**
     * Handler specific for maintaining "Waiting for more contacts" message, which will be shown
     * when:
     * - there are directories to be searched
     * - results from directories are slow to come
     */
    private final class DelayedMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (mRemainingDirectoryCount > 0) {
                updateEntries(constructEntryList(mEntryMap, mNonAggregatedEntries));
            }
        }

        public void sendDelayedLoadMessage() {
            sendMessageDelayed(obtainMessage(MESSAGE_SEARCH_PENDING, 0, 0, null),
                    MESSAGE_SEARCH_PENDING_DELAY);
        }

        public void removeDelayedLoadMessage() {
            removeMessages(MESSAGE_SEARCH_PENDING);
        }
    }

    private final DelayedMessageHandler mDelayedMessageHandler = new DelayedMessageHandler();

    private EntriesUpdatedObserver mEntriesUpdatedObserver;

    /**
     * Constructor for email queries.
     */
    public BaseRecipientAdapter(Context context) {
        this(context, DEFAULT_PREFERRED_MAX_RESULT_COUNT, QUERY_TYPE_EMAIL);
    }

    public BaseRecipientAdapter(Context context, int preferredMaxResultCount) {
        this(context, preferredMaxResultCount, QUERY_TYPE_EMAIL);
    }

    public BaseRecipientAdapter(int queryMode, Context context) {
        this(context, DEFAULT_PREFERRED_MAX_RESULT_COUNT, queryMode);
    }

    public BaseRecipientAdapter(int queryMode, Context context, int preferredMaxResultCount) {
        this(context, preferredMaxResultCount, queryMode);
    }

    public BaseRecipientAdapter(Context context, int preferredMaxResultCount, int queryMode) {
        mContext = context;
        mContentResolver = context.getContentResolver();
        mInflater = LayoutInflater.from(context);
        mPreferredMaxResultCount = preferredMaxResultCount;
        if (mPhotoCacheMap == null) {
            mPhotoCacheMap = new LruCache<Uri, byte[]>(PHOTO_CACHE_SIZE);
        }
        mQueryType = queryMode;

        if (queryMode == QUERY_TYPE_EMAIL) {
            mQuery = Queries.EMAIL;
        } else if (queryMode == QUERY_TYPE_PHONE) {
            mQuery = Queries.PHONE;
        } else {
            mQuery = Queries.EMAIL;
            Log.e(TAG, "Unsupported query type: " + queryMode);
        }
    }

    public Context getContext() {
        return mContext;
    }

    public int getQueryType() {
        return mQueryType;
    }

    public void setDropdownChipLayouter(DropdownChipLayouter dropdownChipLayouter) {
        mDropdownChipLayouter = dropdownChipLayouter;
        mDropdownChipLayouter.setQuery(mQuery);
    }

    public DropdownChipLayouter getDropdownChipLayouter() {
        return mDropdownChipLayouter;
    }

    /**
     * Set the account when known. Causes the search to prioritize contacts from that account.
     */
    @Override
    public void setAccount(Account account) {
        mAccount = account;
    }

    /** Will be called from {@link AutoCompleteTextView} to prepare auto-complete list. */
    @Override
    public Filter getFilter() {
        return new DefaultFilter();
    }

    /**
     * An extesion to {@link RecipientAlternatesAdapter#getMatchingRecipients} that allows
     * additional sources of contacts to be considered as matching recipients.
     * @param addresses A set of addresses to be matched
     * @return A list of matches or null if none found
     */
    public Map<String, RecipientEntry> getMatchingRecipients(Set<String> addresses) {
        return null;
    }

    public static List<DirectorySearchParams> setupOtherDirectories(Context context,
            Cursor directoryCursor, Account account) {
        final PackageManager packageManager = context.getPackageManager();
        final List<DirectorySearchParams> paramsList = new ArrayList<DirectorySearchParams>();
        DirectorySearchParams preferredDirectory = null;
        while (directoryCursor.moveToNext()) {
            final long id = directoryCursor.getLong(DirectoryListQuery.ID);

            // Skip the local invisible directory, because the default directory already includes
            // all local results.
            if (id == Directory.LOCAL_INVISIBLE) {
                continue;
            }

            final DirectorySearchParams params = new DirectorySearchParams();
            final String packageName = directoryCursor.getString(DirectoryListQuery.PACKAGE_NAME);
            final int resourceId = directoryCursor.getInt(DirectoryListQuery.TYPE_RESOURCE_ID);
            params.directoryId = id;
            params.displayName = directoryCursor.getString(DirectoryListQuery.DISPLAY_NAME);
            params.accountName = directoryCursor.getString(DirectoryListQuery.ACCOUNT_NAME);
            params.accountType = directoryCursor.getString(DirectoryListQuery.ACCOUNT_TYPE);
            if (packageName != null && resourceId != 0) {
                try {
                    final Resources resources =
                            packageManager.getResourcesForApplication(packageName);
                    params.directoryType = resources.getString(resourceId);
                    if (params.directoryType == null) {
                        Log.e(TAG, "Cannot resolve directory name: "
                                + resourceId + "@" + packageName);
                    }
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "Cannot resolve directory name: "
                            + resourceId + "@" + packageName, e);
                }
            }

            // If an account has been provided and we found a directory that
            // corresponds to that account, place that directory second, directly
            // underneath the local contacts.
            if (account != null && account.name.equals(params.accountName) &&
                    account.type.equals(params.accountType)) {
                preferredDirectory = params;
            } else {
                paramsList.add(params);
            }
        }

        if (preferredDirectory != null) {
            paramsList.add(1, preferredDirectory);
        }

        return paramsList;
    }

    /**
     * Starts search in other directories using {@link Filter}. Results will be handled in
     * {@link DirectoryFilter}.
     */
    protected void startSearchOtherDirectories(
            CharSequence constraint, List<DirectorySearchParams> paramsList, int limit) {
        final int count = paramsList.size();
        // Note: skipping the default partition (index 0), which has already been loaded
        for (int i = 1; i < count; i++) {
            final DirectorySearchParams params = paramsList.get(i);
            params.constraint = constraint;
            if (params.filter == null) {
                params.filter = new DirectoryFilter(params);
            }
            params.filter.setLimit(limit);
            params.filter.filter(constraint);
        }

        // Directory search started. We may show "waiting" message if directory results are slow
        // enough.
        mRemainingDirectoryCount = count - 1;
        mDelayedMessageHandler.sendDelayedLoadMessage();
    }

    private static void putOneEntry(TemporaryEntry entry, boolean isAggregatedEntry,
            LinkedHashMap<Long, List<RecipientEntry>> entryMap,
            List<RecipientEntry> nonAggregatedEntries,
            Set<String> existingDestinations) {
        if (existingDestinations.contains(entry.destination)) {
            return;
        }

        existingDestinations.add(entry.destination);

        if (!isAggregatedEntry) {
            nonAggregatedEntries.add(RecipientEntry.constructTopLevelEntry(
                    entry.displayName,
                    entry.displayNameSource,
                    entry.destination, entry.destinationType, entry.destinationLabel,
                    entry.contactId, entry.directoryId, entry.dataId, entry.thumbnailUriString,
                    true, entry.lookupKey));
        } else if (entryMap.containsKey(entry.contactId)) {
            // We already have a section for the person.
            final List<RecipientEntry> entryList = entryMap.get(entry.contactId);
            entryList.add(RecipientEntry.constructSecondLevelEntry(
                    entry.displayName,
                    entry.displayNameSource,
                    entry.destination, entry.destinationType, entry.destinationLabel,
                    entry.contactId, entry.directoryId, entry.dataId, entry.thumbnailUriString,
                    true, entry.lookupKey));
        } else {
            final List<RecipientEntry> entryList = new ArrayList<RecipientEntry>();
            entryList.add(RecipientEntry.constructTopLevelEntry(
                    entry.displayName,
                    entry.displayNameSource,
                    entry.destination, entry.destinationType, entry.destinationLabel,
                    entry.contactId, entry.directoryId, entry.dataId, entry.thumbnailUriString,
                    true, entry.lookupKey));
            entryMap.put(entry.contactId, entryList);
        }
    }

    /**
     * Constructs an actual list for this Adapter using {@link #mEntryMap}. Also tries to
     * fetch a cached photo for each contact entry (other than separators), or request another
     * thread to get one from directories.
     */
    private List<RecipientEntry> constructEntryList(
            LinkedHashMap<Long, List<RecipientEntry>> entryMap,
            List<RecipientEntry> nonAggregatedEntries) {
        final List<RecipientEntry> entries = new ArrayList<RecipientEntry>();
        int validEntryCount = 0;
        for (Map.Entry<Long, List<RecipientEntry>> mapEntry : entryMap.entrySet()) {
            final List<RecipientEntry> entryList = mapEntry.getValue();
            final int size = entryList.size();
            for (int i = 0; i < size; i++) {
                RecipientEntry entry = entryList.get(i);
                entries.add(entry);
                tryFetchPhoto(entry, mContentResolver, this, false, i);
                validEntryCount++;
            }
//            if (validEntryCount > mPreferredMaxResultCount) {
//                break;
//            }
        }
        if (validEntryCount <= mPreferredMaxResultCount) {
            for (int i = 0; i < nonAggregatedEntries.size(); i++) {
                RecipientEntry entry = nonAggregatedEntries.get(i);
//                if (validEntryCount > mPreferredMaxResultCount) {
//                    break;
//                }
                entries.add(entry);
                tryFetchPhoto(entry, mContentResolver, this, false, i);

                validEntryCount++;
            }
        }

        return entries;
    }


    public interface EntriesUpdatedObserver {
        public void onChanged(List<RecipientEntry> entries);
    }

    public void registerUpdateObserver(EntriesUpdatedObserver observer) {
        mEntriesUpdatedObserver = observer;
    }

    /** Resets {@link #mEntries} and notify the event to its parent ListView. */
    private void updateEntries(List<RecipientEntry> newEntries) {
        mEntries = newEntries;
        mEntriesUpdatedObserver.onChanged(newEntries);
        notifyDataSetChanged();
    }

    private void cacheCurrentEntries() {
        mTempEntries = mEntries;
    }

    private void clearTempEntries() {
        mTempEntries = null;
    }

    protected List<RecipientEntry> getEntries() {
        return mTempEntries != null ? mTempEntries : mEntries;
    }

    public static void tryFetchPhoto(final RecipientEntry entry, ContentResolver mContentResolver, BaseAdapter adapter, boolean forceLoad, int position) {
        if (forceLoad || position <= 20) {
            final Uri photoThumbnailUri = entry.getPhotoThumbnailUri();
            if (photoThumbnailUri != null) {
                final byte[] photoBytes = mPhotoCacheMap.get(photoThumbnailUri);
                if (photoBytes != null) {
                    entry.setPhotoBytes(photoBytes);
                    // notifyDataSetChanged() should be called by a caller.
                } else {
                    if (DEBUG) {
                        Log.d(TAG, "No photo cache for " + entry.getDisplayName()
                                + ". Fetch one asynchronously");
                    }
                    fetchPhotoAsync(entry, photoThumbnailUri, adapter, mContentResolver);
                }
            }
        }
    }

    // For reading photos for directory contacts, this is the chunksize for
    // copying from the inputstream to the output stream.
    private static final int BUFFER_SIZE = 1024*16;

    private static void fetchPhotoAsync(final RecipientEntry entry, final Uri photoThumbnailUri, final BaseAdapter adapter, final ContentResolver mContentResolver) {
        final AsyncTask<Void, Void, byte[]> photoLoadTask = new AsyncTask<Void, Void, byte[]>() {
            @Override
            protected byte[] doInBackground(Void... params) {
                // First try running a query. Images for local contacts are
                // loaded by sending a query to the ContactsProvider.
                final Cursor photoCursor = mContentResolver.query(
                        photoThumbnailUri, PhotoQuery.PROJECTION, null, null, null);
                if (photoCursor != null) {
                    try {
                        if (photoCursor.moveToFirst()) {
                            return photoCursor.getBlob(PhotoQuery.PHOTO);
                        }
                    } finally {
                        photoCursor.close();
                    }
                } else {
                    // If the query fails, try streaming the URI directly.
                    // For remote directory images, this URI resolves to the
                    // directory provider and the images are loaded by sending
                    // an openFile call to the provider.
                    try {
                        InputStream is = mContentResolver.openInputStream(
                                photoThumbnailUri);
                        if (is != null) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try {
                                int size;
                                while ((size = is.read(buffer)) != -1) {
                                    baos.write(buffer, 0, size);
                                }
                            } finally {
                                is.close();
                            }
                            return baos.toByteArray();
                        }
                    } catch (IOException ex) {
                        // ignore
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(final byte[] photoBytes) {
                entry.setPhotoBytes(photoBytes);
                if (photoBytes != null) {
                    mPhotoCacheMap.put(photoThumbnailUri, photoBytes);
                    if (adapter != null)
                        adapter.notifyDataSetChanged();
                }
            }
        };
        photoLoadTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    protected static void fetchPhoto(final RecipientEntry entry, final Uri photoThumbnailUri, final ContentResolver mContentResolver) {
        byte[] photoBytes = mPhotoCacheMap.get(photoThumbnailUri);
        if (photoBytes != null) {
            entry.setPhotoBytes(photoBytes);
            return;
        }
        final Cursor photoCursor = mContentResolver.query(photoThumbnailUri, PhotoQuery.PROJECTION,
                null, null, null);
        if (photoCursor != null) {
            try {
                if (photoCursor.moveToFirst()) {
                    photoBytes = photoCursor.getBlob(PhotoQuery.PHOTO);
                    entry.setPhotoBytes(photoBytes);
                    mPhotoCacheMap.put(photoThumbnailUri, photoBytes);
                }
            } finally {
                photoCursor.close();
            }
        } else {
            InputStream inputStream = null;
            ByteArrayOutputStream outputStream = null;
            try {
                inputStream = mContentResolver.openInputStream(photoThumbnailUri);
                final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    outputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    photoBytes = outputStream.toByteArray();

                    entry.setPhotoBytes(photoBytes);
                    mPhotoCacheMap.put(photoThumbnailUri, photoBytes);
                }
            } catch (final FileNotFoundException e) {
                Log.w(TAG, "Error opening InputStream for photo", e);
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing photo input stream", e);
                }
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing photo output stream", e);
                }
            }
        }
    }

    private Cursor doQuery(CharSequence constraint, int limit, Long directoryId) {
        final Uri.Builder builder = mQuery.getContentFilterUri().buildUpon();
        builder.appendPath(constraint.toString());
        builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                String.valueOf(limit + ALLOWANCE_FOR_DUPLICATES));

        if (directoryId != null) {
            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                    String.valueOf(directoryId));
        }
        if (mAccount != null) {
            builder.appendQueryParameter(PRIMARY_ACCOUNT_NAME, mAccount.name);
            builder.appendQueryParameter(PRIMARY_ACCOUNT_TYPE, mAccount.type);
        }
        String where = (showMobileOnly && mQueryType == QUERY_TYPE_PHONE) ?
                ContactsContract.CommonDataKinds.Phone.TYPE + "=" + ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE : null;
        final long start = System.currentTimeMillis();
        final Cursor cursor = mContentResolver.query(
                limit == -1 ? mQuery.getContentUri() : builder.build(), mQuery.getProjection(),
                where, null,
                limit == -1 ? ContactsContract.Contacts.DISPLAY_NAME + " ASC" : null);
        final long end = System.currentTimeMillis();
        if (DEBUG) {
            Log.d(TAG, "Time for autocomplete (query: " + constraint
                    + ", directoryId: " + directoryId + ", num_of_results: "
                    + (cursor != null ? cursor.getCount() : "null") + "): "
                    + (end - start) + " ms");
        }
        return cursor;
    }

    // TODO: This won't be used at all. We should find better way to quit the thread..
    /*public void close() {
        mEntries = null;
        mPhotoCacheMap.evictAll();
        if (!sPhotoHandlerThread.quit()) {
            Log.w(TAG, "Failed to quit photo handler thread, ignoring it.");
        }
    }*/

    @Override
    public int getCount() {
        final List<RecipientEntry> entries = getEntries();
        return entries != null ? entries.size() : 0;
    }

    @Override
    public RecipientEntry getItem(int position) {
        return getEntries().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return RecipientEntry.ENTRY_TYPE_SIZE;
    }

    @Override
    public int getItemViewType(int position) {
        return getEntries().get(position).getEntryType();
    }

    @Override
    public boolean isEnabled(int position) {
        return getEntries().get(position).isSelectable();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final RecipientEntry entry = getEntries().get(position);

        final String constraint = mCurrentConstraint == null ? null :
                mCurrentConstraint.toString();

        return mDropdownChipLayouter.bindView(convertView, parent, entry, position,
                AdapterType.BASE_RECIPIENT, constraint);
    }

    public Account getAccount() {
        return mAccount;
    }

    public boolean isShowMobileOnly() {
        return showMobileOnly;
    }

    public void setShowMobileOnly(boolean showMobileOnly) {
        this.showMobileOnly = showMobileOnly;
    }
}
