import React, { useState } from 'react'
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, KeyboardAvoidingView, Platform, ScrollView,
} from 'react-native'
import { router } from 'expo-router'
import { ArrowLeft } from 'lucide-react-native'
import { Colors } from '../../constants/Colors'

export default function RegisterScreen() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')

  const passwordMismatch = confirm !== '' && password !== confirm
  const canSubmit = email.trim() && password.trim() && password === confirm

  function handleRegister() {
    if (canSubmit) {
      router.replace('/auth/login')
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <TouchableOpacity style={styles.back} onPress={() => router.back()}>
          <ArrowLeft color={Colors.TEXT_PRIMARY} size={24} />
        </TouchableOpacity>

        <Text style={styles.title}>회원가입</Text>

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

          <Text style={styles.label}>비밀번호 확인</Text>
          <TextInput
            style={[styles.input, passwordMismatch && styles.inputError]}
            value={confirm}
            onChangeText={setConfirm}
            placeholder="비밀번호를 다시 입력하세요"
            placeholderTextColor={Colors.TEXT_MUTED}
            secureTextEntry
          />
          {passwordMismatch && (
            <Text style={styles.errorText}>비밀번호가 일치하지 않습니다</Text>
          )}

          <TouchableOpacity
            style={[styles.button, !canSubmit && styles.buttonDisabled]}
            onPress={handleRegister}
            disabled={!canSubmit}
          >
            <Text style={styles.buttonText}>가입하기</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: Colors.CARD },
  container: { flexGrow: 1, paddingHorizontal: 24, paddingTop: 60 },
  back: { marginBottom: 24 },
  title: { fontSize: 24, fontWeight: '900', color: Colors.TEXT_PRIMARY, marginBottom: 32 },
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
  inputError: { borderColor: Colors.DANGER },
  errorText: { fontSize: 11, color: Colors.DANGER, marginTop: 2 },
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
})
