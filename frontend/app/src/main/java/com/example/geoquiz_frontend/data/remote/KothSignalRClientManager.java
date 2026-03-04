package com.example.geoquiz_frontend.data.remote;

import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerJoinedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerLeftData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;

public class KothSignalRClientManager {
    private static final String TAG = "KothSignalRClient";
    private static final String HUB_URL = "http://192.168.100.49:5238/kothHub";

    private static KothSignalRClientManager instance;
    private HubConnection hubConnection;
    private String jwtToken;
    private boolean isConnecting = false;

    private final ConcurrentHashMap<String, KothConnectionListener> listeners = new ConcurrentHashMap<>();

    private String currentLobbyId;
    private String currentMatchId;
    private String currentUserId;

    public interface KothConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);

        void onPlayerJoined(PlayerJoinedData data);
        void onPlayerLeft(PlayerLeftData data);
        void onLobbyCountdown(int secondsRemaining);
        void onLobbyCountdownCancelled();
    }

    private KothSignalRClientManager() {}

    public static synchronized KothSignalRClientManager getInstance() {
        if (instance == null) {
            instance = new KothSignalRClientManager();
        }
        return instance;
    }

    public void init(String token, String userId) {
        this.jwtToken = token;
        this.currentUserId = userId;
        buildConnection();
    }

    private void buildConnection() {
        if (hubConnection != null) {
            return;
        }

        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();

        hubConnection = HubConnectionBuilder.create(HUB_URL)
                .withAccessTokenProvider(Completable.complete().toSingleDefault(jwtToken))
                .build();

        registerHandlers();
    }

    private void registerHandlers() {
        hubConnection.on("PlayerJoined", (data) -> {
            Log.d(TAG, "PlayerJoined received");
            notifyListeners(listener -> listener.onPlayerJoined(data));
        }, PlayerJoinedData.class);

        hubConnection.on("PlayerLeft", (data) -> {
            Log.d(TAG, "PlayerLeft received");
            notifyListeners(listener -> listener.onPlayerLeft(data));
        }, PlayerLeftData.class);

        hubConnection.on("LobbyCountdown", (secondsRemaining) -> {
            Log.d(TAG, "LobbyCountdown received: " + secondsRemaining + "s");
            notifyListeners(listener -> listener.onLobbyCountdown(secondsRemaining));
        }, Integer.class);

        hubConnection.on("LobbyCountdownCancelled", () -> {
            Log.d(TAG, "LobbyCountdownCancelled received");
            notifyListeners(listener -> listener.onLobbyCountdownCancelled());
        });

        hubConnection.onClosed(error -> {
            Log.e(TAG, "Connection closed: " + error);
            notifyListeners(listener -> listener.onDisconnected());
        });
    }

    private interface ListenerAction {
        void execute(KothConnectionListener listener);
    }

    private void notifyListeners(ListenerAction action) {
        for (KothConnectionListener listener : listeners.values()) {
            if (listener != null) {
                action.execute(listener);
            }
        }
    }

    public void addListener(String key, KothConnectionListener listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }

    public void start() {
        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED && !isConnecting) {
            isConnecting = true;
            hubConnection.start().doOnComplete(() -> {
                Log.d(TAG, "Connected to KOTH SignalR hub");
                isConnecting = false;
                notifyListeners(listener -> listener.onConnected());
            }).doOnError(error -> {
                Log.e(TAG, "Failed to connect: " + error);
                isConnecting = false;
                notifyListeners(listener -> listener.onError(error.getMessage()));
            }).subscribe();
        }
    }

    public void stop() {
        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.stop().doOnComplete(() -> {
                Log.d(TAG, "Disconnected from KOTH SignalR hub");
            }).subscribe();
        }
    }

    public void joinLobby() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("JoinLobby");
            Log.d(TAG, "JoinLobby sent");
        }
    }

    public void leaveLobby() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("LeaveLobby");
            Log.d(TAG, "LeaveLobby sent");
        }
    }


    public void leaveMatch(String matchId) {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("LeaveMatch", matchId);
            Log.d(TAG, "LeaveMatch sent for match " + matchId);
            this.currentMatchId = null;
        }
    }

    public void setCurrentLobby(String lobbyId) {
        this.currentLobbyId = lobbyId;
    }

    public void setCurrentMatch(String matchId) {
        this.currentMatchId = matchId;
    }

    public String getCurrentLobbyId() {
        return currentLobbyId;
    }

    public String getCurrentMatchId() {
        return currentMatchId;
    }

    public boolean isConnected() {
        return hubConnection != null &&
                hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }

    public HubConnectionState getConnectionState() {
        return hubConnection != null ? hubConnection.getConnectionState() : HubConnectionState.DISCONNECTED;
    }
}
