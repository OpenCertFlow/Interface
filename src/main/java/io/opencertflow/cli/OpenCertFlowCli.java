package io.opencertflow.cli;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * {@code opencertflow} 명령의 진입점.
 *
 * <p><b>스프링 컨텍스트를 띄우지 않는다.</b> 룰을 검증하는 데 DB·Redis·AI 워커가 필요하다면
 * 커뮤니티 기여자는 룰 하나 고칠 때마다 인프라를 세워야 한다. 그러면 "누구나 검증할 수 있다"가
 * 성립하지 않는다. 이 CLI는 파일만 읽고 순수 도메인 코드만 호출한다.
 *
 * <p>같은 jar가 서버로도 CLI로도 동작한다 —
 * {@code java -cp app.jar io.opencertflow.cli.OpenCertFlowCli validate rules/}
 */
@Command(
        name = "opencertflow",
        mixinStandardHelpOptions = true,
        version = "OpenCertFlow CLI 0.1.0",
        description = "KC 인증 사전진단 룰을 서버 없이 검증하고 실행한다.",
        subcommands = {
                ValidateCommand.class,
                ExplainCommand.class,
                CommandLine.HelpCommand.class
        })
public final class OpenCertFlowCli implements Runnable {

    @Override
    public void run() {
        // 서브커맨드 없이 실행하면 사용법을 보여 준다.
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new OpenCertFlowCli())
                // 출력이 한글이다. JDK 17의 기본 stdout 인코딩은 플랫폼 의존이라 Windows(MS949)에서
                // 깨진다. 여기서 UTF-8로 못 박아 두면 실행 방식과 무관하게 같은 바이트가 나간다.
                .setOut(utf8(System.out))
                .setErr(utf8(System.err))
                .execute(args);
        System.exit(exitCode);
    }

    private static PrintWriter utf8(java.io.OutputStream stream) {
        return new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8), true);
    }
}
