package org.scoula.travel.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.scoula.travel.domain.TravelImageVO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelImageDTO {
    private long no;
    private String filename;
    private long travelNo;

    public static TravelImageDTO of(TravelImageVO vo) {
        return TravelImageDTO.builder()
            .no(vo.getNo())
            .filename(vo.getFilename())
            .travelNo(vo.getTravelNo())
            .build();
    }

    // 파일 시스템 상의 전체 경로 (로컬용)
    // filename에 저장된 값이 전체 경로(S3 key 등)일 경우를 대비해 유연하게 처리 필요
    @JsonIgnore
    public String getPath(String baseDir) {
        return baseDir + "/travel/" + filename;
    }

    // 프론트엔드에서 사용할 url 프로퍼티
    public String getUrl() {
        // 우선은 서버 컨트롤러를 거치는 기존 방식 유지
        return "/api/travel/image/" + no;
    }
}
