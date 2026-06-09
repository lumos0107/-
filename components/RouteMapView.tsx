import React from 'react'
import { StyleSheet, View, Text } from 'react-native'
import WebView from 'react-native-webview'
import { RoutePoint } from '../utils/api'
import { getStaticMapHtml } from '../utils/mapHtml'

const MAPBOX_TOKEN = process.env.EXPO_PUBLIC_MAPBOX_TOKEN ?? ''

interface Props {
  points: RoutePoint[]
  anchor?: { latitude: number; longitude: number }
  height?: number
  flex?: boolean
}

export default function RouteMapView({ points, anchor, height = 400, flex = false }: Props) {
  if (points.length < 2) {
    return (
      <View style={[styles.fallback, flex ? { flex: 1 } : { height }]}>
        <Text style={styles.fallbackText}>경로 데이터 없음</Text>
      </View>
    )
  }

  return (
    <WebView
      style={[styles.map, flex ? { flex: 1 } : { height }]}
      source={{ html: getStaticMapHtml(MAPBOX_TOKEN, points, anchor) }}
      originWhitelist={['*']}
      scrollEnabled={false}
      javaScriptEnabled
    />
  )
}

const styles = StyleSheet.create({
  map: { width: '100%' },
  fallback: {
    width: '100%',
    backgroundColor: '#f3f4f6',
    alignItems: 'center',
    justifyContent: 'center',
  },
  fallbackText: { fontSize: 13, color: '#6b7280' },
})
