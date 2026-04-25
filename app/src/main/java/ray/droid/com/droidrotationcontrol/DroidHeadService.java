package ray.droid.com.droidrotationcontrol;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class DroidHeadService extends Service {
    private WindowManager windowManager;
    private ImageView chatHead;
    //private TextView txtHead;

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private Context context;
    private View.OnTouchListener onTouchListener;
    private boolean chatHeadAdded;
    public static boolean killService = false;

    public enum EnumStateButton {
        CLOSE,
        VIEW
    }

    private EnumStateButton StateButton;

    private WindowManager.LayoutParams params;

    @Override
    public IBinder onBind(Intent intent) {
        // Not used
        Log.d(DroidCommon.TAG, "DroidHeadService - onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getBaseContext();
        if (!canDrawOverlays()) {
            Log.d(DroidCommon.TAG, "DroidHeadService - missing overlay permission");
            killService = true;
            stopSelf();
            return;
        }
        InicializarVariavel();
        InicializarAcao();
        AtualizarPosicao();
        GravaStatusRotacao();
        Log.d(DroidCommon.TAG, "DroidHeadService - onCreate");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //call widget update methods/services/broadcasts
        Log.d(DroidCommon.TAG, "onTouch - Neworientation: " + newConfig.orientation);
        //GravarPosicaoAtual();
        AtualizarPosicao();
    }

    private void Vibrar(int valor) {
        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(valor);
        } catch (Exception ex) {
            Log.d(DroidCommon.TAG, "Vibrar: " + ex.getMessage());
        }
    }

    private void GravaStatusRotacao()
    {
        DroidPreferences.SetInteger(context, "statusRotacao", Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0));
    }

    private int LerStatusRotacao()
    {
        int status = 0;
        try {
            status = DroidPreferences.GetInteger(context, "statusRotacao");
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, status);
        }
        catch (Exception ex)
        {
            Log.d(DroidCommon.TAG, "LerStatusRotacao: " + ex.getMessage());
        }
        return status;
    }

    private void AtualizarPosicao() {
        try {
            if (DroidPreferences.GetInteger(context, "orientationActual") == 2 || getResources().getConfiguration().orientation == 2) {
                params.x = DroidPreferences.GetInteger(context, "params.y");
                params.y = DroidPreferences.GetInteger(context, "params.x");
            } else {
                params.x = DroidPreferences.GetInteger(context, "params.x");
                params.y = DroidPreferences.GetInteger(context, "params.y");
            }

            windowManager.updateViewLayout(chatHead, params);
            //windowManager.updateViewLayout(txtHead, params);

        } catch (Exception ex) {
            Log.d(DroidCommon.TAG, "InicializarVariavel: " + ex.getMessage());
        }

        Log.d(DroidCommon.TAG, "onTouch - x: " + DroidPreferences.GetInteger(context, "params.x"));
        Log.d(DroidCommon.TAG, "onTouch - y: " + DroidPreferences.GetInteger(context, "params.y"));
    }

    private void GravarPosicaoAtual() {
        try {
            DroidPreferences.SetInteger(context, "params.x", params.x);
            DroidPreferences.SetInteger(context, "params.y", params.y);
            DroidPreferences.SetInteger(context, "orientationActual", getResources().getConfiguration().orientation);
        } catch (Exception ex) {
        }
    }

    private void InicializarVariavel() {
        context = getBaseContext();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        onTouchListener = new TouchListener();

        chatHead = new ImageView(context);
        chatHead.setImageResource(R.mipmap.stoprec);
        //txtHead = new TextView(context);
        //txtHead.setTextSize(20);
        //txtHead.setText("100");

        StateButton = EnumStateButton.VIEW;
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        windowManager.addView(chatHead, params);
        chatHeadAdded = true;
        //windowManager.addView(txtHead, params);

    }

    private int getOverlayWindowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private boolean canDrawOverlays() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void InicializarAcao() {
        //txtHead.setOnTouchListener(onTouchListener);

        try {
            DroidPreferences.SetInteger(context, "show", 1);
            chatHead.setOnTouchListener(onTouchListener);
        } catch (Exception ex) {
        }
    }


    public class TouchListener implements View.OnTouchListener {

        private GestureDetector gestureDetector = new GestureDetector(DroidHeadService.this, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (StateButton == EnumStateButton.VIEW) {
                    chatHead.setImageResource(R.mipmap.closerec);
                    StateButton = EnumStateButton.CLOSE;
                } else {
                    chatHead.setImageResource(R.mipmap.stoprec);
                    StateButton = EnumStateButton.VIEW;
                }
                return super.onDoubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (StateButton == EnumStateButton.VIEW) {
                    rotateScreen();
                } else {
                    Vibrar(100);

                    try {
                        killService = true;
                        stopSelf();
                    } catch (Exception ex) {
                        Log.d(DroidCommon.TAG, "stopSelf: " + ex.getMessage());
                    }


                }
                return super.onSingleTapConfirmed(e);
            }
        });

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            gestureDetector.onTouchEvent(event);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    Integer totalMoveX = (int) (event.getRawX() - initialTouchX);
                    params.x = initialX + totalMoveX;
                    Integer totalMoveY = (int) (event.getRawY() - initialTouchY);
                    params.y = initialY + totalMoveY;
                    windowManager.updateViewLayout(chatHead, params);
                    //  windowManager.updateViewLayout(txtHead, params);
                    GravarPosicaoAtual();
                    return true;
            }

            return true;
        }

    }

    private void rotateScreen() {
        if (!canWriteSettings()) {
            Toast.makeText(context, R.string.permission_required, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(context, DroidMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        try {
            int currentRotation = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.USER_ROTATION,
                    Surface.ROTATION_0);
            int nextRotation = getNextRotation(currentRotation);

            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION,
                    0);
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.USER_ROTATION,
                    nextRotation);

            Log.d(DroidCommon.TAG, "rotateScreen - current: " + currentRotation + ", next: " + nextRotation);
        } catch (Exception ex) {
            Log.d(DroidCommon.TAG, "rotateScreen: " + ex.getMessage());
            Toast.makeText(context, R.string.rotation_failed, Toast.LENGTH_LONG).show();
        }
    }

    private int getNextRotation(int currentRotation) {
        switch (currentRotation) {
            case Surface.ROTATION_0:
                return Surface.ROTATION_90;
            case Surface.ROTATION_90:
                return Surface.ROTATION_180;
            case Surface.ROTATION_180:
                return Surface.ROTATION_270;
            case Surface.ROTATION_270:
            default:
                return Surface.ROTATION_0;
        }
    }

    private boolean canWriteSettings() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LerStatusRotacao();

        if (chatHeadAdded && chatHead != null) {
            windowManager.removeView(chatHead);
            chatHeadAdded = false;
        }
        //if (txtHead != null) windowManager.removeView(txtHead);

        if (!killService) {
            Intent broadcastIntent = new Intent("com.droid.ray.droidturnoff.ACTION_RESTART_SERVICE");
            sendBroadcast(broadcastIntent);
            Log.d(DroidCommon.TAG, "DroidHeadService - onDestroy");
        }
        if (context != null) {
            DroidPreferences.SetInteger(context, "show", 0);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onUnbind");
        return super.onUnbind(intent);

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(DroidCommon.TAG, "DroidHeadService - onTaskRemoved");

        if (!killService) {
            Intent broadcastIntent = new Intent("com.droid.ray.droidturnoff.ACTION_RESTART_SERVICE");
            sendBroadcast(broadcastIntent);
            Log.d(DroidCommon.TAG, "DroidHeadService - onDestroy");
        }
    }

    @Override
    public void onTrimMemory(int level) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onTrimMemory");
        super.onTrimMemory(level);
    }

    @Override
    public void onLowMemory() {
        Log.d(DroidCommon.TAG, "DroidHeadService - onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(DroidCommon.TAG, "DroidHeadService - onRebind");
        super.onRebind(intent);
    }


}





