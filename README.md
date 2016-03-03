# AndroidContactlessVitalSigns
(Sidenote: The code in this repo is over two years old and I'm sorry for how bad it's organized. It could be much more modular and better overall. Recently, I've just decided to go through and convert it to use Android Studio by default instead, and clean up just a little bit and change to more descriptive names. This repo is not active, but I'll still answer any questions about it.)

Using the Android camera, the app detects faces and starts to calculate heart rate, blood pressure, and body temperature. For more info: [http://danialgoodwin.github.io/android-app-contactless-vital-signs/](http://danialgoodwin.github.io/android-app-contactless-vital-signs/)



## The Code
The main code for heart rate and blood pressure is in `net/simplyadvanced/vitalsigns/bloodpressure/BloodPressureActivity.java` and `net/simplyadvanced/vitalsigns/CheckVitalSignsActivity.java`. The layout for those two can be found at `res/layout/activity_blood_pressure.xml` and `res/layout/activity_vital_signs.xml`.

The code for temperature can be `net/simplyadvanced/vitalsigns/bloodpressure/BloodPressureActivity.java` and `net/simplyadvanced/vitalsigns/bloodpressure/AddTemperatureActivity.java`. The layout for temperture can be found in `res/layout/activity_add_temperature.xml`

The other classes are mainly simulations for how they could be accomplished.

### Algorithms

To calculate blood pressure, there is [`BloodPressureActivity.setBloodPress(...)`](https://github.com/danialgoodwin/android-app-contactless-vital-signs/blob/master/app/src/main/java/net/simplyadvanced/vitalsigns/bloodpressure/BloodPressureActivity.java#L152). Unfortunately, some parts of it are hardcoded and can still be improved.

To calculate oxygen levels, there is [`OxygenSaturationActivity.calculateO2(...)`](https://github.com/danialgoodwin/android-app-contactless-vital-signs/blob/master/app/src/main/java/net/simplyadvanced/vitalsigns/oxygensaturation/OxygenSaturationActivity.java#L30).


## Test the APK
The APK to download to your Android phone can be found in the root of this directory: `app-debug.apk`.



## Background
This project was a team effort done by Danial Goodwin, James Coakley, Yi Zhuo, and Chris Mackey as part of our senior capstone project at the University of South Florida in 2013. In the beginning we knew little about Android development, Java, and health-related processes. In just a few months, we learned it all, completed our IRB certificates to do human testing, and delivered the final working project on-time.

I'm sorry for the lack of documentation on the code. There is still much more work to be done.



#### Disclaimer
This app comes as-is, and makes no guarantee to diagnose or cure any diseases or health problems.
