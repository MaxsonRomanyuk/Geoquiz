package com.example.geoquiz_frontend.data.remote.dtos.solo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BootstrapResponse {
    private List<CountryDto> countries;
    public List<CountryDto> getCountries() { return countries; }

    public static class CountryDto {
        @SerializedName(value = "_id", alternate = {"id"})
        private String id;
        @SerializedName(value = "name", alternate = {"Name"})
        private NameDto name;
        @SerializedName(value = "capital", alternate = {"Capital"})
        private CapitalDto capital;
        @SerializedName(value = "region", alternate = {"Region"})
        private String region;
        @SerializedName(value = "flagImage", alternate = {"FlagImage"})
        private String flagImage;
        @SerializedName(value = "outlineImage", alternate = {"OutlineImage"})
        private String outlineImage;
        @SerializedName(value = "languages", alternate = {"Languages"})
        private List<CountryLanguageDto> languages;

        public String getId() { return id; }
        public NameDto getName() { return name; }
        public CapitalDto getCapital() { return capital; }
        public String getRegion() { return region; }
        public String getFlagImage() { return flagImage; }
        public String getOutlineImage() { return outlineImage; }

        public List<CountryLanguageDto> getLanguages() { return languages; }

        public void setId(String id) { this.id = id; }
        public void setName(NameDto name) { this.name = name; }
        public void setCapital(CapitalDto capital) { this.capital = capital; }
        public void setRegion(String region) { this.region = region; }
        public void setFlagImage(String flagImage) { this.flagImage = flagImage; }
        public void setOutlineImage(String outlineImage) { this.outlineImage = outlineImage; }
        public void setLanguages(List<CountryLanguageDto> languages) { this.languages = languages; }

        public static class NameDto {
            @SerializedName(value = "ru", alternate = {"Ru"})
            private String ru;
            @SerializedName(value = "en", alternate = {"En"})
            private String en;
            public String getRu() { return ru; }
            public String getEn() { return en; }
            public void setRu(String ru) { this.ru = ru; }
            public void setEn(String en) { this.en = en; }
        }

        public static class CapitalDto {
            @SerializedName(value = "ru", alternate = {"Ru"})
            private String ru;
            @SerializedName(value = "en", alternate = {"En"})
            private String en;
            public String getRu() { return ru; }
            public String getEn() { return en; }
            public void setRu(String ru) { this.ru = ru; }
            public void setEn(String en) { this.en = en; }
        }
        public static class CountryLanguageDto {
            @SerializedName(value = "_id", alternate = {"Id"})
            private String id;
            @SerializedName(value = "name", alternate = {"Name"})
            private CountryDto.NameDto name;
            @SerializedName(value = "audioUrl", alternate = {"AudioUrl"})
            private String audioUrl;

            public String getId() { return id; }
            public CountryDto.NameDto getName() { return name; }
            public String getAudioUrl() { return audioUrl; }

            public void setId(String id) {
                this.id = id;
            }
            public void setName(NameDto name) {
                this.name = name;
            }
            public void setAudioUrl(String audioUrl) {
                this.audioUrl = audioUrl;
            }
        }

    }

}
