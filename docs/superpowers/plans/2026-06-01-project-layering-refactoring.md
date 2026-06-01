# Project Layering Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify package naming, standardize layering, eliminate entity duplication, and implement CQRS across all SCM platform modules.

**Architecture:** All modules move from mixed `com.frog.*`/`scm.*` to unified `com.scmcloud.*`. CQRS is applied per scm-system's existing pattern. Entity dedup removes 5 cross-module duplicates.

**Tech Stack:** Java 17+, Spring Boot 3.x, MyBatis Plus, Dubbo, Maven

**Branch:** `refactor/layering-unification`

---

### Task 0: Create Branch

- [ ] **Step 1: Create and switch to refactoring branch**

```bash
git fetch origin
git checkout -b refactor/layering-unification origin/master || git checkout -b refactor/layering-unification master
```
Expected: New branch created from master.

---

### Task 1: Write Package Rename Script

**Files:**
- Create: `scripts/rename-packages.ps1`

- [ ] **Step 1: Write the PowerShell rename script**

The script handles:
1. Replace `package scm.` → `package com.scmcloud.` and `package com.frog.` → `package com.scmcloud.` in all .java files
2. Replace `import scm.` → `import com.scmcloud.` and `import com.frog.` → `import com.scmcloud.` in all .java files
3. Replace MyBatis XML `namespace="scm.` → `namespace="com.scmcloud.` and `namespace="com.frog.` → `namespace="com.scmcloud.`
4. Move directory trees: `src\main\java\scm\` → `src\main\java\com\scmcloud\` and `src\main\java\com\frog\` → `src\main\java\com\scmcloud\`
5. Update `application.yml`/`application.yaml`: `type-aliases-package`, `mapper-locations`, `base-packages`
6. Update main class `@MapperScan`/`@ComponentScan`/`scanBasePackages`

Create `scripts/rename-packages.ps1`:

```powershell
#Requires -Version 5.1
$ErrorActionPreference = "Stop"

$PROJECT_ROOT = "D:\ProgramProject\scm-platform"

function Update-FileContent {
    param($FilePath, $OldPattern, $NewPattern)
    $content = Get-Content -LiteralPath $FilePath -Raw
    if ($content -match $OldPattern) {
        $newContent = $content -replace $OldPattern, $NewPattern
        Set-Content -LiteralPath $FilePath -Value $newContent -NoNewline
        Write-Host "  Updated: $FilePath"
    }
}

function Rename-InDirectory {
    param($BaseDir, $OldPkgPath, $NewPkgPath)
    $oldDir = Join-Path $BaseDir $OldPkgPath
    if (Test-Path $oldDir) {
        $newDir = Join-Path $BaseDir $NewPkgPath
        $parent = Split-Path $newDir -Parent
        if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
        $fullOldPath = (Get-Item $oldDir).FullName
        $fullNewPath = (Get-Item $parent).FullName + "\" + (Split-Path $newDir -Leaf)
        Move-Item -LiteralPath $fullOldPath -Destination $fullNewPath -Force
        Write-Host "  Moved: $fullOldPath → $fullNewPath"
    }
}

# === PHASE 1: Update Java file content ===
Write-Host "=== Phase 1: Updating Java package/import declarations ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "*.java" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^package com\.frog\.' -NewPattern 'package com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^package scm\.' -NewPattern 'package com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^import com\.frog\.' -NewPattern 'import com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern '(?m)^import scm\.' -NewPattern 'import com.scmcloud.'
}

# === PHASE 2: Update MyBatis XML namespace ===
Write-Host "=== Phase 2: Updating MyBatis XML namespace ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "*.xml" -Recurse -File | Where-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw
    $content -match 'namespace="com\.frog\.' -or $content -match 'namespace="scm\.'
} | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'namespace="com\.frog\.' -NewPattern 'namespace="com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern 'namespace="scm\.' -NewPattern 'namespace="com.scmcloud.'
}

