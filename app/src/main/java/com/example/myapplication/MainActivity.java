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

    /**??????????????????*/
    SurfaceView surfaceView;
    CameraSource cameraSource;
    BarcodeDetector barcodeDetector;

    public static final String TAG = MainActivity.class.getSimpleName() + "GTC-016";
    private static final String USB_PERMISSION = "USB_Demo";
    TextView tvStatus, tvInfo, tvRes, tv_SeekbarUp,tv_SeekbarDown;

    UsbManager manager;
    List<UsbSerialDriver> drivers;
    String res = "";  //????????????
    int[][] rematrix; //???????????????0-256??????
    float []temperature1 = new float [256]; //????????????
    int high_place = 0; //???????????????
    int RawX = -1; //?????????????????????X
    int RawY = -1; //?????????????????????Y
    int spinner_interpolation = 0; //????????????????????????
    int spinner_color = 0; //????????????????????????
    int dataup = 4048;
    int datadown = 2000;
    int offset =0;
    int start_check =0;//start ????????????????????????
    int flip_count = 0; //??????180???
    int i =0; //??????
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

        //????????????
        getPermissioncamera();
        surfaceView=(SurfaceView)findViewById(R.id.surfaceview);
        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();

        cameraSource=new CameraSource.Builder(this,barcodeDetector)
                .setRequestedPreviewSize(300,300).build();

        //????????????
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
        //??????
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>(){

            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {

            }
        });



        //???????????????????????????
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





        //????????????
        Spinner mySpinner = findViewById(R.id.spinner);

        ArrayAdapter<String> myAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.interpolation));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(myAdapter);

        //????????????
        Spinner ColorSpinner = findViewById(R.id.spinner2);
        ArrayAdapter<String> ColorAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.color));
        ColorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ColorSpinner.setAdapter(ColorAdapter);


        touchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();//??????????????????
                switch (action){
                    //?????????????????????
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


        /*????????????*/
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


        /**???????????????*/
        SeekBar upseekbar = (SeekBar) findViewById(R.id.seekbar_Up);

        upseekbar.setMax(100);//??????SeekBar?????????

        upseekbar.setProgress(40);//??????SeekBar???????????????

        /**???????????????*/
        SeekBar downseekbar = (SeekBar) findViewById(R.id.seekBar_down);

        downseekbar.setMax(upseekbar.getMax() - 1);//??????SeekBar?????????

        downseekbar.setProgress(20);//??????SeekBar???????????????


        /**??????????????????*/
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
        /**??????????????????*/
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
        
        





        /**?????????????????????*/
        int finalScreen_X = Screen_X;
        btStart.setOnClickListener(view -> {
            //EditText edInput1 = findViewById(R.id.editTextText_Offset);
            offset =0;// Integer.parseInt(edInput1.getText().toString());


            if(start_check ==0) {

                synchronized (this) {

                    /**handler ?????????????????????*/
                    handler = new Handler();

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {

                            send_start(drivers);

                            /**send??????0.2????????????????????????*/
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            handler.postDelayed(this, 60);


                            /**slide ???????????????????????????????????????*/
                            upseekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                @Override

                                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                    tv_SeekbarUp.setText("??????" + progress);
                                    dataup = progress * 100;
                                    if (dataup <= datadown) {
                                        downseekbar.setProgress(progress - 1);
                                        datadown = (progress - 1) * 100;
                                        int down_count = progress - 1;
                                        ;
                                        tv_SeekbarDown.setText("??????" + down_count);

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
                                    tv_SeekbarDown.setText("??????" + progress);
                                    datadown = progress * 100;
                                    if (dataup <= datadown) {
                                        dataup = (progress + 1) * 100;
                                        upseekbar.setProgress(progress + 1);
                                        int Up_count = progress + 1;
                                        tv_SeekbarUp.setText("??????" + Up_count);
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
                            /**???????????????*/
                            Mat srcMat = new Mat();

                            Utils.bitmapToMat(destbitmap, srcMat);
                            //Mat srcMat = new Mat(384, 384, CvType.CV_8UC3);
                            srcMat.setTo(new Scalar(255, 255, 255));

                            /**???????????????*/

                            Mat srcMat2 = new Mat();
                            Mat srcMat3 = new Mat();
                            Size size1 = new Size(16, 16);
                            Size size2 = new Size(64, 64);//16-64cu
                            //Size size3 = new Size(128, 128);
                            //Size size4 = new Size(360, 360);
                            Size size5 = new Size(Screen_draw_size, Screen_draw_size);//ner


                            for (int j = 0; j < 16; j++) {
                                for (int i = 0; i < 16; i++) {
                                    /**???????????????date????????????????????????RGB??????*/
                                    int RGB = rematrix[j][i];//& 0xff ;
                                    /**??????16*16?????????-1??????*/
                                    //Imgproc.rectangle(srcMat, new Point(0+i*24,0+j*24), new Point(0+(i+1)*24, 0+(j+1)*24), new Scalar((j*16+i+1),(j*16+i+1),(j*16+i+1),255), -1);
                                    //Imgproc.rectangle(srcMat, new Point(i * draw_X, j * draw_Y), new Point(((i + 1) * draw_X -1), ((j + 1) * draw_Y -1)), new Scalar(RGB, RGB, RGB, 255), -1);
                                    Imgproc.rectangle(srcMat, new Point( i*2 , j*2 ),new Point( ((i+1)*2-1) , ((j+1)*2-1) ), new Scalar(RGB, RGB, RGB, 255), -1);
                                }
                            }


                            /**????????????*/
                            if (spinner_interpolation == 0) {
                                Imgproc.resize(srcMat, srcMat3, size1,Imgproc.INTER_NEAREST);
                                //Imgproc.resize(srcMat2, srcMat, size5);
                                Imgproc.resize(srcMat3, srcMat, size5, Imgproc.INTER_LINEAR);
                                //Imgproc.resize(srcMat3, srcMat, size5, Imgproc.INTER_NEAREST);

                            } else {


                            }



                            /**????????????*/
                            /*if (i == 1) {
                                Utils.matToBitmap(srcMat, destbitmap);
                                destbitmap = sharpenImageAmeliorate(destbitmap);
                                Utils.bitmapToMat(destbitmap, srcMat);
                            }*/


                            /**????????????RGBA?????????RGB???????????????*/

                            //Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB);
                            /**??????*/

                           /* if (spinner_color == 0) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_INFERNO);
                            } else if (spinner_color == 1) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_JET);
                            } else if (spinner_color == 2) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_COOL);
                            } else if (spinner_color == 3) {
                                Imgproc.applyColorMap(srcMat, srcMat, Imgproc.COLORMAP_HOT);
                            }*/


                            /**apply???RGB?????????GBR???????????????RGB2BGR*/
                            //Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_BGR2RGBA);

                            /**?????????????????????*/
                            //Imgproc.rectangle(srcMat, new Point(0 + (high_place % 16) * draw_X, 0 + (high_place / 16) * draw_Y), new Point(0 + ((high_place % 16) + 1) * draw_X, 0 + ((high_place / 16) + 1) * draw_Y), new Scalar(255, 0, 0, 255), 2);


                            /**??????check*/
                            /*if (flip_count == 0) {
                                Core.flip(srcMat, srcMat, -1);
                            }*/
                            /**????????????????????????????????????????????????*/
                           /* if (RawX >= 0) {
                                Imgproc.rectangle(srcMat, new Point(0 + (RawX / draw_X) * draw_X, 0 + ((RawY) / draw_Y) * draw_Y), new Point(0 + ((RawX / draw_X) + 1) * draw_X, 0 + (((RawY) / draw_Y) + 1) * draw_Y), new Scalar(0, 255, 0, 255), 2);
                                float touch_temp = temperature1[(RawX / draw_X) + (RawY / draw_Y) * 16];

                                if (flip_count == 0) {
                                    touch_temp = temperature1[255 - ((RawX / draw_X) + (RawY / draw_Y) * 16)];
                                }

                                /**????????????????????????*/
                               /* tvStatus.setText(Float.toString(touch_temp));
                                tvStatus.setTextColor(Color.rgb(0, 255, 0));


                            }*/


                            /**???mat??????Bitmap??????*/
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

        /**??????180*/
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


        /**?????????????????????????????????*/
        detectUSB();


    }

    /**??????????????????*/
    public void getPermissioncamera() {
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        }
    }


    /**?????????button*/
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
        /**???????????????*/
        unregisterReceiver(status);
    }



    private class USBStatus extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                /**???Check??????????????????????????????*/
                case USB_PERMISSION:
                    if (drivers.size() == 0) return;
                    boolean hasPermission = manager.hasPermission(drivers.get(0).getDevice());
                    tvStatus.setText("??????: " + hasPermission);
                    if (!hasPermission) {
                        getPermission(drivers);
                        return;
                    }
                    Toast.makeText(context, "???????????????", Toast.LENGTH_SHORT).show();
                    break;
                /**??????USB????????????*/
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Toast.makeText(context, "USB????????????", Toast.LENGTH_SHORT).show();
                    detectUSB();
                    break;
                /**??????USB????????????*/
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    Toast.makeText(context, "USB????????????", Toast.LENGTH_SHORT).show();
                    //tvInfo.setText("TextView");
                    tvRes.setText("TextView");
                    tvStatus.setText("??????: false");
                    break;
            }
        }
    }
    /**????????????*/
    private void detectUSB() {
        manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager == null) return;
        if (manager.getDeviceList().size() == 0)return;
        tvStatus.setText("??????: false");
        /*??????????????????USB-OTG????????????*/
        drivers = getDeviceInfo();
        /*????????????????????????????????????OTG(??????)*/
        getPermission(drivers);
    }
    /**??????????????????USB-OTG??????????????????????????????"?????????"???????????????*/
    private List<UsbSerialDriver> getDeviceInfo() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Log.d(TAG, "??????????????????:\n " + deviceList);
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        ProbeTable customTable = new ProbeTable();
        List<UsbSerialDriver> drivers = null;
        String info = "";
        while (deviceIterator.hasNext()) {
            /**??????????????????*/
            UsbDevice device = deviceIterator.next();
            info = "Vendor ID: " + device.getVendorId()
                    + "\nProduct Id: " + device.getDeviceId()
                    + "\nManufacturerName: " + device.getManufacturerName()
                    + "\nProduceName: " + device.getProductName();
            /**????????????*/
            customTable.addProduct(
                    device.getVendorId(),
                    device.getProductId(),
                    CdcAcmSerialDriver.class
                    /**????????????Diver???CDC?????????
                     * CP21XX, CH34X, FTDI, Prolific ??????????????????*/
            );
            /**???????????????????????????*/
            UsbSerialProber prober = new UsbSerialProber(customTable);
            drivers = prober.findAllDrivers(manager);
        }
        /**??????UI*/
        //tvInfo.setText(info);
        return drivers;
    }
    /**??????OTG???????????????????????????????????????*/
    private void getPermission(List<UsbSerialDriver> drivers) {
        if (PendingIntent.getBroadcast(this, 0, new Intent(USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE) != null) {
            manager.requestPermission(drivers.get(0).getDevice(), PendingIntent.getBroadcast(
                    this, 0, new Intent(USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)); }
    }
    /**????????????*/
    private synchronized void send_start(List<UsbSerialDriver> drivers) {

        if (drivers == null) return;
        /**???????????????????????????*/
        UsbDeviceConnection connect = manager.openDevice(drivers.get(0).getDevice());
        /**?????????USB?????????PORT*/
        UsbSerialPort port = drivers.get(0).getPorts().get(0);
        try {
            /**??????port*/
            port.open(connect);
            /**??????????????????*/

            String s = "S"; //edInput.getText().toString();
            if (s.length() == 0) return;
            /**?????????????????????????????????????????????????????????*/
            port.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            /**????????????*/
            port.write(s.getBytes(), 50);

            /**?????????????????????*/
            SerialInputOutputManager.Listener serialInputOutputManager = getRespond;
            SerialInputOutputManager sL = new SerialInputOutputManager(port, serialInputOutputManager);
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(sL);
        } catch (IOException e) {
            try {
                /**??????Port?????????????????????????????????????????????????????????????????????*/
                port.close();
                send_start(drivers);
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "?????????????????????: " + ex);
            }
        }
    }

    private synchronized void sendValue(List<UsbSerialDriver> drivers) {

        if (drivers == null) return;
        /**???????????????????????????*/
        UsbDeviceConnection connect = manager.openDevice(drivers.get(0).getDevice());
        /**?????????USB?????????PORT*/
        UsbSerialPort port = drivers.get(0).getPorts().get(0);
        try {
            /**??????port*/
            port.open(connect);


            /**?????????????????????????????????????????????????????????*/
            port.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            /**????????????*/

                EditText edInput = findViewById(R.id.editText_Input);
                String test = edInput.getText().toString();
                int editnumber = Integer.valueOf(test).intValue();
                String str = new Character((char) editnumber).toString();
                port.write(str.getBytes(), 50);

            /**?????????????????????*/

        } catch (IOException e) {

            try {
                /**??????Port?????????????????????????????????????????????????????????????????????*/
                port.close();
                sendValue(drivers);
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.e(TAG, "?????????????????????: " + ex);
            }
        }
    }


    /**????????????*/
    private  SerialInputOutputManager.Listener getRespond = new SerialInputOutputManager.Listener() {
        @Override
        public synchronized void onNewData(byte[] data) {

            /**????????????????????????*/
            byte[][] singlematrix = splitBytes(data,1);
            float []temperature = new float [256];
            int[][] RGB_number = new int[256][1];
            float maxtemp = 0;

            for(int i = 0;i<256;i++){
                /**?????????512?????????????????????????????????*/
                int a = singlematrix[i*2][0] & 0xff;
                int b = singlematrix[i*2+1][0] & 0xff ;
                /**?????????????????????????????????????????????*/
                float count1 = (b*256+a);
                count1 = count1/100 -offset;
                temperature[i] = count1;

                /**????????????????????????*/
                    if(maxtemp<count1){
                        maxtemp = count1;
                        high_place = i;
                        res = Float.toString(maxtemp);

                    }


                /**???????????????RGB?????????????????????*/
                int result =  ((b*256+a)-datadown)/((dataup-datadown)/256);
                if(result>=255){
                    RGB_number[i][0] =255;
                }else if(result<=0){
                    RGB_number[i][0]  =0;
                }else{
                    RGB_number[i][0]  = result;
                }

            }



            /**?????????????????????????????????*/
            temperature1 = temperature;

            runOnUiThread(() -> {

                tvRes.setText(res);
                tvRes.setTextColor(Color.rgb(255,0, 0));
            });
            /**?????????16*16??????*/
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
    /**???ByteArray????????????????????????ASCII*/
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
    /**???ByteArray???????????????????????????*/
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


    /**?????????reshape???16*16*/
    private int[][] matrixReshape(int[][] nums, int r, int c) {
        if(nums == null){
            return null;
        }
        if(r == 0 || c == 0){
            return null;
        }
        int row = 0;//???
        int columns = 0;//???
        columns = nums[0].length;
        row = nums.length;
        /**??????????????????1*256?????????>260???null*/
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
                rr = index / c;//???
                cc = index % c;//???
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
        /**??????????????????*/
        int x = (int)event.getX();
        int y = (int)event.getY();
        /**??????????????????*/
         RawX = (int)event.getX();
         RawY = (int)event.getY();
        //tvStatus.setText(String.valueOf(RawX)+","+String.valueOf(RawY)+"\n");
    }




    private Bitmap sharpenImageAmeliorate(Bitmap bmp)
    {
        // ??????????????????
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
        //?????????????????????
        int[] pixels = new int[width*height];
        //?????????????????????????????????????????????
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



