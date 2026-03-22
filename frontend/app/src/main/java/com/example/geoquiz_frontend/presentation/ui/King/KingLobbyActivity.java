package com.example.geoquiz_frontend.presentation.ui.King;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.gridlayout.widget.GridLayout;

import com.example.geoquiz_frontend.presentation.ui.Home.MainActivity;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.remote.KothSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.koth.AnswerResultData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.LobbyInitialStateData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.MatchFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.MatchStartedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerEliminatedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerJoinedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerLeftData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.PlayerLobby;
import com.example.geoquiz_frontend.data.remote.dtos.koth.RoundFinishedData;
import com.example.geoquiz_frontend.data.remote.dtos.koth.RoundStartedData;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.geoquiz_frontend.presentation.ui.Base.BaseActivity;

public class KingLobbyActivity extends BaseActivity {
    private static final String TAG = "KingLobbyActivity";

    private ImageView ivClose;
    private TextView tvPlayerCount, tvTimer;
    private GridLayout gridPlayers;
    private LinearLayout layoutTimer;
    private MaterialButton btnLeave;

    private Handler timerHandler = new Handler();
    private boolean isCountdownActive = false;

    private int currentPlayers = 0;
    private final int MAX_PLAYERS = 32;
    private final int TOTAL_SLOTS = 32;
    private String playerId;
    private String playerName;
    private int playerLevel;

    private List<PlayerSlot> playerSlots = new ArrayList<>();
    private Map<String, PlayerSlot> playerSlotMap = new HashMap<>();
    private PreferencesHelper preferencesHelper;
    private DatabaseHelper databaseHelper;
    private String language;
    private String activityId;

    private KothSignalRClientManager signalRClient;
    private String currentLobbyId;

    private class PlayerSlot {
        int position;
        boolean isOccupied;
        String playerId;
        String playerName;
        int playerLevel;
        View slotView;

        PlayerSlot(int position) {
            this.position = position;
            this.isOccupied = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_king_lobby);

        preferencesHelper = new PreferencesHelper(this);
        databaseHelper = new DatabaseHelper(this);
        activityId = "koth_lobby_" + System.currentTimeMillis();
        language = preferencesHelper.getLanguage();


        initViews();
        setupClickListeners();
        loadPlayerData();
        createPlayerSlots();
        updatePlayerCount();

        initSignalR();
    }

    private void initViews() {
        ivClose = findViewById(R.id.ivClose);
        tvPlayerCount = findViewById(R.id.tvPlayerCount);
        tvTimer = findViewById(R.id.tvTimer);
        gridPlayers = findViewById(R.id.gridPlayers);
        layoutTimer = findViewById(R.id.layoutTimer);
        btnLeave = findViewById(R.id.btn_leave);
    }

    private void setupClickListeners() {
        ivClose.setOnClickListener(v -> cancelSearchAndExit());
        btnLeave.setOnClickListener(v -> cancelSearchAndExit());
    }

    private void loadPlayerData() {
        playerId = preferencesHelper.getUserId();
        playerName = preferencesHelper.getUserName();
        if (playerName == null || playerName.isEmpty()) {
            playerName = "Player";
        }

        UserStats userStats = databaseHelper.getUserStats(preferencesHelper.getUserId());
        playerLevel = userStats.getLevel();
    }

