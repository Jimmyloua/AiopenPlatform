package com.aiplatform.platform.controller;

import com.aiplatform.platform.dto.*;
import com.aiplatform.platform.service.SkillService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Skill Controller
 */
@Tag(name = "Skill", description = "Skill management APIs")
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Create a new skill")
    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody SkillCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            SkillResponse response = skillService.createSkill(request, userId);
            return ResponseEntity.ok(ApiResponse.success("Skill created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Get skill by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkill(@PathVariable Long id) {
        try {
            SkillResponse response = skillService.getSkillById(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "List skills")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SkillResponse>>> listSkills(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "true") Boolean publicOnly) {
        Page<SkillResponse> response = skillService.listSkills(page, size, category, publicOnly);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Update skill")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SkillResponse>> updateSkill(
            @PathVariable Long id,
            @Valid @RequestBody SkillCreateRequest request,
            @RequestAttribute("userId") Long userId) {
        try {
            SkillResponse response = skillService.updateSkill(id, request, userId);
            return ResponseEntity.ok(ApiResponse.success("Skill updated successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Delete skill")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSkill(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        try {
            skillService.deleteSkill(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Skill deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}