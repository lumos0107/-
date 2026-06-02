import React, { useEffect, useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, ActivityIndicator } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router } from 'expo-router'
import { ChevronRight } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import { getRunHistory, RunHistory } from '../../../utils/api'
import { metersToKm, secondsToMinutes } from '../../../utils/format'

export default function RecordScreen() {
  const [records, setRecords] = useState<RunHistory[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getRunHistory()
      .then(setRecords)
      .finally(() => setLoading(false))
  }, [])

  const totalDist = records.reduce((s, r) => s + r.totalDistanceMeters, 0)
  const totalTime = records.reduce((s, r) => s + r.totalTimeSeconds, 0)
  const totalCal = records.reduce((s, r) => s + Math.round(r.totalDistanceMeters / 1000 * 60), 0)

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.titleRow}>
          <View>
            <Text style={styles.label}>HISTORY</Text>
            <Text style={styles.title}>기록</Text>
          </View>
          <View style={styles.countBadge}>
            <Text style={styles.countLabel}>총</Text>
            <Text style={styles.countValue}>{records.length}</Text>
          </View>
        </View>

        <View style={styles.summaryCard}>
          <Text style={styles.summaryTitle}>누적 통계</Text>
          <View style={styles.summaryRow}>
            {[
              { label: '누적 거리', value: metersToKm(totalDist), unit: 'km' },
              { label: '누적 시간', value: secondsToMinutes(totalTime).toString(), unit: '분' },
              { label: '칼로리', value: totalCal.toString(), unit: 'kcal' },
            ].map((s) => (
              <View key={s.label} style={styles.summaryItem}>
                <Text style={styles.summaryLabel}>{s.label}</Text>
                <Text style={styles.summaryValue}>{s.value}</Text>
                <Text style={styles.summaryUnit}>{s.unit}</Text>
              </View>
            ))}
          </View>
        </View>

        {loading && (
          <View style={styles.loadingBox}>
            <ActivityIndicator color={Colors.PRIMARY} />
          </View>
        )}

        {!loading && records.length === 0 && (
          <Text style={styles.empty}>아직 러닝 기록이 없어요{'\n'}코스를 찾아 달려보세요 🏃</Text>
        )}

        {records.map((record) => {
          const d = new Date(record.startedAt)
          const dateStr = `${d.getMonth() + 1}월 ${d.getDate()}일`
          return (
            <TouchableOpacity
              key={record.recordId}
              style={styles.recordCard}
              onPress={() => router.push({ pathname: '/(tabs)/record/[id]', params: { id: record.recordId.toString(), recordJson: JSON.stringify(record) } })}
            >
              <View>
                <Text style={styles.recordDate}>{dateStr}</Text>
                <Text style={styles.recordCourse}>{record.courseName}</Text>
              </View>
              <View style={styles.recordRight}>
                <Text style={styles.recordDist}>{metersToKm(record.totalDistanceMeters)}km</Text>
                <ChevronRight color={Colors.TEXT_SECONDARY} size={16} />
              </View>
            </TouchableOpacity>
          )
        })}
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
  summaryCard: { backgroundColor: Colors.CARD, borderRadius: 16, padding: 16, marginBottom: 16 },
  summaryTitle: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_PRIMARY, marginBottom: 12 },
  summaryRow: { flexDirection: 'row', justifyContent: 'space-around' },
  summaryItem: { alignItems: 'center' },
  summaryLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  summaryValue: { fontSize: 20, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  summaryUnit: { fontSize: 10, color: Colors.TEXT_SECONDARY },
  loadingBox: { paddingVertical: 40, alignItems: 'center' },
  empty: { fontSize: 13, color: Colors.TEXT_SECONDARY, textAlign: 'center', marginTop: 40, lineHeight: 22 },
  recordCard: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    backgroundColor: Colors.CARD, borderWidth: 1, borderColor: Colors.BORDER,
    borderRadius: 12, padding: 16, marginBottom: 8,
  },
  recordDate: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  recordCourse: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginTop: 2 },
  recordRight: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  recordDist: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
})
