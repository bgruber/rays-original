#include <FastLED.h>

/*
  Serial Event example

  When new serial data arrives, this sketch adds it to a String.
  When a newline is received, the loop prints the string and
  clears it.

  A good test for this is to try it with a GPS receiver
  that sends out NMEA 0183 sentences.

  Created 9 May 2011
  by Tom Igoe

  This example code is in the public domain.

  http://www.arduino.cc/en/Tutorial/SerialEvent

*/


/***** setup for the LED strip *****/
// How many leds in your strip?
#define NUM_LEDS 100

// For led chips like Neopixels, which have a data line, ground, and power, you just
// need to define DATA_PIN.  For led chipsets that are SPI based (four wires - data, clock,
// ground, and power), like the LPD8806 define both DATA_PIN and CLOCK_PIN
#define DATA_PIN 31

// Define the array of leds
CRGBArray<NUM_LEDS> leds;


/***** Colors *****/
CRGBPalette16 myPalette = RainbowStripesColors_p;

/***** Buffers for the serial input *****/
String inputString = "";         // a string to hold incoming data
boolean stringComplete = false;  // whether the string is complete

void setup() {
  // setup for LED
  FastLED.addLeds<WS2811, DATA_PIN>(leds, NUM_LEDS);

  // initialize serial:
  Serial.begin(115200);
  // reserve 200 bytes for the inputString:
  inputString.reserve(200);
}

void blinkLed0() {
  // Turn the LED on, then pause
  leds[0] = CRGB::Red;
  FastLED.show();
  delay(500);
  // Now turn the LED off, then pause
  leds[0] = CRGB::Black;
  FastLED.show();
}

void blinkLedColor(String si, String sColor) {
  int i = si.toInt();
  int color = sColor.toInt();

  // leds[i] = ColorFromPalette(myPalette, color)
  leds[i] = CHSV(color,255,255);
  FastLED.show();
  delay(500);
  leds[i] = CRGB::Black;
  FastLED.show();
}

void blinkLed(String si) {
  int i = si.toInt();
  
  // Turn the LED on, then pause
  leds[i] = CRGB::Red;
  FastLED.show();
  delay(500);
  // Now turn the LED off, then pause
  leds[i] = CRGB::Black;
  FastLED.show();
}

int letters[] = {33, 34, 35, 36, 37, 38, 40};
void abcdefm() {
  for (int i=0; i < 7; i++) {
    leds[letters[i]] = CHSV(248, 255, 255);
    FastLED.show();
    delay(200);
  }
  for (int i=0; i < 7; i++) {
    leds[letters[i]] = CRGB::Black;
    FastLED.show();
    delay(100);
  }
}

void splitSpace(String s, String tokens[]) {
  s.trim();
  int i = 0;
  String si = s;
  int nextSpace = si.indexOf(" ");
  while (nextSpace >= 0) {
    tokens[i] = si.substring(0, nextSpace);
    si = si.substring(nextSpace + 1);
    i++;
    nextSpace = si.indexOf(" ");
  }
  if (si.length() > 0) {
    tokens[i] = si;
  }
}

void loop() {
  // print the string when a newline arrives:
  if (stringComplete) {
    // trim newline that must be there:
    inputString = inputString.substring(0, inputString.length() - 1);
    String tokens[5];
    splitSpace(inputString, tokens);
    String command = tokens[0];
    if(command == "blink") {
      blinkLed(tokens[1]);
    }
    else if (command == "value") {
      abcdefm();
    }
    else if (command == "color") {
      blinkLedColor(tokens[1], tokens[2]);
    }
    // clear the string:
    inputString = "";
    stringComplete = false;
  }
}

/*
  SerialEvent occurs whenever a new data comes in the
  hardware serial RX.  This routine is run between each
  time loop() runs, so using delay inside loop can delay
  response.  Multiple bytes of data may be available.
*/
void serialEvent() {
  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      stringComplete = true;
    }
  }
}


