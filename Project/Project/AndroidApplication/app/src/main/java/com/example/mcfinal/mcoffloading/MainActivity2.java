package com.example.mcfinal.mcoffloading;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity2 extends AppCompatActivity {
    public String estimate1, estimate2, estimate3;
    TextView est1;
    TextView est2;
    TextView est3;
    TextView est4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        Intent intent = getIntent();

        ((TextView) findViewById(R.id.row1)).setText(intent.getStringExtra("row1"));
        ((TextView) findViewById(R.id.row2)).setText(intent.getStringExtra("row2"));
        ((TextView) findViewById(R.id.row3)).setText(intent.getStringExtra("row3"));
        ((TextView) findViewById(R.id.row4)).setText(intent.getStringExtra("row4"));
        estimate1 = intent.getStringExtra("estimation1");
        estimate2 = intent.getStringExtra("estimation2");
        estimate3 = intent.getStringExtra("estimation3");
        //Toast.makeText(getApplicationContext(), "Estimate 2 " + estimate2, Toast.LENGTH_SHORT).show();

        est1 = findViewById(R.id.estimation1);
        est2 = findViewById(R.id.estimation2);
        est3 = findViewById(R.id.estimation3);

        est1.setText(estimate1);
        est2.setText(estimate2);
        est3.setText(estimate3);
    }
}
