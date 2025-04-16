package com.example.fdea.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IdToken {
    @Expose  //null일 경우, json으로 만들 필드를 자동 생략
    @SerializedName("idToken") private String idToken;
    @SerializedName("success") private String success;
    @SerializedName("msg") private String msg;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getSuccess(){
        return success;
    }

    public String getMsg(){
        return msg;
    }
}