package shop.zailushang.starter;

import shop.zailushang.flow.Flow;
import shop.zailushang.flow.FlowEngine;

/**
 * 仅可用于下载免费阅读的章节，下载 VIP章节 时，需要添加 VIP 账号登录认证
 * 在 {@link shop.zailushang.component.Reader#read0 } 中添加对应 cookie
 * 作者注：本程序在编时，该网站大多数书籍均为免费阅读，后该网站调整运营策略，全文章需要 VIP校验，仅前 20 章免费
 * 如想测试下载前20章，开启 {@link Flow.Flows#contentListFlow()}  } 85行注释
 */
public class ShuQiSpiderStarter {
    public static void main(String[] args) {
        // 禁用 GraalVM 警告日志
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        try (var engine = FlowEngine.getDefaultFlowEngine()) {
            // 单独下载一本
            engine.start("斗破苍穹");
            // 多本一起下
            // List.of("斗破苍穹", "武动乾坤", "大主宰", "元尊").forEach(engine::start);
        }
    }
}