import React from 'react'
import { View, Text, StyleSheet } from 'react-native'
import { Colors } from '../constants/Colors'

interface Props {
  label: string
  value: string
  unit?: string
}

export default function StatBox({ label, value, unit }: Props) {
  return (
    <View style={styles.container}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.row}>
        <Text style={styles.value}>{value}</Text>
        {unit && <Text style={styles.unit}>{unit}</Text>}
      </View>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.SURFACE_DARK,
    borderRadius: 8,
    paddingVertical: 10,
    alignItems: 'center',
  },
  label: {
    fontSize: 10,
    color: Colors.TEXT_SECONDARY,
    fontWeight: '800',
    marginBottom: 4,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 2,
  },
  value: {
    fontSize: 16,
    fontWeight: '900',
    color: Colors.TEXT_PRIMARY,
  },
  unit: {
    fontSize: 10,
    color: Colors.TEXT_SECONDARY,
  },
})
