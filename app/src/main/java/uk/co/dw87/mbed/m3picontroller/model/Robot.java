package uk.co.dw87.mbed.m3picontroller.model;

import android.graphics.Bitmap;

public class Robot extends Sprite {
    //The distance and angle of the robot center from the canvas center
    protected double centerDistance;
    protected double centerAngle;
    protected int movementRadius;

    protected int velX;
    protected int velY;

    public Robot(Bitmap bitmap, int x, int y) {
        super(bitmap, x, y);
    }

    public void Update() {
        //Recalculate the distance and angle from the centre of the canvas/circle to the centre
        //pixel of the Robot image.
        this.centerDistance = Math.sqrt((Math.pow((this.getX() - this.getxCentre()), 2)) + (Math.pow((this.getY() - this.getyCentre()), 2)));
        this.centerAngle = Math.atan2((this.getY() - this.getyCentre()), (this.getX() - this.getxCentre()));
    }

    public double getCenterDistance() {
        return centerDistance;
    }

    public double getCenterAngle() {
        return centerAngle;
    }

    public void setRadius(int radius){
        this.movementRadius = radius;
    }

    public int getVelX() {
        this.velX = Math.round(((float)(this.getX() - this.getxCentre())/(float)this.movementRadius)*100);
        return velX;
    }

    public int getVelY() {
        this.velY = Math.round(((float)(this.getyCentre() - this.getY())/(float)this.movementRadius)*100);
        return velY;
    }
}