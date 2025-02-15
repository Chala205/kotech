package com.app.androidnewsapp.activity;

import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.viewpager.widget.ViewPager;

import com.app.androidnewsapp.BuildConfig;
import com.app.androidnewsapp.R;
import com.app.androidnewsapp.callback.CallbackUser;
import com.app.androidnewsapp.config.AppConfig;
import com.app.androidnewsapp.database.prefs.AdsPref;
import com.app.androidnewsapp.database.prefs.SharedPref;
import com.app.androidnewsapp.models.User;
import com.app.androidnewsapp.rest.ApiInterface;
import com.app.androidnewsapp.rest.RestAdapter;
import com.app.androidnewsapp.util.AdsManager;
import com.app.androidnewsapp.util.AppBarLayoutBehavior;
import com.app.androidnewsapp.util.Constant;
import com.app.androidnewsapp.util.RtlViewPager;
import com.app.androidnewsapp.util.Tools;
import com.app.androidnewsapp.util.ViewPagerHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.solodroid.ads.sdk.format.AppOpenAd;
import com.solodroid.push.sdk.provider.OneSignalPush;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements DefaultLifecycleObserver {

    private static final String TAG = "MainActivity";
    private long exitTime = 0;
    private BottomNavigationView navigation;
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    TextView titleToolbar;
    User user;
    Call<CallbackUser> callbackCall = null;
    ImageView imgProfile;
    ImageView imgAvatar;
    RelativeLayout btnProfile;
    ImageButton btnSearch;
    ImageView btnSettings;
    CardView lytSearchBar;
    LinearLayout searchBar;
    Toolbar toolbar;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;
    CoordinatorLayout parentView;
    ViewPagerHelper viewPagerHelper;
    private AppUpdateManager appUpdateManager;
    View lyt_dialog_exit;
    LinearLayout lyt_panel_view;
    LinearLayout lyt_panel_dialog;
    OneSignalPush.Builder onesignal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_main);
        Tools.setNavigation(this);
        Tools.setupAppMainBarLayout(this);

        adsPref = new AdsPref(this);
        sharedPref = new SharedPref(this);
        viewPagerHelper = new ViewPagerHelper(this);
        adsManager = new AdsManager(this);

        if (AppConfig.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
            adsPref.setIsOpenAd(true);
        }

        adsManager.initializeAd();
        adsManager.updateConsentStatus();
        adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnResume());
        adsManager.loadBannerAd(adsPref.getIsBannerHome());
        adsManager.loadInterstitialAd(adsPref.getIsInterstitialPostList(), adsPref.getInterstitialAdInterval());

        onesignal = new OneSignalPush.Builder(this);
        onesignal.requestNotificationPermission();

        initView();
        initExitDialog();

        navigation.getMenu().clear();

        if (AppConfig.SET_CATEGORY_AS_MAIN_PAGE) {
            if (sharedPref.getVideoMenu().equals("yes")) {
                navigation.inflateMenu(R.menu.menu_navigation_category);
            } else {
                navigation.inflateMenu(R.menu.menu_navigation_category_no_video);
            }
        } else {
            if (sharedPref.getVideoMenu().equals("yes")) {
                navigation.inflateMenu(R.menu.menu_navigation_default);
            } else {
                navigation.inflateMenu(R.menu.menu_navigation_default_no_video);
            }
        }

        navigation.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        if (sharedPref.getIsDarkTheme()) {
            navigation.setBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_bottom_navigation));
        } else {
            navigation.setBackgroundColor(ContextCompat.getColor(this, R.color.color_light_bottom_navigation));
        }

        if (AppConfig.ENABLE_RTL_MODE) {
            viewPagerHelper.setupViewPagerRTL(viewPagerRTL, navigation, titleToolbar);
        } else {
            viewPagerHelper.setupViewPager(viewPager, navigation, titleToolbar);
        }

        Tools.notificationOpenHandler(this, getIntent());

        if (AppConfig.ENABLE_NEW_APP_DESIGN) {
            setupNewToolbar();
        } else {
            setupToolbar();
        }

        displayUserProfile();

        if (!BuildConfig.DEBUG) {
            appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
            inAppUpdate();
            inAppReview();
        }

        if (!Tools.isConnect(this)) {
            if (sharedPref.getVideoMenu().equals("yes")) {
                if (AppConfig.ENABLE_RTL_MODE) {
                    viewPagerRTL.setCurrentItem(3);
                } else {
                    viewPager.setCurrentItem(3);
                }
            } else {
                if (AppConfig.ENABLE_RTL_MODE) {
                    viewPagerRTL.setCurrentItem(2);
                } else {
                    viewPager.setCurrentItem(2);
                }
            }
        }

    }

    private void initView() {
        AppBarLayout appBarLayout = findViewById(R.id.tab_appbar_layout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());
        parentView = findViewById(R.id.coordinator_layout);
        titleToolbar = findViewById(R.id.title_toolbar);
        imgProfile = findViewById(R.id.img_profile);
        imgAvatar = findViewById(R.id.img_avatar);
        btnProfile = findViewById(R.id.btn_profile);
        btnSettings = findViewById(R.id.btn_settings);
        lytSearchBar = findViewById(R.id.lyt_search_bar);
        searchBar = findViewById(R.id.search_bar);
        btnSearch = findViewById(R.id.btn_search);
        toolbar = findViewById(R.id.toolbar);
        navigation = findViewById(R.id.navigation);
        viewPager = findViewById(R.id.viewpager);
        viewPagerRTL = findViewById(R.id.viewpager_rtl);

    }

    public void setupToolbar() {
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.color_dark_toolbar));
            navigation.setBackgroundColor(getResources().getColor(R.color.color_dark_bottom_navigation));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.color_light_primary));
        }

        btnSearch.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

        btnProfile.setOnClickListener(view -> openSettings());
        btnSettings.setOnClickListener(view -> openSettings());

        if (sharedPref.getLoginFeature().equals("yes")) {
            btnProfile.setVisibility(View.VISIBLE);
            btnSettings.setVisibility(View.GONE);
        } else {
            btnProfile.setVisibility(View.GONE);
            btnSettings.setVisibility(View.VISIBLE);
        }
    }

    private void setupNewToolbar() {
        if (sharedPref.getIsDarkTheme()) {
            lytSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_search_bar));
            btnSearch.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
            titleToolbar.setTextColor(ContextCompat.getColor(this, R.color.color_dark_icon));
        } else {
            lytSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.color_light_search_bar));
            btnSearch.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
            titleToolbar.setTextColor(ContextCompat.getColor(this, R.color.color_light_text));
        }

        searchBar.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

        btnSearch.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

        titleToolbar.setText(getString(R.string.app_name));

        btnProfile.setOnClickListener(view -> openSettings());
        btnSettings.setOnClickListener(view -> openSettings());

        if (sharedPref.getLoginFeature().equals("yes")) {
            btnProfile.setVisibility(View.VISIBLE);
            btnSettings.setVisibility(View.GONE);
        } else {
            btnProfile.setVisibility(View.GONE);
            btnSettings.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (AppConfig.ENABLE_RTL_MODE) {
            if (viewPagerRTL.getCurrentItem() != 0) {
                viewPagerRTL.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        } else {
            if (viewPager.getCurrentItem() != 0) {
                viewPager.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        }

    }

    public void exitApp() {
        if (AppConfig.ENABLE_EXIT_DIALOG) {
            if (lyt_dialog_exit.getVisibility() != View.VISIBLE) {
                showDialog(true);
            }
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showSnackBar(getString(R.string.press_again_to_exit));
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
            }
        }
    }

    public void initExitDialog() {

        lyt_dialog_exit = findViewById(R.id.lyt_dialog_exit);
        lyt_panel_view = findViewById(R.id.lyt_panel_view);
        lyt_panel_dialog = findViewById(R.id.lyt_panel_dialog);

        if (sharedPref.getIsDarkTheme()) {
            lyt_panel_view.setBackgroundColor(getResources().getColor(R.color.color_dialog_background_dark_overlay));
            lyt_panel_dialog.setBackgroundResource(R.drawable.bg_rounded_dark);
        } else {
            lyt_panel_view.setBackgroundColor(getResources().getColor(R.color.color_dialog_background_light));
            lyt_panel_dialog.setBackgroundResource(R.drawable.bg_rounded_default);
        }

        lyt_panel_view.setOnClickListener(view -> {
            //empty state
        });

        LinearLayout nativeAdView = findViewById(R.id.native_ad_view);
        Tools.setNativeAdStyle(this, nativeAdView, adsPref.getNativeAdStyleExitDialog());
        adsManager.loadNativeAdView(adsPref.getIsNativeAdExitDialog());

        Button btnCancel = findViewById(R.id.btn_cancel);
        Button btnExit = findViewById(R.id.btn_exit);

        FloatingActionButton btnRate = findViewById(R.id.btn_rate);
        FloatingActionButton btnShare = findViewById(R.id.btn_share);

        btnCancel.setOnClickListener(view -> showDialog(false));

        btnExit.setOnClickListener(view -> {
            showDialog(false);
            Tools.postDelayed(()-> {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
            }, 300);
            Constant.isAppOpen = false;
        });

        btnRate.setOnClickListener(v -> {
            Tools.rateApps(MainActivity.this);
            showDialog(false);
        });
        btnShare.setOnClickListener(v -> {
            Tools.shareApps(MainActivity.this);
            showDialog(false);
        });
    }

    private void showDialog(boolean show) {
        if (show) {
            lyt_dialog_exit.setVisibility(View.VISIBLE);
            slideUp(lyt_panel_dialog);
            Tools.dialogStatusBarNavigationColor(this, sharedPref.getIsDarkTheme());
        } else {
            slideDown(lyt_panel_dialog);
            Tools.postDelayed(() -> {
                lyt_dialog_exit.setVisibility(View.GONE);
                Tools.setNavigation(this);
            }, 300);
        }
    }

    public void slideUp(View view) {
        view.setVisibility(View.VISIBLE);
        TranslateAnimation animate = new TranslateAnimation(0, 0, findViewById(R.id.main_content).getHeight(), 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void slideDown(View view) {
        TranslateAnimation animate = new TranslateAnimation(0, 0, 0, findViewById(R.id.main_content).getHeight());
        animate.setDuration(300);
        animate.setFillAfter(true);
        view.startAnimation(animate);
    }

    public void showSnackBar(String msg) {
        Snackbar.make(parentView, msg, Snackbar.LENGTH_SHORT).show();
    }

    private void displayUserProfile() {
        if (sharedPref.getIsLogin()) {
            requestUserData();
        } else {
            imgProfile.setImageResource(R.drawable.ic_account_circle_white);
            imgAvatar.setVisibility(View.GONE);
            if (sharedPref.getIsDarkTheme()) {
                imgProfile.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
            } else {
                if (AppConfig.ENABLE_NEW_APP_DESIGN) {
                    imgProfile.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
                } else {
                    imgProfile.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }

    private void requestUserData() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getUser(sharedPref.getUserId());
        callbackCall.enqueue(new Callback<CallbackUser>() {
            @Override
            public void onResponse(@NonNull Call<CallbackUser> call, @NonNull Response<CallbackUser> response) {
                CallbackUser resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    user = resp.response;
                    if (user.image.equals("")) {
                        imgProfile.setImageResource(R.drawable.ic_account_circle_white);
                        imgAvatar.setVisibility(View.GONE);
                        if (sharedPref.getIsDarkTheme()) {
                            imgProfile.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
                        } else {
                            if (AppConfig.ENABLE_NEW_APP_DESIGN) {
                                imgProfile.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
                            } else {
                                imgProfile.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
                            }
                        }
                    } else {
                        Glide.with(MainActivity.this)
                                .load(sharedPref.getBaseUrl() + "/upload/avatar/" + user.image.replace(" ", "%20"))
                                .apply(new RequestOptions().override(54, 54))
                                .thumbnail(Tools.RequestBuilder(MainActivity.this))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .into(imgProfile);

                        Glide.with(MainActivity.this)
                                .load(sharedPref.getBaseUrl() + "/upload/avatar/" + user.image.replace(" ", "%20"))
                                .apply(new RequestOptions().override(54, 54))
                                .thumbnail(Tools.RequestBuilder(MainActivity.this))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .into(imgAvatar);
                        imgAvatar.setVisibility(View.VISIBLE);
                    }

                    if (user.status.equals("0")) {
                        dialogAccountDisabled();
                        imgAvatar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackUser> call, @NonNull Throwable t) {
                if (sharedPref.getLoginFeature().equals("yes")) {
                    imgProfile.setImageResource(R.drawable.ic_account_circle_white);
                } else {
                    imgProfile.setImageResource(R.drawable.ic_settings_white);
                }
            }

        });
    }

    private void dialogAccountDisabled() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.login_disabled);
        dialog.setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
            sharedPref.setIsLogin(false);
            sharedPref.setUserId("");
            sharedPref.setUserName("");
            sharedPref.setUserEmail("");
            sharedPref.setUserImage("");
            sharedPref.setUserPassword("");
            new Handler().postDelayed(this::recreate, 200);
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Tools.postDelayed(() -> {
            if (AppOpenAd.isAppOpenAdLoaded) {
                adsManager.showAppOpenAd(adsPref.getIsAppOpenAdOnResume());
            }
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Constant.isProfileUpdated) {
            Tools.postDelayed(()-> Constant.isProfileUpdated = false, 100);
            displayUserProfile();
        }
        adsManager.resumeBannerAd(adsPref.getIsBannerHome());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBannerAd();
        destroyAppOpenAd();
    }

    public void showInterstitialAd() {
        adsManager.showInterstitialAd();
    }

    public void destroyAppOpenAd() {
        if (AppConfig.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            adsManager.destroyAppOpenAd(adsPref.getIsAppOpenAdOnResume());
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        }
        Constant.isAppOpen = false;
    }

    public void destroyBannerAd() {
        adsManager.destroyBannerAd();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                                Log.d(TAG, "In-App Review Success");
                            }
                    ).addOnFailureListener(failure -> {
                        Log.d(TAG, "In-App Review Rating Failed");
                    });
                }
            }).addOnFailureListener(failure -> Log.d("In-App Review", "In-App Request Failed " + failure));
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }

    private void inAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, Constant.IMMEDIATE_APP_UPDATE_REQ_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_CANCELED) {
                showSnackBar(getString(R.string.msg_cancel_update));
            } else if (resultCode == RESULT_OK) {
                showSnackBar(getString(R.string.msg_success_update));
            } else {
                showSnackBar(getString(R.string.msg_failed_update));
                inAppUpdate();
            }
        }
    }

    private void openSettings() {
        Intent intent;
        if (AppConfig.ENABLE_NEW_APP_DESIGN) {
            intent = new Intent(getApplicationContext(), ActivitySettingsNew.class);
        } else {
            intent = new Intent(getApplicationContext(), ActivitySettings.class);
        }
        startActivity(intent);
    }

}
