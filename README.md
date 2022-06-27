## Android App for Health Tracking - final project in IOT course.

![app video](https://user-images.githubusercontent.com/52024657/175918339-203cc0a3-1b54-4253-a988-05a8260a87b8.mp4)

##### The project includes receiving, processing and presenting data from the following sensors:
    1. IMU 9-DOF -used to count user's steps in real time.
    2. MAX301002 -used to measure and display user's BPM and %SPO2 in real time.

##### Ardunino code for ESP32 can be found here:
    app
        ↳ src
            ↳ main
                ↳ arduino
                    ↳ arduinoESP32
                        ↳ arduinoESP32.ino



##### Track User's Location in Real Time.

Location tracker is created using Google APIs.


    1. Create new firebase project
    2. Download google-services.json for your app and replace current file.
            app
                ↳ google-services.json
    3. Create new API key in Google Cloud Platform and paste in in:
            app
                ↳ src
                    ↳ main
                        ↳ res
                            ↳ values
                                ↳ strings.xml
                                
    4. Restrict the API key to following selected APIs:
      - Selected APIs:
      - Directions API
      - Distance Matrix API
      - Firebase Installations API
      - Geocoding API
      - Geolocation API
      - Maps JavaScript API
      - Maps SDK for Android
      - Places API
      
![location tracking](https://user-images.githubusercontent.com/52024657/175919544-1ecd76a8-9d4e-4c93-9c2c-b581758e8436.png)



##### Prediction of Activity Type (walking\running) and Number of Steps:
    To run prediction algorithms, view calcStepsAlgoJupyter.ipynb file.
    Sample files can be found in "data" folder  

            app
                ↳ src
                    ↳ main
                        ↳ python
                            ↳ calcStepsAlgoJupyter.ipynb
                            ↳ data
 
   
