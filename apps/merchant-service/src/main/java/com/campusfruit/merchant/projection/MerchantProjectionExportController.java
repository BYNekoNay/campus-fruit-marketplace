package com.campusfruit.merchant.projection;

import com.campusfruit.merchant.entity.Merchant;
import com.campusfruit.merchant.entity.Store;
import com.campusfruit.merchant.repository.MerchantRepository;
import com.campusfruit.merchant.repository.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal/merchant")
public class MerchantProjectionExportController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final MerchantRepository merchantRepository;
    private final StoreRepository storeRepository;

    public MerchantProjectionExportController(MerchantRepository merchantRepository,
                                              StoreRepository storeRepository) {
        this.merchantRepository = merchantRepository;
        this.storeRepository = storeRepository;
    }

    /**
     * 导出商家投影数据，分页返回。
     *
     * @param pageSize  每页大小（默认 20，最大 100）
     * @param nextToken 分页标记（Base64 编码的上一页最后 merchantId）
     */
    @GetMapping("/projection/export")
    public ResponseEntity<ProjectionExportResponse> export(
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "nextToken", required = false) String nextToken) {

        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        Pageable pageable = PageRequest.of(0, pageSize);
        Page<Merchant> merchantPage;

        if (nextToken != null && !nextToken.isBlank()) {
            try {
                Long cursorId = Long.parseLong(new String(Base64.getDecoder().decode(nextToken)));
                merchantPage = merchantRepository.findByIdGreaterThan(cursorId, pageable);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            merchantPage = merchantRepository.findAllByOrderByIdAsc(pageable);
        }

        List<MerchantProjection> data = merchantPage.getContent().stream()
                .map(this::toProjection)
                .collect(Collectors.toList());

        boolean hasMore = merchantPage.getNumberOfElements() >= pageSize;
        String newNextToken = null;
        if (hasMore && !merchantPage.getContent().isEmpty()) {
            Long lastId = merchantPage.getContent().get(merchantPage.getContent().size() - 1).getId();
            newNextToken = Base64.getEncoder().encodeToString(String.valueOf(lastId).getBytes());
        }

        return ResponseEntity.ok(new ProjectionExportResponse(data, newNextToken, hasMore));
    }

    private MerchantProjection toProjection(Merchant merchant) {
        MerchantProjection projection = new MerchantProjection();
        projection.setMerchantId(merchant.getId());
        projection.setName(merchant.getName());
        projection.setStatus(merchant.getStatus().name());

        List<Store> stores = storeRepository.findByMerchantId(merchant.getId());
        List<StoreProjection> storeProjections = stores.stream().map(store -> {
            StoreProjection sp = new StoreProjection();
            sp.setStoreId(store.getId());
            sp.setName(store.getName());
            sp.setAddress(store.getAddress());
            sp.setLat(store.getLatitude());
            sp.setLng(store.getLongitude());
            sp.setStatus(store.getStatus().name());
            sp.setBusinessHours(store.getBusinessHours());
            sp.setAvgRating(null);
            sp.setReviewCount(null);
            return sp;
        }).collect(Collectors.toList());

        projection.setStores(storeProjections);
        return projection;
    }
}
