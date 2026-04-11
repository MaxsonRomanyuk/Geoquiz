package com.example.geoquiz_frontend.domain.enums;

import com.google.gson.annotations.SerializedName;

public class LocalizedText {
    @SerializedName("ru")
    private String ru;
    @SerializedName("en")
    private String en;

    public String getRu() { return ru; }
    public String getEn() { return en; }
    public LocalizedText() {};
    public LocalizedText(String ru, String en)
    {
        this.ru = ru;
        this.en = en;
    }

}