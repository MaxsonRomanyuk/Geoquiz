package com.example.geoquiz_frontend.presentation.ui.history;

import com.example.geoquiz_frontend.data.remote.dtos.history.GameSessionDto;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchHistoryEntry {
    private String matchId;
    private int gameType;
    private long timestamp;
    private int totalScore;
    private int correctAnswers;
    private int totalQuestions;
    private int gameMode;
    private boolean isWin;
    private int place;
    private int roundsSurvived;
    private int experienceGained;

    public MatchHistoryEntry(String matchId, int gameType, long timestamp, int totalScore,
                             int correctAnswers, int totalQuestions, int gameMode,
                             boolean isWin, int experienceGained) {
        this.matchId = matchId;
        this.gameType = gameType;
        this.timestamp = timestamp;
        this.totalScore = totalScore;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.gameMode = gameMode;
        this.isWin = isWin;
        this.experienceGained = experienceGained;
        this.place = 0;
        this.roundsSurvived = 0;
    }

    public MatchHistoryEntry(String matchId, int gameType, long timestamp, int totalScore,
                             int correctAnswers, int totalQuestions, int gameMode,
                             int place, int roundsSurvived, int experienceGained) {
        this.matchId = matchId;
        this.gameType = gameType;
        this.timestamp = timestamp;
        this.totalScore = totalScore;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.gameMode = gameMode;
        this.isWin = place == 1;
        this.place = place;
        this.roundsSurvived = roundsSurvived;
        this.experienceGained = experienceGained;
    }

    public static MatchHistoryEntry fromDto(GameSessionDto dto, long timeOffset) {
        long localTimestamp = dto.getPlayedAt() + timeOffset;

        if (dto.getGameType() == 3) {
            return new MatchHistoryEntry(
                    dto.getMatchId(),
                    dto.getGameType(),
                    localTimestamp,
                    dto.getTotalScore(),
                    dto.getCorrectAnswers(),
                    dto.getTotalQuestions(),
                    dto.getGameMode(),
                    dto.getPlace(),
                    dto.getRoundsSurvived(),
                    dto.getExperienceGained()
            );
        } else {
            return new MatchHistoryEntry(
                    dto.getMatchId(),
                    dto.getGameType(),
                    localTimestamp,
                    dto.getTotalScore(),
                    dto.getCorrectAnswers(),
                    dto.getTotalQuestions(),
                    dto.getGameMode(),
                    dto.isWin(),
                    dto.getExperienceGained()
            );
        }
    }

    public String getFormattedDateTime(long timeOffset) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public String getMatchId() { return matchId; }
    public int getGameType() { return gameType; }
    public long getTimestamp() { return timestamp; }
    public int getTotalScore() { return totalScore; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getGameMode() { return gameMode; }
    public boolean isWin() { return isWin; }
    public int getPlace() { return place; }
    public int getRoundsSurvived() { return roundsSurvived; }
    public int getExperienceGained() { return experienceGained; }
}
