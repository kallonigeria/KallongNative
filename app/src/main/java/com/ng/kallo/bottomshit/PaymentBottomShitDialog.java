package com.ng.kallo.bottomshit;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Space;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.ng.kallo.R;
import com.ng.kallo.database.DatabaseHelper;
import com.ng.kallo.network.model.config.PaymentConfig;

public class PaymentBottomShitDialog extends BottomSheetDialogFragment {

    public static final String PAYPAL = "paypal";
    public static final String STRIP = "strip";
    public static final String WALLET = "wallet";
    public static final String USSD = "ussd";
    public static final String RAZOR_PAY = "razorpay";
    private DatabaseHelper databaseHelper;

    private OnBottomShitClickListener bottomShitClickListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_payment_bottom_shit, container,
                false);
        databaseHelper = new DatabaseHelper(getContext());
        PaymentConfig config = databaseHelper.getConfigurationData().getPaymentConfig();
        CardView paypalBt, stripBt, razorpayBt, walletBt, ussd;
        paypalBt = view.findViewById(R.id.razorpay_btn);
        ussd = view.findViewById(R.id.ussd_btn);
//        walletBt = view.findViewById(R.id.wallet_btn);
        stripBt = view.findViewById(R.id.stripe_btn);
        razorpayBt = view.findViewById(R.id.razorpay_btn);
        Space space = view.findViewById(R.id.space2);

        if (!config.getPaypalEnable()) {
            paypalBt.setVisibility(View.GONE);
            space.setVisibility(View.GONE);
        }

        if (!config.getStripeEnable()) {
            stripBt.setVisibility(View.GONE);
            space.setVisibility(View.GONE);
        }



        if (!config.getRazorpayEnable())
            razorpayBt.setVisibility(View.GONE);

        paypalBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShitClickListener.onBottomShitClick(PAYPAL);
            }
        });


//        walletBt.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                bottomShitClickListener.onBottomShitClick(WALLET);
//            }
//        });

        ussd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShitClickListener.onBottomShitClick(USSD);
            }
        });

        stripBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomShitClickListener.onBottomShitClick(STRIP);
            }
        });

        razorpayBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomShitClickListener.onBottomShitClick(RAZOR_PAY);
            }
        });


        return view;

    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            bottomShitClickListener = (OnBottomShitClickListener) context;
        } catch (Exception e) {
            throw new ClassCastException(context.toString() + " must be implemented");
        }

    }

    public interface OnBottomShitClickListener {
        void onBottomShitClick(String paymentMethodName);
    }

}

