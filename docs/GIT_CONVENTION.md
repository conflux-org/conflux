# Git 協作規範

本文件為 **Conflux (Django + Kotlin Multiplatform Monorepo)** 團隊的 Git 協作標準。為了確保專案歷史乾淨、避免 Merge 衝突，請所有人嚴格遵守以下規範。

---

## 一、 分支策略與命名 (Branch Naming)

所有新功能開發與 Bug 修復都必須建立獨立分支，**絕對禁止直接 Push 至 `master` 分支**。

* **主分支 (`master`)**：僅能透過 Pull Request 合併，必須審核通過且能正常 run 起來。
* **開發分支**：
  * **新功能**：`feature/<scope>-<short-description>`
  * **Bug 修復**：`fix/<scope>-<short-description>`
  * **重構**：`refactor/<scope>-<short-description>`
  * **環境/配置**：`chore/<scope>-<short-description>`

### 命名範例：
* `feature/backend-jwt-auth`
* `feature/frontend-channel-list`
* `fix/websocket-reconnect`
* `chore/docker-config`

---

## 二、 Commit Message 規範 (Conventional Commits)

使用帶有 **Scope（作用域）** 的結構化 Commit，讓團隊成員一眼看懂變動模組。

### 1. 提交格式

```text
<type>(<scope>): <summary>

[optional body]
```

### 2. Type 類型

| Type | 說明 | 範例 |
| :--- | :--- | :--- |
| `feat` | 新增功能 (Feature) | `feat(backend-auth): add JWT refresh token endpoint` |
| `fix` | 修復 Bug | `fix(frontend-ui): resolve layout overlap on mobile screens` |
| `refactor` | 重構程式碼（非修 Bug、非加新功能）| `refactor(common-db): optimize SQL queries for user lookup` |
| `style` | 程式碼格式、標點符號調整（不影響邏輯） | `style(backend-api): reformat code using Black` |
| `perf` | 提高效能的程式碼變動 | `perf(frontend-kmp): cache channel list responses` |
| `test` | 新增或修訂測試案例 | `test(backend-user): add unit tests for user signup` |
| `docs` | 僅修改文件 | `docs(readme): update backend API setup instructions` |
| `chore` | 建置流程、依賴套件更新或輔助工具變更 | `chore(deps): bump Kotlin version to 2.0.0` |
| `ci` | CI/CD 設定檔與腳本變更 | `ci(github-actions): add automated Android build workflow` |

### 3. Scope 作用域建議 (Monorepo)

為了明確區分 **Django 後端** 與 **Kotlin Multiplatform (KMP) 前端/跨端**，請優先選用以下 Scope：

#### **後端 (Django)**
* `backend-api`：REST / GraphQL API 介面變動
* `backend-auth`：身份驗證與權限管理
* `backend-db`：ORM Models、Migrations 變動
* `backend-core`：後端核心邏輯與 Middleware

#### **前端 / 跨端 (KMP)**
* `frontend-ui`：Compose Multiplatform 畫面與 UI 組件
* `frontend-state`：ViewModel、State Management
* `shared-domain`：KMP 共享 Domain/Business Logic
* `shared-data`：KMP 共享 Repository/Network/Database (Ktor, SqlDelight)
* `android` / `ios` / `desktop` / `web`：特定平台專屬程式碼 (Expect/Actual)

#### **全域 / 基礎設施**
* `deps`：跨專案依賴庫與 Gradle/Pip 變更
* `docker`：Docker / Compose 設定
* `ci`：GitHub Actions / GitLab CI 設定
* `docs`：專案文件

---

## 三、 Commit 撰寫良好實踐 (Best Practices)

1. **使用祈使句（現在式）**：
   * ⭕ `feat(backend-auth): add Google OAuth login`
   * ❌ `feat(backend-auth): added Google OAuth login`
   * ❌ `feat(backend-auth): 新增了登入功能` (建議主要使用英文，若團隊約定中文請保持統一規範)
2. **保持 Commit 原子化 (Atomic Commit)**：
   * 一個 Commit 只做一件事。請勿將「修復 Bug」與「格式化全部檔案」混在同一個 Commit。
3. **區分單端與跨端提交**：
   * 盡量避免在同一個 Commit 中同時修改 Django Python 檔與 Kotlin KMP 檔，除非該變動屬於高度綁定的單一 Feature。

---

## 四、 Branch 與 Pull Request (PR) 流程

### 1. 開發流程步驟

1. **切換至最新的 `master` 並拉取最新程式碼**：
   ```bash
   git checkout master
   git pull origin master
   ```
2. **建立專屬開發分支**：
   ```bash
   git checkout -b feature/backend-user-profile
   ```
3. **本地開發與分段 Commit**：
   ```bash
   git add .
   git commit -m "feat(backend-user): add avatar upload endpoint"
   ```
4. **提交前與 `master` 同步 (使用 Rebase)**：
   * 為了保持線性 Commit 歷史，**嚴禁在開發分支執行 `git merge master`**，請一律使用 `rebase`：
   ```bash
   git fetch origin
   git rebase origin/master
   ```
   * 若產生衝突 (Conflict)，解決後執行：
   ```bash
   git add <resolved-files>
   git rebase --continue
   ```
5. **推送到遠端倉庫**：
   ```bash
   git push origin feature/backend-user-profile
   ```

---

## 五、 Pull Request (PR) 審核規範

### 1. PR 標題格式
PR 標題應遵循與 Commit 相同的 Conventional Commit 規範：
* `feat(backend-auth): 支持 JWT Token 刷新機制`
* `fix(android-ui): 修復對話框在高解析度螢幕上的顯示溢出`

### 2. PR 說明範例模板

```markdown
## 📝 變更摘要 (Summary)
- 新增 Django 後端 `/api/v1/auth/refresh/` 路由。
- KMP 共享層整合 Token 自動刷新邏輯 (Ktor Auth Plugin)。

## 🧪 測試與驗證 (Verification)
- [x] 後端單元測試通過 (`python manage.py test`)
- [x] Android / Desktop 本地編譯並驗證功能正常
- [x] 覆蓋率測試包含 Token 過期情境

## 🔗 相關 Task / Issue
Closes #123
```

### 3. 合併策略 (Merge Strategy)
* **預設推薦使用 `Squash and Merge`**：將 Feature 分支上的多個泥沙 Commit 壓縮為一個乾淨且紀錄完整功能的 Commit 合併至 `master`。
* 若分支包含多個獨立且語意明確的 Commit，可選用 **`Rebase and Merge`**。
* **禁止直接使用普通 `Merge Commit`**（避免產生不必要的 Merge Bubble 雙軌交錯網狀歷史）。

---

## 六、 防錯機制與 CI/CD 檢查

本專案配置有自動化 CI 檢查機制：
1. **Commit 訊息格式驗證**：格式不合標準將無法 Push 或建立 PR。
2. **自動化 Lint & Format 檢查**：
   * 後端 Python：`Ruff` / `Black`
   * 前端/KMP Kotlin：`ktlint` / `Spotless`
3. **CI 通過門檻**：PR 必須通過全平台 Build 測試與 Unit Tests 後方可被 Reviewer Approve 並合併。

---
