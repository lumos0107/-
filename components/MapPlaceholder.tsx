import React from 'react'
import { View, Text, StyleSheet } from 'react-native'

interface Props {
  height?: number
}

export default function MapPlaceholder({ height = 400 }: Props) {
  return (
    <View style={[styles.container, { height }]}>
      <Text style={styles.text}>지도</Text>
      <Text style={styles.sub}>Mapbox 연동 후 표시됩니다</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#d1d5db',
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    fontSize: 16,
    color: '#6b7280',
    fontWeight: '600',
  },
  sub: {
    fontSize: 11,
    color: '#9ca3af',
    marginTop: 4,
  },
})
