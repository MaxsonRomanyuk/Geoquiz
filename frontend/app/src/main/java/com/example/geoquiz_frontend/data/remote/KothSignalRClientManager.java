package com.example.geoquiz_frontend.data.remote;

import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.koth.AnswerResultData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.LobbyInitialStateData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.MatchFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.MatchResumeData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.MatchStartedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerEliminatedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerJoinedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerLeftData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerLobby;
import com.example.geoquiz_frontend.data.remote.dtos.koth.RoundFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.RoundStartedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.SubmitAnswerRequest;
import com.example.geoquiz_frontend.data.remote.dtos.pvp.GameResumeData;
import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;

public class KothSignalRClientManager {
    private static final String TAG = "KothSignalRClient";
    private static final String HUB_URL = "http://192.168.100.49:5238/kothHub";
    private Disposable connectionDisposable;
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

        void onPlayerJoinedToOthers(PlayerJoinedData data);
        void onPlayerAboutLobby(LobbyInitialStateData data);
        void onPlayerLeft(PlayerLeftData data);
        void onLobbyCountdown(int secondsRemaining);
        void onLobbyCountdownCancelled();

        void onMatchStarted(MatchStartedData data);
        void onMatchResume(MatchResumeData data);
        void onRoundStarted(RoundStartedData data);
        void onRoundFinished(RoundFinishedData data);
        void onPlayerEliminated(PlayerEliminatedData data);
        void onAnswerResult(AnswerResultData data);
        void onMatchFinished(MatchFinishedData data);
        void onForceDisconnect(LocalizedText message);
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

        hubConnection = HubConnectionBuilder.create(HUB_URL)
                .withAccessTokenProvider(Completable.complete().toSingleDefault(jwtToken))
                .build();

        registerHandlers();
    }
    private String formatPlayers(List<PlayerLobby> players) {
        if (players == null) return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < players.size(); i++) {
            PlayerLobby p = players.get(i);
            if (i > 0) sb.append(", ");
            sb.append(String.format("{id:%s, name:%s, level:%d}",
                    p.getId(), p.getName(), p.getLevel()));
        }
        sb.append("]");
        return sb.toString();
    }
    private void registerHandlers() {
        hubConnection.on("PlayerJoinedToOthers", (data) -> {
            Log.d(TAG, "PlayerJoined received");
            Log.d(TAG, String.format("PlayerJoined received - LobbyId: %s, PlayerId: %s, PlayerName: %s, Level: %d, TotalPlayers: %d",
                    data.getLobbyId(), data.getPlayerId(), data.getPlayerName(),
                    data.getPlayerLevel(), data.getTotalPlayers()));
            notifyListeners(listener -> listener.onPlayerJoinedToOthers(data));
        }, PlayerJoinedData.class);

        hubConnection.on("PlayerAboutLobby", (data) -> {
            Log.d(TAG, "PlayerAbout received");
            Log.d(TAG, String.format("PlayerAbout received - LobbyId: %s, TotalPlayers: %d, Players: %s",
                    data.getLobbyId(), data.getTotalPlayers(), formatPlayers(data.getPlayers())));
            notifyListeners(listener -> listener.onPlayerAboutLobby(data));
        }, LobbyInitialStateData.class);

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

        hubConnection.on("MatchStarted", (data) -> {
            Log.d(TAG, "MatchStarted received: " + data.getMatchId());
            notifyListeners(listener -> listener.onMatchStarted(data));
        }, MatchStartedData.class);

        hubConnection.on("MatchResume", (gameData) -> {
            Log.d(TAG, "MatchResume received");
            notifyListeners(listener -> listener.onMatchResume(gameData));
        }, MatchResumeData.class);

        hubConnection.on("RoundStarted", (data) -> {
            Log.d(TAG, "RoundStarted received: Round " + data.getRoundNumber());
            notifyListeners(listener -> listener.onRoundStarted(data));
        }, RoundStartedData.class);

        hubConnection.on("RoundFinished", (data) -> {
            Log.d(TAG, "RoundFinished received: " + data.getEliminatedPlayerIds().size() + " eliminated");
            notifyListeners(listener -> listener.onRoundFinished(data));
        }, RoundFinishedData.class);

        hubConnection.on("PlayerEliminated", (data) -> {
            Log.d(TAG, "PlayerEliminated received for player: " + data.getPlayerId());
            notifyListeners(listener -> listener.onPlayerEliminated(data));
        }, PlayerEliminatedData.class);

        hubConnection.on("AnswerResult", (data) -> {
            Log.d(TAG, "AnswerResult received, correct: " + data.isCorrect());
            notifyListeners(listener -> listener.onAnswerResult(data));
        }, AnswerResultData.class);

        hubConnection.on("MatchFinished", (data) -> {
            Log.d(TAG, "MatchFinished received");
            notifyListeners(listener -> listener.onMatchFinished(data));
        }, MatchFinishedData.class);

        hubConnection.onClosed(error -> {
            Log.e(TAG, "Connection closed: " + error);
            notifyListeners(listener -> listener.onDisconnected());
        });
        hubConnection.on("ForceDisconnect", (message) -> {
            Log.d(TAG, "ForceDisconnect received");
            notifyListeners(listener -> listener.onForceDisconnect(message));
        }, LocalizedText.class);
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
            connectionDisposable = hubConnection.start().doOnComplete(() -> {
                Log.d(TAG, "Connected to KOTH SignalR hub");
                isConnecting = false;
                notifyListeners(listener -> listener.onConnected());
            }).doOnError(error -> {
                Log.e(TAG, "Failed to connect: " + error);
                isConnecting = false;
                notifyListeners(listener -> listener.onError(error.getMessage()));
            }).subscribe(
                    () -> {
                    },
                    throwable -> {
                        Log.e(TAG, "Unexpected subscribe error: " + throwable);
                        isConnecting = false;
                    }
            );
        }
    }

    public void stop() {
        if (connectionDisposable != null && !connectionDisposable.isDisposed()) {
            connectionDisposable.dispose();
            connectionDisposable = null;
        }
        if (hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            try {
                hubConnection.stop().blockingAwait();
                Log.d(TAG, "Disconnected from KOTH SignalR hub");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping connection: " + e);
            }
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
    public void joinMatch(String matchId) {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("JoinMatch", matchId);
            Log.d(TAG, "JoinMatch sent for match " + matchId);
            this.currentMatchId = matchId;
        }
    }
    public void playerReadyForGame(String matchId)
    {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("PlayerReadyForGame", matchId);
        }
    }
    public void leaveMatch(String matchId) {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            hubConnection.send("LeaveMatch", matchId);
            Log.d(TAG, "LeaveMatch sent for match " + matchId);
            this.currentMatchId = null;
        }
    }
    public void submitAnswer(String matchId, int roundNumber, String questionId, int selectedOptionIndex, int timeSpentMs) {
        if (hubConnection.getConnectionState() == HubConnectionState.CONNECTED) {
            SubmitAnswerRequest request = new SubmitAnswerRequest(
                    matchId, roundNumber, questionId, selectedOptionIndex, timeSpentMs
            );
            hubConnection.send("SubmitAnswer", request);
            Log.d(TAG, "SubmitAnswer sent for round " + roundNumber);
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
        return hubConnection != null && hubConnection.getConnectionState() == HubConnectionState.CONNECTED;
    }
    public void reset() {
        Log.d(TAG, "Resetting NotificationManager");
        hubConnection = null;
        jwtToken = null;
        currentUserId = null;
        isConnecting = false;
    }
}
