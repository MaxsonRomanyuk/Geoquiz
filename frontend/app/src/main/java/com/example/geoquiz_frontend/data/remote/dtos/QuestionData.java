package com.example.geoquiz_frontend.data.remote.dtos;

import java.util.List;

public class QuestionData {
    private String questionId;
    private int questionNumber;
    private String questionText;
    private List<OptionData> options;
    private String imageUrl;
    private String audioUrl;

    public String getQuestionId() { return questionId; }
    public int getQuestionNumber() { return questionNumber; }
    public String getQuestionText() { return questionText; }
    public List<OptionData> getOptions() { return options; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
}
