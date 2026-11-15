# 恢复源码

本目录从仓库已有的 algorithm.jar、game.jar、desktop-1.0.0.jar 使用 CFR 0.152 恢复，不是找回的原始 Gradle 工程。保留反编译来源标注，修复 RaceWorld 中攻击命中变量的反编译错误，并实现随机地图。所有类均参与重新编译，资源和第三方依赖沿用现有 JAR。

仓库根目录运行 `bash build-demo.sh`（Java 21 JDK；可设置 JAVA_HOME）。该命令运行原玩法回归和随机地图验证，成功后覆盖原有三个 JAR 及试玩 ZIP。ZIP 排除历史运行日志。图形检查另见发行目录 DEMO.md。
