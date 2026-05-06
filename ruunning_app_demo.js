import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Running from './pages/Running'
import CourseRecommend from './pages/CourseRecommend'
import Navbar from './components/common/Navbar'
import useStore from './store'

// 로그인 안 했으면 /login으로 리다이렉트
function PrivateRoute({ children }) {
  const user = useStore((s) => s.user)
  return user ? children : <Navigate to="/login" />
}

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/recommend" element={<PrivateRoute><CourseRecommend /></PrivateRoute>} />
        <Route path="/" element={<PrivateRoute><Running /></PrivateRoute>} />
        <Route path="*" element={<Navigate to="/login" />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App