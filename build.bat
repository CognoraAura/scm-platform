set JAVA_HOME=C:\Users\Deng\.jdks\graalvm-jdk-21.0.8
set PATH=%JAVA_HOME%\bin;%PATH%
call "C:\Users\Deng\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd" clean compile -f com.scm.parent/pom.xml -DskipTests
