import { Stack, router } from 'expo-router'
import { StatusBar } from 'expo-status-bar'
import * as SplashScreen from 'expo-splash-screen'
import { useEffect, useState } from 'react'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import * as SecureStore from 'expo-secure-store'

SplashScreen.preventAutoHideAsync()

export default function RootLayout() {
  const [ready, setReady] = useState(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)

  useEffect(() => {
    SecureStore.getItemAsync('userId')
      .then(val => setIsLoggedIn(!!val))
      .catch(() => setIsLoggedIn(false))
      .finally(() => {
        SplashScreen.hideAsync()
        setReady(true)
      })
  }, [])

  useEffect(() => {
    if (!ready) return
    if (!isLoggedIn) {
      router.replace('/auth/login')
    }
  }, [ready, isLoggedIn])

  return (
    <SafeAreaProvider>
      <StatusBar style="dark" backgroundColor="#ffffff" />
      <Stack
        screenOptions={{
          contentStyle: { backgroundColor: '#f3f4f6' },
        }}
      >
        <Stack.Screen name="auth/login" options={{ headerShown: false }} />
        <Stack.Screen name="auth/register" options={{ headerShown: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="running/index" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="running/weather" options={{ headerShown: false, presentation: 'transparentModal', animation: 'fade' }} />
      </Stack>
    </SafeAreaProvider>
  )
}
