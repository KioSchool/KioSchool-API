# Kio School Api - 키오스쿨 API

## 개발 환경 세팅

### 1. Docker 설치

- `brew install docker` 명령어로 설치
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)에서 Docker Desktop 설치

### 2. 로컬 DB 세팅

- `src/main/resources/db/docker` 디렉토리에서 `docker compose up -d --build` 실행
- PostgreSQL 17(ko_KR 로케일, `localhost:2345`)과 Redis(`localhost:6379`) 컨테이너가 뜬다
- prod와 같은 PostgreSQL 메이저 버전(17)을 쓴다. 버전이 다르면 prod 백업 덤프를 로컬에 복원할 수 없다

### 3. 로컬 서버 실행

- 서버를 실행시키면 liquibase를 통해 DB 마이그레이션 실행됨
- 제대로 실행되었다면 table이 생성된 것을 확인할 수 있음

  ![Database table.png](src%2Fmain%2Fresources%2Freadme%2FDatabase%20table.png)

### 4. 필수 데이터 추가

- `src/main/resources/db/data.sql` 파일을 참고하여 필수 데이터 추가