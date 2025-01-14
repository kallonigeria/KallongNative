package com.ng.kallo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ng.kallo.adapters.PackageAdapter;
import com.ng.kallo.bottomshit.PaymentBottomShitDialog;
import com.ng.kallo.database.DatabaseHelper;
import com.ng.kallo.network.RetrofitClient;
import com.ng.kallo.network.apis.PackageApi;
import com.ng.kallo.network.apis.PaymentApi;
import com.ng.kallo.network.apis.ProfileApi;
import com.ng.kallo.network.apis.SubscriptionApi;
import com.ng.kallo.network.apis.WalletApi;
import com.ng.kallo.network.model.ActiveStatus;
import com.ng.kallo.network.model.AllPackage;
import com.ng.kallo.network.model.Package;
import com.ng.kallo.network.model.ResponseStatus;
import com.ng.kallo.network.model.User;
import com.ng.kallo.network.model.config.PaymentConfig;
import com.ng.kallo.utils.PreferenceUtils;
import com.ng.kallo.utils.ApiResources;
import com.ng.kallo.utils.RtlUtils;
import com.ng.kallo.utils.ToastMsg;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class PurchasePlanActivity extends AppCompatActivity implements PackageAdapter.OnItemClickListener, PaymentBottomShitDialog.OnBottomShitClickListener{

    private static final String TAG = PurchasePlanActivity.class.getSimpleName();
    private static final int PAYPAL_REQUEST_CODE = 100;
    private TextView noTv;
    private ProgressBar progressBar;
    private ImageView closeIv;
    private RecyclerView packageRv;
    private List<Package> packages = new ArrayList<>();
    private List<ImageView> imageViews = new ArrayList<>();
    private String currency = "";
    private String exchangeRate;
    private boolean isDark;

    private DatabaseHelper db;

    private String bal;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(ApiResources.PAYPAL_CLIENT_ID);
    private Package packageItem;
    private PaymentBottomShitDialog paymentBottomShitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RtlUtils.setScreenDirection(this);
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("push", MODE_PRIVATE);
        isDark = sharedPreferences.getBoolean("dark", false);

        if (isDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppThemeLight);
        }
        setContentView(R.layout.activity_purchase_plan);

        initView();

        db = new DatabaseHelper(this);

        mybalance(db.getUserData().getUserId());


        // ---------- start paypal service ----------
