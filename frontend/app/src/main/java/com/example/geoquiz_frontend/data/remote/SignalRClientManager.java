package com.example.geoquiz_frontend.data.remote;

import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.BanModeRequest;
import com.example.geoquiz_frontend.data.remote.dtos.DraftUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.MatchFoundData;
import com.google.gson.Gson;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;

public class SignalRClientManager {
    private static final String TAG = "SignalRClientManager";
    private static final String HUB_URL = "http://192.168.100.49:5238/pvpHub";

    private static SignalRClientManager instance;
    private HubConnection hubConnection;
    private String jwtToken;
    private boolean isConnecting = false;

    private final ConcurrentHashMap<String, ConnectionListener> listeners = new ConcurrentHashMap<>();

    private String currentMatchId;
    private String currentUserId;

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onError(String error);
        void onMatchFound(MatchFoundData matchData);
        void onDraftUpdated(DraftUpdateData draftData);
    }

    private SignalRClientManager() {}

    public static synchronized SignalRClientManager getInstance() {
        if (instance == null) {
            instance = new SignalRClientManager();
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

        hubConnection = HubConnectionBuilder.create(HUB_URL)
                .withAccessTokenProvider(Completable.complete().toSingleDefault(jwtToken))
                .build();

        registerHandlers();
    }

    private void registerHandlers() {
        hubConnection.on("MatchFound", (matchData) -> {
            Log.d(TAG, "MatchFound received");
            notifyListeners(listener -> listener.onMatchFound(matchData));
        }, MatchFoundData.class);

        hubConnection.on("DraftUpdated", (draftData) -> {
            Log.d(TAG, "DraftUpdated received");
            notifyListeners(listener -> listener.onDraftUpdated(draftData));
        }, DraftUpdateData.class);

        hubConnection.onClosed(error -> {
            Log.e(TAG, "Connection closed: " + error);
            notifyListeners(listener -> listener.onDisconnected());
        });
    }

    private interface ListenerAction {
        void execute(ConnectionListener listener);
    }

    private void notifyListeners(ListenerAction action) {
        for (ConnectionListener listener : listeners.values()) {
            if (listener != null) {
                action.execute(listener);
            }
        }
    }
    public void addListener(String key, ConnectionListener listener) {
        listeners.put(key, listener);
    }

    public void removeListener(String key) {
        listeners.remove(key);
    }
    public void start() {
        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED && !isConnecting) {
            isConnecting = true;
            hubConnection.start().doOnComplete(() -> {
                Log.d(TAG, "Connected to SignalR hub");
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
                Log.d(TAG, "Disconnected from SignalR hub");
            }).subscribe();
        }
    }

    public void joinQueue() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("JoinQueue");
            Log.d(TAG, "JoinQueue sent");
        }
    }

    public void leaveQueue() {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("LeaveQueue");
        }
    }

    public void banMode(String matchId, String mode, String language) {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            int modeValue = convertModeToInt(mode);
            int langValue = language.equals("ru") ? 0 : 1;

            Log.d(TAG, "Sending BanMode with params: matchId=" + matchId +
                    ", mode=" + modeValue + ", lang=" + langValue);

            hubConnection.send("BanMode", matchId, modeValue, langValue);
            Log.d(TAG, "BanMode sent: " + mode + " for match " + matchId);
        }
    }
    private int convertModeToInt(String mode) {
        switch (mode) {
            case "capitals": return 1;
            case "flags": return 2;
            case "outlines": return 3;
            case "languages": return 4;
            default: return 1;
        }
    }
    public void setCurrentMatch(String matchId) {
        this.currentMatchId = matchId;
        Log.d(TAG, "Current match set to: " + matchId);
    }

    public boolean isConnected() {
        return hubConnection != null &&
                hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }
}