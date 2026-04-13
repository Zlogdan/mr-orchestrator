# Контекст кода проекта MR-Orchestrator

## Структура проекта

Проект представляет собой Java-приложение, построенное с использованием Maven, для автоматизации обработки Merge Requests в GitLab. Приложение использует JavaFX для пользовательского интерфейса.

### Основные директории:
- `src/main/java/com/mrOrchestrator/`: Исходный код на Java
  - `api/`: Клиент для взаимодействия с GitLab API
    - `model/`: Модели данных (Branch, MergeRequest, Pipeline)
  - `config/`: Конфигурация приложения (AppConfig, ConfigLoader)
  - `service/`: Бизнес-логика (MergeRequestService, ProcessingService)
  - `ui/`: Пользовательский интерфейс JavaFX
    - `model/`: Модели для UI (TableRowModel)
  - `util/`: Утилиты (AppLogger)
- `src/main/resources/`: Ресурсы
  - `logback.xml`: Конфигурация логирования
  - `css/style.css`: Стили для UI
  - `fxml/main.fxml`: Разметка главного окна
- `target/`: Скомпилированные классы и ресурсы (генерируется Maven)

### Ключевые файлы:
- `pom.xml`: Конфигурация Maven с зависимостями (Jackson, JavaFX, Logback и др.)
- `config.yaml`: Конфигурационный файл приложения
- `README.md`: Документация проекта

## Ключевые классы

### GitLabApiClient
**Путь:** `src/main/java/com/mrOrchestrator/api/GitLabApiClient.java`

Класс для взаимодействия с REST API GitLab. Предоставляет методы для:
- Получения списка веток проекта (с пагинацией)
- Поиска веток по имени
- Получения Merge Requests
- Создания новых Merge Requests
- Одобрения и слияния MR
- Получения статуса пайплайнов

Использует `HttpClient` для HTTP-запросов, `ObjectMapper` для сериализации/десериализации JSON. Поддерживает настройку SSL (включая отключение верификации для тестовых сред).

### MergeRequestService
**Путь:** `src/main/java/com/mrOrchestrator/service/MergeRequestService.java`

Сервис для обработки отдельных строк данных (представляющих ветки для слияния). Основные функции:
- Поиск существующего MR или создание нового
- Автоматическое одобрение MR (с ожиданием)
- Слияние MR (с ожиданием завершения)
- Обработка ошибок с повторными попытками

Работает асинхронно, обновляя UI через `Platform.runLater()`. Поддерживает режим "dry-run" для тестирования без реальных изменений.

### MainController
**Путь:** `src/main/java/com/mrOrchestrator/ui/MainController.java`

Контроллер главного окна JavaFX приложения. Управляет:
- Вводом URL репозитория и токена GitLab
- Выбором целевой ветки
- Отображением таблицы с ветками для обработки
- Запуском процесса обработки MR
- Отображением прогресса и результатов

Использует FXML для привязки UI элементов, `TableView` для отображения данных, `ProgressBar` для индикации прогресса. Интегрируется с `ProcessingService` для выполнения фоновых задач.</content>
<parameter name="filePath">c:\Users\Zlo\Documents\Projects\mr-orchestrator\code_context.md