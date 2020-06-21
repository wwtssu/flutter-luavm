#import <Flutter/Flutter.h>

@interface LuavmPlugin : NSObject<FlutterPlugin>
@property (atomic , strong) FlutterBasicMessageChannel* callback;
@end
