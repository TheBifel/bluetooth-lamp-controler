
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
  mySerial.setTimeout(99999999);
}


void loop() { 
  char input[6];
  mySerial.readBytes(input, 6);

  switch(input[0]) {
    case 'A':                // Alarm
      setAlarm(input);
      break;
    case 'S':                // State (Turn On/Off)
      changeLight();
      break;   
    case 'D':                // Delay betwen alarm flashes
      setDelayBetwenAlarmFalashes(input);
      break;
    case 'O':                // is laight turned On
      isLightOn();
      break;
  }

  static long timeOfLastAlarmFlash = 0;
  if (timeToAlarm != 0 && timeToAlarm < micros()) {
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
      ultrasonic_triggered = micros();
    }
  } else {
    if (ultrasonic_triggered != 0 && (micros()) - ultrasonic_triggered > 500){
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
}

void setAlarm(char* arr) {
  String tmpStr;
  for (byte i = 1; i <= 5; i++) tmpStr += arr[i];
  timeToAlarm = micros() + tmpStr.toInt() * 1000; 
}

long alarmLight(long lastFlash) {
  if (micros() - lastFlash > delayBetwenAlarmFlashes){
    changeLight();
    return micros();      
  }
  return lastFlash;
}

void isLightOn() {
  if (turnOn) {
      mySerial.write("S1\n");
  } else {
      mySerial.write("S0\n");
  }
}

void setDelayBetwenAlarmFalashes(char* arr) {
  String tmpStr;
  for (byte i = 1; i <= 5; i++) tmpStr += arr[i];
  delayBetwenAlarmFlashes = tmpStr.toInt() * 1000;
}
