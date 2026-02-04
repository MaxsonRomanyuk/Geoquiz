package com.example.geoquiz_frontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btnSend);
        TextView txt = findViewById(R.id.txtResult);

        btn.setOnClickListener(v -> {
            ApiService api = ApiClient.getApi();

            TestRequest request = new TestRequest("Hello from Android");

            api.echo(request).enqueue(new Callback<TestResponse>() {
                @Override
                public void onResponse(Call<TestResponse> call, Response<TestResponse> response) {
                    if (response.isSuccessful()) {
                        txt.setText(response.body().message +
                                "\nReceived: " + response.body().received);
                    }
                }

                @Override
                public void onFailure(Call<TestResponse> call, Throwable t) {
                    txt.setText("Error: " + t.getMessage());
                }
            });
        });
    }
}