package tech.waitforu.service;

import org.junit.jupiter.api.Test;
import tech.waitforu.exception.KrlExportException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Excel 导出服务测试。
 */
class CallGraphExcelExportServiceTest {

    /**
     * 空机器人列表不允许导出。
     */
    @Test
    void exportShouldThrowWhenRobotListIsEmpty() {
        CallGraphExcelExportService service = new CallGraphExcelExportService();
        assertThrows(KrlExportException.class, () -> service.export(List.of()));
    }
}
