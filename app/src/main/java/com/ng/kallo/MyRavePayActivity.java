package com.ng.kallo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.flutterwave.raveandroid.RavePayActivity;
import com.flutterwave.raveandroid.RaveUiManager;
import com.flutterwave.raveandroid.rave_java_commons.RaveConstants;
import com.ng.kallo.database.DatabaseHelper;
import com.ng.kallo.network.RetrofitClient;
import com.ng.kallo.network.apis.PaymentApi;
import com.ng.kallo.network.apis.SubscriptionApi;
import com.ng.kallo.network.model.ActiveStatus;
import com.ng.kallo.network.model.Package;
import com.ng.kallo.network.model.User;
import com.ng.kallo.network.model.config.PaymentConfig;
import com.ng.kallo.utils.PreferenceUtils;
import com.ng.kallo.utils.ToastMsg;

import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MyRavePayActivity extends AppCompatActivity{
    private static final String TAG = "RavePayActivity";
    private Package aPackage;
    private DatabaseHelper databaseHelper;
    private ProgressBar progressBar;

    private DatabaseHelper db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_razor_pay);
        progressBar = findViewById(R.id.progress_bar);
        aPackage = (Package) getIntent().getSerializableExtra("package");
        databaseHelper = new DatabaseHelper(this);
        //Checkout.preload(getApplicationContext());
        RaveUiManager();


    }




    private void updateActiveStatus() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        SubscriptionApi subscriptionApi = retrofit.create(SubscriptionApi.class);

        Call<ActiveStatus> call = subscriptionApi.getActiveStatus(Config.API_KEY, PreferenceUtils.getUserId(MyRavePayActivity.this));
        call.enqueue(new Callback<ActiveStatus>() {
            @Override
            public void onResponse(Call<ActiveStatus> call, Response<ActiveStatus> response) {
                if (response.code() == 200) {
                    ActiveStatus activeStatus = response.body();
                    DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                    db.deleteAllActiveStatusData();
                    db.insertActiveStatusData(activeStatus);
                    new ToastMsg(MyRavePayActivity.this).toastIconSuccess(getResources().getString(R.string.payment_success));
                    finish();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ActiveStatus> call, Throwable t) {
                t.printStackTrace();
                new ToastMsg(MyRavePayActivity.this).toastIconError(getString(R.string.something_went_wrong));
                finish();
                progressBar.setVisibility(View.GONE);
            }
        });

    }


    public void saveChargeData() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        Call<ResponseBody> call = paymentApi.savePayment(Config.API_KEY, aPackage.getPlanId(), databaseHelper.getUserData().getUserId(), aPackage.getPrice(),aPackage.getName(), "RavePay");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    updateActiveStatus();

                } else {
                    new ToastMsg(MyRavePayActivity.this).toastIconError(getString(R.string.something_went_wrong));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                new ToastMsg(MyRavePayActivity.this).toastIconError(getString(R.string.something_went_wrong));
            }
        });


    }


    public void RaveUiManager(){
        progressBar.setVisibility(View.VISIBLE);
        PaymentConfig config = databaseHelper.getConfigurationData().getPaymentConfig();
        User user = databaseHelper.getUserData();
        final Activity activity = this;
        Double amount = (Double.valueOf(aPackage.getPrice()));


        String newamount = aPackage.getPrice();

        final int random = new Random().nextInt(61) + 20;
        new RaveUiManager(this).setAmount(amount)
                .setCurrency(config.getCurrency())
                .setEmail(user.getEmail())
                .setfName(user.getName())
                .setlName(user.getName())
                .setNarration(aPackage.getName())
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
                .shouldDisplayFee(true)
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
                saveChargeData();
                new ToastMsg(getApplicationContext()).toastIconSuccess(getString(R.string.payment_success));
            }
            else if (resultCode == RavePayActivity.RESULT_ERROR) {
                finish();
                progressBar.setVisibility(View.GONE);
                new ToastMsg(getApplicationContext()).toastIconError(getString(R.string.payment_failed));

            }
            else if (resultCode == RavePayActivity.RESULT_CANCELLED) {
                finish();
                progressBar.setVisibility(View.GONE);
                new ToastMsg(getApplicationContext()).toastIconError("CANCELLED");

            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
