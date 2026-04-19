package com.example.geoquiz_frontend.data.local;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.geoquiz_frontend.data.remote.dtos.profile.ProfileResponse;
import com.example.geoquiz_frontend.data.remote.dtos.solo.BootstrapResponse;
import com.example.geoquiz_frontend.domain.entities.Achievement;
import com.example.geoquiz_frontend.domain.entities.UserStats;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "GameDatabaseHelper";
    private static final String DATABASE_NAME = "geoquiz.db";
    private static final int DATABASE_VERSION = 7;


    private static final String TABLE_COUNTRIES = "countries";
    private static final String COLUMN_COUNTRY_ID = "_id";
    private static final String COLUMN_COUNTRY_NAME_RU = "name_ru";
    private static final String COLUMN_COUNTRY_NAME_EN = "name_en";
    private static final String COLUMN_COUNTRY_CAPITAL_RU = "capital_ru";
    private static final String COLUMN_COUNTRY_CAPITAL_EN = "capital_en";
    private static final String COLUMN_COUNTRY_REGION = "region";
    private static final String COLUMN_COUNTRY_FLAG_IMAGE = "flag_image";
    private static final String COLUMN_COUNTRY_OUTLINE_IMAGE = "outline_image";
    private static final String COLUMN_COUNTRY_LANGUAGE_AUDIO = "language_audio";



    private static final String TABLE_QUESTIONS = "questions";
    private static final String COLUMN_QUESTION_ID = "_id";
    private static final String COLUMN_QUESTION_COUNTRY_ID = "country_id";
    private static final String COLUMN_QUESTION_DIFFICULTY = "difficulty";
    private static final String COLUMN_QUESTION_TYPE = "type";



    private static final String TABLE_USER_STATS = "user_stats";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_GAMES_PLAYED = "games_played";
    private static final String COLUMN_GAMES_WON = "games_won";
    private static final String COLUMN_WIN_RATE = "win_rate";
    private static final String COLUMN_LEVEL = "level";
    private static final String COLUMN_EXPERIENCE = "experience";
    private static final String COLUMN_SCORE = "score";
    private static final String COLUMN_DAILY_STREAK = "daily_streak";
    private static final String COLUMN_WIN_STREAK = "win_streak";
    private static final String COLUMN_EUROPE_CORRECT = "europe_correct";
    private static final String COLUMN_ASIA_CORRECT = "asia_correct";
    private static final String COLUMN_AFRICA_CORRECT = "africa_correct";
    private static final String COLUMN_AMERICA_CORRECT = "america_correct";
    private static final String COLUMN_OCEANIA_CORRECT = "oceania_correct";
    private static final String COLUMN_BEST_CONTINENT = "best_continent";
    private static final String COLUMN_CAPITALS_CORRECT = "capitals_correct";
    private static final String COLUMN_FLAGS_CORRECT = "flags_correct";
    private static final String COLUMN_OUTLINES_CORRECT = "outlines_correct";
    private static final String COLUMN_LANGUAGES_CORRECT = "languages_correct";



    private static final String TABLE_USER_ACHIEVEMENTS = "user_achievements";
    private static final String COLUMN_UID = "user_id";
    private static final String COLUMN_CODE = "code";
    private static final String COLUMN_PROGRESS = "progress";
    private static final String COLUMN_RARITY = "rarity";
    private static final String COLUMN_IS_UNLOCKED = "is_unlocked";
    private static final String COLUMN_UNLOCKED_AT = "unlocked_at";
    private final Gson gson = new Gson();
    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Создание базы данных");

        String createCountriesTable = "CREATE TABLE " + TABLE_COUNTRIES + "("
                + COLUMN_COUNTRY_ID + " TEXT PRIMARY KEY,"
                + COLUMN_COUNTRY_NAME_RU + " TEXT NOT NULL,"
                + COLUMN_COUNTRY_NAME_EN + " TEXT NOT NULL,"
                + COLUMN_COUNTRY_CAPITAL_RU + " TEXT NOT NULL,"
                + COLUMN_COUNTRY_CAPITAL_EN + " TEXT NOT NULL,"
                + COLUMN_COUNTRY_REGION + " TEXT,"
                + COLUMN_COUNTRY_FLAG_IMAGE + " TEXT,"
                + COLUMN_COUNTRY_OUTLINE_IMAGE + " TEXT,"
                + COLUMN_COUNTRY_LANGUAGE_AUDIO + " TEXT)";
        db.execSQL(createCountriesTable);

        String createQuestionsTable = "CREATE TABLE " + TABLE_QUESTIONS + "("
                + COLUMN_QUESTION_ID + " TEXT PRIMARY KEY,"
                + COLUMN_QUESTION_COUNTRY_ID + " TEXT NOT NULL,"
                + COLUMN_QUESTION_DIFFICULTY + " INTEGER NOT NULL,"
                + COLUMN_QUESTION_TYPE + " INTEGER NOT NULL,"
                + "FOREIGN KEY(" + COLUMN_QUESTION_COUNTRY_ID + ") REFERENCES "
                + TABLE_COUNTRIES + "(" + COLUMN_COUNTRY_ID + "))";
        db.execSQL(createQuestionsTable);

        String createUserStatsTable = "CREATE TABLE " + TABLE_USER_STATS + "("
                + COLUMN_USER_ID + " TEXT PRIMARY KEY,"
                + COLUMN_GAMES_PLAYED + " INTEGER DEFAULT 0,"
                + COLUMN_GAMES_WON + " INTEGER DEFAULT 0,"
                + COLUMN_WIN_RATE + " REAL DEFAULT 0,"
                + COLUMN_LEVEL + " INTEGER DEFAULT 1,"
                + COLUMN_EXPERIENCE + " INTEGER DEFAULT 0,"
                + COLUMN_SCORE + " INTEGER DEFAULT 0,"
                + COLUMN_DAILY_STREAK + " INTEGER DEFAULT 0,"
                + COLUMN_WIN_STREAK + " INTEGER DEFAULT 0,"
                + COLUMN_EUROPE_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_ASIA_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_AFRICA_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_AMERICA_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_OCEANIA_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_BEST_CONTINENT + " TEXT,"
                + COLUMN_CAPITALS_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_FLAGS_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_OUTLINES_CORRECT + " INTEGER DEFAULT 0,"
                + COLUMN_LANGUAGES_CORRECT + " INTEGER DEFAULT 0)";
        db.execSQL(createUserStatsTable);


        String createUserAchievementsTable = "CREATE TABLE " + TABLE_USER_ACHIEVEMENTS + "("
                + COLUMN_UID + " TEXT NOT NULL,"
                + COLUMN_CODE + " TEXT NOT NULL,"
                + COLUMN_PROGRESS + " INTEGER DEFAULT 1,"
                + COLUMN_RARITY + " INTEGER DEFAULT 1,"
                + COLUMN_IS_UNLOCKED + " INTEGER DEFAULT 0,"
                + COLUMN_UNLOCKED_AT + " TEXT NOT NULL,"
                + "PRIMARY KEY (" + COLUMN_UID + ", " + COLUMN_CODE + "))";
        db.execSQL(createUserAchievementsTable);

        db.execSQL("CREATE INDEX idx_questions_type ON " + TABLE_QUESTIONS + "(" + COLUMN_QUESTION_TYPE + ")");
        db.execSQL("CREATE INDEX idx_questions_country ON " + TABLE_QUESTIONS + "(" + COLUMN_QUESTION_COUNTRY_ID + ")");
        db.execSQL("CREATE INDEX idx_user_stats_user ON " + TABLE_USER_STATS + "(" + COLUMN_USER_ID + ")");
        db.execSQL("CREATE INDEX idx_user_achievements ON " + TABLE_USER_ACHIEVEMENTS + "(" + COLUMN_UID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Обновление БД с версии " + oldVersion + " до " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_STATS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_ACHIEVEMENTS);
        onCreate(db);
    }

    public void saveBootstrapData(BootstrapResponse data) {
        Log.d(TAG, "Начало сохранения в БД");
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            int deletedCountries = db.delete(TABLE_COUNTRIES, null, null);
            int deletedQuestions = db.delete(TABLE_QUESTIONS, null, null);

            if (data.getCountries() != null) {
                for (BootstrapResponse.CountryDto country : data.getCountries()) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_COUNTRY_ID, country.getId());
                    values.put(COLUMN_COUNTRY_NAME_RU, country.getName().getRu());
                    values.put(COLUMN_COUNTRY_NAME_EN, country.getName().getEn());
                    values.put(COLUMN_COUNTRY_CAPITAL_RU, country.getCapital().getRu());
                    values.put(COLUMN_COUNTRY_CAPITAL_EN, country.getCapital().getEn());
                    values.put(COLUMN_COUNTRY_REGION, country.getRegion());
                    values.put(COLUMN_COUNTRY_FLAG_IMAGE, country.getFlagImage());
                    values.put(COLUMN_COUNTRY_OUTLINE_IMAGE, country.getOutlineImage());
                    values.put(COLUMN_COUNTRY_LANGUAGE_AUDIO, country.getLanguageAudio());

                    long id = db.insert(TABLE_COUNTRIES, null, values);
                    Log.d(TAG, "Сохранена страна: " + country.getId() + ", rowId=" + id);
                }
                Log.d(TAG, "Сохранено стран: " + data.getCountries().size());
            }

            if (data.getQuestions() != null) {
                for (BootstrapResponse.QuestionDto question : data.getQuestions()) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_QUESTION_ID, question.getId());
                    values.put(COLUMN_QUESTION_COUNTRY_ID, question.getCountryId());
                    values.put(COLUMN_QUESTION_DIFFICULTY, question.getDifficulty());
                    values.put(COLUMN_QUESTION_TYPE, question.getType());

                    long id = db.insert(TABLE_QUESTIONS, null, values);
                    Log.d(TAG, "Сохранен вопрос: " + question.getId() + ", type=" + question.getType() + ", rowId=" + id);
                }
                Log.d(TAG, "Сохранено вопросов: " + data.getQuestions().size());
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения данных", e);
        } finally {
            db.endTransaction();
            Log.d(TAG, "Транзакция завершена");
        }
    }

    public List<BootstrapResponse.CountryDto> getAllCountries() {
        List<BootstrapResponse.CountryDto> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_COUNTRIES, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            BootstrapResponse.CountryDto country = cursorToCountry(cursor);
            list.add(country);
        }
        cursor.close();

        return list;
    }

    public BootstrapResponse.CountryDto getCountry(String id) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_COUNTRIES, null,
                COLUMN_COUNTRY_ID + "=?", new String[]{id},
                null, null, null);

        BootstrapResponse.CountryDto country = null;
        if (cursor.moveToFirst()) {
            country = cursorToCountry(cursor);
        }
        cursor.close();

        return country;
    }

    public List<BootstrapResponse.QuestionDto> getQuestionsByType(int type) {
        List<BootstrapResponse.QuestionDto> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_QUESTIONS, null,
                COLUMN_QUESTION_TYPE + "=?", new String[]{String.valueOf(type)},
                null, null, null);

        while (cursor.moveToNext()) {
            BootstrapResponse.QuestionDto question = cursorToQuestion(cursor);
            list.add(question);
        }
        cursor.close();

        return list;
    }
    public List<BootstrapResponse.QuestionDto> getAllQuestions() {
        List<BootstrapResponse.QuestionDto> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_QUESTIONS, null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            BootstrapResponse.QuestionDto question = cursorToQuestion(cursor);
            list.add(question);
        }
        cursor.close();

        return list;
    }

    public boolean hasData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COUNTRIES, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count > 0;
    }
    @SuppressLint("Range")
    private BootstrapResponse.CountryDto cursorToCountry(Cursor cursor) {
        BootstrapResponse.CountryDto country = new BootstrapResponse.CountryDto();
        BootstrapResponse.CountryDto.NameDto name = new BootstrapResponse.CountryDto.NameDto();
        BootstrapResponse.CountryDto.CapitalDto capital = new BootstrapResponse.CountryDto.CapitalDto();

        country.setId(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_ID)));

        name.setRu(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_NAME_RU)));
        name.setEn(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_NAME_EN)));
        country.setName(name);

        capital.setRu(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_CAPITAL_RU)));
        capital.setEn(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_CAPITAL_EN)));
        country.setCapital(capital);

        country.setRegion(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_REGION)));
        country.setFlagImage(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_FLAG_IMAGE)));
        country.setOutlineImage(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_OUTLINE_IMAGE)));
        country.setLanguageAudio(cursor.getString(cursor.getColumnIndex(COLUMN_COUNTRY_LANGUAGE_AUDIO)));

        return country;
    }
    @SuppressLint("Range")
    private BootstrapResponse.QuestionDto cursorToQuestion(Cursor cursor) {
        BootstrapResponse.QuestionDto question = new BootstrapResponse.QuestionDto();

        question.setId(cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION_ID)));
        question.setCountryId(cursor.getString(cursor.getColumnIndex(COLUMN_QUESTION_COUNTRY_ID)));
        question.setDifficulty(cursor.getInt(cursor.getColumnIndex(COLUMN_QUESTION_DIFFICULTY)));
        question.setType(cursor.getInt(cursor.getColumnIndex(COLUMN_QUESTION_TYPE)));

        return question;
    }

    public void saveUserStats(UserStats stats) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_ID, stats.getUserId());
        values.put(COLUMN_GAMES_PLAYED, stats.getGamesPlayed());
        values.put(COLUMN_GAMES_WON, stats.getGamesWon());
        values.put(COLUMN_WIN_RATE, stats.getWinRate());
        values.put(COLUMN_LEVEL, stats.getLevel());
        values.put(COLUMN_EXPERIENCE, stats.getExperience());
        values.put(COLUMN_SCORE, stats.getScore());
        values.put(COLUMN_DAILY_STREAK, stats.getDailyStreak());
        values.put(COLUMN_WIN_STREAK, stats.getWinStreak());
        values.put(COLUMN_EUROPE_CORRECT, stats.getEuropeCorrect());
        values.put(COLUMN_ASIA_CORRECT, stats.getAsiaCorrect());
        values.put(COLUMN_AFRICA_CORRECT, stats.getAfricaCorrect());
        values.put(COLUMN_AMERICA_CORRECT, stats.getAmericaCorrect());
        values.put(COLUMN_OCEANIA_CORRECT, stats.getOceaniaCorrect());
        values.put(COLUMN_BEST_CONTINENT, stats.getBestContinent());
        values.put(COLUMN_CAPITALS_CORRECT, stats.getCapitalsCorrect());
        values.put(COLUMN_FLAGS_CORRECT, stats.getFlagsCorrect());
        values.put(COLUMN_OUTLINES_CORRECT, stats.getOutlinesCorrect());
        values.put(COLUMN_LANGUAGES_CORRECT, stats.getLanguagesCorrect());

        long result = db.insertWithOnConflict(TABLE_USER_STATS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "Сохранена статистика для пользователя " + stats.getUserId() + ": " + result);
    }
    public UserStats getUserStats(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_USER_STATS, null,
                COLUMN_USER_ID + "=?", new String[]{userId},
                null, null, null);

        UserStats stats = null;
        if (cursor.moveToFirst()) {
            stats = cursorToUserStats(cursor);
        }
        cursor.close();

        return stats;
    }
    public void updateStatsAfterGame
            (String userId, int mode, boolean won, int correctAnswers, int xpGained,
             int correctEu, int correctAs, int correctAf, int correctAm, int correctOc) {
        UserStats stats = getUserStats(userId);
        if (stats == null) {
            stats = new UserStats(userId);
        }

        stats.setGamesPlayed(stats.getGamesPlayed() + 1);
        if (won) {
            stats.setGamesWon(stats.getGamesWon() + 1);
            stats.setWinStreak(stats.getWinStreak() + 1);

            stats.setExperience(stats.getExperience() + xpGained);
            stats.setScore(stats.getScore() + xpGained);
            int exp = stats.getExperience();
            int expToNextLevel = stats.getLevel()*100;
            if (exp > expToNextLevel)
            {
                stats.setExperience(exp-expToNextLevel);
                stats.setLevel(stats.getLevel()+1);
            }
        } else {
            stats.setWinStreak(0);
        }

        stats.setWinRate((float) stats.getGamesWon() / stats.getGamesPlayed() * 100);


        if (correctAnswers>0)
        {
            stats.setEuropeCorrect(stats.getEuropeCorrect() + correctEu);
            stats.setAsiaCorrect(stats.getAsiaCorrect() + correctAs);
            stats.setAfricaCorrect(stats.getAfricaCorrect() + correctAf);
            stats.setAmericaCorrect(stats.getAmericaCorrect() + correctAm);
            stats.setOceaniaCorrect(stats.getOceaniaCorrect() + correctOc);

            updateBestContinent(userId);
        }

        switch (mode) {
            case 1:
                stats.setCapitalsCorrect(stats.getCapitalsCorrect() + correctAnswers);
                break;
            case 2:
                stats.setFlagsCorrect(stats.getFlagsCorrect() + correctAnswers);
                break;
            case 3:
                stats.setOutlinesCorrect(stats.getOutlinesCorrect() + correctAnswers);
                break;
            case 4:
                stats.setLanguagesCorrect(stats.getLanguagesCorrect() + correctAnswers);
                break;
        }

        saveUserStats(stats);
    }
    private void updateBestContinent(String userId) {
        UserStats stats = getUserStats(userId);
        if (stats == null) return;

        int[] values = {
                stats.getEuropeCorrect(),
                stats.getAsiaCorrect(),
                stats.getAfricaCorrect(),
                stats.getAmericaCorrect(),
                stats.getOceaniaCorrect()
        };
        String[] continents = {"europe", "asia", "africa", "america", "oceania"};

        int maxIndex = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] > values[maxIndex]) {
                maxIndex = i;
            }
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_BEST_CONTINENT, continents[maxIndex]);
        db.update(TABLE_USER_STATS, contentValues, COLUMN_USER_ID + "=?", new String[]{userId});
    }

    public void deleteUserStats(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER_STATS, COLUMN_USER_ID + "=?", new String[]{userId});
        Log.d(TAG, "Удалена статистика пользователя " + userId);
    }
    @SuppressLint("Range")
    private UserStats cursorToUserStats(Cursor cursor) {
        UserStats stats = new UserStats(
                cursor.getString(cursor.getColumnIndex(COLUMN_USER_ID))
        );

        stats.setGamesPlayed(cursor.getInt(cursor.getColumnIndex(COLUMN_GAMES_PLAYED)));
        stats.setGamesWon(cursor.getInt(cursor.getColumnIndex(COLUMN_GAMES_WON)));
        stats.setWinRate(cursor.getFloat(cursor.getColumnIndex(COLUMN_WIN_RATE)));
        stats.setLevel(cursor.getInt(cursor.getColumnIndex(COLUMN_LEVEL)));
        stats.setExperience(cursor.getInt(cursor.getColumnIndex(COLUMN_EXPERIENCE)));
        stats.setScore(cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE)));
        stats.setDailyStreak(cursor.getInt(cursor.getColumnIndex(COLUMN_DAILY_STREAK)));
        stats.setWinStreak(cursor.getInt(cursor.getColumnIndex(COLUMN_WIN_STREAK)));

        stats.setEuropeCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_EUROPE_CORRECT)));
        stats.setAsiaCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_ASIA_CORRECT)));
        stats.setAfricaCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_AFRICA_CORRECT)));
        stats.setAmericaCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_AMERICA_CORRECT)));
        stats.setOceaniaCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_OCEANIA_CORRECT)));
        stats.setBestContinent(cursor.getString(cursor.getColumnIndex(COLUMN_BEST_CONTINENT)));

        stats.setCapitalsCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_CAPITALS_CORRECT)));
        stats.setFlagsCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_FLAGS_CORRECT)));
        stats.setOutlinesCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_OUTLINES_CORRECT)));
        stats.setLanguagesCorrect(cursor.getInt(cursor.getColumnIndex(COLUMN_LANGUAGES_CORRECT)));

        return stats;
    }

    public void saveAllEmptyAchievements(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        try {
            List<String> allCodes = getAllAchievementCodes();

            for (String code : allCodes) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_UID, userId);
                values.put(COLUMN_CODE, code);
                values.put(COLUMN_PROGRESS, 1);
                values.put(COLUMN_RARITY, 1);
                values.put(COLUMN_IS_UNLOCKED, 0);
                values.put(COLUMN_UNLOCKED_AT, "");

                db.insertWithOnConflict(TABLE_USER_ACHIEVEMENTS, null, values,
                        SQLiteDatabase.CONFLICT_IGNORE);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Сохранено " + allCodes.size() + " пустых достижений для гостя " + userId);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка сохранения пустых достижений", e);
        } finally {
            db.endTransaction();
        }
    }
    public void saveUserAchievements(ProfileResponse.AchievementDto achievement, String userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_UID, userId);
        values.put(COLUMN_CODE, achievement.getCode());
        values.put(COLUMN_PROGRESS, achievement.getProgress());
        values.put(COLUMN_RARITY, achievement.getRarity());
        values.put(COLUMN_IS_UNLOCKED, achievement.isUnlocked()? 1 : 0);

        String unlockedAt = "";
        if (achievement.getUnlockedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            unlockedAt = sdf.format(achievement.getUnlockedAt());
        }
        values.put(COLUMN_UNLOCKED_AT, unlockedAt);

        long result = db.insertWithOnConflict(TABLE_USER_ACHIEVEMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Log.d(TAG, "Сохранено достижение для пользователя " + userId + ": " + result);
    }
    public List<ProfileResponse.AchievementDto> getUserAchievements(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<ProfileResponse.AchievementDto> achievements = new ArrayList<>();

        Cursor cursor = db.query(TABLE_USER_ACHIEVEMENTS, null,
                COLUMN_UID + "=?", new String[]{userId},
                null, null, null);

        while (cursor.moveToNext()) {
            achievements.add(cursorToAchievements(cursor));
        }
        cursor.close();

        return achievements;
    }
    public void updateUserAchievement(ProfileResponse.AchievementDto achievement) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_PROGRESS, achievement.getProgress());
        values.put(COLUMN_RARITY, achievement.getRarity());
        values.put(COLUMN_IS_UNLOCKED, achievement.isUnlocked() ? 1 : 0);

        String unlockedAt = "";
        if (achievement.getUnlockedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            unlockedAt = sdf.format(achievement.getUnlockedAt());
        }
        values.put(COLUMN_UNLOCKED_AT, unlockedAt);

        int rowsUpdated = db.update(TABLE_USER_ACHIEVEMENTS,
                values,
                COLUMN_UID + "=? AND " + COLUMN_CODE + "=?",
                new String[]{achievement.getUserId(), achievement.getCode()});
    }
    public void deleteUserAchievements(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_USER_ACHIEVEMENTS, COLUMN_UID + "=?", new String[]{userId});
        Log.d(TAG, "Удалены достижения пользователя " + userId);
    }

    @SuppressLint("Range")
    private ProfileResponse.AchievementDto cursorToAchievements(Cursor cursor) {
        ProfileResponse.AchievementDto achievement = new ProfileResponse.AchievementDto();

        achievement.setCode(cursor.getString(cursor.getColumnIndex(COLUMN_CODE)));
        achievement.setProgress(cursor.getInt(cursor.getColumnIndex(COLUMN_PROGRESS)));
        achievement.setRarity(cursor.getInt(cursor.getColumnIndex(COLUMN_RARITY)));
        achievement.setUnlocked(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_UNLOCKED)) == 1);

        String dateString = cursor.getString(cursor.getColumnIndex(COLUMN_UNLOCKED_AT));
        achievement.setUnlockedAt(parseDate(dateString));

        return achievement;
    }
    private List<String> getAllAchievementCodes() {
        List<String> codes = new ArrayList<>();

        try {
            String jsonString = readJsonFromAssets("achievements.json");
            JSONObject root = new JSONObject(jsonString);
            JSONArray achievements = root.getJSONArray("achievements");

            for (int i = 0; i < achievements.length(); i++) {
                JSONObject achievement = achievements.getJSONObject(i);
                String code = achievement.getString("code");
                codes.add(code);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка чтения кодов ачивок из JSON", e);
        }

        return codes;
    }
    private String readJsonFromAssets(String fileName) throws IOException {
        InputStream is = context.getAssets().open(fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
    }
    private Date parseDate(String dateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }
}
