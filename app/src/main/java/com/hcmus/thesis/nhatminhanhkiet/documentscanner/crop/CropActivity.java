package com.hcmus.thesis.nhatminhanhkiet.documentscanner.crop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.R;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.SourceManager;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor.Corners;

import com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor.ImageProcessor;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.view.ImageFullscreen;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CropActivity extends AppCompatActivity {

    Mat picture;
    Mat croppedPicture;

    Corners corners;
    Bitmap croppedBitmap;
    ImageView paper, picture_cropped;
    PaperRectangle paper_rect;
    Button btnCrop;

    private final static String TAG = "CropActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);
        getSupportActionBar().hide();

        setupView();

        SourceManager sourceManager = new SourceManager();
        picture = sourceManager.Companion.getPic();
        corners = sourceManager.Companion.getCorners();

        init();

    }

    void setupView(){
        paper = findViewById(R.id.paper);
        picture_cropped = findViewById(R.id.picture_cropped);
        paper_rect = findViewById(R.id.paper_rect);

        btnCrop = findViewById(R.id.btnCrop);
        btnCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cropImage();
            }
        });
    }

    public void cropImage(){
        if (picture == null) {
            Log.i(TAG, "picture null?");
            return;
        }

        if (croppedBitmap != null) {
            Log.i(TAG, "already cropped");
            return;
        }

        ImageProcessor imageProcessor = new ImageProcessor(CropActivity.this);
        Mat pc = imageProcessor.cropPicture(picture, paper_rect.getCorners2Crop());
        croppedPicture = pc;
        croppedBitmap = Bitmap.createBitmap(pc.width(), pc.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(pc, croppedBitmap);
        //picture_cropped.setImageBitmap(croppedBitmap);

        paper.setVisibility(View.GONE);
        paper_rect.setVisibility(View.GONE);

        String filePath = saveImageToExternalStorage(croppedBitmap);
        if(filePath!=null){
            Intent intent = new Intent(CropActivity.this, ImageFullscreen.class);
            intent.putExtra("file_path", filePath);
            startActivity(intent);
            finish();
        }
    }


    private void init(){
        Bitmap bitmap = Bitmap.createBitmap(picture.width(), picture.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(picture, bitmap, true);

        if(corners == null || isCornersListDuplicates(corners)){
            processImageWithFirebase(bitmap);
            return;
        }

        paper_rect.onCorners2Crop(corners, picture.size());

        Glide.with(this).load(bitmap).into(paper);
    }

    public boolean isCornersListDuplicates(Corners corners)
    {
        final Set<Point> set = new HashSet<>();

        for(int i=0;i<corners.corners.size();i++){
            if(set.add(corners.corners.get(i)) == false){
                return true;
            }
        }
        return false;
    }

    void processImageWithFirebase(final Bitmap bitmap){
        final List<Point> points = new ArrayList<>();

        FirebaseVisionImage visionImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionObjectDetector objectDetector = createFirebaseVisionObjectDetector();
        objectDetector.processImage(visionImage).addOnSuccessListener(
                new OnSuccessListener<List<FirebaseVisionObject>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionObject> firebaseVisionObjects) {
                        Corners firebaseCorners = corners;

                        if(firebaseVisionObjects.size() != 0){
                            FirebaseVisionObject obj = firebaseVisionObjects.get(0);

                            Rect bounds = obj.getBoundingBox();

                            points.add(new Point(bounds.left, bounds.top));
                            points.add(new Point(bounds.right, bounds.top));
                            points.add(new Point(bounds.right, bounds.bottom));
                            points.add(new Point(bounds.left, bounds.bottom));

                            firebaseCorners = new Corners(points, new Size(picture.width(), picture.height()));
                        }
                        else {
                            points.add(new Point(180.0, 320.0));
                            points.add(new Point(900.0, 320.0));
                            points.add(new Point(900.0, 1600.0));
                            points.add(new Point(180.0, 1600.0));

                            firebaseCorners = new Corners(points, new Size(picture.width(), picture.height()));
                        }

                        paper_rect.onCorners2Crop(firebaseCorners, picture.size());
                        Glide.with(CropActivity.this).load(bitmap).into(paper);

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });

    }

    public FirebaseVisionObjectDetector createFirebaseVisionObjectDetector(){
        FirebaseVisionObjectDetectorOptions options =
                new FirebaseVisionObjectDetectorOptions.Builder()
                        .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .build();

        FirebaseVisionObjectDetector objectDetector =
                FirebaseVision.getInstance().getOnDeviceObjectDetector(options);

        return objectDetector;
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


    public String saveImageToExternalStorage(Bitmap image) {
        try
        {
            OutputStream fOut = null;

            File file = createImageFile();

            fOut = new FileOutputStream(file);

            image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            return file.getAbsolutePath();

        }
        catch (Exception e)
        {
            Log.e("saveToExternalStorage()", e.getMessage());
            return null;
        }

    }


    public File createFile(String fileType, String path) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(System.currentTimeMillis());
        File storageDir = new File(path);
        if (!storageDir.exists())
            storageDir.mkdirs();
        File file = File.createTempFile(
                timeStamp,
                "." + fileType,
                storageDir
        );
        return file;
    }

    public File createImageFile() throws IOException {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/KimiScanner/";
        return createFile("jpeg", path);
    }

}
