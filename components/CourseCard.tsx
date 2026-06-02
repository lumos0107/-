import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { Colors } from '../constants/Colors'
import { metersToKm, secondsToMinutes } from '../utils/format'
import type { RouteOption } from '../utils/api'

interface Props {
  course: RouteOption
  onPress: () => void
}

export default function CourseCard({ course, onPress }: Props) {
  return (
    <TouchableOpacity style={styles.card} onPress={onPress} activeOpacity={0.8}>
      <View style={styles.header}>
        <View style={styles.nameRow}>
          <View style={styles.dot} />
          <Text style={styles.name}>{course.courseName}</Text>
        </View>
        {course.score != null && (
          <View style={styles.scoreBadge}>
            <Text style={styles.scoreText}>{course.score}점</Text>
          </View>
        )}
      </View>
      <View style={styles.stats}>
        <Text style={styles.stat}>거리 {metersToKm(course.totalDistanceMeters)}km</Text>
        <Text style={styles.stat}>시간 {secondsToMinutes(course.estimatedDurationSeconds)}분</Text>
      </View>
      <Text style={styles.reason} numberOfLines={2}>{course.reason}</Text>
    </TouchableOpacity>
  )
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderColor: Colors.BORDER,
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    backgroundColor: Colors.CARD,
    shadowColor: '#0f172a',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.08,
    shadowRadius: 12,
    elevation: 3,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  nameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.PRIMARY,
  },
  name: {
    fontSize: 14,
    fontWeight: '900',
    color: Colors.TEXT_PRIMARY,
  },
  scoreBadge: {
    backgroundColor: Colors.PRIMARY_LIGHT,
    borderRadius: 99,
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  scoreText: {
    fontSize: 10,
    fontWeight: '700',
    color: Colors.PRIMARY,
  },
  stats: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 6,
  },
  stat: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.TEXT_SECONDARY,
  },
  reason: {
    fontSize: 11,
    fontWeight: '800',
    color: Colors.TEXT_SECONDARY,
    lineHeight: 16,
  },
})
