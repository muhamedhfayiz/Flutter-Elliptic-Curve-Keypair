# Flutter elliptic curve keypair

This Flutter plugin provides means to perform encrypt data using ellitic curve keypair algorthm,encapsulated with biometric crypto and get the public key to validate.

> Supported Platforms
>
> - Android
> - IOS

> Features
>
> - Encrypt data using ellitic curve keypair algorthm
> - Get public key to validate signed data (Can be used in server side to validate)
> - Support Arabic and English (Only for android)

## Usage in Dart 


```yaml
# add this line to your pubspec.yaml
flutter_elliptic_curve_keypair: ^0.0.1
```

```dart
// Import the relevant plugin:
import 'package:flutter_elliptic_curve_keypair/flutter_elliptic_curve_keypair.dart';
```


To encrypt data

```dart
Future<void> encryptData() async {
    try {

      String dataToSign = "data_to_sign"; // data to encript
      String language = "en"; // support english and arabic (only for android) "en" => english & "ar" => Arabic
      String alias = "your_alias_name"; // key pair alias

     EccResultModel signedData =
          await FlutterEllipticCurveKeyPair.ellipticCurveKeyPairSigning(
        message: dataToSign,
        language: language,
        alias: alias,
      );

      if (signedData.success) {
        setState(() {
          signed = "SUCCEESS, SIGNED DATA HERE => ${signedData.data}";
        });
      } else {
        setState(() {
          public = "FAILED, ERROR => ${signedData.data.toString()}";
        });
      }

    } on PlatformException catch (e) {
      print("Unexpected error occured$e");
    }
  }
```

To get public key

```dart
Future<void> ellipticCurveKeyPairPublicKey() async {
    try {
      EccResultModel publicKey =
          await FlutterEllipticCurveKeyPair.ellipticCurveKeyPairPublicKey();

      if (publicKey.success) {
        setState(() {
          public = "SUCCEESS, PUBLIC KEY HERE => ${publicKey.data}";
        });
      } else {
        setState(() {
          signed = "FAILED, ERROR => ${publicKey.data.toString()}";
        });
      }

    } on PlatformException catch (e) {
      print("Unexpected error occured$e");
    }
  }
```



## Android integration
Update your MainActivity.kt

```kotlin
    // package com.plugin.elliptic_curve_key_pair ** use your app bundle id here **

    import io.flutter.embedding.android.FlutterFragmentActivity
    import io.flutter.embedding.engine.FlutterEngine
    import io.flutter.plugins.GeneratedPluginRegistrant


    class MainActivity: FlutterFragmentActivity() {
        override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        }
    }
```

Update app permission

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```
    

## IOS integration

Update info.plist file and add permission for access biometric
```xml
<key>NSFaceIDUsageDescription</key>
<string>Would like to use Face ID for authetication</string>
```