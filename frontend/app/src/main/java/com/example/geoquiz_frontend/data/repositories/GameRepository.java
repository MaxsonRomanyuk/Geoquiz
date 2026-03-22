package com.example.geoquiz_frontend.data.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.geoquiz_frontend.data.remote.ApiClient;
import com.example.geoquiz_frontend.data.remote.ApiService;
import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.data.remote.dtos.solo.FinishGameRequest;
import com.example.geoquiz_frontend.data.remote.dtos.solo.SyncGameSessionRequest;
import com.example.geoquiz_frontend.data.local.DatabaseHelper;
import com.example.geoquiz_frontend.domain.entities.GameQuestion;
import com.example.geoquiz_frontend.domain.entities.GameSession;
import com.example.geoquiz_frontend.domain.entities.PendingGame;
import com.example.geoquiz_frontend.domain.enums.GameMode;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GameRepository {
    private static final String TAG = "GameRepository";
    private static final String PENDING_GAMES_FILE = "pending_games.dat";

    private final Context context;
    private final PreferencesHelper preferencesHelper;
    private final ApiService apiService;
    private final ApiService apiServiceWithAuth;
    private final DatabaseHelper dbHelper;
    private final Gson gson;
    private final ExecutorService executorService;

    private List<PendingGame> pendingGames;
    private Map<String, BootstrapResponse.CountryDto> countriesCache;
    private Map<Integer, List<BootstrapResponse.QuestionDto>> questionsCache;

    public GameRepository(Context context) {
        this.context = context.getApplicationContext();
        this.preferencesHelper = new PreferencesHelper(context);
        this.apiService = ApiClient.getApi();
        this.apiServiceWithAuth = ApiClient.getApiWithAuth(preferencesHelper);
        this.dbHelper = new DatabaseHelper(context);
        this.gson = new Gson();
        this.executorService = Executors.newSingleThreadExecutor();
        this.pendingGames = loadPendingGames();
        this.countriesCache = new HashMap<>();
        this.questionsCache = new HashMap<>();
    }

    public void loadBootstrapData(BootstrapCallback callback) {
        if (!countriesCache.isEmpty() && !questionsCache.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        executorService.execute(() -> {
            if (dbHelper.hasData()) {
                Log.d(TAG, "Данные найдены в локальной БД");
                loadDataFromDatabase();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onSuccess(null)
                );
                return;
            }

            if (!isNetworkAvailable()) {
                Log.e(TAG, "Нет интернета и нет локальных данных");
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Нет интернета. Подключитесь к сети для первого запуска.")
                );
                return;
            }

            Log.d(TAG, "Загрузка данных с сервера");
            loadDataFromServer(callback);
        });
    }

    private void loadDataFromServer(BootstrapCallback callback) {
        Log.d(TAG, "Отправка запроса на сервер...");

        apiService.bootstrap().enqueue(new Callback<BootstrapResponse>() {
            @Override
            public void onResponse(Call<BootstrapResponse> call, Response<BootstrapResponse> response) {
                Log.d(TAG, "Получен ответ. Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    BootstrapResponse data = response.body();

                    Log.d(TAG, "Получено стран: " +(data.getCountries() != null ? data.getCountries().size() : 0));
                    Log.d(TAG, "Получено вопросов: " + (data.getQuestions() != null ? data.getQuestions().size() : 0));

                    if (data.getCountries() == null || data.getCountries().isEmpty()) {
                        Log.e(TAG, "СЕРВЕР ВЕРНУЛ ПУСТОЙ СПИСОК СТРАН!");
                    }

                    if (data.getQuestions() == null || data.getQuestions().isEmpty()) {
                        Log.e(TAG, "СЕРВЕР ВЕРНУЛ ПУСТОЙ СПИСОК ВОПРОСОВ!");
                    }

                    executorService.execute(() -> {
                        try {
                            dbHelper.saveBootstrapData(data);
                            loadDataFromDatabase();

                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onSuccess(data)
                            );
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка сохранения в БД", e);
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onError("Ошибка сохранения: " + e.getMessage())
                            );
                        }
                    });
                } else {
                    Log.e(TAG, "Ошибка сервера. Код: " + response.code());
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "null";
                        Log.e(TAG, "Тело ошибки: " + errorBody);
                    } catch (Exception e) {
                        Log.e(TAG, "Не удалось прочитать тело ошибки");
                    }

                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Ошибка загрузки: " + response.code())
                    );
                }
            }

            @Override
            public void onFailure(Call<BootstrapResponse> call, Throwable t) {
                Log.e(TAG, "Сетевая ошибка", t);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Ошибка сети: " + t.getMessage())
                );
            }
        });
    }

    private void loadDataFromDatabase() {
        countriesCache.clear();
        questionsCache.clear();

        List<BootstrapResponse.CountryDto> countries = dbHelper.getAllCountries();
        for (BootstrapResponse.CountryDto country : countries) {
            countriesCache.put(country.getId(), country);
        }

        for (GameMode mode : GameMode.values()) {
            List<BootstrapResponse.QuestionDto> questions = dbHelper.getQuestionsByType(mode.getValue());
            questionsCache.put(mode.getValue(), questions);
        }

        Log.d(TAG, String.format(Locale.US,
                "Загружено %d стран и %d вопросов",
                countriesCache.size(),
                questionsCache.values().stream().mapToInt(List::size).sum()));
    }

    public List<GameQuestion> getQuestionsForMode(int mode, int count) {
        List<GameQuestion> result = new ArrayList<>();

        List<BootstrapResponse.QuestionDto> questionDtos = questionsCache.get(mode);
        if (questionDtos == null || questionDtos.isEmpty()) {
            Log.e(TAG, "Нет вопросов для режима: " + mode);
            return result;
        }

        String language = preferencesHelper.getLanguage();

        List<BootstrapResponse.QuestionDto> shuffled = new ArrayList<>(questionDtos);
        Collections.shuffle(shuffled);

        int limit = Math.min(count, shuffled.size());
        for (int i = 0; i < limit; i++) {
            BootstrapResponse.QuestionDto q = shuffled.get(i);
            BootstrapResponse.CountryDto country = countriesCache.get(q.getCountryId());

            if (country != null) {
                GameQuestion question = createQuestion(q, country, mode, language);
                result.add(question);
            }
        }

        Log.d(TAG, String.format(Locale.US,
                "Сгенерировано %d вопросов для режима %s на языке %s",
                result.size(), mode, language));

        return result;
    }

    private GameQuestion createQuestion(BootstrapResponse.QuestionDto q, BootstrapResponse.CountryDto country, int mode, String language) {
        String questionText = "";
        List<String> options = new ArrayList<>();
        String mediaUrl = null;

        String countryName = language.equals("ru") ?
                country.getName().getRu() : country.getName().getEn();
        String capital = language.equals("ru") ?
                country.getCapital().getRu() : country.getCapital().getEn();

        String region = country.getRegion();

        List<BootstrapResponse.CountryDto> otherCountries = getRandomCountries(3, country.getId(), language);

        switch (mode) {
            case 1:
                questionText = language.equals("ru") ?
                        "Столица страны " + countryName + "?" :
                        "Capital of " + countryName + "?";
                options.add(capital);
                for (BootstrapResponse.CountryDto other : otherCountries) {
                    options.add(language.equals("ru") ?
                            other.getCapital().getRu() : other.getCapital().getEn());
                }
                break;

            case 2:
                questionText = language.equals("ru") ?
                        "Какой стране принадлежит этот флаг?" :
                        "Which country does this flag belong to?";
                mediaUrl = country.getFlagImage();
                options.add(countryName);
                for (BootstrapResponse.CountryDto other : otherCountries) {
                    options.add(language.equals("ru") ?
                            other.getName().getRu() : other.getName().getEn());
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

            case 4:
                questionText = language.equals("ru") ?
                        "Язык какой страны звучит?" :
                        "Which country's language is this?";
                mediaUrl = country.getLanguageAudio();
                options.add(countryName);
                for (BootstrapResponse.CountryDto other : otherCountries) {
                    options.add(language.equals("ru") ?
                            other.getName().getRu() : other.getName().getEn());
                }
                break;
        }

        int regionVal = 0;
        switch (region) {
            case "Европа":
                regionVal = 1;
                break;
            case "Азия":
                regionVal = 2;
                break;
            case "Африка":
                regionVal = 3;
                break;
            case "Америка":
                regionVal = 4;
                break;
            default:
                regionVal = 5;
                break;
        }

        Collections.shuffle(options);
        int correctIndex = options.indexOf(
                mode == 1 ? capital : countryName
        );

        return new GameQuestion(
                q.getId(),
                country.getId(),
                questionText,
                options,
                correctIndex,
                mediaUrl,
                mode,
                regionVal
        );
    }

    private List<BootstrapResponse.CountryDto> getRandomCountries(int count, String excludeId, String language) {
        List<BootstrapResponse.CountryDto> allCountries = new ArrayList<>(countriesCache.values());
        List<BootstrapResponse.CountryDto> result = new ArrayList<>();

        allCountries.removeIf(c -> c.getId().equals(excludeId));

        Collections.shuffle(allCountries);

        int limit = Math.min(count, allCountries.size());
        for (int i = 0; i < limit; i++) {
            result.add(allCountries.get(i));
        }

        return result;
    }

    public void saveGameSession(GameSession session) {
        PendingGame pendingGame = new PendingGame(session);
        pendingGames.add(pendingGame);
        savePendingGames();

        Log.d(TAG, "Игра сохранена локально. ID: " + session.getId() +
                ", счет: " + session.getScore());

        if (preferencesHelper.hasValidToken()) {
            if (pendingGames.size() > 1) syncPendingGames();
            else {
                FinishGameRequest fgr = new FinishGameRequest(
                        pendingGame.getModeValue(),
                        pendingGame.getTotalQuestions(),
                        pendingGame.getCorrectAnswers(),
                        pendingGame.getEuropeCorrect(),
                        pendingGame.getAsiaCorrect(),
                        pendingGame.getAfricaCorrect(),
                        pendingGame.getAmericaCorrect(),
                        pendingGame.getOceaniaCorrect(),
                        pendingGame.getScore(),
                        pendingGame.getTimeSpent(),
                        pendingGame.isOnline()
                );
                apiServiceWithAuth.finishGame(fgr).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            clearPendingGames();
                            Log.d(TAG, "Игра сохранена");
                        } else {
                            Log.e(TAG, "Ошибка сохранения: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Ошибка сети при сохранении", t);
                    }
                });
            }
        }
    }

    public void syncPendingGames() {
        if (pendingGames.isEmpty()) {
            Log.d(TAG, "Нет игр для синхронизации");
            return;
        }

        if (!preferencesHelper.hasValidToken()) {
            Log.d(TAG, "Нет токена, синхронизация отложена");
            return;
        }

        //if (!isNetworkAvailable()) {
          //  Log.d(TAG, "Нет интернета, синхронизация отложена");
            //return;
        //}

        List<SyncGameSessionRequest> syncList = new ArrayList<>();

        for (PendingGame game : pendingGames) {
            SyncGameSessionRequest syncRequest = new SyncGameSessionRequest(
                    UUID.randomUUID(),
                    game.getModeValue(),
                    game.getTotalQuestions(),
                    game.getCorrectAnswers(),
                    game.getEuropeCorrect(),
                    game.getAsiaCorrect(),
                    game.getAfricaCorrect(),
                    game.getAmericaCorrect(),
                    game.getOceaniaCorrect(),
                    game.getScore(),
                    game.getTimeSpent(),
                    game.isOnline(),
                    game.getPlayedAt()
            );
            syncList.add(syncRequest);
        }

        Log.d(TAG, "Отправка " + syncList.size() + " игр на сервер");

        apiServiceWithAuth.syncGames(syncList).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "Код ответа sync: " + response.code());
                if (response.isSuccessful()) {
                    clearPendingGames();
                    Log.d(TAG, "Игры успешно синхронизированы");
                } else {
                    Log.e(TAG, "Ошибка синхронизации: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Ошибка сети при синхронизации", t);
            }
        });
    }

    private void savePendingGames() {
        executorService.execute(() -> {
            try {
                File file = new File(context.getFilesDir(), PENDING_GAMES_FILE);
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(gson.toJson(pendingGames));
                oos.close();
                Log.d(TAG, "Сохранено " + pendingGames.size() + " игр в кэш");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения игр", e);
            }
        });
    }
    public void clearPendingGames() {
        pendingGames.clear();
        try {
            File file = new File(context.getFilesDir(), PENDING_GAMES_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(gson.toJson(new ArrayList<>()));
            oos.close();
            Log.d(TAG, "Файл очищен");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка очистки", e);
        }


        //pendingGames = loadPendingGames();
    }
    private List<PendingGame> loadPendingGames() {
        try {
            File file = new File(context.getFilesDir(), PENDING_GAMES_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            String json = (String) ois.readObject();
            ois.close();

            Type listType = new TypeToken<List<PendingGame>>(){}.getType();
            List<PendingGame> list = gson.fromJson(json, listType);
            Log.d(TAG, "Загружено " + (list != null ? list.size() : 0) + " игр из кэша");
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки игр", e);
            return new ArrayList<>();
        }
    }

    private boolean isNetworkAvailable() {
        try {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }


    public interface BootstrapCallback {
        void onSuccess(BootstrapResponse data);
        void onError(String error);
    }
}