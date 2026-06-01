import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { Colors } from '../constants/Colors'
import { secondsToPace } from '../utils/format'

interface Props {
  km: number
  paceSeconds: number
}

export default function PaceItem({ km, paceSeconds }: Props) {
  return (
    <View style={styles.container}>
      <View style={styles.left}>
        <View style={styles.kmBadge}>
          <Text style={styles.kmNum}>{km}</Text>
        </View>
        <Text style={styles.kmLabel}>{km}km</Text>
      </View>
      <Text style={styles.pace}>{secondsToPace(paceSeconds)}</Text>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: Colors.SURFACE_DARK,
    borderRadius: 8,
    padding: 12,
    marginBottom: 8,
  },
  left: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  kmBadge: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: Colors.PRIMARY,
    alignItems: 'center',
    justifyContent: 'center',
  },
  kmNum: {
    fontSize: 12,
    fontWeight: '900',
    color: Colors.TEXT_WHITE,
  },
  kmLabel: {
    fontSize: 12,
    fontWeight: '900',
    color: Colors.TEXT_PRIMARY,
  },
  pace: {
    fontSize: 16,
    fontWeight: '900',
    color: Colors.TEXT_PRIMARY,
  },
})
