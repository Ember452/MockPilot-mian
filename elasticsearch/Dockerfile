# MockPilot Elasticsearch 镜像：官方镜像 + IK 中文分词插件
# 插件来源为 infinilabs 官方分发源，版本号必须与 ES 版本完全一致
ARG ES_VERSION=9.2.1
FROM elasticsearch:${ES_VERSION}
ARG ES_VERSION
# 安装失败不阻断构建：清理可能的半成品残留，保证 ES 能以无 IK 状态正常启动，
# 后端已实现无 IK 时自动降级 standard 分词
RUN bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/${ES_VERSION} \
    || { bin/elasticsearch-plugin remove analysis-ik --purge >/dev/null 2>&1 || true; \
         rm -rf /usr/share/elasticsearch/plugins/analysis-ik; \
         echo "WARN: IK plugin install failed, ES will run without it (backend falls back to standard analyzer)"; }
