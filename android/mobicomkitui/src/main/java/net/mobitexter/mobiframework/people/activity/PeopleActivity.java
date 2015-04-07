package net.mobitexter.mobiframework.people.activity;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import com.mobicomkit.client.ui.R;

import net.mobitexter.mobiframework.commons.core.utils.Utils;
import net.mobitexter.mobiframework.people.OnContactsInteractionListener;
import net.mobitexter.mobiframework.people.SearchListFragment;
import net.mobitexter.mobiframework.people.contact.ContactUtils;
import net.mobitexter.mobiframework.people.contact.device.ContactsListFragment;
import net.mobitexter.mobiframework.people.contact.mobitexter.MobiTexterContactsListFragment;
import net.mobitexter.mobiframework.people.group.Group;
import net.mobitexter.mobiframework.people.group.GroupListFragment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PeopleActivity extends ActionBarActivity implements ActionBar.TabListener, OnContactsInteractionListener,
        SearchView.OnQueryTextListener {

    public static final String SHARED_TEXT = "SHARED_TEXT";
    public static final String FORWARD_MESSAGE = "forwardMessage";
    private boolean isSearchResultView = false;

    private SearchView searchView;

    private String searchTerm;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link android.support.v4.view.ViewPager that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.activity_contacts_title);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                ((SearchListFragment) PlaceholderFragment.getFragmentMap().get(mViewPager.getCurrentItem() + 1)).onQueryTextChange(searchTerm);
                Utils.toggleSoftKeyBoard(PeopleActivity.this, true);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            ((SearchListFragment) PlaceholderFragment.getFragmentMap().get(mViewPager.getCurrentItem() + 1)).onQueryTextChange(query);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setQueryHint(getResources().getString(R.string.search_hint));
        searchView.setOnQueryTextListener(this);
        searchView.setSubmitButtonEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * This interface callback lets the main contacts list fragment notify
     * this activity that a contact has been selected.
     *
     * @param contactUri The contact Uri to the selected contact.
     */
    @Override
    public void onContactSelected(Uri contactUri) {
        Long contactId = ContactUtils.getContactId(getContentResolver(), contactUri);
        Map<String, String> phoneNumbers = ContactUtils.getPhoneNumbers(getApplicationContext(), contactId);

        if (phoneNumbers.isEmpty()) {
            Toast toast = Toast.makeText(this.getApplicationContext(), R.string.phone_number_not_present, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        Intent intent = new Intent();
        intent.putExtra("contactId", contactId);
        intent.setData(contactUri);
        finishActivity(intent);
    }

    public void startNewConversation(String contactNumber) {
        Intent intent = new Intent();
        intent.putExtra("contactNumber", contactNumber);
        finishActivity(intent);
    }

    @Override
    public void onGroupSelected(Group group) {
        Intent intent = new Intent();
        intent.putExtra("groupId", group.getGroupId());
        intent.putExtra("groupName", group.getName());
        finishActivity(intent);
    }

    private void finishActivity(Intent intent) {
        String forwardMessage = getIntent().getStringExtra(FORWARD_MESSAGE);
        if (!TextUtils.isEmpty(forwardMessage)) {
            intent.putExtra(FORWARD_MESSAGE, forwardMessage);
        }

        String sharedText = getIntent().getStringExtra(SHARED_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            intent.putExtra(SHARED_TEXT, sharedText);
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onSelectionCleared() {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
            // For platforms earlier than Android 3.0, triggers the search activity
        } else if (i == R.id.menu_search) {// if (!Utils.hasHoneycomb()) {
            onSearchRequested();
            //}

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        // Don't allow another search if this activity instance is already showing
        // search results. Only used pre-HC.
        return !isSearchResultView && super.onSearchRequested();
    }

    public SearchView getSearchView() {
        return searchView;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.searchTerm = query;
        startNewConversation(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        this.searchTerm = query;
        ((SearchListFragment) PlaceholderFragment.getFragmentMap().get(mViewPager.getCurrentItem() + 1)).onQueryTextChange(query);
        return false;
    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.mobiframework_title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.mobiframework_title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.mobiframework_title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private static Map<Integer, Fragment> fragmentMap = new HashMap<Integer, Fragment>();

        public static Map<Integer, Fragment> getFragmentMap() {
            if (fragmentMap.isEmpty()) {
                fragmentMap.put(1, new ContactsListFragment());
                fragmentMap.put(2, new MobiTexterContactsListFragment());
                fragmentMap.put(3, new GroupListFragment());
            }
            return fragmentMap;
        }


        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static Fragment newInstance(int sectionNumber) {
            //PlaceholderFragment fragment = new PlaceholderFragment();
            Fragment fragment = getFragmentMap().get(sectionNumber);
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_contact, container, false);
            return rootView;
        }
    }

}
