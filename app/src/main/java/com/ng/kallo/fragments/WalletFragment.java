package com.ng.kallo.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.flutterwave.raveandroid.RavePayActivity;
import com.flutterwave.raveandroid.RaveUiManager;
import com.flutterwave.raveandroid.rave_java_commons.RaveConstants;
import com.ng.kallo.Config;
import com.ng.kallo.MainActivity;
import com.ng.kallo.R;
import com.ng.kallo.database.DatabaseHelper;
import com.ng.kallo.network.RetrofitClient;
import com.ng.kallo.network.apis.ProfileApi;
import com.ng.kallo.network.apis.WalletApi;
import com.ng.kallo.network.model.ResponseStatus;
import com.ng.kallo.network.model.User;
import com.ng.kallo.network.model.config.PaymentConfig;
import com.ng.kallo.utils.ApiResources;
import com.ng.kallo.utils.NetworkInst;
import com.ng.kallo.utils.ToastMsg;

import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class WalletFragment extends Fragment {

    private ShimmerFrameLayout shimmerFrameLayout;


    private ApiResources apiResources;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CoordinatorLayout coordinatorLayout;
    private TextView tvNoItem;

    private String amount;
    private RelativeLayout adView;

    private MainActivity activity;
    private LinearLayout searchRootLayout;

    private CardView searchBar;
    private ImageView menuIv, searchIv;
    private TextView pageTitle;
    private TextView txtbalance;

    private String bal;
    private DatabaseHelper db;

    private Button btn_fund;




    private static final int HIDE_THRESHOLD = 20;
    private int scrolledDistance = 0;
    private boolean controlsVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        return inflater.inflate(R.layout.fragment_wallet,null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //getActivity().setTitle(getResources().getString(R.string.live_tv));

        initComponent(view);


//        updateBalance("3000");

        pageTitle.setText(getResources().getString(R.string.wallet));

        if (activity.isDark) {
            pageTitle.setTextColor(activity.getResources().getColor(R.color.white));
            searchBar.setCardBackgroundColor(activity.getResources().getColor(R.color.black_window_light));
            menuIv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_menu));
            searchIv.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_search_white));
        }
    }


    private void initComponent(View view) {

        adView=view.findViewById(R.id.adView);
        apiResources=new ApiResources();
//        shimmerFrameLayout=view.findViewById(R.id.shimmer_view_container);
//        shimmerFrameLayout.startShimmer();
        progressBar=view.findViewById(R.id.item_progress_bar);
        swipeRefreshLayout=view.findViewById(R.id.swipe_layout);
        coordinatorLayout=view.findViewById(R.id.coordinator_lyt);
        tvNoItem=view.findViewById(R.id.tv_noitem);

        searchRootLayout    = view.findViewById(R.id.search_root_layout);
        searchBar           = view.findViewById(R.id.search_bar);
        menuIv              = view.findViewById(R.id.bt_menu);
        pageTitle           = view.findViewById(R.id.page_title_tv);
        searchIv           = view.findViewById(R.id.search_iv);
        db = new DatabaseHelper(getContext());

        txtbalance = view.findViewById(R.id.wallet_balance);

        btn_fund = view.findViewById(R.id.fund_wallet);


        btn_fund.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogFundwallet();
            }
        });





        if (new NetworkInst(activity).isNetworkAvailable()){
            mybalance(db.getUserData().getUserId());
        }else {
            tvNoItem.setText(getString(R.string.no_internet));
//            shimmerFrameLayout.stopShimmer();
//            shimmerFrameLayout.setVisibility(View.GONE);
            coordinatorLayout.setVisibility(View.VISIBLE);
        }


        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                coordinatorLayout.setVisibility(View.GONE);
                if (new NetworkInst(activity).isNetworkAvailable()){
                    mybalance(db.getUserData().getUserId());
                }else {
                    tvNoItem.setText(getString(R.string.no_internet));
//                    shimmerFrameLayout.stopShimmer();
//                    shimmerFrameLayout.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    coordinatorLayout.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        menuIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.openDrawer();
            }
        });
        searchIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.goToSearchActivity();
            }
        });

    }


    boolean isSearchBarHide = false;

    private void animateSearchBar(final boolean hide) {
        if (isSearchBarHide && hide || !isSearchBarHide && !hide) return;
        isSearchBarHide = hide;
        int moveY = hide ? -(2 * searchRootLayout.getHeight()) : 0;
        searchRootLayout.animate().translationY(moveY).setStartDelay(100).setDuration(300).start();
    }



    private void showDialogFundwallet() {
        final Dialog dialog = new Dialog(getContext());
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
                   new ToastMsg(activity).toastIconError("Invalid Amount");

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
                progressBar.setVisibility(View.GONE);

                Log.d("myres", String.valueOf(response.body()));
//                shimmerFrameLayout.stopShimmer();
//                shimmerFrameLayout.setVisibility(View.GONE);
                if (response.code() == 200) {
                    assert response.body() != null;

                    bal = response.body().getBalance();

                    txtbalance.setText("My Balance: $" + bal );

                }else{
                    swipeRefreshLayout.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
//                    shimmerFrameLayout.stopShimmer();
//                    shimmerFrameLayout.setVisibility(View.GONE);

                    coordinatorLayout.setVisibility(View.VISIBLE);
                    tvNoItem.setText(getResources().getString(R.string.something_went_text));
                    new ToastMsg(activity).toastIconError("Something went wrong...");
                }

            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
//                shimmerFrameLayout.stopShimmer();
//                shimmerFrameLayout.setVisibility(View.GONE);

                coordinatorLayout.setVisibility(View.VISIBLE);
                tvNoItem.setText(getResources().getString(R.string.something_went_text));

                new ToastMsg(getContext()).toastIconError(getString(R.string.error_toast));

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
                        new ToastMsg(getContext()).toastIconSuccess(response.body().getData());
                        progressBar.setVisibility(View.GONE);
                    } else {
                        new ToastMsg(getContext()).toastIconError(response.body().getData());
                        progressBar.setVisibility(View.GONE);
                    }
                } else {
                    new ToastMsg(getContext()).toastIconError(getString(R.string.something_went_wrong));
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ResponseStatus> call, Throwable t) {
                new ToastMsg(getContext()).toastIconError(getString(R.string.something_went_wrong));
                progressBar.setVisibility(View.GONE);
                t.printStackTrace();
            }
        });
    }




    public void RaveUiManager(double amount){
        progressBar.setVisibility(View.VISIBLE);

        PaymentConfig config = db.getConfigurationData().getPaymentConfig();

        final int random = new Random().nextInt(61) + 20;
        new RaveUiManager(getParentFragment().getActivity()).setAmount(amount)
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
                .shouldDisplayFee(true)
                .setEncryptionKey("FLWSECK_TESTcd060eff8b17")
                .initialize();
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         *  We advise you to do a further verification of transaction's details on your server to be
         *  sure everything checks out before providing service or goods.
         */
        if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null) {
            String message = data.getStringExtra("response");
            if (resultCode == RavePayActivity.RESULT_SUCCESS) {
                updateBalance(amount);
                Toast.makeText(getContext(), "SUCCESS " + message, Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RavePayActivity.RESULT_ERROR) {
                getActivity().finish();
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "ERROR " + message, Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RavePayActivity.RESULT_CANCELLED) {
                getActivity().finish();
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "CANCELLED " + message, Toast.LENGTH_SHORT).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}