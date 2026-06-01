import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native'
import { router, useLocalSearchParams } from 'expo-router'
import { Colors } from '../../constants/Colors'
import MapPlaceholder from '../../components/MapPlaceholder'
import StatBox from '../../components/StatBox'
import { metersToKm } from '../../utils/format'

export default function RunningActiveScreen() {
  const { courseName, distance } = useLocalSearchParams<{
    courseId: string; courseName: string; distance: string
  }>()
  const totalDist = Number(distance ?? 5000)

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Text style={styles.courseName}>{courseName}</Text>
        <View style={styles.headerRight}>
          <Text style={styles.distText}>0.00km</Text>
          <TouchableOpacity style={styles.weatherBtn} onPress={() => router.push('/running/weather')}>
            <Text style={styles.weatherText}>23°</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.stopBtn} onPress={() => router.replace('/running/complete')}>
            <Text style={styles.stopText}>중단</Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.progress}>
        <View style={styles.progressRow}>
          <Text style={styles.progressLabel}>진행도</Text>
          <Text style={styles.progressLabel}>0.0 / {metersToKm(totalDist)}km  0%</Text>
        </View>
        <View style={styles.progressBar}>
          <View style={[styles.progressFill, { width: '0%' }]} />
        </View>
      </View>

      <MapPlaceholder height={430} />

      <View style={styles.footer}>
        <View style={styles.statsRow}>
          <StatBox label="거리" value="0.00" unit="km" />
          <StatBox label="시간" value="0:00" />
          <StatBox label="현재" value="—" unit="/km" />
          <StatBox label="최고" value="—" unit="/km" />
        </View>
      </View>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 16, paddingVertical: 12, backgroundColor: Colors.CARD,
    borderBottomWidth: 1, borderBottomColor: Colors.BORDER,
  },
  courseName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  headerRight: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  distText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  weatherBtn: {
    backgroundColor: Colors.SURFACE_DARK, borderRadius: 8,
    paddingHorizontal: 10, paddingVertical: 4,
  },
  weatherText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  stopBtn: {
    backgroundColor: Colors.DANGER, borderRadius: 8,
    paddingHorizontal: 10, paddingVertical: 4,
  },
  stopText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_WHITE },
  progress: { paddingHorizontal: 16, paddingVertical: 8, backgroundColor: Colors.CARD },
  progressRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  progressLabel: { fontSize: 9, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  progressBar: { height: 6, backgroundColor: Colors.BORDER, borderRadius: 3 },
  progressFill: { height: 6, backgroundColor: Colors.PRIMARY, borderRadius: 3 },
  footer: {
    padding: 16, backgroundColor: Colors.NAVBAR_BG,
    borderTopWidth: 1, borderTopColor: Colors.BORDER,
  },
  statsRow: { flexDirection: 'row', gap: 8 },
})
