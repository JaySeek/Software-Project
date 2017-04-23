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
    delay(300);
    BTSerial.println(calDistance());
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
    delay(500);
}

