//This code is used to validate the transmission of a single data point in string form via Bluetooth serial. 
//Use an app like Serial Bluetooth Terminal to observe data transmission: https://play.google.com/store/apps/details?id=de.kai_morich.serial_bluetooth_terminal
//By Russ Case, 2019


#include "BluetoothSerial.h"
#include <Wire.h>

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

BluetoothSerial SerialBT;

void setup() {
  Serial.begin(19200);
  SerialBT.begin("BCIT Racing Gyro Test"); //Bluetooth device name
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
  delay(1000); 

}

short reading = 0;
double rpm = 0.0;

void loop() {
  
  Wire.beginTransmission(0x68);             // get slave's attention
  Wire.write(byte(0x45));                   // sets register pointer to echo Xout_h
  Wire.requestFrom(byte(0x68), 2, false);                // request 1 byte from slave device #0x68
  int error4 = Wire.endTransmission();      // stop transmitting

  if (2 <= Wire.available()) { // if two bytes were received
    reading = Wire.read();  // receive high byte (overwrites previous reading)
    reading = reading << 8;    // shift high byte to be high 8 bits
    reading |= Wire.read(); // receive low byte as lower 8 bits
    //rpm = (double(reading))*0.02034505;
    rpm = (double(reading))*4000/6/32768;
    SerialBT.println(rpm);   // print the reading
  }
  
}
