package com.example.fdea.login;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;

    public static Retrofit getRetrofit(){
        if(retrofit==null){
            Retrofit.Builder builder = new Retrofit.Builder();
            builder.baseUrl("http://52.78.93.139:5000/");
            //builder.baseUrl("http://localhost:3000/");
            builder.addConverterFactory(GsonConverterFactory.create());

            retrofit = builder.build();
        }
        return retrofit;
    }
}