import React, { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, KeyboardAvoidingView, Platform, ScrollView,
} from 'react-native'
import { router } from 'expo-router'
import { Colors } from '../../constants/Colors'

export default function LoginScreen() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  function handleLogin() {
    if (email.trim() && password.trim()) {
      router.replace('/(tabs)')
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <View style={styles.header}>
          <Text style={styles.appName}>🏃 길동무</Text>
          <Text style={styles.tagline}>제주도 러닝 코스 추천</Text>
        </View>

        <View style={styles.form}>
          <Text style={styles.label}>이메일</Text>
          <TextInput
            style={styles.input}
            value={email}
            onChangeText={setEmail}
            placeholder="example@email.com"
            placeholderTextColor={Colors.TEXT_MUTED}
            keyboardType="email-address"
            autoCapitalize="none"
          />

          <Text style={styles.label}>비밀번호</Text>
          <TextInput
            style={styles.input}
            value={password}
            onChangeText={setPassword}
            placeholder="비밀번호를 입력하세요"
            placeholderTextColor={Colors.TEXT_MUTED}
            secureTextEntry
          />

          <TouchableOpacity
            style={[styles.button, (!email || !password) && styles.buttonDisabled]}
            onPress={handleLogin}
            disabled={!email || !password}
          >
            <Text style={styles.buttonText}>로그인</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.linkRow} onPress={() => router.push('/auth/register')}>
            <Text style={styles.linkText}>계정이 없으신가요? </Text>
            <Text style={[styles.linkText, styles.linkBold]}>회원가입</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: Colors.CARD },
  container: { flexGrow: 1, paddingHorizontal: 24, paddingTop: 80 },
  header: { marginBottom: 48 },
  appName: { fontSize: 32, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  tagline: { fontSize: 14, color: Colors.TEXT_SECONDARY, marginTop: 4 },
  form: { gap: 8 },
  label: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_PRIMARY, marginTop: 8 },
  input: {
    height: 48,
    backgroundColor: Colors.SURFACE_DARK,
    borderWidth: 1,
    borderColor: Colors.BORDER,
    borderRadius: 8,
    paddingHorizontal: 16,
    fontSize: 14,
    color: Colors.TEXT_PRIMARY,
  },
  button: {
    height: 52,
    backgroundColor: Colors.PRIMARY,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
  },
  buttonDisabled: { backgroundColor: Colors.BORDER },
  buttonText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
  linkRow: { flexDirection: 'row', justifyContent: 'center', marginTop: 16 },
  linkText: { fontSize: 13, color: Colors.TEXT_SECONDARY },
  linkBold: { fontWeight: '800', color: Colors.PRIMARY },
})
