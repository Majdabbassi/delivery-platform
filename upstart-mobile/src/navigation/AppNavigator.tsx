import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { useAuth } from '../context/AuthContext';
import { UserRole } from '../types';

import LoginScreen from '../screens/auth/LoginScreen';
import RegisterScreen from '../screens/auth/RegisterScreen';
import CustomerDashboard from '../screens/dashboard/CustomerDashboard';
import VendorDashboard from '../screens/dashboard/VendorDashboard';
import DriverDashboard from '../screens/dashboard/DriverDashboard';
import AdminDashboard from '../screens/dashboard/AdminDashboard';

const Stack = createStackNavigator();

export default function AppNavigator() {
  const { isAuthenticated, role, isLoading } = useAuth();

  if (isLoading) {
    return null; // Show a splash screen or loading indicator here
  }

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: true }}>
        {!isAuthenticated ? (
          <>
            <Stack.Screen name="Login" component={LoginScreen} />
            <Stack.Screen name="Register" component={RegisterScreen} />
          </>
        ) : (
          <>
            {role === UserRole.CLIENT && (
              <Stack.Screen name="CustomerDashboard" component={CustomerDashboard} options={{ title: 'Home' }} />
            )}
            {role === UserRole.VENDOR_OWNER && (
              <Stack.Screen name="VendorDashboard" component={VendorDashboard} options={{ title: 'Vendor Portal' }} />
            )}
            {role === UserRole.DRIVER && (
              <Stack.Screen name="DriverDashboard" component={DriverDashboard} options={{ title: 'Driver Portal' }} />
            )}
            {(role === UserRole.SUPER_ADMIN || role === UserRole.DELIVERY_OWNER) && (
              <Stack.Screen name="AdminDashboard" component={AdminDashboard} options={{ title: 'Admin Panel' }} />
            )}
          </>
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
