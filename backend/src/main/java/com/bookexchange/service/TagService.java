package com.bookexchange.service;

import com.bookexchange.dto.TagDTO;
import com.bookexchange.entity.BookTag;
import com.bookexchange.entity.Tag;
import com.bookexchange.repository.BookTagRepository;
import com.bookexchange.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;

    @Cacheable(value = "tags", key = "'all'")
    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public Tag getTagById(Long id) {
        return tagRepository.findById(id).orElse(null);
    }

    public Tag getTagByName(String name) {
        return tagRepository.findByName(name).orElse(null);
    }

    @CacheEvict(value = "tags", key = "'all'")
    public Tag createTag(TagDTO tagDTO) {
        if (tagRepository.findByName(tagDTO.getName()).isPresent()) {
            return null;
        }
        Tag tag = new Tag();
        tag.setName(tagDTO.getName());
        tag.setDescription(tagDTO.getDescription());
        tag.setColor(tagDTO.getColor());
        return tagRepository.save(tag);
    }

    @CacheEvict(value = "tags", key = "'all'")
    public Tag updateTag(Long id, TagDTO tagDTO) {
        Tag tag = tagRepository.findById(id).orElse(null);
        if (tag == null) {
            return null;
        }
        if (tagDTO.getName() != null && !tagDTO.getName().equals(tag.getName())) {
            if (tagRepository.findByName(tagDTO.getName()).isPresent()) {
                return null;
            }
            tag.setName(tagDTO.getName());
        }
        if (tagDTO.getDescription() != null) {
            tag.setDescription(tagDTO.getDescription());
        }
        if (tagDTO.getColor() != null) {
            tag.setColor(tagDTO.getColor());
        }
        return tagRepository.save(tag);
    }

    @Transactional
    @CacheEvict(value = "tags", key = "'all'")
    public boolean deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            return false;
        }
        List<BookTag> bookTags = bookTagRepository.findByTagId(id);
        bookTagRepository.deleteAll(bookTags);
        tagRepository.deleteById(id);
        return true;
    }

    public List<Tag> getTagsByIds(List<Long> tagIds) {
        return tagRepository.findAllById(tagIds);
    }
}
