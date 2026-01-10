FROM eclipse-temurin:17-jdk

# 타임존 설정
RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime

# 빌드 아티팩트 복사
COPY build/libs/*SNAPSHOT.jar calio.jar

# 엔트리포인트 설정
ENTRYPOINT ["java","-jar","calio.jar"]