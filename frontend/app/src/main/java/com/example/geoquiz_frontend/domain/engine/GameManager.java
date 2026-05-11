package com.example.geoquiz_frontend.domain.engine;

import android.content.Context;

import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.data.repositories.GameRepository;
import com.example.geoquiz_frontend.domain.entities.GameQuestion;
import com.example.geoquiz_frontend.domain.entities.GameSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GameManager {
    private static GameManager instance;
    private GameRepository repository;

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
    public List<GameQuestion> getQuestionsForMode(int mode, int count, String language) {
        List<GameQuestion> result = new ArrayList<>();

        Map<String, BootstrapResponse.CountryDto> countriesCache = repository.getCountriesCache();
        List<BootstrapResponse.CountryDto> countries =  new ArrayList<>(countriesCache.values());
        Random random = new Random();
        Collections.shuffle(countries, random);

        int limit = Math.min(count, countries.size());
        if (mode == 4) {
            for (int i = 0; i < limit; i++) {
                BootstrapResponse.CountryDto country = countries.get(i);

                GameQuestion question = createLanguageQuestion(country, random, language, countries);

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
                GameQuestion question = createQuestion(country, mode, language, random, countries);
                result.add(question);
            }
        }

        return result;
    }
    private GameQuestion createLanguageQuestion(BootstrapResponse.CountryDto country,
                                                Random random,
                                                String language,
                                                List<BootstrapResponse.CountryDto> allCountries) {

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

            List<BootstrapResponse.CountryDto> otherCountries = allCountries.stream()
                    .filter(c -> !c.getId().equals(country.getId()))
                    .filter(c -> c.getLanguages() == null ||
                            c.getLanguages().stream().noneMatch(l -> l.getId().equals(selectedLanguage.getId())))
                    .collect(Collectors.toList());

            Collections.shuffle(otherCountries, random);
            List<BootstrapResponse.CountryDto> wrongCountries = otherCountries.stream()
                    .limit(3)
                    .collect(Collectors.toList());

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

            Set<String> countryLanguageIds = country.getLanguages().stream()
                    .map(BootstrapResponse.CountryDto.CountryLanguageDto::getId)
                    .collect(Collectors.toSet());

            List<BootstrapResponse.CountryDto.CountryLanguageDto> otherLanguages = allCountries.stream()
                    .flatMap(c -> c.getLanguages().stream())
                    .filter(l -> !l.getId().equals(selectedLanguage.getId()))
                    .filter(l -> !countryLanguageIds.contains(l.getId()))
                    .distinct()
                    .collect(Collectors.toList());

            Collections.shuffle(otherLanguages, random);
            List<BootstrapResponse.CountryDto.CountryLanguageDto> wrongLanguages = otherLanguages.stream()
                    .limit(3)
                    .collect(Collectors.toList());

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
    private GameQuestion createQuestion(BootstrapResponse.CountryDto country,
                                        int mode,
                                        String language,
                                        Random random,
                                        List<BootstrapResponse.CountryDto> allCountries) {
        String questionText = "";
        List<String> options = new ArrayList<>();
        String mediaUrl = null;

        String countryName = language.equals("ru") ? country.getName().getRu() : country.getName().getEn();
        String capital = language.equals("ru") ? country.getCapital().getRu() : country.getCapital().getEn();
        String region = country.getRegion();

        //List<BootstrapResponse.CountryDto> otherCountries = getRandomCountries(3, country.getId());
        List<BootstrapResponse.CountryDto> otherCountries = allCountries.stream()
                .filter(c -> !c.getId().equals(country.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(otherCountries, random);
        otherCountries = otherCountries.stream().limit(3).collect(Collectors.toList());

        switch (mode) {
            case 1:
                questionText = language.equals("ru") ?
                        "Столица страны " + countryName + "?" :
                        "Capital of " + countryName + "?";
                options.add(capital);
                for (BootstrapResponse.CountryDto other : otherCountries) {
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
                for (BootstrapResponse.CountryDto other : otherCountries) {
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
                for (BootstrapResponse.CountryDto other : otherCountries) {
                    options.add(language.equals("ru") ?
                            other.getName().getRu() : other.getName().getEn());
                }
                break;
        }

        int regionVal = 0;
        switch (region) {
            case "europe":
                regionVal = 1;
                break;
            case "asia":
                regionVal = 2;
                break;
            case "africa":
                regionVal = 3;
                break;
            case "america":
                regionVal = 4;
                break;
            default:
                regionVal = 5;
                break;
        }

        Collections.shuffle(options, random);
        int correctIndex = options.indexOf(
                mode == 1 ? capital : countryName
        );

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