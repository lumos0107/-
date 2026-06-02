import { useState, useEffect, useRef } from 'react'
import {
  View, Text, TouchableOpacity, StyleSheet,
  ScrollView, Modal, Pressable, ActivityIndicator, Alert, Animated,
} from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router } from 'expo-router'
import { X } from 'lucide-react-native'
import * as Location from 'expo-location'
import { Colors } from '../../../constants/Colors'
import CourseCard from '../../../components/CourseCard'
import { recommendRoutes, RouteOption } from '../../../utils/api'

const DISTANCES = [3, 5, 10]
const DIFFICULTIES = [
  { label: '초보', value: 'beginner' },
  { label: '고수', value: 'normal' },
]

export default function CourseFindScreen() {
  const [selectedDist, setSelectedDist] = useState<number | null>(null)
  const [selectedDiff, setSelectedDiff] = useState<string | null>(null)
  const [showResults, setShowResults] = useState(false)
  const [loading, setLoading] = useState(false)
  const [routes, setRoutes] = useState<RouteOption[]>([])

  const canSearch = selectedDist !== null && selectedDiff !== null

  // 로딩 애니메이션
  const bounceAnim = useRef(new Animated.Value(0)).current
  const dotAnim1 = useRef(new Animated.Value(0)).current
  const dotAnim2 = useRef(new Animated.Value(0)).current
  const dotAnim3 = useRef(new Animated.Value(0)).current

  useEffect(() => {
    if (!loading) return
    Animated.loop(
      Animated.sequence([
        Animated.timing(bounceAnim, { toValue: -12, duration: 400, useNativeDriver: true }),
        Animated.timing(bounceAnim, { toValue: 0, duration: 400, useNativeDriver: true }),
      ])
    ).start()
    const dotLoop = (anim: Animated.Value, delay: number) =>
      Animated.loop(
        Animated.sequence([
          Animated.delay(delay),
          Animated.timing(anim, { toValue: 1, duration: 300, useNativeDriver: true }),
          Animated.timing(anim, { toValue: 0, duration: 300, useNativeDriver: true }),
          Animated.delay(600 - delay),
        ])
      ).start()
    dotLoop(dotAnim1, 0)
    dotLoop(dotAnim2, 200)
    dotLoop(dotAnim3, 400)
    return () => {
      bounceAnim.stopAnimation()
      dotAnim1.stopAnimation()
      dotAnim2.stopAnimation()
      dotAnim3.stopAnimation()
    }
  }, [loading])

  async function handleSearch() {
    if (!selectedDist) return
    setLoading(true)
    try {
      const { status } = await Location.requestForegroundPermissionsAsync()
      if (status !== 'granted') {
        Alert.alert('위치 권한 필요', '코스 추천을 위해 현재 위치가 필요합니다.')
        setLoading(false)
        return
      }
      const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High })
      const data = await recommendRoutes(
        loc.coords.latitude,
        loc.coords.longitude,
        selectedDist * 1000,
        selectedDiff ?? 'beginner',
      )
      setRoutes(data.routes)
      setShowResults(true)
    } catch (e: any) {
      Alert.alert('오류', e.message || '코스 추천에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  function handleSelectCourse(index: number) {
    setShowResults(false)
    router.push({
      pathname: '/(tabs)/course/detail',
      params: {
        routes: JSON.stringify(routes),
        selectedIndex: index.toString(),
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
          style={[styles.searchBtn, (!canSearch || loading) && styles.searchBtnDisabled]}
          onPress={handleSearch}
          disabled={!canSearch || loading}
        >
          {loading
            ? <ActivityIndicator color={Colors.TEXT_WHITE} />
            : <Text style={styles.searchBtnText}>코스 찾기</Text>
          }
        </TouchableOpacity>
      </ScrollView>

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
            {routes.map((course, index) => (
              <CourseCard
                key={course.courseId}
                course={course}
                onPress={() => handleSelectCourse(index)}
              />
            ))}
          </ScrollView>
        </View>
      </Modal>

      {/* 경로 생성 로딩 오버레이 */}
      <Modal visible={loading} transparent animationType="fade">
        <View style={styles.loadingOverlay}>
          <View style={styles.loadingCard}>
            <Animated.Text style={[styles.runnerEmoji, { transform: [{ translateY: bounceAnim }] }]}>
              🏃
            </Animated.Text>
            <Text style={styles.loadingTitle}>경로 생성 중</Text>
            <Text style={styles.loadingSubtitle}>최적 러닝 코스를 찾고 있어요</Text>
            <View style={styles.dotRow}>
              {[dotAnim1, dotAnim2, dotAnim3].map((anim, i) => (
                <Animated.View
                  key={i}
                  style={[styles.dot, {
                    opacity: anim,
                    transform: [{ scale: anim.interpolate({ inputRange: [0, 1], outputRange: [0.6, 1.2] }) }],
                  }]}
                />
              ))}
            </View>
          </View>
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
  overlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)' },
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

  loadingOverlay: {
    flex: 1, backgroundColor: 'rgba(0,0,0,0.55)',
    alignItems: 'center', justifyContent: 'center',
  },
  loadingCard: {
    backgroundColor: Colors.CARD, borderRadius: 20,
    paddingVertical: 36, paddingHorizontal: 48,
    alignItems: 'center', gap: 10,
    shadowColor: '#0f172a', shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.2, shadowRadius: 24, elevation: 12,
  },
  runnerEmoji: { fontSize: 48, marginBottom: 4 },
  loadingTitle: { fontSize: 17, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  loadingSubtitle: { fontSize: 12, color: Colors.TEXT_SECONDARY, fontWeight: '600' },
  dotRow: { flexDirection: 'row', gap: 8, marginTop: 8 },
  dot: {
    width: 10, height: 10, borderRadius: 5,
    backgroundColor: Colors.PRIMARY,
  },
})
