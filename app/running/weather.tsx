import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ActivityIndicator } from 'react-native'
import { router, useLocalSearchParams } from 'expo-router'
import { X } from 'lucide-react-native'
import { Colors } from '../../constants/Colors'
import { weatherEmoji, runningComment, WeatherData } from '../../utils/weather'

export default function WeatherScreen() {
  const { weatherJson } = useLocalSearchParams<{ weatherJson?: string }>()

  const weather: WeatherData | null = weatherJson ? JSON.parse(weatherJson) : null

  return (
    <View style={styles.overlay}>
      <View style={styles.panel}>
        <View style={styles.header}>
          <View style={styles.titleRow}>
            <Text style={styles.titleEmoji}>
              {weather ? weatherEmoji(weather.icon) : '🌤'}
            </Text>
            <Text style={styles.title}>날씨 정보</Text>
          </View>
          <TouchableOpacity onPress={() => router.back()}>
            <X color={Colors.TEXT_PRIMARY} size={24} />
          </TouchableOpacity>
        </View>

        {weather ? (
          <>
            <View style={styles.grid}>
              {[
                { label: '온도', value: `${weather.temp}°C` },
                { label: '상태', value: weather.condition },
                { label: '습도', value: `${weather.humidity}%` },
                { label: '바람', value: `${weather.windSpeed.toFixed(1)}m/s` },
              ].map((item) => (
                <View key={item.label} style={styles.cell}>
                  <Text style={styles.cellLabel}>{item.label}</Text>
                  <Text style={styles.cellValue}>{item.value}</Text>
                </View>
              ))}
            </View>
            <View style={styles.commentBox}>
              <Text style={styles.comment}>{runningComment(weather.temp, weather.icon)}</Text>
            </View>
          </>
        ) : (
          <View style={styles.loading}>
            <ActivityIndicator color={Colors.PRIMARY} />
            <Text style={styles.loadingText}>날씨 정보를 불러오는 중...</Text>
          </View>
        )}
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
  titleEmoji: { fontSize: 18 },
  title: { fontSize: 14, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  cell: { width: '47%', backgroundColor: Colors.SURFACE_DARK, borderRadius: 10, padding: 12 },
  cellLabel: { fontSize: 9, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 6 },
  cellValue: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  commentBox: { backgroundColor: Colors.SUCCESS_BG, borderRadius: 8, padding: 12 },
  comment: { fontSize: 11, fontWeight: '700', color: Colors.SUCCESS_DARK },
  loading: { alignItems: 'center', gap: 8, paddingVertical: 20 },
  loadingText: { fontSize: 12, color: Colors.TEXT_SECONDARY },
})
