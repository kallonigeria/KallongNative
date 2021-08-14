package com.ng.kallo;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.flutterwave.raveandroid.RavePayActivity;
import com.flutterwave.raveandroid.RaveUiManager;
import com.flutterwave.raveandroid.rave_java_commons.RaveConstants;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.ng.kallo.database.DatabaseHelper;
import com.ng.kallo.network.RetrofitClient;
import com.ng.kallo.network.apis.ProfileApi;
import com.ng.kallo.network.apis.WalletApi;
import com.ng.kallo.network.model.ResponseStatus;
import com.ng.kallo.network.model.User;
import com.ng.kallo.network.model.config.PaymentConfig;
import com.ng.kallo.utils.PreferenceUtils;
import com.ng.kallo.utils.RtlUtils;
import com.ng.kallo.utils.ToastMsg;

import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WalletActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPhone, genderSpinner;
    private TextInputEditText etPass;
    private Button btnUpdate, deactivateBt;
    private ProgressDialog dialog;
    private String URL = "", strGender;
    private CircleImageView userIv;
    private static final int GALLERY_REQUEST_CODE = 1;
    private Uri imageUri;
    private ProgressBar progressBar;
    private String id;
    private boolean isDark;
    private String selectedGender = "Male";

    private String amount;


    private TextView txtbalance;

    private int bal;
    private DatabaseHelper db;

    private Button btn_fund;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RtlUtils.setScreenDirection(this);
        SharedPreferences sharedPreferences = getSharedPreferences("push", MODE_PRIVATE);
        isDark = sharedPreferences.getBoolean("dark", false);

        if (isDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppThemeLight);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        Toolbar toolbar = findViewById(R.id.toolbar);

        if (!isDark) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Wallet");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //---analytics-----------
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "wallet_activity");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "activity");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Please wait");
        dialog.setCancelable(false);

        db = new DatabaseHelper(this);

        txtbalance = findViewById(R.id.wallet_balance);

        btn_fund = findViewById(R.id.fund_wallet);


        btn_fund.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogFundwallet();
            }
        });



        id = PreferenceUtils.getUserId(WalletActivity.this);




        mybalance(db.getUserData().getUserId());




    }


    private void showDialogFundwallet() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.dialog_fund_wallet);
        dialog.setCancelable(true);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;

        final EditText text;
        text = dialog.findViewById(R.id.amount);

        ((View) dialog.findViewById(R.id.fab)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.findViewById(R.id.btn_fund).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                amount = text.getText().toString();
                if(amount.isEmpty()){
                    new ToastMsg(getApplicationContext()).toastIconError("Invalid Amount");

                }else{
                    RaveUiManager(Double.parseDouble(text.getText().toString()));
                }
            }
        });

        dialog.show();
        dialog.getWindow().setAttributes(lp);
    }



    private void mybalance(String userid){
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        WalletApi api = retrofit.create(WalletApi.class);
        Call<User> call = api.postWalletStatus(Config.API_KEY, userid);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {

                if (response.code() == 200) {
                    assert response.body() != null;

                    bal = Integer.parseInt(response.body().getBalance());

                    txtbalance.setText("My Balance: ₦" + bal );

                }else{

                    new ToastMsg(getApplicationContext()).toastIconError("Something went wrong...");
                }

            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {

                new ToastMsg(getApplicationContext()).toastIconError(getString(R.string.error_toast));

            }


        });
    }




    private void updateBalance(String amount) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ProfileApi api = retrofit.create(ProfileApi.class);
        Call<ResponseStatus> call = api.updateBalance(Config.API_KEY, db.getUserData().getUserId(), amount);
        call.enqueue(new Callback<ResponseStatus>() {
            @Override
            public void onResponse(Call<ResponseStatus> call, retrofit2.Response<ResponseStatus> response) {
                if (response.code() == 200) {
                    if (response.body().getStatus().equalsIgnoreCase("success")) {
                        new ToastMsg(getApplicationContext()).toastIconSuccess(response.body().getData());
                        mybalance(db.getUserData().getUserId());
                    } else {
                        new ToastMsg(getApplicationContext()).toastIconError(response.body().getData());
                    }
                } else {
                    new ToastMsg(getApplicationContext()).toastIconError(getString(R.string.something_went_wrong));
                }
            }

            @Override
            public void onFailure(Call<ResponseStatus> call, Throwable t) {
                new ToastMsg(getApplicationContext()).toastIconError(getString(R.string.something_went_wrong));
                t.printStackTrace();
            }
        });
    }




    public void RaveUiManager(double amount){
//        progressBar.setVisibility(View.VISIBLE);

        PaymentConfig config = db.getConfigurationData().getPaymentConfig();

        final int random = new Random().nextInt(61) + 20;
        new RaveUiManager(this).setAmount(amount)
                .setCurrency(config.getCurrency())
                .setEmail(db.getUserData().getEmail())
                .setfName(db.getUserData().getName())
                .setlName(db.getUserData().getName())
                .setNarration("Wallet Funding")
                .setPublicKey("FLWPUBK_TEST-7868ccc828f12110189db5a01dd61591-X")
                .setTxRef(String.valueOf(random))
                .acceptAccountPayments(true)
                .acceptCardPayments(true)
                .acceptMpesaPayments(true)
                .acceptAchPayments(true)
                .acceptGHMobileMoneyPayments(true)
                .acceptUgMobileMoneyPayments(true)
                .acceptZmMobileMoneyPayments(true)
                .acceptRwfMobileMoneyPayments(true)
                .acceptSaBankPayments(true)
                .onStagingEnv(false)
                .acceptUkPayments(true)
                .acceptBankTransferPayments(true)
                .acceptUssdPayments(true)
                .withTheme(R.style.DefaultTheme)
                .acceptBarterPayments(true)
                .acceptFrancMobileMoneyPayments(true)
                .allowSaveCardFeature(true)
//                .shouldDisplayFee(true)
                .setEncryptionKey("FLWSECK_TESTcd060eff8b17")
                .initialize();
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         *  We advise you to do a further verification of transaction's details on your server to be
         *  sure everything checks out before providing service or goods.
         */
        if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null) {
            String message = data.getStringExtra("response");
            if (resultCode == RavePayActivity.RESULT_SUCCESS) {
                updateBalance(amount);
                new ToastMsg(getApplicationContext()).toastIconSuccess(getString(R.string.payment_success));

            }
            else if (resultCode == RavePayActivity.RESULT_ERROR) {
                finish();
                new ToastMsg(getApplicationContext()).toastIconError(getString(R.string.payment_failed));

            }
            else if (resultCode == RavePayActivity.RESULT_CANCELLED) {
                finish();
                new ToastMsg(getApplicationContext()).toastIconError("CANCELLED");
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
