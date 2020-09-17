package com.jeffmony.mediacodecdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements ExtractVideoFrameTask.Callback {

    private static final String TAG = "MainAactivity";

    private static final int REQUEST_CODE_GET_FILE_PATH = 1;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private OutputImageFormat outputImageFormat;
    private MainActivity self = this;
    private String outputDir;

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            String str = (String) msg.obj;
            updateInfo(str);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initImageFormatSpinner();

        final Button buttonFilePathInput = (Button) findViewById(R.id.button_file_path_input);
        buttonFilePathInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
                } else {
                    getFilePath(REQUEST_CODE_GET_FILE_PATH);
                }
            }
        });

        final Button buttonStart = (Button) findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextOutputFolder = (EditText) findViewById(R.id.folder_created);
                outputDir = editTextOutputFolder.getText().toString();
                EditText editTextInputFilePath = (EditText) findViewById(R.id.file_path_input);
                String inputFilePath = editTextInputFilePath.getText().toString();
                ExtractVideoFrameTask task = new ExtractVideoFrameTask();
                task.setCallback(self);
                try {
                    task.setSaveFrames(outputDir, outputImageFormat);
                    updateInfo("运行中...");
                    task.decode(inputFilePath);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getFilePath(REQUEST_CODE_GET_FILE_PATH);
                } else {
                    Toast.makeText(this, "需要开启文件读写权限", Toast.LENGTH_SHORT).show();
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void initImageFormatSpinner() {
        Spinner barcodeFormatSpinner = (Spinner) findViewById(R.id.image_format);
        ArrayAdapter<OutputImageFormat> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, OutputImageFormat.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barcodeFormatSpinner.setAdapter(adapter);
        barcodeFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                outputImageFormat = OutputImageFormat.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void getFilePath(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "选择视频文件"), requestCode);
        } else {
            new AlertDialog.Builder(this).setTitle("未找到文件管理器")
                    .setMessage("请安装文件管理器以选择文件")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int id = 0;
        switch (requestCode) {
            case REQUEST_CODE_GET_FILE_PATH:
                id = R.id.file_path_input;
                break;
        }
        if (resultCode == Activity.RESULT_OK) {
            EditText editText = (EditText) findViewById(id);
            String curFileName = getRealPathFromURI(data.getData());
            Log.i(TAG, "curFileName = " + curFileName);
            editText.setText(curFileName);
        }
    }

    private void updateInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    public void onDecodeFrame(int index) {
        Message msg = handler.obtainMessage();
        msg.obj = "运行中...第" + index + "帧";
        handler.sendMessage(msg);
    }

    public void onFinishDecode() {
        Message msg = handler.obtainMessage();
        msg.obj = "完成！所有图片已存储到" + outputDir;
        handler.sendMessage(msg);
    }

    private String getRealPathFromURI(Uri contentURI) {
        Log.i(TAG, "getRealPathFromURI uri=" + contentURI);
        String result = contentURI.toString();
        String[] proj = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(contentURI, proj, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            if (cursor.moveToFirst()) {
                String[] arr = cursor.getColumnNames();
                for (String str : arr) {
                    Log.i(TAG, "STR = " +str);
                }
                int idx = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                result = cursor.getString(idx);
                Log.i(TAG, "idx = " +idx + ", result=" + result);
            }
            cursor.close();
        }
        return result;
    }
}