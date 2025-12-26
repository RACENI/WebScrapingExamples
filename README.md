# CommunityAnalyzer

Java 21 및 Spring Boot 4.0.1 기반의 커뮤니티 데이터 분석 애플리케이션입니다.  
외부 커뮤니티의 게시글과 댓글 데이터를 수집하여 화면에 표시하고, 게시글–댓글 구조를 기반으로 사용자 반응과 활동 흐름을 확인할 수 있도록 설계되었습니다.

---

## 📌 프로젝트 개요

CommunityAnalyzer는 커뮤니티 게시글과 댓글 데이터를 수집하여 구조적으로 제공하는 백엔드 애플리케이션입니다.  
Spring Boot 아키텍처를 기반으로 하여, 현재는 데이터 조회 중심으로 구성되어 있으며  
향후 분석 및 통계 기능 확장을 고려한 형태로 설계되었습니다.

주요 목적은 다음과 같습니다.

- 커뮤니티 게시글 및 댓글 데이터 조회
- 게시글과 댓글 간 관계 구조화
- 분석 및 시각화 기능 확장을 위한 데이터 기반 구축

---

## 🛠 기술 스택

| 구분 | 기술 |
|----|----|
| Language | Java 21 |
| Framework | Spring Boot 4.0.1 |
| Build Tool | Maven |
| IDE | STS 4.32.2.RELEASE |
| Configuration | application.yml |

---

## 📂 프로젝트 구조

```text
CommunityAnalyzer
 ┣ src
 ┃ ┣ main
 ┃ ┃ ┣ java
 ┃ ┃ ┃ ┗ (도메인 / 서비스 / 컨트롤러 패키지)
 ┃ ┃ ┗ resources
 ┃ ┃   ┗ application.yml
 ┃ ┗ test
 ┣ pom.xml
 ┣ mvnw
 ┣ mvnw.cmd
 ┗ README.md
```

## ⚙ 실행 환경 요구사항

- Java 21 이상
- Maven 3.9 이상
- STS 4.32.2.RELEASE 또는 호환 IDE

---

## ▶ 실행 방법

### 1️⃣ 프로젝트 클론

```bash
git clone https://github.com/RACENI/WebScrapingExamples.git
cd CommunityAnalyzer
```
### 2️⃣ 빌드

```bash
./mvnw clean package
```
Windows 환경에서는 다음 명령을 사용하십시오.

```bash
mvnw.cmd clean package
```
### 3️⃣ 실행

```bash
java -jar target/*.jar
```
또는 STS에서 main 메서드를 직접 실행하시면 됩니다.

---

## 🔧 설정

모든 설정은 application.yml을 통해 관리합니다.

```yaml
spring:
  application:
    name: CommunityAnalyzer
```
환경에 따라 DB, 포트, 로깅 설정을 자유롭게 확장할 수 있습니다.

---

## 🚀 향후 확장 계획

커뮤니티 데이터 수집 로직 고도화

---

## 📄 라이선스

본 프로젝트는 개인 학습 및 연구 목적을 기준으로 작성되었습니다.  
라이선스는 추후 필요 시 명시할 예정입니다.

---

## 🙋 문의

버그 제보, 개선 제안은 Issue 또는 Pull Request로 남겨주시면 감사하겠습니다.

이상입니다.

---

필요한 다른 구간도 같은 규칙으로 바로 처리하겠습니다.
