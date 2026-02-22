package com.example.geoquiz_frontend.UI.Game;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.geoquiz_frontend.Engine.GameManager;
import com.example.geoquiz_frontend.Entities.GameQuestion;
import com.example.geoquiz_frontend.Entities.GameSession;
import com.example.geoquiz_frontend.PreferencesHelper;
import com.example.geoquiz_frontend.R;
import com.example.geoquiz_frontend.UI.Base.BaseActivity;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SoloGameActivity extends BaseActivity {

    private static final String TAG = "QuizActivity";
    private static final int TOTAL_QUESTIONS = 10;
    private static final int TOTAL_TIME_MS = 60000;


    private TextView tvQuestionNumber, tvTimer, tvScore, tvQuestionTitle;
    private Button[] optionButtons = new Button[4];
    private FrameLayout imageContainer;
    private ImageView ivQuestionImage;
    private Button btnPlayAudio, btnEndGame;
    private LinearLayout progressContainer, optionsContainer;


    private GameManager gameManager;
    private PreferencesHelper preferencesHelper;
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
    private int correctEurope = 0;
    private int correctAsia = 0;
    private int correctAfrica = 0;
    private int correctAmerica = 0;
    private int correctOceania = 0;
    private boolean isGameFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solo_game);

        preferencesHelper = new PreferencesHelper(this);
        gameManager = GameManager.getInstance(this);


        language = preferencesHelper.getLanguage();

        gameMode = getIntent().getIntExtra("GAME_MODE", 1);


        currentSession = new GameSession();

        initViews();
        setupClickListeners();
        loadQuestions();
    }

    private void initViews() {
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvTimer = findViewById(R.id.tv_timer);
        tvScore = findViewById(R.id.tv_score);
        tvQuestionTitle = findViewById(R.id.tv_question_title);

        optionButtons[0] = findViewById(R.id.btn_option1);
        optionButtons[1] = findViewById(R.id.btn_option2);
        optionButtons[2] = findViewById(R.id.btn_option3);
        optionButtons[3] = findViewById(R.id.btn_option4);

        imageContainer = findViewById(R.id.image_container);
        ivQuestionImage = findViewById(R.id.iv_question_image);

        btnPlayAudio = findViewById(R.id.btn_play_audio);

        btnEndGame = findViewById(R.id.btn_end_game);

        progressContainer = findViewById(R.id.progress_container);
        optionsContainer = findViewById(R.id.options_container);
    }
    private void setupClickListeners() {
        if (btnEndGame != null) {
            btnEndGame.setOnClickListener(v -> showExitConfirmation());
        }

        if (btnPlayAudio != null) {
            btnPlayAudio.setOnClickListener(v -> playAudio());
        }
    }

    private void loadQuestions() {
        tvQuestionTitle.setText(language.equals("ru") ? "Загрузка вопросов..." : "Loading questions...");

        questions = gameManager.getQuestionsForMode(gameMode, TOTAL_QUESTIONS);

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
                Log.d(TAG, "Time's up!");
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
        if (btnPlayAudio != null) btnPlayAudio.setVisibility(View.GONE);

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
                if (btnPlayAudio != null) {
                    btnPlayAudio.setVisibility(View.VISIBLE);
                }
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

    private void playAudio() {
        GameQuestion question = questions.get(currentQuestionIndex);
        String audioPath = question != null ? question.getMediaUrl() : null;

        if (audioPath == null || audioPath.isEmpty()) {
            Toast.makeText(this,
                    language.equals("ru") ? "Аудио недоступно" : "Audio not available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getAssets().openFd(audioPath).getFileDescriptor());
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

    private void checkAnswer(int selectedIndex) {
        GameQuestion question = questions.get(currentQuestionIndex);
        if (question == null || question.isAnswered()) return;

        long timeSpent = System.currentTimeMillis() - questionStartTime;

        question.setSelectedAnswerIndex(selectedIndex);
        question.setTimeSpent((int) timeSpent);

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

        setButtonsEnabled(false);

        highlightAnswers(question, selectedIndex);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            currentQuestionIndex++;
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
        new AlertDialog.Builder(this)
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

        Log.d(TAG, "Ending game. Score: " + score + ", Correct: " + correctAnswers);

        if (timer != null) {
            timer.cancel();
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }


        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra("SCORE", score);
        intent.putExtra("CORRECT", correctAnswers);
        intent.putExtra("TOTAL", TOTAL_QUESTIONS);
        intent.putExtra("TIME", (int) (TOTAL_TIME_MS - timeLeft));
        intent.putExtra("GAME_MODE", gameMode);
        startActivity(intent);
        finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

}
