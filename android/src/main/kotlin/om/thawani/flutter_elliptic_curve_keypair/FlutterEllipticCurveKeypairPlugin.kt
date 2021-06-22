package om.thawani.flutter_elliptic_curve_keypair

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.arch.core.executor.ArchTaskExecutor.getMainThreadExecutor
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.util.*

/** FLUTTER ELLIPTIC CURVE KEYPAIR PLUGIN */
class FlutterEllipticCurveKeypairPlugin :
    FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel
  private lateinit var context: Context
  private lateinit var activity: Activity
  private lateinit var biometricManager: BiometricManager
  var KEYPAIR_ALIAS = "key_pair_alias"
  var messageToSign: String = ""
  var language: String = "English"
  lateinit var channelResult: MethodChannel.Result

  /** On attached to engine */
  override fun onAttachedToEngine(
      @NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding
  ) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_elliptic_curve_keypair")
    channel.setMethodCallHandler(this)
  }

  /**
   * On method call by flutter channel
   * @param MethodCall contains the method flutter channel triggered
   * @param Result to be sent back to flutter channel Method call should contains Map of Language &
   * Message to be sign
   */
  @RequiresApi(Build.VERSION_CODES.M)
  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    channelResult = result
    when (call.method) {
      "ellipticCurveKeyPairSigning" -> {
        if (call.argument<String>("message") == null && call.argument<String>("alias") ==  null) {
          channelResult.error("Could not authenticate", "Arguments should not be null", false)
        }
        messageToSign = call.argument("message")!!
        language = call.argument("language")!!
        KEYPAIR_ALIAS = call.argument("alias")!!
        intiateEllipticKeyPairSigning()
      }
      "ellipticCurveKeyPairPublicKey" -> {
        try {
          val publicKey: String? = getPublicKey()
          channelResult.success(publicKey)
        } catch (e: Exception) {
          channelResult.error(
              "UNEXPECTED_ERROR_OCCURED", "Could not generate public key ${e.toString()}", null)
        }
      }
      else -> {
        channelResult.notImplemented()
      }
    }
  }

  /**
   * Initiate ECC signing using biometric auth
   * @returns if any occured such BIOMETRIC_ERROR_NONE_ENROLLED or BIOMETRIC_NOT_AVAILABLE if
   * Success will show Biometric prompt
   */
  @RequiresApi(Build.VERSION_CODES.M)
  fun intiateEllipticKeyPairSigning() {
    val canAuthenticate = biometricManager?.canAuthenticate()
    if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
      val signature: Signature?
      try {
        // val generateKeyPair = generateKeyPair()
        signature = initSignature()
      } catch (e: Exception) {
        channelResult.error("UNEXPECTED_ERROR_OCCURED", e.toString(), null)
        throw RuntimeException(e)
      }
      // Show biometric promt
      showBiometricPrompt(signature)
    } else if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
      channelResult.error("BIOMETRIC_ERROR_NONE_ENROLLED", "Biometric not enrolled", null)
    } else {
      channelResult.error("BIOMETRIC_NOT_AVAILABLE", "Biometric not available", null)
    }
  }

  /**
   * Configuring biometric prompt
   * @param signature that already Signed using keypair if successfull authentication will call
   * [getAuthenticationCallback] method
   * @exception if signature null
   */
  @SuppressLint("RestrictedApi")
  fun showBiometricPrompt(signature: Signature?) {
    val authenticationCallback: BiometricPrompt.AuthenticationCallback = getAuthenticationCallback()
    val mBiometricPrompt =
        BiometricPrompt(
            activity as FragmentActivity, getMainThreadExecutor(), authenticationCallback)
    val promptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setDescription(
                if (language == "English") "Scan your fingerprint to authenticate"
                else "للدخول، يجب التحقق من بصمة الإصبع")
            .setTitle(
                if (language == "English") "Fingerprint Authentication"
                else "التحقق من بصمة الإصبع")
            .setSubtitle(if (language == "English") "Touch Sensor" else "مستشعر البصمة")
            .setNegativeButtonText(if (language == "English") "Cancel" else "انسَ الأمر")
            .setConfirmationRequired(false)
            .build()

    if (signature != null) {
      mBiometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(signature))
    } else {
      channelResult.error("CAN_NOT_AUTHENTICATE", "Failed to sign message", null)
    }
  }

  /**
   * Callback for biometric authentication result
   * @return if signed message encoded by string if successfull authentication else returns
   * expections
   */
  private fun getAuthenticationCallback(): BiometricPrompt.AuthenticationCallback {
    return object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationError(errorCode: Int, @NonNull errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Log.d(TAG, "error on biometric authentication")
      }

      // if succesfull authentication
      override fun onAuthenticationSucceeded(
          @NonNull result: BiometricPrompt.AuthenticationResult
      ) {
        super.onAuthenticationSucceeded(result)
        if (result.cryptoObject != null && result.cryptoObject!!.signature != null) {
          val signature = result.cryptoObject!!.signature
          signature!!.update(messageToSign?.toByteArray()!!)
          val signatureString = Base64.encodeToString(signature!!.sign(),Base64.DEFAULT)
          Log.d(TAG, "Succesfully authenticated")
          channelResult.success(signatureString)
        } else {
          Log.d(TAG, "Succesfully authenticated")
          channelResult.error(
              "INVALID_CRYPTO_OBJECT_SIGNATURE",
              "onAuthentication Succeeded but cryptoObject!!.signature is null",
              null)
        }
      }

      // if authentication failed
      fun onAuthenticationFailed(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Log.d(TAG, "error on biometric authentication")
        channelResult.error(
            "onAuthenticationFailed",
            "Authentication failed ${errString}, with code ${errorCode}",
            null)
      }
    }
  }

  /**
   * Initiate signature using SHA256withECDSA
   * @return signature
   */
  @RequiresApi(Build.VERSION_CODES.M)
  fun initSignature(): Signature? {
    val entry = getKeyPair() ?: return null
    val signature = Signature.getInstance("SHA256withECDSA")
    signature.initSign(entry.private)
    return signature
  }


  /**
   * Generate keypair will check if keypair available if Keypair not available generate new keypair
   * @return KeyPair
   */
  @RequiresApi(Build.VERSION_CODES.M)
  private fun getKeyPair(): KeyPair? {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    if(!keyStore.containsAlias(KEYPAIR_ALIAS)){
      generateKeyPair()
      val publicKey = keyStore.getCertificate(KEYPAIR_ALIAS).publicKey
      val privateKey = keyStore.getKey(KEYPAIR_ALIAS, null) as PrivateKey
      return KeyPair(publicKey, privateKey)
    }
    val publicKey = keyStore.getCertificate(KEYPAIR_ALIAS).publicKey
    val privateKey = keyStore.getKey(KEYPAIR_ALIAS, null) as PrivateKey
    return KeyPair(publicKey, privateKey)
    
  }

  /**
   * Get public key will check if keypair available if Keypair not available generate new keypair
   * @return public key encoded string
   */
  @RequiresApi(Build.VERSION_CODES.M)
  private fun getPublicKey(): String {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)
    if(!keyStore.containsAlias(KEYPAIR_ALIAS)){
      generateKeyPair()
      val publicKey = keyStore.getCertificate(KEYPAIR_ALIAS).publicKey
      return Base64.encodeToString(publicKey.encoded, Base64.DEFAULT)
    }
    val publicKey = keyStore.getCertificate(KEYPAIR_ALIAS).publicKey
    return Base64.encodeToString(publicKey.encoded, Base64.DEFAULT)
  }

  /**
   * Generate key pair Configuring key pair: Validity 30 years Generated keys will be invalidated if
   * the biometric templates are added more to user device Using KEY_ALGORITHM_EC
   */
  @RequiresApi(Build.VERSION_CODES.M)
  private fun generateKeyPair(): KeyPair {
    val end = Calendar.getInstance()
    end.add(Calendar.YEAR, 30)
    val keyPairGenerator =
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    val builder =
        KeyGenParameterSpec.Builder(KEYPAIR_ALIAS, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(
                KeyProperties.DIGEST_SHA256) // Require the user to authenticate with a biometric to
            // authorize every use of the key
            .setUserAuthenticationRequired(false)
            .setKeyValidityEnd(end.time)
    // Generated keys will be invalidated if the biometric templates are added more to user device
    if (Build.VERSION.SDK_INT >= 24) {
      builder.setInvalidatedByBiometricEnrollment(true)
    }
    keyPairGenerator.initialize(builder.build())
    return keyPairGenerator.generateKeyPair()
  }

  /**
   * Intialize biometric on attached to activity
   * @param Activity
   */
  private fun setServicesFromActivity(activity: Activity?) {
    if (activity == null) return
    this.activity = activity
    val context = activity.baseContext
    biometricManager = BiometricManager.from(activity)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onDetachedFromActivity() {
    activity.finish()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    binding.addActivityResultListener(this)
    setServicesFromActivity(binding.activity)
    channel.setMethodCallHandler(this)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    binding.addActivityResultListener(this)
    setServicesFromActivity(binding.activity)
    channel.setMethodCallHandler(this)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    return true
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity.finish()
  }
}
