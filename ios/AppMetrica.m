/*
 * Version for React Native
 * © 2020 YANDEX
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://yandex.com/legal/appmetrica_sdk_agreement/
 */

#import "AppMetrica.h"
#import "AppMetricaUtils.h"

static NSString *const kYMMReactNativeExceptionName = @"ReactNativeException";

@implementation AppMetrica

@synthesize methodQueue = _methodQueue;

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(activate:(NSDictionary *)configDict)
{
    [YMMYandexMetrica activateWithConfiguration:[AppMetricaUtils configurationForDictionary:configDict]];
}

RCT_EXPORT_METHOD(getLibraryApiLevel)
{
    // It does nothing for iOS
}

RCT_EXPORT_METHOD(getLibraryVersion:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve([YMMYandexMetrica libraryVersion]);
}

RCT_EXPORT_METHOD(pauseSession)
{
    [YMMYandexMetrica pauseSession];
}

RCT_EXPORT_METHOD(reportAppOpen:(NSString *)deeplink)
{
    [YMMYandexMetrica handleOpenURL:[NSURL URLWithString:deeplink]];
}

RCT_EXPORT_METHOD(reportError:(NSString *)message) {
    NSException *exception = [[NSException alloc] initWithName:message reason:nil userInfo:nil];
    [YMMYandexMetrica reportError:message exception:exception onFailure:NULL];
}

RCT_EXPORT_METHOD(reportEvent:(NSString *)eventName:(NSDictionary *)attributes)
{
    if (attributes == nil) {
        [YMMYandexMetrica reportEvent:eventName onFailure:^(NSError *error) {
            NSLog(@"error: %@", [error localizedDescription]);
        }];
    } else {
        [YMMYandexMetrica reportEvent:eventName parameters:attributes onFailure:^(NSError *error) {
            NSLog(@"error: %@", [error localizedDescription]);
        }];
    }
}

RCT_EXPORT_METHOD(reportReferralUrl:(NSString *)referralUrl)
{
    [YMMYandexMetrica reportReferralUrl:[NSURL URLWithString:referralUrl]];
}

RCT_EXPORT_METHOD(requestAppMetricaDeviceID:(RCTResponseSenderBlock)listener)
{
    YMMAppMetricaDeviceIDRetrievingBlock completionBlock = ^(NSString *_Nullable appMetricaDeviceID, NSError *_Nullable error) {
        listener(@[[self wrap:appMetricaDeviceID], [self wrap:[AppMetricaUtils stringFromRequestDeviceIDError:error]]]);
    };
    [YMMYandexMetrica requestAppMetricaDeviceIDWithCompletionQueue:nil completionBlock:completionBlock];
}

RCT_EXPORT_METHOD(resumeSession)
{
    [YMMYandexMetrica resumeSession];
}

RCT_EXPORT_METHOD(sendEventsBuffer)
{
    [YMMYandexMetrica sendEventsBuffer];
}

RCT_EXPORT_METHOD(setLocation:(NSDictionary *)locationDict)
{
    [YMMYandexMetrica setLocation:[AppMetricaUtils locationForDictionary:locationDict]];
}

RCT_EXPORT_METHOD(setLocationTracking:(BOOL)enabled)
{
    [YMMYandexMetrica setLocationTracking:enabled];
}

RCT_EXPORT_METHOD(setStatisticsSending:(BOOL)enabled)
{
    [YMMYandexMetrica setStatisticsSending:enabled];
}

RCT_EXPORT_METHOD(setUserProfileID:(NSString *)userProfileID)
{
    [YMMYandexMetrica setUserProfileID:userProfileID];
}

- (NSObject *)wrap:(NSObject *)value
{
    if (value == nil) {
        return [NSNull null];
    }
    return value;
}

RCT_EXPORT_METHOD(reportUserProfile:(NSString *)userProfileID
                  userProfileParam:(NSDictionary *)userProfileParam
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
    
    if (userProfileID == NULL) {
        reject(@"-101", @"UserProfileId can't be null", NULL);
    }
    
    [YMMYandexMetrica setUserProfileID:userProfileID];
    
    if (userProfileParam != NULL) {
        YMMMutableUserProfile *profile = [[YMMMutableUserProfile alloc] init];
        
        for (NSString *key in userProfileParam) {
            if ([key isEqual:@"name"]) {
                NSString *value = [userProfileParam valueForKey:key];
                YMMUserProfileUpdate *name = [[YMMProfileAttribute name] withValue:value];
                [profile apply:name];
            } else if ([key isEqual:@"gender"]) {
                NSString *value = [userProfileParam valueForKey:key];
                YMMUserProfileUpdate *gender = NULL;

                if ([value isEqual: @"male"]) {
                    gender = [[YMMProfileAttribute gender] withValue:YMMGenderTypeMale];
                } else if ([value isEqual: @"female"]) {
                    gender = [[YMMProfileAttribute gender] withValue:YMMGenderTypeFemale];
                } else {
                    gender = [[YMMProfileAttribute gender] withValue:YMMGenderTypeOther];
                }
                
                [profile apply:gender];
            } else if ([key isEqual:@"birthDate"]) {
                NSNumber *value = [userProfileParam valueForKey:key];
                
                if (value != NULL) {
                    YMMUserProfileUpdate *birthDate = [[YMMProfileAttribute birthDate] withAge:value.intValue];
                    [profile apply:birthDate];
                }

            } else if ([key isEqual:@"notificationsEnabled"]) {
                BOOL value = [userProfileParam objectForKey:key];
                YMMUserProfileUpdate *notificationsEnabled = [[YMMProfileAttribute notificationsEnabled] withValue:value];
                [profile apply:notificationsEnabled];
            } else {
                YMMUserProfileUpdate *customAttribute = NULL;
                
                if ([[userProfileParam valueForKey:key] isKindOfClass:[NSString class]]) {
                    NSString *value = [userProfileParam valueForKey:key];
                    customAttribute = [[YMMProfileAttribute customString:key] withValue:value];
                } else if ([[userProfileParam valueForKey:key] isKindOfClass:[NSNumber class]]) {
                    NSNumber *value = [userProfileParam valueForKey:key];
                    
                    if (([value isEqual:[NSNumber numberWithBool:YES]] || [value isEqual:[NSNumber numberWithBool:NO]])
                        && value.intValue >= 0 && value.intValue <= 1) {
                        customAttribute = [[YMMProfileAttribute customBool:key] withValue:value.boolValue];
                    } else {
                        customAttribute = [[YMMProfileAttribute customNumber:key] withValue:value.intValue];
                    }
                }

                if (customAttribute != NULL) {
                    [profile apply:customAttribute];
                }
            }
        }
        
        if ([profile updates].count > 0) {
            [YMMYandexMetrica reportUserProfile:[profile copy] onFailure:^(NSError *error) {
                reject(@"-103", error.localizedDescription, error);
            }];
            
            resolve(userProfileParam);
        } else {
            reject(@"-102", @"Valid keys not found", NULL);
        }
    }
    
}

@end
