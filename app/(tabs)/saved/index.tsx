import React, { useEffect, useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router } from 'expo-router'
import { Star, Trash2 } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import { getSavedRoutes, deleteSavedRoute, SavedRoute } from '../../../utils/savedRoutes'
import { metersToKm } from '../../../utils/format'

export default function SavedRoutesScreen() {
  const [routes, setRoutes] = useState<SavedRoute[]>([])

  useEffect(() => {
    getSavedRoutes().then(setRoutes)
  }, [])

  async function handleDelete(id: string) {
    Alert.alert('루트 삭제', '저장된 루트를 삭제할까요?', [
      { text: '취소', style: 'cancel' },
      {
        text: '삭제', style: 'destructive', onPress: async () => {
          await deleteSavedRoute(id)
          setRoutes(prev => prev.filter(r => r.id !== id))
        }
      },
    ])
  }

  function handleRun(route: SavedRoute) {
    router.push({
      pathname: '/running',
      params: {
        courseId: route.courseId.toString(),
        courseName: route.name,
        distance: route.distanceMeters.toString(),
        duration: route.estimatedDurationSeconds.toString(),
        points: JSON.stringify(route.points),
      },
    })
  }

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.titleRow}>
          <View>
            <Text style={styles.label}>SAVED ROUTES</Text>
            <Text style={styles.title}>저장한 루트</Text>
          </View>
          <View style={styles.countBadge}>
            <Text style={styles.countLabel}>총</Text>
            <Text style={styles.countValue}>{routes.length}</Text>
          </View>
        </View>

        {routes.length === 0 && (
          <Text style={styles.empty}>저장된 루트가 없어요{'\n'}달리기 후 루트를 저장해보세요 ⭐</Text>
        )}

        {routes.map((route) => (
          <View key={route.id} style={styles.card}>
            <View style={styles.cardHeader}>
              <View style={styles.nameRow}>
                <Star size={14} color={Colors.WARNING} fill={Colors.WARNING} />
                <Text style={styles.courseName}>{route.name}</Text>
              </View>
              <TouchableOpacity onPress={() => handleDelete(route.id)}>
                <Trash2 color={Colors.TEXT_SECONDARY} size={16} />
              </TouchableOpacity>
            </View>

            <View style={styles.statsRow}>
              {[
                { label: '거리', value: `${metersToKm(route.distanceMeters)}km` },
                { label: '예상 시간', value: `${Math.round(route.estimatedDurationSeconds / 60)}분` },
                { label: '저장일', value: new Date(route.savedAt).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' }) },
              ].map((s) => (
                <View key={s.label} style={styles.statItem}>
                  <Text style={styles.statLabel}>{s.label}</Text>
                  <Text style={styles.statValue}>{s.value}</Text>
                </View>
              ))}
            </View>

            <TouchableOpacity style={styles.runBtn} onPress={() => handleRun(route)}>
              <Text style={styles.runBtnText}>이 루트로 달리기</Text>
            </TouchableOpacity>
          </View>
        ))}
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  container: { padding: 16 },
  titleRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
  label: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  title: { fontSize: 16, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  countBadge: { backgroundColor: Colors.CARD, borderRadius: 12, paddingHorizontal: 10, paddingVertical: 4, alignItems: 'center' },
  countLabel: { fontSize: 9, color: Colors.TEXT_SECONDARY },
  countValue: { fontSize: 16, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  empty: { fontSize: 13, color: Colors.TEXT_SECONDARY, textAlign: 'center', marginTop: 40, lineHeight: 22 },
  card: { backgroundColor: Colors.CARD, borderWidth: 1, borderColor: Colors.BORDER, borderRadius: 12, padding: 16, gap: 12, marginBottom: 12 },
  cardHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 6, flex: 1 },
  courseName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  statsRow: { flexDirection: 'row', gap: 8 },
  statItem: { flex: 1, backgroundColor: Colors.SURFACE_DARK, borderRadius: 8, padding: 10, alignItems: 'center' },
  statLabel: { fontSize: 9, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  statValue: { fontSize: 12, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  runBtn: { height: 40, backgroundColor: Colors.PRIMARY, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  runBtnText: { fontSize: 12, fontWeight: '900', color: Colors.TEXT_WHITE },
})
