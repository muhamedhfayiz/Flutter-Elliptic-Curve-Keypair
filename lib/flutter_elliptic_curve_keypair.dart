import 'dart:async';
import 'package:flutter/services.dart';
import 'models/ecc_result_model.dart';

class FlutterEllipticCurveKeyPair {
  static const MethodChannel _channel =
      const MethodChannel('flutter_elliptic_curve_keypair');

  /// Sign the message using ellipctic curve algorithm (ECC)
  /// argument Map of [message,language]
  /// language should be string of [Arabic] or [English]
  /// return [EccResultModel] with [Signed Message] if success
  /// return [EccResultModel] with [Exception] if fail
  static Future<EccResultModel> ellipticCurveKeyPairSigning(
      {String message, String language, String alias}) async {
    Map arguments = {
      "message": message,
      "language": language,
      "alias": alias,
    };

    try {
      final String signedMessage =
          await _channel.invokeMethod('ellipticCurveKeyPairSigning', arguments);
      EccResultModel result = EccResultModel(
        data: signedMessage,
        success: true,
        description: "Signed message successfully",
      );
      return result;
    } on PlatformException catch (e) {
      EccResultModel platformException = EccResultModel(
        data: e.details,
        success: false,
        description: e.message,
      );
      return platformException;
    }
  }

  /// Get the public key from key pair
  /// return [EccResultModel] with [Publickey] if success
  /// return [EccResultModel] with [Exception] if fail
  static Future<EccResultModel> ellipticCurveKeyPairPublicKey() async {
    try {
      final String publicKey =
          await _channel.invokeMethod('ellipticCurveKeyPairPublicKey');
      EccResultModel result = EccResultModel(
        data: publicKey,
        success: true,
        description: "Public key recieved successfully",
      );
      return result;
    } on PlatformException catch (e) {
      EccResultModel platformException = EccResultModel(
        data: e.details,
        success: false,
        description: e.message,
      );
      return platformException;
    }
  }
}
