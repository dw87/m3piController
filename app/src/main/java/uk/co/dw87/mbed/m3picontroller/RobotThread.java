package uk.co.dw87.mbed.m3picontroller;

import uk.co.dw87.mbed.m3picontroller.model.Robot;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RobotThread extends Thread {
    //Different mMode states
    public static final int STATE_NOWIFI = 1;
    public static final int STATE_READY = 2;
    public static final int STATE_CONNECTED = 3;
    public static final int STATE_RUNNING = 4;
    public static final int STATE_PAUSED = 5;

    //Will store the instance of a robot
    private Robot mRobot;

    //private Drawable mCircle;
    private int mCircleRadius = 0;

    //Control variable for the mode of the game (e.g. STATE_READY)
    protected int mMode = STATE_NOWIFI;
    protected int prevMode = STATE_NOWIFI;

    //Control of the actual running inside run()
    private boolean mRun = false;

    //The surface this thread (and only this thread) writes upon
    private SurfaceHolder mSurfaceHolder;

    //the message handler to the View/Activity thread
    private Handler mHandler;

    //Android Context - this stores almost all we need to know
    private Context mContext;

    //The view
    public RobotView mRobotView;

    //We might want to extend this call - therefore protected
    protected int mCanvasWidth = 1;
    protected int mCanvasHeight = 1;

    //Transmit Rate (per second) - approximate
    protected int mTransmitRate = 2;
    //Last time we transmitted the robot packet
    protected long mLastTime = 0;
    protected long now = 0;

    //Network Connection and data packet
    protected DatagramSocket robotSocket;
    protected InetAddress robotAddr;
    protected int robotPort = 3636;
    protected DatagramPacket robotPacket;
    protected int msgLength = 0;
    protected String messageText;
    protected byte[] messageBytes;

    //Toast notification
    Toast toast;

    public RobotThread(RobotView robotView) {
        mRobotView = robotView;
        mSurfaceHolder = robotView.getHolder();
        mHandler = robotView.getmHandler();
        mContext = robotView.getContext();
    }

    /*
     * Called when app is destroyed, to clean up
     * Dare I say memory leak...
     */
    public void cleanup() {
        this.mContext = null;
        this.mRobotView = null;
        this.mHandler = null;
        this.mSurfaceHolder = null;
    }

    //This is run before a new game (also after an old game)
    public void setupDefaults() {
        //Set the initial position to the centre of the canvas
        mRobot.setX(mCanvasWidth / 2);
        mRobot.setY(mCanvasHeight / 2);

        mLastTime = System.currentTimeMillis()+(1000/mTransmitRate);

        resetDisplay();
    }

    public void resetDisplay() {
        if (mMode == STATE_NOWIFI || mMode == STATE_READY){
            setValues(0,0,0,0,0,0);
        }
        else {
            setValues((mRobot.getX()-mRobot.getX()),(mRobot.getY()-mRobot.getY()),mRobot.getX(),mRobot.getY(),0,0);
        }
    }

    /*
     * Network and data transmission
     */

    //Action on WiFi connection being made
    public void onWiFiConnect () {
        synchronized(mSurfaceHolder) {
            setState(STATE_READY);
            resetDisplay();
        }
    }

    //Action on WiFi connection being lost
    public void onWiFiDisconnect () {
        synchronized(mSurfaceHolder) {
            setState(STATE_NOWIFI);
            resetDisplay();
        }
    }

    //Connect the controller to the robot
    public boolean doConnect (String connectionString, Boolean isBroadcast) {
        synchronized(mSurfaceHolder) {
            try {
                if (robotSocket == null || !robotSocket.isBound()) {
                    robotSocket = new DatagramSocket(robotPort);
                    if (isBroadcast){
                        robotSocket.setBroadcast(true);
                        robotAddr = InetAddress.getByName("255.255.255.255");
                    } else {
                        robotAddr = InetAddress.getByName(connectionString);
                    }
                    setState(STATE_CONNECTED);
                    toast = Toast.makeText(mContext, "Connected.", Toast.LENGTH_SHORT);
                    toast.show();
                    return true;
                }
                else if (robotSocket != null || robotSocket.isBound()){
                    //Do nothing
                    return true;
                }
                else {
                    doDisconnect();
                    toast = Toast.makeText(mContext, "Unable to connect.", Toast.LENGTH_SHORT);
                    toast.show();
                    return false;
                }
            }
            catch (Exception e){
                doDisconnect();
                toast = Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT);
                toast.show();
                return false;
            }
        }
    }

    //Disconnect the controller from the robot (stop transmitting)
    public void doDisconnect() {
        synchronized (mSurfaceHolder) {
            if (mMode == STATE_CONNECTED || mMode == STATE_PAUSED){
                try {
                    robotSocket.disconnect();
                    robotSocket.close();
                    robotSocket = null;
                    robotAddr = null;
                    toast = Toast.makeText(mContext, "Disconnected.", Toast.LENGTH_SHORT);
                    toast.show();
                }
                catch (Exception e){
                    toast = Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT);
                    toast.show();
                }
                setState(STATE_READY);
                resetDisplay();
            }
        }
    }

    protected void sendPacket(){
        try {
            //Send data to Robot
            int x = mRobot.getVelX();
            int y = mRobot.getVelY();

            messageText = "$RP,X," + Integer.toString(x) + ",Y," + Integer.toString(y) + ";\r\n";
            messageBytes = messageText.getBytes();
            msgLength = messageText.length();
            robotPacket = new DatagramPacket(messageBytes, msgLength, robotAddr, robotPort);
            robotSocket.send(robotPacket);
            mLastTime = now;
        }
        catch (Exception e){
            //Failed to send
            //toast = Toast.makeText(mContext, e.toString(), Toast.LENGTH_SHORT);
            //toast.show();
        }
    }

    /*
     * Game states
     */

    //Start running the connected controller
    public void doStart() {
        synchronized(mSurfaceHolder) {
            setupDefaults();
            setState(STATE_RUNNING);
        }
    }

    public void doPause() {
        synchronized (mSurfaceHolder) {
            if (mMode == STATE_PAUSED){
                prevMode = STATE_PAUSED;
            }
            else if (mMode == STATE_RUNNING){
                prevMode = STATE_RUNNING;
                setState(STATE_PAUSED);
                resetDisplay();
            }
        }
    }

    public void unpause() {
        synchronized (mSurfaceHolder) {
            if (mMode == STATE_PAUSED && prevMode == STATE_RUNNING) {
                setState(STATE_RUNNING);
            }
        }
    }

    //Send messages to View/Activity thread
    public void setState(int state) {
        synchronized (mSurfaceHolder) {
            mMode = state;

            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();

            Resources res = mContext.getResources();
            CharSequence str = "";

            if (mMode == STATE_NOWIFI){
                str = res.getText(R.string.status_nowifi);
            } else {
                if (mMode == STATE_READY) {
                    str = res.getText(R.string.status_default);
                } else {
                    if (mMode == STATE_CONNECTED) {
                        str = res.getText(R.string.status_connected);
                    } else {
                        if (mMode == STATE_RUNNING) {
                            str = res.getText(R.string.status_running);
                        } else {
                            if (mMode == STATE_PAUSED) {
                                str = res.getText(R.string.status_paused);
                            }
                        }
                    }
                }
            }

            b.putString("status_value", str.toString());

            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    //The thread start
    @Override
    public void run() {
        Canvas canvasRun;
        while (mRun) {
            canvasRun = null;
            try {
                canvasRun = mSurfaceHolder.lockCanvas(null);
                synchronized (mSurfaceHolder) {
                    doDraw(canvasRun);
                    now = System.currentTimeMillis();
                    if (mMode == STATE_RUNNING){
                        if ((now - mLastTime) >= (1000/mTransmitRate)){
                            sendPacket();
                        }
                    }
                }
            }
            finally {
                if (canvasRun != null) {
                    if(mSurfaceHolder != null)
                        mSurfaceHolder.unlockCanvasAndPost(canvasRun);
                }
            }
        }
    }

    /*
     * Surfaces and drawing
     */

    public void setupSurface(int width, int height) {
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;

            //Create a circle radius based on the shorter dimension of the canvas area
            if (mCanvasWidth > mCanvasHeight){
                mCircleRadius = (int)((mCanvasHeight / 2) * 0.95);
            } else {
                mCircleRadius = (int)((mCanvasWidth / 2) * 0.95);
            }

            //Prepare the image so we can draw it on the screen (using a canvas)
            mRobot = new Robot(BitmapFactory.decodeResource(mRobotView.getContext().getResources(),
                    R.drawable.ball), mCanvasWidth / 2 ,mCanvasHeight / 2);

            mRobot.setRadius(mCircleRadius);
        }
    }

    protected void doDraw(Canvas canvas) {
        if(canvas == null) return;

        //draw the image of the ball using the X and Y of the ball
        //drawBitmap uses top left corner as reference, we use middle of picture
        //null means that we will use the image without any extra features (called Paint)

        //Draw/redraw the canvas background  to black - otherwise the movement of the
        //ball image persists and creates a trail across the screen
        canvas.drawColor(Color.BLACK);

        //If the controller is running or paused, draw/redraw the circle in the centre of
        //the canvas, and draw/redraw the ball on the canvas.
        if (mMode == STATE_RUNNING || mMode==STATE_PAUSED){
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);

            canvas.drawCircle(mCanvasWidth/2, mCanvasHeight/2, mCircleRadius, paint);
            mRobot.draw(canvas);

        }
    }

    //Finger touches the screen
    public boolean onTouch(MotionEvent e) {
        if (mMode == STATE_RUNNING){
            if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL){
                setupDefaults();
            }
            else if(e.getAction() != MotionEvent.ACTION_DOWN && e.getAction() != MotionEvent.ACTION_MOVE) {
                return false;
            }
            else {
                synchronized (mSurfaceHolder) {
                    this.actionOnTouch(e);
                }
            }
        }
        return true;
    }

    protected void actionOnTouch(MotionEvent e) {
        this.setTouch((int) e.getRawX(), (int) e.getRawY());
        this.setSprite((int) e.getX(), (int) e.getY());

        //Set the coordinates of the robot (it's top left pixel) to the touch position
        //coordinates relative to the canvas (0,0 is top left of the canvas, not the screen).
        mRobot.setX((int)e.getX());
        mRobot.setY((int)e.getY());

        //Update the Robot using the new Touch Position coordinates
        mRobot.Update();

        //If the robot's centre is outside the circle, constrain it to a position on the circle's
        //circumference at the same angle out from the centre of the circle
        if (mRobot.getCenterDistance() >= mCircleRadius)
        {
            mRobot.setX((int) (mRobot.getxCentre() + (Math.cos(mRobot.getCenterAngle()) * mCircleRadius)));
            mRobot.setY((int) (mRobot.getyCentre() + (Math.sin(mRobot.getCenterAngle()) * mCircleRadius)));
            this.setSprite(mRobot.getX(), mRobot.getY());
        }

        //Display the Robot position values using the Canvas centre as the origin
        //this.setRobot(mRobot.getX() - mRobot.getxCentre(), mRobot.getyCentre() - mRobot.getY());
        this.setRobot(mRobot.getVelX(), mRobot.getVelY());
    }

    /*
     * Getters and setters
     */
    public void setSurfaceHolder(SurfaceHolder h) {
        mSurfaceHolder = h;
    }
    
    public SurfaceHolder getSurfaceHolder(){
        return mSurfaceHolder;
    }

    public boolean getRunning() {
        return mRun;
    }

    public void setRunning(boolean running) {
        this.mRun = running;
    }

    public int getMode() {
        return mMode;
    }

    public void setMode(int mMode) {
        this.mMode = mMode;
    }

    /*
     * ALL ABOUT NUMBERS
     */

    //Send numbers to the View to view
    public void setValues(int robotX, int robotY, int spriteX, int spriteY, int touchX, int touchY) {
        synchronized (mSurfaceHolder) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean("values", true);
            b.putString("robotX_value", Integer.toString(robotX));
            b.putString("robotY_value", Integer.toString(robotY));
            b.putString("spriteX_value", Integer.toString(spriteX));
            b.putString("spriteY_value", Integer.toString(spriteY));
            b.putString("touchX_value", Integer.toString(touchX));
            b.putString("touchY_value", Integer.toString(touchY));
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    public void setRobot(int robotX, int robotY) {
        synchronized (mSurfaceHolder) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean("robot", true);
            b.putString("robotX_value", Integer.toString(robotX));
            b.putString("robotY_value", Integer.toString(robotY));
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    public void setSprite(int spriteX, int spriteY) {
        synchronized (mSurfaceHolder) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean("sprite", true);
            b.putString("spriteX_value", Integer.toString(spriteX));
            b.putString("spriteY_value", Integer.toString(spriteY));
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }

    public void setTouch(int touchX, int touchY) {
        synchronized (mSurfaceHolder) {
            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();
            b.putBoolean("touch", true);
            b.putString("touchX_value", Integer.toString(touchX));
            b.putString("touchY_value", Integer.toString(touchY));
            msg.setData(b);
            mHandler.sendMessage(msg);
        }
    }
}