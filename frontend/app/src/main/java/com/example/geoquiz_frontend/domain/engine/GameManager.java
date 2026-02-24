package com.example.geoquiz_frontend.domain.engine;

import android.content.Context;

import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.domain.entities.GameQuestion;
import com.example.geoquiz_frontend.domain.entities.GameSession;

import java.util.List;

public class GameManager {
    private static GameManager instance;
    private GameRepository repository;

    private GameManager(Context context) {
        repository = new GameRepository(context);
    }
    public static synchronized GameManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameManager(context.getApplicationContext());
        }
        return instance;
    }
    public void loadBootstrapData(GameRepository.BootstrapCallback callback) {
        repository.loadBootstrapData(callback);
    }
    public List<GameQuestion> getQuestionsForMode(int gameMode, int count) {
        return repository.getQuestionsForMode(gameMode, count);
    }
    public boolean hasLocalData() {
        //return repository.hasLocalData();
        return false;
    }

    public int getQuestionCountForMode(int gameMode) {
        //return repository.getQuestionCountForMode(gameMode);
        return 0;
    }

    public void saveGameSession(GameSession session) {
        repository.saveGameSession(session);
    }

    public void syncGames() {
        repository.syncPendingGames();
    }
}