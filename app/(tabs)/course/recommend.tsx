import React from 'react'
import { View, Text, StyleSheet, SafeAreaView, ScrollView } from 'react-native'
import { router, useLocalSearchParams } from 'expo-router'
import { Colors } from '../../../constants/Colors'
import { DUMMY_ROUTES } from '../../../constants/dummy'
import CourseCard from '../../../components/CourseCard'

export default function RecommendScreen() {
  const { distance, difficulty } = useLocalSearchParams<{ distance: string; difficulty: string }>()

  function handleSelect(courseId: number) {
    const course = DUMMY_ROUTES.routes.find((r) => r.courseId === courseId)!
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
        <Text style={styles.title}>추천 코스</Text>
        {DUMMY_ROUTES.routes.map((course) => (
          <CourseCard key={course.courseId} course={course} onPress={() => handleSelect(course.courseId)} />
        ))}
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  container: { padding: 16 },
  title: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginBottom: 16 },
})
