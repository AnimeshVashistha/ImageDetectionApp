package com.amazingvikings.imagedetectionapp;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

rt android.widget.LinearLayout;

impo
public class MainActivity extends AppCompatActivity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    @BindView(R.id.layout_preview)
    LinearLayout cameraPreview;
    @BindView(R.id.txt_result)
    TextView txtResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
//keep screen always on       getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        checkPermission();
    }
    //camera permission is a dangerous permission, so the user should grant this permission directly in real time. Here we show a permission pop-up and listen for the user’s response.
    private void checkPermission() {
        //Set up the permission listener
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                setupPreview();
            }
            @Override
            public void onPermissionDenied(ArrayList deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };
        //Check camera permission
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.CAMERA)
                .check();
    }
    //Here we set up the camera preview
    private void setupPreview() {
        mCamera = Camera.open();
        mPreview = new CameraPreview(getBaseContext(), mCamera);
        try {
            //Set camera autofocus
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
        }catch (Exception e){
        }
        cameraPreview.addView(mPreview);
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
        mPicture = getPictureCallback();
        mPreview.refreshCamera(mCamera);
    }
    // take photo when the users tap the button
    @OnClick(R.id.btn_take_picture)
    public void takePhoto() {
        mCamera.takePicture(null, null, mPicture);
    }
    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
    //Here we get the photo from the camera and pass it to mlkit processor
    private Camera.PictureCallback getPictureCallback() {
        return new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                mlinit(BitmapFactory.decodeByteArray(data, 0, data.length));
                mPreview.refreshCamera(mCamera);
            }
        };
    }
    //the main method that processes the image from the camera and gives labeling result
    private void mlinit(Bitmap bitmap) {
        //By default, the on-device image labeler returns at most 10 labels for an image.
        //But it is too much for us and we wants to get less
        FirebaseVisionLabelDetectorOptions options =
                new FirebaseVisionLabelDetectorOptions.Builder()
                        .setConfidenceThreshold(0.5f)
                        .build();
        //To label objects in an image, create a FirebaseVisionImage object from a bitmap
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        //Get an instance of FirebaseVisionCloudLabelDetector
        FirebaseVisionLabelDetector detector = FirebaseVision.getInstance()
                .getVisionLabelDetector(options);
        detector.detectInImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener() {
                            @Override
            public void onSuccess(List labels) {
                StringBuilder builder = new StringBuilder();
                // Get information about labeled objects
                for (FirebaseVisionLabel label : labels) {
                    builder.append(label.getLabel())
                            .append(" ")
                            .append(label.getConfidence()).append("\n");
                }
                txtResult.setText(builder.toString());
            }
        })
               .addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtResult.setText(e.getMessage());
                    }
                });
    }
}