package com.hcmus.thesis.nhatminhanhkiet.documentscanner.view;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.R;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor.ImageProcessor;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ImageFullscreen extends AppCompatActivity implements View.OnClickListener {

    String filePath;
    ImageView imageView;

    Button btnExportPdf, btnShare, btnDelete, btnRotate;
    ToggleButton btnBWConversion;
    ProgressBar pbLoading;

    ImageProcessor imageProcessor;
    Bitmap srcBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_fullscreen);
        setupView();

        filePath = getIntent().getExtras().getString("file_path");

        imageView = findViewById(R.id.imageView);

        srcBitmap = BitmapFactory.decodeFile(filePath);

        loadBitmapWithGlide(srcBitmap);

    }

    public void setupView(){
        btnExportPdf = findViewById(R.id.btnExportPdf);
        btnExportPdf.setOnClickListener(this);

        btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(this);

        btnBWConversion = findViewById(R.id.btnBWConversion);
        btnBWConversion.setOnClickListener(this);

        btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(this);

        pbLoading = findViewById(R.id.pbLoading);
        pbLoading.setVisibility(View.GONE);

        btnRotate = findViewById(R.id.btnRotate);
        btnRotate.setOnClickListener(this);
        btnRotate.setVisibility(View.GONE);
    }

    private void loadBitmapWithGlide(Bitmap bitmap){
        Glide.with(this).load(bitmap)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(imageView);

        imageProcessor = new ImageProcessor(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnExportPdf:
                String name = FilenameUtils.getBaseName(filePath);
                new PdfExporter().execute(name);
                break;
            case R.id.btnShare:
                sharePicture(filePath);
                break;

            case R.id.btnBWConversion:
                if(btnBWConversion.isChecked()){
                    convertBitmapToGrayscale();
                }
                else {
                    loadBitmapWithGlide(srcBitmap);
                }
                break;
            case R.id.btnDelete:
                deleteFileByPath(filePath);
                finish();
                break;

            case R.id.btnRotate:
                rotatePhoto();
                break;
        }
    }

    private void rotatePhoto() {
        Bitmap bitmap =  ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        Matrix matrix = new Matrix();
        //degrees+=90;
        matrix.postRotate(90);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,  bitmap.getWidth(), bitmap.getHeight(),true);
        bitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        Glide.with(this).load(bitmap)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(imageView);

        scaledBitmap.recycle();
    }

    public void deleteFileByPath(String filePath)
    {
        File originalFile =  new File(filePath);

        if (!originalFile.exists()) {
            Toast.makeText(this ,getResources().getString(R.string.file_not_found),Toast.LENGTH_SHORT).show();
        }
        else {
            originalFile.delete();
            Toast.makeText(this,getResources().getString(R.string.delete_successfully),Toast.LENGTH_SHORT).show();
        }
    }

    private void convertBitmapToGrayscale(){
        Bitmap bitmap = imageProcessor.convertToGrayscale( ((BitmapDrawable) imageView.getDrawable()).getBitmap());
        Glide.with(this).load(bitmap)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true)
                .into(imageView);
    }

    private void sharePicture(String imagePath) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");

        File imageFileToShare = new File(imagePath);

        Uri uri = Uri.fromFile(imageFileToShare);
        share.putExtra(Intent.EXTRA_STREAM, uri);

        startActivity(Intent.createChooser(share, "Share via"));
    }


    public class PdfExporter extends AsyncTask<String, Void, Void>{

        String destPdfPath;

        public PdfExporter(){
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbLoading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... strings) {
            createPdf(strings[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pbLoading.setVisibility(View.GONE);
            Toast.makeText(ImageFullscreen.this, getResources().getString(R.string.export_complete), Toast.LENGTH_SHORT).show();

            File file = new File(destPdfPath);
            Intent target = new Intent(Intent.ACTION_VIEW);
            target.setDataAndType(Uri.fromFile(file),"application/pdf");
            target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            Intent intent = Intent.createChooser(target, "Open file");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(ImageFullscreen.this, "Can not open pdf file", Toast.LENGTH_SHORT).show();
            }
        }

        private void createPdf(String filename){
            Bitmap bitmap;
            try {
                //bitmap = BitmapFactory.decodeFile(path);
                bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

            } catch (OutOfMemoryError error){
                return;
            }

            if(bitmap == null){
                return;
            }

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            float hight = displaymetrics.heightPixels ;
            float width = displaymetrics.widthPixels ;

            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();


            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#ffffff"));
            canvas.drawPaint(paint);

            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);

            paint.setColor(Color.BLUE);
            canvas.drawBitmap(bitmap, 0, 0 , null);
            document.finishPage(page);



            // write the document content
            String targetPdf = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/KimiScanner/" + filename +"_pdf" + ".pdf";
            File filePath = new File(targetPdf);
            try {
                document.writeTo(new FileOutputStream(filePath));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            document.close();

            destPdfPath = targetPdf;
        }
    }


}
