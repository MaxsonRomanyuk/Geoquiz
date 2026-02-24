package com.example.geoquiz_frontend.data.remote;

import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.MatchFoundData;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import io.reactivex.rxjava3.core.Completable;

public class SignalRClient {
    private static final String TAG = "SignalRClient";
    private static final String HUB_URL = "http://192.168.100.49:5238/pvpHub";

    private HubConnection hubConnection;
    private ConnectionListener listener;
    private String jwtToken;

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);
        void onMatchFound(MatchFoundData matchData);
    }

    public SignalRClient(String jwtToken, ConnectionListener listener) {
        this.jwtToken = jwtToken;
        this.listener = listener;
        buildConnection();
    }

    private void buildConnection() {
        hubConnection = HubConnectionBuilder.create(HUB_URL)
                .withAccessTokenProvider(Completable.complete().toSingleDefault(jwtToken))
                .build();

        registerHandlers();
    }

    private void registerHandlers() {
        hubConnection.on("MatchFound", (matchData) -> {
            Log.d(TAG, "MatchFound received");
            if (listener != null) {
                listener.onMatchFound(matchData);
            }
        }, MatchFoundData.class);

        hubConnection.onClosed(error -> {
            Log.e(TAG, "Connection closed: " + error);
            if (listener != null) {
                listener.onDisconnected();
            }
        });
    }

    public void start() {
        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) {
            hubConnection.start().doOnComplete(() -> {
                Log.d(TAG, "Connected to SignalR hub");
                if (listener != null) {
                    listener.onConnected();
                }
            }).doOnError(error -> {
                Log.e(TAG, "Failed to connect: " + error);
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }).subscribe();
        }
    }

    public void stop() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.stop().doOnComplete(() -> {
                Log.d(TAG, "Disconnected from SignalR hub");
            }).subscribe();
        }
    }

    public void joinQueue() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("JoinQueue");
            Log.d(TAG, "JoinQueue sent");
        } else {
            Log.e(TAG, "Cannot join queue: not connected");
        }
    }

    public void leaveQueue() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("LeaveQueue");
            Log.d(TAG, "LeaveQueue sent");
        }
    }

    public boolean isConnected() {
        return hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }
}