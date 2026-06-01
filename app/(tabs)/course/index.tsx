import React, { useState } from 'react'
import {
  View, Text, TouchableOpacity, StyleSheet,
  SafeAreaView, ScrollView,
} from 'react-native'
import { router } from 'expo-router'
import { Colors } from '../../../constants/Colors'

const DISTANCES = [3, 5, 7, 10]
const DIFFICULTIES = [
  { label: '초보', value: 'beginner' },
  { label: '고수', value: 'normal' },
]

export default function CourseFindScreen() {
  const [selectedDist, setSelectedDist] = useState<number | null>(null)
  const [selectedDiff, setSelectedDiff] = useState<string | null>(null)

  const canSearch = selectedDist !== null && selectedDiff !== null

  function handleSearch() {
    router.push({
      pathname: '/(tabs)/course/recommend',
      params: { distance: (selectedDist! * 1000).toString(), difficulty: selectedDiff! },
    })
  }

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.title}>코스 찾기</Text>

        <View style={styles.section}>
          <Text style={styles.sectionLabel}>얼마나 뛸까요</Text>
          <View style={styles.chipRow}>
            {DISTANCES.map((d) => (
              <TouchableOpacity
                key={d}
                style={[styles.chip, selectedDist === d && styles.chipActive]}
                onPress={() => setSelectedDist(d)}
              >
                <Text style={[styles.chipText, selectedDist === d && styles.chipTextActive]}>
                  {d}km
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionLabel}>난이도를 선택해주세요</Text>
          <View style={styles.chipRow}>
            {DIFFICULTIES.map((d) => (
              <TouchableOpacity
                key={d.value}
                style={[styles.chip, styles.chipHalf, selectedDiff === d.value && styles.chipActive]}
                onPress={() => setSelectedDiff(d.value)}
              >
                <Text style={[styles.chipText, selectedDiff === d.value && styles.chipTextActive]}>
                  {d.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <TouchableOpacity
          style={[styles.searchBtn, !canSearch && styles.searchBtnDisabled]}
          onPress={handleSearch}
          disabled={!canSearch}
        >
          <Text style={styles.searchBtnText}>코스 찾기</Text>
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  container: { padding: 16 },
  title: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginBottom: 24 },
  section: { marginBottom: 20 },
  sectionLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_PRIMARY, marginBottom: 10 },
  chipRow: { flexDirection: 'row', gap: 8 },
  chip: {
    flex: 1, height: 32, borderRadius: 8,
    backgroundColor: Colors.CARD, borderWidth: 1, borderColor: Colors.BORDER,
    alignItems: 'center', justifyContent: 'center',
  },
  chipHalf: { flex: 1 },
  chipActive: { backgroundColor: Colors.PRIMARY, borderColor: Colors.PRIMARY },
  chipText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  chipTextActive: { color: Colors.TEXT_WHITE },
  searchBtn: {
    height: 44, backgroundColor: Colors.PRIMARY,
    borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginTop: 8,
  },
  searchBtnDisabled: { backgroundColor: Colors.BORDER },
  searchBtnText: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_WHITE },
})
