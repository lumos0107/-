import { Tabs } from 'expo-router'
import { Activity, Map, ClipboardList, Star } from 'lucide-react-native'
import { Colors } from '../../constants/Colors'

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: Colors.NAVBAR_BG,
          borderTopColor: Colors.BORDER,
          height: 64,
          paddingBottom: 8,
          paddingTop: 4,
        },
        tabBarActiveTintColor: Colors.PRIMARY,
        tabBarInactiveTintColor: Colors.TEXT_SECONDARY,
        tabBarLabelStyle: { fontSize: 10, fontWeight: '600', marginTop: 2 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: '달리기',
          tabBarIcon: ({ color, size }) => <Activity color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="course/index"
        options={{
          title: '코스생성',
          tabBarIcon: ({ color, size }) => <Map color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="record/index"
        options={{
          title: '기록',
          tabBarIcon: ({ color, size }) => <ClipboardList color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="saved/index"
        options={{
          title: '저장루트',
          tabBarIcon: ({ color, size }) => <Star color={color} size={size} />,
        }}
      />
      {/* 서브 라우트 — 탭 바에서 숨김 */}
      <Tabs.Screen name="course/detail" options={{ href: null }} />
      <Tabs.Screen name="course/recommend" options={{ href: null }} />
      <Tabs.Screen name="record/[id]" options={{ href: null }} />
    </Tabs>
  )
}