# === PHASE 3: Move directories ===
Write-Host "=== Phase 3: Moving Java source directories ==="
Get-ChildItem -Path $PROJECT_ROOT -Directory -Recurse | Where-Object {
    $_.FullName -match '\\src\\main\\java\\scm\\' -or $_.FullName -match '\\src\\main\\java\\com\\frog\\'
} | Sort-Object FullName -Descending | ForEach-Object {
    $relative = [System.IO.Path]::GetRelativePath($PROJECT_ROOT, $_.FullName)
    if ($relative -match 'src\\main\\java\\(scm\\(.+)|com\\frog\\(.+))') {
        $newRelative = $relative -replace 'src\\main\\java\\scm\\', 'src\main\java\com\scmcloud\'
        $newRelative = $newRelative -replace 'src\\main\\java\\com\\frog\\', 'src\main\java\com\scmcloud\'
        $newPath = Join-Path $PROJECT_ROOT $newRelative
        $parent = Split-Path $newPath -Parent
        if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
        if (-not (Test-Path $newPath)) {
            Move-Item -LiteralPath $_.FullName -Destination $newPath -Force
            Write-Host "  Moved: $relative → $newRelative"
        }
    }
}

# === PHASE 4: Update application config files ===
Write-Host "=== Phase 4: Updating application config files ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "application*.yml" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'com\.frog\.' -NewPattern 'com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern 'scm\.' -NewPattern 'com.scmcloud.'
}
Get-ChildItem -Path $PROJECT_ROOT -Filter "application*.yaml" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'com\.frog\.' -NewPattern 'com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern 'scm\.' -NewPattern 'com.scmcloud.'
}
Get-ChildItem -Path $PROJECT_ROOT -Filter "application*.properties" -Recurse -File | ForEach-Object {
    Update-FileContent -FilePath $_.FullName -OldPattern 'com\.frog\.' -NewPattern 'com.scmcloud.'
    Update-FileContent -FilePath $_.FullName -OldPattern 'scm\.' -NewPattern 'com.scmcloud.'
}

# === PHASE 5: Update pom.xml groupId references ===
Write-Host "=== Phase 5: Updating pom.xml references ==="
Get-ChildItem -Path $PROJECT_ROOT -Filter "pom.xml" -Recurse -File | ForEach-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw
    if ($content -match 'com\.frog') {
        $newContent = $content -replace 'com\.frog', 'com.scmcloud'
        Set-Content -LiteralPath $_.FullName -Value $newContent -NoNewline
        Write-Host "  Updated POM: $($_.FullName)"
    }
}

Write-Host "=== Package rename complete! ==="
```

- [ ] **Step 2: Run the rename script**

Run:
```powershell
powershell -ExecutionPolicy Bypass -File "scripts/rename-packages.ps1"
```
Expected: All .java, .xml, .yml/.yaml, .properties, pom.xml files updated with new package paths. Directory structure moved.

- [ ] **Step 3: Verify the results**

Run:
```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "*.java" -Recurse | Select-String -Pattern "^package (com\.frog|scm)\." | ForEach-Object { Write-Host "REMAINING: $($_.Path):$($_.Line)" }
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "*.xml" -Recurse | Select-String -Pattern "namespace=\"(com\.frog|scm)\." | ForEach-Object { Write-Host "REMAINING XML: $($_.Path):$($_.Line)" }
```
Expected: No output (zero remaining old-style packages).

---

### Task 1: Remove Empty Old Directories

- [ ] **Step 1: Remove leftover empty directories**

After the script moves files, old directories like `src\main\java\scm\` and `src\main\java\com\frog\` may be empty. Remove them:

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Directory -Recurse | Where-Object {
    $_.FullName -match '\\src\\main\\java\\scm$' -or $_.FullName -match '\\src\\main\\java\\com\\frog$'
} | Sort-Object FullName -Descending | ForEach-Object {
    if (-not (Get-ChildItem -LiteralPath $_.FullName -Recurse -ErrorAction SilentlyContinue)) {
        Remove-Item -LiteralPath $_.FullName -Recurse -Force
        Write-Host "  Removed empty: $($_.FullName)"
    }
}
```

---

### Task 2: Dubbo Service Path Unification

**Files:**
- Move: `scm-system/service/src/main/java/com/scmcloud/system/rpc/UserDubboServiceImpl.java`
- Move: `scm-system/service/src/main/java/com/scmcloud/system/rpc/PermissionDubboServiceImpl.java`
- Move: `scm-system/service/src/main/java/com/scmcloud/system/rpc/adapter/DubboPermissionServiceAdapter.java`
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/adapter/` directory

- [ ] **Step 1: Create target directory and move files**

```powershell
$systemService = "D:\ProgramProject\scm-platform\scm-system\service\src\main\java\com\scmcloud\system"
New-Item -ItemType Directory -Path "$systemService\service\dubbo\adapter" -Force | Out-Null

