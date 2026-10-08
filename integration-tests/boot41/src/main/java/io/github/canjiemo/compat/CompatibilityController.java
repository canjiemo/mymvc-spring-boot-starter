package io.github.canjiemo.compat;

import io.github.canjiemo.base.myjdbc.dao.IBaseDao;
import io.github.canjiemo.base.myjdbc.tenant.TenantContext;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CompatibilityController {
    private final IBaseDao dao;

    @GetMapping("/employee")
    public MyResponseResult<EmployeeDto> employee() {
        TenantContext.setTenantId(10L);
        try {
            Employee employee = dao.querySingleForSql("SELECT * FROM employee WHERE id = :id",
                    Map.of("id", 9007199254740993L), Employee.class);
            EmployeeDto dto = new EmployeeDto();
            dto.setId(employee.getId().toString());
            dto.setStatus(employee.getStatus());
            dto.setPhone("13812348888");
            return new MyResponseResult<>(dto);
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/employee")
    public MyResponseResult<Void> create(@Valid @RequestBody CreateRequest request) {
        return new MyResponseResult<>();
    }

    @GetMapping("/native-validation")
    public MyResponseResult<Void> nativeValidation(@RequestParam @Min(value = 1, message = "数量至少为1") int count) {
        return new MyResponseResult<>();
    }

    @GetMapping("/business-error")
    public MyResponseResult<Void> businessError() {
        throw new BusinessException("测试业务异常");
    }

    public record CreateRequest(@NotBlank(message = "姓名不能为空") String name) {}
}
