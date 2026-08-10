package com.project.airBnbApp.controller;

import com.project.airBnbApp.dto.InventoryDTO;
import com.project.airBnbApp.dto.UpdateInventoryRequestDTO;
import com.project.airBnbApp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<List<InventoryDTO>> getAllInventoryByRoom(@PathVariable Long roomId){
        return ResponseEntity.ok(inventoryService.getAllInventoryByRoom(roomId));
    }

    @PatchMapping("/rooms/{roomId}")
    public ResponseEntity<Void> updateInventoryByRoom(@PathVariable Long roomId, @RequestBody UpdateInventoryRequestDTO updateInventoryRequestDTO){
        inventoryService.updateInventory(roomId, updateInventoryRequestDTO);
        return ResponseEntity.noContent().build();
    }
}
