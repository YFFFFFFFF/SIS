package com.sis.iids.collab;

/**
 * 协同数据表的一行（原型 P8"基础数据协同表"）：
 * 数据项 / 责任部门 / 当前值 / 锁状态 / 最后编辑。
 */
public record CollabFieldItem(
        String fieldKey,
        String group,
        String itemName,
        String ownerDept,
        String currentValue,
        String lockHolder,
        String lockExpireAt,
        String lastEditor,
        String lastEditAt
) {
}