    private void createPlayerSlots() {
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < TOTAL_SLOTS; i++) {
            View slotView = inflater.inflate(R.layout.item_player_slot, gridPlayers, false);

            PlayerSlot slot = new PlayerSlot(i);
            slot.slotView = slotView;
            playerSlots.add(slot);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(i % 4, 1f);
            params.rowSpec = GridLayout.spec(i / 4);
            params.setMargins(10, 10, 10, 10);

            slotView.setAlpha(0.5f);

            gridPlayers.addView(slotView, params);

            updateSlotDisplay(slot, false, null, 0, null);
        }
    }

    private void updateSlotDisplay(PlayerSlot slot, boolean occupied, String name, int level, String id) {
        View slotView = slot.slotView;

        MaterialCardView card = slotView.findViewById(R.id.cardPlayerSlot);
        TextView tvIcon = slotView.findViewById(R.id.tvIcon);
        TextView tvPlayerName = slotView.findViewById(R.id.tvPlayerName);
        TextView tvPlayerLevel = slotView.findViewById(R.id.tvPlayerLevel);

        if (occupied && name != null) {
            tvIcon.setText(getPlayerIcon(name));
            tvPlayerName.setText(truncateName(name));
            tvPlayerLevel.setText(getString(R.string.level_prefix) + " " + level);

            if (id != null && id.equals(playerId)) {
                card.setStrokeColor(getColorFromAttr(R.attr.colorPrimary));
                card.setStrokeWidth(2);
                tvPlayerName.setTextColor(getColorFromAttr(R.attr.colorPrimary));
                slotView.setAlpha(1f);
            } else {
                card.setStrokeColor(getColorFromAttr(R.attr.colorTertiaryFixed));
                card.setStrokeWidth(1);
                tvPlayerName.setTextColor(getColorFromAttr(R.attr.colorTertiary));
                slotView.setAlpha(0.8f);
            }

            slot.isOccupied = true;
            slot.playerId = id;
            slot.playerName = name;
            slot.playerLevel = level;

            if (id != null) {
                playerSlotMap.put(id, slot);
            }
        } else {
            tvIcon.setText("👤");
            tvPlayerName.setText(getString(R.string.slot) + " " + (slot.position + 1));
            tvPlayerLevel.setText(getString(R.string.wait_players));

            card.setStrokeColor(getColorFromAttr(R.attr.colorTertiaryFixed));
            card.setStrokeWidth(1);
            tvPlayerName.setTextColor(getColorFromAttr(R.attr.colorTertiaryFixed));

            slot.isOccupied = false;
            slot.playerId = null;
            slot.playerName = null;
            slot.playerLevel = 0;
        }
    }

    private String truncateName(String name) {
        if (name == null) return "";
        if (name.length() <= 8) return name;
        return name.substring(0, 7) + "…";
    }

    private String getPlayerIcon(String name) {
        if (name == null || name.isEmpty()) return "👤";
        if (name.length() >= 2) {
            return name.substring(0, 2).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }

    private void updatePlayerCount() {
        currentPlayers = 0;
        for (PlayerSlot slot : playerSlots) {
            if (slot.isOccupied) {
                currentPlayers++;
            }
        }

        tvPlayerCount.setText(currentPlayers + "/" + MAX_PLAYERS);
    }

    private int findEmptySlot() {
        for (int i = 0; i < playerSlots.size(); i++) {
            if (!playerSlots.get(i).isOccupied) {
                return i;
            }
        }
        return -1;
    }

    private void initSignalR() {
        String token = preferencesHelper.getAuthToken();
        if (token == null || token.isEmpty()) {
            finish();
            return;
        }

        signalRClient = KothSignalRClientManager.getInstance();
        signalRClient.init(token, playerId);
        signalRClient.removeListener(activityId);
        connectToSignalR();
    }
    private void connectToSignalR() {
        signalRClient.addListener(activityId, new KothSignalRClientManager.KothConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Connected to KOTH, joining lobby...");
                    showStatus(getString(R.string.wait_players));
                    signalRClient.joinLobby();
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Disconnected from KOTH");
                    showStatus(getString(R.string.connection_lost));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "KOTH Error: " + error);
                    showStatus(getString(R.string.error) + ": " + error);
                });
            }

            @Override
            public void onPlayerJoinedToOthers(PlayerJoinedData data) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Player joined: " + data.getPlayerId());

                    if (currentLobbyId == null) {
                        currentLobbyId = data.getLobbyId();
                        signalRClient.setCurrentLobby(currentLobbyId);
                    }

                    int slotIndex = findEmptySlot();
                    if (slotIndex >= 0) {
                        PlayerSlot slot = playerSlots.get(slotIndex);


                        updateSlotDisplay(slot, true, data.getPlayerName(), data.getPlayerLevel(), data.getPlayerId());
                        updatePlayerCount();
                    }
                });
            }
            @Override
            public void onPlayerAboutLobby(LobbyInitialStateData data) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Player joined to lobby: " + data.getLobbyId());

                    if (currentLobbyId == null) {
                        currentLobbyId = data.getLobbyId();
                        signalRClient.setCurrentLobby(currentLobbyId);
                    }

                    List<PlayerLobby> dataPlayers = data.getPlayers();
                    for (PlayerLobby player : dataPlayers)
                    {
                        int slotIndex = findEmptySlot();
                        if (slotIndex >= 0) {
                            PlayerSlot slot = playerSlots.get(slotIndex);

                            updateSlotDisplay(slot, true, player.getName(), player.getLevel(), player.getId());
                            updatePlayerCount();
                        }
                    }
                });
            }
            @Override
            public void onPlayerLeft(PlayerLeftData data) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Player left: " + data.getPlayerId());

                    for (PlayerSlot slot : playerSlots) {
                        if (data.getPlayerId().equals(slot.playerId)) {
                            updateSlotDisplay(slot, false, null, 0, null);
                            break;
                        }
                    }

                    updatePlayerCount();
                });
            }

            @Override
            public void onLobbyCountdown(int secondsRemaining) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Countdown from server: " + secondsRemaining + "s");
                    layoutTimer.setVisibility(View.VISIBLE);
                    isCountdownActive = true;

                    tvTimer.setText(String.format(Locale.getDefault(), "00:%02d", secondsRemaining));

                    if (secondsRemaining <= 1) {
                        //
                    }
                });
            }

            @Override
            public void onLobbyCountdownCancelled() {
                runOnUiThread(() -> {
                    Log.d(TAG, "Countdown cancelled");
                    stopSearch();
                    isCountdownActive = false;
                });
            }
            @Override
            public void onMatchStarted(MatchStartedData data) {
                signalRClient.joinMatch(data.getMatchId());
                runOnUiThread(() -> handleMatchStarted(data));
            }
            @Override
            public void onRoundStarted(RoundStartedData data) {
            }
            @Override
            public void onRoundFinished(RoundFinishedData data) {
            }
            @Override
            public void onPlayerEliminated(PlayerEliminatedData data) {
            }
            @Override
            public void onAnswerResult(AnswerResultData data) {
            }
            @Override
            public void onMatchFinished(MatchFinishedData data) {
            }
        });

        signalRClient.start();
    }
    private void handleMatchStarted(MatchStartedData data) {
        Log.d(TAG, "Match started! Total players: " + data.getTotalPlayers() + ", rounds: " + data.getTotalRounds());

        Intent intent = new Intent(this, KingGameActivity.class);
        intent.putExtra("match_id", data.getMatchId());
        intent.putExtra("total_players", data.getTotalPlayers());
        intent.putExtra("total_rounds", data.getTotalRounds());
        intent.putExtra("all_players", new ArrayList<>(data.getAllPlayers()));
        startActivity(intent);
        finish();
    }
    private void startSearch() {
        layoutTimer.setVisibility(View.VISIBLE);
        tvTimer.setText("00:00");
    }

    private void stopSearch() {
        layoutTimer.setVisibility(View.GONE);
        tvTimer.setText("00:00");
    }

    private void showStatus(String status) {
        tvTimer.setText(status);
    }
    private void cancelSearchAndExit() {
        if (signalRClient != null && signalRClient.isConnected()) {
            if (currentLobbyId != null) {
                signalRClient.leaveLobby();
            }
            signalRClient.stop();
        }

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private int getColorFromAttr(int attrResId) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        getTheme().resolveAttribute(attrResId, typedValue, true);
        return typedValue.data;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (signalRClient != null) {
            signalRClient.removeListener(activityId);
        }
    }

    @Override
    public void onBackPressed() {
        cancelSearchAndExit();
    }
}