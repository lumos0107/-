import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router, useLocalSearchParams } from 'expo-router'
import { Colors } from '../../../constants/Colors'
import MapPlaceholder from '../../../components/MapPlaceholder'
import { metersToKm } from '../../../utils/format'

export default function CourseDetailScreen() {
  const { courseId, courseName, distance, duration } = useLocalSearchParams<{
    courseId: string; courseName: string; distance: string; duration: string
  }>()

  function handleStart() {
    router.push({ pathname: '/running/ready', params: { courseId, courseName, distance, duration } })
  }

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <View style={styles.nameRow}>
          <Text style={styles.courseName}>{courseName}</Text>
          <Text style={styles.distBadge}>{metersToKm(Number(distance))}km</Text>
        </View>
      </View>

      <MapPlaceholder height={520} />

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
    paddingHorizontal: 16, paddingVertical: 12,
    borderBottomWidth: 1, borderBottomColor: Colors.BORDER,
    backgroundColor: Colors.CARD,
  },
  nameRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  courseName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  distBadge: { fontSize: 11, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  footer: {
    padding: 16, backgroundColor: Colors.NAVBAR_BG,
    borderTopWidth: 1, borderTopColor: Colors.BORDER,
  },
  startBtn: {
    height: 56, backgroundColor: Colors.PRIMARY,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center',
  },
  startBtnText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
})
