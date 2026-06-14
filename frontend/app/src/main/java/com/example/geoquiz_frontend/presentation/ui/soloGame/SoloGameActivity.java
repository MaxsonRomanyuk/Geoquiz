package com.example.geoquiz_frontend.presentation.ui.soloGame;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.repositories.UserRepository;
import com.example.geoquiz_frontend.domain.engine.GameManager;
import com.example.geoquiz_frontend.domain.entities.GameQuestion;
import com.example.geoquiz_frontend.domain.entities.GameSession;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.presentation.ui.base.BaseActivity;
import com.example.geoquiz_frontend.presentation.ui.pvp.PvPGameActivity;
import com.example.geoquiz_frontend.presentation.utils.SecurePreferencesHelper;
import com.google.android.material.card.MaterialCardView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SoloGameActivity extends BaseActivity {

    private static final String TAG = "QuizActivity";
    private static final int TOTAL_QUESTIONS = 10;
    private static final int TOTAL_TIME_MS = 60000;

    private TextView tvQuestionNumber, tvTimer, tvScore, tvQuestionTitle, tvAudioHint;
    private Button[] optionButtons = new Button[4];
    private FrameLayout imageContainer;
    private LinearLayout audioContainer, layoutWave;
    private ImageView ivQuestionImage, btnPlayAudio;
    private Button btnEndGame;


    private GameManager gameManager;
    private DatabaseHelper databaseHelper;
    private UserRepository userRepository;
    private GameSession currentSession;
    private List<GameQuestion> questions;


    private CountDownTimer timer;
    private long timeLeft = TOTAL_TIME_MS;
    private long questionStartTime;


    private MediaPlayer mediaPlayer;


    private int gameMode;
    private String language;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private int correctAnswers = 0;
    private int wrongAnswers = 0;
    private int correctEurope = 0;
    private int correctAsia = 0;
    private int correctAfrica = 0;
    private int correctAmerica = 0;
    private int correctOceania = 0;
    private boolean isGameFinished = false;
    private boolean isPlaying = false;
    private View wave1, wave2, wave3, wave4;
    private Animator waveAnimator1, waveAnimator2, waveAnimator3, waveAnimator4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solo_game);

        preferencesHelper = new SecurePreferencesHelper(this);
        databaseHelper = new DatabaseHelper(this);
        gameManager = GameManager.getInstance(this);
        userRepository = UserRepository.getInstance(this);

        language = preferencesHelper.getLanguage();
        gameMode = getIntent().getIntExtra("GAME_MODE", 1);

        currentSession = new GameSession();

        initViews();
        setupClickListeners();
        loadQuestions();
        hideSystemBars();
    }

    private void initViews() {
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        tvQuestionTitle = findViewById(R.id.tv_question_title);
        tvAudioHint = findViewById(R.id.tv_audio_hint);

        optionButtons[0] = findViewById(R.id.btn_option1);
        optionButtons[1] = findViewById(R.id.btn_option2);
        optionButtons[2] = findViewById(R.id.btn_option3);
        optionButtons[3] = findViewById(R.id.btn_option4);

        imageContainer = findViewById(R.id.image_container);
        audioContainer = findViewById(R.id.audio_container);
        ivQuestionImage = findViewById(R.id.iv_question_image);
        layoutWave = findViewById(R.id.layout_wave);

        btnPlayAudio = findViewById(R.id.btn_play_audio);

        btnEndGame = findViewById(R.id.btn_end_game);

        wave1 = findViewById(R.id.wave1);
        wave2 = findViewById(R.id.wave2);
        wave3 = findViewById(R.id.wave3);
        wave4 = findViewById(R.id.wave4);
    }
    private void setupClickListeners() {
        if (btnEndGame != null) {
            btnEndGame.setOnClickListener(v -> showExitConfirmation());
        }

        btnPlayAudio.setOnClickListener(v -> {
            if (isPlaying) {
                stopAudio();
            } else {
                playAudio();
            }
        });
    }

    private void loadQuestions() {
        int level = databaseHelper.getUserStats(preferencesHelper.getUserId()).getLevel();
        tvQuestionTitle.setText(language.equals("ru") ? "Загрузка вопросов..." : "Loading questions...");

        questions = gameManager.getQuestionsForMode(gameMode, TOTAL_QUESTIONS, language, level);

        if (questions == null || questions.isEmpty()) {
            Log.e(TAG, "No questions loaded for mode: " + gameMode);
            String errorMsg = language.equals("ru") ?
                    "Не удалось загрузить вопросы" :
                    "Failed to load questions";
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Loaded " + questions.size() + " questions");

        startTimer();
        showQuestion();
    }

    private void startTimer() {
        timer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                updateTimerDisplay();
            }

            @Override
            public void onFinish() {
                timeLeft = 0;
                updateTimerDisplay();
                endGame();
            }
        }.start();
    }

    private void updateTimerDisplay() {
        int seconds = (int) (timeLeft / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;

        String timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds);

        if (tvTimer != null) {
            tvTimer.setText(timeText);
            if (seconds <= 10) {
                tvTimer.setTextColor(ContextCompat.getColor(SoloGameActivity.this, R.color.colorError));
            }
        }
    }

    private void showQuestion() {
        if (isGameFinished || currentQuestionIndex >= questions.size()) {
            endGame();
            return;
        }

        GameQuestion question = questions.get(currentQuestionIndex);

        if (tvQuestionNumber != null) {
            String questionText = language.equals("ru") ?
                    "Вопрос " + (currentQuestionIndex + 1) + "/" + TOTAL_QUESTIONS :
                    "Question " + (currentQuestionIndex + 1) + "/" + TOTAL_QUESTIONS;
            questionText = (currentQuestionIndex + 1) + "/" + TOTAL_QUESTIONS;
            tvQuestionNumber.setText(questionText);
        }

        if (tvScore != null) {
            tvScore.setText(String.valueOf(score));
        }

        if (tvQuestionTitle != null) {
            tvQuestionTitle.setText(question.getQuestionText());
        }

        List<String> options = question.getOptions();
        for (int i = 0; i < optionButtons.length && i < options.size(); i++) {
            if (optionButtons[i] != null) {
                final int index = i;
                optionButtons[i].setText(options.get(i));
                optionButtons[i].setEnabled(true);
                optionButtons[i].setBackgroundResource(R.drawable.option_button_normal);
                optionButtons[i].setTextColor(getResources().getColor(android.R.color.black, null));
                optionButtons[i].setOnClickListener(v -> checkAnswer(index));
            }
        }

        handleMedia(question);

        questionStartTime = System.currentTimeMillis();
    }

    private void handleMedia(GameQuestion question) {
        String mediaUrl = question.getMediaUrl();
        if (imageContainer != null) imageContainer.setVisibility(View.GONE);
        if (audioContainer != null) audioContainer.setVisibility(View.GONE);
        stopAudio();

        switch (gameMode) {
            case 2:
            case 3:
                if (imageContainer != null) {
                    imageContainer.setVisibility(View.VISIBLE);
                }
                if (mediaUrl != null && !mediaUrl.isEmpty()) {
                    loadImageFromAssets(mediaUrl, ivQuestionImage);
                }
                break;

            case 4:
//                if (audioContainer != null && mediaUrl != null) {
//                    audioContainer.setVisibility(View.VISIBLE);
//                }
                audioContainer.setVisibility(View.VISIBLE);
                break;

            default:
                break;
        }
    }


    private void loadImageFromAssets(String imagePath, ImageView imageView) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                InputStream ims = getAssets().open(imagePath);
                Drawable d = Drawable.createFromStream(ims, null);
                imageView.setImageDrawable(d);
                ims.close();
                Log.d(TAG, "Image loaded successfully: " + imagePath);
            } else {
                Log.w(TAG, "Image path is empty or null");
                //imageView.setImageResource(R.drawable.ic_placeholder);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from assets: " + imagePath, e);
            //imageView.setImageResource(R.drawable.ic_placeholder);
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
        GameQuestion question = questions.get(currentQuestionIndex);
        String audioPath = question != null ? question.getMediaUrl() : null;

        audioPath = "sounds/languages/afghanistan_pashto.mp3";

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
    private void checkAnswer(int selectedIndex) {
        GameQuestion question = questions.get(currentQuestionIndex);
        currentQuestionIndex++;
        if (question == null || question.isAnswered()) return;

        long timeSpent = System.currentTimeMillis() - questionStartTime;

        question.setSelectedAnswerIndex(selectedIndex);
        question.setTimeSpent((int) timeSpent);

        setButtonsEnabled(false);
        highlightAnswers(question, selectedIndex);

        if (question.isCorrect()) {
            correctAnswers++;

            int timePenalty = (int) (timeSpent / 1000);
            int questionScore = Math.max(1, 10 - timePenalty);
            score += questionScore;

            if (tvScore != null) {
                tvScore.setText(String.valueOf(score));
            }

            int region = question.getRegion();
            switch (region) {
                case 1:
                    correctEurope++;
                    break;
                case 2:
                    correctAsia++;
                    break;
                case 3:
                    correctAfrica++;
                    break;
                case 4:
                    correctAmerica++;
                    break;
                default:
                    correctOceania++;
                    break;
            }
        }
        else {
            wrongAnswers++;
            if (wrongAnswers >=3)
            {
                endGame();
                return;
            }
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentQuestionIndex < questions.size()) {
                showQuestion();
            } else {
                endGame();
            }
        }, 1000);
    }

    private void highlightAnswers(GameQuestion question, int selectedIndex) {
        int correctIndex = question.getCorrectAnswerIndex();

        for (int i = 0; i < optionButtons.length; i++) {
            if (optionButtons[i] == null) continue;

            if (i == correctIndex) {
                optionButtons[i].setBackgroundResource(R.drawable.correct_answer_bg);
                optionButtons[i].setTextColor(getResources().getColor(android.R.color.white, null));
            } else if (i == selectedIndex && i != correctIndex) {
                optionButtons[i].setBackgroundResource(R.drawable.wrong_answer_bg);
                optionButtons[i].setTextColor(getResources().getColor(android.R.color.white, null));
            }
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        for (Button button : optionButtons) {
            if (button != null) {
                button.setEnabled(enabled);
            }
        }
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(SoloGameActivity.this)
                .setTitle(language.equals("ru") ? "Выйти из игры?" : "Exit game?")
                .setMessage(language.equals("ru") ?
                        "Прогресс не сохранится" :
                        "Progress will not be saved")
                .setPositiveButton(language.equals("ru") ? "Да" : "Yes",
                        (dialog, which) -> finish())
                .setNegativeButton(language.equals("ru") ? "Нет" : "No", null)
                .show();
    }

    private void endGame() {
        if (isGameFinished) return;
        isGameFinished = true;

        currentSession = createGameSession();
        gameManager.saveGameSession(currentSession);
        databaseHelper.updateStatsAfterGame(preferencesHelper.getUserId(), gameMode, correctAnswers >= 8, correctAnswers, score,
                                            correctEurope, correctAsia, correctAfrica , correctAmerica , correctOceania);

        Log.d(TAG, "Ending game. Score: " + score + ", Correct: " + correctAnswers);

        if (timer != null) {
            timer.cancel();
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        userRepository.refreshFromDb();

        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("CORRECT", correctAnswers);
        intent.putExtra("TOTAL", currentQuestionIndex);
        intent.putExtra("TIME", (int) (TOTAL_TIME_MS - timeLeft));
        intent.putExtra("GAME_MODE", gameMode);

        removeListener();
        startActivity(intent);
        finish();
    }
    @Override
    protected void handleAchievementUnlocked(List<ProfileResponse.AchievementDto> achievements) {
        runOnUiThread(() -> {
            Log.d("NotificationManager", "AchievementUnlocked in solo game received! Count: " + achievements.size());
            for (ProfileResponse.AchievementDto achievement : achievements) {
                userRepository.savePendingAchievements(achievement);
            }
        });
    }
    private GameSession createGameSession(){
        currentSession.setMode(gameMode);
        currentSession.setPlayedAt(new Date(System.currentTimeMillis()));
        currentSession.setTotalQuestions(TOTAL_QUESTIONS);
        currentSession.setCorrectAnswers(correctAnswers);
        currentSession.setEuropeCorrect(correctEurope);
        currentSession.setAsiaCorrect(correctAsia);
        currentSession.setAfricaCorrect(correctAfrica);
        currentSession.setAmericaCorrect(correctAmerica);
        currentSession.setOceaniaCorrect(correctOceania);
        currentSession.setScore(score);
        currentSession.setTimeSpent((int) (TOTAL_TIME_MS - timeLeft)/1000);
        currentSession.setOnline(false);


        return currentSession;
    }
    private void showErrorAndFinish() {
        String errorMsg = language.equals("ru") ?
                "Произошла ошибка" :
                "An error occurred";
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        finish();
    }
    private void hideSystemBars() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        stopAudio();
    }

}
