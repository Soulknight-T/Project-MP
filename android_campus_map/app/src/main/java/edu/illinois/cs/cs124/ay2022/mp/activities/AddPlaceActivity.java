package edu.illinois.cs.cs124.ay2022.mp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import edu.illinois.cs.cs124.ay2022.mp.R;
import edu.illinois.cs.cs124.ay2022.mp.application.FavoritePlacesApplication;
import edu.illinois.cs.cs124.ay2022.mp.models.Place;

public class AddPlaceActivity extends AppCompatActivity {
  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_addplace);
    Intent backToMap = new Intent(this, MainActivity.class);
    backToMap.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    Button cancelButton = findViewById(R.id.cancel_button);
    cancelButton.setOnClickListener(v ->  {
      startActivity(backToMap);
    });
    Button saveButton = findViewById(R.id.save_button);
    saveButton.setOnClickListener(v -> {
      String name = "Link & Zelda";
      String latitude = getIntent().getStringExtra("latitude");
      String longitude = getIntent().getStringExtra("longitude");
      startActivity(backToMap);
      EditText editText = findViewById(R.id.description);
      String des = editText.getText().toString();
      Place temp = new Place("6ef63dc4-f05e-438b-a999-66d65b78069b",
          name,
          Double.parseDouble(latitude),
          Double.parseDouble(longitude),
          des,
          false);
      FavoritePlacesApplication i = (FavoritePlacesApplication) getApplication();
      i.getClient().postFavoritePlace(temp, booleanResultMightThrow -> {});
      startActivity(backToMap);
    });
    Button highlightButton = findViewById(R.id.highlight_button);
    highlightButton.setOnClickListener(v -> {
      String name = "Link & Zelda";
      String latitude = getIntent().getStringExtra("latitude");
      String longitude = getIntent().getStringExtra("longitude");
      startActivity(backToMap);
      EditText editText = findViewById(R.id.description);
      String des = editText.getText().toString();
      Place temp = new Place("6ef63dc4-f05e-438b-a999-66d65b78069b",
          name,
          Double.parseDouble(latitude),
          Double.parseDouble(longitude),
          des,
          true);
      FavoritePlacesApplication i = (FavoritePlacesApplication) getApplication();
      i.getClient().postFavoritePlace(temp, booleanResultMightThrow -> {});
      startActivity(backToMap);
    });
  }
}
