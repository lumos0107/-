import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router, useLocalSearchParams } from 'expo-router'
import { ArrowLeft, TrendingUp } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import { DUMMY_HISTORY, DUMMY_SEGMENTS } from '../../../constants/dummy'
import PaceItem from '../../../components/PaceItem'
import { metersToKm, secondsToMinutes, secondsToPace } from '../../../utils/format'

export default function RecordDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>()
  const record = DUMMY_HISTORY.find((r) => r.recordId === Number(id)) ?? DUMMY_HISTORY[0]
  const d = new Date(record.startedAt)
  const dateStr = `${d.getMonth() + 1}월 ${d.getDate()}일`

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
              { label: '총 시간', value: secondsToMinutes(record.totalTimeSeconds).toString(), unit: 'min' },
              { label: '평균 페이스', value: secondsToPace(record.averagePaceSeconds), unit: '/km' },
            ].map((s, i) => (
              <React.Fragment key={s.label}>
                {i > 0 && <View style={styles.divider} />}
                <View style={styles.statItem}>
                  <Text style={styles.statLabel}>{s.label}</Text>
                  <Text style={styles.statValue}>{s.value}</Text>
                  <Text style={styles.statUnit}>{s.unit}</Text>
                </View>
              </React.Fragment>
            ))}
          </View>
        </View>

        <View style={styles.extraRow}>
          <View style={styles.extraCard}>
            <Text style={styles.extraLabel}>최고 페이스</Text>
            <Text style={styles.extraValue}>{secondsToPace(record.bestPaceSeconds)}</Text>
          </View>
          <View style={styles.extraCard}>
            <Text style={styles.extraLabel}>칼로리</Text>
            <Text style={styles.extraValue}>{record.calories}</Text>
          </View>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <TrendingUp color={Colors.PRIMARY} size={16} />
            <Text style={styles.sectionTitle}>고도 변화</Text>
          </View>
          <View style={styles.mapPlaceholder} />
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>구간별 페이스</Text>
          {DUMMY_SEGMENTS.map((s) => (
            <PaceItem key={s.km} km={s.km} paceSeconds={s.paceSeconds} />
          ))}
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
  extraCard: {
    flex: 1, backgroundColor: Colors.CARD, borderRadius: 12,
    padding: 14, alignItems: 'center',
  },
  extraLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  extraValue: { fontSize: 18, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  section: { gap: 10 },
  sectionHeader: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  sectionTitle: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  mapPlaceholder: { height: 150, backgroundColor: Colors.SURFACE_DARK, borderRadius: 12 },
})
