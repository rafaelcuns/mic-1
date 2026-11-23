# mic-1
Projeto simulador de MIC-1 para disciplina de Arquitetura de Computadores do professor Vinod Rabello

## Tecnologias

- **Java 21 (JDK 21)** 
- **JavaFX 21.0.2:** Interface gráfica
- **Apache Maven 3.9.9:** Dependências, build e execução
- **Docker:** Containerização da aplicação gráfica

## Instalação e execução

### 1. Instalar Java JDK 21

```powershell
winget install --id Microsoft.OpenJDK.21 -e
```

### 2. Instalar Maven 3.9.9

```powershell
$mavenVersion = "3.9.9"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$installDir = "$env:LOCALAPPDATA\Programs"
$zipPath = "$env:TEMP\maven.zip"

New-Item -ItemType Directory -Force -Path $installDir | Out-Null
Invoke-WebRequest -Uri $mavenUrl -OutFile $zipPath
Expand-Archive -Path $zipPath -DestinationPath $installDir -Force
Remove-Item $zipPath

$javaHome = "C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot"
[Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "User")

$mavenBin = "$installDir\apache-maven-$mavenVersion\bin"
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
[Environment]::SetEnvironmentVariable("Path", "$javaHome\bin;$mavenBin;$currentPath", "User")
```

### 3. Validar instalção

```powershell
java -version
mvn -version
```

### 4. Executar

```powershell
mvn clean javafx:run
```