package com.atharvakale.facerecognition;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.util.Pair;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    FaceDetector detector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;
    ImageView face_preview;
    Interpreter tfLite;
    TextView reco_name, preview_info, textAbove_preview;
    Button recognize, camera_switch, actions;
    EditText criminalRecordEt;

    ImageButton add_face;
    CameraSelector cameraSelector;
    boolean developerMode = false;
    float distance = 1.0f;
    boolean start = true, flipX = false;
    Context context = MainActivity.this;
    int cam_face = CameraSelector.LENS_FACING_BACK;

    int[] intValues;
    int inputSize = 112;
    boolean isModelQuantized = false;
    float[][] embeedings;
    float IMAGE_MEAN = 128.0f;
    float IMAGE_STD = 128.0f;
    int OUTPUT_SIZE = 192;
    private static int SELECT_PICTURE = 1;
    ProcessCameraProvider cameraProvider;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    String modelFile = "mobile_face_net.tflite";

    private HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registered = readFromSP();
        setContentView(R.layout.activity_main);

        face_preview = findViewById(R.id.imageView);
        reco_name = findViewById(R.id.textView);
        preview_info = findViewById(R.id.textView2);
        textAbove_preview = findViewById(R.id.textAbovePreview);
        add_face = findViewById(R.id.imageButton);
        add_face.setVisibility(View.INVISIBLE);
        criminalRecordEt = findViewById(R.id.editTextCriminalStatus);
        SharedPreferences sharedPref = getSharedPreferences("Distance", Context.MODE_PRIVATE);
        distance = sharedPref.getFloat("distance", 1.00f);

        face_preview.setVisibility(View.INVISIBLE);
        recognize = findViewById(R.id.button3);
        camera_switch = findViewById(R.id.button5);
        actions = findViewById(R.id.button2);
        textAbove_preview.setText("Tanınan Personel:");

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }

        actions.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("İşlem Seçin:");
            String[] options = {"Listeyi Görüntüle", "Listeyi Güncelle", "Kaydet", "Yükle", "Hepsini Sil", "Fotoğraftan Yükle", "Hassasiyet", "Geliştirici Modu"};
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: displaynameListview(); break;
                    case 1: updatenameListview(); break;
                    case 2: insertToSP(registered, 0); break;
                    case 3: registered.putAll(readFromSP()); break;
                    case 4: clearnameList(); break;
                    case 5: loadphoto(); break;
                    case 6: hyperparameters(); break;
                    case 7: developerMode(); break;
                }
            });
            builder.setPositiveButton("Kapat", null);
            builder.show();
        });

        camera_switch.setOnClickListener(v -> {
            cam_face = (cam_face == CameraSelector.LENS_FACING_BACK) ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            flipX = !flipX;
            cameraProvider.unbindAll();
            cameraBind();
        });

        add_face.setOnClickListener(v -> addFace());

        recognize.setOnClickListener(v -> {
            if (recognize.getText().toString().equals("Recognize")) {
                start = true;
                textAbove_preview.setText("Tanınan Personel:");
                recognize.setText("Add Face");
                add_face.setVisibility(View.INVISIBLE);
                reco_name.setVisibility(View.VISIBLE);
                face_preview.setVisibility(View.INVISIBLE);
                preview_info.setText("");
            } else {
                textAbove_preview.setText("Yüz Önizleme: ");
                recognize.setText("Recognize");
                add_face.setVisibility(View.VISIBLE);
                reco_name.setVisibility(View.INVISIBLE);
                face_preview.setVisibility(View.VISIBLE);
                preview_info.setText("1. Yüzü kameraya yaklaştırın.\n2. Kutuda yüzü görün.\n3. Ekle tuşuna basın.");
            }
        });

        try {
            tfLite = new Interpreter(loadModelFile(MainActivity.this, modelFile));
        } catch (IOException e) { e.printStackTrace(); }

        detector = FaceDetection.getClient(new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).build());

        cameraBind();
    }

    private void addFace() {
        start = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Personel Detaylarını Girin");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText nameIn = new EditText(context); nameIn.setHint("İsim"); layout.addView(nameIn);
        final EditText surIn = new EditText(context); surIn.setHint("Soyisim"); layout.addView(surIn);
        final EditText ageIn = new EditText(context); ageIn.setHint("Yaş"); ageIn.setInputType(InputType.TYPE_CLASS_NUMBER); layout.addView(ageIn);

        final EditText criminalIn = new EditText(context); criminalIn.setHint("Sabıka Kaydı");
        layout.addView(criminalIn);
        builder.setView(layout);

        builder.setPositiveButton("KAYDET", (dialog, which) -> {
            String combined = nameIn.getText().toString().trim() + "|" +
                    surIn.getText().toString().trim() + "|" +
                    ageIn.getText().toString().trim() + "|" +
                    criminalIn.getText().toString().trim();
            SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition("0", "", -1f);
            result.setExtra(embeedings);
            registered.put(combined, result);
            start = true;
        });
        builder.setNegativeButton("İptal", (dialog, which) -> { start = true; dialog.cancel(); });
        builder.show();
    }

    public void recognizeImage(final Bitmap bitmap) {
        face_preview.setImageBitmap(bitmap);
        ByteBuffer imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());
        intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
            }
        }
        Object[] inputArray = {imgData};
        Map<Integer, Object> outputMap = new HashMap<>();
        embeedings = new float[1][OUTPUT_SIZE];
        outputMap.put(0, embeedings);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        if (registered.size() > 0) {
            final List<Pair<String, Float>> nearest = findNearest(embeedings[0]);
            if (nearest.get(0) != null) {
                String raw = nearest.get(0).first;
                float dist = nearest.get(0).second;
                if (dist < distance) {
                    String[] p = raw.split("\\|");
                    if (p.length == 4) {
                        // p[0]: Ad, p[1]: Soyad, p[2]: Yaş, p[3]: Sabıka Kaydı
                        reco_name.setText("AD: " + p[0] + "\nSOYAD: " + p[1] + "\nYAŞ: " + p[2] + "\nSABIKA: " + p[3]);
                    } else {
                        reco_name.setText(raw);
                    }
                    reco_name.setTextColor(Color.GREEN);
                } else {
                    reco_name.setText("Bilinmiyor");
                    reco_name.setTextColor(Color.RED);
                }
            }
        }
    }

    private List<Pair<String, Float>> findNearest(float[] emb) {
        List<Pair<String, Float>> neighbour_list = new ArrayList<>();
        Pair<String, Float> ret = null;
        Pair<String, Float> prev_ret = null;
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];
            float dist = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                dist += diff * diff;
            }
            dist = (float) Math.sqrt(dist);
            if (ret == null || dist < ret.second) { prev_ret = ret; ret = new Pair<>(name, dist); }
        }
        if (prev_ret == null) prev_ret = ret;
        neighbour_list.add(ret); neighbour_list.add(prev_ret);
        return neighbour_list;
    }

    // --- TÜM YARDIMCI METODLAR (TAM LİSTE) ---

    private void cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) { e.printStackTrace(); }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(cam_face).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), imageProxy -> {
            @SuppressLint("UnsafeExperimentalUsageError")
            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                detector.process(image).addOnSuccessListener(faces -> {
                    if (faces.size() != 0) {
                        Bitmap frame_bmp = toBitmap(mediaImage);
                        Bitmap frame_bmp1 = rotateBitmap(frame_bmp, imageProxy.getImageInfo().getRotationDegrees(), false, false);
                        RectF boundingBox = new RectF(faces.get(0).getBoundingBox());
                        Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);
                        if (flipX) cropped_face = rotateBitmap(cropped_face, 0, flipX, false);
                        Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);
                        if (start) recognizeImage(scaled);
                    } else {
                        reco_name.setText(registered.isEmpty() ? "Add Face" : "No Face Detected!");
                    }
                }).addOnCompleteListener(task -> imageProxy.close());
            }
        });
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth(); int height = bm.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(((float) newWidth) / width, ((float) newHeight) / height);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle(); return resizedBitmap;
    }

    private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(), (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);
        canvas.drawBitmap(source, matrix, paint);
        if (source != null && !source.isRecycled()) source.recycle();
        return resultBitmap;
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        if (rotatedBitmap != bitmap) bitmap.recycle();
        return rotatedBitmap;
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        int width = image.getWidth(); int height = image.getHeight();
        int ySize = width * height; int uvSize = width * height / 4;
        byte[] nv21 = new byte[ySize + uvSize * 2];
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        yBuffer.get(nv21, 0, ySize);
        int pos = ySize;
        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * image.getPlanes()[2].getPixelStride() + row * image.getPlanes()[2].getRowStride();
                nv21[pos++] = vBuffer.get(vuPos); nv21[pos++] = uBuffer.get(vuPos);
            }
        }
        return nv21;
    }

    private Bitmap toBitmap(Image image) {
        byte[] nv21 = YUV_420_888toNV21(image);
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
    }

    private void insertToSP(HashMap<String, SimilarityClassifier.Recognition> jsonMap, int mode) {
        if (mode == 1) jsonMap.clear();
        String jsonString = new Gson().toJson(jsonMap);
        getSharedPreferences("HashMap", MODE_PRIVATE).edit().putString("map", jsonString).apply();
        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
    }

    private HashMap<String, SimilarityClassifier.Recognition> readFromSP() {
        SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
        String json = sharedPreferences.getString("map", "{}");
        TypeToken<HashMap<String, SimilarityClassifier.Recognition>> token = new TypeToken<HashMap<String, SimilarityClassifier.Recognition>>() {};
        HashMap<String, SimilarityClassifier.Recognition> retrievedMap = new Gson().fromJson(json, token.getType());
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : retrievedMap.entrySet()) {
            float[][] output = new float[1][OUTPUT_SIZE];
            ArrayList arrayList = (ArrayList) ((ArrayList) entry.getValue().getExtra()).get(0);
            for (int c = 0; c < arrayList.size(); c++) output[0][c] = ((Double) arrayList.get(c)).floatValue();
            entry.getValue().setExtra(output);
        }
        return retrievedMap;
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private void displaynameListview() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (registered.isEmpty()) builder.setTitle("Kayıt Yok");
        else { builder.setTitle("Kayıtlar:"); builder.setItems(registered.keySet().toArray(new String[0]), null); }
        builder.setPositiveButton("Tamam", null).show();
    }

    private void clearnameList() { registered.clear(); insertToSP(registered, 1); }

    private void updatenameListview() {
        if (registered.isEmpty()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String[] names = registered.keySet().toArray(new String[0]);
        builder.setItems(names, (dialog, which) -> { registered.remove(names[which]); insertToSP(registered, 2); });
        builder.setNegativeButton("İptal", null).show();
    }

    private void hyperparameters() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(distance));
        builder.setView(input).setPositiveButton("Güncelle", (dialog, which) -> {
            distance = Float.parseFloat(input.getText().toString());
            getSharedPreferences("Distance", MODE_PRIVATE).edit().putFloat("distance", distance).apply();
        }).show();
    }

    private void developerMode() { developerMode = !developerMode; Toast.makeText(context, "Dev Mode: " + developerMode, Toast.LENGTH_SHORT).show(); }

    private void loadphoto() {
        start = false;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Resim Seç"), SELECT_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SELECT_PICTURE) {
            Uri uri = data.getData();
            try {
                Bitmap bmp = getBitmapFromUri(uri);
                detector.process(InputImage.fromBitmap(bmp, 0)).addOnSuccessListener(faces -> {
                    if (faces.size() > 0) {
                        Bitmap cropped = getCropBitmapByCPU(bmp, new RectF(faces.get(0).getBoundingBox()));
                        recognizeImage(getResizedBitmap(cropped, 112, 112));
                        addFace();
                    }
                });
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
        pfd.close(); return bitmap;
    }
}
