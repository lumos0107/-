import React, { useState } from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { router } from 'expo-router'
import { Colors } from '../../constants/Colors'

const EMOJIS = ['😞', '😕', '😐', '😊', '😄']

export default function FeedbackScreen() {
  const [selectedEmoji, setSelectedEmoji] = useState<number | null>(null)
  const [saveRoute, setSaveRoute] = useState(false)

  return (
    <View style={styles.overlay}>
      <View style={styles.modal}>
        <Text style={styles.title}>수고하셨어요!</Text>
        <Text style={styles.subtitle}>오늘 러닝은 어떠셨나요?</Text>

        <View style={styles.emojiRow}>
          {EMOJIS.map((emoji, i) => (
            <TouchableOpacity
              key={i}
              style={[styles.emojiBtn, selectedEmoji === i && styles.emojiBtnActive]}
              onPress={() => setSelectedEmoji(i)}
            >
              <Text style={styles.emoji}>{emoji}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <TouchableOpacity style={styles.checkRow} onPress={() => setSaveRoute(!saveRoute)}>
          <View style={[styles.checkbox, saveRoute && styles.checkboxChecked]}>
            {saveRoute && <Text style={styles.checkmark}>✓</Text>}
          </View>
          <Text style={styles.checkLabel}>이 루트 저장하기</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.doneBtn}
          onPress={() => router.replace('/(tabs)/record')}
        >
          <Text style={styles.doneBtnText}>완료</Text>
        </TouchableOpacity>
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1, backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center', alignItems: 'center',
  },
  modal: {
    width: '88%', backgroundColor: Colors.CARD,
    borderRadius: 18, padding: 24, alignItems: 'center', gap: 16,
    shadowColor: '#0f172a', shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.15, shadowRadius: 24, elevation: 8,
  },
  title: { fontSize: 20, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  subtitle: { fontSize: 14, color: Colors.TEXT_SECONDARY, marginTop: -8 },
  emojiRow: { flexDirection: 'row', gap: 8, marginVertical: 4 },
  emojiBtn: {
    width: 40, height: 40, borderRadius: 20,
    borderWidth: 2, borderColor: Colors.BORDER,
    alignItems: 'center', justifyContent: 'center',
  },
  emojiBtnActive: { borderColor: Colors.PRIMARY, backgroundColor: Colors.PRIMARY_LIGHT },
  emoji: { fontSize: 22 },
  checkRow: {
    flexDirection: 'row', alignItems: 'center', gap: 10,
    alignSelf: 'flex-start', paddingHorizontal: 4,
  },
  checkbox: {
    width: 20, height: 20, borderRadius: 4,
    borderWidth: 2, borderColor: Colors.BORDER,
    alignItems: 'center', justifyContent: 'center',
  },
  checkboxChecked: { backgroundColor: Colors.PRIMARY, borderColor: Colors.PRIMARY },
  checkmark: { fontSize: 12, color: Colors.TEXT_WHITE, fontWeight: '900' },
  checkLabel: { fontSize: 14, fontWeight: '800', color: Colors.TEXT_PRIMARY },
  doneBtn: {
    width: '100%', height: 48, backgroundColor: Colors.PRIMARY,
    borderRadius: 10, alignItems: 'center', justifyContent: 'center',
  },
  doneBtnText: { fontSize: 14, fontWeight: '900', color: Colors.TEXT_WHITE },
})
