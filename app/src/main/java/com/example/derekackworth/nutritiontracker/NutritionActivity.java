/*
	Author: Derek Ackworth
	Date: December 14th, 2019
	File: NutritionActivity.java
	Purpose: NutritionActivity class implementation
*/

package com.example.derekackworth.nutritiontracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class NutritionActivity extends AppCompatActivity
{
    private TextView tvTime;
    private TextView tvNutrients;
    private HashMap<Date, NutritionFacts> nutrientsHashMap = new HashMap<>();
    private static final String TAG = "NutritionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            getSupportActionBar().hide();
        }

        tvTime = findViewById(R.id.tvTime);
        tvNutrients = findViewById(R.id.tvNutrients);
        nutrientsHashMap = (HashMap<Date, NutritionFacts>)getIntent().getSerializableExtra("nutrientsHashMap");
        loadNutrients();
    }

    public void onLeftClick(View view)
    {
        if (tvTime.getText().toString() == getString(R.string.today))
        {
            tvTime.setText(getString(R.string.last_30_days));
        }
        else if (tvTime.getText().toString() == getString(R.string.last_7_days))
        {
            tvTime.setText(getString(R.string.today));
        }
        else if (tvTime.getText().toString() == getString(R.string.last_30_days))
        {
            tvTime.setText(getString(R.string.last_7_days));
        }

        loadNutrients();
    }

    public void onRightClick(View view)
    {
        if (tvTime.getText().toString() == getString(R.string.today))
        {
            tvTime.setText(getString(R.string.last_7_days));
        }
        else if (tvTime.getText().toString() == getString(R.string.last_7_days))
        {
            tvTime.setText(getString(R.string.last_30_days));
        }
        else if (tvTime.getText().toString() == getString(R.string.last_30_days))
        {
            tvTime.setText(getString(R.string.today));
        }

        loadNutrients();
    }

    public void onBackClick(View view)
    {
        finish();
    }

    private void loadNutrients()
    {
        String s = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate = null;

        try
        {
            currentDate = formatter.parse(formatter.format(new Date()));
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Error parsing date.", e);
        }

        if (currentDate != null)
        {
            NutritionFacts facts = new NutritionFacts();
            DecimalFormat df = new DecimalFormat("#.##");
            int days = 1;

            if (tvTime.getText().toString() == getString(R.string.last_7_days))
            {
                days = 7;
            }
            else if (tvTime.getText().toString() == getString(R.string.last_30_days))
            {
                days = 30;
            }

            for (int i = 0; i < days; i++)
            {
                if (nutrientsHashMap.containsKey(currentDate))
                {
                    facts.calories += nutrientsHashMap.get(currentDate).calories;
                    facts.fat += nutrientsHashMap.get(currentDate).fat;
                    facts.saturatedAndTrans += nutrientsHashMap.get(currentDate).saturatedAndTrans;
                    facts.cholesterol += nutrientsHashMap.get(currentDate).cholesterol;
                    facts.sodium += nutrientsHashMap.get(currentDate).sodium;
                    facts.carbs += nutrientsHashMap.get(currentDate).carbs;
                    facts.fibre += nutrientsHashMap.get(currentDate).fibre;
                    facts.sugars += nutrientsHashMap.get(currentDate).sugars;
                    facts.protein += nutrientsHashMap.get(currentDate).protein;
                }

                currentDate = addSubtractDays(currentDate, -1);
            }

            s += "Calories: ";
            s += facts.calories + "\n";
            s += "Fat: ";
            s += df.format(facts.fat / days) + "%\n";
            s += "Saturated and Trans Fat: ";
            s += df.format(facts.saturatedAndTrans / days) + "%\n";
            s += "Cholesterol: ";
            s += df.format(facts.cholesterol / days) + "%\n";
            s += "Sodium: ";
            s += df.format(facts.sodium / days) + "%\n";
            s += "Carbohydrate: ";
            s += df.format(facts.carbs / days) + "%\n";
            s += "Fibre: ";
            s += df.format(facts.fibre / days) + "%\n";
            s += "Sugars: ";
            s += df.format(facts.sugars) + "g\n";
            s += "Protein: ";
            s += df.format(facts.protein) + "g";
        }

        tvNutrients.setText(s);
    }

    private static Date addSubtractDays(Date date, int days)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }
}