//        Intent intent = new Intent(this, PayPalService.class);
//        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
//        startService(intent);

        // getting currency symbol
        PaymentConfig config = new DatabaseHelper(PurchasePlanActivity.this).getConfigurationData().getPaymentConfig();
        currency = config.getCurrencySymbol();
        exchangeRate = config.getExchangeRate();
        packageRv.setHasFixedSize(true);
        packageRv.setLayoutManager(new LinearLayoutManager(this));

        getPurchasePlanInfo();
    }

    private void getPurchasePlanInfo() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        final PackageApi packageApi = retrofit.create(PackageApi.class);
        Call<AllPackage> call = packageApi.getAllPackage(Config.API_KEY);
        call.enqueue(new Callback<AllPackage>() {
            @Override
            public void onResponse(Call<AllPackage> call, Response<AllPackage> response) {
                AllPackage allPackage = response.body();
                packages = allPackage.getPackage();
                if (allPackage.getPackage().size() > 0) {
                    noTv.setVisibility(View.GONE);
                    PackageAdapter adapter = new PackageAdapter(PurchasePlanActivity.this, allPackage.getPackage(), currency);
                    adapter.setItemClickListener(PurchasePlanActivity.this);
                    packageRv.setAdapter(adapter);
                } else {
                    noTv.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<AllPackage> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                t.printStackTrace();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        closeIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                PaymentConfirmation confirmation = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirmation != null) {
                    try {
                        String paymentDetails = confirmation.toJSONObject().toString(4);
                        completePayment(paymentDetails);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    new ToastMsg(this).toastIconError("Cancel");
                }
            }

        }else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
            new ToastMsg(this).toastIconError("Invalid");
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    private void completePayment(String paymentDetails) {
        try {
            JSONObject jsonObject = new JSONObject(paymentDetails);
            sendDataToServer(jsonObject.getJSONObject("response"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendDataToServer(JSONObject response) {
        try {
            String payId = response.getString("id");
            final String state = response.getString("state");
            final String userId = PreferenceUtils.getUserId(PurchasePlanActivity.this);

            Retrofit retrofit = RetrofitClient.getRetrofitInstance();
            PaymentApi paymentApi = retrofit.create(PaymentApi.class);
            Call<ResponseBody> call = paymentApi.savePayment(Config.API_KEY, packageItem.getPlanId(), userId, packageItem.getPrice(),
                    payId, "Paypal");

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.code() == 200) {

                        updateActiveStatus(userId);

                    } else {
                        new ToastMsg(PurchasePlanActivity.this).toastIconError(getString(R.string.something_went_wrong));
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    new ToastMsg(PurchasePlanActivity.this).toastIconError(getString(R.string.something_went_wrong));
                    t.printStackTrace();
                    Log.e("PAYMENT", "error: " + t.getLocalizedMessage());
                }

            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void updateActiveStatus(String userId) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        SubscriptionApi subscriptionApi = retrofit.create(SubscriptionApi.class);
        Call<ActiveStatus> call = subscriptionApi.getActiveStatus(com.ng.kallo.Config.API_KEY, userId);
        call.enqueue(new Callback<ActiveStatus>() {
            @Override
            public void onResponse(Call<ActiveStatus> call, Response<ActiveStatus> response) {
                if (response.code() == 200) {
                    ActiveStatus activiStatus = response.body();
                    saveActiveStatus(activiStatus);
                } else {
                    new ToastMsg(PurchasePlanActivity.this).toastIconError("Payment info not save to the own server. something went wrong.");
                }
            }

            @Override
            public void onFailure(Call<ActiveStatus> call, Throwable t) {
                new ToastMsg(PurchasePlanActivity.this).toastIconError(t.getMessage());
                t.printStackTrace();
            }
        });

    }

    private void saveActiveStatus(ActiveStatus activeStatus) {
        DatabaseHelper db = new DatabaseHelper(PurchasePlanActivity.this);
        if (db.getActiveStatusCount() > 1) {
            db.deleteAllActiveStatusData();
        }
        if (db.getActiveStatusCount() == 0) {
            db.insertActiveStatusData(activeStatus);
        } else {
            db.updateActiveStatus(activeStatus, 1);
        }
        new ToastMsg(PurchasePlanActivity.this).toastIconSuccess(getResources().getString(R.string.payment_success));

        /*Intent intent = new Intent(PurchasePlanActivity.this, PapalPaymentActivity.class);
        intent.putExtra("state", state);
        intent.putExtra("amount", packageItem.getPrice());
        startActivity(intent);
*/

        finish();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }

    private void processPaypalPayment(Package packageItem) {
        String[] paypalAcceptedList = getResources().getStringArray(R.array.paypal_currency_list);
        if (Arrays.asList(paypalAcceptedList).contains(ApiResources.CURRENCY)){
            PayPalPayment payPalPayment = new PayPalPayment((new BigDecimal(String.valueOf(packageItem.getPrice()))),
                    ApiResources.CURRENCY,
                    "Payment for Package",
                    PayPalPayment.PAYMENT_INTENT_SALE);

            Log.e("Payment", "currency: " + ApiResources.CURRENCY);
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
            intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);
            startActivityForResult(intent, PAYPAL_REQUEST_CODE);
        }else {
            PaymentConfig paymentConfig = new DatabaseHelper(PurchasePlanActivity.this).getConfigurationData().getPaymentConfig();
            double exchangeRate = Double.parseDouble(paymentConfig.getExchangeRate());
            double price = Double.parseDouble(packageItem.getPrice());
            double priceInUSD = (double) price / exchangeRate;
            PayPalPayment payPalPayment = new PayPalPayment((new BigDecimal(String.valueOf(priceInUSD))),
                    "USD",
                    "Payment for Package",
                    PayPalPayment.PAYMENT_INTENT_SALE);
            Intent intent = new Intent(this, PaymentActivity.class);
            intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
            intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payPalPayment);
            startActivityForResult(intent, PAYPAL_REQUEST_CODE);
        }
    }

    private void initView() {

        noTv = findViewById(R.id.no_tv);
        progressBar = findViewById(R.id.progress_bar);
        packageRv = findViewById(R.id.pacakge_rv);
        closeIv = findViewById(R.id.close_iv);
    }

    @Override
    public void onItemClick(Package pac) {
        packageItem = pac;

        paymentBottomShitDialog = new PaymentBottomShitDialog();
        paymentBottomShitDialog.show(getSupportFragmentManager(), "PaymentBottomShitDialog");
    }

    @Override
    public void onBottomShitClick(String paymentMethodName) {
        if (paymentMethodName.equals(PaymentBottomShitDialog.USSD)) {
//            int amount = Integer.parseInt(packageItem.getPrice());
            int planid = Integer.parseInt(packageItem.getPlanId());
            Log.d("planid", packageItem.getPlanId());

            Intent intent = new Intent(PurchasePlanActivity.this, PaymetWeb.class);
            intent.putExtra("planid", packageItem.getPlanId());
            intent.putExtra("userid", db.getUserData().getUserId());
            startActivity(intent);


//            double mybal = Double.parseDouble(bal);
//            if(mybal < amount){
//                new ToastMsg(PurchasePlanActivity.this).toastIconError(getString(R.string.insufficient_balance));
//
//            }else{
//                updateBalance(amount);
//            }

        }else if (paymentMethodName.equals(PaymentBottomShitDialog.STRIP)) {
            Intent intent = new Intent(PurchasePlanActivity.this, MyRavePayActivity.class);
            intent.putExtra("package", packageItem);
            intent.putExtra("currency", currency);
            startActivity(intent);


        }else if (paymentMethodName.equalsIgnoreCase(PaymentBottomShitDialog.RAZOR_PAY)){
            Intent intent = new Intent(PurchasePlanActivity.this, RazorPayActivity.class);
            intent.putExtra("package", packageItem);
            intent.putExtra("currency", currency);
            startActivity(intent);
        }
    }



    public void saveChargeData() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        Call<ResponseBody> call = paymentApi.savePayment(Config.API_KEY, packageItem.getPlanId(), db.getUserData().getUserId(), packageItem.getPrice(),packageItem.getName(), "Wallet");
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    updateActiveStatus();

                } else {
                    new ToastMsg(PurchasePlanActivity.this).toastIconError(getString(R.string.something_went_wrong));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                new ToastMsg(PurchasePlanActivity.this).toastIconError(getString(R.string.something_went_wrong));
            }
        });


    }



    private void updateActiveStatus() {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        SubscriptionApi subscriptionApi = retrofit.create(SubscriptionApi.class);

        Call<ActiveStatus> call = subscriptionApi.getActiveStatus(Config.API_KEY, PreferenceUtils.getUserId(PurchasePlanActivity.this));
        call.enqueue(new Callback<ActiveStatus>() {
            @Override
            public void onResponse(Call<ActiveStatus> call, Response<ActiveStatus> response) {
                if (response.code() == 200) {
                    ActiveStatus activeStatus = response.body();
                    DatabaseHelper db = new DatabaseHelper(getApplicationContext());
                    db.deleteAllActiveStatusData();
                    db.insertActiveStatusData(activeStatus);
                    new ToastMsg(PurchasePlanActivity.this).toastIconSuccess(getResources().getString(R.string.payment_success));
                    finish();
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ActiveStatus> call, Throwable t) {
                t.printStackTrace();
                new ToastMsg(PurchasePlanActivity.this).toastIconError(getString(R.string.something_went_wrong));
                finish();
                progressBar.setVisibility(View.GONE);
            }
        });

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

                    bal = response.body().getBalance();

//                    txtbalance.setText("My Balance: ₦" + bal );

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


    private void updateBalance(int amount) {
    Retrofit retrofit = RetrofitClient.getRetrofitInstance();
    ProfileApi api = retrofit.create(ProfileApi.class);
    Call<ResponseStatus> call = api.updateBalanceMinus(Config.API_KEY, db.getUserData().getUserId(), String.valueOf(amount));
        call.enqueue(new Callback<ResponseStatus>() {
        @Override
        public void onResponse(Call<ResponseStatus> call, retrofit2.Response<ResponseStatus> response) {
            if (response.code() == 200) {
                if (response.body().getStatus().equalsIgnoreCase("success")) {
                    new ToastMsg(getApplicationContext()).toastIconSuccess(response.body().getData());
//                    mybalance(db.getUserData().getUserId());

//                    updateActiveStatus();
                    saveChargeData();
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





}
