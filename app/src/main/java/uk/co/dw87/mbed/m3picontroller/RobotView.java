package uk.co.dw87.mbed.m3picontroller;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class RobotView extends SurfaceView implements SurfaceHolder.Callback {
    private volatile RobotThread thread;

    //Handle communication from the RobotThread to the View/Activity Thread
    private Handler mHandler;

    //Pointers to the views
    private TextView mRobotXView;
    private TextView mRobotYView;
    private TextView mSpriteXView;
    private TextView mSpriteYView;
    private TextView mTouchXView;
    private TextView mTouchYView;
    private TextView mStatusView;

    public RobotView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //Get the holder of the screen and register interest
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        //Set up a handler for messages from RobotThread
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                if(m.getData().getBoolean("values")) {
                    mRobotXView.setText(m.getData().getString("robotX_value"));
                    mRobotYView.setText(m.getData().getString("robotY_value"));

                    mSpriteXView.setText(m.getData().getString("spriteX_value"));
                    mSpriteYView.setText(m.getData().getString("spriteY_value"));

                    mTouchXView.setText(m.getData().getString("touchX_value"));
                    mTouchYView.setText(m.getData().getString("touchY_value"));
                }
                else if (m.getData().getBoolean("robot")){
                    mRobotXView.setText(m.getData().getString("robotX_value"));
                    mRobotYView.setText(m.getData().getString("robotY_value"));
                }
                else if (m.getData().getBoolean("sprite")){
                    mSpriteXView.setText(m.getData().getString("spriteX_value"));
                    mSpriteYView.setText(m.getData().getString("spriteY_value"));
                }
                else if (m.getData().getBoolean("touch")){
                    mTouchXView.setText(m.getData().getString("touchX_value"));
                    mTouchYView.setText(m.getData().getString("touchY_value"));
                }
                else {
                    //So it is a status
                    mStatusView.setText(m.getData().getString("status_value"));
                }
            }
        };
    }

    //Used to release any resources.
    public void cleanup() {
        this.thread.setRunning(false);
        this.thread.cleanup();

        this.removeCallbacks(thread);
        thread = null;

        this.setOnTouchListener(null);

        SurfaceHolder holder = getHolder();
        holder.removeCallback(this);
    }

    /*
	 * Setters and Getters
	 */

    public void setThread(RobotThread newThread) {

        thread = newThread;

        setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if(thread != null) {
                    return thread.onTouch(event);
                }
                else return false;
            }
        });

        setClickable(true);
        setFocusable(true);
    }

    public RobotThread getThread() {
        return thread;
    }

    public TextView getRobotXView() {
        return mRobotXView;
    }

    public void setRobotXView(TextView mRobotXView) {
        this.mRobotXView = mRobotXView;
    }

    public TextView getRobotYView() {
        return mRobotYView;
    }

    public void setRobotYView(TextView mRobotYView) {
        this.mRobotYView = mRobotYView;
    }

    public TextView getSpriteXView() {
        return mSpriteXView;
    }

    public void setSpriteXView(TextView mSpriteXView) {
        this.mSpriteXView = mSpriteXView;
    }

    public TextView getSpriteYView() {
        return mSpriteYView;
    }

    public void setSpriteYView(TextView mSpriteYView) {
        this.mSpriteYView = mSpriteYView;
    }

    public TextView getTouchXView() {
        return mTouchXView;
    }

    public void setTouchXView(TextView mTouchXView) {
        this.mTouchXView = mTouchXView;
    }

    public TextView getTouchYView() {
        return mTouchYView;
    }

    public void setTouchYView(TextView mTouchYView) {
        this.mTouchYView = mTouchYView;
    }

    public TextView getStatusView() {
        return mStatusView;
    }

    public void setStatusView(TextView mStatusView) {
        this.mStatusView = mStatusView;
    }

    public Handler getmHandler() {
        return mHandler;
    }

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /*
	 * Screen functions
	 */

    //Ensure that we go into pause state if we go out of focus
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if(thread != null) {
            if (!hasWindowFocus) {
                thread.doPause();
            }
            if (hasWindowFocus) {
                thread.unpause();
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
       if(thread != null) {
            thread.setRunning(true);

            if(thread.getState() == Thread.State.NEW){
                //Just start the new thread
                thread.start();
            }
            else {
                if(thread.getState() == Thread.State.TERMINATED){
                    //Start a new thread
                    //Should be this to update screen with old game: new RobotThread(this, thread);
                    //The method should set all fields in new thread to the value of old thread's fields
                    thread = new RobotThread(this);
                    thread.setRunning(true);
                    thread.start();
                }
            }
       }
    }

    //Always called once after surfaceCreated. Tell the RobotThread the actual size
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(thread != null) {
            thread.setupSurface(width, height);
        }
    }

    /*
     * Need to stop the RobotThread if the surface is destroyed
     * Remember this doesn't need to happen when app is paused on even stopped.
     */
    public void surfaceDestroyed(SurfaceHolder arg0) {

        boolean retry = true;
        if(thread != null) {
            thread.setRunning(false);
        }

        //join the thread with this thread
        while (retry) {
            try {
                if(thread != null) {
                    thread.join();
                }
                retry = false;
            }
            catch (InterruptedException e) {
                //naughty, ought to do something...
            }
        }
    }
}