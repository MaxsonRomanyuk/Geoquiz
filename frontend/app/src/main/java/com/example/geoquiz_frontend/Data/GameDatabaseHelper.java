package com.example.geoquiz_frontend.Data;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.geoquiz_frontend.DTOs.BootstrapResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
public class GameDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "GameDatabaseHelper";
    private static final String DATABASE_NAME = "geoquiz.db";
    private static final int DATABASE_VERSION = 2;


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

    private final Gson gson = new Gson();

    public GameDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

        db.execSQL("CREATE INDEX idx_questions_type ON " + TABLE_QUESTIONS + "(" + COLUMN_QUESTION_TYPE + ")");
        db.execSQL("CREATE INDEX idx_questions_country ON " + TABLE_QUESTIONS + "(" + COLUMN_QUESTION_COUNTRY_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Обновление БД с версии " + oldVersion + " до " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COUNTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTIONS);
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

//    public int getCountriesCount() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COUNTRIES, null);
//        cursor.moveToFirst();
//        int count = cursor.getInt(0);
//        cursor.close();
//        return count;
//    }
//
//    public int getQuestionsCount() {
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTIONS, null);
//        cursor.moveToFirst();
//        int count = cursor.getInt(0);
//        cursor.close();
//        return count;
//    }
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
    private void checkDatabaseContents(SQLiteDatabase db) {
        Cursor countryCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_COUNTRIES, null);
        countryCursor.moveToFirst();
        int countryCount = countryCursor.getInt(0);
        countryCursor.close();

        Cursor questionCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_QUESTIONS, null);
        questionCursor.moveToFirst();
        int questionCount = questionCursor.getInt(0);
        questionCursor.close();

        Log.d(TAG, "ИТОГО в БД: " + countryCount + " стран, " + questionCount + " вопросов");

        if (questionCount > 0) {
            Cursor typeCursor = db.rawQuery(
                    "SELECT " + COLUMN_QUESTION_TYPE + ", COUNT(*) FROM " + TABLE_QUESTIONS + " GROUP BY " + COLUMN_QUESTION_TYPE,null);
            while (typeCursor.moveToNext()) {
                Log.d(TAG, "  Тип " + typeCursor.getInt(0) + ": " + typeCursor.getInt(1) + " вопросов");
            }
            typeCursor.close();
        }
    }
}
