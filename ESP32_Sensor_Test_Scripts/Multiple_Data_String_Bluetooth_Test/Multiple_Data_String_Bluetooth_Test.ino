//This code sends two axes readings from the ICM-20601 gyro along with uptime in ms, through bluetooth serial as a string. 
//Use an app like Serial Bluetooth Terminal to observe data transmission: https://play.google.com/store/apps/details?id=de.kai_morich.serial_bluetooth_terminal
//By Russ Case - 2019

#include "BluetoothSerial.h"
#include <Wire.h>

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

void setup() {
  Serial.begin(19200);
  SerialBT.begin("Adobo Analytics ESP32"); //Bluetooth device name
  Serial.println("The device started, now you can pair it with bluetooth!");

  Wire.begin(); // join i2c bus as master
  Wire.setClock(400000);
  Serial.println("Open i2c bus");

  Wire.beginTransmission(0x68); // transmit to device #0x68
  Wire.write(byte(0x1B));      // sets register pointer to the gyro command register (27)
  Wire.write(byte(0x18));      // sets gyro to 4000 dps
  int error1 = Wire.endTransmission();      // stop transmitting


  Wire.beginTransmission(0x68); // transmit to device #0x68
  Wire.write(byte(0x6B));            // sets register pointer to the power management 1 register (107)
  Wire.write(byte(0x9));             // turn off sleep mode, disable temp sensor, set clk type (see pg 43 of datasheet)
  int error2 = Wire.endTransmission();      // stop transmitting


  Wire.beginTransmission(0x68); // transmit to device #0x68
  Wire.write(byte(0x6C));            // sets register pointer to the power management 2 register (108)
  Wire.write(byte(0x38));             // disable accelerometers, enable gyros (see pg. 44 of datasheet)
  int error3 = Wire.endTransmission();      // stop transmitting

  Serial.print(error1);
  Serial.print(error2);
  Serial.print(error3);
  Serial.println("\nSetup Complete");
}

short reading1 = 0;
short reading2 = 0;
double rpm = 0.0;

int time_0;
int time_now;
int i = 0;



void loop() {

  String msg = "";
  String r1 = "";
  String r2 = "";
  String t = "";

  // Record time in ms
  if (i <= 0) {
    time_0 = millis();
    i++;
  }

  //SerialBT.println('A');                    // Send signal indicating that ESP32 is ready to transmit data

  time_now = (millis() - time_0);             // record time of readings
  t = String(time_now, DEC);

  delay(250);                              // Delay for human readability. Take this out later
  
  //if (SerialBT.read() == 'B') {             // Detect phone signal indicating it is ready to receive data

    // Call X-Gyro 
    // (NOTE: replace the X-gyro call with an ADS1115 request, we want to send time, torsion and RPM. This is just to check functionality)
    Wire.beginTransmission(0x68);             // get slave's attention                    
    Wire.write(byte(0x43));                   // sets register pointer to echo X axis high byte 
    Wire.requestFrom(byte(0x68), 2, false);   // request 1 byte from slave device #0x68
    Wire.endTransmission();                   // stop transmitting

    if (2 <= Wire.available()) {              // if two bytes were received
      reading1 = Wire.read();                  // receive high byte (overwrites previous reading)
      reading1 = reading1 << 8;                 // shift high byte to be high 8 bits
      reading1 |= Wire.read();                 // receive low byte as lower 8 bits
      r1 = String(reading1);
    }

    // Call Y-Gyro
    Wire.beginTransmission(0x68);             // get slave's attention
    Wire.write(byte(0x45));                   // sets register pointer to echo Y axis high byte
    Wire.requestFrom(byte(0x68), 2, false);   // request 1 byte from slave device #0x68
    Wire.endTransmission();                   // stop transmitting

    if (2 <= Wire.available()) {              // if two bytes were received
      reading2 = Wire.read();                  // receive high byte (overwrites previous reading)
      reading2 = reading2 << 8;                 // shift high byte to be high 8 bits
      reading2 |= Wire.read();                 // receive low byte as lower 8 bits
      r2 = String(reading2);
    //}

    
    msg = t + "," + r1 + "," + r2;          // Assemble time, reading1, and reading 2 into a string to transmit

  SerialBT.println(msg);   // print the reading

  }
  // use this conversion factor if you want to convert to RPM before transmission. 
  // rpm = (double(reading))*4000/6/32768;
  
}
