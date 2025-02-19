package com.app.androidnewsapp.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.app.androidnewsapp.BuildConfig;
import com.app.androidnewsapp.R;
import com.app.androidnewsapp.adapter.AdapterSearch;
import com.app.androidnewsapp.database.prefs.SharedPref;
import com.app.androidnewsapp.util.Constant;
import com.app.androidnewsapp.util.MaterialProgressDialog;
import com.app.androidnewsapp.util.Tools;
import com.app.androidnewsapp.util.ViewAnimation;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.DecimalFormat;

public class ActivitySettingsNew extends AppCompatActivity {

    private static final String TAG = "Settings";
    MaterialSwitch switchTheme;
    LinearLayout btnSwitchTheme;
    RelativeLayout lytUser;
    View lytSignIn, lytSignOut;
    TextView txtLogin;
    TextView txtRegister, txtUsername, txtEmail;
    ImageView imgProfile;
    MaterialProgressDialog.Builder progressDialog;
    Button btnLogout;
    private String singleChoiceSelected;
    TextView txtCacheSize;
    AdapterSearch adapterSearch;
    SharedPref sharedPref;
    private LinearLayout btn_toggle_general;
    private LinearLayout btn_toggle_cache;
    private LinearLayout btn_toggle_privacy;
    private LinearLayout btn_toggle_about;
    private View lyt_expand_general;
    private View lyt_expand_cache;
    private View lyt_expand_privacy;
    private View lyt_expand_about;
    NestedScrollView nested_scroll_view;
    ImageButton btn_arrow_general;
    ImageButton btn_arrow_cache;
    ImageButton btn_arrow_privacy;
    ImageButton btn_arrow_about;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_settings_new);
        sharedPref = new SharedPref(this);
        Tools.setNavigation(this);
        Tools.setupAppBarLayout(this);
        initView();
        initToggleView();
        setupToolbar();
        requestAction();
    }

    public void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        String title;
        if (sharedPref.getLoginFeature().equals("yes")) {
            title = getResources().getString(R.string.title_menu_profile);
        } else {
            title = getResources().getString(R.string.title_menu_settings);
        }
        Tools.setupToolbar(this, toolbar, title, true);
    }

    private void initView() {
        lytUser = findViewById(R.id.lyt_user);

        lytSignIn = findViewById(R.id.view_sign_in);
        lytSignOut = findViewById(R.id.view_sign_out);

        txtLogin = findViewById(R.id.btn_login);
        txtRegister = findViewById(R.id.txt_register);

        txtUsername = findViewById(R.id.txt_username);
        txtEmail = findViewById(R.id.txt_email);
        imgProfile = findViewById(R.id.img_profile);
        btnLogout = findViewById(R.id.btn_logout);

        if (!sharedPref.getLoginFeature().equals("yes")) {
            lytUser.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
            findViewById(R.id.material_divider).setVisibility(View.GONE);
        }

        switchTheme = findViewById(R.id.switch_theme);
        switchTheme.setChecked(sharedPref.getIsDarkTheme());
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPref.setIsDarkTheme(isChecked);
            Tools.postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }, 350);
        });

        btnSwitchTheme = findViewById(R.id.btn_switch_theme);
        btnSwitchTheme.setOnClickListener(v -> {
            if (switchTheme.isChecked()) {
                sharedPref.setIsDarkTheme(false);
                switchTheme.setChecked(false);
            } else {
                sharedPref.setIsDarkTheme(true);
                switchTheme.setChecked(true);
            }
            Tools.postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }, 350);
        });

        findViewById(R.id.btn_text_size).setOnClickListener(v -> {
            String[] items = getResources().getStringArray(R.array.dialog_font_size);
            singleChoiceSelected = items[sharedPref.getFontSize()];
            int itemSelected = sharedPref.getFontSize();
            new MaterialAlertDialogBuilder(ActivitySettingsNew.this)
                    .setTitle(getString(R.string.title_dialog_font_size))
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> singleChoiceSelected = items[i])
                    .setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                        if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_xsmall))) {
                            sharedPref.updateFontSize(0);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_small))) {
                            sharedPref.updateFontSize(1);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_medium))) {
                            sharedPref.updateFontSize(2);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_large))) {
                            sharedPref.updateFontSize(3);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_xlarge))) {
                            sharedPref.updateFontSize(4);
                        } else {
                            sharedPref.updateFontSize(2);
                        }
                        dialogInterface.dismiss();
                    })
                    .show();
        });

        findViewById(R.id.btn_notification).setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID);
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", BuildConfig.APPLICATION_ID);
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            startActivity(intent);
        });

        txtCacheSize = findViewById(R.id.txt_cache_size);
        initializeCache();

        findViewById(R.id.btn_clear_cache).setOnClickListener(v -> clearCache());

        findViewById(R.id.btn_clear_search_history).setOnClickListener(v -> clearSearchHistory());

        findViewById(R.id.btn_privacy_policy).setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityPrivacyPolicy.class)));

        findViewById(R.id.btn_publisher_info).setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityPublisherInfo.class)));

        findViewById(R.id.btn_rate).setOnClickListener(v -> Tools.rateApps(this));

        findViewById(R.id.btn_share).setOnClickListener(v -> Tools.shareApps(this));

        findViewById(R.id.btn_more_apps).setOnClickListener(v -> Tools.moreApps(this, sharedPref.getMoreAppsUrl()));

        findViewById(R.id.btn_about).setOnClickListener(v -> Tools.aboutDialog(this));

    }

    private void initToggleView() {
        nested_scroll_view = findViewById(R.id.nested_scroll_view);

        btn_arrow_general = findViewById(R.id.btn_arrow_general);
        btn_arrow_cache = findViewById(R.id.btn_arrow_cache);
        btn_arrow_privacy = findViewById(R.id.btn_arrow_privacy);
        btn_arrow_about = findViewById(R.id.btn_arrow_about);

        if (sharedPref.getIsDarkTheme()) {
            btn_arrow_general.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon));
            btn_arrow_cache.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon));
            btn_arrow_privacy.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon));
            btn_arrow_about.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon));
        } else {
            btn_arrow_general.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon));
            btn_arrow_cache.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon));
            btn_arrow_privacy.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon));
            btn_arrow_about.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon));
        }

        btn_toggle_general = findViewById(R.id.btn_toggle_general);
        btn_toggle_cache = findViewById(R.id.btn_toggle_cache);
        btn_toggle_privacy = findViewById(R.id.btn_toggle_privacy);
        btn_toggle_about = findViewById(R.id.btn_toggle_about);

        lyt_expand_general = findViewById(R.id.lyt_expand_general);
        lyt_expand_cache = findViewById(R.id.lyt_expand_cache);
        lyt_expand_privacy = findViewById(R.id.lyt_expand_privacy);
        lyt_expand_about = findViewById(R.id.lyt_expand_about);

        btn_toggle_general.setOnClickListener(view -> {
            toggleSection(btn_toggle_general, lyt_expand_general, btn_arrow_general);
        });

        btn_toggle_cache.setOnClickListener(view -> {
            toggleSection(btn_toggle_cache, lyt_expand_cache, btn_arrow_cache);
        });

        btn_toggle_privacy.setOnClickListener(view -> {
            toggleSection(btn_toggle_privacy, lyt_expand_privacy, btn_arrow_privacy);
        });

        btn_toggle_about.setOnClickListener(view -> {
            toggleSection(btn_toggle_about, lyt_expand_about, btn_arrow_about);
        });

    }

    private void toggleSection(View view, View layout, ImageButton imageButton) {
        boolean show = toggleArrow(view, imageButton);
        viewAnimation(show, layout);
    }

    public boolean toggleArrow(View view, ImageButton imageButton) {
        if (view.getRotation() == 0) {
            view.animate().setDuration(100).rotation((float) 0.001);
            imageButton.setImageResource(R.drawable.ic_arrow_expand);
            return true;
        } else {
            view.animate().setDuration(100).rotation(0);
            imageButton.setImageResource(R.drawable.ic_arrow_collapse);
            return false;
        }
    }

    public void viewAnimation(boolean show, View view) {
        if (show) {
            ViewAnimation.expand(view, () -> Tools.nestedScrollTo(nested_scroll_view, view));
        } else {
            ViewAnimation.collapse(view);
        }
    }

    private void clearSearchHistory() {
        adapterSearch = new AdapterSearch(this);
        if (adapterSearch.getItemCount() > 0) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ActivitySettingsNew.this);
            builder.setTitle(getString(R.string.title_dialog_clear_search_history));
            builder.setMessage(getString(R.string.msg_dialog_clear_search_history));
            builder.setPositiveButton(R.string.dialog_yes, (di, i) -> {
                adapterSearch.clearSearchHistory();
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.clearing_success), Snackbar.LENGTH_SHORT).show();
            });
            builder.setNegativeButton(R.string.dialog_cancel, null);
            builder.show();
        } else {
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.clearing_empty), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void clearCache() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ActivitySettingsNew.this);
        builder.setTitle(getString(R.string.title_dialog_clear_cache));
        builder.setMessage(getString(R.string.msg_dialog_clear_cache));
        builder.setPositiveButton(R.string.dialog_yes, (di, i) -> {
            FileUtils.deleteQuietly(getCacheDir());
            FileUtils.deleteQuietly(getExternalCacheDir());
            txtCacheSize.setText(getString(R.string.sub_settings_clear_cache_start) + " 0 Bytes " + getString(R.string.sub_settings_clear_cache_end));
            Snackbar.make(findViewById(android.R.id.content), getString(R.string.msg_cache_cleared), Snackbar.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();
    }

    private void initializeCache() {
        txtCacheSize.setText(getString(R.string.sub_settings_clear_cache_start) + " " + readableFileSize((0 + getDirSize(getCacheDir())) + getDirSize(getExternalCacheDir())) + " " + getString(R.string.sub_settings_clear_cache_end));
    }

    public long getDirSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file != null && file.isDirectory()) {
                size += getDirSize(file);
            } else if (file != null && file.isFile()) {
                size += file.length();
            }
        }
        return size;
    }

    public static String readableFileSize(long size) {
        if (size <= 0) {
            return "0 Bytes";
        }
        String[] units = new String[]{"Bytes", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10((double) size) / Math.log10(1024.0d));
        StringBuilder stringBuilder = new StringBuilder();
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        double d = (double) size;
        double pow = Math.pow(1024.0d, (double) digitGroups);
        Double.isNaN(d);
        stringBuilder.append(decimalFormat.format(d / pow));
        stringBuilder.append(" ");
        stringBuilder.append(units[digitGroups]);
        return stringBuilder.toString();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void requestAction() {
        if (sharedPref.getIsLogin()) {
            lytSignIn.setVisibility(View.VISIBLE);
            lytSignOut.setVisibility(View.GONE);

            btnLogout.setVisibility(View.VISIBLE);
            btnLogout.setOnClickListener(view -> logoutDialog());

            displayData();
        } else {
            lytSignIn.setVisibility(View.GONE);
            lytSignOut.setVisibility(View.VISIBLE);
            txtLogin.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityUserLogin.class)));
            txtRegister.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityUserRegister.class)));
            btnLogout.setVisibility(View.GONE);
        }
    }

    public void displayData() {

        ((TextView) findViewById(R.id.txt_username)).setText(Tools.usernameFormatter(sharedPref.getUserName()));
        ((TextView) findViewById(R.id.txt_email)).setText(sharedPref.getUserEmail());

        ImageView imgProfile = findViewById(R.id.img_profile);
        if (sharedPref.getUserImage().equals("")) {
            imgProfile.setImageResource(R.drawable.ic_user_account);
        } else {
            Glide.with(this)
                    .load(sharedPref.getBaseUrl() + "/upload/avatar/" + sharedPref.getUserImage().replace(" ", "%20"))
                    .placeholder(R.drawable.ic_user_account)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(new RequestOptions().override(256, 256))
                    .centerCrop()
                    .into(imgProfile);
        }

        RelativeLayout btnEdit = findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), ActivityEditProfile.class);
            intent.putExtra("user_id", sharedPref.getUserId());
            intent.putExtra("user_name", Tools.usernameFormatter(sharedPref.getUserName()));
            intent.putExtra("user_email", sharedPref.getUserEmail());
            intent.putExtra("user_image", sharedPref.getUserImage());
            intent.putExtra("user_password", sharedPref.getUserPassword());
            startActivity(intent);
        });

    }

    private void onFailRequest() {
        if (!Tools.isConnect(this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.msg_no_network), Toast.LENGTH_SHORT).show();
        }
    }

    public void logoutDialog() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ActivitySettingsNew.this);
        builder.setTitle(R.string.logout_title);
        builder.setMessage(R.string.logout_message);
        builder.setPositiveButton(R.string.dialog_yes, (di, i) -> {

            progressDialog = new MaterialProgressDialog.Builder(ActivitySettingsNew.this).build();
            progressDialog.setMessage(getResources().getString(R.string.logout_process));
            progressDialog.setCancelable(false);
            progressDialog.show();

            sharedPref.setIsLogin(false);
            sharedPref.setUserId("");
            sharedPref.setUserName("");
            sharedPref.setUserEmail("");
            sharedPref.setUserImage("");
            sharedPref.setUserPassword("");

            new Handler().postDelayed(() -> {
                progressDialog.dismiss();
                MaterialAlertDialogBuilder builder1 = new MaterialAlertDialogBuilder(ActivitySettingsNew.this);
                builder1.setMessage(R.string.logout_success);
                builder1.setPositiveButton(R.string.dialog_ok, (dialogInterface, i1) -> finish());
                builder1.setCancelable(false);
                builder1.show();
            }, Constant.DELAY_PROGRESS_DIALOG);

            Constant.isProfileUpdated = true;

        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();

    }

    @Override
    public void onResume() {
        super.onResume();
        requestAction();
    }

}
