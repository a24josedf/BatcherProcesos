# BatcherProcesos

Simulador sencillo de un batcher de procesos para Programacion de Servicios y Procesos.

## Que incluye

- Lectura de jobs desde ficheros YAML en la carpeta `jobs/`.
- Validacion de campos obligatorios: `id`, `name`, `priority`, `resources.cpu_cores`, `resources.memory` y `workload.duration_ms`.
- Estados de job: `NEW`, `READY`, `WAITING`, `RUNNING`, `DONE` y `FAILED`.
- Gestion de recursos simulados: cores de CPU y memoria en MB.
- Politicas seleccionables: `FCFS` y `RR`.
- Lanzamiento de workers reales con `ProcessBuilder`.
- Monitor de consola con PID, progreso, recursos y colas.
- Limpieza de procesos hijos al cerrar el batcher.

## Ejecutar desde NetBeans

1. Abre el proyecto como proyecto Maven.
2. Ejecuta la clase `com.mycompany.batcherprocesos.BatcherProcesos`.
3. Responde al menu:
   - carpeta de jobs: `jobs`
   - cores simulados: por ejemplo `4`
   - memoria: por ejemplo `2048`
   - politica: `FCFS` o `RR`
   - quantum: solo se pide para `RR`

## Ejecutar por consola

Si Maven esta disponible:

```powershell
mvn compile exec:java -Dexec.args="jobs 4 2048 FCFS 200"
```

Tambien se puede compilar directamente con Java:

```powershell
New-Item -ItemType Directory -Force target\classes | Out-Null
javac -encoding UTF-8 -d target\classes (Get-ChildItem -Recurse -Filter *.java src\main\java | ForEach-Object { $_.FullName })
java -cp target\classes com.mycompany.batcherprocesos.BatcherProcesos jobs 4 2048 FCFS 200
```

Para Round Robin:

```powershell
java -cp target\classes com.mycompany.batcherprocesos.BatcherProcesos jobs 4 2048 RR 200
```

## Formato de job

```yaml
id: job-0007
name: "Indexar informes"
priority: 2
resources:
  cpu_cores: 1
  memory: "256 MB"
workload:
  duration_ms: 15000
```

La memoria puede escribirse en `MB` o `GB`. Internamente se guarda siempre en MB.
