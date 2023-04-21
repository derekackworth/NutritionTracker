package com.example.derekackworth.nutritiontracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity
{
    private CalendarView cvCalendar;
    private EditText input;
    private String currentPhotoPath;
    private ArrayList<String> nutrientGs;
    private HashMap<Date, NutritionFacts> nutrientsHashMap = new HashMap<>();

    private static final String[] nutrients =
    {
            "Calories", "Fat", "Saturated", "Trans", "Cholesterol",
            "Sodium", "Carbohydrate", "Fibre", "Sugars", "Protein"
    };

    private static final String[] appPermissions =
    {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 1;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            Objects.requireNonNull(getSupportActionBar()).hide();
        }

        if (!arePermissionsEnabled())
        {
            requestMultiplePermissions();
        }

        cvCalendar = findViewById(R.id.cvCalendar);
        cvCalendar.setOnDateChangeListener((view, year, month, day) ->
        {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cvCalendar.setDate(cal.getTime().getTime());
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        try
        {
            FileOutputStream fos = openFileOutput("data", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(nutrientsHashMap);
            oos.close();
            fos.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error creating data file.", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onResume()
    {
        super.onResume();

        try
        {
            FileInputStream fis = openFileInput("data");
            ObjectInputStream ois = new ObjectInputStream(fis);

            nutrientsHashMap = (HashMap<Date, NutritionFacts>) ois.readObject();
            ois.close();
            fis.close();
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error creating data file.", e);
        }
        catch (ClassNotFoundException e)
        {
            Log.e(TAG, "Error class not found.", e);
        }
    }

    public void onAddProductCameraClick(View view)
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try
        {
            File photoFile;
            photoFile = createImageFile();
            Uri photoURI = FileProvider.getUriForFile(this, "com.example.derekackworth.nutritiontracker.android.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            requestCameraResultLauncher.launch(intent);
        }
        catch (ActivityNotFoundException e)
        {
            Log.e(TAG, "Activity not fund.", e);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Error creating image file.", e);
        }
    }

    public void onAddProductImageClick(View view)
    {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        requestImageResultLauncher.launch(intent);
    }

    public void onClearAllClick(View view)
    {
        nutrientsHashMap = new HashMap<>();
    }

    public void onClearSelectedClick(View view)
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date currentDate = null;

        try
        {
            currentDate = formatter.parse(formatter.format(new Date(cvCalendar.getDate())));
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Error parsing date.", e);
        }

        if (currentDate != null)
        {
            if (nutrientsHashMap.containsKey(currentDate))
            {
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).calories = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).fat = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).saturatedAndTrans = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).cholesterol = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).sodium = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).carbs = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).fibre = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).sugars = 0;
                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).protein = 0;
            }
        }
    }

    public void onNutritionClick(View view)
    {
        Intent intent = new Intent(this, NutritionActivity.class);
        intent.putExtra("nutrientsHashMap", nutrientsHashMap);
        startActivity(intent);
    }

    private boolean arePermissionsEnabled()
    {
        for (String permission : appPermissions)
        {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }

        return true;
    }

    private void requestMultiplePermissions(){
        List<String> remainingPermissions = new ArrayList <>();

        for (String permission : appPermissions)
        {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
            {
                remainingPermissions.add(permission);
            }
        }

        requestPermissions(remainingPermissions.toArray(new String[0]), REQUEST_PERMISSIONS);
    }

    private File createImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile
        (
            imageFileName, /* prefix */
            ".jpg",  /* suffix */
            storageDir     /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void getProductNutritionFacts(Bitmap bm)
    {
        nutrientGs = new ArrayList<>();
        InputImage image = InputImage.fromBitmap(bm, 0);
        TextRecognizer detector = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        detector.process(image).addOnSuccessListener(t ->
        {
            for (Text.TextBlock tb : t.getTextBlocks())
            {
                for (Text.Line line : tb.getLines())
                {
                    if (stringContainsItemFromList(line.getText(), nutrients) != -1 && line.getText().matches(".*\\d.*"))
                    {
                        nutrientGs.add(line.getText());
                    }
                }
            }


            if (nutrientGs.size() != 0)
            {
                input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("How many servings of this product?")
                    .setView(input)
                    .setPositiveButton("Ok", (dialog, which) ->
                    {
                        dialog.dismiss();
                        filterNutrientsToHashMap();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                AlertDialog alert = builder.create();
                alert.show();
                Button pButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);

                LinearLayout.LayoutParams positiveParams = new LinearLayout.LayoutParams
                (
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );

                positiveParams.setMargins(10,0,0,0);
                pButton.setLayoutParams(positiveParams);
                Button nButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);

                LinearLayout.LayoutParams negativeParams = new LinearLayout.LayoutParams
                (
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );

                negativeParams.setMargins(0,0,10,0);
                nButton.setLayoutParams(negativeParams);
            }
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                    .setMessage("No nutrition facts found")
                    .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());

                AlertDialog alert = builder.create();
                alert.show();
            }

        }).addOnFailureListener(e ->
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage("No nutrition facts found")
                .setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());

            AlertDialog alert = builder.create();
            alert.show();
        });
    }

    private void filterNutrientsToHashMap()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date currentDate = null;

        try
        {
            currentDate = formatter.parse(formatter.format(new Date(cvCalendar.getDate())));
        }
        catch (ParseException e)
        {
            Log.e(TAG, "Error parsing date.", e);
        }

        if (currentDate != null)
        {
            if (!nutrientsHashMap.containsKey(currentDate))
            {
                nutrientsHashMap.put(currentDate, new NutritionFacts());
            }

            for (String nutrientG : nutrientGs)
            {
                if (nutrientG.contains("Calories"))
                {
                    try
                    {
                        String num = nutrientG.replaceAll("O", "0");

                        if (indexOfRegex("\\d", num) != -1)
                        {
                            num = num.substring(indexOfRegex("\\d", num));
                            Objects.requireNonNull(nutrientsHashMap.get(currentDate)).calories +=
                                Integer.parseInt(input.getText().toString()) *
                                Integer.parseInt(num);
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        Log.e(TAG, "Error parsing.", e);
                    }
                }
                else
                {
                    String num = nutrientG.replaceAll("O", "0");

                    if (indexOfRegex("\\d", num) != -1)
                    {
                        num = num.substring(indexOfRegex("\\d", num));

                        if (num.contains(" "))
                        {
                            num = num.substring(0, num.indexOf(" "));
                        }

                        num = num.replaceAll("[^\\d.]", "");
                        int dailyValue;

                        if (nutrientG.contains("Fat"))
                        {
                            dailyValue = 65;

                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).fat +=
                                    ((Integer.parseInt(input.getText().toString()) * Double.parseDouble(num)) / dailyValue) * 100;
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Saturated") || nutrientG.contains("Trans"))
                        {
                            dailyValue = 20;

                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).saturatedAndTrans +=
                                    ((Integer.parseInt(input.getText().toString()) * Double.parseDouble(num)) / dailyValue) * 100;
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Cholesterol"))
                        {
                            dailyValue = 300;

                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).cholesterol +=
                                    ((Integer.parseInt(input.getText().toString()) * Double.parseDouble(num)) / dailyValue) * 100;
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Sodium"))
                        {
                            dailyValue = 2400;

                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).sodium +=
                                    ((Integer.parseInt(input.getText().toString()) * Double.parseDouble(num)) / dailyValue) * 100;
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Carbohydrate"))
                        {
                            dailyValue = 300;

                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).carbs +=
                                    ((Integer.parseInt(input.getText().toString()) * Double.parseDouble(num)) / dailyValue) * 100;
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Fibre"))
                        {
                            dailyValue = 25;

                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).fibre +=
                                    ((Integer.parseInt(input.getText().toString()) * Double.parseDouble(num)) / dailyValue) * 100;
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Sugars"))
                        {
                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).sugars +=
                                    Integer.parseInt(input.getText().toString()) * Double.parseDouble(num);
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                        else if (nutrientG.contains("Protein"))
                        {
                            try
                            {
                                Objects.requireNonNull(nutrientsHashMap.get(currentDate)).protein +=
                                    Integer.parseInt(input.getText().toString()) * Double.parseDouble(num);
                            }
                            catch (NumberFormatException e)
                            {
                                Log.e(TAG, "Error parsing.", e);
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static int indexOfRegex(String regex, String s)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        return matcher.find() ? matcher.start() : -1;
    }

    @SuppressWarnings("SameParameterValue")
    private static int stringContainsItemFromList(String inputStr, String[] items)
    {
        for(int i = 0; i < items.length; i++)
        {
            if(inputStr.contains(items[i]))
            {
                return i;
            }
        }

        return -1;
    }

    private static Bitmap rotateImage(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS)
        {
            for (int i = 0; i < grantResults.length; i++)
            {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                {
                    if (shouldShowRequestPermissionRationale(permissions[i]))
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setMessage("This app needs permissions to work without errors.")
                            .setPositiveButton("Yes, grant permissions", (dialog, which) ->
                            {
                                dialog.dismiss();
                                requestMultiplePermissions();
                            })
                            .setNegativeButton("No, exit app", (dialog, which) ->
                            {
                                dialog.dismiss();
                                finish();
                            })
                            .setCancelable(false);

                        AlertDialog alert = builder.create();
                        alert.show();
                        Button pButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);

                        LinearLayout.LayoutParams positiveParams = new LinearLayout.LayoutParams
                        (
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                        positiveParams.setMargins(10,0,0,0);
                        pButton.setLayoutParams(positiveParams);
                        Button nButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);

                        LinearLayout.LayoutParams negativeParams = new LinearLayout.LayoutParams
                        (
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                        negativeParams.setMargins(0,0,10,0);
                        nButton.setLayoutParams(negativeParams);
                    }
                    else
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setMessage("You have denied some permissions." +
                                    "Allow all permissions at [App Settings] > [Permissions].")
                            .setPositiveButton("Go to settings", (dialog, which) ->
                            {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .setNegativeButton("No, exit app", (dialog, which) ->
                            {
                                dialog.dismiss();
                                finish();
                            })
                            .setCancelable(false);

                        AlertDialog alert = builder.create();
                        alert.show();
                        Button pButton = alert.getButton(DialogInterface.BUTTON_POSITIVE);

                        LinearLayout.LayoutParams positiveParams = new LinearLayout.LayoutParams
                        (
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                        positiveParams.setMargins(10,0,0,0);
                        pButton.setLayoutParams(positiveParams);
                        Button nButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);

                        LinearLayout.LayoutParams negativeParams = new LinearLayout.LayoutParams
                        (
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        );

                        negativeParams.setMargins(0,0,10,0);
                        nButton.setLayoutParams(negativeParams);

                    }

                    return;
                }
            }
        }
    }

    ActivityResultLauncher<Intent> requestCameraResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result ->
        {
            if (result.getResultCode() == Activity.RESULT_OK)
            {
                File file = new File(currentPhotoPath);

                try
                {
                    ExifInterface ei = new ExifInterface(currentPhotoPath);
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                    Bitmap bmOriginal = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));
                    Bitmap bmRotated;

                    switch(orientation)
                    {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            bmRotated = rotateImage(bmOriginal, 90);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            bmRotated = rotateImage(bmOriginal, 180);
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            bmRotated = rotateImage(bmOriginal, 270);
                            break;
                        case ExifInterface.ORIENTATION_NORMAL:
                        default:
                            bmRotated = bmOriginal;
                    }

                    if (bmRotated != null)
                    {
                        getProductNutritionFacts(bmRotated);
                    }
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Error creating bitmap.", e);
                }
            }
        }
    );

    ActivityResultLauncher<Intent> requestImageResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result ->
        {
            if (result.getResultCode() == Activity.RESULT_OK)
            {
                Intent data = result.getData();
                Uri image = Objects.requireNonNull(data).getData();

                try
                {
                    Bitmap bm = MediaStore.Images.Media.getBitmap(getContentResolver(), image);

                    if (bm != null)
                    {
                        getProductNutritionFacts(bm);
                    }
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Error creating bitmap.", e);
                }
            }
        }
    );
}
