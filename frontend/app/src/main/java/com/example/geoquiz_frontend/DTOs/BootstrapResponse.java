package com.example.geoquiz_frontend.DTOs;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BootstrapResponse {
    private List<CountryDto> countries;
    private List<QuestionDto> questions;

    public List<CountryDto> getCountries() { return countries; }
    public List<QuestionDto> getQuestions() { return questions; }

    public static class CountryDto {
        @SerializedName("id")
        private String id;
        @SerializedName("name")
        private NameDto name;
        @SerializedName("capital")
        private CapitalDto capital;
        @SerializedName("region")
        private String region;

        @SerializedName("flagImage")
        private String flagImage;

        @SerializedName("outlineImage")
        private String outlineImage;

        @SerializedName("languageAudio")
        private String languageAudio;

        public String getId() { return id; }
        public NameDto getName() { return name; }
        public CapitalDto getCapital() { return capital; }
        public String getRegion() { return region; }
        public String getFlagImage() { return flagImage; }
        public String getOutlineImage() { return outlineImage; }
        public String getLanguageAudio() { return languageAudio; }
        public void setId(String id) { this.id = id; }
        public void setName(NameDto name) { this.name = name; }
        public void setCapital(CapitalDto capital) { this.capital = capital; }
        public void setRegion(String region) { this.region = region; }
        public void setFlagImage(String flagImage) { this.flagImage = flagImage; }
        public void setOutlineImage(String outlineImage) { this.outlineImage = outlineImage; }
        public void setLanguageAudio(String languageAudio) { this.languageAudio = languageAudio; }

        public static class NameDto {
            private String ru;
            private String en;
            public String getRu() { return ru; }
            public String getEn() { return en; }
            public void setRu(String ru) { this.ru = ru; }
            public void setEn(String en) { this.en = en; }
        }

        public static class CapitalDto {
            private String ru;
            private String en;
            public String getRu() { return ru; }
            public String getEn() { return en; }
            public void setRu(String ru) { this.ru = ru; }
            public void setEn(String en) { this.en = en; }
        }
    }

    public static class QuestionDto {
        @SerializedName("id")
        private String id;

        @SerializedName("countryId")
        private String countryId;

        @SerializedName("difficulty")
        private int difficulty;

        @SerializedName("type")
        private int type;

        public String getId() { return id; }
        public String getCountryId() { return countryId; }
        public int getDifficulty() { return difficulty; }
        public int getType() { return type; }

        public void setId(String id) { this.id = id; }
        public void setCountryId(String countryId) { this.countryId = countryId; }
        public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
        public void setType(int type) { this.type = type; }
    }
}
