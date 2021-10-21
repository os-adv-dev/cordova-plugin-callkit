#import "AppDelegate+CordovaCall.h"
#import "CordovaCall.h"

@implementation AppDelegate (CordovaCall)

NSString *meetingNumber;
NSString *meetingPassword;


- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler {
    
    NSLog(@"Push notification fetch");
    
    NSError *error;
    NSString *meetingDetailsJson = [userInfo objectForKey:@"u"];
    NSData *jsonData = [meetingDetailsJson dataUsingEncoding:NSUTF8StringEncoding];
    NSMutableDictionary *data = [NSJSONSerialization JSONObjectWithData:jsonData
                                                       options:NSJSONReadingAllowFragments
                                                         error:&error];
    
    meetingNumber = [data objectForKey:@"MeetingNumber"];
    meetingPassword = [data objectForKey:@"MeetingPassword"];
    NSString *displayName = [data objectForKey:@"DisplayName"];
    
    NSUUID *callUUID = [[NSUUID alloc] init];
    
    CXHandle *handle = [[CXHandle alloc] initWithType:CXHandleTypePhoneNumber value:meetingNumber];
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
    
    if (error) {
        NSLog(@"Got an error: %@", error);
    } else {
        NSLog(@"%@", data);
    }
    
    [provider setDelegate:self queue:nil];
    [provider reportNewIncomingCallWithUUID:callUUID update:callUpdate completion:^(NSError * _Nullable error) {
        if(error == nil) {
            NSLog(@"Phone is ringing!");
        } else {
            NSLog(@"An error occoured starting the call");
        }
    }];
    
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action
{
    [action fulfill];
    UIApplication *application = [UIApplication sharedApplication];
    [[NSUserDefaults standardUserDefaults] setBool:true forKey:@"CallAnsweredByUser"];
    [[NSUserDefaults standardUserDefaults] setValue:meetingNumber forKey:@"MeetingNumber"];
    [[NSUserDefaults standardUserDefaults] setValue:meetingPassword forKey:@"MeetingPassword"];
    
    if (application.applicationState == UIApplicationStateActive){
        CordovaCall *cordovaCall = [CordovaCall sharedInstance];
        [cordovaCall callAnswerCallbacks];
    }
    
}

- (void)providerDidReset:(nonnull CXProvider *)provider { 
    NSLog(@"%s","providerdidreset");
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
