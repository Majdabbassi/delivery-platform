import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.swiftdeliver.delivery',
  appName: 'SwiftDeliver Delivery',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
    url: process.env['CAPACITOR_SERVER_URL'],
    cleartext: process.env['CAPACITOR_SCHEME'] === 'http'
  },
  ios: {
    contentInsetAdjustmentBehavior: 'automatic'
  },
  android: {
    targetSdkVersion: 34
  },
  plugins: {
    LocalNotifications: {
      smallIcon: 'ic_stat_icon_config_sample',
      iconColor: '#dc2626'
    },
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert']
    },
    SplashScreen: {
      launchShowDuration: 0
    }
  }
};

export default config;
