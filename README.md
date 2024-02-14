# Kio School Api - 키오스쿨 API

## 개발 환경 세팅

### 1. Docker 설치

- `brew install docker` 명령어로 설치
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)에서 Docker Desktop 설치

### 2. 로컬 DB 세팅

- 터미널에서 /src/main/resources/db/dockerfile 디렉토리로 이동
- `docker build -t kio-school-db .` 명령어로 도커 이미지 생성
- Docker Desktop에서 생성된 이미지 확인
  ![Docker Desktop Image Check.png](src%2Fmain%2Fresources%2Freadme%2FDocker%20Desktop%20Image%20Check.png)

- Run 버튼 클릭 후 아래와 같이 포트 설정
  ![Run Image.png](src%2Fmain%2Fresources%2Freadme%2FRun%20Image.png)

### 3. 로컬 서버 실행

- 서버를 실행시키면 liquibase를 통해 DB 마이그레이션 실행됨
- 제대로 실행되었다면 table이 생성된 것을 확인할 수 있음

  ![DB Table.png](src%2Fmain%2Fresources%2Freadme%2FDatabase%20Table.png)

### 4. 필수 데이터 추가

- `src/main/resources/db/data.sql` 파일을 참고하여 필수 데이터 추가