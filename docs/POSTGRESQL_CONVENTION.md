# PostgreSQL 資料庫設計與操作規範 (PostgreSQL Specifications)

本文件為 **Conflux** 專案的 **PostgreSQL 資料庫 Schema 設計、索引優化、SQL DDL 變更與維運規範**。
本規範專注於純 **SQL與 PostgreSQL 資料庫層級** 的標準，不依賴任何特定的程式語言或框架。所有資料庫設計與 SQL 撰寫請嚴格遵守以下規範。

> 💡 **相關規範文件**：
> - Git 協作與 Commit 規範：[GIT_CONVENTION.md](file:///home/user/AndroidStudioProjects/Conflux/docs/GIT_CONVENTION.md)

---

## 目錄 (Table of Contents)
- [一、 設計基本原則 (General Principles)](#一-設計基本原則-general-principles)
- [二、 命名規範 (Naming Conventions)](#二-命名規範-naming-conventions)
  - [1. 資料庫與 Schema 命名](#1-資料庫與-schema-命名)
  - [2. 資料表命名 (Tables)](#2-資料表命名-tables)
  - [3. 欄位命名 (Columns)](#3-欄位命名-columns)
  - [4. 索引與約束命名 (Indexes & Constraints)](#4-索引與約束命名-indexes--constraints)
- [三、 資料型別選用規範 (Data Type Guidelines)](#三-資料型別選用規範-data-type-guidelines)
  - [1. 主鍵 (Primary Key)](#1-主鍵-primary-key)
  - [2. 字串 (Strings)](#2-字串-strings)
  - [3. 日期與時間 (Date & Time)](#3-日期與時間-date--time)
  - [4. JSON / 半結構化資料 (JSONB)](#4-json--半結構化資料-jsonb)
  - [5. 數值與布林 (Numbers & Booleans)](#5-數值與布林-numbers--booleans)
  - [6. 狀態與列舉 (Choices / Enums)](#6-狀態與列舉-choices--enums)
- [四、 索引與查詢優化 (Indexing & Performance)](#四-索引與查詢優化-indexing--performance)
  - [1. 外鍵索引 (Foreign Key Indexing)](#1-外鍵索引-foreign-key-indexing)
  - [2. 複合索引與最左前綴原則 (Composite Index)](#2-複合索引與最左前綴原則-composite-index)
  - [3. 部分索引 (Partial Index)](#3-部分索引-partial-index)
  - [4. GIN / Full-Text Search 索引](#4-gin--full-text-search-索引)
- [五、 DDL 變更與零停機規範 (Schema Migration & DDL)](#五-ddl-變更與零停機規範-schema-migration--ddl)
  - [1. 零停機 Schema 變更 (Zero-downtime DDL)](#1-零停機-schema-變更-zero-downtime-ddl)
  - [2. 線上併發建立索引 (Concurrent Indexing)](#2-線上併發建立索引-concurrent-indexing)
  - [3. 軟刪除設計 (Soft Delete)](#3-軟刪除設計-soft-delete)
- [六、 交易控制與連線管理 (Transactions & Connection Management)](#六-交易控制與連線管理-transactions--connection-management)
- [七、 安全性與效能調校 (Security & Performance Tuning)](#七-安全性與效能調校-security--performance-tuning)

---

## 一、 設計基本原則 (General Principles)

1. **資料完整性由 DB 層把關**：資料的關聯約束 (Foreign Key)、唯一性 (Unique) 與邊界檢查 (Check Constraint) 必須在 PostgreSQL 資料庫層建立，不應單純依賴應用程式。
2. **統一小寫蛇形命名**：所有 SQL 關鍵字雖然不區分大小寫，但資料庫物件名稱（Table, Column, Index, Constraint）一律使用小寫蛇形命名法 (`snake_case`)。
3. **明確時區與型別**：時間型別必須強制使用 `TIMESTAMPTZ` (UTC)，JSON 欄位必須使用 `JSONB`。
4. **控制寫入與索引成本**：依據查詢情境（如精確查詢、範圍查詢、排序）合理建立索引，避免無效與過度索引影響 `INSERT/UPDATE` 效能。

---

## 二、 命名規範 (Naming Conventions)

所有資料庫物件一律使用 **小寫蛇形命名法 (`snake_case`)**，禁止使用大寫字母、連字號 (`-`) 或 SQL 預留關鍵字（如 `select`, `order`, `user`, `group`）。

### 1. 資料庫與 Schema 命名

* **資料庫 (Database)**：小寫蛇形命名，如 `conflux_dev`, `conflux_prod`。
* **Schema**：預設使用 `public`，業務模組隔離時可自訂 schema（如 `auth`, `chat`）。

### 2. 資料表命名 (Tables)

* **名詞複數**：資料表名稱統一採用複數小寫名詞（例如 `users`, `channels`, `messages`）。
* **多對多中繼表 (Junction Table)**：採用被關聯之兩資料表單/複數組合，以 `_` 連接（例如 `channel_members`）。

#### **建表 SQL 範例**：
```sql
CREATE TABLE channels (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    topic TEXT,
    is_private BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 3. 欄位命名 (Columns)

* **主鍵 (Primary Key)**：統一命名為 `id`。
* **外鍵 (Foreign Key)**：統一命名為 `<target_singular_entity>_id`（例如 `channel_id`, `sender_id`）。
* **布林欄位 (Booleans)**：必須包含 `is_`, `has_`, `can_` 前綴（例如 `is_active`, `is_deleted`, `has_permission`）。
* **時間戳記 (Timestamps)**：
  * 建立時間：`created_at`
  * 更新時間：`updated_at`
  * 刪除時間：`deleted_at`
  * 動作時間：`last_login_at`, `read_at`

### 4. 索引與約束命名 (Indexes & Constraints)

自訂索引與約束時應遵循以下語義化前綴：

| 物件類型 | 命名格式前綴 | 範例 SQL |
| :--- | :--- | :--- |
| 普通索引 (Index) | `idx_<table_name>_<column1>_<column2>` | `idx_messages_channel_id_created_at` |
| 唯一索引 / 約束 (Unique) | `uk_<table_name>_<column1>_<column2>` | `uk_channel_members_channel_id_user_id` |
| 外鍵約束 (Foreign Key) | `fk_<source_table>_<target_table>` | `fk_messages_channels` |
| 檢查約束 (Check) | `chk_<table_name>_<condition>` | `chk_users_age_positive` |

---

## 三、 資料型別選用規範 (Data Type Guidelines)

### 1. 主鍵 (Primary Key)

* **自增數值型主鍵 (推薦)**：使用 `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY` (64-bit 整數)，效能與記憶體空間最佳。
* **分散式 UUID 主鍵**：在需對外隱藏遞增順序或分散式主鍵生成時，使用 `UUID PRIMARY KEY`（預設可用 `gen_random_uuid()`）。

```sql
-- 方式 A：Identity (推薦)
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY

-- 方式 B：UUID
id UUID PRIMARY KEY DEFAULT gen_random_uuid()
```

### 2. 字串 (Strings)

* **長度預期可界定**：使用 `VARCHAR(n)`（例如 Email `VARCHAR(255)`、MD5/SHA256 Hash `VARCHAR(64)`）。
* **長度動態/長文本**：使用 `TEXT`。PostgreSQL 的 `TEXT` 與 `VARCHAR` 底層儲存與查詢效能完全一致。

### 3. 日期與時間 (Date & Time)

* **必須使用 `TIMESTAMPTZ` (`timestamp with time zone`)**：
  * 嚴禁使用無時區的 `TIMESTAMP`。
  * 儲存與傳輸時一律以 **UTC** 為標準。
* **日期與時間預設值**：使用 `DEFAULT CURRENT_TIMESTAMP` 或 `DEFAULT clock_timestamp()`。

```sql
created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
```

### 4. JSON / 半結構化資料 (JSONB)

* **必須使用 `JSONB`**：
  * `JSONB` 為二進位二元剖析格式，支援索引 (GIN) 與 JSON Path 操作；`JSON` 純文字格式效率低下，禁止使用。
* 僅用於非核心關聯的動態設定、擴充屬性或 Hook 資料。

```sql
extra_metadata JSONB NOT NULL DEFAULT '{}'::jsonb
```

### 5. 數值與布林 (Numbers & Booleans)

* **整數**：`SMALLINT` (2 bytes), `INT` / `INTEGER` (4 bytes), `BIGINT` (8 bytes)。
* **精確數值 (金融/貨幣)**：必須使用 `NUMERIC(precision, scale)`，禁止使用 `REAL` / `DOUBLE PRECISION`。
* **布林型別**：必須使用 `BOOLEAN` (`true` / `false`)，禁止以 `SMALLINT` (`0`/`1`) 代替。

### 6. 狀態與列舉 (Choices / Enums)

* 推薦採用 `VARCHAR(32)` 配合 `CHECK` 約束：
```sql
type VARCHAR(32) NOT NULL DEFAULT 'text' 
    CONSTRAINT chk_messages_type CHECK (type IN ('text', 'image', 'file', 'system'))
```
* 或使用 PostgreSQL 原生 Enum 型別：
```sql
CREATE TYPE message_type AS ENUM ('text', 'image', 'file', 'system');
```

---

## 四、 索引與查詢優化 (Indexing & Performance)

### 1. 外鍵索引 (Foreign Key Indexing)

* **所有 Foreign Key 欄位必須獨立建立 B-Tree 索引**，提升 `JOIN` 效能並防止外鍵檢查或級聯操作時的全表掃描 (Table Scan)。

```sql
CREATE INDEX idx_messages_channel_id ON messages (channel_id);
```

### 2. 複合索引與最左前綴原則 (Composite Index)

* 當查詢語句包含多個 `WHERE` 條件或 `WHERE` + `ORDER BY` 時，建立複合索引。
* **最左前綴原則**：等值查詢 (`=`) 欄位放在最左側，範圍查詢 (`>`, `<`) 或排序 (`ORDER BY`) 欄位放在右側。

```sql
-- 適用查詢：WHERE channel_id = 10 ORDER BY created_at DESC
CREATE INDEX idx_messages_channel_created 
ON messages (channel_id, created_at DESC);
```

### 3. 部分索引 (Partial Index)

* 當資料表相當龐大，且多數查詢僅過濾特定條件（如未刪除資料）時，建立部分索引以節省記憶體與維護成本：

```sql
-- 僅對未刪除的資料建立索引
CREATE INDEX idx_active_users_email 
ON users (email) 
WHERE is_deleted = false;
```

### 4. GIN / Full-Text Search 索引

* 針對 `JSONB` 欄位或全文檢索 (`tsvector`) 使用 GIN 索引 (Generalized Inverted Index)：

```sql
-- JSONB 欄位 GIN 索引
CREATE INDEX idx_messages_metadata_gin 
ON messages USING GIN (extra_metadata);

-- 全文檢索 GIN 索引
CREATE INDEX idx_messages_content_search 
ON messages USING GIN (to_tsvector('english', content));
```

---

## 五、 DDL 變更與零停機規範 (Schema Migration & DDL)

### 1. 零停機 Schema 變更 (Zero-downtime DDL)

在大資料量環境下，直連執行阻塞性 DDL 會引發長時間的 Access Exclusive Lock：

1. **新增 `NOT NULL` 欄位**：
   * 步驟 1：新增可為 NULL 的欄位。
     ```sql
     ALTER TABLE users ADD COLUMN phone VARCHAR(32);
     ```
   * 步驟 2：背景分批更新舊資料預設值。
   * 步驟 3：加上 `NOT NULL` 約束。
     ```sql
     ALTER TABLE users ALTER COLUMN phone SET NOT NULL;
     ```
2. **刪除或改名欄位**：採用 Expand-Contract 策略（先增新欄位，雙寫更新後再刪除舊欄位）。

### 2. 線上併發建立索引 (Concurrent Indexing)

生產環境線上建立索引時，**必須使用 `CONCURRENTLY` 關鍵字**，確保不會阻塞 `INSERT/UPDATE/DELETE` 操作：

```sql
-- CONCURRENTLY 無法在 Transaction 內執行
CREATE INDEX CONCURRENTLY idx_messages_created_at 
ON messages (created_at);
```

### 3. 軟刪除設計 (Soft Delete)

所有核心業務資料表統一設計 `is_deleted` 與 `deleted_at` 欄位實作軟刪除：

```sql
ALTER TABLE messages 
ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN deleted_at TIMESTAMPTZ;
```

---

## 六、 交易控制與連線管理 (Transactions & Connection Management)

1. **交易隔離層級 (Transaction Isolation)**：
   * 預設使用 PostgreSQL 的 `READ COMMITTED`。
   * 涉及防範 Race Condition (如庫存扣減、重複扣款) 時，使用 `SELECT ... FOR UPDATE` 或 `SERIALIZABLE`。
2. **明確交易邊界**：
   * 寫入操作必須包裹於 `BEGIN; ... COMMIT;` 內。
   * 保持交易儘可能簡短，嚴禁在 SQL 交易期間等待外部應用程式網路 Response。
3. **連線池 (Connection Pooling)**：
   * 必須使用 PgBouncer 進行 Transaction-level 或是 Session-level 連線池管理，防範大量併發請求耗盡 PostgreSQL 記憶體。

---

## 七、 安全性與效能調校 (Security & Performance Tuning)

### 1. 權限最小化 (Least Privilege)

* 業務應用程式帳號禁止擁有 `SUPERUSER` 權限。
* 針對不同角色精準授權 DML / DDL：

```sql
-- 建立應用程式專用 Role
CREATE ROLE conflux_app WITH LOGIN PASSWORD 'strong_password';

-- 授權表操作權限
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO conflux_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO conflux_app;
```

### 2. 慢查詢與執行計畫分析 (Slow Query Log & EXPLAIN)

* 設定 `log_min_duration_statement = 250`（自動紀錄執行時間超過 250ms 的 SQL）。
* 針對慢查詢使用 `EXPLAIN (ANALYZE, BUFFERS)` 分析執行計畫：

```sql
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM messages 
WHERE channel_id = 100 
ORDER BY created_at DESC 
LIMIT 20;
```
