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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
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
    private static final String PENDING_GAMES_FILE = "pending_games.json";
    private static final String BOOTSTRAP_ASSETS_FILE = "bootstrap_data.json";

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

            if (isNetworkAvailable()) {
                Log.d(TAG, "Есть интернет, загружаем с сервера");
                loadDataFromServer(callback);
                return;
            }

            Log.d(TAG, "Нет интернета, загружаем из assets");
            loadDataFromAssets(callback);
            Log.d(TAG, "Загрузили");
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
                    saveBootstrapData(data, callback);
                } else {
                    Log.e(TAG, "Ошибка сервера. Код: " + response.code());
                    loadDataFromAssets(callback);
                }
            }

            @Override
            public void onFailure(Call<BootstrapResponse> call, Throwable t) {
                Log.e(TAG, "Сетевая ошибка", t);
                loadDataFromAssets(callback);
            }
        });
    }
    private void loadDataFromAssets(BootstrapCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Загрузка данных из assets...");

                if (!hasBootstrapAssets()) {
                    Log.e(TAG, "Файл bootstrap_data.json не найден в assets");
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Нет интернета и нет встроенных данных. Подключитесь к сети.")
                    );
                    return;
                }

                String json = loadJsonFromAssets(BOOTSTRAP_ASSETS_FILE);
                if (json == null || json.isEmpty()) {
                    Log.e(TAG, "Файл bootstrap_data.json пуст или не может быть прочитан");
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Ошибка чтения встроенных данных")
                    );
                    return;
                }

                BootstrapResponse data = gson.fromJson(json, BootstrapResponse.class);
                if (data == null) {
                    Log.e(TAG, "Ошибка парсинга bootstrap_data.json");
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError("Ошибка парсинга встроенных данных")
                    );
                    return;
                }

                Log.d(TAG, "Загружено из assets: " +
                        (data.getCountries() != null ? data.getCountries().size() : 0) + " стран, " +
                        (data.getQuestions() != null ? data.getQuestions().size() : 0) + " вопросов");

                saveBootstrapData(data, callback);

            } catch (Exception e) {
                Log.e(TAG, "Ошибка загрузки из assets", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onError("Ошибка загрузки встроенных данных: " + e.getMessage())
                );
            }
        });
    }
    private void saveBootstrapData(BootstrapResponse data, BootstrapCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Сохранение данных в БД...");
                dbHelper.saveBootstrapData(data);
                loadDataFromDatabase();

                Log.d(TAG, "Данные успешно сохранены");
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
    }
    private boolean hasBootstrapAssets() {
        try {
            context.getAssets().open(BOOTSTRAP_ASSETS_FILE).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    private String loadJsonFromAssets(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
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
                String json = gson.toJson(pendingGames);

                try (FileOutputStream fos = new FileOutputStream(file);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) {
                    osw.write(json);
                }

                Log.d(TAG, "Сохранено " + pendingGames.size() + " игр в JSON файл");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка сохранения игр в JSON", e);
            }
        });
    }
    public void clearPendingGames() {
        pendingGames.clear();
        try {
            File file = new File(context.getFilesDir(), PENDING_GAMES_FILE);
            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8")) {
                 osw.write(gson.toJson(new ArrayList<>()));
            }
            Log.d(TAG, "JSON файл очищен");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка очистки JSON файла", e);
        }
    }
    private List<PendingGame> loadPendingGames() {
        try {
            File file = new File(context.getFilesDir(), PENDING_GAMES_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            StringBuilder json = new StringBuilder();
            try (FileInputStream fis = new FileInputStream(file);
                 InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                 BufferedReader reader = new BufferedReader(isr)) {

                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }

            Type listType = new TypeToken<List<PendingGame>>(){}.getType();
            List<PendingGame> list = gson.fromJson(json.toString(), listType);

            Log.d(TAG, "Загружено " + (list != null ? list.size() : 0) + " игр из JSON файла");
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки игр из JSON", e);
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