## Android App for Health Tracking - final project in IOT course.

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


##### The app also tracks user's location in real time.
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
