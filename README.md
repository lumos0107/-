# 길동무 - 프론트엔드

제주도 러닝 경로 추천 앱의 React Native 프론트엔드입니다.  
GPS 기반 실시간 러닝 추적, 경로 추천, 날씨 정보를 제공합니다.

## 주요 기능

- 러닝 경로 추천 및 지도 시각화 (Mapbox GL JS)
- 실시간 GPS 러닝 추적
- 실시간 날씨 정보 (OpenWeatherMap API)
- 고도 그래프
- 러닝 기록 저장 및 히스토리 조회
- 저장된 경로 관리
- 회원가입 / 로그인

## 기술 스택

| 분류 | 기술 |
|------|------|
| Framework | React Native 0.81.5 |
| Platform | Expo ~54.0.0 |
| Routing | Expo Router (파일 기반) |
| Language | TypeScript |
| 지도 | Mapbox GL JS (WebView) |
| GPS | expo-location |
| 인증 저장 | expo-secure-store |
| 차트 | react-native-svg |

## 화면 구성

```
app/
├── auth/
│   ├── login.tsx       # 로그인
│   └── register.tsx    # 회원가입
├── (tabs)/
│   ├── index.tsx       # 홈
│   ├── course/         # 경로 추천 및 상세
│   ├── record/         # 러닝 기록
│   └── saved/          # 저장된 경로
└── running/
    ├── index.tsx        # 실시간 러닝
    └── weather.tsx      # 날씨 상세
```

## 실행 방법

### 1. 패키지 설치

```bash
npm install
```

### 2. 앱 실행

```bash
# Expo Go 앱으로 실행 (개발)
npm start

# Android
npm run android

# iOS
npm run ios
```

### 3. 백엔드 서버 설정

`utils/api.ts`의 `BASE_URL`을 백엔드 서버 주소로 변경:

```ts
const BASE_URL = 'http://your-server-ip:8080'
```

### 4. 사전 조건

- Node.js 18 이상
- Expo CLI (`npm install -g expo-cli`)
- Expo Go 앱 (모바일) 또는 Android/iOS 에뮬레이터

## 테스트

```bash
npm test
```

## 백엔드 연결

기본 연결 주소: `http://43.200.178.203:8080` (AWS EC2)  
로컬 개발 시 `utils/api.ts`에서 주소 변경 후 사용하세요.
