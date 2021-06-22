import EllipticCurveKeyPair
import Flutter
import UIKit

/** Declared alias empty string*/
public var alias=""

public class SwiftFlutterEllipticCurveKeyPairPlugin: NSObject, FlutterPlugin {
    
    /**
    Configuration of EllipticCurveKeyPair
    Public key secured by [kSecAttrAccessibleAlwaysThisDeviceOnly]
    Private key secured by [kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly]
    */
    enum Shared {
        static let keypair: EllipticCurveKeyPair.Manager = {
            EllipticCurveKeyPair.logger = { print($0) }
            let publicAccessControl = EllipticCurveKeyPair.AccessControl(protection: kSecAttrAccessibleAlwaysThisDeviceOnly, flags: [])
            let privateAccessControl = EllipticCurveKeyPair.AccessControl(protection: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, flags: {
                EllipticCurveKeyPair.Device.hasSecureEnclave ? [.userPresence, .privateKeyUsage] : [.userPresence]
            }())
            let config = EllipticCurveKeyPair.Config(
                publicLabel: alias + ".sign.public",
                privateLabel: alias + ".sign.private",
                operationPrompt: "Sign transact ion",
                publicKeyAccessControl: publicAccessControl,
                privateKeyAccessControl: privateAccessControl,
                token: .secureEnclaveIfAvailable
            )
            return EllipticCurveKeyPair.Manager(config: config)
        }()
    }

    /** 
    Flutter channel configuration
    */
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_elliptic_curve_keypair", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterEllipticCurveKeyPairPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    /** 
    Handling flutter platform channel request
    from [FlutterMethodCall] 
    */
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
            case "ellipticCurveKeyPairSigning":
                if(call.arguments as? [String: Any] != nil) {
                    let arguments = call.arguments as? [String: Any]
                    if let messsage = arguments?["message"] as? String , let aliasname = arguments?["alias"] as? String  {
                        alias = aliasname
                        let signature = Signing(dataToBeSigned: messsage) as [String: Any];
                        if((signature["success"] as? Bool)!){
                            result(signature["message"] as? String)
                        }else{
                            result(FlutterError.init(
                                code: "FAILED_TO_AUTHENTICATE",
                                message: signature["message"] as? String,
                                details: nil
                            ))
                           
                        }
                    } else{
                        result(FlutterError.init(
                            code: "FAILED_TO_AUTHENTICATE",
                            message: "message or alias should not be empty",
                            details: nil
                        ))
                    }
                } else {
                    result(FlutterError.init(
                        code: "FAILED_TO_AUTHENTICATE",
                        message: "Arguments should not be null",
                        details: nil
                    ))
                }
                
            case "ellipticCurveKeyPairPublicKey":
                let publicKey = getPublicKey() as [String: Any]
                if((publicKey["success"] as? Bool)!){
                    result(publicKey["message"] as? String)
                }else{
                    result(FlutterError.init(
                        code: "FAILED_TO_AUTHENTICATE",
                        message: signature["message"] as? String,
                        details: nil
                    ))       
                }

        default:
            result(FlutterError.init(
                code: "METHOD_NOT_IMPLIMENTED",
                message: "Method could not implimented",
                details: nil
            ))
        }
    }

    /**
    EllipticCurveKeyPair Signing
    @param should be the [messsage] to be signing using ECC algorithm 
    @return signed message using [Keypair]  
    */
    public func Signing(dataToBeSigned: String) -> [String: Any] {
        if !checkPrivateKeyAvailable(){
            do {
                try Shared.keypair.deleteKeyPair() 
            }catch {
                let result = ["success": false, "message": "Unexpected error: \(error)"] as [String : Any]
                return result
            }
        }
        do {
            let digest = dataToBeSigned.data(using: .utf8)!
            if #available(iOS 10, *) {
                let singedData = try Shared.keypair.sign(digest, hash: .sha256)
                let signature = singedData.base64EncodedString()
                let result = ["success": true, "message": signature] as [String : Any]
                return result
            } else {
                let result = ["success": false, "message": "Can only authenticate iOS 10 or above"] as [String : Any]
                return result
            }
        } catch {
            let result = ["success": false, "message": "Unexpected error: \(error)"] as [String : Any]
            return result
        }
    }

    /**
    Get the publicKey from generate keypair
    @return [Public Key] encoded to string
    */
    private func getPublicKey() -> [String: Any] {
        do {
            let key = try Shared.keypair.publicKey().data()
            let publicKey = key.DER.base64EncodedString();
            let result = ["success": true, "message": publicKey] as [String : Any]
            return publicKey
        } catch { 
            let result = ["success": false, "message": "Cannot get public key: \(error)"] as [String : Any]
            return result
        }
    }

    /**
    Check the Privatekey available or not
    @return [True] if available of privateKey,[False] if not available
    */
    private func checkPrivateKeyAvailable() -> Bool {
        do{
            let key = try Shared.keypair.privateKey()
            return true
        } catch{
            return false
        }
    }
}
