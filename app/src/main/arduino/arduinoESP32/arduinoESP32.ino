#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_LSM303_U.h>
#include "MAX30105.h"
#include "heartRate.h"
#include "BluetoothSerial.h"

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif




MAX30105 particleSensor;
const int RATE_SIZE = 32; //Increase this for more averaging. 4 is good.
float rates[RATE_SIZE] = {0}; //Array of heart rates
int rateSpot = 0;
long lastBeat = 0; //Time at which the last beat occurred
int numBeats = 0;
float beatsPerMinute;
float beatAvg;

BluetoothSerial SerialBT;

/* Assign a unique ID to this sensor at the same time */
Adafruit_LSM303_Accel_Unified accel = Adafruit_LSM303_Accel_Unified(54321);


void setup(void)
{
#ifndef ESP8266
  while (!Serial);     // will pause Zero, Leonardo, etc until serial console opens
#endif
  Serial.begin(9600);
  SerialBT.begin("A-A-Y Device"); //Bluetooth device name

  /* Initialise the sensor */
  if(!accel.begin())
  {
    /* There was a problem detecting the ADXL345 ... check your connections */
    Serial.println("Ooops, no LSM303 detected ... Check your wiring!");
    while(1);
  }
  while (!particleSensor.begin()) {
    Serial.println("MAX30102 was not found");
    delay(1000);
  }
  
  byte ledBrightness = 70; //Options: 0=Off to 255=50mA
  byte sampleAverage = 4; //Options: 1, 2, 4, 8, 16, 32
  byte ledMode = 2; //Options: 1 = Red only, 2 = Red + IR, 3 = Red + IR + Green
  int sampleRate = 400; //Options: 50, 100, 200, 400, 800, 1000, 1600, 3200
  int pulseWidth = 118; //Options: 69, 118, 215, 411
  int adcRange = 16384; //Options: 2048, 4096, 8192, 16384

  particleSensor.setup(ledBrightness, sampleAverage, ledMode, sampleRate, pulseWidth, adcRange); //Configure sensor with these settings



}


void loop(void)
{

  unsigned long myTime = millis();
  sensors_event_t event;
  accel.getEvent(&event);
  long irValue = particleSensor.getIR();
  
//   if (irValue > 50000){

//    if (checkForBeat(irValue) == true)
//    {
//      numBeats++;
//      Serial.println("We sensed a beat!");
//      //We sensed a beat!
//      long delta = millis() - lastBeat;
//      lastBeat = millis();
//      
//      beatsPerMinute = 60 / (delta / 1000.0);
//  
//      if (beatsPerMinute < 255 && beatsPerMinute > 20)
//      {
//        rates[rateSpot++] = beatsPerMinute; //Store this reading in the array
//        rateSpot %= RATE_SIZE; //Wrap variable
//  
//        //Take average of readings
//        beatAvg = 0;
//        for (int x = 0 ; x < RATE_SIZE ; x++)
//          beatAvg += rates[x];
//        beatAvg /= RATE_SIZE;
//      }
//    }
//   }

  SerialBT.println((String)" " + event.acceleration.x + "," + event.acceleration.y + "," + event.acceleration.z + ","+myTime+","+particleSensor.getFIFOIR()+","+particleSensor.getFIFORed());
//  Serial.print("X: "); Serial.print(event.acceleration.x); Serial.print("  ");
//  Serial.print("Y: "); Serial.print(event.acceleration.y); Serial.print("  ");
//  Serial.print("Z: "); Serial.print(event.acceleration.z); Serial.print("  ");Serial.println("m/s^2 ");
//  Serial.print("IR: "); Serial.print(particleSensor.getIR()); Serial.print("  ");
//  Serial.print("BPM: "); Serial.print(beatsPerMinute); Serial.print("  ");
//  Serial.print("BEATS#: "); Serial.print(numBeats); Serial.print("  ");
//  Serial.print("BPM AVG: "); Serial.print(beatAvg); Serial.print("  ");
  // read next set of samples
//  particleSensor.nextSample(); 
  /* Delay before the next sample */
  delay(33.3);
//  }
}
