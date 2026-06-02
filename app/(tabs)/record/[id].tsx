import React, { useMemo } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router, useLocalSearchParams } from 'expo-router'
import { ArrowLeft } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import { RunHistory } from '../../../utils/api'
import { metersToKm, secondsToTime, secondsToPace } from '../../../utils/format'

export default function RecordDetailScreen() {
  const { recordJson } = useLocalSearchParams<{ recordJson: string }>()

  const record = useMemo<RunHistory | null>(() => {
    try { return JSON.parse(recordJson ?? 'null') } catch { return null }
  }, [recordJson])

  if (!record) {
    return (
      <SafeAreaView style={styles.safe}>
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()}>
            <ArrowLeft color={Colors.TEXT_PRIMARY} size={20} />
          </TouchableOpacity>
        </View>
        <View style={styles.errorBox}>
          <Text style={styles.errorText}>기록을 불러올 수 없습니다</Text>
        </View>
      </SafeAreaView>
    )
  }

  const d = new Date(record.startedAt)
  const dateStr = `${d.getMonth() + 1}월 ${d.getDate()}일`
  const calories = Math.round(record.totalDistanceMeters / 1000 * 60)

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <ArrowLeft color={Colors.TEXT_PRIMARY} size={20} />
        </TouchableOpacity>
        <View>
          <Text style={styles.date}>{dateStr}</Text>
          <Text style={styles.courseName}>{record.courseName}</Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.statsCard}>
          <View style={styles.statsRow}>
            {[
              { label: '총 거리', value: metersToKm(record.totalDistanceMeters), unit: 'km' },
              { label: '총 시간', value: secondsToTime(record.totalTimeSeconds), unit: '' },
              { label: '평균 페이스', value: secondsToPace(record.averagePaceSeconds), unit: '/km' },
            ].map((s, i) => (
              <React.Fragment key={s.label}>
                {i > 0 && <View style={styles.divider} />}
                <View style={styles.statItem}>
                  <Text style={styles.statLabel}>{s.label}</Text>
                  <Text style={styles.statValue}>{s.value}</Text>
                  {s.unit ? <Text style={styles.statUnit}>{s.unit}</Text> : null}
                </View>
              </React.Fragment>
            ))}
          </View>
        </View>

        <View style={styles.extraRow}>
          <View style={styles.extraCard}>
            <Text style={styles.extraLabel}>칼로리 (추정)</Text>
            <Text style={styles.extraValue}>{calories} kcal</Text>
          </View>
          <View style={styles.extraCard}>
            <Text style={styles.extraLabel}>날짜</Text>
            <Text style={styles.extraValue}>{dateStr}</Text>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  header: {
    flexDirection: 'row', alignItems: 'center', gap: 12,
    paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: Colors.BORDER, backgroundColor: Colors.CARD,
  },
  date: { fontSize: 12, color: Colors.TEXT_SECONDARY },
  courseName: { fontSize: 18, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  container: { padding: 16, gap: 16 },
  statsCard: { backgroundColor: Colors.CARD, borderRadius: 12, paddingVertical: 16 },
  statsRow: { flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center' },
  statItem: { alignItems: 'center', flex: 1 },
  statLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  statValue: { fontSize: 22, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  statUnit: { fontSize: 11, color: Colors.TEXT_SECONDARY },
  divider: { width: 1, height: 40, backgroundColor: Colors.BORDER },
  extraRow: { flexDirection: 'row', gap: 12 },
  extraCard: { flex: 1, backgroundColor: Colors.CARD, borderRadius: 12, padding: 14, alignItems: 'center' },
  extraLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  extraValue: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  errorBox: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  errorText: { fontSize: 14, color: Colors.TEXT_SECONDARY },
})
