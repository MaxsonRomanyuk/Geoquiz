package com.example.geoquiz_frontend.data.remote;

import com.example.geoquiz_frontend.data.remote.dtos.auth.AuthResponse;
import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.data.remote.dtos.solo.FinishGameRequest;
import com.example.geoquiz_frontend.data.remote.dtos.auth.LoginRequest;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.remote.dtos.auth.RegisterRequest;
import com.example.geoquiz_frontend.data.remote.dtos.solo.SyncGameSessionRequest;
import com.example.geoquiz_frontend.domain.entities.UserStats;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
public interface ApiService {
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
