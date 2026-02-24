package com.example.geoquiz_frontend.domain.entities;

import java.io.Serializable;
import java.util.List;

public class GameQuestion implements Serializable {
    private String id;
    private String countryId;
    private String questionText;
    private List<String> options;
    private int correctAnswerIndex;
    private String mediaUrl;
    private int mode;
    private int timeSpent;
    private int selectedAnswerIndex = -1;
    private boolean isAnswered = false;
    private boolean isCorrect = false;
    private int region; // 1 eu, 2 as, 3 af, 4 am ,5 oc

    public GameQuestion(String id, String countryId, String questionText,
                        List<String> options, int correctAnswerIndex,
                        String mediaUrl, int mode, int region) {
        this.id = id;
        this.countryId = countryId;
        this.questionText = questionText;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.mediaUrl = mediaUrl;
        this.mode = mode;
        this.region = region;
    }

    public String getId() { return id; }
    public String getCountryId() { return countryId; }
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return options; }
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public String getMediaUrl() { return mediaUrl; }
    public int getMode() { return mode; }
    public int getTimeSpent() { return timeSpent; }
    public int getSelectedAnswerIndex() { return selectedAnswerIndex; }
    public boolean isAnswered() { return isAnswered; }
    public boolean isCorrect() { return isCorrect; }

    public int getRegion() {return region; }

    public void setTimeSpent(int timeSpent) { this.timeSpent = timeSpent; }

    public void setSelectedAnswerIndex(int selectedAnswerIndex) {
        this.selectedAnswerIndex = selectedAnswerIndex;
        this.isAnswered = true;
        this.isCorrect = (selectedAnswerIndex == correctAnswerIndex);
    }
}