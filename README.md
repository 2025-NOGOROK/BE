# 🌌 NOGOROK: 당신의 스트레스 관리 & AI 웰빙 도우미

## 🌟 프로젝트 소개
NOGOROK은 본 프로젝트는 갤럭시 워치의 실시간 심박 기반 스트레스 지수와 구글 캘린더 일정을 통합 분석하여, 
사용자 상태에 맞는 휴식 콘텐츠를 자동 추천하는 AI 웰빙 서비스이다. 

"버튼 한 번으로 개인 맞춤형 시나리오형 휴식 루틴을 제안한다."

<img width="1080" height="1350" alt="image" src="https://github.com/user-attachments/assets/69b7b0a9-8e89-465c-a7d2-b8607344bf28" />
<img width="1080" height="1350" alt="image" src="https://github.com/user-attachments/assets/f20018f0-a966-42e8-aa5b-d1e29d550f1c" />
<img width="1080" height="1350" alt="image" src="https://github.com/user-attachments/assets/e06e01ff-4ae6-43a6-b2cf-cb92d3dd8b17" />
<img width="1080" height="1350" alt="image" src="https://github.com/user-attachments/assets/1a93d0c0-860d-4bd7-a690-6d2f57d4abe2" />

## 🏗️ 아키텍쳐
<img width="2355" height="1359" alt="image" src="https://github.com/user-attachments/assets/9bd56072-438a-47c0-b59a-c8a9bdfd0103" />

## ⚙️ 기술 스택 (Tech Stack)
| 구분 | 기술 스택 | 버전 | 역할 및 사용 목적 |
| :--- | :--- | :--- | :--- |
| **백엔드 코어** | Spring Boot | 3.4.3 (Java 17) | REST API 개발 및 애플리케이션 프레임워크 제공 |
| | Spring Data JPA | 3.4.3 | 데이터베이스(MySQL)와의 객체-관계 매핑(ORM) 및 데이터 접근 관리 |
| | Spring Security | 3.4.3 | 인증 및 인가 처리, OAuth2 클라이언트 기능 지원 |
| **인증/보안** | OAuth2 Client | 3.4.3 | Google 캘린더 등 외부 서비스 연동 및 사용자 인증 처리 |
| | JWT (jjwt) | 0.11.5 | 토큰 기반 인증 구현 (로그인 상태 유지 및 API 접근 제어) |
| | Firebase Admin | 9.2.0 | 푸시 알림 (FCM) 전송 및 사용자 관리 지원 |
| **데이터베이스** | MySQL | 8.0 | 사용자, 일정, 콘텐츠, 스트레스 지수 등 데이터 저장 |
| **외부 API 연동** | Google Calendar API | v3-rev20220715 | 사용자 캘린더 일정 동기화 및 분석 |
| | Google Maps Services | 0.18.0 | 위치 기반 서비스(LBS) 또는 일정의 지리 정보 처리 |
| | Selenium HQ + Jsoup | 4.10.0 / 1.17.2 | 웹 스크래핑 자동화 및 HTML 파싱 |
| **개발 도구** | Lombok | - | 반복적인 자바 코드(Getter, Setter 등) 자동 생성 |
| | Springdoc OpenAPI | 2.7.0 | API 명세 자동화 (Swagger UI) |
| **인프라** | AWS EC2, Docker, Nginx | - | 서버 호스팅, 컨테이너 환경 구축, 리버스 프록시 |
  
## 📄ERD 다이어그램
<img width="332" height="559" alt="image" src="https://github.com/user-attachments/assets/55e5bf71-6645-492e-943d-2e3ec678aaf3" />
