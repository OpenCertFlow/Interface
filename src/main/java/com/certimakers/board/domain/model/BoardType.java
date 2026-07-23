package com.certimakers.board.domain.model;

/**
 * 게시판 종류와 <b>종류별 정책</b>. 이 프로젝트에서 "게시판 타입별 기능 분리"가 실현되는 지점이다.
 *
 * <p>정책을 enum에 담는 이유는, 이것을 서비스나 컨트롤러의 {@code if (type == NOTICE)} 분기로 흩어
 * 두면 새 게시판을 추가할 때 모든 분기를 찾아 고쳐야 하고 한 곳만 놓쳐도 규칙이 깨지기 때문이다.
 * 여기서는 새 상수 한 줄이 곧 새 게시판이 된다.
 *
 * <table>
 *   <caption>게시판별 정책</caption>
 *   <tr><th>게시판</th><th>작성 권한</th><th>댓글</th><th>첨부</th><th>비밀글</th></tr>
 *   <tr><td>공지</td><td>관리자</td><td>불가</td><td>가능</td><td>불가</td></tr>
 *   <tr><td>자유</td><td>회원</td><td>가능</td><td>가능</td><td>불가</td></tr>
 *   <tr><td>질문</td><td>회원</td><td>가능</td><td>가능</td><td>가능</td></tr>
 *   <tr><td>자료실</td><td>관리자</td><td>불가</td><td>가능</td><td>불가</td></tr>
 * </table>
 */
public enum BoardType {

    /** 공지사항. 운영자가 알리는 글이라 회원이 쓰거나 댓글로 흐리지 않는다. */
    NOTICE("공지사항", true, false, true, false),

    /** 자유게시판. 회원 누구나 쓰고 댓글을 단다. */
    FREE("자유게시판", false, true, true, false),

    /** 질문게시판. 인증 준비 상황에는 민감한 제조 정보가 섞이므로 비밀글을 허용한다. */
    QNA("질문게시판", false, true, true, true),

    /** 자료실. 서식·가이드 배포용이라 작성은 관리자만, 대신 첨부가 핵심이다. */
    ARCHIVE("자료실", true, false, true, false);

    private final String displayName;
    private final boolean adminOnlyToWrite;
    private final boolean commentsAllowed;
    private final boolean attachmentsAllowed;
    private final boolean secretPostAllowed;

    BoardType(
            String displayName,
            boolean adminOnlyToWrite,
            boolean commentsAllowed,
            boolean attachmentsAllowed,
            boolean secretPostAllowed) {
        this.displayName = displayName;
        this.adminOnlyToWrite = adminOnlyToWrite;
        this.commentsAllowed = commentsAllowed;
        this.attachmentsAllowed = attachmentsAllowed;
        this.secretPostAllowed = secretPostAllowed;
    }

    public String displayName() {
        return displayName;
    }

    public boolean isAdminOnlyToWrite() {
        return adminOnlyToWrite;
    }

    public boolean allowsComments() {
        return commentsAllowed;
    }

    public boolean allowsAttachments() {
        return attachmentsAllowed;
    }

    public boolean allowsSecretPost() {
        return secretPostAllowed;
    }
}
