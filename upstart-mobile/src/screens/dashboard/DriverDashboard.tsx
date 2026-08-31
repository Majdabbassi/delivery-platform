import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View, Text, StyleSheet, SafeAreaView, FlatList,
  TouchableOpacity, ActivityIndicator, Dimensions, Animated
} from 'react-native';
import MapView, { Marker, PROVIDER_GOOGLE } from 'react-native-maps';
import * as Location from 'expo-location';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { orderApi, trackingApi } from '../../api';
import { Order, OrderStatus } from '../../types';
import { subscribeOrderRealtime } from '../../realtime';
import { theme } from '../../theme';
import { StatusBadge } from '../../components/StatusBadge';
import { Card } from '../../components/Card';
import { PrimaryButton } from '../../components/PrimaryButton';

const { height } = Dimensions.get('window');

const NEXT_STATUS: Partial<Record<OrderStatus, { status: OrderStatus; label: string }>> = {
  [OrderStatus.ASSIGNED]:    { status: OrderStatus.IN_PROGRESS, label: 'Start Pickup' },
  [OrderStatus.IN_PROGRESS]: { status: OrderStatus.PICKED_UP,   label: 'Mark Picked Up' },
  [OrderStatus.PICKED_UP]:   { status: OrderStatus.IN_TRANSIT,  label: 'Start Transit' },
  [OrderStatus.IN_TRANSIT]:  { status: OrderStatus.DELIVERED,   label: 'Mark Delivered' },
};

