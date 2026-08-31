import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { theme } from '../theme';

const STATUS_CONFIG: Record<string, { label: string; bg: string; color: string }> = {
  PENDING:    { label: 'Pending',     bg: '#FEF3C7', color: '#92400E' },
  ASSIGNED:   { label: 'Assigned',    bg: '#DBEAFE', color: '#1E40AF' },
  CONFIRMED:  { label: 'Confirmed',   bg: '#DBEAFE', color: '#1E40AF' },
  IN_PROGRESS:{ label: 'In Progress', bg: '#EDE9FE', color: '#5B21B6' },
  PICKED_UP:  { label: 'Picked Up',   bg: '#EDE9FE', color: '#5B21B6' },
  IN_TRANSIT: { label: 'In Transit',  bg: '#DBEAFE', color: '#1E40AF' },
  DELIVERED:  { label: 'Delivered',   bg: '#D1FAE5', color: '#065F46' },
  COMPLETED:  { label: 'Completed',   bg: '#D1FAE5', color: '#065F46' },
  CANCELLED:  { label: 'Cancelled',   bg: '#FEE2E2', color: '#991B1B' },
  FAILED:     { label: 'Failed',      bg: '#FEE2E2', color: '#991B1B' },
};

export function StatusBadge({ status }: { status: string }) {
  const config = STATUS_CONFIG[status] ?? { label: status, bg: '#F3F4F6', color: '#374151' };
  return (
    <View style={[styles.badge, { backgroundColor: config.bg }]}>
      <Text style={[styles.text, { color: config.color }]}>{config.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { paddingHorizontal: 10, paddingVertical: 3, borderRadius: 20, alignSelf: 'flex-start' },
  text: { fontSize: 12, fontWeight: '600' },
});