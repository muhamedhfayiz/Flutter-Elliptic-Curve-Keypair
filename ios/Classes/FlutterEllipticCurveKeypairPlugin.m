#import "FlutterEllipticCurveKeypairPlugin.h"
#if __has_include(<flutter_elliptic_curve_keypair/flutter_elliptic_curve_keypair-Swift.h>)
#import <flutter_elliptic_curve_keypair/flutter_elliptic_curve_keypair-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_elliptic_curve_keypair-Swift.h"
#endif

@implementation FlutterEllipticCurveKeypairPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    [SwiftFlutterEllipticCurveKeyPairPlugin registerWithRegistrar:registrar];
}
@end
