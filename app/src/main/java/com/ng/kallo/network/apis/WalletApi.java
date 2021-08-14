package com.ng.kallo.network.apis;

import com.ng.kallo.network.model.User;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WalletApi {

    @GET("user_details_by_user_id")
    Call<User> postWalletStatus(@Header("API-KEY") String apiKey,
                                @Query("id") String userid);
}