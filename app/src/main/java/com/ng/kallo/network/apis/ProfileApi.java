package com.ng.kallo.network.apis;

import android.net.Uri;

import com.ng.kallo.network.model.ResponseStatus;

import retrofit2.Call;
import retrofit2.http.Field;

import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ProfileApi {
    @FormUrlEncoded
    @POST("update_profile")
    Call<ResponseStatus> updateProfile(@Header("API-KEY") String apiKey,
                                       @Field("id") String id,
                                       @Field("name") String name,
                                       @Field("email") String email,
                                       @Field("phone") String phone,
                                       @Field("password") String password,
                                       @Field("photo") Uri imageUri,
                                       @Field("gender") String gender);



    @FormUrlEncoded
    @POST("update_profile_wallet")
    Call<ResponseStatus> updateBalance(@Header("API-KEY") String apiKey,
                                       @Field("id") String id,
                                       @Field("amount") String amount);
    @FormUrlEncoded
    @POST("update_profile_wallet_minus")
    Call<ResponseStatus> updateBalanceMinus(@Header("API-KEY") String apiKey,
                                       @Field("id") String id,
                                       @Field("amount") String amount);

}
