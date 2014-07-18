package com.airbitz.activities;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.airbitz.AirbitzApplication;
import com.airbitz.R;
import com.airbitz.adapters.NavigationAdapter;
import com.airbitz.api.CoreAPI;
import com.airbitz.fragments.BusinessDirectoryFragment;
import com.airbitz.fragments.CategoryFragment;
import com.airbitz.fragments.LandingFragment;
import com.airbitz.fragments.NavigationBarFragment;
import com.airbitz.fragments.ReceivedSuccessFragment;
import com.airbitz.fragments.RequestFragment;
import com.airbitz.fragments.SendFragment;
import com.airbitz.fragments.SettingFragment;
import com.airbitz.fragments.TransparentFragment;
import com.airbitz.fragments.WalletsFragment;
import com.airbitz.models.Transaction;
import com.airbitz.models.FragmentSourceEnum;
import com.airbitz.models.Wallet;
import com.crashlytics.android.Crashlytics;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * The main Navigation activity holding fragments for anything controlled with
 * the custom Navigation Bar for Airbitz
 * Created by Thomas Baker on 4/22/14.
 */
public class NavigationActivity extends FragmentActivity
implements NavigationBarFragment.OnScreenSelectedListener,
        CoreAPI.OnIncomingBitcoin {

    private CoreAPI mCoreAPI;
    private boolean bdonly = false;//TODO SWITCH BETWEEN BD-ONLY and WALLET

    private boolean keyBoardUp = false;

    public enum Tabs { BD, REQUEST, SEND, WALLET, SETTING }
    private NavigationBarFragment mNavBarFragment;
    private RelativeLayout mNavBarFragmentLayout;
    private LinearLayout mCalculatorView;
    private RelativeLayout mCalculatorLayout;
    private LinearLayout mFragmentLayout;
    private ViewPager mViewPager;

    private LinearLayout mNormalNavBarLayout;

    private int mNavFragmentId;
    private Fragment[] mNavFragments = {
            new BusinessDirectoryFragment(),
            new RequestFragment(),
            new SendFragment(),
            new WalletsFragment(),
            new SettingFragment()};

    // These stacks are the five "threads" of fragments represented in mNavFragments
    private Stack<Fragment>[] mNavStacks = new Stack[mNavFragments.length];

    public static Typeface montserratBoldTypeFace;
    public static Typeface montserratRegularTypeFace;
    public static Typeface latoBlackTypeFace;
    public static Typeface latoRegularTypeFace;
    public static Typeface helveticaNeueTypeFace;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);

        mCoreAPI = CoreAPI.getApi();
        String seed = getSeedData();

        mCoreAPI.Initialize(this.getApplicationContext().getFilesDir().toString(), seed, seed.length());
        mCoreAPI.setOnIncomingBitcoinListener(this);

        // COMMENT OUT NEXT LINE FOR COMMITS
