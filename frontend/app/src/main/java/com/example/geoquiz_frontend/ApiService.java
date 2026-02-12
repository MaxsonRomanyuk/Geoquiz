package com.example.geoquiz_frontend;

import com.example.geoquiz_frontend.DTOs.AuthResponse;
import com.example.geoquiz_frontend.DTOs.LoginRequest;
import com.example.geoquiz_frontend.DTOs.ProfileResponse;
import com.example.geoquiz_frontend.DTOs.RegisterRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
public interface ApiService {
    @POST("api/test/echo")
    Call<TestResponse> echo(@Body TestRequest request);
    @POST("api/auth/register")
    Call<Void> register(@Body RegisterRequest request);
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);
    @GET("api/profile/me")
    Call<ProfileResponse> getProfile();
}
