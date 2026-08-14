# Техническая реализация и Архитектура

**Документ:** `docs/architecture/edi-carrier-integration-service-tech-spec.md`
**Статус:** Approved for Development
**Сервис:** edi-carrier-integration-service (Поддомен: Warehouse Cross-Docking & B2B EDI)

## 1. Технологический Стек и Зависимости

| Компонент | Технология | Назначение / Обоснование |
| :--- | :--- | :--- |
| **Runtime** | Java 21 (Virtual Threads) | Управление параллельными сетевыми соединениями AS2/SFTP. |
| **Framework** | Apache Camel / Spring Boot 3.3+ | Промышленный движок интеграций и маршрутизации (Camel AS2, Camel SFTP, Camel Bindy). |
| **EDI Parsing** | Smooks / StaEDI | Промышленные Java-библиотеки для ультра-быстрой трансформации EDIFACT/X12 в Java DTO. |
| **Cryptography** | Bouncy Castle (JCE Provider) | Реализация алгоритмов S/MIME шифрования, электронных цифровых подписей и обработки X.509 сертификатов. |
| **Primary Database** | PostgreSQL 16 | Хранение профилей партнеров, метаданных обмена и статусов MDN. |
| **Storage** | MinIO (S3) | Неизменяемое архивное хранение сырых EDI-файлов (.edi, .x12). |
| **Secrets Manager** | HashiCorp Vault | Безопасное хранение приватных ключей и PKI-сертификатов партнеров. |

## 2. Архитектура Маршрутизации и Трансформации (Camel Route Pipeline)

Интеграционный конвейер строится на базе Apache Camel Enterprise Integration Patterns (EIP):

```text
 [Входящий AS2/SFTP запрос]
             │
             ▼
 [1. Camel AS2 Endpoint] ──► [2. S/MIME Decrypt & Verify] ──► [3. Smooks EDIFACT Parser] ──► [4. Canonical Data Mapper] ──► [5. Kafka Producer]
   (Receive raw bytes)         (Bouncy Castle / Vault)         (Transform EDI to DTO)          (Map DTO to Avro Event)        (Publish to Kafka)
```

*   **AS2 Receiver Endpoint:** Принимает зашифрованный HTTP POST запрос от партнера, моментально генерирует и возвращает подписанный асинхронный/синхронный MDN-чек.
*   **Security Filter:** Извлекает публичный сертификат партнера из HashiCorp Vault, проверяет подпись S/MIME и расшифровывает тело файла приватным ключом платформы.
*   **Smooks / StaEDI Engine:** Выполняет синтаксический анализ специфичной структуры EDIFACT (сегменты UNH, BGM, DTM, NAD).
*   **Canonical Mapper (MapStruct):** Трансформирует специфичные поля EDIFACT в единое внутреннее Avro-событие нашей платформы.

## 3. Межсервисное Взаимодействие и Интеграции

### 3.1 Схема интеграционных связей

```text
┌─────────────────────────────┐          ┌─────────────────────────────┐
│  Внешние партнеры (Maersk,  │          │   HashiCorp Vault (Secrets) │
│  Customs, Ports, Rail)      │          └──────────────▲──────────────┘
└──────────────┬──────────────┘                         │
               │ AS2 / SFTP                             │ Fetch Private Keys / Certs
               ▼                                        │
┌──────────────┴──────────────┐                         │
│edi-carrier-integration-serv ├─────────────────────────┘
└──────────────┬──────────────┘
               │
               │ Kafka: ExternalCargoDispatchedEvent / ExternalCustomsClearedEvent
               ▼
┌─────────────────────────────┐
│   warehouse-crossdock-serv  │
└─────────────────────────────┘
```

### 3.2 Описание контрактов взаимодействия

**Внешние протокольные интерфейсы:**
*   `POST /as2/inbound` — входной HTTP-эндпоинт для приема AS2 сообщений от внешних контрагентов.
*   `sftp://partner-carrier.com/outbound` — SFTP-клиент для периодического вычитывания или загрузки файлов по расписанию.

**Исходящие события (Kafka Producers):**
*   `ExternalCargoDispatchedEvent`: Потребляется сервисами платформы при получении EDIFACT DESADV файла от внешнего порта.
*   `ExternalCustomsClearedEvent`: Генерируется при успешном прохождении таможенной очистки по данным сообщения от государственной таможенной системы.

## 4. Требования к Безопасности и Юридической Значимости

### 4.1 Безопасность и PKI (Public Key Infrastructure)
*   Все SSL/TLS и S/MIME сертификаты партнеров хранятся в HashiCorp Vault.
*   Каждые 24 часа задействуется фоновый процесс проверки срока годности сертификатов (Certificate Expiry Monitor). Если до окончания срока жизни ключа партнера остается менее 30 дней, создается инженерный алерт.

### 4.2 Неотрекаемость (Non-Repudiation)
Для решения юридических споров с перевозчиками ("Вы не отправляли инструкцию на погрузку!"):
*   Каждое отправленное EDIFACT-сообщение и полученный ответный MDN-чек с цифровой подписью партнера намертво связываются по Control Number и сохраняются в S3 (MinIO) в виде immutable-архива со сроком хранения 3 года.

## 5. Observability и Эксплуатация

*   **Metrics (Micrometer + Prometheus):**
    *   `edi_messages_processed_total` (counter с тегами `partner=maersk|customs`, `direction=inbound|outbound`, `status=success|failed`) — общий объем EDI-обмена.
    *   `edi_mdn_pending_count` (gauge) — количество отправленных сообщений, ожидающих получения подтверждения MDN.
    *   `edi_parsing_duration_seconds` (histogram) — скорость распарсивания Smooks/StaEDI файлов.
*   **Logging:** Расширенное структурированное логирование с обязательным выводом `edi_control_number`, `partner_as2_id` и `trace_id`.
