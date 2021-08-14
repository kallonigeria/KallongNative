package com.ng.kallo.network.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Wallet {

    @SerializedName("balance")
    @Expose
    private String balance;



    public Wallet() {
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String data) {
        this.balance = data;
    }


    @Override
    public String toString() {
        return "Wallet{" +
                "balance=" + balance;
    }
}
