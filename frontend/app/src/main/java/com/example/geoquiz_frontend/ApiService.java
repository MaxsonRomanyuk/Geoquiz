package com.example.geoquiz_frontend;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
public interface ApiService {
    @POST("api/test/echo")
    Call<TestResponse> echo(@Body TestRequest request);

}
