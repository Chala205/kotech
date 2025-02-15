package com.app.androidnewsapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.androidnewsapp.R;
import com.app.androidnewsapp.callback.CallbackLogin;
import com.app.androidnewsapp.database.prefs.SharedPref;
import com.app.androidnewsapp.models.User;
import com.app.androidnewsapp.rest.ApiInterface;
import com.app.androidnewsapp.rest.RestAdapter;
import com.app.androidnewsapp.util.Constant;
import com.app.androidnewsapp.util.MaterialProgressDialog;
import com.app.androidnewsapp.util.Tools;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import id.solodroid.validationlibrary.Rule;
import id.solodroid.validationlibrary.Validator;
import id.solodroid.validationlibrary.annotation.Email;
import id.solodroid.validationlibrary.annotation.Password;
import id.solodroid.validationlibrary.annotation.Required;
import id.solodroid.validationlibrary.annotation.TextRule;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityUserLogin extends AppCompatActivity implements Validator.ValidationListener {

    String strEmail, strPassword;
    @Required(order = 1)
    @Email(order = 2, message = "Please Check and Enter a valid Email Address")
    EditText edtEmail;

    @Required(order = 3)
    @Password(order = 4, message = "Enter a Valid Password")
    @TextRule(order = 5, minLength = 6, message = "Enter a Password Correctly")
    EditText edtPassword;

    private Validator validator;
    Button btnSingIn, btnSignUp;
    TextView txtForgot;

    SharedPref sharedPref;
    MaterialProgressDialog.Builder progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_user_login);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);

        validator = new Validator(this);
        validator.setValidationListener(this);

        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        btnSingIn = findViewById(R.id.btn_login);
        btnSignUp = findViewById(R.id.btn_create);
        txtForgot = findViewById(R.id.txt_forgot);

        btnSingIn.setOnClickListener(v -> validator.validateAsync());

        txtForgot.setOnClickListener(v -> {
            Intent intent = new Intent(ActivityUserLogin.this, ActivityForgotPassword.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), ActivityUserRegister.class));
            finish();
        });
    }

    @Override
    public void onValidationSucceeded() {
        strEmail = edtEmail.getText().toString();
        strPassword = edtPassword.getText().toString();
        if (Tools.isConnect(ActivityUserLogin.this)) {
            doLogin(strEmail, strPassword);
        }
    }

    @Override
    public void onValidationFailed(View failedView, Rule<?> failedRule) {
        String message = failedRule.getFailureMessage();
        if (failedView instanceof EditText) {
            failedView.requestFocus();
            ((EditText) failedView).setError(message);
        } else {
            Toast.makeText(this, "Record Not Saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void doLogin(String email, String password) {

        progressDialog = new MaterialProgressDialog.Builder(ActivityUserLogin.this).build();
        progressDialog.setMessage(getResources().getString(R.string.login_process));
        progressDialog.setCancelable(false);
        progressDialog.show();

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        Call<CallbackLogin> call = apiInterface.userLogin(email, password);
        call.enqueue(new Callback<CallbackLogin>() {
            @Override
            public void onResponse(@NonNull Call<CallbackLogin> call, @NonNull Response<CallbackLogin> response) {
                CallbackLogin resp = response.body();
                if (resp != null) {
                    User user = resp.user;
                    new Handler().postDelayed(() -> {
                        if (resp.status.equals("ok")) {
                            if (user.status.equals("1")) {
                                sharedPref.setIsLogin(true);
                                sharedPref.setUserId(user.id);
                                sharedPref.setUserName(user.name);
                                sharedPref.setUserEmail(user.email);
                                sharedPref.setUserImage(user.image);
                                sharedPref.setUserPassword(password);
                                dialogSuccessLogin();
                                Constant.isProfileUpdated = true;
                            } else if (user.status.equals("0")) {
                                dialogAccountDisabled();
                            } else {
                                dialogError();
                            }
                        } else if (resp.status.equals("failed")) {
                            dialogFailedLogin();
                        } else {
                            dialogError();
                        }
                        progressDialog.dismiss();
                    }, Constant.DELAY_REFRESH);
                } else {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackLogin> call, @NonNull Throwable t) {
                t.printStackTrace();
                dialogError();
                progressDialog.dismiss();
                Log.d("rawr", t.getMessage());

            }
        });
    }

    private void dialogSuccessLogin() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.login_title);
        dialog.setMessage(R.string.login_success);
        dialog.setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> finish());
        dialog.setCancelable(false);
        dialog.show();
    }

    private void dialogFailedLogin() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.login_failed);
        dialog.setPositiveButton(R.string.dialog_ok, null);
        dialog.setCancelable(false);
        dialog.show();
    }

    public void dialogError() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.msg_no_network);
        dialog.setPositiveButton(R.string.dialog_ok, null);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void dialogAccountDisabled() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.login_disabled);
        dialog.setPositiveButton(R.string.dialog_ok, null);
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

}
