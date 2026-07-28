package org.scoula.travel.service;

import org.scoula.pagination.Page;
import org.scoula.pagination.PageRequest;
import org.scoula.travel.dto.TravelDTO;
import org.scoula.travel.dto.TravelImageDTO;

import java.util.List;

public interface TravelService {
    public Page<TravelDTO> getPage(PageRequest pageRequest);

    List<TravelDTO> getList();

    TravelDTO get(Long no);

    TravelImageDTO getImage(Long no);

    boolean deleteImage(Long no);
}
