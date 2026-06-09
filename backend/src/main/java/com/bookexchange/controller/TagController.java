package com.bookexchange.controller;

import com.bookexchange.dto.Result;
import com.bookexchange.dto.TagDTO;
import com.bookexchange.entity.Tag;
import com.bookexchange.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<Tag>> getAllTags() {
        return Result.success(tagService.getAllTags());
    }

    @GetMapping("/{id}")
    public Result<Tag> getTagById(@PathVariable Long id) {
        Tag tag = tagService.getTagById(id);
        return tag != null ? Result.success(tag) : Result.error("标签不存在");
    }

    @GetMapping("/name/{name}")
    public Result<Tag> getTagByName(@PathVariable String name) {
        Tag tag = tagService.getTagByName(name);
        return tag != null ? Result.success(tag) : Result.error("标签不存在");
    }

    @PostMapping
    public Result<Tag> createTag(@RequestBody TagDTO tagDTO) {
        if (tagDTO.getName() == null || tagDTO.getName().trim().isEmpty()) {
            return Result.error("标签名称不能为空");
        }
        Tag tag = tagService.createTag(tagDTO);
        return tag != null ? Result.success(tag) : Result.error("标签名称已存在");
    }

    @PutMapping("/{id}")
    public Result<Tag> updateTag(@PathVariable Long id, @RequestBody TagDTO tagDTO) {
        Tag tag = tagService.updateTag(id, tagDTO);
        return tag != null ? Result.success(tag) : Result.error("更新失败：标签不存在或名称已存在");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        boolean deleted = tagService.deleteTag(id);
        return deleted ? Result.success() : Result.error("标签不存在");
    }
}
