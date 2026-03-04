package com.example.geoquiz_frontend.data.remote;

import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.pvp.DisconnectData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.DraftUpdateData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameReadyData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.MatchFoundData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.QuestionResultData;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.SubmitAnswerRequest;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.TimerUpdateData;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;

public class PvPSignalRClientManager {
    private static final String TAG = "SignalRClientManager";
    private static final String HUB_URL = "http://192.168.100.49:5238/pvpHub";

    private static PvPSignalRClientManager instance;
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
        void onGameReady(GameReadyData gameData);
        void onQuestionResult(QuestionResultData resultData);
        void onTimerUpdate(TimerUpdateData timerData);
        void onGameFinished(GameFinishedData finishData);
        void onOpponentDisconnected(DisconnectData disconnectData);
    }

    private PvPSignalRClientManager() {}

    public static synchronized PvPSignalRClientManager getInstance() {
        if (instance == null) {
            instance = new PvPSignalRClientManager();
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

        hubConnection.on("GameReady", (gameData) -> {
            Log.d(TAG, "GameReady received");
            notifyListeners(listener -> listener.onGameReady(gameData));
        }, GameReadyData.class);

        hubConnection.on("QuestionResult", (resultData) -> {
            Log.d(TAG, "QuestionResult received for question " + resultData.getQuestionNumber());
            notifyListeners(listener -> listener.onQuestionResult(resultData));
        }, QuestionResultData.class);

        hubConnection.on("TimerUpdate", (timerData) -> {
            Log.d(TAG, "TimerUpdate received: " + timerData.getRemainingTimeSeconds() + "s");
            notifyListeners(listener -> listener.onTimerUpdate(timerData));
        }, TimerUpdateData.class);

        hubConnection.on("GameFinished", (finishData) -> {
            Log.d(TAG, "GameFinished received");
            notifyListeners(listener -> listener.onGameFinished(finishData));
        }, GameFinishedData.class);

        hubConnection.on("OpponentDisconnected", (disconnectData) -> {
            Log.d(TAG, "OpponentDisconnected received");
            notifyListeners(listener -> listener.onOpponentDisconnected(disconnectData));
        }, DisconnectData.class);
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
    public void submitAnswer(String matchId, String questionId, int selectedIndex,
                             int timeSpentMs, int questionNumber) {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            SubmitAnswerRequest request = new SubmitAnswerRequest(
                    matchId, questionId, selectedIndex, timeSpentMs, questionNumber);
            hubConnection.send("SubmitAnswer", request);
            Log.d(TAG, "SubmitAnswer sent for question " + questionNumber);
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