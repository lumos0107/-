import React, { useState, useMemo } from 'react'
import { View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router, useLocalSearchParams } from 'expo-router'
import { Star } from 'lucide-react-native'
import { Colors } from '../../constants/Colors'
import RunningMapView from '../../components/RunningMapView'
import StatBox from '../../components/StatBox'
import PaceItem from '../../components/PaceItem'
import { metersToKm, secondsToTime, secondsToPace } from '../../utils/format'
import { useRunningGPS } from '../../hooks/useRunningGPS'
import { RoutePoint, RunResult } from '../../utils/api'

type Status = 'waiting' | 'running' | 'saving' | 'survey'

export default function RunningScreen() {
  const { courseId, courseName, distance, points } = useLocalSearchParams<{
    courseId: string; courseName: string; distance: string; duration: string; points: string
  }>()
  const totalDist = Number(distance ?? 5000)

  const parsedPoints = useMemo<RoutePoint[]>(() => {
    try { return JSON.parse(points ?? '[]') } catch { return [] }
  }, [points])

  const [status, setStatus] = useState<Status>('waiting')
  const [rating, setRating] = useState(0)
  const [saveRoute, setSaveRoute] = useState(false)
  const [result, setResult] = useState<RunResult | null>(null)

  const gps = useRunningGPS()

  const progressPct = Math.min((gps.distanceMeters / totalDist) * 100, 100)

  async function handleStart() {
    try {
      await gps.start(Number(courseId ?? 0))
      setStatus('running')
    } catch (e: any) {
      Alert.alert('오류', e.message)
    }
  }

  async function handleStop() {
    try {
      const r = await gps.stop()
      setResult(r)
      setStatus('saving')
    } catch {
      setStatus('saving')
    }
  }

  const bestPace = result?.lapPaces.length
    ? Math.min(...result.lapPaces.map((l) => l.paceSecPerKm))
    : 0
  const calories = result ? Math.round(result.totalDistanceMeters / 1000 * 60) : 0

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.header}>
        <Text style={styles.courseName}>{courseName ?? '코스'}</Text>
        <View style={styles.headerRight}>
          <TouchableOpacity
            style={styles.weatherBtn}
            onPress={() => router.push('/running/weather')}
          >
            <Text style={styles.weatherText}>☀ 23°</Text>
          </TouchableOpacity>
          {status === 'running' && (
            <TouchableOpacity style={styles.stopBtn} onPress={handleStop}>
              <Text style={styles.stopText}>중단</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>

      {status === 'running' && (
        <View style={styles.progress}>
          <View style={styles.progressRow}>
            <Text style={styles.progressLabel}>진행도</Text>
            <Text style={styles.progressLabel}>
              {(gps.distanceMeters / 1000).toFixed(1)} / {metersToKm(totalDist)}km  {Math.round(progressPct)}%
            </Text>
          </View>
          <View style={styles.progressBar}>
            <View style={[styles.progressFill, { width: `${progressPct}%` as any }]} />
          </View>
        </View>
      )}

      <RunningMapView points={parsedPoints} currentLocation={gps.currentLocation} />

      <View style={styles.footer}>
        {status === 'waiting' && (
          <TouchableOpacity style={styles.startBtn} onPress={handleStart}>
            <Text style={styles.startBtnText}>시작</Text>
          </TouchableOpacity>
        )}
        <View style={styles.statsRow}>
          <StatBox
            label="거리"
            value={status === 'running' ? (gps.distanceMeters / 1000).toFixed(2) : '0.00'}
            unit="km"
          />
          <StatBox label="시간" value={secondsToTime(gps.elapsedSeconds)} />
          <StatBox
            label="현재"
            value={status === 'running' && gps.currentPaceSecPerKm > 0
              ? secondsToPace(gps.currentPaceSecPerKm)
              : '—'}
            unit={status === 'running' && gps.currentPaceSecPerKm > 0 ? '/km' : undefined}
          />
          <StatBox
            label="최고"
            value={gps.lapPaces.length > 0
              ? secondsToPace(Math.min(...gps.lapPaces.map((l) => l.paceSecPerKm)))
              : '—'}
            unit={gps.lapPaces.length > 0 ? '/km' : undefined}
          />
        </View>
      </View>

      {status === 'saving' && result && (
        <View style={styles.resultOverlay}>
          <ScrollView contentContainerStyle={styles.resultContent}>
            <View style={styles.resultHeader}>
              <Text style={styles.completeLabel}>COMPLETE</Text>
              <Text style={styles.completeTitle}>완주!</Text>
              <Text style={styles.resultCourseName}>{courseName}</Text>
            </View>
            <View style={styles.mainCard}>
              <View style={styles.mainStatRow}>
                {[
                  { label: '총 거리', value: metersToKm(result.totalDistanceMeters), unit: 'km' },
                  { label: '총 시간', value: secondsToTime(result.totalTimeSeconds), unit: '' },
                  { label: '평균 페이스', value: secondsToPace(result.averagePaceSeconds), unit: '/km' },
                ].map((s, i) => (
                  <React.Fragment key={s.label}>
                    {i > 0 && <View style={styles.mainDivider} />}
                    <View style={styles.mainStatItem}>
                      <Text style={styles.mainStatLabel}>{s.label}</Text>
                      <Text style={styles.mainStatValue}>{s.value}</Text>
                      <Text style={styles.mainStatUnit}>{s.unit}</Text>
                    </View>
                  </React.Fragment>
                ))}
              </View>
            </View>
            <View style={styles.extraRow}>
              <View style={styles.extraCard}>
                <Text style={styles.extraLabel}>최고 페이스</Text>
                <Text style={styles.extraValue}>{bestPace > 0 ? secondsToPace(bestPace) : '—'}</Text>
              </View>
              <View style={styles.extraCard}>
                <Text style={styles.extraLabel}>칼로리</Text>
                <Text style={styles.extraValue}>{calories}</Text>
              </View>
            </View>
            {result.lapPaces.length > 0 && (
              <View style={styles.segCard}>
                <Text style={styles.segTitle}>구간별 페이스</Text>
                {result.lapPaces.map((s) => (
                  <PaceItem key={s.km} km={s.km} paceSeconds={s.paceSecPerKm} />
                ))}
              </View>
            )}
            <TouchableOpacity style={styles.continueBtn} onPress={() => setStatus('survey')}>
              <Text style={styles.continueBtnText}>계속</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>
      )}

      {status === 'survey' && (
        <View style={styles.surveyOverlay}>
          <View style={styles.surveyModal}>
            <Text style={styles.surveyTitle}>수고하셨어요!</Text>
            <Text style={styles.surveySubtitle}>오늘 러닝은 어떠셨나요?</Text>
            <View style={styles.starRow}>
              {[1, 2, 3, 4, 5].map((star) => (
                <TouchableOpacity key={star} onPress={() => setRating(star)}>
                  <Star size={36} color="#d97706" fill={star <= rating ? '#d97706' : 'transparent'} />
                </TouchableOpacity>
              ))}
            </View>
            <TouchableOpacity style={styles.checkRow} onPress={() => setSaveRoute((v) => !v)}>
              <View style={[styles.checkbox, saveRoute && styles.checkboxChecked]}>
                {saveRoute && <Text style={styles.checkmark}>✓</Text>}
              </View>
              <Text style={styles.checkLabel}>이 루트 저장하기</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.doneBtn, rating === 0 && styles.doneBtnDisabled]}
              onPress={() => { if (rating > 0) router.replace('/(tabs)/record') }}
            >
              <Text style={styles.doneBtnText}>완료</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  header: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingHorizontal: 16, paddingVertical: 12,
    backgroundColor: Colors.CARD, borderBottomWidth: 1, borderBottomColor: Colors.BORDER,
  },
  courseName: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  headerRight: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  weatherBtn: { backgroundColor: Colors.SURFACE_DARK, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 4 },
  weatherText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  stopBtn: { backgroundColor: Colors.DANGER, borderRadius: 8, paddingHorizontal: 10, paddingVertical: 4 },
  stopText: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_WHITE },
  progress: { paddingHorizontal: 16, paddingVertical: 8, backgroundColor: Colors.CARD },
  progressRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  progressLabel: { fontSize: 9, fontWeight: '800', color: Colors.TEXT_SECONDARY },
  progressBar: { height: 6, backgroundColor: Colors.BORDER, borderRadius: 3 },
  progressFill: { height: 6, backgroundColor: Colors.PRIMARY, borderRadius: 3 },
  footer: { padding: 16, gap: 12, backgroundColor: Colors.NAVBAR_BG, borderTopWidth: 1, borderTopColor: Colors.BORDER },
  startBtn: { height: 48, backgroundColor: Colors.PRIMARY, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  startBtnText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
  statsRow: { flexDirection: 'row', gap: 8 },
  resultOverlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: Colors.CARD, zIndex: 50 },
  resultContent: { padding: 16, paddingBottom: 40 },
  resultHeader: { alignItems: 'center', marginBottom: 24, marginTop: 16 },
  completeLabel: { fontSize: 11, fontWeight: '800', color: Colors.SUCCESS, letterSpacing: 2 },
  completeTitle: { fontSize: 24, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginTop: 4 },
  resultCourseName: { fontSize: 14, color: Colors.TEXT_SECONDARY, marginTop: 4 },
  mainCard: { backgroundColor: Colors.SURFACE_DARK, borderRadius: 16, paddingVertical: 20, marginBottom: 16 },
  mainStatRow: { flexDirection: 'row', justifyContent: 'space-around', alignItems: 'center' },
  mainStatItem: { alignItems: 'center', flex: 1 },
  mainStatLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  mainStatValue: { fontSize: 22, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  mainStatUnit: { fontSize: 11, color: Colors.TEXT_SECONDARY, marginTop: 2 },
  mainDivider: { width: 1, height: 40, backgroundColor: Colors.BORDER },
  extraRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
  extraCard: { flex: 1, backgroundColor: Colors.SURFACE_DARK, borderRadius: 12, padding: 16, alignItems: 'center' },
  extraLabel: { fontSize: 10, fontWeight: '800', color: Colors.TEXT_SECONDARY, marginBottom: 4 },
  extraValue: { fontSize: 18, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  segCard: { backgroundColor: Colors.SURFACE_DARK, borderRadius: 16, padding: 16, marginBottom: 16 },
  segTitle: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginBottom: 12 },
  continueBtn: { height: 48, backgroundColor: Colors.PRIMARY, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  continueBtnText: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_WHITE },
  surveyOverlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', zIndex: 50, justifyContent: 'center', alignItems: 'center' },
  surveyModal: { width: '88%', backgroundColor: Colors.CARD, borderRadius: 18, padding: 24, alignItems: 'center', gap: 16, shadowColor: '#0f172a', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.15, shadowRadius: 24, elevation: 8 },
  surveyTitle: { fontSize: 20, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  surveySubtitle: { fontSize: 14, color: Colors.TEXT_SECONDARY, marginTop: -8 },
  starRow: { flexDirection: 'row', gap: 8, marginVertical: 4 },
  checkRow: { flexDirection: 'row', alignItems: 'center', gap: 10, alignSelf: 'flex-start', paddingHorizontal: 4 },
  checkbox: { width: 20, height: 20, borderRadius: 4, borderWidth: 2, borderColor: Colors.BORDER, alignItems: 'center', justifyContent: 'center' },
  checkboxChecked: { backgroundColor: Colors.PRIMARY, borderColor: Colors.PRIMARY },
  checkmark: { fontSize: 12, color: Colors.TEXT_WHITE, fontWeight: '900' },
  checkLabel: { fontSize: 14, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  doneBtn: { width: '100%', height: 48, backgroundColor: Colors.PRIMARY, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  doneBtnDisabled: { backgroundColor: Colors.BORDER },
  doneBtnText: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_WHITE },
})
