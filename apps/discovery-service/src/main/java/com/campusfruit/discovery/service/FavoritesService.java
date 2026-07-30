package com.campusfruit.discovery.service;

import com.campusfruit.discovery.entity.Favorite;
import com.campusfruit.discovery.repository.FavoriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FavoritesService {

    private static final Logger log = LoggerFactory.getLogger(FavoritesService.class);

    private final FavoriteRepository favoriteRepository;

    public FavoritesService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * 幂等添加收藏。
     */
    @Transactional
    public Favorite addFavorite(Long userId, Long storeId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndStoreId(userId, storeId);
        if (existing.isPresent()) {
            log.debug("Favorite already exists: userId={}, storeId={}", userId, storeId);
            return existing.get();
        }

        Favorite favorite = new Favorite(userId, storeId);
        Favorite saved = favoriteRepository.save(favorite);
        log.info("Added favorite: userId={}, storeId={}", userId, storeId);
        return saved;
    }

    /**
     * 幂等删除收藏。
     */
    @Transactional
    public void removeFavorite(Long userId, Long storeId) {
        Optional<Favorite> existing = favoriteRepository.findByUserIdAndStoreId(userId, storeId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            log.info("Removed favorite: userId={}, storeId={}", userId, storeId);
        } else {
            log.debug("Favorite not found for delete: userId={}, storeId={}", userId, storeId);
        }
    }

    /**
     * 分页查询我的收藏。
     */
    public List<Long> getMyFavorites(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Favorite> favoritesPage = favoriteRepository.findByUserId(userId, pageRequest);

        return favoritesPage.getContent().stream()
                .map(Favorite::getStoreId)
                .collect(Collectors.toList());
    }
}
