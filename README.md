# mic-1
Projeto simulador de MIC-1 para disciplina de Arquitetura de Computadores do professor Vinod Rabello

## Tecnologias

- **Java 21 (JDK 21)**
- **JavaFX 21.0.2:** Interface gráfica
- **Apache Maven 3.9.9:** Dependências, build e execução
- **Docker & Docker Compose:** Containerização da aplicação gráfica (X11/GTK)

## Como Clonar e Executar via Docker

### 1. Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO>
cd mic-1
```

### 2. Executar com Docker Compose

**Linux / Windows (WSL2 com WSLg):**
```bash
xhost +local:docker
docker compose up --build
```

**Windows (PowerShell sem WSLg, usando [VcXsrv](https://sourceforge.net/projects/vcxsrv/)):**
1. Abra o **XLaunch** (VcXsrv) e marque a opção **"Disable access control"**.
2. Execute no PowerShell:
```powershell
docker compose up --build
```

### Alternativa: Executar com Docker CLI (`docker build` / `docker run`)

**1. Construir a imagem:**
```bash
docker build -t mic1-sim .
```

**2. Executar a interface gráfica:**

* **Linux / WSL2 (WSLg):**
  ```bash
  xhost +local:docker
  docker run --rm -it \
    -e DISPLAY=$DISPLAY \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    mic1-sim
  ```
* **Windows (PowerShell com VcXsrv ativo):**
  ```powershell
  docker run --rm -it -e DISPLAY=host.docker.internal:0.0 mic1-sim
  ```
