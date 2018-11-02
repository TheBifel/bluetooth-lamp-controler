
#include <Ultrasonic.h>
#include <SoftwareSerial.h>
#define DISTANCE_LIMIT 15 //CM
#define LAMP 2

Ultrasonic ultrasonic(9, 8); // 9 - Trig, 8 - Echo
SoftwareSerial mySerial(7, 8); // 7 - TX, 8 - RX

long timeToAlarm = 0;
int delayBetwenAlarmFlashes = 0;
int timeOfLAstAlarmFlash = 0;
bool turnOn = false;

void setup() {
  pinMode(LAMP, OUTPUT);
  Serial.begin(9600);
  mySerial.begin(9600);
  mySerial.setTimeout(10);
}


void loop() { 
    String data = mySerial.readStringUntil('\n');
    switch(data.charAt(0)) {
      case 'A':                // Alarm
        setAlarm(data);
        break;
      case 'S':                // State (Turn On/Off)
        changeLight();
        if (timeToAlarm != 0 && timeToAlarm < millis()) {
            timeToAlarm = 0;
        }
        break;   
      case 'D':                // Delay betwen alarm flashes
        setDelayBetwenAlarmFalashes(data);
        break;
      case 'O':                // is laight turned On
          isLightOn();
        break;
    }

    static long timOfLastAlarmFlash = 0;
    if (timeToAlarm != 0 && timeToAlarm < millis()) {
        if (delayBetwenAlarmFlashes == 0){
          setLight(true);
        } else {
            timeOfLAstAlarmFlash = alarmLight(timeOfLAstAlarmFlash); 
        }
    }
    
    int distance = ultrasonic.distanceRead();  
    Serial.println("Distance - " + String(distance));
    if (distance == 0){
        return;
    }
    
    static long ultrasonic_triggered = 0;  
    if (distance < DISTANCE_LIMIT) {
        if (ultrasonic_triggered == 0) {
          ultrasonic_triggered = millis();
        }
    } else {
        if (ultrasonic_triggered != 0 && (millis()) - ultrasonic_triggered > 500){
          changeLight();
          timeToAlarm = 0;
        }
        ultrasonic_triggered = 0;
    }
    
    delay(100);
}


void changeLight() {
  digitalWrite(LAMP, turnOn = !turnOn);
  isLightOn();
}

void setLight(bool state) {
  digitalWrite(LAMP, turnOn = state);
  isLightOn();
}

void setAlarm(String data) {
    timeToAlarm = millis() + data.substring(1, data.length()).toInt(); 

    mySerial.println("A" + String(timeToAlarm - millis()));
}

long alarmLight(long lastFlash) {
  if (millis() - lastFlash > delayBetwenAlarmFlashes){
    changeLight();
    return millis();      
  }
  return lastFlash;
}

void setDelayBetwenAlarmFalashes(String data) {
  delayBetwenAlarmFlashes = data.substring(1, data.length()).toInt() * 1000;
}

void isLightOn() {
  if (turnOn) {
      mySerial.println("S1");
  } else {
      mySerial.println("S0");
  }
}
