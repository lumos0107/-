import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView, ScrollView } from 'react-native'
import { router } from 'expo-router'
import { Star, MoreHorizontal } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import { DUMMY_ROUTES } from '../../../constants/dummy'
import { metersToKm, secondsToPace } from '../../../utils/format'

const SAVED = DUMMY_ROUTES.routes[0]

export default function SavedRoutesScreen() {
  function handleRun() {
    router.push({
      pathname: '/running/ready',
      params: {
        courseId: SAVED.courseId.toString(),
        courseName: SAVED.courseName,
        distance: SAVED.totalDistanceMeters.toString(),
        duration: SAVED.estimatedDurationSeconds.toString(),
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
            <Text style={styles.countValue}>1</Text>
          </View>
        </View>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <View style={styles.nameRow}>
              <View style={styles.dot} />
              <Text style={styles.courseName}>{SAVED.courseName}</Text>
            </View>
            <View style={styles.starRow}>
              {[1, 2, 3, 4, 5].map((i) => (
                <Star key={i} color="#d97706" fill="#d97706" size={12} />
              ))}
            </View>
            <TouchableOpacity style={styles.moreBtn}>
              <MoreHorizontal color={Colors.TEXT_SECONDARY} size={14} />
            </TouchableOpacity>
          </View>

          <View style={styles.statsRow}>
            {[
              { label: '거리', value: `${metersToKm(SAVED.totalDistanceMeters)}km` },
              { label: '평균 페이스', value: "5'28\"" },
              { label: '뛴 횟수', value: '1회' },
            ].map((s) => (
              <View key={s.label} style={styles.statItem}>
                <Text style={styles.statLabel}>{s.label}</Text>
                <Text style={styles.statValue}>{s.value}</Text>
              </View>
            ))}
          </View>

          <TouchableOpacity style={styles.runBtn} onPress={handleRun}>
            <Text style={styles.runBtnText}>이 루트로 달리기</Text>
          </TouchableOpacity>
        </View>
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
  card: {
    borderWidth: 1, borderColor: Colors.BORDER, borderRadius: 12,
    padding: 16, backgroundColor: Colors.CARD, gap: 12,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 8, flex: 1 },
  dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: Colors.PRIMARY },
  courseName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  starRow: { flexDirection: 'row', gap: 2 },
  moreBtn: { padding: 4 },
  statsRow: { flexDirection: 'row', gap: 8 },
  statItem: {
    flex: 1, backgroundColor: Colors.SURFACE_DARK,
    borderRadius: 8, padding: 10, alignItems: 'center',
  },
  statLabel: { fontSize: 9, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  statValue: { fontSize: 12, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  runBtn: {
    height: 40, backgroundColor: Colors.PRIMARY,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center',
  },
  runBtnText: { fontSize: 12, fontWeight: '900', color: Colors.TEXT_WHITE },
})
