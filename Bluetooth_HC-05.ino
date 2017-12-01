#include <SoftwareSerial.h>

SoftwareSerial BTSerial(10,11);

int trigPin = 13;
int echoPin = 12;

void setup() {
  Serial.begin(9600);
  BTSerial.begin(9600);
  pinMode(trigPin, OUTPUT);
  pinMode(echoPin, INPUT);
}

void loop() {
  int i = 0;
  int data = 0;
  
  for(i=0;i<10;i++) {
      data += calDistance();
      delay(100);
  }
  
  data /= 10;
  
  BTSerial.print(data);
  BTSerial.print("d");
}

long calDistance() {
  long duration, distance;
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  duration = pulseIn(echoPin, HIGH);
  distance = duration * 17 / 1000;
  return distance;
  delay(100);
}
