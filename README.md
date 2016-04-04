# Band Detection [![Build Status](https://travis-ci.org/Shelnutt2/BandDetection.svg?branch=master)](https://travis-ci.org/Shelnutt2/BandDetection) [![Coverage Status](https://coveralls.io/repos/github/Shelnutt2/BandDetection/badge.svg?branch=master)](https://coveralls.io/github/Shelnutt2/BandDetection?branch=master)

This library is designed for rooted qualcomm android devices.

This library is experimental and is missing all safety checks.  Use this
at your own risk.

## Documentation

Java docs are available here:
[http://shelnutt2.github.io/BandDetection/](http://shelnutt2.github.io/BandDetection/)

## Usage

This library requires root and uses RootTools for access.

Include this in your gradle dependencies

```
compile 'nu.shel.banddetection:banddetection:0.9.0'
```

To get the band call the static function BandDetection.DetectBand.
This will find the modem serial device, get earfcn and translate it to 
a band object.

```java
LTEBand currentBand = BandDetection.DetectBand();
```

## Advanced Usage

This library also provides a way to run raw commands on the modem.

```java
// Create new modem object
Modem mModem = new Modem();
// Run "AT" serial command and get output in ArrayList of each line.
ArrayList<String> output = mModem.RunModemCommand("AT");
```