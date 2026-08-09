-- R-08 审计日志防篡改（FR-04-03 约束"日志不可篡改"）
-- audit_event 增加链式哈希：prev_hash 指向上一条事件的 hash，hash 为本事件内容的 SHA-256。
-- 内容域：action|targetType|targetId|beforeValue|afterValue|traceId|prevHash（null 以空串参与）。
-- 历史数据（V6 之前的事件）prev_hash/hash 为 NULL，校验时按"未纳入链"处理，不算篡改。

ALTER TABLE audit_event ADD COLUMN prev_hash VARCHAR(64);
ALTER TABLE audit_event ADD COLUMN hash VARCHAR(64);

CREATE INDEX idx_audit_hash ON audit_event(hash);
