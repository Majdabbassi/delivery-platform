import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View, Text, StyleSheet, SafeAreaView, FlatList,
  TouchableOpacity, ActivityIndicator, Dimensions
} from 'react-native';
import MapView, { Marker, PROVIDER_GOOGLE } from 'react-native-maps';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { orderApi, trackingApi } from '../../api';
import { Order, OrderStatus } from '../../types';
import { theme } from '../../theme';
import { StatusBadge } from '../../components/StatusBadge';
import { Card } from '../../components/Card';
import { PrimaryButton } from '../../components/PrimaryButton';

const { height } = Dimensions.get('window');

const ACTIVE_STATUSES = [
  OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS,
  OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT
];

export default function CustomerDashboard() {
  const { logout } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [trackingOrder, setTrackingOrder] = useState<Order | null>(null);
  const [driverLocation, setDriverLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const mapRef = useRef<MapView>(null);

  const fetchOrders = useCallback(async () => {
    try {
      const data = await orderApi.getMyOrders();
      setOrders(data);
      const active = data.find(o => ACTIVE_STATUSES.includes(o.status));
      if (active) setTrackingOrder(active);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  // Poll driver location for active order
  useEffect(() => {
    if (pollingRef.current) clearInterval(pollingRef.current);
    if (!trackingOrder) return;
    const poll = async () => {
      try {
        const loc = await trackingApi.getOrderLocation(trackingOrder.id);
        if (loc?.latitude && loc?.longitude) {
          const pos = { latitude: loc.latitude, longitude: loc.longitude };
          setDriverLocation(pos);
          mapRef.current?.animateToRegion({ ...pos, latitudeDelta: 0.02, longitudeDelta: 0.02 }, 800);
        }
      } catch {}
    };
    poll();
    pollingRef.current = setInterval(poll, 5000);
    return () => { if (pollingRef.current) clearInterval(pollingRef.current); };
  }, [trackingOrder?.id]);

  const activeOrder = orders.find(o => ACTIVE_STATUSES.includes(o.status));
  const otherOrders = orders.filter(o => !ACTIVE_STATUSES.includes(o.status));

  const renderOrder = ({ item }: { item: Order }) => (
    <Card style={styles.orderCard}>
      <View style={styles.orderHeader}>
        <Text style={styles.orderNum}>#{item.orderNumber}</Text>
        <StatusBadge status={item.status} />
      </View>
      <View style={styles.addressRow}>
        <Ionicons name="location-outline" size={14} color={theme.colors.textMuted} />
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
          <Text style={styles.greeting}>Track your delivery</Text>
          <Text style={styles.headerTitle}>{orders.length} order{orders.length !== 1 ? 's' : ''}</Text>
        </View>
        <TouchableOpacity onPress={logout} style={styles.logoutBtn}>
          <Ionicons name="log-out-outline" size={22} color={theme.colors.textSecondary} />
        </TouchableOpacity>
      </View>

      {loading ? (
        <ActivityIndicator color={theme.colors.primary} style={{ marginTop: 48 }} />
      ) : (
        <FlatList
          data={otherOrders}
          keyExtractor={o => String(o.id)}
          renderItem={renderOrder}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            activeOrder ? (
              <View style={styles.activeSection}>
                <Text style={styles.sectionLabel}>Live tracking</Text>
                <Card style={styles.activeCard}>
                  <View style={styles.orderHeader}>
                    <Text style={styles.orderNum}>#{activeOrder.orderNumber}</Text>
                    <StatusBadge status={activeOrder.status} />
                  </View>
                  {/* Map */}
                  <View style={styles.mapWrap}>
                    <MapView
                      ref={mapRef}
                      style={styles.map}
                      provider={PROVIDER_GOOGLE}
                      initialRegion={
                        activeOrder.deliveryLatitude && activeOrder.deliveryLongitude
                          ? { latitude: activeOrder.deliveryLatitude, longitude: activeOrder.deliveryLongitude, latitudeDelta: 0.04, longitudeDelta: 0.04 }
                          : { latitude: 36.8, longitude: 10.18, latitudeDelta: 0.1, longitudeDelta: 0.1 }
                      }
                    >
                      {driverLocation && (
                        <Marker coordinate={driverLocation} title="Driver">
                          <View style={styles.driverMarker}>
                            <Ionicons name="car-sport" size={16} color="#fff" />
                          </View>
                        </Marker>
                      )}
                      {activeOrder.deliveryLatitude && activeOrder.deliveryLongitude && (
                        <Marker
                          coordinate={{ latitude: activeOrder.deliveryLatitude, longitude: activeOrder.deliveryLongitude }}
                          title="Your location"
                          pinColor={theme.colors.danger}
                        />
                      )}
                    </MapView>
                  </View>
                  {/* Status bar */}
                  <View style={styles.statusBar}>
                    <Ionicons name="time-outline" size={14} color={theme.colors.accent} />
                    <Text style={styles.statusBarText}>
                      {driverLocation ? 'Driver is on the way · Tracking live' : 'Waiting for driver location...'}
                    </Text>
                  </View>
                </Card>
                {otherOrders.length > 0 && <Text style={[styles.sectionLabel, { marginTop: 20 }]}>Past orders</Text>}
              </View>
            ) : null
          }
          ListEmptyComponent={
            !activeOrder ? (
              <View style={styles.empty}>
                <Ionicons name="bag-outline" size={48} color={theme.colors.textMuted} />
                <Text style={styles.emptyText}>No orders yet</Text>
              </View>
            ) : null
          }
        />
      )}
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
  list: { padding: theme.spacing.md, paddingBottom: 48 },
  activeSection: { marginBottom: theme.spacing.md },
  sectionLabel: { fontSize: 12, fontWeight: '600', color: theme.colors.textMuted, textTransform: 'uppercase', letterSpacing: 0.8, marginBottom: 10 },
  activeCard: { padding: 0, overflow: 'hidden' },
  orderHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: theme.spacing.md, paddingBottom: 8 },
  orderNum: { fontSize: 14, fontWeight: '700', color: theme.colors.textPrimary },
  mapWrap: { height: 200, borderTopWidth: 0.5, borderBottomWidth: 0.5, borderColor: theme.colors.border },
  map: { flex: 1 },
  statusBar: {
    flexDirection: 'row', alignItems: 'center', gap: 6,
    padding: theme.spacing.sm, paddingHorizontal: theme.spacing.md,
    backgroundColor: '#FFFBEB',
  },
  statusBarText: { fontSize: 13, color: theme.colors.textSecondary },
  driverMarker: {
    backgroundColor: theme.colors.primary, width: 32, height: 32,
    borderRadius: 16, alignItems: 'center', justifyContent: 'center',
    borderWidth: 2, borderColor: '#fff',
  },
  orderCard: { marginBottom: theme.spacing.sm },
  addressRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 4 },
  address: { flex: 1, fontSize: 13, color: theme.colors.textSecondary },
  orderFooter: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 10 },
  amount: { fontSize: 14, fontWeight: '700', color: theme.colors.textPrimary },
  date: { fontSize: 12, color: theme.colors.textMuted },
  empty: { alignItems: 'center', marginTop: 80 },
  emptyText: { fontSize: 15, color: theme.colors.textMuted, marginTop: 12 },
});