#import "AppDelegate.h"
#import "UserNotifications/UNNotificationContent.h"
#import "UserNotifications/UNUserNotificationCenter.h"
#import "UserNotifications/UNNotificationRequest.h"
#import "Intents/Intents.h"
#import <CallKit/CallKit.h>
#import <PushKit/PushKit.h>
#import <objc/runtime.h>

@interface AppDelegate (CordovaCall) <UIApplicationDelegate, CXProviderDelegate>

@end
