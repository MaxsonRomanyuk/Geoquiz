package com.example.geoquiz_frontend.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;

public class NotificationManager {
    private Disposable connectionDisposable;
    private static final String TAG = "NotificationManager";
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    private static final long RECONNECT_DELAY_MS = 5000;
    private static final String HUB_URL = "http://192.168.100.40:5238/notificationHub";

    private static NotificationManager instance;
    private HubConnection hubConnection;
    private String jwtToken;
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private String currentUserId;
    private Handler handler = new Handler(Looper.getMainLooper());

    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onAchievementUnlocked(ProfileResponse.AchievementDto data);
        void onConnectionFailed(String reason);
    }
    private final ConcurrentHashMap<String, NotificationManager.ConnectionListener> listeners = new ConcurrentHashMap<>();
    private NotificationManager() {}

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void init(String token, String userId) {
        if (userId.equals("uid")) {
            Log.d(TAG, "Guest user, SignalR disabled");
            return;
        }

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
        if (hubConnection == null) return;
        hubConnection.on("AchievementUnlocked", (data) -> {
            Log.d(TAG, "Achievement unlocked");
            notifyListeners(listener -> listener.onAchievementUnlocked(data));
        }, ProfileResponse.AchievementDto.class);

        hubConnection.onClosed(exception -> {
            Log.d(TAG, "Connection closed: " + (exception != null ? exception.getMessage() : "normal"));
            notifyListeners(ConnectionListener::onDisconnected);

            if (!currentUserId.equals("uid")) {
                scheduleReconnect();
            }
        });
    }
    private interface ListenerAction {
        void execute(NotificationManager.ConnectionListener listener);
    }
    private void notifyListeners(NotificationManager.ListenerAction action) {
        for (ConnectionListener listener : listeners.values()) {
            if (listener != null) {
                try {
                    action.execute(listener);
                } catch (Exception e) {
                    Log.e(TAG, "Error notifying listener", e);
                }
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
        if (hubConnection == null || currentUserId.equals("uid")) {
            return;
        }

        if (hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED && !isConnecting) {
            isConnecting = true;
            Log.d(TAG, "Starting connection to " + HUB_URL);

            connectionDisposable = hubConnection.start()
                    .doOnComplete(() -> {
                        Log.d(TAG, "Connected to notification hub");
                        isConnecting = false;
                        reconnectAttempts = 0;
                        notifyListeners(ConnectionListener::onConnected);
                    })
                    .doOnError(error -> {
                        Log.e(TAG, "Failed to connect: " + error.getMessage());
                        isConnecting = false;

                        String errorMessage = getReadableErrorMessage(error);
                        notifyListeners(listener -> listener.onConnectionFailed(errorMessage));

                        scheduleReconnect();
                    })
                    .subscribe(
                            () -> {
                            },
                            throwable -> {
                                Log.e(TAG, "Unexpected subscribe error: " + throwable);
                                isConnecting = false;
                            }
                    );
        }
    }
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts (" + MAX_RECONNECT_ATTEMPTS + ") spent.");
            notifyListeners(listener ->
                    listener.onConnectionFailed("Server unavailable. Working in offline mode."));
            return;
        }

        reconnectAttempts++;
        long delay = RECONNECT_DELAY_MS * reconnectAttempts;

        Log.d(TAG, "Reconnect attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS +
                " in " + (delay / 1000) + "s");

        handler.postDelayed(() -> {
            if (hubConnection != null &&
                    hubConnection.getConnectionState() == HubConnectionState.DISCONNECTED) {
                start();
            }
        }, delay);
    }
    private String getReadableErrorMessage(Throwable error) {
        if (error instanceof SocketTimeoutException) {
            return "Connection timeout. Server may be unavailable.";
        } else if (error instanceof ConnectException) {
            return "Cannot connect to server. Check your internet.";
        } else if (error instanceof UnknownHostException) {
            return "No internet connection. Working offline.";
        } else {
            return "Connection error: " + error.getMessage();
        }
    }
    public void stop() {
        handler.removeCallbacksAndMessages(null);

        if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
            connectionDisposable.dispose();
            connectionDisposable = null;
        }

        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            reconnectAttempts = 0;

            try {
                hubConnection.stop().blockingAwait();
                Log.d(TAG, "Disconnected from notification hub");
                notifyListeners(ConnectionListener::onDisconnected);
            } catch (Exception e) {
                Log.e(TAG, "Error stopping connection: " + e);
            }
        }
    }
    public boolean isConnected() {
        return hubConnection != null &&
                hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }
}
