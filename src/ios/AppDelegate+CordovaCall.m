#import "AppDelegate+CordovaCall.h"

@implementation AppDelegate (CordovaCall)

NSString *deepLink;

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
    
    NSLog(@"Push notification fetch");
    
    
    deepLink = [userInfo objectForKey:@"l"];
    
    NSError *error;
    NSString *meetingDetailsJson = [userInfo objectForKey:@"u"];
    NSData *jsonData = [meetingDetailsJson dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableDictionary *data = [NSJSONSerialization JSONObjectWithData:jsonData
                                                       options:NSJSONReadingAllowFragments
                                                         error:&error];
    
    //NSString *displayName = [data objectForKey:@"DisplayName"];
    NSString *displayName = @"test";
    
    NSUUID *callUUID = [[NSUUID alloc] init];
    
    CXHandle *handle = [[CXHandle alloc] initWithType:CXHandleTypePhoneNumber value:@"1"];
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = handle;
    callUpdate.hasVideo = YES;
    callUpdate.localizedCallerName = displayName;
    callUpdate.supportsGrouping = NO;
    callUpdate.supportsUngrouping = NO;
    callUpdate.supportsHolding = NO;
    callUpdate.supportsDTMF = NO;
    
    CXProviderConfiguration *providerConfiguration;
    NSString* appName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    providerConfiguration = [[CXProviderConfiguration alloc] initWithLocalizedName: appName];
    providerConfiguration.maximumCallGroups = 1;
    providerConfiguration.maximumCallsPerCallGroup = 1;
    NSMutableSet *handleTypes = [[NSMutableSet alloc] init];
    [handleTypes addObject:@(CXHandleTypePhoneNumber)];
    providerConfiguration.supportedHandleTypes = handleTypes;
    providerConfiguration.supportsVideo = YES;
    if (@available(iOS 11.0, *)) {
        providerConfiguration.includesCallsInRecents = NO;
    }
    CXProvider *provider = [[CXProvider alloc] initWithConfiguration:providerConfiguration];
    //self.provider = [[CXProvider alloc] initWithConfiguration:providerConfiguration];
    
    if (error) {
        NSLog(@"Got an error: %@", error);
    } else {
        NSLog(@"%@", data);
    }
    
    [provider setDelegate:self queue:nil];
    [provider reportNewIncomingCallWithUUID:callUUID update:callUpdate completion:^(NSError * _Nullable error) {
        
        if(error == nil) {
            //if ([[UIApplication sharedApplication] applicationState] !=  UIApplicationStateActive){
            //CordovaCall *cordovaCall = [CordovaCall sharedInstance];
            //[cordovaCall.provider answer]
                
               
            //}
            //[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"Incoming call successful"] callbackId:command.callbackId];
        } else {
            //[self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]] callbackId:command.callbackId];
        }
    }];
    
}

- (void)applicationDidBecomeActive:(UIApplication *)application{
    if (openDeeplink){
        UIApplication *application = [UIApplication sharedApplication];
        if (application.applicationState == UIApplicationStateBackground){
            NSLog(@"App is in background...");
        }
        
        NSURL *URL = [NSURL URLWithString:deepLink];
        [application openURL:URL options:@{} completionHandler:^(BOOL success) {
            if (success) {
                 NSLog(@"Opened url");
            }
        }];
        openDeeplink = false;
    }
}

bool openDeeplink = false;

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action
{
    [action fulfill];
    openDeeplink = true;
    
    [[NSUserDefaults standardUserDefaults] setBool:true forKey:@"openZoom"];
}

- (BOOL)application:(UIApplication *)application continueUserActivity:(NSUserActivity *)userActivity restorationHandler:(void (^)(NSArray *restorableObjects))restorationHandler
{
    INInteraction *interaction = userActivity.interaction;
    INIntent *intent = interaction.intent;
    BOOL isVideo = [intent isKindOfClass:[INStartVideoCallIntent class]];
    INPerson *contact;
    if(isVideo) {
        INStartVideoCallIntent *startCallIntent = (INStartVideoCallIntent *)intent;
        contact = startCallIntent.contacts.firstObject;
    } else {
        INStartAudioCallIntent *startCallIntent = (INStartAudioCallIntent *)intent;
        contact = startCallIntent.contacts.firstObject;
    }
    INPersonHandle *personHandle = contact.personHandle;
    NSString *callId = personHandle.value;
    NSString *callName = [[NSUserDefaults standardUserDefaults] stringForKey:callId];
    if(!callName) {
        callName = callId;
    }
    NSDictionary *intentInfo = @{ @"callName" : callName, @"callId" : callId, @"isVideo" : isVideo?@YES:@NO};
    [[NSNotificationCenter defaultCenter] postNotificationName:@"RecentsCallNotification" object:intentInfo];
    return YES;
}
@end
