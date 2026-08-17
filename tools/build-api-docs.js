#!/usr/bin/env node
/*
 * openapi/openapi.json → docs/api/openapi.html (자체 완결형 Swagger UI)
 *
 * 왜 단일 파일인가: 이 문서는 심사·상담 자리에서 그대로 열려야 한다. CDN을 참조하면 인터넷이
 * 없거나 사내망이 막힌 곳에서 빈 화면이 뜬다. CSS·JS·스펙을 전부 인라인해 파일 하나로 만든다.
 *
 * 사용:
 *   node tools/build-api-docs.js                 # 자산이 없으면 내려받는다
 *   ./gradlew apiDocsHtml                        # 위와 동일
 *
 * 자산은 tools/vendor/에 캐시된다. 저장소에 커밋하지 않는다(.gitignore) — 1.7MB짜리 벤더
 * 번들을 이력에 남길 이유가 없고, 필요하면 언제든 다시 받을 수 있다.
 */
const fs = require('fs');
const path = require('path');
const https = require('https');

const ROOT = path.resolve(__dirname, '..');
const SPEC = path.join(ROOT, 'openapi', 'openapi.json');
const OUT = path.join(ROOT, 'docs', 'api', 'openapi.html');
const VENDOR = path.join(__dirname, 'vendor');

// 버전을 고정한다. 'latest'로 두면 생성할 때마다 결과가 달라져 diff가 의미를 잃는다.
const SWAGGER_UI_VERSION = '5.32.13';
const ASSETS = ['swagger-ui.css', 'swagger-ui-bundle.js'];

function download(url, dest) {
  return new Promise((resolve, reject) => {
    https
      .get(url, (res) => {
        if (res.statusCode === 302 || res.statusCode === 301) {
          return download(res.headers.location, dest).then(resolve, reject);
        }
        if (res.statusCode !== 200) {
          return reject(new Error(`${url} → HTTP ${res.statusCode}`));
        }
        const chunks = [];
        res.on('data', (c) => chunks.push(c));
        res.on('end', () => {
          fs.writeFileSync(dest, Buffer.concat(chunks));
          resolve();
        });
      })
      .on('error', reject);
  });
}

async function ensureAssets() {
  fs.mkdirSync(VENDOR, { recursive: true });
  for (const name of ASSETS) {
    const dest = path.join(VENDOR, name);
    if (fs.existsSync(dest) && fs.statSync(dest).size > 1000) continue;
    const url = `https://unpkg.com/swagger-ui-dist@${SWAGGER_UI_VERSION}/${name}`;
    process.stdout.write(`  내려받는 중: ${name} … `);
    await download(url, dest);
    console.log(`${(fs.statSync(dest).size / 1024).toFixed(0)}KB`);
  }
}

function build() {
  if (!fs.existsSync(SPEC)) {
    console.error('openapi/openapi.json이 없습니다. ./gradlew updateOpenApiSpec 을 먼저 돌리세요.');
    process.exit(2);
  }
  const spec = JSON.parse(fs.readFileSync(SPEC, 'utf8'));
  const css = fs.readFileSync(path.join(VENDOR, 'swagger-ui.css'), 'utf8');
  const js = fs.readFileSync(path.join(VENDOR, 'swagger-ui-bundle.js'), 'utf8');

  const title = (spec.info && spec.info.title) || 'API';
  const version = (spec.info && spec.info.version) || '';

  // </script>가 스펙 문자열 안에 있으면 스크립트 블록이 조기 종료된다. 이스케이프한다.
  const specJson = JSON.stringify(spec).replace(/<\/script>/gi, '<\\/script>');

  const html = `<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title} ${version} — API 명세서</title>
<style>${css}</style>
<style>
  body { margin: 0; background: #fafafa; }
  .ocf-header {
    padding: 20px 28px; background: #1b1b1f; color: #fff;
    font-family: system-ui, -apple-system, "Segoe UI", "Malgun Gothic", sans-serif;
  }
  .ocf-header h1 { margin: 0 0 6px; font-size: 20px; font-weight: 700; letter-spacing: -0.01em; }
  .ocf-header p { margin: 0; font-size: 13px; opacity: .75; line-height: 1.6; }
  .ocf-header code { background: rgba(255,255,255,.12); padding: 1px 5px; border-radius: 3px; }
  .swagger-ui .topbar { display: none; }
  @media print { .ocf-header { background: #fff; color: #000; border-bottom: 2px solid #000; } }
</style>
</head>
<body>
<div class="ocf-header">
  <h1>${title} <span style="opacity:.6;font-weight:400">${version}</span></h1>
  <p>
    이 파일은 <code>openapi/openapi.json</code>에서 생성된 자체 완결형 문서입니다.
    인터넷 없이 열립니다. 갱신: <code>./gradlew apiDocsHtml</code>
  </p>
</div>
<div id="swagger-ui"></div>
<script>${js}</script>
<script>
  window.ui = SwaggerUIBundle({
    spec: ${specJson},
    dom_id: '#swagger-ui',
    deepLinking: true,
    // 'list' — 태그를 펼쳐 엔드포인트 목록까지 바로 보인다. 명세서로 훑을 때는
    // 클릭해야 목록이 나오는 'none'보다 이쪽이 쓸모 있다.
    docExpansion: 'list',
    defaultModelsExpandDepth: 0,
    displayRequestDuration: true,
    filter: true,
    tryItOutEnabled: false,
    presets: [SwaggerUIBundle.presets.apis],
  });
</script>
</body>
</html>
`;

  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, html, 'utf8');

  const paths = Object.keys(spec.paths || {}).length;
  const ops = Object.values(spec.paths || {}).reduce(
    (n, p) => n + Object.keys(p).filter((m) => ['get', 'post', 'put', 'patch', 'delete'].includes(m)).length,
    0
  );
  console.log(
    `생성: docs/api/openapi.html  (${(html.length / 1024 / 1024).toFixed(2)}MB, ` +
      `경로 ${paths}개 · 오퍼레이션 ${ops}개)`
  );
}

ensureAssets().then(build).catch((e) => {
  console.error('실패: ' + e.message);
  process.exit(1);
});
