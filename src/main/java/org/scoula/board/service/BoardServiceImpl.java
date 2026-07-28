package org.scoula.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.board.domain.BoardAttachmentVO;
import org.scoula.board.domain.BoardVO;
import org.scoula.board.dto.BoardDTO;
import org.scoula.board.mapper.BoardMapper;
import org.scoula.pagination.Page;
import org.scoula.pagination.PageRequest;
import org.scoula.util.S3Service;
import org.scoula.util.UploadFiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {
    @Value("${upload.local.path}")
    private String uploadPath;

    @Value("${upload.storage.type}")
    private String storageType;

    final private BoardMapper mapper;

    // S3Service를 주입받습니다 (@RequiredArgsConstructor + final)
    private final S3Service s3Service;

    @Override
    public Page<BoardDTO> getPage(PageRequest pageRequest) {
        List<BoardVO> boards = mapper.getPage(pageRequest);
        int totalCount = mapper.getTotalCount();

        return Page.of(pageRequest, totalCount, boards.stream().map(BoardDTO::of).toList());
    }

    @Override
    public List<BoardDTO> getList() {
        log.info("getList..........");
        return mapper.getList().stream()    // BoardVO의 스트림
            .map(BoardDTO::of)              // BoardDTO의 스트림
            .toList();                      // List<BoardDTO> 변환
    }

    @Override
    public BoardDTO get(Long no) {
        log.info("get......" + no);
        BoardDTO board = BoardDTO.of(mapper.get(no));

        log.info("==================================" + board);
        return Optional.ofNullable(board)
            .orElseThrow(NoSuchElementException::new);
    }

    // 2개 이상의 insert 문이 실행될 수 있으므로 트랜잭션 처리 필요
    // RuntimeException인 경우만 자동 rollback.
    @Transactional
    @Override
    public BoardDTO create(BoardDTO board) {
        log.info("create......." + board);

        BoardVO boardVO = board.toVo();
        mapper.create(boardVO);

        // 파일 업로드 처리
        List<MultipartFile> files = board.getFiles();
        if(files != null && !files.isEmpty()) { // 첨부 파일이 있는 경우
            upload(boardVO.getNo(), files);
        }

        return get(boardVO.getNo());
    }


    private void upload(Long bno, List<MultipartFile> files) {
        for (MultipartFile part : files) {
            if (part.isEmpty()) continue;

            String path;
            if ("s3".equals(storageType)) {
                // s3Service.upload()는 S3 버킷에 파일을 업로드하고
                // S3 key(경로)를 반환합니다 (예: "public/board/uuid.jpg")
                path = s3Service.upload(part, "public/board");
            } else {
                // 로컬 업로드
                try {
                    path = UploadFiles.upload(uploadPath + "/board", part);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // DB에 경로 저장 (S3 key 또는 로컬 경로)
            BoardAttachmentVO attach = BoardAttachmentVO.of(part, bno, path);
            mapper.createAttachment(attach);
        }
    }


    @Override
    public BoardDTO update(BoardDTO board) {
        log.info("update......" + board);

        mapper.update(board.toVo());

        // 파일 업로드 처리
        List<MultipartFile> files = board.getFiles();
        if(files != null && !files.isEmpty()) {
            upload(board.getNo(), files);
        }

        return get(board.getNo());
    }

    @Override
    public BoardDTO delete(Long no) {
        log.info("delete......." + no);
        BoardDTO board = get(no);

        // 물리 파일 삭제
        List<BoardAttachmentVO> attaches = board.getAttaches();
        if (attaches != null) {
            for (BoardAttachmentVO attach : attaches) {
                if ("s3".equals(storageType)) {
                    s3Service.delete(attach.getPath());
                } else {
                    File file = new File(attach.getPath());
                    if (file.exists()) file.delete();
                }
            }
        }

        mapper.delete(no);
        return board;
    }

    // 첨부 파일 한 개 얻기
    @Override
    public BoardAttachmentVO getAttachment(Long no) {
        return mapper.getAttachment(no);
    }

    // 첨부 파일 삭제
    @Override
    public boolean deleteAttachment(Long no) {
        BoardAttachmentVO attach = mapper.getAttachment(no);
        if (attach != null) {
            if ("s3".equals(storageType)) {
                s3Service.delete(attach.getPath());
            } else {
                File file = new File(attach.getPath());
                if (file.exists()) file.delete();
            }
        }
        return mapper.deleteAttachment(no) == 1;
    }
}
