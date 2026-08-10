package com.smartgrain.controller.v1;

import com.smartgrain.common.api.Result;
import com.smartgrain.iot.IotWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "IoT 监控 Iot")
@RestController
@RequestMapping("/api/v1/iot")
@RequiredArgsConstructor
public class IotController {

    private final IotWebSocketHandler iotWebSocketHandler;

    @Operation(summary = "WebSocket 在线客户端数")
    @GetMapping("/ws-status")
    @PreAuthorize("hasAuthority('iot:reading:view')")
    public Result<Map<String, Object>> wsStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("online", iotWebSocketHandler.onlineCount());
        return Result.success(data);
    }
}
