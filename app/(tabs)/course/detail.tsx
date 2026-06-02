import React, { useMemo, useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router, useLocalSearchParams } from 'expo-router'
import { ArrowLeft } from 'lucide-react-native'
import Svg, { Polyline, Line, Text as SvgText } from 'react-native-svg'
import { Colors } from '../../../constants/Colors'
import RouteMapView from '../../../components/RouteMapView'
import { metersToKm, secondsToMinutes } from '../../../utils/format'
import { RouteOption, RoutePoint } from '../../../utils/api'

const GRAPH_H = 60

function ElevationGraph({ points }: { points: RoutePoint[] }) {
  const elevPoints = points.filter(p => p.elevation != null)
  if (elevPoints.length < 2) return null

  const elevations = elevPoints.map(p => p.elevation as number)
  const minE = Math.min(...elevations)
  const maxE = Math.max(...elevations)
  const range = maxE - minE || 1

  return (
    <View style={graphStyles.container}>
      <Text style={graphStyles.label}>고도 프로필</Text>
      <View style={graphStyles.svgWrap} onLayout={() => {}}>
        <Svg width="100%" height={GRAPH_H + 16} viewBox={`0 0 400 ${GRAPH_H + 16}`} preserveAspectRatio="none">
          <Line x1="0" y1={GRAPH_H} x2="400" y2={GRAPH_H} stroke={Colors.BORDER} strokeWidth="1" />
          <Polyline
            points={elevPoints.map((p, i) => {
              const x = (i / (elevPoints.length - 1)) * 400
              const y = GRAPH_H - (((p.elevation as number) - minE) / range) * GRAPH_H
              return `${x.toFixed(1)},${y.toFixed(1)}`
            }).join(' ')}
            fill="none" stroke={Colors.PRIMARY} strokeWidth="1.5"
          />
          <SvgText x="2" y={GRAPH_H + 13} fontSize="9" fill={Colors.TEXT_SECONDARY}>{Math.round(minE)}m</SvgText>
          <SvgText x="370" y={GRAPH_H + 13} fontSize="9" fill={Colors.TEXT_SECONDARY}>{Math.round(maxE)}m</SvgText>
        </Svg>
      </View>
    </View>
  )
}

const graphStyles = StyleSheet.create({
  container: { paddingHorizontal: 16, paddingVertical: 10, backgroundColor: Colors.CARD, borderBottomWidth: 1, borderBottomColor: Colors.BORDER },
  label: { fontSize: 11, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 6 },
  svgWrap: { width: '100%' },
})

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
      <View style={styles.tabBar}>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.tabContent}>
        {allRoutes.map((r, i) => {
          const TAB_COLORS = ['#2563eb', '#059669', '#d97706']
          const color = TAB_COLORS[i] ?? TAB_COLORS[0]
          const isActive = activeIdx === i
          return (
            <TouchableOpacity
              key={r.courseId}
              style={[styles.tab, isActive && { backgroundColor: color }]}
              onPress={() => setActiveIdx(i)}
            >
              <Text style={[styles.tabText, isActive && styles.tabTextActive]}>
                루트 {i + 1}{'  '}{metersToKm(r.totalDistanceMeters)}km
              </Text>
            </TouchableOpacity>
          )
        })}
      </ScrollView>
      </View>

      {/* 선택된 루트 정보 */}
      {route && (
        <View style={styles.routeInfo}>
          <Text style={styles.routeName}>{route.courseName}</Text>
          <Text style={styles.routeMeta}>
            {metersToKm(route.totalDistanceMeters)}km · 약 {secondsToMinutes(route.estimatedDurationSeconds)}분
          </Text>
        </View>
      )}

      {/* 고도 그래프 */}
      {route && <ElevationGraph points={route.points} />}

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

  tabBar: { backgroundColor: Colors.CARD, borderBottomWidth: 1, borderBottomColor: Colors.BORDER, alignItems: 'center' },
  tabContent: { paddingHorizontal: 16, paddingVertical: 10, gap: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  tab: {
    paddingHorizontal: 18, paddingVertical: 8, borderRadius: 20,
    backgroundColor: Colors.SURFACE_DARK,
    alignItems: 'center', justifyContent: 'center',
  },
  tabActive: { backgroundColor: Colors.PRIMARY },
  tabText: { fontSize: 13, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  tabTextActive: { color: Colors.TEXT_WHITE },

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
