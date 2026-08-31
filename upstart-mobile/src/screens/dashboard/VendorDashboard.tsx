import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, StyleSheet, SafeAreaView,
  FlatList, TouchableOpacity, ActivityIndicator
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { orderApi } from '../../api';
import { Order, OrderStatus } from '../../types';
import { theme } from '../../theme';
import { StatusBadge } from '../../components/StatusBadge';
import { Card } from '../../components/Card';

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <Card style={styles.statCard}>
      <Text style={[styles.statValue, { color }]}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </Card>
  );
}

export default function VendorDashboard() {
  const { logout } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchOrders = useCallback(async () => {
    try {
      const data = await orderApi.getMyOrders();
      setOrders(data);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  const pending   = orders.filter(o => o.status === OrderStatus.PENDING).length;
  const active    = orders.filter(o => [OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.IN_TRANSIT].includes(o.status)).length;
  const completed = orders.filter(o => o.status === OrderStatus.COMPLETED).length;

  const renderOrder = ({ item }: { item: Order }) => (
    <Card style={styles.orderCard}>
      <View style={styles.orderHeader}>
        <Text style={styles.orderNum}>#{item.orderNumber}</Text>
        <StatusBadge status={item.status} />
      </View>
      <View style={styles.addressRow}>
        <Ionicons name="navigate-outline" size={13} color={theme.colors.textMuted} />
        <Text style={styles.address} numberOfLines={1}>{item.deliveryAddress}</Text>
      </View>
      <View style={styles.orderFooter}>
        <Text style={styles.amount}>{item.totalAmount} TND</Text>
        <Text style={styles.date}>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : ''}</Text>
      </View>
    </Card>
  );

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <View>
          <Text style={styles.greeting}>Vendor dashboard</Text>
          <Text style={styles.headerTitle}>{orders.length} total orders</Text>
        </View>
        <TouchableOpacity onPress={logout} style={styles.logoutBtn}>
          <Ionicons name="log-out-outline" size={22} color={theme.colors.textSecondary} />
        </TouchableOpacity>
      </View>

      {loading ? <ActivityIndicator color={theme.colors.primary} style={{ marginTop: 48 }} /> : (
        <FlatList
          data={orders}
          keyExtractor={o => String(o.id)}
          renderItem={renderOrder}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            <View style={styles.statsRow}>
              <StatCard label="Pending"   value={pending}   color={theme.colors.warning} />
              <StatCard label="Active"    value={active}    color={theme.colors.info} />
              <StatCard label="Completed" value={completed} color={theme.colors.success} />
            </View>
          }
          ListEmptyComponent={
            <View style={styles.empty}>
              <Ionicons name="clipboard-outline" size={48} color={theme.colors.textMuted} />
              <Text style={styles.emptyText}>No orders yet</Text>
            </View>
          }
        />
      )}

      {/* FAB */}
      <TouchableOpacity style={styles.fab} activeOpacity={0.85}>
        <Ionicons name="add" size={28} color="#fff" />
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: theme.colors.bg },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: theme.spacing.md, paddingVertical: theme.spacing.md,
    backgroundColor: theme.colors.surface,
    borderBottomWidth: 0.5, borderBottomColor: theme.colors.border,
  },
  greeting: { fontSize: 12, color: theme.colors.textMuted },
  headerTitle: { fontSize: 18, fontWeight: '700', color: theme.colors.textPrimary },
  logoutBtn: { padding: 8 },
  list: { padding: theme.spacing.md, paddingBottom: 80 },
  statsRow: { flexDirection: 'row', gap: 10, marginBottom: theme.spacing.md },
  statCard: { flex: 1, alignItems: 'center', paddingVertical: theme.spacing.md },
  statValue: { fontSize: 26, fontWeight: '800' },
  statLabel: { fontSize: 11, color: theme.colors.textMuted, marginTop: 2, fontWeight: '500' },
  orderCard: { marginBottom: theme.spacing.sm },
  orderHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  orderNum: { fontSize: 14, fontWeight: '700', color: theme.colors.textPrimary },
  addressRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  address: { flex: 1, fontSize: 13, color: theme.colors.textSecondary },
  orderFooter: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
  amount: { fontSize: 14, fontWeight: '700', color: theme.colors.textPrimary },
  date: { fontSize: 12, color: theme.colors.textMuted },
  empty: { alignItems: 'center', marginTop: 80 },
  emptyText: { fontSize: 15, color: theme.colors.textMuted, marginTop: 12 },
  fab: {
    position: 'absolute', bottom: 32, right: 24,
    width: 56, height: 56, borderRadius: 28,
    backgroundColor: theme.colors.primary,
    alignItems: 'center', justifyContent: 'center',
    ...theme.shadow.md,
  },
});