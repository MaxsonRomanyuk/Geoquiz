package com.example.geoquiz_frontend.domain.engine;

import android.content.Context;

import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.domain.entities.CountryGrouping;
import com.example.geoquiz_frontend.domain.entities.GameQuestion;
import com.example.geoquiz_frontend.domain.entities.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameManager {
    private static GameManager instance;
    private GameRepository repository;
    private CountryGrouping cachedGrouping;

    private GameManager(Context context) {
        repository = new GameRepository(context);
    }
    public static synchronized GameManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameManager(context.getApplicationContext());
        }
        return instance;
    }
    public void loadBootstrapData(GameRepository.BootstrapCallback callback) {
        repository.loadBootstrapData(callback);
    }
    public void saveGameSession(GameSession session) {
        repository.saveGameSession(session);
    }

    public void syncGames() {
        repository.syncPendingGames();
    }
    private CountryGrouping getCountryGrouping() {
        if (cachedGrouping == null) {
            Map<String, BootstrapResponse.CountryDto> countriesCache = repository.getCountriesCache();
            List<BootstrapResponse.CountryDto> allCountries = new ArrayList<>(countriesCache.values());
            cachedGrouping = new CountryGrouping(allCountries);
        }
        return cachedGrouping;
    }
    public List<GameQuestion> getQuestionsForMode(int mode, int count, String language, int playerLvl) {
        List<GameQuestion> result = new ArrayList<>();

        CountryGrouping grouping = getCountryGrouping();
        List<BootstrapResponse.CountryDto> countries = new ArrayList<>(grouping.getAllCountries());

        Random random = new Random();
        Collections.shuffle(countries, random);

        int limit = Math.min(count, countries.size());
        int difficulty = calculateDifficulty(playerLvl);
        difficulty = 1;
        if (mode == 4) {
            for (int i = 0; i < limit; i++) {
                BootstrapResponse.CountryDto country = countries.get(i);

                GameQuestion question = createLanguageQuestion(country, random, language, grouping, difficulty);

                if (question != null) {
                    result.add(question);
                } else {
                    limit++;
                    if (limit > countries.size()) break;
                }
            }
        } else {
            for (int i = 0; i < limit; i++) {
                BootstrapResponse.CountryDto country = countries.get(i);
                GameQuestion question = createQuestion(country, mode, language, random, grouping, difficulty);
                result.add(question);
            }
        }

        return result;
    }
    private GameQuestion createQuestion(BootstrapResponse.CountryDto country,
                                        int mode,
                                        String language,
                                        Random random,
                                        CountryGrouping grouping,
                                        int difficulty) {
        String questionText = "";
        List<String> options = new ArrayList<>();
        String mediaUrl = null;

        String countryName = language.equals("ru") ? country.getName().getRu() : country.getName().getEn();
        String capital = language.equals("ru") ? country.getCapital().getRu() : country.getCapital().getEn();

        List<BootstrapResponse.CountryDto> wrongCountries = getWrongCountriesByDifficulty(
                country, grouping, random, difficulty, 3
        );

        switch (mode) {
            case 1:
                questionText = language.equals("ru") ?
                        "Столица страны " + countryName + "?" :
                        "Capital of " + countryName + "?";
                options.add(capital);
                for (BootstrapResponse.CountryDto other : wrongCountries) {
                    options.add(language.equals("ru")
                            ? other.getCapital().getRu()
                            : other.getCapital().getEn());
                }
                break;

            case 2:
                questionText = language.equals("ru")
                        ? "Какой стране принадлежит этот флаг?"
                        : "Which country does this flag belong to?";
                mediaUrl = country.getFlagImage();
                options.add(countryName);
                for (BootstrapResponse.CountryDto other : wrongCountries) {
                    options.add(language.equals("ru")
                            ? other.getName().getRu()
                            : other.getName().getEn());
                }
                break;

            case 3:
                questionText = language.equals("ru") ?
                        "Какой стране принадлежит этот контур?" :
                        "Which country does this outline belong to?";
                mediaUrl = country.getOutlineImage();
                options.add(countryName);
                for (BootstrapResponse.CountryDto other : wrongCountries) {
                    options.add(language.equals("ru") ?
                            other.getName().getRu() : other.getName().getEn());
                }
                break;
        }

        Collections.shuffle(options, random);
        int correctIndex = options.indexOf(
                mode == 1 ? capital : countryName
        );
        int regionVal = getRegionValue(country.getRegion());

        return new GameQuestion(
                UUID.randomUUID().toString(),
                country.getId(),
                questionText,
                options,
                correctIndex,
                mediaUrl,
                mode,
                regionVal
        );
    }
    private GameQuestion createLanguageQuestion(BootstrapResponse.CountryDto country,
                                                Random random,
                                                String language,
                                                CountryGrouping grouping,
                                                int difficulty) {

        if (country.getLanguages() == null || country.getLanguages().isEmpty()) {
            return null;
        }

        BootstrapResponse.CountryDto.CountryLanguageDto selectedLanguage = country.getLanguages().get(random.nextInt(country.getLanguages().size()));

        boolean isCountryByLanguage = random.nextBoolean();

        String questionText;
        List<String> options = new ArrayList<>();
        String mediaUrl = null;
        String correctAnswer;

        if (isCountryByLanguage) {
            questionText = language.equals("ru")
                    ? "В какой стране говорят на " + selectedLanguage.getName().getRu() + "?"
                    : "In which country do they speak " + selectedLanguage.getName().getEn() + "?";

            correctAnswer = language.equals("ru")
                    ? country.getName().getRu()
                    : country.getName().getEn();

            mediaUrl = selectedLanguage.getAudioUrl();

            options.add(correctAnswer);

            List<BootstrapResponse.CountryDto> wrongCountries = getWrongCountriesByDifficultyForLanguage(
                    country, selectedLanguage, grouping, random, difficulty, 3
            );

            for (BootstrapResponse.CountryDto wrongCountry : wrongCountries) {
                options.add(language.equals("ru")
                        ? wrongCountry.getName().getRu()
                        : wrongCountry.getName().getEn());
            }

        } else {
            questionText = language.equals("ru")
                    ? "Какой язык является официальным в " + country.getName().getRu()  + "?"
                    : "Which language is official in " + country.getName().getEn() + "?";

            correctAnswer = language.equals("ru")
                    ? selectedLanguage.getName().getRu()
                    : selectedLanguage.getName().getEn();

            options.add(correctAnswer);

            List<BootstrapResponse.CountryDto.CountryLanguageDto> wrongLanguages =
                    getWrongLanguagesByDifficultyForCountry(
                            country, selectedLanguage, grouping, random, difficulty, 3
                    );

            for (BootstrapResponse.CountryDto.CountryLanguageDto wrongLanguage : wrongLanguages) {
                options.add(language.equals("ru")
                        ? wrongLanguage.getName().getRu()
                        : wrongLanguage.getName().getEn());
            }
        }

        Collections.shuffle(options, random);
        int correctIndex = options.indexOf(correctAnswer);

        int regionVal = getRegionValue(country.getRegion());

        return new GameQuestion(
                UUID.randomUUID().toString(),
                country.getId(),
                questionText,
                options,
                correctIndex,
                mediaUrl,
                4,
                regionVal
        );
    }
    private List<BootstrapResponse.CountryDto> getWrongCountriesByDifficulty(
            BootstrapResponse.CountryDto correctCountry,
            CountryGrouping grouping,
            Random random,
            int difficulty,
            int count ) {
        List<BootstrapResponse.CountryDto> wrongCountries = new ArrayList<>();
        switch (difficulty) {
            case 1:
                List<String> otherRegions = new ArrayList<>(grouping.getRegions());
                otherRegions.remove(correctCountry.getRegion());

                Collections.shuffle(otherRegions, random);

                for (String region : otherRegions) {
                    List<BootstrapResponse.CountryDto> candidates = grouping.getCountriesByRegion(region);
                    List<BootstrapResponse.CountryDto> filtered = new ArrayList<>();
                    for (BootstrapResponse.CountryDto c : candidates) {
                        if (!c.getId().equals(correctCountry.getId())) {
                            filtered.add(c);
                        }
                    }

                    if (!filtered.isEmpty()) {
                        BootstrapResponse.CountryDto selected = filtered.get(random.nextInt(filtered.size()));
                        wrongCountries.add(selected);
                    }

                    if (wrongCountries.size() >= count) break;
                }
                break;
            case 2:
                List<BootstrapResponse.CountryDto> allOther = new ArrayList<>();
                for (BootstrapResponse.CountryDto c : grouping.getAllCountries()) {
                    if (!c.getId().equals(correctCountry.getId())) {
                        allOther.add(c);
                    }
                }
                Collections.shuffle(allOther, random);
                wrongCountries = allOther.subList(0, Math.min(count, allOther.size()));
                break;
            case 3:
                List<BootstrapResponse.CountryDto> sameRegionCandidates = new ArrayList<>();
                for (BootstrapResponse.CountryDto c : grouping.getCountriesByRegion(correctCountry.getRegion())) {
                    if (!c.getId().equals(correctCountry.getId())) {
                        sameRegionCandidates.add(c);
                    }
                }
                Collections.shuffle(sameRegionCandidates, random);
                wrongCountries = sameRegionCandidates.subList(0, Math.min(count, sameRegionCandidates.size()));
                break;
        }
        return wrongCountries;
    }
    private List<BootstrapResponse.CountryDto> getWrongCountriesByDifficultyForLanguage(
            BootstrapResponse.CountryDto correctCountry,
            BootstrapResponse.CountryDto.CountryLanguageDto selectedLanguage,
            CountryGrouping grouping,
            Random random,
            int difficulty,
            int count) {

        List<BootstrapResponse.CountryDto> wrongCountries = new ArrayList<>();

        switch (difficulty) {
            case 1:
                List<String> otherRegions = new ArrayList<>(grouping.getRegions());
                otherRegions.remove(correctCountry.getRegion());

                Collections.shuffle(otherRegions, random);

                for (String region : otherRegions) {
                    List<BootstrapResponse.CountryDto> candidates = grouping.getCountriesByRegion(region);
                    List<BootstrapResponse.CountryDto> filtered = new ArrayList<>();
                    for (BootstrapResponse.CountryDto c : candidates) {
                        if (!c.getId().equals(correctCountry.getId()) && !hasLanguage(c, selectedLanguage)) {
                            filtered.add(c);
                        }
                    }

                    if (!filtered.isEmpty()) {
                        BootstrapResponse.CountryDto selected = filtered.get(random.nextInt(filtered.size()));
                        wrongCountries.add(selected);
                    }

                    if (wrongCountries.size() >= count) break;
                }
                break;

            case 2:
                List<BootstrapResponse.CountryDto> allOther = new ArrayList<>();
                for (BootstrapResponse.CountryDto c : grouping.getAllCountries()) {
                    if (!c.getId().equals(correctCountry.getId()) && !hasLanguage(c, selectedLanguage)) {
                        allOther.add(c);
                    }
                }
                Collections.shuffle(allOther, random);
                wrongCountries = allOther.subList(0, Math.min(count, allOther.size()));
                break;
            case 3:
                List<BootstrapResponse.CountryDto> sameRegionCandidates = new ArrayList<>();
                for (BootstrapResponse.CountryDto c : grouping.getCountriesByRegion(correctCountry.getRegion())) {
                    if (!c.getId().equals(correctCountry.getId()) && !hasLanguage(c, selectedLanguage)) {
                        sameRegionCandidates.add(c);
                    }
                }
                Collections.shuffle(sameRegionCandidates, random);
                wrongCountries = sameRegionCandidates.subList(0, Math.min(count, sameRegionCandidates.size()));
                break;
        }

        return wrongCountries;
    }
    private List<BootstrapResponse.CountryDto.CountryLanguageDto> getWrongLanguagesByDifficultyForCountry(
            BootstrapResponse.CountryDto correctCountry,
            BootstrapResponse.CountryDto.CountryLanguageDto selectedLanguage,
            CountryGrouping grouping,
            Random random,
            int difficulty,
            int count) {

        Set<String> correctCountryLanguageIds = new HashSet<>();
        if (correctCountry.getLanguages() != null) {
            for (BootstrapResponse.CountryDto.CountryLanguageDto lang : correctCountry.getLanguages()) {
                correctCountryLanguageIds.add(lang.getId());
            }
        }

        List<BootstrapResponse.CountryDto.CountryLanguageDto> wrongLanguages = new ArrayList<>();

        switch (difficulty) {
            case 1:
                for (String region : grouping.getRegions()) {
                    if (region.equals(correctCountry.getRegion())) continue;

                    for (BootstrapResponse.CountryDto c : grouping.getCountriesByRegion(region)) {
                        if (c.getLanguages() != null) {
                            for (BootstrapResponse.CountryDto.CountryLanguageDto lang : c.getLanguages()) {
                                if (!lang.getId().equals(selectedLanguage.getId()) &&
                                        !correctCountryLanguageIds.contains(lang.getId())) {
                                    wrongLanguages.add(lang);
                                }
                            }
                        }
                    }
                }
                break;

            case 2:
                for (BootstrapResponse.CountryDto c : grouping.getAllCountries()) {
                    if (!c.getId().equals(correctCountry.getId()) && c.getLanguages() != null) {
                        for (BootstrapResponse.CountryDto.CountryLanguageDto lang : c.getLanguages()) {
                            if (!lang.getId().equals(selectedLanguage.getId()) &&
                                    !correctCountryLanguageIds.contains(lang.getId())) {
                                wrongLanguages.add(lang);
                            }
                        }
                    }
                }
                break;

            case 3:
                for (BootstrapResponse.CountryDto c : grouping.getCountriesByRegion(correctCountry.getRegion())) {
                    if (!c.getId().equals(correctCountry.getId()) && c.getLanguages() != null) {
                        for (BootstrapResponse.CountryDto.CountryLanguageDto lang : c.getLanguages()) {
                            if (!lang.getId().equals(selectedLanguage.getId()) &&
                                    !correctCountryLanguageIds.contains(lang.getId())) {
                                wrongLanguages.add(lang);
                            }
                        }
                    }
                }
                break;
        }

        Set<String> uniqueIds = new HashSet<>();
        List<BootstrapResponse.CountryDto.CountryLanguageDto> uniqueLanguages = new ArrayList<>();
        for (BootstrapResponse.CountryDto.CountryLanguageDto lang : wrongLanguages) {
            if (!uniqueIds.contains(lang.getId())) {
                uniqueIds.add(lang.getId());
                uniqueLanguages.add(lang);
            }
        }

        Collections.shuffle(uniqueLanguages, random);
        return uniqueLanguages.subList(0, Math.min(count, uniqueLanguages.size()));
    }
    private boolean hasLanguage(BootstrapResponse.CountryDto country, BootstrapResponse.CountryDto.CountryLanguageDto language) {
        if (country.getLanguages() == null) return false;
        return country.getLanguages().stream()
                .anyMatch(lang -> lang.getId().equals(language.getId()));
    }
    private int calculateDifficulty(int playerLevel) {
        if (playerLevel <= 5) return 1;
        else if (playerLevel <= 10) return 2;
        else return 3;
    }
    private int getRegionValue(String region) {
        switch (region) {
            case "europe": return 1;
            case "asia": return 2;
            case "africa": return 3;
            case "america": return 4;
            default: return 5;
        }
    }

}