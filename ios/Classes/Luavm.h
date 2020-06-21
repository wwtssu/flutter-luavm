#import <Flutter/Flutter.h>

@interface Luavm : NSObject
+ (Luavm *)inst;
- (NSNumber *) open;
- (NSNumber *) close:(int)idx;
- (NSArray *)eval:(int)idx withCode:(NSString *)code;
- (void)evalAsync:(int)idx withCode:(NSString *)code withCallback:(FlutterBasicMessageChannel*) callback;
@end
