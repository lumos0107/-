import { useState } from 'react'
import {
  View, Text, TouchableOpacity, StyleSheet,
  ScrollView, Modal, Pressable,
} from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router } from 'expo-router'
import { X } from 'lucide-react-native'
import { Colors } from '../../../constants/Colors'
import { DUMMY_ROUTES } from '../../../constants/dummy'
import CourseCard from '../../../components/CourseCard'

const DISTANCES = [3, 5, 7, 10]
const DIFFICULTIES = [
  { label: '초보', value: 'beginner' },
  { label: '고수', value: 'normal' },
]

export default function CourseFindScreen() {
  const [selectedDist, setSelectedDist] = useState<number | null>(null)
  const [selectedDiff, setSelectedDiff] = useState<string | null>(null)
  const [showResults, setShowResults] = useState(false)

  const canSearch = selectedDist !== null && selectedDiff !== null

  function handleSearch() {
    setShowResults(true)
  }

  function handleSelectCourse(courseId: number) {
    const course = DUMMY_ROUTES.routes.find((r) => r.courseId === courseId)!
    setShowResults(false)
    router.push({
      pathname: '/(tabs)/course/detail',
      params: {
        courseId: course.courseId.toString(),
        courseName: course.courseName,
        distance: course.totalDistanceMeters.toString(),
        duration: course.estimatedDurationSeconds.toString(),
      },
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

      {/* 바텀시트 모달 */}
      <Modal
        visible={showResults}
        transparent
        animationType="slide"
        onRequestClose={() => setShowResults(false)}
      >
        <Pressable style={styles.overlay} onPress={() => setShowResults(false)} />
        <View style={styles.sheet}>
          <View style={styles.sheetHandle} />
          <View style={styles.sheetHeader}>
            <Text style={styles.sheetTitle}>추천 코스</Text>
            <TouchableOpacity onPress={() => setShowResults(false)}>
              <X color={Colors.TEXT_SECONDARY} size={20} />
            </TouchableOpacity>
          </View>
          <ScrollView showsVerticalScrollIndicator={false}>
            {DUMMY_ROUTES.routes.map((course) => (
              <CourseCard
                key={course.courseId}
                course={course}
                onPress={() => handleSelectCourse(course.courseId)}
              />
            ))}
          </ScrollView>
        </View>
      </Modal>
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
  // 바텀시트
  overlay: {
    flex: 1, backgroundColor: 'rgba(0,0,0,0.4)',
  },
  sheet: {
    backgroundColor: Colors.CARD,
    borderTopLeftRadius: 20, borderTopRightRadius: 20,
    padding: 16, paddingBottom: 32,
    maxHeight: '75%',
    shadowColor: '#0f172a',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.12, shadowRadius: 16, elevation: 8,
  },
  sheetHandle: {
    width: 40, height: 4, borderRadius: 2,
    backgroundColor: Colors.BORDER_DARK,
    alignSelf: 'center', marginBottom: 16,
  },
  sheetHeader: {
    flexDirection: 'row', justifyContent: 'space-between',
    alignItems: 'center', marginBottom: 16,
  },
  sheetTitle: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_PRIMARY },
})
