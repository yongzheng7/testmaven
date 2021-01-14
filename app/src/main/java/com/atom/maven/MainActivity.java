package com.atom.maven;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.atom.module1.Module1;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Module1.test2();
    }
}