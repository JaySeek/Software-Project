package com.example.wjdtl.eyesafer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;

public class BlueLightService extends Service {
    private View mView;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    @Override
    public void onCreate()
    {
        mView = new MyView(this);

        mParams = new WindowManager.LayoutParams( // 최상위에 떠있는 뷰를 만드는 부분
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY, // 최상위에 있도록 해주는 타입
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // 윈도우가 포커스 받지 못함
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // 뷰가 밀려서 밖으로 나가지 않게 함
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, // 윈도우 영역에 상태바 포함
                PixelFormat.TRANSLUCENT);

        mWindowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mView, mParams);

        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent )
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        ((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(mView);
        mView = null;
    }

    public class MyView extends View
    {
        public MyView(Context context)
        {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            canvas.drawARGB(50, 255, 212, 0); // Alpha, Red, Green, Blue
        }

        @Override
        protected void onAttachedToWindow()
        {
            super.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow()
        {
            super.onDetachedFromWindow();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

}
