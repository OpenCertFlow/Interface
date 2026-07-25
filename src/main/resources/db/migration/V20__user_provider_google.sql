-- Google 로그인 provider 허용 (소셜 로그인 F-AUTH: Google OAuth)
--
-- V5의 app_user.provider CHECK는 ('LOCAL','KAKAO')만 허용했다. 코드(AuthProvider·GoogleAuthService)는
-- GOOGLE을 쓰지만 제약이 막고 있어 실제 Google 가입 시 INSERT가 CHECK 위반으로 실패한다.
-- 제약을 GOOGLE까지 허용하도록 갱신한다. 기존 행(LOCAL/KAKAO)은 영향 없다.

ALTER TABLE app_user DROP CONSTRAINT ck_app_user_provider;
ALTER TABLE app_user
    ADD CONSTRAINT ck_app_user_provider CHECK (provider IN ('LOCAL', 'KAKAO', 'GOOGLE'));
