package com.airbitz.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.airbitz.R;
import com.airbitz.activities.NavigationActivity;
import com.airbitz.adapters.BusinessSearchAdapter;
import com.airbitz.adapters.LocationAdapter;
import com.airbitz.adapters.MoreCategoryAdapter;
import com.airbitz.adapters.VenueAdapter;
import com.airbitz.api.AirbitzAPI;
import com.airbitz.models.Business;
import com.airbitz.models.BusinessSearchResult;
import com.airbitz.models.Categories;
import com.airbitz.models.Category;
import com.airbitz.models.CurrentLocationManager;
import com.airbitz.models.LocationSearchResult;
import com.airbitz.models.SearchResult;
import com.airbitz.utils.CacheUtil;
import com.airbitz.utils.Common;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tom on 4/22/14.
 */
public class BusinessDirectoryFragment extends Fragment implements
        NavigationActivity.OnBackPress,
        CurrentLocationManager.OnLocationChange {

    static String TAG = AirbitzAPI.class.getSimpleName();
    static final int MAX_VENUES = 500;
    static final int PAGE_SIZE = 20;
    static final int VENUE_LOAD_AHEAD = 3;

    public static final String LAT_KEY = "LAT_KEY";
    public static final String LON_KEY = "LON_KEY";
    public static final String PREF_NAME = "PREF_NAME";
    public static final String LOCATION_CACHE_SHARED_PREF = "LOCATION_CACHE_PREF";
    public static final String BUSINESS_CACHE_SHARED_PREF = "BUSINESS_CACHE_PREF";
    public static final String LOCATION = "LOCATION";
    public static final String BUSINESS = "BUSINESS";
    public static final String BUSINESSTYPE = "BUSINESSTYPE";

    private Categories mCategories;

    private EditText mSearchField;
    private EditText mLocationField;
    private ListView mSearchListView;
    private TextView mTitleTextView;

    private TextView mNearYouTextView;

    private ViewGroup mBusinessLayout;

    private ImageButton mBackButton;
    private ImageButton mHelpButton;

    private TextView mRestaurantButton;
    private TextView mBarButton;
    private TextView mCoffeeButton;
    private TextView mMoreButton;

    private boolean locationEnabled = false;

    private List<BusinessSearchResult> mVenuesLoaded;

    private ListView mVenueListView;
    private VenueAdapter mVenueAdapter;

    private Spinner mMoreSpinner;

    private CurrentLocationManager mLocationManager;
    private Location mLastLocation;

    private ViewGroup mViewGroupLoading;
    private TextView mNoResultView;

    private static String mLocationWords = "";
    private static String mBusinessType = "business";

    private ArrayAdapter<Business> mBusinessSearchAdapter;
    private LocationAdapter mLocationAdapter;

    private ArrayList<LocationSearchResult> mLocationList;
    private ArrayList<Business> mBusinessList;

    private String mNextUrl = "null";

    private MoreCategoryAdapter mMoreCategoryAdapter;

    private BusinessCategoryAsyncTask mBusinessCategoryAsynctask;
    private BusinessAutoCompleteAsynctask mBusinessAutoCompleteAsyncTask;
    private LocationAutoCompleteAsynctask mLocationAutoCompleteAsyncTask;

    private VenuesTask mVenuesTask;

    private boolean mFirstLoad = true;
    private boolean mLoadFlag = false;
    private boolean isFirstLoad = true;

    public static Typeface montserratBoldTypeFace;
    public static Typeface montserratRegularTypeFace;
    public static Typeface latoBlackTypeFace;
    public static Typeface latoRegularTypeFace;
    public static Typeface helveticaNeueTypeFace;

    private ProgressDialog mMoreCategoriesProgressDialog;

    protected static int CATEGORY_TIMEOUT = 15000;
    Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mBusinessList == null) {
            mBusinessList = new ArrayList<Business>();
        }
        if (mLocationList == null) {
            mLocationList = new ArrayList<LocationSearchResult>();
        }
        if (mVenuesLoaded == null) {
            mVenuesLoaded = new ArrayList<BusinessSearchResult>();
        }
        if (mLocationManager == null) {
            mLocationManager = CurrentLocationManager.getLocationManager(getActivity());
        }
    }

    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_business_directory, container, false);

        checkLocationManager();

        if (Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        montserratBoldTypeFace = Typeface.createFromAsset(getActivity().getAssets(), "font/Montserrat-Bold.ttf");
        montserratRegularTypeFace = Typeface.createFromAsset(getActivity().getAssets(), "font/Montserrat-Regular.ttf");
        latoBlackTypeFace = Typeface.createFromAsset(getActivity().getAssets(), "font/Lato-Bla.ttf");
        latoRegularTypeFace = Typeface.createFromAsset(getActivity().getAssets(), "font/Lato-RegIta.ttf");
        helveticaNeueTypeFace = Typeface.createFromAsset(getActivity().getAssets(), "font/HelveticaNeue.ttf");

        mVenueListView = (ListView) view.findViewById(R.id.fragment_layout);
        // Add a header
        mBusinessLayout = (ViewGroup) inflater.inflate(R.layout.inc_directory_categories, null, false);
        mVenueListView.addHeaderView(mBusinessLayout);

        // Add a footer
        mViewGroupLoading = (ViewGroup) inflater.inflate(R.layout.loading_indicator, null, false);
        mVenueListView.addFooterView(mViewGroupLoading);

        // Setup venues adapter and listview
        mVenueAdapter = new VenueAdapter(getActivity(), mVenuesLoaded);
        mVenueListView.setAdapter(mVenueAdapter);
        mVenueListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                int newIdx = i - 1;
                showDirectoryDetailFragment(mVenuesLoaded.get(newIdx).getId(),
                                            mVenuesLoaded.get(newIdx).getName(),
                                            mVenuesLoaded.get(newIdx).getDistance());
            }
        });
        mVenueListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (!mNextUrl.equalsIgnoreCase("null")) {
                    if (firstVisibleItem + visibleItemCount + VENUE_LOAD_AHEAD >= totalItemCount && totalItemCount != 0) {
                        if (mVenuesTask == null && mVenuesLoaded.size() <= MAX_VENUES) {
                            mVenuesTask = new VenuesTask(getActivity(), null);
                            mVenuesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mNextUrl);
                        }
                    }
                }
            }
        });

        mRestaurantButton = (TextView) mBusinessLayout.findViewById(R.id.button_restaurant);
        mBarButton = (TextView) mBusinessLayout.findViewById(R.id.button_bar);
        mCoffeeButton = (TextView) mBusinessLayout.findViewById(R.id.button_coffee_tea);
        mMoreButton = (TextView) mBusinessLayout.findViewById(R.id.button_more);
        mMoreButton.setClickable(false);
        mMoreSpinner = (Spinner) mBusinessLayout.findViewById(R.id.spinner_more_categories);

        mNoResultView = (TextView) view.findViewById(R.id.business_fragment_no_result_view);

        mBackButton = (ImageButton) view.findViewById(R.id.fragment_category_button_back);
        mHelpButton = (ImageButton) view.findViewById(R.id.fragment_category_button_help);
        mHelpButton.setVisibility(View.VISIBLE);
        mSearchField = (EditText) view.findViewById(R.id.edittext_search);
        mLocationField = (EditText) view.findViewById(R.id.edittext_location);
        mSearchListView = (ListView) view.findViewById(R.id.listview_search);
        mTitleTextView = (TextView) view.findViewById(R.id.fragment_category_textview_title);

        mNearYouTextView = (TextView) mBusinessLayout.findViewById(R.id.textview_nearyou);

        mTitleTextView.setTypeface(montserratBoldTypeFace);
        mSearchField.setTypeface(montserratRegularTypeFace);
        mLocationField.setTypeface(montserratRegularTypeFace);

        mRestaurantButton.setTypeface(montserratRegularTypeFace);
        mBarButton.setTypeface(montserratRegularTypeFace);
        mCoffeeButton.setTypeface(montserratRegularTypeFace);
        mMoreButton.setTypeface(montserratRegularTypeFace);
        mNearYouTextView.setTypeface(montserratRegularTypeFace);

        mMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMoreCategoryAdapter != null) {
                    mMoreSpinner.setVisibility(View.INVISIBLE);
                    mMoreSpinner.performClick();
                } else {
                    mMoreCategoriesProgressDialog = new ProgressDialog(getActivity());
                    mMoreCategoriesProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mMoreCategoriesProgressDialog.setMessage("Retrieving data...");
                    mMoreCategoriesProgressDialog.setIndeterminate(true);
                    mMoreCategoriesProgressDialog.setCancelable(false);
                    mMoreCategoriesProgressDialog.show();
                }
            }
        });

        mRestaurantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(BUSINESS, ((TextView) view).getText().toString());
                bundle.putString(LOCATION, "");
                bundle.putString(BUSINESSTYPE, "category");
                Fragment fragment = new MapBusinessDirectoryFragment();
                fragment.setArguments(bundle);
                ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());
            }
        });

        mBarButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(BUSINESS, ((TextView) view).getText().toString());
                bundle.putString(LOCATION, "");
                bundle.putString(BUSINESSTYPE, "category");
                Fragment fragment = new MapBusinessDirectoryFragment();
                fragment.setArguments(bundle);
                ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());
            }
        });

        mCoffeeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putString(BUSINESS, ((TextView) view).getText().toString());
                bundle.putString(LOCATION, "");
                bundle.putString(BUSINESSTYPE, "category");
                Fragment fragment = new MapBusinessDirectoryFragment();
                fragment.setArguments(bundle);
                ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onBackPressed();
            }
        });

        mHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                ((NavigationActivity)getActivity()).pushFragment(new HelpFragment(HelpFragment.INFO), NavigationActivity.Tabs.BD.ordinal());
            }
        });

        mBusinessSearchAdapter = new BusinessSearchAdapter(getActivity(), mBusinessList);
        mSearchListView.setAdapter(mBusinessSearchAdapter);

        mSearchField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    mSearchListView.setAdapter(mBusinessSearchAdapter);
                    mLocationField.setVisibility(View.VISIBLE);
                    mSearchListView.setVisibility(View.VISIBLE);
                    mBackButton.setVisibility(View.VISIBLE);
                    mVenueListView.setVisibility(View.GONE);

                    // Start search
                    try {
                        final String text = mSearchField.getText().toString();
                        final List<Business> cachedBusiness = (!TextUtils.isEmpty(text)
                                ? null
                                : CacheUtil.getCachedBusinessSearchData(getActivity()));

                        if (mBusinessAutoCompleteAsyncTask != null && mBusinessAutoCompleteAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                            mBusinessAutoCompleteAsyncTask.cancel(true);
                        }
                        mBusinessAutoCompleteAsyncTask = new BusinessAutoCompleteAsynctask(cachedBusiness);
                        String latLong = "";
                        if(locationEnabled) {
                            Location currentLoc = mLocationManager.getLocation();
                            latLong = String.valueOf(currentLoc.getLatitude());
                            latLong += "," + String.valueOf(currentLoc.getLongitude());
                        }
                        mBusinessAutoCompleteAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, text, mLocationWords, latLong);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mSearchListView.getVisibility() == View.GONE) {
                    return;
                }

                mSearchListView.setAdapter(mBusinessSearchAdapter);
                mLocationField.setVisibility(View.VISIBLE);
                mSearchListView.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.VISIBLE);
                mVenueListView.setVisibility(View.GONE);

                try {
                    String latLong = "";
                    if(locationEnabled) {
                        Location currentLoc = mLocationManager.getLocation();
                        latLong = String.valueOf(currentLoc.getLatitude());
                        latLong += "," + String.valueOf(currentLoc.getLongitude());
                    }
                    // Only include cached searches if text is empty.
                    final String query = editable.toString();
                    List<Business> cachedBusinesses = (TextUtils.isEmpty(query)
                            ? CacheUtil.getCachedBusinessSearchData(getActivity())
                            : null);
                    if(mBusinessAutoCompleteAsyncTask != null && mBusinessAutoCompleteAsyncTask.getStatus()== AsyncTask.Status.RUNNING){
                        mBusinessAutoCompleteAsyncTask.cancel(true);
                    }
                    mBusinessAutoCompleteAsyncTask = new BusinessAutoCompleteAsynctask(cachedBusinesses);
                    mBusinessAutoCompleteAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query,mLocationWords,latLong);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        mLocationAdapter = new LocationAdapter(getActivity(), mLocationList);
        mSearchListView.setAdapter(mLocationAdapter);
        mLocationField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    mSearchListView.setAdapter(mLocationAdapter);
                    mSearchListView.setVisibility(View.VISIBLE);
                    mVenueListView.setVisibility(View.GONE);
                    mBackButton.setVisibility(View.VISIBLE);

                    // Search

                    try {
                        String latLong = "";
                        if(locationEnabled) {
                            Location currentLoc = mLocationManager.getLocation();
                            latLong = String.valueOf(currentLoc.getLatitude());
                            latLong += "," + String.valueOf(currentLoc.getLongitude());
                        }
                        mLocationWords = "";
                        if(mLocationAutoCompleteAsyncTask != null && mLocationAutoCompleteAsyncTask.getStatus()== AsyncTask.Status.RUNNING){
                            mLocationAutoCompleteAsyncTask.cancel(true);
                        }
                        mLocationAutoCompleteAsyncTask = new LocationAutoCompleteAsynctask(CacheUtil.getCachedLocationSearchData(getActivity()));
                        mLocationAutoCompleteAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mLocationWords,latLong);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    mViewGroupLoading.setVisibility(View.VISIBLE);
                    mVenueListView.setVisibility(View.VISIBLE);
                }
            }
        });
        mSearchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(textView);

                    Bundle bundle = new Bundle();
                    bundle.putString(BUSINESS, mSearchField.getText().toString());
                    bundle.putString(LOCATION, mLocationField.getText().toString());
                    bundle.putString(BUSINESSTYPE, mBusinessType);
                    Fragment fragment = new MapBusinessDirectoryFragment();
                    fragment.setArguments(bundle);
                    ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());

                    if (mVenueListView.getVisibility() == View.GONE) {
                        hideSearch();
                    }
                    return true;
                }
                return false;
            }
        });
        mLocationField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    hideKeyboard(textView);

                    Bundle bundle = new Bundle();
                    bundle.putString(BUSINESS, mSearchField.getText().toString());
                    bundle.putString(LOCATION, mLocationField.getText().toString());
                    bundle.putString(BUSINESSTYPE, mBusinessType);
                    Fragment fragment = new MapBusinessDirectoryFragment();
                    fragment.setArguments(bundle);
                    ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());

                    if (mVenueListView.getVisibility() == View.GONE) {
                        hideSearch();
                    }
                    return true;
                }
                // Never report that the event was handled so the keyboard is handled by OS
                return false;
            }
        });

        mLocationField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
             public void afterTextChanged(Editable editable) {
                if (mSearchListView.getVisibility() == View.GONE) {
                    return;
                }

                // if (editable.toString().length() > 0) {
                mSearchListView.setAdapter(mLocationAdapter);
                mSearchListView.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.VISIBLE);
                mVenueListView.setVisibility(View.GONE);

                try {
                    String latLong = "";
                    if(locationEnabled) {
                        Location currentLoc = mLocationManager.getLocation();
                        latLong = String.valueOf(currentLoc.getLatitude());
                        latLong += "," + String.valueOf(currentLoc.getLongitude());
                    }
                    mLocationWords = editable.toString();

                    List<LocationSearchResult> cachedLocationSearch = (TextUtils.isEmpty(mLocationWords)
                            ? CacheUtil.getCachedLocationSearchData(getActivity())
                            : null);
                    if(mLocationAutoCompleteAsyncTask != null && mLocationAutoCompleteAsyncTask.getStatus()== AsyncTask.Status.RUNNING){
                        mLocationAutoCompleteAsyncTask.cancel(true);
                    }
                    mLocationAutoCompleteAsyncTask = new LocationAutoCompleteAsynctask(cachedLocationSearch);
                    mLocationAutoCompleteAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mLocationWords, latLong);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mSearchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView,
                                    View view, int position, long l) {
                boolean locationFieldShouldFocus = false;

                if (mSearchField.isFocused()) {
                    final BusinessSearchAdapter businessSearchAdapter = (BusinessSearchAdapter) mSearchListView.getAdapter();
                    final Business business = businessSearchAdapter.getItem(position);

                    mSearchField.setText(business.getName());
                    mBusinessType = business.getType();

                    if ("business".equalsIgnoreCase(mBusinessType)) {
                        hideKeyboard(mSearchField);

                        Bundle bundle = new Bundle();
                        bundle.putString(DirectoryDetailFragment.BIZID, business.getId());
                        bundle.putString(DirectoryDetailFragment.BIZNAME, business.getName());
                        Fragment fragment = new DirectoryDetailFragment();
                        fragment.setArguments(bundle);
                        ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());
                    } else {
                        CacheUtil.writeCachedBusinessSearchData(getActivity(),
                                businessSearchAdapter.getItem(position));
                        locationFieldShouldFocus = true;
                    }

                } else if (mLocationField.isFocused()) {
                    final LocationAdapter locationAdapter = (LocationAdapter) mSearchListView.getAdapter();
                    final LocationSearchResult location = locationAdapter.getItem(position);
                    mLocationField.setText(location.getLocationName());
                    CacheUtil.writeCachedLocationSearchData(getActivity(),
                            location.getLocationName());
                }
                if (locationFieldShouldFocus) {
                    mLocationField.requestFocus();
                } else {
                    mSearchField.requestFocus();
                }
            }
        });

        if (mCategories != null) {
            updateMoreSpinner(mCategories);
        }
        if (mMoreSpinner != null) {
            mMoreSpinner.setVisibility(View.INVISIBLE);
        }
        mBackButton.setVisibility(View.GONE);
        showLoadingIndicator();
        mNoResultView.setVisibility(View.GONE);

        return view;
    }

    Runnable mProgressTimeout = new Runnable() {
        @Override
        public void run() {
            if (mMoreCategoriesProgressDialog!=null && mMoreCategoriesProgressDialog.isShowing()) {
                mMoreCategoriesProgressDialog.dismiss();
            }
            mMoreCategoriesProgressDialog=null;
        }
    };

    @Override
    public void OnCurrentLocationChange(Location location) {
        String latLon = "";
        if (location != null)
            latLon = "" + location.getLatitude() + "," + location.getLongitude();

        if (mVenuesTask != null && mVenuesTask.getStatus() == AsyncTask.Status.RUNNING) {
            mVenuesTask.cancel(true);
        }
        mVenuesTask = new VenuesTask(getActivity(), latLon);
        mVenuesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        mLastLocation = new Location(location);
        mLocationManager.removeLocationChangeListener(this);
    }

    @Override
    public boolean onBackPress() {
        return onBackPressed();
    }

    public boolean onBackPressed() {
        mLocationWords = "";
        if (mVenueListView.getVisibility() == View.GONE) {
            hideSearch();
            return true;
        }
        return false;
    }

    private void hideSearch() {
        mLocationField.setVisibility(View.GONE);
        mSearchListView.setVisibility(View.GONE);
        mBackButton.setVisibility(View.GONE);
        mVenueListView.setVisibility(View.VISIBLE);

        mSearchField.clearFocus();
        mLocationField.clearFocus();
        hideKeyboard(mSearchField);
    }

    private void hideKeyboard(View view) {
        // hide virtual keyboard
        InputMethodManager imm =
            (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showSearch() {
        mLocationField.setVisibility(View.VISIBLE);
        mSearchListView.setVisibility(View.VISIBLE);
        mBackButton.setVisibility(View.VISIBLE);
        mVenueListView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        if (mMoreSpinner != null) {
            mMoreSpinner.setVisibility(View.GONE);
        }
        checkLocationManager();
        if (mLastLocation != null &&
                (System.currentTimeMillis() - mLastLocation.getTime()) > 30 * 60 * 1000) {
            mVenuesLoaded = new ArrayList<BusinessSearchResult>();
            mLastLocation = null;
        }
        Common.LogD(TAG, "mVenuesLoaded: " + mVenuesLoaded.size());
        if (mVenuesLoaded.isEmpty()) {
            // if no venues, then request location
            mLocationManager.addLocationChangeListener(this);
        } else {
            // copy the list
            List <BusinessSearchResult> venues =
                new ArrayList<BusinessSearchResult>(mVenuesLoaded);
            mVenuesLoaded.clear();
            setVenueListView(venues);
        }
        // If we don't have categories, fetch them
        if (mCategories == null || mCategories.getCountValue() == 0) {
            try {
                mBusinessCategoryAsynctask = new BusinessCategoryAsyncTask();
                mBusinessCategoryAsynctask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "level");
                mHandler.postDelayed(mProgressTimeout, CATEGORY_TIMEOUT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        if (mMoreSpinner != null) {
            mMoreSpinner.setVisibility(View.GONE);
        }
        super.onStop();
    }

    @Override
    public void onPause() {
        if (mMoreSpinner != null) {
            mMoreSpinner.setVisibility(View.GONE);
        }
        mLocationManager.removeLocationChangeListener(this);

        if(mBusinessAutoCompleteAsyncTask != null){
            mBusinessAutoCompleteAsyncTask.cancel(true);
        }
        if(mLocationAutoCompleteAsyncTask != null){
            mLocationAutoCompleteAsyncTask.cancel(true);
        }
        if(mVenuesTask != null && mVenuesTask.getStatus() == AsyncTask.Status.RUNNING){
            mVenuesTask.cancel(true);
        }
        mFirstLoad = true;

        super.onPause();
    }

    private void updateMoreSpinner(Categories categories) {
        if (categories != null) {
            ArrayList<Category> catArrayList = new ArrayList<Category>();

            for (Category cat : categories.getBusinessCategoryArray()) {
                if (!cat.getCategoryLevel().equalsIgnoreCase("1")
                        && !cat.getCategoryLevel().equalsIgnoreCase("2")
                        && !cat.getCategoryLevel().equalsIgnoreCase("3")
                        && !cat.getCategoryLevel().equalsIgnoreCase("null")) {
                    catArrayList.add(cat);
                }
            }
            categories.removeBusinessCategoryArray();
            categories.setBusinessCategoryArray(catArrayList);
            mCategories = categories;

            mMoreCategoryAdapter = new MoreCategoryAdapter(getActivity(), mCategories);
            mMoreSpinner.setAdapter(mMoreCategoryAdapter);
            mMoreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView,
                                            View view, int position, long l) {

                    if (mFirstLoad) {
                        mFirstLoad = false;
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString(BUSINESS, mMoreCategoryAdapter.getListItemName(position)
                                .getCategoryName());
                        bundle.putString(LOCATION, "");
                        bundle.putString(BUSINESSTYPE, "category");
                        Fragment fragment = new MapBusinessDirectoryFragment();
                        fragment.setArguments(bundle);
                        ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            mMoreButton.setClickable(true);

            // If we are displaying a dialog, open up the spinner
            if (mMoreCategoriesProgressDialog!=null && mMoreCategoriesProgressDialog.isShowing()) {
                if (categories == null) {
                    Toast.makeText(getActivity().getApplicationContext(), "Can not retrieve data",
                            Toast.LENGTH_LONG).show();
                }
                mMoreCategoriesProgressDialog.dismiss();
                mMoreCategoriesProgressDialog = null;

                mMoreSpinner.setVisibility(View.INVISIBLE);
                mMoreSpinner.performClick();
            }

            // Change the more button to open up categories
            mMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mMoreSpinner.setVisibility(View.INVISIBLE);
                    mMoreSpinner.performClick();
                }
            });
        } else {
            mMoreButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(getActivity().getApplicationContext(), "No categories retrieved from server",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void hideLoadingIndicator() {
        if (mViewGroupLoading != null) {
            mViewGroupLoading.setVisibility(View.GONE);
        }
    }

    public void showLoadingIndicator() {
        if(mViewGroupLoading != null) {
            mViewGroupLoading.setVisibility(View.VISIBLE);
        }
    }

    public Categories getMoreBusinessCategory(Categories initial, String link) {
        while (!link.equalsIgnoreCase("null")) {

            String jSOnString = AirbitzAPI.getApi().getRequest(link);
            Categories jsonParsingResult = null;
            try {
                jsonParsingResult = new Categories(new JSONObject(jSOnString));
                link = jsonParsingResult.getNextLink();
                initial.addCategories(jsonParsingResult);
            } catch (Exception e) {
                link = "null";
            }
        }

        return initial;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mBusinessCategoryAsynctask!=null) {
            mBusinessCategoryAsynctask.cancel(true);
        }
    }

    private void checkLocationManager() {
        LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationEnabled = false;
            Toast.makeText(getActivity(), getString(R.string.fragment_business_enable_location_services), Toast.LENGTH_SHORT).show();
        }else{
            locationEnabled = true;
        }
    }

    private void showDirectoryDetailFragment(String id, String name, String distance) {
        Bundle bundle = new Bundle();
        bundle.putString(DirectoryDetailFragment.BIZID, id);
        bundle.putString("", name);
        bundle.putString("", distance);
        Fragment fragment = new DirectoryDetailFragment();
        fragment.setArguments(bundle);
        ((NavigationActivity) getActivity()).pushFragment(fragment, NavigationActivity.Tabs.BD.ordinal());
    }

    public void setVenueListView(List<BusinessSearchResult> venues) {
        if (venues != null) {
            mVenueListView.setVisibility(View.VISIBLE);
            mNoResultView.setVisibility(View.GONE);
        } else {
            mVenueListView.setVisibility(View.GONE);
            mNoResultView.setVisibility(View.VISIBLE);
        }
        if (!venues.isEmpty()) {
            mVenuesLoaded.addAll(venues);
            if (venues.size() <= PAGE_SIZE) {
                mVenueAdapter.warmupCache(venues);
            }
            mVenueAdapter.notifyDataSetChanged();
        }
    }

    class BusinessCategoryAsyncTask extends AsyncTask<String, Integer, Categories> {
        private AirbitzAPI api = AirbitzAPI.getApi();

        @Override
        protected Categories doInBackground(String...strings) {
            Categories jsonParsingResult = null;
            try {
                jsonParsingResult = api.getHttpCategories(strings[0]);
                String nextUrl = jsonParsingResult.getNextLink();
                mCategories = jsonParsingResult;
                getMoreBusinessCategory(mCategories, nextUrl);
            } catch (Exception e) {

            }
            return jsonParsingResult;
        }

        @Override
        protected void onPostExecute(Categories categories) {
            if (getActivity()==null) {
                return;
            }
            updateMoreSpinner(categories);
        }
    }

    class BusinessAutoCompleteAsynctask extends AsyncTask<String, Integer, List<Business>> {

        private AirbitzAPI api = AirbitzAPI.getApi();
        private List<Business> mCacheData = null;

        public BusinessAutoCompleteAsynctask(List<Business> cacheData) {
            mCacheData = cacheData;
        }

        @Override
        protected List<Business> doInBackground(String... strings) {
            List<Business> jsonParsingResult = api.getHttpAutoCompleteBusiness(strings[0],
                    strings[1],
                    strings[2]);
            return jsonParsingResult;
        }

        @Override
        protected void onPostExecute(List<Business> businesses) {
            if (getActivity() == null) {
                return;
            }

            mBusinessList.clear();
            if (businesses == null) {
                mBusinessList.add(new Business("No Results Found", "", ""));
            } else {

                // Add all businesses first
                mBusinessList.addAll(businesses);

                // Add cached businesses
                if (mCacheData != null) {
                    for (Business business : mCacheData) {
                        if (!mBusinessList.contains(business)) {
                            mBusinessList.add(0, business);
                        }
                    }
                }
            }
            mBusinessSearchAdapter.notifyDataSetChanged();
            mBusinessAutoCompleteAsyncTask = null;
        }

        @Override protected void onCancelled(List<Business> jSONResult){
            mBusinessAutoCompleteAsyncTask = null;
            super.onCancelled();
        }
    }

    class LocationAutoCompleteAsynctask extends AsyncTask<String, Integer, List<LocationSearchResult>> {

        private List<LocationSearchResult> mCacheData = null;
        private AirbitzAPI api = AirbitzAPI.getApi();

        public LocationAutoCompleteAsynctask(List<LocationSearchResult> cacheData) {
            mCacheData = cacheData;
        }

        @Override
        protected List<LocationSearchResult> doInBackground(String... strings) {
            return api.getHttpAutoCompleteLocation(strings[0], strings[1]);
        }

        @Override
        protected void onPostExecute(List<LocationSearchResult> result) {
            if (getActivity() == null) {
                return;
            }

            mLocationList.clear();

            // Add current location and on the web
            mLocationList.add(new LocationSearchResult(getString(R.string.current_location), false));
            mLocationList.add(new LocationSearchResult(getString(R.string.on_the_web), false));

            if (result == null) {
                mLocationList.add(new LocationSearchResult("No Results Found", false));
            } else {

                // Add cached location searches
                if (mCacheData != null) {
                    for (LocationSearchResult location : mCacheData) {
                        if (!mLocationList.contains(location)) {
                            mLocationList.add(0, location);
                        }
                    }
                }

                // Add all location results
                for (LocationSearchResult l : result) {
                    if (!mLocationList.contains(l)) {
                        mLocationList.add(l);
                    }
                }
            }
            mLocationAdapter.notifyDataSetChanged();
            mLocationAutoCompleteAsyncTask = null;
        }

        @Override
        protected void onCancelled(List<LocationSearchResult> JSONResult){
            super.onCancelled();
            mLocationAutoCompleteAsyncTask = null;
        }

    }

    private class VenuesTask extends AsyncTask<String, Void, String> {

        AirbitzAPI mApi = AirbitzAPI.getApi();
        Context mContext;
        String mLatLng;

        public VenuesTask(Context context, String latlng) {
            mContext = context;
            mLatLng = latlng;
        }

        @Override
        protected String doInBackground(String... params) {
            if (mLatLng != null) {
                return mApi.getSearchByLatLong(mLatLng, String.valueOf(PAGE_SIZE), "", "1");
            } else if (params.length > 0 && !params[0].equalsIgnoreCase("null")) {
                return mApi.getRequest(params[0]);
            } else {
                return "";
            }
        }

        @Override
        protected void onCancelled() {
            mNoResultView.setVisibility(View.VISIBLE);
            hideLoadingIndicator();
            mVenuesTask = null;
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(String searchResult) {
            if (!searchResult.isEmpty()) {
                try {
                    SearchResult results = new SearchResult(new JSONObject(searchResult));
                    mNextUrl = results.getNextLink();
                    setVenueListView(results.getBusinessSearchObjectArray());
                } catch (JSONException e) {
                    e.printStackTrace();
                    this.cancel(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    this.cancel(true);
                }
            } else {
                hideLoadingIndicator();
            }
            if (mVenuesLoaded.size() >= MAX_VENUES) {
                hideLoadingIndicator();
            }
            mVenuesTask = null;
        }
    }
}