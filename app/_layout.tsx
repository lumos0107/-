import { Stack } from 'expo-router'
import { StatusBar } from 'expo-status-bar'
import * as SplashScreen from 'expo-splash-screen'
import { useEffect } from 'react'
import { SafeAreaProvider } from 'react-native-safe-area-context'

SplashScreen.preventAutoHideAsync()

export default function RootLayout() {
  useEffect(() => {
    SplashScreen.hideAsync()
  }, [])

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
        <Stack.Screen name="running/ready" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="running/active" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="running/complete" options={{ headerShown: false, animation: 'slide_from_bottom' }} />
        <Stack.Screen name="running/feedback" options={{ headerShown: false, presentation: 'transparentModal', animation: 'fade' }} />
        <Stack.Screen name="running/weather" options={{ headerShown: false, presentation: 'transparentModal', animation: 'fade' }} />
      </Stack>
    </SafeAreaProvider>
  )
}
