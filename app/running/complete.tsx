import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView, ScrollView } from 'react-native'
import { router } from 'expo-router'
import { Colors } from '../../constants/Colors'
import PaceItem from '../../components/PaceItem'
import { DUMMY_HISTORY, DUMMY_SEGMENTS } from '../../constants/dummy'
import { metersToKm, secondsToMinutes, secondsToPace } from '../../utils/format'

const record = DUMMY_HISTORY[0]

export default function RunningCompleteScreen() {
  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <View style={styles.header}>
          <Text style={styles.completeLabel}>COMPLETE</Text>
          <Text style={styles.completeTitle}>완주!</Text>
          <Text style={styles.courseName}>{record.courseName}</Text>
        </View>

        <View style={styles.mainCard}>
          <View style={styles.statRow}>
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

        <View style={styles.segCard}>
          <Text style={styles.segTitle}>구간별 페이스</Text>
          {DUMMY_SEGMENTS.map((s) => (
            <PaceItem key={s.km} km={s.km} paceSeconds={s.paceSeconds} />
          ))}
        </View>

        <TouchableOpacity style={styles.continueBtn} onPress={() => router.push('/running/feedback')}>
          <Text style={styles.continueBtnText}>계속</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  container: { padding: 16, paddingBottom: 32 },
  header: { alignItems: 'center', marginBottom: 24 },
  completeLabel: { fontSize: 11, fontWeight: '800', color: Colors.SUCCESS, letterSpacing: 2 },
  completeTitle: { fontSize: 24, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginTop: 4 },
  courseName: { fontSize: 14, color: Colors.TEXT_SECONDARY, marginTop: 4 },
  mainCard: {
    backgroundColor: Colors.CARD, borderRadius: 16, padding: 20, marginBottom: 16,
    shadowColor: '#0f172a', shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08, shadowRadius: 12, elevation: 3,
  },
  statRow: { flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center' },
  statItem: { alignItems: 'center', flex: 1 },
  statLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  statValue: { fontSize: 22, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  statUnit: { fontSize: 11, color: Colors.TEXT_SECONDARY, marginTop: 2 },
  divider: { width: 1, height: 40, backgroundColor: Colors.BORDER },
  extraRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
  extraCard: {
    flex: 1, backgroundColor: Colors.CARD, borderRadius: 12, padding: 16, alignItems: 'center',
    shadowColor: '#0f172a', shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06, shadowRadius: 8, elevation: 2,
  },
  extraLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  extraValue: { fontSize: 18, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  segCard: {
    backgroundColor: Colors.CARD, borderRadius: 16, padding: 16, marginBottom: 16,
    shadowColor: '#0f172a', shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06, shadowRadius: 8, elevation: 2,
  },
  segTitle: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginBottom: 12 },
  continueBtn: {
    height: 48, backgroundColor: Colors.PRIMARY,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center',
  },
  continueBtnText: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_WHITE },
})
