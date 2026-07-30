package com.campusfruit.discovery.service;

import com.campusfruit.discovery.dto.NearbyStoreDTO;
import com.campusfruit.discovery.entity.StoreOfferProjection;
import com.campusfruit.discovery.repository.StoreOfferProjectionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NearbyStoreService {

    private static final double KM_TO_LAT = 1.0 / 111.0;
    private static final double KM_TO_LNG_AT_30N = 1.0 / (111.0 * Math.cos(Math.toRadians(30)));

    private final StoreOfferProjectionRepository projectionRepository;

    public NearbyStoreService(StoreOfferProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    /**
     * 查询附近门店。
     *
     * @param lat      用户纬度
     * @param lng      用户经度
     * @param radiusKm 搜索半径（公里）
     * @param limit    返回数量上限
     * @return 附近门店 DTO 列表，按距离升序排列
     */
    public List<NearbyStoreDTO> findNearbyStores(double lat, double lng, double radiusKm, Integer limit) {
        double latDelta = radiusKm * KM_TO_LAT;
        double lngDelta = radiusKm * KM_TO_LNG_AT_30N;
        double latMin = lat - latDelta;
        double latMax = lat + latDelta;
        double lngMin = lng - lngDelta;
        double lngMax = lng + lngDelta;

        int pageSize = limit != null ? limit : 20;
        var page = projectionRepository.findByBoundingBox(
                latMin, latMax, lngMin, lngMax, PageRequest.of(0, Math.max(pageSize * 2, 50)));

        return page.getContent().stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getStoreStatus()))
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.getOfferStatus()))
                .filter(p -> p.getStoreLat() != null && p.getStoreLng() != null)
                .collect(Collectors.toMap(
                        StoreOfferProjection::getStoreId,
                        p -> p,
                        (existing, replacement) -> existing))
                .values().stream()
                .map(p -> toNearbyDTO(p, lat, lng))
                .peek(dto -> {
                    double dlat = dto.getLat() - lat;
                    double dlng = dto.getLng() - lng;
                    double approximateDistance = Math.sqrt(dlat * dlat + dlng * dlng) * 111.0;
                    dto.setDistance(approximateDistance);
                })
                .filter(dto -> dto.getDistance() <= radiusKm)
                .sorted(Comparator.comparing(NearbyStoreDTO::getDistance))
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    private NearbyStoreDTO toNearbyDTO(StoreOfferProjection p, double userLat, double userLng) {
        NearbyStoreDTO dto = new NearbyStoreDTO();
        dto.setStoreId(p.getStoreId());
        dto.setStoreName(p.getStoreName());
        dto.setAddress(p.getStoreAddress());
        dto.setLat(p.getStoreLat());
        dto.setLng(p.getStoreLng());
        dto.setPhone(p.getStorePhone());
        dto.setAvgRating(p.getAvgRating());
        return dto;
    }
}
