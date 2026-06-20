package com.example.geoquiz_frontend.presentation.ui.king;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.remote.dtos.question.OptionData;
import com.example.geoquiz_frontend.data.remote.dtos.question.QuestionData;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.presentation.ui.base.BaseActivity;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.data.remote.KothSignalRClientManager;
import com.example.geoquiz_frontend.data.remote.dtos.koth.*;
import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.example.geoquiz_frontend.domain.enums.RoundType;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KingGameActivity extends BaseActivity {

    private static final String TAG = "KingGameActivity";

    private TextView tvPlayersRemaining, tvTotalPlayers;
    private TextView tvQuestionNumber, tvRoundTypeIcon, tvRoundType, tvTimer, tvScore, tvQuestionTitle, tvAudioHint;
    private LinearLayout layoutQuestionInfo;
    private FrameLayout imageContainer;
    private LinearLayout audioContainer, layoutWave;
    private ImageView ivQuestionImage, btnPlayAudio;
    private Button[] optionButtons = new Button[4];
    private Button btnEndGame;

    private View vSpectatorOverlay;
    private LinearLayout layoutSpectatorLabel;

    private KothSignalRClientManager signalRManager;
    private UserRepository userRepository;
    private String activityId;
    private String matchId;
    private String userId;
    private String language;
    private int yourScore = 0;
    private int totalPlayers = 0;
    private int playersRemaining = 0;
    private List<PlayerInfo> allPlayers = new ArrayList<>();


    private int currentRound = 0;
    private RoundType currentRoundType = RoundType.CLASSIC;
    private QuestionData currentQuestion;
    private int selectedOptionIndex = -1;
    private boolean isQuestionActive = false;
    private boolean hasAnswered = false;
    private boolean isEliminated = false;
    private boolean isSpectator = false;
    private boolean isManualDisconnect = false;
    private long questionStartTime;
    private MediaPlayer mediaPlayer;


    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private int timeLeft = 10;
    private boolean isPlaying = false;
    private View wave1, wave2, wave3, wave4;
    private Animator waveAnimator1, waveAnimator2, waveAnimator3, waveAnimator4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_king_game);

        userRepository = UserRepository.getInstance(this);
        preferencesHelper = new SecurePreferencesHelper(this);
        userId = preferencesHelper.getUserId();
        language = preferencesHelper.getLanguage();
        activityId = "king_game_" + System.currentTimeMillis();

        initViews();
        setupClickListeners();
        getIntentData();

        signalRManager = KothSignalRClientManager.getInstance();
        connectToSignalR();
        sendReadyForGame(matchId);
        hideSystemBars();
    }

    private void initViews() {
        tvPlayersRemaining = findViewById(R.id.tvPlayersRemaining);
        tvTotalPlayers = findViewById(R.id.tvTotalPlayers);
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvRoundTypeIcon = findViewById(R.id.tvRoundTypeIcon);
        tvRoundType = findViewById(R.id.tvRoundType);
        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        tvAudioHint = findViewById(R.id.tv_audio_hint);

        layoutQuestionInfo = findViewById(R.id.layoutQuestionInfo);
        tvQuestionTitle = findViewById(R.id.tv_question_title);
        imageContainer = findViewById(R.id.image_container);
        audioContainer = findViewById(R.id.audio_container);
        ivQuestionImage = findViewById(R.id.iv_question_image);
        btnPlayAudio = findViewById(R.id.btn_play_audio);
        btnEndGame = findViewById(R.id.btn_end_game);
        layoutWave = findViewById(R.id.layout_wave);

        optionButtons[0] = findViewById(R.id.btn_option1);
        optionButtons[1] = findViewById(R.id.btn_option2);
        optionButtons[2] = findViewById(R.id.btn_option3);
        optionButtons[3] = findViewById(R.id.btn_option4);
        setOptionsEnabled(false);

        vSpectatorOverlay = findViewById(R.id.vSpectatorOverlay);
        layoutSpectatorLabel = findViewById(R.id.layoutSpectatorLabel);
        wave1 = findViewById(R.id.wave1);
        wave2 = findViewById(R.id.wave2);
        wave3 = findViewById(R.id.wave3);
        wave4 = findViewById(R.id.wave4);
    }

    private void setupClickListeners() {
        btnEndGame.setOnClickListener(v -> exitGame());

        btnPlayAudio.setOnClickListener(v -> {
            if (isPlaying) {
                stopAudio();
            } else {
                playAudio();
            }
        });

        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> submitAnswer(index));
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        matchId = intent.getStringExtra("match_id");
        totalPlayers = intent.getIntExtra("total_players", 0);
        playersRemaining = intent.getIntExtra("players_remaining", totalPlayers);
        yourScore = intent.getIntExtra("current_score", 0);

        allPlayers = (List<PlayerInfo>) intent.getSerializableExtra("all_players");
        if (allPlayers == null) {
            allPlayers = new ArrayList<>();
        }

        String roundJson = getIntent().getStringExtra("roundData");
        if (roundJson != null) {
            Gson gson = new Gson();
            RoundStartedData roundData = gson.fromJson(roundJson, RoundStartedData.class);
            handleRoundStarted(roundData);
        }

        tvPlayersRemaining.setText(String.valueOf(playersRemaining));
        tvTotalPlayers.setText("/" + String.valueOf(totalPlayers));
        tvScore.setText(String.valueOf(yourScore));
    }

    private void connectToSignalR() {
        if (!signalRManager.isConnected()) {
            signalRManager.start();
        }
        signalRManager.addListener(activityId, new KothSignalRClientManager.KothConnectionListener() {
            @Override
            public void onConnected() {
                Log.d(TAG, "Connected to SignalR for game");
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    if (!isManualDisconnect || !isDestroyed() || isFinishing())
                        Toast.makeText(KingGameActivity.this, getString(R.string.connection_lost), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(KingGameActivity.this,
                            getString(R.string.error) + error, Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onPlayerJoinedToOthers(PlayerJoinedData data) { }
            @Override public void onPlayerAboutLobby(LobbyInitialStateData data) { }
            @Override public void onPlayerLeft(PlayerLeftData data) { }
            @Override public void onLobbyCountdown(int secondsRemaining) { }
            @Override public void onLobbyCountdownCancelled() { }
            @Override public void onMatchStarted(MatchStartedData data) {}
            @Override public void onMatchResume(MatchResumeData data) {}

            @Override
            public void onRoundStarted(RoundStartedData data) {
                runOnUiThread(() -> handleRoundStarted(data));
            }

            @Override
            public void onRoundFinished(RoundFinishedData data) {
                runOnUiThread(() -> handleRoundFinished(data));
            }

            @Override
            public void onPlayerEliminated(PlayerEliminatedData data) {
                runOnUiThread(() -> handlePlayerEliminated(data));
            }

            @Override
            public void onAnswerResult(AnswerResultData data) {
                runOnUiThread(() -> handleAnswerResult(data));
            }

            @Override
            public void onMatchFinished(MatchFinishedData data) {
                runOnUiThread(() -> handleMatchFinished(data));
            }

            @Override
            public void onForceDisconnect(LocalizedText message) {
                runOnUiThread(() -> {
                    handleForceDisconnect(message);
                });
            }
        });
    }
    @Override
    protected void handleAchievementUnlocked(List<ProfileResponse.AchievementDto> achievements) {
        runOnUiThread(() -> {
            Log.d("NotificationManager", "AchievementUnlocked in solo game received! Count: " + achievements.size());
            userRepository = UserRepository.getInstance(this);

            for (ProfileResponse.AchievementDto achievement : achievements) {
                userRepository.savePendingAchievements(achievement);
            }
        });
    }
    private void sendReadyForGame(String matchId)
    {
        if (signalRManager != null && signalRManager.isConnected()) {
            signalRManager.playerReadyForGame(matchId);
        }
    }
    private void handleRoundStarted(RoundStartedData data) {
        Log.d(TAG, "Round " + data.getRoundNumber() + " started, type: " + data.getRoundType());

        currentRound = data.getRoundNumber();
        currentRoundType = data.getRoundType();
        currentQuestion = data.getQuestion();
        isQuestionActive = true;
        hasAnswered = false;
        selectedOptionIndex = -1;
        questionStartTime = SystemClock.elapsedRealtime();

        tvQuestionNumber.setText(getString(R.string.question) +" " +  currentRound);

        if (currentRoundType == RoundType.SPEED) {
            tvRoundTypeIcon.setText("⚡");
            tvRoundType.setText(R.string.speed_round);
            tvRoundTypeIcon.setTextColor(ContextCompat.getColor(this, R.color.colorWarning));
            tvRoundType.setTextColor(ContextCompat.getColor(this, R.color.colorWarning));
        } else {
            tvRoundTypeIcon.setText("📚");
            tvRoundType.setText(R.string.classic_round);
            tvRoundTypeIcon.setTextColor(ContextCompat.getColor(this, R.color.primary));
            tvRoundType.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }

        tvQuestionTitle.setText(getLocalizedText(currentQuestion.getQuestionText()));

        if (currentQuestion.getImageUrl() != null && !currentQuestion.getImageUrl().isEmpty()) {
            imageContainer.setVisibility(View.VISIBLE);
            loadImageFromAssets(currentQuestion.getImageUrl(), ivQuestionImage);
        } else {
            imageContainer.setVisibility(View.GONE);
        }

        if (currentQuestion.getAudioUrl() != null && !currentQuestion.getAudioUrl().isEmpty()) {
            audioContainer.setVisibility(View.VISIBLE);
        } else {
            audioContainer.setVisibility(View.GONE);
        }
        stopAudio();

        List<OptionData> options = currentQuestion.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            if (i < options.size()) {
                optionButtons[i].setVisibility(View.VISIBLE);
                optionButtons[i].setText(getLocalizedText(options.get(i).getText()));
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundResource(R.drawable.option_button_normal);
                optionButtons[i].setTextColor(ContextCompat.getColor(this, android.R.color.black));
            } else {
                optionButtons[i].setVisibility(View.GONE);
            }
        }
        if (isEliminated) setOptionsEnabled(false);
        startTimer(data.getServerTime(), data.getRoundEndAt());
    }

    private void handleRoundFinished(RoundFinishedData data) {
        Log.d(TAG, "Round " + data.getRoundNumber() + " finished, eliminated: " +
                data.getEliminatedPlayerIds().size());

        stopTimer();

        playersRemaining = data.getRemainingPlayers();
        tvPlayersRemaining.setText(String.valueOf(playersRemaining));

        highlightCorrectAnswer(data.getCorrectOptionIndex());
        setOptionsEnabled(false);
    }

    private void handlePlayerEliminated(PlayerEliminatedData data) {
        if (!data.getPlayerId().equals(userId)) return;

        isEliminated = true;

        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage)
                ? "Вы выбыли"
                : "You are eliminated";

        String message = "ru".equals(currentLanguage)
                ? "Вы больше не участвуете в раунде.\n\nХотите продолжить наблюдать за игрой или перейти к результатам?"
                : "You are no longer in the round.\n\nDo you want to keep watching or go to results?";

        String spectate = "ru".equals(currentLanguage)
                ? "Наблюдать"
                : "Watch";

        String exit = "ru".equals(currentLanguage)
                ? "Результаты"
                : "Results";

        String place = "ru".equals(currentLanguage)
                ? "🏆 Ваше место: №" + data.getPlace()
                : "🏆 Your place: №" + data.getPlace();


        View view = getLayoutInflater().inflate(R.layout.dialog_eliminated, null);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvPlace = view.findViewById(R.id.tvPlace);
        MaterialButton btnSpectate = view.findViewById(R.id.btnSpectate);
        MaterialButton btnExit = view.findViewById(R.id.btnExit);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnSpectate.setText(spectate);
        btnExit.setText(exit);
        tvPlace.setText(place);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .create();

        btnSpectate.setOnClickListener(v -> {
            dialog.dismiss();
            enterSpectatorMode();
        });

        btnExit.setOnClickListener(v -> {
            dialog.dismiss();
            openResultScreen(data);
        });

        dialog.show();
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);
        view.setAlpha(0f);

        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(220)
                .start();
    }
    private void submitAnswer(int index) {
        if (!isQuestionActive || hasAnswered || isEliminated || isSpectator) return;

        long answerTime = SystemClock.elapsedRealtime() - questionStartTime;

        hasAnswered = true;
        isQuestionActive = false;
        selectedOptionIndex = index;

        submitLocally(index);

        signalRManager.submitAnswer(matchId, currentRound, currentQuestion.getCountryId(), index,  (int) answerTime);
    }
    private void submitLocally(int selectedIndex)
    {
        setOptionsEnabled(false);
        optionButtons[selectedIndex].setAlpha(0.5f);
    }
    private void handleAnswerResult(AnswerResultData data) {
        Log.d(TAG, "Answer result: correct=" + data.isCorrect() + ", score=" + data.getScoreGained());

        if (!isEliminated) {
            yourScore += data.getScoreGained();
            tvScore.setText(String.valueOf(yourScore));

            highlightSelectedAnswer(data.isCorrect(), data.getCorrectOptionIndex());

            if (data.getRemainingPlayers() > 0) {
                playersRemaining = data.getRemainingPlayers();
                tvPlayersRemaining.setText(String.valueOf(playersRemaining));
            }
        }
    }
    private void enterSpectatorMode() {
        isSpectator = true;
        setOptionsEnabled(false);
        applySpectatorUI();
    }
    private void applySpectatorUI() {

        vSpectatorOverlay.setVisibility(View.VISIBLE);
        layoutSpectatorLabel.setVisibility(View.VISIBLE);

        vSpectatorOverlay.setAlpha(0f);
        vSpectatorOverlay.animate().alpha(1f).setDuration(200).start();

        layoutSpectatorLabel.setScaleX(0.8f);
        layoutSpectatorLabel.setScaleY(0.8f);
        layoutSpectatorLabel.animate().scaleX(1f).scaleY(1f).setDuration(200).start();

        layoutQuestionInfo.setAlpha(0.8f);
        tvScore.setAlpha(0.8f);
    }
    private void openResultScreen(PlayerEliminatedData data)
    {
        String playerName = "Player";
        for (PlayerInfo player: allPlayers) {
            if (player.getPlayerId().equals(userId)) playerName = player.getPlayerName();
        }

        Intent intent = new Intent(this, KingEliminatedActivity.class);
        intent.putExtra("player_name", playerName);
        intent.putExtra("rounds_survived", data.getRoundsSurvived());
        intent.putExtra("place", data.getPlace());
        intent.putExtra("correct_answers", data.getCorrectAnswers());
        intent.putExtra("total_score", data.getTotalScore());
        intent.putExtra("is_manually_disabled", data.isManuallyDisabled());
        intent.putExtra("total_players", totalPlayers);
        startActivity(intent);
    }
    private void handleMatchFinished(MatchFinishedData data) {
        Log.d(TAG, "Match finished! Winner: " + data.getWinnerId());
        if (signalRManager != null) {
            signalRManager.removeListener(activityId);
        }
        Gson gson = new Gson();
        String matchDataJson = gson.toJson(data);

        Intent intent = new Intent(KingGameActivity.this, KingResultActivity.class);
        intent.putExtra("match_data", matchDataJson);
        startActivity(intent);
        finish();
    }

    private void startTimer(long serverTime, long endAt) {
        stopTimer();

        long clientTime = System.currentTimeMillis();
        long offset = serverTime - clientTime;

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis() + offset;
                long remainingMs = endAt - now;
                timeLeft = (int)Math.ceil(remainingMs/1000.0);

                updateTimerDisplay();

                if (timeLeft < 0)
                {
                    if (isQuestionActive && !hasAnswered && !isEliminated) {
                        setOptionsEnabled(false);
                        isQuestionActive = false;
                    }
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateTimerDisplay() {
        String timeFormatted = String.format(Locale.getDefault(), "00:%02d", timeLeft);
        tvTimer.setText(timeFormatted);

        if (timeLeft <= 3) {
            tvTimer.setTextColor(ContextCompat.getColor(this, R.color.colorError));
        } else {
            tvTimer.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void highlightSelectedAnswer(boolean isCorrect, int correctIndex) {
        optionButtons[selectedOptionIndex].setAlpha(1f);
        if (correctIndex >= 0 && correctIndex < optionButtons.length) {
            optionButtons[correctIndex].setBackgroundResource(R.drawable.correct_answer_bg);
            optionButtons[correctIndex].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }

        if (!isCorrect && selectedOptionIndex >= 0 && selectedOptionIndex < optionButtons.length) {
            optionButtons[selectedOptionIndex].setBackgroundResource(R.drawable.wrong_answer_bg);
            optionButtons[selectedOptionIndex].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void highlightCorrectAnswer(int correctIndex) {
        if (correctIndex >= 0 && correctIndex < optionButtons.length) {
            optionButtons[correctIndex].setBackgroundResource(R.drawable.correct_answer_bg);
            optionButtons[correctIndex].setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void setOptionsEnabled(boolean enabled) {
        for (Button button : optionButtons) {
            button.setEnabled(enabled);
        }
    }


    private String getLocalizedText(LocalizedText text) {
        if (text == null) return "";
        if ("ru".equals(language) && text.getRu() != null && !text.getRu().isEmpty()) {
            return text.getRu();
        }
        return text.getEn() != null ? text.getEn() : "";
    }

    private void loadImageFromAssets(String imagePath, ImageView imageView) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                InputStream ims = getAssets().open(imagePath);
                Drawable d = Drawable.createFromStream(ims, null);
                imageView.setImageDrawable(d);
                ims.close();
                Log.d(TAG, "Image loaded successfully: " + imagePath);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from assets: " + imagePath, e);
        }
    }
    private void stopAudio()
    {
        isPlaying = false;

        btnPlayAudio.setImageResource(R.drawable.ic_play_audio);
        tvAudioHint.setText(R.string.play_audio);
        layoutWave.setVisibility(View.GONE);
        stopWaveAnimation();

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void playAudio() {
        String audioPath = currentQuestion != null ? currentQuestion.getAudioUrl() : null;

        if (audioPath == null || audioPath.isEmpty()) {
            Toast.makeText(this,
                    language.equals("ru") ? "Аудио недоступно" : "Audio not available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            stopAudio();

            isPlaying = true;
            btnPlayAudio.setImageResource(R.drawable.ic_stop_audio);
            tvAudioHint.setText(R.string.stop_audio);
            layoutWave.setVisibility(View.VISIBLE);
            startWaveAnimation();

            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor afd = getAssets().openFd(audioPath);
            mediaPlayer.setDataSource(afd.getFileDescriptor(),
                    afd.getStartOffset(),
                    afd.getLength());
            afd.close();
            mediaPlayer.prepare();
            mediaPlayer.start();

            Log.d(TAG, "Playing audio: " + audioPath);

        } catch (IOException e) {
            Log.e(TAG, "Error playing audio", e);
            Toast.makeText(this,
                    language.equals("ru") ? "Ошибка воспроизведения" : "Playback error",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void startWaveAnimation() {
        waveAnimator1 = createWaveAnimator(wave1, 12, 30);
        waveAnimator2 = createWaveAnimator(wave2, 14, 48);
        waveAnimator3 = createWaveAnimator(wave3, 10, 36);
        waveAnimator4 = createWaveAnimator(wave4, 16, 44);

        waveAnimator1.start();
        waveAnimator2.start();
        waveAnimator3.start();
        waveAnimator4.start();
    }

    private Animator createWaveAnimator(View view, int minHeight, int maxHeight) {
        ValueAnimator animator = ValueAnimator.ofInt(minHeight, maxHeight);
        animator.setDuration(400);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(animation -> {
            int height = (int) animation.getAnimatedValue();
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = height;
            view.setLayoutParams(params);
        });
        return animator;
    }

    private void stopWaveAnimation() {
        if (waveAnimator1 != null && waveAnimator1.isRunning()) {
            waveAnimator1.cancel();
            waveAnimator1.end();
        }
        if (waveAnimator2 != null && waveAnimator2.isRunning()) {
            waveAnimator2.cancel();
            waveAnimator2.end();
        }
        if (waveAnimator3 != null && waveAnimator3.isRunning()) {
            waveAnimator3.cancel();
            waveAnimator3.end();
        }
        if (waveAnimator4 != null && waveAnimator4.isRunning()) {
            waveAnimator4.cancel();
            waveAnimator4.end();
        }

        resetWaveHeights();
    }
    private void resetWaveHeights() {
        setWaveHeight(wave1, 12);
        setWaveHeight(wave2, 20);
        setWaveHeight(wave3, 16);
        setWaveHeight(wave4, 24);
    }
    private void setWaveHeight(View wave, int height) {
        if (wave != null) {
            ViewGroup.LayoutParams params = wave.getLayoutParams();
            params.height = height;
            wave.setLayoutParams(params);
        }
    }
    private void exitGame() {
        String currentLanguage = preferencesHelper.getLanguage();

        String title = "ru".equals(currentLanguage) ? "Выход из игры" : "Exit Game";
        String message = "ru".equals(currentLanguage)
                ? "Вы уверены, что хотите покинуть игру?"
                : "Are you sure you want to leave the game?";
        String positiveButton = "ru".equals(currentLanguage) ? "Да, выйти" : "Yes, leave";
        String negativeButton = "ru".equals(currentLanguage) ? "Отмена" : "Cancel";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    performExitGame();
                })
                .setNegativeButton(negativeButton, null)
                .show();
    }
    private void handleDisconnect()
    {
        isManualDisconnect = true;
        if (signalRManager!= null && signalRManager.isConnected()) {
            signalRManager.stop();
        }
    }
    private void handleForceDisconnect(LocalizedText message)
    {
        String msg = preferencesHelper.getLanguage().equals("ru") ? message.getRu() : message.getEn();
        Toast.makeText(KingGameActivity.this, msg, Toast.LENGTH_SHORT).show();
        handleDisconnect();
        finish();
    }
    private void performExitGame() {
        stopTimer();
        if (signalRManager != null && signalRManager.isConnected() && matchId != null) {
            signalRManager.leaveMatch(matchId);
            signalRManager.stop();
        }
        //finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        stopAudio();
        if (signalRManager != null) {
            signalRManager.removeListener(activityId);
        }
    }
    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    @Override
    public void onBackPressed() {
        exitGame();
    }
}