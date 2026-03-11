package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.example.geoquiz_frontend.domain.enums.LocalizedText;

import java.util.List;

public class QuestionData {
    private String questionId;

    private LocalizedText questionText;

    private List<OptionData> options;

    private String imageUrl;

    private String audioUrl;

    public String getQuestionId() { return questionId; }
    public LocalizedText getQuestionText() { return questionText; }
    public List<OptionData> getOptions() { return options; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
}
