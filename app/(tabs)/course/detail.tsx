import React, { useMemo, useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router, useLocalSearchParams } from 'expo-router'
import { ArrowLeft } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import RouteMapView from '../../../components/RouteMapView'
import { metersToKm, secondsToMinutes } from '../../../utils/format'
import { RouteOption } from '../../../utils/api'

export default function CourseDetailScreen() {
  const { routes: routesJson, selectedIndex } = useLocalSearchParams<{
    routes: string
    selectedIndex: string
  }>()

  const allRoutes = useMemo<RouteOption[]>(() => {
    try { return JSON.parse(routesJson ?? '[]') } catch { return [] }
  }, [routesJson])

  const [activeIdx, setActiveIdx] = useState(Number(selectedIndex ?? 0))
  const route = allRoutes[activeIdx]

  function handleStart() {
    if (!route) return
    router.push({
      pathname: '/running',
      params: {
        courseId: route.courseId.toString(),
        courseName: route.courseName,
        distance: route.totalDistanceMeters.toString(),
        duration: route.estimatedDurationSeconds.toString(),
        points: JSON.stringify(route.points),
      },
    })
  }

  return (
    <SafeAreaView style={styles.safe}>
      {/* 헤더 */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <ArrowLeft size={20} color={Colors.TEXT_PRIMARY} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>코스 미리보기</Text>
        <View style={{ width: 36 }} />
      </View>

      {/* 루트 탭 */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.tabBar} contentContainerStyle={styles.tabContent}>
        {allRoutes.map((r, i) => (
          <TouchableOpacity
            key={r.courseId}
            style={[styles.tab, activeIdx === i && styles.tabActive]}
            onPress={() => setActiveIdx(i)}
          >
            <Text style={[styles.tabText, activeIdx === i && styles.tabTextActive]}>
              루트 {i + 1}
            </Text>
            <Text style={[styles.tabDist, activeIdx === i && styles.tabDistActive]}>
              {metersToKm(r.totalDistanceMeters)}km
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {/* 선택된 루트 정보 */}
      {route && (
        <View style={styles.routeInfo}>
          <Text style={styles.routeName}>{route.courseName}</Text>
          <Text style={styles.routeMeta}>
            {metersToKm(route.totalDistanceMeters)}km · 약 {secondsToMinutes(route.estimatedDurationSeconds)}분
          </Text>
        </View>
      )}

      {/* 지도 */}
      {route && <RouteMapView key={activeIdx} points={route.points} flex />}

      {/* 하단 출발 버튼 */}
      <View style={styles.footer}>
        <TouchableOpacity style={styles.startBtn} onPress={handleStart}>
          <Text style={styles.startBtnText}>출발</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },

  header: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    paddingHorizontal: 12, paddingVertical: 10,
    backgroundColor: Colors.CARD,
    borderBottomWidth: 1, borderBottomColor: Colors.BORDER,
  },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 15, fontWeight: '900', color: Colors.TEXT_PRIMARY },

  tabBar: { backgroundColor: Colors.CARD, borderBottomWidth: 1, borderBottomColor: Colors.BORDER },
  tabContent: { paddingHorizontal: 12, paddingVertical: 8, gap: 8 },
  tab: {
    paddingHorizontal: 16, paddingVertical: 8, borderRadius: 10,
    backgroundColor: Colors.SURFACE_DARK,
    alignItems: 'center', minWidth: 72,
  },
  tabActive: { backgroundColor: Colors.PRIMARY },
  tabText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  tabTextActive: { color: Colors.TEXT_WHITE },
  tabDist: { fontSize: 10, fontWeight: '700', color: Colors.TEXT_SECONDARY, marginTop: 2 },
  tabDistActive: { color: 'rgba(255,255,255,0.8)' },

  routeInfo: {
    paddingHorizontal: 16, paddingVertical: 10,
    backgroundColor: Colors.CARD,
    borderBottomWidth: 1, borderBottomColor: Colors.BORDER,
  },
  routeName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  routeMeta: { fontSize: 11, fontWeight: '700', color: Colors.TEXT_SECONDARY, marginTop: 2 },

  footer: {
    padding: 16, backgroundColor: Colors.NAVBAR_BG,
    borderTopWidth: 1, borderTopColor: Colors.BORDER,
  },
  startBtn: {
    height: 52, backgroundColor: Colors.PRIMARY,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center',
  },
  startBtnText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
})
