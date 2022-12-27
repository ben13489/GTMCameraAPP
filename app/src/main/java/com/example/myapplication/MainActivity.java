package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class MainActivity<tvCount> extends AppCompatActivity {
    private USBStatus status = new USBStatus();
    private TextView touchView;
    private SeekBar seekbar;
    DrawerLayout drawerLayout;

    /**內建相機宣告*/
    SurfaceView surfaceView;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;

    public static final String TAG = MainActivity.class.getSimpleName() + "GTC-016";
    private static final String USB_PERMISSION = "USB_Demo";
    TextView tvStatus, tvInfo, tvRes, tv_SeekbarUp,tv_SeekbarDown;

    UsbManager manager;
    List<UsbSerialDriver> drivers;
    String res = "";  //最高溫度
    int[][] rematrix; //翻轉過後的0-256方陣
    float []temperature1 = new float [256]; //溫度陣列
    int high_place = 0; //最高溫位置
    int RawX = -1; //觸控點實際座標X
    int RawY = -1; //觸控點實際座標Y
    int spinner_interpolation = 0; //插補下拉選單變數
    int spinner_color = 0; //上色下拉選單變數
    int dataup = 4048;
    int datadown = 2000;
    int offset =0;
    int start_check =0;//start 按鈕判定是否按過
    int flip_count = 0; //旋轉180度
    int i =0; //捲積
    int Screen_width = 0;
    int Screen_height_helf = 0;
    int Screen_draw_size = 0;
    Handler handler;





    private ImageView imageView;
    private static final String TAG1 = "TestActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //相機權限
        getPermissioncamera();
        surfaceView=(SurfaceView)findViewById(R.id.surfaceview);
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();

        cameraSource=new CameraSource.Builder(this,barcodeDetector)
                .setRequestedPreviewSize(300,300).build();

        //顯示相機
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if(ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED)
                    return;
                try{
                    cameraSource.start(holder);
                }catch (IOException e){
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();

            }
        });
        //掃描
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>(){

            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

            }
        });



        //獲取螢幕長一半與寬
        Resources resources = this.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Screen_width = dm.widthPixels;
        Screen_height_helf = dm.heightPixels/2;
        int Screen_X = 0;
        if(Screen_width<=Screen_height_helf){
            Screen_draw_size = Screen_width;
            Screen_X = Screen_height_helf - Screen_draw_size;
        }else {
            Screen_draw_size = Screen_height_helf;
            Screen_X = Screen_width - Screen_draw_size;
        }



        touchView = findViewById(R.id.touch_area);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(Screen_draw_size, Screen_draw_size);
        touchView.setLayoutParams(params);





        //差補選擇
        Spinner mySpinner = findViewById(R.id.spinner);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.interpolation));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(myAdapter);

        //上色選擇
        Spinner ColorSpinner = findViewById(R.id.spinner2);
        ArrayAdapter<String> ColorAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.color));
        ColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ColorSpinner.setAdapter(ColorAdapter);


        touchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();//獲得事件動作
                switch (action){
                    //手指按下的时候
                    case(MotionEvent.ACTION_DOWN):
                        Display(event);
                        break;
                }
                return  true;
            }

        });




        boolean status1 = OpenCVLoader.initDebug();
        if(status1){
            Log.e(TAG, "onCreate: Succese");
        }else{
            Log.e(TAG, "onCreate: Failed");
        }
        imageView = findViewById(R.id.test_image);


        /*註冊廣播*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(USB_PERMISSION);
        registerReceiver(status, filter);
        /*Find UIs*/
        tvStatus = findViewById(R.id.textView_Status);
       // tvInfo = findViewById(R.id.textView_Info);
        tvRes = findViewById(R.id.textView_Respond);
        tv_SeekbarUp =  findViewById(R.id.tv_UpSeekbar);
        tv_SeekbarDown =  findViewById(R.id.tv_DownSeekbar);


        Button btStart = findViewById(R.id.button_Start);
        Button btS = findViewById(R.id.button2);
        Button btFlip = findViewById(R.id.button_Flip);
        Button btSend = findViewById(R.id.button_Send);


        /**可變式上界*/
        SeekBar upseekbar = (SeekBar) findViewById(R.id.seekbar_Up);

        upseekbar.setMax(100);//設定SeekBar最大值

        upseekbar.setProgress(40);//設定SeekBar拖移初始值

        /**可變式下界*/
        SeekBar downseekbar = (SeekBar) findViewById(R.id.seekBar_down);

        downseekbar.setMax(upseekbar.getMax() - 1);//設定SeekBar最大值

        downseekbar.setProgress(20);//設定SeekBar拖移初始值


        /**選擇是否插補*/
        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    spinner_interpolation = 0;
                } else if (i == 1) {
                    spinner_interpolation = 1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        /**選擇顏色選項*/
        ColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    spinner_color = 0;
                } else if (i == 1) {
                    spinner_color = 1;
                } else if (i == 2) {
                    spinner_color = 2;
                } else if (i == 3) {
                    spinner_color = 3;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        btSend.setOnClickListener(view->{
            sendValue(drivers);


        });
        
        





        /**對裝置送出指令*/
        int finalScreen_X = Screen_X;
        btStart.setOnClickListener(view -> {
            //EditText edInput1 = findViewById(R.id.editTextText_Offset);
            offset =0;// Integer.parseInt(edInput1.getText().toString());


            if(start_check ==0) {

                synchronized (this) {

                    /**handler 定時多久跑一次*/
                    handler = new Handler();

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {

                            send_start(drivers);

                            /**send值等0.2秒後再拿方陣畫圖*/
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.postDelayed(this, 60);


                            /**slide 直線數值變換動作決定上下界*/
                            upseekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override

                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    tv_SeekbarUp.setText("上界" + progress);
                                    dataup = progress * 100;
                                    if (dataup <= datadown) {
                                        downseekbar.setProgress(progress - 1);
                                        datadown = (progress - 1) * 100;
                                        int down_count = progress - 1;
                                        ;
                                        tv_SeekbarDown.setText("下界" + down_count);

                                    }
                                }

                                @Override

                                public void onStartTrackingTouch(SeekBar seekBar) {
                                }

                                @Override

                                public void onStopTrackingTouch(SeekBar seekBar) {

                                }
                            });

                            downseekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override

                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    tv_SeekbarDown.setText("下界" + progress);
                                    datadown = progress * 100;
                                    if (dataup <= datadown) {
                                        dataup = (progress + 1) * 100;
                                        upseekbar.setProgress(progress + 1);
                                        int Up_count = progress + 1;
                                        tv_SeekbarUp.setText("上界" + Up_count);
                                    }

                                }

                                @Override

                                public void onStartTrackingTouch(SeekBar seekBar) {
                                }

                                @Override

                                public void onStopTrackingTouch(SeekBar seekBar) {

                                }
                            });


                            int draw_X = Screen_draw_size / 16;
                            int draw_Y = Screen_draw_size / 16;
                            Bitmap destbitmap = Bitmap.createBitmap(Screen_draw_size, Screen_draw_size, Bitmap.Config.ARGB_8888);
                            /**最底層畫布*/
                            Mat srcMat = new Mat();

                            Utils.bitmapToMat(destbitmap, srcMat);
                            //Mat srcMat = new Mat(384, 384, CvType.CV_8UC3);
                            srcMat.setTo(new Scalar(255, 255, 255));

                            /**第二層畫布*/

                            Mat srcMat2 = new Mat();
                            Mat srcMat3 = new Mat();
                            Size size1 = new Size(16, 16);
                            Size size2 = new Size(64, 64);//16-64cu
                            //Size size3 = new Size(128, 128);
                            //Size size4 = new Size(360, 360);
                            Size size5 = new Size(Screen_draw_size, Screen_draw_size);//ner


                            for (int j = 0; j < 16; j++) {
                                for (int i = 0; i < 16; i++) {
                                    /**拿整理好的date陣列由左至右作為RGB參數*/
                                    int RGB = rematrix[j][i];//& 0xff ;
                                    /**宽度16*16像素，-1填滿*/
                                    //Imgproc.rectangle(srcMat, new Point(0+i*24,0+j*24), new Point(0+(i+1)*24, 0+(j+1)*24), new Scalar((j*16+i+1),(j*16+i+1),(j*16+i+1),255), -1);
                                    //Imgproc.rectangle(srcMat, new Point(i * draw_X, j * draw_Y), new Point(((i + 1) * draw_X -1), ((j + 1) * draw_Y -1)), new Scalar(RGB, RGB, RGB, 255), -1);
                                    Imgproc.rectangle(srcMat, new Point( i*2 , j*2 ),new Point( ((i+1)*2-1) , ((j+1)*2-1) ), new Scalar(RGB, RGB, RGB, 255), -1);
                                }
                            }


                            /**插補選擇*/
                            if (spinner_interpolation == 0) {
                                Imgproc.resize(srcMat, srcMat3, size1,Imgproc.INTER_NEAREST);
                                //Imgproc.resize(srcMat2, srcMat, size5);
                                Imgproc.resize(srcMat3, srcMat, size5, Imgproc.INTER_LINEAR);
                                //Imgproc.resize(srcMat3, srcMat, size5, Imgproc.INTER_NEAREST);

                            } else {


                            }



                            /**捲積選擇*/
                            /*if (i == 1) {
                                Utils.matToBitmap(srcMat, destbitmap);
                                destbitmap = sharpenImageAmeliorate(destbitmap);
                                Utils.bitmapToMat(destbitmap, srcMat);
                            }*/


                            /**上色需要RGBA會轉成RGB所以要對調*/

                            //Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB);
                            /**上色*/

                           /* if (spinner_color == 0) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_INFERNO);
                            } else if (spinner_color == 1) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_JET);
                            } else if (spinner_color == 2) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_COOL);
                            } else if (spinner_color == 3) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_HOT);
                            }*/


                            /**apply後RGB會轉成GBR所以要對調RGB2BGR*/
                            //Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_BGR2RGBA);

                            /**找出最高溫方框*/
                            //Imgproc.rectangle(srcMat, new Point(0 + (high_place % 16) * draw_X, 0 + (high_place / 16) * draw_Y), new Point(0 + ((high_place % 16) + 1) * draw_X, 0 + ((high_place / 16) + 1) * draw_Y), new Scalar(255, 0, 0, 255), 2);


                            /**翻轉check*/
                            /*if (flip_count == 0) {
                                Core.flip(srcMat, srcMat, -1);
                            }*/
                            /**點擊時畫綠色方框並顯示格子內溫度*/
                           /* if (RawX >= 0) {
                                Imgproc.rectangle(srcMat, new Point(0 + (RawX / draw_X) * draw_X, 0 + ((RawY) / draw_Y) * draw_Y), new Point(0 + ((RawX / draw_X) + 1) * draw_X, 0 + (((RawY) / draw_Y) + 1) * draw_Y), new Scalar(0, 255, 0, 255), 2);
                                float touch_temp = temperature1[(RawX / draw_X) + (RawY / draw_Y) * 16];

                                if (flip_count == 0) {
                                    touch_temp = temperature1[255 - ((RawX / draw_X) + (RawY / draw_Y) * 16)];
                                }

                                /**授權狀態改成溫度*/
                               /* tvStatus.setText(Float.toString(touch_temp));
                                tvStatus.setTextColor(Color.rgb(0, 255, 0));


                            }*/


                            /**將mat轉為Bitmap畫圖*/
                            Utils.matToBitmap(srcMat, destbitmap);


                            imageView.setImageBitmap(destbitmap);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Screen_draw_size, Screen_draw_size);
                            lp.gravity = Gravity.CENTER;
                            imageView.setLayoutParams(lp);


                        }
                    };
                    handler.postDelayed(runnable, 2);

                }
            }
            if(start_check ==0){
                start_check = 1;
            }


        });

        /**翻轉180*/
        btFlip.setOnClickListener(view->{
            if(flip_count ==0){
                flip_count = 1;
            }else {
                flip_count =0;}

        });
        btS.setOnClickListener(view -> {
            if(i ==0){
                i = 1;
            }else {
                i =0;}

        });


        /**偵測是否正在有裝置插入*/
        detectUSB();


    }

    /**相機權限獲取*/
    public void getPermissioncamera() {
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }
    }


    /**側邊攔button*/
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawers();
            }else finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }






    @Override
    protected void onStop() {
        super.onStop();
        /**反註冊廣播*/
        unregisterReceiver(status);
    }



    private class USBStatus extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                /**當Check完授權狀態後進入此處*/
                case USB_PERMISSION:
                    if (drivers.size() == 0) return;
                    boolean hasPermission = manager.hasPermission(drivers.get(0).getDevice());
                    tvStatus.setText("授權: " + hasPermission);
                    if (!hasPermission) {
                        getPermission(drivers);
                        return;
                    }
                    Toast.makeText(context, "已獲取權限", Toast.LENGTH_SHORT).show();
                    break;
                /**偵測USB裝置插入*/
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Toast.makeText(context, "USB裝置插入", Toast.LENGTH_SHORT).show();
                    detectUSB();
                    break;
                /**偵測USB裝置拔出*/
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Toast.makeText(context, "USB裝置拔出", Toast.LENGTH_SHORT).show();
                    //tvInfo.setText("TextView");
                    tvRes.setText("TextView");
                    tvStatus.setText("授權: false");
                    break;
            }
        }
    }
    /**偵測裝置*/
    private void detectUSB() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager == null) return;
        if (manager.getDeviceList().size() == 0)return;
        tvStatus.setText("授權: false");
        /*取得目前插在USB-OTG上的裝置*/
        drivers = getDeviceInfo();
        /*確認使用者是否有同意使用OTG(權限)*/
        getPermission(drivers);
    }
    /**取得目前插在USB-OTG上的裝置列表，並取得"第一個"裝置的資訊*/
    private List<UsbSerialDriver> getDeviceInfo() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.d(TAG, "裝置資訊列表:\n " + deviceList);
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ProbeTable customTable = new ProbeTable();
        List<UsbSerialDriver> drivers = null;
        String info = "";
        while (deviceIterator.hasNext()) {
            /**取得裝置資訊*/
            UsbDevice device = deviceIterator.next();
            info = "Vendor ID: " + device.getVendorId()
                    + "\nProduct Id: " + device.getDeviceId()
                    + "\nManufacturerName: " + device.getManufacturerName()
                    + "\nProduceName: " + device.getProductName();
            /**設置驅動*/
            customTable.addProduct(
                    device.getVendorId(),
                    device.getProductId(),
                    CdcAcmSerialDriver.class
                    /**我的設備Diver是CDC，另有
                     * CP21XX, CH34X, FTDI, Prolific 等等可以使用*/
            );
            /**將驅動綁定給此裝置*/
            UsbSerialProber prober = new UsbSerialProber(customTable);
            drivers = prober.findAllDrivers(manager);
        }
        /**更新UI*/
        //tvInfo.setText(info);
        return drivers;
    }
    /**確認OTG使用權限，此處為顯示詢問框*/
    private void getPermission(List<UsbSerialDriver> drivers) {
        if (PendingIntent.getBroadcast(this, 0, new Intent(USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE) != null) {
            manager.requestPermission(drivers.get(0).getDevice(), PendingIntent.getBroadcast(
                    this, 0, new Intent(USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)); }
    }
    /**送出資訊*/
    private synchronized void send_start(List<UsbSerialDriver> drivers) {

        if (drivers == null) return;
        /**初始化整個發送流程*/
        UsbDeviceConnection connect = manager.openDevice(drivers.get(0).getDevice());
        /**取得此USB裝置的PORT*/
        UsbSerialPort port = drivers.get(0).getPorts().get(0);
        try {
            /**開啟port*/
            port.open(connect);
            /**要發送的字串*/

            String s = "S"; //edInput.getText().toString();
            if (s.length() == 0) return;
            /**設定胞率、資料長度、停止位元、檢查位元*/
            port.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            /**寫出資訊*/
            port.write(s.getBytes(), 50);

            /**設置回傳執行緒*/
            SerialInputOutputManager.Listener serialInputOutputManager = getRespond;
            SerialInputOutputManager sL = new SerialInputOutputManager(port, serialInputOutputManager);
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(sL);
        } catch (IOException e) {
            try {
                /**如果Port是開啟狀態，則關閉；再使用遞迴法重複呼叫並嘗試*/
                port.close();
                send_start(drivers);
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "送出失敗，原因: " + ex);
            }
        }
    }

    private synchronized void sendValue(List<UsbSerialDriver> drivers) {

        if (drivers == null) return;
        /**初始化整個發送流程*/
        UsbDeviceConnection connect = manager.openDevice(drivers.get(0).getDevice());
        /**取得此USB裝置的PORT*/
        UsbSerialPort port = drivers.get(0).getPorts().get(0);
        try {
            /**開啟port*/
            port.open(connect);


            /**設定胞率、資料長度、停止位元、檢查位元*/
            port.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            /**寫出資訊*/

                EditText edInput = findViewById(R.id.editText_Input);
                String test = edInput.getText().toString();
                int editnumber = Integer.valueOf(test).intValue();
                String str = new Character((char) editnumber).toString();
                port.write(str.getBytes(), 50);

            /**設置回傳執行緒*/

        } catch (IOException e) {

            try {
                /**如果Port是開啟狀態，則關閉；再使用遞迴法重複呼叫並嘗試*/
                port.close();
                sendValue(drivers);
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "送出失敗，原因: " + ex);
            }
        }
    }


    /**接收回傳*/
    private  SerialInputOutputManager.Listener getRespond = new SerialInputOutputManager.Listener() {
        @Override
        public synchronized void onNewData(byte[] data) {

            /**切割一個一個數字*/
            byte[][] singlematrix = splitBytes(data,1);
            float []temperature = new float [256];
            int[][] RGB_number = new int[256][1];
            float maxtemp = 0;

            for(int i = 0;i<256;i++){
                /**將回傳512筆資料兩兩恢復成溫度值*/
                int a = singlematrix[i*2][0] & 0xff;
                int b = singlematrix[i*2+1][0] & 0xff ;
                /**溫度值為幾千轉為正常有小數溫度*/
                float count1 = (b*256+a);
                count1 = count1/100 -offset;
                temperature[i] = count1;

                /**找最高溫及其位置*/
                    if(maxtemp<count1){
                        maxtemp = count1;
                        high_place = i;
                        res = Float.toString(maxtemp);

                    }


                /**溫度切換成RGB數值並存入陣列*/
                int result =  ((b*256+a)-datadown)/((dataup-datadown)/256);
                if(result>=255){
                    RGB_number[i][0] =255;
                }else if(result<=0){
                    RGB_number[i][0]  =0;
                }else{
                    RGB_number[i][0]  = result;
                }

            }



            /**儲存溫度陣列給外部使用*/
            temperature1 = temperature;

            runOnUiThread(() -> {

                tvRes.setText(res);
                tvRes.setTextColor(Color.rgb(255,0, 0));
            });
            /**重整成16*16陣列*/
            rematrix = (int[][]) matrixReshape(RGB_number,16,16);

            //res =byteArrayToHexStr(data);
            Log.d(TAG,  res);
            runOnUiThread(() -> {
                //tvRes.setText(res);
            });
        }
        @Override
        public void onRunError(Exception e) {
        }
    };
    /**將ByteArray轉成字串可顯示的ASCII*/
    private String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        StringBuilder hex = new StringBuilder(byteArray.length * 2);
        for (byte aData : byteArray) {
            hex.append(String.format("%d ", aData  & 0xff));
        }
        String gethex = hex.toString();
        return gethex;
    }
    /**將ByteArray切割一個一個的數字*/
    private byte[][] splitBytes(byte[] bytes, int size) {
        double splitLength = Double.parseDouble(size + "");
        int arrayLength = (int) Math.ceil(bytes.length / splitLength);
        byte[][] result = new byte[arrayLength][];
        int from, to;
        for (int i = 0; i < arrayLength; i++) {
            from = (int) (i * splitLength);
            to = (int) (from + splitLength);
            if (to > bytes.length)
                to = bytes.length;
            result[i] = Arrays.copyOfRange(bytes, from, to);
        }
        return result;
    }


    /**將陣列reshape成16*16*/
    private int[][] matrixReshape(int[][] nums, int r, int c) {
        if(nums == null){
            return null;
        }
        if(r == 0 || c == 0){
            return null;
        }
        int row = 0;//行
        int columns = 0;//列
        columns = nums[0].length;
        row = nums.length;
        /**回傳格是因為1*256所以改>260才null*/
        if(columns > 260 || row > 260 || columns < 1 || row < 1){
            return null;
        }
        if(columns * row < r * c){
            return null;
        }
        int [][]result = new int[r][c];
        int rr = 0;
        int cc = 0;
        int index = 0;
        for(int i = 0; i < row; i ++){
            for(int j = 0; j < columns; j ++){
                index = i * columns + j + 1;
                if(index > r * c){
                    break;
                }
                rr = index / c;//行
                cc = index % c;//列
                if(rr > 0 && cc ==0){
                    rr = rr -1;
                    cc = c - 1;
                }else if(rr > 0 && cc > 0){
                    cc = cc -1;
                }else{
                    cc = cc -1;
                }
                result[rr][cc] = Integer.parseInt(String.valueOf(nums[i][j]));
            }
        }
        return result;
    }

    public void Display(MotionEvent event){
        /**觸控相對座標*/
        int x = (int)event.getX();
        int y = (int)event.getY();
        /**觸控絕對座標*/
         RawX = (int)event.getX();
         RawY = (int)event.getY();
        //tvStatus.setText(String.valueOf(RawX)+","+String.valueOf(RawY)+"\n");
    }




    private Bitmap sharpenImageAmeliorate(Bitmap bmp)
    {
        // 拉普拉斯矩阵
        //int[] laplacian = new int[]{-1, -1, -1, -1, 9, -1, -1, -1, -1};
        //        int[] laplacian = new int[]{0, -1, 0, -1, 5, -1, 0, -1, 0};
        int[] laplacian = new int[]{1, -2, 1, -2, 5, -2, 1, -2, 1};
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        int pixR = 0;
        int pixG = 0;
        int pixB = 0;

        int pixColor = 0;

        int newR = 0;
        int newG = 0;
        int newB = 0;

        int idx = 0;
        float alpha = 1F;
        //原本像素點數組
        int[] pixels = new int[width*height];
        //創一個新數據保存銳化后的像素點
        int[] pixels_1 = new int[width*height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for(int i = 1, length = height-1; i<length; i++)
        {
            for(int k = 1, len = width-1; k<len; k++)
            {
                idx = 0;
                for(int m = -1; m<=1; m++)
                {
                    for(int n = -1; n<=1; n++)
                    {
                        pixColor = pixels[( i+n )*width+k+m];
                        pixR = Color.red(pixColor);
                        pixG = Color.green(pixColor);
                        pixB = Color.blue(pixColor);

                        newR = newR+(int)( pixR*laplacian[idx]*alpha );
                        newG = newG+(int)( pixG*laplacian[idx]*alpha );
                        newB = newB+(int)( pixB*laplacian[idx]*alpha );
                        idx++;
                    }
                }

                newR = Math.min(255, Math.max(0, newR));
                newG = Math.min(255, Math.max(0, newG));
                newB = Math.min(255, Math.max(0, newB));

                pixels_1[i*width+k] = Color.argb(255, newR, newG, newB);
                newR = 0;
                newG = 0;
                newB = 0;
            }
        }

        bitmap.setPixels(pixels_1, 0, width, 0, 0, width, height);
        return bitmap;

    }





}



