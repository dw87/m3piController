m3piController
==============

A simple Android app to control an m3pi robot over WiFi.

Initially developed to control a specific robot (a Pololu m3pi) over WiFi, the control method and data being sent is generic, and not specific to the m3pi.  The data could be received and processed by any WiFi enabled device, and used to control other devices.  However this initial version targets the robot I have, and so has been named based on that.  

## Requirements

- A [Pololu m3pi Robot](http://www.pololu.com/product/2151) (a [Pololu 3pi robot](http://www.pololu.com/product/975) with the [m3pi Expansion board](http://www.pololu.com/product/2152) and an [mbed LPC1768 microcontroller](http://mbed.org/platforms/mbed-LPC1768/))
- An [RN-XV WiFly Xbee footprint WiFi module](https://www.sparkfun.com/products/10822)
- An Android device with WiFi connectivity
 
## Using the App
 
To use the app and control an m3pi robot, your Android device needs to be connected to a WiFi network.  This could be:
- Directly to a WiFly configured as an Access Point (AP Mode)
- To the same WiFi network (SSID) that the WiFly has been associated with

If there is no WiFi connection when you open the app, please make sure WiFi is enabled and you connect to a WiFi network.  The app will detect this, and enable the controls so you can start the controller.

Use the Settings menu from the Action Bar to configure the IP address of the m3pi or to use the Broadcast address.

Click the Connect button to establish a connection.

Click the Start button to start the controller transmitting data to the robot.  When this is running, drag the ball around the circle to send movement data to the robot.

The data transmitted is simple - it sends a message containing the X and Y values in the range -100 and 100 as displayed as the Robot X and Robot Y positions at the top of the screen.  The centre of the circle, where the ball will return if you are not touching the screen, has the value 0,0.

I am in the process of writing the mbed software to receive these values and suitably move the robot.  When this is complete, I will put the project here and update this README with a link.
 
## Known Bugs/Issues

- Display: On the Nexus 4 I have tested this app on, the numbers displayed at the top of the screen do overlap with the label text.  This is less of a problem on larger screens, but I will fix this eventually.

- Activity Lifecycle: If the app is left by any means (e.g. pressing the Home or Recent Applications buttons) it does not resume correctly.  On relaunching the app from the Recent Applications list, it will load the app, but the centre control area will stay blank and not responsd to any touch input when the app enters the 'Running' state.  The current solution is to close the app, swipe it away from the Recent Applications list to stop it completely, and relaunch it.
