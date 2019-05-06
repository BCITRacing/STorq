// This code is used to validate the serial output of a strain gauge reading sent through an ADS1115 ADC. 
// By Jeremiah Moreno - 2019

#include <Wire.h>
#include <Adafruit_ADS1015.h>

Adafruit_ADS1115 ads;  /* Use this for the 16-bit version */
//Adafruit_ADS1015 ads;     /* Use thi for the 12-bit version */

float result_sum = 0.0;

void setup(void)
{
  Serial.begin(250000);
  //ads1115.begin();  // Initialize ads1115
  ads.begin();  // Initialize ads1115

  //                                                                ADS1015  ADS1115
  //                                                                -------  -------
  //ads.setGain(GAIN_TWOTHIRDS);  // 2/3x gain +/- 6.144V  1 bit = 3mV      0.1875mV (default)
   ads.setGain(GAIN_ONE);        // 1x gain   +/- 4.096V  1 bit = 2mV      0.125mV
  // ads.setGain(GAIN_TWO);        // 2x gain   +/- 2.048V  1 bit = 1mV      0.0625mV
  // ads.setGain(GAIN_FOUR);       // 4x gain   +/- 1.024V  1 bit = 0.5mV    0.03125mV
  // ads.setGain(GAIN_EIGHT);      // 8x gain   +/- 0.512V  1 bit = 0.25mV   0.015625mV
  // ads.setGain(GAIN_SIXTEEN);    // 16x gain  +/- 0.256V  1 bit = 0.125mV  0.0078125mV
}

void loop(void)
{
  //Define those variables yo
  int16_t results, adc0;
  double volt;
  double torque;
  double Vi = 3.3;
  double D = 1.0;
  double pie = 3.141592654;
  double E = 27.5E6; //Somewhere between 27E6-31E6
  double nu = 0.31;
  double zerovalue = 2.0544373989;
  double ampGain = 1000;
  double SG = 1.5; //0.13+-0.2
  double ADCconversion = 0.000125;// This value is found in the above table depending on the chosen gain

 
  //Get the Voltage difference from the IN-AMP
  results = ads.readADC_Differential_0_1();
  volt = (double)results * ADCconversion;
  volt = volt - zerovalue; //The zero no load voltage of shaft
  volt = volt / ampGain;
  torque = volt * pie * pow(D, 3) / SG / 8.0 * (E / (1.0 + nu)) / Vi / 12.0; //the divide 12 is a foot
  Serial.println(torque, 2);

  //Code for zeroing and debugging
  /* 
    //Get the Voltage difference from the IN-AMP
    results = ads.readADC_Differential_0_1();
    volt = (double)results * 0.0001875;
    Serial.print("voltage:");
    Serial.print(volt, 10);
    Serial.print("\n");
    //zero the reading
    volt = volt - zerovalue; //The zero no load voltage of shaft
    //remove the IN AMP Gain
    volt = volt/ampGain;


    Serial.print("corrected voltage:");
    Serial.print(volt, 10);
    Serial.print("\n");

    //Calculate Torsion

    torque = volt * pie * pow(D, 3) / 8.0 * (E / (1.0 + nu)) / Vi/12.0;//the divide 12 is a foot conversion
    Serial.print("torque:");
    Serial.print(torque, 2);
    Serial.print("\n");
    delay(1000);
 */
}
