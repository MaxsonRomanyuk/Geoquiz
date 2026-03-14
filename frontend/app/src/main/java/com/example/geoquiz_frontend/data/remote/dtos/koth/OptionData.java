package com.example.geoquiz_frontend.data.remote.dtos.koth;

import com.example.geoquiz_frontend.domain.enums.LocalizedText;
import com.google.gson.annotations.SerializedName;

public class OptionData {

    @SerializedName("index")
    private int index;

    @SerializedName("text")
    private LocalizedText text;

    public int getIndex() { return index; }
    public LocalizedText getText() { return text; }
}
