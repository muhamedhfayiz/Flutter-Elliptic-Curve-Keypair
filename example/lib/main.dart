import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_elliptic_curve_keypair/flutter_elliptic_curve_keypair.dart';
import 'package:flutter_elliptic_curve_keypair/models/ecc_result_model.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String signed;
  String public;

  Future<void> initPlatformState() async {
    EccResultModel signedData;
    try {
      String dataToSign = "data_to_sign";
      String language = "en";
      String alias = "your_alias_name";

      signedData =
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

  Future<void> ellipticCurveKeyPairPublicKey() async {
    EccResultModel publicKey;
    try {
      publicKey =
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

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text(''),
        ),
        body: Column(
          children: [
            Center(
              child: RaisedButton(
                child: Text('Sign data'),
                onPressed: () => initPlatformState(),
              ),
            ),
            Text(signed != null ? signed : ""),
            Center(
              child: RaisedButton(
                child: Text('Get public key'),
                onPressed: () => ellipticCurveKeyPairPublicKey(),
              ),
            ),
            Text(public != null ? public : ""),
          ],
        ),
      ),
    );
  }
}