export default function DriverDashboard() {
  const { logout, user } = useAuth();
  const mapRef = useRef<MapView>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [driverLocation, setDriverLocation] = useState<{ latitude: number; longitude: number } | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const subs = useRef<Array<() => void>>([]);

  // Request location and start watching
  useEffect(() => {
    let watcher: Location.LocationSubscription | null = null;
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      const loc = await Location.getCurrentPositionAsync({});
      setDriverLocation({ latitude: loc.coords.latitude, longitude: loc.coords.longitude });
      watcher = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, timeInterval: 5000, distanceInterval: 10 },
        (loc) => {
          const pos = { latitude: loc.coords.latitude, longitude: loc.coords.longitude };
          setDriverLocation(pos);
          // Send to backend for active order
          if (selectedOrder) {
            trackingApi.updateLocation(selectedOrder.id, pos).catch(() => {});
          }
        }
      );
    })();
    return () => { watcher?.remove(); };
  }, [selectedOrder]);

  const fetchOrders = useCallback(async () => {
    try {
      const data = await orderApi.getMyOrders();
      setOrders(data);
      if (data.length > 0 && !selectedOrder) setSelectedOrder(data[0]);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  // Subscribe to realtime updates
  useEffect(() => {
    subs.current.forEach(u => u());
    subs.current = [];
    orders.forEach(order => {
      const unsub = subscribeOrderRealtime(order.id, (event) => {
        if (event.status) {
          setOrders(prev => prev.map(o => o.id === event.orderId ? { ...o, status: event.status! } : o));
        }
      });
      if (unsub) subs.current.push(unsub);
    });
    return () => { subs.current.forEach(u => u()); };
  }, [orders.length]);

  const handleUpdateStatus = async (order: Order) => {
    const next = NEXT_STATUS[order.status];
    if (!next) return;
    setUpdatingId(order.id);
    try {
      await orderApi.updateOrderStatus(order.id, next.status);
      setOrders(prev => prev.map(o => o.id === order.id ? { ...o, status: next.status } : o));
    } catch (e) {
      console.error(e);
    } finally {
      setUpdatingId(null);
    }
  };

  const activeOrders = orders.filter(o =>
    [OrderStatus.ASSIGNED, OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT].includes(o.status)
  );

  const renderOrder = ({ item }: { item: Order }) => {
    const next = NEXT_STATUS[item.status];
    const isSelected = selectedOrder?.id === item.id;
    return (
      <TouchableOpacity onPress={() => setSelectedOrder(item)} activeOpacity={0.8}>
        <Card style={[styles.orderCard, isSelected ? styles.selectedCard : undefined]}>
          <View style={styles.orderHeader}>
            <Text style={styles.orderNum}>#{item.orderNumber}</Text>
            <StatusBadge status={item.status} />
          </View>
          <View style={styles.addressRow}>
            <Ionicons name="radio-button-on" size={14} color={theme.colors.info} />
            <Text style={styles.address} numberOfLines={1}>{item.pickupAddress}</Text>
          </View>
          <View style={styles.addressRow}>
            <Ionicons name="location" size={14} color={theme.colors.danger} />
            <Text style={styles.address} numberOfLines={1}>{item.deliveryAddress}</Text>
          </View>
          {next && (
            <PrimaryButton
              label={updatingId === item.id ? '' : next.label}
              loading={updatingId === item.id}
              onPress={() => handleUpdateStatus(item)}
              style={styles.updateBtn}
            />
          )}
        </Card>
      </TouchableOpacity>
    );
  };

  return (
    <View style={styles.container}>
      {/* Map */}
      <MapView
        ref={mapRef}
        style={styles.map}
        provider={PROVIDER_GOOGLE}
        showsUserLocation={false}
        initialRegion={driverLocation ? {
          ...driverLocation, latitudeDelta: 0.05, longitudeDelta: 0.05
        } : { latitude: 36.8, longitude: 10.18, latitudeDelta: 0.1, longitudeDelta: 0.1 }}
      >
        {/* Driver marker */}
        {driverLocation && (
          <Marker coordinate={driverLocation} title="You">
            <View style={styles.driverMarker}>
              <Ionicons name="car-sport" size={18} color="#fff" />
            </View>
          </Marker>
        )}
        {/* Order markers */}
        {selectedOrder?.pickupLatitude && selectedOrder?.pickupLongitude && (
          <Marker
            coordinate={{ latitude: selectedOrder.pickupLatitude, longitude: selectedOrder.pickupLongitude }}
            title="Pickup"
            pinColor={theme.colors.info}
          />
        )}
        {selectedOrder?.deliveryLatitude && selectedOrder?.deliveryLongitude && (
          <Marker
            coordinate={{ latitude: selectedOrder.deliveryLatitude, longitude: selectedOrder.deliveryLongitude }}
            title="Delivery"
            pinColor={theme.colors.danger}
          />
        )}
      </MapView>

      {/* Header overlay */}
      <SafeAreaView style={styles.headerOverlay} pointerEvents="box-none">
        <View style={styles.header}>
          <View>
            <Text style={styles.greeting}>Good morning</Text>
            <Text style={styles.headerTitle}>{activeOrders.length} active order{activeOrders.length !== 1 ? 's' : ''}</Text>
          </View>
          <TouchableOpacity onPress={logout} style={styles.logoutBtn}>
            <Ionicons name="log-out-outline" size={22} color={theme.colors.textSecondary} />
          </TouchableOpacity>
        </View>
      </SafeAreaView>

      {/* Bottom sheet */}
      <View style={styles.sheet}>
        <View style={styles.sheetHandle} />
        <Text style={styles.sheetTitle}>Your Orders</Text>
        {loading ? (
          <ActivityIndicator color={theme.colors.primary} style={{ marginTop: 32 }} />
        ) : activeOrders.length === 0 ? (
          <View style={styles.empty}>
            <Ionicons name="checkmark-circle-outline" size={48} color={theme.colors.textMuted} />
            <Text style={styles.emptyText}>No active orders</Text>
          </View>
        ) : (
          <FlatList
            data={activeOrders}
            keyExtractor={o => String(o.id)}
            renderItem={renderOrder}
            contentContainerStyle={{ paddingBottom: 32 }}
            showsVerticalScrollIndicator={false}
          />
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  map: { height: height * 0.55 },
  headerOverlay: { position: 'absolute', top: 0, left: 0, right: 0 },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    margin: theme.spacing.md,
    backgroundColor: theme.colors.surface,
    borderRadius: theme.radius.lg,
    padding: theme.spacing.md,
    ...theme.shadow.md,
  },
  greeting: { fontSize: 12, color: theme.colors.textMuted },
  headerTitle: { fontSize: 16, fontWeight: '700', color: theme.colors.textPrimary },
  logoutBtn: { padding: 8 },
  sheet: {
    flex: 1,
    backgroundColor: theme.colors.bg,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    marginTop: -24,
    paddingTop: theme.spacing.sm,
    paddingHorizontal: theme.spacing.md,
    ...theme.shadow.md,
  },
  sheetHandle: {
    width: 40, height: 4, borderRadius: 2,
    backgroundColor: theme.colors.border,
    alignSelf: 'center', marginBottom: theme.spacing.md,
  },
  sheetTitle: { fontSize: 17, fontWeight: '700', color: theme.colors.textPrimary, marginBottom: theme.spacing.sm },
  orderCard: { marginBottom: theme.spacing.sm },
  selectedCard: { borderColor: theme.colors.primary, borderWidth: 1.5 },
  orderHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  orderNum: { fontSize: 14, fontWeight: '700', color: theme.colors.textPrimary },
  addressRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 4 },
  address: { flex: 1, fontSize: 13, color: theme.colors.textSecondary },
  updateBtn: { marginTop: 10, paddingVertical: 10 },
  driverMarker: {
    backgroundColor: theme.colors.primary, width: 36, height: 36,
    borderRadius: 18, alignItems: 'center', justifyContent: 'center',
    borderWidth: 2, borderColor: '#fff', ...theme.shadow.md,
  },
  empty: { alignItems: 'center', marginTop: 48 },
  emptyText: { fontSize: 15, color: theme.colors.textMuted, marginTop: 12 },
});