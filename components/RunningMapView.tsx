import React, { useRef, useEffect } from 'react'
import { StyleSheet } from 'react-native'
import WebView from 'react-native-webview'
import { RoutePoint } from '../utils/api'
import { getRunningMapHtml } from '../utils/mapHtml'

const MAPBOX_TOKEN = process.env.EXPO_PUBLIC_MAPBOX_TOKEN ?? ''

interface Props {
  points: RoutePoint[]
  currentLocation: RoutePoint | null
}

export default function RunningMapView({ points, currentLocation }: Props) {
  const webViewRef = useRef<WebView>(null)

  useEffect(() => {
    if (!currentLocation || !webViewRef.current) return
    webViewRef.current.injectJavaScript(`
      updatePosition(${currentLocation.latitude}, ${currentLocation.longitude});
      true;
    `)
  }, [currentLocation])

  return (
    <WebView
      ref={webViewRef}
      style={styles.map}
      source={{ html: getRunningMapHtml(MAPBOX_TOKEN, points) }}
      originWhitelist={['*']}
      scrollEnabled={false}
      javaScriptEnabled
    />
  )
}

const styles = StyleSheet.create({
  map: { flex: 1, width: '100%' },
})
