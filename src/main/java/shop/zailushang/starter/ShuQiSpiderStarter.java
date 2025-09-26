package shop.zailushang.starter;

import shop.zailushang.flow.Flow;
import shop.zailushang.flow.FlowEngine;

import java.util.List;

/**
 * 本程序在编时，该网站大多数书籍均为免费阅读，后该网站调整运营策略，全文章需要 VIP校验，仅前 20 章可免费阅读
 * 最新流程：    Reader[载]  →  Selector[择]  →  Parser[析]  →  Decoder[译]  →  Formatter[椠]  →  Writer[录]  →  Merger[撰]  →  Cleaner[涤]
 * 下载VIP章节需在 {@link shop.zailushang.component.Reader#read0 } 中添加对应 cookie 认证
 * 下载完成时默认清理零散章节 {@link shop.zailushang.flow.FlowEngine#NEED_DELETE } 可控制关闭
 * 测试下载前20章时，开启 {@link Flow.Flows#contentListFlow()} 87行注释
 */
public class ShuQiSpiderStarter {
    public static void main(String[] args) {
        // 禁用 GraalVM 警告日志
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
        try (var engine = FlowEngine.getDefaultFlowEngine()) {
            // 单独下载一本
            engine.start("大主宰");
            // 多本一起下
            //List.of("斗破苍穹", "武动乾坤", "大主宰", "元尊").parallelStream().forEach(engine::start);
        }
    }
}