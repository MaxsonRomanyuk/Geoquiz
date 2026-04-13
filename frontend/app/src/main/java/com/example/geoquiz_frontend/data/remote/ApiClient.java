package com.example.geoquiz_frontend.data.remote;
import com.example.geoquiz_frontend.presentation.utils.PreferencesHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String EMULATOR_URL = "http://10.0.2.2:5238/";

    private static final String DEVICE_URL = "http://192.168.100.40:5238/";
    private static final boolean IS_EMULATOR = false;

    private static final String BASE_URL = IS_EMULATOR ? EMULATOR_URL : DEVICE_URL;
    private static ApiService instance;

    public static ApiService getApi() {
        if (instance == null) {
            instance = createApi();
        }
        return instance;
    }

    private static ApiService createApi() {
        Gson gson = createGsonWithDateAdapter();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);
    }

    public static ApiService getApiWithAuth(PreferencesHelper preferencesHelper) {
        Gson gson = createGsonWithDateAdapter();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(preferencesHelper))
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);
    }
    private static Gson createGsonWithDateAdapter() {
        return new GsonBuilder()
                .registerTypeAdapter(Date.class, dateSerializer)
                .registerTypeAdapter(Date.class, dateDeserializer)
                .create();
    }
    static JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String formattedDate = sdf.format(src);
            return new JsonPrimitive(formattedDate);
        }
    };
    static JsonDeserializer<Date> dateDeserializer = (json, typeOfT, context) -> {
        String dateStr = json.getAsString();
        try {
            dateStr = dateStr.replaceAll("(\\.\\d{3})\\d*", "$1");

            if (!dateStr.endsWith("Z") && !dateStr.contains("+")) {
                dateStr += "Z";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse date: " + dateStr, e);
        }
    };
}
