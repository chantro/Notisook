package com.example.fdea.login;

import com.example.fdea.data.IdToken;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

//api 명세
public interface TokenApiInterface {
    //call 객체로 HTTP POST 요청 보냄
    //Body에 있는 데이터를 IdToken 클래스에 있는 SerializedName의  Key와 매칭
    @POST("/verifyToken")
    Call<IdToken> postIdToken(@Body IdToken idToken);
}