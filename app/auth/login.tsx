import React, { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator, Alert,
} from 'react-native'
import { router } from 'expo-router'
import { Colors } from '../../constants/Colors'
import { loginUser } from '../../utils/api'

export default function LoginScreen() {
  const [email, setEmail] = useState('1111')
  const [password, setPassword] = useState('1111')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleLogin() {
    if (!email.trim() || !password.trim()) {
      setError('이메일과 비밀번호를 입력하세요.')
      return
    }
    setLoading(true)
    setError('')
    try {
      await loginUser(email.trim(), password.trim())
      router.replace('/(tabs)')
    } catch (e: any) {
      setError(e.message || '로그인에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <View style={styles.header}>
          <View style={styles.iconBox}>
            <Text style={styles.iconText}>🏃</Text>
          </View>
          <Text style={styles.appName}>길동무</Text>
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

          {error !== '' && <Text style={styles.errorText}>{error}</Text>}

          <TouchableOpacity
            style={[styles.button, (loading || !email || !password) && styles.buttonDisabled]}
            onPress={handleLogin}
            disabled={loading || !email || !password}
          >
            {loading
              ? <ActivityIndicator color={Colors.TEXT_WHITE} />
              : <Text style={styles.buttonText}>로그인</Text>
            }
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
  container: { flexGrow: 1, paddingHorizontal: 24, paddingTop: 80, paddingBottom: 40 },
  header: { alignItems: 'center', marginBottom: 48 },
  iconBox: {
    width: 64, height: 64, backgroundColor: Colors.PRIMARY,
    borderRadius: 18, alignItems: 'center', justifyContent: 'center', marginBottom: 12,
  },
  iconText: { fontSize: 32 },
  appName: { fontSize: 26, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  tagline: { fontSize: 13, color: Colors.TEXT_SECONDARY, marginTop: 4 },
  form: { gap: 8 },
  label: { fontSize: 12, fontWeight: '800', color: Colors.TEXT_PRIMARY, marginTop: 8 },
  input: {
    height: 48,
    backgroundColor: Colors.SURFACE_DARK,
    borderWidth: 1, borderColor: Colors.BORDER,
    borderRadius: 10, paddingHorizontal: 16,
    fontSize: 14, color: Colors.TEXT_PRIMARY,
  },
  errorText: { fontSize: 12, color: Colors.DANGER, marginTop: 4 },
  button: {
    height: 52, backgroundColor: Colors.PRIMARY,
    borderRadius: 10, alignItems: 'center', justifyContent: 'center', marginTop: 20,
  },
  buttonDisabled: { backgroundColor: Colors.BORDER },
  buttonText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
  linkRow: { flexDirection: 'row', justifyContent: 'center', marginTop: 16 },
  linkText: { fontSize: 13, color: Colors.TEXT_SECONDARY },
  linkBold: { fontWeight: '800', color: Colors.PRIMARY },
})
