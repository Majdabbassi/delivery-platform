import React, { useState } from 'react';
import {
  View, Text, StyleSheet, Alert, ScrollView,
  SafeAreaView, KeyboardAvoidingView, Platform
} from 'react-native';
import { useAuth } from '../../context/AuthContext';
import { useNavigation } from '@react-navigation/native';
import { theme } from '../../theme';
import { StyledInput } from '../../components/StyledInput';
import { PrimaryButton } from '../../components/PrimaryButton';

export default function RegisterScreen() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigation = useNavigation<any>();

  const handleRegister = async () => {
    if (!username || !email || !password || !firstName || !lastName) {
      Alert.alert('Error', 'Please fill in all fields');
      return;
    }
    setLoading(true);
    try {
      await register({ username, email, password, firstName, lastName });
      Alert.alert('Success', 'Account created! Please login.');
      navigation.navigate('Login');
    } catch (error) {
      Alert.alert('Registration Failed', 'Could not create account.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={styles.safe}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={{ flex: 1 }}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <Text style={styles.title}>Create account</Text>
          <Text style={styles.subtitle}>Start with SwiftDeliver in minutes</Text>

          <View style={styles.card}>
            <StyledInput label="Username" placeholder="johndoe" autoCapitalize="none" value={username} onChangeText={setUsername} />
            <StyledInput label="Email" placeholder="you@company.com" keyboardType="email-address" autoCapitalize="none" value={email} onChangeText={setEmail} />
            <StyledInput label="Password" placeholder="••••••••" isPassword value={password} onChangeText={setPassword} />
            <StyledInput label="First Name" placeholder="John" value={firstName} onChangeText={setFirstName} />
            <StyledInput label="Last Name" placeholder="Doe" value={lastName} onChangeText={setLastName} />

            <PrimaryButton label="Create account" onPress={handleRegister} loading={loading} style={{ marginTop: 8 }} />
          </View>

          <PrimaryButton
            label="Already have an account? Sign in"
            onPress={() => navigation.navigate('Login')}
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
  title: { fontSize: 24, fontWeight: '700', color: theme.colors.textPrimary, textAlign: 'center' },
  subtitle: { fontSize: 13, color: theme.colors.textMuted, textAlign: 'center', marginTop: 4, marginBottom: theme.spacing.lg },
  card: {
    backgroundColor: theme.colors.surface,
    borderRadius: theme.radius.lg,
    padding: theme.spacing.lg,
    borderWidth: 0.5,
    borderColor: theme.colors.border,
    ...theme.shadow.sm,
  },
});