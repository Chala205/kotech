package com.app.androidnewsapp.activity;

import static com.app.androidnewsapp.util.Constant.ITEM_ID;
import static com.app.androidnewsapp.util.Constant.ITEM_NAME;
import static com.app.androidnewsapp.util.Constant.LOCALHOST_ADDRESS;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;
import static com.solodroid.ads.sdk.util.Constant.WORTISE;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.androidnewsapp.BuildConfig;
import com.app.androidnewsapp.R;
import com.app.androidnewsapp.callback.CallbackSettings;
import com.app.androidnewsapp.config.AppConfig;
import com.app.androidnewsapp.database.prefs.AdsPref;
import com.app.androidnewsapp.database.prefs.SharedPref;
import com.app.androidnewsapp.models.Ads;
import com.app.androidnewsapp.models.App;
import com.app.androidnewsapp.models.License;
import com.app.androidnewsapp.models.Placement;
import com.app.androidnewsapp.models.Settings;
import com.app.androidnewsapp.rest.RestAdapter;
import com.app.androidnewsapp.util.AdsManager;
import com.app.androidnewsapp.util.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    ProgressBar progressBar;
    SharedPref sharedPref;
    ImageView imgSplash;
    Call<CallbackSettings> callbackCall = null;
    AdsManager adsManager;
    AdsPref adsPref;
    Settings settings;
    Ads ads;
    Placement adsPlacement;
    App app;
    License license;
    boolean isForceOpenAds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_splash);
        Tools.setNavigation(this);
        isForceOpenAds = AppConfig.FORCE_TO_SHOW_APP_OPEN_AD_ON_START;
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        adsManager.initializeAd();

        imgSplash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            imgSplash.setImageResource(R.drawable.bg_splash_dark);
        } else {
            imgSplash.setImageResource(R.drawable.bg_splash_default);
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        Tools.postDelayed(this::requestConfig, AppConfig.DELAY_SPLASH_SCREEN);

    }

    @SuppressWarnings("ConstantValue")
    private void requestConfig() {
        if (AppConfig.SERVER_KEY.contains("XXXX")) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("App not configured")
                    .setMessage("Please put your Server Key and Rest API Key from settings menu in your admin panel to AppConfig, you can see the documentation for more detailed instructions.")
                    .setPositiveButton(getString(R.string.dialog_ok), (dialogInterface, i) -> showAppOpenAdIfAvailable())
                    .setCancelable(false)
                    .show();
        } else {
            String data = Tools.decode(AppConfig.SERVER_KEY);
            String[] results = data.split("_applicationId_");
            String baseUrl = results[0].replace("localhost", LOCALHOST_ADDRESS);
            String applicationId = results[1];
            sharedPref.setBaseUrl(baseUrl);

            if (applicationId.equals(BuildConfig.APPLICATION_ID)) {
                if (Tools.isConnect(this)) {
                    requestAPI(baseUrl);
                } else {
                    showAppOpenAdIfAvailable();
                }
            } else {
                String message = "applicationId does not match, applicationId in your app is : " + BuildConfig.APPLICATION_ID +
                        "\n\n But your Server Key is registered with applicationId : " + applicationId + "\n\n" +
                        "Please update your Server Key with the appropriate registration applicationId that is used in your Android project.";
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Error")
                        .setMessage(Html.fromHtml(message))
                        .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        }
    }

    private void requestAPI(String apiUrl) {
        this.callbackCall = RestAdapter.createAPI(apiUrl).getSettings(BuildConfig.APPLICATION_ID, AppConfig.REST_API_KEY);
        this.callbackCall.enqueue(new Callback<CallbackSettings>() {
            public void onResponse(@NonNull Call<CallbackSettings> call, @NonNull Response<CallbackSettings> response) {
                CallbackSettings resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    settings = resp.settings;
                    app = resp.app;
                    ads = resp.ads;
                    adsPlacement = resp.ads_placement;
                    license = resp.license;

                    adsPref.saveAds(
                            ads.ad_status.replace("on", "1"),
                            ads.ad_type,
                            ads.backup_ads,
                            ads.admob_publisher_id,
                            ads.admob_banner_unit_id,
                            ads.admob_interstitial_unit_id,
                            ads.admob_native_unit_id,
                            ads.admob_app_open_ad_unit_id,
                            ads.ad_manager_banner_unit_id,
                            ads.ad_manager_interstitial_unit_id,
                            ads.ad_manager_native_unit_id,
                            ads.ad_manager_app_open_ad_unit_id,
                            ads.fan_banner_unit_id,
                            ads.fan_interstitial_unit_id,
                            ads.fan_native_unit_id,
                            ads.startapp_app_id,
                            ads.unity_game_id,
                            ads.unity_banner_placement_id,
                            ads.unity_interstitial_placement_id,
                            ads.applovin_banner_ad_unit_id,
                            ads.applovin_interstitial_ad_unit_id,
                            ads.applovin_native_ad_manual_unit_id,
                            ads.applovin_app_open_ad_unit_id,
                            ads.applovin_banner_zone_id,
                            ads.applovin_banner_mrec_zone_id,
                            ads.applovin_interstitial_zone_id,
                            ads.ironsource_app_key,
                            ads.ironsource_banner_placement_name,
                            ads.ironsource_interstitial_placement_name,
                            ads.wortise_app_id,
                            ads.wortise_app_open_unit_id,
                            ads.wortise_banner_unit_id,
                            ads.wortise_interstitial_unit_id,
                            ads.wortise_native_unit_id,
                            ads.interstitial_ad_interval,
                            ads.native_ad_index
                    );

                    adsPref.setNativeAdStyle(
                            ads.native_ad_style_post_list,
                            ads.native_ad_style_video_list,
                            ads.native_ad_style_post_details,
                            ads.native_ad_style_exit_dialog
                    );

                    adsPref.setPlacement(
                            adsPlacement.banner_home == 1,
                            adsPlacement.banner_post_details == 1,
                            adsPlacement.banner_category_details == 1,
                            adsPlacement.banner_search == 1,
                            adsPlacement.banner_comment == 1,
                            adsPlacement.interstitial_post_list == 1,
                            adsPlacement.interstitial_post_details == 1,
                            adsPlacement.native_ad_post_list == 1,
                            adsPlacement.native_ad_post_details == 1,
                            adsPlacement.native_ad_exit_dialog == 1,
                            adsPlacement.app_open_ad_on_start == 1,
                            adsPlacement.app_open_ad_on_resume == 1
                    );

                    sharedPref.setConfig(
                            settings.privacy_policy,
                            settings.publisher_info,
                            settings.login_feature,
                            settings.comment_approval,
                            settings.video_menu,
                            settings.more_apps_url,
                            settings.youtube_api_key,
                            license.item_id,
                            license.item_name,
                            license.license_type,
                            app.redirect_url
                    );

                    if (license.item_id.equals(ITEM_ID) && license.item_name.equals(ITEM_NAME)) {
                        if (app.status != null && app.status.equals("0")) {
                            Tools.postDelayed(()-> {
                                startActivity(new Intent(getApplicationContext(), ActivityRedirect.class));
                                finish();
                            }, 100);
                            Log.d(TAG, "App is inactive, call redirect method");
                        } else {
                            showAppOpenAdIfAvailable();
                            Log.d(TAG, "App is active");
                        }
                        Log.d(TAG, "License is valid");
                    } else {
                        new MaterialAlertDialogBuilder(ActivitySplash.this)
                                .setTitle("Invalid License")
                                .setMessage("Whoops! this application cannot be accessed due to invalid item purchase code, if you are the owner of this application, please buy this item officially on Codecanyon to get item purchase code so that the application can be accessed by your users.")
                                .setPositiveButton("Buy this item", (dialog, which) -> {
                                    finish();
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://codecanyon.net/item/android-news-app/10771397")));
                                })
                                .setNegativeButton("Later", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    }

                }
            }

            public void onFailure(@NonNull Call<CallbackSettings> call, @NonNull Throwable th) {
                Log.e(TAG, "onFailure : " + th.getMessage());
                showAppOpenAdIfAvailable();
            }
        });
    }

    private void showAppOpenAdIfAvailable() {
        if (isForceOpenAds) {
            if (adsPref.getIsOpenAd()) {
                adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnStart(), this::startMainActivity);
            } else {
                startMainActivity();
            }
        } else {
            if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getIsAppOpenAdOnStart()) {
                switch (adsPref.getMainAds()) {
                    case ADMOB:
                        if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    case GOOGLE_AD_MANAGER:
                        if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    case APPLOVIN:
                    case APPLOVIN_MAX:
                        if (!adsPref.getAppLovinAppOpenAdUnitId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    case WORTISE:
                        if (!adsPref.getWortiseAppOpenId().equals("0")) {
                            ((MyApplication) getApplication()).showAdIfAvailable(ActivitySplash.this, this::startMainActivity);
                        } else {
                            startMainActivity();
                        }
                        break;
                    default:
                        startMainActivity();
                        break;
                }
            } else {
                startMainActivity();
            }
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

}
