package org.scoula.travel.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.scoula.pagination.PageRequest;
import org.scoula.travel.domain.TravelImageVO;
import org.scoula.travel.domain.TravelVO;

import java.util.List;

@Mapper
public interface TravelMapper {
    public int getTotalCount();
    
    public List<String> getDistricts();     // 권역 목록 얻기
    
    public List<TravelVO> getTravels();     // 목록 얻기
    
    public List<TravelVO> getPage(PageRequest pageRequest);     // 페이지별 목록 얻기
    
    public List<TravelVO> getTravelsByDistrict(String district);    // 해당 권역의 목록 얻기
    
    public TravelVO getTravel(Long no);        // 특정 관광지 정보 얻기
    
    public List<TravelImageVO> getImages(Long travelNo);       // 해당 관광지 이미지 목록 얻기
    
    public TravelImageVO getImage(Long no);    // 이미지 정보 얻기

    public int delete(Long no);
}
