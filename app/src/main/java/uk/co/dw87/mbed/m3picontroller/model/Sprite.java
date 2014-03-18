package uk.co.dw87.mbed.m3picontroller.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class Sprite {
    private Bitmap bitmap;	// The actual bitmap
    private int x;			// The current/drawn X coordinate
    private int y;			// The current/drawn Y coordinate
    private int xOffset;    // The offset from the image X (top left) to the centre X
    private int yOffset;    // The offset from the image Y (top left) to the centre Y
    //Store the permanent/default centre of the Robot
    private int xDefault;
    private int yDefault;

    public Sprite(Bitmap bitmap, int x, int y) {
        this.bitmap = bitmap;
        //Set the initial position to draw the image centred on x,y
        this.x = x;
        this.y = y;
        //Calculate the offset to the centre of the bitmap image
        this.xOffset = (bitmap.getWidth() / 2);
        this.yOffset = (bitmap.getHeight() / 2);
        //Set the permanent centre of the Robot to the coordinates given when it is created.
        this.xDefault = x;
        this.yDefault = y;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }
    public int getXOffset() {
        return xOffset;
    }
    public int getYOffset() {
        return yOffset;
    }

    public int getxCentre() {
        return xDefault;
    }

    public int getyCentre() {
        return yDefault;
    }

    public void draw(Canvas canvas) {
        //Draw the image, centred on the current x,y coordinates using the Offset values
        canvas.drawBitmap(bitmap, x-xOffset, y-yOffset, null);
    }
}