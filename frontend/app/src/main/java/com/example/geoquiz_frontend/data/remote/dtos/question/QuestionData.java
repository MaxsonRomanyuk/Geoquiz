package com.example.geoquiz_frontend.data.remote.dtos.question;

import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QuestionData {

    @SerializedName("countryId")
    private String countryId;

    @SerializedName("questionText")
    private LocalizedText questionText;

    @SerializedName("options")
    private List<OptionData> options;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("audioUrl")
    private String audioUrl;

    public String getCountryId() { return countryId; }
    public LocalizedText getQuestionText() { return questionText; }
    public List<OptionData> getOptions() { return options; }
    public String getImageUrl() { return imageUrl; }
    public String getAudioUrl() { return audioUrl; }
}
