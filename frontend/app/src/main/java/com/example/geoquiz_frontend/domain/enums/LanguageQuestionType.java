package com.example.geoquiz_frontend.domain.enums;

public enum LanguageQuestionType {
    CountryByLanguages(1),
    LanguageByCountries(2);

    private final int id;
    LanguageQuestionType(int id) {
        this.id = id;
    }
    public int getId() { return id; }
}
