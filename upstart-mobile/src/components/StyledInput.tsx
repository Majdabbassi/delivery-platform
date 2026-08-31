import React, { useState } from 'react';
import { View, Text, TextInput, StyleSheet, TextInputProps, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';

interface Props extends TextInputProps {
  label: string;
  error?: string;
  isPassword?: boolean;
}

export function StyledInput({ label, error, isPassword, ...props }: Props) {
  const [focused, setFocused] = useState(false);
  const [show, setShow] = useState(false);

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>{label}</Text>
      <View style={[styles.inputWrap, focused && styles.focused, !!error && styles.errored]}>
        <TextInput
          style={styles.input}
          placeholderTextColor={theme.colors.textMuted}
          secureTextEntry={isPassword && !show}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          {...props}
        />
        {isPassword && (
          <TouchableOpacity onPress={() => setShow(v => !v)} style={styles.eye}>
            <Ionicons name={show ? 'eye-off' : 'eye'} size={18} color={theme.colors.textMuted} />
          </TouchableOpacity>
        )}
      </View>
      {!!error && <Text style={styles.error}>{error}</Text>}
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { marginBottom: theme.spacing.md },
  label: { fontSize: 13, fontWeight: '600', color: theme.colors.textSecondary, marginBottom: 6 },
  inputWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: theme.colors.border,
    borderRadius: theme.radius.sm,
    backgroundColor: theme.colors.surface,
    paddingHorizontal: theme.spacing.sm,
  },
  focused: { borderColor: theme.colors.primary },
  errored: { borderColor: theme.colors.danger },
  input: { flex: 1, paddingVertical: 12, fontSize: 15, color: theme.colors.textPrimary },
  eye: { padding: 6 },
  error: { fontSize: 12, color: theme.colors.danger, marginTop: 4 },
});