/*
 * Version for React Native
 * © 2020 YANDEX
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://yandex.com/legal/appmetrica_sdk_agreement/
 */

package io.appmetrica.analytics.plugin.reactnative;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.ReadableMapKeySetIterator;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;
import io.appmetrica.analytics.profile.Attribute;
import io.appmetrica.analytics.profile.GenderAttribute;
import io.appmetrica.analytics.profile.UserProfile;
import io.appmetrica.analytics.profile.UserProfileUpdate;
import io.appmetrica.analytics.StartupParamsCallback;
import java.util.Arrays;

public class AppMetricaModule extends ReactContextBaseJavaModule {

    private static final String TAG = "AppMetricaModule";

    private final ReactApplicationContext reactContext;
    
    static ReactApplicationContext reactApplicationContext;
    static ReadableMap activateParams = null;
    static String activateKey = null;

    public AppMetricaModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactApplicationContext = reactContext;
    }

    @Override
    public String getName() {
        return "AppMetrica";
    }
    
    public static void staticReportAppOpen(Intent intent) {
        if (activateMetrics()) {
            AppMetrica.reportAppOpen(intent);
        }
    }

    public static void staticReportAppOpen(Activity activity) {
        if (activateMetrics()) {
            AppMetrica.reportAppOpen(activity);
        }
    }

    public static void staticReportAppOpen(String deeplink) {
        if (activateMetrics()) {
            AppMetrica.reportAppOpen(deeplink);
        }
    }
	
    public static void staticReportReferralUrl(String deeplink) {
        if (activateMetrics()) {
            AppMetrica.reportReferralUrl(deeplink);
        }
    }

    private static boolean activateMetrics() {
        if (activateParams != null) {
            staticActivateWithConfig(activateParams);
            return true;
        } else if (activateKey != null) {
            staticActivateWithApiKey(activateKey);
            return true;
        }

        return false;
    }

    private static void staticActivateWithApiKey(String key) {
        activateKey = key;
        AppMetricaConfig.Builder configBuilder = AppMetricaConfig.newConfigBuilder(key).withLogs();
        AppMetrica.activate(reactApplicationContext.getApplicationContext(), configBuilder.build());
        Activity activity = reactApplicationContext.getCurrentActivity();
        if (activity != null) {
            Application application = activity.getApplication();
            AppMetrica.enableActivityAutoTracking(application);
        }
    }

    private static void staticActivateWithConfig(ReadableMap params) {
        activateParams = params;
        AppMetrica.activate(reactApplicationContext.getApplicationContext(), Utils.toAppMetricaConfig(params));
        Activity activity = reactApplicationContext.getCurrentActivity();
        if (activity != null) {
            Application application = activity.getApplication();
            AppMetrica.enableActivityAutoTracking(application);
        }
    }

    @ReactMethod
    public void activate(ReadableMap configMap) {
        AppMetrica.activate(reactContext, Utils.toAppMetricaConfig(configMap));
        enableActivityAutoTracking();
    }

    private void enableActivityAutoTracking() {
        Activity activity = getCurrentActivity();
        if (activity != null) { // TODO: check
            AppMetrica.enableActivityAutoTracking(activity.getApplication());
        } else {
            Log.w(TAG, "Activity is not attached");
        }
    }

    @ReactMethod
    public void getLibraryApiLevel(Promise promise) {
        promise.resolve(AppMetrica.getLibraryApiLevel());
    }

    @ReactMethod
    public void getLibraryVersion(Promise promise) {
        promise.resolve(AppMetrica.getLibraryVersion());
    }

    @ReactMethod
    public void pauseSession() {
        AppMetrica.pauseSession(getCurrentActivity());
    }

    @ReactMethod
    public void reportAppOpen(String deeplink) {
        AppMetrica.reportAppOpen(deeplink);
    }

    @ReactMethod
    public void reportError(String message) {
        try {
            Integer.valueOf("00xffWr0ng");
        } catch (Throwable error) {
            AppMetrica.reportError(message, error);
        }
    }

    @ReactMethod
    public void reportEvent(String eventName, ReadableMap attributes) {
        if (attributes == null) {
            AppMetrica.reportEvent(eventName);
        } else {
            AppMetrica.reportEvent(eventName, attributes.toHashMap());
        }
    }

    @ReactMethod
    public void reportReferralUrl(String referralUrl) {
        AppMetrica.reportReferralUrl(referralUrl);
    }

    @ReactMethod
    public void requestAppMetricaDeviceID(Callback listener) {

        StartupParamsCallback startupParamsCallback = new StartupParamsCallback() {
            @Override
            public void onReceive(Result result) {
                if (result != null) {
                    String deviceId = result.deviceId;
                    String deviceIdHash = result.deviceIdHash;
                    String uuid = result.uuid;
                    listener.invoke(deviceId, null);
                }
            }

            @Override
            public void onRequestError(Reason reason, Result result) {
                listener.invoke(null, reason.toString());
            }
        };
        AppMetrica.requestStartupParams(
            reactApplicationContext.getApplicationContext(),
            startupParamsCallback,
            Arrays.asList(StartupParamsCallback.APPMETRICA_DEVICE_ID_HASH)
        );
        // AppMetrica.requestAppMetricaDeviceID(new ReactNativeAppMetricaDeviceIDListener(listener));
    }

    @ReactMethod
    public void resumeSession() {
        AppMetrica.resumeSession(getCurrentActivity());
    }

    @ReactMethod
    public void sendEventsBuffer() {
        AppMetrica.sendEventsBuffer();
    }

    @ReactMethod
    public void setLocation(ReadableMap locationMap) {
        AppMetrica.setLocation(Utils.toLocation(locationMap));
    }

    @ReactMethod
    public void setLocationTracking(boolean enabled) {
        AppMetrica.setLocationTracking(enabled);
    }

    @ReactMethod
    public void setStatisticsSending(boolean enabled) {
        AppMetrica.setDataSendingEnabled(enabled);
    }

    @ReactMethod
    public void setUserProfileID(String userProfileID) {
        AppMetrica.setUserProfileID(userProfileID);
    }
    
    @ReactMethod
    public void reportUserProfile(String userProfileID, ReadableMap userProfileParam, Promise promise) {
        if(userProfileID == null) {
            promise.reject("-101", "UserProfileId can't be null");
        }

        setUserProfileID(userProfileID);

        if(userProfileParam != null) {
            UserProfile.Builder userProfileBuilder = UserProfile.newBuilder();
            ReadableMapKeySetIterator iterator = userProfileParam.keySetIterator();

            while (iterator.hasNextKey()) {
                String key = iterator.nextKey();

                switch (key) {
                    case "name": {
                        UserProfileUpdate name = Attribute.name().withValue(userProfileParam.getString(key));
                        userProfileBuilder.apply(name);
                        break;
                    }
                    case "gender": {
                        String genderProp = userProfileParam.getString(key);

                        if(genderProp.equalsIgnoreCase("male")) {
                            UserProfileUpdate gender = Attribute.gender().withValue(GenderAttribute.Gender.MALE);
                            userProfileBuilder.apply(gender);
                        } else if(genderProp.equalsIgnoreCase("female")) {
                            UserProfileUpdate gender = Attribute.gender().withValue(GenderAttribute.Gender.FEMALE);
                            userProfileBuilder.apply(gender);
                        } else {
                            UserProfileUpdate gender = Attribute.gender().withValue(GenderAttribute.Gender.OTHER);
                            userProfileBuilder.apply(gender);
                        }

                        break;
                    }
                    case "birthDate": {
                        UserProfileUpdate birthDate = Attribute.birthDate().withAge(userProfileParam.getInt(key));
                        userProfileBuilder.apply(birthDate);
                        break;
                    }
                    case "notificationsEnabled": {
                        UserProfileUpdate notificationsEnabled = Attribute.notificationsEnabled().withValue(userProfileParam.getBoolean(key));
                        userProfileBuilder.apply(notificationsEnabled);
                        break;
                    }
                    default: {
                        ReadableType keyType = userProfileParam.getType(key);
                        UserProfileUpdate customAttribute = null;

                        if(keyType == ReadableType.String) {
                            customAttribute = Attribute.customString(key).withValue(userProfileParam.getString(key));
                        } else if(keyType == ReadableType.Number) {
                            customAttribute = Attribute.customNumber(key).withValue(userProfileParam.getInt(key));
                        } else if(keyType == ReadableType.Boolean) {
                            customAttribute = Attribute.customBoolean(key).withValue(userProfileParam.getBoolean(key));
                        }

                        if(customAttribute != null) {
                            userProfileBuilder.apply(customAttribute);
                        }

                        break;
                    }
                }
            }

            UserProfile userProfile = userProfileBuilder.build();

            if(userProfile.getUserProfileUpdates().size() > 0) {
                AppMetrica.reportUserProfile(userProfile);

                promise.resolve(true);
            } else {
                promise.reject("-102", "Valid keys not found");
            }
        }
    }
}
