package com.hcmus.thesis.nhatminhanhkiet.documentscanner;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.camera.CameraActivity;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.crop.CropActivity;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.processor.ImageProcessor;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.view.ImageFullscreen;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.view.ImageInfo;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.view.ImageListAdapter;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.view.ImageListItemClickListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    private static final int REQUEST_PERMISSION_CODE = 200;
    private static final int REQUEST_IMPORT_PHOTO = 2;

    private static boolean isPermissionGranted = false;

    FloatingActionButton fabOpenCamera;

    RecyclerView rvImageList;

    ImageListAdapter adapter;
    ArrayList<String> imagePathList;
    ArrayList<ImageInfo> imageInfoList;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.e("InitOpenCV", "Load OpenCV successfully");
                    break;

                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        prepare();
    }

    void prepare(){
        setupView();
        checkOpenCvSetup();
        checkPermission();

    }

    void setupView(){
        fabOpenCamera = findViewById(R.id.fabOpenCamera);
        fabOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        rvImageList = findViewById(R.id.rvImageList);
        rvImageList.setLayoutManager(new LinearLayoutManager(this));
        rvImageList.addOnItemTouchListener(new ImageListItemClickListener(this, rvImageList, new ImageListItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(MainActivity.this, ImageFullscreen.class);
                intent.putExtra("file_path", imageInfoList.get(position).filePath);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        }));
    }

    private ArrayList<ImageInfo> getImageInfo(List<String> paths){
        ArrayList<ImageInfo> imageInfoList = new ArrayList<>();

        for(int i=0;i<paths.size();i++){
            File file = new File(paths.get(i));
            if(file != null) {
                ImageInfo imageInfo = new ImageInfo();
                imageInfo.fileName = file.getName();
                imageInfo.filePath = paths.get(i);

                SimpleDateFormat simpleDateFormat =  new SimpleDateFormat("dd/MM/yyyy");
                Date lastModDate = new Date(file.lastModified());
                imageInfo.dateCreated = simpleDateFormat.format(lastModDate);

                imageInfo.fileSize = String.valueOf(file.length());

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(paths.get(i), options);
                imageInfo.imageHeight = options.outHeight;
                imageInfo.imageWidth = options.outWidth;

                imageInfoList.add(imageInfo);
            }
        }
        return imageInfoList;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap = null;

        if(resultCode == Activity.RESULT_OK) {
            switch (requestCode){
                case REQUEST_IMPORT_PHOTO:
                    if (data != null) {
                        Uri uri = data.getData();
                        ImageProcessor imageProcessor = new ImageProcessor(MainActivity.this);
                        imageProcessor.processImage(uri);
                    }
                    break;
            }
        }
        else {
            Toast.makeText(this, getResources().getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show();
        }

    }

    private void createImageList() {
        imagePathList =  getImagePath(retrieveImageFile());
        imageInfoList = getImageInfo(imagePathList);


        adapter = new ImageListAdapter(this, imageInfoList);
        rvImageList.setAdapter(adapter);
    }

    void checkOpenCvSetup(){
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV not loaded", Toast.LENGTH_SHORT).show();
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    void checkPermission(){
        isPermissionGranted = hasPermission(PERMISSIONS);
        if(isPermissionGranted){

        }
        else {
            askPermission();
        }
    }

    boolean hasPermission(String... permissions){
        if(permissions != null){
            for(String permission:permissions){
                if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    void askPermission(){
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_CODE:
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        isPermissionGranted = true;
                    }
                    else {
                        isPermissionGranted = false;
                    }
                }
            break;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        createImageList();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_import:
                searchImage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void searchImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMPORT_PHOTO);
    }

    public File[] retrieveImageFile(){
        File folder = new File(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/KimiScanner/");
        folder.mkdirs();
        File[] allFiles = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
            }
        });
        return allFiles;
    }

    public ArrayList<String> getImagePath(File[] files){
        ArrayList<String> filePathList = new ArrayList<>();
        for(int i =0;i<files.length;i++){
            filePathList.add(files[i].getPath());
        }
        return filePathList;
    }






}
