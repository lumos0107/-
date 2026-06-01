import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native'
import { router, useLocalSearchParams } from 'expo-router'
import { Colors } from '../../constants/Colors'
import MapPlaceholder from '../../components/MapPlaceholder'
import StatBox from '../../components/StatBox'

export default function RunningReadyScreen() {
  const { courseId, courseName, distance, duration } = useLocalSearchParams<{
    courseId: string; courseName: string; distance: string; duration: string
  }>()

  function handleStart() {
    router.replace({ pathname: '/running/active', params: { courseId, courseName, distance } })
  }

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Text style={styles.courseName}>{courseName ?? '코스'}</Text>
      </View>

      <MapPlaceholder height={480} />

      <View style={styles.footer}>
        <TouchableOpacity style={styles.startBtn} onPress={handleStart}>
          <Text style={styles.startBtnText}>시작</Text>
        </TouchableOpacity>
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
    paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: Colors.BORDER,
    backgroundColor: Colors.CARD,
  },
  courseName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  footer: {
    padding: 16, gap: 12,
    backgroundColor: Colors.NAVBAR_BG,
    borderTopWidth: 1, borderTopColor: Colors.BORDER,
  },
  startBtn: {
    height: 48, backgroundColor: Colors.PRIMARY,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center',
  },
  startBtnText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
  statsRow: { flexDirection: 'row', gap: 8 },
})
