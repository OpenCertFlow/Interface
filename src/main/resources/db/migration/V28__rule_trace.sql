-- 발동한 룰과 그 이유를 진단에 함께 남긴다.
--
-- 이유: 룰셋은 개정된다. 나중에 "왜 이 결과가 나왔는가"를 되짚으려고 그때의 룰을 다시 평가하려면
-- 그 버전의 룰이 남아 있어야 하는데, 그 보장을 전제로 두는 대신 결과 자체를 스냅샷으로 남긴다.
-- 룰셋 버전·가중치를 이미 같은 방식으로 다루고 있다(05-data-model.md).
ALTER TABLE diagnosis ADD COLUMN rule_trace jsonb NOT NULL DEFAULT '[]'::jsonb;
