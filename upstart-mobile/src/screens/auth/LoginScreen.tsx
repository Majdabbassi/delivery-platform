import React, { useState } from 'react';
import {
  View, Text, StyleSheet, SafeAreaView,
  KeyboardAvoidingView, Platform, ScrollView, Alert
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { theme } from '../../theme';
import { StyledInput } from '../../components/StyledInput';
import { PrimaryButton } from '../../components/PrimaryButton';

export default function LoginScreen({ navigation }: any) {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async () => {
    if (!email || !password) { setError('Please fill in all fields.'); return; }
    setLoading(true);
    setError('');
    try {
      await login(email, password);
    } catch {
      setError('Invalid credentials. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.logo}>
            <View style={styles.logoIcon}>
              <Ionicons name="car-sport" size={32} color="#fff" />
            </View>
            <Text style={styles.appName}>SwiftDeliver</Text>
            <Text style={styles.tagline}>Delivery infrastructure for modern businesses</Text>
          </View>

          <View style={styles.card}>
            <Text style={styles.title}>Sign in</Text>
            {!!error && <View style={styles.errorBanner}><Text style={styles.errorText}>{error}</Text></View>}
            <StyledInput
              label="Email"
              placeholder="you@company.com"
              keyboardType="email-address"
              autoCapitalize="none"
              value={email}
              onChangeText={setEmail}
            />
            <StyledInput
              label="Password"
              placeholder="••••••••"
              isPassword
              value={password}
              onChangeText={setPassword}
            />
            <PrimaryButton label="Sign in" onPress={handleLogin} loading={loading} style={{ marginTop: 8 }} />
          </View>

          <PrimaryButton
            label="Don't have an account? Register"
            onPress={() => navigation.navigate('Register')}
            variant="ghost"
            style={{ marginTop: 8 }}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: theme.colors.bg },
  scroll: { flexGrow: 1, justifyContent: 'center', padding: theme.spacing.lg },
  logo: { alignItems: 'center', marginBottom: theme.spacing.xl },
  logoIcon: {
    width: 64, height: 64, borderRadius: 20,
    backgroundColor: theme.colors.primary,
    alignItems: 'center', justifyContent: 'center',
    marginBottom: theme.spacing.sm,
    ...theme.shadow.md,
  },
  appName: { fontSize: 28, fontWeight: '700', color: theme.colors.textPrimary },
  tagline: { fontSize: 13, color: theme.colors.textMuted, marginTop: 4, textAlign: 'center' },
  card: {
    backgroundColor: theme.colors.surface,
    borderRadius: theme.radius.lg,
    padding: theme.spacing.lg,
    borderWidth: 0.5,
    borderColor: theme.colors.border,
    ...theme.shadow.sm,
  },
  title: { fontSize: 20, fontWeight: '700', color: theme.colors.textPrimary, marginBottom: theme.spacing.md },
  errorBanner: {
    backgroundColor: '#FEE2E2', borderRadius: theme.radius.sm,
    padding: theme.spacing.sm, marginBottom: theme.spacing.md,
  },
  errorText: { color: theme.colors.danger, fontSize: 13 },
});