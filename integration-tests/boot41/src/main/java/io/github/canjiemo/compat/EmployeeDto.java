package io.github.canjiemo.compat;

import io.github.canjiemo.base.mymvc.privacy.Privacy;
import io.github.canjiemo.base.mymvc.privacy.PrivacyType;
import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

@Data
public class EmployeeDto {
    private String id;
    @MyDict(type = "employee_status")
    private Integer status;
    @Privacy(type = PrivacyType.PHONE)
    private String phone;
}
