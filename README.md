m3piController
==============

A simple Android app to control an m3pi robot over WiFi.

Initially developed to control a specific robot (a Pololu m3pi) over WiFi, the control method and data being sent is generic, and not specific to the m3pi.  The data could be received by any WiFi enabled device and, if it has been programmed to process the received data, used to control it.  However this initial version targets the robot I have, which happens to be a Pololu m3pi, and so has been named based on that.  

## Requirements

- A [Pololu m3pi Robot](http://www.pololu.com/product/2151) (a [Pololu 3pi robot](http://www.pololu.com/product/975) with the [m3pi Expansion board](http://www.pololu.com/product/2152) and an [mbed LPC1768 microcontroller](http://mbed.org/platforms/mbed-LPC1768/))
- An [RN-XV WiFly] (https://www.sparkfun.com/products/10822) Xbee footprint WiFi module
- An Android device with WiFi connectivity

## Hardware Configuration

This README will not include details on how to program the mbed on the m3pi, or how to configure the RN-XV WiFly module.  When I have completed the m3pi software, I will update this README to link there, where I will cover how to get it working with an m3pi.  

## Using the App
 
To use the app and control an m3pi robot, your Android device needs to be connected to a WiFi network.  
This could be:
- Directly to a WiFly configured as an Access Point (AP Mode)
- To the same WiFi network (SSID) that the WiFly has been associated with (e.g. your home WiFi network)

If there is no WiFi connection when you open the app, you will not be able to start the controller.  Please make sure WiFi is enabled, and it has established a connection.  The app monitors for changes to WiFi connectivity, and switches modes accordingly.  

If it establishes a connection, you will then be able to start the controller.  If it loses a connnection, or WiFi is disabled, it will stop the controller and wait for a WiFi connection to be remade.  

Before you Connect to the m3pi, use the Settings menu from the Action Bar to configure the IP address of the m3pi or to use the Broadcast address.  By default, the Broadcast address is used which will work with any WiFi connection.  

Click the Connect button to establish a connection.  This creates the appropriate DatagramSocket ready to transmit data to the m3pi.  

Click the Start button to start the controller transmitting data to the robot.  When this is running, drag the ball around the circle to send movement data to the robot.

The data transmitted is simple - it sends a message containing the X and Y values in the range -100 and 100 to the m3pi.  These values are displayed as the Robot X and Robot Y positions at the top of the screen.  The centre of the circle, where the ball will return if you are not touching the screen, has the value 0,0.

The Touch X/Y values give the position of the touch in screen coordinates, and Sprite X/Y give the position of the centre of the ball relative to the SurfaceView.  

I am in the process of writing the mbed software to receive these values and suitably move the robot.  When this is complete, I will put the project here and update this README with a link.
 
## Known Bugs/Issues

- Display: On my Nexus 4 the numbers displayed at the top of the screen do overlap with the label text.  This is likely to happen on other phones/small screen devices.  This is not a problem on larger screens, but I will fix this eventually.

- Activity Lifecycle: If the app is left (e.g. pressing the Home or Recent Applications buttons) it does not resume correctly.  On relaunching the app from the Recent Applications list, it will load, but the SurfaceView control area will stay blank and not respond to touch input.  The app will still enter the 'Running' state, and behave normally except for this issue.  The current solution is to close the app, swipe it away from the Recent Applications list to stop it completely, and relaunch it.
