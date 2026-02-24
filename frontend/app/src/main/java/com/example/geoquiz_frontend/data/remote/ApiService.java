package com.example.geoquiz_frontend.data.remote;

import com.example.geoquiz_frontend.data.remote.dtos.AuthResponse;
import com.example.geoquiz_frontend.data.remote.dtos.BootstrapResponse;
import com.example.geoquiz_frontend.data.remote.dtos.FinishGameRequest;
import com.example.geoquiz_frontend.data.remote.dtos.LoginRequest;
import com.example.geoquiz_frontend.data.remote.dtos.ProfileResponse;
import com.example.geoquiz_frontend.data.remote.dtos.RegisterRequest;
import com.example.geoquiz_frontend.data.remote.dtos.SyncGameSessionRequest;
import com.example.geoquiz_frontend.data.remote.dtos.TestRequest;
import com.example.geoquiz_frontend.data.remote.dtos.TestResponse;

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

    @GET("api/content/bootstrap")
    Call<BootstrapResponse> bootstrap();

    @POST("api/game/finish")
    Call<Void> finishGame(@Body FinishGameRequest request);

    @POST("api/game/sync")
    Call<Void> syncGames(@Body List<SyncGameSessionRequest> games);
}
