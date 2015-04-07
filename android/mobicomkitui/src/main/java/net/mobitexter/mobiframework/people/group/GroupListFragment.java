package net.mobitexter.mobiframework.people.group;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.Button;
import android.widget.QuickContactBadge;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.mobicomkit.client.ui.R;
import net.mobitexter.mobiframework.commons.core.utils.Utils;
import net.mobitexter.mobiframework.commons.image.ImageLoader;
import net.mobitexter.mobiframework.people.OnContactsInteractionListener;
import net.mobitexter.mobiframework.people.SearchListFragment;

import java.util.Locale;

/**
 * This fragment displays a list of contacts stored in the Contacts Provider. Each item in the list
 * shows the contact's thumbnail photo and display name.
 */
public class GroupListFragment extends ListFragment implements SearchListFragment,
        AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    // Defines a tag for identifying log entries
    private static final String TAG = "GroupListFragment";

    // Bundle key for saving previously selected search result item
    private static final String STATE_PREVIOUSLY_SELECTED_KEY =
            "net.mobitexter.grouplist.ui.SELECTED_ITEM";

    private GroupAdapter mAdapter; // The main query adapter
    private ImageLoader mImageLoader; // Handles loading the contact image in a background thread
    private String mSearchTerm; // Stores the current search query term

    // Contact selected listener that allows the activity holding this fragment to be notified of
    // a contact being selected
    private OnContactsInteractionListener mOnContactSelectedListener;

    // Stores the previously selected search item so that on a configuration change the same item
    // can be reselected again
    private int mPreviouslySelectedSearchItem = 0;

    // Whether or not the search query has changed since the last time the loader was refreshed
    private boolean mSearchQueryChanged;

    // Whether or not this is a search result view of this fragment, only used on pre-honeycomb
    // OS versions as search results are shown in-line via Action Bar search from honeycomb onward
    private boolean mIsSearchResultView = false;

    private Button actionButton;

    private TextView resultTextView;

    private SearchView searchView;

    /**
     * Fragments require an empty constructor.
     */
    public GroupListFragment() {
    }

    /**
     * In platform versions prior to Android 3.0, the ActionBar and SearchView are not supported,
     * and the UI gets the search string from an EditText. However, the fragment doesn't allow
     * another search when search results are already showing. This would confuse the user, because
     * the resulting search would re-query the Contacts Provider instead of searching the listed
     * results. This method sets the search query and also a boolean that tracks if this Fragment
     * should be displayed as a search result view or not.
     *
     * @param query The contacts search query.
     */
    public void setSearchQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            mIsSearchResultView = false;
        } else {
            mSearchTerm = query;
            mIsSearchResultView = true;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the main contacts adapter
        mAdapter = new GroupAdapter(getActivity());

        if (savedInstanceState != null) {
            // If we're restoring state after this fragment was recreated then
            // retrieve previous search term and previously selected search
            // result.
            mSearchTerm = savedInstanceState.getString(SearchManager.QUERY);
            mPreviouslySelectedSearchItem =
                    savedInstanceState.getInt(STATE_PREVIOUSLY_SELECTED_KEY, 0);
        }

        /*
         * An ImageLoader object loads and resizes an image in the background and binds it to the
         * QuickContactBadge in each item layout of the ListView. ImageLoader implements memory
         * caching for each image, which substantially improves refreshes of the ListView as the
         * user scrolls through it.
         *
         * To learn more about downloading images asynchronously and caching the results, read the
         * Android training class Displaying Bitmaps Efficiently.
         *
         * http://developer.android.com/training/displaying-bitmaps/
         */
        mImageLoader = new ImageLoader(getActivity(), getListPreferredItemHeight()) {
            @Override
            protected Bitmap processBitmap(Object data) {
                // This gets called in a background thread and passed the data from

                return null;
            }
        };

        // Set a placeholder loading image for the image loader
        mImageLoader.setLoadingImage(R.drawable.ic_contact_picture_holo_light);

        // Add a cache to the image loader
        mImageLoader.addImageCache(getActivity().getSupportFragmentManager(), 0.1f);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the list fragment layout
        View view = inflater.inflate(R.layout.contact_list_fragment, container, false);
        resultTextView = (TextView) view.findViewById(R.id.result);
        actionButton = (Button) view.findViewById(R.id.actionButton);
        return view;
    }

    @SuppressLint("NewApi")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set up ListView, assign adapter and set some listeners. The adapter was previously
        // created in onCreate().
        resultTextView.setText(this.getResources().getString(R.string.zero_groups_on_phone));
        actionButton.setVisibility(View.GONE);
        /*actionButton.setText(R.string.menu_add_contact);
        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ContactUtils.startContactAddActivity(getActivity());
            }
        });*/

        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause image loader to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mImageLoader.setPauseWork(true);
                    Utils.toggleSoftKeyBoard(getActivity(), true);
                } else {
                    mImageLoader.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });

        // If there's a previously selected search item from a saved state then don't bother
        // initializing the loader as it will be restarted later when the query is populated into
        // the action bar search view (see onQueryTextChange() in onCreateOptionsMenu()).
        if (mPreviouslySelectedSearchItem == 0) {
            // Initialize the loader, and create a loader identified by ContactsQuery.QUERY_ID
            getLoaderManager().initLoader(GroupQuery.QUERY_ID, null, this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            // Assign callback listener which the holding activity must implement. This is used
            // so that when a contact item is interacted with (selected by the user) the holding
            // activity will be notified and can take further action such as populating the contact
            // detail pane (if in multi-pane layout) or starting a new activity with the contact
            // details (single pane layout).
            mOnContactSelectedListener = (OnContactsInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnContactsInteractionListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // In the case onPause() is called during a fling the image loader is
        // un-paused to let any remaining background work complete.
        mImageLoader.setPauseWork(false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        // Gets the Cursor object currently bound to the ListView
        final Cursor cursor = mAdapter.getCursor();

        // Moves to the Cursor row corresponding to the ListView item that was clicked
        cursor.moveToPosition(position);

        mOnContactSelectedListener.onGroupSelected(new Group(cursor.getLong(GroupQuery.ID), cursor.getString(GroupQuery.TITLE)));
    }

    /**
     * Called when ListView selection is cleared, for example
     * when search mode is finished and the currently selected
     * contact should no longer be selected.
     */
    @SuppressLint("NewApi")
    private void onSelectionCleared() {
        // Uses callback to notify activity this contains this fragment
        mOnContactSelectedListener.onSelectionCleared();

        // Clears currently checked item
        getListView().clearChoices();
    }

    // This method uses APIs from newer OS versions than the minimum that this app supports. This
    // annotation tells Android lint that they are properly guarded so they won't run on older OS
    // versions and can be ignored by lint.
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Locate the search item
        MenuItem searchItem = menu.findItem(R.id.menu_search);

        // In versions prior to Android 3.0, hides the search item to prevent additional
        // searches. In Android 3.0 and later, searching is done via a SearchView in the ActionBar.
        // Since the search doesn't create a new Activity to do the searching, the menu item
        // doesn't need to be turned off.
        if (mIsSearchResultView) {
            searchItem.setVisible(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!TextUtils.isEmpty(mSearchTerm)) {
            // Saves the current search string
            outState.putString(SearchManager.QUERY, mSearchTerm);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Updates
        // the search filter, and restarts the loader to do a new query
        // using the new search string.
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;

        // Don't do anything if the filter is empty
        if (mSearchTerm == null && newFilter == null) {
            return true;
        }

        // Don't do anything if the new filter is the same as the current filter
        if (mSearchTerm != null && mSearchTerm.equals(newFilter)) {
            return true;
        }

        // Updates current filter to new filter
        mSearchTerm = newFilter;

        // Restarts the loader. This triggers onCreateLoader(), which builds the
        // necessary content Uri from mSearchTerm.
        mSearchQueryChanged = true;
        getLoaderManager().restartLoader(
                GroupQuery.QUERY_ID, null, GroupListFragment.this);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // If this is the loader for finding contacts in the Contacts Provider
        // (the only one supported)
        if (id == GroupQuery.QUERY_ID) {
            Uri contentUri;

            // There are two types of searches, one which displays all contacts and
            // one which filters contacts by a search query. If mSearchTerm is set
            // then a search query has been entered and the latter should be used.

            /*if (mSearchTerm == null) {
                // Since there's no search string, use the content URI that searches the entire
                // Contacts table    Uri uri = ContactsContract.Groups.CONTENT_URI;
                contentUri = GroupQuery.CONTENT_URI;
            } else {
                // Since there's a search string, use the special content Uri that searches the
                // Contacts table. The URI consists of a base Uri and the search string.
                //contentUri = Uri.withAppendedPath(GroupQuery.FILTER_URI, Uri.encode(mSearchTerm));
                contentUri = GroupQuery.CONTENT_URI;
            }*/

            /*new CursorLoader(getActivity(), contentUri, GroupQuery.PROJECTION, ContactsContract.Groups.DELETED+"!='1' AND "+
                    ContactsContract.Groups.GROUP_VISIBLE+"!='0' ", null, null);*/

            return new CursorLoader(getActivity(), GroupQuery.CONTENT_URI, GroupQuery.PROJECTION, GroupQuery.SELECTION, null, GroupQuery.SORT_ORDER);
        }

        Log.e(TAG, "onCreateLoader - incorrect ID provided (" + id + ")");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // This swaps the new cursor into the adapter.
        if (loader.getId() == GroupQuery.QUERY_ID) {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == GroupQuery.QUERY_ID) {
            // When the loader is being reset, clear the cursor from the adapter. This allows the
            // cursor resources to be freed.
            mAdapter.swapCursor(null);
        }
    }

    /**
     * Gets the preferred height for each item in the ListView, in pixels, after accounting for
     * screen density. ImageLoader uses this value to resize thumbnail images to match the ListView
     * item height.
     *
     * @return The preferred height in pixels, based on the current theme.
     */
    private int getListPreferredItemHeight() {
        final TypedValue typedValue = new TypedValue();

        // Resolve list item preferred height theme attribute into typedValue
        getActivity().getTheme().resolveAttribute(
                android.R.attr.listPreferredItemHeight, typedValue, true);

        // Create a new DisplayMetrics object
        final DisplayMetrics metrics = new DisplayMetrics();

        // Populate the DisplayMetrics
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Return theme value based on DisplayMetrics
        return (int) typedValue.getDimension(metrics);
    }

    /**
     * This is a subclass of CursorAdapter that supports binding Cursor columns to a view layout.
     * If those items are part of search results, the search string is marked by highlighting the
     * query text. An {@link android.widget.AlphabetIndexer} is used to allow quicker navigation up and down the
     * ListView.
     */
    private class GroupAdapter extends CursorAdapter implements SectionIndexer {
        private LayoutInflater mInflater; // Stores the layout inflater
        private AlphabetIndexer mAlphabetIndexer; // Stores the AlphabetIndexer instance
        private TextAppearanceSpan highlightTextSpan; // Stores the highlight text appearance style

        /**
         * Instantiates a new Contacts Adapter.
         *
         * @param context A context that has access to the app's layout.
         */
        public GroupAdapter(Context context) {
            super(context, null, 0);

            // Stores inflater for use later
            mInflater = LayoutInflater.from(context);

            // Loads a string containing the English alphabet. To fully localize the app, provide a
            // strings.xml file in res/values-<x> directories, where <x> is a locale. In the file,
            // define a string with android:name="alphabet" and contents set to all of the
            // alphabetic characters in the language in their proper sort order, in upper case if
            // applicable.
            final String alphabet = context.getString(R.string.alphabet);

            // Instantiates a new AlphabetIndexer bound to the column used to sort contact names.
            // The cursor is left null, because it has not yet been retrieved.
            mAlphabetIndexer = new AlphabetIndexer(null, GroupQuery.SORT_KEY, alphabet);

            // Defines a span for highlighting the part of a display name that matches the search
            // string
            highlightTextSpan = new TextAppearanceSpan(getActivity(), R.style.searchTextHiglight);
        }

        /**
         * Identifies the start of the search string in the display name column of a Cursor row.
         * E.g. If displayName was "Adam" and search query (mSearchTerm) was "da" this would
         * return 1.
         *
         * @param displayName The contact display name.
         * @return The starting position of the search string in the display name, 0-based. The
         *         method returns -1 if the string is not found in the display name, or if the search
         *         string is empty or null.
         */
        private int indexOfSearchQuery(String displayName) {
            if (!TextUtils.isEmpty(mSearchTerm)) {
                return displayName.toLowerCase(Locale.getDefault()).indexOf(
                        mSearchTerm.toLowerCase(Locale.getDefault()));
            }
            return -1;
        }

        /**
         * Overrides newView() to inflate the list item views.
         */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            // Inflates the list item layout.
            final View itemLayout =
                    mInflater.inflate(R.layout.contact_list_item, viewGroup, false);

            // Creates a new ViewHolder in which to store handles to each view resource. This
            // allows bindView() to retrieve stored references instead of calling findViewById for
            // each instance of the layout.
            final ViewHolder holder = new ViewHolder();
            holder.text1 = (TextView) itemLayout.findViewById(android.R.id.text1);
            holder.text2 = (TextView) itemLayout.findViewById(android.R.id.text2);
            holder.icon = (QuickContactBadge) itemLayout.findViewById(android.R.id.icon);

            // Stores the resourceHolder instance in itemLayout. This makes resourceHolder
            // available to bindView and other methods that receive a handle to the item view.
            itemLayout.setTag(holder);

            // Returns the item layout view
            return itemLayout;
        }

        /**
         * Binds data from the Cursor to the provided view.
         */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Gets handles to individual view resources

            final ViewHolder holder = (ViewHolder) view.getTag();

            String groupId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID));
            String numbers = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SUMMARY_WITH_PHONES));
            final String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE)) + " (" + numbers + ")";

            final int startIndex = indexOfSearchQuery(displayName);

            if (startIndex == -1) {
                // If the user didn't do a search, or the search string didn't match a display
                // name, show the display name without highlighting
                holder.text1.setText(displayName);

                if (TextUtils.isEmpty(mSearchTerm)) {
                    // If the search search is empty, hide the second line of text
                    holder.text2.setVisibility(View.GONE);
                } else {
                    // Shows a second line of text that indicates the search string matched
                    // something other than the display name
                    holder.text2.setVisibility(View.VISIBLE);
                }
            } else {
                // If the search string matched the display name, applies a SpannableString to
                // highlight the search string with the displayed display name

                // Wraps the display name in the SpannableString
                final SpannableString highlightedName = new SpannableString(displayName);

                // Sets the span to start at the starting point of the match and end at "length"
                // characters beyond the starting point
                highlightedName.setSpan(highlightTextSpan, startIndex,
                        startIndex + mSearchTerm.length(), 0);

                // Binds the SpannableString to the display name View object
                holder.text1.setText(highlightedName);

                // Since the search string matched the name, this hides the secondary message
                holder.text2.setVisibility(View.GONE);
            }

            // Processes the QuickContactBadge. A QuickContactBadge first appears as a contact's
            // thumbnail image with styling that indicates it can be touched for additional
            // information. When the user clicks the image, the badge expands into a dialog box
            // containing the contact's details and icons for the built-in apps that can handle
            // each detail type.

            // Generates the contact lookup Uri
            /*
            final Uri contactUri = Uri.withAppendedPath(ContactsContract.Groups.CONTENT_URI, String.valueOf(cursor.getLong(GroupQuery.ID)));

            // Binds the contact's lookup Uri to the QuickContactBadge
            holder.icon.assignContactUri(contactUri);
            mImageLoader.loadImage(contactUri, holder.icon);*/
        }

        /**
         * Overrides swapCursor to move the new Cursor into the AlphabetIndex as well as the
         * CursorAdapter.
         */
        @Override
        public Cursor swapCursor(Cursor newCursor) {
            // Update the AlphabetIndexer with new cursor as well
            mAlphabetIndexer.setCursor(newCursor);
            return super.swapCursor(newCursor);
        }

        /**
         * An override of getCount that simplifies accessing the Cursor. If the Cursor is null,
         * getCount returns zero. As a result, no test for Cursor == null is needed.
         */
        @Override
        public int getCount() {
            if (getCursor() == null) {
                return 0;
            }
            return super.getCount();
        }

        /**
         * Defines the SectionIndexer.getSections() interface.
         */
        @Override
        public Object[] getSections() {
            return mAlphabetIndexer.getSections();
        }

        /**
         * Defines the SectionIndexer.getPositionForSection() interface.
         */
        @Override
        public int getPositionForSection(int i) {
            if (getCursor() == null) {
                return 0;
            }
            return mAlphabetIndexer.getPositionForSection(i);
        }

        /**
         * Defines the SectionIndexer.getSectionForPosition() interface.
         */
        @Override
        public int getSectionForPosition(int i) {
            if (getCursor() == null) {
                return 0;
            }
            return mAlphabetIndexer.getSectionForPosition(i);
        }

        /**
         * A class that defines fields for each resource ID in the list item layout. This allows
         * ContactsAdapter.newView() to store the IDs once, when it inflates the layout, instead of
         * calling findViewById in each iteration of bindView.
         */
        private class ViewHolder {
            TextView text1;
            TextView text2;
            QuickContactBadge icon;
        }
    }

    /**
     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
     * in the {@link android.provider.ContactsContract.Contacts} class.
     */
    public interface GroupQuery {

        // An identifier for the loader
        final static int QUERY_ID = 1;

        // A content URI for the Contacts table
        final static Uri CONTENT_URI = ContactsContract.Groups.CONTENT_SUMMARY_URI;

        // The search/filter query Uri
        final static Uri FILTER_URI = ContactsContract.Groups.CONTENT_URI;

        // The selection clause for the CursorLoader query. The search criteria defined here
        // restrict results to contacts that have a display name and are linked to visible groups.
        // Notice that the search on the string provided by the user is implemented by appending
        // the search string to CONTENT_FILTER_URI.
        @SuppressLint("InlinedApi")
        /*final static String SELECTION = ContactsContract.Groups.DELETED+"!='0' AND "+
                ContactsContract.Groups.GROUP_VISIBLE+"!='0' ";*/

        final static String SELECTION = ContactsContract.Groups.DELETED + "=0";

        // The desired sort order for the returned Cursor. In Android 3.0 and later, the primary
        // sort key allows for localization. In earlier versions. use the display name as the sort
        // key.
        @SuppressLint("InlinedApi")
        final static String SORT_ORDER = ContactsContract.Groups.TITLE;

        // The projection for the CursorLoader query. This is a list of columns that the Contacts
        // Provider should return in the Cursor.
        final String[] PROJECTION = new String[] {
                ContactsContract.Groups._ID,
                ContactsContract.Groups.TITLE,
                ContactsContract.Groups.SUMMARY_WITH_PHONES,
        };


        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int TITLE = 1;
        final static int SUMMARY_WITH_PHONES = 2;

        final static int SORT_KEY = 1;
    }
}
