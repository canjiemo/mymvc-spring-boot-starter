package io.github.canjiemo.compat;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

@Data
@MyTable("employee")
public class Employee implements MyTableEntity {
    private Long id;
    private Integer status;
    private Integer deleteFlag;
    private Long tenantId;
    @MyField(fill = AuditFill.CREATE_BY)
    private Long createdBy;
}
