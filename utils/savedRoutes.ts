import * as SecureStore from 'expo-secure-store'
import { RoutePoint } from './api'

export type SavedRoute = {
  id: string
  courseId: number
  name: string
  distanceMeters: number
  estimatedDurationSeconds: number
  points: RoutePoint[]
  savedAt: string
}

const KEY = 'saved_routes'

export async function getSavedRoutes(): Promise<SavedRoute[]> {
  try {
    const val = await SecureStore.getItemAsync(KEY)
    return val ? JSON.parse(val) : []
  } catch {
    return []
  }
}

export async function saveRoute(route: SavedRoute): Promise<void> {
  const routes = await getSavedRoutes()
  const exists = routes.find(r => r.courseId === route.courseId)
  if (exists) return
  routes.unshift(route)
  await SecureStore.setItemAsync(KEY, JSON.stringify(routes))
}

export async function deleteSavedRoute(id: string): Promise<void> {
  const routes = await getSavedRoutes()
  const filtered = routes.filter(r => r.id !== id)
  await SecureStore.setItemAsync(KEY, JSON.stringify(filtered))
}