//        AirbitzApplication.Login("tb2", "Aaaaaaaa1@");
        AirbitzApplication.Login("matt104", "O@21grapes");

        setContentView(R.layout.activity_navigation);
        getWindow().setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_app));
        mNavBarFragment = (NavigationBarFragment) getFragmentManager().findFragmentById(R.id.navigationFragment);
        mNavBarFragmentLayout = (RelativeLayout) findViewById(R.id.navigationLayout);
        mFragmentLayout = (LinearLayout) findViewById(R.id.activityLayout);
        mCalculatorView = (LinearLayout) findViewById(R.id.calculator_layout);
        mCalculatorLayout = (RelativeLayout) findViewById(R.id.navigation_calculator_layout);

        setTypeFaces();

        for(int i=0; i< mNavFragments.length; i++) {
            mNavStacks[i] = new Stack<Fragment>();
            mNavStacks[i].push(mNavFragments[i]);
        }

        // for keyboard hide and show
        final View activityRootView = findViewById(R.id.activity_navigation_root);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    hideNavBar();
                    keyBoardUp = true;
                    if(mNavStacks[mNavFragmentId].get(mNavStacks[mNavFragmentId].size()-1) instanceof CategoryFragment){
                        ((CategoryFragment)mNavStacks[mNavFragmentId].get(mNavStacks[mNavFragmentId].size()-1)).hideDoneCancel();
                    }
                } else {
                    showNavBar();
                    keyBoardUp = false;
                    if(mNavStacks[mNavFragmentId].get(mNavStacks[mNavFragmentId].size()-1) instanceof CategoryFragment){
                        ((CategoryFragment)mNavStacks[mNavFragmentId].get(mNavStacks[mNavFragmentId].size()-1)).showDoneCancel();
                    }
                }
            }
        });

        // Setup top screen - the Landing - that swipes away if no login
        mViewPager = (ViewPager) findViewById(R.id.navigation_view_pager);

        List<Fragment> fragments = new ArrayList<Fragment>();
        fragments.add(new TransparentFragment());
        fragments.add(new LandingFragment());
        fragments.add(new TransparentFragment());

        NavigationAdapter pageAdapter = new NavigationAdapter(getSupportFragmentManager(), fragments);
        mViewPager.setAdapter(pageAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                // Disappear if transparent page shows
                if (position == 0 || position == 2) {
                    int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                    if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                    mViewPager.setVisibility(View.GONE);
                }
            }
        });
        mViewPager.setCurrentItem(2);

        if(bdonly){
            System.out.println("BD ONLY");
            mNavBarFragmentLayout.setVisibility(View.GONE);
            mNavBarFragment.hideNavBarFragment();
            mNavBarFragmentLayout.invalidate();
            RelativeLayout.LayoutParams lLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.MATCH_PARENT);
            mFragmentLayout.setLayoutParams(lLP);
        }
    }


    public void setLoggedIn(boolean loggedIn) {
        if(!loggedIn) {
            mViewPager.setVisibility(View.VISIBLE);
            mViewPager.setCurrentItem(1);
        } else {
            mViewPager.setCurrentItem(2);
            mViewPager.setVisibility(View.INVISIBLE);

            switchFragmentThread(mNavFragmentId);
            mCoreAPI.startExchangeRateUpdates();
        }
    }

    private void setTypeFaces() {
        montserratBoldTypeFace=Typeface.createFromAsset(getAssets(), "font/Montserrat-Bold.ttf");
        montserratRegularTypeFace=Typeface.createFromAsset(getAssets(), "font/Montserrat-Regular.ttf");
        latoBlackTypeFace=Typeface.createFromAsset(getAssets(), "font/Lato-Bla.ttf");
        latoRegularTypeFace=Typeface.createFromAsset(getAssets(), "font/Lato-Regular.ttf");
        helveticaNeueTypeFace=Typeface.createFromAsset(getAssets(), "font/HelveticaNeue.ttf");
    }

    /*
        Implements interface to receive navigation changes from the bottom nav bar
     */
    public void onNavBarSelected(int position) {
        if(AirbitzApplication.isLoggedIn()) {
            switchFragmentThread(position);
        } else {
            setLoggedIn(false);
        }
    }

    public void switchFragmentThread(int id) {
        mNavBarFragment.unselectTab(mNavFragmentId);
        mNavBarFragment.unselectTab(id); // just needed for resetting mLastTab
        mNavBarFragment.selectTab(id);
        mNavFragmentId = id;
        AirbitzApplication.setLastNavTab(id);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.activityLayout, mNavStacks[id].peek()).commit();
    }

    public void switchFragmentThread(int id, Bundle bundle) {
        if(bundle!=null)
            mNavFragments[id].setArguments(bundle);
        switchFragmentThread(id);
    }

    public void pushFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if(mNavStacks[mNavFragmentId].size()!=0){
            transaction.setCustomAnimations(R.anim.slide_in_from_right,R.anim.slide_out_left,R.anim.slide_in_from_right, R.anim.slide_out_left);
        }
        transaction.replace(R.id.activityLayout, fragment);
        mNavStacks[mNavFragmentId].push(fragment);
        transaction.commit();
    }

    public void popFragment() {
        mNavStacks[mNavFragmentId].pop();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if(mNavStacks[mNavFragmentId].size()!=0){
            transaction.setCustomAnimations(R.anim.slide_in_from_left,R.anim.slide_out_right,R.anim.slide_in_from_left,R.anim.slide_out_right);
        }
        transaction.replace(R.id.activityLayout, mNavStacks[mNavFragmentId].peek());
        transaction.commit();
    }

    public void hideNavBar() {
        mNavBarFragmentLayout.setVisibility(View.GONE);
        mNavBarFragment.hideNavBarFragment();
        mNavBarFragmentLayout.invalidate();
        RelativeLayout.LayoutParams rLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mFragmentLayout.setLayoutParams(rLP);
        mFragmentLayout.invalidate();
    }

    public void showNavBar() {
        if(!bdonly) {
            mNavBarFragmentLayout.setVisibility(View.VISIBLE);
            mNavBarFragment.showNavBarFragment();
            mNavBarFragmentLayout.invalidate();
            RelativeLayout.LayoutParams rLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            rLP.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.nav_bar_height));
            mFragmentLayout.setLayoutParams(rLP);
            mFragmentLayout.invalidate();
        }
    }

    public LinearLayout getCalculatorView() {
        return mCalculatorView;
    }

    public void hideCalculator() {
        if(mCalculatorLayout.getVisibility()==View.VISIBLE) {
            mCalculatorLayout.setVisibility(View.GONE);
            mCalculatorLayout.setEnabled(false);
            showNavBar();
        }
    }

    public void showCalculator() {
        if(mCalculatorLayout.getVisibility()!=View.VISIBLE) {
            hideNavBar();
            mCalculatorLayout.setVisibility(View.VISIBLE);
            mCalculatorLayout.setEnabled(true);
        }
    }

    @Override
    public void onBackPressed() {
        if(mNavStacks[mNavFragmentId].size() == 1) {
            super.onBackPressed();
        }else {
            popFragment();
        }
    }

    @Override
    public void onResume() {
        mNavFragmentId = AirbitzApplication.getLastNavTab();
        if(!AirbitzApplication.isLoggedIn()) {
            mNavFragmentId = Tabs.BD.ordinal();
        }
        setLoggedIn(AirbitzApplication.isLoggedIn());
        super.onResume();
    }

    public void switchToWallets(FragmentSourceEnum fragmentSourceEnum, Bundle bundle){
        if(fragmentSourceEnum == FragmentSourceEnum.REQUEST){
            while(!mNavStacks[mNavFragmentId].isEmpty()){
                mNavStacks[mNavFragmentId].pop();
            }
            Fragment fragment = new RequestFragment();
            mNavStacks[mNavFragmentId].add(fragment);
            switchFragmentThread(3);
            mNavFragmentId = 3;
            while (!mNavStacks[mNavFragmentId].isEmpty()){
                mNavStacks[mNavFragmentId].pop();
            }
            Fragment frag = new WalletsFragment();
            bundle.putString(WalletsFragment.FROM_SOURCE, "REQUEST");
            bundle.putBoolean(WalletsFragment.CREATE, true);
            frag.setArguments(bundle);
            pushFragment(frag);
        }else if(fragmentSourceEnum == FragmentSourceEnum.SEND){
            while(!mNavStacks[mNavFragmentId].isEmpty()){
                mNavStacks[mNavFragmentId].pop();
            }
            Fragment fragment = new SendFragment();
            mNavStacks[mNavFragmentId].add(fragment);
            switchFragmentThread(3);
            mNavFragmentId = 3;
            while (!mNavStacks[mNavFragmentId].isEmpty()){
                mNavStacks[mNavFragmentId].pop();
            }
            Fragment frag = new WalletsFragment();
            bundle.putString(WalletsFragment.FROM_SOURCE, "SEND");
            bundle.putBoolean(WalletsFragment.CREATE, true);
            frag.setArguments(bundle);
            pushFragment(frag);
        }
    }

    private String getSeedData()
    {
        String strSeed = new String();

        strSeed += Build.MANUFACTURER;
        strSeed += Build.DEVICE;
        strSeed += Build.SERIAL;

        long time = System.nanoTime();
        ByteBuffer bb1 = ByteBuffer.allocate(8);
        bb1.putLong(time);
        strSeed += bb1.array();

        Random r = new Random();
        ByteBuffer bb2 = ByteBuffer.allocate(4);
        bb2.putInt(r.nextInt());
        strSeed += bb2.array();

        return strSeed;
    }


    @Override
    public void onIncomingBitcoin(String walletUUID, String txId) {
        Bundle bundle = new Bundle();
        bundle.putString(WalletsFragment.FROM_SOURCE,"REQUEST");
        bundle.putString(Transaction.TXID, txId);
        bundle.putString(Wallet.WALLET_UUID, walletUUID);

        Fragment frag = new ReceivedSuccessFragment();
        frag.setArguments(bundle);
        pushFragment(frag);
    }
}