# Move files
Move-Item -LiteralPath "$systemService\rpc\UserDubboServiceImpl.java" -Destination "$systemService\service\dubbo\UserDubboServiceImpl.java" -Force
Move-Item -LiteralPath "$systemService\rpc\PermissionDubboServiceImpl.java" -Destination "$systemService\service\dubbo\PermissionDubboServiceImpl.java" -Force
Move-Item -LiteralPath "$systemService\rpc\adapter\DubboPermissionServiceAdapter.java" -Destination "$systemService\service\dubbo\adapter\DubboPermissionServiceAdapter.java" -Force
```

- [ ] **Step 2: Update package declarations in moved files**

```powershell
$files = @(
    "$systemService\service\dubbo\UserDubboServiceImpl.java",
    "$systemService\service\dubbo\PermissionDubboServiceImpl.java",
    "$systemService\service\dubbo\adapter\DubboPermissionServiceAdapter.java"
)
foreach ($f in $files) {
    $content = Get-Content -LiteralPath $f -Raw
    $content = $content -replace 'package com\.scmcloud\.system\.rpc(\.adapter)?;', 'package com.scmcloud.system.service.dubbo${1};'
    $content = $content -replace 'import com\.scmcloud\.system\.rpc\.', 'import com.scmcloud.system.service.dubbo.'
    Set-Content -LiteralPath $f -Value $content -NoNewline
    Write-Host "Updated: $f"
}
```

- [ ] **Step 3: Clean up old empty rpc/ directory**

```powershell
Remove-Item -LiteralPath "$systemService\rpc" -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "Removed old rpc/ directory"
```

---

### Task 3: DTO Location Standardization

**Files:**
- Move: `scm-warehouse/service/.../vo/NearExpiryProductVO.java` → `scm-warehouse/service/.../dto/NearExpiryProductVO.java`
- Move: `scm-supplier/service/.../vo/OverdueApprovalTaskVO.java` → `scm-supplier/service/.../dto/OverdueApprovalTaskVO.java`
- Move: `scm-inventory/service/.../domain/dto/*.java` → `scm-inventory/service/.../dto/*.java`

- [ ] **Step 1: Find exact paths for DTO files**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "NearExpiryProductVO.java" -Recurse | ForEach-Object { $_.FullName }
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "OverdueApprovalTaskVO.java" -Recurse | ForEach-Object { $_.FullName }
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "InventoryTransferRequest.java" -Recurse | ForEach-Object { $_.FullName }
```

- [ ] **Step 2: Move warehouse vo/ → dto/**

```powershell
$whService = "D:\ProgramProject\scm-platform\scm-warehouse\service\src\main\java\com\scmcloud\warehouse"
New-Item -ItemType Directory -Path "$whService\dto" -Force | Out-Null
Move-Item -LiteralPath "$whService\vo\NearExpiryProductVO.java" -Destination "$whService\dto\NearExpiryProductVO.java" -Force
Remove-Item -LiteralPath "$whService\vo" -Recurse -Force -ErrorAction SilentlyContinue
# Update package declaration
$content = Get-Content -LiteralPath "$whService\dto\NearExpiryProductVO.java" -Raw
$content = $content -replace 'package com\.scmcloud\.warehouse\.vo;', 'package com.scmcloud.warehouse.dto;'
Set-Content -LiteralPath "$whService\dto\NearExpiryProductVO.java" -Value $content -NoNewline
```

- [ ] **Step 3: Move supplier vo/ → dto/**

```powershell
$supService = "D:\ProgramProject\scm-platform\scm-supplier\service\src\main\java\com\scmcloud\supplier"
New-Item -ItemType Directory -Path "$supService\dto" -Force | Out-Null
Move-Item -LiteralPath "$supService\vo\OverdueApprovalTaskVO.java" -Destination "$supService\dto\OverdueApprovalTaskVO.java" -Force
Remove-Item -LiteralPath "$supService\vo" -Recurse -Force -ErrorAction SilentlyContinue
$content = Get-Content -LiteralPath "$supService\dto\OverdueApprovalTaskVO.java" -Raw
$content = $content -replace 'package com\.scmcloud\.supplier\.vo;', 'package com.scmcloud.supplier.dto;'
Set-Content -LiteralPath "$supService\dto\OverdueApprovalTaskVO.java" -Value $content -NoNewline
```

- [ ] **Step 4: Move inventory domain/dto/ → dto/**

```powershell
$invService = "D:\ProgramProject\scm-platform\scm-inventory\service\src\main\java\com\scmcloud\inventory"
New-Item -ItemType Directory -Path "$invService\dto" -Force | Out-Null
Get-ChildItem -LiteralPath "$invService\domain\dto" -Filter "*.java" | ForEach-Object {
    Move-Item -LiteralPath $_.FullName -Destination "$invService\dto\$($_.Name)" -Force
    # Update package
    $content = Get-Content -LiteralPath "$invService\dto\$($_.Name)" -Raw
    $content = $content -replace 'package com\.scmcloud\.inventory\.domain\.dto;', 'package com.scmcloud.inventory.dto;'
    $content = $content -replace 'import com\.scmcloud\.inventory\.domain\.dto\.', 'import com.scmcloud.inventory.dto.'
    Set-Content -LiteralPath "$invService\dto\$($_.Name)" -Value $content -NoNewline
}
Remove-Item -LiteralPath "$invService\domain\dto" -Recurse -Force -ErrorAction SilentlyContinue
if (-not (Get-ChildItem -LiteralPath "$invService\domain" -Recurse -ErrorAction SilentlyContinue)) {
    Remove-Item -LiteralPath "$invService\domain" -Recurse -Force -ErrorAction SilentlyContinue
}
```

---

### Task 4: Entity ID Type Fix (scm-order)

**Files:**
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrder.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdOrderItem.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdPayment.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdRefund.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrdStatusHistory.java`
- Modify: `scm-order/api/src/main/java/com/scmcloud/order/api/dto/OrderVO.java` (add `@JsonSerialize`)
- Also check: `scm-order/service/.../dto/*.java` and scm-logistics/warehouse `orderId` references

- [ ] **Step 1: Search all orderId foreign key references across project**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "*.java" -Recurse | Select-String -Pattern "orderId|order_id" | ForEach-Object {
    Write-Host "$($_.Path):$($_.LineNumber) → $($_.Line.Trim())"
}
```
Review output to identify all foreign key fields that reference `OrdOrder.id`.

- [ ] **Step 2: Change String id → Long id in order entities**

For each entity file, change `private String id;` → `private Long id;`. Also change `orderId` foreign key fields from `String` to `Long`.

Script:
```powershell
$orderEntityFiles = @(
    "D:\ProgramProject\scm-platform\scm-order\service\src\main\java\com\scmcloud\order\domain\entity\OrdOrder.java",
    "D:\ProgramProject\scm-platform\scm-order\service\src\main\java\com\scmcloud\order\domain\entity\OrdOrderItem.java",
    "D:\ProgramProject\scm-platform\scm-order\service\src\main\java\com\scmcloud\order\domain\entity\OrdPayment.java",
    "D:\ProgramProject\scm-platform\scm-order\service\src\main\java\com\scmcloud\order\domain\entity\OrdRefund.java",
    "D:\ProgramProject\scm-platform\scm-order\service\src\main\java\com\scmcloud\order\domain\entity\OrdStatusHistory.java"
)
# Change id field from String to Long
foreach ($f in $orderEntityFiles) {
    $content = Get-Content -LiteralPath $f -Raw
    $content = $content -replace '(?m)private String (id|orderId);', 'private Long $1;'
    Set-Content -LiteralPath $f -Value $content -NoNewline
    Write-Host "Updated ID types: $f"
}
```

- [ ] **Step 3: Add @JsonSerialize on OrderVO.id for frontend JS precision**

```powershell
$orderVO = "D:\ProgramProject\scm-platform\scm-order\api\src\main\java\com\scmcloud\order\api\dto\OrderVO.java"
$content = Get-Content -LiteralPath $orderVO -Raw
# Add Jackson import and annotation on id field
$content = $content -replace '(?m)(import lombok\.Data;)', '$1
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;'
$content = $content -replace '(?m)(private Long id;)', '@JsonSerialize(using = ToStringSerializer.class)
private Long id;'
Set-Content -LiteralPath $orderVO -Value $content -NoNewline
Write-Host "Added @JsonSerialize to OrderVO.id"
```

- [ ] **Step 4: Update mappers and XMLs**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform\scm-order" -Filter "*.xml" -Recurse | ForEach-Object {
    $content = Get-Content -LiteralPath $_.FullName -Raw
    if ($content -match 'jdbcType="VARCHAR"') {
        $content = $content -replace '(id\s+jdbcType=)"VARCHAR"', '${1}"BIGINT"'
        $content = $content -replace '(order_id\s+jdbcType=)"VARCHAR"', '${1}"BIGINT"'
        Set-Content -LiteralPath $_.FullName -Value $content -NoNewline
        Write-Host "Updated XML: $($_.FullName)"
    }
}
```

---

### Task 5: Entity Deduplication - Remove from scm-common/web

**Files:**
- Delete: `scm-common/web/src/main/java/com/scmcloud/common/log/entity/SysAuditLog.java`

- [ ] **Step 1: Remove SysAuditLog from scm-common/web**

```powershell
Remove-Item -LiteralPath "D:\ProgramProject\scm-platform\scm-common\web\src\main\java\com\scmcloud\common\log\entity\SysAuditLog.java" -Force
Write-Host "Deleted SysAuditLog from scm-common/web"
```

- [ ] **Step 2: Check references to common.log.entity.SysAuditLog in other modules**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "*.java" -Recurse | Select-String -Pattern "common\.log\.entity\.SysAuditLog|SysAuditLog" | ForEach-Object {
    Write-Host "REFERENCE: $($_.Path):$($_.LineNumber)"
}
```

- [ ] **Step 3: Replace references with Dubbo client calls (transition)**

SysAuditLog is a high-frequency write — ideally async Kafka. For this refactoring:
- **Transition phase**: Replace direct references with sync Dubbo calls to `scm-audit` API
- **Future optimization**: Kafka async audit event will be tracked as a separate issue

For each reference found in Step 2, replace with:
```java
// Before
sysAuditLogMapper.insert(log);

// After
auditDubboService.saveAuditLog(AuditLogVO.from(log));
```

Note: The `AuditDubboService` in scm-audit/api must have a `saveAuditLog` method. If missing, add it.

---

### Task 6: Entity Deduplication - Remove from scm-system

**Files to delete from scm-system:**
- `SysSensitiveOperationLog.java`
- `SysPermissionApproval.java`
- `SysNotificationTemplate.java`
- `SysUserNotificationPreference.java`
- Corresponding mappers and XMLs

- [ ] **Step 1: Find and delete duplicated entity files in scm-system**

```powershell
$systemEntities = @(
    "SysSensitiveOperationLog.java",
    "SysPermissionApproval.java",
    "SysNotificationTemplate.java",
    "SysUserNotificationPreference.java"
)
foreach ($entity in $systemEntities) {
    $files = Get-ChildItem -Path "D:\ProgramProject\scm-platform\scm-system" -Filter $entity -Recurse
    foreach ($f in $files) {
        Remove-Item -LiteralPath $f.FullName -Force
        Write-Host "Deleted: $($f.FullName)"
    }
}
```

- [ ] **Step 2: Find and delete corresponding mappers and XMLs in scm-system**

```powershell
$mapperFiles = @(
    "*SensitiveOperationLog*",
    "*PermissionApproval*",
    "*NotificationTemplate*",
    "*UserNotificationPreference*"
)
foreach ($pattern in $mapperFiles) {
    $files = Get-ChildItem -Path "D:\ProgramProject\scm-platform\scm-system" -Filter $pattern -Recurse
    foreach ($f in $files) {
        Remove-Item -LiteralPath $f.FullName -Force
        Write-Host "Deleted: $($f.FullName)"
    }
}
```

- [ ] **Step 3: Find all references to removed entities in scm-system**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform\scm-system" -Filter "*.java" -Recurse | Select-String -Pattern "SysSensitiveOperationLog|SysPermissionApproval|SysNotificationTemplate|SysUserNotificationPreference" | ForEach-Object {
    Write-Host "REF in system: $($_.Path):$($_.LineNumber) → $($_.Line.Trim())"
}
```

For each reference, replace direct DB access with Dubbo client calls to the owning module's API.

---

### Task 7: CQRS Separation (Per-Module)

This is the largest task. For each module, create `query/` and `command/` service packages.

**Transaction Boundary Rules (MUST follow):**

| Scenario | Rule |
|----------|------|
| Single-module write | `@Transactional` on CommandService method |
| Write-then-read (same request) | Controller calls CommandService first (transaction commits), THEN QueryService (separate transaction); **NEVER** mix read/write in one transaction |
| Cross-module write | `@GlobalTransactional` on CommandService; **NEVER** on QueryService |
| CommandService needs query inside | Allowed to inject QueryService; **FORBIDDEN** for QueryService to inject CommandService |
| Report / complex aggregation | `@Transactional(readOnly = true)` on QueryService; no write logic mixed in |

**Pattern for each service interface `I{Entity}Service`:**

1. Create `service/query/{Entity}QueryService.java`:
   - Extract all read methods (`get*`, `find*`, `list*`, `page*`, `search*`, `query*`)
   - Add `@Slave` annotation
   - Implement by delegating to mapper

2. Create `service/command/{Entity}CommandService.java`:
   - Extract all write methods (`create*`, `save*`, `update*`, `delete*`, `batch*`)
   - Add `@Master` annotation
   - Implement by delegating to mapper with `@Transactional`

3. Update Controller:
   - Replace `I{Entity}Service` with `{Entity}QueryService` for query endpoints
   - Replace with `{Entity}CommandService` for write endpoints

4. Keep `I{Entity}Service` interface and `Impl` only if coordinating between query/command

- [ ] **Step 1: CQRS for scm-system (already has pattern, verify and standardize)**

```powershell
# scm-system already has query/ and command/ packages - verify all services follow the pattern
Get-ChildItem -Path "D:\ProgramProject\scm-platform\scm-system\service\src\main\java\com\scmcloud\system\service\query" -Filter "*.java"
Get-ChildItem -Path "D:\ProgramProject\scm-platform\scm-system\service\src\main\java\com\scmcloud\system\service\command" -Filter "*.java"
```

- [ ] **Step 2-12: Apply CQRS pattern to each module**

For each module (scm-order, scm-product, scm-inventory, scm-warehouse, scm-logistics, scm-supplier, scm-purchase, scm-finance, scm-approval, scm-audit, scm-notify, scm-tenant):

Create query/ and command/ services for each service interface, following the exact same pattern as scm-system's `UserCrossDatabaseQueryService`/`UserRoleCrossDatabaseCommandService`.

**WIP Checkpoint after Task 7:**
```bash
git add -A
git commit -m "wip: CQRS separation completed for all modules"
```

---

### Task 8: Fix Cross-Module Imports & Config

- [ ] **Step 1: Global search for unresolved references**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "*.java" -Recurse | Select-String -Pattern "com\.frog|import scm\." | ForEach-Object {
    Write-Host "UNFIXED: $($_.Path):$($_.LineNumber) → $($_.Line.Trim())"
}
```
Expected: No output — all old package references eliminated.

- [ ] **Step 2: Verify all config files**

```powershell
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "application*.yml" -Recurse | Select-String -Pattern "com\.frog|type-aliases-package: scm\." | ForEach-Object {
    Write-Host "UNFIXED CONFIG: $($_.Path):$($_.LineNumber) → $($_.Line.Trim())"
}
Get-ChildItem -Path "D:\ProgramProject\scm-platform" -Filter "application*.yaml" -Recurse | Select-String -Pattern "com\.frog|type-aliases-package: scm\." | ForEach-Object {
    Write-Host "UNFIXED CONFIG: $($_.Path):$($_.LineNumber) → $($_.Line.Trim())"
}
```

- [ ] **Step 3: Verify Dubbo scan paths**

For each module, check `application.yml`/`yaml` for:
```yaml
dubbo:
  scan:
    base-packages: com.scmcloud.{module}
```

- [ ] **Step 4: WIP Checkpoint commit**

```bash
git add -A
git commit -m "wip: Phase 7 - cross-module imports and config fixes complete"
```

---

### Task 9: Build & Verify

- [ ] **Step 1: Per-module checklist verification**

For each module, confirm:
- [ ] `package` declarations use `com.scmcloud.{module}`
- [ ] No remaining `com.frog.*` or `scm.*` imports
- [ ] MyBatis XML `namespace` updated
- [ ] `application.yml` `type-aliases-package` updated
- [ ] Spring Boot `@MapperScan`/`@ComponentScan` updated
- [ ] Dubbo `scan.base-packages` updated
- [ ] DTO locations follow `api/dto/` and `service/dto/` standard
- [ ] CQRS split complete, Controller injection switched
- [ ] Transaction boundaries follow rules (Task 7)

- [ ] **Step 2: Build compilation**

```powershell
mvn clean compile -f com.scm.parent/pom.xml -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Fix compilation errors**

Iterate on errors:
- Missing imports (especially after entity dedup)
- Incorrect Dubbo reference scan paths
- Wrong mapper namespace references

- [ ] **Step 4: Build with tests**

```powershell
mvn clean test -f com.scm.parent/pom.xml
```
Expected: All tests pass.

- [ ] **Step 5: Full build**

```powershell
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```
Expected: BUILD SUCCESS

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "refactor: complete layering unification - package rename, CQRS, entity dedup, DTO standardization"
```
