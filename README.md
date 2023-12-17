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