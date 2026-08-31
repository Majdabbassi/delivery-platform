import React, { useEffect, useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  ActivityIndicator,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { UserRole } from '../../types';
import {
  dashboardApi,
  DashboardOverview,
  StatsMap,
  roleLabel,
} from '../../api';
import { theme } from '../../theme';
import { Card } from '../../components/Card';

type StatCardProps = { label: string; value: number | string | null | undefined };
const StatCard = ({ label, value }: StatCardProps) => (
  <Card style={styles.card}>
    <Text style={styles.cardValue}>{value ?? '—'}</Text>
    <Text style={styles.cardLabel}>{label}</Text>
  </Card>
);

export default function AdminDashboard() {
  const { logout, user, role } = useAuth();
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [superAdminStats, setSuperAdminStats] = useState<StatsMap | null>(null);
  const [customerStats, setCustomerStats] = useState<StatsMap | null>(null);
  const [driverStats, setDriverStats] = useState<StatsMap | null>(null);
  const [productStats, setProductStats] = useState<StatsMap | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setError(null);
    try {
      setOverview(await dashboardApi.getOverview());
      if (role === UserRole.SUPER_ADMIN) {
        const [sa, cu, dr, pr] = await Promise.all([
          dashboardApi.getSuperAdminStats(),
          dashboardApi.getCustomerUserStats(),
          dashboardApi.getDriverStats(),
          dashboardApi.getProductStats(),
        ]);
        setSuperAdminStats(sa);
        setCustomerStats(cu);
        setDriverStats(dr);
        setProductStats(pr);
      }
    } catch (err) {
      console.error('Dashboard load error:', err);
      setError('Could not load dashboard data.');
    }
  }, [role]);

  useEffect(() => {
    loadData().finally(() => setLoading(false));
  }, [loadData]);

  const onRefresh = async () => {
    setRefreshing(true);
    await loadData();
    setRefreshing(false);
  };

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Admin Panel</Text>
          <Text style={styles.welcome}>
            Welcome, {user?.firstName} {user?.lastName} · {user ? roleLabel(user.role) : ''}
          </Text>
        </View>
        <TouchableOpacity onPress={logout} style={styles.logoutBtn}>
          <Ionicons name="log-out-outline" size={22} color={theme.colors.textSecondary} />
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={theme.colors.primary} />
        }
      >
        {error ? (
          <Text style={styles.error}>{error}</Text>
        ) : overview ? (
          <>
            <Text style={styles.section}>Business Overview</Text>
            <View style={styles.grid}>
              <StatCard label="Customers" value={overview.customers} />
              <StatCard label="Total Orders" value={overview.orders} />
              <StatCard label="Revenue" value={overview.totalRevenue} />
              <StatCard label="Vendor Companies" value={overview.vendorCompanies} />
              <StatCard label="Vendor Owners" value={overview.vendorOwners} />
              <StatCard label="Delivery Companies" value={overview.deliveryCompanies} />
              <StatCard label="Delivery Owners" value={overview.deliveryOwners} />
              <StatCard label="Drivers" value={overview.drivers} />
              <StatCard label="Products" value={overview.products} />
              <StatCard label="Admins" value={overview.admins} />
              <StatCard label="Partnerships" value={overview.partnerships} />
            </View>

            <Text style={styles.section}>Order Flow</Text>
            <View style={styles.grid}>
              <StatCard label="Pending" value={overview.pendingOrders} />
              <StatCard label="In Progress" value={overview.inProgressOrders} />
              <StatCard label="Completed" value={overview.completedOrders} />
              <StatCard label="Cancelled" value={overview.cancelledOrders} />
            </View>

            {role === UserRole.SUPER_ADMIN && (
              <>
                <Text style={styles.section}>Live Metrics</Text>
                <View style={styles.grid}>
                  <StatCard label="Active Customers" value={customerStats?.active} />
                  <StatCard label="Verified Customers" value={customerStats?.verified} />
                  <StatCard label="Active & Verified" value={customerStats?.activeAndVerified} />
                  <StatCard label="Drive-Enabled" value={driverStats?.active} />
                  <StatCard label="Verified Drivers" value={driverStats?.verified} />
                  <StatCard label="Products Available" value={productStats?.total} />
                  <StatCard label="Active Admins" value={superAdminStats?.active} />
                  <StatCard label="System Access" value={superAdminStats?.totalSystemAccess} />
                </View>
              </>
            )}
          </>
        ) : (
          <Text style={styles.info}>
            Manage users, platforms, partnerships, and overall activities.
          </Text>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: theme.colors.bg },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: theme.spacing.md, paddingVertical: theme.spacing.md,
    backgroundColor: theme.colors.surface,
    borderBottomWidth: 0.5, borderBottomColor: theme.colors.border,
  },
  logoutBtn: { padding: 8 },
  content: { padding: theme.spacing.md, paddingBottom: 48 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: theme.colors.bg },
  title: { fontSize: 20, fontWeight: '700', color: theme.colors.textPrimary },
  welcome: { fontSize: 12, color: theme.colors.textMuted, marginTop: 2 },
  section: { fontSize: 13, fontWeight: '600', marginTop: theme.spacing.md, marginBottom: theme.spacing.sm, color: theme.colors.textSecondary, textTransform: 'uppercase', letterSpacing: 0.6 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' },
  card: { width: '48%', marginBottom: theme.spacing.sm },
  cardValue: { fontSize: 22, fontWeight: '800', color: theme.colors.textPrimary },
  cardLabel: { fontSize: 12, color: theme.colors.textMuted, marginTop: 4 },
  error: { color: theme.colors.danger, marginTop: theme.spacing.md, textAlign: 'center' },
  info: { fontSize: 15, color: theme.colors.textSecondary, textAlign: 'center', marginVertical: theme.spacing.lg },
});