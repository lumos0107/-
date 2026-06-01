import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { router } from 'expo-router'
import { Sun, X } from 'lucide-react-native'
import { Colors } from '../../constants/Colors'

const WEATHER = { temp: '23°C', condition: '맑음', humidity: '65%', wind: '2.3m/s' }

export default function WeatherScreen() {
  return (
    <View style={styles.overlay}>
      <View style={styles.panel}>
        <View style={styles.header}>
          <View style={styles.titleRow}>
            <Sun color={Colors.WARNING} size={16} />
            <Text style={styles.title}>날씨 정보</Text>
          </View>
          <TouchableOpacity onPress={() => router.back()}>
            <X color={Colors.TEXT_PRIMARY} size={24} />
          </TouchableOpacity>
        </View>

        <View style={styles.grid}>
          {[
            { label: '온도', value: WEATHER.temp },
            { label: '상태', value: WEATHER.condition },
            { label: '습도', value: WEATHER.humidity },
            { label: '바람', value: WEATHER.wind },
          ].map((item) => (
            <View key={item.label} style={styles.cell}>
              <Text style={styles.cellLabel}>{item.label}</Text>
              <Text style={styles.cellValue}>{item.value}</Text>
            </View>
          ))}
        </View>

        <View style={styles.commentBox}>
          <Text style={styles.comment}>러닝하기 좋은 날씨예요 👍</Text>
        </View>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  overlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  panel: {
    backgroundColor: Colors.CARD, borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 20, gap: 16,
    shadowColor: '#0f172a', shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.1, shadowRadius: 12, elevation: 8,
  },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  title: { fontSize: 14, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  cell: { width: '47%', backgroundColor: Colors.SURFACE_DARK, borderRadius: 10, padding: 12 },
  cellLabel: { fontSize: 9, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 6 },
  cellValue: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  commentBox: { backgroundColor: Colors.SUCCESS_BG, borderRadius: 8, padding: 12 },
  comment: { fontSize: 11, fontWeight: '700', color: Colors.SUCCESS_DARK },
})
