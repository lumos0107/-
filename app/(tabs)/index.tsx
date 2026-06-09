import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { router } from 'expo-router'
import { Activity } from 'lucide-react-native'
import { Colors } from '../../constants/Colors'

export default function RunningTabScreen() {
  function handleStart() {
    router.push({
      pathname: '/running',
      params: { courseId: '0', courseName: '자유 달리기', distance: '5000', duration: '1800' },
    })
  }

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.container}>
        <Activity color={Colors.PRIMARY} size={48} />
        <Text style={styles.title}>달리기 시작</Text>
        <Text style={styles.sub}>코스 없이 자유롭게 달려요</Text>
        <TouchableOpacity style={styles.btn} onPress={handleStart}>
          <Text style={styles.btnText}>시작</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: Colors.BACKGROUND },
  container: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 16 },
  title: { fontSize: 22, fontWeight: '900', color: Colors.TEXT_PRIMARY },
  sub: { fontSize: 14, color: Colors.TEXT_SECONDARY },
  btn: {
    marginTop: 8, width: 160, height: 52,
    backgroundColor: Colors.PRIMARY, borderRadius: 14,
    alignItems: 'center', justifyContent: 'center',
  },
  btnText: { fontSize: 16, fontWeight: '900', color: Colors.TEXT_WHITE },
})
